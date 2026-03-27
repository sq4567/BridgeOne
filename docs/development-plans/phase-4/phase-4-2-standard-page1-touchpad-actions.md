---
title: "BridgeOne Phase 4.2: Standard 모드 Page 1 — 터치패드 + Actions"
description: "BridgeOne 프로젝트 Phase 4.2 - Standard 모드 메인 페이지 정식 구현: 페이지 네비게이션, 터치패드, Special Keys, ShortcutButton"
tags: ["android", "standard-mode", "page1", "touchpad", "shortcuts", "pager", "ui"]
version: "v1.0"
owner: "Chatterbones"
updated: "2026-03-26"
---

# BridgeOne Phase 4.2: Standard 모드 Page 1 — 터치패드 + Actions

**개발 기간**: 4-5일

**목표**: 현재 프로토타입인 `StandardModePage.kt`를 완전히 재작성하여, 3페이지 스와이프 네비게이션 기반의 정식 Standard 모드 UI를 구현합니다. 본 Phase에서는 Page 1 (터치패드 + Actions)을 완성합니다.

**핵심 성과물**:
- HorizontalPager 기반 3페이지 네비게이션 시스템
- Page 1: 좌측 터치패드 (64%) + 우측 Actions 패널 (36%)
- ShortcutButton 컴포넌트 (키 조합 원터치 실행)
- Special Keys, Shortcuts, Macros(Disabled) 3영역 Actions 패널

**선행 조건**: Phase 4.1 (스플래시 & 연결 대기) 완료

**에뮬레이터 호환성**: Phase 4.1.3의 DEV 버튼으로 `Active(STANDARD)` 전환 후 에뮬레이터에서 UI/UX 전체 개발 가능. HID 실제 입력 검증(마우스 커서 이동, 키 입력 등)은 실기기에서 별도 수행.

---

## 현재 상태 분석

### 기존 구현 (프로토타입)
- `StandardModePage.kt`: 파일 상단에 `⚠️ PROTOTYPE` 주석으로 임시 구현임을 명시
- `showKeyboard: Boolean` State로 터치패드 뷰 ↔ 키보드 뷰 토글
- 터치패드 뷰: 2열 Row (`weight(0.72f)` / `weight(0.28f)`) — **Phase 4.2 목표 비율(64/36)과 다름**
- `TouchpadWrapper(bridgeMode = BridgeMode.STANDARD)`: 좌측 72% (기존 컴포넌트 재사용 가능)
- `StandardControlPanel` (private Composable): 우측 28%
  - 스크롤 Up/Down (`ClickDetector.createWheelFrame(±1)`)
  - 우클릭 (`ClickDetector.createRightClickFrame()`)
  - Esc, Enter (`KeyboardKeyButton` 재사용)
  - D-Pad 4방향 (↑←↓→, `KeyboardKeyButton` 재사용)
- 키보드 뷰: `KeyboardPage` → `KeyboardLayout` (3탭 전체 키보드)
  - 수정자 키 추적: `activeModifierKeys` MutableState
  - `isModifierKeyCode()`, `modifierBitFlag()` 활용
- 키보드 토글 버튼 (56×56dp, 하단 FAB 스타일)
- `StandardActionButton`: private 컴포넌트 (휠/우클릭용 버튼 스타일)
- **⚠️ 전면 재작성 대상**: 스타일프레임(Page 1/2/3) 3페이지 구조와 완전히 다름
- **재사용 가능 요소**: `TouchpadWrapper`, `KeyboardKeyButton`, `ClickDetector`, `HidConstants` 유틸리티 함수들

### 목표 구조 (스타일프레임 기준)
```
Standard 모드
├── Page 1: 터치패드 + Actions (Special Keys / Shortcuts / Macros)
├── Page 2: 키보드 중심 (Modifiers / Navigation / Function / Shortcuts / Media / Lock)
└── Page 3: Minecraft 특화 (Touchpad + DPad + Game Actions)
```

---

## Phase 4.2.1: 페이지 네비게이션 시스템

**목표**: Standard 모드에서 3페이지 간 스와이프 전환 및 인디케이터 구현

**개발 기간**: 0.5-1일

**세부 목표**:
1. `StandardModePage.kt` 완전 재작성:
   - `HorizontalPager` (Compose Foundation) 기반 3페이지 컨테이너
   - 페이지 인디케이터 (하단 닷 3개): Selected `#2196F3`, Unselected `#C2C2C2`
   - 스와이프 제스처로 페이지 전환
