package com.bridgeone.app.ui.components

import android.util.Log
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.bridgeone.app.ui.common.EdgeSwipeConstants
import com.bridgeone.app.ui.common.TouchpadEdgeZoneAssignment
import com.bridgeone.app.ui.common.loadTargetMonitor
import com.bridgeone.app.ui.common.saveTargetMonitor
import com.bridgeone.app.ui.components.touchpad.ClickMode
import com.bridgeone.app.ui.components.touchpad.ControlButtonConfig
import com.bridgeone.app.ui.components.touchpad.ControlButtonContainer
import com.bridgeone.app.ui.components.touchpad.EdgeBumpOverlay
import com.bridgeone.app.ui.components.touchpad.EdgeZoneAction
import com.bridgeone.app.ui.components.touchpad.EdgeZoneActionHandler
import com.bridgeone.app.ui.components.touchpad.EdgeZoneConfig
import com.bridgeone.app.ui.components.touchpad.EdgeZoneDetector
import com.bridgeone.app.ui.components.touchpad.EdgeZoneOverlay
import com.bridgeone.app.ui.components.touchpad.EdgeZoneTrigger
import com.bridgeone.app.ui.components.touchpad.EntryEdge
import com.bridgeone.app.ui.components.touchpad.MacroStep
import com.bridgeone.app.ui.components.touchpad.MouseButton
import com.bridgeone.app.ui.components.touchpad.MouseHoldMode
import com.bridgeone.app.ui.components.touchpad.PageNav
import com.bridgeone.app.ui.components.touchpad.TouchpadColorGreen
import com.bridgeone.app.ui.components.touchpad.TouchpadColorPink
import com.bridgeone.app.ui.components.touchpad.TouchpadColorRed
import com.bridgeone.app.ui.components.touchpad.TouchpadColorYellow
import com.bridgeone.app.ui.components.touchpad.TouchpadState
import com.bridgeone.app.ui.components.touchpad.detectEntryEdge
import com.bridgeone.app.ui.components.touchpad.filterConfigForAbsolutePad
import com.bridgeone.app.ui.components.touchpad.getAlongEdgePosition
import com.bridgeone.app.ui.components.touchpad.getInwardDistance
import com.bridgeone.app.ui.components.touchpad.unmapFromValid
import com.bridgeone.app.protocol.FrameBuilder
import com.bridgeone.app.usb.UsbSerialManager
import com.bridgeone.app.ui.utils.AbsoluteCoordinateCalculator
import com.bridgeone.app.ui.utils.AbsolutePointingConstants
import com.bridgeone.app.ui.utils.ClickDetector
import com.bridgeone.app.ui.utils.TouchRatio
import com.bridgeone.app.ui.utils.resolveTargetMonitor
import com.bridgeone.app.ui.utils.getDistance
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ============================================================
// AbsolutePointingPad (Page 3) — Phase 4.9.1: 기본 구현 (자유 비율)
// ============================================================
//
// 터치한 위치가 곧 PC 커서 위치가 되는 절대좌표 포인팅 페이지.
// Phase 4.9.2: 터치 비율을 FrameBuilder.buildAbsolutePositionCommand()로 인코딩해
// UsbSerialManager.sendCommandBytes()로 서버 중계 전송한다(DOWN 즉시 전송 + MOVE 실시간 전송).
// Phase 4.9.3: 엣지존 통합(CLICK 전용). ZONE 모드만 지원하며(LEGACY_POPUP 배제),
// 좌표 무관 이산 액션(매크로/단축키/페이지 전환/클릭모드 토글/마우스 홀드)만 노출한다.
// 존 단위 gate(화이트리스트 통과 존이 있는 위치에서 시작한 터치만 엣지 후보로 인식) +
// 엣지 띠 탭=좌표클릭(탭이면 armed 여부와 무관하게 그 지점 좌표로 클릭)으로 좌표 도달성
// 손실을 0으로 유지한다.
//
// Reference: docs/development-plans/phase-4/phase-4-9-page3-absolute-pointing.md

