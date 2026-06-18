---
title: "BridgeOne Phase 4.15: 페이지 커스터마이징"
description: "BridgeOne 프로젝트 Phase 4.15 - 환경설정 내 페이지 추가·삭제·순서변경·편집 및 그리드 기반 컴포넌트 배치 기능 구현"
tags: ["android", "page", "customization", "layout", "grid", "drag-drop", "editor"]
version: "v1.0"
owner: "Chatterbones"
updated: "2026-06-18"
---

# BridgeOne Phase 4.15: 페이지 커스터마이징

**개발 기간**: 미정

**목표**: 환경설정에 "페이지 커스터마이징" 메뉴를 추가하여, 사용자가 페이지를 직접 추가·삭제·순서 변경·편집하고 각 페이지에 컴포넌트를 원하는 위치·크기로 배치할 수 있도록 한다. 현재 `StandardModePage.kt`에 하드코딩된 5개 페이지를 데이터 모델 기반 동적 렌더링으로 전환한다.

**핵심 성과물**:
- 페이지 데이터 모델 및 JSON 직렬화/영속화
- 그리드 기반 동적 페이지 렌더러 (하드코딩 `when` 분기 제거)
- 페이지 목록 편집 UI (순서변경·삭제·추가·복제)
- 페이지 편집 화면 (그리드 드래그/리사이즈·방향·padding·격자수·카탈로그)
- 기존 5개 페이지 기본 템플릿으로 데이터화 + 기존 사용자 설정(엣지존·버튼표시) 마이그레이션

**선행 조건**: **Phase 4.7.4** (StandardModePage 분해 + `StandardModePageState` 상태 홀더 추출) 완료. 동적 렌더러가 `when(page)` 분기를 통째로 대체하므로 분해가 선행돼야 충돌·리스크가 작다.

**에뮬레이터 호환성**: 레이아웃·편집 UI는 에뮬레이터에서 개발 가능. USB 동작 및 터치패드 제스처 회귀 확인은 실기기 필요.

**성능 최적화와의 관계**: 본 Phase는 모든 기능 완성 후 진행하는 `phase-4-16-performance.md`보다 먼저 완료된다. PageLayoutRepository의 전체 JSON 저장은 4.15.2에서 debounce를 기본 포함하며, 더 깊은 부분 저장 최적화는 4.16에서 다룬다.

**설계 문서**: `docs/android/technical-specification-app.md` §2.11, `docs/android/design-guide-app.md` §9

---

## Phase 4.15.1: 데이터 모델 + 직렬화

**개발 기간**: 0.5~1일

**목표**: 페이지 레이아웃을 표현하는 데이터 모델과 JSON 직렬화를 구축한다. 이 단계에서는 기존 코드를 건드리지 않으며, 모델과 직렬화 단위 테스트만 추가한다.

**구현 항목**:
1. `PageLayout`, `PlacedComponent`, `PlacedComponentConfig`(sealed), `ComponentType`(enum), `GridPadding`, `PageOrientation`, `PageKind` 정의
   - `PlacedComponentConfig.Touchpad`는 `edgeZoneAssignment: TouchpadEdgeZoneAssignment`, `buttonVisibility: TouchpadButtonVisibility`, `showControlButtons: Boolean` 포함
   - `PageKind.SETTINGS_NATIVE`: Page5 설정 페이지 예외 처리용
2. `PageLayoutJson.kt` — `ui/common/EdgeZoneJson.kt` 패턴 복제
   - `pageLayoutToJsonObject` / `pageLayoutFromJsonObject`
   - `Touchpad` config 직렬화는 기존 `edgeZoneConfigToJsonObject(assignment.config)` 중첩 호출
   - `MacroButtonCfg.steps` 직렬화는 `EdgeZoneJson`의 `SendMacro` 블록을 헬퍼로 추출해 공유
   - 모든 역직렬화에 `optInt`/`optString` + `runCatching` 폴백
3. `PageLayoutJsonTest.kt` — 라운드트립 단위 테스트

**신규 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/layout/PageLayoutModel.kt`
- `src/android/app/src/main/java/com/bridgeone/app/ui/layout/ComponentType.kt`
- `src/android/app/src/main/java/com/bridgeone/app/ui/layout/PageLayoutJson.kt`
- `src/android/app/src/test/.../ui/layout/PageLayoutJsonTest.kt`

**수정 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/common/EdgeZoneJson.kt` — `SendMacro` 직렬화 블록을 internal 헬퍼로 추출(`macroStepsToJson` / `macroStepsFromJson`)

