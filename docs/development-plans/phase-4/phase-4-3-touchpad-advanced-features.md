---
title: "BridgeOne Phase 4.3: 터치패드 고급 기능"
description: "BridgeOne 프로젝트 Phase 4.3 - ControlButtonContainer, 스크롤 모드, 직각 이동, DPI 조절, 스크롤 가이드라인 구현"
tags: ["android", "touchpad", "scroll", "dpi", "control-buttons", "right-angle", "ui"]
version: "v1.0"
owner: "Chatterbones"
updated: "2026-03-26"
---

# BridgeOne Phase 4.3: 터치패드 고급 기능

**개발 기간**: 5-7일

**목표**: 터치패드의 고급 기능을 구현하여, 스크롤 모드, 직각 커서 이동, DPI 조절 등 스타일프레임에 정의된 전체 터치패드 기능을 완성합니다.

**핵심 성과물**:
- ControlButtonContainer 오버레이 (모드 제어 버튼 4개 + 옵션 버튼 2개)
- 일반 스크롤 모드 및 무한 스크롤 모드
- 직각 커서 이동 모드 (축 잠금 알고리즘)
- DPI 3단계 조절 시스템
- 스크롤 가이드라인 시각적 피드백

**선행 조건**: Phase 4.2 (Page 1 정식 구현) 완료

**에뮬레이터 호환성**: DEV 버튼으로 `Active(STANDARD)` 상태에서 터치패드 UI 전체 개발 가능. DPI 조절 실제 커서 속도 검증 및 스크롤 반응은 실기기에서 별도 수행.

---

## 현재 상태 분석

### 기존 구현
- `TouchpadWrapper.kt`: 기본 제스처 감지 (DOWN/MOVE/RELEASE), Historical 터치 샘플 처리
  - 클릭 판정: 500ms 미만 + 15dp 미만 이동 = LEFT_CLICK, 500ms 이상 = RIGHT_CLICK
  - `BridgeMode` 파라미터로 Essential/Standard 모드 구분
  - 1:2 비율 직사각형, 12dp 라운드 코너
- `DeltaCalculator.kt`: 상대 이동량 계산 (데드존 **5dp** 임계값)
  - `calculateAndCompensate()`: 델타 계산 → dp→px 변환 → 데드존 보상 → -127~127 정규화
  - `normalizeOnly()`: 데드존 탈출 후 범위 정규화만 수행
  - **⚠️ DPI 곱수 미지원** — Phase 4.3.5에서 추가 필요
  - **⚠️ 축 잠금(직각 이동) 미지원** — Phase 4.3.4에서 추가 필요
- `ClickDetector.kt`: 프레임 생성 및 전송
  - `createFrame()`: 마우스 이동/클릭 프레임
  - `createWheelFrame(wheelDelta)`: 휠 스크롤 프레임
  - `createRightClickFrame(pressed)`: 우클릭 프레임
  - `createKeyboardFrame()`: 키보드 프레임
  - `sendFrame()`: `UsbSerialManager` 통한 프레임 전송
- ControlButtonContainer 없음, 스크롤 모드 없음, DPI 고정, 가이드라인 없음

### 목표 구조 (component-touchpad.md 기준)
```
TouchpadWrapper
├── TouchpadAreaWrapper (터치 감지 영역)
│   └── Touchpad1Area (싱글 커서)
├── ControlButtonContainer (상단 15% 오버레이)
│   ├── 좌측: ClickMode | MoveMode | ScrollMode | CursorMode
│   └── 우측: DPI | ScrollSensitivity
└── ScrollGuideline (스크롤 시 시각 피드백)
```

---

## Phase 4.3.1: ControlButtonContainer 오버레이 구현

**목표**: 터치패드 상단 15%에 모드 제어 및 옵션 버튼 컨테이너 오버레이

**개발 기간**: 1-1.5일

**세부 목표**:
1. `ControlButtonContainer` Composable:
   - 위치: `TouchpadAreaWrapper` 상단에 오버레이
   - 크기: 터치패드 너비 × 높이의 15% (최소 48dp, 최대 72dp)
   - 여백: 터치패드 너비의 3% (최소 8dp, 최대 16dp)
