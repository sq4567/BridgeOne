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

    /** 모니터 셀렉터 저장값 없음/모니터 구성 변경 시 폴백 대상 (주 모니터, Phase 4.9.5). 기본값: 0x01 */
    val DEFAULT_TARGET_MONITOR: UByte = 0x01u

    /** targetMonitor: 특정 모니터가 아닌 가상 데스크톱 전체 매핑 (Phase 4.9.5). 기본값: 0x00 */
    val TARGET_MONITOR_ALL: UByte = 0x00u

    /** 줌 최소 배율(해제 상태) (Phase 4.9.6). 기본값: 1f */
    const val ZOOM_LEVEL_MIN: Float = 1f

    /** 줌 최대 배율 (Phase 4.9.6). 기본값: 8f */
    const val ZOOM_LEVEL_MAX: Float = 8f

    /** 줌 확정 대기 중 안내 문구("탭하여 확정") 텍스트 크기 (sp) (Phase 4.9.6). 기본값: 16f */
    const val ZOOM_CONFIRM_HINT_TEXT_SIZE_SP: Float = 16f

    /** 줌 상태 Vendor CDC 전송 스로틀 간격 (ms), 드래그 중 30Hz 상한 (Phase 4.9.7). 기본값: 33L */
    const val ZOOM_STATE_THROTTLE_MS: Long = 33L

    /** 단일 줌 arming 진입 시 제어버튼/패드테두리/엣지존 fade-out 시간 (ms) (Phase 4.9.12). 기본값: 150L */
    const val ZOOM_DEFINE_FADE_OUT_MS: Long = 150L

    /** 단일 줌 확정 시 정의 프리뷰 직사각형이 패드 전체 경계로 확대되는 애니메이션 시간 (ms) (Phase 4.9.12). 기본값: 250L */
    const val ZOOM_DEFINE_EXPAND_MS: Long = 250L

    /** 단일 줌 확대 완료(또는 취소) 후 제어버튼/패드테두리/엣지존 fade-in·확대 오버레이 크로스페이드 시간 (ms) (Phase 4.9.12). 기본값: 200L */
    const val ZOOM_DEFINE_FADE_IN_MS: Long = 200L

    /** 멀티 존 개수 최소값 (Phase 4.9.10). 기본값: 2 */
    const val MULTI_ZONE_COUNT_MIN: Int = 2

    /** 멀티 존 개수 최대값 (Phase 4.9.10). 기본값: 8 */
    const val MULTI_ZONE_COUNT_MAX: Int = 8

    /** 멀티 존 개수 기본값 (Phase 4.9.10). 기본값: 2 */
    const val MULTI_ZONE_COUNT_DEFAULT: Int = 2

    /** 멀티 존 정의 중 PC 매핑 직사각형 프리뷰 테두리 굵기 (dp) (Phase 4.9.11). 기본값: 2f */
    const val MULTI_ZONE_RECT_PREVIEW_WIDTH_DP: Float = 2f

    /** 멀티 존 정의 중 PC 매핑 직사각형 프리뷰 불투명도 (Phase 4.9.11). 기본값: 0.9f */
    const val MULTI_ZONE_RECT_PREVIEW_ALPHA: Float = 0.9f

    /** 멀티 존 정의 중 안내 텍스트("존 k/N 정의 중") 크기 (sp) (Phase 4.9.11). 기본값: 16f */
    const val MULTI_ZONE_GUIDE_TEXT_SIZE_SP: Float = 16f

    /** 정의 중 안내 텍스트가 정의 rect와 겹치지 않도록 패드 상/하단에서 띄우는 여백 (dp) (Phase 4.9.12). 기본값: 24f */
    const val GUIDE_TEXT_EDGE_PADDING_DP: Float = 24f

    /** 멀티 존 확정 대기 중 재터치가 롱프레스로 판정되어 개수 선택 팝업부터 재시작되는 임계 시간 (ms) (Phase 4.9.11). 기본값: 500L */
    const val MULTI_ZONE_RESTART_LONGPRESS_MS: Long = 500L

    /** 단일 줌 확정 대기 중 재터치가 롱프레스로 판정되어 줌 모드가 완전히 해제되는 임계 시간 (ms) (Phase 4.9.12). 기본값: 500L */
    const val ZOOM_DEFINE_CANCEL_LONGPRESS_MS: Long = 500L

    /** 멀티 존 정의 중 이미 확정된 이전 존 오버레이 불투명도(현재 프리뷰보다 옅게, 겹침 확인용) (Phase 4.9.11). 기본값: 0.35f */
    const val MULTI_ZONE_PREVIOUS_RECT_ALPHA: Float = 0.35f

    /** 멀티 존 정의 중 이미 확정된 이전 존 오버레이 테두리 굵기 (dp) (Phase 4.9.11). 기본값: 1.5f */
    const val MULTI_ZONE_PREVIOUS_RECT_WIDTH_DP: Float = 1.5f

    /** 멀티 존 실시간 점프 중 서브 패드 경계선 굵기 (dp) (Phase 4.9.11). 기본값: 1.5f */
    const val MULTI_ZONE_GRID_LINE_WIDTH_DP: Float = 1.5f

    /** 멀티 존 실시간 점프 중 서브 패드 경계선 불투명도 (Phase 4.9.11). 기본값: 0.35f */
    const val MULTI_ZONE_GRID_LINE_ALPHA: Float = 0.35f

    /** 멀티 존 정의 직사각형 최소 크기(가로/세로 각각, 모니터 전체 대비 비율) (Phase 4.9.11).
     * 손가락이 중심점 부근에서 손을 떼도 지나치게 작은 존이 만들어지지 않도록 보장한다. 기본값: 0.1f */
    const val MULTI_ZONE_MIN_RECT_SIZE_RATIO: Float = 0.1f

    /** 멀티 존 정의 중 중심점 표시 점의 반지름 (dp) (Phase 4.9.11). 기본값: 5f */
    const val MULTI_ZONE_CENTER_DOT_RADIUS_DP: Float = 5f

    /** 멀티 존 정의 중 중심점 표시 점의 테두리 굵기 (dp) (Phase 4.9.11). 기본값: 2f */
    const val MULTI_ZONE_CENTER_DOT_RING_WIDTH_DP: Float = 2f
}
