---
title: "BridgeOne Phase 4.8: Page 2 — 풀 와이드 터치패드 (멀티 커서 사전 준비)"
description: "BridgeOne 프로젝트 Phase 4.8 - Standard 모드 Page 2를 멀티 커서 전용 페이지로 정식화. Windows 서버(Phase 5) 준비 전, 앱 단독으로 완결 동작하는 멀티 커서 UI·상태·레이아웃 골격 구축"
tags: ["android", "multi-cursor", "touchpad", "full-width", "cursor-mode", "ui"]
version: "v2.1"
owner: "Chatterbones"
updated: "2026-07-03"
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

> **계획과 다르게 구현된 부분**:
> - **레이아웃 모드 전환 UI**: 설계 문서에 진입점이 명시돼 있지 않았다("설정에서 구성"으로만 서술). `CursorModeButton` 롱프레스로 그리드 분할 ↔ 직접 전환 버튼을 토글하는 방식으로 확정했다(별도 버튼 추가 없이 기존 버튼 재사용, ModePresetButton 롱프레스 패턴과 일관). `ControlButtonContainer`에 `onCursorModeLongPress: (() -> Unit)? = null` 파라미터를 추가하고 `ControlButton`의 기존 `onLongClick` 파라미터에 연결했다(`ControlButton` 자체는 이미 롱프레스를 지원해 수정 불필요). `MultiCursorController.toggleLayoutMode()` 신규 추가. 설계 문서(`component-touchpad.md` §1.2.3, `technical-specification-app.md` §2.2.6)에 진입점을 반영 완료.
> - **PadSwitchButtonPanel 스펙 문서 충돌 해소**: `component-touchpad.md`(높이 48dp, 활성 보라색 `#B552F6`)와 `styleframe-page2.md`(높이 40dp, 활성 파란색 `#2196F3`)가 상충했다. **component-touchpad.md 기준(48dp)**으로 구현하되, 사용자 피드백에 따라 활성 색상은 파란색(`#2196F3`, `TouchpadColorBlue`)으로 확정했다(비활성 alpha 0.4는 styleframe 값을 그대로 채용). `MultiCursorConstants.kt`에 `DIRECT_BUTTON_PANEL_HEIGHT_DP = 48f`, `DIRECT_BUTTON_INACTIVE_ALPHA = 0.4f` 추가.
> - **버튼 패널 모서리 둥글기**: 사용자 피드백("터치패드 모서리가 둥그니까 버튼도 둥글게") 반영 — 처음엔 버튼별 개별 `RoundedCornerShape` + 버튼 간 간격으로 구현했으나, 후속 피드백("버튼 하나하나가 아니라 버튼 묶음의 외곽만 둥글게")에 따라 패널 전체(`Row`)에 `clip(RoundedCornerShape(DIRECT_BUTTON_CORNER_RADIUS_DP.dp = 8f))`를 적용하는 방식으로 수정했다. 개별 버튼은 간격 없이 붙어 있고 각진 채로 유지되며, 패널 외곽 4모서리만 둥글다.
> - **순수 기하 함수 미추출**: Phase 4.8.3 노트가 "순수 기하 함수 + 단위 테스트" 패턴을 권장했으나, DIRECT_BUTTON 모드는 좌표→인덱스 매핑이 필요 없다(Row + weight + clickable로 프레임워크가 히트 판정을 담당). 그리드 분할 특유의 요구사항이었으므로 이번 모드엔 해당 패턴을 적용하지 않았다 — `MultiCursorGridGeometry.kt`에 신규 함수 추가 없음.
> - **신규 컴포넌트**: `PadSwitchButtonPanel.kt`(`ui/components/touchpad/`) 신규. `Row` + `weight(1f)` N등분(버튼 사이 간격 없음), 활성 패드 `TouchpadColorBlue` 배경, 비활성 패드 동일 색 `alpha=0.4f`, 패널 외곽 8dp 둥근 모서리(`clip`을 패널 `Row`에 적용, 개별 버튼엔 미적용), 탭 시 `onPadSwitch(index)` + 햅틱(`KEYBOARD_TAP`).
> - **레이아웃 모드 전환 시 토스트 안내**: 사용자 피드백에 따라 `CursorModeButton` 롱프레스로 레이아웃 모드를 바꿀 때 `ToastController.show("그리드 분할 모드로 전환"/"직접 전환 버튼 모드로 전환", ToastType.INFO)`를 `StandardModePage.kt`의 `onCursorModeLongPress` 콜백에 추가했다.
> - **`Page2MultiCursorTouchpad.kt` 렌더 분기 3분기화**: 기존 `if(showGrid) else` 2분기를 `if(showGrid) else if(showDirectButton) else` 3분기로 확장. DIRECT_BUTTON 분기는 `TouchpadWrapper`에 `Modifier.padding(bottom = 48dp)`로 하단 패널 공간을 비우고, `PadSwitchButtonPanel`을 `Alignment.BottomCenter`로 겹쳐 배치했다. `effectiveState`/`effectiveOnStateChange` projection과 `onPadSwitch` 파라미터는 그리드 분기와 동일하게 재사용 — 변경 없음.

