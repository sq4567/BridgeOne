# BridgeOne ESP32-S3 Hardware Reference

ESP32-S3 하드웨어 관련 상세 정보를 제공합니다. USB 포트 구성, 로그 출력 경로, 연결 방식, TinyUSB 설정 등을 포함합니다.

---

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
  idf.py -p COM8 flash            # 펌웨어 플래시
  idf.py -p COM8 monitor          # 시리얼 모니터링
Windows: COM8 (현재 개발 환경 기준)
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
  - Windows: COM3 (현재 개발 환경 기준, 장치 관리자 → 포트(COM & LPT) → "USB Serial Device")
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
- **포트**: CH343P USB-to-UART (포트 1) → **COM8**
- **케이블**: USB-A to USB-C
- **명령어**: `idf.py -p COM8 flash`
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

**현재 개발 환경 기준 COM 포트 매핑:**
| 포트 | COM 포트 | 장치 관리자 표시 |
|------|---------|----------------|
| 포트 1️⃣ (CH343P) | **COM8** | "USB-Enhanced-SERIAL CH343" |
| 포트 2️⃣ (USB-OTG) | **COM3** | "USB Serial Device" |

```bash
# 사용 가능한 포트 확인
idf.py list-ports

# 포트별 식별 방법
# CH343P (포트 1): "CH343" 또는 "CP210x" 표시 → COM8
# Native USB (포트 2): "USB Serial Device" 표시 → COM3
```

---

## ESP32-S3 로그 출력 경로

**⚠️ 중요: 부팅 로그와 애플리케이션 로그의 출력 경로가 다릅니다**

### 📍 포트별 로그 출력 범위

#### **포트 1️⃣ (COM8 - CH343P UART0)**
**항상 볼 수 있는 로그:**
- ✅ ROM Bootloader 로그 (최초 부팅 메시지)
- ✅ 2nd Stage Bootloader 로그
- ✅ 애플리케이션 초기화 로그 (USB CDC 초기화 전)
- ⚠️ 애플리케이션 실행 로그 (프로젝트 설정에 따라 다름)

**특징:**
- ESP32-S3의 기본 콘솔 출력 포트
- 전원 인가 직후부터 모든 부팅 과정 확인 가능
- `idf.py monitor` 명령어가 연결되는 포트
- 부팅 실패, 초기화 오류 디버깅에 필수

#### **포트 2️⃣ (COM3 - USB-OTG CDC)**
**볼 수 있는 로그:**
- ❌ ROM Bootloader 로그 (절대 볼 수 없음)
- ❌ 2nd Stage Bootloader 로그 (절대 볼 수 없음)
- ❌ USB CDC 초기화 전 로그 (볼 수 없음)
- ✅ **USB CDC 초기화 후** 애플리케이션 로그 (리다이렉트 설정 시)

**특징:**
- TinyUSB 스택 초기화 후에만 사용 가능
- 애플리케이션 레벨 로그만 출력
- HID 장치와 동일한 포트 (동시 사용 가능)
- 애플리케이션 실행 중 로그 확인에 유용

### 🔍 BridgeOne 프로젝트의 로그 설정

본 프로젝트는 **애플리케이션 로그를 USB CDC로 리다이렉트** 설정되어 있습니다:
- 부팅 과정: COM 포트로만 출력
- 앱 초기화: COM 포트로만 출력
- USB CDC 초기화 후: USB CDC 포트로 리다이렉트
- HID 동작 로그: USB CDC 포트로 출력

### 📊 로그 타임라인

```
시간 순서별 로그 출력 경로:

├─ [부팅 시작] ──────────────────────────────────┐
│  ROM Bootloader 로그                          │ 포트 1️⃣ (COM) 전용
│  2nd Stage Bootloader 로그                    │
├─ [앱 초기화 시작] ──────────────────────────────┤
│  FreeRTOS 초기화                              │ 포트 1️⃣ (COM) 전용
│  USB 스택 초기화 전 로그                        │
├─ [USB CDC 초기화 완료] ─────────────────────────┤
│  애플리케이션 실행 로그 (ESP_LOG)              │ 포트 2️⃣ (USB CDC)
│  UART 통신 로그                               │ (리다이렉트 설정 시)
│  HID 전송 로그                                │
└─────────────────────────────────────────────────┘

⚠️ USB CDC 초기화 실패 시: 모든 로그가 포트 1️⃣ (COM)로 출력
```

