/**
 * @file main.cpp
 * @brief Phase 1.1.2.5: FreeRTOS 태스크 구조 구현
 * @details UART 수신 및 디버그 출력을 별도 태스크로 분리
 */

#include <Arduino.h>
#include <freertos/FreeRTOS.h>
#include <freertos/task.h>
#include <freertos/queue.h>

// ============================================================================
// BridgeFrame 구조체 정의
// ============================================================================

/**
 * @brief BridgeOne 프로토콜 프레임 구조체 (8바이트 고정)
 * 
 * Android 앱에서 UART를 통해 전송되는 입력 데이터 프레임입니다.
 * 마우스 이동, 버튼 클릭, 키보드 입력을 하나의 프레임에 담습니다.
 */
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
// 전역 변수
// ============================================================================

// 프레임 수신 버퍼
BridgeFrame g_rxFrame;

// 순번 관리
uint8_t g_lastSeq = 255;

// 통계 카운터
uint32_t g_frameCount = 0;
uint32_t g_lostFrames = 0;

// FreeRTOS 태스크 핸들
TaskHandle_t g_uartRxTaskHandle = NULL;
TaskHandle_t g_debugTaskHandle = NULL;

// 프레임 큐 (향후 HID 전송용)
QueueHandle_t g_frameQueue = NULL;

// ============================================================================
// UART 설정 상수
// ============================================================================

constexpr uint32_t UART_BAUD_RATE = 1000000;  // 1Mbps
constexpr uint8_t UART_RX_PIN = 44;
constexpr uint8_t UART_TX_PIN = 43;

// ============================================================================
// FreeRTOS 태스크 설정 상수
// ============================================================================

constexpr uint32_t UART_RX_TASK_STACK_SIZE = 4096;    // 4KB 스택
constexpr uint32_t DEBUG_TASK_STACK_SIZE = 4096;      // 4KB 스택
constexpr UBaseType_t UART_RX_TASK_PRIORITY = 3;      // 높은 우선순위
constexpr UBaseType_t DEBUG_TASK_PRIORITY = 1;        // 낮은 우선순위
constexpr BaseType_t UART_RX_TASK_CORE = 1;           // Core 1
constexpr BaseType_t DEBUG_TASK_CORE = 0;             // Core 0
constexpr uint32_t FRAME_QUEUE_SIZE = 32;             // 최대 32개 프레임 큐잉

// ============================================================================
// 함수 선언 및 구현
// ============================================================================

/**
 * @brief UART로부터 BridgeFrame 수신 시도
 * 
 * Serial2에 8바이트 이상 데이터가 있으면 프레임을 읽어옵니다.
 * 
 * @return true 프레임 수신 성공
 * @return false 수신할 데이터 부족 또는 수신 실패
 */
bool receiveFrame() {
  if (Serial2.available() >= 8) {
    size_t bytesRead = Serial2.readBytes((uint8_t*)&g_rxFrame, sizeof(BridgeFrame));
    return (bytesRead == 8);
  }
  return false;
}

/**
 * @brief 프레임 순번 검증 및 손실 감지
 * 
 * 순번 카운터(0~255 순환)를 기반으로 프레임 손실을 감지합니다.
 * 첫 프레임은 무조건 유효한 것으로 처리합니다.
 * 
 * @param currentSeq 현재 수신한 프레임의 순번
 * @return true 순번이 정상
 * @return false 순번 불일치 (프레임 손실 발생)
 */
