package com.bridgeone.app.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.bridgeone.app.protocol.BridgeMode
import com.bridgeone.app.ui.common.ScrollConstants.DOUBLE_TAP_MAX_INTERVAL_MS
import com.bridgeone.app.ui.common.ScrollConstants.SCROLL_AXIS_DIAGONAL_DEAD_ZONE_DEG
import com.bridgeone.app.ui.common.ScrollConstants.SCROLL_AXIS_LOCK_DISTANCE_DP
import com.bridgeone.app.ui.common.ScrollConstants.SCROLL_GUIDELINE_HIDE_DELAY_MS
import com.bridgeone.app.ui.common.ScrollConstants.SCROLL_GUIDELINE_STEP_DP
import com.bridgeone.app.ui.common.ScrollConstants.SCROLL_STOP_THRESHOLD_MS
import com.bridgeone.app.ui.common.ScrollConstants.SCROLL_UNIT_DISTANCE_DP
import com.bridgeone.app.ui.components.touchpad.ScrollAxis
import com.bridgeone.app.ui.components.touchpad.ScrollGuideline
import com.bridgeone.app.ui.components.touchpad.ScrollMode
import com.bridgeone.app.ui.components.touchpad.TouchpadState
import com.bridgeone.app.ui.utils.ClickDetector
import com.bridgeone.app.ui.utils.DeltaCalculator
import com.bridgeone.app.ui.utils.getDistance
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.atan2

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
 * **스크롤 축 확정 알고리즘:**
 * 1. DOWN 시 scrollAxis = UNDECIDED, 누적 벡터 초기화
 * 2. MOVE 중 누적 이동이 [SCROLL_AXIS_LOCK_DISTANCE_DP] 초과 시 축 판정:
 *    - atan2(|ΔY|, |ΔX|) 기준: 45° 근방 ±15° = 대각선(UNDECIDED 유지)
 *    - 0°~30° = HORIZONTAL, 60°~90° = VERTICAL
 * 3. 축 확정 후 해당 축으로만 스크롤 프레임 전송 (UP까지 유지)
 *
 * **더블탭으로 스크롤 종료:**
 * 스크롤 모드에서 드래그 없는 탭 두 번 → [onTouchpadStateChange] 호출, scrollMode = OFF
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
    onTouchEvent: (
        eventType: PointerEventType,
        currentPosition: Offset,
        previousPosition: Offset
    ) -> Unit = { _, _, _ -> }
) {
    val density = LocalDensity.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()

    // rememberUpdatedState: pointerInput(Unit) 제스처 루프 재시작 없이 최신 상태 참조
    val latestState by rememberUpdatedState(touchpadState)
    val latestOnStateChange by rememberUpdatedState(onTouchpadStateChange)

    // ── 커서 모드 상태 (기존) ──
    val currentTouchPosition = remember { mutableStateOf(Offset.Zero) }
    val previousTouchPosition = remember { mutableStateOf(Offset.Zero) }
    val touchDownTime = remember { mutableStateOf(0L) }
    val touchDownPosition = remember { mutableStateOf(Offset.Zero) }
    val compensatedDeltaX = remember { mutableStateOf(0f) }
    val compensatedDeltaY = remember { mutableStateOf(0f) }
    val deadZoneEscaped = remember { mutableStateOf(false) }

    // ── 스크롤 모드 상태 (Phase 4.3.3) ──
    // 더블탭 감지: 스크롤 모드에서 연속 탭으로 종료
    var lastScrollTapTime by remember { mutableStateOf(0L) }

    // ScrollGuideline 표시 상태
    var guidelineVisible by remember { mutableStateOf(false) }
    var guidelineAxis by remember { mutableStateOf(ScrollAxis.VERTICAL) }
    var guidelineTarget by remember { mutableFloatStateOf(0f) }

    // 가이드라인 자동 숨김 Job
    var guidelineHideJob by remember { mutableStateOf<Job?>(null) }

    // 가이드라인 숨김 스케줄러
    fun scheduleGuidelineHide() {
        guidelineHideJob?.cancel()
        guidelineHideJob = coroutineScope.launch {
            delay(SCROLL_STOP_THRESHOLD_MS + SCROLL_GUIDELINE_HIDE_DELAY_MS)
            guidelineVisible = false
        }
    }

    val CORNER_RADIUS = 5.dp

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(CORNER_RADIUS))
            .background(Color(0xFF1A1A1A))
            .pointerInput(Unit) {
                // dp → px 변환 상수 (제스처 루프 시작 전 1회 계산)
                val axisLockDistPx = density.run { SCROLL_AXIS_LOCK_DISTANCE_DP.dp.toPx() }
                val scrollUnitPx = density.run { SCROLL_UNIT_DISTANCE_DP.dp.toPx() }
                val deadZoneThresholdPx = DeltaCalculator.getDeadZoneThresholdPx(density)

                awaitEachGesture {
                    // ── DOWN ──
                    val down = awaitPointerEvent()
                    if (down.type != PointerEventType.Press) return@awaitEachGesture
                    down.changes.forEach { it.consume() }

                    currentTouchPosition.value = down.changes.first().position
                    previousTouchPosition.value = currentTouchPosition.value
                    touchDownTime.value = System.currentTimeMillis()
                    touchDownPosition.value = currentTouchPosition.value
                    compensatedDeltaX.value = 0f
                    compensatedDeltaY.value = 0f
                    deadZoneEscaped.value = false

                    // 스크롤 모드 전용 로컬 상태 (각 제스처마다 초기화)
                    var scrollAxis = ScrollAxis.UNDECIDED
                    var axisAccumX = 0f   // 축 확정용 누적 X (px)
                    var axisAccumY = 0f   // 축 확정용 누적 Y (px)
                    var scrollAccum = 0f  // 스크롤 단위 누적 (소수점 보존)

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

                            // ── 스크롤 모드 분기 ──
                            if (latestState.scrollMode == ScrollMode.NORMAL_SCROLL) {
                                val sensitivity = latestState.scrollSensitivity.multiplier
                                val effectiveUnitPx = scrollUnitPx / sensitivity

                                // 데드존 체크 (스크롤 모드에서도 동일 로직 재활용)
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
                                            // 가이드라인 설정
                                            guidelineAxis = scrollAxis
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
                                    // scrollAccum 양수 = 아래/오른쪽 방향
                                    scrollAccum += axisDelta

                                    while (abs(scrollAccum) >= effectiveUnitPx) {
                                        val direction = if (scrollAccum > 0) 1 else -1
                                        scrollAccum -= direction * effectiveUnitPx

                                        // 스크롤 프레임 전송
                                        // 손가락 아래 → 화면 내려감 → 음수 휠 (클래식 스크롤 방향)
                                        val wheelDelta = (-direction).toByte()
                                        val frame = when (scrollAxis) {
                                            ScrollAxis.VERTICAL ->
                                                ClickDetector.createWheelFrame(wheelDelta)
                                            ScrollAxis.HORIZONTAL ->
                                                ClickDetector.createHorizontalWheelFrame(wheelDelta)
                                            ScrollAxis.UNDECIDED -> continue
                                        }
                                        ClickDetector.sendFrame(frame)

                                        // 가이드라인 재표시 (타이머로 숨겨진 후 스크롤 재개 시)
                                        guidelineVisible = true
                                        guidelineTarget += direction * SCROLL_GUIDELINE_STEP_DP

                                        // 햅틱 피드백 (스크롤 틱: 가벼운 CLOCK_TICK)
                                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)

                                        // 가이드라인 숨김 타이머 리셋
                                        scheduleGuidelineHide()
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

                                compensatedDeltaX.value = finalDelta.x
                                compensatedDeltaY.value = finalDelta.y

                                if (finalDelta.x != 0f || finalDelta.y != 0f) {
                                    val dragFrame = ClickDetector.createFrame(
                                        buttonState = 0x00u,
                                        deltaX = finalDelta.x,
                                        deltaY = finalDelta.y
                                    )
                                    ClickDetector.sendFrame(dragFrame)
                                }
                            }
                        }

                        onTouchEvent(PointerEventType.Move, currentTouchPosition.value, previousTouchPosition.value)
                        moveEvent = awaitPointerEvent()
                    }

                    // ── UP ──
                    if (moveEvent.type == PointerEventType.Release) {
                        previousTouchPosition.value = currentTouchPosition.value
                        currentTouchPosition.value = moveEvent.changes.first().position

                        if (latestState.scrollMode == ScrollMode.NORMAL_SCROLL) {
                            // 스크롤 모드: 탭 판정 → 더블탭으로 스크롤 종료
                            if (!deadZoneEscaped.value) {
                                val now = System.currentTimeMillis()
                                if (now - lastScrollTapTime <= DOUBLE_TAP_MAX_INTERVAL_MS) {
                                    // 더블탭 감지 → 스크롤 모드 종료
                                    latestOnStateChange(
                                        latestState.copy(
                                            scrollMode = ScrollMode.OFF,
                                            lastScrollMode = latestState.scrollMode
                                        )
                                    )
                                    guidelineVisible = false
                                    guidelineHideJob?.cancel()
                                    lastScrollTapTime = 0L
                                } else {
                                    lastScrollTapTime = now
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

                            compensatedDeltaX.value = releaseFinalDelta.x
                            compensatedDeltaY.value = releaseFinalDelta.y

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
        // ScrollGuideline 오버레이 (스크롤 모드에서만 유의미)
        ScrollGuideline(
            isVisible = guidelineVisible,
            scrollAxis = guidelineAxis,
            targetOffset = guidelineTarget,
            modifier = Modifier.fillMaxSize()
        )
    }
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
