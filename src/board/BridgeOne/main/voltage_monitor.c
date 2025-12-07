/**
 * @file voltage_monitor.c
 * @brief BridgeOne 전압 모니터링 모드 구현
 *
 * ESP32-S3 내부 센서를 활용하여 USB 포트 전원 안정성을 확인합니다.
 */

#include <string.h>
#include <math.h>
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "esp_log.h"
#include "esp_task_wdt.h"
#include "driver/temperature_sensor.h"
#include "driver/gpio.h"
#include "esp_adc/adc_oneshot.h"
#include "voltage_monitor.h"

// ==================== 로깅 설정 ====================
static const char* TAG = "VMON";

// ==================== ADC 설정 ====================
// ADC1 채널 4 (GPIO5) - YD-ESP32-S3에서 사용 가능한 핀
#define VMON_ADC_UNIT       ADC_UNIT_1
#define VMON_ADC_CHANNEL    ADC_CHANNEL_4   // GPIO5
#define VMON_ADC_ATTEN      ADC_ATTEN_DB_12 // 0~3.3V 측정 범위
#define VMON_ADC_SAMPLES    10              // 노이즈 측정용 샘플 수

// ==================== 내부 변수 ====================
static temperature_sensor_handle_t s_temp_sensor = NULL;
static adc_oneshot_unit_handle_t s_adc_handle = NULL;
static bool s_initialized = false;
static float s_prev_temperature = 0.0f;
static uint32_t s_start_tick = 0;

// ==================== 로그 레벨 억제 ====================

/**
 * @brief 다른 모듈의 로그 레벨을 WARNING으로 설정하여 출력 억제
 */
static void suppress_other_logs(void) {
    // 주요 모듈의 로그 레벨을 WARNING으로 설정
    esp_log_level_set("BridgeOne", ESP_LOG_WARN);
    esp_log_level_set("USB", ESP_LOG_WARN);
    esp_log_level_set("HID", ESP_LOG_WARN);
    esp_log_level_set("UART", ESP_LOG_WARN);
    esp_log_level_set("HID_TEST", ESP_LOG_WARN);
    esp_log_level_set("tinyusb", ESP_LOG_WARN);
    esp_log_level_set("tusb_desc", ESP_LOG_WARN);
    esp_log_level_set("TinyUSB", ESP_LOG_WARN);

    // VMON 로그는 INFO 레벨 유지
    esp_log_level_set(TAG, ESP_LOG_INFO);
}

// ==================== 초기화 함수 ====================

bool voltage_monitor_init(void) {
    esp_err_t ret;

    // 다른 모듈 로그 억제
    suppress_other_logs();

    ESP_LOGI(TAG, "========================================");
    ESP_LOGI(TAG, "  BridgeOne Voltage Monitor Mode");
    ESP_LOGI(TAG, "========================================");

    // 1. 온도 센서 초기화
    ESP_LOGI(TAG, "Initializing temperature sensor...");
    temperature_sensor_config_t temp_config = {
        .range_min = 10,    // 최소 10도
        .range_max = 80,    // 최대 80도
    };

    ret = temperature_sensor_install(&temp_config, &s_temp_sensor);
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "Temperature sensor install failed: %s", esp_err_to_name(ret));
        return false;
    }

    ret = temperature_sensor_enable(s_temp_sensor);
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "Temperature sensor enable failed: %s", esp_err_to_name(ret));
        temperature_sensor_uninstall(s_temp_sensor);
        return false;
    }
    ESP_LOGI(TAG, "Temperature sensor OK");

    // 2. ADC 초기화
    ESP_LOGI(TAG, "Initializing ADC (GPIO5)...");
    adc_oneshot_unit_init_cfg_t adc_init_config = {
        .unit_id = VMON_ADC_UNIT,
        .ulp_mode = ADC_ULP_MODE_DISABLE,
    };

    ret = adc_oneshot_new_unit(&adc_init_config, &s_adc_handle);
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "ADC unit init failed: %s", esp_err_to_name(ret));
        temperature_sensor_disable(s_temp_sensor);
        temperature_sensor_uninstall(s_temp_sensor);
        return false;
    }

    adc_oneshot_chan_cfg_t adc_chan_config = {
        .atten = VMON_ADC_ATTEN,
        .bitwidth = ADC_BITWIDTH_12,    // 12비트 해상도 (0~4095)
    };

    ret = adc_oneshot_config_channel(s_adc_handle, VMON_ADC_CHANNEL, &adc_chan_config);
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "ADC channel config failed: %s", esp_err_to_name(ret));
        adc_oneshot_del_unit(s_adc_handle);
        temperature_sensor_disable(s_temp_sensor);
        temperature_sensor_uninstall(s_temp_sensor);
        return false;
    }
    ESP_LOGI(TAG, "ADC OK (12-bit, 0-3.3V range)");

    // 3. 시작 시간 기록
    s_start_tick = xTaskGetTickCount();

    // 4. 초기 온도 측정
    float init_temp;
    ret = temperature_sensor_get_celsius(s_temp_sensor, &init_temp);
    if (ret == ESP_OK) {
        s_prev_temperature = init_temp;
        ESP_LOGI(TAG, "Initial temperature: %.1f C", init_temp);
    }

    s_initialized = true;
    ESP_LOGI(TAG, "========================================");
    ESP_LOGI(TAG, "  Monitoring started (1s interval)");
    ESP_LOGI(TAG, "========================================");

    return true;
}