**검증**:
- [x] 직접 전환 모드에서 하단 N개 버튼 표시
- [x] 버튼 탭 → 해당 패드로 즉시 전환
- [x] 버튼 탭 시 터치패드 커서 이동 미발생 (이벤트 소비 — `PadSwitchButtonPanel`이 자체 `clickable`로 소비, 하단 영역엔 `TouchpadWrapper`가 마운트되지 않아 구조적으로 보장)
- [x] 버튼 패널 제외 전체 면적이 활성 패드 입력 영역 (`padding(bottom = 48dp)`로 `TouchpadWrapper` 영역이 패널을 침범하지 않음)
- [x] 그리드 분할 ↔ 직접 전환 모드 전환 시 레이아웃 즉시 반영 (`CursorModeButton` 롱프레스 → `toggleLayoutMode()` → `when` 분기 즉시 재평가)

---

## Phase 4.8.5: 서버 연동 사전 준비 (전송 훅 + 서버 미연결 완결 동작)

> **⚠️ Phase 4.8.2 변경사항**: 활성화(`onCursorCountSelected`)/비활성화(`onCursorModeClick`의 disable 분기)가 `StandardModePage`에 이미 배선되어 있다. `show_virtual_cursor`/`hide_virtual_cursor` 전송 훅은 이 두 콜백 안에, `multi_cursor_switch` 훅은 `MultiCursorController.switchPad` 호출 지점(4.8.3에서 연결됨)에 추가할 것. `MultiCursorState`에는 아직 서버 왕복용 필드(`padCursorPositions`/`isPendingAck` 등)가 없으므로 이 Phase에서 필요 시 확장.

> **⚠️ Phase 4.8.4 변경사항**:
> - `MultiCursorController`에 `toggleLayoutMode()` 신규 — `CursorModeButton` 롱프레스로 GRID↔DIRECT_BUTTON 토글. `switch_cursor` 전송 훅과 별개로, 레이아웃 모드 전환 자체는 서버 전송 대상이 아니다(로컬 UI 상태). 다만 `switch_cursor` 훅은 DIRECT_BUTTON 모드의 `PadSwitchButtonPanel.onPadSwitch`에서도 동일하게 호출되어야 한다 — `onPadSwitch`가 그리드/직접 전환 두 모드에서 동일 콜백(`multiCursor.switchPad(index)`)을 공유하므로 훅 배치 지점은 하나로 충분하다.
> - `PadSwitchButtonPanel.kt`(`ui/components/touchpad/`), `DIRECT_BUTTON_*` 상수(`MultiCursorConstants.kt`) 신규 — 전송 훅과 직접 관련은 없으나 참고.

> **계획과 다르게 구현된 부분**:
> - **패드 전환 명령 규격 확정**: 계획서 초안은 `switch_cursor` + `pad_index`로 서술했으나, 기술명세 4종(`technical-specification-app.md` §2.2.6.3, `technical-specification.md` §4.4.3, `technical-specification-server.md` §3.6, `esp32s3-code-implementation-guide.md` §4.1.1)과 Windows 서버 구현 명세가 모두 `multi_cursor_switch` + `touchpad_id`("pad1" 등) + `cursor_position{x,y}` 규격으로 일치되어 있어, 이 규격으로 통일했다(`switch_cursor` 문구는 계획서·`styleframe-page2.md` 2곳뿐이라 이쪽을 수정). `cursor_position`은 Android가 PC 화면 크기를 몰라 값을 채울 수 없으므로 이번엔 필드 자리만(`null`) 두고 Phase 5(`show_virtual_cursor` ACK로 초기 위치 수신)에서 채운다.
> - **전송 훅 구현 수준 = JSON 빌더 + 로그**: 실제 UART write는 하지 않는다(ESP32 0xFF 중계 왕복은 에뮬레이터로 검증 불가, Phase 5에서 실기기 검증). 신규 `protocol/MultiCursorCommand.kt`에 페이로드를 구성하는 순수 함수 3개(`buildShowVirtualCursor`/`buildHideVirtualCursor`/`buildMultiCursorSwitch`)를 두고, `StandardModePage.kt`의 `sendMultiCursorCommand` 콜백이 `UsbSerialManager.bridgeMode`로 서버 연결 여부를 확인해 `BridgeMode.STANDARD`면 `Log.d`, `ESSENTIAL`이면 스킵 로그만 남긴다.
> - **timestamp = epoch millis 문자열**: `Instant.now()`(API 26+)는 minSdk 24에 desugaring 미적용이라 사용 불가. `System.currentTimeMillis().toString()`으로 대체(로그 수준이므로 ISO8601 엄밀성보다 호환성 우선). Phase 5에서 실제 전송 포맷을 정할 때 재검토 필요.
> - **훅 배치 지점**: `onCursorCountSelected`(`multiCursor.enable` 직후), `onCursorModeClick`의 disable 분기(`multiCursor.disable` 직후), `onPadSwitch`(`multiCursor.switchPad` 직후) 3곳. `onCursorModeLongPress`(레이아웃 모드 토글)는 로컬 UI 상태라 서버 전송 대상에서 제외.

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
- [x] 활성/비활성/패드 전환 시 전송 콜백 훅 호출 확인 (로그 또는 no-op) — 에뮬레이터 실기 테스트로 Logcat 확인 완료(`StandardModePage` 태그)
- [x] 서버 미연결 상태에서 멀티 커서 UI·전환 완결 동작, 크래시 없음 — 에뮬레이터에서 활성화(2)→패드 전환→비활성화 흐름 후 앱 프로세스 생존, `AndroidRuntime:E` 크래시 로그 없음 확인
- [x] 명령/페이로드 형식이 설계 문서와 일치 — 로그 페이로드가 `command`/`cursor_count`/`touchpad_id`/`cursor_position`/`timestamp` 필드로 출력됨을 확인

