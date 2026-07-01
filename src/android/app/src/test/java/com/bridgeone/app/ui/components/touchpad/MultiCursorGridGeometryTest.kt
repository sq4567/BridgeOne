package com.bridgeone.app.ui.components.touchpad

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * MultiCursorGridGeometry 단위 테스트
 *
 * Phase 4.8.3: 그리드 분할 영역 계산(divideGridAreas)과 영역 판정(hitTestPad)을 고정한다.
 */
class MultiCursorGridGeometryTest {

    // ── divideGridAreas ───────────────────────────────────────────

    @Test
    fun `divideGridAreas N=2 - 좌우 50퍼센트 분할`() {
        val areas = divideGridAreas(width = 400f, height = 600f, cursorCount = 2)
        assertEquals(2, areas.size)
        // pad1: 좌측 절반
        assertEquals(0f, areas[0].left)
        assertEquals(0f, areas[0].top)
        assertEquals(200f, areas[0].right)
        assertEquals(600f, areas[0].bottom)
        // pad2: 우측 절반
        assertEquals(200f, areas[1].left)
        assertEquals(400f, areas[1].right)
    }

    @Test
    fun `divideGridAreas N=3 - 좌중우 33퍼센트 분할`() {
        val areas = divideGridAreas(width = 300f, height = 600f, cursorCount = 3)
        assertEquals(3, areas.size)
        // 300 / 3 = 100 단위로 균등 분할
        assertEquals(0f, areas[0].left)
        assertEquals(100f, areas[0].right)
        assertEquals(100f, areas[1].left)
        assertEquals(200f, areas[1].right)
        assertEquals(200f, areas[2].left)
        assertEquals(300f, areas[2].right)
        areas.forEach { assertEquals(600f, it.bottom) }
    }

    @Test
    fun `divideGridAreas N=4 - 2x2 그리드 행 우선 배치`() {
        val areas = divideGridAreas(width = 400f, height = 600f, cursorCount = 4)
        assertEquals(4, areas.size)
        // pad1: 좌상
        assertEquals(0f, areas[0].left); assertEquals(0f, areas[0].top)
        assertEquals(200f, areas[0].right); assertEquals(300f, areas[0].bottom)
        // pad2: 우상
        assertEquals(200f, areas[1].left); assertEquals(0f, areas[1].top)
        assertEquals(400f, areas[1].right); assertEquals(300f, areas[1].bottom)
        // pad3: 좌하
        assertEquals(0f, areas[2].left); assertEquals(300f, areas[2].top)
        assertEquals(200f, areas[2].right); assertEquals(600f, areas[2].bottom)
        // pad4: 우하
        assertEquals(200f, areas[3].left); assertEquals(300f, areas[3].top)
        assertEquals(400f, areas[3].right); assertEquals(600f, areas[3].bottom)
    }

    @Test
    fun `divideGridAreas - 영역 합집합이 전체 너비 높이를 덮는다`() {
        for (count in MULTI_CURSOR_COUNT_MIN..MULTI_CURSOR_COUNT_MAX) {
            val areas = divideGridAreas(width = 360f, height = 800f, cursorCount = count)
            val totalArea = areas.sumOf { ((it.right - it.left) * (it.bottom - it.top)).toDouble() }
            assertEquals(360.0 * 800.0, totalArea, 0.01)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `divideGridAreas - 범위 밖 cursorCount는 예외`() {
        divideGridAreas(width = 400f, height = 600f, cursorCount = 5)
    }

    // ── hitTestPad ────────────────────────────────────────────────

    @Test
    fun `hitTestPad N=2 - 각 영역 중앙 판정`() {
        val areas = divideGridAreas(width = 400f, height = 600f, cursorCount = 2)
        assertEquals(0, hitTestPad(Offset(100f, 300f), areas))
        assertEquals(1, hitTestPad(Offset(300f, 300f), areas))
    }

    @Test
    fun `hitTestPad N=3 - 각 영역 중앙 판정`() {
        val areas = divideGridAreas(width = 300f, height = 600f, cursorCount = 3)
        assertEquals(0, hitTestPad(Offset(50f, 300f), areas))
        assertEquals(1, hitTestPad(Offset(150f, 300f), areas))
        assertEquals(2, hitTestPad(Offset(250f, 300f), areas))
    }

    @Test
    fun `hitTestPad N=4 - 각 코너 판정`() {
        val areas = divideGridAreas(width = 400f, height = 600f, cursorCount = 4)
        assertEquals(0, hitTestPad(Offset(50f, 100f), areas))   // 좌상
        assertEquals(1, hitTestPad(Offset(350f, 100f), areas))  // 우상
        assertEquals(2, hitTestPad(Offset(50f, 500f), areas))   // 좌하
        assertEquals(3, hitTestPad(Offset(350f, 500f), areas))  // 우하
    }

    @Test
    fun `hitTestPad - 경계선 위 좌표는 마지막 매칭 영역에 귀속`() {
        val areas = divideGridAreas(width = 400f, height = 600f, cursorCount = 2)
        // x=200은 pad1(0..200)과 pad2(200..400) 경계 → 나중 영역(pad2, index 1) 귀속
        assertEquals(1, hitTestPad(Offset(200f, 300f), areas))
    }

    @Test
    fun `hitTestPad - 영역 밖 좌표는 -1`() {
        val areas = divideGridAreas(width = 400f, height = 600f, cursorCount = 2)
        assertEquals(-1, hitTestPad(Offset(-10f, 300f), areas))
        assertEquals(-1, hitTestPad(Offset(410f, 300f), areas))
    }
}
