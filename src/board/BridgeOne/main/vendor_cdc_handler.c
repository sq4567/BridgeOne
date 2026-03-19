/**
 * @file vendor_cdc_handler.c
 * @brief Vendor CDC 바이너리 프로토콜 구현
 *
 * CRC16-CCITT 계산 및 프레임 조립/전송 기능을 구현합니다.
 *
 * CRC16 알고리즘은 Windows 서버의 C# 구현과 동일합니다:
 * - 다항식: 0x1021
 * - 초기값: 0x0000
 * - 계산 범위: payload만
 *
 * 참조:
 * - docs/windows/technical-specification-server.md §2.3.3 (C# 참조 구현)
 * - docs/development-plans/phase-3-1-esp32-vendor-cdc.md
 */

#include "vendor_cdc_handler.h"
#include "tusb.h"
#include "esp_log.h"
#include "esp_timer.h"
#include <string.h>

static const char *TAG = "VENDOR_CDC";

/**
 * CRC16-CCITT 계산 함수.
 *
 * Windows 서버 C# 구현의 정확한 포팅:
 *   ushort crc = 0;
 *   crc ^= (ushort)(data[i] << 8);
 *   if ((crc & 0x8000) != 0) crc = (ushort)((crc << 1) ^ 0x1021);
 *   else crc <<= 1;
 */
uint16_t vendor_cdc_crc16(const uint8_t *data, size_t length)
{
    uint16_t crc = 0x0000;

    for (size_t i = 0; i < length; i++) {
        crc ^= (uint16_t)(data[i] << 8);
        for (int j = 0; j < 8; j++) {
            if (crc & 0x8000) {
                crc = (crc << 1) ^ 0x1021;
            } else {
                crc <<= 1;
            }
        }
    }

    return crc;
}

bool vendor_cdc_send_frame(uint8_t command, const uint8_t *payload, uint16_t payload_len)
{
    // 페이로드 크기 검증
    if (payload_len > VCDC_MAX_PAYLOAD_SIZE) {
        ESP_LOGE(TAG, "Payload too large: %u > %d", payload_len, VCDC_MAX_PAYLOAD_SIZE);
        return false;
    }

    // CDC 연결 확인
    if (!tud_cdc_connected()) {
        ESP_LOGW(TAG, "CDC not connected, frame not sent (cmd=0x%02X)", command);
        return false;
    }

    // 프레임 조립용 버퍼 (정적 할당, 임베디드 환경 안정성)
    uint8_t frame_buf[VCDC_MAX_FRAME_SIZE];
    uint16_t frame_len = 0;

    // Header (1B)
    frame_buf[frame_len++] = VCDC_FRAME_HEADER;

    // Command (1B)
    frame_buf[frame_len++] = command;

    // Length (2B, Little-Endian)
    frame_buf[frame_len++] = (uint8_t)(payload_len & 0xFF);
    frame_buf[frame_len++] = (uint8_t)((payload_len >> 8) & 0xFF);

    // Payload (0~448B)
    if (payload != NULL && payload_len > 0) {
        memcpy(&frame_buf[frame_len], payload, payload_len);
        frame_len += payload_len;
    }

    // CRC16 계산 (payload만 대상)
    uint16_t crc = vendor_cdc_crc16(payload, payload_len);

    // CRC16 (2B, Little-Endian)
    frame_buf[frame_len++] = (uint8_t)(crc & 0xFF);
    frame_buf[frame_len++] = (uint8_t)((crc >> 8) & 0xFF);

    // CDC 전송
    uint32_t written = tud_cdc_write(frame_buf, frame_len);
    tud_cdc_write_flush();

    if (written != frame_len) {
        ESP_LOGW(TAG, "Partial write: %lu/%u bytes (cmd=0x%02X)",
                 (unsigned long)written, frame_len, command);
        return false;
    }

    ESP_LOGD(TAG, "Frame sent: cmd=0x%02X, payload=%u, crc=0x%04X",
             command, payload_len, crc);

    return true;
}

// ==================== 프레임 파싱 상태 머신 ====================

