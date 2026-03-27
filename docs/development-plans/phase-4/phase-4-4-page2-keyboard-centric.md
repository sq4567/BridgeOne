---
title: "BridgeOne Phase 4.4: Page 2 — 키보드 중심 페이지"
description: "BridgeOne 프로젝트 Phase 4.4 - Standard 모드 Page 2: Modifiers, Navigation/Editing, Function Row, Shortcuts, Media, Lock Keys"
tags: ["android", "keyboard", "modifiers", "shortcuts", "media", "lock-keys", "ui"]
version: "v1.0"
owner: "Chatterbones"
updated: "2026-03-26"
---

# BridgeOne Phase 4.4: Page 2 — 키보드 중심 페이지

**개발 기간**: 4-5일

**목표**: 키보드 입력과 단축키 트리거에 특화된 Page 2를 구현합니다. 터치패드 없이 키 입력에 집중하는 전용 인터페이스입니다.

**핵심 성과물**:
- Modifiers Bar (Ctrl/Shift/Alt/Win) — Sticky/토글/홀드 3단계
- Navigation/Editing Grid (방향키 + 편집 키)
- Function Row (F1-F12 수평 스크롤)
- Shortcuts 패널 (12개 확장 단축키)
- Media Controls (Play/Pause, Stop)
- Lock Keys (CapsLock, NumLock, ScrollLock) — HID LED 동기화

**선행 조건**: Phase 4.2 (Page 1 + 페이지 네비게이션) 완료

**에뮬레이터 호환성**: 에뮬레이터에서 Page 2 레이아웃, Modifiers Bar, Navigation Grid, Function Row, Shortcuts, Media Controls UI 전체 개발 가능. Lock Keys HID LED 동기화(PC → ESP32 → Android)는 실기기에서 별도 검증 필요.

---

## 현재 상태 분석

### 기존 구현
- `KeyboardLayout.kt`: Standard 모드에서 3탭 레이아웃 (문자/숫자·기호/기능)
  - Tab 0: QWERTY 문자 + Shift/Ctrl/Alt + Space + Enter + 한/영
  - Tab 1: 숫자/기호 + Tab, Esc, Del, 한자, Home/End
  - Tab 2: F1-F12 + 화살표 + Win, PrtSc, Pause
- `KeyboardKeyButton.kt`: **Sticky Hold 이미 구현됨** (500ms 롱프레스 → 키 유지 → 재탭 해제)
  - Fill 애니메이션 (좌→우), 오렌지 테두리, 색상 전환
  - `isStickyLatched`, `stickyActivatedDuringPress` 상태 관리
  - **⚠️ Key Repeat(반복 전송)은 미구현** — Phase 4.4.2에서 추가 필요
- `HidConstants.kt`: KEY_F1~F12, 방향키, 수정자 키, `isModifierKeyCode()`, `modifierBitFlag()` 완비
- `StandardModePage.kt` → `KeyboardPage`: 수정자 키 추적 (`activeModifierKeys` MutableState) 로직 존재
- Modifiers 3단계 Sticky (탭/더블탭/롱프레스) 없음, Lock Keys HID LED 동기화 없음, Media Controls 없음
- **기존 3탭 키보드는 Page 2 구조와 완전히 다름** → 새로 구현
- **ESP32 펌웨어**: `tud_hid_set_report_cb()` 구현됨, `g_hid_keyboard_led_status`에 LED 상태 저장됨. 단, **UART를 통한 Android 전달 로직은 미구현** (TODO 주석만 존재)

### 목표 구조 (styleframe-page2.md 기준)
```
Page 2
├── 좌측 Key Cluster (64%)
│   ├── Modifiers Bar: [Ctrl] [Shift] [Alt] [Win]
│   ├── Navigation/Editing Grid
│   │   ├── Inverted-T 방향키
│   │   └── Backspace, Delete, Enter, Tab, Home, End, PgUp, PgDn
│   └── Function Row: F1-F12 (수평 스크롤)
└── 우측 Actions (36%)
    ├── Shortcuts (12개, 2열 그리드)
    ├── Media Controls (Play/Pause, Stop)
    └── Lock Keys (CapsLock, NumLock, ScrollLock)
```

