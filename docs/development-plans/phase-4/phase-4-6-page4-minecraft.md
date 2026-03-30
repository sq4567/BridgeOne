---
title: "BridgeOne Phase 4.6: Page 4 — Minecraft 특화 페이지"
description: "BridgeOne 프로젝트 Phase 4.6 - Standard 모드 Page 4: Touchpad(시점) + DPad(이동) + 게임 액션 버튼"
tags: ["android", "minecraft", "dpad", "game", "touchpad", "camera", "hotbar", "ui"]
version: "v1.0"
owner: "Chatterbones"
updated: "2026-03-26"
---

# BridgeOne Phase 4.6: Page 4 — Minecraft 특화 페이지

**개발 기간**: 4-5일

**목표**: Minecraft 플레이에 최적화된 한 손가락 조작 전용 페이지를 구현합니다. 좌측에 터치패드(시점)와 DPad(이동)를 상하 배치하고, 우측에 전투/이동/인벤토리/핫바 액션 버튼을 배치합니다.

**핵심 성과물**:
- 좌측: 터치패드 (시점/카메라, 60%) + DPad (WASD 이동, 40%)
- 우측: Combat & Use, Movement Support, Inventory/Utility, Hotbar 4개 그룹
- DPad: Sticky Hold (더블탭), 드래그 방향 전환, WASD 매핑
- 한 손가락 조작 최적화 (터치패드 ↔ DPad 즉시 전환)

**선행 조건**: Phase 4.3 (터치패드 고급 기능) 완료

**에뮬레이터 호환성**: Page 4 전체 레이아웃, DPad Sticky Hold 시각 피드백, 게임 액션 버튼 UI 에뮬레이터에서 개발 가능. WASD/마우스 실제 게임 내 반응 검증은 실기기에서 별도 수행.

---

## 현재 상태 분석

### 기존 구현
- `EssentialBootCluster` (`EssentialModePage.kt` 내 private Composable):
  - 기본 DPad (4방향): `KEY_UP`, `KEY_DOWN`, `KEY_LEFT`, `KEY_RIGHT` (Arrow 키 HID 코드)
  - `KeyboardKeyButton` 재사용, 3×3 그리드 중앙 비움 배치
  - `ClickDetector.createKeyboardFrame()` → `sendFrame()` 전송
- **Minecraft DPad과의 차이점**:
  - Essential DPad: Arrow 키 HID 코드 (0x52/0x51/0x50/0x4F)
  - Minecraft DPad: **WASD 키** HID 코드 필요 (W=0x1A, A=0x04, S=0x16, D=0x07)
  - Essential DPad: 4방향만, Minecraft DPad: **8방향(대각선 포함)** 필요
- Sticky Hold 없음, 대각선 미지원, 드래그 방향 전환 미지원
- **⚠️ DPad은 완전 새로 구현**: `EssentialBootCluster`의 DPad은 `KeyboardKeyButton` 조합이므로, Minecraft용 원형 DPad는 별도 컴포넌트(`DPad.kt`)로 신규 개발

### 목표 구조 (styleframe-page4.md 기준)
```
Page 4 — Minecraft
├── 좌측 (50%)
│   ├── Touchpad (카메라/시점) — 60% 높이
│   │   └── ControlButtonContainer (Click/Move/Scroll + DPI)
│   └── DPad (WASD 이동) — 40% 높이
└── 우측 (50%)
    ├── Combat & Use (Attack/Use/Pick Block)
    ├── Movement Support (Jump/Sneak/Sprint)
    ├── Inventory / Utility (E/Q/F/F5/ESC/T)
    └── Hotbar (1-9 숫자 버튼)
```

---

## Phase 4.6.1: Page 4 레이아웃 및 DPad 기본 구현

**목표**: 좌측 터치패드+DPad, 우측 액션 패널의 2열 레이아웃 구현

**개발 기간**: 1.5일

**세부 목표**:
1. `Page4Minecraft` Composable:
   - 2열 Row: 좌측 50% / 우측 50% (한 손가락 조작 최적화)
   - 소형 화면: 좌 45% / 우 55% 허용
