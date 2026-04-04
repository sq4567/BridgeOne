---
title: "BridgeOne Phase 4.6: 엣지 스와이프 대안 조작 방식 탐색"
description: "BridgeOne 프로젝트 Phase 4.6 - 터치패드 모드/옵션 전환을 위한 다양한 조작 방식을 구현·비교하여 최적의 UX를 선정"
tags: ["android", "edge-swipe", "gesture", "ux", "interaction", "pie-menu", "flick", "zone", "drawing"]
version: "v1.0"
owner: "Chatterbones"
updated: "2026-04-03"
---

# BridgeOne Phase 4.6: 엣지 스와이프 대안 조작 방식 탐색

**개발 기간**: 4-5일 (각 방식 0.5~1일)

**목표**: 현재 엣지 스와이프 팝업의 사용성 한계(5단계 조작)를 극복하기 위해, **여러 대안 조작 방식을 모두 구현**한 뒤 실제 사용 비교 테스트를 통해 최적의 방식을 선정합니다.

**배경**: 현재 엣지 스와이프 → 팝업 모드 선택(SWIPE/DIRECT_TOUCH) → 모드 버튼 선택 → 탭 토글 → 확인 버튼의 5단계를 거쳐야 모드 전환이 가능합니다. "우클릭 모드로 바꾸고 싶다" 같은 단순한 작업에도 5번의 조작이 필요해 사용성이 떨어집니다.

| 하위 Phase | 내용 | 상태 |
|-----------|------|------|
| 4.6.1 | 엣지 존(Zone) 분할 방식 | 미시작 |
| 4.6.2 | 파이 메뉴(Radial Menu) 방식 | 미시작 |
| 4.6.3 | 방향 플릭(Flick) 방식 | 미시작 |
| 4.6.4 | 제스처 드로잉 인식 방식 | 미시작 |
| 4.6.5 | 비교 테스트 및 최종 선정 | 미시작 |

**선행 조건**: Phase 4.5 (E2E 테스트 수정사항) 완료

**에뮬레이터 호환성**: 모든 하위 Phase 에뮬레이터에서 개발 가능. 최종 비교 테스트(4.6.5)는 실기기 권장.

**공통 전제**:
- 기존 엣지 스와이프 인프라(산봉우리 시각화, 엣지 감지, `EdgeSwipeConstants`)를 최대한 재사용
- 각 방식은 독립적인 `EdgeInteractionMode` enum 값으로 구분하여, 설정에서 원하는 방식을 선택 가능하게 구현
- 모든 방식에서 조절 가능한 모드/옵션: ClickMode, MoveMode, ScrollMode, DpiLevel, DynamicsPreset, ModePreset

---

## Phase 4.6.1: 엣지 존(Zone) 분할 방식

**개발 기간**: 1일

**쉬운 설명**: 터치패드 가장자리를 여러 구역으로 나눠서, 각 구역에서 손가락을 안쪽으로 밀면 해당 구역에 배정된 모드가 바로 토글되거나, 상세 설정 팝업이 뜨는 방식입니다. 예를 들어 왼쪽 위 모서리에서 밀면 우클릭 전환, 왼쪽 아래에서 밀면 스크롤 토글 같은 식입니다.

### 핵심 설계

**존 정의 구조**:
```kotlin
data class EdgeZone(
    val edge: EntryEdge,           // TOP, BOTTOM, LEFT, RIGHT
    val startRatio: Float,         // 엣지 시작 비율 (0.0~1.0)
    val endRatio: Float,           // 엣지 끝 비율 (0.0~1.0)
    val action: EdgeZoneAction,    // 이 존에서 트리거할 동작
    val label: String,             // 사용자에게 보여줄 라벨
    val icon: ImageVector          // 시각적 아이콘
)

sealed class EdgeZoneAction {
    data class ToggleMode(val mode: EdgeSwipeMode) : EdgeZoneAction()     // 즉시 토글
    data class OpenSettings(val settingsType: SettingsType) : EdgeZoneAction()  // 상세 설정 팝업
}
```

**기본 존 프리셋** (사용자 커스터마이징 가능):

