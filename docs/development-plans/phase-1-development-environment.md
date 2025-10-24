---
title: "BridgeOne Phase 1: 개발환경 설정"
description: "BridgeOne 프로젝트 Phase 1 - Android, ESP32-S3, Windows 서버 개발환경 구축 및 초기 설정 (1-2주)"
tags: ["android", "esp32-s3", "devkitc-1", "windows", "development", "environment", "setup", "vibe-coding"]
version: "v1.0"
owner: "Chatterbones"
updated: "2025-10-20"
---

# BridgeOne Phase 1: 개발환경 설정

**개발 기간**: 1-2주

**목표**: 3개 플랫폼(Android, Board, Windows) 개발환경 구축 및 초기 설정

## Phase 구조 설계 원칙

### 검증 전략

- **기본 원칙**: 각 하위 Phase 개발 완료 즉시 해당 내용을 바로 검증
- **예외**: 여러 하위 Phase를 모아서 통합 검증하는 게 더 효율적인 경우에만 별도 통합 검증 Phase 추가
  - 예: Phase 2.5 - 전체 통신 경로 End-to-End 지연시간 검증
  - 예: Phase 4.5 - PC 화면에서 실제 커서 이동 정확성 통합 테스트
- 최종 Phase에서 전체 시스템 E2E 테스트 수행

### Phase 명명 규칙

- **Phase X.Y**: 구현 및 검증 하위 Phase (구현 완료 즉시 검증)
- **Phase X.통합검증**: 통합 검증이 필요한 경우에만 추가 (예외적)

### 바이브 코딩 활용 방침

- 각 하위 Phase별 바이브 코딩 프롬프트는 별도 섹션에서 제공 예정
- 본 문서는 전체 개발 로드맵의 큰 틀을 제시

---

## Phase 1.1: Android 개발환경 구축

#### Phase 1.1.1: Android Studio 설치 및 프로젝트 생성 검증

**목표**: 유저가 수동으로 생성한 Android Studio 프로젝트의 설정 상태를 검증

**유저 사전 작업** (LLM 실행 전 필수):
1. Android Studio Arctic Fox 이상 버전을 수동 설치
2. Android Studio에서 새 프로젝트 생성:
   - 템플릿: "Empty Compose Activity" 선택
   - 프로젝트명: "BridgeOne", 패키지명: "com.bridgeone.app"
   - Save location: `{workspace}/src/android/`
   - Language: Kotlin, Minimum SDK: API 24 (Android 7.0)
   - Build configuration language: Kotlin DSL (build.gradle.kts)
3. 프로젝트 생성 완료 후 Gradle 동기화 대기

**LLM 검증 작업**:
1. `src/android/` 디렉터리 존재 여부 확인
2. `app/build.gradle.kts` 파일 읽기 및 필수 설정 검증 (Compose 의존성, compileSdk, minSdk, targetSdk)
3. `app/src/main/AndroidManifest.xml` 존재 확인
4. `app/src/main/java/com/bridgeone/app/MainActivity.kt` 존재 확인

**참조 문서 및 섹션**:
- `docs/android/technical-specification-app.md` §2.1 스플래시 스크린 구현 요구사항
- `docs/technical-specification.md` §4.1 Android 플랫폼 개요

**검증**:
- [x] `src/android/` 디렉터리가 존재함
- [x] `src/android/app/build.gradle.kts` 파일이 존재하고 Compose BOM 의존성 포함됨
- [x] `minSdk = 24`, `targetSdk = 34` 설정 확인
- [x] `MainActivity.kt` 파일이 `com.bridgeone.app` 패키지에 존재함
- [x] `AndroidManifest.xml`에 MainActivity가 등록되어 있음
- [x] 프로젝트 구조가 표준 Android 프로젝트 구조(`app/src/main/java/`)를 따름

---

#### Phase 1.1.2: Jetpack Compose 및 핵심 의존성 설정

**목표**: Compose, USB Serial, 기타 필수 라이브러리 의존성 추가 및 버전 관리 설정

