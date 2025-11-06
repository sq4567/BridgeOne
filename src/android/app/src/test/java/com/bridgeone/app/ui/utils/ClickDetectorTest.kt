package com.bridgeone.app.ui.utils

import org.junit.Test
import org.junit.Assert.*
import com.bridgeone.app.protocol.BridgeFrame
import com.bridgeone.app.protocol.FrameBuilder

/**
 * Unit tests for ClickDetector utility class
 *
 * Verifies keyboard frame creation, modifier key handling, and key combinations.
 * Phase 2.2.4.3: 수정자 키 및 키 조합 최적화
 */
class ClickDetectorTest {

    /**
     * Test: Modifier keys bitwise OR operation
     *
     * Verifies: Multiple modifiers are combined correctly using bitwise OR
     * Expected: Result of OR operation matches expected values
     */
    @Test
    fun testModifierBitwiseOR() {
        // Test single modifier
        val mod1 = 0x02.toUByte()  // LEFT_SHIFT
        assertEquals("single modifier", 0x02.toUByte(), mod1)

        // Test two modifiers combined
        val mod2 = (0x02.toUByte() or 0x01.toUByte())  // SHIFT | CTRL
        assertEquals("two modifiers (SHIFT | CTRL)", 0x03.toUByte(), mod2)

        // Test three modifiers combined
        val mod3 = (0x02.toUByte() or 0x01.toUByte() or 0x04.toUByte())  // SHIFT | CTRL | ALT
        assertEquals("three modifiers (SHIFT | CTRL | ALT)", 0x07.toUByte(), mod3)

        // Test all four modifiers
        val mod4 = (0x02.toUByte() or 0x01.toUByte() or 0x04.toUByte() or 0x08.toUByte())  // All
        assertEquals("four modifiers (all)", 0x0F.toUByte(), mod4)
    }

    /**
     * Test: Keyboard frame structure validation using BridgeFrame
     *
     * Verifies: Frame with keyboard input has correct structure
     * Expected: All fields populated correctly for keyboard input
     */
    @Test
    fun testKeyboardFrameStructureWithShift() {
        // Create a frame manually with Shift modifier and KEY_A (0x04)
        val modifiersByte: UByte = 0x02.toUByte()  // LEFT_SHIFT
        val frame = BridgeFrame(
            seq = 1u,
            buttons = 0u,  // No mouse buttons
            deltaX = 0,    // No mouse movement
            deltaY = 0,    // No mouse movement
            wheel = 0,
            modifiers = modifiersByte,
            keyCode1 = 0x04.toUByte(),  // KEY_A
            keyCode2 = 0u
        )

        assertEquals("modifiers should be 0x02 (LEFT_SHIFT)", 0x02.toUByte(), frame.modifiers)
        assertTrue("LEFT_SHIFT should be active", frame.isShiftModifierActive())
        assertFalse("LEFT_CTRL should be inactive", frame.isCtrlModifierActive())
        assertEquals("keyCode1 should be KEY_A", 0x04.toUByte(), frame.keyCode1)
    }

    /**
     * Test: Keyboard frame with Shift+Ctrl combination
     *
     * Verifies: Multiple modifiers combine correctly
     * Expected: modifiers=0x03 (0x02 | 0x01), both helpers return true
     */
    @Test
    fun testKeyboardFrameWithShiftAndCtrl() {
        // Manually combine modifiers
        val modifiersByte: UByte = (0x02.toUByte() or 0x01.toUByte())  // SHIFT | CTRL = 0x03
        val frame = BridgeFrame(
            seq = 2u,
            buttons = 0u,
            deltaX = 0,
            deltaY = 0,
            wheel = 0,
            modifiers = modifiersByte,
            keyCode1 = 0x04.toUByte(),  // KEY_A
            keyCode2 = 0u
        )

        assertEquals("modifiers should be 0x03", 0x03.toUByte(), frame.modifiers)
        assertTrue("LEFT_CTRL should be active", frame.isCtrlModifierActive())
        assertTrue("LEFT_SHIFT should be active", frame.isShiftModifierActive())
        assertFalse("LEFT_ALT should be inactive", frame.isAltModifierActive())
    }

