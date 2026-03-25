package com.bridgeone.app.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import android.util.Log
import com.bridgeone.app.protocol.BridgeMode
import com.bridgeone.app.ui.common.*
import com.bridgeone.app.ui.components.KeyboardKeyButton
import com.bridgeone.app.ui.components.TouchpadWrapper
import com.bridgeone.app.ui.utils.ClickDetector

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
fun EssentialModePage() {
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
