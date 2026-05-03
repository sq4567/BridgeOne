---
title: "BridgeOne ScrollPad 컴포넌트 명세"
description: "휠 스크롤 전용 독립 입력 패드 컴포넌트. 현재 페이지 1 Actions 패널 하단에 배치되어 있으며, 다른 위치로 이동 가능."
tags: ["component", "scrollpad", "scroll", "wheel", "ui", "compose"]
version: "v1.0"
owner: "UX"
updated: "2026-05-03"
note: "본 문서에 존재하는 모든 상수값 및 설정값은 초기 값으로, 확정된 고정값이 아님"
---

# BridgeOne ScrollPad 컴포넌트 명세서

> **상세 아키텍처**: 전체 시스템 아키텍처는 [`technical-specification.md` §3]를 참조하세요.
> **스크롤 상수 참조**: 모든 튜닝 상수는 `ui/common/ScrollConstants.kt`에 집중 관리됩니다.

## 개요

- **목적**: **휠 스크롤 전용 독립 입력 패드** 컴포넌트입니다. 터치패드로 커서를 이동하면서 ScrollPad로 동시에 휠 스크롤을 입력하는 양손 동시 조작이 가능합니다.
- **현재 배치**: 페이지 1 Actions 패널 하단 (`MacrosPlaceholder` 자리 대체). 컴포넌트 자체는 특정 페이지에 종속되지 않으며, 다른 위치에도 배치 가능합니다.
- **터치패드 ScrollMode와의 관계**: 터치패드 내부의 ScrollMode 토글(엣지 스와이프 또는 모드 사이클 버튼)은 그대로 유지됩니다. ScrollPad는 **항상 활성 상태**인 보조 진입점이며, 두 진입점은 독립적으로 동작합니다. 동시에 사용해도 `FrameBuilder`의 시퀀스 자동 할당으로 프레임 충돌이 없습니다.
- **지원 축**: 수직(상하) + 수평(좌우). 드래그 시작 후 축이 자동으로 잠금됩니다.
- **스크롤 모드**: NORMAL(일반) + INFINITE(무한 관성) 두 가지. ScrollPad 내부에서 토글 가능합니다.

---

## 1. 내부 컴포넌트 구성

### 1.1 ScrollPad 래퍼 (ScrollPadWrapper)

- **파일**: `ui/components/scrollpad/ScrollPadWrapper.kt`
- **레이아웃**: `Box`, `fillMaxWidth()` + `fillMaxHeight()` (배치 컨텍스트에 따라 높이 지정 방식 변경 가능)
- **배경**: `#1A1A1A`, 라운드 코너 `12dp` (매크로 placeholder `#2A2A2A`보다 약간 어둡게 — 활성 영역 명시)
- **테두리**: 비활성 `#2A2A2A` 1dp → 드래그/관성 중 강조색(`#2196F3`)
- **터치 입력**: `pointerInput`으로 손가락 1개 드래그 감지. 멀티터치 시 무시(첫 번째 포인터만 추적).
- **이벤트 소비**: 부모 `HorizontalPager`로 터치 전파 차단 (TouchpadWrapper 패턴 준용)

### 1.2 축 가이드라인 시각화

- **컴포넌트**: `ScrollGuideline` 재사용 (`ui/components/touchpad/ScrollGuideline.kt`)
- **표시 조건**: 드래그 시작 후 축이 확정된 순간부터 표시
- **수직 잠금**: 세로 가이드라인, 일반 스크롤 초록 / 무한 스크롤 빨강 (ScrollGuideline 기존 색상 규칙 준용)
- **수평 잠금**: 가로 가이드라인, 동일 색상 규칙
- **숨김**: 손가락을 떼고 일정 시간 경과 후, 또는 관성 종료 후

### 1.3 모드 토글 칩 (NORMAL / INFINITE)

- **위치**: ScrollPadWrapper 내 우측 상단 오버레이 (`Alignment.TopEnd`, `padding(end = 6.dp, top = 6.dp)`)
- **크기**: 작은 칩 형태 (텍스트 "∞" / "≈", 또는 "INF" / "NRM"), 최소 터치 타겟 40dp × 40dp
- **동작**: 탭 시 NORMAL ↔ INFINITE 토글. 마지막 모드는 세션 내 기억 (재진입 시 유지).
- **이벤트 소비**: 칩 탭이 드래그 입력으로 오인되지 않도록 `consume()`

