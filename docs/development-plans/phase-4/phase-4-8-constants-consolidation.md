---
title: "BridgeOne Phase 4.8: 리팩터링"
description: "BridgeOne 프로젝트 Phase 4.8 - 조정 가능 상수 중앙화 + 터치패드 엣지 스와이프 제스처 탐색"
tags: ["android", "refactoring", "constants", "maintainability", "ux", "gesture"]
version: "v1.1"
owner: "Chatterbones"
updated: "2026-03-30"
---

# BridgeOne Phase 4.8: 리팩터링

**개발 기간**: 미정

**목표**: 코드 유지보수성 개선 + 접근성 UX 개선을 함께 다루는 리팩터링 Phase.

| 하위 Phase | 내용 | 상태 |
|-----------|------|------|
| 4.8.1 | 조정 가능 상수 중앙화 | 미시작 |
| 4.8.2 | 터치패드 엣지 스와이프 제스처 | 설계 검토 중 |

**선행 조건**: Phase 4.7 완료

---

## Phase 4.8.1: AppConstants.kt 신규 생성 및 상수 통합

**개발 기간**: 0.5일

**목표**: 실기기 테스트나 취향에 따라 조정할 수 있는 모든 값이 여러 파일에 흩어져 있어 찾기 불편한 문제를 해결합니다. `AppConstants.kt` 하나만 열면 모든 조정 가능한 값을 한눈에 보고 수정할 수 있도록 통합합니다.

**핵심 성과물**:
- `AppConstants.kt` 신규 생성 (`ui/common/`) — 모든 조정 가능 상수를 섹션별로 정리
- `LayoutConstants.kt`, `ScrollConstants.kt` 삭제 (내용이 `AppConstants.kt`로 흡수됨)
- `ClickDetector.kt`, `DeltaCalculator.kt`, `TouchpadWrapper.kt` 내 조정 가능 상수 이전
- 동작 변경 없음 — 값과 이름 유지, 참조 위치만 변경

**에뮬레이터 호환성**: 전체 에뮬레이터에서 개발 가능.

### AppConstants.kt 파일 구조

```
ui/common/AppConstants.kt
```

```kotlin
package com.bridgeone.app.ui.common

import androidx.compose.ui.unit.dp

// ============================================================
// AppConstants — 조정 가능한 모든 값을 한 곳에서 관리합니다.
//
// 실기기 테스트 후 값을 바꿀 때는 이 파일만 열면 됩니다.
// ============================================================

object AppConstants {

    // ── 레이아웃 Safe Zone ──────────────────────────────────
    object Layout {
        val TOP_SAFE_ZONE    = 40.dp
        val BOTTOM_SAFE_ZONE = 40.dp
    }

    // ── 비주얼 ──────────────────────────────────────────────
    object Visual {
        val TOUCHPAD_CORNER_RADIUS = 5.dp
    }

    // ── 인터랙션: 클릭 감지 ──────────────────────────────────
    object Click {
        const val MAX_DURATION_MS  = 500L   // ms: 이내 → 좌클릭, 초과 → 우클릭(롱터치)
        const val MAX_MOVEMENT_DP  = 15f    // dp: 이내 이동이면 클릭으로 판정
    }

    // ── 인터랙션: 데드존 ─────────────────────────────────────
    object Interaction {
        val DEAD_ZONE_THRESHOLD = 5.dp      // dp: 이 거리 이상 이동해야 드래그 인식
    }

    // ── 스크롤 ───────────────────────────────────────────────
    object Scroll {
        // 스크롤 축 확정
        const val AXIS_LOCK_DISTANCE_DP        = 8f    // dp: 이 거리 전까지는 UNDECIDED 유지
        const val AXIS_DIAGONAL_DEAD_ZONE_DEG  = 15f   // °: 45° ±이 범위 = 대각선 판정

        // 스크롤 단위 및 감도
        const val UNIT_DISTANCE_DP             = 20f   // dp: 1단위 스크롤에 필요한 이동량
        const val SENSITIVITY_SLOW             = 0.5f
        const val SENSITIVITY_NORMAL           = 1.0f
        const val SENSITIVITY_FAST             = 2.0f

        // 스크롤 가이드라인
        const val GUIDELINE_STEP_DP            = 20f   // dp: 1단위 전송 시 가이드라인 이동량
        const val GUIDELINE_SPACING_DP         = 40f   // dp: 가이드라인 선 간격
        const val GUIDELINE_HIDE_DELAY_MS      = 800L  // ms: 스크롤 정지 후 숨김 대기
        const val GUIDELINE_SPRING_STIFFNESS   = 10_000f
        const val GUIDELINE_SPRING_DAMPING     = 1.0f
        const val GUIDELINE_AXIS_ROTATION_MS   = 100   // ms: 축 전환 회전 애니메이션

        // 스크롤 정지 판정
        const val STOP_THRESHOLD_MS            = 150L  // ms: 이 시간 입력 없으면 정지 판정

        // 무한 스크롤 관성
        const val INFINITE_TIME_CONSTANT_MS    = 800f  // ms: 속도 1/e로 감소하는 시간 상수
        const val INFINITE_MIN_VELOCITY_DP_MS  = 0.08f // dp/ms: 이 미만이면 관성 종료
        const val INFINITE_VELOCITY_WINDOW_MS  = 100L  // ms: 초기 속도 계산 샘플 윈도우
        const val INFINITE_HAPTIC_MAX_VEL      = 2.0f  // dp/ms: 이 속도 이상 → 진동 최대

        // 일반 스크롤 버튼
        const val NORMAL_BUTTON_INTERVAL_MS    = 100L  // ms: 홀드 시 전송 간격

        // 직각 이동 모드
        const val RIGHT_ANGLE_AXIS_LOCK_DP     = 12f   // dp: 주축 확정 누적 이동 임계값
        const val RIGHT_ANGLE_DEADBAND_DEG     = 22.5f // °: 45° ±이 범위 = 대각선 판정
    }
}
```

