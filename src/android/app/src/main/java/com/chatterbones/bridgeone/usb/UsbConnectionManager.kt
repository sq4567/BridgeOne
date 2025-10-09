package com.chatterbones.bridgeone.usb

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException

/**
 * USB 연결 관리자 클래스
 * 
 * CP2102 USB-Serial 칩셋 기반 ESP32-S3 장치의 자동 감지, 권한 요청, 연결 관리를 담당합니다.
 * Cancel and Restart 패턴을 적용하여 안전한 연결 복구를 보장합니다.
 * 
 * 주요 기능:
 * - CP2102 장치 자동 스캔 및 감지 (VID: 0x10C4, PID: 0xEA60)
 * - USB 장치 접근 권한 요청 및 응답 처리
 * - 연결 상태를 StateFlow로 실시간 관리
 * - USB 장치 연결/분리 이벤트 감지
 * 
 * @param context Android 애플리케이션 컨텍스트
 * 
 * @see [technical-specification-app.md §1.1.1 하드웨어 연결 요구사항]
 * @see [technical-specification-app.md §1.1.2 연결 관리 요구사항]
 * @see [technical-specification-app.md §1.6.1 Cancel and Restart 패턴 구현]
 */
class UsbConnectionManager(private val context: Context) {

    companion object {
        private const val TAG = "UsbConnectionManager"
        
        // CP2102 USB-Serial 칩셋 식별자
        // @see [technical-specification-app.md §1.1.1 하드웨어 연결 요구사항]
        private const val CP2102_VENDOR_ID = 0x10C4  // Silicon Labs
        private const val CP2102_PRODUCT_ID = 0xEA60 // CP2102 USB to UART Bridge Controller
        
        // 권한 요청 액션
        private const val ACTION_USB_PERMISSION = "com.chatterbones.bridgeone.USB_PERMISSION"
        
        // USB Serial 통신 파라미터
        // @see [technical-specification-app.md §1.1.1 하드웨어 연결 요구사항]
        private const val BAUD_RATE = 1000000          // 1Mbps (1,000,000 bps)
        private const val DATA_BITS = 8                // 8비트 데이터
        private const val STOP_BITS = UsbSerialPort.STOPBITS_1  // 1 스톱비트
        private const val PARITY = UsbSerialPort.PARITY_NONE    // 패리티 없음
        private const val READ_TIMEOUT_MS = 200        // 읽기 타임아웃 200ms
    }

    // USB 관리자 시스템 서비스
    private val usbManager: UsbManager =
        context.getSystemService(Context.USB_SERVICE) as UsbManager

    // 연결 상태 StateFlow (읽기 전용 외부 노출)
    private val _connectionState = MutableStateFlow<UsbConnectionState>(UsbConnectionState.Disconnected)
    val connectionState: StateFlow<UsbConnectionState> = _connectionState.asStateFlow()

    // 현재 감지된 USB 장치
    private var currentDevice: UsbDevice? = null
    
    // USB Serial 포트 인스턴스
    private var serialPort: UsbSerialPort? = null

