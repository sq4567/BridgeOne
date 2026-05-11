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

> **⚠️ 실제 구현 (계획과 다름)**: cap=5 per MOVE 이벤트로는 120Hz 기기에서 최대 600프레임/초까지 가능해 phantom 입력이 재현됨. 이벤트 단위 제한이 아닌 **시간 단위 rate limiter**가 필요했음.
> - `SCROLL_MAX_FRAMES_PER_EVENT` = **5 → 3** 축소 (2차 안전장치)
> - `SCROLL_FRAME_MIN_INTERVAL_MS = 8L` 추가 → 초당 최대 125프레임 하드 상한. 게이트 미충족 시 나머지 `scrollAccum` 다음 이벤트 이월.
> - `TouchpadWrapper.kt`에 `lastScrollFrameSentMs` remember 상태 추가 (`mutableLongStateOf`)
> - while 루프 시작부에 시간 게이트 (`nowMs - lastScrollFrameSentMs < SCROLL_FRAME_MIN_INTERVAL_MS`) 추가. UNDECIDED 이터레이션은 타이머 갱신 안 함.

**검증**:
- [x] 무한 스크롤 모드에서 최대한 빠르게 스크롤 → 경보음 없고 phantom 키 입력 없음 확인
- [x] 동일 조건에서 브라우저/앱이 예상치 않게 열리지 않음 확인
- [x] 빠른 스크롤에서도 스크롤 속도·거리가 느린 스크롤 대비 비례적으로 유지되는지 확인 (거리 손실 없음)
- [x] 일반 스크롤 모드의 고속 스크롤에서도 동일 현상 없음 확인

---

## Phase 4.5.8: 무한 스크롤 방향별 속도 비대칭 보정 옵션

> **⚠️ Phase 4.5.7 변경사항**: `ScrollConstants.kt`에 `SCROLL_FRAME_MIN_INTERVAL_MS = 8L`, `SCROLL_MAX_FRAMES_PER_EVENT = 3` 추가됨.
> `TouchpadWrapper.kt` remember 블록에 `lastScrollFrameSentMs` (`mutableLongStateOf`) 상태 추가됨.
> MOVE 이벤트 스크롤 while 루프에 시간 게이트 로직 추가됨 — 방향별 배율 적용 시 이 게이트를 우회하거나 제거하지 말 것.

**개발 기간**: 0.5-1일

**작업 내용**:
- `ScrollConstants.kt`에 `ScrollDirectionBoost` 상수 객체 추가 (UP/DOWN/LEFT/RIGHT 방향별 배율, 기본값 `1.0f`)
- `TouchpadWrapper.kt` MOVE 이벤트 scrollAccum 누적 및 무한 스크롤 관성 초기 속도 계산에 방향별 배율 적용
- 적용 범위: 일반·무한 스크롤 공통 적용, 커서 이동 영향 없음

**수정 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/common/ScrollConstants.kt`
  — `ScrollDirectionBoost` 상수 추가, `SCROLL_GUIDELINE_STEP_DP` 삭제 (`SCROLL_UNIT_DISTANCE_DP`로 통합)
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/TouchpadWrapper.kt`
  — MOVE 이벤트 scrollAccum 누적에 방향별 배율 적용 (일반·무한 스크롤 공통)
  — 무한 스크롤 가이드라인 연속 추적에도 배율 반영
  — 관성 초기 속도 계산에 방향별 배율 적용

**검증**:
- [x] `DOWN_MULTIPLIER = 1.5f` 설정 후 아래로 스와이프 시 기존 대비 스크롤 속도 1.5배 증가 확인
- [x] `UP_MULTIPLIER = 1.0f` 유지 시 위로 스와이프 속도 변화 없음 확인
- [x] 배율 변경 시 관성 단계에도 동일하게 반영되는지 확인
- [x] 일반 스크롤 모드에도 배율 적용 확인
- [x] 양방향 모두 `1.0f`일 때 기존 동작과 완전 동일한지 확인

> **실제 구현 (계획과 다름)**: 계획은 무한 스크롤 전용 `InfiniteScrollDirectionBoost`(UP/DOWN)이었으나 다음과 같이 확장됨.
> - `ScrollDirectionBoost`로 이름 변경, LEFT/RIGHT 배율 추가 (4방향 지원)
> - 일반·무한 스크롤 공통 적용으로 범위 확대 (커서 이동에는 미적용)
> - 무한 스크롤 드래그 중 가이드라인도 배율 반영 (`guidelineTarget += axisDeltaDp * dirMult`) — 스크롤 효과를 시각적으로 일관되게 표시
> - `SCROLL_GUIDELINE_STEP_DP` 삭제 — `SCROLL_UNIT_DISTANCE_DP`와 항상 동일해야 자연스러우므로 통합

---

## Phase 4.5.9: 엣지 스와이프 제스처 UX 개선

**개발 기간**: 1일

**에뮬레이터 호환성**: 전체 에뮬레이터에서 개발 가능.

**팝업 내용**: 스와이프 모드 / 직접 터치 모드 중 하나를 선택하는 모드 선택기 화면. 현재 모드가 하이라이트된 상태로 표시됨.

**최종 결정**: 실사용 테스트 후 2단계 방식으로 완전 대체. `USE_TWO_STEP_EDGE_SWIPE` 상수 및 1단계 코드 삭제.

### 동작 흐름 (2단계, 최종)

| 단계 | 동작 |
|------|------|
| ① | 엣지에서 안쪽으로 스와이프 |
| ② | 트리거 거리 도달 → 모드 선택기 팝업 등장 (스와이프 모드 / 직접 터치 모드) |
| ③ | **손을 뗌 → 팝업 고정 (메뉴 유지)** |
| ④ | 화면 안쪽 어디서나 스와이프로 항목 탐색, 끝에서 경계 피드백 (빨간 테두리 + 흔들림 + 햅틱) |
| ⑤ | **탭으로 확정** |
| 취소 (고정 전) | 진입 엣지로 되돌리기 |
| 취소 (고정 후) | 안쪽→바깥쪽 스와이프 (`TWO_STEP_CANCEL_SWIPE_DP = 60f`) |

> **추가 구현 사항**:
> - 트리거 거리 도달(모드 선택기 등장) 시 산봉우리 애니메이션이 진입 엣지 방향으로 수축하며 소멸 (`LaunchedEffect(isModeSelecting)`)
> - 모드 선택기 취소 시 스와이프 모드 UI가 잠깐 보이던 버그 수정 (`isPopupShowing` 플래그 추가)

**검증**:
- [x] 엣지 스와이프 → 트리거 → 손 뗌 → 팝업이 닫히지 않고 고정 확인
- [x] 고정 상태에서 스와이프로 항목 이동 시 선택 항목 갱신 확인
- [x] 첫 번째 항목에서 이전(↑·←) 시도 시 끝임을 표시 확인
- [x] 마지막 항목에서 다음(↓·→) 시도 시 끝임을 표시 확인
- [x] 고정 상태에서 탭 → 모드 적용 + 팝업 닫힘 확인
- [x] 고정 상태에서 바깥쪽 스와이프 → 취소·팝업 닫힘 확인 (모드 미변경)
- [x] 고정 상태에서 단순히 손만 뗌(탭 거리 초과, 바깥 스와이프 미달) → 팝업 유지 확인
- [x] 팝업 고정 전 취소(진입 엣지로 되돌리기) → 기존과 동일하게 동작 확인
- [x] 안내 텍스트가 고정 전/후 올바르게 전환되는지 확인

