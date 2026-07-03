package com.bridgeone.app.ui.components.touchpad

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 멀티 커서 상태 홀더 (평범한 클래스 + remember, Phase 4.7.4 컨벤션).
 *
 * `StandardModePageState`와 동일 계층·패턴을 따른다: 순수 상태 전이만 담당하고
 * 사이드이펙트(서버 명령 전송·토스트·햅틱)는 보유하지 않는다. 페이저 상위에서
 * `remember { MultiCursorController() }`로 1회 생성해 hoist하면 페이지 전환에도
 * 상태가 유지된다.
 */
class MultiCursorController {

    var state by mutableStateOf(MultiCursorState())
        private set

    /**
     * 멀티 커서 활성화. 모든 패드의 초기 모드 상태를 [seed]로 시드한다.
     *
     * @param cursorCount 활성화할 커서 수 (2~4)
     * @param seed 각 패드의 초기 모드 상태 (활성화 직전 싱글 커서 상태를 그대로 전달)
     */
    fun enable(cursorCount: Int, seed: PadModeState) {
        require(cursorCount in MULTI_CURSOR_COUNT_MIN..MULTI_CURSOR_COUNT_MAX) {
            "cursorCount는 $MULTI_CURSOR_COUNT_MIN~$MULTI_CURSOR_COUNT_MAX 범위여야 함: $cursorCount"
        }
        state = MultiCursorState(
            isEnabled = true,
            cursorCount = cursorCount,
            layoutMode = state.layoutMode,
            activePadIndex = 0,
            padModeStates = List(cursorCount) { seed }
        )
    }

    /**
     * 멀티 커서 활성화. 패드별로 서로 다른 초기 모드 상태를 [seeds]로 시드한다 (Phase 4.8.8).
     *
     * @param seeds 패드별 초기 모드 상태 리스트. 리스트 크기가 곧 커서 수(2~4)가 된다.
     */
    fun enable(seeds: List<PadModeState>) {
        require(seeds.size in MULTI_CURSOR_COUNT_MIN..MULTI_CURSOR_COUNT_MAX) {
            "seeds 크기는 $MULTI_CURSOR_COUNT_MIN~$MULTI_CURSOR_COUNT_MAX 범위여야 함: ${seeds.size}"
        }
        state = MultiCursorState(
            isEnabled = true,
            cursorCount = seeds.size,
            layoutMode = state.layoutMode,
            activePadIndex = 0,
            padModeStates = seeds
        )
    }

    /** 멀티 커서 비활성화. 패드 상태를 모두 정리하고 싱글 커서로 복귀한다. */
    fun disable() {
        state = MultiCursorState(layoutMode = state.layoutMode)
    }

    /** 활성 패드 전환 (Phase 4.8.3/4.8.4에서 UI 연결). 범위를 벗어나면 무시한다. */
    fun switchPad(index: Int) {
        if (!state.isEnabled || index !in state.padModeStates.indices) return
        state = state.copy(activePadIndex = index)
    }

    /**
     * 멀티 커서 활성 중 커서 수를 변경한다 (Phase 4.8.6). [disable]과 달리 해제 없이 수만 바꾼다.
     *
     * 기존 [PadModeState]는 보존하며 늘어난 패드는 pad1 상태로 시드, 줄어든 패드는 뒤에서부터 절단한다.
     * [MultiCursorState.activePadIndex]가 새 범위를 벗어나면 마지막 인덱스로 clamp한다.
     */
    fun changeCursorCount(count: Int) {
        val seed = state.padModeStates.firstOrNull() ?: PadModeState()
        changeCursorCount(count) { seed }
    }

    /**
     * 멀티 커서 활성 중 커서 수를 변경한다 (Phase 4.8.8). 늘어난 패드의 시드를 [seedForNewPad]로
     * 지정할 수 있다는 점만 다르고 나머지 동작은 [changeCursorCount] (Int)와 동일하다.
     *
     * @param seedForNewPad 신규 패드 인덱스를 받아 시드값을 반환하는 함수
     */
    fun changeCursorCount(count: Int, seedForNewPad: (Int) -> PadModeState) {
        require(count in MULTI_CURSOR_COUNT_MIN..MULTI_CURSOR_COUNT_MAX) {
            "count는 $MULTI_CURSOR_COUNT_MIN~$MULTI_CURSOR_COUNT_MAX 범위여야 함: $count"
        }
        if (!state.isEnabled || count == state.cursorCount) return
        val updatedPads = List(count) { index -> state.padModeStates.getOrNull(index) ?: seedForNewPad(index) }
        state = state.copy(
            cursorCount = count,
            padModeStates = updatedPads,
            activePadIndex = state.activePadIndex.coerceIn(0, count - 1)
        )
    }

    /** 그리드 분할 ↔ 직접 전환 버튼 레이아웃 모드를 토글한다 (Phase 4.8.4). */
    fun toggleLayoutMode() {
        val next = if (state.layoutMode == MultiCursorLayoutMode.GRID) {
            MultiCursorLayoutMode.DIRECT_BUTTON
        } else {
            MultiCursorLayoutMode.GRID
        }
        state = state.copy(layoutMode = next)
    }

    /** 활성 패드의 모드 상태만 교체한다. 다른 패드 상태는 보존된다. */
    fun updateActivePadMode(transform: (PadModeState) -> PadModeState) {
        if (!state.isEnabled) return
        val updated = state.padModeStates.toMutableList()
        val idx = state.activePadIndex
        if (idx !in updated.indices) return
        updated[idx] = transform(updated[idx])
        state = state.copy(padModeStates = updated)
    }
}
