---
title: "BridgeOne Phase 4.6: 엣지 스와이프 대안 조작 방식 탐색"
description: "BridgeOne 프로젝트 Phase 4.6 - 터치패드 모드/옵션 전환을 위한 다양한 조작 방식을 구현·비교하여 최적의 UX를 선정"
tags: ["android", "edge-swipe", "gesture", "ux", "interaction", "pie-menu", "flick", "zone", "drawing", "swipe-carousel"]
version: "v1.0"
owner: "Chatterbones"
updated: "2026-06-30"
---

# BridgeOne Phase 4.6: 엣지 스와이프 대안 조작 방식 탐색

**목표**: 현재 엣지 스와이프 메뉴의 사용성 한계(5단계 조작)를 극복하기 위해, **여러 대안 조작 방식을 모두 구현**한 뒤 실제 사용 비교 테스트를 통해 최적의 방식을 선정합니다.

**배경**: 기존에는 엣지 스와이프 → 팝업 모드 선택(SWIPE/DIRECT_TOUCH) → 모드 버튼 선택 → 탭 토글 → 확인 버튼의 5단계를 거쳐야 모드 전환이 가능했습니다. "우클릭 모드로 바꾸고 싶다" 같은 단순한 작업에도 5번의 조작이 필요해 사용성이 떨어졌습니다.

> **변경사항**: 팝업 표시 방식(직접 터치/스와이프) 선택 단계를 제거했습니다. 앱 전역 `InputMode`(NORMAL/SWIPE)에 따라 자동으로 결정되어(NORMAL → 직접 터치, SWIPE → 스와이프), 엣지 팝업 방식(`LEGACY_POPUP`)이 4단계(엣지 스와이프 → 모드 버튼 선택 → 탭 토글 → 확인)로 단축되었습니다. 관련 상태 머신은 `TouchpadWrapper.kt`, UI는 `EdgeSwipeOverlay.kt`에 위치.

| 하위 Phase | 내용 | 상태 |
|-----------|------|------|
| 4.6.1 | 임시 환경 설정 페이지 (조작 방식 전환) | 완료 |
| 4.6.2 | 엣지 존(Zone) 분할 방식 + 커스터마이징 | 완료 |

**선행 조건**: Phase 4.5 (E2E 테스트 수정사항) 완료

**에뮬레이터 호환성**: 모든 하위 Phase 에뮬레이터에서 개발 가능. 최종 비교 테스트는 실기기 권장.

**공통 전제**:
- 기존 엣지 스와이프 인프라(산봉우리 시각화, 엣지 감지, `EdgeSwipeConstants`)를 최대한 재사용
- 각 방식은 독립적인 `EdgeInteractionMode` enum 값으로 구분하여, 설정에서 원하는 방식을 선택 가능하게 구현
- 모든 방식에서 조절 가능한 모드/옵션: ClickMode, MoveMode, ScrollMode, DpiLevel, DynamicsPreset, ModePreset

---

## Phase 4.6.1: 임시 환경 설정 페이지 (조작 방식 전환)

### 핵심 설계

`TouchpadState.edgeInteractionMode` 필드와 `EdgeInteractionMode` enum은 이미 존재 (Phase 4.5.18에서 추가됨). 설정 페이지에서 이 값만 변경하면 됨.

**진입 방법**: `StandardModePage`의 `HorizontalPager`를 5페이지로 확장. 기존 4개 페이지(터치패드, 절대좌표, 키보드, 마인크래프트) 뒤에 설정 페이지를 추가. 1페이지에서 우측 스와이프, 또는 4페이지에서 좌측 스와이프로 진입.

**원형(circular) 스와이프**: 1페이지 ↔ 5페이지 간 양방향 wrap-around 지원. `HorizontalPager`의 `pageCount`를 `Int.MAX_VALUE`로 설정하고 초기 페이지를 `PAGE_COUNT(5)`의 배수로 지정, 실제 콘텐츠는 `page % 5`로 매핑.

**설정 페이지 구성**:
- 섹션: "엣지 조작 방식"
- 항목: `EdgeInteractionMode` 각 값에 대한 라디오 버튼 목록
  - LEGACY_POPUP: "기존 팝업 방식 (5단계)"
  - ZONE: "엣지 존 방식"
- 선택 즉시 적용, 앱 재시작 없이 반영
- SharedPreferences에 영속화

### 구현 파일

| 파일 | 변경 |
|------|------|
| `SettingsDialog.kt` (신규) | EdgeInteractionMode 선택 UI (현재 미사용, 참조용으로 보존) |
| `StandardModePage.kt` | `HorizontalPager` 5페이지 확장, circular 스크롤 구현, `Page5Settings` / `SettingsEdgeInteractionModeSection` composable 추가, `PageIndicator` 파라미터 변경 |

