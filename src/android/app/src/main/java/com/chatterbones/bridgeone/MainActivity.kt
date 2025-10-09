package com.chatterbones.bridgeone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chatterbones.bridgeone.ui.theme.BridgeOneTheme
import com.chatterbones.bridgeone.usb.UsbConnectionManager
import com.chatterbones.bridgeone.usb.UsbConnectionState

/**
 * BridgeOne 메인 액티비티
 * 
 * USB 연결 관리 및 앱의 주요 UI를 담당합니다.
 * 
 * @see [technical-specification-app.md §1.1.2 연결 관리 요구사항]
 */
class MainActivity : ComponentActivity() {
    
    // USB 연결 관리자
    private lateinit var usbConnectionManager: UsbConnectionManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // USB 연결 관리자 초기화
        usbConnectionManager = UsbConnectionManager(applicationContext)
        usbConnectionManager.initialize()
        
        enableEdgeToEdge()
        setContent {
            BridgeOneTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    UsbConnectionScreen(
                        usbConnectionManager = usbConnectionManager,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // USB 연결 관리자 정리
        usbConnectionManager.cleanup()
    }
}

/**
 * USB 연결 상태를 표시하는 화면
 * 
 * @param usbConnectionManager USB 연결 관리자
 * @param modifier Compose Modifier
 */
@Composable
fun UsbConnectionScreen(
    usbConnectionManager: UsbConnectionManager,
    modifier: Modifier = Modifier
) {
    // USB 연결 상태 구독
    val connectionState by usbConnectionManager.connectionState.collectAsState()
    
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 연결 상태 표시
        Text(
            text = when (connectionState) {
                is UsbConnectionState.Disconnected -> "USB 장치가 연결되지 않았습니다"
                is UsbConnectionState.Requesting -> "USB 권한 요청 중..."
                is UsbConnectionState.Connected -> {
                    val deviceName = (connectionState as UsbConnectionState.Connected).deviceName
                    "USB 연결 완료: $deviceName"
                }
                is UsbConnectionState.Error -> {
                    val errorMessage = (connectionState as UsbConnectionState.Error).message
                    "오류: $errorMessage"
                }
            },
            modifier = Modifier.padding(16.dp)
        )
        
        // 재스캔 버튼
        Button(
            onClick = { usbConnectionManager.scanAndRequestPermission() },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("USB 장치 스캔")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    BridgeOneTheme {
        Text("BridgeOne Preview")
    }
}