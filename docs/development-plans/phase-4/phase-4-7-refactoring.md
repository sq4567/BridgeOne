---
title: "BridgeOne Phase 4.7: Android 코드베이스 리팩토링"
description: "BridgeOne 프로젝트 Phase 4.7 - Phase 4.1~4.6 산출물의 외부 동작을 유지한 채 내부 구조 개선 (상수 정리·중복 제거·거대 파일 분해·MVVM ViewModel 도입)"
tags: ["android", "refactoring", "architecture", "mvvm", "code-quality"]
version: "v1.0"
owner: "Chatterbones"
updated: "2026-06-16"
---

# BridgeOne Phase 4.7: Android 코드베이스 리팩토링

**개발 기간**: 미정

**목표**: Phase 4.1~4.6에서 리팩토링 없이 누적 개발된 Android 코드의 외부 동작을 100% 유지한 채 내부 구조를 개선합니다. 상수 정리, 중복 제거, 거대 파일 분해, MVVM ViewModel 도입을 통해 코드 품질과 유지보수성을 높입니다.

**핵심 성과물**:
- 기능별로 분리된 `*Constants.kt` 파일군
- 중복 제거용 공통 유틸 (`HapticFeedbackHelper`, `PopupScaffold`)
- `TouchpadWrapper` / `StandardModePage` / `EdgeZoneEditorScreen` / `DynamicsCurveEditor` 분해
- 화면별 `ViewModel` (`TouchpadViewModel`, `StandardModePageViewModel`, `EdgeZoneEditorViewModel`)

**선행 조건**: Phase 4.6 완료

**불변 조건 (핵심)**: 모든 하위 Phase는 사용자가 체감하는 동작을 바꾸지 않습니다. 이것이 기능 변경과 리팩토링을 가르는 기준입니다. 각 하위 Phase 완료 시 `.\gradlew assembleDebug` 빌드 통과 + 해당 기능 수동 회귀 확인을 마친 뒤 다음 단계로 진행합니다.

**진행 순서**: 저위험(상수·중복) → 고위험(구조·ViewModel) 순서로 진행합니다. 각 하위 Phase는 독립적으로 빌드·검증 가능한 단위이므로, 중간에 중단해도 코드가 깨지지 않습니다.

**Phase 4.15(성능 최적화)와의 관계**: 본 Phase는 **구조 개선**이 목적이며, 성능 향상은 부수적 결과입니다. 측정 기반의 성능 최적화는 모든 기능 완성 후 `phase-4-15-performance.md`에서 별도로 진행합니다. 일부 대상 코드가 겹치므로(예: 햅틱 호출, 제스처 루프), 본 Phase에서 만든 구조 위에서 4.15가 동작하게 됩니다.

---

## 현재 상태 분석

### 리팩토링 대상 (Phase 4.1~4.6 산출물)

| 파일 | 규모 | 주요 문제 |
|-----|------|---------|
| `ui/components/touchpad/EdgeZoneEditorScreen.kt` | 비대 | 다수 Composable + 다수 상태 변수가 한 함수에 집중, 편집/팝업/라벨/프리셋/매크로 책임 혼재 |
| `ui/components/touchpad/DynamicsCurveEditor.kt` | 비대 | 캔버스 렌더링 + 노드 드래그 제스처 + 템플릿/요약 생성 혼재 |
| `ui/components/TouchpadWrapper.kt` | 비대 | UI 렌더링 + 제스처 + 상태 계산(스크롤/관성/가속도) + 통신 + 햅틱 단일 Composable 집중 |
| `ui/pages/StandardModePage.kt` | 비대 | 다수 Composable 한 파일 정의, 페이지 간 공유 상태 집중 |
| `ui/common/ScrollConstants.kt` | 혼재 | 스크롤 상수와 엣지 스와이프 상수가 한 파일에 공존 |

### 중복 패턴

