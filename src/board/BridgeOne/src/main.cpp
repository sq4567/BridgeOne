/**
 * @file main.cpp
 * @brief Phase 1.1.2.4: 순번 카운터 검증 로직 구현
 * @details ESP32-S3 UART2를 통한 8바이트 BridgeFrame 수신, 파싱 및 순번 검증
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
// 순번 검증 변수 (Phase 1.1.2.4)
// ============================================================================

/**
 * @brief 순번 검증을 위한 전역 변수
 * @details 프레임 손실 및 중복 감지를 위한 카운터
 * 
 * - lastSeq: 마지막으로 수신한 프레임의 순번 (초기값 255, 첫 프레임 감지용)
 * - frameCount: 정상 수신된 프레임 개수
 * - lostFrames: 손실된 프레임 개수 (순번 불일치로 감지)
 */
uint8_t g_lastSeq = 255;       // 마지막 수신 순번 (255는 초기화 상태)
uint32_t g_frameCount = 0;     // 수신 프레임 카운터
uint32_t g_lostFrames = 0;     // 손실 프레임 카운터

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
// 순번 검증 함수 (Phase 1.1.2.4)
// ============================================================================

/**
 * @brief 수신한 프레임의 순번(seq)을 검증하고 손실 감지
 * @details 순번은 0~255 범위에서 순환하며, 255 다음은 0으로 순환
 * 
 * 검증 과정:
 * 1. 첫 프레임 수신 (g_lastSeq == 255): 초기화 상태로, 순번을 저장하고 통과
 * 2. 정상 순번: (g_lastSeq + 1) % 256 == currentSeq인 경우
 *    - g_lastSeq 업데이트
 *    - g_frameCount 증가
 * 3. 불일치 순번: 예상 순번과 다른 경우
 *    - 손실 프레임 계산 (순환 고려)
 *    - 경고 메시지 출력
 *    - g_lostFrames 증가
 * 
 * @param currentSeq 현재 수신한 프레임의 순번 (0~255)
 * @return true  - 순번 검증 통과 (첫 프레임 또는 정상 순번)
 * @return false - 순번 불일치 (프레임 손실 감지)
 * 
 * @note 255→0 순환을 정확히 처리하기 위해 모듈로 연산 사용
 * @see docs/Board/esp32s3-code-implementation-guide.md §2.1
 */
bool validateSequence(uint8_t currentSeq) {
  // 첫 프레임 수신 (초기화 상태)
  if (g_lastSeq == 255) {
    g_lastSeq = currentSeq;
    g_frameCount++;
    Serial.printf("[SEQ] First frame received: seq=%d\n", currentSeq);
    return true;
  }
  
  // 예상 순번 계산 (0~255 순환 처리)
  uint8_t expectedSeq = (g_lastSeq + 1) % 256;
  
  // 순번 일치: 정상 수신
  if (currentSeq == expectedSeq) {
    g_lastSeq = currentSeq;
    g_frameCount++;
    return true;
  }
  
  // 순번 불일치: 프레임 손실 감지
  // 손실된 프레임 개수 계산 (순환 고려)
  uint32_t lost;
  if (currentSeq > expectedSeq) {
    // 일반적인 경우: currentSeq가 더 큼
    lost = currentSeq - expectedSeq;
  } else {
    // 순환 케이스: 예) expectedSeq=254, currentSeq=2 → 손실=4 (254,255,0,1)
    lost = (256 - expectedSeq) + currentSeq;
  }
  
  g_lostFrames += lost;
  g_lastSeq = currentSeq;  // 현재 순번으로 업데이트 (다음 검증을 위해)
  g_frameCount++;          // 현재 프레임은 수신됨으로 카운트
  
  Serial.printf("[WARN] Sequence mismatch! Expected=%d, Received=%d, Lost=%lu frames\n",
                expectedSeq, currentSeq, lost);
  
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
  Serial.println("  Phase 1.1.2.4: Sequence Validation");
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
 * @details UART2로부터 프레임 수신, 순번 검증 및 통계 출력
 */
void loop() {
  // ========================================
  // 통계 출력 (1초마다)
  // ========================================
  static uint32_t lastStatsTime = 0;  // 마지막 통계 출력 시간 (ms)
  uint32_t currentTime = millis();
  
  // 1초(1000ms) 경과 시 통계 출력
  if (currentTime - lastStatsTime >= 1000) {
    lastStatsTime = currentTime;
    
    // 수신율 계산 (손실률)
    float lossRate = 0.0f;
    if (g_frameCount > 0) {
      lossRate = (g_lostFrames * 100.0f) / (g_frameCount + g_lostFrames);
    }
    
    Serial.println("----------------------------------------");
    Serial.printf("[STATS] Uptime: %lu sec\n", currentTime / 1000);
    Serial.printf("[STATS] Received: %lu frames\n", g_frameCount);
    Serial.printf("[STATS] Lost: %lu frames (%.2f%%)\n", g_lostFrames, lossRate);
    Serial.printf("[STATS] Last Seq: %d\n", g_lastSeq);
    Serial.println("----------------------------------------");
  }
  
  // ========================================
  // UART2 프레임 수신 및 순번 검증
  // ========================================
  if (receiveFrame()) {
    // 순번 검증 (Phase 1.1.2.4)
    bool seqValid = validateSequence(g_rxFrame.seq);
    
    // ========================================
    // 프레임 수신 성공 - 내용 출력
    // ========================================
    Serial.printf("[RX #%lu] seq=%d %s, buttons=0x%02X, delta=(%d,%d), wheel=%d, mods=0x%02X, keys=[0x%02X, 0x%02X]\n",
                  g_frameCount,
                  g_rxFrame.seq,
                  seqValid ? "✓" : "✗",  // 순번 검증 결과 표시
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