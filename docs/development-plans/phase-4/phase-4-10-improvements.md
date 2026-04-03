---
title: "BridgeOne Phase 4.10: 개선 필요 사항 반영"
description: "BridgeOne 프로젝트 Phase 4.10 - 개발 과정에서 발견된 개선 필요 사항 반영"
tags: ["android", "improvement", "ux", "bugfix"]
version: "v1.0"
owner: "Chatterbones"
updated: "2026-04-03"
---

# BridgeOne Phase 4.10: 개선 필요 사항 반영

**개발 기간**: 미정

**목표**: 개발 진행 과정에서 발견된 개선 필요 사항을 반영합니다.

| 하위 Phase | 내용 | 상태 |
|-----------|------|------|
| 4.10.1 | 엣지 스와이프 2단계 제스처 UX 개선 | 미시작 |

**선행 조건**: Phase 4.9 완료

---

## Phase 4.10.1: 엣지 스와이프 2단계 제스처 UX 개선

**개발 기간**: 1일

**목표**: 현재 "스와이프 중에 손 떼기로 모드 확정"하는 방식을, "스와이프로 메뉴만 열고 → 손을 한 번 뗀 후 → 두 번째 제스처로 선택·확정"하는 2단계 방식으로 전환합니다. 기존 방식과 새 방식을 상수 하나로 즉시 전환하여 비교 테스트할 수 있습니다.

**에뮬레이터 호환성**: 전체 에뮬레이터에서 개발 가능.

### 현재 동작 vs 새 동작

| 단계 | 현재 (1단계) | 변경 후 (2단계) |
|------|------------|----------------|
| ① | 엣지에서 안쪽으로 스와이프 | 엣지에서 안쪽으로 스와이프 |
| ② | 손가락을 원하는 모드 위로 이동 | 트리거 거리 도달 → 팝업 등장 |
| ③ | **손을 뗌 → 모드 확정·팝업 닫힘** | **손을 뗌 → 팝업 고정 (메뉴 유지)** |
| ④ | — | 다시 손가락을 올려 원하는 모드 위로 이동 |
| ⑤ | — | **탭으로 확정** |
| 취소 | 진입 엣지로 되돌리기 | 팝업 고정 전: 진입 엣지로 되돌리기<br>팝업 고정 후: 안쪽→바깥쪽 스와이프 |

### 추가 상수 (`ScrollConstants.kt` — `EdgeSwipeConstants`)

```kotlin
object EdgeSwipeConstants {
    // ... 기존 상수 유지 ...

    // ── A/B 비교 테스트 ───────────────────────────────────────
    const val LEGACY_GESTURE_MODE = false
    // true  → 기존 방식 (손 뗌 = 확정)
    // false → 새 방식 (손 뗌 = 고정, 탭 = 확정)

    // ── 2단계 제스처 (LEGACY_GESTURE_MODE = false 시 사용) ─────
    const val PINNED_TAP_MAX_DISTANCE_DP = 12f   // dp: 이 이내 이동이면 탭으로 판정
    const val PINNED_TAP_MAX_DURATION_MS = 400L  // ms: 이 이내 터치 유지면 탭으로 판정
    const val PINNED_CLOSE_DISTANCE_DP   = 60f   // dp: 바깥쪽으로 이 이상 스와이프 시 닫기
}
```

### 상태 변수 추가 (`TouchpadWrapper.kt`)

```kotlin
// 기존 엣지 스와이프 상태 (유지)
var showEdgePopup    by remember { mutableStateOf(false) }
var isEdgeCandidate  by remember { mutableStateOf(false) }
var hoveredMode      by remember { mutableStateOf<EdgeSwipeMode?>(null) }
// ... entryEdge, fingerAlongEdgePx, inwardDistancePx, bumpShrinkAnimatable ...

// 신규 — 고정 상태 (2단계 제스처)
var edgeMenuPinned   by remember { mutableStateOf(false) }
var pinnedDownX      by remember { mutableStateOf(0f) }
var pinnedDownY      by remember { mutableStateOf(0f) }
var pinnedDownTimeMs by remember { mutableStateOf(0L) }
var pinnedCurX       by remember { mutableStateOf(0f) }
var pinnedCurY       by remember { mutableStateOf(0f) }
```

### 이벤트 처리 변경 (`TouchpadWrapper.kt`)

#### DOWN 이벤트

```
// 기존 분기 순서 유지 — edgeMenuPinned 분기를 showEdgePopup 분기보다 먼저 배치
if (edgeMenuPinned):
    pinnedDownX = event.x ; pinnedDownY = event.y
    pinnedDownTimeMs = System.currentTimeMillis()
    pinnedCurX = event.x ; pinnedCurY = event.y
    consume()

else if (isEdgeCandidate / showEdgePopup / 일반 커서):
    // 기존 로직 그대로
```

#### MOVE 이벤트

```
if (edgeMenuPinned):
    pinnedCurX = event.x ; pinnedCurY = event.y
    // hoveredMode 갱신 — 기존 calculateHoveredEdgeMode() 재사용
    hoveredMode = calculateHoveredEdgeMode(pinnedCurX, pinnedCurY, visibleModes, buttonRects)
    consume()
```

#### UP 이벤트

