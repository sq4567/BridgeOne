package com.bridgeone.app.ui.common

/**
 * 멀티 커서 그리드 분할 레이아웃 관련 조정 가능 상수 (Phase 4.8.3)
 */
object MultiCursorConstants {
    /** 비활성 패드 영역 dim 오버레이 불투명도 (0~1). 기본값: 0.4f */
    const val GRID_INACTIVE_PAD_DIM_ALPHA = 0.4f

    /** 활성 패드 전환 시 dim 알파 변화 애니메이션 지속 시간 (ms). 기본값: 200 */
    const val GRID_PAD_SWITCH_ANIM_DURATION_MS = 200

    /** 터치패드 전체 영역을 감싸는 외곽 테두리 두께 (dp). 기본값: 2f */
    const val GRID_OUTER_BORDER_WIDTH_DP = 2f

    /** 비활성 패드 점선 테두리 두께 (dp). 기본값: 1.5f */
    const val GRID_INACTIVE_BORDER_WIDTH_DP = 1.5f

    /** 비활성 패드 점선 테두리의 선 길이 (dp). 기본값: 6f */
    const val GRID_DASH_ON_LENGTH_DP = 6f

    /** 비활성 패드 점선 테두리의 간격 길이 (dp). 기본값: 4f */
    const val GRID_DASH_OFF_LENGTH_DP = 4f

    // ============================================================
    // 직접 전환 버튼 레이아웃 모드 관련 상수 (Phase 4.8.4)
    // ============================================================

    /** 하단 패드 전환 버튼 패널 높이 (dp). 기본값: 48f */
    const val DIRECT_BUTTON_PANEL_HEIGHT_DP = 48f

    /** 비활성 전환 버튼 배경 불투명도 (0~1). 기본값: 0.4f */
    const val DIRECT_BUTTON_INACTIVE_ALPHA = 0.4f

    /** 전환 버튼 패널 외곽 모서리 둥글기 (dp, 터치패드 본체의 둥근 모서리와 시각적으로 통일). 기본값: 8f */
    const val DIRECT_BUTTON_CORNER_RADIUS_DP = 8f
}
