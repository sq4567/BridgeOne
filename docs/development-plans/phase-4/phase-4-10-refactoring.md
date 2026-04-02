---
title: "BridgeOne Phase 4.10: 리팩터링"
description: "BridgeOne 프로젝트 Phase 4.10 - 조정 가능 상수 중앙화 및 Deprecated API 교체"
tags: ["android", "refactoring", "constants", "maintainability"]
version: "v1.2"
owner: "Chatterbones"
updated: "2026-04-01"
---

# BridgeOne Phase 4.10: 리팩터링

**개발 기간**: 미정

**목표**: 코드 유지보수성 개선을 다루는 리팩터링 Phase.

| 하위 Phase | 내용 | 상태 |
|-----------|------|------|
| 4.10.1 | 조정 가능 상수 중앙화 | 미시작 |
| 4.10.2 | Deprecated API 교체 | 미시작 |

**선행 조건**: Phase 4.9 완료

---

## Phase 4.10.1: AppConstants.kt 신규 생성 및 상수 통합

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

> **⚠️ Phase 4.3.6 변경사항**: `DpiAdjustPopup.kt`에 조정 가능 상수 추가됨 (`DPI_ADJUST_STEP_DP`, `DPI_MIN`, `DPI_MAX`, `DPI_STEP`, `TAP_THRESHOLD_DP`). `StandardModePage.kt`에 SharedPreferences 키 상수 추가됨 (`PREF_NAME`, `KEY_DPI_LEVEL`). 전수 조사 시 이 파일들도 포함해야 함.

> **이전하지 않는 상수**: `HidConstants.kt`(USB HID 규격 고정값), `UsbConstants.kt`(VID/PID·UART 설정 등 하드웨어·프로토콜 고정값)는 조정 대상이 아니므로 건드리지 않습니다.

**검증**:
- [ ] 빌드 성공
- [ ] 실기기에서 터치패드·스크롤·클릭 동작 정상 확인 (값 변경 없음)

---

## Phase 4.10.2: Deprecated API 교체

**개발 기간**: 0.5일 미만

**목표**: 빌드 시 발생하는 deprecated API 경고를 제거합니다. 기능 변경 없이 내부 구현만 최신 API로 교체하는 순수 리팩터링입니다.

**에뮬레이터 호환성**: 에뮬레이터에서 빌드 및 검증 가능.

### 대상 경고 목록

| 파일 | 라인 | 현재 사용 | 교체 대상 | 비고 |
|------|------|----------|----------|------|
| `HidConstants.kt` | 143 | `Context.VIBRATOR_SERVICE` | `Context.getSystemService(Vibrator::class.java)` 또는 `VibratorManager` (API 31+) | API 31 미만은 기존 방식 폴백 필요 |
| `UsbPermissionReceiver.kt` | 75 | `Intent.getExtra(String)` | `Intent.getParcelableExtra(String, Class)` (API 33+) | API 33 미만은 기존 방식 폴백 필요 |

> **참고**: 최소 SDK 24를 지원하므로 두 경우 모두 `Build.VERSION.SDK_INT` 분기로 신/구 API를 분기해야 합니다.

### 구현 절차

1. **`HidConstants.kt`** — `VIBRATOR_SERVICE` 사용부를 API 31+ 분기로 교체
2. **`UsbPermissionReceiver.kt`** — `getExtra(String)` 사용부를 API 33+ 분기로 교체
3. **빌드 후 경고 0건 확인**

**검증**:
- [ ] 빌드 경고 0건
- [ ] 실기기에서 햅틱 피드백 정상 동작
- [ ] USB 권한 요청 및 연결 정상 동작
