package com.bridgeone.app.usb

import android.content.Context
import android.hardware.usb.UsbManager
import android.util.Log
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.IOException

/**
 * USB 직렬 통신을 관리하는 싱글톤 매니저.
 *
 * 이 클래스는 usb-serial-for-android 라이브러리를 통해 USB 장치와
 * 1Mbps 8N1 설정으로 통신합니다.
 */
object UsbSerialManager {
    private const val TAG = "UsbSerialManager"
    
    // USB 디바이스 상수 (Espressif VID: 0x303A)
    private const val ESPRESSIF_VID = 0x303A
    private const val ESP32_S3_PID_1 = 0x1001    // CP2102N USB-to-UART
    private const val ESP32_S3_PID_2 = 0x8108    // Alternate PID configuration
    
    // USB 포트 및 연결 상태
    private var usbPort: UsbSerialPort? = null
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected
    
    // Android UsbManager (액티비티에서 주입)
    private var usbManager: UsbManager? = null
    private var context: Context? = null
    
    /**
     * UsbSerialManager를 초기화합니다.
     *
     * @param context Android Context (UsbManager 획득용)
     */
    fun initialize(context: Context) {
        Log.d(TAG, "Initializing UsbSerialManager")
        this.context = context
        usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
    }
    
    /**
     * 현재 USB 포트가 열려있는지 확인합니다.
     *
     * @return 포트가 열려있으면 true, 아니면 false
     */
    fun isPortOpen(): Boolean = usbPort != null && _isConnected.value
    
    /**
     * VID 필터링을 통해 ESP32-S3 USB 장치를 찾습니다.
     *
     * usb-serial-for-android의 UsbSerialProber를 사용하여 연결된 USB 장치 중
     * Espressif VID(0x303A)를 가진 장치를 찾고, 첫 번째로 발견된 UsbSerialPort를
     * 반환합니다. 찾지 못한 경우 null을 반환합니다.
     *
     * @return ESP32-S3 USB 장치의 UsbSerialPort, 또는 null
     */
    fun findEsp32s3Device(): UsbSerialPort? {
        if (usbManager == null) {
            Log.e(TAG, "UsbManager not initialized")
            return null
        }
        
        // UsbSerialProber를 통해 모든 연결된 USB 직렬 장치 획득
        val drivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager!!)
        
        if (drivers.isEmpty()) {
            Log.w(TAG, "No USB serial devices found")
            return null
        }
        
        // Espressif VID를 가진 장치 검색
        for (driver in drivers) {
            val device = driver.device
            if (device.vendorId == ESPRESSIF_VID && 
                (device.productId == ESP32_S3_PID_1 || device.productId == ESP32_S3_PID_2)) {
                
                // 일치하는 장치의 포트 획득 (일반적으로 첫 번째 포트)
                if (driver.ports.isNotEmpty()) {
                    val port = driver.ports[0]
                    Log.d(TAG, "Found ESP32-S3 device: VID=0x${device.vendorId.toString(16)}, " +
                            "PID=0x${device.productId.toString(16)}, Name=${device.deviceName}")
                    return port
                }
            }
        }
        
