---
title: "BridgeOne Phase 3.4: Keep-alive 시스템 구현"
description: "BridgeOne 프로젝트 Phase 3.4 - Ping-pong 방식 연결 유지 및 끊김 감지 메커니즘 구현"
tags: ["keepalive", "ping-pong", "vendor-cdc", "esp32-s3", "windows", "connection"]
version: "v1.0"
owner: "Chatterbones"
updated: "2026-03-19"
---

# BridgeOne Phase 3.4: Keep-alive 시스템 구현

**개발 기간**: 2-3일

**목표**: Ping-pong 방식으로 연결 유지를 모니터링하고, 연결 끊김을 신속히 감지하여 자동 복구합니다.

**핵심 성과물**:
- 0.5초 주기 ping-pong 메커니즘
- 연속 3회 실패 시 연결 끊김 판정
- 지수 백오프 자동 재연결
- RTT(왕복 지연) 측정 및 UI 표시

**선행 조건**: Phase 3.3 (핸드셰이크 프로토콜) 완료

---

## 📋 Phase 3.3 완료 사항 요약

- Authentication + State Sync 2단계 핸드셰이크 완성
- ESP32-S3 연결 상태 머신 (`connection_state.c/h`) 구동
- 핸드셰이크 완료 시 ESP32-S3 상태: CONNECTED (Standard 모드)
- 기능 협상 결과 저장 (wheel, drag, right_click 등)

### ⚠️ Phase 3.3.3 구현에서 적용된 사항 (Phase 3.4 영향)

1. **`HandshakeService`에 `PerformHandshakeAsync()` 오케스트레이션 메서드가 추가됨**:
   - Auth + State Sync 전체 핸드셰이크를 한 번에 수행하며, 실패 시 최대 3회 재시도(지수 백오프 1초→2초→4초)를 내장하고 있음.
   - Phase 3.4에서 Keep-alive 실패 후 재연결 시, `PerformHandshakeAsync()`를 호출하면 재시도 로직을 별도로 구현할 필요 없음.
   - 반환 타입: `HandshakeResult` (Success, FailReason, AcceptedFeatures, Mode, DeviceName, FirmwareVersion 포함).

2. **`SyncResult` 타입이 추가됨 (계획에 없던 신규 타입)**:
   - `StateSyncAsync()`의 반환 타입으로 `SyncResult` 클래스가 `HandshakeService.cs`에 정의됨.
   - `SyncResult`에 `AcceptedFeatures` (string[])과 `Mode` (string)이 포함되어, Keep-alive 서비스가 현재 활성 기능을 참조할 수 있음.

3. **`HandshakeFailReason` enum 추가 (`AuthFailed`, `SyncFailed`, `MaxRetriesExceeded`)**:
   - Phase 3.4에서 재연결 시 핸드셰이크 실패 원인을 `HandshakeResult.FailReason`으로 구분하여 UI 메시지를 다르게 표시할 수 있음.

4. **`ConnectionViewModel`에 핸드셰이크 흐름 통합이 아직 안 됨**:
   - Phase 3.3.3에서 `ConnectionViewModel.cs` 수정을 계획했으나, CdcConnectionService와의 통합이 선행되어야 해서 미구현.
   - Phase 3.4에서 `KeepAliveService`를 구현할 때 CdcConnectionService의 Connect 흐름에 `PerformHandshakeAsync()` 호출을 삽입하고, 그 결과를 ConnectionViewModel에 반영하는 것을 권장.

### ⚠️ Phase 3.2 E2E 검증에서 적용된 사항 (Phase 3.4 영향)

1. **CDC TX FIFO 대기 로직 (이미 적용됨)**:
   - `vendor_cdc_send_frame()`에서 FIFO 부족 시 최대 20ms 대기 후 전송
   - Phase 3.4 PONG 응답의 < 5ms 지연 요구에 긍정적 영향: FIFO 부족으로 인한 프레임 누락 방지
   - 단, 20ms 대기가 발생하면 5ms 목표를 초과할 수 있으므로, PONG 응답은 가능한 경량으로 유지할 것

2. **ConnectionViewModel 비대화 주의**:
   - Phase 3.2.4 진단 코드(330줄) + Phase 3.3 핸드셰이크 상태가 이미 `ConnectionViewModel`에 존재
   - Phase 3.4에서 RTT/연결 품질 표시를 추가하면 ViewModel이 더 비대해짐
   - **권장**: RTT/품질 로직은 `KeepAliveService`에 구현하고, ViewModel은 바인딩 프로퍼티만 노출

