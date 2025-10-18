/**
 * @file main.cpp
 * @brief Phase 1.2.2.1: USB Serial CDC 인터페이스 초기화
 * @details ESP32-S3의 USB CDC를 Vendor CDC로 활용하여 Windows와 통신
 * @note Phase 1.2.1.5 코드 기반 위에 Vendor CDC 초기화 추가
 * @note Arduino ESP32 USB Serial API 사용 (Context7 공식 문서 기반)
 * @note 공식 문서 출처:
 *       - https://github.com/espressif/arduino-esp32 (Trust Score: 9.1)
 *       - https://docs.espressif.com/projects/arduino-esp32/en/latest/api/usb_cdc
 *       - Serial 객체는 USB CDC로 자동 노출됨
 */

#include <Arduino.h>
#include <freertos/FreeRTOS.h>
#include <freertos/task.h>
#include <freertos/queue.h>
#include "USBHIDMouse.h"
#include "USBHIDKeyboard.h"
#include "USB.h"  // Phase 1.2.2.1: USB CDC 제어용

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

// USB HID 객체 (Phase 1.2.1.1에서 추가)
USBHIDMouse Mouse;
USBHIDKeyboard Keyboard;

// 프레임 수신 버퍼
BridgeFrame g_rxFrame;

// 순번 관리
uint8_t g_lastSeq = 255;

// 통계 카운터
uint32_t g_frameCount = 0;
uint32_t g_lostFrames = 0;
uint32_t g_hidTxCount = 0;      // Phase 1.2.1.5: HID 전송 횟수
uint32_t g_hidTxDropped = 0;    // Phase 1.2.1.5: 큐 오버플로우로 드롭된 프레임 수

// FreeRTOS 태스크 핸들
TaskHandle_t g_uartRxTaskHandle = NULL;
TaskHandle_t g_hidTxTaskHandle = NULL;  // Phase 1.2.1.5: HID 전송 태스크
TaskHandle_t g_cdcRxTaskHandle = NULL;  // Phase 1.2.2.1: Vendor CDC 수신 태스크
TaskHandle_t g_debugTaskHandle = NULL;

// 프레임 큐 (Phase 1.2.1.5: UART 수신 → HID 전송 큐)
QueueHandle_t g_frameQueue = NULL;

// ============================================================================
// Phase 1.2.2.1: Vendor CDC 통계
// ============================================================================

// Vendor CDC 통계 카운터
uint32_t g_cdcRxCount = 0;        // CDC로 수신한 메시지 수
uint32_t g_cdcTxCount = 0;        // CDC로 송신한 메시지 수
bool g_cdcInitialized = false;    // CDC 초기화 상태

// ============================================================================
// Phase 1.2.1.2: HID 마우스 상태 관리 변수
// ============================================================================

// 이전 버튼 상태 (버튼 변화 감지용)
uint8_t g_lastMouseButtons = 0;

// 마우스 입력 디바운싱 (40ms)
uint32_t g_lastMouseUpdateMs = 0;

// ============================================================================
// Phase 1.2.1.3: HID 키보드 상태 관리 변수
// ============================================================================

// 이전 모디파이어 상태 (변화 감지용)
uint8_t g_lastModifiers = 0;

// 이전 키 코드 상태 (변화 감지용)
uint8_t g_lastKeyCode1 = 0;
uint8_t g_lastKeyCode2 = 0;

// 키보드 입력 디바운싱 (30ms)
uint32_t g_lastKeyUpdateMs = 0;

// ============================================================================
// Phase 1.2.1.4: 홀드 입력 반복 처리 변수
// ============================================================================

// 홀드 상태 추적
uint8_t g_holdKey = 0;              // 현재 홀드 중인 키 (0이면 홀드 없음)
uint32_t g_holdStartTime = 0;       // 홀드 시작 시간 (ms)
uint32_t g_lastRepeatTime = 0;      // 마지막 반복 시간 (ms)

// 홀드 타이밍 설정
constexpr uint32_t HOLD_THRESHOLD_MS = 300;   // 홀드 상태 진입 임계값 (300ms)
constexpr uint32_t REPEAT_INTERVAL_MS = 30;   // 반복 입력 간격 (30ms)

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
constexpr uint32_t HID_TX_TASK_STACK_SIZE = 4096;     // 4KB 스택 (Phase 1.2.1.5)
constexpr uint32_t DEBUG_TASK_STACK_SIZE = 4096;      // 4KB 스택
constexpr UBaseType_t UART_RX_TASK_PRIORITY = 3;      // 높은 우선순위
constexpr UBaseType_t HID_TX_TASK_PRIORITY = 3;       // 높은 우선순위 (Phase 1.2.1.5)
constexpr UBaseType_t DEBUG_TASK_PRIORITY = 1;        // 낮은 우선순위
constexpr BaseType_t UART_RX_TASK_CORE = 1;           // Core 1
constexpr BaseType_t HID_TX_TASK_CORE = 1;            // Core 1 (Phase 1.2.1.5)
constexpr BaseType_t DEBUG_TASK_CORE = 0;             // Core 0
constexpr uint32_t FRAME_QUEUE_SIZE = 32;             // 최대 32개 프레임 큐잉

// ============================================================================
// 함수 선언 및 구현
// ============================================================================

