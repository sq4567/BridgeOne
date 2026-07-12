package com.bridgeone.app.protocol

import com.bridgeone.app.ui.utils.AbsoluteCoordinateCalculator
import com.bridgeone.app.ui.utils.AbsoluteZoomState
import com.bridgeone.app.ui.utils.ZoneRect
import org.json.JSONObject

/**
 * Vendor CDC 줌 상태 커스텀 명령 빌더 (Phase 4.9.7).
 *
 * 절대좌표 패드(Page 3)의 줌 상태(zoom_level, 매핑 범위, target_monitor)를 JSON payload로
 * 인코딩하고 [0xFF][command][length 2B LE][payload UTF-8][CRC16 2B LE] 프레임으로 조립한다.
 * ESP32는 이 프레임을 파싱하지 않고 Vendor CDC Frame으로 감싸 Windows 서버로 투명 중계한다
 * (ESP32 중계·PC 오버레이 렌더링은 본 Phase 범위 밖).
 *
 * CRC16-CCITT(다항식 0x1021, 초기값 0x0000, payload만 대상)는 펌웨어
 * src/board/BridgeOne/main/vendor_cdc_handler.c의 vendor_cdc_crc16()과 동일 알고리즘이다.
 *
 * 참조: docs/technical-specification.md §2.4.6.1.2
 */
object ZoomStateCommand {

    /** JSON payload의 command 필드 값. */
    const val COMMAND_ZOOM_STATE = "ZOOM_STATE"

    /** Vendor CDC 프레임 헤더(프로토콜 예약 바이트). 기본값: 0xFF */
    const val VENDOR_CDC_HEADER = 0xFF

    /** 줌 상태 서브커맨드 VCDC_CMD_ZOOM_STATE. 기본값: 0x30 */
    const val VCDC_CMD_ZOOM_STATE = 0x30

    /** payload 최대 바이트 (펌웨어 VCDC_MAX_PAYLOAD_SIZE와 동일). 기본값: 448 */
    const val MAX_PAYLOAD_BYTES = 448

    /**
     * 줌 상태 JSON payload 문자열을 생성합니다 (순수 함수, 단위테스트 대상).
     * timestamp 필드는 스펙 예시(technical-specification.md §2.4.6.1.2)에 없어 포함하지 않는다.
     */
    fun buildPayload(
        zoomLevel: Float,
        minX: Int,
        minY: Int,
        maxX: Int,
        maxY: Int,
        targetMonitor: Int
    ): String = JSONObject().apply {
        put("command", COMMAND_ZOOM_STATE)
        put("zoom_level", zoomLevel.toDouble())
        put("min_x", minX)
        put("min_y", minY)
        put("max_x", maxX)
        put("max_y", maxY)
        put("target_monitor", targetMonitor)
    }.toString()

    /**
     * CRC16-CCITT(다항식 0x1021, 초기값 0x0000)를 계산합니다.
     * 펌웨어 vendor_cdc_crc16()(vendor_cdc_handler.c)의 정확한 포팅입니다.
     */
    fun crc16Ccitt(data: ByteArray): Int {
        var crc = 0x0000
        for (b in data) {
            crc = crc xor ((b.toInt() and 0xFF) shl 8)
            repeat(8) {
                crc = if (crc and 0x8000 != 0) (crc shl 1) xor 0x1021 else crc shl 1
                crc = crc and 0xFFFF
            }
        }
        return crc and 0xFFFF
    }

    /**
     * payload 문자열을 완성된 Vendor CDC 프레임 바이트로 조립합니다.
     * 레이아웃: [0xFF][0x30][length 2B LE][payload UTF-8][CRC16 2B LE]
     *
     * @throws IllegalArgumentException payload가 MAX_PAYLOAD_BYTES를 초과하는 경우
     */
    fun frame(payload: String): ByteArray {
        val payloadBytes = payload.toByteArray(Charsets.UTF_8)
        require(payloadBytes.size <= MAX_PAYLOAD_BYTES) {
            "payload too large: ${payloadBytes.size} > $MAX_PAYLOAD_BYTES"
        }
        val len = payloadBytes.size
        val crc = crc16Ccitt(payloadBytes)
        return ByteArray(6 + len).also { f ->
            f[0] = VENDOR_CDC_HEADER.toByte()
            f[1] = VCDC_CMD_ZOOM_STATE.toByte()
            f[2] = (len and 0xFF).toByte()
            f[3] = ((len shr 8) and 0xFF).toByte()
            System.arraycopy(payloadBytes, 0, f, 4, len)
            f[4 + len] = (crc and 0xFF).toByte()
            f[5 + len] = ((crc shr 8) and 0xFF).toByte()
        }
    }

    /**
     * 줌 상태와 대상 모니터로부터 완성된 Vendor CDC 프레임을 만듭니다 (호출측 편의 함수).
     * min/max 매핑 범위는 AbsoluteCoordinateCalculator.calculateZoomMappingRange()로 산출한다.
     */
    fun buildFrame(zoom: AbsoluteZoomState, targetMonitor: Int): ByteArray {
        val range = AbsoluteCoordinateCalculator.calculateZoomMappingRange(zoom)
        val payload = buildPayload(
            zoomLevel = zoom.level,
            minX = range.minX,
            minY = range.minY,
            maxX = range.maxX,
            maxY = range.maxY,
            targetMonitor = targetMonitor
        )
        return frame(payload)
    }

    /**
     * 멀티 존 PC 매핑 직사각형([ZoneRect], 임의 종횡비)과 대상 모니터로부터 완성된 Vendor CDC
     * 프레임을 만듭니다 (Phase 4.9.11). min/max 매핑 범위는 AbsoluteCoordinateCalculator.
     * zoneRectToMappingRange()로 산출한다(축 독립 인코딩, 정사각 윈도우 가정 없음).
     * zoom_level 필드는 표시용으로 zoomLevelFromPcRect()(정사각 가정 역산)를 사용한다.
     */
    fun buildFrame(pcRect: ZoneRect, targetMonitor: Int): ByteArray {
        val range = AbsoluteCoordinateCalculator.zoneRectToMappingRange(pcRect)
        val payload = buildPayload(
            zoomLevel = AbsoluteCoordinateCalculator.zoomLevelFromPcRect(pcRect),
            minX = range.minX,
            minY = range.minY,
            maxX = range.maxX,
            maxY = range.maxY,
            targetMonitor = targetMonitor
        )
        return frame(payload)
    }
}
