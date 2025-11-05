package com.bridgeone.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview

/**
 * TouchpadWrapper Composable
 *
 * 터치패드 UI 구성 및 터치 이벤트 감지를 담당합니다.
 * 1:2 비율의 직사각형으로 구성되며, 라운드 모서리가 적용됩니다.
 * 터치 이벤트(DOWN, MOVE, UP)를 감지하고 터치 좌표를 저장합니다.
 *
 * @param modifier 외부에서 추가할 수 있는 Modifier
 * @param onTouchEvent 터치 이벤트 콜백 함수
 *   - eventType: PointerEventType (DOWN, MOVE, UP)
 *   - currentPosition: 현재 터치 위치
 *   - previousPosition: 이전 터치 위치 (델타 계산용)
 */
@Composable
fun TouchpadWrapper(
    modifier: Modifier = Modifier,
    onTouchEvent: (
        eventType: PointerEventType,
        currentPosition: Offset,
        previousPosition: Offset
    ) -> Unit = { _, _, _ -> }
) {
    // 현재 터치 위치 및 이전 터치 위치 상태
    val currentTouchPosition = remember { mutableStateOf(Offset.Zero) }
    val previousTouchPosition = remember { mutableStateOf(Offset.Zero) }

    // 터치패드의 기본 크기: 가로 160dp, 세로 320dp (1:2 비율)
    val TOUCHPAD_WIDTH = 160.dp
    val TOUCHPAD_HEIGHT = 320.dp

    // 모서리 반경: 너비의 3% = 160dp * 0.03 = 4.8dp ≈ 5dp
    val CORNER_RADIUS = (TOUCHPAD_WIDTH * 0.03f)

    Box(
        modifier = modifier
            .size(TOUCHPAD_WIDTH, TOUCHPAD_HEIGHT)
            .clip(RoundedCornerShape(CORNER_RADIUS))
            .background(Color(0xFF1A1A1A))  // 다크 테마 배경색
            .pointerInput(Unit) {
                awaitEachGesture {
                    // 포인터 DOWN 이벤트 처리
                    val down = awaitPointerEvent()
                    if (down.type == PointerEventType.Press) {
                        currentTouchPosition.value = down.changes.first().position
                        previousTouchPosition.value = currentTouchPosition.value
                        onTouchEvent(
                            PointerEventType.Press,
                            currentTouchPosition.value,
                            previousTouchPosition.value
                        )
                    }

                    // 포인터 MOVE 이벤트 처리
                    var moveEvent = awaitPointerEvent()
                    while (moveEvent.type == PointerEventType.Move) {
                        previousTouchPosition.value = currentTouchPosition.value
                        currentTouchPosition.value = moveEvent.changes.first().position
                        onTouchEvent(
                            PointerEventType.Move,
                            currentTouchPosition.value,
                            previousTouchPosition.value
                        )
                        moveEvent = awaitPointerEvent()
                    }

                    // 포인터 UP 이벤트 처리
                    if (moveEvent.type == PointerEventType.Release) {
                        previousTouchPosition.value = currentTouchPosition.value
                        currentTouchPosition.value = moveEvent.changes.first().position
                        onTouchEvent(
                            PointerEventType.Release,
                            currentTouchPosition.value,
                            previousTouchPosition.value
                        )
                    }
                }
            }
    ) {
        // 터치패드 내용 (향후 UI 요소 추가)
    }
}

/**
 * TouchpadWrapper Preview
 *
 * Android Studio에서 UI를 시각적으로 확인하기 위한 Preview 함수입니다.
 * 1:2 비율, 둥근 모서리, 터치 이벤트 감지 기능을 표시합니다.
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
            .background(Color(0xFF0D0D0D))
    )
}

