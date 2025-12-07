/**
 * @file voltage_monitor.h
 * @brief BridgeOne 전압 모니터링 모드 - USB 포트 전원 안정성 테스트
 *
 * ESP32-S3 내부 센서를 활용하여 추가 하드웨어 없이
 * USB 포트의 전원 안정성을 확인합니다.
 *
 * 측정 항목:
 * - 칩 내부 온도: 전원 불안정 시 온도 급변 감지
 * - ADC 노이즈 레벨: 전원 노이즈 수준 확인
 * - 시스템 가동시간: 리셋/크래시 감지
 *
 * 사용법:
 * 1. BridgeOne.c에서 #define VOLTAGE_MONITOR_MODE 주석 해제
 * 2. idf.py build && idf.py flash
 * 3. 시리얼 터미널로 로그 확인 (115200bps)
 */

#ifndef VOLTAGE_MONITOR_H
#define VOLTAGE_MONITOR_H

#include <stdint.h>
#include <stdbool.h>

// ==================== 모니터링 설정 ====================

/**
 * @brief 모니터링 간격 (밀리초)
 */
#define VMON_INTERVAL_MS        1000

/**
 * @brief 온도 급변 감지 임계값 (섭씨)
 * 이 값 이상 온도가 변하면 경고 출력
 */
#define VMON_TEMP_CHANGE_THRESHOLD  3.0f

/**
 * @brief ADC 노이즈 레벨 임계값
 * 연속 측정 시 표준편차가 이 값 이상이면 노이즈 높음
 */
#define VMON_ADC_NOISE_THRESHOLD    50

// ==================== 노이즈 레벨 분류 ====================

typedef enum {
    NOISE_LEVEL_LOW,        // 안정적
    NOISE_LEVEL_MEDIUM,     // 보통
    NOISE_LEVEL_HIGH        // 불안정
} noise_level_t;

// ==================== 모니터링 결과 구조체 ====================

typedef struct {
    float temperature;          // 현재 온도 (섭씨)
    float prev_temperature;     // 이전 온도 (급변 감지용)
    int adc_raw;                // ADC 원시값
    int adc_noise;              // ADC 노이즈 (표준편차)
    noise_level_t noise_level;  // 노이즈 레벨
    uint32_t uptime_sec;        // 가동시간 (초)
    bool temp_warning;          // 온도 급변 경고
} voltage_monitor_result_t;

// ==================== 함수 선언 ====================

/**
 * @brief 전압 모니터링 태스크
 *
 * 1초 간격으로 온도, ADC, 가동시간을 측정하여 로그로 출력합니다.
 * 다른 모듈의 로그는 억제하여 모니터링 로그만 출력됩니다.
 *
 * @param param 미사용 (NULL)
 */
void voltage_monitor_task(void* param);

/**
 * @brief 전압 모니터링 초기화
 *
 * 온도 센서 및 ADC를 초기화합니다.
 * 다른 모듈의 로그 레벨을 WARNING으로 설정하여 출력을 억제합니다.
 *
 * @return true 초기화 성공
 * @return false 초기화 실패
 */
bool voltage_monitor_init(void);

/**
 * @brief 칩 내부 온도 측정
 *
 * @return float 온도 (섭씨), 오류 시 -999.0f
 */
float voltage_monitor_get_temperature(void);

/**
 * @brief ADC 노이즈 레벨 측정
 *
 * 여러 번 ADC를 읽어 표준편차를 계산합니다.
 *
 * @param out_raw ADC 평균값 출력 (NULL 허용)
 * @param out_noise 노이즈(표준편차) 출력 (NULL 허용)
 * @return noise_level_t 노이즈 레벨
 */
noise_level_t voltage_monitor_get_adc_noise(int* out_raw, int* out_noise);

/**
 * @brief 시스템 가동시간 반환
 *
 * @return uint32_t 가동시간 (초)
 */
uint32_t voltage_monitor_get_uptime_sec(void);

/**
 * @brief 현재 모니터링 결과 가져오기
 *
 * @param result 결과 저장 구조체
 */
void voltage_monitor_get_result(voltage_monitor_result_t* result);

#endif // VOLTAGE_MONITOR_H
