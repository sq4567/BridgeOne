---
title: "BridgeOne Phase 4.7: Android 코드베이스 리팩토링"
description: "BridgeOne 프로젝트 Phase 4.7 - Phase 4.1~4.6 산출물의 외부 동작을 유지한 채 내부 구조 개선 (상수 정리·중복 제거·거대 파일 분해·MVVM ViewModel 도입)"
tags: ["android", "refactoring", "architecture", "mvvm", "code-quality"]
version: "v1.0"
owner: "Chatterbones"
updated: "2026-06-17"
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

**Phase 4.16(성능 최적화)와의 관계**: 본 Phase는 **구조 개선**이 목적이며, 성능 향상은 부수적 결과입니다. 측정 기반의 성능 최적화는 모든 기능 완성 후 `phase-4-16-performance.md`에서 별도로 진행합니다. 일부 대상 코드가 겹치므로(예: 햅틱 호출, 제스처 루프), 본 Phase에서 만든 구조 위에서 4.16이 동작하게 됩니다.

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
- **존 분할/병합** — `EdgeZoneEditorScreen`의 로컬 `fun splitInto`/`tryMergeWith`(393~427행). 클로저에 갇힘 → 4.7.5-A

**검증**:
- [x] `.\gradlew testDebugUnitTest` 전체 그린 (103 tests, 0 fail)
- [x] 4.7.2-B 대상 로직이 모두 테스트로 고정됨 (이후 회귀 시 즉시 탐지)

---

## Phase 4.7.3: TouchpadWrapper 순수 함수·햅틱 분리

> **⚠️ Phase 4.16.3 / 4.16.4 영향**: 가이드라인 리컴포지션 최소화(4.16.3)와 제스처 루프 작업 제거(4.16.4)는 현재 `TouchpadWrapper` 제스처 루프 구조 위에서 진행됩니다. 4.16 진행 시 이 파일 구조를 먼저 확인할 것.

**목표**: `TouchpadWrapper`에서 떼어낼 가치가 분명한 순수 함수와 중복 로직을 분리합니다. 제스처 루프(`pointerInput`)는 코루틴·부작용·콜백이 본질적으로 강결합된 상태머신이라 분해하지 않습니다.

**작업 항목**:

### 4.7.3-A (저위험): 순수 기하/판정 함수 분리 ✅
- 신규 `ui/components/touchpad/EdgeGeometry.kt`: `detectEntryEdge`/`getInwardDistance`/`getAlongEdgePosition`/`findNearestEdge`/`computeDirectTouchButtonRects`/`applyEdgeModeToggle` 6개 함수를 `internal` top-level로 이동 (동작 동일)
- `TouchpadWrapper.kt`에서 함수 정의 삭제 → import 6개 추가 (`com.bridgeone.app.ui.components.touchpad.*`), 불필요 import (`CornerOverlap`) 제거
- 신규 `EdgeGeometryTest.kt`: 42 tests 그린 (4.7.2-C "엣지 진입 판정" 항목 해소)

### 4.7.3-B (중위험): 햅틱 단일화 (기존 4.7.2 항목 흡수) ✅
- 신규 `ui/common/HapticFeedbackHelper.kt`: 완전 중복이던 `vibrator.vibrate(...)` 2곳 (터치 드래그 중 / 관성 코루틴 중)을 `fun vibrateByVelocity(velocity: Float)`로 단일화. SDK O 가드·amplitude 계산·VibrationEffect 생성 흡수
- `TouchpadWrapper.kt`: `hapticHelper = remember(vibrator) { HapticFeedbackHelper(vibrator) }` 추가, 두 호출부를 `hapticHelper.vibrateByVelocity(speed/abs(velocity))`로 교체. 불필요 import (`VibrationEffect`, `INFINITE_SCROLL_HAPTIC_*` 4개) 제거

> **⚠️ Phase 4.16.2 영향**: 4.16.2(햅틱 호출 빈도 최적화)는 `HapticFeedbackHelper`에 시간 게이트(`HAPTIC_MIN_INTERVAL_MS`)를 추가하는 방식으로 진행합니다. 호출부가 헬퍼로 단일화되어 있으므로 한 곳만 수정하면 전체 적용됩니다.

