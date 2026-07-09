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

    // ── dragDistanceToZoomLevel (Phase 4.9.6) ──────────────────────────

    @Test
    fun `dragDistanceToZoomLevel - 0dp는 1x`() {
        assertEquals(1f, AbsoluteCoordinateCalculator.dragDistanceToZoomLevel(0f), 0.0001f)
    }

    @Test
    fun `dragDistanceToZoomLevel - 50dp는 2x`() {
        assertEquals(2f, AbsoluteCoordinateCalculator.dragDistanceToZoomLevel(50f), 0.0001f)
    }

    @Test
    fun `dragDistanceToZoomLevel - 100dp는 4x`() {
        assertEquals(4f, AbsoluteCoordinateCalculator.dragDistanceToZoomLevel(100f), 0.0001f)
    }

    @Test
    fun `dragDistanceToZoomLevel - 150dp는 8x(최대)`() {
        assertEquals(8f, AbsoluteCoordinateCalculator.dragDistanceToZoomLevel(150f), 0.0001f)
    }

    @Test
    fun `dragDistanceToZoomLevel - 75dp는 선형보간으로 3x`() {
        assertEquals(3f, AbsoluteCoordinateCalculator.dragDistanceToZoomLevel(75f), 0.0001f)
    }

    @Test
    fun `dragDistanceToZoomLevel - 150dp 초과는 8x로 클램핑`() {
        assertEquals(8f, AbsoluteCoordinateCalculator.dragDistanceToZoomLevel(200f), 0.0001f)
    }

    @Test
    fun `dragDistanceToZoomLevel - 음수 거리는 1x로 안전 처리`() {
        assertEquals(1f, AbsoluteCoordinateCalculator.dragDistanceToZoomLevel(-10f), 0.0001f)
    }

    // ── applyZoom (Phase 4.9.6) ──────────────────────────────────────

    @Test
    fun `applyZoom - level 1이면 원본 ratio 그대로(항등)`() {
        val ratio = TouchRatio(0.3f, 0.7f)
        val result = AbsoluteCoordinateCalculator.applyZoom(ratio, AbsoluteZoomState(level = 1f))
        assertEquals(ratio.x, result.x, 0.0001f)
        assertEquals(ratio.y, result.y, 0.0001f)
    }

    @Test
    fun `applyZoom - level 2, center 0_5면 0_25~0_75 윈도우로 매핑`() {
        val zoom = AbsoluteZoomState(level = 2f, centerX = 0.5f, centerY = 0.5f)
        val min = AbsoluteCoordinateCalculator.applyZoom(TouchRatio(0f, 0f), zoom)
        val max = AbsoluteCoordinateCalculator.applyZoom(TouchRatio(1f, 1f), zoom)
        val mid = AbsoluteCoordinateCalculator.applyZoom(TouchRatio(0.5f, 0.5f), zoom)
        assertEquals(0.25f, min.x, 0.0001f)
        assertEquals(0.25f, min.y, 0.0001f)
        assertEquals(0.75f, max.x, 0.0001f)
        assertEquals(0.75f, max.y, 0.0001f)
        assertEquals(0.5f, mid.x, 0.0001f)
        assertEquals(0.5f, mid.y, 0.0001f)
    }

    @Test
    fun `applyZoom - 경계 근처 center는 0~1 범위로 클램핑`() {
        val zoom = AbsoluteZoomState(level = 4f, centerX = 0f, centerY = 1f)
        val result = AbsoluteCoordinateCalculator.applyZoom(TouchRatio(0f, 1f), zoom)
        assertTrue(result.x in 0f..1f)
        assertTrue(result.y in 0f..1f)
        assertEquals(0f, result.x, 0.0001f)
        assertEquals(1f, result.y, 0.0001f)
    }

    @Test
    fun `AbsoluteZoomState isActive - level 1이면 false, 1 초과면 true`() {
        assertFalse(AbsoluteZoomState(level = 1f).isActive)
        assertTrue(AbsoluteZoomState(level = 2f).isActive)
    }

    // ── calculateZoomMappingRange (Phase 4.9.7) ──────────────────────────

    @Test
    fun `calculateZoomMappingRange - level 1이면 전체 범위(0,0,32767,32767)`() {
        val range = AbsoluteCoordinateCalculator.calculateZoomMappingRange(AbsoluteZoomState(level = 1f))
        assertEquals(0, range.minX)
        assertEquals(0, range.minY)
        assertEquals(32767, range.maxX)
        assertEquals(32767, range.maxY)
    }

    @Test
    fun `calculateZoomMappingRange - level 2, center 0_5는 8192~24575`() {
        // windowSize=0.5, min=(0.5-0.25)=0.25→round(0.25*32767)=8192, max=0.75→round(0.75*32767)=24575
        val range = AbsoluteCoordinateCalculator.calculateZoomMappingRange(
            AbsoluteZoomState(level = 2f, centerX = 0.5f, centerY = 0.5f)
        )
        assertEquals(8192, range.minX)
        assertEquals(8192, range.minY)
        assertEquals(24575, range.maxX)
        assertEquals(24575, range.maxY)
    }

    @Test
    fun `calculateZoomMappingRange - level 8, center 0_5는 14336~18431`() {
        // windowSize=0.125, min=(0.5-0.0625)=0.4375→round(0.4375*32767)=14336, max=0.5625→round(0.5625*32767)=18431
        val range = AbsoluteCoordinateCalculator.calculateZoomMappingRange(
            AbsoluteZoomState(level = 8f, centerX = 0.5f, centerY = 0.5f)
        )
        assertEquals(14336, range.minX)
        assertEquals(14336, range.minY)
        assertEquals(18431, range.maxX)
        assertEquals(18431, range.maxY)
    }

    @Test
    fun `calculateZoomMappingRange - 경계 center(0,1)는 0~1 범위로 클램핑`() {
        // windowSize=0.5. centerX=0: min=(0-0.25)→clamp 0, max=0.5→round(16383.5)=16384
        // centerY=1: min=(1-0.25)=0.75→clamp 0.5→round(16383.5)=16384, max=1.0→32767
        val range = AbsoluteCoordinateCalculator.calculateZoomMappingRange(
            AbsoluteZoomState(level = 2f, centerX = 0f, centerY = 1f)
        )
        assertEquals(0, range.minX)
        assertEquals(16384, range.maxX)
        assertEquals(16384, range.minY)
        assertEquals(32767, range.maxY)
        assertTrue(range.minX in 0..32767)
        assertTrue(range.maxY in 0..32767)
    }
}
