# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

**BridgeOne**는 근육장애로 인해 기존 키보드/마우스 사용이 어려운 사용자를 위한 접근성 우선 Android-PC 입력 브릿지입니다. Android 스마트폰을 터치패드로 사용하여 ESP32-S3 하드웨어 동글을 통해 PC의 마우스/키보드를 제어합니다.

## 시스템 아키텍처

본 프로젝트는 3개의 독립적인 서브 프로젝트로 구성됩니다:

### 1. Android 클라이언트 (`src/android/`)
- **언어**: Kotlin
- **UI 프레임워크**: Jetpack Compose
- **아키텍처**: MVVM + Clean Architecture
- **통신**: USB-OTG (Android USB Host API)
- **주요 역할**: 터치패드 UI 제공 및 ESP32-S3로 입력 전송

### 2. ESP32-S3 하드웨어 동글 (`src/board/BridgeOne/`)
- **플랫폼**: ESP-IDF v5.5+
- **언어**: C/C++
- **USB 스택**: TinyUSB (HID + CDC 복합 장치)
- **주요 역할**: Android로부터 UART로 입력 수신 → USB HID로 PC에 전송
- **지원 보드**: YD-ESP32-S3 N16R8 (YD-ESP32-23, VCC-GND Studio 클론)

### 3. Windows 서버 (선택적, `src/windows/BridgeOne/`)
- **언어**: C#
- **플랫폼**: WPF
- **주요 역할**: 양방향 통신 및 고급 기능 제공 (매크로, 멀티 커서 등)

## 코드 구조 및 주요 컴포넌트

### Android 앱 구조

```
src/android/app/src/main/java/com/bridgeone/app/
├── MainActivity.kt                    # 메인 액티비티
├── protocol/                          # 통신 프로토콜 계층
│   ├── BridgeFrame.kt                # 8바이트 프레임 데이터 클래스
│   └── FrameBuilder.kt               # 프레임 생성 및 순번 관리
├── usb/                              # USB 통신 계층
│   ├── UsbSerialManager.kt           # UART 통신 관리
│   ├── UsbConstants.kt               # ESP32-S3 통신 상수
│   ├── DeviceDetector.kt             # ESP32-S3 자동 감지
│   ├── UsbPermissionReceiver.kt      # USB 권한 처리
│   └── UsbDeviceDetectionReceiver.kt # 자동 연결/해제 감지
└── ui/                               # UI 계층
    ├── BridgeOneApp.kt               # 최상위 Composable
    ├── components/                   # UI 컴포넌트
    │   ├── TouchpadWrapper.kt        # 터치패드 컴포넌트
    │   ├── KeyboardLayout.kt         # 키보드 레이아웃
    │   └── KeyboardKeyButton.kt      # 키보드 버튼
    ├── utils/                        # UI 유틸리티
    │   ├── DeltaCalculator.kt        # 델타 이동량 계산
    │   └── ClickDetector.kt          # 클릭 감지 알고리즘
    └── theme/                        # Material3 테마 설정
        ├── Color.kt
        ├── Type.kt
        └── Theme.kt
```

### ESP32-S3 펌웨어 구조

```
src/board/BridgeOne/main/
├── BridgeOne.c           # 메인 엔트리 포인트 (app_main)
├── hid_handler.c/h       # USB HID 마우스/키보드 처리
├── uart_handler.c/h      # UART 통신 처리
├── usb_descriptors.c/h   # USB 디스크립터 정의
└── tusb_config.h         # TinyUSB 설정
```

## 중요 기술 스택

### Android
- **최소 SDK**: 24 (Android 7.0)
- **타겟 SDK**: 36 (Android 14)
- **컴파일 SDK**: 36
- **Kotlin**: JVM 타겟 17
- **Compose BOM**: androidx.compose.bom
- **USB Serial**: `com.github.mik3y:usb-serial-for-android`
- **Permissions**: `com.google.accompanist:accompanist-permissions`

