package com.chatterbones.bridgeone.protocol

/**
 * BridgeOne 프로토콜 프레임 데이터 클래스
 *
 * Android 앱과 ESP32-S3 보드 간 UART 통신을 위한 8바이트 고정 크기 프레임입니다.
 * Little-Endian 바이트 순서를 사용하며, 마우스/키보드 입력을 전달합니다.
 *
 * 프레임 구조는 @see <a href="file:///Docs/Board/esp32s3-code-implementation-guide.md">
 * esp32s3-code-implementation-guide.md §2.1</a>을 따릅니다.
 *
 * @property seq 순번 카운터 (0-255 순환, 패킷 손실 감지용)
 * @property buttons 마우스 버튼 상태 (bit0=Left, bit1=Right, bit2=Middle)
 * @property deltaX X축 상대 이동 (-127~127)
 * @property deltaY Y축 상대 이동 (-127~127)
 * @property wheel 휠 스크롤량 (-127~127)
 * @property modifiers 키보드 모디파이어 키 (Ctrl, Alt, Shift, GUI)
 * @property keyCode1 주요 키 입력 코드
 * @property keyCode2 보조 키 입력 코드
 */
@OptIn(ExperimentalUnsignedTypes::class)
data class BridgeFrame(
    val seq: UByte,
    val buttons: UByte,
    val deltaX: Byte,
    val deltaY: Byte,
    val wheel: Byte,
    val modifiers: UByte,
    val keyCode1: UByte,
    val keyCode2: UByte
) {
    init {
        // 범위 검증: UByte는 타입 시스템에서 자동 보장 (0-255)
        // Byte는 타입 시스템에서 자동 보장 (-128~127)
        // 추가 검증이 필요한 경우 여기에 구현
    }

    /**
     * 프레임을 8바이트 배열로 직렬화합니다.
     *
     * Little-Endian 바이트 순서로 변환하여 UART 전송에 사용합니다.
     * 각 필드는 순서대로 1바이트씩 배치됩니다.
     *
     * @return 8바이트 크기의 ByteArray
     */
    fun toByteArray(): ByteArray {
        return byteArrayOf(
            seq.toByte(),        // [0] 순번
            buttons.toByte(),    // [1] 마우스 버튼
            deltaX,              // [2] X축 이동
            deltaY,              // [3] Y축 이동
            wheel,               // [4] 휠
            modifiers.toByte(),  // [5] 모디파이어
            keyCode1.toByte(),   // [6] 키 코드 1
            keyCode2.toByte()    // [7] 키 코드 2
        )
    }

    /**
     * 프레임의 문자열 표현을 반환합니다.
     *
     * 디버깅 및 로깅 목적으로 사용됩니다.
     *
     * @return 프레임 정보를 포함하는 문자열
     */
    override fun toString(): String {
        return "BridgeFrame(seq=$seq, buttons=0x${buttons.toString(16).padStart(2, '0')}, " +
                "deltaX=$deltaX, deltaY=$deltaY, wheel=$wheel, " +
                "modifiers=0x${modifiers.toString(16).padStart(2, '0')}, " +
                "keyCode1=0x${keyCode1.toString(16).padStart(2, '0')}, " +
                "keyCode2=0x${keyCode2.toString(16).padStart(2, '0')})"
    }

    companion object {
        /**
         * 프레임 크기 상수 (8바이트)
         */
        const val FRAME_SIZE = 8

        /**
         * 마우스 버튼 비트 마스크 상수
         */
        const val BUTTON_LEFT: UByte = 0x01u
        const val BUTTON_RIGHT: UByte = 0x02u
        const val BUTTON_MIDDLE: UByte = 0x04u

        /**
         * 키보드 모디파이어 비트 마스크 상수
         */
        const val MODIFIER_CTRL: UByte = 0x01u
        const val MODIFIER_SHIFT: UByte = 0x02u
        const val MODIFIER_ALT: UByte = 0x04u
        const val MODIFIER_GUI: UByte = 0x08u

        /**
         * 8바이트 배열로부터 프레임을 역직렬화합니다.
         *
         * Little-Endian 바이트 순서로 해석하여 BridgeFrame 객체를 생성합니다.
         * 입력 배열의 크기와 데이터 유효성을 검증합니다.
         *
         * @param data 8바이트 크기의 ByteArray
         * @return 역직렬화된 BridgeFrame 객체
         * @throws IllegalArgumentException 배열 크기가 8바이트가 아닌 경우
         */
        fun fromByteArray(data: ByteArray): BridgeFrame {
            require(data.size == FRAME_SIZE) {
                "프레임 크기는 정확히 $FRAME_SIZE 바이트여야 합니다. (실제: ${data.size} 바이트)"
            }

            // Little-Endian 바이트 순서로 각 필드 추출
            return BridgeFrame(
                seq = data[0].toUByte(),        // [0] 순번
                buttons = data[1].toUByte(),    // [1] 마우스 버튼
                deltaX = data[2],               // [2] X축 이동 (signed)
                deltaY = data[3],               // [3] Y축 이동 (signed)
                wheel = data[4],                // [4] 휠 (signed)
                modifiers = data[5].toUByte(),  // [5] 모디파이어
                keyCode1 = data[6].toUByte(),   // [6] 키 코드 1
                keyCode2 = data[7].toUByte()    // [7] 키 코드 2
            )
        }

        /**
         * 빈 프레임을 생성합니다.
         *
         * 모든 필드가 0으로 초기화된 프레임을 반환합니다.
         *
         * @param seq 순번 카운터 (기본값: 0)
         * @return 초기화된 BridgeFrame 객체
         */
        fun empty(seq: UByte = 0u): BridgeFrame {
            return BridgeFrame(
                seq = seq,
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
         * 마우스 이동 프레임을 생성합니다.
         *
         * @param seq 순번 카운터
         * @param deltaX X축 이동량
         * @param deltaY Y축 이동량
         * @return 마우스 이동 프레임
         */
        fun mouseMove(seq: UByte, deltaX: Byte, deltaY: Byte): BridgeFrame {
            return BridgeFrame(
                seq = seq,
                buttons = 0u,
                deltaX = deltaX,
                deltaY = deltaY,
                wheel = 0,
                modifiers = 0u,
                keyCode1 = 0u,
                keyCode2 = 0u
            )
        }

        /**
         * 마우스 버튼 프레임을 생성합니다.
         *
         * @param seq 순번 카운터
         * @param buttons 버튼 상태 비트 플래그
         * @return 마우스 버튼 프레임
         */
        fun mouseButton(seq: UByte, buttons: UByte): BridgeFrame {
            return BridgeFrame(
                seq = seq,
                buttons = buttons,
                deltaX = 0,
                deltaY = 0,
                wheel = 0,
                modifiers = 0u,
                keyCode1 = 0u,
                keyCode2 = 0u
            )
        }

        /**
         * 휠 스크롤 프레임을 생성합니다.
         *
         * @param seq 순번 카운터
         * @param wheel 휠 이동량
         * @return 휠 스크롤 프레임
         */
        fun mouseWheel(seq: UByte, wheel: Byte): BridgeFrame {
            return BridgeFrame(
                seq = seq,
                buttons = 0u,
                deltaX = 0,
                deltaY = 0,
                wheel = wheel,
                modifiers = 0u,
                keyCode1 = 0u,
                keyCode2 = 0u
            )
        }

        /**
         * 키보드 입력 프레임을 생성합니다.
         *
         * @param seq 순번 카운터
         * @param modifiers 모디파이어 키 비트 플래그
         * @param keyCode1 주요 키 코드
         * @param keyCode2 보조 키 코드 (기본값: 0)
         * @return 키보드 입력 프레임
         */
        fun keyboard(
            seq: UByte,
            modifiers: UByte,
            keyCode1: UByte,
            keyCode2: UByte = 0u
        ): BridgeFrame {
            return BridgeFrame(
                seq = seq,
                buttons = 0u,
                deltaX = 0,
                deltaY = 0,
                wheel = 0,
                modifiers = modifiers,
                keyCode1 = keyCode1,
                keyCode2 = keyCode2
            )
        }
    }
}

