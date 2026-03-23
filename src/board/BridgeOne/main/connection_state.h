/**
 * @file connection_state.h
 * @brief ESP32-S3 연결 상태 머신
 *
 * Windows 서버와의 핸드셰이크 연결 상태를 관리합니다.
 * 상태 전이는 FreeRTOS 뮤텍스로 보호되어 스레드 안전합니다.
 *
 * 상태 흐름:
 *   IDLE ──(AUTH_CHALLENGE 수신)──> AUTH_PENDING
 *   AUTH_PENDING ──(AUTH_RESPONSE 전송 성공)──> AUTH_OK
 *   AUTH_PENDING ──(타임아웃)──> IDLE
 *   AUTH_OK ──(STATE_SYNC 수신)──> SYNC_PENDING
 *   AUTH_OK ──(타임아웃)──> IDLE
 *   SYNC_PENDING ──(STATE_SYNC_ACK 전송 성공)──> CONNECTED
 *   SYNC_PENDING ──(타임아웃)──> IDLE
 *   CONNECTED ──(Keep-alive 실패 또는 CDC 해제)──> IDLE
 *   ERROR ──(리셋)──> IDLE
 *
 * 참조:
 * - docs/development-plans/phase-3-3-handshake-protocol.md §3.3.1
 * - docs/windows/technical-specification-server.md §3.2
 */

#ifndef CONNECTION_STATE_H
#define CONNECTION_STATE_H

#include <stdint.h>
#include <stdbool.h>

// ==================== 연결 상태 열거형 ====================

/**
 * ESP32-S3 ↔ Windows 서버 연결 상태.
 */
typedef enum {
    CONN_STATE_IDLE,          // 서버 미연결 (Essential 모드)
    CONN_STATE_AUTH_PENDING,  // 인증 챌린지 수신, 응답 대기
    CONN_STATE_AUTH_OK,       // 인증 성공, State Sync 대기
    CONN_STATE_SYNC_PENDING,  // State Sync 수신, ACK 대기
    CONN_STATE_CONNECTED,     // 핸드셰이크 완료 (Standard 모드)
    CONN_STATE_ERROR,         // 오류 상태

    CONN_STATE_COUNT          // 상태 개수 (유효성 검사용)
} connection_state_t;

// ==================== 기능 협상 ====================

/** 서버가 요청할 수 있는 최대 기능 수 */
#define CONN_MAX_FEATURES  16

/** 기능 이름 최대 길이 (null 포함) */
#define CONN_FEATURE_NAME_MAX  32

/**
 * 기능 협상 결과 구조체.
 *
 * 서버가 STATE_SYNC에서 요청한 기능 목록과
 * ESP32-S3가 수락한 기능 목록을 저장합니다.
 * Phase 3.5 (모드 전환)에서 모드별 동작 결정에 사용됩니다.
 */
typedef struct {
    /** 서버가 요청한 기능 목록 */
    char     requested[CONN_MAX_FEATURES][CONN_FEATURE_NAME_MAX];
    uint8_t  requested_count;

    /** ESP32-S3가 수락한 기능 목록 */
    char     accepted[CONN_MAX_FEATURES][CONN_FEATURE_NAME_MAX];
    uint8_t  accepted_count;

    /** 합의된 Keep-alive 주기 (ms) */
    uint16_t keepalive_ms;
} connection_features_t;

// ==================== 브릿지 모드 열거형 ====================

/**
 * ESP32-S3 브릿지 동작 모드.
 *
 * - ESSENTIAL: 서버 미연결 상태. 기본 마우스 이동 + 좌클릭만 허용.
 * - STANDARD:  서버 연결 완료 상태. 모든 HID 기능 활성화.
 *
 * 모드 전환은 연결 상태 변경에 의해 자동으로 수행됩니다:
 *   CONN_STATE_CONNECTED 진입 → BRIDGE_MODE_STANDARD
 *   CONN_STATE_IDLE 진입      → BRIDGE_MODE_ESSENTIAL
 */
typedef enum {
    BRIDGE_MODE_ESSENTIAL,  // 기본 모드 (서버 미연결)
    BRIDGE_MODE_STANDARD    // 서버 연결 모드
} bridge_mode_t;

/**
 * 모드 변경 콜백 함수 포인터 타입.
 *
 * @param old_mode 이전 모드
 * @param new_mode 새로운 모드
 */
typedef void (*bridge_mode_change_cb_t)(bridge_mode_t old_mode,
                                        bridge_mode_t new_mode);

// ==================== 콜백 타입 ====================

/**
 * 상태 변경 콜백 함수 포인터 타입.
 *
 * @param old_state 이전 상태
 * @param new_state 새로운 상태
 */
