package com.bridgeone.app

import android.content.Context
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.bridgeone.app.ui.BridgeOneApp
import com.bridgeone.app.ui.theme.BridgeOneTheme
import com.bridgeone.app.usb.UsbDeviceDetectionReceiver
import com.bridgeone.app.usb.UsbPermissionReceiver
import com.bridgeone.app.usb.UsbSerialManager

/**
 * BridgeOne 안드로이드 앱의 메인 액티비티입니다.
 *
 * 이 액티비티는 BridgeOne 앱의 진입점으로, BridgeOneApp Composable을
 * 호출하여 UI를 구성합니다.
 *
 * **USB 통신 초기화:**
 * - onCreate(): UsbSerialManager 초기화 및 USB 디바이스 검색
 * - onStart(): BroadcastReceiver 등록 (USB 이벤트 감지)
 * - onStop(): BroadcastReceiver 등록 해제
 */
class MainActivity : ComponentActivity() {

    private lateinit var usbDeviceDetectionReceiver: UsbDeviceDetectionReceiver
    private lateinit var usbPermissionReceiver: UsbPermissionReceiver
    private var receiversRegistered = false

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BridgeOneTheme {
                BridgeOneApp()
            }
        }

        // Phase 2.3.1.1: USB Serial 인식 검증을 위한 초기화
        initializeUsbSystem()
    }

    override fun onStart() {
        super.onStart()
        // onStart()에서 BroadcastReceiver 등록
        // (onStop에서 등록 해제하므로, onStart에서 다시 등록)
        registerUsbBroadcastReceivers()
    }

    override fun onStop() {
        super.onStop()
        // 리소스 정리: BroadcastReceiver 등록 해제
        unregisterUsbBroadcastReceivers()
    }

    /**
     * USB 시스템 초기화 (onCreate에서 호출).
     *
     * 다음 작업을 수행합니다:
     * 1. UsbManager 초기화
     * 2. UsbSerialManager 설정
     * 3. USB 디바이스 자동 검색 및 권한 요청 시작
     *
     * 참조:
     * - UsbSerialManager.setUsbManager(): USB Manager 저장
     * - UsbSerialManager.connect(): 디바이스 검색 및 권한 요청
     */
    private fun initializeUsbSystem() {
        try {
            // UsbManager 획득
            val usbManager = getSystemService(Context.USB_SERVICE) as? UsbManager
            if (usbManager == null) {
                Log.e(TAG, "Failed to get UsbManager from system service")
                return
            }

            // UsbSerialManager 초기화
            UsbSerialManager.setUsbManager(usbManager)
            Log.d(TAG, "UsbManager initialized")

            // USB 디바이스 검색 및 권한 요청 시작
            // DeviceDetector가 자동으로 ESP32-S3을 찾고 권한을 요청합니다
            UsbSerialManager.connect(this)
            Log.i(TAG, "USB device search and permission request initiated")

        } catch (e: Exception) {
            Log.e(TAG, "Error initializing USB system: ${e.message}", e)
        }
    }

    /**
     * BroadcastReceiver 등록 (onStart에서 호출).
     *
     * 두 개의 BroadcastReceiver를 등록합니다:
     * 1. UsbDeviceDetectionReceiver: USB 연결/해제 이벤트 감지
     * 2. UsbPermissionReceiver: USB 권한 요청 결과 수신
     *
     * 참조:
     * - UsbDeviceDetectionReceiver.getIntentFilter(): USB 이벤트 필터
     */
    private fun registerUsbBroadcastReceivers() {
        if (receiversRegistered) {
            return  // 이미 등록된 경우 중복 등록 방지
        }

        try {
            // UsbDeviceDetectionReceiver 등록
            usbDeviceDetectionReceiver = UsbDeviceDetectionReceiver()
            val deviceDetectionFilter = UsbDeviceDetectionReceiver.getIntentFilter()
            registerReceiver(
                usbDeviceDetectionReceiver,
                deviceDetectionFilter,
                Context.RECEIVER_NOT_EXPORTED
            )
            Log.d(TAG, "UsbDeviceDetectionReceiver registered")

            // UsbPermissionReceiver 등록
            usbPermissionReceiver = UsbPermissionReceiver()
            val permissionFilter = android.content.IntentFilter().apply {
                addAction("android.hardware.usb.action.USB_PERMISSION")
            }
            registerReceiver(
                usbPermissionReceiver,
                permissionFilter,
                Context.RECEIVER_NOT_EXPORTED
            )
            Log.d(TAG, "UsbPermissionReceiver registered")

            receiversRegistered = true
        } catch (e: Exception) {
            Log.e(TAG, "Error registering BroadcastReceivers: ${e.message}", e)
        }
    }

    /**
     * BroadcastReceiver 등록 해제 (onStop에서 호출).
     *
     * 등록된 모든 BroadcastReceiver를 해제합니다.
     * 이를 통해 메모리 누수를 방지합니다.
     */
    private fun unregisterUsbBroadcastReceivers() {
        if (!receiversRegistered) {
            return  // 등록되지 않은 경우 무시
        }

        try {
            if (::usbDeviceDetectionReceiver.isInitialized) {
                unregisterReceiver(usbDeviceDetectionReceiver)
                Log.d(TAG, "UsbDeviceDetectionReceiver unregistered")
            }

            if (::usbPermissionReceiver.isInitialized) {
                unregisterReceiver(usbPermissionReceiver)
                Log.d(TAG, "UsbPermissionReceiver unregistered")
            }

            receiversRegistered = false
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering BroadcastReceivers: ${e.message}", e)
        }
    }
}