2. 좌측 영역 (상하 배치):
   - 상단 60%: `TouchpadWrapper` (1:1.2 비율, 최소 280dp×336dp)
     - ControlButtonContainer 포함 (Phase 4.3에서 구현한 것 재사용)
     - 시점 이동 전용 (마우스 X/Y)
   - 하단 40%: `DPad` 컴포넌트
     - 정사각형, 최소 200dp
     - 12dp 라운드 코너
3. DPad 기본 구현:
   - 4방향 입력: Up/Left/Right/Down
   - **WASD 매핑** (Minecraft 기본): Up=W, Left=A, Down=S, Right=D
   - 영역 분할: 중심 원형 (반지름 25-30%) + 4방향 + 4대각선 = 8분할
   - 판정: 중심 기준 벡터 각도 45도 단위, 경계 허용오차 10도
   - 중앙 영역: 입력 없음 (무시)
4. DPad 피드백:
   - 탭: 해당 방향 KeyDown → KeyUp
   - 디바운스: 동일 방향 재탭 50ms 이내 무시
   - 햅틱: Light 1회 (탭/전환)
   - 시각: Selected 방향 `#2196F3` (alpha 0.3) 오버레이
5. 우측 Actions 패널:
   - `LazyColumn` 기반 세로 스크롤
   - 터치 타겟 ≥ 64dp (한 손가락 최적화)
   - 리플 비활성
   > **⚠️ Phase 4.2.6 변경사항**: Page 1 Actions 패널 기준으로 LazyColumn 아이템 간격 4dp, 그리드 행 간격 4dp, 열 간격 6dp, 버튼 높이 36dp로 변경됨. Page 4 Actions 패널 구현 시 동일 패턴 적용 또는 별도 조정 필요.

**신규 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/pages/standard/Page4Minecraft.kt`
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/DPad.kt`

**참조 문서**:
- `docs/android/styleframe-page4.md` §2 (레이아웃 구조)
- `docs/android/component-design-guide-app.md` §3 (DPad)

> **⚠️ Phase 4.1.7 변경사항**: Page 4 레이아웃은 `AppState.Active` 박스 내 `padding(top=40dp, bottom=40dp)` 적용 영역 안에서 렌더링됨. DPad(하단 40%) 및 Hotbar 등 하단 요소 치수 계산 시 유효 화면 높이 = 전체 높이 − 80dp 기준 사용. DPad 터치 영역 등 새 레이아웃 상수 정의 시 `ui/common/LayoutConstants.kt`에 추가.

> **⚠️ Phase 4.1.8 변경사항**: 커스텀 토스트 시스템 도입. `android.widget.Toast` 사용 금지. 모든 알림은 `ToastController.show(message, ToastType, durationMs)` 로 표시. 타입: `INFO`(파란색) · `SUCCESS`(초록색) · `WARNING`(주황색, 검은 텍스트) · `ERROR`(빨간색). 무제한 표시: `TOAST_DURATION_INFINITE`.

> **⚠️ StatusToast 개선 사항 (Phase 4.2 이후 반영)**: `StatusToast.kt` 대폭 업데이트 — 호출 측 API(`ToastController.show`) 변경 없음.
> 1. **타이머 테두리**: 유한 `durationMs` 토스트는 자동으로 남은 시간 비례 shrinking border 표시 — 호출 측 코드 변경 불필요.
> 2. **다중 토스트 스태킹**: 기존 토스트 표시 중 새 토스트 표시 시 → 새 토스트가 위에서 아래로 슬라이드인(350ms) → 완료 후 기존 토스트가 위로 슬라이드아웃(300ms). 두 토스트가 잠시 동시에 표시됨.
> 3. **중복 제거**: `ToastMessage.equals()`가 message·type·durationMs 내용 기준(내부 id 제외). 동일 내용의 `show()` 연속 호출 → StateFlow 충돌 방지 → 단일 토스트만 표시.

**검증**:
- [ ] 2열 50/50 레이아웃 정상
- [ ] 터치패드 + DPad 상하 배치 (60/40)
- [ ] DPad 4방향 탭 → WASD 키 전송
- [ ] 중앙 영역 터치 무시
- [ ] 햅틱 및 시각 피드백

