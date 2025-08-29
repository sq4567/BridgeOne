---
title: "BridgeOne Windows 스플래시 스크린 설계 가이드"
description: "WPF/XAML 기반 애니메이션 스플래시 스크린 설계 명세 및 구현 요구사항"
tags: ["splash-screen", "animation", "wpf", "xaml", "branding", "windows"]
version: "v0.1"
owner: "Chatterbones"
updated: "2025-09-19"
note: "본 문서에 존재하는 모든 상수값 및 설정값은 초기 값으로, 확정된 고정값이 아님"
---

# BridgeOne Windows 스플래시 스크린 설계 가이드

## 개요 및 목적

BridgeOne Windows 서버 프로그램의 브랜드 아이덴티티를 담당하는 애니메이션 스플래시 스크린의 설계 명세입니다.

### 설계 목표
- **브랜딩 철학 구현**: 연결성(Connection), 희망성(Hope), 단순함(Simplicity) 시각적 표현
- **일관된 사용자 경험**: Android 앱과 동일한 브랜딩 경험 제공
- **성능 최적화**: 60fps 안정적 애니메이션과 DirectX 하드웨어 가속 활용
- **접근성 준수**: 내레이터 지원 및 고대비 모드 대응

### 기술적 접근법
- **WPF XAML**: 선언적 UI 구성과 리소스 기반 스타일 시스템
- **Storyboard 애니메이션**: 타임라인 기반 복합 애니메이션 제어
- **Path 기반 렌더링**: 벡터 그래픽과 스트로크 애니메이션
- **DirectX 가속**: WPF의 하드웨어 가속 렌더링 파이프라인 활용

## 브랜딩 철학 반영

### 연결성(Connection) 표현
- **브릿지 구조**: `Path.StrokeDashOffset` 애니메이션을 통한 양방향 Wipe-in
- **점진적 구축**: 대시 패턴 조작으로 브릿지 라인의 점진적 출현
- **의미적 완성**: 물리적-디지털 세계 연결의 순간 시각화

### 희망성(Hope) 표현
- **빛의 확산**: 어두운 배경(`#000000`)에서 밝은 요소(`#FFFFFF`)로의 전환
- **부드러운 전환**: WPF의 `EasingFunction`을 활용한 자연스러운 움직임
- **확장의 순간**: `ScaleTransform`을 통한 별의 크기 확장과 가능성 표현

### 단순함(Simplicity) 표현
- **최소한의 요소**: 브릿지와 별의 핵심 기하학적 형태만 활용
- **직관적 패턴**: 선형적이고 예측 가능한 애니메이션 진행
- **명확한 대비**: 흑백 기반의 강력한 시각적 구분

## 애니메이션 시퀀스 명세

총 지속시간: **2.5초** (6개 단계)

### Phase 1: 초기 대기 (0.0s ~ 0.3s)
```
지속시간: 300ms
목적: 윈도우 초기화 및 애니메이션 준비
시각 상태: 완전한 검은 배경 (#000000)
구현 방식: Storyboard BeginTime 지연
```

### Phase 2: 브릿지 등장 (0.3s ~ 1.0s)
```
지속시간: 700ms
목적: 연결성 철학의 핵심 표현
애니메이션: StrokeDashOffset을 이용한 양방향 Wipe-in
색상: 흰색 (#FFFFFF) 스트로크
구현 방식: DoubleAnimation with Path.StrokeDashOffset
```

### Phase 3: 별 회전 (1.0s ~ 1.5s)
```
지속시간: 500ms
목적: 가능성과 역동성 표현
애니메이션: RotateTransform 시계방향 180도
크기: 최종 크기보다 약간 작음 (ScaleTransform 0.85)
구현 방식: DoubleAnimation on RotateTransform.Angle
```

### Phase 4: 회전 안정화 (1.5s ~ 1.8s)
```
지속시간: 300ms
목적: 다음 단계 준비를 위한 안정화
상태: 회전 완료 후 정지 유지
준비 동작: ScaleTransform 애니메이션 준비
```