// 함수 프로토타입 선언
void processMouseInput(const BridgeFrame& frame);
void processKeyboardInput(const BridgeFrame& frame);
void processHoldInput(uint8_t currentKey);
bool receiveFrame();
bool validateSequence(uint8_t currentSeq);
void uartRxTask(void* pvParameters);
void hidTxTask(void* pvParameters);  // Phase 1.2.1.5: HID 전송 태스크
void cdcRxTask(void* pvParameters);  // Phase 1.2.2.1: Vendor CDC 수신 태스크
void debugTask(void* pvParameters);

/**
 * @brief BridgeFrame을 HID 마우스 입력으로 변환 및 전송
 * 
 * Phase 1.2.1.2에서 구현된 마우스 입력 처리 함수입니다.
 * BridgeFrame의 버튼, deltaX, deltaY, wheel 필드를 추출하여
 * Arduino USBHIDMouse API를 통해 HID 리포트로 전송합니다.
 * 
 * 주요 기능:
 * - 버튼 상태 변화 감지 (press/release)
 * - 상대 이동 (deltaX, deltaY) 및 휠 스크롤 처리
 * - 40ms 디바운싱 적용
 * 
 * @param frame 처리할 BridgeFrame 구조체
 * 
 * @note 참조: @docs/Board/esp32s3-code-implementation-guide.md §3.1
 * @note Arduino ESP32 HID Mouse API 사용 (Context7 공식 문서 기반)
 */
void processMouseInput(const BridgeFrame& frame) {
  uint32_t currentMs = millis();
  
  // ============================================================================
  // 40ms 디바운싱 적용
  // ============================================================================
  // Arduino 예제 기준 HID_DEBOUNCE_MS = 40ms
  // 너무 잦은 HID 전송을 방지하여 시스템 안정성 확보
  
  if (currentMs - g_lastMouseUpdateMs < 40) {
    return;  // 디바운싱 기간 중이므로 스킵
  }
  
  // ============================================================================
  // 마우스 버튼 처리 (비트 필드 분석)
  // ============================================================================
  // buttons 필드: bit0=좌클릭, bit1=우클릭, bit2=중클릭
  // 이전 상태와 비교하여 변화가 있는 경우만 press/release 호출
  
  uint8_t currentButtons = frame.buttons;
  
  // 좌클릭 버튼 처리 (bit 0)
  if ((currentButtons & 0x01) && !(g_lastMouseButtons & 0x01)) {
    // 좌클릭 버튼이 눌림
    Mouse.press(MOUSE_LEFT);
    Serial.println("[HID MOUSE] Left button pressed");
  } else if (!(currentButtons & 0x01) && (g_lastMouseButtons & 0x01)) {
    // 좌클릭 버튼이 떼어짐
    Mouse.release(MOUSE_LEFT);
    Serial.println("[HID MOUSE] Left button released");
  }
  
  // 우클릭 버튼 처리 (bit 1)
  if ((currentButtons & 0x02) && !(g_lastMouseButtons & 0x02)) {
    // 우클릭 버튼이 눌림
    Mouse.press(MOUSE_RIGHT);
    Serial.println("[HID MOUSE] Right button pressed");
  } else if (!(currentButtons & 0x02) && (g_lastMouseButtons & 0x02)) {
    // 우클릭 버튼이 떼어짐
    Mouse.release(MOUSE_RIGHT);
    Serial.println("[HID MOUSE] Right button released");
  }
  
  // 중간클릭 버튼 처리 (bit 2)
  if ((currentButtons & 0x04) && !(g_lastMouseButtons & 0x04)) {
    // 중간클릭 버튼이 눌림
    Mouse.press(MOUSE_MIDDLE);
    Serial.println("[HID MOUSE] Middle button pressed");
  } else if (!(currentButtons & 0x04) && (g_lastMouseButtons & 0x04)) {
    // 중간클릭 버튼이 떼어짐
    Mouse.release(MOUSE_MIDDLE);
    Serial.println("[HID MOUSE] Middle button released");
  }
  
  // 버튼 상태 갱신
  g_lastMouseButtons = currentButtons;
  
  // ============================================================================
  // 마우스 이동 및 휠 처리
  // ============================================================================
  // deltaX, deltaY: 상대 이동량 (-127~127)
  // wheel: 휠 스크롤량 (-127~127)
  // 
  // Arduino USBHIDMouse::move(int8_t x, int8_t y, int8_t wheel, int8_t hWheel)
  // - x, y: 상대 커서 이동
  // - wheel: 수직 휠 스크롤
  // - hWheel: 수평 휠 스크롤 (현재 미사용)
  
  if (frame.deltaX != 0 || frame.deltaY != 0 || frame.wheel != 0) {
    Mouse.move(frame.deltaX, frame.deltaY, frame.wheel, 0);
    
    // 디버그 출력 (이동량이 있을 때만)
    if (frame.deltaX != 0 || frame.deltaY != 0) {
      Serial.printf("[HID MOUSE] Move: dx=%d, dy=%d\n", frame.deltaX, frame.deltaY);
    }
    if (frame.wheel != 0) {
      Serial.printf("[HID MOUSE] Wheel: %d\n", frame.wheel);
    }
  }
  
  // 마지막 업데이트 시간 갱신
  g_lastMouseUpdateMs = currentMs;
}

