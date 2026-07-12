package com.bridgeone.app.protocol

import com.bridgeone.app.ui.utils.AbsoluteZoomState
import com.bridgeone.app.ui.utils.ZoneRect
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ZoomStateCommand 단위 테스트 (Phase 4.9.7)
 *
 * CRC16-CCITT 정합성, Vendor CDC 프레임 레이아웃, JSON payload 필드를 검증한다.
 */
class ZoomStateCommandTest {

    // ── crc16Ccitt ──────────────────────────────────────────

    @Test
    fun `crc16Ccitt - 표준 체크 문자열 123456789는 0x31C3(CRC-16 XMODEM 골든값)`() {
        // CRC-16/XMODEM(poly=0x1021, init=0x0000, refin/refout=false, xorout=0x0000) 표준 체크값.
        // 펌웨어 vendor_cdc_crc16()과 동일 알고리즘이므로 이 골든값으로 정합성을 검증한다.
        val crc = ZoomStateCommand.crc16Ccitt("123456789".toByteArray(Charsets.US_ASCII))
        assertEquals(0x31C3, crc)
    }

    @Test
    fun `crc16Ccitt - 빈 배열은 0`() {
        assertEquals(0, ZoomStateCommand.crc16Ccitt(ByteArray(0)))
    }

    @Test
    fun `crc16Ccitt - 결과는 항상 16비트 범위(0~0xFFFF)`() {
        val crc = ZoomStateCommand.crc16Ccitt(byteArrayOf(0xFF.toByte(), 0x00, 0x7F, 0x80.toByte()))
        assertTrue(crc in 0..0xFFFF)
    }

    // ── buildPayload ──────────────────────────────────────────

    @Test
    fun `buildPayload - JSON 필드가 스펙과 일치(command,zoom_level,min_max,target_monitor)`() {
        val payload = ZoomStateCommand.buildPayload(
            zoomLevel = 2.0f,
            minX = 8192, minY = 8192, maxX = 24575, maxY = 24575,
            targetMonitor = 1
        )
        val json = JSONObject(payload)
        assertEquals("ZOOM_STATE", json.getString("command"))
        assertEquals(2.0, json.getDouble("zoom_level"), 0.0001)
        assertEquals(8192, json.getInt("min_x"))
        assertEquals(8192, json.getInt("min_y"))
        assertEquals(24575, json.getInt("max_x"))
        assertEquals(24575, json.getInt("max_y"))
        assertEquals(1, json.getInt("target_monitor"))
    }

    @Test
    fun `buildPayload - timestamp 필드는 포함하지 않는다(스펙 예시에 없음)`() {
        val payload = ZoomStateCommand.buildPayload(1f, 0, 0, 32767, 32767, 0)
        val json = JSONObject(payload)
        assertFalse(json.has("timestamp"))
    }

    // ── frame ──────────────────────────────────────────

    @Test
    fun `frame - 레이아웃(헤더,커맨드,길이 LE,payload,CRC LE) 및 총 길이 검증`() {
        val payload = ZoomStateCommand.buildPayload(1f, 0, 0, 32767, 32767, 0)
        val payloadBytes = payload.toByteArray(Charsets.UTF_8)
        val frame = ZoomStateCommand.frame(payload)

        assertEquals("총 길이 = 6 + payload", 6 + payloadBytes.size, frame.size)
        assertEquals("헤더", 0xFF.toByte(), frame[0])
        assertEquals("커맨드", 0x30.toByte(), frame[1])

        val len = (frame[2].toInt() and 0xFF) or ((frame[3].toInt() and 0xFF) shl 8)
        assertEquals("length(LE)", payloadBytes.size, len)

        val payloadInFrame = frame.copyOfRange(4, 4 + payloadBytes.size)
        assertTrue("payload 바이트 일치", payloadInFrame.contentEquals(payloadBytes))

        val crcInFrame = (frame[4 + payloadBytes.size].toInt() and 0xFF) or
            ((frame[5 + payloadBytes.size].toInt() and 0xFF) shl 8)
        val expectedCrc = ZoomStateCommand.crc16Ccitt(payloadBytes)
        assertEquals("CRC16(LE)", expectedCrc, crcInFrame)
    }

