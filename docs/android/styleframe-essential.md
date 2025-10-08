---
title: "Styleframe - Essential Mode"
description: "Essential 모드(Windows 서버 미연결) 전용 페이지 스타일프레임. BIOS/로그온 등 OS 부팅 전후 모든 상황에서 필수 입력을 제공합니다."
tags: ["styleframe", "essential", "ui"]
version: "v0.1"
owner: "Chatterbones"
updated: "2025-09-19"
note: "본 문서에 존재하는 모든 상수값 및 설정값은 초기 값으로, 확정된 고정값이 아님"
---

# Essential 모드 스타일프레임 문서

## 1. 개요

이 문서는 Essential 모드 전용 페이지의 **UI 레이아웃과 스타일**을 정의합니다. 본 페이지는 PC가 Windows 서버와 연결되지 않은 모든 상태(BIOS/UEFI 등 OS 부팅 전 단계 포함)에서 표준 HID 입력 장치로 동작할 수 있도록 최소한의 UI를 제공합니다.

> **상세 아키텍처**: 전체 시스템 아키텍처는 [`technical-specification.md` §3]를 참조하세요.
> **용어 정의**: 용어 정의는 [`technical-specification.md` §6.2 Android 플랫폼 용어집]을 참조하세요.
> **유저 플로우 및 모드 전환**: Essential 페이지의 상세한 유저 플로우, 진입/해제 트리거, 모드 전환 로직은 `docs/Android/design-guide-app.md` §8.6을 참조하세요.

**참조**: `docs/design-guide-app.md` §10.2.3, §10.6.1, §10.7(플로우), `docs/usb-hid-bridge-architecture.md` §2.2, §5.1.1, §6.10(BOOT↔REPORT), `docs/PRD.md` §1.3~§1.4(핵심 가치/경로).

## 2. 레이아웃 구조

- 기본 그리드: 2-열 구조(좌: Touchpad, 우: Minimal Actions). 페이지 네비게이션/인디케이터는 기본 숨김.
- 권장 비율(가로 기준): 좌측 72% / 우측 28% (작은 화면에서는 좌 68%/우 32%까지 허용).
- 화면 여백: 바깥 16dp, 컬럼 간격 12dp, 그룹 간 세로 간격 12~16dp.
- 상태 안내: 상단 토스트(`docs/design-guide-app.md` §5.2)로만 제공. 별도 배너 없음.

### 2.1 좌측 Touchpad 영역

- 배치: 좌측 모서리 밀착(anchor-left), 상하 중앙 정렬.
- 크기: 컬럼 폭 100%, 1:2 종횡비(Width:Height), 최소 폭 320dp, 최소 높이 560dp.
- 모서리/마스크: 12dp 라운드, 상태 우선순위에 따른 경계선(`docs/touchpad.md` §3 참조).
- 모드/옵션 노출: 없음. 패드 내부 탭=좌클릭 단발만 허용. 드래그/스크롤/우클릭/멀티커서는 Disabled.
- 구현 소스: `docs/touchpad.md`의 `Touchpad1Area`만 사용.
- 고정 모드: `PrimaryMode=MOVE`, `MoveMode=FREE`, `ClickMode=LEFT`, `CursorMode=SINGLE`.

시각 토큰(권장):
- 배경: `#121212`
- 패드 표면: `#1E1E1E`
- 경계/강조: `#2196F3`(선택/포커스), Disabled는 컴포넌트별 `#C2C2C2` 60% alpha 적용

### 2.2 우측 Minimal Actions 패널

- 스크롤 컨테이너. 상단에서 하단: Boot Keyboard Cluster(옵션)만 포함.
- 터치 타겟 ≥ 56dp, 간격 8~12dp, 리플 비활성.

### Boot Keyboard Cluster

- 구성: `Del`, `Esc`, `Enter`, `F1–F12`(수평 스크롤), `Arrow Keys(↑/↓/←/→)` D-Pad.
- 노출 조건: 동글/펌웨어가 HID Boot Keyboard를 지원하는 경우 `Enabled`. 미지원이면 전부 `Disabled` 상태로 표기하고 탭 시 안내 토스트 "이 기능은 현재 하드웨어에서 사용할 수 없습니다".
- 상호작용:
  - `Del`, `Esc`, `Enter`, `F1–F12`: 단발 입력(길게 눌러도 반복 없음).
  - `Arrow Keys`: 단발 입력(길게 눌러도 반복 없음). 입력 시 순간 `Selected` 시각 상태.
