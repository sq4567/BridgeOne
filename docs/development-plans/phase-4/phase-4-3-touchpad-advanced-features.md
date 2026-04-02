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
- 스크롤 감도 3단계
- 앱 아이콘 시스템 (`AppIcons`)

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
   - 터치 피드백: 햅틱 + scaleY (1.0 → 0.95, 상단 피벗 고정 — X축 유지, Y축만 찌그러짐)
   - 히트 영역: 최소 48×48dp 보장
3. 좌측 모드 제어 버튼 (4개):
   - **ClickModeButton**: 좌클릭 ↔ 우클릭 전환
   - **MoveModeButton**: 자유 이동 ↔ 직각 이동 전환 (커서 이동 모드에서만 표시)
   - **ScrollModeButton**: 커서 이동 → 일반 스크롤 → 무한 스크롤 순환
   - **CursorModeButton**: Page 1에서는 비표시, Page 2에서는 표시 (Phase 4+에서 Page 2 신설 시 구현)
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

**목표**: 터치 드래그를 세로/가로 스크롤 입력으로 변환하는 일반 스크롤 모드 구현 + ScrollGuideline Composable 기본 구현 (초록색, 스크롤 단위마다 끊기는 애니메이션 + 햅틱)

**개발 기간**: 1-1.5일

**쉬운 설명**: 스크롤 모드에서 손가락을 움직이면, 처음 움직인 방향을 기준으로 세로 스크롤인지 가로 스크롤인지 자동으로 결정됩니다. 그 이후로는 손가락을 어떻게 움직여도 처음 정해진 방향으로만 스크롤됩니다 (실제 마우스 휠처럼). 화면에는 스크롤 단위마다 가이드라인이 톡톡톡 끊기면서 움직이고, 그때마다 진동으로 피드백을 줍니다.

**상수 정의** (`ui/common/ScrollConstants.kt` 신규 파일에 정의):

| 상수명 | 기본값 | 설명 |
|---|---|---|
| `SCROLL_AXIS_LOCK_DISTANCE_DP` | `8f` | 축 확정을 시도하기 위한 누적 이동 임계값 (dp). 이 거리 이전에는 UNDECIDED 유지 |
| `SCROLL_AXIS_DIAGONAL_DEAD_ZONE_DEG` | `15f` | 45°에서 이 각도 이내이면 대각선으로 판정 — 축을 확정하지 않고 계속 누적 (0이면 비활성화) |
| `SCROLL_UNIT_DISTANCE_DP` | `20f` | 스크롤 1단위를 전송하기 위해 손가락이 이동해야 하는 거리 (dp). 값이 작을수록 빠름 |
| `SCROLL_SENSITIVITY_SLOW` | `0.5f` | 느림 단계 스크롤 비율 배수 |
| `SCROLL_SENSITIVITY_NORMAL` | `1.0f` | 보통 단계 스크롤 비율 배수 (기본) |
| `SCROLL_SENSITIVITY_FAST` | `2.0f` | 빠름 단계 스크롤 비율 배수 |
| `SCROLL_GUIDELINE_STEP_DP` | `20f` | 스크롤 1단위 전송 시 가이드라인이 이동하는 스텝 거리 (dp) |
| `SCROLL_GUIDELINE_HIDE_DELAY_MS` | `800L` | 스크롤 정지 판정 후 가이드라인이 사라지기 시작하기까지의 대기 시간 (ms) |
| `SCROLL_STOP_THRESHOLD_MS` | `150L` | 마지막 스크롤 이벤트 이후 이 시간 동안 입력 없으면 스크롤 정지로 판정 (ms) |
| `SCROLL_GUIDELINE_SPRING_STIFFNESS` | `10_000f` | 가이드라인 spring 강성 (높을수록 빠르고 즉각적 / `Spring.StiffnessHigh` 기준) |
| `SCROLL_GUIDELINE_SPRING_DAMPING` | `1.0f` | 가이드라인 spring 감쇠비 (1.0=오버슈트 없음 / 0.5 이하=통통 튀는 느낌) |

**세부 목표**:
1. 스크롤 모드 상태 관리:
   - `TouchpadState.scrollMode == ScrollMode.NORMAL_SCROLL` 상태 활용 (이미 정의됨)
   - ScrollModeButton 탭으로 진입 (이미 구현됨)
   - 터치패드 영역 **원탭**으로 스크롤 모드 종료 → 커서 이동 복귀
     - NORMAL_SCROLL: 현재 제스처에 드래그 없으면(`!deadZoneEscaped`) 즉시 해제
     - INFINITE_SCROLL: DOWN 시 관성이 활성이었으면 해제 차단 (`inertiaWasActiveOnDown`)
