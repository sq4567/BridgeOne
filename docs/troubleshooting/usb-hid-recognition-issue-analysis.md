# ESP32-S3 USB 완전 끊김 문제 - 원인 정밀 분석

## 문서 정보
- **작성일**: 2025-11-12
- **문제 발생일**: 2025-11-12 (eFuse USB_PHY_SEL 설정 후)
- **프로젝트**: BridgeOne ESP32-S3 펌웨어
- **심각도**: 🔴 CRITICAL (부팅 불가, USB 완전 차단)
- **상태**: 🔴 복구 불가능 (ROM 부트로더 버그)
- **마지막 업데이트**: 2025-11-14 (최종 진단: ROM 부트로더 eFuse 버그로 인한 보드 복구 불가능)

---

## 1. 문제 현상

### 1.1 즉각적인 증상
ESP32-S3에 eFuse USB_PHY_SEL 설정 후 펌웨어 플래시를 진행했을 때:

```bash
# 펌웨어 플래시
idf.py -p COM7 flash

# 결과
Wrote 230976 bytes (130027 compressed) at 0x00010000 in 3.7 seconds
Hash of data verified. ✅

# Hard reset 시도
Hard resetting with a watchdog...
A serial exception error occurred: Cannot configure port, something went wrong.
PermissionError(13, '시스템에 부착된 장치가 작동하지 않습니다.', None, 31)
```

**관찰 사항**:
- ✅ 펌웨어 플래시 성공 (Hash verified)
- ❌ Hard reset 직후 USB 연결 **완전히 끊어짐**
- ❌ 보드 리셋 버튼 눌러도 USB 재연결 안 됨
- ❌ USB 케이블 물리적 재연결해도 PC에서 인식 못함
- ❌ 시리얼 로그 출력 없음 (부팅 초기에 크래시 추정)

### 1.2 Windows 장치 상태
```powershell
Get-PnpDevice | Where-Object {$_.DeviceID -match "VID_303A"}
# 결과: 아무것도 표시되지 않음 (장치 완전히 사라짐)
```

### 1.3 이전 작업 히스토리
- **시도 #4B**: eFuse USB_PHY_SEL = 1 설정 완료 (ROM 부트로더 USB 비활성화)
- **시도 #5-6**: sdkconfig HID 설정 확인, tusb_config.h 수정
- **시도 #7**: CDC-only 디버깅 진행 (HID 완전 비활성화)
  - `CFG_TUD_HID = 0`
  - `usb_descriptors.c`에서 HID 디스크립터 제거
  - CDC만 활성화 (Interface 0/1)

---

## 2. 환경 정보

### 2.1 하드웨어
- **보드**: ESP32-S3-DevkitC-1-N16R8
- **USB 연결**: Type-C (USB OTG + Serial JTAG 겸용)
- **eFuse 설정**: `USB_PHY_SEL = 1` (ROM 부트로더 USB 비활성화됨)

### 2.2 펌웨어 설정 (시도 #7 상태)
**tusb_config.h**:
```c
#define CFG_TUD_HID         0   // HID 완전 비활성화
#define CFG_TUD_CDC         1   // CDC 활성화
```

**usb_descriptors.c**:
- HID Report Descriptor: `#if 0` (비활성화)
- Configuration Descriptor: CDC만 활성화 (Interface 0/1)

**BridgeOne.c**:
- HID 태스크: `#if 0` (비활성화)
- USB 태스크: 활성화 (Core 1, Priority 4)
- UART 태스크: 활성화 (Core 0, Priority 6)

### 2.3 ESP-IDF 설정 (sdkconfig)
**중요한 설정**:
```ini
# 콘솔 설정
CONFIG_ESP_CONSOLE_UART=y                           ✅ (UART로 콘솔)
CONFIG_ESP_CONSOLE_SECONDARY_USB_SERIAL_JTAG=y      ❌ (보조 콘솔 USB JTAG)
CONFIG_ESP_CONSOLE_USB_SERIAL_JTAG_ENABLED=y        ❌ (USB Serial JTAG 활성화)

# USB 설정
CONFIG_USB_HOST_CONTROL_TRANSFER_MAX_SIZE=256       ✅
CONFIG_USB_HOST_HW_BUFFER_BIAS_BALANCED=y           ✅

# TinyUSB 설정
CONFIG_TINYUSB=y                                    ✅
CONFIG_TINYUSB_CDC_ENABLED=y                        ✅
CONFIG_TINYUSB_HID_ENABLED=n                        ✅ (HID 비활성화)
```

---

## 3. 문제 현황 업데이트 (2025-11-13)

### 3.0 시도 #9 진행 결과 및 부팅 실패 분석

#### 3.0.1 실행 결과 요약
- ✅ **sdkconfig 패치**: 성공 (USB_JTAG 관련 3개 설정 모두 =n 확인)
- ✅ **펌웨어 빌드**: 성공 (경고 없음)
- ✅ **펌웨어 플래시**: 성공 (Hash verified, 모든 파티션 정상 쓰기)
- ❌ **부팅**: 실패 (부팅 초기 크래시 추정)
- ❌ **USB 인식**: 안 됨 (연결 해제음만 들림, 재연결음 없음)

#### 3.0.2 증상 비교 (이전 vs 현재)

