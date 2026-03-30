package com.bridgeone.app.ui.components.touchpad

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.bridgeone.app.ui.common.AppIcon
import com.bridgeone.app.ui.common.DYNAMICS_PRESETS
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val ButtonColor = Color(0xFF7C9EFF)       // 커서 이동 모드 파란색 계열
private val ButtonColorPressed = Color(0xFF4C6ECC)
private val ButtonIconColor = Color(0xFF1E1E1E)

private const val LONG_PRESS_DELAY_MS = 500L

/**
 * 포인터 다이나믹스 프리셋 선택 버튼 (Phase 4.3.8)
 *
 * 커서 이동 모드(ScrollMode.OFF)에서 터치패드 왼쪽 하단에 오버레이됩니다.
 * NormalScrollButtons와 동일 위치 — AnimatedVisibility로 교대 표시.
 *
 * - 탭: 다음 프리셋으로 사이클 전환 (Off → Precision → Standard → Fast → Off)
 * - 롱프레스: [onLongPress] 콜백 → 프리셋 목록 팝업 표시
 *
 * @param touchpadState        현재 터치패드 상태
 * @param onTouchpadStateChange 상태 변경 콜백
 * @param onLongPress          롱프레스 시 팝업 표시 콜백
 */
@Composable
fun DynamicsPresetButton(
    touchpadState: TouchpadState,
    onTouchpadStateChange: (TouchpadState) -> Unit,
    onLongPress: () -> Unit,
    showLabel: Boolean = false,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var isPressed by remember { mutableStateOf(false) }
    var longPressJob by remember { mutableStateOf<Job?>(null) }

    val currentPreset = DYNAMICS_PRESETS.getOrNull(touchpadState.dynamicsPresetIndex)
        ?: DYNAMICS_PRESETS.first()

    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isPressed) ButtonColorPressed else ButtonColor)
            .pointerInput(touchpadState.dynamicsPresetIndex) {
                awaitEachGesture {
                    // ── DOWN ──
                    val down = awaitPointerEvent()
                    if (down.type != PointerEventType.Press) return@awaitEachGesture
                    down.changes.forEach { it.consume() }

                    isPressed = true

                    // 롱프레스 타이머 시작
                    var longPressTriggered = false
                    longPressJob?.cancel()
                    longPressJob = coroutineScope.launch {
                        delay(LONG_PRESS_DELAY_MS)
                        longPressTriggered = true
                        onLongPress()
                    }

                    // ── RELEASE 대기 ──
                    var event = awaitPointerEvent()
                    while (event.type != PointerEventType.Release) {
                        event.changes.forEach { it.consume() }
                        event = awaitPointerEvent()
                    }
                    event.changes.forEach { it.consume() }

                    longPressJob?.cancel()
                    longPressJob = null
                    isPressed = false

                    if (!longPressTriggered) {
                        // 탭: 다음 프리셋으로 사이클
                        val nextIndex = (touchpadState.dynamicsPresetIndex + 1) % DYNAMICS_PRESETS.size
                        onTouchpadStateChange(touchpadState.copy(dynamicsPresetIndex = nextIndex))
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AppIcon(
            def = currentPreset.icon,
            contentDescription = "다이나믹스: ${currentPreset.name}",
            tint = ButtonIconColor,
            modifier = Modifier.size(28.dp)
        )

        // 프리셋 탭 라벨 — Popup으로 clip 밖에 버튼 바로 위 가운데 렌더링
        if (showLabel) {
            val density = LocalDensity.current
            Popup(
                alignment = Alignment.TopCenter,
                offset = IntOffset(0, with(density) { (-29).dp.roundToPx() }),
                properties = PopupProperties(focusable = false)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = currentPreset.name,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
