---
title: "BridgeOne Phase 4.8: Page 2 — 풀 와이드 터치패드 (멀티 커서 사전 준비)"
description: "BridgeOne 프로젝트 Phase 4.8 - Standard 모드 Page 2를 멀티 커서 전용 페이지로 정식화. Windows 서버(Phase 5) 준비 전, 앱 단독으로 완결 동작하는 멀티 커서 UI·상태·레이아웃 골격 구축"
tags: ["android", "multi-cursor", "touchpad", "full-width", "cursor-mode", "ui"]
version: "v2.0"
owner: "Chatterbones"
updated: "2026-07-01"
---

# BridgeOne Phase 4.8: Page 2 — 풀 와이드 터치패드 (멀티 커서 사전 준비)

**성격**: Windows 서버(Phase 5) 준비 전 **Android 단독 사전 준비 단계**. 이 Phase의 결과물은 Windows 서버가 없어도 앱 안에서 완결적으로 동작하는 멀티 커서 UI·상태·레이아웃 골격이며, PC 화면의 실제 가상 커서 표시·텔레포트 등 서버 의존 기능은 Phase 5에서 완성한다.

**목표**: Page 2를 멀티 커서 전용 페이지로 정식화한다. `CursorModeButton`으로 싱글 ↔ 멀티 커서를 전환하고, 멀티 커서 활성 시 터치패드를 N개(최대 4개)로 분할하거나 하단 전환 버튼으로 여러 패드를 독립 선택한다. 서버 연동은 명령 전송 훅 지점만 마련하고, 실제 왕복은 Phase 5로 넘긴다.

**핵심 성과물**:
- Page 2를 멀티 커서 홈으로 승격 (`CursorModeButton` 활성화)
- 커서 수 선택 팝업 → 멀티 커서 활성/비활성 흐름
- 페이지 전환에도 유지되는 멀티 커서 상태 홀더
- 그리드 분할 레이아웃 모드
- 직접 전환 버튼 레이아웃 모드
- 서버 명령 전송 훅 지점 + 서버 미연결 시 앱 내부 완결 동작

**이 Phase에서 다루지 않는 것 (Phase 5로 이관)**: PC 화면의 N-1개 가상 커서 실제 렌더링, 실제 커서 텔레포트, 커서 팩 감지·동기화, `show_virtual_cursor` 명령의 완전한 왕복(ACK 기반 커서 위치 수신). 근거: `docs/windows/technical-specification-server.md` §3.6.

**선행 조건**: Phase 4.3 (터치패드 고급 기능) 완료

**에뮬레이터 호환성**: 페이지 레이아웃, 커서 수 선택 팝업, 그리드 분할 UI, 직접 전환 버튼은 에뮬레이터에서 개발 가능. Windows 서버 가상 커서 연동은 Phase 5에서 실기기로 검증.

## 현재 상태 분석 (실제 코드 기준)

- `StandardModePage.kt`: 이미 `PAGE_COUNT = 5`, 무한 페이저(`page % PAGE_COUNT`) 구조. 현재 페이지 매핑은 index 0 `Page1TouchpadActions`, 1 `Page2TestTouchpad`(제어 버튼 없는 풀스크린 터치패드), 2 `Page3KeyboardPlaceholder`, 3 `Page4MinecraftPlaceholder`, 4 `Page5Settings`. **절대좌표 페이지는 없고 Page 5는 설정 페이지다.** → 페이지 수 확장·placeholder 재배치는 이미 다른 형태로 끝나 있으므로 이 Phase의 과제가 아니다.
- `Page2TestTouchpad.kt`: 멀티 커서 없는 풀스크린 터치패드. 이 페이지를 멀티 커서 홈으로 승격하는 것이 4.8.1의 출발점.
- `TouchpadWrapper.kt`: 싱글 커서 완전 구현. 멀티 커서 분기 없음.
- `ControlButtonContainer.kt`: `ControlButtonConfig(showCursorMode = false)` 기본값. `CursorModeButton`은 UI만 존재하고 현재 `enabled = false`("멀티 커서 미구현").
- `TouchpadMode.kt`: `CursorMode(SINGLE/MULTI)` 정의만 존재(동작 없음). `ClickMode`/`MoveMode`/`ScrollMode`/`DpiLevel` 등 모드 enum 완비. **`PadModeState`는 이미 존재**(`ModePreset` 스냅샷 용도, 필드 `clickMode`/`moveMode`/`scrollMode`/`dpi`). → 멀티 커서용으로 재정의하지 말고 재사용 가능 여부를 먼저 판단할 것.
- `StandardModePageState.kt`: 앱의 상태 홀더 컨벤션(AndroidX ViewModel 미채택, 평범한 클래스 + `remember` + `mutableStateOf`, 사이드이펙트는 Composable 콜백) 선례.
- 미존재(이 Phase에서 신규): `MultiCursorState`/`MultiCursorLayoutMode`, 멀티 커서 상태 홀더, 커서 수 선택 팝업, 영역 분할 계산기, 직접 전환 버튼 패널.