- 햅틱 진동 호출 블록이 `TouchpadWrapper.kt` 등 여러 위치에 반복
- 4개 Popup(`DynamicsPresetPopup` / `ModePresetPopup` / `EdgeZonePresetPopup` / `DpiAdjustPopup`)의 열기·닫기·dismiss 보일러플레이트 반복
- 흩어진 magic number (프레임 딜레이, 햅틱 amplitude 상한 등)

### 목표 아키텍처

BridgeOne 기존 아키텍처(MVVM + Clean Architecture)에 맞춰, 화면 단위로 상태·로직을 `ViewModel`로 이관하고 Composable은 렌더링과 이벤트 위임만 담당하는 단방향 데이터 흐름(UDF)으로 재구성합니다.

```
[Composable] --user event--> [ViewModel] --state(StateFlow)--> [Composable]
   (렌더링 전용)                (상태·비즈니스 로직)
```

---

## Phase 4.7.1: 상수 정리 및 분리

**목표**: 흩어지거나 혼재된 상수를 기능별 `*Constants.kt`로 정리합니다.

**작업 항목**:
1. `ScrollConstants.kt`에서 엣지 스와이프 관련 상수를 `EdgeSwipeConstants.kt`(신규)로 분리
2. 코드에 하드코딩된 magic number를 적절한 `*Constants.kt`로 이동 (상수 기본값 주석 정책 준수)
3. `PointerDynamicsConstants.kt`에 혼재된 곡선 템플릿 데이터를 별도 파일로 분리할지 검토 후 적용

**신규 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/common/EdgeSwipeConstants.kt`

**수정 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/common/ScrollConstants.kt`
- `src/android/app/src/main/java/com/bridgeone/app/ui/common/PointerDynamicsConstants.kt`
- 분리된 상수를 참조하던 호출부 (import 경로 갱신)

**검증**:
- [ ] `.\gradlew assembleDebug` 빌드 통과
- [ ] 분리 전후 상수 값이 동일 (의도치 않은 값 변경 없음)
- [ ] 스크롤·엣지 스와이프 동작 회귀 없음
- [ ] 추가/이동한 상수에 기본값 주석 존재

---

## Phase 4.7.2: 공통 유틸 추출 (중복 제거)

**목표**: 반복되는 햅틱 호출과 Popup 보일러플레이트를 공통 단위로 통합합니다.

**작업 항목**:
1. 반복되는 햅틱 진동 호출을 `HapticFeedbackHelper`로 단일화 (호출부는 헬퍼를 통해 진동)
2. 4개 Popup의 공통 열기·닫기·dismiss 구조를 `PopupScaffold`로 추출, 각 Popup이 이를 사용하도록 변경

**신규 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/common/HapticFeedbackHelper.kt`
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/common/PopupScaffold.kt` (위치는 구현 시 확정)

**수정 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/TouchpadWrapper.kt` (햅틱 호출부)
- `DynamicsPresetPopup.kt` / `ModePresetPopup.kt` / `EdgeZonePresetPopup.kt` / `DpiAdjustPopup.kt`

> **⚠️ Phase 4.15.2 영향**: 4.15.2(햅틱 호출 빈도 최적화)는 본 Phase에서 만든 `HapticFeedbackHelper`에 시간 게이트(`HAPTIC_MIN_INTERVAL_MS`)를 추가하는 방식으로 진행하면 됩니다. 호출부가 헬퍼로 단일화되어 있으므로 한 곳만 수정하면 전체 적용됩니다.

**검증**:
- [ ] `.\gradlew assembleDebug` 빌드 통과
- [ ] 무한 스크롤·관성·경계 피드백 등 모든 햅틱 체감 동일
- [ ] 4개 Popup의 열기·선택·취소 동작 동일

---

## Phase 4.7.3: TouchpadWrapper 분해 + TouchpadViewModel 도입

**목표**: `TouchpadWrapper.kt`의 상태·로직을 ViewModel로 이관하고 Composable은 렌더링 전용으로 축소합니다.

**작업 항목**:
1. 제스처 처리 로직을 별도 핸들러로 추출
2. 스크롤 축 확정·관성·가속도 등 상태 계산과 다수 상태 변수를 `TouchpadViewModel`로 이관, UDF 적용
3. Composable에는 UI 렌더링과 이벤트 위임만 잔류

**신규 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/TouchpadViewModel.kt`
- 제스처 핸들러 파일 (이름·위치는 구현 시 확정)

