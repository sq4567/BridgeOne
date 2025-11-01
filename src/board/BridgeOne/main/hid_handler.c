/**
 * @file hid_handler.c
 * @brief BridgeOne HID 핸들러 구현 - Keyboard/Mouse 리포트 및 콜백 처리
 * 
 * 역할:
 * - TinyUSB HID 콜백 함수 구현
 *   - tud_hid_get_report_cb(): 호스트 요청 시 현재 리포트 상태 반환
 *   - tud_hid_set_report_cb(): 호스트 설정 요청 처리 (LED 상태)
 * - HID 상태 저장소 및 헬퍼 함수 구현
 * - Keyboard/Mouse 인스턴스 구분 처리
 * 
 * 참조:
 * - .cursor/rules/tinyusb-hid-implementation.mdc - HID 구현 패턴
 * - docs/board/esp32s3-code-implementation-guide.md §3.3
 * - usb_descriptors.h - 인터페이스 번호 및 엔드포인트 정의
 */

#include <string.h>
#include "esp_log.h"
#include "tusb.h"
#include "class/hid/hid.h"
#include "hid_handler.h"
#include "usb_descriptors.h"
#include "esp_task_wdt.h"

// ==================== 로깅 설정 ====================
static const char* TAG = "HID_HANDLER";

// ==================== 전역 상태 저장소 ====================

/**
 * @brief 마지막으로 전송된 Keyboard 리포트
 * 
 * 역할:
 * - tud_hid_get_report_cb()에서 호스트 요청 시 반환할 상태
 * - send_keyboard_report() 함수에서 업데이트됨 (Phase 2.1.2)
 * 
 * 초기값: 모두 0 (아무 키도 눌리지 않음)
 */
hid_keyboard_report_t g_last_kb_report = {0};

/**
 * @brief 마지막으로 전송된 Mouse 리포트
 * 
 * 역할:
 * - tud_hid_get_report_cb()에서 호스트 요청 시 반환할 상태
 * - send_mouse_report() 함수에서 업데이트됨 (Phase 2.1.2)
 * 
 * 초기값: 모두 0 (이동/버튼 없음)
 */
hid_mouse_report_t g_last_mouse_report = {0};

/**
 * @brief 키보드 LED 상태 버퍼
 * 
 * 역할:
 * - tud_hid_set_report_cb()에서 호스트로부터 수신한 LED 상태 저장
 * - Num Lock(bit0), Caps Lock(bit1), Scroll Lock(bit2)
 * 
 * 초기값: 0 (모든 LED 꺼짐)
 */
uint8_t g_hid_keyboard_led_status = 0;

// ==================== TinyUSB HID 콜백 함수 ====================

/**
 * @brief HID Get Report 콜백 - 호스트가 현재 리포트 상태를 요청할 때 호출
 * 
 * 호스트가 GET_REPORT 요청을 보낼 때 디바이스의 현재 상태(예: 마지막으로 보낸 키 상태)를
 * 반환합니다. 이는 BIOS/UEFI 부트 시 또는 특정 USB 드라이버에서 상태 동기화 시 필요합니다.
 * 
 * @param instance: HID 인터페이스 번호 (0=Keyboard, 1=Mouse)
 * @param report_id: HID Report ID (1=Keyboard, 2=Mouse)
 * @param report_type: 요청 타입 (INPUT, OUTPUT, FEATURE)
 * @param buffer: 호스트가 수신할 데이터 버퍼 (최대 reqlen 바이트)
 * @param reqlen: 호스트가 요청한 최대 길이
 * 
 * @return: 실제로 복사된 바이트 수 (0=데이터 없음, 또는 오류)
 * 
 * 구현 흐름:
 * 1. report_type이 INPUT인 경우만 처리 (Output/Feature는 0 반환)
 * 2. instance 값으로 Keyboard/Mouse 구분
 * 3. 각 인스턴스에 맞는 last_report를 버퍼에 복사
 * 4. 복사된 바이트 수 반환
 */