---

## Phase 4.4.1: Page 2 레이아웃 및 Modifiers Bar

**목표**: Page 2 기본 레이아웃 구조와 Sticky Modifiers 구현

**개발 기간**: 1.5일

**세부 목표**:
1. `Page2KeyboardCentric` Composable:
   - 2열 Row: 좌측 64% / 우측 36%
   - 좌측: 세로 배치 (Modifiers → Nav/Edit → Function)
   - 우측: LazyColumn (Shortcuts → Media → Lock)
2. Modifiers Bar (상단 고정):
   - 4개 키: `Ctrl`, `Shift`, `Alt`, `Win`
   - **3단계 상호작용**:
     - 탭: 일시 고정 (다음 키 입력까지 유지, 최대 800ms 자동 해제)
     - 더블탭: 토글 고정 (해제까지 지속, UI에 토글 배지 표시)
     - 롱프레스 (≥400ms): 누르는 동안만 유지 (물리 키보드와 동일)
   - HID 전송:
     - 탭: KeyDown 전송 → 다음 키 입력 시 조합 전송 후 KeyUp
     - 더블탭: KeyDown 유지 → 재더블탭 또는 다른 동작으로 KeyUp
     - 롱프레스: 누르는 동안 KeyDown, 떼면 KeyUp
   - 시각 상태:
     - 기본: `#2196F3`
     - Selected (활성): 강조색 + 토글 배지
     - Disabled: `#C2C2C2` (alpha 0.6)
3. 페이지 전환 시 모디파이어 자동 해제:
   - 더블탭 토글 상태는 페이지 전환 시 자동 KeyUp + 해제

**신규 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/pages/standard/Page2KeyboardCentric.kt`
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/ModifiersBar.kt`

**참조 문서**:
- `docs/android/styleframe-page2.md` §2.1-1 (Modifiers Bar)
- `docs/android/component-design-guide-app.md` §2.3.1 (KeyboardKeyButton Sticky Hold)
- `docs/android/technical-specification-app.md` §2.3.2.7 (Modifiers Bar 3단계 Sticky 구현 요구사항)

> **⚠️ Phase 4.1.7 변경사항**: Page 2 레이아웃은 `AppState.Active` 박스 내 `padding(top=40dp, bottom=40dp)` 적용 영역 안에서 렌더링됨. Function Row, Lock Keys 등 하단 요소 배치 시 유효 화면 높이 = 전체 높이 − 80dp 기준 사용. 새 레이아웃 상수 추가 시 `ui/common/LayoutConstants.kt`에 함께 정의.

> **⚠️ Phase 4.1.8 변경사항**: 커스텀 토스트 시스템 도입. `android.widget.Toast` 사용 금지. 모든 알림은 `ToastController.show(message, ToastType, durationMs)` 로 표시. 타입: `INFO`(파란색) · `SUCCESS`(초록색) · `WARNING`(주황색, 검은 텍스트) · `ERROR`(빨간색). 무제한 표시: `TOAST_DURATION_INFINITE`.

**검증**:
- [ ] 4개 모디파이어 키 렌더링
- [ ] 탭 → 일시 고정 (다음 키와 조합 후 해제)
- [ ] 더블탭 → 토글 고정 (배지 표시)
- [ ] 롱프레스 → 누르는 동안만 유지
- [ ] 페이지 전환 시 자동 해제

---

## Phase 4.4.2: Navigation/Editing Grid

**목표**: 방향키 (Inverted-T) + 편집 키 그리드 구현

**개발 기간**: 1일

**세부 목표**:
1. Inverted-T 방향키:
   - `↑` 중앙 상단, `←`/`→` 좌우, `↓` 중앙 하단
   - Key Repeat: 길게 누르면 반복 전송
     - 초기 지연: 400ms
     - 반복 간격: 최소 40ms (25-30Hz 상한)
   - 기존 `KeyboardKeyButton` 재사용
