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

    /** 무한 스크롤 연속 진동 1회 지속 시간 (ms). 기본값: 20L */
    const val INFINITE_SCROLL_HAPTIC_DURATION_MS = 20L

    /** 무한 스크롤 진동 amplitude 상한 (Android VibrationEffect 최대). 기본값: 255 */
    const val INFINITE_SCROLL_HAPTIC_AMPLITUDE_MAX = 255

    /** 무한 스크롤 진동 amplitude 하한. 기본값: 1 */
    const val INFINITE_SCROLL_HAPTIC_AMPLITUDE_MIN = 1

    /** 무한 스크롤 관성 코루틴 프레임 주기 (ms), ~60fps. 기본값: 16L */
    const val INFINITE_SCROLL_INERTIA_FRAME_MS = 16L

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

