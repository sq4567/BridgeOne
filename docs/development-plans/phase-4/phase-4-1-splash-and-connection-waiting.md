---
title: "BridgeOne Phase 4.1: 스플래시 스크린 & 연결 대기 화면"
description: "BridgeOne 프로젝트 Phase 4.1 - 앱 진입 흐름 완성: 스플래시 스크린 애니메이션 및 연결 대기 화면 구현"
tags: ["android", "splash-screen", "connection-waiting", "animation", "compose", "ui"]
version: "v1.0"
owner: "Chatterbones"
updated: "2026-03-26"
---

# BridgeOne Phase 4.1: 스플래시 스크린 & 연결 대기 화면

**개발 기간**: 3-4일

**목표**: 앱 실행 시 브랜딩 스플래시 스크린을 표시한 후, USB 동글 연결 전까지 연결 대기 화면을 제공하여 완성된 앱 진입 흐름을 구현합니다.

**핵심 성과물**:
- 2.5초 6단계 브랜딩 애니메이션 스플래시 스크린
- USB 동글 연결 대기 화면 (단계별 동적 메시지)
- 앱 상태 머신 통합 (Splash → WaitingForConnection → Essential/Standard)
- 접근성 준수 (WCAG AA, 스크린 리더, 모터 접근성)

**선행 조건**: Phase 3.5 (모드 전환 시스템) 완료

---

## 현재 상태 분석

### 기존 구현
- `BridgeOneApp.kt`: `UsbSerialManager.bridgeMode` StateFlow 기반으로 `EssentialModePage` / `StandardModePage` 직접 분기 (`MainContent()` 내 `when` 분기)
- `BridgeOneApp.kt`: 모드 전환 시 `LaunchedEffect(bridgeMode)`로 토스트 알림 표시
- `BridgeOneApp.kt`: 디버그 패널(`UsbDebugPanel`) 상단 표시/숨김 토글 존재
- 스플래시 스크린 없음 — 앱 시작 시 바로 모드 페이지로 진입
- 연결 대기 화면 없음 — USB 미연결 시에도 Essential 페이지 표시
- 2초 주기 USB 상태 폴링 `LaunchedEffect(Unit)` → `UsbSerialManager.scanAndUpdateDebugState()` 존재
- `UsbSerialManager.bridgeMode`: `MutableStateFlow<BridgeMode>` (ESSENTIAL/STANDARD), 포트 닫힘 시 ESSENTIAL로 자동 복귀

### 목표 앱 진입 흐름
```
앱 실행 → Splash (2.5s) → WaitingForConnection → USB 연결 → Essential → (핸드셰이크) → Standard
                                                    ↑                        │
                                                    └────── USB 해제 ────────┘
```

---

## Phase 4.1.1: 스플래시 스크린 구현

**목표**: BridgeOne 브랜딩 아이덴티티를 표현하는 2.5초 애니메이션 스플래시 스크린

**개발 기간**: 1.5-2일

**세부 목표**:
1. `SplashScreen` Composable 생성:
   - 전체 화면, 검은 배경 (`#000000`)
   - `AnimationPhase` sealed class/enum 기반 6단계 상태 관리
2. 로고 SVG Path 데이터를 Compose `Path` 객체로 변환:
   - **별 모양 Path**: 8개 뾰족한 끝의 별 (viewBox "0 0 669.6 669.6")
   - **브릿지 Path**: Y=537.7에서 시작하는 수평 브릿지 구조
3. 6단계 애니메이션 시퀀스:
   - Phase 1 (0-300ms): 초기 대기, 검은 배경
   - Phase 2 (300-1000ms): 브릿지 양방향 Wipe-in (`PathMeasure` + `getSegment()`)
   - Phase 3 (1000-1500ms): 별 시계방향 180도 회전 (scale 0.85)
   - Phase 4 (1500-1800ms): 회전 안정화 및 정지
   - Phase 5 (1800-1900ms): 별 빠른 확장 + 방사형 배경 전환
   - Phase 6 (1900-2500ms): "BridgeOne" 텍스트 페이드 인
4. 접근성 처리:
   - `Settings.Global.TRANSITION_ANIMATION_SCALE` 확인
   - 애니메이션 축소/비활성 설정 시 즉시 완료 상태로 전환
   - 탭으로 스킵 가능 (500ms 이후)

