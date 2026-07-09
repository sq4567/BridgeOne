package com.bridgeone.app.ui.utils

/**
 * 모니터 셀렉터 기본값/영속화 폴백 규칙 (Phase 4.9.5, 순수 함수로 분리하여 단위테스트 가능하게 함).
 *
 * @param savedValue SharedPreferences에서 로드한 targetMonitor 값. 저장값 없음은 -1(음수)로 전달.
 * @param monitorCount 현재 통지된 모니터 개수.
 * @return 실제 사용할 targetMonitor 값 (0x00=전체, 0x01~N=특정 모니터)
 *
 * 규칙 (사용자 확정, styleframe-page3.md §2.2b):
 * - 저장값 없음(최초 진입, savedValue < 0) → 주 모니터로 폴백
 * - 저장값 0(전체)은 모니터 구성과 무관하게 항상 유효
 * - 저장값이 현재 monitorCount를 초과(모니터 구성 변경) → 주 모니터로 재폴백
 * - 그 외 유효한 인덱스는 그대로 복원
 */
fun resolveTargetMonitor(savedValue: Int, monitorCount: Int): Int {
    val fallback = AbsolutePointingConstants.DEFAULT_TARGET_MONITOR.toInt()
    return when {
        savedValue < 0 -> fallback
        savedValue == 0 -> 0
        savedValue > monitorCount -> fallback
        else -> savedValue
    }
}