2. 제어 버튼 공통 구조:
   - 크기: 터치패드 너비의 8% × 16% (최소 24dp×48dp)
   - 배경: 하단 둥근 모서리 직사각형
   - 텍스트: 버튼 높이의 30%, 중앙 정렬
   - 아이콘: 버튼 높이의 40%, 텍스트 하단
   - 터치 피드백: 햅틱 + 스케일 (1.0 → 0.95 → 1.0)
   - 히트 영역: 최소 48×48dp 보장
3. 좌측 모드 제어 버튼 (4개):
   - **ClickModeButton**: 좌클릭 ↔ 우클릭 전환
   - **MoveModeButton**: 자유 이동 ↔ 직각 이동 전환 (커서 이동 모드에서만 표시)
   - **ScrollModeButton**: 커서 이동 → 일반 스크롤 → 무한 스크롤 순환
   - **CursorModeButton**: 싱글 ↔ 멀티 커서 전환 (Phase 4+ 멀티 커서 미구현, Disabled)
4. 우측 옵션 버튼 (2개):
   - **DPIControlButton**: DPI 순환 (낮음 → 보통 → 높음)
   - **ScrollSensitivityButton**: 스크롤 감도 순환 (스크롤 모드에서만 표시)
5. 버튼 가시성 규칙:
   - 스크롤 모드 Selected 시: ClickModeButton, DPIControlButton 숨김
   - 스크롤 모드 Selected 시: ScrollSensitivityButton 표시
   - 커서 모드 시: MoveModeButton 표시
6. Essential 모드에서 숨김:
   - ControlButtonContainer 전체를 Essential 모드에서 비표시

**신규 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/touchpad/ControlButtonContainer.kt`
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/touchpad/TouchpadMode.kt`

**참조 문서**:
- `docs/android/component-touchpad.md` §1.3 (제어 버튼 컨테이너)
- `docs/android/component-touchpad.md` §1.4 (공통 구조)

> **⚠️ Phase 4.1.7 변경사항**: `TouchpadWrapper`는 `AppState.Active` 박스 내 `padding(top=40dp, bottom=40dp)` 영역 안에서 렌더링됨. `ControlButtonContainer` 위치(터치패드 상단 15% 오버레이)는 터치패드 자체 크기 기준이므로 safe zone과 별도 계산 불필요. 단, 화면 절대 좌표가 필요한 경우 유효 높이 = 전체 − 80dp. ControlButtonContainer 높이 비율 등 새 레이아웃 상수 추가 시 `ui/common/LayoutConstants.kt`에 함께 정의.

**검증**:
- [ ] 상단 15% 오버레이 정상 배치
- [ ] 6개 버튼 렌더링 및 터치 반응
- [ ] 버튼 가시성 규칙 (스크롤 모드 시 전환)
- [ ] Essential 모드에서 컨테이너 숨김
- [ ] 히트 영역 48×48dp 보장

---

## Phase 4.3.2: 일반 스크롤 모드

**목표**: 터치 드래그를 수직 스크롤 입력으로 변환하는 일반 스크롤 모드

**개발 기간**: 1일

**세부 목표**:
1. 스크롤 모드 상태 관리:
   - `TouchpadMode.SCROLL_NORMAL` 상태 추가
   - ScrollModeButton 탭으로 진입
   - 터치패드 영역 더블탭으로 스크롤 모드 종료 → 커서 이동 복귀
2. 스크롤 입력 변환:
   - 터치 드래그 Y축 → 휠 스크롤 프레임 전송
   - 정량적 스크롤: 일정 이동량당 1 스크롤 단위 전송
   - `ClickDetector.createWheelFrame()` 활용
3. 스크롤 감도 3단계:
   - 느림: 이동량 대비 스크롤 비율 ×0.5
   - 보통: 이동량 대비 스크롤 비율 ×1.0 (기본)
   - 빠름: 이동량 대비 스크롤 비율 ×2.0
   - ScrollSensitivityButton으로 순환
4. 모드 전환 시 입력 정리:
   - 커서 모드 → 스크롤 모드: 진행 중 드래그 중단
   - 스크롤 모드 → 커서 모드: 스크롤 관성 정지

