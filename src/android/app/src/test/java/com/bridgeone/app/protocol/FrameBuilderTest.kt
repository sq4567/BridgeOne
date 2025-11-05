package com.bridgeone.app.protocol

import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

/**
 * Unit tests for FrameBuilder singleton
 *
 * Verifies sequence number management, frame creation, and thread safety.
 */
class FrameBuilderTest {

    /**
     * Reset sequence counter before each test
     */
    @Before
    fun setUp() {
        FrameBuilder.resetSequence()
    }

    /**
     * Test: buildFrame() auto-increments seq (0, 1, 2, ...)
     */
    @Test
    fun testSequenceAutoIncrement() {
        FrameBuilder.resetSequence()

        for (i in 0 until 5) {
            val frame = FrameBuilder.buildFrame(
                buttons = 0u,
                deltaX = 0,
                deltaY = 0,
                wheel = 0,
                modifiers = 0u,
                keyCode1 = 0u,
                keyCode2 = 0u
            )
            assertEquals("seq should be $i", i.toUByte(), frame.seq)
        }
    }

    /**
     * Test: Sequence wraps around (255 -> 0)
     */
    @Test
    fun testSequenceWraparound() {
        // Setup: Create frames up to sequence 253
        repeat(253) {
            FrameBuilder.buildFrame(0u, 0, 0, 0, 0u, 0u, 0u)
        }

        val frame253 = FrameBuilder.buildFrame(0u, 0, 0, 0, 0u, 0u, 0u)
        val frame254 = FrameBuilder.buildFrame(0u, 0, 0, 0, 0u, 0u, 0u)
        val frame255 = FrameBuilder.buildFrame(0u, 0, 0, 0, 0u, 0u, 0u)
        val frameWrapped = FrameBuilder.buildFrame(0u, 0, 0, 0, 0u, 0u, 0u)

        assertEquals("seq 253", 253u.toUByte(), frame253.seq)
        assertEquals("seq 254", 254u.toUByte(), frame254.seq)
        assertEquals("seq 255", 255u.toUByte(), frame255.seq)
        assertEquals("seq wraps to 0", 0u.toUByte(), frameWrapped.seq)
    }

    /**
     * Test: Multiple wrap-arounds (255 -> 0 -> 1 -> ...)
     */
    @Test
    fun testMultipleWraparounds() {
        repeat(250) {
            FrameBuilder.buildFrame(0u, 0, 0, 0, 0u, 0u, 0u)
        }

        for (i in 250 until 260) {
            val frame = FrameBuilder.buildFrame(0u, 0, 0, 0, 0u, 0u, 0u)
            val expected = (i % 256).toUByte()
            assertEquals("seq at $i", expected, frame.seq)
        }
    }

    /**
     * Test: resetSequence() initializes counter to 0
     */
    @Test
    fun testResetSequence() {
        repeat(5) {
            FrameBuilder.buildFrame(0u, 0, 0, 0, 0u, 0u, 0u)
        }

        FrameBuilder.resetSequence()

        val frame = FrameBuilder.buildFrame(0u, 0, 0, 0, 0u, 0u, 0u)
        assertEquals("after reset, seq is 0", 0u.toUByte(), frame.seq)
    }

    /**
     * Test: getCurrentSequence() returns counter value
     */
    @Test
    fun testGetCurrentSequence() {
        FrameBuilder.resetSequence()

        assertEquals("initial counter", 0, FrameBuilder.getCurrentSequence())

        repeat(3) { FrameBuilder.buildFrame(0u, 0, 0, 0, 0u, 0u, 0u) }

        assertEquals("after 3 calls", 3, FrameBuilder.getCurrentSequence())
    }

    /**
     * Test: buildFrame() transfers all input fields correctly
     */
    @Test
    fun testBuildFrameFieldsTransfer() {
        val buttons = 0x03.toUByte()
        val deltaX = 50.toByte()
        val deltaY = (-30).toByte()
        val wheel = 2.toByte()
        val modifiers = 0x05.toUByte()
        val keyCode1 = 0x65.toUByte()
        val keyCode2 = 0x66.toUByte()

        val frame = FrameBuilder.buildFrame(buttons, deltaX, deltaY, wheel, modifiers, keyCode1, keyCode2)

        assertEquals("buttons", buttons, frame.buttons)
        assertEquals("deltaX", deltaX, frame.deltaX)
        assertEquals("deltaY", deltaY, frame.deltaY)
        assertEquals("wheel", wheel, frame.wheel)
        assertEquals("modifiers", modifiers, frame.modifiers)
        assertEquals("keyCode1", keyCode1, frame.keyCode1)
        assertEquals("keyCode2", keyCode2, frame.keyCode2)
    }

    /**
     * Test: Multi-threaded access produces unique sequence numbers
     */
    @Test
    fun testMultiThreadSequenceUniqueness() {
        val NUM_THREADS = 10
        val FRAMES_PER_THREAD = 100
        val totalFrames = NUM_THREADS * FRAMES_PER_THREAD

        val sequences = mutableListOf<UByte>()
        val lock = java.lang.Object()

        val threads = (0 until NUM_THREADS).map { threadId ->
            thread {
                repeat(FRAMES_PER_THREAD) {
                    val frame = FrameBuilder.buildFrame(
                        buttons = 0u,
                        deltaX = 0,
                        deltaY = 0,
                        wheel = 0,
                        modifiers = 0u,
                        keyCode1 = 0u,
                        keyCode2 = 0u
                    )
                    synchronized(lock) {
                        sequences.add(frame.seq)
                    }
                }
            }
        }

        threads.forEach { it.join() }

        assertEquals("total frames", totalFrames, sequences.size)

        val seqCounts = mutableMapOf<Int, Int>()
        sequences.forEach { seq ->
            val count = seqCounts.getOrDefault(seq.toInt(), 0)
            seqCounts[seq.toInt()] = count + 1
        }

        for ((seq, count) in seqCounts) {
            assertTrue(
                "seq $seq count: $count (expected 3-4)",
                count in 3..4
            )
        }
    }