---

## Phase 4.6.2: DPad 고급 기능 (Sticky Hold, 드래그 전환, 대각선)

**목표**: DPad Sticky Hold, 드래그 방향 전환, 대각선 입력 구현

**개발 기간**: 1.5일

**세부 목표**:
1. **Sticky Hold (더블탭)**:
   - 동일 방향 300ms 이내 연속 탭 → Sticky Hold 진입
   - Hold 중: 해당 방향 KeyDown 유지 (KeyUp 전송 안 함)
   - Hold 해제: 동일 방향 탭 또는 다른 방향 터치
   - 한 번에 한 방향만 Hold 가능
   - 상태: `currentStickyDirection`, `lastTapTime`, `lastTapDirection`
2. **Sticky Hold 시각적 구분**:
   - 일반 선택: `#2196F3` (alpha 0.3) 연한 파란색
   - Sticky Hold: `#1976D2` (alpha 0.5) 진한 파란색 + 2dp 테두리
   - 진입 애니메이션: 200ms scale (1.0 → 1.15 → 1.1) + 색상 전환
   - 해제 애니메이션: 150ms scale (1.1 → 0.95 → 1.0) + 색상 전환
   - 진입 햅틱: Medium 1회, 해제 햅틱: Light 1회
3. **드래그 방향 전환**:
   - 동일 포인터 유지 상태에서 섹터 간 전환 허용
   - 전환 시: 이전 방향 KeyUp → 새 방향 KeyDown (1프레임 내)
   - Sticky Hold 중에도 드래그 전환 허용 (Hold 방향 유지 + 새 방향 동시)
4. **대각선 입력**:
   - 대각선 섹터: 두 방향 동시 KeyDown/KeyUp (예: UpLeft = W+A)
   - Sticky Hold + 대각선 조합 가능
5. **안전 처리**:
   - Disabled 상태 진입 시 모든 Hold 강제 해제
   - 비정상 종료 시 Hold 상태 복구

**수정 파일**:
- `DPad.kt`

**참조 문서**:
- `docs/android/component-design-guide-app.md` §3.4 (Sticky Hold)
- `docs/android/component-design-guide-app.md` §3.1 (입력 처리 유저 플로우)

**검증**:
- [ ] 더블탭 Sticky Hold 진입/해제
- [ ] Hold 중 다른 방향 터치 시 Hold 해제
- [ ] 진한/연한 파란색 시각적 구분
- [ ] 드래그 방향 전환 시 키 전환 정확성
- [ ] 대각선 입력 (W+A, W+D 등) 동시 전송
- [ ] PC Minecraft에서 이동 + 시점 전환 테스트

---

## Phase 4.6.3: Combat & Movement 버튼

**목표**: 전투, 사용, 이동 보조 버튼 구현

**개발 기간**: 0.5-1일

> **⚠️ Phase 4.4.1 변경사항**: `KeyDisplayRegistry`가 도입됨. Movement 버튼(Jump=Space, Sneak=Shift, Sprint=Ctrl)에 `KeyboardKeyButton`을 사용할 경우 `useRegistry = true`로 아이콘 자동 표시 활용 가능. 단, 게임 맥락에서는 "Jump", "Sneak" 등 게임 용어 레이블이 더 적절할 수 있으므로 `keyLabel` 수동 지정이 나을 수 있음 — 구현 시 판단.

**세부 목표**:
1. **Combat & Use** (우측 상단):
   - Attack: LClick 단발/길게=지속 공격, 버튼 크기 ≥ 64dp
   - Use/Place: RClick 단발/길게=지속 사용, 버튼 크기 ≥ 64dp
   - Pick Block: MClick (선택적), 버튼 크기 ≥ 56dp
   - HID: 마우스 버튼 프레임 전송
     - Attack(LClick): `ClickDetector.createFrame(buttonState=1, 0, 0)` (bit0=Left)
     - Use/Place(RClick): `ClickDetector.createRightClickFrame(pressed)` 활용
     - Pick Block(MClick): `ClickDetector.createFrame(buttonState=4, 0, 0)` (bit2=Middle)
     - 길게 누르기: 누르기 시 pressed=true 프레임, 떼기 시 pressed=false 프레임 전송
