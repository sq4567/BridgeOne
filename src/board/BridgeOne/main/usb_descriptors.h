/**
 * @file usb_descriptors.h
 * @brief BridgeOne USB Composite 디바이스 디스크립터 정의
 * 
 * 역할:
 * - USB Device/Configuration/HID Report/String Descriptor 상수 정의
 * - Keyboard + Mouse HID 인터페이스 설정 (Boot Protocol)
 * - CDC 통신 인터페이스 설정
 * - 엔드포인트 번호 및 VID/PID 정의
 * 
 * 참조: esp32s3-code-implementation-guide.md §1.3 USB Composite 디바이스 설계 계약
 */

#ifndef USB_DESCRIPTORS_H
#define USB_DESCRIPTORS_H

#include <stdint.h>
#include "tusb.h"

// ==================== VID/PID 설정 ====================
/**
 * VID: 0x303A (Espressif 공식 VID)
 * PID: 0x4001 (BridgeOne 프로젝트용)
 */
#define USB_VID             0x303A
#define USB_PID             0x4001

// ==================== 엔드포인트 번호 정의 ====================
/**
 * 엔드포인트 번호 (방향 비트 포함: IN=0x80, OUT=0x00)
 * ESP32-S3는 최대 6개의 IN/OUT 엔드포인트 쌍을 지원
 */
#define EPNUM_HID_KB        0x81    // IN EP1: Keyboard 리포트
#define EPNUM_HID_MOUSE     0x82    // IN EP2: Mouse 리포트
#define EPNUM_CDC_NOTIF     0x83    // IN EP3: CDC Notification
#define EPNUM_CDC_OUT       0x04    // OUT EP4: CDC Data Out
#define EPNUM_CDC_IN        0x84    // IN EP4: CDC Data In

// ==================== 인터페이스 번호 정의 ====================
/**
 * 인터페이스 순서 (절대 변경 금지)
 * - 이 순서는 Android/Windows 드라이버에서 인터페이스 식별에 사용
 * - BIOS/UEFI Boot Protocol 호환성을 위해 HID 인터페이스가 먼저 배치
 */
typedef enum {
    ITF_NUM_HID_KEYBOARD = 0,   // Interface 0: HID Boot Keyboard
    ITF_NUM_HID_MOUSE    = 1,   // Interface 1: HID Boot Mouse
    ITF_NUM_CDC_COMM     = 2,   // Interface 2: CDC-ACM Communication
    ITF_NUM_CDC_DATA     = 3,   // Interface 3: CDC-ACM Data
    ITF_NUM_TOTAL                // 총 인터페이스 수: 4개
} usb_interface_num_t;

// ==================== 디스크립터 전체 길이 ====================
/**
 * Configuration Descriptor 총 길이 계산
 * - TUD_CONFIG_DESC_LEN: 9 bytes
 * - TUD_HID_DESC_LEN × 2: 9 bytes × 2 (Keyboard + Mouse)
 * - TUD_CDC_DESC_LEN: 66 bytes
 * 총합: 9 + 18 + 66 = 93 bytes (약 50 bytes로 명시된 요구사항)
 * 
 * 주의: 실제 Configuration Descriptor 길이는 구현에 따라 다를 수 있음
 */
#define CONFIG_TOTAL_LEN    (TUD_CONFIG_DESC_LEN + TUD_HID_DESC_LEN * 2 + TUD_CDC_DESC_LEN)

// ==================== HID 관련 설정 ====================
/**
 * HID Boot Keyboard 리포트 구조 (8바이트, 고정)
 * 
 * 주의: TinyUSB 헤더(hid.h)에서 이미 정의되어 있으므로 여기서는 참고용 주석만 제공합니다.
 * 
 * 구조:
 * - modifiers: 1 byte (Ctrl, Shift, Alt, GUI)
 * - reserved: 1 byte (0x00)
 * - keyCodes: 6 bytes (6-key rollover)
 */

/**
 * HID Boot Mouse 리포트 구조 (4바이트, 고정)
 * 
 * 주의: TinyUSB 헤더(hid.h)에서 이미 정의되어 있으므로 여기서는 참고용 주석만 제공합니다.
 * 
 * 구조:
 * - buttons: 1 byte (bit0=Left, bit1=Right, bit2=Middle)
 * - deltaX: 1 byte signed
 * - deltaY: 1 byte signed
 * - wheel: 1 byte signed
 */

// ==================== 엔드포인트 버퍼 크기 ====================
/**
 * Full-speed USB는 최대 64바이트 패킷 크기 사용
 */
#define CFG_TUD_HID_EP_BUFSIZE      64
#define CFG_TUD_CDC_EP_BUFSIZE      64

// ==================== 외부 참조 선언 ====================
// usb_descriptors.c에서 정의되는 디스크립터 배열
extern tusb_desc_device_t const desc_device;
extern uint8_t const desc_configuration[];
extern uint8_t const desc_hid_keyboard_report[];
extern uint8_t const desc_hid_mouse_report[];

#endif // USB_DESCRIPTORS_H