uint16_t tud_hid_get_report_cb(uint8_t instance, uint8_t report_id,
                                hid_report_type_t report_type,
                                uint8_t* buffer, uint16_t reqlen) {
    // Input Report 요청만 처리
    // Output/Feature Report는 처리 불필요 (0 반환)
    if (report_type != HID_REPORT_TYPE_INPUT) {
        return 0;
    }

    // Instance 구분: Keyboard (0) 및 Mouse (1)
    if (instance == ITF_NUM_HID_KEYBOARD && report_id == 1) {
        // Keyboard Instance - Boot Protocol 리포트 (8바이트)
        uint16_t len = (reqlen < sizeof(g_last_kb_report)) 
                       ? reqlen 
                       : sizeof(g_last_kb_report);
        memcpy(buffer, &g_last_kb_report, len);
        
        ESP_LOGD(TAG, "GET_REPORT Keyboard: modifier=0x%02x, keycode[0]=0x%02x",
                 g_last_kb_report.modifier, g_last_kb_report.keycode[0]);
        
        return len;
    } 
    else if (instance == ITF_NUM_HID_MOUSE && report_id == 2) {
        // Mouse Instance - Boot Protocol 리포트 (4바이트)
        uint16_t len = (reqlen < sizeof(g_last_mouse_report)) 
                       ? reqlen 
                       : sizeof(g_last_mouse_report);
        memcpy(buffer, &g_last_mouse_report, len);
        
        ESP_LOGD(TAG, "GET_REPORT Mouse: buttons=0x%02x, x=%d, y=%d",
                 g_last_mouse_report.buttons, g_last_mouse_report.x, 
                 g_last_mouse_report.y);
        
        return len;
    }

    // 인식되지 않은 instance/report_id
    ESP_LOGW(TAG, "GET_REPORT: Unknown instance=%d, report_id=%d", 
             instance, report_id);
    return 0;
}

/**
 * @brief HID Set Report 콜백 - 호스트가 디바이스 상태를 설정할 때 호출
 * 
 * 주로 키보드의 LED 상태(Num Lock, Caps Lock, Scroll Lock)를 호스트로부터
 * 수신하는 데 사용됩니다. 이 값을 저장한 후, 실제 GPIO를 제어하여
 * 물리적 LED를 켜고 끌 수 있습니다 (선택적).
 * 
 * @param instance: HID 인터페이스 번호 (0=Keyboard, 1=Mouse)
 * @param report_id: HID Report ID
 * @param report_type: 요청 타입 (INPUT, OUTPUT, FEATURE)
 * @param buffer: 호스트가 전송한 데이터 버퍼
 * @param bufsize: 버퍼 크기
 * 
 * 구현 흐름:
 * 1. Keyboard Output Report만 처리
 * 2. buffer[0]에서 LED 상태 비트마스크 추출
 * 3. LED 상태를 전역 변수에 저장
 * 4. 로깅 출력
 * 5. 나중에 hid_get_keyboard_led_status()로 조회 가능
 */
void tud_hid_set_report_cb(uint8_t instance, uint8_t report_id,
                            hid_report_type_t report_type,
                            uint8_t const* buffer, uint16_t bufsize) {
    (void)report_id;  // report_id 미사용 (Keyboard는 일반적으로 1)

    // Keyboard 인스턴스의 Output Report만 처리
    // Mouse는 Output Report를 사용하지 않으므로 무시
    if (instance == ITF_NUM_HID_KEYBOARD && report_type == HID_REPORT_TYPE_OUTPUT) {
        if (bufsize >= 1) {
            // buffer[0] = LED 상태 비트마스크
            // bit 0: Num Lock
            // bit 1: Caps Lock
            // bit 2: Scroll Lock
            uint8_t leds = buffer[0];
            g_hid_keyboard_led_status = leds;
            
            ESP_LOGI(TAG, "Keyboard LEDs: NumLock=%d, CapsLock=%d, ScrollLock=%d",
                     (leds & KEYBOARD_LED_NUMLOCK) ? 1 : 0,
                     (leds & KEYBOARD_LED_CAPSLOCK) ? 1 : 0,
                     (leds & KEYBOARD_LED_SCROLLLOCK) ? 1 : 0);
            
            // TODO: 실제 GPIO를 제어하여 LED를 켜고 끄는 로직 구현 (Phase 3에서)
            // 예: gpio_set_level(GPIO_KB_LED_NUMLOCK, (leds & KEYBOARD_LED_NUMLOCK) ? 1 : 0);
        }
    }
}