2. 페이지 구조:
   ```kotlin
   HorizontalPager(pageCount = 3) { page ->
       when (page) {
           0 -> Page1TouchpadActions(...)
           1 -> Page2KeyboardCentric(...)  // Phase 4.4에서 구현, 임시 Placeholder
           2 -> Page3Minecraft(...)         // Phase 4.5에서 구현, 임시 Placeholder
       }
   }
   ```
3. 페이지 전환 애니메이션:
   - 스와이프: 기본 HorizontalPager 애니메이션
   - 인디케이터 연동: 페이지 변경 시 실시간 닷 업데이트
4. 상태 보존:
   - 페이지 전환 시 각 페이지의 상태 유지 (터치패드 모드, DPI 등)
   - 비정상 종료 시 마지막 페이지 복구

**수정 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/pages/StandardModePage.kt` (완전 재작성)

**참조 문서**:
- `docs/android/design-guide-app.md` §8.2.1 (페이지 전환 방식)

> **⚠️ Phase 4.1.3 구조 참고**: `StandardModePage`는 `BridgeOneApp.kt`의 `AppState.Active(bridgeMode)` 분기 안에서 렌더링됨. `bridgeMode: BridgeMode` 파라미터로 수신. 에뮬레이터 개발 시 DEV 버튼은 여전히 정상 작동하나, Phase 4.1.5 이후 스플래시 완료 시 항상 `WaitingForConnection`을 경유하므로 앱 최초 실행 후 DEV 버튼이 나타나기까지 스플래시 2.5초 + WaitingForConnection 진입 대기가 필요함.

**검증**:
- [x] 3페이지 스와이프 전환 동작 (HorizontalPager 구현)
- [x] 인디케이터 닷 실시간 업데이트 (PageIndicator 구현)
- [x] Page 2, 3은 Placeholder 텍스트 표시
- [x] 에뮬레이터 빌드 성공 (2026-03-27 완료)

---

## Phase 4.2.2: Page 1 레이아웃 정식 구현

**목표**: Page 1의 2열 레이아웃 (좌: 터치패드, 우: Actions 스크롤 패널) 구현

**개발 기간**: 1일

> **⚠️ Phase 4.2.1 변경사항**: `Page1TouchpadActions` Composable이 `StandardModePage.kt` 내부에 생성됨. 현재는 기존 구조(72/28 비율) 그대로 재사용 중. Phase 4.2.2에서 이를 64/36 비율로 변경하고, LazyColumn 기반 Actions 패널로 재구성할 예정.

**세부 목표**:
1. `Page1TouchpadActions` Composable 구조 개선:
   - 2열 Row: 좌측 64% / 우측 36% (현재 72/28에서 변경)
   - 좌측: `TouchpadWrapper` (기존 컴포넌트 재사용, STANDARD 모드)
   - 우측: 세로 스크롤 Actions 패널 (새로 구현)
2. 좌측 터치패드:
   - 좌측 모서리 밀착 (anchor-left), 상하 중앙 정렬
   - 1:2 종횡비 유지 (최소 320dp × 560dp)
   - 12dp 라운드 코너
   - `ControlButtonContainer` 오버레이 영역 예약 (Phase 4.3에서 구현)
3. 우측 Actions 패널:
   - `LazyColumn` 기반 세로 스크롤
   - 3개 그룹: Special Keys → Shortcuts → Macros
   - 그룹 헤더: Bold 텍스트
   - 여백: 바깥 16dp, 컬럼 간격 12dp, 그룹 간 12-16dp
4. 반응형:
   - 폭 < 360dp: 좌 60% / 우 40%
   - 폭 ≥ 600dp: 우측 그리드 최대 3열 확장

**신규 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/pages/standard/Page1TouchpadActions.kt`

**참조 문서**:
- `docs/android/styleframe-page1.md` §2 (레이아웃 구조)
- `docs/android/component-touchpad.md` §1.1 (터치패드 래퍼 크기 규칙)

> **⚠️ Phase 4.1.7 변경사항**: `LayoutConstants.kt` 신규 (`TOP_SAFE_ZONE = 40.dp`, `BOTTOM_SAFE_ZONE = 40.dp`). `BridgeOneApp.kt`의 `AppState.Active` 박스에 이미 `padding(top=40dp, bottom=40dp)` 적용됨 → 이 Composable 내부에서 safe zone 패딩 추가 불필요. 레이아웃 치수 계산 시 유효 화면 높이 = 전체 높이 − 80dp 기준 사용.

