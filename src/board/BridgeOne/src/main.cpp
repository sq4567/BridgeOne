/**
 * @file main.cpp
 * @brief Phase 1.1.2.5: FreeRTOS 태스크 구조 구현
 * @details UART 수신 및 디버그 출력을 별도 태스크로 분리한 멀티태스킹 구조
 */

#include <Arduino.h>
#include <freertos/FreeRTOS.h>
#include <freertos/task.h>

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
// FreeRTOS 태스크 핸들 (Phase 1.1.2.5)
// ============================================================================

/**
 * @brief FreeRTOS 태스크 핸들
 * @details UART 수신 태스크와 디버그 태스크를 관리하기 위한 핸들
 * 
 * - uartRxTaskHandle: UART2 수신 및 프레임 처리 태스크
 * - debugTaskHandle: 통계 정보 출력 및 시스템 모니터링 태스크
 */
TaskHandle_t uartRxTaskHandle = NULL;  // UART 수신 태스크 핸들
TaskHandle_t debugTaskHandle = NULL;   // 디버그 출력 태스크 핸들

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

// FreeRTOS 태스크 동기화용 뮤텍스 (통계 변수 보호)
SemaphoreHandle_t g_statsMutex = NULL;

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
// FreeRTOS 태스크 함수 (Phase 1.1.2.5)
// ============================================================================

/**
 * @brief UART 수신 태스크 (Core 1, 우선순위 3)
 * @details UART2로부터 BridgeFrame을 수신하고 순번 검증을 수행
 * 
 * 동작 과정:
 * 1. receiveFrame()을 호출하여 8바이트 프레임 수신 시도
 * 2. 수신 성공 시 validateSequence()로 순번 검증
 * 3. 프레임 내용 출력 (버튼, 이동, 휠, 키 입력)
 * 4. 5ms 대기 후 반복 (200Hz 폴링)
 * 
 * @param pvParameters 태스크 파라미터 (미사용)
 * 
 * @note Core 1에 할당되어 UART 통신 전용으로 동작
 * @note 우선순위 3으로 설정되어 실시간 처리 보장
 * @see receiveFrame(), validateSequence()
 */
void uartRxTask(void* pvParameters) {
  Serial.println("[UART RX Task] Started on Core 1");
  
  // 무한 루프: UART 수신 처리
  for (;;) {
    // UART2로부터 프레임 수신 시도
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
    
    // 5ms 대기 (200Hz 폴링, CPU 부하 감소)
    // vTaskDelay는 다른 태스크에게 CPU 시간을 양보
    vTaskDelay(pdMS_TO_TICKS(5));
  }
}

/**
 * @brief 디버그 출력 태스크 (Core 0, 우선순위 1)
 * @details 1초마다 시스템 통계 정보를 출력
 * 
 * 출력 정보:
 * - Uptime: 시스템 가동 시간 (초)
 * - Received: 수신된 프레임 수
 * - Lost: 손실된 프레임 수 및 손실률 (%)
 * - Last Seq: 마지막 수신 순번
 * - Free Heap: 사용 가능한 힙 메모리 크기
 * - UART Rx Task: UART 수신 태스크 상태 (스택 워터마크)
 * 
 * @param pvParameters 태스크 파라미터 (미사용)
 * 
 * @note Core 0에 할당되어 백그라운드 모니터링 수행
 * @note 우선순위 1로 설정되어 UART 태스크에 영향 최소화
 */
void debugTask(void* pvParameters) {
  Serial.println("[Debug Task] Started on Core 0");
  
  // 무한 루프: 통계 출력
  for (;;) {
    // 1초 대기
    vTaskDelay(pdMS_TO_TICKS(1000));
    
    // ========================================
    // 통계 정보 계산
    // ========================================
    uint32_t currentTime = millis();
    
    // 수신율 계산 (손실률)
    float lossRate = 0.0f;
    if (g_frameCount > 0) {
      lossRate = (g_lostFrames * 100.0f) / (g_frameCount + g_lostFrames);
    }
    
    // 메모리 정보
    uint32_t freeHeap = ESP.getFreeHeap();
    uint32_t minFreeHeap = ESP.getMinFreeHeap();
    
    // 태스크 상태 정보
    UBaseType_t uartRxStackWatermark = 0;
    if (uartRxTaskHandle != NULL) {
      uartRxStackWatermark = uxTaskGetStackHighWaterMark(uartRxTaskHandle);
    }
    
    // ========================================
    // 통계 출력
    // ========================================
    Serial.println("========================================");
    Serial.printf("[STATS] Uptime: %lu sec\n", currentTime / 1000);
    Serial.printf("[STATS] Received: %lu frames\n", g_frameCount);
    Serial.printf("[STATS] Lost: %lu frames (%.2f%%)\n", g_lostFrames, lossRate);
    Serial.printf("[STATS] Last Seq: %d\n", g_lastSeq);
    Serial.println("----------------------------------------");
    Serial.printf("[MEMORY] Free Heap: %lu bytes\n", freeHeap);
    Serial.printf("[MEMORY] Min Free Heap: %lu bytes\n", minFreeHeap);
    Serial.println("----------------------------------------");
    Serial.printf("[TASK] UART Rx Task: Stack Watermark = %u words\n", uartRxStackWatermark);
    Serial.println("========================================");
  }
}

