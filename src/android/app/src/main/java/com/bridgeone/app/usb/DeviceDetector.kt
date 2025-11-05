package com.bridgeone.app.usb

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log

/**
 * ESP32-S3 보드를 감지하는 헬퍼 클래스입니다.
 *
 * USB 디바이스 목록을 순회하여 VID/PID 필터링을 통해
 * BridgeOne ESP32-S3 디바이스를 자동으로 감지합니다.
 *
 * 감지 후 자동으로 권한 요청을 트리거합니다.
 *
 * 참조:
 * - UsbConstants: ESP32-S3 VID/PID 상수 정의
 * - UsbPermissionReceiver.kt: requestUsbPermission() 함수
 */
object DeviceDetector {

    private const val TAG = "DeviceDetector"

    /**
     * USB 디바이스 목록에서 ESP32-S3 보드를 찾습니다.
     *
     * UsbManager의 디바이스 목록을 순회하면서 다음 조건을 확인:
     * 1. VID가 0x303A (Espressif Systems) 인지 확인
     * 2. PID가 0x82C5 (BridgeOne) 인지 확인
     *
     * **흐름:**
     * 1. usbManager.deviceList를 순회
     * 2. 각 디바이스의 vendorId와 productId를 확인
     * 3. VID/PID가 일치하는 디바이스 발견 시 즉시 반환
     * 4. 발견되지 않으면 null 반환
     *
     * **호출 위치:**
     * - Phase 2.2.2에서 UsbSerialManager 초기화 시
     * - 권한 요청 전에 호출되어 디바이스 존재 여부 확인
     *
     * @param context 현재 앱의 Context (UsbManager 획득용)
     * @return ESP32-S3 디바이스가 발견되면 UsbDevice 인스턴스, 없으면 null
     */
    fun findEsp32s3Device(context: Context): UsbDevice? {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
        
        if (usbManager == null) {
            Log.e(TAG, "Failed to get UsbManager from system service")
            return null
        }

        return findEsp32s3Device(usbManager)
    }

    /**
     * 제공된 UsbManager를 사용하여 ESP32-S3 보드를 찾습니다.
     *
     * 이 오버로드된 함수는 테스트 또는 기존 UsbManager 인스턴스를 사용할 때
     * 유용합니다.
     *
     * **VID/PID 필터링 로직:**
     * - VID (Vendor ID): 0x303A (상수: UsbConstants.ESP32_S3_VID)
     * - PID (Product ID): 0x82C5 (상수: UsbConstants.ESP32_S3_PID)
     *
     * **검증:**
     * - usbManager.deviceList가 null 또는 비어있는 경우 처리
     * - VID/PID 일치 여부를 16진수 형식으로 로그 출력
     *
     * @param usbManager USB 관리자 인스턴스
     * @return ESP32-S3 디바이스가 발견되면 UsbDevice 인스턴스, 없으면 null
     */
    fun findEsp32s3Device(usbManager: UsbManager): UsbDevice? {
        val deviceList = usbManager.deviceList
        
        if (deviceList.isEmpty()) {
            Log.i(TAG, "No USB devices found")
            return null
        }

        Log.d(TAG, "Searching for ESP32-S3 device among ${deviceList.size} USB device(s)")

        // USB 디바이스 목록 순회
        for (device in deviceList.values) {
            Log.d(
                TAG,
                "Checking device: ${device.deviceName} " +
                "VID=0x${device.vendorId.toString(16).uppercase().padStart(4, '0')}, " +
                "PID=0x${device.productId.toString(16).uppercase().padStart(4, '0')}"
            )

            // VID/PID 필터링: ESP32-S3 BridgeOne 디바이스 확인
            if (device.vendorId == UsbConstants.ESP32_S3_VID &&
                device.productId == UsbConstants.ESP32_S3_PID
            ) {
                Log.i(
                    TAG,
                    "ESP32-S3 device found: ${device.deviceName} " +
                    "(VID=0x${device.vendorId.toString(16).uppercase()}, " +
                    "PID=0x${device.productId.toString(16).uppercase()})"
                )
                return device
            }
        }

        Log.w(TAG, "ESP32-S3 device not found. Looking for VID=0x${UsbConstants.ESP32_S3_VID.toString(16).uppercase()}, " +
            "PID=0x${UsbConstants.ESP32_S3_PID.toString(16).uppercase()}")
        return null
    }

    /**
     * ESP32-S3 보드를 찾고 권한을 요청합니다.
     *
     * 이 함수는 DeviceDetector의 편의 함수로, 다음을 수행합니다:
     * 1. findEsp32s3Device()를 호출하여 디바이스 검색
     * 2. 디바이스 발견 시 requestUsbPermission()을 호출하여 권한 요청
     * 3. 발견 여부를 반환값으로 알림
     *
     * **호출 예시:**
     * ```kotlin
     * val context = this  // Activity 또는 Fragment
     * if (DeviceDetector.findAndRequestPermission(context)) {
     *     Log.i("App", "USB permission request initiated")
     * } else {
     *     Log.w("App", "ESP32-S3 device not found")
     * }
     * ```
     *
     * **권한 결과 처리:**
     * - 권한 결과는 UsbPermissionReceiver.onReceive()에서 처리됨
     * - 권한 승인 시 Phase 2.2.2의 UsbSerialManager가 연동
     *
     * @param context 현재 앱의 Context
     * @return 디바이스 발견 여부 (발견 및 권한 요청 시작 시 true)
     */
    fun findAndRequestPermission(context: Context): Boolean {
        val device = findEsp32s3Device(context) ?: return false

        val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
        if (usbManager == null) {
            Log.e(TAG, "Failed to get UsbManager for permission request")
            return false
        }

        // 권한 요청 트리거
        // requestUsbPermission()은 UsbPermissionReceiver.kt에서 구현됨
        requestUsbPermission(context, usbManager, device)
        return true
    }
}