**세부 목표**:
1. `gradle/libs.versions.toml`에 버전 카탈로그 작성 (Compose BOM, Kotlin 1.9.0+, USB Serial 3.7.3, Accompanist Permissions 0.32.0)
2. `app/build.gradle.kts`에 의존성 추가 (Compose UI, Material3, Activity Compose, USB Serial, Accompanist)
3. Compose 컴파일러 옵션 설정
4. Java 17 타겟 설정

**참조 문서 및 섹션**:
- `docs/android/technical-specification-app.md` §1.1 통신 아키텍처 설계
- `docs/android/technical-specification-app.md` §2.1.1 Jetpack Compose 기반 설계

**검증**:
- [x] Gradle 동기화 성공 (모든 의존성 다운로드 완료)
- [x] `libs.versions.toml`에 모든 버전이 명확히 정의됨
- [x] USB Serial 라이브러리가 External Libraries에 표시됨
- [x] Compose Preview 기능 정상 동작 확인
- [x] 빌드 시 Java 버전 관련 오류 없음

---

#### Phase 1.1.3: AndroidManifest.xml 권한 및 기본 설정

**목표**: USB 통신 및 앱 실행에 필요한 모든 권한과 기본 설정 구성

**세부 목표**:
1. `AndroidManifest.xml`에 권한 추가 (USB_PERMISSION, usb.host, VIBRATE)
2. MainActivity에 `android:screenOrientation="portrait"` 설정
3. 앱 아이콘 및 이름 설정 (`android:label="BridgeOne"`)
4. Theme 설정 (`Theme.BridgeOne` - Material3 기반)
5. USB 장치 연결 Intent Filter 추가 (VID: 0x303A)

**참조 문서 및 섹션**:
- `docs/android/technical-specification-app.md` §1.1.1 하드웨어 연결 요구사항
- `docs/android/technical-specification-app.md` §1.1.2 연결 관리 요구사항

**검증**:
- [x] Manifest Merger 오류 없음
- [x] 앱 설치 시 권한 요청 대화상자 표시 확인
- [x] 앱 아이콘이 런처에 "BridgeOne"으로 표시됨
- [x] USB 장치 연결 시 앱 선택 대화상자에 BridgeOne 표시됨
- [x] Portrait 모드 강제 적용 확인

---

#### Phase 1.1.4: 커스텀 리소스 준비 (Drawable 및 Font)

**목표**: BridgeOne 앱에서 사용할 커스텀 아이콘(drawable)과 폰트(font) 리소스를 프로젝트에 통합

**유저 사전 작업** (LLM 실행 전 필수):
1. 파일 탐색기 또는 IDE의 파일 탐색기에서 프로젝트 루트의 `resources/drawable/` 디렉터리 열기
2. `resources/drawable/` 내의 **모든 XML 벡터 아이콘 파일 복사**:
   - 33개 파일 모두 선택 (Ctrl+A)
   - 복사 (Ctrl+C)
   - `src/android/app/src/main/res/drawable/` 디렉터리로 이동 (디렉터리가 없으면 생성)
   - 붙여넣기 (Ctrl+V)
3. 프로젝트 루트의 `resources/font/` 디렉터리 열기
4. `resources/font/` 내의 **모든 Pretendard TTF 폰트 파일 복사**:
   - 9개 파일 모두 선택 (Ctrl+A)
   - 복사 (Ctrl+C)
   - `src/android/app/src/main/res/font/` 디렉터리로 이동 (디렉터리가 없으면 생성)
   - 붙여넣기 (Ctrl+V)
5. Android Studio에서 `src/android/` 프로젝트 새로고침 (F5 또는 우클릭 → Refresh)

**LLM 검증 작업**:
1. `src/android/app/src/main/res/drawable/` 디렉터리에 33개 아이콘 파일 존재 확인
2. `src/android/app/src/main/res/font/` 디렉터리에 9개 폰트 파일 존재 확인
3. `app/src/main/res/font/pretendard.xml` Font Family XML 파일 생성 (모든 weight 포함)
4. 각 벡터 drawable의 `android:fillColor` 속성 확인 (대부분 `#FF000000` 검은색, 런타임에 tint 적용 가능)
5. 리소스 ID 접근 가능 확인 (`R.drawable.ic_*`, `R.font.pretendard_*`)