- 레이아웃:
  - **Essential Entry Key**: `Del` 키를 최상단에 독립적으로 배치 (BIOS 진입용 강조 표시).
  - **Function Keys Container**: F1~F12를 컨테이너 버튼으로 그룹화. 탭 시 팝업 오버레이로 3×4 그리드 표시.
  - **Main Keys Row**: `Esc`, `Enter`를 한 줄에 배치.
  - **Navigation Cluster**: 방향키를 D-Pad 형태(3×3)로 배치. 중앙은 비어있으며 상/하/좌/우 방향만 사용.
  - **Spacing**: 그룹 간 12dp 간격, 버튼 간 8dp 간격 유지.
- 컨테이너 버튼 상세:
  - **F1~F12 컨테이너**: 메인 라벨 "F1-F12", 탭 시 팝업으로 F1~F12 키들을 3×4 그리드 형태로 표시.
  - **상호작용**: 컨테이너 탭 → 팝업 표시, F키 탭 → 해당 키 입력 후 팝업 자동 닫힘.
  - **시각 피드백**: 컨테이너는 기본 버튼 스타일, 팝업은 스크림 배경에 둥근 사각형 컨테이너.

### 2.3 ASCII 레이아웃

```text
┌──────────────────────────────────────────────────────────────────────────────┐
│ ┌──────────────────────────────────────────────┐  ┌─────────────────────────┐ │
│ │                Touchpad (1:2)                │  │   Boot Keyboard         │ │
│ │  [Control Buttons · Click | Move (only)]     │  │   Cluster (옵션)        │ │
│ │                                              │  │                         │ │
│ │                                              │  │      [Del] ⚡Essential   │ │
│ │                                              │  │                         │ │
│ │                                              │  │  ┌─────────────────┐    │ │
│ │                                              │  │  │  [F1-F12]       │    │ │
│ │                                              │  │  │  컨테이너 버튼  │    │ │
│ │                                              │  │  └─────────────────┘    │ │
│ │                                              │  │                         │ │
│ │                                              │  │  [Esc]      [Enter]     │ │
│ │                                              │  │                         │ │
│ │                                              │  │        [↑]              │ │
│ │                                              │  │  [←]   [ ]   [→]        │ │
│ │                                              │  │        [↓]              │ │
│ └──────────────────────────────────────────────┘  └─────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────────────┘
  «권장 비율: 좌 72% / 우 28%, Portrait 기준, Del 키는 Essential 진입용 최상단 배치, F1-F12는 컨테이너 버튼으로 그룹화, D-Pad는 3×3 중앙 비움»
```

## 3. 허용/비활성 기능(프로토콜 제약)

- 허용(UI/신호): X/Y 상대 이동, 좌클릭 단발.
- 선택 허용(Boot Keyboard): `Del`, `Esc`, `Enter`, `F1–F12`, `Arrow Keys(↑/↓/←/→)`. 하드웨어/펌웨어가 Boot Keyboard를 지원하지 않는 경우 해당 UI는 `Disabled`.
- 비활성: Wheel 보고 금지(항상 0), 클릭-드래그 금지, 확장 리포트/매크로/기타 키보드 키(일반 타이핑)/페이지 전환/인디케이터 제스처.
- UI 정책: 휠/스크롤 관련 UI는 Essential에서 숨김(`Disabled`). 시도 시 토스트: "고급 기능은 Windows 서버 연결 시 사용 가능합니다"(파란색, 2초).

## 4. 상호작용 및 상태

- 용어: 선택 상태 `Selected/Unselected`, 입력 가능 여부 `Enabled/Disabled` [[memory:5809234]].
- 피드백: 성공 `Success`(초록), 오류 `Error`(빨강), 진행 `Streaming`(파랑) 아이콘/색상 토큰 사용. 과다 피드백 방지(토스트+햅틱 중복 금지).
  - Essential 진입 시: 상단 토스트(정보, 2초) 각 1회 순차 표시 "PC: Essential 모드", "Essential 페이지 표시" — 두 토스트 사이 간격 300ms(`docs/design-guide-app.md` §5.2 규격)
- 햅틱: Light(탭), Error(거부).
- 디바운스: 버튼 300~500ms, 드래그 전환 시 100ms 보호.
- 접근성: 라벨은 명령형.
  - 키 입력: 방향키는 길게 눌러 Auto-repeat(초기 지연 400ms, 반복 간격 60ms). Esc/Enter/F1–F12는 단발. 누름 동안 시각 상태는 `Selected`, 사용 불가 시 `Disabled`.

### 간이 Touchpad 요약

