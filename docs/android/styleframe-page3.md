---
title: "Styleframe - Page 3 (AbsolutePointingPad)"
description: "절대좌표 패드 전용 페이지. 터치 위치가 곧 PC 커서 위치가 되는 절대좌표 포인팅 + 줌 기능"
tags: ["styleframe", "absolute-pointing", "pointing-pad", "zoom", "ui"]
version: "v0.3"
owner: "Chatterbones"
updated: "2026-07-06"
note: "본 문서에 존재하는 모든 상수값 및 설정값은 초기 값으로, 확정된 고정값이 아님"
---

# Page 3 스타일프레임 문서

## 1. 개요

이 문서는 AbsolutePointingPad 전용 페이지(Page 3)의 스타일프레임을 정의합니다. 터치한 위치가 곧 PC 커서의 절대 위치가 되는 "펜 태블릿" 방식의 포인팅 페이지입니다.

> **상세 아키텍처**: 전체 시스템 아키텍처는 [`technical-specification.md` §3]를 참조하세요.
> **용어 정의**: 용어 정의는 [`technical-specification.md` §6.2 Android 플랫폼 용어집]을 참조하세요.

**참조**: `docs/android/component-design-guide-app.md` §4(AbsolutePointingPad 컴포넌트 설계), `docs/android/design-guide-app.md` §5(토스트/인디케이터/햅틱), `docs/android/technical-specification-app.md` §2.10(구현 요구사항).

**페이지 순서 내 위치**:
- Page 1: 터치패드 + Actions (상대좌표)
- Page 2: 풀 와이드 터치패드 (멀티 커서)
- **Page 3: 절대좌표 패드** ← 이 문서
- Page 4: 키보드 중심
- Page 5: Minecraft 특화

## 2. 레이아웃 구조

> **⚠️ 설계 변경(사용자 확정) — 16:9 강제 폐기 + 하단 ControlBar 폐기**: 절대좌표 패드는 **Standard 모드 전용 페이지**로 재설계되었다(`BridgeOneApp.kt`가 `bridgeMode`로 `EssentialModePage()`/`StandardModePage()`를 완전히 분리 라우팅하므로, Essential 모드에서는 이 페이지 자체가 렌더링되지 않는다). 서버 중계 경로 하나만 사용하도록 단순화되면서(`technical-specification-app.md` §2.10) 다음이 바뀌었다.
> 1. **PointingArea 비율 자유화**: 16:9/16:10 강제와 letterbox/pillarbox 규칙을 폐기하고, **stretch 매핑**(패드 전 영역을 대상 화면 전체에 균등 매핑)을 전제로 유저가 비율을 자유롭게 선택한다(§2.1, §2.10.7 참조)
> 2. **전용 하단 ControlBar 폐기**: 아래 §2.2는 더 이상 유효하지 않다. 다른 터치패드 페이지와 동일하게 **상단 `ControlButtonContainer` 오버레이**를 재사용하며, ClickModeButton + 신규 ZoomButton + 신규 DragModeButton만 노출한다(ScrollToggleButton은 절대좌표에서 성립하지 않아 배제, `phase-4-9-page3-absolute-pointing.md` 참조)
> 3. **QuickKeyStrip/SwapButton은 영향 없음** — 본 변경은 PointingArea 비율과 제어 버튼 위치에만 해당

- PointingArea가 페이지의 대부분을 차지하고, 상단에 `ControlButtonContainer` 오버레이, 하단에 QuickKeyStrip이 배치됩니다.
- Page 1과 달리 좌/우 분할 구조가 아닌, 전체 화면 단일 영역 구조입니다.
- 여백: 바깥 16dp.
- 방향: Portrait 최적화. Landscape에서는 PointingArea가 자동으로 가로 확장.

### 2.1 PointingArea (메인 터치 영역)

- **배치**: 페이지 중앙, 가용 공간 최대 활용
- **비율**: 유저 자유 설정. **기본값 = Fill**(가용 공간 전체를 별도 설정 없이 채움), 프리셋 로우(16:9/21:9/4:3)로 원탭 전환 가능. 자세한 UX는 `technical-specification-app.md` §2.10.7 참조
- **최소 크기**: 프리셋별 최소 크기 하한만 적용 (구체 값은 구현 시 `PadRatioConfig` 상수로 확정)
- **최대 크기**: 가용 화면에서 QuickKeyStrip과 `ControlButtonContainer` 영역을 제외한 전체
- **모서리**: 8dp 라운드 코너
- **테두리**: 2dp 두께, 상태에 따른 색상 변화 (`component-design-guide-app.md` §4.3, §4.5.7 참조)

