package com.bridgeone.app.ui.components.touchpad

import com.bridgeone.app.ui.common.AppIconDef

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
// 포인터 다이나믹스 알고리즘 (Phase 4.3.8)
// ============================================================

enum class DynamicsAlgorithm {
    NONE,         // 가속 없음 (배율 항상 1.0)
    WINDOWS_EPP,  // Windows 포인터 정밀도 향상 유사 (S커브 가속)
    LINEAR        // 속도에 비례하는 선형 가속
}

/**
 * 포인터 다이나믹스 프리셋 정의
 *
 * @property name            프리셋 이름 (버튼 + 팝업 표시용)
 * @property algorithm       사용할 가속 알고리즘
 * @property intensityFactor 가속 강도 계수
 * @property velocityThresholdDpMs 가속 시작 속도 임계값 (dp/ms)
 * @property maxMultiplier   최대 가속 배율 (coerceIn 상한)
 * @property icon            UI 아이콘 (AppIcons에서 참조)
 * @property description     팝업 확인 단계에서 표시할 한 줄 설명
 */
data class PointerDynamicsPreset(
    val name: String,
    val algorithm: DynamicsAlgorithm,
    val intensityFactor: Float,
    val velocityThresholdDpMs: Float,
    val maxMultiplier: Float,
    val icon: AppIconDef,
    val description: String
)

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
    val lastScrollMode: ScrollMode = ScrollMode.NORMAL_SCROLL,
    /** 임시 커스텀 DPI 배율 (null = 사전 정의 레벨 사용). 앱 재시작 및 USB 끊김 시 소멸. */
    val customDpiMultiplier: Float? = null,
    /** 현재 포인터 다이나믹스 프리셋 인덱스 (DYNAMICS_PRESETS 기준). 기본값: 0 = Off */
    val dynamicsPresetIndex: Int = 0
) {
    /** 실제 적용되는 DPI 배율 (커스텀 우선, 없으면 레벨 배율) */
    val effectiveDpiMultiplier: Float
        get() = customDpiMultiplier ?: dpiLevel.multiplier

    /** 스크롤 모드가 활성화되어 있는지 */
    val isScrollActive: Boolean
        get() = scrollMode != ScrollMode.OFF

    /** 커서 이동 모드가 활성화되어 있는지 (= 스크롤 비활성) */
    val isCursorMoveActive: Boolean
        get() = scrollMode == ScrollMode.OFF
}
