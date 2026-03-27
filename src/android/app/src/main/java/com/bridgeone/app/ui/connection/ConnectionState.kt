package com.bridgeone.app.ui.connection

/**
 * 연결 대기 화면의 단계별 상태를 정의합니다.
 *
 * 각 상태는 주 메시지, 부 메시지, 아이콘 회전 여부를 포함합니다.
 */
sealed class ConnectionState(
    val primaryMessage: String,
    val secondaryMessage: String,
    val isProcessing: Boolean
) {
    /** 1단계: USB 동글 연결 대기 */
    object WaitingForUsb : ConnectionState(
        primaryMessage = "USB 동글을 연결해주세요",
        secondaryMessage = "OTG 케이블과 동글을 확인해주세요",
        isProcessing = true
    )

    /** 2단계: 서버 탐색 중 */
    object SearchingServer : ConnectionState(
        primaryMessage = "서버를 찾고 있습니다",
        secondaryMessage = "잠시만 기다려주세요",
        isProcessing = true
    )

    /** 권한 요청 단계 */
    object PermissionRequired : ConnectionState(
        primaryMessage = "USB 권한을 요청했습니다",
        secondaryMessage = "권한을 허용해주세요",
        isProcessing = false
    )

    /** 오류 단계 */
    data class Error(val errorMessage: String) : ConnectionState(
        primaryMessage = "연결에 실패했습니다",
        secondaryMessage = errorMessage,
        isProcessing = false
    )
}
