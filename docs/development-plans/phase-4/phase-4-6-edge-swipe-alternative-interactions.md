---
title: "BridgeOne Phase 4.6: 엣지 스와이프 대안 조작 방식 탐색"
description: "BridgeOne 프로젝트 Phase 4.6 - 터치패드 모드/옵션 전환을 위한 다양한 조작 방식을 구현·비교하여 최적의 UX를 선정"
tags: ["android", "edge-swipe", "gesture", "ux", "interaction", "pie-menu", "flick", "zone", "drawing", "swipe-carousel"]
version: "v1.0"
owner: "Chatterbones"
updated: "2026-05-16"
---

# BridgeOne Phase 4.6: 엣지 스와이프 대안 조작 방식 탐색

**개발 기간**: 5.5~6.5일 (4.6.2 커스터마이징 포함으로 확장)

**목표**: 현재 엣지 스와이프 메뉴의 사용성 한계(5단계 조작)를 극복하기 위해, **여러 대안 조작 방식을 모두 구현**한 뒤 실제 사용 비교 테스트를 통해 최적의 방식을 선정합니다.

**배경**: 현재 엣지 스와이프 → 팝업 모드 선택(SWIPE/DIRECT_TOUCH) → 모드 버튼 선택 → 탭 토글 → 확인 버튼의 5단계를 거쳐야 모드 전환이 가능합니다. "우클릭 모드로 바꾸고 싶다" 같은 단순한 작업에도 5번의 조작이 필요해 사용성이 떨어집니다.

| 하위 Phase | 내용 | 상태 |
|-----------|------|------|
| 4.6.1 | 임시 환경 설정 페이지 (조작 방식 전환) | 완료 |
| 4.6.2 | 엣지 존(Zone) 분할 방식 + 커스터마이징 | 미시작 |
| 4.6.3 | 파이 메뉴(Radial Menu) 방식 | 미시작 |
| 4.6.4 | 방향 플릭(Flick) 방식 | 미시작 |
| 4.6.5 | 제스처 드로잉 인식 방식 | 미시작 |
| 4.6.6 | 엣지 스트립 스와이프 캐러셀 방식 | 미시작 |
| 4.6.7 | 비교 테스트 및 최종 선정 | 미시작 |

**선행 조건**: Phase 4.5 (E2E 테스트 수정사항) 완료

**에뮬레이터 호환성**: 모든 하위 Phase 에뮬레이터에서 개발 가능. 최종 비교 테스트(4.6.7)는 실기기 권장.

**공통 전제**:
- 기존 엣지 스와이프 인프라(산봉우리 시각화, 엣지 감지, `EdgeSwipeConstants`)를 최대한 재사용
- 각 방식은 독립적인 `EdgeInteractionMode` enum 값으로 구분하여, 설정에서 원하는 방식을 선택 가능하게 구현
- 모든 방식에서 조절 가능한 모드/옵션: ClickMode, MoveMode, ScrollMode, DpiLevel, DynamicsPreset, ModePreset

---

## Phase 4.6.1: 임시 환경 설정 페이지 (조작 방식 전환)

**개발 기간**: 0.5일

### 핵심 설계

`TouchpadState.edgeInteractionMode` 필드와 `EdgeInteractionMode` enum은 이미 존재 (Phase 4.5.18에서 추가됨). 설정 페이지에서 이 값만 변경하면 됨.

**진입 방법**: `StandardModePage`의 `HorizontalPager`를 5페이지로 확장. 기존 4개 페이지(터치패드, 절대좌표, 키보드, 마인크래프트) 뒤에 설정 페이지를 추가. 1페이지에서 우측 스와이프, 또는 4페이지에서 좌측 스와이프로 진입.

**원형(circular) 스와이프**: 1페이지 ↔ 5페이지 간 양방향 wrap-around 지원. `HorizontalPager`의 `pageCount`를 `Int.MAX_VALUE`로 설정하고 초기 페이지를 `PAGE_COUNT(5)`의 배수로 지정, 실제 콘텐츠는 `page % 5`로 매핑.

**설정 페이지 구성**:
- 섹션: "엣지 조작 방식"
- 항목: `EdgeInteractionMode` 각 값에 대한 라디오 버튼 목록
  - LEGACY_POPUP: "기존 팝업 방식 (5단계)"
  - ZONE: "엣지 존 방식"
  - (이후 Phase들에서 PIE_MENU, FLICK 등 항목 추가)
- 선택 즉시 적용, 앱 재시작 없이 반영
- SharedPreferences에 영속화

