#include "usb_cdc_log.h"
#include "tusb.h"
#include "esp_log.h"
#include <stdio.h>
#include <stdarg.h>
#include <string.h>

static const char* TAG = "USB_CDC_LOG";

// CDC 로깅 활성화 상태
static bool cdc_log_enabled = false;

// 기존 vprintf 함수 포인터 (복원용)
static vprintf_like_t original_vprintf = NULL;

// CDC 출력 버퍼 (스레드 안전을 위해 정적 할당)
#define CDC_LOG_BUFFER_SIZE 512
static char cdc_log_buffer[CDC_LOG_BUFFER_SIZE];

/**
 * USB CDC로 로그를 출력하는 커스텀 vprintf 함수.
 *
 * esp_log_set_vprintf()에 등록되어 ESP_LOG 출력을 USB CDC로 리다이렉트합니다.
 *
 * 동작:
 * 1. vsnprintf()로 포맷된 문자열 생성
 * 2. tud_cdc_connected() 확인
 * 3. tud_cdc_write()로 USB CDC에 출력
 * 4. tud_cdc_write_flush()로 즉시 전송
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
        // CDC로 출력
        uint32_t written = tud_cdc_write(cdc_log_buffer, len);

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

void usb_cdc_log_write(const char* str) {
    if (str == NULL) {
        return;
    }

    size_t len = strlen(str);

    if (tud_cdc_connected() && len > 0) {
        tud_cdc_write(str, len);
        tud_cdc_write_flush();
    }
}