/** 파싱 상태 정의 */
typedef enum {
    VCDC_PARSE_WAIT_HEADER,     // 0xFF 헤더 바이트 대기
    VCDC_PARSE_READ_COMMAND,    // command 바이트 1개 읽기
    VCDC_PARSE_READ_LENGTH,     // length 2바이트 읽기 (Little-Endian)
    VCDC_PARSE_READ_PAYLOAD,    // payload 읽기 (length만큼)
    VCDC_PARSE_READ_CRC,        // CRC16 2바이트 읽기 (Little-Endian)
} vcdc_parse_state_t;

/** 파싱 컨텍스트 (정적 할당, 동적 할당 금지) */
typedef struct {
    vcdc_parse_state_t state;
    uint8_t  command;
    uint16_t payload_len;
    uint16_t payload_received;
    uint8_t  payload[VCDC_MAX_PAYLOAD_SIZE];
    uint8_t  length_buf[2];
    uint8_t  length_bytes_read;
    uint8_t  crc_buf[2];
    uint8_t  crc_bytes_read;
    int64_t  last_byte_time_us;     // 마지막 바이트 수신 시각 (us)
} vcdc_parser_ctx_t;

/** 파싱 타임아웃: 프레임 수신 중 500ms 이상 데이터 없으면 리셋 */
#define VCDC_PARSE_TIMEOUT_US   (500 * 1000)

/** 파싱된 프레임 큐 크기 */
#define VCDC_FRAME_QUEUE_SIZE   5

/** FreeRTOS 큐 핸들 (외부에서 vendor_cdc_task가 수신 대기) */
QueueHandle_t vendor_cdc_frame_queue = NULL;

/** 파서 컨텍스트 (정적 할당) */
static vcdc_parser_ctx_t parser_ctx;

/**
 * 파서 상태를 초기 상태(WAIT_HEADER)로 리셋.
 */
static void parser_state_reset(void)
{
    parser_ctx.state              = VCDC_PARSE_WAIT_HEADER;
    parser_ctx.payload_received   = 0;
    parser_ctx.length_bytes_read  = 0;
    parser_ctx.crc_bytes_read     = 0;
    parser_ctx.last_byte_time_us  = 0;
}

bool vendor_cdc_parser_init(void)
{
    parser_state_reset();

    vendor_cdc_frame_queue = xQueueCreate(
        VCDC_FRAME_QUEUE_SIZE,
        sizeof(vendor_cdc_frame_t)
    );

    if (vendor_cdc_frame_queue == NULL) {
        ESP_LOGE(TAG, "Failed to create vendor CDC frame queue");
        return false;
    }

    ESP_LOGI(TAG, "Vendor CDC parser initialized (queue_size=%d, frame_size=%u)",
             VCDC_FRAME_QUEUE_SIZE, (unsigned)sizeof(vendor_cdc_frame_t));
    return true;
}

void vendor_cdc_parser_reset(void)
{
    if (parser_ctx.state != VCDC_PARSE_WAIT_HEADER) {
        ESP_LOGW(TAG, "Parser reset (was in state %d)", parser_ctx.state);
    }
    parser_state_reset();
}

