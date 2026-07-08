package com.bridgeone.app.ui.components

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
// 본 Phase는 좌표 계산 + 클릭 감지 + 시각 피드백까지만 구현하며,
// 서버 중계 프레임 전송(FrameBuilder.buildAbsolutePositionCommand)은 Phase 4.9.2에서 추가된다.
// 엣지존/엣지스와이프 통합은 Phase 4.9.1b(후속)로 분리되었다.
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
                                // Phase 4.9.2에서 buildAbsolutePositionCommand() 서버 중계 전송 추가 예정
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
