package com.bridgeone.app.usb

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log
import com.bridgeone.app.protocol.BridgeFrame
import com.bridgeone.app.usb.UsbConstants
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * USB Serial 통신 싱글톤 매니저
 *
 * Android 앱에서 ESP32-S3 BridgeOne 디바이스와의 USB Serial 통신을 관리합니다.
 * usb-serial-for-android 라이브러리를 사용하여 일반적인 USB-to-Serial 칩셋을 지원합니다.
 *
 * **싱글톤 패턴**:
 * - Kotlin의 `object` 선언으로 자동 싱글톤 구현
 * - 스레드 안전(Thread-safe) 보장
 * - 지연 초기화(Lazy initialization)
 *
 * **사용 예시**:
 * ```kotlin
 * // USB Serial 포트 열기 (1Mbps, 8N1 설정)
 * UsbSerialManager.openPort(device)
 *
 * // 프레임 전송
 * UsbSerialManager.sendFrame(frame)
 *
 * // 연결 상태 확인
 * val isConnected = UsbSerialManager.isConnected()
 *
 * // 포트 닫기
 * UsbSerialManager.closePort()
 * ```
 *
 * **주의사항**:
 * - 포트는 Activity가 시작될 때 열고, 종료될 때 반드시 닫아야 합니다.
 * - USB 권한(Permission)은 별도로 요청되어야 합니다 (UsbPermissionReceiver 참조).
 * - 모든 UART 통신은 1Mbps 8N1 형식으로 설정됩니다 (BridgeOne 프로토콜).
 *
 * 참조:
 * - usb-serial-for-android: https://github.com/mik3y/usb-serial-for-android
 * - UsbConstants.kt: USB 상수 및 UART 설정
 * - UsbPermissionReceiver.kt: USB 권한 요청 처리
 * - DeviceDetector.kt: ESP32-S3 디바이스 자동 감지
 */
/**
 * USB 디버그 상태 정보 (임시 디버그용)
 */
data class UsbDebugState(
    val allDevices: List<UsbDeviceInfo> = emptyList(),
    val targetDevice: UsbDeviceInfo? = null,
    val isConnected: Boolean = false,
    val connectionStatus: String = "초기화 대기",
    val lastError: String? = null
)

/**
 * USB 장치 정보 (디버그 표시용)
 */
data class UsbDeviceInfo(
    val deviceName: String,
    val vendorId: Int,
    val productId: Int,
    val manufacturerName: String?,
    val productName: String?,
    val isTarget: Boolean = false
) {
    val vidHex: String get() = "0x${vendorId.toString(16).uppercase().padStart(4, '0')}"
    val pidHex: String get() = "0x${productId.toString(16).uppercase().padStart(4, '0')}"
}

object UsbSerialManager {

    // ========== 태그 정의 (로그 출력용) ==========
    private const val TAG = "UsbSerialManager"

    // ========== 디버그 상태 (UI 표시용) ==========
    private val _debugState = MutableStateFlow(UsbDebugState())
    val debugState: StateFlow<UsbDebugState> = _debugState.asStateFlow()

    // ========== 데이터 멤버 (상태 관리) ==========

    /**
     * Android USB Manager 인스턴스.
     * USB 디바이스 관리를 위해 필요합니다.
     * null이면 아직 초기화되지 않은 상태입니다.
     */
    private var usbManager: UsbManager? = null

    /**
     * USB Serial 포트 인스턴스.
     * null이면 포트가 닫혀있거나 아직 열려있지 않은 상태입니다.
     */
    private var usbSerialPort: UsbSerialPort? = null

    /**
     * USB 포트 연결 상태 플래그.
     * true이면 포트가 열려있고 통신 가능한 상태입니다.
     */
    private var isConnected: Boolean = false

    // ========== 공개 인터페이스 (Public API) ==========

