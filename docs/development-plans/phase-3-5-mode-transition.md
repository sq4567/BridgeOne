---
title: "BridgeOne Phase 3.5: Essential ↔ Standard 모드 전환 시스템"
description: "BridgeOne 프로젝트 Phase 3.5 - ESP32-S3에서 Essential/Standard 모드 자동 전환 및 모드별 HID 동작 변경"
tags: ["mode-transition", "essential", "standard", "esp32-s3", "hid", "wheel"]
version: "v1.0"
owner: "Chatterbones"
updated: "2026-03-19"
---

# BridgeOne Phase 3.5: Essential ↔ Standard 모드 전환 시스템

**개발 기간**: 3-4일

**목표**: ESP32-S3에서 Essential ↔ Standard 모드 자동 전환을 구현하고, 모드에 따라 HID 동작을 변경합니다.

**핵심 성과물**:
- ESP32-S3 모드 상태 관리 (bridge_mode_t)
- 모드 전환 트리거 및 자동 전환
- 모드 전환 시 눌린 키/버튼 자동 해제 (stuck 방지)
- 전환 중 입력 손실 없음 보장
- ESP32-S3 → Android 역방향 UART 알림 시스템
- Android 모드 인지 및 Essential/Standard 모드별 UI 전환
- Essential 모드 기능 제한은 Android 앱 UI 레벨에서 구현 (펌웨어 필터링 없음)

**선행 조건**: Phase 3.4 (Keep-alive 시스템) 완료

---

## 📋 Phase 3.4 완료 사항 요약

- 0.5초 주기 ping-pong Keep-alive 안정 동작
- 연속 3회 실패 시 연결 끊김 감지
- 끊김 시 ESP32-S3가 자체적으로 IDLE 상태 복귀
- 지수 백오프 자동 재연결

### ⚠️ Phase 3.3.3 구현에서 적용된 사항 (Phase 3.5 영향)

1. **ESP32-S3 지원 기능 목록이 `vendor_cdc_handler.c`에 `supported_features[]` 배열로 하드코딩됨**:
   - 현재 값: `{"wheel", "drag", "right_click"}`.
   - `is_feature_supported()` 헬퍼 함수로 지원 여부를 판별.
   - Phase 3.5에서 모드별 기능 필터링 시, `connection_state_get_features()`로 수락된 기능 목록을 조회하면 이 배열에 있는 기능만 반환됨.
   - Phase 4+에서 `multi_cursor`, `macro`, `extended_keyboard` 등 새 기능을 추가할 때는 이 배열에 항목을 추가해야 함.

2. **`connection_state_set_features()`가 State Sync 핸들러에서 실제로 호출됨**:
   - Phase 3.3.1에서 함수를 정의만 해두었고, Phase 3.3.3에서 실제 데이터를 채워 호출하도록 구현 완료.
   - `connection_features_t`에 `requested[]`, `requested_count`, `accepted[]`, `accepted_count`, `keepalive_ms` 모두 정상 저장됨.
   - Phase 3.5.1에서 `connection_state_get_features()`로 수락된 기능을 조회하여 모드별 세부 제어에 활용 가능.
   - IDLE 복귀 시 features가 자동 초기화되므로 Essential 모드 복귀 시 별도 정리 코드 불필요.

3. **State Sync 완료 시 ESP32-S3 상태가 `CONN_STATE_CONNECTED`로 전이됨**:
   - Phase 3.5.1의 모드 전환 트리거 조건 (`CONN_STATE_CONNECTED` 진입 시 → `BRIDGE_MODE_STANDARD`)이 정상 동작할 기반이 마련됨.

### ⚠️ Phase 3.4.2 구현에서 적용된 사항 (Phase 3.5 영향)

1. **Keep-alive 타임아웃 로직이 `connection_state.c`가 아닌 `vendor_cdc_handler.c`에 구현됨**:
   - `vendor_cdc_handler.c`에 `s_last_ping_time_us` 변수와 `KEEPALIVE_TIMEOUT_US` (3초) 상수가 정의됨.
   - CONNECTED 상태에서 3초간 PING 미수신 시 `connection_state_reset()` 호출 → IDLE 전환.
   - Phase 3.5.1에서 `connection_state`의 상태 변경 콜백(IDLE 진입 → `BRIDGE_MODE_ESSENTIAL`)을 등록하면, Keep-alive 타임아웃 시 자동으로 모드 전환이 트리거됨.
   - `connection_state.c`에는 Keep-alive 관련 함수가 없으므로, Phase 3.5.1에서 `connection_state.c/h`를 수정할 때 Keep-alive 타이머를 고려할 필요 없음.

2. **DTR=false 시 `connection_state_reset()`이 `usb_cdc_log.c`에서 호출됨**:
   - `tud_cdc_line_state_cb()`에서 DTR 해제 감지 → 즉시 IDLE 전환.
   - Phase 3.5.1의 모드 전환 콜백이 이 경로에서도 정상 트리거됨 (CDC 해제 → IDLE → BRIDGE_MODE_ESSENTIAL).
   - 모드 전환 다이어그램의 "DTR=false" 트리거 경로가 이미 구현 완료.

3. **`vendor_cdc_task` 큐 대기가 `portMAX_DELAY` → `pdMS_TO_TICKS(100)`으로 변경됨**:
   - 100ms 주기로 Keep-alive 타임아웃을 체크하는 구조로 변경됨.
   - Phase 3.5에서 `vendor_cdc_handler.c`를 직접 수정하지 않으므로 영향 없음. 참고 정보로만 기록.

### 📌 이전 Phase에서의 참고 사항

- **Phase 3.2.4에서 수정된 payload 버퍼 +1 확장**: `vendor_cdc_handler.c`의 payload 버퍼가 `VCDC_MAX_PAYLOAD_SIZE + 1`로 변경됨. Phase 3.5에서 `vendor_cdc_handler.c`를 직접 수정하지 않으므로 영향 없음
- **Phase 3.5는 주로 `connection_state.c/h` (신규)와 `hid_handler.c`를 수정**: Phase 3.2.4 변경 파일과 겹치지 않음

---

## 모드 전환 개요

### 상태 다이어그램

```
                ┌──────────────────────────────────────────────┐
                │                                              │
                ▼                                              │
        ┌───────────────┐    핸드셰이크 완료     ┌──────────────────┐
        │   ESSENTIAL   │ ─────────────────────> │    STANDARD      │
        │   (기본 모드)  │                        │   (서버 연결)     │
        │               │ <───────────────────── │                  │
        └───────────────┘    Keep-alive 실패     └──────────────────┘
                              CDC 해제
                              DTR=false
```

### 모드별 기능 비교

> **표 읽는 방법**: Essential 모드의 ❌는 ESP32-S3 펌웨어가 해당 기능을 지원하지 않는다는 의미가 아닙니다.
> Android UI에서 해당 기능을 사용하는 컴포넌트(버튼, 제스처 영역 등)를 **표시하지 않는다**는 의미입니다.
> 펌웨어 자체는 모든 HID 입력을 처리할 수 있으나, Essential 모드에서는 Android 앱이 해당 UI를 노출하지 않습니다.

