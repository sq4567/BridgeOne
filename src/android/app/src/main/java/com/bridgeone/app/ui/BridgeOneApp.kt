package com.bridgeone.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import android.content.Context
import android.os.Build
import android.os.Vibrator
import android.os.VibrationEffect
import androidx.compose.ui.draw.scale
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.MutableState
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
 * 버튼을 사용하여 터치패드와 키보드를 전환합니다.
 * 사용자는 하단의 전환 버튼을 터치하여 두 입력 방식을 전환할 수 있습니다.
 *
 * Phase 2.4: 버튼 기반 UI 전환
 */
@Composable
private fun MainContent() {
    // 현재 표시 모드 상태 (0: 터치패드, 1: 키보드)
    var currentMode by remember { mutableStateOf(0) }

    // 활성 키 상태 관리 (키보드의 다중 입력 시각화용)
    val activeKeys = remember { mutableStateOf(setOf<UByte>()) }

    // 활성 수정자 키 추적 (BridgeFrame 생성용)
    val activeModifierKeys = remember { mutableStateOf(setOf<UByte>()) }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 메인 콘텐츠 영역 (위쪽)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter
        ) {
            when (currentMode) {
                0 -> TouchpadPage()
                1 -> KeyboardPage(activeKeys, activeModifierKeys)
            }
        }

        // 전환 버튼 (하단)
        Button(
            onClick = {
                currentMode = 1 - currentMode  // 0 ↔ 1 전환

                // 햅틱 피드백: 50ms 진동
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(50)
                }

                // 로그
                val message = when (currentMode) {
                    0 -> "터치패드 페이지로 전환"
                    1 -> "키보드 페이지로 전환"
                    else -> ""
                }
                Log.d("MainContent", message)
            },
            modifier = Modifier
                .padding(16.dp)
                .size(width = 60.dp, height = 60.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2196F3)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = if (currentMode == 0) "⌨️" else "🖱️",
                fontSize = 28.sp
            )
        }

        // 페이지 표시기 (현재 모드 텍스트)
        Text(
            text = if (currentMode == 0) "터치패드" else "키보드",
            color = Color(0xFFC2C2C2),
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )
    }
}

/**
 * 터치패드 페이지
 *
 * 터치패드를 화면 최대 크기로 표시합니다.
 * 1:2 비율을 유지하며 화면 폭의 90%를 사용합니다.
 */
@Composable
private fun TouchpadPage() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        TouchpadWrapper(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .aspectRatio(0.5f)  // 1:2 비율 (가로:세로)
        )
    }
}

/**
 * 키보드 페이지
 *
 * 키보드를 화면 최대 크기로 표시합니다.
 * 화면 폭 전체를 사용하고 약간 확대된 크기로 표시합니다.
 */
@Composable
private fun KeyboardPage(
    activeKeys: MutableState<Set<UByte>>,
    activeModifierKeys: MutableState<Set<UByte>>
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        KeyboardLayout(
            onKeyPressed = { keyCode ->
                activeKeys.value = activeKeys.value + keyCode

                // 수정자 키인 경우
                if (keyCode == 0x01.toUByte() ||  // LEFT_CTRL
                    keyCode == 0x02.toUByte() ||  // LEFT_SHIFT
                    keyCode == 0x04.toUByte() ||  // LEFT_ALT
                    keyCode == 0x08.toUByte()) {  // LEFT_GUI
                    activeModifierKeys.value = activeModifierKeys.value + keyCode
                } else {
                    // 일반 키인 경우, 프레임 생성 및 전송
                    try {
                        val frame = ClickDetector.createKeyboardFrame(
                            activeModifierKeys = activeModifierKeys.value,
                            keyCode1 = keyCode,
                            keyCode2 = 0u
                        )
                        ClickDetector.sendFrame(frame)
                        Log.d("KeyboardPage", "Keyboard frame sent: keyCode=0x${keyCode.toString(16)}, modifiers=0x${activeModifierKeys.value}")
                    } catch (e: Exception) {
                        Log.e("KeyboardPage", "Failed to send keyboard frame: ${e.message}", e)
                    }
                }
            },
            onKeyReleased = { keyCode ->
                activeKeys.value = activeKeys.value - keyCode

                // 수정자 키인 경우
                if (keyCode == 0x01.toUByte() ||  // LEFT_CTRL
                    keyCode == 0x02.toUByte() ||  // LEFT_SHIFT
                    keyCode == 0x04.toUByte() ||  // LEFT_ALT
                    keyCode == 0x08.toUByte()) {  // LEFT_GUI
                    activeModifierKeys.value = activeModifierKeys.value - keyCode
                } else {
                    // 일반 키 해제 시 프레임 전송
                    try {
                        val frame = ClickDetector.createKeyboardFrame(
                            activeModifierKeys = activeModifierKeys.value,
                            keyCode1 = 0u,  // 키 해제 표시
                            keyCode2 = 0u
                        )
                        ClickDetector.sendFrame(frame)
                        Log.d("KeyboardPage", "Keyboard key-release frame sent: modifiers=0x${activeModifierKeys.value}")
                    } catch (e: Exception) {
                        Log.e("KeyboardPage", "Failed to send keyboard key-release frame: ${e.message}", e)
                    }
                }
            },
            activeKeys = activeKeys.value,
            modifier = Modifier
                .fillMaxWidth()
                .scale(1.2f)  // 약간 확대
        )
    }
}

/**
 * 페이지 인디케이터
 *
 * 상단 중앙에 현재 페이지를 나타내는 닷 표시합니다.
 */
@Composable
private fun PageIndicator(
    currentPage: Int,
    pageCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(pageCount) { index ->
            val size by animateFloatAsState(
                targetValue = if (currentPage == index) 12f else 8f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "PageIndicatorSize"
            )

            Box(
                modifier = Modifier
                    .size(size.dp)
                    .background(
                        color = if (currentPage == index) {
                            Color(0xFF2196F3)  // 파란색 (Selected)
                        } else {
                            Color(0xFFC2C2C2).copy(alpha = 0.6f)  // 회색 60% (Unselected)
                        },
                        shape = CircleShape
                    )
            )
        }
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
