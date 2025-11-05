package com.bridgeone.app.usb

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log

/**
 * USB 권한 요청 결과를 처리하는 BroadcastReceiver 클래스입니다.
 * 
 * 사용자가 USB 기기의 권한 요청에 승인 또는 거부하면,
 * 이 BroadcastReceiver를 통해 권한 결과를 수신합니다.
 * 
 * 권한 결과에 따라 후속 작업을 수행합니다:
 * - 승인: USB 기기와의 통신 시작
 * - 거부: 사용자에게 권한 거부 안내
 */
class UsbPermissionReceiver : BroadcastReceiver() {
    
    /**
     * USB 권한 요청 결과를 수신합니다.
     * 
     * android.hardware.usb.action.USB_PERMISSION 인텐트 필터를 통해
     * 사용자의 권한 승인/거부 결과를 수신합니다.
     * 
     * @param context 현재 앱의 Context
     * @param intent 권한 요청 결과를 포함한 Intent
     *               - UsbManager.EXTRA_DEVICE: 요청한 USB 기기
     *               - UsbManager.EXTRA_PERMISSION_GRANTED: 권한 승인 여부 (boolean)
     */
    override fun onReceive(context: Context, intent: Intent) {
        // 인텐트 액션 검증
        if (intent.action != ACTION_USB_PERMISSION) {
            Log.w(TAG, "Received unexpected action: ${intent.action}")
            return
        }

        // Intent에서 USB 기기와 권한 결과 추출
        // getParcelableExtra()는 API 33 이상 필요하므로, 호환성을 위해 @Suppress 사용
        @Suppress("DEPRECATION")
        val device: UsbDevice? = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(
                UsbManager.EXTRA_DEVICE,
                UsbDevice::class.java
            )
        } else {
            // API 32 이하: 레거시 방식 사용
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
        }
        val permissionGranted = intent.getBooleanExtra(
            UsbManager.EXTRA_PERMISSION_GRANTED,
            false
        )

        if (device == null) {
            Log.e(TAG, "USB device is null in permission result")
            return
        }

        if (permissionGranted) {
            Log.i(TAG, "USB permission granted for device: VID=${device.vendorId}, PID=${device.productId}")
            
            // 권한이 승인된 경우: USB 기기와의 통신 준비
            // Phase 2.2.2에서 UsbSerialManager가 이를 처리합니다.
            // 여기서는 콜백을 통해 성공 신호를 전달할 수 있습니다.
            notifyPermissionResult(context, device, true)
        } else {
            Log.w(TAG, "USB permission denied for device: VID=${device.vendorId}, PID=${device.productId}")
            
            // 권한이 거부된 경우: 사용자에게 알리기
            notifyPermissionResult(context, device, false)
        }
    }

    /**
     * USB 권한 결과를 알립니다 (Phase 2.2.2.2에서 UsbSerialManager 연동).
     * 
     * UsbSerialManager의 notifyPermissionResult() 콜백을 호출하여
     * 권한 승인/거부 결과를 관리 시스템에 통보합니다.
     * 
     * **권한 결과 처리:**
     * - 권한 승인: UsbSerialManager.notifyPermissionResult()에서 추후 포트 열기 준비
     * - 권한 거부: UsbSerialManager.notifyPermissionResult()에서 포트 닫기 및 정리
     * 
     * @param context 현재 앱의 Context
     * @param device 권한 결과에 해당하는 USB 기기
     * @param granted 권한 승인 여부
     */
    private fun notifyPermissionResult(
        context: Context,
        device: UsbDevice,
        granted: Boolean
    ) {
        val status = if (granted) "granted" else "denied"
        Log.d(TAG, "Permission status: $status for device ${device.deviceName}")
        
        // Phase 2.2.2.2: UsbSerialManager 연동
        // UsbSerialManager에서 권한 결과를 처리하도록 콜백
        UsbSerialManager.notifyPermissionResult(context, device, granted)
    }

    companion object {
        private const val TAG = "UsbPermissionReceiver"
        private const val ACTION_USB_PERMISSION = "android.hardware.usb.action.USB_PERMISSION"
    }
}

/**
 * USB 기기에 권한을 요청합니다.
 * 
 * android.permission.USB_DEVICE 권한이 필요한 경우,
 * 이 함수를 호출하여 사용자에게 권한 요청 대화상자를 표시합니다.
 * 
 * 권한 결과는 UsbPermissionReceiver를 통해 수신됩니다.
 * 
 * **권한 요청 흐름:**
 * 1. Context.sendBroadcast()를 통해 PendingIntent 생성 및 등록
 * 2. UsbManager.requestPermission()으로 권한 요청 시작
 * 3. 사용자가 승인/거부 선택
 * 4. UsbPermissionReceiver.onReceive()에서 결과 처리
 * 
 * @param context 현재 앱의 Context (Activity 또는 Service)
 * @param usbManager USB 관리자 인스턴스 (context.getSystemService())
 * @param device 권한을 요청할 USB 기기
 */
fun requestUsbPermission(
    context: Context,
    usbManager: UsbManager,
    device: UsbDevice
) {
    Log.d("requestUsbPermission", "Requesting USB permission for device: ${device.deviceName}")

    // PendingIntent 생성:
    // - ACTION: android.hardware.usb.action.USB_PERMISSION
    // - FLAG_UPDATE_CURRENT: 기존 PendingIntent가 있으면 갱신
    // - FLAG_IMMUTABLE: Android 12+ 보안 권장사항 (수정 불가)
    val permissionIntent = android.app.PendingIntent.getBroadcast(
        context,
        device.deviceId,  // requestCode: 디바이스별로 고유한 ID
        Intent(ACTION_USB_PERMISSION).apply {
            setPackage(context.packageName)  // 패키지 지정 (보안)
        },
        android.app.PendingIntent.FLAG_UPDATE_CURRENT or
        android.app.PendingIntent.FLAG_IMMUTABLE
    )

    // USB Manager를 통해 권한 요청
    // - 권한이 없으면 사용자 대화상자 표시
    // - 권한이 있으면 즉시 BroadcastReceiver 콜백 호출
    usbManager.requestPermission(device, permissionIntent)
    Log.i("requestUsbPermission", "USB permission request sent for device: ${device.deviceName}")
}

/**
 * USB 권한을 확인합니다 (Runtime Permissions).
 * 
 * Android 6.0(API 23)부터는 런타임 권한 요청이 필수입니다.
 * 이 함수는 기기가 해당 USB 권한을 가지고 있는지 확인합니다.
 * 
 * @param context 현재 앱의 Context
 * @return 권한이 있으면 true, 없으면 false
 */
fun hasUsbPermission(context: Context): Boolean {
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
        // Android 6.0 이상: 런타임 권한 확인
        // USB_DEVICE는 비표준 권한이므로, 공식 정의되지 않음
        // AndroidManifest.xml에서 권한을 선언했으므로 checkSelfPermission 사용 불필요
        // 대신 직접 권한 문자열로 확인
        context.checkSelfPermission("android.permission.USB_DEVICE") ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
    } else {
        // Android 5.x 이하: 매니페스트 권한만으로 충분
        true
    }
}

private const val ACTION_USB_PERMISSION = "android.hardware.usb.action.USB_PERMISSION"

