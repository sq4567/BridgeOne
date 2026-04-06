package com.bridgeone.app.ui.common

/**
 * 스크롤 모드 관련 조정 가능 상수 (Phase 4.3.3)
 *
 * 실기기 테스트 후 값을 조정할 수 있도록 한 곳에 모아 정의합니다.
 */
object ScrollConstants {

    /** 축 확정을 시도하기 위한 누적 이동 임계값 (dp). 이 거리 이전에는 UNDECIDED 유지. 기본값: 8f */
    const val SCROLL_AXIS_LOCK_DISTANCE_DP = 8f

    /** 45°에서 이 각도 이내이면 대각선으로 판정 — 축을 확정하지 않고 계속 누적 (0이면 비활성화). 기본값: 15f */
    const val SCROLL_AXIS_DIAGONAL_DEAD_ZONE_DEG = 15f

    /** 스크롤 1단위를 전송하기 위해 손가락이 이동해야 하는 거리 (dp). 값이 작을수록 빠름. 기본값: 15f */
    const val SCROLL_UNIT_DISTANCE_DP = 15f

    /** 느림 단계 스크롤 비율 배수. 기본값: 0.5f */
    const val SCROLL_SENSITIVITY_SLOW = 0.5f

    /** 보통 단계 스크롤 비율 배수 (기본). 기본값: 1.0f */
    const val SCROLL_SENSITIVITY_NORMAL = 1.0f

    /** 빠름 단계 스크롤 비율 배수. 기본값: 2.0f */
    const val SCROLL_SENSITIVITY_FAST = 2.0f

    /** 가이드라인 선 간격 (dp) — 배경에 등간격으로 여러 선이 표시됨. 기본값: 40f */
    const val SCROLL_GUIDELINE_SPACING_DP = 40f

    /** 스크롤 정지 판정 후 가이드라인이 사라지기 시작하기까지의 대기 시간 (ms). 기본값: 800L */
    const val SCROLL_GUIDELINE_HIDE_DELAY_MS = 800L

    /** 마지막 스크롤 이벤트 이후 이 시간 동안 입력 없으면 스크롤 정지로 판정 (ms). 기본값: 150L */
    const val SCROLL_STOP_THRESHOLD_MS = 150L

    /** 가이드라인 spring 강성 (높을수록 빠르고 즉각적 / Spring.StiffnessHigh 기준). 기본값: 10_000f */
    const val SCROLL_GUIDELINE_SPRING_STIFFNESS = 10_000f

    /** 가이드라인 spring 감쇠비 (1.0=오버슈트 없음 / 0.5 이하=통통 튀는 느낌). 기본값: 1.0f */
    const val SCROLL_GUIDELINE_SPRING_DAMPING = 1.0f

    /** 스크롤 축 전환 시 가이드라인 회전 애니메이션 시간 (ms). 기본값: 100 */
    const val SCROLL_GUIDELINE_AXIS_ROTATION_MS = 100

    // ── 무한 스크롤 관성 (Phase 4.3.4) ──

    /** 관성 지수 감쇠 시간 상수 (ms). 이 시간 후 속도가 약 37%(1/e)로 감소. 클수록 오래 지속. 기본값: 800f ⚠️ 의도적 변경 */
    const val INFINITE_SCROLL_TIME_CONSTANT_MS = 1500f

    /** 관성 정지 임계 속도 (dp/ms). 이 속도 미만이 되면 관성 종료. 기본값: 0.08f ⚠️ 의도적 변경 */
    const val INFINITE_SCROLL_MIN_VELOCITY_DP_MS = 0.2f

    /** 관성 속도 샘플 윈도우 (ms). 이 시간 범위 내 이동량으로 초기 속도 계산. 기본값: 100L */
    const val INFINITE_SCROLL_VELOCITY_WINDOW_MS = 100L

    /** 무한 스크롤 진동 최대 기준 속도 (dp/ms). 이 속도 이상이면 진동 amplitude 최대(255). 기본값: 2.0f */
    const val INFINITE_SCROLL_HAPTIC_MAX_VELOCITY_DP_MS = 2.0f

    // ── 일반 스크롤 버튼 (Phase 4.3.x) ──

    /** 일반 스크롤 버튼을 홀드할 때 스크롤 프레임 전송 간격 (ms). 값이 작을수록 빠름. 기본값: 100L */
    const val NORMAL_SCROLL_BUTTON_INTERVAL_MS = 100L

    /** 단일 MOVE 이벤트에서 전송할 수 있는 최대 스크롤 프레임 수 (2차 안전장치).
     *  주 제어는 SCROLL_FRAME_MIN_INTERVAL_MS의 시간 게이트.
     *  초과분은 다음 MOVE 이벤트로 이월됨 (스크롤 거리 손실 없음). 기본값: 3 */
    const val SCROLL_MAX_FRAMES_PER_EVENT = 3

    /** 연속 스크롤 프레임 사이의 최소 간격 (ms).
     *  초당 최대 전송 프레임 수 = 1000 / SCROLL_FRAME_MIN_INTERVAL_MS.
     *  8ms = 최대 125fps → ESP32-S3 HID 1ms 폴링 기준 안전 마진 확보.
     *  초과 시 해당 MOVE 이벤트의 나머지 누적분은 다음 이벤트로 이월됨. 기본값: 8L */
    const val SCROLL_FRAME_MIN_INTERVAL_MS = 8L

    // ── 직각 이동 모드 (Phase 4.3.5) ──

    /** 직각 이동 모드에서 주축 확정을 위한 누적 이동 임계값 (dp). 이 거리 이전에는 UNDECIDED 유지. 기본값: 12f */
    const val RIGHT_ANGLE_AXIS_LOCK_DISTANCE_DP = 12f

    /** 직각 이동 모드에서 주축 전환 방지 데드밴드 각도 (°). 45° ± 이 값 범위 내이면 대각선 판정. 기본값: 22.5f */
    const val RIGHT_ANGLE_DEADBAND_DEG = 22.5f
}

/**
 * 스크롤 방향별 속도 배율 (Phase 4.5.8)
 *
 * 특정 방향 스크롤이 다른 방향보다 느리게 느껴질 때 조정합니다.
 * 원인은 기기 특성일 수도 있고, 사용자의 신체 상황으로 인해
 * 특정 방향으로의 동일한 움직임이 어려운 경우일 수도 있습니다.
 * 1.0f = 변경 없음. MOVE 이벤트 scrollAccum 누적에 적용됩니다 (일반·무한 스크롤 공통).
 * 무한 스크롤의 관성 초기 속도에도 동일 배율이 반영됩니다.
 * 커서 이동 모드에는 영향 없음.
 */
object ScrollDirectionBoost {
    /** 아래 방향(손가락↓) 배율. 1.0f = 변경 없음. 기본값: 1.0f ⚠️ 의도적 변경 */
    const val DOWN_MULTIPLIER = 2.0f
    /** 위 방향(손가락↑) 배율. 1.0f = 변경 없음. 기본값: 1.0f */
    const val UP_MULTIPLIER = 1.0f
    /** 오른쪽 방향(손가락→) 배율. 1.0f = 변경 없음. 기본값: 1.0f */
    const val RIGHT_MULTIPLIER = 1.0f
    /** 왼쪽 방향(손가락←) 배율. 1.0f = 변경 없음. 기본값: 1.0f */
    const val LEFT_MULTIPLIER = 1.0f
}

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
}