---

## Phase 4.5.10: 엣지 스와이프 UX 개선 (햅틱·클릭 차단·존 힌트·취소 확장)

> **⚠️ Phase 4.5.9 변경사항**: 2단계 방식으로 고정됨. `showEdgePopup`은 모드 선택기에서 탭 확정 후에야 `true`로 전환됨.
> `EdgeSwipeOverlay` 시그니처에 `isPopupPinned`, `pinnedBorderColor`, `pinnedShakeOffsetDp` 파라미터 추가됨.

**개발 기간**: 0.5일 미만

**작업 내용** (계획 대비 확장 구현):
- **햅틱**: DOWN 이벤트에서 엣지 감지(`detectedEntryEdge != null`) 시 `CLOCK_TICK` 1회 — `showEdgePopup` 전환 시점이 아닌 손가락이 엣지 존에 닿는 순간에 발생
- **클릭 차단**: UP 이벤트에서 `isEdgeCandidate == true`이면 클릭 판정 차단 (엣지 존은 클릭 불가 영역)
- **엣지 존 힌트 오버레이**: 엣지 존 경계를 테두리 색으로 시각화
  - 평상시 alpha 6%, 손가락 진입 시 20%로 강조 (150ms 전환)
  - 색상: LEFT → `animatedLeftColor`, RIGHT → `animatedRightColor`, TOP/BOTTOM → lerp 50%
  - 버튼보다 아래 레이어로 렌더링, Essential 모드 제외
  - 상수: `EDGE_ZONE_HINT_BASE_ALPHA`, `EDGE_ZONE_HINT_ACTIVE_ALPHA`, `EDGE_ZONE_HINT_ANIM_MS` → `EdgeSwipeConstants`
- **모드 선택기 취소 확장**: 진입 엣지 방향뿐 아니라 임의 방향으로 엣지 존 도달 시 취소
  - 1단계(손 올린 채 MOVE): `currentInward <= cancelThresholdPx` OR `isNearAnyEdge`
  - 2단계(손 뗀 후 새 제스처): `outwardDist >= twoStepCancelSwipePx` AND `isNearAnyEdge`