### 1.4 시각 상태

| 상태 | 배경 | 테두리 | 가이드라인 | 모드 칩 |
|------|------|--------|-----------|---------|
| IDLE | `#1A1A1A` | `#2A2A2A` 1dp | 숨김 | 표시 (흐리게) |
| DRAGGING | `#1E1E1E` | `#2196F3` 2dp | 표시 | 표시 |
| INERTIA | `#1A1A1A` | `#2196F3` 1dp (점선 또는 흐릿) | 표시 | 표시 |

---

## 2. 색상 시스템

### 2.1 배경 및 표면

| 요소 | 색상 | 비고 |
|------|------|------|
| 기본 배경 | `#1A1A1A` | IDLE 상태 |
| 드래그 중 표면 | `#1E1E1E` | 약간 밝게 |
| 테두리 비활성 | `#2A2A2A` 1dp | |
| 테두리 활성 | `#2196F3` 2dp | DRAGGING / INERTIA |

### 2.2 가이드라인 색상

ScrollGuideline 기존 색상 규칙 준용:
- NORMAL 스크롤: 초록 계열
- INFINITE 스크롤: 빨강 계열

### 2.3 모드 칩 색상

- NORMAL 모드: 칩 배경 `#2A2A2A`, 텍스트/아이콘 `#8A8A8A`
- INFINITE 모드: 칩 배경 `#2A2A2A`, 텍스트/아이콘 강조색 (`#FF5252` 또는 빨강 계열)

---

## 3. 상세 유저 플로우

### 3.1 초기 진입 (IDLE → DRAGGING)

1. 손가락이 ScrollPad 위에 DOWN
2. 상태 → DRAGGING (테두리 강조)
3. 드래그 시작 — 축 미확정(UNDECIDED)
4. `SCROLL_AXIS_LOCK_DISTANCE_DP`(기본값: 8dp) 초과 시 축 확정(VERTICAL / HORIZONTAL)
5. 가이드라인 표시 시작, 스크롤 단위 송출 시작

### 3.2 NORMAL 스크롤 플로우

1. 축 확정 후 드래그 중
2. `SCROLL_UNIT_DISTANCE_DP`(기본값: 15dp)마다 휠 1틱 송출
   - 수직: `ClickDetector.createWheelFrame(wheelDelta)` — `wheelDelta = (-direction).toByte()`
   - 수평: `ClickDetector.createHorizontalWheelFrame(wheelDelta)`
3. 프레임 게이트: `SCROLL_FRAME_MIN_INTERVAL_MS`(기본값: 8ms) 최소 간격, `SCROLL_MAX_FRAMES_PER_EVENT`(기본값: 3) 최대 누적
4. 방향 부스트 적용: `ScrollDirectionBoost` 배율 (DOWN_MULTIPLIER 등)
5. 손가락 UP → 상태 IDLE, 가이드라인 페이드아웃

### 3.3 INFINITE 스크롤 플로우

1. 드래그 중 속도 샘플링 (최근 N ms 윈도우 평균 속도)
2. 손가락 UP → 즉시 관성 코루틴 시작 (상태 INERTIA)
3. 지수 감쇠: `v(t) = v₀ × exp(-t / τ)`, τ = `INFINITE_SCROLL_TIME_CONSTANT_MS`(기본값: 1500f ⚠️ 의도적 변경)
4. 매 dt마다 이동량 누적 → 단위 거리 초과 시 휠 프레임 송출 (방향 부스트 포함)
5. 속도가 최소 임계값 이하로 감소하면 관성 종료 → 상태 IDLE
6. 관성 중 DOWN → 즉시 관성 코루틴 취소, 상태 DRAGGING

### 3.4 모드 칩 토글

1. 칩 탭 → NORMAL ↔ INFINITE 전환
2. 현재 모드 상태가 칩 텍스트/색상에 즉시 반영
3. 관성 진행 중 모드 칩 탭 → 관성 취소 후 모드 전환

