---
title: "BridgeOne 컴포넌트 디자인 가이드"
description: "특수/공통 컴포넌트(UI) 명세와 구현 지침: 버튼, DPad 등"
tags: ["design-guide", "components", "ui", "compose"]
version: "v0.3"
owner: "Chatterbones"
updated: "2025-09-18"
note: "본 문서에 존재하는 모든 상수값 및 설정값은 초기 값으로, 확정된 고정값이 아님"
---

# BridgeOne 컴포넌트 디자인 가이드

## 시스템 아키텍처 개요

> **상세 아키텍처**: 전체 시스템 아키텍처는 [`technical-specification.md` §3]를 참조하세요.

**핵심**:
- 본 앱은 사용자의 터치 입력을 8바이트 델타 프레임으로 변환하여 USB-Serial을 통해 ESP32-S3 동글로 전송합니다.
- 키보드 입력(버튼 컴포넌트)은 KeyDown/KeyUp 명령으로 ESP32-S3를 통해 HID Boot Keyboard 인터페이스로 PC에 전송됩니다.
- 멀티 커서, 매크로 등의 고급 기능은 0xFF 헤더와 JSON 구조의 커스텀 명령으로 처리되며, Vendor-specific Interface를 통해 PC Windows 서비스와 양방향 통신합니다.

**입력 통로 구분**:
- **마우스 입력**: 8바이트 델타 프레임 → HID Boot Mouse (터치패드, DPad 컴포넌트)
- **키보드 입력**: KeyDown/KeyUp 명령 → HID Boot Keyboard (버튼 컴포넌트)
- **고급 기능**: 커스텀 명령 → Vendor-specific Interface (매크로, 멀티 커서 등)

## 용어집/정의

- Selected/Unselected: 선택 상태. 시각적 강조 여부를 나타냅니다(페이지 인디케이터 등). 입력 가능 여부와 구분합니다.
- Enabled/Disabled: 입력 가능 상태. 터치/클릭이 가능한지를 의미합니다(장기 작업 중 Disabled 등).
- Essential/Standard: 시스템 운용 상태. Windows 서버와 연결되지 않은 상태는 Essential(필수 기능), 연결된 상태는 Standard(모든 기능)입니다.
- TransportState: NoTransport | UsbOpening | UsbReady.
- 상태 용어 사용 원칙(금칙어 포함):
  - "활성/비활성" 표현은 금지. 선택 상태는 Selected/Unselected, 입력 가능 상태는 Enabled/Disabled로 표기합니다 [[memory:5809234]].
  - "선택됨/선택 안 됨" 대신 Selected/Unselected 사용.
  - "사용 가능/불가" 대신 Enabled/Disabled 사용.

## UI 컴포넌트 상세 명세

**참고**:
> 이 문서는 `Docs/design-guide-app.md`의 `특수 컴포넌트 디자인` 섹션을 별도로 분리한 문서입니다. 전체 디자인 가이드는 메인 가이드를 참조하세요.
> 상수/임계값 단일 출처(SSOT): 컴포넌트별 디바운스/반복/크기 등 수치 상수는 `Docs/Android/technical-specification-app.md` §3를 참조합니다.

---

## 1. 터치패드 컴포넌트

> 본 프로젝트의 최중요 컴포넌트이자 기능의 수 및 복잡도가 매우 높은 컴포넌트입니다.
> 따라서 `Docs\touchpad.md`에 개별 문서로 작성되었습니다.

---

## 2. 버튼 컴포넌트

### 2.1 버튼 구조 및 색상 시스템

#### 2.1.1 버튼 구조

- 높이: 48dp
- 둥근 모서리: 8dp
- 배경: 상태별 색상
- 텍스트: 16sp, Medium

#### 2.1.2 버튼 색상 시스템

`#2196F3`: Enabled 상태 버튼 배경색
`#121212`: 버튼 텍스트
`#C2C2C2`: Disabled 상태 버튼 배경
`#1976D2`: 터치 및 Sticky Hold Fill 애니메이션 및 Hold 상태 버튼 배경

### 2.2 버튼 타입과 기능 매핑

**개요**:
- 각 버튼은 탭 1회로 즉시 실행되는 것을 기본으로 하며, 필요 시 길게 누르기/토글 동작을 지원합니다.
- 색상은 `Docs/design-guide-app.md` §2(색상 시스템)의 토큰을 따릅니다.

#### 2.2.1 키보드 버튼(KeyboardKeyButton)

