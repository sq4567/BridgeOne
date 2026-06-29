package com.bridgeone.app.ui.common.swipe

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** content 내부에서 현재 요소의 포커스 상태를 읽기 위한 CompositionLocal. */
val LocalSwipeFocused = compositionLocalOf { false }

/** content 내부에서 경계 히트 플래시 알파(0f~0.45f)를 읽기 위한 CompositionLocal. */
val LocalSwipeFlashAlpha = compositionLocalOf { 0f }

/**
 * SWIPE 모드 포커스 대상 wrapper.
 *
 * - [onGloballyPositioned]로 자신의 절대 좌표를 [SwipeFocusController]에 등록.
 * - 포커스 시 [LocalContentColor]를 primary로 오버라이드 → content 내부 아이콘/텍스트가 별도 처리 없이 색 변경됨.
 * - content가 비어 있거나 자체 색 변경이 없는 경우 [showBorderHighlight] = true로 border 하이라이트를 명시적으로 활성화.
 * - [LocalSwipeFocusController]가 null이면 wrapper는 아무 추가 동작도 하지 않고 [content]만 그대로 렌더 → NORMAL 모드 회귀 없음.
 *
 * @param element 이 요소의 식별자
 * @param scope 이 요소가 속한 scope (기본: ROOT_SCOPE)
 * @param manipulatable true면 activate 시 조작 모드 진입
 * @param onActivate 탭 시 동작 (보통 기존 onClick 람다 전달)
 * @param onActivateAlt 더블탭 시 동작 (기본: onActivate와 동일)
 * @param onManipulate 조작 모드 스와이프 콜백
 * @param shape border 하이라이트 모양. 기본값: RoundedCornerShape(8.dp)
 * @param highlightBorderWidth border 두께. 기본값: 2dp
 * @param showBorderHighlight true면 포커스 시 border + 배경 오버레이 추가. content가 빈 요소에 사용. 기본값: false
 * @param showFlashOverlay true면 경계 히트 시 붉은 오버레이 Box를 직접 렌더. false면 [LocalSwipeFlashAlpha]만 제공 — content가 직접 색상으로 반응할 때 사용. 기본값: true
 */
@Composable
fun SwipeFocusable(
    element: FocusableElement,
    scope: Any = ROOT_SCOPE,
    manipulatable: Boolean = false,
    manipulationAxis: ManipulationAxis = ManipulationAxis.HORIZONTAL,
    onActivate: () -> Unit = {},
    onActivateAlt: () -> Unit = onActivate,
    onManipulate: (deltaPx: Float, screenWidthPx: Float) -> Unit = { _, _ -> },
    shape: Shape = RoundedCornerShape(8.dp),
    highlightBorderWidth: Dp = 2.dp,
    showBorderHighlight: Boolean = false,
    showFlashOverlay: Boolean = true,
    gridRow: Int? = null,
    gridCol: Int? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val controller = LocalSwipeFocusController.current
    if (controller == null) {
        Box(modifier = modifier) { content() }
        return
    }

    val isFocused = controller.currentFocus == element
    val inManipulation = isFocused && controller.mode == SwipeMode.MANIPULATION

    val primary = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val errorColor = MaterialTheme.colorScheme.error
    val currentContentColor = LocalContentColor.current

    val borderColor by animateColorAsState(
        targetValue = if (showBorderHighlight && isFocused) primary else Color.Transparent,
        animationSpec = tween(durationMillis = 50),  // 기본값: 50
        label = "swipeFocusBorder",
    )
    val bgColor by animateColorAsState(
        targetValue = when {
            showBorderHighlight && inManipulation -> primaryContainer.copy(alpha = 0.30f)
            showBorderHighlight && isFocused -> primaryContainer.copy(alpha = 0.15f)
            else -> Color.Transparent
        },
        animationSpec = tween(durationMillis = 50),  // 기본값: 50
        label = "swipeFocusBg",
    )
    val contentColor by animateColorAsState(
        targetValue = if (isFocused) primary else currentContentColor,
        animationSpec = tween(durationMillis = 50),  // 기본값: 50
        label = "swipeFocusContentColor",
    )

    // 경계 히트 플래시: 붉은 오버레이를 순간 표시 후 페이드아웃
    val flashAlpha = remember { Animatable(0f) }
    val flashKey = controller.flashSignal?.takeIf { it.element == element }
    LaunchedEffect(flashKey) {
        if (flashKey != null) {
            flashAlpha.snapTo(SWIPE_FLASH_PEAK_ALPHA)
            flashAlpha.animateTo(0f, animationSpec = tween(durationMillis = 350))
        } else if (flashAlpha.value > 0f) {
            flashAlpha.animateTo(0f, animationSpec = tween(durationMillis = 150))
        }
    }

    val rememberedActivate = remember(onActivate) { onActivate }
    val rememberedActivateAlt = remember(onActivateAlt) { onActivateAlt }
    val rememberedManipulate = remember(onManipulate) { onManipulate }

    Box(
        modifier = modifier
            .onGloballyPositioned { coords ->
                controller.register(
                    FocusableEntry(
                        element = element,
                        bounds = coords.boundsInWindow(),
                        manipulatable = manipulatable,
                        scope = scope,
                        onActivate = rememberedActivate,
                        onActivateAlt = rememberedActivateAlt,
                        onManipulate = rememberedManipulate,
                        gridRow = gridRow,
                        gridCol = gridCol,
                        manipulationAxis = manipulationAxis,
                    )
                )
            }
            .then(
                if (showBorderHighlight && isFocused) {
                    Modifier
                        .clip(shape)
                        .background(bgColor)
                        .border(highlightBorderWidth, borderColor, shape)
                } else Modifier
            ),
    ) {
        val fa = flashAlpha.value
        // [0..1] 정규화: 0 = 플래시 없음, 1 = 피크. showFlashOverlay=false 사용자가 lerp로 색상 보간할 때 활용
        val flashFraction = fa / SWIPE_FLASH_PEAK_ALPHA
        CompositionLocalProvider(
            LocalSwipeFocused provides isFocused,
            LocalSwipeFlashAlpha provides flashFraction,
            LocalContentColor provides contentColor,
        ) {
            content()
        }
        if (showFlashOverlay && fa > 0f) {
            Box(
                Modifier
                    .matchParentSize()
                    .clip(shape)
                    .background(errorColor.copy(alpha = fa))
            )
        }
    }

    DisposableEffect(element, scope) {
        onDispose {
            controller.unregister(element)
        }
    }
}

/** 경계 히트 플래시의 피크 alpha. [LocalSwipeFlashAlpha]는 이 값으로 정규화된 [0..1] 분율을 제공. 기본값: 0.45f */
internal const val SWIPE_FLASH_PEAK_ALPHA = 0.45f

