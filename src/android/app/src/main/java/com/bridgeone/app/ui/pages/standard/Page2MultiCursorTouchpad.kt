package com.bridgeone.app.ui.pages.standard

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOut
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bridgeone.app.protocol.BridgeMode
import com.bridgeone.app.ui.common.CustomPointerDynamicsPreset
import com.bridgeone.app.ui.common.MultiCursorConstants
import com.bridgeone.app.ui.common.TouchpadButtonVisibility
import com.bridgeone.app.ui.common.TouchpadEdgeZoneAssignment
import com.bridgeone.app.ui.common.TouchpadIds
import com.bridgeone.app.ui.components.TouchpadWrapper
import com.bridgeone.app.ui.components.touchpad.ControlButtonContainer
import com.bridgeone.app.ui.components.touchpad.CursorCountSelectionPopup
import com.bridgeone.app.ui.components.touchpad.EdgeZoneAction
import com.bridgeone.app.ui.components.touchpad.MacroStep
import com.bridgeone.app.ui.components.touchpad.MouseButton
import com.bridgeone.app.ui.components.touchpad.MouseHoldMode
import com.bridgeone.app.ui.components.touchpad.ModePresetPopup
import com.bridgeone.app.ui.components.touchpad.MULTI_CURSOR_COUNT_MIN
import com.bridgeone.app.ui.components.touchpad.MultiCursorLayoutMode
import com.bridgeone.app.ui.components.touchpad.MultiCursorState
import com.bridgeone.app.ui.components.touchpad.PadSwitchButtonPanel
import com.bridgeone.app.ui.components.touchpad.PageNav
import com.bridgeone.app.ui.components.touchpad.TouchpadColorBlue
import com.bridgeone.app.ui.components.touchpad.TouchpadState
import com.bridgeone.app.ui.components.touchpad.divideGridAreas
import com.bridgeone.app.ui.components.touchpad.touchpadBorderColors
import kotlinx.coroutines.launch

// ============================================================
// Page 2: 멀티 커서 홈 — 풀 와이드 터치패드 (Phase 4.8)
// ============================================================

