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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.bridgeone.app.ui.components.touchpad.TouchpadColorZoom
import com.bridgeone.app.ui.components.touchpad.TouchpadState
import com.bridgeone.app.ui.components.touchpad.applyRoi
import com.bridgeone.app.ui.components.touchpad.detectEntryEdge
import com.bridgeone.app.ui.components.touchpad.filterConfigForAbsolutePad
import com.bridgeone.app.ui.components.touchpad.getAlongEdgePosition
import com.bridgeone.app.ui.components.touchpad.getInwardDistance
import com.bridgeone.app.ui.components.touchpad.unmapFromValid
import com.bridgeone.app.protocol.FrameBuilder
import com.bridgeone.app.protocol.ZoomStateCommand
import com.bridgeone.app.usb.UsbSerialManager
import com.bridgeone.app.ui.utils.AbsoluteCoordinateCalculator
import com.bridgeone.app.ui.utils.AbsolutePointingConstants
import com.bridgeone.app.ui.utils.AbsoluteZoomState
import com.bridgeone.app.ui.utils.ClickDetector
import com.bridgeone.app.ui.utils.MagnificationMode
import com.bridgeone.app.ui.utils.TouchRatio
import com.bridgeone.app.ui.utils.ZoneMapping
import com.bridgeone.app.ui.utils.ZoneRect
import com.bridgeone.app.ui.utils.resolveTargetMonitor
import com.bridgeone.app.ui.utils.getDistance
import kotlin.math.abs
import kotlinx.coroutines.Job
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

/** [EdgeZoneTrigger]에서 실행할 액션을 결정. 로테이션은 armed 동안 회전 코루틴이 갱신한 [rotationIndex] 후보(Phase 4.9.8). */
private fun EdgeZoneTrigger.resolveAction(rotationIndex: Int): EdgeZoneAction? = when (this) {
    is EdgeZoneTrigger.SingleAction -> action.takeIf { it !is EdgeZoneAction.Unassigned }
    is EdgeZoneTrigger.Rotation -> candidates.getOrNull(rotationIndex)?.action?.takeIf { it !is EdgeZoneAction.Unassigned }
}