void vendor_cdc_parser_feed(const uint8_t *data, uint32_t len)
{
    for (uint32_t i = 0; i < len; i++) {
        uint8_t byte = data[i];
        int64_t now  = esp_timer_get_time();

        // 타임아웃 검사: 프레임 수신 중 500ms 이상 데이터 없으면 리셋
        if (parser_ctx.state != VCDC_PARSE_WAIT_HEADER &&
            parser_ctx.last_byte_time_us > 0) {
            if ((now - parser_ctx.last_byte_time_us) > VCDC_PARSE_TIMEOUT_US) {
                ESP_LOGW(TAG, "Parse timeout in state %d, resetting", parser_ctx.state);
                parser_state_reset();
            }
        }

        parser_ctx.last_byte_time_us = now;

        switch (parser_ctx.state) {

        case VCDC_PARSE_WAIT_HEADER:
            if (byte == VCDC_FRAME_HEADER) {
                parser_ctx.state = VCDC_PARSE_READ_COMMAND;
            }
            // 0xFF가 아닌 바이트는 무시 (디버그 텍스트로 간주)
            break;

        case VCDC_PARSE_READ_COMMAND:
            parser_ctx.command = byte;
            parser_ctx.state = VCDC_PARSE_READ_LENGTH;
            parser_ctx.length_bytes_read = 0;
            break;

        case VCDC_PARSE_READ_LENGTH:
            parser_ctx.length_buf[parser_ctx.length_bytes_read++] = byte;

            if (parser_ctx.length_bytes_read >= 2) {
                // Little-Endian으로 length 조립
                parser_ctx.payload_len = (uint16_t)(
                    parser_ctx.length_buf[0] |
                    (parser_ctx.length_buf[1] << 8)
                );

                // 페이로드 크기 검증
                if (parser_ctx.payload_len > VCDC_MAX_PAYLOAD_SIZE) {
                    ESP_LOGE(TAG, "Payload too large: %u > %d, resetting",
                             parser_ctx.payload_len, VCDC_MAX_PAYLOAD_SIZE);
                    parser_state_reset();
                    break;
                }

                if (parser_ctx.payload_len == 0) {
                    // 페이로드 없는 프레임: 바로 CRC 읽기
                    parser_ctx.state = VCDC_PARSE_READ_CRC;
                    parser_ctx.crc_bytes_read = 0;
                } else {
                    parser_ctx.state = VCDC_PARSE_READ_PAYLOAD;
                    parser_ctx.payload_received = 0;
                }
            }
            break;

        case VCDC_PARSE_READ_PAYLOAD:
            parser_ctx.payload[parser_ctx.payload_received++] = byte;

            if (parser_ctx.payload_received >= parser_ctx.payload_len) {
                parser_ctx.state = VCDC_PARSE_READ_CRC;
                parser_ctx.crc_bytes_read = 0;
            }
            break;

        case VCDC_PARSE_READ_CRC:
            parser_ctx.crc_buf[parser_ctx.crc_bytes_read++] = byte;

            if (parser_ctx.crc_bytes_read >= 2) {
                // Little-Endian으로 CRC 조립
                uint16_t received_crc = (uint16_t)(
                    parser_ctx.crc_buf[0] |
                    (parser_ctx.crc_buf[1] << 8)
                );

                // CRC16 검증 (계산 범위: payload만)
                uint16_t computed_crc = vendor_cdc_crc16(
                    parser_ctx.payload, parser_ctx.payload_len
                );

                if (received_crc != computed_crc) {
                    ESP_LOGE(TAG, "CRC mismatch: recv=0x%04X, calc=0x%04X (cmd=0x%02X, len=%u)",
                             received_crc, computed_crc,
                             parser_ctx.command, parser_ctx.payload_len);

                    // CRC 오류 응답 프레임 전송
                    uint8_t err_payload[2] = {
                        parser_ctx.command,  // 원래 명령 코드
                        0x02                 // 에러 코드: CRC 불일치
                    };
                    vendor_cdc_send_frame(VCDC_CMD_ERROR, err_payload, sizeof(err_payload));

                    parser_state_reset();
                    break;
                }

                // FRAME_COMPLETE: 검증 성공한 프레임을 큐에 전달
                vendor_cdc_frame_t frame;
                frame.header      = VCDC_FRAME_HEADER;
                frame.command     = parser_ctx.command;
                frame.payload_len = parser_ctx.payload_len;
                frame.crc16       = received_crc;

                if (parser_ctx.payload_len > 0) {
                    memcpy(frame.payload, parser_ctx.payload, parser_ctx.payload_len);
                }

                if (vendor_cdc_frame_queue != NULL) {
                    BaseType_t status = xQueueSend(
                        vendor_cdc_frame_queue, &frame, pdMS_TO_TICKS(10)
                    );

                    if (status == pdPASS) {
                        ESP_LOGD(TAG, "Frame parsed OK: cmd=0x%02X, len=%u, crc=0x%04X",
                                 frame.command, frame.payload_len, frame.crc16);
                    } else {
                        ESP_LOGW(TAG, "Frame queue full, dropped (cmd=0x%02X)",
                                 frame.command);
                    }
                }

                parser_state_reset();
            }
            break;
        }
    }
}
