# Geekble nano ESP32-S3 커스텀 보드 설정 가이드

## 개요

이 디렉토리에는 Geekble nano ESP32-S3 보드를 위한 PlatformIO 커스텀 보드 정의가 포함되어 있습니다.

## 보드 스펙

- **MCU**: Dual-core Xtensa LX7 @ 240MHz
- **Flash**: 4MB QSPI
- **RAM**: 512KB SRAM
- **ROM**: 384KB
- **PSRAM**: 없음
- **USB**: Native USB-OTG (Device 모드)
- **WiFi**: 802.11 b/g/n
- **Bluetooth**: BLE 5.0

## 사용 방법

### 방법 1: 프로젝트 로컬 보드 사용 (권장)

현재 설정된 방식으로, `platformio.ini`에서 `board_dir = boards`가 설정되어 있어 프로젝트 내 `boards/` 폴더의 보드 정의를 자동으로 사용합니다.

```ini
[env]
platform = espressif32
board = geekble-nano-esp32s3
framework = arduino
board_dir = boards
```

**장점**:
- 프로젝트와 함께 버전 관리 가능
- 팀원 간 동일한 설정 공유
- PlatformIO 전역 설정 수정 불필요

### 방법 2: 전역 보드 디렉토리에 설치 (선택사항)

전역적으로 사용하고 싶다면 다음 경로에 JSON 파일을 복사:

**Windows**:
```
%USERPROFILE%\.platformio\platforms\espressif32\boards\geekble-nano-esp32s3.json
```

**Linux/Mac**:
```
~/.platformio/platforms/espressif32/boards/geekble-nano-esp32s3.json
```

이 경우 `platformio.ini`에서 `board_dir` 라인을 제거할 수 있습니다.

## 보드 설정 상세

### USB 설정

```json
"extra_flags": [
  "-DARDUINO_GEEKBLE_NANO_ESP32S3",
  "-DARDUINO_USB_MODE=1",           // USB OTG 모드 활성화
  "-DARDUINO_USB_CDC_ON_BOOT=1"     // 부팅 시 CDC 활성화
]
```

- `USB_MODE=1`: Native USB OTG 활성화
- `USB_CDC_ON_BOOT=1`: 시리얼 디버깅 활성화 (개발 시)
- `USB_CDC_ON_BOOT=0`: HID 우선 모드 (배포 시)

### 메모리 설정

```json
"upload": {
  "flash_size": "4MB",              // 4MB Flash
  "maximum_ram_size": 327680,       // 320KB 사용 가능 SRAM
  "maximum_size": 4194304           // 4MB = 4 * 1024 * 1024
}
```

### 파티션 테이블

기본적으로 `default.csv` 사용 (4MB Flash에 최적화):
- Bootloader: ~32KB
- Partition Table: ~4KB
- NVS: ~24KB
- OTA Data: ~8KB
- App0: ~1.2MB
- App1: ~1.2MB (OTA 업데이트용)
- SPIFFS/FATFS: ~1.5MB

## 빌드 및 업로드

### 빌드
```bash
pio run -e geekble_nano_esp32s3_test
```

### 업로드
```bash
pio run -e geekble_nano_esp32s3_test --target upload
```

### 시리얼 모니터
```bash
pio device monitor
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
- 개발 시에는 `geekble_nano_esp32s3_test` 환경 사용 (CDC 활성화)
- 배포 시에는 `geekble_nano_esp32s3` 환경 사용 (HID 우선)

### 3. 보드를 찾을 수 없음

**증상**: `Error: Unknown board ID 'geekble-nano-esp32s3'`

**해결방법**:
1. `boards/geekble-nano-esp32s3.json` 파일 존재 확인
2. `platformio.ini`에 `board_dir = boards` 설정 확인
3. PlatformIO 캐시 정리: `pio run --target clean`

## 참고 자료

- [PlatformIO 보드 생성 가이드](https://docs.platformio.org/en/latest/platforms/creating_board.html)
- [ESP32-S3 공식 문서](https://docs.espressif.com/projects/esp-idf/en/latest/esp32s3/)
- [PlatformIO 커뮤니티 - ESP32-S3 설정](https://community.platformio.org/t/how-do-you-set-up-a-new-board-esp32-s3-n16r8/32306)

## 버전 히스토리

- **v1.0** (2025-10-09): 초기 보드 정의 생성
  - 4MB Flash 지원
  - Native USB-OTG 지원
  - 개발/배포 환경 분리

