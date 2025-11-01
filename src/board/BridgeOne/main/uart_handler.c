#include "uart_handler.h"
#include "driver/uart.h"
#include "esp_log.h"
#include "esp_task_wdt.h"

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

/**
 * UART 수신 프레임 큐.
 *
 * Phase 2.1.2.2에서 app_main()으로부터 초기화되며,
 * uart_task()가 검증된 프레임을 이 큐에 전송합니다.
 */
QueueHandle_t frame_queue = NULL;

/**
 * 프레임 시퀀스 번호 검증.
 *
 * Android로부터 수신한 프레임의 시퀀스 번호가 올바른 순서대로
 * 증가하는지 검증합니다. 0~255 범위에서 순환합니다.
 *
 * @param current_seq 수신한 시퀀스 번호
 * @param expected_seq 예상되는 다음 시퀀스 번호 (0~255)
 * @return 시퀀스 번호가 올바르면 true, 그렇지 않으면 false
 */
static bool validateSequenceNumber(uint8_t current_seq, uint8_t* expected_seq) {
    if (current_seq != *expected_seq) {
        // 프레임 손실 감지: 예상되는 번호와 실제 번호가 다름
        uint8_t lost_frames = (current_seq - *expected_seq) & 0xFF;
        ESP_LOGW(TAG, "Frame loss detected: Expected seq=%u, Got seq=%u, Lost frames=%u",
                 *expected_seq, current_seq, lost_frames);
        
        // 예상 시퀀스를 현재 값으로 업데이트 (다음 프레임 검증용)
        *expected_seq = current_seq;
    }
    
    // 다음 프레임의 예상 시퀀스 번호 계산 (0~255 순환)
    *expected_seq = (current_seq + 1) & 0xFF;
    
    return true;  // 시퀀스 번호는 항상 유효함 (손실 감지만 수행)
}

/**
 * BridgeOne 프레임 유효성 검증.
 *
 * 수신한 프레임의 필드 범위를 검증합니다.
 * - 프레임 크기: 정확히 8바이트
 * - buttons 필드: 0x00~0x07 범위 (마우스 버튼 3개만 지원)
 *
 * @param frame 검증할 프레임 포인터
 * @return 프레임이 유효하면 true, 그렇지 않으면 false
 */
static bool validateBridgeFrame(const bridge_frame_t* frame) {
    // 프레임 크기 검증 (컴파일 타임 검증이지만, 런타임 안전성을 위해 추가)
    if (sizeof(bridge_frame_t) != 8) {
        ESP_LOGE(TAG, "Invalid frame size: expected 8 bytes, got %u bytes",
                 sizeof(bridge_frame_t));
        return false;
    }
    
    // buttons 필드 범위 검증 (마우스 버튼 3개: L, R, M)
    // 0x00: 아무 버튼 안 눌림
    // 0x01: Left 버튼 (Bit 0)
    // 0x02: Right 버튼 (Bit 1)
    // 0x04: Middle 버튼 (Bit 2)
    // 0x03, 0x05, 0x06, 0x07: 조합
    if (frame->buttons > 0x07) {
        ESP_LOGE(TAG, "Invalid buttons value: 0x%02X (expected 0x00~0x07)",
                 frame->buttons);
        return false;
    }
    
    return true;
}

/**
 * UART 수신 태스크.
 *
 * UART에서 8바이트 프레임을 수신하고 검증하여 FreeRTOS 큐로 전송합니다.
 *
 * 동작:
 * 1. uart_read_bytes()로 8바이트 프레임 수신 (100ms 타임아웃)
 * 2. 수신 바이트 수에 따른 오류 처리:
 *    - len < 0: 드라이버 오류 발생
 *    - len == 0: 타임아웃 (정상적인 대기 상태)
 *    - len != 8: 불완전 수신 (프로토콜 오류)
 * 3. validateSequenceNumber()로 순번 검증
 * 4. validateBridgeFrame()으로 필드 범위 검증
 * 5. 검증 성공 시 xQueueSend()로 프레임을 큐에 전송
 * 6. esp_task_wdt_reset()으로 태스크 워치독 리셋 (무한 루프 방지)
 *
 * @param param 미사용
 */