2. **Movement Support** (우측 중간):
   - Jump (`Space`): 탭=단발 점프, 300ms 디바운스, 버튼 ≥ 64dp
   - Sneak (`Shift`): 탭=800ms 홀드, 더블탭=토글 유지, 버튼 ≥ 64dp
   - Sprint (`Ctrl`): 탭=800ms 홀드, 더블탭=토글 유지, 버튼 ≥ 64dp
   - Sneak/Sprint 동시 토글 충돌: 마지막 입력 우선, 이전 자동 해제
3. 버튼 배치:
   - 한 손가락 조작 최적화: 자주 사용하는 버튼을 엄지 접근 영역에 우선 배치
   - 버튼 간 12dp 이상 간격 (오조작 방지)

**수정 파일**:
- `Page4Minecraft.kt`

**참조 문서**:
- `docs/android/styleframe-page4.md` §6.1 (Combat & Use)
- `docs/android/styleframe-page4.md` §4 (Movement 보조 버튼)

**검증**:
- [ ] Attack 길게 누르기 → 지속 공격 동작
- [ ] Sneak 더블탭 토글 → PC에서 웅크리기 유지
- [ ] Sprint 토글 + Sneak 토글 충돌 → 마지막 우선
- [ ] Jump 탭 → 점프 1회

---

## Phase 4.6.4: Inventory/Utility 및 Hotbar

**목표**: 인벤토리, 유틸리티, 핫바 버튼 구현

**개발 기간**: 0.5-1일

**세부 목표**:
1. **Inventory/Utility** (우측 하단 중간):
   - Inventory (`E`): 버튼 ≥ 56dp
   - Drop (`Q`): 단발, 길게=연속 드롭 (기본 Disabled 옵션 제공)
   - Swap (`F`): 손 아이템 교환, 버튼 ≥ 56dp
   - Perspective (`F5`): 토글형, 버튼 ≥ 56dp
   - Pause/Menu (`ESC`): 버튼 ≥ 56dp
   - Chat (`T`): 버튼 ≥ 56dp
2. **Hotbar** (우측 최하단):
   - 숫자 1-9 버튼 (수평 배치)
   - 버튼 크기 ≥ 48dp (9개이므로 상대적 작게)
   - 또는 Wheel Up/Down 2개 버튼으로 대체 가능
   - 연속 휠 스크롤: 25-30Hz 상한 스로틀링
3. 반응형:
   - 소형 화면: Hotbar 2줄 배치 (1-5, 6-9)
   - 대형 화면: Hotbar 1줄 고정
   - 높이 제약: Utility 그룹 우선 스크롤

**수정 파일**:
- `Page4Minecraft.kt`

**참조 문서**:
- `docs/android/styleframe-page4.md` §6.3 (Inventory/Utility)
- `docs/android/styleframe-page4.md` §6.4 (Hotbar)

**검증**:
- [ ] 인벤토리(E) 탭 → PC에서 인벤토리 열림
- [ ] Hotbar 1-9 각 버튼 정상 동작
- [ ] Drop(Q) 길게 누르기 연속 드롭 (Disabled 옵션)
- [ ] Minecraft에서 전체 게임 플레이 테스트

---

## Phase 4.6 완료 후 Page 4 구조

```
Page 4 — Minecraft Specialized
├── 좌측 (50%)
│   ├── Touchpad (60% 높이)
│   │   ├── ControlButtonContainer (Click/Move/Scroll/DPI)
│   │   └── 시점 이동: 마우스 X/Y
│   └── DPad (40% 높이)
│       ├── 4방향 + 4대각선 (WASD 매핑)
│       ├── Sticky Hold (더블탭)
│       └── 드래그 방향 전환
└── 우측 (50%)
    ├── Combat & Use
    │   └── [Attack] [Use/Place] [Pick Block]
    ├── Movement Support
    │   └── [Jump] [Sneak ☆토글] [Sprint ☆토글]
    ├── Inventory / Utility
    │   └── [E] [Q] [F] [F5] [ESC] [T]
    └── Hotbar
        └── [1][2][3][4][5][6][7][8][9]
```
