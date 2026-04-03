package com.bridgeone.app.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.bridgeone.app.protocol.BridgeMode
import com.bridgeone.app.ui.common.EdgeSwipeConstants
import com.bridgeone.app.ui.common.ScrollConstants.INFINITE_SCROLL_HAPTIC_MAX_VELOCITY_DP_MS
import com.bridgeone.app.ui.common.ScrollConstants.INFINITE_SCROLL_MIN_VELOCITY_DP_MS
import com.bridgeone.app.ui.common.ScrollConstants.INFINITE_SCROLL_TIME_CONSTANT_MS
import com.bridgeone.app.ui.common.ScrollConstants.INFINITE_SCROLL_VELOCITY_WINDOW_MS
import com.bridgeone.app.ui.common.ScrollConstants.RIGHT_ANGLE_AXIS_LOCK_DISTANCE_DP
import com.bridgeone.app.ui.common.ScrollConstants.RIGHT_ANGLE_DEADBAND_DEG
import com.bridgeone.app.ui.common.ScrollConstants.SCROLL_AXIS_DIAGONAL_DEAD_ZONE_DEG
import com.bridgeone.app.ui.common.ScrollConstants.SCROLL_AXIS_LOCK_DISTANCE_DP
import com.bridgeone.app.ui.common.ScrollConstants.SCROLL_GUIDELINE_HIDE_DELAY_MS
import com.bridgeone.app.ui.common.ScrollConstants.SCROLL_GUIDELINE_STEP_DP
import com.bridgeone.app.ui.common.ScrollConstants.SCROLL_STOP_THRESHOLD_MS
import com.bridgeone.app.ui.common.ScrollConstants.SCROLL_UNIT_DISTANCE_DP
import com.bridgeone.app.ui.common.DYNAMICS_PRESETS
import com.bridgeone.app.ui.components.touchpad.ClickMode
import com.bridgeone.app.ui.components.touchpad.ControlButtonConfig
import com.bridgeone.app.ui.components.touchpad.CursorMode
import com.bridgeone.app.ui.components.touchpad.DynamicsPresetButton
import com.bridgeone.app.ui.components.touchpad.ModePresetButton
import com.bridgeone.app.ui.components.touchpad.EdgeBumpOverlay
import com.bridgeone.app.ui.components.touchpad.EdgePopupMode
import com.bridgeone.app.ui.components.touchpad.EdgeSwipeMode
import com.bridgeone.app.ui.components.touchpad.EdgeSwipeOverlay
import com.bridgeone.app.ui.components.touchpad.EntryEdge
import com.bridgeone.app.ui.components.touchpad.NormalScrollButtons
import com.bridgeone.app.ui.components.touchpad.RightAngleGuideline
import com.bridgeone.app.ui.components.touchpad.ScrollAxis
import com.bridgeone.app.ui.components.touchpad.ScrollGuideline
import com.bridgeone.app.ui.components.touchpad.ScrollMode
import com.bridgeone.app.ui.components.touchpad.MoveMode
import com.bridgeone.app.ui.components.touchpad.TouchpadState
import com.bridgeone.app.ui.components.touchpad.touchpadBorderColors
import com.bridgeone.app.ui.utils.ClickDetector
import com.bridgeone.app.ui.utils.DeltaCalculator
import com.bridgeone.app.ui.utils.RightAngleAxis
import com.bridgeone.app.ui.utils.getDistance
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.exp
import kotlin.math.roundToInt


