---
title: "BridgeOne Phase 4.7: Page 2 — 풀 와이드 터치패드 (멀티 커서)"
description: "BridgeOne 프로젝트 Phase 4.7 - Standard 모드 Page 2: 풀 와이드 터치패드 + 멀티 커서(최대 4개) + 그리드 분할/직접 전환 버튼 레이아웃 + 소리 감지 전환"
tags: ["android", "multi-cursor", "touchpad", "full-width", "cursor-mode", "ui"]
version: "v1.0"
owner: "Chatterbones"
updated: "2026-04-01"
---

# BridgeOne Phase 4.7: Page 2 — 풀 와이드 터치패드 (멀티 커서)

**개발 기간**: 6-8일

**목표**: 터치패드 전체 화면 너비를 활용하는 Page 2를 구현합니다. 이 페이지는 멀티 커서 기능의 전용 홈으로, `CursorModeButton`이 활성화되어 싱글 ↔ 멀티 커서 전환이 가능합니다. 멀티 커서 활성 시 터치패드를 N개(최대 4개)로 분할하거나 직접 전환 버튼을 통해 여러 PC 커서를 독립 제어합니다.

**핵심 성과물**:
- HorizontalPager 4 → 5페이지 확장 + Page 2 Placeholder
- 풀 와이드 터치패드 레이아웃 (Actions 패널 없음, 터치패드가 전체 너비 점유)
- `CursorModeButton` 활성화 + `CursorCountPopup` (2~4개 선택)
- `MultiCursorState` + `PadModeState` 앱 수준 ViewModel 구조
- 그리드 분할 레이아웃 모드 (N개 영역 자동 분할)
- 직접 전환 버튼 레이아웃 모드 (`PadSwitchButtonPanel`)
- 소리 감지 패드 전환 (AudioRecord + 주파수 감지)
- Windows 서버 연동: `show_virtual_cursor` 전송, N-1개 가상 커서

**선행 조건**: Phase 4.3 (터치패드 고급 기능) 완료

**에뮬레이터 호환성**: 페이지 레이아웃, CursorCountPopup, 그리드 분할 UI, PadSwitchButtonPanel 에뮬레이터에서 개발 가능. 소리 감지 전환 및 Windows 서버 가상 커서 연동은 실기기에서 별도 검증.

---

## 현재 상태 분석

### 기존 구현
- `StandardModePage.kt`: 4페이지 HorizontalPager 구조 (Page 2는 `Page2AbsolutePointingPlaceholder()` → Phase 4.7에서 Page 2 = 멀티 커서로 재정의, 이전 Placeholder는 Page 3으로 이동)
- `TouchpadWrapper.kt`: 싱글 커서 터치패드 완전 구현 (Phase 4.3 완료)
- `ControlButtonContainer.kt`: `CursorModeButton` 이미 존재 — Page 1에서는 비표시, Page 2에서 표시
- `TouchpadState`: `cursorMode` 필드 존재 (SINGLE/MULTI)
- `MultiCursorState`, `PadModeState`: 설계 완료, 미구현

### 목표 구조 (component-touchpad.md §1.2 기준)
```
Page 2 — 풀 와이드 터치패드 (멀티 커서)
├── TouchpadAreaWrapper (전체 너비 × 전체 높이, Actions 패널 없음)
│   ├── [싱글 커서 모드] Touchpad1Area (전체 면적)
│   └── [멀티 커서 모드] 레이아웃 모드에 따라:
│       ├── [그리드 분할] N개 PadArea로 자동 분할
│       └── [직접 전환 버튼] 전체 면적 유지 + 하단 PadSwitchButtonPanel
├── ControlButtonContainer (상단 오버레이, CursorModeButton 포함)
└── 각종 팝업/오버레이 (CursorCountPopup, ScrollGuideline 등)
```

---

## Phase 4.7.1: HorizontalPager 5페이지 확장 + Page 2 풀 와이드 레이아웃

> **⚠️ Phase 4.3.11 변경사항**: `ControlButtonContainer`에 `config: ControlButtonConfig` 파라미터 추가.
> CursorModeButton 표시 활성화는 `showCursorModeButton = true` 파라미터가 아닌
> `config = ControlButtonConfig(showCursorMode = true)` 방식으로 전달.
> 기본값 `ControlButtonConfig()`는 기존 Page 1 구성(CursorMode 비표시)과 동일.

