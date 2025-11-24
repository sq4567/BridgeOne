# USB PID 인식 문제 및 해결 시도 내역

## 문제 정의

### 현상
- **예상**: Windows에서 ESP32-S3을 `VID_303A&PID_4001` HID 복합 장치로 인식
- **실제**: Windows에서 `VID_303A&PID_1001` USB Serial/JTAG 장치로만 인식
- **누락**: HID 인터페이스 (MI_01: HID 마우스/키보드)가 완전히 없음

### 인식된 장치 (잘못된 상태)
```
FriendlyName               Status DeviceID
------------               ------ --------
USB 직렬 장치(COM10)       OK     USB\VID_303A&PID_1001&MI_00\8&332A6FC3&0&0000
USB JTAG/serial debug unit OK     USB\VID_303A&PID_1001&MI_02\8&332A6FC3&0&0002
USB Composite Device       OK     USB\VID_303A&PID_1001\DC:B4:D9:14:4E:18
```

**문제점**:
- PID가 0x1001 (ROM 부트로더의 USB Serial/JTAG)
- MI_00: USB 직렬 장치 (CDC)
- **MI_01 없음** ← HID 키보드/마우스가 있어야 함
- MI_02: USB JTAG/serial debug unit

### 기대되는 장치 (올바른 상태)
```
FriendlyName         Status DeviceID
------------         ------ --------
HID 키보드 장치      OK     USB\VID_303A&PID_4001&MI_00\...
HID 규격 마우스      OK     USB\VID_303A&PID_4001&MI_01\...
USB Composite Device OK     USB\VID_303A&PID_4001\...
```

## 근본 원인 분석

### ESP32-S3 USB 아키텍처 충돌

ESP32-S3은 두 가지 USB 기능을 지원합니다:

1. **ROM 부트로더의 USB Serial/JTAG** (PID_1001)
   - 칩 내장 기능 (ROM에 고정)
   - 디버깅 및 플래싱용
   - 기본적으로 활성화됨

2. **TinyUSB 기반 애플리케이션 USB OTG** (PID_4001)
   - 애플리케이션 레벨에서 제어
   - HID, CDC 등 복합 장치 구현
   - USB Serial/JTAG와 배타적 관계

**충돌**: ROM의 USB Serial/JTAG가 활성화되어 있으면, TinyUSB가 USB 컨트롤러를 완전히 제어할 수 없습니다.

### 시리얼 모니터 로그 분석

```
I (838) BridgeOne: USB Descriptor: VID=0x303A, PID=0x4001
I (843) BridgeOne: Interfaces: HID Keyboard(0), HID Mouse(1), CDC(2,3)
```

**펌웨어는 올바르게 PID=0x4001로 설정**되어 있지만, **Windows는 PID_1001로 인식**합니다.

이는 TinyUSB 초기화 중 ASSERT 에러와 연관됩니다:
```
check_dwc2 183: ASSERT FAILED
dwc2_core_init 221: ASSERT FAILED
dcd_init 399: ASSERT FAILED
```

→ USB 컨트롤러 초기화가 **부분적으로만 성공**했음을 의미

## 해결 시도 내역

### 시도 1: TinyUSB 드라이버 초기화 수정 (실패)

**날짜**: 2025-11-23 00:09

**문제**: `tinyusb_driver.h` 헤더가 없어서 빌드 실패

**조치**:
- `BridgeOne.c`에서 `#include "tinyusb_driver.h"` 제거
- `tinyusb_driver_install()` 호출 제거
- Native TinyUSB는 `tusb_init()`만 사용함을 확인

**결과**: 빌드 성공, 하지만 USB 인식 문제 지속

---

### 시도 2: 완전 재빌드 및 재플래시 (실패)

**날짜**: 2025-11-23 00:26

**조치**:
```bash
idf.py fullclean
idf.py build
idf.py -p COM8 flash
```

**결과**:
- 플래시 성공
- 펌웨어는 PID=0x4001로 설정됨 (시리얼 로그 확인)
- Windows는 여전히 PID_1001로 인식
- HID 인터페이스 여전히 없음

