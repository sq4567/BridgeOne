# ESP32-S3 USB HID 인식 문제 - 원인 분석 문서

## 문서 정보
- **작성일**: 2025-11-10
- **문제 발생일**: 2025-11-10
- **프로젝트**: BridgeOne ESP32-S3 펌웨어
- **심각도**: 🔴 CRITICAL (핵심 기능 완전 차단)
- **상태**: ⚠️ 진행 중 (TinyUSB 초기화 성공, Windows 인식 대기)
- **마지막 업데이트**: 2025-11-10 17:30

---

## 1. 문제 현상

### 1.1 증상
ESP32-S3을 Windows PC에 USB로 연결 시, 다음과 같이 인식됨:

```powershell
FriendlyName               Status DeviceID
------------               ------ --------
USB JTAG/serial debug unit OK     USB\VID_303A&PID_1001&MI_02\9&783726D&1&0002
USB 직렬 장치(COM3)        OK     USB\VID_303A&PID_1001&MI_00\9&783726D&1&0000
USB Composite Device       OK     USB\VID_303A&PID_1001\24:58:7C:DE:81:C8
```

**기대 동작**: HID Keyboard 및 HID Mouse 장치로 인식되어야 함
**실제 동작**: USB JTAG/serial debug unit 및 USB 직렬 장치(CDC)로만 인식됨

### 1.2 추가 증상
- `idf.py menuconfig`에서 "Component config → TinyUSB Stack" 메뉴가 표시되지 않음
- 이는 TinyUSB 컴포넌트 자체가 활성화되지 않았을 가능성을 시사함

### 1.3 VID/PID 분석
- **현재 인식된 VID/PID**: 0x303A:0x1001
  - 0x303A: Espressif Systems 공식 VID
  - 0x1001: ESP32-S3 내장 USB JTAG/CDC 장치 (ROM 부트로더 기본 PID)

- **의도한 VID/PID**: 0x303A:0x4001
  - 코드에서 정의된 BridgeOne 커스텀 PID
  - `usb_descriptors.h:25-26` 참조

**결론**: ESP32-S3가 TinyUSB 스택을 사용하지 않고, 내장 ROM 부트로더의 기본 USB 장치로 동작하고 있음

---

## 2. 코드 및 설정 분석

### 2.1 소스 코드 검토

#### 2.1.1 USB 디스크립터 (`usb_descriptors.c`)
- ✅ **정상**: Device Descriptor 정의 완료 (VID=0x303A, PID=0x4001)
- ✅ **정상**: HID Keyboard + Mouse + CDC 복합 장치 Configuration Descriptor 정의
- ✅ **정상**: HID Report Descriptor 정의 (Boot Protocol 준수)
- ✅ **정상**: String Descriptor 정의 ("BridgeOne USB Bridge")

#### 2.1.2 메인 초기화 코드 (`BridgeOne.c:66-82`)
```c
esp_err_t ret = tusb_init();
if (ret != ESP_OK) {
    ESP_LOGE(TAG, "TinyUSB initialization failed: %s", esp_err_to_name(ret));
    return;
}
ESP_LOGI(TAG, "TinyUSB device stack initialized");
```
- ✅ **정상**: `tusb_init()` 호출 및 에러 처리 구현

#### 2.1.3 CMakeLists.txt (`main/CMakeLists.txt:1-13`)
```cmake
idf_component_register(
    SRCS
        "BridgeOne.c"
        "usb_descriptors.c"
        "hid_handler.c"
        "uart_handler.c"
    INCLUDE_DIRS "."
    REQUIRES
        tinyusb
        freertos
        driver
        esp_timer
)
```
- ✅ **정상**: `tinyusb` 컴포넌트를 REQUIRES에 명시적으로 포함

#### 2.1.4 TinyUSB 설정 파일 (`tusb_config.h`)
- ✅ **정상**: `CFG_TUD_ENABLED = 1` (Device 모드 활성화)
- ✅ **정상**: `CFG_TUD_HID = 1` (HID 클래스 활성화)
- ✅ **정상**: `CFG_TUD_CDC = 1` (CDC 클래스 활성화)
- ✅ **정상**: `CFG_TUSB_OS = OPT_OS_FREERTOS` (FreeRTOS 통합)

### 2.2 ESP-IDF 설정 검토

