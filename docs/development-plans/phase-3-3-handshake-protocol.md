---
title: "BridgeOne Phase 3.3: 핸드셰이크 프로토콜 구현"
description: "BridgeOne 프로젝트 Phase 3.3 - Authentication + State Sync 2단계 핸드셰이크로 안전한 Standard 모드 연결 확립"
tags: ["handshake", "authentication", "state-sync", "vendor-cdc", "esp32-s3", "windows"]
version: "v1.0"
owner: "Chatterbones"
updated: "2026-03-19"
---

# BridgeOne Phase 3.3: 핸드셰이크 프로토콜 구현

**개발 기간**: 3-4일

**목표**: Authentication + State Sync 2단계 핸드셰이크를 구현하여 Windows 서버와 ESP32-S3 간 안전한 연결 확립 절차를 완성합니다.

**핵심 성과물**:
- Phase 1 Authentication: 에코백 방식 인증 (모듈화 설계, 추후 HMAC 교체 가능)
- Phase 2 State Sync: 기능 협상 및 Keep-alive 주기 합의
- ESP32-S3 연결 상태 머신 (`connection_state.c/h`)
- 핸드셰이크 타임아웃 및 재시도 처리

**선행 조건**: Phase 3.2 (Windows CDC 연결 계층) 완료

---

## 📋 Phase 3.2 완료 사항 요약

- Windows 서버가 ESP32-S3 CDC COM 포트를 자동 감지하고 SerialPort로 연결 가능
- Vendor CDC 프레임 양방향 송수신 가능 (CRC16 검증 포함)
- 디버그 텍스트와 Vendor CDC 바이너리 프레임이 간섭 없이 공존
- MVVM 아키텍처 기반 연결 상태 UI 구축

---

## 핸드셰이크 프로토콜 개요

### 전체 흐름

```
Windows 서버                          ESP32-S3 동글
     │                                     │
     │  ── CDC 포트 열기 ──>               │
     │                                     │  (IDLE 상태)
     │                                     │
     │  ═══ Phase 1: Authentication (1초 타임아웃) ═══
     │                                     │
     │  CMD_AUTH_CHALLENGE (0x01)           │
     │  {"command":"AUTH_CHALLENGE",        │
     │   "challenge":"<random_hex>",       │
     │   "version":"1.0"}                  │
     │  ──────────────────────────────────> │
     │                                     │  (AUTH_PENDING)
     │                                     │
     │           CMD_AUTH_RESPONSE (0x02)   │
     │  {"command":"AUTH_RESPONSE",         │
     │   "response":"<echo_challenge>",    │
     │   "device":"BridgeOne",             │
     │   "fw_version":"1.0.0"}             │
     │  <────────────────────────────────── │
     │                                     │  (AUTH_OK)
     │                                     │
     │  ═══ Phase 2: State Sync (1초 타임아웃) ═══
     │                                     │
     │  CMD_STATE_SYNC (0x03)              │
     │  {"command":"STATE_SYNC",           │
     │   "features":["wheel","drag",       │
     │    "right_click","multi_cursor",    │
     │    "macro","extended_keyboard"],    │
     │   "keepalive_ms":500}               │
     │  ──────────────────────────────────> │
     │                                     │  (SYNC_PENDING)
     │                                     │
     │         CMD_STATE_SYNC_ACK (0x04)   │
     │  {"command":"STATE_SYNC_ACK",       │
     │   "accepted_features":["wheel",     │
     │    "drag","right_click"],           │
     │   "mode":"standard"}                │
     │  <────────────────────────────────── │
     │                                     │  (CONNECTED / Standard)
     │                                     │
     │  ═══ 핸드셰이크 완료 ═══             │
     │  Keep-alive 시작 (Phase 3.4)        │
     │                                     │
```

### 인증 방식: 에코백 (모듈화 설계)

현재 구현은 단순 에코백 방식입니다:
- 서버가 랜덤 16바이트 hex 문자열(challenge)을 전송
- ESP32-S3가 동일한 challenge를 그대로 응답(echo back)
- 서버가 응답의 일치를 확인

**보안 분석**: BridgeOne은 USB 물리 연결 기반이므로 원격 공격 벡터가 없습니다. 동일 PC의 악성 프로그램이 CDC 포트를 열어 가짜 서버로 위장할 이론적 가능성은 있으나, 그 상황에서는 이미 PC를 직접 제어할 수 있으므로 실질적 추가 위험은 없습니다.

**추후 업그레이드 경로**: 인증 로직을 인터페이스로 추상화하여, 필요 시 HMAC-SHA256 등으로 교체할 수 있도록 설계합니다.

---

## Phase 3.3.1: ESP32-S3 연결 상태 머신 구현

**목표**: ESP32-S3에서 Windows 서버와의 연결 상태를 관리하는 상태 머신 구현

**개발 기간**: 1-1.5일

