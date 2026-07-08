package com.bridgeone.app.protocol

import com.bridgeone.app.ui.utils.TouchRatio
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

    // ========== 절대좌표 서버 중계 프레임 상수 (Phase 4.9.2, 프로토콜 고정 식별자) ==========

    /** 확장 프레임 헤더 (프로토콜 예약 바이트) */
    private const val EXTENDED_FRAME_HEADER = 0xFF

    /** 절대좌표 서버 중계 서브커맨드 (ABS_POS_TO_SERVER) */
    private const val ABS_POS_TO_SERVER_SUBCOMMAND = 0x02

    /** 비율(0.0~1.0)을 정수 좌표로 인코딩할 때의 최대값 */
    private const val ABS_COORDINATE_MAX = 32767

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

    /**
     * 절대좌표 서버 중계 프레임을 생성합니다 (Phase 4.9.2).
     *
     * AbsolutePointingPad(Page 3, Standard 전용)에서 터치 비율을 서버로 중계할 때 사용하는
     * 8바이트 고정 바이너리 프레임입니다. buildFrame()과 달리 시퀀스 번호를 쓰지 않는
     * 확장 프레임(0xFF 헤더)이라 시퀀스 카운터를 소비하지 않습니다.
     *
     * 프레임 구조: [0xFF][0x02][absX_H][absX_L][absY_H][absY_L][buttons][targetMonitor]
     * ESP32는 이 프레임을 파싱하지 않고 Vendor CDC로 그대로 중계하며, 서버가
     * absX/absY(0~32767)를 대상 모니터 rect에 stretch 매핑해 SetCursorPos를 호출합니다.
     *
     * @param ratio PointingArea 내 터치 위치 비율 (0.0~1.0)
     * @param buttons 버튼 상태 (드래그 모드 시 bit0 유지, Phase 4.9.4 참조)
     * @param targetMonitor 대상 모니터 (0x00=전체 가상 데스크톱, 0x01~=특정 모니터, Phase 4.9.6 참조)
     * @return 8바이트 프레임
     *
     * 참조: docs/android/technical-specification-app.md §2.10.2
     */
    fun buildAbsolutePositionCommand(
        ratio: TouchRatio,
        buttons: UByte,
        targetMonitor: UByte
    ): ByteArray {
        val absX = (ratio.x * ABS_COORDINATE_MAX).toInt().coerceIn(0, ABS_COORDINATE_MAX)
        val absY = (ratio.y * ABS_COORDINATE_MAX).toInt().coerceIn(0, ABS_COORDINATE_MAX)
        return ByteArray(8).also { f ->
            f[0] = EXTENDED_FRAME_HEADER.toByte()
            f[1] = ABS_POS_TO_SERVER_SUBCOMMAND.toByte()
            f[2] = ((absX shr 8) and 0xFF).toByte()
            f[3] = (absX and 0xFF).toByte()
            f[4] = ((absY shr 8) and 0xFF).toByte()
            f[5] = (absY and 0xFF).toByte()
            f[6] = buttons.toByte()
            f[7] = targetMonitor.toByte()
        }
    }
}