| 단계 | 이전 성공 빌드 | 현재 빌드 (Attempt #9) |
|------|---------------|----------------------|
| 플래시 완료 | ✅ Hash verified | ✅ Hash verified |
| Hard reset | ✅ "연결 해제음 + 연결음" 연달아 | ❌ "연결 해제음만" |
| USB 재인식 | ✅ VID_303A 장치 인식됨 | ❌ 아무 장치도 인식 안 됨 |
| 보드 강제 리셋 | ✅ USB 재연결됨 | ❌ USB 인식 안 됨 |
| 시리얼 로그 | ✅ `[BOOT STAGE 1]` 출력 | ❌ 아무 로그도 안 나옴 |

#### 3.0.3 부팅 실패 메커니즘 정밀 분석

**3.0.3.1 "연결 해제음만" 들리는 현상의 의미**

Windows에서 USB 장치 연결/해제 시 소리가 나는 시점:
1. **연결 해제음**: 기존 USB 장치가 제거될 때
   - 현재 상황: Hard reset 시 Download Mode의 USB Serial JTAG가 연결 해제됨
   - 이것은 정상 (reset으로 인한 USB 연결 끊김)

2. **연결음**: 새로운 USB 장치가 열거될 때
   - 정상 상황: 펌웨어 부팅 후 TinyUSB가 초기화되면 USB 장치 열거 시작
   - 현재 상황: **연결음이 없음** → TinyUSB 초기화에 도달하지 못함

**3.0.3.2 USB Enumeration이 시작되지 않는다는 증거**

USB 장치가 열거되려면:
```
1. 펌웨어가 정상적으로 부팅됨
2. app_main() 진입
3. tusb_init() 호출
4. TinyUSB가 USB PHY 초기화
5. USB 장치 디스크립터 전송
6. Windows가 장치 인식 → "연결음" 발생
```

현재 상황: **1번 또는 2번 단계에서 크래시 발생 추정**
- 3번 tusb_init()에 도달하지 못함
- USB enumeration이 전혀 시작되지 않음
- Windows는 아무 USB 장치도 감지하지 못함

**3.0.3.3 ROM 부트로더도 USB를 열거하지 않는 이유**

정상 상황 (eFuse USB_PHY_SEL = 0):
```
ROM 부트로더 → USB Serial JTAG 초기화 → Download Mode
```

eFuse 설정 후 (USB_PHY_SEL = 1):
```
ROM 부트로더 → eFuse 확인 → USB Serial JTAG 스킵
→ UART 부트로더만 활성화 → 펌웨어 로드
→ 펌웨어 실행 (TinyUSB가 USB를 담당해야 함)
```

**결론**: ROM 부트로더가 USB를 열거하지 않는 것은 정상 (eFuse 설정의 의도된 동작)
**문제**: 펌웨어가 부팅에 실패하여 TinyUSB도 초기화되지 않음

#### 3.0.4 부팅 실패 타이밍 추정

**시리얼 로그가 없다는 것의 의미**:

ESP32-S3 부팅 순서:
```
1. ROM 부트로더 시작
   └─ UART 초기화 (115200 baud) ✅
   └─ 간단한 부트 메시지 출력 (보통 출력됨) ✅

2. 2nd stage 부트로더 시작
   └─ 파티션 테이블 로드 ✅
   └─ 부트로더 로그 출력 (ESP-ROM:esp32s3-...) ✅

3. 애플리케이션 로드
   └─ app_main() 진입 준비
   └─ ESP-IDF 시스템 초기화 시작 🔴 ← 여기서 크래시 추정
```

**관찰**:
- ROM 부트로더 메시지도 안 보임
- 2nd stage 부트로더 메시지도 안 보임
- app_main() 로그는 당연히 안 보임

**가능한 원인**:
1. **UART 출력이 아예 안 되는 상황** (하드웨어 문제 가능성 낮음)
2. **부팅이 너무 일찍 크래시되어 UART 출력도 못함**
3. **크래시 핸들러가 실행되지 않음** (패닉 루프)

#### 3.0.5 sdkconfig 설정의 한계

**시도 #9에서 올바르게 설정된 것**:
```ini
CONFIG_USJ_ENABLE_USB_SERIAL_JTAG=n              ✅
CONFIG_ESP_CONSOLE_SECONDARY_USB_SERIAL_JTAG=n   ✅
CONFIG_ESP_CONSOLE_USB_SERIAL_JTAG_ENABLED=n     ✅
```

**하지만 여전히 부팅 실패하는 이유**:
1. **ESP-IDF 내부 코드가 여전히 USB Serial JTAG 초기화 시도 가능성**
   - sdkconfig 설정만으로는 모든 코드 경로를 막을 수 없음
   - 컴파일 타임에 제거되지 않은 코드가 런타임에 실행될 수 있음

2. **TinyUSB와 ESP-IDF 간 충돌 가능성**
   - 둘 다 USB PHY에 접근하려고 시도
   - eFuse 설정으로 인한 PHY 접근 제한이 예상치 못한 방식으로 작동

3. **eFuse 설정이 예상과 다르게 작동 가능성**
   - USB_PHY_SEL=1이 실제로 적용되지 않았을 수 있음
   - 또는 다른 eFuse 설정이 충돌

#### 3.0.6 펌웨어 코드 검토 필요성

**sdkconfig로 해결할 수 없는 이유**:
- sdkconfig는 ESP-IDF의 **컴파일 타임 설정**
- 하지만 일부 코드는 **런타임에 조건부로 실행**될 수 있음
- 예: `if (usb_phy_available()) { usb_serial_jtag_init(); }`

**반드시 검토해야 할 것**:
1. **BridgeOne 펌웨어 코드**:
   - BridgeOne.c의 초기화 순서
   - TinyUSB 초기화 전에 USB PHY 접근하는 코드 있는지

2. **ESP-IDF 시스템 코드** (필요 시):
   - esp_vfs_console.c: 콘솔 초기화 코드
   - usb_serial_jtag_driver.c: USB Serial JTAG 드라이버
   - 이들이 sdkconfig 설정을 무시하는지 확인

---

## 3. 근본 원인 분석 (sdkconfig 한계 도달)

### 3.1 🔴 확정된 핵심 원인: eFuse와 ESP-IDF 설정 불일치 (부분 해결됨)

#### 3.1.1 eFuse 설정의 의미
```bash
python %IDF_PATH%\components\esptool_py\esptool\espefuse.py --port COM3 burn_efuse USB_PHY_SEL 1
```

**eFuse `USB_PHY_SEL = 1`의 효과**:
- **내부 USB PHY가 USB_OTG 전용으로 전환됨**
- ROM 부트로더의 USB Serial JTAG 기능이 **하드웨어 레벨에서 완전히 비활성화**됨
- 이후 모든 USB 통신은 **TinyUSB를 통한 USB_OTG만 가능**

#### 3.1.2 🔴 eFuse 충돌 상황 (2025-11-14 확인됨)

**현재 eFuse USB/JTAG 설정 상태**:

| eFuse 이름 | 현재 값 | 의도 | 상태 |
|-----------|--------|------|------|
| **DIS_USB_JTAG** | 1 (True) | USB JTAG 비활성화 | ✅ OK |
| **DIS_USB_SERIAL_JTAG** | 0 (False) | USB Serial JTAG **활성화** | 🔴 **문제!** |
| **USB_PHY_SEL** | 1 | 내부 PHY를 USB_OTG 전용으로 할당 | ✅ OK |

**충돌 메커니즘**:
```
상황: USB_PHY_SEL = 1 설정
  ├─ 내부 PHY → USB_OTG 전용 (TinyUSB 사용)
  └─ USB Serial JTAG는 PHY 없음 (hard disabled)

BUT

sdkconfig (또는 ESP-IDF 자동 감지):
  ├─ DIS_USB_SERIAL_JTAG = 0 (활성화되어 있음)
  └─ ROM 부트로더/부팅 중 USB Serial JTAG 초기화 시도

🔴 CONFLICT!
  ⚠️ ROM 부트로더 또는 ESP-IDF가 PHY 없는 USB Serial JTAG 초기화 시도
  ⚠️ PHY 접근 불가능 → 즉시 크래시
```

#### 3.1.3 ESP-IDF 설정의 모순
하지만 `sdkconfig`에서도:
```ini
CONFIG_ESP_CONSOLE_SECONDARY_USB_SERIAL_JTAG=y      ❌ (=n으로 패치됨)
CONFIG_ESP_CONSOLE_USB_SERIAL_JTAG_ENABLED=y        ❌ (=n으로 패치됨)
CONFIG_USJ_ENABLE_USB_SERIAL_JTAG=y                 ❌ (=n으로 패치됨)
```

**결과**: eFuse `DIS_USB_SERIAL_JTAG = 0` + sdkconfig USB Serial JTAG 활성화 → **dual activation conflict** → ESP-IDF가 부팅 시 USB Serial JTAG 초기화를 시도하지만, eFuse 레벨에서 PHY에 접근할 수 없음 → **즉시 크래시**

#### 3.1.4 근본 원인 정리

### 🔴 **결론: 문제는 eFuse만입니다!**

| 항목 | 상태 | 이유 |
|------|------|------|
| **펌웨어 코드** | ✅ 정상 | USB Serial JTAG 관련 코드 없음 |
| **sdkconfig** | ✅ 정상 | 이미 USB Serial JTAG =n으로 패치됨 |
| **eFuse 설정** | 🔴 **문제!** | DIS_USB_SERIAL_JTAG=0 + USB_PHY_SEL=1 충돌 |

**eFuse 레벨 문제 (하드웨어)**:
- `DIS_USB_SERIAL_JTAG = 0`: USB Serial JTAG가 **eFuse 레벨에서 활성화**되어 있음
- `USB_PHY_SEL = 1`: 하지만 PHY는 USB_OTG 전용으로 할당됨
- **크래시 시점**: ROM 부트로더가 부팅 중에 eFuse를 읽음 → USB Serial JTAG 초기화 시도 → PHY 없음 → **즉시 크래시**

**왜 sdkconfig와 펌웨어 코드는 도움이 안 되는가?**:
- ROM 부트로더는 **eFuse만 읽음** (sdkconfig 무시)
- ROM 부트로더는 **펌웨어 로드 이전에 실행됨** (펌웨어 코드 무시)
- 따라서 sdkconfig/펌웨어 수정은 ROM 부트로더 크래시를 방지할 수 없음

### ✅ **유일한 해결책: eFuse BURN**

```bash
espefuse.py --port COM7 burn_efuse DIS_USB_SERIAL_JTAG 1
```

**이유**: eFuse `DIS_USB_SERIAL_JTAG = 1`로 설정하면, ROM 부트로더가 USB Serial JTAG 초기화를 시도하지 않음 → 크래시 방지 ✅

### 3.2 크래시 메커니즘 (상세 분석)

#### 3.2.1 정상 부팅 순서 (문제 없을 때)
```
1. ESP32-S3 ROM 부트로더 시작
   └─ USB Serial JTAG 초기화 (eFuse USB_PHY_SEL = 0일 때만)

2. ESP-IDF 2단계 부트로더 시작
   └─ 파티션 테이블 로드

3. 애플리케이션 시작 (app_main)
   ├─ 콘솔 초기화 (CONFIG_ESP_CONSOLE_UART=y)
   ├─ 보조 콘솔 초기화 (CONFIG_ESP_CONSOLE_SECONDARY_USB_SERIAL_JTAG=y)
   ├─ TinyUSB 초기화 (tusb_init)
   └─ 태스크 생성 (USB, UART, HID)
```

#### 3.2.2 실제 크래시 순서 (eFuse USB_PHY_SEL = 1일 때)
```
1. ESP32-S3 ROM 부트로더 시작
   └─ USB Serial JTAG 초기화 시도 ❌ (eFuse로 인해 PHY 접근 불가)
   └─ ROM 부트로더는 eFuse를 존중하므로 초기화 스킵 (크래시 안 함)

2. ESP-IDF 2단계 부트로더 시작
   └─ 파티션 테이블 로드 ✅

3. 애플리케이션 시작 (app_main)
   ├─ 콘솔 초기화 (CONFIG_ESP_CONSOLE_UART=y) ✅
   ├─ 보조 콘솔 초기화 시도 (CONFIG_ESP_CONSOLE_SECONDARY_USB_SERIAL_JTAG=y)
   │   └─ usb_serial_jtag_driver_install() 호출
   │   └─ USB PHY 접근 시도 ❌ (eFuse로 인해 PHY는 USB_OTG 전용)
   │   └─ 🔴 ASSERT FAILED 또는 NULL POINTER DEREFERENCE
   │   └─ 🔴 IMMEDIATE CRASH (Guru Meditation Error)
   └─ TinyUSB 초기화에 도달 못함 ❌
```

#### 3.2.3 크래시 타이밍
- **크래시 시점**: `app_main()` 시작 직후 (TinyUSB 초기화 이전)
- **크래시 위치**: ESP-IDF 콘솔 초기화 코드 (usb_serial_jtag_driver.c)
- **크래시 유형**:
  - Assert 실패: `assert(usb_phy_available())`
  - 또는 NULL 포인터 접근: PHY 핸들이 NULL일 때 접근

### 3.3 왜 시리얼 로그가 출력되지 않는가?

#### 3.3.1 로그 출력 순서
```
1. UART 초기화 (CONFIG_ESP_CONSOLE_UART=y)
   └─ UART0 (GPIO43/44) 활성화 ✅

2. ESP_LOG 시스템 초기화
   └─ 로그 출력 대상: UART0 ✅

3. 보조 콘솔 초기화 (CONFIG_ESP_CONSOLE_SECONDARY_USB_SERIAL_JTAG=y)
   └─ 🔴 여기서 크래시 발생
   └─ 크래시 핸들러 실행 (Guru Meditation)
   └─ 크래시 정보 출력 시도 → UART0으로 전송 ❓

4. 크래시 정보가 UART로 출력되어야 하지만...
   └─ 타이밍 문제: 크래시 핸들러가 UART 플러시 전에 종료
   └─ 또는 크래시가 너무 심각해서 핸들러도 실행 안 됨
```

#### 3.3.2 왜 COM7 포트도 사라지는가?
- eFuse `USB_PHY_SEL = 1` 설정으로 ROM 부트로더 USB가 비활성화됨
- 펌웨어가 크래시하면 TinyUSB도 초기화되지 않음
- 결과: **어떤 USB 장치도 열거되지 않음** (ROM도 안 됨, TinyUSB도 안 됨)
- Windows는 USB 장치를 완전히 제거함

### 3.4 ESP32-S3 부팅 시퀀스 상세 분석 (크래시 포인트 특정)

#### 3.4.1 정상 부팅 시퀀스 (단계별)

**Phase 1: ROM 부트로더 (하드웨어 초기화)**
```
1. Power-On / Reset
2. ROM 코드 실행 시작
3. CPU 기본 초기화 (클럭, 메모리)
4. eFuse 읽기 및 적용
   ├─ USB_PHY_SEL = 1 감지
   └─ USB Serial JTAG 초기화 스킵 ✅ (eFuse 존중)
5. UART0 초기화 (115200 baud) ✅
6. SPI Flash 초기화
7. 2nd stage 부트로더 로드
```

**Phase 2: 2nd Stage 부트로더 (ESP-IDF)**
```
8. 2nd stage 부트로더 실행
9. 파티션 테이블 로드
10. 애플리케이션 파티션 검증
11. 애플리케이션 이미지를 RAM으로 로드
12. 애플리케이션 엔트리 포인트로 점프
```

**Phase 3: ESP-IDF 런타임 초기화 (app_main 이전)**
```
13. ESP-IDF 시스템 초기화 시작
    ├─ 13.1. FreeRTOS 스케줄러 준비
    ├─ 13.2. 힙 메모리 초기화
    ├─ 13.3. 이벤트 루프 초기화
    ├─ 13.4. 타이머 초기화
    └─ 13.5. 콘솔 시스템 초기화 🔴 ← 크래시 의심 지점
         ├─ esp_vfs_dev_uart_register() ✅
         ├─ uart_driver_install() ✅
         └─ 🔴 usb_serial_jtag 보조 콘솔 초기화 시도? ❌
              └─ CONFIG_ESP_CONSOLE_SECONDARY_USB_SERIAL_JTAG=n이지만
              └─ 런타임에 조건부 실행 가능성?

14. app_main() 태스크 생성
15. FreeRTOS 스케줄러 시작
```

**Phase 4: 사용자 애플리케이션 (app_main)**
```
16. app_main() 진입
17. ESP_LOGI(TAG, "[BOOT STAGE 1] app_main() started") ❌ 출력 안 됨
18. tusb_init() 호출 (도달 못함)
19. TinyUSB USB PHY 초기화 (도달 못함)
20. USB 장치 열거 시작 (도달 못함)
```

#### 3.4.2 실제 크래시 시퀀스 추정 (Attempt #9)

```
Phase 1: ROM 부트로더
[1-7] ✅ 정상 동작 (eFuse 확인, UART 초기화, 부트로더 로드)

Phase 2: 2nd Stage 부트로더
[8-12] ✅ 정상 동작 (펌웨어 로드 성공 - Hash verified 증거)

Phase 3: ESP-IDF 런타임 초기화
[13.1-13.4] ✅ 정상 동작 (기본 시스템 초기화)
[13.5] 🔴 CRASH: 콘솔 초기화 중
    ├─ esp_vfs_dev_uart_register() ✅
    ├─ uart_driver_install() ✅
    └─ 🔴 usb_serial_jtag 관련 코드 실행? ❌
         └─ 가능성 1: esp_vfs_console_register() 내부
         └─ 가능성 2: usb_serial_jtag_driver_install() 호출
         └─ 가능성 3: USB PHY 상태 체크 중
         └─ 결과: eFuse로 인한 PHY 접근 불가 → ASSERT FAILED

[14-20] ❌ 도달 못함 (크래시로 인한 부팅 중단)
```

#### 3.4.3 크래시가 Phase 3에서 발생한다는 증거

**증거 1: 펌웨어가 정상적으로 로드됨**
- `idf.py flash` 성공 (Hash verified)
- 2nd stage 부트로더가 펌웨어를 RAM으로 로드 완료
- 따라서 Phase 1, 2는 정상 통과

**증거 2: app_main() 로그가 출력되지 않음**
- `[BOOT STAGE 1] app_main() started` 메시지 없음
- app_main()에 도달하지 못함
- 따라서 Phase 3에서 크래시

**증거 3: USB 장치가 열거되지 않음**
- TinyUSB 초기화 도달 못함 (app_main() 내부)
- USB enumeration 시작 안 됨
- Windows에서 연결음 없음

**증거 4: UART 로그가 전혀 없음**
- ROM 부트로더 메시지도 없음
- BUT: ROM 부트로더는 실행되었음 (펌웨어 로드 성공)
- 추정: UART가 초기화되었지만 출력이 매우 빨리 끝나고, 크래시 후 버퍼 플러시 안 됨

### 3.5 가능한 크래시 포인트 정밀 특정

#### 3.5.1 ESP-IDF 콘솔 초기화 코드 (가장 유력)

**파일**: `components/vfs/vfs_console.c`

```c
// ESP-IDF v5.5 추정 코드 (실제 코드 확인 필요)
esp_err_t esp_vfs_dev_uart_register(void) {
    // UART 초기화 (정상 동작) ✅
    uart_driver_install(UART_NUM_0, ...);

#ifdef CONFIG_ESP_CONSOLE_USB_SERIAL_JTAG_ENABLED
    // 🔴 이 부분이 문제! sdkconfig에서 =n인데 여전히 실행?
    usb_serial_jtag_driver_install(...);
    // eFuse USB_PHY_SEL = 1로 인해 PHY 접근 불가 → CRASH
#endif
}
```

**왜 sdkconfig =n인데 실행될 수 있는가?**
1. **조건부 컴파일 vs 조건부 실행**:
   ```c
   // 조건부 컴파일 (sdkconfig로 제어됨) ✅
   #ifdef CONFIG_ESP_CONSOLE_USB_SERIAL_JTAG_ENABLED
   // 이 코드는 =n이면 컴파일 안 됨
   #endif

   // 조건부 실행 (런타임에 결정됨) ❌
   if (is_usb_serial_jtag_available()) {
       // sdkconfig와 관계없이 실행될 수 있음
   }
   ```

2. **ESP-IDF의 자동 감지 로직**:
   - ESP-IDF가 런타임에 eFuse를 읽고 자동으로 USB Serial JTAG 초기화 시도
   - sdkconfig 설정을 무시하는 코드 경로 존재 가능성

#### 3.5.2 USB Serial JTAG 드라이버 (2차 의심)

**파일**: `components/driver/usb_serial_jtag/usb_serial_jtag.c`

```c
esp_err_t usb_serial_jtag_driver_install(...) {
    // USB PHY 상태 확인
    if (usb_phy_check_available()) { // 🔴 여기서 크래시?
        // eFuse USB_PHY_SEL = 1이면 PHY 사용 불가
        // 하지만 체크 로직이 eFuse를 제대로 확인하지 않을 수 있음
    }

    // PHY 초기화 시도
    usb_phy_init(...); // 🔴 또는 여기서 크래시?
    // eFuse로 인해 PHY 레지스터 접근 불가 → NULL POINTER or ASSERT
}
```

#### 3.5.3 TinyUSB 초기화 (가능성 낮음)

**파일**: `src/board/BridgeOne/main/BridgeOne.c`

```c
void app_main(void) {
    ESP_LOGI(TAG, "[BOOT STAGE 1] app_main() started"); // ❌ 출력 안 됨

    // 여기에 도달하지 못함
    ESP_LOGI(TAG, "[BOOT STAGE 2] Calling tusb_init()");
    tusb_init();
}
```

**가능성 낮은 이유**:
- app_main() 자체에 진입하지 못함
- 크래시가 ESP-IDF 시스템 초기화 단계에서 발생

#### 3.5.4 크래시 유형 추정

**유형 1: ASSERT FAILED**
```c
assert(usb_phy_available() == true);
// eFuse USB_PHY_SEL = 1이면 false → ASSERT 실패 → abort()
```

**유형 2: NULL POINTER DEREFERENCE**
```c
usb_phy_handle_t phy = get_usb_phy();
// eFuse 설정으로 인해 phy == NULL
phy->init(); // 🔴 NULL 포인터 접근 → CRASH
```

**유형 3: HARDWARE ACCESS FAULT**
```c
// USB PHY 레지스터 직접 접근
USB_SERIAL_JTAG.conf0.usb_pad_enable = 1;
// eFuse로 인해 레지스터 접근 불가 → BUS ERROR
```

---

## 4. 영향 범위

### 4.1 직접적 영향
- 🔴 **부팅 완전 불가**: 펌웨어가 app_main 초기에 크래시
- 🔴 **USB 완전 차단**: ROM 부트로더 USB도 안 됨, TinyUSB도 초기화 안 됨
- 🔴 **디버깅 불가**: 시리얼 로그 출력 없음
- 🔴 **복구 어려움**: Download Mode 진입 필요 (BOOT + RST 버튼)

### 4.2 프로젝트 영향
- 🔴 **Phase 2 목표 완전 차단**: HID 기능은 물론 기본 USB 통신도 불가능
- 🔴 **개발 중단**: 디버깅 및 테스트 불가능
- ⚠️ **보드 복구 가능**: Download Mode로 진입하면 새 펌웨어 플래시 가능

---

## 5. 검증 증거

### 5.1 eFuse 상태 확인 (시도 #4B 결과)
```bash
python %IDF_PATH%\components\esptool_py\esptool\espefuse.py --port COM3 summary
# 결과:
USB_PHY_SEL (BLOCK0):                              1 (Internal PHY → USB_OTG)
```

### 5.2 sdkconfig 검증 (Grep 결과)
```bash
grep "CONFIG_ESP_CONSOLE" sdkconfig
# 결과:
CONFIG_ESP_CONSOLE_UART=y                           ✅
CONFIG_ESP_CONSOLE_SECONDARY_USB_SERIAL_JTAG=y      ❌ (이것이 문제!)
CONFIG_ESP_CONSOLE_USB_SERIAL_JTAG_ENABLED=y        ❌ (이것이 문제!)
```

### 5.3 펌웨어 코드 검증
**BridgeOne.c:66-86**:
```c
void app_main(void) {
    ESP_LOGI(TAG, "[BOOT STAGE 1] app_main() started");

    // eFuse USB_PHY_SEL = 1 상태에서
    // 이 로그도 출력되지 않음 → 크래시가 더 일찍 발생

    ESP_LOGI(TAG, "[BOOT STAGE 2] Calling tusb_init()");
    // 이 라인에 도달하지 못함
}
```

**관찰**: `[BOOT STAGE 1]` 로그도 출력되지 않음 → 크래시가 app_main 이전 또는 초기에 발생

---

## 6. 다른 가설 검토 및 배제

### 6.1 가설: TinyUSB 초기화 실패
- **배제 이유**: TinyUSB 초기화에 도달조차 못함 (크래시가 더 일찍 발생)
- **증거**: `[BOOT STAGE 2] Calling tusb_init()` 로그 출력 안 됨

### 6.2 가설: HID 설정 문제
- **배제 이유**: HID를 완전히 비활성화했음에도 문제 지속
- **증거**: `CFG_TUD_HID = 0`, HID 디스크립터 제거, HID 태스크 비활성화

### 6.3 가설: USB PHY 초기화 문제
- **부분적으로 맞음**: eFuse 설정으로 인해 USB Serial JTAG가 PHY에 접근할 수 없음
- **하지만**: 문제는 TinyUSB가 아니라 **ESP-IDF 콘솔 초기화**에서 발생

### 6.4 가설: GPIO Pin Mux 문제
- **배제 이유**: GPIO 설정은 TinyUSB 초기화 시 자동으로 처리됨
- **증거**: 크래시가 TinyUSB 초기화 이전에 발생

---

## 7. 해결 방향 (sdkconfig 한계 도달 후)

### 🔴 중요: sdkconfig 설정만으로는 해결 불가능

**시도 #9 결과**: sdkconfig의 모든 USB Serial JTAG 설정을 =n으로 완벽히 패치했으나 여전히 부팅 실패
- CONFIG_USJ_ENABLE_USB_SERIAL_JTAG=n ✅
- CONFIG_ESP_CONSOLE_SECONDARY_USB_SERIAL_JTAG=n ✅
- CONFIG_ESP_CONSOLE_USB_SERIAL_JTAG_ENABLED=n ✅

**결론**: ESP-IDF 내부 코드 또는 펌웨어 코드에 여전히 USB Serial JTAG 관련 초기화가 존재하며, 이는 sdkconfig 설정으로 완전히 제거되지 않음

### 7.1 우선순위 1: 펌웨어 코드 검토 및 수정 (필수)

#### 7.1.1 BridgeOne 펌웨어 코드 검토

**검토 대상 파일 (우선순위 순)**:

1. **`src/board/BridgeOne/main/BridgeOne.c`** (최우선)
   - **확인 사항**: app_main() 초기화 순서
   - **찾을 코드**:
     ```c
     // USB Serial JTAG 관련 초기화 코드가 있는지 확인
     usb_serial_jtag_*();
     esp_vfs_usb_serial_jtag_*();
     ```
   - **조치**: 모든 USB Serial JTAG 초기화 코드 제거 또는 조건부 비활성화

2. **`src/board/BridgeOne/main/tusb_config.h`**
   - **확인 사항**: TinyUSB 설정이 USB Serial JTAG와 충돌하는지
   - **찾을 코드**:
     ```c
     #define CFG_TUD_CDC  // CDC 설정
     ```
   - **조치**: TinyUSB CDC와 ESP-IDF USB Serial JTAG가 동시에 사용되지 않도록 확인

3. **`src/board/BridgeOne/main/usb_descriptors.c`**
   - **확인 사항**: USB 디스크립터에 USB Serial JTAG 관련 항목이 없는지
   - **찾을 코드**: `TUD_USB_SERIAL_JTAG_DESCRIPTOR` 등
   - **조치**: 있다면 제거

4. **`src/board/BridgeOne/sdkconfig.defaults`**
   - **재확인**: 아래 설정이 명시적으로 =n인지
     ```ini
     CONFIG_USJ_ENABLE_USB_SERIAL_JTAG=n
     CONFIG_ESP_CONSOLE_SECONDARY_USB_SERIAL_JTAG=n
     CONFIG_ESP_CONSOLE_USB_SERIAL_JTAG_ENABLED=n
     ```

#### 7.1.2 ESP-IDF 내부 코드 검토 (필요 시)

**⚠️ 주의**: ESP-IDF 내부 코드 수정은 최후의 수단. 펌웨어 코드 검토 후에도 해결 안 될 경우에만 진행.

**검토 대상 (ESP-IDF v5.5 기준)**:

1. **`$IDF_PATH/components/vfs/vfs_console.c`**
   - **확인 사항**: esp_vfs_dev_uart_register() 내부에 USB Serial JTAG 초기화 코드
   - **찾을 코드**:
     ```c
     #ifdef CONFIG_ESP_CONSOLE_USB_SERIAL_JTAG_ENABLED
     usb_serial_jtag_driver_install(...);
     #endif
     ```
   - **예상**: CONFIG가 =n이므로 이 코드는 컴파일되지 않아야 함
   - **BUT**: 런타임 조건부 코드가 있을 수 있음

2. **`$IDF_PATH/components/driver/usb_serial_jtag/usb_serial_jtag.c`**
   - **확인 사항**: driver_install() 함수가 eFuse 확인을 제대로 하는지
   - **찾을 코드**:
     ```c
     esp_err_t usb_serial_jtag_driver_install(...) {
         // eFuse USB_PHY_SEL 확인 로직이 있는지?
         if (!usb_phy_available_for_serial_jtag()) {
             return ESP_ERR_NOT_SUPPORTED;
         }
     }
     ```
   - **조치**: eFuse 확인 로직이 없다면 ESP-IDF 버그 가능성

#### 7.1.3 코드 검토 방법

**검색 키워드 (BridgeOne 코드베이스)**:

**Windows PowerShell 명령어**:
```powershell
cd src\board\BridgeOne
Get-ChildItem -Path main -File -Recurse | Select-String "usb_serial_jtag"
Get-ChildItem -Path main -File -Recurse | Select-String "USB_SERIAL_JTAG"
Get-ChildItem -Path main -File -Recurse | Select-String "esp_vfs_usb_serial_jtag"
```

**예상 결과**:
- ✅ 아무것도 나오지 않아야 함 (BridgeOne 코드에서 USB Serial JTAG 사용 안 함)
- ❌ 만약 나온다면 해당 코드 제거 필요

### 7.2 우선순위 2: UART 로그 확보 (디버깅)

**목적**: 정확한 크래시 원인과 스택 트레이스 확보

**준비물**: USB-to-UART 어댑터 (CH340, CP2102, FT232 등)

**연결 방법**:
```
ESP32-S3 개발보드 → USB-to-UART 어댑터
GPIO43 (TX)      → RX (어댑터)
GPIO44 (RX)      → TX (어댑터)
GND              → GND
```

**로그 수신 절차**:
```bash
# 1. USB-to-UART 어댑터 COM 포트 확인
Get-PnpDevice | Where-Object {$_.FriendlyName -match "USB-SERIAL|CH340|CP210|FT232"}

# 2. 시리얼 터미널 연결 (115200 baud)
# 방법 A: idf.py monitor 사용
idf.py -p COM? monitor  # COM 포트는 위에서 확인된 포트로 변경

# 방법 B: PuTTY 사용
# Serial line: COM? / Speed: 115200 / Connection type: Serial

# 3. ESP32-S3 보드 리셋 (RST 버튼)

# 4. 로그 캡처
# - 모든 부팅 로그 수집
# - Guru Meditation Error 메시지 확인
# - 백트레이스 (Backtrace) 수집
```

**예상 로그 (정상 시)**:
```
ESP-ROM:esp32s3-20210327
Build:Mar 27 2021
rst:0x1 (POWERON),boot:0x8 (SPI_FAST_FLASH_BOOT)
...
I (771) BridgeOne: [BOOT STAGE 1] app_main() started
```

**예상 로그 (크래시 시)**:
```
ESP-ROM:esp32s3-20210327
Build:Mar 27 2021
...
Guru Meditation Error: Core  0 panic'ed (LoadProhibited). Exception was unhandled.
Core  0 register dump:
PC      : 0x42008abc  PS      : 0x00060030  A0      : 0x82009def  A1      : 0x3fcebff0
...
Backtrace: 0x42008abc:0x3fcebff0 0x82009def:0x3fcebc20 ...
```

**로그 분석 시 확인 사항**:
- 크래시가 어느 함수에서 발생했는지 (Backtrace)
- 크래시 유형 (LoadProhibited, StoreProhibited, IllegalInstruction 등)
- usb_serial_jtag 관련 함수 호출이 백트레이스에 있는지

### 7.3 우선순위 3: eFuse 재확인

**목적**: USB_PHY_SEL이 실제로 1로 설정되었는지 확인

**명령어**:
```bash
python %IDF_PATH%\components\esptool_py\esptool\espefuse.py --port COM7 summary

# 확인할 eFuse:
# - USB_PHY_SEL: 1이어야 함 (Internal PHY → USB_OTG)
# - 다른 USB 관련 eFuse 확인
```

**예상 결과**:
```
USB_PHY_SEL (BLOCK0):                              1
```

**만약 0이라면**: eFuse 설정이 실제로 적용되지 않음 → 다시 burn 필요
**만약 1이라면**: eFuse 설정은 정상 → 펌웨어 코드 문제 확정

### 7.4 최종 검증 기준 (해결 시)

문제가 완전히 해결되었다고 판단하려면 다음 모든 항목이 충족되어야 함:

- [ ] ✅ 펌웨어 빌드 성공 (경고 없음)
- [ ] ✅ 펌웨어 플래시 성공 (Hash verified)
- [ ] ✅ Hard reset 후 USB 연결 유지 (Windows에서 "연결 해제음 + 연결음" 연달아 들림)
- [ ] ✅ app_main() 진입 확인 (`[BOOT STAGE 1] app_main() started` 로그 출력)
- [ ] ✅ TinyUSB 초기화 성공 (`TinyUSB device stack initialized` 로그 출력)
- [ ] ✅ Windows에서 VID:PID 0x303A:0x0009 (또는 0x4001) 장치 인식
- [ ] ✅ COM 포트 할당됨 (CDC-ACM 장치)
- [ ] ✅ 시리얼 모니터로 로그 수신 가능

---

## 8. 관련 파일 목록

### 8.1 설정 파일
- `src/board/BridgeOne/sdkconfig.defaults` - ESP-IDF 기본 설정 (영구 해결 방법) ✅
- `src/board/BridgeOne/sdkconfig` - ESP-IDF 실제 설정 (reconfigure 후 자동 재생성, 직접 수정 비권장)
- `src/board/BridgeOne/main/tusb_config.h` - TinyUSB 설정

### 8.2 소스 코드
- `src/board/BridgeOne/main/BridgeOne.c` - app_main 초기화
- `src/board/BridgeOne/main/usb_descriptors.c` - USB 디스크립터

### 8.3 참고 문서
- `docs/troubleshooting/usb-hid-recognition-issue-solutions.md` - 해결 시도 기록

---

## 9. 참고 자료

### 9.1 ESP-IDF 문서
- ESP32-S3 eFuse: https://docs.espressif.com/projects/esp-idf/en/latest/esp32s3/api-reference/system/efuse.html
- USB Serial JTAG: https://docs.espressif.com/projects/esp-idf/en/latest/esp32s3/api-reference/peripherals/usb_serial_jtag.html
- Console Configuration: https://docs.espressif.com/projects/esp-idf/en/latest/esp32s3/api-guides/console.html

### 9.2 공식 기술 문서 (eFuse BURN 전 필독)
- [ESP32-S3 기술 참고서](https://docs.espressif.com/projects/esp-idf/en/latest/esp32s3/) - 섹션 21.2 "Downstream JTAG Enable Mode"
- [eFuse 프로그래밍 가이드](https://docs.espressif.com/projects/esp-idf/en/latest/esp32s3/api-reference/system/efuse.html)
- [USB_PHY_SEL 설정 가이드](https://docs.espressif.com/projects/esp-idf/en/latest/esp32s3/api-reference/peripherals/usb_serial_jtag.html)

**중요**: eFuse는 한 번 BURN하면 영구적으로 되돌릴 수 없으므로, 공식 문서를 먼저 확인하고 진행하세요.

### 9.3 관련 커뮤니티 논의
- [ESP32 Forum: JTAG Disabling (t=45328)](https://esp32.com/viewtopic.php?t=45328)
  - DIS_PAD_JTAG 및 eFuse BURN 관련 논의
  - eFuse BURN 전 공식 문서 확인의 중요성
- ESP-IDF GitHub Issues: "USB_PHY_SEL eFuse" 검색 권장
- TinyUSB GitHub Issues: "ESP32-S3 USB_OTG" 검색 권장

---

## 부록: 진단 명령어

### Download Mode 진입 방법
```
1. BOOT 버튼을 누른 상태 유지
2. RST 버튼을 눌렀다 놓기
3. BOOT 버튼 놓기
4. Windows에서 COM 포트 확인 (Download Mode: PID_0009)
5. idf.py -p [PORT] flash 실행
```

### eFuse 상태 확인
```bash
python %IDF_PATH%\components\esptool_py\esptool\espefuse.py --port COM7 summary
```

### sdkconfig 검증
```bash
grep "CONFIG_ESP_CONSOLE" sdkconfig
grep "CONFIG_USB" sdkconfig
grep "CONFIG_TINYUSB" sdkconfig
```

---

**작성자**: Claude Code (SuperClaude Framework)
**분석 방법**: Ultrathink mode - 코드 분석, 설정 검증, 크래시 메커니즘 추론
**업데이트 이력**:
- 2025-11-12 19:00: 초기 작성 (eFuse 설정 후 USB 완전 끊김 문제 정밀 분석)
- 2025-11-12 20:00: 문서 개정 (idf.py reconfigure 후 sdkconfig 복원 문제 해결)
  - 해결 방향 명확화: menuconfig 대신 sdkconfig.defaults 사용 강조
  - reconfigure 메커니즘 설명 추가
  - 방법 A/B/C 비교표 추가
- 2025-11-13 10:00: 🔴 중대한 발견 - 모든 자동화 방법 실패 + 근본 원인 재분석
  - Kconfig.projbuild, patch_sdkconfig.py, CMakeLists.txt 수정 모두 작동 안 함
  - `idf.py set-target esp32s3` 후 `reconfigure` 실행 시 sdkconfig 완전 재초기화 확인
  - ESP-IDF의 Kconfig가 우리 설정을 우선순위가 낮은 것으로 취급 가능성
  - 새로운 접근: confgen 후처리 또는 빌드 전 Python 스크립트 수동 실행 검토 필요

---

## 📊 요약

### 🔴 핵심 원인
**eFuse `USB_PHY_SEL = 1` 설정으로 내부 PHY가 USB_OTG 전용으로 전환되었지만, ESP-IDF 설정에서 `CONFIG_ESP_CONSOLE_USB_SERIAL_JTAG_ENABLED=y`가 여전히 활성화되어 있어, 부팅 시 USB Serial JTAG 초기화가 실패하면서 즉시 크래시 발생**

### ✅ 해결책 (sdkconfig.defaults 수정, 권장)
```bash
# 1. sdkconfig.defaults 파일 수정
CONFIG_USJ_ENABLE_USB_SERIAL_JTAG=n
CONFIG_ESP_CONSOLE_SECONDARY_USB_SERIAL_JTAG=n
CONFIG_ESP_CONSOLE_USB_SERIAL_JTAG_ENABLED=n

# 2. 재빌드
idf.py fullclean
idf.py reconfigure
idf.py build

# 3. Download Mode 진입 후 플래시
idf.py -p COM7 flash  # Download Mode 진입 필수
```

### ⚠️ 주의사항
- **sdkconfig.defaults에 설정 필수**: idf.py reconfigure 후에도 복원되지 않음
- **menuconfig 수정은 임시**: reconfigure 실행 시 모두 원래대로 돌아옴
- **sdkconfig 직접 수정은 비권장**: reconfigure에 의해 완전 재생성됨
- Download Mode 진입 필요 (BOOT + RST)
- 플래시 후 부팅 로그에서 `[BOOT STAGE 1]` 및 TinyUSB 초기화 성공 확인 필수

---

## 🔴 **최종 분석 결론 (2025-11-14)**

### 문제의 근본 원인 - eFuse ROM 부트로더 버그

실전 테스트 결과 sdkconfig 설정만으로는 문제를 해결할 수 **없음**이 확인되었습니다.

**이유:**
1. **ROM 부트로더는 eFuse를 읽음**: 펌웨어 로드 이전에 실행
2. **ROM 부트로더는 sdkconfig를 무시함**: eFuse 설정만 참고
3. **eFuse 충돌 감지**: `DIS_USB_JTAG = 1` + `DIS_USB_SERIAL_JTAG = 0` 조합에서 ROM 버그 발생

### 현재 eFuse 상태 (복구 불가능)

```
DIS_USB_JTAG = 1              ← 영구 설정됨
DIS_USB_SERIAL_JTAG = 0       ← 1로 변경 필요 (하지만 불가능)
USB_PHY_SEL = 1               ← 영구 설정됨

ROM 부트로더 에러:
"DIS_USB_JTAG and DIS_USB_SERIAL_JTAG cannot be
 set together due to a bug in the ROM bootloader!"
```

### 🔴 최종 결론

| 항목 | 결과 | 설명 |
|------|------|------|
| **BridgeOne 펌웨어** | ✅ 정상 | USB Serial JTAG 관련 코드 없음 |
| **sdkconfig 설정** | ✅ 정상 | USB Serial JTAG disabled 확인 |
| **ROM 부트로더** | 🔴 버그 | eFuse 충돌로 인한 크래시 발생 |
| **현재 보드** | 🔴 복구 불가 | eFuse는 영구적, 변경 불가 |

**복구 가능성:** 매우 낮음 (새 보드 필요)
