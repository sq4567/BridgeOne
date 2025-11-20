#include <stdio.h>
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "esp_log.h"
#include "tusb.h"
#include "usb_descriptors.h"
#include "uart_handler.h"
#include "hid_handler.h"  // hid_task 함수 선언
#include "esp_task_wdt.h"

static const char* TAG = "BridgeOne";

/**
 * @brief USB 디바이스 스택 태스크
 * 
 * TinyUSB 스택의 주기적 처리를 담당하는 FreeRTOS 태스크
 * - tud_task(): TinyUSB 이벤트 폴링 및 처리 (비블로킹)
 * - 2ms 주기로 반복 실행하여 USB 이벤트를 신속하게 처리
 * 
 * 참고: esp32s3-code-implementation-guide.md §3.3.4 usb_task 구현
 * 
 * @param param 미사용
 */
void usb_task(void* param) {
    ESP_LOGI(TAG, "USB task started");
    
    // USB 태스크 루프 카운터 (디버깅용)
    uint32_t loop_count = 0;
    
    while (1) {
        loop_count++;
        
        // 100회 루프마다 디버깅 로그 출력
        if (loop_count % 100 == 0) {
            ESP_LOGI(TAG, "USB task running (loop_count=%u)", loop_count);
        }
        
        // TinyUSB 스택 처리 (호스트로부터의 제어 전송, 데이터 이벤트 처리)
        tud_task();
        
        // 워치독 리셋 (무한 루프 방지)
        esp_task_wdt_reset();
        
        // 1ms 주기로 반복 (가속화: 2ms → 1ms)
        vTaskDelay(pdMS_TO_TICKS(1));
    }
}

/**
 * 애플리케이션 메인 함수 (ESP-IDF app_main)
 * 
 * 목적:
 * - TinyUSB 디바이스 스택 초기화
 * - USB Composite 디바이스 설정 (HID Keyboard + Mouse + CDC)
 * - FreeRTOS 태스크 생성
 * 
 * 진행 순서:
 * 1. TinyUSB 디바이스 스택 초기화
 * 2. 환경 정보 로깅
 * 3. 필수 FreeRTOS 태스크 생성 (USB 처리 태스크)
 * 
 * 참고:
 * - ESP-IDF 및 TinyUSB 초기화는 이 함수에서만 수행됨
 * - USB 이벤트는 tud_task()에서 폴링 방식으로 처리됨
 */
