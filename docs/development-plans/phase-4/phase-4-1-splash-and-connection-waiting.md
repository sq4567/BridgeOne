---
title: "BridgeOne Phase 4.1: 스플래시 스크린 & 연결 대기 화면"
description: "BridgeOne 프로젝트 Phase 4.1 - 앱 진입 흐름 완성: 스플래시 스크린 애니메이션 및 연결 대기 화면 구현"
tags: ["android", "splash-screen", "connection-waiting", "animation", "compose", "ui"]
version: "v1.0"
owner: "Chatterbones"
updated: "2026-03-27"
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
- [x] 단계별 메시지 동적 변경 정상 동작
- [x] USB 아이콘 회전 애니메이션 부드러운 60fps
- [x] 뒤로가기 더블 탭 종료 로직
- [x] CrossFade 텍스트 전환 자연스러움
- [x] 스크린 리더 `LiveRegion` 알림 동작

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
- [x] Splash 화면이 2.5초 표시 후 자동 전환
- [x] USB 미연결 시 WaitingForConnection 화면 표시
- [x] USB 연결 시 Essential 페이지로 전환
- [x] USB 해제 시 WaitingForConnection으로 복귀
- [x] 핸드셰이크 완료 시 Standard 모드 전환 (기존 동작 유지)
- [x] 화면 전환 시 애니메이션 자연스러움
- [x] 기존 모드 전환 토스트가 Active 상태 내에서 정상 동작
- [x] 에뮬레이터 빌드 및 실행 성공

---

## Phase 4.1.4: 연결 대기 화면 UI 개선

**목표**: 밋밋한 연결 대기 화면을 스플래시의 브랜드 아이덴티티와 이어지는 세련된 UI로 개선

**개발 기간**: 0.5-1일