**참조 문서 및 섹션**:
- `docs/android/design-guide-app.md` §3.2 타이포그래피 (Pretendard 폰트 사용)
- `docs/android/component-design-guide-app.md` (각 컴포넌트의 아이콘 사용)
- `resources/drawable/` 디렉터리 (소스 아이콘 파일)
- `resources/font/` 디렉터리 (소스 폰트 파일)

**검증**:
- [ ] `app/src/main/res/drawable/` 디렉터리에 33개 아이콘 파일 모두 존재
- [ ] `app/src/main/res/font/` 디렉터리에 9개 Pretendard 폰트 파일 모두 존재
- [ ] `app/src/main/res/font/pretendard.xml` Font Family 파일 생성됨
- [ ] Android Studio에서 drawable 리소스 미리보기 정상 표시 (Vector Asset Studio)
- [ ] Kotlin 코드에서 `R.drawable.ic_touchpad` 등 리소스 ID 정상 인식됨
- [ ] Kotlin 코드에서 `R.font.pretendard` 또는 `R.font.pretendard_medium` 등 폰트 리소스 정상 인식됨
- [ ] 빌드 시 리소스 관련 오류 없음
- [ ] Compose Preview에서 `painterResource(R.drawable.ic_touchpad)` 정상 렌더링됨
- [ ] Compose에서 `FontFamily(Font(R.font.pretendard_medium))` 정상 적용됨

---

#### Phase 1.1.5: 기본 Compose UI 구조 및 Hello World

**목표**: Compose 기반 기본 UI 구조 생성, 커스텀 폰트 적용 및 정상 렌더링 확인

**세부 목표**:
1. `MainActivity.kt`에서 `setContent { BridgeOneApp() }` 호출
2. `ui/BridgeOneApp.kt` 생성 - 최상위 Composable 함수
3. `ui/theme/` 디렉터리에 Color.kt, Type.kt, Theme.kt 생성
4. `ui/theme/Type.kt`에 Pretendard 폰트 기반 Typography 정의:
   - `FontFamily` 생성: `val PretendardFontFamily = FontFamily(Font(R.font.pretendard_medium))`
   - Material3 Typography에 Pretendard 폰트 적용
5. Material3 테마 적용 (`BridgeOneTheme`)
6. 중앙에 "BridgeOne - Hello World" 텍스트 표시 (Pretendard 폰트 사용)
7. 다크 테마 기본 적용 (배경색 `#121212`)
8. 테스트용 아이콘 표시 (예: `ic_touchpad`, `ic_usb` 등)

**참조 문서 및 섹션**:
- `docs/android/design-guide-app.md` §3 색상 시스템
- `docs/android/design-guide-app.md` §3.2 타이포그래피 (Pretendard 폰트)
- `docs/android/technical-specification-app.md` §2.1.1 Jetpack Compose 기반 설계

**검증**:
- [ ] 에뮬레이터/실제 기기에서 "BridgeOne - Hello World" 텍스트가 화면 중앙에 표시됨
- [ ] 텍스트가 Pretendard 폰트로 정확히 렌더링됨 (시스템 폰트와 구분됨)
- [ ] 다크 테마가 적용되어 배경이 검은색(`#121212`)임
- [ ] 테스트 아이콘(drawable)이 정상 표시됨
- [ ] Compose Preview에서 UI가 정확히 미리보기됨 (폰트 및 아이콘 포함)
- [ ] 시스템 테마 변경 시 앱 테마도 자동 전환됨
- [ ] 빌드 시 Compose 및 리소스 관련 경고나 오류 없음

---

## Phase 1.2: Board (ESP32-S3) 개발환경 구축

#### Phase 1.2.1: ESP-IDF 설치 검증 및 프로젝트 생성 준비

