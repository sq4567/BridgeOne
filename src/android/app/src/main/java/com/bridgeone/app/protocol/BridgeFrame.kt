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
)
