# BridgeOne - 접근성 우선 Android-PC 입력 브릿지

## 🎯 프로젝트 목적

**BridgeOne**는 근육장애로 인해 기존 키보드/마우스 사용이 어려운 사용자들을 위한 **접근성 우선 Android-PC 입력 브릿지**입니다.

### 핵심 설계 원칙
- **🤏 단일 터치**: 모든 기능을 한 번의 터치로 수행
- **🎯 컴팩트 레이아웃**: 중앙 하단 240×280dp 영역에 모든 조작 집중
- **🤚 한손 조작**: 엄지손가락만으로 완전한 PC 제어
- **⚡ 즉시 사용**: 복잡한 설정 없이 연결만 하면 바로 사용 가능
- **🔄 안정성**: 작업 중 연결 끊김으로 인한 스트레스 방지

---

## 🏗️ 프로젝트 구조

```
BridgeOne/
├── docs/                      # 프로젝트 문서화
│   ├── PRD.md                # 제품 요구사항 명세서
│   ├── technical-specification.md # 기술 명세서
│   ├── development-plan-checklist.md # 개발 계획 체크리스트
│   ├── Android/              # Android 관련 문서들
│   ├── Board/                # ESP32-S3 관련 문서들
│   ├── Windows/              # Windows 관련 문서들
│   └── *.md                  # 기타 문서들
├── resources/                # 공통 리소스 파일들
│   ├── drawable/             # 아이콘 및 드로어블
│   ├── font/                # 폰트 파일들
│   ├── svg/                 # SVG 이미지들
│   ├── cur,ani/             # 커서 파일들
│   └── ico/                 # 아이콘 파일들
├── src/                      # 개발환경 (소스 코드)
│   ├── android/              # Android 클라이언트 앱 (Kotlin)
│   │   ├── app/src/main/java/com/bridgeone/
│   │   │   ├── MainActivity.kt      # 메인 액티비티
│   │   │   └── ui/theme/           # 앱 테마 설정
│   │   └── build.gradle.kts
│   ├── board/                # ESP32-S3 하드웨어 동글
│   │   └── BridgeOne/
│   │       ├── src/               # 펌웨어 소스 코드
│   │       ├── platformio.ini     # PlatformIO 설정
│   │       └── library.properties # Arduino 라이브러리 설정
│   └── windows/              # Windows 서버 애플리케이션 (C#)
│       └── BridgeOne/
│           ├── src/              # 서버 소스 코드
│           └── BridgeOne.csproj  # 프로젝트 파일
├── README.md
└── build.gradle.kts          # 프로젝트 전체 빌드 설정
```

---

## 🚀 주요 기능

### 접근성 특화 기능
- **터치패드 모드**: 단일 터치로 정밀한 커서 제어
- **탭 안정화**: 손떨림 방지 알고리즘으로 정확한 클릭
- **키보드 모드**: 자주 사용하는 단축키 큰 버튼으로 제공
- **페이지 슬라이드**: 여백 영역 슬라이드로 직관적 모드 전환
- **길게 누르기**: 우클릭, 드래그 등을 위한 대체 입력 방식

### 핵심 기능
- **🖱️ 마우스 제어**: 이동, 좌/우클릭, 스크롤, 드래그
- **⌨️ 키보드 제어**: 단축키 (Ctrl+C/V, Alt+Tab, Space, Enter)
- **🔌 USB-OTG 하드웨어 연결**: ESP32-S3 동글을 통한 안정적이고 빠른 연결
- **⚡ 초저지연 통신**: 하드웨어 기반 USB HID 프로토콜로 1-2ms 입력 지연
- **🛡️ Boot-safe 지원**: BIOS, BitLocker, UAC 등 OS 부팅 전 단계에서도 동작
- **🔄 양방향 통신**: 확장 기능을 위한 Android ↔ PC 양방향 데이터 채널

---

## 💻 시스템 요구사항

### Android 클라이언트
- **OS**: Android 8.0 (API 26) 이상
- **연결**: USB-OTG 지원 (필수)
- **USB 지원**: USB Host API 지원
- **접근성**: 단일 터치 입력 가능

### ESP32-S3 하드웨어 동글
- **MCU**: ESP32-S3-WROOM-1-N16R8
- **메모리**: 512KB SRAM, 16MB Flash, 8MB PSRAM
- **펌웨어**: Arduino HID + Vendor CDC 하이브리드 방식
- **연결**: USB-C (PC 연결), UART (Android 연결)
- **프로토콜**: USB HID Boot Mouse + Vendor CDC Interface