bool validateSequence(uint8_t currentSeq) {
  // 첫 프레임은 무조건 유효
  if (g_lastSeq == 255) {
    g_lastSeq = currentSeq;
    g_frameCount++;
    Serial.printf("[SEQ] First frame: seq=%d\n", currentSeq);
    return true;
  }
  
  // 예상 순번 계산 (0~255 순환)
  uint8_t expectedSeq = (g_lastSeq + 1) % 256;
  
  // 순번 일치
  if (currentSeq == expectedSeq) {
    g_lastSeq = currentSeq;
    g_frameCount++;
    return true;
  }
  
  // 순번 불일치 - 프레임 손실 계산
  uint32_t lost;
  if (currentSeq > expectedSeq) {
    lost = currentSeq - expectedSeq;
  } else {
    // 순환 경계 처리 (255 → 0)
    lost = (256 - expectedSeq) + currentSeq;
  }
  
  g_lostFrames += lost;
  g_lastSeq = currentSeq;
  g_frameCount++;
  
  Serial.printf("[WARN] Sequence mismatch! Expected=%d, Received=%d, Lost=%lu\n",
                expectedSeq, currentSeq, lost);
  
  return false;
}

// ============================================================================
// FreeRTOS 태스크 함수
// ============================================================================

/**
 * @brief UART 수신 태스크
 * 
 * 이 태스크는 UART로부터 BridgeFrame을 수신하고 검증합니다.
 * 향후 HID 전송을 위해 유효한 프레임을 큐에 추가합니다.
 * 
 * 우선순위: 3 (높음) - 실시간 입력 처리를 위해 높은 우선순위 설정
 * 코어: 1 (Core 1에 고정)
 * 
 * @param pvParameters 태스크 파라미터 (미사용)
 */
void uartRxTask(void* pvParameters) {
  Serial.println("[UART_RX_TASK] Started on Core 1");
  
  while (true) {
    // UART 프레임 수신 시도
    if (receiveFrame()) {
      bool seqValid = validateSequence(g_rxFrame.seq);
      
      // 프레임 내용 출력 (디버깅용)
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
      
      // 프레임 큐에 추가 (향후 HID 전송용)
      // 현재는 큐가 NULL이므로 스킵
      if (g_frameQueue != NULL) {
        // 큐가 가득 찼을 경우 대기하지 않고 즉시 드롭
        if (xQueueSend(g_frameQueue, &g_rxFrame, 0) != pdTRUE) {
          Serial.println("[WARN] Frame queue full - frame dropped!");
        }
      }
    }
    
    // 5ms 대기 (약 200Hz 폴링 주기)
    // vTaskDelay는 틱 단위이며, portTICK_PERIOD_MS로 변환
    vTaskDelay(pdMS_TO_TICKS(5));
  }
}

/**
 * @brief 디버그 출력 태스크
 * 
 * 1초마다 시스템 통계 정보를 출력합니다.
 * - 업타임
 * - 수신 프레임 수
 * - 손실 프레임 수 및 손실률
 * - 메모리 사용량
 * 
 * 우선순위: 1 (낮음) - 백그라운드 모니터링 태스크
 * 코어: 0 (Core 0에 고정)
 * 
 * @param pvParameters 태스크 파라미터 (미사용)
 */
void debugTask(void* pvParameters) {
  Serial.println("[DEBUG_TASK] Started on Core 0");
  
  while (true) {
    // 1초마다 통계 정보 출력
    float lossRate = 0.0f;
    if (g_frameCount > 0) {
      lossRate = (g_lostFrames * 100.0f) / (g_frameCount + g_lostFrames);
    }
    
    Serial.println("========================================");
    Serial.printf("[STATS] Uptime: %lu sec\n", millis() / 1000);
    Serial.printf("[STATS] Received: %lu frames\n", g_frameCount);
    Serial.printf("[STATS] Lost: %lu frames (%.2f%%)\n", g_lostFrames, lossRate);
    Serial.printf("[STATS] Last Seq: %d\n", g_lastSeq);
    Serial.printf("[MEMORY] Free Heap: %u bytes\n", ESP.getFreeHeap());
    Serial.println("========================================");
    
    // 1초 대기
    vTaskDelay(pdMS_TO_TICKS(1000));
  }
}

// ============================================================================
// Setup & Loop
// ============================================================================

/**
 * @brief 시스템 초기화 및 FreeRTOS 태스크 생성
 * 
 * Arduino setup() 함수에서는 다음을 수행합니다:
 * 1. USB CDC 디버그 시리얼 초기화
 * 2. UART2 초기화 (1Mbps, 8N1)
 * 3. BridgeFrame 구조체 크기 검증
 * 4. FreeRTOS 태스크 생성 (UART 수신, 디버그)
 */