/**
 * TouchpadWrapper Composable
 *
 * 터치패드 UI 구성 및 터치 이벤트 감지를 담당합니다.
 *
 * Phase 4.3.3 업데이트:
 * - [touchpadState] 파라미터 추가 (스크롤/커서 모드 분기)
 * - [onTouchpadStateChange] 파라미터 추가 (더블탭으로 스크롤 모드 종료)
 * - 일반 스크롤 모드 (NORMAL_SCROLL): 축 확정 알고리즘 + 스크롤 프레임 전송
 * - ScrollGuideline 오버레이 표시
 *
 * Phase 4.3.4 업데이트:
 * - 무한 스크롤 모드 (INFINITE_SCROLL): 드래그 중 속도 샘플 수집 → UP 시 관성 코루틴 시작
 * - 관성: 프레임-레이트 독립적 지수 감쇠 (exp(-dt/τ)), τ = INFINITE_SCROLL_TIME_CONSTANT_MS
 * - 관성 중 터치 DOWN → 즉시 관성 정지
 * - ScrollGuideline에 scrollMode/scrollIntensity 전달 (무한 스크롤: 빨강 + 강도 감소)
 *
 * **스크롤 축 확정 알고리즘:**
 * 1. DOWN 시 scrollAxis = UNDECIDED, 누적 벡터 초기화
 * 2. MOVE 중 누적 이동이 [SCROLL_AXIS_LOCK_DISTANCE_DP] 초과 시 축 판정:
 *    - atan2(|ΔY|, |ΔX|) 기준: 45° 근방 ±15° = 대각선(UNDECIDED 유지)
 *    - 0°~30° = HORIZONTAL, 60°~90° = VERTICAL
 * 3. 축 확정 후 해당 축으로만 스크롤 프레임 전송 (UP까지 유지)
 *
 * **원탭으로 스크롤 종료:**
 * 스크롤 모드에서 드래그 없는 탭 한 번 → [onTouchpadStateChange] 호출, scrollMode = OFF.
 * 단, NORMAL_SCROLL에서 직전 제스처가 드래그였으면 차단 (빠른 스와이프 후 실수 탭 방지),
 * INFINITE_SCROLL에서 관성 진행 중 탭이었으면 차단.
 *
 * @param modifier 외부에서 추가할 Modifier
 * @param bridgeMode Essential/Standard 모드 구분
 * @param touchpadState 현재 터치패드 모드 상태 (스크롤/커서/DPI 등)
 * @param onTouchpadStateChange 터치패드 상태 변경 콜백 (더블탭 스크롤 종료 등)
 * @param onTouchEvent 터치 이벤트 콜백 (선택사항)
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TouchpadWrapper(
    modifier: Modifier = Modifier,
    bridgeMode: BridgeMode = BridgeMode.ESSENTIAL,
    touchpadState: TouchpadState = TouchpadState(),
    onTouchpadStateChange: (TouchpadState) -> Unit = {},
    onDynamicsLongPress: () -> Unit = {},
    onModePresetLongPress: () -> Unit = {},
    config: ControlButtonConfig = ControlButtonConfig(),
    onTouchEvent: (
        eventType: PointerEventType,
        currentPosition: Offset,
        previousPosition: Offset
    ) -> Unit = { _, _, _ -> }
) {
    val density = LocalDensity.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    @Suppress("DEPRECATION")
    val vibrator = remember { view.context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator }

    // rememberUpdatedState: pointerInput(Unit) 제스처 루프 재시작 없이 최신 상태 참조
    val latestState by rememberUpdatedState(touchpadState)
    val latestOnStateChange by rememberUpdatedState(onTouchpadStateChange)
    val latestConfig by rememberUpdatedState(config)

    // ── 커서 모드 상태 (기존) ──
    val currentTouchPosition = remember { mutableStateOf(Offset.Zero) }
    val previousTouchPosition = remember { mutableStateOf(Offset.Zero) }
    val touchDownTime = remember { mutableStateOf(0L) }
    val touchDownPosition = remember { mutableStateOf(Offset.Zero) }
    val compensatedDeltaX = remember { mutableStateOf(0f) }
    val compensatedDeltaY = remember { mutableStateOf(0f) }
    val deadZoneEscaped = remember { mutableStateOf(false) }

    // ── 스크롤 모드 상태 (Phase 4.3.3) ──
    // 원탭 해제 조건: DOWN 시 관성이 활성이었으면 INFINITE_SCROLL 탭 해제 차단
    var inertiaWasActiveOnDown by remember { mutableStateOf(false) }

    // ScrollGuideline 표시 상태
    var guidelineVisible by remember { mutableStateOf(false) }
    var guidelineAxis by remember { mutableStateOf(ScrollAxis.VERTICAL) }
    var guidelineTarget by remember { mutableFloatStateOf(0f) }
    var guidelineScrollMode by remember { mutableStateOf(ScrollMode.NORMAL_SCROLL) }

    // 가이드라인 자동 숨김 Job
    var guidelineHideJob by remember { mutableStateOf<Job?>(null) }

    // 직각 이동 가이드라인 상태 (Phase 4.3.5)
    var rightAngleGuidelineVisible by remember { mutableStateOf(false) }
    var rightAngleGuidelineAxis by remember { mutableStateOf(RightAngleAxis.UNDECIDED) }

    // 무한 스크롤 관성 Job (Phase 4.3.4)
    var inertiaJob by remember { mutableStateOf<Job?>(null) }

    // 가이드라인 숨김 스케줄러
    fun scheduleGuidelineHide() {
        guidelineHideJob?.cancel()
        guidelineHideJob = coroutineScope.launch {
            delay(SCROLL_STOP_THRESHOLD_MS + SCROLL_GUIDELINE_HIDE_DELAY_MS)
            guidelineVisible = false
        }
    }

    // ── 엣지 스와이프 상태 (Phase 4.3.12) ──
    var isEdgeCandidate by remember { mutableStateOf(false) }
    var showEdgePopup by remember { mutableStateOf(false) }
    var entryEdge by remember { mutableStateOf<EntryEdge?>(null) }
    var fingerAlongEdgePx by remember { mutableFloatStateOf(0f) }
    var inwardDistancePx by remember { mutableFloatStateOf(0f) }

    // ── 산봉우리 애니메이션 상태 (Phase 4.4.6) ──
    // 드래그 중 마지막 유효 값 (release 시 즉시 0으로 리셋되지 않음 → 수축 애니메이션 시작점)
    var lastBumpInwardPx by remember { mutableFloatStateOf(0f) }
    var lastBumpAlongPx by remember { mutableFloatStateOf(0f) }
    var lastBumpEntryEdge by remember { mutableStateOf<EntryEdge?>(null) }
    val bumpShrinkAnimatable = remember { Animatable(0f) }
    var isBumpShrinking by remember { mutableStateOf(false) }
    var edgeSwipeHapticFired by remember { mutableStateOf(false) }
    // 대기 상태: 팝업 열릴 때 현재 상태로 초기화, 탭 토글로 변경, 확인 탭 시 적용
    var pendingEdgeState by remember { mutableStateOf<TouchpadState?>(null) }
    // 현재 선택(하이라이트)된 항목 인덱스 (null = 없음)
    var selectedItemIndex by remember { mutableStateOf<Int?>(null) }
    // 직접 터치 모드: 손가락을 놓은 위치 (버튼 그리드 앵커)
    var popupAnchorPx by remember { mutableStateOf(Offset.Zero) }
    // EdgePopupModeSelector(팝업 모드 선택기): 팝업 모드(스와이프/직접 터치)를 선택 중
    var isModeSelecting by remember { mutableStateOf(false) }
    // 선택된(또는 선택 중인) 팝업 모드 (null = 미선택)
    var selectedPopupMode by remember { mutableStateOf<EdgePopupMode?>(null) }

    // 프리셋 탭 라벨 표시 상태 (Phase 4.3.8)
    var showPresetLabel by remember { mutableStateOf(false) }
    var isFirstPresetRender by remember { mutableStateOf(true) }
    LaunchedEffect(touchpadState.dynamicsPresetIndex) {
        if (isFirstPresetRender) { isFirstPresetRender = false; return@LaunchedEffect }
        showPresetLabel = true
        delay(1500L)
        showPresetLabel = false
    }

    // 모드 프리셋 탭 라벨 표시 상태 (Phase 4.4.8)
    var showModePresetLabel by remember { mutableStateOf(false) }
    var isFirstModePresetRender by remember { mutableStateOf(true) }
    LaunchedEffect(touchpadState.modePresetIndex) {
        if (isFirstModePresetRender) { isFirstModePresetRender = false; return@LaunchedEffect }
        showModePresetLabel = true
        delay(1500L)
        showModePresetLabel = false
    }

    // ── 산봉우리 수축 애니메이션 (Phase 4.4.6) ──
    // 드래그 중에는 raw 값을 직접 전달 (LaunchedEffect/Animatable 불필요)
    // 릴리즈/취소 시에만 Animatable로 spring 수축
    LaunchedEffect(isEdgeCandidate) {
        if (!isEdgeCandidate && lastBumpInwardPx > 0f && !isModeSelecting) {
            // 팝업 등장 여부 무관 — 손 뗌 또는 모드 선택 직후 항상 수축 애니메이션 재생
            isBumpShrinking = true
            bumpShrinkAnimatable.snapTo(lastBumpInwardPx)
            bumpShrinkAnimatable.animateTo(
                0f,
                spring(
                    dampingRatio = EdgeSwipeConstants.BUMP_SHRINK_SPRING_DAMPING,
                    stiffness = EdgeSwipeConstants.BUMP_SHRINK_SPRING_STIFFNESS
                )
            )
            isBumpShrinking = false
            lastBumpEntryEdge = null
            lastBumpInwardPx = 0f
            lastBumpAlongPx = 0f
        } else if (!isEdgeCandidate && !isModeSelecting) {
            lastBumpEntryEdge = null
            lastBumpInwardPx = 0f
            lastBumpAlongPx = 0f
        }
    }

    val CORNER_RADIUS = 12.dp

    // 테두리 색상 애니메이션 (300ms) — Essential 모드는 Transparent로 처리
    val (leftTarget, rightTarget) = if (bridgeMode != BridgeMode.ESSENTIAL) {
        touchpadBorderColors(touchpadState)
    } else {
        Color.Transparent to Color.Transparent
    }
    val animatedLeftColor by animateColorAsState(
        targetValue = leftTarget,
        animationSpec = tween(300),
        label = "borderLeft"
    )
    val animatedRightColor by animateColorAsState(
        targetValue = rightTarget,
        animationSpec = tween(300),
        label = "borderRight"
    )

    // 글로우 애니메이션: 밝은 스팟이 테두리를 따라 천천히 이동 (3초 주기 무한 반복)
    // 단색일 때는 양쪽 색상이 동일하므로 시각적 변화 없음
    var glowProgress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            animate(0f, 1f, animationSpec = tween(3000, easing = LinearEasing)) { value, _ ->
                glowProgress = value
            }
        }
    }

    BoxWithConstraints(modifier = modifier) {
        // 테두리 너비: 터치패드 너비의 0.8% (최소 2dp, 최대 4dp)
        val borderWidth = (maxWidth * 0.008f).coerceIn(2.dp, 4.dp)

        // 글로우 Brush: [배경색, 밝은 스팟, 배경색] 패턴이 좌→우로 3초 주기 이동
        // 중심점: -widthPx(화면 왼쪽 밖) → +2*widthPx(화면 오른쪽 밖)
        // 양 끝에서 화면이 단색이므로 반복 리셋 시 점프가 보이지 않음
        val widthPx: Float = with(density) { maxWidth.toPx() }
        val glowShift: Float = (glowProgress * 3f - 1f) * widthPx
        val glowStartX: Float = glowShift - widthPx
        val glowEndX: Float = glowShift + widthPx
        val borderBrush = Brush.linearGradient(
            colors = listOf(animatedRightColor, animatedLeftColor, animatedRightColor),
            start = Offset(glowStartX, 0f),
            end = Offset(glowEndX, 0f)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(CORNER_RADIUS))
                .border(borderWidth, borderBrush, RoundedCornerShape(CORNER_RADIUS))
                .background(Color(0xFF1A1A1A))
                .pointerInput(Unit) {
                // dp → px 변환 상수 (제스처 루프 시작 전 1회 계산)
                val axisLockDistPx = density.run { SCROLL_AXIS_LOCK_DISTANCE_DP.dp.toPx() }
                val scrollUnitPx = density.run { SCROLL_UNIT_DISTANCE_DP.dp.toPx() }
                val deadZoneThresholdPx = DeltaCalculator.getDeadZoneThresholdPx(density)
                val rightAngleLockDistPx = density.run { RIGHT_ANGLE_AXIS_LOCK_DISTANCE_DP.dp.toPx() }

                // 엣지 스와이프 상수 (Phase 4.3.12)
                val edgeHitWidthPx = density.run { EdgeSwipeConstants.EDGE_HIT_WIDTH_DP.dp.toPx() }
                val triggerDistancePx = density.run { EdgeSwipeConstants.TRIGGER_DISTANCE_DP.dp.toPx() }
                val cancelThresholdPx = density.run { EdgeSwipeConstants.CANCEL_THRESHOLD_DP.dp.toPx() }
                val tapThresholdPx = density.run { EdgeSwipeConstants.EDGE_POPUP_TAP_THRESHOLD_DP.dp.toPx() }
                val navStepPx = density.run { EdgeSwipeConstants.EDGE_POPUP_NAV_STEP_DP.dp.toPx() }
                val directButtonSizePx = density.run { EdgeSwipeConstants.EDGE_POPUP_DIRECT_BUTTON_SIZE_DP.dp.toPx() }
                val directButtonGapPx = density.run { EdgeSwipeConstants.EDGE_POPUP_DIRECT_BUTTON_GAP_DP.dp.toPx() }
                val bumpAppearThresholdPx = density.run { EdgeSwipeConstants.DROPLET_APPEAR_THRESHOLD_DP.dp.toPx() }
                // 모드 선택 step: navStepPx 재사용

                awaitEachGesture {
                    // ── DOWN ──
                    val down = awaitPointerEvent()
                    if (down.type != PointerEventType.Press) return@awaitEachGesture
                    // NormalScrollButtons 등 자식 컴포넌트가 이미 이벤트 소비 시 터치패드 처리 건너뜀
                    if (down.changes.any { it.isConsumed }) return@awaitEachGesture
                    down.changes.forEach { it.consume() }

                    // 관성 중 터치 → 즉시 관성 정지 (Phase 4.3.4)
                    // 탭 해제 조건 판단을 위해 취소 전에 상태 캡처
                    inertiaWasActiveOnDown = inertiaJob?.isActive == true
                    inertiaJob?.cancel()
                    inertiaJob = null

                    currentTouchPosition.value = down.changes.first().position
                    previousTouchPosition.value = currentTouchPosition.value
                    touchDownTime.value = System.currentTimeMillis()
                    touchDownPosition.value = currentTouchPosition.value
                    compensatedDeltaX.value = 0f
                    compensatedDeltaY.value = 0f
                    deadZoneEscaped.value = false

                    // ── 팝업 열린 상태: 상대 이동으로 버튼 선택, 탭으로 토글/확정 ──
                    // DOWN 지점이 기준(0,0). 손가락이 navStepPx 이동할 때마다 선택이 1칸 이동.
                    // 절대 위치 무관 — 어디서 시작하든 상대 이동량으로만 선택 갱신.
                    if (showEdgePopup) {
                        val bgDownPos = down.changes.first().position

                        // visibleModes: overlay와 동일한 로직 (스크롤 활성 시 CLICK/MOVE 제외)
                        val isScrollingForPopup =
                            (pendingEdgeState ?: latestState).scrollMode != ScrollMode.OFF
                        val visibleModes = buildList<EdgeSwipeMode> {
                            if (latestConfig.showScrollMode) add(EdgeSwipeMode.SCROLL)
                            if (latestConfig.showClickMode && !isScrollingForPopup) add(EdgeSwipeMode.CLICK)
                            if (latestConfig.showMoveMode && !isScrollingForPopup) add(EdgeSwipeMode.MOVE)
                            if (latestConfig.showCursorMode) add(EdgeSwipeMode.CURSOR)
                        }
                        val modeCount = visibleModes.size

                        // ── 공통 엣지 취소 판정 람다 ──
                        val isNearEdge: (Offset) -> Boolean = { pos ->
                            pos.x < edgeHitWidthPx ||
                                    pos.x > size.width - edgeHitWidthPx ||
                                    pos.y < edgeHitWidthPx ||
                                    pos.y > size.height - edgeHitWidthPx
                        }
                        // ── 공통 팝업 리셋 ──
                        fun resetPopup() {
                            showEdgePopup = false
                            isModeSelecting = false
                            isEdgeCandidate = false
                            pendingEdgeState = null
                            selectedItemIndex = null
                            popupAnchorPx = Offset.Zero
                            selectedPopupMode = null
                        }

                        if (selectedPopupMode == EdgePopupMode.DIRECT_TOUCH && popupAnchorPx != Offset.Zero) {
                            // ═══ 직접 터치 모드 ═══
                            // 버튼 영역 히트 테스트 → 손가락 따라 하이라이트 → UP 위치의 버튼 동작
                            val buttonRects = computeDirectTouchButtonRects(
                                popupAnchorPx, size.width.toFloat(), size.height.toFloat(),
                                modeCount, directButtonSizePx, directButtonGapPx, density
                            )

                            // DOWN 시점 hover 표시
                            selectedItemIndex = buttonRects.indexOfFirst { it.contains(bgDownPos) }
                                .takeIf { it >= 0 }

                            var bgEv = awaitPointerEvent()
                            while (bgEv.type == PointerEventType.Move) {
                                bgEv.changes.forEach { it.consume() }
                                val pos = bgEv.changes.first().position

                                if (isNearEdge(pos)) { resetPopup(); return@awaitEachGesture }

                                // hover 업데이트
                                selectedItemIndex = buttonRects.indexOfFirst { it.contains(pos) }
                                    .takeIf { it >= 0 }
                                bgEv = awaitPointerEvent()
                            }

                            if (bgEv.type == PointerEventType.Release) {
                                val upPos = bgEv.changes.first().position
                                val hitIndex = buttonRects.indexOfFirst { it.contains(upPos) }
                                    .takeIf { it >= 0 }

                                selectedItemIndex = null  // 하이라이트 제거

                                if (hitIndex != null && modeCount > 0) {
                                    if (hitIndex < modeCount) {
                                        // 모드 버튼 탭
                                        val mode = visibleModes[hitIndex]
                                        pendingEdgeState = applyEdgeModeToggle(
                                            pendingEdgeState ?: latestState, mode
                                        )
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                        } else {
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        }
                                    } else {
                                        // 확인 버튼 탭
                                        val finalState = pendingEdgeState ?: latestState
                                        latestOnStateChange(finalState)
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                        } else {
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        }
                                        resetPopup()
                                    }
                                }
                            }

                        } else if (selectedPopupMode == EdgePopupMode.DIRECT_TOUCH && popupAnchorPx == Offset.Zero) {
                            // ═══ 직접 터치 앵커 지정 ═══
                            // 탭한 위치를 버튼 그리드 중심(앵커)으로 저장
                            var bgEv = awaitPointerEvent()
                            while (bgEv.type == PointerEventType.Move) {
                                bgEv.changes.forEach { it.consume() }
                                val pos = bgEv.changes.first().position
                                if (isNearEdge(pos)) { resetPopup(); return@awaitEachGesture }
                                bgEv = awaitPointerEvent()
                            }
                            if (bgEv.type == PointerEventType.Release) {
                                popupAnchorPx = bgEv.changes.first().position
                            }
                        } else if (selectedPopupMode == EdgePopupMode.SWIPE) {
                            // ═══ 스와이프 탐색 모드 ═══
                            val totalItems = modeCount + 1  // 모드 버튼 + 확인 버튼
                            val startIdx = selectedItemIndex ?: 0

                            var bgEv = awaitPointerEvent()
                            while (bgEv.type == PointerEventType.Move) {
                                bgEv.changes.forEach { it.consume() }
                                val pos = bgEv.changes.first().position

                                if (isNearEdge(pos)) { resetPopup(); return@awaitEachGesture }

                                val dx = pos.x - bgDownPos.x
                                val dy = pos.y - bgDownPos.y
                                val linearOffset = if (kotlin.math.abs(dx) >= kotlin.math.abs(dy))
                                    (dx / navStepPx).roundToInt()
                                else
                                    (dy / navStepPx).roundToInt()
                                selectedItemIndex = (startIdx + linearOffset).coerceIn(0, totalItems - 1)
                                bgEv = awaitPointerEvent()
                            }

                            if (bgEv.type == PointerEventType.Release) {
                                val upPos = bgEv.changes.first().position
                                val dist = (upPos - bgDownPos).getDistance()
                                if (dist < tapThresholdPx && modeCount > 0) {
                                    val idx = selectedItemIndex ?: 0
                                    if (idx < modeCount) {
                                        val mode = visibleModes[idx]
                                        pendingEdgeState = applyEdgeModeToggle(
                                            pendingEdgeState ?: latestState, mode
                                        )
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                        } else {
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        }
                                    } else {
                                        val finalState = pendingEdgeState ?: latestState
                                        latestOnStateChange(finalState)
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                        } else {
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        }
                                        resetPopup()
                                    }
                                }
                            }
                        }
                        return@awaitEachGesture
                    }

                    // ── 엣지 스와이프 감지 (Phase 4.3.12) ──
                    val downPos = down.changes.first().position
                    val detectedEntryEdge = detectEntryEdge(
                        pos = downPos,
                        width = size.width.toFloat(),
                        height = size.height.toFloat(),
                        edgeWidthPx = edgeHitWidthPx
                    )
                    isEdgeCandidate = detectedEntryEdge != null
                    entryEdge = detectedEntryEdge
                    edgeSwipeHapticFired = false
                    val edgeStartInwardPx = if (detectedEntryEdge != null)
                        getInwardDistance(downPos, detectedEntryEdge, size.width.toFloat(), size.height.toFloat())
                    else 0f
                    val edgeStartAlongPx = if (detectedEntryEdge != null)
                        getAlongEdgePosition(downPos, detectedEntryEdge)
                    else 0f

                    // 스크롤 모드 전용 로컬 상태 (각 제스처마다 초기화)
                    var scrollAxis = ScrollAxis.UNDECIDED
                    var axisAccumX = 0f   // 축 확정용 누적 X (px)
                    var axisAccumY = 0f   // 축 확정용 누적 Y (px)
                    var scrollAccum = 0f  // 스크롤 단위 누적 (소수점 보존)

                    // 무한 스크롤 속도 샘플 (Phase 4.3.4)
                    // 각 샘플: Pair(이동량 dp, 타임스탬프 ms)
                    val velocitySamples = ArrayDeque<Pair<Float, Long>>()

                    // 직각 이동 모드 전용 로컬 상태 (Phase 4.3.5)
                    var rightAngleAxis = RightAngleAxis.UNDECIDED
                    var rightAngleAccumX = 0f
                    var rightAngleAccumY = 0f
                    // 새 제스처 시작 시 가이드라인 초기화
                    rightAngleGuidelineVisible = false

                    // 커서 이동 속도 샘플 (Phase 4.3.8 다이나믹스용)
                    // 각 샘플: Pair(이동량 dp, 타임스탬프 ms)
                    val cursorVelocitySamples = ArrayDeque<Pair<Float, Long>>()

                    onTouchEvent(PointerEventType.Press, currentTouchPosition.value, previousTouchPosition.value)

                    // ── MOVE ──
                    var moveEvent = awaitPointerEvent()
                    while (moveEvent.type == PointerEventType.Move) {
                        moveEvent.changes.forEach { it.consume() }
                        val change = moveEvent.changes.first()
                        val allPositions = change.historical.map { it.position } + change.position

                        for (pos in allPositions) {
                            previousTouchPosition.value = currentTouchPosition.value
                            currentTouchPosition.value = pos

                            val rawDelta = DeltaCalculator.calculateDelta(
                                previousTouchPosition.value,
                                currentTouchPosition.value
                            )

                            // ── 엣지 스와이프 처리 (Phase 4.3.12) ──
                            if (isEdgeCandidate) {
                                val edge = entryEdge
                                val currentInward = getInwardDistance(
                                    pos, edge, size.width.toFloat(), size.height.toFloat()
                                )
                                val inwardMoved = currentInward - edgeStartInwardPx
                                val currentAlong = getAlongEdgePosition(pos, edge)
                                val perpMoved = abs(currentAlong - edgeStartAlongPx)

                                fingerAlongEdgePx = currentAlong
                                inwardDistancePx = inwardMoved.coerceAtLeast(0f)

                                // 산봉우리 시각화: 가장 가까운 엣지 기준으로 갱신 (Phase 4.4.6)
                                // 제스처 로직(entryEdge 기준 inwardMoved/perpMoved)은 변경하지 않음
                                val visualEdge = findNearestEdge(pos, size.width.toFloat(), size.height.toFloat())
                                lastBumpEntryEdge = visualEdge
                                lastBumpInwardPx = getInwardDistance(pos, visualEdge, size.width.toFloat(), size.height.toFloat()).coerceAtLeast(0f)
                                lastBumpAlongPx = getAlongEdgePosition(pos, visualEdge)

                                if (!showEdgePopup && !isModeSelecting) {
                                    when {
                                        inwardMoved >= triggerDistancePx -> {
                                            // 모드 선택 단계 진입: 관성 중단 후 모드 선택 UI 표시
                                            inertiaWasActiveOnDown = inertiaJob?.isActive == true
                                            inertiaJob?.cancel()
                                            inertiaJob = null
                                            isModeSelecting = true
                                            pendingEdgeState = latestState
                                            // 햅틱 피드백 (Phase 4.4.6)
                                            if (!edgeSwipeHapticFired) {
                                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                                edgeSwipeHapticFired = true
                                            }
                                        }
                                        inwardMoved < 0f -> {
                                            // 시작점보다 엣지 방향으로 되돌아감 → 후보 취소
                                            isEdgeCandidate = false
                                        }
                                        perpMoved >= triggerDistancePx && inwardDistancePx < bumpAppearThresholdPx -> {
                                            // 산봉우리가 나오기 전에 엣지 방향으로 충분히 이동 → 일반 커서로 처리
                                            // 산봉우리가 이미 나온 상태(inwardDistancePx >= bumpAppearThresholdPx)에서는 취소하지 않음
                                            isEdgeCandidate = false
                                        }
                                    }
                                } else if (isModeSelecting) {
                                    // 모드 선택 단계: 카드 레이아웃 방향에 맞춰 선택 축 결정
                                    // 가로 레이아웃(width >= 400dp): 좌/우 이동으로 선택
                                    // 세로 레이아웃: 위/아래 이동으로 선택
                                    val isHorizontalCardLayout = size.width >= with(density) { 400.dp.toPx() }
                                    val modeSelectDelta = if (isHorizontalCardLayout)
                                        pos.x - downPos.x
                                    else
                                        pos.y - downPos.y
                                    val modeStep = (modeSelectDelta / navStepPx).roundToInt()
                                    selectedPopupMode = if (modeStep >= 0) EdgePopupMode.SWIPE else EdgePopupMode.DIRECT_TOUCH
                                    // 진입 엣지로 되돌아오면 취소
                                    if (currentInward <= cancelThresholdPx) {
                                        isModeSelecting = false
                                        isEdgeCandidate = false
                                        pendingEdgeState = null
                                        selectedPopupMode = null
                                    }
                                } else {
                                    // 팝업 표시 중(Gesture 1): 진입 엣지로 되돌아오면 취소
                                    if (currentInward <= cancelThresholdPx) {
                                        showEdgePopup = false
                                        isModeSelecting = false
                                        isEdgeCandidate = false
                                        pendingEdgeState = null
                                        selectedItemIndex = null
                                        popupAnchorPx = Offset.Zero
                                        selectedPopupMode = null
                                    }
                                }

                                if (isEdgeCandidate) continue  // 일반 커서/스크롤 처리 건너뜀
                            }

                            // ── 스크롤 모드 분기 (NORMAL_SCROLL / INFINITE_SCROLL 공통) ──
                            if (latestState.scrollMode == ScrollMode.NORMAL_SCROLL ||
                                latestState.scrollMode == ScrollMode.INFINITE_SCROLL
                            ) {
                                val sensitivity = latestState.scrollSensitivity.multiplier
                                val effectiveUnitPx = scrollUnitPx / sensitivity

                                // 데드존 체크
                                if (!deadZoneEscaped.value) {
                                    val dist = (currentTouchPosition.value - touchDownPosition.value).getDistance()
                                    if (dist >= deadZoneThresholdPx) {
                                        deadZoneEscaped.value = true
                                    }
                                }
                                if (!deadZoneEscaped.value) continue

                                // 축 확정 로직
                                if (scrollAxis == ScrollAxis.UNDECIDED) {
                                    axisAccumX += rawDelta.x
                                    axisAccumY += rawDelta.y
                                    val accumDist = Offset(axisAccumX, axisAccumY).getDistance()

                                    if (accumDist >= axisLockDistPx) {
                                        // atan2: 0°=완전가로, 90°=완전세로
                                        val angleRad = atan2(abs(axisAccumY), abs(axisAccumX))
                                        val angleDeg = Math.toDegrees(angleRad.toDouble()).toFloat()
                                        val deadZoneDeg = SCROLL_AXIS_DIAGONAL_DEAD_ZONE_DEG

                                        scrollAxis = when {
                                            angleDeg < 45f - deadZoneDeg -> ScrollAxis.HORIZONTAL
                                            angleDeg > 45f + deadZoneDeg -> ScrollAxis.VERTICAL
                                            else -> ScrollAxis.UNDECIDED  // 대각선 구간: 계속 누적
                                        }

                                        if (scrollAxis != ScrollAxis.UNDECIDED) {
                                            // 축 확정: 누적 초기화 (확정 이전 분 버림)
                                            scrollAccum = 0f
                                            velocitySamples.clear()
                                            // 가이드라인 설정
                                            guidelineAxis = scrollAxis
                                            guidelineScrollMode = latestState.scrollMode
                                            guidelineVisible = true
                                            scheduleGuidelineHide()
                                        }
                                    }
                                }

                                // 축이 확정된 경우에만 스크롤 누적 및 전송
                                if (scrollAxis != ScrollAxis.UNDECIDED) {
                                    val axisDelta = when (scrollAxis) {
                                        ScrollAxis.VERTICAL -> rawDelta.y
                                        ScrollAxis.HORIZONTAL -> rawDelta.x
                                        ScrollAxis.UNDECIDED -> 0f
                                    }

                                    // 스크롤 단위 누적 (소수점 보존으로 단위 손실 방지)
                                    scrollAccum += axisDelta

                                    while (abs(scrollAccum) >= effectiveUnitPx) {
                                        val direction = if (scrollAccum > 0) 1 else -1
                                        scrollAccum -= direction * effectiveUnitPx

                                        // 스크롤 프레임 전송
                                        val wheelDelta = (-direction).toByte()
                                        val frame = when (scrollAxis) {
                                            ScrollAxis.VERTICAL ->
                                                ClickDetector.createWheelFrame(wheelDelta)
                                            ScrollAxis.HORIZONTAL ->
                                                ClickDetector.createHorizontalWheelFrame(wheelDelta)
                                            ScrollAxis.UNDECIDED -> continue
                                        }
                                        ClickDetector.sendFrame(frame)

                                        // 가이드라인 재표시 및 타이머 리셋
                                        guidelineVisible = true
                                        // 일반 스크롤: 단위별 스텝 이동 (무한 스크롤은 아래에서 연속 추적)
                                        if (latestState.scrollMode == ScrollMode.NORMAL_SCROLL) {
                                            guidelineTarget += direction * SCROLL_GUIDELINE_STEP_DP
                                        }

                                        // 햅틱 피드백 (일반 스크롤: 단위별 틱)
                                        if (latestState.scrollMode == ScrollMode.NORMAL_SCROLL) {
                                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                        }

                                        scheduleGuidelineHide()
                                    }

                                    // 무한 스크롤 전용: 가이드라인 연속 추적 + 속도 샘플 수집
                                    if (latestState.scrollMode == ScrollMode.INFINITE_SCROLL) {
                                        val now = System.currentTimeMillis()
                                        // dp 단위로 변환 (px / density)
                                        val axisDeltaDp = axisDelta / density.density
                                        // 가이드라인이 손가락과 함께 연속 이동
                                        guidelineTarget += axisDeltaDp
                                        velocitySamples.addLast(Pair(axisDeltaDp, now))
                                        // 윈도우 밖 오래된 샘플 제거
                                        val cutoff = now - INFINITE_SCROLL_VELOCITY_WINDOW_MS
                                        while (velocitySamples.isNotEmpty() &&
                                            velocitySamples.first().second < cutoff
                                        ) {
                                            velocitySamples.removeFirst()
                                        }
                                    }
                                }

                            } else {
                                // ── 커서 이동 모드 (기존 로직) ──
                                if (!deadZoneEscaped.value) {
                                    val dist = (currentTouchPosition.value - touchDownPosition.value).getDistance()
                                    if (dist >= deadZoneThresholdPx) {
                                        deadZoneEscaped.value = true
                                    }
                                }

                                val finalDelta = if (deadZoneEscaped.value) {
                                    DeltaCalculator.normalizeOnly(rawDelta)
                                } else {
                                    Offset.Zero
                                }

                                // ── 직각 이동 모드 축 잠금 (Phase 4.3.5) ──
                                val axisLockedDelta = if (latestState.moveMode == MoveMode.RIGHT_ANGLE &&
                                    deadZoneEscaped.value
                                ) {
                                    if (rightAngleAxis == RightAngleAxis.UNDECIDED) {
                                        // 축 판정용 누적 (rawDelta: px 단위)
                                        rightAngleAccumX += rawDelta.x
                                        rightAngleAccumY += rawDelta.y
                                        val determined = DeltaCalculator.determineRightAngleAxis(
                                            accumX = rightAngleAccumX,
                                            accumY = rightAngleAccumY,
                                            lockDistPx = rightAngleLockDistPx,
                                            deadbandDeg = RIGHT_ANGLE_DEADBAND_DEG
                                        )
                                        if (determined != RightAngleAxis.UNDECIDED) {
                                            rightAngleAxis = determined
                                            rightAngleGuidelineAxis = determined
                                            rightAngleGuidelineVisible = true
                                            // 축 결정 순간 Light 햅틱 1회
                                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                        }
                                        // 아직 미결정: 주축 확정 전까지 커서 이동 차단
                                        Offset.Zero
                                    } else {
                                        // 주축 확정 후: 반대 축 이동량 = 0
                                        DeltaCalculator.applyRightAngleLock(finalDelta, rightAngleAxis)
                                    }
                                } else {
                                    finalDelta
                                }

                                // ── 커서 속도 샘플 수집 (Phase 4.3.8 다이나믹스용) ──
                                val now = System.currentTimeMillis()
                                val distDp = rawDelta.getDistance() / density.density
                                cursorVelocitySamples.addLast(Pair(distDp, now))
                                val cutoff = now - INFINITE_SCROLL_VELOCITY_WINDOW_MS
                                while (cursorVelocitySamples.isNotEmpty() &&
                                    cursorVelocitySamples.first().second < cutoff
                                ) {
                                    cursorVelocitySamples.removeFirst()
                                }
                                val velocityDpMs = if (cursorVelocitySamples.size >= 2) {
                                    val oldest = cursorVelocitySamples.first()
                                    val newest = cursorVelocitySamples.last()
                                    val totalDp = cursorVelocitySamples.sumOf { it.first.toDouble() }.toFloat()
                                    val timeSpanMs = (newest.second - oldest.second).toFloat()
                                    if (timeSpanMs > 0f) totalDp / timeSpanMs else 0f
                                } else 0f

                                // ── DPI 곱수 적용 (Phase 4.3.6) ──
                                val dpiMultiplierMove = latestState.effectiveDpiMultiplier
                                val dpiDeltaRaw = Offset(
                                    axisLockedDelta.x * dpiMultiplierMove,
                                    axisLockedDelta.y * dpiMultiplierMove
                                )

                                // ── 포인터 다이나믹스 배율 적용 (Phase 4.3.8) ──
                                val dynamicsPreset = DYNAMICS_PRESETS.getOrNull(latestState.dynamicsPresetIndex)
                                    ?: DYNAMICS_PRESETS.first()
                                val dpiDelta = if (dynamicsPreset.algorithm != com.bridgeone.app.ui.components.touchpad.DynamicsAlgorithm.NONE) {
                                    Offset(
                                        DeltaCalculator.applyPointerDynamics(dpiDeltaRaw.x, velocityDpMs, dynamicsPreset).coerceIn(-127f, 127f),
                                        DeltaCalculator.applyPointerDynamics(dpiDeltaRaw.y, velocityDpMs, dynamicsPreset).coerceIn(-127f, 127f)
                                    )
                                } else {
                                    Offset(
                                        dpiDeltaRaw.x.coerceIn(-127f, 127f),
                                        dpiDeltaRaw.y.coerceIn(-127f, 127f)
                                    )
                                }

                                compensatedDeltaX.value = dpiDelta.x
                                compensatedDeltaY.value = dpiDelta.y

                                if (dpiDelta.x != 0f || dpiDelta.y != 0f) {
                                    val dragFrame = ClickDetector.createFrame(
                                        buttonState = 0x00u,
                                        deltaX = dpiDelta.x,
                                        deltaY = dpiDelta.y
                                    )
                                    ClickDetector.sendFrame(dragFrame)
                                }
                            }
                        }

                        // 무한 스크롤: 속도 비례 연속 진동 (MOVE 이벤트당 1회)
                        if (latestState.scrollMode == ScrollMode.INFINITE_SCROLL &&
                            scrollAxis != ScrollAxis.UNDECIDED &&
                            velocitySamples.size >= 2 &&
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        ) {
                            val oldest = velocitySamples.first()
                            val newest = velocitySamples.last()
                            val totalDp = velocitySamples.sumOf { it.first.toDouble() }.toFloat()
                            val timeSpanMs = (newest.second - oldest.second).toFloat()
                            val speed = if (timeSpanMs > 0f) abs(totalDp / timeSpanMs) else 0f
                            val amplitude = (speed / INFINITE_SCROLL_HAPTIC_MAX_VELOCITY_DP_MS * 255)
                                .toInt().coerceIn(1, 255)
                            vibrator.vibrate(VibrationEffect.createOneShot(20, amplitude))
                        }

                        onTouchEvent(PointerEventType.Move, currentTouchPosition.value, previousTouchPosition.value)
                        moveEvent = awaitPointerEvent()
                    }

                    // ── UP ──
                    if (moveEvent.type == PointerEventType.Release) {
                        previousTouchPosition.value = currentTouchPosition.value
                        currentTouchPosition.value = moveEvent.changes.first().position

                        if (isModeSelecting) {
                            // ── 모드 선택 단계에서 손 뗌 → 항상 확정 (항상 모드 선택 상태) ──
                            val confirmedMode = selectedPopupMode ?: EdgePopupMode.SWIPE
                            isModeSelecting = false
                            showEdgePopup = true
                            selectedItemIndex = if (confirmedMode == EdgePopupMode.SWIPE) 0 else null
                            popupAnchorPx = Offset.Zero  // 앵커는 다음 탭에서 설정
                            isEdgeCandidate = false
                            fingerAlongEdgePx = 0f
                            inwardDistancePx = 0f
                        } else if (showEdgePopup) {
                            // ── 팝업 열린 채로 손 뗌 → 팝업 유지 ──
                            isEdgeCandidate = false
                            fingerAlongEdgePx = 0f
                            inwardDistancePx = 0f
                        } else if (latestState.scrollMode == ScrollMode.NORMAL_SCROLL ||
                            latestState.scrollMode == ScrollMode.INFINITE_SCROLL
                        ) {
                            if (!deadZoneEscaped.value) {
                                // 탭 판정 → 원탭으로 스크롤 종료
                                // NORMAL_SCROLL: 현재 제스처에 드래그 없음 → 그냥 해제
                                // INFINITE_SCROLL: 관성 진행 중 탭이었으면 차단 (inertiaWasActiveOnDown)
                                val isInertiaInterruption =
                                    latestState.scrollMode == ScrollMode.INFINITE_SCROLL &&
                                    inertiaWasActiveOnDown
                                if (!isInertiaInterruption) {
                                    latestOnStateChange(
                                        latestState.copy(
                                            scrollMode = ScrollMode.OFF,
                                            lastScrollMode = latestState.scrollMode
                                        )
                                    )
                                    guidelineVisible = false
                                    guidelineHideJob?.cancel()
                                    inertiaJob?.cancel()
                                    inertiaJob = null
                                }
                            } else if (latestState.scrollMode == ScrollMode.INFINITE_SCROLL &&
                                scrollAxis != ScrollAxis.UNDECIDED
                            ) {
                                // 무한 스크롤 손가락 릴리즈 → 관성 시작 (Phase 4.3.4)
                                val capturedAxis = scrollAxis
                                val capturedSensitivity = latestState.scrollSensitivity.multiplier

                                // 속도 샘플에서 초기 속도 계산 (dp/ms, 부호 포함)
                                val initialVelocity = if (velocitySamples.size >= 2) {
                                    val oldest = velocitySamples.first()
                                    val newest = velocitySamples.last()
                                    val totalDp = velocitySamples.sumOf { it.first.toDouble() }.toFloat()
                                    val timeSpanMs = (newest.second - oldest.second).toFloat()
                                    if (timeSpanMs > 0f) totalDp / timeSpanMs else 0f
                                } else if (velocitySamples.size == 1) {
                                    velocitySamples.first().first / 16f  // 1프레임(16ms) 기준
                                } else {
                                    0f
                                }

                                if (abs(initialVelocity) > INFINITE_SCROLL_MIN_VELOCITY_DP_MS) {
                                    val capturedScrollUnitDp = SCROLL_UNIT_DISTANCE_DP

                                    inertiaJob = coroutineScope.launch {
                                        var velocity = initialVelocity
                                        var inertiaScrollAccum = 0f
                                        var lastTimestamp = System.currentTimeMillis()
                                        val effectiveUnitDp = capturedScrollUnitDp / capturedSensitivity

                                        while (abs(velocity) > INFINITE_SCROLL_MIN_VELOCITY_DP_MS) {
                                            delay(16L)  // ~60fps

                                            val now = System.currentTimeMillis()
                                            val dt = (now - lastTimestamp).toFloat()
                                            lastTimestamp = now

                                            // 지수 감쇠: v(t) = v0 * e^(-dt/τ)
                                            velocity *= exp(-dt / INFINITE_SCROLL_TIME_CONSTANT_MS)

                                            // 속도 비례 연속 진동 (매 프레임)
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                val amplitude = (abs(velocity) / INFINITE_SCROLL_HAPTIC_MAX_VELOCITY_DP_MS * 255)
                                                    .toInt().coerceIn(1, 255)
                                                vibrator.vibrate(VibrationEffect.createOneShot(20, amplitude))
                                            }

                                            // 이동량 누적 (dp 단위, velocity * dt = dp)
                                            val moveDp = velocity * dt
                                            inertiaScrollAccum += moveDp
                                            // 가이드라인 연속 추적
                                            guidelineTarget += moveDp

                                            while (abs(inertiaScrollAccum) >= effectiveUnitDp) {
                                                val dir = if (inertiaScrollAccum > 0) 1 else -1
                                                inertiaScrollAccum -= dir * effectiveUnitDp

                                                val wheelDelta = (-dir).toByte()
                                                val frame = when (capturedAxis) {
                                                    ScrollAxis.VERTICAL ->
                                                        ClickDetector.createWheelFrame(wheelDelta)
                                                    ScrollAxis.HORIZONTAL ->
                                                        ClickDetector.createHorizontalWheelFrame(wheelDelta)
                                                    ScrollAxis.UNDECIDED -> break
                                                }
                                                ClickDetector.sendFrame(frame)

                                                guidelineVisible = true
                                                scheduleGuidelineHide()
                                            }
                                        }

                                        // 관성 종료: 숨김 스케줄
                                        scheduleGuidelineHide()
                                    }
                                }
                            }
                        } else {
                            // 커서 이동 모드: 기존 클릭/드래그 로직
                            val releaseDelta = DeltaCalculator.calculateDelta(
                                previousTouchPosition.value,
                                currentTouchPosition.value
                            )
                            val releaseFinalDelta = if (deadZoneEscaped.value) {
                                DeltaCalculator.normalizeOnly(releaseDelta)
                            } else {
                                Offset.Zero
                            }

                            // 직각 이동 모드 축 잠금 적용 (Phase 4.3.5)
                            val releaseAxisLockedDelta = if (latestState.moveMode == MoveMode.RIGHT_ANGLE &&
                                rightAngleAxis != RightAngleAxis.UNDECIDED
                            ) {
                                DeltaCalculator.applyRightAngleLock(releaseFinalDelta, rightAngleAxis)
                            } else {
                                releaseFinalDelta
                            }

                            // ── DPI 곱수 적용 (Phase 4.3.6) ──
                            val dpiMultiplierRelease = latestState.effectiveDpiMultiplier
                            val releaseDpiDeltaRaw = Offset(
                                releaseAxisLockedDelta.x * dpiMultiplierRelease,
                                releaseAxisLockedDelta.y * dpiMultiplierRelease
                            )

                            // ── 포인터 다이나믹스 배율 적용 (Phase 4.3.8) ──
                            val releaseVelocityDpMs = if (cursorVelocitySamples.size >= 2) {
                                val oldest = cursorVelocitySamples.first()
                                val newest = cursorVelocitySamples.last()
                                val totalDp = cursorVelocitySamples.sumOf { it.first.toDouble() }.toFloat()
                                val timeSpanMs = (newest.second - oldest.second).toFloat()
                                if (timeSpanMs > 0f) totalDp / timeSpanMs else 0f
                            } else 0f
                            val releaseDynamicsPreset = DYNAMICS_PRESETS.getOrNull(latestState.dynamicsPresetIndex)
                                ?: DYNAMICS_PRESETS.first()
                            val releaseDpiDelta = if (releaseDynamicsPreset.algorithm != com.bridgeone.app.ui.components.touchpad.DynamicsAlgorithm.NONE) {
                                Offset(
                                    DeltaCalculator.applyPointerDynamics(releaseDpiDeltaRaw.x, releaseVelocityDpMs, releaseDynamicsPreset).coerceIn(-127f, 127f),
                                    DeltaCalculator.applyPointerDynamics(releaseDpiDeltaRaw.y, releaseVelocityDpMs, releaseDynamicsPreset).coerceIn(-127f, 127f)
                                )
                            } else {
                                Offset(
                                    releaseDpiDeltaRaw.x.coerceIn(-127f, 127f),
                                    releaseDpiDeltaRaw.y.coerceIn(-127f, 127f)
                                )
                            }
                            compensatedDeltaX.value = releaseDpiDelta.x
                            compensatedDeltaY.value = releaseDpiDelta.y

                            val buttonState = if (deadZoneEscaped.value) {
                                0x00u.toUByte()
                            } else {
                                val pressDuration = System.currentTimeMillis() - touchDownTime.value
                                val movement = (currentTouchPosition.value - touchDownPosition.value).getDistance()
                                val detected = ClickDetector.detectClick(pressDuration, movement)
                                if (bridgeMode == BridgeMode.ESSENTIAL && detected == 0x02u.toUByte()) {
                                    0x01u.toUByte()
                                } else {
                                    detected
                                }
                            }

                            val frame = ClickDetector.createFrame(
                                buttonState = buttonState,
                                deltaX = compensatedDeltaX.value,
                                deltaY = compensatedDeltaY.value
                            )
                            ClickDetector.sendFrame(frame)

                            if (buttonState != 0x00u.toUByte()) {
                                val releaseFrame = ClickDetector.createFrame(
                                    buttonState = 0x00u.toUByte(),
                                    deltaX = 0f,
                                    deltaY = 0f
                                )
                                ClickDetector.sendFrame(releaseFrame)
                            }
                        }

                        // 직각 이동 가이드라인 숨김 (Phase 4.3.5)
                        rightAngleGuidelineVisible = false

                        // 엣지 후보 상태에서 팝업 없이 손 뗌 → 후보 취소 (산봉우리 수축 트리거)
                        if (isEdgeCandidate && !showEdgePopup) {
                            isEdgeCandidate = false
                            fingerAlongEdgePx = 0f
                            inwardDistancePx = 0f
                        }

                        // 공통 상태 초기화
                        touchDownTime.value = 0L
                        touchDownPosition.value = Offset.Zero
                        compensatedDeltaX.value = 0f
                        compensatedDeltaY.value = 0f
                        deadZoneEscaped.value = false

                        onTouchEvent(PointerEventType.Release, currentTouchPosition.value, previousTouchPosition.value)
                    }
                }
            }
    ) {
        // 직각 이동 가이드라인 오버레이 (Phase 4.3.5)
        RightAngleGuideline(
            isVisible = rightAngleGuidelineVisible,
            axis = rightAngleGuidelineAxis,
            modifier = Modifier.fillMaxSize()
        )

        // ScrollGuideline 오버레이 (스크롤 모드에서만 유의미)
        ScrollGuideline(
            isVisible = guidelineVisible,
            scrollAxis = guidelineAxis,
            targetOffset = guidelineTarget,
            scrollMode = guidelineScrollMode,
            modifier = Modifier.fillMaxSize()
        )

        // 일반 스크롤 버튼 (NORMAL_SCROLL 모드에서만 표시)
        AnimatedVisibility(
            visible = touchpadState.scrollMode == ScrollMode.NORMAL_SCROLL,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 8.dp, bottom = 8.dp)
        ) {
            NormalScrollButtons()
        }

        // 다이나믹스 프리셋 버튼 (커서 이동 모드 + Standard 모드에서만 표시, Phase 4.3.8)
        AnimatedVisibility(
            visible = touchpadState.scrollMode == ScrollMode.OFF &&
                bridgeMode != BridgeMode.ESSENTIAL,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 8.dp, bottom = 8.dp)
        ) {
            DynamicsPresetButton(
                touchpadState = touchpadState,
                onTouchpadStateChange = onTouchpadStateChange,
                onLongPress = onDynamicsLongPress,
                showLabel = showPresetLabel
            )
        }

        // 모드 프리셋 버튼 (Standard 모드에서 항상 표시, Phase 4.4.8)
        if (bridgeMode != BridgeMode.ESSENTIAL) {
            ModePresetButton(
                touchpadState = touchpadState,
                onTouchpadStateChange = onTouchpadStateChange,
                onLongPress = onModePresetLongPress,
                showLabel = showModePresetLabel,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 8.dp, bottom = 8.dp)
            )
        }

        // 산봉우리 오버레이 (Phase 4.4.6)
        // 드래그 중: lastBump*(nearest edge 기준) 사용 / 수축 중: Animatable 값 사용
        val effectiveBumpEdge = when {
            isEdgeCandidate || lastBumpInwardPx > 0f || isBumpShrinking -> lastBumpEntryEdge
            else -> null
        }
        val effectiveBumpInward = when {
            isBumpShrinking -> bumpShrinkAnimatable.value
            else -> lastBumpInwardPx  // 드래그 중 및 gap 프레임 모두 커버
        }
        val effectiveBumpAlong = lastBumpAlongPx
        if (effectiveBumpEdge != null && effectiveBumpInward > 0f) {
            val maxPeakPx = with(density) { EdgeSwipeConstants.MAX_PEAK_HEIGHT_DP.dp.toPx() }
            val baseHalfPx = with(density) { EdgeSwipeConstants.BUMP_BASE_HALF_SIZE_DP.dp.toPx() }
            val strokePx = with(density) { EdgeSwipeConstants.BUMP_STROKE_WIDTH_DP.dp.toPx() }
            val glowPx = with(density) { EdgeSwipeConstants.BUMP_GLOW_RADIUS_DP.dp.toPx() }
            val glowMaxPx = with(density) { EdgeSwipeConstants.BUMP_GLOW_MAX_RADIUS_DP.dp.toPx() }
            EdgeBumpOverlay(
                entryEdge = effectiveBumpEdge,
                fingerAlongEdgePx = effectiveBumpAlong,
                inwardDistancePx = effectiveBumpInward,
                maxPeakHeightPx = maxPeakPx,
                baseHalfSizePx = baseHalfPx,
                strokeWidthPx = strokePx,
                glowRadiusPx = glowPx,
                glowMaxRadiusPx = glowMaxPx,
                borderColors = animatedLeftColor to animatedRightColor,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 엣지 스와이프 팝업 오버레이 (Phase 4.3.12) — 최상단 레이어
        EdgeSwipeOverlay(
            visible = showEdgePopup,
            pendingState = pendingEdgeState ?: touchpadState,
            config = config,
            selectedIndex = selectedItemIndex,
            popupAnchorPx = popupAnchorPx,
            isModeSelecting = isModeSelecting,
            selectedPopupMode = selectedPopupMode,
            isEdgeCandidate = isEdgeCandidate,
            entryEdge = entryEdge,
            fingerAlongEdgePx = fingerAlongEdgePx,
            inwardDistancePx = inwardDistancePx,
            modifier = Modifier.fillMaxSize()
        )
        }  // inner Box 끝

    }  // outer Box 끝
}

// ============================================================
// 엣지 스와이프 헬퍼 함수 (Phase 4.3.12)
// ============================================================

/**
 * 터치 시작점([pos])이 어느 가장자리 영역에 속하는지 반환합니다.
 * 코너는 LEFT 우선(순서상 첫 번째 매칭)으로 처리됩니다.
 */
