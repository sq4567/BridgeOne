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
- Standard 모드에서 wheel, drag, right_click 활성화
- Essential 모드에서 wheel=0 강제 및 기능 제한 복귀
- 전환 중 입력 손실 없음 보장

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

| 기능 | Essential 모드 | Standard 모드 |
|------|---------------|--------------|
| 마우스 이동 (X/Y) | ✅ | ✅ |
| 좌클릭 | ✅ | ✅ |
| 우클릭 | ❌ | ✅ |
| 중앙 클릭 | ❌ | ✅ |
| 휠 스크롤 | ❌ (wheel=0 강제) | ✅ |
| 클릭-드래그 | ❌ | ✅ |
| 모든 키보드 키 | ❌ (Boot 키만) | ✅ |
| 멀티 커서 | ❌ | ⏳ (Phase 4+) |
| 매크로 | ❌ | ⏳ (Phase 4+) |

---

## Phase 3.5.1: ESP32-S3 모드 상태 관리

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
   - `bridge_mode_set(bridge_mode_t mode)`: 모드 설정 (내부용)
   - `bridge_mode_on_change(callback)`: 모드 변경 콜백 등록
3. 모드 전환 트리거 연동:
   - `connection_state`의 상태 변경 콜백에서 모드 전환:
     - `CONN_STATE_CONNECTED` 진입 시 → `BRIDGE_MODE_STANDARD`
     - `CONN_STATE_IDLE` 진입 시 → `BRIDGE_MODE_ESSENTIAL`
   - 전환 시 디버그 로그 출력:
     - "Mode changed: Essential → Standard"
     - "Mode changed: Standard → Essential"
4. 수락된 기능 목록 기반 세부 제어:
   - Phase 3.3에서 저장된 `accepted_features`에 따라 기능 활성화
   - 예: `wheel`이 수락되지 않았으면 Standard에서도 wheel=0

**수정 파일**:
- `src/board/BridgeOne/main/connection_state.c/h`: 모드 관리 함수 추가

**참조 문서 및 섹션**:
- `docs/android/styleframe-essential.md` §8 상태 머신
- `docs/android/styleframe-essential.md` §3 허용/비활성 기능

**검증**:
- [ ] `bridge_mode_t` 열거형 정의됨
- [ ] 핸드셰이크 완료 시 Standard 모드 전환 확인 (로그)
- [ ] Keep-alive 실패 시 Essential 모드 복귀 확인 (로그)
- [ ] 모드 변경 콜백 정상 호출
- [ ] 수락된 기능 목록 기반 세부 제어 동작
- [ ] `idf.py build` 성공

---

## Phase 3.5.2: 모드별 HID 동작 변경

**목표**: ESP32-S3의 HID 리포트 생성 시 현재 모드에 따라 동작을 변경

**개발 기간**: 1-1.5일

**세부 목표**:
1. `hid_handler.c`의 프레임 처리 로직 수정:
   - `processBridgeFrame()` 함수에서 현재 모드 확인
   - **Essential 모드**:
     - 마우스 리포트: `wheel = 0` 강제 (프레임의 wheel 값 무시)
     - 마우스 리포트: `buttons`에서 bit1(우클릭), bit2(중앙클릭) 마스킹 → 좌클릭만 허용
     - 키보드 리포트: Boot 키만 허용 (Del, Esc, Enter, F1-F12, Arrow) → 그 외 키코드 무시
   - **Standard 모드**:
     - 마우스 리포트: 모든 필드 그대로 전달 (wheel, 모든 버튼)
     - 키보드 리포트: 모든 HID 키코드 허용
2. Essential 모드 키코드 화이트리스트:
   ```c
   static const uint8_t essential_allowed_keycodes[] = {
       0x4C,  // Delete
       0x29,  // Escape
       0x28,  // Enter/Return
       0x3A, 0x3B, 0x3C, 0x3D, 0x3E, 0x3F,  // F1-F6
       0x40, 0x41, 0x42, 0x43, 0x44, 0x45,  // F7-F12
       0x52,  // Up Arrow
       0x51,  // Down Arrow
       0x50,  // Left Arrow
       0x4F,  // Right Arrow
   };
   ```