**목표**: 유저가 수동으로 설치한 ESP-IDF 개발환경의 정상 설치 여부를 검증

**유저 사전 작업** (LLM 실행 전 필수):
1. ESP-IDF Windows Installer 다운로드 및 설치 (v5.5 이상 권장)
   - 다운로드: https://github.com/espressif/esp-idf/releases
   - 최신 안정 버전 권장 (v5.5 이상, TinyUSB Composite 안정성)
2. 설치 경로: `C:\Espressif\esp-idf` (기본값 권장, 다른 경로 선택 시 경로 메모)
3. ESP-IDF Tools 자동 설치 선택 (CMake, Ninja, Python 등 체크박스 선택)
4. 설치 완료 후 PowerShell 재시작
5. PowerShell에서 `C:\Espressif\esp-idf\export.ps1` 실행하여 환경변수 로드
   ```powershell
   # PowerShell 실행 정책 임시 변경 (필요시)
   Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope Process

   # ESP-IDF 환경 활성화
   C:\Espressif\esp-idf\export.ps1
   ```

**LLM 검증 작업**:
1. PowerShell에서 `idf.py --version` 명령 실행 (버전 확인)
2. `$env:IDF_PATH` 환경변수 확인 (설치 경로 확인)
3. 설치 경로 물리적 존재 확인: `Test-Path "C:\Espressif\esp-idf"`
4. CMake, Ninja, Python 버전 확인
5. ESP-IDF 필수 도구 설치 상태 점검 (esptool.py 등)
6. TinyUSB 컴포넌트 availability 확인

**참조 문서 및 섹션**:
- `docs/board/esp32s3-code-implementation-guide.md` §7.1 ESP-IDF 설치 및 환경 설정

**검증**:
- [ ] `idf.py --version` 실행 시 5.1.0 이상 버전 출력
  - 예상 결과: `ESP-IDF v5.5` 또는 유사
- [ ] `$env:IDF_PATH` 환경변수 출력 결과가 설치 경로 (예: `C:\Espressif\esp-idf`)
- [ ] ESP-IDF 설치 경로 존재 확인: `C:\Espressif\esp-idf\export.ps1` 파일 존재
- [ ] `cmake --version` 3.16 이상, `ninja --version`, `python --version` 3.8 이상 확인
- [ ] `esptool.py --version` 실행 성공 (ESP-IDF 도구 체인 완성)
- [ ] 모든 검증 통과 시 "ESP-IDF 개발환경이 정상적으로 설치되었습니다" 메시지 출력

---

#### Phase 1.2.2: ESP32-S3 프로젝트 생성 및 타겟 설정

**목표**: BridgeOne Board 펌웨어 프로젝트 생성 및 ESP32-S3 타겟 지정

**세부 목표**:
1. `src/board/` 디렉터리 내에서 프로젝트 생성 (기존 `BridgeOne/` 디렉터리 있으면 백업)
2. `idf.py create-project bridgeone_board` 실행 (프로젝트명은 소문자, 언더스코어 조합)
3. `idf.py set-target esp32s3` 실행 (ESP32-S3 타겟 지정)
4. CMakeLists.txt 확인 (프로젝트명 "bridgeone_board")
5. 프로젝트 디렉터리 구조 검증

**실행 단계**:
```powershell
# 1. 프로젝트 디렉터리 탐색 및 기존 백업 (있을 경우)
cd "F:\C\Programming\MobileDevelopment\Projects\Android\BridgeOne\src\board"
if (Test-Path "BridgeOne") {
    Rename-Item -Path "BridgeOne" -NewName "BridgeOne.backup.$(Get-Date -Format 'yyyyMMdd_HHmmss')"
    Write-Host "기존 BridgeOne 디렉터리를 백업했습니다."
}

# 2. 프로젝트 생성 (자동으로 BridgeOne 디렉터리 생성)
idf.py create-project bridgeone_board

# 3. 프로젝트 디렉터리 진입
cd "bridgeone_board"

# 4. ESP32-S3 타겟 설정
idf.py set-target esp32s3
```

