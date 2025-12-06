#include "usb_cdc_log.h"
#include "tusb.h"
#include "esp_log.h"
#include "esp_system.h"  // esp_restart()
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"  // vTaskDelay()
#include <stdio.h>
#include <stdarg.h>
#include <string.h>
#include <ctype.h>  // tolower()

static const char* TAG = "USB_CDC_LOG";

// CDC 로깅 활성화 상태
static bool cdc_log_enabled = false;

// 기존 vprintf 함수 포인터 (복원용)
static vprintf_like_t original_vprintf = NULL;

// CDC 출력 버퍼 (스레드 안전을 위해 정적 할당)
// LF → CRLF 변환으로 최대 2배 크기 필요
#define CDC_LOG_BUFFER_SIZE 512
#define CDC_OUTPUT_BUFFER_SIZE (CDC_LOG_BUFFER_SIZE * 2)
static char cdc_log_buffer[CDC_LOG_BUFFER_SIZE];
static char cdc_output_buffer[CDC_OUTPUT_BUFFER_SIZE];

/**
 * LF(\n)를 CRLF(\r\n)로 변환하는 헬퍼 함수.
 *
 * Windows 터미널(Tera Term 등)은 CRLF를 기대합니다.
 * LF만 전송하면 줄바꿈 시 커서가 줄 시작으로 돌아가지 않아
 * 로그가 계단식으로 밀려나가는 현상이 발생합니다.
 *
 * @param src 원본 문자열
 * @param src_len 원본 문자열 길이
 * @param dst 출력 버퍼 (최대 src_len * 2 크기 필요)
 * @param dst_size 출력 버퍼 크기
 * @return 변환된 문자열 길이
 */
static int convert_lf_to_crlf(const char* src, int src_len, char* dst, int dst_size) {
    int j = 0;
    for (int i = 0; i < src_len && j < dst_size - 1; i++) {
        if (src[i] == '\n') {
            // LF 앞에 CR이 없으면 CR 추가
            if (i == 0 || src[i - 1] != '\r') {
                if (j < dst_size - 2) {
                    dst[j++] = '\r';
                }
            }
        }
        dst[j++] = src[i];
    }
    dst[j] = '\0';
    return j;
}

/**
 * USB CDC로 로그를 출력하는 커스텀 vprintf 함수.
 *
 * esp_log_set_vprintf()에 등록되어 ESP_LOG 출력을 USB CDC로 리다이렉트합니다.
 * Windows 터미널 호환성을 위해 LF → CRLF 변환이 자동으로 적용됩니다.
 *
 * @param fmt 포맷 문자열
 * @param args 가변 인자 리스트
 * @return 출력된 문자 수
 */
static int cdc_vprintf(const char* fmt, va_list args) {
    // 포맷된 문자열 생성
    int len = vsnprintf(cdc_log_buffer, CDC_LOG_BUFFER_SIZE, fmt, args);

    if (len <= 0) {
        return len;
    }

    // 버퍼 오버플로우 방지
    if (len >= CDC_LOG_BUFFER_SIZE) {
        len = CDC_LOG_BUFFER_SIZE - 1;
        cdc_log_buffer[len] = '\0';
    }

    // USB CDC가 연결되어 있는지 확인
    if (tud_cdc_connected()) {
        // LF → CRLF 변환 (Windows 터미널 호환성)
        int output_len = convert_lf_to_crlf(cdc_log_buffer, len,
                                             cdc_output_buffer, CDC_OUTPUT_BUFFER_SIZE);

        // CDC로 출력
        uint32_t written = tud_cdc_write(cdc_output_buffer, output_len);

        // 즉시 전송 (버퍼링 방지)
        tud_cdc_write_flush();

        return (int)written;
    }

    // CDC 미연결 시 기본 출력 (printf)으로 폴백
    // 이 경우 출력이 손실될 수 있지만, 시스템 안정성을 위해 허용
    return len;
}