**검증**:
- [x] `.\gradlew testDebugUnitTest`(EdgeGeometry 42 tests) + `.\gradlew assembleDebug` 통과 (4.7.3-A/B)
- [x] 커서 이동·클릭·드래그·스크롤·관성·엣지 스와이프·햅틱 수동 회귀 통과 (A/B 완료 시점)

---

## Phase 4.7.4: StandardModePage 분해 + 매크로 시퀀서 추출

**목표**: 한 파일에 정의된 13개 Composable을 페이지별로 분리하고, 매크로 시퀀싱 순수 로직을 추출하며, 공유 상태를 상태 홀더로 이관합니다.

**작업 항목**:

### 4.7.4-A (저위험): 페이지 Composable 파일 분리 + 재사용 컴포넌트 추출
`ui/pages/standard/` 신설. 페이지 래퍼와 재사용 컴포넌트를 구분해 분리합니다.

**페이지 래퍼** (4.15.4에서 `DynamicPage`로 대체될 임시 구조. 각 래퍼의 레이아웃 비율/배치 로직이 4.15.2 `DefaultPageTemplates` 데이터화의 기준이 됨):
- `Page1TouchpadActions.kt` / `Page2TestTouchpad.kt` / `Page3KeyboardPlaceholder.kt` / `Page4MinecraftPlaceholder.kt` / `Page5Settings.kt`(+`SettingsInputModeSection`/`SettingsEdgeInteractionModeSection`)
- `PageIndicator.kt` — 이미 `pageCount` 파라미터를 받으므로 단순 이동, 4.15 영향 없음
- `StandardModePrefs.kt`: SharedPreferences 헬퍼(`loadDpiLevel`/`saveDpiLevel`/`loadEdgeInteractionMode` 등) 이동 — 전역 설정이라 4.15 영향 없음

**재사용 컴포넌트** (`ui/pages/standard/components/` 신설, 가시성 `internal`):
- `ActionsPanel.kt`, `SpecialKeysGrid.kt`, `ShortcutsGrid.kt`, `MacrosPlaceholder.kt`
- 각 Composable의 페이지 컨텍스트 의존을 끊고 콜백·상태를 **명시적 파라미터**로 수령. 단, 4.15의 `ComponentCallbacks` 번들 강제 도입은 금지 — 그건 4.15.3 작업

> **⚠️ Phase 4.15 영향**: 4.15.4가 `when(page % PAGE_COUNT)` 분기를 `DynamicPage`(데이터 기반 렌더링)로 통째 대체한다. 따라서 페이지 래퍼(`Page1TouchpadActions` 등)는 임시 구조이며 4.15.2 `DefaultPageTemplates`의 데이터화 기준으로만 쓰인다. 반면 내부 컴포넌트(`ActionsPanel`/`SpecialKeysGrid`/`ShortcutsGrid`)는 4.15.3 `ComponentRenderer`가 직접 디스패치해 재사용하므로, 페이지 의존을 끊은 standalone `internal` Composable로 추출한다(`ui/pages/standard/components/`).

### 4.7.4-B (중위험): 매크로/단축키 시퀀싱 순수화
- 신규 `ui/common/MacroFrameSequencer.kt`: `onSendMacro`(218~320행)의 스텝→`List<BridgeFrame>` 시퀀스 생성 로직을 순수 함수로 추출 (딜레이는 호출부 적용 또는 `(frame, delayMs)` 리스트 반환). `onSendShortcut`도 동일
- `sendFrame`/`coroutineScope.launch`/`MacroOverlayController`/`ToastController` 등 사이드이펙트는 Composable 콜백에 잔류
- 신규 `MacroFrameSequencerTest.kt`: TAP repeat, 홀드 합성(combinedMod/keys 2개 제한), dangling hold 안전 해제, estimatedMs 계산 (4.7.2-C 항목 해소)

