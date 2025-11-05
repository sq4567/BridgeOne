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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.bridgeone.app.ui.utils.DeltaCalculator
import android.util.Log

/**
 * TouchpadWrapper Composable
 *
 * 터치패드 UI 구성 및 터치 이벤트 감지를 담당합니다.
 * 1:2 비율의 직사각형으로 구성되며, 라운드 모서리가 적용됩니다.
 * 터치 이벤트(DOWN, MOVE, UP)를 감지하고 터치 좌표를 저장합니다.
 *
 * Phase 2.2.3.2 업데이트: 델타 계산 및 데드존 보상
 * - onTouchEvent 콜백에서 DeltaCalculator 호출 가능
 * - MOVE 이벤트 발생 시 자동으로 델타 계산
 * - 디버그 로그 출력 (원본/보상 후 값)
 *
 * @param modifier 외부에서 추가할 수 있는 Modifier
 * @param onTouchEvent 터치 이벤트 콜백 함수
 *   - eventType: PointerEventType (PRESS, MOVE, RELEASE)
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
    
    // Phase 2.2.3.2: 현재 화면의 밀도 정보
    val density = LocalDensity.current

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
                        
                        // Phase 2.2.3.2: 델타 계산 및 데드존 보상
                        val rawDelta = DeltaCalculator.calculateDelta(
                            previousTouchPosition.value,
                            currentTouchPosition.value
                        )
                        val compensatedDelta = DeltaCalculator.applyDeadZone(
                            density,
                            DeltaCalculator.convertDpToPixels(density, rawDelta)
                        )
                        
                        // 디버그 로그: 원본 및 보상 후 델타 값
                        Log.d(
                            "TouchpadWrapper",
                            "MOVE Event - Raw Delta: (${rawDelta.x.toInt()}, ${rawDelta.y.toInt()}) → " +
                            "Compensated: (${compensatedDelta.x.toInt()}, ${compensatedDelta.y.toInt()})"
                        )
                        
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
                        
                        // Phase 2.2.3.2: RELEASE 이벤트 시에도 델타 계산
                        val releaseDelta = DeltaCalculator.calculateDelta(
                            previousTouchPosition.value,
                            currentTouchPosition.value
                        )
                        val releaseCompensatedDelta = DeltaCalculator.applyDeadZone(
                            density,
                            DeltaCalculator.convertDpToPixels(density, releaseDelta)
                        )
                        
                        // 디버그 로그: RELEASE 이벤트의 델타 값
                        Log.d(
                            "TouchpadWrapper",
                            "RELEASE Event - Raw Delta: (${releaseDelta.x.toInt()}, ${releaseDelta.y.toInt()}) → " +
                            "Compensated: (${releaseCompensatedDelta.x.toInt()}, ${releaseCompensatedDelta.y.toInt()})"
                        )
                        
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