**매핑 규칙**:
- PointingArea는 letterbox/pillarbox 보정을 하지 않는다 — **stretch 매핑**으로 패드 전 영역이 항상 대상 화면(모니터) 전체에 도달한다
- 패드 비율과 모니터 비율이 다르면 이동 감도가 축(가로/세로)별로 달라질 수 있음(의도된 트레이드오프 — 화면 전 영역 도달을 우선)

시각 토큰:
- 배경: `#1E1E1E` (진한 회색, 터치패드와 동일)
- 기본 테두리: `#E91E63` (핑크색)
- 우클릭 모드 테두리: `#F3D021` (노란색)
- 드래그 모드 테두리: `#84E268` (초록색, 구 스크롤 모드 색상 재사용)
- 줌 활성 테두리: `#FF9800` (주황색)

### 2.2 (폐기됨) ControlBar → `ControlButtonContainer` 상단 오버레이 재사용

> 이 절은 더 이상 유효하지 않다. 하단 전용 ControlBar 대신 다른 터치패드 페이지와 동일한 상단 `ControlButtonContainer`를 재사용하며, 절대좌표에서 유효한 버튼만 노출한다(`ControlButtonConfig` 필터링).

**버튼 구성**:

```
ControlButtonContainer (상단 오버레이)
├── ClickModeButton (좌클릭 ↔ 우클릭 전환)
├── ZoomButton (줌 모드 진입/해제, DPI 슬롯 자리)
└── DragModeButton (신규 — 커서 이동만 vs 누른 채 이동, ScrollSensitivity 슬롯 자리)
```

- **ClickModeButton**: 터치패드의 동일 컨트롤과 같은 디자인. 좌클릭(기본)/우클릭 토글
- **ZoomButton**: 줌 모드 진입/해제. 활성(>1x) 시 배율 배지 표시 (예: "2x")
- **DragModeButton** (신규): OFF(기본)=커서 이동만, ON=터치 다운~업 동안 좌클릭 버튼을 누른 채 유지(드래그 앤 드롭). 활성 시 테두리 초록색. 상세 동작은 `technical-specification-app.md` §2.10.5 참조
- Move/Scroll/Cursor/DPI/ScrollSensitivity 버튼은 노출하지 않는다(델타 벡터 연산 기반이라 절대좌표에서 성립하지 않음)

각 버튼: 터치 타겟 ≥ 48dp, 아이콘 24dp, 리플 비활성.

### 2.2b MonitorSelector (신규, Standard 모드 전용)

- **배치**: `ControlButtonContainer` 근처 또는 PointingArea 상단 모서리(구현 시 확정)
- **구성**: "전체" 칩 + 모니터 개수만큼 번호 칩(1, 2, 3...)
- **동작**: 칩 선택 → 이후 전송되는 좌표 명령의 `targetMonitor` 값 갱신
- **표시 조건**: 모니터 개수 ≥ 2일 때만 노출(페이지 자체가 Standard 전용이므로 별도 모드 분기 불필요)
- 상세: `technical-specification-app.md` §2.10.6

### 2.3 QuickKeyStrip (퀵 특수키 스트립)

- **배치**: PointingArea와 ControlBar 사이
- **높이**: 40dp
- **구성**: 한 줄 6개 키, 가용 폭에 균등 분배
- **간격**: 키 간 8dp
- **배경**: 투명 (페이지 배경과 동일)

**키 구성**:

| 키 | 라벨 | 비고 |
|----|------|------|
| `Esc` | Esc | 단발성 |
| `Tab` | Tab | 단발성 |
| `Backspace` | ⌫ | 길게 누르기: 반복 입력 지원 |
| `Delete` | Del | 단발성 |
| `Enter` | ↵ | 길게 누르기: 반복 입력 지원 |
| `Space` | ␣ | 단발성 |

