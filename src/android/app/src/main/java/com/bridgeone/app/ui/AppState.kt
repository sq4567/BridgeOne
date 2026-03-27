package com.bridgeone.app.ui

import com.bridgeone.app.protocol.BridgeMode

/**
 * 앱 전체 화면 상태를 정의하는 sealed class.
 *
 * Splash → WaitingForConnection → Active 흐름을 관리합니다.
 */
sealed class AppState {
    /** 스플래시 스크린 표시 중 */
    object Splash : AppState()

    /** USB 동글 연결 대기 중 */
    object WaitingForConnection : AppState()

    /** USB 연결 완료, 실제 사용 중 */
    data class Active(val bridgeMode: BridgeMode) : AppState()
}