### 구현 노트

- `EdgeInteractionMode` enum 추가 (`LEGACY_POPUP`, `ZONE`) → `EdgeSwipeOverlay.kt`
- `TouchpadState.edgeInteractionMode` 필드 추가 (기본값: `LEGACY_POPUP`) → `TouchpadMode.kt`
- `HorizontalPager` pageCount: 5 → `Int.MAX_VALUE` (circular), `PAGER_INITIAL_PAGE` = `Int.MAX_VALUE / 2`를 5의 배수로 내림
- 콘텐츠 매핑: `when (page % PAGE_COUNT)` — 논리 페이지 0~4
- `Page5Settings` composable 추가 (`StandardModePage.kt` 내) — "환경 설정" 제목 + `SettingsEdgeInteractionModeSection`
- `SettingsEdgeInteractionModeSection` composable 추가 — `EdgeInteractionMode.entries` 순회하여 RadioButton 목록 렌더링
- `PageIndicator` 파라미터 변경: `pagerState: PagerState` → `currentPage: Int, offsetFraction: Float` — wrap-around 전환(0→4, 4→0) 시 `offsetFraction = 0`으로 고정하여 worm이 화면 밖으로 튀는 현상 방지
- SharedPreferences 저장/로드: `StandardModePage.kt`에 `loadEdgeInteractionMode` / `saveEdgeInteractionMode` 추가 (기존 `PREF_NAME` 재사용)
- `SettingsRepository.kt`는 별도 파일 미생성 — 기존 DPI 저장 패턴(private 함수)으로 통합

**앱 전체 조작 방식(InputMode) 인프라 추가**:
- 환경 설정 페이지 최상단에 "조작 방식" 섹션 추가. 앱 전체 UI 조작 방식을 일반(NORMAL) ↔ 스와이프(SWIPE) 중 하나로 선택. 두 방식은 런타임에 배타적 (동시 활성 불가)
- `InputMode` enum 신규 (`NORMAL`, `SWIPE`) → `ui/common/InputMode.kt`
- `loadInputMode` / `saveInputMode` top-level 함수 → 동일 파일, `input_mode_prefs` SharedPreferences 사용
- `StandardModePage`: `inputMode` 상태를 페이지 레벨로 호이스팅, `Page5Settings`에 전달
- `Page5Settings`: "조작 방식" 섹션을 "엣지 조작 방식" 섹션 위에 배치 (상위 개념)
- `SettingsInputModeSection` composable 신규 — 각 모드에 레이블 + 설명 텍스트 표시

**EdgeZoneEditorScreen SWIPE 레이어 1차 통합**:
- 인터랙션 모델: 2단계 모드 시스템 (Selection / Manipulation). 4방향 스와이프 = 포커스 이동, 탭 = 활성화 또는 조작 모드 진입, 더블탭 = 길게 누르기 등가, 조작 모드에서 스와이프 = 직접 조정 (감도는 화면 너비 기준 적응적)
- 신규 일반 인프라 파일:
  - `ui/common/swipe/SwipeFocusController.kt` — `SwipeFocusController`, `SwipeMode`, `Direction`, `FocusableElement`, `FocusableEntry`, `ROOT_SCOPE`, `LocalSwipeFocusController`, `rememberSwipeFocusController()`. 좌표 기반 nearest-neighbor traversal
  - `ui/common/swipe/SwipeFocusable.kt` — wrapper composable. `onGloballyPositioned`로 bounds 등록, 포커스 시에만 외곽선/배경 modifier 적용 (비포커스 시 레이아웃 영향 없음)
  - `ui/common/swipe/SwipeGestureLayer.kt` — 최상단 transparent 오버레이. 탭/스와이프/더블탭 분류, MANIPULATION 모드에서 연속 deltaPx 전달. 더블탭 윈도우는 `LaunchedEffect`로 처리
- 신규 편집기 전용 파일:
  - `ui/components/touchpad/EdgeZoneEditorFocus.kt` — `EdgeEditorElement` sealed class (약 30개 케이스), `EdgeEditorScope` (Root/UndoMenu/MoreMenu/PresetPopup/IconSheet/ZoneActionPopup/ResetDialog/DiscardDialog/LabelKeyboard)
