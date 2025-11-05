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

### 3. Windows 서버 (선택적, `src/windows/BridgeOne/`)
- **언어**: C#
- **플랫폼**: WPF
- **주요 역할**: 양방향 통신 및 고급 기능 제공 (매크로, 멀티 커서 등)

## 주요 개발 명령어

### Android 앱 개발

```bash
# Android Studio 프로젝트 디렉토리
cd src/android

# Gradle 동기화
./gradlew sync

# 빌드 (Debug)
./gradlew assembleDebug

# 빌드 (Release)
./gradlew assembleRelease

# 디바이스에 설치
./gradlew installDebug

# 린트 검사
./gradlew lint

# 테스트 실행
./gradlew test                    # 유닛 테스트
./gradlew connectedAndroidTest    # 계측 테스트 (디바이스 필요)

# 클린 빌드
./gradlew clean
```

### ESP32-S3 펌웨어 개발

```bash
# ESP-IDF 환경 활성화 (Windows)
cd <ESP-IDF 설치 경로>
export.bat

# ESP-IDF 환경 활성화 (Linux/macOS)
source <ESP-IDF 설치 경로>/export.sh

# BridgeOne 펌웨어 디렉토리
cd src/board/BridgeOne

# 빌드
idf.py build

# 플래시 (PORT는 COM3, /dev/ttyUSB0 등)
idf.py -p <PORT> flash

# 모니터링 (디버그 로그 확인, Ctrl+] 종료)
idf.py -p <PORT> monitor

# 빌드 + 플래시 + 모니터링 (한 번에)
idf.py -p <PORT> flash monitor

# 사용 가능한 포트 확인
idf.py list-ports

# 빌드 설정 변경
idf.py menuconfig

# 클린 빌드
idf.py fullclean
```

## 코드 구조 및 주요 컴포넌트

### Android 앱 구조

```
src/android/app/src/main/java/com/bridgeone/app/
├── MainActivity.kt           # 메인 액티비티
├── ui/
│   ├── BridgeOneApp.kt       # 최상위 Composable
│   └── theme/                # Material3 테마 설정
│       ├── Color.kt
│       ├── Type.kt
│       └── Theme.kt
```

**주요 특징**:
- **Jetpack Compose**: 선언적 UI 프레임워크 사용
- **Material3**: 다크 테마만 지원
- **Pretendard 폰트**: 접근성을 위한 가독성 높은 한글 폰트
- **USB Serial**: `usbSerialForAndroid` 라이브러리 사용

### ESP32-S3 펌웨어 구조

```
src/board/BridgeOne/main/
├── BridgeOne.c           # 메인 엔트리 포인트 (app_main)
├── hid_handler.c         # USB HID 마우스 처리
├── uart_handler.c        # UART 통신 처리
└── usb_descriptors.c     # USB 디스크립터 정의
```

**주요 특징**:
- **FreeRTOS 태스크**: `usb_task`, `hid_task`, `uart_task` 등 멀티태스킹
- **TinyUSB**: HID Keyboard + Mouse + CDC 복합 장치 구현
- **UART 통신**: 1Mbps 속도로 Android와 통신
- **델타 프레임**: 8바이트 고정 크기 프레임으로 마우스 입력 전송

## 통신 프로토콜

### UART 델타 프레임 (Android → ESP32-S3)

```c
// 8바이트 고정 크기 프레임 (Little-Endian)
struct MouseFrame {
    uint8_t  seq;      // 순번 (유실 감지용)
    uint8_t  buttons;  // bit0: L, bit1: R, bit2: M
    int16_t  dx;       // 상대 X 이동
    int16_t  dy;       // 상대 Y 이동
    int8_t   wheel;    // 휠 (Boot 모드에서는 0)
    uint8_t  flags;    // 제어 플래그
};

// 전송 주기: 4-8ms (125-250 Hz)
```

### USB HID 프로토콜 (ESP32-S3 → PC)

```c
// Boot Protocol (BIOS/BitLocker 호환)
struct HidMouseReport {
    uint8_t buttons;   // 마우스 버튼 상태
    int8_t  x;         // X 이동 (-127~127)
    int8_t  y;         // Y 이동 (-127~127)
};
```

## 중요 기술 스택

### Android
- **최소 SDK**: 24 (Android 7.0)
- **타겟 SDK**: 36 (Android 14)
- **컴파일 SDK**: 36
- **Kotlin**: JVM 타겟 17
- **Compose BOM**: androidx.compose.bom
- **USB Serial**: `com.github.mik3y:usb-serial-for-android`

### ESP32-S3
- **MCU**: ESP32-S3-DevkitC-1-N16R8
- **메모리**: 16MB Flash, 8MB PSRAM
- **프레임워크**: ESP-IDF v5.5+
- **USB**: TinyUSB 스택
- **RTOS**: FreeRTOS

## 개발 가이드라인

### USB 통신 최적화
- Android에서 ESP32-S3 USB Serial 장치 인식 시 VID: 0x303A 필터링
- 1Mbps UART 속도 필수 설정 (8N1)
- 8바이트 델타 프레임 구조 준수
- 순번(seq) 필드로 패킷 유실 감지

### 터치패드 알고리즘
- **데드존**: 작은 움직임 무시하여 손떨림 방지
- **델타 계산**: 이전 터치 위치와의 상대 이동량 계산
- **클릭 감지**: 짧은 탭 감지 시 마우스 클릭 전송
- **비동기 전송**: 120Hz 주기로 프레임 전송 (디바이스 성능에 따라 적응)

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

### 개발 계획 문서 (`docs/development-plans/`)
- `phase-*-*.md`: 단계별 개발 계획 및 체크리스트

## Git 워크플로우

### 최근 개발 히스토리
- Phase 2.1.9.3: 클릭 감지 및 비동기 프레임 전송 완성
- Phase 2.1.9.2: 델타 계산 및 데드존 보상 알고리즘 구현 완료
- Phase 2.1.9.1: 터치패드 UI 및 터치 이벤트 감지 구현
- Phase 2.1.8.4: 프레임 전송 및 USB 연결 모니터링 구현 완료
- Phase 2.1.8.3: UART 통신 설정 및 포트 관리 구현 완료

### 브랜치 전략
- **main**: 안정적인 릴리스 브랜치
- 기능 개발 시 feature 브랜치 사용 권장

## 주의사항

### Windows USB 성능 최적화
Windows 11에서 ESP32-S3 USB CDC 통신 시 다음 레지스트리 설정이 필요할 수 있습니다:

```
경로: HKEY_LOCAL_MACHINE\SYSTEM\CurrentControlSet\Enum\USB\VID_303A&PID_82C5\Device Parameters
이름: IdleUsbSelectiveSuspendPolicy
타입: DWORD (32-bit)
값: 0x00000001
```

설정 후 시스템 재부팅 필요.

### 개발 환경
- **Android 개발**: Android Studio (최신 버전 권장)
- **ESP32-S3 개발**: ESP-IDF v5.5+ 설치 필수
- **Python**: 3.8 이상 (ESP-IDF 요구사항)

### 테스트
- Android 앱 테스트 시 USB-OTG 지원 실제 디바이스 필요
- ESP32-S3 펌웨어 테스트 시 ESP32-S3-DevkitC-1 보드 필요
- 전체 시스템 통합 테스트 시 Android 디바이스, ESP32-S3 보드, PC 모두 필요