### 구현 파일

| 파일 | 변경 |
|------|------|
| `SettingsDialog.kt` (신규) | EdgeInteractionMode 선택 UI (현재 미사용, 참조용으로 보존) |
| `StandardModePage.kt` | `HorizontalPager` 5페이지 확장, circular 스크롤 구현, `Page5Settings` / `SettingsEdgeInteractionModeSection` composable 추가, `PageIndicator` 파라미터 변경 |

### 구현 노트

> **⚠️ Phase 4.6.1 구현 변경사항**
>
> - `EdgeInteractionMode` enum 추가 (`LEGACY_POPUP`, `ZONE`) → `EdgeSwipeOverlay.kt`
> - `TouchpadState.edgeInteractionMode` 필드 추가 (기본값: `LEGACY_POPUP`) → `TouchpadMode.kt`
> - `HorizontalPager` pageCount: 5 → `Int.MAX_VALUE` (circular), `PAGER_INITIAL_PAGE` = `Int.MAX_VALUE / 2`를 5의 배수로 내림
> - 콘텐츠 매핑: `when (page % PAGE_COUNT)` — 논리 페이지 0~4
> - `Page5Settings` composable 추가 (`StandardModePage.kt` 내) — "환경 설정" 제목 + `SettingsEdgeInteractionModeSection`
> - `SettingsEdgeInteractionModeSection` composable 추가 — `EdgeInteractionMode.entries` 순회하여 RadioButton 목록 렌더링
> - `PageIndicator` 파라미터 변경: `pagerState: PagerState` → `currentPage: Int, offsetFraction: Float` — wrap-around 전환(0→4, 4→0) 시 `offsetFraction = 0`으로 고정하여 worm이 화면 밖으로 튀는 현상 방지
> - SharedPreferences 저장/로드: `StandardModePage.kt`에 `loadEdgeInteractionMode` / `saveEdgeInteractionMode` 추가 (기존 `PREF_NAME` 재사용)
> - `SettingsRepository.kt`는 별도 파일 미생성 — 기존 DPI 저장 패턴(private 함수)으로 통합
>
> **후속 Phase 영향**: Phase 4.6.3~4.6.6 구현 시 `EdgeInteractionMode`에 `PIE_MENU`, `FLICK`, `GESTURE_DRAWING`, `SWIPE_CAROUSEL` 값 추가 후 `SettingsEdgeInteractionModeSection`의 `when(mode)` 분기에 해당 레이블 추가 필요.

### 검증

- [x] 1페이지에서 우측 스와이프 시 설정 페이지(5페이지)로 이동 확인
- [x] 5페이지에서 좌측 스와이프 시 1페이지로 wrap-around 확인
- [x] LEGACY_POPUP / ZONE 선택 즉시 적용 확인
- [x] 앱 재시작 후에도 선택한 방식이 유지되는지 확인

---

## Phase 4.6.2: 엣지 존(Zone) 분할 방식 + 커스터마이징

**개발 기간**: 2~2.5일

### 개요

**존(Zone)이란**: 터치패드 4개 엣지(TOP/BOTTOM/LEFT/RIGHT) 각각을 따라 설정된 위치 구간 단위. 각 존은 독립적인 액션을 보유하며, 손가락이 해당 구간에서 안쪽으로 진입·트리거하면 그 액션이 즉시 실행된다. 하나의 엣지는 여러 존으로 분할될 수 있다.

**기존 LEGACY_POPUP과의 차이**: LEGACY_POPUP은 엣지 진입 후 팝업을 열고 메뉴에서 선택하는 5단계 과정이다. 존 방식은 팝업 없이 "엣지의 어느 위치에서 진입했느냐"가 곧 액션을 결정하므로 1단계로 완료된다. 메뉴 탐색 비용이 없고, 위치 기억만으로 반복 조작이 가능하다.

**커스터마이징의 필요성**: 사용자마다 손이 닿기 편한 위치(가동 범위)와 자주 쓰는 기능이 다르다. 특히 근육장애 사용자는 엣지 구간별 도달 가능 여부의 개인차가 크므로, 존의 위치·크기·할당 액션을 개인화할 수 있어야 한다.

### 존 구성 요소

**엣지(Edge)**: 터치패드의 4개 측면(TOP/BOTTOM/LEFT/RIGHT). 각 엣지는 독립적인 존 목록을 가진다. 존이 없는 엣지 구간은 LEGACY_POPUP 방식의 산봉우리만 표시하고 액션을 실행하지 않는다.

