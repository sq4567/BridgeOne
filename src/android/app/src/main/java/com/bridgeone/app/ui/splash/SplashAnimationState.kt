package com.bridgeone.app.ui.splash

/**
 * 스플래시 스크린 애니메이션 단계 (6단계 + 완료)
 */
enum class AnimationPhase {
    WAITING,        // Phase 1: 초기 대기 (0-300ms)
    BRIDGE_WIPE,    // Phase 2: 브릿지 등장 (300-1000ms)
    STAR_ROTATION,  // Phase 3: 별 회전 (1000-1500ms)
    STABILIZATION,  // Phase 4: 안정화 (1500-1800ms)
    EXPANSION,      // Phase 5: 확장 + 배경 전환 (1800-1900ms)
    TEXT_FADE,      // Phase 6: 텍스트 페이드인 (1900-2500ms)
    COMPLETED
}

object SplashConstants {
    const val PHASE_1_DURATION_MS = 100
    const val PHASE_2_DURATION_MS = 300
    const val PHASE_3_DURATION_MS = 500
    const val PHASE_4_DURATION_MS = 300
    const val PHASE_5_DURATION_MS = 100
    const val PHASE_6_DURATION_MS = 600
    const val TOTAL_DURATION_MS = 1900
    const val BRIDGE_LINE_WIDTH_DP = 2.0f
    const val STAR_ROTATION_DEGREES = 180f
    const val STAR_SCALE_RATIO = 0.85f
    const val SKIP_TAP_THRESHOLD_MS = 500
}