- 목적: Essential 단계에서 필수 입력만 제공하는 간이 터치패드 동작 요약.
- 구조:
  - 좌측 `Touchpad1Area` 단일 사용, 1:2 비율 고정. `ControlButtonContainer`는 숨김.
  - 모드 고정: `PrimaryMode=MOVE`, `MoveMode=FREE`, `ClickMode=LEFT`, `CursorMode=SINGLE`.
  - 테두리: 기본 `#2196F3`. 오류 시 빨간 점멸(§8 오류 처리 규칙 준수).
- 상호작용:
  - 탭: 좌클릭 단발.
  - 드래그: 커서 X/Y 상대 이동. 클릭-드래그(누른 채 이동)는 금지.
  - 더블탭/롱프레스/스크롤/우클릭/멀티 커서/DPI/스크롤 감도: 비활성(무효 입력, 토스트 정책 준수).

> **유저 플로우**: Essential 페이지의 상세한 유저 플로우와 모드 전환 로직은 `docs/Android/design-guide-app.md` §8.6을 참조하세요.

## 5. 반응형/적응 규칙

- 폭 < 360dp: 우측 패널을 아이콘형 단일 열로 축소. F1-F12 컨테이너는 팝업 내 그리드를 2×6으로 축소. 메인 키와 D-Pad는 단일 열 유지.
- 폭 ≥ 600dp(Landscape/Tablet): Touchpad 확대, 키 클러스터는 2열로 확장. F1-F12 컨테이너 팝업은 4×3 그리드로 확대. D-Pad는 고정 3×3 유지.
- 높이 제약: 키 클러스터를 우선 스크롤로 이동. F1-F12 컨테이너 팝업은 화면 중앙에 위치하여 가시성 우선.

## 6. 아이콘/이미지 가이드

- 내부 자산 우선: `res/drawable/`의 VectorDrawable(@drawable, xml) 사용. 예: `ic_keyboard.xml`, `ic_dpad.xml`, `ic_error.xml`, `ic_success.xml`.
- **Essential Entry Key**: `Del` 키는 텍스트 라벨 "Del" + ⚡ 아이콘으로 Essential 진입 기능 강조 표시.
- **컨테이너 버튼**: `ic_keyboard.xml` 아이콘 사용, 메인 라벨 "F1-F12".
- **메인 키**: `Esc`, `Enter`는 텍스트 라벨 키캡 스타일 사용(아이콘 불필요).
- **D-Pad 방향키**: 텍스트 화살표 심볼(←/↑/→/↓) 사용.
- 대체 텍스트: 버튼 라벨과 동일. Disabled 시 "비활성" 포함 가능. Del 키는 "Essential 진입 키" 추가.

## 7. 구현 메모(개발자용)

- Compose: Essential에서 페이지 인디케이터/탭/스와이프 네비게이션은 `Disabled`. 상태 안내는 상단 토스트(`docs/design-guide-app.md` §5.2)로 제공.
- 상태 저장: Essential에서는 별도 상태를 저장하지 않음. Standard 전환 시 Touchpad/DPI 등은 일반 규칙으로 복구.
 - HID/프로토콜: Boot 모드에서는 wheel=0 강제, 3버튼+X/Y만 보고. Windows 서버 연결 확인 시 내부 상태 `Standard`로 전환하여 휠/확장 허용.
 - Boot Keyboard(옵션): HID Usage 매핑 — Del `0x4C`, Esc `0x29`, Enter `0x28`, Arrow Right `0x4F`, Arrow Left `0x50`, Arrow Down `0x51`, Arrow Up `0x52`, F1–F12 `0x3A–0x45`. Boot Keyboard 8바이트 리포트(6KRO) 사용. 키 유지 시 반복 전송되므로 앱은 키다운 유지/해제만 정확히 관리(불필요한 폴링 금지).
- 오류 처리: USB 연결 끊김 시 즉시 장치 연결 대기 화면으로 자동 전환. 백오프 재연결은 `docs/design-guide-app.md` §10.2.4 준수.
- 성능 목표: 입력 지연 < 50ms, 60fps 유지. 프레임 주기 4–8 ms(125–250 Hz) 권장.

## 8. 상태 머신(개발 메모)

- states: {Essential, Standard}
- events: {UsbConnected, OsEnteredFromDongle, ServerHandshakeOk, UsbDisconnected}
- transitions:
  - Essential --(OsEnteredFromDongle | ServerHandshakeOk)--> Standard
  - Standard --(UsbDisconnected)--> Essential
