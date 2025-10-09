/**
 * @file main.cpp
 * @brief Phase 1.1.2.1: UART 통신 초기화
 * @details ESP32-S3 UART2 포트 초기화 및 1Mbps 통신 설정
 */

#include <Arduino.h>

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
  Serial.println("  Phase 1.1.2.1: UART Initialization");
  Serial.println("========================================");
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