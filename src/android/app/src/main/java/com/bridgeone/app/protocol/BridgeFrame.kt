package com.bridgeone.app.protocol

/**
 * BridgeOne 8바이트 고정 크기 프레임 데이터 클래스
 *
 * Android 앱에서 터치 입력을 ESP32-S3로 전송하기 위한 프로토콜 정의입니다.
 * 모든 필드는 Little-Endian 바이트 순서로 직렬화됩니다.
 *
 * 프레임 구조 (8바이트):
 * ```
 * ┌────┬─────────┬────────┬────────┬────────┬──────────┬──────────┬──────────┐
 * │Seq │ Buttons │ DeltaX │ DeltaY │ Wheel  │Modifiers │ KeyCode1 │ KeyCode2 │
 * │ 1B │   1B    │   1B   │   1B   │   1B   │    1B    │    1B    │    1B    │
 * └────┴─────────┴────────┴────────┴────────┴──────────┴──────────┴──────────┘
 * ```
 *
 * @property seq 패킷 순번 (0~255 순환), 유실 감지용
 * @property buttons 마우스 버튼 상태 비트:
 *   - bit0: LEFT_CLICK (0x01)
 *   - bit1: RIGHT_CLICK (0x02)
 *   - bit2: MIDDLE_CLICK (0x04)
 * @property deltaX X축 상대 이동값 (-128 ~ 127)
 * @property deltaY Y축 상대 이동값 (-128 ~ 127)
 * @property wheel 마우스 휠 값 (-128 ~ 127), Boot 모드에서는 0
 * @property modifiers 키보드 수정자 키 상태 비트:
 *   - bit0: LEFT_CTRL (0x01)
 *   - bit1: LEFT_SHIFT (0x02)
 *   - bit2: LEFT_ALT (0x04)
 *   - bit3: LEFT_GUI (0x08)
 * @property keyCode1 첫 번째 키코드 (HID 키코드 테이블)
 * @property keyCode2 두 번째 키코드 (HID 키코드 테이블)
 */
data class BridgeFrame(
    val seq: UByte,           // 패킷 순번 (0~255)
    val buttons: UByte,       // 마우스 버튼 비트
    val deltaX: Byte,         // X축 이동값
    val deltaY: Byte,         // Y축 이동값
    val wheel: Byte,          // 휠 값
    val modifiers: UByte,     // 키보드 수정자 키
    val keyCode1: UByte,      // 첫 번째 키코드
    val keyCode2: UByte       // 두 번째 키코드
) {
    companion object {
        // 프레임 크기 상수
        const val FRAME_SIZE_BYTES = 8

        // 마우스 버튼 비트 마스크
        val BUTTON_LEFT_MASK = 0x01.toUByte()
        val BUTTON_RIGHT_MASK = 0x02.toUByte()
        val BUTTON_MIDDLE_MASK = 0x04.toUByte()

        // 키보드 수정자 키 비트 마스크
        val MODIFIER_LEFT_CTRL_MASK = 0x01.toUByte()
        val MODIFIER_LEFT_SHIFT_MASK = 0x02.toUByte()
        val MODIFIER_LEFT_ALT_MASK = 0x04.toUByte()
        val MODIFIER_LEFT_GUI_MASK = 0x08.toUByte()

        /**
         * 기본값으로 초기화된 BridgeFrame을 생성합니다.
         *
         * 모든 필드가 0으로 초기화되며, 시퀀스 번호는 0부터 시작합니다.
         *
         * @return 기본값 BridgeFrame
         */
        fun default(): BridgeFrame = BridgeFrame(
            seq = 0u,
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
     * BridgeFrame을 8바이트 ByteArray로 직렬화합니다.
     *
     * Little-Endian 바이트 순서로 직렬화되며, 모든 필드가 1바이트이므로
     * 실제로는 바이트 순서가 중요하지 않습니다.
     *
     * 바이트 배열 형식:
     * ```
     * [0] = seq
     * [1] = buttons
     * [2] = deltaX
     * [3] = deltaY
     * [4] = wheel
     * [5] = modifiers
     * [6] = keyCode1
     * [7] = keyCode2
     * ```
     *
     * @return 8바이트 ByteArray 형식의 직렬화된 프레임
     */
    fun toByteArray(): ByteArray = ByteArray(FRAME_SIZE_BYTES).apply {
        this[0] = seq.toByte()
        this[1] = buttons.toByte()
        this[2] = deltaX
        this[3] = deltaY
        this[4] = wheel
        this[5] = modifiers.toByte()
        this[6] = keyCode1.toByte()
        this[7] = keyCode2.toByte()
    }

    /**
     * 왼쪽 클릭 버튼이 눌린 상태인지 확인합니다.
     *
     * @return 왼쪽 클릭 버튼이 눌린 상태이면 true
     */
    fun isLeftClickPressed(): Boolean = (buttons and BUTTON_LEFT_MASK) != 0u.toUByte()

    /**
     * 오른쪽 클릭 버튼이 눌린 상태인지 확인합니다.
     *
     * @return 오른쪽 클릭 버튼이 눌린 상태이면 true
     */
    fun isRightClickPressed(): Boolean = (buttons and BUTTON_RIGHT_MASK) != 0u.toUByte()

    /**
     * 중앙 클릭 버튼이 눌린 상태인지 확인합니다.
     *
     * @return 중앙 클릭 버튼이 눌린 상태이면 true
     */
    fun isMiddleClickPressed(): Boolean = (buttons and BUTTON_MIDDLE_MASK) != 0u.toUByte()

    /**
     * LEFT_CTRL 수정자 키가 활성화되어 있는지 확인합니다.
     *
     * @return LEFT_CTRL 수정자 키가 활성화 상태이면 true
     */
    fun isCtrlModifierActive(): Boolean = (modifiers and MODIFIER_LEFT_CTRL_MASK) != 0u.toUByte()

    /**
     * LEFT_SHIFT 수정자 키가 활성화되어 있는지 확인합니다.
     *
     * @return LEFT_SHIFT 수정자 키가 활성화 상태이면 true
     */
    fun isShiftModifierActive(): Boolean = (modifiers and MODIFIER_LEFT_SHIFT_MASK) != 0u.toUByte()

    /**
     * LEFT_ALT 수정자 키가 활성화되어 있는지 확인합니다.
     *
     * @return LEFT_ALT 수정자 키가 활성화 상태이면 true
     */
    fun isAltModifierActive(): Boolean = (modifiers and MODIFIER_LEFT_ALT_MASK) != 0u.toUByte()

    /**
     * LEFT_GUI (Windows 키) 수정자 키가 활성화되어 있는지 확인합니다.
     *
     * @return LEFT_GUI 수정자 키가 활성화 상태이면 true
     */
    fun isGuiModifierActive(): Boolean = (modifiers and MODIFIER_LEFT_GUI_MASK) != 0u.toUByte()
}
