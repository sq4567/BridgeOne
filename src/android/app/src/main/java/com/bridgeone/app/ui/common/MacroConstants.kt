package com.bridgeone.app.ui.common

// ============================================================
// 매크로 타이밍 상수
// ============================================================

/** 스텝 간 기본 딜레이 (ms). 기본값: 80 */
const val MACRO_STEP_DELAY_DEFAULT_MS: Int = 80

/** 스텝 간 딜레이 슬라이더 최솟값 (ms). 기본값: 20 */
const val MACRO_STEP_DELAY_MIN_MS: Int = 20

/** 스텝 간 딜레이 슬라이더 최댓값 (ms). 기본값: 500 */
const val MACRO_STEP_DELAY_MAX_MS: Int = 500

/**
 * 매크로 단일 스텝 내 press → release 사이 인위적 딜레이 (ms).
 * 빠른 연속 스텝 전송 시 USB 큐 오버플로로 release 프레임이 유실되는 것을 방지.
 * 기본값: 16
 */
const val MACRO_INTRA_STEP_PRESS_RELEASE_MS: Long = 16L

/**
 * HOLD 스텝으로 동시에 유지할 수 있는 일반 키(비 modifier) 최대 개수.
 * BridgeFrame 프로토콜 제약(keyCode1 + keyCode2)에 의한 하드 상한.
 * 기본값: 2
 */
const val MACRO_MAX_HELD_KEYS: Int = 2

// ============================================================
// 매크로 편집기 레이아웃 상수
// ============================================================

/**
 * 다이얼로그 카드가 차지할 최대 화면 높이 비율.
 * 나머지(1 - fraction)는 타이틀·상하 여백으로 사용.
 * 기본값: 0.82f
 */
const val MACRO_DIALOG_MAX_SCREEN_FRACTION: Float = 0.82f

/**
 * 다이얼로그 카드 최대 높이에서 스텝 목록을 제외한 고정 영역 (dp).
 * 카드 내부 패딩(20dp) + 스텝 추가·일괄 추가·그룹 묶기·구분선·취소/다음 합계.
 * 기본값: 240
 */
const val MACRO_STEP_LIST_RESERVED_DP: Int = 240

// ============================================================
// 매크로 실행 중 화면 차단 (스크림) 상수
// ============================================================

/** 매크로 차단 스크림 최소 표시 시간 (ms). 기본값: 400L */
const val MACRO_SCRIM_MIN_DISPLAY_MS: Long = 400L

/** 매크로 차단 스크림 어둡기 (0~1). 기본값: 0.4f */
const val MACRO_SCRIM_ALPHA: Float = 0.4f
