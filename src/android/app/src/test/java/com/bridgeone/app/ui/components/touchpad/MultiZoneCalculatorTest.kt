package com.bridgeone.app.ui.components.touchpad

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.bridgeone.app.ui.utils.AbsolutePointingConstants
import com.bridgeone.app.ui.utils.TouchRatio
import com.bridgeone.app.ui.utils.ZoneMapping
import com.bridgeone.app.ui.utils.ZoneRect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * MultiZoneCalculator 단위 테스트
 *
 * Phase 4.9.10: 멀티 존 데이터 모델 + 직사각형 ROI 좌표 변환 순수 함수 검증.
 */
class MultiZoneCalculatorTest {

    // ── divideZoneAreas ───────────────────────────────────────────

    @Test
    fun `divideZoneAreas N=2 - 좌우 50퍼센트 분할`() {
        val areas = divideZoneAreas(width = 400f, height = 600f, zoneCount = 2)
        assertEquals(2, areas.size)
        assertEquals(0f, areas[0].left)
        assertEquals(200f, areas[0].right)
        assertEquals(200f, areas[1].left)
        assertEquals(400f, areas[1].right)
    }

    @Test
    fun `divideZoneAreas N=3 - 좌중우 33퍼센트 분할`() {
        val areas = divideZoneAreas(width = 300f, height = 600f, zoneCount = 3)
        assertEquals(3, areas.size)
        assertEquals(0f, areas[0].left); assertEquals(100f, areas[0].right)
        assertEquals(100f, areas[1].left); assertEquals(200f, areas[1].right)
        assertEquals(200f, areas[2].left); assertEquals(300f, areas[2].right)
    }

    @Test
    fun `divideZoneAreas N=4 - 2x2 그리드 행 우선 배치`() {
        val areas = divideZoneAreas(width = 400f, height = 600f, zoneCount = 4)
        assertEquals(4, areas.size)
        assertEquals(0f, areas[0].left); assertEquals(0f, areas[0].top)
        assertEquals(200f, areas[0].right); assertEquals(300f, areas[0].bottom)
        assertEquals(200f, areas[3].left); assertEquals(300f, areas[3].top)
        assertEquals(400f, areas[3].right); assertEquals(600f, areas[3].bottom)
    }

    @Test
    fun `divideZoneAreas N=5 - 2행 그리드(위3 아래2), 행 우선 번호`() {
        val areas = divideZoneAreas(width = 600f, height = 400f, zoneCount = 5)
        assertEquals(5, areas.size)
        // 윗줄 3칸: 200폭씩, 아랫줄 2칸: 300폭씩
        assertEquals(0f, areas[0].left); assertEquals(200f, areas[0].right); assertEquals(0f, areas[0].top); assertEquals(200f, areas[0].bottom)
        assertEquals(200f, areas[1].left); assertEquals(400f, areas[1].right)
        assertEquals(400f, areas[2].left); assertEquals(600f, areas[2].right)
        assertEquals(0f, areas[3].left); assertEquals(300f, areas[3].right); assertEquals(200f, areas[3].top); assertEquals(400f, areas[3].bottom)
        assertEquals(300f, areas[4].left); assertEquals(600f, areas[4].right)
    }

    @Test
    fun `divideZoneAreas N=8 - 2행 그리드(위4 아래4)`() {
        val areas = divideZoneAreas(width = 800f, height = 400f, zoneCount = 8)
        assertEquals(8, areas.size)
        for (i in 0..3) {
            assertEquals(200f * i, areas[i].left)
            assertEquals(200f * (i + 1), areas[i].right)
            assertEquals(0f, areas[i].top); assertEquals(200f, areas[i].bottom)
        }
        for (i in 4..7) {
            val col = i - 4
            assertEquals(200f * col, areas[i].left)
            assertEquals(200f * (col + 1), areas[i].right)
            assertEquals(200f, areas[i].top); assertEquals(400f, areas[i].bottom)
        }
    }

