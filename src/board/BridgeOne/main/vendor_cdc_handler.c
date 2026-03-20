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
#include "connection_state.h"
#include "tusb.h"
#include "esp_log.h"
#include "esp_timer.h"
#include "cJSON.h"
#include "freertos/task.h"
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

    // CDC TX FIFO 가용 공간 확인 (부족 시 최대 20ms 대기)
    uint32_t available = tud_cdc_write_available();
    if (available < frame_len) {
        tud_cdc_write_flush();
        for (int i = 0; i < 20 && tud_cdc_write_available() < frame_len; i++) {
            vTaskDelay(pdMS_TO_TICKS(1));
        }
        available = tud_cdc_write_available();
        if (available < frame_len) {
            ESP_LOGW(TAG, "TX FIFO insufficient: avail=%lu need=%u (cmd=0x%02X)",
                     (unsigned long)available, frame_len, command);
            return false;
        }
    }

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
    uint8_t  payload[VCDC_MAX_PAYLOAD_SIZE + 1]; // +1: JSON null 종료용
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

bool vendor_cdc_parser_is_active(void)
{
    return parser_ctx.state != VCDC_PARSE_WAIT_HEADER;
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

// ==================== 명령 핸들러 (스켈레톤) ====================

/**
 * PING 명령 핸들러.
 * Server→ESP: Keep-alive ping 수신 → PONG 응답 전송.
 */
static void handle_cmd_ping(const vendor_cdc_frame_t *frame, cJSON *json)
{
    ESP_LOGI(TAG, "PING received (payload_len=%u)", frame->payload_len);

    // PONG 응답: 수신한 payload를 그대로 에코백
    vendor_cdc_send_frame(VCDC_CMD_PONG, frame->payload, frame->payload_len);
}

/** 지원 프로토콜 버전 */
#define AUTH_PROTOCOL_VERSION "1.0"

/** 디바이스 펌웨어 버전 */
#define AUTH_FW_VERSION "1.0.0"

/**
 * 인증 검증 함수 (모듈화).
 * 현재는 단순 에코백 방식: challenge를 그대로 반환.
 * 추후 HMAC-SHA256 등으로 교체 가능한 구조.
 *
 * @param challenge 서버로부터 수신한 챌린지 문자열
 * @param out_response 응답 문자열을 저장할 버퍼
 * @param out_len 버퍼 크기
 * @return true: 검증 성공 (응답 생성 완료), false: 실패
 */
static bool auth_verify(const char *challenge, char *out_response, size_t out_len)
{
    if (challenge == NULL || out_response == NULL || out_len == 0) {
        return false;
    }

    // 에코백 방식: challenge를 그대로 response로 복사
    size_t challenge_len = strlen(challenge);
    if (challenge_len >= out_len) {
        ESP_LOGE(TAG, "Challenge too long for response buffer: %u >= %u",
                 (unsigned)challenge_len, (unsigned)out_len);
        return false;
    }

    strncpy(out_response, challenge, out_len - 1);
    out_response[out_len - 1] = '\0';
    return true;
}

/**
 * AUTH_CHALLENGE 명령 핸들러.
 * Server→ESP: 인증 챌린지 수신 → 에코백 응답 전송.
 *
 * 수신 JSON: {"command":"AUTH_CHALLENGE","challenge":"<hex>","version":"1.0"}
 * 응답 JSON: {"command":"AUTH_RESPONSE","response":"<echo>","device":"BridgeOne","fw_version":"1.0.0"}
 */
static void handle_cmd_auth_challenge(const vendor_cdc_frame_t *frame, cJSON *json)
{
    ESP_LOGI(TAG, "AUTH_CHALLENGE received (payload_len=%u)", frame->payload_len);

    // JSON 페이로드 필수
    if (json == NULL) {
        ESP_LOGE(TAG, "AUTH_CHALLENGE: JSON payload required");
        connection_state_reset();
        return;
    }

    // 상태 전이: IDLE → AUTH_PENDING
    if (!connection_state_transition(CONN_STATE_AUTH_PENDING)) {
        ESP_LOGE(TAG, "AUTH_CHALLENGE: State transition to AUTH_PENDING failed (current=%s)",
                 connection_state_name(connection_state_get()));
        connection_state_reset();
        return;
    }
    ESP_LOGI(TAG, "State: %s", connection_state_name(connection_state_get()));

    // challenge 필드 추출
    cJSON *challenge_item = cJSON_GetObjectItemCaseSensitive(json, "challenge");
    if (!cJSON_IsString(challenge_item) || challenge_item->valuestring == NULL) {
        ESP_LOGE(TAG, "AUTH_CHALLENGE: 'challenge' field missing or invalid");
        connection_state_reset();
        return;
    }
    const char *challenge = challenge_item->valuestring;

    // version 필드로 프로토콜 버전 호환성 확인
    cJSON *version_item = cJSON_GetObjectItemCaseSensitive(json, "version");
    if (cJSON_IsString(version_item) && version_item->valuestring != NULL) {
        ESP_LOGI(TAG, "AUTH_CHALLENGE: protocol version=%s", version_item->valuestring);
        // 현재는 버전 체크를 경고 수준으로만 처리 (호환성 유지)
        if (strcmp(version_item->valuestring, AUTH_PROTOCOL_VERSION) != 0) {
            ESP_LOGW(TAG, "Protocol version mismatch: server=%s, device=%s",
                     version_item->valuestring, AUTH_PROTOCOL_VERSION);
        }
    }

    // 인증 검증 (에코백)
    char response[128];
    if (!auth_verify(challenge, response, sizeof(response))) {
        ESP_LOGE(TAG, "AUTH_CHALLENGE: auth_verify() failed");
        connection_state_reset();
        return;
    }

    // AUTH_RESPONSE JSON 생성
    cJSON *resp_json = cJSON_CreateObject();
    if (resp_json == NULL) {
        ESP_LOGE(TAG, "AUTH_CHALLENGE: Failed to create response JSON");
        connection_state_reset();
        return;
    }

    cJSON_AddStringToObject(resp_json, "command", "AUTH_RESPONSE");
    cJSON_AddStringToObject(resp_json, "response", response);
    cJSON_AddStringToObject(resp_json, "device", "BridgeOne");
    cJSON_AddStringToObject(resp_json, "fw_version", AUTH_FW_VERSION);

    char *resp_str = cJSON_PrintUnformatted(resp_json);
    cJSON_Delete(resp_json);

    if (resp_str == NULL) {
        ESP_LOGE(TAG, "AUTH_CHALLENGE: Failed to serialize response JSON");
        connection_state_reset();
        return;
    }

    // AUTH_RESPONSE 프레임 전송
    bool send_ok = vendor_cdc_send_frame(
        VCDC_CMD_AUTH_RESPONSE,
        (const uint8_t *)resp_str,
        (uint16_t)strlen(resp_str)
    );

    free(resp_str);

    if (send_ok) {
        // 상태 전이: AUTH_PENDING → AUTH_OK
        if (connection_state_transition(CONN_STATE_AUTH_OK)) {
            ESP_LOGI(TAG, "AUTH_RESPONSE sent, State: %s",
                     connection_state_name(connection_state_get()));
        } else {
            ESP_LOGE(TAG, "State transition to AUTH_OK failed");
            connection_state_reset();
        }
    } else {
        ESP_LOGE(TAG, "AUTH_RESPONSE send failed");
        connection_state_reset();
    }
}

/**
 * STATE_SYNC 명령 핸들러 (스켈레톤).
 * Server→ESP: 상태 동기화 요청. 실제 로직은 Phase 3.4에서 구현.
 */
static void handle_cmd_state_sync(const vendor_cdc_frame_t *frame, cJSON *json)
{
    ESP_LOGI(TAG, "STATE_SYNC received (payload_len=%u) - skeleton handler", frame->payload_len);

    // 스켈레톤: ACK 에코백
    vendor_cdc_send_frame(VCDC_CMD_STATE_SYNC_ACK, frame->payload, frame->payload_len);
}

/**
 * ERROR 명령 핸들러.
 * 양방향: 오류 응답 수신 시 로그 출력.
 */
static void handle_cmd_error(const vendor_cdc_frame_t *frame, cJSON *json)
{
    if (frame->payload_len >= 2) {
        ESP_LOGW(TAG, "ERROR received: original_cmd=0x%02X, error_code=0x%02X",
                 frame->payload[0], frame->payload[1]);
    } else {
        ESP_LOGW(TAG, "ERROR received (payload_len=%u)", frame->payload_len);
    }
}

// ==================== 명령 디스패처 ====================

/** 명령 핸들러 함수 포인터 타입 */
typedef void (*vcdc_cmd_handler_t)(const vendor_cdc_frame_t *frame, cJSON *json);

/** 명령 코드 → 핸들러 매핑 테이블 엔트리 */
typedef struct {
    uint8_t             command;
    vcdc_cmd_handler_t  handler;
    const char         *name;
} vcdc_cmd_entry_t;

/** 명령 핸들러 디스패치 테이블 */
static const vcdc_cmd_entry_t cmd_dispatch_table[] = {
    { VCDC_CMD_PING,            handle_cmd_ping,            "PING"           },
    { VCDC_CMD_AUTH_CHALLENGE,   handle_cmd_auth_challenge,  "AUTH_CHALLENGE" },
    { VCDC_CMD_STATE_SYNC,       handle_cmd_state_sync,      "STATE_SYNC"    },
    { VCDC_CMD_ERROR,            handle_cmd_error,           "ERROR"         },
};

#define CMD_DISPATCH_TABLE_SIZE \
    (sizeof(cmd_dispatch_table) / sizeof(cmd_dispatch_table[0]))

/**
 * 명령 코드에 해당하는 핸들러를 찾아 호출.
 *
 * @param frame 파싱된 프레임
 * @param json  JSON 파싱 결과 (payload가 없거나 JSON이 아니면 NULL)
 * @return true: 핸들러 발견 및 호출, false: 미지원 명령
 */
static bool dispatch_command(const vendor_cdc_frame_t *frame, cJSON *json)
{
    for (size_t i = 0; i < CMD_DISPATCH_TABLE_SIZE; i++) {
        if (cmd_dispatch_table[i].command == frame->command) {
            ESP_LOGD(TAG, "Dispatching command: %s (0x%02X)",
                     cmd_dispatch_table[i].name, frame->command);
            cmd_dispatch_table[i].handler(frame, json);
            return true;
        }
    }
    return false;
}

// ==================== Vendor CDC 태스크 ====================

void vendor_cdc_task(void *param)
{
    (void)param;

    ESP_LOGI(TAG, "Vendor CDC task started");

    vendor_cdc_frame_t frame;

    while (1) {
        // 큐에서 파싱된 프레임 대기 (무한 대기)
        if (xQueueReceive(vendor_cdc_frame_queue, &frame, portMAX_DELAY) != pdPASS) {
            continue;
        }

        ESP_LOGI(TAG, "Frame received: cmd=0x%02X, payload_len=%u, crc=0x%04X",
                 frame.command, frame.payload_len, frame.crc16);

        // JSON 페이로드 파싱 (payload가 있는 경우)
        cJSON *json = NULL;

        if (frame.payload_len > 0) {
            // payload를 null-terminate (버퍼는 VCDC_MAX_PAYLOAD_SIZE+1이므로 항상 안전)
            frame.payload[frame.payload_len] = '\0';

            json = cJSON_Parse((const char *)frame.payload);

            if (json == NULL) {
                // JSON 파싱 실패: 바이너리 payload일 수 있으므로 경고만 출력
                // (모든 payload가 JSON인 것은 아님)
                ESP_LOGD(TAG, "Payload is not JSON (cmd=0x%02X, len=%u)",
                         frame.command, frame.payload_len);
            } else {
                ESP_LOGD(TAG, "JSON parsed OK (cmd=0x%02X)", frame.command);
            }
        }

        // 명령 디스패처 호출
        if (!dispatch_command(&frame, json)) {
            ESP_LOGW(TAG, "Unknown command: 0x%02X (payload_len=%u)",
                     frame.command, frame.payload_len);

            // 미지원 명령 에러 응답
            uint8_t err_payload[2] = {
                frame.command,  // 원래 명령 코드
                0x01            // 에러 코드: 미지원 명령
            };
            vendor_cdc_send_frame(VCDC_CMD_ERROR, err_payload, sizeof(err_payload));
        }

        // cJSON 메모리 해제
        if (json != NULL) {
            cJSON_Delete(json);
        }
    }
}