2. 스크롤 축 확정 알고리즘 (스크롤 방향 잠금):
   - 터치 DOWN 시점에 `scrollAxis: ScrollAxis = ScrollAxis.UNDECIDED` 초기화 (`UNDECIDED / HORIZONTAL / VERTICAL`)
   - 드래그 중 누적 이동 벡터(ΔX, ΔY) 크기가 `SCROLL_AXIS_LOCK_DISTANCE_DP`를 초과하는 순간 축 판정 시도:
     - `angle = atan2(|ΔY|, |ΔX|)` (0°=완전가로, 90°=완전세로)
     - `|angle − 45°| ≤ SCROLL_AXIS_DIAGONAL_DEAD_ZONE_DEG` → 대각선 구간 → **UNDECIDED 유지**, 계속 누적
     - `angle < 45° − SCROLL_AXIS_DIAGONAL_DEAD_ZONE_DEG` → `HORIZONTAL` 확정
     - `angle > 45° + SCROLL_AXIS_DIAGONAL_DEAD_ZONE_DEG` → `VERTICAL` 확정
   - 축이 한 번 결정되면 손가락을 뗄 때(UP)까지 변경 불가
   - UNDECIDED 상태에서는 스크롤 프레임을 전송하지 않음
3. 스크롤 입력 변환:
   - `VERTICAL`: 터치 드래그 Y축 누적량 → 수직 휠 스크롤 프레임 전송 (`createWheelFrame(delta)`)
   - `HORIZONTAL`: 터치 드래그 X축 누적량 → 수평 휠 스크롤 프레임 전송 (`createHorizontalWheelFrame(delta)` 신규 추가)
   - 정량적 스크롤: `SCROLL_UNIT_DISTANCE_DP` 이동당 1 스크롤 단위 전송 (소수점 누적으로 단위 손실 방지)
   - 감도 배수(`SCROLL_SENSITIVITY_*`) 적용 후 스크롤 단위 계산
   - 축 확정 시 누적된 이동량(임계값 이전 분)은 버리고 확정 시점 이후부터 새로 누적
4. 스크롤 감도 3단계 (`SCROLL_SENSITIVITY_*` 상수 사용):
   - 느림: `SCROLL_SENSITIVITY_SLOW` (×0.5)
   - 보통: `SCROLL_SENSITIVITY_NORMAL` (×1.0, 기본)
   - 빠름: `SCROLL_SENSITIVITY_FAST` (×2.0)
   - ScrollSensitivityButton으로 순환
5. 모드 전환 시 입력 정리:
   - 커서 모드 → 스크롤 모드: 진행 중 드래그 중단
   - 스크롤 모드 → 커서 모드: 스크롤 관성 정지, `scrollAxis` 초기화
6. `ScrollGuideline` Composable 기본 구현:
   - 위치: 터치패드 내부 (테두리 제외)
   - 표시 조건: 스크롤 모드에서 축 확정 후 첫 스크롤 단위 전송 시
   - 숨김 조건: 마지막 입력 후 `SCROLL_STOP_THRESHOLD_MS` 경과 시 정지 판정 → `SCROLL_GUIDELINE_HIDE_DELAY_MS` 대기 후 페이드아웃, 또는 모드 종료 시 즉시 숨김
   - 일반 스크롤 색상: `#84E268` (초록)
   - 방향 표시: 확정된 축(`VERTICAL` / `HORIZONTAL`)에 따라 수직선 또는 수평선으로 표시
   - **스크롤 속도 연동 이동 애니메이션**:
     - 각 스텝마다 고정 duration 애니메이션을 독립 실행하는 방식이 **아님** — 이 방식은 빠른 스크롤 시 이전 애니메이션 큐가 쌓여 뒤처지는 문제가 생김
     - **설계 방식: 목표 추적(target-chasing) + spring 애니메이션**
       - `Animatable<Float>` 하나로 가이드라인의 오프셋 위치를 관리
       - 스크롤 단위 전송 시마다 `targetOffset += SCROLL_GUIDELINE_STEP_DP` (또는 방향에 따라 -=)로 목표만 업데이트
       - `animatable.animateTo(targetOffset, animationSpec = spring(stiffness = ..., dampingRatio = ...))` 호출
       - Compose spring은 목표가 바뀌어도 **현재 속도·위치를 유지하며 새 목표로 자연스럽게 방향 전환** — 고정 duration 없이 속도에 따라 자동 조절됨
     - **스크롤 속도에 따른 자동 반응**:
       - 느린 스크롤: 목표가 드문드문 이동 → spring이 여유 있게 따라가며 감속 정지
       - 빠른 스크롤: 목표가 빠르게 앞서 나감 → spring이 강하게 가속해서 쫓아가며 감속 정지 → "확 치고 확 멈추는" 느낌이 자동으로 발생
     - spring 파라미터 상수화 (`ScrollConstants.kt`):
       - `SCROLL_GUIDELINE_SPRING_STIFFNESS`: stiffness (기본값 `Spring.StiffnessHigh` ≈ `10_000f`) — 높을수록 빠르고 즉각적
       - `SCROLL_GUIDELINE_SPRING_DAMPING`: dampingRatio (기본값 `Spring.DampingRatioNoBouncy` = `1f`) — 1이면 오버슈트 없이 딱 멈춤, 0.5 이하면 통통 튀는 느낌
     - 가이드라인이 터치패드 끝에 도달하면 반대쪽 끝으로 랩어라운드
   - **스크롤 단위마다 햅틱 피드백**:
     - 스크롤 단위 1이 전송될 때마다 `GESTURE_TICK` 또는 `KEYBOARD_TAP` 수준의 Light 햅틱
   - `TouchpadWrapper` 내부에서 `ScrollMode.NORMAL_SCROLL` 상태에 따라 표시/숨김

