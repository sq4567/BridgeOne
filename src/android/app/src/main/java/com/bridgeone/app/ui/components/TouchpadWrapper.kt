package com.bridgeone.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.math.sqrt

/**
 * 델타 계산 결과를 저장하는 데이터 클래스.
 *
 * @property deltaX X축 델타 값 (-127 ~ 127)
 * @property deltaY Y축 델타 값 (-127 ~ 127)
 */
data class DeltaResult(
    val deltaX: Byte,
    val deltaY: Byte
)

/**
 * 델타 계산 및 데드존 보상 관련 상수 정의.
 */
object TouchpadConstants {
    // 데드존 임계값 (15dp) - 이 값 이하의 이동은 무시
    const val DEAD_ZONE_THRESHOLD_DP = 15f
    
    // 델타 범위 제한 (i8: -128 ~ 127)
    const val DELTA_MAX = 127
    const val DELTA_MIN = -128
}

/**
 * 터치 좌표 및 상태를 추적하는 데이터 클래스.
 *
 * @property previousX 이전 터치 X 좌표 (픽셀)
 * @property previousY 이전 터치 Y 좌표 (픽셀)
 * @property accumulatedDeltaX 데드존 누적 X 델타 (픽셀)
 * @property accumulatedDeltaY 데드존 누적 Y 델타 (픽셀)
 */
data class TouchState(
    val previousX: Float = 0f,
    val previousY: Float = 0f,
    val accumulatedDeltaX: Float = 0f,
    val accumulatedDeltaY: Float = 0f
)

/**
 * 터치 좌표의 델타를 계산합니다 (dp 단위).
 *
 * Jetpack Compose의 터치 이벤트는 픽셀 단위이므로, LocalDensity를 사용하여
 * 픽셀을 dp로 변환한 후 델타를 계산합니다. 이를 통해 화면 밀도와 관계없이
 * 일관된 터치 경험을 제공합니다.
 *
 * **변환 과정**:
 * 1. 현재/이전 터치 위치 (픽셀) 수신
 * 2. LocalDensity를 사용하여 픽셀 → dp 변환
 * 3. dp 단위로 델타 계산: (current_dp - previous_dp)
 *
 * @param currentX 현재 터치 X 좌표 (픽셀)
 * @param currentY 현재 터치 Y 좌표 (픽셀)
 * @param previousX 이전 터치 X 좌표 (픽셀)
 * @param previousY 이전 터치 Y 좌표 (픽셀)
 * @param density LocalDensity.current (LocalDensity 접근용)
 * @return X축, Y축 델타 값 (dp 단위)
 */
fun calculateDelta(
    currentX: Float,
    currentY: Float,
    previousX: Float,
    previousY: Float,
    density: Density
): Pair<Float, Float> {
    // LocalDensity를 사용하여 픽셀을 dp로 변환
    // with(density)는 LocalDensity의 extension function을 사용하기 위한 구문
    return with(density) {
        val currentXDp = currentX.toDp().value
        val currentYDp = currentY.toDp().value
        val previousXDp = previousX.toDp().value
        val previousYDp = previousY.toDp().value
        
        val deltaXDp = currentXDp - previousXDp
        val deltaYDp = currentYDp - previousYDp
        
        Pair(deltaXDp, deltaYDp)
    }
}

/**
 * 데드존 보상을 적용하고 델타를 정규화합니다.
 *
 * **데드존 알고리즘**:
 * 1. 데드존 임계값(15dp) 이하의 이동은 누적하여 저장
 * 2. 누적된 이동이 임계값을 초과하면 누적값을 한 번에 반영
 * 3. 임계값 초과 후는 실시간 이동을 그대로 전송
 *
 * **정규화**:
 * - 최종 델타를 i8 범위(-128~127)로 클램핑
 * - 정규화된 델타를 Byte 타입으로 반환
 *
 * @param deltaXDp X축 델타 (dp 단위)
 * @param deltaYDp Y축 델타 (dp 단위)
 * @param accumulatedDeltaX 누적 X 델타 (픽셀)
 * @param accumulatedDeltaY 누적 Y 델타 (픽셀)
 * @param density LocalDensity.current (dp→픽셀 변환용)
 * @return Pair<DeltaResult, Pair<newAccumulatedX, newAccumulatedY>>
 *         DeltaResult: 정규화된 최종 델타 (-128~127)
 *         newAccumulated: 업데이트된 누적 델타
 */
