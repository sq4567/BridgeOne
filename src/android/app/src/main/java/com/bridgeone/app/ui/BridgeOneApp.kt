package com.bridgeone.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import android.content.Context
import android.os.Build
import android.os.Vibrator
import android.os.VibrationEffect
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import android.widget.Toast
import com.bridgeone.app.protocol.BridgeMode
import com.bridgeone.app.ui.theme.BridgeOneTheme
import com.bridgeone.app.ui.components.TouchpadWrapper
import com.bridgeone.app.ui.components.KeyboardLayout
import com.bridgeone.app.ui.components.KeyboardKeyButton
import com.bridgeone.app.ui.utils.ClickDetector
import com.bridgeone.app.usb.UsbSerialManager
import com.bridgeone.app.usb.UsbDebugState
import com.bridgeone.app.usb.UsbDeviceInfo
import android.util.Log
import kotlinx.coroutines.delay

// ============================================================
// HID 키 코드 상수 (Essential/Standard 공용)
// ============================================================
private val KEY_ENTER = 0x28.toUByte()
private val KEY_ESC = 0x29.toUByte()
private val KEY_DELETE = 0x4C.toUByte()
private val KEY_F1 = 0x3A.toUByte(); private val KEY_F2 = 0x3B.toUByte()
private val KEY_F3 = 0x3C.toUByte(); private val KEY_F4 = 0x3D.toUByte()
private val KEY_F5 = 0x3E.toUByte(); private val KEY_F6 = 0x3F.toUByte()
private val KEY_F7 = 0x40.toUByte(); private val KEY_F8 = 0x41.toUByte()
private val KEY_F9 = 0x42.toUByte(); private val KEY_F10 = 0x43.toUByte()
private val KEY_F11 = 0x44.toUByte(); private val KEY_F12 = 0x45.toUByte()
private val KEY_RIGHT = 0x4F.toUByte()
private val KEY_LEFT = 0x50.toUByte()
private val KEY_DOWN = 0x51.toUByte()
private val KEY_UP = 0x52.toUByte()

// ============================================================
// 최상위 Composable
// ============================================================

/**
 * BridgeOne 앱의 최상위 Composable 함수입니다.
 */