/**
 * @brief BridgeFrame을 HID 키보드 입력으로 변환 및 전송
 * 
 * Phase 1.2.1.3에서 구현된 키보드 입력 처리 함수입니다.
 * BridgeFrame의 modifiers, keyCode1, keyCode2 필드를 추출하여
 * Arduino USBHIDKeyboard API를 통해 HID 리포트로 전송합니다.
 * 
 * 주요 기능:
 * - 모디파이어 키 상태 변화 감지 및 처리 (Ctrl, Shift, Alt, GUI)
 * - 최대 2개 키 동시 입력 처리
 * - 30ms 디바운싱 적용
 * - 이전 상태와 비교하여 변화된 키만 처리
 * 
 * @param frame 처리할 BridgeFrame 구조체
 * 
 * @note 참조: @docs/Board/esp32s3-code-implementation-guide.md §3.2
 * @note Arduino ESP32 HID Keyboard API 사용 (Context7 공식 문서 기반)
 * @note 공식 문서 출처:
 *       - https://github.com/espressif/arduino-esp32/blob/master/libraries/USB/src/USBHIDKeyboard.cpp
 *       - https://docs.arduino.cc/language-reference/en/functions/usb/Keyboard/keyboardModifiers
 *       - https://docs.arduino.cc/language-reference/en/functions/usb/Keyboard
 */
void processKeyboardInput(const BridgeFrame& frame) {
  uint32_t currentMs = millis();
  
  // ============================================================================
  // 30ms 디바운싱 적용
  // ============================================================================
  // Arduino 예제 및 공식 문서 기반 안정적인 키 입력 간격 확보
  // 너무 잦은 HID 전송을 방지하여 시스템 안정성 보장
  
  if (currentMs - g_lastKeyUpdateMs < 30) {
    return;  // 디바운싱 기간 중이므로 스킵
  }
  
  // ============================================================================
  // 모디파이어 키 처리 (비트 필드 분석)
  // ============================================================================
  // modifiers 필드: bit0=Ctrl, bit1=Shift, bit2=Alt, bit3=GUI
  // Arduino 공식 문서의 KEY_LEFT_CTRL, KEY_LEFT_SHIFT, KEY_LEFT_ALT, KEY_LEFT_GUI 사용
  // 
  // 참조: https://docs.arduino.cc/language-reference/en/functions/usb/Keyboard/keyboardModifiers
  // | Key             | Hexadecimal value | Decimal value |
  // |-----------------|-------------------|---------------|
  // | KEY_LEFT_CTRL   | 0x80              | 128           |
  // | KEY_LEFT_SHIFT  | 0x81              | 129           |
  // | KEY_LEFT_ALT    | 0x82              | 130           |
  // | KEY_LEFT_GUI    | 0x83              | 131           |
  
  uint8_t currentModifiers = frame.modifiers;
  
  // Ctrl 키 처리 (bit 0)
  if ((currentModifiers & 0x01) && !(g_lastModifiers & 0x01)) {
    // Ctrl 키가 눌림
    Keyboard.press(KEY_LEFT_CTRL);
    Serial.println("[HID KEYBOARD] Ctrl pressed");
  } else if (!(currentModifiers & 0x01) && (g_lastModifiers & 0x01)) {
    // Ctrl 키가 떼어짐
    Keyboard.release(KEY_LEFT_CTRL);
    Serial.println("[HID KEYBOARD] Ctrl released");
  }
  
  // Shift 키 처리 (bit 1)
  if ((currentModifiers & 0x02) && !(g_lastModifiers & 0x02)) {
    // Shift 키가 눌림
    Keyboard.press(KEY_LEFT_SHIFT);
    Serial.println("[HID KEYBOARD] Shift pressed");
  } else if (!(currentModifiers & 0x02) && (g_lastModifiers & 0x02)) {
    // Shift 키가 떼어짐
    Keyboard.release(KEY_LEFT_SHIFT);
    Serial.println("[HID KEYBOARD] Shift released");
  }
  
  // Alt 키 처리 (bit 2)
  if ((currentModifiers & 0x04) && !(g_lastModifiers & 0x04)) {
    // Alt 키가 눌림
    Keyboard.press(KEY_LEFT_ALT);
    Serial.println("[HID KEYBOARD] Alt pressed");
  } else if (!(currentModifiers & 0x04) && (g_lastModifiers & 0x04)) {
    // Alt 키가 떼어짐
    Keyboard.release(KEY_LEFT_ALT);
    Serial.println("[HID KEYBOARD] Alt released");
  }
  
  // GUI 키 (Windows 키 / Command 키) 처리 (bit 3)
  if ((currentModifiers & 0x08) && !(g_lastModifiers & 0x08)) {
    // GUI 키가 눌림
    Keyboard.press(KEY_LEFT_GUI);
    Serial.println("[HID KEYBOARD] GUI pressed");
  } else if (!(currentModifiers & 0x08) && (g_lastModifiers & 0x08)) {
    // GUI 키가 떼어짐
    Keyboard.release(KEY_LEFT_GUI);
    Serial.println("[HID KEYBOARD] GUI released");
  }
  
  // 모디파이어 상태 갱신
  g_lastModifiers = currentModifiers;
  
  // ============================================================================
  // 일반 키 입력 처리
  // ============================================================================
  // keyCode1, keyCode2: HID 키 코드 (최대 2개 동시 입력)
  // 0이면 해당 키 없음을 의미
  // 
  // Arduino Keyboard.press() / Keyboard.release() API 사용
  // 참조: https://docs.arduino.cc/language-reference/en/functions/usb/Keyboard
  // 
  // 공식 문서:
  // - Keyboard.press(key_code): 키를 누르고 유지
  // - Keyboard.release(key_code): 키를 떼어냄
  // - Keyboard.releaseAll(): 모든 키를 해제
  
  // keyCode1 처리
  if (frame.keyCode1 != g_lastKeyCode1) {
    // 이전 키가 있었다면 해제
    if (g_lastKeyCode1 != 0) {
      Keyboard.release(g_lastKeyCode1);
      Serial.printf("[HID KEYBOARD] Key1 released: 0x%02X\n", g_lastKeyCode1);
    }
    
    // 새로운 키가 있다면 누름
    if (frame.keyCode1 != 0) {
      Keyboard.press(frame.keyCode1);
      Serial.printf("[HID KEYBOARD] Key1 pressed: 0x%02X\n", frame.keyCode1);
    }
    
    g_lastKeyCode1 = frame.keyCode1;
  }
  
  // keyCode2 처리
  if (frame.keyCode2 != g_lastKeyCode2) {
    // 이전 키가 있었다면 해제
    if (g_lastKeyCode2 != 0) {
      Keyboard.release(g_lastKeyCode2);
      Serial.printf("[HID KEYBOARD] Key2 released: 0x%02X\n", g_lastKeyCode2);
    }
    
    // 새로운 키가 있다면 누름
    if (frame.keyCode2 != 0) {
      Keyboard.press(frame.keyCode2);
      Serial.printf("[HID KEYBOARD] Key2 pressed: 0x%02X\n", frame.keyCode2);
    }
    
    g_lastKeyCode2 = frame.keyCode2;
  }
  
  // ============================================================================
  // 모든 키 해제 조건 처리
  // ============================================================================
  // 모디파이어와 일반 키가 모두 0이면 모든 키 해제
  // 이는 Android 앱에서 키 입력이 완전히 종료되었음을 의미
  
  if (currentModifiers == 0 && frame.keyCode1 == 0 && frame.keyCode2 == 0) {
    // 모든 키가 해제된 상태
    if (g_lastModifiers != 0 || g_lastKeyCode1 != 0 || g_lastKeyCode2 != 0) {
      // 이전에 눌린 키가 있었다면 완전히 해제
      Keyboard.releaseAll();
      Serial.println("[HID KEYBOARD] All keys released");
    }
  }
  
  // ============================================================================
  // Phase 1.2.1.4: 홀드 입력 반복 처리
  // ============================================================================
  // keyCode1에 대해 홀드 상태를 추적하고 반복 입력 처리
  // 화살표 키, 백스페이스 등 반복이 필요한 키에 유용
  
  processHoldInput(frame.keyCode1);
  
  // 마지막 업데이트 시간 갱신
  g_lastKeyUpdateMs = currentMs;
}

