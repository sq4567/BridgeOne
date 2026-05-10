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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bridgeone.app.ui.common.AppIcon
import com.bridgeone.app.ui.common.CustomPointerDynamicsPreset
import com.bridgeone.app.ui.common.DYNAMICS_PRESETS
import com.bridgeone.app.ui.common.customPresetIconOrNull
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sqrt

// 스와이프 1단계 이동 임계값 (dp). 기본값: 30f
private const val SWIPE_STEP_DP = 30f

// 탭 판정 최대 이동량 (dp). 기본값: 8f
private const val TAP_THRESHOLD_DP = 8f

// 롱프레스 판정 시간 (ms). 기본값: 500
private const val LONG_PRESS_MS = 500L

private enum class PopupPhase { GRID, CONFIRM, DELETE_CONFIRM }

/**
 * 포인터 다이나믹스 프리셋 선택 팝업 (Phase 4.3.8 / 4.3.9 / 4.5.16)
 *
 * 빌트인 프리셋 + 커스텀 프리셋 통합 그리드로 표시.
 * 커스텀 프리셋 롱프레스 → 편집/삭제 옵션 메뉴.
 *
 * @param visible              팝업 표시 여부
 * @param currentIndex         현재 적용 중인 프리셋 인덱스 (통합 목록 기준)
 * @param customPresets        커스텀 프리셋 목록
 * @param onPresetConfirmed    프리셋 확정 콜백 (통합 목록 인덱스 전달)
 * @param onDismiss            취소 콜백
 * @param onAddCustomPreset    "+" 버튼 탭 콜백 (편집기 열기)
 * @param onEditCustomPreset   커스텀 프리셋 편집 콜백
 * @param onDeleteCustomPreset 커스텀 프리셋 삭제 콜백 (id 전달)
 */