- 현재 SWIPE 모드 동작 가능 항목: Top/Bottom 바 네비게이션, 캔버스 엣지/코너 선택, 존 선택, 경계선 조작 모드 비율 조정, 더블탭으로 Undo 히스토리/존 액션 팝업 열기, 메뉴/팝업 scope 자동 전환
- **미구현 (후속 작업 대상)**: ActionTypeToggle, ActionDomainPicker 내부 요소들, CustomMultiplierSlider 조작 모드, RotationEditor 요소들, 메뉴/팝업 내부 항목 개별 wrap, 편집 패널 `bringIntoViewRequester` 자동 스크롤, 설정 변경 시 즉시 반영을 위한 lifecycle resume hook
- 재사용 패턴: 다른 화면에 SWIPE 레이어 추가 시 `SwipeFocusController`/`SwipeFocusable`/`SwipeGestureLayer` + 화면별 `*Element` sealed class + 호이스팅 + `SwipeFocusable` wrap + popup scope `DisposableEffect`

### 검증

- [x] 1페이지에서 우측 스와이프 시 설정 페이지(5페이지)로 이동 확인
- [x] 5페이지에서 좌측 스와이프 시 1페이지로 wrap-around 확인
- [x] LEGACY_POPUP / ZONE 선택 즉시 적용 확인
- [x] 앱 재시작 후에도 선택한 방식이 유지되는지 확인

---

## Phase 4.6.2: 엣지 존(Zone) 분할 방식 + 커스터마이징

**개발 기간**: 2~3주

**목표**: 터치패드 4개 엣지를 구간(존)으로 나누고, 각 구간에 원하는 동작을 직접 배정하는 방식을 구현합니다. 엣지의 어느 위치에서 진입했느냐가 곧 동작을 결정하므로 LEGACY_POPUP의 5단계 과정이 1단계로 줄어듭니다. 구간 배치와 동작 배정은 전용 편집기에서 자유롭게 변경할 수 있습니다.

### 존(Zone) 개념

**엣지와 존**: TOP/BOTTOM/LEFT/RIGHT 각 엣지를 여러 구간(존)으로 분할할 수 있습니다. 각 존은 자신만의 동작·라벨·아이콘·색상을 가집니다. 같은 엣지의 모든 존 비율 합은 항상 1.0이며, 존당 최소 비율(`MIN_ZONE_RATIO = 0.10f`)과 엣지당 최대 존 수(`MAX_ZONES_PER_EDGE = 4`)가 제한됩니다.

**모서리 버튼 제약**: 모드 변경 버튼이 특정 모서리에 배치된 경우, 해당 모서리에 인접한 두 엣지의 끝 구간(`CORNER_BUTTON_BLOCKED_RATIO = 0.15f`)은 자동으로 Unassigned로 고정됩니다. 편집기에서 회색으로 표시되며 수정할 수 없습니다.

**두 엣지가 겹치는 코너**: 코너 부분은 어느 엣지의 존으로 처리할지 우선순위를 설정할 수 있습니다. 편집기 미리보기의 코너를 탭하면 우선 엣지가 전환됩니다.

### 존 트리거

각 존에는 두 가지 트리거 방식 중 하나를 설정합니다.

- **SingleAction**: 트리거 임계값 도달 시 지정된 동작 1회 실행 후 종료
- **Rotation**: armed 상태에서 설정된 간격(intervalMs)마다 후보 동작을 순환 표시하다가, 손을 뗄 때 현재 후보를 실행. 하나의 존에 여러 동작을 순서대로 돌릴 수 있습니다

### 배정 가능한 동작

| 분류 | 동작 |
|------|------|
| 토글/순환 | 클릭 모드(좌↔우), 스크롤 모드(OFF↔ON), 이동 모드(자유↔직각), 일반↔무한 스크롤 전환, DPI 단계 순환, 스크롤 속도 단계 순환, 다이나믹스 프리셋 순환, 모드 프리셋 순환, 이전 상태 복원 |
| 직접 지정 | 클릭 모드/이동 모드/스크롤 모드 직접 지정, DPI 레벨 직접 지정, 스크롤 속도 직접 지정, 커스텀 DPI·스크롤 배율 슬라이더, 모드·다이나믹스 프리셋 인덱스 직접 점프 |
| 키보드·마우스 | 단축키 1회 발송, 다단계 매크로 실행, 마우스 버튼 홀드/릴리즈/토글 |
| 내비게이션 | 다음/이전 페이지 전환, 특정 페이지로 이동 |
| 미할당 | 아무 동작 없음 |

### 시각적 가이드

idle 상태에서 각 존을 **고유 색상 블록 + 아이콘**으로 상시 표시합니다. 화면을 직접 보지 않아도 위치를 외워 사용할 수 있도록, 편집기 미리보기와 실제 터치패드에서 동일한 색상 팔레트를 씁니다.

