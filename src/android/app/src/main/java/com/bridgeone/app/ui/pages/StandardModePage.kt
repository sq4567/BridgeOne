package com.bridgeone.app.ui.pages

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.util.Log
import com.bridgeone.app.protocol.BridgeMode
import com.bridgeone.app.ui.common.*
import com.bridgeone.app.ui.components.KeyboardKeyButton
import com.bridgeone.app.ui.components.KeyboardLayout
import com.bridgeone.app.ui.components.TouchpadWrapper
import com.bridgeone.app.ui.utils.ClickDetector

// ╔════════════════════════════════════════════════════════════╗
// ║  ⚠️ PROTOTYPE — 이 파일은 임시 프로토타입입니다.            ║
// ║                                                            ║
// ║  Standard 모드의 최종 디자인이 확정되지 않은 상태에서       ║
// ║  기본 동작 검증을 위해 작성된 임시 구현입니다.              ║
// ║  향후 디자인 확정 시 전면 재작성될 예정입니다.              ║
// ║                                                            ║
// ║  - 레이아웃: Essential과 유사한 2열 구조 (임시)             ║
// ║  - 컨트롤 패널: 휠/우클릭 추가 (임시 배치)                 ║
// ║  - 키보드 뷰: 기존 3탭 키보드 레이아웃 재사용              ║
// ╚════════════════════════════════════════════════════════════╝

// ============================================================
// Standard 모드 페이지 (프로토타입)
// ============================================================

/**
 * Standard 모드 페이지 (프로토타입)
 *
 * ⚠️ 임시 구현 — 최종 디자인 확정 후 전면 재작성 예정
 *
 * Essential과 유사한 2열 구조 + 하단 키보드 전환 버튼
 * - 터치패드 뷰: 좌측 터치패드(72%) + 우측 컨트롤 패널(28%)
 * - 키보드 뷰: 전체 3탭 키보드 레이아웃
 */
@Composable
fun StandardModePage() {
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

// ============================================================
// Standard 컨트롤 패널 (프로토타입)
// ============================================================

/**
 * Standard 모드 컨트롤 패널 (프로토타입)
 *
 * ⚠️ 임시 구현 — Essential Boot Cluster와 유사한 구조에 휠/우클릭 추가
 *
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

// ============================================================
// Standard 전용 컴포넌트 (프로토타입)
// ============================================================

/**
 * Standard 전용 액션 버튼 (휠, 우클릭 등)
 *
 * ⚠️ 임시 구현
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
// 키보드 페이지 (Standard 모드 전용, 프로토타입)
// ============================================================

/**
 * 키보드 페이지 (Standard 모드 전체 3탭 키보드)
 *
 * ⚠️ 임시 구현
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