**목표**: 페이지 수를 4 → 5로 늘리고, Page 2를 Actions 패널 없이 터치패드가 전체 너비를 점유하는 레이아웃으로 구현합니다.

**개발 기간**: 0.5일

**쉬운 설명**: 지금 앱은 페이지가 4개인데, 멀티 커서 전용 새 페이지를 2번째에 끼워 넣어 총 5개로 늘립니다. 새 Page 2는 터치패드가 화면 전체를 꽉 채우는 구조입니다.

**세부 목표**:
1. `StandardModePage.kt` HorizontalPager 수정:
   - `pageCount = 4` → `pageCount = 5`
   - page 인덱스 재배치:
     - index 0: Page 1 (Standard, 기존 유지)
     - index 1: Page 2 (멀티 커서 풀 와이드, **신규**)
     - index 2: Page 3 (절대좌표 — 기존 Page 2 Placeholder 이동)
     - index 3: Page 4 (키보드 — 기존 Page 3 Placeholder 이동)
     - index 4: Page 5 (Minecraft — 기존 Page 4 Placeholder 이동)
   - `PageIndicator` 닷 5개로 확장
2. `Page2MultiCursorTouchpad` Composable 신규:
   - **레이아웃**: `TouchpadWrapper`가 전체 너비(100%) × 전체 높이 점유 (Page 1의 64% 비율 없음)
   - `CursorModeButton` 표시 활성화 (`showCursorModeButton = true` 파라미터)
   - 이 Phase에서는 싱글 커서 동작만 (멀티 커서 로직은 이후 Phase에서 추가)
3. 기존 Placeholder 함수명 일괄 수정:
   - `Page2AbsolutePointingPlaceholder` → `Page3AbsolutePointingPlaceholder`
   - `Page3KeyboardPlaceholder` → `Page4KeyboardPlaceholder`
   - `Page4MinecraftPlaceholder` → `Page5MinecraftPlaceholder`

**신규 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/pages/standard/Page2MultiCursorTouchpad.kt`

**수정 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/pages/StandardModePage.kt`

**참조 문서**:
- `docs/android/component-touchpad.md` §1.2 (터치패드 영역 구조)
- `docs/android/component-touchpad.md` §1.3 (버튼 구성 독립성 원칙)

**검증**:
- [ ] 5페이지 스와이프 전환 동작 (Page 1~5)
- [ ] PageIndicator 닷 5개 표시
- [ ] Page 2 터치패드 전체 너비 점유 확인
- [ ] Page 2에서 CursorModeButton 표시, Page 1에서 비표시 확인
- [ ] Page 3~5 Placeholder 정상 표시 (기존 Page 2~4와 동일)

---

## Phase 4.7.2: CursorModeButton 활성화 + CursorCountPopup + MultiCursorState ViewModel

**목표**: CursorModeButton 탭 시 커서 수 선택 팝업을 표시하고, 멀티 커서 활성화 흐름을 구현합니다. `MultiCursorState`를 앱 수준 ViewModel로 관리하는 구조도 이 Phase에서 확립합니다.

**개발 기간**: 1.5일

**쉬운 설명**: 상단 버튼을 눌러 "커서 2개? 3개? 4개?" 를 고르면 멀티 커서 모드로 진입합니다. 멀티 커서 상태는 페이지를 왔다 갔다 해도 유지되어야 하므로, 앱 전체에서 하나의 상태로 관리합니다.

**세부 목표**:
1. **`CursorCountPopup` Composable** (`ControlButtonContainer.kt` 또는 별도 파일):
   - 트리거: `CursorModeButton` 탭 (싱글 → 멀티 전환 시도 시)
   - 위치: `CursorModeButton` 위쪽에 소형 팝업
   - 구성: "2", "3", "4" 버튼 3개 — 탭 시 즉시 팝업 닫힘 + 선택 커서 수로 멀티 커서 활성화
   - 취소: 팝업 외부 탭 시 닫힘 (멀티 커서 미활성화)
   - 참조: `docs/android/component-touchpad.md` §1.6 (CursorCountPopup 설계)