**세부 목표**:
1. `connection_state.h` 헤더 파일 생성:
   ```c
   typedef enum {
       CONN_STATE_IDLE,          // 서버 미연결 (Essential 모드)
       CONN_STATE_AUTH_PENDING,  // 인증 챌린지 수신, 응답 대기
       CONN_STATE_AUTH_OK,       // 인증 성공, State Sync 대기
       CONN_STATE_SYNC_PENDING,  // State Sync 수신, ACK 대기
       CONN_STATE_CONNECTED,     // 핸드셰이크 완료 (Standard 모드)
       CONN_STATE_ERROR          // 오류 상태
   } connection_state_t;
   ```
2. `connection_state.c` 구현:
   - 상태 전이 함수: `connection_state_transition(new_state)`
   - 현재 상태 조회: `connection_state_get()`
   - 상태 변경 콜백 등록: `connection_state_on_change(callback)`
   - 스레드 안전성: FreeRTOS 뮤텍스 보호
3. 상태 전이 규칙:
   ```
   IDLE ──(AUTH_CHALLENGE 수신)──> AUTH_PENDING
   AUTH_PENDING ──(AUTH_RESPONSE 전송 성공)──> AUTH_OK
   AUTH_PENDING ──(타임아웃 1초)──> IDLE
   AUTH_OK ──(STATE_SYNC 수신)──> SYNC_PENDING
   AUTH_OK ──(타임아웃 1초)──> IDLE
   SYNC_PENDING ──(STATE_SYNC_ACK 전송 성공)──> CONNECTED
   SYNC_PENDING ──(타임아웃 1초)──> IDLE
   CONNECTED ──(Keep-alive 실패 또는 CDC 해제)──> IDLE
   ERROR ──(리셋)──> IDLE
   ```
4. 기능 협상 결과 저장:
   - 서버가 요청한 기능 목록과 ESP32-S3가 수락한 기능 목록을 구조체로 관리
   - Phase 3.5 (모드 전환)에서 모드별 동작 결정에 사용

**신규 파일**:
- `src/board/BridgeOne/main/connection_state.c`
- `src/board/BridgeOne/main/connection_state.h`

**수정 파일**:
- `src/board/BridgeOne/main/CMakeLists.txt`: SRCS에 `connection_state.c` 추가

**참조 문서 및 섹션**:
- `docs/android/styleframe-essential.md` §8 상태 머신 (Essential/Standard 전환 참조)
- `docs/windows/technical-specification-server.md` §3.2 핸드셰이크 프로토콜

**검증**:
- [ ] `connection_state.h/c` 파일 생성됨
- [ ] 6가지 연결 상태 열거형 정의됨
- [ ] 상태 전이 함수 구현됨 (유효하지 않은 전이 거부)
- [ ] FreeRTOS 뮤텍스로 스레드 안전성 보장
- [ ] 상태 변경 콜백 등록 및 호출 동작
- [ ] `idf.py build` 성공

---

## Phase 3.3.2: Authentication 핸들러 구현

**목표**: ESP32-S3와 Windows 서버 양쪽에 Authentication 단계 구현

**개발 기간**: 1일

**세부 목표**:

### ESP32-S3 측

1. `vendor_cdc_handler.c`에 `CMD_AUTH_CHALLENGE` (0x01) 핸들러 추가:
   - JSON 페이로드에서 `challenge` 필드 추출
   - `version` 필드로 프로토콜 버전 호환성 확인
   - 상태를 `AUTH_PENDING` → `AUTH_OK`로 전환
   - 에코백 응답 전송:
     ```json
     {
       "command": "AUTH_RESPONSE",
       "response": "<echo_challenge>",
       "device": "BridgeOne",
       "fw_version": "1.0.0"
     }
     ```
2. 인증 로직 모듈화:
   - `auth_verify()` 함수를 별도로 분리
   - 현재는 단순 에코백, 추후 HMAC 등으로 교체 가능한 구조

### Windows 서버 측

1. `HandshakeService` 클래스 구현:
   - `AuthenticateAsync(CancellationToken)` 메서드
   - 16바이트 랜덤 hex 문자열 생성 (`RandomNumberGenerator`)
   - `CMD_AUTH_CHALLENGE` 프레임 전송
   - 1초 타임아웃으로 `CMD_AUTH_RESPONSE` 대기
   - 응답의 `response` 필드가 challenge와 일치하는지 검증
   - 디바이스 정보 (`device`, `fw_version`) 저장
2. 인증 검증 인터페이스 추상화:
   ```csharp
   public interface IAuthVerifier
   {
       string GenerateChallenge();
       bool VerifyResponse(string challenge, string response);
   }
   ```
   - `EchoBackVerifier`: 현재 구현 (challenge == response)
   - 추후 `HmacVerifier` 등으로 교체 가능

**신규 파일**:
- `src/windows/BridgeOne/Services/HandshakeService.cs`
- `src/windows/BridgeOne/Services/IAuthVerifier.cs`
- `src/windows/BridgeOne/Services/EchoBackVerifier.cs`

**수정 파일**:
- `src/board/BridgeOne/main/vendor_cdc_handler.c`: AUTH_CHALLENGE 핸들러 추가

