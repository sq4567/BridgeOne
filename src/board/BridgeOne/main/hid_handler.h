/**
 * @file hid_handler.h
 * @brief BridgeOne HID 핸들러 - Keyboard/Mouse 리포트 전송 및 콜백 처리
 * 
 * 역할:
 * - HID Keyboard/Mouse 리포트 구조체 정의
 * - TinyUSB 콜백 함수 선언
 * - HID 상태 관리 헬퍼 함수 선언
 * - LED 상태 버퍼 관리
 * 
 * 참조: 
 * - .cursor/rules/tinyusb-hid-implementation.mdc
 * - docs/board/esp32s3-code-implementation-guide.md §3.3
 */

#ifndef HID_HANDLER_H
#define HID_HANDLER_H

#include <stdint.h>
#include <stdbool.h>
#include "tusb.h"
#include "class/hid/hid.h"  // HID_REPORT_TYPE_* 매크로 및 리포트 구조체 사용 필수
#include "uart_handler.h"  // bridge_frame_t 정의 및 frame_queue 사용 필수

// ==================== HID 리포트 구조체 (TinyUSB에서 제공) ====================

/**
 * @brief Boot Protocol Keyboard Report (8바이트)
 * 
 * TinyUSB 헤더 (class/hid/hid.h)에서 정의됨:
 * - hid_keyboard_report_t
 * 
 * 구조:
 * - modifier: 1바이트 (Ctrl, Shift, Alt, GUI 비트마스크)
 * - reserved: 1바이트 (0x00으로 고정)
 * - keycode: 6바이트 (동시 입력 가능한 키 코드, 6-Key Rollover)
 * 
 * @note 이 구조체는 hid.h에서 이미 정의되어 있으므로, 여기서는 참고만 제공합니다.
 */
// typedef struct __attribute__((packed)) {
//     uint8_t modifier;
//     uint8_t reserved;
//     uint8_t keycode[6];
// } hid_keyboard_report_t;  // hid.h에서 정의됨

/**
 * @brief Boot Protocol Mouse Report (4바이트)
 * 
 * TinyUSB 헤더 (class/hid/hid.h)에서 정의됨:
 * - hid_mouse_report_t
 * 
 * 구조:
 * - buttons: 1바이트 (bit0=Left, bit1=Right, bit2=Middle)
 * - x: 1바이트 signed (X축 상대 이동량, -127 ~ 127)
 * - y: 1바이트 signed (Y축 상대 이동량, -127 ~ 127)
 * - wheel: 1바이트 signed (휠 스크롤량, -127 ~ 127)
 * 
 * @note 이 구조체는 hid.h에서 이미 정의되어 있으므로, 여기서는 참고만 제공합니다.
 */
// typedef struct __attribute__((packed)) {
//     uint8_t buttons;
//     int8_t  x;
//     int8_t  y;
//     int8_t  wheel;
// } hid_mouse_report_t;  // hid.h에서 정의됨

// ==================== Keyboard LED 상태 정의 ====================

/**
 * @brief 키보드 LED 상태 비트마스크
 * 
 * SET_REPORT 콜백에서 호스트로부터 수신되는 LED 상태
 */
#define KEYBOARD_LED_NUMLOCK    0x01    // Num Lock
#define KEYBOARD_LED_CAPSLOCK   0x02    // Caps Lock
#define KEYBOARD_LED_SCROLLLOCK 0x04    // Scroll Lock

// ==================== HID 콜백 함수 선언 ====================
// (usb_descriptors.c에서 구현되었지만, hid_handler.c에서 재정의될 수 있음)

/**
 * @brief HID Get Report 콜백 (선택적)
 * 
 * 호스트가 GET_REPORT 요청을 보낼 때 호출됨
 * 현재 HID 리포트 상태를 반환
 */
uint16_t tud_hid_get_report_cb(uint8_t instance, uint8_t report_id,
                                hid_report_type_t report_type,
                                uint8_t* buffer, uint16_t reqlen);

/**
 * @brief HID Set Report 콜백
 * 
 * 호스트가 SET_REPORT 요청을 보낼 때 호출됨 (예: 키보드 LED 상태)
 */
void tud_hid_set_report_cb(uint8_t instance, uint8_t report_id,
                            hid_report_type_t report_type,
                            uint8_t const* buffer, uint16_t bufsize);

// ==================== HID 헬퍼 함수 선언 ====================

