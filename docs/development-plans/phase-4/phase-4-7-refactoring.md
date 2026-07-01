---
title: "BridgeOne Phase 4.7: Android 코드베이스 리팩토링"
description: "BridgeOne 프로젝트 Phase 4.7 - Phase 4.1~4.6 산출물의 외부 동작을 유지한 채 내부 구조 개선 (상수 정리·중복 제거·거대 파일 분해·MVVM ViewModel 도입)"
tags: ["android", "refactoring", "architecture", "mvvm", "code-quality"]
version: "v1.0"
owner: "Chatterbones"
updated: "2026-06-30"
---

# BridgeOne Phase 4.7: Android 코드베이스 리팩토링

**개발 기간**: 미정

**목표**: Phase 4.1~4.6에서 리팩토링 없이 누적 개발된 Android 코드의 외부 동작을 100% 유지한 채 내부 구조를 개선합니다. 테스트 안전망 구축, 상수 정리, 중복 제거, 거대 파일 분해, 상태 홀더 분리를 통해 코드 품질과 유지보수성을 높입니다.

**핵심 성과물**:
- 핵심 입력 로직(`DeltaCalculator`·매크로 시퀀싱·엣지 기하 등)의 단위 테스트 안전망
- 기능별로 분리된 `*Constants.kt` 파일군
- 중복 제거용 공통 유틸 (`HapticFeedbackHelper`)
- `TouchpadWrapper` / `StandardModePage` / `EdgeZoneEditorScreen` / `DynamicsCurveEditor` 분해
- 화면별 상태 홀더 (`StandardModePageState`, `EdgeZoneEditorState`)

**선행 조건**: Phase 4.6 완료

**불변 조건 (핵심)**: 모든 하위 Phase는 사용자가 체감하는 동작을 바꾸지 않습니다. 이것이 기능 변경과 리팩토링을 가르는 기준입니다. **이 불변 조건을 자동으로 보증하기 위해, 거대 파일을 손대기 전에 핵심 로직의 단위 테스트를 먼저 깝니다(Phase 4.7.2).** 각 하위 Phase 완료 시 `.\gradlew testDebugUnitTest`(해당 시) + `.\gradlew assembleDebug` 빌드 통과 + 해당 기능 수동 회귀 확인을 마친 뒤 다음 단계로 진행합니다.

**진행 순서**: 테스트 안전망 → 저위험(상수·중복·파일 이동) → 고위험(구조·상태 분리) 순서로 진행합니다. 각 하위 Phase는 독립적으로 빌드·검증 가능한 단위이므로, 중간에 중단해도 코드가 깨지지 않습니다. 회귀가 생기면 어느 단계가 원인인지 단위 테스트가 특정해 줍니다.

**Phase 4.17(성능 최적화)와의 관계**: 본 Phase는 **구조 개선**이 목적이며, 성능 향상은 부수적 결과입니다. 측정 기반의 성능 최적화는 모든 기능 완성 후 `phase-4-17-performance.md`에서 별도로 진행합니다. 일부 대상 코드가 겹치므로(예: 햅틱 호출, 제스처 루프), 본 Phase에서 만든 구조 위에서 4.17이 동작하게 됩니다.

---

## 현재 상태 분석

### 리팩토링 대상 (Phase 4.1~4.6 산출물)

| 파일 | 규모 | 주요 문제 |
|-----|------|---------|
| `ui/components/touchpad/EdgeZoneEditorScreen.kt` | 비대 | 다수 Composable + 다수 상태 변수가 한 함수에 집중, 편집/팝업/라벨/프리셋/매크로 책임 혼재 |
| `ui/components/touchpad/DynamicsCurveEditor.kt` | 비대 | 캔버스 렌더링 + 노드 드래그 제스처 + 템플릿/요약 생성 혼재 |
| `ui/components/TouchpadWrapper.kt` | 비대 | UI 렌더링 + 제스처 + 상태 계산(스크롤/관성/가속도) + 통신 + 햅틱 단일 Composable 집중 |
| `ui/pages/StandardModePage.kt` | 비대 | 다수 Composable 한 파일 정의, 페이지 간 공유 상태 집중 |
| `ui/common/ScrollConstants.kt` | 혼재 | 스크롤 상수와 엣지 스와이프 상수가 한 파일에 공존 |

### 중복 패턴

- 햅틱 진동 호출 블록이 `TouchpadWrapper.kt` 등 여러 위치에 반복
- 4개 Popup(`DynamicsPresetPopup` / `ModePresetPopup` / `EdgeZonePresetPopup` / `DpiAdjustPopup`)의 열기·닫기·dismiss 보일러플레이트 반복
- 흩어진 magic number (프레임 딜레이, 햅틱 amplitude 상한 등)

### 목표 아키텍처

화면 단위로 상태·로직을 **평범한 상태 홀더 클래스**(`*State`)로 이관하고 Composable은 렌더링과 이벤트 위임만 담당하는 단방향 데이터 흐름(UDF)으로 재구성합니다.

```
[Composable] --user event--> [상태 홀더] --state--> [Composable]
   (렌더링 전용)              (상태·비즈니스 로직)
```

> **상태 홀더 방식 결정 (AndroidX ViewModel 미채택)**: 본 Phase는 `androidx.lifecycle.ViewModel`이 아니라 **평범한 클래스 상태 홀더 + `remember`**를 표준으로 합니다. 근거:
> - 이 앱은 단일 모듈·화면 소수이고 의존성 그래프가 얕습니다(리포지토리 전부 `(context)` 1-인자). DI 프레임워크(Hilt) 비용 대비 이득이 적어 **도입하지 않습니다.**
> - 프로젝트가 이미 `SwipeFocusController`·`ModeHistoryStack`·`*Repository(context)`로 **수동 DI + 상태 홀더 컨벤션**을 확립해 두었습니다. 이 컨벤션과 일치시킵니다.
> - `TouchpadWrapper`는 페이저 안에서 `touchpadId`별 다중 인스턴스라, AndroidX ViewModel은 `viewModel(key=...)` 키 관리가 오히려 번거롭습니다. `remember(touchpadId)`가 현 구조에 더 맞습니다.
> - 예외: 구성 변경(화면 회전) 시 상태 보존이 꼭 필요한 화면에 한해 선택적으로 `androidx.lifecycle.ViewModel`(+`lifecycle-viewmodel-compose` 의존성) 채택을 검토합니다. 매니페스트 `screenOrientation` 확인 후 결정합니다.

---

## Phase 4.7.1: 상수 정리 및 분리 ✅

**목표**: 흩어지거나 혼재된 상수를 기능별 `*Constants.kt`로 정리합니다.

**작업 항목**:
1. `ScrollConstants.kt`에서 엣지 스와이프 관련 상수를 `EdgeSwipeConstants.kt`(신규)로 분리
2. 코드에 하드코딩된 magic number를 적절한 `*Constants.kt`로 이동 (상수 기본값 주석 정책 준수)
3. `PointerDynamicsConstants.kt`에 혼재된 곡선 템플릿 데이터를 `CustomPresetTemplates.kt`(신규)로 분리

