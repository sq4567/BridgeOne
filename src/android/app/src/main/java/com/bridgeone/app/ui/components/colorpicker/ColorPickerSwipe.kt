package com.bridgeone.app.ui.components.colorpicker

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
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
import com.bridgeone.app.ui.common.ColorCategory
import com.bridgeone.app.ui.common.ColorCategoryTab
import com.bridgeone.app.ui.common.ColorCodec
import com.bridgeone.app.ui.common.ColorPickerConstants
import com.bridgeone.app.ui.common.ColorRegistry
import com.bridgeone.app.ui.common.swipe.LocalSwipeFocused
import com.bridgeone.app.ui.common.swipe.SwipeFocusController
import com.bridgeone.app.ui.common.swipe.SwipeFocusable
import com.bridgeone.app.ui.common.swipe.SwipeMode
import kotlin.math.roundToInt

// ── 파일 로컬 상수 ──────────────────────────────────────────
private val SWATCH_COLS = ColorPickerConstants.SWATCH_COLS
private val SWATCH_CELL_DP = ColorPickerConstants.SWATCH_CELL_DP
private val SWATCH_GAP_DP = ColorPickerConstants.SWATCH_CELL_GAP_DP
private val SWATCH_CORNER_DP = ColorPickerConstants.SWATCH_CORNER_DP

/** 배경 딤 알파. 기본값: 0.72f */
private const val CP_SCRIM_ALPHA = 0.72f

/** 앵커 중심 선명 반경 (dp). 기본값: 120f */
private const val CP_GRADIENT_CLEAR_RADIUS_DP = 120f

/** 딤 완전 소멸 반경 (dp). 기본값: 280f */
private const val CP_GRADIENT_FADE_RADIUS_DP = 280f

/** 하단 힌트 띠 높이 (dp). 기본값: 100f */
private const val CP_LABEL_BAND_DP = 100f

/** 하단 힌트 텍스트 하단 패딩 (dp). 기본값: 40f */
private const val CP_HINT_BOTTOM_PAD_DP = 40f

/** 그리드 패닝 애니메이션 duration (ms). 기본값: 200 */
private const val CP_PANNING_DURATION_MS = 200

/** 비포커스 셀 스케일. 기본값: 0.82f */
private const val CP_UNFOCUSED_SCALE = 0.82f

/** 비포커스 셀 알파. 기본값: 0.38f */
private const val CP_UNFOCUSED_ALPHA = 0.38f

/** 단계 전환 애니메이션 duration (ms). 기본값: 220 */
private const val CP_STAGE_TRANSITION_MS = 220

/** HSV 슬라이더 트랙 높이 (dp). 기본값: 28f */
private const val SLIDER_TRACK_HEIGHT_DP = 28f

/** HSV 슬라이더 thumb 지름 (dp). 기본값: 20f */
private const val SLIDER_THUMB_DP = 20f

/** 카테고리 탭 ID: 전체(All). */
private const val ALL_TAB_ID = "__all__"

/**
 * SWIPE 모드 전용 컬러 피커 (3단계 drill-down).
 *
 * - [ColorPickerStage.Category]: 카테고리 선택 그리드 (진입 기본)
 * - [ColorPickerStage.Swatches]: 선택 카테고리의 팔레트 스와치 그리드
 * - [ColorPickerStage.DirectInput]: HSV 슬라이더 직접 입력
 *
 * @param stage 현재 화면 단계 (호출부에서 소유)
 * @param onStageChange 단계 전환 요청 콜백
 */