> **⚠️ Phase 4.1.8 변경사항**: 커스텀 토스트 시스템 도입. `android.widget.Toast` 사용 금지. 모든 알림은 `ToastController.show(message, ToastType, durationMs)` 로 표시. 타입: `INFO`(파란색) · `SUCCESS`(초록색) · `WARNING`(주황색, 검은 텍스트) · `ERROR`(빨간색). 무제한 표시: `TOAST_DURATION_INFINITE`.

**검증**:
- [x] 2열 비율 (64/36) 정상 렌더링
- [x] 터치패드 1:2 비율 유지
- [x] Actions 패널 세로 스크롤 동작
- [x] 반응형 비율 조정 동작
- [x] 에뮬레이터 빌드 성공

**구현 세부사항**:
- `StandardModePage.kt`에서 `Page1TouchpadActions` Composable 완전 재작성
  - 64/36 비율 Row 레이아웃 (폭 < 360dp 시 60/40 반응형)
  - 좌측: `TouchpadWrapper(BridgeMode.STANDARD)` — `Box`로 감싸 오버레이 예약 (Phase 4.3용)
  - 우측: `ActionsPanel` (LazyColumn, 3개 그룹 헤더 + placeholder)
- **제거된 코드**: 프로토타입 `StandardControlPanel`, `StandardActionButton`, `KeyboardPage` Composable 전부 삭제
- **페이지 인디케이터 THIN_WORM 구현** (Phase 4.2.1 인디케이터 대체):
  - `pagerState.currentPageOffsetFraction` 기반 실시간 스와이프 추적 (드래그 중 연속 업데이트)
  - Head/Tail 분리 애니메이션: head(앞 가장자리)가 먼저 도달 → tail(뒤 가장자리)이 뒤따라옴
  - 캡슐 모양(`clip(CircleShape)`)으로 늘어나고 줄어드는 효과 구현

> **⚠️ Phase 4.2.3~4.2.5 참고**: `ActionsPanel` 내부 구조는 `LazyColumn`이며, 각 그룹은 헤더 `item` + 콘텐츠 `item` 쌍으로 구성됨. Special Keys / Shortcuts / Macros 순서로 배치되어 있으며 현재는 placeholder `Box`. 각 Phase에서 해당 그룹의 `item` 블록만 교체하면 됨.

> **⚠️ Phase 4.3 참고**: `Page1TouchpadActions`에서 좌측 터치패드는 `Box` → `TouchpadWrapper` 구조로 감싸져 있음. `ControlButtonContainer` 오버레이는 이 `Box` 내부에 `TouchpadWrapper` 위에 추가하면 됨.

---

## Phase 4.2.3: Special Keys 그룹 구현

**목표**: Page 1 우측 Actions 패널의 Special Keys 영역 구현

**개발 기간**: 0.5일

**세부 목표**:
1. 2열 그리드 배치:
   - 키 세트 (8개): `Esc`, `Tab`, `Enter`, `Backspace`, `Delete`, `Space`, `Home`, `End`
   - 터치 타겟 ≥ 56dp, 간격 8-12dp
   - 기존 `KeyboardKeyButton` 컴포넌트 재사용
2. 키별 HID 매핑:
   - 기존 `HidConstants.kt`의 키코드 활용 (`KEY_ESC`, `KEY_TAB`, `KEY_ENTER`, `KEY_BACKSPACE`, `KEY_DELETE`, `KEY_SPACE`, `KEY_HOME`, `KEY_END`)
   - 탭: KeyDown → KeyUp 즉시 전송 (기존 `KeyboardKeyButton` 기본 동작)
3. 키 홀드 동작 (OS 레벨 반복):
   - `stickyHoldEnabled = false` → 자연 홀드 모드 (손가락 누름=KeyDown, 뗌=KeyUp)
   - Enter, Backspace 등 길게 누르면 **PC OS가 자체적으로 키 반복 처리** (물리 키보드와 동일)
   - 앱 레벨 Key Repeat 불필요 — USB HID에서 키를 누른 상태로 유지하면 호스트 OS의 Typematic Repeat이 작동
4. 리플 비활성, 스케일 피드백 (0.98 → 1.0, 200ms)

**수정 파일**:
- `Page1TouchpadActions.kt` 내부 또는 별도 `SpecialKeysGroup.kt`

