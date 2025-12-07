/**
 * @file hid_test.c
 * @brief BridgeOne HID 테스트 모드 구현
 *
 * Android 없이 ESP32-S3 보드 자체에서 테스트 HID 데이터를 생성하여
 * PC에서 마우스/키보드 제어가 정상 작동하는지 검증합니다.
 */

#include <string.h>
#include <math.h>
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "esp_log.h"
#include "esp_task_wdt.h"
#include "hid_test.h"
#include "hid_handler.h"
#include "class/hid/hid.h"

// ==================== 로깅 설정 ====================
static const char* TAG = "HID_TEST";

// ==================== 테스트 모드 전역 변수 ====================
bool g_hid_test_mode = false;

// ==================== 테스트 함수 구현 ====================

/**
 * @brief 마우스 원형 이동 테스트
 *
 * 원형 궤적을 그리는 마우스 이동 HID 리포트를 생성합니다.
 * - 반지름: 30 픽셀
 * - 각도: 0도부터 360도까지 10도씩 증가
 * - 각 단계마다 50ms 대기
 */
void test_mouse_circle_movement(void) {
    ESP_LOGI(TAG, "Starting mouse circle movement test...");

    const int radius = 30;          // 원 반지름 (픽셀)
    const int step_degrees = 10;    // 각도 증가량 (도)
    const int delay_ms = 50;        // 각 단계 간격 (ms)

    for (int angle = 0; angle < 360; angle += step_degrees) {
        // 라디안 변환
        float rad = angle * M_PI / 180.0f;

        // 원형 좌표 계산
        int8_t dx = (int8_t)(radius * cosf(rad));
        int8_t dy = (int8_t)(radius * sinf(rad));

        // 마우스 리포트 생성
        hid_mouse_report_t mouse_report = {
            .buttons = 0,       // 버튼 누르지 않음
            .x = dx,            // X축 이동
            .y = dy,            // Y축 이동
            .wheel = 0          // 휠 사용 안 함
        };

        // HID 리포트 전송
        if (sendMouseReport(&mouse_report)) {
            ESP_LOGD(TAG, "Mouse moved: angle=%d, dx=%d, dy=%d", angle, dx, dy);
        } else {
            ESP_LOGW(TAG, "Failed to send mouse report at angle=%d", angle);
        }

        // 다음 단계까지 대기
        vTaskDelay(pdMS_TO_TICKS(delay_ms));
    }

    ESP_LOGI(TAG, "Mouse circle movement test completed");
}

/**
 * @brief "HELLO" 키보드 타이핑 테스트
 *
 * "HELLO" 문자열을 PC에 타이핑하는 HID 리포트를 생성합니다.
 */
void test_keyboard_typing_hello(void) {
    ESP_LOGI(TAG, "Starting keyboard typing 'HELLO' test...");

    // "HELLO" 문자열의 HID 키코드
    // HID 키코드는 class/hid/hid.h에 정의되어 있음
    const uint8_t hello_keys[] = {
        HID_KEY_H,  // 'H'
        HID_KEY_E,  // 'E'
        HID_KEY_L,  // 'L'
        HID_KEY_L,  // 'L'
        HID_KEY_O   // 'O'
    };

    const int num_keys = sizeof(hello_keys) / sizeof(hello_keys[0]);
    const int key_press_ms = 50;    // 키 누름 유지 시간
    const int key_release_ms = 50;  // 키 떼기 후 대기 시간

    for (int i = 0; i < num_keys; i++) {
        // 1. 키 누름 리포트 전송
        hid_keyboard_report_t kb_report_press = {
            .modifier = 0,                  // modifier 없음
            .reserved = 0,
            .keycode = {hello_keys[i], 0, 0, 0, 0, 0}  // 첫 번째 키만 사용
        };

        if (sendKeyboardReport(&kb_report_press)) {
            ESP_LOGD(TAG, "Key pressed: keycode=0x%02x", hello_keys[i]);
        } else {
            ESP_LOGW(TAG, "Failed to send key press report");
        }

        vTaskDelay(pdMS_TO_TICKS(key_press_ms));

        // 2. 키 떼기 리포트 전송 (모든 필드 0)
        hid_keyboard_report_t kb_report_release = {0};

        if (sendKeyboardReport(&kb_report_release)) {
            ESP_LOGD(TAG, "Key released");
        } else {
            ESP_LOGW(TAG, "Failed to send key release report");
        }

        vTaskDelay(pdMS_TO_TICKS(key_release_ms));
    }

    ESP_LOGI(TAG, "Keyboard typing 'HELLO' test completed");
}

/**
 * @brief 마우스 클릭 테스트
 *
 * 마우스 좌클릭을 수행하는 HID 리포트를 생성합니다.
 */
