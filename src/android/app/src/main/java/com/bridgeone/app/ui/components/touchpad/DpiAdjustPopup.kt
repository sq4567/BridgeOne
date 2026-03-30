package com.bridgeone.app.ui.components.touchpad

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.offset
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

// DPI 조절 파라미터
private const val DPI_ADJUST_STEP_DP = 20f   // 0.1x 변화당 필요한 드래그 거리 (dp)
private const val DPI_MIN = 0.1f
private const val DPI_MAX = 5.0f
private const val DPI_STEP = 0.1f
private const val TAP_THRESHOLD_DP = 8f       // 탭 판정 최대 이동량 (dp)

/**
 * DPI 세밀 조절 팝업 (Phase 4.3.6)
 *
 * DPIControlButton 롱 프레스 시 터치패드 영역 내 반투명 오버레이로 표시됩니다.
 * 팝업 내 pointerInput이 모든 이벤트를 소비하므로 TouchpadWrapper 제스처 처리가
 * 자동으로 차단됩니다 (Phase 4.3.4.5 메커니즘).
 *
 * - 스와이프(상하·좌우): 0.1 단위로 DPI 값 증감
 * - 탭: 현재 값 확정 → [onConfirm] 콜백
 * - 경계(0.1x / 5.0x) 도달 시: 붉은 텍스트 + shake + 햅틱
 *
 * @param initialMultiplier 팝업 열릴 때의 초기 DPI 배율
 * @param onConfirm 탭 확정 시 콜백 (확정된 Float 배율 전달)
 * @param onDismiss 외부 취소(페이지 전환 등) 시 콜백 — 값 미적용
 */
@Composable
fun DpiAdjustPopup(
    initialMultiplier: Float,
    onConfirm: (Float) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()

    // 초기값: 0.1 단위로 반올림 후 범위 제한 (부동소수점 오차 방지: ×10 → 반올림 → ÷10)
    val roundedInitial = remember(initialMultiplier) {
        ((initialMultiplier * 10f).roundToInt() / 10f).coerceIn(DPI_MIN, DPI_MAX)
    }

    var currentValue by remember { mutableFloatStateOf(roundedInitial) }
    var textColor by remember { mutableStateOf(Color.White) }
    val shakeAnim = remember { Animatable(0f) }

    val stepDistancePx = with(density) { DPI_ADJUST_STEP_DP.dp.toPx() }
    val tapThresholdPx = with(density) { TAP_THRESHOLD_DP.dp.toPx() }

    fun triggerBoundaryFeedback() {
        if (shakeAnim.isRunning) return
        coroutineScope.launch {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                view.performHapticFeedback(HapticFeedbackConstants.REJECT)
            } else {
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }
            textColor = Color(0xFFFF4444)
            shakeAnim.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 350
                    10f at 50
                    -10f at 110
                    8f at 170
                    -8f at 230
                    4f at 280
                    0f at 350
                }
            )
            textColor = Color.White
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitPointerEvent()
                    if (down.type != PointerEventType.Press) return@awaitEachGesture
                    down.changes.forEach { it.consume() }

                    val downPos = down.changes.first().position
                    var lastPos = downPos
                    var hasDragged = false
                    var accumDrag = 0f   // 스텝 계산용 누적 이동량 (소수점 보존)

                    var moveEvent = awaitPointerEvent()
                    while (moveEvent.type == PointerEventType.Move) {
                        moveEvent.changes.forEach { it.consume() }
                        val pos = moveEvent.changes.first().position
                        val dx = pos.x - lastPos.x
                        val dy = pos.y - lastPos.y

                        // 탭 여부 판정: 다운 지점에서 누적 이동량 초과 시 드래그
                        val fromDown = pos - downPos
                        val distFromDown = sqrt(fromDown.x * fromDown.x + fromDown.y * fromDown.y)
                        if (distFromDown > tapThresholdPx) hasDragged = true

                        // 우(+dx) / 상(-dy) → 증가, 좌(-dx) / 하(+dy) → 감소
                        accumDrag += dx - dy

                        val steps = (accumDrag / stepDistancePx).toInt()
                        if (steps != 0) {
                            accumDrag -= steps * stepDistancePx

                            val proposed = currentValue + steps * DPI_STEP
                            // 부동소수점 오차 방지: ×10 → 반올림 → ÷10
                            val rounded = ((proposed * 10f).roundToInt() / 10f)
                                .coerceIn(DPI_MIN, DPI_MAX)

                            if (abs(rounded - currentValue) > 0.001f) {
                                currentValue = rounded
                            } else if (
                                (steps > 0 && currentValue >= DPI_MAX - 0.001f) ||
                                (steps < 0 && currentValue <= DPI_MIN + 0.001f)
                            ) {
                                triggerBoundaryFeedback()
                                accumDrag = 0f  // 경계에서 누적 초기화
                            }
                        }

                        lastPos = pos
                        moveEvent = awaitPointerEvent()
                    }

                    if (moveEvent.type == PointerEventType.Release) {
                        moveEvent.changes.forEach { it.consume() }
                        if (!hasDragged) {
                            // 탭: 현재 값 확정
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            } else {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            }
                            onConfirm(currentValue)
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${"%.1f".format(currentValue)}x",
                fontSize = 52.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                modifier = Modifier.offset { IntOffset(shakeAnim.value.roundToInt(), 0) }
            )
            Text(
                text = "스와이프로 조절 · 탭하여 확정",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}
