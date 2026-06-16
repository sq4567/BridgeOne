package com.bridgeone.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

// ============================================================
// MacroOverlayController — 전역 싱글톤 (ToastController 패턴)
// ============================================================

object MacroOverlayController {
    private val _isBlocking = MutableStateFlow(false)
    val isBlocking = _isBlocking.asStateFlow()

    fun show() { _isBlocking.value = true }
    fun dismiss() { _isBlocking.value = false }
}

// ============================================================
// MacroScrimOverlay — BridgeOneApp 최상위 Box에 배치
// ============================================================

/**
 * 매크로 실행 중 화면 전체를 반투명으로 덮어 모든 터치 입력을 차단하는 오버레이.
 *
 * BridgeOneApp의 최상위 Box에서 StatusToastOverlay() 바로 앞에 배치.
 * 토스트는 이 스크림 위로 보이게 됩니다.
 *
 * MacroOverlayController.show() / dismiss()로 표시를 제어합니다.
 */
@Composable
fun MacroScrimOverlay() {
    val blocking by MacroOverlayController.isBlocking.collectAsState()
    if (!blocking) return
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = MACRO_SCRIM_ALPHA))
            .pointerInput(Unit) {
                awaitEachGesture {
                    while (true) {
                        awaitPointerEvent().changes.forEach { it.consume() }
                    }
                }
            }
    )
}