---

## Keep-alive 프로토콜 개요

### 동작 흐름

```
Windows 서버                          ESP32-S3 동글
     │                                     │
     │  (핸드셰이크 완료 후 시작)            │  (CONNECTED 상태)
     │                                     │
     │  ── 매 0.5초마다 ──                  │
     │                                     │
     │  CMD_PING (0x10)                    │
     │  {"command":"PING",                 │
     │   "timestamp":1234567890}           │
     │  ──────────────────────────────────> │
     │                                     │
     │           CMD_PONG (0x11)            │
     │  {"command":"PONG",                 │
     │   "timestamp":1234567890}           │  (timestamp 에코백)
     │  <────────────────────────────────── │
     │                                     │
     │  RTT 계산: 현재시각 - timestamp      │
     │                                     │
```

### 실패 감지

```
PING 전송 → 1초 내 PONG 미수신 → 실패 카운트 +1
  ├─ 실패 1회: 경고 로그
  ├─ 실패 2회: UI에 "연결 불안정" 표시
  └─ 실패 3회: 연결 끊김 판정 → Essential 모드 복귀

재연결: 지수 백오프 (1초 → 2초 → 4초 → 8초 → 최대 30초)
```

---

## Phase 3.4.1: Windows 서버 Keep-alive 서비스

**목표**: Windows 서버에서 주기적 PING 전송 및 PONG 응답 모니터링

**개발 기간**: 1-1.5일

**세부 목표**:
1. `KeepAliveService` 클래스 구현:
   - 핸드셰이크 완료 시 자동 시작
   - `System.Threading.PeriodicTimer`로 0.5초(500ms) 주기 PING 전송:
     ```json
     {"command": "PING", "timestamp": <unix_ms>}
     ```
   - PONG 응답 대기 (1초 타임아웃)
   - 응답 수신 시 RTT 계산: `현재시각 - echo된 timestamp`
2. 실패 카운팅:
   - PONG 미수신 또는 타임아웃 시 `failureCount++`
   - PONG 정상 수신 시 `failureCount = 0` 리셋
   - 연속 3회 실패 시 `ConnectionLost` 이벤트 발생
3. 연결 끊김 시 동작:
   - Keep-alive 타이머 중지
   - `CdcConnectionService`에 연결 해제 통보
   - 자동 재연결 시도 시작
4. 자동 재연결:
   - 지수 백오프: 1초 → 2초 → 4초 → 8초 → 16초 → 30초 (최대)
   - CDC 포트 재열기 → 핸드셰이크 재시도
   - 재연결 성공 시 Keep-alive 타이머 재시작
5. 연결 품질 모니터링:
   - 최근 10개 RTT의 이동 평균 계산
   - UI에 연결 품질 표시 (양호/보통/불안정)

**신규 파일**:
- `src/windows/BridgeOne/Services/KeepAliveService.cs`

**수정 파일**:
- `src/windows/BridgeOne/ViewModels/ConnectionViewModel.cs`: RTT 및 연결 품질 표시
- `src/windows/BridgeOne/Services/HandshakeService.cs`: Keep-alive 서비스와 연동
- `src/windows/BridgeOne/App.xaml.cs`: DI에 KeepAliveService 등록

**참조 문서 및 섹션**:
- `docs/windows/technical-specification-server.md` §3.3 Keep-alive 정책
- `docs/windows/technical-specification-server.md` §3.1 연결 관리 기술 구현

**검증**:
- [x] 핸드셰이크 완료 후 0.5초 주기 PING 자동 시작
- [x] PONG 응답 수신 시 RTT 정상 계산
- [x] 연속 3회 PONG 미수신 시 `ConnectionLost` 이벤트 발생
- [x] 연결 끊김 후 지수 백오프 재연결 시도
- [x] 재연결 성공 시 Keep-alive 재시작
- [x] UI에 RTT 및 연결 품질 표시
- [x] `dotnet build` 성공

---

## Phase 3.4.2: ESP32-S3 PING/PONG 핸들러 및 자체 타임아웃

### ⚠️ Phase 3.4.1 구현에서 적용된 사항 (Phase 3.4.2 영향)