**모서리 버튼 제약**: 터치패드에 모드 변경 버튼이 특정 모서리에 배치된 경우, 해당 모서리에 인접한 두 엣지의 끝 구간은 존으로 사용할 수 없다. 예를 들어 LEFT-TOP 모서리에 버튼이 있으면, LEFT 엣지의 상단(0.0~`CORNER_BUTTON_BLOCKED_RATIO`) 구간과 TOP 엣지의 좌측(0.0~`CORNER_BUTTON_BLOCKED_RATIO`) 구간은 존 배정이 불가하며 자동으로 `Unassigned`로 고정된다. `EdgeZoneConfig.default()` 생성 시에도 이 제약이 적용되어, 버튼 위치 설정에 따라 기본 프리셋의 존 구성이 달라진다.

**비율(Ratio)**: 엣지 위에서 존이 차지하는 구간 (0.0~1.0). TOP/BOTTOM은 좌→우, LEFT/RIGHT는 위→아래 방향으로 증가한다. 같은 엣지의 모든 존 비율 합은 반드시 1.0이다. 최소 비율은 `MIN_ZONE_RATIO`로 제한한다.

**액션(Action)**: 존 트리거 시 실행되는 동작의 종류:
- **ToggleMode**: ON/OFF 두 상태가 있는 모드를 전환. 현재 상태에서 반대 상태로 즉시 이동 (예: 좌클릭 ↔ 우클릭, 스크롤 OFF ↔ ON)
- **CyclePreset**: 3개 이상의 값을 순환하는 설정을 한 단계 전진 (예: 다이나믹스 프리셋, 모드 프리셋). 끝에서 처음으로 순환
- **OpenSettings**: DPI·스크롤 속도처럼 수치형 설정을 미리 정의된 단계 순으로 사이클 변경. 트리거만으로 다음 단계로 이동하며 별도 팝업을 열지 않는다
- **Unassigned**: 아무 동작 없음. 엣지 구간을 명시적으로 비워두거나 아직 액션을 배정하지 않은 존

**라벨(Label)과 아이콘(IconKey)**: 존 트리거 준비 중 산봉우리 위에 표시하는 식별자. 유저가 어떤 존에 진입했는지 시각적으로 확인할 수 있다.

**설정 컨테이너 (EdgeZoneConfig)**: 4개 엣지의 존 목록 전체를 하나의 단위로 묶어 저장·복원·기본값 리셋하는 역할. 앱 재시작 시 SharedPreferences에서 JSON으로 로드한다.

### 핵심 설계

**존 정의 구조**:
```kotlin
@Serializable
data class EdgeZone(
    val edge: EntryEdge,        // TOP, BOTTOM, LEFT, RIGHT
    val startRatio: Float,      // 엣지 시작 비율 (0.0~1.0)
    val endRatio: Float,        // 엣지 끝 비율 (startRatio < endRatio)
    val action: EdgeZoneAction,
    val label: String,
    val iconKey: String         // ImageVector 직렬화 불가 → String 키 사용, IconRegistry에서 변환
)

@Serializable
sealed class EdgeZoneAction {
    @Serializable data class ToggleMode(val mode: EdgeSwipeMode) : EdgeZoneAction()
    @Serializable data class CyclePreset(val presetType: PresetType) : EdgeZoneAction()
    @Serializable data class OpenSettings(val settingsType: SettingsType) : EdgeZoneAction()
    @Serializable object Unassigned : EdgeZoneAction()
}

enum class PresetType { DYNAMICS, MODE }
enum class SettingsType { DPI, SCROLL_SPEED }

@Serializable
data class EdgeZoneConfig(
    val topZones: List<EdgeZone>,
    val bottomZones: List<EdgeZone>,
    val leftZones: List<EdgeZone>,
    val rightZones: List<EdgeZone>
) {
    companion object {
        fun default(): EdgeZoneConfig = /* 아래 기본 프리셋 표의 값으로 생성 */
    }
}
```

`TouchpadState`에 `edgeZoneConfig: EdgeZoneConfig = EdgeZoneConfig.default()` 필드 추가.

**기본 존 프리셋** (`EdgeZoneConfig.default()` 초기값):