private fun detectEntryEdge(
    pos: androidx.compose.ui.geometry.Offset,
    width: Float,
    height: Float,
    edgeWidthPx: Float
): EntryEdge? = when {
    pos.x < edgeWidthPx          -> EntryEdge.LEFT
    pos.x > width - edgeWidthPx  -> EntryEdge.RIGHT
    pos.y < edgeWidthPx          -> EntryEdge.TOP
    pos.y > height - edgeWidthPx -> EntryEdge.BOTTOM
    else                         -> null
}

/**
 * [edge] 방향 기준으로, 현재 손가락 위치[pos]가 가장자리에서 안쪽으로 얼마나 들어왔는지(px)를 반환합니다.
 * 값이 클수록 안쪽, 0에 가까울수록 가장자리에 있는 것입니다.
 */
private fun getInwardDistance(
    pos: androidx.compose.ui.geometry.Offset,
    edge: EntryEdge?,
    width: Float,
    height: Float
): Float = when (edge) {
    EntryEdge.LEFT   -> pos.x
    EntryEdge.RIGHT  -> width - pos.x
    EntryEdge.TOP    -> pos.y
    EntryEdge.BOTTOM -> height - pos.y
    null             -> 0f
}

/**
 * [edge] 방향 기준으로, 손가락의 엣지 축(진입 방향에 수직인 축) 위치(px)를 반환합니다.
 * LEFT/RIGHT 엣지 → y 좌표, TOP/BOTTOM 엣지 → x 좌표
 */