> **⚠️ Phase 4.15 영향**: `MacroFrameSequencer`는 4.15의 `MACRO_BUTTON` 렌더링이 그대로 재사용한다. 스텝 입력은 4.15 `MacroButtonCfg.steps`와 호환되도록 페이지 비의존 데이터 타입으로 받을 것. 단, 매크로 스텝의 JSON 직렬화 헬퍼(`macroStepsToJson`/`macroStepsFromJson`) 추출은 `EdgeZoneJson.kt`를 손대는 별개 작업으로 4.15.1 소관이며 4.7.4-B 범위가 아니다.

### 4.7.4-C (고위험): StandardModePageState 상태 홀더
- 신규 `ui/pages/StandardModePageState.kt` (평범한 상태 홀더): `touchpadState` 호이스팅, `ModeHistoryStack`/`recordingOnChange`/`onRestorePrevious`, `heldMouseButtons` 이관. 페이지는 상태 구독·이벤트 위임만
- `standardAssignments`/`standardButtonVisibility` 맵은 **단순 호이스팅**만 수행. 정교한 accessor/변환 API를 새로 만들지 않음 — 4.15.4가 깔끔히 제거할 수 있도록 격리 유지

> **⚠️ Phase 4.15 영향**: `StandardModePageState`는 4.15.2가 `pages: List<PageLayout>` 상태와 debounce 저장 `LaunchedEffect`를 가산(additive)으로 추가한다. 또한 `standardAssignments`/`standardButtonVisibility` 맵은 4.15.4에서 제거되어 `PlacedComponent` config로 흡수된다. 따라서 이 두 맵은 단순 호이스팅만 하고 정교한 변환 API를 새로 만들지 않는다(4.15.4 제거 용이성 확보).

**검증**:
- [ ] `.\gradlew testDebugUnitTest`(MacroFrameSequencer) + `.\gradlew assembleDebug` 통과
- [ ] 5페이지 스와이프 전환 동작 동일
- [ ] 페이지 간 상태 공유·동기화 동일
- [ ] DPI·매크로·곡선 편집 등 팝업 호출 동일
- [ ] 추출된 재사용 컴포넌트(`ActionsPanel`/`SpecialKeysGrid`/`ShortcutsGrid`)가 페이지 비의존 standalone `internal` Composable로 분리되고, Page1 렌더 결과 동일

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
1. **4.7.5-A**: 순수 함수(`EdgeZoneActionResolver`) + config 변환·Undo(`EdgeZoneEditorState`) 추출 + 테스트(splitInto/tryMerge/describeUndoStep/JSON 라운드트립). 4.7.2-C 항목 해소
2. **4.7.5-B** (저위험): 이미 독립적인 `private fun` Composable(Macro/Shortcut/Rotation/팝업류)을 파일 이동만 (시그니처 유지)
3. **4.7.5-C**: 편집 패널을 섹션별 Composable로 분리, 상태는 `EdgeZoneEditorState`/hoist 파라미터로 전달
4. **4.7.5-D**: SWIPE/NORMAL 레이어 분리 + `CompositionLocal` 도입

> **주의**: 4.7.1 회귀목록의 ⚠️ 기존 스트립 에디터 스와이프 버그(구분선 1칸 이동 후 무반응)는 본 리팩토링 범위 밖입니다. 동작 동일성만 유지하고 별도 추적합니다.

**검증**:
- [ ] `.\gradlew testDebugUnitTest`(EdgeZoneEditorState) + `.\gradlew assembleDebug` 통과
- [ ] 4개 엣지 존 시각화·드래그 편집 동작 동일
- [ ] 존 분할/병합, 액션·아이콘·컬러·숏컷 할당 동작 동일
- [ ] 회전 트리거, 라벨 편집(IME), 프리셋 저장/로드 동작 동일
- [ ] Undo/Redo 동작 동일
- [ ] SWIPE/NORMAL 두 모드 UI 모두 동작 동일

---

## Phase 4.7.6: DynamicsCurveEditor 분해 (난도 최저)

**목표**: 곡선 편집기를 렌더링·제스처·데이터 생성 단위로 분리합니다. 이미 내부 서브 Composable이 잘 나뉘어 있어 4개 대상 중 난도가 가장 낮습니다.

