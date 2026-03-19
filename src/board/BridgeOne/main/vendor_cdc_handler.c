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
