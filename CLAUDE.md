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

## YD-ESP32-S3 N16R8 USB 포트 정의

**⚠️ 중요: 보드에는 2개의 독립적인 USB 포트가 있습니다**

### 포트 1️⃣: CH343P USB-to-UART 브릿지 (USB-C 포트)
```
용도: Android 통신 및 펌웨어 플래싱
특징:
  - 칩셋: WCH Qinheng CH343P
  - GPIO43 (TX), GPIO44 (RX) → UART0
  - VID/PID: 0x1A86:0x55D3
  - 최대 3 Mbps 속도 (BridgeOne은 1 Mbps 사용)
  - 하드웨어 TX/RX LED 내장

Android 연결:
  - Android 스마트폰 → USB-OTG 케이블 → 포트 1️⃣
  - 표준 CDC-ACM 드라이버 자동 인식
  - 1Mbps, 8N1 통신

펌웨어 개발:
  idf.py -p <포트명> flash        # 펌웨어 플래시
  idf.py -p <포트명> monitor      # 시리얼 모니터링
Windows: COM 포트 (예: COM3, COM4)
Linux/macOS: /dev/ttyUSB0, /dev/ttyACM0

⚠️ 주의: 펌웨어 플래싱 시 Android 연결을 해제해야 합니다.
```

### 포트 2️⃣: ESP32-S3 Native USB OTG (Micro-USB 포트, 보드 상단)
```
용도: HID 통신 (PC와 마우스/키보드 신호) + 디버그 로깅
특징:
  - GPIO19 (USB_D-), GPIO20 (USB_D+)
  - TinyUSB 스택 기반
  - HID Boot Protocol 지원 (BIOS/BitLocker 호환)
  - CDC 시리얼 통신도 동시 지원 (디버그 로그 출력)

디버그 로깅:
  - ESP_LOG 출력이 이 포트의 CDC 인터페이스로 리다이렉트됨
  - PC에서 시리얼 터미널로 연결하여 로그 확인 (Tera Term 권장)
  - Windows: 장치 관리자 → 포트(COM & LPT) → "USB Serial Device (COMx)"
  - Linux: /dev/ttyACM0
  - macOS: /dev/cu.usbmodem*
```

### 사용 시나리오별 연결 가이드

**📱 Android 스마트폰 연결**:
- **포트**: CH343P USB-to-UART (포트 1)
- **케이블**: USB-A to USB-C
- **모드**: USB Host (Android) → Device (ESP32-S3)

**🖥️ Windows PC HID 통신**:
- **포트**: ESP32-S3 Native USB OTG (포트 2)
- **케이블**: USB-A to Micro-USB
- **결과**: 장치 관리자에서 "HID 호환 마우스" + "HID 키보드 장치" 표시

**💻 펌웨어 개발 (빌드/플래시)**:
- **포트**: CH343P USB-to-UART (포트 1)
- **케이블**: USB-A to USB-C
- **명령어**: `idf.py -p <포트명> flash`
- **주의**: 플래시 전 Android 케이블 분리 필요

**🔧 개발 시 동시 연결 (권장)**:
```
Android 스마트폰 ← USB-C → 포트 1️⃣ (UART 통신)
PC ← Micro-USB → 포트 2️⃣ (디버그 로그 + HID)

결과:
- Android에서 터치 입력 전송 (UART0 경유)
- PC에서 디버그 로그 확인 (CDC 시리얼)
- PC에서 마우스/키보드 동작 (HID)
```

### 포트 결정 방법

```bash
# 사용 가능한 포트 확인
idf.py list-ports

# 포트별 식별 방법
# CH343P (포트 1): "CH343" 또는 "CP210x" 표시
# Native USB (포트 2): 일반 USB 장치로 표시
```

## 하드웨어 연결 방식 약어 정의

개발 시 다양한 연결 시나리오를 빠르게 지칭하기 위해 다음의 표준 약어를 사용합니다:

### 연결 방식 분류

