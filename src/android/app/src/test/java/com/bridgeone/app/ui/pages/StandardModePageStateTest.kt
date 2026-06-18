package com.bridgeone.app.ui.pages

import com.bridgeone.app.ui.components.touchpad.EdgeInteractionMode
import com.bridgeone.app.ui.components.touchpad.MouseButton
import com.bridgeone.app.ui.components.touchpad.MouseHoldMode
import com.bridgeone.app.ui.components.touchpad.TouchpadState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * StandardModePageState 단위 테스트
 *
 * Phase 4.7.4-C: 상태 홀더의 순수 상태 전이 로직 고정.
 * - changeStateRecordingHistory: 의미있는 변화만 히스토리 push
 * - restorePrevious: 직전 상태 복원, edgeInteractionMode는 현재 값 유지, 복원은 재push 안 됨
 * - toggleMouseHold: 버튼 집합 갱신 + 전송할 buttons 비트 산출
 *
 * mutableStateOf 기반이지만 스냅샷 컨텍스트 없이 JVM에서 읽기/쓰기 가능.
 */
class StandardModePageStateTest {

    // ======================================================
    // changeStateRecordingHistory
    // ======================================================

    @Test
    fun changeState_meaningfulChange_updatesState() {
        val state = StandardModePageState(TouchpadState())
        state.changeStateRecordingHistory(TouchpadState(dynamicsPresetIndex = 1))
        assertEquals("상태가 새 값으로 교체", 1, state.touchpadState.dynamicsPresetIndex)
    }

    @Test
    fun changeState_meaningfulChange_pushesHistory() {
        // 의미있는 변화(dynamicsPresetIndex 0→1)면 직전 상태가 push되어 복원 가능
        val state = StandardModePageState(TouchpadState(dynamicsPresetIndex = 0))
        state.changeStateRecordingHistory(TouchpadState(dynamicsPresetIndex = 1))
        assertTrue("복원 성공", state.restorePrevious())
        assertEquals("직전 상태(index=0) 복원", 0, state.touchpadState.dynamicsPresetIndex)
    }

    @Test
    fun changeState_meaninglessChange_doesNotPush() {
        // edgeInteractionMode만 변경 → isMeaningfulChange 제외 대상이라 push 안 됨
        val state = StandardModePageState(TouchpadState(edgeInteractionMode = EdgeInteractionMode.LEGACY_POPUP))
        state.changeStateRecordingHistory(TouchpadState(edgeInteractionMode = EdgeInteractionMode.ZONE))
        assertFalse("의미없는 변화는 push 안 됨", state.restorePrevious())
    }

    @Test
    fun changeState_meaninglessChange_stillUpdatesState() {
        // push는 안 되더라도 상태 자체는 교체되어야 함
        val state = StandardModePageState(TouchpadState(edgeInteractionMode = EdgeInteractionMode.LEGACY_POPUP))
        state.changeStateRecordingHistory(TouchpadState(edgeInteractionMode = EdgeInteractionMode.ZONE))
        assertEquals("상태는 교체됨", EdgeInteractionMode.ZONE, state.touchpadState.edgeInteractionMode)
    }

    // ======================================================
    // restorePrevious
    // ======================================================

    @Test
    fun restorePrevious_emptyStack_returnsFalse() {
        val state = StandardModePageState(TouchpadState())
        assertFalse("빈 스택 복원 실패", state.restorePrevious())
    }

    @Test
    fun restorePrevious_emptyStack_keepsState() {
        val state = StandardModePageState(TouchpadState(dynamicsPresetIndex = 5))
        state.restorePrevious()
        assertEquals("빈 스택일 때 상태 불변", 5, state.touchpadState.dynamicsPresetIndex)
    }

    @Test
    fun restorePrevious_keepsCurrentEdgeInteractionMode() {
        // 직전 상태는 LEGACY_POPUP였지만 복원 시 현재 edgeInteractionMode(ZONE) 유지
        val state = StandardModePageState(
            TouchpadState(dynamicsPresetIndex = 0, edgeInteractionMode = EdgeInteractionMode.LEGACY_POPUP)
        )
        state.changeStateRecordingHistory(
            TouchpadState(dynamicsPresetIndex = 1, edgeInteractionMode = EdgeInteractionMode.LEGACY_POPUP)
        )
        // 조작 방식만 ZONE으로 직접 변경 (push 없는 직접 할당 경로)
        state.touchpadState = state.touchpadState.copy(edgeInteractionMode = EdgeInteractionMode.ZONE)
        assertTrue(state.restorePrevious())
        assertEquals("직전 dynamicsPresetIndex 복원", 0, state.touchpadState.dynamicsPresetIndex)
        assertEquals("현재 edgeInteractionMode 유지", EdgeInteractionMode.ZONE, state.touchpadState.edgeInteractionMode)
    }

