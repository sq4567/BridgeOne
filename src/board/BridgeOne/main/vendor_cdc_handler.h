/**
 * @file vendor_cdc_handler.h
 * @brief Vendor CDC 바이너리 프로토콜 정의 및 프레임 처리
 *
 * Windows 서버와 ESP32-S3 간의 Vendor CDC 바이너리 통신을 위한
 * 프레임 구조체, 명령 코드, CRC16 검증 함수를 정의합니다.
 *
 * 프레임 구조:
 * ┌────────┬─────────┬────────────┬──────────┬──────────┐
 * │ Header │ Command │ Length(LE) │ Payload  │ CRC16(LE)│
 * │  0xFF  │   1B    │    2B      │ 0~448B   │   2B     │
 * └────────┴─────────┴────────────┴──────────┴──────────┘
 *
 * CRC16 계산 범위: payload만 (header, command, length 제외)
 *
 * 참조:
 * - docs/windows/technical-specification-server.md §2.3.3
 * - docs/development-plans/phase-3-1-esp32-vendor-cdc.md
 */

#ifndef VENDOR_CDC_HANDLER_H
#define VENDOR_CDC_HANDLER_H

#include <stdint.h>
#include <stdbool.h>
#include <stddef.h>

// ==================== 프레임 상수 ====================

/** Vendor CDC 프레임 헤더 바이트 (텍스트 명령과 구분용) */
#define VCDC_FRAME_HEADER       0xFF

/** 최대 페이로드 크기 (바이트) */
#define VCDC_MAX_PAYLOAD_SIZE   448

/** 프레임 오버헤드: header(1) + command(1) + length(2) + crc16(2) = 6바이트 */
#define VCDC_FRAME_OVERHEAD     6

/** 최대 프레임 크기: 오버헤드 + 최대 페이로드 = 454바이트 */
#define VCDC_MAX_FRAME_SIZE     (VCDC_FRAME_OVERHEAD + VCDC_MAX_PAYLOAD_SIZE)

// ==================== 명령 코드 정의 ====================

/**
 * Vendor CDC 명령 코드.
 *
 * Windows 서버와 ESP32-S3 간 통신에 사용되는 명령 코드입니다.
 * 각 명령의 방향(Server→ESP 또는 ESP→Server)에 주의하세요.
 */
typedef enum {
    VCDC_CMD_AUTH_CHALLENGE  = 0x01,  // Server→ESP: 인증 챌린지
    VCDC_CMD_AUTH_RESPONSE   = 0x02,  // ESP→Server: 인증 응답
    VCDC_CMD_STATE_SYNC      = 0x03,  // Server→ESP: 상태 동기화 요청
    VCDC_CMD_STATE_SYNC_ACK  = 0x04,  // ESP→Server: 상태 동기화 확인
    VCDC_CMD_PING            = 0x10,  // Server→ESP: Keep-alive ping
    VCDC_CMD_PONG            = 0x11,  // ESP→Server: Keep-alive pong
    VCDC_CMD_MODE_NOTIFY     = 0x20,  // ESP→Server: 모드 변경 알림
    VCDC_CMD_ERROR           = 0xFE,  // 양방향: 오류 응답
} vendor_cdc_cmd_t;

// ==================== 프레임 구조체 ====================

/**
 * Vendor CDC 프레임 구조체.
 *
 * CDC를 통해 수신/송신되는 바이너리 프레임을 나타냅니다.
 * payload는 가변 길이이며, payload_len 필드로 실제 크기를 지정합니다.
 */
typedef struct {
    uint8_t  header;                            // 항상 0xFF
    uint8_t  command;                           // vendor_cdc_cmd_t 값
    uint16_t payload_len;                       // 페이로드 길이 (Little-Endian, 0~448)
    uint8_t  payload[VCDC_MAX_PAYLOAD_SIZE];    // 페이로드 데이터
    uint16_t crc16;                             // CRC16-CCITT (Little-Endian)
} vendor_cdc_frame_t;

// ==================== 함수 선언 ====================

/**
 * CRC16-CCITT 계산 함수.
 *
 * Windows 서버의 C# 구현과 동일한 알고리즘입니다.
 * 다항식: 0x1021, 초기값: 0x0000
 *
 * @param data 계산 대상 데이터 (payload만)
 * @param length 데이터 길이
 * @return CRC16 값
 */
uint16_t vendor_cdc_crc16(const uint8_t *data, size_t length);

/**
 * Vendor CDC 프레임 조립 및 전송.
 *
 * command와 payload를 받아 완전한 프레임을 구성하고 CDC로 전송합니다.
 * - 헤더(0xFF) 자동 부착
 * - CRC16 자동 계산 및 부착
 * - tud_cdc_write() + tud_cdc_write_flush()로 전송
 *
 * @param command 명령 코드 (vendor_cdc_cmd_t)
 * @param payload 페이로드 데이터 (NULL 가능, payload_len=0일 때)
 * @param payload_len 페이로드 길이 (0~448)
 * @return true: 전송 성공, false: 실패 (페이로드 초과 또는 CDC 미연결)
 */
bool vendor_cdc_send_frame(uint8_t command, const uint8_t *payload, uint16_t payload_len);

#endif // VENDOR_CDC_HANDLER_H