**검증**:
- [ ] `.\gradlew testDebugUnitTest` 통과 (`PageLayoutJsonTest` 포함)
- [ ] `.\gradlew assembleDebug` 빌드 통과
- [ ] `PageLayout` → JSON → `PageLayout` 라운드트립 동치 확인
- [ ] `PlacedComponentConfig.Touchpad` config 직렬화에서 `EdgeZoneConfig` 값 보존 확인

---

## Phase 4.15.2: Repository + 영속화 + index→id 마이그레이션

**개발 기간**: 0.5~1일

**목표**: 페이지 레이아웃을 `{filesDir}/page_layouts.json`에 저장·불러오는 Repository를 구축하고, 기존 page-index 기반 사용자 설정을 새 모델로 흡수한다. debounce 저장을 기본 포함한다.

**구현 항목**:
1. `PageLayoutRepository.kt` — `TouchpadEdgeZoneAssignmentRepository.kt` 패턴 복제
   - `File(context.filesDir, "page_layouts.json")`
   - 루트 구조: `{ "version": 1, "pages": [...] }`
   - `loadAll(): List<PageLayout>` — 없으면 `defaultPageLayouts()` + 마이그레이션
   - `saveAll(pages: List<PageLayout>)` (debounce: `PAGE_EDIT_SAVE_DEBOUNCE_MS = 300L`)
   - `migrateFromIndexBasedIfNeeded(context)`: `TouchpadEdgeZoneAssignmentRepository.load("standard_page_0/1")`과 `TouchpadButtonVisibilityRepository` 값을 읽어 템플릿 터치패드 config에 흡수. 기존 파일은 삭제하지 않고 방치
2. `DefaultPageTemplates.kt` — 기존 5개 페이지를 `PageLayout` 코드 팩토리로 정의
   - 각 페이지 터치패드 `instanceId`는 고정 상수 UUID (멱등 보장)
   - Page5 설정 페이지는 `kind = SETTINGS_NATIVE`로 정의
   - Page1 기본 그리드: `gridCols=12`, 터치패드 `colSpan=8`, Actions `colSpan=4`
3. `PageEditConstants.kt` — `PAGE_EDIT_SAVE_DEBOUNCE_MS` 등 상수 (기본값 주석 정책 준수)
4. `StandardModePageState`에 `pages: List<PageLayout>` 상태 추가 + `LaunchedEffect(pages)` debounce 저장