// ==================== HID 헬퍼 함수 ====================

// ==================== HID 리포트 전송 함수 ====================

/**
 * @brief HID Keyboard 리포트 전송
 * 
 * @param report 전송할 키보드 리포트 (8바이트)
 * @return true 전송 성공, false 전송 실패
 * 
 * 동작:
 * 1. tud_hid_n_ready()로 전송 가능 상태 확인
 * 2. 불가능하면 false 반환
 * 3. tud_hid_n_report()로 리포트 전송
 * 4. g_last_kb_report 업데이트 (GET_REPORT 콜백용)
 * 
 * 참고: .cursor/rules/tinyusb-hid-implementation.mdc §1.2 API 사용 패턴 준수
 */
bool sendKeyboardReport(const hid_keyboard_report_t* report) {
    if (report == NULL) {
        ESP_LOGW(TAG, "sendKeyboardReport: report is NULL");
        return false;
    }
    
    uint8_t instance = ITF_NUM_HID_KEYBOARD;
    
    // 1. USB가 마운트되었고, 이전 전송이 완료되었는지 확인
    if (!tud_hid_n_ready(instance)) {
        ESP_LOGW(TAG, "Keyboard not ready (USB disconnected or buffer full)");
        return false;
    }
    
    // 2. 상태 저장 (GET_REPORT 콜백용)
    memcpy(&g_last_kb_report, report, sizeof(hid_keyboard_report_t));
    
    // 3. 리포트 전송
    // Report ID 1: Boot Protocol Keyboard
    if (!tud_hid_n_report(instance, 1, report, sizeof(hid_keyboard_report_t))) {
        ESP_LOGE(TAG, "Failed to send keyboard report");
        return false;
    }
    
    // 디버그 로그: 전송된 키보드 리포트 정보
    ESP_LOGD(TAG, "Keyboard report sent: modifier=0x%02x, keycode[0]=0x%02x, keycode[1]=0x%02x",
             report->modifier, report->keycode[0], report->keycode[1]);
    
    return true;
}

/**
 * @brief HID Mouse 리포트 전송
 * 
 * @param report 전송할 마우스 리포트 (4바이트)
 * @return true 전송 성공, false 전송 실패
 * 
 * 동작:
 * 1. tud_hid_n_ready()로 전송 가능 상태 확인
 * 2. 불가능하면 false 반환
 * 3. tud_hid_n_report()로 리포트 전송
 * 4. g_last_mouse_report 업데이트 (GET_REPORT 콜백용)
 */
bool sendMouseReport(const hid_mouse_report_t* report) {
    if (report == NULL) {
        ESP_LOGW(TAG, "sendMouseReport: report is NULL");
        return false;
    }
    
    uint8_t instance = ITF_NUM_HID_MOUSE;
    
    // 1. USB가 마운트되었고, 이전 전송이 완료되었는지 확인
    if (!tud_hid_n_ready(instance)) {
        ESP_LOGW(TAG, "Mouse not ready (USB disconnected or buffer full)");
        return false;
    }
    
    // 2. 상태 저장 (GET_REPORT 콜백용)
    memcpy(&g_last_mouse_report, report, sizeof(hid_mouse_report_t));
    
    // 3. 리포트 전송
    // Report ID 2: Boot Protocol Mouse
    if (!tud_hid_n_report(instance, 2, report, sizeof(hid_mouse_report_t))) {
        ESP_LOGE(TAG, "Failed to send mouse report");
        return false;
    }
    
    // 디버그 로그: 전송된 마우스 리포트 정보
    ESP_LOGD(TAG, "Mouse report sent: buttons=0x%02x, x=%d, y=%d, wheel=%d",
             report->buttons, report->x, report->y, report->wheel);
    
    return true;
}

// ==================== BridgeFrame 처리 함수 ====================

