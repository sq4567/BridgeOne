---
title: "BridgeOne Phase 4.5: E2E 하드웨어 테스트 수정사항"
description: "BridgeOne 프로젝트 Phase 4.5 - Phase 4.4.9 E2E 테스트에서 발견된 버그 수정 및 UX 개선, 엣지 스와이프 2단계 제스처"
tags: ["android", "bugfix", "ux", "e2e-test", "right-click", "infinite-scroll", "animation", "edge-swipe"]
version: "v1.0"
owner: "Chatterbones"
updated: "2026-04-03"
---

# BridgeOne Phase 4.5: E2E 하드웨어 테스트 수정사항

**개발 기간**: 2.5-3일

**목표**: Phase 4.4.9 E2E 하드웨어 테스트에서 발견된 버그와 UX 문제를 수정하고, 개발 과정에서 발견된 개선 사항을 반영합니다.

---

## Phase 4.5.1: 우클릭 모드에서 좌클릭만 발생하는 버그 수정

**개발 기간**: 0.5일

**작업 내용**:
- `TouchpadWrapper.kt` UP 이벤트의 `buttonState` 결정 로직에 `latestState.clickMode` 분기 추가
- `latestState.clickMode`가 `RIGHT_CLICK`이면, 자동 판별 결과에 관계없이 우클릭으로 설정

**수정 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/TouchpadWrapper.kt`

**검증**:
- [x] 우클릭 모드 전환 후 짧은 탭 → PC에서 우클릭 메뉴 등장 확인
- [x] 우클릭 모드 전환 후 롱터치 → 우클릭으로 전송 확인 (좌클릭 아님)
- [x] 좌클릭 모드(기본)에서 기존 동작 유지 확인 (짧은 탭 = 좌클릭, 롱터치 = 우클릭)
- [x] Essential 모드에서 우클릭 강제 차단 유지 확인
- [x] 엣지 스와이프로 우클릭 모드 전환 후 탭 → PC에서 우클릭 메뉴 등장 확인

> **추가 수정사항**: Phase 문서에는 `clickMode` 분기 추가만 기술되어 있었으나, 실기기 테스트에서 press→release HID 리포트가 거의 동시에 전송되어 클릭이 토글처럼 동작하는 문제를 발견함. `coroutineScope.launch { delay(30L) }`로 release 프레임에 30ms 지연을 추가하여 해결. 좌클릭·우클릭 모두에 적용됨.

---

## Phase 4.5.2: Essential 모드에서 엣지 스와이프 제스처 차단

**개발 기간**: 0.5일 미만

**작업 내용**:
- `TouchpadWrapper.kt` DOWN 이벤트의 `detectEntryEdge()` 호출 앞에 `bridgeMode == BridgeMode.ESSENTIAL` 가드 추가
- Essential 모드일 때 엣지 진입 감지를 항상 `null`로 처리하여 이후 엣지 스와이프 로직 전체를 비활성화

**수정 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/TouchpadWrapper.kt`

**검증**:
- [x] Essential 모드에서 터치패드 가장자리 스와이프 → 산봉우리·팝업이 전혀 나타나지 않음 확인
- [x] Essential 모드에서 가장자리 터치 시작 후 안쪽 이동 → 일반 커서 이동으로 처리됨 확인
- [x] Standard 모드에서 엣지 스와이프 기존 동작 정상 유지 확인
- [x] Essential 모드에서 테두리 색상이 여전히 투명인지 확인 (기존 동작 보존)

---

## Phase 4.5.3: 직접 터치 모드 확인 시 스와이프 메뉴 깜빡임 버그 수정

**개발 기간**: 0.5일 미만

**증상**:
- 엣지 스와이프 → 직접 터치 모드 선택 → 앵커 지정 → 모드 버튼 토글 → 확인 버튼 탭
- 확인 버튼 탭 직후, 스와이프 모드(SWIPE) 메뉴 UI가 잠깐(1~2프레임) 나타났다 사라짐
- 기능적으로 모드 적용은 정상이지만, 시각적으로 깜빡임이 보임

**작업 내용**:
- `TouchpadWrapper.kt`: 직접 터치 모드 및 스와이프 모드 확인 버튼 처리에서 `resetPopup()` → `latestOnStateChange()` 순서로 변경. `finalState`를 `resetPopup()` 호출 전에 로컬 변수에 캡처
- `EdgeSwipeOverlay.kt`: 소멸 애니메이션 중 `resetPopup()`이 `selectedPopupMode=null`, `popupAnchorPx=Offset.Zero`로 리셋하면 UI 분기가 잘못되어 다른 모드 UI가 깜빡이는 근본 원인 수정. `lastPopupMode`/`lastAnchorPx`로 이전 값을 보존하여 소멸 애니메이션 동안 올바른 UI 분기 유지

**수정 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/TouchpadWrapper.kt`
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/touchpad/EdgeSwipeOverlay.kt`

**검증**:
- [x] 직접 터치 모드로 모드 조합 변경 후 확인 → 스와이프 메뉴 깜빡임 없이 팝업 닫힘 확인
- [x] 직접 터치 모드로 모드 변경 없이 확인 → 깜빡임 없음 확인
- [x] 스와이프 모드에서 확인 → 깜빡임 없음 확인
- [x] 직접 터치 모드 확인 후 변경된 모드가 정상 적용됨 확인 (기능 회귀 없음)
- [x] 스와이프 모드 확인 후 변경된 모드가 정상 적용됨 확인 (기능 회귀 없음)
- [x] 엣지 스와이프 취소(진입 엣지로 되돌리기) 동작에 영향 없음 확인

---

## Phase 4.5.4: 팝업 안내 텍스트 강제 줄바꿈 수정

**개발 기간**: 0.5일 미만

**증상**:
- "직접 터치" 앵커 선택 안내 카드(`"메뉴를 띄울 곳을\n터치하세요"`)에서 첫 줄이 컨테이너 폭을 초과해 추가 줄바꿈이 발생, 총 3줄로 표시됨
- 하단 조작 안내 카드(`"버튼을 직접 터치 · 확인으로 적용\n엣지로 밀어서 취소"`)도 첫 줄이 컨테이너 폭을 초과해 총 3줄로 표시됨

