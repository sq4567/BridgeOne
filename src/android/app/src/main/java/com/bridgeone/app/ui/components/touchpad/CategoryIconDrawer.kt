package com.bridgeone.app.ui.components.touchpad

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import com.bridgeone.app.ui.common.IconCategory
import com.bridgeone.app.ui.common.IconCategoryTab
import com.bridgeone.app.ui.common.swipe.FocusableElement
import com.bridgeone.app.ui.common.swipe.LocalSwipeFocused
import com.bridgeone.app.ui.common.swipe.SwipeFocusController
import com.bridgeone.app.ui.common.swipe.SwipeFocusable
import kotlin.math.roundToInt

// ============================================================
// 카테고리 아이콘 서랍 (2단계) — 애플워치 앱 서랍 스타일
// ============================================================

/** 그리드 열 수. 기본값: 6 */
private const val CD_COLS = 6

/** 각 셀의 크기 (dp). 기본값: 56f */
private const val CD_CELL_DP = 56f

/** 셀 간 간격 (dp). 기본값: 6f */
private const val CD_CELL_GAP_DP = 6f

/** 셀렉터 링 크기 (dp). 셀 컨테이너 크기와 일치. 기본값: 56f */
private const val CD_SELECTOR_SIZE_DP = 56f

/** 셀렉터 링 테두리 두께 (dp). 기본값: 2.5f */
private const val CD_SELECTOR_BORDER_DP = 2.5f

/** 그리드 패닝 애니메이션 duration (ms). 기본값: 200 */
private const val CD_PANNING_DURATION_MS = 200

/** 포커스 외 셀의 스케일 (0~1). 기본값: 0.82f */
private const val CD_UNFOCUSED_SCALE = 0.82f

/** 포커스 외 셀의 알파 (0~1). 기본값: 0.38f */
private const val CD_UNFOCUSED_ALPHA = 0.38f

/** 셀 내 아이콘 크기 (dp). 기본값: 24f */
private const val CD_ICON_SIZE_DP = 24f

/** 배경 딤(scrim) 알파 (0~1). 기본값: 0.72f */
private const val CD_SCRIM_ALPHA = 0.72f

/** 셀렉터(anchor) 중심에서 아이콘이 선명하게 보이는 반경 (dp). 이 밖은 딤으로 페이드. 기본값: 150f */
private const val CD_GRADIENT_CLEAR_RADIUS_DP = 150f

/** 딤으로 완전히 사라지는 반경 (dp). 기본값: 320f */
private const val CD_GRADIENT_FADE_RADIUS_DP = 320f

/** 하단 조작법 텍스트 띠의 높이 (dp). 그라데이션 가독성 영역. 기본값: 120f */
private const val CD_LABEL_BAND_DP = 120f

/** 하단 조작법 텍스트의 하단 패딩 (dp, 네비게이션 바 회피). 기본값: 40f */
private const val CD_HINT_BOTTOM_PAD_DP = 40f

/** 진입 fade 애니메이션 duration (ms). 기본값: 260 */
const val CATEGORY_DRAWER_OPEN_DURATION_MS = 260

/** 단계(카테고리↔아이콘) drill-down 전환 duration (ms). 기본값: 220 */
private const val CD_STAGE_TRANSITION_MS = 220

/** 서랍 열림 시 에디터 본문 배경 블러 반경 (dp). 기본값: 6 */
const val CATEGORY_DRAWER_BACKDROP_BLUR_DP = 6

/**
 * 카테고리 아이콘 서랍의 단계.
 * 단계 전환은 상위(호출부)가 소유하여 뒤로가기 분기를 직접 제어한다.
 */
sealed interface IconDrawerStage {
    /** 1단계: 카테고리 선택 */
    object Category : IconDrawerStage

    /** 2단계: 선택한 카테고리([tab])의 아이콘 그리드 */
    data class Icons(val tab: IconCategoryTab) : IconDrawerStage
}

/** WatchDrawerGrid 셀 1개의 데이터. */
private class WatchCell(
    val element: FocusableElement,
    val label: String,
    val onActivate: () -> Unit,
    val icon: ImageVector,
)

/**
 * 카테고리 → 아이콘 2단계 애플워치 서랍.
 *
 * - [stage]가 [IconDrawerStage.Category]이면 카테고리 격자, [IconDrawerStage.Icons]이면 아이콘 격자를 렌더.
 * - 한 시점에 한 단계의 그리드만 컴포즈되므로(다른 단계는 dispose → SwipeFocusable 자동 unregister)
 *   같은 [scope] 안에서 traversal 후보가 섞이지 않는다.
 * - 카테고리 선택 시 [onStageChange]로 상위에 단계 전환을 위임, 아이콘 선택 시 [onPick] 호출.
 *
 * @param scope SwipeFocusable scope (예: EdgeEditorScope.IconSheet)
 */