---

## Phase 4.8.6: UX/UI 보강 (패드 전환 애니메이션 + 다듬기)

> **⚠️ Phase 4.8.3/4.8.4 변경사항**: 그리드 분할 모드의 dim `animateFloatAsState`(`Page2MultiCursorTouchpad.kt`)는 패드 전환 시 셀이 `TouchpadWrapper` 분기 ↔ dim Box 분기로 재마운트되어 tween이 실제로 재생되지 않는 죽은 애니메이션이었다. 이 Phase에서 dim 오버레이를 전 셀 상시 컴포지션 레이어로 분리하며 함께 수복했다.

**목표**: 골격이 완성된 멀티 커서 UI에 패드 전환 애니메이션 2종과 출시 수준 다듬기를 적용한다.

**세부 목표**:
1. **그리드 분할 모드 — 하이라이트 슬라이드**: `Page2MultiCursorTouchpad.kt`의 GRID 분기를 3레이어로 재편(셀 본체 / dim 오버레이+번호 라벨 / 슬라이드 하이라이트). 활성 패드 강조가 이전 셀에서 새 셀로 `Animatable<Float>` 4개(left/top/right/bottom)로 250ms 슬라이드 이동 후, 도착 펄스(0.1초, 80% 불투명도 → 소멸)로 마무리한다. 도착 펄스는 `component-touchpad.md` §3.2.4.1의 미구현 "파란색 펄스" 스펙을 흡수 구현한 것이다. 하이라이트 레이어는 포인터 modifier가 없는 최상단 `drawBehind` Box라 히트 테스트에 관여하지 않으며, `TouchpadWrapper`의 마운트/입력 경로는 변경하지 않았다.
2. **직접 전환 버튼 모드 — 터치패드 본체 슬라이드**: `AnimatedContent`는 `TouchpadWrapper`(수십 개 `remember` 상태 보유)를 재마운트시켜 관성 스크롤·엣지 팝업 상태가 끊기므로 채택하지 않았다. 대신 단일 `TouchpadWrapper` 인스턴스를 유지한 채 `graphicsLayer { translationX }`로만 이동시키고, 전환 중에는 이전 패드의 배경+테두리만 재현하는 경량 "유령 패널"을 반대 방향에서 함께 슬라이드시켜 페이저 감각을 낸다. `PadSwitchButtonPanel`의 활성 하이라이트도 `animateDpAsState`로 슬라이드하도록 별도 레이어로 재구성했다.
3. **활성 중 커서 수 변경**: `CursorModeButton` 탭이 멀티 활성 중에도 즉시 해제하지 않고 `CursorCountSelectionPopup`을 띄우도록 변경(`StandardModePage.kt`). 팝업은 `currentCount`/`onDisable` 파라미터를 새로 받아 현재 수를 강조 표시하고 우측에 "해제" 버튼을 추가한다. `MultiCursorController.changeCursorCount(count)` 신규 — 기존 `padModeStates`를 보존하며 절단/확장(신규 패드는 pad1 상태로 시드), `activePadIndex`를 새 범위로 clamp한다. 수 변경 시에도 `show_virtual_cursor`(새 cursor_count) 훅을 재전송한다.
4. **다듬기**: 그리드 비활성 셀 탭 시 햅틱 피드백 추가(`PadSwitchButtonPanel`과 일관), 그리드 비활성 셀에 패드 번호 라벨 표시, 직접 전환 버튼 패널과 터치패드 사이 간격(`DIRECT_BUTTON_PANEL_TOP_GAP_DP = 8f`) 추가, `CursorCountSelectionPopup` 퇴장 애니메이션(120ms, 등장의 역재생) 추가.

