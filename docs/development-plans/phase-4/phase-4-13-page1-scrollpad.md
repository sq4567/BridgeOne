---
title: "BridgeOne Phase 4.13: ScrollPad 컴포넌트 — 페이지 1 스크롤 전용 패드"
description: "페이지 1 Actions 패널 하단 매크로 영역을 ScrollPad 컴포넌트로 교체. 항상 활성 상태의 독립 휠 스크롤 입력 패드 구현."
tags: ["android", "scrollpad", "scroll", "wheel", "hid", "ui", "page1"]
version: "v1.0"
owner: "Chatterbones"
updated: "2026-05-03"
---

# BridgeOne Phase 4.13: ScrollPad 컴포넌트

**개발 기간**: 2~3일

**목표**: 페이지 1 Actions 패널 하단의 비활성 매크로 플레이스홀더(`MacrosPlaceholder`)를 제거하고, 항상 활성 상태인 **ScrollPad 컴포넌트**로 교체합니다. 커서 이동(터치패드)과 휠 스크롤(ScrollPad)을 동시에 사용하는 양손 조작 플로우를 구현합니다.

**핵심 성과물**:
- `ScrollPadWrapper.kt` 신규 컴포넌트 (수직 + 수평 스크롤, NORMAL + INFINITE 모드)
- `ActionsPanel.kt`(`ui/pages/standard/components/`) 내 `MacrosPlaceholder` 제거 및 `ScrollPadWrapper` 배치 + `MacrosPlaceholder.kt` 파일 삭제
- `styleframe-page1.md` §2.2-C 갱신 완료 (Phase 4.13 시작 전 기완료)
- `component-scrollpad.md` 신규 명세서 작성 완료 (Phase 4.13 시작 전 기완료)

**선행 조건**: Phase 4.2 (Page 1 Actions 패널) 완료

> **⚠️ Phase 4.7.4-A 변경사항**: Phase 4.7.4에서 `StandardModePage`가 분해되어 Actions 패널 관련 코드가 별도 파일로 이동했다. 매크로 영역은 더 이상 `StandardModePage.kt`에 없고 `ui/pages/standard/components/ActionsPanel.kt`(LazyColumn 본체)와 `ui/pages/standard/components/MacrosPlaceholder.kt`(internal 함수 정의)에 있다. 아래 모든 줄 번호(`StandardModePage.kt:643-678` 등)는 폐기하고 본문의 교체된 위치를 따를 것.

> **⚠️ Phase 간 순서 주의**: Phase 4.15.4가 `ActionsPanel`을 동적 렌더링(`DefaultPageTemplates`)으로 흡수할 예정이다. Phase 4.13을 4.15보다 먼저 구현하면 4.15.7 컴포넌트 카탈로그에 `SCROLL_PAD` 타입 추가가 필요하다.

**재사용 자산** (수정 없음):
- `TouchpadMode.kt` — `ScrollMode`, `ScrollAxis`, `ScrollSensitivity`
- `ScrollConstants.kt` — 스크롤 튜닝 상수 (단, Phase 4.7.1에서 엣지 스와이프 상수는 `EdgeSwipeConstants.kt`로 분리됨 — ScrollPad가 엣지 상수를 참조하면 import 경로 주의)
- `DeltaCalculator.kt` — `determineRightAngleAxis`, `applyDeadZone`, `convertDpToPixels`
- `ClickDetector.kt` — `createWheelFrame`, `createHorizontalWheelFrame`, `sendFrame`
- `ScrollGuideline.kt` — 축 가이드라인 시각화
- `protocol/BridgeFrame.kt` / `FrameBuilder.kt` — HID 프로토콜 변경 없음

**에뮬레이터 호환성**: 4.13.1~4.13.3 UI 개발은 에뮬레이터에서 가능. 실제 휠 이벤트 확인(4.13.2+) 및 관성 튜닝(4.13.4)은 실기기 필요.

| 하위 Phase | 내용 | 상태 |
|-----------|------|------|
| 4.13.1 | ScrollPad 골격 + MacrosPlaceholder 교체 | 미시작 |
| 4.13.2 | 수직 NORMAL 스크롤 구현 | 미시작 |
| 4.13.3 | 수평 축 추가 + 가이드라인 | 미시작 |
| 4.13.4 | INFINITE 관성 모드 + 모드 칩 | 미시작 |
| 4.13.5 | 레이아웃 비율 재조정 + 실기기 검증 | 미시작 |

---

## Phase 4.13.1: ScrollPad 골격 + MacrosPlaceholder 교체

**개발 기간**: 0.5일

