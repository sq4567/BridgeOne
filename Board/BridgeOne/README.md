# BridgeOne - Geekble nano ESP32-S3 Project

이 프로젝트는 **Geekble nano ESP32-S3** 보드를 위한 BridgeOne 펌웨어 프로젝트입니다.

## 보드 정보

- **보드명**: Geekble nano ESP32-S3
- **제조사**: SooDragon
- **MCU**: ESP32-S3
- **클럭**: 240MHz
- **Flash**: 8MB
- **RAM**: 320KB
- **특징**: WiFi, Bluetooth, USB OTG, TinyUSB 지원

## 프로젝트 구조

```
BridgeOne/
├── boards/                          # 커스텀 보드 정의
│   └── geekble_nano_esp32s3.json   # Geekble nano ESP32-S3 보드 설정
├── src/                             # 소스 코드
│   └── main.cpp                     # 메인 애플리케이션
├── include/                         # 헤더 파일
├── lib/                            # 프로젝트 라이브러리
├── test/                           # 테스트 코드
└── platformio.ini                  # PlatformIO 설정
```

## 개발 환경 설정

### 1. PlatformIO 설치

```bash
# PlatformIO Core 설치
pip install platformio

# 또는 PlatformIO IDE 사용 (VS Code Extension)
```

### 2. 프로젝트 빌드

```bash
# 프로젝트 디렉토리로 이동
cd Board/BridgeOne

# 빌드 실행
pio run

# 업로드 (보드 연결 후)
pio run --target upload

# 시리얼 모니터
pio device monitor
```

### 3. 보드 연결 확인

```bash
# 연결된 보드 확인
pio device list

# 커스텀 보드가 인식되는지 확인
pio boards | findstr geekble
```

## 기능

현재 구현된 기능:

- ✅ **기본 시스템 정보 출력**
  - CPU 주파수, Flash/RAM 크기
  - WiFi MAC 주소
  - SDK 버전 정보

- ✅ **LED 제어**
  - 내장 LED (GPIO 48) 깜빡임
  - 1초 간격으로 상태 표시

- ✅ **TinyUSB 설정**
  - USB HID 지원
  - USB CDC 지원
  - 컴파일 타임 기능 확인

- ✅ **메모리 모니터링**
  - 실시간 Free Heap 확인
  - 10초마다 상세 상태 출력

## 설정 상세

### PlatformIO 설정 (`platformio.ini`)

```ini
[env:geekble_nano_esp32s3]
platform = espressif32
board = geekble_nano_esp32s3
framework = arduino

; 커스텀 보드 설정
boards_dir = boards

; Geekble nano ESP32-S3 전용 빌드 플래그
build_flags = 
    -DCONFIG_TINYUSB_ENABLED=1
    -DCONFIG_TINYUSB_DEVICE_ENABLED=1
    -DCONFIG_TINYUSB_HID_ENABLED=1
    -DCONFIG_TINYUSB_CDC_ENABLED=1
    -DCONFIG_LOG_DEFAULT_LEVEL=3
    -DCONFIG_ESP_CONSOLE_UART=1

; 시리얼 모니터 설정
monitor_speed = 115200

; 업로드 설정
upload_speed = 921600
```

### 보드 정의 (`boards/geekble_nano_esp32s3.json`)

주요 설정값:
- **MCU**: esp32s3
- **F_CPU**: 240MHz
- **Flash Size**: 8MB
- **Upload Speed**: 921600 baud
- **Hardware ID**: `0x303A:0x1001`
- **Frameworks**: arduino, espidf

## 빌드 결과

최근 빌드 결과:
- **RAM 사용량**: 5.9% (19,192 / 327,680 bytes)
- **Flash 사용량**: 8.4% (282,053 / 3,342,336 bytes)
- **빌드 시간**: ~34초

## 시리얼 출력 예제

```
====================================
   Geekble nano ESP32-S3 Test
====================================
Board Information:
  - Board: Geekble nano ESP32-S3
  - Manufacturer: SooDragon
  - Chip: ESP32-S3
  - CPU Frequency: 240 MHz
  - Flash Size: 8192 KB
  - PSRAM Size: 8192 KB
  - WiFi MAC: XX:XX:XX:XX:XX:XX

Features:
  ✓ TinyUSB Enabled
  ✓ USB HID Enabled
  ✓ USB CDC Enabled
  ✓ Geekble nano Board Definition Active
  ✓ PSRAM Available

LED pin 48 initialized
Setup completed!
====================================
```

## 문제 해결

### 일반적인 문제

1. **보드가 인식되지 않는 경우**
   ```bash
   # 보드 목록 새로고침
   pio platform update espressif32
   ```

2. **업로드 실패**
   - USB 케이블이 데이터 전송을 지원하는지 확인
   - 보드의 Boot 버튼을 누른 상태에서 업로드 시도

3. **컴파일 오류**
   - PlatformIO를 최신 버전으로 업데이트
   ```bash
   pip install -U platformio
   ```

### 경고 메시지

빌드 시 `CONFIG_LOG_DEFAULT_LEVEL` 재정의 경고가 표시될 수 있으나, 이는 정상적인 동작에 영향을 주지 않습니다.

## 라이센스

이 프로젝트는 BridgeOne 프로젝트의 일부로, 해당 라이센스를 따릅니다.

## 기여

이슈나 기능 제안은 GitHub 저장소를 통해 제출해 주세요.

## 참고 자료

- [Geekble nano ESP32-S3 GitHub](https://github.com/SooDragon/Geekble-nano-ESP32S3)
- [PlatformIO 문서](https://docs.platformio.org/)
- [ESP32-S3 기술 참조서](https://www.espressif.com/sites/default/files/documentation/esp32-s3_technical_reference_manual_en.pdf)