#### 2.2.1 sdkconfig.defaults
```ini
CONFIG_TINYUSB=y
CONFIG_TINYUSB_HID_ENABLED=y
CONFIG_TINYUSB_CDC_ENABLED=y
CONFIG_TINYUSB_DEVICE_ENABLED=y
CONFIG_TINYUSB_DEVICE_BUS_POWERED=y
CONFIG_TINYUSB_HID_COUNT=2
CONFIG_TINYUSB_HID_BUFSIZE=64
CONFIG_TINYUSB_CDC_COUNT=1
CONFIG_TINYUSB_CDC_RX_BUFSIZE=512
CONFIG_TINYUSB_CDC_TX_BUFSIZE=512
```
- ✅ **정상**: TinyUSB 관련 모든 설정이 명시적으로 정의됨

#### 2.2.2 실제 sdkconfig 파일
```bash
# Grep 결과: CONFIG_TINYUSB 패턴이 하나도 없음
$ grep "CONFIG_TINYUSB" sdkconfig
(결과 없음)
```
- ❌ **문제 발견**: `sdkconfig.defaults`의 설정이 실제 `sdkconfig`에 전혀 반영되지 않음

---

## 3. 근본 원인 분석

### 3.1 🔴 발견된 핵심 문제
**Kconfig 심볼이 인식되지 않음** - TinyUSB 컴포넌트의 Kconfig 설정이 ESP-IDF의 빌드 시스템에 등록되지 않음

### 3.2 원인 가설 (업데이트됨)

#### 가설 0: TinyUSB Kconfig 심볼 미등록 ⭐⭐⭐⭐⭐ (최우선)
- **가능성**: ⭐⭐⭐⭐⭐ (확정)
- **증거 (터미널 출력)**:
  ```
  warning: unknown kconfig symbol 'TINYUSB' assigned to 'y'
  warning: unknown kconfig symbol 'TINYUSB_HID_ENABLED' assigned to 'y'
  warning: unknown kconfig symbol 'TINYUSB_CDC_ENABLED' assigned to 'y'
  warning: unknown kconfig symbol 'TINYUSB_DEVICE_ENABLED' assigned to 'y'
  warning: unknown kconfig symbol 'TINYUSB_DEVICE_BUS_POWERED' assigned to 'y'
  warning: unknown kconfig symbol 'TINYUSB_HID_COUNT' assigned to '2'
  warning: unknown kconfig symbol 'TINYUSB_HID_BUFSIZE' assigned to '64'
  warning: unknown kconfig symbol 'TINYUSB_CDC_COUNT' assigned to '1'
  warning: unknown kconfig symbol 'TINYUSB_CDC_RX_BUFSIZE' assigned to '512'
  warning: unknown kconfig symbol 'TINYUSB_CDC_TX_BUFSIZE' assigned to '512'
  ```
- **근거**:
  - `idf.py menuconfig` 실행 시 TinyUSB 컴포넌트는 로드됨 (`NOTICE: Processing 2 dependencies...`)
  - 하지만 TinyUSB의 Kconfig 설정이 인식되지 않음
  - `sdkconfig.defaults`의 모든 TinyUSB 설정이 "unknown kconfig symbol" 경고 발생
  - TinyUSB 컴포넌트는 로드되었으나, 그 Kconfig 정의가 빌드 시스템에 병합되지 않음
- **원인**:
  - TinyUSB 컴포넌트의 `Kconfig` 또는 `Kconfig.projbuild` 파일이 누락되었을 가능성
  - 또는 TinyUSB 컴포넌트의 CMakeLists.txt에서 Kconfig 파일을 등록하지 않음
- **검증 방법**:
  - TinyUSB 컴포넌트 디렉토리에서 `Kconfig` 또는 `Kconfig.projbuild` 파일 존재 여부 확인
  - TinyUSB CMakeLists.txt 검토

#### 가설 1: sdkconfig.defaults가 무시됨 (부분적으로 해결됨)
- **가능성**: ⭐⭐ (부분 해결)
- **업데이트**: `idf.py set-target esp32s3` 후 `sdkconfig.defaults`는 로드되고 있음
  - "Loading defaults file" 메시지 확인
  - 하지만 TinyUSB 심볼이 인식되지 않아 경고만 발생