/** [EdgeZoneTrigger]에 실행 가능한(Unassigned가 아닌) 액션이 하나라도 있는지 판정. */
private fun EdgeZoneTrigger.hasAssignedAction(): Boolean = when (this) {
    is EdgeZoneTrigger.SingleAction -> action !is EdgeZoneAction.Unassigned
    is EdgeZoneTrigger.Rotation -> candidates.isNotEmpty()
}

/** [EdgeZoneTrigger]에서 실행할 액션을 결정(로테이션은 4.9.9 전까지 첫 후보로 고정). */
private fun EdgeZoneTrigger.resolveAction(): EdgeZoneAction? = when (this) {
    is EdgeZoneTrigger.SingleAction -> action.takeIf { it !is EdgeZoneAction.Unassigned }
    is EdgeZoneTrigger.Rotation -> candidates.firstOrNull()?.action?.takeIf { it !is EdgeZoneAction.Unassigned }
}

/**
 * 절대좌표 패드 페이지. PointingArea(자유 비율, stretch 매핑) + 상단 ControlButtonContainer
 * (ClickModeButton/DragModeButton 활성, ZoomButton은 Disabled 슬롯)로 구성된다.
 *
 * @param edgeZoneAssignment Page 3 전용 엣지존 할당(Phase 4.9.3). 화이트리스트 필터를 거쳐 사용.
 * @param onEdgeZoneAssignmentChange 존 할당 변경 콜백(편집 UI는 4.9.9, 현재는 시그니처만 배선).
 * @param onRestorePrevious/onSendShortcut/onSendMacro/onMouseHoldToggle/onCyclePage/onJumpToPage
 *        엣지존 부수효과형 액션 콜백. Page 1이 쓰는 StandardModePage의 기존 콜백을 그대로 재사용.
 */