fun applyDeadZone(
    deltaXDp: Float,
    deltaYDp: Float,
    accumulatedDeltaX: Float,
    accumulatedDeltaY: Float,
    density: Density
): Pair<DeltaResult, Pair<Float, Float>> {
    // 1. 데드존 임계값을 픽셀 단위로 변환
    val deadZoneThresholdPx = with(density) {
        TouchpadConstants.DEAD_ZONE_THRESHOLD_DP.dp.toPx()
    }
    
    // 2. 현재 델타를 픽셀 단위로 변환
    val deltaXPx = with(density) { deltaXDp.dp.toPx() }
    val deltaYPx = with(density) { deltaYDp.dp.toPx() }
    
    // 3. 누적 델타에 현재 델타 추가
    var newAccumulatedX = accumulatedDeltaX + deltaXPx
    var newAccumulatedY = accumulatedDeltaY + deltaYPx
    
    // 4. 누적된 이동거리 계산 (피타고라스 정리)
    val accumulatedDistance = sqrt(newAccumulatedX * newAccumulatedX + newAccumulatedY * newAccumulatedY)
    
    // 5. 최종 델타 계산
    var finalDeltaX = 0f
    var finalDeltaY = 0f
    
    if (accumulatedDistance > deadZoneThresholdPx) {
        // 데드존 임계값 초과: 누적값을 한번에 반영하고 초기화
        finalDeltaX = newAccumulatedX
        finalDeltaY = newAccumulatedY
        newAccumulatedX = 0f
        newAccumulatedY = 0f
    }
    // else: 임계값 이하면 누적만 하고 델타는 0
    
    // 6. 최종 델타를 i8 범위(-128~127)로 정규화
    val normalizedDeltaX = finalDeltaX.toInt().coerceIn(
        TouchpadConstants.DELTA_MIN,
        TouchpadConstants.DELTA_MAX
    ).toByte()
    
    val normalizedDeltaY = finalDeltaY.toInt().coerceIn(
        TouchpadConstants.DELTA_MIN,
        TouchpadConstants.DELTA_MAX
    ).toByte()
    
    return Pair(
        DeltaResult(normalizedDeltaX, normalizedDeltaY),
        Pair(newAccumulatedX, newAccumulatedY)
    )
}

/**
 * 터치패드 컴포넌트 - 2D 터치 입력을 처리합니다.
 *
 * **UI 구조**:
 * - 1:2 비율의 직사각형 (가로 1, 세로 2)
 * - 최소 크기: 160dp × 320dp
 * - 둥근 모서리: 너비의 3%
 *
 * **터치 처리**:
 * - 터치 시작 (DOWN): 이전 위치 저장
 * - 터치 이동 (MOVE): 델타 계산 및 데드존 보상 적용
 * - 터치 종료 (UP): 상태 초기화
 *
 * **데드존 알고리즘**:
 * - 15dp 이하 이동: 누적 (데드존 내)
 * - 15dp 초과 이동: 누적값을 한번에 반영 후 실시간 처리
 * - 정규화: -128~127 범위로 클램핑
 *
 * @param onDeltaCalculated 델타 계산 완료 콜백 (deltaX, deltaY)
 * @param modifier 추가 Modifier
 */
@Composable
fun TouchpadWrapper(
    onDeltaCalculated: (deltaX: Byte, deltaY: Byte) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    
    // 터치 상태 관리
    val touchState = remember { mutableStateOf(TouchState()) }
    
    // 터치패드 너비의 3%를 둥근 모서리 크기로 설정
    // 실제 크기는 fillMaxWidth() 적용 후 결정되므로, 여기서는 고정값 사용
    val cornerRadius = 4.8.dp  // 160dp 기준 너비의 3% = 4.8dp
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f / 2f)  // 1:2 비율
            .background(
                color = Color.LightGray,
                shape = RoundedCornerShape(cornerRadius)
            )
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        // DOWN: 터치 시작
                        touchState.value = TouchState(
                            previousX = offset.x,
                            previousY = offset.y,
                            accumulatedDeltaX = 0f,
                            accumulatedDeltaY = 0f
                        )
                    },
                    onDrag = { change, dragAmount ->
                        // MOVE: 터치 이동
                        val current = touchState.value
                        val newX = current.previousX + dragAmount.x
                        val newY = current.previousY + dragAmount.y
                        
                        // 1. 델타 계산 (dp 단위)
                        val (deltaXDp, deltaYDp) = calculateDelta(
                            newX, newY,
                            current.previousX, current.previousY,
                            density
                        )
                        
                        // 2. 데드존 보상 적용
                        val (deltaResult, newAccumulated) = applyDeadZone(
                            deltaXDp, deltaYDp,
                            current.accumulatedDeltaX, current.accumulatedDeltaY,
                            density
                        )
                        
                        // 3. 디버그 로그
                        android.util.Log.d(
                            "TouchpadWrapper",
                            "Delta: X=${deltaXDp}dp Y=${deltaYDp}dp → " +
                            "Final: X=${deltaResult.deltaX} Y=${deltaResult.deltaY} " +
                            "Accumulated: X=${newAccumulated.first} Y=${newAccumulated.second}"
                        )
                        
                        // 4. 콜백 호출 (델타가 0이 아닐 때만)
                        if (deltaResult.deltaX != 0.toByte() || deltaResult.deltaY != 0.toByte()) {
                            onDeltaCalculated(deltaResult.deltaX, deltaResult.deltaY)
                        }
                        
                        // 5. 상태 업데이트
                        touchState.value = TouchState(
                            previousX = newX,
                            previousY = newY,
                            accumulatedDeltaX = newAccumulated.first,
                            accumulatedDeltaY = newAccumulated.second
                        )
                    },
                    onDragEnd = {
                        // UP: 터치 종료
                        touchState.value = TouchState()
                    }
                )
            }
    )
}

/**
 * TouchpadWrapper 미리보기 함수 (Compose Preview용).
 */
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun TouchpadWrapperPreview() {
    TouchpadWrapper(
        onDeltaCalculated = { deltaX, deltaY ->
            android.util.Log.d("Preview", "Delta: X=$deltaX Y=$deltaY")
        }
    )
}