### PC 호환성
- **OS**: Windows 10/11, macOS, Linux (USB HID 표준 지원)
- **부팅 단계**: BIOS/UEFI, BitLocker, UAC 호환
- **연결**: USB 포트 (데이터 통신 지원 필수)

---

## 🔧 설치 및 실행

### ESP32-S3 하드웨어 설정

#### 1. 하드웨어 준비물
- **ESP32-S3 DevKitC-1 보드** (ESP32-S3-WROOM-1-N16R8 모듈 탑재)
- **OTG Y 케이블** (USB-C, Charge-while-OTG 지원)
- **USB-C 케이블** 2개 (보드 UART 포트 ↔ Android, 보드 USB 포트 ↔ PC)
- **충전기** (5V 2A 이상, PD 지원 권장)

**참고**: 외부 USB-Serial 어댑터나 점퍼 케이블은 필요하지 않습니다. 보드 내장 USB-Serial을 사용합니다.

#### 2. 펌웨어 빌드 및 플래시

##### Arduino IDE를 사용한 방법
```bash
# 1. Arduino IDE 설치 및 설정
# - Arduino IDE 다운로드 및 설치
# - 보드 매니저에서 ESP32 지원 추가
# - 라이브러리 매니저에서 USB HID 및 CDC 라이브러리 설치

# 2. 프로젝트 열기 및 빌드
# Arduino IDE에서 BridgeOne.ino 파일 열기
# Tools > Board에서 ESP32-S3 선택
# Tools > Port에서 해당 포트 선택
# Upload 버튼 클릭하여 빌드 및 플래시
```

##### PlatformIO를 사용한 방법
```bash
# 1. PlatformIO 설치
pip install platformio

# 2. 프로젝트 빌드
cd src/board/BridgeOne
pio run

# 3. ESP32-S3에 펌웨어 플래시
pio run --target upload

# 4. 시리얼 모니터 (디버그용)
pio device monitor
```

#### 3. 하드웨어 연결
```
[스마트폰] ─USB-C─> [OTG Y 케이블] ┬─Device포트─> [ESP32-S3 DevKitC-1 UART 포트] ─(내장)─> [ESP32-S3] ─USB 포트─> [PC]
                                  └─Power포트─> [충전기(PD)]
```

**연결 순서**:
1. ESP32-S3 DevKitC-1의 **USB 포트**(오른쪽)를 PC에 연결
2. ESP32-S3 DevKitC-1의 **UART 포트**(왼쪽)를 OTG Y 케이블의 Device 포트에 연결
3. OTG Y 케이블을 스마트폰에 연결
4. OTG Y 케이블의 Power 포트에 충전기 연결

#### 4. USB 성능 최적화 설정 (Windows)

**목적**: Windows 11에서의 USB CDC 성능 문제 방지 및 실시간 통신 최적화

**배경 및 필요성**:

Windows 11에서 ESP32-S3의 USB CDC 통신 시 다음과 같은 성능 문제가 발생할 수 있습니다:

- **데이터 전송 오류**: 정상적으로 작동하던 장치가 Windows 11에서 오류 발생
- **CRC 체크 실패**: 데이터 무결성 검증 실패로 인한 통신 불안정
- **데이터 순서 뒤바뀜**: 전송된 데이터의 순서가 뒤섞여 명령 오류 발생
- **통신 지연**: 실시간 통신 요구사항에 부적합한 지연 시간 증가

**BridgeOne에서의 영향**:
- 멀티 커서 기능의 부정확한 동작
- 매크로 실행 요청의 지연 또는 실패
- 터치패드 입력의 반응성 저하
- 전체적인 사용자 경험 악화

**해결책**:
USB Selective Suspend 설정을 통해 Windows 11의 USB 전력 관리 정책을 최적화하여 위 문제들을 해결할 수 있습니다.

**설정 방법**:
1. **레지스트리 편집기 열기**
   ```bash
   Win + R → regedit → Enter
   ```

2. **해당 경로로 이동**
   ```
   HKEY_LOCAL_MACHINE\SYSTEM\CurrentControlSet\Enum\USB\VID_303A&PID_82C5\Device Parameters
   ```

3. **새 값 추가**
   - **이름**: `IdleUsbSelectiveSuspendPolicy`
   - **타입**: `DWORD (32-bit)`
   - **값**: `0x00000001` (유휴 시 선택적 일시정지)

4. **시스템 재부팅**
   설정 적용을 위해 재부팅 또는 ESP32-S3 재연결

**주의사항**:
- ⚠️ **레지스트리 백업**: 설정 전 반드시 백업
- 🧪 **테스트 환경**: 개발 환경에서 먼저 테스트
- 📊 **성능 측정**: 설정 후 통신 지연 시간 측정