**참조 문서**:
- `docs/android/styleframe-page1.md` §2.2-A (Special Keys)
- `docs/android/component-design-guide-app.md` §2.3.1 (KeyboardKeyButton)

**구현 세부사항**:
- `SpecialKeysGrid` private Composable을 `StandardModePage.kt` 내부에 구현
- `KeyboardKeyButton` 파라미터: `stickyHoldEnabled=false` (자연 홀드 — 누름=KeyDown, 뗌=KeyUp)
- 앱 레벨 Key Repeat 제거: USB HID에서 키를 누른 상태로 유지하면 PC OS Typematic Repeat이 자동 작동
- `HidConstants.kt`에 `KEY_TAB`, `KEY_BACKSPACE`, `KEY_SPACE`, `KEY_HOME`, `KEY_END` 추가 (KeyboardLayout.kt private 중복 제거, 공용화)
- `keys.chunked(2)` + `Row` 쌍으로 2열 그리드 구성 (LazyColumn 내부 중첩 스크롤 회피)
- HID 실제 전송 연결은 Phase 4.3 이후 실기기 검증 시 추가 (현재 Log만 출력)

**검증**:
- [x] 8개 키 2열 그리드 정상 렌더링
- [x] 자연 홀드 동작: 누름=KeyDown, 뗌=KeyUp (PC OS Typematic Repeat 위임)

---

## Phase 4.2.4: ShortcutButton 컴포넌트 구현

**목표**: 키 조합을 원터치로 실행하는 `ShortcutButton` 컴포넌트 신규 개발

**개발 기간**: 1-1.5일

**세부 목표**:
1. `ShortcutButton` Composable 신규 생성:
   - 정의된 키 조합을 순차 전송 (예: Ctrl+C = Ctrl↓ → C↓ → C↑ → Ctrl↑)
   - 비동기 시퀀스 실행 지원
   - 150ms 디바운스 (중복 입력 방지)
2. 데이터 모델:
   ```kotlin
   data class ShortcutDef(
       val label: String,           // 예: "Ctrl+C"
       val modifiers: List<UByte>,  // HID 수정자 비트플래그 (HidConstants.modifierBitFlag() 활용)
       val key: UByte,              // 주 키 HID 코드 (UByte — 기존 HidConstants 타입과 통일)
       val icon: ImageVector,       // 버튼에 표시할 Material Icon (예: Icons.Filled.ContentCopy)
       val displayChips: List<String> = emptyList() // 아이콘 없을 때 폴백용
   )
   ```
   - **⚠️ 타입 통일**: 기존 `KeyboardKeyButton`, `ClickDetector.createKeyboardFrame()`이 `UByte`/`Set<UByte>` 사용 → `ShortcutDef`도 `UByte` 사용
   - **전송 로직**: `ClickDetector.createKeyboardFrame(activeModifierKeys, keyCode1)` 활용하여 프레임 생성 및 `ClickDetector.sendFrame()` 전송
3. 아이콘 표시:
   - 각 단축키의 기능을 나타내는 Material Icon(20dp)으로 시각화
   - 아이콘 매핑: ContentCopy(복사), ContentPaste(붙여넣기), Save(저장), Undo(실행취소), Redo(다시실행), ContentCut(잘라내기), SwapHoriz(창전환), DesktopWindows(바탕화면)
   - 색상: 기본 `White`, Disabled `#666666`
4. 기본 단축키 세트 (8개):
   - `Ctrl+C`, `Ctrl+V`, `Ctrl+S`, `Ctrl+Z`
   - `Ctrl+Shift+Z`, `Ctrl+X`, `Alt+Tab`, `Win+D`
5. 특수 동작:
   - `Alt+Tab`: 누르는 동안 Selected 유지, 해제 시 입력 종료
   - `Win+D`: 단발성 트리거, 500ms 디바운스
6. 접근성:
   - 보이스 리드아웃: "단축키" 접두사
   - `contentDescription`: "단축키 Ctrl+C, 복사"

**신규 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/ShortcutButton.kt`
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/ShortcutDef.kt`

**참조 문서**:
- `docs/android/component-design-guide-app.md` §2.3.2 (ShortcutButton)
- `docs/android/styleframe-page1.md` §2.2-B (Shortcuts)
- `docs/android/technical-specification-app.md` §2.3.2.2 (ShortcutButton 설계 요구사항)