#### 가설 2: ESP-IDF TinyUSB 컴포넌트 미설치 (해결됨)
- **가능성**: ⭐ (해결됨)
- **근거**: TinyUSB 컴포넌트가 정상 로드됨
  - `NOTICE: [1/2] espressif/tinyusb (0.19.0~1)`
  - 로컬 컴포넌트 경로: `F:\...\components\espressif__tinyusb`

#### 가설 3: USB 초기화 실패 (여전히 가능)
- **가능성**: ⭐⭐ (가능성 높음)
- **근거**: Kconfig 심볼이 인식되지 않으면 TinyUSB 설정을 적용할 수 없음

#### 가설 4: USB 설정 충돌 (여전히 가능)
- **가능성**: ⭐⭐ (가능성 중간)
- **근거**: 기본 설정은 UART0 선택됨

---

## 4. 영향 범위

### 4.1 직접적 영향
- ❌ TinyUSB Kconfig 심볼 미등록 → menuconfig에서 TinyUSB 설정 불가능
- ❌ TinyUSB 설정 적용 불가 → HID Keyboard/Mouse 기능 비활성화
- ❌ HID 기능 미작동 → Windows/Linux PC에서 마우스/키보드로 인식 불가
- ❌ Android 앱에서 ESP32-S3로 마우스 입력 전송 불가능
- ⚠️ CDC 시리얼 통신은 ROM 부트로더 기본 기능이므로 일부 작동 가능 (하지만 설정되지 않음)

### 4.2 프로젝트 영향
- 🔴 **Phase 2 목표 달성 불가**: Android → ESP32-S3 → PC HID 입력 전달 체인 완전 차단
- 🔴 **테스트 불가**: HID 기능을 테스트할 수 없어 모든 후속 개발 차단
- 🟡 **UART 통신은 독립적**: Android ↔ ESP32-S3 UART 통신은 영향 없음

---

## 5. 관련 파일 목록

### 5.1 소스 코드
- `src/board/BridgeOne/main/BridgeOne.c` - 메인 초기화 코드
- `src/board/BridgeOne/main/usb_descriptors.c` - USB 디스크립터 정의
- `src/board/BridgeOne/main/usb_descriptors.h` - USB 디스크립터 헤더
- `src/board/BridgeOne/main/tusb_config.h` - TinyUSB 설정

### 5.2 빌드 설정
- `src/board/BridgeOne/CMakeLists.txt` - 프로젝트 CMake 설정
- `src/board/BridgeOne/main/CMakeLists.txt` - 컴포넌트 CMake 설정
- `src/board/BridgeOne/sdkconfig.defaults` - 기본 ESP-IDF 설정
- `src/board/BridgeOne/sdkconfig` - 실제 ESP-IDF 설정 (문제 발생)
- `src/board/BridgeOne/sdkconfig.old` - 이전 ESP-IDF 설정 백업

### 5.3 참고 문서
- `docs/board/esp32s3-code-implementation-guide.md` - ESP32-S3 구현 가이드

---

## 6. 다음 단계

### 6.1 즉시 시도할 해결책 (우선순위 순) - ⭐ 업데이트됨

1. **🔴 TinyUSB Kconfig 파일 확인 (가설 0 검증)** - 최우선
   - TinyUSB 컴포넌트 디렉토리에서 `Kconfig` 또는 `Kconfig.projbuild` 파일 존재 여부 확인
   - 파일이 없다면 생성 필요
   - TinyUSB CMakeLists.txt에서 Kconfig 등록 여부 확인
   - **명령어**:
     ```bash
     ls components/espressif__tinyusb/Kconfig*
     ls components/espressif__tinyusb/CMakeLists.txt
     ```

2. **sdkconfig 재생성** (가설 1 검증 - 이미 부분 진행)
   - ✅ `idf.py set-target esp32s3` 완료
   - 현재 상태: TinyUSB Kconfig 심볼 미등록으로 인해 설정 적용 불가

3. **USB 설정 충돌 확인** (가설 4 검증)
   - 현재: `CONFIG_ESP_CONSOLE_UART` (UART0) 선택 - ✅ 올바름
   - USB Serial/JTAG 미활성화 상태

4. **TinyUSB 컴포넌트 Kconfig 수정** (가설 0 해결)
   - Kconfig 파일 생성 또는 수정 필요
   - CMakeLists.txt 확인 및 필요 시 수정

