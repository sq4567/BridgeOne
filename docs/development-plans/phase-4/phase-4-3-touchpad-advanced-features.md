---
title: "BridgeOne Phase 4.3: 터치패드 고급 기능"
description: "BridgeOne 프로젝트 Phase 4.3 - ControlButtonContainer, 스크롤 모드+가이드라인, 직각 이동, DPI 조절, 테두리 색상 구현"
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
- 모드별 터치패드 테두리 색상 표시 (단색/그라데이션)

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
  - **⚠️ DPI 곱수 미지원** — Phase 4.3.6에서 추가 필요
  - **⚠️ 축 잠금(직각 이동) 미지원** — Phase 4.3.5에서 추가 필요
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

> **⚠️ Phase 4.1.8 변경사항**: 커스텀 토스트 시스템 도입. `android.widget.Toast` 사용 금지. 모든 알림은 `ToastController.show(message, ToastType, durationMs)` 로 표시. 타입: `INFO`(파란색) · `SUCCESS`(초록색) · `WARNING`(주황색, 검은 텍스트) · `ERROR`(빨간색). 무제한 표시: `TOAST_DURATION_INFINITE`.

> **⚠️ StatusToast 개선 사항 (Phase 4.2 이후 반영)**: `StatusToast.kt` 대폭 업데이트 — 호출 측 API(`ToastController.show`) 변경 없음.
> 1. **타이머 테두리**: 유한 `durationMs` 토스트는 자동으로 남은 시간 비례 shrinking border 표시 — 호출 측 코드 변경 불필요.
> 2. **다중 토스트 스태킹**: 기존 토스트 표시 중 새 토스트 표시 시 → 새 토스트가 위에서 아래로 슬라이드인(350ms) → 완료 후 기존 토스트가 위로 슬라이드아웃(300ms). 두 토스트가 잠시 동시에 표시됨.
> 3. **중복 제거**: `ToastMessage.equals()`가 message·type·durationMs 내용 기준(내부 id 제외). 동일 내용의 `show()` 연속 호출 → StateFlow 충돌 방지 → 단일 토스트만 표시.

> **⚠️ Phase 4.2.2 변경사항**: `Page1TouchpadActions`에서 좌측 터치패드 영역은 `Box { TouchpadWrapper(...) }` 구조로 감싸져 있음. `ControlButtonContainer`는 이 `Box` 내부에서 `TouchpadWrapper` 위에 오버레이로 추가하면 됨 — 별도 부모 레이아웃 변경 불필요. `TouchpadWrapper`에 `modifier = Modifier.fillMaxSize()` 적용되어 있으므로 오버레이는 `Box` 내 `align(Alignment.TopCenter)` 또는 `offset`으로 배치.

**검증**:
- [x] 상단 15% 오버레이 정상 배치
- [x] 6개 버튼 렌더링 및 터치 반응
- [x] Phase 4.2.3 Special Keys: ClickDetector 연결 후 HID KeyDown/KeyUp 실기기 전송 확인
- [x] 버튼 가시성 규칙 (스크롤 모드 시 전환)
- [x] Essential 모드에서 컨테이너 숨김
- [x] 히트 영역 48×48dp 보장

---

## Phase 4.3.2: ControlButtonContainer 등장/사라짐 애니메이션

**목표**: ControlButtonContainer 및 개별 버튼의 표시/숨김 전환을 자연스러운 슬라이드 애니메이션으로 구현

**개발 기간**: 0.5일

**쉬운 설명**: 제어 버튼들이 갑자기 교체되지 않고, 기존 버튼이 위로 올라가며 사라진 뒤 새 버튼이 위에서 내려오면서 나타나도록 합니다. 예를 들어 스크롤 모드로 전환하면 DPI 버튼이 위로 올라가며 완전히 사라진 후 스크롤 감도 버튼이 위에서 내려오는 식입니다. 단, 버튼이 나가는 모습이 터치패드 영역 밖까지 보이면 어색하므로 ControlButtonContainer 경계에서 잘려야 합니다. 또한 스크롤 버튼 자체는 ON/OFF 전환 시 위치가 바뀌지 않고 제자리를 유지합니다.

**실제 구현 내용**:
1. ControlButtonContainer 컨테이너 전체 애니메이션:
   - Standard → Essential 모드 전환: 위로 슬라이드 아웃 + 페이드아웃 (300ms)
   - Essential → Standard 모드 전환: 위에서 슬라이드 인 + 페이드인 (300ms)
   - `AnimatedVisibility(visible = isStandardMode)` — `isStandardMode` 파라미터 추가 (기본값 `true`)