**참조 문서 및 섹션**:
- `docs/board/esp32s3-code-implementation-guide.md` §3.1 ESP-IDF 프로젝트 구조

**검증**:
- [ ] `idf.py set-target esp32s3` 실행 성공 (오류 없음)
- [ ] `sdkconfig` 파일 생성 및 ESP32-S3 타겟 설정 확인
  - 파일 위치: `{프로젝트}/sdkconfig`
- [ ] `CMakeLists.txt`에 `project(bridgeone_board)` 존재
- [ ] `main/CMakeLists.txt`에 main.c 등록 확인
- [ ] 프로젝트 디렉터리 구조 정확히 생성됨:
  ```
  bridgeone_board/
  ├── CMakeLists.txt
  ├── sdkconfig
  ├── main/
  │   ├── CMakeLists.txt
  │   └── main.c
  └── build/ (이 단계에서는 생성되지 않음, 다음 Phase에서 생성)
  ```
- [ ] 프로젝트 루트 경로: `src/board/bridgeone_board/`

---

#### Phase 1.2.3: sdkconfig 기본 설정 및 TinyUSB 활성화

**목표**: ESP-IDF 설정 파일(sdkconfig)에 TinyUSB 및 필수 컴포넌트 활성화

**세부 목표**:
1. `sdkconfig.defaults` 파일 생성 및 설정 추가 (TinyUSB, HID, CDC, UART ISR, FreeRTOS)
2. `idf.py reconfigure` 실행하여 설정 적용
3. 선택적으로 `idf.py menuconfig`로 UI를 통해 추가 커스터마이징 가능

**설정 파일 생성**:
프로젝트 루트(`src/board/bridgeone_board/`)에 `sdkconfig.defaults` 파일 생성:

```ini
# TinyUSB Composite 디바이스 설정
CONFIG_TINYUSB=y
CONFIG_TINYUSB_HID_ENABLED=y
CONFIG_TINYUSB_CDC_ENABLED=y
CONFIG_TINYUSB_DEVICE_ENABLED=y

# USB 버스 전원 모드 선택
# (참고: ESP32-S3 DevKit은 USB 버스에서 전원 공급받으므로 BUS_POWERED 권장)
CONFIG_TINYUSB_DEVICE_BUS_POWERED=y

# HID 버퍼 설정 (BridgeOne 8바이트 프레임 처리)
CONFIG_TINYUSB_HID_COUNT=2
CONFIG_TINYUSB_HID_BUFSIZE=64

# CDC 버퍼 설정 (Vendor CDC 메시지 프레임 처리)
CONFIG_TINYUSB_CDC_COUNT=1
CONFIG_TINYUSB_CDC_RX_BUFSIZE=512
CONFIG_TINYUSB_CDC_TX_BUFSIZE=512

# UART 최적화 (빠른 응답성)
CONFIG_UART_ISR_IN_IRAM=y

# FreeRTOS 설정 (1ms Tick rate → 1000Hz, 듀얼 코어 활용)
CONFIG_FREERTOS_HZ=1000
CONFIG_FREERTOS_UNICORE=n

# 메모리 최적화 (크기 우선, PSRAM 활용)
CONFIG_COMPILER_OPTIMIZATION_SIZE=y
CONFIG_SPIRAM_MODE_OCT=y

# 플래시 설정 (ESP32-S3-DevkitC-1-N16R8: 16MB Flash)
CONFIG_ESPTOOLPY_FLASHSIZE_8MB=y
CONFIG_PARTITION_TABLE_CUSTOM=y
CONFIG_PARTITION_TABLE_CUSTOM_FILENAME="partitions.csv"

# 로깅 레벨 설정 (개발: DEBUG, 프로덕션: INFO)
CONFIG_LOG_DEFAULT_LEVEL_INFO=y

# 주변기기 비활성화 (불필요한 전력 소비 방지)
CONFIG_ESP_WIFI_ENABLED=n
CONFIG_BT_ENABLED=n
```

