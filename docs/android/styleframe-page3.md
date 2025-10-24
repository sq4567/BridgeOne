---
title: "Styleframe - Page 3 (Minecraft-specialized)"
description: "Minecraft 특화 페이지. 좌측 Movement Cluster(DPad) + 우측 Camera & Actions"
tags: ["styleframe", "minecraft", "dpad", "camera", "hotbar", "ui"]
version: "v0.2"
owner: "Chatterbones"
updated: "2025-09-22"
note: "본 문서에 존재하는 모든 상수값 및 설정값은 초기 값으로, 확정된 고정값이 아님"
---

# Page 3 스타일프레임 문서

## 1. 개요

이 문서는 Minecraft 플레이에 특화된 Page 3의 스타일프레임을 정의합니다. **한 손가락 조작에 최적화**되어 좌측에 터치패드(시점 제어)와 DPad(이동)를 상하 배치하고, 우측에 전투/인벤토리/핫바 등 액션 버튼들을 배치합니다.

> **상세 아키텍처**: 전체 시스템 아키텍처는 [`technical-specification.md` §3]를 참조하세요.
> **용어 정의**: 용어 정의는 [`technical-specification.md` §6.2 Android 플랫폼 용어집]을 참조하세요.

**참조**: `docs/design-guide-app.md` §1.2.1(DPad 예외 규칙), §5(토스트/인디케이터/햅틱), §9(토큰), `docs/component-design-guide.md` §2(버튼/단축키), `docs/touchpad.md` §3(터치 가이드), `docs/usb-hid-bridge-architecture.md`(HID 주입 규칙).

## 2. 레이아웃 구조

- 2-열 구조: 좌측 Touchpad+DPad(상하 배치), 우측 Actions.
- 권장 비율(가로 기준): 좌측 50% / 우측 50% (한 손가락 조작 최적화). 소형 화면에서는 좌 45%/우 55%까지 허용.
- 여백: 바깥 16dp, 컬럼 간 12dp, 그룹 간 세로 12~16dp.
- **한 손가락 조작 최적화**: 좌측 영역에 터치패드와 DPad를 상하 배치하여 엄지손가락으로 쉽게 전환 조작 가능.

### 2.1 좌측 Touchpad 영역 (상단)

- 배치: 좌측 상단에 배치, 상하 중앙 정렬.
- 크기: 좌측 컬럼 폭의 100% 사용, 좌측 영역 높이의 60% 사용.
- 종횡비: 1:1.2 (Width:Height), 최소 폭 280dp, 최소 높이 336dp.
- 모서리/마스크: 12dp 라운드, 상태 우선순위에 따른 테두리 표시(`docs/touchpad.md` §2.2, §3 참조).
- 모드/옵션: 패드 내부 상단 15% 높이에 `ControlButtonContainer`(Click/Move/Scroll/Cursor, DPI/Scroll 감도) 오버레이.
- **한 손가락 조작**: 엄지손가락으로 시점 조작 후 하단 DPad로 쉽게 전환 가능.

게임 매핑 메모:
- Touchpad 드래그=시점 이동(마우스 X/Y). 스크롤 모드=Hotbar 스크롤(Wheel step), 클릭 모드=Attack/Use.

### 2.2 좌측 DPad 영역 (하단)

- 배치: 좌측 하단에 배치, 터치패드 바로 아래.
- 크기: 좌측 컬럼 폭의 100% 사용, 좌측 영역 높이의 40% 사용.
- 형식: 4방향(Up/Left/Right/Down), 정사각형, 최소 변 200dp, 라운드 12dp.
- **한 손가락 조작**: 터치패드에서 엄지손가락을 아래로 이동하여 즉시 조작 가능.
- 피드백: 방향 전환 시 Light 햅틱, 오류 입력 시 Error 햅틱.

### 2.3 우측 Actions 패널

- 스크롤 컨테이너. 상단에서 하단: Combat & Use, Movement Support, Inventory/Utility, Hotbar.
- 그룹 헤더: 굵게, 보조 캡션 12sp. 터치 타겟 ≥ 64dp (한 손가락 조작 최적화), 간격 12dp, 리플 비활성.
- **한 손가락 조작**: 엄지손가락이 닿기 쉬운 위치에 자주 사용하는 버튼 우선 배치.

### 2.4 Touchpad 통합(@touchpad.md)