@Composable
fun CategoryIconDrawer(
    controller: SwipeFocusController,
    stage: IconDrawerStage,
    onStageChange: (IconDrawerStage) -> Unit,
    selectedIconKey: String,
    anchorCenterInWindow: Offset,
    scope: Any,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 컨테이너(패널 카드·진입 애니·셀렉터 링)는 stage와 무관하게 한 번만 구성하고,
    // stage에 따라 cells/초기 포커스/힌트만 교체해 내부 컨텐츠만 새로 렌더링되게 한다.
    val cells: List<WatchCell>
    val initialFocus: FocusableElement?
    val fallbackHint: String
    when (stage) {
        is IconDrawerStage.Category -> {
            cells = buildList {
                add(
                    WatchCell(
                        element = EdgeEditorElement.IconCategoryItem(IconCategoryTab.All),
                        label = "전체",
                        onActivate = { onStageChange(IconDrawerStage.Icons(IconCategoryTab.All)) },
                        icon = Icons.Filled.Apps,
                    )
                )
                IconRegistry.categories.forEach { cat ->
                    val tab = IconCategoryTab.Real(cat)
                    add(
                        WatchCell(
                            element = EdgeEditorElement.IconCategoryItem(tab),
                            label = cat.displayName,
                            onActivate = { onStageChange(IconDrawerStage.Icons(tab)) },
                            icon = IconRegistry.get(cat.representativeKey),
                        )
                    )
                }
            }
            initialFocus = EdgeEditorElement.IconCategoryItem(IconCategoryTab.All)
            fallbackHint = "스와이프로 이동  •  탭으로 열기  •  길게 눌러 닫기"
        }

        is IconDrawerStage.Icons -> {
            val keys = IconRegistry.keysFor(stage.tab)
            cells = keys.map { key ->
                WatchCell(
                    element = EdgeEditorElement.IconSheetItem(key),
                    label = key,
                    onActivate = { onPick(key) },
                    icon = IconRegistry.get(key),
                )
            }
            val normalized = IconRegistry.normalizeIconKey(selectedIconKey)
            initialFocus = if (selectedIconKey.isNotEmpty() && keys.contains(normalized)) {
                EdgeEditorElement.IconSheetItem(normalized)
            } else {
                keys.firstOrNull()?.let { EdgeEditorElement.IconSheetItem(it) }
            }
            fallbackHint = "스와이프로 이동  •  탭으로 선택  •  길게 눌러 뒤로"
        }
    }

    // drill-down 전환: 진입(더 깊은 단계) = 0.92→1 확대, 복귀 = 1.08→1 축소. 그리드만 적용.
    val depth = if (stage is IconDrawerStage.Icons) 1 else 0
    val progress = remember { Animatable(1f) }
    var startScale by remember { mutableStateOf(1f) }
    var prevDepth by remember { mutableStateOf(depth) }
    LaunchedEffect(stage) {
        startScale = if (depth >= prevDepth) 0.92f else 1.08f
        prevDepth = depth
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(CD_STAGE_TRANSITION_MS, easing = FastOutSlowInEasing))
    }
    val gridScale = lerp(startScale, 1f, progress.value)
    val gridAlpha = progress.value

    WatchDrawerGrid(
        controller = controller,
        cells = cells,
        scope = scope,
        anchorCenterInWindow = anchorCenterInWindow,
        initialFocusElement = initialFocus,
        bottomHint = fallbackHint,
        gridScale = gridScale,
        gridAlpha = gridAlpha,
        modifier = modifier,
    )
}

/**
 * 애플워치 서랍 공용 그리드.
 * anchor 중심에서 확장, 중앙 셀렉터 링 = anchor 위치, 스와이프로 그리드 패닝, 가장자리 방사형 페이드.
 * 패닝은 [graphicsLayer] translation으로 처리해 layout bounds를 보존한다(컨트롤러 grid traversal 정상 동작).
 */