**실행 단계**:
```powershell
# 1. 프로젝트 디렉터리 내 sdkconfig.defaults 생성 (위 내용 붙여넣기)
# (편집기로 파일 생성 또는 PowerShell에서)
$sdkconfigContent = @"
CONFIG_TINYUSB=y
# ... 위 설정값 ...
"@
Set-Content -Path "sdkconfig.defaults" -Value $sdkconfigContent

# 2. 설정 적용
idf.py reconfigure

# (또는 UI 기반 수동 설정)
# idf.py menuconfig
```

**참조 문서 및 섹션**:
- `docs/board/esp32s3-code-implementation-guide.md` §7.2 프로젝트 설정 (sdkconfig)
- `docs/board/esp32s3-code-implementation-guide.md` §0 USB Composite 디바이스 설계 계약 (프로토콜 명세)

**검증**:
- [ ] `sdkconfig` 파일에 `CONFIG_TINYUSB=y` 존재
- [ ] `sdkconfig`에 `CONFIG_TINYUSB_HID_ENABLED=y`, `CONFIG_TINYUSB_CDC_ENABLED=y` 존재
- [ ] `CONFIG_UART_ISR_IN_IRAM=y`, `CONFIG_FREERTOS_HZ=1000` 확인
- [ ] `CONFIG_TINYUSB_HID_COUNT=2` (Keyboard + Mouse)
- [ ] `CONFIG_TINYUSB_CDC_COUNT=1` (Vendor CDC)
- [ ] `sdkconfig.defaults`에 모든 설정 백업됨
- [ ] `idf.py reconfigure` 실행 성공 (오류 없음)

---

#### Phase 1.2.4: Hello World 빌드 및 ESP32-S3 플래싱

**목표**: 기본 Hello World 펌웨어 빌드 및 ESP32-S3-DevkitC-1에 플래싱

**세부 목표**:
1. `main/main.c`에 Hello World 코드 작성 (ESP_LOGI 사용)
2. `idf.py build` 실행하여 펌웨어 컴파일
3. USB 연결 후 `idf.py -p COMx flash` 실행 (동적 포트 감지)
4. `idf.py -p COMx monitor` 실행하여 시리얼 로그 확인

**main.c 코드 예시**:
```c
#include <stdio.h>
#include "esp_log.h"
#include "esp_system.h"

static const char* TAG = "BridgeOne";

void app_main(void) {
    ESP_LOGI(TAG, "====================================");
    ESP_LOGI(TAG, "BridgeOne Board - Hello World!");
    ESP_LOGI(TAG, "ESP-IDF Version: %s", esp_get_idf_version());
    ESP_LOGI(TAG, "Free heap: %ld bytes", esp_get_free_heap_size());
    ESP_LOGI(TAG, "====================================");

    // 무한 루프 (향후 UART/USB 핸들러 추가)
    while (1) {
        vTaskDelay(1000 / portTICK_PERIOD_MS);
    }
}
```

**실행 단계**:
```powershell
# 1. main.c 파일 편집 (위 코드 사용)

# 2. 프로젝트 빌드
idf.py build

# 3. 빌드 결과 확인
# 기대 결과: "build/bridgeone_board.bin" 생성

# 4. ESP32-S3 DevKit USB 연결 (Micro-USB-C)
# (이 단계에서 일시 중지, 사용자가 보드 연결 대기)

# 5. 연결된 COM 포트 자동 감지 및 플래싱
$comPort = (Get-WmiObject Win32_SerialPort | Where-Object {$_.Description -match "USB"} | Select-Object -First 1).DeviceID
if ($comPort) {
    Write-Host "감지된 COM 포트: $comPort"
    idf.py -p $comPort flash
} else {
    Write-Host "USB 포트를 자동 감지하지 못했습니다. Device Manager에서 확인하세요."
}

# (또는 수동으로 COM 포트 지정)
# idf.py -p COM3 flash  # (실제 포트번호로 변경)

# 6. 플래시 완료 후 시리얼 모니터 시작
idf.py -p $comPort monitor
# (또는 수동: idf.py -p COM3 monitor)
```