**BridgeOne에서의 중요성**:
- 멀티 커서 등 고급 기능의 실시간 통신 최적화
- 매크로 실행 요청 중계 시 지연 시간 최소화
- Windows 11 환경에서의 안정성 향상

### Android 앱 설치
```bash
# 1. Android Studio에서 프로젝트 열기
cd src/android

# 2. Gradle 동기화
./gradlew sync

# 3. 디바이스에 설치
./gradlew installDebug
```

---

## 🎮 사용법

### 1. 하드웨어 연결 설정
1. **ESP32-S3 동글을 PC에 연결**
   - USB-C 케이블로 ESP32-S3와 PC 연결
   - PC에서 "HID-compliant mouse" 장치 인식 확인
   - 장치 관리자에서 정상 인식 여부 확인

2. **Android와 ESP32-S3 연결**
   - OTG Y 케이블을 스마트폰에 연결
   - Device 포트에 ESP32-S3 DevKitC-1 UART 포트 연결
   - Power 포트에 충전기 연결 (동시 충전)

3. **연결 확인**
   - Android에서 ESP32-S3 DevKitC-1 UART 포트 인식 확인
   - BridgeOne 앱 실행 → 자동으로 보드 연결
   - PC에서 마우스 커서 움직임 테스트

### 2. BIOS/Boot-safe 모드
- **BIOS 진입 전**: 기본 마우스 기능만 사용 가능
- **OS 진입 후**: 휠, 스크롤, 확장 기능 활성화
- **지원 환경**: BIOS, BitLocker, UAC, 로그인 화면

### 3. 접근성 조작 방법
- **터치패드 페이지**: 터치로 커서 이동, R버튼으로 우클릭
- **키보드 페이지**: 큰 버튼으로 주요 단축키 입력
- **페이지 전환**: 여백 영역에서 좌우 슬라이드

---

## 🏛️ 기술 스택

### Android (Kotlin)
- **UI**: Jetpack Compose
- **아키텍처**: MVVM + Clean Architecture
- **의존성 주입**: Hilt
- **통신**: USB-OTG (USB Host API)
- **비동기**: Coroutines + Flow
- **USB**: Android USB Host API, UsbSerialForAndroid

### ESP32-S3 하드웨어 (C++)
- **플랫폼**: Arduino IDE / PlatformIO (ESP32 Arduino Core)
- **USB 스택**: Arduino USB 라이브러리 (HID + CDC 복합 장치)
- **통신**: UART (Android 연결), USB HID (PC 연결)
- **프로토콜**: USB HID Boot Mouse + Vendor CDC Interface
- **RTOS**: FreeRTOS (멀티 태스킹)

---

## 📋 통신 프로토콜

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
// 큰 델타는 ESP32-S3에서 분할 처리
```

### USB HID 프로토콜 (ESP32-S3 → PC)
```c
// Boot Protocol (BIOS/BitLocker 호환)
struct HidMouseReport {
    uint8_t buttons;   // 마우스 버튼 상태
    int8_t  x;         // X 이동 (-127~127)
    int8_t  y;         // Y 이동 (-127~127)
};

// Report Protocol (OS 진입 후)
struct HidMouseReportExtended {
    uint8_t buttons;   // 마우스 버튼 상태
    int8_t  x, y;      // 좌표 이동
    int8_t  wheel;     // 휠 스크롤
};
```

### 양방향 확장 통신 (Vendor CDC Interface)
```c
// Vendor CDC 프로토콜 (ESP32-S3 ↔ PC)
// 시리얼 통신을 통한 양방향 데이터 교환

// 명령 프레임 (Android → ESP32-S3 → PC)
struct VendorCommand {
    uint8_t command;     // 명령 타입 (0x01: 매크로 실행 등)
    uint8_t length;      // 데이터 길이
    uint8_t data[62];    // 명령 데이터 (최대 62바이트)
};

// 응답 프레임 (PC → ESP32-S3 → Android)
struct VendorResponse {
    uint8_t status;      // 응답 상태 (0x00: 성공, 0x01: 실패 등)
    uint8_t length;      // 응답 데이터 길이
    uint8_t data[62];    // 응답 데이터
};
```

```json
// 활용 예시 - 매크로 실행 요청
{
  "command": "MACRO_START_REQUEST",
  "macroId": "photoshop_new_document",
  "parameters": {
    "width": 1920,
    "height": 1080
  }
}