## 목표 구조 (component-touchpad.md §1.2 기준)

```
Page 2 — 풀 와이드 터치패드 (멀티 커서)
├── 터치패드 영역 (전체 너비 × 전체 높이, Actions 패널 없음)
│   ├── [싱글 커서] 전체 면적 단일 터치패드
│   └── [멀티 커서] 레이아웃 모드에 따라:
│       ├── [그리드 분할] N개 영역 자동 분할
│       └── [직접 전환 버튼] 전체 면적 유지 + 하단 전환 버튼 패널
├── ControlButtonContainer (상단 오버레이, CursorModeButton 포함)
└── 각종 팝업/오버레이 (커서 수 선택 팝업, 기존 가이드라인 등)
```

---

## Phase 4.8.1: Page 2를 멀티 커서 홈으로 정식화

**목표**: 현재 `Page2TestTouchpad`(제어 버튼 없는 풀스크린 터치패드)를 멀티 커서 전용 페이지로 승격한다. 상단 제어 버튼과 `CursorModeButton`이 표시되며, 이 단계에서는 아직 싱글 커서로만 동작한다.

**개발 기간**: 0.5일

**세부 목표**:
1. Page 2에 `ControlButtonContainer` 표시 (`ControlButtonConfig(showCursorMode = true)` — Page 1 구성과 달리 `CursorModeButton` 포함).
2. `CursorModeButton` 활성화 (현재 `enabled = false` → 탭 가능하도록).
3. 터치패드는 전체 너비 × 전체 높이 유지 (Page 1의 분할 비율 없음).
4. 파일/함수 rename(`Page2TestTouchpad` → 멀티 커서 홈에 맞는 이름) 여부는 세션 판단. rename 시 `StandardModePage.kt`의 `when(page % PAGE_COUNT)` 분기도 함께 수정.

> **계획과 다르게 구현된 부분**: 원 계획에는 없었으나, 구현 중 `ControlButtonContainer`가 화면 전체 너비로 늘어나면 버튼이 과도하게 커져 시각적 일관성이 깨지는 문제가 발견되어 폭을 조정했다. `ControlButtonContainer`를 터치패드 전체 너비가 아니라 Page 1의 터치패드 컬럼 폭과 동일한 비율(화면 폭 360dp 미만 60%, 그 외 64%)로 축소하고 터치패드 좌우 중앙(`Alignment.TopCenter`)에 정렬했다. 설계 문서 `docs/android/component-touchpad.md` §1.3에 Page 2 예외로 반영 완료.

**참조 문서**:
- `docs/android/component-touchpad.md` §1.2 (터치패드 영역 구조)
- `docs/android/component-touchpad.md` §1.3 (버튼 구성 독립성 원칙 + Page 2 폭 예외)

**검증**:
- [x] Page 2에 상단 제어 버튼 + `CursorModeButton` 표시, Page 1에서는 `CursorModeButton` 비표시
- [x] Page 2 터치패드가 전체 너비 점유
- [x] 기존 싱글 커서 동작 정상 유지
- [x] `ControlButtonContainer`가 Page 1 비율 폭으로 축소되고 터치패드 좌우 중앙에 정렬

---

## Phase 4.8.2: 멀티 커서 상태 구조 + 상태 홀더 + 커서 수 선택 팝업