| 엣지 | 구간 | 동작 | 시각적 힌트 |
|------|------|------|------------|
| LEFT 상단 (0.0~0.5) | 클릭 모드 토글 | 좌↔우 | 마우스 아이콘 |
| LEFT 하단 (0.5~1.0) | 스크롤 모드 토글 | ON↔OFF | 스크롤 아이콘 |
| RIGHT 상단 (0.0~0.5) | 이동 모드 토글 | 자유↔직각 | 화살표 아이콘 |
| RIGHT 하단 (0.5~1.0) | DPI 설정 팝업 | 팝업 | 속도 아이콘 |
| TOP 좌측 (0.0~0.5) | 다이나믹스 프리셋 사이클 | 순환 | 곡선 아이콘 |
| TOP 우측 (0.5~1.0) | 모드 프리셋 사이클 | 순환 | 프리셋 아이콘 |
| BOTTOM | (사용자 커스텀 영역) | 미할당 | — |

**동작 흐름**:
1. 엣지에서 안쪽으로 드래그 시작 → 산봉우리 등장 + 해당 존의 라벨/아이콘 표시
2. `TRIGGER_DISTANCE_DP` 도달 → 햅틱 피드백 + 동작 실행:
   - `ToggleMode`: 즉시 토글 후 산봉우리 수축 (총 1동작)
   - `OpenSettings`: 해당 설정의 미니 팝업 등장 (총 2동작: 꺼내기 + 선택)
3. 손 떼면 산봉우리 수축 애니메이션

**시각적 가이드**:
- 엣지 근처에 손가락이 닿으면 해당 존의 경계를 미세한 선으로 표시 (idle 시 숨김)
- 활성 존의 라벨과 아이콘이 산봉우리 위에 표시
- 존 간 경계 이동 시 햅틱 피드백 (가벼운 틱)

**커스터마이징 UI** (Phase 4.6.1에서는 기본 프리셋만 구현, 커스텀은 Phase 4.6.5 이후):
- 존 개수, 크기 비율, 할당 동작을 사용자가 편집 가능
- 같은 엣지에 최대 4개 존까지 분할 가능
- 설정 데이터는 SharedPreferences에 JSON으로 영속화

### 구현 파일

| 파일 | 변경 |
|------|------|
| `EdgeZone.kt` (신규) | 존 데이터 클래스, 기본 프리셋 정의 |
| `EdgeZoneDetector.kt` (신규) | 터치 위치 → 활성 존 판별 로직 |
| `EdgeZoneOverlay.kt` (신규) | 존 경계, 라벨, 아이콘 시각화 Composable |
| `TouchpadWrapper.kt` | 엣지 감지 로직에 존 판별 분기 추가 |
| `EdgeSwipeConstants.kt` | 존 관련 상수 추가 |

### 검증

- [ ] 왼쪽 상단 존에서 안쪽으로 밀면 클릭 모드 토글 확인
- [ ] 왼쪽 하단 존에서 안쪽으로 밀면 스크롤 모드 토글 확인
- [ ] 존 경계를 넘어갈 때 라벨이 변경되고 햅틱 피드백 발생 확인
- [ ] 산봉우리 위에 현재 존의 아이콘/라벨 표시 확인
- [ ] DPI 설정 존에서 밀면 미니 팝업 등장 확인
- [ ] idle 상태에서 존 경계선 숨김 확인

---

## Phase 4.6.2: 파이 메뉴(Radial Menu) 방식

**개발 기간**: 1일

**쉬운 설명**: 터치패드 가장자리에서 손가락을 안쪽으로 밀면, 손가락 위치를 중심으로 부채꼴 모양의 원형 메뉴가 펼쳐집니다. 손가락을 떼지 않고 원하는 방향으로 밀면 해당 모드가 선택됩니다. 한 번의 드래그로 모든 게 끝납니다.

### 핵심 설계

**파이 메뉴 구조**:
```kotlin
data class PieMenuItem(
    val label: String,
    val icon: ImageVector,
    val action: PieMenuAction,
    val angleStart: Float,      // 시작 각도 (0° = 오른쪽, 시계 방향)
    val angleEnd: Float          // 끝 각도
)

sealed class PieMenuAction {
    data class ToggleMode(val mode: EdgeSwipeMode) : PieMenuAction()
    data class CyclePreset(val presetType: PresetType) : PieMenuAction()
    object Cancel : PieMenuAction()
}
```

**메뉴 항목 배치** (6분할, 각 60°):

