/**
 * @file main.cpp
 * @brief Phase 1.2.1.1: Arduino USB HID 라이브러리 초기화
 * @details ESP32-S3 USB 복합 장치 구성 (HID Mouse + HID Keyboard)
 */

#include <Arduino.h>
#include "USBHIDMouse.h"
#include "USBHIDKeyboard.h"

// ============================================================================
// BridgeFrame 구조체 정의
// ============================================================================

typedef struct __attribute__((packed)) {
  uint8_t seq;        // [0] 순번 카운터 (0~255 순환)
  uint8_t buttons;    // [1] 마우스 버튼 상태
  int8_t deltaX;      // [2] X축 상대 이동
  int8_t deltaY;      // [3] Y축 상대 이동
  int8_t wheel;       // [4] 휠 스크롤량
  uint8_t modifiers;  // [5] 키보드 모디파이어
  uint8_t keyCode1;   // [6] 주요 키 입력
  uint8_t keyCode2;   // [7] 보조 키 입력
} BridgeFrame;

static_assert(sizeof(BridgeFrame) == 8, "BridgeFrame must be exactly 8 bytes!");

// ============================================================================
// USB HID 객체
// ============================================================================

USBHIDMouse Mouse;
USBHIDKeyboard Keyboard;

// ============================================================================
// 전역 변수
// ============================================================================

BridgeFrame g_rxFrame;
uint8_t g_lastSeq = 255;
uint32_t g_frameCount = 0;
uint32_t g_lostFrames = 0;
uint32_t g_lastStatsTime = 0;

// ============================================================================
// UART 설정
// ============================================================================

constexpr uint32_t UART_BAUD_RATE = 1000000;
constexpr uint8_t UART_RX_PIN = 44;
constexpr uint8_t UART_TX_PIN = 43;

// ============================================================================
// 함수 선언
// ============================================================================

bool receiveFrame() {
  if (Serial2.available() >= 8) {
    size_t bytesRead = Serial2.readBytes((uint8_t*)&g_rxFrame, sizeof(BridgeFrame));
    return (bytesRead == 8);
  }
  return false;
}

bool validateSequence(uint8_t currentSeq) {
  // 첫 프레임
  if (g_lastSeq == 255) {
    g_lastSeq = currentSeq;
    g_frameCount++;
    Serial.printf("[SEQ] First frame: seq=%d\n", currentSeq);
    return true;
  }
  
  // 예상 순번
  uint8_t expectedSeq = (g_lastSeq + 1) % 256;
  
  if (currentSeq == expectedSeq) {
    g_lastSeq = currentSeq;
    g_frameCount++;
    return true;
  }
  
  // 순번 불일치 - 프레임 손실
  uint32_t lost;
  if (currentSeq > expectedSeq) {
    lost = currentSeq - expectedSeq;
  } else {
    lost = (256 - expectedSeq) + currentSeq;
  }
  
  g_lostFrames += lost;
  g_lastSeq = currentSeq;
  g_frameCount++;
  
  Serial.printf("[WARN] Sequence mismatch! Expected=%d, Received=%d, Lost=%lu\n",
                expectedSeq, currentSeq, lost);
  
  return false;
}

void printStats() {
  uint32_t now = millis();
  
  if (now - g_lastStatsTime >= 10000) {  // 1초마다
    float lossRate = 0.0f;
    if (g_frameCount > 0) {
      lossRate = (g_lostFrames * 100.0f) / (g_frameCount + g_lostFrames);
    }
    
    Serial.println("========================================");
    Serial.printf("[STATS] Uptime: %lu sec\n", now / 1000);
    Serial.printf("[STATS] Received: %lu frames\n", g_frameCount);
    Serial.printf("[STATS] Lost: %lu frames (%.2f%%)\n", g_lostFrames, lossRate);
    Serial.printf("[STATS] Last Seq: %d\n", g_lastSeq);
    Serial.printf("[MEMORY] Free Heap: %u bytes\n", ESP.getFreeHeap());
    Serial.println("========================================");
    
    g_lastStatsTime = now;
  }
}

// ============================================================================
// Setup & Loop
// ============================================================================

void setup() {
  // USB CDC 초기화 (디버그용)
  Serial.begin(115200);
  delay(1500);  // USB enumeration 안정화 대기
  
  Serial.println();
  Serial.println("========================================");
  Serial.println("  BridgeOne ESP32-S3 Board");
  Serial.println("  Phase 1.2.1.1: USB HID Initialization");
  Serial.println("========================================");
  Serial.println();
  
  // USB HID 초기화
  Serial.println("[USB HID] Initializing...");
  
  // HID 장치 초기화 (USB.begin()은 자동 처리됨)
  Mouse.begin();
  Serial.println("  ✓ USBHIDMouse initialized");
  
  Keyboard.begin();
  Serial.println("  ✓ USBHIDKeyboard initialized");
  
  delay(100);  // HID 초기화 안정화 대기
  
  Serial.println("  ✓ USB HID initialization complete");
  Serial.println();
  
  // BridgeFrame 검증
  Serial.println("[BridgeFrame] Structure Verification:");
  Serial.printf("  - Size: %d bytes (expected: 8 bytes)\n", sizeof(BridgeFrame));
  if (sizeof(BridgeFrame) == 8) {
    Serial.println("  ✓ BridgeFrame size verification: PASS");
  } else {
    Serial.println("  ✗ BridgeFrame size verification: FAIL!");
  }
  Serial.println();
  
  // UART2 초기화
  Serial2.setRxBufferSize(256);
  Serial2.setTxBufferSize(128);
  Serial2.begin(UART_BAUD_RATE, SERIAL_8N1, UART_RX_PIN, UART_TX_PIN);
  Serial.printf("[UART2] Initialized: %d bps, 8N1\n", UART_BAUD_RATE);
  Serial.printf("[UART2] Pins: RX=GPIO%d, TX=GPIO%d\n", UART_RX_PIN, UART_TX_PIN);
  Serial.println();
  
  Serial.println("[INIT] System ready!");
  Serial.println("[INIT] Waiting for Android connection...");
  Serial.println("========================================");
  Serial.println();
}

void loop() {
  // UART 프레임 수신
  if (receiveFrame()) {
    bool seqValid = validateSequence(g_rxFrame.seq);
    
    // 프레임 내용 출력
    Serial.printf("[RX #%lu] seq=%d %s, btn=0x%02X, delta=(%d,%d), wheel=%d, mod=0x%02X, keys=[0x%02X,0x%02X]\n",
                  g_frameCount,
                  g_rxFrame.seq,
                  seqValid ? "✓" : "✗",
                  g_rxFrame.buttons,
                  g_rxFrame.deltaX,
                  g_rxFrame.deltaY,
                  g_rxFrame.wheel,
                  g_rxFrame.modifiers,
                  g_rxFrame.keyCode1,
                  g_rxFrame.keyCode2);
  }
  
  // 통계 출력
  printStats();
  
  delay(1);  // CPU 부하 감소
}
