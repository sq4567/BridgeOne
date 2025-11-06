package com.bridgeone.app.usb

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log

/**
 * USB 장치 연결/해제 감지 BroadcastReceiver (Phase 2.2.2.4)
 *
 * Android 시스템의 USB 기기 연결 및 해제 이벤트를 감지하여,
 * UsbSerialManager의 연결 상태를 자동으로 관리합니다.
 *
 * **감지 이벤트:**
 * - `ACTION_USB_DEVICE_ATTACHED`: USB 기기 연결됨
 * - `ACTION_USB_DEVICE_DETACHED`: USB 기기 해제됨
 *
 * **동작:**
 * 1. ESP32-S3 디바이스 연결 시: `UsbSerialManager.connect(context)` 자동 호출
 * 2. ESP32-S3 디바이스 해제 시: `UsbSerialManager.disconnect()` 자동 호출
 * 3. 로그 기록 및 디버그 정보 출력
 *
 * **등록 방법:**
 * ```kotlin
 * val receiver = UsbDeviceDetectionReceiver()
 * val filter = UsbDeviceDetectionReceiver.getIntentFilter()
 * context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
 * ```
 *
 * **등록 해제:**
 * ```kotlin
 * context.unregisterReceiver(receiver)
 * ```
 *
 * 참조:
 * - UsbSerialManager.kt: connect/disconnect 함수
 * - UsbConstants.kt: ESP32_S3_VID, ESP32_S3_PID
 */
class UsbDeviceDetectionReceiver : BroadcastReceiver() {

    companion object {
        // 태그 (로그 출력용)
        private const val TAG = "UsbDeviceDetectionReceiver"

        /**
         * USB 기기 감지를 위한 Intent Filter 생성.
         *
         * 이 필터는 USB 기기 연결/해제 이벤트를 수신합니다.
         *
         * @return UsbDeviceDetectionReceiver 등록에 필요한 IntentFilter
         */
        fun getIntentFilter(): IntentFilter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
    }

    /**
     * USB 기기 연결/해제 이벤트 수신 콜백.
     *
     * **처리 흐름:**
     * 1. Intent 액션 확인 (ATTACHED 또는 DETACHED)
     * 2. USB 기기 정보 추출
     * 3. ESP32-S3 디바이스 여부 확인 (VID/PID)
     * 4. UsbSerialManager에 연결/해제 신호 전달
     * 5. 디버그 로그 기록
     *
     * @param context 현재 앱의 Context
     * @param intent USB 장치 이벤트 Intent
     *
     * 참조:
     * - UsbConstants.kt: ESP32_S3_VID (0x303A), ESP32_S3_PID (0x82C5)
     * - UsbManager.ACTION_USB_DEVICE_ATTACHED/DETACHED
     */
    override fun onReceive(context: Context?, intent: Intent?) {
        // null 안전성 확인
        if (context == null || intent == null) {
            Log.w(TAG, "onReceive called with null context or intent")
            return
        }

        // USB 기기 정보 추출 (Intent Extra로 전달됨)
        // API 32 이하 호환성 유지: getParcelableExtra(String) 사용
        @Suppress("DEPRECATION")
        val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)

        if (device == null) {
            Log.w(TAG, "No USB device found in intent")
            return
        }

        // 액션 확인 및 처리
        when (intent.action) {
            UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                // USB 기기 연결됨
                Log.i(TAG, "USB device attached - VID: 0x${device.vendorId.toString(16)}, " +
                    "PID: 0x${device.productId.toString(16)}")

                // ESP32-S3 디바이스 여부 확인
                if (isEsp32s3Device(device)) {
                    Log.i(TAG, "ESP32-S3 BridgeOne device detected")
                    // 자동 연결 시도
                    UsbSerialManager.connect(context)
                    Log.d(TAG, "USB connection initiated")
                } else {
                    Log.d(TAG, "Other USB device attached, ignoring")
                }
            }

            UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                // USB 기기 해제됨
                Log.i(TAG, "USB device detached - VID: 0x${device.vendorId.toString(16)}, " +
                    "PID: 0x${device.productId.toString(16)}")

                // ESP32-S3 디바이스 여부 확인
                if (isEsp32s3Device(device)) {
                    Log.i(TAG, "ESP32-S3 BridgeOne device disconnected")
                    // 자동 연결 해제
                    UsbSerialManager.disconnect()
                    Log.d(TAG, "USB connection closed")
                } else {
                    Log.d(TAG, "Other USB device detached, ignoring")
                }
            }

            else -> {
                Log.w(TAG, "Unknown intent action: ${intent.action}")
            }
        }
    }

    /**
     * 전달된 USB 기기가 ESP32-S3 BridgeOne인지 확인합니다.
     *
     * VID (Vendor ID) 및 PID (Product ID)를 확인하여 판별합니다:
     * - VID: 0x303A (Espressif Systems)
     * - PID: 0x82C5 (BridgeOne TinyUSB 디스크립터)
     *
     * @param device 확인할 USB 디바이스
     * @return ESP32-S3 BridgeOne 디바이스이면 true, 아니면 false
     *
     * 참조:
     * - UsbConstants.kt: ESP32_S3_VID (0x303A), ESP32_S3_PID (0x82C5)
     * - usb_descriptors.c (ESP32-S3 펌웨어): 디바이스 디스크립터 정의
     */
    private fun isEsp32s3Device(device: UsbDevice): Boolean =
        device.vendorId == UsbConstants.ESP32_S3_VID &&
        device.productId == UsbConstants.ESP32_S3_PID
}

