/**
 * @file connection_state.c
 * @brief ESP32-S3 연결 상태 머신 구현
 *
 * Windows 서버와의 핸드셰이크 연결 상태를 FreeRTOS 뮤텍스로
 * 보호하여 스레드 안전하게 관리합니다.
 *
 * 참조:
 * - docs/development-plans/phase-3-3-handshake-protocol.md §3.3.1
 */

#include "connection_state.h"
#include "freertos/FreeRTOS.h"
#include "freertos/semphr.h"
#include "esp_log.h"
#include <string.h>

static const char *TAG = "CONN_STATE";

// ==================== 내부 상태 ====================

/** 현재 연결 상태 */
static connection_state_t s_state = CONN_STATE_IDLE;

/** 스레드 안전성을 위한 뮤텍스 */
static SemaphoreHandle_t s_mutex = NULL;

/** 상태 변경 콜백 */
static connection_state_change_cb_t s_change_cb = NULL;

/** 기능 협상 결과 */
static connection_features_t s_features;

/** 기능 협상 결과 유효 플래그 */
static bool s_features_valid = false;

// ==================== 상태 이름 테이블 ====================

static const char *state_names[] = {
    [CONN_STATE_IDLE]         = "IDLE",
    [CONN_STATE_AUTH_PENDING] = "AUTH_PENDING",
    [CONN_STATE_AUTH_OK]      = "AUTH_OK",
    [CONN_STATE_SYNC_PENDING] = "SYNC_PENDING",
    [CONN_STATE_CONNECTED]    = "CONNECTED",
    [CONN_STATE_ERROR]        = "ERROR",
};

// ==================== 상태 전이 유효성 테이블 ====================

/**
 * 유효한 상태 전이를 정의하는 2차원 배열.
 * transition_table[현재상태][새상태] = true면 전이 허용.
 */
static const bool transition_table[CONN_STATE_COUNT][CONN_STATE_COUNT] = {
    /* From IDLE */
    [CONN_STATE_IDLE] = {
        [CONN_STATE_AUTH_PENDING] = true,   // AUTH_CHALLENGE 수신
        [CONN_STATE_ERROR]        = true,   // 오류 발생
    },
    /* From AUTH_PENDING */
    [CONN_STATE_AUTH_PENDING] = {
        [CONN_STATE_AUTH_OK]      = true,   // AUTH_RESPONSE 전송 성공
        [CONN_STATE_IDLE]         = true,   // 타임아웃 또는 실패
        [CONN_STATE_ERROR]        = true,   // 오류 발생
    },
    /* From AUTH_OK */
    [CONN_STATE_AUTH_OK] = {
        [CONN_STATE_SYNC_PENDING] = true,   // STATE_SYNC 수신
        [CONN_STATE_IDLE]         = true,   // 타임아웃
        [CONN_STATE_ERROR]        = true,   // 오류 발생
    },
    /* From SYNC_PENDING */
    [CONN_STATE_SYNC_PENDING] = {
        [CONN_STATE_CONNECTED]    = true,   // STATE_SYNC_ACK 전송 성공
        [CONN_STATE_IDLE]         = true,   // 타임아웃
        [CONN_STATE_ERROR]        = true,   // 오류 발생
    },
    /* From CONNECTED */
    [CONN_STATE_CONNECTED] = {
        [CONN_STATE_IDLE]         = true,   // Keep-alive 실패 또는 CDC 해제
        [CONN_STATE_ERROR]        = true,   // 오류 발생
    },
    /* From ERROR */
    [CONN_STATE_ERROR] = {
        [CONN_STATE_IDLE]         = true,   // 리셋
    },
};

// ==================== 함수 구현 ====================

bool connection_state_init(void)
{
    s_mutex = xSemaphoreCreateMutex();
    if (s_mutex == NULL) {
        ESP_LOGE(TAG, "Failed to create mutex");
        return false;
    }

    s_state = CONN_STATE_IDLE;
    s_change_cb = NULL;
    s_features_valid = false;
    memset(&s_features, 0, sizeof(s_features));

    ESP_LOGI(TAG, "Connection state initialized (state=IDLE)");
    return true;
}

connection_state_t connection_state_get(void)
{
    connection_state_t state;

    if (s_mutex != NULL && xSemaphoreTake(s_mutex, pdMS_TO_TICKS(100)) == pdTRUE) {
        state = s_state;
        xSemaphoreGive(s_mutex);
    } else {
        state = s_state;  // 뮤텍스 획득 실패 시 직접 읽기 (최선의 노력)
    }

    return state;
}