- 아이콘은 존 중앙에 16dp 크기로 표시. 세로 엣지처럼 공간이 좁으면 아이콘만 표시
- Unassigned 존은 회색 블록으로만 표시 (아이콘·라벨 없음)
- 진입 시 해당 존만 흰색 fill로 강조되고, Rotation 존은 현재 후보 아이콘이 순환 표시됨
- 존 경계 진입 시 가벼운 햅틱 틱
- 오디오 피드백이 켜져 있으면 존 진입 시 라벨을 TTS로 읽어줌

### 프리셋

**구조 프리셋**: 존 분할 비율 구조(레이아웃)만 저장하며 동작 배정은 포함하지 않습니다. 프리셋을 적용하면 존 구조는 교체되지만 기존 동작 배정은 Unassigned로 초기화됩니다.

빌트인 프리셋 7종이 내장되어 있으며(기본, 각 1존, 각 2분할, 각 3분할, 상하만, 좌우만, 위 미사용), 사용자가 직접 구성을 저장해 커스텀 프리셋으로 관리할 수 있습니다.

### 커스터마이징 편집기

**진입 경로**: Page 5 설정 페이지 → "엣지 존 편집" 항목 (ZONE 모드일 때만 표시)

```
┌─ ← Back | PresetBadge (현재 프리셋) | Undo
├─ 미리보기 (Canvas):
│   - 터치패드 모형 + 4엣지를 색상 블록으로 렌더링
│   - 엣지 탭 → 해당 엣지 선택 (스트립 편집기 전환)
│   - 코너 탭 → 해당 코너의 우선 엣지 전환
│   - 캔버스 씬 중앙: 구조 변경 모드 버튼 5개 (병합/분할/이동/삭제/비율)
├─ 존 스트립 (선택된 엣지):
│   - 존들을 가로 한 줄로 표시
│   - 경계 핸들 드래그 → 인접 두 존의 비율 실시간 조정 (MIN_ZONE_RATIO 보장)
│   - 존 탭 → 액션 편집 패널로 전환
├─ 액션 편집 패널 (선택된 존):
│   - SingleAction 모드: 도메인 분류 → 동작 선택 → 배율 슬라이더(해당 시)
│     아이콘 / 색상 / 라벨 개별 설정, 자동값으로 되돌리기
│   - Rotation 모드: 후보 목록 관리(순서·추가·삭제), 순환 간격 설정
└─ [저장] — 확정 전까지 변경은 임시 상태로 유지
```

변경 사항은 [저장] 버튼을 누르기 전까지 임시 상태에만 보관됩니다. Undo 버튼으로 이전 상태로 되돌릴 수 있습니다. 존 추가 시 해당 엣지에서 가장 큰 존을 절반으로 분할하며, 삭제 시 인접 존이 공간을 균등 흡수합니다.

**아이콘 선택**: Material Icons를 13개 카테고리(포인터/화살표/미디어/편집/시스템/도형/기호/커뮤니케이션/파일/데이터/날씨/감정/기기)로 분류한 팔레트에서 선택합니다.

**색상 선택**: 11개 카테고리 × 12색 팔레트에서 선택하거나, HSV 슬라이더 + Hex 직접 입력으로 커스텀 색상을 지정할 수 있습니다.

### 캔버스 구조 변경 모드 (병합/분할/이동/삭제/비율)

캔버스 씬에 존 구조를 바꾸는 5개 모드를 통합 제공합니다. `CanvasEditMode` sealed class(`None` / `Merging` / `Splitting` / `Moving` / `Deleting` / `Resizing`)로 상태를 관리하며, 캔버스 중앙에 모드 진입 버튼 5개(아이콘+라벨 카드)가 표시됩니다. NORMAL은 카드 탭, SWIPE는 `SwipeFocusable` 포커스+탭으로 진입합니다.

**병합(MERGE)**: 같은 엣지 내 인접 존을 다중 선택해 하나로 합칩니다. `MergeSelectionOverlay`가 선택 구간을 초록 영역 박스로 강조하고, `buildMergeMorph`로 생성된 `ZoneMorph`가 수축 애니메이션을 구동합니다.

**분할(SPLIT)**: 대상 존 1개를 선택한 뒤 나눌 갯수(2~5)를 선택합니다. `computeSplitZones`가 폭을 균등 분배하고, `buildSplitMorph`가 확장 애니메이션을 구동합니다.

**삭제(DELETE)**: cross-edge 다중 선택 후 일괄 삭제합니다. `deleteZones`가 엣지별로 정렬 삭제하며 인접 존이 공간을 흡수합니다.