**수정 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/TouchpadWrapper.kt`
- `TouchpadWrapper`를 호출하는 페이지 (ViewModel 연결)

> **⚠️ Phase 4.15.3 / 4.15.4 영향**: 가이드라인 리컴포지션 최소화(4.15.3)와 제스처 루프 작업 제거(4.15.4)는 본 Phase에서 분리된 ViewModel·핸들러 구조 위에서 진행됩니다. 대상 파일 위치가 달라질 수 있으므로 4.15 진행 시 본 Phase 결과를 먼저 확인할 것.

**검증**:
- [ ] `.\gradlew assembleDebug` 빌드 통과
- [ ] 커서 이동·클릭·드래그 동작 동일
- [ ] 스크롤(일반/무한)·관성·축 확정·직각 이동 동작 동일
- [ ] 엣지 스와이프 메뉴·가이드라인 표시 동일
- [ ] 햅틱 피드백 타이밍 동일

---

## Phase 4.7.4: StandardModePage 분해 + StandardModePageViewModel 도입

**목표**: 한 파일에 정의된 다수 Composable을 페이지별로 분리하고 공유 상태를 ViewModel로 이관합니다.

**작업 항목**:
1. 한 파일 내 정의된 페이지/팝업 Composable을 페이지 단위 파일로 분리
2. 페이지 간 공유 상태와 모드 전환 로직을 `StandardModePageViewModel`로 이관

**신규 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/pages/StandardModePageViewModel.kt`
- 페이지별 분리 파일 (이름·위치는 구현 시 확정)

**수정 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/pages/StandardModePage.kt`

**검증**:
- [ ] `.\gradlew assembleDebug` 빌드 통과
- [ ] 5페이지 스와이프 전환 동작 동일
- [ ] 페이지 간 상태 공유·동기화 동일
- [ ] DPI·매크로·곡선 편집 등 팝업 호출 동일

---

## Phase 4.7.5: EdgeZoneEditorScreen 분해 + EdgeZoneEditorViewModel 도입

**목표** (최대 규모): 다수 Composable과 상태 변수가 집중된 `EdgeZoneEditorScreen.kt`를 상태 홀더(ViewModel) + 기능별 하위 Composable로 분할합니다.

**작업 항목**:
1. 존 편집·액션 할당·UndoStack 등 로직을 `EdgeZoneEditorViewModel`로 이관
2. 캔버스·팝업·라벨 편집·프리셋 등 UI를 기능별 하위 Composable로 분리 (예: `ZoneCanvas`, `ZonePopups`, `ZoneLabelEditor`, `ZonePresetPopups` — 명칭은 구현 시 확정)
3. 다수의 `remember`/`LaunchedEffect`/`DisposableEffect`를 책임 단위로 재배치

**신규 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/touchpad/EdgeZoneEditorViewModel.kt`
- 기능별 하위 Composable 파일들 (구현 시 확정)

