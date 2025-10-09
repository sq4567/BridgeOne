package com.chatterbones.bridgeone.usb

/**
 * USB 연결 상태를 나타내는 봉인 클래스 (Sealed Class)
 * 
 * BridgeOne 프로토콜에서 정의한 USB 연결 관리 상태를 표현합니다.
 * 
 * @see [technical-specification-app.md §1.1.2 연결 관리 요구사항]
 */
sealed class UsbConnectionState {
    /**
     * USB 장치가 연결되지 않은 상태
     * 
     * 초기 상태이거나 USB 장치가 분리된 경우를 나타냅니다.
     */
    data object Disconnected : UsbConnectionState()

    /**
     * USB 장치 접근 권한 요청 중인 상태
     * 
     * CP2102 장치 감지 후 사용자에게 권한을 요청하는 중입니다.
     * PendingIntent를 통해 시스템 권한 다이얼로그가 표시됩니다.
     */
    data object Requesting : UsbConnectionState()

    /**
     * USB 장치 연결 완료 상태
     * 
     * 권한 승인이 완료되어 USB 통신이 가능한 상태입니다.
     * 
     * @param deviceName 연결된 USB 장치 이름 (예: "CP2102 USB to UART Bridge Controller")
     */
    data class Connected(val deviceName: String) : UsbConnectionState()

    /**
     * USB 연결 오류 상태
     * 
     * 권한 거부, 장치 인식 실패, 통신 오류 등 다양한 오류 상황을 나타냅니다.
     * 
     * @param message 오류 메시지 (사용자에게 표시할 오류 설명)
     * @param cause 오류 원인 예외 (디버깅용, nullable)
     */
    data class Error(val message: String, val cause: Throwable? = null) : UsbConnectionState()
}