**작업 내용**:
- `EdgeSwipeOverlay.kt` 앵커 선택 안내 텍스트의 `\n` 분기점을 재조정하거나 텍스트를 단축해 각 줄이 2줄 이내로 표시되도록 수정
- 하단 안내 카드 텍스트도 동일하게 줄바꿈 위치 재조정 또는 텍스트 단축

**수정 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/touchpad/EdgeSwipeOverlay.kt`
  — 앵커 선택 안내 텍스트(`"메뉴를 띄울 곳을\n터치하세요"`) `\n` 위치 조정
  — 하단 조작 안내 텍스트(`"버튼을 직접 터치 · 확인으로 적용\n엣지로 밀어서 취소"`) `\n` 위치 조정

**검증**:
- [x] "직접 터치" 선택 → 앵커 미설정 상태에서 안내 텍스트가 2줄 이내로 표시됨 확인
- [x] 하단 안내 카드가 스와이프 모드·직접 터치 모드 모두에서 2줄 이내로 표시됨 확인
- [x] SM-G970N 실기기 기준 강제 줄바꿈 미발생 확인

> **실제 구현 (계획과 다름)**: 앵커 안내 카드는 `\n` 조정이 아닌 폰트 축소 + 패딩 감소로 해결.
> - 앵커 카드: `\n` 제거(한 줄화), 폰트 `14.sp → 12.sp`, 외부 패딩 `40dp → 20dp`, 내부 패딩 `20dp/16dp → 12dp/12dp`
> - 하단 안내 카드: `"버튼을 직접 터치 · 확인으로 적용"` → `"직접 터치 · 확인으로 적용"` ("버튼을 " 제거)
> - 원인: 카드가 컨텐츠 너비로 크기가 결정되므로, `\n`으로 첫 줄을 짧게 하면 카드가 오히려 더 좁아져 나머지 줄도 강제 줄바꿈되는 역효과 발생

---

## Phase 4.5.5: Y축 스크롤 축 고정이 풀리는 버그 수정

**개발 기간**: 0.5-1일

**증상**:
- 일반 스크롤·무한 스크롤 모두에서 발생
- 상하 스크롤 시 Y축으로 축이 고정된 후, 손가락을 터치패드 안에서 자유롭게 움직이면 Y축 고정이 풀림
- 축 고정이 풀리면 스크롤이 멈추거나 오동작하고, 가이드라인도 Y축 방향을 유지하지 못함
- **X축(좌우) 고정은 같은 상황에서 정상 유지됨** — Y축에서만 발생하는 비대칭 문제

**원인 분석**:
- `scrollAxis`는 제스처당 로컬 변수로 매 `awaitEachGesture` 반복마다 `UNDECIDED`로 초기화됨
- 스크롤 중 손가락을 자유 이동하다 살짝 뗐다 다시 터치하면 새 제스처가 시작되어 축 리셋 발생
- 새 제스처의 초기 이동이 대각선 데드존(30°-60°)에 걸리면 축 결정이 지연되고, 이전과 다른 축으로 고정될 수 있음
- Y축이 X축보다 취약한 이유: 수직 스크롤 후 재터치 시 손가락 재배치로 X 성분이 커져 대각선 데드존 진입 빈도가 높음
- 추가 방어: `while (type == Move)` 루프가 Enter/Exit 등 비정상 이벤트로 조기 종속되는 경로도 차단

**수정 내용**:
1. `lastScrollAxis` remember 상태 추가 — 이전 제스처에서 확정된 축을 제스처 간 유지
2. 데드존(30°-60°) 구간에서 `lastScrollAxis`가 있으면 `UNDECIDED` 대신 해당 축으로 즉시 확정
3. 스크롤 모드 종료 시 `lastScrollAxis` 리셋하여 다음 스크롤 진입 시 깨끗한 상태 보장
4. MOVE while 루프를 `type != Release` 조건으로 변경하여 Enter/Exit 이벤트로 인한 조기 종료 방지

**수정 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/TouchpadWrapper.kt`

**검증**:
- [x] 일반 스크롤 모드에서 Y축(상하) 고정 후 손가락을 자유롭게 움직여도 스크롤이 Y축으로만 계속 작동 확인
- [x] 무한 스크롤 모드에서 Y축 고정 후 동일하게 작동 확인
- [x] X축(좌우) 고정 후 자유 이동 시 기존과 동일하게 잘 유지되는지 확인 (회귀 없음)
- [x] Y축 스크롤 중 가이드라인이 Y축 방향으로만 계속 표시되는지 확인
- [x] 스크롤 모드 외 기능(커서 이동, 클릭, 엣지 스와이프 등)에 영향 없음 확인

---

## Phase 4.5.6: 무한 스크롤 가이드라인 애니메이션 버그 수정

**개발 기간**: 0.5일

### 확인된 증상

**버그 A — 드래그 중 가이드라인이 스크롤 단위마다만 점프**
- 무한 스크롤 모드에서 손가락을 떼지 않은 채 Y축으로 이동하면 스크롤(HID 전송)과 진동은 정상 동작하지만, 가이드라인 선들이 손가락과 함께 연속으로 흐르지 않음
- 완전히 멈춰 있지는 않으며, 약 1–2초에 한 번씩 위치가 뚝 이동하는 형태로 매우 끊겨서 보임
- 끊김 간격은 드래그 속도와 연동됨: 느리게 드래그할수록 간격이 더 길어짐

**버그 B — 관성 단계 가이드라인 끊김**
- 손가락을 밀어 놓으면 관성 스크롤 자체(속도·감속)는 원하는 대로 동작하나, 가이드라인 이동 애니메이션이 뚝뚝 끊기면서 보임

### 관련 코드 위치

| 위치 | 역할 |
|------|------|
| `TouchpadWrapper.kt` 라인 183–199 | `guidelineHideJob` (mutableStateOf) 선언, `scheduleGuidelineHide()` 정의 |
| `TouchpadWrapper.kt` 라인 176 | `guidelineTarget` (mutableFloatStateOf) 선언 |
| `TouchpadWrapper.kt` 라인 754–766 | 스크롤 단위 전송 시 `scheduleGuidelineHide()` 호출 → `guidelineHideJob` 갱신 |
| `TouchpadWrapper.kt` 라인 770–784 | 무한 스크롤 MOVE 이벤트마다 `guidelineTarget += axisDeltaDp` 업데이트 |
| `TouchpadWrapper.kt` 라인 979–1023 | 관성 루프 — `delay(16L)` 간격으로 `guidelineTarget += moveDp` 업데이트 |
| `ScrollGuideline.kt` 라인 103–111 | `LaunchedEffect(targetOffset)` → spring 애니메이션으로 `animOffset` 추적 |
| `ScrollConstants.kt` 라인 41–44 | `SCROLL_GUIDELINE_SPRING_STIFFNESS = 10_000f`, `SCROLL_GUIDELINE_SPRING_DAMPING = 1.0f` |

