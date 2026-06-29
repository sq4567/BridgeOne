package com.bridgeone.app.ui.common

/**
 * 엣지 스와이프 제스처 관련 조정 가능 상수 (Phase 4.3.12)
 */
object EdgeSwipeConstants {
    /** 가장자리에서 이 폭 이내에서 시작해야 엣지 스와이프 후보로 인식 (dp). 기본값: 24f */
    const val EDGE_HIT_WIDTH_DP           = 24f

    /** 이 이상 안쪽으로 이동 시 모드 선택 팝업 등장 (dp). 기본값: 28f */
    const val TRIGGER_DISTANCE_DP         = 28f

    /** 팝업 등장 후, 진입 엣지에서 이 폭 이내로 되돌아오면 팝업 취소 (dp). 기본값: 12f */
    const val CANCEL_THRESHOLD_DP         = 12f

    /** 이 이상 안쪽으로 이동 시 물방울 애니메이션 등장 — Phase 4.3.13에서 사용 (dp). 기본값: 4f */
    const val DROPLET_APPEAR_THRESHOLD_DP =  4f

    /** 팝업 열린 상태에서 탭 vs 스와이프 구분 이동 임계값 (dp). 이 미만이면 탭, 이상이면 스와이프. 기본값: 15f */
    const val EDGE_POPUP_TAP_THRESHOLD_DP = 15f

    /** 고정 상태에서 안쪽→바깥쪽 스와이프 취소 판정 거리 (dp). 기본값: 60f */
    const val TWO_STEP_CANCEL_SWIPE_DP = 60f

    /** 팝업 내 버튼 탐색 시 선택이 1칸 이동하기 위한 스와이프 거리 (dp). 기본값: 30f */
    const val EDGE_POPUP_NAV_STEP_DP = 30f

    // ── 직접 터치 모드 ──

    /** 직접 터치 모드 버튼 크기 (dp). 기본값: 48f */
    const val EDGE_POPUP_DIRECT_BUTTON_SIZE_DP = 48f

    /** 직접 터치 모드 버튼 간격 (dp). 기본값: 6f */
    const val EDGE_POPUP_DIRECT_BUTTON_GAP_DP = 6f

    /** 직접 터치 모드 확인 버튼 높이 (dp) — 가로는 버튼 크기와 동일, 높이만 줄여 직사각형으로 표시. 기본값: 28f */
    const val EDGE_POPUP_DIRECT_CONFIRM_HEIGHT_DP = 28f

    // ── 산봉우리 애니메이션 (Phase 4.4.6) ──

    /** 산봉우리 피크 높이 상한 (dp). TRIGGER_DISTANCE_DP 이상이어야 트리거 전 시각 피드백이 끊기지 않음. 기본값: 36f */
    const val MAX_PEAK_HEIGHT_DP = 36f

    /** 산봉우리 기저부 반폭 (dp). 이 값 × 2 = 기저부 전체 너비. 기본값: 40f */
    const val BUMP_BASE_HALF_SIZE_DP = 40f

    /** 산봉우리 테두리(stroke) 두께 (dp). 기본값: 2f */
    const val BUMP_STROKE_WIDTH_DP = 2f

    /** 산봉우리 glow 기본 블러 반경 (dp). 기본값: 8f */
    const val BUMP_GLOW_RADIUS_DP = 8f

    /** 산봉우리 glow MAX 도달 시 블러 반경 (dp). 기본값: 16f */
    const val BUMP_GLOW_MAX_RADIUS_DP = 16f

    /** 산봉우리 수축 spring 강성. 기본값: 800f */
    const val BUMP_SHRINK_SPRING_STIFFNESS = 800f

    /** 산봉우리 수축 spring 감쇠비. 기본값: 0.7f */
    const val BUMP_SHRINK_SPRING_DAMPING = 0.7f

    // ── 엣지 존 힌트 오버레이 (Phase 4.5.10) ──

    /** 엣지 존 힌트 평상시 알파 (흰색 기준). 기본값: 0.06f */
    const val EDGE_ZONE_HINT_BASE_ALPHA = 0.06f