// 응답 예시
{
  "status": "SUCCESS",
  "message": "매크로 실행 완료",
  "result": {
    "documentCreated": true,
    "documentId": "doc_12345"
  }
}
```

---

## 🔒 보안 및 접근성

### 보안
- **하드웨어 분리**: 물리적 USB HID 브릿지로 네트워크 공격 벡터 제거
- **표준 HID 프로토콜**: OS 레벨에서 신뢰할 수 있는 표준 인터페이스
- **BIOS 레벨 호환**: 부팅 과정 전체에서 안전한 입력 보장
- **프레임 검증**: 8바이트 프로토콜 무결성 및 시퀀스 검증
- **권한 최소화**: USB Host 권한만 필요 (네트워크 권한 불요)

### 접근성 고려사항
- **큰 터치 영역**: 최소 80×60dp 버튼 크기
- **햅틱 피드백**: 모든 터치에 진동 응답
- **시각적 피드백**: 연결 상태 및 처리 결과 표시
- **개인 맞춤화**: 터치 감도, 버튼 크기 조절

---

## 🛣️ 개발 로드맵

### 🔄 Phase 1: 하드웨어 브릿지 기반 구현 (진행중)
- **ESP32-S3 USB HID 동글 개발** (하드웨어 브릿지)
- 8바이트 UART 프로토콜 구현
- Boot Protocol 마우스 (BIOS 호환)
- Android USB-OTG 통신

### 📋 Phase 2: 접근성 UI & 확장 기능 (예정)
- **Android 앱 UI 구현** (Jetpack Compose)
- 단일 터치 터치패드
- 모드 전환 시스템 (터치패드 ↔ 키보드)
- Report Protocol 확장 (휠, 스크롤)
- 1-2ms 입력 지연 최적화

### 📋 Phase 3: 고급 기능 & 양방향 통신 (예정)
- Vendor CDC Interface (양방향 채널)
- 매크로 시스템
- 멀티 커서 기능
- Windows 백그라운드 서비스 (선택적)

---

## 🤝 기여 방법

1. **이슈 제보**: 버그 발견이나 개선사항 제안
2. **접근성 피드백**: 실제 사용자 관점의 개선사항
3. **코드 기여**: 풀 리퀘스트를 통한 기능 개선
4. **문서 개선**: 사용법, 설치 가이드 개선
5. **테스트**: 다양한 환경에서의 호환성 테스트

---

## 📚 참고 및 활용한 라이브러리 및 자료

BridgeOne 프로젝트 개발에 활용된 핵심 라이브러리와 공식 문서들을 정리했습니다.

### 하드웨어 관련

#### ESP32-S3 DevKitC-1 보드 내장 USB-Serial
- **공식 문서**: [ESP32-S3-DevKitC-1 사용자 가이드](https://docs.espressif.com/projects/esp-idf/en/latest/esp32s3/hw-reference/esp32s3/user-guide-devkitc-1.html)
- **주요 활용**: Android 앱에서 보드 내장 USB-Serial을 통한 UART 통신
- **핵심 기능**: 보드 내장 USB-Serial 칩, GPIO43/44 연결, 외부 어댑터 불필요

#### ESP32-S3 Arduino Core (Espressif)
- **공식 문서**: [ESP32 Arduino Core](https://docs.espressif.com/projects/arduino-esp32/en/latest/)
- **주요 활용**: USB HID 마우스 펌웨어, UART 통신, Arduino HID + CDC 복합 장치
- **핵심 기술**: Arduino Core, FreeRTOS, USB HID Boot Protocol
- **보드 지원**: [Boards Manager 설치 가이드](https://docs.espressif.com/projects/arduino-esp32/en/latest/installing.html)

#### ESP-IDF (Espressif)
- **공식 문서**: [ESP-IDF Programming Guide](https://docs.espressif.com/projects/esp-idf/en/latest/esp32s3/)
- **주요 활용**: 고급 USB 기능, 세부 하드웨어 제어 (선택적 사용)
- **핵심 기술**: ESP-IDF 프레임워크, 고성능 USB 스택

#### ESP32-S3-WROOM-1 모듈
- **공식 문서**: [ESP32-S3-WROOM-1 데이터시트](https://www.espressif.com/sites/default/files/documentation/esp32-s3-wroom-1_wroom-1u_datasheet_en.pdf)
- **주요 활용**: 하드웨어 스펙, 핀 배치, GPIO43/44 UART 매핑, 16MB Flash 파티션 설계

### Android 개발 관련

#### USB Host API (Google)
- **공식 문서**: [Android USB Host API](https://developer.android.com/develop/connectivity/usb/host)
- **주요 활용**: USB-OTG 장치 인식, USB-Serial 장치 연결 및 권한 관리
- **핵심 기능**: USB 장치 탐지, 연결 상태 모니터링

#### USB-Serial-for-Android
- **GitHub**: [mik3y/usb-serial-for-android](https://github.com/mik3y/usb-serial-for-android)
- **주요 활용**: 범용 USB-Serial 통신 (다양한 칩셋 지원), 8바이트 프레임 송수신
- **핵심 기능**: 보드 내장 USB-Serial 칩과의 안정적인 UART 통신
- **라이선스**: MIT License

#### Jetpack Compose & Android Architecture
- **공식 문서**: [Android Jetpack](https://developer.android.com/jetpack)
- **주요 활용**: 접근성 UI 구현, MVVM 아키텍처, Coroutines + Flow
- **핵심 컴포넌트**: Hilt (DI), Compose UI, Navigation

### 표준 및 프로토콜

#### USB HID 표준
- **출처**: [USB 2.0 Specification](https://www.usb.org/documents)
- **주요 활용**: HID Boot Protocol 구현, BIOS/BitLocker 호환성
- **핵심 문서**: USB HID Usage Tables, USB OTG Specification

#### Windows HID API
- **공식 문서**: [Windows HID API](https://docs.microsoft.com/en-us/windows/win32/inputdev/human-interface-devices)
- **주요 활용**: Windows 서버 개발, 멀티 커서 관리, 매크로 시스템

### Arduino 라이브러리 및 프레임워크

#### Arduino USB Host Library
- **GitHub**: [arduino-libraries/USBHost](https://github.com/arduino-libraries/USBHost)
- **주요 활용**: USB HID/CDC 복합 장치 구현, 호스트-디바이스 통신
- **핵심 기능**: HID 마우스 에뮬레이션, CDC 시리얼 통신

#### ESP32 Arduino USB Library
- **공식 문서**: [ESP32 USB Library](https://docs.espressif.com/projects/arduino-esp32/en/latest/libraries.html)
- **주요 활용**: ESP32-S3 USB 기능 활용, HID/CDC 동시 지원
- **핵심 기능**: USB 장치 모드, 복합 디바이스 설정

### 접근성 및 사용성

#### Android 접근성 가이드
- **공식 문서**: [Android Accessibility](https://developer.android.com/guide/topics/ui/accessibility)
- **주요 활용**: 근육장애 사용자 터치패드 최적화, WCAG 2.1 AA 준수
- **핵심 원칙**: 단일 터치 지원, 큰 터치 영역, 햅틱 피드백

### 개발 도구 및 환경

#### Arduino IDE
- **공식 사이트**: [Arduino IDE](https://www.arduino.cc/en/software)
- **주요 활용**: ESP32-S3 펌웨어 개발, USB HID/CDC 라이브러리 활용
- **핵심 기능**: 보드 매니저를 통한 ESP32-S3 지원, 직관적 코드 작성 환경
- **장점**: 초보자 친화적, 광범위한 커뮤니티 지원

#### PlatformIO
- **공식 사이트**: [PlatformIO](https://platformio.org/)
- **주요 활용**: ESP32-S3 펌웨어 빌드, 라이브러리 관리, 디버깅
- **장점**: 크로스 플랫폼 지원, 통합 개발 환경, 고급 프로젝트 관리

#### Android Studio & Gradle
- **공식 도구**: [Android Studio](https://developer.android.com/studio)
- **주요 활용**: Android 앱 개발, 의존성 관리, 빌드 자동화
- **버전 관리**: Kotlin DSL, Version Catalog

---

## 📄 라이선스

이 프로젝트는 **접근성 향상**을 목적으로 개발되었으며, **MIT 라이선스** 하에 배포됩니다.

---

## 🙏 감사의 말

이 프로젝트는 신체적 제약으로 인해 기존 입력 장치 사용이 어려운 모든 사용자들이 더 자유롭게 컴퓨터를 사용할 수 있기를 바라는 마음으로 개발되었습니다.

**"기술은 모든 사람을 위한 것이어야 합니다"**

---

## 📞 지원 및 문의

- **GitHub Issues**: 버그 리포트 및 기능 요청
- **문서**: `/docs` 폴더의 상세 기술 문서
  - `PRD.md`: 제품 요구사항 명세서
  - `design-guide-app.md`: 앱 전체 디자인 가이드
  - `component-design-guide.md`: UI 컴포넌트 상세 명세
  - `styleframe-*.md`: 페이지별 스타일프레임
- **하드웨어**: `/Board/BridgeOne` 폴더의 ESP32-S3 펌웨어

**BridgeOne**와 함께 더 접근 가능한 디지털 환경을 만들어 나가요! 🌟