**비율(RESIZE)**: `ZoneCanvasResizeOverlay`가 각 엣지의 내부 경계마다 `SwipeFocusable`을 배치합니다. NORMAL은 경계 드래그, SWIPE는 경계에 포커스 후 MANIPULATION 진입(가로 스와이프)으로 조정합니다. 모드 진입 시 `beginResizeMode`로 스냅샷을 기록하고, 취소(discardResizeMode)·확인(commitResizeMode)으로 세션을 종료합니다. SWIPE MANIPULATION 진입 시 Undo 스택에 1회 적립(`LaunchedEffect(swipeController.mode)`)합니다.

**코너 차단 영역 크기 조절**: 편집기 캔버스 씬에 `CornerBlockedSlider`(SWIPE 포커스 가능)를 배치해 코너 버튼 차단 비율(`cornerBlockedRatio`, 0.05~0.30)을 실시간 미리보기와 함께 조절할 수 있습니다. '저장' 시점에만 `saveCornerBlockedRatio`로 영속화됩니다.

**ZoneMorph 보간 시스템**: 병합·분할·이동 확정 시 `ZoneMorph`(startFrame→endFrame 쌍)를 생성하고, `LaunchedEffect`가 0→1 보간으로 존 비율을 부드럽게 전환합니다. 이동은 선택 존을 `ZoneMoveFloatingOverlay`로 따로 렌더해 나머지 존만 morph하며 "고스트가 떠다니는" 효과를 표현합니다.

**이동(MOVE)**은 존을 다른 위치로 옮깁니다. `존 선택 → 존 이동 → 존 내려놓기` 3단계로 동작하며, **cross-edge 이동**(예: 상단 존 → 좌측 엣지)과 **엣지 양 끝 + 존 사이 경계** 드롭을 모두 지원합니다. 같은 엣지 재배치는 폭을 보존하고, cross-edge는 출발 엣지 잔여 존을 비율대로 [0,1] 재분배하고 도착 엣지는 삽입 존(폭 carry) 포함 재정규화합니다. 액션·라벨·아이콘·Rotation 후보 등 trigger는 전부 보존됩니다.

**조작 방식 설정** (`ZoneMoveMethod`, `input_mode_prefs`에 영속화):
- **탭**(기본, 모든 레이어): 존을 탭해 들어올린 뒤 드롭 위치(경계/양 끝)를 탭. SWIPE 레이어는 드롭 슬롯을 포커스 대상(`CanvasDropSlot`)으로 렌더.
- **드래그 앤 드롭**(NORMAL 전용): 존을 잡고 끌면 다른 존이 실시간으로 밀려나는 미리보기가 표시되고, 손을 떼면 가장 가까운 경계에 안착.
- 설정 UI는 Page 5의 ZONE 섹션에 노출되며, **SWIPE 레이어에서는 옵션이 숨겨지고 탭으로 강제**됩니다.

**들림 고스트 + 밀림 미리보기**(이동 추적이 있는 흐름 = SWIPE 포커스 이동 / NORMAL 드래그):
- 선택한 존을 **반투명(α 0.35) + 드롭섀도 + 주황 보더**의 "들어올린 고스트"로 렌더하고, 다른 존은 그 위치에 맞춰 실시간으로 밀려납니다(`computeMove` 결과를 `displayConfig`에 반영). 고스트는 trigger 참조 보존을 이용해 미리보기 config에서 식별(`liftedKey`).
- SWIPE는 포커스 슬롯을 옮길 때 직전→새 미리보기를 `lerpConfig`로 보간해 **부드럽게 따라 이동**(같은 엣지 매끄럽게, cross-edge는 해당 엣지만 즉시). NORMAL 드래그는 터치 추적이므로 즉시 반영(spring 없음).
- 이때 SWIPE 드롭 슬롯의 경계선 마커는 표시하지 않고(`showMarker=false`), 고스트가 위치를 대신 알립니다.
- **NORMAL 탭**은 추적이 없어 고스트가 따라올 수 없으므로 기존 드롭 후보 마커(`ZoneCanvasDropOverlay`, `showMarker=true`)를 유지합니다.

**SWIPE 드롭 단계 포커스 동작**:
- 존을 들어올리면(picked) 포커스는 **선택 존의 원래 위치 슬롯**으로 시드되어 고스트가 제자리에서 시작합니다.
- 들어올린 뒤 스와이프는 `moveInterceptor`(`movingDropNav`)로 **드롭 슬롯끼리만** 이동합니다(TopAppBar 등 캔버스 밖 요소 제외). 방향 네비는 같은 엣지 슬롯을 우선하므로, 한 엣지 내 인접 존과 자리 교환이 직관적으로 됩니다.

