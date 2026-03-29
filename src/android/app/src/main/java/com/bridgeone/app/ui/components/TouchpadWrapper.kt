package com.bridgeone.app.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
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
import com.bridgeone.app.ui.common.ScrollConstants.INFINITE_SCROLL_HAPTIC_MAX_VELOCITY_DP_MS
import com.bridgeone.app.ui.common.ScrollConstants.INFINITE_SCROLL_MIN_VELOCITY_DP_MS
import com.bridgeone.app.ui.common.ScrollConstants.INFINITE_SCROLL_TIME_CONSTANT_MS
import com.bridgeone.app.ui.common.ScrollConstants.INFINITE_SCROLL_VELOCITY_WINDOW_MS
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
import kotlin.math.exp

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
    @Suppress("DEPRECATION")
    val vibrator = remember { view.context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator }

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
    var guidelineScrollMode by remember { mutableStateOf(ScrollMode.NORMAL_SCROLL) }

    // 가이드라인 자동 숨김 Job
    var guidelineHideJob by remember { mutableStateOf<Job?>(null) }

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

                    // 관성 중 터치 → 즉시 관성 정지 (Phase 4.3.4)
                    inertiaJob?.cancel()
                    inertiaJob = null

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

                    // 무한 스크롤 속도 샘플 (Phase 4.3.4)
                    // 각 샘플: Pair(이동량 dp, 타임스탬프 ms)
                    val velocitySamples = ArrayDeque<Pair<Float, Long>>()

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

                        if (latestState.scrollMode == ScrollMode.NORMAL_SCROLL ||
                            latestState.scrollMode == ScrollMode.INFINITE_SCROLL
                        ) {
                            if (!deadZoneEscaped.value) {
                                // 탭 판정 → 더블탭으로 스크롤 종료
                                val now = System.currentTimeMillis()
                                if (now - lastScrollTapTime <= DOUBLE_TAP_MAX_INTERVAL_MS) {
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
                                    lastScrollTapTime = 0L
                                } else {
                                    lastScrollTapTime = now
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
            scrollMode = guidelineScrollMode,
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