**신규 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/common/EdgeSwipeConstants.kt`
- `src/android/app/src/main/java/com/bridgeone/app/ui/common/CustomPresetTemplates.kt`

**수정 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/common/ScrollConstants.kt` — `EdgeSwipeConstants` 블록 제거, 신규 상수 4개 추가
- `src/android/app/src/main/java/com/bridgeone/app/ui/common/PointerDynamicsConstants.kt` — `CUSTOM_PRESET_TEMPLATES` 블록 제거
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/TouchpadWrapper.kt` — 햅틱/프레임 magic number 상수화, import 추가
- `src/android/app/src/main/java/com/bridgeone/app/usb/UsbSerialManager.kt` — `port.read(buf, 100)` → 기존 `UsbConstants.USB_READ_TIMEOUT_MS` 사용
- `src/android/app/src/main/java/com/bridgeone/app/ui/utils/ClickDetector.kt` — `CLICK_PRESS_RELEASE_GAP_MS` 상수 추가

> **참고 (import 변경 불필요)**: `EdgeSwipeConstants`와 `CUSTOM_PRESET_TEMPLATES`는 같은 `com.bridgeone.app.ui.common` 패키지의 새 파일로 이동했으므로, 기존 호출부의 import 경로(`import com.bridgeone.app.ui.common.EdgeSwipeConstants`)가 그대로 유효합니다. 호출부 수정 0건.

**검증**:
- [x] `.\gradlew assembleDebug` 빌드 통과 (경고 없음)
- [x] 분리 전후 상수 값이 동일 (의도치 않은 값 변경 없음)
- [x] 추가/이동한 상수에 기본값 주석 존재

**수동 회귀 체크리스트** (실기기):

- [x] **무한 스크롤 진동** — 무한 스크롤 모드에서 손가락을 빠르게 드래그하면 강하게, 천천히 드래그하면 약하게 진동하는지 (`HAPTIC_AMPLITUDE_MAX/MIN` 상수화 영향)
- [x] **관성 진동** — 손가락을 떼고 관성이 진행되는 동안 속도 감소에 따라 진동도 점점 약해지다가 멈출 때 진동도 함께 멈추는지
- [x] **관성 감속 곡선** — 무한 스크롤 관성이 이전과 동일하게 부드럽게 감속하는지, 너무 갑자기 멈추거나 오래 지속되는 느낌이 없는지 (`INERTIA_FRAME_MS=16L` 상수화 영향)
- [x] **좌클릭** — 짧게 탭하면 좌클릭 1회만 전달되는지 (연속 이중 클릭 없음)
- [x] **우클릭** — 길게 탭하면 우클릭 컨텍스트 메뉴가 열리는지, 다시 우클릭해도 메뉴가 토글(닫혔다 열렸다)되지 않는지 (`CLICK_PRESS_RELEASE_GAP_MS=30L` 상수화 영향)
- [x] **엣지 스와이프 팝업** — 4방향(상·하·좌·우) 엣지에서 스와이프 시 팝업이 정상 등장하고, 엣지 쪽으로 되돌리면 취소되는지 (`EdgeSwipeConstants` 파일 분리 영향)
- [x] **엣지 존 힌트 오버레이** — 엣지 근처에 손가락을 가져가면 힌트 오버레이가 나타나고, 떼면 사라지는지
- [x] **엣지 스트립 에디터 (스와이프 레이어)** — 스와이프 레이어 설정 화면에서 존 경계 핸들 드래그로 존 크기 조절이 되는지 ⚠️ 기존 버그 확인됨: 스와이프 시 구분선 1칸 이동 후 조작 모드 강제 해제 + UI 무반응. 4.7.1 이전부터 존재하는 버그이며 별도 추적 필요.
- [x] **커스텀 프리셋 템플릿** — 커스텀 프리셋이 없는 초기 상태에서 템플릿 목록(균형·정밀 우선·빠른 이동·손 떨림 방지 4개)이 표시되는지 (`CUSTOM_PRESET_TEMPLATES` 파일 분리 영향)
- [x] **USB 수신** — 연결 후 커서 이동·클릭 명령이 ESP32-S3로 정상 전달되는지 (`UsbSerialManager` 수신 타임아웃 상수 교체 영향)

---

## Phase 4.7.2: 테스트 안전망 선행 구축

> **신설 (4.7.2의 기존 "공통 유틸 추출"은 4.7.3-B로 이동)**: 거대 파일을 손대기 전에 핵심 로직의 단위 테스트를 먼저 깝니다. 동작을 전혀 바꾸지 않으므로 가장 안전하며, 이후 모든 분해 단계에서 "무엇이 깨졌는지" 자동으로 특정하는 기반이 됩니다. 현재 입력 핵심인 `DeltaCalculator`에 테스트가 0개라는 점이 본 리팩토링의 가장 큰 위험 요소입니다.

**목표**: 외부 동작 100% 유지를 자동 검증할 단위 테스트 안전망을 구축합니다.

**작업 항목**:

### 4.7.2-A: 테스트 의존성 추가

> **⚠️ 계획 변경 (Robolectric 미채택 → 경량 구성 채택)**: 원안의 Robolectric/Truth/coroutines-test 대신 다음 최소 의존성만 추가. 테스트 대상 4종의 Android 의존성이 `android.util.Log`(ClickDetector)와 `org.json`(EdgeZoneJson) 둘뿐이며, `Offset`/`Density`/`Dp`는 순수 Compose 값 클래스라 JVM에서 그대로 로드됨. 기존 테스트 전부 순수 JUnit4(`assertEquals`) → 컨벤션 유지.

**실제 추가된 의존성**:
- `testOptions { unitTests { isReturnDefaultValues = true } }` (android 블록) — `android.util.Log` 미구현 호출을 0/null/false로 처리
- `org.json:json:20240303` (testImplementation) — EdgeZoneJson 라운드트립용 PC측 실구현
- `kotlinx-coroutines-test`: 4.7.2-B 대상에 코루틴 로직 없음 → 4.7.4-B에서 추가
- `org.robolectric:robolectric`: 미채택
- `com.google.truth`: 미채택 (기존 `assertEquals` 컨벤션 유지)

### 4.7.2-B: 이미 순수한 로직 즉시 테스트 (코드 수정 불요, 최우선)
- `src/test/.../ui/utils/DeltaCalculatorTest.kt` (신규):
  - `calculateDelta`, `applyDeadZone`(데드존 미만→0 / 초과→±127 clamp / 축 독립), `normalizeOnly`
  - `determineRightAngleAxis`(데드밴드 각도 경계 HORIZONTAL/VERTICAL/UNDECIDED), `applyRightAngleLock`
  - `applyPointerDynamics`(NONE/WINDOWS_EPP/LINEAR 분기 + maxMultiplier clamp), `interpolateCurve`(경계·보간), `applyCustomDynamics`(가속/감속 곡선 히스테리시스)
- `ClickDetectorTest.kt` (기존 보강): `detectClick`의 500ms/15dp 경계 4상한(LEFT/RIGHT/NO_CLICK) 명시
- `MacroTextEncoderTest.kt` (신규): 한/영 전환·자모 스텝 생성
- `EdgeZoneJsonTest.kt` (신규): config↔JSON 라운드트립 (직렬화 보존 → 프리셋 회귀 방지)

### 4.7.2-C: 추출 후 테스트할 로직 식별 (식별만, 추출은 해당 Phase에서)
현재 Composable에 묶여 테스트 불가능한 순수 로직. 이후 단계에서 추출하며 테스트를 함께 작성:
- **매크로 시퀀싱** — `StandardModePage.onSendMacro`(약 218~320행) → 4.7.4-B에서 `MacroFrameSequencer`로 추출
- **엣지 진입 판정** — `TouchpadWrapper`의 `detectEntryEdge`/`getInwardDistance`/`findNearestEdge`/`computeDirectTouchButtonRects`/`applyEdgeModeToggle`(1756~1919행). 이미 top-level `private fun`이라 visibility만 `internal`로 올리면 즉시 테스트 가능 → 4.7.3-A
- ~~**존 분할/병합** — `EdgeZoneEditorScreen`의 로컬 `fun splitInto`/`tryMergeWith`(393~427행). 클로저에 갇힘 → 4.7.5-A~~ ✅ **해소(4.7.5-A)**: `EdgeZoneEditorState`로 추출, `EdgeZoneEditorStateTest`로 고정

**검증**:
- [x] `.\gradlew testDebugUnitTest` 전체 그린 (103 tests, 0 fail)
- [x] 4.7.2-B 대상 로직이 모두 테스트로 고정됨 (이후 회귀 시 즉시 탐지)

---

## Phase 4.7.3: TouchpadWrapper 순수 함수·햅틱 분리

> **⚠️ Phase 4.17.3 / 4.17.4 영향**: 가이드라인 리컴포지션 최소화(4.17.3)와 제스처 루프 작업 제거(4.17.4)는 현재 `TouchpadWrapper` 제스처 루프 구조 위에서 진행됩니다. 4.17 진행 시 이 파일 구조를 먼저 확인할 것.

**목표**: `TouchpadWrapper`에서 떼어낼 가치가 분명한 순수 함수와 중복 로직을 분리합니다. 제스처 루프(`pointerInput`)는 코루틴·부작용·콜백이 본질적으로 강결합된 상태머신이라 분해하지 않습니다.

**작업 항목**:

### 4.7.3-A (저위험): 순수 기하/판정 함수 분리 ✅
- 신규 `ui/components/touchpad/EdgeGeometry.kt`: `detectEntryEdge`/`getInwardDistance`/`getAlongEdgePosition`/`findNearestEdge`/`computeDirectTouchButtonRects`/`applyEdgeModeToggle` 6개 함수를 `internal` top-level로 이동 (동작 동일)
- `TouchpadWrapper.kt`에서 함수 정의 삭제 → import 6개 추가 (`com.bridgeone.app.ui.components.touchpad.*`), 불필요 import (`CornerOverlap`) 제거
- 신규 `EdgeGeometryTest.kt`: 42 tests 그린 (4.7.2-C "엣지 진입 판정" 항목 해소)

### 4.7.3-B (중위험): 햅틱 단일화 (기존 4.7.2 항목 흡수) ✅
- 신규 `ui/common/HapticFeedbackHelper.kt`: 완전 중복이던 `vibrator.vibrate(...)` 2곳 (터치 드래그 중 / 관성 코루틴 중)을 `fun vibrateByVelocity(velocity: Float)`로 단일화. SDK O 가드·amplitude 계산·VibrationEffect 생성 흡수
- `TouchpadWrapper.kt`: `hapticHelper = remember(vibrator) { HapticFeedbackHelper(vibrator) }` 추가, 두 호출부를 `hapticHelper.vibrateByVelocity(speed/abs(velocity))`로 교체. 불필요 import (`VibrationEffect`, `INFINITE_SCROLL_HAPTIC_*` 4개) 제거

> **⚠️ Phase 4.17.2 영향**: 4.17.2(햅틱 호출 빈도 최적화)는 `HapticFeedbackHelper`에 시간 게이트(`HAPTIC_MIN_INTERVAL_MS`)를 추가하는 방식으로 진행합니다. 호출부가 헬퍼로 단일화되어 있으므로 한 곳만 수정하면 전체 적용됩니다.

**검증**:
- [x] `.\gradlew testDebugUnitTest`(EdgeGeometry 42 tests) + `.\gradlew assembleDebug` 통과 (4.7.3-A/B)
- [x] 커서 이동·클릭·드래그·스크롤·관성·엣지 스와이프·햅틱 수동 회귀 통과 (A/B 완료 시점)

---

## Phase 4.7.4: StandardModePage 분해 + 매크로 시퀀서 추출

**목표**: 한 파일에 정의된 13개 Composable을 페이지별로 분리하고, 매크로 시퀀싱 순수 로직을 추출하며, 공유 상태를 상태 홀더로 이관합니다.

**작업 항목**:

### 4.7.4-A (저위험): 페이지 Composable 파일 분리 + 재사용 컴포넌트 추출 ✅
`ui/pages/standard/` 신설. 페이지 래퍼와 재사용 컴포넌트를 구분해 분리합니다.

**페이지 래퍼** (4.15.4에서 `DynamicPage`로 대체될 임시 구조. 각 래퍼의 레이아웃 비율/배치 로직이 4.15.2 `DefaultPageTemplates` 데이터화의 기준이 됨):
- `Page1TouchpadActions.kt` / `Page2TestTouchpad.kt` / `Page3KeyboardPlaceholder.kt` / `Page4MinecraftPlaceholder.kt` / `Page5Settings.kt`(+`SettingsInputModeSection`/`SettingsEdgeInteractionModeSection`/`ZoneEditorEntryRow`/`SettingsButtonVisibilitySection`/`SettingsToggleRow`)
- `PageIndicator.kt` — 이미 `pageCount` 파라미터를 받으므로 단순 이동, 4.15 영향 없음
- `StandardModePrefs.kt`: SharedPreferences 헬퍼(`loadDpiLevel`/`saveDpiLevel`/`loadEdgeInteractionMode`/`saveEdgeInteractionMode`) + 상수(`PREF_NAME`/`KEY_DPI_LEVEL`/`KEY_EDGE_INTERACTION_MODE`) 이동 — 전역 설정이라 4.15 영향 없음

**재사용 컴포넌트** (`ui/pages/standard/components/` 신설, 가시성 `internal`):
- `ActionsPanel.kt`, `SpecialKeysGrid.kt`, `ShortcutsGrid.kt`, `MacrosPlaceholder.kt`
- 현재 이 4개는 파라미터가 거의 없는 정적 컴포넌트(modifier만). 4.15의 `ComponentCallbacks` 번들 강제 도입 금지 — 그건 4.15.3 작업

> **⚠️ Phase 4.15 영향**: 4.15.4가 `when(page % PAGE_COUNT)` 분기를 `DynamicPage`(데이터 기반 렌더링)로 통째 대체한다. 따라서 페이지 래퍼(`Page1TouchpadActions` 등)는 임시 구조이며 4.15.2 `DefaultPageTemplates`의 데이터화 기준으로만 쓰인다. 반면 내부 컴포넌트(`ActionsPanel`/`SpecialKeysGrid`/`ShortcutsGrid`)는 4.15.3 `ComponentRenderer`가 직접 디스패치해 재사용하므로, 페이지 의존을 끊은 standalone `internal` Composable로 추출한다(`ui/pages/standard/components/`). **확정 경로**: `com.bridgeone.app.ui.pages.standard.components`.

**4.7.4-A 신규 파일 (11개)**:
- `ui/pages/standard/StandardModePrefs.kt`
- `ui/pages/standard/PageIndicator.kt`
- `ui/pages/standard/Page1TouchpadActions.kt` (29 파라미터)
- `ui/pages/standard/Page2TestTouchpad.kt` (13 파라미터)
- `ui/pages/standard/Page3KeyboardPlaceholder.kt`
- `ui/pages/standard/Page4MinecraftPlaceholder.kt`
- `ui/pages/standard/Page5Settings.kt` (하위 섹션 composable 포함)
- `ui/pages/standard/components/ActionsPanel.kt`
- `ui/pages/standard/components/SpecialKeysGrid.kt`
- `ui/pages/standard/components/ShortcutsGrid.kt`
- `ui/pages/standard/components/MacrosPlaceholder.kt`

### 4.7.4-B (중위험): 매크로/단축키 시퀀싱 순수화 ✅
- 신규 `ui/common/MacroFrameSequencer.kt`: `object MacroFrameSequencer`
  - `data class TimedFrame(val frame: BridgeFrame, val delayAfterMs: Long)`
  - `fun buildMacro(steps: List<MacroStep>, stepDelayMs: Int): List<TimedFrame>`
  - `fun buildShortcut(modifierBits: Int, keyCodes: List<Int>): List<BridgeFrame>`
- `StandardModePage.kt`의 `onSendMacro`: `MacroFrameSequencer.buildMacro()`로 교체. `timedFrames.sumOf { delayAfterMs }`로 `estimatedMs` 산출 (기존 foldIndexed와 수학적 동등성 확인). 코루틴 내 프레임 순차 전송으로 간소화
- `onSendShortcut`: `MacroFrameSequencer.buildShortcut().forEach { sendFrame(it) }` 1줄로 교체
- 제거된 import: `MACRO_INTRA_STEP_PRESS_RELEASE_MS`, `MACRO_MAX_HELD_KEYS`, `MacroStepKind` (StandardModePage에서 불필요)
- 신규 `test/.../ui/common/MacroFrameSequencerTest.kt`: 20 테스트 (TAP repeat·홀드 합성·INTRA 딜레이·dangling hold·RELEASE 전체/특정·estimatedMs 동등성·buildShortcut) → 4.7.2-C "매크로 시퀀싱" 항목 해소

> **⚠️ Phase 4.15 영향**: `MacroFrameSequencer`는 4.15의 `MACRO_BUTTON` 렌더링이 그대로 재사용한다. API: `buildMacro(steps: List<MacroStep>, stepDelayMs: Int): List<TimedFrame>`. 스텝 입력이 `MacroStep`(페이지 비의존 데이터 타입)이므로 4.15 `MacroButtonCfg.steps`와 호환. 단, 매크로 스텝 JSON 직렬화 헬퍼 추출은 4.15.1 소관이며 4.7.4-B 범위 아님.

### 4.7.4-C (고위험): StandardModePageState 상태 홀더 ✅
- 신규 `ui/pages/StandardModePageState.kt` (평범한 클래스 상태 홀더, `remember`로 생성):
  - `var touchpadState by mutableStateOf(initialTouchpadState)` — public var. 히스토리 미기록 변경(팝업 confirm·곡선 편집)은 외부 직접 할당, 기록 필요 변경은 메서드 사용
  - `private val historyStack = ModeHistoryStack()`
  - `var heldMouseButtons by mutableStateOf(...)` — private set
  - `fun changeStateRecordingHistory(newState)` — 구 `recordingOnChange` (히스토리 push 후 교체)
  - `fun restorePrevious(): Boolean` — 구 `onRestorePrevious`. 스택 비면 false 반환 (토스트는 호출부)
  - `fun toggleMouseHold(button, mode): UByte` — 구 `onMouseHoldToggle`의 상태 전이. 전송할 buttons 바이트 반환 (sendFrame·토스트는 호출부)
- **사이드이펙트 격리**: 상태 홀더는 순수 상태 전이만 보유. `sendFrame`/토스트/`MacroOverlay`는 전부 Composable 콜백에 잔류 (4.7.4-B 철학 일관)
- **계획과 다르게 구현**: `standardAssignments`/`standardButtonVisibility` 두 Map은 상태 홀더로 옮기지 **않고** Composable의 `remember`에 그대로 잔류. 두 Map은 각자의 repo(`assignmentRepo`/`buttonVisibilityRepo`)·저장 `LaunchedEffect`와 결합돼 있어 상태 홀더로 옮기면 context·repo 의존이 따라와 격리가 깨진다. Composable 잔류가 "정교한 API 금지 + 4.15.4 제거 용이성" 목표에 더 부합
- 호출부 `StandardModePage.kt`: `touchpadState` 직접 참조 18곳을 `pageState.touchpadState`로 교체. 불필요해진 import(`BridgeFrame`/`ModeHistoryStack`) 제거. `StandardModePage.kt`는 페이저·콜백 배선·팝업 오버레이만 잔류
- 신규 `test/.../ui/pages/StandardModePageStateTest.kt`: 14 테스트 (changeStateRecordingHistory push 조건·restorePrevious edgeInteractionMode 유지·연속 복원 재push 없음·toggleMouseHold 비트 OR/RELEASE/TOGGLE)

> **⚠️ Phase 4.15 영향**: `StandardModePageState`는 4.15.2가 `pages: List<PageLayout>` 상태와 debounce 저장 `LaunchedEffect`를 가산(additive)으로 추가한다. `standardAssignments`/`standardButtonVisibility` 맵은 **상태 홀더가 아니라 `StandardModePage.kt` Composable에 잔류** 중이며, 4.15.4에서 제거되어 `PlacedComponent` config로 흡수된다. `touchpadState`는 `pageState.touchpadState`(public var)로 노출돼 있어 4.15.4의 동적 렌더러도 동일하게 읽고 직접 할당 가능.

**검증** (A/B/C 완료):
- [x] `.\gradlew testDebugUnitTest`(MacroFrameSequencerTest 20 + StandardModePageStateTest 14 tests) + `.\gradlew assembleDebug` 통과 (A/B/C 각 단계 빌드·테스트 그린, 신규 경고 없음)

#### 수동 회귀 체크리스트 (실기기)

**4.7.4-A: 페이지 분리 회귀**

페이지 전환 / 인디케이터:
- [x] Page 1→2→3→4→5 순차 스와이프 전환 정상 (5→1 wrap도 확인)
- [x] 하단 인디케이터 도트가 현재 페이지에 맞게 이동하는지 (worm 애니메이션)
- [x] 인디케이터 도트 탭으로 페이지 점프가 정상 동작하는지

Page 1 렌더링 (ActionsPanel 분리 영향):
- [x] 우측 패널 "특수 키" 섹션: Esc·Tab·Enter·⌫·Del·Space·Home·End 버튼 표시 및 탭 시 PC에 해당 키 전달 확인
- [x] 우측 패널 "단축키" 섹션: Ctrl+C·Ctrl+V·Ctrl+S·Ctrl+Z 등 8개 버튼 표시 및 탭 시 실행 확인
- [x] 우측 패널 "매크로" 섹션 표시 정상

SharedPreferences 저장/복원 (StandardModePrefs 분리 영향):
- [x] 앱 재시작 후 DPI 레벨이 이전 설정값으로 복원되는지
- [x] 앱 재시작 후 엣지 조작 방식(일반/스와이프)이 이전 설정으로 복원되는지

Page 5 설정 렌더링:
- [x] 설정 페이지 전체 항목이 정상 표시 (입력 방식·엣지 조작 방식·버튼 표시·TTS 슬라이더 등)
- [x] TTS 속도 슬라이더 핸들이 정상 렌더링되는지 (BoxWithConstraints offset 수정 영향)

**4.7.4-B: 매크로 시퀀서 회귀**

단축키 (buildShortcut 교체 영향):
- [x] 특수키 탭(예: Esc) → PC에서 해당 키 동작 확인 (press+release 2프레임 전송)
- [x] 단축키 탭(예: Ctrl+C) → PC에서 복사 동작 확인 (modifier+key press, 전체 release)

매크로 실행 흐름:
- [x] 매크로 실행 시 스크림 오버레이(화면 어두워짐)가 나타나고 실행 완료 후 사라지는지
- [x] PROGRESS 토스트 "매크로 실행 중"이 스크림과 함께 등장하고 완료 후 사라지는지
- [x] 매크로 표시 시간이 너무 짧거나(400ms 최소 보장) 너무 길지 않은지

매크로 스텝 종류별 (엣지 존에 매크로 할당 후 트리거):
- [x] TAP repeat 매크로 (예: repeatCount=3): 키가 3회 연속 전달되는지
- [x] HOLD + TAP 조합 매크로 (예: Ctrl HOLD → C TAP → RELEASE): Ctrl+C 동작 확인
- [x] HOLD만으로 끝나는 매크로: 매크로 종료 후 PC에서 키가 눌린 채 남지 않는지 (dangling 해제 확인 — 텍스트 에디터에서 키 누름 상태 해제 확인)

**4.7.4-C: 상태 홀더 회귀**

터치패드 상태 동기화 (touchpadState → pageState.touchpadState 이관):
- [ ] 페이지 1/2 간 전환 시 터치패드 모드(클릭/이동/스크롤)·DPI가 동일하게 공유·유지되는지 (제어 버튼으로 바꾼 모드가 페이지 전환 후에도 유지)
- [x] DPI 세밀 조절 팝업 confirm 후 값이 적용되는지 (직접 할당 경로)
- [x] 다이나믹스 프리셋 / 모드 프리셋 팝업 confirm 후 적용되는지 (직접 할당 경로)
- [x] 곡선 편집기로 커스텀 프리셋 저장 시 즉시 선택되는지 (직접 할당 경로)

모드 이력 / 되돌리기 (ModeHistoryStack 이관):
- [x] 모드를 여러 번 바꾼 뒤 "되돌리기"로 직전 상태가 순서대로 복원되는지
- [x] 이력이 없을 때 되돌리기 시 "이전 모드 및 세팅이 없습니다" 토스트가 뜨는지
- [x] 되돌리기로 복원된 상태가 다시 이력에 쌓이지 않는지 (연속 되돌리기 정상)

마우스 홀드 (heldMouseButtons 이관):
- [x] 좌/우/중간 클릭 홀드 토글 시 "○클릭 홀드 ON/OFF" 토스트 + 실제 버튼 유지 동작 확인
- [ ] 홀드 ON 상태에서 앱 종료(또는 화면 이탈) 시 PC에서 버튼이 눌린 채 남지 않는지 (DisposableEffect 해제)

---

## Phase 4.7.5: EdgeZoneEditorScreen 분해 (최대 규모)

> **⚠️ Phase 4.7.1 변경사항**: `EdgeSwipeConstants`가 `ScrollConstants.kt`에서 `EdgeSwipeConstants.kt`(신규)로 분리됨. 같은 패키지(`ui/common`)라서 `EdgeZoneEditorScreen.kt`의 `import com.bridgeone.app.ui.common.EdgeSwipeConstants`는 수정 불필요.

**목표**: 8,828줄 / 단일 함수 약 2,840줄 / 상태 157개의 파일을 `ui/components/touchpad/edgezone/` 하위 단위로 분할합니다. **SWIPE/NORMAL 분기가 함수 전체에 산재**한 것이 최대 난점이라, 4개 서브단계로 나눠 각 단계가 독립 빌드되도록 합니다.

**분해 단위** (실제 섹션 주석·행 번호 기반):

| 신규 파일 | 책임 | 추출 출처(행) |
|---|---|---|
| `EdgeZoneEditorState.kt` | 상태 홀더(평범 클래스): `workConfig`/`selectedZone`/`currentPresetId`/`undoStack`+`pushUndo`, `splitInto`/`tryMergeWith`/`deleteZone`/존 비율 프리셋 적용 등 순수 config 변환 + Undo | 318~427, 762~807 |
| `EdgeZoneActionResolver.kt` | 순수 함수: `domainOf`/`actionEquals`/`describeUndoStep`/`migrateDynamicsIndicesAfterDelete`/`ratioPresetsFor` | 3086, 4429, 7855, 8023, 8052 |
| `ZoneCanvasSection.kt` | 캔버스 + SWIPE hit 오버레이 | 1059~1240 |
| `ZoneEditPanel.kt` | 선택 존 편집 패널 컨테이너 (영역 비율~표시 설정 슬롯 배치) | 1241~2181 |
| `ZoneActionPicker.kt` | 액션 선택 + `ActionDomainPicker` 폴더 트리/그리드 | 1758~1906, 3124~4225 |
| `ZoneLabelIconColorEditor.kt` | 표시 설정(라벨 IME/아이콘/컬러) + 라벨 커서 애니메이션 | 1907~2117, 361~389 |
| `ZoneRotationEditor.kt` | `RotationEditor` + 후보 편집 hoist 상태 | 2118~2181, 352~360, 8094~ |
| `ZonePopups.kt` | `ZoneActionPopup`(Initial/Split/Merge/Delete) when 분기 | 1481~1706 |
| `MacroEditorPopup.kt` / `ShortcutEditorPopup.kt` | 이미 독립 `private fun` — 별도 파일 이동(+`MacroDelaySliderRow`) | 4457~5398, 5417~ |
| `ZonePresetPopups.kt` / `UndoHistorySwipePopup.kt` / `RatioPresetSwipePopup.kt` | 프리셋/Undo/비율 스와이프 팝업 | 2500~2552, 7885~8022 |

**SWIPE/NORMAL 분기 처리 (핵심 설계 결정)**: 함수 전체에 흩어진 `if (inputMode == InputMode.SWIPE)`를 정리합니다.
- (권장) `InputMode`를 `CompositionLocal`로 제공 + 분기를 각 하위 Composable 내부로 가둠. SWIPE 전용 오버레이(2233~2810행: 힌트/툴팁/서랍/컬러피커/제스처오버레이)는 `SwipeOverlayLayer.kt`로, NORMAL 전용 바텀시트(2818~2880행)는 `NormalSheetLayer.kt`로 모음. 분기가 "레이어 선택" 한 곳으로 수렴
- (대안·비권장) `EdgeZoneEditorScreenSwipe`/`...Normal` 완전 분리 → 공통 편집 패널 중복 부담 큼

**서브단계** (각 독립 빌드):
1. **4.7.5-A** ✅: 순수 함수(`EdgeZoneActionResolver`) + config 변환·Undo(`EdgeZoneEditorState`) 추출 + 테스트. 4.7.2-C 항목 해소
2. **4.7.5-B** ✅ (저위험): 이미 독립적인 `private fun` Composable(Macro/Shortcut/Rotation/팝업류)을 파일 이동만 (시그니처 유지)
3. **4.7.5-C** ✅ (저위험 범위로 축소): `ActionDomainPicker`(+전속 헬퍼 4개)를 `ZoneActionPicker.kt`로 순수 파일 이동(C-1) + 미사용 데드코드 제거(C-2). **편집 패널 섹션(영역비율/표시설정/캔버스/ZonePopups) 추출은 4.7.5-D로 이월** — 30+개 클로저 캡처를 파라미터로 hoist해야 해 byte-identical이 깨지므로, `CompositionLocal` 선행 도입(4.7.5-D) 후 진행하는 것이 안전·구조적으로 옳음
4. **4.7.5-D** ✅: `LocalInputMode` CompositionLocal 도입 + 오버레이 레이어 분리 + 편집 패널 섹션 분리(4.7.5-C 이월분). 저위험(D-1·D-3·D-4) → 고위험(D-2·D-5) 순으로 진행, 각 독립 커밋. **byte-identical 미보장(캡처 hoist)** → 실기기 회귀 필요

> **⚠️ 4.7.5-A 완료 기록 (4.7.5-B/C/D 전제)**:
> - **신규 `EdgeZoneActionResolver.kt`** (`object` + 순수 함수 5개 `domainOf`/`actionEquals`/`describeUndoStep`/`migrateDynamicsIndicesAfterDelete`/`ratioPresetsFor`). `ActionDomain` enum도 이 파일로 이동(`internal` top-level, 같은 패키지라 호출부 import 불필요). 호출부는 `EdgeZoneActionResolver.도메인함수(...)` prefix로 호출.
> - **신규 `EdgeZoneEditorState.kt`** (평범한 클래스 + `mutableStateOf`, `StandardModePageState` 컨벤션). `workConfig`/`selectedZone`/`currentPresetId`/`undoStack` 상태 + `pushUndo`/`splitInto`/`tryMergeWith`/`deleteZone`/`applyRatioPreset` 메서드 보유.
> - **상태 hoist 방식**: 홀더가 `workConfigState` 등 `MutableState`를 노출하고, `EdgeZoneEditorScreen`은 기존 지역 변수명을 `var workConfig by state.workConfigState` **위임**으로 유지 → 함수 내 113곳 참조를 그대로 둠. **4.7.5-C에서 패널 Composable로 상태를 넘길 때는 `state` 인스턴스(또는 `state.xxxState`/hoist 콜백)를 파라미터로 전달**한다.
> - **`zonePopup` 책임 이동**: `splitInto`/`tryMergeWith`가 갖던 `zonePopup = ZoneActionPopup.None` 리셋을 홀더에서 제거하고 **호출부(Composable)**로 옮김(홀더는 UI-free). `splitInto`/`tryMergeWith`는 적용 성공 여부를 `Boolean`으로 반환하고, 호출부는 **성공 시에만** 팝업을 닫는다(원본 동작 보존: 비인접 탭 시 병합 모드 유지). 4.7.5-C에서 이 패턴 유지.
> - **신규 테스트**: `EdgeZoneActionResolverTest.kt` / `EdgeZoneEditorStateTest.kt` (순수 JUnit4).

> **⚠️ 4.7.5-B 완료 기록 (4.7.5-C/D 전제)**:
> - **신규 파일 5개** (모두 `touchpad/` 패키지 직하 — `edgezone/` 하위폴더 미사용, 4.7.5-A 선례 따름):
>   - `ShortcutEditorPopup.kt` (`ShortcutEditorPopup`)
>   - `MacroEditorPopup.kt` (`MacroEditorPopup` + `MacroDelaySliderRow`(private 유지) + enum `MacroStepEditMode`/`MacroEditorPage`/`MacroKbTarget` + `macroButtonLabel`)
>   - `UndoHistorySwipePopup.kt` (`UndoHistorySwipePopup`)
>   - `RatioPresetSwipePopup.kt` (`RatioPresetSwipePopup` + `MiniRatioBar`)
>   - `ZoneRotationEditor.kt` (`RotationEditor`)
> - **가시성 승격**: 이동한 top-level 함수는 메인 파일에서 cross-file 호출되므로 `private fun`→`internal fun`. 빌드 중 메인 함수도 `MiniRatioBar`/`macroButtonLabel`을 직접 호출함이 드러나 두 함수도 `internal`로 올림(나머지 helper enum·`MacroDelaySliderRow`는 같은 파일 전용이라 `private` 유지).
> - **EdgeZoneEditorScreen.kt 잔류 심볼 승격(private→internal)**: `CUSTOM_SLIDER_TRACK_HEIGHT_DP`/`CUSTOM_SLIDER_LINE_WIDTH_DP`(잔류 `ActionDomainPicker`+이동 함수들이 공유), `ActionDomainPicker`(메인 함수+이동된 `RotationEditor`가 호출), `CustomPresetTarget`(internal이 된 `ActionDomainPicker`의 파라미터 타입이라 노출 에러 방지). **4.7.5-C에서 `ActionDomainPicker`를 `ZoneActionPicker.kt`로 옮길 때 이 4종 심볼을 그대로 internal로 가져가면 됨** (`RotationEditor`·`MacroEditorPopup`·메인이 참조 중).
> - **미사용 import 정리**: 이동으로 메인 파일에서 안 쓰게 된 import 50개 제거(아이콘·`MACRO_*`·`MOD_BIT_*`·IME·컬러피커 등). 새 파일은 사용 import만 포함.
> - **검증**: `.\gradlew assembleDebug` + `testDebugUnitTest` 통과, 신규 경고 없음(잔존 경고 deprecated 아이콘·tautological 체크는 이동 코드에 원래 있던 것). git HEAD 원본 대비 정밀 대조 통과 — 이동 코드는 가시성 키워드 외 바이트 동일, 메인 파일은 import 50개 차감 + 가시성 4건 외 변경 0. 실기기 회귀 통과(숏컷/매크로 편집 팝업·회전 트리거·Undo·비율 프리셋 스와이프 팝업 SWIPE/NORMAL 동작 동일).

> **⚠️ 4.7.5-C 완료 기록 (4.7.5-D 전제)**:
> - **신규 `ZoneActionPicker.kt`** (`touchpad/` 패키지 직하): `ActionDomainPicker`(`internal fun`, ~1,100줄) + 전속 private 헬퍼 `ActionOption`/`ActionTreeNode`/`DomainGroup`/`DEFAULT_DOMAIN_GROUPS`를 `EdgeZoneEditorScreen.kt`에서 순수 이동. 함수 시그니처·본문 **byte-identical**(git HEAD 대조 통과, 가시성 변경 없음). 같은 패키지라 호출부(메인 `ActionDomainPicker(...)` 호출) import 수정 0건. 4.7.5-B에서 internal로 올려둔 공유 심볼(`CUSTOM_SLIDER_*`·`CustomPresetTarget`·`ActionDomain` 등)은 같은 패키지 cross-file 접근이라 그대로 동작.
> - **import 정리**: 이동으로 메인에서 picker 전용이 된 import 31개 제거(grid·icon·gesture 등). 새 파일은 사용 import만 포함. **주의**: `getValue`/`setValue`는 `by` 위임에 암묵적으로 쓰여 코드에 literal로 등장하지 않으므로, 텍스트 기반 미사용 판정에서 false-positive가 난다 — 두 파일 모두 **반드시 유지**(4.7.5-D 추출 시 동일 주의).
> - **데드코드 제거(C-2)**: 미참조 `_RemovedPlaceholder_PresetEditDeleteMenu_UNUSED`(192줄) 삭제. 이로써 미사용이 된 import 2개(`Delete` 아이콘·`SwipeFocusController`) 정리.
> - **결과**: `EdgeZoneEditorScreen.kt` **4,260 → 2,877줄**. 메인 git diff는 **0 추가 / 1,383 삭제**(순수 삭제, 코드 변경 0). `.\gradlew assembleDebug`+`testDebugUnitTest` 통과, 신규 경고 없음(잔존 경고는 deprecated 아이콘·tautological 체크로 원래 있던 것이 코드와 함께 이동/줄번호 시프트).
> - **4.7.5-D 전제**: 잔류 편집 패널 섹션(영역비율·표시설정·SWIPE 캔버스 hit 오버레이·`ZoneActionPopup` when 분기)은 메인 함수 지역 상태를 30+개 캡처 중. 4.7.5-D에서 `InputMode` 등을 `CompositionLocal`로 제공해 캡처를 줄인 뒤 섹션 Composable로 분리한다. `state` 인스턴스 전달 방식(4.7.5-A 기록)과 병행.

> **⚠️ 4.7.5-D 완료 기록 (4.7.6 이후 전제)**:
> - **신규 `LocalInputMode`** (`ui/common/InputMode.kt`, `compositionLocalOf { InputMode.NORMAL }`). 루트 provider(`EdgeZoneEditorScreen` 657행)에 `LocalSwipeFocusController`와 병합 제공. 신규 추출 섹션은 `LocalInputMode.current`로 모드 판별. **기존 `inputMode: InputMode` 파라미터를 받던 `RotationEditor`/`MacroEditorPopup`/`ActionDomainPicker`는 미마이그레이션**(이미 안정·리터럴 전달처 있음, 향후 별도 작업 후보).
> - **신규 파일 6개** (모두 `touchpad/` 직하):
>   - `ZoneCanvasHitOverlay.kt` (D-3): SWIPE 캔버스 hit 영역. 값+콜백.
>   - `ZoneDisplaySettingSection.kt` (D-4): 표시 설정(아이콘/컬러/라벨/되돌리기). 값+콜백.
>   - `NormalSheetLayer.kt` (D-2a): NORMAL 전용 바텀시트 4개. 값+콜백. 공유 `iconSheetState` 인스턴스 전달.
>   - `EdgeZoneOverlayUiState.kt` (D-2b): **오버레이/팝업 UI 상태 홀더**(46종 `MutableState`). `EdgeZoneEditorState`(config·Undo)와 분리된 UI 사이드이펙트 상태 전용. `internal class`(`CustomPresetTarget` 노출 회피). 화면·레이어가 `var x by overlayUi.xState` 위임으로 공유.
>   - `EdgeZoneOverlayLayer.kt` (D-2b): 메인 Box 안 오버레이 체인(~544줄). **`BoxScope` 확장 + 무조건 호출**. 모드 무관 프리셋 팝업·NORMAL 매크로 편집기가 SWIPE 체인 중간에 섞여 있어 호출부 모드 가드 없이 내부 `inputMode` 분기 유지(byte-identical). 다이나믹스 편집기·미저장 다이얼로그는 화면 잔류.
>   - `ZoneRatioSection.kt` (D-5): 영역 비율(프리셋 메뉴·스트립 에디터·`ZoneActionPopup` 팝업). `state`+`zonePopupState`(MutableState) 위임, `stripBounds` 내부 이동.
> - **`ZoneActionPopup` 가시성 승격**: `private sealed class`→`internal`(`ZoneRatioSection`이 cross-file 참조 + `zonePopupState: MutableState<ZoneActionPopup>` 파라미터).
> - **파라미터 축소 전략(사용자 결정)**: 거대 오버레이 레이어(~47개 캡처)는 개별 파라미터 대신 `EdgeZoneOverlayUiState` 홀더 1개로 묶어 전달. 화면 선언부는 `var x by remember{mutableStateOf}` → `var x by overlayUi.xState` 위임으로 변경(사용처 코드 무변경). `localCustomPresets`는 `remember(customPresets)` 키를 가져 홀더 제외·화면 소유(값 전달).
> - **미사용 import 정리**: D-4에서 3개(`CircleShape`/`TextOverflow`/`ColorCodec`), D-2b에서 11개(애니메이션 9·`SwipeGestureLayer`·`SwipeMode`), D-5에서 8개(`IntRect`/`IntSize`/`IntOffset`/`LayoutDirection`/`PopupPositionProvider`/`positionInWindow`/`wrapContentWidth`/`BarChart`). `mutableIntStateOf`도 제거(holder가 `mutableStateOf` 사용). `getValue`/`setValue`는 위임에 암묵 사용이라 유지.
> - **결과**: `EdgeZoneEditorScreen.kt` **2,877 → 1,591줄**(D 누적 순감 ~1,286줄). 커밋 4건(c8280bd 저위험 묶음 D-1·D-3·D-4, e46e711 D-2a, cd439fd D-2b, 02b68c8 D-5). `.\gradlew assembleDebug`+`testDebugUnitTest` 통과, 신규 경고 없음(잔존 경고는 기존 deprecated `VIBRATOR_SERVICE`·`CallMerge`).
> - **⚠️ 동작 동일성 미입증**: byte-identical을 의도적으로 깸(캡처를 파라미터/위임으로 hoist). 빌드·단위 테스트는 통과하나 와이어링 오류를 컴파일러가 전부 잡지 못함 → **실기기 회귀 필수**(검증 체크리스트 참조).

> **주의**: 4.7.1 회귀목록의 ⚠️ 기존 스트립 에디터 스와이프 버그(구분선 1칸 이동 후 무반응)는 본 리팩토링 범위 밖입니다. 동작 동일성만 유지하고 별도 추적합니다.

> **⚠️ 기존 버그 (리팩토링 이전부터 존재, 별도 추적)**: 4.7.5-A 수동 회귀 중 확인됨. 동작 동일성 검증 결과 git HEAD(리팩토링 전)와 코드 경로가 동일하여 본 리팩토링이 원인이 아님이 확정됨. 별도 기능 수정 작업 필요:
> - **NORMAL 존 병합 미동작**: 존 롱프레스 → 3버튼(병합/분할/삭제) → "병합" 진입 후 인접 존을 탭해도 병합되지 않음
> - **SWIPE 레이어 3버튼 미표시**: SWIPE 모드에서 존 포커스 상태에서 롱프레스·더블탭으로 병합/분할/삭제 3버튼(`ZoneActionPopup.Initial`)을 띄울 방법이 없음

**검증**:
- [x] `.\gradlew testDebugUnitTest`(EdgeZoneActionResolver/EdgeZoneEditorState) + `.\gradlew assembleDebug` 통과 (4.7.5-A, 신규 경고 없음)
- [x] **4.7.5-C**: `ZoneActionPicker.kt` 분리(C-1) + 데드코드 제거(C-2) 후 `.\gradlew assembleDebug`+`testDebugUnitTest` 통과, 신규 경고 없음. byte-identical 대조 통과(메인 0 추가/1,383 삭제) → 동작 보존은 구조적으로 보장. 실기기 회귀(액션 선택 폴더 트리/그리드·도메인 네비·커스텀 프리셋 추가/수정/삭제·SWIPE/NORMAL)는 사용자가 실기기 연결 시 최종 확인(코드 무변경이라 형식적 확인)
- [x] **4.7.5-D**: `.\gradlew assembleDebug`+`testDebugUnitTest` 통과, 신규 경고 없음. 신규 파일 6개 + `LocalInputMode`. byte-identical **미보장**(캡처 hoist)이었으나 아래 실기기 회귀로 동작 동일성 입증 완료
  - [x] **(실기기)** D-3: SWIPE 캔버스 4개 엣지 존 포커스 순회·disabled 엣지 제외·코너 클리핑 동일
  - [x] **(실기기)** D-4: 아이콘/컬러/라벨(커서 깜빡임)·auto 뱃지·되돌리기 동일(두 모드)
  - [x] **(실기기)** D-2a: NORMAL 아이콘/컬러 시트 + 후보 시트 + 공유 `iconSheetState` 동작 동일
  - [x] **(실기기)** D-2b: SWIPE 전체 오버레이 — StripBoundary 힌트·아이콘버튼 툴팁·아이콘 서랍(main/candidate/shortcut) stage 네비·컬러피커 long-press back·Undo 드롭다운(스와이프 투과)·프리셋 팝업 back-stack·숏컷/매크로 에디터 키보드 양보·`SwipeGestureLayer` long-press dispatch 전 분기. NORMAL 미니 툴팁·매크로 편집기 Popup. 프리셋 팝업(양 모드)
  - [x] **(실기기)** D-5: 비율 프리셋 메뉴(NORMAL 드롭다운/SWIPE 팝업)·스트립 드래그·병합/분할/삭제 팝업 상태머신(SWIPE scope push/pop·포커스 복원)
- [x] 4개 엣지 존 시각화·드래그 편집 동작 동일 *(4.7.5-A 실기기 검증 완료)*
- [x] 존 분할/병합, 액션·아이콘·컬러·숏컷 할당 동작 동일 *(4.7.5-A 실기기 검증 완료)*
- [x] 회전 트리거, 라벨 편집(IME), 프리셋 저장/로드 동작 동일 *(4.7.5-A 실기기 검증 완료)*
- [x] Undo/Redo 동작 동일 *(4.7.5-A 실기기 검증 완료)*
- [x] SWIPE/NORMAL 두 모드 UI 모두 동작 동일 *(4.7.5-A 실기기 검증 완료)*
  - 단, 4.7.1부터 추적 중인 기존 스트립 에디터 스와이프 버그(구분선 1칸 이동 후 무반응)는 본 리팩토링 범위 밖 (동작 동일성 유지 확인)

---

## Phase 4.7.6: DynamicsCurveEditor 분해 (난도 최저)

**목표**: 곡선 편집기를 렌더링·카드·노드편집 단위로 분리합니다. 이미 내부 서브 Composable이 잘 나뉘어 있어 4개 대상 중 난도가 가장 낮습니다.

**원안 작업 항목**:
1. `CurveGraphCanvas.kt`: 곡선 캔버스 렌더링 + 노드 드래그 `pointerInput` 분리
2. `NodeEditPanel.kt`: `NodeEditGrid`/`NodeEditHeader`/`EditorActionGrid`/십자패드
3. `EditorCards.kt`: `MetaCard`/`CurveCard`/`ActionCard`/`EditorHeader`
4. `CurveSummary.kt`: 곡선 요약(자연어)·`templateAccent`·step 정밀도 (순수) + 테스트
5. 필요 시 `CurveEditorState.kt`: `GridContext` 네비 상태 홀더

> **⚠️ 4.7.6 완료 기록 (계획 대비 변경)**:
> - **패키지 위치(사용자 결정)**: `curve/` 하위 패키지 대신 `ui/components/touchpad/` **직하**(4.7.5-A~D 선례 일관). 같은 패키지라 `internal` 승격만으로 cross-file 접근, 호출부(`StandardModePage.kt`·`EdgeZoneEditorScreen.kt`의 `DynamicsCurveEditor(...)` 호출) import 변경 0.
> - **제스처 분리 안 함(사용자 결정)**: 노드 선택 제스처(`pointerInput`)는 메인 함수 지역 상태(`selectedNodeIndex`/`gridContext`/`hoveredSlot` 등)에 강결합 → **메인 잔류**, 순수 Canvas 렌더만 분리. 4.7.3의 "강결합 제스처 루프는 분해 안 함" 원칙과 일치. 메인 함수 본문 byte-identical 보존.
> - **신규 파일 6개**(모두 `touchpad/` 직하):
>   - `CurveEditorTheme.kt`: 공유 프리미티브 일괄 — 색상 7개(`BG`/`SURFACE`/`ACCENT_BLUE`/`ACCENT_ORANGE`/`ACCENT_RED`/`GRID_COLOR`/`LABEL_COLOR`) + `ActionSlot`/`SlotStyle` + `cellBgColor`/`cellBorder`/`AnimatedCellBox`. 전부 `internal`(여러 분해 파일이 공유).
>   - `CurveGraphCanvas.kt`: `CurveGraphCanvas`(순수 렌더) + `drawGrid`/`drawCurve`/`drawAxisLabels`.
>   - `EditorCards.kt`: `MetaCard`/`CurveCard`/`EditorHeader`.
>   - `NodeEditPanel.kt`: `EditorActionGrid`/`Slot9Card`/`NodeEditHeader`/`ActiveTabLabel`/`NodeEditGrid`(십자패드 포함).
>   - `CurveTemplatePicker.kt`: `TemplatePickerContent`(internal) + `TemplateSquareCard`/`HintLine`/`TemplatePhase`/`TemplateAccent`/`TEMPLATE_ACCENTS`/`templateAccent`(같은 파일 전용 `private`).
>   - `CurveEditorOverlays.kt`: `SaveConfirmOverlay`/`NodeStepScalePickerOverlay`.
> - **`CurveSummary.kt` 미생성**: 원안의 "곡선 요약(자연어)"인 `describeCurves`/`CurveDescription`은 **이미 `ui/common/PointerDynamicsConstants.kt`에 추출돼 있음** → 신규 파일 불필요, 테스트만 추가. `templateAccent`는 단순 색상 매핑이라 `CurveTemplatePicker.kt`에 동거.
> - **`CurveEditorState.kt` 미생성**: 함수 분리만으로 메인 파일이 924줄로 축소돼 상태 홀더 추출 불요(`GridContext`/`EditorScreen`/슬롯 상수는 메인 잔류).
> - **데드코드 `ActionCard` 제거**: 정의만 있고 호출처 0(4.7.5-C 선례).
> - **가시성 승격(`private`→`internal`)**: cross-file 참조용 — `FIELD_NAME`/`FIELD_DESC`(MetaCard), `resolveSlot`+`SAVE_CONFIRM_ROW_SLOTS`/`SAVE_CONFIRM_START_ROW`(SaveConfirmOverlay·테스트). 그 외 메인 전용 슬롯 상수·`EditorScreen`/`GridContext`는 `private` 유지.
> - **신규 테스트 2개**: `ResolveSlotTest.kt`(슬롯 매핑 — 행/열 이동·편향·coerce 경계 9 케이스), `CurveDescriptionTest.kt`(`describeCurves` 보간·비대칭 라벨·요약 문구 4 케이스).
> - **결과**: `DynamicsCurveEditor.kt` **2,831 → 924줄**. git HEAD 대비 메인 파일 diff = **5 추가 / 1,943 삭제**(5 추가는 가시성 승격 5줄, 나머지 순삭제) → 동작 보존 구조적 보장. `.\gradlew assembleDebug`+`testDebugUnitTest` 통과, 신규 경고 없음(잔존 경고는 기존 deprecated 아이콘·tautological 체크).

**검증**:
- [x] `.\gradlew testDebugUnitTest`(ResolveSlotTest 9 + CurveDescriptionTest 4) + `.\gradlew assembleDebug` 통과, 신규 경고 없음
- [x] 메인 함수 본문·이동 코드 byte-identical(가시성 키워드 외) → 동작 보존 구조적 보장. 실기기 회귀는 코드 무변경이라 형식적 확인(사용자 실기기 연결 시 최종 확인)
  - [x] **(실기기)** 곡선 노드 추가·삭제·드래그(선택→조작) 동작 동일
  - [x] **(실기기)** 템플릿 선택(그리드→확정)·스텝 정밀도·요약 텍스트 동일
  - [x] **(실기기)** 곡선 저장(이름/설명)·아이콘/색상 피커 동작 동일

---

## Phase 4.7.7: 빌드·회귀 검증 종합

**목표**: 전체 리팩토링 결과를 종합 검증합니다.

**작업 항목**:
1. 전체 `.\gradlew testDebugUnitTest` + `.\gradlew assembleDebug` 통과 확인
2. 빌드 경고 점검 및 정리 (가능 범위)
3. Phase 4.1~4.6 주요 기능 수동 회귀 체크리스트 일괄 점검

**검증**:
- [x] 전체 단위 테스트 그린(14 suites / 222 tests / 0 fail / 0 error / 0 skip), 전체 빌드(`clean assembleDebug`) 통과, **신규 경고 없음**
  - 잔존 경고 10건 모두 기존 것(이동 코드에 원래 있던 것). 동작 보존 위해 미수정:
    - deprecated 아이콘 4: `AppIcons.kt:108`(TrendingUp)·`MacroEditorPopup.kt:1727`(CallSplit/CallMerge)·`ZoneActionPicker.kt:844`(KeyboardArrowRight)
    - tautological 4: `EdgeZoneEditorScreen.kt:1149/1220`·`EdgeZoneOverlay.kt:56`·`MacroEditorPopup.kt:1736`
    - deprecated API 2: `HidConstants.kt:143`(VIBRATOR_SERVICE)·`UsbPermissionReceiver.kt:75`(getParcelableExtra)
  - 코틀린 컴파일러는 미사용 import를 경고로 내지 않아 신규 정리 대상 없음. VIBRATOR_SERVICE→VibratorManager 마이그레이션은 동작 변경 위험으로 본 Phase 범위 밖(별도 작업)
- [x] **(실기기)** 스플래시·연결 대기 (Phase 4.1) 동작 동일
- [x] **(실기기)** Page 1 터치패드·Actions (Phase 4.2~4.3) 동작 동일
- [x] **(실기기)** 터치패드 다이나믹스·제스처 (Phase 4.4) 동작 동일
- [x] **(실기기)** 엣지 스와이프 대안 조작 (Phase 4.6) 동작 동일
- [x] **(실기기)** 리팩토링 전후 사용자 체감 동작 차이 없음
- [x] **(실기기)** 4.7.4-C 미체크 잔여: 페이지 1/2 전환 시 터치패드 모드·DPI 공유 유지
- [ ] **(기존 버그, 별도 추적)** 마우스 홀드 ON 상태 앱 종료 시 PC 버튼 잔류 없음(DisposableEffect 해제) — 리팩토링 전부터 존재, 본 Phase 범위 밖

---

## Phase 4.7.8: 존 이동/캔버스 모드 신규 코드 정리 (커밋 799f11c 후속)

> **⚠️ 적용 범위**: 커밋 `799f11c`로 추가된 존 이동 방식·캔버스 편집 모드 코드 한정. 4.7.1~4.7.7과 동일하게 **외부 동작 100% 유지**가 불변 조건. 동작 변경 위험이 있는 항목(아래 "별도 추적")은 본 Phase에서 제외한다.

**목표**: 신규 기능 도입으로 재비대화된 `EdgeZoneEditorScreen.kt`와 928줄 단일 오버레이를 분해하고, 오버레이 6개 파일의 기하/렌더 중복을 공통 유틸로 흡수하며, 흩어진 상수를 정리합니다. 저위험(상수·중복)부터 고위험(Screen 상태/이펙트 추출) 순으로 진행하며 각 서브단계는 독립 빌드·커밋됩니다.

**현재 상태 (커밋 직후)**:

| 파일 | 줄 수 | 문제 |
|---|---|---|
| `EdgeZoneEditorScreen.kt` | 2,509 (메인 Composable ~2,350) | 존 이동 미리보기 애니메이션 엔진·커밋 콜백 7종·`onZoneInteract` 71줄 모드 분기가 인라인 |
| `EdgeZoneCanvasModeOverlay.kt` | 928 (신규) | 제스처/hit-test(140줄)+비율 패널+모드 바 혼재, AnimatedVisibility 트랜지션 7회·포커스 Surface 버튼 6회 중복 |
| `EdgeZoneCanvasMode.kt` | 248 (신규) | 모델 타입 + 공간 네비 알고리즘 공존, `2.5f`/`1f` 튜닝값 리터럴 |
| `EdgeZoneCanvasGeometry.kt` | 278 (신규) | `findZoneAt`/`edgeAlongRatioAt` 엣지밴드 판정 루프 중복 (양호한 편) |
| 오버레이 6종 (`BoundaryManipulationHint`/`MergeSelectionOverlay`/`ZoneCanvasDropOverlay`/`ZoneCanvasResizeOverlay`/`ZoneMoveFloatingOverlay`/`EdgeZoneMorph`) | 77~241 | 엣지 스트립 `when(edge)` 기하 5파일 반복, 두께/shape/알파 상수 분산 |
| `Page5Settings.kt` | +54 | 세그먼트 선택 칩 UI 3중 복제 (존 페이지·존 이동 방식·TTS 성별) |
| `EdgeZoneEditorState.kt` | 422 (+263) | **모범** — 순수 로직 잘 분리, 일부 메서드 테스트 미커버 |
| `InputMode.kt` | +26 | `ZoneMoveMethod` enum·영속화 — 이상 없음 |

---

### 4.7.8-A (저위험): 상수 정리 + 모드 색상 enum 귀속

**작업 항목**:
- 캔버스 모드 5색(`EdgeZoneCanvasModeOverlay.kt` L510~514: MERGE/SPLIT/MOVE/DELETE/RESIZE) + ResizeGuide 퍼플(L785)을 `CanvasModeKind` 멤버 프로퍼티 또는 `touchpad/` 내 `CanvasModeColors` object로 귀속 (색은 도메인 enum 종속이라 `EdgeSwipeConstants`보다 응집도 우월)
- 반복 dp/알파/sp를 `EdgeSwipeConstants`의 기존 `EDGE_ZONE_MODE_*` 그룹(L267~280)에 추가: 모드 카드 `76.dp`(L288/304)·`RoundedCornerShape(14.dp)`·안내 배경 `0.55f`(L344/721/739 3회)·구분선 `0.15f` 등. 상수 기본값 주석 정책 준수
- 캔버스 네비 튜닝값 상수화: `EdgeZoneCanvasMode.kt` L121/123 cross-axis penalty `2.5f`(주석에 "2.5배"로 명시된 핵심 튜닝값), L109~112 deadzone `1f` → `CANVAS_NAV_*` 그룹
- 오버레이 두께/형상 단일화: `DROP_LINE_THICKNESS_DP`(ZoneCanvasDropOverlay L25)+`BOUNDARY_LINE_THICKNESS_DP`(ZoneCanvasResizeOverlay L29) 둘 다 `3f` → 단일 상수, `RoundedCornerShape(4.dp)`(Drop L96/Resize L89) 공통화, 기존 `EDGE_ZONE_FOCUS_BORDER_DP`(=2.5f) 재사용

**검증**:
- [x] `.\gradlew assembleDebug` 통과, 신규 경고 없음
- [x] 분리 전후 상수 값 동일

---

### 4.7.8-B (저위험): 오버레이 기하/렌더 공통 유틸 추출

**작업 항목**:
- **엣지 스트립 기하 헬퍼** (최대 효과): Drop L75~92·Resize L68~85(거의 바이트 동일)·Merge L63~68·Hint L83~88·`ZoneMoveFloatingOverlay.clippedStripDpRect` L66~95가 공유하는 "EDGE_HIT_WIDTH 두께 엣지 스트립 + along축 비율 위치" 계산을 `EdgeZoneCanvasGeometry.kt`(이미 `mapToValid`/`edgeValidRange` 보유)에 `edgeStripRect(...)`/`edgeHandleRect(...)`로 추출. `StripRect`(ZoneMoveFloatingOverlay L48 private)를 공용 반환 타입으로 승격
- **`EdgeLineMarker(vertical, thickness, color)` 컴포저블**: Drop L108~114 + Resize L110~116 동일 구조 통합
- **`brighten(color, fraction)` 헬퍼**: Drop L107(`lerp(accent, White, 0.5f)`) + Resize L109(`lerp(base, White, 0.45f)`) 통합 + 계수 상수화
- **`CornerClip(hasBottomLeft, hasBottomRight, blockedRatio)` 데이터 클래스**: 5개 Composable에 반복되는 코너 클리핑 3-파라미터 시그니처 축소
- **`EdgeZoneCanvasGeometry` 엣지밴드 루프 통합**: `findZoneAt`(L160~191)을 `edgeAlongRatioAt`(L209~231) 위에 재구성 (현재 each 독립 구현). `findBoundaryAt`은 이미 재사용 중

**검증**:
- [x] `.\gradlew testDebugUnitTest`(`edgeStripRect`/`edgeHandleRect` 단위 테스트 12건 추가) + `.\gradlew assembleDebug` 통과, 신규 경고 없음
- [ ] 경계 리사이즈·드롭 라인 마커·머지 선택 오버레이 시각 회귀 (실기기)

---

### 4.7.8-C (중위험): EdgeZoneCanvasModeOverlay.kt 928줄 분해

`touchpad/` 직하로 분리 (4.7.5~4.7.6 선례 일관, `internal` 승격으로 cross-file 접근):

| 신규 파일 | 추출 대상 | 줄 범위 |
|---|---|---|
| `EdgeZoneCanvasGestures.kt` | `inputModifier` when-블록 → `Modifier.canvasModeInput(...)` 확장. Geometry 함수만 의존 | L118~256 |
| `EdgeZoneCanvasRatioPanel.kt` | `ResizeGuideCard`/`ManipulationGuideBubble`/`ResizeGuideRow`/`RatioPresetEdgeBar`/`PanelCancelButton`/`RatioPresetChip` | L717~928 |
| `EdgeZoneCanvasModeBars.kt` | `ModeActiveBar`/`SplitModeBar`/`CancelButton`/`ConfirmButton` | L559~714 |
| `EdgeZoneCanvasModeButtons.kt` | `ModeCard` + 진입 버튼 2행 | L517~557 |
| `EdgeZoneCanvasMode.kt`(기존)로 이동 | `icon()`/`accentColor()`/`guideText()` — 모드 메타데이터를 kind/toMode와 동거 | L474~515 |

**중복 헬퍼화 (구현 결과)**:
- `canvasModeEnter(delay)`/`canvasModeExit()`: AnimatedVisibility 트랜지션 7회 → `internal` 헬퍼 2개로 통합 (`EdgeZoneCanvasModeOverlay.kt` 잔류, 동일 패키지 파일들이 직접 호출)
- `CanvasPillButton` 통합 생략: `CancelButton`/`ConfirmButton`(fontSize 12sp, padding 14/4)과 `SplitModeBar` 번호 버튼(fontSize 14sp, padding 12/5)이 스타일이 달라 통합 시 동작 변경 위험 → 개별 유지 (무리한 통합보다 동작 동등성 우선)
- 지역 `fun zoneAt(offset)` 람다: `findZoneAt` 5회 호출 → `EdgeZoneCanvasGestures.kt` 내 로컬 헬퍼로 통합
- 제스처 추출 시 토스트 직접 호출 → `onEdgeBlocked: (EntryEdge) -> Unit` 콜백으로 호이스트 (4.7.4-B 사이드이펙트 격리 철학 일관)

결과: `EdgeZoneCanvasModeOverlay`는 오케스트레이션만 남아 ~250줄로 축소 (공유 헬퍼 포함). 신규 파일 4개 + 기존 파일 1개(`EdgeZoneCanvasMode.kt`) 보강.

**검증**:
- [x] `.\gradlew assembleDebug` 통과, 신규 경고 없음
- [x] 제스처 byte-identical 대조 (토스트 콜백화·zoneAt 헬퍼화 외 변경 없음)
- [x] **(실기기 · NORMAL)** 캔버스 씬 진입 시 모드 버튼 5개(병합/분할/이동/삭제/비율) 순차 등장, 탭으로 각 모드 진입
- [x] **(실기기 · NORMAL)** 병합: 존 탭 → 인접 존 탭 → 확인 버튼으로 병합 완료. 선택 부족 시 확인 버튼 비활성
- [x] **(실기기 · NORMAL)** 분할: 존 탭 → 갯수 버튼(2~5, 불가한 수는 회색) 등장 → 탭으로 분할
- [x] **(실기기 · NORMAL)** 이동(탭): 존 탭으로 들어올림 → 경계/끝 탭으로 드롭. 롱프레스로 되돌리기 후 모드 선택 복귀
- [x] **(실기기 · NORMAL)** 이동(드래그앤드롭): 존 드래그 → 릴리스로 드롭. 확인/취소 버튼 노출, 취소 탭으로 역순 되돌리기
- [x] **(실기기 · NORMAL)** 삭제: 존 탭 다중 선택 → 확인으로 삭제. 선택 0개 시 확인 비활성
- [x] **(실기기 · NORMAL)** 비율: 안내 카드+확인/취소 버튼 표시 → 경계 드래그로 비율 조정 → 확인 적용/취소 원복
- [x] **(실기기 · NORMAL)** 비율: 엣지 탭 → 엣지 옆 프리셋 칩 패널 등장 → 칩 탭으로 적용. 비활성 엣지 탭 시 토스트 표시
- [x] **(실기기 · SWIPE)** 스와이프로 모드 버튼에 포커스 이동, 탭으로 모드 진입
- [x] **(실기기 · SWIPE)** 각 모드 진입 후 취소 버튼에 초기 포커스, 스와이프로 확인/갯수 버튼 이동 및 탭 실행
- [x] **(실기기 · SWIPE)** 비율 프리셋 패널 진입 시 포커스가 칩·취소 버튼으로 한정되고 캔버스 존으로 새지 않음

---

### 4.7.8-D (저위험): 설정 칩 추출 + State 테스트 보강

**작업 항목**:
- **`SegmentedChipSelector(options, selected, onSelect)` 추출**: `Page5Settings.kt` 3중 복제(존 페이지 L184~201 / 존 이동 방식 L239~263 / TTS 성별 L331~355) 통합. 색상 리터럴은 추출 시 테마 상수화. `SettingsToggleRow`(L593)는 토글 전용이라 별개
- **`EdgeZoneEditorState` 테스트 보강**: 이미 순수 분리된 `deleteZones`/`mergeContiguous`/`adjustBoundary`/`computeSplitZones`가 미커버 → `EdgeZoneEditorStateTest`에 케이스 추가 (코드 수정 없이 테스트만)

**검증**:
- [x] `.\gradlew testDebugUnitTest`(State 보강 테스트 그린, `deleteZones` 5건·`mergeContiguous` 3건·`adjustBoundary` 4건·`computeSplitZones` 3건 = 15건 추가) + `.\gradlew assembleDebug` 통과
- [x] **(실기기)** 설정 페이지 세그먼트 칩 3종(존 페이지·존 이동 방식·TTS 성별) 선택 동작 동일

---

### 4.7.8-E: EdgeZoneEditorScreen.kt 존 이동 계층 추출 (컨벤션 부합 변형)

> **계획 명세(rememberZoneMovePreviewState 홀더 / ZoneMoveController 클래스)와 다르게 컨벤션 부합 순수 추출로 진행.**
> 이유: (1) 이 프로젝트엔 LaunchedEffect/Animatable을 내부에 품는 remember 홀더 선례 없음, (2) `EdgeZoneOverlayUiState` KDoc이 "로더는 화면에 남긴다" 규칙 명시, (3) 추출 대상 상태가 onZoneInteract뿐 아니라 onConfirm·onSplitInto·settle effect까지 양방향으로 읽고 써 노출 ~12개 + 쓰기훅 3개로 홀더가 캡슐화가 아닌 파라미터 셔플이 됨.
> → 순수 로직만 State/Morph로 추출 + 단위 테스트. Animatable/LaunchedEffect 안무와 얇은 디스패처 콜백은 Screen 인라인 유지.

**산출물**:
- `EdgeZoneEditorState`: `InteractRejection` sealed class (토스트 5종), `MergeTap` sealed class + `mergeTapDecision()` 순수 메서드
- `EdgeZoneMorph.kt`: `resolveDisplayConfig()` / `liftedKeyFor()` / `buildCommitFloat()` 순수 함수 3종
- `EdgeZoneEditorScreen.kt`: 호출부 치환 (토스트 인라인 문자열 → `InteractRejection`, Merging else 블록 → `mergeTapDecision`, displayConfig/liftedKey/moveFloat 구성 → 순수 함수 위임)
- `EdgeZoneEditorStateTest.kt`: `mergeTapDecision` 7건 + `resolveDisplayConfig` 4건 + `liftedKeyFor` 3건 + `buildCommitFloat` 2건 = 16건 추가

**검증**:
- [x] `.\gradlew testDebugUnitTest` + `.\gradlew assembleDebug` 통과, 신규 경고 없음
- [x] **(실기기 · InteractRejection 토스트)** 삭제 모드: 1개 존만 있는 엣지 탭 → "엣지에 존이 하나뿐이라 삭제할 수 없어요" 토스트
- [x] **(실기기 · InteractRejection 토스트)** 분할 모드: 존이 최대 개수인 엣지 탭 → "존이 N개로 가득 차 더 나눌 수 없어요" 토스트 (N은 실제 MAX_ZONES_PER_EDGE 값)
- [x] **(실기기 · InteractRejection 토스트)** 비율 모드: 1개 존만 있는 엣지 탭 → "존이 하나뿐인 가장자리는 비율을 나눌 수 없어요" 토스트
- [x] **(실기기 · InteractRejection 토스트)** 병합 모드: 다른 엣지 존 탭 → "같은 가장자리의 인접한 존만 선택할 수 있어요" 토스트
- [x] **(실기기 · InteractRejection 토스트)** 병합 모드: 구간에서 멀거나 내부 존 탭 → "인접한 존만 선택할 수 있어요" 토스트
- [x] **(실기기 · mergeTapDecision)** 병합 모드 NORMAL: 첫 존 탭 → 기준 설정, 인접 존 탭 → 선택 추가, 반대편 인접 존 탭 → 반대쪽으로도 추가, 끝점 재탭 → 해제, 확인 → 병합 완료
- [x] **(실기기 · mergeTapDecision)** 병합 모드 SWIPE: 스와이프로 존 포커스 이동 후 탭으로 동일한 추가/해제/거부 동작
- [x] **(실기기 · resolveDisplayConfig)** 존 이동 커밋(NORMAL 탭/드래그) 직후: morph 애니메이션(stretch·shrink) 재생 후 최종 위치에 존 안착
- [x] **(실기기 · resolveDisplayConfig)** 비율 조정 롱프레스 '되돌리고 나가기': 각 단계마다 ratioMorph 보간 애니메이션이 끊김 없이 재생되고 원래 비율로 복원
- [x] **(실기기 · resolveDisplayConfig)** 분할 모드 SWIPE: 분할 갯수 버튼 포커스 이동 시 previewConfig 기반 n등분 미리보기가 실시간 업데이트
- [x] **(실기기 · liftedKeyFor)** NORMAL 탭 이동: 픽 후 캔버스에 들림 고스트(반투명)가 해당 존 위치에 정확히 표시, 드롭 시 사라짐
- [x] **(실기기 · liftedKeyFor)** SWIPE 이동 / NORMAL 드래그앤드롭: 들림 고스트 없이 떠다니는 float 오버레이만 표시
- [x] **(실기기 · buildCommitFloat)** NORMAL 탭 이동 커밋: float 오버레이가 출발 위치에서 도착 위치로 슬라이드하며 morph와 동기
- [x] **(실기기 · buildCommitFloat)** cross-edge 이동(예: TOP→RIGHT): float가 출발 엣지→도착 엣지로 올바르게 이동, 라벨 있는 존은 float에 라벨 표시
- [x] **(실기기)** 삭제·분할·비율·이동 모드 취소(취소 버튼) 및 모드 선택 복귀 정상
- [x] **(실기기)** SWIPE·NORMAL 전환 시 위 모든 동작 동일

---

**별도 추적 (본 Phase 범위 밖 — 동작 변경 위험)**:
- `MergeSelectionOverlay.MERGE_REGION_COLOR`(`0xFF4CAF50`)와 `CanvasModeKind.MERGE.accentColor()`(`0xFF1A7A3A`) 초록색 불일치 — 단일화는 색 변경이라 의도 확인 필요
- `StandardModePrefs.saveCornerBlockedRatio` clamp `coerceIn(0.05f, 0.30f)` 매직넘버 상수화 — 슬라이더 UI와 범위 공유 필요. 이 기능은 "존 이동 방식"과 무관하게 같은 커밋에 혼입됨
- `MAX_ZONES_PER_EDGE` 4f→5f 변경은 이번 커밋의 의도된 동작 변경 (리팩토링 대상 아님)