**수정 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/touchpad/EdgeZoneEditorScreen.kt`

**검증**:
- [ ] `.\gradlew assembleDebug` 빌드 통과
- [ ] 4개 엣지 존 시각화·드래그 편집 동작 동일
- [ ] 존 분할/병합, 액션·아이콘·컬러·숏컷 할당 동작 동일
- [ ] 회전 트리거, 라벨 편집(IME), 프리셋 저장/로드 동작 동일
- [ ] Undo/Redo 동작 동일

---

## Phase 4.7.6: DynamicsCurveEditor 분해

**목표**: 곡선 편집기를 렌더링·제스처·데이터 생성 단위로 분리합니다.

**작업 항목**:
1. 곡선 캔버스 렌더링과 노드 드래그 제스처를 분리
2. 템플릿 선택·곡선 요약(자연어 설명) 생성 로직을 별도 단위로 추출

**신규 파일**:
- 분리된 곡선 캔버스/노드 에디터 파일 (구현 시 확정)

**수정 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/touchpad/DynamicsCurveEditor.kt`

**검증**:
- [ ] `.\gradlew assembleDebug` 빌드 통과
- [ ] 곡선 노드 추가·삭제·드래그 동작 동일
- [ ] 템플릿 선택·스텝 정밀도·요약 텍스트 동일
- [ ] 곡선 저장(이름/설명) 동작 동일

---

## Phase 4.7.7: 빌드·회귀 검증 종합

**목표**: 전체 리팩토링 결과를 종합 검증합니다.

**작업 항목**:
1. 전체 `.\gradlew assembleDebug` 통과 확인
2. 빌드 경고 점검 및 정리 (가능 범위)
3. Phase 4.1~4.6 주요 기능 수동 회귀 체크리스트 일괄 점검

**검증**:
- [ ] 전체 빌드 통과, 신규 경고 없음
- [ ] 스플래시·연결 대기 (Phase 4.1) 동작 동일
- [ ] Page 1 터치패드·Actions (Phase 4.2~4.3) 동작 동일
- [ ] 터치패드 다이나믹스·제스처 (Phase 4.4) 동작 동일
- [ ] 엣지 스와이프 대안 조작 (Phase 4.6) 동작 동일
- [ ] 리팩토링 전후 사용자 체감 동작 차이 없음

---

## Phase 4.7.8: 리팩토링 후 동작 검증

> **선행 조건**: Phase 4.7.7 빌드·회귀 통과 후 진행. 4.7.7이 빌드와 개략 회귀를 다룬다면, 본 Phase는 리팩토링 전 버전과의 동작 동일성을 실기기에서 시나리오별로 입증합니다.

**목표**: 리팩토링이 사용자 체감 동작을 바꾸지 않았음을 실기기 E2E로 확인합니다. 본 Phase의 불변 조건(외부 동작 100% 유지)을 최종 입증하는 단계입니다.

**작업 항목**:
1. 리팩토링 착수 전 빌드(기준 버전)의 핵심 시나리오 동작·타이밍·피드백을 기준값으로 확보
2. 핵심 입력 시나리오를 실기기에서 재현하여 기준 버전과 비교
   - 커서 이동/클릭/드래그
   - 스크롤(일반/무한)/관성/축 확정
   - 엣지 스와이프 메뉴 및 대안 조작
   - 멀티 패드/존 편집/프리셋
   - 곡선 편집/다이나믹스 적용
3. 입력 정확성·지연·햅틱 타이밍에 회귀가 없는지 확인
4. 차이 발견 시 해당 하위 Phase(4.7.1~4.7.6)로 돌아가 수정 후 재검증

**에뮬레이터 호환성**: 입력 정확성·지연·하드웨어 피드백 검증은 ESP32-S3 동글 + PC 연결 실기기 필요. 에뮬레이터는 UI 흐름 확인까지만.

**검증**:
- [ ] ESP32-S3 동글 + PC 연결 실기기 E2E 정상 동작
- [ ] 커서 이동·클릭·드래그 정확성 기준 버전과 동일
- [ ] 스크롤(일반/무한)·관성 동작 및 감도 동일
- [ ] 엣지 스와이프 메뉴·존 편집·프리셋 동작 동일
- [ ] 곡선 편집·다이나믹스 적용 결과 동일
- [ ] 햅틱·토스트 등 피드백 타이밍 동일
- [ ] 리팩토링 전후 입력 지연 체감 차이 없음
