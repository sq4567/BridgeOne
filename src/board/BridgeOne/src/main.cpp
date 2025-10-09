/**
 * @file main.cpp
 * @brief Phase 1.1.2.2: BridgeOne 프레임 구조체 정의
 * @details ESP32-S3 UART2 포트 초기화 및 BridgeFrame 데이터 구조 구현
 */

#include <Arduino.h>

// ============================================================================
// BridgeOne 프레임 구조체 정의 (Phase 1.1.2.2)
// ============================================================================

/**
 * @brief BridgeOne 통신 프레임 구조체
 * @details Android 앱에서 UART를 통해 전송되는 8바이트 프레임 구조
 * 
 * 프레임 레이아웃:
 * - [0] seq:       순번 카운터 (0-255 순환, 패킷 손실 감지용)
 * - [1] buttons:   마우스 버튼 상태 (bit0=왼쪽, bit1=오른쪽, bit2=가운데)
 * - [2] deltaX:    X축 상대 이동량 (-127~127)
 * - [3] deltaY:    Y축 상대 이동량 (-127~127)
 * - [4] wheel:     마우스 휠 스크롤량 (-127~127)
 * - [5] modifiers: 키보드 모디파이어 키 (Ctrl, Alt, Shift, GUI)
 * - [6] keyCode1:  주요 키 코드 (첫 번째 키)
 * - [7] keyCode2:  보조 키 코드 (두 번째 키)
 * 
 * @note __attribute__((packed))를 사용하여 컴파일러 패딩 방지
 * @note 정확히 8바이트 크기를 보장해야 함
 * @see docs/Board/esp32s3-code-implementation-guide.md §2.1
 */
typedef struct __attribute__((packed)) {
  uint8_t seq;        // [0] 순번 카운터 (0~255 순환)
  uint8_t buttons;    // [1] 마우스 버튼 상태 (bit mask)
  int8_t deltaX;      // [2] X축 상대 이동 (-127~127)
  int8_t deltaY;      // [3] Y축 상대 이동 (-127~127)
  int8_t wheel;       // [4] 휠 스크롤량 (-127~127)
  uint8_t modifiers;  // [5] 키보드 모디파이어 (bit mask)
  uint8_t keyCode1;   // [6] 주요 키 입력
  uint8_t keyCode2;   // [7] 보조 키 입력
} BridgeFrame;

// 컴파일 타임 크기 검증: BridgeFrame이 정확히 8바이트인지 확인
static_assert(sizeof(BridgeFrame) == 8, 
              "BridgeFrame must be exactly 8 bytes! Check structure packing.");

// 전역 BridgeFrame 버퍼 (UART 수신 데이터 저장용)
BridgeFrame g_rxFrame;

// ============================================================================
// UART 설정 상수
// ============================================================================
// 참고: ESP32-S3 Arduino 프레임워크에는 Serial2가 이미 정의되어 있음
// HardwareSerial Serial2(2)는 HardwareSerial.cpp에서 자동으로 생성됨
constexpr uint32_t UART_BAUD_RATE = 1000000;    // 1Mbps (1,000,000 bps)
constexpr uint8_t UART_RX_PIN = 16;             // GPIO16 (RX)
constexpr uint8_t UART_TX_PIN = 17;             // GPIO17 (TX)
constexpr size_t UART_RX_BUFFER_SIZE = 256;     // RX 버퍼: 256 bytes
constexpr size_t UART_TX_BUFFER_SIZE = 128;     // TX 버퍼: 128 bytes

/**
 * @brief 시스템 초기화
 * @details USB CDC 및 UART2 초기화 수행
 */
void setup() {
  // ========================================
  // USB CDC 시리얼 초기화 (디버그 로그용)
  // ========================================
  Serial.begin(115200);
  delay(2000);  // USB CDC 준비 대기 (최대 2초)
  
  Serial.println();
  Serial.println("========================================");
  Serial.println("  BridgeOne ESP32-S3 Board");
  Serial.println("  Phase 1.1.2.2: BridgeFrame Structure");
  Serial.println("========================================");
  Serial.println();
  
  // ========================================
  // BridgeFrame 구조체 검증 (Phase 1.1.2.2)
  // ========================================
  Serial.println("[BridgeFrame] Structure Verification:");
  Serial.printf("  - Size: %d bytes (expected: 8 bytes)\n", sizeof(BridgeFrame));
  Serial.printf("  - seq offset: %d\n", offsetof(BridgeFrame, seq));
  Serial.printf("  - buttons offset: %d\n", offsetof(BridgeFrame, buttons));
  Serial.printf("  - deltaX offset: %d\n", offsetof(BridgeFrame, deltaX));
  Serial.printf("  - deltaY offset: %d\n", offsetof(BridgeFrame, deltaY));
  Serial.printf("  - wheel offset: %d\n", offsetof(BridgeFrame, wheel));
  Serial.printf("  - modifiers offset: %d\n", offsetof(BridgeFrame, modifiers));
  Serial.printf("  - keyCode1 offset: %d\n", offsetof(BridgeFrame, keyCode1));
  Serial.printf("  - keyCode2 offset: %d\n", offsetof(BridgeFrame, keyCode2));
  
  // 구조체 크기 검증 (런타임)
  if (sizeof(BridgeFrame) == 8) {
    Serial.println("  ✓ BridgeFrame size verification: PASS");
  } else {
    Serial.println("  ✗ BridgeFrame size verification: FAIL!");
  }
  Serial.println();
  
  // ========================================
  // UART2 버퍼 크기 설정 (begin() 전에 호출 필수)
  // ========================================
  Serial2.setRxBufferSize(UART_RX_BUFFER_SIZE);
  Serial2.setTxBufferSize(UART_TX_BUFFER_SIZE);
  Serial.printf("[UART2] Buffer Size Set: RX=%d, TX=%d\n", 
                UART_RX_BUFFER_SIZE, UART_TX_BUFFER_SIZE);
  
  // ========================================
  // UART2 초기화
  // ========================================
  // 파라미터:
  //   - baud: 1,000,000 bps (1Mbps)
  //   - config: SERIAL_8N1 (8 data bits, No parity, 1 stop bit)
  //   - rxPin: GPIO16
  //   - txPin: GPIO17
  Serial2.begin(UART_BAUD_RATE, SERIAL_8N1, UART_RX_PIN, UART_TX_PIN);
  Serial.printf("[UART2] Initialized: %d bps, 8N1\n", UART_BAUD_RATE);
  Serial.printf("[UART2] Pins: RX=GPIO%d, TX=GPIO%d\n", UART_RX_PIN, UART_TX_PIN);
  
  Serial.println();
  Serial.println("[INIT] All systems initialized successfully!");
  Serial.println("========================================");
  Serial.println();
}

/**
 * @brief 메인 루프
 * @details UART2 연결 상태 확인 및 카운터 출력
 */
void loop() {
  static uint32_t counter = 0;
  
  // USB CDC로 카운터 출력
  Serial.printf("[Loop] Counter: %lu | UART2 Available: %d bytes\n", 
                counter++, Serial2.available());
  
  delay(1000);
}