| 기능 | Essential 모드 | Standard 모드 |
|------|---------------|--------------|
| 마우스 이동 (X/Y) | ✅ UI 제공 | ✅ UI 제공 |
| 좌클릭 | ✅ UI 제공 | ✅ UI 제공 |
| 우클릭 | ❌ UI 미표시 | ✅ UI 제공 |
| 중앙 클릭 | ❌ UI 미표시 | ✅ UI 제공 |
| 휠 스크롤 | ❌ UI 미표시 | ✅ UI 제공 |
| 클릭-드래그 | ❌ UI 미표시 | ✅ UI 제공 |
| 모든 키보드 키 | ❌ UI 미표시 (기본 키만 표시) | ✅ UI 제공 |
| 멀티 커서 | ❌ UI 미표시 | ⏳ (Phase 4+) |
| 매크로 | ❌ UI 미표시 | ⏳ (Phase 4+) |

---

## Phase 3.5.1: ESP32-S3 모드 상태 관리 ✅ 완료

**목표**: 브릿지 모드 열거형 정의 및 모드 전환 트리거 구현

**개발 기간**: 1-1.5일

**세부 목표**:
1. `connection_state.h`에 모드 열거형 추가:
   ```c
   typedef enum {
       BRIDGE_MODE_ESSENTIAL,  // 기본 모드 (서버 미연결)
       BRIDGE_MODE_STANDARD    // 서버 연결 모드
   } bridge_mode_t;
   ```
2. 모드 관리 함수:
   - `bridge_mode_get()`: 현재 모드 조회
   - `bridge_mode_on_change(callback)`: 모드 변경 콜백 등록
   - `bridge_mode_name(mode)`: 모드 이름 문자열 반환 (디버그용)
   - `bridge_mode_is_feature_active(feature_name)`: 개별 기능 활성화 여부 조회
3. 모드 전환 트리거 연동:
   - `connection_state`의 상태 변경 콜백에서 모드 전환:
     - `CONN_STATE_CONNECTED` 진입 시 → `BRIDGE_MODE_STANDARD`
     - `CONN_STATE_IDLE` 진입 시 → `BRIDGE_MODE_ESSENTIAL`
   - 전환 시 디버그 로그 출력:
     - "Mode changed: ESSENTIAL -> STANDARD"
     - "Mode changed: STANDARD -> ESSENTIAL"
4. 수락된 기능 목록 기반 세부 제어:
   - Phase 3.3에서 저장된 `accepted_features`에 따라 기능 활성화
   - 예: `wheel`이 수락되지 않았으면 Standard에서도 wheel=0

**수정 파일**:
- `src/board/BridgeOne/main/connection_state.c/h`: 모드 관리 함수 추가

**참조 문서 및 섹션**:
- `docs/android/styleframe-essential.md` §8 상태 머신
- `docs/android/styleframe-essential.md` §3 허용/비활성 기능

**검증**:
- [x] `bridge_mode_t` 열거형 정의됨
- [x] 핸드셰이크 완료 시 Standard 모드 전환 확인 (로그)
- [x] Keep-alive 실패 시 Essential 모드 복귀 확인 (로그)
- [x] 모드 변경 콜백 정상 호출
- [x] 수락된 기능 목록 기반 세부 제어 동작
- [x] `idf.py build` 성공

### ⚠️ Phase 3.5.1 구현에서 적용된 사항 (Phase 3.5.2 영향)

1. **`bridge_mode_set()`가 외부 공개되지 않음 → `bridge_mode_set_internal()`로 내부 전용**:
   - 모드 전환은 연결 상태 변경에 의해서만 자동 수행됩니다.
   - Phase 3.5.2에서 모드를 직접 변경할 필요 없이 `bridge_mode_get()`으로 현재 모드만 조회하면 됩니다.

2. **모드 자동 전환이 `connection_state_transition()`/`connection_state_reset()` 내부에서 직접 호출됨**:
   - 별도의 콜백 등록 방식이 아닌, 상태 전이 함수 내부에서 `bridge_mode_auto_transition()`을 직접 호출합니다.
   - 이로 인해 `connection_state_on_change()` 단일 콜백 슬롯이 외부 사용자(vendor_cdc_handler 등)에게 온전히 보존됩니다.
   - 모드 전환은 외부 콜백보다 **먼저** 실행되므로, 외부 콜백에서 `bridge_mode_get()`을 호출하면 이미 전환된 모드를 읽을 수 있습니다.

3. **`bridge_mode_is_feature_active(feature_name)` 헬퍼 함수 추가**:
   - Phase 3.5.2에서 HID 필터링 시 이 함수로 개별 기능 활성화 여부를 간편히 조회 가능합니다.
   - Essential 모드 → 항상 `false` 반환
   - Standard 모드 → `accepted_features`에 해당 기능이 포함되어 있으면 `true`
   - 사용 예: `bridge_mode_is_feature_active("wheel")`, `bridge_mode_is_feature_active("right_click")`
   - Phase 3.5.2의 `hid_handler.c`에서 `bridge_mode_get()` + 수동 feature 조회 대신 이 함수 하나로 처리 가능

4. **`bridge_mode_on_change(callback)` 콜백이 제공됨**:
   - Phase 3.5.2에서 모드 전환 시 눌린 키/버튼 해제 리포트를 전송해야 하는 경우, 이 콜백을 등록하여 전환 시점을 감지할 수 있습니다.
   - 콜백 시그니처: `void callback(bridge_mode_t old_mode, bridge_mode_t new_mode)`

---

## Phase 3.5.2: 모드별 HID 동작 변경 ✅ 완료

**목표**: ESP32-S3의 HID 리포트 생성 시 현재 모드에 따라 동작을 변경

**개발 기간**: 1-1.5일

**세부 목표**:
1. `hid_handler.c`의 프레임 처리 로직:
   - `processBridgeFrame()` 함수는 모드에 관계없이 프레임 값을 **그대로** HID 리포트로 변환하여 전송
   - **펌웨어에서 Essential 모드 필터링을 하지 않음** — Android 앱이 모드에 따라 UI를 표시/숨김하여 기능을 제어
   - ESP32-S3는 순수한 브릿지 역할: 받은 프레임을 그대로 HID로 전달
2. 모드 전환 시 입력 손실 방지:
   - 현재 처리 중인 프레임 완료 후 모드 전환
   - 전환 즉시 키 해제 리포트 전송 (눌린 키가 있는 경우)
   - 전환 즉시 버튼 해제 리포트 전송 (눌린 버튼이 있는 경우)

**수정 파일**:
- `src/board/BridgeOne/main/hid_handler.c`: 모드별 필터링 로직 추가
- `src/board/BridgeOne/main/hid_handler.h`: `hid_register_mode_callback()` 선언 추가
- `src/board/BridgeOne/main/BridgeOne.c`: `app_main()`에서 콜백 등록 호출 추가

**참조 문서 및 섹션**:
- `docs/android/styleframe-essential.md` §3 허용/비활성 기능
- `docs/android/styleframe-essential.md` §7 구현 메모 (HID/프로토콜)
- `src/board/BridgeOne/main/hid_handler.c` - 기존 프레임 처리 로직

