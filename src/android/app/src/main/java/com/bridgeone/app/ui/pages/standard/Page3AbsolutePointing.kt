package com.bridgeone.app.ui.pages.standard

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bridgeone.app.ui.common.TouchpadEdgeZoneAssignment
import com.bridgeone.app.ui.components.AbsolutePointingPad
import com.bridgeone.app.ui.components.touchpad.MacroStep
import com.bridgeone.app.ui.components.touchpad.MouseButton
import com.bridgeone.app.ui.components.touchpad.MouseHoldMode
import com.bridgeone.app.ui.components.touchpad.PageNav
import com.bridgeone.app.ui.utils.MagnificationMode

// ============================================================
// Page 3: 절대좌표 패드 (Phase 4.9.1, 엣지존 통합은 Phase 4.9.3)
// ============================================================

@Composable
internal fun Page3AbsolutePointing(
    edgeZoneAssignment: TouchpadEdgeZoneAssignment = TouchpadEdgeZoneAssignment.default(),
    onEdgeZoneAssignmentChange: (TouchpadEdgeZoneAssignment) -> Unit = {},
    onRestorePrevious: () -> Unit = {},
    onSendShortcut: (Int, List<Int>, Boolean) -> Unit = { _, _, _ -> },
    onSendMacro: (List<MacroStep>, Int) -> Unit = { _, _ -> },
    onMouseHoldToggle: (MouseButton, MouseHoldMode) -> Unit = { _, _ -> },
    onCyclePage: (PageNav) -> Unit = {},
    onJumpToPage: (Int) -> Unit = {},
    magnificationMode: MagnificationMode = MagnificationMode.Off,
    onMagnificationModeChange: (MagnificationMode) -> Unit = {}
) {
    AbsolutePointingPad(
        modifier = Modifier.fillMaxSize(),
        edgeZoneAssignment = edgeZoneAssignment,
        onEdgeZoneAssignmentChange = onEdgeZoneAssignmentChange,
        onRestorePrevious = onRestorePrevious,
        onSendShortcut = onSendShortcut,
        onSendMacro = onSendMacro,
        onMouseHoldToggle = onMouseHoldToggle,
        onCyclePage = onCyclePage,
        onJumpToPage = onJumpToPage,
        magnificationMode = magnificationMode,
        onMagnificationModeChange = onMagnificationModeChange
    )
}