2. 편집 키 (2-3열 그리드):
   - `Backspace`, `Delete`, `Enter`, `Tab`
   - `Home`, `End`, `PageUp`, `PageDown`
   - Backspace/Space/Enter: Key Repeat 지원
3. Key Repeat 구현:
   - `LaunchedEffect` 기반 반복 전송
   - OS 설정 기본값 존중, 앱 레벨 25-30Hz 스로틀링
   - 60fps 성능 저해 방지
   - **⚠️ 기존 Sticky Hold(500ms)와의 관계 정리**:
     - 현재 `KeyboardKeyButton`은 롱프레스 시 Sticky Hold 진입 (키 유지, 재탭 해제)
     - Key Repeat은 롱프레스 시 **반복 전송** (키 누름/뗌 반복)
     - 두 기능은 **상호 배타적**: `repeatEnabled=true`면 Sticky Hold 비활성, 반복 전송 수행
     - `KeyboardKeyButton`에 `repeatEnabled: Boolean = false` 파라미터 추가
     - 방향키, Backspace, Enter 등에 `repeatEnabled=true` 적용
     - 수정자 키(Ctrl, Shift 등)에는 `repeatEnabled=false` (Sticky Hold 유지)
   - **Phase 4.2.3의 Enter 롱프레스 반복 입력도 이 구현에 통합**

**수정 파일**:
- `Page2KeyboardCentric.kt`
- `KeyboardKeyButton.kt` (Key Repeat 기능 추가 — `repeatEnabled` 파라미터)

**참조 문서**:
- `docs/android/styleframe-page2.md` §2.1-2 (Navigation/Editing Grid)
- `docs/android/technical-specification-app.md` §2.3.2.6 (Key Repeat 구현 요구사항)

**검증**:
- [ ] Inverted-T 배치 정상
- [ ] 방향키 롱프레스 시 반복 전송
- [ ] 반복 주기 25-30Hz 이내
- [ ] 편집 키 8개 정상 동작

---

## Phase 4.4.3: Function Row 및 확장 Shortcuts

**목표**: F1-F12 수평 스크롤 + 12개 확장 단축키 패널

**개발 기간**: 0.5-1일

**세부 목표**:
1. Function Row (좌측 하단):
   - F1-F12 수평 스크롤 (Chips 또는 Compact 버튼)
   - 그룹 간 경계: 얇은 디바이더 (alpha 0.2)
   - 길게 누르기 반복 지원
2. Shortcuts 패널 (우측, 12개):
   - 기본 12개: `Ctrl+C`, `Ctrl+V`, `Ctrl+S`, `Ctrl+Z`, `Ctrl+Shift+Z`, `Ctrl+X`, `Ctrl+N`, `Ctrl+O`, `Ctrl+P`, `Ctrl+W`, `Ctrl+T`, `Alt+F4`
   - Phase 4.2에서 구현한 `ShortcutButton` 컴포넌트 재사용
   - 2열 그리드
   - `Alt+Tab`은 Modifiers + Tab 조합으로 대체 (UX 혼동 방지)

**수정 파일**:
- `Page2KeyboardCentric.kt`

**참조 문서**:
- `docs/android/styleframe-page2.md` §2.1-3 (Function Row)
- `docs/android/styleframe-page2.md` §2.2-1 (Shortcuts)

**검증**:
- [ ] F1-F12 수평 스크롤 동작
- [ ] 12개 단축키 2열 그리드
- [ ] 그룹 간 디바이더 표시

---

## Phase 4.4.4: Media Controls 및 Lock Keys

**목표**: 미디어 제어 버튼과 Lock Keys (HID LED 동기화)

**개발 기간**: 1일

