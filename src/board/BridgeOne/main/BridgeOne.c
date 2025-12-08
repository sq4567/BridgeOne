#include <stdio.h>
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "esp_log.h"
#include "driver/uart.h"  // uart_wait_tx_done() 함수 사용
#include "tinyusb.h"  // esp_tinyusb wrapper 헤더
#include "tusb.h"     // TinyUSB core 헤더 (tud_task 등)
#include "usb_descriptors.h"
#include "uart_handler.h"
#include "hid_handler.h"  // hid_task 함수 선언
#include "hid_test.h"     // HID 테스트 모드
#include "voltage_monitor.h"  // 전압 모니터링 모드
#include "usb_cdc_log.h"  // USB CDC 디버그 로깅
#include "esp_task_wdt.h"

// ==================== 테스트 모드 설정 ====================
/**
 * HID_TEST_MODE 활성화 방법:
 * 1. 아래 주석을 해제: #define HID_TEST_MODE
 * 2. 빌드 및 플래시
 * 3. PC USB에 연결하면 자동으로 테스트 시작
 *
 * 테스트 내용 (1회 실행):
 * - 마우스 원형 이동
 * - 키보드 "HELLO" 타이핑
 */
// #define HID_TEST_MODE

/**
 * VOLTAGE_MONITOR_MODE 활성화 방법:
 * 1. 아래 주석을 해제: #define VOLTAGE_MONITOR_MODE
 * 2. 빌드 및 플래시
 * 3. 시리얼 터미널로 로그 확인 (115200bps)
 *
 * 모니터링 항목:
 * - 칩 내부 온도 (전원 불안정 시 급변 감지)
 * - ADC 노이즈 레벨 (전원 노이즈 확인)
 * - 시스템 가동시간 (리셋/크래시 감지)
 *
 * 주의: HID_TEST_MODE와 동시에 활성화하지 마세요.
 */
// #define VOLTAGE_MONITOR_MODE

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

    esp_err_t ret;

    // ==================== 1. TinyUSB 디바이스 스택 초기화 ====================
    // esp_tinyusb wrapper를 사용한 초기화
    // 이 방식의 장점:
    // - USB PHY를 자동으로 USB OTG 컨트롤러로 전환
    // - USB Serial/JTAG와의 충돌 방지
    // - ESP-IDF 시스템과 완벽한 통합
    //
    // tinyusb_driver_install()가 호출되면:
    // - USB PHY가 USB Serial/JTAG에서 USB OTG로 전환됨
    // - Device Descriptor 콜백: tud_descriptor_device_cb()
    // - Configuration Descriptor 콜백: tud_descriptor_configuration_cb()
    // - HID Report Descriptor 콜백: tud_hid_descriptor_report_cb()
    // - String Descriptor 콜백: tud_descriptor_string_cb()
    // 등이 호스트의 디바이스 열거 요청에 응답

    const tinyusb_config_t tusb_cfg = {
        .device_descriptor = &desc_device,          // Custom device descriptor
        .string_descriptor = string_desc_arr,       // Custom string descriptors
        .external_phy = false,                      // Use internal USB PHY
        .configuration_descriptor = desc_configuration, // Custom configuration
    };

    ret = tinyusb_driver_install(&tusb_cfg);
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "TinyUSB driver installation failed: %s", esp_err_to_name(ret));
        return;
    }
    ESP_LOGI(TAG, "TinyUSB driver installed (USB PHY switched to USB OTG)");

    // UART0 TX 버퍼 플러시 대기 (CDC 전환 전 로그 손실 방지)
    // uart_wait_tx_done() 대신 간단한 딜레이 사용 (UART 드라이버 초기화 전이므로 안전)
    vTaskDelay(pdMS_TO_TICKS(50));

    // ==================== 1.2. USB CDC 디버그 로깅 초기화 ====================
#if !defined(HID_TEST_MODE) && !defined(VOLTAGE_MONITOR_MODE)
    // 정상 모드: ESP_LOG 출력을 Native USB OTG의 CDC 인터페이스로 리다이렉트합니다.
    // 이를 통해 UART0 (CH343P)를 Android 통신에 사용하면서도
    // 포트 2️⃣ (Micro-USB)를 통해 PC에서 디버그 로그를 확인할 수 있습니다.
    //
    // 물리적 연결:
    // - 포트 1️⃣ (USB-C, CH343P) → Android 스마트폰 (UART 통신)
    // - 포트 2️⃣ (Micro-USB, Native USB OTG) → PC (디버그 로그 + HID)
    if (usb_cdc_log_init()) {
        ESP_LOGI(TAG, "Debug logs redirected to USB CDC (Port 2, Micro-USB)");
    } else {
        ESP_LOGW(TAG, "USB CDC logging init failed, using UART output");
    }
#elif defined(HID_TEST_MODE)
    // HID 테스트 모드: CDC 리다이렉션 비활성화
    // 모든 로그를 UART0 (포트 1️⃣, COM8)로 출력하여 단일 포트로 간편하게 모니터링
    ESP_LOGI(TAG, "HID_TEST_MODE: All logs remain on UART0 (Port 1, COM8)");