private fun getAlongEdgePosition(
    pos: androidx.compose.ui.geometry.Offset,
    edge: EntryEdge?
): Float = when (edge) {
    EntryEdge.LEFT, EntryEdge.RIGHT  -> pos.y
    EntryEdge.TOP, EntryEdge.BOTTOM  -> pos.x
    null                             -> 0f
}

/**
 * [pos] 에서 가장 가까운 엣지를 반환합니다.
 * 산봉우리 시각화에 사용되며, 제스처 로직과는 무관합니다.
 */
private fun findNearestEdge(
    pos: androidx.compose.ui.geometry.Offset,
    width: Float,
    height: Float
): EntryEdge {
    val fromLeft   = pos.x
    val fromRight  = width - pos.x
    val fromTop    = pos.y
    val fromBottom = height - pos.y
    return when (minOf(fromLeft, fromRight, fromTop, fromBottom)) {
        fromLeft   -> EntryEdge.LEFT
        fromRight  -> EntryEdge.RIGHT
        fromTop    -> EntryEdge.TOP
        else       -> EntryEdge.BOTTOM
    }
}

/**
 * 직접 터치 모드에서 버튼 영역(Rect) 리스트를 계산합니다.
 * 인덱스 0..<modeCount = 모드 버튼, modeCount = 확인 버튼.
 * 2열 그리드 배치이며 앵커를 중심으로 하되, 터치패드 경계 안에 clamping합니다.
 */