**신규 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/touchpad/ScrollGuideline.kt`
- `src/android/app/src/main/java/com/bridgeone/app/ui/common/ScrollConstants.kt` (스크롤 관련 조정 가능 상수 일괄 정의)

**수정 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/TouchpadWrapper.kt`
- `TouchpadMode.kt` (`ScrollAxis` enum 추가: `UNDECIDED`, `HORIZONTAL`, `VERTICAL`)
- `ClickDetector.kt` (`createHorizontalWheelFrame(delta)` 추가)

**참조 문서**:
- `docs/android/component-touchpad.md` §1.3.1.3 (ScrollModeButton)
- `docs/android/component-touchpad.md` §1.5 (스크롤 가이드라인)
- `docs/android/technical-specification-app.md` §2.2 (터치패드 알고리즘)

**검증**:
- [x] ScrollModeButton 탭으로 스크롤 모드 진입
- [x] 세로 드래그 시 수직 휠 프레임 전송 (PC에서 세로 스크롤 동작)
- [x] 가로 드래그 시 수평 휠 프레임 전송 (PC에서 가로 스크롤 동작)
- [x] 축 확정 후 대각선 드래그 시 확정된 축으로만 스크롤 (다른 축 무시)
- [x] 손가락 떼면 축 초기화, 다음 터치에서 새로 축 확정
- [x] 감도 3단계 전환 동작
- [x] 원탭으로 커서 이동 모드 복귀 (직전 드래그/관성 조건 제외)
- [x] PC에서 실제 세로/가로 스크롤 동작 확인 (하드웨어 E2E)
- [x] 드래그 시 초록색 가이드라인 표시 (축 방향에 맞게 수직선/수평선)
- [x] 스크롤 단위마다 가이드라인이 톡톡톡 끊기며 이동
- [x] 스크롤 단위마다 햅틱 피드백 발생
- [x] 스크롤 정지 후 가이드라인 자연스럽게 숨김
- [x] 모드 종료 시 가이드라인 즉시 숨김

---

## Phase 4.3.4: 무한 스크롤 모드 + 관성 + ScrollGuideline 색상 확장

> **⚠️ Phase 4.3.1 변경사항**: `ScrollMode.INFINITE_SCROLL` enum과 ScrollModeButton 전환 로직은 이미 구현됨. `TouchpadState.lastScrollMode`로 마지막 스크롤 모드 기억. 스크롤 버튼 탭=토글(ON/OFF), 롱프레스=NORMAL↔INFINITE 전환. 이 Phase에서는 관성 알고리즘과 TouchpadWrapper 연동, ScrollGuideline 무한 스크롤 확장에 집중.