### 작업 내용

> **실제 구현 (계획과 다름)**: `TouchpadWrapper.kt`는 수정 불필요. `ScrollGuideline.kt`만 수정.
> - 원인: `LaunchedEffect(targetOffset)`은 타겟이 바뀔 때마다 이전 `animateTo` 코루틴을 취소하고 새로 시작. 드래그/관성 중처럼 매 프레임 타겟이 변하면 `animateTo`가 시작되자마자 취소되어 실제 이동 없다가 값이 잠시 멈추는 순간에만 spring이 "뚝" 진행됨.
> - 해결: `LaunchedEffect(targetOffset, scrollMode)`로 변경 + 무한 스크롤 모드에서는 `animateTo` 대신 `snapTo`(즉각 추적) 사용. 일반 스크롤 모드는 기존 spring `animateTo` 유지.

**수정 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/touchpad/ScrollGuideline.kt`
  — `LaunchedEffect(targetOffset)` → `LaunchedEffect(targetOffset, scrollMode)` + 무한 스크롤 시 `snapTo` 분기 추가

**검증**:
- [x] 무한 스크롤 드래그 중 가이드라인 선들이 손가락 이동과 함께 연속으로 흐르는지 실기기 확인 (버그 A)
- [x] 드래그 속도와 무관하게 가이드라인 업데이트 간격이 일정하고 부드러운지 확인 (버그 A 회귀)
- [x] 관성 단계에서 가이드라인이 끊김 없이 부드럽게 감속하는지 확인 (버그 B)
- [x] 일반 스크롤 모드의 가이드라인 동작에 영향 없는지 확인
- [x] 스크롤 정지 후 가이드라인 숨김 타이밍이 기존과 동일한지 확인

---

## Phase 4.5.7: 무한 스크롤 고속 스크롤 시 phantom 키 입력 버그 수정

**개발 기간**: 0.5일

**증상**:
- 무한 스크롤 모드에서 매우 빠르게 스크롤하면 Windows에서 경보음(삐삐삐삐) 발생
- 이후 알 수 없는 키 입력이 발생하고 Microsoft 365 URL(`https://m365.cloud.microsoft/`)이 브라우저에서 열림
- 느린 스크롤에서는 재현 안 됨 — 빠를수록 재현 확률 높음

**원인**:
- `TouchpadWrapper.kt`의 MOVE 이벤트 처리 중 `while (abs(scrollAccum) >= effectiveUnitPx)` 루프가 throttle 없이 단일 터치 이벤트에서 수십 개의 BridgeFrame을 연속 전송
- 예: 1회 MOVE에서 손가락이 300dp 이동 + `SCROLL_UNIT_DISTANCE_DP = 20dp` + 감도 1.0 → 15프레임 즉시 전송
- ESP32-S3의 UART RX 버퍼 크기는 256바이트(= 32프레임 수용 한계), `frame_queue` 크기는 10
- `frame_queue`가 가득 차면 `uart_task`가 최대 10ms 블록되고, 그 사이 UART RX 버퍼에 쌓이는 바이트가 한도를 초과하면 바이트 드롭 발생
- 바이트가 드롭되면 8바이트 프레임 경계가 어긋나고, 이후 read가 잘못된 오프셋에서 시작됨
- 미정렬 프레임에서는 `wheel`·`seq`·`deltaX` 등 다른 필드의 바이트 값이 `modifier`(키보드 수정자) 또는 `keycode1`/`keycode2` 위치에 놓이게 됨
- 예: `seq = 0x08`이 `modifier` 위치에 오면 `LEFT_GUI`(bit3 = Windows 키) 설정 → Windows 단축키 트리거 → M365 URL 오픈
- 연속된 phantom 수정자 키 토글이 삐삐삐삐 경보음(Windows Sticky Keys 혹은 FilterKeys 경고)을 유발

**수정 내용**:
- `TouchpadWrapper.kt`: MOVE 이벤트 내 스크롤 전송 루프의 최대 프레임 수를 `SCROLL_MAX_FRAMES_PER_EVENT`로 제한
  - `framesThisEvent < SCROLL_MAX_FRAMES_PER_EVENT` 조건을 `while` 루프에 추가
  - 제한 초과 시 나머지 `scrollAccum`은 다음 MOVE 이벤트로 자연 이월 (스크롤 거리 손실 없음)
- `ScrollConstants.kt`: `SCROLL_MAX_FRAMES_PER_EVENT = 5` 상수 추가

> **선택적 보강**: `uart_handler.h`의 `UART_RX_BUFFER_SIZE`를 256 → 1024로 늘리면 RX 버퍼 여유가 4배로 커져 버스트 내성이 높아짐. Android 측 수정만으로도 재현이 없으면 펌웨어 수정 불필요.

**수정 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/common/ScrollConstants.kt`
  — `SCROLL_MAX_FRAMES_PER_EVENT = 5` 상수 추가
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/TouchpadWrapper.kt`
  — MOVE 이벤트 스크롤 루프에 `framesThisEvent` 카운터 및 `SCROLL_MAX_FRAMES_PER_EVENT` 상한 추가

**검증**:
- [ ] 무한 스크롤 모드에서 최대한 빠르게 스크롤 → 경보음 없고 phantom 키 입력 없음 확인
- [ ] 동일 조건에서 브라우저/앱이 예상치 않게 열리지 않음 확인
- [ ] 빠른 스크롤에서도 스크롤 속도·거리가 느린 스크롤 대비 비례적으로 유지되는지 확인 (거리 손실 없음)
- [ ] 일반 스크롤 모드의 고속 스크롤에서도 동일 현상 없음 확인

---

## Phase 4.5.8: 무한 스크롤 방향별 속도 비대칭 보정 옵션

**개발 기간**: 0.5-1일

