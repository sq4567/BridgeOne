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
import com.bridgeone.app.ui.connection.ConnectionState
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

    // 연결 대기 화면의 현재 ConnectionState (자동 진행용)
    var connectionState by remember { mutableStateOf<ConnectionState>(ConnectionState.WaitingForUsb) }

    // 자동 진행 중 여부 (이미 연결된 상태에서 스텝 애니메이션 진행 중)
    var isAutoProgressing by remember { mutableStateOf(false) }

    // Splash 완료 콜백 → 항상 WaitingForConnection 경유
    val onSplashFinished = remember {
        {
            appState = AppState.WaitingForConnection
        }
    }

    // WaitingForConnection 진입 시 이미 USB 연결되어 있으면 스텝 자동 진행
    LaunchedEffect(appState) {
        if (appState !is AppState.WaitingForConnection) {
            isAutoProgressing = false
            return@LaunchedEffect
        }

        // 현재 USB 상태 한 번 스캔
        UsbSerialManager.scanAndUpdateDebugState(context)
        // 스캔 결과 반영 대기
        delay(100L)

        val connected = UsbSerialManager.debugState.value.isConnected
        val mode = UsbSerialManager.bridgeMode.value

        if (connected) {
            // 이미 연결된 상태 → 스텝 자동 진행 애니메이션
            isAutoProgressing = true
            connectionState = ConnectionState.WaitingForUsb

            // Step 1 → Step 2 진행
            delay(600L)
            connectionState = ConnectionState.SearchingServer

            if (mode == BridgeMode.STANDARD) {
                // 서버까지 연결 → Step 3 진행 후 Standard 안내
                delay(600L)
                connectionState = ConnectionState.EnteringStandard
                delay(1200L)
                isAutoProgressing = false
                appState = AppState.Active(BridgeMode.STANDARD)
            } else {
                // 보드만 연결 → Essential 안내
                delay(600L)
                connectionState = ConnectionState.EnteringEssential
                delay(1200L)
                isAutoProgressing = false
                appState = AppState.Active(BridgeMode.ESSENTIAL)
            }
        } else {
            // 미연결 → Disconnected 상태가 아닌 경우에만 WaitingForUsb로 변경
            if (connectionState !is ConnectionState.Disconnected) {
                connectionState = ConnectionState.WaitingForUsb
            }
            // USB 폴링 시작
            while (true) {
                UsbSerialManager.scanAndUpdateDebugState(context)
                delay(2000L)
            }
        }
    }

    // USB 연결 상태 변화 감시 (Splash/자동진행 중에는 반응하지 않음)
    LaunchedEffect(isUsbConnected) {
        if (appState is AppState.Splash || isAutoProgressing) return@LaunchedEffect

        if (isUsbConnected) {
            if (appState is AppState.WaitingForConnection) {
                // 대기 중 USB 연결됨 → 스텝 자동 진행 시작
                isAutoProgressing = true
                connectionState = ConnectionState.SearchingServer

                val mode = UsbSerialManager.bridgeMode.value
                if (mode == BridgeMode.STANDARD) {
                    delay(600L)
                    connectionState = ConnectionState.EnteringStandard
                    delay(1200L)
                    isAutoProgressing = false
                    appState = AppState.Active(BridgeMode.STANDARD)
                } else {
                    delay(600L)
                    connectionState = ConnectionState.EnteringEssential
                    delay(1200L)
                    isAutoProgressing = false
                    appState = AppState.Active(BridgeMode.ESSENTIAL)
                }
            }
        } else {
            // USB 해제됨 → WaitingForConnection으로 복귀
            // Active 상태에서 해제된 경우 재연결 안내, 그 외는 기본 대기
            connectionState = if (appState is AppState.Active) {
                ConnectionState.Disconnected
            } else {
                ConnectionState.WaitingForUsb
            }
            appState = AppState.WaitingForConnection
        }
    }

    // Active 상태에서 bridgeMode 변경 추적 (USB 연결 중일 때만)
    LaunchedEffect(bridgeMode) {
        if (appState is AppState.Active && isUsbConnected) {
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
                    ConnectionWaitingScreen(connectionState = connectionState)
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