> **⚠️ Phase 4.3.3 변경사항**:
> - **`ScrollGuideline.kt` 현재 파라미터**: `isVisible: Boolean`, `scrollAxis: ScrollAxis`, `targetOffset: Float`. 무한 스크롤 빨간색을 지원하려면 `scrollMode: ScrollMode` 파라미터 추가 후 내부에서 색상 분기 필요.
> - **`ScrollGuideline.kt` 구현 구조 변경**: 단일 선이 아닌 **등간격 다중 선 패턴** + **굵은/얇은 선 구분**(매 4번째 굵은 선). 항상 수평선으로 그린 뒤 `DrawScope.rotate()`로 축 방향 전환. 축 전환 시 `FastOutSlowInEasing` tween 회전 애니메이션 적용. 색상을 변경하려면 `ScrollGuidelineColor` (현재 private)를 파라미터화하거나 `scrollMode` 기반으로 내부 분기.
> - **`ScrollConstants.kt` 추가 상수**: `SCROLL_GUIDELINE_SPACING_DP = 40f` (선 간격), `SCROLL_GUIDELINE_AXIS_ROTATION_MS = 100` (축 전환 회전 애니메이션 시간).
> - **`TouchpadWrapper.kt` 스크롤 상태 변수들**: `guidelineVisible`, `guidelineAxis`, `guidelineTarget`(mutableFloatStateOf), `guidelineHideJob` — 이미 존재. 스크롤 틱마다 `guidelineVisible = true` 재설정하여 타이머 숨김 후 스크롤 재개 시 자동 재표시. 무한 스크롤 관성 루프는 `ScrollMode.INFINITE_SCROLL` 분기를 기존 `ScrollMode.NORMAL_SCROLL` 분기와 나란히 추가하면 됨.
> - **`scheduleGuidelineHide()`**: 이미 TouchpadWrapper 내부에 구현됨 — 재활용 가능.
> - **`ScrollConstants.kt`**: `SCROLL_STOP_THRESHOLD_MS`, `SCROLL_GUIDELINE_HIDE_DELAY_MS` 등 관련 상수 이미 정의됨. 관성 감쇠 계수는 이 파일에 추가할 것.
> - **HorizontalPager 제스처 충돌 방지**: `StandardModePage.kt`에서 스크롤 모드 활성 시 `userScrollEnabled = false` + 부모 Box에 `PointerEventPass.Initial` Move 이벤트 선제 소비 구현됨 — 무한 스크롤 모드에서도 동일하게 적용됨 (`touchpadState.scrollMode != ScrollMode.OFF` 조건).

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
- [x] 손가락 떼면 관성 스크롤 지속
- [x] 관성 감쇠로 자연스러운 정지
- [x] 관성 중 터치 시 즉시 정지
- [x] 감도 설정 적용
- [x] 무한 스크롤 중 빨간색 가이드라인 표시
- [x] 가이드라인이 손가락/관성과 1:1 연속 이동 (단위별 스텝 아닌 연속 추적)
- [x] 무한 스크롤 햅틱이 속도 비례 연속 진동 (빠르면 강하고, 감속 시 약해짐)
- [x] 일반 ↔ 무한 스크롤 전환 시 가이드라인 색상 즉시 반영

---

## Phase 4.3.5: 직각 커서 이동 모드

> **⚠️ Phase 4.3.1 변경사항**: `MoveMode.FREE` / `MoveMode.RIGHT_ANGLE` enum 및 MoveModeButton 전환 로직은 이미 구현됨. `TouchpadState.moveMode`로 상태 관리. 이 Phase에서는 `DeltaCalculator`에 축 잠금 알고리즘 추가와 `TouchpadWrapper`에서 `moveMode` 반영에 집중.

> **⚠️ Phase 4.3.3 변경사항**: `TouchpadWrapper.kt`의 커서 이동 분기는 `else { ... }` 블록 (스크롤 모드가 아닌 경우). 직각 이동 로직은 이 `else` 블록 안의 `finalDelta` 계산 이후에 `latestState.moveMode == MoveMode.RIGHT_ANGLE` 체크를 추가하면 됨. `latestState`는 `rememberUpdatedState`로 이미 제공됨.

> **⚠️ Phase 4.3.3 추가 변경사항**: `StandardModePage.kt`의 `Page1TouchpadActions`가 `touchpadState: TouchpadState`, `onTouchpadStateChange: (TouchpadState) -> Unit` 파라미터를 받는 구조로 변경됨. `touchpadState`는 `StandardModePage` 레벨에서 관리됨. 이 Phase에서 직각 이동은 `TouchpadWrapper` 내부에서 `latestState.moveMode`만 참조하므로 상위 구조 변경 불필요.

**목표**: X축 또는 Y축으로만 커서를 이동하는 축 잠금 알고리즘

**개발 기간**: 0.5-1일