| 방향 | 각도 | 동작 |
|------|------|------|
| → 오른쪽 (330°~30°) | 클릭 모드 토글 (좌↔우) |
| ↗ 우상 (30°~90°) | 이동 모드 토글 (자유↔직각) |
| ↑ 위 (90°~150°) | 다이나믹스 프리셋 사이클 |
| ← 왼쪽 (150°~210°) | 스크롤 모드 토글 (OFF↔ON) |
| ↙ 좌하 (210°~270°) | DPI 레벨 사이클 |
| ↓ 아래 (270°~330°) | 모드 프리셋 사이클 |

**동작 흐름**:
1. 엣지에서 `TRIGGER_DISTANCE_DP` 이상 안쪽으로 드래그 → 파이 메뉴 등장
   - 메뉴 중심점 = 트리거 시점의 손가락 위치
   - 등장 애니메이션: scale 0→1 + alpha fade-in (150ms)
2. 손가락을 떼지 않고 원하는 방향으로 드래그:
   - 중심에서 `PIE_DEAD_ZONE_DP` (20dp) 이내: 아무 것도 선택 안 됨 (중앙 = 취소)
   - `PIE_DEAD_ZONE_DP` 밖: 각도 계산 → 해당 섹터 하이라이트
   - 햅틱: 섹터 전환 시 가벼운 틱
3. 손가락을 뗌 → 하이라이트된 항목 실행
   - 중앙에서 뗌: 취소 (아무 변경 없음)
   - 섹터에서 뗌: 해당 동작 실행 + 메뉴 닫힘

**시각적 디자인**:
- 반투명 어두운 배경 원형 (반지름 `PIE_RADIUS_DP` = 100dp)
- 각 섹터: 아이콘 + 짧은 라벨
- 선택된 섹터: 밝은 하이라이트 + 확대 효과
- 중앙: 현재 모드 상태 요약 텍스트
- 산봉우리는 파이 메뉴 등장 시 즉시 수축

### 구현 파일

| 파일 | 변경 |
|------|------|
| `PieMenu.kt` (신규) | 파이 메뉴 Composable (Canvas 기반 렌더링) |
| `PieMenuConstants.kt` (신규) | 반지름, 데드존, 항목 정의 |
| `TouchpadWrapper.kt` | 트리거 후 파이 메뉴 모드 진입 로직 |

### 검증

- [ ] 엣지에서 안쪽으로 밀면 파이 메뉴 등장 확인
- [ ] 손가락 드래그 방향에 따라 섹터 하이라이트 변경 확인
- [ ] 중앙에서 손 떼면 취소 (모드 변경 없음) 확인
- [ ] 섹터에서 손 떼면 해당 모드 토글 확인
- [ ] 섹터 전환 시 햅틱 피드백 확인
- [ ] 파이 메뉴 등장/소멸 애니메이션 확인

---

## Phase 4.6.3: 방향 플릭(Flick) 방식

**개발 기간**: 0.5일

**쉬운 설명**: 터치패드 가장자리에서 산봉우리를 꺼낸 뒤, 특정 방향으로 빠르게 튕기면(플릭) 해당 방향에 매핑된 모드가 바로 토글됩니다. 메뉴 없이 한 번의 제스처로 끝나는 가장 빠른 방식입니다.

### 핵심 설계

**플릭 방향 매핑** (4방향):

| 플릭 방향 | 동작 | 시각적 힌트 |
|----------|------|------------|
| ↑ 위 | 클릭 모드 토글 (좌↔우) | 짧은 화살표 + "Click" |
| → 오른쪽 | 스크롤 모드 토글 (OFF↔ON) | 짧은 화살표 + "Scroll" |
| ↓ 아래 | DPI 레벨 사이클 | 짧은 화살표 + "DPI" |
| ← 왼쪽 | 이동 모드 토글 (자유↔직각) | 짧은 화살표 + "Move" |

**플릭 감지 알고리즘**:
```kotlin
data class FlickResult(
    val direction: FlickDirection,  // UP, DOWN, LEFT, RIGHT
    val velocity: Float             // dp/ms
)

// 감지 조건:
// 1. 엣지에서 TRIGGER_DISTANCE 이상 안쪽으로 진입 (산봉우리 활성화)
// 2. 진입 후 특정 방향으로 FLICK_MIN_DISTANCE_DP (30dp) 이상 이동
// 3. 이동 속도 >= FLICK_MIN_VELOCITY_DP_MS (0.5 dp/ms)
// 4. 주축 이동량이 부축의 2배 이상 (방향 명확성)
```

