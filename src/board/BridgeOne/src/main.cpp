/**
 * @file main.cpp
 * @brief Geekble nano ESP32-S3 시리얼 모니터 테스트
 * @details USB CDC 초기화 및 출력 확인용 단순 테스트 코드
 */

#include <Arduino.h>

void setup() {
  // USB CDC 시리얼 초기화
  Serial.begin(115200);
  
  // 시리얼 준비 대기 (최대 2초)
  delay(2000);
  
  Serial.println();
  Serial.println("========================================");
  Serial.println("  Geekble nano ESP32-S3 Test");
  Serial.println("  USB CDC Serial Monitor OK!");
  Serial.println("========================================");
  Serial.println();
}

void loop() {
  static uint32_t counter = 0;
  
  Serial.print("Counter: ");
  Serial.println(counter++);
  
  delay(1000);
}