| 약어 | 정식 명칭 | 구성 | 주요 용도 |
|------|---------|------|---------|
| **풀 연결** | Full Connection | Android ↔ 포트 1️⃣(COM) + PC ↔ 포트 2️⃣(USB-OTG) | 완전한 통합 테스트, 실제 운영 환경 |
| **1차 연결** | Primary Connection | Android ↔ 포트 1️⃣(COM) | Android 앱 개발, USB 통신 테스트 |
| **2차 연결** | Secondary Connection | PC ↔ 포트 2️⃣(USB-OTG) | HID 동작 테스트, 디버그 로그 확인 |
| **플래시용 연결** | Flash Connection / Debug Connection | PC ↔ 포트 1️⃣(COM) | 펌웨어 플래시, 시리얼 모니터링 |

### 연결 방식별 상세 설명

#### 풀 연결 (Full Connection)
```
Android 스마트폰 ←[USB-C]→ 포트 1️⃣ CH343P (COM)
PC ←[Micro-USB]→ 포트 2️⃣ Native USB-OTG

목적:
- Android 앱 ↔ ESP32-S3 UART 통신 (완전히 동작)
- ESP32-S3 ↔ PC HID 통신 (완전히 동작)
- 디버그 로그 PC에서 확인 (CDC 시리얼)
- 실제 마우스/키보드 제어 가능

사용 시나리오:
- E2E 통합 테스트
- 실제 운영 환경에서의 성능 검증
- 전체 시스템 동작 확인
```

#### 1차 연결 (Primary Connection)
```
Android 스마트폰 ←[USB-C]→ 포트 1️⃣ CH343P (COM)

목적:
- Android 앱에서 UART 프레임 전송/수신 테스트
- USB 시리얼 연결 안정성 확인
- 터치패드 입력 → UART 전송 검증

사용 시나리오:
- Android 앱 기능 개발/테스트
- USB 통신 프로토콜 검증
- 펌웨어 플래시 없이 Android 측만 테스트
```

#### 2차 연결 (Secondary Connection)
```
PC ←[Micro-USB]→ 포트 2️⃣ Native USB-OTG

목적:
- ESP32-S3 → PC HID 신호 검증
- 디버그 로그 확인 (CDC 시리얼)
- PC에서 마우스/키보드 장치 인식 확인

사용 시나리오:
- 펌웨어 로직 검증
- HID 프로토콜 디버깅
- 하드웨어 초기화 상태 확인
```

#### 플래시용 연결 (Flash Connection)
```
PC ←[USB-C]→ 포트 1️⃣ CH343P (COM)

목적:
- `idf.py flash` 명령으로 펌웨어 다운로드
- 펌웨어 빌드 후 보드에 로드
- 시리얼 모니터로 ESP-IDF 로그 확인

사용 시나리오:
- 펌웨어 개발 후 보드에 로드
- 펌웨어 변경 테스트
- ESP-IDF 로그 디버깅

주의사항:
- 플래시 중에는 Android 연결 해제 필수
- 포트 번호 충돌 방지
```

### 연결 방식 선택 가이드

```yaml
작업 유형별 추천 연결:
  Android 앱 기능 개발:
    추천: 1차 연결
    설명: Android 측 UART 통신 완전히 검증 가능

  펌웨어 로직 개발:
    추천: 2차 연결 + 플래시용 연결
    설명: HID 테스트 + 펌웨어 로깅 동시 확인

  통합 테스트:
    추천: 풀 연결
    설명: 전체 시스템 동작 확인

  긴급 디버깅:
    추천: 풀 연결
    설명: 모든 레이어의 신호를 동시에 확인 가능
```

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

**주요 특징**:
- **Jetpack Compose**: 선언적 UI 프레임워크 사용
- **Material3**: 다크 테마만 지원
- **Pretendard 폰트**: 접근성을 위한 가독성 높은 한글 폰트
- **USB Serial**: `usbSerialForAndroid` 라이브러리 사용
- **Accompanist Permissions**: 권한 관리 라이브러리

### ESP32-S3 펌웨어 구조