> **계획과 다르게 구현된 부분 (사용자 스크린샷 확인 후 수정)**:
> - **커서 수 뱃지 제거**: 세부 목표 3에서 계획했던 `CursorModeButton` 뱃지(`ControlButtonContainer`의 `multiCursorCursorCount` 파라미터)는 사용자 피드백으로 제거했다. `ControlButtonContainer`/`Page2MultiCursorTouchpad`에서 관련 파라미터·UI를 모두 되돌렸다.
> - **`CursorCountSelectionPopup` 겹침 버그**: 팝업 카드가 `Alignment.TopCenter + padding(top = 56.dp)` 고정 오프셋을 쓰고 있었는데, `ControlButtonContainer`의 실제 높이(`controlHeight = (containerHeight * 0.15f).coerceIn(48.dp, 72.dp)`)가 화면에 따라 72dp까지 커져 56dp보다 큰 경우 팝업이 제어 버튼 아이콘을 가렸다. `Page2MultiCursorTouchpad.kt`에서 `ControlButtonContainer`에 `Modifier.onGloballyPositioned`를 붙여 실제 렌더 높이를 측정해 `anchorTopDp`로 팝업에 전달하도록 수정했다(하드코딩 오프셋 제거, 파일 로컬 여백 상수 `ANCHOR_TOP_GAP_DP = 8f`는 `CursorCountSelectionPopup.kt`에 정의).
> - **선택 강조 스타일 통일**: 활성 중 재호출 시 현재 커서 수 버튼의 강조가 기존 `TouchpadColorBlue` 100% 불투명 단색 채움이었는데, `ModePresetPopup`의 "연한 배경 틴트 + 테두리" 컨벤션(현재 적용 항목은 45% alpha 틴트)과 스타일이 달라 유독 진하고 무거웠다. `TouchpadColorPurple.copy(alpha = 0.45f)` 배경 + `TouchpadColorPurple` 2dp 테두리로 변경했다 — 보라색은 `CursorModeButton`이 멀티 진입 시 이미 쓰는 대표색이라(`ControlButtonContainer.kt`) 트리거 버튼과 팝업의 색 언어가 일치한다. "해제" 버튼은 선택 상태가 아닌 독립 액션이라 기존 solid red 스타일을 유지했다.
> - **선택/해제 직후 상태 플래시 버그**: 커서 수를 선택하거나 "해제"를 탭하면 부모(`multiCursorState`)가 즉시 바뀌는데, `CursorCountSelectionPopup`은 퇴장 애니메이션(120ms) 동안에도 `isActive = true`라 계속 리컴포지션되어 `currentCount`/`onDisable` 같은 살아있는 파라미터를 그대로 읽고 있었다. 그 결과 팝업이 사라지는 도중 "방금 바뀐 새 상태"(예: 방금 활성화된 커서 수+해제 버튼, 또는 방금 해제된 직후의 평범한 선택 화면)가 한 프레임 정도 비쳐 보이는 플래시가 발생했다. `CursorCountSelectionPopup.kt`에서 팝업이 열리는 순간의 `currentCount`/`onDisable`을 `remember` 상태로 스냅샷해 `frozenCurrentCount`/`frozenOnDisable`로 렌더링에 사용하도록 수정 — 퇴장 중에는 항상 "열렸을 때의 모습" 그대로 페이드아웃한다.
> - **패드 전환 애니메이션 속도 2배 (사용자 피드백)**: `GRID_PAD_SWITCH_ANIM_DURATION_MS`(200→100), `GRID_HIGHLIGHT_SLIDE_DURATION_MS`(250→125), `GRID_HIGHLIGHT_FADE_OUT_MS`(100→50), `DIRECT_SLIDE_DURATION_MS`(250→125), `DIRECT_BUTTON_HIGHLIGHT_SLIDE_MS`(200→100)를 전부 절반으로 줄여 패드 전환이 더 빠르게 느껴지도록 조정. 이후 사용자 확인을 거쳐 이 값들을 신규 기본값으로 확정(`MultiCursorConstants.kt`의 `⚠️ 의도적 변경` 표기 제거).

**신규 상수** (`MultiCursorConstants.kt`): `GRID_HIGHLIGHT_SLIDE_DURATION_MS`(250), `GRID_HIGHLIGHT_STROKE_WIDTH_DP`(3f), `GRID_HIGHLIGHT_CORNER_RADIUS_DP`(12f), `GRID_HIGHLIGHT_ARRIVAL_PULSE_ALPHA`(0.8f), `GRID_HIGHLIGHT_FADE_OUT_MS`(100), `GRID_CELL_LABEL_ALPHA`(0.35f), `DIRECT_SLIDE_DURATION_MS`(250), `DIRECT_BUTTON_HIGHLIGHT_SLIDE_MS`(200), `DIRECT_BUTTON_PANEL_TOP_GAP_DP`(8f).

**참조 문서**:
- `docs/android/component-touchpad.md` §1.7.2 (활성 중 모드), §3.2.4.1 (전환 효과)

**검증**:
- [x] `./gradlew assembleDebug` 빌드 성공
- [x] 그리드 2/3/4에서 패드 전환 시 하이라이트 슬라이드+도착 펄스, dim fade, 번호 라벨, 탭 햅틱 동작 확인 (실기 확인 완료)
- [x] 직접 전환 모드에서 본체 슬라이드 방향(인덱스 증가 시 왼쪽으로 밀림) + 패널 하이라이트 슬라이드 확인 (실기 확인 완료)
- [x] 관성 스크롤 중 직접 전환 모드 패드 전환 시 관성이 끊기지 않는지 확인 (단일 인스턴스 보존 검증, 실기 확인 완료)
- [x] 멀티 활성 중 `CursorModeButton` 탭 → 팝업(현재 수 강조+해제 버튼), 수 변경 시 패드 상태 보존, 해제 시 싱글 복귀 확인 (실기 확인 완료, 위치 겹침·상태 플래시 버그 수정 후 재확인)

---

## Phase 4.8.7: 엣지 존 멀티 커서 액션군

**목표**: `EdgeZoneAction`에 멀티 커서 제어 액션을 추가해, 4개 엣지 어느 존에서든 멀티 커서를 조작할 수 있게 한다. `EdgeZoneAction`은 이미 제어 버튼 액션의 상위집합이지만 멀티 커서 제어만 빠져 있고, `MultiCursorController`(`enable`/`disable`/`switchPad`/`changeCursorCount`/`toggleLayoutMode`)는 이미 필요한 메서드를 전부 갖추고 있다 — 신규 로직 없이 배선만으로 구현 가능한 확장 지점이다.

**개발 기간**: 1일