**작업 내용**:
- `ScrollConstants.kt`에 `InfiniteScrollDirectionBoost` 상수 객체 추가 (UP/DOWN 방향별 배율, 기본값 `1.0f`)
- `TouchpadWrapper.kt` 무한 스크롤 MOVE 이벤트 및 관성 초기 속도 계산에 방향별 배율 적용
- 적용 범위: 무한 스크롤 모드에만 적용, 일반 스크롤·커서 이동 영향 없음

**수정 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/common/ScrollConstants.kt`
  — `InfiniteScrollDirectionBoost` 상수 추가
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/TouchpadWrapper.kt`
  — 무한 스크롤 MOVE 이벤트 및 관성 초기 속도 계산에 방향별 배율 적용

**검증**:
- [ ] `DOWN_MULTIPLIER = 1.5f` 설정 후 아래로 스와이프 시 기존 대비 스크롤 속도 1.5배 증가 확인
- [ ] `UP_MULTIPLIER = 1.0f` 유지 시 위로 스와이프 속도 변화 없음 확인
- [ ] 배율 변경 시 관성 단계에도 동일하게 반영되는지 확인
- [ ] 일반 스크롤 모드에 영향 없음 확인
- [ ] 양방향 모두 `1.0f`일 때 기존 동작과 완전 동일한지 확인

---

## Phase 4.5.9: 엣지 스와이프 2단계 제스처 UX 개선

**개발 기간**: 1일

**에뮬레이터 호환성**: 전체 에뮬레이터에서 개발 가능.

### 현재 동작 vs 새 동작

| 단계 | 현재 (1단계) | 변경 후 (2단계) |
|------|------------|----------------|
| ① | 엣지에서 안쪽으로 스와이프 | 엣지에서 안쪽으로 스와이프 |
| ② | 손가락을 원하는 모드 위로 이동 | 트리거 거리 도달 → 팝업 등장 |
| ③ | **손을 뗌 → 모드 확정·팝업 닫힘** | **손을 뗌 → 팝업 고정 (메뉴 유지)** |
| ④ | — | 다시 손가락을 올려 원하는 모드 위로 이동 |
| ⑤ | — | **탭으로 확정** |
| 취소 | 진입 엣지로 되돌리기 | 팝업 고정 전: 진입 엣지로 되돌리기<br>팝업 고정 후: 안쪽→바깥쪽 스와이프 |

### 추가 상수 (`ScrollConstants.kt` — `EdgeSwipeConstants`)

- `LEGACY_GESTURE_MODE`: A/B 비교 플래그 (`true` = 기존 방식, `false` = 2단계 방식)
- `PINNED_TAP_MAX_DISTANCE_DP`, `PINNED_TAP_MAX_DURATION_MS`: 고정 상태에서 탭 판정 기준
- `PINNED_CLOSE_DISTANCE_DP`: 바깥쪽 스와이프 취소 최소 거리

### 상태 변수 추가 (`TouchpadWrapper.kt`)

- `edgeMenuPinned`: 팝업 고정 상태 플래그 (신규)
- `pinnedDownX`, `pinnedDownY`, `pinnedDownTimeMs`: 고정 상태에서 터치 다운 기록
- `pinnedCurX`, `pinnedCurY`: 고정 상태에서 현재 터치 위치

### 이벤트 처리 변경 (`TouchpadWrapper.kt`)

- **DOWN**: `edgeMenuPinned` 분기를 기존 `showEdgePopup` 분기보다 먼저 배치하여 고정 상태 터치 기록
- **MOVE**: `edgeMenuPinned` 상태에서 `hoveredMode` 갱신 (기존 `calculateHoveredEdgeMode()` 재사용)
- **UP**: `LEGACY_GESTURE_MODE` 분기
  - `true`: 기존 방식 (손 뗌 = 확정)
  - `false`: 고정 상태 판정 — 탭이면 모드 적용, 바깥 스와이프면 취소, 그 외는 팝업 유지
- `isOutwardSwipe()` private 헬퍼 추가
- `resetEdgeSwipeState()` 통합 헬퍼 추가 (기존 분산된 초기화 로직 통합)

### 팝업 안내 텍스트 변경 (`EdgeSwipeOverlay.kt`)

`EdgeSwipeOverlay`에 `isPinned: Boolean` 파라미터 추가:

| 상태 | `isPinned` | 안내 텍스트 |
|------|-----------|------------|
| 팝업 등장 중 (스와이프 유지) | `false` | 새 방식: "손을 떼면 메뉴가 고정됩니다" |
| 메뉴 고정 후 (핀 상태) | `true` | "원하는 모드를 탭하세요 / 바깥쪽으로 스와이프하면 닫힘" |

`TouchpadWrapper.kt`에서 `EdgeSwipeOverlay` 호출 시 `isPinned = edgeMenuPinned` 전달.

### 수정 파일

- `src/android/app/src/main/java/com/bridgeone/app/ui/common/ScrollConstants.kt`
  — `EdgeSwipeConstants`에 `LEGACY_GESTURE_MODE`, `PINNED_TAP_*`, `PINNED_CLOSE_DISTANCE_DP` 추가
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/TouchpadWrapper.kt`
  — `edgeMenuPinned` 등 신규 상태 추가, UP/DOWN/MOVE 이벤트 분기 수정, `isOutwardSwipe()` 헬퍼 추가, `resetEdgeSwipeState()` 헬퍼 추가
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/touchpad/EdgeSwipeOverlay.kt`
  — `isPinned: Boolean` 파라미터 추가, 안내 텍스트 분기

**검증**:

**새 방식 (`LEGACY_GESTURE_MODE = false`)**:
- [ ] 엣지 스와이프 → 트리거 → 손 뗌 → 팝업이 닫히지 않고 고정 확인
- [ ] 고정 상태에서 다시 터치 후 이동 시 hoveredMode 하이라이트 갱신 확인
- [ ] 고정 상태에서 탭 → 모드 적용 + 팝업 닫힘 확인
- [ ] 고정 상태에서 바깥쪽 스와이프(60dp 이상) → 취소·팝업 닫힘 확인 (모드 미변경)
- [ ] 고정 상태에서 단순히 손만 뗌(탭 거리 초과, 바깥 스와이프 미달) → 팝업 유지 확인
- [ ] 팝업 고정 전 취소(진입 엣지로 되돌리기) → 기존과 동일하게 동작 확인
- [ ] 안내 텍스트가 고정 전/후 올바르게 전환되는지 확인

