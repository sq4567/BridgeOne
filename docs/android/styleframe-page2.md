---
title: "Styleframe - Page 2 (AbsolutePointingPad)"
description: "절대좌표 패드 전용 페이지. 터치 위치가 곧 PC 커서 위치가 되는 절대좌표 포인팅 + 줌 기능"
tags: ["styleframe", "absolute-pointing", "pointing-pad", "zoom", "ui"]
version: "v0.1"
owner: "Chatterbones"
updated: "2026-03-30"
note: "본 문서에 존재하는 모든 상수값 및 설정값은 초기 값으로, 확정된 고정값이 아님"
---

# Page 2 스타일프레임 문서

## 1. 개요

이 문서는 AbsolutePointingPad 전용 페이지(Page 2)의 스타일프레임을 정의합니다. 터치한 위치가 곧 PC 커서의 절대 위치가 되는 "펜 태블릿" 방식의 포인팅 페이지입니다.

> **상세 아키텍처**: 전체 시스템 아키텍처는 [`technical-specification.md` §3]를 참조하세요.
> **용어 정의**: 용어 정의는 [`technical-specification.md` §6.2 Android 플랫폼 용어집]을 참조하세요.

**참조**: `docs/android/component-design-guide-app.md` §4(AbsolutePointingPad 컴포넌트 설계), `docs/android/design-guide-app.md` §5(토스트/인디케이터/햅틱), `docs/android/technical-specification-app.md` §2.10(구현 요구사항).

**페이지 순서 내 위치**:
- Page 1: 터치패드 + Actions (상대좌표)
- **Page 2: 절대좌표 패드** ← 이 문서
- Page 3: 키보드 중심
- Page 4: Minecraft 특화

## 2. 레이아웃 구조

- **단일 컴포넌트 페이지**: AbsolutePointingPad가 페이지 전체를 차지합니다.
- Page 1과 달리 좌/우 분할 구조가 아닌, 전체 화면 단일 영역 구조입니다.
- 여백: 바깥 16dp.
- 방향: Portrait 최적화. Landscape에서는 PointingArea가 자동으로 가로 확장.

### 2.1 PointingArea (메인 터치 영역)

- **배치**: 페이지 중앙, 가용 공간 최대 활용
- **종횡비**: 16:9 권장 (PC 모니터 비율에 근사). 16:10도 허용
- **최소 크기**: 280dp × 158dp (16:9 기준)
- **최대 크기**: 가용 화면에서 ControlBar 영역을 제외한 전체
- **모서리**: 8dp 라운드 코너
- **테두리**: 2dp 두께, 상태에 따른 색상 변화 (`component-design-guide-app.md` §4.3, §4.5.7 참조)

**비율 유지 규칙**:
- PointingArea는 항상 16:9 (또는 16:10) 비율을 유지
- 가용 공간이 이 비율보다 세로로 길면 → 상하 여백 추가 (letterbox)
- 가용 공간이 이 비율보다 가로로 길면 → 좌우 여백 추가 (pillarbox)

시각 토큰:
- 배경: `#1E1E1E` (진한 회색, 터치패드와 동일)
- 기본 테두리: `#E91E63` (핑크색)
- 우클릭 모드 테두리: `#F3D021` (노란색)
- 스크롤 모드 테두리: `#84E268` (초록색)
- 줌 활성 테두리: `#FF9800` (주황색)

### 2.2 ControlBar (제어 버튼 영역)

- **배치**: PointingArea 하단 외부에 배치 (PointingArea와 겹치지 않음)
- **높이**: 48dp
- **정렬**: 수평 중앙 정렬, 버튼 간 간격 16dp
- **배경**: 투명 (페이지 배경과 동일)

**버튼 구성**:

```
ControlBar
├── ClickModeButton (좌클릭 ↔ 우클릭 전환)
├── ScrollToggleButton (스크롤 모드 전환)
└── ZoomButton (줌 모드 진입/해제)
```

- **ClickModeButton**: 터치패드의 동일 컨트롤과 같은 디자인. 좌클릭(기본)/우클릭 토글
- **ScrollToggleButton**: 스크롤 모드 진입/해제. 활성 시 테두리 초록색
- **ZoomButton**: 줌 모드 진입/해제. 활성(>1x) 시 배율 배지 표시 (예: "2x")

각 버튼: 터치 타겟 ≥ 48dp, 아이콘 24dp, 리플 비활성.

### 2.3 CoordinateIndicator (터치 위치 표시)

- **위치**: PointingArea 내부, 현재 터치 좌표 위
- **형태**: 십자선 (가로 + 세로 1dp 선, 길이 20dp) + 중앙 점 (4dp 원)
- **색상**: `#FFFFFF` (alpha 0.6)
- **동작**: 터치 중일 때만 표시, 터치 종료 후 300ms 페이드 아웃
- **목적**: 손가락에 가려진 정확한 터치 위치를 시각적으로 확인