### ESP32-S3
- **MCU**: ESP32-S3 N16R8 (YD-ESP32-S3 호환 보드)
- **메모리**: 16MB Flash, 8MB Octal SPI PSRAM
- **프레임워크**: ESP-IDF v5.5+
- **USB**: TinyUSB 스택
- **RTOS**: FreeRTOS
- **UART 통신**: UART0 (GPIO43/44, CH343P 브릿지)

## 문서 구조

프로젝트 문서는 `docs/` 디렉토리에 체계적으로 정리되어 있습니다:

### Android 관련 문서 (`docs/android/`)
- `technical-specification-app.md`: Android 앱 기술 명세서
- `design-guide-app.md`: 앱 전체 디자인 가이드
- `component-*.md`: 개별 UI 컴포넌트 설계 명세
- `styleframe-*.md`: 페이지별 스타일프레임

### ESP32-S3 관련 문서 (`docs/board/`)
- `esp32s3-code-implementation-guide.md`: ESP32-S3 펌웨어 구현 가이드
- `YD-ESP32-S3-N16R8-analysis.md`: YD-ESP32-S3 보드 분석 및 호환성 평가
- `YD-ESP32-S3-migration-guide.md`: YD-ESP32-S3 보드 마이그레이션 가이드

### Windows 서버 관련 문서 (`docs/windows/`)
- `technical-specification-server.md`: Windows 서버 기술 명세서 (§3.10: Software Macro + §3.10.10: Native Macro 관계)
- `design-guide-server.md`: 서버 앱 디자인 가이드

### 개발 계획 문서 (`docs/development-plans/`)
- `phase-*-*.md`: 단계별 개발 계획 및 체크리스트

### Native Macro 관련 문서 (각 플랫폼 문서에 분산)
- **시스템 전체 플로우**: `docs/technical-specification.md` §4.4.2
- **ESP32-S3 구현 명세**: `docs/board/esp32s3-code-implementation-guide.md` §4.6
- **Android 구현 명세**: `docs/android/technical-specification-app.md` §2.9
- **Windows 서버 관계**: `docs/windows/technical-specification-server.md` §3.10.10

## Claude의 Phase 개발 작업 가이드라인

**핵심 원칙: `docs/development-plans/` 내 Phase 문서에 따라 개발 작업을 진행할 때 아래 규칙을 반드시 따릅니다.**

### 1. 작업 시작 전 쉬운 설명
- 각 Phase(또는 하위 Phase) 작업을 시작하기 전에, **이 작업이 무엇인지 비기술적이고 쉬운 말로 먼저 설명**합니다
- 유저가 지금 무슨 작업이 진행되는지 직관적으로 이해할 수 있도록 합니다

### 2. 하위 Phase 완료 직후 (후속 Phase 문서 영향 검토)

> **이 단계를 건너뛰면 다음 번 작업 시 맥락이 끊깁니다. 반드시 수행하세요.**

각 하위 Phase(예: Phase 4.1.4) 구현이 끝나면, **커밋 전에** 아래 체크리스트를 실행합니다.

> **적용 범위**: 계획대로 진행한 Phase든, 계획보다 다르게 구현한 Phase든, 계획에 없던 Phase를 새로 추가한 경우든 **모두 동일하게 적용**합니다. 계획에 있었어도 막상 구현하다 보면 더 나은 방향이 떠올라 달라질 수 있고, 그 차이가 후속 Phase에 영향을 줄 수 있습니다.

#### 완료 직후 체크리스트
1. **신규 파일·컴포넌트**: 이번 Phase에서 새로 만든 파일/Composable/클래스가 있는가?
   - 있으면 → 후속 Phase에서 이를 재사용하거나 고려해야 하는지 확인하고, 해당 Phase 문서에 기록
2. **API·상태 구조 변경**: 기존 클래스/StateFlow/파라미터가 추가·변경됐는가?
   - 있으면 → 이 클래스를 다루는 후속 Phase 문서에 변경 내용 기록
3. **레이아웃·제약 변화**: 전역 패딩, Safe Zone, 몰입 모드 등 레이아웃 전제가 달라졌는가?
   - 있으면 → UI를 다루는 모든 후속 Phase 문서에 기록