private fun computeDirectTouchButtonRects(
    anchorPx: Offset,
    containerWidth: Float,
    containerHeight: Float,
    modeCount: Int,
    buttonSizePx: Float,
    gapPx: Float,
    density: androidx.compose.ui.unit.Density
): List<Rect> {
    val cols = if (modeCount <= 1) 1 else 2
    val modeRows = (modeCount + cols - 1) / cols
    val confirmHeightPx = density.run { EdgeSwipeConstants.EDGE_POPUP_DIRECT_CONFIRM_HEIGHT_DP.dp.toPx() }

    val gridW = cols * buttonSizePx + (cols - 1) * gapPx
    val gridH = modeRows * buttonSizePx + modeRows * gapPx + confirmHeightPx

    val gridLeft = (anchorPx.x - gridW / 2).coerceIn(0f, (containerWidth - gridW).coerceAtLeast(0f))
    val gridTop = (anchorPx.y - gridH / 2).coerceIn(0f, (containerHeight - gridH).coerceAtLeast(0f))

    val rects = mutableListOf<Rect>()
    for (i in 0 until modeCount) {
        val row = i / cols
        val col = i % cols
        val x = gridLeft + col * (buttonSizePx + gapPx)
        val y = gridTop + row * (buttonSizePx + gapPx)
        rects.add(Rect(x, y, x + buttonSizePx, y + buttonSizePx))
    }
    // 확인 버튼: 마지막 행, 그리드 가로 중앙, 높이만 줄인 직사각형
    val confirmX = gridLeft + (gridW - buttonSizePx) / 2
    val confirmY = gridTop + modeRows * (buttonSizePx + gapPx)
    rects.add(Rect(confirmX, confirmY, confirmX + buttonSizePx, confirmY + confirmHeightPx))

    return rects
}

