package com.bridgeone.app.ui.components.touchpad

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import com.bridgeone.app.ui.common.DYNAMICS_PRESETS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * EdgeGeometry 단위 테스트
 *
 * Phase 4.7.3-A: TouchpadWrapper.kt에서 EdgeGeometry.kt로 분리된 6개 순수 함수를 고정.
 * 4.7.2-C에서 식별된 "엣지 진입 판정" 항목을 해소한다.
 */
class EdgeGeometryTest {

    // ── detectEntryEdge ──────────────────────────────────────────

    @Test
    fun `detectEntryEdge - 단일 LEFT 엣지`() {
        val result = detectEntryEdge(Offset(5f, 100f), width = 400f, height = 600f, edgeWidthPx = 40f)
        assertEquals(EntryEdge.LEFT, result)
    }

    @Test
    fun `detectEntryEdge - 단일 RIGHT 엣지`() {
        val result = detectEntryEdge(Offset(395f, 100f), width = 400f, height = 600f, edgeWidthPx = 40f)
        assertEquals(EntryEdge.RIGHT, result)
    }

    @Test
    fun `detectEntryEdge - 단일 TOP 엣지`() {
        val result = detectEntryEdge(Offset(200f, 5f), width = 400f, height = 600f, edgeWidthPx = 40f)
        assertEquals(EntryEdge.TOP, result)
    }

    @Test
    fun `detectEntryEdge - 단일 BOTTOM 엣지`() {
        val result = detectEntryEdge(Offset(200f, 595f), width = 400f, height = 600f, edgeWidthPx = 40f)
        assertEquals(EntryEdge.BOTTOM, result)
    }

    @Test
    fun `detectEntryEdge - 중앙은 null`() {
        val result = detectEntryEdge(Offset(200f, 300f), width = 400f, height = 600f, edgeWidthPx = 40f)
        assertNull(result)
    }

    @Test
    fun `detectEntryEdge - TOP_LEFT 코너 기본값은 TOP`() {
        val result = detectEntryEdge(Offset(5f, 5f), width = 400f, height = 600f, edgeWidthPx = 40f)
        assertEquals(EntryEdge.TOP, result)
    }

    @Test
    fun `detectEntryEdge - TOP_RIGHT 코너 기본값은 TOP`() {
        val result = detectEntryEdge(Offset(395f, 5f), width = 400f, height = 600f, edgeWidthPx = 40f)
        assertEquals(EntryEdge.TOP, result)
    }

    @Test
    fun `detectEntryEdge - BOTTOM_LEFT 코너 기본값은 BOTTOM`() {
        val result = detectEntryEdge(Offset(5f, 595f), width = 400f, height = 600f, edgeWidthPx = 40f)
        assertEquals(EntryEdge.BOTTOM, result)
    }

    @Test
    fun `detectEntryEdge - BOTTOM_RIGHT 코너 기본값은 BOTTOM`() {
        val result = detectEntryEdge(Offset(395f, 595f), width = 400f, height = 600f, edgeWidthPx = 40f)
        assertEquals(EntryEdge.BOTTOM, result)
    }

    @Test
    fun `detectEntryEdge - cornerPriority로 TOP_LEFT를 LEFT로 오버라이드`() {
        val priority = mapOf(CornerOverlap.TOP_LEFT to EntryEdge.LEFT)
        val result = detectEntryEdge(Offset(5f, 5f), width = 400f, height = 600f, edgeWidthPx = 40f, cornerPriority = priority)
        assertEquals(EntryEdge.LEFT, result)
    }

    @Test
    fun `detectEntryEdge - cornerPriority로 BOTTOM_RIGHT를 RIGHT로 오버라이드`() {
        val priority = mapOf(CornerOverlap.BOTTOM_RIGHT to EntryEdge.RIGHT)
        val result = detectEntryEdge(Offset(395f, 595f), width = 400f, height = 600f, edgeWidthPx = 40f, cornerPriority = priority)
        assertEquals(EntryEdge.RIGHT, result)
    }

    // ── getInwardDistance ─────────────────────────────────────────

    @Test
    fun `getInwardDistance - LEFT 엣지는 x 좌표`() {
        assertEquals(15f, getInwardDistance(Offset(15f, 100f), EntryEdge.LEFT, 400f, 600f))
    }

    @Test
    fun `getInwardDistance - RIGHT 엣지는 width - x`() {
        assertEquals(20f, getInwardDistance(Offset(380f, 100f), EntryEdge.RIGHT, 400f, 600f))
    }

    @Test
    fun `getInwardDistance - TOP 엣지는 y 좌표`() {
        assertEquals(10f, getInwardDistance(Offset(200f, 10f), EntryEdge.TOP, 400f, 600f))
    }

    @Test
    fun `getInwardDistance - BOTTOM 엣지는 height - y`() {
        assertEquals(25f, getInwardDistance(Offset(200f, 575f), EntryEdge.BOTTOM, 400f, 600f))
    }

    @Test
    fun `getInwardDistance - null 엣지는 0`() {
        assertEquals(0f, getInwardDistance(Offset(200f, 300f), null, 400f, 600f))
    }

