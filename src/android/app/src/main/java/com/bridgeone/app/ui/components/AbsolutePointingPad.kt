package com.bridgeone.app.ui.components

import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.bridgeone.app.ui.components.touchpad.ClickMode
import com.bridgeone.app.ui.components.touchpad.ControlButtonConfig
import com.bridgeone.app.ui.components.touchpad.ControlButtonContainer
import com.bridgeone.app.ui.components.touchpad.TouchpadColorPink
import com.bridgeone.app.ui.components.touchpad.TouchpadColorYellow
import com.bridgeone.app.ui.components.touchpad.TouchpadState
import com.bridgeone.app.protocol.FrameBuilder
import com.bridgeone.app.usb.UsbSerialManager
import com.bridgeone.app.ui.utils.AbsoluteCoordinateCalculator
import com.bridgeone.app.ui.utils.AbsolutePointingConstants
import com.bridgeone.app.ui.utils.ClickDetector
import com.bridgeone.app.ui.utils.TouchRatio
import com.bridgeone.app.ui.utils.getDistance
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ============================================================
// AbsolutePointingPad (Page 3) — Phase 4.9.1: 기본 구현 (자유 비율)
// ============================================================
//
// 터치한 위치가 곧 PC 커서 위치가 되는 절대좌표 포인팅 페이지.
// Phase 4.9.2: 터치 비율을 FrameBuilder.buildAbsolutePositionCommand()로 인코딩해
// UsbSerialManager.sendCommandBytes()로 서버 중계 전송한다(DOWN 즉시 전송 + MOVE 실시간 전송).
// 엣지존/엣지스와이프 통합은 Phase 4.9.3(후속)으로 분리되었다.
//
// Reference: docs/development-plans/phase-4/phase-4-9-page3-absolute-pointing.md

/**
 * 절대좌표 패드 페이지. PointingArea(자유 비율, stretch 매핑) + 상단 ControlButtonContainer
 * (ClickModeButton만 활성, ZoomButton/DragModeButton은 Disabled 슬롯)로 구성된다.
 */
@Composable
fun AbsolutePointingPad(modifier: Modifier = Modifier) {
    // 클릭 모드는 Page 1/2의 pageState.touchpadState와 공유하지 않는 페이지 로컬 상태.
    // ControlButtonContainer가 요구하는 TouchpadState 타입을 재사용하되 clickMode 외 필드는 미사용.
    var localState by remember { mutableStateOf(TouchpadState()) }

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
        PointingArea(
            clickMode = localState.clickMode,
            modifier = Modifier.fillMaxSize()
        )

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
            modifier = Modifier
                .fillMaxWidth(controlButtonWidthFraction)
                .align(Alignment.TopCenter)
        )
    }
}

/**
 * 절대좌표 서버 중계 프레임을 UART로 전송합니다 (Phase 4.9.2).
 *
 * FrameBuilder.buildAbsolutePositionCommand()로 8바이트 프레임을 만들고
 * UsbSerialManager.sendCommandBytes()로 전송한다. 포트 미연결 시 IllegalStateException이
 * 발생할 수 있으므로 터치 제스처 루프가 죽지 않도록 예외를 흡수한다.
 * buttons는 항상 0x00(클릭은 별도 버튼 프레임으로 처리, 드래그 모드는 Phase 4.9.4),
 * targetMonitor는 모니터 셀렉터(Phase 4.9.6) 도입 전까지 기본값 사용.
 */
private fun sendAbsolutePosition(ratio: TouchRatio) {
    try {
        val command = FrameBuilder.buildAbsolutePositionCommand(
            ratio = ratio,
            buttons = 0x00u,
            targetMonitor = AbsolutePointingConstants.DEFAULT_TARGET_MONITOR
        )
        UsbSerialManager.sendCommandBytes(command)
    } catch (e: IllegalStateException) {
        Log.w("AbsolutePointingPad", "Failed to send absolute position: ${e.message}")
    }
}

@Composable
private fun PointingArea(
    clickMode: ClickMode,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    var touchActive by remember { mutableStateOf(false) }
    var indicatorPosition by remember { mutableStateOf(Offset.Zero) }

    val indicatorAlpha by animateFloatAsState(
        targetValue = if (touchActive) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (touchActive) 0 else AbsolutePointingConstants.COORDINATE_INDICATOR_FADE_MS.toInt()
        ),
        label = "coordinateIndicatorAlpha"
    )

    val borderColor = if (clickMode == ClickMode.LEFT_CLICK) TouchpadColorPink else TouchpadColorYellow

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(AbsolutePointingConstants.POINTING_AREA_CORNER_RADIUS_DP.dp))
            .background(Color(0xFF1E1E1E))
            .border(
                width = AbsolutePointingConstants.POINTING_AREA_BORDER_WIDTH_DP.dp,
                color = borderColor,
                shape = RoundedCornerShape(AbsolutePointingConstants.POINTING_AREA_CORNER_RADIUS_DP.dp)
            )
            .pointerInput(clickMode) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downTime = System.currentTimeMillis()
                    val downPosition = down.position
                    val areaWidth = size.width.toFloat()
                    val areaHeight = size.height.toFloat()

                    touchActive = true
                    indicatorPosition = downPosition
                    var lastRatio: TouchRatio = AbsoluteCoordinateCalculator.calculateTouchRatio(
                        downPosition.x, downPosition.y, areaWidth, areaHeight
                    )
                    sendAbsolutePosition(lastRatio)

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break

                        if (change.changedToUpIgnoreConsumed()) {
                            change.consume()
                            val pressDuration = System.currentTimeMillis() - downTime
                            val movementPx = (change.position - downPosition).getDistance()
                            val movementDp = with(density) { movementPx.toDp().value }

                            if (pressDuration <= AbsolutePointingConstants.CLICK_MAX_DURATION_MS &&
                                movementDp <= AbsolutePointingConstants.CLICK_MAX_MOVEMENT_DP
                            ) {
                                val buttons: UByte = if (clickMode == ClickMode.LEFT_CLICK) 0x01u.toUByte() else 0x02u.toUByte()
                                ClickDetector.sendFrame(ClickDetector.createMouseButtonFrame(buttons))
                                coroutineScope.launch {
                                    delay(ClickDetector.CLICK_PRESS_RELEASE_GAP_MS)
                                    ClickDetector.sendFrame(ClickDetector.createMouseButtonFrame(0x00u.toUByte()))
                                }
                            }
                            break
                        }

                        if (change.positionChanged()) {
                            indicatorPosition = change.position
                            val ratio = AbsoluteCoordinateCalculator.calculateTouchRatio(
                                change.position.x, change.position.y, areaWidth, areaHeight
                            )
                            if (AbsoluteCoordinateCalculator.shouldTransmit(ratio, lastRatio)) {
                                lastRatio = ratio
                                sendAbsolutePosition(ratio)
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