**세부 목표**:
1. 직각 이동 알고리즘:
   - 터치 시작 후 12dp 이동 시 주축(X/Y) 결정 (하드웨어 테스트 후 30dp→12dp 조정)
   - 22.5도 데드밴드 (주축 전환 방지)
   - **주축 확정 전(UNDECIDED)**: 커서 이동 완전 차단 (`Offset.Zero` 반환) — 직각 모드 진입 시 커서가 의도치 않게 틀어지는 것을 방지
   - 주축이 결정되면 반대 축 이동량 = 0
   - 손가락 떼면 축 잠금 해제
2. MoveModeButton과 연동:
   - 자유 이동 ↔ 직각 이동 전환
   - 직각 이동 시 MoveModeButton에 "커서 자유 이동 모드" 텍스트 표시
3. 시각적 피드백:
   - `RightAngleGuideline.kt` — 십자 양방향 화살표 오버레이 (잠긴 축 밝게 / 차단 축 흐리게)
   - 색상: `Color(0xFFFF8A00)` (직각 이동 버튼 배경색과 동일)
   - 상단 여백: `topEdgePad = 80.dp` (컨테이너 버튼 영역 회피)
   - 페이드인/아웃: `animateFloatAsState(tween 250ms)`
   - 햅틱: 축 결정 순간 Light 1회

**신규 파일**:
- `RightAngleGuideline.kt` (`src/android/app/src/main/java/com/bridgeone/app/ui/components/touchpad/`)

**수정 파일**:
- `DeltaCalculator.kt` (`RightAngleAxis` enum, `determineRightAngleAxis()`, `applyRightAngleLock()` 추가)
- `TouchpadWrapper.kt` (직각 이동 제스처 로직 + 가이드라인 상태 연결)
- `ScrollConstants.kt` (`RIGHT_ANGLE_AXIS_LOCK_DISTANCE_DP`, `RIGHT_ANGLE_DEADBAND_DEG` 추가)

**참조 문서**:
- `docs/android/technical-specification-app.md` §2.2.3 (직각 이동 알고리즘)
- `docs/android/component-touchpad.md` §1.3.1.2 (MoveModeButton)

**검증**:
- [x] 12dp 이동 후 축 결정 정상 동작
- [x] 주축 확정 전 커서 이동 완전 차단
- [x] 직각 이동 중 반대 축 이동 차단
- [x] 손가락 떼면 축 잠금 해제
- [x] MoveModeButton 토글 동작
- [x] PC에서 수평/수직 직선 커서 이동 확인

---

## Phase 4.3.6: DPI 조절 시스템

> **⚠️ Phase 4.3.1 변경사항**: `DpiLevel` enum (LOW/NORMAL/HIGH, multiplier 포함)과 DPIControlButton 순환 로직은 이미 구현됨. `TouchpadState.dpiLevel`로 상태 관리. 이 Phase에서는 `DeltaCalculator`에 DPI 곱수 적용과 SharedPreferences 저장에 집중.

> **⚠️ Phase 4.3.2 변경사항**:
> - `ControlButton`에 `iconResId: Int` 파라미터 필수 추가됨. DPIControlButton 아이콘(`ic_slow`/`ic_normal`/`ic_fast`)은 `dpiButtonIcon()` 헬퍼로 이미 매핑 완료 → 아이콘 관련 작업 불필요.
> - 버튼 너비 6등분 → 5등분 변경 (DPI/ScrollSensitivity 동일 슬롯 공유).
> - disabled 투명도(alpha 0.4f) 제거됨 — 모든 버튼 항상 alpha 1f.

> **⚠️ Phase 4.3.3 변경사항**: `TouchpadWrapper.kt`의 커서 이동 `else` 분기에서 `finalDelta` 산출 후 `latestState.dpiLevel.multiplier`를 곱한 뒤 `coerceIn(-127f, 127f)` 적용. `latestState`는 `rememberUpdatedState`로 이미 제공됨.

> **⚠️ Phase 4.3.3 추가 변경사항**: `StandardModePage.kt`의 `Page1TouchpadActions`가 파라미터로 `touchpadState`/`onTouchpadStateChange`를 받는 구조로 변경됨. DPI 상태는 `TouchpadWrapper` 내부에서 `latestState.dpiLevel`로 접근하므로 추가 변경 불필요.

> **⚠️ Phase 4.3.4.5 변경사항**: `TouchpadWrapper.kt` DOWN 이벤트에 이벤트 소비 체크 패턴 추가됨:
> `if (down.changes.any { it.isConsumed }) return@awaitEachGesture`
> `DpiAdjustPopup`을 TouchpadWrapper Box 내에 오버레이로 배치 시, 팝업 내 `pointerInput`에서 `event.changes.forEach { it.consume() }` 호출하면 TouchpadWrapper 제스처 처리가 자동 차단됨 — 별도 분기 로직 불필요.

