#include <stdio.h>
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "esp_log.h"
#include "tusb.h"
#include "usb_descriptors.h"

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
    
    while (1) {
        // TinyUSB 스택 처리 (호스트로부터의 제어 전송, 데이터 이벤트 처리)
        tud_task();
        
        // 2ms 주기로 반복
        vTaskDelay(pdMS_TO_TICKS(2));
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
    // RHPORT: USB OTG 포트 지정 (ESP32-S3는 RHPORT 0만 지원)
    // 이 함수가 호출되면:
    // - Device Descriptor 콜백: tud_descriptor_device_cb()
    // - Configuration Descriptor 콜백: tud_descriptor_configuration_cb()
    // - HID Report Descriptor 콜백: tud_hid_descriptor_report_cb()
    // - String Descriptor 콜백: tud_descriptor_string_cb()
    // 등이 호스트의 디바이스 열거 요청에 응답
    esp_err_t ret = tud_init(0);  // RHPORT 0: ESP32-S3의 USB-OTG 포트
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "TinyUSB initialization failed: %s", esp_err_to_name(ret));
        return;
    }
    ESP_LOGI(TAG, "TinyUSB device stack initialized (RHPORT=0)");
    
    // ==================== 2. 시스템 정보 로깅 ====================
    ESP_LOGI(TAG, "Hardware: ESP32-S3-DevkitC-1-N16R8");
    ESP_LOGI(TAG, "USB Descriptor: VID=0x%04X, PID=0x%04X", USB_VID, USB_PID);
    ESP_LOGI(TAG, "Interfaces: HID Keyboard(0), HID Mouse(1), CDC(2,3)");
    ESP_LOGI(TAG, "Endpoints: KB=0x%02X, Mouse=0x%02X, CDC_NOTIF=0x%02X, CDC_DATA=0x%02X/0x%02X",
             EPNUM_HID_KB, EPNUM_HID_MOUSE, EPNUM_CDC_NOTIF, EPNUM_CDC_OUT, EPNUM_CDC_IN);
    
    // ==================== 3. FreeRTOS 태스크 생성 ====================
    // USB 태스크: TinyUSB 스택 폴링 담당
    // - 우선순위 5: 일반 우선순위 (높지 않음)
    // - Core 1에서 실행: 멀티코어 활용
    // - 스택 크기 4096 bytes: TinyUSB 콜백 처리에 충분
    BaseType_t usb_task_created = xTaskCreatePinnedToCore(
        usb_task,           // 태스크 함수
        "USB",              // 태스크 이름
        4096,               // 스택 크기 (bytes)
        NULL,               // 매개변수
        5,                  // 우선순위
        NULL,               // 생성된 태스크 핸들 (미사용)
        1                   // Core 1에서 실행
    );
    
    if (usb_task_created != pdPASS) {
        ESP_LOGE(TAG, "Failed to create USB task");
        return;
    }
    ESP_LOGI(TAG, "USB task created (Core 1, Priority 5)");
    
    // ==================== 4. 초기화 완료 ====================
    ESP_LOGI(TAG, "BridgeOne USB Bridge Ready - Waiting for host connection...");
    
    // 메인 태스크는 idle로 반환 (FreeRTOS 스케줄러가 USB 태스크 실행)
}