**동작 흐름**:
1. 엣지에서 안쪽으로 드래그 → 산봉우리 등장
2. `TRIGGER_DISTANCE_DP` 도달 → 플릭 대기 모드 진입
   - 4방향 힌트 아이콘이 산봉우리 주변에 미세하게 표시
3. 빠르게 특정 방향으로 플릭 → 해당 모드 즉시 토글 + 방향 표시 애니메이션
4. 플릭 없이 손 떼면 → 취소 (아무 변경 없음)

**시각적 피드백**:
- 플릭 대기 중: 4방향에 작은 아이콘 표시 (alpha 0.4)
- 플릭 방향 감지 시: 해당 방향 아이콘이 커지며 밝아짐 + 잔상 효과
- 모드 변경 완료: 짧은 토스트형 피드백 ("→ Right Click" 등, 1초 후 소멸)

### 구현 파일

| 파일 | 변경 |
|------|------|
| `FlickDetector.kt` (신규) | 플릭 방향 및 속도 감지 |
| `FlickHintOverlay.kt` (신규) | 4방향 힌트 아이콘 오버레이 |
| `TouchpadWrapper.kt` | 트리거 후 플릭 감지 모드 분기 |
| `EdgeSwipeConstants.kt` | 플릭 관련 상수 추가 |

### 검증

- [ ] 엣지에서 진입 후 위로 플릭 → 클릭 모드 토글 확인
- [ ] 오른쪽으로 플릭 → 스크롤 토글 확인
- [ ] 느린 드래그 (속도 미달) → 플릭 미인식 확인
- [ ] 대각선 드래그 (방향 불명확) → 플릭 미인식 확인
- [ ] 플릭 없이 손 떼기 → 취소 확인
- [ ] 4방향 힌트 아이콘 표시/소멸 확인

---

## Phase 4.6.4: 제스처 드로잉 인식 방식

**개발 기간**: 1.5일

**쉬운 설명**: 터치패드 가장자리에서 산봉우리를 꺼내면 "그리기 모드"로 들어갑니다. 이 상태에서 터치패드에 특정 모양(동그라미, S자, L자 등)을 그리면, 그 모양을 인식해서 해당 설정 화면을 띄워줍니다. 마치 마우스 제스처처럼 동작합니다.

### 핵심 설계

**제스처 정의**:

| 제스처 모양 | 동작 | 그리기 가이드 |
|------------|------|--------------|
| ○ (원) | 스크롤 모드 토글 | 동그랗게 한 바퀴 |
| L (꺾은선) | 클릭 모드 토글 | 아래로 내린 후 오른쪽으로 |
| S (S자) | DPI 설정 팝업 | S자 곡선 |
| Z (지그재그) | 모드 프리셋 사이클 | 지그재그 |
| → (직선 오른쪽) | 이동 모드 토글 | 오른쪽으로 직선 |
| ↑ (직선 위) | 다이나믹스 프리셋 사이클 | 위로 직선 |

**인식 알고리즘** ($1 Unistroke Recognizer 기반):
- 터치 포인트를 일정 간격으로 리샘플링 (64포인트)
- 정규화 (회전 불변, 크기 불변, 위치 불변)
- 미리 등록된 템플릿과 유사도 비교
- 최고 유사도가 `GESTURE_MATCH_THRESHOLD` (0.75) 이상이면 인식 성공

**동작 흐름**:
1. 엣지에서 `TRIGGER_DISTANCE_DP` 이상 안쪽 진입 → 제스처 모드 활성화
   - 배경 약간 어두워짐 (alpha 0.3)
   - "제스처를 그려주세요" 안내 텍스트
2. 손 떼기 → 제스처 인식 시도
   - 성공: 해당 동작 실행 + 인식된 제스처 모양 하이라이트 애니메이션
   - 실패: "인식 실패" 피드백 + 궤적 빨간색으로 표시 후 소멸
3. 제스처 그리는 동안: 실시간 궤적 표시 (밝은 선)

**시각적 피드백**:
- 그리기 중: 손가락 궤적을 밝은 선으로 실시간 표시
- 인식 성공: 궤적이 초록색으로 변하며 매칭된 제스처 이름 표시
- 인식 실패: 궤적이 빨간색으로 변하며 1초 후 소멸
- 제스처 가이드: 첫 사용 시 또는 설정에서 각 제스처 모양 가이드 표시

