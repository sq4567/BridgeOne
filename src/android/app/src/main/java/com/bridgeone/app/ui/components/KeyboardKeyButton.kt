package com.bridgeone.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.util.Log

/**
 * KeyboardKeyButton 컴포넌트
 *
 * 단일 키 입력을 담당하는 버튼으로, 탭 시 KEY_DOWN/KEY_UP 신호를 전송합니다.
 * Sticky Hold 기능을 지원하여 길게 누르기로 키를 유지할 수 있습니다.
 *
 * **기능**:
 * - 기본 탭: KEY_DOWN → KEY_UP 즉시 전송
 * - 롱프레스 (500ms): Sticky Hold 모드 진입, 다음 탭에서 해제
 * - 시각적 피드백: 누르기 상태에 따른 배경색/테두리 변화
 *
 * **구조**:
 * - 단일 Box + clickable (중첩 Button 없음, 내부 사각형 패턴 제거)
 * - InteractionSource로 누르기 상태 추적
 *
 * @param keyLabel 버튼에 표시될 키 레이블 (예: "Shift", "Ctrl", "A")
 * @param keyCode HID 키 코드 (0x00~0xFF)
 * @param modifier 외부에서 크기를 지정하는 Modifier (내부에서 크기를 결정하지 않음)
 * @param isEnabled 버튼 사용 가능 여부 (기본값: true)
 * @param isActive 다중 입력 활성화 상태
 * @param onKeyPressed 키 누르기 시 호출되는 콜백
 * @param onKeyReleased 키 해제 시 호출되는 콜백
 */
@Composable
fun KeyboardKeyButton(
    keyLabel: String,
    keyCode: UByte,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    isActive: Boolean = false,
    onKeyPressed: ((keyCode: UByte) -> Unit)? = null,
    onKeyReleased: ((keyCode: UByte) -> Unit)? = null
) {
    var isStickyLatched by remember { mutableStateOf(false) }
    var stickyHoldProgressInternal by remember { mutableStateOf(0f) }
    // Sticky Hold가 현재 누르기 중에 활성화되었는지 추적
    // true면 손을 뗄 때 onClick이 실행되어도 해제하지 않음 (다음 탭에서 해제)
    var stickyActivatedDuringPress by remember { mutableStateOf(false) }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val STICKY_HOLD_THRESHOLD_MS = 500L

    val stickyHoldProgress by animateFloatAsState(
        targetValue = if (isPressed && !isStickyLatched) stickyHoldProgressInternal else 0f,
        animationSpec = tween(durationMillis = STICKY_HOLD_THRESHOLD_MS.toInt()),
        label = "StickyHoldProgress"
    )

    // Sticky Hold 진행 타이머 (롱프레스 감지)
    LaunchedEffect(isPressed) {
        if (isPressed && !isStickyLatched) {
            val startTime = System.currentTimeMillis()
            while (isPressed && System.currentTimeMillis() - startTime < STICKY_HOLD_THRESHOLD_MS) {
                stickyHoldProgressInternal = ((System.currentTimeMillis() - startTime).toFloat() / STICKY_HOLD_THRESHOLD_MS).coerceIn(0f, 1f)
                kotlinx.coroutines.delay(16L)
            }
            if (isPressed && System.currentTimeMillis() - startTime >= STICKY_HOLD_THRESHOLD_MS) {
                isStickyLatched = true
                stickyActivatedDuringPress = true
                onKeyPressed?.invoke(keyCode)
                Log.d("KeyboardKeyButton", "Sticky Hold latched: $keyLabel (0x${keyCode.toString(16)})")
            }
            stickyHoldProgressInternal = 0f
        } else if (!isPressed) {
            stickyHoldProgressInternal = 0f
        }
    }

    val backgroundColor by animateColorAsState(
        targetValue = when {
            !isEnabled -> Color(0xFF3A3A3A)
            isStickyLatched -> Color(0xFF1565C0)
            isPressed -> Color(0xFF1976D2)
            isActive -> Color(0xFF42A5F5)
            else -> Color(0xFF2196F3)
        },
        label = "KeyBgColor"
    )

    val borderColor = if (isStickyLatched) Color(0xFFFF9800) else Color.Transparent
    val borderWidth = if (isStickyLatched) 2.dp else 0.dp
    val shape = RoundedCornerShape(6.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .border(borderWidth, borderColor, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = isEnabled
            ) {
                if (stickyActivatedDuringPress) {
                    // Sticky Hold 활성화 직후의 손 뗌 — 무시 (latched 상태 유지)
                    stickyActivatedDuringPress = false
                    Log.d("KeyboardKeyButton", "Sticky Hold kept on release: $keyLabel")
                } else if (isStickyLatched) {
                    // 이미 latched 상태에서 다시 탭 — 해제
                    onKeyReleased?.invoke(keyCode)
                    isStickyLatched = false
                    Log.d("KeyboardKeyButton", "Sticky Hold released: $keyLabel (0x${keyCode.toString(16)})")
                } else {
                    // 일반 탭 (짧게 눌렀다 뗌)
                    onKeyPressed?.invoke(keyCode)
                    onKeyReleased?.invoke(keyCode)
                    Log.d("KeyboardKeyButton", "Key tapped: $keyLabel (0x${keyCode.toString(16)})")
                }
                stickyHoldProgressInternal = 0f
            }
            .drawWithContent {
                drawContent()
                if (stickyHoldProgress > 0f && !isStickyLatched) {
                    drawRect(
                        color = Color(0xFF1976D2).copy(alpha = 0.3f),
                        topLeft = Offset.Zero,
                        size = Size(size.width * stickyHoldProgress, size.height)
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = keyLabel,
            fontSize = when {
                keyLabel.length == 1 -> 15.sp
                keyLabel.length <= 3 -> 13.sp
                else -> 11.sp
            },
            fontWeight = FontWeight.Medium,
            color = if (!isEnabled) Color(0xFF666666) else Color.White,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFF0D0D0D,
    widthDp = 300,
    heightDp = 100
)
@Composable
fun KeyboardKeyButtonPreview() {
    Row(
        modifier = Modifier
            .size(300.dp, 100.dp)
            .background(Color(0xFF0D0D0D))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        KeyboardKeyButton(
            keyLabel = "A",
            keyCode = 0x04u,
            modifier = Modifier.size(48.dp, 42.dp)
        )
        KeyboardKeyButton(
            keyLabel = "Ctrl",
            keyCode = 0x01u,
            modifier = Modifier.size(60.dp, 42.dp)
        )
        KeyboardKeyButton(
            keyLabel = "Enter",
            keyCode = 0x28u,
            modifier = Modifier.size(80.dp, 42.dp)
        )
        KeyboardKeyButton(
            keyLabel = "↑",
            keyCode = 0x52u,
            isEnabled = false,
            modifier = Modifier.size(48.dp, 42.dp)
        )
    }
}
