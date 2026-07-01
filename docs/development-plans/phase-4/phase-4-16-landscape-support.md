---
title: "BridgeOne Phase 4.16: 가로 방향(Landscape) 지원"
description: "모든 페이지/모드 구현 완료 후, 세로 전용 레이아웃을 가로 방향에서도 사용 가능하도록 확장. 앱 설정 방향 3택(세로/가로/자동) + 방향별 레이아웃 분기 + 방향 전환 시 상태 유지"
tags: ["android", "landscape", "orientation", "layout", "responsive", "accessibility"]
version: "v1.0"
owner: "Chatterbones"
updated: "2026-07-01"
---

# BridgeOne Phase 4.16: 가로 방향(Landscape) 지원

**개발 기간**: 3-4주 (추정)

**목표**: 현재 세로(portrait) 고정인 앱을 가로(landscape) 방향에서도 사용할 수 있도록 확장합니다. 앱 설정에서 방향(세로/가로/자동)을 선택하면 해당 방향으로 동작하고, 각 페이지·모드가 방향에 맞는 레이아웃으로 렌더링됩니다. 방향 전환 시 입력/연결/멀티커서 등 상태는 유지됩니다.

**핵심 성과물**:
- `AndroidManifest.xml` 방향 고정 해제 + `configChanges`로 회전 시 액티비티 재생성 방지
- 방향 감지 유틸 + 좌우 Safe Zone 상수 (`LayoutConstants.kt` 확장)
- 앱 설정 방향 3택(세로 고정 / 가로 고정 / 자동) — Phase 4.14 `SettingsRepository`에 항목 추가
- 최상위 레이아웃(`BridgeOneApp.kt`) 방향 분기
- Essential 모드 + Page 1~5 각각의 가로 레이아웃
- 존 편집/엣지 스와이프 오버레이의 가로 대응
- 방향 전환 중 상태 유지 및 전체 회귀 검증

**선행 조건**: Phase 4.15 완료 (모든 페이지·모드·설정·커스터마이징 완료). 가로화는 완성된 세로 레이아웃 위에서 진행하므로 **모든 페이지 Phase(4.8~4.11), 설정(4.14), 커스터마이징(4.15)이 끝난 뒤** 착수합니다. 성능 최적화(Phase 4.17)는 가로 레이아웃까지 들어간 최종 상태에서 프로파일링해야 하므로 본 Phase **이후**에 진행합니다.

> **방향 전환 시 상태 유지 원칙**: Phase 4.7이 ViewModel/Hilt 대신 상태 홀더(`remember`) 패턴을 확립했으므로, 회전 시 액티비티가 재생성되면 `remember` 상태가 날아간다. 자동(sensor) 모드를 지원하려면 `android:configChanges="orientation|screenSize"`로 **재생성 자체를 막는 것**이 전제다(입력 앱 표준). 영구 보존이 필요한 값은 기존 Repository 패턴으로 저장. 관련 배경: [[project_future_landscape_support]]

**에뮬레이터 호환성**: 레이아웃 분기·방향 설정 UI는 에뮬레이터(방향 회전)에서 개발 가능. 실제 조작감(가로 자세에서 터치패드/존/햅틱)은 실기기 검증 필요.

| 하위 Phase | 내용 | 상태 |
|-----------|------|------|
| 4.16.1 | 방향 인프라 (매니페스트·감지 유틸·Safe Zone·설정 3택) | 미시작 |
| 4.16.2 | 최상위 레이아웃 방향 분기 (BridgeOneApp) | 미시작 |
| 4.16.3 | Essential 모드 가로 레이아웃 | 미시작 |
| 4.16.4 | Page 1 (터치패드 + Actions) 가로 레이아웃 | 미시작 |
| 4.16.5 | Page 2~5 가로 레이아웃 | 미시작 |
| 4.16.6 | 존 편집 / 엣지 스와이프 오버레이 가로 대응 | 미시작 |
| 4.16.7 | 방향 전환 상태 유지 + 통합 검증 | 미시작 |

---

## Phase 4.16.1: 방향 인프라 (매니페스트·감지 유틸·Safe Zone·설정 3택)

**개발 기간**: 1-1.5일

**세부 목표**:
1. `AndroidManifest.xml` 수정:
   - `MainActivity`의 `android:screenOrientation="portrait"` 제거
   - `android:configChanges="orientation|screenSize|keyboardHidden"` 추가 (회전 시 액티비티 재생성 방지)
2. 방향 감지:
   - 방향 판별을 한 곳에서 제공 (예: `LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE` 래핑 또는 `BoxWithConstraints` 종횡비 기반)
   - 모든 레이아웃 분기가 이 단일 소스를 참조하도록 함
