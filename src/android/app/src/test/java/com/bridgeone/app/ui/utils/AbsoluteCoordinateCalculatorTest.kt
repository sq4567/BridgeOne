package com.bridgeone.app.ui.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * AbsoluteCoordinateCalculator 단위 테스트
 *
 * Phase 4.9.1: 터치 좌표 → 비율 변환 및 전송 최적화 순수 함수 검증.
 */
class AbsoluteCoordinateCalculatorTest {

    // ── calculateTouchRatio ──────────────────────────────────────────

    @Test
    fun `calculateTouchRatio - 중앙 터치는 0_5, 0_5`() {
        val result = AbsoluteCoordinateCalculator.calculateTouchRatio(
            touchX = 200f, touchY = 300f, areaWidth = 400f, areaHeight = 600f
        )
        assertEquals(0.5f, result.x, 0.0001f)
        assertEquals(0.5f, result.y, 0.0001f)
    }

    @Test
    fun `calculateTouchRatio - 좌상단 원점은 0_0, 0_0`() {
        val result = AbsoluteCoordinateCalculator.calculateTouchRatio(
            touchX = 0f, touchY = 0f, areaWidth = 400f, areaHeight = 600f
        )
        assertEquals(0f, result.x, 0.0001f)
        assertEquals(0f, result.y, 0.0001f)
    }

    @Test
    fun `calculateTouchRatio - 우하단 끝은 1_0, 1_0`() {
        val result = AbsoluteCoordinateCalculator.calculateTouchRatio(
            touchX = 400f, touchY = 600f, areaWidth = 400f, areaHeight = 600f
        )
        assertEquals(1f, result.x, 0.0001f)
        assertEquals(1f, result.y, 0.0001f)
    }

    @Test
    fun `calculateTouchRatio - 영역 밖 음수 좌표는 0으로 클램핑`() {
        val result = AbsoluteCoordinateCalculator.calculateTouchRatio(
            touchX = -50f, touchY = -10f, areaWidth = 400f, areaHeight = 600f
        )
        assertEquals(0f, result.x, 0.0001f)
        assertEquals(0f, result.y, 0.0001f)
    }

    @Test
    fun `calculateTouchRatio - 영역 밖 초과 좌표는 1로 클램핑`() {
        val result = AbsoluteCoordinateCalculator.calculateTouchRatio(
            touchX = 500f, touchY = 700f, areaWidth = 400f, areaHeight = 600f
        )
        assertEquals(1f, result.x, 0.0001f)
        assertEquals(1f, result.y, 0.0001f)
    }

    @Test
    fun `calculateTouchRatio - 영역 크기 0이면 0으로 안전 처리`() {
        val result = AbsoluteCoordinateCalculator.calculateTouchRatio(
            touchX = 10f, touchY = 10f, areaWidth = 0f, areaHeight = 0f
        )
        assertEquals(0f, result.x, 0.0001f)
        assertEquals(0f, result.y, 0.0001f)
    }

    // ── shouldTransmit ──────────────────────────────────────────

    @Test
    fun `shouldTransmit - 이전 값 없으면(null) 항상 true`() {
        assertTrue(AbsoluteCoordinateCalculator.shouldTransmit(TouchRatio(0.3f, 0.4f), null))
    }

    @Test
    fun `shouldTransmit - 동일 좌표면 false`() {
        val ratio = TouchRatio(0.3f, 0.4f)
        assertFalse(AbsoluteCoordinateCalculator.shouldTransmit(ratio, TouchRatio(0.3f, 0.4f)))
    }

    @Test
    fun `shouldTransmit - x만 달라도 true`() {
        assertTrue(AbsoluteCoordinateCalculator.shouldTransmit(TouchRatio(0.31f, 0.4f), TouchRatio(0.3f, 0.4f)))
    }

    @Test
    fun `shouldTransmit - y만 달라도 true`() {
        assertTrue(AbsoluteCoordinateCalculator.shouldTransmit(TouchRatio(0.3f, 0.41f), TouchRatio(0.3f, 0.4f)))
    }
}