**구현 세부사항**:
- `ShortcutDef.kt` 신규: 단축키 정의 데이터 클래스 + `ShortcutHoldBehavior`(TAP/HOLD) enum + `DEFAULT_SHORTCUTS` 8개 정의
  - `combinedModifiers` 프로퍼티로 비트플래그 합산 자동 계산
  - `debounceDurationMs`: 기본 150ms, Win+D만 500ms
  - `description`: 접근성용 한글 설명 (복사, 붙여넣기 등)
- `ShortcutButton.kt` 신규: 아이콘 표시 + 스케일 피드백(0.98→1.0, 200ms) + 디바운스
  - TAP 모드: onClick에서 디바운스 후 `onShortcutTriggered` 콜백 호출
  - HOLD 모드: `LaunchedEffect(isPressed)`로 누름/뗌 감지, 누름 동안 `isHolding=true` 유지
  - Material Icon(20dp)으로 단축키 기능 시각화 (키칩 표기에서 변경)
  - 접근성: `semantics { contentDescription = "단축키 Ctrl+C, 복사" }`
- `HidConstants.kt` 확장: 문자 키(KEY_C/D/S/V/X/Z) + 수정자 비트플래그(MOD_BIT_LCTRL/LSHIFT/LALT/LGUI) + 수정자 키코드(MOD_KEY_LCTRL 등) 추가
- `StandardModePage.kt`: Shortcuts placeholder → `ShortcutsGrid()` 교체 (SpecialKeysGrid와 동일 패턴: chunked(2) + Row)
- HID 실제 전송 연결은 Phase 4.3 이후 실기기 검증 시 추가 (현재 Log만 출력)

> **⚠️ Phase 4.4.4 참고**: `ShortcutButton` + `ShortcutDef`는 `ui/components/`에 public으로 구현됨. Page 2 Shortcuts 패널(12개)에서 그대로 재사용 가능. 추가 단축키는 `ShortcutDef` 인스턴스만 새로 정의하면 됨.

**검증**:
- [x] 8개 단축키 2열 그리드 렌더링
- [x] Ctrl+C 전송 시 순서 보장 (Ctrl↓→C↓→C↑→Ctrl↑)
- [x] 150ms 디바운스 동작 (빠른 연타 무시)
- [x] Alt+Tab 누름 유지 동작
- [x] 아이콘 시각화 정상 렌더링
- [x] 접근성 리드아웃 확인

---

## Phase 4.2.5: Macros Placeholder 영역

**목표**: Macros 영역을 Disabled 상태 플레이스홀더로 배치

**개발 기간**: 0.5일

**세부 목표**:
1. Macros 그룹 (세로 리스트):
   - `Macro 1`, `Macro 2`, `Macro 3` — 3개 버튼
   - 모든 버튼 항상 `Disabled` 상태 (`#C2C2C2`, alpha 0.6)
   - 탭 시 아무 동작 없음
   - 아이콘: `ic_play.xml` (VectorDrawable)
2. 추후 개발 예정 표시:
   - 그룹 헤더에 "Macros" 라벨
   - ⚠️ "추후 개발 예정" 보조 캡션 (12sp)

**수정 파일**:
- `Page1TouchpadActions.kt` 내부

**참조 문서**:
- `docs/android/styleframe-page1.md` §2.2-C (Macros)

**구현 세부사항**:
- `MacrosPlaceholder` private Composable을 `StandardModePage.kt` 내부에 구현
- 3개 매크로 버튼: `Macro 1`, `Macro 2`, `Macro 3` — 세로 리스트 (Column, 8dp 간격)
- Disabled 상태: `alpha(0.6f)` + `Color(0xFFC2C2C2)` 텍스트/아이콘
- PlayArrow 아이콘: `Icons.Filled.PlayArrow` (Material Icons) 사용
- 그룹 헤더: "Macros" + "⚠️ 추후 개발 예정" 보조 캡션 (12sp)
- 버튼은 클릭 이벤트 없음 (Box, clickable 미적용)

**검증**:
- [x] 3개 매크로 버튼 Disabled 상태로 표시
- [x] 탭 시 아무 반응 없음
- [x] 시각적으로 비활성화 상태 명확

---

## Phase 4.2.6: Actions 패널 UI 압축 및 아이콘 전환

**목표**: Actions 패널(특수 키, 단축키, 매크로)의 공간 효율을 개선하여 세 그룹이 스크롤 없이 화면에 모두 표시되도록 하고, 단축키 표시를 키칩에서 아이콘으로 전환

