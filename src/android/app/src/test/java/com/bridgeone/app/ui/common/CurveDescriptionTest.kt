package com.bridgeone.app.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * describeCurves 단위 테스트
 *
 * Phase 4.7.6-D: 곡선 자연어 요약 순수 함수 describeCurves를 고정.
 * 저/중/고속 보간값·가감속 비대칭 라벨·요약 문구 분기 검증.
 * (대상은 이미 PointerDynamicsConstants.kt(common)에 추출돼 있음)
 */
class CurveDescriptionTest {

    private val delta = 0.001f

    @Test
    fun `평탄 곡선 - 변화 없음`() {
        val flat = listOf(CurveNode(0f, 1f), CurveNode(8f, 1f))
        val d = describeCurves(flat, flat)

        assertEquals(1.0f, d.lowSpeedMultiplier, delta)
        assertEquals(1.0f, d.midSpeedMultiplier, delta)
        assertEquals(1.0f, d.highSpeedMultiplier, delta)
        assertEquals("없음", d.asymmetryLabel)
        assertEquals("느린 움직임은 자연스럽게, 속도 변화 적음.", d.summary)
    }

    @Test
    fun `강한 가속 곡선 - 고속 가속 + 약한 비대칭`() {
        val accel = listOf(CurveNode(0f, 0.5f), CurveNode(6f, 3f), CurveNode(8f, 3f))
        val decel = listOf(CurveNode(0f, 1f), CurveNode(8f, 1f))
        val d = describeCurves(accel, decel)

        // mid = 0.5 + (3/6)*(3-0.5) = 1.75, high = 3.0 (노드)
        assertEquals(1.75f, d.midSpeedMultiplier, delta)
        assertEquals(3.0f, d.highSpeedMultiplier, delta)
        assertEquals("약함", d.asymmetryLabel) // |1.75 - 1.0| = 0.75 < 0.8
        assertEquals("느린 움직임은 자연스럽게, 빠른 움직임은 약 3.0배로 가속, 멈출 때 부드럽게 감속.", d.summary)
    }

    @Test
    fun `둔감 곡선 - 느린·빠른 움직임 모두 축소`() {
        val slow = listOf(CurveNode(0f, 0.3f), CurveNode(8f, 0.3f))
        val d = describeCurves(slow, slow)

        assertEquals(0.3f, d.lowSpeedMultiplier, delta)
        assertEquals(0.3f, d.highSpeedMultiplier, delta)
        assertEquals("없음", d.asymmetryLabel)
        assertEquals("느린 움직임을 더 천천히, 빠른 움직임을 오히려 줄여 안정.", d.summary)
    }

    @Test
    fun `가속 강하고 감속 약함 - 강한 비대칭`() {
        val accel = listOf(CurveNode(0f, 1f), CurveNode(8f, 3f))
        val decel = listOf(CurveNode(0f, 1f), CurveNode(8f, 0.5f))
        val d = describeCurves(accel, decel)

        // mid_accel = 1 + (3/8)*2 = 1.75, decelMid = 1 + (3/8)*(-0.5) = 0.8125
        // |1.75 - 0.8125| = 0.9375 > 0.8 → 강함
        assertEquals("강함", d.asymmetryLabel)
    }
}
