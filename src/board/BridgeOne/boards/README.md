# ESP32-S3-WROOM-1-N16R8 보드 설정 가이드

## 개요

이 프로젝트는 ESP32-S3-WROOM-1-N16R8 모듈을 사용하며, PlatformIO의 표준 `esp32-s3-devkitc-1` 보드 정의를 사용합니다.

## 보드 스펙

- **MCU**: Dual-core Xtensa LX7 @ 240MHz
- **Flash**: 16MB QSPI
- **RAM**: 512KB SRAM
- **ROM**: 384KB
- **PSRAM**: 8MB (추후 활용 예정, 현재 비활성화)
- **USB**: Native USB-OTG (Device 모드)
- **WiFi**: 802.11 b/g/n
- **Bluetooth**: BLE 5.0

## PlatformIO 설정

프로젝트는 표준 ESP32-S3 보드 정의를 사용하며, `platformio.ini`에서 하드웨어 스펙에 맞게 설정합니다:

```ini
[platformio]
default_envs = esp32s3_wroom_test

[env]
platform = espressif32
board = esp32-s3-devkitc-1
framework = arduino

; ESP32-S3-WROOM-1-N16R8 하드웨어 설정
board_build.mcu = esp32s3
board_build.f_cpu = 240000000L
board_build.f_flash = 80000000L
board_build.flash_size = 16MB

; 16MB Flash에 적합한 파티션 테이블
board_build.partitions = default_16MB.csv

; PSRAM 설정 (추후 활용 예정, 현재 비활성화)
; board_build.arduino.memory_type = qio_opi
; board_build.psram_type = opi
```

**주요 특징**:
- 표준 보드 정의 사용으로 유지보수 용이
- 16MB Flash 파티션 테이블 적용
- PSRAM 설정 준비 (추후 활용 시 주석 해제)

## 빌드 환경

### 테스트 환경 (esp32s3_wroom_test)
개발 및 디버깅용으로, Serial.print() 출력이 USB CDC로 정상 동작합니다.

```ini
[env:esp32s3_wroom_test]
build_flags = 
    -D ARDUINO_USB_MODE=1               ; USB OTG 모드
    -D ARDUINO_USB_CDC_ON_BOOT=1        ; 부팅 시 CDC 활성화 (시리얼 출력 가능)
    -D CONFIG_TINYUSB_ENABLED=1
    -D CONFIG_TINYUSB_CDC_ENABLED=1
    -D CONFIG_TINYUSB_HID_ENABLED=1
    -Os
    -D CORE_DEBUG_LEVEL=3               ; 상세 디버그 로그
```

### 실사용 환경 (esp32s3_wroom)
HID 장치 우선 운영용으로, Vendor CDC는 보조 채널로만 동작합니다.

```ini
[env:esp32s3_wroom]
build_flags = 
    -D ARDUINO_USB_MODE=1               ; USB OTG 모드
    -D ARDUINO_USB_CDC_ON_BOOT=0        ; HID 우선 (시리얼 출력 비활성화)
    -D CONFIG_TINYUSB_ENABLED=1
    -D CONFIG_TINYUSB_CDC_ENABLED=1
    -D CONFIG_TINYUSB_HID_ENABLED=1
    -Os
    -D CORE_DEBUG_LEVEL=1               ; 최소 로그
```

## 빌드 및 업로드

### 빌드
```bash
pio run -e esp32s3_wroom_test
```

### 업로드
```bash
pio run -e esp32s3_wroom_test --target upload
```

### 시리얼 모니터
```bash
pio device monitor
```

## 파티션 테이블

16MB Flash에 최적화된 `default_16MB.csv` 사용:
- Bootloader: ~32KB
- Partition Table: ~4KB
- NVS: ~24KB
- OTA Data: ~8KB
- App0: ~3MB
- App1: ~3MB (OTA 업데이트용)
- SPIFFS/FATFS: ~10MB

## PSRAM 활용 계획

현재는 512KB SRAM만 사용하며, 8MB PSRAM은 추후 다음 용도로 활용 예정:
- 대용량 버퍼링 (UART 링 버퍼, JSON 버퍼 확장)
- OTA 업데이트 임시 저장소
- 고급 기능 확장 시 동적 메모리 할당

PSRAM 활성화가 필요한 경우 `platformio.ini`에서 주석 해제:
```ini
board_build.arduino.memory_type = qio_opi
board_build.psram_type = opi
```

## 트러블슈팅

### 1. 업로드 실패 (포트 인식 안됨)

**증상**: `Error: Could not open port`

**해결방법**:
1. BOOT 버튼을 누른 상태에서 RST 버튼 누르기
2. BOOT 버튼 해제
3. 업로드 재시도

### 2. 시리얼 모니터 출력 없음

**원인**: `USB_CDC_ON_BOOT=0`으로 설정된 경우

**해결방법**:
- 개발 시에는 `esp32s3_wroom_test` 환경 사용 (CDC 활성화)
- 배포 시에는 `esp32s3_wroom` 환경 사용 (HID 우선)

### 3. 플래시 메모리 부족

**증상**: `Error: sketch too big`

**해결방법**:
1. `default_16MB.csv` 파티션 테이블 사용 확인
2. 빌드 플래그에 `-Os` 최적화 옵션 확인
3. 불필요한 디버그 로그 비활성화 (`CORE_DEBUG_LEVEL=1`)

## 참고 자료

- [ESP32-S3-WROOM-1 데이터시트](https://www.espressif.com/sites/default/files/documentation/esp32-s3-wroom-1_wroom-1u_datasheet_en.pdf)
- [ESP32-S3 기술 참조 매뉴얼](https://www.espressif.com/sites/default/files/documentation/esp32-s3_technical_reference_manual_en.pdf)
- [PlatformIO ESP32-S3 가이드](https://docs.platformio.org/en/latest/boards/espressif32/esp32-s3-devkitc-1.html)
- [Arduino ESP32 USB 라이브러리](https://docs.espressif.com/projects/arduino-esp32/en/latest/api/usb.html)

## 버전 히스토리

- **v2.0** (2025-10-15): ESP32-S3-WROOM-1-N16R8로 보드 변경
  - 16MB Flash 지원 (4MB → 16MB)
  - 8MB PSRAM 추가 (추후 활용 예정)
  - 표준 `esp32-s3-devkitc-1` 보드 정의 사용
  - 환경명 변경: `esp32s3_wroom_test`, `esp32s3_wroom`
- **v1.0** (2025-10-09): 초기 보드 정의 생성 (Geekble nano)
  - 4MB Flash 지원
  - Native USB-OTG 지원
  - 개발/배포 환경 분리