@Composable
fun ColorPickerSwipe(
    controller: SwipeFocusController,
    pickerScope: Any,
    selectedColorHex: String,
    anchorCenterInWindow: Offset,
    stage: ColorPickerStage,
    onStageChange: (ColorPickerStage) -> Unit,
    onPick: (hex: String) -> Unit,
    /** 현재 롱프레스 시 확정될 후보 hex를 부모에 보고. Category 단계 또는 ExpandToggle 포커스 시 null. */
    onCommitCandidateChange: (hex: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val density = LocalDensity.current

    // ── HSV 상태 ──
    val initialColor = ColorCodec.hexToColorOrNull(selectedColorHex) ?: cs.primary
    val initialHsv = remember(selectedColorHex) { ColorCodec.colorToHsv(initialColor) }
    var hsv by remember { mutableStateOf(initialHsv) }
    val previewColor = ColorCodec.hsvToColor(hsv[0], hsv[1], hsv[2])
    val previewHex = ColorCodec.colorToHex(previewColor)

    // ── 카테고리 탭 목록 (Category 단계) ──
    val categoryTabs: List<ColorCategoryTab> = remember {
        listOf(ColorCategoryTab.All) + ColorRegistry.categories.map { ColorCategoryTab.Real(it) }
    }
    val categoryTabIds: List<String> = remember {
        categoryTabs.map { tab ->
            when (tab) {
                is ColorCategoryTab.All  -> ALL_TAB_ID
                is ColorCategoryTab.Real -> tab.category.id
            }
        }
    }

    // ── 현재 스와치 목록 (Swatches 단계) ──
    val currentSwatchTab = (stage as? ColorPickerStage.Swatches)?.tab ?: ColorCategoryTab.All
    val currentSwatchColors = remember(currentSwatchTab) { ColorRegistry.colorsFor(currentSwatchTab) }
    val allSwatchHexes = remember(currentSwatchTab) { currentSwatchColors.map { ColorCodec.colorToHex(it) } }
    val swatchCount = allSwatchHexes.size

    // ── 초기 포커스 ──
    LaunchedEffect(stage) {
        when (stage) {
            is ColorPickerStage.Category -> {
                controller.setFocus(ColorPickerElement.CategoryCell(ALL_TAB_ID))
            }
            is ColorPickerStage.Swatches -> {
                val nearestHex = allSwatchHexes.firstOrNull { it == selectedColorHex }
                    ?: allSwatchHexes.firstOrNull() ?: return@LaunchedEffect
                controller.setFocus(ColorPickerElement.Swatch(nearestHex))
            }
            is ColorPickerStage.DirectInput -> {
                controller.setFocus(ColorPickerElement.HueSlider)
            }
        }
    }

    // ── 단계 전환 애니메이션 (CategoryIconDrawer drill-down 패턴) ──
    val depth = when (stage) {
        is ColorPickerStage.Category    -> 0
        is ColorPickerStage.Swatches    -> 1
        is ColorPickerStage.DirectInput -> 2
    }
    val transitionProgress = remember { Animatable(1f) }
    var startScale by remember { mutableStateOf(1f) }
    var prevDepth by remember { mutableStateOf(depth) }
    LaunchedEffect(stage) {
        startScale = if (depth >= prevDepth) 0.92f else 1.08f
        prevDepth = depth
        transitionProgress.snapTo(0f)
        transitionProgress.animateTo(
            1f,
            animationSpec = tween(CP_STAGE_TRANSITION_MS, easing = FastOutSlowInEasing),
        )
    }
    val contentScale = androidx.compose.ui.util.lerp(startScale, 1f, transitionProgress.value)
    val contentAlpha = transitionProgress.value

    // ── 초기 스와치 포커스 인덱스 ──
    val initialSwatchIndex = remember(selectedColorHex, currentSwatchTab) {
        val idx = allSwatchHexes.indexOfFirst { it == selectedColorHex }
        if (idx >= 0) idx else 0
    }

    // ── 포커스 인덱스 (그리드 패닝용) ──
    val focusedElement = controller.currentFocus
    val focusIndex: Int = when (stage) {
        is ColorPickerStage.Category -> {
            when (val e = focusedElement) {
                is ColorPickerElement.CategoryCell -> categoryTabIds.indexOf(e.tabId).coerceAtLeast(0)
                ColorPickerElement.ExpandToggle -> categoryTabs.size
                else -> 0
            }
        }
        is ColorPickerStage.Swatches -> when (val e = focusedElement) {
            is ColorPickerElement.Swatch      -> allSwatchHexes.indexOf(e.hex).coerceAtLeast(0)
            ColorPickerElement.ExpandToggle   -> swatchCount
            else                              -> initialSwatchIndex
        }
        is ColorPickerStage.DirectInput -> 0
    }
    val focusCol = focusIndex % SWATCH_COLS
    val focusRow = focusIndex / SWATCH_COLS

    val cellPx = with(density) { SWATCH_CELL_DP.dp.toPx() }
    val gapPx = with(density) { SWATCH_GAP_DP.dp.toPx() }
    val stepPx = cellPx + gapPx
    val selectorHalfPx = cellPx / 2f
    val gradientClearPx = with(density) { CP_GRADIENT_CLEAR_RADIUS_DP.dp.toPx() }
    val gradientFadePx = with(density) { CP_GRADIENT_FADE_RADIUS_DP.dp.toPx() }
    val scrim = Color.Black.copy(alpha = CP_SCRIM_ALPHA)

    // ── 셀렉터 링 점멸 ──
    val selectorFlashAlpha = remember { Animatable(0f) }
    val flashSignal = controller.flashSignal
    LaunchedEffect(flashSignal) {
        if (flashSignal != null) {
            selectorFlashAlpha.snapTo(1f)
            selectorFlashAlpha.animateTo(0f, tween(350))
        } else if (selectorFlashAlpha.value > 0f) {
            selectorFlashAlpha.animateTo(0f, tween(150))
        }
    }
    val selectorBorderColor = lerp(cs.primary, cs.error, selectorFlashAlpha.value)

    // ── 포커스 프리뷰 pill 정보 ──
    val pillColor: Color
    val pillHex: String
    val pillLabel: String
    when (stage) {
        is ColorPickerStage.Category -> {
            if (focusedElement == ColorPickerElement.ExpandToggle) {
                pillColor = cs.primary
                pillHex = ""
                pillLabel = "직접 입력"
            } else {
                val focusedTabId = (focusedElement as? ColorPickerElement.CategoryCell)?.tabId
                val focusedCategory = if (focusedTabId != null && focusedTabId != ALL_TAB_ID)
                    ColorCategory.entries.firstOrNull { it.id == focusedTabId } else null
                pillColor = if (focusedCategory != null) Color(focusedCategory.representativeArgb) else cs.primary
                pillHex = ""
                pillLabel = focusedCategory?.displayName ?: "전체"
            }
        }
        is ColorPickerStage.Swatches -> {
            val fc = (focusedElement as? ColorPickerElement.Swatch)
                ?.let { ColorCodec.hexToColorOrNull(it.hex) }
            if (fc != null) {
                pillColor = fc
                pillHex = ColorCodec.colorToHex(fc)
                pillLabel = pillHex.toDisplayHex()
            } else {
                pillColor = cs.primary
                pillHex = ""
                pillLabel = "직접 입력"
            }
        }
        is ColorPickerStage.DirectInput -> {
            pillColor = previewColor
            pillHex = previewHex
            pillLabel = previewHex.toDisplayHex()
        }
    }

    // pillHex(확정 후보)가 바뀔 때마다 부모에 보고
    LaunchedEffect(pillHex) { onCommitCandidateChange(pillHex.ifEmpty { null }) }

    // DirectInput 패널 측정 크기
    var diPanelSize by remember { mutableStateOf(IntSize.Zero) }
    var drawerWindowOffset by remember { mutableStateOf(Offset.Zero) }

    BoxWithConstraints(
        modifier = modifier.onGloballyPositioned { coords ->
            drawerWindowOffset = coords.positionInWindow()
        }
    ) {
        val anchorX = anchorCenterInWindow.x - drawerWindowOffset.x
        val anchorY = anchorCenterInWindow.y - drawerWindowOffset.y

        // 그리드 패닝: focusCell 중심 → anchorX/Y
        val targetX = anchorX - (focusCol * stepPx + cellPx / 2f)
        val targetY = anchorY - (focusRow * stepPx + cellPx / 2f)
        val animX by animateFloatAsState(targetX, tween(CP_PANNING_DURATION_MS), label = "cpX")
        val animY by animateFloatAsState(targetY, tween(CP_PANNING_DURATION_MS), label = "cpY")

        val gridOriginX = if (constraints.maxWidth > 0) (anchorX / constraints.maxWidth).coerceIn(0f, 1f) else 0.5f
        val gridOriginY = if (constraints.maxHeight > 0) (anchorY / constraints.maxHeight).coerceIn(0f, 1f) else 0.5f

        // DirectInput 패널 위치
        val panelPadPx = with(density) { 16.dp.toPx() }
        val previewHalfPx = with(density) { ColorPickerConstants.COLOR_PREVIEW_SIZE_DP.dp.toPx() } / 2f
        val rawPanelX = anchorX - panelPadPx - previewHalfPx
        val rawPanelY = anchorY - panelPadPx - previewHalfPx
        val panelX = rawPanelX.coerceIn(0f, (constraints.maxWidth - diPanelSize.width).coerceAtLeast(0).toFloat())
        val panelY = rawPanelY.coerceIn(0f, (constraints.maxHeight - diPanelSize.height).coerceAtLeast(0).toFloat())

        // ── 1. 전체 화면 scrim ──
        Box(Modifier.matchParentSize().background(scrim))

        // ── 2. 단계별 주 콘텐츠 (전환 scale/alpha 적용) ──
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    scaleX = contentScale
                    scaleY = contentScale
                    alpha = contentAlpha
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(gridOriginX, gridOriginY)
                }
        ) {
            when (stage) {
                is ColorPickerStage.Category -> CategoryContent(
                    controller = controller,
                    pickerScope = pickerScope,
                    categoryTabs = categoryTabs,
                    categoryTabIds = categoryTabIds,
                    animX = animX,
                    animY = animY,
                    onCategorySelect = { tab -> onStageChange(ColorPickerStage.Swatches(tab)) },
                    onDirectInput = { onStageChange(ColorPickerStage.DirectInput(sourceTab = null)) },
                )
                is ColorPickerStage.Swatches -> SwatchesContent(
                    controller = controller,
                    pickerScope = pickerScope,
                    allSwatchHexes = allSwatchHexes,
                    selectedColorHex = selectedColorHex,
                    swatchCount = swatchCount,
                    currentTab = stage.tab,
                    animX = animX,
                    animY = animY,
                    onPick = onPick,
                    onEnterDirectInput = { sourceTab ->
                        onStageChange(ColorPickerStage.DirectInput(sourceTab))
                    },
                )
                is ColorPickerStage.DirectInput -> DirectInputContent(
                    hsv = hsv,
                    previewColor = previewColor,
                    previewHex = previewHex,
                    pickerScope = pickerScope,
                    swatchCount = swatchCount,
                    controller = controller,
                    onHsvChange = { hsv = it },
                    onPick = onPick,
                    modifier = Modifier
                        .onSizeChanged { diPanelSize = it }
                        .offset { IntOffset(panelX.roundToInt(), panelY.roundToInt()) },
                )
            }
        }

        // ── 3. 방사형 비네트 ──
        Box(
            Modifier.matchParentSize().background(
                Brush.radialGradient(
                    0f to Color.Transparent,
                    (gradientClearPx / gradientFadePx) to Color.Transparent,
                    1f to scrim,
                    center = Offset(anchorX, anchorY),
                    radius = gradientFadePx,
                )
            )
        )

        // ── 4. 셀렉터 링 (앵커 고정) ──
        Box(
            Modifier
                .offset {
                    IntOffset(
                        (anchorX - selectorHalfPx).roundToInt(),
                        (anchorY - selectorHalfPx).roundToInt(),
                    )
                }
                .size(SWATCH_CELL_DP.dp)
                .background(
                    if (stage is ColorPickerStage.DirectInput) pillColor.copy(alpha = 0.3f)
                    else Color.Transparent,
                    RoundedCornerShape(SWATCH_CORNER_DP.dp),
                )
                .border(
                    ColorPickerConstants.SELECTOR_BORDER_DP.dp,
                    selectorBorderColor,
                    RoundedCornerShape(SWATCH_CORNER_DP.dp),
                )
        )

        // ── 5. 포커스 프리뷰 pill (Category + Swatches 단계, DirectInput 단계는 패널에 이미 표시) ──
        var pillSize by remember { mutableStateOf(IntSize.Zero) }
        if (stage !is ColorPickerStage.DirectInput) {
            val pillGapPx = with(density) { 10.dp.toPx() }
            Box(
                modifier = Modifier
                    .onSizeChanged { pillSize = it }
                    .offset {
                        val x = (anchorX - pillSize.width / 2f)
                            .coerceIn(0f, (constraints.maxWidth - pillSize.width).coerceAtLeast(0).toFloat())
                        val y = anchorY - selectorHalfPx - pillGapPx - pillSize.height
                        IntOffset(x.roundToInt(), y.coerceAtLeast(0f).roundToInt())
                    }
                    .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 5.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (pillHex.isNotEmpty()) {
                        Box(
                            Modifier
                                .size(14.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(pillColor)
                        )
                    }
                    Text(
                        text = pillLabel,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        // ── 6. 하단 힌트 바 ──
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(CP_LABEL_BAND_DP.dp)
                .background(Brush.verticalGradient(0f to Color.Transparent, 1f to scrim)),
        ) {
            val hint = when (stage) {
                is ColorPickerStage.Category    -> "스와이프  •  탭으로 카테고리 선택  •  길게 눌러 닫기"
                is ColorPickerStage.Swatches    -> "스와이프  •  탭으로 선택  •  길게 눌러 뒤로"
                is ColorPickerStage.DirectInput ->
                    if (controller.mode == SwipeMode.MANIPULATION) "좌우로 조절  •  탭으로 완료  •  길게 눌러 뒤로"
                    else "상하로 슬라이더 선택  •  탭으로 조절  •  길게 눌러 뒤로"
            }
            Text(
                text = hint,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.75f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = CP_HINT_BOTTOM_PAD_DP.dp, start = 24.dp, end = 24.dp),
            )
        }
    }
}

// ============================================================
// Category 단계 콘텐츠
// ============================================================

@Composable
private fun CategoryContent(
    controller: SwipeFocusController,
    pickerScope: Any,
    categoryTabs: List<ColorCategoryTab>,
    categoryTabIds: List<String>,
    animX: Float,
    animY: Float,
    onCategorySelect: (ColorCategoryTab) -> Unit,
    onDirectInput: () -> Unit,
) {
    Box(
        modifier = Modifier.graphicsLayer {
            translationX = animX
            translationY = animY
        }
    ) {
        val totalCells = categoryTabs.size + 1
        val rows = (totalCells + SWATCH_COLS - 1) / SWATCH_COLS
        Column(verticalArrangement = Arrangement.spacedBy(SWATCH_GAP_DP.dp)) {
            repeat(rows) { rowIdx ->
                Row(horizontalArrangement = Arrangement.spacedBy(SWATCH_GAP_DP.dp)) {
                    repeat(SWATCH_COLS) { colIdx ->
                        val idx = rowIdx * SWATCH_COLS + colIdx
                        when {
                            idx < categoryTabs.size -> {
                                val tab = categoryTabs[idx]
                                val tabId = categoryTabIds[idx]
                                SwipeFocusable(
                                    element = ColorPickerElement.CategoryCell(tabId),
                                    scope = pickerScope,
                                    showBorderHighlight = false,
                                    onActivate = { onCategorySelect(tab) },
                                    shape = RoundedCornerShape(SWATCH_CORNER_DP.dp),
                                    gridRow = rowIdx,
                                ) {
                                    CategoryDrawerCell(
                                        tab = tab,
                                        sizeDp = SWATCH_CELL_DP,
                                    )
                                }
                            }
                            idx == categoryTabs.size -> {
                                SwipeFocusable(
                                    element = ColorPickerElement.ExpandToggle,
                                    scope = pickerScope,
                                    showBorderHighlight = false,
                                    onActivate = { onDirectInput() },
                                    shape = RoundedCornerShape(SWATCH_CORNER_DP.dp),
                                    gridRow = rowIdx,
                                ) {
                                    ExpandToggleDrawerCell(sizeDp = SWATCH_CELL_DP)
                                }
                            }
                            else -> Box(Modifier.size(SWATCH_CELL_DP.dp))
                        }
                    }
                }
            }
        }
    }
}

// ============================================================
// Swatches 단계 콘텐츠
// ============================================================

@Composable
private fun SwatchesContent(
    controller: SwipeFocusController,
    pickerScope: Any,
    allSwatchHexes: List<String>,
    selectedColorHex: String,
    swatchCount: Int,
    currentTab: ColorCategoryTab,
    animX: Float,
    animY: Float,
    onPick: (String) -> Unit,
    onEnterDirectInput: (sourceTab: ColorCategoryTab) -> Unit,
) {
    Box(
        modifier = Modifier.graphicsLayer {
            translationX = animX
            translationY = animY
        }
    ) {
        val totalCells = swatchCount + 1
        val rows = (totalCells + SWATCH_COLS - 1) / SWATCH_COLS
        Column(verticalArrangement = Arrangement.spacedBy(SWATCH_GAP_DP.dp)) {
            repeat(rows) { rowIdx ->
                Row(horizontalArrangement = Arrangement.spacedBy(SWATCH_GAP_DP.dp)) {
                    repeat(SWATCH_COLS) { colIdx ->
                        val idx = rowIdx * SWATCH_COLS + colIdx
                        when {
                            idx < swatchCount -> {
                                val swatchHex = allSwatchHexes[idx]
                                val swatchColor = ColorCodec.hexToColorOrNull(swatchHex) ?: Color.Gray
                                val isSelected = swatchHex == selectedColorHex
                                SwipeFocusable(
                                    element = ColorPickerElement.Swatch(swatchHex),
                                    scope = pickerScope,
                                    showBorderHighlight = false,
                                    onActivate = { onPick(swatchHex) },
                                    shape = RoundedCornerShape(SWATCH_CORNER_DP.dp),
                                    gridRow = rowIdx,
                                ) {
                                    SwatchDrawerCell(
                                        color = swatchColor,
                                        isSelected = isSelected,
                                        sizeDp = SWATCH_CELL_DP,
                                    )
                                }
                            }
                            idx == swatchCount -> {
                                SwipeFocusable(
                                    element = ColorPickerElement.ExpandToggle,
                                    scope = pickerScope,
                                    showBorderHighlight = false,
                                    onActivate = { onEnterDirectInput(currentTab) },
                                    shape = RoundedCornerShape(SWATCH_CORNER_DP.dp),
                                    gridRow = rowIdx,
                                ) {
                                    ExpandToggleDrawerCell(sizeDp = SWATCH_CELL_DP)
                                }
                            }
                            else -> Box(Modifier.size(SWATCH_CELL_DP.dp))
                        }
                    }
                }
            }
        }
    }
}

// ============================================================
// DirectInput 단계 콘텐츠
// ============================================================

@Composable
private fun DirectInputContent(
    hsv: FloatArray,
    previewColor: Color,
    previewHex: String,
    pickerScope: Any,
    swatchCount: Int,
    controller: SwipeFocusController,
    onHsvChange: (FloatArray) -> Unit,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val swatchRows = (swatchCount + SWATCH_COLS - 1) / SWATCH_COLS
    val hsvBaseRow = swatchRows + 1

    Box(
        modifier = modifier
            .widthIn(max = ColorPickerConstants.HSV_PANEL_MAX_WIDTH_DP.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(cs.surfaceVariant.copy(alpha = 0.97f))
            .padding(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(ColorPickerConstants.COLOR_PREVIEW_SIZE_DP.dp)
                        .clip(RoundedCornerShape(SWATCH_CORNER_DP.dp))
                        .background(previewColor)
                        .border(1.dp, cs.outline.copy(alpha = 0.4f), RoundedCornerShape(SWATCH_CORNER_DP.dp))
                )
                Text(
                    text = previewHex.toDisplayHex(),
                    fontSize = 13.sp,
                    color = cs.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                SwipeFocusable(
                    element = ColorPickerElement.Apply,
                    scope = pickerScope,
                    onActivate = { onPick(previewHex) },
                    shape = RoundedCornerShape(8.dp),
                    showBorderHighlight = false,
                    gridRow = hsvBaseRow - 1,
                ) {
                    val isApplyFocused = LocalSwipeFocused.current
                    Box(
                        modifier = Modifier
                            .background(
                                if (isApplyFocused) cs.primary else Color.White,
                                RoundedCornerShape(8.dp),
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "적용",
                            fontSize = 12.sp,
                            color = if (isApplyFocused) cs.onPrimary else cs.primary,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            HsvSlider(
                label = "H",
                value = hsv[0] / 360f,
                trackBrush = Brush.horizontalGradient(
                    (0..12).map { i ->
                        ColorCodec.hsvToColor(i * 30f, hsv[1].coerceAtLeast(0.5f), hsv[2].coerceAtLeast(0.5f))
                    }
                ),
                thumbTrackColor = ColorCodec.hsvToColor(hsv[0], hsv[1].coerceAtLeast(0.5f), hsv[2].coerceAtLeast(0.5f)),
                element = ColorPickerElement.HueSlider,
                scope = pickerScope,
                controller = controller,
                gridRow = hsvBaseRow,
                onManipulate = { deltaPx, width ->
                    val delta = (deltaPx / width) * 360f
                    onHsvChange(floatArrayOf((hsv[0] + delta).coerceIn(0f, 360f), hsv[1], hsv[2]))
                },
            )

            HsvSlider(
                label = "S",
                value = hsv[1],
                trackBrush = Brush.horizontalGradient(
                    listOf(
                        ColorCodec.hsvToColor(hsv[0], 0f, hsv[2]),
                        ColorCodec.hsvToColor(hsv[0], 1f, hsv[2]),
                    )
                ),
                thumbTrackColor = ColorCodec.hsvToColor(hsv[0], hsv[1], hsv[2].coerceAtLeast(0.5f)),
                element = ColorPickerElement.SatSlider,
                scope = pickerScope,
                controller = controller,
                gridRow = hsvBaseRow + 1,
                onManipulate = { deltaPx, width ->
                    val delta = deltaPx / width
                    onHsvChange(floatArrayOf(hsv[0], (hsv[1] + delta).coerceIn(0f, 1f), hsv[2]))
                },
            )

            HsvSlider(
                label = "V",
                value = hsv[2],
                trackBrush = Brush.horizontalGradient(
                    listOf(Color.Black, ColorCodec.hsvToColor(hsv[0], hsv[1], 1f))
                ),
                thumbTrackColor = ColorCodec.hsvToColor(hsv[0], hsv[1].coerceAtLeast(0.5f), hsv[2]),
                element = ColorPickerElement.ValSlider,
                scope = pickerScope,
                controller = controller,
                gridRow = hsvBaseRow + 2,
                onManipulate = { deltaPx, width ->
                    val delta = deltaPx / width
                    onHsvChange(floatArrayOf(hsv[0], hsv[1], (hsv[2] + delta).coerceIn(0f, 1f)))
                },
            )
        }
    }
}

// ============================================================
// 개별 셀 컴포저블
// ============================================================

@Composable
private fun CategoryDrawerCell(tab: ColorCategoryTab, sizeDp: Float) {
    val cs = MaterialTheme.colorScheme
    val isFocused = LocalSwipeFocused.current
    val scale by animateFloatAsState(
        if (isFocused) 1f else CP_UNFOCUSED_SCALE, tween(CP_PANNING_DURATION_MS), label = "cs",
    )
    val alpha by animateFloatAsState(
        if (isFocused) 1f else CP_UNFOCUSED_ALPHA, tween(CP_PANNING_DURATION_MS), label = "ca",
    )
    val background: Brush = when (tab) {
        is ColorCategoryTab.All -> Brush.horizontalGradient(
            listOf(
                Color(0xFFF32121), Color(0xFF2196F3), Color(0xFF84E268), Color(0xFFB552F6),
            )
        )
        is ColorCategoryTab.Real -> Brush.linearGradient(
            listOf(Color(tab.category.representativeArgb), Color(tab.category.representativeArgb))
        )
    }
    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }
            .clip(RoundedCornerShape(SWATCH_CORNER_DP.dp))
            .background(background),
    )
}

@Composable
private fun SwatchDrawerCell(color: Color, isSelected: Boolean, sizeDp: Float) {
    val isFocused = LocalSwipeFocused.current
    val scale by animateFloatAsState(
        if (isFocused) 1f else CP_UNFOCUSED_SCALE, tween(CP_PANNING_DURATION_MS), label = "ss",
    )
    val alpha by animateFloatAsState(
        if (isFocused) 1f else CP_UNFOCUSED_ALPHA, tween(CP_PANNING_DURATION_MS), label = "sa",
    )
    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }
            .clip(RoundedCornerShape(SWATCH_CORNER_DP.dp))
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Box(
                Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (color.luminance() > 0.5f) Color.Black.copy(0.6f)
                        else Color.White.copy(0.8f)
                    )
            )
        }
    }
}