**수정 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/TouchpadWrapper.kt`
- `src/android/app/src/main/java/com/bridgeone/app/ui/common/ScrollConstants.kt` — `EdgeSwipeConstants`에 힌트 알파 상수 3개 추가

**검증**:
- [x] 엣지 스와이프로 산봉우리 팝업이 등장하는 순간 진동 발생 확인
- [x] 팝업 닫힘·취소 시 추가 진동 없음 확인 (등장 시 1회만)
- [x] 기기 진동 설정이 꺼진 상태에서 앱 크래시 없음 확인
- [x] Essential 모드(Phase 4.5.2 적용 후)에서는 팝업도 진동도 발생하지 않음 확인

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
- [x] LEFT 엣지 진입 후 손가락을 위아래로 움직일 때 산봉우리 발이 진입점에 고정되고 피크만 기울어지는지 확인
- [x] TOP / BOTTOM / RIGHT 엣지에서도 동일하게 동작 확인
- [x] 손가락을 엣지 따라 크게 이동해도 베이스가 이탈하지 않는지 확인
- [x] 피크가 지나치게 기울어져 모양이 무너지는 경우 없는지 확인 (베이스에서 피크까지 거리 상한 필요 시 `fingerAlongEdgePx.coerceIn()` 적용 검토)
- [x] `resetEdgeSwipeState()` 이후 다음 제스처 시 베이스 위치가 올바르게 초기화되는지 확인
- [x] 팝업 등장 및 소멸 애니메이션(Phase 4.4.7)과 시각적으로 어색함 없이 연동되는지 확인

> **구현 노트**: `entryAlongEdgePx` remember 상태를 추가. DOWN 이벤트에서 `edgeStartAlongPx`로 초기화, 손 뗌 시 3곳(isModeSelecting 고정, showEdgePopup 유지, 후보 취소)에서 `0f`로 리셋. `EdgeBumpOverlay` 시그니처에 `entryAlongEdgePx` 파라미터 추가, 4개 엣지 분기에서 베이스(발) 좌표는 `entryAlongEdgePx` 기준, 피크 및 glow는 `fingerAlongEdgePx`(현재 손가락) 기준으로 분리.

---

## Phase 4.5.12: 앱 중복 실행 방지 (백그라운드 실행 중 보드 재연결)

**개발 기간**: 0.5일 미만

**증상**:
- 앱이 백그라운드에서 실행 중인 상태에서 보드를 핸드폰에 재연결하면 앱이 포그라운드로 복귀하지 않아야 하는데, 앱이 포그라운드로 나타남

**작업 내용**:
- `AndroidManifest.xml`의 `MainActivity`에서 `USB_DEVICE_ATTACHED` intent-filter 및 `meta-data` 제거
- 보드 재연결 이벤트는 이미 정적 등록된 `UsbDeviceDetectionReceiver`가 처리하므로 기능 유지
- 앱이 완전히 종료된 상태에서 보드 연결 시에는 자동 시작되지 않음 (직접 앱 실행 필요)

> **실제 구현 (계획과 다름)**: 원래 계획(`singleTop` 추가 + `onNewIntent()` 추가)은 포그라운드 전환을 막지 못함. `singleTask`는 이미 설정되어 있었고 `onNewIntent()`도 이미 구현되어 있었음. 근본 원인은 Activity의 `USB_DEVICE_ATTACHED` intent-filter 자체이므로 제거하는 방식으로 변경.

**수정 파일**:
- `src/android/app/src/main/AndroidManifest.xml`
  — `MainActivity`의 `USB_DEVICE_ATTACHED` intent-filter 및 `meta-data` 제거

**검증**:
- [x] 앱 백그라운드 실행 중 보드 재연결 시 앱이 포그라운드로 나타나지 않고 아무 반응 없음 확인
- [x] 재연결 후 USB 통신이 정상 재개되는지 확인

---

## Phase 4.5.13: Windows 서버 "미연결" 고착 수정

**개발 기간**: 0.5일

**증상**:
- 하드웨어(ESP32-S3)가 PC에 연결되어 있고 Android 앱으로 마우스 이동이 정상 작동함
- 그럼에도 Windows 서버 UI가 "미연결" 상태로 고착되고 "연결" 버튼을 눌러도 변화 없음
- 서버를 재시작해도 동일하게 "미연결"

**원인**:

`CdcConnectionService.cs`의 WMI 장치 검색 쿼리가 `VID_303A&PID_4001`(Espressif 기본값)을 찾도록 하드코딩되어 있으나, 펌웨어 VID/PID가 실제로는 `0x04D9 / 0x0024`(Holtek)으로 변경된 상태다.

```c
// usb_descriptors.h (실제 펌웨어)
#define USB_VID  0x04D9   // Holtek (안티치트 우회용으로 변경됨)
#define USB_PID  0x0024
```

```cs
// CdcConnectionService.cs (현재 서버, 미갱신 상태)
private const ushort TargetVid = 0x303A;   // ← 잘못된 값
private const ushort TargetPid = 0x4001;   // ← 잘못된 값
```

WMI 쿼리 `PNPDeviceID LIKE '%VID_303A&PID_4001%'`이 아무 장치도 반환하지 않아 `FindBridgeOneDevice()` → `null` → `State = Disconnected`.

**추가 확인 필요**:
- Holtek VID/PID로 Windows가 CDC 인터페이스에 `usbser.sys` 드라이버를 자동 설치했는지 여부
  - 설치됐다면: VID/PID 상수만 수정하면 해결
  - 설치 안 됐다면: Windows 장치 관리자에서 "알 수 없는 장치"로 표시될 수 있으며, 별도 INF 파일 또는 CDC 인터페이스 클래스코드 재검토 필요
- 실제 장치 관리자에서 COM 포트가 어떤 VID/PID로 표시되는지 확인

**작업 내용**:

1. `CdcConnectionService.cs`의 VID/PID 상수를 실제 펌웨어 값으로 수정:
   ```cs
   private const ushort TargetVid = 0x04D9;  // Holtek (안티치트 우회)
   private const ushort TargetPid = 0x0024;
   ```
2. (조건부) CDC COM 포트가 장치 관리자에 나타나지 않는 경우, WMI 쿼리 전략 변경 검토:
   - VID/PID 대신 제품 문자열 기반 탐색
   - 또는 `iManufacturer`/`iProduct` String Descriptor를 "BridgeOne" 고유값으로 설정하여 식별

**수정 파일**:
- `src/windows/BridgeOne/Services/CdcConnectionService.cs`
  — `TargetVid = 0x04D9`, `TargetPid = 0x0024`으로 변경

**검증**:
- [x] 장치 관리자에서 BridgeOne ESP32 관련 COM 포트 확인 (VID/PID 표시 확인)
  > `USB\VID_04D9&PID_0024&MI_02` → "USB 직렬 장치(COM10)", Status: OK, usbser.sys 정상 설치 확인
- [x] VID/PID 수정 후 서버 실행 시 "연결 중..." → "연결됨" 상태 전환 확인
- [x] 핸드셰이크 완료 및 Keep-alive 정상 동작 확인
- [x] USB 재연결(핫플러그) 시 자동 재연결 확인

---

## Phase 4.5.14: Windows 서버 감지 실패 — 종합 원인 조사 및 수정

**개발 기간**: 2-3일 (조사 포함)

**증상**:

증상 1 — 간헐적 실패:
- ESP32-S3가 PC에 연결되어 있고 Windows 서버가 실행 중임에도, 앱이 ESSENTIAL 모드로 진입
- 항상 발생하는 것은 아니고 **간헐적**
- 아래 방법 중 하나로 복구되는 경우가 있음 (일관되지 않음):
  - **보드를 PC에서 분리 후 재연결** → 복구됨
  - **핸드폰 쪽 USB 케이블 분리 후 재연결** → 복구되는 경우도 있음
  - **핸드폰 재연결만으로는 복구 안 되는 경우**도 있음

증상 2 — 리빌드 후 **항상** 재현:
- Android Studio에서 앱이 열려 있는 상태로 리빌드하면, 앱이 종료됐다가 재시작되는데 이때 **매번** ESSENTIAL 모드로 진입
- 케이블은 그대로이고 서버도 계속 실행 중인 상태
- 리빌드 직후 앱 프로세스가 재시작될 때만 발생 (USB 연결 상태는 변경 없음)
- **복구 방법**: 핸드폰 쪽 USB 케이블을 뺐다가 다시 꽂으면 정상적으로 STANDARD 모드로 진입
- **재현 조건이 명확**하므로, 간헐적 실패보다 원인 특정이 용이한 단서가 될 수 있음

### 조사 결과 (코드 분석 완료)

- **A (ESP32 고착)**: `connection_state.c` 분석 결과, IDLE 상태에서의 모드 쿼리(0xFF 0x01)는 `handle_uart_query()`가 별도 처리하여 connection_state와 무관하게 즉시 응답. 고착 아님
- **B (CDC 포트)**: Phase 4.5.13에서 해결됨. 잔여 케이스 없음 확인
- **C (타임아웃)**: E/G에서 UART RX가 비활성화되면 5초 내 응답 불가로 ESSENTIAL 진입. 이 자체는 원인이 아닌 결과
- **D (열거 순서)**: 주된 원인 아님
- **E (핵심 원인)**: `openPort()` 시 DTR/RTS OFF → 50ms → ON으로 토글하지만 ON 후 안정화 대기가 없음. CH343P RX→USB 방향이 완전히 활성화되기 전에 쿼리가 전송될 수 있음
- **F (재감지)**: `pollingThread` 2초 주기 재시도 있지만, CH343P RX가 비활성화된 경우 응답 수신 자체가 불가 → 자동 복구 불가
- **G (핵심 원인)**: 앱 재시작 시 USB 인터페이스만 re-claim하므로 CH343P가 이전 세션의 RX 비활성화 상태를 유지. 케이블 재연결 시 USB 재열거로 CH343P가 완전 초기화되는 것과 차이. 이것이 증상 2(리빌드 후 항상 ESSENTIAL)를 설명하는 근본 원인

**수정 내용** (`UsbSerialManager.kt`):

1. DTR/RTS 토글 강화: OFF → 150ms → ON → 100ms 안정화 대기 (`DTR_RTS_OFF_WAIT_MS`, `DTR_RTS_ON_STABILIZE_MS`)
2. `lastModeResponseMs` 추가: 마지막 모드 응답 수신 시각 추적
3. `pollingThread`에서 10초 이상 응답 없으면 `reinitializeUart()` 호출 (DTR/RTS 재토글로 CH343P 소프트웨어 재초기화)
4. `receiverThread`에서 모드 응답 수신 시 `lastModeResponseMs` 갱신

**수정 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/usb/UsbSerialManager.kt`

**검증**:
- [x] **[증상 2]** Android Studio 리빌드 후 앱 재시작 시 매번 STANDARD 모드로 진입 확인 (5회 이상)
- [x] 서버 실행 중 앱 시작 시 STANDARD 모드로 감지되는지 20회 이상 반복 확인
- [x] 핸드폰 재연결만으로 정상 감지 복구되는지 확인
- [x] 케이블 재연결 없이 10초 내 자동 복구되는지 확인 (reinitializeUart 동작 검증)
- [x] 서버 강제 종료 → 재시작 후 자동으로 STANDARD 복귀 확인
- [x] 서버 미실행 시 ESSENTIAL 모드 진입이 기존과 동일한지 확인
- [x] 수정 후 간헐적 실패가 재현되지 않음을 장시간(1시간+) 사용으로 확인

---

## Phase 4.5.15: 엣지 스와이프로 DPI·스크롤 속도·포인터 다이나믹스 조정

