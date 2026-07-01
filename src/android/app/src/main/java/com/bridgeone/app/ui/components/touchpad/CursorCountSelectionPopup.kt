package com.bridgeone.app.ui.components.touchpad

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

// 등장 애니메이션 지속 시간 (ms). 기본값: 150
private const val POPUP_ENTER_DURATION_MS = 150
// 슬라이드 시작 오프셋 (dp, 위로 슬라이드). 기본값: 12f
private const val POPUP_SLIDE_OFFSET_DP = 12f

/**
 * 커서 수 선택 팝업 (Phase 4.8.2, component-touchpad.md §1.7.2).
 *
 * `CursorModeButton` 탭(싱글 → 멀티 시도) 시 표시되는 소형 팝업. 전체 오버레이가 아니라
 * 버튼 위쪽에 뜨는 작은 카드로, [2][3][4] 중 하나를 선택하면 즉시 닫히고 멀티 커서가
 * 활성화된다. 외부 터치 시 아무 동작 없이 닫힌다(싱글 커서 유지).
 *
 * StandardModePage에서 항상 렌더링하고 [visible] 파라미터로 표시/숨김을 제어한다.
 *
 * @param visible     팝업 표시 여부
 * @param onSelect    커서 수(2/3/4) 선택 콜백
 * @param onDismiss   외부 터치로 인한 취소 콜백
 */
@Composable
fun CursorCountSelectionPopup(
    visible: Boolean,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current

    val bgAlpha = remember { Animatable(0f) }
    val cardAlpha = remember { Animatable(0f) }
    val cardOffset = remember { Animatable(POPUP_SLIDE_OFFSET_DP) }
    var isActive by remember { mutableStateOf(false) }

    LaunchedEffect(visible) {
        if (visible) {
            isActive = true
            bgAlpha.snapTo(0f)
            cardAlpha.snapTo(0f)
            cardOffset.snapTo(POPUP_SLIDE_OFFSET_DP)
            launch { bgAlpha.animateTo(0.3f, tween(POPUP_ENTER_DURATION_MS)) }
            launch { cardAlpha.animateTo(1f, tween(POPUP_ENTER_DURATION_MS)) }
            launch { cardOffset.animateTo(0f, tween(POPUP_ENTER_DURATION_MS)) }
        } else {
            isActive = false
        }
    }

    if (!isActive) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = bgAlpha.value))
            .pointerInput(visible) {
                detectTapGestures(onTap = { onDismiss() })
            }
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 56.dp)
                .offset(y = cardOffset.value.dp)
                .alpha(cardAlpha.value)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E1E1E).copy(alpha = 0.9f))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            for (count in MULTI_CURSOR_COUNT_MIN..MULTI_CURSOR_COUNT_MAX) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(),
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                } else {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                }
                                onSelect(count)
                            }
                        )
                ) {
                    Text(
                        text = count.toString(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