> **⚠️ Phase 4.8.1 변경사항**:
> - `Page2TestTouchpad` → `Page2MultiCursorTouchpad`로 rename (`Page2MultiCursorTouchpad.kt`).
> - `ControlButtonContainer`의 CursorModeButton `onClick`은 현재 no-op(`{ }`) 상태. 이 Phase에서 팝업 콜백(`onCursorModeClick`)을 `ControlButtonContainer`에 추가하고 `Page2MultiCursorTouchpad` → `StandardModePage`까지 hoist.
> - `defaultFor(standardPage(1))`이 `showControlButtons = true`, `controlButtonConfig = ControlButtonConfig(showCursorMode = true)`로 변경됨 → 이 Phase에서 별도 설정 변경 불필요.

> **⚠️ 아키텍처 (Phase 4.7.4 결정)**: 멀티 커서 상태는 AndroidX ViewModel이 아니라 **평범한 클래스 상태 홀더 + `remember`**로 구현한다(`StandardModePageState` 선례). "페이지를 넘나들어도 상태 유지" 요구는 상태 홀더를 페이저 **상위**에서 `remember`로 1회 생성해 페이저 바깥에 hoist하면 달성된다. 사이드이펙트(서버 명령 전송·토스트 등)는 홀더에 넣지 않고 Composable 콜백에 둔다(4.7.4-C 철학).

**목표**: `CursorModeButton` 탭 시 커서 수(2/3/4)를 고르는 팝업을 띄우고, 멀티 커서 활성/비활성 흐름을 구현한다. 페이지 전환에도 유지되는 멀티 커서 상태를 상태 홀더로 확립한다.

**개발 기간**: 1.5일

**세부 목표**:
1. **멀티 커서 상태 데이터 구조**: `MultiCursorState`(활성 여부, 커서 수, 활성 패드 인덱스, 패드별 모드 상태 목록, 레이아웃 모드)와 `MultiCursorLayoutMode`(GRID / DIRECT_BUTTON, `technical-specification-app.md` §2.2.6 enum명과 통일)를 신규 정의. **패드별 모드 상태는 기존 `PadModeState`(`TouchpadMode.kt`) 재사용 가능 여부를 먼저 검토**하고, 그대로 쓸 수 없을 때만 확장한다(재정의 금지).
2. **상태 홀더**: 페이저 바깥에 hoist하는 상태 홀더를 신규 생성(`StandardModePageState`와 동일 계층·패턴). 활성화/비활성화/패드 전환 메서드를 제공하되, 서버 전송·토스트는 호출 측 콜백으로 위임.
3. **커서 수 선택 팝업**: `CursorModeButton` 탭(싱글→멀티 시도) 시 2/3/4 선택 팝업 표시, 선택 즉시 멀티 커서 활성화, 외부 탭 시 취소.
4. **비활성화**: 멀티 커서 상태에서 `CursorModeButton` 재탭 시 즉시 싱글 커서 복귀.
5. **제어 버튼 ↔ 활성 패드 연동**: `ControlButtonContainer`가 활성 패드의 모드 상태를 반영. `ModePresetButton` 등 프리셋 적용은 활성 패드에만 적용되고 다른 패드 상태는 보존.

**참조 문서**:
- `docs/android/component-touchpad.md` §1.7.2 (커서 수 선택 팝업 — CursorCountSelectionPopup)
- `docs/android/component-touchpad.md` §1.2.3 (멀티 커서 선택 상태 관리)
- `docs/android/component-touchpad.md` §3.2.4 (커서 모드 플로우)
- `docs/android/technical-specification-app.md` §2.2.6 (멀티 커서 알고리즘 명세)

**검증**:
- [x] `CursorModeButton` 탭 → 커서 수 선택 팝업 표시
- [x] 2/3/4 선택 후 멀티 커서 활성화
- [x] `CursorModeButton` 재탭 → 싱글 커서 복귀
- [x] 제어 버튼이 활성 패드의 모드 상태 반영
- [x] 페이지 전환 후 복귀 시 멀티 커서 상태 유지 (페이저 바깥 hoist)
- [x] 프리셋 적용이 활성 패드에만 반영되고 다른 패드 상태 보존 (4.8.2 시점엔 pad1만 도달 가능, "다른 패드 보존"은 4.8.3 스위칭 UI 추가 후 실질 검증)

---

## Phase 4.8.3: 그리드 분할 레이아웃 모드