    /** 손가락이 엣지 존에 닿았을 때 힌트 알파. 기본값: 0.20f */
    const val EDGE_ZONE_HINT_ACTIVE_ALPHA = 0.20f

    /** 엣지 존 힌트 알파 전환 애니메이션 시간 (ms). 기본값: 150 */
    const val EDGE_ZONE_HINT_ANIM_MS = 150

    /** 엣지 존 활성 칩이 엣지로부터 안쪽으로 떨어진 거리 (dp). 기본값: 48f */
    const val EDGE_ZONE_CHIP_INWARD_OFFSET_DP = 48f

    // ── 존 분할 방식 (Phase 4.6.2) ──

    /** 존 최소 크기 비율. 기본값: 0.10f */
    const val MIN_ZONE_RATIO = 0.10f

    /** 엣지당 최대 존 수. 기본값: 5 */
    const val MAX_ZONES_PER_EDGE = 5f

    /** 편집기 경계선 드래그 히트박스 (dp). 기본값: 24f */
    const val ZONE_BOUNDARY_DRAG_HIT_DP = 24f

    /** 이동 모드 탭 드롭에서 '엣지 양 끝'으로 인정하는 가장자리 비율 영역. 기본값: 0.12f */
    const val EDGE_END_DROP_RATIO = 0.12f

    /** SWIPE 경계 조작(MANIPULATION) 진입 시 이동 데모(화살표+손가락)를 표시하는 시간 (ms). 기본값: 2500 */
    const val ZONE_BOUNDARY_HINT_VISIBLE_MS = 2500

    /** 모서리에 모드 변경 버튼이 있을 때 해당 엣지 끝에서 차단되는 비율. 기본값: 0.15f */
    const val CORNER_BUTTON_BLOCKED_RATIO = 0.15f

    /** 스트립 에디터에서 코너 오버랩 차단 구간 근사 비율 (실제 값 = edgePx/엣지길이이며 기기마다 다름). 기본값: 0.06f */
    const val EDGE_CORNER_OVERLAP_RATIO = 0.06f

    // ── 존 상시 시각화 (Phase 4.6.3) ──

    /** idle 존 fill 알파 (색상 블록 불투명도). 기본값: 0.35f */
    const val EDGE_ZONE_IDLE_BLOCK_ALPHA = 0.35f

    /** idle 존 아이콘 크기 (dp). 기본값: 14f */
    const val EDGE_ZONE_IDLE_ICON_SIZE_DP = 14f

    /** idle 존 아이콘 tint 알파 (흰색 기준). 기본값: 0.75f */
    const val EDGE_ZONE_IDLE_ICON_ALPHA = 0.75f

    /** 라벨 병행 표시 최소 존 크기 임계값 (dp). 이 이상일 때 아이콘 + 라벨 함께 표시. 기본값: 32f */
    const val EDGE_ZONE_IDLE_LABEL_MIN_SIZE_DP = 32f

    /** idle 아이콘 외곽 테두리에서 안쪽으로 밀어주는 오프셋 (dp). 기본값: 2f */
    const val EDGE_ZONE_IDLE_ICON_INSET_DP = 2f

    // ── 존 편집기 시각 (UI/UX 리디자인) ──

    /** 경계 드래그 핸들 반지름 (dp). 기본값: 6f */
    const val ZONE_DRAG_HANDLE_RADIUS_DP = 6f

    /** 캔버스 내 존 라벨 폰트 크기 (sp). 기본값: 9f */
    const val ZONE_LABEL_FONT_SIZE_SP = 9f

    /** 존 편집 캔버스의 포커스/선택(picked) 테두리 두께 (dp). 파란 포커스·주황 떠다니는 존 공통. 기본값: 2.5f */
    const val EDGE_ZONE_FOCUS_BORDER_DP = 2.5f

    // ── 엣지 스트립 에디터 (Phase 4.6.2+) ──

    /** 엣지 스트립 에디터 높이 (dp). 기본값: 48f */
    const val EDGE_STRIP_HEIGHT_DP = 48f