3. Safe Zone 확장 (`ui/common/LayoutConstants.kt`):
   - 가로 방향용 좌우 Safe Zone 상수 추가 (예: `LEFT_SAFE_ZONE`, `RIGHT_SAFE_ZONE`) — 기본값은 기존 상하단과 동일 톤으로 결정하고 기본값 주석 명시
   - 방향에 따라 적용할 Safe Zone을 선택하는 헬퍼 제공
4. 앱 설정 방향 3택:
   - Phase 4.14 `SettingsRepository`에 방향 설정 항목 추가 (`OrientationSetting` enum: `PORTRAIT` / `LANDSCAPE` / `AUTO`)
   - 세로/가로 고정은 `Activity.requestedOrientation`을 런타임에 강제, 자동은 `SENSOR`(또는 `UNSPECIFIED`)로 폰 자동회전 설정에 위임
   - 설정 변경 즉시 반영 + 재시작 후 유지

**수정 파일**:
- `src/android/app/src/main/AndroidManifest.xml`
- `src/android/app/src/main/java/com/bridgeone/app/ui/common/LayoutConstants.kt`
- `src/android/app/src/main/java/com/bridgeone/app/MainActivity.kt` (`requestedOrientation` 적용)
- `src/android/app/src/main/java/com/bridgeone/app/data/SettingsRepository.kt` (방향 설정 항목)
- 방향 설정 UI: Phase 4.14 설정 페이지에 항목 추가

**검증**:
- [ ] 에뮬레이터 회전 시 액티비티 재생성 없이 방향 전환 (로그로 `onCreate` 미호출 확인)
- [ ] 설정에서 세로/가로 고정 선택 시 즉시 해당 방향 고정
- [ ] 자동 선택 시 폰 자동회전 ON에서만 기울기 따라 회전, OFF면 현재 방향 유지
- [ ] 방향 설정 재시작 후 유지

---

## Phase 4.16.2: 최상위 레이아웃 방향 분기 (BridgeOneApp)

**개발 기간**: 0.5-1일

**세부 목표**:
1. `BridgeOneApp.kt` Active 박스 방향 분기:
   - 세로: 기존 `padding(top/bottom = SAFE_ZONE)` + `Alignment.BottomCenter` 유지
   - 가로: 좌우 Safe Zone 패딩 + 방향에 맞는 정렬로 분기
2. Splash / 연결 대기 화면 방향 점검:
   - 중앙 정렬 기반이면 대부분 방향 중립 — 깨지는 요소만 조정

**수정 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/BridgeOneApp.kt`

**검증**:
- [ ] 가로에서 Active 영역이 좌우 Safe Zone을 반영하여 잘림 없이 표시
- [ ] 세로 동작 회귀 없음
- [ ] Splash·연결 대기 화면 가로에서 정상 표시

---

## Phase 4.16.3: Essential 모드 가로 레이아웃

**개발 기간**: 2-3일

> 세로 레이아웃이 `Row` 2열(터치패드 72% / 키보드 28%) + `fillMaxHeight(0.75f)` 고정 비율이라 가로에서 키보드 영역이 과도하게 좁아진다. 방향 분기 재구성이 필요하다.

**세부 목표**:
1. 가로 레이아웃 분기:
   - 가로에서 터치패드/Boot Keyboard Cluster 배치를 방향에 맞게 재구성 (세로 비율을 그대로 쓰지 않음)
   - `fillMaxHeight(0.75f)` 등 세로 전제 고정 비율을 방향별 값으로 분기
2. 세로 레이아웃은 현행 유지 (회귀 방지)

**수정 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/pages/EssentialModePage.kt`

**검증**:
- [ ] 가로에서 터치패드/키보드 모두 사용 가능한 크기로 표시 (키보드 과소 영역 없음)
- [ ] 세로 레이아웃 회귀 없음
- [ ] 방향 전환 시 입력/홀드 상태 유지

---

## Phase 4.16.4: Page 1 (터치패드 + Actions) 가로 레이아웃

**개발 기간**: 1.5-2일

> 세로 레이아웃이 `Row` 2열(터치패드 64% / Actions 36%)이며 비율 분기가 `screenWidthDp` 기준이라 가로에서 재계산이 필요하다.

**세부 목표**:
1. 방향별 배치 분기:
   - 비율 분기 기준을 `screenWidthDp` 단독에서 방향 인식 기준으로 확장
   - 가로에서 터치패드·Actions 패널·(Phase 4.13 적용 시) ScrollPad가 잘리지 않도록 배치
2. `ActionsPanel`(`ui/pages/standard/components/`) 가로에서의 잔여 높이/스크롤 점검