    /**
     * Test: Keyboard frame with all modifiers
     *
     * Verifies: Maximum modifier combination (all 4 modifiers)
     * Expected: modifiers=0x0F, all helper functions return true
     */
    @Test
    fun testKeyboardFrameWithAllModifiers() {
        // Combine all four modifiers: SHIFT(0x02) | CTRL(0x01) | ALT(0x04) | GUI(0x08) = 0x0F
        val modifiersByte: UByte = (0x02.toUByte() or 0x01.toUByte() or 0x04.toUByte() or 0x08.toUByte())
        val frame = BridgeFrame(
            seq = 3u,
            buttons = 0u,
            deltaX = 0,
            deltaY = 0,
            wheel = 0,
            modifiers = modifiersByte,
            keyCode1 = 0x04.toUByte(),  // KEY_A
            keyCode2 = 0u
        )

        assertEquals("modifiers should be 0x0F (all)", 0x0F.toUByte(), frame.modifiers)
        assertTrue("LEFT_CTRL should be active", frame.isCtrlModifierActive())
        assertTrue("LEFT_SHIFT should be active", frame.isShiftModifierActive())
        assertTrue("LEFT_ALT should be active", frame.isAltModifierActive())
        assertTrue("LEFT_GUI should be active", frame.isGuiModifierActive())
    }

    /**
     * Test: Keyboard frame with two keyCodes
     *
     * Verifies: Both keyCode1 and keyCode2 fields can be set
     * Expected: Both fields preserved correctly
     */
    @Test
    fun testKeyboardFrameWithTwoKeyCodes() {
        val frame = BridgeFrame(
            seq = 4u,
            buttons = 0u,
            deltaX = 0,
            deltaY = 0,
            wheel = 0,
            modifiers = 0x02.toUByte(),  // LEFT_SHIFT
            keyCode1 = 0x04.toUByte(),  // KEY_A
            keyCode2 = 0x05.toUByte()   // KEY_B
        )

        assertEquals("keyCode1 should be KEY_A", 0x04.toUByte(), frame.keyCode1)
        assertEquals("keyCode2 should be KEY_B", 0x05.toUByte(), frame.keyCode2)
        assertTrue("LEFT_SHIFT should be active", frame.isShiftModifierActive())
    }

    /**
     * Test: Keyboard frame serialization with modifiers and keyCodes
     *
     * Verifies: Frame serializes to correct 8-byte array
     * Expected: ByteArray positions match frame fields
     */
    @Test
    fun testKeyboardFrameSerializationWithModifiers() {
        val modifiersByte: UByte = (0x02.toUByte() or 0x01.toUByte())  // SHIFT | CTRL = 0x03
        val frame = BridgeFrame(
            seq = 5u,
            buttons = 0u,
            deltaX = 0,
            deltaY = 0,
            wheel = 0,
            modifiers = modifiersByte,
            keyCode1 = 0x04.toUByte(),  // KEY_A
            keyCode2 = 0x05.toUByte()   // KEY_B
        )

        val bytes = frame.toByteArray()

        assertEquals("byte[0] seq", 5.toByte(), bytes[0])
        assertEquals("byte[1] buttons", 0.toByte(), bytes[1])
        assertEquals("byte[2] deltaX", 0.toByte(), bytes[2])
        assertEquals("byte[3] deltaY", 0.toByte(), bytes[3])
        assertEquals("byte[4] wheel", 0.toByte(), bytes[4])
        assertEquals("byte[5] modifiers = 0x03", 0x03.toByte(), bytes[5])
        assertEquals("byte[6] keyCode1 = KEY_A", 0x04.toByte(), bytes[6])
        assertEquals("byte[7] keyCode2 = KEY_B", 0x05.toByte(), bytes[7])
    }

    /**
     * Test: Keyboard frame key release representation
     *
     * Verifies: Key release is keyCode1=0 while modifiers remain
     * Expected: keyCode1=0, modifiers still set for held modifiers
     */
    @Test
    fun testKeyboardFrameKeyRelease() {
        val frame = BridgeFrame(
            seq = 6u,
            buttons = 0u,
            deltaX = 0,
            deltaY = 0,
            wheel = 0,
            modifiers = 0x02.toUByte(),  // LEFT_SHIFT still held
            keyCode1 = 0u,  // Key released (empty keyCode)
            keyCode2 = 0u
        )

        assertEquals("keyCode1 should be 0 (key release)", 0u.toUByte(), frame.keyCode1)
        assertTrue("LEFT_SHIFT should still be active", frame.isShiftModifierActive())
    }
}