**검증**:
- [x] 모든 모드에서 프레임 값이 필터링 없이 그대로 HID 리포트로 전송됨
- [x] 모드 전환 시 눌린 키/버튼 해제 리포트 전송
- [x] 모드 전환 중 프레임 손실 없음
- [x] `idf.py build` 성공

### 📌 Phase 3.5.2 구현 참고 사항

- **펌웨어 필터링 없음**: ESP32-S3는 Android 앱으로부터 수신한 프레임을 모드에 관계없이 그대로 HID 리포트로 전달합니다. Essential 모드의 기능 제한은 Android 앱이 해당 UI를 숨김으로써 구현됩니다.
- **모드 전환 콜백 슬롯 점유**: `bridge_mode_on_change()`에 `hid_on_mode_change()` 콜백이 등록됨. 단일 슬롯이므로 다른 모듈에서 덮어쓰면 입력 해제 기능이 해제됨에 주의.
- Phase 3.5.3(Windows 서버 UI)에는 직접적인 영향 없음.

---

## Phase 3.5.3: Windows 서버 연결 상태 표시 ✅ 완료

> ⚠️ **설계 원칙**: Essential 모드는 "서버가 실행되지 않는 상태"를 의미합니다. 서버가 실행 중이라면 그 자체가 이미 Standard 경로 위에 있으므로, 서버 UI에 "Essential"을 표시하는 것은 논리적으로 불가능합니다. Windows 서버의 UI 상태는 "Standard (연결됨)" vs "연결 없음" 두 가지만 존재합니다.

### ⚠️ Phase 3.4.3 구현에서 선행 완료된 사항

다음 항목들이 Phase 3.4.3에서 이미 구현 완료되어 있습니다:

1. **`Esp32Mode` 열거형 및 `ModeDisplayText` / `ModeBrush` 프로퍼티** (`ConnectionViewModel.cs`):
   - `Esp32Mode.Standard` → `"Standard 모드"` (녹색), `Esp32Mode.Disconnected` → `"연결 없음"` (회색)
   - 핸드셰이크 완료 시 `Esp32Mode = Standard`, Keep-alive 끊김 / 연결 해제 시 `Esp32Mode = Disconnected` 설정

2. **`IsReconnecting` / `ReconnectStatusText` 프로퍼티** (`ConnectionViewModel.cs`):
   - `KeepAliveService.ConnectionLost` 이벤트 → `IsReconnecting = true`
   - `KeepAliveService.Reconnected` 또는 연결 해제 → `IsReconnecting = false`
   - `KeepAliveService.ReconnectAttempt` 이벤트 → `ReconnectStatusText = "재연결 중... (시도 #N, Xs 대기)"`
   - XAML에서 `IsReconnecting` 바인딩으로 노란색 재연결 메시지 표시/숨김 처리 완료

3. **`MainWindow.xaml` UI 바인딩**:
   - 모드 표시 (색상 점 + `ModeDisplayText`), RTT 표시, 연결 품질 표시, 재연결 메시지 표시 모두 구현 완료

**목표**: Windows 서버에서 ESP32-S3 연결 상태를 관리하고 UI에 반영

**세부 목표**:
1. `ConnectionViewModel`에 연결 상태 반영:
   - ~~핸드셰이크 완료 시 → "Standard 모드 활성" (녹색) 표시~~ ✅ Phase 3.4.3 완료
   - ~~Keep-alive 타임아웃 / DTR=false 시 → "ESP32 연결 없음" (회색) 표시~~ ✅ Phase 3.4.3 완료
   - ~~재연결 시도 중 → "재연결 중..." (노란색) 표시~~ ✅ Phase 3.4.3 완료
2. UI에 현재 연결 상태 표시:
   - ~~상태바에 "Standard" (연결됨) 또는 "연결 없음" 레이블~~ ✅ Phase 3.4.3 완료
   - ~~Standard 상태: 핸드셰이크에서 협상된 활성 기능 목록 표시~~ ✅ Phase 3.5.3 완료
3. ~~ESP32-S3로부터 `CMD_MODE_NOTIFY` (0x20) 수신 처리~~ → **삭제** (아래 사유 참조)

### 🚫 CMD_MODE_NOTIFY (0x20) 수신 처리 삭제 사유

이 항목은 구현하지 않고 삭제합니다. 이유:

1. **ESP32 전송 코드 부재**: `VCDC_CMD_MODE_NOTIFY`는 `vendor_cdc_handler.h`에 상수만 정의되어 있고, ESP32에서 실제로 이 명령을 전송하는 코드가 없습니다. 수신 처리만 구현해도 동작하지 않습니다.
2. **기존 메커니즘으로 충분**: 서버는 이미 두 가지 경로로 연결 끊김을 감지합니다:
   - **KeepAliveService**: PING/PONG 타임아웃 시 `ConnectionLost` 이벤트 → `Esp32Mode = Disconnected`
   - **CdcConnectionService**: CDC 직렬 포트 해제 시 `StateChanged` → `ConnectionState.Disconnected` → 상태 초기화
3. **논리적 모순**: ESP32가 Keep-alive 타임아웃으로 IDLE에 진입했다면, 그 시점에서 CDC 연결 자체가 이미 끊겼거나 서버도 동시에 타임아웃을 감지합니다. MODE_NOTIFY를 보낼 수 있는 상황이면 연결이 살아있다는 뜻이므로 모순입니다.

> **후속 영향**: 이 결정은 후속 Phase에 영향을 주지 않습니다. `VCDC_CMD_MODE_NOTIFY` 상수는 헤더에 남겨두되, 필요 시 Phase 4+에서 재검토할 수 있습니다.

**수정 파일**:
- `src/windows/BridgeOne/ViewModels/ConnectionViewModel.cs`: `ActiveFeatures`, `ActiveFeaturesText`, `IsActiveFeaturesVisible` 프로퍼티 추가
- `src/windows/BridgeOne/MainWindow.xaml`: 활성 기능 목록 UI (Standard 모드에서만 녹색으로 표시)

**검증**:
- [x] 핸드셰이크 완료 시 UI에 "Standard" (녹색) 표시 ← Phase 3.4.3 완료
- [x] Keep-alive 타임아웃 또는 DTR=false 시 UI에 "연결 없음" 표시 ← Phase 3.4.3 완료
- [x] 활성화된 기능 목록 UI 표시 (Standard 상태에서만)
- [x] 연결 해제 시 기능 목록 초기화

### 📌 Phase 3.5.3 구현 참고 사항

