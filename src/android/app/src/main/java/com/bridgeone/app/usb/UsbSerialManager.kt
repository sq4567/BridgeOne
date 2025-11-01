package com.bridgeone.app.usb

import android.content.Context
import android.hardware.usb.UsbManager
import android.util.Log
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * USB 직렬 통신을 관리하는 싱글톤 매니저.
 *
 * 이 클래스는 usb-serial-for-android 라이브러리를 통해 USB 장치와
 * 1Mbps 8N1 설정으로 통신합니다.
 */
object UsbSerialManager {
    private const val TAG = "UsbSerialManager"
    
    // USB 디바이스 상수
    private const val ESPRESSIF_VID = 0x303A  // ESP32-S3 VID
    
    // USB 포트 및 연결 상태
    private var usbPort: UsbSerialPort? = null
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected
    
    // Android UsbManager (액티비티에서 주입)
    private var usbManager: UsbManager? = null
    
    /**
     * UsbSerialManager를 초기화합니다.
     *
     * @param context Android Context (UsbManager 획득용)
     */
    fun initialize(context: Context) {
        Log.d(TAG, "Initializing UsbSerialManager")
        usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
    }
    
    /**
     * 현재 USB 포트가 열려있는지 확인합니다.
     *
     * @return 포트가 열려있으면 true, 아니면 false
     */
    fun isPortOpen(): Boolean = usbPort != null && _isConnected.value
}
