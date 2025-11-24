# ESP32-S3 USB PID 인식 문제 근본 원인 분석

**작성일**: 2025-11-23
**분석자**: Claude (with User)
**관련 이슈**: [usb-pid-recognition-issue.md](usb-pid-recognition-issue.md)

---

## 요약

ESP32-S3이 TinyUSB HID 복합 장치(PID_4001)가 아닌 USB Serial/JTAG(PID_1001)로 인식되는 문제의 **근본 원인**은 **USB PHY가 USB OTG 컨트롤러로 전환되지 않았기 때문**입니다.

현재 프로젝트는 **Native TinyUSB**를 사용하고 있으나, ESP-IDF의 공식 예제는 **`esp_tinyusb` wrapper**를 사용합니다. 이 wrapper가 USB PHY 전환을 자동으로 처리하는데, Native TinyUSB만 사용할 경우 이 과정이 누락됩니다.

---

## ESP32-S3 USB 아키텍처 분석

### 1. 하드웨어 구조의 근본적 제약

ESP32-S3에는 **단 하나의 내부 USB PHY**만 존재하며, 이 PHY는 두 개의 USB 컨트롤러 중 하나에만 연결될 수 있습니다:

```
┌─────────────────────────────────────────┐
│         ESP32-S3 USB 아키텍처          │
├─────────────────────────────────────────┤
│                                         │
│  ┌─────────────┐    ┌───────────────┐  │
│  │USB Serial/  │    │  USB OTG      │  │
│  │JTAG         │◄───┤  Controller   │  │
│  │Controller   │ ❌ │               │  │
│  │(ROM 내장)   │    │  (TinyUSB)    │  │
│  └─────────────┘    └───────────────┘  │
│         │                    │          │
│         └────────┬───────────┘          │
│                  │                      │
│            ┌─────▼─────┐                │
│            │  Internal │  ◄─ 단 하나!  │
│            │  USB PHY  │                │
│            └─────┬─────┘                │
│                  │                      │
│              GPIO19/20                  │
│              (USB D+/D-)                │
└─────────────────────────────────────────┘
```

**핵심 제약사항**:
- **배타적 사용**: USB Serial/JTAG와 USB OTG는 **동시에 사용할 수 없음**
- **기본 연결**: 부팅 시 PHY는 **USB Serial/JTAG에 기본 연결**됨
- **전환 필요**: USB OTG 사용을 위해서는 **명시적으로 PHY를 전환**해야 함

### 2. 현재 상태 분석

#### 부팅 시퀀스

```
Boot ROM
  ↓
USB PHY → USB Serial/JTAG (PID_1001) ← 기본 연결
  ↓
App 시작
  ↓
tusb_init() 호출 ← Native TinyUSB 초기화
  ↓
??? (USB PHY 전환 누락) ???
  ↓
Windows: "아직도 PID_1001이네?" ← 인식 실패
```

#### 시리얼 로그 분석 결과

```
I (838) BridgeOne: USB Descriptor: VID=0x303A, PID=0x4001  ← 펌웨어는 올바름
I (843) BridgeOne: Interfaces: HID Keyboard(0), HID Mouse(1), CDC(2,3)

check_dwc2 183: ASSERT FAILED        ← USB 컨트롤러 접근 실패
dwc2_core_init 221: ASSERT FAILED    ← PHY가 USB OTG에 연결되지 않음
dcd_init 399: ASSERT FAILED          ← Device Controller 초기화 실패
```

**결론**: TinyUSB가 USB OTG 컨트롤러를 초기화하려 했으나, **USB PHY가 여전히 USB Serial/JTAG에 연결**되어 있어 실패했습니다.

---

## 근본 원인: Native TinyUSB vs esp_tinyusb Wrapper

### 현재 프로젝트 (Native TinyUSB)

**구조**:
```c
// BridgeOne.c
tusb_init();  // Native TinyUSB API 직접 호출
```

**컴포넌트 의존성**:
```yaml
# idf_component.yml
dependencies:
  espressif/tinyusb: "*"  # Native TinyUSB만 사용
```

**문제점**:
- ❌ USB PHY 전환 로직 **없음**
- ❌ ESP-IDF 시스템과의 통합 **부족**
- ❌ USB Serial/JTAG 비활성화 **불완전**

### ESP-IDF 공식 예제 (esp_tinyusb Wrapper)

**구조**:
```c
// tusb_hid_example_main.c
tinyusb_config_t tusb_cfg = TINYUSB_DEFAULT_CONFIG();
tinyusb_driver_install(&tusb_cfg);  // Wrapper API 사용
```

**컴포넌트 의존성**:
```yaml
dependencies:
  espressif/esp_tinyusb: "*"  # Wrapper 사용
  espressif/tinyusb: "*"      # 내부적으로 Native 사용
```

**장점**:
- ✅ **USB PHY 자동 전환**: `tinyusb_driver_install()` 내부에서 처리
- ✅ **ESP-IDF 통합**: 시스템 초기화 순서 보장
- ✅ **USB Serial/JTAG 자동 비활성화**: 런타임에 처리

