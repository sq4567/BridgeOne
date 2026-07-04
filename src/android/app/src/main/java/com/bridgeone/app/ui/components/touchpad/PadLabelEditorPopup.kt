package com.bridgeone.app.ui.components.touchpad

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bridgeone.app.ui.common.MultiCursorConstants
import com.bridgeone.app.ui.components.SwipeKeyboardOverlay

private val PadLabelFieldAccent = Color(0xFF4F8EF7)

/**
 * 멀티 커서 패드 커스텀 라벨 편집 팝업 (Phase 4.8.10).
 *
 * 그리드 셀 또는 [PadSwitchButtonPanel] 전환 버튼 롱프레스로 진입한다.
 * 시스템 IME 대신 BridgeOne 내장 [SwipeKeyboardOverlay]로 입력을 받는다
 * (`EdgeZoneEditorScreen.kt`의 라벨 편집 선례 재사용, 시스템 키보드 미사용).
 * 빈 이름으로 완료하면 [MultiCursorState.labelFor]가 번호로 폴백한다.
 *
 * @param padIndex   편집 대상 패드 인덱스 (0-based)
 * @param currentLabel 현재 저장된 커스텀 라벨 (null이면 미지정)
 * @param onConfirm  완료(키보드 "완료" 키) 시 콜백. 트리밍 전 원본 텍스트를 전달한다.
 * @param onDismiss  취소(키보드 "취소" 키) 시 콜백
 */
@Composable
internal fun PadLabelEditorPopup(
    padIndex: Int,
    currentLabel: String?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var input by remember(padIndex) { mutableStateOf(currentLabel ?: "") }

    SwipeKeyboardOverlay(
        initialText = input,
        maxLength = MultiCursorConstants.PAD_LABEL_MAX_LENGTH,
        onTextChange = { input = it },
        onCancel = onDismiss,
        onDone = { result -> onConfirm(result) },
        revertOnCancel = false,
        overlay = {
            PadLabelInputField(padIndex = padIndex, input = input)
        }
    )
}

@Composable
private fun BoxScope.PadLabelInputField(padIndex: Int, input: String) {
    // 캐럿 점멸 (ShortcutEditorPopup 액션명 필드 패턴 재사용). 기본값: 1000ms 주기, 500ms on/off
    val caretTransition = rememberInfiniteTransition(label = "padLabelCaret")
    val caretAlpha by caretTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1000
                1f at 0
                1f at 500
                0f at 501
                0f at 1000
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "padLabelCaretAlpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = 96.dp)
    ) {
        Text(
            text = "패드 ${padIndex + 1} 이름",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 12.sp,
        )
        Box(
            modifier = Modifier
                .padding(top = 8.dp)
                .widthIn(min = 96.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1A1A1A))
                .border(1.5.dp, PadLabelFieldAccent, RoundedCornerShape(10.dp))
                .padding(horizontal = 18.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (input.isEmpty()) {
                    // 최초 이름(번호)은 실제 입력이 아니라 미지정 시 적용될 기본값임을 알 수 있도록 반투명 표시
                    Text(
                        text = "${padIndex + 1}",
                        color = Color.White.copy(alpha = 0.35f),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                } else {
                    Text(
                        text = input,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = "|",
                    color = PadLabelFieldAccent.copy(alpha = caretAlpha),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