- **`ActiveFeatures` 프로퍼티**: 핸드셰이크 성공 시 `result.AcceptedFeatures`를 저장. 연결 해제/Keep-alive 끊김 시 빈 배열로 초기화.
- **`ActiveFeaturesText`**: `string.Join(", ", ActiveFeatures)`로 쉼표 구분 표시. 비어있으면 `"--"`.
- **`IsActiveFeaturesVisible`**: `Esp32Mode == Standard && ActiveFeatures.Length > 0`일 때만 `true`.
- **UI 위치**: ESP32 모드 표시와 RTT 표시 사이에 녹색(#10B981)으로 기능 목록을 표시.

---

## Phase 3.5.4: ESP32-S3 → Android 역방향 UART 알림 시스템

**목표**: ESP32-S3가 모드 변경 시 UART TX를 통해 Android에 알림을 전송하고, Android가 이를 수신/파싱하여 현재 모드를 인지

**개발 기간**: 2-2.5일

**선행 조건**: Phase 3.1에서 seq 범위 0~253 제한 완료 (0xFE/0xFF 예약 바이트 확보)

### 역방향 UART 프로토콜

#### 기존 통신 vs 역방향 통신

| 구분 | 기존 (Android → ESP32-S3) | 역방향 (ESP32-S3 → Android) |
|------|--------------------------|---------------------------|
| 방향 | Android → ESP32-S3 | ESP32-S3 → Android |
| 헤더 | seq (0x00~0xFD) | **0xFE** (고정) |
| 크기 | 8바이트 | 8바이트 (동일) |
| 용도 | 입력 프레임 (마우스/키보드) | 이벤트 알림 (모드 변경 등) |
| 빈도 | 120Hz (입력 시) | 이벤트 발생 시만 |

#### 역방향 알림 프레임 구조 (8바이트)

```
┌────────┬────────────┬────────┬──────────────────────────┐
│ Header │ Event Type │  Data  │      Reserved (5B)       │
│  0xFE  │    1B      │   1B   │  0x00 0x00 0x00 0x00 0x00│
└────────┴────────────┴────────┴──────────────────────────┘
```

| 필드 | 크기 | 설명 |
|------|------|------|
| Header | 1B | **0xFE** 고정 (알림 프레임 식별자) |
| Event Type | 1B | 이벤트 종류 |
| Data | 1B | 이벤트별 데이터 |
| Reserved | 5B | 예약 (0x00 패딩, 향후 확장용) |

#### 이벤트 타입 정의

| Event Type | 이름 | Data 필드 |
|-----------|------|-----------|
| 0x01 | MODE_CHANGED | 0x00=Essential, 0x01=Standard |
| 0x02 | CONNECTION_STATE | 0x00=Disconnected, 0x01=Connecting, 0x02=Connected |
| 0x03~0x0F | 예약 | 향후 확장용 |

#### 프레임 구분 로직 (Android 수신 측)

Android가 UART에서 8바이트를 수신했을 때:
1. 첫 바이트가 `0xFE`이면 → **역방향 알림 프레임**
2. 첫 바이트가 `0x00~0xFD`이면 → 무시

### 세부 목표

#### Part A: ESP32-S3 역방향 UART 전송 (1일)

1. `uart_handler.c/h`에 역방향 전송 함수 추가:
   ```c
   /**
    * ESP32-S3 → Android 알림 프레임 전송
    * @param event_type 이벤트 종류 (EVENT_MODE_CHANGED 등)
    * @param data 이벤트 데이터
    */
   void uart_send_notification(uint8_t event_type, uint8_t data);
   ```
2. 알림 프레임 조립:
   - 8바이트 버퍼: `{0xFE, event_type, data, 0x00, 0x00, 0x00, 0x00, 0x00}`
   - `uart_write_bytes()` (ESP-IDF UART API)로 전송
3. 모드 변경 콜백 연동:
   - `connection_state.c`의 모드 변경 시 `uart_send_notification()` 호출
   - Essential → Standard: `uart_send_notification(0x01, 0x01)`
   - Standard → Essential: `uart_send_notification(0x01, 0x00)`
4. 알림 전송 신뢰성:
   - 모드 변경 시 알림을 **3회 반복 전송** (50ms 간격)
   - Android가 1개라도 수신하면 충분 (중복 수신은 Android에서 처리)

**⚠️ UART 방향 주의**:
- 기존: Android(TX) → ESP32-S3(RX) 단방향
- 추가: ESP32-S3(TX) → Android(RX) 역방향
- UART0 (GPIO43=TX, GPIO44=RX)는 양방향 지원이므로 물리적 변경 불필요
- Android USB Serial 라이브러리(usb-serial-for-android)는 수신 기능 내장

#### Part B: Android UART 수신 및 알림 파싱 (1-1.5일)

1. `UsbSerialManager`에 UART 수신 기능 추가:
   - `usb-serial-for-android` 라이브러리의 `SerialInputOutputManager` 활용
   - 수신 데이터를 8바이트 단위로 프레임 정렬
2. 프레임 분류:
   - 첫 바이트 == 0xFE → 역방향 알림 프레임으로 파싱
   - 그 외 → 무시
3. 알림 프레임 파서:
   ```kotlin
   data class NotificationFrame(
       val eventType: UByte,
       val data: UByte
   ) {
       companion object {
           val HEADER: UByte = 0xFEu
           val EVENT_MODE_CHANGED: UByte = 0x01u

           fun parse(bytes: ByteArray): NotificationFrame? {
               if (bytes.size < 3 || bytes[0].toUByte() != HEADER) return null
               return NotificationFrame(
                   eventType = bytes[1].toUByte(),
                   data = bytes[2].toUByte()
               )
           }
       }
   }
   ```
4. 중복 알림 처리:
   - ESP32-S3가 3회 반복 전송하므로 동일 이벤트 중복 수신 가능
   - 마지막 수신 이벤트와 동일하면 무시 (디바운스)

**수정 파일 (ESP32)**:
- `src/board/BridgeOne/main/uart_handler.c`: `uart_send_notification()` 함수 추가
- `src/board/BridgeOne/main/uart_handler.h`: 함수 선언 및 이벤트 타입 상수 정의
- `src/board/BridgeOne/main/connection_state.c`: 모드 변경 시 알림 전송 호출

**신규 파일 (Android)**:
- `src/android/app/src/main/java/com/bridgeone/app/protocol/NotificationFrame.kt`

**수정 파일 (Android)**:
- `src/android/app/src/main/java/com/bridgeone/app/usb/UsbSerialManager.kt`: UART 수신 스레드 추가

**참조 문서 및 섹션**:
- `src/board/BridgeOne/main/uart_handler.c`: 기존 UART 수신 코드 참조
- `docs/board/esp32s3-code-implementation-guide.md` §1.3.3 UART 프로토콜

**검증**:
- [x] ESP32: `uart_send_notification()` 함수 구현됨
- [x] ESP32: 모드 변경 시 알림 프레임 3회 전송 (50ms 간격)
- [x] ESP32: 알림 프레임이 8바이트, 0xFE 헤더 확인
- [x] ESP32: `idf.py build` 성공
- [x] Android: UART 수신 스레드 정상 동작
- [x] Android: 0xFE 헤더 알림 프레임 파싱 성공
- [x] Android: 중복 알림 디바운스 처리
- [x] Android: Android Studio 빌드 및 실행 성공

---

## Phase 3.5.5: 모드 상태 관리 구현

**목표**: Android 앱에서 `BridgeMode` 열거형을 정의하고, `StateFlow<BridgeMode>`로 모드 상태를 관리하며, ESP32-S3로부터 수신한 알림 프레임을 기반으로 프로덕션 모드 전환 로직을 구현

**개발 기간**: 0.5일

### ⚠️ Phase 3.5.4 구현에서 적용된 사항 (Phase 3.5.5 영향)

1. **`SerialInputOutputManager` 대신 `Thread + port.read()` 방식 사용**:
   - 계획에서는 `usb-serial-for-android`의 `SerialInputOutputManager` 활용을 언급했으나, 기존 `startSenderThread()` 패턴과 일관성을 위해 `Thread + port.read(100ms 타임아웃)` 방식으로 구현함.
   - 수신 스레드 이름: `"BridgeOne-UART-Receiver"` (데몬 스레드).

2. **`uart_send_notification()`이 `bridge_mode_on_change()` 콜백이 아닌 `bridge_mode_set_internal()` 내부에서 직접 호출됨**:
   - `bridge_mode_on_change()` 콜백 슬롯이 이미 Phase 3.5.2에서 `hid_handler.c`의 `hid_on_mode_change()`에 점유되어 있어, 별도 콜백 없이 `bridge_mode_set_internal()` 내에 `uart_send_notification()` 직접 추가.
   - Phase 3.5.5에서 추가 콜백 등록이 필요한 경우 `bridge_mode_on_change()`는 이미 점유 상태임을 주의.

3. **`UsbSerialManager.lastNotification: StateFlow<NotificationFrame?>` 공개됨**:
   - Phase 3.5.5에서 Android 모드 상태 관리 시, `UsbSerialManager.lastNotification`을 구독하여 `EVENT_MODE_CHANGED` 수신 시 `BridgeMode`를 업데이트해야 함.
   - 구독 예: `UsbSerialManager.lastNotification.collect { frame -> if (frame?.eventType == NotificationFrame.EVENT_MODE_CHANGED) { ... } }`
   - 연결 해제 시 `lastNotification`은 자동으로 초기화되지 않으므로, 포트 닫힘 시 `ESSENTIAL` 모드로 강제 복귀 처리를 Phase 3.5.5에서 별도 구현해야 함.

### 모드 전환 메커니즘 (프로덕션)

- **Windows 서버 연결**: `EVENT_MODE_CHANGED` 알림 프레임 수신 → Standard 모드
- **Windows 서버 해제**: USB DTR 신호 감지 → Essential 모드
- **자동 전환**: 사용자 개입 없음

### 세부 목표

1. `BridgeMode` 열거형 정의:
   ```kotlin
   enum class BridgeMode {
       ESSENTIAL,  // 기본 모드 (서버 미연결)
       STANDARD    // 서버 연결 모드
   }
   ```
2. 모드 상태 관리:
   - `UsbSerialManager`에 `StateFlow<BridgeMode>` 노출
   - ~~알림 프레임(EVENT_MODE_CHANGED) 수신 시 모드 업데이트~~ → **Phase 3.5.4에서 `UsbSerialManager.lastNotification: StateFlow<NotificationFrame?>`이 이미 공개됨**. `lastNotification`을 `collect`하여 `EVENT_MODE_CHANGED` 수신 시 `BridgeMode` StateFlow를 업데이트하면 됨.
   - 앱 시작 시 기본값: `ESSENTIAL`
   - **추가**: USB 포트 닫힘(연결 해제) 시 `ESSENTIAL` 모드로 강제 복귀 처리 필요 (`lastNotification`은 포트 닫힘 시 자동 초기화되지 않음)
3. 모드 전환 시 피드백:
   - 토스트 알림: "Standard 모드로 전환되었습니다" / "Essential 모드로 전환되었습니다"

**신규 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/protocol/BridgeMode.kt`

**수정 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/usb/UsbSerialManager.kt`: 모드 StateFlow 노출
- `src/android/app/src/main/java/com/bridgeone/app/ui/BridgeOneApp.kt`: 모드 상태 구독 및 토스트 알림

**검증**:
- [x] `BridgeMode` enum 정의됨
- [x] `StateFlow<BridgeMode>` 모드 상태 관리 동작
- [x] 알림 수신 시 모드 상태 업데이트 (프로덕션 로직)
- [x] USB 포트 닫힘 시 `ESSENTIAL` 모드로 강제 복귀
- [x] 모드 전환 시 토스트 알림 표시
- [x] Android Studio 빌드 및 실행 성공

---

## Phase 3.5.6: 모드별 UI 레이아웃 구현

**목표**: Essential/Standard 모드에 따라 터치패드 및 키보드 UI를 조건부로 표시/숨김 처리

**개발 기간**: 1일

**선행 조건**: Phase 3.5.5 (모드 상태 관리 구현) 완료

### ⚠️ Phase 3.5.5 구현에서 적용된 사항 (Phase 3.5.6 영향)

1. **`UsbSerialManager.bridgeMode: StateFlow<BridgeMode>` 공개됨**:
   - Phase 3.5.6에서 UI에 모드를 반영할 때 이 StateFlow를 사용합니다.
   - `BridgeOneApp.kt`에서 이미 `val bridgeMode by UsbSerialManager.bridgeMode.collectAsState()`로 구독 중이므로, `TouchpadPage()`와 `KeyboardPage()`에 `bridgeMode: BridgeMode` 파라미터를 추가하여 전달하는 방식으로 구현하면 됩니다.
   - 별도의 `ViewModel` 계층 없이 `UsbSerialManager`에서 직접 상태를 읽는 현재 패턴과 일관성을 유지합니다.

2. **앱 시작 시 초기 모드: `ESSENTIAL`**:
   - `_bridgeMode = MutableStateFlow(BridgeMode.ESSENTIAL)`로 초기화됨.
   - Phase 3.5.6에서 앱 최초 실행 시 Essential UI가 기본으로 표시되어야 하며, 별도 초기화 코드 불필요.

3. **`BridgeMode` 열거형 위치: `com.bridgeone.app.protocol.BridgeMode`**:
   - `TouchpadWrapper.kt`, `KeyboardLayout.kt` 수정 시 이 패키지를 import해야 합니다.

### 세부 목표

1. Essential 모드 UI (`styleframe-essential.md` 기반):
   - **터치패드**: 이동 + 좌클릭만 (현재 구현과 동일)
   - **키보드**: Boot Keyboard Cluster만 표시 (Del, Esc, Enter, F1-F12, 방향키)
   - 우클릭, 휠 스크롤, 드래그 UI **미표시**
2. Standard 모드 UI:
   - **터치패드**: 이동 + 좌클릭 + 우클릭 버튼 + 휠 스크롤 제스처
   - **키보드**: 전체 키보드 레이아웃 (현재 KeyboardLayout 구현)
   - 모든 기능 UI 활성화
3. UI 레이아웃 즉시 전환 (애니메이션 없음, 안정성 우선)

### 모드별 UI 비교

| UI 요소 | Essential 모드 | Standard 모드 |
|---------|---------------|--------------|
| 터치패드 (이동) | ✅ 표시 | ✅ 표시 |
| 좌클릭 (탭) | ✅ 표시 | ✅ 표시 |
| 우클릭 버튼 | ❌ 숨김 | ✅ 표시 |
| 휠 스크롤 | ❌ 숨김 | ✅ 표시 |
| 드래그 | ❌ 숨김 | ✅ 표시 |
| 키보드 - Boot Cluster (Del, Esc, Enter, F1-F12, 방향키) | ✅ 표시 | ✅ 표시 |
| 키보드 - 전체 레이아웃 (문자, 숫자, 특수키) | ❌ 숨김 | ✅ 표시 |
| 모드 전환 버튼 (터치패드 ↔ 키보드) | ✅ 표시 | ✅ 표시 |

### Essential 모드 레이아웃 (styleframe-essential.md 기반)

```
┌────────────────────────────────────────────────────────┐
│  ┌─────────────────────────┐  ┌─────────────────────┐  │
│  │                         │  │  Boot Keyboard      │  │
│  │     Touchpad (1:2)      │  │  Cluster            │  │
│  │                         │  │                     │  │
│  │  이동 + 좌클릭(탭) 전용  │  │  [Del] ⚡Essential  │  │
│  │                         │  │  [F1-F12] 컨테이너  │  │
│  │                         │  │  [Esc] [Enter]      │  │
│  │                         │  │  [←][↑][→]          │  │
│  │                         │  │     [↓]             │  │
│  └─────────────────────────┘  └─────────────────────┘  │
│                [🖱️ ↔ ⌨️ 전환]                         │
└────────────────────────────────────────────────────────┘
```

**수정 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/TouchpadWrapper.kt`: 우클릭, 휠, 드래그 버튼 모드별 표시/숨김
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/KeyboardLayout.kt`: Essential은 Boot Cluster만, Standard는 전체 레이아웃

**참조 문서 및 섹션**:
- `docs/android/styleframe-essential.md`: Essential 모드 UI 레이아웃 상세
- `docs/android/design-guide-app.md`: 모드 전환 UI 가이드

**검증**:
- [x] Essential 모드: 우클릭, 휠, 드래그 UI 숨김 확인 ← 현재 UI 미구현이므로 모드 무관하게 없음
- [x] Standard 모드: 모든 기능 UI 표시 확인 ← Standard 모드에서 3탭 레이아웃 표시
- [x] Essential 모드: Boot Keyboard Cluster만 표시 확인
- [x] Standard 모드: 전체 키보드 레이아웃 표시 확인
- [x] Android Studio 빌드 및 실행 성공

---

## Phase 3.5.7: 모드별 UI 재구성 및 Standard 기능 구현

**목표**: Essential 페이지를 스타일프레임 문서(`styleframe-essential.md`) 기반으로 재구성하고, Standard 모드에 휠 스크롤/우클릭 기능을 구현

**개발 기간**: 0.5일

**세부 목표**:
1. `BridgeOneApp.kt` 전면 재구성:
   - 기존 전환 버튼 기반 단일 구조 → 모드별 독립 페이지 (`EssentialModePage`, `StandardModePage`)
   - Essential 모드: 2열 구조 (터치패드 72% + Boot Keyboard Cluster 28%), 전환 버튼 없음
   - Standard 모드: 2열 구조 (터치패드 72% + 컨트롤 패널 28%) + 키보드 전환 버튼
2. Essential Boot Keyboard Cluster (스타일프레임 §2.2 기반):
   - ⚡Del (최상단 강조)
   - F1-F12 (4×3 그리드 직접 표시)
   - Esc / Enter
   - D-Pad 방향키 (weight 기반 반응형)
3. Standard 컨트롤 패널 (프로토타입):
   - 휠 Up/Down 버튼 → `ClickDetector.createWheelFrame()` 호출
   - 우클릭 버튼 → `ClickDetector.createRightClickFrame()` 호출
   - Esc / Enter + D-Pad
   - 하단 전환 버튼으로 3탭 풀 키보드 접근
4. `ClickDetector.kt`에 프레임 생성 메서드 추가:
   - `createWheelFrame(wheelDelta: Byte)`: 휠 스크롤 프레임
   - `createRightClickFrame(pressed: Boolean)`: 우클릭 전용 프레임
5. `TouchpadWrapper.kt`: Essential 모드에서 롱프레스 우클릭 비활성화 (LEFT_CLICK으로 대체)

**수정 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/BridgeOneApp.kt`: 전면 재구성
- `src/android/app/src/main/java/com/bridgeone/app/ui/utils/ClickDetector.kt`: 메서드 추가
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/TouchpadWrapper.kt`: Essential 모드 우클릭 제한

**참조 문서**:
- `docs/android/styleframe-essential.md` §2 레이아웃 구조, §3 허용/비활성 기능

**검증** (코드/빌드 수준):
- [x] `EssentialModePage`, `StandardModePage` Composable 함수 구현됨
- [x] `EssentialBootCluster`: ⚡Del, F1-F12 (4×3), Esc/Enter, D-Pad 컴포넌트 구성됨
- [x] `StandardControlPanel`: 휠 Up/Down, 우클릭, Esc/Enter, D-Pad 컴포넌트 구성됨
- [x] `ClickDetector.createWheelFrame()`, `createRightClickFrame()` 메서드 추가됨
- [x] `TouchpadWrapper`: Essential 모드 롱프레스 우클릭 → 좌클릭 대체 로직 구현됨
- [x] `MainContent()`에서 `BridgeMode`에 따라 `EssentialModePage` / `StandardModePage` 분기됨
- [x] Android Studio 빌드 성공 (컴파일 에러 없음)

---

## Phase 3.5.8: 에뮬레이터 검증 및 임시 모드 전환 버튼

**목표**: 임시 모드 전환 버튼을 추가하여 ESP32 연결 없이 에뮬레이터에서 Essential/Standard 모드 UI를 전환하며 검증한 뒤, 검증 완료 후 임시 코드를 삭제

**선행 조건**: Phase 3.5.7 (모드별 UI 재구성 및 Standard 기능 구현) 완료

**세부 목표**:
1. 임시 모드 전환 기능 추가:
   - `UsbSerialManager`에 `toggleModeForDevelopment()` 함수 추가 (Essential ↔ Standard 토글)
   - 디버그 패널 또는 화면 상단에 임시 전환 버튼 배치
2. 에뮬레이터에서 두 모드의 UI 검증
3. 검증 완료 후 임시 코드 전체 삭제

**검증**:
- [x] 임시 토글 버튼 동작 확인 (Essential ↔ Standard 전환)
- [x] Essential 모드: 2열 레이아웃 (좌 터치패드 72% + 우 Boot Cluster 28%) 정상 표시
- [x] Essential 모드: Del, F1-F12 컨테이너 버튼, Esc/Enter, D-Pad 방향키 표시 확인
- [x] Essential 모드: F1-F12 버튼 탭 → 3×4 팝업 다이얼로그 정상 표시 및 닫힘
- [x] Essential 모드: 휠/우클릭 UI 미표시 확인
- [x] Standard 모드: 2열 레이아웃 (좌 터치패드 72% + 우 컨트롤 패널 28%) 정상 표시
- [x] Standard 모드: ▲Scroll, RC Right Click, ▼Scroll 버튼 표시 확인
- [x] Standard 모드: Esc/Enter, D-Pad 방향키 표시 확인
- [x] Standard 모드: 하단 ⌨️/🖱️ 전환 버튼 → 3탭 풀 키보드 레이아웃 전환 확인
- [x] 모드 전환 토스트 알림 정상 동작
- [x] 반복 전환 안정성 확인 (크래시/레이아웃 깨짐 없음)
- [x] 임시 코드 전체 삭제 완료 (`toggleModeForDevelopment` 함수 + 임시 버튼 UI)
- [x] Android Studio 빌드 및 에뮬레이터 실행 성공

---

## Phase 3.5.9: E2E 검증 (실제 하드웨어)

**사전 조건**: Phase 3.5.7 완료 (모드별 UI 재구성) 및 Phase 3.5.8 완료 (에뮬레이터 검증)

**목표**: 실제 하드웨어(Android 폰 + ESP32-S3 + PC + Windows 서버)를 사용하여 전체 시스템 엔드-투-엔드 검증

### 🔧 사전 준비

#### 하드웨어 연결

아래 연결을 **테스트 시작 전에 모두 완료**해둡니다. 테스트 도중 케이블을 바꿔 꽂는 작업은 없습니다.

```
Android 폰 (USB-C)
    │
    └── USB-OTG 케이블 ──→ ESP32-S3 보드의 COM 포트 (우측 하단, "COM" 실크스크린)
                              │
                              └── USB 케이블 ──→ PC USB 포트 (좌측 상단, "USB" 실크스크린)
```

- **케이블 2개**: Android↔ESP32 (OTG), ESP32↔PC (일반 USB)
- **전원**: ESP32-S3는 PC USB 포트에서 5V 공급받으므로 별도 전원 불필요

#### 소프트웨어 준비

모든 소프트웨어를 **미리 빌드하고 배포**해둡니다. 테스트 도중 빌드 작업은 하지 않습니다.

| 항목 | 준비 내용 | 확인 |
|------|---------|------|
| ESP32-S3 펌웨어 | Phase 3.5.4 반영 펌웨어가 플래시되어 있는지 확인 (역방향 UART 포함) | [ ] |
| Android 앱 | Phase 3.5.7 반영 APK가 설치되어 있고 모드별 UI 전환이 동작하는지 확인 | [ ] |
| Windows 서버 | `dotnet build` 완료, 실행 파일 위치 파악 | [ ] |
| 메모장 (또는 텍스트 에디터) | PC에서 키보드 입력 확인용으로 열어둠 | [ ] |

#### 초기 상태 확인

테스트 시작 전 아래 상태가 모두 갖춰져야 합니다:

- [ ] Android 앱 실행 → "Connected" 상태 표시
- [ ] Windows 서버 **미실행** 상태 (이 시점에서 ESP32는 Essential 모드)
- [ ] PC 장치 관리자에서 HID 마우스 + HID 키보드 인식 확인

#### 확인 창 설정 (단일 창 원칙)

> Phase 3.4.3에서 구현한 Windows 서버 통합 상태 패널을 활용합니다.
> **Android Logcat이나 ESP32 시리얼 모니터를 별도로 열 필요 없습니다.**

- **주 확인 창**: Windows 서버 UI (모드 표시 / 활성 기능 목록 / RTT / 연결 품질)
- **보조 확인**: PC 화면의 커서 움직임 + 메모장 키 입력 (눈으로 직접 확인)
- **문제 발생 시에만**: Tera Term으로 ESP32 시리얼 모니터 연결 (COM 포트, 115200 baud)

---

### 테스트 1: Essential 모드 기본 동작 확인

**목표**: 서버 미연결 상태(Essential 모드)에서 Android 앱이 제공하는 기본 기능이 PC에 정상 전달되는지 확인

> ℹ️ **Essential 모드의 기능 제한 원리**: 펌웨어가 입력을 차단하는 것이 아닙니다.
> Android 앱이 Essential 모드에서 우클릭, 휠 스크롤 등의 **UI를 표시하지 않으므로** 해당 프레임 자체가 생성되지 않습니다.
> 따라서 이 테스트는 Essential 모드에서 **표시되는 UI 기능**이 정상 동작하는지를 검증합니다.

**사전 조건**:
- Windows 서버 **미실행**
- Android 앱 Connected 상태
- PC에 메모장 열려 있음 (포커스 상태)

**테스트 진행**:

| 순서 | 동작 | 예상 결과 | 확인 |
|------|------|---------|------|
| 1 | Android 터치패드에서 손가락을 움직인다 | PC 커서가 따라 움직인다 | [ ] |
| 2 | Android 터치패드를 짧게 탭한다 (좌클릭) | 메모장에 커서가 찍힌다 | [ ] |
| 3 | Android UI에 우클릭, 휠 스크롤 등 고급 기능 버튼이 **표시되지 않음**을 확인한다 | Essential 모드에서는 해당 UI가 숨겨져 있다 | [ ] |
| 4 | Android에서 **방향키(↑↓←→)** 를 누른다 | 메모장에서 커서가 이동한다 | [ ] |
| 5 | Android에서 **Esc** 키를 누른다 | 메모장에서 Esc 동작 발생 | [ ] |

**성공 기준**:
- 순서 1~2: 마우스 이동 + 좌클릭 정상 동작
- 순서 3: Essential 모드에서 고급 기능 UI가 숨겨져 있음
- 순서 4~5: 기본 키보드 키 정상 동작

**롤백 기준**: 기본 기능(마우스 이동, 좌클릭, 방향키)이 PC에 전달되지 않음 → HID 리포트 전송 로직 확인

---

### 테스트 2: Essential → Standard 전환

**목표**: Windows 서버 실행 → 핸드셰이크 완료 → Standard 모드 전환 → 모든 기능 활성화

**사전 조건**:
- 테스트 1 완료 상태 (서버 미실행, Essential 모드)
- PC에 메모장 열려 있음 (긴 텍스트가 있으면 스크롤 확인이 쉬움)

**테스트 진행**:

| 순서 | 동작 | 예상 결과 | 확인 |
|------|------|---------|------|
| 1 | Windows 서버를 실행한다 | 서버 UI가 열린다 | [ ] |
| 2 | 서버 UI를 관찰한다 | 핸드셰이크 완료 → **"Standard 모드" (녹색)** 표시 | [ ] |
| 3 | 서버 UI에서 활성 기능 목록을 확인한다 | **"wheel, drag, right_click"** 표시 (녹색) | [ ] |
| 4 | Android에서 **휠 스크롤**을 시도한다 | 메모장이 **스크롤된다** | [ ] |
| 5 | Android에서 **우클릭 버튼**을 누른다 | PC에서 **우클릭 컨텍스트 메뉴가 나타난다** | [ ] |
| 6 | Android에서 **일반 문자 키 (A, B 등)** 를 누른다 | 메모장에 **문자가 입력된다** | [ ] |
| 7 | Android 터치패드에서 손가락을 움직인다 | PC 커서가 여전히 정상 움직인다 | [ ] |

**성공 기준**:
- 순서 2~3: 서버 연결 시 Standard 모드 자동 전환 + 활성 기능 목록 표시
- 순서 4~6: Standard 모드에서 새로 표시된 UI 기능(휠, 우클릭, 일반 키)이 모두 동작
- 순서 7: 기존 기능(마우스 이동)도 영향 없이 정상

**롤백 기준**: 서버 연결 후에도 휠/우클릭 UI가 표시되지 않음 → Android 앱의 모드별 UI 전환 로직 확인

---

### 테스트 3: Standard → Essential 복귀

**목표**: Windows 서버 종료 → ESP32가 Essential 모드로 자동 복귀 → Android 앱 UI가 Essential 레이아웃으로 전환

**사전 조건**:
- 테스트 2 완료 상태 (서버 실행 중, Standard 모드)

**테스트 진행**:

| 순서 | 동작 | 예상 결과 | 확인 |
|------|------|---------|------|
| 1 | Windows 서버 프로세스를 **강제 종료**한다 (작업 관리자 → BridgeOne.exe → 작업 끝내기) | 서버 UI가 닫힌다 | [ ] |
| 2 | **즉시** Android 터치패드에서 손가락을 움직인다 | PC 커서가 **계속 움직인다** (HID는 서버 무관) | [ ] |
| 3 | Android 앱 UI를 확인한다 | 우클릭, 휠 스크롤 등 고급 기능 UI가 **숨겨져** Essential 레이아웃으로 복귀 | [ ] |
| 4 | Android에서 **방향키(↑↓←→)** 를 누른다 | 메모장에서 커서가 **이동한다** | [ ] |
| 5 | Android 터치패드를 짧게 탭한다 (좌클릭) | 메모장에 커서가 찍힌다 | [ ] |

> ℹ️ **순서 1→2 사이에 별도 대기 시간 없음**: 서버 종료 시 DTR=false가 즉시 발생하고, ESP32는 `connection_state_reset()` → `BRIDGE_MODE_ESSENTIAL` 전환이 < 1ms에 완료됩니다.

**성공 기준**:
- 순서 2: 마우스 이동이 끊기지 않음 (HID USB 연결은 유지되므로)
- 순서 3: Android 앱이 Essential 레이아웃으로 전환됨 (고급 기능 UI 숨김)
- 순서 4~5: 기본 기능 정상 동작

**롤백 기준**: 서버 종료 후에도 Android 앱이 Standard 레이아웃을 유지함 → Android 앱의 모드 전환 감지 로직 확인

---

### 테스트 4: 모드 전환 중 입력 연속성

**목표**: 마우스를 움직이는 도중에 모드 전환이 발생해도 커서 끊김이 없는지 확인

**사전 조건**:
- 테스트 3 완료 상태 (서버 종료됨, Essential 모드)

**테스트 진행**:

| 순서 | 동작 | 예상 결과 | 확인 |
|------|------|---------|------|
| 1 | Android 터치패드에서 **손가락을 천천히 원을 그리며 계속 움직인다** (멈추지 않음) | PC 커서가 원을 그린다 | [ ] |
| 2 | 손가락을 **계속 움직이면서** 다른 손(또는 도움을 받아)으로 Windows 서버를 실행한다 | 커서가 **멈추거나 튀지 않고** 계속 원을 그린다 | [ ] |
| 3 | 서버 UI에서 "Standard 모드" (녹색) 전환을 확인한다 | 녹색 표시 확인 | [ ] |
| 4 | 손가락을 **계속 움직이면서** 서버를 강제 종료한다 | 커서가 **멈추거나 튀지 않고** 계속 움직인다 | [ ] |

> ℹ️ **혼자서 테스트하기 어려운 경우**: 순서 2, 4에서 동시 조작이 필요합니다.
> **대안 방법**: (1) 터치패드에서 손을 떼고 → 서버 실행/종료 → 다시 터치패드를 만지면서 커서 움직임 확인.
> 이 경우 "전환 직후 첫 터치에서 커서가 정상 움직이는지"를 확인하면 동일한 검증 효과를 얻을 수 있습니다.

**성공 기준**:
- 모드 전환 전후로 커서가 한 곳에 멈추거나 화면 끝으로 튀는 현상 없음
- 전환 직후 첫 터치 입력이 정상 반영됨

**롤백 기준**: 전환 시 커서가 튀거나 1초 이상 멈춤 → `hid_on_mode_change()` 콜백의 키/버튼 해제 리포트 전송 로직 확인

---

### 테스트 5: 모드 전환 시 눌린 키 해제 확인

**목표**: 모드 전환 순간 눌려 있던 키/버튼이 자동 해제되는지 확인 (키가 "붙는" 현상 방지)

**사전 조건**:
- Windows 서버 실행 중 (Standard 모드)
- 메모장에 포커스

**테스트 진행**:

| 순서 | 동작 | 예상 결과 | 확인 |
|------|------|---------|------|
| 1 | Android에서 **Shift 키를 누른 상태로 유지**한다 | 메모장에서 Shift 상태 표시 (대문자 입력 가능) | [ ] |
| 2 | Shift를 **누른 채로** 서버를 강제 종료한다 | — | [ ] |
| 3 | Android에서 Shift를 놓는다 | — | [ ] |
| 4 | 서버를 다시 실행한다 (Standard 모드 전환) | — | [ ] |
| 5 | Android에서 **일반 문자 키 (a)** 를 누른다 | 메모장에 **소문자 "a"** 가 입력된다 (Shift가 붙어있지 않음) | [ ] |

> ℹ️ **이 테스트의 핵심**: 순서 5에서 "A"(대문자)가 입력되면 Shift 키가 해제되지 않고 "붙어있는" 것입니다.
> ESP32의 `hid_on_mode_change()` 콜백이 전환 시 모든 키/버튼 해제 리포트를 전송하므로, 정상이라면 소문자 "a"가 입력됩니다.

**성공 기준**:
- 순서 5에서 소문자 "a" 입력 (Shift 해제 정상)

**롤백 기준**: 대문자 "A" 입력 → `hid_on_mode_change()`의 키 해제 리포트 전송 누락 확인

---

### 📊 전체 결과 요약

**테스트 실행 날짜**: ____년 __월 __일

| 테스트 | 검증 내용 | 결과 | 통과 | 비고 |
|--------|---------|------|------|------|
| 1. Essential 기본 동작 | 기본 기능 정상 동작, 고급 기능 UI 숨김 확인 | | [ ] | |
| 2. Essential → Standard | 서버 연결 → 모든 기능 활성화 | | [ ] | |
| 3. Standard → Essential | 서버 종료 → 기능 제한 즉시 재적용 | | [ ] | |
| 4. 전환 중 입력 연속성 | 모드 전환 시 커서 끊김 없음 | | [ ] | |
| 5. 눌린 키 해제 | 전환 시 Shift 등 수정자 키 자동 해제 | | [ ] | |

**전체 통과 여부**: [ ] PASS / [ ] FAIL

**발견 사항 및 개선 필요사항**:
- (테스트 후 기록)

---

## Phase 3.5 핵심 성과

**Phase 3.5 완료 시 달성되는 상태**:
- ✅ Essential ↔ Standard 자동 모드 전환 완성
- ✅ ESP32-S3는 순수 브릿지 역할 (모드에 관계없이 프레임을 그대로 HID로 전달)
- ✅ Essential 모드 기능 제한은 Android 앱 UI 레벨에서 구현
- ✅ 모드 전환 시 눌린 키/버튼 자동 해제 (stuck 방지)
- ✅ 모드 전환 시 입력 손실 없음
- ✅ ESP32-S3 → Android 역방향 UART 알림 통신 구축
- ✅ Android 앱이 모드를 인지하고 Essential/Standard UI 자동 전환
- ✅ 3개 컴포넌트(Android, ESP32-S3, Windows) 모두 현재 모드 인지
- ✅ Phase 4 (고급 기능: 멀티 커서, 매크로, 확장 키보드)의 통신 기반 완성