/**
 * 절대좌표 패드 페이지. PointingArea(자유 비율, stretch 매핑) + 상단 ControlButtonContainer
 * (ClickModeButton/DragModeButton 활성, ZoomButton은 Disabled 슬롯)로 구성된다.
 *
 * @param edgeZoneAssignment Page 3 전용 엣지존 할당(Phase 4.9.3). 화이트리스트 필터를 거쳐 사용.
 * @param onEdgeZoneAssignmentChange 존 할당 변경 콜백(편집 UI는 4.9.9, 현재는 시그니처만 배선).
 * @param onRestorePrevious/onSendShortcut/onSendMacro/onMouseHoldToggle/onCyclePage/onJumpToPage
 *        엣지존 부수효과형 액션 콜백. Page 1이 쓰는 StandardModePage의 기존 콜백을 그대로 재사용.
 * @param magnificationMode 확대 매핑 모드(단일 줌/멀티 존은 상호 배타, Phase 4.9.10). 페이지 전환에도
 *        유지되어야 하므로 호출측(StandardModePage, 페이저 바깥)에서 hoisting해 전달한다.
 * @param onMagnificationModeChange 확대 매핑 모드 변경 콜백.
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
    onJumpToPage: (Int) -> Unit = {},
    magnificationMode: MagnificationMode = MagnificationMode.Off,
    onMagnificationModeChange: (MagnificationMode) -> Unit = {}
) {
    // 단일 줌 좌표 계산/UI에 쓰는 파생값(멀티 존은 4.9.11에서 배선, 이 Phase는 Single만 다룬다).
    val currentMapping = (magnificationMode as? MagnificationMode.Single)?.mapping ?: ZoneMapping()
    // 클릭 모드는 Page 1/2의 pageState.touchpadState와 공유하지 않는 페이지 로컬 상태.
    // ControlButtonContainer가 요구하는 TouchpadState 타입을 재사용하되 clickMode 외 필드는 미사용.
    var localState by remember { mutableStateOf(TouchpadState()) }

    // ── 줌 arming 상태 (Phase 4.9.6) ──
    // ZoomButton 탭으로 진입하는 "줌 모드 대기" 상태. 제스처 스코프 트랜지언트라 페이지 전환 시
    // 리셋되어도 무방(magnificationMode의 매핑만 hoisted로 유지하면 됨).
    var zoomArming by remember { mutableStateOf(false) }

    // 줌 정의 드래그가 끝나고(손을 뗀 뒤) 별도의 탭으로 확정하기를 기다리는 상태(Phase 4.9.6,
    // 유저 확정: 손 떼는 즉시 확정 대신 확정용 탭을 한 번 더 요구). zoomArming이 true인 동안만 의미 있다.
    var zoomAwaitingConfirm by remember { mutableStateOf(false) }

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
    // 로테이션 존 회전 코루틴이 갱신하는 현재 후보 인덱스 (Phase 4.9.8, TouchpadWrapper.kt 패턴 이식).
    // EdgeZoneOverlay(하이라이트 표시)와 PointingArea(release 시 실행할 후보 결정) 양쪽이 읽어야 해 상위로 hoisting.
    val rotationIndex = remember { mutableStateOf(0) }

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
                rotationIndex = rotationIndex,
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
                magnificationMode = magnificationMode,
                zoomArming = zoomArming,
                zoomAwaitingConfirm = zoomAwaitingConfirm,
                onMagnificationModeChange = onMagnificationModeChange,
                onZoomArmingChange = { zoomArming = it },
                onZoomAwaitingConfirmChange = { zoomAwaitingConfirm = it },
                modifier = Modifier.fillMaxSize()
            )

            // ── 산봉우리 시각화: 드래그 중은 raw 값, 릴리즈/취소 후에는 Animatable 수축 값 사용 ──
            val effectiveBumpEdge = when {
                isEdgeCandidate.value || lastBumpInwardPx.value > 0f || isBumpShrinking.value -> lastBumpEntryEdge.value
                else -> null
            }
            val effectiveBumpInward = if (isBumpShrinking.value) bumpShrinkAnimatable.value else lastBumpInwardPx.value
            if (effectiveBumpEdge != null && effectiveBumpInward > 0f) {
                // 색상 우선순위 (설계 component-design-guide-app.md §4.5.7):
                // 드래그 ON(초록) > 우클릭(노랑) > 줌 활성(주황, Phase 4.9.6) > 기본 좌클릭(핑크)
                val bumpColor = when {
                    localState.dragMode -> TouchpadColorGreen
                    localState.clickMode == ClickMode.RIGHT_CLICK -> TouchpadColorYellow
                    currentMapping.defined || zoomArming -> TouchpadColorZoom
                    else -> TouchpadColorPink
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
                rotationIndex = rotationIndex.value,
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
                zoomLevel = AbsoluteCoordinateCalculator.zoomLevelFromPcRect(currentMapping.pcRect),
                zoomArming = zoomArming,
                onZoomClick = {
                    toggleAbsoluteZoom(
                        isActive = currentMapping.defined,
                        isArming = zoomArming,
                        targetMonitor = targetMonitor,
                        onMagnificationModeChange = onMagnificationModeChange,
                        onZoomArmingChange = { zoomArming = it },
                        onZoomAwaitingConfirmChange = { zoomAwaitingConfirm = it },
                    )
                },
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

/**
 * 줌 상태 Vendor CDC 커스텀 명령을 UART로 전송합니다 (Phase 4.9.7).
 *
 * ZoomStateCommand.buildFrame()으로 [0xFF][0x30][len][JSON payload][CRC16] 프레임을 만들고
 * UsbSerialManager.sendVendorCdcFrame()으로 전송한다. 포트 미연결 시 IllegalStateException이
 * 발생할 수 있으므로 sendAbsolutePosition()과 동일하게 예외를 흡수한다.
 * 전송 시점(확정 1회/드래그 중 30Hz 스로틀/해제 1회)은 호출측이 결정한다.
 */
