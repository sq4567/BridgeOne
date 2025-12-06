/**
 * @file hid_test.h
 * @brief BridgeOne HID 테스트 모드 - Android 없이 자체 테스트 데이터 생성
 *
 * 역할:
 * - Android 통신 없이 ESP32-S3 보드 자체에서 HID 프레임 생성
 * - PC에서 마우스/키보드 제어가 정상 작동하는지 검증
 * - UART 통신 대신 내부적으로 테스트 데이터 생성
 *
 * 테스트 항목:
 * - 마우스 원형 이동
 * - 키보드 타이핑 ("HELLO")
 * - 마우스 클릭
 * - 키 조합 (Ctrl+C)
 */

#ifndef HID_TEST_H
#define HID_TEST_H

#include <stdint.h>
#include <stdbool.h>

// ==================== 테스트 모드 설정 ====================

/**
 * @brief 테스트 모드 활성화 여부
 *
 * true: 테스트 태스크 실행 (Android 통신 비활성화)
 * false: 정상 모드 (UART 통신 활성화)
 */
extern bool g_hid_test_mode;

/**
 * @brief 테스트 타입 열거형
 */
typedef enum {
    HID_TEST_MOUSE_CIRCLE,      // 마우스 원형 이동
    HID_TEST_KEYBOARD_HELLO,    // "HELLO" 타이핑
    HID_TEST_MOUSE_CLICK,       // 마우스 클릭
    HID_TEST_KEY_COMBO,         // 키 조합 (Ctrl+C)
    HID_TEST_ALL                // 모든 테스트 순차 실행
} hid_test_type_t;

// ==================== 테스트 태스크 ====================

/**
 * @brief HID 테스트 태스크
 *
 * UART 통신 대신 내부적으로 테스트 데이터를 생성하여 HID 리포트로 전송합니다.
 * 이를 통해 Android 없이도 PC에서 마우스/키보드 제어가 정상 작동하는지 확인할 수 있습니다.
 *
 * 동작:
 * 1. 테스트 타입에 따라 다른 패턴의 HID 데이터 생성
 * 2. sendKeyboardReport() 및 sendMouseReport()로 전송
 * 3. 일정 간격으로 반복 실행
 *
 * @param param 테스트 타입 (hid_test_type_t*)
 */
void hid_test_task(void* param);

/**
 * @brief 마우스 원형 이동 테스트
 *
 * 마우스 커서가 원형으로 이동하는 HID 리포트를 생성합니다.
 * - 반지름: 50 픽셀
 * - 속도: 초당 1회전
 * - PC 화면에서 원형 궤적을 그리는 것으로 확인 가능
 */
void test_mouse_circle_movement(void);

/**
 * @brief "HELLO" 키보드 타이핑 테스트
 *
 * "HELLO" 문자열을 PC에 타이핑하는 HID 리포트를 생성합니다.
 * - 각 키는 100ms 간격으로 전송
 * - 키 누름 → 키 떼기 순서 보장
 * - PC 메모장 등에서 "HELLO" 입력 확인 가능
 */
void test_keyboard_typing_hello(void);

/**
 * @brief 마우스 클릭 테스트
 *
 * 마우스 좌클릭을 수행하는 HID 리포트를 생성합니다.
 * - 클릭 누름 → 클릭 떼기 순서 보장
 * - 50ms 간격
 */
void test_mouse_click(void);

/**
 * @brief 키 조합 테스트 (Ctrl+C)
 *
 * Ctrl+C 키 조합을 전송하는 HID 리포트를 생성합니다.
 * - Ctrl 누름 → C 누름 → C 떼기 → Ctrl 떼기
 * - PC에서 복사 기능 동작 확인 가능
 */
void test_key_combination_ctrl_c(void);

#endif // HID_TEST_H