**신규 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/common/PageLayoutRepository.kt`
- `src/android/app/src/main/java/com/bridgeone/app/ui/layout/DefaultPageTemplates.kt`
- `src/android/app/src/main/java/com/bridgeone/app/ui/layout/PageEditConstants.kt`

**수정 파일**:
- `StandardModePageState.kt` (Phase 4.7.4-C 산출물) — `pages` 상태 + debounce LaunchedEffect 추가

**검증**:
- [ ] `.\gradlew assembleDebug` 빌드 통과
- [ ] 앱 최초 실행 시 `defaultPageLayouts()` 5개 페이지가 `page_layouts.json`에 저장됨 확인
- [ ] 기존 `touchpad_edge_zone_assignments.json`에 `standard_page_0` 데이터가 있을 때, 마이그레이션 후 해당 엣지존 config가 Page1 터치패드 config에 정확히 흡수됨 확인
- [ ] 앱 재시작 후 `page_layouts.json`에서 동일 `pages` 복원 확인

---

## Phase 4.15.3: 그리드 시스템 + 컴포넌트 레지스트리/어댑터

**개발 기간**: 1~1.5일

**목표**: 그리드 렌더러와 컴포넌트 레지스트리를 구축한다. 이 단계에서는 기존 `HorizontalPager`를 건드리지 않으며, 4.15.4에서 스와핑할 준비만 한다.

> **⚠️ Phase 4.7.4-A 변경사항**: `ActionsPanel`/`SpecialKeysGrid`/`ShortcutsGrid`/`MacrosPlaceholder`가 `ui/pages/standard/components/`에 `internal` standalone Composable로 추출됨. `ComponentRenderer`가 이들을 재추출 없이 직접 디스패치 가능.

> **⚠️ Phase 4.7.4-B 변경사항**: `ui/common/MacroFrameSequencer` 완성. `MACRO_BUTTON` 실행 경로에서 `MacroFrameSequencer.buildMacro(steps, stepDelayMs): List<TimedFrame>`을 호출해 프레임 시퀀스 생성. `TimedFrame(frame: BridgeFrame, delayAfterMs: Long)`. 페이지 비의존 입력(`List<MacroStep>`)이므로 4.15 `MacroButtonCfg.steps`와 직접 호환.

**구현 항목**:
1. `GridContainer.kt` — `BoxWithConstraints`로 `cellW = maxWidth / cols`, 컴포넌트를 `.offset(cellW*colStart, …).size(cellW*colSpan, …)`로 배치
2. `ComponentCallbacks.kt` — 12개 콜백 번들 (4.7.4 분해 후 `StandardModePage` 콜백 배선 압축 — 4.7.4-A 완료 후 정확한 위치/시그니처 확인 후 작성)
3. `ComponentRegistry.kt` + `CatalogMeta.kt` — `ComponentType` → `CatalogEntry(displayName, icon, defaultColSpan, defaultRowSpan, minColSpan, minRowSpan, defaultConfig)` 매핑. `COMPONENT_CATALOG: List<CatalogEntry>` 정의
4. `ComponentRenderer.kt` — `PlacedComponent` + `ComponentCallbacks` → 실제 Composable 디스패치
   - `TOUCHPAD`: `TouchpadWrapper` 본체만 셀에 렌더. DPI/Dynamics/ModePreset 팝업 오버레이는 `DynamicPageRenderer` 루트에서 1회 렌더 (이 파일에서는 제외)
   - `SHORTCUT_BUTTON`, `KEYBOARD_KEY`, `MACRO_BUTTON` 등 나머지 타입은 단순 Composable 호출

**신규 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/layout/GridContainer.kt`
- `src/android/app/src/main/java/com/bridgeone/app/ui/layout/ComponentCallbacks.kt`
- `src/android/app/src/main/java/com/bridgeone/app/ui/layout/ComponentRegistry.kt`
- `src/android/app/src/main/java/com/bridgeone/app/ui/layout/CatalogMeta.kt`
- `src/android/app/src/main/java/com/bridgeone/app/ui/layout/ComponentRenderer.kt`

**검증**:
- [ ] `.\gradlew assembleDebug` 빌드 통과
- [ ] `GridContainer`에 고정 테스트 레이아웃(임시 Composable) 배치 시 셀 크기·위치가 정확한지 시각 확인

---

## Phase 4.15.4: 동적 렌더러 + HorizontalPager 전환 ★분수령

**개발 기간**: 1~1.5일

**목표**: `DynamicPageRenderer`를 완성하고, `StandardModePage`의 하드코딩 `when(page % PAGE_COUNT)` 분기를 동적 페이지 리스트 렌더링으로 전환한다. **완료 후 기존 5개 페이지가 100% 동일하게 동작해야 한다.** 이 단계가 릴리스 가능한 중간 지점이다.

> **⚠️ Phase 4.7.4 변경사항**: 페이지 래퍼(`Page1TouchpadActions` 등)는 4.7.4-A에서 `ui/pages/standard/`로 분리됐으나 이 단계에서 `DynamicPage` 호출로 대체된다. `standardAssignments`/`standardButtonVisibility` 맵은 4.7.4-C에서 `StandardModePageState`로 단순 호이스팅된 상태이며, 이 단계에서 제거하고 `PlacedComponent` config로 흡수한다.

**구현 항목**:
1. `DynamicPageRenderer.kt` — `PageLayout` → `GridContainer` + `ComponentRenderer` 렌더. 터치패드 전역 팝업(DPI/Dynamics/ModePreset 블러 포함) 오버레이를 렌더러 루트에서 1회 처리
2. `StandardModePage` (또는 Phase 4.7.4 분해 후 후속 파일) 수정:
   - `PAGE_COUNT = 5` 상수 → `sharedState.pages.size` 동적화
   - `when(page % PAGE_COUNT) { 0 -> Page1TouchpadActions() ... }` → `DynamicPage(pages[page % pageCount], ...)` 단일 호출로 대체
   - `HorizontalPager` `initialPage` 계산의 `5` → `pageCount` 참조
   - `onCyclePage`/`onJumpToPage`는 동일하게 유지 (pageCount만 동적)
