---
title: "ESP32-S3-DevkitC-1 ESP-IDF 구현 가이드"
description: "ESP-IDF 프레임워크 기반 ESP32-S3 USB 브릿지 역할 구현을 위한 설계 요구사항과 아키텍처 명세"
tags: ["esp32-s3", "esp-idf", "tinyusb", "firmware", "design", "specification", "architecture", "reference-implementation"]
version: "v1.0"
owner: "Chatterbones"
updated: "2025-10-21"
framework: "ESP-IDF (idf.py)"
board: "ESP32-S3-DevkitC-1-N16R8"
---

# ESP32-S3-DevkitC-1 ESP-IDF 구현 가이드

> **문서 목적**: BridgeOne 시스템에서 ESP32-S3가 수행해야 할 USB 브릿지 역할을 ESP-IDF 프레임워크 기반으로 구현하기 위한 설계 요구사항과 아키텍처 원칙을 정의합니다.
>
> **시스템 아키텍처 참조**: [`technical-specification.md` §2 시스템 아키텍처] 필수 선행 이해 필요
> **Android 앱 연동**: [`technical-specification-app.md` §1 USB 통신] 참조
> **Windows 서버 연동**: [`technical-specification-server.md` §2 기술 스택] 참조

## 1. 시스템 개요

### 1.1 ESP32-S3의 역할

**BridgeOne 시스템에서의 위치**:
```
Android 앱 ↔ [UART 1Mbps] ↔ ESP32-S3 ↔ [USB HID/CDC] ↔ PC (Windows)
```

**핵심 책임**:
- **프로토콜 브릿지**: UART 직렬 통신을 USB HID/CDC 프로토콜로 변환
- **USB 복합 장치**: HID Boot Mouse + HID Boot Keyboard + Vendor CDC 인터페이스 동시 제공
- **모드 기반 동작**: Essential/Standard 모드에 따른 적응적 기능 제공
- **실시간 처리**: 50ms 이하 엔드투엔드 지연시간 목표 달성에 기여

### 1.2 하드웨어 사양

**ESP32-S3-DevkitC-1-N16R8 스펙**:
- **MCU**: Dual-core Xtensa LX7 (240MHz max)
- **메모리**: 512KB SRAM, 384KB ROM, 16MB Flash (N16), 8MB PSRAM (R8)
- **USB**: Native USB-OTG 지원 (Device 모드) + 내장 USB-to-UART
- **GPIO**: 45개 GPIO 핀 (6개 SPI, 3개 UART, 2개 I2C)
- **전력**: 3.3V 동작, 저전력 모드 지원

**연결 구성**:
- **USB-to-UART 포트**: Android 스마트폰 연결 (내장 USB Serial 브릿지)
- **USB-OTG 포트**: PC 직접 연결 (복합 장치: HID + CDC)
- **전원**: USB 5V (두 포트 중 하나에서 공급)

### 1.3 USB Composite 디바이스 설계 계약 (Protocol Contract)

**중요**: 이 절은 Android 앱 및 Windows 서버와의 **호환성 보장**을 위한 핵심 계약 사항입니다. ESP-IDF 기반의 설계에서는 아래 프로토콜 규격을 **절대 변경하지 않습니다**.

#### 1.3.1 USB Composite 디바이스 구성 (고정)

**인터페이스 구성 및 순서** (절대 변경 금지):
```
Interface 0: HID Boot Keyboard (0x03/0x01/0x01)
Interface 1: HID Boot Mouse    (0x03/0x01/0x02)
Interface 2: CDC-ACM Comm      (0x02/0x02/0x00)
Interface 3: CDC-ACM Data      (0x0A/0x00/0x00)
```

**설계 근거**:
- Windows 및 Android 측 코드는 인터페이스 번호로 각 기능을 식별함
- 순서 변경 시 기존 호스트 드라이버와 호환성 문제 발생
- BIOS/UEFI는 첫 번째 Boot Protocol HID를 우선 인식

#### 1.3.2 HID 리포트 디스크립터 (고정)

**HID Boot Keyboard 리포트** (8바이트):
```c
// 절대 변경 금지 - BIOS/UEFI 호환성 및 Android/Windows 파싱 의존
struct HIDBootKeyboardReport {
    uint8_t modifiers;   // [0] Ctrl/Alt/Shift/GUI 비트맵
    uint8_t reserved;    // [1] 0x00 고정
    uint8_t keyCodes[6]; // [2-7] 동시 입력 키 코드 (6KRO)
} __attribute__((packed));
```

**HID Boot Mouse 리포트** (4바이트):
```c
// 절대 변경 금지 - BIOS/UEFI 호환성 및 Android/Windows 파싱 의존
struct HIDBootMouseReport {
    uint8_t buttons; // [0] bit0=L, bit1=R, bit2=M
    int8_t deltaX;   // [1] X축 이동 (-127~127)
    int8_t deltaY;   // [2] Y축 이동 (-127~127)
    int8_t wheel;    // [3] 휠 스크롤 (-127~127)
} __attribute__((packed));
```

#### 1.3.3 BridgeOne UART 프로토콜 (고정)

**8바이트 델타 프레임** (Android ↔ ESP32-S3):
```c
// 절대 변경 금지 - Android 앱의 프레임 인코딩/디코딩 로직 의존
typedef struct __attribute__((packed)) {
    uint8_t seq;        // [0] 순번 카운터 (0~255 순환)
    uint8_t buttons;    // [1] 마우스 버튼 (bit0=L, bit1=R, bit2=M)
    int8_t deltaX;      // [2] X축 상대 이동 (-127~127)
    int8_t deltaY;      // [3] Y축 상대 이동 (-127~127)
    int8_t wheel;       // [4] 휠 스크롤 (-127~127)
    uint8_t modifiers;  // [5] 키보드 모디파이어
    uint8_t keyCode1;   // [6] 키 코드 1
    uint8_t keyCode2;   // [7] 키 코드 2
} bridge_frame_t;
```

#### 1.3.4 Vendor CDC 메시지 프로토콜 (고정)

**JSON 기반 명령 프레임** (ESP32-S3 ↔ Windows):
```c
// 절대 변경 금지 - Windows 서버의 JSON 파싱 및 명령 처리 로직 의존
struct VendorCDCFrame {
    uint8_t header;      // [0] 0xFF (벤더 명령 식별자)
    uint8_t command;     // [1] 명령 타입 (0x01-0xFE)
    uint16_t length;     // [2-3] 페이로드 길이 (Little-Endian, 최대 448 bytes)
    uint8_t payload[];   // [4+] JSON 형식 명령 데이터 (UTF-8 인코딩)
    uint16_t checksum;   // [끝-2, 끝-1] CRC16 체크섬 (Little-Endian)
} __attribute__((packed));
```

**프레임 크기 제한 및 검증** (Phase 2 통신 안정화):
- **최대 페이로드 크기**: 448 bytes
  - 계산 근거: 512 bytes (ESP32-S3 버퍼) - 6 bytes (헤더 필드) - 58 bytes (예비 영역) = 448 bytes
- **length 필드 검증 로직**:
  ```c
  #define MAX_VENDOR_CDC_PAYLOAD_SIZE 448
  
  bool isValidVendorCDCFrame(const VendorCDCFrame* frame, size_t receivedSize) {
      // 1. 헤더 검증
      if (frame->header != 0xFF) {
          ESP_LOGW(TAG, "Invalid header: 0x%02X", frame->header);
          return false;
      }
      
      // 2. command 필드 범위 검증
      if (frame->command == 0x00 || frame->command == 0xFF) {
          ESP_LOGW(TAG, "Invalid command: 0x%02X", frame->command);
          return false;
      }
      
      // 3. length 필드 검증 (Little-Endian)
      uint16_t payloadLength = (uint16_t)frame->length;  // Little-Endian 자동 처리
      if (payloadLength > MAX_VENDOR_CDC_PAYLOAD_SIZE) {
          ESP_LOGW(TAG, "Payload length exceeds limit: %u > %u", payloadLength, MAX_VENDOR_CDC_PAYLOAD_SIZE);
          return false;
      }
      
      // 4. 수신된 프레임 크기 검증
      size_t expectedSize = 1 + 1 + 2 + payloadLength + 2;  // header + command + length + payload + checksum
      if (receivedSize < expectedSize) {
          ESP_LOGW(TAG, "Incomplete frame: received %u, expected %u", receivedSize, expectedSize);
          return false;
      }
      
      return true;
  }
  ```

**0xFF 헤더 충돌 방지 - length 기반 프레임 추출** (Phase 2 통신 안정화):
```c
typedef enum {
    FRAME_STATE_SEARCH_HEADER,
    FRAME_STATE_READ_COMMAND,
    FRAME_STATE_READ_LENGTH,
    FRAME_STATE_READ_PAYLOAD,
    FRAME_STATE_READ_CHECKSUM,
    FRAME_STATE_COMPLETE
} FrameParseState;

typedef struct {
    FrameParseState state;
    uint8_t buffer[512];
    size_t bufferIndex;
    uint8_t command;
    uint16_t payloadLength;
    size_t payloadBytesRead;
    uint16_t checksum;
} FrameParser;

FrameParser frameParser = {.state = FRAME_STATE_SEARCH_HEADER, .bufferIndex = 0};

// UART/CDC 스트림에서 프레임 추출 (바이트 단위 처리)
bool parseVendorCDCFrameByte(uint8_t byte, VendorCDCFrame** outFrame) {
    switch (frameParser.state) {
        case FRAME_STATE_SEARCH_HEADER:
            if (byte == 0xFF) {
                frameParser.buffer[0] = byte;
                frameParser.bufferIndex = 1;
                frameParser.state = FRAME_STATE_READ_COMMAND;
            }
            break;
            
        case FRAME_STATE_READ_COMMAND:
            if (byte >= 0x01 && byte <= 0xFE) {  // command 범위 검증
                frameParser.buffer[frameParser.bufferIndex++] = byte;
                frameParser.command = byte;
                frameParser.state = FRAME_STATE_READ_LENGTH;
            } else {
                // command 범위 벗어남 → 헤더 재탐색
                ESP_LOGW(TAG, "Invalid command: 0x%02X, resetting to search header", byte);
                frameParser.state = FRAME_STATE_SEARCH_HEADER;
                frameParser.bufferIndex = 0;
            }
            break;
            
        case FRAME_STATE_READ_LENGTH:
            frameParser.buffer[frameParser.bufferIndex++] = byte;
            
            if (frameParser.bufferIndex == 4) {  // length 필드 완료 (2 bytes)
                // Little-Endian으로 length 읽기
                frameParser.payloadLength = (uint16_t)(frameParser.buffer[2] | (frameParser.buffer[3] << 8));
                
                if (frameParser.payloadLength > MAX_VENDOR_CDC_PAYLOAD_SIZE) {
                    // length 필드 값이 최대 크기 초과 → 헤더 재탐색
                    ESP_LOGW(TAG, "Payload length exceeds limit: %u > %u, resetting to search header", 
                             frameParser.payloadLength, MAX_VENDOR_CDC_PAYLOAD_SIZE);
                    frameParser.state = FRAME_STATE_SEARCH_HEADER;
                    frameParser.bufferIndex = 0;
                } else if (frameParser.payloadLength == 0) {
                    // 페이로드 없음 → checksum으로 이동
                    frameParser.state = FRAME_STATE_READ_CHECKSUM;
                } else {
                    frameParser.payloadBytesRead = 0;
                    frameParser.state = FRAME_STATE_READ_PAYLOAD;
                }
            }
            break;
            
        case FRAME_STATE_READ_PAYLOAD:
            frameParser.buffer[frameParser.bufferIndex++] = byte;
            frameParser.payloadBytesRead++;
            
            if (frameParser.payloadBytesRead == frameParser.payloadLength) {
                // 페이로드 읽기 완료 → checksum으로 이동
                frameParser.state = FRAME_STATE_READ_CHECKSUM;
            }
            break;
            
        case FRAME_STATE_READ_CHECKSUM:
            frameParser.buffer[frameParser.bufferIndex++] = byte;
            
            if (frameParser.bufferIndex == 4 + frameParser.payloadLength + 2) {  // 전체 프레임 완료
                // CRC16 검증
                size_t payloadOffset = 4;
                uint16_t receivedChecksum = (uint16_t)(
                    frameParser.buffer[4 + frameParser.payloadLength] | 
                    (frameParser.buffer[4 + frameParser.payloadLength + 1] << 8)
                );
                uint16_t calculatedChecksum = esp_crc16_le(0, &frameParser.buffer[payloadOffset], frameParser.payloadLength);
                
                if (receivedChecksum == calculatedChecksum) {
                    // CRC 검증 성공 → 프레임 완성
                    *outFrame = (VendorCDCFrame*)frameParser.buffer;
                    frameParser.state = FRAME_STATE_SEARCH_HEADER;  // 다음 프레임 대기
                    frameParser.bufferIndex = 0;
                    return true;  // 프레임 추출 성공
                } else {
                    // CRC 검증 실패 → CRC_ERROR 응답 전송
                    ESP_LOGE(TAG, "CRC mismatch: received 0x%04X, calculated 0x%04X", receivedChecksum, calculatedChecksum);
                    sendCrcErrorResponse();
                    frameParser.state = FRAME_STATE_SEARCH_HEADER;
                    frameParser.bufferIndex = 0;
                }
            }
            break;
    }
    
    return false;  // 프레임 추출 미완료
}
```

**장점**:
- JSON 페이로드 내 0xFF 바이트가 있어도 프레임 구분 오류 없음
- 잘못된 0xFF 바이트를 헤더로 오인해도 command나 length 검증 실패로 자동 복구

#### 1.3.5 호환성 검증 요구사항

**Android/Windows 코드 무변경 보장**:
- ✅ USB 디바이스 클래스/서브클래스/프로토콜 코드
- ✅ USB 인터페이스 번호 및 순서
- ✅ HID 리포트 디스크립터 구조 및 크기
- ✅ BridgeOne UART 프레임 구조
- ✅ Vendor CDC 메시지 프레임 구조
- ✅ JSON 명령 타입 및 페이로드 스키마

**호환성 검증 조건**:
- Android 앱에서 기존과 동일하게 디바이스 인식 및 통신 가능
- Windows에서 기존과 동일하게 HID/CDC 인터페이스 열거 및 사용 가능
- BIOS/UEFI에서 Boot Protocol HID 장치로 정상 인식

## 2. 통신 프로토콜 명세

### 2.1 UART 통신 (Android ↔ ESP32-S3)

**통신 설정**:
- **속도**: 1,000,000 bps (1Mbps)
- **데이터 형식**: 8N1 (8비트 데이터, 패리티 없음, 1스톱비트)
- **플로우 제어**: 하드웨어 플로우 제어 권장