### ✅ 권장 모니터링 방식

#### **풀 연결 시 (개발 권장 구성):**
```
Tera Term 인스턴스 1 (포트 1️⃣ - COM8):
  - 용도: 전체 부팅 과정 + 초기화 로그
  - 확인 항목: ROM/Bootloader 메시지, 시스템 크래시, 리셋 원인
  - 보드 레이트: 115200

Tera Term 인스턴스 2 (포트 2️⃣ - COM3):
  - 용도: 애플리케이션 실행 로그 (CDC 초기화 후)
  - 확인 항목: UART 프레임 수신, HID 전송, 디버그 메시지
  - 보드 레이트: 115200
```

#### **디버깅 목적별 포트 선택:**
```yaml
부팅 실패 / 시스템 크래시 디버깅:
  필수 포트: 포트 1️⃣ (COM8)
  이유: ROM bootloader, 리셋 원인 확인 필수

USB 초기화 문제 디버깅:
  필수 포트: 포트 1️⃣ (COM8)
  이유: TinyUSB 초기화 전 로그 확인 필수

애플리케이션 로직 디버깅:
  권장 포트: 포트 2️⃣ (COM3) 또는 포트 1️⃣ (COM8)
  이유: 양쪽 모두 가능, USB CDC는 HID와 동시 확인 가능

HID 동작 확인:
  권장 포트: 포트 2️⃣ (COM3)
  이유: HID 장치와 같은 USB 포트 사용

UART 프레임 분석:
  권장 포트: 포트 2️⃣ (COM3)
  이유: Android 입력 → UART 수신 → HID 전송 전체 흐름 확인
```

### 🎯 핵심 원칙

**완전한 로그 확인을 위한 원칙:**
1. **부팅 로그는 항상 포트 1️⃣ (COM8)**: ROM bootloader부터 확인 가능
2. **애플리케이션 로그는 포트 2️⃣ (COM3)**: 초기화 후 사용 가능
3. **문제 발생 시 포트 1️⃣ (COM8) 먼저 확인**: 초기화 실패 시 모든 로그가 여기로 출력
4. **양쪽 포트 동시 모니터링 권장**: COM8 + COM3 전체 시스템 동작 이해에 유리

**흔한 오해:**
- ❌ "CDC 관련 로그는 USB-OTG로만 볼 수 있다"
  - ⚠️ 부분적으로 맞음: USB CDC **초기화 후** 애플리케이션 로그만 해당
  - ✅ 정확한 이해: USB CDC 초기화 전 로그는 여전히 COM 포트에서만 확인 가능

- ❌ "USB-OTG 포트를 연결하면 모든 로그를 볼 수 있다"
  - ✅ 정확한 이해: 부팅 과정과 USB 초기화 전 로그는 절대 볼 수 없음

---

## 하드웨어 연결 방식 약어 정의

개발 시 다양한 연결 시나리오를 빠르게 지칭하기 위해 다음의 표준 약어를 사용합니다:

### 연결 방식 분류

| 약어 | 정식 명칭 | 구성 | 주요 용도 |
|------|---------|------|---------|
| **풀 연결** | Full Connection | Android ↔ 포트 1️⃣(COM) + PC ↔ 포트 2️⃣(USB-OTG) | 완전한 통합 테스트, 실제 운영 환경 |
| **1차 연결** | Primary Connection | Android ↔ 포트 1️⃣(COM) (포트 2️⃣ 미연결) | Android 앱 개발, USB 통신 테스트 |
| **2차 연결** | Secondary Connection | PC ↔ 포트 2️⃣(USB-OTG) (포트 1️⃣ 미연결) | HID 동작 테스트, 디버그 로그 확인 |
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
포트 2️⃣ (USB-OTG): 미연결

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
포트 1️⃣ (COM): 미연결

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

---

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