- 기반 컴포넌트: Page 1과 동일한 전체 `TouchpadWrapper` + `TouchpadAreaWrapper` + `ControlButtonContainer`를 그대로 사용합니다.
  - 참조: `docs/touchpad.md` §1.1~§1.6, §2, §3, §5.
- 게임 특화 매핑:
  - Click 모드: 좌클릭=Attack, 우클릭=Use/Place.
  - 스크롤 모드: Hotbar 스크롤로 매핑(Wheel step).
  - 커서 모드: 싱글/멀티 커서 정책은 기본 규칙 준수(멀티 커서가 필요 없다면 기본 싱글 유지).

### 2.5 ASCII 레이아웃(개략)

```text
┌──────────────────────────────────────┐  ┌──────────────────────────────────────┐
│           Touchpad (Camera)          │  │        Combat & Use                  │
│  [Control Buttons · 15% overlay]     │  │   Attack   Use   Pick Block         │
│  Click · Move · Scroll · Cursor      │  │     (LClick) (RClick) (MClick)      │
├──────────────────────────────────────┤  ├──────────────────────────────────────┤
│              DPad                    │  │     Movement Support                 │
│               [↑]                    │  │   Jump   Sneak   Sprint             │
│           [←] [ ] [→]                │  │   (Space) (Shift) (Ctrl)            │
│               [↓]                    │  ├──────────────────────────────────────┤
└──────────────────────────────────────┘  │     Inventory / Utility              │
                                          │   E  Q  F  F5  ESC  T               │
                                          ├──────────────────────────────────────┤
                                          │              Hotbar                  │
                                          │   [1][2][3][4][5][6][7][8][9]       │
                                          └──────────────────────────────────────┘
  «권장 비율: 좌 50% / 우 50%, 한 손가락 조작 최적화»
```

## 3. DPad(필수)

- 형식: 4방향(Up/Left/Right/Down). 기본 단방향 입력. 대각선은 기본 미지원.
- 예외 규칙: 동일 포인터 유지 상태에서 드래그로 방향 전환 허용(`docs/design-guide-app.md` §1.2.1).
- 크기/비율: 정사각형, 최소 변 200dp(한 손가락 조작 최적화), 라운드 12dp.
- 피드백: 방향 전환 시 Light 햅틱. 오류 입력 시 Error 햅틱.
- 상태: 입력 중 `Selected`, 비입력 `Unselected`.
- **한 손가락 조작**: 터치패드에서 엄지손가락을 아래로 이동하여 즉시 조작 가능.

HID 매핑(기본):
- Up=W, Left=A, Down=S, Right=D.
- 입력 지속 시 키다운 유지, 해제 시 키업. 전환 시(예: W→A) 이전 키업 후 다음 키다운.

## 4. Movement 보조 버튼

### 4.1 Jump(Space)
- 탭: 단발 점프.
- 길게: 점프 유지(게임 내 의미 제한적, 기본 비권장). 디바운스 300ms.

### 4.2 Sneak(Shift)
- 탭: 800ms 홀드.
- 더블탭: 토글 유지(상태 `Selected`), 다시 더블탭 또는 길게 누르기 해제로 복귀.

### 4.3 Sprint(Ctrl)
- 탭: 800ms 홀드.
- 더블탭: 토글 유지. Sneak와 동시 토글 충돌 시 마지막 입력 우선, 이전 상태 자동 해제.

## 5. Camera Touch Area(시점)

- 목적: 마우스 이동만 담당. 스크롤/버튼은 액션 그룹에서 제공.
- 비율: 1:1.2(가로:세로) 소형 패드, 최소 폭 200dp.
- 감도: 기본 Normal. `docs/touchpad.md`의 DPI/감도 정책을 약식 적용.
- 제스처: 드래그=시점 이동. 더블탭/롱프레스는 미사용.

HID 매핑: 마우스 X/Y 상수 입력. i16→−127..127 분할 주입 규칙 준수(`docs/usb-hid-bridge-architecture.md`).

## 6. Actions(전투/인벤토리/핫바)

### 6.1 Combat & Use
- Attack: LClick(단발). 길게=지속 공격. **버튼 크기 64dp 이상**.
- Use/Place: RClick(단발). 길게=지속 사용/설치. **버튼 크기 64dp 이상**.
- Pick Block: MClick(선택적, 리소스 팩/크리에이티브에 유용). **버튼 크기 56dp 이상**.
- **한 손가락 조작**: 우측 상단에 배치하여 엄지손가락이 가장 쉽게 접근 가능.