    /**
     * USB Manager 설정 (초기화).
     *
     * UsbSerialManager를 사용하기 전에 반드시 호출해야 합니다.
     * Context에서 UsbManager를 획득하여 저장합니다.
     *
     * **Android API**:
     * - `context.getSystemService(Context.USB_SERVICE)` → UsbManager
     *
     * @param manager Android USB Manager 인스턴스
     * @throws IllegalArgumentException manager가 null인 경우
     *
     * 예시:
     * ```kotlin
     * val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
     * UsbSerialManager.setUsbManager(usbManager)
     * ```
     */
    fun setUsbManager(manager: UsbManager) {
        this.usbManager = manager
        Log.d(TAG, "UsbManager initialized")
    }

    /**
     * USB Serial 포트 열기.
     *
     * 지정된 USB 디바이스를 사용하여 포트를 열고, 1Mbps 8N1 형식으로 설정합니다.
     * 포트가 이미 열려있으면 먼저 닫고 새로 엽니다.
     *
     * **프로세스**:
     * 1. 기존 포트가 열려있으면 closePort() 호출
     * 2. UsbSerialProber를 사용하여 UsbSerialDriver 탐색
     * 3. 첫 번째 포트(port 0) 획득
     * 4. USB Manager에서 포트 열기
     * 5. setParameters()로 1Mbps 8N1 설정
     * 6. isConnected = true
     *
     * @param device Android USB 디바이스 객체
     * @throws IllegalStateException UsbManager가 초기화되지 않았거나, 포트 열기 실패
     * @throws IOException USB 통신 실패
     *
     * 예시:
     * ```kotlin
     * val device = findEsp32s3Device() // DeviceDetector.findEsp32s3Device()
     * UsbSerialManager.openPort(device)
     * ```
     */
    fun openPort(device: android.hardware.usb.UsbDevice) {
        val manager = usbManager
        check(manager != null) { "UsbManager not initialized. Call setUsbManager() first." }

        try {
            // 기존 포트 닫기
            if (isConnected) {
                closePort()
            }

            // UsbSerialProber로 드라이버 찾기
            val driver = UsbSerialProber.getDefaultProber()
                .probeDevice(device)
                ?: throw IllegalStateException("No USB Serial driver found for device: ${device.deviceName}")

            // 첫 번째 포트 획득
            val port = driver.ports.firstOrNull()
                ?: throw IllegalStateException("Device has no available serial ports")

            usbSerialPort = port

            // 포트 열기 (타임아웃: UsbConstants.USB_OPEN_TIMEOUT_MS)
            // manager는 로컬 val이므로 스마트 캐스트 가능
            val portConnection = manager.openDevice(device)
                ?: throw IllegalStateException("Cannot open USB device: ${device.deviceName}")

            port.open(portConnection)

            // UART 파라미터 설정: 1Mbps, 8N1
            port.setParameters(
                UsbConstants.UART_BAUDRATE,    // 1Mbps
                UsbConstants.UART_DATA_BITS,   // 8 bits
                UsbConstants.UART_STOP_BITS,   // 1 stop bit
                UsbConstants.UART_PARITY       // No parity (0)
            )

            isConnected = true
            Log.d(TAG, "USB Serial port opened successfully: ${device.deviceName} (1Mbps, 8N1)")

        } catch (e: Exception) {
            isConnected = false
            Log.e(TAG, "Failed to open USB Serial port: ${e.message}", e)
            throw IllegalStateException("Cannot open USB Serial port", e)
        }
    }

