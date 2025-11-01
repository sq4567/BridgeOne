package com.bridgeone.app.protocol

import org.junit.Test
import org.junit.Before
import org.junit.Assert.*

/**
 * BridgeFrame 프레임 구조 및 FrameBuilder 직렬화 로직 테스트.
 *
 * 이 테스트 클래스는 다음 항목을 검증합니다:
 * - 프레임 크기가 정확히 8바이트인지 확인
 * - 바이트 직렬화 순서가 올바른지 확인 (Little-Endian)
 * - 시퀀스 번호 순환 (255 → 0)이 정상 작동하는지 확인
 * - 모든 필드 값 범위가 올바른지 확인
 *
 * @see BridgeFrame
 * @see FrameBuilder
 */
class BridgeFrameTest {

    @Before
    fun setUp() {
        // 각 테스트 실행 전 시퀀스 카운터 초기화
        FrameBuilder.resetSequenceCounter()
    }

    /**
     * 테스트: 직렬화된 프레임 크기가 정확히 8바이트인지 확인.
     */
    @Test
    fun testFrameSizeIs8Bytes() {
        val frame = BridgeFrame(
            seq = 0u,
            buttons = 0u,
            deltaX = 0,
            deltaY = 0,
            wheel = 0,
            modifiers = 0u,
            keyCode1 = 0u,
            keyCode2 = 0u
        )
        val byteArray = FrameBuilder.toByteArray(frame)
        
        assertEquals("frame size must be 8 bytes", 8, byteArray.size)
    }

    /**
     * 테스트: 바이트 직렬화 순서가 올바른지 확인.
     *
     * 직렬화 순서: seq(0), buttons(1), deltaX(2), deltaY(3), wheel(4), modifiers(5), keyCode1(6), keyCode2(7)
     */
    @Test
    fun testByteOrderCorrectness() {
        val frame = BridgeFrame(
            seq = 0xAAu,           // 시퀀스: 0xAA
            buttons = 0xBBu,       // 버튼: 0xBB
            deltaX = 0xCC.toByte(), // 델타X: 0xCC (음수 표현)
            deltaY = 0xDD.toByte(), // 델타Y: 0xDD (음수 표현)
            wheel = 0xEEu.toByte(), // 휠: 0xEE (음수 표현)
            modifiers = 0xFFu,     // 수정자: 0xFF
            keyCode1 = 0x11u,      // 키코드1: 0x11
            keyCode2 = 0x22u       // 키코드2: 0x22
        )
        val byteArray = FrameBuilder.toByteArray(frame)
        
        assertEquals("Byte 0 (seq): 0xAA", 0xAA.toByte(), byteArray[0])
        assertEquals("Byte 1 (buttons): 0xBB", 0xBB.toByte(), byteArray[1])
        assertEquals("Byte 2 (deltaX): 0xCC", 0xCC.toByte(), byteArray[2])
        assertEquals("Byte 3 (deltaY): 0xDD", 0xDD.toByte(), byteArray[3])
        assertEquals("Byte 4 (wheel): 0xEE", 0xEE.toByte(), byteArray[4])
        assertEquals("Byte 5 (modifiers): 0xFF", 0xFF.toByte(), byteArray[5])
        assertEquals("Byte 6 (keyCode1): 0x11", 0x11.toByte(), byteArray[6])
        assertEquals("Byte 7 (keyCode2): 0x22", 0x22.toByte(), byteArray[7])
    }

    /**
     * 테스트: 시퀀스 번호 순환 (255 → 0)이 정상 작동하는지 확인.
     */
    @Test
    fun testSequenceNumberWraparound() {
        // 시퀀스를 0부터 255까지 모두 순회
        // 초기 상태: counter = 0
        // 반복 256번: counter는 0 → 1 → 2 → ... → 254 → 255 → 0 순환
        
        val sequences = mutableListOf<UByte>()
        for (i in 0 until 256) {
            sequences.add(FrameBuilder.getNextSequence())
        }
        
        // 첫 256개 호출의 시퀀스가 0~255 순서인지 확인
        for (i in 0 until 256) {
            assertEquals("seq [$i] value", i.toUByte(), sequences[i])
        }
        
        // 그 다음 호출이 0이 되어야 함 (순환)
        val nextSeq = FrameBuilder.getNextSequence()
        assertEquals("257th call: seq must be 0 (wrap-around)", 0.toUByte(), nextSeq)
    }

