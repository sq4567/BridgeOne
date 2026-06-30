---
title: "BridgeOne Phase 4.16: 성능 최적화"
description: "BridgeOne 프로젝트 Phase 4.16 - 전반적 입력 지연 및 렉 원인 조사 및 수정"
tags: ["android", "performance", "haptic", "recomposition", "input-latency"]
version: "v1.0"
owner: "Chatterbones"
updated: "2026-04-11"
---

# BridgeOne Phase 4.16: 성능 최적화

**개발 기간**: 미정

**목표**: 실기기에서 체감되는 전반적 느림(입력 지연, UI 렉)의 원인을 프로파일링으로 특정한 후 수정합니다.

| 하위 Phase | 내용 | 상태 |
|-----------|------|------|
| 4.16.1 | 프로파일링 및 병목 특정 | 미시작 |
| 4.16.2 | 햅틱 호출 빈도 최적화 | 미시작 |
| 4.16.3 | Compose 리컴포지션 최소화 | 미시작 |
| 4.16.4 | 제스처 루프 내 불필요한 작업 제거 | 미시작 |

---

## Phase 4.16.1: 프로파일링 및 병목 특정

**개발 기간**: 0.5일

**목표**: 수정 전에 실제 병목을 데이터로 확인합니다. 감각에 의존하지 않고 프로파일러 수치가 원인을 지목하면 그 항목만 수정합니다.

### 조사 A: 메인 스레드 점유 시간 (CPU Profiler)

- Android Studio → Profiler → CPU → `System Trace` 모드
- 터치패드를 빠르게 조작하면서 1~2초 캡처
- 확인 항목:
  - `pointerInput` 람다가 메인 스레드에서 얼마나 오래 실행되는지
  - `vibrate()` Binder IPC 호출이 메인 스레드를 얼마나 점유하는지
  - `performHapticFeedback()` 호출 비율

### 조사 B: Compose 리컴포지션 빈도 (Compose Recomposition Highlight)

- Android Studio → Layout Inspector → Recomposition 카운트 활성화
- 터치패드 빠른 드래그 중 어떤 Composable이 가장 자주 리컴포지션되는지 확인
- 확인 항목:
  - `TouchpadWrapper`, `ScrollGuideline`, `EdgeSwipeOverlay` 리컴포지션 빈도
  - `guidelineTarget`, `guidelineVisible` 상태 변경이 불필요하게 넓은 범위를 리컴포지션시키는지

### 조사 C: 프레임 드롭 (Frame Timing)

- Android Studio → Profiler → Display → Frame Timing
- 빠른 드래그 중 16ms 초과 프레임 비율 확인
- Choreographer jank 발생 구간과 위 조사 A/B 결과를 대조

### 조사 결과 기록

> 각 조사 완료 후 아래 표를 채웁니다.

| 항목 | 측정값 | 기대치 | 병목 여부 |
|-----|-------|-------|---------|
| pointerInput 메인 스레드 점유 (ms/이벤트) | - | < 1ms | - |
| vibrate() 호출 빈도 (회/초) | - | < 30 | - |
| 리컴포지션 빈도 (TouchpadWrapper, 회/초) | - | < 60 | - |
| 16ms 초과 프레임 비율 | - | < 5% | - |

**검증**:
- [ ] System Trace 캡처 완료 및 메인 스레드 점유 항목 기록
- [ ] Compose Recomposition 카운트 확인 완료
- [ ] Frame Timing 캡처 완료 및 jank 비율 기록
- [ ] 조사 결과 표 작성 완료
- [ ] 병목 항목 특정 완료 → 해당 Phase(4.16.2~4.16.4) 진행 여부 결정

---

## Phase 4.16.2: 햅틱 호출 빈도 최적화

> **선행 조건**: Phase 4.16.1 조사 A에서 `vibrate()` 가 병목으로 확인된 경우에만 진행.