// ==================== 측정 함수 ====================

float voltage_monitor_get_temperature(void) {
    if (!s_initialized || s_temp_sensor == NULL) {
        return -999.0f;
    }

    float temp;
    esp_err_t ret = temperature_sensor_get_celsius(s_temp_sensor, &temp);
    if (ret != ESP_OK) {
        return -999.0f;
    }

    return temp;
}

noise_level_t voltage_monitor_get_adc_noise(int* out_raw, int* out_noise) {
    if (!s_initialized || s_adc_handle == NULL) {
        if (out_raw) *out_raw = 0;
        if (out_noise) *out_noise = 0;
        return NOISE_LEVEL_HIGH;
    }

    int samples[VMON_ADC_SAMPLES];
    int sum = 0;

    // 여러 번 샘플링
    for (int i = 0; i < VMON_ADC_SAMPLES; i++) {
        int raw;
        esp_err_t ret = adc_oneshot_read(s_adc_handle, VMON_ADC_CHANNEL, &raw);
        if (ret != ESP_OK) {
            samples[i] = 0;
        } else {
            samples[i] = raw;
            sum += raw;
        }
        vTaskDelay(pdMS_TO_TICKS(1));  // 1ms 간격
    }

    // 평균 계산
    int avg = sum / VMON_ADC_SAMPLES;
    if (out_raw) *out_raw = avg;

    // 표준편차 계산 (노이즈)
    float variance = 0.0f;
    for (int i = 0; i < VMON_ADC_SAMPLES; i++) {
        float diff = (float)(samples[i] - avg);
        variance += diff * diff;
    }
    variance /= VMON_ADC_SAMPLES;
    int stddev = (int)sqrtf(variance);
    if (out_noise) *out_noise = stddev;

    // 노이즈 레벨 분류
    if (stddev < 20) {
        return NOISE_LEVEL_LOW;
    } else if (stddev < VMON_ADC_NOISE_THRESHOLD) {
        return NOISE_LEVEL_MEDIUM;
    } else {
        return NOISE_LEVEL_HIGH;
    }
}

uint32_t voltage_monitor_get_uptime_sec(void) {
    uint32_t current_tick = xTaskGetTickCount();
    return (current_tick - s_start_tick) / configTICK_RATE_HZ;
}