void uart_task(void* param) {
    // 이 태스크를 Task WDT에 등록 (NULL = 현재 태스크)
    esp_task_wdt_add(NULL);
    
    // 예상되는 다음 시퀀스 번호 (0부터 시작)
    static uint8_t expected_seq = 0;
    
    // 수신 버퍼
    bridge_frame_t frame_buffer;
    uint8_t* raw_buffer = (uint8_t*)&frame_buffer;
    
    ESP_LOGI(TAG, "UART task started");
    
    while (1) {
        // UART에서 8바이트 프레임 수신 (100ms 타임아웃)
        int len = uart_read_bytes(
            UART_NUM,
            raw_buffer,
            sizeof(bridge_frame_t),
            pdMS_TO_TICKS(UART_RX_TIMEOUT_MS)
        );
        
        // 수신 바이트 수에 따른 오류 처리
        if (len < 0) {
            // 드라이버 오류: 상태 리셋 필요
            ESP_LOGE(TAG, "UART driver error: %d", len);
            vTaskDelay(pdMS_TO_TICKS(10));
            esp_task_wdt_reset();
            continue;
        } else if (len == 0) {
            // 타임아웃: 정상적인 대기 상태 (로그 불필요)
            esp_task_wdt_reset();
            continue;
        } else if (len != sizeof(bridge_frame_t)) {
            // 불완전 수신: 프로토콜 오류
            ESP_LOGW(TAG, "Incomplete frame received: expected %u bytes, got %d bytes",
                     sizeof(bridge_frame_t), len);
            esp_task_wdt_reset();
            continue;
        }
        
        // 시퀀스 번호 검증 (손실 감지)
        validateSequenceNumber(frame_buffer.seq, &expected_seq);
        
        // 프레임 유효성 검증
        if (!validateBridgeFrame(&frame_buffer)) {
            // 유효성 검증 실패 시 프레임 폐기
            ESP_LOGW(TAG, "Frame validation failed, discarding frame");
            esp_task_wdt_reset();
            continue;
        }
        
        // 검증 성공한 프레임을 큐에 전송
        // - frame_queue: FreeRTOS 큐 핸들
        // - &frame_buffer: 프레임 포인터 (8바이트)
        // - pdMS_TO_TICKS(10): 10ms 타임아웃 (대기하지 않고 즉시 전송 시도)
        BaseType_t queue_status = xQueueSend(
            frame_queue,
            &frame_buffer,
            pdMS_TO_TICKS(10)
        );
        
        if (queue_status != pdPASS) {
            // 큐가 가득 차서 전송 실패 (드물게 발생)
            ESP_LOGW(TAG, "Queue full, frame dropped: seq=%u, buttons=0x%02X",
                     frame_buffer.seq, frame_buffer.buttons);
        } else {
            // 디버그: 수신한 프레임 정보 출력 (DEBUG_FRAME_VERBOSE 매크로 사용)
            #ifdef DEBUG_FRAME_VERBOSE
            ESP_LOGI(TAG, "Frame received and queued: seq=%u, buttons=0x%02X, x=%d, y=%d, "
                     "wheel=%d, modifier=0x%02X, key1=0x%02X, key2=0x%02X",
                     frame_buffer.seq, frame_buffer.buttons,
                     frame_buffer.x, frame_buffer.y, frame_buffer.wheel,
                     frame_buffer.modifier, frame_buffer.keycode1, frame_buffer.keycode2);
            #endif
        }
        
        // 태스크 워치독 리셋 (무한 루프 방지)
        esp_task_wdt_reset();
    }
}