**확인 / 되돌리기**:
- 들어올린 존이 없는 단계(존 선택 단계)에는 캔버스 중앙에 **'확인' 버튼**(`CanvasModeConfirm`)이 표시됩니다. 누르면 지금까지의 이동을 **그대로 유지한 채** 모드 선택 화면(`CanvasEditMode.None`)으로 복귀합니다. SWIPE에서는 엣지 존에서 안쪽으로 스와이프하면 확인 버튼에 닿습니다(`movingPickNav` isTarget에 `CanvasModeConfirm` 포함).
- **롱프레스**는 이 이동 모드 세션에서 커밋된 이동을 **역순으로 한 단계씩 애니메이션하며 모두 되돌린 뒤** 모드 선택 화면으로 복귀합니다(NORMAL 탭 롱프레스 / SWIPE 롱프레스 공통). 세션 시작 지점은 모드 진입 시 `state.beginMoveMode()`가 Undo 스택 크기로 기록하며, 그 이전의 편집은 보존됩니다. 역순 되돌리기는 `state.moveRevertRequested` 플래그를 화면의 `LaunchedEffect`가 감지해 단계마다 `ratioMorph` 보간을 구동합니다.
- **NORMAL 드래그 앤 드롭** 방식은 롱프레스 제스처가 없으므로(드래그가 점유), 존 선택 단계에 '확인'과 함께 **'취소' 버튼**(`CanvasModeCancel`)을 나란히 노출합니다. 취소는 롱프레스와 동일하게 `onMovingLongCancel`(역순 되돌리기)로 동작합니다. 탭/스와이프 방식은 롱프레스가 되돌리기를 담당하므로 '확인'만 노출합니다.

거부 규칙: 출발 엣지의 마지막 1개 존은 이동 불가, 도착 엣지가 `MAX_ZONES_PER_EDGE`를 초과하거나 `MIN_ZONE_RATIO` 위반 시 거부(토스트), 비활성 엣지는 드롭 대상에서 제외. 원위치 드롭은 Undo 미적립 no-op. 확정 시 출발·도착 엣지를 동시에 보간하는 모핑(`zoneMorphs` 리스트)으로 전환합니다.

### 매크로·단축키

단축키와 매크로를 존 동작으로 직접 배정할 수 있습니다. 단축키 프리셋과 매크로 프리셋은 각각 별도로 관리되며 앱 재시작 후에도 유지됩니다. 기본 매크로 프리셋 6종이 내장되어 있습니다(안녕하세요, 감사합니다, 전체선택후잘라내기, 전체선택후복사, 실행취소×3, 화면영역캡처).

매크로는 텍스트를 입력하면 키 시퀀스로 자동 변환되며, 한/영 전환 삽입과 두벌식 한글 분해를 자동 처리합니다. TAP(입력)/HOLD(누름 유지)/RELEASE(뗌) 세 가지 스텝 타입을 조합할 수 있습니다. 매크로 실행 중에는 화면 전체가 반투명 오버레이로 덮혀 터치 입력이 차단됩니다.

### 오디오 피드백

설정에서 활성화하면 존에 진입할 때마다 해당 존의 라벨을 TTS로 읽어줍니다. 읽기 속도와 성별(기본/여성/남성)을 설정할 수 있습니다.

### 신규 파일

**`ui/common/`**:
- `EdgeZoneJson.kt`, `EdgeZonePresetConstants.kt`, `EdgeZonePresetsRepository.kt`
- `TouchpadEdgeZoneAssignment.kt`, `TouchpadEdgeZoneAssignmentRepository.kt`
- `IconCategory.kt`, `ColorCategory.kt`, `ColorCodec.kt`, `ColorRegistry.kt`, `SwatchPalette.kt`
- `MacroConstants.kt`, `MacroTextEncoder.kt`, `MacroScrimOverlay.kt`
- `CustomMacroPresetsRepository.kt`, `CustomShortcutPresetsRepository.kt`
- `AudioController.kt`, `AudioFeedbackPrefs.kt`

