package com.bridgeone.app.ui.pages.standard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bridgeone.app.protocol.BridgeMode
import com.bridgeone.app.ui.common.CustomPointerDynamicsPreset
import com.bridgeone.app.ui.common.TouchpadButtonVisibility
import com.bridgeone.app.ui.common.TouchpadEdgeZoneAssignment
import com.bridgeone.app.ui.common.TouchpadIds
import com.bridgeone.app.ui.components.TouchpadWrapper
import com.bridgeone.app.ui.components.touchpad.ControlButtonContainer
import com.bridgeone.app.ui.components.touchpad.MacroStep
import com.bridgeone.app.ui.components.touchpad.MouseButton
import com.bridgeone.app.ui.components.touchpad.MouseHoldMode
import com.bridgeone.app.ui.components.touchpad.PageNav
import com.bridgeone.app.ui.components.touchpad.TouchpadState

// ============================================================
// Page 2: 테스트 터치패드 (제어 버튼 없는 풀스크린 터치패드)
// ============================================================

@Composable
internal fun Page2TestTouchpad(
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
    onDpiLongPress: () -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxSize()) {
        TouchpadWrapper(
            touchpadId = TouchpadIds.standardPage(1),
            bridgeMode = BridgeMode.STANDARD,
            touchpadState = touchpadState,
            edgeZoneAssignment = edgeZoneAssignment,
            onEdgeZoneAssignmentChange = onEdgeZoneAssignmentChange,
            customPresets = customPresets,
            onTouchpadStateChange = onTouchpadStateChange,
            onRestorePrevious = onRestorePrevious,
            onSendShortcut = onSendShortcut,
            onSendMacro = onSendMacro,
            onMouseHoldToggle = onMouseHoldToggle,
            onCyclePage = onCyclePage,
            onJumpToPage = onJumpToPage,
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
                touchpadState = touchpadState,
                onStateChange = onTouchpadStateChange,
                onDpiLongPress = onDpiLongPress,
                config = buttonVisibility.controlButtonConfig,
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.TopCenter)
            )
        }
    }
}