**수정 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/pages/standard/Page1TouchpadActions.kt`
- `src/android/app/src/main/java/com/bridgeone/app/ui/pages/standard/components/ActionsPanel.kt` (필요 시)

**검증**:
- [ ] 가로에서 터치패드·Actions·ScrollPad 모두 표시 (오버플로우 없음)
- [ ] 세로 회귀 없음
- [ ] 페이지 전환·존 편집 진입 가로에서 정상

---

## Phase 4.16.5: Page 2~5 가로 레이아웃

**개발 기간**: 3-5일

> Page 2(멀티커서), Page 3(절대좌표), Page 4(키보드), Page 5(Minecraft)의 가로 레이아웃을 각각 추가한다. Phase 4.8~4.11에서 방향 전환을 고려한 구조로 구현됐다면(아래 대비 가이드) 작업량이 줄어든다.

**세부 목표** (페이지별):
1. Page 2 (풀 와이드 터치패드 / 멀티커서):
   - 풀 와이드는 비교적 방향 중립적 — 그리드 분할(N개 PadArea) 경계 계산만 가로 종횡비에서 검증
2. Page 3 (절대좌표 패드):
   - 16:9 매핑 영역의 letterbox/pillarbox가 가로에서 올바른지 검증
3. Page 4 (키보드 중심):
   - 좌 64% / 우 36% 2열을 가로에 맞게 분기
4. Page 5 (Minecraft):
   - 좌(터치패드+DPad) / 우(액션) 비율을 가로에 맞게 분기, DPad 정사각 유지

**수정 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/pages/standard/Page2MultiCursorTouchpad.kt`
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/AbsolutePointingPad.kt`
- `src/android/app/src/main/java/com/bridgeone/app/ui/pages/standard/Page3KeyboardCentric.kt`
- `src/android/app/src/main/java/com/bridgeone/app/ui/pages/standard/Page4Minecraft.kt`
- 각 페이지의 종속 컴포넌트 (필요 시)

**검증**:
- [ ] Page 2~5 각각 가로에서 레이아웃 잘림·오버플로우 없음
- [ ] 멀티커서 그리드 분할이 가로 종횡비에서 올바른 경계로 분할
- [ ] 절대좌표 매핑이 가로에서 정확
- [ ] 세로 회귀 없음

---

## Phase 4.16.6: 존 편집 / 엣지 스와이프 오버레이 가로 대응

**개발 기간**: 2-3일

> 존 편집(`EdgeZoneEditorScreen`)과 엣지 스와이프 오버레이는 전체 화면 캔버스 + 엣지 좌표 매핑을 사용한다. 비율은 상대값이라 일부 중립적이나, 엣지 스트립 두께 등 세로 전제 상수와 좌표 매핑을 방향에 맞게 조정한다.

**세부 목표**:
1. 엣지 스와이프 상수 방향 대응 (`ui/common/EdgeSwipeConstants.kt`):
   - `EDGE_STRIP_HEIGHT_DP` 등 세로 전제 치수를 방향별로 분기 또는 방향 무관 값으로 재정의 (기본값 주석 유지)
2. 존 캔버스 좌표 매핑:
   - 가로 종횡비에서 엣지(상/하/좌/우) 히트 영역·핸들 위치가 올바른지 검증 (`EdgeZoneCanvasGeometry` 순수 함수 단위 테스트 추가)
3. 존 편집 오버레이/패널 배치 가로 점검

**수정 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/common/EdgeSwipeConstants.kt`
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/touchpad/EdgeZoneEditorScreen.kt` (필요 시)
- `EdgeZoneCanvasGeometry` 및 관련 테스트

**검증**:
- [ ] 가로에서 4방향 엣지 존 히트·핸들 정상
- [ ] 존 편집 진입/조작 가로에서 정상
- [ ] 세로 회귀 없음

---

## Phase 4.16.7: 방향 전환 상태 유지 + 통합 검증

**개발 기간**: 1-2일

**세부 목표**:
1. 방향 전환 중 상태 유지 검증:
   - 자동 모드에서 회전 시 USB 연결·터치패드 상태·멀티커서 상태·존 설정이 유지되는지 (configChanges로 재생성 방지 전제)
2. 전체 회귀:
   - 모든 페이지/모드를 세로·가로 각각에서 순회하며 레이아웃·입력·햅틱 점검
3. 단위 테스트:
   - 방향 분기가 추가된 순수 로직(좌표 매핑 등)에 단위 테스트 보강 (4.7.2 컨벤션)

**수정 파일**:
- 회귀 중 발견된 문제 파일

**검증**:
- [ ] 자동 모드 회전 시 USB 연결 끊김 없음
- [ ] 회전 시 멀티커서/홀드/존 설정 상태 유지
- [ ] 세로·가로 전체 페이지 순회 시 레이아웃 깨짐 없음
- [ ] `.\gradlew test` 통과 (방향 분기 로직 단위 테스트 포함)
- [ ] 실기기에서 가로 자세 조작감 최종 확인