4. **계획과 다르게 구현된 부분**: 원래 Phase 문서에 적혀 있던 것과 실제 구현이 다른 부분이 있는가?
   - 있으면 → 그 차이가 후속 Phase의 전제(재사용 컴포넌트, API 호출 방식, 동작 흐름 등)에 영향을 주는지 확인하고, 해당 Phase 문서에 기록
5. **계획에 없던 새 하위 Phase 추가**: 원래 계획에 없던 하위 Phase를 새로 만들었는가?
   - 있으면 → 그 Phase에서 발생한 ①②③④ 변경사항도 동일하게 후속 Phase에 전파

#### 문서 최상단이 아닌 영향받는 하위 Phase 섹션 안에 직접 기록
변경 내용을 Phase 문서 최상단에 일괄 나열하지 않습니다. **실제로 영향을 받는 하위 Phase 섹션**(예: `## Phase 4.2.2`) 안에 직접 기록해야, 그 Phase 작업 시 즉시 눈에 띕니다.

```markdown
## Phase 4.2.2: Page 1 레이아웃 정식 구현
...

> **⚠️ Phase 4.1.7 변경사항**: `LayoutConstants.kt` 신규 (TOP/BOTTOM_SAFE_ZONE = 40dp).
> `BridgeOneApp.kt`의 Active 박스에 이미 padding(40dp) 적용됨 → 이 레이아웃 내부에서 safe zone 패딩 추가 불필요.
> 유효 화면 높이 = 전체 높이 − 80dp 기준으로 설계.
```

#### 변경 사항 전파 규칙
- **설계 문서 반영 기준**: 다른 컴포넌트의 public API, 전역 레이아웃 제약, 아키텍처 구조가 바뀐 경우 → `docs/` 설계 문서에도 함께 반영 (내부 구현만 바뀐 사소한 차이는 제외)
- **적용 범위**: 계획대로 진행된 Phase든, 계획과 달리 구현된 Phase든, 예상치 못해 추가된 Phase든 동일하게 적용

### 3. 설계 문서 부족 시 먼저 보강
- Phase 작업에 필요한 설계 내용이 관련 문서(`docs/`)에 **누락되거나 부족하다면, 먼저 해당 설계 문서를 보강**합니다
- 설계 문서 보강이 완료된 후에 개발 계획 문서(`docs/development-plans/`)를 수정합니다
- 순서: **설계 문서 보강 → 개발 계획 문서 반영 → 구현 진행**

### 4. 이미 완료된 Phase는 삭제
- 해당 Phase의 작업 내용이 **이미 코드에 잘 적용되어 있어서 진행할 필요가 없다면**, 해당 Phase(하위 Phase 포함)를 문서에서 **아예 삭제**합니다

## Claude의 Git 커밋 정책

**핵심 원칙: Claude는 보통 직접 Git 커밋을 수행하지 않으나, 유저가 명시적으로 요청하면 진행합니다.**

### 기본 동작 (유저 요청 없을 시)
- **변경사항 설명**: 커밋할 변경사항을 명확히 설명하고, 권장 커밋 메시지를 제안합니다
- **단계별 가이드**: 유저가 스스로 커밋을 수행할 수 있도록 단계별 절차를 안내합니다
- **조언 제공**: 필요한 경우, 커밋 메시지 작성이나 변경사항 검토에 대한 기술적 조언을 제공합니다

### 유저 요청 시 (예: "커밋해줄래?", "변경사항 commit 부탁", "지금 바로 커밋")
- **직접 수행**: `git add`, `git commit`, `git push` 등의 커밋 작업을 직접 실행합니다
- **투명한 진행**: 수행할 작업과 커밋 메시지를 명확히 제시한 후 진행합니다
- **결과 확인**: 커밋 완료 후 결과를 보고합니다

## Claude의 Android 빌드 정책

**핵심 원칙: Android 코드를 수정한 후에는 반드시 빌드를 직접 수행합니다.**