> **⚠️ Phase 4.16(가로 지원) 대비**: ScrollPad는 Page 1 Actions 패널에 들어가므로 Page 1 가로 레이아웃(Phase 4.16.4)에서 ScrollPad 배치도 함께 다뤄진다. ScrollPad 자체 크기를 상하단 Safe Zone이나 세로 비율에 고정하지 말고 잔여 영역 기반(`weight`/`fillMaxHeight`)으로 두어 가로에서도 적응되게 할 것.

**세부 목표**:
1. `ui/components/scrollpad/ScrollPadWrapper.kt` 신규 파일 생성
   - 빈 `Box`, `fillMaxWidth()` + `fillMaxHeight()`
   - 배경 `#1A1A1A`, 라운드 코너 12dp, 테두리 `#2A2A2A` 1dp
   - "스크롤 패드" 텍스트 표시 (임시, 이후 제거)
2. 매크로 영역 제거 및 ScrollPad 배치:
   - `ActionsPanel.kt`(`ui/pages/standard/components/`)의 LazyColumn에서 "매크로" 그룹 헤더 `item {}` + `item { MacrosPlaceholder() }` 제거 (현재 "── Macros 그룹 ──" 블록)
   - 같은 위치에 "스크롤 패드" 그룹 헤더 + `item { ScrollPadWrapper() }` 배치
   - `MacrosPlaceholder.kt` 파일 삭제 (`ui/pages/standard/components/MacrosPlaceholder.kt`)
   - `StandardModePage.kt`는 수정 불필요

**수정 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/pages/standard/components/ActionsPanel.kt`
  — "매크로" 그룹 → "스크롤 패드" 그룹 교체

**삭제 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/pages/standard/components/MacrosPlaceholder.kt`

**신규 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/scrollpad/ScrollPadWrapper.kt`

**참조 문서**:
- `docs/android/component-scrollpad.md` §1.1
- `docs/android/styleframe-page1.md` §2.2-C

**검증**:
- [ ] 빌드 성공
- [ ] 페이지 1 우측 하단에 ScrollPad 배경 영역이 표시됨
- [ ] 매크로 버튼 3개 완전히 제거됨

---

## Phase 4.13.2: 수직 NORMAL 스크롤 구현

**개발 기간**: 0.5일

**세부 목표**:
1. `ScrollPadWrapper`에 `pointerInput` 드래그 감지 추가
2. 축 잠금:
   - `DeltaCalculator.determineRightAngleAxis(dx, dy)` 재사용
   - 축 미확정(UNDECIDED) → `SCROLL_AXIS_LOCK_DISTANCE_DP`(기본: 8dp) 초과 시 VERTICAL 확정
3. NORMAL 스크롤 단위 송출:
   - `SCROLL_UNIT_DISTANCE_DP`(기본: 15dp)마다 `ClickDetector.createWheelFrame(wheelDelta)` 호출
   - 부호: `wheelDelta = (-direction).toByte()`
   - 방향 부스트: `ScrollDirectionBoost.getBoost(direction, axis)` 적용
   - 프레임 게이트: `SCROLL_FRAME_MIN_INTERVAL_MS`, `SCROLL_MAX_FRAMES_PER_EVENT`
4. 시각 상태:
   - DOWN → 테두리 `#2196F3` 2dp
   - UP → 테두리 `#2A2A2A` 1dp 복귀

**수정 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/scrollpad/ScrollPadWrapper.kt`

**참조 문서**:
- `docs/android/component-scrollpad.md` §3.2, §4.2
- `ui/common/ScrollConstants.kt`

**참조 코드 패턴**:
- `TouchpadWrapper.kt` 단위 송출 루프 (Phase 4.7.1/4.7.3로 줄 번호가 이동했으니 `:925-957`은 근사치 — 함수명/패턴으로 탐색)

**검증**:
- [ ] 빌드 성공
- [ ] ScrollPad 위에서 위아래로 드래그 시 PC 브라우저/메모장에서 휠 스크롤 동작
- [ ] 드래그 중 테두리 강조색 변경 확인
- [ ] 부호 방향 확인 (손가락 위로 → 콘텐츠 아래로 스크롤)

---

## Phase 4.13.3: 수평 축 추가 + 가이드라인

**개발 기간**: 0.5일

**세부 목표**:
1. 수평 스크롤 분기:
   - 축 잠금 HORIZONTAL 시 `ClickDetector.createHorizontalWheelFrame(wheelDelta)` 사용
   - 수직/수평 동일한 단위 거리 및 방향 부스트 적용
2. 축 가이드라인 시각화:
   - `ScrollGuideline.kt` 재사용
   - VERTICAL 확정 → 세로 가이드라인 표시 (NORMAL: 초록, INFINITE: 빨강)
   - HORIZONTAL 확정 → 가로 가이드라인 표시
   - 드래그 종료 후 페이드아웃

**수정 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/scrollpad/ScrollPadWrapper.kt`

