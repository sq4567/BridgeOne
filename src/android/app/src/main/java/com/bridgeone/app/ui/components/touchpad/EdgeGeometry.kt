package com.bridgeone.app.ui.components.touchpad

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.bridgeone.app.ui.common.DYNAMICS_PRESETS
import com.bridgeone.app.ui.common.EdgeSwipeConstants

// ============================================================
// 엣지 스와이프 헬퍼 함수 (Phase 4.3.12 / Phase 4.7.3-A 분리)
// ============================================================

/**
 * 터치 시작점([pos])이 어느 가장자리 영역에 속하는지 반환합니다.
 * 코너 겹침 영역은 [cornerPriority] 설정을 따르며, 기본값은 수평 엣지(TOP/BOTTOM) 우선입니다.
 */
internal fun detectEntryEdge(
    pos: Offset,
    width: Float,
    height: Float,
    edgeWidthPx: Float,
    cornerPriority: Map<CornerOverlap, EntryEdge> = emptyMap()
): EntryEdge? {
    val inLeft   = pos.x < edgeWidthPx
    val inRight  = pos.x > width - edgeWidthPx
    val inTop    = pos.y < edgeWidthPx
    val inBottom = pos.y > height - edgeWidthPx
    return when {
        inLeft && inTop     -> cornerPriority.getOrDefault(CornerOverlap.TOP_LEFT,     EntryEdge.TOP)
        inRight && inTop    -> cornerPriority.getOrDefault(CornerOverlap.TOP_RIGHT,    EntryEdge.TOP)
        inLeft && inBottom  -> cornerPriority.getOrDefault(CornerOverlap.BOTTOM_LEFT,  EntryEdge.BOTTOM)
        inRight && inBottom -> cornerPriority.getOrDefault(CornerOverlap.BOTTOM_RIGHT, EntryEdge.BOTTOM)
        inLeft   -> EntryEdge.LEFT
        inRight  -> EntryEdge.RIGHT
        inTop    -> EntryEdge.TOP
        inBottom -> EntryEdge.BOTTOM
        else     -> null
    }
}

/**
 * [edge] 방향 기준으로, 현재 손가락 위치[pos]가 가장자리에서 안쪽으로 얼마나 들어왔는지(px)를 반환합니다.
 * 값이 클수록 안쪽, 0에 가까울수록 가장자리에 있는 것입니다.
 */
internal fun getInwardDistance(
    pos: Offset,
    edge: EntryEdge?,
    width: Float,
    height: Float
): Float = when (edge) {
    EntryEdge.LEFT   -> pos.x
    EntryEdge.RIGHT  -> width - pos.x
    EntryEdge.TOP    -> pos.y
    EntryEdge.BOTTOM -> height - pos.y
    null             -> 0f
}

/**
 * [edge] 방향 기준으로, 손가락의 엣지 축(진입 방향에 수직인 축) 위치(px)를 반환합니다.
 * LEFT/RIGHT 엣지 → y 좌표, TOP/BOTTOM 엣지 → x 좌표
 */
internal fun getAlongEdgePosition(
    pos: Offset,
    edge: EntryEdge?
): Float = when (edge) {
    EntryEdge.LEFT, EntryEdge.RIGHT  -> pos.y
    EntryEdge.TOP, EntryEdge.BOTTOM  -> pos.x
    null                             -> 0f
}

/**
 * [pos] 에서 가장 가까운 엣지를 반환합니다.
 * 산봉우리 시각화에 사용되며, 제스처 로직과는 무관합니다.
 */
internal fun findNearestEdge(
    pos: Offset,
    width: Float,
    height: Float
): EntryEdge {
    val fromLeft   = pos.x
    val fromRight  = width - pos.x
    val fromTop    = pos.y
    val fromBottom = height - pos.y
    return when (minOf(fromLeft, fromRight, fromTop, fromBottom)) {
        fromLeft   -> EntryEdge.LEFT
        fromRight  -> EntryEdge.RIGHT
        fromTop    -> EntryEdge.TOP
        else       -> EntryEdge.BOTTOM
    }
}

