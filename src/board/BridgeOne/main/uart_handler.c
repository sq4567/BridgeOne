#include "uart_handler.h"
#include "driver/uart.h"
#include "esp_log.h"

static const char *TAG = "UART_HANDLER";

/**
 * UART 드라이버 초기화.
 *
 * ESP32-S3의 UART_NUM_0을 1Mbps 8N1 설정으로 초기화합니다.
 * ESP32-S3-DevkitC-1은 내장 USB-to-UART 브릿지를 사용하므로
 * GPIO 핀 설정(uart_set_pin)은 필요하지 않습니다.
 *
 * 초기화 절차:
 * 1. uart_param_config()로 UART 파라미터 설정
 * 2. uart_driver_install()로 UART 드라이버 설치 및 버퍼 할당
 *
 * @return
 *   - ESP_OK: 초기화 성공
 *   - 이외: 오류 코드
 */
int uart_init(void) {
    // UART 설정 구조체 초기화
    uart_config_t uart_config = {
        .baud_rate = UART_BAUDRATE,
        .data_bits = UART_DATA_BITS,
        .parity = UART_PARITY,
        .stop_bits = UART_STOP_BITS,
        .flow_ctrl = UART_HW_FLOWCTRL_DISABLE,  // 흐름 제어 미사용
    };

    // UART 파라미터 설정
    esp_err_t ret = uart_param_config(UART_NUM, &uart_config);
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "Failed to set UART parameter config: %s", esp_err_to_name(ret));
        return ret;
    }
    ESP_LOGI(TAG, "UART parameter configured: %d bps, 8N1", UART_BAUDRATE);

    // UART 드라이버 설치
    // - 파라미터: UART 번호, RX 버퍼 크기, TX 버퍼 크기, 큐 크기, 큐 핸들, 인터럽트 할당 플래그
    // - queue_size: 0 (UART 큐를 사용하지 않음, Phase 2.1.2.2에서 FreeRTOS 큐 사용)
    // - uart_queue: NULL (큐를 사용하지 않으므로 NULL)
    // - intr_alloc_flags: 0 (기본 설정)
    ret = uart_driver_install(
        UART_NUM,
        UART_RX_BUFFER_SIZE,
        UART_TX_BUFFER_SIZE,
        0,      // queue_size: 0 (UART 내부 큐 미사용)
        NULL,   // uart_queue: NULL (QueueHandle_t* 타입)
        0       // intr_alloc_flags: 기본값
    );
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "Failed to install UART driver: %s", esp_err_to_name(ret));
        return ret;
    }
    ESP_LOGI(TAG, "UART driver installed with RX buffer (%d) and TX buffer (%d)",
             UART_RX_BUFFER_SIZE, UART_TX_BUFFER_SIZE);

    ESP_LOGI(TAG, "UART initialization completed successfully");
    return ESP_OK;
}
