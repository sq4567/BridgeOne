package com.bridgeone.app.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bridgeone.app.ui.theme.BackgroundPrimary
import com.bridgeone.app.ui.theme.PretendardFontFamily
import com.bridgeone.app.ui.theme.StateError
import com.bridgeone.app.ui.theme.StateInfo
import com.bridgeone.app.ui.theme.StateSuccess
import com.bridgeone.app.ui.theme.StateWarning
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

// ============================================================
// 토스트 타입
// ============================================================

enum class ToastType {
    INFO, SUCCESS, WARNING, ERROR
}

// ============================================================
// 토스트 메시지 모델
// ============================================================

data class ToastMessage(
    val message: String,
    val type: ToastType,
    val durationMs: Long = 3000L
) {
    /** equals/hashCode에 포함되지 않는 고유 ID — Compose key 용도 */
    val id: Long = System.nanoTime()
}

/** 자동 사라짐 없이 무제한 표시 */
const val TOAST_DURATION_INFINITE = Long.MAX_VALUE

// ============================================================
// ToastController — 전역 싱글톤
// ============================================================

object ToastController {
    private val _current = MutableStateFlow<ToastMessage?>(null)
    val current = _current.asStateFlow()

    fun show(message: String, type: ToastType, durationMs: Long = 3000L) {
        _current.value = ToastMessage(message, type, durationMs)
    }

    fun dismiss() {
        _current.value = null
    }
}

// ============================================================
// 내부 애니메이션 이징 상수
// ============================================================


private const val ENTER_DURATION_MS = 350
private const val EXIT_DURATION_MS = 300

// ============================================================
// ToastSlot — 슬롯별 toast + visibility 상태
// ============================================================

private class ToastSlot(
    val toast: ToastMessage,
    val visible: MutableState<Boolean> = mutableStateOf(false)
)

// ============================================================
// StatusToastOverlay — BridgeOneApp 최상위 Box에 배치
// ============================================================

/**
 * 커스텀 상태 알림 토스트 오버레이.
 *
 * BridgeOneApp의 최상위 Box에서 마지막 자식으로 배치해야 합니다.
 * ToastController.show(...)를 통해 어디서든 토스트를 표시할 수 있습니다.
 *
 * 위치: 화면 상단 중앙 (상태표시줄 바로 아래)
 * 스타일: 알약 형태, 상태별 색상, EaseOutBack 등장 애니메이션
 *
 * 전환 동작:
 * 1. 새 토스트가 기존 토스트 아래에 슬라이드인
 * 2. enter 애니메이션 완료 후 기존 토스트가 위로 슬라이드아웃
 */