private fun sendZoomStateFrame(zoom: AbsoluteZoomState, targetMonitor: Int) {
    try {
        UsbSerialManager.sendVendorCdcFrame(ZoomStateCommand.buildFrame(zoom, targetMonitor))
    } catch (e: IllegalStateException) {
        Log.w("AbsolutePointingPad", "Failed to send zoom state: ${e.message}")
    }
}

/**
 * ZoomButton 탭 토글 로직. 엣지존 `ToggleAbsoluteZoom` 액션(Phase 4.9.9)이 동일 동작을 공유하도록
 * 콜백 기반으로 추출한 헬퍼 — 줌 활성/arming 중 재호출 시 즉시 1x 해제, 아니면 arming 진입.
 */
private fun toggleAbsoluteZoom(
    isActive: Boolean,
    isArming: Boolean,
    targetMonitor: Int,
    onMagnificationModeChange: (MagnificationMode) -> Unit,
    onZoomArmingChange: (Boolean) -> Unit,
    onZoomAwaitingConfirmChange: (Boolean) -> Unit,
) {
    if (isActive || isArming) {
        // 줌 활성 중 재탭 → 즉시 1x 해제. arming/확정 대기 중(패드 탭 확정 전) 재탭 → 전부 취소.
        onMagnificationModeChange(MagnificationMode.Off)
        onZoomArmingChange(false)
        onZoomAwaitingConfirmChange(false)
        // (C) 줌 해제 1회 전송 (Phase 4.9.7)
        sendZoomStateFrame(AbsoluteZoomState(), targetMonitor)
    } else {
        onZoomArmingChange(true)
        onZoomAwaitingConfirmChange(false)
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
    rotationIndex: MutableState<Int>,
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
    magnificationMode: MagnificationMode,
    zoomArming: Boolean,
    zoomAwaitingConfirm: Boolean,
    onMagnificationModeChange: (MagnificationMode) -> Unit,
    onZoomArmingChange: (Boolean) -> Unit,
    onZoomAwaitingConfirmChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val view = LocalView.current
    // 단일 줌 좌표 계산/UI에 쓰는 파생값(멀티 존은 4.9.11에서 배선, 이 Phase는 Single만 다룬다).
    val mapping = (magnificationMode as? MagnificationMode.Single)?.mapping ?: ZoneMapping()
    // pointerInput은 clickMode/filteredConfig/localState.dragMode 변경 시에만 재시작되므로,
    // targetMonitor(모니터 셀렉터 선택값)·확대 매핑 모드 변경을 실행 중인 제스처 루프에서도 즉시
    // 반영하려면 rememberUpdatedState로 최신값을 캡처해야 한다.
    val currentTargetMonitor by rememberUpdatedState(targetMonitor)
    val currentMapping by rememberUpdatedState(mapping)
    val currentZoomArming by rememberUpdatedState(zoomArming)
    val currentZoomAwaitingConfirm by rememberUpdatedState(zoomAwaitingConfirm)

    var touchActive by remember { mutableStateOf(false) }
    var indicatorPosition by remember { mutableStateOf(Offset.Zero) }

    val indicatorAlpha by animateFloatAsState(
        targetValue = if (touchActive) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (touchActive) 0 else AbsolutePointingConstants.COORDINATE_INDICATOR_FADE_MS.toInt()
        ),
        label = "coordinateIndicatorAlpha"
    )

    // 색상 우선순위 (설계 component-design-guide-app.md §4.5.7):
    // 드래그 ON(초록) > 우클릭(노랑) > 줌 활성/대기(주황, Phase 4.9.6) > 기본 좌클릭(핑크)
    // zoomArming(ZoomButton 탭 후 패드 터치 대기 상태)도 주황으로 표시해야 버튼을 눌렀을 때
    // 즉시 시각 피드백이 생긴다(그렇지 않으면 "눌러도 아무 변화 없음"으로 보임).
    val borderColor = when {
        localState.dragMode -> TouchpadColorGreen
        clickMode == ClickMode.RIGHT_CLICK -> TouchpadColorYellow
        mapping.defined || zoomArming -> TouchpadColorZoom
        else -> TouchpadColorPink
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

                    // 로테이션 존 회전 상태(Phase 4.9.8, TouchpadWrapper.kt:901-930 패턴 이식).
                    // 제스처 스코프 트랜지언트 — armed 동안만 의미 있고 release/cancel 시 정리된다.
                    var rotationJob: Job? = null
                    var armedZoneKey: Pair<Float, Float>? = null

                    // 줌 정의 모드(Phase 4.9.6): ZoomButton arming 상태에서 시작한 이번 제스처가
                    // 줌 중심점 드래그로 인식됐는지. 제스처 스코프 트랜지언트.
                    var zoomDefining = false
                    var zoomCenterRatio = lastRatio

                    // 줌 상태 Vendor CDC 전송용(Phase 4.9.7). pendingZoomState는 드래그 중 마지막으로
                    // 계산된 상태를 보관해 UP 확정 시 재계산 없이 그대로 전송한다. lastZoomTxMs는
                    // 드래그 중 30Hz 스로틀 기준 시각(제스처 스코프 — 단일 정의 드래그는 한 제스처 내 완결).
                    // 프로토콜 프레임(0xFF/0x30)은 여전히 level+center 스칼라 기반이라(Phase 4.9.10 범위
                    // 밖) AbsoluteZoomState로 계산·전송하고, hoisted 외부 상태로 내보낼 때만 ZoneRect로 변환한다.
                    var lastZoomTxMs = 0L
                    var pendingZoomState = AbsoluteCoordinateCalculator.zoomStateFromZoneMapping(currentMapping)

                    // 줌 확정 대기 중(zoomAwaitingConfirm) 발생한 이번 제스처가 "탭(확정)"인지
                    // "드래그(재정의)"인지 아직 판가름나지 않은 상태. 이동 거리가 CLICK_MAX_MOVEMENT_DP를
                    // 넘는 순간 zoomDefining으로 전환(재정의)되고, 넘지 않은 채 손을 떼면 탭으로 판정해
                    // 확정한다. 제스처 스코프 트랜지언트.
                    var zoomAdjusting = false
                    var zoomAdjustCenterCandidate = lastRatio

                    // 직사각형 ROI 매핑을 적용해 좌표를 전송하는 헬퍼(Phase 4.9.10). 미정의 존은
                    // pcRect=ZoneRect.FULL(항등)이라 applyRoi가 자동으로 원본 ratio를 그대로 반환한다.
                    fun sendZoomed(ratio: TouchRatio, buttons: UByte = 0x00u) {
                        val zoomed = applyRoi(ratio, currentMapping.pcRect)
                        sendAbsolutePosition(zoomed, buttons, currentTargetMonitor)
                    }

                    // 이번 제스처의 DOWN 위치 대비 드래그 거리(dp) → 줌 레벨로 변환해 실시간 반영.
                    // 최초 정의(zoomDefining)와 확정 대기 중 재정의(zoomAdjusting→zoomDefining 전환) 양쪽에서 재사용.
                    // 정의 제스처 자체(배율 스칼라 계산)는 이 Phase에서 바꾸지 않는다(4.9.12 예정) —
                    // 계산 결과만 ZoneRect로 변환해 hoisted MagnificationMode로 내보낸다.
                    fun updateZoomLevelFromDrag(pos: Offset) {
                        val dragDistancePx = (pos - downPosition).getDistance()
                        val dragDistanceDp = with(density) { dragDistancePx.toDp().value }
                        val newLevel = AbsoluteCoordinateCalculator.dragDistanceToZoomLevel(dragDistanceDp)
                        val newState = AbsoluteZoomState(
                            level = newLevel,
                            centerX = zoomCenterRatio.x,
                            centerY = zoomCenterRatio.y
                        )
                        pendingZoomState = newState
                        onMagnificationModeChange(
                            MagnificationMode.Single(
                                ZoneMapping(
                                    pcRect = AbsoluteCoordinateCalculator.zoneRectFromZoomState(newState),
                                    defined = newState.isActive
                                )
                            )
                        )
                        // (B) 줌 드래그 중 30Hz 스로틀 실시간 전송 (Phase 4.9.7)
                        val now = System.currentTimeMillis()
                        if (now - lastZoomTxMs >= AbsolutePointingConstants.ZOOM_STATE_THROTTLE_MS) {
                            lastZoomTxMs = now
                            sendZoomStateFrame(newState, currentTargetMonitor.toInt())
                        }
                    }

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
                        if (currentZoomArming && currentZoomAwaitingConfirm) {
                            // ── 확정 대기 중 추가 터치: 탭(확정)인지 드래그(재정의)인지 MOVE에서 판가름.
                            // 판가름 전까지는 기존 확정 후보(currentMapping)를 건드리지 않는다.
                            zoomAdjusting = true
                            zoomAdjustCenterCandidate = lastRatio
                        } else if (currentZoomArming) {
                            // ── 줌 정의 모드 시작: DOWN 위치를 중심점으로, 좌표 전송은 억제 ──
                            zoomDefining = true
                            zoomCenterRatio = lastRatio
                            onMagnificationModeChange(
                                MagnificationMode.Single(
                                    ZoneMapping(
                                        pcRect = ZoneRect.FULL,
                                        defined = false
                                    )
                                )
                            )
                        } else {
                            touchActive = true
                            indicatorPosition = downPosition
                            if (localState.dragMode) dragPressed = true
                            sendZoomed(lastRatio, if (dragPressed) 0x01u else 0x00u)
                        }
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

                            if (zoomDefining) {
                                // ── 줌 정의 드래그 종료: 레벨/중심점은 이 시점 값으로 고정, 확정은
                                // 아직 안 됨 — arming 유지한 채 "확정 대기"로 전환(유저 확정: 손 떼는
                                // 즉시 확정 대신 별도 탭을 한 번 더 요구) ──
                                zoomDefining = false
                                onZoomAwaitingConfirmChange(true)
                                // (A) 줌 확정(정의 드래그 종료) 1회 전송 — 스로틀 무시 (Phase 4.9.7)
                                sendZoomStateFrame(pendingZoomState, currentTargetMonitor.toInt())
                            } else if (zoomAdjusting) {
                                // ── 확정 대기 중 재터치가 끝까지 드래그로 전환되지 않음 → 탭 판정.
                                // 탭이면 확정(arming 해제), 아니면(짧은 이동 없는 롱프레스 등) 무시하고 대기 유지 ──
                                if (isTapGesture) {
                                    onZoomArmingChange(false)
                                    onZoomAwaitingConfirmChange(false)
                                }
                                zoomAdjusting = false
                            } else if (dragPressed) {
                                // ── 드래그 앤 드롭 모드: release 프레임 1회 전송(drop) — 클릭 판정과 상호배타 ──
                                sendZoomed(lastRatio, 0x00u)
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
                                val actionToApply = activeZone?.trigger?.resolveAction(rotationIndex.value)
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
                                        EdgeZoneAction.ToggleAbsoluteZoom -> toggleAbsoluteZoom(
                                            isActive = currentMapping.defined,
                                            isArming = currentZoomArming,
                                            targetMonitor = currentTargetMonitor.toInt(),
                                            onMagnificationModeChange = onMagnificationModeChange,
                                            onZoomArmingChange = onZoomArmingChange,
                                            onZoomAwaitingConfirmChange = onZoomAwaitingConfirmChange,
                                        )
                                        else -> {
                                            val newState = EdgeZoneActionHandler.applyZoneAction(localState, actionToApply, 0)
                                            onLocalStateChange(newState)
                                        }
                                    }
                                }
                                rotationJob?.cancel()
                                rotationJob = null
                                rotationIndex.value = 0
                                armedZoneKey = null
                            } else if (isTapGesture) {
                                // ── 엣지 띠 탭이든 일반 탭이든 → 클릭. 엣지 후보였다면 DOWN 지점 좌표를 먼저 전송 ──
                                if (isEdgeCandidate.value) {
                                    sendZoomed(lastRatio)
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
                            if (zoomDefining) {
                                // ── 줌 정의 모드: 중심점 대비 드래그 거리(dp) → 줌 레벨 실시간 갱신 ──
                                updateZoomLevelFromDrag(pos)
                            } else if (zoomAdjusting) {
                                // ── 확정 대기 중 재터치: 탭/드래그 아직 미판정. 이동 거리가 클릭 임계값을
                                // 넘으면 재정의 드래그로 전환(기존 확정 후보를 이 시점부터 덮어씀) ──
                                val movedPx = (pos - downPosition).getDistance()
                                val movedDp = with(density) { movedPx.toDp().value }
                                if (movedDp > AbsolutePointingConstants.CLICK_MAX_MOVEMENT_DP) {
                                    zoomDefining = true
                                    zoomAdjusting = false
                                    zoomCenterRatio = zoomAdjustCenterCandidate
                                    updateZoomLevelFromDrag(pos)
                                }
                            } else if (isEdgeCandidate.value) {
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
                                        // 로테이션 존이면 armed 유지 중에도 손가락이 다른 존으로 옮겨갈 때마다
                                        // 회전 코루틴을 재시작해야 하므로 현재 존을 매 MOVE마다 판정한다.
                                        val zoneEdgeLen = if (edge == EntryEdge.LEFT || edge == EntryEdge.RIGHT) areaHeight else areaWidth
                                        val zoneAlongRatio = if (zoneEdgeLen > 0f && edge != null)
                                            unmapFromValid(edge, curAlong / zoneEdgeLen, false, false, EdgeSwipeConstants.CORNER_BUTTON_BLOCKED_RATIO)
                                        else null
                                        val curZone = if (zoneAlongRatio != null && edge != null)
                                            EdgeZoneDetector.findActiveZone(filteredConfig, edge, zoneAlongRatio)
                                        else null
                                        val curZoneKey = curZone?.let { it.startRatio to it.endRatio }

                                        fun restartRotationIfNeeded() {
                                            val rotTrigger = curZone?.trigger as? EdgeZoneTrigger.Rotation
                                            if (rotTrigger != null && rotTrigger.candidates.size >= EdgeSwipeConstants.EDGE_ZONE_ROTATION_MIN_CANDIDATES) {
                                                rotationJob = coroutineScope.launch {
                                                    while (true) {
                                                        delay(rotTrigger.intervalMs.toLong())
                                                        rotationIndex.value = (rotationIndex.value + 1) % rotTrigger.candidates.size
                                                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                                    }
                                                }
                                            }
                                        }

                                        if (!isZoneArmed.value) {
                                            isZoneArmed.value = true
                                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                            rotationIndex.value = 0
                                            rotationJob?.cancel()
                                            restartRotationIfNeeded()
                                            armedZoneKey = curZoneKey
                                        } else if (curZoneKey != armedZoneKey) {
                                            // 이미 armed 상태에서 다른 존으로 이동 → 회전 재시작
                                            armedZoneKey = curZoneKey
                                            rotationJob?.cancel()
                                            rotationJob = null
                                            rotationIndex.value = 0
                                            restartRotationIfNeeded()
                                        }
                                    }
                                    isZoneArmed.value && inwardMoved < triggerDistancePx -> {
                                        // 임계값 아래로 후퇴 → disarm (재진입 시 재발동 가능)
                                        isZoneArmed.value = false
                                        rotationJob?.cancel()
                                        rotationJob = null
                                        rotationIndex.value = 0
                                        armedZoneKey = null
                                    }
                                    inwardMoved < 0f -> {
                                        // 시작점보다 엣지 방향으로 되돌아감 → 후보 취소, 일반 포인팅 재개
                                        isZoneArmed.value = false
                                        isEdgeCandidate.value = false
                                        rotationJob?.cancel()
                                        rotationJob = null
                                        rotationIndex.value = 0
                                        armedZoneKey = null
                                        entryEdge.value = null
                                        touchActive = true
                                        indicatorPosition = pos
                                        lastRatio = AbsoluteCoordinateCalculator.calculateTouchRatio(pos.x, pos.y, areaWidth, areaHeight)
                                        if (localState.dragMode) dragPressed = true
                                        sendZoomed(lastRatio, if (dragPressed) 0x01u else 0x00u)
                                    }
                                    perpMoved >= triggerDistancePx && inwardDistancePx.value < bumpAppearThresholdPx -> {
                                        // 산봉우리 등장 전에 엣지 방향으로 충분히 이동 → 일반 포인팅으로 전환
                                        isEdgeCandidate.value = false
                                        entryEdge.value = null
                                        touchActive = true
                                        indicatorPosition = pos
                                        lastRatio = AbsoluteCoordinateCalculator.calculateTouchRatio(pos.x, pos.y, areaWidth, areaHeight)
                                        if (localState.dragMode) dragPressed = true
                                        sendZoomed(lastRatio, if (dragPressed) 0x01u else 0x00u)
                                    }
                                }
                            } else {
                                indicatorPosition = pos
                                val ratio = AbsoluteCoordinateCalculator.calculateTouchRatio(pos.x, pos.y, areaWidth, areaHeight)
                                if (AbsoluteCoordinateCalculator.shouldTransmit(ratio, lastRatio)) {
                                    lastRatio = ratio
                                    sendZoomed(ratio, if (dragPressed) 0x01u else 0x00u)
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
        // 줌 레벨 텍스트 (Phase 4.9.6): 정의/확정 대기 중(zoomArming)은 화면 정가운데 크게(유저 확정,
        // 원탭 확정 흐름에서 진행 상태를 명확히 보여주기 위함), 확정된 활성 줌(mapping.defined만,
        // arming 아님)은 설계 §4.5.4대로 우상단에 작게 표시. 1x(둘 다 아님)에서는 미표시.
        // 배율 수치는 pcRect 폭에서 역산(Phase 4.9.10, zoomLevelFromPcRect).
        if (zoomArming) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.align(Alignment.Center)
            ) {
                Text(
                    text = "${"%.1f".format(AbsoluteCoordinateCalculator.zoomLevelFromPcRect(mapping.pcRect))}x",
                    color = TouchpadColorZoom,
                    fontSize = AbsolutePointingConstants.ZOOM_LEVEL_CENTER_TEXT_SIZE_SP.sp,
                    fontWeight = FontWeight.Bold
                )
                if (zoomAwaitingConfirm) {
                    Text(
                        text = "탭하여 확정",
                        color = TouchpadColorZoom,
                        fontSize = AbsolutePointingConstants.ZOOM_CONFIRM_HINT_TEXT_SIZE_SP.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else if (mapping.defined) {
            Text(
                text = "${"%.1f".format(AbsoluteCoordinateCalculator.zoomLevelFromPcRect(mapping.pcRect))}x",
                color = TouchpadColorZoom,
                fontSize = AbsolutePointingConstants.ZOOM_LEVEL_TEXT_SIZE_SP.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(AbsolutePointingConstants.ZOOM_LEVEL_TEXT_PADDING_DP.dp)
            )
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