    /**
     * USB 권한 응답 및 장치 연결/분리 이벤트를 처리하는 BroadcastReceiver
     * 
     * 처리 이벤트:
     * - ACTION_USB_PERMISSION: 권한 요청 응답 수신
     * - UsbManager.ACTION_USB_DEVICE_ATTACHED: USB 장치 연결 감지
     * - UsbManager.ACTION_USB_DEVICE_DETACHED: USB 장치 분리 감지
     */
    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_USB_PERMISSION -> {
                    // 권한 요청 응답 처리
                    synchronized(this) {
                        val device: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                        }

                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            // 권한 승인 성공 - Serial 포트 열기
                            device?.let {
                                Log.i(TAG, "USB 권한 승인: ${it.deviceName}")
                                // Serial 포트 열기 (openSerialPort()에서 Connected 상태로 전환)
                                openSerialPort()
                            }
                        } else {
                            // 권한 거부
                            Log.w(TAG, "USB 권한 거부: ${device?.deviceName}")
                            _connectionState.value = UsbConnectionState.Error(
                                "USB 장치 접근 권한이 거부되었습니다. 권한을 허용해야 BridgeOne을 사용할 수 있습니다.",
                                null
                            )
                        }
                    }
                }

                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    // USB 장치 연결 이벤트
                    val device: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    }

                    device?.let {
                        if (isCp2102Device(it)) {
                            Log.i(TAG, "CP2102 장치 연결 감지: ${it.deviceName}")
                            scanAndRequestPermission()
                        }
                    }
                }

                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    // USB 장치 분리 이벤트
                    val device: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    }

                    device?.let {
                        if (isCp2102Device(it)) {
                            Log.i(TAG, "CP2102 장치 분리 감지: ${it.deviceName}")
                            closeSerialPort()  // Serial 포트 닫기
                            currentDevice = null
                            _connectionState.value = UsbConnectionState.Disconnected
                        }
                    }
                }
            }
        }
    }

    /**
     * USB 연결 관리자 초기화
     * 
     * BroadcastReceiver를 등록하고 CP2102 장치를 스캔합니다.
     * Activity의 onCreate() 또는 ViewModel의 초기화에서 호출해야 합니다.
     */
    fun initialize() {
        Log.d(TAG, "UsbConnectionManager 초기화 시작")

        // BroadcastReceiver 등록
        val filter = IntentFilter().apply {
            addAction(ACTION_USB_PERMISSION)
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(usbReceiver, filter)
        }

        // 초기 장치 스캔
        scanAndRequestPermission()
    }

    /**
     * USB 연결 관리자 정리
     * 
     * BroadcastReceiver를 해제하고 리소스를 정리합니다.
     * Activity의 onDestroy() 또는 ViewModel의 onCleared()에서 호출해야 합니다.
     */
    fun cleanup() {
        Log.d(TAG, "UsbConnectionManager 정리")
        
        // Serial 포트 닫기
        closeSerialPort()
        
        try {
            context.unregisterReceiver(usbReceiver)
        } catch (e: IllegalArgumentException) {
            // 이미 등록 해제된 경우 무시
            Log.w(TAG, "BroadcastReceiver 등록 해제 실패: ${e.message}")
        }
        currentDevice = null
        _connectionState.value = UsbConnectionState.Disconnected
    }

    /**
     * CP2102 장치 스캔 및 권한 요청
     * 
     * 연결된 모든 USB 장치를 스캔하여 CP2102 칩셋을 식별하고,
     * 권한이 없는 경우 사용자에게 권한을 요청합니다.
     * 
     * @see [technical-specification-app.md §1.1.2 연결 관리 요구사항]
     */
    fun scanAndRequestPermission() {
        Log.d(TAG, "USB 장치 스캔 시작")

        // 연결된 모든 USB 장치 목록 가져오기
        val deviceList: HashMap<String, UsbDevice> = usbManager.deviceList
        Log.d(TAG, "연결된 USB 장치 수: ${deviceList.size}")

        // CP2102 장치 필터링
        val cp2102Device = deviceList.values.firstOrNull { device ->
            isCp2102Device(device).also { isMatch ->
                if (isMatch) {
                    Log.i(TAG, "CP2102 장치 발견: VID=0x${device.vendorId.toString(16)}, PID=0x${device.productId.toString(16)}, Name=${device.deviceName}")
                }
            }
        }

        if (cp2102Device == null) {
            Log.w(TAG, "CP2102 장치를 찾을 수 없습니다")
            _connectionState.value = UsbConnectionState.Error(
                "USB 장치를 찾을 수 없습니다. CP2102 칩셋이 탑재된 ESP32-S3 장치를 연결해주세요.",
                null
            )
            return
        }

        currentDevice = cp2102Device

        // 권한 확인 및 요청
        if (usbManager.hasPermission(cp2102Device)) {
            Log.i(TAG, "USB 장치 권한 이미 보유: ${cp2102Device.deviceName}")
            // Serial 포트 열기 (openSerialPort()에서 Connected 상태로 전환)
            openSerialPort()
        } else {
            Log.i(TAG, "USB 장치 권한 요청: ${cp2102Device.deviceName}")
            _connectionState.value = UsbConnectionState.Requesting

            // 권한 요청 PendingIntent 생성
            val permissionIntent = PendingIntent.getBroadcast(
                context,
                0,
                Intent(ACTION_USB_PERMISSION),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
            )

            // 권한 요청
            usbManager.requestPermission(cp2102Device, permissionIntent)
        }
    }

    /**
     * USB 장치가 CP2102 칩셋인지 확인
     * 
     * @param device 확인할 USB 장치
     * @return CP2102 장치 여부 (VID: 0x10C4, PID: 0xEA60)
     * 
     * @see [technical-specification-app.md §1.1.1 하드웨어 연결 요구사항]
     */
    private fun isCp2102Device(device: UsbDevice): Boolean {
        return device.vendorId == CP2102_VENDOR_ID && device.productId == CP2102_PRODUCT_ID
    }

    /**
     * USB Serial 포트 열기 및 통신 설정
     * 
     * 권한이 승인된 USB 장치에 대해 Serial 포트를 열고 1Mbps UART 통신을 설정합니다.
     * 이 메서드는 권한 승인 후 자동으로 호출되며, 통신 파라미터를 구성합니다.
     * 
     * **통신 파라미터:**
     * - Baud Rate: 1,000,000 bps (1Mbps)
     * - Data Bits: 8
     * - Stop Bits: 1
     * - Parity: None
     * - Read Timeout: 200ms
     * 
     * **예외 처리:**
     * - IOException: 포트 열기 실패, 통신 설정 실패
     * - SecurityException: 권한 부족
     * - IllegalArgumentException: 잘못된 통신 파라미터
     * 
     * @see [technical-specification-app.md §1.1.1 하드웨어 연결 요구사항]
     * @see [technical-specification-app.md §1.1.2 연결 관리 요구사항]
     */
    fun openSerialPort() {
        val device = currentDevice
        if (device == null) {
            Log.e(TAG, "USB 장치가 없어 Serial 포트를 열 수 없습니다")
            _connectionState.value = UsbConnectionState.Error(
                "USB 장치를 찾을 수 없습니다.",
                null
            )
            return
        }
        
        try {
            // UsbSerialDriver 인스턴스 생성 (usb-serial-for-android 라이브러리)
            val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
            val driver = availableDrivers.firstOrNull { it.device.deviceId == device.deviceId }
            
            if (driver == null) {
                Log.e(TAG, "USB Serial 드라이버를 찾을 수 없습니다: ${device.deviceName}")
                _connectionState.value = UsbConnectionState.Error(
                    "USB Serial 드라이버를 찾을 수 없습니다. CP2102 드라이버가 지원되지 않을 수 있습니다.",
                    null
                )
                return
            }
            
            // USB 연결 생성
            val connection = usbManager.openDevice(driver.device)
            if (connection == null) {
                Log.e(TAG, "USB 장치 연결을 열 수 없습니다: ${device.deviceName}")
                _connectionState.value = UsbConnectionState.Error(
                    "USB 장치 연결을 열 수 없습니다. USB 케이블을 확인해주세요.",
                    null
                )
                return
            }
            
            // Serial 포트 가져오기 (첫 번째 포트 사용)
            val port = driver.ports.firstOrNull()
            if (port == null) {
                connection.close()
                Log.e(TAG, "USB Serial 포트를 찾을 수 없습니다: ${device.deviceName}")
                _connectionState.value = UsbConnectionState.Error(
                    "USB Serial 포트를 찾을 수 없습니다.",
                    null
                )
                return
            }
            
            // 포트 열기
            port.open(connection)
            Log.i(TAG, "USB Serial 포트 열기 성공: ${device.deviceName}")
            
            // 통신 파라미터 설정 (1Mbps, 8N1)
            port.setParameters(
                BAUD_RATE,    // 1,000,000 bps (1Mbps)
                DATA_BITS,    // 8 데이터 비트
                STOP_BITS,    // 1 스톱 비트
                PARITY        // 패리티 없음
            )
            Log.i(TAG, "USB Serial 통신 파라미터 설정 완료: ${BAUD_RATE}bps, ${DATA_BITS}N${STOP_BITS}")
            
            // 읽기 타임아웃 설정 (200ms)
            // usb-serial-for-android 라이브러리에서는 read() 메서드 호출 시 타임아웃을 파라미터로 전달
            // 여기서는 포트가 성공적으로 열렸음을 확인
            Log.i(TAG, "USB Serial 읽기 타임아웃: ${READ_TIMEOUT_MS}ms")
            
            // Serial 포트 저장
            serialPort = port
            
            // 연결 성공 상태로 전환
            _connectionState.value = UsbConnectionState.Connected(device.deviceName)
            Log.i(TAG, "USB Serial 포트 초기화 완료: ${device.deviceName}")
            
        } catch (e: IOException) {
            Log.e(TAG, "USB Serial 포트 열기 실패 (IOException): ${e.message}", e)
            _connectionState.value = UsbConnectionState.Error(
                "USB Serial 포트를 열 수 없습니다: ${e.message}",
                e
            )
            closeSerialPort()
        } catch (e: SecurityException) {
            Log.e(TAG, "USB Serial 포트 열기 실패 (SecurityException): ${e.message}", e)
            _connectionState.value = UsbConnectionState.Error(
                "USB 장치 접근 권한이 부족합니다: ${e.message}",
                e
            )
            closeSerialPort()
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "USB Serial 통신 파라미터 설정 실패: ${e.message}", e)
            _connectionState.value = UsbConnectionState.Error(
                "통신 파라미터 설정에 실패했습니다: ${e.message}",
                e
            )
            closeSerialPort()
        } catch (e: Exception) {
            Log.e(TAG, "USB Serial 포트 초기화 중 예상치 못한 오류: ${e.message}", e)
            _connectionState.value = UsbConnectionState.Error(
                "USB 연결 중 오류가 발생했습니다: ${e.message}",
                e
            )
            closeSerialPort()
        }
    }
    
    /**
     * USB Serial 포트 닫기 및 리소스 정리
     * 
     * 열려있는 Serial 포트를 안전하게 닫고 관련 리소스를 해제합니다.
     * 이 메서드는 연결 오류 발생 시, USB 장치 분리 시, 또는 앱 종료 시 호출됩니다.
     * 
     * **정리 작업:**
     * - Serial 포트 닫기 (IOException 처리)
     * - 포트 인스턴스 null로 설정
     * - 연결 상태를 Disconnected로 전환 (선택적)
     */
    fun closeSerialPort() {
        serialPort?.let { port ->
            try {
                port.close()
                Log.i(TAG, "USB Serial 포트 닫기 완료")
            } catch (e: IOException) {
                Log.w(TAG, "USB Serial 포트 닫기 중 오류: ${e.message}", e)
            }
        }
        serialPort = null
    }
    
    /**
     * 현재 열린 USB Serial 포트 반환
     * 
     * @return 현재 열려있는 UsbSerialPort 인스턴스, 닫혀있는 경우 null
     */
    fun getSerialPort(): UsbSerialPort? = serialPort
    
    /**
     * 현재 연결된 USB 장치 반환
     * 
     * @return 현재 연결된 CP2102 USB 장치, 연결되지 않은 경우 null
     */
    fun getCurrentDevice(): UsbDevice? = currentDevice
}