@Composable
private fun ExpandToggleDrawerCell(sizeDp: Float) {
    val cs = MaterialTheme.colorScheme
    val isFocused = LocalSwipeFocused.current
    val scale by animateFloatAsState(
        if (isFocused) 1f else CP_UNFOCUSED_SCALE, tween(CP_PANNING_DURATION_MS), label = "es",
    )
    val alpha by animateFloatAsState(
        if (isFocused) 1f else CP_UNFOCUSED_ALPHA, tween(CP_PANNING_DURATION_MS), label = "ea",
    )
    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }
            .clip(RoundedCornerShape(SWATCH_CORNER_DP.dp))
            .background(if (isFocused) cs.primary else cs.surface),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Tune,
            contentDescription = "직접 입력",
            tint = if (isFocused) cs.onPrimary else cs.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

private fun Color.luminance(): Float = 0.2126f * red + 0.7152f * green + 0.0722f * blue

/** "#FFRRGGBB" → "#RRGGBB" 변환 (alpha가 FF일 때만). 표시용. */
private fun String.toDisplayHex(): String =
    if (startsWith("#FF") && length == 9) "#${substring(3)}" else this

private fun complementOf(color: Color): Color {
    val hsv = ColorCodec.colorToHsv(color)
    return ColorCodec.hsvToColor(
        (hsv[0] + 180f).mod(360f),
        hsv[1].coerceAtLeast(0.7f),
        hsv[2].coerceAtLeast(0.7f),
    )
}

