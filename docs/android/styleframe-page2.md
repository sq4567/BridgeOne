---
title: "Styleframe - Page 2 (MultiCursor Touchpad)"
description: "풀 와이드 터치패드 + 멀티 커서(최대 4개) 전용 페이지. 그리드 분할 / 직접 전환 버튼 두 가지 레이아웃 모드."
tags: ["styleframe", "multi-cursor", "touchpad", "full-width", "grid-split", "direct-switch", "ui"]
version: "v1.0"
owner: "Chatterbones"
updated: "2026-04-01"
note: "본 문서에 존재하는 모든 상수값 및 설정값은 초기 값으로, 확정된 고정값이 아님"
---

# Page 2 스타일프레임 문서

## 1. 개요

이 문서는 풀 와이드 터치패드 + 멀티 커서 전용 페이지(Page 2)의 스타일프레임을 정의합니다. Page 1과 달리 Actions 패널이 없으며, 터치패드가 전체 화면 너비와 높이를 점유합니다.

`CursorModeButton`이 이 페이지에서만 활성화되어 싱글 ↔ 멀티 커서 전환이 가능합니다. 멀티 커서 활성 시 최대 4개의 PC 커서를 독립 제어할 수 있으며, 터치패드를 N개 영역으로 분할하는 **그리드 분할 모드**와 전체 면적을 유지하면서 하단 버튼으로 전환하는 **직접 전환 버튼 모드** 두 가지를 지원합니다.

> **상세 아키텍처**: 전체 시스템 아키텍처는 [`technical-specification.md` §3]를 참조하세요.
> **용어 정의**: 용어 정의는 [`technical-specification.md` §6.2 Android 플랫폼 용어집]을 참조하세요.

**참조**: `docs/android/component-touchpad.md` §1.2(Page 2 레이아웃 구조), §1.6(CursorCountPopup), §3.2.4(커서 모드 플로우), `docs/android/technical-specification-app.md` §2.2.6(멀티 커서 알고리즘 명세).

**페이지 순서 내 위치**:
- Page 1: 터치패드 + Actions (상대좌표)
- **Page 2: 풀 와이드 터치패드 (멀티 커서)** ← 이 문서
- Page 3: 절대좌표 패드
- Page 4: 키보드 중심
- Page 5: Minecraft 특화

## 2. 레이아웃 구조

- **단일 영역 구조**: 터치패드가 전체 너비 × 전체 높이를 점유 (Page 1의 64/36 분할 없음)
- **ControlButtonContainer**: 상단 오버레이로 터치패드 위에 표시 (`CursorModeButton` 포함, 활성화)
- 여백: 없음 (터치패드가 Safe Zone 내 전체 점유)
- 방향: Portrait 최적화

### 2.1 싱글 커서 모드 레이아웃

```text
┌──────────────────────────────────────────────────────────────────────────────┐
│  [Click] [Move] [Scroll] [Cursor ▼] [DPI]  ← ControlButtonContainer(상단)  │
│                                                                              │
│   ┌──────────────────────────────────────────────────────────────────────┐   │
│   │                                                                      │   │
│   │                                                                      │   │
│   │              Touchpad Area (전체 너비 × 전체 높이)                    │   │
│   │                       단일 패드 (전체 면적)                           │   │
│   │                                                                      │   │
│   │                                                                      │   │
│   └──────────────────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────────────────┘
```

- `TouchpadAreaWrapper`가 Safe Zone 내 전체 공간 점유
- 기존 `TouchpadWrapper`의 모든 기능 그대로 사용 (클릭, 스크롤, DPI 등)
- `ControlButtonContainer` 상단 오버레이로 표시 (Page 1과 동일)

### 2.2 멀티 커서 — 그리드 분할 모드

멀티 커서 활성화 + `GRID` 레이아웃 모드일 때:

**2개 분할 (좌/우 50/50)**:
```text
┌────────────────────────────────┬────────────────────────────────┐
│                                │                                │
│      PadArea 1 (Selected)      │    PadArea 2 (Unselected)      │
│      ← 테두리 표시              │    ← dim 오버레이 (alpha 0.3)  │
│                                │                                │
└────────────────────────────────┴────────────────────────────────┘
```

**3개 분할 (L자형)**:
```text
┌────────────────────────────────┬────────────────────────────────┐
│                                │                                │
│      PadArea 1 (Selected)      │    PadArea 2 (Unselected)      │
│      ← 테두리 표시              │                                │
│                                ├────────────────────────────────┤
│                                │    PadArea 3 (Unselected)      │
│                                │                                │
└────────────────────────────────┴────────────────────────────────┘
```

**4개 분할 (2×2 그리드)**:
```text
┌────────────────────────────────┬────────────────────────────────┐
│    PadArea 1 (Selected)        │    PadArea 2 (Unselected)      │
│    ← 테두리 표시                │    ← dim 오버레이              │
├────────────────────────────────┼────────────────────────────────┤
│    PadArea 3 (Unselected)      │    PadArea 4 (Unselected)      │
│                                │                                │
└────────────────────────────────┴────────────────────────────────┘
```

