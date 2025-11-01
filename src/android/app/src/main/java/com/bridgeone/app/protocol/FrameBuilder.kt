package com.bridgeone.app.protocol

import java.util.concurrent.atomic.AtomicInteger

/**
 * BridgeFrame 프레임을 생성하고 바이트 배열로 직렬화하는 싱글톤 객체.
 *
 * 이 객체는 다음 기능을 제공합니다:
 * - 프레임의 시퀀스 번호 자동 증분 (0~255 순환)
 * - BridgeFrame을 8바이트 배열로 직렬화 (Little-Endian)
 * - 기본값을 가진 프레임 생성 헬퍼 메서드
 *
 * **Thread-Safety**: AtomicInteger를 사용하여 멀티스레드 환경에서 안전한 시퀀스 번호 관리.
 * **Little-Endian**: 각 필드는 1바이트이므로 직렬화 순서는 seq, buttons, deltaX, deltaY, wheel, modifiers, keyCode1, keyCode2.
 *
 * @see BridgeFrame
 */
object FrameBuilder {
    /**
     * 프레임의 시퀀스 번호 카운터 (0~255 순환).
     * AtomicInteger를 사용하여 멀티스레드 환경에서 thread-safe하게 관리.
     */
    private val sequenceCounter = AtomicInteger(0)

    /**
     * 다음 시퀀스 번호를 반환하고 카운터를 증분합니다.
     *
     * 0부터 255까지 순환하며, 255 다음은 다시 0으로 돌아갑니다.
     * 이 메서드는 멀티스레드 환경에서 안전하게 호출할 수 있습니다.
     *
     * @return 현재 시퀀스 번호 (0~255)
     *
     * 사용 예:
     * ```kotlin
     * val seq1 = FrameBuilder.getNextSequence()  // 0
     * val seq2 = FrameBuilder.getNextSequence()  // 1
     * val seq255 = FrameBuilder.getNextSequence() // 255
     * val seq0 = FrameBuilder.getNextSequence()  // 0 (순환)
     * ```
     */
    fun getNextSequence(): UByte {
        // CAS(Compare-And-Set) 루프로 원자적 연산 보장
        while (true) {
            val current = sequenceCounter.get()
            val next = (current + 1) % 256
            if (sequenceCounter.compareAndSet(current, next)) {
                return current.toUByte()
            }
        }
    }

    /**
     * BridgeFrame을 8바이트 Little-Endian 배열로 직렬화합니다.
     *
     * 직렬화 순서 (각 필드 1바이트):
     * 1. Byte 0: seq (시퀀스 번호)
     * 2. Byte 1: buttons (마우스 버튼 비트)
     * 3. Byte 2: deltaX (X축 이동값)
     * 4. Byte 3: deltaY (Y축 이동값)
     * 5. Byte 4: wheel (휠 값)
     * 6. Byte 5: modifiers (키보드 수정자)
     * 7. Byte 6: keyCode1 (첫 번째 키코드)
     * 8. Byte 7: keyCode2 (두 번째 키코드)
     *
     * @param frame 직렬화할 BridgeFrame 객체
     * @return 8바이트 ByteArray (Little-Endian)
     *
     * @see BridgeFrame
     */
    fun toByteArray(frame: BridgeFrame): ByteArray {
        return byteArrayOf(
            frame.seq.toByte(),           // Byte 0: 시퀀스 번호
            frame.buttons.toByte(),       // Byte 1: 마우스 버튼 비트
            frame.deltaX,                 // Byte 2: X축 이동값
            frame.deltaY,                 // Byte 3: Y축 이동값
            frame.wheel,                  // Byte 4: 휠 값
            frame.modifiers.toByte(),     // Byte 5: 키보드 수정자
            frame.keyCode1.toByte(),      // Byte 6: 첫 번째 키코드
            frame.keyCode2.toByte()       // Byte 7: 두 번째 키코드
        )
    }

    /**
     * 기본값을 가진 BridgeFrame을 생성합니다.
     *
     * 모든 필드가 0으로 초기화되며, 시퀀스 번호는 자동으로 증분됩니다.
     * 이 메서드는 테스트나 기본 프레임 생성 시 유용합니다.
     *
     * @return 모든 필드가 0으로 초기화된 BridgeFrame 객체
     *
     * @see getNextSequence
     */
    fun createDefaultFrame(): BridgeFrame {
        return BridgeFrame(
            seq = getNextSequence(),
            buttons = 0u,
            deltaX = 0,
            deltaY = 0,
            wheel = 0,
            modifiers = 0u,
            keyCode1 = 0u,
            keyCode2 = 0u
        )
    }

    /**
     * 시퀀스 번호 카운터를 초기화합니다.
     *
     * 주로 테스트 환경에서 사용됩니다.
     * 프로덕션 환경에서는 호출하지 않아야 합니다.
     */
    fun resetSequenceCounter() {
        sequenceCounter.set(0)
    }
}