    // ── getAlongEdgePosition ──────────────────────────────────────

    @Test
    fun `getAlongEdgePosition - LEFT 엣지는 y`() {
        assertEquals(150f, getAlongEdgePosition(Offset(5f, 150f), EntryEdge.LEFT))
    }

    @Test
    fun `getAlongEdgePosition - RIGHT 엣지는 y`() {
        assertEquals(200f, getAlongEdgePosition(Offset(395f, 200f), EntryEdge.RIGHT))
    }

    @Test
    fun `getAlongEdgePosition - TOP 엣지는 x`() {
        assertEquals(120f, getAlongEdgePosition(Offset(120f, 5f), EntryEdge.TOP))
    }

    @Test
    fun `getAlongEdgePosition - BOTTOM 엣지는 x`() {
        assertEquals(300f, getAlongEdgePosition(Offset(300f, 595f), EntryEdge.BOTTOM))
    }

    @Test
    fun `getAlongEdgePosition - null 엣지는 0`() {
        assertEquals(0f, getAlongEdgePosition(Offset(200f, 300f), null))
    }

    // ── findNearestEdge ───────────────────────────────────────────

    @Test
    fun `findNearestEdge - 왼쪽 사분면은 LEFT`() {
        assertEquals(EntryEdge.LEFT, findNearestEdge(Offset(10f, 300f), 400f, 600f))
    }

    @Test
    fun `findNearestEdge - 오른쪽 사분면은 RIGHT`() {
        assertEquals(EntryEdge.RIGHT, findNearestEdge(Offset(395f, 300f), 400f, 600f))
    }

    @Test
    fun `findNearestEdge - 위쪽 사분면은 TOP`() {
        assertEquals(EntryEdge.TOP, findNearestEdge(Offset(200f, 5f), 400f, 600f))
    }

    @Test
    fun `findNearestEdge - 아래쪽 사분면은 BOTTOM`() {
        assertEquals(EntryEdge.BOTTOM, findNearestEdge(Offset(200f, 595f), 400f, 600f))
    }

    // ── computeDirectTouchButtonRects ─────────────────────────────

    /** 픽셀 단위 density: 1dp = 1px (density = 1f) */
    private val unitDensity = Density(density = 1f, fontScale = 1f)

    @Test
    fun `computeDirectTouchButtonRects - modeCount=1 단일 열 배치`() {
        val rects = computeDirectTouchButtonRects(
            anchorPx = Offset(100f, 100f),
            containerWidth = 400f,
            containerHeight = 600f,
            modeCount = 1,
            buttonSizePx = 60f,
            gapPx = 8f,
            density = unitDensity
        )
        // 모드 버튼 1개 + 확인 버튼 1개
        assertEquals(2, rects.size)
    }

    @Test
    fun `computeDirectTouchButtonRects - modeCount=2 두 열 배치`() {
        val rects = computeDirectTouchButtonRects(
            anchorPx = Offset(200f, 300f),
            containerWidth = 400f,
            containerHeight = 600f,
            modeCount = 2,
            buttonSizePx = 60f,
            gapPx = 8f,
            density = unitDensity
        )
        // 모드 버튼 2개 + 확인 버튼 1개
        assertEquals(3, rects.size)
    }

    @Test
    fun `computeDirectTouchButtonRects - 앵커가 좌측 경계에 붙으면 왼쪽으로 clamp`() {
        val rects = computeDirectTouchButtonRects(
            anchorPx = Offset(0f, 300f),
            containerWidth = 400f,
            containerHeight = 600f,
            modeCount = 2,
            buttonSizePx = 60f,
            gapPx = 8f,
            density = unitDensity
        )
        // 모든 Rect의 left >= 0
        rects.forEach { rect ->
            assert(rect.left >= 0f) { "Rect left ${rect.left} should be >= 0" }
        }
    }

    @Test
    fun `computeDirectTouchButtonRects - 확인 버튼은 마지막 인덱스`() {
        val modeCount = 2
        val buttonSize = 60f
        val gap = 8f
        val rects = computeDirectTouchButtonRects(
            anchorPx = Offset(200f, 300f),
            containerWidth = 400f,
            containerHeight = 600f,
            modeCount = modeCount,
            buttonSizePx = buttonSize,
            gapPx = gap,
            density = unitDensity
        )
        val confirmRect = rects.last()
        // 확인 버튼이 모드 버튼보다 아래에 있어야 함
        val modeRectsMaxBottom = rects.dropLast(1).maxOf { it.bottom }
        assert(confirmRect.top >= modeRectsMaxBottom) {
            "확인 버튼 top(${confirmRect.top}) should be >= mode rects maxBottom($modeRectsMaxBottom)"
        }
    }

    // ── applyEdgeModeToggle ───────────────────────────────────────

    private val baseState = TouchpadState()

    @Test
    fun `applyEdgeModeToggle SCROLL - OFF → NORMAL_SCROLL`() {
        val state = baseState.copy(scrollMode = ScrollMode.OFF)
        val result = applyEdgeModeToggle(state, EdgeSwipeMode.SCROLL)
        assertEquals(ScrollMode.NORMAL_SCROLL, result.scrollMode)
        assertEquals(ScrollMode.NORMAL_SCROLL, result.lastScrollMode)
    }