Android 개발 작업 시, Claude는:
- **빌드 필수 수행**: Android 코드 수정이 완료되면 `./gradlew assembleDebug` (또는 필요시 `assembleRelease`)를 직접 실행합니다
- **에러 즉시 수정**: 빌드 에러가 발생하면 에러 로그를 분석하고 반드시 수정합니다. 에러가 모두 해결될 때까지 빌드-수정 사이클을 반복합니다
- **경고 보고**: 빌드 경고(Warning)는 수정하지 않더라도 유저에게 보고합니다
- **빌드 경로**: Android 프로젝트 루트 `src/android/`에서 Gradle 명령을 실행합니다

### 빌드 명령어
```
# Android 프로젝트 디렉토리로 이동 후 실행
cd src/android
./gradlew assembleDebug        # 디버그 빌드 (일반적)
./gradlew assembleRelease      # 릴리즈 빌드 (필요시)
./gradlew build                # 전체 빌드 (테스트 포함)
```

> **⚠️ Windows 환경 주의**: PowerShell에서는 `./gradlew` 대신 `.\gradlew` 또는 `gradlew.bat` 사용

## Claude의 펌웨어 빌드 정책

**핵심 원칙: 보드 펌웨어 빌드는 LLM이 직접 수행하지 않고 유저에게 위임합니다.**

ESP32-S3 펌웨어 빌드/플래시 관련 작업 시, Claude는:
- **직접 빌드 금지**: `idf.py build`, `idf.py flash` 등의 빌드/플래시 명령을 직접 실행하지 않습니다
- **코드 수정만 수행**: 펌웨어 소스 코드 수정, 분석, 리뷰 작업은 정상적으로 수행합니다
- **명령어 안내**: 빌드가 필요한 경우, 유저가 실행할 정확한 명령어를 제공합니다
- **결과 분석**: 유저가 빌드 결과를 공유하면 오류 분석 및 해결책을 제시합니다

## Claude의 하드웨어 설명 가이드라인

**핵심 원칙: 유저는 임베디드 지식이 전혀 없습니다.**

하드웨어 관련 설명이 필요한 경우, Claude는:
- **비기술적 설명 우선**: "GPIO43 핀을 TX에 연결" 대신 "보드의 43번이라고 적힌 구멍과 TX라고 적힌 구멍을 점퍼선으로 연결"처럼 직관적으로 설명합니다
- **시각적 안내 제공**: 가능한 경우 연결 위치를 그림이나 표로 설명합니다
- **단계별 가이드**: 복잡한 연결은 한 번에 하나씩 순서대로 안내합니다
- **용어 설명 포함**: 점퍼선, 핀, 헤더 등 기본 용어도 필요시 간단히 설명합니다
- **실수 방지**: 잘못 연결하면 위험한 경우 명확히 경고합니다

## Claude의 PowerShell 환경 가이드라인

**⚠️ 중요: 이 프로젝트는 Windows PowerShell 환경에서 개발됩니다**

### Unix/Linux 명령어 사용 금지
- **절대 제안하지 않음**: `grep`, `sed`, `awk`, `cat`, `ls`, `find`, `xargs`, `cut`, `sort` 등 Unix/Linux 명령어 제안 금지
- **절대 실행하지 않음**: PowerShell 터미널에서 이러한 명령어를 실행하지 않습니다

### PowerShell 대안 제시
| 작업 | Unix 명령어 | Claude 도구 |
|------|-----------|-----------|
| 파일 검색 | `find` | `Glob` (권장) |
| 내용 검색 | `grep` | `Grep` (권장) |
| 파일 읽기 | `cat` | `Read` (권장) |
| 텍스트 처리 | `sed`, `awk` | `Edit` (권장) |
| 파일 작성 | `echo >` | `Write` (권장) |

### 핵심 규칙
- **도구 우선**: 파일/내용 작업 → Claude 도구 사용 (Glob, Grep, Read, Edit, Write)
- **PowerShell 사용**: 실제 시스템 명령 (git, gradle, build 등)
- **Unix 금지**: grep, sed, awk, find, cat 등 절대 제안/실행 금지