        Log.w(TAG, "ESP32-S3 device not found (looking for VID 0x${ESPRESSIF_VID.toString(16)})")
        return null
    }
    
    /**
     * ESP32-S3 USB 장치에 대한 권한을 요청합니다.
     *
     * Android USB Host API의 requestPermission() 메서드를 사용하여
     * 사용자 또는 시스템으로부터 USB 장치 액세스 권한을 요청합니다.
     * 
     * 권한 부여 결과는 BroadcastReceiver를 통해 ACTION_USB_PERMISSION
     * Intent로 전달되며, 액티비티에서 등록한 리시버가 처리합니다.
     *
     * @param port 권한을 요청할 UsbSerialPort
     * @param permissionReceiver 권한 결과를 수신할 PendingIntent
     */
    fun requestPermission(port: UsbSerialPort, permissionReceiver: android.app.PendingIntent) {
        if (usbManager == null) {
            Log.e(TAG, "UsbManager not initialized")
            return
        }
        
        val device = port.device
        Log.d(TAG, "Requesting USB permission for device: ${device.deviceName}")
        
        // UsbManager.requestPermission() 호출 - Android USB Host API 공식 API
        // 권한 결과는 permissionReceiver의 Intent로 전달됨
        usbManager!!.requestPermission(device, permissionReceiver)
    }
    
    /**
     * USB 권한 Intent 결과를 처리합니다.
     *
     * BroadcastReceiver에서 ACTION_USB_PERMISSION Intent를 수신할 때
     * 이 메서드를 호출하여 권한 부여 여부를 확인합니다.
     *
     * @param intent ACTION_USB_PERMISSION Intent
     * @return 권한이 부여되면 true, 거부되면 false
     */
    @Suppress("DEPRECATION")
    fun handlePermissionResult(intent: android.content.Intent): Boolean {
        // Intent 엑스트라에서 권한 여부 추출
        // UsbManager.requestPermission()의 결과로 다음 엑스트라가 포함됨:
        // - "permissionGranted": Boolean (권한 부여 여부)
        // - UsbManager.EXTRA_DEVICE: UsbDevice (대상 장치)
        val isGranted = intent.getBooleanExtra("permissionGranted", false)
        val device = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // Android 13 이상: 타입 안전 오버로드 사용
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, android.hardware.usb.UsbDevice::class.java)
        } else {
            // Android 12 이하: 기존 메서드 사용 (deprecated 경고 무시)
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
        }
        
        return if (isGranted && device != null) {
            Log.d(TAG, "USB permission granted for device: ${device.deviceName}")
            true
        } else {
            Log.w(TAG, "USB permission denied for device: ${device?.deviceName ?: "unknown"}")
            false
        }
    }
    
    /**
     * USB 직렬 포트를 엽니다 (1Mbps, 8N1 설정).
     *
     * 포트를 열기 전에 USB 권한이 확보되어야 합니다.
     * 권한 미확보 시 Exception을 던집니다.
     * 성공 시 _isConnected를 true로 설정합니다.
     *
     * **기술 정보**:
     * - Baud Rate: 1,000,000 bps (1Mbps)
     * - Data Bits: 8
     * - Stop Bits: 1
     * - Parity: None (0)
     * - usb-serial-for-android 라이브러리의 UsbSerialPort.open() 및
     *   setParameters() API 사용
     *
     * @param port 열 UsbSerialPort (일반적으로 findEsp32s3Device() 결과)
     * @throws IOException USB 포트 열기 실패 시
     * @throws SecurityException USB 권한 미확보 시
     */
    fun openPort(port: UsbSerialPort) {
        try {
            if (usbManager == null) {
                throw IllegalStateException("UsbManager not initialized")
            }
            
            // 권한 확인: UsbManager에서 해당 장치의 권한 확보 여부 검사
            val hasPermission = usbManager!!.hasPermission(port.device)
            if (!hasPermission) {
                Log.e(TAG, "USB permission not granted for device: ${port.device.deviceName}")
                throw SecurityException("USB permission not granted for device: ${port.device.deviceName}")
            }
            
            Log.d(TAG, "Opening USB port: ${port.device.deviceName}")
            
            // 1. UsbDeviceConnection 획득 (usb-serial-for-android의 open()은 UsbDeviceConnection을 받음)
            val usbConnection = usbManager!!.openDevice(port.device)
            if (usbConnection == null) {
                throw IOException("Failed to open USB device connection: ${port.device.deviceName}")
            }
            
            // 2. 포트 열기 (usb-serial-for-android의 UsbSerialPort.open(UsbDeviceConnection))
            port.open(usbConnection)
            
            // 3. 통신 파라미터 설정 - 1Mbps 8N1
            // setParameters(baudrate, dataBits, stopBits, parity)
            port.setParameters(
                1000000,        // Baud rate: 1Mbps
                8,              // Data bits: 8
                1,              // Stop bits: 1
                UsbSerialPort.PARITY_NONE               // Parity: None
            )
            
            // 4. 포트 오픈 성공 - 상태 업데이트
            usbPort = port
            _isConnected.value = true
            
            Log.d(TAG, "USB port opened successfully: ${port.device.deviceName} (1Mbps 8N1)")
        } catch (e: SecurityException) {
            Log.e(TAG, "USB permission error: ${e.message}")
            _isConnected.value = false
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open USB port: ${e.message}", e)
            _isConnected.value = false
            throw IOException("Failed to open USB port: ${e.message}", e)
        }
    }
    
    /**
     * USB 직렬 포트를 닫습니다.
     *
     * 포트가 열려있는 경우 안전하게 닫고 리소스를 해제합니다.
     * _isConnected를 false로 설정합니다.
     * 포트가 이미 닫혀있는 경우 아무 작업도 수행하지 않습니다.
     */
    fun closePort() {
        try {
            if (usbPort == null) {
                Log.d(TAG, "USB port is already closed")
                _isConnected.value = false
                return
            }
            
            Log.d(TAG, "Closing USB port")
            
            // usb-serial-for-android의 UsbSerialPort.close()
            usbPort!!.close()
            
            usbPort = null
            _isConnected.value = false
            
            Log.d(TAG, "USB port closed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing USB port: ${e.message}", e)
            // 에러 발생해도 상태는 false로 설정
            _isConnected.value = false
        }
    }
    
    /**
     * USB 직렬 포트의 연결 상태를 반환합니다.
     *
     * @return 포트가 열려있고 연결되어 있으면 true, 아니면 false
     */
    fun isConnected(): Boolean = _isConnected.value
}