    @Test
    fun `frame - MAX_PAYLOAD_BYTES 초과 시 예외`() {
        val oversized = "x".repeat(ZoomStateCommand.MAX_PAYLOAD_BYTES + 1)
        var threw = false
        try {
            ZoomStateCommand.frame(oversized)
        } catch (e: IllegalArgumentException) {
            threw = true
        }
        assertTrue("초과 payload는 IllegalArgumentException", threw)
    }

    @Test
    fun `frame - MAX_PAYLOAD_BYTES 정확히 일치하면 예외 없음`() {
        val exact = "x".repeat(ZoomStateCommand.MAX_PAYLOAD_BYTES)
        val frame = ZoomStateCommand.frame(exact)
        assertEquals(6 + ZoomStateCommand.MAX_PAYLOAD_BYTES, frame.size)
    }

    // ── buildFrame (통합) ──────────────────────────────────────────

    @Test
    fun `buildFrame - level 2x, center 0_5, monitor 1 통합 검증`() {
        val frame = ZoomStateCommand.buildFrame(
            AbsoluteZoomState(level = 2f, centerX = 0.5f, centerY = 0.5f),
            targetMonitor = 1
        )
        assertEquals(0xFF.toByte(), frame[0])
        assertEquals(0x30.toByte(), frame[1])

        val len = (frame[2].toInt() and 0xFF) or ((frame[3].toInt() and 0xFF) shl 8)
        val payloadBytes = frame.copyOfRange(4, 4 + len)
        val json = JSONObject(String(payloadBytes, Charsets.UTF_8))

        assertEquals(2.0, json.getDouble("zoom_level"), 0.0001)
        assertEquals(8192, json.getInt("min_x"))
        assertEquals(24575, json.getInt("max_x"))
        assertEquals(1, json.getInt("target_monitor"))
    }

    @Test
    fun `buildFrame - 1x(해제)는 전체 범위(0,0,32767,32767)`() {
        val frame = ZoomStateCommand.buildFrame(AbsoluteZoomState(), targetMonitor = 0)
        val len = (frame[2].toInt() and 0xFF) or ((frame[3].toInt() and 0xFF) shl 8)
        val payloadBytes = frame.copyOfRange(4, 4 + len)
        val json = JSONObject(String(payloadBytes, Charsets.UTF_8))

        assertEquals(1.0, json.getDouble("zoom_level"), 0.0001)
        assertEquals(0, json.getInt("min_x"))
        assertEquals(0, json.getInt("min_y"))
        assertEquals(32767, json.getInt("max_x"))
        assertEquals(32767, json.getInt("max_y"))
        assertEquals(0, json.getInt("target_monitor"))
    }

    // ── buildFrame(ZoneRect) — 멀티 존 임의 종횡비 인코딩 (Phase 4.9.11) ─────────

    @Test
    fun `buildFrame(pcRect) - 임의 종횡비 직사각형을 축 독립 인코딩(정사각 가정 없음)`() {
        val frame = ZoomStateCommand.buildFrame(
            ZoneRect(minX = 0.1f, minY = 0.2f, maxX = 0.9f, maxY = 0.4f),
            targetMonitor = 2
        )
        val len = (frame[2].toInt() and 0xFF) or ((frame[3].toInt() and 0xFF) shl 8)
        val payloadBytes = frame.copyOfRange(4, 4 + len)
        val json = JSONObject(String(payloadBytes, Charsets.UTF_8))

        assertEquals(3277, json.getInt("min_x"))
        assertEquals(6553, json.getInt("min_y"))
        assertEquals(29490, json.getInt("max_x"))
        assertEquals(13107, json.getInt("max_y"))
        assertEquals(2, json.getInt("target_monitor"))
    }

    @Test
    fun `buildFrame(pcRect) - FULL은 전체 범위(0,0,32767,32767)`() {
        val frame = ZoomStateCommand.buildFrame(ZoneRect.FULL, targetMonitor = 0)
        val len = (frame[2].toInt() and 0xFF) or ((frame[3].toInt() and 0xFF) shl 8)
        val payloadBytes = frame.copyOfRange(4, 4 + len)
        val json = JSONObject(String(payloadBytes, Charsets.UTF_8))

        assertEquals(0, json.getInt("min_x"))
        assertEquals(0, json.getInt("min_y"))
        assertEquals(32767, json.getInt("max_x"))
        assertEquals(32767, json.getInt("max_y"))
    }
}
