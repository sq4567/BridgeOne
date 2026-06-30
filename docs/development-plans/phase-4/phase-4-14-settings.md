---
title: "BridgeOne Phase 4.14: 환경 설정"
description: "BridgeOne 프로젝트 Phase 4.14 - 앱 내 설정 UI 구현, 접근 편의 모드(Assisted Mode), DataStore 기반 설정 영속화"
tags: ["android", "settings", "accessibility", "datastore", "preferences", "assisted-mode"]
version: "v1.0"
owner: "Chatterbones"
updated: "2026-04-05"
---

# BridgeOne Phase 4.14: 환경 설정

**개발 기간**: 3-5일

**목표**: 사용자가 앱 내에서 터치패드, 스크롤, 레이아웃 등 다양한 동작 파라미터를 직접 조정할 수 있는 환경 설정 페이지를 구현합니다. 손이 불편한 사용자도 불편 없이 세밀한 조정을 할 수 있도록 **접근 편의 모드(Assisted Mode)**를 함께 제공합니다.

**핵심 성과물**:
- 설정 페이지 UI (카테고리별 설정 목록)
- 접근 편의 모드 (한 항목씩 화면 중앙에 크게 표시, 슬라이더 조작)
- DataStore 기반 설정값 영속화
- 설정 변경 시 실시간 반영

**선행 조건**: Phase 4.11 완료 (모든 페이지 UI 구현 완료 상태)

**에뮬레이터 호환성**: 전체 에뮬레이터에서 개발 가능.

---

## Phase 4.14.1: DataStore 인프라 및 SettingsRepository

**개발 기간**: 0.5-1일

**목표**: 설정값을 앱 재시작 후에도 유지하기 위한 Jetpack DataStore 기반 저장 계층을 구축합니다. 현재 하드코딩된 상수값들을 런타임에 변경 가능하도록 StateFlow로 노출합니다.

**구현 항목**:
1. `build.gradle`에 `androidx.datastore:datastore-preferences` 의존성 추가
2. `SettingsRepository.kt` 신규 생성 (`data/` 패키지)
   - Preferences DataStore 인스턴스 생성
   - 설정 카테고리별 키 정의 (아래 설정 항목 참조)
   - 각 설정값을 `StateFlow`로 노출
   - 기본값은 현재 상수 파일의 값을 그대로 사용
3. 기존 `SharedPreferences` 사용부 → DataStore로 마이그레이션

   > **⚠️ Phase 4.7.4-A / 4.7.1 변경사항**: DPI prefs 헬퍼(`loadDpiLevel`/`saveDpiLevel`)는 `StandardModePage.kt`가 아니라 `ui/pages/standard/StandardModePrefs.kt`에 있다. prefs가 여러 파일로 분산됐으므로 마이그레이션 대상은 다음을 포함: `ui/pages/standard/StandardModePrefs.kt`(DPI 등), `ui/common/InputMode.kt`(input mode·zone move method), `ui/common/ShortcutEditorPrefs.kt`, `ui/common/AudioFeedbackPrefs.kt`. 또한 설정 항목의 **기본값 출처 상수도 4.7.1에서 여러 파일로 분산**됐다 — 포인터 가속/데드존 → `PointerDynamicsConstants.kt`, 스크롤 → `ScrollConstants.kt`, Safe Zone → `LayoutConstants.kt`, 엣지 스와이프 → `EdgeSwipeConstants.kt`. 항목별 기본값 출처를 정확히 매핑할 것.

4. 싱글톤 제공: `Application` 클래스 또는 수동 싱글톤 (Phase 4.7이 ViewModel/Hilt를 채택하지 않았으므로 DI 라이브러리 없이 수동 싱글톤 권장)

**설정 항목 목록** (Phase 4.14.2에서 UI로 노출):