| 엣지 | 구간 | 동작 | 시각적 힌트 |
|------|------|------|------------|
| LEFT 상단 (0.0~0.5) | 클릭 모드 토글 | 좌↔우 | 마우스 아이콘 |
| LEFT 하단 (0.5~1.0) | 스크롤 모드 토글 | ON↔OFF | 스크롤 아이콘 |
| RIGHT 상단 (0.0~0.5) | 이동 모드 토글 | 자유↔직각 | 화살표 아이콘 |
| RIGHT 하단 (0.5~1.0) | DPI 사이클 | 순환 | 속도 아이콘 |
| TOP 좌측 (0.0~0.5) | 다이나믹스 프리셋 사이클 | 순환 | 곡선 아이콘 |
| TOP 우측 (0.5~1.0) | 모드 프리셋 사이클 | 순환 | 프리셋 아이콘 |
| BOTTOM (0.0~1.0) | 미할당 | — | — |

**동작 흐름**:
1. 엣지에서 안쪽으로 드래그 시작 → 산봉우리 등장 + 해당 존의 라벨/아이콘 표시
2. `TRIGGER_DISTANCE_DP` 도달 → 햅틱 피드백 + 동작 실행:
   - `ToggleMode` / `CyclePreset`: 즉시 실행 후 산봉우리 수축 (총 1동작)
   - `OpenSettings`: DPI/스크롤 속도 사이클로 처리 (미니 팝업 없음)
   - `Unassigned`: 아무 동작 없이 수축
3. 손 떼면 산봉우리 수축 애니메이션

**시각적 가이드**:
- idle 시 존 경계를 미세한 선으로 표시 (`EDGE_ZONE_HINT_BASE_ALPHA` 재사용)
- 활성 존의 라벨과 아이콘이 산봉우리 위에 표시
- 존 경계 진입 시 햅틱 틱 (가벼운 `CLOCK_TICK`)

**영속화**: `EdgeZoneConfig`를 kotlinx.serialization JSON으로 직렬화 → `SharedPreferences`의 `KEY_EDGE_ZONE_CONFIG` 키에 저장. 저장/복원 함수는 `StandardModePage.kt`의 기존 패턴(line 994~1012)을 따름.

**제약 상수** (`EdgeSwipeConstants`에 신규 추가):
- `MIN_ZONE_RATIO = 0.10f` — 존 최소 크기, 기본값: 0.10f
- `MAX_ZONES_PER_EDGE = 4` — 엣지당 최대 존 수, 기본값: 4
- `ZONE_BOUNDARY_DRAG_HIT_DP = 24f` — 편집기 경계선 드래그 히트박스, 기본값: 24f
- `ZONE_PREVIEW_ASPECT_RATIO = 0.6f` — 편집기 미리보기 가로:세로 비율, 기본값: 0.6f
- `CORNER_BUTTON_BLOCKED_RATIO = 0.15f` — 모서리에 모드 변경 버튼이 있을 때 해당 엣지 끝에서 차단되는 비율, 기본값: 0.15f

### 커스터마이징 UI

**편집기의 역할**: 유저가 4개 엣지 각각의 존 구성을 직접 변경하는 화면. 유저가 결정하는 항목은 (1) 각 엣지를 몇 개의 존으로 나눌지, (2) 각 존이 엣지의 어느 구간을 차지할지(비율), (3) 각 존에 어떤 액션을 배정할지, (4) 각 존의 식별 라벨·아이콘이다. 변경 결과는 [저장] 전까지 임시 상태로 유지되며 언제든 기본값으로 리셋 가능하다.

**진입 경로**:
- `Page5Settings`에서 `edgeInteractionMode == ZONE`일 때만 "엣지 존 편집" 항목 표시
- 항목: 라벨 "엣지 존 편집" + 보조 텍스트 "{총 존 개수}개 존 설정됨" + 우측 chevron
- 탭 시 풀스크린 편집기로 진입 (Dialog 또는 별도 Composable 라우트)

**풀스크린 편집기 (`EdgeZoneEditorScreen.kt`) 레이아웃**:
```
┌─ TopAppBar: ← 뒤로 | "엣지 존 편집" | [기본값] [저장]
├─ 미리보기 (화면 약 60%, Canvas 기반):
│   - 터치패드 모형 + 4엣지 띠 렌더링
│   - 각 존을 색상 블록으로 표시, 라벨 오버레이
│   - 경계선 드래그 → 인접 두 존의 비율 실시간 변경
│   - 존 탭 → 선택 상태 (테두리 강조)
│   - 빈 영역 길게 누름 → 해당 엣지에 새 존 추가
├─ 선택된 존 패널 (선택 시에만):
│   - 라벨 입력 필드
│   - 아이콘 선택 그리드 (IconRegistry 항목)
│   - 액션 라디오: ToggleMode | CyclePreset | OpenSettings | Unassigned
│     + 각 분기별 보조 선택자 (EdgeSwipeMode / PresetType / SettingsType)
│   - 비율 정밀 슬라이더
│   - [삭제] 버튼
└─ 하단 액션 바: [TOP +] [BOTTOM +] [LEFT +] [RIGHT +] (MAX_ZONES_PER_EDGE 미만일 때만 활성)
```