@Composable
fun BridgeOneApp() {
    val context = LocalContext.current
    val debugState by UsbSerialManager.debugState.collectAsState()
    val bridgeMode by UsbSerialManager.bridgeMode.collectAsState()

    var showDebugPanel by remember { mutableStateOf(true) }

    // Phase 3.5.5: 브릿지 모드 전환 시 토스트 알림
    var isFirstMode by remember { mutableStateOf(true) }
    LaunchedEffect(bridgeMode) {
        if (isFirstMode) {
            isFirstMode = false
            return@LaunchedEffect
        }
        val message = when (bridgeMode) {
            BridgeMode.STANDARD -> "Standard 모드로 전환되었습니다"
            BridgeMode.ESSENTIAL -> "Essential 모드로 전환되었습니다"
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        Log.i("BridgeOneApp", "BridgeMode toast: $message")
    }

    // 주기적으로 USB 상태 스캔 (2초마다)
    LaunchedEffect(Unit) {
        while (true) {
            UsbSerialManager.scanAndUpdateDebugState(context)
            delay(2000L)
        }
    }

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

// ============================================================
// MainContent: 모드별 페이지 분기
// ============================================================

@Composable
private fun MainContent() {
    val bridgeMode by UsbSerialManager.bridgeMode.collectAsState()

    when (bridgeMode) {
        BridgeMode.ESSENTIAL -> EssentialModePage()
        BridgeMode.STANDARD -> StandardModePage()
    }
}

// ============================================================
// Essential 모드 페이지
// ============================================================

/**
 * Essential 모드 페이지 (스타일프레임 문서 기반)
 *
 * 2열 구조: 좌측 터치패드(72%) + 우측 Boot Keyboard Cluster(28%)
 * 전환 버튼 없음, 단일 뷰로 구성
 */
@Composable
private fun EssentialModePage() {
    val activeKeys = remember { mutableStateOf(setOf<UByte>()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.Bottom
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f),  // 화면 하단 75%만 사용, 상단 25% 여백
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 좌측: 터치패드 (72%)
            TouchpadWrapper(
                bridgeMode = BridgeMode.ESSENTIAL,
                modifier = Modifier
                    .weight(0.72f)
                    .fillMaxHeight()
            )

            // 우측: Boot Keyboard Cluster (28%)
            EssentialBootCluster(
                activeKeys = activeKeys,
                modifier = Modifier
                    .weight(0.28f)
                    .fillMaxHeight()
            )
        }
    }
}

/**
 * Essential Boot Keyboard Cluster (스타일프레임 §2.2 기반)
 *
 * 레이아웃:
 * ```
 * [Del] ⚡ (Essential 진입용, 최상단 강조)
 * [F1-F12]  (컨테이너 버튼 → 팝업 3×4)
 * [Esc] [Enter]
 *       [↑]
 *   [←] [↓] [→]
 * ```
 */
@Composable
private fun EssentialBootCluster(
    activeKeys: MutableState<Set<UByte>>,
    modifier: Modifier = Modifier
) {
    var showFKeyPopup by remember { mutableStateOf(false) }

    // 키 전송 콜백
    val onKeyPressed: (UByte) -> Unit = { keyCode ->
        activeKeys.value = activeKeys.value + keyCode
        try {
            val frame = ClickDetector.createKeyboardFrame(
                activeModifierKeys = emptySet(),
                keyCode1 = keyCode
            )
            ClickDetector.sendFrame(frame)
        } catch (e: Exception) {
            Log.e("EssentialBootCluster", "Failed to send key: ${e.message}", e)
        }
    }

    val onKeyReleased: (UByte) -> Unit = { keyCode ->
        activeKeys.value = activeKeys.value - keyCode
        try {
            val frame = ClickDetector.createKeyboardFrame(
                activeModifierKeys = emptySet(),
                keyCode1 = 0u
            )
            ClickDetector.sendFrame(frame)
        } catch (e: Exception) {
            Log.e("EssentialBootCluster", "Failed to send release: ${e.message}", e)
        }
    }

    Column(
        modifier = modifier
            .background(Color(0xFF121212), RoundedCornerShape(12.dp))
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // [Del] — Essential 진입용 강조 (최상단)
        KeyboardKeyButton(
            keyLabel = "⚡Del",
            keyCode = KEY_DELETE,
            isActive = KEY_DELETE in activeKeys.value,
            onKeyPressed = { onKeyPressed(KEY_DELETE) },
            onKeyReleased = { onKeyReleased(KEY_DELETE) },
            modifier = Modifier.fillMaxWidth().weight(1.2f)
        )

        // [F1-F12] 컨테이너 버튼 (탭 → 팝업)
        Button(
            onClick = { showFKeyPopup = true },
            modifier = Modifier.fillMaxWidth().weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
            shape = RoundedCornerShape(6.dp)
        ) {
            Text("F1-F12", fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }

        // [Esc] [Enter] 행
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            KeyboardKeyButton(
                keyLabel = "Esc", keyCode = KEY_ESC,
                isActive = KEY_ESC in activeKeys.value,
                onKeyPressed = { onKeyPressed(KEY_ESC) },
                onKeyReleased = { onKeyReleased(KEY_ESC) },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            KeyboardKeyButton(
                keyLabel = "Enter", keyCode = KEY_ENTER,
                isActive = KEY_ENTER in activeKeys.value,
                onKeyPressed = { onKeyPressed(KEY_ENTER) },
                onKeyReleased = { onKeyReleased(KEY_ENTER) },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }

        // D-Pad: [　][↑][　]
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Spacer(Modifier.weight(1f))
            KeyboardKeyButton(
                keyLabel = "↑", keyCode = KEY_UP,
                isActive = KEY_UP in activeKeys.value,
                onKeyPressed = { onKeyPressed(KEY_UP) },
                onKeyReleased = { onKeyReleased(KEY_UP) },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            Spacer(Modifier.weight(1f))
        }

        // D-Pad: [←][↓][→]
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            KeyboardKeyButton(
                keyLabel = "←", keyCode = KEY_LEFT,
                isActive = KEY_LEFT in activeKeys.value,
                onKeyPressed = { onKeyPressed(KEY_LEFT) },
                onKeyReleased = { onKeyReleased(KEY_LEFT) },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            KeyboardKeyButton(
                keyLabel = "↓", keyCode = KEY_DOWN,
                isActive = KEY_DOWN in activeKeys.value,
                onKeyPressed = { onKeyPressed(KEY_DOWN) },
                onKeyReleased = { onKeyReleased(KEY_DOWN) },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            KeyboardKeyButton(
                keyLabel = "→", keyCode = KEY_RIGHT,
                isActive = KEY_RIGHT in activeKeys.value,
                onKeyPressed = { onKeyPressed(KEY_RIGHT) },
                onKeyReleased = { onKeyReleased(KEY_RIGHT) },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }
    }

    // F1-F12 팝업 다이얼로그 (3×4 그리드)
    if (showFKeyPopup) {
        FKeyPopupDialog(
            activeKeys = activeKeys.value,
            onKeyTap = { keyCode ->
                onKeyPressed(keyCode)
                onKeyReleased(keyCode)
                showFKeyPopup = false  // 키 입력 후 자동 닫힘
            },
            onDismiss = { showFKeyPopup = false }
        )
    }
}

/**
 * F1-F12 팝업 다이얼로그 (스타일프레임 §2.2 기반)
 *
 * 3×4 그리드로 표시. 키 탭 → 해당 키 입력 → 팝업 자동 닫힘.
 */
@Composable
private fun FKeyPopupDialog(
    activeKeys: Set<UByte>,
    onKeyTap: (UByte) -> Unit,
    onDismiss: () -> Unit
) {
    val fKeys = listOf(
        "F1" to KEY_F1, "F2" to KEY_F2, "F3" to KEY_F3,
        "F4" to KEY_F4, "F5" to KEY_F5, "F6" to KEY_F6,
        "F7" to KEY_F7, "F8" to KEY_F8, "F9" to KEY_F9,
        "F10" to KEY_F10, "F11" to KEY_F11, "F12" to KEY_F12
    )

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .background(Color(0xFF1E1E2E), RoundedCornerShape(12.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                "Function Keys",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))

            // 3×4 그리드 (F1-F3 / F4-F6 / F7-F9 / F10-F12)
            fKeys.chunked(3).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    row.forEach { (label, keyCode) ->
                        KeyboardKeyButton(
                            keyLabel = label,
                            keyCode = keyCode,
                            isActive = keyCode in activeKeys,
                            onKeyPressed = { onKeyTap(keyCode) },
                            onKeyReleased = { /* 자동 닫힘이므로 별도 해제 불필요 */ },
                            modifier = Modifier.weight(1f).height(48.dp)
                        )
                    }
                }
            }
        }
    }
}

// ============================================================
// Standard 모드 페이지 (프로토타입)
// ============================================================

/**
 * Standard 모드 페이지 (프로토타입)
 *
 * Essential과 유사한 2열 구조 + 하단 키보드 전환 버튼
 * - 터치패드 뷰: 좌측 터치패드(72%) + 우측 컨트롤 패널(28%)
 * - 키보드 뷰: 전체 3탭 키보드 레이아웃
 */
@Composable
private fun StandardModePage() {
    var showKeyboard by remember { mutableStateOf(false) }
    val activeKeys = remember { mutableStateOf(setOf<UByte>()) }
    val activeModifierKeys = remember { mutableStateOf(setOf<UByte>()) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 메인 콘텐츠 영역
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f),  // 화면 하단 75%만 사용, 상단 25% 여백
            contentAlignment = Alignment.BottomCenter
        ) {
            if (!showKeyboard) {
                // 터치패드 뷰: 2열 구조
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 좌측: 터치패드 (72%)
                    TouchpadWrapper(
                        bridgeMode = BridgeMode.STANDARD,
                        modifier = Modifier
                            .weight(0.72f)
                            .fillMaxHeight()
                    )

                    // 우측: Standard 컨트롤 패널 (28%)
                    StandardControlPanel(
                        activeKeys = activeKeys,
                        modifier = Modifier
                            .weight(0.28f)
                            .fillMaxHeight()
                    )
                }
            } else {
                // 키보드 뷰: 전체 3탭 레이아웃
                KeyboardPage(
                    bridgeMode = BridgeMode.STANDARD,
                    activeKeys = activeKeys,
                    activeModifierKeys = activeModifierKeys
                )
            }
        }

        // 하단: 전환 버튼
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                showKeyboard = !showKeyboard
                triggerHaptic(context)
                Log.d("StandardModePage", if (showKeyboard) "키보드 전환" else "터치패드 전환")
            },
            modifier = Modifier.size(width = 56.dp, height = 56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = if (!showKeyboard) "⌨️" else "🖱️",
                fontSize = 24.sp
            )
        }
        Text(
            text = if (!showKeyboard) "터치패드" else "키보드",
            color = Color(0xFFC2C2C2),
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
    }
}