### 6.2 Movement Support
- Jump: `Space`(단발). **버튼 크기 64dp 이상**.
- Sneak: `Shift`(토글). **버튼 크기 64dp 이상**.
- Sprint: `Ctrl`(토글). **버튼 크기 64dp 이상**.
- **한 손가락 조작**: DPad와 함께 사용하기 위해 우측 중간에 배치.

### 6.3 Inventory/Utility
- Inventory: `E`. **버튼 크기 56dp 이상**.
- Drop: `Q`(단발), 길게=연속 드롭 허용(주의: 기본 비권장, `Disabled` 옵션 가능). **버튼 크기 56dp 이상**.
- Swap items in hands: `F`. **버튼 크기 56dp 이상**.
- Perspective: `F5` 토글. **버튼 크기 56dp 이상**.
- Pause/Menu: `ESC`. **버튼 크기 56dp 이상**.
- Chat: `T`. **버튼 크기 56dp 이상**.

### 6.4 Hotbar
- 숫자 1~9 버튼 또는 Wheel Up/Down(두 개의 작은 버튼) 제공.
- **버튼 크기 48dp 이상** (9개 버튼이므로 상대적으로 작게).
- 연속 휠 스크롤은 25~30Hz 상한으로 스로틀.
- **한 손가락 조작**: 우측 하단에 배치하여 엄지손가락으로 접근 가능.

## 7. 상호작용/상태/피드백

- 디바운스: 버튼/단축키 500ms(휠/시점 제외). Danger 액션은 기본 `Disabled`.
- 토스트: 주요 상태 전환/토글에만 사용. 과다 피드백 방지(토스트+햅틱 중복 금지).
- 접근성: 버튼 라벨을 명령형으로 간단히. 상태/토글 여부를 리드아웃.
- **한 손가락 조작 최적화**: 
  - 버튼 간 충분한 간격 (12dp 이상)으로 오조작 방지.
  - 명확한 시각적 구분과 햅틱 피드백으로 터치 확인.
  - 자주 사용하는 버튼을 엄지손가락이 닿기 쉬운 위치에 우선 배치.

## 8. 반응형/적응

- **소형 화면 (폭 < 360dp)**: 
  - 좌측 영역을 45%로 축소, 우측을 55%로 확대.
  - 터치패드와 DPad 크기를 최소값으로 조정.
  - Hotbar를 2줄로 배치 (1-5, 6-9).
  - 액션 버튼들을 수평 스크롤로 배치.
- **중형 화면 (360dp ≤ 폭 < 600dp)**: 
  - 기본 레이아웃 유지 (좌 50% / 우 50%).
  - 버튼 크기와 간격 최적화.
- **대형 화면 (폭 ≥ 600dp)**: 
  - 좌측 영역을 55%로 확대, 우측을 45%로 축소.
  - 터치패드와 DPad를 모두 확대.
  - Hotbar 숫자 1~9를 한 줄로 고정 표시.
  - 모든 액션 버튼을 고정 표시.
- **높이 제약**: Utility 그룹(Chat/Pause 등)을 우선 스크롤 이동.
- **한 손가락 조작**: 모든 화면 크기에서 엄지손가락 접근성 유지.

## 9. 구현 메모(개발자용)

- HID 키 프레이밍: 키다운→키업. DPad 전환 시 이전 키업 후 다음 키다운 순서 보장.
- 시점 입력: i16 분할 주입, 프레임당 60Hz 목표. 입력 지연 < 50ms 유지.
- 상태 저장: Sneak/Sprint 토글, 마지막 Hotbar, Camera 감도. 비정상 종료 복구 시 세션 동기화.
- Disabled 처리: 연속 드롭/위험 조합은 정책에 따라 기본 `Disabled` 설정.
- **한 손가락 조작 구현**:
  - 터치패드와 DPad 간 전환 시 부드러운 애니메이션 제공.
  - 동시 조작 지원: DPad + 액션 버튼, 터치패드 + 액션 버튼.
  - 버튼 크기와 간격을 Material Design 가이드라인에 따라 최적화.
  - 햅틱 피드백으로 터치 확인 및 방향 전환 알림.

---

문서 간 역할 분리: UI 정책은 `docs/design-guide-app.md`, 컴포넌트 동작은 `docs/component-design-guide.md`, 터치/시점/감도는 `docs/touchpad.md`, HID 세부는 `docs/usb-hid-bridge-architecture.md`를 우선 참조하세요.


