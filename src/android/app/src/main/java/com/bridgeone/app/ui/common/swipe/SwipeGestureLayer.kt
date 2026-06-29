package com.bridgeone.app.ui.common.swipe

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs

/**
 * 화면 최상단에 transparent 오버레이로 깔리는 SWIPE 모드 입력 감지 레이어.
 *
 * 동작 분류:
 * - 이동 거리 < touchSlop → 탭 → 즉시 처리
 *   - SELECTION 모드: [SwipeFocusController.activate] 즉시 호출
 *   - MANIPULATION 모드: 실제 조작 없이 손가락만 댔다 뗀 경우 → [SwipeFocusController.exitManipulation]
 * - 이동 거리 > touchSlop → 스와이프로 분류
 *   - SELECTION 모드: 매 move 이벤트의 x/y delta를 accumulator로 추적 → [SwipeFocusController.moveFocus]
 *   - MANIPULATION 모드: 매 move 이벤트의 가로 delta를 [SwipeFocusController.manipulate]에 즉시 전달 (연속 조작)
 *
 * MANIPULATION 모드 종료:
 * - touchSlop 미만 이동(진짜 탭)으로 손가락을 뗐을 때 → 즉시 exitManipulation()
 * - touchSlop 미만이더라도 실제 manipulate()가 발화됐으면 탭으로 보지 않아 모드 유지
 *
 * 이 레이어는 [Modifier.matchParentSize] 또는 fillMaxSize로 부모 Box의 zIndex 최상단에 배치할 것.
 * 키보드 활성 등 다른 시스템에 입력을 양보해야 할 때는 이 컴포저블 자체를 미렌더(`if` 분기)로 제거하는 것이 가장 안전.
 */