3. 모드 전환 시 입력 손실 방지:
   - 현재 처리 중인 프레임 완료 후 모드 전환
   - 전환 즉시 키 해제 리포트 전송 (눌린 키가 있는 경우)
   - 전환 즉시 버튼 해제 리포트 전송 (눌린 버튼이 있는 경우)

**수정 파일**:
- `src/board/BridgeOne/main/hid_handler.c`: 모드별 필터링 로직 추가
- `src/board/BridgeOne/main/hid_handler.h`: Essential 키코드 화이트리스트 정의

**참조 문서 및 섹션**:
- `docs/android/styleframe-essential.md` §3 허용/비활성 기능
- `docs/android/styleframe-essential.md` §7 구현 메모 (HID/프로토콜)
- `src/board/BridgeOne/main/hid_handler.c` - 기존 프레임 처리 로직

**검증**:
- [ ] Essential 모드: wheel=0 강제 확인 (PC에서 스크롤 안 됨)
- [ ] Essential 모드: 우클릭/중앙클릭 무시 확인
- [ ] Essential 모드: Boot 키 이외의 키코드 무시 확인
- [ ] Standard 모드: wheel 값 정상 전달 (PC에서 스크롤 동작)
- [ ] Standard 모드: 모든 버튼/키코드 정상 전달
- [ ] 모드 전환 시 눌린 키/버튼 해제 리포트 전송
- [ ] 모드 전환 중 프레임 손실 없음
- [ ] `idf.py build` 성공

---

## Phase 3.5.3: Windows 서버 모드 상태 동기화

**목표**: Windows 서버에서 현재 모드를 관리하고 UI에 반영

**개발 기간**: 0.5-1일

**세부 목표**:
1. `ConnectionViewModel`에 모드 상태 추가:
   - `[ObservableProperty]` BridgeMode: Essential / Standard
   - 핸드셰이크 완료 시 Standard로 전환
   - 연결 끊김 시 Essential로 전환
2. UI에 현재 모드 표시:
   - 상태바에 "Essential" / "Standard" 레이블
   - Standard 모드: 활성화된 기능 목록 표시
3. ESP32-S3로부터 `CMD_MODE_NOTIFY` (0x20) 수신 처리:
   - ESP32-S3가 자체적으로 모드를 변경한 경우 (예: 타임아웃) 알림 수신
   - 서버 측 모드 상태 동기화

**수정 파일**:
- `src/windows/BridgeOne/ViewModels/ConnectionViewModel.cs`: 모드 상태 추가
- `src/windows/BridgeOne/MainWindow.xaml`: 모드 표시 UI

**검증**:
- [ ] 핸드셰이크 완료 시 UI에 "Standard" 표시
- [ ] 연결 끊김 시 UI에 "Essential" 표시
- [ ] 활성화된 기능 목록 UI 표시
- [ ] MODE_NOTIFY 수신 시 모드 동기화

---

## Phase 3.5 E2E 검증

1. **Essential → Standard 전환**: 서버 연결 → 핸드셰이크 → wheel 스크롤 동작 확인
2. **Standard → Essential 복귀**: 서버 종료 → wheel 비활성 확인 → Boot 키만 동작
3. **전환 중 입력 연속성**: 마우스 이동 중 모드 전환 → 커서 끊김 없음
4. **기능 필터링**: Essential에서 우클릭 시도 → PC에서 우클릭 이벤트 없음

---

## Phase 3.5 핵심 성과

**Phase 3.5 완료 시 달성되는 상태**:
- ✅ Essential ↔ Standard 자동 모드 전환 완성
- ✅ Standard 모드에서 wheel, drag, right_click 활성화
- ✅ Essential 모드에서 기능 제한 복귀 (BIOS 호환성 유지)
- ✅ 모드 전환 시 입력 손실 없음
- ✅ Phase 3.6 (Android 모드 인지)의 선행 조건 충족