> **⚠️ Phase 4.3.5 변경사항**: 커서 이동 `else` 분기에서 `finalDelta` 이후 `axisLockedDelta`가 산출됨 (직각 이동 모드 축 잠금). DPI 곱수는 `finalDelta`가 아닌 **`axisLockedDelta`에 적용** 후 `coerceIn(-127f, 127f)` 처리해야 함 (직각 잠금 → DPI 순서). `ScrollConstants.RIGHT_ANGLE_AXIS_LOCK_DISTANCE_DP`(12f), `RIGHT_ANGLE_DEADBAND_DEG`(22.5f) 상수 추가됨. `DeltaCalculator`에 `RightAngleAxis` enum, `determineRightAngleAxis()`, `applyRightAngleLock()` 함수 추가됨. **주축 확정 전(UNDECIDED) `axisLockedDelta = Offset.Zero`** — 커서가 이동하지 않으므로 DPI 곱수 적용도 자동으로 0이 됨 (별도 처리 불필요).

**목표**: 터치패드 커서 감도를 3단계 + 커스텀 값으로 조절하는 DPI 시스템

**개발 기간**: 1일

**쉬운 설명**: DPI 버튼을 짧게 누르면 느림/보통/빠름이 바뀌고, 길게 누르면 터치패드 영역이 조절판이 되어 손가락을 밀면 원하는 정확한 배율 값을 고를 수 있습니다. 값을 정하면 버튼에 "1.3x"처럼 표시됩니다. 커스텀 값은 앱을 껐다 켜거나 케이블을 뽑으면 사라집니다.

**세부 목표**:
1. DPI 3단계 + 커스텀(임시 값):
   - 낮음: 델타 곱수 ×0.5 (정밀 작업용)
   - 보통: 델타 곱수 ×1.0 (기본)
   - 높음: 델타 곱수 ×2.0 (빠른 이동용)
   - 커스텀: 0.1x ~ 5.0x 범위에서 0.1 단위로 자유 설정 (임시 값 — 영속 저장 안 함)
2. DPIControlButton 탭 순환:
   - 커스텀 없을 때: 높음 → 낮음 → 보통 → 높음
   - 커스텀 있을 때: 커스텀 → **커스텀 값보다 큰 사전 정의 값 중 가장 작은 것** (이 순간 커스텀 소멸) → 이후 사전 정의 순환
     - 예: 커스텀 1.3x → 높음(2.0) → 낮음 → 보통 → 높음
     - 예: 커스텀 0.4x → 낮음(0.5) → 보통 → 높음 → 낮음
     - 예: 커스텀 0.7x → 보통(1.0) → 높음 → 낮음 → 보통
     - 예: 커스텀 3.0x (모든 사전 정의 값 초과) → 낮음(0.5)으로 wrap → 보통 → 높음 → 낮음
   - 아이콘/배경색: 커스텀일 때 탭 순환 연결 대상(커스텀 값보다 큰 사전 정의 값 중 가장 작은 것)의 아이콘·색상 사용
     - 예: 커스텀 1.3x → 높음 아이콘(`ic_fast.xml`) + 높음 색상
     - 예: 커스텀 0.4x → 낮음 아이콘(`ic_slow.xml`) + 낮음 색상
     - 예: 커스텀 3.0x → 낮음 아이콘(`ic_slow.xml`) + 낮음 색상 (wrap)
   - 텍스트: 사전 정의 레벨은 "DPI\n낮음/보통/높음", 커스텀은 "DPI\n1.3x" 형식
3. DPIControlButton 롱 프레스 → DPI 세밀 조절 팝업:
   - `DpiAdjustPopup` Composable 신규 작성
   - **터치패드 영역 안에만** 표시되는 반투명 오버레이
   - **블러 배경**: 팝업이 열리면 뒤쪽의 ControlButtonContainer + 터치패드 영역 전체에 블러 적용
     - `Modifier.blur(radius = 8.dp)` — 팝업 뒤 전체 콘텐츠에 적용
     - 적용 위치: `StandardModePage`(또는 `Page1TouchpadActions`) 최상위 Column/Box에 팝업 상태에 따라 조건부 블러
     - 블러 + 반투명 오버레이 `Color.Black.copy(alpha = 0.4f)` 중첩 (블러만으로는 조작 불가능함이 시각적으로 불분명)
     - 블러 진입/해제: `animateDpAsState`로 0.dp ↔ 8.dp 부드럽게 전환
   - 터치패드 정중앙에 현재 배율 값을 크게 표시 (예: "1.3x")
   - 터치패드 내 상하 또는 좌우 스와이프 → 드래그 거리에 비례하여 0.1 단위로 값 증감
     - 드래그 시작 기준값: 팝업 오픈 시점의 현재 배율
     - 범위 제한: 0.1x ~ 5.0x, 경계 초과 시도 시 피드백:
       - 배율 텍스트 잠시 붉은색으로 변경 후 원래 색으로 복귀
       - 텍스트 좌우 shake 애니메이션
       - 진동 피드백 (햅틱)
   - 터치패드 내 탭 → 현재 값 확정, 팝업 닫힘, 햅틱
   - **페이지 전환 발생 시** → 커스텀 값 적용 없이 팝업 취소
   - 사전 정의 값(0.5/1.0/2.0)과 정확히 일치할 경우 해당 DpiLevel로 자동 매핑