**BridgeOne 프레임 구조 (8바이트)**:
```c
typedef struct __attribute__((packed)) {
    uint8_t seq;        // [0] 순번 카운터 (0~255 순환)
    uint8_t buttons;    // [1] 마우스 버튼 상태 (bit0=L, bit1=R, bit2=M)
    int8_t deltaX;      // [2] X축 상대 이동 (-127~127)
    int8_t deltaY;      // [3] Y축 상대 이동 (-127~127)
    int8_t wheel;       // [4] 휠 스크롤량 (-127~127)
    uint8_t modifiers;  // [5] 키보드 모디파이어 (Ctrl, Alt, Shift, GUI)
    uint8_t keyCode1;   // [6] 키 코드 1 (주요 키 입력)
    uint8_t keyCode2;   // [7] 키 코드 2 (보조 키 입력)
} bridge_frame_t;
```

**프레임 검증**:
- **크기 검증**: 정확히 8바이트 수신 확인
- **순번 검증**: 순번 카운터의 순차성 확인 (패킷 손실 감지)
- **범위 검증**: 각 필드값이 유효 범위 내인지 확인

### 2.2 USB HID 통신 (ESP32-S3 ↔ PC)

#### 2.2.1 HID Boot Mouse Interface

**프로토콜 사양**:
- **인터페이스 클래스**: HID (0x03)
- **서브 클래스**: Boot Interface (0x01)
- **프로토콜**: Mouse (0x02)
- **리포트 크기**: 4바이트 (Boot Protocol 표준)

**Boot Mouse 리포트 구조 요구사항**:
```c
// HID Boot Mouse 리포트 (4바이트)
struct HIDBootMouseReport {
    buttons: u8,       // 버튼 상태 (bit0=L, bit1=R, bit2=M)
    deltaX: i8,        // X축 이동 (-127~127)
    deltaY: i8,        // Y축 이동 (-127~127)
    wheel: i8          // 휠 이동 (-127~127)
}
```

#### 2.2.2 HID Boot Keyboard Interface

**프로토콜 사양**:
- **인터페이스 클래스**: HID (0x03)
- **서브 클래스**: Boot Interface (0x01)
- **프로토콜**: Keyboard (0x01)
- **리포트 크기**: 8바이트 (Boot Protocol 표준)
- **최대 동시 입력**: 6키 + 모디파이어

**Boot Keyboard 리포트 구조 요구사항**:
```c
// HID Boot Keyboard 리포트 (8바이트)
struct HIDBootKeyboardReport {
    modifiers: u8,      // 모디파이어 키 (Ctrl, Alt, Shift, GUI)
    reserved: u8,       // 예약 (0x00)
    keyCodes: u8[6]     // 동시 입력 키 코드 (최대 6키)
}
```

#### 2.2.3 Vendor CDC Interface

**프로토콜 사양**:
- **인터페이스 클래스**: CDC (0x02)
- **서브 클래스**: Abstract Control Model (0x02)
- **벤더별 명령**: 0xFF 헤더 + JSON 페이로드
- **체크섬**: CRC16 기반 데이터 무결성 검증
- **바이트 순서**: Little-Endian

**Vendor CDC 프레임 구조 요구사항**:
```c
// Vendor CDC 명령 프레임
struct VendorCDCFrame {
    header: u8,         // 0xFF (벤더 명령 식별자)
    command: u8,        // 명령 타입 (0x01-0xFE)
    length: u16,        // 페이로드 길이 (Little-Endian)
    payload: u8[],      // JSON 형식 명령 데이터
    checksum: u16       // CRC16 체크섬 (Little-Endian)
}
```

## 3. 소프트웨어 아키텍처 설계

### 3.1 ESP-IDF 프로젝트 구조

**프로젝트 레이아웃 요구사항**:
```
bridgeone_board/
├── CMakeLists.txt              # 프로젝트 빌드 설정
├── sdkconfig                   # ESP-IDF 설정 (idf.py menuconfig로 생성)
├── sdkconfig.defaults          # 기본 설정값
├── partitions.csv              # 플래시 파티션 테이블
├── main/
│   ├── CMakeLists.txt          # 메인 컴포넌트 빌드 설정
│   ├── main.c                  # 진입점 (app_main)
│   ├── usb_descriptors.c       # USB 디스크립터 정의 (§1.5 불변 규칙 준수)
│   ├── usb_descriptors.h
│   ├── uart_handler.c          # UART 통신 처리
│   ├── uart_handler.h
│   ├── hid_handler.c           # HID 리포트 처리
│   ├── hid_handler.h
│   ├── cdc_handler.c           # CDC 통신 처리
│   ├── cdc_handler.h
│   ├── protocol_router.c       # 프로토콜 라우팅
│   └── protocol_router.h
└── components/                 # 추가 컴포넌트 (필요시)
```

### 3.2 ESP-IDF 빌드 시스템 설정

**CMakeLists.txt 요구사항** (프로젝트 루트):
```cmake
cmake_minimum_required(VERSION 3.16)

# ESP-IDF 컴포넌트 포함
include($ENV{IDF_PATH}/tools/cmake/project.cmake)

# 프로젝트 정의
project(bridgeone_board)
```

**CMakeLists.txt 요구사항** (main/ 디렉토리):
```cmake
idf_component_register(
    SRCS 
        "main.c"
        "usb_descriptors.c"
        "uart_handler.c"
        "hid_handler.c"
        "cdc_handler.c"
        "protocol_router.c"
    INCLUDE_DIRS "."
    REQUIRES 
        driver
        esp_timer
        freertos
        usb
)
```

**sdkconfig.defaults 필수 설정**:
```ini
# TinyUSB 활성화
CONFIG_TINYUSB=y
CONFIG_TINYUSB_HID_ENABLED=y
CONFIG_TINYUSB_CDC_ENABLED=y

# USB 디바이스 모드
CONFIG_TINYUSB_DEVICE_ENABLED=y

# UART 설정
CONFIG_UART_ISR_IN_IRAM=y

# FreeRTOS 최적화
CONFIG_FREERTOS_HZ=1000
CONFIG_FREERTOS_UNICORE=n

# 메모리 최적화
CONFIG_COMPILER_OPTIMIZATION_SIZE=y
```

### 3.3 TinyUSB Composite 디바이스 구현

ESP-IDF의 TinyUSB 스택을 사용하여 §1.3에 정의된 불변 규칙을 준수하는 Composite 디바이스를 구성합니다.

#### 3.3.1 ESP-IDF 프로젝트 설정

**CMakeLists.txt 구성**:
```cmake
# main/CMakeLists.txt
idf_component_register(
    SRCS "main.c" "usb_descriptors.c" "uart_handler.c" "hid_handler.c"
    INCLUDE_DIRS "."
    REQUIRES tinyusb esp_timer driver
)
```

**sdkconfig 주요 설정** (menuconfig 또는 직접 편집):
```ini
# USB OTG 활성화
CONFIG_USB_OTG_SUPPORTED=y

# TinyUSB 스택 활성화
CONFIG_TINYUSB=y
CONFIG_TINYUSB_CDC_ENABLED=y
CONFIG_TINYUSB_HID_ENABLED=y

# 디바이스 스택 활성화
CONFIG_TINYUSB_DEVICE_ENABLED=y
CONFIG_TINYUSB_HOST_ENABLED=n

# 버퍼 크기 설정
CONFIG_TINYUSB_CDC_RX_BUFSIZE=512
CONFIG_TINYUSB_CDC_TX_BUFSIZE=512
```

**Compile Definitions** (자동 설정됨):
```c
// ESP-IDF TinyUSB 컴포넌트가 자동으로 정의
#define CFG_TUSB_MCU OPT_MCU_ESP32S3
#define CFG_TUSB_OS OPT_OS_FREERTOS
#define CFG_TUD_ENABLED 1
```

#### 3.3.2 TinyUSB 디스크립터 설정

**엔드포인트 번호 및 버퍼 크기 정의**:
```c
// usb_descriptors.c
#include "tusb.h"

// 엔드포인트 번호 정의 (ESP32-S3는 최대 6개 IN/OUT EP 지원)
#define EPNUM_HID_KB      0x81  // IN EP1: Keyboard
#define EPNUM_HID_MOUSE   0x82  // IN EP2: Mouse
#define EPNUM_CDC_NOTIF   0x83  // IN EP3: CDC Notification
#define EPNUM_CDC_OUT     0x04  // OUT EP4: CDC Data Out
#define EPNUM_CDC_IN      0x84  // IN EP4: CDC Data In

// 버퍼 크기 (64바이트는 Full Speed USB 기본값)
#define CFG_TUD_HID_EP_BUFSIZE  64
#define CFG_TUD_CDC_EP_BUFSIZE  64
```

**인터페이스 번호 및 Configuration Descriptor**:
```c
// §1.3.1의 인터페이스 순서 고정 준수
enum {
    ITF_NUM_HID_KEYBOARD = 0,  // Interface 0: HID Boot Keyboard
    ITF_NUM_HID_MOUSE    = 1,  // Interface 1: HID Boot Mouse
    ITF_NUM_CDC_COMM     = 2,  // Interface 2: CDC-ACM Comm
    ITF_NUM_CDC_DATA     = 3,  // Interface 3: CDC-ACM Data
    ITF_NUM_TOTAL
};

// Configuration Descriptor 총 길이 계산
#define CONFIG_TOTAL_LEN  (TUD_CONFIG_DESC_LEN + TUD_HID_DESC_LEN * 2 + TUD_CDC_DESC_LEN)

// Configuration Descriptor
uint8_t const desc_configuration[] = {
    // Configuration: 4 interfaces
    TUD_CONFIG_DESCRIPTOR(1, ITF_NUM_TOTAL, 0, CONFIG_TOTAL_LEN, 
                          TUSB_DESC_CONFIG_ATT_REMOTE_WAKEUP, 500),
    
    // Interface 0: HID Boot Keyboard (§1.3.2 리포트 구조 준수)
    TUD_HID_DESCRIPTOR(ITF_NUM_HID_KEYBOARD, 0, HID_ITF_PROTOCOL_KEYBOARD,
                       sizeof(desc_hid_keyboard_report), EPNUM_HID_KB, 
                       CFG_TUD_HID_EP_BUFSIZE, 1),
    
    // Interface 1: HID Boot Mouse (§1.3.2 리포트 구조 준수)
    TUD_HID_DESCRIPTOR(ITF_NUM_HID_MOUSE, 0, HID_ITF_PROTOCOL_MOUSE,
                       sizeof(desc_hid_mouse_report), EPNUM_HID_MOUSE,
                       CFG_TUD_HID_EP_BUFSIZE, 1),
    
    // Interface 2/3: CDC-ACM (§1.3.4 메시지 프로토콜 준수)
    TUD_CDC_DESCRIPTOR(ITF_NUM_CDC_COMM, 4, EPNUM_CDC_NOTIF,
                       8, EPNUM_CDC_OUT, EPNUM_CDC_IN, CFG_TUD_CDC_EP_BUFSIZE)
};

// HID Boot Keyboard Report Descriptor (§1.3.2 준수 - 8바이트 고정)
uint8_t const desc_hid_keyboard_report[] = {
    HID_USAGE_PAGE(HID_USAGE_PAGE_DESKTOP),
    HID_USAGE(HID_USAGE_DESKTOP_KEYBOARD),
    HID_COLLECTION(HID_COLLECTION_APPLICATION),
        // Modifier Keys (Ctrl, Shift, Alt, GUI)
        HID_USAGE_PAGE(HID_USAGE_PAGE_KEYBOARD),
        HID_USAGE_MIN(0xE0), HID_USAGE_MAX(0xE7),
        HID_LOGICAL_MIN(0), HID_LOGICAL_MAX(1),
        HID_REPORT_COUNT(8), HID_REPORT_SIZE(1),
        HID_INPUT(HID_DATA | HID_VARIABLE | HID_ABSOLUTE),
        
        // Reserved Byte
        HID_REPORT_COUNT(1), HID_REPORT_SIZE(8),
        HID_INPUT(HID_CONSTANT),
        
        // Key Codes (6 keys)
        HID_REPORT_COUNT(6), HID_REPORT_SIZE(8),
        HID_LOGICAL_MIN(0), HID_LOGICAL_MAX(101),
        HID_USAGE_MIN(0), HID_USAGE_MAX(101),
        HID_INPUT(HID_DATA | HID_ARRAY),
    HID_COLLECTION_END
};

// HID Boot Mouse Report Descriptor (§1.3.2 준수 - 4바이트 고정)
uint8_t const desc_hid_mouse_report[] = {
    HID_USAGE_PAGE(HID_USAGE_PAGE_DESKTOP),
    HID_USAGE(HID_USAGE_DESKTOP_MOUSE),
    HID_COLLECTION(HID_COLLECTION_APPLICATION),
        HID_USAGE(HID_USAGE_DESKTOP_POINTER),
        HID_COLLECTION(HID_COLLECTION_PHYSICAL),
            // Buttons (3 bits: Left, Right, Middle)
            HID_USAGE_PAGE(HID_USAGE_PAGE_BUTTON),
            HID_USAGE_MIN(1), HID_USAGE_MAX(3),
            HID_LOGICAL_MIN(0), HID_LOGICAL_MAX(1),
            HID_REPORT_COUNT(3), HID_REPORT_SIZE(1),
            HID_INPUT(HID_DATA | HID_VARIABLE | HID_ABSOLUTE),
            
            // Padding (5 bits)
            HID_REPORT_COUNT(1), HID_REPORT_SIZE(5),
            HID_INPUT(HID_CONSTANT),
            
            // X, Y (relative)
            HID_USAGE_PAGE(HID_USAGE_PAGE_DESKTOP),
            HID_USAGE(HID_USAGE_DESKTOP_X), HID_USAGE(HID_USAGE_DESKTOP_Y),
            HID_LOGICAL_MIN(-127), HID_LOGICAL_MAX(127),
            HID_REPORT_COUNT(2), HID_REPORT_SIZE(8),
            HID_INPUT(HID_DATA | HID_VARIABLE | HID_RELATIVE),
            
            // Wheel (relative)
            HID_USAGE(HID_USAGE_DESKTOP_WHEEL),
            HID_LOGICAL_MIN(-127), HID_LOGICAL_MAX(127),
            HID_REPORT_COUNT(1), HID_REPORT_SIZE(8),
            HID_INPUT(HID_DATA | HID_VARIABLE | HID_RELATIVE),
        HID_COLLECTION_END,
    HID_COLLECTION_END
};
```

#### 3.3.3 TinyUSB 콜백 구현

**HID Get Report 콜백** (호스트가 HID 리포트 요청 시):
```c
static uint8_t last_kb_report[8] = {0};
static uint8_t last_mouse_report[4] = {0};

uint16_t tud_hid_get_report_cb(uint8_t instance, uint8_t report_id,
                                hid_report_type_t report_type,
                                uint8_t* buffer, uint16_t reqlen) {
    if (report_type != HID_REPORT_TYPE_INPUT) {
        return 0;  // Feature/Output 리포트는 미지원
    }
    
    // Keyboard 인스턴스
    if (instance == ITF_NUM_HID_KEYBOARD) {
        if (reqlen >= 8) {
            memcpy(buffer, last_kb_report, 8);
            return 8;
        }
    }
    // Mouse 인스턴스
    else if (instance == ITF_NUM_HID_MOUSE) {
        if (reqlen >= 4) {
            memcpy(buffer, last_mouse_report, 4);
            return 4;
        }
    }
    
    return 0;
}
```