/**
 * Standard 모드 컨트롤 패널 (프로토타입)
 *
 * Essential Boot Cluster와 유사한 구조에 휠/우클릭 추가:
 * ```
 * [▲ Scroll]
 * [RC Right Click]
 * [▼ Scroll]
 * ─────────────
 * [Esc] [Enter]
 *       [↑]
 *   [←] [↓] [→]
 * ```
 */
@Composable
private fun StandardControlPanel(
    activeKeys: MutableState<Set<UByte>>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // 키 전송 콜백
    val onKeyPressed: (UByte) -> Unit = { keyCode ->
        activeKeys.value = activeKeys.value + keyCode
        try {
            val frame = ClickDetector.createKeyboardFrame(
                activeModifierKeys = emptySet(),
                keyCode1 = keyCode
            )
            ClickDetector.sendFrame(frame)
        } catch (e: Exception) {
            Log.e("StandardControlPanel", "Failed to send key: ${e.message}", e)
        }
    }

    val onKeyReleased: (UByte) -> Unit = { keyCode ->
        activeKeys.value = activeKeys.value - keyCode
        try {
            val frame = ClickDetector.createKeyboardFrame(
                activeModifierKeys = emptySet(),
                keyCode1 = 0u
            )
            ClickDetector.sendFrame(frame)
        } catch (e: Exception) {
            Log.e("StandardControlPanel", "Failed to send release: ${e.message}", e)
        }
    }

    Column(
        modifier = modifier
            .background(Color(0xFF121212), RoundedCornerShape(12.dp))
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // ── Standard 전용: 휠 + 우클릭 ──

        // [▲ Scroll Up]
        StandardActionButton(
            label = "▲",
            sublabel = "Scroll",
            onClick = {
                triggerHaptic(context)
                ClickDetector.sendFrame(ClickDetector.createWheelFrame(1))
            },
            modifier = Modifier.fillMaxWidth().weight(1f)
        )

        // [RC Right Click]
        StandardActionButton(
            label = "RC",
            sublabel = "Right",
            onClick = {
                triggerHaptic(context)
                ClickDetector.sendFrame(ClickDetector.createRightClickFrame(pressed = true))
                ClickDetector.sendFrame(ClickDetector.createRightClickFrame(pressed = false))
            },
            modifier = Modifier.fillMaxWidth().weight(1f),
            containerColor = Color(0xFF2E4A2E)
        )

        // [▼ Scroll Down]
        StandardActionButton(
            label = "▼",
            sublabel = "Scroll",
            onClick = {
                triggerHaptic(context)
                ClickDetector.sendFrame(ClickDetector.createWheelFrame(-1))
            },
            modifier = Modifier.fillMaxWidth().weight(1f)
        )

        // ── 구분선 ──
        Spacer(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFF333333))
        )

        // ── 공통: 기본 키 + D-Pad ──

        // [Esc] [Enter]
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            KeyboardKeyButton(
                keyLabel = "Esc", keyCode = KEY_ESC,
                isActive = KEY_ESC in activeKeys.value,
                onKeyPressed = { onKeyPressed(KEY_ESC) },
                onKeyReleased = { onKeyReleased(KEY_ESC) },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            KeyboardKeyButton(
                keyLabel = "Enter", keyCode = KEY_ENTER,
                isActive = KEY_ENTER in activeKeys.value,
                onKeyPressed = { onKeyPressed(KEY_ENTER) },
                onKeyReleased = { onKeyReleased(KEY_ENTER) },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }

        // D-Pad: [　][↑][　]
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Spacer(Modifier.weight(1f))
            KeyboardKeyButton(
                keyLabel = "↑", keyCode = KEY_UP,
                isActive = KEY_UP in activeKeys.value,
                onKeyPressed = { onKeyPressed(KEY_UP) },
                onKeyReleased = { onKeyReleased(KEY_UP) },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            Spacer(Modifier.weight(1f))
        }

        // D-Pad: [←][↓][→]
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            KeyboardKeyButton(
                keyLabel = "←", keyCode = KEY_LEFT,
                isActive = KEY_LEFT in activeKeys.value,
                onKeyPressed = { onKeyPressed(KEY_LEFT) },
                onKeyReleased = { onKeyReleased(KEY_LEFT) },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            KeyboardKeyButton(
                keyLabel = "↓", keyCode = KEY_DOWN,
                isActive = KEY_DOWN in activeKeys.value,
                onKeyPressed = { onKeyPressed(KEY_DOWN) },
                onKeyReleased = { onKeyReleased(KEY_DOWN) },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            KeyboardKeyButton(
                keyLabel = "→", keyCode = KEY_RIGHT,
                isActive = KEY_RIGHT in activeKeys.value,
                onKeyPressed = { onKeyPressed(KEY_RIGHT) },
                onKeyReleased = { onKeyReleased(KEY_RIGHT) },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }
    }
}

/**
 * Standard 전용 액션 버튼 (휠, 우클릭 등)
 */
@Composable
private fun StandardActionButton(
    label: String,
    sublabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = Color(0xFF2A2A3A)
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        shape = RoundedCornerShape(6.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(text = sublabel, fontSize = 9.sp, color = Color(0xFFA0A0A0))
        }
    }
}