4. 커스텀 값 초기화 조건 (임시 값이므로 아래 이벤트 발생 시 `customDpiMultiplier = null`):
   - 탭 순환으로 사전 정의 값 전환 시
   - 앱 종료 후 재시작 시 (SharedPreferences에 저장 안 함)
   - USB 연결 끊김 이벤트 감지 시
   - 스크롤 모드 진입 시
5. `TouchpadState` 확장:
   - `customDpiMultiplier: Float? = null` 필드 추가 (null = 커스텀 없음)
   - DPI 유효 배율: `customDpiMultiplier ?: dpiLevel.multiplier`
6. `DeltaCalculator` 연동:
   - DPI 곱수를 `calculateAndCompensate()` 또는 `applyDeadZone()` 결과에 곱하여 적용
   - **구현 위치**: `DeltaCalculator`에 `dpiMultiplier: Float = 1.0f` 파라미터 추가, 또는 `TouchpadWrapper`에서 결과에 곱수 적용
   - 기존 데드존(5dp), `normalizeOnly()` 범위 정규화(-127~127)와 호환 필요
   - **⚠️ 곱수 적용 후 -127~127 범위 초과 가능** → `coerceIn()` 재적용 필요
7. 상태 저장:
   - DPI 레벨(사전 정의 값)은 앱 종료 후에도 유지 (SharedPreferences)
   - 커스텀 값은 저장하지 않음 (메모리에만 유지)