**기존 방식 (`LEGACY_GESTURE_MODE = true`)**:
- [ ] 엣지 스와이프 → 트리거 → 손 뗌 → 즉시 모드 확정 확인 (기존 동작 보존)
- [ ] 진입 엣지로 되돌리기 → 취소 확인 (기존 동작 보존)

---

## Phase 4.5.10: 엣지 스와이프 팝업 등장 시 햅틱 피드백

**개발 기간**: 0.5일 미만

**작업 내용**:
- `TouchpadWrapper.kt`에서 `showEdgePopup`이 `false → true`로 전환되는 시점에 `LaunchedEffect(showEdgePopup)`으로 햅틱 피드백 1회 발생

**수정 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/TouchpadWrapper.kt`

**검증**:
- [ ] 엣지 스와이프로 산봉우리 팝업이 등장하는 순간 진동 발생 확인
- [ ] 팝업 닫힘·취소 시 추가 진동 없음 확인 (등장 시 1회만)
- [ ] 기기 진동 설정이 꺼진 상태에서 앱 크래시 없음 확인
- [ ] Essential 모드(Phase 4.5.2 적용 후)에서는 팝업도 진동도 발생하지 않음 확인

---

## Phase 4.5.11: 산봉우리 애니메이션 간소화 (베이스 고정, 피크만 추종)

**개발 기간**: 0.5일

### 현재 동작 vs 새 동작

| 항목 | 현재 | 변경 후 |
|------|------|---------|
| 베이스(발) 위치 | 손가락 현재 위치를 계속 따라 이동 | 제스처 진입점에 고정 |
| 피크(꼭대기) 위치 | 항상 베이스 중앙 | 손가락 현재 위치를 추종 |
| 높이(돌출량) | `inwardDistancePx` 비례 | 동일 (변경 없음) |

### 구현 상세

#### `TouchpadWrapper.kt` — 진입 시 `entryAlongEdgePx` 기록

- `entryAlongEdgePx` 상태 변수 추가
- DOWN 이벤트에서 `isEdgeCandidate = true`가 되는 시점에 현재 `fingerAlongEdgePx` 값을 한 번만 고정 저장
- `resetEdgeSwipeState()`에 `entryAlongEdgePx = 0f` 초기화 추가

#### `EdgeBumpOverlay` — `entryAlongEdgePx` 파라미터 추가

- 시그니처에 `entryAlongEdgePx: Float` 파라미터 추가
- 4개 엣지 분기 각각에서 베이스(발) 좌표는 `entryAlongEdgePx` 기준, 피크(꼭대기) 좌표는 `fingerAlongEdgePx` 기준으로 분리
- glow 위치(피크 끝점)는 `fingerAlongEdgePx` 기준 유지

**수정 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/touchpad/EdgeSwipeOverlay.kt`
  — `EdgeBumpOverlay` 시그니처에 `entryAlongEdgePx: Float` 파라미터 추가
  — 4개 엣지 분기에서 베이스/피크 좌표 분리 적용
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/TouchpadWrapper.kt`
  — `entryAlongEdgePx` 상태 변수 추가 및 DOWN 이벤트에서 기록
  — `resetEdgeSwipeState()`에 `entryAlongEdgePx = 0f` 초기화 추가
  — `EdgeBumpOverlay` 호출 시 `entryAlongEdgePx` 전달

**검증**:
- [ ] LEFT 엣지 진입 후 손가락을 위아래로 움직일 때 산봉우리 발이 진입점에 고정되고 피크만 기울어지는지 확인
- [ ] TOP / BOTTOM / RIGHT 엣지에서도 동일하게 동작 확인
- [ ] 손가락을 엣지 따라 크게 이동해도 베이스가 이탈하지 않는지 확인
- [ ] 피크가 지나치게 기울어져 모양이 무너지는 경우 없는지 확인 (베이스에서 피크까지 거리 상한 필요 시 `fingerAlongEdgePx.coerceIn()` 적용 검토)
- [ ] `resetEdgeSwipeState()` 이후 다음 제스처 시 베이스 위치가 올바르게 초기화되는지 확인
- [ ] 팝업 등장 및 소멸 애니메이션(Phase 4.4.7)과 시각적으로 어색함 없이 연동되는지 확인

---

## Phase 4.5.12: Windows 서버 감지 간헐적 실패 — 종합 원인 조사 및 수정

**개발 기간**: 2-3일 (조사 포함)

**쉬운 설명**: 핸드폰이 "PC에 윈도우 서버가 켜져 있는지"를 가끔 감지 못해서 기능이 제한된 모드(ESSENTIAL)로 들어가는 문제입니다. 보드를 PC에서 뺐다 꽂거나, 핸드폰 쪽 선을 뺐다 꽂으면 해결되기도 하는 등 원인이 다양해 보여서 처음부터 전체적으로 조사합니다.

**증상**:
- ESP32-S3가 PC에 연결되어 있고 Windows 서버가 실행 중임에도, 앱이 ESSENTIAL 모드로 진입
- 항상 발생하는 것은 아니고 **간헐적**
- 아래 방법 중 하나로 복구되는 경우가 있음 (일관되지 않음):
  - **보드를 PC에서 분리 후 재연결** → 복구됨
  - **핸드폰 쪽 USB 케이블 분리 후 재연결** → 복구되는 경우도 있음
  - **핸드폰 재연결만으로는 복구 안 되는 경우**도 있음

### 1단계: 원인 조사 (구현 전 반드시 선행)

아래 후보 원인들을 **코드 분석 + 로그 확인**으로 하나씩 검증합니다. 각 항목에서 실제 원인인지 아닌지를 판별한 후, 확인된 원인에 대해서만 수정을 진행합니다.

#### 조사 A: ESP32 핸드셰이크 상태 고착
- ESP32 ↔ Windows 서버 핸드셰이크가 실패한 뒤 재시도 없이 IDLE 상태에 영구 고착되는지
- 핸드폰 재연결(UART 리셋)이 ESP32 내부 connection_state를 리셋하는지 여부
- 보드 PC 재연결(USB 리셋)이 connection_state를 리셋하는지 여부
- **확인 방법**: `connection_state.c` 상태 전이 로직 분석, ESP32 CDC 디버그 로그 확인

#### 조사 B: Windows 서버 CDC 포트 열기/인식 실패
- 서버가 ESP32의 CDC 포트를 열지 못하거나, 열었지만 통신이 성립하지 않는 경우
- 서버 시작 타이밍에 따라 ESP32가 아직 CDC를 열거나(enumerate) 초기화하지 못한 상태일 수 있음
- **확인 방법**: Windows 서버 로그에서 COM 포트 오픈 성공/실패 기록 확인, 서버 시작 순서별 재현 테스트

#### 조사 C: Android 앱의 모드 판정 로직 타이밍
- 앱이 USB 연결 직후 서버 감지 응답을 기다리는 타임아웃이 너무 짧아서, ESP32가 아직 핸드셰이크 중인데 ESSENTIAL로 결정하는 경우
- 핸드폰 재연결로 복구되는 이유가 "두 번째 시도 시 ESP32가 이미 STANDARD 상태"이기 때문일 수 있음
- **확인 방법**: Android 앱의 모드 판정 타이밍 및 타임아웃 값 분석, 로그 확인

#### 조사 D: USB 열거(Enumeration) 순서 문제
- ESP32가 PC에 연결될 때 HID + CDC 복합 장치로 열거되는 순서에 따라 Windows가 CDC 포트를 늦게 인식하는 경우
- 핸드폰 쪽 재연결이 ESP32의 USB 재열거를 유발하여 우연히 해결되는 것일 수 있음
- **확인 방법**: Windows 장치 관리자에서 COM 포트 인식 타이밍 확인, ESP32 USB 초기화 순서 분석

#### 조사 E: UART 통신 경로 (Android ↔ ESP32) 불안정
- CH343P UART 브릿지의 초기화 타이밍이나 DTR/RTS 시그널 상태에 따라 첫 프레임이 유실되는 경우
- 핸드폰 재연결이 UART를 리셋하면서 DTR 신호가 재전송되어 복구되는 것일 수 있음
- **확인 방법**: UART 초기화 시퀀스 분석, DTR/RTS 핸들링 코드 확인

#### 조사 F: Keep-alive / 연결 유지 메커니즘 부재 또는 미흡
- 한 번 연결된 후 서버가 종료·재시작되거나 일시적 통신 끊김이 있을 때 재감지 메커니즘이 없는 경우
- **확인 방법**: 현재 keep-alive 구현 여부 및 타임아웃 후 재연결 로직 확인

### 2단계: 확인된 원인별 수정

> 1단계 조사 결과에 따라 이 섹션을 구체화합니다. 아래는 수정이 필요할 수 있는 영역의 개요입니다.

- **ESP32 펌웨어**: 상태 고착 방지, 재시도 로직, 상태 전이 보완
- **Windows 서버**: CDC 포트 재연결/재시도, 핸드셰이크 타임아웃 처리
- **Android 앱**: 모드 판정 타임아웃 조정, 재감지 트리거 로직
- **공통**: 각 구간의 로깅 강화 (재현 시 원인 특정을 위해)

> **⚠️ 펌웨어 수정 포함 가능**: 이 Phase는 ESP32 펌웨어 수정이 필요할 수 있습니다. 코드 수정 후 유저가 직접 빌드/플래시해야 합니다.

**검증**:
- [ ] 1단계 조사 완료: 각 후보 원인(A~F)에 대해 실제 원인 여부 판별 기록
- [ ] 서버 실행 중 앱 시작 시 STANDARD 모드로 감지되는지 20회 이상 반복 확인
- [ ] 핸드폰 재연결만으로 정상 감지 복구되는지 확인
- [ ] 보드 PC 재연결 없이도 정상 복구되는지 확인
- [ ] 서버 강제 종료 → 재시작 후 자동으로 STANDARD 복귀 확인
- [ ] 서버 미실행 시 ESSENTIAL 모드 진입이 기존과 동일한지 확인
- [ ] 수정 후 간헐적 실패가 재현되지 않음을 장시간(1시간+) 사용으로 확인

---

## Phase 4.5.13: 엣지 스와이프로 DPI·스크롤 속도·포인터 다이나믹스 조정

**개발 기간**: 1-1.5일

### 새 EdgeSwipeMode 항목

`EdgeSwipeOverlay.kt`의 `EdgeSwipeMode` enum에 3개 값 추가: `DPI`, `SCROLL_SPEED`, `DYNAMICS`

| 항목 | EdgeSwipeMode | 순환 동작 | 상태 위치 |
|-----|--------------|---------|---------|
| 마우스 감도 | `DPI` | `LOW → NORMAL → HIGH → LOW` | `TouchpadState.dpi` (`DpiLevel`) |
| 스크롤 속도 | `SCROLL_SPEED` | `SLOW → NORMAL → FAST → SLOW` | `TouchpadState.scrollSpeedLevel` (신규) |
| 포인터 다이나믹스 | `DYNAMICS` | 프리셋 인덱스 순환 | `TouchpadState.dynamicsPresetIndex` |

### 신규 상태: ScrollSpeedLevel

- `ScrollConstants.kt`에 `ScrollSpeedLevel` enum (`SLOW`, `NORMAL`, `FAST`) 및 각 배율 상수 추가
- `TouchpadState`에 `scrollSpeedLevel` 필드 추가 (기본값 `NORMAL`)
- 스크롤 델타에 `ScrollSpeedLevelConstants` 배율 적용 (Phase 4.5.7의 `InfiniteScrollDirectionBoost`와 독립적으로 먼저 적용)

### visibleModes 로직 변경

- `TouchpadWrapper.kt`의 `visibleModes` 빌드 블록에 `DPI`, `SCROLL_SPEED`, `DYNAMICS` 항상 추가

### applyEdgeModeToggle 확장

- `DPI`: `LOW → NORMAL → HIGH → LOW` 순환
- `SCROLL_SPEED`: `SLOW → NORMAL → FAST → SLOW` 순환
- `DYNAMICS`: `dynamicsPresetIndex` 순환 (전체 프리셋 수 기준 modulo)

### EdgeSwipeOverlay 팝업 표시

`EdgeSwipeOverlay.kt`에서 3개 신규 항목의 아이콘·라벨·보조 라벨(현재 값)을 추가합니다:

| EdgeSwipeMode | 아이콘 | 라벨 | 보조 라벨 (현재 값) |
|--------------|-------|-----|-----------------|
| `DPI` | `speed` | "DPI" | `LOW` / `NORMAL` / `HIGH` |
| `SCROLL_SPEED` | `swap_vert` | "스크롤 속도" | `느림` / `보통` / `빠름` |
| `DYNAMICS` | `tune` | "다이나믹스" | 프리셋 이름 또는 인덱스 |

현재 값이 팝업 항목 아래에 보조 라벨로 표시되어, 선택 전에 현재 설정을 확인할 수 있도록 합니다.

**수정 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/touchpad/EdgeSwipeOverlay.kt`
  — `EdgeSwipeMode` enum에 `DPI`, `SCROLL_SPEED`, `DYNAMICS` 추가
  — 각 신규 항목의 아이콘·라벨·보조 라벨 렌더링 추가