- **컴포넌트**: Page 1의 Special Keys와 동일한 `KeyboardKeyButton` 재사용
- **시각 토큰**: Page 1 Special Keys 버튼 스타일과 동일 (`design-guide-app.md` §9 참조)
- **리플 피드백**: 비활성 (터치패드 영역 근처이므로 시각 노이즈 최소화)
- **햅틱**: 키 입력 시 Light (30ms)

### 2.4 SwapButton (패드/키 영역 교체 버튼)

- **배치**: PointingArea 내부 우하단 모서리 오버레이. 모서리에서 8dp 안쪽
- **크기**: 터치 타겟 48dp × 48dp, 아이콘 20dp
- **아이콘**: 위아래 화살표 (swap / swap_vert)
- **배경**: 반투명 원형, `#000000` alpha 0.35
- **리플**: 비활성

**동작**:
- 탭 → PointingArea와 QuickKeyStrip의 **수직 위치를 즉시 교체**
  - **Pad-Top 상태** (기본): PointingArea 위 / QuickKeyStrip 아래
  - **KB-Top 상태**: QuickKeyStrip 위 / PointingArea 아래
- ControlBar는 항상 최하단 고정, 교체 대상이 아님
- 상태는 페이지 전환 시 유지 (SharedPreferences)

**햅틱**: 탭 시 Medium (50ms)

### 2.5 CoordinateIndicator (터치 위치 표시)

- **위치**: PointingArea 내부, 현재 터치 좌표 위
- **형태**: 십자선 (가로 + 세로 1dp 선, 길이 20dp) + 중앙 점 (4dp 원)
- **색상**: `#FFFFFF` (alpha 0.6)
- **동작**: 터치 중일 때만 표시, 터치 종료 후 300ms 페이드 아웃
- **목적**: 손가락에 가려진 정확한 터치 위치를 시각적으로 확인

### 2.6 줌 시각 피드백

#### Android 앱 내 (PointingArea)

- **줌 레벨 텍스트**: PointingArea 우상단 모서리, 14sp, `#FF9800`
- **줌 진입 중 (드래그 단계)**: 드래그에 따라 줌 레벨이 실시간 변화

#### PC 화면 (Windows 서버 오버레이, Standard 모드 전용)

줌이 활성화된 상태(>1x)에서 **PC 모니터 위**에 줌 영역 박스가 표시됩니다:

- **줌 영역 박스**: PC 전체 화면 위에 반투명 사각형 오버레이로, 현재 패드가 매핑하는 영역을 표시
  - 테두리: `#FF9800` (alpha 0.8), 2px
  - 배경: `#FF9800` (alpha 0.08)
- **줌 레벨 라벨**: 박스 우상단 외부에 배율 표시 (예: "2.0x"), 14pt, `#FF9800`
- **실시간 업데이트**: 줌 레벨/중심점 변경 시 박스 위치·크기 즉시 갱신
- **1x 시**: 박스 비표시 (전체 화면이므로 별도 표시 불필요)
- **멀티 모니터**: 줌 박스는 MonitorSelector(§2.2b)로 선택한 대상 모니터(또는 전체 가상 데스크톱) 기준으로 그려짐
- **통신 경로**: Android → ESP32 (UART) → Windows 서버 (Vendor CDC) → WPF 투명 오버레이 윈도우
- **상세 구현**: `technical-specification-server.md` §3.6.1.4, §3.6.9 참조

### 2.7 ASCII 레이아웃 (개략)