2. 개별 버튼 교체 애니메이션 (위에서 아래로):
   - 퇴장: 위로 슬라이드 아웃 (200ms, `slideOutVertically { -it }`)
   - 등장: **위에서 아래로** 슬라이드 인 (200ms, `slideInVertically { -it }`)
   - 퇴장하는 버튼은 AnimatedVisibility 퇴장 시 자동 비활성화
   - 대상 버튼: 스크롤 모드 전환 시 ClickModeButton, MoveModeButton, DPIControlButton ↔ ScrollSensitivityButton
3. DPI ↔ ScrollSensitivity 순차 교체:
   - 퇴장 200ms → 200ms 대기 → 등장 200ms (`tween(200, delayMillis = 200)`)
   - 동일 Box 슬롯 공유 (기존 별도 Row → 단일 Box로 변경)
4. 퇴장 버튼 클리핑:
   - Row, 좌측 Row, 우측 Box에 `Modifier.clipToBounds()` 적용
   - 좌측 ClickMode/MoveMode는 고정 크기 `Box(Modifier.size(...).clipToBounds())` 내부 AnimatedVisibility
5. 스크롤 버튼 위치 고정:
   - ClickModeButton, MoveModeButton을 고정 크기 Box로 감싸 공간 항상 확보
   - ScrollModeButton은 항상 3번째 위치 유지
6. 아이콘 추가 (§1.4 설계 반영):
   - `ControlButton`에 `iconResId: Int` 파라미터 추가
   - 텍스트 하단에 버튼 높이 40% 크기로 배치
   - 모든 버튼에 상태별 VectorDrawable 아이콘 매핑
7. 버튼 너비: 6등분 → 5등분 (DPI/ScrollSensitivity 동일 슬롯 공유 반영)
8. disabled 투명도 제거 (alpha 항상 1f)

**수정 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/touchpad/ControlButtonContainer.kt`

**참조 문서**:
- `docs/android/component-touchpad.md` §1.3 (제어 버튼 컨테이너)
- `docs/android/component-touchpad.md` §1.4 (공통 구조 — 아이콘 사양)

**검증**:
- [x] Standard↔Essential 전환 시 컨테이너 슬라이드 인/아웃
- [x] 스크롤 모드 ON: ClickModeButton·MoveModeButton·DPIControlButton 위로 슬라이드 아웃, ScrollSensitivityButton 위에서 슬라이드 인
- [x] 스크롤 모드 OFF: ScrollSensitivityButton 위로 슬라이드 아웃, ClickModeButton·MoveModeButton·DPIControlButton 위에서 슬라이드 인
- [x] DPI↔ScrollSensitivity 순차 교체 (퇴장 완료 후 등장)
- [x] 퇴장 버튼이 ControlButtonContainer 경계 밖으로 노출되지 않음 (클립 확인)
- [x] ScrollModeButton이 스크롤 ON/OFF 전환 시 동일 위치 유지
- [x] 애니메이션 도중 고정 버튼 탭 정상 동작
- [x] 모든 버튼에 상태별 아이콘 표시

---

## Phase 4.3.3: 일반 스크롤 모드 + ScrollGuideline 기본 구현

> **⚠️ Phase 4.3.1 변경사항**:
> - 신규 파일 `ui/components/touchpad/TouchpadMode.kt`: `TouchpadState` 데이터 클래스에 모든 모드 상태 통합 관리 (ClickMode, MoveMode, ScrollMode, CursorMode, DpiLevel, ScrollSensitivity).
> - 신규 파일 `ui/components/touchpad/ControlButtonContainer.kt`: 6개 제어 버튼 오버레이 구현 완료.
> - **스크롤 버튼 동작 변경**: `cycleScrollMode()` 제거됨 → `toggleScrollMode()` + `switchScrollMode()` 분리.
>   - **탭**: OFF ↔ lastScrollMode 토글 (켜기/끄기). OFF→lastScrollMode 복원, NORMAL/INFINITE→OFF (lastScrollMode 저장).
>   - **롱프레스** (스크롤 ON 중만): NORMAL ↔ INFINITE 전환.
>   - 기존 3단계 순환(OFF→NORMAL→INFINITE→OFF)이 아닌 **토글+롱프레스** 패턴.
> - `TouchpadState`는 `StandardModePage.kt`의 `Page1TouchpadActions`에서 `remember { mutableStateOf(TouchpadState()) }`로 관리됨 — `TouchpadWrapper`에 `touchpadState`를 전달하는 연결은 아직 미구현.
> - 스크롤 모드 enum: `ScrollMode.OFF` / `ScrollMode.NORMAL_SCROLL` / `ScrollMode.INFINITE_SCROLL` (문서의 `TouchpadMode.SCROLL_NORMAL`이 아님).
> - ScrollSensitivityButton, ScrollModeButton은 ControlButtonContainer에서 이미 동작 → 이 Phase에서는 `TouchpadWrapper`에 스크롤 입력 변환 로직 추가와 `touchpadState` 연결에 집중.
> - `ControlButton`은 `combinedClickable` 사용 (`onLongClick` 파라미터 지원).

**목표**: 터치 드래그를 수직 스크롤 입력으로 변환하는 일반 스크롤 모드 구현 + ScrollGuideline Composable 기본 구현 (초록색)

**개발 기간**: 1-1.5일

**세부 목표**:
1. 스크롤 모드 상태 관리:
   - `TouchpadState.scrollMode == ScrollMode.NORMAL_SCROLL` 상태 활용 (이미 정의됨)
   - ScrollModeButton 탭으로 진입 (이미 구현됨)
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
5. `ScrollGuideline` Composable 기본 구현:
   - 위치: 터치패드 내부 (테두리 제외)
   - 표시 조건: 스크롤 모드에서 드래그 시작 시
   - 숨김 조건: 스크롤 정지 판정 후 일정 시간 경과, 또는 모드 종료 시 즉시
   - 일반 스크롤 색상: `#84E268` (초록)
   - 방향 표시: 수직 스크롤 방향에 따른 화살표 또는 라인
   - `TouchpadWrapper` 내부에서 `ScrollMode.NORMAL_SCROLL` 상태에 따라 표시/숨김

