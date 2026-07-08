package com.bridgeone.app.ui.utils

/**
 * AbsolutePointingPad(Page 3) 전용 상수 중앙화.
 *
 * Reference: docs/development-plans/phase-4/phase-4-9-page3-absolute-pointing.md Phase 4.9.1
 */
object AbsolutePointingConstants {

    /** 클릭 판정 최대 누르는 시간 (ms). 기본값: 500L */
    const val CLICK_MAX_DURATION_MS: Long = 500L

    /** 클릭 판정 최대 이동 거리 (dp). 기존 상대좌표 터치패드(15f)보다 작음 — 절대좌표는
     * 손가락 아래가 곧 커서 위치라 작은 흔들림도 의도치 않은 커서 이동으로 이어지기 쉬움. 기본값: 5f */
    const val CLICK_MAX_MOVEMENT_DP: Float = 5f

    /** PointingArea 바깥 여백 (dp). 기본값: 16f */
    const val PAD_OUTER_MARGIN_DP: Float = 16f

    /** CoordinateIndicator 십자선 크기 (dp). 기본값: 20f */
    const val COORDINATE_INDICATOR_CROSSHAIR_SIZE_DP: Float = 20f

    /** CoordinateIndicator 중앙 점 크기 (dp). 기본값: 4f */
    const val COORDINATE_INDICATOR_DOT_SIZE_DP: Float = 4f

    /** CoordinateIndicator 선 굵기 (dp). 기본값: 2f */
    const val COORDINATE_INDICATOR_STROKE_WIDTH_DP: Float = 2f

    /** 터치 종료 후 CoordinateIndicator 페이드 아웃 시간 (ms). 기본값: 300L */
    const val COORDINATE_INDICATOR_FADE_MS: Long = 300L

    /** PointingArea 모서리 반경 (dp). 기본값: 8f */
    const val POINTING_AREA_CORNER_RADIUS_DP: Float = 8f

    /** PointingArea 테두리 굵기 (dp). 기본값: 2f */
    const val POINTING_AREA_BORDER_WIDTH_DP: Float = 2f

    /** 모니터 셀렉터(4.9.6) 도입 전 기본 대상 모니터 (주 모니터). 기본값: 0x01 */
    val DEFAULT_TARGET_MONITOR: UByte = 0x01u
}