    /**
     * 테스트: 모든 필드 값 범위가 올바른지 확인.
     *
     * - UByte 필드: 0~255
     * - Byte 필드 (signed): -128~127
     */
    @Test
    fun testFieldValueRanges() {
        // UByte 필드 최댓값 테스트
        val frameMaxUByte = BridgeFrame(
            seq = UByte.MAX_VALUE,
            buttons = UByte.MAX_VALUE,
            deltaX = 0,
            deltaY = 0,
            wheel = 0,
            modifiers = UByte.MAX_VALUE,
            keyCode1 = UByte.MAX_VALUE,
            keyCode2 = UByte.MAX_VALUE
        )
        val byteArrayMaxUByte = FrameBuilder.toByteArray(frameMaxUByte)
        
        assertEquals("UByte.MAX_VALUE (0xFF) serialization", 0xFF.toByte(), byteArrayMaxUByte[0])
        assertEquals("buttons MAX_VALUE", 0xFF.toByte(), byteArrayMaxUByte[1])
        assertEquals("modifiers MAX_VALUE", 0xFF.toByte(), byteArrayMaxUByte[5])
        
        // UByte 필드 최솟값 테스트
        val frameMinUByte = BridgeFrame(
            seq = UByte.MIN_VALUE,
            buttons = UByte.MIN_VALUE,
            deltaX = 0,
            deltaY = 0,
            wheel = 0,
            modifiers = UByte.MIN_VALUE,
            keyCode1 = UByte.MIN_VALUE,
            keyCode2 = UByte.MIN_VALUE
        )
        val byteArrayMinUByte = FrameBuilder.toByteArray(frameMinUByte)
        
        assertEquals("UByte.MIN_VALUE (0x00) serialization", 0.toByte(), byteArrayMinUByte[0])
        assertEquals("buttons MIN_VALUE", 0.toByte(), byteArrayMinUByte[1])
        
        // Byte 필드 최댓값 테스트 (127)
        val frameMaxByte = BridgeFrame(
            seq = 0u,
            buttons = 0u,
            deltaX = Byte.MAX_VALUE,
            deltaY = Byte.MAX_VALUE,
            wheel = Byte.MAX_VALUE,
            modifiers = 0u,
            keyCode1 = 0u,
            keyCode2 = 0u
        )
        val byteArrayMaxByte = FrameBuilder.toByteArray(frameMaxByte)
        
        assertEquals("Byte.MAX_VALUE (127) serialization", Byte.MAX_VALUE, byteArrayMaxByte[2])
        assertEquals("deltaY MAX_VALUE", Byte.MAX_VALUE, byteArrayMaxByte[3])
        assertEquals("wheel MAX_VALUE", Byte.MAX_VALUE, byteArrayMaxByte[4])
        
        // Byte 필드 최솟값 테스트 (-128)
        val frameMinByte = BridgeFrame(
            seq = 0u,
            buttons = 0u,
            deltaX = Byte.MIN_VALUE,
            deltaY = Byte.MIN_VALUE,
            wheel = Byte.MIN_VALUE,
            modifiers = 0u,
            keyCode1 = 0u,
            keyCode2 = 0u
        )
        val byteArrayMinByte = FrameBuilder.toByteArray(frameMinByte)
        
        assertEquals("Byte.MIN_VALUE (-128) serialization", Byte.MIN_VALUE, byteArrayMinByte[2])
        assertEquals("deltaY MIN_VALUE", Byte.MIN_VALUE, byteArrayMinByte[3])
        assertEquals("wheel MIN_VALUE", Byte.MIN_VALUE, byteArrayMinByte[4])
    }

    /**
     * 테스트: FrameBuilder의 createDefaultFrame() 메서드.
     *
     * - 모든 필드가 0으로 초기화되는지 확인
     * - 시퀀스 번호가 자동으로 증분되는지 확인
     *
     * 주의: setUp()에서 resetSequenceCounter()로 카운터를 0으로 초기화하므로,
     * 첫 번째 호출은 seq=0을 반환하고 두 번째 호출은 seq=1을 반환합니다.
     */
    @Test
    fun testCreateDefaultFrame() {
        val frame1 = FrameBuilder.createDefaultFrame()
        val frame2 = FrameBuilder.createDefaultFrame()
        
        // 첫 번째 호출: seq=0 (카운터 0 반환 후 1로 증가)
        assertEquals("1st frame seq must be 0", 0.toUByte(), frame1.seq)
        
        // 두 번째 호출: seq=1 (카운터 1 반환 후 2로 증가)
        assertEquals("2nd frame seq must be 1", 1.toUByte(), frame2.seq)
        
        // 모든 필드가 0으로 초기화되는지 확인
        assertEquals("buttons must be 0", 0u.toUByte(), frame1.buttons)
        assertEquals("deltaX must be 0", 0.toByte(), frame1.deltaX)
        assertEquals("deltaY must be 0", 0.toByte(), frame1.deltaY)
        assertEquals("wheel must be 0", 0.toByte(), frame1.wheel)
        assertEquals("modifiers must be 0", 0u.toUByte(), frame1.modifiers)
        assertEquals("keyCode1 must be 0", 0u.toUByte(), frame1.keyCode1)
        assertEquals("keyCode2 must be 0", 0u.toUByte(), frame1.keyCode2)
    }

    /**
     * 테스트: 모든 필드가 1로 설정된 프레임의 직렬화.
     *
     * 이 테스트는 모든 필드가 동시에 데이터를 전달하는 경우를 시뮬레이션합니다.
     */
    @Test
    fun testFrameWithAllFieldsSet() {
        val frame = BridgeFrame(
            seq = 42u,
            buttons = 7u,           // 0x07 (Left=1, Right=1, Middle=1)
            deltaX = 10,            // 마우스 X 이동
            deltaY = -10,           // 마우스 Y 이동
            wheel = 5,              // 스크롤 업
            modifiers = 15u,        // 0x0F (모든 수정자 활성화)
            keyCode1 = 65u,         // 'A' 키
            keyCode2 = 66u          // 'B' 키
        )
        val byteArray = FrameBuilder.toByteArray(frame)
        
        assertEquals("frame size", 8, byteArray.size)
        assertEquals("seq", 42.toByte(), byteArray[0])
        assertEquals("buttons", 7.toByte(), byteArray[1])
        assertEquals("deltaX", 10.toByte(), byteArray[2])
        assertEquals("deltaY", (-10).toByte(), byteArray[3])
        assertEquals("wheel", 5.toByte(), byteArray[4])
        assertEquals("modifiers", 15.toByte(), byteArray[5])
        assertEquals("keyCode1", 65.toByte(), byteArray[6])
        assertEquals("keyCode2", 66.toByte(), byteArray[7])
    }
}