    @Test
    fun `divideZoneAreas - 영역 합집합이 전체 너비 높이를 덮는다(2~8 전체)`() {
        for (count in AbsolutePointingConstants.MULTI_ZONE_COUNT_MIN..AbsolutePointingConstants.MULTI_ZONE_COUNT_MAX) {
            val areas = divideZoneAreas(width = 360f, height = 800f, zoneCount = count)
            val totalArea = areas.sumOf { ((it.right - it.left) * (it.bottom - it.top)).toDouble() }
            assertEquals(360.0 * 800.0, totalArea, 0.01)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `divideZoneAreas - 범위 밖 zoneCount는 예외`() {
        divideZoneAreas(width = 400f, height = 600f, zoneCount = 1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `divideZoneAreas - 9는 범위 밖으로 예외`() {
        divideZoneAreas(width = 400f, height = 600f, zoneCount = 9)
    }

    // ── normalizeInZone ───────────────────────────────────────────

    @Test
    fun `normalizeInZone - 셀 좌상단은 0,0`() {
        val padRect = Rect(100f, 50f, 300f, 250f)
        val result = normalizeInZone(Offset(100f, 50f), padRect)
        assertEquals(0f, result.x, 0.0001f)
        assertEquals(0f, result.y, 0.0001f)
    }

    @Test
    fun `normalizeInZone - 셀 중앙은 0_5,0_5(셀 오프셋 반영)`() {
        val padRect = Rect(100f, 50f, 300f, 250f)
        val result = normalizeInZone(Offset(200f, 150f), padRect)
        assertEquals(0.5f, result.x, 0.0001f)
        assertEquals(0.5f, result.y, 0.0001f)
    }

    @Test
    fun `normalizeInZone - 셀 밖 좌표는 0~1 클램핑`() {
        val padRect = Rect(100f, 50f, 300f, 250f)
        val result = normalizeInZone(Offset(0f, 0f), padRect)
        assertEquals(0f, result.x, 0.0001f)
        assertEquals(0f, result.y, 0.0001f)
    }

    // ── applyRoi ───────────────────────────────────────────

    @Test
    fun `applyRoi - FULL은 항등 매핑`() {
        val ratio = TouchRatio(0.3f, 0.7f)
        val result = applyRoi(ratio, ZoneRect.FULL)
        assertEquals(ratio.x, result.x, 0.0001f)
        assertEquals(ratio.y, result.y, 0.0001f)
    }

    @Test
    fun `applyRoi - 임의 종횡비 직사각형(세로로 긴 존) 합성`() {
        // 가로 0.2 폭, 세로 0.8 높이 — x/y 스케일이 다른 임의 종횡비
        val pcRect = ZoneRect(minX = 0.1f, minY = 0f, maxX = 0.3f, maxY = 0.8f)
        val min = applyRoi(TouchRatio(0f, 0f), pcRect)
        val max = applyRoi(TouchRatio(1f, 1f), pcRect)
        val mid = applyRoi(TouchRatio(0.5f, 0.5f), pcRect)
        assertEquals(0.1f, min.x, 0.0001f); assertEquals(0f, min.y, 0.0001f)
        assertEquals(0.3f, max.x, 0.0001f); assertEquals(0.8f, max.y, 0.0001f)
        assertEquals(0.2f, mid.x, 0.0001f); assertEquals(0.4f, mid.y, 0.0001f)
    }

    @Test
    fun `applyRoi - 결과는 0~1 범위로 클램핑`() {
        val pcRect = ZoneRect(minX = -0.5f, minY = -0.5f, maxX = 1.5f, maxY = 1.5f)
        val result = applyRoi(TouchRatio(0f, 1f), pcRect)
        assertTrue(result.x in 0f..1f)
        assertTrue(result.y in 0f..1f)
    }

    // ── resolveZoneRatio ───────────────────────────────────────────

    @Test
    fun `resolveZoneRatio - 미정의 존은 항등(로컬 비율 그대로)`() {
        val padRect = Rect(0f, 0f, 200f, 200f)
        val mapping = ZoneMapping(defined = false)
        val result = resolveZoneRatio(Offset(100f, 50f), padRect, mapping)
        assertEquals(0.5f, result.x, 0.0001f)
        assertEquals(0.25f, result.y, 0.0001f)
    }

    @Test
    fun `resolveZoneRatio - 정의된 존은 pcRect로 재매핑`() {
        val padRect = Rect(0f, 0f, 200f, 200f)
        val mapping = ZoneMapping(pcRect = ZoneRect(0.5f, 0.5f, 1f, 1f), defined = true)
        val result = resolveZoneRatio(Offset(100f, 100f), padRect, mapping)
        assertEquals(0.75f, result.x, 0.0001f)
        assertEquals(0.75f, result.y, 0.0001f)
    }

    // ── rectFromCenterDrag ───────────────────────────────────────────

    @Test
    fun `rectFromCenterDrag - 중심에서 대칭 확장`() {
        val rect = rectFromCenterDrag(TouchRatio(0.5f, 0.5f), TouchRatio(0.6f, 0.4f))
        assertEquals(0.4f, rect.minX, 0.0001f)
        assertEquals(0.6f, rect.maxX, 0.0001f)
        assertEquals(0.4f, rect.minY, 0.0001f)
        assertEquals(0.6f, rect.maxY, 0.0001f)
    }

    @Test
    fun `rectFromCenterDrag - 세로로만 드래그하면 세로로 긴 직사각형`() {
        val rect = rectFromCenterDrag(TouchRatio(0.5f, 0.5f), TouchRatio(0.5f, 0.9f))
        assertEquals(0.5f, rect.minX, 0.0001f)
        assertEquals(0.5f, rect.maxX, 0.0001f)
        assertEquals(0.1f, rect.minY, 0.0001f)
        assertEquals(0.9f, rect.maxY, 0.0001f)
    }

    @Test
    fun `rectFromCenterDrag - 손가락이 화면 밖이면 0~1로 클램핑되어 모니터 끝까지 확장`() {
        val rect = rectFromCenterDrag(TouchRatio(0.5f, 0.5f), TouchRatio(-0.5f, 1.5f))
        assertEquals(0f, rect.minX, 0.0001f)
        assertEquals(1f, rect.maxX, 0.0001f)
        assertEquals(0f, rect.minY, 0.0001f)
        assertEquals(1f, rect.maxY, 0.0001f)
    }

    @Test
    fun `rectFromCenterDrag - 손가락이 중심과 동일하면 0폭 직사각형`() {
        val rect = rectFromCenterDrag(TouchRatio(0.3f, 0.3f), TouchRatio(0.3f, 0.3f))
        assertEquals(0.3f, rect.minX, 0.0001f)
        assertEquals(0.3f, rect.maxX, 0.0001f)
    }
}
