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
- [ ] Windows 인식: `VID_303A&PID_4001`
- [ ] HID 인터페이스: `MI_00` (HID 키보드), `MI_01` (HID 마우스)
- [ ] 장치 관리자: "HID 호환 마우스", "HID 키보드 장치" 표시

---

**작성일**: 2025-11-23
**작성자**: Claude (with User)
**관련 Phase**: 2.3.1.2 - ESP32-S3 → Windows PC HID 장치 인식 검증