void setup() {
  // USB CDC 초기화 (디버그용)
  Serial.begin(115200);
  delay(1500);  // USB enumeration 안정화 대기
  
  Serial.println();
  Serial.println("========================================");
  Serial.println("  BridgeOne ESP32-S3 Board");
  Serial.println("  Phase 1.1.2.5: FreeRTOS Task Structure");
  Serial.println("========================================");
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
  Serial2.setRxBufferSize(256);  // 256바이트 수신 버퍼 (약 32프레임)
  Serial2.setTxBufferSize(128);  // 128바이트 송신 버퍼
  Serial2.begin(UART_BAUD_RATE, SERIAL_8N1, UART_RX_PIN, UART_TX_PIN);
  Serial.printf("[UART2] Initialized: %d bps, 8N1\n", UART_BAUD_RATE);
  Serial.printf("[UART2] Pins: RX=GPIO%d, TX=GPIO%d\n", UART_RX_PIN, UART_TX_PIN);
  Serial.println();
  
  // 프레임 큐 생성 (향후 HID 전송용)
  // 현재는 NULL로 설정하여 큐 사용 안 함
  // g_frameQueue = xQueueCreate(FRAME_QUEUE_SIZE, sizeof(BridgeFrame));
  // if (g_frameQueue == NULL) {
  //   Serial.println("[ERROR] Failed to create frame queue!");
  // }
  
  // FreeRTOS 태스크 생성
  Serial.println("[FREERTOS] Creating tasks...");
  
  // UART 수신 태스크 생성 (Core 1, 우선순위 3)
  BaseType_t result1 = xTaskCreatePinnedToCore(
    uartRxTask,                     // 태스크 함수
    "UART_RX",                      // 태스크 이름
    UART_RX_TASK_STACK_SIZE,        // 스택 크기 (바이트)
    NULL,                           // 파라미터
    UART_RX_TASK_PRIORITY,          // 우선순위
    &g_uartRxTaskHandle,            // 태스크 핸들
    UART_RX_TASK_CORE               // 코어 번호
  );
  
  if (result1 == pdPASS) {
    Serial.printf("  ✓ UART_RX task created (Core %d, Priority %d)\n", 
                  UART_RX_TASK_CORE, UART_RX_TASK_PRIORITY);
  } else {
    Serial.println("  ✗ Failed to create UART_RX task!");
  }
  
  // 디버그 태스크 생성 (Core 0, 우선순위 1)
  BaseType_t result2 = xTaskCreatePinnedToCore(
    debugTask,                      // 태스크 함수
    "DEBUG",                        // 태스크 이름
    DEBUG_TASK_STACK_SIZE,          // 스택 크기 (바이트)
    NULL,                           // 파라미터
    DEBUG_TASK_PRIORITY,            // 우선순위
    &g_debugTaskHandle,             // 태스크 핸들
    DEBUG_TASK_CORE                 // 코어 번호
  );
  
  if (result2 == pdPASS) {
    Serial.printf("  ✓ DEBUG task created (Core %d, Priority %d)\n", 
                  DEBUG_TASK_CORE, DEBUG_TASK_PRIORITY);
  } else {
    Serial.println("  ✗ Failed to create DEBUG task!");
  }
  
  Serial.println();
  Serial.println("[INIT] System ready!");
  Serial.println("[INIT] Waiting for Android connection...");
  Serial.println("========================================");
  Serial.println();
}

/**
 * @brief 메인 루프 (FreeRTOS에서는 사용하지 않음)
 * 
 * FreeRTOS를 사용할 때는 loop() 함수를 비우고 vTaskDelete(NULL)로
 * 태스크를 종료합니다. 모든 로직은 별도 태스크에서 실행됩니다.
 */
void loop() {
  // FreeRTOS 태스크를 사용하므로 loop()는 비움
  // loop() 태스크 자체를 삭제
  vTaskDelete(NULL);
}