**개발 기간**: 1-1.5일

**작업 내용**:
- `EdgeSwipeOverlay.kt`의 `EdgeSwipeMode` enum에 `DPI`, `SCROLL_SPEED`, `DYNAMICS` 3개 값 추가

  | 항목 | EdgeSwipeMode | 순환 동작 | 상태 위치 |
  |-----|--------------|---------|---------|
  | 마우스 감도 | `DPI` | `LOW → NORMAL → HIGH → LOW` | `TouchpadState.dpi` (`DpiLevel`) |
  | 스크롤 속도 | `SCROLL_SPEED` | `SLOW → NORMAL → FAST → SLOW` | `TouchpadState.scrollSpeedLevel` (신규) |
  | 포인터 다이나믹스 | `DYNAMICS` | 프리셋 인덱스 순환 | `TouchpadState.dynamicsPresetIndex` |

- `ScrollConstants.kt`에 `ScrollSpeedLevel` enum (`SLOW`, `NORMAL`, `FAST`) 및 각 배율 상수 추가
- `TouchpadState`에 `scrollSpeedLevel` 필드 추가 (기본값 `NORMAL`)
- 스크롤 델타에 `ScrollSpeedLevelConstants` 배율 적용 (Phase 4.5.7의 `InfiniteScrollDirectionBoost`와 독립적으로 먼저 적용)
- `TouchpadWrapper.kt`의 `visibleModes` 빌드 블록에 `DPI`, `SCROLL_SPEED`, `DYNAMICS` 항상 추가
- `applyEdgeModeToggle`에 3개 분기 추가: `DPI`(`LOW→NORMAL→HIGH→LOW`), `SCROLL_SPEED`(`SLOW→NORMAL→FAST→SLOW`), `DYNAMICS`(전체 프리셋 수 기준 modulo 순환)
- `EdgeSwipeOverlay.kt`에서 3개 신규 항목의 아이콘·라벨·보조 라벨(현재 값) 추가

  | EdgeSwipeMode | 아이콘 | 라벨 | 보조 라벨 (현재 값) |
  |--------------|-------|-----|-----------------|
  | `DPI` | `speed` | "DPI" | `LOW` / `NORMAL` / `HIGH` |
  | `SCROLL_SPEED` | `swap_vert` | "스크롤 속도" | `느림` / `보통` / `빠름` |
  | `DYNAMICS` | `tune` | "다이나믹스" | 프리셋 이름 또는 인덱스 |

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
- [x] 엣지 스와이프 팝업에 DPI·스크롤 속도·다이나믹스 항목이 표시됨 확인
- [x] 각 항목에 현재 값이 보조 라벨로 표시됨 확인
- [x] DPI 항목 선택 시 `LOW→NORMAL→HIGH→LOW` 순환 확인
- [x] DPI 변경 후 터치패드 이동 시 PC 커서 속도 차이 확인
- [x] 스크롤 속도 항목 선택 시 `SLOW→NORMAL→FAST→SLOW` 순환 확인
- [x] FAST 스크롤 속도로 무한 스크롤 시 NORMAL 대비 2.5배 빠른 이동 확인
- [x] 스크롤 속도 NORMAL 상태에서 Phase 4.5.7 방향별 비대칭 배율이 정상 동작 확인
- [x] 다이나믹스 항목 선택 시 프리셋 인덱스 순환 확인
- [x] 변경된 설정이 팝업 닫힘 후에도 유지 확인
- [x] 엣지 스와이프 2단계 제스처(Phase 4.5.8)와 충돌 없음 확인
- [x] Essential 모드에서 엣지 스와이프 차단(Phase 4.5.2) 후 DPI 등 신규 항목도 차단됨 확인

> **실제 구현 (계획과 다름)**:
> - `ScrollSpeedLevel` 신규 enum 및 `TouchpadState.scrollSpeedLevel` 신규 필드 미추가. 기존 `TouchpadState.scrollSensitivity: ScrollSensitivity`가 동일 역할(SLOW/NORMAL/FAST 배율)을 이미 수행하고 스크롤 델타에도 적용되어 있어 재사용.
> - `SCROLL_SPEED` 항목 가시성 조건 추가: 스크롤 ON 시에만 표시. `DPI` 항목도 스크롤 ON 시 숨김 (CLICK·MOVE와 동일 조건). 이는 계획에 없던 가시성 규칙.
> - 스와이프 모드 팝업 레이아웃 반응형 개선 (계획 외 추가 구현): `BoxWithConstraints`로 터치패드 크기 측정 → 제어 버튼 높이(height×15%, 48~72dp 클램프)만큼 상단 padding 추가 → 아이템 크기를 너비·높이 기반으로 동적 계산(52~80dp 클램프). `EdgeSwipeModeItem`에 `itemSize: Dp` 파라미터 추가, 아이콘·폰트 크기 비례 조정.
> - `FlowRow`(실험적 API) → 명시적 `chunked(2)` 기반 Row+Column 2열 그리드로 교체. `maxAnimItems` 9로 증가.
> - `ModeDisplayInfo`(private) 에 `imageVector: ImageVector? = null` 필드 추가 — Material Icons 직접 사용 모드(DPI·스크롤 속도·다이나믹스)를 위한 내부 변경.

---

## Phase 4.5.16: 포인터 다이나믹스 커스텀 프리셋 그래프 편집기

> **⚠️ Phase 4.5.15 변경사항**: `EdgeSwipeMode.DYNAMICS`의 `applyEdgeModeToggle` 분기가 `DYNAMICS_PRESETS.size`를 기준으로 modulo 순환함. 커스텀 프리셋을 통합 목록에 추가하면 이 분기를 통합 목록 크기 기준으로 업데이트해야 함.

**개발 기간**: 2-3일

**개념: 두 가지 곡선**

포인터 다이나믹스 알고리즘은 현재 단일 속도→배율 곡선만 사용합니다. 커스텀 프리셋은 두 개의 독립적인 곡선을 지원합니다:

| 곡선 | 의미 | 적용 조건 |
|-----|------|---------|
| **가속 곡선** (Acceleration Curve) | 손가락 속도가 빨라질 때 커서 배율이 얼마나 빠르게 올라가는가 | 현재 속도 > 이전 프레임 속도 |
| **감속 곡선** (Deceleration Curve) | 손가락이 느려질 때 커서 배율이 얼마나 천천히 내려오는가 | 현재 속도 ≤ 이전 프레임 속도 |

두 곡선이 다르면 **히스테리시스(이력 현상)** 효과가 생깁니다. 예를 들어 가속 곡선은 가파르게 올리고 감속 곡선은 완만하게 내리면, 손가락이 멈춰도 커서가 잠시 빠른 속도를 유지하다가 천천히 줄어드는 "여운" 효과를 낼 수 있습니다.

**데이터 모델**:

- `CurveNode`: X축(손가락 속도 dp/ms), Y축(커서 배율)으로 구성된 꺾임점 데이터 클래스
- `CustomPointerDynamicsPreset`: id, name, accelerationCurve, decelerationCurve 포함
  - 첫 번째 노드 `(0f, 1.0f)` 및 마지막 노드 velocity는 고정 (삭제 불가)
  - 중간 노드 최대 개수 제한 (`CURVE_MAX_NODES - 2`)
  - 두 노드 사이 배율은 선형 보간
