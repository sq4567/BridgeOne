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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
    }

    // USB 관리자 시스템 서비스
    private val usbManager: UsbManager =
        context.getSystemService(Context.USB_SERVICE) as UsbManager

    // 연결 상태 StateFlow (읽기 전용 외부 노출)
    private val _connectionState = MutableStateFlow<UsbConnectionState>(UsbConnectionState.Disconnected)
    val connectionState: StateFlow<UsbConnectionState> = _connectionState.asStateFlow()

    // 현재 감지된 USB 장치
    private var currentDevice: UsbDevice? = null

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
                            // 권한 승인 성공
                            device?.let {
                                Log.i(TAG, "USB 권한 승인: ${it.deviceName}")
                                _connectionState.value = UsbConnectionState.Connected(it.deviceName)
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
            _connectionState.value = UsbConnectionState.Connected(cp2102Device.deviceName)
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
     * 현재 연결된 USB 장치 반환
     * 
     * @return 현재 연결된 CP2102 USB 장치, 연결되지 않은 경우 null
     */
    fun getCurrentDevice(): UsbDevice? = currentDevice
}