void app_main(void) {
    ESP_LOGI(TAG, "BridgeOne Board - USB Composite Device Initialization");
    
    // ==================== 1. TinyUSB 디바이스 스택 초기화 ====================
    // tusb_init(): TinyUSB 전체 초기화 (RHPORT 0 자동 설정)
    // 이 함수가 호출되면:
    // - Device Descriptor 콜백: tud_descriptor_device_cb()
    // - Configuration Descriptor 콜백: tud_descriptor_configuration_cb()
    // - HID Report Descriptor 콜백: tud_hid_descriptor_report_cb()
    // - String Descriptor 콜백: tud_descriptor_string_cb()
    // 등이 호스트의 디바이스 열거 요청에 응답
    esp_err_t ret = tusb_init();
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "TinyUSB initialization failed: %s", esp_err_to_name(ret));
        return;
    }
    ESP_LOGI(TAG, "TinyUSB device stack initialized");
    
    // ==================== 1.5. UART 통신 초기화 ====================
    // Android와의 UART 통신을 위해 UART 드라이버 초기화
    // 보드별 구성:
    // - ESP32-S3-DevkitC-1: UART0 (GPIO43/44, CP2102N 연결)
    // - YD-ESP32-S3 N16R8: UART1 (GPIO17/18, Android 통신 전용)
    // - 1Mbps, 8N1: 고속 시리얼 통신
    ret = uart_init();
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "UART initialization failed: %s", esp_err_to_name(ret));
        return;
    }
    ESP_LOGI(TAG, "UART initialized (1Mbps, 8N1)");
    
    // ==================== 1.6. FreeRTOS 큐 생성 ====================
    // UART 수신 태스크가 검증된 프레임을 이 큐에 전송합니다.
    // - 큐 크기: UART_FRAME_QUEUE_SIZE (최대 10개 프레임 보관)
    // - 각 아이템 크기: sizeof(bridge_frame_t) = 8 바이트
    #define UART_FRAME_QUEUE_SIZE 10
    frame_queue = xQueueCreate(UART_FRAME_QUEUE_SIZE, sizeof(bridge_frame_t));
    if (frame_queue == NULL) {
        ESP_LOGE(TAG, "Failed to create frame queue");
        return;
    }
    ESP_LOGI(TAG, "Frame queue created (size=%d, item_size=%u bytes)",
             UART_FRAME_QUEUE_SIZE, sizeof(bridge_frame_t));
    
    // ==================== 2. 시스템 정보 로깅 ====================
    ESP_LOGI(TAG, "Hardware: ESP32-S3 N16R8 (DevkitC-1 or YD-ESP32-S3 compatible)");
    ESP_LOGI(TAG, "USB Descriptor: VID=0x%04X, PID=0x%04X", USB_VID, USB_PID);
    ESP_LOGI(TAG, "Interfaces: HID Keyboard(0), HID Mouse(1), CDC(2,3)");
    ESP_LOGI(TAG, "Endpoints: KB=0x%02X, Mouse=0x%02X, CDC_NOTIF=0x%02X, CDC_DATA=0x%02X/0x%02X",
             EPNUM_HID_KB, EPNUM_HID_MOUSE, EPNUM_CDC_NOTIF, EPNUM_CDC_OUT, EPNUM_CDC_IN);
    
    // ==================== 3. FreeRTOS 태스크 생성 ====================
    // 태스크 핸들 선언 (워치독 등록용)
    TaskHandle_t uart_task_handle = NULL;
    TaskHandle_t hid_task_handle = NULL;
    TaskHandle_t usb_task_handle = NULL;
    
    // UART 수신 태스크: Android로부터 마우스/키보드 입력 수신
    // - 우선순위 6: USB 태스크(5)보다 높음 (실시간 통신 우선)
    // - Core 0에서 실행: 통신 처리 전담
    // - 스택 크기 3072 bytes: UART 수신 처리에 충분
    BaseType_t uart_task_created = xTaskCreatePinnedToCore(
        uart_task,          // 태스크 함수
        "UART",             // 태스크 이름
        3072,               // 스택 크기 (bytes)
        NULL,               // 매개변수
        6,                  // 우선순위 (USB 태스크보다 높음)
        &uart_task_handle,  // 태스크 핸들 저장
        0                   // Core 0에서 실행
    );
    
    if (uart_task_created != pdPASS) {
        ESP_LOGE(TAG, "Failed to create UART task");
        return;
    }
    ESP_LOGI(TAG, "UART task created (Core 0, Priority 6)");
    
    // HID 태스크: UART 큐에서 프레임 수신하여 HID 리포트로 변환 및 전송
    // - 우선순위 5: UART 태스크(6)보다는 낮고, USB 태스크(4)보다는 높음 (데이터 흐름 순서)
    // - Core 0에서 실행: UART와 함께 Core 0에서 집중 처리
    // - 스택 크기 3072 bytes: HID 리포트 생성 처리에 충분
    BaseType_t hid_task_created = xTaskCreatePinnedToCore(
        hid_task,           // 태스크 함수
        "HID",              // 태스크 이름
        3072,               // 스택 크기 (bytes)
        NULL,               // 매개변수
        5,                  // 우선순위 (UART보다는 낮음, USB보다는 높음)
        &hid_task_handle,   // 태스크 핸들 저장
        0                   // Core 0에서 실행
    );
    
    if (hid_task_created != pdPASS) {
        ESP_LOGE(TAG, "Failed to create HID task");
        return;
    }
    ESP_LOGI(TAG, "HID task created (Core 0, Priority 5)");

    // USB 태스크: TinyUSB 스택 폴링 담당
    // - 우선순위 4: 낮은 우선순위 (데이터 처리 후 최종 전송)
    // - Core 1에서 실행: 멀티코어 활용
    // - 스택 크기 4096 bytes: TinyUSB 콜백 처리에 충분
    BaseType_t usb_task_created = xTaskCreatePinnedToCore(
        usb_task,           // 태스크 함수
        "USB",              // 태스크 이름
        4096,               // 스택 크기 (bytes)
        NULL,               // 매개변수
        4,                  // 우선순위
        &usb_task_handle,   // 태스크 핸들 저장
        1                   // Core 1에서 실행
    );
    
    if (usb_task_created != pdPASS) {
        ESP_LOGE(TAG, "Failed to create USB task");
        return;
    }
    ESP_LOGI(TAG, "USB task created (Core 1, Priority 4)");
    
    // ==================== 4. 초기화 완료 ====================
    ESP_LOGI(TAG, "BridgeOne USB Bridge Ready - Waiting for host connection...");
    
    // 메인 태스크는 idle로 반환 (FreeRTOS 스케줄러가 USB 태스크 실행)
}