@Composable
private fun HsvSlider(
    label: String,
    value: Float,
    trackBrush: Brush,
    thumbTrackColor: Color,
    element: ColorPickerElement,
    scope: Any,
    controller: SwipeFocusController,
    gridRow: Int,
    onManipulate: (Float, Float) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val isLabelFocused = controller.currentFocus == element
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = if (isLabelFocused) cs.primary else cs.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.width(12.dp),
        )
        SwipeFocusable(
            element = element,
            scope = scope,
            manipulatable = true,
            onManipulate = { deltaPx, width -> onManipulate(deltaPx, width) },
            shape = RoundedCornerShape(6.dp),
            showBorderHighlight = false,
            gridRow = gridRow,
            modifier = Modifier.weight(1f).height(SLIDER_TRACK_HEIGHT_DP.dp),
        ) {
            val isFocused = LocalSwipeFocused.current
            val inManip = isFocused && controller.mode == SwipeMode.MANIPULATION
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(SLIDER_TRACK_HEIGHT_DP.dp)
                    .clip(RoundedCornerShape(6.dp))
            ) {
                Box(Modifier.matchParentSize().background(trackBrush))
                val thumbFraction = value.coerceIn(0f, 1f)
                val trackWidthPx = constraints.maxWidth
                val complement = if (isFocused) complementOf(thumbTrackColor) else Color.Unspecified
                val thumbBorderColor = when {
                    inManip -> complement
                    isFocused -> complement.copy(alpha = 0.75f)
                    else -> Color.White.copy(alpha = 0.9f)
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset {
                            val thumbPx = SLIDER_THUMB_DP.dp.roundToPx()
                            IntOffset(
                                (thumbFraction * (trackWidthPx - thumbPx))
                                    .roundToInt()
                                    .coerceIn(0, (trackWidthPx - thumbPx).coerceAtLeast(0)),
                                0,
                            )
                        }
                        .size(SLIDER_THUMB_DP.dp, SLIDER_TRACK_HEIGHT_DP.dp)
                        .border(
                            ColorPickerConstants.SELECTOR_BORDER_DP.dp,
                            thumbBorderColor,
                            RoundedCornerShape(4.dp),
                        )
                )
            }
        }
    }
}
