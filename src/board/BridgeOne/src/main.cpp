/**
 * @file main.cpp
 * @brief Phase 1.1.2.3: UART 수신 및 프레임 파싱 구현
 * @details ESP32-S3 UART2를 통한 8바이트 BridgeFrame 수신 및 파싱
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
// UART 수신 함수 (Phase 1.1.2.3)
// ============================================================================

/**
 * @brief UART2로부터 8바이트 BridgeFrame을 수신하고 파싱
 * @details 버퍼에 8바이트 이상의 데이터가 있을 때만 읽기 수행
 * 
 * 동작 과정:
 * 1. UART2.available()로 수신 버퍼의 데이터 크기 확인
 * 2. 8바이트 이상이면 readBytes()로 g_rxFrame에 직접 읽기
 * 3. 정확히 8바이트 읽었는지 검증
 * 
 * @return true  - 8바이트 프레임 수신 성공
 * @return false - 수신할 데이터 부족 또는 읽기 실패
 * 
 * @note 버퍼 오버플로우 방지를 위해 available() 체크 필수
 * @note readBytes()는 블로킹 함수이므로 available() 선행 체크로 안전성 확보
 * @see HardwareSerial::available() - ESP32 Arduino Core
 * @see HardwareSerial::readBytes() - ESP32 Arduino Core
 */
bool receiveFrame() {
  // 수신 버퍼에 8바이트 이상의 데이터가 있는지 확인
  if (Serial2.available() >= 8) {
    // 8바이트를 g_rxFrame 구조체에 직접 읽기
    // readBytes()는 실제로 읽은 바이트 수를 반환
    size_t bytesRead = Serial2.readBytes((uint8_t*)&g_rxFrame, sizeof(BridgeFrame));
    
    // 정확히 8바이트를 읽었는지 검증
    if (bytesRead == 8) {
      return true;  // 수신 성공
    } else {
      // 예상치 못한 읽기 오류 (정상적으로는 발생하지 않아야 함)
      Serial.printf("[WARN] receiveFrame: Expected 8 bytes, but read %d bytes\n", bytesRead);
      return false;
    }
  }
  
  // 8바이트 미만이면 대기 (버퍼 오버플로우 방지)
  return false;
}

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
  Serial.println("  Phase 1.1.2.3: UART RX & Frame Parsing");
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
 * @details UART2로부터 프레임 수신 및 처리
 */
void loop() {
  static uint32_t frameCount = 0;  // 수신 프레임 카운터
  
  // UART2로부터 프레임 수신 시도
  if (receiveFrame()) {
    // ========================================
    // 프레임 수신 성공 - 내용 출력
    // ========================================
    frameCount++;
    
    Serial.printf("[RX #%lu] seq=%d, buttons=0x%02X, delta=(%d,%d), wheel=%d, mods=0x%02X, keys=[0x%02X, 0x%02X]\n",
                  frameCount,
                  g_rxFrame.seq,
                  g_rxFrame.buttons,
                  g_rxFrame.deltaX,
                  g_rxFrame.deltaY,
                  g_rxFrame.wheel,
                  g_rxFrame.modifiers,
                  g_rxFrame.keyCode1,
                  g_rxFrame.keyCode2);
    
    // 버튼 상태 상세 출력 (디버깅용)
    if (g_rxFrame.buttons != 0) {
      Serial.print("  └─ Buttons: ");
      if (g_rxFrame.buttons & 0x01) Serial.print("[LEFT] ");
      if (g_rxFrame.buttons & 0x02) Serial.print("[RIGHT] ");
      if (g_rxFrame.buttons & 0x04) Serial.print("[MIDDLE] ");
      Serial.println();
    }
    
    // 마우스 이동 출력 (0이 아닐 때만)
    if (g_rxFrame.deltaX != 0 || g_rxFrame.deltaY != 0) {
      Serial.printf("  └─ Mouse Move: X%+d Y%+d\n", g_rxFrame.deltaX, g_rxFrame.deltaY);
    }
    
    // 휠 스크롤 출력 (0이 아닐 때만)
    if (g_rxFrame.wheel != 0) {
      Serial.printf("  └─ Wheel: %+d\n", g_rxFrame.wheel);
    }
  }
  
  // CPU 부하 감소를 위한 짧은 지연 (1ms)
  // 너무 빠른 폴링은 불필요한 CPU 사용 증가
  delay(1);
}