**신규 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/touchpad/ScrollGuideline.kt`

**수정 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/TouchpadWrapper.kt`
- `TouchpadMode.kt`

**참조 문서**:
- `docs/android/component-touchpad.md` §1.3.1.3 (ScrollModeButton)
- `docs/android/component-touchpad.md` §1.5 (스크롤 가이드라인)
- `docs/android/technical-specification-app.md` §2.2 (터치패드 알고리즘)

**검증**:
- [ ] ScrollModeButton 탭으로 스크롤 모드 진입
- [ ] Y축 드래그 시 휠 프레임 전송
- [ ] 감도 3단계 전환 동작
- [ ] 더블탭으로 커서 이동 모드 복귀
- [ ] PC에서 실제 스크롤 동작 확인 (하드웨어 E2E)
- [ ] 드래그 시 초록색 가이드라인 표시
- [ ] 스크롤 정지 후 가이드라인 자연스럽게 숨김
- [ ] 모드 종료 시 가이드라인 즉시 숨김

---

## Phase 4.3.4: 무한 스크롤 모드 + 관성 + ScrollGuideline 색상 확장

> **⚠️ Phase 4.3.1 변경사항**: `ScrollMode.INFINITE_SCROLL` enum과 ScrollModeButton 전환 로직은 이미 구현됨. `TouchpadState.lastScrollMode`로 마지막 스크롤 모드 기억. 스크롤 버튼 탭=토글(ON/OFF), 롱프레스=NORMAL↔INFINITE 전환. 이 Phase에서는 관성 알고리즘과 TouchpadWrapper 연동, ScrollGuideline 무한 스크롤 확장에 집중.

**목표**: 관성 기반 무한 스크롤 모드 구현 + ScrollGuideline 무한 스크롤 색상 및 속도 비례 강도 추가 (Standard 모드 전용)

**개발 기간**: 1-1.5일

**세부 목표**:
1. 무한 스크롤 상태:
   - `TouchpadState.scrollMode == ScrollMode.INFINITE_SCROLL` 활용 (이미 정의됨)
   - 일반 스크롤 상태에서 ScrollModeButton 탭으로 전환 (이미 구현됨)
   - 무한 스크롤에서 ScrollModeButton 탭 → 일반 스크롤로 복귀 (이미 구현됨)
2. 관성 알고리즘:
   - 프레임-레이트 독립적 지수 감쇠 (exponential decay)
   - 손가락 떼는 순간의 속도를 초기 관성 속도로 사용
   - 감쇠 계수: 조절 가능 (기본값 TBD, 실측 후 확정)
   - 관성 중 터치 시 즉시 관성 정지