---

## 공식 문서 증거

### 1. USB OTG Console 가이드

출처: [ESP-IDF USB OTG Console - ESP32-S3](https://docs.espressif.com/projects/esp-idf/en/stable/esp32s3/api-guides/usb-otg-console.html)

> **"During protocol stack initialization, the USB-PHY connection will automatically switch to USB-OTG, and users do not need to perform additional configuration."**

**해석**: `esp_tinyusb` wrapper를 사용하면 **자동으로** PHY가 전환됩니다.

### 2. USB Serial/JTAG 비활성화 한계

출처: [ESP-IDF USB Serial/JTAG Controller Console](https://docs.espressif.com/projects/esp-idf/en/stable/esp32s3/api-guides/usb-serial-jtag-console.html)

> **"The USB Serial/JTAG Controller is a fixed function device, implemented entirely in hardware. This means it cannot be reconfigured to perform any function other than to provide a serial channel and JTAG debugging functionality."**

**해석**: `CONFIG_ESP_CONSOLE_USB_SERIAL_JTAG=n` 설정만으로는 **USB Serial/JTAG 컨트롤러 자체를 비활성화할 수 없습니다**. 이는 **콘솔 출력만** 비활성화합니다.

### 3. TinyUSB와의 비호환성

출처: [ESP-IDF USB OTG Console - TinyUSB](https://docs.espressif.com/projects/esp-idf/en/stable/esp32s3/api-guides/usb-otg-console.html)

> **"At the moment, this 'USB Console' feature is incompatible with TinyUSB stack."**

**해석**: ESP-IDF의 기본 USB Console 기능(`CONFIG_ESP_CONSOLE_USB_CDC=y`)과 TinyUSB는 **동시에 사용할 수 없습니다**.

### 4. eFuse Burning (영구적 전환)

출처: [ESP-IoT-Solution USB PHY Introduction](https://docs.espressif.com/projects/esp-iot-solution/en/latest/usb/usb_overview/usb_phy.html)

> **"You can permanently switch the internal USB PHY to work with USB OTG peripheral instead of USB_SERIAL_JTAG by burning USB_PHY_SEL eFuse."**

**주의**: 이 방법은 **영구적**이며, 한 번 burning하면 되돌릴 수 없습니다. USB Serial/JTAG를 **다시 사용할 수 없게** 됩니다.

---

## 해결 방안 비교

### 방안 A: esp_tinyusb Wrapper 사용 (권장 ✅)

**장점**:
- ✅ USB PHY 자동 전환 (코드 수정 불필요)
- ✅ ESP-IDF 시스템과 완벽한 통합
- ✅ 공식 지원 및 예제 풍부
- ✅ 추가 sdkconfig 설정 불필요
- ✅ 가역적 (펌웨어 변경만으로 되돌릴 수 있음)

**단점**:
- ⚠️ 기존 코드 일부 수정 필요 (`tusb_init()` → `tinyusb_driver_install()`)
- ⚠️ `idf_component.yml`에 `esp_tinyusb` 의존성 추가

**구현 복잡도**: 중간
**예상 소요 시간**: 1-2시간

---

### 방안 B: Native TinyUSB + 수동 PHY 전환 (고급)

**장점**:
- ✅ Native TinyUSB 유지 가능
- ✅ 세밀한 제어 가능

**단점**:
- ❌ USB PHY 전환 코드 직접 구현 필요
- ❌ ESP-IDF 내부 API 사용 필요 (비공개 API 위험)
- ❌ 향후 ESP-IDF 버전 업그레이드 시 호환성 문제 가능
- ❌ 공식 지원 없음

**구현 복잡도**: 높음
**예상 소요 시간**: 4-6시간 (디버깅 포함)

---

### 방안 C: eFuse Burning (비권장 ❌)

**장점**:
- ✅ 영구적 해결
- ✅ 부팅 시 자동 PHY 전환

**단점**:
- ❌ **영구적** - 되돌릴 수 없음
- ❌ USB Serial/JTAG 완전히 사용 불가 (디버깅 어려움)
- ❌ 플래싱 시 UART만 사용 가능
- ❌ 보드 재활용 불가

**구현 복잡도**: 낮음 (단, 위험성 높음)
**권장 여부**: **강력히 비권장**

---

## 권장 해결 방안: esp_tinyusb Wrapper 마이그레이션

### 1. 변경 사항 요약

| 항목 | 현재 (Native) | 변경 후 (Wrapper) |
|------|---------------|-------------------|
| **컴포넌트** | `espressif/tinyusb` | `espressif/esp_tinyusb` |
| **초기화 함수** | `tusb_init()` | `tinyusb_driver_install(&config)` |
| **USB PHY 전환** | ❌ 수동 필요 | ✅ 자동 처리 |
| **sdkconfig** | 복잡 | 간단 |

### 2. 필요한 코드 변경

#### idf_component.yml
```yaml
# 변경 전
dependencies:
  espressif/tinyusb: "*"

# 변경 후
dependencies:
  espressif/esp_tinyusb: "^1.0.0"
```

#### BridgeOne.c
```c
// 변경 전
#include "tusb.h"
tusb_init();

// 변경 후
#include "tinyusb.h"
tinyusb_config_t tusb_cfg = {
    .device_descriptor = NULL,  // 기본 디스크립터 사용
    .string_descriptor = NULL,
    .external_phy = false,      // 내부 PHY 사용
    .configuration_descriptor = NULL,
};
ESP_ERROR_CHECK(tinyusb_driver_install(&tusb_cfg));
```

### 3. sdkconfig.defaults 간소화

```ini
# 변경 전 (복잡)
CONFIG_TINYUSB=y
CONFIG_TINYUSB_HID_ENABLED=y
CONFIG_TINYUSB_CDC_ENABLED=y
CONFIG_ESP_CONSOLE_USB_SERIAL_JTAG=n
# ... 많은 설정

# 변경 후 (간단)
# esp_tinyusb wrapper가 자동으로 처리
```

### 4. 예상 효과

✅ **USB PHY 자동 전환**: `tinyusb_driver_install()` 호출 시 자동
✅ **PID_4001 인식**: HID 복합 장치로 올바르게 열거
✅ **HID 인터페이스 정상 표시**: MI_00, MI_01 표시
✅ **ASSERT 에러 해결**: USB OTG 컨트롤러 접근 성공

---

## 검증 계획

### 1. 변경 전 상태 확인

```bash
# 현재 PID 확인
idf.py -p COM8 monitor
# 로그에서 "USB Descriptor: VID=0x303A, PID=0x4001" 확인

# Windows에서 장치 확인
Get-PnpDevice -Class USB | Where-Object {$_.InstanceId -like "*303A*"}
# 예상: PID_1001 (USB Serial/JTAG)
```

### 2. 변경 후 검증

```bash
# 빌드 및 플래시
idf.py fullclean
idf.py build
idf.py -p COM8 flash monitor

# 로그 확인
# 예상: "TinyUSB Driver installed" 메시지
# 예상: ASSERT 에러 없음
```

```powershell
# Windows 장치 확인
Get-PnpDevice -Class USB | Where-Object {$_.InstanceId -like "*303A*"}
# 예상: PID_4001 (BridgeOne HID Composite)

Get-PnpDevice -Class HID
# 예상: "HID 호환 마우스", "HID 키보드 장치" 표시
```

### 3. 성공 기준

- [x] 펌웨어 로그: `TinyUSB Driver installed`
- [ ] Windows 인식: `VID_303A&PID_4001`
- [ ] HID 인터페이스: `MI_00` (HID 키보드), `MI_01` (HID 마우스)
- [ ] 장치 관리자: "HID 호환 마우스", "HID 키보드 장치" 표시
- [ ] ASSERT 에러: 없음

---

## 참고 자료

### 공식 문서
- [ESP-IDF USB Device Stack](https://docs.espressif.com/projects/esp-idf/en/stable/esp32s3/api-reference/peripherals/usb_device.html)
- [USB OTG Console - ESP32-S3](https://docs.espressif.com/projects/esp-idf/en/stable/esp32s3/api-guides/usb-otg-console.html)
- [USB Serial/JTAG Controller Console](https://docs.espressif.com/projects/esp-idf/en/stable/esp32s3/api-guides/usb-serial-jtag-console.html)
- [ESP-IoT-Solution USB PHY Introduction](https://docs.espressif.com/projects/esp-iot-solution/en/latest/usb/usb_overview/usb_phy.html)
- [TinyUSB Application Guide](https://docs.espressif.com/projects/esp-iot-solution/en/latest/usb/usb_overview/tinyusb_guide.html)

### 커뮤니티 리소스
- [ESP32-S3: Permanently disabling USB-JTAG controller (GitHub Issue)](https://github.com/espressif/esp-idf/issues/13946)
- [ESP32 Forum: Using usb_jtag_debug_unit driver on esp32-s3](https://esp32.com/viewtopic.php?t=33623)
- [TinyUSB Issue: ESP32S3 Composite HID Device](https://github.com/hathach/tinyusb/issues/1984)

### ESP-IDF 예제
- [peripherals/usb/device/tusb_hid](https://github.com/espressif/esp-idf/tree/master/examples/peripherals/usb/device/tusb_hid)
- [peripherals/usb/device/tusb_composite_msc_serialdevice](https://github.com/espressif/esp-idf/tree/master/examples/peripherals/usb/device/tusb_composite_msc_serialdevice)

---

## 결론

**근본 원인**: Native TinyUSB 사용 시 **USB PHY가 USB OTG로 자동 전환되지 않음**

**권장 해결책**: **`esp_tinyusb` wrapper로 마이그레이션** (1-2시간 소요)

**예상 결과**: USB PHY 자동 전환 → PID_4001 인식 → HID 복합 장치 정상 동작

**다음 단계**:
1. `esp_tinyusb` wrapper 마이그레이션 구현
2. 테스트 및 검증
3. 성공 시 문서 업데이트

---

**문서 이력**:
- 2025-11-23: 초안 작성 (근본 원인 분석 완료)