### Phase 5: 별 확장 및 배경 전환 (1.8s ~ 1.9s)
```
지속시간: 100ms
목적: 임팩트와 전환의 순간
애니메이션: ScaleTransform (0.85 → 1.0) + 방사형 배경 전환
효과: 매우 빠른 전환으로 시각적 임팩트 제공
구현 방식: DoubleAnimation on ScaleTransform + OpacityMask
```

### Phase 6: 텍스트 등장 (1.9s ~ 2.5s)
```
지속시간: 600ms
목적: 브랜드명 표시 및 완결
애니메이션: TextBlock Opacity (0.0 → 1.0)
위치: 별 로고 하단 중앙 정렬
전환 준비: 메인 윈도우로의 자연스러운 연결
```

## WPF/XAML 기반 설계 요구사항

### XAML 구조 설계
- **Window 설정**: `WindowStyle="None"`, `AllowsTransparency="True"`, `WindowStartupLocation="CenterScreen"`
- **Canvas 컨테이너**: 절대 위치 기반 레이아웃으로 정확한 애니메이션 제어
- **리소스 정의**: 색상, 브러시, 스타일을 ResourceDictionary로 중앙 관리
- **Path 요소**: 브릿지와 별의 기하학적 형태를 Path.Data로 정의

### Storyboard 기반 애니메이션 시스템
- **타임라인 제어**: `BeginTime` 속성으로 순차적 애니메이션 실행
- **Duration 정밀성**: 각 애니메이션의 정확한 지속시간 설정
- **이징 함수**: `QuadraticEase`, `CubicEase` 등을 활용한 자연스러운 움직임
- **EventTrigger**: 윈도우 Loaded 이벤트에서 Storyboard 자동 시작

### 속성 기반 애니메이션 설계
- **StrokeDashOffset**: 브릿지 Wipe-in을 위한 대시 패턴 애니메이션
- **RotateTransform**: 별 회전을 위한 각도 속성 애니메이션
- **ScaleTransform**: 별 크기 확장을 위한 스케일 속성 애니메이션
- **Opacity**: 텍스트 페이드 인을 위한 투명도 애니메이션

### 윈도우 생명주기 통합
- **시작 조건**: 프로그램 시작 시 스플래시 윈도우 최우선 표시
- **완료 처리**: 애니메이션 완료 이벤트에서 메인 윈도우 표시 및 스플래시 윈도우 닫기
- **예외 처리**: 사용자 클릭이나 키 입력 시 스킵 기능
- **리소스 관리**: 윈도우 닫기 시 모든 애니메이션 리소스 정리

## 성능 및 품질 목표

### 렌더링 성능
- **프레임률**: 60fps 안정적 유지 (16.67ms 프레임 시간)
- **DirectX 가속**: WPF의 하드웨어 가속 렌더링 파이프라인 활용
- **벡터 최적화**: Path 기반 벡터 그래픽으로 확장성과 효율성 확보
- **메모리 효율**: Storyboard 완료 후 즉시 리소스 해제

### 호환성 목표
- **Windows 버전**: Windows 10 1903+ (90% 사용자 커버리지)
- **.NET Framework**: .NET 6.0 이상 또는 .NET Framework 4.8
- **해상도 지원**: 96 DPI부터 고해상도 디스플레이까지 DPI 인식
- **멀티 모니터**: 다양한 모니터 구성에서 일관된 중앙 배치

### 품질 검증 기준
- **시각적 정확성**: 디자인 스펙과의 픽셀 단위 일치
- **타이밍 정확성**: 각 단계별 시간 오차 ±30ms 이내 (Windows 타이머 정밀도 고려)
- **부드러움**: GPU 가속을 통한 끊김 없는 애니메이션
- **일관성**: 다양한 하드웨어 환경에서 동일한 결과

## 접근성 요구사항

### 모션 접근성
- **시스템 설정**: `SystemParameters.ClientAreaAnimation` 확인
- **애니메이션 감소**: 시스템 설정에 따른 애니메이션 단순화 또는 비활성화
- **사용자 제어**: ESC 키, 클릭, Enter 키로 애니메이션 즉시 완료

### 내레이터 지원
- **콘텐츠 설명**: `AutomationProperties.Name`으로 "BridgeOne 시작 화면" 설명
- **진행 상태**: 애니메이션 단계별 `AutomationProperties.HelpText` 업데이트
- **완료 알림**: 애니메이션 완료 시 "애플리케이션 준비 완료" 알림

