package com.bridgeone.app.protocol

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for NotificationFrame parsing (Phase 4.9.5: EVENT_MONITOR_COUNT 검증)
 */
class NotificationFrameTest {

    @Test
    fun testParseEventMonitorCount() {
        val bytes = byteArrayOf(0xFE.toByte(), 0x03, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00)
        val frame = NotificationFrame.parse(bytes)

        assertNotNull(frame)
        assertEquals("eventType", NotificationFrame.EVENT_MONITOR_COUNT, frame!!.eventType)
        assertEquals("data (monitor_count)", 2u.toUByte(), frame.data)
    }

    @Test
    fun testParseRejectsWrongHeader() {
        val bytes = byteArrayOf(0x00, 0x03, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00)
        assertNull(NotificationFrame.parse(bytes))
    }

    @Test
    fun testParseRejectsShortFrame() {
        val bytes = byteArrayOf(0xFE.toByte(), 0x03, 0x02)
        assertNull(NotificationFrame.parse(bytes))
    }

    @Test
    fun testEventMonitorCountConstantValue() {
        assertEquals(0x03u.toUByte(), NotificationFrame.EVENT_MONITOR_COUNT)
    }
}