    @Test
    fun `applyEdgeModeToggle SCROLL - NORMAL_SCROLL → INFINITE_SCROLL`() {
        val state = baseState.copy(scrollMode = ScrollMode.NORMAL_SCROLL)
        val result = applyEdgeModeToggle(state, EdgeSwipeMode.SCROLL)
        assertEquals(ScrollMode.INFINITE_SCROLL, result.scrollMode)
        assertEquals(ScrollMode.INFINITE_SCROLL, result.lastScrollMode)
    }

    @Test
    fun `applyEdgeModeToggle SCROLL - INFINITE_SCROLL → OFF (lastScrollMode=NORMAL_SCROLL)`() {
        val state = baseState.copy(scrollMode = ScrollMode.INFINITE_SCROLL)
        val result = applyEdgeModeToggle(state, EdgeSwipeMode.SCROLL)
        assertEquals(ScrollMode.OFF, result.scrollMode)
        // OFF 후 다음 활성화는 항상 NORMAL_SCROLL
        assertEquals(ScrollMode.NORMAL_SCROLL, result.lastScrollMode)
    }

    @Test
    fun `applyEdgeModeToggle CLICK - LEFT → RIGHT`() {
        val state = baseState.copy(clickMode = ClickMode.LEFT_CLICK)
        val result = applyEdgeModeToggle(state, EdgeSwipeMode.CLICK)
        assertEquals(ClickMode.RIGHT_CLICK, result.clickMode)
    }

    @Test
    fun `applyEdgeModeToggle CLICK - RIGHT → LEFT`() {
        val state = baseState.copy(clickMode = ClickMode.RIGHT_CLICK)
        val result = applyEdgeModeToggle(state, EdgeSwipeMode.CLICK)
        assertEquals(ClickMode.LEFT_CLICK, result.clickMode)
    }

    @Test
    fun `applyEdgeModeToggle MOVE - FREE → RIGHT_ANGLE`() {
        val state = baseState.copy(moveMode = MoveMode.FREE)
        val result = applyEdgeModeToggle(state, EdgeSwipeMode.MOVE)
        assertEquals(MoveMode.RIGHT_ANGLE, result.moveMode)
    }

    @Test
    fun `applyEdgeModeToggle MOVE - RIGHT_ANGLE → FREE`() {
        val state = baseState.copy(moveMode = MoveMode.RIGHT_ANGLE)
        val result = applyEdgeModeToggle(state, EdgeSwipeMode.MOVE)
        assertEquals(MoveMode.FREE, result.moveMode)
    }

    @Test
    fun `applyEdgeModeToggle CURSOR - SINGLE → MULTI`() {
        val state = baseState.copy(cursorMode = CursorMode.SINGLE)
        val result = applyEdgeModeToggle(state, EdgeSwipeMode.CURSOR)
        assertEquals(CursorMode.MULTI, result.cursorMode)
    }

    @Test
    fun `applyEdgeModeToggle CURSOR - MULTI → SINGLE`() {
        val state = baseState.copy(cursorMode = CursorMode.MULTI)
        val result = applyEdgeModeToggle(state, EdgeSwipeMode.CURSOR)
        assertEquals(CursorMode.SINGLE, result.cursorMode)
    }

    @Test
    fun `applyEdgeModeToggle DPI - next 호출 및 customDpiMultiplier 클리어`() {
        val state = baseState.copy(dpiLevel = DpiLevel.LOW, customDpiMultiplier = 1.5f)
        val result = applyEdgeModeToggle(state, EdgeSwipeMode.DPI)
        assertEquals(DpiLevel.LOW.next(), result.dpiLevel)
        assertNull(result.customDpiMultiplier)
    }

    @Test
    fun `applyEdgeModeToggle SCROLL_SPEED - next 호출`() {
        val state = baseState.copy(scrollSensitivity = ScrollSensitivity.NORMAL)
        val result = applyEdgeModeToggle(state, EdgeSwipeMode.SCROLL_SPEED)
        assertEquals(ScrollSensitivity.NORMAL.next(), result.scrollSensitivity)
    }

    @Test
    fun `applyEdgeModeToggle DYNAMICS - 인덱스 순환 (커스텀 없음)`() {
        val total = DYNAMICS_PRESETS.size  // 5
        val state = baseState.copy(dynamicsPresetIndex = total - 1)
        val result = applyEdgeModeToggle(state, EdgeSwipeMode.DYNAMICS, customPresetsCount = 0)
        assertEquals(0, result.dynamicsPresetIndex)
    }

    @Test
    fun `applyEdgeModeToggle DYNAMICS - 커스텀 프리셋 포함 순환`() {
        val total = DYNAMICS_PRESETS.size + 2  // 5 + 2 = 7
        val state = baseState.copy(dynamicsPresetIndex = total - 1)
        val result = applyEdgeModeToggle(state, EdgeSwipeMode.DYNAMICS, customPresetsCount = 2)
        assertEquals(0, result.dynamicsPresetIndex)
    }
}