- 목적: Ctrl, Alt, Shift, Enter, Space, Tab, Backspace 등 단일 키에 대한 KeyDown/KeyUp 전송
- **기술적 구현**: ESP32-S3 동글을 통해 HID Boot Keyboard 인터페이스로 PC에 전송. BIOS/BitLocker 단계에서도 호환되며, Boot Protocol에서는 기본 키만 지원(A-Z, 0-9, 특수키 등). 상세한 구현 로직은 `Docs/Windows/technical-guide-server.md` §2.3.2(HID Boot Keyboard)를 참조하세요.
- **강제 재연결 처리**: 기본 HID 기능이므로 재연결 중에도 정상 동작, Disabled 상태로 전환하지 않음
- 유저 플로우:
  - 탭: pressed 동안 해당 키도 press, release 즉시 해당 키도 release 됨(Sticky Hold=Off)
  - 롱프레스(Sticky Hold, 선택): 임계 초과 시 `isStickyLatched=true`로 Down 유지, 다음 탭에서 Up 전송 후 라치 해제
    - per-key 옵션: `canStickyHoldOnLongPress=true`일 때만 활성(기본 false)
    - 임계시간: `KEY_STICKY_HOLD_LONG_PRESS_THRESHOLD_MS = 500ms`
- 시각/피드백:
  - 색상: 기본 `#2196F3`, Disabled `#C2C2C2`, 터치/StickyHold `#1976D2`
  - 햅틱: Light 1회(§5.4.4)
  - 리플: 사용하지 않음. 대신 200ms 스케일 0.98→1.0 적용
  - 롱프레스 Fill 애니메이션:
    - 트리거: `canStickyHoldOnLongPress=true`이고 롱프레스 시작 시 활성화
    - 효과: 버튼 배경 전체가 좌측에서부터 수평으로 채워지는 Fill 애니메이션(0% → 100%)
    - 애니메이션 지속시간: `KEY_STICKY_HOLD_LONG_PRESS_THRESHOLD_MS = 500ms`와 동일
    - 색상: Fill 진행 영역은 `#1976D2`, 미진행 영역은 기본 색상 `#2196F3` 유지
    - 구현: 버튼 배경을 두 영역으로 분할하여 Fill 진행률에 따라 좌측 영역 width 비율 조정
    - 중단: 터치 해제 시 즉시 리셋하여 기본 배경색으로 복구
    - 완료: 임계시간 도달 시 전체 배경이 `#1976D2`으로 채워진 상태로 Sticky Hold 진입
- 상태 모델:
  - `isEnabled: Boolean` — 입력 가능 여부(Enabled/Disabled, §10.4 준수)
  - `isStickyLatched: Boolean` — Sticky Hold 라치 상태(기본 false)
  - `longPressProgress: Float` — 롱프레스 진행률 (0.0f ~ 1.0f, Fill 애니메이션용)
  - `isLongPressInProgress: Boolean` — 롱프레스 진행 중 여부 (Fill 애니메이션 상태 관리용)
- 접근성 요구사항:
  - 탭과 롱프레스 이벤트를 모두 지원해야 함
  - 버튼 역할과 상태를 포함한 설명을 제공해야 함

#### 2.2.2 단축키 버튼(ShortcutButton)

- 목적: Ctrl+C, Ctrl+V, Alt+Tab 등 복합 조합을 원터치로 실행
- **기술적 구현**: ESP32-S3 동글을 통해 HID Boot Keyboard 인터페이스로 PC에 전송. 모디파이어 키(Shift, Ctrl, Alt, Win)와 일반 키의 조합을 순차적으로 KeyDown/KeyUp 처리하여 단축키 시퀀스를 완성합니다.
- **강제 재연결 처리**: 기본 HID 기능이므로 재연결 중에도 정상 동작, Disabled 상태로 전환하지 않음
- 유저 플로우:
  - 탭: 정의된 순서로 Down/Up 주입(예: Ctrl+C = CtrlDown → CDown → CUp → CtrlUp)
  - 디바운스: `REINPUT_DEBOUNCE_MS = 150ms` 내 재탭 무시(중복 주입 방지)
- 시각/피드백:
  - 색상: 기본 `#2196F3`, Disabled `#C2C2C2`
  - 햅틱: Light 1회
  - 리플: 사용하지 않음. 200ms 스케일 0.98→1.0 적용
- 상태 모델:
  - `isEnabled: Boolean`
- 접근성 요구사항:
  - 비동기 시퀀스 실행을 지원해야 함
  - 버튼 역할과 상태를 포함한 설명을 제공해야 함

#### 2.2.3 매크로 버튼(MacroButton)

- 목적: Android 앱이 동글을 통해 PC(Windows) 서버의 특정 매크로 실행을 트리거
- **기술적 구현**: `usb-serial-for-android` 라이브러리 기반 양방향 통신으로 실시간 응답 처리. 상세한 구현 로직은 `Docs/Android/technical-guide-app.md` §3.1.2(MacroButton 구현 로직)를 참조하세요.
- 유저 플로우 요약:
  - 탭: `MACRO_START_REQUEST(macroId)` 전송 → 버튼 Disabled(중복 입력 방지)
  - 응답: `onNewData()` 콜백으로 `TASK_COMPLETED` 또는 `MACRO_NOT_FOUND` 수신
  - 강제 해제: 동일 버튼 재탭 시 취소 요청 및 UI 강제 활성화
- **강제 재연결 처리**: 
  - 재연결 시작: 서버로부터 `SERVER_INITIATED_RECONNECTION_REQUEST` 수신 시 모든 매크로 버튼 일시 Disabled 상태로 전환, 진행 중인 매크로는 자동 중단
  - 재연결 진행 중: Standard 모드 유지하되 서버 의존 기능(매크로)만 비활성화, 시각적으로 회색 처리
  - 재연결 완료: 모든 매크로 버튼 자동으로 Enabled 상태 복원, 이전 설정값 유지
