package com.bridgeone.app.ui.common

/**
 * 스크롤 모드 관련 조정 가능 상수 (Phase 4.3.3)
 *
 * 실기기 테스트 후 값을 조정할 수 있도록 한 곳에 모아 정의합니다.
 */
object ScrollConstants {

    /** 축 확정을 시도하기 위한 누적 이동 임계값 (dp). 이 거리 이전에는 UNDECIDED 유지 */
    const val SCROLL_AXIS_LOCK_DISTANCE_DP = 8f

    /** 45°에서 이 각도 이내이면 대각선으로 판정 — 축을 확정하지 않고 계속 누적 (0이면 비활성화) */
    const val SCROLL_AXIS_DIAGONAL_DEAD_ZONE_DEG = 15f

    /** 스크롤 1단위를 전송하기 위해 손가락이 이동해야 하는 거리 (dp). 값이 작을수록 빠름 */
    const val SCROLL_UNIT_DISTANCE_DP = 20f

    /** 느림 단계 스크롤 비율 배수 */
    const val SCROLL_SENSITIVITY_SLOW = 0.5f

    /** 보통 단계 스크롤 비율 배수 (기본) */
    const val SCROLL_SENSITIVITY_NORMAL = 1.0f

    /** 빠름 단계 스크롤 비율 배수 */
    const val SCROLL_SENSITIVITY_FAST = 2.0f

    /** 스크롤 1단위 전송 시 가이드라인이 이동하는 스텝 거리 (dp) */
    const val SCROLL_GUIDELINE_STEP_DP = 20f

    /** 가이드라인 선 간격 (dp) — 배경에 등간격으로 여러 선이 표시됨 */
    const val SCROLL_GUIDELINE_SPACING_DP = 40f

    /** 스크롤 정지 판정 후 가이드라인이 사라지기 시작하기까지의 대기 시간 (ms) */
    const val SCROLL_GUIDELINE_HIDE_DELAY_MS = 800L

    /** 마지막 스크롤 이벤트 이후 이 시간 동안 입력 없으면 스크롤 정지로 판정 (ms) */
    const val SCROLL_STOP_THRESHOLD_MS = 150L

    /** 가이드라인 spring 강성 (높을수록 빠르고 즉각적 / Spring.StiffnessHigh 기준) */
    const val SCROLL_GUIDELINE_SPRING_STIFFNESS = 10_000f

    /** 가이드라인 spring 감쇠비 (1.0=오버슈트 없음 / 0.5 이하=통통 튀는 느낌) */
    const val SCROLL_GUIDELINE_SPRING_DAMPING = 1.0f

    /** 스크롤 축 전환 시 가이드라인 회전 애니메이션 시간 (ms) */
    const val SCROLL_GUIDELINE_AXIS_ROTATION_MS = 100

    /** 더블탭 인식 최대 간격 (ms) */
    const val DOUBLE_TAP_MAX_INTERVAL_MS = 300L
}