> **⚠️ Phase 4.8.2 변경사항**:
> - 신규 상태 구조: `MultiCursorState`/`MultiCursorLayoutMode`(`TouchpadMode.kt`), 상태 홀더 `MultiCursorController`(`MultiCursorController.kt`, `ui/components/touchpad/`). `StandardModePage`가 `remember { MultiCursorController() }`로 페이저 바깥에서 1회 생성해 보유. 그리드 분할 UI에서 재사용할 것 — 재정의 금지.
> - `MultiCursorController.switchPad(index)`가 이미 정의되어 있으나 아직 어디서도 호출되지 않음. 이 Phase(비활성 영역 탭 → 패드 전환)에서 연결.
> - `Page2MultiCursorTouchpad`가 `multiCursorState`를 받아 `ControlButtonContainer`/`TouchpadWrapper`에 **활성 패드의 4개 모드 필드로 projection한 `effectiveState`**를 전달하는 로직이 이미 구현됨(읽기/쓰기 모두). 그리드 분할 UI 추가 시 이 로직을 건드릴 필요 없음 — 영역별 렌더링만 추가하면 됨.
> - `CursorCountSelectionPopup.kt` 신규(`ui/components/touchpad/`). `MULTI_CURSOR_COUNT_MIN/MAX` 상수도 `TouchpadMode.kt`에 정의됨.
> - `ControlButtonContainer`에 `onCursorModeClick: (() -> Unit)? = null` 파라미터 추가됨.
> - `Page2MultiCursorTouchpad`에 `onModePresetLongPress`/`modePresetPopupVisible`/`onModePresetConfirmed`/`onModePresetDismiss` 배선 및 `ModePresetPopup` 렌더 추가됨(Page1과 동일 패턴). 프리셋 confirm 시 멀티 활성이면 `multiCursor.updateActivePadMode`로, 아니면 기존 글로벌 경로로 라우팅.

**목표**: 멀티 커서 활성 시 터치패드를 N개 영역으로 자동 분할한다. 활성 영역에서만 커서를 제어하고, 비활성 영역 탭으로 패드를 전환한다.

**개발 기간**: 1.5일

**세부 목표**:
1. **N개 영역 계산**: 커서 수에 따른 영역 분할을 계산한다. 구체 분할 방식(N=2/3/4)은 설계 문서 기준을 따른다.

   > **⚠️ 테스트 컨벤션 (Phase 4.7.2)**: 영역 경계 계산은 순수 기하 함수로 추출하고 **동시에 단위 테스트**를 작성한다(`EdgeGeometryTest` 선례, JUnit4). 입력 좌표→영역 매핑이 순수 함수라 테스트 비용이 낮고 회귀 안전망 가치가 크다.

2. **분할 렌더링**: `MultiCursorLayoutMode.GRID`일 때 N개 영역을 렌더링하고 각 영역이 독립적으로 터치 이벤트를 처리한다. 활성 패드는 테두리로 표시(색상은 설계 문서 규칙), 비활성 패드는 dim 처리, 영역 간 구분선 표시.
3. **패드 전환**: 비활성 패드 영역 탭 시 활성 패드를 전환하고 제어 버튼 상태를 즉시 갱신. 전환 시 짧은 애니메이션.
4. **입력 격리**: 활성 패드에서만 커서 이동 입력이 반영되고 비활성 패드 입력은 무시.
5. **싱글 복귀**: 멀티 해제 시 분할 없이 단일 터치패드로 즉시 복원.

**참조 문서**:
- `docs/android/component-touchpad.md` §1.2 (터치패드 영역 구조)
- `docs/android/component-touchpad.md` §2.2 (테두리 색상 규칙 — 멀티 커서 활성 패드)
- `docs/android/technical-specification-app.md` §2.2.6.1 (N개 패드 영역 분할)
- `docs/android/technical-specification-app.md` §2.2.6.2 (영역 전환 감지)