---

### 시도 3: USB Serial/JTAG 비활성화 (실패)

**날짜**: 2025-11-23 01:00

**근거**: ROM 부트로더의 USB Serial/JTAG가 TinyUSB와 충돌

**조치**: `sdkconfig.defaults`에 다음 추가
```ini
# USB Console Configuration - Disable USB Serial/JTAG to allow TinyUSB control
CONFIG_ESP_CONSOLE_UART_DEFAULT=y
CONFIG_ESP_CONSOLE_USB_SERIAL_JTAG=n
CONFIG_ESP_CONSOLE_SECONDARY_NONE=y
```

**예상 효과**:
- ESP-IDF가 UART를 콘솔로 사용
- USB Serial/JTAG 기능 비활성화
- TinyUSB가 USB 컨트롤러 완전 제어

**실행**:
```bash
idf.py fullclean
idf.py build
idf.py -p COM8 flash
```

**결과**: 실패 - 동일한 증상 지속
- Windows는 여전히 PID_1001로 인식
- HID 인터페이스 여전히 없음
- USB Serial/JTAG로 계속 인식됨

**분석**: `CONFIG_ESP_CONSOLE_USB_SERIAL_JTAG=n` 설정만으로는 불충분. ROM 레벨의 USB 기능이 여전히 활성화되어 있을 가능성

---

### 시도 4: esp_tinyusb Wrapper 마이그레이션 (✅ 성공)

**날짜**: 2025-11-23 02:30 ~ 2025-11-24 02:00

**근거**: 근본 원인 분석 문서 ([usb-pid-recognition-root-cause-analysis.md](usb-pid-recognition-root-cause-analysis.md)) 결과:
- ESP32-S3의 단일 내부 USB PHY는 USB Serial/JTAG와 USB OTG 컨트롤러 간에 배타적 전환 필요
- Native TinyUSB (`tusb_init()`)는 이 전환을 **자동으로 수행하지 않음**
- esp_tinyusb wrapper (`tinyusb_driver_install()`)가 초기화 중 자동으로 USB PHY를 USB OTG로 전환

**조치 내역**:

1. **idf_component.yml 수정**:
   ```yaml
   # Before
   espressif/tinyusb: '*'

   # After
   espressif/esp_tinyusb: "^1.0.0"
   ```

2. **BridgeOne.c 수정**:
   ```c
   // Before
   #include "tusb.h"
   ret = tusb_init();

   // After
   #include "tinyusb.h"  // esp_tinyusb wrapper 헤더
   #include "tusb.h"     // TinyUSB core 헤더

   const tinyusb_config_t tusb_cfg = {
       .device_descriptor = &desc_device,
       .string_descriptor = string_desc_arr,
       .external_phy = false,
       .configuration_descriptor = desc_configuration,
   };
   ret = tinyusb_driver_install(&tusb_cfg);
   ```

3. **usb_descriptors.c 수정**:
   - ❌ `tud_descriptor_device_cb()` 제거 (esp_tinyusb wrapper가 제공)
   - ❌ `tud_descriptor_configuration_cb()` 제거 (esp_tinyusb wrapper가 제공)
   - ❌ `tud_descriptor_string_cb()` 제거 (esp_tinyusb wrapper가 제공)
   - ✅ `tud_hid_descriptor_report_cb()` **유지** (TinyUSB가 직접 호출, wrapper 미제공)

4. **usb_descriptors.h 수정**:
   ```c
   // 디스크립터 extern 선언 추가
   extern tusb_desc_device_t const desc_device;
   extern uint8_t const desc_configuration[];
   extern char const* string_desc_arr[];
   ```

5. **sdkconfig.defaults 정리**:
   - 중복 TinyUSB 설정 제거 (esp_tinyusb가 자동 처리)
   - 모든 주석 한글 → 영문 변환
   - 핵심 설정만 유지 (HID_COUNT=2, CDC_COUNT=1)

**빌드 과정**:

