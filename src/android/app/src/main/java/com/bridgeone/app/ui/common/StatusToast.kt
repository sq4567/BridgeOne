package com.bridgeone.app.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
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
)

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

// 살짝 튀어 들어오는 탄력감 있는 등장
private val EaseOutBack = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)
// 살짝 당겨졌다가 위로 사라지는 퇴장
private val EaseInBack = CubicBezierEasing(0.36f, 0f, 0.66f, -0.56f)

private const val ENTER_DURATION_MS = 350
private const val EXIT_DURATION_MS = 300

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
 */
@Composable
fun StatusToastOverlay() {
    val toastMessage by ToastController.current.collectAsState()
    var isVisible by remember { mutableStateOf(false) }
    var displayedMessage by remember { mutableStateOf<ToastMessage?>(null) }

    LaunchedEffect(toastMessage) {
        val msg = toastMessage
        if (msg != null) {
            displayedMessage = msg
            isVisible = true
            if (msg.durationMs != TOAST_DURATION_INFINITE) {
                delay(msg.durationMs)
                // 아직 같은 메시지가 표시 중인 경우에만 자동 사라짐
                if (ToastController.current.value == msg) {
                    isVisible = false
                    delay(EXIT_DURATION_MS.toLong() + 50L)
                    if (ToastController.current.value == msg) {
                        ToastController.dismiss()
                    }
                }
            }
        } else {
            isVisible = false
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = tween(durationMillis = ENTER_DURATION_MS, easing = EaseOutBack)
            ) + fadeIn(animationSpec = tween(durationMillis = 150)),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(durationMillis = EXIT_DURATION_MS, easing = EaseInBack)
            ) + fadeOut(animationSpec = tween(durationMillis = 200))
        ) {
            displayedMessage?.let { msg ->
                StatusToastContent(message = msg.message, type = msg.type)
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
    type: ToastType
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

    Row(
        modifier = Modifier
            .statusBarsPadding()
            .padding(top = 35.dp, start = 16.dp, end = 16.dp)
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = Color.Black.copy(alpha = 0.3f),
                spotColor = Color.Black.copy(alpha = 0.3f)
            )
            .background(backgroundColor, RoundedCornerShape(28.dp))
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