**작업 항목** (`ui/components/touchpad/curve/` 신설):
1. `CurveGraphCanvas.kt`: 곡선 캔버스 렌더링(715행 호출부) + 노드 드래그 `pointerInput`(659행) 분리
2. `NodeEditPanel.kt`: `NodeEditGrid`/`NodeEditHeader`/`EditorActionGrid`/십자패드
3. `EditorCards.kt`: `MetaCard`/`CurveCard`/`ActionCard`/`EditorHeader`
4. `CurveSummary.kt`: 곡선 요약(자연어)·`templateAccent`·step 정밀도 (순수) + 테스트
5. 필요 시 `CurveEditorState.kt`: `GridContext` 네비 상태 홀더

**검증**:
- [ ] `.\gradlew testDebugUnitTest`(CurveSummary) + `.\gradlew assembleDebug` 통과
- [ ] 곡선 노드 추가·삭제·드래그 동작 동일
- [ ] 템플릿 선택·스텝 정밀도·요약 텍스트 동일
- [ ] 곡선 저장(이름/설명) 동작 동일

---

## Phase 4.7.7: 빌드·회귀 검증 종합

**목표**: 전체 리팩토링 결과를 종합 검증합니다.

**작업 항목**:
1. 전체 `.\gradlew testDebugUnitTest` + `.\gradlew assembleDebug` 통과 확인
2. 빌드 경고 점검 및 정리 (가능 범위)
3. Phase 4.1~4.6 주요 기능 수동 회귀 체크리스트 일괄 점검

**검증**:
- [ ] 전체 단위 테스트 그린, 전체 빌드 통과, 신규 경고 없음
- [ ] 스플래시·연결 대기 (Phase 4.1) 동작 동일
- [ ] Page 1 터치패드·Actions (Phase 4.2~4.3) 동작 동일
- [ ] 터치패드 다이나믹스·제스처 (Phase 4.4) 동작 동일
- [ ] 엣지 스와이프 대안 조작 (Phase 4.6) 동작 동일
- [ ] 리팩토링 전후 사용자 체감 동작 차이 없음

---

## Phase 4.7.8: 리팩토링 후 동작 검증

> **선행 조건**: Phase 4.7.7 빌드·회귀 통과 후 진행. 4.7.7이 빌드와 개략 회귀를 다룬다면, 본 Phase는 리팩토링 전 버전과의 동작 동일성을 실기기에서 시나리오별로 입증합니다.

**목표**: 리팩토링이 사용자 체감 동작을 바꾸지 않았음을 실기기 E2E로 확인합니다. 본 Phase의 불변 조건(외부 동작 100% 유지)을 최종 입증하는 단계입니다.

**작업 항목**:
1. 리팩토링 착수 전 빌드(기준 버전)의 핵심 시나리오 동작·타이밍·피드백을 기준값으로 확보
2. 핵심 입력 시나리오를 실기기에서 재현하여 기준 버전과 비교
   - 커서 이동/클릭/드래그
   - 스크롤(일반/무한)/관성/축 확정
   - 엣지 스와이프 메뉴 및 대안 조작
   - 멀티 패드/존 편집/프리셋
   - 곡선 편집/다이나믹스 적용
3. 입력 정확성·지연·햅틱 타이밍에 회귀가 없는지 확인
4. 차이 발견 시 해당 하위 Phase(4.7.1~4.7.6)로 돌아가 수정 후 재검증

**에뮬레이터 호환성**: 입력 정확성·지연·하드웨어 피드백 검증은 ESP32-S3 동글 + PC 연결 실기기 필요. 에뮬레이터는 UI 흐름 확인까지만.

**검증**:
- [ ] ESP32-S3 동글 + PC 연결 실기기 E2E 정상 동작
- [ ] 커서 이동·클릭·드래그 정확성 기준 버전과 동일
- [ ] 스크롤(일반/무한)·관성 동작 및 감도 동일
- [ ] 엣지 스와이프 메뉴·존 편집·프리셋 동작 동일
- [ ] 곡선 편집·다이나믹스 적용 결과 동일
- [ ] 햅틱·토스트 등 피드백 타이밍 동일
- [ ] 리팩토링 전후 입력 지연 체감 차이 없음