**참조 문서 및 섹션**:
- `docs/windows/technical-specification-server.md` §3.2 Phase 1: Authentication

**검증**:
- [ ] Windows 서버가 AUTH_CHALLENGE 전송 → ESP32-S3가 AUTH_RESPONSE 응답
- [ ] 응답의 challenge 에코백 일치 확인
- [ ] 인증 성공 시 ESP32-S3 상태가 AUTH_OK
- [ ] 인증 실패 시 (잘못된 응답) 적절한 오류 처리
- [ ] 1초 타임아웃 초과 시 재시도
- [ ] `IAuthVerifier` 인터페이스로 인증 로직 교체 가능 확인

---

## Phase 3.3.3: State Sync 핸들러 구현

**목표**: ESP32-S3와 Windows 서버 양쪽에 State Sync 단계 구현

**개발 기간**: 1-1.5일

**세부 목표**:

### ESP32-S3 측

1. `vendor_cdc_handler.c`에 `CMD_STATE_SYNC` (0x03) 핸들러 추가:
   - JSON 페이로드에서 `features` 배열, `keepalive_ms` 추출
   - 지원 가능한 기능 필터링:
     - `wheel`: 지원 (HID 리포트에 wheel 필드 있음)
     - `drag`: 지원 (buttons 필드의 지속 상태)
     - `right_click`: 지원 (buttons bit1)
     - `multi_cursor`: 미지원 (Phase 4+ 구현 예정)
     - `macro`: 미지원 (Phase 4+ 구현 예정)
     - `extended_keyboard`: 미지원 (Phase 4+ 구현 예정)
   - Keep-alive 주기 저장 (기본 500ms)
   - 상태를 `SYNC_PENDING` → `CONNECTED`로 전환
   - ACK 응답 전송:
     ```json
     {
       "command": "STATE_SYNC_ACK",
       "accepted_features": ["wheel", "drag", "right_click"],
       "mode": "standard"
     }
     ```
2. 수락된 기능 목록을 `connection_state`에 저장

### Windows 서버 측

1. `HandshakeService`에 `StateSyncAsync(CancellationToken)` 메서드 추가:
   - 서버가 지원하는 전체 기능 목록 전송
   - `CMD_STATE_SYNC` 프레임 전송
   - 1초 타임아웃으로 `CMD_STATE_SYNC_ACK` 대기
   - ESP32-S3가 수락한 기능 목록 저장
   - 모드 상태를 "standard"로 전환
2. 전체 핸드셰이크 오케스트레이션:
   ```csharp
   public async Task<HandshakeResult> PerformHandshakeAsync(CancellationToken ct)
   {
       // Phase 1: Authentication
       var authResult = await AuthenticateAsync(ct);
       if (!authResult.Success) return HandshakeResult.AuthFailed;

       // Phase 2: State Sync
       var syncResult = await StateSyncAsync(ct);
       if (!syncResult.Success) return HandshakeResult.SyncFailed;

       return HandshakeResult.Connected(syncResult.AcceptedFeatures);
   }
   ```
3. 핸드셰이크 재시도 정책:
   - 최대 3회 재시도
   - 재시도 간격: 1초 → 2초 → 4초 (지수 백오프)
   - 3회 모두 실패 시 Disconnected 상태로 복귀

**수정 파일**:
- `src/board/BridgeOne/main/vendor_cdc_handler.c`: STATE_SYNC 핸들러 추가
- `src/board/BridgeOne/main/connection_state.c`: 기능 협상 결과 저장
- `src/windows/BridgeOne/Services/HandshakeService.cs`: StateSyncAsync 추가
- `src/windows/BridgeOne/ViewModels/ConnectionViewModel.cs`: 핸드셰이크 상태 반영

**참조 문서 및 섹션**:
- `docs/windows/technical-specification-server.md` §3.2 Phase 2: State Sync
- `docs/android/styleframe-essential.md` §8 상태 머신

**검증**:
- [ ] Authentication 성공 후 State Sync 진행
- [ ] ESP32-S3가 지원 가능한 기능만 수락하여 응답
- [ ] Windows 서버가 수락된 기능 목록 저장
- [ ] 전체 핸드셰이크 (Auth + Sync) 2초 이내 완료
- [ ] 핸드셰이크 완료 시 ESP32-S3 상태가 CONNECTED
- [ ] 핸드셰이크 실패 시 최대 3회 재시도 (지수 백오프)
- [ ] 3회 실패 후 Disconnected 상태 복귀
- [ ] Windows UI에 핸드셰이크 진행 상태 표시

---

## Phase 3.3 핵심 성과

**Phase 3.3 완료 시 달성되는 상태**:
- ✅ Windows 서버와 ESP32-S3 간 2단계 핸드셰이크 완성
- ✅ ESP32-S3 연결 상태 머신으로 연결 상태 체계적 관리
- ✅ 기능 협상으로 양쪽이 지원 가능한 기능 합의
- ✅ 인증 모듈화로 추후 보안 강화 가능
- ✅ Phase 3.4 (Keep-alive)의 선행 조건 충족