**세부 목표**:
1. **신규 액션 정의** (`EdgeZone.kt` sealed class):
   - `ToggleMultiCursor` — 활성/비활성 토글(마지막 커서 수 기억, 없으면 기본 2)
   - `ActivatePad(index: Int)` — 특정 패드 즉시 활성화
   - `CyclePad(direction: PageNav)` — 다음/이전 패드 순환 (`PageNav` enum 재사용)
   - `SetCursorCount(count: Int)` — 커서 수 직접 지정(2/3/4). 비활성 시 활성화, 활성 시 수 변경
   - `ToggleMultiCursorLayout` — 그리드 ↔ 직접 전환 버튼 토글
2. **컴파일러 강제 확장 지점 채우기**: `categoryColor()`/`displayName()`/`defaultIconKey()`(`EdgeZone.kt`), `applyZoneAction`(`EdgeZoneActionHandler.kt` — 부수효과형이라 `state` 그대로 반환하고 콜백 위임), `domainOf`/`actionEquals`(`EdgeZoneActionResolver.kt` — `ActionDomain`에 `MULTI_CURSOR` 신설), 직렬화/역직렬화(`EdgeZoneJson.kt`).
3. **콜백 배선**: `TouchpadWrapper.kt`에 콜백 파라미터 추가(`rememberUpdatedState`) + Release 실행부 `when` 분기(기존 `onCyclePage`/`onJumpToPage` 패턴). `Page1TouchpadActions.kt`/`Page2MultiCursorTouchpad.kt`를 거쳐 `StandardModePage.kt`에서 `multiCursor` 컨트롤러를 직접 참조해 구현.
4. **편집기 등록**: `ZoneActionPicker.kt`의 `ActionDomainPicker`에 `MULTI_CURSOR` 도메인 카드 추가, `DEFAULT_DOMAIN_GROUPS`에 배치.
5. **정리**: `ToggleMode(CURSOR)`가 현재 CLICK 도메인으로 잘못 매핑돼 무동작 상태 — 이 액션군 도입과 함께 제거하거나 올바르게 재매핑.

**참조 문서**:
- `docs/android/component-touchpad.md` §1.2.3 (멀티 커서 선택 상태 관리)
- `docs/android/technical-specification-app.md` §2.2.6 (멀티 커서 알고리즘 명세)

**검증** (에뮬레이터(Pixel_6a) 실기기 조작으로 확인. 항목별 확인 방법 명시):
- [x] 5개 신규 액션 각각을 엣지 존에 할당 가능(편집기 피커에 노출) — 존 편집기 → 모드·탐색 → 멀티 커서 폴더에서 토글/레이아웃전환/커서2·3·4개/다음·이전 패드/패드1~4 카드 전부 노출 확인
- [x] armed 상태에서 손 뗄 때 해당 멀티 커서 동작 실행 — Page2에 `CyclePad(NEXT)`를 좌측 존에 할당 후 안쪽으로 밀었다 떼자 활성 패드가 실제로 전환됨(그리드 활성 테두리 이동) 확인
- [x] 존 할당 JSON 저장/복원 시 신규 액션 필드 보존 — `touchpad_edge_zone_assignments.json`에 `{"type":"CyclePad","dir":"NEXT"}` 저장 확인 + 앱 강제종료 후 재시작해도 존 아이콘/동작 유지 확인
- [x] 서버 미연결 상태에서도 크래시 없이 앱 내부 완결 동작 — ESSENTIAL 모드에서 5종 액션 실행 시 logcat에 `multiCursorCommand skipped (ESSENTIAL): ...` 로그만 남고 크래시(FATAL/AndroidRuntime) 없음 확인
- [x] `ToggleMode(CURSOR)` 무동작 상태 정리 완료 — **실기기 재검증 완료**. 최초 시도에서 `adb shell input swipe`로 팝업이 안 열렸던 원인은 `TouchpadWrapper.kt`의 `if (bridgeMode == BridgeMode.ESSENTIAL) { null }`(엣지 스와이프 감지 자체가 ESSENTIAL 모드에서 비활성화됨) — Page1은 기본 ESSENTIAL이라 막혔고, Page2는 `bridgeMode = BridgeMode.STANDARD`가 하드코딩돼 있어 열림. Page2에서 왼쪽 가장자리를 밀어 팝업 모드 선택기(직접 터치/스와이프) → 스와이프 그리드까지 정상 진입 확인, 그리드의 "커서" 카드를 스와이프로 선택 후 릴리즈하자 `latestOnMultiCursorAction(ToggleMultiCursor)`가 실제로 멀티 커서를 활성화(그리드 분할 렌더링)함을 확인

