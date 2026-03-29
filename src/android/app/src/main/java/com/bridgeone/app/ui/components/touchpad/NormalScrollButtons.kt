package com.bridgeone.app.ui.components.touchpad

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.bridgeone.app.ui.common.ScrollConstants.NORMAL_SCROLL_BUTTON_INTERVAL_MS
import com.bridgeone.app.ui.utils.ClickDetector
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val ScrollButtonColor = Color(0xFF84E268)  // 일반 스크롤 초록색
private val ScrollButtonIconColor = Color(0xFF1E1E1E)

/**
 * 일반 스크롤 모드 전용 상하 스크롤 버튼
 *
 * NORMAL_SCROLL 모드에서 터치패드 왼쪽 하단에 오버레이됩니다.
 * 버튼을 누르고 있으면 [NORMAL_SCROLL_BUTTON_INTERVAL_MS] 간격으로
 * 스크롤 프레임을 연속 전송합니다.
 *
 * - 위 버튼: 홀드 → 위로 스크롤 (wheelDelta = +1)
 * - 아래 버튼: 홀드 → 아래로 스크롤 (wheelDelta = -1)
 */
@Composable
fun NormalScrollButtons(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ScrollHoldButton(
            icon = Icons.Default.KeyboardArrowUp,
            contentDescription = "위로 스크롤",
            wheelDelta = 1.toByte()
        )
        ScrollHoldButton(
            icon = Icons.Default.KeyboardArrowDown,
            contentDescription = "아래로 스크롤",
            wheelDelta = (-1).toByte()
        )
    }
}

@Composable
private fun ScrollHoldButton(
    icon: ImageVector,
    contentDescription: String,
    wheelDelta: Byte
) {
    val coroutineScope = rememberCoroutineScope()
    var scrollJob by remember { mutableStateOf<Job?>(null) }
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isPressed) ScrollButtonColor.copy(alpha = 0.60f) else ScrollButtonColor
            )
            .pointerInput(wheelDelta) {
                awaitEachGesture {
                    // ── DOWN ──
                    val down = awaitPointerEvent()
                    if (down.type != PointerEventType.Press) return@awaitEachGesture
                    down.changes.forEach { it.consume() }

                    isPressed = true
                    scrollJob?.cancel()
                    scrollJob = coroutineScope.launch {
                        // 첫 스크롤 즉시 전송
                        ClickDetector.sendFrame(ClickDetector.createWheelFrame(wheelDelta))
                        // 이후 일정 간격으로 반복
                        while (true) {
                            delay(NORMAL_SCROLL_BUTTON_INTERVAL_MS)
                            ClickDetector.sendFrame(ClickDetector.createWheelFrame(wheelDelta))
                        }
                    }

                    // ── RELEASE 대기 ──
                    var event = awaitPointerEvent()
                    while (event.type != PointerEventType.Release) {
                        event.changes.forEach { it.consume() }
                        event = awaitPointerEvent()
                    }
                    event.changes.forEach { it.consume() }

                    isPressed = false
                    scrollJob?.cancel()
                    scrollJob = null
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = ScrollButtonIconColor,
            modifier = Modifier.size(28.dp)
        )
    }
}