// ============================================================================
// UART 설정 상수
// ============================================================================
// 참고: ESP32-S3 Arduino 프레임워크에는 Serial2가 이미 정의되어 있음
// HardwareSerial Serial2(2)는 HardwareSerial.cpp에서 자동으로 생성됨
// 
// ESP32-S3 DevKitC-1 보드 내장 USB-Serial 사용:
// - 보드의 UART 포트(왼쪽)가 내장 USB-Serial 칩을 통해 GPIO43/44와 연결됨
// - Android 폰에서 OTG 케이블로 보드의 UART 포트에 직접 연결
// - 외부 USB-Serial 어댑터 불필요
constexpr uint32_t UART_BAUD_RATE = 1000000;    // 1Mbps (1,000,000 bps)
constexpr uint8_t UART_RX_PIN = 44;             // GPIO44 (RX) - 보드 내장 UART 포트
constexpr uint8_t UART_TX_PIN = 43;             // GPIO43 (TX) - 보드 내장 UART 포트
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
  Serial.println("  Phase 1.1.2.5: FreeRTOS Task Structure");
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
  // UART2 초기화 (보드 내장 USB-Serial 경로)
  // ========================================
  // 파라미터:
  //   - baud: 1,000,000 bps (1Mbps)
  //   - config: SERIAL_8N1 (8 data bits, No parity, 1 stop bit)
  //   - rxPin: GPIO44 (보드 UART 포트와 연결)
  //   - txPin: GPIO43 (보드 UART 포트와 연결)
  Serial2.begin(UART_BAUD_RATE, SERIAL_8N1, UART_RX_PIN, UART_TX_PIN);
  Serial.printf("[UART2] Initialized: %d bps, 8N1\n", UART_BAUD_RATE);
  Serial.printf("[UART2] Pins: RX=GPIO%d, TX=GPIO%d (Board UART Port)\n", UART_RX_PIN, UART_TX_PIN);
  
  Serial.println();
  Serial.println("[INIT] All systems initialized successfully!");
  Serial.println("========================================");
  Serial.println();
  
  // ========================================
  // FreeRTOS 태스크 생성 (Phase 1.1.2.5)
  // ========================================
  Serial.println("[FreeRTOS] Creating tasks...");
  
  // UART 수신 태스크 생성 (Core 1, 우선순위 3)
  // - Stack Size: 4096 bytes (4KB)
  // - Priority: 3 (높음 - 실시간 UART 처리)
  // - Core: 1 (UART 통신 전용 코어)
  BaseType_t xReturned;
  xReturned = xTaskCreatePinnedToCore(
    uartRxTask,           // 태스크 함수
    "UART_RX",            // 태스크 이름 (디버깅용)
    4096,                 // 스택 크기 (words)
    NULL,                 // 태스크 파라미터
    3,                    // 우선순위 (0=lowest, 24=highest)
    &uartRxTaskHandle,    // 태스크 핸들
    1                     // 코어 ID (Core 1)
  );
  
  if (xReturned == pdPASS) {
    Serial.println("  ✓ UART RX Task created on Core 1 (Priority 3)");
  } else {
    Serial.println("  ✗ Failed to create UART RX Task!");
  }
  
  // 디버그 출력 태스크 생성 (Core 0, 우선순위 1)
  // - Stack Size: 3072 bytes (3KB)
  // - Priority: 1 (낮음 - 백그라운드 모니터링)
  // - Core: 0 (메인 코어, Arduino loop()와 동일)
  xReturned = xTaskCreatePinnedToCore(
    debugTask,            // 태스크 함수
    "DEBUG",              // 태스크 이름 (디버깅용)
    3072,                 // 스택 크기 (words)
    NULL,                 // 태스크 파라미터
    1,                    // 우선순위 (낮음)
    &debugTaskHandle,     // 태스크 핸들
    0                     // 코어 ID (Core 0)
  );
  
  if (xReturned == pdPASS) {
    Serial.println("  ✓ Debug Task created on Core 0 (Priority 1)");
  } else {
    Serial.println("  ✗ Failed to create Debug Task!");
  }
  
  Serial.println("[FreeRTOS] Task creation completed!");
  Serial.println("========================================");
  Serial.println();
}

/**
 * @brief 메인 루프 (사용 안 함)
 * @details FreeRTOS 태스크 구조로 전환되어 loop()는 비활성화됨
 * 
 * FreeRTOS 태스크 구조:
 * - uartRxTask: UART 수신 및 프레임 처리 (Core 1, 우선순위 3)
 * - debugTask: 통계 출력 및 모니터링 (Core 0, 우선순위 1)
 * 
 * @note loop() 대신 FreeRTOS 태스크가 무한 루프로 동작
 * @note vTaskDelete(NULL)로 setup() 태스크 종료
 */
void loop() {
  // FreeRTOS 태스크로 전환되어 loop()는 사용하지 않음
  // setup() 완료 후 이 태스크를 삭제하여 메모리 절약
  vTaskDelete(NULL);
}