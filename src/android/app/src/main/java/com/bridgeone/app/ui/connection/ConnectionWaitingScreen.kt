package com.bridgeone.app.ui.connection

import android.app.Activity
import com.bridgeone.app.ui.common.ToastController
import com.bridgeone.app.ui.common.ToastType
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bridgeone.app.ui.common.BridgeOneLogo
import com.bridgeone.app.ui.theme.BackgroundPrimary
import com.bridgeone.app.ui.theme.BridgeOneTheme
import com.bridgeone.app.ui.theme.PretendardFontFamily
import com.bridgeone.app.ui.theme.StateWarning
import kotlin.math.min

// 배경 그라데이션 색상
private val GradientCenter = Color(0xFF1E1E2E)

/**
 * USB 동글 미연결 시 표시되는 연결 대기 화면.
 *
 * 상단: 별 로고 + 안내 텍스트 + 스텝 인디케이터
 * 하단: USB 케이블을 핸드폰에 꽂는 애니메이션
 */
@Composable
fun ConnectionWaitingScreen(
    connectionState: ConnectionState = ConnectionState.WaitingForUsb,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    // --- 뒤로가기 더블 탭 종료 ---
    var lastBackPressTime by remember { mutableLongStateOf(0L) }
    BackHandler {
        val now = System.currentTimeMillis()
        if (now - lastBackPressTime < 3000L) {
            (context as? Activity)?.finish()
        } else {
            lastBackPressTime = now
            ToastController.show("앱을 종료하시겠습니까?", ToastType.INFO, durationMs = 1000L)
        }
    }

    // --- 화면 진입 애니메이션 ---
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // --- 배경: 방사형 그라데이션 ---
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clearAndSetSemantics { }
        ) {
            drawRect(color = BackgroundPrimary)

            val center = androidx.compose.ui.geometry.Offset(
                size.width / 2f, size.height * 0.35f
            )
            val radius = min(size.width, size.height) * 0.7f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(GradientCenter, Color.Transparent),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
        }

        // --- 상단 별 로고 ---
        BridgeOneLogo(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp),
            size = 32.dp,
            color = Color(0xFF2AA9FF),
            alpha = 0.6f
        )

        // --- 상단 텍스트 영역 (진입 애니메이션) ---
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(300)) + slideInVertically(
                animationSpec = tween(300),
                initialOffsetY = { with(density) { 20.dp.roundToPx() } }
            ),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 120.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .semantics {
                        liveRegion = LiveRegionMode.Polite
                    }
            ) {
                // 주 메시지 (왼쪽 슬라이드 + 스케일 전환)
                AnimatedContent(
                    targetState = connectionState.primaryMessage,
                    transitionSpec = {
                        // 들어오는 텍스트: 오른쪽에서 작고 투명하게 → 커지며 선명해짐
                        (slideInHorizontally(
                            animationSpec = tween(400, easing = FastOutSlowInEasing),
                            initialOffsetX = { it / 4 }
                        ) + fadeIn(tween(400, easing = FastOutSlowInEasing))
                          + scaleIn(tween(400, easing = FastOutSlowInEasing), initialScale = 0.88f)
                        ) togetherWith
                        // 나가는 텍스트: 왼쪽으로 작아지며 사라짐
                        (slideOutHorizontally(
                            animationSpec = tween(350, easing = FastOutLinearInEasing),
                            targetOffsetX = { -it / 4 }
                        ) + fadeOut(tween(300, easing = FastOutLinearInEasing))
                          + scaleOut(tween(350, easing = FastOutLinearInEasing), targetScale = 0.88f)
                        )
                    },
                    label = "primaryMessage"
                ) { message ->
                    Text(
                        text = message,
                        fontFamily = PretendardFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 부 메시지 (왼쪽 슬라이드 + 스케일 전환)
                AnimatedContent(
                    targetState = connectionState.secondaryMessage,
                    transitionSpec = {
                        (slideInHorizontally(
                            animationSpec = tween(400, easing = FastOutSlowInEasing),
                            initialOffsetX = { it / 4 }
                        ) + fadeIn(tween(400, easing = FastOutSlowInEasing))
                          + scaleIn(tween(400, easing = FastOutSlowInEasing), initialScale = 0.88f)
                        ) togetherWith
                        (slideOutHorizontally(
                            animationSpec = tween(350, easing = FastOutLinearInEasing),
                            targetOffsetX = { -it / 4 }
                        ) + fadeOut(tween(300, easing = FastOutLinearInEasing))
                          + scaleOut(tween(350, easing = FastOutLinearInEasing), targetScale = 0.88f)
                        )
                    },
                    label = "secondaryMessage"
                ) { message ->
                    Text(
                        text = message,
                        fontFamily = PretendardFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 16.sp,
                        color = Color(0xFFC2C2C2),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // 스텝 인디케이터
                StepIndicator(currentStep = connectionState.step)
            }
        }

        // --- 하단 USB 꽂기 애니메이션 ---
        UsbPlugAnimation(
            isAnimating = connectionState.isProcessing,
            accentColor = StateWarning,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
        )

    }
}

// ============================================================
// Preview
// ============================================================

@Preview(showBackground = true, name = "USB 대기")
@Composable
private fun ConnectionWaitingScreenPreview() {
    BridgeOneTheme {
        ConnectionWaitingScreen(connectionState = ConnectionState.WaitingForUsb)
    }
}

@Preview(showBackground = true, name = "서버 탐색")
@Composable
private fun ConnectionWaitingScreenSearchingPreview() {
    BridgeOneTheme {
        ConnectionWaitingScreen(connectionState = ConnectionState.SearchingServer)
    }
}

@Preview(showBackground = true, name = "권한 요청")
@Composable
private fun ConnectionWaitingScreenPermissionPreview() {
    BridgeOneTheme {
        ConnectionWaitingScreen(connectionState = ConnectionState.PermissionRequired)
    }
}

@Preview(showBackground = true, name = "Essential 진입")
@Composable
private fun ConnectionWaitingScreenEnteringEssentialPreview() {
    BridgeOneTheme {
        ConnectionWaitingScreen(connectionState = ConnectionState.EnteringEssential)
    }
}

@Preview(showBackground = true, name = "Standard 진입")
@Composable
private fun ConnectionWaitingScreenEnteringStandardPreview() {
    BridgeOneTheme {
        ConnectionWaitingScreen(connectionState = ConnectionState.EnteringStandard)
    }
}

@Preview(showBackground = true, name = "연결 해제")
@Composable
private fun ConnectionWaitingScreenDisconnectedPreview() {
    BridgeOneTheme {
        ConnectionWaitingScreen(connectionState = ConnectionState.Disconnected)
    }
}

@Preview(showBackground = true, name = "오류")
@Composable
private fun ConnectionWaitingScreenErrorPreview() {
    BridgeOneTheme {
        ConnectionWaitingScreen(connectionState = ConnectionState.Error("장치를 찾을 수 없습니다"))
    }
}
