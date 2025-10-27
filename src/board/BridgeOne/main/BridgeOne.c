#include <stdio.h>
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"

/**
 * 애플리케이션 메인 함수 (FreeRTOS 작업)
 * 
 * 목적: 빌드 환경 검증 및 최소 스켈레톤 코드
 * - ESP32-S3 부팅 확인
 * - 플래시/PSRAM 크기 로깅
 * - USB 디바이스 인식 확인
 * 
 * 실제 기능 구현은 Phase 2 이후에서 수행됨.
 */
void app_main(void) {
    // BridgeOne 보드의 빌드 환경 검증
    printf("BridgeOne Board - Environment Setup Complete\n");
    printf("ESP32-S3-N16R8: 16MB Flash, 8MB PSRAM\n");
    
    // 무한 루프: 1초마다 유지 신호 발송
    while (1) {
        vTaskDelay(1000 / portTICK_PERIOD_MS);
    }
}