- `src/android/app/src/main/java/com/bridgeone/app/ui/common/ScrollConstants.kt`
  — `ScrollSpeedLevel` enum 및 `ScrollSpeedLevelConstants` 추가
- `src/android/app/src/main/java/com/bridgeone/app/ui/` (TouchpadState 정의 파일)
  — `TouchpadState`(또는 `PadModeState`)에 `scrollSpeedLevel` 필드 추가
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/TouchpadWrapper.kt`
  — `visibleModes`에 `DPI`, `SCROLL_SPEED`, `DYNAMICS` 추가
  — `applyEdgeModeToggle`에 3개 분기 추가
  — 스크롤 이벤트 처리(일반·무한)에서 `scrollSpeedLevel` 배율 적용

**검증**:
- [ ] 엣지 스와이프 팝업에 DPI·스크롤 속도·다이나믹스 항목이 표시됨 확인
- [ ] 각 항목에 현재 값이 보조 라벨로 표시됨 확인
- [ ] DPI 항목 선택 시 `LOW→NORMAL→HIGH→LOW` 순환 확인
- [ ] DPI 변경 후 터치패드 이동 시 PC 커서 속도 차이 확인
- [ ] 스크롤 속도 항목 선택 시 `SLOW→NORMAL→FAST→SLOW` 순환 확인
- [ ] FAST 스크롤 속도로 무한 스크롤 시 NORMAL 대비 2.5배 빠른 이동 확인
- [ ] 스크롤 속도 NORMAL 상태에서 Phase 4.5.7 방향별 비대칭 배율이 정상 동작 확인
- [ ] 다이나믹스 항목 선택 시 프리셋 인덱스 순환 확인
- [ ] 변경된 설정이 팝업 닫힘 후에도 유지 확인
- [ ] 엣지 스와이프 2단계 제스처(Phase 4.5.8)와 충돌 없음 확인
- [ ] Essential 모드에서 엣지 스와이프 차단(Phase 4.5.2) 후 DPI 등 신규 항목도 차단됨 확인

---

## Phase 4.5.14: 포인터 다이나믹스 커스텀 프리셋 그래프 편집기

**개발 기간**: 2-3일

---

### 개념: 두 가지 곡선

포인터 다이나믹스 알고리즘은 현재 단일 속도→배율 곡선만 사용합니다. 커스텀 프리셋은 두 개의 독립적인 곡선을 지원합니다:

| 곡선 | 의미 | 적용 조건 |
|-----|------|---------|
| **가속 곡선** (Acceleration Curve) | 손가락 속도가 빨라질 때 커서 배율이 얼마나 빠르게 올라가는가 | 현재 속도 > 이전 프레임 속도 |
| **감속 곡선** (Deceleration Curve) | 손가락이 느려질 때 커서 배율이 얼마나 천천히 내려오는가 | 현재 속도 ≤ 이전 프레임 속도 |

두 곡선이 다르면 **히스테리시스(이력 현상)** 효과가 생깁니다. 예를 들어 가속 곡선은 가파르게 올리고 감속 곡선은 완만하게 내리면, 손가락이 멈춰도 커서가 잠시 빠른 속도를 유지하다가 천천히 줄어드는 "여운" 효과를 낼 수 있습니다.

---

### 데이터 모델

- `CurveNode`: X축(손가락 속도 dp/ms), Y축(커서 배율)으로 구성된 꺾임점 데이터 클래스
- `CustomPointerDynamicsPreset`: id, name, accelerationCurve, decelerationCurve 포함
  - 첫 번째 노드 `(0f, 1.0f)` 및 마지막 노드 velocity는 고정 (삭제 불가)
  - 중간 노드 최대 개수 제한 (`CURVE_MAX_NODES - 2`)
  - 두 노드 사이 배율은 선형 보간

- `PointerDynamicsConstants.kt`에 `CurveEditorConstants` 객체 추가:
  - `CURVE_VELOCITY_MAX`, `CURVE_MULTIPLIER_MAX`, `CURVE_MAX_NODES`
  - `CURVE_MIN_VELOCITY_GAP`, `CURVE_SNAP_THRESHOLD_DP`, `CURVE_ADD_MIN_DP`

---

### 커스텀 프리셋 저장/관리

- 앱 내부 저장소(`{filesDir}/dynamics_presets.json`)에 JSON으로 직렬화하여 저장
- `DynamicsPresetPopup`에서 빌트인 프리셋 + 커스텀 프리셋 통합 그리드로 표시: `[빌트인 ...] [커스텀 ...] [+ 추가]`
- 빌트인 프리셋: 롱프레스 시 "미리보기"만 제공 (편집/삭제 불가)
- 커스텀 프리셋: 롱프레스 시 "편집" / "삭제" / "이름 변경" 제공
- `TouchpadState.dynamicsPresetIndex`는 통합 목록에서의 인덱스

---

### 그래프 편집기 UI (`DynamicsCurveEditor`)

**트리거**: `DynamicsPresetPopup`에서 "+" 버튼 탭 (신규 프리셋 생성) 또는 커스텀 프리셋 롱프레스 후 "편집" 선택

**화면 구조**:

```
┌──────────────────────────────────────────────────────┐
│  [← 취소]   커스텀 프리셋 편집   [저장 →]              │  ← 상단 바
├──────────────────────────────────────────────────────┤
│  프리셋 이름: [내 설정 1_____________]  [🖊]           │  ← 이름 입력 행
├──────────────────────────────────────────────────────┤
│  [● 가속 곡선]  ○ 감속 곡선                            │  ← 탭 전환
├──────────────────────────────────────────────────────┤
│                                                      │
│  6.0×│                              ●               │
│      │                         ╱                    │
│  4.0×│                    ╱                         │  ← 그래프 영역
│      │               ●─╱                            │
│  2.0×│          ╱                                   │
│      │     ●─╱                                      │
│  1.0×●────────────────────────────────────────────  │
│      0    1    2    3    4    5    6 (dp/ms)         │
│                                                      │
├──────────────────────────────────────────────────────┤
│  빈 곳 탭 = 노드 추가  |  노드 롱프레스 = 삭제         │  ← 조작 안내
└──────────────────────────────────────────────────────┘
```

#### 그래프 캔버스 (Canvas Composable)

- 배경: 격자 선 (연한 회색)
- 곡선: 노드 사이 꺾인 선 연결 (가속=연파랑, 감속=주황, 비활성 탭 곡선은 흐리게 참고 표시)
- 노드 원: 지름 16dp (드래그 중 24dp로 확대), 고정 노드(양 끝)는 드래그 불가
- X/Y 축 레이블 표시

#### 터치 인터랙션 (`pointerInput`)

- 기존 노드 근처 터치: 드래그(노드 이동) 또는 롱프레스(삭제 확인 다이얼로그)
- 빈 곳 탭: 노드 추가 (`CURVE_ADD_MIN_DP` 조건 확인 후)
- 드래그 제약: X 이동은 인접 노드 범위 내로 제한, Y 이동은 `1.0f ~ CURVE_MULTIPLIER_MAX` 클램프, 고정 노드는 X 이동 금지
- 햅틱: 경계 도달 시 가벼운 진동, 노드 추가/삭제 성공 시 명확한 진동

#### 탭 전환 (가속 / 감속)

- 탭 전환 시 두 곡선 모두 그래프에 표시 (비활성 곡선은 흐리게)
- 감속 탭에 "가속 곡선 복사" 버튼 제공

---

### 알고리즘 구현 (`DeltaCalculator.kt` 수정)

- `previousVelocityDpMs` 상태 추가 (히스테리시스 계산용)
- `applyCustomDynamics()`: 현재 속도와 이전 속도를 비교해 가속/감속 곡선 선택 후 배율 적용
- `interpolateCurve()`: 두 노드 사이 선형 보간으로 배율 계산

---

### 신규 파일

| 파일 | 위치 | 역할 |
|-----|------|------|
| `DynamicsCurveEditor.kt` | `ui/components/touchpad/` | 그래프 편집기 전체 Composable (Canvas + 탭 + 상단바) |
| `CustomPresetsRepository.kt` | `ui/common/` 또는 `data/` | JSON 직렬화/역직렬화, 저장/불러오기, CRUD |

### 수정 파일

- `src/android/app/src/main/java/com/bridgeone/app/ui/common/PointerDynamicsConstants.kt`
  — `CurveEditorConstants` 상수 객체 추가
  — `CurveNode`, `CustomPointerDynamicsPreset` 데이터 클래스 추가 (또는 `TouchpadMode.kt`에 추가)
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/touchpad/DynamicsPresetPopup.kt`
  — 커스텀 프리셋 목록 행 추가 (빌트인 + 커스텀 통합 그리드)
  — "+" 추가 버튼 셀 추가
  — 커스텀 프리셋 롱프레스 → 편집/삭제/이름변경 옵션 메뉴 추가
