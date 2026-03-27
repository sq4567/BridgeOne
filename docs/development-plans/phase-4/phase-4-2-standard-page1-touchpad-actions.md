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
- [x] 에뮬레이터 빌드 성공 (2026-03-28 완료)

**✅ Phase 4.2.2 완료** (2026-03-28)

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
3. `Enter` 롱프레스 반복 입력:
   - 400ms 초기 지연 후 60ms 간격 반복
   - 반복 중 토스트 피드백: `ToastController.show("Enter 반복 입력 중", ToastType.INFO, 1500L)`
   - **⚠️ 기존 `KeyboardKeyButton` Sticky Hold(500ms)와 충돌**: Page 1 Special Keys에서는 Sticky Hold 대신 Key Repeat 동작이 필요
   - **구현 방안**: `KeyboardKeyButton`에 `repeatEnabled: Boolean = false` 파라미터 추가, `repeatEnabled=true`일 때 Sticky Hold 대신 Key Repeat 동작 수행
   - 또는 Phase 4.4.2에서 Key Repeat 기능을 `KeyboardKeyButton`에 통합 구현 시 함께 적용
4. 리플 비활성, 스케일 피드백 (0.98 → 1.0, 200ms)

**수정 파일**:
- `Page1TouchpadActions.kt` 내부 또는 별도 `SpecialKeysGroup.kt`

**참조 문서**:
- `docs/android/styleframe-page1.md` §2.2-A (Special Keys)
- `docs/android/component-design-guide-app.md` §2.3.1 (KeyboardKeyButton)

**검증**:
- [ ] 8개 키 2열 그리드 정상 렌더링
- [ ] 각 키 탭 시 HID KeyDown/KeyUp 전송
- [ ] Enter 롱프레스 반복 입력 동작
- [ ] 햅틱 피드백 동작

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
       val displayChips: List<String> // UI 표시용 ["Ctrl", "C"]
   )
   ```
   - **⚠️ 타입 통일**: 기존 `KeyboardKeyButton`, `ClickDetector.createKeyboardFrame()`이 `UByte`/`Set<UByte>` 사용 → `ShortcutDef`도 `UByte` 사용
   - **전송 로직**: `ClickDetector.createKeyboardFrame(activeModifierKeys, keyCode1)` 활용하여 프레임 생성 및 `ClickDetector.sendFrame()` 전송
3. 키칩 표기:
   - 키 조합을 칩 형태로 시각화 (예: `[Ctrl]` + `[C]`)
   - 색상: 기본 `#2196F3`, Disabled `#C2C2C2`
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

**검증**:
- [ ] 8개 단축키 2열 그리드 렌더링
- [ ] Ctrl+C 전송 시 순서 보장 (Ctrl↓→C↓→C↑→Ctrl↑)
- [ ] 150ms 디바운스 동작 (빠른 연타 무시)
- [ ] Alt+Tab 누름 유지 동작
- [ ] 키칩 시각화 정상 렌더링
- [ ] 접근성 리드아웃 확인

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

**검증**:
- [ ] 3개 매크로 버튼 Disabled 상태로 표시
- [ ] 탭 시 아무 반응 없음
- [ ] 시각적으로 비활성화 상태 명확

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
