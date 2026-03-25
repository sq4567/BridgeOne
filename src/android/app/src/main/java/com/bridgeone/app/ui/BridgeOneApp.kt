package com.bridgeone.app.ui

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
import com.bridgeone.app.protocol.BridgeMode
import com.bridgeone.app.ui.debug.UsbDebugPanel
import com.bridgeone.app.ui.pages.EssentialModePage
import com.bridgeone.app.ui.pages.StandardModePage
import com.bridgeone.app.ui.theme.BridgeOneTheme
import com.bridgeone.app.usb.UsbSerialManager
import android.util.Log
import kotlinx.coroutines.delay

// ============================================================
// 최상위 Composable
// ============================================================

/**
 * BridgeOne 앱의 최상위 Composable 함수입니다.
 */
@Composable
fun BridgeOneApp() {
    val context = LocalContext.current
    val debugState by UsbSerialManager.debugState.collectAsState()
    val bridgeMode by UsbSerialManager.bridgeMode.collectAsState()

    var showDebugPanel by remember { mutableStateOf(true) }

    // Phase 3.5.5: 브릿지 모드 전환 시 토스트 알림
    var isFirstMode by remember { mutableStateOf(true) }
    LaunchedEffect(bridgeMode) {
        if (isFirstMode) {
            isFirstMode = false
            return@LaunchedEffect
        }
        val message = when (bridgeMode) {
            BridgeMode.STANDARD -> "Standard 모드로 전환되었습니다"
            BridgeMode.ESSENTIAL -> "Essential 모드로 전환되었습니다"
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        Log.i("BridgeOneApp", "BridgeMode toast: $message")
    }

    // 주기적으로 USB 상태 스캔 (2초마다)
    LaunchedEffect(Unit) {
        while (true) {
            UsbSerialManager.scanAndUpdateDebugState(context)
            delay(2000L)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 메인 콘텐츠 (하단 정렬)
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            MainContent()
        }

        // 디버그 패널 (상단)
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

// ============================================================
// MainContent: 모드별 페이지 분기
// ============================================================

@Composable
private fun MainContent() {
    val bridgeMode by UsbSerialManager.bridgeMode.collectAsState()

    when (bridgeMode) {
        BridgeMode.ESSENTIAL -> EssentialModePage()
        BridgeMode.STANDARD -> StandardModePage()
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
