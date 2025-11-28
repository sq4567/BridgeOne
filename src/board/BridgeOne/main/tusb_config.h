/**
 * @file tusb_config.h
 * @brief TinyUSB Stack Configuration for BridgeOne
 * 
 * 역할:
 * - TinyUSB 스택의 컴파일 타임 설정 (Configuration)
 * - HID, CDC 클래스 활성화
 * - 엔드포인트 및 버퍼 크기 설정
 * - FreeRTOS 통합 설정
 * 
 * 참고:
 * - 이 파일은 tusb_option.h에서 #include되며, 빌드 전에 반드시 있어야 함
 * - ESP-IDF 통합: TUSB_OPT_MCU는 CMakeLists.txt에서 자동 설정
 */

#ifndef TUSB_CONFIG_H_
#define TUSB_CONFIG_H_

#ifdef __cplusplus
extern "C" {
#endif

// ==================== MCU Configuration ====================
// 이 값은 CMakeLists.txt의 -DCFG_TUSB_MCU=OPT_MCU_ESP32S3에서 자동 설정됨
// #define CFG_TUSB_MCU OPT_MCU_ESP32S3

// ==================== Mode Configuration ====================
/**
 * 디바이스 모드만 활성화 (Host 모드 비활성화)
 * BridgeOne은 USB 디바이스로만 동작하므로 Device 모드만 필요
 */
#define CFG_TUD_ENABLED 1
#define CFG_TUSB_RHPORT0_MODE    OPT_MODE_DEVICE

// ==================== Device Configuration ====================
/**
 * USB 디바이스 엔드포인트 설정
 * - RHPORT: USB OTG 포트 번호 (ESP32-S3는 0만 지원)
 * - 최대 핸들 수: 디바이스 FIFO 크기에 따라 결정
 */
#define CFG_TUD_RHPORT0_MODE    OPT_MODE_DEVICE

// ==================== Endpoint Configuration ====================
/**
 * 엔드포인트 수 설정 (기본값 대비 최적화)
 * 
 * BridgeOne 구성:
 * - EP 0: Control (자동, 항상 필수)
 * - EP 1: HID Keyboard IN
 * - EP 2: HID Mouse IN
 * - EP 3: CDC Notification IN
 * - EP 4: CDC Data OUT/IN (양방향)
 * 
 * 총 필요: 5개 엔드포인트
 */
#define CFG_TUD_ENDPOINT0_SIZE  64

// ==================== HID Configuration ====================
/**
 * HID (Human Interface Device) Class 활성화
 *
 * 설정:
 * - CFG_TUD_HID: HID 인터페이스 개수 (Keyboard + Mouse = 2개)
 * - CFG_TUD_HID_EP_BUFSIZE: 각 HID 엔드포인트 버퍼 크기 (64 bytes = Full-speed 최대)
 * - CFG_TUD_HID_EP_BUFSIZE: 두 HID 인터페이스(Keyboard, Mouse) 각각에 적용
 */
#define CFG_TUD_HID         2
#define CFG_TUD_HID_EP_BUFSIZE  64

// ==================== CDC Configuration ====================
/**
 * CDC-ACM (Communications Device Class) 활성화
 * 
 * 설정:
 * - CFG_TUD_CDC: CDC 인터페이스 개수 (1개)
 * - CFG_TUD_CDC_RX_BUFSIZE: 호스트 → 디바이스 수신 버퍼 (512 bytes)
 * - CFG_TUD_CDC_TX_BUFSIZE: 디바이스 → 호스트 전송 버퍼 (512 bytes)
 * - CFG_TUD_CDC_EP_BUFSIZE: 각 CDC 엔드포인트 버퍼 (64 bytes)
 */
#define CFG_TUD_CDC         1
#define CFG_TUD_CDC_RX_BUFSIZE  512
#define CFG_TUD_CDC_TX_BUFSIZE  512
#define CFG_TUD_CDC_EP_BUFSIZE  64

// ==================== OS Configuration ====================
/**
 * FreeRTOS 통합 설정
 * 
 * TinyUSB는 FreeRTOS를 인지하며, 다음 기능을 자동 활용:
 * - Task 동기화
 * - IRQ 안전 연산
 * - 다중 코어 지원
 */
#define CFG_TUSB_OS         OPT_OS_FREERTOS

// ==================== Logging Configuration ====================
/**
 * 디버그 로깅 설정 (개발 단계에서 활성화 권장)
 * 
 * 옵션:
 * - 0: 비활성화 (프로덕션)
 * - 1: 기본 로깅 (디버그)
 * - 2: 상세 로깅 (개발)
 */
#define CFG_TUSB_DEBUG      1

// ==================== Additional Features ====================
/**
 * 추가 기능 설정 (선택 사항)
 * 
 * - CFG_TUD_ENABLED: Device 기능 활성화 (필수)
 * - CFG_TUSB_RHPORT_SPEED: USB 속도 (OPT_MODE_FULL_SPEED: 12Mbps)
 */
#define CFG_TUSB_RHPORT_SPEED   OPT_MODE_FULL_SPEED

// ==================== Buffer Configuration ====================
/**
 * 전역 FIFO 버퍼 크기
 * 
 * ESP32-S3의 USB FIFO 총 크기는 제한적이므로, 각 엔드포인트 크기를 
 * 신중하게 설정해야 함
 * 
 * 계산: 64 (EP0) + 64 (KB) + 64 (Mouse) + 8 (CDC Notif) + 64 (CDC Data) = 264 bytes
 */

#ifdef __cplusplus
}
#endif

#endif /* TUSB_CONFIG_H_ */