### 시각적 접근성
- **고대비 모드**: `SystemParameters.HighContrast` 설정 확인 및 대응
- **색상 독립성**: 흑백 기반 디자인으로 색맹 사용자 고려
- **텍스트 크기**: `SystemParameters.MessageFontSize` 설정 반영

### 키보드 접근성
- **포커스 관리**: 스플래시 윈도우의 키보드 포커스 적절한 처리
- **키보드 스킵**: Tab, Enter, Space 키로 애니메이션 스킵 가능
- **접근성 트리**: UI Automation을 통한 보조 기술 지원

## 색상 및 시각적 명세

### 색상 팔레트
- **배경색**: `#000000` (SolidColorBrush)
- **브릿지/별**: `#FFFFFF` (SolidColorBrush)
- **텍스트**: `#FFFFFF` (Foreground Brush)
- **대비비**: 21:1 (WCAG AAA 등급)

### 타이포그래피
- **폰트**: Segoe UI (Windows 시스템 기본) 또는 Pretendard (프로젝트 표준)
- **크기**: 시스템 폰트 크기 설정 반영 (`SystemFonts.MessageFontSize`)
- **두께**: FontWeight.Medium (500)
- **정렬**: HorizontalAlignment.Center

### 레이아웃 구조
- **윈도우 크기**: 시스템 주 모니터의 중앙 고정 크기
- **브릿지 위치**: Canvas 중앙 수평선 (`Canvas.Top`, `Canvas.Left`)
- **별 위치**: 브릿지 중앙 교차점에 정확히 정렬
- **텍스트 위치**: 별 하단에 적절한 여백을 두고 중앙 정렬

## 상수 및 임계값 정의

### 애니메이션 타이밍 상수
```
PHASE_1_DURATION_MS = "0:0:0.3"
PHASE_2_DURATION_MS = "0:0:0.7"
PHASE_3_DURATION_MS = "0:0:0.5"
PHASE_4_DURATION_MS = "0:0:0.3"
PHASE_5_DURATION_MS = "0:0:0.1"
PHASE_6_DURATION_MS = "0:0:0.6"
TOTAL_ANIMATION_DURATION_MS = "0:0:2.5"
```

### Storyboard BeginTime 상수
```
PHASE_1_BEGIN_TIME = "0:0:0"
PHASE_2_BEGIN_TIME = "0:0:0.3"
PHASE_3_BEGIN_TIME = "0:0:1.0"
PHASE_4_BEGIN_TIME = "0:0:1.5"
PHASE_5_BEGIN_TIME = "0:0:1.8"
PHASE_6_BEGIN_TIME = "0:0:1.9"
```

### 시각적 속성 상수
```
BRIDGE_STROKE_THICKNESS = 2.0
STAR_ROTATION_ANGLE = 180.0
STAR_SCALE_SMALL = 0.85
STAR_SCALE_NORMAL = 1.0
TEXT_OPACITY_START = 0.0
TEXT_OPACITY_END = 1.0
```

### 윈도우 설정 상수
```
SPLASH_WINDOW_WIDTH = 800
SPLASH_WINDOW_HEIGHT = 600
WINDOW_TOPMOST = true
WINDOW_SHOW_IN_TASKBAR = false
ANIMATION_SKIP_KEYS = { Key.Escape, Key.Enter, Key.Space }
```

## WPF 특화 구현 고려사항

### XAML 리소스 관리
- **ResourceDictionary**: 색상, 브러시, 스타일의 중앙집중식 관리
- **StaticResource**: 컴파일 타임 리소스 참조로 성능 최적화
- **Freezable**: Brush와 Pen 객체의 Freeze()를 통한 성능 향상
- **템플릿**: ControlTemplate과 DataTemplate을 통한 재사용성 확보

### 하드웨어 가속 최적화
- **RenderOptions**: `RenderOptions.ProcessRenderMode="Default"`로 하드웨어 가속 활성화
- **BitmapCache**: 복잡한 시각 요소에 대한 비트맵 캐싱 고려
- **LayeredWindow**: 투명 윈도우를 위한 적절한 컴포지션 모드 설정
- **Timeline**: 하드웨어 가속 친화적인 속성 애니메이션 우선 사용