    /** 엣지 스트립 경계 핸들 시각 폭 (dp). 기본값: 3f */
    const val EDGE_STRIP_HANDLE_WIDTH_DP = 3f

    /** 엣지 스트립 경계 핸들 터치 히트 폭 (dp). 기본값: 28f */
    const val EDGE_STRIP_HANDLE_HIT_DP = 28f

    /** 엣지 스트립 탭 vs 드래그 구분 임계값 (dp). 기본값: 6f */
    const val EDGE_STRIP_TAP_THRESHOLD_DP = 6f

    // ── 로테이션 트리거 (Phase 4.6.2+) ──

    /** 로테이션 존 후보 1개당 기본 머무는 시간 (ms). 기본값: 800 */
    const val EDGE_ZONE_ROTATION_INTERVAL_DEFAULT_MS = 800

    /** 로테이션 간격 최솟값 (ms). 기본값: 300 */
    const val EDGE_ZONE_ROTATION_INTERVAL_MIN_MS = 300

    /** 로테이션 간격 최댓값 (ms). 기본값: 2000 */
    const val EDGE_ZONE_ROTATION_INTERVAL_MAX_MS = 2000

    /** 로테이션 존 최소 후보 수. 기본값: 2 */
    const val EDGE_ZONE_ROTATION_MIN_CANDIDATES = 2

    /** 로테이션 인터벌 프리셋 — 빠름 (ms). 기본값: 300 */
    const val EDGE_ZONE_ROTATION_INTERVAL_FAST_MS = 300
    /** 로테이션 인터벌 프리셋 — 보통 (ms). 기본값: 600 */
    const val EDGE_ZONE_ROTATION_INTERVAL_NORMAL_MS = 600
    /** 로테이션 인터벌 프리셋 — 느림 (ms). 기본값: 1000 */
    const val EDGE_ZONE_ROTATION_INTERVAL_SLOW_MS = 1000

    /** 엣지 존 옵션 카드 그리드 표시 영역 최대 높이 (dp). 카드 80dp×3행 + 간격 8dp×2 = 256. 기본값: 256 */
    const val EDGE_ZONE_OPTION_GRID_MAX_HEIGHT_DP = 256

    // ── 데포르메 비활성 오브젝트 (편집기 캔버스) ──

    /** 편집기 캔버스 내 코너 버튼 미니어처 크기 (dp). 기본값: 30f */
    const val DEFORMED_BUTTON_SIDE_DP = 30f

    /** 편집기 캔버스 내 코너 버튼 미니어처 모서리 반지름 (dp). 기본값: 6f */
    const val DEFORMED_BUTTON_CORNER_DP = 6f

    /** 미니어처 버튼 아이콘이 버튼 사이드에서 차지하는 비율. 기본값: 0.62f */
    const val DEFORMED_BUTTON_ICON_RATIO = 0.62f

    /** 제어버튼 묶음 블록의 높이 배율 (edgePx 대비). 기본값: 1.5f */
    const val DEFORMED_CONTROL_BLOCK_HEIGHT_RATIO = 1.5f
    /** 엣지 존 라벨 최대 길이. 기본값: 12 */
    const val EDGE_ZONE_LABEL_MAX_LEN = 12
    /** 엣지 존 라벨 고정 추천어. 기본값: 아래 목록 */
    val EDGE_ZONE_LABEL_SUGGESTIONS = listOf("복사", "붙여넣기", "실행 취소", "뒤로")
    /** 엣지 존 폴더 네비게이션 그리드 전환 애니메이션 길이 (ms). 기본값: 220 */
    const val EDGE_ZONE_FOLDER_NAV_ANIM_MS = 220
    /** 폴더 네비게이션 슬라이드 진입 거리 비율 (콘텐츠 폭 대비). 기본값: 0.18f */
    const val EDGE_ZONE_FOLDER_NAV_SLIDE_FRACTION = 0.18f

