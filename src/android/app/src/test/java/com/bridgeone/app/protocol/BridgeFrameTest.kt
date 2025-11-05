package com.bridgeone.app.protocol

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for BridgeFrame data class
 *
 * Verifies frame structure, serialization, helper functions, and constants.
 */
class BridgeFrameTest {

    /**
     * Test: Frame size is exactly 8 bytes
     */
    @Test
    fun testFrameSize() {
        val frame = BridgeFrame(
            seq = 1u,
            buttons = 0x01u,
            deltaX = 10,
            deltaY = -5,
            wheel = 0,
            modifiers = 0u,
            keyCode1 = 0u,
            keyCode2 = 0u
        )

        val bytes = frame.toByteArray()

        assertEquals("frame size should be 8 bytes", BridgeFrame.FRAME_SIZE_BYTES, bytes.size)
    }

    /**
     * Test: toByteArray() serialization byte order
     */
    @Test
    fun testToByteArrayByteOrder() {
        val frame = BridgeFrame(
            seq = 1u,
            buttons = 0x02u,
            deltaX = 10,
            deltaY = (-5).toByte(),
            wheel = 0,
            modifiers = 0x04u,
            keyCode1 = 65u,
            keyCode2 = 66u
        )

        val bytes = frame.toByteArray()

        assertEquals("seq byte", 1.toByte(), bytes[0])
        assertEquals("buttons byte", 2.toByte(), bytes[1])
        assertEquals("deltaX byte", 10.toByte(), bytes[2])
        assertEquals("deltaY byte", (-5).toByte(), bytes[3])
        assertEquals("wheel byte", 0.toByte(), bytes[4])
        assertEquals("modifiers byte", 4.toByte(), bytes[5])
        assertEquals("keyCode1 byte", 65.toByte(), bytes[6])
        assertEquals("keyCode2 byte", 66.toByte(), bytes[7])
    }

    /**
     * Test: BridgeFrame.default() creates all-zero frame
     */
    @Test
    fun testDefaultFrame() {
        val frame = BridgeFrame.default()

        assertEquals("seq default", 0.toUByte(), frame.seq)
        assertEquals("buttons default", 0.toUByte(), frame.buttons)
        assertEquals("deltaX default", 0.toByte(), frame.deltaX)
        assertEquals("deltaY default", 0.toByte(), frame.deltaY)
        assertEquals("wheel default", 0.toByte(), frame.wheel)
        assertEquals("modifiers default", 0.toUByte(), frame.modifiers)
        assertEquals("keyCode1 default", 0.toUByte(), frame.keyCode1)
        assertEquals("keyCode2 default", 0.toUByte(), frame.keyCode2)
    }

    /**
     * Test: Default frame serializes to all zeros
     */
    @Test
    fun testDefaultFrameSerialization() {
        val frame = BridgeFrame.default()
        val bytes = frame.toByteArray()

        for (i in bytes.indices) {
            assertEquals("byte[$i] should be 0x00", 0.toByte(), bytes[i])
        }
    }

    /**
     * Test: isLeftClickPressed() helper function
     */
    @Test
    fun testIsLeftClickPressed() {
        val framePressed = BridgeFrame(
            seq = 0u,
            buttons = BridgeFrame.BUTTON_LEFT_MASK,
            deltaX = 0, deltaY = 0, wheel = 0,
            modifiers = 0u, keyCode1 = 0u, keyCode2 = 0u
        )
        val frameNotPressed = BridgeFrame(
            seq = 0u,
            buttons = 0u,
            deltaX = 0, deltaY = 0, wheel = 0,
            modifiers = 0u, keyCode1 = 0u, keyCode2 = 0u
        )

        assertTrue("left click pressed", framePressed.isLeftClickPressed())
        assertFalse("left click not pressed", frameNotPressed.isLeftClickPressed())
    }

    /**
     * Test: isRightClickPressed() helper function
     */
    @Test
    fun testIsRightClickPressed() {
        val framePressed = BridgeFrame(
            seq = 0u,
            buttons = BridgeFrame.BUTTON_RIGHT_MASK,
            deltaX = 0, deltaY = 0, wheel = 0,
            modifiers = 0u, keyCode1 = 0u, keyCode2 = 0u
        )
        val frameNotPressed = BridgeFrame(
            seq = 0u,
            buttons = 0u,
            deltaX = 0, deltaY = 0, wheel = 0,
            modifiers = 0u, keyCode1 = 0u, keyCode2 = 0u
        )

        assertTrue("right click pressed", framePressed.isRightClickPressed())
        assertFalse("right click not pressed", frameNotPressed.isRightClickPressed())
    }