**수정 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/TouchpadWrapper.kt`
- `TouchpadMode.kt`

**참조 문서**:
- `docs/android/component-touchpad.md` §1.3.1.3 (ScrollModeButton)
- `docs/android/technical-specification-app.md` §2.2 (터치패드 알고리즘)

**검증**:
- [ ] ScrollModeButton 탭으로 스크롤 모드 진입
- [ ] Y축 드래그 시 휠 프레임 전송
- [ ] 감도 3단계 전환 동작
- [ ] 더블탭으로 커서 이동 모드 복귀
- [ ] PC에서 실제 스크롤 동작 확인 (하드웨어 E2E)

---

## Phase 4.3.3: 무한 스크롤 모드 + 관성

**목표**: 관성 기반 무한 스크롤 모드 구현 (Standard 모드 전용)

**개발 기간**: 1-1.5일

**세부 목표**:
1. 무한 스크롤 상태:
   - `TouchpadMode.SCROLL_INFINITE` 상태 추가
   - 일반 스크롤 상태에서 ScrollModeButton 탭으로 전환
   - 무한 스크롤에서 ScrollModeButton 탭 → 일반 스크롤로 복귀
2. 관성 알고리즘:
   - 프레임-레이트 독립적 지수 감쇠 (exponential decay)
   - 손가락 떼는 순간의 속도를 초기 관성 속도로 사용
   - 감쇠 계수: 조절 가능 (기본값 TBD, 실측 후 확정)
   - 관성 중 터치 시 즉시 관성 정지
3. 햅틱 피드백:
   - 스크롤 단위마다 Light 햅틱
   - 관성 감속 시 점진적으로 햅틱 간격 증가
4. 가이드라인 색상 구분:
   - 일반 스크롤: `#84E268` (초록)
   - 무한 스크롤: `#F32121` (빨강)

**수정 파일**:
- `TouchpadWrapper.kt`
- `DeltaCalculator.kt` (관성 계산 추가)
- `TouchpadMode.kt`

**참조 문서**:
- `docs/android/component-touchpad.md` §1.3.1.3 (스크롤 모드 버튼)
- `docs/android/component-touchpad.md` §2.4 (스크롤 가이드라인 색상)

**검증**:
- [ ] 손가락 떼면 관성 스크롤 지속
- [ ] 관성 감쇠로 자연스러운 정지
- [ ] 관성 중 터치 시 즉시 정지
- [ ] 감도 설정 적용
- [ ] 빨간색 가이드라인 표시

---

## Phase 4.3.4: 직각 커서 이동 모드

**목표**: X축 또는 Y축으로만 커서를 이동하는 축 잠금 알고리즘

**개발 기간**: 0.5-1일

**세부 목표**:
1. 직각 이동 알고리즘:
   - 터치 시작 후 30dp 이동 시 주축(X/Y) 결정
   - 22.5도 데드밴드 (주축 전환 방지)
   - 주축이 결정되면 반대 축 이동량 = 0
   - 손가락 떼면 축 잠금 해제
2. MoveModeButton과 연동:
   - 자유 이동 ↔ 직각 이동 전환
   - 직각 이동 시 MoveModeButton에 "커서 자유 이동 모드" 텍스트 표시
3. 시각적 피드백:
   - 축 결정 시 터치패드 테두리 색상 힌트 (선택적)
   - 햅틱: 축 결정 순간 Light 1회

**수정 파일**:
- `DeltaCalculator.kt` (축 잠금 로직 추가)
- `TouchpadWrapper.kt`

**참조 문서**:
- `docs/android/technical-specification-app.md` §2.2.3 (직각 이동 알고리즘)
- `docs/android/component-touchpad.md` §1.3.1.2 (MoveModeButton)

**검증**:
- [ ] 30dp 이동 후 축 결정 정상 동작
- [ ] 직각 이동 중 반대 축 이동 차단
- [ ] 손가락 떼면 축 잠금 해제
- [ ] MoveModeButton 토글 동작
- [ ] PC에서 수평/수직 직선 커서 이동 확인

---

## Phase 4.3.5: DPI 조절 시스템