@Composable
fun AbsolutePointingPad(
    modifier: Modifier = Modifier,
    edgeZoneAssignment: TouchpadEdgeZoneAssignment = TouchpadEdgeZoneAssignment.default(),
    onEdgeZoneAssignmentChange: (TouchpadEdgeZoneAssignment) -> Unit = {},
    onRestorePrevious: () -> Unit = {},
    onSendShortcut: (Int, List<Int>, Boolean) -> Unit = { _, _, _ -> },
    onSendMacro: (List<MacroStep>, Int) -> Unit = { _, _ -> },
    onMouseHoldToggle: (MouseButton, MouseHoldMode) -> Unit = { _, _ -> },
    onCyclePage: (PageNav) -> Unit = {},
    onJumpToPage: (Int) -> Unit = {}
) {
    // 클릭 모드는 Page 1/2의 pageState.touchpadState와 공유하지 않는 페이지 로컬 상태.
    // ControlButtonContainer가 요구하는 TouchpadState 타입을 재사용하되 clickMode 외 필드는 미사용.
    var localState by remember { mutableStateOf(TouchpadState()) }

    // ── 모니터 셀렉터 상태 (Phase 4.9.5) ──
    // targetMonitor: 0x00=전체 가상 데스크톱, 0x01~N=특정 모니터 인덱스. UByte 프레임 규약과 동일한 Int로 다룬다.
    val context = LocalContext.current
    val monitorCount by UsbSerialManager.monitorCount.collectAsState()
    var targetMonitor by remember { mutableStateOf(AbsolutePointingConstants.DEFAULT_TARGET_MONITOR.toInt()) }

    // 모니터 개수 통지 수신/변경 시 저장값 복원 또는 폴백(사용자 확정 규칙, styleframe-page3.md §2.2b)
    LaunchedEffect(monitorCount) {
        targetMonitor = resolveTargetMonitor(loadTargetMonitor(context), monitorCount)
    }

    // 화이트리스트 필터: 델타·스크롤·DPI·멀티커서 계열 액션을 Unassigned로 치환.
    // detection·overlay 양쪽에 동일한 filteredConfig를 사용해 시각과 동작을 일치시킨다.
    val filteredConfig = remember(edgeZoneAssignment) { filterConfigForAbsolutePad(edgeZoneAssignment.config) }

    // 엣지 상태(PointingArea ↔ EdgeZoneOverlay 공유를 위해 상위로 hoisting)
    val isEdgeCandidate = remember { mutableStateOf(false) }
    val entryEdge = remember { mutableStateOf<EntryEdge?>(null) }
    val fingerAlongEdgePx = remember { mutableStateOf(0f) }
    val inwardDistancePx = remember { mutableStateOf(0f) }
    val isZoneArmed = remember { mutableStateOf(false) }

    // ── 산봉우리(Bump) 시각화 상태 (Phase 4.9.3, TouchpadWrapper.kt 패턴 이식) ──
    // 드래그 중 마지막 유효 값(release 시 즉시 0으로 리셋되지 않음 → 수축 애니메이션 시작점)
    val lastBumpInwardPx = remember { mutableStateOf(0f) }
    val lastBumpAlongPx = remember { mutableStateOf(0f) }
    val lastBumpEntryAlongPx = remember { mutableStateOf(0f) }
    val lastBumpEntryEdge = remember { mutableStateOf<EntryEdge?>(null) }
    val bumpShrinkAnimatable = remember { Animatable(0f) }
    val isBumpShrinking = remember { mutableStateOf(false) }

    // 엣지 후보 종료(release/취소) 시 수축 spring 애니메이션 재생
    LaunchedEffect(isEdgeCandidate.value) {
        if (!isEdgeCandidate.value && lastBumpInwardPx.value > 0f) {
            isBumpShrinking.value = true
            bumpShrinkAnimatable.snapTo(lastBumpInwardPx.value)
            bumpShrinkAnimatable.animateTo(
                0f,
                spring(
                    dampingRatio = EdgeSwipeConstants.BUMP_SHRINK_SPRING_DAMPING,
                    stiffness = EdgeSwipeConstants.BUMP_SHRINK_SPRING_STIFFNESS
                )
            )
            isBumpShrinking.value = false
            lastBumpEntryEdge.value = null
            lastBumpInwardPx.value = 0f
            lastBumpAlongPx.value = 0f
            lastBumpEntryAlongPx.value = 0f
        } else if (!isEdgeCandidate.value) {
            lastBumpEntryEdge.value = null
            lastBumpInwardPx.value = 0f
            lastBumpAlongPx.value = 0f
            lastBumpEntryAlongPx.value = 0f
        }
    }

    // Page 2와 동일한 예외: 풀와이드 페이지에서는 ControlButtonContainer 폭을 Page 1
    // 터치패드 컬럼 폭 비율로 축소·중앙 정렬해야 버튼 크기가 다른 페이지와 동일해진다
    // (component-touchpad.md §1.3 Page 2 예외 참조).
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val controlButtonWidthFraction = if (screenWidthDp < 360) 0.60f else 0.64f

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(AbsolutePointingConstants.PAD_OUTER_MARGIN_DP.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val touchpadWidthPx = with(density) { maxWidth.toPx() }
            val touchpadHeightPx = with(density) { maxHeight.toPx() }

            PointingArea(
                clickMode = localState.clickMode,
                filteredConfig = filteredConfig,
                localState = localState,
                onLocalStateChange = { localState = it },
                isEdgeCandidate = isEdgeCandidate,
                entryEdge = entryEdge,
                fingerAlongEdgePx = fingerAlongEdgePx,
                inwardDistancePx = inwardDistancePx,
                isZoneArmed = isZoneArmed,
                lastBumpInwardPx = lastBumpInwardPx,
                lastBumpAlongPx = lastBumpAlongPx,
                lastBumpEntryAlongPx = lastBumpEntryAlongPx,
                lastBumpEntryEdge = lastBumpEntryEdge,
                onRestorePrevious = onRestorePrevious,
                onSendShortcut = onSendShortcut,
                onSendMacro = onSendMacro,
                onMouseHoldToggle = onMouseHoldToggle,
                onCyclePage = onCyclePage,
                onJumpToPage = onJumpToPage,
                targetMonitor = targetMonitor.toUByte(),
                modifier = Modifier.fillMaxSize()
            )

            // ── 산봉우리 시각화: 드래그 중은 raw 값, 릴리즈/취소 후에는 Animatable 수축 값 사용 ──
            val effectiveBumpEdge = when {
                isEdgeCandidate.value || lastBumpInwardPx.value > 0f || isBumpShrinking.value -> lastBumpEntryEdge.value
                else -> null
            }
            val effectiveBumpInward = if (isBumpShrinking.value) bumpShrinkAnimatable.value else lastBumpInwardPx.value
            if (effectiveBumpEdge != null && effectiveBumpInward > 0f) {
                val bumpColor = when {
                    localState.dragMode -> TouchpadColorGreen
                    localState.clickMode == ClickMode.LEFT_CLICK -> TouchpadColorPink
                    else -> TouchpadColorYellow
                }
                EdgeBumpOverlay(
                    entryEdge = effectiveBumpEdge,
                    fingerAlongEdgePx = lastBumpAlongPx.value,
                    entryAlongEdgePx = lastBumpEntryAlongPx.value,
                    inwardDistancePx = effectiveBumpInward,
                    maxPeakHeightPx = with(density) { EdgeSwipeConstants.MAX_PEAK_HEIGHT_DP.dp.toPx() },
                    baseHalfSizePx = with(density) { EdgeSwipeConstants.BUMP_BASE_HALF_SIZE_DP.dp.toPx() },
                    strokeWidthPx = with(density) { EdgeSwipeConstants.BUMP_STROKE_WIDTH_DP.dp.toPx() },
                    glowRadiusPx = with(density) { EdgeSwipeConstants.BUMP_GLOW_RADIUS_DP.dp.toPx() },
                    glowMaxRadiusPx = with(density) { EdgeSwipeConstants.BUMP_GLOW_MAX_RADIUS_DP.dp.toPx() },
                    borderColors = bumpColor to bumpColor,
                    modifier = Modifier.fillMaxSize()
                )
            }

            EdgeZoneOverlay(
                config = filteredConfig,
                isEdgeCandidate = isEdgeCandidate.value,
                entryEdge = entryEdge.value,
                fingerAlongEdgePx = fingerAlongEdgePx.value,
                inwardDistancePx = inwardDistancePx.value,
                touchpadWidthPx = touchpadWidthPx,
                touchpadHeightPx = touchpadHeightPx,
                isZoneArmed = isZoneArmed.value,
                rotationIndex = 0,
                hasBottomLeft = false,
                hasBottomRight = false,
                blockedRatio = EdgeSwipeConstants.CORNER_BUTTON_BLOCKED_RATIO,
                modifier = Modifier.fillMaxSize()
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth(controlButtonWidthFraction)
                .align(Alignment.TopCenter),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ControlButtonContainer(
                touchpadState = localState,
                onStateChange = { localState = it },
                config = ControlButtonConfig(
                    showClickMode = true,
                    showMoveMode = false,
                    showScrollMode = false,
                    showCursorMode = false,
                    showDpi = false,
                    showScrollSensitivity = false,
                    showZoom = true,
                    showDrag = true
                ),
                baseColor = TouchpadColorRed,
                modifier = Modifier.weight(1f)
            )

            // 모니터 셀렉터 (Phase 4.9.5): 모니터 2대 이상일 때만 노출, ControlButtonContainer 우측
            if (monitorCount >= 2) {
                MonitorSelector(
                    monitorCount = monitorCount,
                    selectedMonitor = targetMonitor,
                    onSelect = { selected ->
                        targetMonitor = selected
                        saveTargetMonitor(context, selected)
                    },
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

/**
 * 절대좌표 서버 중계 프레임을 UART로 전송합니다 (Phase 4.9.2).
 *
 * FrameBuilder.buildAbsolutePositionCommand()로 8바이트 프레임을 만들고
 * UsbSerialManager.sendCommandBytes()로 전송한다. 포트 미연결 시 IllegalStateException이
 * 발생할 수 있으므로 터치 제스처 루프가 죽지 않도록 예외를 흡수한다.
 * buttons 기본값은 0x00(클릭은 별도 버튼 프레임으로 처리) — 드래그 앤 드롭 모드(Phase 4.9.4)에서는
 * 호출측이 bit0(0x01)을 press 상태 동안 명시적으로 전달한다.
 * targetMonitor는 모니터 셀렉터(Phase 4.9.5)에서 선택한 값을 호출측이 전달한다.
 */
private fun sendAbsolutePosition(ratio: TouchRatio, buttons: UByte = 0x00u, targetMonitor: UByte) {
    try {
        val command = FrameBuilder.buildAbsolutePositionCommand(
            ratio = ratio,
            buttons = buttons,
            targetMonitor = targetMonitor
        )
        UsbSerialManager.sendCommandBytes(command)
    } catch (e: IllegalStateException) {
        Log.w("AbsolutePointingPad", "Failed to send absolute position: ${e.message}")
    }
}

@Composable
private fun PointingArea(
    clickMode: ClickMode,
    filteredConfig: EdgeZoneConfig,
    localState: TouchpadState,
    onLocalStateChange: (TouchpadState) -> Unit,
    isEdgeCandidate: MutableState<Boolean>,
    entryEdge: MutableState<EntryEdge?>,
    fingerAlongEdgePx: MutableState<Float>,
    inwardDistancePx: MutableState<Float>,
    isZoneArmed: MutableState<Boolean>,
    lastBumpInwardPx: MutableState<Float>,
    lastBumpAlongPx: MutableState<Float>,
    lastBumpEntryAlongPx: MutableState<Float>,
    lastBumpEntryEdge: MutableState<EntryEdge?>,
    onRestorePrevious: () -> Unit,
    onSendShortcut: (Int, List<Int>, Boolean) -> Unit,
    onSendMacro: (List<MacroStep>, Int) -> Unit,
    onMouseHoldToggle: (MouseButton, MouseHoldMode) -> Unit,
    onCyclePage: (PageNav) -> Unit,
    onJumpToPage: (Int) -> Unit,
    targetMonitor: UByte,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val view = LocalView.current
    // pointerInput은 clickMode/filteredConfig/localState.dragMode 변경 시에만 재시작되므로,
    // targetMonitor(모니터 셀렉터 선택값) 변경을 실행 중인 제스처 루프에서도 즉시 반영하려면
    // rememberUpdatedState로 최신값을 캡처해야 한다.
    val currentTargetMonitor by rememberUpdatedState(targetMonitor)

    var touchActive by remember { mutableStateOf(false) }
    var indicatorPosition by remember { mutableStateOf(Offset.Zero) }

    val indicatorAlpha by animateFloatAsState(
        targetValue = if (touchActive) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (touchActive) 0 else AbsolutePointingConstants.COORDINATE_INDICATOR_FADE_MS.toInt()
        ),
        label = "coordinateIndicatorAlpha"
    )

    val borderColor = when {
        localState.dragMode -> TouchpadColorGreen
        clickMode == ClickMode.LEFT_CLICK -> TouchpadColorPink
        else -> TouchpadColorYellow
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(AbsolutePointingConstants.POINTING_AREA_CORNER_RADIUS_DP.dp))
            .background(Color(0xFF1E1E1E))
            .border(
                width = AbsolutePointingConstants.POINTING_AREA_BORDER_WIDTH_DP.dp,
                color = borderColor,
                shape = RoundedCornerShape(AbsolutePointingConstants.POINTING_AREA_CORNER_RADIUS_DP.dp)
            )
            .pointerInput(clickMode, filteredConfig, localState.dragMode) {
                // 엣지 스와이프 상수 (Phase 4.9.3, TouchpadWrapper.kt와 동일 값)
                val edgeHitWidthPx = density.run { EdgeSwipeConstants.EDGE_HIT_WIDTH_DP.dp.toPx() }
                val triggerDistancePx = density.run { EdgeSwipeConstants.TRIGGER_DISTANCE_DP.dp.toPx() }
                val bumpAppearThresholdPx = density.run { EdgeSwipeConstants.DROPLET_APPEAR_THRESHOLD_DP.dp.toPx() }

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downTime = System.currentTimeMillis()
                    val downPosition = down.position
                    val areaWidth = size.width.toFloat()
                    val areaHeight = size.height.toFloat()

                    var lastRatio: TouchRatio = AbsoluteCoordinateCalculator.calculateTouchRatio(
                        downPosition.x, downPosition.y, areaWidth, areaHeight
                    )
                    // 드래그 앤 드롭 모드(Phase 4.9.4): 이번 제스처에서 실제 press를 시작했는지.
                    // 제스처 스코프 트랜지언트 — heldMouseButtons(영구 홀드)와 무관.
                    var dragPressed = false

                    // ── 존 단위 gate: 화이트리스트 통과 존이 있는 위치에서 시작했을 때만 엣지 후보 ──
                    val detectedEdge = detectEntryEdge(downPosition, areaWidth, areaHeight, edgeHitWidthPx, filteredConfig.cornerPriority)
                    val gateZone = if (detectedEdge != null && detectedEdge != EntryEdge.TOP) {
                        val edgeLen = if (detectedEdge == EntryEdge.LEFT || detectedEdge == EntryEdge.RIGHT) areaHeight else areaWidth
                        val rawRatio = if (edgeLen > 0f) getAlongEdgePosition(downPosition, detectedEdge) / edgeLen else 0f
                        unmapFromValid(detectedEdge, rawRatio, false, false, EdgeSwipeConstants.CORNER_BUTTON_BLOCKED_RATIO)
                            ?.let { EdgeZoneDetector.findActiveZone(filteredConfig, detectedEdge, it) }
                    } else null
                    val isAssignedZone = gateZone?.trigger?.hasAssignedAction() == true

                    var edgeStartInwardPx = 0f
                    var edgeStartAlongPx = 0f

                    if (isAssignedZone && detectedEdge != null) {
                        isEdgeCandidate.value = true
                        entryEdge.value = detectedEdge
                        edgeStartInwardPx = getInwardDistance(downPosition, detectedEdge, areaWidth, areaHeight)
                        edgeStartAlongPx = getAlongEdgePosition(downPosition, detectedEdge)
                        fingerAlongEdgePx.value = edgeStartAlongPx
                        inwardDistancePx.value = 0f
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        // 좌표 전송 억제 — 엣지 후보인 동안 커서가 튀지 않도록 DOWN 즉시 전송 생략
                    } else {
                        isEdgeCandidate.value = false
                        entryEdge.value = null
                        touchActive = true
                        indicatorPosition = downPosition
                        if (localState.dragMode) dragPressed = true
                        sendAbsolutePosition(lastRatio, if (dragPressed) 0x01u else 0x00u, currentTargetMonitor)
                    }

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break

                        if (change.changedToUpIgnoreConsumed()) {
                            change.consume()
                            val pressDuration = System.currentTimeMillis() - downTime
                            val movementPx = (change.position - downPosition).getDistance()
                            val movementDp = with(density) { movementPx.toDp().value }
                            val isTapGesture = pressDuration <= AbsolutePointingConstants.CLICK_MAX_DURATION_MS &&
                                movementDp <= AbsolutePointingConstants.CLICK_MAX_MOVEMENT_DP

                            if (dragPressed) {
                                // ── 드래그 앤 드롭 모드: release 프레임 1회 전송(drop) — 클릭 판정과 상호배타 ──
                                sendAbsolutePosition(lastRatio, 0x00u, currentTargetMonitor)
                                dragPressed = false
                            } else if (isZoneArmed.value) {
                                // ── armed 상태에서 손 뗌 → 엣지 액션 실행 ──
                                val edge = entryEdge.value
                                val activeZone = if (edge != null) {
                                    val edgeLen = if (edge == EntryEdge.LEFT || edge == EntryEdge.RIGHT) areaHeight else areaWidth
                                    val rawRatio = if (edgeLen > 0f) fingerAlongEdgePx.value / edgeLen else 0f
                                    unmapFromValid(edge, rawRatio, false, false, EdgeSwipeConstants.CORNER_BUTTON_BLOCKED_RATIO)
                                        ?.let { EdgeZoneDetector.findActiveZone(filteredConfig, edge, it) }
                                } else null
                                val actionToApply = activeZone?.trigger?.resolveAction()
                                if (actionToApply != null) {
                                    when (actionToApply) {
                                        EdgeZoneAction.RestorePreviousMode -> onRestorePrevious()
                                        is EdgeZoneAction.SendShortcut ->
                                            onSendShortcut(actionToApply.modifierBits, actionToApply.keyCodes, actionToApply.hold)
                                        is EdgeZoneAction.SendMacro ->
                                            onSendMacro(actionToApply.steps, actionToApply.stepDelayMs)
                                        is EdgeZoneAction.MouseHoldToggle ->
                                            onMouseHoldToggle(actionToApply.button, actionToApply.mode)
                                        is EdgeZoneAction.CyclePage -> onCyclePage(actionToApply.direction)
                                        is EdgeZoneAction.JumpToPage -> onJumpToPage(actionToApply.pageIndex)
                                        else -> {
                                            val newState = EdgeZoneActionHandler.applyZoneAction(localState, actionToApply, 0)
                                            onLocalStateChange(newState)
                                        }
                                    }
                                }
                            } else if (isTapGesture) {
                                // ── 엣지 띠 탭이든 일반 탭이든 → 클릭. 엣지 후보였다면 DOWN 지점 좌표를 먼저 전송 ──
                                if (isEdgeCandidate.value) {
                                    sendAbsolutePosition(lastRatio, targetMonitor = currentTargetMonitor)
                                }
                                val buttons: UByte = if (clickMode == ClickMode.LEFT_CLICK) 0x01u.toUByte() else 0x02u.toUByte()
                                ClickDetector.sendFrame(ClickDetector.createMouseButtonFrame(buttons))
                                coroutineScope.launch {
                                    delay(ClickDetector.CLICK_PRESS_RELEASE_GAP_MS)
                                    ClickDetector.sendFrame(ClickDetector.createMouseButtonFrame(0x00u.toUByte()))
                                }
                            }

                            isEdgeCandidate.value = false
                            entryEdge.value = null
                            isZoneArmed.value = false
                            fingerAlongEdgePx.value = 0f
                            inwardDistancePx.value = 0f
                            break
                        }

                        if (change.positionChanged()) {
                            val pos = change.position
                            if (isEdgeCandidate.value) {
                                val edge = entryEdge.value
                                val curInward = getInwardDistance(pos, edge, areaWidth, areaHeight)
                                val inwardMoved = curInward - edgeStartInwardPx
                                val curAlong = getAlongEdgePosition(pos, edge)
                                val perpMoved = abs(curAlong - edgeStartAlongPx)
                                fingerAlongEdgePx.value = curAlong
                                inwardDistancePx.value = inwardMoved.coerceAtLeast(0f)

                                // 산봉우리 시각화 갱신 (진입 엣지 고정 — 손가락이 다른 엣지에 가까워져도 점프하지 않음)
                                lastBumpEntryEdge.value = edge
                                lastBumpInwardPx.value = curInward.coerceAtLeast(0f)
                                lastBumpAlongPx.value = curAlong
                                lastBumpEntryAlongPx.value = edgeStartAlongPx

                                when {
                                    inwardMoved >= triggerDistancePx -> {
                                        if (!isZoneArmed.value) {
                                            isZoneArmed.value = true
                                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                        }
                                    }
                                    isZoneArmed.value && inwardMoved < triggerDistancePx -> {
                                        // 임계값 아래로 후퇴 → disarm (재진입 시 재발동 가능)
                                        isZoneArmed.value = false
                                    }
                                    inwardMoved < 0f -> {
                                        // 시작점보다 엣지 방향으로 되돌아감 → 후보 취소, 일반 포인팅 재개
                                        isZoneArmed.value = false
                                        isEdgeCandidate.value = false
                                        entryEdge.value = null
                                        touchActive = true
                                        indicatorPosition = pos
                                        lastRatio = AbsoluteCoordinateCalculator.calculateTouchRatio(pos.x, pos.y, areaWidth, areaHeight)
                                        if (localState.dragMode) dragPressed = true
                                        sendAbsolutePosition(lastRatio, if (dragPressed) 0x01u else 0x00u, currentTargetMonitor)
                                    }
                                    perpMoved >= triggerDistancePx && inwardDistancePx.value < bumpAppearThresholdPx -> {
                                        // 산봉우리 등장 전에 엣지 방향으로 충분히 이동 → 일반 포인팅으로 전환
                                        isEdgeCandidate.value = false
                                        entryEdge.value = null
                                        touchActive = true
                                        indicatorPosition = pos
                                        lastRatio = AbsoluteCoordinateCalculator.calculateTouchRatio(pos.x, pos.y, areaWidth, areaHeight)
                                        if (localState.dragMode) dragPressed = true
                                        sendAbsolutePosition(lastRatio, if (dragPressed) 0x01u else 0x00u, currentTargetMonitor)
                                    }
                                }
                            } else {
                                indicatorPosition = pos
                                val ratio = AbsoluteCoordinateCalculator.calculateTouchRatio(pos.x, pos.y, areaWidth, areaHeight)
                                if (AbsoluteCoordinateCalculator.shouldTransmit(ratio, lastRatio)) {
                                    lastRatio = ratio
                                    sendAbsolutePosition(ratio, if (dragPressed) 0x01u else 0x00u, currentTargetMonitor)
                                }
                            }
                            change.consume()
                        }
                    }

                    touchActive = false
                }
            }
    ) {
        if (indicatorAlpha > 0f) {
            CoordinateIndicator(position = indicatorPosition, alpha = indicatorAlpha)
        }
    }
}

@Composable
private fun CoordinateIndicator(position: Offset, alpha: Float) {
    val density = LocalDensity.current
    val crosshairSizePx = with(density) { AbsolutePointingConstants.COORDINATE_INDICATOR_CROSSHAIR_SIZE_DP.dp.toPx() }
    val dotSizePx = with(density) { AbsolutePointingConstants.COORDINATE_INDICATOR_DOT_SIZE_DP.dp.toPx() }
    val strokeWidthPx = with(density) { AbsolutePointingConstants.COORDINATE_INDICATOR_STROKE_WIDTH_DP.dp.toPx() }
    val color = Color.White.copy(alpha = 0.6f * alpha)

    Canvas(modifier = Modifier.fillMaxSize()) {
        val half = crosshairSizePx / 2
        drawLine(
            color = color,
            start = Offset(position.x - half, position.y),
            end = Offset(position.x + half, position.y),
            strokeWidth = strokeWidthPx
        )
        drawLine(
            color = color,
            start = Offset(position.x, position.y - half),
            end = Offset(position.x, position.y + half),
            strokeWidth = strokeWidthPx
        )
        drawCircle(
            color = color,
            radius = dotSizePx / 2,
            center = position
        )
    }
}