- 시각/피드백:
  - 색상: 기본 `#2196F3`, Disabled `#C2C2C2`
  - 햅틱: Light 1회(시작/종료 시 토스트와 조합)
  - 토스트: 매크로 성공 시 #4CAF50 (녹색), 실패/중단 시 #F44336 (빨간색) 토스트 표시
- 접근성 요구사항:
  - 비활성 상태 관리와 자동 복구 처리를 지원해야 함
  - 버튼 역할과 상태를 포함한 설명을 제공해야 함

#### 2.2.4 컨테이너 버튼(ContainerButton)

- 목적: 여러 하위 버튼을 그룹화하여 팝업 오버레이 형태로 표시하는 컨테이너 버튼
- 유저 플로우:
  - **탭 (일반 모드)**: 팝업 오버레이 표시 (하위 버튼들이 메인 컬러 테두리의 둥근 사각형 박스 안에 그리드 형태로 배치)
    - 하위 버튼 탭: 해당 버튼의 기능 실행 후 팝업 닫힘
    - 팝업 외부 탭/Back 키: 팝업 닫힘
  - **길게 누르기 (지속 모드)**: 팝업 오버레이 표시, 지속 모드로 전환
    - 임계시간: `CONTAINER_POPUP_LONG_PRESS_THRESHOLD_MS = 500ms`
    - 하위 버튼 탭: 해당 버튼의 기능 실행 후에도 팝업은 닫히지 않음 (지속 모드)
    - 팝업 닫는 방법: 팝업 외부 영역 터치/Back 키만으로 닫힘
  - 디바운스: `REINPUT_DEBOUNCE_MS = 150ms` 내 재탭 무시(중복 팝업 방지)
- 시각/피드백:
  - 색상: 기본 `#2196F3`, Disabled `#C2C2C2`, 팝업 중 `#1976D2`
  - 햅틱: Light 1회 (팝업 열기/닫기, 하위 버튼 선택)
  - 리플: 사용하지 않음. 200ms 스케일 0.98→1.0 적용
  - 팝업: 스크림 `#121212`(alpha 0.4), 컨텐츠 블러 8dp
  - 팝업 컨테이너: 메인 컬러(`#2196F3`) 테두리의 둥근 사각형 박스, 배경색 `#2A2A2A`, 둥근 모서리 12dp
  - 롱프레스 Fill 애니메이션 (지속 모드 진입용):
    - 트리거: 길게 누르기 시작 시 활성화
    - 효과: 버튼 배경 전체가 좌측에서부터 수평으로 채워지는 Fill 애니메이션(0% → 100%)
    - 애니메이션 지속시간: `CONTAINER_POPUP_LONG_PRESS_THRESHOLD_MS = 500ms`와 동일
    - 색상: Fill 진행 영역은 `#1976D2` (기본 색상보다 어두운 톤), 미진행 영역은 기본 색상 `#2196F3` 유지
    - 구현: 버튼 배경을 두 영역으로 분할하여 Fill 진행률에 따라 좌측 영역 width 비율 조정
    - 중단: 터치 해제 시 즉시 리셋하여 기본 배경색으로 복구, 일반 모드로 팝업 오픈
    - 완료: 임계시간 도달 시 전체 배경이 `#1976D2`으로 채워진 상태로 지속 모드 진입
- 상태 모델:
  - `isEnabled: Boolean` — 입력 가능 여부
  - `isPopupOpen: Boolean` — 팝업 표시 여부
  - `isPersistentMode: Boolean` — 지속 모드 여부 (길게 누르기로 열린 팝업인지)
  - `childButtons: List<ChildButton>` — 하위 버튼 목록
  - `selectedChildButton: ChildButton?` — 현재 선택된 하위 버튼
  - `longPressProgress: Float` — 롱프레스 진행률 (0.0f ~ 1.0f, Fill 애니메이션용)
  - `isLongPressInProgress: Boolean` — 롱프레스 진행 중 여부 (Fill 애니메이션 상태 관리용)
- 접근성 요구사항:
  - 터치 이벤트와 팝업 오버레이 애니메이션을 지원해야 함
  - 탭과 길게 누르기 이벤트를 모두 지원해야 함
  - 롱프레스 Fill 애니메이션에 대한 시각적 피드백과 접근성 안내를 제공해야 함
  - 버튼 역할과 상태를 포함한 설명을 제공해야 함 (팝업 모드 구분 포함)
  - 키보드 네비게이션을 지원해야 함(하위 버튼 간 이동)
  - 앵커 좌표 계산과 정확한 위치 팝업을 지원해야 함

### 2.3 설계 요구사항

목표: 버튼 컴포넌트의 디자인, 상태, 상호작용 플로우에 대한 설계 요구사항을 정의합니다.

