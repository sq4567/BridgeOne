package com.bridgeone.app.protocol

/**
 * ESP32-S3 → Android 역방향 UART 알림 프레임 (Phase 3.5.4).
 *
 * ESP32-S3가 브릿지 모드 변경 등 이벤트 발생 시 UART TX로 전송하는 8바이트 알림 프레임.
 * Android는 수신한 8바이트의 첫 바이트가 0xFE이면 이 클래스로 파싱합니다.
 *
 * 프레임 구조 (8바이트):
 * ┌────────┬────────────┬────────┬──────────────────────────┐
 * │ Header │ Event Type │  Data  │      Reserved (5B)       │
 * │  0xFE  │    1B      │   1B   │  0x00 * 5                │
 * └────────┴────────────┴────────┴──────────────────────────┘
 */
data class NotificationFrame(
    val eventType: UByte,
    val data: UByte
) {
    companion object {
        /** 역방향 알림 프레임 헤더 바이트 (0xFE) */
        val HEADER: UByte = 0xFEu

        /** 이벤트 타입: 브릿지 모드 변경 */
        val EVENT_MODE_CHANGED: UByte = 0x01u

        /** 이벤트 타입: 연결 상태 변경 (향후 확장용) */
        val EVENT_CONNECTION_STATE: UByte = 0x02u

        /** 데이터: Essential 모드 */
        val MODE_ESSENTIAL: UByte = 0x00u

        /** 데이터: Standard 모드 */
        val MODE_STANDARD: UByte = 0x01u

        /** 알림 프레임 크기 (바이트) */
        const val FRAME_SIZE = 8

        /**
         * 8바이트 배열에서 NotificationFrame을 파싱합니다.
         *
         * @param bytes 수신한 8바이트 배열
         * @return 파싱 성공 시 NotificationFrame, 실패 시 null
         */
        fun parse(bytes: ByteArray): NotificationFrame? {
            if (bytes.size < FRAME_SIZE) return null
            if (bytes[0].toUByte() != HEADER) return null
            return NotificationFrame(
                eventType = bytes[1].toUByte(),
                data = bytes[2].toUByte()
            )
        }
    }
}