/**
 * 직접 터치 모드에서 버튼 영역(Rect) 리스트를 계산합니다.
 * 인덱스 0..<modeCount = 모드 버튼, modeCount = 확인 버튼.
 * 2열 그리드 배치이며 앵커를 중심으로 하되, 터치패드 경계 안에 clamping합니다.
 */
internal fun computeDirectTouchButtonRects(
    anchorPx: Offset,
    containerWidth: Float,
    containerHeight: Float,
    modeCount: Int,
    buttonSizePx: Float,
    gapPx: Float,
    density: Density
): List<Rect> {
    val cols = if (modeCount <= 1) 1 else 2
    val modeRows = (modeCount + cols - 1) / cols
    val confirmHeightPx = density.run { EdgeSwipeConstants.EDGE_POPUP_DIRECT_CONFIRM_HEIGHT_DP.dp.toPx() }

    val gridW = cols * buttonSizePx + (cols - 1) * gapPx
    val gridH = modeRows * buttonSizePx + modeRows * gapPx + confirmHeightPx

    val gridLeft = (anchorPx.x - gridW / 2).coerceIn(0f, (containerWidth - gridW).coerceAtLeast(0f))
    val gridTop = (anchorPx.y - gridH / 2).coerceIn(0f, (containerHeight - gridH).coerceAtLeast(0f))

    val rects = mutableListOf<Rect>()
    for (i in 0 until modeCount) {
        val row = i / cols
        val col = i % cols
        val x = gridLeft + col * (buttonSizePx + gapPx)
        val y = gridTop + row * (buttonSizePx + gapPx)
        rects.add(Rect(x, y, x + buttonSizePx, y + buttonSizePx))
    }
    // 확인 버튼: 마지막 행, 그리드 가로 중앙, 높이만 줄인 직사각형
    val confirmX = gridLeft + (gridW - buttonSizePx) / 2
    val confirmY = gridTop + modeRows * (buttonSizePx + gapPx)
    rects.add(Rect(confirmX, confirmY, confirmX + buttonSizePx, confirmY + confirmHeightPx))

    return rects
}

/**
 * 엣지 스와이프로 [mode]를 토글한 새로운 [TouchpadState]를 반환합니다.
 */
internal fun applyEdgeModeToggle(state: TouchpadState, mode: EdgeSwipeMode, customPresetsCount: Int = 0): TouchpadState = when (mode) {
    EdgeSwipeMode.SCROLL -> when (state.scrollMode) {
        ScrollMode.OFF             -> state.copy(
            scrollMode = ScrollMode.NORMAL_SCROLL,
            lastScrollMode = ScrollMode.NORMAL_SCROLL,
            customDpiMultiplier = null
        )
        ScrollMode.NORMAL_SCROLL   -> state.copy(
            scrollMode = ScrollMode.INFINITE_SCROLL,
            lastScrollMode = ScrollMode.INFINITE_SCROLL
        )
        ScrollMode.INFINITE_SCROLL -> state.copy(
            scrollMode = ScrollMode.OFF,
            lastScrollMode = ScrollMode.NORMAL_SCROLL  // OFF 후 다음 활성화는 항상 NORMAL_SCROLL
        )
    }
    EdgeSwipeMode.CLICK -> state.copy(
        clickMode = if (state.clickMode == ClickMode.LEFT_CLICK) ClickMode.RIGHT_CLICK else ClickMode.LEFT_CLICK
    )
    EdgeSwipeMode.MOVE -> state.copy(
        moveMode = if (state.moveMode == MoveMode.FREE) MoveMode.RIGHT_ANGLE else MoveMode.FREE
    )
    EdgeSwipeMode.CURSOR -> state.copy(
        cursorMode = if (state.cursorMode == CursorMode.SINGLE) CursorMode.MULTI else CursorMode.SINGLE
    )
    EdgeSwipeMode.DPI -> state.copy(
        dpiLevel = state.dpiLevel.next(),
        customDpiMultiplier = null
    )
    EdgeSwipeMode.SCROLL_SPEED -> state.copy(
        scrollSensitivity = state.scrollSensitivity.next()
    )
    EdgeSwipeMode.DYNAMICS -> state.copy(
        dynamicsPresetIndex = (state.dynamicsPresetIndex + 1) % (DYNAMICS_PRESETS.size + customPresetsCount)
    )
}