/**
 * @brief BridgeFrame 처리 및 HID 리포트로 변환
 * 
 * UART에서 수신한 bridge_frame_t를 분석하여 Keyboard 및 Mouse 리포트를
 * 생성하고 호스트에 전송합니다.
 * 
 * 세부 동작:
 * 1. frame->modifier, frame->keycode1/keycode2 추출 → Keyboard 리포트 생성
 * 2. frame->buttons, frame->x, frame->y, frame->wheel 추출 → Mouse 리포트 생성
 * 3. sendKeyboardReport()로 키보드 리포트 전송
 * 4. sendMouseReport()로 마우스 리포트 전송
 * 5. 에러 발생 시 로깅
 * 
 * @param frame UART에서 수신한 검증된 프레임 (bridge_frame_t)
 * 
 * 참고: uart_handler.h bridge_frame_t 정의:
 *  - seq (바이트 0): 시퀀스 번호
 *  - buttons (바이트 1): 마우스 버튼
 *  - x (바이트 2): X축 이동값
 *  - y (바이트 3): Y축 이동값
 *  - wheel (바이트 4): 휠 값
 *  - modifier (바이트 5): 키보드 modifier
 *  - keycode1 (바이트 6): 첫 번째 키코드
 *  - keycode2 (바이트 7): 두 번째 키코드
 */
void processBridgeFrame(const bridge_frame_t* frame) {
    if (frame == NULL) {
        ESP_LOGE(TAG, "processBridgeFrame: frame is NULL");
        return;
    }
    
    // ==================== Keyboard 리포트 생성 및 전송 ====================
    // 조건: modifier가 0이 아니거나, keycode1/keycode2 중 하나라도 0이 아닐 때
    if (frame->modifier != 0 || frame->keycode1 != 0 || frame->keycode2 != 0) {
        // Boot Protocol Keyboard 리포트 구성 (8바이트)
        hid_keyboard_report_t kb_report = {
            .modifier = frame->modifier,    // 바이트 0: Ctrl/Shift/Alt/GUI
            .reserved = 0,                  // 바이트 1: 예약 필드 (0x00)
            .keycode = {                    // 바이트 2-7: 키 코드 배열 (6-Key Rollover)
                frame->keycode1,            // 첫 번째 키 코드
                frame->keycode2,            // 두 번째 키 코드
                0, 0, 0, 0                  // 나머지는 0 (미사용)
            }
        };
        
        if (!sendKeyboardReport(&kb_report)) {
            ESP_LOGW(TAG, "Failed to send keyboard report (seq=%d)", frame->seq);
        }
    }
    
    // ==================== Mouse 리포트 생성 및 전송 ====================
    // 조건: buttons/x/y/wheel 중 하나라도 0이 아닐 때
    if (frame->buttons != 0 || frame->x != 0 || frame->y != 0 || frame->wheel != 0) {
        // Boot Protocol Mouse 리포트 구성 (4바이트)
        hid_mouse_report_t mouse_report = {
            .buttons = frame->buttons,      // 바이트 0: Left/Right/Middle 버튼
            .x = frame->x,                  // 바이트 1: X축 상대 이동 (-127~127)
            .y = frame->y,                  // 바이트 2: Y축 상대 이동 (-127~127)
            .wheel = frame->wheel           // 바이트 3: 휠 스크롤 (-127~127)
        };
        
        if (!sendMouseReport(&mouse_report)) {
            ESP_LOGW(TAG, "Failed to send mouse report (seq=%d)", frame->seq);
        }
    }
    
    // 디버그 로그: 처리된 프레임 정보
    ESP_LOGD(TAG, "Bridge frame processed: seq=%d, kb=[mod=0x%02x, k1=0x%02x, k2=0x%02x], "
             "mouse=[btn=0x%02x, x=%d, y=%d, wheel=%d]",
             frame->seq, frame->modifier, frame->keycode1, frame->keycode2,
             frame->buttons, frame->x, frame->y, frame->wheel);
}

// ==================== HID 태스크 ====================

