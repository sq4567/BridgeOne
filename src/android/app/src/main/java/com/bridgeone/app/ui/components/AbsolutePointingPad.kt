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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bridgeone.app.ui.common.EdgeSwipeConstants
import com.bridgeone.app.ui.common.ToastController
import com.bridgeone.app.ui.common.ToastType
import com.bridgeone.app.ui.common.TouchpadEdgeZoneAssignment
import com.bridgeone.app.ui.common.ZoneCrossBehavior
import com.bridgeone.app.ui.common.loadTargetMonitor
import com.bridgeone.app.ui.common.saveTargetMonitor
import com.bridgeone.app.ui.components.touchpad.ClickMode
import com.bridgeone.app.ui.components.touchpad.ControlButtonConfig
import com.bridgeone.app.ui.components.touchpad.ControlButtonContainer
import com.bridgeone.app.ui.components.touchpad.CursorCountSelectionPopup
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
import com.bridgeone.app.ui.components.touchpad.divideZoneAreas
import com.bridgeone.app.ui.components.touchpad.filterConfigForAbsolutePad
import com.bridgeone.app.ui.components.touchpad.getAlongEdgePosition
import com.bridgeone.app.ui.components.touchpad.getInwardDistance
import com.bridgeone.app.ui.components.touchpad.hitTestPad
import com.bridgeone.app.ui.components.touchpad.rectFromCenterDrag
import com.bridgeone.app.ui.components.touchpad.rectsOverlap
import com.bridgeone.app.ui.components.touchpad.resolveZoneRatio
import com.bridgeone.app.ui.components.touchpad.unmapFromValid
import com.bridgeone.app.protocol.FrameBuilder
import com.bridgeone.app.protocol.ZoomStateCommand
import com.bridgeone.app.usb.UsbSerialManager
import com.bridgeone.app.ui.utils.AbsoluteCoordinateCalculator
import com.bridgeone.app.ui.utils.AbsolutePointingConstants
import com.bridgeone.app.ui.utils.AbsoluteZoomState
import com.bridgeone.app.ui.utils.ClickDetector
import com.bridgeone.app.ui.utils.MagnificationMode
import com.bridgeone.app.ui.utils.MultiZoneState
import com.bridgeone.app.ui.utils.TouchRatio
import com.bridgeone.app.ui.utils.ZoneMapping
import com.bridgeone.app.ui.utils.ZonePlacement
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
 * @param zoneCrossBehavior 멀티 존 실시간 점프 중 손을 떼지 않고 다른 서브 패드로 경계를 넘었을 때의
 *        동작(끄기/진동/점프 금지, Phase 4.9.11). 환경 설정(Page 5)에서 편집, StandardModePage에서 hoisting.
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
    onMagnificationModeChange: (MagnificationMode) -> Unit = {},
    zoneCrossBehavior: ZoneCrossBehavior = ZoneCrossBehavior.OFF
) {
    // 단일 줌 좌표 계산/UI에 쓰는 파생값(멀티 존은 Phase 4.9.11에서 배선).
    val currentMapping = (magnificationMode as? MagnificationMode.Single)?.mapping ?: ZoneMapping()
    // 멀티 존 파생값 (Phase 4.9.11). Zone(enabled=false)이면 정의 세션 진행 중, enabled=true면 실시간 점프.
    val zoneState = (magnificationMode as? MagnificationMode.Zone)?.state
    val isZoneMode = zoneState != null
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

    // ── 멀티 존 정의 세션 상태 (Phase 4.9.11) ──
    // 개수 선택 팝업 표시 여부. ZoomButton 롱프레스로 true가 된다.
    var multiZonePopupVisible by remember { mutableStateOf(false) }
    // 정의 순서(0부터). 개수 선택 확정 시 0으로 리셋, 존 하나 확정마다 ++.
    val definingZoneIndex = remember { mutableStateOf(0) }
    // 정의 드래그가 끝나고 별도 탭으로 확정하기를 기다리는 상태(4.9.6 zoomAwaitingConfirm과 동형).
    val zoneRectAwaitingConfirm = remember { mutableStateOf(false) }
    // 정의 중/확정 대기 중인 PC 매핑 직사각형(오버레이 프리뷰와 UP 커밋이 공유).
    val zoneRectPreview = remember { mutableStateOf<ZoneRect?>(null) }
    // 정의 드래그의 중심점(오버레이에 점으로 표시). zoneRectPreview와 생명주기를 함께한다(설정/해제 동시).
    val zoneCenterPoint = remember { mutableStateOf<TouchRatio?>(null) }
    // 확정 대기 중 롱프레스 재시작이 "이번 존 재정의"인지(true) 최초 정의인지(false) 구분 —
    // 오버레이 안내 텍스트를 "정의 중"/"재정의 중"으로 분기하는 데만 쓰인다. 존 커밋 시 false로 리셋.
    val zoneRedefining = remember { mutableStateOf(false) }

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
                zoneCrossBehavior = zoneCrossBehavior,
                zoomArming = zoomArming,
                zoomAwaitingConfirm = zoomAwaitingConfirm,
                onMagnificationModeChange = onMagnificationModeChange,
                onZoomArmingChange = { zoomArming = it },
                onZoomAwaitingConfirmChange = { zoomAwaitingConfirm = it },
                definingZoneIndex = definingZoneIndex,
                zoneRectAwaitingConfirm = zoneRectAwaitingConfirm,
                zoneRectPreview = zoneRectPreview,
                zoneCenterPoint = zoneCenterPoint,
                zoneRedefining = zoneRedefining,
                onRequestRestartDefinition = {
                    // 확정 대기 중 롱프레스 재시작(§713). 첫 존(idx==0)은 아직 확정된 존이 없으므로
                    // 개수 선택 팝업부터 전체 재시작. 두 번째 존부터는 이미 확정된 이전 존들을
                    // 잃지 않도록 이번 존의 직사각형만 지우고 같은 존을 다시 그리게 한다(재정의 중).
                    if (definingZoneIndex.value == 0) {
                        onMagnificationModeChange(MagnificationMode.Off)
                        zoneRectAwaitingConfirm.value = false
                        zoneRectPreview.value = null
                        zoneCenterPoint.value = null
                        multiZonePopupVisible = true
                    } else {
                        zoneRectAwaitingConfirm.value = false
                        zoneRectPreview.value = null
                        zoneCenterPoint.value = null
                        zoneRedefining.value = true
                    }
                },
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
                    currentMapping.defined || zoomArming || isZoneMode -> TouchpadColorZoom
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

        // ControlButtonContainer 실제 렌더 높이 측정 (멀티 존 개수 선택 팝업 앵커링용, Phase 4.9.11.
        // Page2MultiCursorTouchpad.kt의 커서 수 팝업 앵커링 패턴과 동일).
        val controlButtonDensity = LocalDensity.current
        var controlButtonHeightDp by remember { mutableStateOf(0.dp) }

        Row(
            modifier = Modifier
                .fillMaxWidth(controlButtonWidthFraction)
                .align(Alignment.TopCenter)
                .onGloballyPositioned {
                    controlButtonHeightDp = with(controlButtonDensity) { it.size.height.toDp() }
                },
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
                // 멀티 존 활성(실시간 점프) 중엔 버튼이 "풀사이즈 모드"(해제 목적지)로 표시되도록 최대
                // 배율로 취급한다(단일 줌과 동일한 "목적지 미리보기" 관용구, Phase 4.9.11).
                zoomLevel = if (zoneState?.enabled == true) {
                    AbsolutePointingConstants.ZOOM_LEVEL_MAX
                } else {
                    AbsoluteCoordinateCalculator.zoomLevelFromPcRect(currentMapping.pcRect)
                },
                zoomArming = zoomArming,
                onZoomClick = {
                    if (isZoneMode) {
                        // 멀티 존 활성/정의 중 재탭 → 즉시 해제 + 해제 프레임 1회 전송(§715).
                        onMagnificationModeChange(MagnificationMode.Off)
                        sendZoneStateFrame(ZoneRect.FULL, targetMonitor)
                    } else {
                        toggleAbsoluteZoom(
                            isActive = currentMapping.defined,
                            isArming = zoomArming,
                            targetMonitor = targetMonitor,
                            onMagnificationModeChange = onMagnificationModeChange,
                            onZoomArmingChange = { zoomArming = it },
                            onZoomAwaitingConfirmChange = { zoomAwaitingConfirm = it },
                        )
                    }
                },
                onZoomLongClick = {
                    // 멀티 존 진입(Phase 4.9.11): 단일 줌 arming 취소 후 개수 선택 팝업 표시.
                    zoomArming = false
                    zoomAwaitingConfirm = false
                    multiZonePopupVisible = true
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

        // 멀티 존 개수 선택 팝업 (Phase 4.9.11). CursorCountSelectionPopup 재사용 —
        // 범위는 2~8(countRange), 패드별 프리셋 개념이 없으므로 skipPreset=true로 즉시 확정.
        CursorCountSelectionPopup(
            visible = multiZonePopupVisible,
            onDismiss = { multiZonePopupVisible = false },
            currentCount = if (zoneState?.enabled == true) zoneState.zoneCount else null,
            onDisable = if (zoneState?.enabled == true) {
                {
                    onMagnificationModeChange(MagnificationMode.Off)
                    sendZoneStateFrame(ZoneRect.FULL, targetMonitor)
                    multiZonePopupVisible = false
                }
            } else null,
            anchorTopDp = controlButtonHeightDp,
            countRange = AbsolutePointingConstants.MULTI_ZONE_COUNT_MIN..AbsolutePointingConstants.MULTI_ZONE_COUNT_MAX,
            skipPreset = true,
            onConfirm = { count, _ ->
                // 정의 세션 시작(§709): 모든 존을 미정의 상태로 초기화하고 idx=0부터 순차 정의.
                onMagnificationModeChange(
                    MagnificationMode.Zone(
                        MultiZoneState(
                            enabled = false,
                            zoneCount = count,
                            placement = ZonePlacement.AUTO,
                            zones = List(AbsolutePointingConstants.MULTI_ZONE_COUNT_MAX) {
                                ZoneMapping(targetMonitor = targetMonitor)
                            }
                        )
                    )
                )
                definingZoneIndex.value = 0
                zoneRectAwaitingConfirm.value = false
                zoneRectPreview.value = null
                zoneCenterPoint.value = null
                zoneRedefining.value = false
                zoomArming = false
                zoomAwaitingConfirm = false
                localState = localState.copy(dragMode = false)
                multiZonePopupVisible = false
            },
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
        )
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
 * 멀티 존 PC 매핑 직사각형(임의 종횡비 [ZoneRect]) Vendor CDC 프레임을 UART로 전송합니다
 * (Phase 4.9.11). 단일 줌([sendZoomStateFrame])과 달리 중심점+배율 모델로 표현할 수 없는
 * 임의 종횡비 직사각형을 4축 독립 인코딩으로 손실 없이 전송한다. 전송 시점(확정 1회/정의
 * 드래그 중 30Hz 스로틀/해제 1회)은 호출측이 결정한다.
 */
private fun sendZoneStateFrame(pcRect: ZoneRect, targetMonitor: Int) {
    try {
        UsbSerialManager.sendVendorCdcFrame(ZoomStateCommand.buildFrame(pcRect, targetMonitor))
    } catch (e: IllegalStateException) {
        Log.w("AbsolutePointingPad", "Failed to send zone state: ${e.message}")
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
    zoneCrossBehavior: ZoneCrossBehavior,
    zoomArming: Boolean,
    zoomAwaitingConfirm: Boolean,
    onMagnificationModeChange: (MagnificationMode) -> Unit,
    onZoomArmingChange: (Boolean) -> Unit,
    onZoomAwaitingConfirmChange: (Boolean) -> Unit,
    definingZoneIndex: MutableState<Int>,
    zoneRectAwaitingConfirm: MutableState<Boolean>,
    zoneRectPreview: MutableState<ZoneRect?>,
    zoneCenterPoint: MutableState<TouchRatio?>,
    zoneRedefining: MutableState<Boolean>,
    onRequestRestartDefinition: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val view = LocalView.current
    // 단일 줌 좌표 계산/UI에 쓰는 파생값.
    val mapping = (magnificationMode as? MagnificationMode.Single)?.mapping ?: ZoneMapping()
    // 멀티 존 파생값 (Phase 4.9.11).
    val zoneState = (magnificationMode as? MagnificationMode.Zone)?.state
    val isZoneMode = zoneState != null
    // pointerInput은 clickMode/filteredConfig/localState.dragMode 변경 시에만 재시작되므로,
    // targetMonitor(모니터 셀렉터 선택값)·확대 매핑 모드 변경을 실행 중인 제스처 루프에서도 즉시
    // 반영하려면 rememberUpdatedState로 최신값을 캡처해야 한다.
    val currentTargetMonitor by rememberUpdatedState(targetMonitor)
    val currentMapping by rememberUpdatedState(mapping)
    val currentMagnificationMode by rememberUpdatedState(magnificationMode)
    val currentZoneCrossBehavior by rememberUpdatedState(zoneCrossBehavior)
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
        mapping.defined || zoomArming || isZoneMode -> TouchpadColorZoom
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

                    // ── 멀티 존 정의/실시간 점프 파생값 (Phase 4.9.11) ──
                    // defining: 개수 선택 직후~마지막 존 확정 전(enabled=false). liveJump: 정의 완료 후
                    // 실시간 점프(enabled=true). 둘 다 아니면 단일 줌/일반 포인팅 분기로 진행.
                    val zoneStateNow = (currentMagnificationMode as? MagnificationMode.Zone)?.state
                    val defining = zoneStateNow != null && !zoneStateNow.enabled
                    val liveJump = zoneStateNow != null && zoneStateNow.enabled
                    val zoneAreas = if (liveJump) {
                        divideZoneAreas(areaWidth, areaHeight, zoneStateNow!!.zoneCount)
                    } else {
                        emptyList()
                    }

                    // 존 정의 드래그가 이번 제스처에서 진행 중인지(단일 줌 zoomDefining과 동형).
                    var zoneDefining = false
                    var zoneCenterRatio = lastRatio
                    // 확정 대기 중 재터치가 탭(확정)/드래그(재조정)/롱프레스(재시작) 중 무엇인지 판가름 전 상태.
                    var zoneAdjusting = false
                    // 확정 대기 중 재터치 시작한 롱프레스 재시작 타이머(§713). 재드래그 전환/탭 확정 시 취소.
                    var zoneRestartJob: Job? = null
                    // 실시간 점프 중 마지막으로 전송한 존 인덱스(중복 ZoneState 프레임 억제, §721).
                    var lastSentZoneIdx = -1
                    // 이번 연속 터치(DOWN~UP)가 시작된 존 인덱스(Phase 4.9.11 zoneCrossBehavior=BLOCK용).
                    // 손을 떼지 않은 채 다른 존으로 넘어가도 이 값으로 좌표를 고정한다.
                    var zoneTouchStartIdx = -1
                    // 존 정의 드래그 중 30Hz 스로틀 기준 시각(§723, 단일 줌과 동일 상수 재사용).
                    var lastZoneTxMs = 0L

                    // 멀티 존 확정 대기 중 재드래그로 새 직사각형 정의를 시작하는 헬퍼. 중심점도 함께
                    // hoisted 상태로 내보내 오버레이가 점으로 표시할 수 있게 한다. 초기 프리뷰도
                    // rectFromCenterDrag(center, center)로 계산해 최소 크기 보장을 그대로 적용받는다
                    // (MOVE 없이 바로 손을 떼는 경우에도 0폭 직사각형이 남지 않도록).
                    fun startZoneDefining(centerRatio: TouchRatio) {
                        zoneDefining = true
                        zoneCenterRatio = centerRatio
                        zoneRectPreview.value = rectFromCenterDrag(centerRatio, centerRatio)
                        zoneCenterPoint.value = centerRatio
                    }

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
                    // 멀티 존 정의 중(defining)에는 대상 셀이 패드 전체를 차지하므로 엣지 gate를
                    // 억제한다(§710 확정 해석). 실시간 점프(liveJump)는 엣지존과 공존(§727)이라 gate 유지.
                    val isAssignedZone = gateZone?.trigger?.hasAssignedAction() == true && !defining

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
                        if (defining && zoneRectAwaitingConfirm.value) {
                            // ── 확정 대기 중 추가 터치: 탭(확정)/드래그(재조정)/롱프레스(재시작) 미판정.
                            // 판가름 전까지는 기존 프리뷰(zoneRectPreview)를 건드리지 않는다. 롱프레스
                            // 재시작 타이머(§713)를 시작 — 재드래그 전환/탭 확정 시 취소된다.
                            zoneAdjusting = true
                            zoneRestartJob = coroutineScope.launch {
                                delay(AbsolutePointingConstants.MULTI_ZONE_RESTART_LONGPRESS_MS)
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                onRequestRestartDefinition()
                            }
                        } else if (defining) {
                            // ── 새 존 정의 시작: 패드 전체가 대상 셀(§710 확정 해석), DOWN 위치를 중심점으로 ──
                            startZoneDefining(lastRatio)
                        } else if (liveJump) {
                            // ── 실시간 점프 DOWN: hitTestPad로 셀 판정, 존 전환 시 ZoneState 1회 전송 ──
                            // 이번 연속 터치의 시작 존을 기록(zoneCrossBehavior=BLOCK일 때 좌표 고정 기준).
                            val idx = hitTestPad(downPosition, zoneAreas)
                            if (idx >= 0) {
                                zoneTouchStartIdx = idx
                                val zm = zoneStateNow!!.zones[idx]
                                if (idx != lastSentZoneIdx) {
                                    sendZoneStateFrame(zm.pcRect, zm.targetMonitor)
                                    lastSentZoneIdx = idx
                                }
                                touchActive = true
                                indicatorPosition = downPosition
                                // 드래그 앤 드롭과 충돌 방지(§727): dragPressed는 절대 세우지 않는다.
                                sendAbsolutePosition(
                                    resolveZoneRatio(downPosition, zoneAreas[idx], zm),
                                    0x00u,
                                    zm.targetMonitor.toUByte()
                                )
                            }
                        } else if (currentZoomArming && currentZoomAwaitingConfirm) {
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

                            if (zoneDefining) {
                                // ── 존 정의 드래그 종료 ──
                                zoneDefining = false
                                val drawnRect = zoneRectPreview.value ?: ZoneRect.FULL
                                // ── 겹침 검증(Phase 4.9.11): 존을 그리는 시점(드래그 종료)에 바로 검증한다
                                // (원탭 확정까지 기다리지 않음). 이미 확정된 존과 겹치면 확정 대기로 넘어가지
                                // 않고 그 자리에서 거부 후 같은 존을 다시 그리게 한다(재정의 중 표시) ──
                                val overlapsExisting = zoneStateNow != null && zoneStateNow.zones.filter { it.defined }
                                    .any { rectsOverlap(drawnRect, it.pcRect) }
                                if (overlapsExisting) {
                                    view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                                    ToastController.show(
                                        "이미 정의된 존과 겹칩니다. 다시 정의해주세요.",
                                        ToastType.ERROR
                                    )
                                    zoneRectPreview.value = null
                                    zoneCenterPoint.value = null
                                    zoneRedefining.value = true
                                } else {
                                    // 직사각형은 이 시점 프리뷰로 고정, 확정은 아직 안 됨 — "확정 대기"로
                                    // 전환(4.9.6 zoomDefining UP 패턴과 동형, §711) ──
                                    zoneRectAwaitingConfirm.value = true
                                    sendZoneStateFrame(drawnRect, currentTargetMonitor.toInt())
                                }
                            } else if (zoneAdjusting) {
                                // ── 확정 대기 중 재터치가 끝까지 드래그로 전환되지 않음 → 탭 판정.
                                // 겹침 검증은 이미 정의(드래그 종료) 시점에 끝났으므로 여기서는 재검증 없이
                                // 그대로 확정하고 다음 존으로(또는 마지막이면 실시간 전환, §712/§714) ──
                                zoneRestartJob?.cancel()
                                zoneRestartJob = null
                                if (isTapGesture && zoneStateNow != null) {
                                    val idx = definingZoneIndex.value
                                    val newRect = zoneRectPreview.value ?: ZoneRect.FULL
                                    val committedZones = zoneStateNow.zones.toMutableList().also {
                                        it[idx] = ZoneMapping(
                                            pcRect = newRect,
                                            targetMonitor = currentTargetMonitor.toInt(),
                                            defined = true
                                        )
                                    }
                                    zoneRectAwaitingConfirm.value = false
                                    zoneRectPreview.value = null
                                    zoneCenterPoint.value = null
                                    zoneRedefining.value = false
                                    if (idx + 1 >= zoneStateNow.zoneCount) {
                                        // 마지막 존 확정 → 실시간 점프 모드로 전환(§714)
                                        onMagnificationModeChange(
                                            MagnificationMode.Zone(zoneStateNow.copy(enabled = true, zones = committedZones))
                                        )
                                    } else {
                                        onMagnificationModeChange(
                                            MagnificationMode.Zone(zoneStateNow.copy(zones = committedZones))
                                        )
                                        definingZoneIndex.value = idx + 1
                                    }
                                }
                                zoneAdjusting = false
                            } else if (liveJump) {
                                // ── 실시간 점프는 DOWN/MOVE에서 이미 좌표를 전송했으므로 UP은 상태만 정리 ──
                            } else if (zoomDefining) {
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
                            if (zoneDefining) {
                                // ── 존 정의 모드: 중심점 대비 손가락 위치로 직사각형 실시간 갱신·프리뷰 (§711) ──
                                val fingerRatio = AbsoluteCoordinateCalculator.calculateTouchRatio(pos.x, pos.y, areaWidth, areaHeight)
                                val rect = rectFromCenterDrag(zoneCenterRatio, fingerRatio)
                                zoneRectPreview.value = rect
                                val now = System.currentTimeMillis()
                                if (now - lastZoneTxMs >= AbsolutePointingConstants.ZOOM_STATE_THROTTLE_MS) {
                                    lastZoneTxMs = now
                                    sendZoneStateFrame(rect, currentTargetMonitor.toInt())
                                }
                            } else if (zoneAdjusting) {
                                // ── 확정 대기 중 재터치: 이동 거리가 클릭 임계값을 넘으면 재정의 드래그로
                                // 전환(기존 확정 후보를 이 시점부터 덮어씀), 롱프레스 재시작 타이머는 취소 ──
                                val movedPx = (pos - downPosition).getDistance()
                                val movedDp = with(density) { movedPx.toDp().value }
                                if (movedDp > AbsolutePointingConstants.CLICK_MAX_MOVEMENT_DP) {
                                    zoneRestartJob?.cancel()
                                    zoneRestartJob = null
                                    zoneAdjusting = false
                                    val fingerRatio = AbsoluteCoordinateCalculator.calculateTouchRatio(downPosition.x, downPosition.y, areaWidth, areaHeight)
                                    startZoneDefining(fingerRatio)
                                }
                            } else if (liveJump) {
                                // ── 실시간 점프 MOVE: hitTestPad로 셀 판정, 존 전환 시에만 ZoneState 재전송 ──
                                val rawIdx = hitTestPad(pos, zoneAreas)
                                if (rawIdx >= 0) {
                                    // 손을 떼지 않은 채 다른 존으로 경계를 넘었는지(§ zoneCrossBehavior, Phase 4.9.11).
                                    // lastSentZoneIdx가 아직 -1(이번 터치 첫 판정)이면 "진입"이지 "이동"이
                                    // 아니므로 크로스로 취급하지 않는다.
                                    val crossedZone = lastSentZoneIdx >= 0 && rawIdx != lastSentZoneIdx
                                    if (crossedZone && currentZoneCrossBehavior == ZoneCrossBehavior.HAPTIC) {
                                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    }
                                    // BLOCK이면 점프를 막고 좌표를 시작 존에 고정 — normalizeInZone의 0~1
                                    // 클램프 덕분에 손가락이 계속 나가도 커서는 시작 존의 매핑 경계에서 멈춘다.
                                    val effectiveIdx = if (crossedZone &&
                                        currentZoneCrossBehavior == ZoneCrossBehavior.BLOCK &&
                                        zoneTouchStartIdx >= 0
                                    ) {
                                        zoneTouchStartIdx
                                    } else {
                                        rawIdx
                                    }
                                    val zm = zoneStateNow!!.zones[effectiveIdx]
                                    if (effectiveIdx != lastSentZoneIdx) {
                                        sendZoneStateFrame(zm.pcRect, zm.targetMonitor)
                                        lastSentZoneIdx = effectiveIdx
                                    }
                                    indicatorPosition = pos
                                    sendAbsolutePosition(
                                        resolveZoneRatio(pos, zoneAreas[effectiveIdx], zm),
                                        0x00u,
                                        zm.targetMonitor.toUByte()
                                    )
                                }
                            } else if (zoomDefining) {
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

        // 멀티 존 정의 오버레이 (Phase 4.9.11): 정의 중(enabled=false)에만 표시. 확정된 해석(§710)대로
        // 대상 셀이 패드 전체를 차지하므로 그리드 분할선·나머지 셀 딤 표시는 없다. 다만 이미 확정된
        // 이전 존들의 PC 매핑 직사각형을 옅게 겹쳐 그려, 새 존을 그릴 때 겹치지 않게 참고할 수 있게 한다.
        if (zoneState != null && !zoneState.enabled) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // 이미 확정된 이전 존들 — 겹침 방지 참고용, 현재 프리뷰보다 옅고 얇게 그려 구분한다.
                zoneState.zones.filter { it.defined }.forEach { z ->
                    val r = z.pcRect
                    drawRect(
                        color = TouchpadColorZoom.copy(alpha = AbsolutePointingConstants.MULTI_ZONE_PREVIOUS_RECT_ALPHA),
                        topLeft = Offset(r.minX * size.width, r.minY * size.height),
                        size = Size(
                            (r.maxX - r.minX) * size.width,
                            (r.maxY - r.minY) * size.height
                        ),
                        style = Stroke(
                            width = with(density) { AbsolutePointingConstants.MULTI_ZONE_PREVIOUS_RECT_WIDTH_DP.dp.toPx() }
                        )
                    )
                }
                // 현재 정의/재정의 중인 프리뷰 — 위 레이어보다 진하고 굵게 그려 구분한다.
                zoneRectPreview.value?.let { rect ->
                    drawRect(
                        color = TouchpadColorZoom.copy(alpha = AbsolutePointingConstants.MULTI_ZONE_RECT_PREVIEW_ALPHA),
                        topLeft = Offset(rect.minX * size.width, rect.minY * size.height),
                        size = Size(
                            (rect.maxX - rect.minX) * size.width,
                            (rect.maxY - rect.minY) * size.height
                        ),
                        style = Stroke(
                            width = with(density) { AbsolutePointingConstants.MULTI_ZONE_RECT_PREVIEW_WIDTH_DP.dp.toPx() }
                        )
                    )
                }
                // 정의 드래그의 중심점 — 손가락이 어디서 시작했는지 기준점을 명확히 보여준다.
                zoneCenterPoint.value?.let { center ->
                    val dotCenter = Offset(center.x * size.width, center.y * size.height)
                    val dotRadiusPx = with(density) { AbsolutePointingConstants.MULTI_ZONE_CENTER_DOT_RADIUS_DP.dp.toPx() }
                    drawCircle(color = Color.White, radius = dotRadiusPx, center = dotCenter)
                    drawCircle(
                        color = TouchpadColorZoom,
                        radius = dotRadiusPx,
                        center = dotCenter,
                        style = Stroke(
                            width = with(density) { AbsolutePointingConstants.MULTI_ZONE_CENTER_DOT_RING_WIDTH_DP.dp.toPx() }
                        )
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.align(Alignment.Center)
            ) {
                Text(
                    text = "존 ${definingZoneIndex.value + 1}/${zoneState.zoneCount} ${if (zoneRedefining.value) "재정의 중" else "정의 중"}",
                    color = TouchpadColorZoom,
                    fontSize = AbsolutePointingConstants.MULTI_ZONE_GUIDE_TEXT_SIZE_SP.sp,
                    fontWeight = FontWeight.Bold
                )
                if (zoneRectAwaitingConfirm.value) {
                    Text(
                        text = "탭하여 확정 · 길게 눌러 재시작",
                        color = TouchpadColorZoom,
                        fontSize = AbsolutePointingConstants.ZOOM_CONFIRM_HINT_TEXT_SIZE_SP.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 멀티 존 실시간 점프 그리드 오버레이 (Phase 4.9.11): 모든 존 정의가 끝나 enabled=true로
        // 전환된 뒤에도 서브 패드 경계를 계속 표시해, 하나의 큰 패드가 아니라 균등 분할된 여러
        // 서브 패드로 보이게 한다(divideZoneAreas는 hitTestPad 판정과 동일한 셀 분할을 재사용).
        if (zoneState != null && zoneState.enabled) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                divideZoneAreas(size.width, size.height, zoneState.zoneCount).forEach { cell ->
                    drawRect(
                        color = TouchpadColorZoom.copy(alpha = AbsolutePointingConstants.MULTI_ZONE_GRID_LINE_ALPHA),
                        topLeft = Offset(cell.left, cell.top),
                        size = Size(cell.width, cell.height),
                        style = Stroke(
                            width = with(density) { AbsolutePointingConstants.MULTI_ZONE_GRID_LINE_WIDTH_DP.dp.toPx() }
                        )
                    )
                }
            }
        }

        // 줌 레벨 텍스트 (Phase 4.9.6): 정의/확정 대기 중(zoomArming)은 화면 정가운데 크게(유저 확정,
        // 원탭 확정 흐름에서 진행 상태를 명확히 보여주기 위함), 확정된 활성 줌(mapping.defined만,
        // arming 아님)은 설계 §4.5.4대로 우상단에 작게 표시. 1x(둘 다 아님)에서는 미표시.
        // 배율 수치는 pcRect 폭에서 역산(Phase 4.9.10, zoomLevelFromPcRect).
        // 멀티 존 모드(isZoneMode)에서는 위 오버레이(정의 중 프리뷰 또는 실시간 그리드)가 대신
        // 표시되므로 이 블록은 건너뛴다.
        if (isZoneMode) {
            // 멀티 존: 정의 중엔 정의 오버레이, 실시간 점프 중엔 그리드 오버레이(위)가 표시. 텍스트는 없음.
        } else if (zoomArming) {
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