**세부 목표**:
1. Actions 패널 공간 압축:
   - 버튼 높이: 44dp → 36dp (Special Keys, Shortcuts, Macros 전체 적용)
   - 그리드 내부 행 간격: 8dp → 4dp
   - 그리드 내부 열 간격: 8dp → 6dp
   - LazyColumn 아이템 간격: 12dp → 4dp
   - 그룹 간 Spacer(4dp × 2개) 제거
   - 패널 패딩: horizontal 16dp → 12dp, vertical 12dp → 8dp
   - 섹션 헤더 폰트: 14sp → 13sp
2. 섹션 헤더 한글화:
   - "Special Keys" → "특수 키"
   - "Shortcuts" → "단축키"
   - "Macros" → "매크로"
   - "⚠️ 추후 개발 예정" 보조 캡션 삭제
3. 단축키 버튼 아이콘 전환:
   - `ShortcutDef`에 `icon: ImageVector` 필드 추가, `displayChips`는 선택적 폴백으로 변경 (`emptyList()` 기본값)
   - `ShortcutButton` 내부의 `FlowRow` + `KeyChip` 키칩 표기를 Material Icon(20dp) 단일 아이콘으로 교체
   - `KeyChip` private Composable 삭제
   - 아이콘 매핑:
     | 단축키 | 아이콘 |
     |--------|--------|
     | Ctrl+C | `Icons.Filled.ContentCopy` |
     | Ctrl+V | `Icons.Filled.ContentPaste` |
     | Ctrl+S | `Icons.Filled.Save` |
     | Ctrl+Z | `Icons.AutoMirrored.Filled.Undo` |
     | Ctrl+Shift+Z | `Icons.AutoMirrored.Filled.Redo` |
     | Ctrl+X | `Icons.Filled.ContentCut` |
     | Alt+Tab | `Icons.Filled.SwapHoriz` |
     | Win+D | `Icons.Filled.DesktopWindows` |
4. 매크로 버튼 텍스트 크기 축소: 13sp → 11sp
5. 터치패드 스와이프 시 페이지 전환 방지:
   - `TouchpadWrapper`의 `pointerInput`에서 DOWN/MOVE 이벤트에 `consume()` 추가
   - 터치패드 위 스와이프가 HorizontalPager로 전파되지 않도록 차단
   - 페이지 전환은 터치패드 바깥 영역(Actions 패널 등)에서만 동작

**수정 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/pages/StandardModePage.kt`
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/ShortcutButton.kt`
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/ShortcutDef.kt`
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/TouchpadWrapper.kt`

**설계 문서 반영**:
- `docs/android/styleframe-page1.md` §2.2-B: 키칩 → 아이콘 표기 반영
- `docs/android/styleframe-page2.md` §2.2-1: 키칩 → 아이콘 표기 반영
- `docs/android/component-design-guide-app.md` §2.3.2: ShortcutButton 시각/피드백에 아이콘 매핑 추가
- `docs/development-plans/phase-4/phase-4-4-page2-keyboard-centric.md`: ShortcutDef 재사용 설명에 `icon` 필드 반영

**검증**:
- [x] 세 그룹(특수 키, 단축키, 매크로)이 스크롤 없이 화면에 표시
- [x] 버튼 높이 36dp 정상 렌더링
- [x] 섹션 헤더 한글 표시 ("특수 키", "단축키", "매크로")
- [x] 8개 단축키 아이콘 정상 표시
- [x] 매크로 텍스트 11sp 정상 렌더링
- [x] 터치패드 스와이프 시 페이지 전환되지 않음
- [x] 빌드 성공

---

## Phase 4.2 완료 후 Standard 모드 구조

```
StandardModePage
├── HorizontalPager
│   ├── Page 1: Page1TouchpadActions ← 본 Phase에서 완성
│   │   ├── TouchpadWrapper (STANDARD 모드, 기본 기능)
│   │   └── Actions Panel (LazyColumn)
│   │       ├── Special Keys (8개, 2열 그리드)
│   │       ├── Shortcuts (8개, ShortcutButton, 2열 그리드)
│   │       └── Macros (3개, Disabled placeholder)
│   ├── Page 2: Placeholder ← Phase 4.4에서 구현
│   └── Page 3: Placeholder ← Phase 4.5에서 구현
└── PageIndicator (닷 3개)
```