3. `standardAssignments`/`standardButtonVisibility` 맵 상태 제거 (PlacedComponent config로 흡수됨)
4. `PageIndicator`는 `pageCount` 파라미터 이미 받으므로 무변경

**수정 파일**:
- `StandardModePage.kt` (또는 Phase 4.7.4 분해 후 관련 파일)
- 신규: `src/android/app/src/main/java/com/bridgeone/app/ui/layout/DynamicPageRenderer.kt`

**주의사항**:
- `pages.size`가 0이면 HorizontalPager 크래시 — 빈 리스트 방어 처리 필요
- `pages.size` 런타임 변경(편집 후) 시 `pagerState` currentPage 클램프 처리

**검증**:
- [ ] `.\gradlew assembleDebug` 빌드 통과
- [ ] 전환 후 기존 5개 페이지의 **시각적 레이아웃이 100% 동일**함 실기기 확인
- [ ] 엣지존·버튼표시 등 기존 사용자 설정 값이 마이그레이션 후 정확히 반영됨 확인
- [ ] 페이지 스와이프 전환, PageIndicator 닷 클릭, EdgeZone CyclePage 액션 정상 동작 확인
- [ ] 터치패드 엣지존·다이나믹스·DPI 팝업 정상 동작 확인 (렌더러 루트 오버레이 통합 영향)
- [ ] `.\gradlew testDebugUnitTest` 전체 그린 유지

**수동 회귀 체크리스트** (실기기):
- [ ] Page1 커서 이동·클릭·우클릭·스크롤·무한 스크롤·관성
- [ ] Page1 Actions 패널 (특수키·단축키·매크로) 탭 동작
- [ ] Page2 풀스크린 터치패드 동작
- [ ] Page5 설정 화면 정상 표시 (`SETTINGS_NATIVE` 예외 처리)
- [ ] 엣지존 스와이프 트리거 (TOP/BOTTOM/LEFT/RIGHT 4방향)
- [ ] DPI/Dynamics/ModePreset 팝업 열기·닫기·blur

---

## Phase 4.15.5: 페이지 목록 편집

**개발 기간**: 0.5~1일

**목표**: 환경설정에서 페이지 순서변경·삭제·추가·복제를 할 수 있는 목록 편집 화면을 구현한다.

**구현 항목**:
1. `PageListEditorScreen.kt` — `LazyColumn` 기반 페이지 목록
   - 각 행: 페이지 이름 + 그리드 썸네일(축소 렌더) + 편집 진입 버튼
   - 순서 변경: 드래그 핸들 + ▲/▼ 버튼 대안 (터치타겟 56dp+) 동시 제공
   - 삭제: 확인 다이얼로그 포함
   - 추가: 빈 `PageLayout`(기본 PORTRAIT, 12×8 그리드) 생성
   - 복제: `copy(id=UUID.randomUUID().toString(), instanceId 재발급)`
2. `PageEditState.kt` — 편집 세션 상태 홀더 (ViewModel 미사용 컨벤션 유지, `remember` 기반)
3. 진입점: 현 Page5 Settings의 `ZoneEditorEntryRow` 패턴 복제 → `showPageListEditor` 오버레이