@Composable
private fun WatchDrawerGrid(
    controller: SwipeFocusController,
    cells: List<WatchCell>,
    scope: Any,
    anchorCenterInWindow: Offset,
    initialFocusElement: FocusableElement?,
    bottomHint: String,
    gridScale: Float,
    gridAlpha: Float,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val density = LocalDensity.current

    val cellPx = with(density) { CD_CELL_DP.dp.toPx() }
    val gapPx = with(density) { CD_CELL_GAP_DP.dp.toPx() }
    val stepPx = cellPx + gapPx

    val selectorHalfPx = with(density) { (CD_SELECTOR_SIZE_DP / 2f).dp.toPx() }
    val gradientClearPx = with(density) { CD_GRADIENT_CLEAR_RADIUS_DP.dp.toPx() }
    val gradientFadePx = with(density) { CD_GRADIENT_FADE_RADIUS_DP.dp.toPx() }

    var drawerWindowOffset by remember { mutableStateOf(Offset.Zero) }

    // stage 전환 시(initialFocusElement 변경) 새 단계의 초기 요소로 포커스 시드.
    // scope가 더 이상 active하지 않을 때(exit animation 중)는 setFocus를 건너뜀.
    // 서랍 닫힘 직후 selectedZone 변경으로 initialFocusElement가 바뀌는 경우,
    // 이 LaunchedEffect가 focus를 IconSheetItem으로 되돌려 이후 unregister 시 null이 되는 버그를 방지.
    LaunchedEffect(initialFocusElement) {
        if (initialFocusElement != null && controller.activeScope == scope) {
            controller.setFocus(initialFocusElement)
        }
    }

    BoxWithConstraints(
        modifier = modifier.onGloballyPositioned { coords ->
            drawerWindowOffset = coords.positionInWindow()
        }
    ) {
        val anchorX = anchorCenterInWindow.x - drawerWindowOffset.x
        val anchorY = anchorCenterInWindow.y - drawerWindowOffset.y

        val focusIndex = (cells.indexOfFirst { it.element == controller.currentFocus }
            .takeIf { it >= 0 } ?: 0)
            .coerceIn(0, (cells.size - 1).coerceAtLeast(0))
        val focusCol = focusIndex % CD_COLS
        val focusRow = focusIndex / CD_COLS

        // 그리드를 패닝해 focus 셀 중심이 anchor(= 진입 버튼 = 셀렉터 링 위치)에 오도록
        val targetTranslationX = anchorX - (focusCol * stepPx + cellPx / 2f)
        val targetTranslationY = anchorY - (focusRow * stepPx + cellPx / 2f)

        val animX by animateFloatAsState(
            targetValue = targetTranslationX,
            animationSpec = tween(durationMillis = CD_PANNING_DURATION_MS),
            label = "drawerX",
        )
        val animY by animateFloatAsState(
            targetValue = targetTranslationY,
            animationSpec = tween(durationMillis = CD_PANNING_DURATION_MS),
            label = "drawerY",
        )

        // 경계 히트 시 셀렉터 링 점멸 (이 그리드의 셀에 대한 flash만 반응)
        val selectorFlashAlpha = remember { Animatable(0f) }
        val flashForSelector = controller.flashSignal
            ?.takeIf { sig -> cells.any { it.element == sig.element } }
        LaunchedEffect(flashForSelector) {
            if (flashForSelector != null) {
                selectorFlashAlpha.snapTo(1f)
                selectorFlashAlpha.animateTo(0f, animationSpec = tween(durationMillis = 350))
            } else if (selectorFlashAlpha.value > 0f) {
                selectorFlashAlpha.animateTo(0f, animationSpec = tween(durationMillis = 150))
            }
        }
        val selectorBorderColor = lerp(cs.primary, cs.error, selectorFlashAlpha.value)

        // 상단 이름: 현재 포커스 셀의 라벨 (카테고리명/아이콘명)
        val focusedLabel = cells.firstOrNull { it.element == controller.currentFocus }?.label ?: ""

        val scrim = Color.Black.copy(alpha = CD_SCRIM_ALPHA)

        // drill-down scale 원점 = anchor(셀렉터) 화면 비율
        val gridOriginX = if (constraints.maxWidth > 0) (anchorX / constraints.maxWidth).coerceIn(0f, 1f) else 0.5f
        val gridOriginY = if (constraints.maxHeight > 0) (anchorY / constraints.maxHeight).coerceIn(0f, 1f) else 0.5f

        // ── 딤 배경 (전체 화면) ──
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(scrim)
        )

        // ── 패닝 그리드 (전체 화면, anchor 중심 패닝) + drill-down scale/alpha (셀렉터 원점) ──
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    scaleX = gridScale
                    scaleY = gridScale
                    alpha = gridAlpha
                    transformOrigin = TransformOrigin(gridOriginX, gridOriginY)
                }
        ) {
            Box(
                modifier = Modifier.graphicsLayer {
                    translationX = animX
                    translationY = animY
                }
            ) {
                val rows = (cells.size + CD_COLS - 1) / CD_COLS
                Column(verticalArrangement = Arrangement.spacedBy(CD_CELL_GAP_DP.dp)) {
                    repeat(rows) { rowIdx ->
                        Row(horizontalArrangement = Arrangement.spacedBy(CD_CELL_GAP_DP.dp)) {
                            repeat(CD_COLS) { colIdx ->
                                val idx = rowIdx * CD_COLS + colIdx
                                if (idx < cells.size) {
                                    val cell = cells[idx]
                                    SwipeFocusable(
                                        element = cell.element,
                                        scope = scope,
                                        shape = RoundedCornerShape(12.dp),
                                        showBorderHighlight = false,
                                        onActivate = cell.onActivate,
                                        gridRow = rowIdx,
                                    ) {
                                        DrawerCell(icon = cell.icon, contentDescription = cell.label)
                                    }
                                } else {
                                    Box(Modifier.size(CD_CELL_DP.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── 방사형 비네트 (anchor 중심, 가장자리는 딤으로 자연스럽게 페이드) ──
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.radialGradient(
                        0f to Color.Transparent,
                        (gradientClearPx / gradientFadePx) to Color.Transparent,
                        1f to scrim,
                        center = Offset(anchorX, anchorY),
                        radius = gradientFadePx,
                    )
                )
        )

        // ── 셀렉터 링 (anchor = 진입 버튼 위치에 고정) ──
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        (anchorX - selectorHalfPx).roundToInt(),
                        (anchorY - selectorHalfPx).roundToInt(),
                    )
                }
                .size(CD_SELECTOR_SIZE_DP.dp)
                .border(
                    width = CD_SELECTOR_BORDER_DP.dp,
                    color = selectorBorderColor,
                    shape = RoundedCornerShape(12.dp),
                )
        )

        // ── 이름 (셀렉터 링 바로 위): 현재 포커스 카테고리명/아이콘명 ──
        // 셀렉터(anchor)를 따라다니며 바로 위에 표시. 반투명 pill 배경으로 아이콘 위에서도 또렷하게.
        if (focusedLabel.isNotEmpty()) {
            var labelSize by remember { mutableStateOf(IntSize.Zero) }
            val labelGapPx = with(density) { 10.dp.toPx() }
            Box(
                modifier = Modifier
                    .onSizeChanged { labelSize = it }
                    .offset {
                        val x = (anchorX - labelSize.width / 2f)
                            .coerceIn(0f, (constraints.maxWidth - labelSize.width).coerceAtLeast(0).toFloat())
                        val y = anchorY - selectorHalfPx - labelGapPx - labelSize.height
                        IntOffset(x.roundToInt(), y.roundToInt())
                    }
                    .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 5.dp),
            ) {
                Text(
                    text = focusedLabel,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // ── 조작법 안내 (화면 하단 고정) ──
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(CD_LABEL_BAND_DP.dp)
                .background(Brush.verticalGradient(0f to Color.Transparent, 1f to scrim)),
        ) {
            Text(
                text = bottomHint,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.75f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = CD_HINT_BOTTOM_PAD_DP.dp, start = 24.dp, end = 24.dp),
            )
        }
    }
}

/**
 * 서랍 그리드의 단일 셀.
 * [LocalSwipeFocused]로 포커스 여부를 읽어 비포커스 셀은 축소+페이드.
 */
@Composable
private fun DrawerCell(
    icon: ImageVector,
    contentDescription: String,
) {
    val cs = MaterialTheme.colorScheme
    val isFocused = LocalSwipeFocused.current

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1f else CD_UNFOCUSED_SCALE,
        animationSpec = tween(durationMillis = CD_PANNING_DURATION_MS),
        label = "cellScale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else CD_UNFOCUSED_ALPHA,
        animationSpec = tween(durationMillis = CD_PANNING_DURATION_MS),
        label = "cellAlpha",
    )

    Box(
        modifier = Modifier
            .size(CD_CELL_DP.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .clip(RoundedCornerShape(12.dp))
            .background(cs.surface),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isFocused) cs.primary else cs.onSurfaceVariant,
            modifier = Modifier.size(CD_ICON_SIZE_DP.dp),
        )
    }
}