```
src/board/BridgeOne/main/
├── BridgeOne.c           # 메인 엔트리 포인트 (app_main)
├── hid_handler.c/h       # USB HID 마우스/키보드 처리
├── uart_handler.c/h      # UART 통신 처리
├── usb_descriptors.c/h   # USB 디스크립터 정의
└── tusb_config.h         # TinyUSB 설정
```

**주요 특징**:
- **FreeRTOS 태스크**: `usb_task`, `hid_task`, `uart_task` 등 멀티태스킹
- **TinyUSB**: HID Keyboard + Mouse + CDC 복합 장치 구현
- **UART 통신**: 1Mbps 속도로 Android와 통신 (UART0, GPIO43/44, CH343P 브릿지)
- **복합 프레임**: 8바이트 고정 크기 프레임으로 마우스 및 키보드 입력 전송

## TinyUSB 설정 가이드 (ESP32-S3)

### ⚠️ 중요: menuconfig에 TinyUSB Stack 메뉴가 없는 것은 정상입니다

본 프로젝트는 **Native TinyUSB** (`espressif/tinyusb`)를 사용하며, 이는 **Kconfig 파일이 없는** 컴포넌트입니다.
`idf.py menuconfig`에서 TinyUSB Stack 관련 메뉴가 표시되지 않는 것은 **ESP-IDF v5.5의 정상적인 동작**입니다.

### TinyUSB 통합 방식 비교

#### 1. Native TinyUSB (본 프로젝트 사용 방식)
- **컴포넌트**: `espressif/tinyusb`
- **설정 방식**: `tusb_config.h` 헤더 파일로 매크로 기반 설정
- **Kconfig**: ❌ 없음
- **menuconfig**: ❌ TinyUSB 메뉴 없음 (정상)
- **장점**: 직접적인 제어, 유연한 커스터마이징
- **단점**: 설정 파일 직접 작성 필요

#### 2. ESP TinyUSB Wrapper (사용하지 않음)
- **컴포넌트**: `espressif/esp_tinyusb`
- **설정 방식**: menuconfig + `tinyusb_config_t` 구조체
- **Kconfig**: ✅ 있음
- **menuconfig**: ✅ TinyUSB Stack 메뉴 표시
- **장점**: 설정이 간편함
- **단점**: 커스터마이징 제한적

### 현재 프로젝트 설정 구조

```
src/board/BridgeOne/
├── main/
│   ├── idf_component.yml          # espressif/tinyusb: '*' (Native)
│   ├── tusb_config.h              # TinyUSB 설정 (매크로)
│   └── CMakeLists.txt             # 컴파일 정의 설정
├── sdkconfig.defaults             # ESP-IDF 레벨 기본값
├── sdkconfig                      # 빌드 설정 (CONFIG_TINYUSB 없음 = 정상)
└── managed_components/
    └── espressif__tinyusb/
        ├── CMakeLists.txt         # 컴파일 정의로 설정 전달
        └── (Kconfig 파일 없음)    # ← 이것이 정상!
```

### TinyUSB 설정 변경 방법

#### ❌ 절대 하지 말 것
- `idf.py menuconfig`에서 TinyUSB 설정을 찾으려고 시도
- Kconfig 파일을 생성하려고 시도
- `esp_tinyusb` wrapper로 전환 제안 (불필요한 복잡성 증가)

#### ✅ 올바른 설정 방법

**1. TinyUSB 세부 설정 변경:**
```c
// main/tusb_config.h 편집
#define CFG_TUD_HID         2      // HID 인터페이스 개수
#define CFG_TUD_CDC         1      // CDC 인터페이스 개수
#define CFG_TUD_HID_EP_BUFSIZE  64 // HID 버퍼 크기
```

**2. ESP-IDF 레벨 기본값 설정:**
```bash
# sdkconfig.defaults 편집 (참고용, TinyUSB에 직접 영향 없음)
CONFIG_TINYUSB=y
CONFIG_TINYUSB_HID_COUNT=2
```
⚠️ 주의: Native TinyUSB는 이 설정을 **무시**합니다. 실제 설정은 `tusb_config.h`에서만 유효합니다.