**참조 문서**:
- `docs/android/component-scrollpad.md` §1.2, §2.2

**참조 컴포넌트**:
- `ui/components/touchpad/ScrollGuideline.kt`

**검증**:
- [ ] 빌드 성공
- [ ] 좌우 드래그 시 수평 휠 이벤트 발생 (브라우저 좌우 스크롤 등으로 확인)
- [ ] 축 방향 가이드라인 표시 확인 (수직/수평 각각)
- [ ] 축 잠금 후 반대 방향 드래그해도 잠금 유지 확인

---

## Phase 4.13.4: INFINITE 관성 모드 + 모드 칩

**개발 기간**: 0.5일

**세부 목표**:
1. 관성 코루틴 구현:
   - 드래그 중 속도 샘플링 (최근 윈도우 평균)
   - UP 시 관성 코루틴 시작: 지수 감쇠 `v(t) = v₀ × exp(-t/τ)`, τ = `INFINITE_SCROLL_TIME_CONSTANT_MS`(기본: 1500f ⚠️ 의도적 변경)
   - 속도 < 임계값 시 자동 종료
   - DOWN 시 즉시 코루틴 취소
2. 모드 토글 칩 UI:
   - `Alignment.TopEnd` 오버레이, 패딩 `end=6dp, top=6dp`
   - NORMAL 모드: "≈" 텍스트, 흐린 색상
   - INFINITE 모드: "∞" 텍스트, 빨강 계열 강조
   - 탭 시 NORMAL ↔ INFINITE 전환, 세션 내 상태 기억
   - 이벤트 소비: 칩 탭이 드래그로 오인 방지
3. 관성 중 시각 상태: 테두리 `#2196F3` 1dp (점선 또는 낮은 불투명도)

**수정 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/scrollpad/ScrollPadWrapper.kt`

**참조 문서**:
- `docs/android/component-scrollpad.md` §1.3, §1.4, §3.3, §3.4, §4.3

> **⚠️ Phase 4.7.3-B 변경사항**: 관성/드래그 햅틱은 인라인 `vibrate()`가 아니라 `ui/common/HapticFeedbackHelper.vibrateByVelocity()`로 단일화됐다. ScrollPad가 관성 햅틱이 필요하면 인라인 재작성 말고 이 헬퍼를 재사용할 것. 아래 줄 번호는 4.7.1/4.7.3로 시프트됨.

**참조 코드 패턴**:
- `TouchpadWrapper.kt` 관성 코루틴 (`:1169-1217`은 근사치 — 함수명/패턴으로 탐색)
- `TouchpadWrapper.kt` 관성 중 DOWN → 즉시 정지 (`:413-417` 근사치)

**검증**:
- [ ] 빌드 성공
- [ ] INFINITE 모드에서 빠른 드래그 후 손을 떼면 관성 스크롤 지속
- [ ] 관성 중 손가락 올리면 즉시 정지
- [ ] 모드 칩 탭으로 NORMAL ↔ INFINITE 전환 동작
- [ ] 앱 재시작 후에도 마지막 모드 유지 여부 확인 (세션 내 기억)

---

## Phase 4.13.5: 레이아웃 비율 재조정 + 실기기 검증

**개발 기간**: 0.5일

**세부 목표**:
1. ScrollPad 높이 확보:
   - `ActionsPanel.kt`(`ui/pages/standard/components/`) LazyColumn에서 ScrollPad 그룹이 잔여 높이를 사용하도록 조정
   - `item { ScrollPadWrapper(modifier = Modifier.fillParentMaxHeight()) }` 또는 `weight(1f)` 패턴 적용
   - Special Keys + Shortcuts 그룹이 고정 높이 차지, 나머지를 ScrollPad가 사용
2. 전반적인 시각 점검:
   - Phase 4.2.6 확정 버튼 높이(36dp), 그룹 간격(4dp) 톤 유지
   - 세 그룹이 스크롤 없이 화면에 모두 보이는지 확인

**수정 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/pages/standard/components/ActionsPanel.kt`

**참조 문서**:
- `docs/android/styleframe-page1.md` §2.2, §5
- `docs/android/component-scrollpad.md` §1.1

**검증**:
- [ ] 빌드 성공
- [ ] 실기기에서 페이지 1 전체 레이아웃 시각 확인 (세 그룹 모두 스크롤 없이 표시)
- [ ] ScrollPad 영역이 충분한 높이로 표시됨 (터치 타겟 확보)
- [ ] 터치패드 ScrollMode와 ScrollPad 동시 사용 시 프레임 충돌 없음 확인
- [ ] 수직/수평 스크롤 실기기 정상 동작 최종 확인