**Pad-Top 상태** (기본):
```text
┌──────────────────────────────────────────────────────────────────────────────┐
│      [ClickMode] [Zoom] [DragMode]  [전체|1|2] ← ControlButtonContainer +   │
│                                                    MonitorSelector(상단)     │
│   ┌──────────────────────────────────────────────────────────────────────┐   │
│   │                                                                      │   │
│   │                    PointingArea (자유 비율, Fill 기본)                │   │
│   │                                                                      │   │
│   │                           ＋  ← CoordinateIndicator                 │   │
│   │                                                                      │   │
│   │                          [2.0x] ← 줌          [⇅] ← SwapButton     │   │
│   └──────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
│     [Esc]  [Tab]  [⌫ ]  [Del]  [ ↵ ]  [  ␣  ]  ← QuickKeyStrip            │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

**KB-Top 상태** (교체 후):
```text
┌──────────────────────────────────────────────────────────────────────────────┐
│      [ClickMode] [Zoom] [DragMode]  [전체|1|2] ← ControlButtonContainer +   │
│                                                    MonitorSelector(상단)     │
│     [Esc]  [Tab]  [⌫ ]  [Del]  [ ↵ ]  [  ␣  ]  ← QuickKeyStrip            │
│                                                                              │
│   ┌──────────────────────────────────────────────────────────────────────┐   │
│   │                                                                      │   │
│   │                    PointingArea (자유 비율, Fill 기본)                │   │
│   │                                                                      │   │
│   │                           ＋  ← CoordinateIndicator                 │   │
│   │                                                                      │   │
│   │                          [2.0x] ← 줌          [⇅] ← SwapButton     │   │
│   └──────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```
«Portrait 기준. ControlButtonContainer는 항상 상단 고정(다른 페이지와 동일)»

## 3. 유저 플로우

### 3.1 기본 포인팅

1. PointingArea 위 아무 곳이나 터치
2. 터치 좌표가 PC 화면 절대 위치로 즉시 매핑 → 커서 이동
3. 드래그하면 커서가 손가락을 따라 이동
4. 터치 종료

### 3.2 클릭

1. PointingArea를 짧게 탭 (≤500ms, 이동량 ≤5dp)
2. 터치 위치로 커서 이동 + 클릭 이벤트 전송
3. 현재 ClickMode에 따라 좌클릭 또는 우클릭

### 3.3 줌 사용

1. ZoomButton 탭 → 줌 모드 진입
2. 확대할 중심 위치를 PointingArea에서 터치
3. 터치를 유지한 채 바깥 방향으로 드래그 → 드래그 거리에 비례해 줌 레벨 증가
4. 손 떼기 → 해당 줌 레벨 확정, 일반 포인팅으로 복귀
5. 줌 상태에서 포인팅/클릭 수행 (매핑 범위가 축소된 상태)
6. ZoomButton 재탭 → 1x 복귀

### 3.4 드래그 앤 드롭 모드 (구 스크롤 모드 대체)

1. DragModeButton 탭 → 드래그 모드 진입(ON), 테두리 초록색
2. PointingArea 터치 시작(`ACTION_DOWN`) 즉시 좌클릭 버튼을 누른 상태로 전송
3. 터치 유지한 채 이동 → 커서 이동 + 좌클릭 유지(드래그)
4. 터치 종료(`ACTION_UP`) → 좌클릭 해제 전송(drop). 드래그 모드 자체는 ON 유지(제스처마다 반복 가능)
5. DragModeButton 재탭 → 드래그 모드 해제(OFF), 이후 터치는 커서 이동만 수행

> 스크롤 모드는 절대좌표에서 성립하지 않아(델타 누적 기반) 배제되었다. 자세한 근거는 `phase-4-9-page3-absolute-pointing.md` 참조.

### 3.5 패드/키 영역 교체

1. PointingArea 우하단의 SwapButton 탭
2. PointingArea와 QuickKeyStrip의 수직 위치 즉시 교체
3. 재탭 시 원래 위치로 복귀

## 4. 상호작용 및 상태

### 4.1 테두리 색상 상태 규칙

테두리 색상으로 현재 패드 상태를 직관적으로 전달:

| 우선순위 | 상태 | 테두리 색상 | 비고 |
|---------|------|------------|------|
| 1 (최고) | 드래그 모드 ON | `#84E268` (초록) | |
| 2 | 우클릭 모드 | `#F3D021` (노란) | |
| 3 | 줌 활성 (>1x) | `#FF9800` (주황) | 줌 레벨 배지는 항상 표시 |
| 4 (기본) | 좌클릭 + 포인팅 | `#E91E63` (핑크) | |

### 4.2 ControlButtonContainer 버튼 상태

| 버튼 | Unselected | Selected | 전환 방법 |
|------|-----------|----------|----------|
| ClickModeButton | 좌클릭 (기본) | 우클릭 | 탭 토글 |
| ZoomButton | 1x (기본) | >1x (배율 배지) | 탭으로 진입/해제 |
| DragModeButton | OFF (기본, 커서 이동만) | ON (누른 채 이동) | 탭 토글 |
| MonitorSelector (Standard 전용) | "전체" 선택 (기본) | 특정 모니터 번호 선택 | 칩 탭 |