**참조 문서 및 섹션**:
- `docs/board/esp32s3-code-implementation-guide.md` §7.3 빌드, 플래시, 모니터링

**검증**:
- [ ] `idf.py build` 실행 성공 (오류 없음)
- [ ] `build/` 디렉터리에 `bridgeone_board.bin` 생성됨
- [ ] 플래싱 성공 ("Hash of data verified" 메시지 표시)
- [ ] 시리얼 모니터에 "BridgeOne Board - Hello World!" 로그 출력
- [ ] ESP32-S3 정상 동작 확인 (보드의 LED 점멸 또는 로그 출력 지속)

---

#### Phase 1.2.4.1: USB 복합 디바이스 인식 확인

**목표**: USB Composite 디바이스가 Windows에서 정상 인식되는지 확인

**세부 목표**:
1. Device Manager에서 ESP32-S3 Composite 디바이스 확인
2. 3개의 인터페이스(Keyboard, Mouse, CDC) 표시 확인
3. VID/PID 정보 기록 (향후 디버깅용)

**실행 단계**:
```powershell
# 1. Device Manager 열기
devmgmt.msc

# (또는 PowerShell로 직접 조회)
Get-WmiObject Win32_PnPDevice | Where-Object {$_.Description -match "HID|COM"} | Select-Object Description, DeviceID
```

**Device Manager 확인 항목**:
1. **Human Interface Devices** 섹션:
   - ✅ "BridgeOne USB Keyboard" 또는 "USB Input Device"
   - ✅ "BridgeOne USB Mouse" 또는 "USB Input Device"

2. **Ports** 섹션:
   - ✅ "USB-to-UART Bridge (COMx)" 또는 유사 항목

3. **오류 확인**:
   - ❌ 노란색 느낌표(!) 또는 "Unknown Device" 없음
   - ❌ 드라이버 오류 없음

**참조 문서 및 섹션**:
- `docs/board/esp32s3-code-implementation-guide.md` §0.1 USB Composite 디바이스 구성

**검증**:
- [ ] Device Manager에서 "BridgeOne" 또는 "USB Composite" 디바이스 표시
- [ ] 최소 2개 이상의 HID 인터페이스 표시 (Keyboard, Mouse)
- [ ] 1개의 CDC/COM 포트 표시
- [ ] 모든 인터페이스에 드라이버 오류 없음
- [ ] VID/PID 기록:
  - Keyboard: `VID_XXXX&PID_XXXX`
  - Mouse: `VID_XXXX&PID_XXXX`
  - COM Port: `VID_XXXX&PID_XXXX`

---

## Phase 1.3: Windows 서버 개발환경 구축

#### Phase 1.3.1: .NET 8 SDK 및 Visual Studio 설치 검증

**목표**: 유저가 수동으로 설치한 .NET 8 SDK 및 Visual Studio 2022의 정상 설치 여부를 검증

**유저 사전 작업** (LLM 실행 전 필수):
1. .NET 8 SDK (LTS) 다운로드 및 설치
2. Visual Studio 2022 설치 (Workload: ".NET desktop development", 컴포넌트: "WPF", ".NET 8.0 Runtime")
3. 시스템 재시작 (권장)

**LLM 검증 작업**:
1. PowerShell에서 `dotnet --version` 실행
2. `dotnet --list-sdks` 명령으로 SDK 목록 확인
3. Visual Studio 설치 경로 확인
4. WPF 템플릿 사용 가능 확인

**참조 문서 및 섹션**:
- `docs/windows/technical-specification-server.md` §2.2 기술 스택 선택 원칙

**검증**:
- [ ] `dotnet --version` 실행 시 8.0.x 출력
- [ ] `dotnet --list-sdks`에 .NET 8.0.x 표시
- [ ] Visual Studio 2022 설치 경로 존재
- [ ] `dotnet new wpf --help` 실행 성공
- [ ] 모든 검증 통과 시 ".NET 8 개발환경이 정상적으로 설치되었습니다" 메시지 출력

---

#### Phase 1.3.2: WPF 프로젝트 생성 및 기본 구조 검증