@Composable
internal fun Page2MultiCursorTouchpad(
    touchpadState: TouchpadState,
    edgeZoneAssignment: TouchpadEdgeZoneAssignment = TouchpadEdgeZoneAssignment.default(),
    onEdgeZoneAssignmentChange: (TouchpadEdgeZoneAssignment) -> Unit = {},
    // Phase 4.8.9: 패드별 엣지 존 할당(멀티 커서 활성 시 활성 패드 것을 사용, 인덱스 0~3)
    padEdgeZoneAssignments: List<TouchpadEdgeZoneAssignment> = emptyList(),
    onPadEdgeZoneAssignmentChange: (Int, TouchpadEdgeZoneAssignment) -> Unit = { _, _ -> },
    customPresets: List<CustomPointerDynamicsPreset> = emptyList(),
    onTouchpadStateChange: (TouchpadState) -> Unit = {},
    onRestorePrevious: () -> Unit = {},
    onSendShortcut: (Int, List<Int>, Boolean) -> Unit = { _, _, _ -> },
    onSendMacro: (List<MacroStep>, Int) -> Unit = { _, _ -> },
    onMouseHoldToggle: (MouseButton, MouseHoldMode) -> Unit = { _, _ -> },
    onCyclePage: (PageNav) -> Unit = {},
    onJumpToPage: (Int) -> Unit = {},
    onMultiCursorAction: (EdgeZoneAction) -> Unit = {},
    // 엣지 팝업 커서 개수 선택 서브 화면 진입 시 강조할 현재 멀티 커서 수. 기본값: MULTI_CURSOR_COUNT_MIN
    currentMultiCursorCount: Int = MULTI_CURSOR_COUNT_MIN,
    buttonVisibility: TouchpadButtonVisibility = TouchpadButtonVisibility.defaultFor(TouchpadIds.standardPage(1)),
    onDpiLongPress: () -> Unit = {},
    multiCursorState: MultiCursorState = MultiCursorState(),
    onCursorModeClick: () -> Unit = {},
    onCursorModeLongPress: () -> Unit = {},
    onActivePadModeChange: (TouchpadState) -> Unit = {},
    onPadSwitch: (Int) -> Unit = {},
    cursorCountPopupVisible: Boolean = false,
    onCursorCountDismiss: () -> Unit = {},
    onCursorCountDisable: () -> Unit = {},
    // Phase 4.8.8: 패드별 프리셋 시드. 팝업 PRESET 단계의 초기값 및 확정 콜백.
    padPresetMapping: List<Int?> = emptyList(),
    onCursorCountConfirmed: (Int, List<Int?>) -> Unit = { _, _ -> },
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

    // Phase 4.8.9: 멀티 커서 활성 시 활성 패드의 엣지 존 할당을, 싱글 모드는 기존 페이지 단위 할당을 사용
    val effectiveAssignment = if (multiCursorState.isEnabled) {
        padEdgeZoneAssignments.getOrNull(multiCursorState.activePadIndex) ?: edgeZoneAssignment
    } else {
        edgeZoneAssignment
    }
    val effectiveOnAssignmentChange: (TouchpadEdgeZoneAssignment) -> Unit = if (multiCursorState.isEnabled) {
        { updated -> onPadEdgeZoneAssignmentChange(multiCursorState.activePadIndex, updated) }
    } else {
        onEdgeZoneAssignmentChange
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val showGrid = multiCursorState.isEnabled && multiCursorState.layoutMode == MultiCursorLayoutMode.GRID
        val showDirectButton = multiCursorState.isEnabled && multiCursorState.layoutMode == MultiCursorLayoutMode.DIRECT_BUTTON
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
                val view = LocalView.current
                val widthPx = with(density) { maxWidth.toPx() }
                val heightPx = with(density) { maxHeight.toPx() }
                val areas = remember(widthPx, heightPx, multiCursorState.cursorCount) {
                    divideGridAreas(widthPx, heightPx, multiCursorState.cursorCount)
                }

                // 레이어 1: 셀 본체 — 활성 셀엔 TouchpadWrapper, 비활성 셀엔 점선 테두리 + 탭 감지
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
                            edgeZoneAssignment = effectiveAssignment,
                            onEdgeZoneAssignmentChange = effectiveOnAssignmentChange,
                            customPresets = customPresets,
                            onTouchpadStateChange = effectiveOnStateChange,
                            onRestorePrevious = onRestorePrevious,
                            onSendShortcut = onSendShortcut,
                            onSendMacro = onSendMacro,
                            onMouseHoldToggle = onMouseHoldToggle,
                            onCyclePage = onCyclePage,
                            onJumpToPage = onJumpToPage,
                            onMultiCursorAction = onMultiCursorAction,
                            currentMultiCursorCount = currentMultiCursorCount,
                            onModePresetLongPress = onModePresetLongPress,
                            buttonVisibility = buttonVisibility,
                            modifier = cellModifier.background(color = Color(0xFF1A1A1A))
                        )
                    } else {
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
                                    detectTapGestures {
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        onPadSwitch(index)
                                    }
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
                        )
                    }
                }

                // 레이어 2: dim 오버레이 + 번호 라벨 — 전 셀 상시 컴포지션(재마운트 없음)이라 fade가 실제로 재생된다.
                // 포인터 modifier가 없어 터치는 레이어 1로 그대로 통과한다.
                areas.forEachIndexed { index, rect ->
                    val offsetXDp = with(density) { rect.left.toDp() }
                    val offsetYDp = with(density) { rect.top.toDp() }
                    val cellWidthDp = with(density) { (rect.right - rect.left).toDp() }
                    val cellHeightDp = with(density) { (rect.bottom - rect.top).toDp() }
                    val isActive = index == multiCursorState.activePadIndex
                    val dimAlpha by animateFloatAsState(
                        targetValue = if (isActive) 0f else MultiCursorConstants.GRID_INACTIVE_PAD_DIM_ALPHA,
                        animationSpec = tween(MultiCursorConstants.GRID_PAD_SWITCH_ANIM_DURATION_MS),
                        label = "gridInactivePadDim"
                    )
                    Box(
                        modifier = Modifier
                            .offset(x = offsetXDp, y = offsetYDp)
                            .size(cellWidthDp, cellHeightDp)
                            .background(Color.Black.copy(alpha = dimAlpha)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!isActive) {
                            Text(
                                text = "${index + 1}",
                                color = Color.White.copy(alpha = MultiCursorConstants.GRID_CELL_LABEL_ALPHA),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // 레이어 3: 슬라이드 하이라이트 — 활성 패드 강조가 이전 셀에서 새 셀로 미끄러져 이동 후
                // 도착 펄스로 소멸한다. 최상단 비인터랙티브 레이어라 히트 테스트에 관여하지 않는다.
                val activeRect = areas[multiCursorState.activePadIndex]
                val highlightLeft = remember { Animatable(activeRect.left) }
                val highlightTop = remember { Animatable(activeRect.top) }
                val highlightRight = remember { Animatable(activeRect.right) }
                val highlightBottom = remember { Animatable(activeRect.bottom) }
                val highlightAlpha = remember { Animatable(0f) }
                var prevPadIndex by remember { mutableIntStateOf(multiCursorState.activePadIndex) }

                LaunchedEffect(multiCursorState.activePadIndex, areas) {
                    if (prevPadIndex == multiCursorState.activePadIndex) {
                        // 셀 크기만 바뀐 경우(회전 등) — 슬라이드 없이 스냅
                        highlightLeft.snapTo(activeRect.left)
                        highlightTop.snapTo(activeRect.top)
                        highlightRight.snapTo(activeRect.right)
                        highlightBottom.snapTo(activeRect.bottom)
                        return@LaunchedEffect
                    }
                    prevPadIndex = multiCursorState.activePadIndex
                    highlightAlpha.snapTo(1f)
                    launch { highlightLeft.animateTo(activeRect.left, tween(MultiCursorConstants.GRID_HIGHLIGHT_SLIDE_DURATION_MS, easing = EaseInOut)) }
                    launch { highlightTop.animateTo(activeRect.top, tween(MultiCursorConstants.GRID_HIGHLIGHT_SLIDE_DURATION_MS, easing = EaseInOut)) }
                    launch { highlightRight.animateTo(activeRect.right, tween(MultiCursorConstants.GRID_HIGHLIGHT_SLIDE_DURATION_MS, easing = EaseInOut)) }
                    highlightBottom.animateTo(activeRect.bottom, tween(MultiCursorConstants.GRID_HIGHLIGHT_SLIDE_DURATION_MS, easing = EaseInOut))
                    // 도착 펄스 (component-touchpad.md §3.2.4 "파란색 펄스" 흡수 구현)
                    highlightAlpha.snapTo(MultiCursorConstants.GRID_HIGHLIGHT_ARRIVAL_PULSE_ALPHA)
                    highlightAlpha.animateTo(0f, tween(MultiCursorConstants.GRID_HIGHLIGHT_FADE_OUT_MS))
                }

                val highlightStrokeWidthPx = with(density) { MultiCursorConstants.GRID_HIGHLIGHT_STROKE_WIDTH_DP.dp.toPx() }
                val highlightCornerRadiusPx = with(density) { MultiCursorConstants.GRID_HIGHLIGHT_CORNER_RADIUS_DP.dp.toPx() }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            if (highlightAlpha.value <= 0f) return@drawBehind
                            val inset = highlightStrokeWidthPx / 2
                            val rect = Rect(
                                left = highlightLeft.value + inset,
                                top = highlightTop.value + inset,
                                right = highlightRight.value - inset,
                                bottom = highlightBottom.value - inset
                            )
                            drawRoundRect(
                                color = TouchpadColorBlue.copy(alpha = highlightAlpha.value),
                                topLeft = rect.topLeft,
                                size = rect.size,
                                cornerRadius = CornerRadius(highlightCornerRadiusPx),
                                style = Stroke(highlightStrokeWidthPx)
                            )
                        }
                )
            }
        } else if (showDirectButton) {
            // Phase 4.8.4: 직접 전환 버튼 모드 — 터치패드 전체 면적(하단 패널 높이 제외) + 하단 전환 버튼 패널
            // Phase 4.8.6: 패드 전환 시 본체가 페이저처럼 방향성 있게 밀리는 슬라이드 추가.
            // TouchpadWrapper는 재마운트하지 않고(관성/엣지 팝업 상태 보존) graphicsLayer로만 이동시킨다.
            val slideProgress = remember { Animatable(0f) } // 1f = 전환 시작(화면 밖), 0f = 정착
            var slideDirection by remember { mutableIntStateOf(1) }
            var prevDirectPadIndex by remember { mutableIntStateOf(multiCursorState.activePadIndex) }
            var ghostBorderState by remember { mutableStateOf<TouchpadState?>(null) }

            LaunchedEffect(multiCursorState.activePadIndex) {
                val old = prevDirectPadIndex
                prevDirectPadIndex = multiCursorState.activePadIndex
                if (old == multiCursorState.activePadIndex) return@LaunchedEffect
                slideDirection = if (multiCursorState.activePadIndex > old) 1 else -1
                val oldPadMode = multiCursorState.padModeStates.getOrNull(old)
                ghostBorderState = oldPadMode?.let {
                    TouchpadState(clickMode = it.clickMode, moveMode = it.moveMode, scrollMode = it.scrollMode)
                }
                slideProgress.snapTo(1f)
                slideProgress.animateTo(0f, tween(MultiCursorConstants.DIRECT_SLIDE_DURATION_MS, easing = EaseInOut))
                ghostBorderState = null
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = MultiCursorConstants.DIRECT_BUTTON_PANEL_HEIGHT_DP.dp + MultiCursorConstants.DIRECT_BUTTON_PANEL_TOP_GAP_DP.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                val density = LocalDensity.current
                val widthPx = with(density) { maxWidth.toPx() }

                val ghostState = ghostBorderState
                if (ghostState != null && slideProgress.value > 0f) {
                    val (ghostLeft, ghostRight) = touchpadBorderColors(ghostState)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { translationX = slideDirection * (slideProgress.value - 1f) * widthPx }
                            .background(color = Color(0xFF1A1A1A), shape = RoundedCornerShape(12.dp))
                            .border(
                                width = 2.dp,
                                brush = Brush.horizontalGradient(listOf(ghostLeft, ghostRight)),
                                shape = RoundedCornerShape(12.dp)
                            )
                    )
                }

                TouchpadWrapper(
                    touchpadId = TouchpadIds.standardPage(1),
                    bridgeMode = BridgeMode.STANDARD,
                    touchpadState = effectiveState,
                    edgeZoneAssignment = effectiveAssignment,
                    onEdgeZoneAssignmentChange = effectiveOnAssignmentChange,
                    customPresets = customPresets,
                    onTouchpadStateChange = effectiveOnStateChange,
                    onRestorePrevious = onRestorePrevious,
                    onSendShortcut = onSendShortcut,
                    onSendMacro = onSendMacro,
                    onMouseHoldToggle = onMouseHoldToggle,
                    onCyclePage = onCyclePage,
                    onJumpToPage = onJumpToPage,
                    onMultiCursorAction = onMultiCursorAction,
                    currentMultiCursorCount = currentMultiCursorCount,
                    onModePresetLongPress = onModePresetLongPress,
                    buttonVisibility = buttonVisibility,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { translationX = slideDirection * slideProgress.value * widthPx }
                        .background(
                            color = Color(0xFF1A1A1A),
                            shape = RoundedCornerShape(12.dp)
                        )
                )
            }
            PadSwitchButtonPanel(
                cursorCount = multiCursorState.cursorCount,
                activePadIndex = multiCursorState.activePadIndex,
                onPadSwitch = onPadSwitch,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
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
                onMultiCursorAction = onMultiCursorAction,
                currentMultiCursorCount = currentMultiCursorCount,
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
        // Phase 4.8.6: 커서 수 선택 팝업을 제어 버튼 줄 바로 아래에 앵커링하기 위해
        // ControlButtonContainer의 실제 렌더 높이를 측정한다(controlHeight coerce 범위가
        // 화면마다 달라 하드코딩 오프셋으로는 겹침을 피할 수 없음).
        val controlButtonDensity = LocalDensity.current
        var controlButtonHeightDp by remember { mutableStateOf(0.dp) }
        if (buttonVisibility.showControlButtons) {
            ControlButtonContainer(
                touchpadState = effectiveState,
                onStateChange = effectiveOnStateChange,
                onDpiLongPress = onDpiLongPress,
                onCursorModeClick = onCursorModeClick,
                onCursorModeLongPress = onCursorModeLongPress,
                config = buttonVisibility.controlButtonConfig,
                modifier = Modifier
                    .fillMaxWidth(touchpadWidthFraction)
                    .align(Alignment.TopCenter)
                    .onGloballyPositioned {
                        controlButtonHeightDp = with(controlButtonDensity) { it.size.height.toDp() }
                    }
            )
        }

        // Phase 4.8.2: 커서 수 선택 팝업 (싱글 → 멀티 시도 시)
        CursorCountSelectionPopup(
            visible = cursorCountPopupVisible,
            onDismiss = onCursorCountDismiss,
            currentCount = if (multiCursorState.isEnabled) multiCursorState.cursorCount else null,
            onDisable = if (multiCursorState.isEnabled) onCursorCountDisable else null,
            anchorTopDp = controlButtonHeightDp,
            initialPadPresetMapping = padPresetMapping,
            onConfirm = onCursorCountConfirmed,
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
