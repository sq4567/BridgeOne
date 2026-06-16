package com.bridgeone.app.ui.components.touchpad

/**
 * 터치패드 모드/세팅 변경 이력을 관리하는 스택.
 * StandardModePage에서 remember { ModeHistoryStack() }으로 세션 내 유지.
 * 앱 재시작 시 자동 초기화(비영속).
 */
class ModeHistoryStack {

    /** 스냅샷 최대 보유 개수. 초과 시 가장 오래된 항목 제거. 기본값: 32 */
    private val maxDepth = 32

    private val stack = ArrayDeque<TouchpadState>()

    /**
     * 현재 상태를 스택에 push.
     * maxDepth 초과 시 가장 오래된 항목(인덱스 0)을 제거한다.
     */
    fun push(state: TouchpadState) {
        stack.addLast(state)
        if (stack.size > maxDepth) stack.removeFirst()
    }

    /**
     * 스택에서 최근 항목을 꺼내 반환.
     * 스택이 비어 있으면 null.
     */
    fun pop(): TouchpadState? = if (stack.isEmpty()) null else stack.removeLast()

    /** 스택 전체 초기화. */
    fun clear() = stack.clear()

    /**
     * 두 상태가 "의미있는 변화"인지 판단.
     * lastScrollMode(스크롤 전용 메모리)와 edgeInteractionMode(조작 방식)는 비교 대상에서 제외.
     * 동일 모드 재적용·노이즈 변경은 false를 반환해 중복 push를 방지한다.
     */
    fun isMeaningfulChange(old: TouchpadState, new: TouchpadState): Boolean =
        old.clickMode                          != new.clickMode ||
        old.moveMode                           != new.moveMode ||
        old.scrollMode                         != new.scrollMode ||
        old.cursorMode                         != new.cursorMode ||
        old.dpiLevel                           != new.dpiLevel ||
        old.scrollSensitivity                  != new.scrollSensitivity ||
        old.customDpiMultiplier                != new.customDpiMultiplier ||
        old.customScrollSensitivityMultiplier  != new.customScrollSensitivityMultiplier ||
        old.dynamicsPresetIndex                != new.dynamicsPresetIndex ||
        old.modePresetIndex                    != new.modePresetIndex
}