> **계획과 다르게 구현된 부분**: 설계 문서 두 곳(`component-touchpad.md` §2.2.1과 §3.2.4.2)이 활성 패드 테두리 규칙을 서로 다르게 서술해 모순이 있었다(기능 모드 기반 vs 보라색 고정). **기능 모드 기반**(§2.2.1)으로 결정했다 — 활성 영역을 실제 `TouchpadWrapper` 인스턴스로 배치해 기존 테두리 로직을 그대로 재사용하고, 비활성 영역은 별도 dim 처리 `Box`로 렌더링하는 구조를 택했기 때문에 자연스럽게 이 방향과 맞았다. 또한 비활성 영역의 "반투명 처리"(§3.2.4.2) 구체 alpha 값이 설계 문서에 없어 신규 `MultiCursorConstants.kt`(`ui/common/`)에 `GRID_INACTIVE_PAD_DIM_ALPHA = 0.4f`, `GRID_PAD_SWITCH_ANIM_DURATION_MS = 200`로 확정해 기본값 주석과 함께 정의했다(초기에 함께 정의했던 구분 gap 두께 상수 `GRID_DIVIDER_WIDTH_DP`는 아래 피드백 ②에서 gap 자체를 없애며 삭제됨).
>
> 초기 구현 후 실기기 확인 결과 dim 처리만으로는 비활성 패드 구분이 눈에 잘 띄지 않는다는 피드백을 받아 시각 요소를 추가했다: 그리드 전체를 감싸는 외곽 테두리 1개(`GRID_OUTER_BORDER_WIDTH_DP = 2f`, 흰색 30% 불투명도)와, 비활성 패드마다 점선 테두리(`GRID_INACTIVE_BORDER_WIDTH_DP = 1.5f`, `GRID_DASH_ON_LENGTH_DP = 6f`, `GRID_DASH_OFF_LENGTH_DP = 4f`, 흰색 50% 불투명도)를 `MultiCursorConstants.kt`에 추가하고 `Page2MultiCursorTouchpad.kt`에서 `Modifier.drawBehind` + `PathEffect.dashPathEffect`로 그린다.
>
> 이어진 피드백 2건을 추가 반영했다. ① 비활성 패드의 점선이 그리드 외곽 실선과 겹쳐 보이는 문제 — 각 셀의 4변 중 그리드 바깥 경계와 맞닿는 변에는 점선을 그리지 않고, 다른 패드와 실제로 맞닿는 내부 경계에만 점선을 그리도록 수정(`rect.left/top/right/bottom`을 전체 폭/높이와 비교해 외곽 여부 판정 후 `drawLine` 4개를 조건부 호출). ② 패드 사이 여백 제거 — 각 셀에 있던 `padding(GRID_DIVIDER_WIDTH_DP.dp / 2)`를 제거해 인접 패드가 빈 공간 없이 완전히 맞닿도록 했고, 더 이상 쓰이지 않는 `GRID_DIVIDER_WIDTH_DP` 상수를 삭제했다.

**검증**:
- [x] 커서 2/3/4개 각각 설계 문서대로 영역 분할 표시
- [x] 비활성 패드 탭 → 해당 패드로 즉시 전환 (테두리 이동)
- [x] 활성 패드에서만 커서 이동 프레임 전송 (비활성 입력 무시 — 비활성 영역엔 `TouchpadWrapper` 자체가 마운트되지 않아 구조적으로 보장)
- [x] 패드 전환 시 제어 버튼 상태 갱신 (`effectiveState` projection이 `activePadIndex`를 그대로 따라가므로 별도 처리 불필요)
- [x] 싱글 복귀 시 단일 터치패드로 즉시 복원

---

## Phase 4.8.4: 직접 전환 버튼 레이아웃 모드 (PadSwitchButtonPanel)

> **⚠️ Phase 4.8.2 변경사항**: `MultiCursorState.layoutMode`(`MultiCursorLayoutMode.GRID`/`DIRECT_BUTTON`)가 이미 정의되어 있다. `MultiCursorController`에 레이아웃 모드 전환 메서드가 아직 없으므로 이 Phase에서 추가하거나 `state.copy(layoutMode = ...)`를 직접 다루는 메서드를 신설할 것.

