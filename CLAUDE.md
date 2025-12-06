# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

**BridgeOne**는 근육장애로 인해 기존 키보드/마우스 사용이 어려운 사용자를 위한 접근성 우선 Android-PC 입력 브릿지입니다. Android 스마트폰을 터치패드로 사용하여 ESP32-S3 하드웨어 동글을 통해 PC의 마우스/키보드를 제어합니다.

### 핵심 설계 원칙
- **단일 터치**: 모든 기능을 한 번의 터치로 수행
- **컴팩트 레이아웃**: 중앙 하단 240×280dp 영역에 모든 조작 집중
- **한손 조작**: 엄지손가락만으로 완전한 PC 제어
- **즉시 사용**: 복잡한 설정 없이 연결만 하면 바로 사용 가능

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

## 상세 참조 문서 (Slash Commands)

특정 작업 시 필요한 상세 정보는 다음 슬래시 명령어로 참조하세요:

| 명령어 | 용도 |
|--------|------|
| `/bridgeone-esp32` | ESP32-S3 하드웨어: USB 포트, 로그 경로, 연결 방식, TinyUSB 설정 |
| `/bridgeone-protocol` | 통신 프로토콜: UART 프레임 구조, USB HID 프로토콜, 키코드 매핑 |
| `/bridgeone-commands` | 개발 명령어: Android Gradle, ESP-IDF, Git 워크플로우 |
| `/bridgeone-troubleshoot` | 트러블슈팅: 디버깅 가이드, 환경 설정, 자주 발생하는 문제 |

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

## 개발 가이드라인

### USB 통신 최적화
- Android에서 ESP32-S3 USB Serial 장치 인식 시 VID: 0x303A 필터링
- 1Mbps UART 속도 필수 설정 (8N1)
- 8바이트 복합 프레임 구조 준수
- 순번(seq) 필드로 패킷 유실 감지
- 마우스와 키보드 입력을 단일 프레임으로 전송

### 터치패드 알고리즘
- **데드존**: 작은 움직임 무시하여 손떨림 방지 (기본 8px)
- **델타 계산**: 이전 터치 위치와의 상대 이동량 계산 (DeltaCalculator)
- **클릭 감지**: 짧은 탭 감지 시 마우스 클릭 전송 (ClickDetector)
- **비동기 전송**: 120Hz 주기로 프레임 전송 (디바이스 성능에 따라 적응)

### 키보드 레이아웃
- **수정자 키**: Ctrl, Shift, Alt, Win 조합 지원
- **단축키**: 자주 사용하는 단축키를 큰 버튼으로 제공
- **탭 시스템**: 페이지 좌우 슬라이드로 터치패드 ↔ 키보드 전환
- **HID Usage**: 표준 HID 키코드 사용

### 접근성 고려사항
- **최소 터치 영역**: 80×60dp (권장)
- **햅틱 피드백**: 모든 터치 이벤트에 진동 응답
- **시각적 피드백**: 연결 상태 및 처리 결과 명확히 표시
- **컴팩트 레이아웃**: 중앙 하단 240×280dp 영역에 UI 집중

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

### 개발 계획 문서 (`docs/development-plans/`)
- `phase-*-*.md`: 단계별 개발 계획 및 체크리스트

## Git 워크플로우

### 최근 개발 히스토리
- Phase 2.3.1.1: Android → ESP32-S3 USB Serial 인식 검증 완료
- Phase 2.3.1.0: USB 초기화 및 장치 자동 감지 기능 구현
- Phase 2.2.5: 최종 검증 및 호환성 확인 완료
- Phase 2.2.4.3: 수정자 키 및 키 조합 최적화 완료
- Phase 2.2.4.2: 키보드 레이아웃 최적화 및 탭 시스템 구현
- Phase 2.2.4.1: KeyboardKeyButton 컴포넌트 시각적/UX 개선 완료
- Phase 2.2.3.3: 클릭 감지 및 프레임 생성 자동화 완료
- Phase 2.2.3.2: 델타 계산 및 데드존 보상 알고리즘 구현
- Phase 2.2.3.1: 터치패드 UI 및 터치 이벤트 감지 구현 완료

### 브랜치 전략
- **main**: 안정적인 릴리스 브랜치
- 기능 개발 시 feature 브랜치 사용 권장

## Claude의 Git 커밋 정책

**핵심 원칙: Claude는 절대 직접 Git 커밋을 수행하지 않습니다.**

유저가 커밋 관련 작업을 제안하거나 요청하는 경우, Claude는:
- **절대 직접 수행 금지**: `git add`, `git commit`, `git push` 등의 커밋 작업을 절대 직접 수행하지 않습니다
- **변경사항 설명**: 커밋할 변경사항을 명확히 설명하고, 권장 커밋 메시지를 제안합니다
- **단계별 가이드**: 유저가 스스로 커밋을 수행할 수 있도록 단계별 절차를 안내합니다
- **조언 제공**: 필요한 경우, 커밋 메시지 작성이나 변경사항 검토에 대한 기술적 조언을 제공합니다

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