**세부 목표**:
1. 방사형 그라데이션 배경 (#1E1E2E 중앙 → #121212 외곽, 깊이감)
2. USB 케이블을 핸드폰에 꽂는 Canvas 애니메이션 (`UsbPlugAnimation`)
   - 핸드폰 하단 실루엣 + USB-C 커넥터 (외곽선 스타일)
   - 3초 루프: 상승 → 꽂힘 → 글로우 → 빠짐
   - `isProcessing = false` 시 꽂힌 정지 상태
3. 라벨 포함 연결 단계 스텝 인디케이터 (`StepIndicator`)
   - "USB 연결 ── 서버 탐색 ── 준비 완료" 3단계
   - 현재 단계 텍스트 크기/색상 애니메이션 강조
4. 상단 BridgeOne 별 로고 (`BridgeOneLogo` 공용 Composable)
   - `SplashScreen`의 `STAR_PATH_DATA`를 `ui/common/`으로 분리
5. `ConnectionState`에 `step` 프로퍼티 추가 (스텝 인디케이터 연동)
6. 화면 레이아웃 재구성: 상단(로고+텍스트+스텝) / 하단(USB 꽂기 애니메이션)

**신규 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/connection/UsbPlugAnimation.kt`
- `src/android/app/src/main/java/com/bridgeone/app/ui/connection/StepIndicator.kt`
- `src/android/app/src/main/java/com/bridgeone/app/ui/common/BridgeOneLogo.kt`

**수정 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/connection/ConnectionWaitingScreen.kt`
- `src/android/app/src/main/java/com/bridgeone/app/ui/connection/ConnectionState.kt`
- `src/android/app/src/main/java/com/bridgeone/app/ui/splash/SplashScreen.kt`

**삭제 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/connection/PulseRingAnimation.kt`

**참조 문서**:
- `docs/android/styleframe-connection-waiting.md`

**검증**:
- [x] 방사형 그라데이션 배경이 자연스럽게 표시
- [x] USB 케이블 꽂기 애니메이션 정상 동작 (커넥터가 폰 내부 통과하지 않음)
- [x] 라벨 포함 스텝 인디케이터가 ConnectionState에 따라 정확히 변경
- [x] 상단 별 로고 표시
- [x] 기존 기능 (뒤로가기, CrossFade, 접근성) 정상 동작
- [x] 에뮬레이터 실행 확인

---

## Phase 4.1.5: 연결 대기 화면 진입 안내 및 전환 애니메이션 개선

**목표**: 앱 실행 시 보드가 이미 연결되어 있어도 연결 대기 화면을 잠깐 거치며 모드 진입을 안내하고, 스텝 인디케이터 구조 개편·펄스 점선 애니메이션·텍스트 슬라이드 전환·연결 해제 안내·USB 해제 감지 수정을 포함

**개발 기간**: 0.5-1일

**세부 목표**:

1. **스플래시 완료 후 항상 `WaitingForConnection` 경유**:
   - `onSplashFinished`에서 USB 연결 여부와 무관하게 **항상** `AppState.WaitingForConnection`으로 전환
   - `WaitingForConnection` 진입 시 현재 USB 상태를 스캔하여 이미 연결되어 있으면 스텝 자동 진행 시작

2. **스텝 자동 진행 (이미 연결된 경우)**:
   - `BridgeOneApp.kt`에 `connectionState` 상태 변수 및 `isAutoProgressing` 플래그 도입
   - `LaunchedEffect(appState)`: WaitingForConnection 진입 시 USB 스캔 → 연결 감지 시 자동 진행
   - 보드만 연결: `WaitingForUsb`(600ms) → `SearchingServer`(600ms) → `EnteringEssential`(1200ms) → `Active(ESSENTIAL)`
   - 보드+서버: `WaitingForUsb`(600ms) → `SearchingServer`(600ms) → `EnteringStandard`(1200ms) → `Active(STANDARD)`
   - 미연결: 기존과 동일하게 2초 주기 USB 폴링 대기
   - 대기 중 새로 USB 연결 시에도 `LaunchedEffect(isUsbConnected)`에서 동일한 자동 진행 시퀀스 실행

3. **`ConnectionState` 확장** (3개 상태 추가):
   ```kotlin
   /** Essential 모드 진입 안내 */
   object EnteringEssential : ConnectionState(
       primaryMessage = "Essential 모드로 진입합니다",
       secondaryMessage = "보드가 연결되었습니다",
       isProcessing = false, step = 2
   )

   /** Standard 모드 진입 안내 */
   object EnteringStandard : ConnectionState(
       primaryMessage = "Standard 모드로 진입합니다",
       secondaryMessage = "서버와 연결되었습니다",
       isProcessing = false, step = 3
   )

   /** 연결 해제됨 — 다시 연결 안내 */
   object Disconnected : ConnectionState(
       primaryMessage = "연결이 해제되었습니다",
       secondaryMessage = "USB 동글을 다시 연결해주세요",
       isProcessing = true, step = 1
   )
   ```

4. **연결 해제 시 재연결 안내**:
   - Active 상태(Essential/Standard)에서 USB 해제 시 `ConnectionState.Disconnected`로 설정 후 `WaitingForConnection` 전환
   - "연결이 해제되었습니다 / USB 동글을 다시 연결해주세요" 메시지가 재연결 시까지 유지
   - `LaunchedEffect(appState)`의 미연결 분기에서 `Disconnected` 상태를 `WaitingForUsb`로 덮어쓰지 않도록 조건 분기 (`connectionState !is ConnectionState.Disconnected`)
   - 초기 앱 실행의 "USB 동글을 연결해주세요"와 시각적으로 구분

5. **USB 해제 감지 버그 수정** (`UsbSerialManager.closePort()`):
   - **문제**: `closePort()`에서 내부 `isConnected = false`만 설정하고 `_debugState`의 `isConnected`를 갱신하지 않아 UI에서 해제를 감지하지 못함
   - **수정**: `closePort()`의 `finally` 블록에서 `_debugState.value`를 `isConnected = false`로 즉시 갱신, `_bridgeMode` 변경보다 **먼저** 실행
   - **추가 수정**: `LaunchedEffect(bridgeMode)`에 `isUsbConnected` 조건 추가 — USB 해제로 인한 bridgeMode 변경(STANDARD→ESSENTIAL)은 무시하고 `LaunchedEffect(isUsbConnected)`가 `WaitingForConnection` 전환을 담당

6. **스텝 인디케이터 구조 전면 개편** (`StepIndicator.kt`):
   - **문제**: 기존 Row 안에 Column(라벨+점)과 StepLine을 혼재시켜 연결선이 점 높이가 아닌 Row의 수직 중앙(라벨 높이 부근)에 위치
   - **수정**: 라벨 행과 점+선 행을 Column으로 분리
     - 상단 Row: `StepLabel` × 3 + `Spacer(LineGapWidth)` × 2
     - 하단: 단일 `Canvas`에서 점 3개 + 점선 2개 + 펄스를 정확한 좌표로 렌더링
   - 점선: `PathEffect.dashPathEffect(dash=4dp, gap=3dp)`, 점과 점 사이 4dp 패딩
   - 점 크기: 현재 단계 반지름 5dp, 비활성 4dp (animateFloatAsState)
   - 완료 구간 점선: 활성 색상 `#FF9800`, 미완료: `#404040`

7. **스텝 인디케이터 펄스 애니메이션**:
   - 현재 활성 단계의 점선 위를 펄스 점이 왼쪽→오른쪽으로 흘러감 (1.2초 주기, `infiniteRepeatable` + `LinearEasing`)
   - 코어 점: 반지름 2.5dp, 투명도 85%
   - 글로우: 반지름 6dp, 투명도 25%
   - 양 끝 12% 구간에서 페이드 인/아웃으로 자연스러운 출현/소멸

8. **텍스트 왼쪽 슬라이드 전환 애니메이션** (`ConnectionWaitingScreen.kt`):
   - 주/부 메시지를 `Crossfade` → `AnimatedContent`로 교체
   - 나가는 텍스트: 왼쪽 1/4폭 슬라이드 아웃 + 0.88배 축소 + fadeOut (350ms, `FastOutLinearInEasing`)
   - 들어오는 텍스트: 오른쪽 1/4폭에서 0.88배 작은 상태로 시작 → 원래 크기로 커지며 fadeIn (400ms, `FastOutSlowInEasing`)

**수정 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/BridgeOneApp.kt`
- `src/android/app/src/main/java/com/bridgeone/app/ui/connection/ConnectionState.kt`
- `src/android/app/src/main/java/com/bridgeone/app/ui/connection/ConnectionWaitingScreen.kt`
- `src/android/app/src/main/java/com/bridgeone/app/ui/connection/StepIndicator.kt`
- `src/android/app/src/main/java/com/bridgeone/app/usb/UsbSerialManager.kt`

**참조 문서**:
- `docs/android/design-guide-app.md` §8.1.1 (앱 실행 및 초기 상태 감지)
- `docs/android/styleframe-connection-waiting.md`

**검증**:
- [x] 보드 미연결 상태에서 앱 실행 → "USB 동글을 연결해주세요" 대기 화면 표시
- [x] 보드만 연결된 상태에서 앱 실행 → 스텝 1→2 진행 → "Essential 모드로 진입합니다" → Active(ESSENTIAL)
- [x] 보드+서버 연결 상태에서 앱 실행 → 스텝 1→2→3 진행 → "Standard 모드로 진입합니다" → Active(STANDARD)
- [x] 스텝 인디케이터 점선이 점과 점 사이에 정확히 위치
- [x] 스텝 인디케이터 점선 위 펄스 점이 현재 단계에서 다음 방향으로 흘러감 (글로우 포함)
- [x] 텍스트 전환 시 왼쪽 슬라이드 + 축소 + 페이드 애니메이션이 끊김 없이 부드러움
- [x] Essential 모드에서 USB 해제 시 "연결이 해제되었습니다" 메시지 표시 및 유지
- [x] Standard 모드에서 USB 해제 시 Essential이 아닌 "연결이 해제되었습니다" 화면으로 전환
- [x] 재연결 안내 메시지가 "USB 동글을 연결해주세요"로 변경되지 않고 유지
- [x] USB 재연결 시 자동 진행 시퀀스 정상 동작
- [x] 전체 자동 진행 체류 시간이 약 2.5초 (600ms+600ms+1200ms)
- [x] 에뮬레이터 및 실기기에서 전환 흐름 확인

---

## Phase 4.1.6: USB 재연결 안정성 및 모드 판별 타이밍 수정

**목표**: USB 케이블 재연결 시 앱 중복 실행 방지 및 모드 판별 경쟁 조건(Race Condition) 해결

**개발 기간**: 0.5일

**세부 목표**:

1. **앱 중복 실행 방지** (`AndroidManifest.xml`):
   - **문제**: 앱 사용 중 케이블이 빠졌다가 다시 연결되면 `USB_DEVICE_ATTACHED` intent로 인해 새 Activity 인스턴스가 생성되어 앱이 이중 실행
   - **수정**: `MainActivity`에 `android:launchMode="singleTask"` 추가
   - 앱이 이미 실행 중이면 새 인스턴스를 만들지 않고 기존 Activity의 `onNewIntent()`를 호출

2. **모드 판별 경쟁 조건 해결**:
   - **문제**: USB 해제 시 `bridgeMode`가 즉시 ESSENTIAL로 리셋되는데, 재연결 시 UI가 ESP32 응답을 기다리지 않고 이 값을 즉시 읽어 모드를 결정 → 서버가 실행 중이어도 항상 Essential로 잘못 진입하거나, "Essential 모드로 진입합니다" 메시지가 뜬 후 실제로는 Standard 화면이 표시되는 불일치 발생
   - **원인**: 폴링 스레드가 2초 대기 후 첫 쿼리를 전송하므로, UI가 모드를 판별하는 시점에 아직 ESP32 응답이 없음

3. **`UsbSerialManager`에 모드 확정 추적 추가**:
   - `modeConfirmed: StateFlow<Boolean>` 추가 — ESP32로부터 첫 모드 응답 수신 여부
   - `openPort()`: `_modeConfirmed.value = false`로 리셋 (스레드 시작 전)
   - `closePort()`: `_modeConfirmed.value = false`로 리셋
   - `receiverThread`: `EVENT_MODE_CHANGED` 수신 시 `_modeConfirmed.value = true` 설정

4. **폴링 스레드 즉시 첫 쿼리 전송**:
   - **기존**: `Thread.sleep(2000)` → 쿼리 전송 (첫 쿼리까지 2초 대기)
   - **수정**: 쿼리 전송 → `Thread.sleep(2000)` (첫 쿼리 즉시 전송, 이후 2초 간격 유지)

5. **`BridgeOneApp`에 모드 확정 대기 로직 추가**:
   - `waitForModeConfirmed()` suspend 함수 신설 — `modeConfirmed`가 true가 될 때까지 100ms 간격으로 확인 (최대 5초 타임아웃)
   - `LaunchedEffect(appState)`: WaitingForConnection 진입 시 USB 연결 감지 → `SearchingServer` 표시 → `waitForModeConfirmed()` → 확정된 모드로 진입
   - `LaunchedEffect(isUsbConnected)`: USB 새 연결 시 동일하게 모드 확정 대기 후 진입

**수정 파일**:
- `src/android/app/src/main/AndroidManifest.xml`
- `src/android/app/src/main/java/com/bridgeone/app/usb/UsbSerialManager.kt`
- `src/android/app/src/main/java/com/bridgeone/app/ui/BridgeOneApp.kt`

**검증**:
- [x] 앱 사용 중 케이블 분리 후 재연결 시 앱이 중복 실행되지 않음
- [x] Essential 모드에서 케이블 분리 → 서버 실행 → 재연결 시 Standard 모드로 정확히 진입
- [x] Standard 모드에서 케이블 분리 → 재연결 시 올바른 모드 안내 메시지와 실제 화면 일치
- [x] "서버를 찾고 있습니다" 단계에서 모드 확정까지 대기 후 진입
- [x] 서버 미실행 상태에서 재연결 시 Essential 모드로 정상 진입
- [x] 5초 타임아웃 시 현재 모드(ESSENTIAL)로 안전하게 진입

---

## Phase 4.1.7: Active 화면 상하단 Safe Zone 적용

**목표**: 화면 최상단·최하단에 컴포넌트가 배치되지 않는 Safe Zone을 전역으로 적용하여 오조작 방지

**개발 기간**: 0.5일 미만

**세부 목표**:

1. **문제 정의**:
   - 내비게이션 바가 숨겨진 몰입 모드(`BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`)에서, 내비게이션 바를 꺼내려면 화면 하단에서 위로 스와이프해야 함
   - 스와이프 시작점이 최하단이 아닌 약간 올라온 지점이면 하단 컴포넌트가 의도치 않게 터치될 수 있음
   - 사용자의 신체적 특성상 화면 최상단·최하단은 엄지손가락으로 닿기 어려워 컴포넌트를 배치해도 활용 가치가 낮음

2. **Safe Zone 상수 정의** (`LayoutConstants.kt` 신규 생성):
   ```kotlin
   val TOP_SAFE_ZONE    = 40.dp  // 상단 — 손이 닿기 어려운 영역
   val BOTTOM_SAFE_ZONE = 40.dp  // 하단 — 내비게이션 스와이프 제스처 영역
   ```

3. **전역 적용 지점 (`BridgeOneApp.kt`)**:
   - `AppState.Active` 렌더링 Box에 `padding(top = TOP_SAFE_ZONE, bottom = BOTTOM_SAFE_ZONE)` 추가
   - 이 한 지점만 수정하면 `EssentialModePage`, `StandardModePage` 등 모든 Active 페이지가 자동으로 Safe Zone 안에 배치됨
   - 값 조정 시 `LayoutConstants.kt` 한 파일만 수정

**신규 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/common/LayoutConstants.kt`

**수정 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/BridgeOneApp.kt`

**검증**:
- [x] Active 화면 하단에 40dp Safe Zone이 확보되어 컴포넌트가 배치되지 않음
- [x] Active 화면 상단에 40dp Safe Zone이 확보되어 컴포넌트가 배치되지 않음
- [x] `EssentialModePage`, `StandardModePage` 양쪽 모두 Safe Zone 적용 확인
- [x] Splash, ConnectionWaitingScreen 등 Active 외 화면에는 Safe Zone이 적용되지 않음
- [x] `LayoutConstants.kt`의 값 변경 시 모든 페이지에 즉시 반영

---

## Phase 4.1.8: 커스텀 상태 알림 토스트 (Status Toast)

**목표**: 시스템 기본 Toast를 대체하는 세련된 커스텀 토스트 컴포넌트로 앱의 전체 상태 알림 경험을 고도화

**개발 기간**: 0.5일

**세부 목표**:

1. **`StatusToast.kt` 신규 컴포넌트**:
   - `ToastType` enum: INFO, SUCCESS, WARNING, ERROR 4가지 상태
   - `ToastMessage` data class: 메시지, 타입, 지속시간 관리
   - `ToastController` 싱글톤: 어디서든 `ToastController.show(...)` 호출 가능
   - `StatusToastOverlay` Composable: BridgeOneApp 최상위 Box에 배치

2. **디자인 구현**:
   - **형태**: 알약 모양 (28dp border-radius)
   - **색상**: 상태별 배경 (INFO: #2196F3 파란색, SUCCESS: #4CAF50 초록색, WARNING: #FF9800 주황색, ERROR: #F44336 빨간색)
   - **아이콘**: 흰 원형 배경(34dp) 위에 배경색 아이콘(26dp) 표시
   - **텍스트**: 13sp Pretendard-Medium, 대부분 흰색 (WARNING만 검은색 #121212)
   - **애니메이션**:
     - 등장: 상단에서 슬라이드 인, 350ms, EaseOutBack (탄력감)
     - 퇴장: 상단으로 슬라이드 아웃, 300ms, EaseInBack (당겨지는 느낌)
   - **위치**: 상태표시줄 아래 35dp, 화면 중앙 정렬, 좌우 16dp 여백
   - **높이**: 약 38dp (상하 10dp 패딩 + 18dp 아이콘/텍스트)

3. **의존성 추가**:
   - `libs.versions.toml`: `androidx-compose-material-icons-extended` 라이브러리 등록
   - `build.gradle.kts`: `implementation(libs.androidx.compose.material.icons.extended)` 추가
   - Material Icons Extended에서 Info, CheckCircle, Warning, Error 아이콘 사용

4. **기존 Toast 제거**:
   - `BridgeOneApp.kt`: 모드 전환 토스트를 `android.widget.Toast` → `ToastController.show(...)` 교체
   - `ConnectionWaitingScreen.kt`: 뒤로가기 확인 토스트를 커스텀 토스트로 교체

5. **토스트 호출 예시**:
   ```kotlin
   // INFO 타입, 2초 후 자동 사라짐
   ToastController.show("Standard 모드로 전환되었습니다", ToastType.INFO, 2000L)

   // WARNING 타입, 1초 후 자동 사라짐
   ToastController.show("앱을 종료하시겠습니까?", ToastType.WARNING, 1000L)

   // SUCCESS 타입, 3초 후 자동 사라짐
   ToastController.show("연결 성공", ToastType.SUCCESS, 3000L)

   // ERROR 타입, 무제한 표시
   ToastController.show("연결 실패", ToastType.ERROR, TOAST_DURATION_INFINITE)
   ```

**신규 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/common/StatusToast.kt`

**수정 파일**:
- `src/android/gradle/libs.versions.toml` (material-icons-extended 등록)
- `src/android/app/build.gradle.kts` (material-icons-extended 의존성)
- `src/android/app/src/main/java/com/bridgeone/app/ui/BridgeOneApp.kt`
- `src/android/app/src/main/java/com/bridgeone/app/ui/connection/ConnectionWaitingScreen.kt`

**검증**:
- [x] ToastController를 통해 모드 전환 토스트 표시 (BridgeOneApp)
- [x] ToastController를 통해 뒤로가기 확인 토스트 표시 (ConnectionWaitingScreen)
- [x] INFO 타입 토스트 표시 (파란색 배경, 흰 텍스트)
- [x] SUCCESS 타입 토스트 표시 (초록색 배경, 흰 텍스트)
- [x] WARNING 타입 토스트 표시 (주황색 배경, 검은 텍스트)
- [x] ERROR 타입 토스트 표시 (빨간색 배경, 흰 텍스트)
- [x] 등장 애니메이션 정상 실행 (상단에서 탄력감 있게 내려옴)
- [x] 퇴장 애니메이션 정상 실행 (상단으로 스르륵 올라감)
- [x] durationMs 설정에 따라 자동 사라짐 (3000ms 이내)
- [x] TOAST_DURATION_INFINITE 사용 시 무제한 표시
- [x] 아이콘이 흰 원형 배경 위에 배경색으로 명확히 표시
- [x] 새로운 토스트가 나타나면 기존 토스트 교체

---

## 반응형 규칙 (전체 Phase 4.1 공통)

| 화면 크기 | 스플래시 | 연결 대기 |
|-----------|---------|----------|
| 폭 < 360dp | 기본 크기 유지 | 아이콘 96dp, 텍스트 유지 |
| 360-600dp | 기본 크기 | 아이콘 128dp (기본) |
| 폭 ≥ 600dp | 확대 가능 | 아이콘 160dp, 가로 배치 고려 |
