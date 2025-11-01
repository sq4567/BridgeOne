package com.bridgeone.app.protocol

/**
 * BridgeOne 프로토콜의 8바이트 프레임 데이터 클래스.
 *
 * 이 클래스는 Android 앱에서 생성한 사용자 입력을 ESP32-S3로 전송하는
 * BridgeOne 프로토콜의 기본 데이터 구조를 정의합니다.
 *
 * **프레임 구조 (8 bytes, Little-Endian)**:
 * - Byte 0: seq (시퀀스 번호, 0~255 순환)
 * - Byte 1: buttons (마우스 버튼 비트, bit0=Left, bit1=Right, bit2=Middle)
 * - Byte 2: deltaX (X축 이동값, -128~127)
 * - Byte 3: deltaY (Y축 이동값, -128~127)
 * - Byte 4: wheel (휠 값, -128~127)
 * - Byte 5: modifiers (키보드 수정자 키, bit0=Ctrl, bit1=Shift, bit2=Alt, bit3=Win)
 * - Byte 6: keyCode1 (첫 번째 키코드, 0~255)
 * - Byte 7: keyCode2 (두 번째 키코드, 0~255)
 *
 * @property seq 프레임의 시퀀스 번호 (0~255)
 * @property buttons 마우스 버튼 상태 (비트마스크)
 * @property deltaX X축 이동값
 * @property deltaY Y축 이동값
 * @property wheel 마우스 휠 값
 * @property modifiers 키보드 수정자 키 상태 (비트마스크)
 * @property keyCode1 첫 번째 키보드 키코드
 * @property keyCode2 두 번째 키보드 키코드
 */
data class BridgeFrame(
    val seq: UByte,           // 시퀀스 번호 (0~255)
    val buttons: UByte,       // 마우스 버튼 비트 (Left, Right, Middle)
    val deltaX: Byte,         // X축 이동값 (-128~127)
    val deltaY: Byte,         // Y축 이동값 (-128~127)
    val wheel: Byte,          // 휠 값 (-128~127)
    val modifiers: UByte,     // 키보드 수정자 (Ctrl, Shift, Alt, Win)
    val keyCode1: UByte,      // 첫 번째 키코드 (0~255)
    val keyCode2: UByte       // 두 번째 키코드 (0~255)
) {
    /**
     * BridgeFrame을 8바이트 배열로 직렬화합니다.
     *
     * 각 필드를 순서대로 바이트로 변환하여 Little-Endian 형식의
     * 8바이트 배열을 생성합니다. 이 배열은 UART를 통해 ESP32-S3로
     * 전송됩니다.
     *
     * **변환 규칙**:
     * - UByte (0~255) → 부호 없는 바이트
     * - Byte (-128~127) → 부호 있는 바이트 (2의 보수 표현)
     * - toByteArray() 호출 시 배열 길이는 항상 8입니다.
     *
     * @return 8바이트 배열 [seq, buttons, deltaX, deltaY, wheel, modifiers, keyCode1, keyCode2]
     */
    fun toByteArray(): ByteArray = byteArrayOf(
        seq.toByte(),
        buttons.toByte(),
        deltaX,
        deltaY,
        wheel,
        modifiers.toByte(),
        keyCode1.toByte(),
        keyCode2.toByte()
    )
}