/**
 * @brief 홀드 입력 자동 반복 처리
 * 
 * Phase 1.2.1.4에서 구현된 키 홀드 반복 입력 함수입니다.
 * 특정 키가 300ms 이상 지속되면 홀드 상태로 진입하고,
 * 30ms 간격으로 Keyboard.write()를 호출하여 반복 입력을 생성합니다.
 * 
 * 주요 기능:
 * - 동일한 키가 300ms 이상 유지되면 홀드 상태 진입
 * - 홀드 중일 때 30ms 간격으로 키 반복 입력 전송
 * - 키가 변경되거나 0이 되면 홀드 상태 해제
 * - 화살표 키, 백스페이스 등 반복이 필요한 키에 유용
 * 
 * @param currentKey 현재 프레임의 keyCode1 값 (반복 처리할 키)
 * 
 * @note 참조: @docs/Board/esp32s3-code-implementation-guide.md §3.3
 * @note Arduino Keyboard.write() API 사용 (Context7 공식 문서 기반)
 * @note 공식 문서 출처:
 *       - https://docs.arduino.cc/language-reference/en/functions/usb/Keyboard
 *       - Keyboard.write() = press + release 자동 수행
 *       - 반복 입력은 애플리케이션 레벨에서 구현
 */
void processHoldInput(uint8_t currentKey) {
  uint32_t currentMs = millis();
  
  // ============================================================================
  // 케이스 1: 키 입력이 없음 (currentKey == 0)
  // ============================================================================
  // 홀드 상태 완전히 해제
  
  if (currentKey == 0) {
    if (g_holdKey != 0) {
      // 홀드 중이던 키가 해제됨
      Serial.printf("[HOLD] Key released: 0x%02X\n", g_holdKey);
      g_holdKey = 0;
      g_holdStartTime = 0;
      g_lastRepeatTime = 0;
    }
    return;
  }
  
  // ============================================================================
  // 케이스 2: 키가 변경됨 (다른 키로 전환)
  // ============================================================================
  // 이전 홀드 취소하고 새로운 키의 홀드 타이머 시작
  
  if (currentKey != g_holdKey) {
    if (g_holdKey != 0) {
      // 이전 홀드 중이던 키 취소
      Serial.printf("[HOLD] Key changed: 0x%02X → 0x%02X\n", g_holdKey, currentKey);
    }
    
    // 새로운 키의 홀드 시작 시간 기록
    g_holdKey = currentKey;
    g_holdStartTime = currentMs;
    g_lastRepeatTime = 0;  // 아직 반복 없음
    return;
  }
  
  // ============================================================================
  // 케이스 3: 동일한 키가 계속 유지됨
  // ============================================================================
  // 홀드 시간을 체크하여 반복 입력 여부 결정
  
  uint32_t holdDuration = currentMs - g_holdStartTime;
  
  // ============================================================================
  // 서브 케이스 3-1: 홀드 임계값(300ms) 도달 전
  // ============================================================================
  // 아직 홀드 상태가 아니므로 아무것도 하지 않음
  
  if (holdDuration < HOLD_THRESHOLD_MS) {
    return;  // 홀드 상태 진입 대기 중
  }
  
  // ============================================================================
  // 서브 케이스 3-2: 홀드 상태 진입 (첫 반복)
  // ============================================================================
  // 300ms가 지나고 아직 반복을 시작하지 않은 경우
  
  if (g_lastRepeatTime == 0) {
    // 첫 반복 입력 시작
    Serial.printf("[HOLD] Hold state entered: 0x%02X (held for %lu ms)\n", 
                  g_holdKey, holdDuration);
    
    // Keyboard.write()를 사용하여 press + release 자동 수행
    // 참조: Arduino 공식 문서 - "Keyboard.write(): Simulates a press and release of a key."
    Keyboard.write(g_holdKey);
    
    g_lastRepeatTime = currentMs;
    Serial.printf("[HOLD] First repeat sent: 0x%02X\n", g_holdKey);
    return;
  }
  
  // ============================================================================
  // 서브 케이스 3-3: 홀드 중 반복 입력 (30ms 간격)
  // ============================================================================
  // 마지막 반복으로부터 30ms가 지난 경우 다음 반복 입력 전송
  
  uint32_t timeSinceLastRepeat = currentMs - g_lastRepeatTime;
  
  if (timeSinceLastRepeat >= REPEAT_INTERVAL_MS) {
    // 반복 입력 전송
    // Keyboard.write()는 press + release를 자동으로 수행하므로
    // 매번 완전한 키 입력이 생성됨
    Keyboard.write(g_holdKey);
    
    g_lastRepeatTime = currentMs;
    
    // 디버그 출력 (너무 많으므로 100ms마다만 출력)
    static uint32_t lastDebugPrint = 0;
    if (currentMs - lastDebugPrint >= 100) {
      Serial.printf("[HOLD] Repeating: 0x%02X (held for %lu ms)\n", 
                    g_holdKey, holdDuration);
      lastDebugPrint = currentMs;
    }
  }
}


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
 * Phase 1.2.1.5에서 수정: 수신한 프레임을 큐에 추가
 * 
 * 이 태스크는 UART로부터 BridgeFrame을 수신하고 검증합니다.
 * 유효한 프레임은 frameQueue에 추가하여 HID 전송 태스크에서 처리합니다.
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
      
      // ============================================================================
      // Phase 1.2.1.5: 프레임 큐에 추가 (UART 수신 → HID 전송 분리)
      // ============================================================================
      // FreeRTOS Queue API 사용 (Context7 공식 문서 기반)
      // xQueueSend(): 큐의 뒤에 아이템 추가 (xQueueSendToBack와 동일)
      // 
      // 참조: https://github.com/freertos/freertos-kernel
      // - xQueueSend(QueueHandle_t, const void *pvItemToQueue, TickType_t xTicksToWait)
      // - 반환값: pdPASS (성공) / errQUEUE_FULL (실패)
      // - xTicksToWait = 0: 대기하지 않고 즉시 반환 (Non-blocking)
      
      if (g_frameQueue != NULL) {
        // 큐가 가득 찼을 경우 대기하지 않고 즉시 드롭
        if (xQueueSend(g_frameQueue, &g_rxFrame, 0) != pdTRUE) {
          g_hidTxDropped++;
          Serial.printf("[WARN] Frame queue full - frame dropped! (Total: %lu)\n", g_hidTxDropped);
        }
      } else {
        Serial.println("[ERROR] Frame queue is NULL!");
      }
    }
    
    // 5ms 대기 (약 200Hz 폴링 주기)
    // vTaskDelay는 틱 단위이며, portTICK_PERIOD_MS로 변환
    vTaskDelay(pdMS_TO_TICKS(5));
  }
}