---

## 4. 상세 알고리즘 로직

### 4.1 축 결정

`DeltaCalculator.determineRightAngleAxis(dx, dy)` 재사용.
- 드래그 누적 거리가 `SCROLL_AXIS_LOCK_DISTANCE_DP` 초과 시 `RightAngleAxis.VERTICAL` 또는 `HORIZONTAL` 확정
- 일단 확정된 축은 손가락을 뗄 때까지 유지 (재결정 없음)

### 4.2 단위 송출 루프

`TouchpadWrapper.kt:925-957` 패턴을 ScrollPadWrapper 내부에 추출·적용:

```kotlin
// 누적 이동량이 SCROLL_UNIT_DISTANCE_PX 이상이면 휠 전송
while (abs(accumulated) >= unitPx) {
    val direction = if (accumulated > 0f) 1 else -1
    val boost = ScrollDirectionBoost.getBoost(direction, axis)
    val wheelDelta = ((-direction) * boost).toInt().coerceIn(-127, 127).toByte()
    val frame = if (axis == VERTICAL) createWheelFrame(wheelDelta)
                else createHorizontalWheelFrame(wheelDelta)
    ClickDetector.sendFrame(frame)
    accumulated -= direction * unitPx
}
```

### 4.3 관성 루프

`TouchpadWrapper.kt:1169-1217` 패턴 추출·적용:

```kotlin
inertiaJob = coroutineScope.launch {
    var velocity = capturedVelocity  // px/ms
    var lastTime = System.currentTimeMillis()
    var accumulated = 0f

    while (isActive && abs(velocity) > MIN_VELOCITY_THRESHOLD) {
        val now = System.currentTimeMillis()
        val dt = (now - lastTime).coerceAtLeast(1L)
        lastTime = now

        velocity *= exp(-dt.toFloat() / INFINITE_SCROLL_TIME_CONSTANT_MS)
        accumulated += velocity * dt

        // 단위 초과 시 휠 프레임 송출 (§4.2 루프 재사용)
        sendWheelFrames(accumulated, axis)

        delay(SCROLL_FRAME_MIN_INTERVAL_MS)
    }
}
```

### 4.4 휠 프레임 전송

- `ClickDetector.createWheelFrame(wheelDelta: Byte)` — 수직 휠
- `ClickDetector.createHorizontalWheelFrame(wheelDelta: Byte)` — 수평 휠
- 부호 규약: `wheelDelta = (-direction).toByte()` (손가락 방향과 역방향으로 PC 기본 스크롤 방향과 일치)
- HID 프로토콜 변경 없음: `BridgeFrame.wheel` 단일 바이트(인덱스 4) 그대로 사용

---

## 5. 상수/임계값

ScrollPad는 **별도 상수 파일을 두지 않고** `ui/common/ScrollConstants.kt`를 그대로 재사용합니다. (CLAUDE.md "Android 상수 중앙화 원칙" 준수)

| 상수 | 파일 위치 | 기본값 | 용도 |
|------|----------|--------|------|
| `SCROLL_UNIT_DISTANCE_DP` | `ScrollConstants.kt` | 15f | 휠 1틱 기준 드래그 거리 |
| `SCROLL_AXIS_LOCK_DISTANCE_DP` | `ScrollConstants.kt` | 8f | 축 확정 임계값 |
| `INFINITE_SCROLL_TIME_CONSTANT_MS` | `ScrollConstants.kt` | 1500f ⚠️ 의도적 변경 | 관성 감쇠 시간 상수 |
| `SCROLL_FRAME_MIN_INTERVAL_MS` | `ScrollConstants.kt` | 8L | 프레임 최소 간격 |
| `SCROLL_MAX_FRAMES_PER_EVENT` | `ScrollConstants.kt` | 3 | 이벤트당 최대 프레임 |
| `ScrollDirectionBoost.*` | `ScrollConstants.kt` | DOWN: 2.0f 등 | 방향별 속도 배율 |

신규 상수가 필요한 경우 `ScrollConstants.kt`에 `SCROLL_PAD_` 접두사로 추가하고 기본값 주석을 명시합니다.