**편집기 상호작용 규칙**:
- 경계선 드래그 시 인접 두 존의 비율 동시 변경, `MIN_ZONE_RATIO` 하한 보장
- 존 삭제 시 인접 존이 빈 공간을 균등 분할로 흡수
- 존 추가 시 해당 엣지의 가장 큰 존을 절반 분할, 새 존은 `Unassigned`
- 변경은 임시 상태에 보관, [저장] 클릭 시에만 SharedPreferences 반영
- [기본값] 클릭 시 확인 다이얼로그 → `EdgeZoneConfig.default()`로 리셋
- 모서리에 모드 변경 버튼이 있는 경우, 해당 모서리에 인접한 엣지 끝 구간(`CORNER_BUTTON_BLOCKED_RATIO`)은 회색으로 비활성화 표시하며 드래그로 조정 불가. 해당 구간에 존 추가도 불가

### 구현 파일

| 파일 | 변경 |
|------|------|
| `EdgeZone.kt` (신규) | `EdgeZone`, `EdgeZoneAction`, `EdgeZoneConfig`, `PresetType`, `SettingsType`, `EdgeZoneConfig.default()` |
| `IconRegistry.kt` (신규) | iconKey ↔ ImageVector 양방향 매핑 |
| `EdgeZoneDetector.kt` (신규) | (entryEdge, alongEdgeRatio) → 활성 `EdgeZone?` 판별 |
| `EdgeZoneOverlay.kt` (신규) | idle 경계선, 활성 존 라벨·아이콘 시각화 Composable |
| `EdgeZoneActionHandler.kt` (신규) | `EdgeZoneAction` → `TouchpadState` 변경 실행 |
| `EdgeZoneEditorScreen.kt` (신규) | 풀스크린 편집기 Composable |
| `EdgeZoneEditorPreviewCanvas.kt` (신규) | 미리보기 Canvas + 드래그 처리 |
| `TouchpadWrapper.kt` | 엣지 트리거 분기에 `ZONE` 케이스 추가 |
| `TouchpadMode.kt` | `TouchpadState.edgeZoneConfig` 필드 추가 |
| `StandardModePage.kt` | Page5에 편집기 진입 항목, `loadEdgeZoneConfig`/`saveEdgeZoneConfig`, 편집기 호스팅 |
| `ScrollConstants.kt` (`EdgeSwipeConstants`) | 신규 상수 4개 |

재사용 자산: `EdgeSwipeMode`(EdgeSwipeOverlay.kt:82), `EntryEdge`(EdgeSwipeOverlay.kt:72), `applyEdgeModeToggle()` 로직, `EDGE_ZONE_HINT_*` alpha 상수(ScrollConstants.kt:168), 햅틱 패턴(`LONG_PRESS`/`CLOCK_TICK`), SharedPreferences 패턴(StandardModePage.kt:978~1012).

> **후속 Phase 참고**: `EdgeZoneAction` sealed class와 `EdgeZoneConfig`는 다른 인터랙션 방식(PIE_MENU, FLICK 등)에서도 액션 매핑 체계로 재사용 가능 → Phase 4.6.3~4.6.6 설계 시 검토. `IconRegistry`도 재사용 가능.

### 검증

- [ ] 기본 프리셋: 각 엣지 존에서 안쪽으로 밀면 정의된 동작 실행 (왼쪽 상단 클릭 토글, 왼쪽 하단 스크롤 토글 등)
- [ ] 산봉우리 위에 활성 존의 라벨·아이콘 표시 확인
- [ ] idle 상태에서 약한 흰색 경계선 표시 확인
- [ ] Page 5 → "엣지 존 편집" 항목이 ZONE 모드일 때만 표시 확인
- [ ] 편집기 진입 → 미리보기에서 경계선 드래그로 비율 조정 가능, `MIN_ZONE_RATIO` 보장
- [ ] 존 탭 → 패널에서 라벨/아이콘/액션 변경 가능
- [ ] 존 추가/삭제 동작 + `MAX_ZONES_PER_EDGE` 제한 확인
- [ ] [저장] 후 앱 재시작 시 커스텀 구성 유지 확인
- [ ] [기본값] 리셋 정상 동작 확인
- [ ] `LEGACY_POPUP` 선택 시 기존 팝업 동작 유지 (회귀 없음)
- [ ] 모서리에 모드 변경 버튼이 있는 경우, 해당 모서리 인접 엣지 구간이 편집기에서 회색 비활성화로 표시되고 존 추가/드래그 불가 확인
- [ ] 모서리 버튼 없는 경우 해당 제약 미적용 확인