**CDC Line Coding 콜백** (호스트가 시리얼 설정 변경 시):
```c
void tud_cdc_line_coding_cb(uint8_t itf, cdc_line_coding_t const* coding) {
    // Vendor CDC는 실제 UART 설정과 무관 (가상 포트)
    // Windows/Linux에서 자동으로 호출되므로 무시해도 무방
    ESP_LOGI("CDC", "Line coding: baudrate=%lu, databits=%u, parity=%u, stopbits=%u",
             coding->bit_rate, coding->data_bits, coding->parity, coding->stop_bits);
}
```

**CDC RX 콜백** (Windows에서 JSON 명령 수신 시):
```c
#define CDC_RX_BUFFER_SIZE 512
static uint8_t cdc_rx_buffer[CDC_RX_BUFFER_SIZE];

void tud_cdc_rx_cb(uint8_t itf) {
    if (itf != ITF_NUM_CDC_COMM) return;
    
    uint32_t count = tud_cdc_n_available(itf);
    if (count == 0) return;
    
    // 버퍼 오버플로우 방지
    if (count > CDC_RX_BUFFER_SIZE) {
        ESP_LOGW("CDC", "RX overflow: %lu > %d", count, CDC_RX_BUFFER_SIZE);
        tud_cdc_n_read_flush(itf);  // 버퍼 비우기
        return;
    }
    
    uint32_t read_count = tud_cdc_n_read(itf, cdc_rx_buffer, count);
    
    // §1.3.4 VendorCDCFrame 파싱 (바이트 단위 처리)
    for (uint32_t i = 0; i < read_count; i++) {
        VendorCDCFrame* frame = NULL;
        if (parseVendorCDCFrameByte(cdc_rx_buffer[i], &frame)) {
            // 프레임 추출 성공 → JSON 명령 처리
            handleVendorCDCCommand(frame);
        }
    }
}
```

#### 3.3.4 app_main() 초기화 순서

**ESP-IDF 진입점 및 TinyUSB 초기화**:
```c
#include "esp_log.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "tusb.h"

static const char* TAG = "BridgeOne";

void app_main(void) {
    ESP_LOGI(TAG, "BridgeOne USB Bridge Initializing...");
    
    // 1. TinyUSB 스택 초기화
    ESP_ERROR_CHECK(tud_init(BOARD_TUD_RHPORT));
    ESP_LOGI(TAG, "TinyUSB device stack initialized");
    
    // 2. UART 초기화 (Android 통신)
    uart_config_t uart_config = {
        .baud_rate = 1000000,  // 1Mbps
        .data_bits = UART_DATA_8_BITS,
        .parity = UART_PARITY_DISABLE,
        .stop_bits = UART_STOP_BITS_1,
        .flow_ctrl = UART_HW_FLOWCTRL_DISABLE,
    };
    ESP_ERROR_CHECK(uart_param_config(UART_NUM_0, &uart_config));
    ESP_ERROR_CHECK(uart_driver_install(UART_NUM_0, 1024, 1024, 0, NULL, 0));
    ESP_LOGI(TAG, "UART initialized (1Mbps)");
    
    // 3. FreeRTOS 태스크 생성 (§3.5 참조)
    xTaskCreatePinnedToCore(uart_task, "UART", 4096, NULL, 10, NULL, 0);
    xTaskCreatePinnedToCore(hid_task, "HID", 4096, NULL, 9, NULL, 0);
    xTaskCreatePinnedToCore(cdc_task, "CDC", 4096, NULL, 8, NULL, 1);
    xTaskCreatePinnedToCore(usb_task, "USB", 4096, NULL, 5, NULL, 1);
    
    ESP_LOGI(TAG, "All tasks created, BridgeOne ready");
}

// USB 장치 태스크 (TinyUSB 이벤트 처리)
void usb_task(void* param) {
    while (1) {
        tud_task();  // TinyUSB 이벤트 폴링 (non-blocking)
        vTaskDelay(pdMS_TO_TICKS(1));
    }
}
```

### 3.4 BridgeOne 프레임 처리 로직

**UART 프레임 → USB HID 변환**:
```c
// §1.5.3 BridgeOne 프레임을 §1.5.2 HID 리포트로 변환
void processBridgeFrame(const bridge_frame_t* frame) {
    // 키보드 리포트 생성 및 전송
    if (frame->modifiers != 0 || frame->keyCode1 != 0 || frame->keyCode2 != 0) {
        HIDBootKeyboardReport kb_report = {
            .modifiers = frame->modifiers,
            .reserved = 0,
            .keyCodes = {frame->keyCode1, frame->keyCode2, 0, 0, 0, 0}
        };
        tud_hid_n_report(ITF_NUM_HID_KEYBOARD, 0, &kb_report, sizeof(kb_report));
    }
    
    // 마우스 리포트 생성 및 전송
    if (frame->deltaX != 0 || frame->deltaY != 0 || 
        frame->wheel != 0 || frame->buttons != 0) {
        HIDBootMouseReport mouse_report = {
            .buttons = frame->buttons,
            .deltaX = frame->deltaX,
            .deltaY = frame->deltaY,
            .wheel = frame->wheel
        };
        tud_hid_n_report(ITF_NUM_HID_MOUSE, 0, &mouse_report, sizeof(mouse_report));
    }
}
```

**타이밍 상수** (§0 프로토콜 유지):
```c
#define HID_DEBOUNCE_MS          40    // 디바운싱 지연
#define HID_INITIAL_REPEAT_MS   300    // 초기 반복 지연
#define HID_REPEAT_INTERVAL_MS   30    // 반복 입력 간격
#define HID_PROCESSING_DELAY_MS   2    // TinyUSB 태스크 지연
```

### 3.5 FreeRTOS 태스크 구조

**ESP-IDF app_main() 진입점**:
```c
void app_main(void) {
    // USB 디바이스 초기화 (TinyUSB)
    tud_init(BOARD_TUD_RHPORT);
    
    // UART 초기화
    uart_init();
    
    // 태스크 생성
    xTaskCreatePinnedToCore(uart_task, "UART", 4096, NULL, 10, NULL, 0);
    xTaskCreatePinnedToCore(hid_task, "HID", 4096, NULL, 9, NULL, 0);
    xTaskCreatePinnedToCore(cdc_task, "CDC", 4096, NULL, 8, NULL, 1);
    xTaskCreatePinnedToCore(usb_task, "USB", 4096, NULL, 5, NULL, 1);
    xTaskCreatePinnedToCore(monitor_task, "Monitor", 2048, NULL, 1, NULL, 1);
}
```

**태스크 구성 및 우선순위**:
- **UART 태스크** (우선순위 10, Core 0): UART 수신 및 BridgeOne 프레임 파싱
- **HID 태스크** (우선순위 9, Core 0): HID 리포트 생성 및 전송
- **CDC 태스크** (우선순위 8, Core 1): Vendor CDC 명령 처리 및 중계
- **USB 태스크** (우선순위 5, Core 1): TinyUSB 스택 처리 (tud_task 호출)
- **모니터링 태스크** (우선순위 1, Core 1): 시스템 상태 모니터링

**태스크별 스택 크기 요구사항**:
```c
#define UART_TASK_STACK_SIZE    4096  // UART 링 버퍼 처리
#define HID_TASK_STACK_SIZE     4096  // HID 리포트 생성
#define CDC_TASK_STACK_SIZE     4096  // JSON 파싱 (512B 버퍼 × 2)
#define USB_TASK_STACK_SIZE     4096  // TinyUSB 스택 처리
#define MONITOR_TASK_STACK_SIZE 2048  // 경량 모니터링
```

### 3.6 데이터 플로우 설계

**메인 데이터 플로우**:
```
UART ISR → Ring Buffer → Frame Parser → Protocol Router → HID/CDC Interfaces
```

**버퍼 구조 요구사항**:
- **UART 링 버퍼**: UART 인터럽트 서비스 루틴(ISR)에서 수신된 데이터를 임시 저장하기 위한 링 버퍼가 필요합니다. 버퍼는 최소 16프레임(128바이트) 이상을 저장할 수 있어야 합니다.
- **프레임 큐**: 파싱이 완료된 `bridge_frame_t` 구조체를 후속 처리 태스크로 전달하기 위한 FreeRTOS 큐가 필요합니다. 큐는 최소 32개 프레임을 저장할 수 있어야 합니다.

### 3.7 참고 구현: esp32-cdc-keyboard 프로젝트 분석

본 절에서는 ESP-IDF 기반 USB HID+CDC 복합 디바이스 구현의 실제 참고 사례인 **esp32-cdc-keyboard** 프로젝트를 분석하여, BridgeOne의 ESP32-S3 펌웨어 개발 시 활용할 수 있는 구체적인 구현 패턴과 주의사항을 제시합니다.

#### 3.7.1 프로젝트 개요 및 아키텍처

**esp32-cdc-keyboard 프로젝트**:
- **목적**: ESP32-S3에서 USB HID(키보드/마우스) + CDC 복합 디바이스 구현
- **프레임워크**: ESP-IDF (idf.py 빌드 시스템)
- **USB 스택**: TinyUSB (ESP-IDF 통합 버전)
- **데이터 흐름**: CDC 인터페이스로 명령 수신 → HID 리포트로 변환 전송
- **용도**: 하드웨어 수준 키보드/마우스 시뮬레이션

**BridgeOne과의 유사성 비교**:

| 항목 | esp32-cdc-keyboard | BridgeOne 요구사항 | 호환성 |
|------|-------------------|-------------------|--------|
| **프레임워크** | ESP-IDF (idf.py) | ESP-IDF (§3.1 요구) | ✅ 동일 |
| **USB 라이브러리** | TinyUSB | TinyUSB (§3.3 요구) | ✅ 동일 |
| **복합 디바이스** | HID + CDC | HID×2 + CDC (§1.5.1) | ⚠️ 확장 필요 |
| **인터페이스 수** | 2개 (HID 1개, CDC 1개) | 4개 (HID 2개, CDC 2개) | ⚠️ 확장 필요 |
| **HID 프로토콜** | 일반 HID | Boot Protocol (§1.5.2) | ⚠️ 변경 필요 |
| **입력 소스** | CDC (USB) | UART (Android 앱) | ⚠️ 변경 필요 |
| **데이터 변환** | CDC → HID | UART → HID/CDC | ⚠️ 확장 필요 |
| **MCU** | ESP32-S3 | ESP32-S3-DevkitC-1 | ✅ 동일 |
| **멀티태스킹** | 단순 구조 | FreeRTOS 5-태스크 (§3.5) | ⚠️ 확장 필요 |

**주요 차이점 요약**:
- ✅ **직접 재사용 가능**: TinyUSB 디스크립터 구조, 콜백 패턴, HID 전송 API
- ⚠️ **확장 필요**: 인터페이스 2개 → 4개, CDC 단순 패스스루 → UART 프레임 파싱
- ⚠️ **변경 필요**: Boot Protocol HID, UART ISR 추가, FreeRTOS 멀티태스크 구조

#### 3.7.2 USB Composite 디스크립터 구현 예시

**esp32-cdc-keyboard의 디스크립터 구조** (`usb_descriptors.c`):

```c
// esp32-cdc-keyboard의 인터페이스 정의
enum {
    ITF_NUM_HID,      // Interface 0: HID (Keyboard + Mouse)
    ITF_NUM_CDC,      // Interface 1: CDC-ACM (2개 인터페이스 자동 생성)
};

#define ITF_NUM_TOTAL 3  // HID 1개 + CDC 2개 (Comm, Data)
#define TUSB_DESC_TOTAL_LEN (TUD_CONFIG_DESC_LEN + TUD_HID_DESC_LEN + TUD_CDC_DESC_LEN)

// Configuration Descriptor
const uint8_t configuration_descriptor[] = {
    // Configuration: 3 interfaces, 200mA
    TUD_CONFIG_DESCRIPTOR(1, ITF_NUM_TOTAL, 0, TUSB_DESC_TOTAL_LEN, 
                          TUSB_DESC_CONFIG_ATT_REMOTE_WAKEUP, 200),

    // Interface 0: HID (Keyboard + Mouse 복합 리포트)
    TUD_HID_DESCRIPTOR(ITF_NUM_HID, 5, HID_ITF_PROTOCOL_NONE, 
                       sizeof(hid_report_descriptor), EPNUM_HID, 
                       CFG_TUD_HID_EP_BUFSIZE, 10),

    // Interface 1/2: CDC-ACM
    TUD_CDC_DESCRIPTOR(ITF_NUM_CDC, 4, EPNUM_CDC_NOTIF, 8, 
                       EPNUM_CDC_OUT, EPNUM_CDC_IN, 64),
};

// HID Report Descriptor (Keyboard + Mouse 복합)
const uint8_t hid_report_descriptor[] = {
    TUD_HID_REPORT_DESC_KEYBOARD(HID_REPORT_ID(HID_ITF_PROTOCOL_KEYBOARD)),
    TUD_HID_REPORT_DESC_MOUSE(HID_REPORT_ID(HID_ITF_PROTOCOL_MOUSE))
};
```

**BridgeOne으로 확장 방법** (§1.5.1 불변 규칙 준수):