- `PointerDynamicsConstants.kt`에 `CurveEditorConstants` 객체 추가:
  - `CURVE_VELOCITY_MAX`, `CURVE_MULTIPLIER_MAX`, `CURVE_MAX_NODES`
  - `CURVE_MIN_VELOCITY_GAP`, `CURVE_SNAP_THRESHOLD_DP`, `CURVE_ADD_MIN_DP`

**커스텀 프리셋 저장/관리**:

- 앱 내부 저장소(`{filesDir}/dynamics_presets.json`)에 JSON으로 직렬화하여 저장
- `DynamicsPresetPopup`에서 빌트인 프리셋 + 커스텀 프리셋 통합 그리드로 표시: `[빌트인 ...] [커스텀 ...] [+ 추가]`
- 빌트인 프리셋: 롱프레스 시 "미리보기"만 제공 (편집/삭제 불가)
- 커스텀 프리셋: 롱프레스 시 "편집" / "삭제" / "이름 변경" 제공
- `TouchpadState.dynamicsPresetIndex`는 통합 목록에서의 인덱스

**그래프 편집기 UI (`DynamicsCurveEditor`)**:

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

**그래프 캔버스 (Canvas Composable)**:

- 배경: 격자 선 (연한 회색)
- 곡선: 노드 사이 꺾인 선 연결 (가속=연파랑, 감속=주황, 비활성 탭 곡선은 흐리게 참고 표시)
- 노드 원: 지름 16dp (드래그 중 24dp로 확대), 고정 노드(양 끝)는 드래그 불가
- X/Y 축 레이블 표시

**터치 인터랙션 (`pointerInput`)**:

- 기존 노드 근처 터치: 드래그(노드 이동) 또는 롱프레스(삭제 확인 다이얼로그)
- 빈 곳 탭: 노드 추가 (`CURVE_ADD_MIN_DP` 조건 확인 후)
- 드래그 제약: X 이동은 인접 노드 범위 내로 제한, Y 이동은 `1.0f ~ CURVE_MULTIPLIER_MAX` 클램프, 고정 노드는 X 이동 금지
- 햅틱: 경계 도달 시 가벼운 진동, 노드 추가/삭제 성공 시 명확한 진동

**탭 전환 (가속 / 감속)**:

- 탭 전환 시 두 곡선 모두 그래프에 표시 (비활성 곡선은 흐리게)
- 감속 탭에 "가속 곡선 복사" 버튼 제공

**알고리즘 구현 (`DeltaCalculator.kt` 수정)**:

- `previousVelocityDpMs` 상태 추가 (히스테리시스 계산용)
- `applyCustomDynamics()`: 현재 속도와 이전 속도를 비교해 가속/감속 곡선 선택 후 배율 적용
- `interpolateCurve()`: 두 노드 사이 선형 보간으로 배율 계산

**신규 파일**:

| 파일 | 위치 | 역할 |
|-----|------|------|
| `DynamicsCurveEditor.kt` | `ui/components/touchpad/` | 그래프 편집기 전체 Composable (Canvas + 탭 + 상단바) |
| `CustomPresetsRepository.kt` | `ui/common/` 또는 `data/` | JSON 직렬화/역직렬화, 저장/불러오기, CRUD |

**수정 파일**:

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
- [x] "+" 버튼으로 새 커스텀 프리셋 생성 및 그래프 편집기 진입 확인
- [x] 빈 곳 탭으로 노드 추가, 드래그로 노드 이동, 롱프레스로 노드 삭제 동작 확인
- [x] 고정 노드(첫 번째, 마지막)는 X 이동이 불가하고 롱프레스 삭제도 불가함 확인
- [x] 가속 곡선 / 감속 곡선 탭 전환 시 각 곡선이 독립적으로 편집됨 확인
- [x] "가속 곡선 복사" 버튼이 올바르게 동작 확인
- [x] 저장 후 앱 종료·재시작 시 커스텀 프리셋이 그대로 복원되는지 확인
- [x] 커스텀 프리셋 적용 후 터치패드 사용 시 손가락 가속/감속에 따라 다른 배율 곡선이 적용됨 확인
- [x] 빌트인 프리셋 롱프레스 시 편집/삭제 옵션이 나타나지 않음 확인
- [x] 커스텀 프리셋이 엣지 스와이프 다이나믹스 순환(Phase 4.5.15)에 통합 목록으로 올바르게 포함됨 확인
- [x] 노드 수가 `CURVE_MAX_NODES`에 도달하면 추가 탭이 무시되는지 확인
- [x] 프리셋 이름 최대 12자 제한 및 빈 이름 방지 확인

> **실제 구현 노트**:
> - `CustomPresetsRepository.kt` 신규 — `{filesDir}/dynamics_presets.json` JSON 저장. `org.json` 기본 API 사용 (별도 라이브러리 불필요). 파일 미존재 시(최초 실행) `CUSTOM_PRESET_TEMPLATES` 3개를 자동 저장 후 반환.
> - `DynamicsCurveEditor.kt` 신규 — 전체 화면 오버레이 Composable. Canvas 그래프 + 탭 전환(가속/감속) + 이름 입력 + 설명 입력 + 아이콘 선택 + 템플릿 불러오기 + 조작 안내. `existingPresets` 파라미터로 중복 이름 방지 및 기본 이름 자동 증가("내 설정 1", "내 설정 2" …).
> - `DynamicsPresetButton.kt` 추가 수정 — `customPresets` 파라미터 추가. 커스텀 인덱스 시 `iconKey` 있으면 아이콘, 없으면 이름 첫 2자 표시, 탭 순환 범위 `totalPresets`로 확장.
> - `DynamicsPresetPopup.kt` 전면 재작성 — 빌트인+커스텀+"+버튼" 단일 `chunked(3)` 통합 그리드. `PopupPhase` 3단계(GRID → CONFIRM → DELETE_CONFIRM). 롱프레스 삭제 제거 → CONFIRM 화면에서 [적용/취소/편집/삭제] 4개 옵션으로 대체. DELETE_CONFIRM은 별도 팝업이 아닌 3번째 페이지로 구현. `PresetLabel` 11sp→7sp 자동 축소 + 최대 2줄.
> - `PointerDynamicsConstants.kt` — `CustomPointerDynamicsPreset`에 `description`, `iconKey` 필드 추가. `CUSTOM_PRESET_ICON_OPTIONS`(25개 아이콘), `customPresetIconOrNull()`, `CUSTOM_PRESET_TEMPLATES`(3개 기본 템플릿) 추가.
> - `AppIcons.kt` — `PickXXX` 25개 아이콘 항목 추가.
> - `StandardModePage.kt` — `customPresets` 상태 + `CustomPresetsRepository` remember로 관리. Column을 Box로 감싸서 `DynamicsCurveEditor` 전체 화면 오버레이 추가. `DynamicsCurveEditor` 호출 시 `existingPresets = customPresets` 전달.
> - `applyEdgeModeToggle` — `customPresetsCount` 파라미터 추가, DYNAMICS 순환 범위 `DYNAMICS_PRESETS.size + customPresetsCount`로 확장.
> - 히스테리시스: `previousVelocityDpMs` mutableFloatStateOf 추가, MOVE/RELEASE 이벤트에서 커스텀 분기 처리 후 갱신.
> - 빌트인 프리셋 편집/삭제는 팝업에서 `tempIndex >= totalBuiltin` 조건으로 자동 차단.