**3. 재빌드:**
```bash
idf.py fullclean
idf.py build
```

### 문제 해결

#### Q: menuconfig에서 TinyUSB Stack 메뉴가 사라졌어요!
**A**: 정상입니다. Native TinyUSB (`espressif/tinyusb`)는 Kconfig 파일이 없으므로 menuconfig에 메뉴가 표시되지 않습니다. 이는 ESP-IDF v5.5의 의도된 설계입니다.

#### Q: sdkconfig에 CONFIG_TINYUSB 설정이 없어요!
**A**: 정상입니다. Native TinyUSB는 Kconfig를 사용하지 않으므로 sdkconfig에 관련 설정이 없는 것이 정상입니다.

#### Q: TinyUSB 설정을 어떻게 변경하나요?
**A**: `main/tusb_config.h` 파일을 직접 편집하세요. 모든 TinyUSB 기능은 매크로로 제어됩니다.

### 참고 자료

- [ESP-IDF USB Device Stack 공식 문서](https://docs.espressif.com/projects/esp-idf/en/stable/esp32s3/api-reference/peripherals/usb_device.html)
- [ESP Component Registry - espressif/tinyusb](https://components.espressif.com/components/espressif/tinyusb)
- [ESP-IoT-Solution TinyUSB 개발 가이드](https://docs.espressif.com/projects/esp-iot-solution/en/latest/usb/usb_overview/tinyusb_development_guide.html)

## 통신 프로토콜

### UART 델타 프레임 (Android → ESP32-S3)

```c
// 8바이트 고정 크기 복합 프레임 (Little-Endian)
struct BridgeFrame {
    uint8_t  seq;       // 순번 (유실 감지용, 0~255 순환)
    uint8_t  buttons;   // 마우스 버튼: bit0: L, bit1: R, bit2: M
    int8_t   deltaX;    // 상대 X 이동 (-127~127)
    int8_t   deltaY;    // 상대 Y 이동 (-127~127)
    int8_t   wheel;     // 휠 스크롤
    uint8_t  modifiers; // 수정자 키: Ctrl, Shift, Alt, Win
    uint8_t  keyCode1;  // 주 키코드 (HID Usage)
    uint8_t  keyCode2;  // 보조 키코드 (HID Usage)
};

// 전송 주기: 4-8ms (125-250 Hz)
// 특징: 마우스와 키보드 입력을 단일 프레임으로 전송
```

### USB HID 프로토콜 (ESP32-S3 → PC)

```c
// Boot Protocol - 마우스 (BIOS/BitLocker 호환)
struct HidMouseReport {
    uint8_t buttons;   // 마우스 버튼 상태
    int8_t  x;         // X 이동 (-127~127)
    int8_t  y;         // Y 이동 (-127~127)
};

// Boot Protocol - 키보드 (BIOS/BitLocker 호환)
struct HidKeyboardReport {
    uint8_t modifiers;      // 수정자 키 (Ctrl, Shift, Alt, Win)
    uint8_t reserved;       // 예약됨 (0x00)
    uint8_t keycodes[6];    // 동시에 눌린 키 (최대 6개)
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
- **Permissions**: `com.google.accompanist:accompanist-permissions`

### ESP32-S3
- **MCU**: ESP32-S3 N16R8 (YD-ESP32-S3 호환 보드)
- **메모리**: 16MB Flash, 8MB Octal SPI PSRAM
- **프레임워크**: ESP-IDF v5.5+
- **USB**: TinyUSB 스택
- **RTOS**: FreeRTOS
- **UART 통신**: UART1 (GPIO17/18)

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

**💎 핵심 원칙: Claude는 절대 직접 Git 커밋을 수행하지 않습니다.**

유저가 커밋 관련 작업을 제안하거나 요청하는 경우, Claude는:
- 🚫 **절대 직접 수행 금지**: `git add`, `git commit`, `git push` 등의 커밋 작업을 절대 직접 수행하지 않습니다
- 📋 **변경사항 설명**: 커밋할 변경사항을 명확히 설명하고, 권장 커밋 메시지를 제안합니다
- 🎯 **단계별 가이드**: 유저가 스스로 커밋을 수행할 수 있도록 단계별 절차를 안내합니다
- 💬 **조언 제공**: 필요한 경우, 커밋 메시지 작성이나 변경사항 검토에 대한 기술적 조언을 제공합니다

이 정책의 의의:
- **소유권 보장**: 유저가 모든 Git 작업의 완전한 제어와 책임을 유지합니다
- **투명성 확보**: 모든 커밋이 명시적으로 유저의 의도하에 수행됩니다
- **안전성 증진**: 의도하지 않은 변경사항이나 잘못된 커밋이 저장소에 등록되는 것을 방지합니다
- **감사 추적 명확화**: 모든 커밋 기록이 유저의 의식적인 선택을 반영하므로 추적과 검토가 명확합니다

## 주의사항

### Windows USB 성능 최적화
Windows 11에서 ESP32-S3 USB CDC 통신 시 다음 레지스트리 설정이 필요할 수 있습니다:

**경로** (시리얼 번호는 각 기기마다 다름):
```
HKEY_LOCAL_MACHINE\SYSTEM\CurrentControlSet\Enum\USB\VID_303A&PID_4001\[시리얼번호]\Device Parameters
```

**설정 항목**:
```
이름: IdleUsbSelectiveSuspendPolicy
타입: DWORD (32-bit)
값: 0x00000001
```

**적용 방법**:
1. `레지스트리 편집기` 열기 (regedit.exe)
2. 위 경로로 이동 (시리얼 번호는 각 USB 장치마다 다름)
3. `Device Parameters` 폴더 우클릭 → 새로 만들기 → DWORD (32-bit) 값
4. 이름: `IdleUsbSelectiveSuspendPolicy`
5. 값: `0x00000001` 입력
6. 시스템 재부팅

### 개발 환경
- **Android 개발**: Android Studio (최신 버전 권장)
- **ESP32-S3 개발**: ESP-IDF v5.5+ 설치 필수
- **Python**: 3.8 이상 (ESP-IDF 요구사항)

### 시리얼 모니터 도구

**⚠️ 중요: `idf.py monitor` 대신 Tera Term을 사용합니다**

`idf.py monitor`는 Windows 환경에서 출력 누락이 발생할 수 있습니다. 안정적인 시리얼 모니터링을 위해 **Tera Term**을 기본 도구로 사용합니다.

**Tera Term 설정:**
```
다운로드: https://ttssh2.osdn.jp/index.html.en

Setup → Serial port...
├─ Port: COMx (ESP32-S3 포트)
├─ Baud rate: 115200
├─ Data: 8 bit
├─ Parity: none
├─ Stop: 1 bit
└─ Flow control: none
```

**용도별 도구 선택:**
| 용도 | 권장 도구 | 비고 |
|------|----------|------|
| 일반 디버그 로그 | **Tera Term** | 안정적, 로그 파일 저장 |
| HEX/바이너리 분석 | RealTerm | UART 프레임 디버깅 |
| 다중 포트 모니터링 | CoolTerm | Android + PC 동시 확인 |
| 플래시 직후 확인 | `idf.py monitor` | ESP-IDF 통합 필요시만 |

**Claude 지침:**
- 시리얼 모니터링 관련 안내 시 **Tera Term 사용을 우선 제안**
- `idf.py monitor` 사용은 플래시 직후 빠른 확인 용도로만 권장
- 로그 분석이 필요한 경우 Tera Term의 로그 파일 저장 기능 활용 안내

### 테스트
- Android 앱 테스트 시 USB-OTG 지원 실제 디바이스 필요
- ESP32-S3 펌웨어 테스트 시 YD-ESP32-S3 N16R8 보드 필요
- 전체 시스템 통합 테스트 시 Android 디바이스, YD-ESP32-S3 보드, PC 모두 필요

### 보드별 주의사항
- **YD-ESP32-S3 N16R8**: UART1 (GPIO17/18) 사용, CH343 드라이버 필요
  - 일부 보드에서 5V 핀 전압 이슈 보고됨 (사용 전 멀티미터로 확인 권장)
  - 상세 정보: `docs/board/YD-ESP32-S3-N16R8-analysis.md` 참조

## Claude의 PowerShell 환경 가이드라인

**⚠️ 중요: 이 프로젝트는 Windows PowerShell 환경에서 개발됩니다**

### Unix/Linux 명령어 사용 금지
- **절대 제안하지 않음**: `grep`, `sed`, `awk`, `cat`, `ls`, `find`, `xargs`, `cut`, `sort` 등 Unix/Linux 명령어 제안 금지
- **절대 실행하지 않음**: PowerShell 터미널에서 이러한 명령어를 실행하지 않습니다
- **이유**: Windows PowerShell에서 Unix 명령어 호환성이 불완전하거나 기본으로 제공되지 않을 수 있습니다

### PowerShell 대안 제시
File/Content 검색이나 처리가 필요한 경우:
- **파일 찾기**: `Glob` 도구 사용 (Unix `find` 대신)
- **내용 검색**: `Grep` 도구 사용 (Unix `grep` 대신)
- **파일 읽기**: `Read` 도구 사용 (Unix `cat` 대신)
- **파일 편집**: `Edit` 도구 사용 (Unix `sed` 대신)
- **파일 작성**: `Write` 도구 사용 (Unix `echo` 또는 리다이렉션 대신)

### PowerShell 명령어 선택기
| 작업 | Unix 명령어 | PowerShell 대안 | Claude 도구 |
|------|-----------|----------------|-----------|
| 파일 검색 | `find file.txt` | `Get-Item`, `Get-ChildItem` | `Glob` (권장) |
| 내용 검색 | `grep "pattern"` | `Select-String` | `Grep` (권장) |
| 파일 나열 | `ls`, `find` | `Get-ChildItem` (ls 별칭) | `Glob` (권장) |
| 파일 읽기 | `cat file.txt` | `Get-Content` | `Read` (권장) |
| 텍스트 처리 | `sed`, `awk` | `ForEach-Object`, `-replace` | `Edit` (권장) |
| 파일 작성 | `echo > file.txt` | `Set-Content` | `Write` (권장) |
| 파일 삭제 | `rm`, `del` | `Remove-Item` | (직접 사용 가능) |
| 디렉토리 생성 | `mkdir` | `New-Item -Type Directory` | (직접 사용 가능) |

### 사용 예시

❌ **절대 이렇게 하지 않음**:
```powershell
# grep으로 패턴 검색
grep -r "TODO" src/

# sed로 파일 수정
sed -i 's/old/new/g' file.txt

# find로 파일 검색
find . -name "*.kt" -type f
```

✅ **대신 이렇게 함**:
```powershell
# Grep 도구로 패턴 검색
# → Claude가 Grep 도구 사용 또는 Select-String 제안

# Edit 도구로 파일 수정
# → Claude가 Edit 도구 사용

# Glob 도구로 파일 검색
# → Claude가 Glob 도구 사용 또는 Get-ChildItem 제안
```

### PowerShell 명령어 사용 가능
실제 시스템 작업이 필요한 경우 PowerShell 명령어는 사용 가능합니다:
```powershell
# 빌드 및 실행 관련
./gradlew build
idf.py build
npm install

# 버전 확인
java -version
python --version

# 경로/환경 확인
echo $env:JAVA_HOME
Get-ChildItem env:
```

### 핵심 규칙
- **도구 우선**: 파일/내용 작업 → Claude 도구 사용 (Glob, Grep, Read, Edit, Write)
- **PowerShell 사용**: 실제 시스템 명령 (git, gradle, build 등)
- **Unix 금지**: grep, sed, awk, find, cat 등 절대 제안/실행 금지