---

## Phase 4.6.3: 파이 메뉴(Radial Menu) 방식

> **⚠️ Phase 4.6.1 변경사항**: `EdgeInteractionMode` enum이 `EdgeSwipeOverlay.kt`에 `LEGACY_POPUP`, `ZONE` 두 값으로 이미 정의됨.
> `PIE_MENU` 값을 `EdgeSwipeOverlay.kt`의 `EdgeInteractionMode` enum에 추가하고,
> `StandardModePage.kt`의 `SettingsEdgeInteractionModeSection` 내 `when(mode)` 분기에 `EdgeInteractionMode.PIE_MENU -> "파이 메뉴 방식"` 추가 필요.
>
> **⚠️ Phase 4.5.18 변경사항**: `EdgeInteractionMode` enum에 `PIE_MENU` 값 추가 필요 (`EdgeSwipeOverlay.kt`).
> `TouchpadWrapper.kt`의 trigger 분기에 `EdgeInteractionMode.PIE_MENU` 케이스 추가.

**개발 기간**: 1일

### 개요

**파이 메뉴(방사형 메뉴)란**: 엣지 트리거 이후 손가락 위치를 중심으로 원형 섹터들이 펼쳐지는 방식. 손가락을 뗄 때의 드래그 방향이 선택 기준이 된다. 진입 위치가 아닌 이후 방향으로 항목을 구별하므로, 어느 엣지에서 진입하든 동일한 메뉴 구성을 제공할 수 있다.

**존 방식과의 차이**: 존 방식은 "어디서 진입했느냐"가 액션을 결정하고 별도의 추가 입력이 없다. 파이 메뉴는 진입 후 한 번 더 드래그 방향을 입력해야 하므로 2단계이지만, 진입 위치 암기 부담이 없다. 또한 메뉴가 시각적으로 명시적으로 표시되어 있어 학습 비용이 낮다.

**데드존의 역할**: 메뉴 중심에서 일정 반경 이내(`PIE_DEAD_ZONE_DP`)는 아무것도 선택되지 않는다. 이 영역이 곧 취소 경로로, 실수로 진입했을 때 아무 변경 없이 빠져나올 수 있는 탈출구다.

**섹터 분할 원리**: 방향각도로 항목을 나눠 공간적 기억에 의존한다. "우클릭은 오른쪽 방향"처럼 방향-액션 연결이 직관적이면 학습 속도가 빠르다.

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

## Phase 4.6.4: 방향 플릭(Flick) 방식

> **⚠️ Phase 4.6.1 변경사항**: `FLICK` 값을 `EdgeSwipeOverlay.kt`의 `EdgeInteractionMode` enum에 추가하고,
> `StandardModePage.kt`의 `SettingsEdgeInteractionModeSection` 내 `when(mode)` 분기에 `EdgeInteractionMode.FLICK -> "방향 플릭 방식"` 추가 필요.
>
> **⚠️ Phase 4.5.18 변경사항**: `EdgeInteractionMode` enum에 `FLICK` 값 추가 필요 (`EdgeSwipeOverlay.kt`).
> `TouchpadWrapper.kt`의 trigger 분기에 `EdgeInteractionMode.FLICK` 케이스 추가.

**개발 기간**: 0.5일

### 개요

**플릭이란**: 엣지에서 산봉우리를 꺼낸 직후 특정 방향으로 빠르게 이동(플릭)하면 그 방향에 매핑된 액션이 즉시 실행되는 방식. "방향 = 액션"의 직접 매핑이므로 메뉴나 추가 선택 단계가 없다.

**속도 조건의 역할**: 느린 드래그와 의도적인 플릭을 속도로 구별한다. 산봉우리를 꺼내고 천천히 움직이는 것은 플릭으로 인식하지 않으므로 오조작을 억제한다. 존 방식과 달리 진입 위치가 아닌 진입 이후의 이동 패턴으로 액션을 결정한다.