2. **`MultiCursorViewModel`** (앱 수준 ViewModel, `viewmodel/` 또는 `ui/` 상위에 위치):
   - `MultiCursorState` 싱글톤 상태 관리
   - `PadModeState` 리스트 (`List<PadModeState>`, N개)
   - `activePadIndex: Int` (현재 조작 중인 패드)
   - `cursorCount: Int` (2~4)
   - `isMultiCursorActive: Boolean`
   - `layoutMode: MultiCursorLayoutMode` (GRID / DIRECT_SWITCH)
   - 주요 함수: `activateMultiCursor(count)`, `deactivateMultiCursor()`, `switchPad(index)`
3. **`MultiCursorState` + `PadModeState` 데이터 구조** (`TouchpadMode.kt` 또는 별도 파일):
   ```kotlin
   data class PadModeState(
       val clickMode: ClickMode = ClickMode.LEFT_CLICK,
       val moveMode: MoveMode = MoveMode.FREE,
       val scrollMode: ScrollMode = ScrollMode.OFF,
       val dpiLevel: DpiLevel = DpiLevel.NORMAL
   )

   data class MultiCursorState(
       val isActive: Boolean = false,
       val cursorCount: Int = 0,
       val activePadIndex: Int = 0,
       val padModeStates: List<PadModeState> = emptyList(),
       val layoutMode: MultiCursorLayoutMode = MultiCursorLayoutMode.GRID
   )

   enum class MultiCursorLayoutMode { GRID, DIRECT_SWITCH }
   ```
4. **`TouchpadWrapper` → `MultiCursorViewModel` 연결**:
   - Page 2 터치패드가 `MultiCursorViewModel`의 상태를 구독
   - `ControlButtonContainer`의 버튼 상태가 `activePad`의 `PadModeState`를 반영
5. **멀티 커서 비활성화** (멀티 → 싱글 복귀):
   - `CursorModeButton` 재탭 → 즉시 팝업 없이 싱글 커서 복귀
   - `deactivateMultiCursor()` 호출 → Windows 서버에 `hide_virtual_cursor` 전송 (Phase 4.7.6)
6. **싱글 커서 상태에서 `TouchpadState` 우선 사용**:
   - 싱글 커서 모드: 기존 `TouchpadState` 그대로 사용
   - 멀티 커서 모드: `MultiCursorState.activePad.PadModeState`를 참조

**신규 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/viewmodel/MultiCursorViewModel.kt`

**수정 파일**:
- `TouchpadMode.kt` (`MultiCursorState`, `PadModeState`, `MultiCursorLayoutMode` 추가)
- `Page2MultiCursorTouchpad.kt` (ViewModel 연결, CursorCountPopup 통합)
- `ControlButtonContainer.kt` (`CursorModeButton` 활성화, `PadModeState` 반영)

**참조 문서**:
- `docs/android/component-touchpad.md` §1.6 (CursorCountPopup)
- `docs/android/component-touchpad.md` §3.2.4 (커서 모드 플로우)
- `docs/android/technical-specification-app.md` §2.2.6 (멀티 커서 알고리즘 명세)

**검증**:
- [ ] CursorModeButton 탭 → CursorCountPopup 표시
- [ ] 2/3/4 선택 후 멀티 커서 활성화 (isMultiCursorActive = true)
- [ ] CursorModeButton 재탭 → 싱글 커서 복귀
- [ ] ControlButtonContainer 버튼이 activePad의 PadModeState 반영 확인
- [ ] 페이지 전환 후 복귀 시 멀티 커서 상태 유지 (ViewModel 싱글톤 확인)
- [ ] `ModePresetButton` 프리셋 적용이 활성 패드(`activePadIndex`)에만 적용되고, 다른 패드의 `PadModeState`는 유지 확인

---

## Phase 4.7.3: 그리드 분할 레이아웃 모드

**목표**: 멀티 커서 활성 시 터치패드를 N개 영역으로 자동 분할합니다. 비활성 영역 탭으로 즉시 패드 전환, 활성 영역에서만 커서 제어가 동작합니다.

**개발 기간**: 1.5일

**쉬운 설명**: 커서 3개 모드를 켜면 터치패드가 3칸으로 나뉩니다. 내가 지금 조종하는 커서 칸에는 테두리가 표시되고, 다른 칸을 탭하면 그 커서로 전환됩니다.

**세부 목표**:
1. **N개 패드 영역 계산 알고리즘** (`MultiCursorLayoutCalculator.kt`):
   - 2개: 좌/우 50/50 분할
   - 3개: 좌 50% / 우상 50%×50% / 우하 50%×50% (L자형)
   - 4개: 2×2 그리드
   - 분할 기준: `TouchpadAreaWrapper`의 실제 크기 기준 `RectF`로 경계 계산
   - 참조: `docs/android/technical-specification-app.md` §2.2.6.2 (N개 패드 영역 계산)
2. **`TouchpadAreaWrapper` 분할 렌더링**:
   - `MultiCursorState.isActive == true && layoutMode == GRID`일 때 N개 `PadArea` 렌더링
   - 각 `PadArea`에 개별 터치 이벤트 처리 (독립 `pointerInput`)
   - Selected(활성) 패드: 테두리 표시 (`§2.2` 색상 규칙 적용)
   - Unselected 패드: 테두리 없음 + 약한 dim 오버레이 (`alpha = 0.3f`)
3. **패드 전환 (그리드 분할 모드)**:
   - 비활성 패드 영역 탭 → `MultiCursorViewModel.switchPad(index)` 호출
   - 활성 패드 전환 시 `ControlButtonContainer` 버튼 상태 즉시 갱신
   - 전환 시 300ms `ease-out` 애니메이션 (테두리 이동)
4. **분할 경계선 표시**:
   - 패드 간 경계: 얇은 구분선 (`alpha = 0.2f`, 1dp)
   - 이 구분선을 통해 N개 영역이 시각적으로 구분됨
5. **싱글 커서 복귀 시 레이아웃 복원**:
   - 멀티 해제 시 분할 없이 단일 `Touchpad1Area`로 즉시 복원

**신규 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/utils/MultiCursorLayoutCalculator.kt`