**목표**: 유저가 수동으로 생성한 WPF 프로젝트의 구조와 설정을 검증

**유저 사전 작업** (LLM 실행 전 필수):
1. Visual Studio 2022에서 "새 프로젝트 만들기"
2. 템플릿: "WPF 앱(.NET)" 선택
3. 프로젝트명: "BridgeOne.Server", 위치: `{workspace}/src/windows/BridgeOne/`, Framework: ".NET 8.0"

**LLM 검증 작업**:
1. `src/windows/BridgeOne/` 디렉터리 존재 확인
2. `BridgeOne.Server.csproj` 파일 읽기 (`<TargetFramework>net8.0-windows</TargetFramework>` 확인)
3. XAML 파일 존재 확인

**참조 문서 및 섹션**:
- `docs/windows/technical-specification-server.md` §10.1 라이브러리 선정 배경

**검증**:
- [ ] `BridgeOne.Server.csproj` 파일 존재
- [ ] `.csproj`에 `<TargetFramework>net8.0-windows</TargetFramework>` 존재
- [ ] `App.xaml`, `MainWindow.xaml` 파일 존재
- [ ] 프로젝트 구조가 표준 WPF 구조를 따름
- [ ] 솔루션 파일(`.sln`) 존재
- [ ] 모든 검증 통과 시 "WPF 프로젝트가 정상적으로 생성되었습니다" 메시지 출력

---

#### Phase 1.3.3: WPF UI 라이브러리 NuGet 패키지 설치

**목표**: WPF UI (lepoco/wpfui) 라이브러리 설치 및 Fluent Design 기반 설정

**세부 목표**:
1. NuGet에서 `WPF-UI` 패키지 설치 (3.x 이상)
2. `App.xaml`에 리소스 딕셔너리 추가
3. `MainWindow.xaml`을 `<ui:FluentWindow>`로 변경
4. Mica 배경 효과 설정

**참조 문서 및 섹션**:
- `docs/windows/technical-specification-server.md` §10.2.1 아키텍처 설계

**검증**:
- [ ] `.csproj`에 `<PackageReference Include="WPF-UI" />` 존재
- [ ] `App.xaml`에 `<ui:ThemesDictionary Theme="Dark" />` 존재
- [ ] `MainWindow.xaml`이 `<ui:FluentWindow>` 사용
- [ ] 빌드 시 WPF-UI 관련 오류 없음
- [ ] NuGet 패키지 복원 성공

---

#### Phase 1.3.4: Hello World UI 및 Fluent Design 테마 확인

**목표**: WPF UI 기반 Hello World 화면 구성 및 Fluent Design 정상 동작 확인

**세부 목표**:
1. `MainWindow.xaml`에 UI 추가 (TitleBar, TextBlock, Button)
2. `SystemThemeWatcher` 연동
3. 윈도우 크기 1200x800, 중앙 배치

**참조 문서 및 섹션**:
- `docs/windows/technical-specification-server.md` §10.2.2 Fluent Design System 구현

**검증**:
- [ ] 빌드 성공
- [ ] 실행 시 윈도우가 중앙에 1200x800 크기로 표시
- [ ] 타이틀바에 "BridgeOne Server" 표시
- [ ] "BridgeOne Server - Hello World" 텍스트 표시
- [ ] 버튼이 Fluent Design 스타일로 렌더링
- [ ] 시스템 테마 변경 시 앱 테마 자동 전환

## Phase 1 핵심 성과

- 각 플랫폼별 프로젝트 구성 및 Hello World 빌드 성공
- 각 Phase 완료 즉시 빌드 및 실행 검증 완료
- 디버깅 환경 정상 동작 확인

---

## 다음 단계

Phase 1 완료 후 Phase 2: 통신 안정화로 진행

**Phase 2 목표**: Android ↔ Board ↔ Windows 간 HID/CDC 통신 완전 안정화
**개발 기간**: 5-6주
**핵심 목표**: HID 키보드/마우스 입력 + Vendor CDC JSON 쌍방향 통신 완벽 검증
