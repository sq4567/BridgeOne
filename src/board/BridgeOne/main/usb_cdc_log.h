#ifndef USB_CDC_LOG_H
#define USB_CDC_LOG_H

#include <stdbool.h>

/**
 * USB CDC 디버그 로깅 모듈.
 *
 * ESP_LOG 출력을 Native USB OTG의 CDC 인터페이스로 리다이렉트합니다.
 * 이를 통해 UART0 (CH343P)를 Android 통신에 사용하면서도
 * 포트 2️⃣ (Native USB OTG)를 통해 PC에서 디버그 로그를 확인할 수 있습니다.
 *
 * 물리적 연결:
 * - 포트 1️⃣ (USB-C, CH343P) → Android 스마트폰 (UART 통신)
 * - 포트 2️⃣ (Micro-USB, Native USB OTG) → PC (디버그 로그 + HID)
 *
 * 사용법:
 * 1. app_main()에서 usb_cdc_log_init() 호출
 * 2. PC에서 포트 2️⃣의 CDC 시리얼 포트 연결 (PuTTY, minicom 등)
 * 3. ESP_LOGI/ESP_LOGW/ESP_LOGE 등의 로그가 CDC로 출력됨
 *
 * PC에서 CDC 포트 확인:
 * - Windows: 장치 관리자 → 포트(COM & LPT) → "USB Serial Device (COMx)"
 * - Linux: /dev/ttyACM0 또는 /dev/ttyUSB0
 * - macOS: /dev/cu.usbmodem*
 */

/**
 * USB CDC 로깅 초기화.
 *
 * ESP_LOG의 출력을 USB CDC로 리다이렉트합니다.
 * TinyUSB가 초기화된 후에 호출해야 합니다.
 *
 * @return true: 초기화 성공, false: 실패
 */
bool usb_cdc_log_init(void);

/**
 * USB CDC 로깅 비활성화.
 *
 * ESP_LOG의 출력을 기본 UART로 복원합니다.
 */
void usb_cdc_log_deinit(void);

/**
 * USB CDC 로깅 활성화 여부 확인.
 *
 * @return true: CDC 로깅 활성화됨, false: 기본 UART 로깅
 */
bool usb_cdc_log_is_enabled(void);

/**
 * USB CDC로 직접 문자열 출력.
 *
 * ESP_LOG를 거치지 않고 USB CDC로 직접 문자열을 출력합니다.
 * 디버깅이나 테스트 목적으로 사용할 수 있습니다.
 *
 * @param str 출력할 문자열
 */
void usb_cdc_log_write(const char* str);

#endif // USB_CDC_LOG_H