**수정 파일**:
- `Page2MultiCursorTouchpad.kt` (분할 렌더링 및 패드 전환 로직)
- `TouchpadWrapper.kt` (멀티 커서 분기 추가)

**참조 문서**:
- `docs/android/component-touchpad.md` §1.2 (터치패드 영역 구조)
- `docs/android/technical-specification-app.md` §2.2.6.2 (N개 패드 영역 계산)
- `docs/android/component-touchpad.md` §2.2 (테두리 색상 규칙 — 멀티 커서 Selected 패드)

**검증**:
- [ ] 멀티 커서 2개: 좌/우 50/50 분할 정상 표시
- [ ] 멀티 커서 3개: L자형 3분할 정상 표시
- [ ] 멀티 커서 4개: 2×2 그리드 정상 표시
- [ ] 비활성 패드 탭 → 해당 패드로 즉시 전환 (테두리 이동)
- [ ] 활성 패드에서만 커서 이동 프레임 전송 (비활성 패드 입력 무시)
- [ ] 패드 전환 시 ControlButtonContainer 상태 갱신 확인

---

## Phase 4.7.4: 직접 전환 버튼 레이아웃 모드 (PadSwitchButtonPanel)

**목표**: 터치패드 전체 면적을 유지하면서 하단에 N개 전환 버튼을 표시합니다. 탭 1번으로 어떤 패드든 즉시 이동합니다.

**개발 기간**: 1일

**쉬운 설명**: 그리드 분할 대신, 터치패드는 그대로 두고 아래쪽에 "패드 1", "패드 2", "패드 3" 버튼들이 나타납니다. 원하는 버튼을 탭하면 그 커서로 바로 바뀝니다.

**세부 목표**:
1. **`PadSwitchButtonPanel` Composable** (신규):
   - 위치: `TouchpadAreaWrapper` 하단, 오버레이로 배치
   - 크기: 높이 40dp, 전체 너비 N등분
   - 구성: N개 버튼 (`Pad 1`~`Pad N`)
   - 활성 버튼: 강조색 배경 + 볼드 텍스트
   - 비활성 버튼: 반투명 배경
   - 탭: `MultiCursorViewModel.switchPad(index)` 즉시 호출
   - 이벤트 소비: `down.changes.forEach { it.consume() }` — TouchpadWrapper 제스처 차단
2. **레이아웃 모드 전환 UI**:
   - `ControlButtonContainer`에 레이아웃 모드 버튼 추가 (또는 설정 메뉴) — 설계 확정 후 반영
   - `MultiCursorViewModel.layoutMode`를 `GRID` ↔ `DIRECT_SWITCH` 전환
3. **직접 전환 버튼 모드에서 터치패드 입력**:
   - 전체 면적이 활성 패드의 입력 영역
   - 하단 버튼 패널 영역은 터치패드 입력 제외 (이미 이벤트 소비)

