package com.bridgeone.app.usb

import android.hardware.usb.UsbManager
import android.util.Log
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber

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
object UsbSerialManager {

    // ========== 태그 정의 (로그 출력용) ==========
    private const val TAG = "UsbSerialManager"

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
        require(manager != null) { "UsbManager cannot be null" }
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
}