3. 햅틱 피드백:
   - 스크롤 단위마다 Light 햅틱
   - 관성 감속 시 점진적으로 햅틱 간격 증가
4. ScrollGuideline 무한 스크롤 확장:
   - 무한 스크롤 색상: `#F32121` (빨강) — `ScrollMode`에 따라 초록/빨강 자동 전환
   - 스크롤 속도에 비례하는 시각적 강도 (관성 중 강도 점진적 감소)
   - 관성 중에도 가이드라인 유지, 관성 정지 후 숨김

**수정 파일**:
- `TouchpadWrapper.kt`
- `DeltaCalculator.kt` (관성 계산 추가)
- `TouchpadMode.kt`
- `ScrollGuideline.kt` (무한 스크롤 색상 + 속도 비례 강도 추가)

**참조 문서**:
- `docs/android/component-touchpad.md` §1.3.1.3 (스크롤 모드 버튼)
- `docs/android/component-touchpad.md` §1.5 (스크롤 가이드라인)
- `docs/android/component-touchpad.md` §2.4 (스크롤 가이드라인 색상)

**검증**:
- [ ] 손가락 떼면 관성 스크롤 지속
- [ ] 관성 감쇠로 자연스러운 정지
- [ ] 관성 중 터치 시 즉시 정지
- [ ] 감도 설정 적용
- [ ] 무한 스크롤 중 빨간색 가이드라인 표시
- [ ] 스크롤 속도에 따라 가이드라인 강도 변화
- [ ] 관성 감속 중 가이드라인 강도 점진적 감소
- [ ] 일반 ↔ 무한 스크롤 전환 시 가이드라인 색상 즉시 반영

---

## Phase 4.3.5: 직각 커서 이동 모드

> **⚠️ Phase 4.3.1 변경사항**: `MoveMode.FREE` / `MoveMode.RIGHT_ANGLE` enum 및 MoveModeButton 전환 로직은 이미 구현됨. `TouchpadState.moveMode`로 상태 관리. 이 Phase에서는 `DeltaCalculator`에 축 잠금 알고리즘 추가와 `TouchpadWrapper`에서 `moveMode` 반영에 집중.

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

## Phase 4.3.6: DPI 조절 시스템

> **⚠️ Phase 4.3.1 변경사항**: `DpiLevel` enum (LOW/NORMAL/HIGH, multiplier 포함)과 DPIControlButton 순환 로직은 이미 구현됨. `TouchpadState.dpiLevel`로 상태 관리. 이 Phase에서는 `DeltaCalculator`에 DPI 곱수 적용과 SharedPreferences 저장에 집중.

> **⚠️ Phase 4.3.2 변경사항**:
> - `ControlButton`에 `iconResId: Int` 파라미터 필수 추가됨. DPIControlButton 아이콘(`ic_slow`/`ic_normal`/`ic_fast`)은 `dpiButtonIcon()` 헬퍼로 이미 매핑 완료 → 아이콘 관련 작업 불필요.
> - 버튼 너비 6등분 → 5등분 변경 (DPI/ScrollSensitivity 동일 슬롯 공유).
> - disabled 투명도(alpha 0.4f) 제거됨 — 모든 버튼 항상 alpha 1f.

**목표**: 터치패드 커서 감도를 3단계로 조절하는 DPI 시스템

**개발 기간**: 0.5일

**세부 목표**:
1. DPI 3단계:
   - 낮음: 델타 곱수 ×0.5 (정밀 작업용)
   - 보통: 델타 곱수 ×1.0 (기본)
   - 높음: 델타 곱수 ×2.0 (빠른 이동용)
2. DPIControlButton:
   - 탭으로 순환: 낮음 → 보통 → 높음 → 낮음 (이미 구현됨)
   - 아이콘: `ic_slow.xml` / `ic_normal.xml` / `ic_fast.xml`
   - 텍스트: "DPI" + 현재 레벨 (이미 구현됨)
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

## Phase 4.3.7: 터치패드 테두리 모드 색상 표시

> **⚠️ Phase 4.3.2 변경사항**: `ControlButtonContainer.kt`의 색상 상수는 여전히 private. 아이콘 헬퍼 함수(`dpiButtonIcon`, `scrollModeButtonIcon`, `scrollSensitivityButtonIcon`)도 private으로 추가됨. 색상을 `TouchpadColors.kt`로 추출 시 아이콘 헬퍼는 ControlButtonContainer에 유지하면 됨.

