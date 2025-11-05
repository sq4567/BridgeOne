package com.bridgeone.app.usb

import com.hoho.android.usbserial.driver.UsbSerialPort

/**
 * USB 상수 정의 및 UART 통신 설정.
 *
 * ESP32-S3 BridgeOne 디바이스와의 USB 연결을 위한 Vendor ID(VID), Product ID(PID),
 * 그리고 UART 통신 파라미터를 정의합니다.
 *
 * 참조:
 * - Espressif VID: https://github.com/espressif/esp-usb
 * - UART 설정: CLAUDE.md "통신 프로토콜" 섹션
 */
object UsbConstants {

    // ========== USB Device Identifiers (ESP32-S3 BridgeOne) ==========

    /**
     * Espressif Systems의 USB Vendor ID.
     * 모든 Espressif 제품이 사용합니다.
     */
    const val ESP32_S3_VID = 0x303A

    /**
     * BridgeOne ESP32-S3 디바이스의 Product ID.
     * TinyUSB 디스크립터(usb_descriptors.c)에서 정의됩니다.
     */
    const val ESP32_S3_PID = 0x82C5

    // ========== UART Communication Settings ==========

    /**
     * UART 통신 속도 (baud rate).
     * Android → ESP32-S3 간 직렬 통신 속도 (1Mbps).
     * CLAUDE.md에 명시: "1Mbps 속도로 Android와 통신"
     */
    const val UART_BAUDRATE = 1000000

    /**
     * UART 데이터 비트 수.
     * 표준 설정: 8비트 데이터
     * CLAUDE.md에 명시: "8N1" 프로토콜
     */
    const val UART_DATA_BITS = 8

    /**
     * UART 정지 비트 수.
     * 표준 설정: 1비트 정지 비트
     * CLAUDE.md에 명시: "8N1" 프로토콜
     */
    const val UART_STOP_BITS = 1

    /**
     * UART 패리티 설정.
     * usb-serial-for-android 라이브러리의 UsbSerialPort.PARITY_NONE 상수.
     * No parity (NONE)
     * CLAUDE.md에 명시: "8N1" 프로토콜
     *
     * 참조: com.hoho.android.usbserial.driver.UsbSerialPort.PARITY_NONE
     */
    const val UART_PARITY = UsbSerialPort.PARITY_NONE

    // ========== USB Timeout and Retry Settings ==========

    /**
     * USB 포트 오픈 시도 타임아웃 (밀리초).
     * 포트 열기 작업이 이 시간 내에 완료되어야 합니다.
     */
    const val USB_OPEN_TIMEOUT_MS = 1000

    /**
     * USB 포트 읽기 작업 타임아웃 (밀리초).
     * 데이터 수신 대기 시간.
     */
    const val USB_READ_TIMEOUT_MS = 100

    /**
     * USB 포트 쓰기 작업 타임아웃 (밀리초).
     * 데이터 송신 대기 시간.
     */
    const val USB_WRITE_TIMEOUT_MS = 1000

    // ========== Frame Protocol Settings ==========

    /**
     * 델타 프레임의 고정 크기 (바이트).
     * CLAUDE.md "UART 델타 프레임" 섹션:
     * - seq (1바이트)
     * - buttons (1바이트)
     * - dx (2바이트, Little-Endian)
     * - dy (2바이트, Little-Endian)
     * - wheel (1바이트)
     * - flags (1바이트)
     */
    const val DELTA_FRAME_SIZE = 8

    /**
     * 순번(sequence number) 최대값.
     * 0~255 범위에서 순환하여 패킷 유실을 감지합니다.
     * CLAUDE.md "UART 델타 프레임" 섹션: "순번 (유실 감지용)"
     */
    const val MAX_SEQUENCE_NUMBER = 255
}