    /**
     * Test: isMiddleClickPressed() helper function
     */
    @Test
    fun testIsMiddleClickPressed() {
        val framePressed = BridgeFrame(
            seq = 0u,
            buttons = BridgeFrame.BUTTON_MIDDLE_MASK,
            deltaX = 0, deltaY = 0, wheel = 0,
            modifiers = 0u, keyCode1 = 0u, keyCode2 = 0u
        )
        val frameNotPressed = BridgeFrame(
            seq = 0u,
            buttons = 0u,
            deltaX = 0, deltaY = 0, wheel = 0,
            modifiers = 0u, keyCode1 = 0u, keyCode2 = 0u
        )

        assertTrue("middle click pressed", framePressed.isMiddleClickPressed())
        assertFalse("middle click not pressed", frameNotPressed.isMiddleClickPressed())
    }

    /**
     * Test: isCtrlModifierActive() helper function
     */
    @Test
    fun testIsCtrlModifierActive() {
        val frameActive = BridgeFrame(
            seq = 0u, buttons = 0u,
            deltaX = 0, deltaY = 0, wheel = 0,
            modifiers = BridgeFrame.MODIFIER_LEFT_CTRL_MASK,
            keyCode1 = 0u, keyCode2 = 0u
        )
        val frameInactive = BridgeFrame(
            seq = 0u, buttons = 0u,
            deltaX = 0, deltaY = 0, wheel = 0,
            modifiers = 0u,
            keyCode1 = 0u, keyCode2 = 0u
        )

        assertTrue("LEFT_CTRL active", frameActive.isCtrlModifierActive())
        assertFalse("LEFT_CTRL inactive", frameInactive.isCtrlModifierActive())
    }

    /**
     * Test: isShiftModifierActive() helper function
     */
    @Test
    fun testIsShiftModifierActive() {
        val frameActive = BridgeFrame(
            seq = 0u, buttons = 0u,
            deltaX = 0, deltaY = 0, wheel = 0,
            modifiers = BridgeFrame.MODIFIER_LEFT_SHIFT_MASK,
            keyCode1 = 0u, keyCode2 = 0u
        )
        val frameInactive = BridgeFrame(
            seq = 0u, buttons = 0u,
            deltaX = 0, deltaY = 0, wheel = 0,
            modifiers = 0u,
            keyCode1 = 0u, keyCode2 = 0u
        )

        assertTrue("LEFT_SHIFT active", frameActive.isShiftModifierActive())
        assertFalse("LEFT_SHIFT inactive", frameInactive.isShiftModifierActive())
    }

    /**
     * Test: isAltModifierActive() helper function
     */
    @Test
    fun testIsAltModifierActive() {
        val frameActive = BridgeFrame(
            seq = 0u, buttons = 0u,
            deltaX = 0, deltaY = 0, wheel = 0,
            modifiers = BridgeFrame.MODIFIER_LEFT_ALT_MASK,
            keyCode1 = 0u, keyCode2 = 0u
        )
        val frameInactive = BridgeFrame(
            seq = 0u, buttons = 0u,
            deltaX = 0, deltaY = 0, wheel = 0,
            modifiers = 0u,
            keyCode1 = 0u, keyCode2 = 0u
        )

        assertTrue("LEFT_ALT active", frameActive.isAltModifierActive())
        assertFalse("LEFT_ALT inactive", frameInactive.isAltModifierActive())
    }

    /**
     * Test: isGuiModifierActive() helper function
     */
    @Test
    fun testIsGuiModifierActive() {
        val frameActive = BridgeFrame(
            seq = 0u, buttons = 0u,
            deltaX = 0, deltaY = 0, wheel = 0,
            modifiers = BridgeFrame.MODIFIER_LEFT_GUI_MASK,
            keyCode1 = 0u, keyCode2 = 0u
        )
        val frameInactive = BridgeFrame(
            seq = 0u, buttons = 0u,
            deltaX = 0, deltaY = 0, wheel = 0,
            modifiers = 0u,
            keyCode1 = 0u, keyCode2 = 0u
        )

        assertTrue("LEFT_GUI active", frameActive.isGuiModifierActive())
        assertFalse("LEFT_GUI inactive", frameInactive.isGuiModifierActive())
    }

    /**
     * Test: Multiple buttons pressed simultaneously
     */
    @Test
    fun testMultipleButtonsPressedSimultaneously() {
        val frame = BridgeFrame(
            seq = 0u,
            buttons = (BridgeFrame.BUTTON_LEFT_MASK or BridgeFrame.BUTTON_RIGHT_MASK),
            deltaX = 0, deltaY = 0, wheel = 0,
            modifiers = 0u, keyCode1 = 0u, keyCode2 = 0u
        )

        assertTrue("left click", frame.isLeftClickPressed())
        assertTrue("right click", frame.isRightClickPressed())
        assertFalse("middle click not pressed", frame.isMiddleClickPressed())
    }