```
// ① 팝업 등장 중 (showEdgePopup == true, edgeMenuPinned == false)
if (showEdgePopup && !edgeMenuPinned):
    if (LEGACY_GESTURE_MODE):
        // 기존: 손 뗌 = 확정
        applySelectedMode(hoveredMode)
        resetEdgeSwipeState()
    else:
        // 신규: 손 뗌 = 팝업 고정
        edgeMenuPinned = true
        // showEdgePopup, hoveredMode 유지 — 팝업 그대로 남음
        // bumpShrinkAnimatable 수축은 트리거 시 이미 시작됨 (추가 불필요)

// ② 고정 상태 (edgeMenuPinned == true)
if (edgeMenuPinned):
    val dx = pinnedCurX - pinnedDownX
    val dy = pinnedCurY - pinnedDownY
    val moveDist = sqrt(dx * dx + dy * dy)
    val duration = System.currentTimeMillis() - pinnedDownTimeMs

    val isTap = moveDist < PINNED_TAP_MAX_DISTANCE_PX
             && duration < PINNED_TAP_MAX_DURATION_MS

    if (isTap && hoveredMode != null):
        applySelectedMode(hoveredMode)          // 모드 적용
        resetEdgeSwipeState()                   // 팝업 닫기

    else if (!isTap && isOutwardSwipe(pinnedDownX, pinnedDownY, pinnedCurX, pinnedCurY,
                                      touchpadWidthPx, touchpadHeightPx,
                                      PINNED_CLOSE_DISTANCE_PX)):
        resetEdgeSwipeState()                   // 취소, 팝업 닫기

    // else: 손만 뗀 것 (다음 제스처 대기) — 아무 동작 없음
```

#### `isOutwardSwipe()` private 헬퍼 추가 (TouchpadWrapper 하단)

```kotlin
// 스와이프가 "바깥쪽"(가장 가까운 엣지 방향)인지 판정
private fun isOutwardSwipe(
    startX: Float, startY: Float,
    endX: Float, endY: Float,
    width: Float, height: Float,
    minDistPx: Float
): Boolean {
    val dx = endX - startX
    val dy = endY - startY
    if (sqrt(dx * dx + dy * dy) < minDistPx) return false

    // 출발 위치에서 가장 가까운 엣지 방향
    val toLeft   = startX
    val toRight  = width - startX
    val toTop    = startY
    val toBottom = height - startY
    val nearestDist = minOf(toLeft, toRight, toTop, toBottom)

    return when (nearestDist) {
        toLeft   -> dx < 0
        toRight  -> dx > 0
        toTop    -> dy < 0
        else     -> dy > 0   // toBottom
    }
}
```

#### `resetEdgeSwipeState()` 통합 헬퍼 — 기존 분산 초기화를 하나로 묶기

```kotlin
// 기존 코드가 곳곳에 흩어진 reset 로직을 이 함수로 통합
fun resetEdgeSwipeState() {
    showEdgePopup    = false
    isEdgeCandidate  = false
    edgeMenuPinned   = false
    hoveredMode      = null
    entryEdge        = null
    fingerAlongEdgePx = 0f
    inwardDistancePx  = 0f
    pinnedDownX = 0f ; pinnedDownY = 0f
    pinnedCurX  = 0f ; pinnedCurY  = 0f
}
```

> **주의**: 기존 코드에서 취소/확정 시 상태를 초기화하던 부분을 `resetEdgeSwipeState()` 호출로 교체할 것. 누락 시 상태 오염 발생.

### 팝업 안내 텍스트 변경 (`EdgeSwipeOverlay.kt`)

`EdgeSwipeOverlay`에 `isPinned: Boolean` 파라미터 추가 후:

| 상태 | `isPinned` | 안내 텍스트 |
|------|-----------|------------|
| 팝업 등장 중 (스와이프 유지) | `false` | 새 방식: **"손을 떼면 메뉴가 고정됩니다"**<br>기존 방식: "원하는 모드로 이동 후 손 떼기 / 들어온 가장자리로 되돌리면 취소" |
| 메뉴 고정 후 (핀 상태) | `true` | **"원하는 모드를 탭하세요 / 바깥쪽으로 스와이프하면 닫힘"** |

`TouchpadWrapper.kt`에서 `EdgeSwipeOverlay` 호출 시 `isPinned = edgeMenuPinned` 전달.

### 수정 파일

- `src/android/app/src/main/java/com/bridgeone/app/ui/common/ScrollConstants.kt`
  — `EdgeSwipeConstants`에 `LEGACY_GESTURE_MODE`, `PINNED_TAP_*`, `PINNED_CLOSE_DISTANCE_DP` 추가
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/TouchpadWrapper.kt`
  — `edgeMenuPinned` 등 신규 상태 추가, UP/DOWN/MOVE 이벤트 분기 수정, `isOutwardSwipe()` 헬퍼 추가, `resetEdgeSwipeState()` 헬퍼 추가
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/touchpad/EdgeSwipeOverlay.kt`
  — `isPinned: Boolean` 파라미터 추가, 안내 텍스트 분기

**검증**:

**새 방식 (`LEGACY_GESTURE_MODE = false`)**:
- [ ] 엣지 스와이프 → 트리거 → 손 뗌 → 팝업이 닫히지 않고 고정 확인
- [ ] 고정 상태에서 다시 터치 후 이동 시 hoveredMode 하이라이트 갱신 확인
- [ ] 고정 상태에서 탭 → 모드 적용 + 팝업 닫힘 확인
- [ ] 고정 상태에서 바깥쪽 스와이프(60dp 이상) → 취소·팝업 닫힘 확인 (모드 미변경)
- [ ] 고정 상태에서 단순히 손만 뗌(탭 거리 초과, 바깥 스와이프 미달) → 팝업 유지 확인
- [ ] 팝업 고정 전 취소(진입 엣지로 되돌리기) → 기존과 동일하게 동작 확인
- [ ] 안내 텍스트가 고정 전/후 올바르게 전환되는지 확인

**기존 방식 (`LEGACY_GESTURE_MODE = true`)**:
- [ ] 엣지 스와이프 → 트리거 → 손 뗌 → 즉시 모드 확정 확인 (기존 동작 보존)
- [ ] 진입 엣지로 되돌리기 → 취소 확인 (기존 동작 보존)