**파이 메뉴와의 차이**: 파이 메뉴는 손을 뗄 때 방향을 인식하고 시각적 선택 UI가 표시된다. 플릭은 시각적 메뉴 없이 속도+방향만으로 즉시 처리되어 최소한의 조작 시간을 갖지만, 방향 정확도와 속도 임계값을 모두 충족해야 한다.

**커버리지 제약**: 4방향 플릭만 구별 가능하므로 최대 4개 액션만 배정할 수 있다.

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

## Phase 4.6.5: 제스처 드로잉 인식 방식

**개발 기간**: 1.5일

### 개요

**드로잉 제스처란**: 엣지 트리거 이후 터치패드 화면에 미리 등록된 모양(원, L자, S자 등)을 직접 그리면 해당 액션이 실행되는 방식. 마우스 제스처와 동일한 개념을 터치 입력에 적용한 것이다.

**다른 방식과의 차이점**: 존/플릭/파이 메뉴는 방향이나 위치 구간 수만큼만 액션을 배정할 수 있다. 드로잉 제스처는 구별 가능한 모양의 수가 더 많으므로 더 많은 액션을 단일 상호작용 모드 안에 담을 수 있다. 단 패턴 학습 비용이 있고 인식 실패 가능성이 존재한다.

**$1 Recognizer의 역할**: 터치 포인트 궤적을 크기·회전·위치에 무관하게 정규화한 뒤 사전 등록된 템플릿과 비교한다. "크게 그리든 작게 그리든", "각도가 달라도" 같은 모양으로 인식되므로 입력 일관성 요구가 낮다. 라이브러리 의존 없이 직접 구현한다.

**인식 실패 경로**: 유사도가 임계값(`GESTURE_MATCH_THRESHOLD`) 미만이면 어떤 템플릿과도 매칭하지 않는다. 시각적으로 실패를 명확히 표시하여 유저가 재시도할 수 있어야 한다.

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

## Phase 4.6.6: 엣지 스트립 스와이프 캐러셀 방식

**개발 기간**: 0.5일

### 개요

**캐러셀 방식이란**: 엣지에 손가락을 대고 안쪽(수직 방향)이 아닌 엣지 방향(수평/수직)으로 슬라이드하면 모드 목록이 순서대로 순환하는 방식. 기존 산봉우리 진입 제스처와 동일한 시작점을 사용하지만 이동 방향으로 두 기능을 분기한다.

**순서 기반 탐색의 특성**: 모드들이 고정된 순서로 나열되어 있어 "다음"/"이전"으로만 이동한다. 원하는 모드로 직접 건너뛸 수 없으므로 모드 수가 많으면 여러 번 스와이프가 필요하다. 반면 순서 자체가 예측 가능하고, 인디케이터 도트로 현재 위치를 시각적으로 확인할 수 있다.

**제스처 방향 분기 설계 이유**: 안쪽/옆쪽 이동을 같은 엣지 진입에서 분기하므로 별도의 트리거 영역이 필요하지 않다. 단 두 방향이 명확히 구별되도록 `CAROUSEL_AXIS_RATIO` 임계값으로 판별한다. 대각선 이동은 명확한 방향이 확정될 때까지 판정을 유예한다.

### 핵심 설계

**제스처 감지 분기**:
기존 엣지 진입 감지 로직에서 손가락 이동 방향을 판별해 분기:
- 엣지에서 **안쪽(수직 방향)** 으로 이동 → 기존 산봉우리 동작 유지
- 엣지에서 **가로(수평 방향)** 로 이동 → 캐러셀 스와이프로 처리

```kotlin
// 방향 판별 기준
// abs(deltaX) > abs(deltaY) * CAROUSEL_AXIS_RATIO → 가로 스와이프
// CAROUSEL_AXIS_RATIO 기본값: 1.5f  기본값: 1.5f
const val CAROUSEL_AXIS_RATIO = 1.5f

// 캐러셀 트리거 최소 이동 거리
const val CAROUSEL_MIN_SWIPE_DP = 40f  // 기본값: 40f
```

**모드 순환 목록** (순서대로 순환):

| 순서 | 모드 이름 | 설명 |
|------|----------|------|
| 1 | Standard | 기본 이동 + 좌클릭 |
| 2 | Right Click | 우클릭 모드 |
| 3 | Scroll | 스크롤 모드 ON |
| 4 | Precise | 저속 정밀 이동 |
| 5 | Fast | 고속 이동 |