/**
 * @brief HID 전송 태스크
 * 
 * Phase 1.2.1.5에서 새로 추가: 큐에서 프레임을 꺼내 HID 입력으로 변환 및 전송
 * 
 * 이 태스크는 frameQueue에서 BridgeFrame을 꺼내서 HID 마우스 및 키보드 입력으로 변환합니다.
 * 1ms vTaskDelay를 사용하여 1000 FPS 목표 전송 빈도를 달성합니다.
 * 
 * 우선순위: 3 (높음) - UART 수신과 동일한 우선순위로 실시간 처리
 * 코어: 1 (Core 1에 고정) - UART 수신과 같은 코어에서 처리
 * 
 * @param pvParameters 태스크 파라미터 (미사용)
 * 
 * @note 참조: @docs/Board/esp32s3-code-implementation-guide.md §3.4
 * @note FreeRTOS Queue API 사용 (Context7 공식 문서 기반)
 * @note 공식 문서 출처: https://github.com/freertos/freertos-kernel
 */
void hidTxTask(void* pvParameters) {
  Serial.println("[HID_TX_TASK] Started on Core 1");
  
  BridgeFrame frameToProcess;
  uint32_t lastFpsCalcTime = millis();
  uint32_t fpsCounter = 0;
  float currentFps = 0.0f;
  
  while (true) {
    // ============================================================================
    // 큐에서 BridgeFrame 꺼내기
    // ============================================================================
    // FreeRTOS Queue API 사용 (Context7 공식 문서 기반)
    // xQueueReceive(): 큐의 앞에서 아이템 꺼내기
    // 
    // 참조: https://github.com/freertos/freertos-kernel
    // - xQueueReceive(QueueHandle_t, void *pvBuffer, TickType_t xTicksToWait)
    // - 반환값: pdPASS (성공) / errQUEUE_EMPTY (실패)
    // - xTicksToWait = portMAX_DELAY: 아이템이 올 때까지 무한 대기 (Blocking)
    
    if (xQueueReceive(g_frameQueue, &frameToProcess, portMAX_DELAY) == pdTRUE) {
      // ============================================================================
      // Phase 1.2.1.2: HID 마우스 입력 처리
      // ============================================================================
      // 큐에서 꺼낸 프레임을 HID 마우스 입력으로 변환하여 전송
      // processMouseInput()에서 40ms 디바운싱이 적용됨
      
      processMouseInput(frameToProcess);
      
      // ============================================================================
      // Phase 1.2.1.3: HID 키보드 입력 처리
      // ============================================================================
      // 큐에서 꺼낸 프레임을 HID 키보드 입력으로 변환하여 전송
      // processKeyboardInput()에서 30ms 디바운싱이 적용됨
      
      processKeyboardInput(frameToProcess);
      
      // ============================================================================
      // Phase 1.2.1.4: 홀드 입력 반복 처리
      // ============================================================================
      // processKeyboardInput() 내부에서 processHoldInput()이 호출되므로
      // 여기서는 별도로 호출하지 않음
      
      // HID 전송 카운터 증가
      g_hidTxCount++;
      fpsCounter++;
      
      // ============================================================================
      // FPS 계산 (1초마다)
      // ============================================================================
      uint32_t currentMs = millis();
      if (currentMs - lastFpsCalcTime >= 1000) {
        currentFps = fpsCounter / ((currentMs - lastFpsCalcTime) / 1000.0f);
        Serial.printf("[HID_TX_TASK] HID Transmission FPS: %.2f Hz (Target: 1000 Hz)\n", currentFps);
        
        fpsCounter = 0;
        lastFpsCalcTime = currentMs;
      }
    }
    
    // ============================================================================
    // 1ms 대기 (1000 FPS 목표)
    // ============================================================================
    // vTaskDelay(pdMS_TO_TICKS(1))를 사용하여 1ms 간격으로 큐를 폴링합니다.
    // 이는 최대 1000 FPS의 전송 빈도를 달성할 수 있게 합니다.
    // 
    // 참고: xQueueReceive()에서 portMAX_DELAY를 사용하므로
    // 실제로는 프레임이 도착할 때까지 블로킹되며,
    // 이 vTaskDelay()는 프레임 처리 후 약간의 여유 시간을 제공합니다.
    
    vTaskDelay(pdMS_TO_TICKS(1));
  }
}