---

## Phase 4.5.17: 관성 스크롤 단계 phantom 키 입력 버그 근본 수정

**개발 기간**: 0.5일

**증상**:
- 고속 무한 스크롤 후 손가락을 떼면 관성이 시작되는 직후에 경보음 및 phantom 키 입력 재현
- Phase 4.5.7 수정 후에도 동일 증상이 재현됨 (재현 조건: 빠른 스와이프 → 손가락 뗌 직후)

**원인**:

Phase 4.5.7은 **MOVE 이벤트 루프**에만 `SCROLL_FRAME_MIN_INTERVAL_MS` 시간 게이트와 `SCROLL_MAX_FRAMES_PER_EVENT` 상한을 적용했다. 그러나 손가락을 뗀 후 시작되는 **관성 코루틴**(`inertiaJob`)의 내부 `while` 루프는 동일한 게이트 없이 모든 누적분을 즉시 전송한다.

관성 루프 구조:
```
outer while (velocity > MIN):
    delay(16ms)                         ← 16ms 대기
    moveDp = velocity * dt              ← 예: 6dp/ms × 16ms = 96dp
    inertiaScrollAccum += moveDp       ← 96dp 누적
    inner while (accum >= unitDp):      ← 96 / 15 ≈ 6회 반복
        ClickDetector.sendFrame(...)    ← 6프레임 연속 전송 ← ⚠️ 게이트 없음
```

`DOWN_MULTIPLIER = 2.0f`, 초기 속도 3dp/ms인 경우 boosted 속도 6dp/ms → 첫 tick에서 최대 6프레임이 간격 없이 연속 전송된다. 이는 Phase 4.5.7이 해결하려 했던 원래 문제와 동일한 메커니즘이다.

**수정 내용**:

관성 코루틴의 내부 `while` 루프에 MOVE 이벤트 루프와 동일한 시간 게이트를 적용한다.

- `TouchpadWrapper.kt` 관성 내부 `while` 루프 시작부에 시간 게이트 추가:
  ```kotlin
  while (abs(inertiaScrollAccum) >= effectiveUnitDp) {
      val nowMs = System.currentTimeMillis()
      if (nowMs - lastScrollFrameSentMs < SCROLL_FRAME_MIN_INTERVAL_MS) break  // 나머지는 다음 tick으로 이월
      
      val dir = if (inertiaScrollAccum > 0) 1 else -1
      inertiaScrollAccum -= dir * effectiveUnitDp
      ...
      lastScrollFrameSentMs = nowMs  // 공유 타이머 갱신
      ClickDetector.sendFrame(frame)
      ...
  }
  ```
- `lastScrollFrameSentMs`는 이미 MOVE 이벤트 루프와 공유되는 `mutableLongStateOf` 상태이므로 추가 선언 불필요. 관성 코루틴에서 동일 변수를 읽고 쓰면 MOVE ↔ 관성 간 전송 간격도 자동으로 보장됨.
- 결과: 관성 루프 16ms tick당 최대 1프레임 전송 (8ms 게이트 기준). MOVE 이벤트와 합산해도 초당 최대 125프레임 하드 상한 유지.

> **선택적 보강 (펌웨어)**:
> - `uart_handler.h` → `UART_RX_BUFFER_SIZE` 256 → 1024 바이트 변경 (버스트 내성 4배 향상)
> - Android 측 수정만으로 재현이 없으면 펌웨어 수정 불필요