/**
 * @brief HID 태스크 - BridgeFrame을 HID 리포트로 변환하여 전송
 * 
 * FreeRTOS 큐에서 검증된 프레임을 수신하고, 이를 HID Keyboard/Mouse 리포트로
 * 변환하여 Windows 호스트에 전송합니다.
 * 
 * 동작:
 * 1. xQueueReceive()로 frame_queue에서 bridge_frame_t 수신
 * 2. processBridgeFrame()을 호출하여 리포트 생성
 * 3. sendKeyboardReport() 및 sendMouseReport()로 호스트 전송
 * 4. 100ms 타임아웃 후 다시 대기
 * 
 * @param param 미사용
 * 
 * @note Phase 2.1.2.3에서 구현됨
 */
void hid_task(void* param);

/**
 * @brief BridgeFrame 처리 및 HID 리포트 변환
 * 
 * UART에서 수신한 bridge_frame_t를 분석하고 Keyboard/Mouse 리포트를 생성
 * 하여 호스트에 전송합니다.
 * 
 * 세부 동작:
 * 1. frame->modifiers/keycode[0]/keycode[1] → Keyboard 리포트
 * 2. frame->buttons/x/y/wheel → Mouse 리포트
 * 3. 각 리포트는 별도의 USB 엔드포인트로 전송
 * 
 * @param frame UART에서 수신한 검증된 프레임
 * 
 * @note Phase 2.1.2.3에서 구현됨
 */
void processBridgeFrame(const bridge_frame_t* frame);

/**
 * @brief HID Keyboard 리포트 전송
 * 
 * 준비된 keyboard 리포트를 USB HID Keyboard 인터페이스(ITF_NUM_HID_KEYBOARD)로
 * 전송합니다. 이전 전송 완료 여부를 확인한 후 새 데이터를 전송합니다.
 * 
 * @param report 전송할 키보드 리포트 (8바이트)
 * @return true 전송 성공, false 전송 실패 (USB 미연결 등)
 * 
 * @note Phase 2.1.2.3에서 구현됨
 */
bool sendKeyboardReport(const hid_keyboard_report_t* report);

/**
 * @brief HID Mouse 리포트 전송
 * 
 * 준비된 mouse 리포트를 USB HID Mouse 인터페이스(ITF_NUM_HID_MOUSE)로
 * 전송합니다. 이전 전송 완료 여부를 확인한 후 새 데이터를 전송합니다.
 * 
 * @param report 전송할 마우스 리포트 (4바이트)
 * @return true 전송 성공, false 전송 실패 (USB 미연결 등)
 * 
 * @note Phase 2.1.2.3에서 구현됨
 */
bool sendMouseReport(const hid_mouse_report_t* report);

/**
 * @brief 현재 키보드 LED 상태 조회
 *
 * 호스트로부터 설정된 LED 상태(Num Lock, Caps Lock, Scroll Lock)를 반환
 * 실제 GPIO 제어에 필요
 *
 * @return 키보드 LED 상태 비트마스크
 */
uint8_t hid_get_keyboard_led_status(void);

/**
 * @brief HID 리포트 대기 큐 초기화
 *
 * 키보드와 마우스 리포트 대기 큐를 생성합니다.
 * USB HID가 busy 상태일 때 리포트를 임시 저장하고,
 * ready 상태가 되면 재전송합니다.
 *
 * 목적:
 * - 키 해제 리포트 누락 방지 (키 stuck 문제 해결)
 * - 마우스 버튼 해제 리포트 누락 방지 (드래그 stuck 문제 해결)
 *
 * @note app_main()에서 HID 태스크 생성 전에 호출해야 합니다.
 */
void hid_init_queues(void);

// ==================== HID 상태 저장소 ====================

/**
 * @brief 마지막으로 전송된 Keyboard 리포트
 * 
 * tud_hid_get_report_cb()에서 반환될 상태 저장
 */
extern hid_keyboard_report_t g_last_kb_report;

/**
 * @brief 마지막으로 전송된 Mouse 리포트
 * 
 * tud_hid_get_report_cb()에서 반환될 상태 저장
 */
extern hid_mouse_report_t g_last_mouse_report;

/**
 * @brief 키보드 LED 상태 버퍼
 * 
 * 호스트로부터 설정된 LED 상태를 저장
 * SET_REPORT 콜백에서 업데이트됨
 */
extern uint8_t g_hid_keyboard_led_status;

#endif // HID_HANDLER_H