- `src/android/app/src/main/java/com/bridgeone/app/ui/utils/DeltaCalculator.kt`
  — `interpolateCurve()` 함수 추가
  — `applyCustomDynamics()` 함수 추가
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/TouchpadWrapper.kt`
  — 커스텀 프리셋 인덱스에 해당할 때 `applyCustomDynamics()` 호출 분기 추가
  — `previousVelocityDpMs` 상태 추가 (히스테리시스 계산용)

**검증**:
- [ ] "+" 버튼으로 새 커스텀 프리셋 생성 및 그래프 편집기 진입 확인
- [ ] 빈 곳 탭으로 노드 추가, 드래그로 노드 이동, 롱프레스로 노드 삭제 동작 확인
- [ ] 고정 노드(첫 번째, 마지막)는 X 이동이 불가하고 롱프레스 삭제도 불가함 확인
- [ ] 가속 곡선 / 감속 곡선 탭 전환 시 각 곡선이 독립적으로 편집됨 확인
- [ ] "감속 곡선 → 가속 곡선으로 복사" 버튼이 올바르게 동작 확인
- [ ] 저장 후 앱 종료·재시작 시 커스텀 프리셋이 그대로 복원되는지 확인
- [ ] 커스텀 프리셋 적용 후 터치패드 사용 시 손가락 가속/감속에 따라 다른 배율 곡선이 적용됨 확인
- [ ] 빌트인 프리셋 롱프레스 시 편집/삭제 옵션이 나타나지 않음 확인
- [ ] 커스텀 프리셋이 엣지 스와이프 다이나믹스 순환(Phase 4.5.12)에 통합 목록으로 올바르게 포함됨 확인
- [ ] 노드 수가 `CURVE_MAX_NODES`에 도달하면 추가 탭이 무시(또는 안내 표시)되는지 확인
- [ ] 프리셋 이름 최대 12자 제한 및 빈 이름 방지 확인

---

## Phase 4.5 완료 후 Phase 4.4.9 검증 항목 영향

Phase 4.5 수정 완료 후 아래 Phase 4.4.9 검증 항목을 재검증해야 합니다:

| 검증 항목 | Phase 4.5 하위 Phase | 재검증 필요 |
|----------|---------------------|------------|
| A. 앱 연결 상태 표시가 실제 연결 여부와 일치 | 4.5.11 | ✅ |
| C. 우클릭 모드 전환 후 탭 → 우클릭 신호 전달 | 4.5.1 | ✅ |
| D. 무한 스크롤 연속 패킷 전송 | 4.5.5, 4.5.6, 4.5.7 | ✅ |
| D. 무한 스크롤 속도 비례 | 4.5.7 | ✅ |
| H. 엣지 스와이프로 우클릭 모드 전환 후 탭 | 4.5.1 | ✅ |
