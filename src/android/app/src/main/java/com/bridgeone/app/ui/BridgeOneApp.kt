package com.bridgeone.app.ui

import android.app.Activity
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bridgeone.app.protocol.BridgeMode
import com.bridgeone.app.ui.common.BOTTOM_SAFE_ZONE
import com.bridgeone.app.ui.common.StatusToastOverlay
import com.bridgeone.app.ui.common.TOP_SAFE_ZONE
import com.bridgeone.app.ui.common.ToastController
import com.bridgeone.app.ui.common.ToastType
import com.bridgeone.app.ui.connection.ConnectionState
import com.bridgeone.app.ui.connection.ConnectionWaitingScreen
import com.bridgeone.app.ui.pages.EssentialModePage
import com.bridgeone.app.ui.pages.StandardModePage
import com.bridgeone.app.ui.splash.SplashScreen
import com.bridgeone.app.ui.theme.BridgeOneTheme
import com.bridgeone.app.usb.UsbSerialManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ============================================================
// 상수
// ============================================================

private const val EXIT_CONFIRMATION_TOAST_DURATION_MS = 2000L

// [DEV] true → USB 연결 없이 UI 테스트 (Splash 후 바로 Active, 모드 전환 버튼 표시)
private const val DEV_SKIP_CONNECTION = false

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
    val coroutineScope = rememberCoroutineScope()
    val debugState by UsbSerialManager.debugState.collectAsState()
    val bridgeMode by UsbSerialManager.bridgeMode.collectAsState()
    val isUsbConnected = debugState.isConnected

    // ========== 앱 상태 머신 ==========
    var appState by remember { mutableStateOf<AppState>(AppState.Splash) }

    // ========== 뒤로가기 더블 탭 종료 ==========
    // Splash를 제외한 모든 상태에서 동작: 첫 번째 → 토스트, 두 번째(토스트 표시 중) → 종료
    var exitToastShowing by remember { mutableStateOf(false) }
    BackHandler(enabled = appState !is AppState.Splash) {
        if (exitToastShowing) {
            (context as? Activity)?.finish()
        } else {
            exitToastShowing = true
            ToastController.show("BridgeOne을 종료하려면 한 번 더 터치하세요", ToastType.INFO, durationMs = EXIT_CONFIRMATION_TOAST_DURATION_MS)
            coroutineScope.launch {
                delay(EXIT_CONFIRMATION_TOAST_DURATION_MS)
                exitToastShowing = false
            }
        }
    }

    // 연결 대기 화면의 현재 ConnectionState (자동 진행용)
    var connectionState by remember { mutableStateOf<ConnectionState>(ConnectionState.WaitingForUsb) }

    // 자동 진행 중 여부 (이미 연결된 상태에서 스텝 애니메이션 진행 중)
    var isAutoProgressing by remember { mutableStateOf(false) }

    // [DEV] 임시 모드 전환 상태 (USB 연결 없이 UI 테스트용)
    var devMode by remember { mutableStateOf<BridgeMode>(BridgeMode.ESSENTIAL) }

    // Splash 완료 콜백
    val onSplashFinished = remember {
        {
            appState = if (DEV_SKIP_CONNECTION) AppState.Active(devMode) else AppState.WaitingForConnection
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

        if (connected) {
            // 이미 연결된 상태 → 스텝 자동 진행 애니메이션
            isAutoProgressing = true
            connectionState = ConnectionState.WaitingForUsb

            // Step 1 → Step 2: 서버 탐색 중 (모드 확정 대기)
            delay(600L)
            connectionState = ConnectionState.SearchingServer

            // 모드 확정 대기 (최대 5초, 100ms 간격)
            val mode = waitForModeConfirmed()

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

                // 모드 확정 대기 (최대 5초)
                val mode = waitForModeConfirmed()

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
        val (message, toastType) = when ((appState as AppState.Active).bridgeMode) {
            BridgeMode.STANDARD -> "Standard 모드로 전환되었습니다" to ToastType.INFO
            BridgeMode.ESSENTIAL -> "Essential 모드로 전환되었습니다" to ToastType.WARNING
        }
        ToastController.show(message, toastType, durationMs = 2000L)
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
                    // Safe Zone: 상하단 터치 불가 영역을 확보하여 컴포넌트 오조작을 방지합니다.
                    // 값 조정 시 LayoutConstants.kt의 TOP_SAFE_ZONE / BOTTOM_SAFE_ZONE을 수정하세요.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = TOP_SAFE_ZONE, bottom = BOTTOM_SAFE_ZONE),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        when (state.bridgeMode) {
                            BridgeMode.ESSENTIAL -> EssentialModePage()
                            BridgeMode.STANDARD -> StandardModePage()
                        }
                    }

                    // [DEV] 임시 모드 전환 버튼 (좌상단 고정)
                    if (DEV_SKIP_CONNECTION) {
                        val nextMode = if (state.bridgeMode == BridgeMode.ESSENTIAL) BridgeMode.STANDARD else BridgeMode.ESSENTIAL
                        val btnColor = if (state.bridgeMode == BridgeMode.ESSENTIAL) Color(0xFF1565C0) else Color(0xFF6A1B9A)
                        Button(
                            onClick = {
                                devMode = nextMode
                                appState = AppState.Active(nextMode)
                            },
                            modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = btnColor)
                        ) {
                            Text(
                                text = "→ ${if (nextMode == BridgeMode.STANDARD) "Standard" else "Essential"}",
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // 커스텀 토스트 오버레이 (항상 최상단에 렌더링)
        StatusToastOverlay()
    }
}

// ============================================================
// 모드 확정 대기
// ============================================================

/**
 * ESP32로부터 모드 응답이 올 때까지 대기합니다.
 * 최대 5초 대기 후 타임아웃 시 현재 bridgeMode 값을 반환합니다.
 */
private suspend fun waitForModeConfirmed(): BridgeMode {
    val maxWaitMs = 5000L
    val intervalMs = 100L
    var elapsed = 0L

    while (elapsed < maxWaitMs) {
        if (UsbSerialManager.modeConfirmed.value) {
            return UsbSerialManager.bridgeMode.value
        }
        delay(intervalMs)
        elapsed += intervalMs
    }
    Log.w("BridgeOneApp", "Mode confirmation timed out (${maxWaitMs}ms), using current: ${UsbSerialManager.bridgeMode.value}")
    return UsbSerialManager.bridgeMode.value
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