bool usb_cdc_log_init(void) {
    if (cdc_log_enabled) {
        ESP_LOGW(TAG, "CDC logging already enabled");
        return true;
    }

    // 기존 vprintf 함수 저장 (복원용)
    original_vprintf = esp_log_set_vprintf(cdc_vprintf);

    cdc_log_enabled = true;

    // 초기화 완료 메시지 (이 메시지부터 CDC로 출력됨)
    ESP_LOGI(TAG, "USB CDC logging initialized");
    ESP_LOGI(TAG, "Debug output redirected to Native USB OTG (Port 2)");
    ESP_LOGI(TAG, "Connect PC to Micro-USB port for debug logs");

    return true;
}

void usb_cdc_log_deinit(void) {
    if (!cdc_log_enabled) {
        return;
    }

    // 기존 vprintf 함수 복원
    if (original_vprintf != NULL) {
        esp_log_set_vprintf(original_vprintf);
        original_vprintf = NULL;
    }

    cdc_log_enabled = false;

    ESP_LOGI(TAG, "USB CDC logging disabled, reverted to UART output");
}

bool usb_cdc_log_is_enabled(void) {
    return cdc_log_enabled;
}

/**
 * USB CDC로 문자열을 직접 출력하는 함수.
 *
 * ESP_LOG가 아닌 직접 문자열 출력이 필요할 때 사용합니다.
 * LF → CRLF 변환이 자동으로 적용됩니다.
 *
 * @param str 출력할 문자열 (NULL-terminated)
 */
void usb_cdc_log_write(const char* str) {
    if (str == NULL) {
        return;
    }

    size_t len = strlen(str);

    if (tud_cdc_connected() && len > 0) {
        // LF → CRLF 변환 (Windows 터미널 호환성)
        int output_len = convert_lf_to_crlf(str, (int)len,
                                             cdc_output_buffer, CDC_OUTPUT_BUFFER_SIZE);
        tud_cdc_write(cdc_output_buffer, output_len);
        tud_cdc_write_flush();
    }
}

// ==================== TinyUSB CDC 콜백 함수 구현 ====================
/**
 * TinyUSB CDC Line State 콜백 함수.
 *
 * 호스트가 DTR(Data Terminal Ready) 또는 RTS(Request To Send) 신호를 변경하면 호출됩니다.
 * DTR=true는 보통 호스트의 시리얼 터미널이 연결되었음을 의미합니다.
 *
 * @param itf CDC 인터페이스 번호 (0-based)
 * @param dtr Data Terminal Ready 상태 (true: 연결됨, false: 연결 해제)
 * @param rts Request To Send 상태 (일반적으로 사용 안 함)
 */
void tud_cdc_line_state_cb(uint8_t itf, bool dtr, bool rts) {
    (void) rts;  // 사용하지 않는 매개변수

    if (dtr) {
        // 호스트가 CDC 포트를 열었음 (시리얼 터미널 연결)
        ESP_LOGI(TAG, "CDC interface %d connected (DTR=true)", itf);
        usb_cdc_log_write("\n\n=== BridgeOne USB CDC Debug Log ===\n");
        usb_cdc_log_write("Connected successfully. Logs will appear below.\n\n");
    } else {
        // 호스트가 CDC 포트를 닫았음 (시리얼 터미널 연결 해제)
        ESP_LOGI(TAG, "CDC interface %d disconnected (DTR=false)", itf);
    }
}

// ==================== CDC 명령 처리 ====================
// CDC 명령 수신 버퍼
#define CDC_CMD_BUFFER_SIZE 64
static char cdc_cmd_buffer[CDC_CMD_BUFFER_SIZE];
static int cdc_cmd_index = 0;

/**
 * CDC 명령어 처리 함수.
 *
 * 지원 명령어:
 * - reset, RESET: 소프트웨어 리셋 수행
 * - help, HELP: 사용 가능한 명령어 목록 출력
 *
 * @param cmd NULL-terminated 명령어 문자열
 */
