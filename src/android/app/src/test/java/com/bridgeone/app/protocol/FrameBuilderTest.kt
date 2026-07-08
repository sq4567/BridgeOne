package com.bridgeone.app.protocol

import com.bridgeone.app.ui.utils.TouchRatio
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
     * Test: Sequence wraps around (253 -> 0)
     *
     * SEQ_MODULUS=254: 유효 범위는 0~253. 0xFE(254)·0xFF(255)는 프로토콜 예약 바이트.
     */
    @Test
    fun testSequenceWraparound() {
        // seq 0..252 소비
        repeat(252) {
            FrameBuilder.buildFrame(0u, 0, 0, 0, 0u, 0u, 0u)
        }

        val frame252 = FrameBuilder.buildFrame(0u, 0, 0, 0, 0u, 0u, 0u)
        val frame253 = FrameBuilder.buildFrame(0u, 0, 0, 0, 0u, 0u, 0u)
        val frameWrapped = FrameBuilder.buildFrame(0u, 0, 0, 0, 0u, 0u, 0u)  // 254번째 → 0
        val frameAfterWrap = FrameBuilder.buildFrame(0u, 0, 0, 0, 0u, 0u, 0u)

        assertEquals("seq 252", 252u.toUByte(), frame252.seq)
        assertEquals("seq 253", 253u.toUByte(), frame253.seq)
        assertEquals("seq wraps to 0 after 253", 0u.toUByte(), frameWrapped.seq)
        assertEquals("seq 1 after wrap", 1u.toUByte(), frameAfterWrap.seq)
    }

    /**
     * Test: Multiple wrap-arounds (253 -> 0 -> 1 -> ...)
     *
     * SEQ_MODULUS=254 기준으로 254번째 프레임부터 0으로 순환.
     */
    @Test
    fun testMultipleWraparounds() {
        repeat(250) {
            FrameBuilder.buildFrame(0u, 0, 0, 0, 0u, 0u, 0u)
        }

        for (i in 250 until 260) {
            val frame = FrameBuilder.buildFrame(0u, 0, 0, 0, 0u, 0u, 0u)
            val expected = (i % 254).toUByte()  // SEQ_MODULUS=254 기준
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
            totalExpectedCalls % 254,  // SEQ_MODULUS=254
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
     *
     * SEQ_MODULUS=254: 250번째 이후 253→0→1→2 순환 확인.
     */
    @Test
    fun testExtremeSequenceWraparound() {
        repeat(250) {
            FrameBuilder.buildFrame(0u, 0, 0, 0, 0u, 0u, 0u)
        }

        // 250부터 시작해서 253→0→1→2 순환 확인
        val expectedSequences = listOf(250, 251, 252, 253, 0, 1, 2, 3, 4, 5, 6, 7)
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
     * Test: Full 0-253 sequence cycle (SEQ_MODULUS=254)
     *
     * 0xFE(254)·0xFF(255)는 프로토콜 예약 바이트라 유효 범위는 0~253.
     * 한 사이클 = 254 프레임.
     */
    @Test
    fun testFullSequenceCycle() {
        FrameBuilder.resetSequence()

        val sequences = mutableListOf<UByte>()
        repeat(254) {  // SEQ_MODULUS=254 한 사이클
            val frame = FrameBuilder.buildFrame(0u, 0, 0, 0, 0u, 0u, 0u)
            sequences.add(frame.seq)
        }

        for (i in 0 until 254) {
            assertEquals("seq at position $i", i.toUByte(), sequences[i])
        }

        val nextFrame = FrameBuilder.buildFrame(0u, 0, 0, 0, 0u, 0u, 0u)
        assertEquals("after full cycle, wraps to 0", 0u.toUByte(), nextFrame.seq)
    }

    // ========== buildAbsolutePositionCommand() 테스트 (Phase 4.9.2) ==========

    /**
     * Test: 헤더 바이트가 0xFF/0x02로 고정되는지 확인
     */
    @Test
    fun testBuildAbsolutePositionCommandHeaderBytes() {
        val command = FrameBuilder.buildAbsolutePositionCommand(
            ratio = TouchRatio(0f, 0f),
            buttons = 0x00u,
            targetMonitor = 0x01u
        )
        assertEquals("frame size", 8, command.size)
        assertEquals("header", 0xFF.toByte(), command[0])
        assertEquals("subcommand", 0x02.toByte(), command[1])
    }

    /**
     * Test: 비율 0.5 → absX/absY=16383 → 상위/하위 바이트 분해 확인
     */
    @Test
    fun testBuildAbsolutePositionCommandMidRatio() {
        val command = FrameBuilder.buildAbsolutePositionCommand(
            ratio = TouchRatio(0.5f, 0.5f),
            buttons = 0x00u,
            targetMonitor = 0x01u
        )
        val absX = ((command[2].toInt() and 0xFF) shl 8) or (command[3].toInt() and 0xFF)
        val absY = ((command[4].toInt() and 0xFF) shl 8) or (command[5].toInt() and 0xFF)
        assertEquals("absX at ratio 0.5", 16383, absX)
        assertEquals("absY at ratio 0.5", 16383, absY)
    }

    /**
     * Test: 비율 0.0 → 0, 비율 1.0 → 32767 경계값 확인
     */
    @Test
    fun testBuildAbsolutePositionCommandBoundaryRatios() {
        val zeroCommand = FrameBuilder.buildAbsolutePositionCommand(
            ratio = TouchRatio(0f, 0f),
            buttons = 0x00u,
            targetMonitor = 0x01u
        )
        val zeroAbsX = ((zeroCommand[2].toInt() and 0xFF) shl 8) or (zeroCommand[3].toInt() and 0xFF)
        val zeroAbsY = ((zeroCommand[4].toInt() and 0xFF) shl 8) or (zeroCommand[5].toInt() and 0xFF)
        assertEquals("absX at ratio 0.0", 0, zeroAbsX)
        assertEquals("absY at ratio 0.0", 0, zeroAbsY)

        val oneCommand = FrameBuilder.buildAbsolutePositionCommand(
            ratio = TouchRatio(1f, 1f),
            buttons = 0x00u,
            targetMonitor = 0x01u
        )
        val oneAbsX = ((oneCommand[2].toInt() and 0xFF) shl 8) or (oneCommand[3].toInt() and 0xFF)
        val oneAbsY = ((oneCommand[4].toInt() and 0xFF) shl 8) or (oneCommand[5].toInt() and 0xFF)
        assertEquals("absX at ratio 1.0", 32767, oneAbsX)
        assertEquals("absY at ratio 1.0", 32767, oneAbsY)
    }

    /**
     * Test: buttons/targetMonitor 바이트가 인자대로 위치 6/7에 실리는지 확인
     */
    @Test
    fun testBuildAbsolutePositionCommandButtonsAndTargetMonitor() {
        val command = FrameBuilder.buildAbsolutePositionCommand(
            ratio = TouchRatio(0f, 0f),
            buttons = 0x01u,
            targetMonitor = 0x02u
        )
        assertEquals("buttons byte", 0x01.toByte(), command[6])
        assertEquals("targetMonitor byte", 0x02.toByte(), command[7])
    }
}