/**
 * @brief HID 태스크 - BridgeFrame을 HID 리포트로 변환하여 전송
 * 
 * FreeRTOS 태스크로서 다음 동작을 반복 수행합니다:
 * 1. xQueueReceive()로 frame_queue에서 검증된 bridge_frame_t 수신 (100ms 타임아웃)
 * 2. processBridgeFrame()을 호출하여 Keyboard/Mouse 리포트 생성
 * 3. 각 리포트를 USB HID 인터페이스로 전송
 * 4. 다음 프레임을 대기
 * 
 * 타임아웃:
 * - 100ms: UART 프레임 수신 간격보다 충분히 길어서 모든 프레임 처리 가능
 * - 너무 길지 않아서 반응성 유지
 * 
 * 에러 처리:
 * - 큐 수신 실패: 타임아웃 후 재시도 (정상 동작)
 * - USB 미연결: sendKeyboardReport/sendMouseReport에서 처리
 * - 프레임 NULL: processBridgeFrame에서 안전하게 처리
 * 
 * @param param 미사용
 * 
 * 참고:
 * - uart_handler.h: frame_queue, bridge_frame_t 정의
 * - .cursor/rules/tinyusb-freertos-integration.mdc: FreeRTOS + TinyUSB 통합 패턴
 */
void hid_task(void* param) {
    // 이 태스크를 Task WDT에 등록 (NULL = 현재 태스크)
    esp_task_wdt_add(NULL);
    
    (void)param;  // 미사용 파라미터 경고 제거
    
    ESP_LOGI(TAG, "HID task started (waiting for frames from UART queue)");
    
    // Phase 2.1.2.1에서 uart_handler.h에 extern QueueHandle_t frame_queue 선언됨
    // Phase 2.1.2.2에서 app_main()의 "1.6" 섹션에서 xQueueCreate() 호출됨
    // 이 frame_queue에서 검증된 프레임을 수신합니다.
    
    bridge_frame_t frame_buffer;
    
    while (1) {
        // 1. FreeRTOS 큐에서 검증된 프레임 수신
        // - portMAX_DELAY 대신 100ms 타임아웃 사용 (태스크 응답성 유지)
        // - xQueueReceive() 반환: pdTRUE(성공) 또는 pdFALSE(타임아웃)
        BaseType_t result = xQueueReceive(
            frame_queue,                    // uart_task에서 전송한 큐
            &frame_buffer,                  // 수신 버퍼
            pdMS_TO_TICKS(100)              // 100ms 타임아웃
        );
        
        if (result == pdTRUE) {
            // 2. 검증된 프레임 처리: Keyboard/Mouse 리포트 생성 및 전송
            processBridgeFrame(&frame_buffer);
            
            // 워치독 리셋 (무한 루프 방지)
            esp_task_wdt_reset();
            
            // 디버그 로그: 큐에서 수신한 프레임
            ESP_LOGV(TAG, "Frame received from queue: seq=%d, "
                     "buttons=0x%02x, x=%d, y=%d, wheel=%d, "
                     "modifier=0x%02x, keycode1=0x%02x, keycode2=0x%02x",
                     frame_buffer.seq, frame_buffer.buttons,
                     frame_buffer.x, frame_buffer.y, frame_buffer.wheel,
                     frame_buffer.modifier, frame_buffer.keycode1, frame_buffer.keycode2);
        } 
        else {
            // 타임아웃 (정상 상황): 100ms 동안 프레임이 없음
            // 워치독 리셋 (무한 루프 방지)
            esp_task_wdt_reset();
            
            // 다시 대기 (loop 계속)
            ESP_LOGV(TAG, "HID task: queue receive timeout (no frame for 100ms)");
        }
    }
}

/**
 * @brief 현재 키보드 LED 상태 조회
 * 
 * 호스트로부터 설정된 LED 상태(Num Lock, Caps Lock, Scroll Lock)를 반환합니다.
 * 이 값을 바탕으로 실제 GPIO를 제어하여 물리적 LED를 제어할 수 있습니다.
 * 
 * @return: 키보드 LED 상태 비트마스크
 *          - bit 0: Num Lock (1=on, 0=off)
 *          - bit 1: Caps Lock (1=on, 0=off)
 *          - bit 2: Scroll Lock (1=on, 0=off)
 * 
 * 사용 예시:
 * ```c
 * uint8_t leds = hid_get_keyboard_led_status();
 * if (leds & KEYBOARD_LED_CAPSLOCK) {
 *     gpio_set_level(GPIO_CAPSLOCK_LED, 1);  // LED 켜기
 * }
 * ```
 */
uint8_t hid_get_keyboard_led_status(void) {
    return g_hid_keyboard_led_status;
}
