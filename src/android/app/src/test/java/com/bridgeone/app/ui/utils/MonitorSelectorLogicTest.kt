package com.bridgeone.app.ui.utils

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for resolveTargetMonitor() 폴백 규칙 (Phase 4.9.5)
 */
class MonitorSelectorLogicTest {

    @Test
    fun testNoSavedValueFallsBackToPrimaryMonitor() {
        // 최초 진입(저장값 없음, -1) → 주 모니터(0x01)
        assertEquals(1, resolveTargetMonitor(savedValue = -1, monitorCount = 2))
    }

    @Test
    fun testSavedIndexExceedingMonitorCountFallsBack() {
        // 저장된 인덱스(3)가 현재 monitorCount(2)를 초과 → 주 모니터로 재폴백
        assertEquals(1, resolveTargetMonitor(savedValue = 3, monitorCount = 2))
    }

    @Test
    fun testSavedAllMonitorsAlwaysValid() {
        // 저장값 0(전체)은 모니터 구성과 무관하게 항상 유효
        assertEquals(0, resolveTargetMonitor(savedValue = 0, monitorCount = 1))
    }

    @Test
    fun testSavedValidIndexRestored() {
        // 저장값(2)이 현재 monitorCount(2) 이하 → 그대로 복원
        assertEquals(2, resolveTargetMonitor(savedValue = 2, monitorCount = 2))
    }
}