    /** 엣지 존 라벨 키보드 등장/퇴장 애니메이션 길이 (ms). 기본값: 280 */
    const val EDGE_ZONE_LABEL_KEYBOARD_ANIM_MS = 280
    /** 엣지 존 이동(재배치) 시 블록 슬라이드 애니메이션 길이 (ms). 기본값: 220 */
    const val EDGE_ZONE_MOVE_ANIM_MS = 220
    /** 엣지 존 드래그 들어올림(pick) 애니메이션 길이 (ms). 기본값: 150 */
    const val EDGE_ZONE_LIFT_MS = 150
    /** 엣지 존 드래그 내려놓기(settle) 애니메이션 길이 (ms). 기본값: 180 */
    const val EDGE_ZONE_SETTLE_MS = 180
    /** 엣지 존 라벨 키보드 시각 컨텐츠 높이 (dp), 폼 하단 여백 계산용. 기본값: 440 */
    const val EDGE_ZONE_LABEL_KEYBOARD_VISUAL_HEIGHT_DP = 440

    // ── 프리셋 팝업 미리보기 (Phase 4.6.3) ──

    /** 프리셋 썸네일/미리보기의 종횡비 (너비 / 높이). 기본값: 16f / 9f */
    const val PRESET_PREVIEW_ASPECT_RATIO = 16f / 9f

    // ── 비율 프리셋 가로 서랍 ──

    /** 비율 프리셋 서랍 슬라이드 인 애니메이션 길이 (ms). 기본값: 220 */
    const val RATIO_DRAWER_OPEN_DURATION_MS = 220
    /** 비율 프리셋 서랍 슬라이드 아웃 애니메이션 길이 (ms). 기본값: 160 */
    const val RATIO_DRAWER_CLOSE_DURATION_MS = 160
    /** SWIPE 비율 프리셋 서랍 위로 올림 (헤더 행 top 기준, dp). 기본값: 8f */
    const val RATIO_DRAWER_SWIPE_Y_LIFT_DP = 8f
    /** 비율 프리셋 서랍 컨테이너 모서리 반경 (dp). 기본값: 12f */
    const val RATIO_DRAWER_CORNER_RADIUS_DP = 12f
    /** 비율 프리셋 서랍 컨테이너 그림자 elevation (dp). 기본값: 6f */
    const val RATIO_DRAWER_ELEVATION_DP = 6f
    /** 비율 프리셋 서랍 컨테이너 내부 여백 (dp). 기본값: 4f */
    const val RATIO_DRAWER_CONTENT_PADDING_DP = 4f
    /** 비율 프리셋 서랍 좌측 손잡이 너비 (dp). 기본값: 4f */
    const val RATIO_DRAWER_HANDLE_WIDTH_DP = 4f
    /** 비율 프리셋 서랍 좌측 손잡이 높이 (dp). 기본값: 18f */
    const val RATIO_DRAWER_HANDLE_HEIGHT_DP = 18f
    /** 비율 프리셋 서랍 좌측 손잡이 모서리 반경 (dp). 기본값: 2f */
    const val RATIO_DRAWER_HANDLE_CORNER_DP = 2f
    /** 비율 프리셋 서랍 좌측 손잡이 좌우 여백 (dp). 기본값: 4f */
    const val RATIO_DRAWER_HANDLE_PADDING_DP = 4f
    /** 비율 프리셋 서랍 좌측 손잡이 알파. 기본값: 0.5f */
    const val RATIO_DRAWER_HANDLE_ALPHA = 0.5f
    /** 비율 프리셋 서랍 항목 셀 모서리 반경 (dp). 기본값: 8f */
    const val RATIO_DRAWER_ITEM_CORNER_RADIUS_DP = 8f
    /** 비율 프리셋 서랍 항목 셀 테두리 두께 (dp). 기본값: 1f */
    const val RATIO_DRAWER_ITEM_BORDER_WIDTH_DP = 1f
    /** 비율 프리셋 서랍 항목 셀 테두리 알파 (비포커스). 기본값: 0.3f */
    const val RATIO_DRAWER_ITEM_BORDER_ALPHA = 0.3f
    /** 비율 프리셋 서랍 항목 막대 그림 너비 (dp). 기본값: 40f ⚠️ 의도적 변경 */
    const val RATIO_DRAWER_ITEM_MINI_BAR_WIDTH_DP = 28f
    /** 비율 프리셋 서랍 항목 막대 그림 높이 (dp). 기본값: 10f */
    const val RATIO_DRAWER_ITEM_MINI_BAR_HEIGHT_DP = 10f
    /** 비율 프리셋 서랍 항목 내부 막대/텍스트 간격 (dp). 기본값: 10f ⚠️ 의도적 변경 */
    const val RATIO_DRAWER_ITEM_SPACING_DP = 6f
    /** 비율 프리셋 서랍 항목 수평 패딩 (dp). 기본값: 10f ⚠️ 의도적 변경 */
    const val RATIO_DRAWER_ITEM_PADDING_HORIZONTAL_DP = 8f
    /** 비율 프리셋 서랍 항목 수직 패딩 (dp). 기본값: 7f ⚠️ 의도적 변경 */
    const val RATIO_DRAWER_ITEM_PADDING_VERTICAL_DP = 5f
    /** 비율 프리셋 미리보기 중 스트립 amber 보더 두께 (dp). 기본값: 1.5f */
    const val RATIO_PREVIEW_BORDER_WIDTH_DP = 1.5f
    /** 비율 프리셋 미리보기 중 스트립 amber 보더 알파. 기본값: 0.55f */
    const val RATIO_PREVIEW_BORDER_ALPHA = 0.55f