@Composable
fun StatusToastOverlay() {
    val toastMessage by ToastController.current.collectAsState()
    // cleanup 코루틴이 LaunchedEffect 재시작에도 살아있도록 별도 scope 사용
    val scope = rememberCoroutineScope()
    val slots = remember { mutableStateListOf<ToastSlot>() }

    LaunchedEffect(toastMessage) {
        val msg = toastMessage
        if (msg == null) {
            slots.forEach { it.visible.value = false }
            delay(EXIT_DURATION_MS.toLong() + 100L)
            slots.clear()
            return@LaunchedEffect
        }

        // ① 새 슬롯을 visible=false로 추가 (한 프레임 후 true로 전환해야 enter 애니메이션 실행)
        val newSlot = ToastSlot(msg)
        slots.add(newSlot)

        // ② 한 프레임 대기 후 visible=true로 전환 → AnimatedVisibility가 enter 애니메이션 재생
        withFrameNanos { }
        newSlot.visible.value = true

        // ③ enter 애니메이션 완료까지 대기
        delay(ENTER_DURATION_MS.toLong())

        // ③ 기존 토스트를 위로 dismiss
        val dismissing = slots.filter { it.visible.value && it !== newSlot }.toList()
        dismissing.forEach { it.visible.value = false }

        // exit 애니메이션 완료 후 dismiss된 슬롯 제거
        scope.launch {
            delay(EXIT_DURATION_MS.toLong() + 100L)
            slots.removeAll(dismissing.toSet())
        }

        // ④ 자동 사라짐 (enter 대기 시간만큼 차감)
        if (msg.durationMs != TOAST_DURATION_INFINITE) {
            val remaining = (msg.durationMs - ENTER_DURATION_MS).coerceAtLeast(0L)
            delay(remaining)
            if (ToastController.current.value == msg) {
                newSlot.visible.value = false
                delay(EXIT_DURATION_MS.toLong() + 50L)
                slots.remove(newSlot)
                if (ToastController.current.value == msg) {
                    ToastController.dismiss()
                }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(top = 35.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)
        ) {
            slots.forEach { slot ->
                key(slot.toast.id) {
                    AnimatedVisibility(
                        visible = slot.visible.value,
                        // 새 토스트: 위에서 아래로 슬라이드인
                        enter = slideInVertically(
                            initialOffsetY = { -it },
                            animationSpec = tween(
                                durationMillis = ENTER_DURATION_MS,
                                easing = FastOutSlowInEasing
                            )
                        ) + fadeIn(animationSpec = tween(durationMillis = ENTER_DURATION_MS)),
                        // 기존 토스트: 위로 슬라이드아웃
                        exit = slideOutVertically(
                            targetOffsetY = { -it },
                            animationSpec = tween(
                                durationMillis = EXIT_DURATION_MS,
                                easing = FastOutLinearInEasing
                            )
                        ) + fadeOut(animationSpec = tween(durationMillis = EXIT_DURATION_MS))
                    ) {
                        StatusToastContent(
                            message = slot.toast.message,
                            type = slot.toast.type,
                            durationMs = slot.toast.durationMs
                        )
                    }
                }
            }
        }
    }
}

// ============================================================
// StatusToastContent — 토스트 UI 본체
// ============================================================

@Composable
private fun StatusToastContent(
    message: String,
    type: ToastType,
    durationMs: Long = 3000L
) {
    val backgroundColor: Color = when (type) {
        ToastType.INFO -> StateInfo
        ToastType.SUCCESS -> StateSuccess
        ToastType.WARNING -> StateWarning
        ToastType.ERROR -> StateError
    }
    val contentColor: Color = if (type == ToastType.WARNING) BackgroundPrimary else Color.White
    val iconTint: Color = backgroundColor  // 흰 원 위에서 토스트 배경색으로 아이콘을 표시
    val icon: ImageVector = when (type) {
        ToastType.INFO -> Icons.Filled.Info
        ToastType.SUCCESS -> Icons.Filled.CheckCircle
        ToastType.WARNING -> Icons.Filled.Warning
        ToastType.ERROR -> Icons.Filled.Error
    }

    // 타이머 테두리 애니메이션: 남은 시간에 비례하여 테두리가 줄어듦
    val showTimerBorder = durationMs != TOAST_DURATION_INFINITE
    val timerProgress = remember { Animatable(1f) }
    val borderColor = contentColor.copy(alpha = 0.4f)

    LaunchedEffect(message) {
        timerProgress.snapTo(1f)
        if (showTimerBorder) {
            timerProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = durationMs.toInt(),
                    easing = LinearEasing
                )
            )
        }
    }

    Row(
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp)
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = Color.Black.copy(alpha = 0.3f),
                spotColor = Color.Black.copy(alpha = 0.3f)
            )
            .background(backgroundColor, RoundedCornerShape(28.dp))
            .then(
                if (showTimerBorder) {
                    Modifier.drawWithContent {
                        drawContent()
                        val progress = timerProgress.value
                        if (progress > 0.005f) {
                            val cornerRadiusPx = 28.dp.toPx()
                            val strokeWidthPx = 3.5.dp.toPx()
                            val halfStroke = strokeWidthPx / 2f
                            val r = cornerRadiusPx - halfStroke
                            val w = size.width - strokeWidthPx
                            val h = size.height - strokeWidthPx
                            val straight = 2f * (w - 2f * r) + 2f * (h - 2f * r)
                            val curved = 2f * PI.toFloat() * r
                            val totalLen = straight + curved

                            drawRoundRect(
                                color = borderColor,
                                topLeft = Offset(halfStroke, halfStroke),
                                size = Size(w, h),
                                cornerRadius = CornerRadius(r),
                                style = Stroke(
                                    width = strokeWidthPx,
                                    pathEffect = PathEffect.dashPathEffect(
                                        intervals = floatArrayOf(
                                            totalLen * progress,
                                            totalLen
                                        ),
                                        phase = 0f
                                    )
                                )
                            )
                        }
                    }
                } else Modifier
            )
            .padding(start = 10.dp, end = 20.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .background(Color.White, CircleShape)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = message,
            color = contentColor,
            fontFamily = PretendardFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
    }
}