**신규 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/layout/editor/PageListEditorScreen.kt`
- `src/android/app/src/main/java/com/bridgeone/app/ui/layout/editor/PageEditState.kt`

**수정 파일**:
- 설정 진입점(현 `Page5Settings` 또는 Phase 4.14 분리 후 `SettingsPage.kt`) — "페이지 커스터마이징" 행 + `showPageListEditor` 상태 추가

**검증**:
- [ ] `.\gradlew assembleDebug` 빌드 통과
- [ ] 설정 화면에서 "페이지 커스터마이징" 항목 탭 → `PageListEditorScreen` 오버레이 진입
- [ ] 페이지 추가 → 새 페이지가 목록에 나타나고 앱 재시작 후 유지
- [ ] 페이지 삭제 → 확인 후 제거, 페이저에서 해당 페이지 사라짐
- [ ] 순서 변경 → 페이저 순서 즉시 반영
- [ ] 페이지 복제 → 동일 레이아웃의 새 페이지 생성 (instanceId는 별도 UUID)

---

## Phase 4.15.6: 페이지 편집 화면

**개발 기간**: 1.5~2일

**목표**: 그리드 위에서 컴포넌트를 드래그/리사이즈하고 방향·padding·격자수를 설정하는 편집 화면을 구현한다. 카탈로그에서 컴포넌트를 꺼내 배치하는 기능도 포함한다.

**구현 항목**:
1. `PageEditScreen.kt`
   - 상단 컨트롤: 방향 세그먼트, padding 4슬라이더(기존 Page5 슬라이더 패턴 재사용), cols/rows 스테퍼
   - 본문: `GridContainer` 편집 모드 — 그리드 라인 오버레이(`PAGE_EDIT_GRID_LINE_ALPHA = 0.15f`) + 컴포넌트 드래그/리사이즈
     - 드래그: `awaitEachGesture`/`drag` + 셀 스냅(`PAGE_EDIT_SNAP_THRESHOLD_DP = 8f`)
     - 리사이즈: 모서리 핸들(`PAGE_EDIT_HANDLE_SIZE_DP = 24f`) 드래그 + `colSpan`/`rowSpan` 스냅
     - 충돌: 빨강 테두리 경고, 겹침 상태에서 저장 불가
     - 모든 드래그/리사이즈에 ▲▼◀▶ 버튼 대안 동시 제공
   - 하단 FAB "컴포넌트 추가" → `ComponentCatalogSheet`
   - 편집 모드 제스처 격리: `HorizontalPager userScrollEnabled = false` + 컴포넌트 자체 제스처 비활성
2. `ComponentCatalogSheet.kt` — `ModalBottomSheet` 형태, `COMPONENT_CATALOG` 표시
   - 항목 탭 → 빈 셀에 `defaultColSpan×defaultRowSpan`으로 자동 배치
3. 편집 모드 컴포넌트 선택 + config 진입점:
   - 배치된 컴포넌트 탭 → 선택 상태 진입 (강조 테두리 + 선택 툴바 등장: 삭제 / 설정 / ▲▼◀▶ 이동)
   - 드래그는 길게 누르기 후 이동으로 구분하여 탭(선택)과 충돌 없이 처리
   - 툴바 "설정" 탭 → `ComponentConfigEditor`로 타입별 config 편집 UI 진입 (4.15.7에서 구현)
   - 카탈로그 배치 직후, `SHORTCUT_BUTTON` / `KEYBOARD_KEY` / `MACRO_BUTTON`은 `ComponentConfigEditor` 자동 오픈. 그 외 타입은 자동 진입 없음

**신규 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/layout/editor/PageEditScreen.kt`
- `src/android/app/src/main/java/com/bridgeone/app/ui/layout/editor/ComponentCatalogSheet.kt`

**검증**:
- [ ] `.\gradlew assembleDebug` 빌드 통과
- [ ] 컴포넌트 드래그 → 그리드 셀에 스냅 배치 확인
- [ ] 리사이즈 핸들 드래그 → `colSpan`/`rowSpan` 변경 확인
- [ ] 충돌 시 빨강 테두리 표시, 저장 버튼 비활성화 확인
- [ ] 카탈로그에서 컴포넌트 추가 → 그리드에 기본 크기로 배치 확인
- [ ] 방향 변경(PORTRAIT↔LANDSCAPE) → 그리드 렌더 즉시 반영 확인
- [ ] padding 슬라이더 조작 → 페이지 여백 실시간 반영 확인
- [ ] 컴포넌트 탭 → 선택 상태 + 선택 툴바 등장 확인
- [ ] SHORTCUT_BUTTON 배치 직후 `ComponentConfigEditor` 자동 오픈 확인
- [ ] 선택 툴바 "설정" 탭 → config 편집 UI 진입 후 복귀 확인 (TOUCHPAD: EdgeZoneEditorScreen)
- [ ] 편집 중 HorizontalPager 페이지 전환이 차단되는지 확인

---

## Phase 4.15.7: 카탈로그 전체 확장 + 컴포넌트별 config 편집

**개발 기간**: 0.5~1일