### 2.4 줌 시각 피드백

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
- **Essential 모드**: Windows 서버 미연결 → PC 오버레이 불가, 앱 내 줌 레벨 텍스트만 표시
- **통신 경로**: Android → ESP32 (UART) → Windows 서버 (Vendor CDC) → WPF 투명 오버레이 윈도우
- **상세 구현**: `technical-specification-server.md` 줌 영역 오버레이 섹션 참조

### 2.5 ASCII 레이아웃 (개략)

```text
┌──────────────────────────────────────────────────────────────────────────────┐
│                                                                              │
│   ┌──────────────────────────────────────────────────────────────────────┐   │
│   │                                                                      │   │
│   │                                                                      │   │
│   │                       PointingArea (16:9)                            │   │
│   │                                                                      │   │
│   │                           ＋  ← CoordinateIndicator                 │   │
│   │                                                                      │   │
│   │                                                      [2.0x] ← 줌    │   │
│   └──────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
│              [ClickMode]    [Scroll]    [Zoom]  ← ControlBar                │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
  «단일 컴포넌트 전체 화면, Portrait 기준»
```

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

### 3.4 스크롤 모드

1. ScrollToggleButton 탭 → 스크롤 모드 진입
2. 터치 드래그가 커서 이동 대신 스크롤 신호로 변환
3. ScrollToggleButton 재탭 또는 PointingArea 원탭 → 스크롤 모드 종료

## 4. 상호작용 및 상태

### 4.1 테두리 색상 상태 규칙

테두리 색상으로 현재 패드 상태를 직관적으로 전달:

| 우선순위 | 상태 | 테두리 색상 | 비고 |
|---------|------|------------|------|
| 1 (최고) | 스크롤 모드 | `#84E268` (초록) | |
| 2 | 우클릭 모드 | `#F3D021` (노란) | |
| 3 | 줌 활성 (>1x) | `#FF9800` (주황) | 줌 레벨 배지는 항상 표시 |
| 4 (기본) | 좌클릭 + 포인팅 | `#E91E63` (핑크) | |

### 4.2 ControlBar 버튼 상태

| 버튼 | Unselected | Selected | 전환 방법 |
|------|-----------|----------|----------|
| ClickModeButton | 좌클릭 (기본) | 우클릭 | 탭 토글 |
| ScrollToggleButton | 포인팅 (기본) | 스크롤 | 탭 토글 |
| ZoomButton | 1x (기본) | >1x (배율 배지) | 탭으로 진입/해제 |

### 4.3 햅틱 피드백

- ClickMode 전환: Medium (50ms)
- 스크롤 모드 진입/해제: Light (30ms)
- 줌 모드 진입: Light (30ms)
- 줌 확정 (손 떼기): Medium (50ms)
- 줌 해제 (1x 복귀): Light (30ms)

## 5. 반응형/적응 규칙

- **소형 화면 (폭 < 360dp)**:
  - PointingArea 최소 크기 적용 (280dp × 158dp)
  - ControlBar 버튼 간격 12dp로 축소
- **중형 화면 (360dp ≤ 폭 < 600dp)**:
  - 기본 레이아웃 유지
  - PointingArea가 가용 공간 최대 활용 (16:9 비율 유지)
- **대형 화면 (폭 ≥ 600dp, Landscape)**:
  - PointingArea 가로 확장, 16:9 비율 유지
  - ControlBar를 PointingArea 우측에 세로 배치 가능 (공간 활용 최적화)
- **높이 제약**: ControlBar는 항상 고정 표시, PointingArea 크기를 줄여서 대응

## 6. 접근성

- **ClickModeButton**: `contentDescription` = "클릭 모드: 좌클릭" / "클릭 모드: 우클릭"
- **ScrollToggleButton**: `contentDescription` = "스크롤 모드: 해제" / "스크롤 모드: 활성"
- **ZoomButton**: `contentDescription` = "줌: 1배" / "줌: 2배" 등 현재 배율 포함
- **PointingArea**: `contentDescription` = "절대좌표 터치 영역. 터치한 위치가 PC 커서 위치가 됩니다"
- 고대비 모드: 테두리 두께 3dp로 증가, 줌 오버레이 alpha 값 상향

## 7. 구현 메모 (개발자용)

- **Composable**: `AbsolutePointingPad` 단일 컴포넌트가 페이지 전체를 구성
- **좌표 변환**: `AbsoluteCoordinateCalculator`에서 줌 상태를 반영한 매핑 범위 계산
- **프레임 전송**: `FrameBuilder.buildAbsoluteFrame()` 사용, `frame[1] == 0x80`으로 절대좌표 식별
- **전송 최적화**: 동일 좌표 연속 전송 방지, 120Hz 주기 준수
- **상태 저장**: 줌 레벨/중심점, 클릭 모드를 페이지 전환 시 유지 (SharedPreferences)
- **성능**: 좌표 변환 < 1ms, 전송 지연 < 50ms 목표

---

문서 간 역할 분리: 컴포넌트 설계는 `docs/android/component-design-guide-app.md` §4, 구현 요구사항은 `docs/android/technical-specification-app.md` §2.10, 전체 UI 정책은 `docs/android/design-guide-app.md`를 우선 참조하세요.
