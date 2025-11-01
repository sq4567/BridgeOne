#ifndef UART_HANDLER_H
#define UART_HANDLER_H

#include <stdint.h>
#include "hal/uart_types.h"

/**
 * UART 통신 설정 상수.
 */
#define UART_NUM UART_NUM_0
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
 * ESP32-S3-DevkitC-1의 내장 USB-to-UART 브릿지를 설정합니다.
 * GPIO43(U0TXD), GPIO44(U0RXD)를 사용하며, 핀 설정은 자동으로 처리됩니다.
 *
 * @return
 *   - 성공 시 0 반환
 *   - 실패 시 0이 아닌 값 반환
 */
int uart_init(void);

#endif // UART_HANDLER_H