핵심 설계 요소:
- 크기/모양: 높이 48dp, 최소 터치 48×48dp, 둥근 모서리 12dp
- 색상: 기본 색상, 흰색 텍스트, 회색 Disabled 상태(alpha 0.6), 어두운 색상(터치/StickyHold)
- 터치 피드백: 리플 효과를 사용하지 않고 200ms 스케일 변화(0.98 → 1.0)로 표현
- 상태 구분: Enabled/Disabled는 입력 가능 여부, Selected/Unselected는 선택 상태를 의미함(혼동 금지)

상태별 동작 정의:
- 키보드 버튼(KeyboardKeyButton):
  - 탭 동작: 키 누름과 해제를 즉시 전송
  - 롱프레스 Sticky Hold: 500ms 임계값 초과 시 Sticky 상태로 키 누름을 유지하며, 다음 탭에서 키 해제 후 상태 해제
- 단축키 버튼(ShortcutButton):
  - 키 시퀀스: 정의된 순서에 따라 키 누름/해제를 순차 전송(예: Ctrl+C = Ctrl누름 → C누름 → C해제 → Ctrl해제)
  - 중복 입력 방지: 150ms 내 재탭 무시
- 매크로 버튼(MacroButton):
  - 동작 흐름: 탭 시 매크로 시작 요청 → 버튼 비활성화 → 작업 완료 시 버튼 활성화 복구
  - 진행 상태 표시: 버튼 내부 로더를 사용하지 않고, 상단 상태 알림 토스트로만 표시(§5.2)
  - 성공 상태 표시: 매크로 완료 시 #4CAF50 (녹색) 토스트로 성공 알림
  - 실패/중단 상태 표시: 매크로 실패/중단 시 #F44336 (빨간색) 토스트로 오류 알림
  - 강제 해제: 동일 버튼 재탭 시 매크로 취소 요청과 전체 터치 강제 활성화 요청을 전송하며, 토스트로 1000ms간 알림
- 컨테이너 버튼(ContainerButton):
  - **일반 모드 (탭)**: 탭 시 팝업 오버레이를 표시
    - 하위 선택: 하위 버튼 선택 시 해당 기능 실행 후 팝업 자동 닫힘
    - 팝업 닫기: 외부 영역 탭, Back 키, ESC 키로 닫기 가능
  - **지속 모드 (길게 누르기)**: 임계시간 초과 시 팝업 오버레이를 지속 모드로 표시
    - 하위 선택: 하위 버튼 선택 시 해당 기능 실행 후에도 팝업 유지
    - 팝업 닫기: 외부 영역 터치, Back 키, ESC 키로만 닫기 가능
  - 중복 입력 방지: 150ms 내 재탭 무시

접근성 요구사항:
- 버튼 역할과 현재 상태를 포함한 설명을 제공해야 함(Disabled 상태 포함)
- 시각적 의존성 감소: 색상과 아이콘을 함께 사용하며, 상태 알림은 LiveRegion(Polite)으로 제공

### 2.4 Disabled 상태 정의(§10.4 준수)

**개요**:
- 본 절은 각 버튼 컴포넌트의 입력 불가 상태(Enabled/Disabled)를 `Docs/design-guide-app.md` §10.4(컴포넌트 비활성화 플로우)에 맞추어 규정한다.
- 선택 상태(Selected/Unselected)와 입력 가능 상태(Enabled/Disabled)를 혼동하지 말 것.

공통 규칙(모든 버튼 공통):
- 시각: 각 버튼 컴포넌트가 자체적으로 Disabled 상태 스타일 적용. 일반적으로 `#C2C2C2` 기반 회색 톤, alpha = 0.6. 내부 아이콘/텍스트는 동일 톤다운 규칙 적용.
- 동작: 모든 입력을 차단하며, 사용자의 터치 이벤트를 처리하지 않는다.
- 접근성: `contentDescription`에 "(비활성)" 부가, 상태 변화 시 상단 토스트는 LiveRegion(Polite)로 고지.
- 애니메이션: 활성↔비활성 전환 시 150–250ms `tween`으로 색/알파를 부드럽게 변경.
- 용어: 선택 상태는 Selected/Unselected, 입력 가능 상태는 Enabled/Disabled로 표기한다.

트리거/복구:
- 개별 비활성: `UI_DISABLE_SELF_REQUEST(componentId)` 수신 시 해당 버튼만 비활성(§10.4.1).
- 전체 비활성: `UI_DISABLE_ALL_TOUCHABLES_REQUEST(pageId)` 수신 시 페이지 내 모든 터치 가능한 버튼 비활성(§10.4.2).
- 강제 해제: 비활성화를 유발한 동일 소스 재탭 시 강제 해제 요청(`UI_FORCE_ENABLE_ALL_TOUCHABLES_REQUEST`) 전송 가능(§10.4.2).
- 작업 완료: `TASK_COMPLETED` 수신 시 원상 복구(§10.4.3).

