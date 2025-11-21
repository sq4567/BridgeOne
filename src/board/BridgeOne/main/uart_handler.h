#ifndef UART_HANDLER_H
#define UART_HANDLER_H

#include <stdint.h>
#include "hal/uart_types.h"
#include "hal/gpio_types.h"
#include "freertos/FreeRTOS.h"
#include "freertos/queue.h"

/**
 * UART 통신 설정 상수.
 *
 * 보드별 UART 구성:
 * - ESP32-S3-DevkitC-1: UART0 (GPIO43/44) - CP2102N USB-UART 브릿지 연결
 * - YD-ESP32-S3 N16R8: UART1 (GPIO17/18) - Android 통신 전용 (CH343는 플래시용)
 *
 * YD-ESP32-S3 보드 사용 시:
 * - UART1 (GPIO17/18): Android 앱과 직접 통신 (1Mbps)
 * - UART0 (GPIO43/44): CH343 USB-UART 브릿지 (펌웨어 플래시 및 디버그 로그)
 */
#define UART_NUM UART_NUM_1
#define UART_TX_PIN GPIO_NUM_17     // Android 통신용 TX 핀 (YD-ESP32-S3 UART1)
#define UART_RX_PIN GPIO_NUM_18     // Android 통신용 RX 핀 (YD-ESP32-S3 UART1)
#define UART_BAUDRATE 1000000       // 1 Mbps
#define UART_DATA_BITS UART_DATA_8_BITS
#define UART_PARITY UART_PARITY_DISABLE
#define UART_STOP_BITS UART_STOP_BITS_1
#define UART_RX_BUFFER_SIZE 256     // RX 버퍼 크기
#define UART_TX_BUFFER_SIZE 256     // TX 버퍼 크기
#define UART_RX_TIMEOUT_MS 100      // RX 타임아웃 (100ms)

/**
 * BridgeOne 프로토콜 데이터 구조체 (정확히 8바이트).
 *
 * Android에서 전송하는 마우스/키보드 입력 데이터를 나타냅니다.
 * UART 프로토콜 정의이므로 필드 순서와 크기는 변경 불가합니다.
 *
 * 레이아웃 (총 8바이트):
 *  - seq:      시퀀스 번호 (0~255 순환)
 *  - buttons:  마우스 버튼 비트 (Bit 0: L, Bit 1: R, Bit 2: M)
 *  - x:        X축 이동값 (signed -127~127)
 *  - y:        Y축 이동값 (signed -127~127)
 *  - wheel:    스크롤 휠 값 (signed -127~127)
 *  - modifier: 키보드 modifier 키 (Ctrl, Shift, Alt, Meta)
 *  - keycode1: 첫 번째 키코드
 *  - keycode2: 두 번째 키코드
 */
typedef struct {
    uint8_t seq;        // 바이트 0: 시퀀스 번호
    uint8_t buttons;    // 바이트 1: 마우스 버튼
    int8_t x;           // 바이트 2: X축 이동값
    int8_t y;           // 바이트 3: Y축 이동값
    int8_t wheel;       // 바이트 4: 휠 값
    uint8_t modifier;   // 바이트 5: 키보드 modifier
    uint8_t keycode1;   // 바이트 6: 첫 번째 키코드
    uint8_t keycode2;   // 바이트 7: 두 번째 키코드
} bridge_frame_t;

/**
 * UART 초기화 함수.
 *
 * Android ↔ ESP32-S3 통신용 UART를 설정합니다.
 *
 * 보드별 구성:
 * - ESP32-S3-DevkitC-1: UART0 (GPIO43/44) - CP2102N USB-UART 브릿지
 * - YD-ESP32-S3 N16R8: UART0 (GPIO43/44) - CH343P USB-UART 브릿지
 *
 * 두 보드 모두 USB-UART 브릿지를 통해 UART0으로 Android와 통신합니다.
 * USB만 연결하면 점퍼 케이블 없이 바로 동작합니다.
 *
 * @return
 *   - ESP_OK (0): 성공
 *   - 기타: 실패 (esp_err_t 값)
 */
int uart_init(void);

/**
 * UART 수신 태스크.
 *
 * UART에서 8바이트 프레임을 수신하고 검증하여 FreeRTOS 큐로 전송합니다.
 * 프레임 유효성 검증 실패 시 해당 프레임은 폐기됩니다.
 *
 * @param param 미사용
 */
void uart_task(void* param);

/**
 * FreeRTOS 큐 핸들.
 *
 * uart_task()에서 검증된 프레임을 이 큐에 전송합니다.
 * app_main()에서 xQueueCreate()로 초기화됨.
 */
extern QueueHandle_t frame_queue;

#endif // UART_HANDLER_H