> **⚠️ Phase 4.7.3-B 변경사항**: 드래그/관성 햅틱의 인라인 `vibrator.vibrate(...)` 2개 호출부가 `src/android/app/src/main/java/com/bridgeone/app/ui/common/HapticFeedbackHelper.kt`의 `vibrateByVelocity()` **단일 지점**으로 통합됨. 따라서 시간 게이트(`HAPTIC_MIN_INTERVAL_MS` + `lastHapticTimestampMs`)는 `TouchpadWrapper`가 아니라 **`HapticFeedbackHelper` 한 곳**에 추가하면 두 호출부에 동시 적용된다. `lastHapticTimestampMs`는 헬퍼 내부 필드로 보유(remember 상태 불필요). `HAPTIC_MIN_INTERVAL_MS` 상수는 `ScrollConstants.kt`에 유지 가능.
> 추가 주의: 햅틱 enable 토글(Phase 4.14.4)도 이 헬퍼를 게이트하므로, 시간 게이트와 enable 플래그를 같은 지점에서 함께 처리하면 충돌이 없다.

**개발 기간**: 0.5일 미만

**문제**:
- 무한 스크롤 MOVE 이벤트마다 `HapticFeedbackHelper.vibrateByVelocity()`(내부에서 `vibrator.vibrate(...)`) 호출
- 관성 루프 16ms tick마다 동일 호출 (4.7.3-B로 두 경로가 같은 헬퍼로 단일화됨)
- `vibrate()`는 시스템 서비스로의 Binder IPC이므로 호출 빈도가 높을수록 오버헤드 증가
- 120Hz 기기에서 MOVE 이벤트가 초당 120회 발생 → 초당 120회 Binder IPC

**수정 내용**:
- `HAPTIC_MIN_INTERVAL_MS` 상수 추가 — 연속 햅틱 호출 최소 간격 (기본값: 32ms → 최대 약 30회/초)
- `HapticFeedbackHelper` 내부에 `lastHapticTimestampMs` 필드 추가
- `vibrateByVelocity()` 내부에서 실제 `vibrate()` 호출 전 시간 게이트 적용 (호출부가 단일화되어 이 한 곳만 수정하면 드래그/관성 두 경로에 동시 적용):
  ```kotlin
  // HapticFeedbackHelper 내부
  val nowMs = System.currentTimeMillis()
  if (nowMs - lastHapticTimestampMs >= HAPTIC_MIN_INTERVAL_MS) {
      vibrator.vibrate(VibrationEffect.createOneShot(20, amplitude))
      lastHapticTimestampMs = nowMs
  }
  ```