    /**
     * Test: Multiple modifiers active simultaneously
     */
    @Test
    fun testMultipleModifiersActive() {
        val frame = BridgeFrame(
            seq = 0u, buttons = 0u,
            deltaX = 0, deltaY = 0, wheel = 0,
            modifiers = (BridgeFrame.MODIFIER_LEFT_CTRL_MASK or BridgeFrame.MODIFIER_LEFT_SHIFT_MASK),
            keyCode1 = 0u, keyCode2 = 0u
        )

        assertTrue("LEFT_CTRL active", frame.isCtrlModifierActive())
        assertTrue("LEFT_SHIFT active", frame.isShiftModifierActive())
        assertFalse("LEFT_ALT inactive", frame.isAltModifierActive())
        assertFalse("LEFT_GUI inactive", frame.isGuiModifierActive())
    }

    /**
     * Test: UByte range validation (0-255)
     */
    @Test
    fun testUByteRange() {
        val frameMin = BridgeFrame(
            seq = 0u, buttons = 0u,
            deltaX = 0, deltaY = 0, wheel = 0,
            modifiers = 0u, keyCode1 = 0u, keyCode2 = 0u
        )
        val frameMax = BridgeFrame(
            seq = 255u, buttons = 255u,
            deltaX = 0, deltaY = 0, wheel = 0,
            modifiers = 255u, keyCode1 = 255u, keyCode2 = 255u
        )

        assertEquals("seq min", 0.toUByte(), frameMin.seq)
        assertEquals("buttons min", 0.toUByte(), frameMin.buttons)
        assertEquals("modifiers min", 0.toUByte(), frameMin.modifiers)
        assertEquals("keyCode1 min", 0.toUByte(), frameMin.keyCode1)
        assertEquals("keyCode2 min", 0.toUByte(), frameMin.keyCode2)

        assertEquals("seq max", 255.toUByte(), frameMax.seq)
        assertEquals("buttons max", 255.toUByte(), frameMax.buttons)
        assertEquals("modifiers max", 255.toUByte(), frameMax.modifiers)
        assertEquals("keyCode1 max", 255.toUByte(), frameMax.keyCode1)
        assertEquals("keyCode2 max", 255.toUByte(), frameMax.keyCode2)
    }

    /**
     * Test: Byte range validation (-128 to 127)
     */
    @Test
    fun testByteRange() {
        val frameMin = BridgeFrame(
            seq = 0u, buttons = 0u,
            deltaX = (-128).toByte(), deltaY = (-128).toByte(), wheel = (-128).toByte(),
            modifiers = 0u, keyCode1 = 0u, keyCode2 = 0u
        )
        val frameMax = BridgeFrame(
            seq = 0u, buttons = 0u,
            deltaX = 127, deltaY = 127, wheel = 127,
            modifiers = 0u, keyCode1 = 0u, keyCode2 = 0u
        )

        assertEquals("deltaX min", (-128).toByte(), frameMin.deltaX)
        assertEquals("deltaY min", (-128).toByte(), frameMin.deltaY)
        assertEquals("wheel min", (-128).toByte(), frameMin.wheel)

        assertEquals("deltaX max", 127.toByte(), frameMax.deltaX)
        assertEquals("deltaY max", 127.toByte(), frameMax.deltaY)
        assertEquals("wheel max", 127.toByte(), frameMax.wheel)
    }

    /**
     * Test: Bit mask constants are correct values
     */
    @Test
    fun testBitMaskConstants() {
        assertEquals("BUTTON_LEFT_MASK", 0x01.toUByte(), BridgeFrame.BUTTON_LEFT_MASK)
        assertEquals("BUTTON_RIGHT_MASK", 0x02.toUByte(), BridgeFrame.BUTTON_RIGHT_MASK)
        assertEquals("BUTTON_MIDDLE_MASK", 0x04.toUByte(), BridgeFrame.BUTTON_MIDDLE_MASK)

        assertEquals("MODIFIER_LEFT_CTRL_MASK", 0x01.toUByte(), BridgeFrame.MODIFIER_LEFT_CTRL_MASK)
        assertEquals("MODIFIER_LEFT_SHIFT_MASK", 0x02.toUByte(), BridgeFrame.MODIFIER_LEFT_SHIFT_MASK)
        assertEquals("MODIFIER_LEFT_ALT_MASK", 0x04.toUByte(), BridgeFrame.MODIFIER_LEFT_ALT_MASK)
        assertEquals("MODIFIER_LEFT_GUI_MASK", 0x08.toUByte(), BridgeFrame.MODIFIER_LEFT_GUI_MASK)
    }
}