**세부 목표**:
1. Media Controls:
   - `Play/Pause` (토글): HID Consumer Usage `0xCD`
   - `Stop`: HID Consumer Usage `0xB7`
   - 아이콘: `ic_play.xml`, `ic_pause.xml`, `ic_stop.xml`
   - Play/Pause는 토글형 (Selected 상태 시각 전환)
2. Lock Keys:
   - `CapsLock`, `NumLock`, `ScrollLock`
   - Selected = 켜짐 (라쳇), Unselected = 꺼짐
   - **HID LED Report 기반 상태 동기화**:
     - Host(PC)가 LED Report를 ESP32-S3로 전송
     - ESP32-S3가 UART를 통해 Android로 LED 상태 전달
     - LED Report 미수신 시: 2초 간격 폴링, 최대 3회 재시도
   - 상태 미동기화 감지 시: `ToastController.show("Lock Key 상태 동기화 실패", ToastType.ERROR, 2000L)` + 재동기화 시도
3. LED Report 수신 경로:
   - ESP32-S3 `tud_hid_set_report_cb()` → UART 알림 → Android 파싱
   - Phase 3.5.4에서 구현한 역방향 UART 알림 시스템 활용
   - 필요 시 새로운 이벤트 타입 정의: `EVENT_LED_STATUS (0x02)`

**신규 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/MediaControlButton.kt`
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/LockKeyButton.kt`

**수정 파일**:
- `Page2KeyboardCentric.kt`

**참조 문서**:
- `docs/android/styleframe-page2.md` §2.2-2 (Media Controls)
- `docs/android/styleframe-page2.md` §2.2-3 (Lock Keys)
- `docs/android/technical-specification-app.md` §2.3.2.8 (Media Controls 구현 요구사항)
- `docs/android/technical-specification-app.md` §2.3.2.9 (Lock Keys HID LED 동기화 구현 요구사항)

**⚠️ ESP32-S3 펌웨어 수정 필요 (Phase 4.4 사전 작업)**:
- `tud_hid_set_report_cb()` **이미 구현됨** (`hid_handler.c:313`): LED 상태를 `g_hid_keyboard_led_status`에 저장
- `hid_get_keyboard_led_status()` **이미 구현됨**: LED 상태 조회 함수
- **⚠️ 미구현 부분**: LED 상태 변경 시 **UART를 통해 Android로 알림 전송**하는 로직
  - `tud_hid_set_report_cb()` 내 `TODO` 주석 위치에 UART 전송 코드 추가 필요
  - 새 이벤트 타입 정의: `EVENT_LED_STATUS (0x02)` — `NotificationFrame` 또는 별도 프레임 형식
  - `uart_handler`를 통해 LED 상태 바이트 전송
- 이 작업은 유저가 펌웨어를 빌드해야 하므로 별도 안내

**검증**:
- [ ] Play/Pause 토글 동작 및 시각 전환
- [ ] Stop 단발 전송
- [ ] CapsLock 탭 → PC에서 토글 확인
- [ ] PC에서 CapsLock 상태 변경 → Android Lock Key UI 동기화
- [ ] LED Report 미수신 시 폴링 재시도 동작

---

## Phase 4.4 완료 후 Page 2 구조

```
Page 2 — Keyboard Centric
├── 좌측 Key Cluster (64%)
│   ├── Modifiers Bar
│   │   └── [Ctrl] [Shift] [Alt] [Win] — 3단계 Sticky
│   ├── Navigation/Editing Grid
│   │   ├── Inverted-T: ↑ ← ↓ → (Key Repeat)
│   │   └── Backspace, Delete, Enter, Tab, Home, End, PgUp, PgDn
│   └── Function Row
│       └── F1-F12 (수평 스크롤, Key Repeat)
└── 우측 Actions (36%)
    ├── Shortcuts (12개, ShortcutButton, 2열 그리드)
    ├── Media Controls
    │   └── [Play/Pause] [Stop]
    └── Lock Keys
        └── [CapsLock] [NumLock] [ScrollLock] — HID LED 동기화
```
