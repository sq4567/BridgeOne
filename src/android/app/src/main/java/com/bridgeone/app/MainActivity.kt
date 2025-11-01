package com.bridgeone.app

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import com.bridgeone.app.ui.BridgeOneApp
import com.bridgeone.app.ui.theme.BridgeOneTheme
import com.bridgeone.app.usb.UsbSerialManager

/**
 * BridgeOne 안드로이드 앱의 메인 액티비티입니다.
 *
 * 이 액티비티는 BridgeOne 앱의 진입점으로, BridgeOneApp Composable을
 * 호출하여 UI를 구성합니다. 또한 USB 권한 요청 결과를 처리하는
 * BroadcastReceiver를 등록합니다.
 */
class MainActivity : ComponentActivity() {
    
    private val TAG = "MainActivity"
    
    // USB 권한 요청 Action 상수
    private val ACTION_USB_PERMISSION = "com.bridgeone.app.USB_PERMISSION"
    
    // BroadcastReceiver를 통한 USB 권한 처리
    private val usbPermissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_USB_PERMISSION) {
                // UsbSerialManager의 handlePermissionResult()를 통해 권한 결과 처리
                val isGranted = UsbSerialManager.handlePermissionResult(intent)
                
                if (isGranted) {
                    Log.d(TAG, "USB permission request successful")
                    // 다음 단계: Phase 2.1.8.3에서 포트 열기 및 통신 설정
                } else {
                    Log.w(TAG, "USB permission request denied by user")
                }
            } else if (intent?.action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
                // USB 장치가 새로 연결된 경우
                Log.d(TAG, "USB device attached")
                // 자동 감지 및 권한 요청 로직은 Phase 2.1.8.3에서 구현됨
            } else if (intent?.action == UsbManager.ACTION_USB_DEVICE_DETACHED) {
                // USB 장치가 분리된 경우
                Log.d(TAG, "USB device detached")
                // 연결 해제 처리 로직은 Phase 2.1.8.3에서 구현됨
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // UsbSerialManager 초기화
        UsbSerialManager.initialize(this)
        Log.d(TAG, "UsbSerialManager initialized")
        
        // USB 권한 요청 Intent 필터 등록 (ACTION_USB_PERMISSION)
        val intentFilter = IntentFilter().apply {
            addAction(ACTION_USB_PERMISSION)
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        
        // ContextCompat.registerReceiver()를 사용하여 all API level에서
        // 일관되게 RECEIVER_NOT_EXPORTED 플래그 적용 (보안 강화)
        // 본 리시버는 다른 앱과 공유되지 않으므로 RECEIVER_NOT_EXPORTED 사용
        ContextCompat.registerReceiver(
            this,
            usbPermissionReceiver,
            intentFilter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        
        Log.d(TAG, "USB BroadcastReceiver registered")
        
        setContent {
            BridgeOneTheme {
                BridgeOneApp()
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // BroadcastReceiver 등록 해제 (메모리 누수 방지)
        unregisterReceiver(usbPermissionReceiver)
        Log.d(TAG, "USB BroadcastReceiver unregistered")
    }
}