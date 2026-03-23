package com.bridgeone.app.protocol

/**
 * BridgeOne 브릿지 동작 모드 (Phase 3.5.5).
 *
 * ESP32-S3와의 연결 상태에 따라 두 가지 모드로 동작합니다:
 * - ESSENTIAL: Windows 서버 미연결 상태. 기본 기능(마우스 이동 + 좌클릭)만 UI 제공.
 * - STANDARD: Windows 서버 연결 완료 상태. 우클릭, 휠, 드래그, 전체 키보드 등 모든 기능 UI 제공.
 *
 * 전환 조건:
 * - ESSENTIAL → STANDARD: ESP32-S3로부터 EVENT_MODE_CHANGED(Standard) 알림 수신
 * - STANDARD → ESSENTIAL: USB 포트 닫힘 (서버 연결 해제 또는 케이블 분리)
 */
enum class BridgeMode {
    ESSENTIAL,  // 기본 모드 (서버 미연결)
    STANDARD    // 서버 연결 모드
}
