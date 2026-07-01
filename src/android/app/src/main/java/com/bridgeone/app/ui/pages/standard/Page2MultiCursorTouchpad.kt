package com.bridgeone.app.ui.pages.standard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.bridgeone.app.protocol.BridgeMode
import com.bridgeone.app.ui.common.CustomPointerDynamicsPreset
import com.bridgeone.app.ui.common.MultiCursorConstants
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
import com.bridgeone.app.ui.components.touchpad.MultiCursorLayoutMode
import com.bridgeone.app.ui.components.touchpad.MultiCursorState
import com.bridgeone.app.ui.components.touchpad.PageNav
import com.bridgeone.app.ui.components.touchpad.TouchpadState
import com.bridgeone.app.ui.components.touchpad.divideGridAreas

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
    onPadSwitch: (Int) -> Unit = {},
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
    // Phase 4.8.3: 그리드 분할 UI에서 activePadIndex가 바뀌면 이 projection도 자동으로 따라간다.
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
        val showGrid = multiCursorState.isEnabled && multiCursorState.layoutMode == MultiCursorLayoutMode.GRID
        if (showGrid) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1A1A1A))
                    .border(
                        width = MultiCursorConstants.GRID_OUTER_BORDER_WIDTH_DP.dp,
                        color = Color.White.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                val density = LocalDensity.current
                val widthPx = with(density) { maxWidth.toPx() }
                val heightPx = with(density) { maxHeight.toPx() }
                val areas = remember(widthPx, heightPx, multiCursorState.cursorCount) {
                    divideGridAreas(widthPx, heightPx, multiCursorState.cursorCount)
                }
                areas.forEachIndexed { index, rect ->
                    val offsetXDp = with(density) { rect.left.toDp() }
                    val offsetYDp = with(density) { rect.top.toDp() }
                    val cellWidthDp = with(density) { (rect.right - rect.left).toDp() }
                    val cellHeightDp = with(density) { (rect.bottom - rect.top).toDp() }
                    val cellModifier = Modifier
                        .offset(x = offsetXDp, y = offsetYDp)
                        .size(cellWidthDp, cellHeightDp)

                    if (index == multiCursorState.activePadIndex) {
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
                            modifier = cellModifier.background(color = Color(0xFF1A1A1A))
                        )
                    } else {
                        val dimAlpha by animateFloatAsState(
                            targetValue = MultiCursorConstants.GRID_INACTIVE_PAD_DIM_ALPHA,
                            animationSpec = tween(MultiCursorConstants.GRID_PAD_SWITCH_ANIM_DURATION_MS),
                            label = "gridInactivePadDim"
                        )
                        val dashStrokeWidthPx = with(density) { MultiCursorConstants.GRID_INACTIVE_BORDER_WIDTH_DP.dp.toPx() }
                        val dashOnPx = with(density) { MultiCursorConstants.GRID_DASH_ON_LENGTH_DP.dp.toPx() }
                        val dashOffPx = with(density) { MultiCursorConstants.GRID_DASH_OFF_LENGTH_DP.dp.toPx() }
                        // 그리드 외곽 테두리와 겹치지 않도록, 전체 영역 바깥쪽과 맞닿는 변은 점선을 그리지 않고
                        // 다른 패드와 맞닿는 내부 경계에만 점선을 그린다.
                        val isLeftEdge = rect.left <= 0.5f
                        val isTopEdge = rect.top <= 0.5f
                        val isRightEdge = rect.right >= widthPx - 0.5f
                        val isBottomEdge = rect.bottom >= heightPx - 0.5f
                        Box(
                            modifier = cellModifier
                                .background(Color(0xFF1A1A1A))
                                .pointerInput(index) {
                                    detectTapGestures { onPadSwitch(index) }
                                }
                                .drawBehind {
                                    val inset = dashStrokeWidthPx / 2
                                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(dashOnPx, dashOffPx))
                                    val dashColor = Color.White.copy(alpha = 0.5f)
                                    if (!isTopEdge) {
                                        drawLine(dashColor, Offset(0f, inset), Offset(size.width, inset), dashStrokeWidthPx, pathEffect = dashEffect)
                                    }
                                    if (!isBottomEdge) {
                                        drawLine(dashColor, Offset(0f, size.height - inset), Offset(size.width, size.height - inset), dashStrokeWidthPx, pathEffect = dashEffect)
                                    }
                                    if (!isLeftEdge) {
                                        drawLine(dashColor, Offset(inset, 0f), Offset(inset, size.height), dashStrokeWidthPx, pathEffect = dashEffect)
                                    }
                                    if (!isRightEdge) {
                                        drawLine(dashColor, Offset(size.width - inset, 0f), Offset(size.width - inset, size.height), dashStrokeWidthPx, pathEffect = dashEffect)
                                    }
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = dimAlpha))
                            )
                        }
                    }
                }
            }
        } else {
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
        }
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