    /**
     * USB Serial 포트 닫기.
     *
     * 포트가 열려있으면 안전하게 닫고, 관련 리소스를 정리합니다.
     * 여러 번 호출해도 안전합니다 (이미 닫혀있으면 무시됨).
     *
     * **프로세스**:
     * 1. isConnected 플래그 확인
     * 2. 포트가 열려있으면 port.close() 호출
     * 3. usbSerialPort = null
     * 4. isConnected = false
     *
     * 주의: USB 디바이스 연결 해제 감지 시에도 자동으로 호출됩니다.
     */
    fun closePort() {
        try {
            if (isConnected && usbSerialPort != null) {
                usbSerialPort?.close()
                Log.d(TAG, "USB Serial port closed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error closing USB Serial port: ${e.message}", e)
        } finally {
            usbSerialPort = null
            isConnected = false
        }
    }

    /**
     * USB Serial 포트 연결 상태 확인.
     *
     * @return 포트가 열려있고 통신 가능한 상태이면 true
     */
    fun isConnected(): Boolean = isConnected

    /**
     * 현재 USB Serial 포트 인스턴스 획득 (내부 용도).
     *
     * Phase 2.2.2.4에서 sendFrame() 구현 시 사용됩니다.
     * 일반적으로 외부 코드에서는 직접 접근하지 않습니다.
     *
     * @return UsbSerialPort 인스턴스, 포트가 없으면 null
     */
    internal fun getPort(): UsbSerialPort? = usbSerialPort

    /**
     * BridgeFrame을 직렬화하여 UART로 전송합니다 (Phase 2.2.2.4).
     *
     * 다음 작업을 수행합니다:
     * 1. 포트 연결 상태 확인
     * 2. frame.toByteArray()로 8바이트 데이터 직렬화
     * 3. USB Serial 포트로 데이터 전송 (port.write()는 void 반환)
     * 4. IOException 발생 시 예외 처리 및 포트 자동 종료
     *
     * **사용 예시:**
     * ```kotlin
     * val frame = FrameBuilder.buildFrame(
     *     buttons = 0x01u,
     *     deltaX = 10,
     *     deltaY = -5,
     *     wheel = 0,
     *     modifiers = 0u,
     *     keyCode1 = 0u,
     *     keyCode2 = 0u
     * )
     * UsbSerialManager.sendFrame(frame)
     * ```
     *
     * **예외 처리:**
     * - USB 포트가 닫혀있으면 IllegalStateException 발생
     * - UART 전송 실패 (IOException) 시 포트 자동 닫기 후 예외 던짐
     *
     * **구현 참고:**
     * - usb-serial-for-android 라이브러리의 UsbSerialPort.write()는 void 반환
     * - 전송 성공 여부는 IOException 발생 여부로 판단
     *
     * @param frame 전송할 BridgeFrame 객체
     * @throws IllegalStateException 포트가 열려있지 않은 경우 또는 전송 실패
     *
     * 참조:
     * - BridgeFrame.kt: frame.toByteArray() 직렬화 메서드
     * - UsbConstants.kt: DELTA_FRAME_SIZE, USB_WRITE_TIMEOUT_MS 상수
     * - usb-serial-for-android: UsbSerialPort.write() API
     */
    fun sendFrame(frame: BridgeFrame) {
        // 포트 연결 상태 확인
        val port = usbSerialPort
        check(port != null && isConnected) { "USB Serial port is not connected" }

        try {
            // BridgeFrame을 8바이트 ByteArray로 직렬화
            val frameData = frame.toByteArray()

            // 프레임 크기 검증 (내부 안전장치)
            check(frameData.size == UsbConstants.DELTA_FRAME_SIZE) {
                "Invalid frame size: ${frameData.size}, expected: ${UsbConstants.DELTA_FRAME_SIZE}"
            }

            // USB Serial 포트로 데이터 전송 (Phase 2.1.8.4에서 확인: write()는 void 반환)
            // IOException이 발생하면 catch 블록에서 처리
            port.write(frameData, UsbConstants.USB_WRITE_TIMEOUT_MS)

            // 전송 성공 로그 (프레임 전송 정보)
            Log.d(
                TAG,
                "Frame sent successfully - seq=${frame.seq}, buttons=${frame.buttons}, " +
                    "dx=${frame.deltaX}, dy=${frame.deltaY}, size=${UsbConstants.DELTA_FRAME_SIZE}"
            )

        } catch (e: IOException) {
            // USB 전송 실패 (타임아웃 또는 포트 오류)
            // 포트 자동 종료 후 예외 던짐
            Log.e(TAG, "IOException during frame transmission: ${e.message}", e)
            closePort()
            throw IllegalStateException("USB transmission failed, port closed", e)

        } catch (e: IllegalStateException) {
            // 프레임 크기 또는 포트 연결 상태 검증 실패
            Log.e(TAG, "Frame validation failed: ${e.message}", e)
            throw e
        }
    }

    // ========== 권한 처리 함수 (Phase 2.2.2.2에서 추가) ==========

    /**
     * USB 기기에 권한을 요청합니다 (Phase 2.2.1.4.2의 requestUsbPermission() 래핑).
     *
     * android.permission.USB_DEVICE 권한이 필요한 경우,
     * 이 함수를 호출하여 사용자에게 권한 요청 대화상자를 표시합니다.
     *
     * 권한 결과는 UsbPermissionReceiver를 통해 수신됩니다.
     *
     * **권한 요청 흐름:**
     * 1. PendingIntent 생성 및 등록
     * 2. UsbManager.requestPermission()으로 권한 요청 시작
     * 3. 사용자가 승인/거부 선택
     * 4. UsbPermissionReceiver.onReceive()에서 결과 처리
     *
     * @param context 현재 앱의 Context (Activity 또는 Service)
     * @param device 권한을 요청할 USB 기기
     *
     * 예시:
     * ```kotlin
     * UsbSerialManager.requestPermission(context, device)
     * ```
     */
    fun requestPermission(context: android.content.Context, device: android.hardware.usb.UsbDevice) {
        val manager = usbManager
        check(manager != null) { "UsbManager not initialized. Call setUsbManager() first." }

        // Phase 2.2.1.4.2의 requestUsbPermission() 함수 위임
        requestUsbPermission(context, manager, device)
        Log.d(TAG, "USB permission requested for device: ${device.deviceName}")
    }

    /**
     * USB 권한을 확인합니다 (Phase 2.2.1.4.2의 hasUsbPermission() 래핑).
     *
     * Android 6.0(API 23)부터는 런타임 권한 요청이 필수입니다.
     * 이 함수는 기기가 해당 USB 권한을 가지고 있는지 확인합니다.
     *
     * @param context 현재 앱의 Context
     * @return 권한이 있으면 true, 없으면 false
     */
    fun hasPermission(context: android.content.Context): Boolean {
        return hasUsbPermission(context)
    }

    /**
     * ESP32-S3 디바이스를 찾고 초기화 및 연결을 시도합니다 (Phase 2.2.1.4.4의 DeviceDetector 통합).
     *
     * 다음 작업을 수행합니다:
     * 1. DeviceDetector.findAndRequestPermission()을 호출하여 디바이스 검색 및 권한 요청
     * 2. 권한 승인 대기 (비동기 콜백)
     *
     * **주의사항**:
     * - 이 함수는 권한 요청만 수행하며, 실제 포트 열기는 권한 승인 후 수행됩니다.
     * - 권한 결과는 UsbPermissionReceiver.notifyPermissionResult()를 통해 처리됩니다.
     * - Phase 2.2.2.3에서 더 자세한 초기화/연결 로직이 구현됩니다.
     *
     * @param context 현재 앱의 Context
     * @return 디바이스 발견 및 권한 요청 시작 시 true, 실패 시 false
     *
     * 예시:
     * ```kotlin
     * if (UsbSerialManager.initializeAndConnect(context)) {
     *     Log.i(TAG, "USB permission request initiated")
     * } else {
     *     Log.w(TAG, "ESP32-S3 device not found")
     * }
     * ```
     */
    fun initializeAndConnect(context: android.content.Context): Boolean {
        return DeviceDetector.findAndRequestPermission(context)
    }

    /**
     * 권한 결과 콜백을 처리합니다 (Phase 2.2.2.2에서 추가, Phase 2.2.2.3에서 업데이트).
     *
     * UsbPermissionReceiver에서 호출되어 권한 승인/거부 결과를 처리합니다.
     * 권한 상태를 SharedPreferences에 저장합니다.
     *
     * **권한 승인 시**: 상태를 "granted"로 저장
     * **권한 거부 시**: 상태를 초기화하고 포트 닫기
     *
     * @param context 현재 앱의 Context
     * @param device 권한 요청 대상 디바이스
     * @param granted 권한 승인 여부
     */
    fun notifyPermissionResult(
        context: android.content.Context,
        device: android.hardware.usb.UsbDevice,
        granted: Boolean
    ) {
        if (granted) {
            // Phase 2.2.2.3: 권한 상태를 SharedPreferences에 저장
            savePermissionStatus(context, true)
            Log.d(TAG, "USB permission granted and saved for device: ${device.deviceName}")
            
            // Phase 2.3.1.1: 권한 승인 후 자동으로 포트 오픈
            try {
                openPort(device)
                Log.i(TAG, "USB port opened successfully after permission granted")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open port after permission granted: ${e.message}", e)
                clearPermissionStatus(context)
            }
        } else {
            // Phase 2.2.2.3: 권한 상태 초기화 및 포트 닫기
            clearPermissionStatus(context)
            closePort()
            Log.w(TAG, "USB permission denied for device: ${device.deviceName}, port closed")
        }
    }

    // ========== 고수준 연결 함수 (Phase 2.2.2.3에서 추가) ==========

    /**
     * SharedPreferences 키: USB 권한 상태.
     * 값은 "granted" 또는 "denied"
     */
    private const val PREF_USB_PERMISSION_STATUS = "usb_permission_status"

    /**
     * SharedPreferences에 권한 상태를 저장합니다.
     *
     * 권한이 승인되었을 때 호출되어 상태를 "granted"로 저장합니다.
     *
     * @param context 현재 앱의 Context
     * @param granted 권한 승인 여부
     */
    private fun savePermissionStatus(context: Context, granted: Boolean) {
        val sharedPrefs = context.getSharedPreferences("bridge_one_usb", Context.MODE_PRIVATE)
        val status = if (granted) "granted" else "denied"
        sharedPrefs.edit().apply {
            putString(PREF_USB_PERMISSION_STATUS, status)
            apply()
        }
        Log.d(TAG, "Permission status saved: $status")
    }

    /**
     * SharedPreferences에서 권한 상태를 조회합니다.
     *
     * @param context 현재 앱의 Context
     * @return 권한이 승인된 상태이면 "granted", 거부되었으면 "denied", 저장된 상태가 없으면 ""
     */
    private fun getPermissionStatus(context: Context): String {
        val sharedPrefs = context.getSharedPreferences("bridge_one_usb", Context.MODE_PRIVATE)
        return sharedPrefs.getString(PREF_USB_PERMISSION_STATUS, "") ?: ""
    }

    /**
     * SharedPreferences에서 권한 상태를 초기화합니다.
     *
     * 권한이 거부되었을 때 호출되어 이전 승인 상태를 제거합니다.
     *
     * @param context 현재 앱의 Context
     */
    private fun clearPermissionStatus(context: Context) {
        val sharedPrefs = context.getSharedPreferences("bridge_one_usb", Context.MODE_PRIVATE)
        sharedPrefs.edit().apply {
            remove(PREF_USB_PERMISSION_STATUS)
            apply()
        }
        Log.d(TAG, "Permission status cleared")
    }

    /**
     * Context에서 ESP32-S3 디바이스를 자동 감지하여 연결합니다 (Phase 2.2.2.3).
     *
     * DeviceDetector를 사용하여 연결 가능한 ESP32-S3 디바이스를 찾고,
     * 권한 요청을 시작합니다. 실제 포트 열기는 권한 승인 후 수행됩니다.
     *
     * **프로세스**:
     * 1. DeviceDetector.findEsp32s3Device(context)를 호출하여 디바이스 자동 감지
     * 2. null 반환 시 예외 처리 및 false 반환
     * 3. 디바이스 발견 시 권한 상태를 SharedPreferences에서 확인
     * 4. 이미 권한이 승인된 경우: 포트 열기 시도
     * 5. 권한이 없는 경우: 권한 요청 시작
     *
     * @param context 현재 앱의 Context
     * @return 디바이스 발견 및 연결 시도 시 true, 실패 시 false
     *
     * 예시:
     * ```kotlin
     * if (UsbSerialManager.connect(context)) {
     *     Log.i(TAG, "Connection process started")
     * } else {
     *     Log.w(TAG, "ESP32-S3 device not found")
     * }
     * ```
     */
    fun connect(context: Context): Boolean {
        try {
            // Phase 2.2.1.4.4의 DeviceDetector 활용
            val device = DeviceDetector.findEsp32s3Device(context)
                ?: run {
                    Log.w(TAG, "ESP32-S3 device not found")
                    return false
                }

            // USB Manager 초기화 확인
            if (usbManager == null) {
                val manager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
                if (manager == null) {
                    Log.e(TAG, "Failed to get UsbManager")
                    return false
                }
                setUsbManager(manager)
            }

            // SharedPreferences에서 권한 상태 확인
            val permissionStatus = getPermissionStatus(context)

            if (permissionStatus == "granted") {
                // 권한이 이미 승인된 경우: 포트 열기
                try {
                    openPort(device)
                    Log.i(TAG, "USB port opened successfully with cached permission")
                    return true
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to open port with cached permission: ${e.message}, requesting new permission")
                    clearPermissionStatus(context)
                    // 권한 요청으로 재시도
                    requestPermission(context, device)
                    return true
                }
            } else {
                // 권한이 없는 경우: 권한 요청 시작
                requestPermission(context, device)
                Log.i(TAG, "USB permission request initiated for device: ${device.deviceName}")
                return true
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error in connect(): ${e.message}", e)
            return false
        }
    }

    /**
     * USB 연결을 해제합니다 (Phase 2.2.2.3).
     *
     * closePort()를 래핑하여 더 명확한 인터페이스를 제공합니다.
     * 포트가 이미 닫혀있으면 안전하게 무시됩니다.
     *
     * 예시:
     * ```kotlin
     * UsbSerialManager.disconnect()
     * ```
     */
    fun disconnect() {
        closePort()
        Log.i(TAG, "USB connection disconnected")
    }

    // ========== 디버그 함수 (임시) ==========

    /**
     * USB 장치 목록을 스캔하고 디버그 상태를 업데이트합니다.
     * UI에서 현재 연결된 USB 장치들을 표시하기 위해 사용됩니다.
     *
     * @param context 현재 앱의 Context
     */
    fun scanAndUpdateDebugState(context: Context) {
        try {
            val manager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
            if (manager == null) {
                _debugState.value = _debugState.value.copy(
                    connectionStatus = "UsbManager 획득 실패",
                    lastError = "시스템 서비스 없음"
                )
                return
            }

            val deviceList = manager.deviceList
            val allDevices = deviceList.values.map { device ->
                val isTarget = device.vendorId == UsbConstants.ESP32_S3_VID &&
                        device.productId == UsbConstants.ESP32_S3_PID
                UsbDeviceInfo(
                    deviceName = device.deviceName,
                    vendorId = device.vendorId,
                    productId = device.productId,
                    manufacturerName = device.manufacturerName,
                    productName = device.productName,
                    isTarget = isTarget
                )
            }

            val targetDevice = allDevices.find { it.isTarget }

            val status = when {
                deviceList.isEmpty() -> "USB 장치 없음"
                targetDevice != null && isConnected -> "연결됨 ✓"
                targetDevice != null -> "CH343P 발견 (권한 필요)"
                else -> "CH343P 미발견 (${deviceList.size}개 장치)"
            }

            _debugState.value = UsbDebugState(
                allDevices = allDevices,
                targetDevice = targetDevice,
                isConnected = isConnected,
                connectionStatus = status,
                lastError = null
            )

            Log.d(TAG, "Debug state updated: ${allDevices.size} devices, target=${targetDevice != null}, connected=$isConnected")

        } catch (e: Exception) {
            _debugState.value = _debugState.value.copy(
                connectionStatus = "스캔 오류",
                lastError = e.message
            )
            Log.e(TAG, "Error scanning USB devices: ${e.message}", e)
        }
    }

    /**
     * 디버그 상태의 연결 상태를 업데이트합니다 (내부용).
     */
    private fun updateDebugConnectionStatus(status: String, error: String? = null) {
        _debugState.value = _debugState.value.copy(
            isConnected = isConnected,
            connectionStatus = status,
            lastError = error
        )
    }
}