// ============================================================
// 공통 유틸리티
// ============================================================

/**
 * 햅틱 피드백 (50ms 진동)
 */
private fun triggerHaptic(context: Context) {
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(50)
    }
}

/**
 * 수정자 키 코드 판별 (HID Usage 0xE0-0xE7)
 */
private fun isModifierKeyCode(keyCode: UByte): Boolean =
    keyCode.toInt() in 0xE0..0xE7

/**
 * 수정자 키 코드(HID Usage) → HID 수정자 비트 플래그 변환
 */
private fun modifierBitFlag(keyCode: UByte): UByte = when (keyCode.toInt()) {
    0xE0 -> 0x01  // Left Ctrl
    0xE1 -> 0x02  // Left Shift
    0xE2 -> 0x04  // Left Alt
    0xE3 -> 0x08  // Left GUI (Win)
    0xE4 -> 0x10  // Right Ctrl (한자)
    0xE5 -> 0x20  // Right Shift
    0xE6 -> 0x40  // Right Alt (한/영)
    0xE7 -> 0x80  // Right GUI
    else -> 0x00
}.toUByte()

// ============================================================
// 키보드 페이지 (Standard 모드 전용)
// ============================================================

/**
 * 키보드 페이지 (Standard 모드 전체 3탭 키보드)
 */