@Composable
fun DynamicsPresetPopup(
    visible: Boolean,
    currentIndex: Int,
    customPresets: List<CustomPointerDynamicsPreset> = emptyList(),
    onPresetConfirmed: (Int) -> Unit,
    onDismiss: () -> Unit,
    onAddCustomPreset: () -> Unit = {},
    onEditCustomPreset: (CustomPointerDynamicsPreset) -> Unit = {},
    onDeleteCustomPreset: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()

    val totalBuiltin = DYNAMICS_PRESETS.size
    val totalPresets = totalBuiltin + customPresets.size  // 프리셋 수 (빌트인 + 커스텀)
    val addButtonIndex = totalPresets                     // "+" 버튼 인덱스

    var phase by remember { mutableStateOf(PopupPhase.GRID) }
    var tempIndex by remember { mutableIntStateOf(currentIndex) }
    var confirmOptionIndex by remember { mutableIntStateOf(0) }

    // 범위 초과 피드백용 상태
    var borderColor by remember { mutableStateOf(Color.White) }
    val shakeAnim = remember { Animatable(0f) }

    // 등장/닫힘 애니메이션 상태
    val animCount = totalPresets + 1 // +1 for "+" cell
    val bgAlpha = remember { Animatable(0f) }
    val iconOffsets = remember(animCount) { List(animCount) { Animatable(300f) } }
    val iconAlphas = remember(animCount) { List(animCount) { Animatable(0f) } }
    val cardAlpha = remember { Animatable(0f) }

    var isActive by remember { mutableStateOf(false) }

    LaunchedEffect(visible) {
        if (visible) {
            phase = PopupPhase.GRID
            tempIndex = currentIndex
            confirmOptionIndex = 0
            bgAlpha.snapTo(0f)
            iconOffsets.forEach { it.snapTo(300f) }
            iconAlphas.forEach { it.snapTo(0f) }
            cardAlpha.snapTo(0f)
            isActive = true

            launch { bgAlpha.animateTo(0.6f, tween(200)) }
            repeat(animCount) { i ->
                launch {
                    delay(i * 30L)
                    launch { iconOffsets[i].animateTo(0f, tween(280, easing = FastOutSlowInEasing)) }
                    launch { iconAlphas[i].animateTo(1f, tween(200)) }
                }
            }
            delay(animCount * 30L + 150L)
            cardAlpha.animateTo(1f, tween(150))
        } else {
            if (!isActive) return@LaunchedEffect
            launch { cardAlpha.animateTo(0f, tween(100)) }
            val lastJob = (0 until animCount).map { i ->
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
                                    if (proposed < 0 || proposed > totalPresets) {
                                        triggerBoundaryFeedback()
                                        accumDrag = 0f
                                    } else {
                                        tempIndex = proposed.coerceIn(0, totalPresets)
                                    }
                                }
                                PopupPhase.CONFIRM -> {
                                    val isCustomConfirm = tempIndex >= totalBuiltin
                                    val optCount = if (isCustomConfirm) 4 else 2
                                    confirmOptionIndex = (confirmOptionIndex + steps).coerceIn(0, optCount - 1)
                                }
                                PopupPhase.DELETE_CONFIRM -> {
                                    confirmOptionIndex = (confirmOptionIndex + steps).coerceIn(0, 1)
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
                                    if (tempIndex == addButtonIndex) {
                                        onDismiss()
                                        onAddCustomPreset()
                                    } else if (tempIndex == currentIndex && tempIndex < totalBuiltin) {
                                        // 빌트인 현재 프리셋 탭 → 취소
                                        onDismiss()
                                    } else {
                                        confirmOptionIndex = 0
                                        phase = PopupPhase.CONFIRM
                                    }
                                }
                                PopupPhase.CONFIRM -> {
                                    val isCustomConfirm = tempIndex >= totalBuiltin
                                    val cp = if (isCustomConfirm) customPresets.getOrNull(tempIndex - totalBuiltin) else null
                                    when (confirmOptionIndex) {
                                        0 -> { // 적용 / 예
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                            } else {
                                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                            }
                                            onPresetConfirmed(tempIndex)
                                        }
                                        1 -> { // 취소 / 아니요
                                            confirmOptionIndex = 0
                                            phase = PopupPhase.GRID
                                        }
                                        2 -> if (cp != null) { // 편집
                                            onDismiss(); onEditCustomPreset(cp)
                                        }
                                        3 -> if (cp != null) { // 삭제
                                            confirmOptionIndex = 0
                                            phase = PopupPhase.DELETE_CONFIRM
                                        }
                                    }
                                }
                                PopupPhase.DELETE_CONFIRM -> {
                                    val cp = customPresets.getOrNull(tempIndex - totalBuiltin)
                                    when (confirmOptionIndex) {
                                        0 -> if (cp != null) { // 삭제 확정
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                            }
                                            onDeleteCustomPreset(cp.id)
                                            onDismiss()
                                        }
                                        1 -> { // 취소 → CONFIRM으로 복귀
                                            confirmOptionIndex = 3
                                            phase = PopupPhase.CONFIRM
                                        }
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
                    if (targetState == PopupPhase.CONFIRM || targetState == PopupPhase.DELETE_CONFIRM) {
                        (fadeIn(tween(200)) + slideInVertically(tween(250)) { it / 4 }) togetherWith
                                (fadeOut(tween(150)) + slideOutVertically(tween(200)) { -it / 4 })
                    } else {
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
                        customPresets = customPresets,
                        addButtonIndex = addButtonIndex,
                        borderColor = borderColor,
                        shakeOffsetDp = shakeAnim.value,
                        iconOffsets = iconOffsets,
                        iconAlphas = iconAlphas,
                        cardAlpha = cardAlpha.value,
                        onAddCustomPreset = onAddCustomPreset
                    )

                    PopupPhase.CONFIRM -> ConfirmPhaseContent(
                        tempIndex = tempIndex,
                        customPresets = customPresets,
                        confirmOptionIndex = confirmOptionIndex
                    )

                    PopupPhase.DELETE_CONFIRM -> DeleteConfirmPhaseContent(
                        tempIndex = tempIndex,
                        customPresets = customPresets,
                        confirmOptionIndex = confirmOptionIndex
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// GRID 단계 콘텐츠
// ─────────────────────────────────────────────

@Composable
private fun GridPhaseContent(
    tempIndex: Int,
    currentIndex: Int,
    customPresets: List<CustomPointerDynamicsPreset>,
    addButtonIndex: Int,
    borderColor: Color,
    shakeOffsetDp: Float,
    iconOffsets: List<Animatable<Float, *>>,
    iconAlphas: List<Animatable<Float, *>>,
    cardAlpha: Float,
    onAddCustomPreset: () -> Unit
) {
    val totalBuiltin = DYNAMICS_PRESETS.size

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

        // 빌트인 + 커스텀 + "+" 버튼을 단일 인덱스 목록으로 통합 후 chunked(3)
        val allIndices = (0..addButtonIndex).toList()

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            allIndices.chunked(3).forEach { rowIndices ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)) {
                    rowIndices.forEach { idx ->
                        val isSelected = idx == tempIndex
                        val isCurrent = idx == currentIndex
                        val isAddButton = idx == addButtonIndex
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .alpha((iconAlphas.getOrNull(idx)?.value ?: 1f) * (if (isSelected) 1f else 0.5f))
                                .offset(y = (iconOffsets.getOrNull(idx)?.value ?: 0f).dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        when {
                                            isAddButton -> Color.White.copy(alpha = 0.08f)
                                            isCurrent -> Color(0xFF7C9EFF).copy(alpha = 0.45f)
                                            else -> Color.White.copy(alpha = 0.12f)
                                        }
                                    )
                                    .then(
                                        if (isSelected && !isAddButton) Modifier.border(2.dp, borderColor, RoundedCornerShape(8.dp))
                                        else if (isSelected) Modifier.border(2.dp, borderColor, RoundedCornerShape(8.dp))
                                        else if (isAddButton) Modifier.border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isAddButton) {
                                    Icon(
                                        Icons.Filled.Add,
                                        contentDescription = "추가",
                                        tint = Color.White.copy(alpha = if (isSelected) 1f else 0.7f),
                                        modifier = Modifier.size(26.dp)
                                    )
                                } else if (idx < totalBuiltin) {
                                    val preset = DYNAMICS_PRESETS[idx]
                                    AppIcon(
                                        def = preset.icon,
                                        contentDescription = preset.name,
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                } else {
                                    val cp = customPresets.getOrNull(idx - totalBuiltin)
                                    val cpIcon = customPresetIconOrNull(cp?.iconKey ?: "")
                                    if (cpIcon != null) {
                                        AppIcon(
                                            def = cpIcon,
                                            contentDescription = cp?.name,
                                            tint = Color.White,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    } else {
                                        Text(
                                            text = (cp?.name ?: "").take(2),
                                            color = Color.White,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            val labelText = when {
                                isAddButton -> "추가"
                                idx < totalBuiltin -> DYNAMICS_PRESETS[idx].name
                                else -> customPresets.getOrNull(idx - totalBuiltin)?.name ?: ""
                            }
                            PresetLabel(
                                text = labelText,
                                isCurrent = isCurrent,
                                isAddButton = isAddButton,
                                isSelected = isSelected
                            )
                        }
                    }
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
    customPresets: List<CustomPointerDynamicsPreset>,
    confirmOptionIndex: Int
) {
    val totalBuiltin = DYNAMICS_PRESETS.size
    val isCustom = tempIndex >= totalBuiltin
    val customPreset = if (isCustom) customPresets.getOrNull(tempIndex - totalBuiltin) else null
    val presetName = customPreset?.name ?: if (!isCustom) DYNAMICS_PRESETS[tempIndex].name else "커스텀"
    val description = customPreset?.description?.ifEmpty { "커스텀 포인터 다이나믹스 프리셋" }
        ?: if (!isCustom) DYNAMICS_PRESETS[tempIndex].description else "커스텀 포인터 다이나믹스 프리셋"
    val customIconDef = customPresetIconOrNull(customPreset?.iconKey ?: "")

    // 선택지 정의
    data class ConfirmOption(val label: String, val color: Color)
    val options = if (isCustom) listOf(
        ConfirmOption("적용", Color.White),
        ConfirmOption("취소", Color.White),
        ConfirmOption("편집", Color(0xFF4F8EF7)),
        ConfirmOption("삭제", Color(0xFFFF5252))
    ) else listOf(
        ConfirmOption("예", Color.White),
        ConfirmOption("아니요", Color.White)
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))

        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            if (!isCustom) {
                AppIcon(
                    def = DYNAMICS_PRESETS[tempIndex].icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            } else if (customIconDef != null) {
                AppIcon(
                    def = customIconDef,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            } else {
                Text(
                    text = presetName.take(2),
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = presetName,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = description,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEachIndexed { i, opt ->
                val isSelected = i == confirmOptionIndex
                Text(
                    text = opt.label,
                    fontSize = if (isSelected) 16.sp else 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) opt.color else opt.color.copy(alpha = 0.35f)
                )
            }
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

// ─────────────────────────────────────────────
// DELETE_CONFIRM 단계 콘텐츠
// ─────────────────────────────────────────────

@Composable
private fun DeleteConfirmPhaseContent(
    tempIndex: Int,
    customPresets: List<CustomPointerDynamicsPreset>,
    confirmOptionIndex: Int
) {
    val totalBuiltin = DYNAMICS_PRESETS.size
    val cp = customPresets.getOrNull(tempIndex - totalBuiltin)
    val presetName = cp?.name ?: "프리셋"
    val customIconDef = customPresetIconOrNull(cp?.iconKey ?: "")

    data class DeleteOption(val label: String, val color: Color)
    val options = listOf(
        DeleteOption("삭제", Color(0xFFFF5252)),
        DeleteOption("취소", Color.White)
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))

        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFFF5252).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            if (customIconDef != null) {
                AppIcon(
                    def = customIconDef,
                    contentDescription = null,
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(36.dp)
                )
            } else {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = null,
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = presetName,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = "이 프리셋을 삭제하시겠습니까?",
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEachIndexed { i, opt ->
                val isSelected = i == confirmOptionIndex
                Text(
                    text = opt.label,
                    fontSize = if (isSelected) 16.sp else 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) opt.color else opt.color.copy(alpha = 0.35f)
                )
            }
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

// ─────────────────────────────────────────────
// 프리셋 라벨: 긴 이름은 폰트 자동 축소, 최소 크기 이하면 2줄 허용
// ─────────────────────────────────────────────

@Composable
private fun PresetLabel(
    text: String,
    isCurrent: Boolean,
    isAddButton: Boolean,
    isSelected: Boolean
) {
    // 11sp → 최소 7sp까지 0.5sp씩 축소, 그래도 넘치면 2줄 허용
    var fontSize by remember(text) { mutableStateOf(11f) }
    Text(
        text = text,
        fontSize = fontSize.sp,
        maxLines = 2,
        softWrap = true,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
        color = Color.White.copy(alpha = if (isAddButton && !isSelected) 0.5f else 1f),
        onTextLayout = { result ->
            if (result.hasVisualOverflow && fontSize > 7f) {
                fontSize = (fontSize - 0.5f).coerceAtLeast(7f)
            }
        },
        modifier = Modifier
            .width(52.dp)
            .wrapContentHeight()
            .padding(top = 4.dp)
    )
}