> **⚠️ Phase 4.8.3 변경사항**:
> - 신규 순수 함수 `divideGridAreas`/`hitTestPad`(`MultiCursorGridGeometry.kt`, `ui/components/touchpad/`)와 단위 테스트(`MultiCursorGridGeometryTest.kt`)가 추가됨. DIRECT_BUTTON 모드는 영역 분할이 아니라 하단 버튼 패널이라 이 함수들을 그대로 재사용하지는 않지만, 같은 파일/패키지에 유사 패턴(순수 기하 함수 + 단위 테스트)으로 추가할 것.
> - `Page2MultiCursorTouchpad`에 `onPadSwitch: (Int) -> Unit` 파라미터가 추가되고 `StandardModePage`에서 `multiCursor.switchPad(index)`로 배선됨. DIRECT_BUTTON의 하단 버튼도 이 파라미터를 그대로 재사용해 탭 시 `onPadSwitch(index)` 호출.
> - `Page2MultiCursorTouchpad`의 렌더 분기가 `multiCursorState.isEnabled && layoutMode == GRID`로 그리드 렌더와 else(단일 `TouchpadWrapper`) 두 갈래로 나뉨. DIRECT_BUTTON은 이 `if/else` 구조에 세 번째 분기(`layoutMode == DIRECT_BUTTON`)로 추가해야 함 — else 분기를 "싱글 커서 전용"으로 오인해 그대로 재사용하면 안 됨.
> - 신규 상수 `MultiCursorConstants.kt`(`ui/common/`)에 `GRID_*` 상수 6개(`GRID_INACTIVE_PAD_DIM_ALPHA`, `GRID_PAD_SWITCH_ANIM_DURATION_MS`, `GRID_OUTER_BORDER_WIDTH_DP`, `GRID_INACTIVE_BORDER_WIDTH_DP`, `GRID_DASH_ON_LENGTH_DP`, `GRID_DASH_OFF_LENGTH_DP`) 정의됨. DIRECT_BUTTON 전용 상수(버튼 크기, 간격 등)는 이 파일에 `DIRECT_BUTTON_*` 접두사로 추가할 것 — 재정의 금지. 패드 간 gap 상수(`GRID_DIVIDER_WIDTH_DP`)는 시각 검토 후 제거되어 현재 셀들이 여백 없이 맞닿아 있다 — DIRECT_BUTTON은 분할 자체가 없어 해당 없음.

**목표**: 터치패드 전체 면적을 유지하면서 하단에 N개 전환 버튼을 표시한다. 탭 1번으로 어떤 패드든 즉시 이동한다.

**개발 기간**: 1일

**세부 목표**:
1. **전환 버튼 패널**: 터치패드 하단 오버레이로 N개 버튼(패드 1~N)을 배치. 활성 버튼 강조, 비활성 버튼 반투명. 버튼 크기·색상 등 값은 설계 문서 기준.
2. **입력 소비**: 버튼 패널은 터치패드 제스처와 충돌하지 않도록 이벤트를 소비한다.
3. **레이아웃 모드 전환**: `MultiCursorLayoutMode`를 GRID ↔ DIRECT_BUTTON으로 전환하는 UI(제어 버튼 또는 설정). 전환 시 레이아웃 즉시 반영.
4. **입력 영역**: 직접 전환 모드에서 버튼 패널을 제외한 전체 면적이 활성 패드의 입력 영역.

**참조 문서**:
- `docs/android/component-touchpad.md` §1.2.2 (직접 전환 버튼 — PadSwitchButtonPanel)
- `docs/android/component-touchpad.md` §1.2.3 (멀티 커서 선택 상태 관리)

**검증**:
- [ ] 직접 전환 모드에서 하단 N개 버튼 표시
- [ ] 버튼 탭 → 해당 패드로 즉시 전환
- [ ] 버튼 탭 시 터치패드 커서 이동 미발생 (이벤트 소비)
- [ ] 버튼 패널 제외 전체 면적이 활성 패드 입력 영역
- [ ] 그리드 분할 ↔ 직접 전환 모드 전환 시 레이아웃 즉시 반영

---

## Phase 4.8.5: 서버 연동 사전 준비 (전송 훅 + 서버 미연결 완결 동작)

> **⚠️ Phase 4.8.2 변경사항**: 활성화(`onCursorCountSelected`)/비활성화(`onCursorModeClick`의 disable 분기)가 `StandardModePage`에 이미 배선되어 있다. `show_virtual_cursor`/`hide_virtual_cursor` 전송 훅은 이 두 콜백 안에, `switch_cursor` 훅은 `MultiCursorController.switchPad` 호출 지점(4.8.3에서 연결됨)에 추가할 것. `MultiCursorState`에는 아직 서버 왕복용 필드(`padCursorPositions`/`isPendingAck` 등)가 없으므로 이 Phase에서 필요 시 확장.