**`ui/components/touchpad/`**:
- `EdgeZone.kt`, `EdgeZoneDetector.kt`, `EdgeZoneOverlay.kt`, `EdgeZoneActionHandler.kt`
- `EdgeZoneEditorScreen.kt`, `EdgeZoneEditorPreviewCanvas.kt`, `EdgeZoneEditorFocus.kt`
- `EdgeStripEditor.kt`, `EdgeZonePresetPopup.kt`, `ModeHistoryStack.kt`
- `IconRegistry.kt`, `NormalCategoryIconSheet.kt`, `NormalCategoryColorSheet.kt`, `CategoryIconDrawer.kt`
- `EdgeZoneCanvasMode.kt` — `CanvasEditMode` sealed(None/Merging/Splitting/Moving/Deleting/Resizing), `CanvasModeKind`, `ZoneKey`, `DropTarget`, SWIPE 방향 우선 공간 네비게이션 함수(canvasSpatialNav/movingPickNav/movingDropNav)
- `EdgeZoneCanvasModeOverlay.kt` — 캔버스 씬 모드 진입 버튼 5개 + 각 모드 진행 UI(NORMAL 입력 통합). `onConfirm`/`onModeChange` 콜백 구조
- `EdgeZoneCanvasGeometry.kt` — 캔버스 좌표 변환 공용 함수(`edgeValidRange`, `mapToValid`, `unmapFromValid`, `unmapClamped`, `zoneRect`, `findBoundaryAt`, `BoundaryHit`)
- `EdgeZoneMorph.kt` — `ZoneMorph`(startFrame→endFrame 보간), `ConfigMorph`, `buildMergeMorph`, `buildSplitMorph`, `buildMoveReflowMorphs`
- `ZoneMoveFloatingOverlay.kt` — 이동 커밋 애니메이션에서 출발→도착으로 떠다니는 선택 존 렌더러(`ZoneMoveFloat`, `ZoneStrip`, 코너 차단 클리핑 동기화)
- `ZoneCanvasDropOverlay.kt` — SWIPE 이동 모드 드롭 슬롯 `SwipeFocusable` 오버레이(`showMarker` 플래그로 고스트 흐름에서 마커 숨김 지원)
- `ZoneCanvasResizeOverlay.kt` — SWIPE 비율 조정 모드 경계 핸들 `SwipeFocusable` 오버레이(경계 포커스+탭→MANIPULATION 진입)
- `MergeSelectionOverlay.kt` — 병합 선택 구간 영역 박스 강조(애니메이션으로 경계 보간)
- `BoundaryManipulationHint.kt` — SWIPE MANIPULATION 진입 시 표시하는 조작 안내 카드

**`ui/components/colorpicker/`**:
- `ColorPickerFocus.kt`, `ColorPickerSwipe.kt`

### 수정 파일

- `TouchpadMode.kt` — `edgeZoneConfig` 필드 추가
- `TouchpadWrapper.kt` — ZONE 케이스 + Rotation armed 처리
- `StandardModePage.kt` — Page5에 편집기 진입 항목, 존 설정 로드/저장
- `BridgeOneApp.kt` — `MacroScrimOverlay` 최상위 배치
- `MainActivity.kt` — `AudioController` 초기화/종료
- `ScrollConstants.kt` — 신규 제약 상수 추가
- `InputMode.kt` — `ZoneMoveMethod` enum(`TAP`/`DRAG_AND_DROP`), `loadZoneMoveMethod`/`saveZoneMoveMethod` 추가 (`input_mode_prefs` 재사용)
- `EdgeZoneEditorState.kt` — `ratioMorph`/`moveRevertRequested` 상태 추가; 이동 관련 함수(`computeMove`, `commitMove`, `validateMove`, `dropInsertIndex`, `rescaleToFill`, `beginMoveMode`, `canRevertMove`, `popMoveUndo`), 병합(`mergeContiguous`), 분할(`computeSplitZones`), 다중 삭제(`deleteZones`), 비율 조정(`adjustBoundary`, `beginResizeMode`/`discardResizeMode`/`commitResizeMode`, `beginResizeSession`/`discardResizeSession`/`revertBoundaryManipulation`) 추가; `MoveRejection` sealed class(SourceLastZone/TargetFull/TooSmall/DisabledEdge)
- `EdgeZoneEditorScreen.kt` — `canvasMode`/`cornerBlockedRatio` 상태 추가; SWIPE `moveInterceptor` `DisposableEffect`(`canvasSpatialNav`/`movingPickNav`/`movingDropNav`); 캔버스 씬 포커스 초기화·복귀 로직; 이동 모드 포커스 시드 `LaunchedEffect`; 비율 MANIPULATION Undo 적립 `LaunchedEffect`
- `EdgeZoneEditorFocus.kt` — `CanvasModeButton`/`CanvasModeConfirm`/`CanvasModeCancel`/`CanvasSplitChoice`/`CanvasBoundary`/`CanvasDropSlot`/`CanvasRatioPreset`/`CornerBlockedSlider` 포커스 요소 추가; `CanvasMode` scope 추가; `ZoneActionMove*` 관련 요소 제거(캔버스 모드 통합으로 대체)
- `Page5Settings.kt` — 존 이동 방식 UI(`ZoneMoveMethod` 선택 칩, NORMAL 레이어에서만 노출) 추가; `cornerBlockedRatio` 파라미터 추가
- `StandardModePrefs.kt` — `loadCornerBlockedRatio`/`saveCornerBlockedRatio` 추가 (기본값: `CORNER_BUTTON_BLOCKED_RATIO`, 0.05~0.30 clamp)
- `SwipeFocusController.kt` — `moveInterceptor` 지원 추가 (방향 이동 시 interceptor 먼저 실행, 반환 true이면 기본 traversal 스킵)
- `ZoneRatioSection.kt` — 삭제 (기능이 `ZoneCanvasResizeOverlay`로 이전)