**신규 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/touchpad/DpiAdjustPopup.kt`

**수정 파일**:
- `TouchpadMode.kt` (`TouchpadState`에 `customDpiMultiplier` 추가)
- `DeltaCalculator.kt` (DPI 곱수 적용)
- `ControlButtonContainer.kt` (롱 프레스 핸들러 + 버튼 텍스트 커스텀 표시 + 탭 시 커스텀 소멸 처리)
- `StandardModePage.kt` 또는 `TouchpadWrapper.kt` (팝업 표시 상태 관리, 페이지 전환 취소 처리)
- USB 연결 끊김 핸들러 (커스텀 DPI 초기화 연동)

**참조 문서**:
- `docs/android/component-touchpad.md` §1.3.2.1 (DPIControlButton)
- `docs/android/component-touchpad.md` §1.3.2.3 (DpiAdjustPopup)
- `docs/android/component-touchpad.md` §3.2.5 (DPI 유저 플로우)

**검증**:
- [x] DPI 3단계 탭 순환 동작 (높음 → 낮음 → 보통 → 높음)
- [x] 낮음에서 정밀한 커서 이동
- [x] 높음에서 빠른 커서 이동
- [x] 롱 프레스 시 터치패드 영역 내 팝업 표시
- [x] 팝업 표시 시 ControlButtonContainer + 터치패드 전체 블러 배경 적용 확인
- [x] 블러 진입/해제 시 부드러운 애니메이션 전환 확인
- [x] 팝업 내 스와이프로 DPI 값 실시간 변경
- [x] 터치패드 영역 내 탭으로 값 확정 및 팝업 닫힘
- [x] 확정 후 버튼 텍스트 "1.3x" 형식 표시
- [x] 커스텀 상태에서 탭 순환 시 커스텀 소멸 후 사전 정의 순환 진입
- [x] 페이지 전환 시 팝업 취소 (커스텀 미적용)
- [x] 앱 재시작 후 커스텀 값 초기화 (레벨만 유지)
- [x] USB 끊김 시 커스텀 값 초기화
- [x] 스크롤 모드 진입 시 커스텀 값 초기화

---

## Phase 4.3.7: 앱 아이콘 시스템 (`AppIcons`)

**목표**: 앱 전역에서 사용하는 아이콘을 단일 파일에서 중앙 관리하는 시스템 구축. 향후 애니메이션 아이콘(Lottie 등) 도입 시 폴백 구조를 유지한 채 교체 가능하도록 확장성을 고려합니다.

**개발 기간**: 0.5일

**쉬운 설명**: 앱에서 쓰는 모든 아이콘을 한 곳에서 관리하는 "아이콘 목록표"를 만드는 작업입니다. 지금은 정적 아이콘만 쓰지만, 나중에 움직이는 아이콘으로 교체할 때도 이 목록표만 수정하면 앱 전체에 반영되도록 구조를 잡아둡니다.

**세부 목표**:

1. **`AppIconDef` 데이터 모델** (`AppIcons.kt`에 정의):
   ```kotlin
   data class AppIconDef(
       val staticIcon: ImageVector,   // 항상 존재 — Material Symbols 정적 아이콘
       // val animation: AppIconAnimation? = null  // 향후 Lottie/AVD 확장용 (현재 미구현)
   )
   ```

2. **`AppIcons` 중앙 관리 객체** (`AppIcons.kt` 신규):
   ```kotlin
   object AppIcons {
       // 포인터 다이나믹스 프리셋
       val DynamicsOff       = AppIconDef(Icons.Outlined.Speed)
       val DynamicsPrecision = AppIconDef(Icons.Filled.Adjust)
       val DynamicsStandard  = AppIconDef(Icons.Filled.Speed)
       val DynamicsFast      = AppIconDef(Icons.Filled.FlashOn)

       // DPI
       val DpiLow    = AppIconDef(Icons.Outlined.Mouse)
       val DpiNormal = AppIconDef(Icons.Filled.Mouse)
       val DpiHigh   = AppIconDef(Icons.Filled.KeyboardDoubleArrowRight)

       // 스크롤
       val ScrollUp   = AppIconDef(Icons.Filled.KeyboardArrowUp)
       val ScrollDown = AppIconDef(Icons.Filled.KeyboardArrowDown)

       // 모드
       val ScrollMode  = AppIconDef(Icons.Filled.SwapVert)
       val CursorMode  = AppIconDef(Icons.Filled.OpenWith)
   }
   ```
   - 아이콘 추가/교체는 이 파일만 수정하면 전체 반영
   - 아이콘 이름은 기능 기준으로 작명 (컴포넌트명·위치 기준 아님)

3. **`AppIcon` 래퍼 Composable** (`AppIcons.kt`에 함께 정의):
   ```kotlin
   @Composable
   fun AppIcon(
       def: AppIconDef,
       contentDescription: String?,
       modifier: Modifier = Modifier,
       tint: Color = LocalContentColor.current
   ) {
       // 현재: 정적 아이콘만
       // 향후: def.animation != null && animatedIconsEnabled → LottieAnimation 분기
       Icon(
           imageVector = def.staticIcon,
           contentDescription = contentDescription,
           modifier = modifier,
           tint = tint
       )
   }
   ```

4. **기존 코드 마이그레이션**:
   - `NormalScrollButtons.kt`의 `Icon(Icons.Default.KeyboardArrowUp/Down)` → `AppIcon(AppIcons.ScrollUp/Down)` 교체
     - `ScrollHoldButton` 파라미터: `ImageVector` → `AppIconDef`
   - `ControlButtonContainer.kt`: `Icons.*` 미사용 (커스텀 `R.drawable.*` 사용) — 마이그레이션 불해당
   - `DpiAdjustPopup.kt`: 아이콘 없음 — 마이그레이션 불해당
   - 이후 Phase에서 새로 추가되는 Material Icons는 처음부터 `AppIcons`에 등록 후 사용

**신규 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/common/AppIcons.kt`

**수정 파일**:
- `NormalScrollButtons.kt` (Icons.Default → AppIcons 교체)

**검증**:
- [x] `AppIcons.kt` 단일 파일에서 모든 아이콘 정의 확인
- [x] `AppIcon()` 래퍼로 기존 `Icon()` 직접 호출 대체 확인 (NormalScrollButtons.kt)
- [x] 기존 화면에서 아이콘 표시 이상 없음 (시각적 회귀 없음)
- [x] 새 아이콘 추가 시 `AppIcons.kt`만 수정하면 되는 구조 확인

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
| 스크롤 가이드라인 | ❌ | ✅ (스크롤 모드 시) |
| ControlButtonContainer | ❌ 숨김 | ✅ 표시 |
| 앱 아이콘 시스템 (AppIcons) | ✅ | ✅ |
