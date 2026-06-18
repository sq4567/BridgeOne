package com.bridgeone.app.ui.pages

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.bridgeone.app.protocol.BridgeFrame
import com.bridgeone.app.ui.components.touchpad.ModeHistoryStack
import com.bridgeone.app.ui.components.touchpad.MouseButton
import com.bridgeone.app.ui.components.touchpad.MouseHoldMode
import com.bridgeone.app.ui.components.touchpad.TouchpadState

/**
 * StandardModePage의 페이지 레벨 상태 홀더 (평범한 클래스 + remember).
 *
 * Phase 4.7.4-C: `touchpadState` 호이스팅, 모드 변경 이력(`ModeHistoryStack`),
 * 마우스 홀드 세션 상태를 Composable에서 분리.
 *
 * 사이드이펙트(프레임 전송/토스트/오버레이)는 보유하지 않는다. 순수 상태 전이만 담당하고,
 * 전송할 프레임 바이트나 성공 여부를 반환해 Composable이 사이드이펙트를 실행하게 한다.
 *
 * AndroidX ViewModel이 아니라 `SwipeFocusController`/`ModeHistoryStack`과 동일한
 * 수동 상태 홀더 컨벤션을 따른다.
 *
 * @param initialTouchpadState 초기 터치패드 상태 (SharedPreferences 복원값은 호출부에서 주입)
 */
class StandardModePageState(initialTouchpadState: TouchpadState) {

    /**
     * 터치패드 상태.
     * 히스토리 미기록 변경(팝업 confirm·곡선 편집 등)은 외부에서 직접 할당하고,
     * 히스토리 기록이 필요한 변경은 [changeStateRecordingHistory]를 사용한다.
     */
    var touchpadState by mutableStateOf(initialTouchpadState)

    /** 모드/세팅 변경 이력 스택 (세션 내 유지, 비영속) */
    private val historyStack = ModeHistoryStack()

    /** 마우스 홀드 토글 세션 상태 (비영속, 앱 종료 시 자동 해제) */
    var heldMouseButtons by mutableStateOf(setOf<MouseButton>())
        private set

    /**
     * 의미있는 변화를 히스토리에 push한 뒤 상태 교체 (구 `recordingOnChange`).
     * 페이저 하위 컴포넌트의 `onTouchpadStateChange` 콜백으로 전달된다.
     */
    fun changeStateRecordingHistory(newState: TouchpadState) {
        if (historyStack.isMeaningfulChange(touchpadState, newState)) {
            historyStack.push(touchpadState)
        }
        touchpadState = newState
    }

    /**
     * 히스토리 스택에서 직전 상태를 pop해 복원 (구 `onRestorePrevious`).
     * 복원 자체는 다시 push되지 않는다. `edgeInteractionMode`는 현재 값을 유지한다.
     *
     * @return 복원에 성공하면 true, 스택이 비어 복원할 게 없으면 false
     */
    fun restorePrevious(): Boolean {
        val prev = historyStack.pop() ?: return false
        touchpadState = prev.copy(edgeInteractionMode = touchpadState.edgeInteractionMode)
        return true
    }

    /**
     * 마우스 홀드 토글 (구 `onMouseHoldToggle`의 상태 전이 부분).
     * [heldMouseButtons]를 갱신하고, 전송할 마우스 버튼 비트 바이트를 반환한다.
     * 프레임 전송·토스트는 호출부(Composable)가 반환값으로 수행한다.
     *
     * @return 전송할 buttons 비트 바이트
     */
    fun toggleMouseHold(button: MouseButton, mode: MouseHoldMode): UByte {
        val newHeld = when (mode) {
            MouseHoldMode.HOLD -> heldMouseButtons + button
            MouseHoldMode.RELEASE -> heldMouseButtons - button
            MouseHoldMode.TOGGLE ->
                if (heldMouseButtons.contains(button)) heldMouseButtons - button
                else heldMouseButtons + button
        }
        heldMouseButtons = newHeld
        return newHeld.fold(0) { acc, btn ->
            acc or when (btn) {
                MouseButton.LEFT   -> BridgeFrame.BUTTON_LEFT_MASK.toInt()
                MouseButton.RIGHT  -> BridgeFrame.BUTTON_RIGHT_MASK.toInt()
                MouseButton.MIDDLE -> BridgeFrame.BUTTON_MIDDLE_MASK.toInt()
            }
        }.toUByte()
    }
}