버튼 유형별 세부 규정:
1) KeyboardKeyButton
- 자체적으로 Disable을 발신하지 않는다. 외부 비활성/전체 비활성 신호만 준수한다.
- 시각: 배경 `#C2C2C2`(alpha 0.6), 텍스트/아이콘 동일 톤다운. 선택(토글) 하이라이트는 표시하지 않는다.
- 동작: 탭, 롱프레스, Sticky Hold 모든 입력을 차단해야 함.

2) ShortcutButton
- 자체 Disable 발신 없음. 외부/전체 비활성 신호 준수. 자체 재입력 방지는 디바운스(§2.2.2)로 처리하며 Disable과 구분한다.
- 시각/동작/접근성: KeyboardKeyButton과 동일 규칙을 따른다.

3) MacroButton
- 탭 즉시 `MACRO_START_REQUEST(macroId)` 전송 후 본인 `isEnabled=false`로 전환(중복 입력 방지). `TASK_COMPLETED` 수신 시 자동 복구.
- 강제 해제: 동일 버튼 재탭 시 `MACRO_CANCEL_REQUEST(macroId)` + `UI_FORCE_ENABLE_ALL_TOUCHABLES_REQUEST(pageId, sourceComponentId)` 전송(§10.4.2), 즉시 활성화 복구.
- 시각: Disabled 동안 배경 `#C2C2C2`(alpha 0.6). 진행 인디케이터는 사용하지 않으며, 상태 알림은 상단 토스트로만 고지(§5.2).
- 상태 관리: 비활성 상태를 추적하고, 색상 전환을 애니메이션으로 처리하며, 리플 효과 없이 터치 이벤트를 지원해야 함.

4) ContainerButton
- 자체 Disable 발신 없음. 외부/전체 비활성 신호 준수. 팝업이 열려있을 때 Disabled 전환 시 팝업을 즉시 닫고 지속 모드도 해제한다.
- 시각: Disabled 동안 배경 `#C2C2C2`(alpha 0.6), 팝업 표시 불가.
- 동작: Disabled 시 탭/길게 누르기/팝업 열기 모두 차단. 팝업이 열려있으면 즉시 닫힘, 지속 모드(`isPersistentMode`)도 false로 초기화.
- 상태 관리: 비활성 상태, 팝업 열림 상태, 지속 모드 상태를 추적하고, 색상 전환을 애니메이션으로 처리하며, 리플 효과 없이 터치 이벤트를 지원해야 함.

표준 색상/알파 및 상수(권장):
- DISABLED_OVERLAY_COLOR = `#C2C2C2`
- DISABLED_OVERLAY_ALPHA = 0.6
- CONTAINER_POPUP_SCRIM_COLOR = `#121212`
- CONTAINER_POPUP_SCRIM_ALPHA = 0.4
- CONTAINER_POPUP_BLUR_DP = 8
- CONTAINER_POPUP_BORDER_COLOR = `#2196F3`
- CONTAINER_POPUP_BACKGROUND_COLOR = `#2A2A2A`
- CONTAINER_POPUP_CORNER_RADIUS_DP = 12
- CONTAINER_POPUP_LONG_PRESS_THRESHOLD_MS = 500
- LONG_PRESS_FILL_COLOR = `#1976D2`
- LONG_PRESS_FILL_RESET_DURATION_MS = 150

참조: `Docs/design-guide-app.md` §2(색상 시스템), §5.2(상태 알림 토스트), §10.4(컴포넌트 비활성화 플로우)

### 2.5 버튼 키/단축키 프리셋 배열 준비 문서

본 절은 버튼 컴포넌트(§2.2)의 실제 사용 시, 코드 단에서 손쉽게 선택·구성할 수 있도록 키보드 단일 키 및 단축키 조합의 "프리셋 배열"을 어떤 구조로 설계·정의할지에 대한 문서입니다.
구현 코드는 포함하지 않으며, 명세·분류·식별자 규칙과 카탈로그만 제시합니다.
용어는 `Selected/Unselected`(선택 상태)와 `Enabled/Disabled`(입력 가능 상태)를 엄격히 구분합니다.

#### 2.5.1 목표와 범위
- **목표**: 디자이너/기획/개발이 공통으로 참조 가능한 키/단축키 프리셋 카탈로그와 데이터 스키마를 확정해, 추후 코드에서 배열만 참조하여 신뢰성 있게 재사용.
- **범위**: Windows 환경 중심. Android/USB-HID 브리지(입력 주입) 정책은 `Docs/usb-hid-bridge-architecture.md` 최종안에 따름.
- **비포함**: 실제 주입 가능 여부의 저수준 제약 처리(예: 보안 시퀀스)는 런타임 정책으로 분리. 본 절은 선택·구성 관점의 준비 문서.

#### 2.5.2 데이터 스키마(문서상 제안)
- KeyPreset(단일 키)
  - `id`(UPPER_SNAKE_CASE): 예 `KEY_ENTER`, `MOD_CTRL`.
  - `labelKo`/`labelEn`: 표시용 라벨.
  - `category`: `Modifiers | Editing | Navigation | System | Media | Function | Numpad | Symbols | Alphanumeric` 등.
  - `platformMapping`(참고 키 식별): `windows.vk`(예: `VK_RETURN`), `hid.usagePage/usageId`(옵션), `android.keyCode`(옵션).
  - `isModifier: Boolean`: Ctrl/Shift/Alt/Win 등 구분.
  - `canStickyHoldOnLongPressDefault: Boolean`: §2.2.1 및 `KEY_STICKY_HOLD_LONG_PRESS_THRESHOLD_MS = 500` 정책과 연계.
  - `tags: [String]`: 검색/필터용 태그(예: "text", "system").
  - 접근성: `contentDescriptionTemplate`(상태 포함 표현 가이드).