### 구현 절차

1. **코드베이스 전수 조사** — 작업 시점의 모든 `.kt` 파일을 훑어 조정 가능한 값을 파악
2. **`AppConstants.kt` 생성** — 위 섹션 구조를 뼈대로 삼아 `ui/common/AppConstants.kt` 작성
3. **기존 상수 파일에서 조정 가능 상수 이전** — 각 파일에서 상수 선언을 제거하고 `AppConstants.XXX` 참조로 교체; 상수 선언만 담던 파일은 삭제
4. **인라인·로컬 상수 이전** — 각 컴포넌트 내부에 박혀 있는 조정 가능한 리터럴·로컬 상수를 `AppConstants`로 옮기고 참조로 교체

> **이전하지 않는 상수**: `HidConstants.kt`(USB HID 규격 고정값), `UsbConstants.kt`(VID/PID·UART 설정 등 하드웨어·프로토콜 고정값)는 조정 대상이 아니므로 건드리지 않습니다.

**검증**:
- [ ] 빌드 성공
- [ ] 실기기에서 터치패드·스크롤·클릭 동작 정상 확인 (값 변경 없음)

---

## Phase 4.8.2: 터치패드 엣지 스와이프 제스처

> **상태**: 설계 검토 중. 구현 방향이 결정되면 세부 절차를 추가합니다.

**목표**: 터치패드 상단에 고정된 `ControlButtonContainer`에 손이 닿기 어렵다는 접근성 문제를 해결합니다. 버튼을 대체하는 것이 아닌, **엣지 스와이프**라는 보조 입력 방식을 추가해 손을 크게 움직이지 않고도 모드를 전환할 수 있도록 합니다.

**배경 아이디어**: Android 제스처 내비게이션처럼, 화면 가장자리에서 안쪽으로 스와이프하는 동작으로 특정 기능을 트리거. 어느 가장자리에서 스와이프하느냐에 따라 서로 다른 모드를 전환합니다.

**에뮬레이터 호환성**: 에뮬레이터에서 기능 확인 가능. 실기기에서 느낌 검증 필요.

### 현재 버튼 목록 (변경 대상)

| 버튼 | 기능 | 현재 위치 |
|------|------|----------|
| 클릭 모드 버튼 | 좌클릭 ↔ 우클릭 전환 | 상단 좌측 1번 (커서 이동 시만 표시) |
| 이동 모드 버튼 | 자유이동 ↔ 직각이동 전환 | 상단 좌측 2번 (커서 이동 시만 표시) |
| 스크롤 버튼 | 스크롤 ON/OFF, 롱프레스=일반↔무한 전환 | 상단 좌측 3번 (항상 표시) |
| 커서 버튼 | 싱글 ↔ 멀티 커서 | 상단 좌측 4번 |
| DPI / 스크롤 감도 버튼 | 수치 3단계 순환 | 상단 우측 (슬롯 공유) |

### 핵심 설계 질문 (결정 필요)

아래 질문들에 대한 답이 나와야 구체적인 구현 절차를 작성할 수 있습니다.

#### Q1. 어떤 엣지에 어떤 모드를 배정할까?

현재 후보 배정안 (확정 아님):

