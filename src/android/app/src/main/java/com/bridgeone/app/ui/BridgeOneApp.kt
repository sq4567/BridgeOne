package com.bridgeone.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bridgeone.app.R
import com.bridgeone.app.ui.theme.BridgeOneTheme
import com.bridgeone.app.ui.theme.TextPrimary
import com.bridgeone.app.ui.components.TouchpadWrapper
import com.bridgeone.app.ui.components.KeyboardLayout
import com.bridgeone.app.ui.utils.ClickDetector
import com.bridgeone.app.usb.UsbSerialManager
import com.bridgeone.app.usb.UsbDebugState
import com.bridgeone.app.usb.UsbDeviceInfo
import android.util.Log
import kotlinx.coroutines.delay

/**
 * BridgeOne 앱의 최상위 Composable 함수입니다.
 *
 * 이 함수는 앱의 전체 레이아웃과 테마를 정의합니다.
 * Material3 테마와 Pretendard 폰트가 적용되며, 다크 테마만 지원합니다.
 */
@Composable
fun BridgeOneApp() {
    val context = LocalContext.current
    val debugState by UsbSerialManager.debugState.collectAsState()

    // 디버그 패널 표시 여부 (기본: 표시)
    var showDebugPanel by remember { mutableStateOf(true) }

    // 주기적으로 USB 상태 스캔 (2초마다)
    LaunchedEffect(Unit) {
        while (true) {
            UsbSerialManager.scanAndUpdateDebugState(context)
            delay(2000L)
        }
    }

    // 전체 화면을 채우는 배경
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 메인 콘텐츠 (하단 정렬)
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            MainContent()
        }

        // 디버그 패널 (상단)
        if (showDebugPanel) {
            UsbDebugPanel(
                debugState = debugState,
                onClose = { showDebugPanel = false },
                onRefresh = { UsbSerialManager.scanAndUpdateDebugState(context) },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 32.dp)
            )
        } else {
            // 디버그 패널 열기 버튼 (접었을 때)
            Text(
                text = "🔍 USB Debug",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 40.dp, end = 16.dp)
                    .clickable { showDebugPanel = true }
            )
        }
    }
}

/**
 * 앱의 메인 콘텐츠를 렌더링합니다.
 *
 * 중앙 하단 영역에 터치패드(160×320dp)와 키보드(240×280dp)를 배치합니다.
 * 터치패드는 마우스 입력, 키보드는 키 입력을 처리합니다.
 */
@Composable
private fun MainContent() {
    // 활성 키 상태 관리 (키보드의 다중 입력 시각화용)
    val activeKeys = remember { mutableStateOf(setOf<UByte>()) }
    
    // 활성 수정자 키 추적 (BridgeFrame 생성용)
    // Phase 2.2.4.3: 수정자 키 상태 관리 최적화
    val activeModifierKeys = remember { mutableStateOf(setOf<UByte>()) }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 메인 입력 영역: 터치패드 (좌측) + 키보드 (우측) 구성
        // 중앙 하단 배치: 가로 410dp (160 + 250) × 세로 320dp
        Row(
            modifier = Modifier,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // 터치패드 (좌측): 160×320dp
            TouchpadWrapper(
                modifier = Modifier
            )
            
            // 키보드 (우측): 240×280dp
            KeyboardLayout(
                onKeyPressed = { keyCode ->
                    activeKeys.value = activeKeys.value + keyCode
                    
                    // Phase 2.2.4.3: 수정자 키인 경우 activeModifierKeys에도 추가
                    if (keyCode == 0x01.toUByte() ||  // LEFT_CTRL
                        keyCode == 0x02.toUByte() ||  // LEFT_SHIFT
                        keyCode == 0x04.toUByte() ||  // LEFT_ALT
                        keyCode == 0x08.toUByte()) {  // LEFT_GUI
                        activeModifierKeys.value = activeModifierKeys.value + keyCode
                    } else {
                        // 일반 키인 경우, 현재 활성 수정자 키와 함께 프레임 생성 및 전송
                        try {
                            val frame = ClickDetector.createKeyboardFrame(
                                activeModifierKeys = activeModifierKeys.value,
                                keyCode1 = keyCode,
                                keyCode2 = 0u
                            )
                            ClickDetector.sendFrame(frame)
                            Log.d("MainContent", "Keyboard frame sent: keyCode=0x${keyCode.toString(16)}, modifiers=0x${activeModifierKeys.value}")
                        } catch (e: Exception) {
                            Log.e("MainContent", "Failed to send keyboard frame: ${e.message}", e)
                        }
                    }
                },
                onKeyReleased = { keyCode ->
                    activeKeys.value = activeKeys.value - keyCode
                    
                    // Phase 2.2.4.3: 수정자 키인 경우 activeModifierKeys에서 제거
                    if (keyCode == 0x01.toUByte() ||  // LEFT_CTRL
                        keyCode == 0x02.toUByte() ||  // LEFT_SHIFT
                        keyCode == 0x04.toUByte() ||  // LEFT_ALT
                        keyCode == 0x08.toUByte()) {  // LEFT_GUI
                        activeModifierKeys.value = activeModifierKeys.value - keyCode
                    } else {
                        // 일반 키 해제 시에도 프레임 전송 (keyCode=0으로 설정)
                        // 이를 통해 PC에서 키 해제를 인식
                        try {
                            val frame = ClickDetector.createKeyboardFrame(
                                activeModifierKeys = activeModifierKeys.value,
                                keyCode1 = 0u,  // 키 해제 표시
                                keyCode2 = 0u
                            )
                            ClickDetector.sendFrame(frame)
                            Log.d("MainContent", "Keyboard key-release frame sent: modifiers=0x${activeModifierKeys.value}")
                        } catch (e: Exception) {
                            Log.e("MainContent", "Failed to send keyboard key-release frame: ${e.message}", e)
                        }
                    }
                },
                activeKeys = activeKeys.value
            )
        }
        
        // 하단 여백
        Spacer(modifier = Modifier.size(16.dp))
    }
}