**목표**: 터치패드 커서 감도를 3단계로 조절하는 DPI 시스템

**개발 기간**: 0.5일

**세부 목표**:
1. DPI 3단계:
   - 낮음: 델타 곱수 ×0.5 (정밀 작업용)
   - 보통: 델타 곱수 ×1.0 (기본)
   - 높음: 델타 곱수 ×2.0 (빠른 이동용)
2. DPIControlButton:
   - 탭으로 순환: 낮음 → 보통 → 높음 → 낮음
   - 아이콘: `ic_slow.xml` / `ic_normal.xml` / `ic_fast.xml`
   - 텍스트: "DPI" + 현재 레벨
3. `DeltaCalculator` 연동:
   - DPI 곱수를 `calculateAndCompensate()` 또는 `applyDeadZone()` 결과에 곱하여 적용
   - **구현 위치**: `DeltaCalculator`에 `dpiMultiplier: Float = 1.0f` 파라미터 추가, 또는 `TouchpadWrapper`에서 결과에 곱수 적용
   - 기존 데드존(5dp), `normalizeOnly()` 범위 정규화(-127~127)와 호환 필요
   - **⚠️ 곱수 적용 후 -127~127 범위 초과 가능** → `coerceIn()` 재적용 필요
4. 상태 저장:
   - DPI 설정은 앱 종료 후에도 유지 (SharedPreferences)

**수정 파일**:
- `DeltaCalculator.kt` (DPI 곱수 적용)
- `ControlButtonContainer.kt`

**참조 문서**:
- `docs/android/component-touchpad.md` §1.3.2.1 (DPIControlButton)

**검증**:
- [ ] DPI 3단계 전환 동작
- [ ] 낮음에서 정밀한 커서 이동
- [ ] 높음에서 빠른 커서 이동
- [ ] 앱 재시작 후 DPI 설정 유지

---

## Phase 4.3.6: 스크롤 가이드라인 시각적 피드백

**목표**: 스크롤 방향을 시각적으로 표시하는 가이드라인 UI

**개발 기간**: 0.5일

**세부 목표**:
1. `ScrollGuideline` Composable:
   - 위치: 터치패드 내부 (테두리 제외)
   - 표시 조건: 스크롤 모드에서 드래그 시작 시
   - 숨김 조건: 스크롤 정지 판정 후 일정 시간 경과, 또는 모드 종료 시 즉시
2. 색상:
   - 일반 스크롤: `#84E268` (초록)
   - 무한 스크롤: `#F32121` (빨강)
3. 방향 표시:
   - 수직 스크롤 방향에 따른 화살표 또는 라인 표시
   - 스크롤 속도에 비례하는 시각적 강도

**신규 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/touchpad/ScrollGuideline.kt`

**참조 문서**:
- `docs/android/component-touchpad.md` §1.5 (스크롤 가이드라인)
- `docs/android/component-touchpad.md` §2.4 (색상)

**검증**:
- [ ] 스크롤 모드 드래그 시 가이드라인 표시
- [ ] 일반/무한 모드에 따른 색상 구분
- [ ] 스크롤 정지 시 자연스러운 숨김
- [ ] 모드 종료 시 즉시 숨김

---

## Phase 4.3 완료 후 터치패드 기능 매트릭스

| 기능 | Essential | Standard |
|------|----------|----------|
| 커서 이동 (자유) | ✅ | ✅ |
| 좌클릭 (탭) | ✅ | ✅ |
| 우클릭 | ❌ (좌클릭으로 강제) | ✅ (ClickModeButton) |
| 직각 이동 | ❌ | ✅ (MoveModeButton) |
| 일반 스크롤 | ❌ | ✅ (ScrollModeButton) |
| 무한 스크롤 | ❌ | ✅ (ScrollModeButton) |
| DPI 조절 | ❌ | ✅ (DPIControlButton) |
| 스크롤 감도 | ❌ | ✅ (ScrollSensitivityButton) |
| 멀티 커서 | ❌ | ⏳ Phase 4+ |
| ControlButtonContainer | ❌ 숨김 | ✅ 표시 |
| 스크롤 가이드라인 | ❌ | ✅ (스크롤 모드 시) |