| 엣지 | 스와이프 방향 | 제안 동작 | 이유 |
|------|------------|---------|------|
| **왼쪽** | 오른쪽으로 스와이프 | 스크롤 모드 ON/OFF 토글 | 가장 자주 쓰는 전환, 엄지손가락 접근 용이 |
| **오른쪽** | 왼쪽으로 스와이프 | 클릭 모드 토글 (좌↔우) | 검지/중지 자연스러운 위치 |
| **하단** | 위로 스와이프 | 이동 모드 토글 (자유↔직각) | 상단보다 접근 쉬움 |
| **상단** | 아래로 스와이프 | 스크롤 일반↔무한 전환 | 기존 버튼 롱프레스 대체 |

> **DPI / 스크롤 감도**는 엣지 1회 스와이프로 3단계 중 1단계씩 이동하기에는 부자연스러울 수 있습니다. 엣지 배정 보류 후 별도 검토 가능.

#### Q2. 스와이프 인식 범위와 진입 임계값

- **진입 영역 너비**: 엣지에서 얼마나 안쪽까지를 "엣지 영역"으로 볼 것인가? (예: 가장자리에서 24dp 이내 시작한 터치만 엣지 스와이프로 인식)
- **트리거 거리**: 얼마나 스와이프해야 동작이 발동되는가? (예: 48dp 이상 스와이프 시 즉시 발동, 손가락 떼기 전)
- **일반 터치패드 입력과 충돌 방지**: 커서 이동 중 실수로 엣지에 닿았을 때 오발동 방지 기준이 필요

#### Q3. 시각 피드백

- 스와이프 진행 중: 반투명 인디케이터 스트립 + 목표 모드 레이블 표시 (Android 백 제스처 호와 유사)
- 발동 완료: StatusToast 재활용 ("스크롤 ON" 등), 햅틱 피드백
- 엣지 영역 힌트: 터치패드 가장자리에 희미한 그라디언트나 점선을 항상 표시할지, 아예 숨길지

#### Q4. 기존 버튼과의 공존 방식

- 버튼과 엣지 스와이프 모두 같은 `TouchpadState`를 변경 → 한쪽에서 바꾸면 다른 쪽도 동기화
- 버튼은 제거하지 않음 — 엣지 스와이프에 익숙하지 않은 경우 버튼으로 폴백
- 장기적으로 버튼 영역을 줄이거나 숨기는 옵션을 추가할 수도 있음 (이 Phase 범위 밖)

### 구현 스케치 (설계 확정 후 구체화)

설계 질문이 결정되면 아래 구조로 구현할 예정입니다.

#### 새로운 파일

```
ui/components/touchpad/EdgeSwipeDetector.kt   — 엣지 스와이프 인식 로직
```

#### `TouchpadWrapper.kt` 수정

현재 `TouchpadWrapper`가 전체 터치패드 영역의 `Modifier.pointerInput`을 관리합니다. 엣지 스와이프는 이 레이어 위에 별도 `pointerInput`을 추가하거나, 기존 gesture 파이프라인 안에서 엣지 진입 조건을 먼저 판별하는 방식으로 처리합니다.

```kotlin
// 대략적인 엣지 인식 흐름 (구현 확정 전 의사 코드)
// 1. 터치 DOWN 이벤트: x < EDGE_WIDTH_PX → 왼쪽 엣지 후보 플래그 세우기
// 2. 터치 MOVE 이벤트: 후보 플래그 ON + deltaX > TRIGGER_DISTANCE_PX → 스와이프 발동
//    → 해당 모드 토글, 이후 이 터치 포인터는 일반 커서 이동 이벤트로 넘기지 않음
// 3. 후보 플래그 ON + deltaY 또는 다른 방향으로 이탈 → 후보 플래그 취소, 일반 커서 이동 처리
```

#### `AppConstants.kt` 추가 상수 (4.8.1과 연동)

```kotlin
// ── 엣지 스와이프 ────────────────────────────────────────────
object EdgeSwipe {
    const val EDGE_HIT_WIDTH_DP   = 24f   // dp: 가장자리에서 이 폭 이내 시작해야 엣지 스와이프
    const val TRIGGER_DISTANCE_DP = 48f   // dp: 이 이상 스와이프해야 발동
}
```

### 남은 설계 결정 체크리스트

- [ ] 엣지-모드 배정 확정 (Q1)
- [ ] 진입 영역 너비 및 트리거 거리 수치 확정 (Q2)
- [ ] 시각 피드백 방식 선택 (Q3)
- [ ] 기존 버튼 공존 정책 확정 (Q4)
- [ ] 위 결정 후 구현 절차 작성 및 이 섹션 업데이트