**목표**: Windows 서버가 준비되기 전 단계로, 멀티 커서 활성/비활성/패드 전환 시 서버에 명령을 보낼 **전송 지점(콜백 훅)과 프로토콜 자리만** 마련한다. 서버가 없어도 앱 내부 상태만으로 멀티 커서가 완결적으로 동작해야 한다. 실제 서버 왕복과 PC 화면 가상 커서는 Phase 5에서 완성한다.

**개발 기간**: 1일

**세부 목표**:
1. **전송 훅 배치**: 활성화 시 `show_virtual_cursor`, 비활성화 시 `hide_virtual_cursor`, 패드 전환 시 `switch_cursor`에 해당하는 명령을 보낼 **콜백 지점**을 상태 변경 흐름에 연결(4.7.4-C: 전송 로직 자체는 상태 홀더가 아니라 Composable 콜백). 명령/페이로드 형식은 설계 문서 기준.
2. **서버 미연결(Essential) 완결 동작**: 서버 미연결 시 전송을 스킵하고 앱 내부 멀티 커서 상태만으로 모든 UI·전환이 크래시 없이 동작.
3. **패드 경계 홀드 리셋(의도된 설계 확인)**: 손가락이 패드 경계를 넘으면 터치업으로 홀드가 리셋되는 것은 의도된 설계다. 패드 간 드래그는 외부 보조 버튼(독립 HID)과 조합하며 BridgeOne 앱에 별도 구현은 불필요. 문서상 이 전제만 명확히 반영.

**이 Phase에서 하는 것 vs Phase 5에서 하는 것**:

| 항목 | Phase 4.8.5 (지금) | Phase 5 (서버) |
|------|------|------|
| 명령 전송 훅/프로토콜 자리 | 마련 | — |
| 서버 미연결 시 앱 내부 동작 | 완결 | — |
| `show_virtual_cursor` 완전 왕복(ACK로 커서 위치 수신) | 훅만 | 완성 |
| PC 화면 N-1개 가상 커서 렌더 | — | 완성 |
| 실제 커서 텔레포트 / 커서 팩 감지·동기화 | — | 완성 |

**참조 문서**:
- `docs/android/technical-specification-app.md` §2.2.6 (멀티 커서 활성화/비활성화 플로우)
- `docs/technical-specification.md` §4.4.3 (멀티 커서 시스템 전체 플로우)
- `docs/windows/technical-specification-server.md` §3.6 (Windows 서버 N-1개 가상 커서 — Phase 5)

**검증**:
- [ ] 활성/비활성/패드 전환 시 전송 콜백 훅 호출 확인 (로그 또는 no-op)
- [ ] 서버 미연결 상태에서 멀티 커서 UI·전환 완결 동작, 크래시 없음
- [ ] 명령/페이로드 형식이 설계 문서와 일치

---

## Phase 4.8 완료 후 Page 2 구조

```
Page 2 — 풀 와이드 터치패드 (멀티 커서)
├── 터치패드 영역 (100% 너비 × 100% 높이)
│   ├── [싱글 커서] 전체 면적 단일 터치패드
│   └── [멀티 커서 — 그리드 분할] 활성 패드 테두리 + 비활성 dim
│   └── [멀티 커서 — 직접 전환 버튼] 전체 면적 입력 + 하단 전환 버튼 패널
├── ControlButtonContainer (CursorModeButton 포함, 상단 오버레이)
├── 커서 수 선택 팝업 (CursorModeButton 위, 싱글→멀티 전환 시)
└── 기존 가이드라인 등 오버레이
```

| 기능 | 싱글 커서 | 멀티 커서 |
|------|---------|---------|
| CursorModeButton | 멀티로 전환 (팝업) | 싱글로 복귀 |
| 터치패드 레이아웃 | 전체 면적 | 그리드 분할 또는 직접 전환 버튼 |
| ControlButtonContainer | 전체 표시 | 활성 패드 모드 상태 반영 |
| Windows 가상 커서 | 없음 | 전송 훅만 (실제 표시는 Phase 5) |
| 패드 경계 홀드 리셋 | 해당 없음 | 외부 보조 버튼 조합으로 드래그 |

> **소리 감지 패드 전환**: 마이크 입력으로 패드를 전환하는 기능은 멀티 커서 전용을 넘어 여러 앱 요소를 소리로 제어하는 일반 기능으로 확장하여 **Phase 7(추가 기능 개발)**에서 별도 계획한다. 이 Phase 범위에서 제외.