- **첫 빌드**: Multiple definition 에러
  ```
  multiple definition of `tud_descriptor_device_cb'
  multiple definition of `tud_descriptor_configuration_cb'
  multiple definition of `tud_descriptor_string_cb'
  ```
  → **해결**: 디스크립터 콜백 3개 제거 (wrapper가 제공하므로)

- **재빌드**: 성공 ✅

**플래시 및 테스트 결과 (1차)**:

Windows 장치 인식:
```powershell
FriendlyName         Status DeviceID
------------         ------ --------
USB Composite Device Error  USB\VID_303A&PID_4001\DC:B4:D9:14:4E:18
```

**분석**:
- ✅ **PID_4001 인식 성공** (USB PHY 전환 작동)
- ❌ **Status: Error** (장치 열거 실패)
- ❌ **HID 인터페이스 없음** (MI_00, MI_01 누락)

**추가 조치 (2차)**:

**문제 원인**: HID Report Descriptor 콜백 누락
- esp_tinyusb wrapper는 Device/Configuration/String 디스크립터 콜백만 제공
- `tud_hid_descriptor_report_cb()`는 TinyUSB가 **직접 호출**하므로 애플리케이션이 반드시 제공해야 함

**조치**: usb_descriptors.c에 `tud_hid_descriptor_report_cb()` 복원
```c
uint8_t const* tud_hid_descriptor_report_cb(uint8_t instance) {
    if (instance == ITF_NUM_HID_KEYBOARD) {
        return desc_hid_keyboard_report;
    } else if (instance == ITF_NUM_HID_MOUSE) {
        return desc_hid_mouse_report;
    }
    return NULL;
}
```

**플래시 및 테스트 결과 (3차)**:

빌드 및 플래시 성공:
```bash
idf.py build
idf.py -p COM8 flash monitor
```

시리얼 모니터 로그:
```
I (936) tusb_desc:
│idVendor           │ 0x303a      │
│idProduct          │ 0x4001      │

I (936) TinyUSB: TinyUSB Driver installed
I (940) BridgeOne: TinyUSB driver installed (USB PHY switched to USB OTG)
I (946) UART_HANDLER: UART parameter configured: 1000000 bps, 8N1
I (1016) BridgeOne: BridgeOne USB Bridge Ready - Waiting for host connection...

W (1065) HID_HANDLER: Mouse not ready (USB disconnected or buffer full)
W (1066) HID_HANDLER: Failed to send mouse report (seq=0)
```

**분석**:
- ✅ **펌웨어 초기화 성공** (PID=0x4001, TinyUSB 드라이버 설치 완료)
- ✅ **USB PHY 전환 성공** ("USB PHY switched to USB OTG" 로그)
- ❌ **USB가 PC에 연결되지 않음** ("USB disconnected" 경고)
- ⚠️ **UART 프레임 수신 중** (Android 앱이 프레임 전송 중)

**다음 단계**:
1. **ESP32-S3 Native USB OTG 포트(포트 2)를 Windows PC에 연결**
   - 보드 상단의 USB 포트 (GPIO19/20)
   - CH343P 포트(COM8)와는 다른 포트
2. **Windows 장치 관리자에서 인식 확인**:
   ```powershell
   Get-PnpDevice -PresentOnly | Where-Object { $_.InstanceId -match 'VID_303A' } | Format-Table FriendlyName, Status, InstanceId -AutoSize
   ```
3. **인식된 PID 확인**:
   - PID_4001로 인식되는지
   - Status가 OK인지
   - HID 인터페이스(MI_00, MI_01)가 있는지

**플래시 및 테스트 결과 (4차 - 최종)**:

**문제 발견**: ASSERT 에러 발생
```
E (1145) TinyUSB: process_set_config 1127: ASSERT FAILED
E (1145) TinyUSB: process_control_request 860: ASSERT FAILED
```

**근본 원인 분석**:
- TinyUSB 소스 코드 분석: `components/espressif__tinyusb/src/device/usbd.c:1127`
- ASSERT 지점: `TU_ASSERT(drv_id < TOTAL_DRIVER_COUNT)` - 인터페이스에 대한 드라이버를 찾지 못함
- Configuration Descriptor는 CDC 인터페이스(ITF_NUM_CDC_COMM, ITF_NUM_CDC_DATA) 정의
- 하지만 sdkconfig에서 CDC 드라이버가 비활성화되어 있음: `# CONFIG_TINYUSB_CDC_ENABLED is not set`