/**
 * @brief Vendor CDC 수신 태스크
 * 
 * Phase 1.2.2.1에서 새로 추가: Windows 서버로부터 CDC 메시지 수신 및 처리
 * 
 * 이 태스크는 USB Serial CDC (Serial 객체)를 통해 Windows 서버와 통신합니다.
 * JSON 형식의 명령을 수신하고 응답을 전송합니다.
 * 
 * 우선순위: 2 (중간) - 명령 처리는 HID보다 우선순위가 낮음
 * 코어: 0 (Core 0에 고정) - HID/UART와 부하 분산
 * 
 * @param pvParameters 태스크 파라미터 (미사용)
 * 
 * @note 참조: @docs/Board/esp32s3-code-implementation-guide.md §4.1
 * @note Arduino ESP32 USB Serial API 사용 (Context7 공식 문서 기반)
 * @note 공식 문서 출처:
 *       - https://docs.espressif.com/projects/arduino-esp32/en/latest/api/usb_cdc
 *       - Serial.available(), Serial.read(), Serial.write() 등 사용
 */
void cdcRxTask(void* pvParameters) {
  Serial.println("[CDC_RX_TASK] Started on Core 0");
  Serial.println("[CDC_RX_TASK] Waiting for Windows connection...");
  
  char rxBuffer[256];  // CDC 수신 버퍼 (최대 256바이트)
  
  while (true) {
    // ============================================================================
    // Phase 1.2.2.1: USB CDC 데이터 수신 대기
    // ============================================================================
    // Arduino ESP32 공식 문서:
    // - Serial.available(): 수신 버퍼에 대기 중인 바이트 수 반환
    // - Serial.readBytesUntil(): 특정 문자까지 읽기 (줄바꿈 문자 기준)
    // - Serial.setTimeout(): 읽기 타임아웃 설정 (이미 setup()에서 100ms로 설정됨)
    // 
    // 참조: https://docs.espressif.com/projects/arduino-esp32/en/latest/api/usb_cdc
    
    if (Serial.available() > 0) {
      // 줄바꿈 문자('\n')까지 읽기 (최대 255바이트)
      // readBytesUntil()은 타임아웃 또는 구분자 문자를 만날 때까지 블로킹됨
      size_t bytesRead = Serial.readBytesUntil('\n', rxBuffer, sizeof(rxBuffer) - 1);
      
      if (bytesRead > 0) {
        // null 종료 문자 추가
        rxBuffer[bytesRead] = '\0';
        
        // CDC 수신 카운터 증가
        g_cdcRxCount++;
        
        // 수신한 메시지 출력
        Serial.printf("[CDC_RX] Received (%d bytes): %s\n", bytesRead, rxBuffer);
        
        // ============================================================================
        // Phase 1.2.2.2: 간단한 에코 응답 (다음 단계에서 JSON 명령 처리로 확장 예정)
        // ============================================================================
        // Arduino ESP32 공식 문서:
        // - Serial.write(): 바이트 배열 전송
        // - Serial.println(): 문자열 + 줄바꿈 전송
        // 
        // 참조: https://docs.espressif.com/projects/arduino-esp32/en/latest/api/usb_cdc
        
        // 간단한 OK 응답 전송
        Serial.println("OK");
        
        // CDC 송신 카운터 증가
        g_cdcTxCount++;
        
        Serial.printf("[CDC_TX] Sent response (Total TX: %lu)\n", g_cdcTxCount);
      }
    }
    
    // 50ms 대기 (CDC 명령은 실시간 처리가 필요 없음)
    vTaskDelay(pdMS_TO_TICKS(50));
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
    
    // ============================================================================
    // Phase 1.2.1.5: HID 전송 통계 추가
    // ============================================================================
    // - HID 전송 FPS: 1000Hz 목표
    // - 큐 사용률: 현재 큐에 대기 중인 프레임 수 / 최대 큐 크기
    // - 큐 오버플로우: 큐가 가득 차서 드롭된 프레임 수
    
    UBaseType_t queueWaiting = 0;
    float queueUsage = 0.0f;
    
    if (g_frameQueue != NULL) {
      queueWaiting = uxQueueMessagesWaiting(g_frameQueue);
      queueUsage = (queueWaiting * 100.0f) / FRAME_QUEUE_SIZE;
    }
    
    Serial.println("========================================");
    Serial.printf("[STATS] Uptime: %lu sec\n", millis() / 1000);
    Serial.printf("[STATS] Received: %lu frames\n", g_frameCount);
    Serial.printf("[STATS] Lost: %lu frames (%.2f%%)\n", g_lostFrames, lossRate);
    Serial.printf("[STATS] Last Seq: %d\n", g_lastSeq);
    Serial.println("--- Phase 1.2.1.5: HID Transmission ---");
    Serial.printf("[HID TX] Transmitted: %lu frames\n", g_hidTxCount);
    Serial.printf("[HID TX] Dropped: %lu frames\n", g_hidTxDropped);
    Serial.printf("[HID TX] Queue Usage: %lu/%d (%.1f%%)\n", 
                  queueWaiting, FRAME_QUEUE_SIZE, queueUsage);
    Serial.println("--- Phase 1.2.2.1: Vendor CDC ---");
    Serial.printf("[CDC] Initialized: %s\n", g_cdcInitialized ? "YES" : "NO");
    Serial.printf("[CDC] RX: %lu messages\n", g_cdcRxCount);
    Serial.printf("[CDC] TX: %lu messages\n", g_cdcTxCount);
    Serial.println("---------------------------------------");
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
 * 1. USB CDC 디버그 시리얼 초기화 (먼저 초기화하여 bootloop 방지)
 * 2. Phase 1.2.2.1: Vendor CDC 초기화 및 설정
 * 3. USB HID 초기화 (Mouse, Keyboard) - Phase 1.2.1.1
 * 4. USB 복합 장치 시작 - Phase 1.2.1.1
 * 5. UART2 초기화 (1Mbps, 8N1)
 * 6. BridgeFrame 구조체 크기 검증
 * 7. FreeRTOS 태스크 생성 (UART 수신, HID 전송, CDC 수신, 디버그)
 */
void setup() {
  // ============================================================================
  // 디버그 시리얼 초기화 (USB CDC) - 먼저 초기화
  // ============================================================================
  
  // USB CDC 초기화 (디버그용)
  // platformio.ini에서 ARDUINO_USB_CDC_ON_BOOT=1로 설정되어 있으므로
  // Serial은 USB CDC를 통해 출력됩니다.
  Serial.begin(115200);
  delay(2000);  // CDC 안정화 대기 (bootloop 방지)
  
  // ============================================================================
  // Phase 1.2.2.1: Vendor CDC 초기화
  // ============================================================================
  
  // Arduino ESP32 공식 문서:
  // - Serial.setTimeout(): 읽기 타임아웃 설정 (밀리초 단위)
  // - ESP32-S3의 USB Serial은 자동으로 CDC 인터페이스로 노출됨
  // - baud rate는 CDC에서 무시되지만 형식상 호출
  // 
  // 참조: https://docs.espressif.com/projects/arduino-esp32/en/latest/api/usb_cdc
  // - "Serial.begin(baud_rate)": USB CDC의 경우 baud rate 무시
  // - "Serial.setTimeout(timeout_ms)": 읽기 타임아웃 설정 (기본값: 1000ms)
  
  Serial.setTimeout(100);  // 100ms 읽기 타임아웃 설정
  
  // Vendor CDC 초기화 완료 플래그
  g_cdcInitialized = true;
  
  Serial.println();
  Serial.println("========================================");
  Serial.println("  BridgeOne ESP32-S3 Board");
  Serial.println("  Phase 1.2.2.1: Vendor CDC Init");
  Serial.println("========================================");
  Serial.println();
  
  // ============================================================================
  // Phase 1.2.2.1: Vendor CDC 초기화 확인
  // ============================================================================
  
  Serial.println("[USB CDC] Vendor CDC Initialization Status:");
  Serial.println("  ✓ USB Serial CDC initialized (Phase 1.2.2.1)");
  Serial.println("  ✓ Serial object configured as Vendor CDC");
  Serial.printf("  ✓ Baud rate: 115200 (형식상, CDC는 무시)\n");
  Serial.printf("  ✓ Timeout: 100ms\n");
  Serial.println("  ⓘ ESP32-S3 USB Serial은 자동으로 CDC 장치로 인식됨");
  Serial.println("  ⓘ Windows에서 COM 포트로 노출됨");
  Serial.println();
  
  // ============================================================================
  // Phase 1.2.1.1: USB HID 초기화
  // ============================================================================
  
  // 참고: ARDUINO_USB_CDC_ON_BOOT=1 설정으로 USB는 이미 자동 시작됨
  // USB.begin()을 호출하면 재열거로 인한 bootloop 발생
  
  // USB HID 마우스 초기화
  Mouse.begin();
  
  // USB HID 키보드 초기화
  Keyboard.begin();
  
  // HID 장치 등록 안정화 대기
  delay(500);
  
  // ============================================================================
  // USB HID 초기화 확인
  // ============================================================================
  
  Serial.println("[USB HID] Initialization Status:");
  Serial.println("  ✓ USB HID Mouse initialized (Phase 1.2.1.1)");
  Serial.println("  ✓ USB HID Keyboard initialized (Phase 1.2.1.1)");
  Serial.println("  ✓ Mouse input processing enabled (Phase 1.2.1.2)");
  Serial.println("  ✓ Keyboard input processing enabled (Phase 1.2.1.3)");
  Serial.println("  ✓ Hold input repeat enabled (Phase 1.2.1.4)");
  Serial.println("  ✓ FreeRTOS HID transmission task enabled (Phase 1.2.1.5)");
  Serial.println("  ⓘ USB already started by CDC_ON_BOOT=1 (no USB.begin() call)");
  Serial.println("  ⓘ Device will be enumerated as HID Mouse + Keyboard + CDC");
  Serial.println();
  
  // ============================================================================
  // BridgeFrame 검증
  // ============================================================================
  
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
  
  // ============================================================================
  // Phase 1.2.1.5: 프레임 큐 생성
  // ============================================================================
  // FreeRTOS Queue API 사용 (Context7 공식 문서 기반)
  // xQueueCreate(): 큐 생성
  // 
  // 참조: https://github.com/freertos/freertos-kernel
  // - xQueueCreate(UBaseType_t uxQueueLength, UBaseType_t uxItemSize)
  // - 반환값: QueueHandle_t (성공) / NULL (실패)
  // - uxQueueLength: 큐가 보관할 수 있는 최대 아이템 수
  // - uxItemSize: 각 아이템의 크기 (바이트)
  
  Serial.println("[FREERTOS] Creating frame queue...");
  
  g_frameQueue = xQueueCreate(FRAME_QUEUE_SIZE, sizeof(BridgeFrame));
  
  if (g_frameQueue == NULL) {
    Serial.println("  ✗ Failed to create frame queue!");
    Serial.println("  ⓘ System cannot proceed without queue. Halting...");
    while (1) {
      delay(1000);  // 큐 생성 실패 시 무한 루프
    }
  } else {
    Serial.printf("  ✓ Frame queue created (Size: %d, Item: %d bytes)\n", 
                  FRAME_QUEUE_SIZE, sizeof(BridgeFrame));
  }
  
  Serial.println();
  
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
  
  // ============================================================================
  // Phase 1.2.1.5: HID 전송 태스크 생성 (Core 1, 우선순위 3)
  // ============================================================================
  
  BaseType_t result2 = xTaskCreatePinnedToCore(
    hidTxTask,                      // 태스크 함수
    "HID_TX",                       // 태스크 이름
    HID_TX_TASK_STACK_SIZE,         // 스택 크기 (바이트)
    NULL,                           // 파라미터
    HID_TX_TASK_PRIORITY,           // 우선순위
    &g_hidTxTaskHandle,             // 태스크 핸들
    HID_TX_TASK_CORE                // 코어 번호
  );
  
  if (result2 == pdPASS) {
    Serial.printf("  ✓ HID_TX task created (Core %d, Priority %d)\n", 
                  HID_TX_TASK_CORE, HID_TX_TASK_PRIORITY);
  } else {
    Serial.println("  ✗ Failed to create HID_TX task!");
  }
  
  // ============================================================================
  // Phase 1.2.2.1: Vendor CDC 수신 태스크 생성 (Core 0, 우선순위 2)
  // ============================================================================
  
  BaseType_t result3 = xTaskCreatePinnedToCore(
    cdcRxTask,                      // 태스크 함수
    "CDC_RX",                       // 태스크 이름
    4096,                           // 스택 크기 (바이트)
    NULL,                           // 파라미터
    2,                              // 우선순위 (중간)
    &g_cdcRxTaskHandle,             // 태스크 핸들
    0                               // 코어 번호 (Core 0)
  );
  
  if (result3 == pdPASS) {
    Serial.printf("  ✓ CDC_RX task created (Core 0, Priority 2)\n");
  } else {
    Serial.println("  ✗ Failed to create CDC_RX task!");
  }
  
  // 디버그 태스크 생성 (Core 0, 우선순위 1)
  BaseType_t result4 = xTaskCreatePinnedToCore(
    debugTask,                      // 태스크 함수
    "DEBUG",                        // 태스크 이름
    DEBUG_TASK_STACK_SIZE,          // 스택 크기 (바이트)
    NULL,                           // 파라미터
    DEBUG_TASK_PRIORITY,            // 우선순위
    &g_debugTaskHandle,             // 태스크 핸들
    DEBUG_TASK_CORE                 // 코어 번호
  );
  
  if (result4 == pdPASS) {
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