**신규 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/touchpad/PadSwitchButtonPanel.kt`

**수정 파일**:
- `Page2MultiCursorTouchpad.kt` (레이아웃 모드에 따른 조건부 렌더링)

**참조 문서**:
- `docs/android/component-touchpad.md` §1.2.2 (PadSwitchButtonPanel 설계)
- `docs/android/component-touchpad.md` §1.2.3 (멀티 커서 선택 상태 관리)

**검증**:
- [ ] 직접 전환 버튼 모드에서 하단 N개 버튼 표시
- [ ] 버튼 탭 → 해당 패드로 즉시 전환
- [ ] 버튼 탭 시 터치패드 커서 이동 미발생 (이벤트 소비 확인)
- [ ] 터치패드 전체 면적이 활성 패드 입력 영역으로 동작
- [ ] 그리드 분할 ↔ 직접 전환 버튼 모드 전환 시 레이아웃 즉시 반영

---

## Phase 4.7.5: 소리 감지 패드 전환 (AudioRecord)

**목표**: 마이크로 특정 음(솔 근방 주파수 + 볼륨 임계값 조합)을 감지하면 다음 패드로 순환 전환합니다. 헤드폰 사용 환경에서 손 조작 없이 패드를 전환할 수 있도록 합니다.

**개발 기간**: 1.5일

**쉬운 설명**: 설정에서 소리 감지를 켜두면, 특정 음을 내거나 특정 소리를 냈을 때 다음 커서로 자동으로 넘어갑니다. 손가락을 많이 움직이기 어려운 환경에서 유용합니다.

**세부 목표**:
1. **`SoundPadSwitchDetector`** (신규):
   - `AudioRecord` API로 마이크 입력 실시간 분석
   - FFT 기반 주파수 분석 (솔 근방: ~784Hz)
   - 볼륨 임계값 AND 주파수 임계값 동시 만족 시 트리거
   - 연속 오발동 방지: 트리거 후 최소 500ms 쿨다운
   - 마이크 권한(`RECORD_AUDIO`) 없으면 기능 비활성화 + 안내 토스트
2. **활성화 조건**:
   - `MultiCursorState.isActive == true`이고 설정에서 소리 감지 옵션 활성화된 경우에만 동작
   - 멀티 커서 비활성화 시 즉시 중지
3. **패드 전환 동작**:
   - 감지 → `MultiCursorViewModel.switchPad((activePadIndex + 1) % cursorCount)` 순환
   - 전환 시 햅틱 피드백 + `ToastController.show("패드 ${n} 전환", ToastType.INFO, 1000L)`

**신규 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/utils/SoundPadSwitchDetector.kt`

**수정 파일**:
- `Page2MultiCursorTouchpad.kt` (소리 감지 활성화/비활성화 연결)

**참조 문서**:
- `docs/android/component-touchpad.md` §1.2.3 (소리 감지 전환)
- `docs/android/technical-specification-app.md` §2.2.6 (멀티 커서 패드 전환 트리거)

> **⚠️ 마이크 권한**: `AndroidManifest.xml`에 `RECORD_AUDIO` 권한 추가 필요. 런타임 권한 요청은 `accompanist-permissions` 사용.

**검증**:
- [ ] 특정 주파수 + 볼륨 조합 감지 시 다음 패드로 순환 전환 (에뮬레이터: 마이크 입력 시뮬레이션)
- [ ] 500ms 쿨다운 — 연속 빠른 소리에 오발동 없음
- [ ] 멀티 커서 비활성화 시 감지 중지 확인
- [ ] 마이크 권한 없을 때 기능 비활성화 + 안내 토스트 표시

---

## Phase 4.7.6: Windows 서버 연동 (show_virtual_cursor + 패드 경계 홀드 리셋)

**목표**: 멀티 커서 활성화/비활성화 시 Windows 서버에 가상 커서 표시/숨김 명령을 전송합니다. 패드 경계 홀드 리셋 워크플로우도 이 Phase에서 구현합니다.

**개발 기간**: 1일

**쉬운 설명**: 스마트폰에서 멀티 커서를 켜면, PC 화면에 보조 커서들이 표시됩니다. 또한 손가락이 패드 경계를 넘을 때 클릭 상태가 자동으로 해제되는 문제가 있는데, 이를 보조 버튼과 조합해서 드래그가 끊기지 않도록 처리합니다.