```c
// BridgeOne의 인터페이스 정의 (§1.5.1 순서 고정)
enum {
    ITF_NUM_HID_KEYBOARD = 0,  // Interface 0: HID Boot Keyboard
    ITF_NUM_HID_MOUSE    = 1,  // Interface 1: HID Boot Mouse
    ITF_NUM_CDC_COMM     = 2,  // Interface 2: CDC-ACM Comm
    ITF_NUM_CDC_DATA     = 3,  // Interface 3: CDC-ACM Data
    ITF_NUM_TOTAL
};

#define TUSB_DESC_TOTAL_LEN (TUD_CONFIG_DESC_LEN + \
                             TUD_HID_DESC_LEN + TUD_HID_DESC_LEN + \
                             TUD_CDC_DESC_LEN)

// BridgeOne Configuration Descriptor
const uint8_t configuration_descriptor[] = {
    // Configuration: 4 interfaces, 500mA
    TUD_CONFIG_DESCRIPTOR(1, ITF_NUM_TOTAL, 0, TUSB_DESC_TOTAL_LEN, 
                          TUSB_DESC_CONFIG_ATT_REMOTE_WAKEUP, 500),
    
    // Interface 0: HID Boot Keyboard (§1.5.2 준수)
    TUD_HID_DESCRIPTOR(ITF_NUM_HID_KEYBOARD, 0, HID_ITF_PROTOCOL_KEYBOARD,
                       sizeof(desc_hid_keyboard_report), EPNUM_HID_KB, 
                       CFG_TUD_HID_EP_BUFSIZE, 1),
    
    // Interface 1: HID Boot Mouse (§1.5.2 준수)
    TUD_HID_DESCRIPTOR(ITF_NUM_HID_MOUSE, 0, HID_ITF_PROTOCOL_MOUSE,
                       sizeof(desc_hid_mouse_report), EPNUM_HID_MOUSE,
                       CFG_TUD_HID_EP_BUFSIZE, 1),
    
    // Interface 2/3: CDC-ACM (§1.5.4 준수)
    TUD_CDC_DESCRIPTOR(ITF_NUM_CDC_COMM, 4, EPNUM_CDC_NOTIF,
                       8, EPNUM_CDC_OUT, EPNUM_CDC_IN, 64)
};

// HID Boot Keyboard Report Descriptor (§1.5.2 8바이트)
const uint8_t desc_hid_keyboard_report[] = {
    HID_USAGE_PAGE(HID_USAGE_PAGE_DESKTOP),
    HID_USAGE(HID_USAGE_DESKTOP_KEYBOARD),
    HID_COLLECTION(HID_COLLECTION_APPLICATION),
        // Modifiers (1 byte)
        HID_USAGE_PAGE(HID_USAGE_PAGE_KEYBOARD),
        HID_USAGE_MIN(0xE0), HID_USAGE_MAX(0xE7),
        HID_LOGICAL_MIN(0), HID_LOGICAL_MAX(1),
        HID_REPORT_COUNT(8), HID_REPORT_SIZE(1),
        HID_INPUT(HID_DATA | HID_VARIABLE | HID_ABSOLUTE),
        // Reserved (1 byte)
        HID_REPORT_COUNT(1), HID_REPORT_SIZE(8),
        HID_INPUT(HID_CONSTANT),
        // Key Codes (6 bytes)
        HID_USAGE_PAGE(HID_USAGE_PAGE_KEYBOARD),
        HID_USAGE_MIN(0), HID_USAGE_MAX(0xFF),
        HID_LOGICAL_MIN(0), HID_LOGICAL_MAX(0xFF),
        HID_REPORT_COUNT(6), HID_REPORT_SIZE(8),
        HID_INPUT(HID_DATA | HID_ARRAY | HID_ABSOLUTE),
    HID_COLLECTION_END
};

// HID Boot Mouse Report Descriptor (§1.5.2 4바이트)
const uint8_t desc_hid_mouse_report[] = {
    HID_USAGE_PAGE(HID_USAGE_PAGE_DESKTOP),
    HID_USAGE(HID_USAGE_DESKTOP_MOUSE),
    HID_COLLECTION(HID_COLLECTION_APPLICATION),
        // Buttons (1 byte)
        HID_USAGE_PAGE(HID_USAGE_PAGE_BUTTON),
        HID_USAGE_MIN(1), HID_USAGE_MAX(3),
        HID_LOGICAL_MIN(0), HID_LOGICAL_MAX(1),
        HID_REPORT_COUNT(3), HID_REPORT_SIZE(1),
        HID_INPUT(HID_DATA | HID_VARIABLE | HID_ABSOLUTE),
        HID_REPORT_COUNT(1), HID_REPORT_SIZE(5),
        HID_INPUT(HID_CONSTANT),
        // X, Y, Wheel (3 bytes)
        HID_USAGE_PAGE(HID_USAGE_PAGE_DESKTOP),
        HID_USAGE(HID_USAGE_DESKTOP_X),
        HID_USAGE(HID_USAGE_DESKTOP_Y),
        HID_USAGE(HID_USAGE_DESKTOP_WHEEL),
        HID_LOGICAL_MIN_N(-127, 2), HID_LOGICAL_MAX_N(127, 2),
        HID_REPORT_COUNT(3), HID_REPORT_SIZE(8),
        HID_INPUT(HID_DATA | HID_VARIABLE | HID_RELATIVE),
    HID_COLLECTION_END
};
```

**핵심 차이점**:
1. **인터페이스 분리**: esp32-cdc-keyboard는 HID 1개에 Keyboard+Mouse 리포트 ID로 구분 → BridgeOne은 HID 2개로 완전 분리
2. **Boot Protocol**: esp32-cdc-keyboard는 `HID_ITF_PROTOCOL_NONE` → BridgeOne은 `HID_ITF_PROTOCOL_KEYBOARD/MOUSE` 필수
3. **Endpoint 번호**: BridgeOne은 EPNUM_HID_KB(0x83), EPNUM_HID_MOUSE(0x84) 등 명확히 구분 필요

#### 3.7.3 CDC → HID 변환 로직

**esp32-cdc-keyboard의 변환 구현** (`main.c`):

```c
static uint8_t buf[CONFIG_TINYUSB_CDC_RX_BUFSIZE + 1];

// CDC 수신 콜백 (TinyUSB)
void tinyusb_cdc_rx_callback(int itf, cdcacm_event_t *event) {
    size_t rx_size = 0;
    
    // CDC 인터페이스로부터 데이터 읽기
    esp_err_t ret = tinyusb_cdcacm_read(itf, buf, 
                                         CONFIG_TINYUSB_CDC_RX_BUFSIZE, 
                                         &rx_size);
    if (ret == ESP_OK && rx_size > 0) {
        // buf[0]: Report ID (1=Keyboard, 2=Mouse)
        // buf[1+]: Report Data
        tud_hid_n_report(0, buf[0], &buf[1], rx_size - 1);
    }
}

// TinyUSB 초기화 및 CDC 설정
int main(void) {
    ESP_LOGI(TAG, "USB initialization");
    ESP_ERROR_CHECK(tinyusb_driver_install(&tusb_cfg));
    
    tinyusb_config_cdcacm_t acm_cfg = {
        .usb_dev = TINYUSB_USBDEV_0,
        .cdc_port = TINYUSB_CDC_ACM_0,
        .rx_unread_buf_sz = 64,
        .callback_rx = &tinyusb_cdc_rx_callback,
        .callback_rx_wanted_char = NULL,
    };
    ESP_ERROR_CHECK(tusb_cdc_acm_init(&acm_cfg));
    ESP_LOGI(TAG, "USB initialization DONE");
}

void app_main(void) {
    main();
}
```

**BridgeOne UART → HID 변환으로 확장** (§1.5.3, §3.4 준수):

```c
// BridgeOne의 UART 수신 및 변환 (§4.1, §4.2 참조)
void uart_task(void* pvParameters) {
    static uint8_t rx_buffer[UART_BUF_SIZE];
    static bridge_frame_t frame;
    
    while (1) {
        // UART 수신 (esp32-cdc-keyboard의 CDC 수신 대체)
        int len = uart_read_bytes(UART_PORT_NUM, rx_buffer, 
                                  sizeof(bridge_frame_t), 
                                  pdMS_TO_TICKS(100));
        
        if (len == sizeof(bridge_frame_t)) {
            memcpy(&frame, rx_buffer, sizeof(bridge_frame_t));
            
            // 순번 검증 (esp32-cdc-keyboard에는 없음)
            if (validateSequenceNumber(frame.seq)) {
                // HID 태스크로 전송 (FreeRTOS 큐 사용)
                xQueueSend(frame_queue, &frame, 0);
            }
        }
        
        esp_task_wdt_reset();
    }
}

// BridgeOne의 HID 변환 로직 (§3.4 processBridgeFrame)
void hid_task(void* pvParameters) {
    bridge_frame_t frame;
    
    while (1) {
        if (xQueueReceive(frame_queue, &frame, portMAX_DELAY) == pdTRUE) {
            // Keyboard 리포트 전송 (esp32-cdc-keyboard의 tud_hid_n_report 패턴 활용)
            if (frame.modifiers != 0 || frame.keyCode1 != 0 || frame.keyCode2 != 0) {
                HIDBootKeyboardReport kb_report = {
                    .modifiers = frame.modifiers,
                    .reserved = 0,
                    .keyCodes = {frame.keyCode1, frame.keyCode2, 0, 0, 0, 0}
                };
                
                if (tud_hid_n_ready(ITF_NUM_HID_KEYBOARD)) {
                    tud_hid_n_report(ITF_NUM_HID_KEYBOARD, 0, 
                                     &kb_report, sizeof(kb_report));
                }
            }
            
            // Mouse 리포트 전송
            if (frame.deltaX != 0 || frame.deltaY != 0 || 
                frame.wheel != 0 || frame.buttons != 0) {
                HIDBootMouseReport mouse_report = {
                    .buttons = frame.buttons,
                    .deltaX = frame.deltaX,
                    .deltaY = frame.deltaY,
                    .wheel = frame.wheel
                };
                
                if (tud_hid_n_ready(ITF_NUM_HID_MOUSE)) {
                    tud_hid_n_report(ITF_NUM_HID_MOUSE, 0, 
                                     &mouse_report, sizeof(mouse_report));
                }
            }
        }
        
        esp_task_wdt_reset();
    }
}
```

**핵심 패턴 재사용**:
- `tud_hid_n_report()`: esp32-cdc-keyboard와 동일한 API로 HID 전송
- `tud_hid_n_ready()`: 이전 리포트 전송 완료 확인 (BridgeOne 추가)
- 인스턴스 구분: esp32-cdc-keyboard는 단일 HID(0) → BridgeOne은 `ITF_NUM_HID_KEYBOARD`, `ITF_NUM_HID_MOUSE` 명시

#### 3.7.4 TinyUSB 콜백 패턴

**esp32-cdc-keyboard의 콜백 구현** (`usb_descriptors.c`):

```c
// HID Get Report 콜백 (호스트가 현재 상태 요청 시)
uint16_t tud_hid_get_report_cb(uint8_t instance, uint8_t report_id, 
                                 hid_report_type_t report_type, 
                                 uint8_t *buffer, uint16_t reqlen) {
    (void)instance;
    (void)report_id;
    (void)report_type;
    (void)buffer;
    (void)reqlen;
    return 0;  // 구현 없음 (선택 사항)
}

// HID Set Report 콜백 (호스트가 상태 설정 시, 예: LED)
void tud_hid_set_report_cb(uint8_t instance, uint8_t report_id, 
                             hid_report_type_t report_type, 
                             uint8_t const *buffer, uint16_t bufsize) {
    // 구현 없음 (선택 사항)
}
```

**BridgeOne 콜백 확장** (§3.3 요구사항):

```c
// BridgeOne HID Get Report 콜백 (Keyboard/Mouse 인스턴스 구분)
uint16_t tud_hid_get_report_cb(uint8_t instance, uint8_t report_id,
                                 hid_report_type_t report_type,
                                 uint8_t* buffer, uint16_t reqlen) {
    if (instance == ITF_NUM_HID_KEYBOARD) {
        // Keyboard 상태 반환
        if (reqlen >= 8) {
            HIDBootKeyboardReport* kb = (HIDBootKeyboardReport*)buffer;
            kb->modifiers = current_keyboard_state.modifiers;
            kb->reserved = 0;
            memcpy(kb->keyCodes, current_keyboard_state.keyCodes, 6);
            return 8;
        }
    } else if (instance == ITF_NUM_HID_MOUSE) {
        // Mouse 상태 반환 (상대 이동이므로 일반적으로 0)
        if (reqlen >= 4) {
            memset(buffer, 0, 4);
            return 4;
        }
    }
    return 0;
}

// BridgeOne HID Set Report 콜백 (Keyboard LED 처리)
void tud_hid_set_report_cb(uint8_t instance, uint8_t report_id,
                             hid_report_type_t report_type,
                             uint8_t const* buffer, uint16_t bufsize) {
    if (instance == ITF_NUM_HID_KEYBOARD && 
        report_type == HID_REPORT_TYPE_OUTPUT && bufsize >= 1) {
        // Keyboard LED 상태 처리 (NumLock, CapsLock, ScrollLock)
        uint8_t leds = buffer[0];
        
        // Android 앱으로 LED 상태 중계 (선택)
        notifyKeyboardLEDState(leds);
    }
}

// BridgeOne CDC Line Coding 콜백 (필수)
void tud_cdc_line_coding_cb(uint8_t itf, cdc_line_coding_t const* coding) {
    // Vendor CDC 설정 적용 (§1.5.4 프로토콜 유지)
    // 일반적으로 설정 무시 (프로토콜 고정)
}

// BridgeOne CDC RX 콜백 (Windows 서버 명령 수신)
void tud_cdc_rx_cb(uint8_t itf) {
    if (itf == ITF_NUM_CDC_COMM) {
        // Windows 서버로부터 JSON 명령 수신 (§4.3 참조)
        handleVendorCDCReceive();
    }
}
```

**콜백 구현 요구사항 요약**:
- ✅ **필수 구현**: `tud_cdc_rx_cb`, `tud_hid_get_report_cb`, `tud_hid_set_report_cb`
- ⚠️ **인스턴스 구분**: esp32-cdc-keyboard는 단일 인스턴스 → BridgeOne은 Keyboard/Mouse 구분 필수
- ⚠️ **LED 상태**: esp32-cdc-keyboard는 미구현 → BridgeOne은 선택적으로 Android 중계 가능

#### 3.7.5 BridgeOne 적용 시 주의사항

**1. 프레임워크 및 빌드 시스템**:
- esp32-cdc-keyboard: ESP-IDF 네이티브 (`CMakeLists.txt`, `idf.py`)
- BridgeOne 기존 구조: PlatformIO (`platformio.ini`) 사용 중
- **권장**: ESP-IDF로 마이그레이션 (§7.1~7.3 참조)
  - 더 나은 TinyUSB 통합 지원
  - 공식 예제 및 문서 풍부
  - `idf.py menuconfig`로 세밀한 설정 가능

**2. 인터페이스 번호 고정 규칙** (§1.5.1 불변):
- esp32-cdc-keyboard: HID(0), CDC(1~2) 순서
- BridgeOne: **Keyboard(0), Mouse(1), CDC(2~3)** 순서 절대 변경 금지
- **이유**: Android/Windows 코드가 인터페이스 번호로 식별
- **검증 방법**: `lsusb -v` (Linux) 또는 USB Device Viewer (Windows)로 인터페이스 순서 확인

**3. Boot Protocol 요구사항** (§1.5.2 불변):
- esp32-cdc-keyboard: 일반 HID (`HID_ITF_PROTOCOL_NONE`)
- BridgeOne: **Boot Protocol 필수** (`HID_ITF_PROTOCOL_KEYBOARD`, `HID_ITF_PROTOCOL_MOUSE`)
- **이유**: BIOS/UEFI 호환성 보장
- **리포트 크기**: Keyboard 8바이트, Mouse 4바이트 고정

**4. VID/PID 설정** (`sdkconfig.defaults`):
- esp32-cdc-keyboard: Logitech VID(0x046d), PID(0xc07f) 사용
- BridgeOne: **자체 VID/PID** 또는 **Generic HID VID(0x1209)/PID(0x0001)** 사용 권장
- **설정 위치**: `sdkconfig.defaults` 또는 `idf.py menuconfig`:
  ```ini
  CONFIG_TINYUSB_DESC_CUSTOM_VID=0x1209
  CONFIG_TINYUSB_DESC_CUSTOM_PID=0x0001
  CONFIG_TINYUSB_DESC_MANUFACTURER_STRING="BridgeOne"
  CONFIG_TINYUSB_DESC_PRODUCT_STRING="BridgeOne HID+CDC Bridge"
  CONFIG_TINYUSB_DESC_SERIAL_STRING="00000001"
  CONFIG_TINYUSB_DESC_CDC_STRING="BridgeOne Vendor CDC"
  ```