> **⚠️ Phase 4.3.3 변경사항**: `ScrollGuideline.kt` 신규 생성됨. `#84E268`(초록), `#F32121`(빨강) 색상 상수가 이미 `ScrollGuideline.kt` 내부에 정의되어 있을 수 있음 → `TouchpadColors.kt` 추출 시 `ScrollGuideline.kt`의 색상도 함께 이동하여 단일 소스로 관리.

**목표**: 현재 활성 모드 조합에 따라 터치패드 테두리를 단색 또는 좌→우 그라데이션으로 표시

**개발 기간**: 0.5-1일

**쉬운 설명**: 지금 터치패드가 어떤 모드인지(좌클릭/우클릭, 자유이동/직각이동, 스크롤 등) 테두리 색상만 보고 바로 알 수 있게 하는 기능입니다. 모드가 하나면 단색, 두 가지 모드가 섞이면 왼쪽에서 오른쪽으로 색이 변하는 그라데이션으로 표시됩니다.

**세부 목표**:
1. 테두리 사양:
   - 너비: 터치패드 너비의 0.8% (최소 2dp, 최대 4dp)
   - 적용 대상: `TouchpadAreaWrapper` (싱글 커서 모드에서 `Touchpad1Area`)
   - 라운드 코너: 기존 12dp 코너에 맞춤
2. 색상 결정 우선순위 (§2.2.1):
   - **1순위** 무한 스크롤 활성: 빨간색 `#F32121` 단색
   - **2순위** 일반 스크롤 활성: 초록색 `#84E268` 단색
   - **3순위** 커서 이동 모드: 클릭×이동 모드 조합 (§2.2.2 조합 규칙)
3. 조합 규칙 (§2.2.2 — 커서 이동 모드 상태에서):
   - 좌클릭(파랑) + 자유이동(파랑) → 파란색 `#2196F3` 단색
   - 좌클릭(파랑) + 직각이동(주황) → 주황색 `#FF8A00` 단색 (파랑 아닌 쪽 사용)
   - 우클릭(노랑) + 자유이동(파랑) → 노란색 `#F3D021` 단색 (파랑 아닌 쪽 사용)
   - 우클릭(노랑) + 직각이동(주황) → 좌=노란색, 우=주황색 **좌→우 그라데이션**
4. 테두리 색상 함수:
   - `TouchpadState`를 입력으로 받아 단색(`Color`) 또는 그라데이션(`Brush`)을 반환
   - 색상 상수: `ControlButtonContainer.kt`의 private 색상을 공용 파일로 추출 (또는 별도 `TouchpadColors.kt` 생성)
5. 그라데이션 구현:
   - `Brush.horizontalGradient(listOf(leftColor, rightColor))` 활용
   - `Modifier.border(width, brush, shape)` 으로 테두리 적용
6. Essential 모드:
   - 테두리 완전 비표시 (ControlButtonContainer와 동일하게 숨김)

**신규 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/touchpad/TouchpadColors.kt` — 터치패드 관련 공용 색상 상수 및 테두리 색상 결정 함수

**수정 파일**:
- `TouchpadWrapper.kt` — 테두리 Modifier 적용
- `ControlButtonContainer.kt` — private 색상 상수를 `TouchpadColors.kt`로 이동 후 참조 변경

**참조 문서**:
- `docs/android/component-touchpad.md` §2.2 (테두리 색상 적용 규칙)
- `docs/android/component-touchpad.md` §2.2.1 (결정 기준)
- `docs/android/component-touchpad.md` §2.2.2 (조합 규칙)

**검증**:
- [ ] 기본 상태(좌클릭 + 자유이동): 파란색 단색 테두리
- [ ] 우클릭 전환 시: 노란색 단색 테두리
- [ ] 직각 이동 전환 시: 주황색 단색 테두리
- [ ] 우클릭 + 직각 이동: 노란색→주황색 그라데이션 테두리
- [ ] 일반 스크롤 모드: 초록색 단색 테두리
- [ ] 무한 스크롤 모드: 빨간색 단색 테두리 (최우선)
- [ ] 모드 전환 시 테두리 색상 즉시 반영
- [ ] Essential 모드에서 테두리 비표시
- [ ] 테두리 너비 2~4dp 범위 준수

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
| 모드별 테두리 색상 | ❌ | ✅ (모드 조합에 따른 단색/그라데이션) |