    @Test
    fun restorePrevious_consecutiveRestores_notRePushed() {
        // 복원 자체는 다시 push되지 않아 연속 복원이 순서대로 동작
        val state = StandardModePageState(TouchpadState(dynamicsPresetIndex = 0))
        state.changeStateRecordingHistory(TouchpadState(dynamicsPresetIndex = 1))  // push index=0
        state.changeStateRecordingHistory(TouchpadState(dynamicsPresetIndex = 2))  // push index=1
        assertTrue(state.restorePrevious())
        assertEquals("1차 복원 → index=1", 1, state.touchpadState.dynamicsPresetIndex)
        assertTrue(state.restorePrevious())
        assertEquals("2차 복원 → index=0", 0, state.touchpadState.dynamicsPresetIndex)
        assertFalse("더 복원할 것 없음", state.restorePrevious())
    }

    // ======================================================
    // toggleMouseHold
    // ======================================================

    @Test
    fun toggleMouseHold_hold_addsButtonAndReturnsBit() {
        val state = StandardModePageState(TouchpadState())
        val bits = state.toggleMouseHold(MouseButton.LEFT, MouseHoldMode.HOLD)
        assertEquals("LEFT 비트=0x01", 0x01.toUByte(), bits)
        assertTrue("heldMouseButtons에 LEFT 포함", state.heldMouseButtons.contains(MouseButton.LEFT))
    }

    @Test
    fun toggleMouseHold_multipleButtons_orsBits() {
        val state = StandardModePageState(TouchpadState())
        state.toggleMouseHold(MouseButton.LEFT, MouseHoldMode.HOLD)
        val bits = state.toggleMouseHold(MouseButton.RIGHT, MouseHoldMode.HOLD)
        assertEquals("LEFT|RIGHT 비트=0x03", 0x03.toUByte(), bits)
    }

    @Test
    fun toggleMouseHold_release_removesButton() {
        val state = StandardModePageState(TouchpadState())
        state.toggleMouseHold(MouseButton.LEFT, MouseHoldMode.HOLD)
        state.toggleMouseHold(MouseButton.RIGHT, MouseHoldMode.HOLD)
        val bits = state.toggleMouseHold(MouseButton.LEFT, MouseHoldMode.RELEASE)
        assertEquals("RIGHT만 남음 비트=0x02", 0x02.toUByte(), bits)
        assertFalse("LEFT 제거됨", state.heldMouseButtons.contains(MouseButton.LEFT))
    }

    @Test
    fun toggleMouseHold_toggle_flipsState() {
        val state = StandardModePageState(TouchpadState())
        // 없는 상태에서 TOGGLE → 추가
        val on = state.toggleMouseHold(MouseButton.MIDDLE, MouseHoldMode.TOGGLE)
        assertEquals("MIDDLE 비트=0x04", 0x04.toUByte(), on)
        assertTrue(state.heldMouseButtons.contains(MouseButton.MIDDLE))
        // 있는 상태에서 TOGGLE → 제거
        val off = state.toggleMouseHold(MouseButton.MIDDLE, MouseHoldMode.TOGGLE)
        assertEquals("전부 해제=0x00", 0x00.toUByte(), off)
        assertFalse(state.heldMouseButtons.contains(MouseButton.MIDDLE))
    }

    @Test
    fun toggleMouseHold_allReleased_returnsZero() {
        val state = StandardModePageState(TouchpadState())
        state.toggleMouseHold(MouseButton.LEFT, MouseHoldMode.HOLD)
        val bits = state.toggleMouseHold(MouseButton.LEFT, MouseHoldMode.RELEASE)
        assertEquals("전부 해제=0x00", 0x00.toUByte(), bits)
        assertTrue("heldMouseButtons 비어있음", state.heldMouseButtons.isEmpty())
    }
}