> **⚠️ 실제 구현이 계획과 다른 부분**:
> - **콜백 배선을 5종 개별이 아닌 단일 `onMultiCursorAction: (EdgeZoneAction) -> Unit`으로 통합**했다(`TouchpadWrapper.kt`/`Page2MultiCursorTouchpad.kt`/`StandardModePage.kt` 공통). 3계층 파이프라인에서 5개 콜백을 각각 뚫으면 시그니처가 폭증하고, 5종 모두 단일 `MultiCursorController`가 대상이라 응집도 상 하나로 묶는 게 자연스럽다. **후속 Phase에서 멀티 커서 관련 신규 액션을 추가할 때도 이 단일 콜백에 `when` 분기만 추가하면 된다** — TouchpadWrapper/Page2/StandardModePage 시그니처를 다시 늘릴 필요 없음.
> - **세부 목표 5 "정리"는 "제거/재매핑"이 아니라 "실동작화"로 처리했다**(사용자 결정). `EdgeSwipeMode.CURSOR`/`ToggleMode(CURSOR)` enum과 `domainOf`의 CURSOR→CLICK 매핑은 그대로 유지(하위 호환, `EdgeZoneActionResolverTest.kt` 98~99행 테스트 무수정). 엣지 스와이프 팝업의 "커서" 항목이 실제로 멀티 커서를 켜고 끄도록 배선했다 — 구체적인 탭/개수 선택/확정 방식은 **Phase 4.8.11에서 pending 방식으로 전면 재설계**됐으니 그쪽을 최신 기준으로 볼 것. **멀티 커서 진입 경로는 3개**: ① 상단 `CursorModeButton`(커서 수 선택 팝업), ② 엣지 존 안쪽 밀기(5종 신규 액션), ③ 엣지 스와이프 팝업의 "커서" 카드(Phase 4.8.11 참조).
> - **`Page1TouchpadActions.kt`는 수정하지 않았다.** Page1은 싱글 커서 전용이라 `onMultiCursorAction`을 배선할 대상이 없음(파라미터 기본값 `{}` no-op으로 TouchpadWrapper 호출부가 그대로 동작). 후속 Phase에서 Page1에도 멀티 커서 액션을 노출하려면 이 콜백을 새로 뚫어야 함. **참고**: Page1은 `bridgeMode`가 기본 ESSENTIAL이라 실제 USB 연결 없이는 엣지 스와이프 팝업 자체가 열리지 않음 — Page1에 멀티 커서 스와이프를 노출하려면 이 제약도 함께 고려해야 함.
> - **`disable()` 시 `cursorCount`가 0으로 리셋**되는 기존 `MultiCursorController` 동작 때문에, `ToggleMultiCursor` 재활성화용 `lastMultiCursorCount` 상태를 `StandardModePage.kt`에 신설(`multiCursor` 컨트롤러 옆). Phase 4.8.8(패드별 프리셋 시드)에서 활성화 로직을 건드릴 경우 이 상태와 `enableMultiCursor`/`disableMultiCursor`/`setMultiCursorCount`/`switchToPad` 추출 람다(`StandardModePage.kt`, `multiCursor` 선언부 바로 아래)를 재사용할 것.
> - ~~스와이프 팝업의 "커서" 카드에서 멀티 커서 수(2/3/4)를 세로 추가 스와이프로 순환 선택하는 기능(`cursorLocked` 등 커서 카드 잠금 상태 기반)을 구현했다~~ **→ Phase 4.8.11에서 pending 방식으로 완전히 대체되며 이 세로 스와이프 순환 잠금 메커니즘은 제거됐다.** 이 항목은 히스토리 보존 목적으로만 남긴다.

---

## Phase 4.8.8: 패드별 프리셋 시드

**목표**: 멀티 커서 활성화 시 각 패드에 서로 다른 모드 프리셋을 자동 배정한다(예: pad1=Precise, pad2=Fast). 현재 `enable(count, seed)`는 모든 패드를 단일 `seed`로 동일하게 시드한다.

**개발 기간**: 0.5일

**세부 목표**:
1. **패드별 시드 경로**: `MultiCursorController`에 패드별 `PadModeState` 리스트를 받는 오버로드 추가, 또는 활성화 직후 `updateActivePadMode`를 패드마다 반복 적용.
2. **프리셋 소스 재사용**: 신규 프리셋 정의 없이 기존 `MODE_PRESETS`(`ModePresetConstants.kt`)의 `padModeState` 필드를 그대로 시드값으로 사용.
3. **시드 매핑 설정 UI**: 커서 수 선택 팝업(`CursorCountSelectionPopup`) 확장 또는 별도 배정 UI로 패드마다 프리셋 지정. 구체 UI는 구현 시점에 결정.
4. **수 변경 시 규칙 유지**: `changeCursorCount()`로 패드 수가 늘어날 때 신규 패드의 시드 규칙(예: 마지막 지정값 반복 또는 pad1 시드) 확정.

**참조 문서**:
- `docs/android/component-touchpad.md` §1.7.2 (커서 수 선택 팝업)
- `docs/android/technical-specification-app.md` §2.2.6 (멀티 커서 알고리즘 명세)

**검증**:
- [ ] 멀티 커서 활성화 시 각 패드가 지정 프리셋 모드로 시작
- [ ] 패드 전환 시 제어 버튼이 해당 패드의 프리셋 모드 상태 반영
- [ ] `changeCursorCount` 증가 시 신규 패드에 시드 규칙 적용
- [ ] 프리셋 미지정 패드는 기존 `seed` 기본 동작 유지(하위 호환)

---

## Phase 4.8.9: 패드별 엣지 존 할당 (그리드 포함 전체 패드)

**목표**: 멀티 커서 각 패드가 독립적인 엣지 존 액션 세트를 갖는다. 그리드 분할/직접 전환 버튼 두 레이아웃 모드 모두 패드별로 지원한다.

**개발 기간**: 1.5일

**현재 구조**: `Page5Settings`에 이미 "터치패드" 페이지 셀렉터(`SegmentedChipSelector`, `selectedZonePage`)가 있어 페이지 단위로 엣지 존 할당을 분리 편집한다. 각 페이지는 단일 `touchpadId`(`TouchpadIds.standardPage(n)`)에 대응하는 `TouchpadEdgeZoneAssignment`를 별도 저장한다.

