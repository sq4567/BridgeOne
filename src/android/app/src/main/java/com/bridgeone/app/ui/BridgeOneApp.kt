package com.bridgeone.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import android.util.Log
import com.bridgeone.app.protocol.BridgeMode
import com.bridgeone.app.ui.connection.ConnectionWaitingScreen
import com.bridgeone.app.ui.debug.UsbDebugPanel
import com.bridgeone.app.ui.pages.EssentialModePage
import com.bridgeone.app.ui.pages.StandardModePage
import com.bridgeone.app.ui.splash.SplashScreen
import com.bridgeone.app.ui.theme.BridgeOneTheme
import com.bridgeone.app.usb.UsbSerialManager
import kotlinx.coroutines.delay

// ============================================================
// 최상위 Composable
// ============================================================

/**
 * BridgeOne 앱의 최상위 Composable 함수입니다.
 *
 * 앱 상태 머신: Splash → WaitingForConnection → Active(ESSENTIAL/STANDARD)
 */
@Composable
fun BridgeOneApp() {
    val context = LocalContext.current
    val debugState by UsbSerialManager.debugState.collectAsState()
    val bridgeMode by UsbSerialManager.bridgeMode.collectAsState()
    val isUsbConnected = debugState.isConnected

    // ========== 앱 상태 머신 ==========
    var appState by remember { mutableStateOf<AppState>(AppState.Splash) }
    var showDebugPanel by remember { mutableStateOf(true) }

    // Splash 완료 콜백 → WaitingForConnection 전환
    val onSplashFinished = remember {
        {
            appState = if (isUsbConnected) {
                AppState.Active(bridgeMode)
            } else {
                AppState.WaitingForConnection
            }
        }
    }

    // USB 연결 상태 변화 감시 (Splash 이후에만 반응)
    LaunchedEffect(isUsbConnected) {
        if (appState is AppState.Splash) return@LaunchedEffect

        if (isUsbConnected) {
            // USB 연결됨 → Active 상태로 전환
            appState = AppState.Active(bridgeMode)
        } else {
            // USB 해제됨 → WaitingForConnection으로 복귀
            appState = AppState.WaitingForConnection
        }
    }

    // Active 상태에서 bridgeMode 변경 추적
    LaunchedEffect(bridgeMode) {
        if (appState is AppState.Active) {
            appState = AppState.Active(bridgeMode)
        }
    }

    // Active 상태에서만 모드 전환 토스트
    var hasShownFirstModeInActive by remember { mutableStateOf(false) }
    LaunchedEffect(bridgeMode, appState) {
        if (appState !is AppState.Active) {
            hasShownFirstModeInActive = false
            return@LaunchedEffect
        }
        if (!hasShownFirstModeInActive) {
            hasShownFirstModeInActive = true
            return@LaunchedEffect
        }
        val message = when (bridgeMode) {
            BridgeMode.STANDARD -> "Standard 모드로 전환되었습니다"
            BridgeMode.ESSENTIAL -> "Essential 모드로 전환되었습니다"
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        Log.i("BridgeOneApp", "BridgeMode toast: $message")
    }

    // WaitingForConnection 상태에서만 USB 상태 폴링 (2초 주기)
    LaunchedEffect(appState) {
        if (appState !is AppState.WaitingForConnection) return@LaunchedEffect
        while (true) {
            UsbSerialManager.scanAndUpdateDebugState(context)
            delay(2000L)
        }
    }

    // ========== UI 렌더링 ==========
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 화면 전환 애니메이션
        AnimatedContent(
            targetState = appState,
            transitionSpec = {
                when {
                    // Splash → WaitingForConnection: Fade Out
                    initialState is AppState.Splash ->
                        fadeIn(tween(0)) togetherWith fadeOut(tween(200))
                    // WaitingForConnection → Active: Fade In
                    initialState is AppState.WaitingForConnection && targetState is AppState.Active ->
                        fadeIn(tween(300)) togetherWith fadeOut(tween(0))
                    // Active → WaitingForConnection: 즉시 전환
                    initialState is AppState.Active && targetState is AppState.WaitingForConnection ->
                        fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                    // 기타 (Active 내 모드 변경 등)
                    else ->
                        fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                }
            },
            label = "appStateTransition"
        ) { state ->
            when (state) {
                is AppState.Splash -> {
                    SplashScreen(onSplashComplete = onSplashFinished)
                }
                is AppState.WaitingForConnection -> {
                    ConnectionWaitingScreen()
                }
                is AppState.Active -> {
                    // 메인 콘텐츠 (하단 정렬)
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        when (state.bridgeMode) {
                            BridgeMode.ESSENTIAL -> EssentialModePage()
                            BridgeMode.STANDARD -> StandardModePage()
                        }
                    }
                }
            }
        }

        // 디버그 패널 (Active 상태에서만 표시)
        if (appState is AppState.Active) {
            if (showDebugPanel) {
                UsbDebugPanel(
                    debugState = debugState,
                    onClose = { showDebugPanel = false },
                    onRefresh = { UsbSerialManager.scanAndUpdateDebugState(context) },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 32.dp)
                )
            } else {
                Text(
                    text = "🔍 USB Debug",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 40.dp, end = 16.dp)
                        .clickable { showDebugPanel = true }
                )
            }
        }

    }
}

// ============================================================
// Preview
// ============================================================

@Preview(showBackground = true)
@Composable
private fun BridgeOneAppPreview() {
    BridgeOneTheme {
        BridgeOneApp()
    }
}
