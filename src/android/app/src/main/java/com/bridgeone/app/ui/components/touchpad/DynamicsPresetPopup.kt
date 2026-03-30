package com.bridgeone.app.ui.components.touchpad

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bridgeone.app.ui.common.AppIcon
import com.bridgeone.app.ui.common.DYNAMICS_PRESETS
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sqrt

// 스와이프 1단계 이동 임계값 (dp)
private const val SWIPE_STEP_DP = 30f

// 탭 판정 최대 이동량 (dp)
private const val TAP_THRESHOLD_DP = 8f

private enum class PopupPhase { GRID, CONFIRM }

/**
 * 포인터 다이나믹스 프리셋 선택 팝업 (Phase 4.3.8 / 4.3.9)
 *
 * [DynamicsPresetButton] 롱프레스 시 터치패드 전체 영역에 오버레이됩니다.
 * StandardModePage에서 항상 렌더링하고 [visible] 파라미터로 표시/숨김을 제어합니다.
 *
 * 2단계 UX:
 * 1. GRID 단계: 프리셋 아이콘 목록 표시 — 좌우 스와이프로 선택, 탭으로 확인 단계 진입
 * 2. CONFIRM 단계: 선택 프리셋 확대 + 예/아니요 선택 — 탭으로 확정 또는 그리드 복귀
 *
 * @param visible              팝업 표시 여부 (false 시 exit 애니메이션 후 숨김)
 * @param currentIndex         현재 적용 중인 프리셋 인덱스
 * @param onPresetConfirmed    프리셋 확정 시 콜백 (확정된 인덱스 전달)
 * @param onDismiss            취소 콜백 (즉시 호출 → 부모가 visible=false 설정 → exit 애니메이션)
 */