- ShortcutPreset(단축키 조합/시퀀스)
  - `id`(UPPER_SNAKE_CASE): 예 `SC_COPY`, `SC_PASTE`, `SC_TASK_MANAGER`.
  - `labelKo`/`labelEn`
  - `category`: `Editing | Navigation | System | WindowMgmt | Media | Browser | AppCommon` 등.
  - `combo`: 키 조합 식별자 배열(예: `[MOD_CTRL, KEY_C]`).
  - `sequencePolicy`: Down/Up 순서 규칙. 기본은 "모디파이어 Down → 일반키 Down → 일반키 Up → 모디파이어 Up".
  - `reinputDebounceMs`: 기본 `REINPUT_DEBOUNCE_MS = 150`(§2.2.2).
  - 제한/주의: 보안 시퀀스 등 런타임에서 차단될 수 있음을 명시.

> 구현 시에는 위 스키마를 코드 상 상수/배열로 매핑함.

#### 2.5.3 단일 키 프리셋 카테고리/카탈로그(요약)

- Modifiers: `MOD_CTRL`, `MOD_SHIFT`, `MOD_ALT`, `MOD_WIN`
- Editing(Text): `KEY_ENTER`, `KEY_BACKSPACE`, `KEY_DELETE`, `KEY_TAB`, `KEY_SPACE`
- Navigation: `KEY_UP`, `KEY_DOWN`, `KEY_LEFT`, `KEY_RIGHT`, `KEY_HOME`, `KEY_END`, `KEY_PAGE_UP`, `KEY_PAGE_DOWN`
- System: `KEY_ESC`, `KEY_PRINT_SCREEN`, `KEY_INSERT`
- Function: `KEY_F1` … `KEY_F12`
- Alphanumeric: `KEY_0` … `KEY_9`, `KEY_A` … `KEY_Z`
- Symbols(예): `KEY_BRACKET_LEFT([)`, `KEY_BRACKET_RIGHT(])`, `KEY_SEMICOLON(;)`, `KEY_APOSTROPHE(')`, `KEY_COMMA(,)`, `KEY_PERIOD(.)`, `KEY_SLASH(/)`, `KEY_BACKSLASH(\)`
- Media(하드웨어 지원 시): `KEY_MEDIA_PLAY_PAUSE`, `KEY_MEDIA_NEXT_TRACK`, `KEY_MEDIA_PREV_TRACK`, `KEY_VOLUME_UP`, `KEY_VOLUME_DOWN`, `KEY_VOLUME_MUTE`

주: 실제 `windows.vk` 매핑은 표준 Virtual-Key를 따릅니다. HID Usage 매핑은 필요 시 병기합니다.

#### 2.5.4 단축키 프리셋 카테고리/카탈로그(Windows 중심, 요약)

- Editing: `SC_COPY(CTRL+C)`, `SC_PASTE(CTRL+V)`, `SC_CUT(CTRL+X)`, `SC_UNDO(CTRL+Z)`, `SC_REDO(CTRL+Y)`, `SC_SELECT_ALL(CTRL+A)`, `SC_SAVE(CTRL+S)`, `SC_OPEN(CTRL+O)`, `SC_NEW(CTRL+N)`, `SC_PRINT(CTRL+P)`
- Navigation/App: `SC_FIND(CTRL+F)`, `SC_FIND_NEXT(F3)`, `SC_REFRESH(F5 or CTRL+R)`
- System/WindowMgmt: `SC_TASK_MANAGER(CTRL+SHIFT+ESC)`, `SC_CLOSE_WINDOW(ALT+F4)`, `SC_SWITCH_WINDOW(ALT+TAB)`(전환형), `SC_DESKTOP_SHOW(WIN+D)`, `SC_FILE_EXPLORER(WIN+E)`, `SC_RUN(WIN+R)`
- Browser: `SC_NEW_TAB(CTRL+T)`, `SC_CLOSE_TAB(CTRL+W)`, `SC_REOPEN_CLOSED_TAB(CTRL+SHIFT+T)`, `SC_NEXT_TAB(CTRL+TAB)`, `SC_PREV_TAB(CTRL+SHIFT+TAB)`, `SC_ADDR_FOCUS(ALT+D or CTRL+L)`
- Media: `SC_MEDIA_PLAY_PAUSE`, `SC_MEDIA_NEXT`, `SC_MEDIA_PREV`, `SC_VOL_UP`, `SC_VOL_DOWN`, `SC_MUTE`