    // ── 캔버스 <-> 존 설정 전환 (AnimatedContent) ──

    /** 캔버스 <-> 편집 전환 길이 (ms). 기본값: 260 */
    const val EDGE_ZONE_SCENE_TRANSITION_MS = 260
    /** 캔버스가 사라질 때 축소되는 목표 배율. 기본값: 0.85f */
    const val EDGE_ZONE_CANVAS_SCALE_MIN = 0.85f
    /** 편집 패널 enter 시작 배율. 기본값: 0.96f */
    const val EDGE_ZONE_EDIT_ENTER_SCALE = 0.96f
    /** 편집 enter를 캔버스 exit보다 늦게 시작시키는 지연 (ms). "그 이후에" 느낌. 기본값: 60 */
    const val EDGE_ZONE_EDIT_ENTER_DELAY_MS = 60

    // ── 캔버스 존 병합/분할 stretch·shrink 애니메이션 ──

    /** 존 병합/분할 stretch·shrink 보간 길이 (ms). 기본값: 220 */
    const val EDGE_ZONE_MORPH_MS = 220

    /** 비율 조정 경계 전환(되돌리기·프리셋 적용) 보간 애니메이션 길이 (ms). 기본값: 220 */
    const val EDGE_ZONE_RATIO_MORPH_MS = 220

    // ── 캔버스 모드 버튼 순차 전환 (AnimatedVisibility per-button stagger) ──

    /** 모드 버튼 개별 퇴장 애니메이션 길이 (ms). 기본값: 100 */
    const val EDGE_ZONE_MODE_BTN_EXIT_MS = 100
    /** 모드 버튼 개별 등장 애니메이션 길이 (ms). 기본값: 200 */
    const val EDGE_ZONE_MODE_BTN_ENTER_MS = 200
    /** 모드 버튼 순차 퇴장/등장 간격 (ms). 기본값: 35 */
    const val EDGE_ZONE_MODE_BTN_STAGGER_MS = 35
    /** 모드 취소 후 버튼 등장 시작 기저 지연 (ms). 기본값: 60 */
    const val EDGE_ZONE_MODE_BTN_ENTER_BASE_DELAY_MS = 60
    /** 모드 진행 UI enter 지연 (ms). 마지막 버튼 퇴장 완료 시점 근방. 기본값: 230 */
    const val EDGE_ZONE_MODE_UI_ENTER_DELAY_MS = 230
    /** 모드 전환 시 scale 시작/목표 배율. 기본값: 0.88f */
    const val EDGE_ZONE_MODE_SWITCH_SCALE = 0.88f
}