### 6.2 검증 기준
각 해결 시도 후 다음 사항 확인:
- [ ] `idf.py menuconfig`에서 "Component config → TinyUSB Stack" 메뉴 표시
- [ ] Windows 장치 관리자에서 VID:PID가 0x303A:0x4001로 표시
- [ ] "BridgeOne USB Bridge" 또는 "HID Keyboard/Mouse" 이름으로 인식
- [ ] HID 장치가 별도로 열거됨 (Keyboard, Mouse 각각)

---

## 7. 참고 자료

### 7.1 ESP-IDF 문서
- ESP-IDF TinyUSB 컴포넌트: https://docs.espressif.com/projects/esp-idf/en/latest/esp32s3/api-reference/peripherals/usb_device.html
- ESP-IDF Configuration: https://docs.espressif.com/projects/esp-idf/en/latest/esp32s3/api-reference/kconfig.html

### 7.2 TinyUSB 문서
- TinyUSB Configuration: https://github.com/hathach/tinyusb/blob/master/docs/reference/getting_started.rst

### 7.3 관련 이슈
- ESP-IDF GitHub Issues: TinyUSB 관련 이슈 검색 필요

---

## 부록: 진단 명령어

### Windows PowerShell
```powershell
# USB 장치 확인
Get-PnpDevice | Where-Object {$_.DeviceID -match "VID_303A"} | Format-Table FriendlyName, Status, DeviceID

# 상세 정보 확인
Get-PnpDevice | Where-Object {$_.DeviceID -match "VID_303A"} | Get-PnpDeviceProperty
```

### ESP-IDF (Windows)
```bash
# 포트 확인
idf.py list-ports

# 설정 확인
idf.py menuconfig

# 빌드 클린
idf.py fullclean

# 재설정
idf.py reconfigure

# 빌드
idf.py build

# 플래시 및 모니터
idf.py -p COM3 flash monitor
```

### Grep 검색
```bash
# sdkconfig에서 TinyUSB 설정 확인
grep "CONFIG_TINYUSB" sdkconfig

# sdkconfig에서 USB 콘솔 설정 확인
grep "CONFIG_ESP_CONSOLE" sdkconfig

# sdkconfig에서 USB 관련 모든 설정 확인
grep "CONFIG_USB" sdkconfig
```

---

**작성자**: Claude Code (SuperClaude Framework)
**업데이트 이력**:
- 2025-11-10: 초기 작성
- 2025-11-10 17:30: 해결 진행 상황 업데이트 (시도 #1-3 완료)

---

## 📊 현재 진행 상황 (2025-11-10 17:30 기준)

### ✅ 완료된 작업
1. **TinyUSB Kconfig 파일 생성** (시도 #1)
   - 파일: `components/espressif__tinyusb/Kconfig`
   - 상태: ✅ 완료
   - menuconfig "Component config → TinyUSB Stack" 메뉴 정상 표시

2. **menuconfig 설정** (시도 #2)
   - HID Keyboard/Mouse 활성화
   - CDC Serial 활성화
   - 상태: ✅ 완료

3. **펌웨어 빌드 및 플래시** (시도 #3)
   - 빌드: ✅ 성공
   - 플래시: ✅ 성공
   - TinyUSB 초기화: ✅ 성공 ("TinyUSB device stack initialized" 로그 출력)
   - USB Descriptor: ✅ VID=0x303A, PID=0x4001

### ⚠️ 진행 중인 작업
- **Windows HID 장치 인식** (시도 #4 대기)
  - 현재 상태: Windows가 여전히 ROM 부트로더 PID_1001 표시
  - 기대 상태: HID Keyboard, HID Mouse 등으로 표시
  - 다음 단계: USB JTAG/CDC 설정 변경 필요

### 📋 다음 단계 (시도 #4)
```
1. sdkconfig 확인
   - CONFIG_ESP_CONSOLE_UART=y (현재 상태)
   - CONFIG_USB_JTAG_CDC_ENABLED=n (변경 필요?)

2. menuconfig에서 USB 콘솔 설정 확인
   → Component config → ESP System Settings → Channel for console output

3. USB JTAG 비활성화 시도
   → Component config → Hardware Settings → USB Serial/JTAG

4. 빌드/플래시/재테스트
```