1. **`HandshakeService.cs`는 수정하지 않음**:
   - 계획에서는 HandshakeService를 Keep-alive와 연동하기 위해 수정하도록 되어 있었으나, `KeepAliveService`가 DI로 `HandshakeService`를 직접 주입받아 `PerformHandshakeAsync()`를 호출하는 방식으로 구현하여 HandshakeService 자체 수정이 불필요했음.
   - Phase 3.4.2에서 HandshakeService 수정을 참조하는 부분이 있다면 무시해도 됨.

2. **`ConnectionViewModel`에서 핸드셰이크-Keep-alive 연동이 완료됨**:
   - Phase 3.3.3에서 미구현이었던 "핸드셰이크 흐름 통합"이 Phase 3.4.1에서 구현됨.
   - `RunHandshakeTestAsync()` 성공 시 `_keepAliveService.Start()` 자동 호출.
   - 연결 상태가 `Disconnected`/`Error`로 변경 시 `_keepAliveService.Stop()` 자동 호출.

3. **PING 전송 시 JSON 페이로드 포함**:
   - 기존 수동 PING 테스트(`SendPingAsync`)는 페이로드 없이 전송했으나, `KeepAliveService`는 `{"command":"PING","timestamp":<unix_ms>}` JSON 페이로드를 포함하여 전송함.
   - Phase 3.4.2의 ESP32-S3 PING 핸들러는 페이로드 없는 PING과 JSON 페이로드 PING 모두 처리할 수 있어야 함.

4. **`ConnectionQuality` enum 및 이벤트 타입이 `KeepAliveService.cs`에 정의됨**:
   - `ConnectionQuality` (Unknown, Good, Fair, Unstable, Disconnected)
   - `RttUpdatedEventArgs` (CurrentRttMs, AverageRttMs)
   - `ReconnectAttemptEventArgs` (Attempt, DelaySeconds)
   - Phase 3.4.2에서 이 타입들을 참조할 필요 없음 (서버 측 전용).

5. **재연결 시 `CdcConnectionService.TryScanAndConnect()` 호출**:
   - `KeepAliveService`가 재연결 시 CDC 포트를 다시 스캔하므로, ESP32-S3 측에서 USB 재열거가 필요한 경우 타이밍에 주의.
   - ESP32-S3는 CONNECTED → IDLE 전환 후에도 USB CDC 포트를 유지해야 서버 재연결이 가능.

**목표**: ESP32-S3에서 PING 수신 시 즉시 PONG 응답 및 서버 타임아웃 감지

**개발 기간**: 1-1.5일

**세부 목표**:
1. `vendor_cdc_handler.c`에 PING/PONG 핸들러 추가:
   - `CMD_PING` (0x10) 수신 시:
     - JSON에서 `timestamp` 필드 추출
     - 즉시 `CMD_PONG` (0x11) 응답:
       ```json
       {"command": "PONG", "timestamp": <echo_timestamp>}
       ```
     - 마지막 PING 수신 시각 기록
   - 응답 지연 최소화: cJSON 사용 최소화, 가능하면 직접 문자열 조립
2. 서버 타임아웃 자체 감지:
   - `connection_state.c`에 타임아웃 감시 로직 추가
   - CONNECTED 상태에서 3초간 PING 미수신 시:
     - 상태를 `CONNECTED` → `IDLE`로 전환 (Essential 모드 복귀)
     - 디버그 로그: "Server keep-alive timeout, reverting to Essential mode"
   - 타임아웃 체크: vendor_cdc_task의 메인 루프에서 주기적 확인 (100ms 주기)
3. DTR 신호 연동:
   - `tud_cdc_line_state_cb()`에서 DTR=false 감지 시 즉시 IDLE 전환
   - 서버가 비정상 종료해도 DTR 신호로 빠르게 감지 가능

### ⚠️ Phase 3.4.2 구현에서 적용된 사항 (후속 Phase 영향)

1. **`connection_state.c`는 수정하지 않음** → **Phase 3.5.1 영향**:
   - 계획에서는 `connection_state.c`에 타임아웃 감시 로직을 추가하도록 되어 있었으나, Keep-alive 타임아웃 변수(`s_last_ping_time_us`)와 타임아웃 체크 로직 모두 `vendor_cdc_handler.c`의 태스크 루프에서 동작하므로, 불필요한 API 추가 없이 `vendor_cdc_handler.c`에서 완결 처리함.
   - Phase 3.5.1에서 `connection_state.c/h`에 모드 관리 함수를 추가할 때, Keep-alive 관련 코드가 이 파일에 없음을 인지해야 함.
   - Keep-alive 타임아웃 및 DTR 해제 시 `connection_state_reset()` 호출 경로:
     - `vendor_cdc_handler.c` → 3초 PING 미수신 → `connection_state_reset()`
     - `usb_cdc_log.c` → DTR=false → `connection_state_reset()`
   - Phase 3.5.1의 모드 전환 콜백(IDLE 진입 → ESSENTIAL)은 위 두 경로 모두에서 자동 트리거됨.