### $1 Recognizer 구현

외부 라이브러리 없이 직접 구현 (알고리즘이 단순하고 경량):
```kotlin
class DollarOneRecognizer {
    private val templates: List<GestureTemplate>
    
    fun recognize(points: List<PointF>): RecognitionResult {
        val resampled = resample(points, 64)
        val rotated = rotateToZero(resampled)
        val scaled = scaleToSquare(rotated, 250f)
        val translated = translateToOrigin(scaled)
        
        return templates
            .map { it to distanceAtBestAngle(translated, it.points) }
            .minByOrNull { it.second }
            ?.let { (template, distance) ->
                val score = 1f - distance / (0.5f * sqrt(250f * 250f + 250f * 250f))
                RecognitionResult(template.name, score, template.action)
            }
            ?: RecognitionResult.NONE
    }
}
```

### 구현 파일

| 파일 | 변경 |
|------|------|
| `DollarOneRecognizer.kt` (신규) | $1 Unistroke 인식 알고리즘 |
| `GestureTemplate.kt` (신규) | 제스처 템플릿 정의 (원, L, S, Z, 직선 등) |
| `GestureDrawingOverlay.kt` (신규) | 궤적 표시 + 인식 결과 피드백 Composable |
| `TouchpadWrapper.kt` | 트리거 후 제스처 모드 진입 로직 |

### 검증

- [ ] 엣지에서 진입 후 원을 그리면 스크롤 토글 확인
- [ ] L자를 그리면 클릭 모드 토글 확인
- [ ] 인식 불가 모양 → 실패 피드백 확인
- [ ] 궤적 실시간 표시 확인
- [ ] 성공/실패 시 색상 변화 애니메이션 확인
- [ ] 다양한 크기/속도로 그려도 인식 정확도 유지 확인

---

## Phase 4.6.5: 비교 테스트 및 최종 선정

**개발 기간**: 0.5일

**쉬운 설명**: 앞서 구현한 4가지 방식을 모두 실제로 써보면서, 어떤 방식이 가장 빠르고 직관적인지 비교합니다.

### 테스트 항목

**방식 전환 설정 UI**:
- 설정에서 `EdgeInteractionMode` 선택 가능:
  ```kotlin
  enum class EdgeInteractionMode {
      LEGACY_POPUP,    // 기존 5단계 팝업 (Phase 4.4 방식)
      ZONE,            // Phase 4.6.1
      PIE_MENU,        // Phase 4.6.2
      FLICK,           // Phase 4.6.3
      GESTURE_DRAWING  // Phase 4.6.4
  }
  ```

**비교 기준**:

| 기준 | 설명 |
|------|------|
| 조작 단계 수 | 목표 모드에 도달하기까지 필요한 동작 수 |
| 학습 용이성 | 처음 사용자가 방식을 이해하는 데 걸리는 시간 |
| 오조작 빈도 | 의도하지 않은 모드 전환이 발생하는 빈도 |
| 곁눈 사용성 | 화면을 직접 보지 않고도 조작 가능한 정도 |
| 모드 커버리지 | 모든 모드/옵션을 커버할 수 있는지 |
| 구현 안정성 | 제스처 인식 정확도, 엣지 케이스 처리 |

**테스트 시나리오**:
1. "좌클릭 → 우클릭 전환" (단순 토글)
2. "스크롤 OFF → 무한 스크롤 ON" (2단계 변경)
3. "DPI LOW → HIGH로 변경" (값 선택)
4. "모드 프리셋 Standard → Precise 전환" (프리셋 변경)
5. "연속으로 3가지 모드 변경" (연속 조작)

### 최종 선정 후 작업

- 선정된 방식을 기본 `EdgeInteractionMode`로 설정
- 미선정 방식의 코드는 제거하지 않고 옵션으로 유지 (사용자 선호에 따라 선택 가능)
- 선정 결과에 따라 기존 5단계 팝업(LEGACY_POPUP) 유지 여부 결정

### 검증

- [ ] 모든 방식 간 전환이 설정에서 정상 동작 확인
- [ ] 각 방식으로 5가지 시나리오 모두 수행 가능 확인
- [ ] 최종 선정 방식 결정 및 기본값 설정