### 검증

- [x] 기본 프리셋: 각 엣지 존에서 안쪽으로 밀면 정의된 동작 실행
- [x] idle 시 존 색상 블록 + 아이콘 상시 표시 확인
- [x] 진입 시 해당 존 흰색 fill 강조 확인
- [x] Rotation 존: armed 상태에서 후보 순환, 손 뗄 때 현재 후보 실행 확인
- [x] Page 5 → "엣지 존 편집" 항목이 ZONE 모드일 때만 표시 확인
- [x] 편집기 미리보기에서 엣지 탭 → 존 스트립 전환 확인
- [x] 경계 핸들 드래그로 비율 조정, MIN_ZONE_RATIO 보장 확인
- [x] 존 탭 → 액션/아이콘/색상/라벨 변경 확인
- [x] 존 병합/분할/삭제, MAX_ZONES_PER_EDGE 제한 확인
- [x] 캔버스 이동 모드(탭): 존 선택 후 경계/양 끝 탭으로 같은 엣지 재배치·cross-edge 이동, trigger 보존, 원위치 드롭 no-op (NORMAL·SWIPE 양쪽)
- [x] 캔버스 이동 모드(드래그, NORMAL): 존을 끌면 다른 존 실시간 밀림, 릴리스 시 안착, 가득 참/최소 비율 위반 거부 토스트
- [x] 존 이동 방식 설정: NORMAL에서 탭/드래그 선택 노출, SWIPE에서 옵션 숨김+탭 강제
- [x] 들림 고스트(SWIPE): 존 선택 후 스와이프로 포커스 이동 시 선택 존이 반투명 고스트로 들려 따라가고 다른 존 밀림, 같은 엣지 글라이드·cross-edge 즉시, 슬롯 경계선 마커 미표시
- [x] 들림 고스트(NORMAL 드래그): 끄는 동안 선택 존 반투명 고스트 + 다른 존 즉시 밀림
- [x] NORMAL 탭: 고스트 미적용, 드롭 후보 마커 유지 (회귀)
- [x] SWIPE 드롭 포커스: 들어올린 직후 고스트가 원래 자리 유지, 스와이프 시 드롭 슬롯만 포커스(상단바 안 됨), 같은 엣지에서 인접 존과 자리 교환(예: 우측 상단 ↓ → DPI와 교환)
- [x] 이동 '확인': 존 선택 단계 중앙 '확인' 버튼 → 이동 유지한 채 모드 선택 복귀 (NORMAL·SWIPE), SWIPE는 안쪽 스와이프로 확인 버튼 포커스 도달
- [x] 이동 롱프레스 되돌리기: 세션 내 이동을 역순 애니메이션으로 모두 되돌린 뒤 모드 선택 복귀 (NORMAL 탭 롱프레스 / SWIPE 롱프레스), 모드 진입 이전 편집은 보존
- [x] 이동 '취소'(NORMAL 드래그): 존 선택 단계 '확인' 옆 '취소' 버튼 → 롱프레스와 동일하게 역순 되돌리기 후 모드 선택 복귀
- [x] 단축키·매크로 액션 배정 후 트리거 시 실행 확인
- [x] 매크로 실행 중 MacroScrimOverlay 표시 확인
- [x] CyclePage / JumpToPage 액션 동작 확인
- [x] 구조 프리셋 적용 시 액션 Unassigned 초기화 확인
- [x] 커스텀 프리셋 추가/편집/삭제 확인
- [x] 저장 후 앱 재시작 시 커스텀 구성 유지 확인
- [x] LEGACY_POPUP 선택 시 기존 팝업 동작 유지 (회귀 없음)
- [x] 오디오 피드백 활성화 시 존 진입 시 라벨 TTS 출력 확인
- [x] 모서리 버튼 제약: 해당 구간 편집기에서 회색 비활성화, 수정 불가 확인