**수정 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/common/ScrollConstants.kt`
  — `HAPTIC_MIN_INTERVAL_MS` 상수 추가
- `src/android/app/src/main/java/com/bridgeone/app/ui/common/HapticFeedbackHelper.kt`
  — `lastHapticTimestampMs` 내부 필드 추가
  — `vibrateByVelocity()` 내 `vibrate()` 호출에 시간 게이트 적용 (드래그/관성 두 호출부에 동시 적용됨)

**검증**:
- [ ] 무한 스크롤 빠른 드래그 중 햅틱 체감 유지되는지 확인 (진동 느낌 여전히 있음)
- [ ] Phase 4.16.1 System Trace 재측정 → `vibrate()` 호출 빈도 감소 확인
- [ ] 메인 스레드 점유 시간 감소 확인
- [ ] 일반 스크롤 CLOCK_TICK 햅틱에는 영향 없음 확인 (별도 경로)

---

## Phase 4.16.3: Compose 리컴포지션 최소화

> **선행 조건**: Phase 4.16.1 조사 B에서 과잉 리컴포지션이 병목으로 확인된 경우에만 진행.

> **⚠️ Phase 4.7.3 변경사항**: `TouchpadWrapper`의 제스처 루프(`pointerInput`)는 4.7.3에서 **의도적으로 분해하지 않음**(코루틴·부작용·콜백이 강결합된 상태머신). 따라서 이 Phase의 리컴포지션 조사 대상 구조는 그대로 유효하나, 순수 기하 함수는 `EdgeGeometry.kt`로, 햅틱은 `HapticFeedbackHelper.kt`로 외부 추출되어 `TouchpadWrapper.kt` 내부 줄 번호는 이동했다 — 위치는 함수명/패턴으로 탐색할 것. 조사 B 목록의 `EdgeSwipeOverlay`는 실재 파일(`ui/components/touchpad/EdgeSwipeOverlay.kt`)로 유효.

**개발 기간**: 0.5~1일

**문제**:
MOVE 이벤트마다 `guidelineTarget`(Float 상태), `guidelineVisible`(Boolean 상태) 등이 변경되어 이를 읽는 Composable 전체가 리컴포지션됨. 특히 `TouchpadWrapper`가 대형 Composable이라 리컴포지션 범위가 넓을 가능성이 있음.

**수정 방향** (조사 결과에 따라 선택):

| 방법 | 적용 조건 |
|-----|---------|
| `derivedStateOf` 적용 | 상태 A에서 파생된 값 B가 따로 있을 때 B만 변경 시 리컴포지션 방지 |
| 상태 읽는 위치를 하위 Composable로 내리기 (State Hoisting Reversal) | 넓은 범위가 불필요하게 리컴포지션될 때 |
| `remember { mutableFloatStateOf }` → `Animatable` 로컬화 | 애니메이션 상태를 Composable 내부로 캡슐화 |

**수정 파일**: 조사 결과 확인 후 결정

**검증**:
- [ ] Phase 4.16.1 Recomposition 카운트 재측정 → 빈도 감소 확인
- [ ] 가이드라인·산봉우리 애니메이션 시각적 동작 동일 확인 (기능 회귀 없음)

---

## Phase 4.16.4: 제스처 루프 내 불필요한 작업 제거

> **선행 조건**: Phase 4.16.1 조사 A에서 `pointerInput` 람다 실행 시간이 병목으로 확인된 경우에만 진행.

> **⚠️ Phase 4.7.3 변경사항**: MOVE 핸들러·`scheduleGuidelineHide()`는 `TouchpadWrapper.kt`에 그대로 잔류한다(제스처 루프 미분해). 단 import 정리·`EdgeGeometry`/햅틱 외부 추출로 줄 번호가 이동했으므로 본 Phase가 참조하는 위치는 함수명/패턴으로 재확인할 것. 기하 계산이 필요하면 `EdgeGeometry.kt`의 순수 함수를 호출(중복 구현 금지).

**개발 기간**: 0.5일 미만

**문제**:
`TouchpadWrapper.kt`의 MOVE 이벤트 핸들러가 매 이벤트마다 수행하는 작업 목록:

1. `System.currentTimeMillis()` 다중 호출
2. `velocitySamples`/`cursorVelocitySamples` `ArrayDeque` 조작
3. `scheduleGuidelineHide()` — coroutine 취소·재생성 (`launch`)
4. 여러 `mutableStateOf` 쓰기

**수정 방향**:
- `scheduleGuidelineHide()` 내 `coroutineScope.launch` → `delay` 기반 단일 coroutine으로 교체 (취소·재생성 오버헤드 제거)
- `System.currentTimeMillis()` 중복 호출 단일 변수로 캐싱
- 이벤트 처리 중 불필요한 상태 쓰기 조건부 처리 (값이 바뀌지 않으면 쓰지 않음)

**수정 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/TouchpadWrapper.kt`

**검증**:
- [ ] Phase 4.16.1 System Trace 재측정 → `pointerInput` 람다 실행 시간 감소 확인
- [ ] 가이드라인 숨김 타이밍 동일하게 동작 확인
- [ ] 커서 이동·클릭·스크롤 기능 회귀 없음 확인