void voltage_monitor_get_result(voltage_monitor_result_t* result) {
    if (!result) return;

    memset(result, 0, sizeof(voltage_monitor_result_t));

    // 온도 측정
    result->temperature = voltage_monitor_get_temperature();
    result->prev_temperature = s_prev_temperature;

    // 온도 급변 감지
    float temp_diff = fabsf(result->temperature - s_prev_temperature);
    result->temp_warning = (temp_diff >= VMON_TEMP_CHANGE_THRESHOLD);

    // ADC 노이즈 측정
    result->noise_level = voltage_monitor_get_adc_noise(&result->adc_raw, &result->adc_noise);

    // 가동시간
    result->uptime_sec = voltage_monitor_get_uptime_sec();

    // 이전 온도 업데이트
    s_prev_temperature = result->temperature;
}

// ==================== 노이즈 레벨 문자열 ====================

static const char* noise_level_to_str(noise_level_t level) {
    switch (level) {
        case NOISE_LEVEL_LOW:    return "LOW (stable)";
        case NOISE_LEVEL_MEDIUM: return "MEDIUM";
        case NOISE_LEVEL_HIGH:   return "HIGH (unstable)";
        default:                 return "UNKNOWN";
    }
}

// ==================== 모니터링 설정 ====================

/**
 * @brief 테스트 지속 시간 (초)
 * 이 시간이 지나면 모니터링이 자동 종료됩니다.
 */
#define VMON_TEST_DURATION_SEC  180

// ==================== 모니터링 태스크 ====================

void voltage_monitor_task(void* param) {
    // 태스크 워치독 등록
    esp_task_wdt_add(NULL);

    ESP_LOGI(TAG, "Voltage Monitor Task started");

    // 초기화
    if (!voltage_monitor_init()) {
        ESP_LOGE(TAG, "Initialization failed, task ending");
        esp_task_wdt_delete(NULL);
        vTaskDelete(NULL);
        return;
    }

    // 초기 대기 (시스템 안정화)
    ESP_LOGI(TAG, "Waiting 2s for system stabilization...");
    vTaskDelay(pdMS_TO_TICKS(2000));

    ESP_LOGI(TAG, "");
    ESP_LOGI(TAG, "Format: Temp | ADC (noise) | Uptime");
    ESP_LOGI(TAG, "Test duration: %d seconds", VMON_TEST_DURATION_SEC);
    ESP_LOGI(TAG, "----------------------------------------");

    // 모니터링 루프 (180초 후 자동 종료)
    while (1) {
        voltage_monitor_result_t result;
        voltage_monitor_get_result(&result);

        // 테스트 시간 초과 확인
        if (result.uptime_sec >= VMON_TEST_DURATION_SEC) {
            ESP_LOGI(TAG, "----------------------------------------");
            ESP_LOGI(TAG, "Test completed! Duration: %us", result.uptime_sec);
            ESP_LOGI(TAG, "========================================");
            break;
        }

        // 온도 급변 경고
        if (result.temp_warning) {
            ESP_LOGW(TAG, "TEMP CHANGE: %.1f C -> %.1f C (power unstable?)",
                     result.prev_temperature, result.temperature);
        }

        // ADC 전압 계산 (0~3.3V, 12비트)
        float voltage = (result.adc_raw / 4095.0f) * 3.3f;

        // 상태 출력
        ESP_LOGI(TAG, "%.1f C | ADC:%d (%.2fV, noise:%d %s) | %us",
                 result.temperature,
                 result.adc_raw,
                 voltage,
                 result.adc_noise,
                 noise_level_to_str(result.noise_level),
                 result.uptime_sec);

        // 고노이즈 경고
        if (result.noise_level == NOISE_LEVEL_HIGH) {
            ESP_LOGW(TAG, "HIGH NOISE detected - power supply may be unstable!");
        }

        // 워치독 리셋
        esp_task_wdt_reset();

        // 다음 측정까지 대기
        vTaskDelay(pdMS_TO_TICKS(VMON_INTERVAL_MS));
    }

    // 태스크 정리
    ESP_LOGI(TAG, "Voltage Monitor Task ended");
    esp_task_wdt_delete(NULL);
    vTaskDelete(NULL);
}
