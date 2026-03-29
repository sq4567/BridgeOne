package com.bridgeone.app.ui.components.touchpad

/**
 * 터치패드 모드 상태 정의
 *
 * Phase 4.3.1: ControlButtonContainer에서 사용하는 모드 상태들.
 * 각 모드는 독립적으로 조합 가능 (ex: 우클릭 + 직각이동 + 싱글커서).
 */

// ============================================================
// 클릭 모드
// ============================================================

enum class ClickMode {
    LEFT_CLICK,   // 좌클릭 (기본)
    RIGHT_CLICK   // 우클릭
}

// ============================================================
// 커서 이동 모드
// ============================================================

enum class MoveMode {
    FREE,         // 자유 이동 (기본)
    RIGHT_ANGLE   // 직각 이동 (축 잠금)
}

// ============================================================
// 스크롤 모드
// ============================================================

enum class ScrollMode {
    OFF,              // 스크롤 비활성 = 커서 이동 모드 (기본)
    NORMAL_SCROLL,    // 일반 스크롤
    INFINITE_SCROLL   // 무한 스크롤 (관성)
}

// ============================================================
// 커서 모드
// ============================================================

enum class CursorMode {
    SINGLE,  // 싱글 커서 (기본)
    MULTI    // 멀티 커서 (Phase 4+ 미구현)
}

// ============================================================
// DPI 레벨
// ============================================================

enum class DpiLevel(val multiplier: Float, val label: String) {
    LOW(0.5f, "낮음"),
    NORMAL(1.0f, "보통"),
    HIGH(2.0f, "높음");

    fun next(): DpiLevel = when (this) {
        LOW -> NORMAL
        NORMAL -> HIGH
        HIGH -> LOW
    }
}

// ============================================================
// 스크롤 감도 레벨
// ============================================================

enum class ScrollSensitivity(val multiplier: Float, val label: String) {
    NORMAL(1.0f, "보통"),
    FAST(2.0f, "빠름"),
    SLOW(0.5f, "느림");

    fun next(): ScrollSensitivity = when (this) {
        NORMAL -> FAST
        FAST -> SLOW
        SLOW -> NORMAL
    }
}

// ============================================================
// 스크롤 축 (Phase 4.3.3)
// ============================================================

enum class ScrollAxis {
    UNDECIDED,   // 아직 축 미확정 (터치 직후 초기 상태)
    HORIZONTAL,  // 가로 스크롤 확정
    VERTICAL     // 세로 스크롤 확정
}

// ============================================================
// 터치패드 전체 상태
// ============================================================

/**
 * 터치패드의 모든 모드 상태를 통합 관리하는 데이터 클래스.
 *
 * 각 모드는 독립적으로 저장되며, 스크롤 모드 진입/종료 시에도
 * 클릭/이동/커서 모드의 마지막 상태가 보존됩니다 (상태 메모리).
 *
 * @property lastScrollMode 마지막으로 사용한 스크롤 모드 (스크롤 OFF에서
 *   ScrollModeButton 탭 시 이 모드로 재진입). 기본값: NORMAL_SCROLL.
 */
data class TouchpadState(
    val clickMode: ClickMode = ClickMode.LEFT_CLICK,
    val moveMode: MoveMode = MoveMode.FREE,
    val scrollMode: ScrollMode = ScrollMode.OFF,
    val cursorMode: CursorMode = CursorMode.SINGLE,
    val dpiLevel: DpiLevel = DpiLevel.NORMAL,
    val scrollSensitivity: ScrollSensitivity = ScrollSensitivity.NORMAL,
    val lastScrollMode: ScrollMode = ScrollMode.NORMAL_SCROLL
) {
    /** 스크롤 모드가 활성화되어 있는지 */
    val isScrollActive: Boolean
        get() = scrollMode != ScrollMode.OFF

    /** 커서 이동 모드가 활성화되어 있는지 (= 스크롤 비활성) */
    val isCursorMoveActive: Boolean
        get() = scrollMode == ScrollMode.OFF
}