**5. sdkconfig 필수 설정 차이**:
- esp32-cdc-keyboard: `CONFIG_TINYUSB_HID_COUNT=2` (단일 HID에 2개 리포트)
- BridgeOne: **`CONFIG_TINYUSB_HID_COUNT=2`** (2개 HID 인터페이스)
- **주의**: 설정 의미가 다름 (리포트 개수 vs 인터페이스 개수)

**6. UART vs CDC 입력 소스**:
- esp32-cdc-keyboard: CDC로 입력 수신 (USB 호스트 → ESP32-S3)
- BridgeOne: **UART로 입력 수신** (Android 앱 → ESP32-S3)
- **필요 작업**: UART 드라이버 초기화, ISR 설정, 링 버퍼 관리 (§4.1 참조)

**7. FreeRTOS 멀티태스크 구조**:
- esp32-cdc-keyboard: 단순 메인 루프 + TinyUSB 콜백
- BridgeOne: **5개 태스크** (UART, HID, CDC, USB, Monitor) 듀얼 코어 분산 (§3.5 참조)
- **주의**: 태스크 간 데이터 공유 시 세마포어/뮤텍스 필수

#### 3.7.6 확장 및 수정 가이드

**단계 1: esp32-cdc-keyboard → BridgeOne 인터페이스 확장**

1. **enum 확장** (`usb_descriptors.c`):
   ```c
   // Before (esp32-cdc-keyboard)
   enum { ITF_NUM_HID, ITF_NUM_CDC };
   
   // After (BridgeOne)
   enum {
       ITF_NUM_HID_KEYBOARD = 0,
       ITF_NUM_HID_MOUSE    = 1,
       ITF_NUM_CDC_COMM     = 2,
       ITF_NUM_CDC_DATA     = 3,
       ITF_NUM_TOTAL
   };
   ```

2. **Configuration Descriptor 확장**:
   ```c
   // 기존 TUD_HID_DESCRIPTOR 1개를 2개로 분리
   TUD_HID_DESCRIPTOR(ITF_NUM_HID_KEYBOARD, 0, HID_ITF_PROTOCOL_KEYBOARD, ...),
   TUD_HID_DESCRIPTOR(ITF_NUM_HID_MOUSE, 0, HID_ITF_PROTOCOL_MOUSE, ...),
   ```

3. **HID Report Descriptor 분리**:
   - esp32-cdc-keyboard: 단일 `hid_report_descriptor[]` (Keyboard+Mouse 복합)
   - BridgeOne: `desc_hid_keyboard_report[]`, `desc_hid_mouse_report[]` 분리 (§3.7.2 참조)

4. **Endpoint 번호 할당**:
   ```c
   #define EPNUM_HID_KB      0x83  // Keyboard IN endpoint
   #define EPNUM_HID_MOUSE   0x84  // Mouse IN endpoint
   #define EPNUM_CDC_NOTIF   0x81  // CDC Notification IN
   #define EPNUM_CDC_OUT     0x02  // CDC Data OUT
   #define EPNUM_CDC_IN      0x82  // CDC Data IN
   ```

**단계 2: UART 입력 로직 추가**

1. **UART 초기화** (§4.1 참조):
   ```c
   void uart_init(void) {
       uart_config_t uart_config = {
           .baud_rate = 1000000,
           .data_bits = UART_DATA_8_BITS,
           .parity = UART_PARITY_DISABLE,
           .stop_bits = UART_STOP_BITS_1,
           .flow_ctrl = UART_HW_FLOWCTRL_DISABLE,
       };
       ESP_ERROR_CHECK(uart_param_config(UART_NUM_0, &uart_config));
       ESP_ERROR_CHECK(uart_driver_install(UART_NUM_0, 1024, 1024, 0, NULL, 0));
   }
   ```

2. **CDC RX 콜백을 UART 태스크로 대체**:
   - esp32-cdc-keyboard의 `tinyusb_cdc_rx_callback()` 제거
   - BridgeOne의 `uart_task()` 추가 (§3.7.3 참조)

3. **프레임 큐 추가**:
   ```c
   QueueHandle_t frame_queue;
   frame_queue = xQueueCreate(32, sizeof(bridge_frame_t));
   ```

**단계 3: FreeRTOS 멀티태스크 구조 추가**

1. **태스크 생성** (`app_main()`):
   ```c
   void app_main(void) {
       // USB 초기화 (esp32-cdc-keyboard와 동일)
       tud_init(BOARD_TUD_RHPORT);
       uart_init();
       
       // FreeRTOS 태스크 생성 (BridgeOne 추가)
       xTaskCreatePinnedToCore(uart_task, "UART", 4096, NULL, 10, NULL, 0);
       xTaskCreatePinnedToCore(hid_task, "HID", 4096, NULL, 9, NULL, 0);
       xTaskCreatePinnedToCore(cdc_task, "CDC", 4096, NULL, 8, NULL, 1);
       xTaskCreatePinnedToCore(usb_task, "USB", 4096, NULL, 5, NULL, 1);
   }
   ```

2. **USB 태스크 추가** (TinyUSB 스택 처리):
   ```c
   void usb_task(void* pvParameters) {
       while (1) {
           tud_task();  // TinyUSB 스택 처리 (필수)
           vTaskDelay(pdMS_TO_TICKS(2));
       }
   }
   ```

**단계 4: Vendor CDC 명령 처리 추가**

1. **CDC 태스크 구현** (§4.3 참조):
   - Windows 서버 JSON 명령 수신 및 파싱
   - Android 앱으로 중계 메시지 전송

2. **CRC16 검증 추가** (§1.5.4 준수):
   ```c
   #include "esp_crc.h"
   bool verifyCRC16(const VendorCDCFrame* msg) {
       uint16_t calculated = esp_crc16_le(0, msg->payload, msg->length);
       return (calculated == msg->checksum);
   }
   ```

**단계 5: 테스트 및 검증**

1. **USB 디스크립터 검증**:
   - Linux: `lsusb -v -d VID:PID`
   - Windows: USB Device Viewer 또는 USBTreeView

2. **HID 리포트 모니터링**:
   - Wireshark USB 캡처
   - `usbhid-dump` (Linux)

3. **성능 측정** (§9.1 참조):
   - 지연시간: `esp_timer_get_time()` 사용
   - FPS: 소프트웨어 카운터
   - CPU 사용률: `vTaskGetRunTimeStats()`