시각 토큰:
- Selected 패드 테두리: 활성 패드의 현재 모드 색상 (`component-touchpad.md` §2.2 규칙 적용)
- Unselected 패드: 테두리 없음 + 검정 dim 오버레이 (`alpha = 0.3f`)
- 패드 간 경계선: 1dp, `alpha = 0.2f`
- 패드 전환 애니메이션: 300ms `ease-out` (테두리 이동)

### 2.3 멀티 커서 — 직접 전환 버튼 모드

멀티 커서 활성화 + `DIRECT_SWITCH` 레이아웃 모드일 때:

```text
┌──────────────────────────────────────────────────────────────────────────────┐
│  [Click] [Move] [Scroll] [Cursor ▼] [DPI]  ← ControlButtonContainer        │
│                                                                              │
│   ┌──────────────────────────────────────────────────────────────────────┐   │
│   │                                                                      │   │
│   │              전체 면적 (활성 패드의 입력 영역)                         │   │
│   │                                                                      │   │
│   │   ┌──────────────────────────────────────────────────────────────┐   │   │
│   │   │  [★ Pad 1]  │    [Pad 2]    │    [Pad 3]    │  ...          │   │   │
│   │   └─────── PadSwitchButtonPanel (하단 40dp 오버레이) ─────────────┘   │   │
│   └──────────────────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────────────────┘
```

`PadSwitchButtonPanel` 시각 토큰:
- 높이: 40dp, 전체 너비, N등분
- 활성 버튼: 강조색 배경 (`#2196F3`) + 볼드 텍스트
- 비활성 버튼: 반투명 배경 (`alpha = 0.4f`)
- 터치패드 이벤트 소비 — 버튼 탭이 커서 이동으로 처리되지 않음

### 2.4 CursorCountPopup

- **트리거**: `CursorModeButton` 탭 (싱글 → 멀티 전환 시도 시)
- **위치**: `CursorModeButton` 위쪽 소형 팝업
- **구성**: "2", "3", "4" 버튼 3개
- **선택**: 탭 시 즉시 팝업 닫힘 + 선택한 커서 수로 멀티 커서 활성화
- **취소**: 팝업 외부 탭 시 닫힘 (멀티 커서 미활성화)

```text
                 ┌─────────────────┐
                 │  [2]  [3]  [4]  │  ← CursorCountPopup
                 └─────────────────┘
    [Cursor ▼]  ← CursorModeButton
```

## 3. 상호작용 및 상태

### 3.1 CursorModeButton 상태

| 상태 | 표시 | 탭 동작 |
|------|------|---------|
| 싱글 커서 | 아이콘: 커서 1개 | CursorCountPopup 표시 |
| 멀티 커서 활성 (N개) | 아이콘: 커서 N개 + 배지 | 즉시 싱글 복귀 |

### 3.2 패드 전환 (그리드 분할 모드)

- 비활성 패드 영역 탭 → 즉시 해당 패드로 전환
- `MultiCursorViewModel.switchPad(index)` 호출
- 전환 시 `ControlButtonContainer` 버튼 상태 즉시 갱신 (새 활성 패드의 `PadModeState` 반영)
- 전환 햅틱: Medium (50ms)

### 3.3 패드 전환 (직접 전환 버튼 모드)

- `PadSwitchButtonPanel`의 버튼 탭 → 즉시 해당 패드로 전환
- 버튼 탭은 터치패드 이벤트 소비 (커서 이동 없음)
- 전환 햅틱: Medium (50ms)

### 3.4 소리 감지 패드 전환

- **조건**: 멀티 커서 활성 + 설정에서 소리 감지 옵션 ON + `RECORD_AUDIO` 권한 승인
- **트리거**: 솔 근방 주파수(~784Hz) + 볼륨 임계값 동시 만족
- **동작**: 다음 패드로 순환 전환 (modulo cursorCount)
- **쿨다운**: 트리거 후 500ms 재트리거 방지
- **피드백**: 햅틱 Medium + 토스트 "패드 N 전환" (1초)
- **마이크 권한 없음**: 기능 비활성화 + 안내 토스트

### 3.5 패드 경계 홀드 리셋

- 손가락이 패드 경계를 넘는 순간 터치업 이벤트 발생 → 홀드 상태 자동 리셋
- **드래그 연속 워크플로우**: 보조 하드웨어 버튼으로 클릭 홀드 유지 → 패드 경계를 넘어 드래그 가능
- 보조 버튼이 독립 HID 디바이스로 처리되므로 앱 내 추가 구현 불필요

### 3.6 테두리 색상 상태 규칙

그리드 분할 모드에서 Selected 패드에만 테두리 표시. 색상은 해당 패드의 현재 모드 우선순위 따름:

| 우선순위 | 상태 | 테두리 색상 |
|---------|------|------------|
| 1 (최고) | 스크롤 모드 | `#84E268` (초록) |
| 2 | 우클릭 모드 | `#F3D021` (노란) |
| 3 (기본) | 좌클릭 + 이동 | `#E91E63` (핑크) |

