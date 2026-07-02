package com.bridgeone.app.ui.components.touchpad

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

// 등장 애니메이션 지속 시간 (ms). 기본값: 150
private const val POPUP_ENTER_DURATION_MS = 150
// 퇴장 애니메이션 지속 시간 (ms). 기본값: 120
private const val POPUP_EXIT_DURATION_MS = 120
// 슬라이드 시작 오프셋 (dp, 위로 슬라이드). 기본값: 12f
private const val POPUP_SLIDE_OFFSET_DP = 12f
// 앵커(ControlButtonContainer) 실제 높이 아래 여백 (dp, Phase 4.8.6). 기본값: 8f
private const val ANCHOR_TOP_GAP_DP = 8f

/**
 * 커서 수 선택 팝업 (Phase 4.8.2, component-touchpad.md §1.7.2).
 *
 * `CursorModeButton` 탭(싱글 → 멀티 시도) 시 표시되는 소형 팝업. 전체 오버레이가 아니라
 * 버튼 위쪽에 뜨는 작은 카드로, [2][3][4] 중 하나를 선택하면 즉시 닫히고 멀티 커서가
 * 활성화된다. 외부 터치 시 아무 동작 없이 닫힌다(싱글 커서 유지).
 *
 * StandardModePage에서 항상 렌더링하고 [visible] 파라미터로 표시/숨김을 제어한다.
 *
 * 멀티 커서 활성 중에 재호출되면(Phase 4.8.6) [currentCount]에 해당하는 버튼이 강조되고
 * 우측에 "해제" 버튼이 추가로 표시되어, 해제 없이 커서 수만 바꿀 수 있다.
 *
 * 카드 위치는 [anchorTopDp](호출부가 측정한 `ControlButtonContainer` 실제 높이)를 기준으로
 * 그 바로 아래에 앵커링된다(Phase 4.8.6). `ControlButtonContainer`는 화면 크기에 따라 높이가
 * 48~72dp로 가변이라 하드코딩 오프셋으로는 겹침을 피할 수 없어 실측값을 전달받는다.
 *
 * @param visible     팝업 표시 여부
 * @param onSelect    커서 수(2/3/4) 선택 콜백
 * @param onDismiss   외부 터치로 인한 취소 콜백
 * @param currentCount 멀티 커서 활성 중일 때 현재 커서 수 (비활성 중이면 null)
 * @param onDisable   "해제" 버튼 탭 콜백 (currentCount가 null이면 버튼 자체가 표시되지 않음)
 * @param anchorTopDp `ControlButtonContainer`의 실제 렌더 높이 (호출부에서 `onGloballyPositioned`로 측정해 전달)
 */
@Composable
fun CursorCountSelectionPopup(
    visible: Boolean,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    currentCount: Int? = null,
    onDisable: (() -> Unit)? = null,
    anchorTopDp: Dp = 0.dp
) {
    val view = LocalView.current

    val bgAlpha = remember { Animatable(0f) }
    val cardAlpha = remember { Animatable(0f) }
    val cardOffset = remember { Animatable(POPUP_SLIDE_OFFSET_DP) }
    var isActive by remember { mutableStateOf(false) }

    // 팝업이 열릴 때의 currentCount/onDisable을 스냅샷으로 고정해 사용한다. 선택/해제 액션은
    // 호출부의 multiCursorState를 즉시 바꾸므로, 퇴장 애니메이션(120ms) 동안 이 값을 그대로
    // 읽으면 사라지는 도중 바뀐 상태(예: 방금 활성화된 커서 수, 방금 해제된 상태)가 잠깐
    // 비쳐 보인다. 열린 시점의 값으로 고정해 퇴장 중에는 항상 열렸을 때의 모습으로 사라지게 한다.
    var displayedCurrentCount by remember { mutableStateOf<Int?>(null) }
    var displayedOnDisable by remember { mutableStateOf<(() -> Unit)?>(null) }

    LaunchedEffect(visible) {
        if (visible) {
            displayedCurrentCount = currentCount
            displayedOnDisable = onDisable
            isActive = true
            bgAlpha.snapTo(0f)
            cardAlpha.snapTo(0f)
            cardOffset.snapTo(POPUP_SLIDE_OFFSET_DP)
            launch { bgAlpha.animateTo(0.3f, tween(POPUP_ENTER_DURATION_MS)) }
            launch { cardAlpha.animateTo(1f, tween(POPUP_ENTER_DURATION_MS)) }
            launch { cardOffset.animateTo(0f, tween(POPUP_ENTER_DURATION_MS)) }
        } else if (isActive) {
            launch { bgAlpha.animateTo(0f, tween(POPUP_EXIT_DURATION_MS)) }
            launch { cardAlpha.animateTo(0f, tween(POPUP_EXIT_DURATION_MS)) }
            cardOffset.animateTo(POPUP_SLIDE_OFFSET_DP, tween(POPUP_EXIT_DURATION_MS))
            isActive = false
        }
    }

    if (!isActive) return

    val frozenCurrentCount = displayedCurrentCount
    val frozenOnDisable = displayedOnDisable

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
                .padding(top = anchorTopDp + ANCHOR_TOP_GAP_DP.dp)
                .offset(y = cardOffset.value.dp)
                .alpha(cardAlpha.value)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E1E1E).copy(alpha = 0.9f))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            for (count in MULTI_CURSOR_COUNT_MIN..MULTI_CURSOR_COUNT_MAX) {
                val isCurrent = count == frozenCurrentCount
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isCurrent) TouchpadColorPurple.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.1f))
                        .then(
                            if (isCurrent) Modifier.border(2.dp, TouchpadColorPurple, RoundedCornerShape(8.dp))
                            else Modifier
                        )
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
            if (frozenCurrentCount != null && frozenOnDisable != null) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(TouchpadColorRed.copy(alpha = 0.85f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(),
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                } else {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                }
                                frozenOnDisable()
                            }
                        )
                ) {
                    Text(
                        text = "해제",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
