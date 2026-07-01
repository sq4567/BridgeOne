package com.bridgeone.app.ui.pages.standard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.bridgeone.app.protocol.BridgeMode
import com.bridgeone.app.ui.common.CustomPointerDynamicsPreset
import com.bridgeone.app.ui.common.TouchpadButtonVisibility
import com.bridgeone.app.ui.common.TouchpadEdgeZoneAssignment
import com.bridgeone.app.ui.common.TouchpadIds
import com.bridgeone.app.ui.components.TouchpadWrapper
import com.bridgeone.app.ui.components.touchpad.ControlButtonContainer
import com.bridgeone.app.ui.components.touchpad.CursorCountSelectionPopup
import com.bridgeone.app.ui.components.touchpad.MacroStep
import com.bridgeone.app.ui.components.touchpad.MouseButton
import com.bridgeone.app.ui.components.touchpad.MouseHoldMode
import com.bridgeone.app.ui.components.touchpad.ModePresetPopup
import com.bridgeone.app.ui.components.touchpad.MultiCursorState
import com.bridgeone.app.ui.components.touchpad.PageNav
import com.bridgeone.app.ui.components.touchpad.TouchpadState

// ============================================================
// Page 2: 멀티 커서 홈 — 풀 와이드 터치패드 (Phase 4.8)
// ============================================================

@Composable
internal fun Page2MultiCursorTouchpad(
    touchpadState: TouchpadState,
    edgeZoneAssignment: TouchpadEdgeZoneAssignment = TouchpadEdgeZoneAssignment.default(),
    onEdgeZoneAssignmentChange: (TouchpadEdgeZoneAssignment) -> Unit = {},
    customPresets: List<CustomPointerDynamicsPreset> = emptyList(),
    onTouchpadStateChange: (TouchpadState) -> Unit = {},
    onRestorePrevious: () -> Unit = {},
    onSendShortcut: (Int, List<Int>, Boolean) -> Unit = { _, _, _ -> },
    onSendMacro: (List<MacroStep>, Int) -> Unit = { _, _ -> },
    onMouseHoldToggle: (MouseButton, MouseHoldMode) -> Unit = { _, _ -> },
    onCyclePage: (PageNav) -> Unit = {},
    onJumpToPage: (Int) -> Unit = {},
    buttonVisibility: TouchpadButtonVisibility = TouchpadButtonVisibility.defaultFor(TouchpadIds.standardPage(1)),
    onDpiLongPress: () -> Unit = {},
    multiCursorState: MultiCursorState = MultiCursorState(),
    onCursorModeClick: () -> Unit = {},
    onActivePadModeChange: (TouchpadState) -> Unit = {},
    cursorCountPopupVisible: Boolean = false,
    onCursorCountSelected: (Int) -> Unit = {},
    onCursorCountDismiss: () -> Unit = {},
    onModePresetLongPress: () -> Unit = {},
    modePresetPopupVisible: Boolean = false,
    onModePresetConfirmed: (Int) -> Unit = {},
    onModePresetDismiss: () -> Unit = {}
) {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val touchpadWidthFraction = if (screenWidthDp < 360) 0.60f else 0.64f

    // Phase 4.8.2: 멀티 커서 활성 시 제어 버튼은 활성 패드의 4개 모드 필드를 읽고 쓴다(projection).
    // 활성 패드 전환 UI는 4.8.3에서 추가되며, 그전까지는 항상 pad1(index 0)만 도달 가능하다.
    val effectiveState = if (multiCursorState.isEnabled) {
        val padMode = multiCursorState.activePadModeState
        touchpadState.copy(
            clickMode = padMode.clickMode,
            moveMode = padMode.moveMode,
            scrollMode = padMode.scrollMode,
            dpiLevel = padMode.dpi
        )
    } else {
        touchpadState
    }
    val effectiveOnStateChange: (TouchpadState) -> Unit = { newState ->
        if (multiCursorState.isEnabled) {
            // 클릭/이동/스크롤/DPI 변경만 활성 패드로 라우팅, 나머지 필드는 글로벌 상태로 라우팅
            val padModeChanged = newState.clickMode != effectiveState.clickMode ||
                newState.moveMode != effectiveState.moveMode ||
                newState.scrollMode != effectiveState.scrollMode ||
                newState.dpiLevel != effectiveState.dpiLevel
            if (padModeChanged) onActivePadModeChange(newState)

            val globalUpdate = newState.copy(
                clickMode = touchpadState.clickMode,
                moveMode = touchpadState.moveMode,
                scrollMode = touchpadState.scrollMode,
                dpiLevel = touchpadState.dpiLevel
            )
            if (globalUpdate != touchpadState) onTouchpadStateChange(globalUpdate)
        } else {
            onTouchpadStateChange(newState)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        TouchpadWrapper(
            touchpadId = TouchpadIds.standardPage(1),
            bridgeMode = BridgeMode.STANDARD,
            touchpadState = effectiveState,
            edgeZoneAssignment = edgeZoneAssignment,
            onEdgeZoneAssignmentChange = onEdgeZoneAssignmentChange,
            customPresets = customPresets,
            onTouchpadStateChange = effectiveOnStateChange,
            onRestorePrevious = onRestorePrevious,
            onSendShortcut = onSendShortcut,
            onSendMacro = onSendMacro,
            onMouseHoldToggle = onMouseHoldToggle,
            onCyclePage = onCyclePage,
            onJumpToPage = onJumpToPage,
            onModePresetLongPress = onModePresetLongPress,
            buttonVisibility = buttonVisibility,
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = Color(0xFF1A1A1A),
                    shape = RoundedCornerShape(12.dp)
                )
        )
        if (buttonVisibility.showControlButtons) {
            ControlButtonContainer(
                touchpadState = effectiveState,
                onStateChange = effectiveOnStateChange,
                onDpiLongPress = onDpiLongPress,
                onCursorModeClick = onCursorModeClick,
                config = buttonVisibility.controlButtonConfig,
                modifier = Modifier
                    .fillMaxWidth(touchpadWidthFraction)
                    .align(Alignment.TopCenter)
            )
        }

        // Phase 4.8.2: 커서 수 선택 팝업 (싱글 → 멀티 시도 시)
        CursorCountSelectionPopup(
            visible = cursorCountPopupVisible,
            onSelect = onCursorCountSelected,
            onDismiss = onCursorCountDismiss,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
        )

        // 모드 프리셋 팝업 오버레이 (멀티 커서 활성 시 활성 패드에만 적용, Phase 4.8.2)
        ModePresetPopup(
            visible = modePresetPopupVisible,
            currentIndex = touchpadState.modePresetIndex,
            onPresetConfirmed = onModePresetConfirmed,
            onDismiss = onModePresetDismiss,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
        )
    }
}