@Composable
private fun KeyboardPage(
    bridgeMode: BridgeMode,
    activeKeys: MutableState<Set<UByte>>,
    activeModifierKeys: MutableState<Set<UByte>>
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        KeyboardLayout(
            bridgeMode = bridgeMode,
            onKeyPressed = { keyCode ->
                activeKeys.value = activeKeys.value + keyCode

                if (isModifierKeyCode(keyCode)) {
                    activeModifierKeys.value = activeModifierKeys.value + modifierBitFlag(keyCode)
                    try {
                        val frame = ClickDetector.createKeyboardFrame(
                            activeModifierKeys = activeModifierKeys.value,
                            keyCode1 = 0u,
                            keyCode2 = 0u
                        )
                        ClickDetector.sendFrame(frame)
                    } catch (e: Exception) {
                        Log.e("KeyboardPage", "Failed to send modifier frame: ${e.message}", e)
                    }
                } else {
                    try {
                        val frame = ClickDetector.createKeyboardFrame(
                            activeModifierKeys = activeModifierKeys.value,
                            keyCode1 = keyCode,
                            keyCode2 = 0u
                        )
                        ClickDetector.sendFrame(frame)
                    } catch (e: Exception) {
                        Log.e("KeyboardPage", "Failed to send key frame: ${e.message}", e)
                    }
                }
            },
            onKeyReleased = { keyCode ->
                activeKeys.value = activeKeys.value - keyCode

                if (isModifierKeyCode(keyCode)) {
                    activeModifierKeys.value = activeModifierKeys.value - modifierBitFlag(keyCode)
                }

                try {
                    val frame = ClickDetector.createKeyboardFrame(
                        activeModifierKeys = activeModifierKeys.value,
                        keyCode1 = 0u,
                        keyCode2 = 0u
                    )
                    ClickDetector.sendFrame(frame)
                } catch (e: Exception) {
                    Log.e("KeyboardPage", "Failed to send release frame: ${e.message}", e)
                }
            },
            activeKeys = activeKeys.value,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ============================================================
// USB 디버그 패널 (임시)
// ============================================================

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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "상태:", color = Color.Gray, fontSize = 14.sp)
            Text(
                text = debugState.connectionStatus,
                color = when {
                    debugState.isConnected -> Color(0xFF4CAF50)
                    debugState.targetDevice != null -> Color(0xFFFFEB3B)
                    else -> Color(0xFFFF5722)
                },
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        debugState.lastError?.let { error ->
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "⚠️ $error",
                color = Color(0xFFFF5722),
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "찾는 장치: CH343P (VID=0x1A86, PID=0x55D3)",
            color = Color(0xFF888888),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(12.dp))

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

// ============================================================
// Preview
// ============================================================

@Preview(showBackground = true)
@Composable
private fun BridgeOneAppPreview() {
    BridgeOneTheme {
        BridgeOneApp()
    }
}