void test_mouse_click(void) {
    ESP_LOGI(TAG, "Starting mouse click test...");

    const int click_duration_ms = 50;   // 클릭 유지 시간
    const int release_delay_ms = 100;   // 클릭 해제 후 대기

    // 1. 좌클릭 누름
    hid_mouse_report_t mouse_click_press = {
        .buttons = MOUSE_BUTTON_LEFT,  // 좌클릭 (0x01)
        .x = 0,
        .y = 0,
        .wheel = 0
    };

    if (sendMouseReport(&mouse_click_press)) {
        ESP_LOGD(TAG, "Mouse left button pressed");
    } else {
        ESP_LOGW(TAG, "Failed to send mouse click press");
    }

    vTaskDelay(pdMS_TO_TICKS(click_duration_ms));

    // 2. 좌클릭 떼기
    hid_mouse_report_t mouse_click_release = {0};

    if (sendMouseReport(&mouse_click_release)) {
        ESP_LOGD(TAG, "Mouse left button released");
    } else {
        ESP_LOGW(TAG, "Failed to send mouse click release");
    }

    vTaskDelay(pdMS_TO_TICKS(release_delay_ms));

    ESP_LOGI(TAG, "Mouse click test completed");
}

/**
 * @brief 키 조합 테스트 (Ctrl+C)
 *
 * Ctrl+C 키 조합을 전송하는 HID 리포트를 생성합니다.
 */
void test_key_combination_ctrl_c(void) {
    ESP_LOGI(TAG, "Starting key combination (Ctrl+C) test...");

    const int key_press_ms = 100;
    const int key_release_ms = 100;

    // 1. Ctrl+C 누름
    hid_keyboard_report_t kb_ctrl_c_press = {
        .modifier = KEYBOARD_MODIFIER_LEFTCTRL,  // Ctrl 키
        .reserved = 0,
        .keycode = {HID_KEY_C, 0, 0, 0, 0, 0}    // 'C' 키
    };

    if (sendKeyboardReport(&kb_ctrl_c_press)) {
        ESP_LOGD(TAG, "Ctrl+C pressed");
    } else {
        ESP_LOGW(TAG, "Failed to send Ctrl+C press");
    }

    vTaskDelay(pdMS_TO_TICKS(key_press_ms));

    // 2. Ctrl+C 떼기
    hid_keyboard_report_t kb_release = {0};

    if (sendKeyboardReport(&kb_release)) {
        ESP_LOGD(TAG, "Ctrl+C released");
    } else {
        ESP_LOGW(TAG, "Failed to send key release");
    }

    vTaskDelay(pdMS_TO_TICKS(key_release_ms));

    ESP_LOGI(TAG, "Key combination (Ctrl+C) test completed");
}

// ==================== HID 테스트 태스크 ====================

/**
 * @brief HID 테스트 태스크
 *
 * 테스트 타입에 따라 다른 패턴의 HID 데이터를 생성하여 전송합니다.
 *
 * @param param 테스트 타입 (hid_test_type_t*)
 */
void hid_test_task(void* param) {
    // 태스크 워치독 등록
    esp_task_wdt_add(NULL);

    ESP_LOGI(TAG, "HID Test Task started");

    // 테스트 타입 추출
    hid_test_type_t test_type = HID_TEST_ALL;
    if (param != NULL) {
        test_type = *((hid_test_type_t*)param);
    }

    // USB 연결 대기 (최대 5초)
    ESP_LOGI(TAG, "Waiting for USB connection...");
    for (int i = 0; i < 50; i++) {
        if (tud_ready()) {
            ESP_LOGI(TAG, "USB connected!");
            break;
        }
        vTaskDelay(pdMS_TO_TICKS(100));
        esp_task_wdt_reset();
    }

    if (!tud_ready()) {
        ESP_LOGW(TAG, "USB not connected after 5 seconds, continuing anyway...");
    }

    // 초기 대기 (USB 열거 완료 대기)
    ESP_LOGI(TAG, "Waiting 2 seconds for USB enumeration...");
    vTaskDelay(pdMS_TO_TICKS(2000));

    // 테스트 실행
    while (1) {
        switch (test_type) {
            case HID_TEST_MOUSE_CIRCLE:
                test_mouse_circle_movement();
                vTaskDelay(pdMS_TO_TICKS(2000));  // 2초 대기 후 반복
                break;

            case HID_TEST_KEYBOARD_HELLO:
                test_keyboard_typing_hello();
                vTaskDelay(pdMS_TO_TICKS(3000));  // 3초 대기 후 반복
                break;

            case HID_TEST_MOUSE_CLICK:
                test_mouse_click();
                vTaskDelay(pdMS_TO_TICKS(2000));  // 2초 대기 후 반복
                break;

            case HID_TEST_KEY_COMBO:
                test_key_combination_ctrl_c();
                vTaskDelay(pdMS_TO_TICKS(3000));  // 3초 대기 후 반복
                break;

            case HID_TEST_ALL:
            default:
                ESP_LOGI(TAG, "=== Running tests (1 cycle) ===");

                ESP_LOGI(TAG, "Test 1/2: Mouse circle movement");
                test_mouse_circle_movement();
                vTaskDelay(pdMS_TO_TICKS(1000));

                ESP_LOGI(TAG, "Test 2/2: Keyboard typing 'HELLO'");
                test_keyboard_typing_hello();

                ESP_LOGI(TAG, "=== All tests completed - Task ending ===");

                // 워치독 해제 후 태스크 종료
                esp_task_wdt_delete(NULL);
                vTaskDelete(NULL);
                break;
        }

        // 워치독 리셋
        esp_task_wdt_reset();
    }
}