주의(주입 제약): `CTRL+ALT+DEL`(Secure Attention Sequence), `WIN+L`(잠금) 등 일부 시스템 시퀀스는 일반 입력 주입으로 동작하지 않을 수 있습니다. 런타임에서 "미지원/권한 필요" 경고 처리 권장.

#### 2.5.5 식별자/네이밍 규칙

- 상수형 ID는 UPPER_SNAKE_CASE. 단일 키는 `KEY_*`, 모디파이어는 `MOD_*`, 단축키는 `SC_*` 접두.
- 라벨/검색을 위한 `aliases`(동의어) 필드 권장. 예: `SC_REFRESH` ↔ `SC_RELOAD`.
- 로컬라이즈 라벨은 `labelKo`/`labelEn` 병행.

#### 2.5.6 UI/상태/접근성 연계

- Disabled: §2.4 규칙 준수(입력 차단, 회색 톤, LiveRegion 토스트).
- Selected: 버튼 자체의 토글/Sticky Hold 상태 표현과 혼동하지 않도록 주의.
- 햅틱/피드백: §2.3 및 §5.2 정책 사용. 단축키 실행 시 디바운스는 `REINPUT_DEBOUNCE_MS = 150` 준수.

#### 2.5.7 품질 기준(검토 체크리스트)

- 중복/충돌 없는 ID 네임스페이스 유지.
- 모디파이어 Down/Up 순서 일관성.
- 보안/시스템 시퀀스의 지원 한계 주석 명시.
- i18n 라벨/접근성 문구 제공.

#### 2.5.8 후속 작업 안내(코드 반영 시)

- 위치 제안(코드, 참고용): `Android/app/src/main/java/.../presets/KeyboardPresets.kt`, `ShortcutPresets.kt` 등으로 분리(한 파일 하나의 주요 기능 원칙).
- 테스트: Windows 대상 주입 시퀀스 시뮬레이션과 디바운스/Sticky Hold 검증.

---

## 3. 4방향 D패드 컴포넌트

**강제 재연결 처리**: 기본 HID 키보드 기능(방향키)이므로 재연결 중에도 정상 동작, Disabled 상태로 전환하지 않음

### 3.2 구조 및 색상 시스템

#### 3.2.1 구조
- 컨테이너 크기(권장): 120dp × 120dp 정사각형
- 모양: 원형 또는 둥근 사각형(`RoundedCornerShape(12.dp)` 이상)
- 배경: VectorDrawable(`dpad.xml`) 베이스 레이어 사용(뷰박스 12×12, 방향 팔 4개 폴리곤)
- 레이어: 베이스(배경) · 방향 하이라이트(선택 시)
- 최소 터치 영역: 48dp × 48dp

영역 분할(센서틱 히트맵):
- 중심 원형: 반지름 = 컨테이너의 0.25–0.30
- 방향(상/하/좌/우) 4개 + 대각선(좌상/우상/좌하/우하) 4개 = 총 8분할
- 판정 방식: 중심 기준 벡터 각도로 섹터 매핑(45° 단위, 경계 허용오차 10°)
  - 중앙 영역 입력: 입력 없음(무시)

#### 3.2.2 색상 시스템
`#C2C2C2`: 기본 테두리; 비활성(Disabled) 오버레이(alpha 0.6)
`#2196F3`: 선택(Selected) 하이라이트; 방향 영역 오버레이(alpha 0.3)
`#FFFFFF`: 텍스트/아이콘(배경 대비 4.5:1 이상일 때)

#### 3.2.3 이미지 소스 매핑(VectorDrawable `dpad.xml`)
- 뷰박스: 12 × 12. 폴리곤 4개가 각 방향 팔을 구성(상/좌/우/하).
- 폴리곤 인덱스 → 방향 매핑(위에서부터 선언 순서 기준):
  - 0: Up, 1: Left, 2: Right, 3: Down
- 폴리곤 정점(정규 좌표, 원본 단위 그대로 사용 — 렌더 시 컨테이너 크기에 비례 스케일):
  - Up: [(8.25,0.28),(8.24,2.98),(5.99,5.20),(3.75,2.96),(3.75,0.28)]
  - Left: [(0.28,8.25),(0.28,3.75),(2.98,3.76),(5.20,6.01),(2.96,8.25)]
  - Right: [(11.72,3.75),(11.72,8.25),(9.02,8.24),(6.80,5.99),(9.04,3.75)]
  - Down: [(8.25,11.72),(3.75,11.72),(3.75,9.04),(5.99,6.80),(8.24,9.02)]
- 대각선 하이라이트: 선택된 두 방향 팔의 폴리곤을 동시에 오버레이(예: UpLeft = Up + Left).
- 리소스 네이밍/적용 권장:
  - Android 리소스는 소문자 스네이크케이스 권장. 런타임 적용은 `dpad` 리소스 ID 기준으로 관리.
  - VectorDrawable(`ic_dpad.xml`) 사용 권장. 빌드 시 `DPad.svg` → `ic_dpad.xml` 변환 적용됨.