### 메모리 및 리소스 관리
- **WeakEventManager**: 이벤트 핸들러의 메모리 누수 방지
- **Dispatcher**: UI 스레드와 백그라운드 작업의 적절한 분리
- **GC 최적화**: 애니메이션 중 가비지 컬렉션 최소화를 위한 객체 풀링
- **리소스 해제**: 윈도우 Closed 이벤트에서 모든 리소스 명시적 해제

### 예외 처리 및 복구
- **애니메이션 실패**: Storyboard 예외 시 정적 로고로 안전한 대체
- **하드웨어 가속 실패**: 소프트웨어 렌더링으로 자동 전환
- **타임아웃**: `DispatcherTimer`를 이용한 최대 대기 시간 설정
- **복구 메커니즘**: 중단된 애니메이션의 안전한 스킵 및 완료

## 품질 검증 방법론

### 자동화 테스트
- **Storyboard 이벤트**: 각 애니메이션 완료 이벤트의 정확한 발생 검증
- **속성 값 검증**: 애니메이션 완료 시점의 변환된 속성 값 확인
- **메모리 누수**: 스플래시 윈도우 종료 후 리소스 해제 검증
- **성능 카운터**: 렌더링 스레드 사용률과 프레임 드롭 모니터링

### 수동 테스트
- **다중 해상도**: 다양한 DPI와 해상도에서 시각적 일관성 확인
- **하드웨어 호환성**: 다양한 그래픽 카드에서 하드웨어 가속 동작 검증
- **접근성 도구**: Windows 내레이터와 고대비 모드에서 동작 확인
- **멀티 모니터**: 다양한 모니터 구성에서 윈도우 위치와 애니메이션 확인

### 성능 프로파일링
- **WPF Performance Suite**: 렌더링 성능과 메모리 사용량 분석
- **Visual Studio Diagnostics**: CPU 사용률과 가비지 컬렉션 패턴 분석
- **DirectX 디버깅**: GPU 사용률과 하드웨어 가속 효율성 측정

## 크로스플랫폼 일관성 유지

### Android 버전과의 동기화
- **타이밍 동일성**: 정확히 동일한 2.5초 총 지속시간과 6단계 구간
- **시각적 일관성**: 동일한 색상(`#000000`, `#FFFFFF`)과 기하학적 형태
- **브랜딩 철학**: 연결성, 희망성, 단순함의 동일한 표현 방식과 의미

### 플랫폼 특성 활용
- **WPF 장점**: Storyboard의 정밀한 타이밍 제어와 선언적 애니메이션
- **DirectX 가속**: Windows 플랫폼의 강력한 그래픽 가속 활용
- **시스템 통합**: Windows 접근성 설정과 테마 시스템 완전 연동
- **데스크톱 최적화**: 윈도우 기반 애플리케이션의 특성에 맞는 UX

### 품질 표준 통일
- **브랜드 인식**: 두 플랫폼에서 동일한 브랜드 경험 제공
- **성능 기준**: 플랫폼별 최적화된 60fps 성능 달성
- **접근성 표준**: 각 플랫폼의 접근성 가이드라인 완전 준수

## 결론

본 설계 가이드는 BridgeOne Windows 서버 프로그램의 스플래시 스크린이 WPF와 XAML의 강력한 선언적 UI 시스템을 활용하여 브랜드 아이덴티티를 효과적으로 표현하도록 설계되었습니다.

Storyboard 기반의 타임라인 애니메이션과 DirectX 하드웨어 가속을 통해 Android 버전과 동일한 품질의 브랜딩 경험을 제공하면서도, Windows 플랫폼의 고유한 장점을 충분히 활용할 수 있도록 구성되었습니다. [[memory:7359157]]

개발 시에는 WPF의 리소스 관리 시스템과 하드웨어 가속 최적화를 적극 활용하여, 본 가이드에서 제시한 브랜딩 철학과 애니메이션 시퀀스를 정확히 구현해 주시기 바랍니다. 특히 접근성과 성능 요구사항을 모두 만족하는 완전한 구현이 필요합니다. [[memory:7685458]]