#elif defined(VOLTAGE_MONITOR_MODE)
    // 전압 모니터링 모드: CDC 리다이렉션 비활성화
    // 모든 로그를 UART0 (포트 1️⃣)로 출력
    ESP_LOGI(TAG, "VOLTAGE_MONITOR_MODE: All logs remain on UART0");
#endif

    // ==================== 1.5. UART 통신 초기화 ====================
#if !defined(HID_TEST_MODE) && !defined(VOLTAGE_MONITOR_MODE)
    // 정상 모드: Android와의 UART 통신을 위해 UART 드라이버 초기화
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
#elif defined(HID_TEST_MODE)
    // HID 테스트 모드: UART 초기화 및 큐 생성 건너뛰기
    ESP_LOGI(TAG, "HID_TEST_MODE enabled - UART and frame queue skipped");
#elif defined(VOLTAGE_MONITOR_MODE)
    // 전압 모니터링 모드: UART 초기화 및 큐 생성 건너뛰기
    ESP_LOGI(TAG, "VOLTAGE_MONITOR_MODE enabled - UART and frame queue skipped");
#endif
    
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

#if defined(HID_TEST_MODE)
    // ==================== 테스트 모드: HID 테스트 태스크 생성 ====================
    ESP_LOGI(TAG, "Creating HID Test Task (TEST MODE)...");

    // HID 테스트 태스크: Android 없이 자체 테스트 데이터 생성
    // - 우선순위 5: USB 태스크(4)보다 높음
    // - Core 0에서 실행
    // - 스택 크기 4096 bytes: 테스트 로직에 충분
    static hid_test_type_t test_type = HID_TEST_ALL;  // 모든 테스트 순차 실행

    BaseType_t test_task_created = xTaskCreatePinnedToCore(
        hid_test_task,      // 태스크 함수
        "HID_TEST",         // 태스크 이름
        4096,               // 스택 크기 (bytes)
        &test_type,         // 테스트 타입 전달
        5,                  // 우선순위
        &hid_task_handle,   // 태스크 핸들 저장
        0                   // Core 0에서 실행
    );

    if (test_task_created != pdPASS) {
        ESP_LOGE(TAG, "Failed to create HID test task");
        return;
    }
    ESP_LOGI(TAG, "HID test task created (Core 0, Priority 5)");
    ESP_LOGI(TAG, "Test mode: Mouse circle + Keyboard 'HELLO' (1 cycle)");

#elif defined(VOLTAGE_MONITOR_MODE)
    // ==================== 전압 모니터링 모드: 모니터링 태스크 생성 ====================
    ESP_LOGI(TAG, "Creating Voltage Monitor Task (MONITOR MODE)...");

    // 전압 모니터링 태스크: USB 포트 전원 안정성 테스트
    // - 우선순위 5: USB 태스크(4)보다 높음
    // - Core 0에서 실행
    // - 스택 크기 4096 bytes: ADC + 온도 센서 처리에 충분
    TaskHandle_t vmon_task_handle = NULL;

    BaseType_t vmon_task_created = xTaskCreatePinnedToCore(
        voltage_monitor_task,   // 태스크 함수
        "VMON",                 // 태스크 이름
        4096,                   // 스택 크기 (bytes)
        NULL,                   // 매개변수 없음
        5,                      // 우선순위
        &vmon_task_handle,      // 태스크 핸들 저장
        0                       // Core 0에서 실행
    );

    if (vmon_task_created != pdPASS) {
        ESP_LOGE(TAG, "Failed to create voltage monitor task");
        return;
    }
    ESP_LOGI(TAG, "Voltage monitor task created (Core 0, Priority 5)");

#else
    // ==================== 정상 모드: UART + HID 태스크 생성 ====================

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
#endif

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
#if defined(HID_TEST_MODE)
    ESP_LOGI(TAG, "========================================");
    ESP_LOGI(TAG, "BridgeOne HID Test Mode Ready!");
    ESP_LOGI(TAG, "========================================");
    ESP_LOGI(TAG, "Connect to PC via USB and observe:");
    ESP_LOGI(TAG, "  1. Mouse cursor moving in circle");
    ESP_LOGI(TAG, "  2. Keyboard typing 'HELLO'");
    ESP_LOGI(TAG, "  (Tests run once and stop)");
    ESP_LOGI(TAG, "========================================");
#elif defined(VOLTAGE_MONITOR_MODE)
    ESP_LOGI(TAG, "========================================");
    ESP_LOGI(TAG, "BridgeOne Voltage Monitor Mode Ready!");
    ESP_LOGI(TAG, "========================================");
    ESP_LOGI(TAG, "Monitoring USB port power stability...");
    ESP_LOGI(TAG, "Check serial terminal for readings.");
    ESP_LOGI(TAG, "========================================");
#else
    ESP_LOGI(TAG, "BridgeOne USB Bridge Ready - Waiting for Android connection...");
#endif

    // 메인 태스크는 idle로 반환 (FreeRTOS 스케줄러가 USB 태스크 실행)
}