@Preview(showBackground = true)
@Composable
private fun BridgeOneAppPreview() {
    BridgeOneTheme {
        BridgeOneApp()
    }
}

// ========== USB 디버그 패널 (임시) ==========

/**
 * USB 디버그 정보를 표시하는 패널.
 * 연결된 USB 장치 목록과 연결 상태를 실시간으로 표시합니다.
 */
@Composable
private fun UsbDebugPanel(
    debugState: UsbDebugState,
    onClose: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(
                color = Color(0xFF1E1E2E),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = Color(0xFF3E3E5E),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        // 헤더
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🔌 USB Debug Panel",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "🔄",
                    fontSize = 18.sp,
                    modifier = Modifier.clickable { onRefresh() }
                )
                Text(
                    text = "✕",
                    color = Color.Gray,
                    fontSize = 18.sp,
                    modifier = Modifier.clickable { onClose() }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 연결 상태
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "상태:",
                color = Color.Gray,
                fontSize = 14.sp
            )
            Text(
                text = debugState.connectionStatus,
                color = when {
                    debugState.isConnected -> Color(0xFF4CAF50)  // 녹색
                    debugState.targetDevice != null -> Color(0xFFFFEB3B)  // 노란색
                    else -> Color(0xFFFF5722)  // 주황색
                },
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // 에러 메시지
        debugState.lastError?.let { error ->
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "⚠️ $error",
                color = Color(0xFFFF5722),
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 타겟 VID/PID 정보
        Text(
            text = "찾는 장치: CH343P (VID=0x1A86, PID=0x55D3)",
            color = Color(0xFF888888),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 발견된 장치 목록
        Text(
            text = "발견된 USB 장치 (${debugState.allDevices.size}개):",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (debugState.allDevices.isEmpty()) {
            Text(
                text = "연결된 USB 장치가 없습니다",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 8.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.height(120.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(debugState.allDevices) { device ->
                    UsbDeviceItem(device = device)
                }
            }
        }
    }
}

/**
 * USB 장치 항목 표시
 */
@Composable
private fun UsbDeviceItem(device: UsbDeviceInfo) {
    val backgroundColor = if (device.isTarget) Color(0xFF2E4A2E) else Color(0xFF2A2A3A)
    val borderColor = if (device.isTarget) Color(0xFF4CAF50) else Color.Transparent

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .border(
                width = if (device.isTarget) 1.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (device.isTarget) "✓ TARGET" else device.deviceName,
                color = if (device.isTarget) Color(0xFF4CAF50) else Color.White,
                fontSize = 12.sp,
                fontWeight = if (device.isTarget) FontWeight.Bold else FontWeight.Normal
            )
            Text(
                text = "${device.vidHex}:${device.pidHex}",
                color = Color(0xFFAADDFF),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        device.productName?.let { name ->
            Text(
                text = name,
                color = Color.Gray,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        device.manufacturerName?.let { manufacturer ->
            Text(
                text = "제조사: $manufacturer",
                color = Color(0xFF666666),
                fontSize = 10.sp
            )
        }
    }
}