@Composable
fun DynamicsPresetPopup(
    visible: Boolean,
    currentIndex: Int,
    onPresetConfirmed: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()

    var phase by remember { mutableStateOf(PopupPhase.GRID) }
    var tempIndex by remember { mutableIntStateOf(currentIndex) }
    var confirmChoice by remember { mutableStateOf(false) }

    // 범위 초과 피드백용 상태
    var borderColor by remember { mutableStateOf(Color.White) }
    val shakeAnim = remember { Animatable(0f) }

    // 등장/닫힘 애니메이션 상태
    val presetCount = DYNAMICS_PRESETS.size
    val bgAlpha = remember { Animatable(0f) }
    val iconOffsets = remember { List(presetCount) { Animatable(300f) } }
    val iconAlphas = remember { List(presetCount) { Animatable(0f) } }
    val cardAlpha = remember { Animatable(0f) }

    // 실제 렌더링 여부 — exit 애니메이션이 끝날 때까지 true를 유지
    var isActive by remember { mutableStateOf(false) }

    LaunchedEffect(visible) {
        if (visible) {
            // 재오픈 시 초기 상태 리셋
            phase = PopupPhase.GRID
            tempIndex = currentIndex
            confirmChoice = false
            bgAlpha.snapTo(0f)
            iconOffsets.forEach { it.snapTo(300f) }
            iconAlphas.forEach { it.snapTo(0f) }
            cardAlpha.snapTo(0f)
            isActive = true

            // 등장 애니메이션: 배경 fade-in + 아이콘 stagger slide-up
            launch { bgAlpha.animateTo(0.6f, tween(200)) }
            repeat(presetCount) { i ->
                launch {
                    delay(i * 30L)
                    launch { iconOffsets[i].animateTo(0f, tween(280, easing = FastOutSlowInEasing)) }
                    launch { iconAlphas[i].animateTo(1f, tween(200)) }
                }
            }
            // 안내 카드 / 텍스트: 아이콘 등장 직후 fade-in
            delay(presetCount * 30L + 150L)
            cardAlpha.animateTo(1f, tween(150))
        } else {
            if (!isActive) return@LaunchedEffect
            // 닫힘 애니메이션: 안내 카드 fade-out → 아이콘 stagger slide-down → 배경 fade-out
            launch { cardAlpha.animateTo(0f, tween(100)) }
            val lastJob = (0 until presetCount).map { i ->
                launch {
                    delay(i * 20L)
                    launch { iconOffsets[i].animateTo(300f, tween(200)) }
                    launch { iconAlphas[i].animateTo(0f, tween(150)) }
                }
            }.last()
            lastJob.join()
            bgAlpha.animateTo(0f, tween(200))
            isActive = false
        }
    }

    if (!isActive) return

    val swipeStepPx = with(density) { SWIPE_STEP_DP.dp.toPx() }
    val tapThresholdPx = with(density) { TAP_THRESHOLD_DP.dp.toPx() }

    fun triggerBoundaryFeedback() {
        if (shakeAnim.isRunning) return
        coroutineScope.launch {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                view.performHapticFeedback(HapticFeedbackConstants.REJECT)
            } else {
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }
            borderColor = Color(0xFFFF4444)
            shakeAnim.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 300
                    8f at 50
                    -8f at 110
                    6f at 170
                    -6f at 230
                    0f at 300
                }
            )
            borderColor = Color.White
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = bgAlpha.value))
            .pointerInput(visible, phase) {
                if (!visible) return@pointerInput
                awaitEachGesture {
                    val down = awaitPointerEvent()
                    if (down.type != PointerEventType.Press) return@awaitEachGesture
                    down.changes.forEach { it.consume() }

                    val downPos = down.changes.first().position
                    var lastPos = downPos
                    var hasDragged = false
                    var accumDrag = 0f

                    var moveEvent = awaitPointerEvent()
                    while (moveEvent.type == PointerEventType.Move) {
                        moveEvent.changes.forEach { it.consume() }
                        val pos = moveEvent.changes.first().position

                        val fromDown = pos - downPos
                        val distFromDown = sqrt(fromDown.x * fromDown.x + fromDown.y * fromDown.y)
                        if (distFromDown > tapThresholdPx) hasDragged = true

                        val dx = pos.x - lastPos.x
                        accumDrag += dx

                        val steps = (accumDrag / swipeStepPx).toInt()
                        if (steps != 0) {
                            accumDrag -= steps * swipeStepPx
                            when (phase) {
                                PopupPhase.GRID -> {
                                    val proposed = tempIndex + steps
                                    if (proposed < 0 || proposed > DYNAMICS_PRESETS.lastIndex) {
                                        triggerBoundaryFeedback()
                                        accumDrag = 0f
                                    } else {
                                        tempIndex = proposed.coerceIn(0, DYNAMICS_PRESETS.lastIndex)
                                    }
                                }
                                PopupPhase.CONFIRM -> {
                                    // 왼쪽(steps < 0) = 예, 오른쪽 = 아니요
                                    confirmChoice = steps < 0
                                }
                            }
                        }

                        lastPos = pos
                        moveEvent = awaitPointerEvent()
                    }

                    if (moveEvent.type == PointerEventType.Release) {
                        moveEvent.changes.forEach { it.consume() }
                        if (!hasDragged) {
                            when (phase) {
                                PopupPhase.GRID -> {
                                    if (tempIndex == currentIndex) {
                                        onDismiss()
                                    } else {
                                        confirmChoice = false
                                        phase = PopupPhase.CONFIRM
                                    }
                                }
                                PopupPhase.CONFIRM -> {
                                    if (confirmChoice) {
                                        // 예 선택 → 프리셋 확정
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                        } else {
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        }
                                        onPresetConfirmed(tempIndex)
                                    } else {
                                        // 아니요 선택 → 그리드 복귀
                                        confirmChoice = false
                                        phase = PopupPhase.GRID
                                    }
                                }
                            }
                        }
                    }
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedContent(
                modifier = Modifier.weight(1f),
                targetState = phase,
                transitionSpec = {
                    if (targetState == PopupPhase.CONFIRM) {
                        // GRID → CONFIRM: 위로 슬라이드 + fade-in
                        (fadeIn(tween(200)) + slideInVertically(tween(250)) { it / 4 }) togetherWith
                                (fadeOut(tween(150)) + slideOutVertically(tween(200)) { -it / 4 })
                    } else {
                        // CONFIRM → GRID: 아래로 슬라이드 + fade-in
                        (fadeIn(tween(200)) + slideInVertically(tween(250)) { -it / 4 }) togetherWith
                                (fadeOut(tween(150)) + slideOutVertically(tween(200)) { it / 4 })
                    }
                },
                label = "phaseTransition"
            ) { targetPhase ->
                when (targetPhase) {
                    PopupPhase.GRID -> GridPhaseContent(
                        tempIndex = tempIndex,
                        currentIndex = currentIndex,
                        borderColor = borderColor,
                        shakeOffsetDp = shakeAnim.value,
                        iconOffsets = iconOffsets,
                        iconAlphas = iconAlphas,
                        cardAlpha = cardAlpha.value
                    )

                    PopupPhase.CONFIRM -> ConfirmPhaseContent(
                        tempIndex = tempIndex,
                        confirmChoice = confirmChoice
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// GRID 단계 콘텐츠
// ─────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GridPhaseContent(
    tempIndex: Int,
    currentIndex: Int,
    borderColor: Color,
    shakeOffsetDp: Float,
    iconOffsets: List<Animatable<Float, *>>,
    iconAlphas: List<Animatable<Float, *>>,
    cardAlpha: Float
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .offset(x = shakeOffsetDp.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))

        Text(
            text = "원하는 알고리즘\n프리셋을 선택하세요.",
            fontSize = 16.sp,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.alpha(cardAlpha)
        )

        Spacer(Modifier.height(12.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            DYNAMICS_PRESETS.forEachIndexed { idx, preset ->
                val isSelected = idx == tempIndex
                val isCurrent = idx == currentIndex
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .alpha(iconAlphas[idx].value * (if (isSelected) 1f else 0.5f))
                        .offset(y = iconOffsets[idx].value.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when {
                                    isCurrent -> Color(0xFF7C9EFF).copy(alpha = 0.45f)
                                    else -> Color.White.copy(alpha = 0.12f)
                                }
                            )
                            .then(
                                if (isSelected) Modifier.border(
                                    2.dp,
                                    borderColor,
                                    RoundedCornerShape(8.dp)
                                ) else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        AppIcon(
                            def = preset.icon,
                            contentDescription = preset.name,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Text(
                        text = preset.name,
                        fontSize = 11.sp,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                        color = Color.White,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .alpha(cardAlpha)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "◀▶  스와이프로 이동",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Text(
                    text = "현재 프리셋 탭 → 취소",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Text(
                    text = "다른 프리셋 탭 → 변경",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }

        Spacer(Modifier.weight(1f))
    }
}

// ─────────────────────────────────────────────
// CONFIRM 단계 콘텐츠
// ─────────────────────────────────────────────

@Composable
private fun ConfirmPhaseContent(
    tempIndex: Int,
    confirmChoice: Boolean
) {
    val preset = DYNAMICS_PRESETS[tempIndex]

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))

        // 선택 프리셋 확대 표시
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            AppIcon(
                def = preset.icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = preset.name,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = preset.description,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        // 예 / 아니요 선택
        Row(
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "예",
                fontSize = 15.sp,
                fontWeight = if (confirmChoice) FontWeight.Bold else FontWeight.Normal,
                color = if (confirmChoice) Color.White else Color.White.copy(alpha = 0.45f)
            )
            Text(
                text = "아니요",
                fontSize = 15.sp,
                fontWeight = if (!confirmChoice) FontWeight.Bold else FontWeight.Normal,
                color = if (!confirmChoice) Color.White else Color.White.copy(alpha = 0.45f)
            )
        }

        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "◀▶  스와이프로 선택",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Text(
                    text = "탭으로 확정",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }

        Spacer(Modifier.weight(1f))
    }
}