**세부 목표**:
1. **`show_virtual_cursor` 전송** (멀티 커서 활성화 시):
   - JSON payload: `{"command": "show_virtual_cursor", "cursor_count": N}`
   - UART → Vendor CDC → Windows 서버 전달
   - ACK 수신: `pad1~padN` 초기 커서 위치 저장 → `MultiCursorState` 업데이트
2. **`hide_virtual_cursor` 전송** (멀티 커서 비활성화 시):
   - JSON payload: `{"command": "hide_virtual_cursor"}`
   - 전송 후 `MultiCursorState` 초기화
3. **패드 전환 시 `switch_cursor` 전송**:
   - JSON payload: `{"command": "switch_cursor", "pad_index": N}`
   - Windows 서버가 활성 가상 커서를 교체
4. **패드 경계 홀드 리셋 워크플로우**:
   - 손가락이 패드 경계를 넘는 순간 터치업 이벤트 발생 → BridgeOne 홀드 상태 리셋
   - **드래그 워크플로우 (외부 보조 버튼 조합)**:
     - 보조 버튼으로 클릭 홀드 유지 → 패드를 넘나들며 드래그 → 보조 버튼 해제로 홀드 종료
   - BridgeOne 앱에서는 별도 구현 불필요 — 보조 버튼이 독립 HID 디바이스로 처리됨
   - 참조: `docs/android/component-touchpad.md` §1.2 (⚠️ 패드 경계 홀드 리셋 설명)
5. **Essential 모드 처리**:
   - Windows 서버 미연결 시 `show_virtual_cursor` 전송 스킵 — 앱 내에서만 멀티 커서 상태 관리

**수정 파일**:
- `MultiCursorViewModel.kt` (서버 명령 전송 로직)
- `src/android/app/src/main/java/com/bridgeone/app/protocol/FrameBuilder.kt` (멀티 커서 커스텀 명령 생성)

**참조 문서**:
- `docs/android/technical-specification-app.md` §2.2.6 (멀티 커서 활성화/비활성화 플로우)
- `docs/technical-specification.md` §4.4.3 (멀티 커서 시스템 전체 플로우)
- `docs/windows/technical-specification-server.md` §3.6 (Windows 서버 N-1개 가상 커서)

**검증**:
- [ ] 멀티 커서 활성화 시 `show_virtual_cursor` UART 전송 확인 (로그)
- [ ] ACK 수신 후 `MultiCursorState` 커서 위치 업데이트 확인
- [ ] 멀티 커서 비활성화 시 `hide_virtual_cursor` 전송 확인
- [ ] Essential 모드(서버 미연결)에서 전송 스킵, 크래시 없음
- [ ] Windows 서버 연동 시 PC 화면에 N-1개 가상 커서 표시 (실기기 검증)

---

## Phase 4.7 완료 후 Page 2 구조

```
Page 2 — 풀 와이드 터치패드 (멀티 커서)
├── TouchpadAreaWrapper (100% 너비 × 100% 높이)
│   ├── [싱글 커서] Touchpad1Area (전체 면적)
│   └── [멀티 커서 — 그리드 분할]
│       ├── PadArea 1 (Selected: 테두리 표시)
│       ├── PadArea 2 (Unselected: dim 오버레이)
│       ├── PadArea 3 (선택 시)
│       └── PadArea 4 (선택 시)
│   └── [멀티 커서 — 직접 전환 버튼]
│       ├── 전체 면적 (활성 패드 입력 영역)
│       └── PadSwitchButtonPanel (하단 40dp 오버레이)
├── ControlButtonContainer (CursorModeButton 포함, 상단 오버레이)
├── CursorCountPopup (CursorModeButton 위, 싱글→멀티 전환 시)
└── ScrollGuideline, RightAngleGuideline 등 기존 오버레이
```

| 기능 | 싱글 커서 | 멀티 커서 |
|------|---------|---------|
| CursorModeButton | 멀티로 전환 (팝업) | 싱글로 복귀 |
| 터치패드 레이아웃 | 전체 면적 | 그리드 분할 또는 직접 전환 버튼 |
| ControlButtonContainer | 전체 표시 | 활성 패드 PadModeState 반영 |
| 소리 감지 전환 | 비활성 | 설정 시 활성 |
| Windows 가상 커서 | 없음 | N-1개 표시 |
| 패드 경계 홀드 리셋 | 해당 없음 | 보조 버튼 조합으로 드래그 |