**해결 방법**:
1. **sdkconfig.defaults 수정**:
   ```ini
   # Before (WRONG)
   CONFIG_TINYUSB_CDC_COUNT=1

   # After (CORRECT)
   CONFIG_TINYUSB_CDC_ENABLED=y
   ```
   **설명**: esp_tinyusb wrapper는 `CONFIG_TINYUSB_CDC_ENABLED` 플래그로 CDC 드라이버를 활성화합니다. 단순히 COUNT만 설정해서는 드라이버가 컴파일되지 않습니다.

2. **재빌드 및 플래시**:
   ```bash
   idf.py fullclean
   idf.py reconfigure
   idf.py build
   idf.py -p COM8 flash monitor
   ```

**최종 결과**: ✅ **완전 성공**

시리얼 모니터 로그:
```
I (936) tusb_desc:
│idVendor           │ 0x303a      │
│idProduct          │ 0x4001      │

I (936) TinyUSB: TinyUSB Driver installed
I (940) BridgeOne: TinyUSB driver installed (USB PHY switched to USB OTG)

CDC init
HID init
Set Configuration 1
Open EP 81 with Size = 64       # HID Keyboard EP
HID opened
Open EP 82 with Size = 64       # HID Mouse EP
HID opened
Open EP 83 with Size = 8        # CDC Notification EP
Open EP 4 with Size = 64        # CDC Data OUT EP
Open EP 84 with Size = 64       # CDC Data IN EP
CDC opened
```

**분석**:
- ✅ **모든 ASSERT 에러 사라짐**
- ✅ **CDC 인터페이스 정상 초기화** ("CDC init" → "CDC opened")
- ✅ **HID 인터페이스 정상 초기화** ("HID init" → "HID opened" × 2)
- ✅ **모든 엔드포인트 정상 오픈** (EP 81, 82, 83, 4, 84)

Windows 장치 관리자 인식 결과:
```powershell
FriendlyName         Status InstanceId
------------         ------ ----------
HID 규격 마우스      OK     HID\VID_303A&PID_4001&MI_01\7&3A91D35C&0&0000
USB 입력 장치        OK     USB\VID_303A&PID_4001&MI_00\8&1AB2F213&0&0000
USB 입력 장치        OK     USB\VID_303A&PID_4001&MI_01\8&1AB2F213&0&0001
USB 직렬 장치(COM3)  OK     USB\VID_303A&PID_4001&MI_02\8&1AB2F213&0&0002
HID 키보드 장치      OK     HID\VID_303A&PID_4001&MI_00\7&3A91D35C&0&0000
USB Composite Device OK     USB\VID_303A&PID_4001\00000001
```

**분석**:
- ✅ **PID_4001 인식 성공** (USB PHY 전환 완료)
- ✅ **모든 인터페이스 인식 성공**:
  - MI_00: HID 키보드
  - MI_01: HID 마우스
  - MI_02: CDC 직렬 포트 (COM3)
- ✅ **모든 장치 Status: OK** (에러 없음)
- ✅ **HID 클래스 드라이버 로드 성공** ("HID 규격 마우스", "HID 키보드 장치")

**성공 요인 요약**:
1. **esp_tinyusb wrapper 사용** → USB PHY 자동 전환 (USB Serial/JTAG → USB OTG)
2. **CDC 드라이버 활성화** → Configuration Descriptor와 드라이버 매칭 성공
3. **올바른 디스크립터 구성** → HID Report Descriptor 콜백 제공

---

## 기술적 배경

### ESP32-S3 USB 포트 구조

**YD-ESP32-S3 N16R8 보드**:
- **포트 1 (COM 포트)**: CH343P USB-to-UART 브릿지
  - VID_1A86&PID_55D3
  - 플래싱 및 시리얼 모니터링용
  - UART0 (GPIO43/44)에 연결