- 목록은 `ModePreset` 순서를 그대로 사용
- 좌 → 우 스와이프: 다음 모드 (인덱스 +1, 끝에서 처음으로 순환)
- 우 → 좌 스와이프: 이전 모드 (인덱스 -1, 처음에서 끝으로 순환)
- 좌/우 엣지 존 모두 동일하게 동작

**동작 흐름**:
1. 좌/우 엣지 존에 손가락 터치
2. 수평 방향으로 `CAROUSEL_MIN_SWIPE_DP` 이상 이동 → 방향 확정
3. 손가락을 뗌 → 다음/이전 모드로 전환 + 햅틱 피드백
4. 전환된 모드 이름을 토스트형 피드백으로 1초 표시 후 소멸
5. 스와이프 도중 방향 확정 시점에 가벼운 햅틱 틱 (예고 피드백)

**시각적 피드백**:
- 스와이프 중: 현재 모드 이름 + 방향 화살표 표시 (alpha 0.6)
- 전환 완료: 새 모드 이름이 슬라이드 인 애니메이션으로 등장 후 페이드 아웃
- 인디케이터 도트: 엣지 존 위에 현재 모드 위치를 점으로 표시 (예: ○●○○○)

### 구현 파일

| 파일 | 변경 |
|------|------|
| `EdgeCarouselDetector.kt` (신규) | 수평 스와이프 방향 판별 및 모드 인덱스 관리 |
| `EdgeCarouselOverlay.kt` (신규) | 모드 이름 슬라이드 애니메이션 + 인디케이터 도트 Composable |
| `TouchpadWrapper.kt` | 엣지 진입 후 수직/수평 방향 분기 추가 |
| `EdgeSwipeConstants.kt` | `CAROUSEL_AXIS_RATIO`, `CAROUSEL_MIN_SWIPE_DP` 상수 추가 |

### 검증

- [ ] 좌 엣지에서 오른쪽으로 스와이프 → 다음 모드 전환 확인
- [ ] 우 엣지에서 왼쪽으로 스와이프 → 이전 모드 전환 확인
- [ ] 반대 방향(이전 모드) 스와이프 정상 동작 확인
- [ ] 모드 목록 끝에서 처음으로 순환 확인
- [ ] 수직 방향 스와이프 시 기존 산봉우리 동작 유지 확인
- [ ] 대각선 스와이프 시 축 판별 정확성 확인
- [ ] 인디케이터 도트 현재 모드 위치 정확히 표시 확인
- [ ] 전환 애니메이션 및 햅틱 피드백 확인

---

## Phase 4.6.7: 비교 테스트 및 최종 선정

**개발 기간**: 0.5일

### 개요

**이 Phase의 목적**: 4.6.2~4.6.6에서 구현한 5가지 방식을 동일한 조건에서 직접 사용해보고 최적 방식을 선정한다. 각 방식은 서로 다른 입력 특성(위치 기반/방향 기반/패턴 기반/순서 기반)과 트레이드오프를 가지므로, 이론적 분석만으로는 우열을 판단하기 어렵다.

**선정 기준**: BridgeOne의 핵심 사용자층(근육장애로 인한 정밀 조작 제약)에게 실제로 더 편리한 방식이어야 한다. 조작 단계 수와 오조작 빈도가 가장 중요한 기준이며, 곁눈 사용성(화면을 안 봐도 조작 가능한지)과 학습 부담도 고려한다.

**미선정 방식 처리**: 선정되지 않은 방식의 코드는 제거하지 않는다. 사용자마다 선호가 다를 수 있고 Phase 4.6.1 설정 페이지에서 이미 방식 전환이 가능하므로, 모든 구현을 옵션으로 유지한다.

### 테스트 항목

> **⚠️ Phase 4.5.18 변경사항**: `EdgeInteractionMode` enum은 이미 `EdgeSwipeOverlay.kt`에 `LEGACY_POPUP, ZONE` 두 값으로 정의됨.
> Phase 4.6.3~4.6.6 구현 시 `PIE_MENU`, `FLICK`, `GESTURE_DRAWING`, `SWIPE_CAROUSEL` 값을 추가하면 됨.
> `TouchpadState.edgeInteractionMode` 필드도 이미 존재. 설정 UI는 Phase 4.6.1에서 구현됨.

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

- [ ] 모든 방식(ZONE, PIE_MENU, FLICK, GESTURE_DRAWING, SWIPE_CAROUSEL) 간 전환이 Phase 4.6.1 설정에서 정상 동작 확인
- [ ] 각 방식으로 5가지 시나리오 모두 수행 가능 확인
- [ ] 최종 선정 방식 결정 및 기본값 설정