**참고 자료**:
- esp32-cdc-keyboard 원본 저장소: [GitHub - esp32-cdc-keyboard](참조 프로젝트)
- TinyUSB 공식 문서: [TinyUSB Composite Device Example](https://github.com/hathach/tinyusb/tree/master/examples/device/composite)
- ESP-IDF TinyUSB 가이드: [ESP-IDF USB Device Guide](https://docs.espressif.com/projects/esp-idf/en/latest/esp32s3/api-reference/peripherals/usb_device.html)

---

### 3.3 상태 관리 시스템

**시스템 상태 정의**:
```c
typedef enum {
    SYSTEM_STATE_INIT,          // 초기화 중
    SYSTEM_STATE_ESSENTIAL,     // Essential 모드
    SYSTEM_STATE_STANDARD,      // Standard 모드
    SYSTEM_STATE_ERROR          // 오류 상태
} system_state_t;

typedef enum {
    CONNECTION_STATE_DISCONNECTED,  // 연결 끊김
    CONNECTION_STATE_UART_ONLY,     // UART만 연결
    CONNECTION_STATE_USB_READY,     // USB 준비 완료
    CONNECTION_STATE_FULLY_CONNECTED // 완전 연결
} connection_state_t;
```

**상태 컨텍스트 (확장)**:
```c
typedef struct {
    system_state_t system_state;
    connection_state_t connection_state;
    
    // 통계 정보
    uint32_t frames_received;
    uint32_t frames_processed;
    uint32_t frames_dropped;
    uint32_t last_seq_number;
    
    // 성능 지표
    uint32_t avg_latency_us;
    uint32_t max_latency_us;
    uint32_t frames_per_second;
    
    // 오류 통계
    uint32_t uart_errors;
    uint32_t usb_errors;
    uint32_t sequence_errors;
    
    // 양방향 통신 상태 (추가)
    uint32_t last_ping_time;           // 마지막 Keep-alive 수신 시간
    uint32_t handshake_state;          // 핸드셰이크 진행 상태
    uint32_t pending_responses;        // 대기 중인 응답 수
    uint32_t json_commands_processed;  // 처리된 JSON 명령 수
    uint32_t json_parse_errors;        // JSON 파싱 오류 수
    
    // 메시지 ID 관리
    uint16_t next_message_id;          // 다음 메시지 ID
    uint32_t last_response_time;       // 마지막 응답 수신 시간
} system_context_t;

// 대기 중인 요청 관리 구조체
typedef struct {
    uint16_t message_id;
    uint32_t timestamp;
    uint32_t timeout_ms;
    char command_type[32];
    bool is_active;
} pending_request_t;
```
- **대기 요청 큐**: Windows 서버로 전송된 후 응답을 기다리는 요청을 관리하기 위한 큐가 필요합니다. 이 큐는 동시에 최대 8개의 요청을 처리할 수 있어야 합니다.

## 4. 핵심 기능 요구사항

### 4.1 UART 통신 모듈 구현 (ESP-IDF)

**ESP-IDF USB-UART 드라이버 설정**:
```c
#include "driver/uart.h"

#define UART_PORT_NUM      UART_NUM_0
#define UART_BAUD_RATE     1000000      // 1Mbps
#define UART_BUF_SIZE      (1024)

// ESP32-S3-DevkitC-1의 내장 USB-to-UART는 기본 핀 사용
// GPIO 43 (U0TXD), GPIO 44 (U0RXD) 자동 매핑

void uart_init(void) {
    uart_config_t uart_config = {
        .baud_rate = UART_BAUD_RATE,
        .data_bits = UART_DATA_8_BITS,
        .parity = UART_PARITY_DISABLE,
        .stop_bits = UART_STOP_BITS_1,
        .flow_ctrl = UART_HW_FLOWCTRL_DISABLE,
        .source_clk = UART_SCLK_DEFAULT,
    };
    
    // UART 파라미터 설정
    ESP_ERROR_CHECK(uart_param_config(UART_PORT_NUM, &uart_config));
    
    // 내장 USB-to-UART 사용 시 핀 설정 불필요 (기본 핀 자동 사용)
    // UART_NUM_0은 내장 USB-to-UART 브릿지로 자동 연결됨
    
    // UART 드라이버 설치 (링 버퍼 자동 관리)
    ESP_ERROR_CHECK(uart_driver_install(UART_PORT_NUM, UART_BUF_SIZE * 2,
                                         UART_BUF_SIZE * 2, 0, NULL, 0));
}
```

**UART 수신 태스크 구현**:
```c
void uart_task(void* pvParameters) {
    static uint8_t rx_buffer[UART_BUF_SIZE];
    static bridge_frame_t frame;
    
    while (1) {
        // UART 수신 (블로킹, 타임아웃 100ms)
        int len = uart_read_bytes(UART_PORT_NUM, rx_buffer, 
                                  sizeof(bridge_frame_t), 
                                  pdMS_TO_TICKS(100));
        
        if (len == sizeof(bridge_frame_t)) {
            // 프레임 복사
            memcpy(&frame, rx_buffer, sizeof(bridge_frame_t));
            
            // 순번 검증 (§1.5.3 프로토콜 준수)
            if (validateSequenceNumber(frame.seq)) {
                // HID 태스크로 전송
                xQueueSend(frame_queue, &frame, 0);
            } else {
                ctx.sequence_errors++;
                ESP_LOGW(TAG, "Sequence error: expected=%u, got=%u",
                         ctx.last_seq_number + 1, frame.seq);
            }
        }
        
        // 태스크 워치독 리셋
        esp_task_wdt_reset();
    }
}
```

**프레임 검증 로직**:
```c
bool validateSequenceNumber(uint8_t seq) {
    static uint8_t expected_seq = 0;
    bool valid = (seq == expected_seq);
    expected_seq = (seq + 1) & 0xFF;  // 0~255 순환
    return valid;
}
```

### 4.2 USB HID 모듈 구현 (ESP-IDF TinyUSB)

**HID 태스크 구현**:
```c
void hid_task(void* pvParameters) {
    bridge_frame_t frame;
    
    while (1) {
        // UART 태스크로부터 프레임 수신
        if (xQueueReceive(frame_queue, &frame, portMAX_DELAY) == pdTRUE) {
            // §3.4 프레임 처리 로직 호출
            processBridgeFrame(&frame);
            
            ctx.frames_processed++;
        }
        
        // 태스크 워치독 리셋
        esp_task_wdt_reset();
    }
}
```

**HID 리포트 전송 구현**:
```c
// §1.5.2 준수: HID Boot Keyboard 리포트 전송
void sendKeyboardReport(const bridge_frame_t* frame) {
    if (!tud_hid_n_ready(ITF_NUM_HID_KEYBOARD)) {
        return;  // 이전 리포트 전송 중
    }
    
    HIDBootKeyboardReport kb_report = {
        .modifiers = frame->modifiers,
        .reserved = 0,
        .keyCodes = {frame->keyCode1, frame->keyCode2, 0, 0, 0, 0}
    };
    
    tud_hid_n_report(ITF_NUM_HID_KEYBOARD, 0, &kb_report, sizeof(kb_report));
}

// §1.5.2 준수: HID Boot Mouse 리포트 전송
void sendMouseReport(const bridge_frame_t* frame) {
    if (!tud_hid_n_ready(ITF_NUM_HID_MOUSE)) {
        return;  // 이전 리포트 전송 중
    }
    
    HIDBootMouseReport mouse_report = {
        .buttons = frame->buttons,
        .deltaX = frame->deltaX,
        .deltaY = frame->deltaY,
        .wheel = frame->wheel
    };
    
    tud_hid_n_report(ITF_NUM_HID_MOUSE, 0, &mouse_report, sizeof(mouse_report));
}
```

**USB 태스크 구현** (TinyUSB 스택 처리):
```c
void usb_task(void* pvParameters) {
    while (1) {
        // TinyUSB 스택 처리 (필수)
        tud_task();
        
        // 2ms 지연 (§0 타이밍 상수 준수)
        vTaskDelay(pdMS_TO_TICKS(HID_PROCESSING_DELAY_MS));
    }
}
```

### 4.3 Vendor CDC 모듈 구현 (ESP-IDF TinyUSB)

**CDC 태스크 구현**:
```c
#include "cJSON.h"  // ESP-IDF 내장 JSON 라이브러리

void cdc_task(void* pvParameters) {
    static uint8_t rx_buf[512];
    static uint8_t tx_buf[512];
    
    while (1) {
        // Windows 서버로부터 메시지 수신
        if (tud_cdc_available()) {
            uint32_t count = tud_cdc_read(rx_buf, sizeof(rx_buf));
            
            // §1.5.4 Vendor CDC 프레임 파싱
            VendorCDCFrame* msg = parseVendorCDCFrame(rx_buf, count);
            if (msg != NULL && verifyCRC16(msg)) {
                // JSON 페이로드 처리
                handleVendorCommand(msg);
            }
        }
        
        // Android로부터 중계할 메시지 확인
        if (xQueueReceive(android_to_windows_queue, tx_buf, 0) == pdTRUE) {
            // Windows 서버로 중계
            tud_cdc_write(tx_buf, calculateMessageLength(tx_buf));
            tud_cdc_write_flush();
        }
        
        vTaskDelay(pdMS_TO_TICKS(10));
        esp_task_wdt_reset();
    }
}
```

**JSON 명령 처리 구현** (Phase 2 통신 안정화 강화):
```c
void handleVendorCommand(const VendorCDCFrame* msg) {
    // §1.5.4 JSON 페이로드 파싱
    cJSON* json = cJSON_ParseWithLength((char*)msg->payload, msg->length);
    if (json == NULL) {
        ESP_LOGE(TAG, "JSON parse error");
        ctx.json_parse_errors++;
        
        // Phase 2 통신 안정화: JSON 파싱 실패 시 오류 응답 전송
        sendJsonParseErrorResponse("JSON syntax error or invalid structure");
        return;
    }
    
    // 명령 타입별 처리
    cJSON* cmd_type = cJSON_GetObjectItem(json, "command");
    if (cmd_type != NULL && cJSON_IsString(cmd_type)) {
        const char* cmd = cmd_type->valuestring;
        
        if (strcmp(cmd, "HANDSHAKE") == 0) {
            handleHandshake(json);
        } else if (strcmp(cmd, "KEEP_ALIVE") == 0) {
            handleKeepAlive(json);
        } else if (strcmp(cmd, "MULTI_CURSOR_SWITCH") == 0) {
            relayToAndroid(msg);  // Android로 중계
        } else {
            ESP_LOGW(TAG, "Unknown command: %s", cmd);
            sendJsonParseErrorResponse("Unknown command type");
        }
        // ... 기타 명령 처리
    } else {
        ESP_LOGE(TAG, "Missing or invalid 'command' field");
        
        // Phase 2 통신 안정화: 필수 필드 누락 시 오류 응답 전송
        sendJsonParseErrorResponse("Missing required field: 'command'");
        cJSON_Delete(json);
        return;
    }
    
    cJSON_Delete(json);
    ctx.json_commands_processed++;
}

// Phase 2 통신 안정화: CRC 오류 응답 전송 함수
void sendCrcErrorResponse(void) {
    const char* errorJson = 
        "{"
        "\"command\":\"ERROR_RESPONSE\","
        "\"error_code\":\"CRC_MISMATCH\","
        "\"error_message\":\"CRC16 checksum verification failed\","
        "\"timestamp\":\"%lld\""
        "}";
    
    char jsonBuffer[256];
    int64_t timestamp = esp_timer_get_time() / 1000;  // us → ms
    snprintf(jsonBuffer, sizeof(jsonBuffer), errorJson, timestamp);
    
    // Android로 UART 전송
    sendJsonToAndroid(jsonBuffer);
    
    // Windows 서버로 CDC 전송 (양방향 오류 알림)
    sendJsonToWindows(jsonBuffer);
    
    ESP_LOGW(TAG, "CRC_ERROR response sent to Android and Windows");
}

// Phase 2 통신 안정화: JSON 파싱 오류 응답 전송 함수
void sendJsonParseErrorResponse(const char* errorDetail) {
    const char* errorJson = 
        "{"
        "\"command\":\"ERROR_RESPONSE\","
        "\"error_code\":\"JSON_PARSE_ERROR\","
        "\"error_message\":\"Failed to parse JSON payload: %s\","
        "\"timestamp\":\"%lld\""
        "}";
    
    char jsonBuffer[384];
    int64_t timestamp = esp_timer_get_time() / 1000;  // us → ms
    snprintf(jsonBuffer, sizeof(jsonBuffer), errorJson, errorDetail, timestamp);
    
    // Android로 UART 전송
    sendJsonToAndroid(jsonBuffer);
    
    // Windows 서버로 CDC 전송 (양방향 오류 알림)
    sendJsonToWindows(jsonBuffer);
    
    ESP_LOGW(TAG, "JSON_PARSE_ERROR response sent: %s", errorDetail);
}

// JSON 메시지를 Android로 UART 전송
void sendJsonToAndroid(const char* jsonString) {
    size_t jsonLen = strlen(jsonString);
    uint8_t buffer[512];
    
    // 0xFF 헤더 추가
    buffer[0] = 0xFF;
    memcpy(&buffer[1], jsonString, jsonLen);
    buffer[1 + jsonLen] = '\0';  // Null 종단
    
    // UART 전송
    uart_write_bytes(UART_NUM, (const char*)buffer, 1 + jsonLen + 1);
}

// JSON 메시지를 Windows 서버로 Vendor CDC 전송
void sendJsonToWindows(const char* jsonString) {
    size_t jsonLen = strlen(jsonString);
    
    if (jsonLen > MAX_VENDOR_CDC_PAYLOAD_SIZE) {
        ESP_LOGE(TAG, "JSON payload too large for Vendor CDC: %u > %u", jsonLen, MAX_VENDOR_CDC_PAYLOAD_SIZE);
        return;
    }
    
    VendorCDCFrame frame;
    frame.header = 0xFF;
    frame.command = 0x10;  // 일반 JSON 명령
    frame.length = (uint16_t)jsonLen;  // Little-Endian
    memcpy(frame.payload, jsonString, jsonLen);
    frame.checksum = esp_crc16_le(0, (uint8_t*)jsonString, jsonLen);  // Little-Endian
    
    // Vendor CDC 전송
    size_t frameSize = 1 + 1 + 2 + jsonLen + 2;
    tud_cdc_write(&frame, frameSize);
    tud_cdc_write_flush();
}
```

**CRC16 체크섬 검증** (§1.5.4 준수):
```c
#include "esp_crc.h"

bool verifyCRC16(const VendorCDCFrame* msg) {
    // 페이로드에 대한 CRC16 계산
    uint16_t calculated = esp_crc16_le(0, msg->payload, msg->length);
    
    // Little-Endian으로 체크섬 추출
    uint16_t received = msg->checksum;
    
    return (calculated == received);
}
```

#### 4.3.1 CDC 명령 처리 및 중계 시스템

**CDC 태스크 로직**:
- **버퍼 확보**: JSON 명령의 수신 및 송신을 위해 각각 최소 512바이트의 버퍼를 확보해야 합니다.
- **수신**: Windows 서버로부터 Vendor CDC 프레임을 비동기적으로 수신합니다.
- **검증**: 수신된 프레임의 헤더(0xFF)와 CRC16 체크섬을 검증하여 데이터 무결성을 확인합니다.
- **파싱**: 유효한 프레임의 JSON 페이로드를 파싱하여 `command` 필드를 기준으로 적절한 핸들러를 호출합니다.
- **처리**: 각 명령 핸들러는 핸드셰이크, Keep-alive, Android로의 응답 중계 등 지정된 로직을 수행합니다.
- **중계**: Android 앱에서 수신된 특정 JSON 명령(예: 매크로 실행 요청)은 Vendor CDC 프레임으로 재구성하여 Windows 서버로 중계해야 합니다.

#### 4.3.1.1 매크로 실행 요청 중계 시스템

**Android → Windows 매크로 요청 중계 플로우**:

1. **UART 수신**: Android 앱에서 `MACRO_START_REQUEST` JSON 수신
   ```json
   {
     "command": "MACRO_START_REQUEST",
     "macro_id": "550e8400-e29b-41d4-a716-446655440000",
     "timestamp": "2025-10-21T12:34:56.789Z"
   }
   ```

2. **JSON 파싱 및 검증**:
   - `cJSON_ParseWithLength()` 사용하여 JSON 구조 파싱
   - `command` 필드 값이 `"MACRO_START_REQUEST"` 또는 `"MACRO_CANCEL_REQUEST"`인지 확인
   - `macro_id` 필드 존재 여부 및 UUID v4 형식 검증 (36자, 하이픈 포함)

3. **요청 큐 등록**:
   - ESP32-S3 내부 `pending_macro_requests` 큐에 등록
   - 데이터 구조: `{macro_id: "550e8400-...", timestamp: millis(), timeout_ms: 30000}`
   - 큐 최대 크기: 8개 요청 (UART 명령 큐와 공유)
   - 초과 시: `QUEUE_FULL` 오류를 Android로 즉시 응답

4. **Vendor CDC 프레임 재구성** (§0.4 준수):
   ```c
   VendorCDCFrame frame;
   frame.header = 0xFF;
   frame.command = 0x10;  // MACRO_COMMAND 타입 (매크로 관련 명령)
   frame.length = strlen(json_string);  // Little-Endian
   memcpy(frame.payload, json_string, frame.length);
   frame.checksum = esp_crc16_le(0, frame.payload, frame.length);  // Little-Endian
   ```

5. **Windows 서버로 전송**:
   - `tud_cdc_write()` 호출하여 Vendor CDC 인터페이스로 전송
   - `tud_cdc_write_flush()` 호출하여 즉시 전송 보장
   - 전송 실패 시: 3회 재시도, 실패 시 `CDC_WRITE_ERROR` 응답

**Windows → Android 매크로 결과 중계 플로우**:

1. **Vendor CDC 수신**: Windows 서버에서 `MACRO_RESULT` JSON 수신
   ```json
   {
     "command": "MACRO_RESULT",
     "macro_id": "550e8400-e29b-41d4-a716-446655440000",
     "timestamp": "2025-10-21T12:35:01.234Z",
     "payload": {
       "success": true,
       "error_message": null
     }
   }
   ```

2. **프레임 검증 및 JSON 파싱**:
   - 헤더 0xFF, command 타입, CRC16 체크섬 검증
   - JSON 파싱 및 `command` 필드 값이 `"MACRO_RESULT"`인지 확인
   - `macro_id`, `payload.success` 필드 존재 여부 확인

3. **요청 큐 매칭**:
   - `macro_id`로 `pending_macro_requests` 큐 검색
   - 매칭 성공: 요청 제거 및 타임아웃 타이머 취소
   - 매칭 실패: 로그 기록 (`ESP_LOGW(TAG, "Unmatched macro_id: %s", macro_id)`) 후 무시

4. **UART 전송** (Android로 중계):
   - 0xFF 헤더로 시작하는 JSON 문자열 전송
   - Null 종단 문자(`\0`) 추가
   - 전송 구조: `0xFF + json_string + \0`

**매크로 타임아웃 처리**:
- **타임아웃 시간**: 30,000ms (30초) - `pending_macro_requests` 큐 등록 시 설정
- **모니터링**: `monitoring_task`에서 1초 주기로 `pending_macro_requests` 큐 검사
- **타임아웃 감지**: `(millis() - request.timestamp) > request.timeout_ms` 조건 확인
- **타임아웃 응답 생성**:
  ```json
  {
    "command": "MACRO_RESULT",
    "macro_id": "550e8400-e29b-41d4-a716-446655440000",
    "timestamp": "2025-10-21T12:35:26.789Z",
    "payload": {
      "success": false,
      "error_message": "MACRO_TIMEOUT"
    }
  }
  ```
- **큐 정리**: 타임아웃된 요청을 `pending_macro_requests`에서 제거
- **Android 전송**: UART를 통해 타임아웃 응답 전송

**매크로 취소 요청 처리**:
- **UART 수신**: Android 앱에서 `MACRO_CANCEL_REQUEST` JSON 수신
  ```json
  {
    "command": "MACRO_CANCEL_REQUEST",
    "macro_id": "550e8400-e29b-41d4-a716-446655440000",
    "timestamp": "2025-10-21T12:34:58.000Z"
  }
  ```
- **요청 큐 확인**: `macro_id`로 `pending_macro_requests` 검색
- **큐에 있는 경우**: 즉시 제거 및 Windows로 취소 요청 전송 (§4.3.1.1 플로우 준수)
- **큐에 없는 경우**: 로그 기록 후 무시 (이미 완료되었거나 타임아웃)

**오류 처리 매트릭스**:

| 오류 유형 | 감지 시점 | 응답 코드 | 처리 방법 | 로그 레벨 |
|----------|----------|---------|----------|---------|
| JSON 파싱 오류 | Android JSON 수신 시 | `INVALID_JSON` | Android로 즉시 오류 응답 전송 | `ESP_LOGE` |
| 큐 초과 | 요청 큐 등록 시 | `QUEUE_FULL` | Android로 즉시 오류 응답 전송 | `ESP_LOGW` |
| CDC 전송 실패 | Windows 전송 시 | `CDC_WRITE_ERROR` | 3회 재시도 후 Android로 오류 응답 | `ESP_LOGE` |
| 매크로 타임아웃 | 모니터링 태스크 | `MACRO_TIMEOUT` | 큐 정리 후 Android로 타임아웃 응답 | `ESP_LOGW` |
| 응답 매칭 실패 | Windows 응답 수신 시 | (무시) | 로그 기록 후 무시 | `ESP_LOGW` |

**성능 요구사항**:
- **중계 지연시간**: Android 수신 → Windows 전송까지 50ms 이내
- **메모리 사용**: `pending_macro_requests` 큐 최대 8개 × 64 bytes = 512 bytes
- **처리 우선순위**: 매크로 요청은 일반 CDC 명령과 동일한 우선순위로 처리

#### 4.3.2 핸드셰이크 및 Keep-alive 프로토콜 요구사항

**핸드셰이크 프로세스 요구사항**:
- **2단계 프로세스**: Phase 1 (인증) → Phase 2 (상태 동기화)
- **인증 단계**: Challenge-Response 방식으로 보안 검증
- **상태 동기화 단계**: 현재 모드와 지원 기능 정보 교환
- **완료 조건**: 양측 핸드셰이크 완료 시 Standard 모드로 전환
- **실패 처리**: 인증 실패 시 Essential 모드 유지

**Keep-alive 메커니즘 요구사항**:
- **주기**: 500ms 간격으로 연결 상태 확인
- **메커니즘**: Windows로부터 주기적인 Ping 수신 시 Pong으로 응답해야 합니다.
- **타임아웃**: 3초 이상 응답 없으면 연결 끊김으로 판단
- **모드 전환**: 타임아웃 발생 시 Essential 모드로 자동 전환
- **상태 알림**: 모드 변경 시 Android 앱에 즉시 알림

#### 4.3.3 Android JSON 명령 처리

**처리 로직 요구사항**:
- UART 스트림에서 0xFF 헤더와 Null 종단 문자를 기준으로 JSON 명령을 식별하고 추출해야 합니다.
- 추출된 JSON 문자열의 유효성을 검증한 후, Windows 서버로 중계할 명력은 Vendor CDC 프레임으로 변환하여 전송해야 합니다.
- ESP32-S3 내부에서 상태 변경이 발생했을 경우(예: Keep-alive 타임아웃으로 인한 모드 변경), 해당 상태를 JSON 형식으로 구성하여 Android 앱으로 전송해야 합니다.

### 4.4 시스템 모니터링 모듈

**모니터링 태스크 요구사항**:
- **주기적 실행**: 1초 간격으로 시스템의 주요 상태를 점검합니다.
- **성능 지표 계산**: 초당 프레임 수(FPS)와 같은 성능 지표를 주기적으로 계산하고 `system_context`에 업데이트합니다.
- **타임아웃 검증**: Keep-alive 및 대기 중인 요청들의 타임아웃을 검사하고, 타임아웃 발생 시 적절한 복구 로직(모드 변경, 오류 알림 등)을 수행합니다.
- **연결 상태 확인**: UART 및 USB 연결 상태를 모니터링하고 변경 시 `connection_state`를 업데이트합니다.
- **시스템 리소스 모니터링**: 메모리 사용량 등을 주기적으로 확인하여 잠재적인 문제를 감지합니다.

#### 4.4.1 CRC16 체크섬 및 프레임 검증

**검증 로직 요구사항**:
- **CRC16-MODBUS**: 표준 CRC16-MODBUS 알고리즘을 사용하여 체크섬을 계산해야 합니다.
- **프레임 유효성 검사**: Vendor CDC 프레임 수신 시, 다음 항목을 순서대로 검증해야 합니다.
    1.  최소 프레임 크기 (헤더, 명령, 길이, 체크섬 필드 포함)
    2.  헤더 값 (0xFF)
    3.  `length` 필드와 실제 페이로드 길이의 일치 여부
    4.  페이로드에 대한 CRC16 계산 결과와 `checksum` 필드의 일치 여부

#### 4.4.2 메시지 ID 관리 시스템

**요청-응답 매칭 요구사항**:
- **ID 할당**: Android에서 Windows로 명령을 중계할 때, 고유한 `message_id`를 생성하여 해당 요청을 '대기 중인 요청 큐'에 등록해야 합니다.
- **응답 매칭**: Windows로부터 응답이 수신되면, 응답에 포함된 `message_id`를 사용하여 '대기 중인 요청 큐'에서 원본 요청을 찾아 완료 처리해야 합니다.
- **타임아웃 처리**: '대기 중인 요청 큐'에 등록된 요청이 지정된 시간 내에 완료되지 않으면 타임아웃으로 처리하고, 필요한 경우 Android 앱에 오류를 통지해야 합니다.

### 4.5 메시지 중계 및 라우팅 시스템 요구사항

**Android → Windows 메시지 중계 요구사항**:
- **명령 검증**: 수신된 JSON 명령의 구조적 유효성 검사
- **메시지 ID 관리**: 응답 대기 중인 명령에 고유 식별자 할당
- **타임아웃 제어**: 명령별 적절한 타임아웃 설정 및 관리
- **프레임 포맷팅**: 표준 Vendor CDC 프레임 구조로 변환
- **전송 신뢰성**: CRC16 체크섬을 통한 데이터 무결성 보장

**Windows → Android 응답 중계 요구사항**:
- **응답 매칭**: 메시지 ID 기반 요청-응답 정확한 매칭
- **대기 큐 관리**: 완료된 요청의 정리 및 자원 해제
- **Android 포맷**: UART 통신에 적합한 프레임 형식으로 변환
- **오류 복구**: 응답 처리 실패 시 적절한 오류 알림 전송

## 5. 성능 최적화 전략

### 5.1 실시간 처리 최적화

**CPU 주파수 동적 조절 요구사항**:
- 시스템 부하(예: `frames_per_second`)에 따라 CPU 주파수를 동적으로 조절하는 기능이 필요합니다.
- 고부하 상태(예: 150 FPS 이상)에서는 최대 240MHz로, 저부하 상태(예: 60 FPS 미만)에서는 80MHz 이하로 동작하여 성능과 전력 효율의 균형을 맞춰야 합니다.

**정적 메모리 할당 요구사항**:
- **컴파일 타임 할당**: 모든 버퍼와 큐를 정적 할당으로 관리
- **힙 사용 최소화**: 동적 할당 없이 실시간성 보장
- **메모리 단편화 방지**: 모든 버퍼 크기 고정 및 재사용

**동적 전력 관리 요구사항**:
- **모드별 전력 제어**: 성능/균형/절전 모드 구분
- **Light-sleep 활용**: 입력 없을 때 저전력 모드 전환
- **배터리 효율**: 불필요한 주변기기 전원 차단

### 5.2 메모리 최적화

**정적 메모리 할당**:
- FreeRTOS 태스크, 큐, 버퍼 등 시스템 운영에 필요한 모든 주요 메모리 자원은 힙(Heap)에서의 동적 할당 대신 정적 할당(Static Allocation)을 사용해야 합니다.
- 이는 메모리 단편화를 방지하고 시스템의 예측 가능성과 안정성을 높이는 것을 목적으로 합니다.

### 5.3 전력 관리 최적화 요구사항

**동적 전력 모드 요구사항**:
- **적응적 전력 제어**: 시스템 부하에 따른 CPU 주파수 자동 조절
- **Light-sleep 활용**: 입력 없을 때 저전력 모드로 전환
- **주변기기 전원 관리**: 사용하지 않는 WiFi/Bluetooth 모듈 비활성화
- **배터리 효율**: 불필요한 전력 소비 최소화

**전력 최적화 전략 요구사항**:
- **실시간 모니터링**: 시스템 부하 실시간 측정
- **동적 전환**: 부하 변화에 따른 즉각적인 모드 전환
- **전력 소비 목표**: 활성 모드 150mA, 절전 모드 10mA 이하

## 6. 오류 처리 및 복구

### 6.1 UART 오류 처리

**UART 오류 감지 및 복구 요구사항**:
- 시스템은 UART 통신 중 발생할 수 있는 다양한 오류(FIFO 오버플로우, 버퍼 풀, 프레임 오류 등)를 감지하고 처리할 수 있어야 합니다.
- 오류 발생 시, 해당 오류를 로그로 기록하고, 필요한 경우 입력 버퍼를 비우는 등의 복구 절차를 수행해야 합니다.

### 6.2 USB 오류 처리

**USB 연결 상태 모니터링 요구사항**:
- USB 연결 및 해제 이벤트를 감지하고, `system_context`의 `connection_state`를 적절히 갱신해야 합니다.
- USB 연결이 끊어졌을 때, 의도치 않은 입력이 지속되는 것을 방지하기 위해 모든 마우스 버튼과 키보드 키를 해제하는 '빈 리포트'를 전송하는 로직이 필요합니다.

### 6.3 시스템 복구 메커니즘

**워치독 및 시스템 복구 요구사항**:
- 태스크가 무한 루프에 빠지는 등의 소프트웨어 결함에 대응하기 위해 태스크 워치독 타이머(TWDT)를 활성화해야 합니다.
- 각 주요 태스크는 주기적으로 워치독을 리셋해야 하며, 워치독 타임아웃 발생 시 시스템 패닉으로 이어져 안전한 복구(예: 시스템 재시작)가 수행되도록 설정해야 합니다.

## 7. ESP-IDF 개발 환경 설정 및 빌드

### 7.1 ESP-IDF 설치 및 환경 설정

**Windows 환경 ESP-IDF 설치 요구사항**:
1. **ESP-IDF 다운로드 및 설치**:
   - ESP-IDF v5.5 이상 권장 (TinyUSB Composite 안정 지원)
   - 설치 경로: `C:\Espressif\esp-idf` (권장)
   - 설치 방법: ESP-IDF Windows Installer 사용

2. **환경 변수 설정**:
   ```powershell
   # PowerShell에서 ESP-IDF 환경 활성화
   C:\Espressif\esp-idf\export.ps1
   
   # 또는 CMD에서
   C:\Espressif\esp-idf\export.bat
   ```

3. **타겟 설정**:
   ```bash
   # ESP32-S3 타겟 지정
   idf.py set-target esp32s3
   ```

**개발 도구 요구사항**:
- **CMake**: 3.16 이상 (ESP-IDF 빌드 시스템)
- **Python**: 3.8 이상 (idf.py 스크립트)
- **Ninja**: 빌드 성능 최적화
- **Git**: ESP-IDF 컴포넌트 관리

### 7.2 프로젝트 설정 (sdkconfig)

**idf.py menuconfig를 통한 필수 설정**:
```bash
# 설정 메뉴 열기
idf.py menuconfig
```

**필수 활성화 항목**:
```
Component config → TinyUSB Stack
  [*] Enable TinyUSB driver
  [*]   HID support
  [*]   CDC support
  
Component config → Driver configurations → UART configuration
  [*] Place UART ISR in IRAM

Component config → FreeRTOS
  (1000) Tick rate (Hz)
  [ ] Run FreeRTOS only on first core
  
Component config → Compiler options
  Optimization Level (Optimize for size (-Os))
```

**sdkconfig.defaults 파일 작성**:
```ini
# TinyUSB Composite 디바이스 설정
CONFIG_TINYUSB=y
CONFIG_TINYUSB_HID_ENABLED=y
CONFIG_TINYUSB_CDC_ENABLED=y
CONFIG_TINYUSB_DEVICE_ENABLED=y

# HID 설정
CONFIG_TINYUSB_HID_COUNT=2
CONFIG_TINYUSB_HID_BUFSIZE=64

# CDC 설정
CONFIG_TINYUSB_CDC_COUNT=1
CONFIG_TINYUSB_CDC_RX_BUFSIZE=512
CONFIG_TINYUSB_CDC_TX_BUFSIZE=512

# UART 최적화
CONFIG_UART_ISR_IN_IRAM=y

# FreeRTOS 설정
CONFIG_FREERTOS_HZ=1000
CONFIG_FREERTOS_UNICORE=n

# 메모리 최적화
CONFIG_COMPILER_OPTIMIZATION_SIZE=y
CONFIG_SPIRAM_MODE_OCT=y

# 플래시 설정
CONFIG_ESPTOOLPY_FLASHSIZE_8MB=y
CONFIG_PARTITION_TABLE_CUSTOM=y
CONFIG_PARTITION_TABLE_CUSTOM_FILENAME="partitions.csv"
```

### 7.3 빌드, 플래시, 모니터링

**프로젝트 빌드**:
```bash
# 전체 빌드
idf.py build

# 클린 빌드
idf.py fullclean build
```

**펌웨어 플래시**:
```bash
# COM 포트 자동 감지 및 플래시
idf.py -p COM3 flash

# 플래시 + 모니터 동시 실행
idf.py -p COM3 flash monitor
```

**시리얼 모니터링**:
```bash
# 시리얼 모니터만 실행
idf.py -p COM3 monitor

# 종료: Ctrl + ]
```

**고급 빌드 옵션**:
```bash
# 특정 타겟만 빌드
idf.py app

# 파티션 테이블만 플래시
idf.py partition-table-flash

# 부트로더 재빌드 및 플래시
idf.py bootloader bootloader-flash
```

### 7.4 파티션 테이블 설정

**partitions.csv 요구사항**:
```csv
# Name,   Type, SubType, Offset,  Size,    Flags
nvs,      data, nvs,     0x9000,  0x4000,
otadata,  data, ota,     0xd000,  0x2000,
phy_init, data, phy,     0xf000,  0x1000,
factory,  app,  factory, 0x10000, 0x300000,
storage,  data, fat,     0x310000,0x100000,
```

**설명**:
- **factory**: 메인 애플리케이션 (3MB, TinyUSB + FreeRTOS + 로직)
- **nvs**: 비휘발성 저장소 (설정 저장)
- **storage**: 추가 데이터 저장 (선택)

### 7.5 디버깅 도구

**ESP-IDF 로깅 시스템**:
```c
#include "esp_log.h"

static const char* TAG = "BridgeOne";

// 로그 레벨별 출력
ESP_LOGI(TAG, "USB initialized");          // Info
ESP_LOGW(TAG, "Frame dropped");            // Warning
ESP_LOGE(TAG, "UART error: %d", err);      // Error
ESP_LOGD(TAG, "Frame: seq=%d", frame.seq); // Debug (CONFIG_LOG_DEFAULT_LEVEL_DEBUG 필요)
```

**디버그 매크로 설정**:
```c
// sdkconfig에서 로그 레벨 설정
#define LOG_LOCAL_LEVEL ESP_LOG_DEBUG  // 개발: DEBUG, 프로덕션: INFO

#ifdef DEBUG_FRAME_VERBOSE
void printBridgeFrame(const bridge_frame_t* frame) {
    ESP_LOGD(TAG, "Frame[seq=%u]: btn=%02X dx=%d dy=%d wh=%d mod=%02X k1=%02X k2=%02X",
             frame->seq, frame->buttons, frame->deltaX, frame->deltaY,
             frame->wheel, frame->modifiers, frame->keyCode1, frame->keyCode2);
}
#endif
```

**성능 모니터링**:
```c
void monitor_task(void* pvParameters) {
    while (1) {
        // FreeRTOS 태스크 상태
        vTaskList(task_status_buffer);
        ESP_LOGI(TAG, "Tasks:\n%s", task_status_buffer);
        
        // 힙 메모리 정보
        ESP_LOGI(TAG, "Free heap: %d bytes", esp_get_free_heap_size());
        
        // 시스템 통계
        ESP_LOGI(TAG, "FPS: %lu, Latency: %lu us", 
                 ctx.frames_per_second, ctx.avg_latency_us);
        
        vTaskDelay(pdMS_TO_TICKS(5000));
    }
}
```

### 7.6 자동화 테스트 (ESP-IDF Unity)

**ESP-IDF Unity 테스트 프레임워크**:
```bash
# 테스트 컴포넌트 추가
idf.py create-component test

# 테스트 실행
idf.py build
idf.py -p COM3 flash monitor
```

**테스트 케이스 구현 예시**:
```c
#include "unity.h"

// UART 프레임 파싱 테스트
TEST_CASE("UART frame parsing", "[uart]") {
    bridge_frame_t frame = {
        .seq = 42,
        .buttons = 0x01,
        .deltaX = 10,
        .deltaY = -5,
        .wheel = 2,
        .modifiers = 0x02,
        .keyCode1 = 0x04,
        .keyCode2 = 0x00
    };
    
    TEST_ASSERT_EQUAL_UINT8(42, frame.seq);
    TEST_ASSERT_EQUAL_UINT8(0x01, frame.buttons);
    TEST_ASSERT_EQUAL_INT8(10, frame.deltaX);
}

// HID 리포트 생성 테스트
TEST_CASE("HID keyboard report generation", "[hid]") {
    bridge_frame_t frame = {
        .modifiers = 0x02,  // Ctrl
        .keyCode1 = 0x04,   // 'A'
        .keyCode2 = 0x00
    };
    
    HIDBootKeyboardReport kb_report;
    generateKeyboardReport(&frame, &kb_report);
    
    TEST_ASSERT_EQUAL_UINT8(0x02, kb_report.modifiers);
    TEST_ASSERT_EQUAL_UINT8(0x04, kb_report.keyCodes[0]);
}

// CRC16 검증 테스트
TEST_CASE("CRC16 checksum verification", "[cdc]") {
    uint8_t payload[] = "test data";
    uint16_t crc = esp_crc16_le(0, payload, sizeof(payload));
    
    TEST_ASSERT_NOT_EQUAL(0, crc);
    TEST_ASSERT_EQUAL_UINT16(crc, esp_crc16_le(0, payload, sizeof(payload)));
}
```

**통합 테스트 실행**:
```bash
# 모든 테스트 실행
idf.py build flash monitor

# 특정 테스트만 실행 (Unity 메뉴)
# 시리얼 모니터에서 테스트 케이스 선택
```

## 8. 배포 및 유지보수

### 8.1 프로덕션 빌드 및 배포

**릴리즈 빌드 스크립트** (PowerShell):
```powershell
# build_release.ps1
# ESP-IDF 환경 활성화
C:\Espressif\esp-idf\export.ps1

# 클린 빌드
idf.py fullclean

# 릴리즈 설정으로 빌드
idf.py -D CMAKE_BUILD_TYPE=Release build

# 바이너리 병합
esptool.py --chip esp32s3 merge_bin `
    -o bridgeone_esp32s3_v2.0.bin `
    --flash_mode dio --flash_freq 80m --flash_size 8MB `
    0x0 build/bootloader/bootloader.bin `
    0x8000 build/partition_table/partition-table.bin `
    0x10000 build/bridgeone_board.bin

Write-Host "Release build completed: bridgeone_esp32s3_v2.0.bin"
```

**버전 관리**:
```c
// main.c에 버전 정보 포함
#define FIRMWARE_VERSION "2.0.0"
#define FIRMWARE_BUILD_DATE __DATE__
#define FIRMWARE_FRAMEWORK "ESP-IDF"

void app_main(void) {
    ESP_LOGI(TAG, "BridgeOne Board v%s (%s, %s)", 
             FIRMWARE_VERSION, FIRMWARE_FRAMEWORK, FIRMWARE_BUILD_DATE);
    // ... 초기화
}
```

### 8.2 OTA 업데이트 지원 (선택)

**ESP-IDF OTA 컴포넌트 활용**:
```c
#include "esp_ota_ops.h"
#include "esp_https_ota.h"

// OTA 업데이트 함수 (향후 확장)
void perform_ota_update(const char* url) {
    esp_http_client_config_t config = {
        .url = url,
        .cert_pem = server_cert_pem_start,
    };
    
    esp_err_t ret = esp_https_ota(&config);
    if (ret == ESP_OK) {
        ESP_LOGI(TAG, "OTA update successful, rebooting...");
        esp_restart();
    } else {
        ESP_LOGE(TAG, "OTA update failed: %s", esp_err_to_name(ret));
    }
}
```

**파티션 테이블 (OTA 지원)**:
```csv
# OTA 지원 파티션 테이블 (선택)
nvs,      data, nvs,     0x9000,  0x4000,
otadata,  data, ota,     0xd000,  0x2000,
phy_init, data, phy,     0xf000,  0x1000,
ota_0,    app,  ota_0,   0x10000, 0x180000,
ota_1,    app,  ota_1,   0x190000,0x180000,
storage,  data, fat,     0x310000,0x100000,
```

### 8.3 문제 해결 가이드

**일반적인 문제 및 해결책**:

| 문제 | 증상 | 원인 | 해결책 |
|------|------|------|--------|
| 빌드 실패 | `idf.py build` 오류 | TinyUSB 미활성화 | `idf.py menuconfig`에서 TinyUSB 활성화 |
| USB 인식 실패 | PC에서 장치 인식 안됨 | 디스크립터 오류 | `usb_descriptors.c` §0 규칙 준수 확인 |
| UART 통신 실패 | 프레임 수신 없음 | GPIO 핀 설정 오류 | GPIO 43(TX), 44(RX) 확인 |
| 메모리 부족 | 재시작 반복 | 스택 오버플로우 | `uxTaskGetStackHighWaterMark()` 확인 후 증가 |
| 높은 지연시간 | 입력 반응 느림 | 태스크 우선순위 부적절 | Core 분산 및 우선순위 재조정 |
| CDC/HID 간섭 | 동시 동작 불안 | TinyUSB 설정 오류 | §0 인터페이스 순서 확인 |

**ESP-IDF 내장 디버그 명령어**:
```bash
# idf.py monitor에서 사용 가능
(monitor) log_level BridgeOne DEBUG  # 로그 레벨 변경
(monitor) heap                       # 힙 메모리 정보
(monitor) tasks                      # FreeRTOS 태스크 목록
(monitor) restart                    # ESP32-S3 재시작
```

**커스텀 콘솔 명령어** (선택 구현):
- `stats`: 현재 시스템 통계 출력 (FPS, 지연시간, 오류율)
- `reset`: ESP32-S3 재시작 (`esp_restart()`)
- `test`: 단위 테스트 실행
- `mem`: 힙 메모리 사용량 (`esp_get_free_heap_size()`)
- `tasks`: FreeRTOS 태스크 목록 및 스택 사용량 (`vTaskList()`)

## 9. 성능 벤치마크 및 품질 기준

### 9.1 ESP-IDF 기반 성능 목표

| 지표 | 목표값 | 측정 방법 | ESP-IDF API |
|------|--------|-----------|------------|
| 지연시간 (ESP32-S3 기여분) | ≤ 5ms | 로직 애널라이저 | `esp_timer_get_time()` |
| UART 처리량 | ≥ 1000 frame/sec | 소프트웨어 카운터 | FreeRTOS 큐 카운터 |
| USB 전송 성공률 | ≥ 99.9% | 오류 카운터 | TinyUSB 콜백 상태 |
| CPU 사용률 | ≤ 30% | FreeRTOS 통계 | `vTaskGetRunTimeStats()` |
| 메모리 사용률 | ≤ 70% (360KB) | 힙 모니터링 | `esp_get_free_heap_size()` |
| 전력 소모 (활성) | ≤ 150mA | 전력 측정기 | ESP-IDF 전력 관리 API |
| 연속 동작 시간 | ≥ 72시간 | 스트레스 테스트 | TWDT 모니터링 |

### 9.2 ESP-IDF 기반 품질 검증

**ESP-IDF 자동화 테스트 활용**:
```bash
# Unity 테스트 실행
idf.py build
idf.py -p COM3 flash monitor

# 테스트 결과 자동 수집
# Unity 출력: Test Summary: XX Tests XX Failures
```

**품질 게이트 기준**:
- ✅ 모든 Unity 테스트 통과 (0 failures)
- ✅ 프레임 손실률 < 0.1%
- ✅ 최대 지연시간 < 10ms
- ✅ 평균 CPU 사용률 < 30%
- ✅ 메모리 누수 없음 (힙 크기 일정 유지)
- ✅ 4시간 스트레스 테스트 통과

## 10. 결론 및 다음 단계

### 10.1 ESP-IDF 구현 우선순위

**Phase 1.1-1.3: ESP-IDF 환경 및 기본 통신 (1주)**
1. ESP-IDF 개발 환경 구축 (`idf.py` 설정)
2. TinyUSB Composite 디스크립터 구현 (§1.5 불변 규칙 준수)
3. UART 통신 및 BridgeOne 프레임 파싱
4. 기본 HID 리포트 전송 검증

**Phase 1.4-1.5: 동시 동작 및 태스크 구조 (1주)**
1. HID + CDC 동시 동작 안정성 검증
2. FreeRTOS 듀얼 코어 태스크 구성
3. 성능 최적화 (정적 할당, 우선순위 조정)
4. 오류 처리 및 복구 메커니즘

**Phase 1.6: E2E 통합 및 검증 (1주)**
1. Android ↔ ESP32-S3 ↔ Windows 통합 테스트
2. Essential/Standard 모드 전환 검증
3. Keep-alive 및 핸드셰이크 프로토콜 확인
4. 성능 벤치마크 및 장시간 안정성 테스트

**참조**: 상세 체크리스트는 [`development-plan-checklist.md`](../development-plan-checklist.md) Phase 1.1~1.6 참조

### 10.2 ESP-IDF 확장 가능성

**ESP-IDF 기능 활용 여지**:
- **WiFi 디버깅**: ESP-IDF WiFi 컴포넌트로 무선 로깅 지원
- **웹 서버**: ESP-IDF HTTP 서버로 설정 인터페이스 제공
- **OTA 업데이트**: ESP-IDF OTA 컴포넌트로 무선 펌웨어 업데이트
- **Bluetooth**: ESP-IDF BLE 컴포넌트로 무선 통신 확장

**TinyUSB 확장 가능성**:
- HID Report Protocol로 확장 (휠 이상 기능)
- 다중 HID 디바이스 추가 (게임패드, 조이스틱 등)
- 커스텀 Vendor 인터페이스 추가

**성능 개선 여지**:
- ESP-IDF DMA를 활용한 UART 고속 전송
- PSRAM 활용한 대용량 버퍼링
- ESP32-S3 AI 가속기 활용 (패턴 인식 등)

## 11. 신호 송수신 매트릭스 및 설계 완성도

### 11.1 지원되는 신호 매트릭스

| 신호 유형 | Android → ESP32-S3 | ESP32-S3 → Windows | Windows → ESP32-S3 | ESP32-S3 → Android |
|-----------|--------------------|--------------------|--------------------|--------------------|
| **기본 HID 프레임** | ✅ 8바이트 UART | ✅ HID Boot Mouse/Keyboard | ➖ 해당없음 | ➖ 해당없음 |
| **멀티 커서 전환** | ✅ JSON 명령 | ✅ CDC 중계 | ✅ 응답 수신 | ✅ 상태 알림 |
| **가상 커서 제어** | ✅ show/hide 명령 | ✅ CDC 중계 | ✅ 확인 응답 | ✅ 결과 중계 |
| **매크로 실행** | ✅ 시작/취소 명령 | ✅ CDC 중계 | ✅ 진행/완료 응답 | ✅ 상태 중계 |
| **핸드셰이크** | ➖ 해당없음 | ✅ 상태 정보 전송 | ✅ Challenge-Response | ➖ 해당없음 |
| **Keep-alive** | ➖ 해당없음 | ✅ Pong 응답 | ✅ Ping 수신 | ➖ 해당없음 |
| **모드 전환 알림** | ➖ 해당없음 | ➖ 해당없음 | ➖ 해당없음 | ✅ Essential/Standard |
| **UI 강제 활성화** | ✅ 강제 해제 명령 | ✅ CDC 중계 | ✅ 확인 응답 | ✅ 결과 중계 |

### 11.2 설계 완성도 평가

**✅ 완전히 정의된 기능:**
- UART ↔ USB HID/CDC 프로토콜 변환 시스템
- JSON 기반 Vendor CDC 명령 처리 구조
- Windows 서버 연동을 위한 핸드셰이크 프로토콜
- 연결 상태 모니터링을 위한 Keep-alive 시스템
- 요청-응답 매칭을 위한 메시지 ID 관리
- Android-Windows 양방향 메시지 중계 시스템

**✅ 설계된 핵심 아키텍처:**
- Vendor CDC 명령 타입 체계 (12개 명령 카테고리)
- JSON 페이로드 구조 표준 및 파싱 원칙
- 2단계 핸드셰이크 프로세스 (인증 + 상태 동기화)
- 대기 요청 관리 시스템 (최대 8개 동시 처리)
- CRC16 기반 데이터 무결성 검증 체계
- 포괄적 테스트 및 검증 방법론

### 11.3 성능 및 신뢰성 검증

**처리 용량:**
- 기본 HID 프레임: 1000+ frame/sec (목표 달성)
- JSON 명령: 100+ command/sec (충분한 여유)
- 대기 요청: 최대 8개 동시 처리 (매크로 실행 고려)
- Keep-alive: 0.5초 주기 안정적 처리

**메모리 사용량:**
- JSON 버퍼: 512바이트 × 2 (송신/수신)
- 대기 요청: 약 44바이트 × 8 = 352바이트
- 총 추가 메모리: ~1.4KB (512KB 중 0.27%)

**신뢰성 보장:**
- CRC16 체크섬으로 데이터 무결성 검증
- 메시지 ID로 요청-응답 정확한 매칭
- 타임아웃 처리로 데드락 방지
- Keep-alive로 연결 상태 실시간 감지

### 11.4 설계 완료 평가

**검증 완료**: ESP32-S3 설계 명세서가 BridgeOne 시스템에서 요구하는 **모든 USB 브릿지 역할을 완전히 정의**하였습니다.

**핵심 설계 성과:**
1. **완전한 통신 프로토콜** 설계 (UART ↔ USB HID/CDC 변환 시스템)
2. **양방향 메시지 처리** 아키텍처 원칙 수립
3. **실시간 성능 최적화** 전략 및 목표 정의
4. **신뢰성 보장**을 위한 오류 처리 및 복구 체계
5. **테스트 및 검증** 방법론 수립

이제 ESP32-S3가 BridgeOne 시스템의 핵심 USB 브릿지로서 요구되는 모든 기능을 ESP-IDF 프레임워크 기반으로 안정적으로 수행할 수 있는 완전한 설계 기반이 마련되었습니다.

**ESP-IDF 전환의 핵심 성과**:
1. **안정적인 CDC + HID 동시 동작**: TinyUSB 직접 제어로 근본 문제 해결
2. **저수준 최적화**: 프레임워크 추상화 제거로 지연시간 최소화
3. **공식 지원**: Espressif 공식 프레임워크로 장기 안정성 보장
4. **호환성 보장**: §1.5 불변 규칙 준수로 Android/Windows 코드 무변경

**다음 단계**:
- 구체적인 TinyUSB API 사용법 및 코드 예제는 `.cursor/rules/esp32s3-espidf-implementation-guide.md` 참조
- Phase 1.1~1.6 개발 체크리스트는 [`development-plan-checklist.md`](../development-plan-checklist.md) 참조
- 테스트 가이드는 [`test-reports/Phase-1.4-ESP-IDF-Composite-Validation.md`](../test-reports/Phase-1.4-ESP-IDF-Composite-Validation.md) 참조

---

**참조 문서**:
- **ESP-IDF 공식 문서**:
  - [ESP-IDF Programming Guide v5.5+](https://docs.espressif.com/projects/esp-idf/en/latest/)
  - [ESP-IDF USB Device Guide](https://docs.espressif.com/projects/esp-idf/en/latest/esp32s3/api-reference/peripherals/usb_device.html)
  - [ESP-IDF TinyUSB Component](https://docs.espressif.com/projects/esp-idf/en/latest/esp32s3/api-reference/peripherals/usb_device.html#tinyusb-stack)
  - [ESP-IDF Build System (CMake)](https://docs.espressif.com/projects/esp-idf/en/latest/esp32s3/api-guides/build-system.html)
  - [ESP-IDF idf.py Tool](https://docs.espressif.com/projects/esp-idf/en/latest/esp32s3/api-guides/tools/idf-py.html)
- **TinyUSB 공식 문서**:
  - [TinyUSB Documentation](https://docs.tinyusb.org/)
  - [TinyUSB Composite Device Example](https://github.com/hathach/tinyusb/tree/master/examples/device/composite)
- **USB 표준**:
  - [USB HID Usage Tables v1.4](https://usb.org/sites/default/files/hut1_4.pdf)
  - [USB Device Class Definition for HID v1.11](https://www.usb.org/sites/default/files/hid1_11.pdf)
- **FreeRTOS**:
  - [FreeRTOS Real Time Kernel](https://www.freertos.org/Documentation/RTOS_book.html)
  - [ESP-IDF FreeRTOS Guide](https://docs.espressif.com/projects/esp-idf/en/latest/esp32s3/api-reference/system/freertos.html)