**목표**: 카탈로그에 모든 컴포넌트 타입을 추가하고, 각 컴포넌트의 config(버튼 키코드, 단축키 조합, 매크로 스텝 등)를 편집 화면에서 직접 편집할 수 있도록 바인딩한다.

**구현 항목**:
1. `ComponentRegistry`에 미구현 타입 추가:
   - `KEYBOARD_LAYOUT` — `KeyboardLayout` Composable 연결
   - `EDGE_ZONE_STRIP` — `EdgeStripEditor` 중첩
   - `SPECIAL_KEY_GRID` — 특수키 그리드 (현 `SpecialKeysGrid`)
2. `ComponentConfigEditor.kt` — 타입별 config 편집 UI 디스패처:
   - `TOUCHPAD` → `TouchpadPageConfigSheet` 호출
   - `SHORTCUT_BUTTON` → 단축키 다이얼로그 (수식키 조합 + 키코드, 레이블; 기존 `ShortcutEditorPrefs` 패턴 참조)
   - `KEYBOARD_KEY` → 키코드 선택 다이얼로그
   - `MACRO_BUTTON` → 매크로 스텝 편집 + 레이블 (기존 `MacroTextEncoder` UI 참조)
   - `EDGE_ZONE_STRIP` → 스트립 방향·할당 액션 다이얼로그
   - `SPECIAL_KEY_GRID` → 표시할 특수키 선택 다이얼로그
   - `KEYBOARD_LAYOUT` → config 없음, 설정 버튼 비활성화
3. `TouchpadPageConfigSheet.kt` — 터치패드 전용 config 편집 바텀시트:
   - 버튼 표시 토글 섹션:
     - 마스터 토글 `showControlButtons` (ON 시 개별 6개 토글 노출)
     - 개별 `ControlButtonConfig` 토글 6개: 클릭 모드 / 이동 모드 / 스크롤 모드 / 커서 모드 / DPI / 스크롤 감도
     - `showDynamicsButton`: 포인트 다이나믹스 버튼
     - `showModePresetButton`: 모드 프리셋 버튼
     - `showScrollButtons`: 스크롤 위/아래 버튼
   - "엣지존 설정 →" 행 탭 → 기존 `EdgeZoneEditorScreen` 중첩 진입 (편집 후 시트로 복귀)
3. `PageEditScreen`에 배치 직후 자동 config 진입 트리거 추가:
   - 카탈로그 배치 완료 시 `SHORTCUT_BUTTON` / `KEYBOARD_KEY` / `MACRO_BUTTON` → `ComponentConfigEditor` 자동 호출

**신규 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/layout/editor/ComponentConfigEditor.kt`
- `src/android/app/src/main/java/com/bridgeone/app/ui/layout/editor/TouchpadPageConfigSheet.kt`

**수정 파일**:
- `ui/layout/ComponentRegistry.kt` — 타입별 `CatalogEntry` 추가
- `ui/layout/ComponentRenderer.kt` — 신규 타입 렌더 케이스 추가
- `ui/layout/editor/PageEditScreen.kt` — 배치 직후 자동 config 진입 트리거

**검증**:
- [ ] `.\gradlew assembleDebug` 빌드 통과
- [ ] `KEYBOARD_LAYOUT` 컴포넌트를 카탈로그에서 꺼내 배치 후 키 입력이 ESP32로 정상 전달 확인
- [ ] `SHORTCUT_BUTTON` config 편집 후 해당 단축키 실행 확인
- [ ] `MACRO_BUTTON` config 편집 후 매크로 실행 확인
- [ ] `.\gradlew testDebugUnitTest` 전체 그린 유지

---

## 종합 검증 (전체 Phase 완료 후)

- [ ] 페이지 추가 → 컴포넌트 배치 → 저장 → 앱 재시작 후 동일 레이아웃 유지
- [ ] 기존 사용자의 엣지존·버튼표시 커스텀 값이 신규 구조로 정확히 마이그레이션
- [ ] Essential 모드 동작에 영향 없음 (기존 Repository 그대로 유지)
- [ ] Page5 설정 화면이 `SETTINGS_NATIVE` 예외로 정상 표시, 편집 카탈로그에 미노출
- [ ] `.\gradlew testDebugUnitTest` 전체 그린
- [ ] `.\gradlew assembleDebug` 빌드 통과