2. **PONG 응답 방식: payload 에코백 유지 (JSON 조립하지 않음)** → **후속 Phase 영향 없음**:
   - 계획에서는 JSON에서 `timestamp` 필드를 추출하여 새 JSON으로 PONG을 조립하도록 되어 있었으나, 기존에 구현된 payload 에코백 방식을 유지함.
   - 서버가 보낸 `{"command":"PING","timestamp":1234567890}`을 그대로 돌려보내므로 timestamp 에코백은 정확하게 작동함.
   - 이 방식이 cJSON 파싱/조립 오버헤드가 없어 PONG 응답 지연 최소화(< 5ms)에 더 유리함.
   - 페이로드 없는 PING과 JSON 페이로드 PING 모두 동일하게 처리됨.

3. **PING 핸들러 로그 레벨 변경: `ESP_LOGI` → `ESP_LOGD`** → **후속 Phase 영향 없음**:
   - 0.5초마다 수신되는 PING의 로그를 INFO → DEBUG로 변경하여 로그 폭주를 방지함.
   - 디버깅 시 `esp_log_level_set("VENDOR_CDC", ESP_LOG_DEBUG)`로 활성화 가능.

4. **`vendor_cdc_task` 큐 대기 방식 변경: `portMAX_DELAY` → `pdMS_TO_TICKS(100)`** → **Phase 3.5 참고 정보**:
   - 기존에는 프레임이 올 때까지 무한 대기했으나, 100ms 타임아웃으로 변경하여 주기적으로 Keep-alive 타임아웃을 체크함.
   - 이 변경은 태스크의 CPU 사용량에 미미한 영향만 줌 (100ms마다 상태 체크 1회).
   - Phase 3.5에서 `vendor_cdc_handler.c`를 직접 수정하지 않으므로 실질적 영향 없음.

**수정 파일**:
- `src/board/BridgeOne/main/vendor_cdc_handler.c`: PING 시각 기록 + Keep-alive 타임아웃 체크
- `src/board/BridgeOne/main/usb_cdc_log.c`: DTR 해제 시 connection_state_reset() 호출

**미수정 파일** (계획과 다름):
- `src/board/BridgeOne/main/connection_state.c`: 수정 불필요 (타임아웃 로직이 vendor_cdc_handler.c에 구현됨)

**참조 문서 및 섹션**:
- `docs/windows/technical-specification-server.md` §3.3 Keep-alive 정책
- `src/board/BridgeOne/main/usb_cdc_log.c` - 기존 DTR 감지 코드 참조

**검증**:
- [x] CMD_PING 수신 시 즉시 CMD_PONG 응답 전송
- [x] timestamp 에코백 정확성 확인
- [x] PONG 응답 지연시간 < 5ms (ESP32-S3 내부 처리)
- [x] 3초간 PING 미수신 시 Essential 모드 자동 복귀
- [x] DTR=false 시 즉시 IDLE 상태 전환
- [x] `idf.py build` 성공

---

## Phase 3.4 E2E 검증

1. **정상 동작**: 핸드셰이크 → 0.5초 주기 ping-pong 안정 동작 → RTT < 10ms
2. **케이블 분리**: USB 케이블 제거 → 약 1.5~3초 내 연결 끊김 판정 → Essential 복귀
3. **서버 종료**: Windows 서버 프로세스 종료 → DTR 해제 → 즉시 Essential 복귀
4. **케이블 재연결**: 분리 → 재연결 → 자동 재핸드셰이크 → Keep-alive 재개
5. **서버 재시작**: 서버 종료 → 재시작 → 지수 백오프 → 핸드셰이크 → Connected

---

## Phase 3.4 핵심 성과

**Phase 3.4 완료 시 달성되는 상태**:
- ✅ 실시간 연결 상태 모니터링 (0.5초 주기)
- ✅ 연결 끊김 신속 감지 (최대 3초)
- ✅ 자동 재연결 메커니즘 (지수 백오프)
- ✅ RTT 측정으로 연결 품질 가시화
- ✅ Phase 3.5 (모드 전환)의 선행 조건 충족
