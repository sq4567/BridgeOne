/**
 * @file main.cpp
 * @brief Geekble nano ESP32-S3 테스트 코드
 * @author BridgeOne Project
 * @date 2024
 * 
 * Geekble nano ESP32-S3 보드의 기본 동작을 확인하는 테스트 코드입니다.
 * USB HID, WiFi, Bluetooth 기능이 포함되어 있습니다.
 */

#include <Arduino.h>

// Geekble nano ESP32-S3 보드 정보 출력
void printBoardInfo();
void setupLED();
void blinkLED();

// LED 핀 정의 (일반적인 ESP32-S3 내장 LED)
#define LED_PIN 48

/**
 * @brief 초기 설정 함수
 * 시리얼 통신, LED, USB 등을 초기화합니다.
 */
void setup() {
  // 시리얼 통신 초기화
  Serial.begin(115200);
  
  // 시리얼 연결 대기 (최대 3초)
  unsigned long startTime = millis();
  while (!Serial && (millis() - startTime < 3000)) {
    delay(100);
  }
  
  Serial.println("====================================");
  Serial.println("   Geekble nano ESP32-S3 Test");
  Serial.println("====================================");
  
  // 보드 정보 출력
  printBoardInfo();
  
  // LED 초기화
  setupLED();
  
  Serial.println("Setup completed!");
  Serial.println("LED will blink every 1 second");
  Serial.println("====================================");
}

/**
 * @brief 메인 루프 함수
 * LED 깜빡임과 시리얼 출력을 반복합니다.
 */
void loop() {
  static unsigned long lastTime = 0;
  static int counter = 0;
  
  // 1초마다 실행
  if (millis() - lastTime >= 1000) {
    lastTime = millis();
    counter++;
    
    // LED 깜빡임
    blinkLED();
    
    // 카운터 출력
    Serial.printf("Running... Counter: %d (Free heap: %d bytes)\n", 
                  counter, ESP.getFreeHeap());
    
    // 10초마다 보드 상태 출력
    if (counter % 10 == 0) {
      Serial.println("Board Status:");
      Serial.printf("  - CPU Frequency: %d MHz\n", ESP.getCpuFreqMHz());
      Serial.printf("  - Flash Size: %d KB\n", ESP.getFlashChipSize() / 1024);
      Serial.printf("  - PSRAM Size: %d KB\n", ESP.getPsramSize() / 1024);
      Serial.printf("  - Chip Model: %s\n", ESP.getChipModel());
      Serial.printf("  - SDK Version: %s\n", ESP.getSdkVersion());
      Serial.println("--------------------");
    }
  }
  
  delay(10); // CPU 사용률 낮추기
}

/**
 * @brief 보드 정보 출력 함수
 * Geekble nano ESP32-S3의 하드웨어 정보를 출력합니다.
 */
void printBoardInfo() {
  Serial.println("Board Information:");
  Serial.println("  - Board: Geekble nano ESP32-S3");
  Serial.println("  - Manufacturer: SooDragon");
  Serial.printf("  - Chip: %s\n", ESP.getChipModel());
  Serial.printf("  - Chip Revision: %d\n", ESP.getChipRevision());
  Serial.printf("  - CPU Cores: %d\n", ESP.getChipCores());
  Serial.printf("  - CPU Frequency: %d MHz\n", ESP.getCpuFreqMHz());
  Serial.printf("  - Flash Size: %d KB\n", ESP.getFlashChipSize() / 1024);
  Serial.printf("  - Flash Speed: %d MHz\n", ESP.getFlashChipSpeed() / 1000000);
  Serial.printf("  - PSRAM Size: %d KB\n", ESP.getPsramSize() / 1024);
  Serial.printf("  - Free Heap: %d bytes\n", ESP.getFreeHeap());
  Serial.printf("  - SDK Version: %s\n", ESP.getSdkVersion());
  
  // MAC 주소 출력
  uint8_t mac[6];
  esp_read_mac(mac, ESP_MAC_WIFI_STA);
  Serial.printf("  - WiFi MAC: %02X:%02X:%02X:%02X:%02X:%02X\n", 
                mac[0], mac[1], mac[2], mac[3], mac[4], mac[5]);
  
  Serial.println();
  
  // 특수 기능 확인
  Serial.println("Features:");
  
#ifdef CONFIG_TINYUSB_ENABLED
  Serial.println("  ✓ TinyUSB Enabled");
#else
  Serial.println("  ✗ TinyUSB Disabled");
#endif

#ifdef CONFIG_TINYUSB_HID_ENABLED
  Serial.println("  ✓ USB HID Enabled");
#else
  Serial.println("  ✗ USB HID Disabled");
#endif

#ifdef CONFIG_TINYUSB_CDC_ENABLED
  Serial.println("  ✓ USB CDC Enabled");
#else
  Serial.println("  ✗ USB CDC Disabled");
#endif

#ifdef ARDUINO_GEEKBLE_NANO_ESP32S3
  Serial.println("  ✓ Geekble nano Board Definition Active");
#else
  Serial.println("  ✗ Generic ESP32-S3 Board");
#endif

#ifdef BOARD_HAS_PSRAM
  Serial.println("  ✓ PSRAM Available");
#else
  Serial.println("  ✗ No PSRAM");
#endif

  Serial.println();
}

/**
 * @brief LED 초기화 함수
 * 내장 LED 핀을 출력 모드로 설정합니다.
 */
void setupLED() {
  pinMode(LED_PIN, OUTPUT);
  digitalWrite(LED_PIN, LOW); // LED 초기 상태: 꺼짐
  Serial.printf("LED pin %d initialized\n", LED_PIN);
}

/**
 * @brief LED 깜빡임 함수
 * LED를 짧게 켰다가 끕니다.
 */
void blinkLED() {
  digitalWrite(LED_PIN, HIGH); // LED 켜기
  delay(100);                  // 100ms 대기
  digitalWrite(LED_PIN, LOW);  // LED 끄기
}