| 카테고리 | 설정 항목 | 타입 | 기본값 | 범위/옵션 | 설명 |
|----------|----------|------|--------|----------|------|
| **터치패드** | 포인터 가속 프리셋 | Enum | Off | Off/Precision/Standard/Fast | 커서 가속 곡선 |
| **터치패드** | 클릭 판정 시간 | Long | 500ms | 200-1000ms | 이 시간 이내 탭 = 좌클릭 |
| **터치패드** | 클릭 판정 거리 | Float | 15dp | 5-30dp | 이 거리 이내 이동 = 클릭 |
| **터치패드** | 데드존 임계값 | Float | 5dp | 1-15dp | 이 거리 이상 이동해야 드래그 |
| **스크롤** | 스크롤 감도 | Enum | Normal | Slow/Normal/Fast | 스크롤 속도 배율 |
| **스크롤** | 스크롤 단위 거리 | Float | 20dp | 10-40dp | 1단위 스크롤에 필요한 손가락 이동량 |
| **스크롤** | 무한 스크롤 관성 시간 | Float | 800ms | 200-3000ms | 관성 감속 시간 상수 |
| **스크롤** | 무한 스크롤 최소 속도 | Float | 0.08dp/ms | 0.02-0.3dp/ms | 이 미만이면 관성 종료 |
| **스크롤** | 방향별 속도 보정 (하) | Float | 2.0x | 0.5-4.0x | 아래 스크롤 속도 배율 |
| **스크롤** | 방향별 속도 보정 (상) | Float | 1.0x | 0.5-4.0x | 위 스크롤 속도 배율 |
| **레이아웃** | 상단 Safe Zone | Float | 40dp | 0-80dp | 상단 여백 크기 |
| **레이아웃** | 하단 Safe Zone | Float | 40dp | 0-80dp | 하단 여백 크기 |
| **피드백** | 햅틱 진동 | Boolean | On | On/Off | 전체 햅틱 피드백 토글 |
| **시스템** | 접근 편의 모드 기본 사용 | Boolean | Off | On/Off | 설정 진입 시 항상 접근 편의 모드로 열기 |

**신규 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/data/SettingsRepository.kt`

**검증**:
- [ ] 빌드 성공
- [ ] 설정값 저장 후 앱 재시작 시 유지 확인
- [ ] 기존 SharedPreferences DPI 값이 DataStore로 정상 마이그레이션

---

## Phase 4.14.2: 설정 페이지 UI (일반 모드)

**개발 기간**: 1-1.5일

**목표**: 카테고리별 설정 항목을 스크롤 목록으로 표시하는 일반 설정 페이지를 구현합니다. Android 기본 설정 패턴을 따르되, 모든 조작을 슬라이더와 토글로 수행할 수 있도록 합니다 (텍스트 직접 입력 불필요).

> **⚠️ Phase 4.7.8-D 재사용 기회**: Enum 설정 항목(포인터 가속 프리셋, 스크롤 감도 등)을 세그먼트 선택 UI로 표현할 경우, 새로 만들지 말고 `ui/pages/standard/SettingsSegmentedChip.kt`의 `SegmentedChipSelector<T>`(제네릭, 파일명과 함수명이 다름에 주의)를 재사용할 것 — 존 페이지/이동 방식/TTS 성별 선택에서 이미 쓰이는 공통 컴포넌트.

**UI 구조**:
```
┌──────────────────────────────────┐
│  ← 설정                    [접근 편의 모드 전환]  │
├──────────────────────────────────┤
│                                  │
│  ── 터치패드 ────────────────     │
│  포인터 가속     [Off ▾]         │
│  클릭 판정 시간  ───●──── 500ms  │
│  클릭 판정 거리  ────●─── 15dp   │
│  데드존          ──●───── 5dp    │
│                                  │
│  ── 스크롤 ──────────────────     │
│  감도            [Normal ▾]      │
│  스크롤 단위     ────●─── 20dp   │
│  관성 시간       ───●──── 800ms  │
│  ...                             │
│                                  │
│  ── 레이아웃 ────────────────     │
│  상단 여백       ────●─── 40dp   │
│  하단 여백       ────●─── 40dp   │
│                                  │
│  ── 피드백 ──────────────────     │
│  햅틱 진동       [ON]            │
│                                  │
│        [기본값으로 초기화]        │
└──────────────────────────────────┘
```

**구현 항목**:
1. `SettingsPage.kt` 신규 생성 (`ui/pages/`)
   - `LazyColumn` 기반 카테고리별 설정 목록
   - 각 설정 항목에 맞는 입력 컴포넌트:
     - **슬라이더**: 연속 값 (dp, ms 등) — 큰 터치 타겟 (최소 높이 56dp)
     - **드롭다운/세그먼트**: 열거형 프리셋 선택
     - **토글 스위치**: Boolean 값
   - 현재 값 표시: 슬라이더 옆에 숫자 레이블
   - 카테고리 헤더: 시각적 구분선 + 카테고리명
2. 설정 페이지 진입 경로:
   - Edge Swipe 제스처 또는 전용 버튼 (기존 UI 구조에 맞게 결정)
   - 설정 페이지에서 뒤로가기 → 이전 페이지로 복귀
3. "기본값으로 초기화" 버튼: 모든 설정을 기본값으로 리셋 (확인 다이얼로그 포함)
4. 설정 변경 시 `SettingsRepository`에 즉시 저장 + StateFlow를 통해 실시간 반영

**설계 원칙**:
- 모든 값을 슬라이더/토글/선택으로 조작 — 키보드 텍스트 입력 일체 불필요
- 슬라이더 터치 타겟: 높이 56dp 이상 (접근성 최소 기준 48dp 초과)
- 변경 즉시 반영 — 별도 "저장" 버튼 없음

**신규 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/pages/SettingsPage.kt`