### 3.3 상태 모델
- `isEnabled: Boolean` — 입력 가능 여부(Enabled/Disabled)
- `currentDirection: Direction?` — 현재 선택된 방향(null = 없음)
  - `Direction = Up | Down | Left | Right | UpLeft | UpRight | DownLeft | DownRight`
- `activePointerId: PointerId?` — 동일 터치 유지 전환 보장을 위한 포인터 식별자

### 3.1 입력 처리 유저 플로우
- 탭:
  - 방향 영역 탭: 해당 방향 Down → Up 즉시 전송
  - 중앙 영역 탭: 입력 없음(무시)
- 드래그 전환:
  - 동일 포인터 유지 시 섹터 간 전환 허용(§1.2.1 DPad 예외)
  - 전환 시 이전 방향 Up → 새 방향 Down(레이턴시 최소화, 최대 1프레임 내)
- 대각선 입력:
  - 대각선 섹터에서는 두 방향을 동시에 Down/Up 처리(예: UpLeft = Up+Left)
- 디바운스:
  - 동일 방향 재탭 50ms 이내 무시(`REINPUT_DEBOUNCE_MS = 50`)

### 3.4 시각/피드백

- Selected 방향: VectorDrawable(`dpad.xml`) 내 해당 방향 팔(폴리곤)을 선택 색으로 오버레이 + 컨테이너 테두리 `#2196F3`
- Unselected: 원본 상태 유지
- 햅틱:
  - 탭/전환: Light 1회

### 3.5 Disabled 상태 정의(§10.4 준수)

- 용어: 선택 상태는 `Selected/Unselected`, 입력 가능 상태는 `Enabled/Disabled`로 구분한다(혼동 금지).

트리거/복구(§10.4 연동):
- 개별 비활성: `UI_DISABLE_SELF_REQUEST(componentId)` 수신 시 본 컴포넌트만 비활성화(§10.4.1).
- 전체 비활성: `UI_DISABLE_ALL_TOUCHABLES_REQUEST(pageId)` 수신 시 페이지 내 터치 가능한 요소 전체 비활성화(§10.4.2).
- 강제 해제: 동일 소스 재탭 등으로 `UI_FORCE_ENABLE_ALL_TOUCHABLES_REQUEST` 수신 시 즉시 활성화 복구(§10.4.2). DPad는 해당 요청을 발신하지 않는다.
- 작업 완료: `TASK_COMPLETED` 수신 시 활성 복귀(§10.4.3).

시각:
- DPad 컴포넌트 자체적으로 Disabled 상태 스타일 적용. 회색 톤 `#C2C2C2`(alpha 0.6), 선택 하이라이트(Selected 오버레이) 및 컨테이너 테두리 하이라이트는 숨김.
- 전환 애니메이션: 150–250ms `tween`으로 알파/색상을 부드럽게 변경.

동작:
- 입력 차단: `enabled=false` 또는 포인터 이벤트 소비로 모든 탭/드래그 입력을 무효화.
- 포커스/접근성 포커스블 비활성화.

이벤트/상태 정리:
- Disabled 진입 시:
  - `currentDirection != null`이면 즉시(최대 1프레임 내) 해당 방향의 Up 신호를 주입한다. 대각선인 경우 두 방향 모두 Up 주입.
  - `currentDirection = null`, `activePointerId = null`로 초기화.
- Disabled 해제 시:
  - 자동 Down 또는 재선택 없음. 사용자의 새로운 탭/드래그 입력을 대기한다.

드래그 중 비활성 전환:
- 포인터가 유지된 상태에서 비활성 전환되면, 위 "Disabled 진입 시" 규칙에 따라 Up을 즉시 주입하고 이후 포인터 입력을 무시한다(재활성 전까지).

구현 지침(§10.4.6 참조):
- 비활성 상태를 시각적으로 표현하는 컨테이너를 상위에 위치시켜 회색 오버레이와 입력 차단을 통합 적용해야 함.
- 터치 이벤트 처리에서 비활성 상태를 확인하여 이벤트를 조기에 차단해야 함.

접근성:
- `contentDescription`에 "(비활성)" 문구를 부가하고, 상태 변화는 상단 상태 알림 토스트로 LiveRegion(Polite) 고지.

표준 색상/알파(권장):
- DISABLED_OVERLAY_COLOR = `#C2C2C2`
- DISABLED_OVERLAY_ALPHA = 0.6

### 3.6 설계 요구사항

> 📋 **기술적 구현 세부사항**: DPad 컴포넌트의 상세한 구현 알고리즘은 별도 기술 문서 `Docs/Android/technical-guide-app.md` §3.2(DPad 컴포넌트 구현)를 참조하세요.

**주요 구현 요구사항:**
- **8분할 섹터 판정**: 각도 기반 방향 감지 알고리즘
- **방향 전환 처리**: 드래그를 통한 연속 방향 전환 지원
- **대각선 입력**: 두 방향 키 동시 처리
- **시각적 피드백**: VectorDrawable 기반 방향 하이라이트

**성능 요구사항:**
- 입력 지연 < 50ms
- 60fps 애니메이션 유지
- 터치 추적 연속성 보장

### 3.7 상수 정의
- 재입력 디바운스: 50ms