    /**
     * Test: Multi-threaded getCurrentSequence() verification
     */
    @Test
    fun testMultiThreadCurrentSequence() {
        FrameBuilder.resetSequence()
        val NUM_THREADS = 5
        val FRAMES_PER_THREAD = 50
        val countDownLatch = CountDownLatch(NUM_THREADS)
        val sequenceValues = AtomicInteger(0)

        repeat(NUM_THREADS) {
            thread {
                repeat(FRAMES_PER_THREAD) {
                    FrameBuilder.buildFrame(0u, 0, 0, 0, 0u, 0u, 0u)
                }
                synchronized(sequenceValues) {
                    sequenceValues.set(FrameBuilder.getCurrentSequence())
                }
                countDownLatch.countDown()
            }
        }

        countDownLatch.await()

        val totalExpectedCalls = NUM_THREADS * FRAMES_PER_THREAD
        assertEquals(
            "counter after all calls",
            totalExpectedCalls % 256,
            sequenceValues.get()
        )
    }

    /**
     * Test: resetSequence() works repeatedly
     */
    @Test
    fun testResetSequenceMultipleTimes() {
        for (cycle in 0 until 3) {
            FrameBuilder.resetSequence()
            val frame = FrameBuilder.buildFrame(0u, 0, 0, 0, 0u, 0u, 0u)
            assertEquals("cycle $cycle initial seq", 0u.toUByte(), frame.seq)

            repeat(5) { FrameBuilder.buildFrame(0u, 0, 0, 0, 0u, 0u, 0u) }
            // After 1 (above) + 5 (repeat) = 6 calls total
            assertEquals("cycle $cycle after 6 total calls", 6, FrameBuilder.getCurrentSequence())
        }
    }

    /**
     * Test: Extreme sequence wraparound
     */
    @Test
    fun testExtremeSequenceWraparound() {
        repeat(254) {
            FrameBuilder.buildFrame(0u, 0, 0, 0, 0u, 0u, 0u)
        }

        val expectedSequences = listOf(254, 255, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
        expectedSequences.forEach { expected ->
            val frame = FrameBuilder.buildFrame(0u, 0, 0, 0, 0u, 0u, 0u)
            assertEquals("wraparound seq", expected.toUByte(), frame.seq)
        }
    }

    /**
     * Test: Frame data integrity after repeated calls
     */
    @Test
    fun testBuildFrameDataIntegrityAfterRepeatedCalls() {
        // Already reset in @Before, so start fresh
        repeat(10) {
            FrameBuilder.buildFrame(0u, 0, 0, 0, 0u, 0u, 0u)
        }

        val frame = FrameBuilder.buildFrame(
            buttons = 0x05u,
            deltaX = 100,
            deltaY = (-50).toByte(),
            wheel = 1,
            modifiers = 0x03u,
            keyCode1 = 0xABu,
            keyCode2 = 0xCDu
        )

        assertEquals("seq after 11 total calls", 10u.toUByte(), frame.seq)
        assertEquals("buttons", 0x05.toUByte(), frame.buttons)
        assertEquals("deltaX", 100.toByte(), frame.deltaX)
        assertEquals("deltaY", (-50).toByte(), frame.deltaY)
        assertEquals("wheel", 1.toByte(), frame.wheel)
        assertEquals("modifiers", 0x03.toUByte(), frame.modifiers)
        assertEquals("keyCode1", 0xAB.toUByte(), frame.keyCode1)
        assertEquals("keyCode2", 0xCD.toUByte(), frame.keyCode2)
    }

    /**
     * Test: getCurrentSequence() returns next sequence value
     */
    @Test
    fun testGetCurrentSequenceReturnsNextSequence() {
        FrameBuilder.resetSequence()
        repeat(7) { FrameBuilder.buildFrame(0u, 0, 0, 0, 0u, 0u, 0u) }

        val currentSeq = FrameBuilder.getCurrentSequence()

        val nextFrame = FrameBuilder.buildFrame(0u, 0, 0, 0, 0u, 0u, 0u)
        assertEquals("getCurrentSequence matches next frame", currentSeq.toUByte(), nextFrame.seq)
    }

    /**
     * Test: Full 0-255 sequence cycle
     */
    @Test
    fun testFullSequenceCycle() {
        FrameBuilder.resetSequence()

        val sequences = mutableListOf<UByte>()
        repeat(256) {
            val frame = FrameBuilder.buildFrame(0u, 0, 0, 0, 0u, 0u, 0u)
            sequences.add(frame.seq)
        }

        for (i in 0 until 256) {
            assertEquals("seq at position $i", i.toUByte(), sequences[i])
        }

        val nextFrame = FrameBuilder.buildFrame(0u, 0, 0, 0, 0u, 0u, 0u)
        assertEquals("after full cycle, wraps to 0", 0u.toUByte(), nextFrame.seq)
    }
}