**검증**:
- [ ] 모든 설정 항목이 카테고리별로 표시
- [ ] 슬라이더 조작으로 값 변경 가능
- [ ] 변경값이 즉시 저장되고 동작에 반영
- [ ] 기본값 초기화 동작

---

## Phase 4.14.3: 접근 편의 모드 (Assisted Mode)

**개발 기간**: 1-1.5일

**목표**: 손이 불편한 사용자가 화면 전체에 분산된 설정 항목을 일일이 찾아 조작하지 않아도 되도록, **한 번에 하나의 설정 항목을 화면 중앙에 크게 표시**하고 이전/다음으로 탐색하는 모드를 구현합니다.

**접근 편의 모드가 해결하는 문제**:
- 일반 설정 목록에서 특정 항목까지 스크롤하기 어려움 (손가락 정밀 조작 부담)
- 슬라이더가 화면 여러 위치에 분산되어 손이 닿기 힘든 영역 발생
- 작은 드롭다운/토글을 정확히 탭하기 어려움

**UI 구조**:
```
┌──────────────────────────────────┐
│  ← 설정 (접근 편의)     [일반 모드 전환]  │
├──────────────────────────────────┤
│                                  │
│         카테고리: 터치패드         │
│                                  │
│     ┌────────────────────┐       │
│     │                    │       │
│     │   클릭 판정 시간    │       │
│     │                    │       │
│     │   ════●════════    │       │
│     │      500ms         │       │
│     │                    │       │
│     │  (200ms ← → 1000ms)│      │
│     │                    │       │
│     └────────────────────┘       │
│                                  │
│   ┌────────┐      ┌────────┐    │
│   │  ◀ 이전 │      │ 다음 ▶  │    │
│   └────────┘      └────────┘    │
│                                  │
│          3 / 14                  │
└──────────────────────────────────┘
```

**구현 항목**:
1. `AssistedSettingsMode.kt` 신규 생성 (`ui/pages/`)
   - 현재 설정 항목 인덱스 관리 (`currentIndex`)
   - 화면 중앙에 현재 항목의 설정 카드 크게 표시
     - 카테고리명, 항목명, 설명
     - 항목 타입에 맞는 입력 컴포넌트 (슬라이더/선택/토글)
     - 슬라이더: 전체 화면 너비의 80% 이상 차지 (조작 정밀도 향상)
     - 토글/선택: 최소 64dp 높이 버튼
   - 하단에 "이전" / "다음" 네비게이션 버튼
     - 큰 터치 타겟 (최소 64×48dp)
     - 화면 하단 고정 — 손이 자연스럽게 닿는 위치
   - 진행 표시: "3 / 14" 형태로 현재 위치 표시
2. 모드 전환:
   - 일반 모드 ↔ 접근 편의 모드 전환 버튼 (설정 페이지 상단)
   - 전환 시 현재 보고 있던 항목 위치 유지
3. 카테고리 건너뛰기:
   - 이전/다음 버튼 롱프레스 → 다음 카테고리 첫 항목으로 점프
   - 카테고리 전환 시 햅틱 피드백 (짧은 진동)