bool connection_state_transition(connection_state_t new_state)
{
    if (new_state >= CONN_STATE_COUNT) {
        ESP_LOGE(TAG, "Invalid state value: %d", new_state);
        return false;
    }

    if (s_mutex == NULL) {
        ESP_LOGE(TAG, "Module not initialized");
        return false;
    }

    if (xSemaphoreTake(s_mutex, pdMS_TO_TICKS(100)) != pdTRUE) {
        ESP_LOGE(TAG, "Mutex timeout on transition to %s", state_names[new_state]);
        return false;
    }

    connection_state_t old_state = s_state;

    // 동일 상태로의 전이는 무시
    if (old_state == new_state) {
        xSemaphoreGive(s_mutex);
        return true;
    }

    // 전이 유효성 검사
    if (!transition_table[old_state][new_state]) {
        ESP_LOGW(TAG, "Invalid transition: %s -> %s",
                 state_names[old_state], state_names[new_state]);
        xSemaphoreGive(s_mutex);
        return false;
    }

    // 상태 전이 수행
    s_state = new_state;

    // IDLE로 돌아가면 기능 협상 결과 초기화
    if (new_state == CONN_STATE_IDLE) {
        s_features_valid = false;
        memset(&s_features, 0, sizeof(s_features));
    }

    // 콜백 포인터를 로컬에 복사 (뮤텍스 해제 후 호출하기 위함)
    connection_state_change_cb_t cb = s_change_cb;

    xSemaphoreGive(s_mutex);

    ESP_LOGI(TAG, "State: %s -> %s", state_names[old_state], state_names[new_state]);

    // 콜백 호출 (뮤텍스 해제 후 → 데드락 방지)
    if (cb != NULL) {
        cb(old_state, new_state);
    }

    return true;
}

void connection_state_reset(void)
{
    connection_state_t old_state;
    connection_state_change_cb_t cb = NULL;

    if (s_mutex != NULL && xSemaphoreTake(s_mutex, pdMS_TO_TICKS(100)) == pdTRUE) {
        old_state = s_state;
        s_state = CONN_STATE_IDLE;
        s_features_valid = false;
        memset(&s_features, 0, sizeof(s_features));
        cb = s_change_cb;
        xSemaphoreGive(s_mutex);
    } else {
        old_state = s_state;
        s_state = CONN_STATE_IDLE;
        s_features_valid = false;
    }

    if (old_state != CONN_STATE_IDLE) {
        ESP_LOGI(TAG, "State reset: %s -> IDLE", state_names[old_state]);
        if (cb != NULL) {
            cb(old_state, CONN_STATE_IDLE);
        }
    }
}

void connection_state_on_change(connection_state_change_cb_t callback)
{
    if (s_mutex != NULL && xSemaphoreTake(s_mutex, pdMS_TO_TICKS(100)) == pdTRUE) {
        s_change_cb = callback;
        xSemaphoreGive(s_mutex);
    } else {
        s_change_cb = callback;
    }

    ESP_LOGI(TAG, "State change callback %s",
             callback != NULL ? "registered" : "cleared");
}

void connection_state_set_features(const connection_features_t *features)
{
    if (features == NULL) {
        ESP_LOGW(TAG, "set_features called with NULL");
        return;
    }

    if (s_mutex != NULL && xSemaphoreTake(s_mutex, pdMS_TO_TICKS(100)) == pdTRUE) {
        memcpy(&s_features, features, sizeof(connection_features_t));
        s_features_valid = true;
        xSemaphoreGive(s_mutex);
    }

    ESP_LOGI(TAG, "Features saved: requested=%u, accepted=%u, keepalive=%ums",
             features->requested_count, features->accepted_count,
             features->keepalive_ms);
}

bool connection_state_get_features(connection_features_t *out_features)
{
    if (out_features == NULL) {
        return false;
    }

    bool result = false;

    if (s_mutex != NULL && xSemaphoreTake(s_mutex, pdMS_TO_TICKS(100)) == pdTRUE) {
        if (s_features_valid) {
            memcpy(out_features, &s_features, sizeof(connection_features_t));
            result = true;
        }
        xSemaphoreGive(s_mutex);
    }

    return result;
}

const char *connection_state_name(connection_state_t state)
{
    if (state < CONN_STATE_COUNT) {
        return state_names[state];
    }
    return "UNKNOWN";
}
