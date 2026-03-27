package com.bridgeone.app.ui.connection

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bridgeone.app.R
import com.bridgeone.app.ui.theme.BridgeOneTheme
import com.bridgeone.app.ui.theme.PretendardFontFamily
import com.bridgeone.app.ui.theme.BackgroundPrimary
import com.bridgeone.app.ui.theme.StateWarning

/**
 * USB 동글 미연결 시 표시되는 연결 대기 화면.
 *
 * 단계별 상태에 따라 아이콘, 주 메시지, 부 메시지가 동적으로 변경됩니다.
 * USB 아이콘은 처리 중일 때 2초 주기로 회전합니다.
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
            Toast.makeText(context, "앱을 종료하시겠습니까?", Toast.LENGTH_SHORT).show()
        }
    }

    // --- USB 아이콘 회전 애니메이션 ---
    val infiniteTransition = rememberInfiniteTransition(label = "usbRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing)
        ),
        label = "usbIconRotation"
    )
    val currentRotation = if (connectionState.isProcessing) rotation else 0f

    // --- 접근성: contentDescription ---
    val iconDescription = when (connectionState) {
        is ConnectionState.WaitingForUsb -> "USB 연결 진행 중"
        is ConnectionState.SearchingServer -> "서버 연결 진행 중"
        is ConnectionState.PermissionRequired -> "USB 권한 요청 중"
        is ConnectionState.Error -> "연결 실패, 자동 재시도 중"
    }

    // --- 화면 진입 애니메이션 ---
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundPrimary),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(300)) + slideInVertically(
                animationSpec = tween(300),
                initialOffsetY = { with(density) { 20.dp.roundToPx() } }
            )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .padding(32.dp)
                    .semantics {
                        liveRegion = LiveRegionMode.Polite
                    }
            ) {
                // USB 아이콘 (128dp, 회전)
                Image(
                    painter = painterResource(id = R.drawable.ic_usb),
                    contentDescription = iconDescription,
                    colorFilter = ColorFilter.tint(StateWarning),
                    modifier = Modifier
                        .size(128.dp)
                        .rotate(currentRotation)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 주 메시지 (CrossFade 전환)
                Crossfade(
                    targetState = connectionState.primaryMessage,
                    animationSpec = tween(200),
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

                Spacer(modifier = Modifier.height(16.dp))

                // 부 메시지 (CrossFade 전환)
                Crossfade(
                    targetState = connectionState.secondaryMessage,
                    animationSpec = tween(200),
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
            }
        }
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

@Preview(showBackground = true, name = "오류")
@Composable
private fun ConnectionWaitingScreenErrorPreview() {
    BridgeOneTheme {
        ConnectionWaitingScreen(connectionState = ConnectionState.Error("장치를 찾을 수 없습니다"))
    }
}