**수정 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/TouchpadWrapper.kt`
  — 관성 코루틴 내부 `while` 루프 시작부에 `lastScrollFrameSentMs` 시간 게이트 추가
  — 프레임 전송 직전 `lastScrollFrameSentMs` 갱신

**검증**:
- [x] 최대한 빠르게 아래로 스와이프 후 손가락을 뗌 → 관성 시작 직후 경보음·phantom 키 입력 없음 확인 (10회 이상)
- [x] 관성 중 PC 화면에서 브라우저/앱이 예상치 않게 열리지 않음 확인
- [x] 관성 스크롤 속도·거리가 수정 전과 동등하게 유지되는지 확인 (이월로 인한 스크롤 손실 없음)
- [x] 관성 → 재터치 → MOVE 스크롤 전환 시 phantom 없음 확인
- [x] 일반(Normal) 스크롤 모드의 고속 드래그에서 동일 현상 없음 확인 (회귀 없음)

---

## Phase 4.5.18: 커스텀 프리셋 편집 UI BridgeOne 스타일 재설계 및 UX 개선

**개발 기간**: 3-4일

**개요**: `DynamicsCurveEditor`의 입력 방식 및 UI 전반을 BridgeOne 스타일로 재설계. 키보드 직접 타이핑(BasicTextField), AlertDialog 팝업, Material 버튼 방식을 스와이프 기반 인터랙션으로 교체.

---

### Phase 4.5.18.1: 스와이프 키보드 오버레이 (`SwipeKeyboardOverlay`)

이름/설명 필드 탭 시 그래프 영역을 대체하며 키보드 등장. `BasicTextField` + 시스템 키보드 방식 제거.

**화면 구조**:

```
┌──────────────────────────────────────────────────────┐
│  내 설정 1▮                                    6/12  │  ← 입력 결과 (조합 중 글자 ACCENT 강조, 캐럿 깜빡임)
├──────────────────────────────────────────────────────┤
│  ㅂ  ㅈ  ㄷ  ㄱ  ㅅ  ㅛ  ㅕ  ㅑ  ㅐ  ㅔ           │  ← Row0 (10키)
│  ㅁ  ㄴ  ㅇ  ㄹ  ㅎ  ㅗ  ㅓ  ㅏ  ㅣ              │  ← Row1 (9키)
│  ⇧  ㅋ  ㅌ  ㅊ  ㅍ  ㅠ  ㅜ  ㅡ  ⌫              │  ← Row2 (9키, ⇧·⌫ 1.6× 폭)
│  한글  ?123  ───⎵───  취소  완료                  │  ← Row3 (5키, ⎵ 2× 폭, 정중앙)
├──────────────────────────────────────────────────────┤
│  ↔ 드래그 → 손가락을 밀어 키 선택                    │
│  ⊙ 손 떼기 → 선택된 키 입력                         │  ← 조작 안내 (아이콘+액션+설명, 구분선)
│  ⇧ Shift → 쌍자음·대문자 (1회 후 복귀)              │
│  ⇄ 모드 → 한·A·123 순으로 전환                      │
└──────────────────────────────────────────────────────┘
```

**키보드 모드**:

| 모드 | Row0 (10) | Row1 (9) | Row2 (9) |
|------|-----------|----------|----------|
| 한글 | ㅂㅈㄷㄱㅅㅛㅕㅑㅐㅔ | ㅁㄴㅇㄹㅎㅗㅓㅏㅣ | ⇧ ㅋㅌㅊㅍㅠㅜㅡ ⌫ |
| 한글+Shift | ㅃㅉㄸㄲㅆㅛㅕㅑㅒㅖ | 동일 | ⇧(강조) ㅋㅌㅊㅍㅠㅜㅡ ⌫ |
| 영문 | qwertyuiop | asdfghjkl | ⇧ zxcvbnm ⌫ |
| 영문+Shift | QWERTYUIOP | ASDFGHJKL | ⇧(강조) ZXCVBNM ⌫ |
| 기호 | 1234567890 | -/:;()$&@ | .,?!'"_ ⌫ |

Row3 (공통): `한글/영문(현재모드표시)` `?123/한글` `⎵(2×)` `취소` `완료`

**상태 타입**:

```kotlin
enum class KeyboardMode { HANGUL, ENGLISH, SYMBOL }
enum class ShiftMode { OFF, ON }   // one-shot: 자모 1회 입력 후 자동 OFF
enum class ComposePhase { IDLE, JASO_ONLY, HAS_VOWEL, JAMO_PENDING }
sealed class GridCell { Jamo(char), Special(key), Empty }
enum class SpecialKey { SP, BACKSPACE, SHIFT, LANG_TOGGLE, SYMBOL_TOGGLE, ERASE, CANCEL, DONE }
data class CellPos(val row: Int, val col: Int)
data class KeyRow(val cells: List<GridCell>)
data class ComposerState(committed, phase, choIdx, jungIdx, pendingJasoChar, pendingJasoChar2)
```

**한글 조합 상태 머신**:

```
IDLE         + 자음 → JASO_ONLY (pendingJasoChar=c)
IDLE         + 모음 → committed += c
JASO_ONLY    + 모음 → HAS_VOWEL (초성+중성 조합)
JASO_ONLY    + 자음 → commitCurrent(), 새 JASO_ONLY
HAS_VOWEL    + 자음(종성가능) → JAMO_PENDING (pendingJasoChar=c)
HAS_VOWEL    + 자음(종성불가) → commitCurrent(), 새 JASO_ONLY
HAS_VOWEL    + 모음 → commitCurrent(), committed += 모음
JAMO_PENDING + 자음(이중받침가능, pendingJasoChar2==' ') → pendingJasoChar2=c (이중 받침)
JAMO_PENDING + 자음(이중받침불가 or pendingJasoChar2!=' ') → commitCurrent(), 새 JASO_ONLY
JAMO_PENDING + 모음(pendingJasoChar2!=' ') → 첫받침 jong으로 확정, 둘째받침→초성, 새 HAS_VOWEL
JAMO_PENDING + 모음(pendingJasoChar2==' ') → 받침→초성 분리, 새 HAS_VOWEL
```

이중 받침 지원(ㄳ ㄵ ㄶ ㄺ ㄻ ㄼ ㄽ ㄾ ㄿ ㅀ ㅄ): `pendingJasoChar2` 필드로 관리.
자동 모드 전환(`tryAutoSwitchMode`) 없음 — 한 화면에 자음·모음 모두 표시.

**스와이프 계산 (가변 행 폭)**:

손가락 down 시 `startCell: CellPos` + `startPos: Offset` 기록. move마다:
- `rowDelta = (dy / rowH).roundToInt()` → `newRow` 결정
- `startFracX = (startCell.col + 0.5f) / startRowCols`
- `newFracX = startFracX + dx / totalW` → `newCol = (newFracX * newRowCols).toInt()`
- `resolveToValidCell`로 Empty 셀 보정 (같은 행 좌·우 → 인접 행 동일 분수 위치)
- 10dp 미만 이동 후 up → tap으로 처리 (현재 선택 셀 활성화)

**특수 키 동작**:

| 키 | 동작 |
|----|------|
| SHIFT | ShiftMode 토글. ON 시 ACCENT 강조. 자모/영문 입력 1회 후 자동 OFF |
| LANG_TOGGLE | HANGUL↔ENGLISH 토글 (SYMBOL에서 탭하면 HANGUL로). 선택 위치 유지 |
| SYMBOL_TOGGLE | SYMBOL↔HANGUL 토글. 선택 위치 유지. 라벨: SYMBOL일 때 "한글", 아닐 때 "?123" |
| SP | commitCurrent() + 공백 추가 |
| BACKSPACE | commitCurrent() → dropLast(1). committed 비면 mode=HANGUL |
| ERASE | 전체 초기화 + mode=HANGUL + shift=OFF + selectedCell=(0,0) |
| CANCEL | onCancel() 호출 (그래프 화면으로 복귀) |
| DONE | onDone(committed) 호출 |

**키 폭 가중치**:
- 일반 자모/기호 키: `weight(1f)`
- SHIFT, BACKSPACE: `weight(1.6f)`
- SP: `weight(2f)` — Row3에서 좌 2f + 우 2f로 정중앙 배치

**LANG_TOGGLE 라벨**: 현재 모드 표시 (한글→"한글", 영문→"영문", 기호→"한/영")

**신규 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/SwipeKeyboardOverlay.kt`