- **포트 2 (USB 포트)**: ESP32-S3 Native USB OTG
  - VID_303A&PID_xxxx (애플리케이션에서 설정)
  - HID 통신용 (목표)
  - GPIO19 (D-), GPIO20 (D+)

### TinyUSB 구성

**현재 설정** (`usb_descriptors.h`):
```c
#define USB_VID             0x303A  // Espressif
#define USB_PID             0x4001  // BridgeOne

// Interface 순서 (변경 금지)
ITF_NUM_HID_KEYBOARD = 0,  // Interface 0: HID Boot Keyboard
ITF_NUM_HID_MOUSE    = 1,  // Interface 1: HID Boot Mouse
ITF_NUM_CDC_COMM     = 2,  // Interface 2: CDC-ACM Communication
ITF_NUM_CDC_DATA     = 3,  // Interface 3: CDC-ACM Data
```

**Configuration Descriptor**:
- TUD_HID_DESCRIPTOR (Keyboard, Interface 0)
- TUD_HID_DESCRIPTOR (Mouse, Interface 1)
- TUD_CDC_DESCRIPTOR (CDC, Interface 2-3)

### USB Serial/JTAG vs TinyUSB

| 기능 | USB Serial/JTAG (ROM) | TinyUSB (애플리케이션) |
|------|----------------------|----------------------|
| PID | 0x1001 | 0x4001 (설정 가능) |
| 용도 | 디버깅, 플래싱 | HID, CDC, MSC 등 |
| 제어 | ROM 부트로더 | 애플리케이션 코드 |
| 우선순위 | 기본 활성화 | 명시적 비활성화 필요 |

## 참고 문서

- [CLAUDE.md - YD-ESP32-S3 N16R8 USB 포트 정의](../../CLAUDE.md)
- [ESP-IDF USB Device Stack 공식 문서](https://docs.espressif.com/projects/esp-idf/en/stable/esp32s3/api-reference/peripherals/usb_device.html)
- [TinyUSB 설정 가이드](../board/TinyUSB-configuration-guide.md) (작성 예정)

## 추가 조사 필요 사항

### 1. menuconfig 전체 USB 설정 확인
```bash
idf.py menuconfig
```
확인할 항목:
- Component config → TinyUSB Stack
- Component config → USB-OTG
- Component config → ESP System Settings → Channel for console output

### 2. ESP-IDF 공식 TinyUSB 예제 확인
- `examples/peripherals/usb/device/tusb_hid/` 예제와 비교
- 예제의 sdkconfig와 현재 설정 차이 분석

### 3. USB PHY 설정 검토
- Internal USB PHY vs External PHY
- GPIO19/20 핀 설정 확인

### 4. eFuse 설정 확인
```bash
python %IDF_PATH%\components\esptool_py\esptool\espefuse.py --port COM8 summary
```
USB 관련 eFuse 비트 확인

### 5. 하드웨어 검증
- 다른 ESP32-S3 보드에서 테스트
- USB 케이블 교체 시도
- 다른 Windows PC에서 테스트

## 다음 단계 우선순위

**우선순위 1**: menuconfig에서 USB 관련 설정 전체 점검
**우선순위 2**: ESP-IDF 공식 TinyUSB HID 예제 참조 및 비교
**우선순위 3**: eFuse 및 하드웨어 검증

## 성공 기준

다음 조건이 모두 충족되어야 함:
- [x] 펌웨어 로그: `USB Descriptor: VID=0x303A, PID=0x4001`
- [x] Windows 인식: `VID_303A&PID_4001`
- [x] HID 인터페이스: `MI_00` (HID 키보드), `MI_01` (HID 마우스)
- [x] 장치 관리자: "HID 호환 마우스", "HID 키보드 장치" 표시

**✅ 모든 성공 기준 충족 (2025-11-24)**

---

**작성일**: 2025-11-23
**작성자**: Claude (with User)
**관련 Phase**: 2.3.1.2 - ESP32-S3 → Windows PC HID 장치 인식 검증