**세부 목표**:
1. **데이터 계층**: Page 2 각 패드에 별도 `touchpadId` 발급(예: `standard_page2_pad1`~`pad4`). `TouchpadIds`에 패드 변형 헬퍼 추가. `standardAssignments` 저장 키를 (페이지, 패드) 복합 키로 확장. `TouchpadEdgeZoneAssignmentRepository`(JSON 파일 영속) 키 스킴 조정.
2. **설정 UI**: 페이지 셀렉터에서 "페이지 2" 선택 시 패드 서브 셀렉터(패드 1~N) 노출하는 2단 셀렉터로 재구성. 기존 `SegmentedChipSelector` 컴포넌트 재사용.
3. **런타임 배선**: `Page2MultiCursorTouchpad`가 현재 `edgeZoneAssignment` 파라미터 하나만 받는 구조 → 그리드 각 셀 `TouchpadWrapper`에 패드 인덱스별 assignment를 전달하도록 변경. 직접 전환 버튼 모드는 활성 패드의 assignment만 전달.
4. **UX 전제**: 그리드 4분할 시 셀 면적이 작아 4방향 엣지 존 조작 실효성이 제한될 수 있음을 인지하되, 그리드 모드도 패드별 할당을 동일하게 지원한다.

**참조 문서**:
- `docs/android/component-touchpad.md` §1.2 (터치패드 영역 구조)
- `docs/android/technical-specification-app.md` §2.2.6 (멀티 커서 알고리즘 명세)

**검증**:
- [ ] 설정 화면에서 페이지→패드 2단 선택으로 각 패드의 엣지 존을 독립 편집
- [ ] 그리드 분할 모드에서 각 셀이 해당 패드 전용 존 액션 실행
- [ ] 직접 전환 버튼 모드에서 활성 패드 전환 시 존 액션도 해당 패드 것으로 즉시 전환
- [ ] 패드별 존 할당 JSON 저장/복원 정상 동작
- [ ] 싱글 커서 복귀 시 기존 페이지 단위 존 할당으로 정상 복원

---

## Phase 4.8.10: 패드 커스텀 라벨 (롱프레스 편집)

**목표**: 패드 번호(1/2/3/4) 대신 사용자 지정 이름을 표시한다. 편집은 패드 롱프레스로 진입한다.

**개발 기간**: 0.5일

**세부 목표**:
1. **편집 진입**: 그리드 셀 또는 `PadSwitchButtonPanel` 전환 버튼 롱프레스(`combinedClickable`의 `onLongClick`) → 이름 편집 팝업(신규 소형 컴포넌트).
2. **상태/영속**: `MultiCursorController`는 순수 상태 홀더이므로 라벨은 별도 영속 계층에 둔다 — `MultiCursorState`에 `padLabels: List<String>` 추가 후 SharedPreferences repository 배선(`InputMode.kt`의 load/save 패턴 선례 재사용).
3. **렌더**: 라벨이 지정되면 이름 표시, 없으면 기존 번호로 fallback. 그리드 dim 오버레이와 `PadSwitchButtonPanel` 양쪽에 반영.
4. **피드백**: 롱프레스 햅틱은 기존 `HapticFeedbackConstants.LONG_PRESS` 패턴 재사용, 저장 완료 시 `ToastController.show(...)`.

**참조 문서**:
- `docs/android/component-touchpad.md` §1.2.2 (직접 전환 버튼 — PadSwitchButtonPanel)

**검증**:
- [ ] 그리드 셀 롱프레스 → 이름 편집 팝업 표시
- [ ] 전환 버튼 롱프레스 → 이름 편집 팝업 표시
- [ ] 지정한 이름이 그리드 dim 오버레이와 전환 버튼 양쪽에 표시
- [ ] 앱 재시작 후에도 라벨 유지(영속 확인)
- [ ] 빈 이름으로 저장 시 번호로 fallback

---

## Phase 4.8.11: 엣지 팝업 커서 버튼 pending 방식 통일

**목표**: 엣지 스와이프 팝업의 "싱글/멀티 커서" 버튼이 다른 모드 버튼(스크롤·클릭·이동·DPI 등)과 달리 탭 즉시 컨트롤러를 조작하고 팝업이 닫히던 것을, 다른 버튼과 동일한 "탭 → pending 변경 → 확인 버튼으로 커밋" 방식으로 통일한다. 계획에 없던 하위 Phase — Phase 4.8.7에서 "실동작화" 목적으로 즉발 처리해뒀던 것을 사용자 요청으로 재설계했다.

**개발 기간**: (세션 중 즉시 처리, 별도 산정 없음)

**세부 목표**:
1. 커서 버튼 탭: 싱글→멀티 시 **pending으로만** 전환하고, 동시에 다른 모드 버튼 자리가 분할 개수 옵션(2/3/4) 그리드로 대체되는 서브 화면에 진입한다. 팝업은 닫히지 않는다.
2. 개수 옵션 화면에서 스와이프로 하나를 고른 뒤 탭하면 개수가 pending으로 결정되고 원래 모드 그리드로 복귀한다(다른 버튼 재등장).
3. 이미 pending MULTI 상태에서 커서 버튼을 다시 탭하면 즉시 pending SINGLE로 돌아간다(개수 화면 재진입 안 함). 이때도 커서 버튼에 포커스가 유지된다(포커스가 다른 곳으로 튀지 않게, 사용자 추가 요청).
4. "확인" 버튼을 눌러야 pending cursorMode/개수가 실제 `MultiCursorController`에 반영된다(나머지 모드 상태를 먼저 커밋해 seed를 최신화한 뒤 `SetCursorCount`/`ToggleMultiCursor` 디스패치).
5. 스와이프 모드·직접 터치 모드 양쪽에 동일하게 적용.

