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
- `StandardModePage.kt` 내 `MacrosPlaceholder` 제거 및 `ScrollPadWrapper` 배치
- `styleframe-page1.md` §2.2-C 갱신 완료 (Phase 4.13 시작 전 기완료)
- `component-scrollpad.md` 신규 명세서 작성 완료 (Phase 4.13 시작 전 기완료)

**선행 조건**: Phase 4.2 (Page 1 Actions 패널) 완료

**재사용 자산** (수정 없음):
- `TouchpadMode.kt` — `ScrollMode`, `ScrollAxis`, `ScrollSensitivity`
- `ScrollConstants.kt` — 모든 스크롤 튜닝 상수
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

**세부 목표**:
1. `ui/components/scrollpad/ScrollPadWrapper.kt` 신규 파일 생성
   - 빈 `Box`, `fillMaxWidth()` + `fillMaxHeight()`
   - 배경 `#1A1A1A`, 라운드 코너 12dp, 테두리 `#2A2A2A` 1dp
   - "스크롤 패드" 텍스트 표시 (임시, 이후 제거)
2. `StandardModePage.kt` 변경:
   - `MacrosPlaceholder()` 함수 삭제 (`StandardModePage.kt:643-678`)
   - `ActionsPanel` LazyColumn에서 "매크로" 그룹 헤더 + `item { MacrosPlaceholder() }` 제거 (`StandardModePage.kt:494-506`)
   - 해당 위치에 "스크롤 패드" 그룹 헤더 + `item { ScrollPadWrapper() }` 배치

**수정 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/pages/StandardModePage.kt`

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
- `TouchpadWrapper.kt:925-957` (단위 송출 루프)

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

**참조 코드 패턴**:
- `TouchpadWrapper.kt:1169-1217` (관성 코루틴)
- `TouchpadWrapper.kt:413-417` (관성 중 DOWN → 즉시 정지)

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
   - `ActionsPanel` LazyColumn에서 ScrollPad 그룹이 잔여 높이를 사용하도록 조정
   - `item { ScrollPadWrapper(modifier = Modifier.fillParentMaxHeight()) }` 또는 `weight(1f)` 패턴 적용
   - Special Keys + Shortcuts 그룹이 고정 높이 차지, 나머지를 ScrollPad가 사용
2. 전반적인 시각 점검:
   - Phase 4.2.6 확정 버튼 높이(36dp), 그룹 간격(4dp) 톤 유지
   - 세 그룹이 스크롤 없이 화면에 모두 보이는지 확인

**수정 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/pages/StandardModePage.kt`

**참조 문서**:
- `docs/android/styleframe-page1.md` §2.2, §5
- `docs/android/component-scrollpad.md` §1.1

**검증**:
- [ ] 빌드 성공
- [ ] 실기기에서 페이지 1 전체 레이아웃 시각 확인 (세 그룹 모두 스크롤 없이 표시)
- [ ] ScrollPad 영역이 충분한 높이로 표시됨 (터치 타겟 확보)
- [ ] 터치패드 ScrollMode와 ScrollPad 동시 사용 시 프레임 충돌 없음 확인
- [ ] 수직/수평 스크롤 실기기 정상 동작 최종 확인
