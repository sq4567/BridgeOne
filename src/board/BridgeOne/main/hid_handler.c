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

/**
 * @brief HID 리포트 상태 업데이트 (스켈레톤)
 * 
 * UART에서 수신한 데이터를 기반으로 Keyboard/Mouse 리포트 상태를 업데이트합니다.
 * 이 함수는 Phase 2.1.2에서 UART 통신 로직이 완성된 후 구현될 예정입니다.
 * 
 * @param frame_data: UART로부터 수신한 8바이트 프레임 데이터
 *                   - [0] seq: 시퀀스 번호
 *                   - [1] buttons: 마우스 버튼 상태
 *                   - [2] x: X축 이동값 (signed)
 *                   - [3] y: Y축 이동값 (signed)
 *                   - [4] wheel: 휠 스크롤값 (signed)
 *                   - [5] modifier: 키보드 modifier 키
 *                   - [6] keycode1: 첫 번째 키코드
 *                   - [7] keycode2: 두 번째 키코드
 * 
 * 예상 동작:
 * 1. frame_data 파싱
 * 2. g_last_kb_report 업데이트 (modifier, keycode)
 * 3. g_last_mouse_report 업데이트 (buttons, x, y, wheel)
 * 4. tud_hid_n_report()를 통해 호스트로 전송 (Phase 2.1.2)
 * 
 * @note 이 함수는 Phase 2.1.2에서 구현됩니다. 현재는 스켈레톤만 제공.
 */
void hid_update_report_state(uint8_t* frame_data) {
    if (frame_data == NULL) {
        ESP_LOGW(TAG, "hid_update_report_state: frame_data is NULL");
        return;
    }

    // Phase 2.1.2에서 구현될 예정
    // 1. Keyboard 리포트 업데이트
    // 2. Mouse 리포트 업데이트
    // 3. tud_hid_n_report()를 통해 전송
    
    ESP_LOGV(TAG, "hid_update_report_state: frame_data[0]=0x%02x (seq=%d)",
             frame_data[0], frame_data[0]);
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