### 4.3 햅틱 피드백

- ClickMode 전환: Medium (50ms)
- 드래그 모드 진입/해제: Light (30ms)
- 줌 모드 진입: Light (30ms)
- 줌 확정 (손 떼기): Medium (50ms)
- 줌 해제 (1x 복귀): Light (30ms)

## 5. 반응형/적응 규칙

- **소형 화면 (폭 < 360dp)**:
  - PointingArea 최소 크기 적용 (프리셋별 최소 크기 하한, `PadRatioConfig` 참조)
  - QuickKeyStrip 키 간격 4dp로 축소
  - `ControlButtonContainer` 버튼 간격 12dp로 축소
- **중형 화면 (360dp ≤ 폭 < 600dp)**:
  - 기본 레이아웃 유지
  - PointingArea가 가용 공간 최대 활용 (Fill 기본, 프리셋 선택 시 해당 비율 유지)
- **대형 화면 (폭 ≥ 600dp, Landscape)**:
  - PointingArea 가로 확장 (Fill 기본이므로 확장된 가용 공간을 그대로 채움)
- **높이 제약**: QuickKeyStrip(40dp)과 `ControlButtonContainer`는 항상 고정 표시, PointingArea 크기를 줄여서 대응

## 6. 접근성

- **ClickModeButton**: `contentDescription` = "클릭 모드: 좌클릭" / "클릭 모드: 우클릭"
- **DragModeButton**: `contentDescription` = "드래그 모드: 해제" / "드래그 모드: 활성"
- **ZoomButton**: `contentDescription` = "줌: 1배" / "줌: 2배" 등 현재 배율 포함
- **MonitorSelector**: `contentDescription` = "매핑 대상: 전체 화면" / "매핑 대상: 모니터 N"
- **PointingArea**: `contentDescription` = "절대좌표 터치 영역. 터치한 위치가 PC 커서 위치가 됩니다"
- **QuickKeyStrip 각 키**: `contentDescription` = 키 이름 (예: "Escape 키", "백스페이스 키")
- **SwapButton**: `contentDescription` = "패드/키 영역 교체. 현재: 패드 위" / "패드/키 영역 교체. 현재: 키 위"
- 고대비 모드: 테두리 두께 3dp로 증가, 줌 오버레이 alpha 값 상향

## 7. 구현 메모 (개발자용)

- **Composable**: `AbsolutePointingPad` + `QuickKeyStrip` + 상단 `ControlButtonContainer`로 페이지 구성 (하단 전용 ControlBar 없음)
- **QuickKeyStrip**: Page 1의 Special Keys와 동일한 `KeyboardKeyButton` 재사용. 별도 Composable 분리 권장
- **SwapButton**: PointingArea 위에 `Box`로 오버레이. `swapState: Boolean` (Pad-Top / KB-Top) → `AnimatedContent` 또는 단순 순서 변경으로 구현. 상태는 SharedPreferences에 저장
- **좌표 변환**: `AbsoluteCoordinateCalculator`에서 줌 상태를 반영한 매핑 범위 계산(경로 공통, 비율 산출까지)
- **좌표 전송**: `FrameBuilder.buildAbsolutePositionCommand()`(`frame[0]==0xFF, frame[1]==0x02`) 사용, Vendor CDC 서버 중계. 상세는 `technical-specification-app.md` §2.10.2
- **전송 최적화**: 동일 좌표 연속 전송 방지, 120Hz 주기 준수
- **상태 저장**: 줌 레벨/중심점, 클릭 모드, 드래그 모드, 패드 비율 프리셋(`PadRatioConfig`)을 페이지 전환 시 유지 (SharedPreferences)
- **성능**: 좌표 변환 < 1ms, 전송 지연 < 50ms 목표

---

문서 간 역할 분리: 컴포넌트 설계는 `docs/android/component-design-guide-app.md` §4, 구현 요구사항은 `docs/android/technical-specification-app.md` §2.10, 전체 UI 정책은 `docs/android/design-guide-app.md`를 우선 참조하세요.
