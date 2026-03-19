package com.bridgeone.app.protocol

import java.util.concurrent.atomic.AtomicInteger

/**
 * BridgeFrame 생성 및 순번 관리를 담당하는 싱글톤 빌더
 *
 * 스레드 안전한 순번 관리(0~253 순환)를 제공하며, 각 buildFrame() 호출 시
 * 자동으로 시퀀스 번호가 증가합니다.
 * 0xFE(역방향 알림 프레임 헤더)와 0xFF(미래 예약)는 프로토콜 예약 바이트입니다.
 *
 * 사용 예:
 * ```
 * val frame = FrameBuilder.buildFrame(
 *     buttons = 0x01u,
 *     deltaX = 10,
 *     deltaY = -5,
 *     wheel = 0,
 *     modifiers = 0u,
 *     keyCode1 = 0u,
 *     keyCode2 = 0u
 * )
 * ```
 */
object FrameBuilder {
    /**
     * 시퀀스 번호 최대값 (exclusive).
     * 0xFE(역방향 알림 프레임 헤더)와 0xFF(미래 예약)를 프로토콜 예약 바이트로 확보하기 위해
     * 시퀀스 번호 범위를 0~253(0x00~0xFD)으로 제한합니다.
     */
    private const val SEQ_MODULUS = 254

    /**
     * 순번 카운터 (0~253 순환)
     * AtomicInteger를 사용하여 멀티 스레드 환경에서도 안전한 카운터 증가 보장
     */
    private val sequenceCounter = AtomicInteger(0)

    /**
     * 다음 시퀀스 번호를 획득하고 카운터를 증가시킵니다.
     *
     * 스레드 안전하게 0~253 범위의 순번을 순환하면서 제공합니다.
     * 0xFE와 0xFF는 프로토콜 예약 바이트로 사용되지 않습니다.
     *
     * @return 0~253 범위의 시퀀스 번호
     */
    private fun getNextSequence(): UByte {
        while (true) {
            val current = sequenceCounter.get()
            val next = (current + 1) % SEQ_MODULUS
            if (sequenceCounter.compareAndSet(current, next)) {
                return current.toUByte()
            }
        }
    }

    /**
     * 모든 입력값을 바탕으로 BridgeFrame을 생성합니다.
     *
     * 시퀀스 번호는 자동으로 할당되며, 0~253 범위에서 순환합니다.
     * 스레드 안전하게 작동하므로 여러 스레드에서 동시에 호출해도 안전합니다.
     *
     * @param buttons 마우스 버튼 비트 (0x00~0x07)
     * @param deltaX X축 상대 이동값 (-128 ~ 127)
     * @param deltaY Y축 상대 이동값 (-128 ~ 127)
     * @param wheel 마우스 휠 값 (-128 ~ 127)
     * @param modifiers 키보드 수정자 키 비트 (0x00~0x0F)
     * @param keyCode1 첫 번째 키코드
     * @param keyCode2 두 번째 키코드
     * @return 시퀀스 번호가 할당된 BridgeFrame
     */
    fun buildFrame(
        buttons: UByte,
        deltaX: Byte,
        deltaY: Byte,
        wheel: Byte,
        modifiers: UByte,
        keyCode1: UByte,
        keyCode2: UByte
    ): BridgeFrame = BridgeFrame(
        seq = getNextSequence(),
        buttons = buttons,
        deltaX = deltaX,
        deltaY = deltaY,
        wheel = wheel,
        modifiers = modifiers,
        keyCode1 = keyCode1,
        keyCode2 = keyCode2
    )

    /**
     * 순번 카운터를 초기화합니다.
     *
     * 테스트 또는 디버깅 목적으로 카운터를 0으로 리셋할 때 사용합니다.
     * 프로덕션 환경에서는 일반적으로 호출할 필요가 없습니다.
     */
    fun resetSequence() {
        sequenceCounter.set(0)
    }

    /**
     * 현재 시퀀스 번호를 조회합니다.
     *
     * 다음 buildFrame() 호출 시 할당될 시퀀스 번호를 반환합니다.
     * 테스트 또는 디버깅 목적으로 사용합니다.
     *
     * @return 현재 내부 카운터 값 (0~253)
     */
    fun getCurrentSequence(): Int = sequenceCounter.get()
}
