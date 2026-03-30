package com.bridgeone.app.ui.connection

/**
 * 연결 대기 화면의 단계별 상태를 정의합니다.
 *
 * 각 상태는 주 메시지, 부 메시지, 아이콘 회전 여부를 포함합니다.
 */
sealed class ConnectionState(
    val primaryMessage: String,
    val secondaryMessage: String,
    val isProcessing: Boolean,
    /** 스텝 인디케이터용 현재 단계 (1~3) */
    val step: Int
) {
    /** 1단계: USB 동글 연결 대기 */
    object WaitingForUsb : ConnectionState(
        primaryMessage = "USB 동글을 연결해주세요",
        secondaryMessage = "OTG 케이블과 동글을 확인해주세요",
        isProcessing = true,
        step = 1
    )

    /** 2단계: 서버 탐색 중 */
    object SearchingServer : ConnectionState(
        primaryMessage = "서버를 찾고 있습니다",
        secondaryMessage = "잠시만 기다려주세요",
        isProcessing = true,
        step = 2
    )

    /** 권한 요청 단계 */
    object PermissionRequired : ConnectionState(
        primaryMessage = "USB 권한을 요청했습니다",
        secondaryMessage = "권한을 허용해주세요",
        isProcessing = false,
        step = 3
    )

    /** Essential 모드 진입 안내 */
    object EnteringEssential : ConnectionState(
        primaryMessage = "Essential 모드로 진입합니다",
        secondaryMessage = "보드가 연결되었습니다",
        isProcessing = false,
        step = 2
    )

    /** Essential 모드 진입 안내 — Windows 서버 미발견(타임아웃) 경우 */
    object EnteringEssentialNoServer : ConnectionState(
        primaryMessage = "Essential 모드로 진입합니다",
        secondaryMessage = "Windows 서버를 찾지 못했습니다",
        isProcessing = false,
        step = 2
    )

    /** Standard 모드 진입 안내 */
    object EnteringStandard : ConnectionState(
        primaryMessage = "Standard 모드로 진입합니다",
        secondaryMessage = "서버와 연결되었습니다",
        isProcessing = false,
        step = 3
    )

    /** 연결 해제됨 — 다시 연결 안내 */
    object Disconnected : ConnectionState(
        primaryMessage = "연결이 해제되었습니다",
        secondaryMessage = "USB 동글을 다시 연결해주세요",
        isProcessing = true,
        step = 1
    )

    /** 오류 단계 */
    data class Error(val errorMessage: String) : ConnectionState(
        primaryMessage = "연결에 실패했습니다",
        secondaryMessage = errorMessage,
        isProcessing = false,
        step = 3
    )
}