4. 설정 항목 필터링 (선택적):
   - 카테고리별 필터 — 특정 카테고리만 탐색 가능
   - 전체 항목을 순서대로 탐색하는 것이 기본

**접근성 고려사항**:
- 모든 조작이 화면 하단 절반에서 완결 (손이 닿는 범위)
- 슬라이더 드래그 외에도 양쪽 "+/-" 버튼으로 값 미세 조정 가능
- 값 변경 시 햅틱 피드백 (단위 경계마다)
- 큰 폰트 크기 (항목명 20sp, 현재 값 24sp)

**신규 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/pages/AssistedSettingsMode.kt`

**검증**:
- [ ] 한 번에 하나의 항목만 화면 중앙에 표시
- [ ] 이전/다음 버튼으로 모든 항목 탐색 가능
- [ ] 롱프레스로 카테고리 건너뛰기
- [ ] 일반 모드 ↔ 접근 편의 모드 전환 시 위치 유지
- [ ] 슬라이더 "+/-" 버튼으로 미세 조정 가능

---

## Phase 4.14.4: 설정-동작 바인딩 및 통합

**개발 기간**: 0.5-1일

**목표**: `SettingsRepository`의 StateFlow를 각 컴포넌트에 연결하여, 설정 변경이 터치패드/스크롤/레이아웃 등 실제 동작에 실시간으로 반영되도록 합니다.

> **⚠️ Phase 4.7.3-B 변경사항 (햅틱 이중 경로)**: 햅틱 경로가 둘로 분리됐다 — (1) 이산 햅틱: `HidConstants.triggerHaptic()` 및 `view.performHapticFeedback(...)`, (2) 속도 기반 스크롤/관성 햅틱: `ui/common/HapticFeedbackHelper.vibrateByVelocity()`. 햅틱 토글로 "모든 진동 정지"(아래 검증 항목)를 달성하려면 **두 경로 모두** 게이트해야 한다 — `HapticFeedbackHelper`에도 enable 플래그를 주입할 것. Phase 4.16.2가 같은 헬퍼에 시간 게이트를 추가하므로, enable 플래그와 시간 게이트를 한 지점에서 함께 처리하면 충돌이 없다.

**구현 항목**:
1. **터치패드 바인딩**:
   - `DeltaCalculator` → 포인터 가속 프리셋, 데드존 임계값
   - `ClickDetector` → 클릭 판정 시간, 클릭 판정 거리
2. **스크롤 바인딩**:
   - `ScrollHandler`/`TouchpadWrapper` → 스크롤 감도, 단위 거리, 관성 파라미터
   - `ScrollDirectionBoost` → 방향별 속도 보정 값
3. **레이아웃 바인딩**:
   - `BridgeOneApp.kt` → Safe Zone 값 (`LayoutConstants` 대신 StateFlow 참조)
4. **피드백 바인딩** (두 햅틱 경로 모두 게이트):
   - 이산 햅틱: `HidConstants.triggerHaptic()` + `view.performHapticFeedback(...)` → 햅틱 토글에 따라 on/off
   - 속도 햅틱: `HapticFeedbackHelper.vibrateByVelocity()` → 동일 토글로 게이트 (enable 플래그 주입)
5. 바인딩 방식:
   - 각 컴포넌트에서 `SettingsRepository.xxxFlow.collectAsState()`로 현재값 수집
   - 하드코딩된 상수 참조를 StateFlow 값으로 교체
   - 기본값은 기존 상수와 동일하게 유지 (DataStore에 값이 없을 때)

**수정 파일**:
- `DeltaCalculator.kt`, `ClickDetector.kt`, `TouchpadWrapper.kt`
- `ScrollHandler` 관련 코드
- `BridgeOneApp.kt` (Safe Zone)
- `HidConstants.kt` (이산 햅틱 토글)
- `ui/common/HapticFeedbackHelper.kt` (속도 햅틱 enable 플래그)

**검증**:
- [ ] 설정에서 클릭 판정 시간 변경 → 즉시 반영 확인
- [ ] 스크롤 감도 변경 → 스크롤 속도 변화 확인
- [ ] Safe Zone 변경 → 레이아웃 즉시 업데이트
- [ ] 햅틱 끄기 → 모든 진동 정지 확인
- [ ] 앱 재시작 후 변경된 설정값 유지 확인