### 3.7 Windows 서버 연동

| 이벤트 | 전송 명령 |
|--------|----------|
| 멀티 커서 활성 | `{"command": "show_virtual_cursor", "cursor_count": N}` |
| 멀티 커서 비활성 | `{"command": "hide_virtual_cursor"}` |
| 패드 전환 | `{"command": "switch_cursor", "pad_index": N}` |

- Essential 모드(서버 미연결): 전송 스킵, 앱 내 상태만 관리

## 4. 유저 플로우

### 4.1 멀티 커서 활성화

1. Page 2 진입 → 싱글 커서 풀 와이드 터치패드 표시
2. `CursorModeButton` 탭 → `CursorCountPopup` 표시
3. "2" / "3" / "4" 선택 → 멀티 커서 활성화
4. 레이아웃 모드에 따라 그리드 분할 또는 직접 전환 버튼 표시
5. Windows 서버에 `show_virtual_cursor` 전송 (Standard 모드 시)

### 4.2 그리드 분할 모드에서 패드 전환

1. 비활성 패드 영역 탭
2. 해당 패드로 즉시 전환 (테두리 이동, 300ms ease-out)
3. `ControlButtonContainer`가 새 패드의 `PadModeState` 반영
4. 활성 패드에서 커서 제어 계속

### 4.3 직접 전환 버튼 모드에서 패드 전환

1. 하단 `PadSwitchButtonPanel`의 원하는 패드 버튼 탭
2. 해당 패드로 즉시 전환
3. 전체 터치패드 면적에서 계속 조작

### 4.4 멀티 커서 비활성화

1. `CursorModeButton` 재탭 → 즉시 팝업 없이 싱글 커서 복귀
2. 분할 레이아웃 해제 → 단일 전체 면적으로 복원
3. Windows 서버에 `hide_virtual_cursor` 전송

## 5. 반응형/적응 규칙

- **소형 화면 (폭 < 360dp)**:
  - 그리드 분할 시 4개 패드는 각 패드 최소 폭 80dp 보장
  - `PadSwitchButtonPanel` 높이 36dp로 축소
  - `ControlButtonContainer` 버튼 간격 8dp로 축소
- **중형 화면 (360dp ≤ 폭 < 600dp)**:
  - 기본 레이아웃 유지
- **대형 화면 (폭 ≥ 600dp, Landscape)**:
  - 그리드 분할 패드 비율은 세로 분할 방향으로 재계산
  - `PadSwitchButtonPanel`을 우측 세로 배치 가능 (공간 최적화)
- **높이 제약**: Safe Zone 보장 후 나머지 전체를 터치패드에 할당

## 6. 접근성

- **CursorModeButton**: `contentDescription` = "커서 모드: 싱글" / "커서 모드: 멀티 N개"
- **각 PadArea**: `contentDescription` = "패드 N: 비활성. 탭하면 이 패드로 전환" / "패드 N: 활성"
- **PadSwitchButtonPanel 각 버튼**: `contentDescription` = "패드 N 전환. 현재: 비활성" / "현재 활성"
- **CursorCountPopup 버튼**: `contentDescription` = "커서 N개로 멀티 커서 시작"
- 고대비 모드: Selected 패드 테두리 두께 3dp로 증가, Unselected dim alpha 0.4로 상향

## 7. 구현 메모(개발자용)

- **Composable 구조**: `Page2MultiCursorTouchpad` → `TouchpadAreaWrapper` + `ControlButtonContainer` + `CursorCountPopup`
- **ViewModel**: `MultiCursorViewModel` (앱 수준 싱글톤) — 멀티 커서 상태, 레이아웃 모드, 패드 전환
- **레이아웃 계산**: `MultiCursorLayoutCalculator` — N개 패드 영역을 `RectF`로 계산
  - 2개: 좌/우 50/50 분할
  - 3개: 좌 50% / 우상 50%×50% / 우하 50%×50% (L자형)
  - 4개: 2×2 그리드
- **패드 분할 렌더링**: `MultiCursorState.isActive && layoutMode == GRID`일 때 N개 `PadArea` 렌더링. 각 독립 `pointerInput` 모디파이어
- **PadSwitchButtonPanel**: `down.changes.forEach { it.consume() }`로 터치패드 이벤트 차단
- **소리 감지**: `SoundPadSwitchDetector` (AudioRecord + FFT) — 멀티 커서 활성 + 권한 보유 시에만 시작
- **상태 저장**: 레이아웃 모드 설정(SharedPreferences). 멀티 커서 활성 여부는 앱 수명 내에서만 유지(재시작 시 싱글 복귀)
- **성능**: 입력 지연 < 50ms, 패드 전환 애니메이션 300ms

---

문서 간 역할 분리: 멀티 커서 알고리즘 명세는 `docs/android/technical-specification-app.md` §2.2.6, 컴포넌트 구조는 `docs/android/component-touchpad.md` §1.2, 전체 시스템 플로우는 `docs/technical-specification.md` §4.4.3을 참조하세요.