**신규 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/splash/SplashScreen.kt`
- `src/android/app/src/main/java/com/bridgeone/app/ui/splash/SplashAnimationState.kt`

**참조 문서**:
- `docs/android/styleframe-loading-splash.md` (전체 스타일프레임)
- `docs/android/design-guide-app.md` §8.1.1 (앱 실행 및 초기 상태 감지)
- `docs/android/technical-specification-app.md` §2.1 (스플래시 스크린 구현 요구사항)

**상수 정의**:
```kotlin
object SplashConstants {
    const val PHASE_1_DURATION_MS = 300
    const val PHASE_2_DURATION_MS = 700
    const val PHASE_3_DURATION_MS = 500
    const val PHASE_4_DURATION_MS = 300
    const val PHASE_5_DURATION_MS = 100
    const val PHASE_6_DURATION_MS = 600
    const val TOTAL_DURATION_MS = 2500
    const val BRIDGE_LINE_WIDTH_DP = 2.0f
    const val STAR_ROTATION_DEGREES = 180f
    const val STAR_SCALE_RATIO = 0.85f
    const val SKIP_TAP_THRESHOLD_MS = 500
}
```

**검증**:
- [x] 6단계 애니메이션이 순서대로 정상 실행 (2.5초)
- [x] Canvas 기반 Path 렌더링 60fps 유지
- [x] 탭 스킵 동작 (500ms 이후)
- [x] 애니메이션 비활성 접근성 설정 시 즉시 완료
- [x] 에뮬레이터 및 실기기에서 시각적 확인

---

## Phase 4.1.2: 연결 대기 화면 구현

**목표**: USB 동글 미연결 시 사용자에게 연결 안내와 진행 상태를 표시하는 대기 화면

**개발 기간**: 1-1.5일

**세부 목표**:
1. `ConnectionWaitingScreen` Composable 생성:
   - 전체 화면, 앱 기본 배경 (`#121212`)
   - 화면 중앙 정렬 콘텐츠 (수직·수평 중앙)
2. 동적 단계별 UI 구성:
   - **1단계 (USB 대기)**:
     - 아이콘: `ic_usb.xml` (128dp, `#FF9800`, 2초 주기 회전)
     - 주 메시지: "USB 동글을 연결해주세요" (24sp, Bold, `#FFFFFF`)
     - 부 메시지: "OTG 케이블과 동글을 확인해주세요" (16sp, `#C2C2C2`)
   - **2단계 (서버 탐색)**:
     - 주 메시지: "서버를 찾고 있습니다"
     - 부 메시지: "잠시만 기다려주세요"
   - **권한 요청 단계**:
     - 주 메시지: "USB 권한을 요청했습니다"
     - 부 메시지: "권한을 허용해주세요"
   - **오류 단계**:
     - 주 메시지: "연결에 실패했습니다"
     - 부 메시지: 구체적 오류 메시지
3. 애니메이션:
   - USB 아이콘: `infiniteRepeatable` 360도 회전 (2초 주기, LinearEasing)
   - 화면 진입: Fade In (300ms) + Slide Up (20dp)
   - 메시지 변경: CrossFade (200ms)
4. 뒤로가기 처리:
   - 첫 번째 터치: "앱을 종료하시겠습니까?" 토스트 (3초)
   - 3초 내 두 번째 터치: 앱 종료
5. 접근성:
   - `contentDescription` 단계별 동적 변경
   - `LiveRegion`으로 상태 변화 실시간 알림
   - 대비율 WCAG AA 준수 (주 메시지 21:1, 부 메시지 7.7:1)