**수정 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/touchpad/DynamicsCurveEditor.kt`
  — 이름/설명 행: `BasicTextField` 제거, `Text` + `pointerInput` 탭 핸들러로 교체
  — 그래프 영역 `Column` 내부를 `if (showKeyboard) SwipeKeyboardOverlay(...) else CurveGraphCanvas(...)` 분기로 교체
  — `SwipeKeyboardOverlay` 시그니처: `(initialText, maxLength, onCancel, onDone)`

**검증**:
- [x] 이름/설명 필드 탭 → 그래프 영역에 스와이프 키보드 표시
- [x] 한글 모드: Row0=10키, Row1=9키, Row2=9키, Row3=5키
- [x] 자음+모음 조합 (예: ㄴ+ㅏ=나, ㄴ+ㅏ+ㅇ=낭)
- [x] 이중 받침 조합 (예: ㄷ+ㅏ+ㄹ+ㄱ=닭, 닭+ㅏ=다가)
- [x] ⇧ 1회 탭 → 쌍자음(ㅃㅉㄸㄲㅆ) 노출, 입력 후 자동 복귀
- [x] 한/영 탭 → HANGUL↔ENGLISH 토글, 선택 위치 유지
- [x] ?123 탭 → 기호 모드 진입/복귀, 선택 위치 유지
- [x] 기호 모드 Row2 여백 없음 (7자+⌫=8셀)
- [x] ⎵ 키 Row3 정중앙 배치 (좌 2f=한글+?123, 우 2f=취소+완료)
- [x] 캐럿 깜빡임, 조합 중 글자 ACCENT 강조
- [x] 취소 탭 → 그래프 화면 복귀
- [x] 완료 탭 → committed 텍스트 전달 후 복귀
- [x] maxLength 초과 시 입력 차단
- [x] 조작 안내 섹션 (아이콘+액션+설명, 구분선) 키보드 하단 표시

---

### Phase 4.5.18.2: 아이콘 선택 인라인 화면 전환

> **⚠️ Phase 4.5.18.1 변경사항**: `DynamicsCurveEditor`에 `showKeyboard: Boolean` + `keyboardTarget: String` 상태 추가. 이름/설명 행이 `BasicTextField` 대신 `Text` + `pointerInput` 탭 핸들러로 교체됨.
> `SwipeKeyboardOverlay`는 전체화면 오버레이가 아니라 그래프 `Column` 내부의 `if/else` 분기로 `CurveGraphCanvas`를 대체함 — `Box(Modifier.fillMaxWidth().weight(1f))`로 감싸 그래프와 동일한 공간 점유.
> `AnimatedContent(currentScreen)` 도입 시 `SwipeKeyboardOverlay`는 `AnimatedContent` 바깥에 배치해야 이름/설명 탭이 정상 동작함 (오버레이가 화면 전환과 독립적으로 표시되어야 함).
> `SwipeKeyboardOverlay` 시그니처에 `onCancel: () -> Unit = {}` 추가됨 — 취소 탭 시 `showKeyboard = false` 호출.

`AlertDialog` 아이콘 선택 팝업 제거. "아이콘" 행 탭 → 편집기 내부가 아이콘 선택 화면으로 `AnimatedContent` 전환.

**아이콘 선택 화면 구조**:

```
┌──────────────────────────────────────────────────────┐
│  [← 뒤로]   아이콘 선택                              │
├──────────────────────────────────────────────────────┤
│  [없음] (이름 2자 표시)                              │
├──────────────────────────────────────────────────────┤
│  [○] [○] [○] [○] [○]                               │
│  [○] [○] [○] [○] [○]   ← 아이콘 그리드              │
│  [○] [○] [○] [○] [○]   (5열, 스크롤 가능)           │
│  ...                                                 │
└──────────────────────────────────────────────────────┘
```

- 현재 선택 아이콘은 `ACCENT_BLUE` 테두리 강조
- 탭으로 즉시 선택 + 이전 화면 복귀

**수정 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/touchpad/DynamicsCurveEditor.kt`
  — `showIconPicker` AlertDialog 제거
  — `currentScreen` 상태 추가 (`MAIN | ICON_PICKER`)
  — `AnimatedContent(currentScreen)` 분기로 본 화면 / 아이콘 선택 화면 전환

**검증**:
- [ ] 아이콘 행 탭 → 아이콘 선택 화면으로 전환 확인
- [ ] 아이콘 탭 → 즉시 선택 + 본 화면 복귀 확인
- [ ] "없음" 선택 → 이름 2자 표시 방식으로 복귀 확인
- [ ] 전환 애니메이션이 BridgeOne 스타일과 어울리는지 확인

---

### Phase 4.5.18.3: 템플릿 선택 스와이프 오버레이

`AlertDialog` 템플릿 선택 팝업 제거. 템플릿 항목을 스와이프로 순환, 탭으로 적용.

**오버레이 구조 (DynamicsPresetPopup CONFIRM 단계 참고)**:

```
┌──────────────────────────────────────────────────────┐
│  (반투명 배경)                                        │
│                                                      │
│  ┌────────────────────────────────────┐              │
│  │  [템플릿 아이콘]                    │              │
│  │  템플릿 이름                         │              │
│  │  템플릿 설명                         │              │
│  │                                    │              │
│  │  적용    취소                        │  ← 스와이프  │
│  └────────────────────────────────────┘              │
│  ◀▶ 스와이프로 템플릿 이동                           │
└──────────────────────────────────────────────────────┘
```

- 좌우 스와이프로 템플릿 목록 순환
- 탭으로 "적용" / "취소" 확정

**수정 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/touchpad/DynamicsCurveEditor.kt`
  — `showTemplatePicker` AlertDialog 제거
  — `TemplatePickerOverlay` private Composable 신규 추가

**검증**:
- [ ] 템플릿 선택 진입 시 스와이프 오버레이 등장 확인
- [ ] 좌우 스와이프로 템플릿 순환 확인
- [ ] 적용 탭 → 가속/감속 곡선 교체 확인
- [ ] 취소 탭 → 곡선 변경 없이 오버레이 닫힘 확인

---

### Phase 4.5.18.4: 상단 바 및 전체 레이아웃 스타일 통일

상단 바의 Material 버튼을 제거하고 BridgeOne 스타일로 정리. 삭제 확인 AlertDialog 제거.

**상단 바 변경**:
- `TextButton("템플릿")` 제거 → 이름 입력 행 아래 별도 "템플릿" 항목 행으로 이동 (아이콘 선택 행과 동일한 방식)
- 저장 `IconButton` → `Text("저장")` 대형 텍스트 (비활성 시 Color.Gray)
- 취소 `IconButton(Close)` 유지 (크기 및 여백 조정)

**삭제 확인 AlertDialog 제거**:
- 노드 롱프레스 → AlertDialog 대신 인라인 확인: 하단 안내 바가 "삭제 확인: 탭 · 취소: 다른 곳 터치"로 교체
- 500ms 롱프레스 후 안내 바 전환, 다음 탭으로 삭제 실행

**수정 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/touchpad/DynamicsCurveEditor.kt`
  — 상단 바 레이아웃 수정
  — `showTemplatePicker` 상태 → `currentScreen = TEMPLATE_PICKER` 방식으로 통합
  — `deleteTargetIndex` AlertDialog 제거, 하단 안내 바 인라인 확인 방식으로 교체

**검증**:
- [ ] 상단 바에 Material 버튼 없이 BridgeOne 스타일로 렌더링 확인
- [ ] 노드 롱프레스 후 하단 안내 바가 삭제 확인으로 전환 확인
- [ ] 확인 탭 → 노드 삭제 + 안내 바 복귀 확인
- [ ] 다른 곳 터치 → 삭제 취소 + 안내 바 복귀 확인
- [ ] 저장 비활성(이름 비어있음/중복) 시 저장 텍스트 회색 표시 + 탭 무시 확인

---

**신규 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/SwipeKeyboardOverlay.kt`

**수정 파일 종합**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/touchpad/DynamicsCurveEditor.kt`

---

## Phase 4.5 완료 후 Phase 4.4.9 검증 항목 영향

Phase 4.5 수정 완료 후 아래 Phase 4.4.9 검증 항목을 재검증해야 합니다:

| 검증 항목 | Phase 4.5 하위 Phase | 재검증 필요 |
|----------|---------------------|------------|
| A. 앱 연결 상태 표시가 실제 연결 여부와 일치 | 4.5.11 | ✅ |
| C. 우클릭 모드 전환 후 탭 → 우클릭 신호 전달 | 4.5.1 | ✅ |
| D. 무한 스크롤 연속 패킷 전송 | 4.5.5, 4.5.6, 4.5.7, 4.5.17 | ✅ |
| D. 무한 스크롤 속도 비례 | 4.5.7 | ✅ |
| D. 무한 스크롤 관성 단계 phantom 키 입력 | 4.5.17 | ✅ |
| H. 엣지 스와이프로 우클릭 모드 전환 후 탭 | 4.5.1 | ✅ |