@Composable
fun SwipeGestureLayer(
    controller: SwipeFocusController,
    modifier: Modifier = Modifier,
    /**
     * 롱프레스(제자리 길게 누름) 콜백. null이면 롱프레스 감지를 하지 않아 기존 동작과 동일.
     * true 반환 시 해당 제스처를 소비(탭/스와이프로 처리하지 않음), false면 무시하고 기존 흐름 유지.
     */
    onLongPress: (() -> Boolean)? = null,
) {
    val viewConfig = LocalViewConfiguration.current
    val longPressTimeoutMs = viewConfig.longPressTimeoutMillis
    val stepPx = LocalDensity.current.density * SWIPE_SELECTION_STEP_DP
    val haptic = LocalHapticFeedback.current
    val currentOnLongPress by rememberUpdatedState(onLongPress)

    Box(
        modifier = modifier.pointerInput(controller, stepPx) {
            val touchSlop = viewConfiguration.touchSlop
            val swipeMinDistance = touchSlop * SWIPE_MIN_DISTANCE_MULTIPLIER

            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val downPos = down.position
                var lastPos = downPos
                var maxDist = 0f
                var classified = GestureType.PENDING
                // SELECTION 실시간 스텝 추적 — x/y 독립 accumulator (트랙패드 방식)
                var accumX = 0f
                var accumY = 0f
                var anyStepFired = false
                // MANIPULATION 모드에서 실제 조작이 발생했는지 추적
                // true이면 touchSlop 미만 이동으로 손가락을 떼도 탭으로 보지 않아 모드를 유지
                var anyManipulationFired = false
                // 제자리 롱프레스 감지 (PENDING 상태에서 이동 없이 timeout 경과 시)
                var longPressArmed = currentOnLongPress != null

                while (true) {
                    val event = if (longPressArmed && classified == GestureType.PENDING) {
                        withTimeoutOrNull(longPressTimeoutMs) { awaitPointerEvent() }
                    } else {
                        awaitPointerEvent()
                    }
                    if (event == null) {
                        // timeout = 제자리 롱프레스 후보 (이동/뗌 없이 시간 경과)
                        longPressArmed = false
                        val handled = currentOnLongPress?.invoke() ?: false
                        if (handled) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            // 손가락 뗄 때까지 흡수 후 제스처 종료 (탭/스와이프로 처리 안 함)
                            while (true) {
                                val e = awaitPointerEvent()
                                if (e.changes.all { !it.pressed }) break
                            }
                            break
                        }
                        continue
                    }
                    val change = event.changes.firstOrNull() ?: break

                    val totalDelta = change.position - downPos
                    maxDist = maxOf(maxDist, totalDelta.getDistance())

                    if (classified == GestureType.PENDING && maxDist > touchSlop) {
                        classified = GestureType.SWIPE
                    }

                    val moveDelta = change.position - lastPos
                    if (classified == GestureType.SWIPE) {
                        when (controller.mode) {
                            SwipeMode.MANIPULATION -> {
                                if (moveDelta != Offset.Zero) {
                                    controller.manipulate(moveDelta.x, moveDelta.y, size.width.toFloat(), size.height.toFloat())
                                    anyManipulationFired = true
                                }
                            }
                            SwipeMode.SELECTION -> {
                                accumX += moveDelta.x
                                accumY += moveDelta.y
                                val maxSteps = controller.maxFocusStepsPerEvent
                                val rawStepsX = (accumX / stepPx).toInt()
                                val stepsX = rawStepsX.coerceIn(-maxSteps, maxSteps)
                                if (stepsX != 0) {
                                    val dir = if (stepsX > 0) Direction.RIGHT else Direction.LEFT
                                    repeat(kotlin.math.abs(stepsX)) {
                                        val moved = controller.moveFocus(dir)
                                        if (!moved) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            controller.triggerFlash(controller.currentFocus)
                                        }
                                    }
                                    accumX -= stepsX * stepPx
                                    // 빠른 스와이프로 maxSteps를 초과한 누적분은 버려 오버슈트(셀렉션 링 이탈) 방지
                                    if (kotlin.math.abs(rawStepsX) > maxSteps) accumX = 0f
                                    anyStepFired = true
                                }
                                val rawStepsY = (accumY / stepPx).toInt()
                                val stepsY = rawStepsY.coerceIn(-maxSteps, maxSteps)
                                if (stepsY != 0) {
                                    val dir = if (stepsY > 0) Direction.DOWN else Direction.UP
                                    repeat(kotlin.math.abs(stepsY)) {
                                        val moved = controller.moveFocus(dir)
                                        if (!moved) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            controller.triggerFlash(controller.currentFocus)
                                        }
                                    }
                                    accumY -= stepsY * stepPx
                                    // 빠른 스와이프로 maxSteps를 초과한 누적분은 버려 오버슈트(셀렉션 링 이탈) 방지
                                    if (kotlin.math.abs(rawStepsY) > maxSteps) accumY = 0f
                                    anyStepFired = true
                                }
                            }
                        }
                    } else if (controller.mode == SwipeMode.MANIPULATION && moveDelta != Offset.Zero) {
                        // MANIPULATION 모드에서는 touchSlop 미만 이동도 즉시 조작으로 처리
                        controller.manipulate(moveDelta.x, moveDelta.y, size.width.toFloat(), size.height.toFloat())
                        anyManipulationFired = true
                    }

                    lastPos = change.position

                    if (change.changedToUpIgnoreConsumed()) {
                        when (classified) {
                            GestureType.PENDING -> {
                                if (controller.mode == SwipeMode.MANIPULATION) {
                                    if (!anyManipulationFired) {
                                        // 실제 조작 없이 손가락만 댔다 뗀 경우 → 탭 → 모드 종료
                                        controller.exitManipulation()
                                    }
                                    // anyManipulationFired=true이면 스와이프가 touchSlop 미만이었던 것
                                    // 탭이 아닌 조작 시도로 보아 모드 유지
                                } else {
                                    // SELECTION: 즉시 활성화
                                    controller.activate()
                                }
                            }
                            GestureType.SWIPE -> {
                                // SELECTION: 실시간으로 이미 처리됨.
                                // 매우 빠른 스와이프로 move 이벤트 없이 up이 도착한 경우 최소 1칸 보장
                                if (controller.mode == SwipeMode.SELECTION && !anyStepFired) {
                                    val dir = classifyDirection(change.position - downPos)
                                    if (dir != null) {
                                        val moved = controller.moveFocus(dir)
                                        if (!moved) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            controller.triggerFlash(controller.currentFocus)
                                        }
                                    }
                                }
                                // MANIPULATION: move 이벤트에서 즉시 반영됐으므로 no-op
                            }
                        }
                        break
                    }
                }
            }
        },
    )
}

private enum class GestureType { PENDING, SWIPE }

private fun classifyDirection(delta: Offset): Direction? {
    val absX = abs(delta.x)
    val absY = abs(delta.y)
    if (absX == 0f && absY == 0f) return null
    return if (absX >= absY) {
        if (delta.x > 0f) Direction.RIGHT else Direction.LEFT
    } else {
        if (delta.y > 0f) Direction.DOWN else Direction.UP
    }
}

/** 스와이프로 인정하는 최소 거리 = touchSlop * 이 배수. 기본값: 1.5f */
private const val SWIPE_MIN_DISTANCE_MULTIPLIER = 1.5f

/** SELECTION 모드에서 포커스 한 칸 이동에 해당하는 스와이프 거리 (dp). 기본값: 40f */
private const val SWIPE_SELECTION_STEP_DP = 40f