/**
 * 엣지 스와이프로 [mode]를 토글한 새로운 [TouchpadState]를 반환합니다.
 */
private fun applyEdgeModeToggle(state: TouchpadState, mode: EdgeSwipeMode): TouchpadState = when (mode) {
    EdgeSwipeMode.SCROLL -> when (state.scrollMode) {
        ScrollMode.OFF             -> state.copy(
            scrollMode = ScrollMode.NORMAL_SCROLL,
            lastScrollMode = ScrollMode.NORMAL_SCROLL,
            customDpiMultiplier = null
        )
        ScrollMode.NORMAL_SCROLL   -> state.copy(
            scrollMode = ScrollMode.INFINITE_SCROLL,
            lastScrollMode = ScrollMode.INFINITE_SCROLL
        )
        ScrollMode.INFINITE_SCROLL -> state.copy(
            scrollMode = ScrollMode.OFF,
            lastScrollMode = ScrollMode.NORMAL_SCROLL  // OFF 후 다음 활성화는 항상 NORMAL_SCROLL
        )
    }
    EdgeSwipeMode.CLICK -> state.copy(
        clickMode = if (state.clickMode == ClickMode.LEFT_CLICK) ClickMode.RIGHT_CLICK else ClickMode.LEFT_CLICK
    )
    EdgeSwipeMode.MOVE -> state.copy(
        moveMode = if (state.moveMode == MoveMode.FREE) MoveMode.RIGHT_ANGLE else MoveMode.FREE
    )
    EdgeSwipeMode.CURSOR -> state.copy(
        cursorMode = if (state.cursorMode == CursorMode.SINGLE) CursorMode.MULTI else CursorMode.SINGLE
    )
}

/**
 * TouchpadWrapper Preview
 */
@Preview(
    showBackground = true,
    backgroundColor = 0xFF0D0D0D,
    widthDp = 200,
    heightDp = 400
)
@Composable
fun TouchpadWrapperPreview() {
    TouchpadWrapper(
        modifier = Modifier
            .size(160.dp, 320.dp)
            .background(Color(0xFF0D0D0D))
    )
}