typedef void (*connection_state_change_cb_t)(connection_state_t old_state,
                                             connection_state_t new_state);

// ==================== 함수 선언 ====================

/**
 * 연결 상태 모듈 초기화.
 *
 * FreeRTOS 뮤텍스를 생성하고 상태를 IDLE로 설정합니다.
 * app_main()에서 핸드셰이크 관련 태스크 시작 전에 호출해야 합니다.
 *
 * @return true: 초기화 성공, false: 뮤텍스 생성 실패
 */
bool connection_state_init(void);

/**
 * 현재 연결 상태 조회.
 *
 * 뮤텍스 보호 하에 현재 상태를 반환합니다.
 *
 * @return 현재 connection_state_t 값
 */
connection_state_t connection_state_get(void);

/**
 * 연결 상태 전이.
 *
 * 유효한 전이만 허용하며, 잘못된 전이 시도 시 false를 반환합니다.
 * 성공 시 등록된 콜백을 호출합니다.
 *
 * 유효한 전이:
 * - IDLE → AUTH_PENDING
 * - AUTH_PENDING → AUTH_OK, IDLE
 * - AUTH_OK → SYNC_PENDING, IDLE
 * - SYNC_PENDING → CONNECTED, IDLE
 * - CONNECTED → IDLE
 * - ERROR → IDLE
 * - (모든 상태) → ERROR
 *
 * @param new_state 전이할 새로운 상태
 * @return true: 전이 성공, false: 유효하지 않은 전이
 */
bool connection_state_transition(connection_state_t new_state);

/**
 * 상태를 IDLE로 리셋.
 *
 * 어떤 상태에서든 무조건 IDLE로 전이합니다.
 * CDC 해제, Keep-alive 실패, 오류 복구 등에 사용합니다.
 */
void connection_state_reset(void);

/**
 * 상태 변경 콜백 등록.
 *
 * 상태가 변경될 때 호출되는 콜백을 등록합니다.
 * 콜백은 뮤텍스 해제 후 호출되므로 콜백 내에서
 * connection_state 함수를 호출해도 데드락이 발생하지 않습니다.
 *
 * @param callback 콜백 함수 (NULL이면 콜백 해제)
 */
void connection_state_on_change(connection_state_change_cb_t callback);

/**
 * 기능 협상 결과 저장.
 *
 * STATE_SYNC 핸들러에서 협상 완료 후 호출합니다.
 * 뮤텍스 보호 하에 features 구조체를 복사합니다.
 *
 * @param features 저장할 기능 협상 결과
 */
void connection_state_set_features(const connection_features_t *features);

/**
 * 기능 협상 결과 조회.
 *
 * 현재 저장된 기능 협상 결과를 복사합니다.
 *
 * @param out_features 결과를 받을 구조체 포인터
 * @return true: 유효한 데이터 복사됨, false: CONNECTED 상태가 아니거나 데이터 없음
 */
bool connection_state_get_features(connection_features_t *out_features);

/**
 * 상태 이름 문자열 반환 (디버그/로깅용).
 *
 * @param state 상태 값
 * @return 상태 이름 문자열 (예: "IDLE", "CONNECTED")
 */
const char *connection_state_name(connection_state_t state);

// ==================== 브릿지 모드 관리 ====================

/**
 * 현재 브릿지 모드 조회.
 *
 * @return 현재 bridge_mode_t 값
 */
bridge_mode_t bridge_mode_get(void);

/**
 * 모드 변경 콜백 등록.
 *
 * 모드가 변경될 때 호출되는 콜백을 등록합니다.
 * Phase 3.5.2에서 HID 필터링 전환에 사용됩니다.
 *
 * @param callback 콜백 함수 (NULL이면 콜백 해제)
 */
void bridge_mode_on_change(bridge_mode_change_cb_t callback);

/**
 * 모드 이름 문자열 반환 (디버그/로깅용).
 *
 * @param mode 모드 값
 * @return 모드 이름 문자열 ("ESSENTIAL" 또는 "STANDARD")
 */
const char *bridge_mode_name(bridge_mode_t mode);

/**
 * 수락된 기능 목록에서 특정 기능 지원 여부 조회.
 *
 * CONNECTED 상태에서 협상된 accepted_features 중
 * 지정된 기능이 포함되어 있는지 확인합니다.
 *
 * @param feature_name 조회할 기능 이름 (예: "wheel", "drag", "right_click")
 * @return true: 기능이 수락됨, false: 미수락 또는 CONNECTED 상태 아님
 */
bool bridge_mode_is_feature_active(const char *feature_name);

#endif // CONNECTION_STATE_H