**제거된 것**: Phase 4.8.7에서 구현했던 "커서 카드에 착지 후 세로 추가 스와이프로 개수 순환"(`cursorLocked` 등 제스처 로컬 잠금 상태) 및 커서 버튼 탭 즉발 `ToggleMultiCursor` 호출은 전부 제거됐다.

**변경 파일**:
- `TouchpadWrapper.kt`: `currentMultiCursorCount` 파라미터 추가, `isCursorCountSelecting` 상태 추가, `pendingMultiCursorCount`를 "개수 선택 서브 화면의 pending 개수" 의미로 재정의, `handleCursorTap()`/`handleCursorCountTap()`/`commitPopup()` 헬퍼로 스와이프·직접 터치 두 경로 로직 공유
- `EdgeSwipeOverlay.kt`: `isCursorCountSelecting` 파라미터 추가, 스와이프·직접 터치 그리드 양쪽에 개수 옵션(2/3/4) 렌더링 분기
- `Page2MultiCursorTouchpad.kt` / `StandardModePage.kt`: 현재 멀티 커서 개수(`currentMultiCursorCount`)를 `TouchpadWrapper`까지 배선(활성 시 실제 개수, 비활성 시 `lastMultiCursorCount`)

**참조 문서**: 이 Phase는 UX 플로우 변경으로 별도 설계 문서 보강 없이 진행(기존 §2.2.6 멀티 커서 알고리즘 명세 자체는 영향 없음 — 컨트롤러 조작 시점만 pending 커밋으로 이동).

**검증**:
- [ ] 커서 버튼 탭 → 다른 버튼들이 2/3/4 개수 버튼으로 대체되고 팝업 유지
- [ ] 개수 스와이프 선택 → 탭 → 모드 그리드 복귀, 커서 카드가 "멀티 커서"로 표시
- [ ] 확인 버튼 탭 → 실제 멀티 커서가 선택 개수로 활성화
- [ ] 멀티(pending) 상태에서 커서 버튼 재탭 → 싱글로 pending 전환 + 포커스가 커서 버튼에 유지, 확인 시 실제 비활성화
- [ ] 개수 화면에서 엣지로 밀기 → 팝업 전체 취소
- [ ] 직접 터치 모드에서도 위 전부 동일 동작

---

## Phase 4.8 완료 후 Page 2 구조

```
Page 2 — 풀 와이드 터치패드 (멀티 커서)
├── 터치패드 영역 (100% 너비 × 100% 높이)
│   ├── [싱글 커서] 전체 면적 단일 터치패드
│   └── [멀티 커서 — 그리드 분할] 활성 패드 테두리 + 비활성 dim + 패드별 엣지 존/라벨(4.8.9~10)
│   └── [멀티 커서 — 직접 전환 버튼] 전체 면적 입력 + 하단 전환 버튼 패널 + 패드별 엣지 존/라벨(4.8.9~10)
├── ControlButtonContainer (CursorModeButton 포함, 상단 오버레이)
├── 커서 수 선택 팝업 (CursorModeButton 위, 싱글→멀티 전환 시, 4.8.8 프리셋 시드 배정 포함 가능)
├── 4개 엣지 존 (전 페이지 공용, 4.8.7부터 멀티 커서 제어 액션 할당 가능)
└── 기존 가이드라인 등 오버레이
```

| 기능 | 싱글 커서 | 멀티 커서 |
|------|---------|---------|
| CursorModeButton | 멀티로 전환 (팝업) | 싱글로 복귀 |
| 터치패드 레이아웃 | 전체 면적 | 그리드 분할 또는 직접 전환 버튼 |
| ControlButtonContainer | 전체 표시 | 활성 패드 모드 상태 반영 |
| Windows 가상 커서 | 없음 | 전송 훅만 (실제 표시는 Phase 5) |
| 패드 경계 홀드 리셋 | 해당 없음 | 외부 보조 버튼 조합으로 드래그 |
| 엣지 존 멀티 커서 액션 (4.8.7) | 해당 없음 | 활성화/패드 전환/수 변경/레이아웃 토글을 4개 엣지 존 어디서든 실행 |
| 패드별 프리셋 시드 (4.8.8) | 해당 없음 | 활성화 시 패드마다 다른 모드 프리셋 자동 배정 |
| 패드별 엣지 존 할당 (4.8.9) | 해당 없음 | 각 패드가 독립 엣지 존 액션 세트 보유(그리드/직접 전환 모두) |
| 패드 커스텀 라벨 (4.8.10) | 해당 없음 | 롱프레스로 패드 이름 편집, 번호 대신 표시 |
| 엣지 팝업 커서 버튼 pending 방식 (4.8.11) | 해당 없음 | 커서 버튼도 다른 모드 버튼처럼 확인 버튼으로 커밋, 개수 선택은 서브 화면 |

> **소리 감지 패드 전환**: 마이크 입력으로 패드를 전환하는 기능은 멀티 커서 전용을 넘어 여러 앱 요소를 소리로 제어하는 일반 기능으로 확장하여 **Phase 7(추가 기능 개발)**에서 별도 계획한다. 이 Phase 범위에서 제외.