**신규 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/connection/ConnectionWaitingScreen.kt`
- `src/android/app/src/main/java/com/bridgeone/app/ui/connection/ConnectionState.kt`

**참조 문서**:
- `docs/android/styleframe-connection-waiting.md` (전체 스타일프레임)
- `docs/android/design-guide-app.md` §8.1.3 (연결 대기 화면)

**검증**:
- [ ] 단계별 메시지 동적 변경 정상 동작
- [ ] USB 아이콘 회전 애니메이션 부드러운 60fps
- [ ] 뒤로가기 더블 탭 종료 로직
- [ ] CrossFade 텍스트 전환 자연스러움
- [ ] 스크린 리더 `LiveRegion` 알림 동작

> **에뮬레이터 참고**: `ConnectionWaitingScreen` UI 렌더링 및 애니메이션은 에뮬레이터에서 완전 테스트 가능. USB 연결 감지는 에뮬레이터에서 불가능하므로, Phase 4.1.3에서 추가되는 DEV 버튼으로 화면 전환을 대신한다.

---

## Phase 4.1.3: 앱 상태 머신 및 화면 전환 통합

**목표**: `BridgeOneApp.kt`를 리팩토링하여 Splash → WaitingForConnection → Mode 전환 흐름을 통합

**개발 기간**: 0.5-1일

**세부 목표**:
1. `AppState` sealed class 정의:
   ```kotlin
   sealed class AppState {
       object Splash : AppState()
       object WaitingForConnection : AppState()
       data class Active(val bridgeMode: BridgeMode) : AppState()
   }
   ```
2. `BridgeOneApp.kt` 리팩토링:
   - `appState: MutableStateFlow<AppState>` 도입
   - 앱 시작 시 `AppState.Splash` → 2.5초 후 `AppState.WaitingForConnection`
   - USB 연결 감지 시 `AppState.Active(BridgeMode.ESSENTIAL)` 전환
   - USB 해제 시 `AppState.WaitingForConnection` 복귀
   - `bridgeMode` 변경은 `Active` 상태 내에서만 처리
3. **기존 `UsbSerialManager.bridgeMode` StateFlow와의 관계 정리**:
   - `UsbSerialManager.bridgeMode`는 ESSENTIAL/STANDARD 모드 전환 전용으로 유지
   - `AppState`는 앱 레벨 화면 상태 (Splash/WaitingForConnection/Active) 관리
   - `Active` 상태 진입 후 `UsbSerialManager.bridgeMode` 변경을 observe하여 `Active(bridgeMode)` 업데이트
   - USB 포트 닫힘 시 `UsbSerialManager`가 ESSENTIAL로 복귀하는 기존 로직 → `AppState.WaitingForConnection`으로 전환하도록 변경
4. **기존 디버그 패널(`UsbDebugPanel`) 처리**:
   - `Active` 상태에서만 디버그 패널 표시 가능
   - Splash/WaitingForConnection 상태에서는 숨김
5. **기존 모드 전환 토스트 (`LaunchedEffect(bridgeMode)`) 통합**:
   - `Active` 상태 내에서만 토스트 표시 (기존 로직 유지)
   - `isFirstMode` 플래그 로직은 `AppState` 전환으로 대체 가능
6. 화면 전환 애니메이션 (`AnimatedVisibility` + `fadeIn`/`fadeOut` 패턴 검증 완료):
   - Splash → WaitingForConnection: Fade Out (200ms)
   - WaitingForConnection → Active: Fade In (300ms)
   - Active → WaitingForConnection: 즉시 전환 (USB 해제는 긴급 상황)
7. 기존 USB 폴링 로직과 통합:
   - 2초 주기 폴링(`scanAndUpdateDebugState`)은 `WaitingForConnection` 상태에서만 활성
   - `Active` 상태에서는 기존 `UsbSerialManager` 이벤트 기반 감지 유지
8. **에뮬레이터 개발용 DEV 디버그 버튼** (`BuildConfig.DEBUG` 조건부):
   - `WaitingForConnection` 상태에서 `Active(BridgeMode.STANDARD)` 강제 전환 버튼
   - `Active` 상태에서 `ESSENTIAL` ↔ `STANDARD` 모드 토글 버튼
   - `BuildConfig.DEBUG = true`일 때만 표시 → 릴리스 빌드에서 자동 제거
   - 위치: 화면 우상단 FAB 스타일 (라벨 "DEV", 배경 `#303030`, 반투명)
   ```kotlin
   if (BuildConfig.DEBUG) {
       Box(modifier = Modifier.fillMaxSize()) {
           FloatingActionButton(
               onClick = { appState.value = AppState.Active(BridgeMode.STANDARD) },
               modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
               containerColor = Color(0xFF303030)
           ) { Text("DEV", fontSize = 10.sp) }
       }
   }
   ```

**수정 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/BridgeOneApp.kt` (리팩토링)

**신규 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/AppState.kt` (sealed class 별도 분리 권장)

**참조 문서**:
- `docs/android/design-guide-app.md` §8.1.1 (앱 실행 및 초기 상태 감지)
- `docs/android/styleframe-essential.md` §8 (상태 머신)

**검증**:
- [ ] Splash 화면이 2.5초 표시 후 자동 전환
- [ ] USB 미연결 시 WaitingForConnection 화면 표시
- [ ] USB 연결 시 Essential 페이지로 전환
- [ ] USB 해제 시 WaitingForConnection으로 복귀
- [ ] 핸드셰이크 완료 시 Standard 모드 전환 (기존 동작 유지)
- [ ] 화면 전환 시 애니메이션 자연스러움
- [ ] 디버그 패널이 Active 상태에서만 표시
- [ ] 기존 모드 전환 토스트가 Active 상태 내에서 정상 동작
- [ ] 에뮬레이터 빌드 및 실행 성공
- [ ] (에뮬레이터) DEV 버튼으로 WaitingForConnection → Active(STANDARD) 강제 전환
- [ ] (에뮬레이터) DEV 버튼으로 Active 상태에서 ESSENTIAL ↔ STANDARD 토글
- [ ] DEV 버튼이 릴리스 빌드에서 표시되지 않음 확인

---

## 반응형 규칙 (전체 Phase 4.1 공통)

| 화면 크기 | 스플래시 | 연결 대기 |
|-----------|---------|----------|
| 폭 < 360dp | 기본 크기 유지 | 아이콘 96dp, 텍스트 유지 |
| 360-600dp | 기본 크기 | 아이콘 128dp (기본) |
| 폭 ≥ 600dp | 확대 가능 | 아이콘 160dp, 가로 배치 고려 |