static void process_cdc_command(const char* cmd) {
    // 명령어를 소문자로 변환하여 비교
    char lower_cmd[CDC_CMD_BUFFER_SIZE];
    int i = 0;
    while (cmd[i] && i < CDC_CMD_BUFFER_SIZE - 1) {
        lower_cmd[i] = tolower((unsigned char)cmd[i]);
        i++;
    }
    lower_cmd[i] = '\0';

    // 명령어 처리
    if (strcmp(lower_cmd, "reset") == 0 || strcmp(lower_cmd, "reboot") == 0) {
        usb_cdc_log_write("\r\n*** Resetting ESP32-S3... ***\r\n");
        vTaskDelay(pdMS_TO_TICKS(100));  // 메시지 전송 대기
        esp_restart();
    }
    else if (strcmp(lower_cmd, "help") == 0 || strcmp(lower_cmd, "?") == 0) {
        usb_cdc_log_write("\r\n=== BridgeOne CDC Commands ===\r\n");
        usb_cdc_log_write("  reset, reboot  - Software reset\r\n");
        usb_cdc_log_write("  help, ?        - Show this help\r\n");
        usb_cdc_log_write("==============================\r\n");
    }
    else if (strlen(lower_cmd) > 0) {
        usb_cdc_log_write("\r\nUnknown command: ");
        usb_cdc_log_write(cmd);
        usb_cdc_log_write("\r\nType 'help' for available commands.\r\n");
    }
}

/**
 * TinyUSB CDC RX 콜백 함수.
 *
 * 호스트로부터 데이터를 수신하면 호출됩니다.
 * 수신된 데이터를 버퍼에 모아서 줄바꿈(\n 또는 \r) 시 명령어로 처리합니다.
 *
 * 지원 명령어:
 * - reset: 소프트웨어 리셋
 * - help: 명령어 목록
 *
 * @param itf CDC 인터페이스 번호 (0-based)
 */
void tud_cdc_rx_cb(uint8_t itf) {
    uint8_t buf[64];
    uint32_t count;

    while (tud_cdc_available()) {
        count = tud_cdc_read(buf, sizeof(buf));

        for (uint32_t i = 0; i < count; i++) {
            char c = (char)buf[i];

            // 에코백 (입력한 문자를 터미널에 표시)
            if (c >= 32 && c < 127) {
                char echo[2] = {c, '\0'};
                usb_cdc_log_write(echo);
            }

            // Enter 키 (줄바꿈) 처리
            if (c == '\r' || c == '\n') {
                cdc_cmd_buffer[cdc_cmd_index] = '\0';

                // 공백 제거 (trim)
                char* trimmed = cdc_cmd_buffer;
                while (*trimmed == ' ') trimmed++;
                char* end = trimmed + strlen(trimmed) - 1;
                while (end > trimmed && *end == ' ') *end-- = '\0';

                // 명령어 처리
                process_cdc_command(trimmed);

                // 버퍼 초기화
                cdc_cmd_index = 0;
                memset(cdc_cmd_buffer, 0, sizeof(cdc_cmd_buffer));
            }
            // Backspace 처리
            else if (c == 8 || c == 127) {
                if (cdc_cmd_index > 0) {
                    cdc_cmd_index--;
                    usb_cdc_log_write("\b \b");  // 화면에서 문자 삭제
                }
            }
            // 일반 문자 버퍼에 추가
            else if (c >= 32 && c < 127 && cdc_cmd_index < CDC_CMD_BUFFER_SIZE - 1) {
                cdc_cmd_buffer[cdc_cmd_index++] = c;
            }
        }
    }
}

/**
 * TinyUSB CDC Line Coding 콜백 함수.
 *
 * 호스트가 시리얼 통신 설정(baud rate, data bits, stop bits, parity)을 변경하면 호출됩니다.
 * BridgeOne은 가상 CDC이므로 이 설정을 무시하지만, 콜백 함수는 반드시 구현되어야 합니다.
 *
 * @param itf CDC 인터페이스 번호 (0-based)
 * @param p_line_coding 라인 코딩 설정 구조체 포인터
 */
void tud_cdc_line_coding_cb(uint8_t itf, cdc_line_coding_t const* p_line_coding) {
    // 가상 CDC이므로 설정을 무시 (로그만 출력)
    ESP_LOGD(TAG, "CDC line coding changed: baud=%lu, bits=%u, stop=%u, parity=%u",
             p_line_coding->bit_rate, p_line_coding->data_bits,
             p_line_coding->stop_bits, p_line_coding->parity);
}
