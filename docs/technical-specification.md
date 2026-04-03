---
title: "BridgeOne 기술 명세서"
description: "프로토콜·상태모델·알고리즘·성능·오류와 플랫폼 간 계약을 단일 출처로 규정하는 SSOT(Single Source of Truth) 문서"
tags: ["spec", "contract", "protocol", "state-machine", "algorithms"]
version: "v0.1"
owner: "Chatterbones"
updated: "2025-10-20"
note: "본 문서에 존재하는 모든 상수값 및 설정값은 초기 값으로, 확정된 고정값이 아님"
---

# BridgeOne 기술 명세서

## 용어집/정의

- Selected/Unselected: 선택 상태. 시각 강조/선택 표시. 입력 가능과 혼동 금지.
- Enabled/Disabled: 입력 가능 상태. 포인터/키 입력 허용 여부.
- Essential/Standard: 운용 상태. Windows 서버와 연결되지 않은 상태는 Essential(필수 기능), 연결된 상태는 Standard(모든 기능)입니다.
- TransportState: NoTransport | UsbOpening | UsbReady.
- RFC2119: MUST/SHOULD/MAY 규범 용어.
- 상태 용어 사용 원칙(금칙어 포함): "활성/비활성" 금지. Selected/Unselected, Enabled/Disabled로 표기[[memory:5809234]].

## 1. 목적/범위/용어

### 1.1 문서 역할 정의

본 문서는 **BridgeOne 프로젝트의 SSOT(Single Source of Truth) 규범 문서**입니다:
- **목적**: 프로토콜·상태·알고리즘·성능·오류 정책을 중앙에서 규정해 문서 간 드리프트를 방지
- **성격**: 기술 명세 및 플랫폼 간 계약서 ("**무엇을**" 해야 하는가)

### 1.2 범위 및 용어

- **범위**: 앱↔동글↔PC 입력 경로 전반. UI 시각 규칙은 `docs/design-guide-app.md`를 참조하되, 상호작용 알고리즘은 본 문서가 우선
- **용어**: `Selected/Unselected`(선택), `Enabled/Disabled`(입력 가능), `Essential/Standard`(운용 상태), `TransportState` 등

## 2. 시스템 아키텍처

### 2.1 하드웨어 연결 구조

```mermaid
graph TD
    subgraph "물리적 연결 구조"
        Android_App["Android 앱<br/>(Samsung Galaxy s10e)"] -- "USB-C" --> ESP32_S3_UART["ESP32-S3<br/>USB-to-UART 포트<br/>(내장)"]
        ESP32_S3_UART -- "내부 연결<br/>(1Mbps, 8N1)" --> ESP32_S3["ESP32-S3<br/>(YD-ESP32-S3 N16R8)"]
        ESP32_S3 -- "USB-OTG 포트<br/>(복합 장치)" --> PC["PC<br/>(Windows 10/11)"]
        Charger["충전기<br/>(5V 2A+)"] -- "USB-C" --> Android_App
    end
    
    subgraph "전력 공급"
        Charger -- "5V 2A+" --> Android_App
    end
```

**하드웨어 구성 요소:**
- **Android 앱**: Samsung Galaxy s10e (2280×1080, 5.8인치, Android 12)
- **ESP32-S3**: ESP32-S3 N16R8 보드 (16MB Flash, 8MB Octal SPI PSRAM)
  - 지원 보드: YD-ESP32-S3 N16R8
  - UART 포트 (Android 연결, GPIO17/18)
  - USB-OTG 포트 (PC 연결, 복합 장치 구성)
- **PC**: Windows 11, USB Host 포트
- **충전기**: 5V 2A 이상 (Android 충전용)

### 2.2 데이터 흐름 아키텍처

```mermaid
graph TD
    subgraph "Essential 모드 데이터 흐름"
        Essential_Page["Essential 전용<br/>페이지"] -- "제한된 입력<br/>(마우스+키보드)" --> Android_App
        Android_App -- "8B 델타 프레임<br/>(HID Boot Protocol)" --> ESP32_S3
        ESP32_S3 -- "HID Boot Mouse<br/>Interface" --> HID_Mouse_Driver["HID Boot Mouse Driver"]
        ESP32_S3 -- "HID Boot Keyboard<br/>Interface" --> HID_Keyboard_Driver["HID Boot Keyboard Driver"]
        ESP32_S3 -. "Vendor CDC Interface<br/>(Disabled in Essential)" .-> Windows_Server["Windows Server<br/>(미연결)"]
        HID_Mouse_Driver --> PC_BIOS["PC (BIOS/로그인)"]
        HID_Keyboard_Driver --> PC_BIOS
    end

    subgraph "Standard 모드 데이터 흐름"
        Standard_Page["Standard 페이지<br/>(모든 기능)"] -- "전체 입력<br/>(마우스+키보드+고급)" --> Android_App
        Android_App -- "8B 델타 프레임<br/>(HID Report Protocol)" --> ESP32_S3
        Android_App -- "커스텀 명령<br/>(0xFF 헤더 + JSON)" --> ESP32_S3
        ESP32_S3 -- "HID Report Mouse<br/>Interface" --> HID_Mouse_Driver
        ESP32_S3 -- "HID Report Keyboard<br/>Interface" --> HID_Keyboard_Driver
        ESP32_S3 -- "Vendor CDC<br/>(멀티 커서, 매크로)" --> Windows_Service["Windows 서비스<br/>(고급 기능)"]
        Windows_Service -- "응답 데이터" --> ESP32_S3
        ESP32_S3 -- "응답 중계" --> Android_App
        HID_Mouse_Driver --> PC_OS["PC (Windows OS)"]
        HID_Keyboard_Driver --> PC_OS
    end
```

**데이터 흐름 특징:**
- **Essential 모드**: Windows 서버 미연결 상태로 HID Boot Protocol만 사용하여 BIOS/로그인 단계에서 동작
- **Standard 모드**: Windows 서버 연결 상태로 HID Report Protocol과 Vendor CDC를 통한 고급 기능 제공
- **기본 경로**: 8바이트 프레임을 통한 마우스/키보드 입력 (모든 모드에서 동작)
- **확장 경로**: 커스텀 명령을 통한 멀티 커서, 매크로 등 고급 기능 (Standard 모드에서만)

**프로토콜 구분:**
- **HID Boot Protocol**: Essential 모드에서 사용, BIOS/UEFI 호환성 보장
- **HID Report Protocol**: Standard 모드에서 사용, 확장 기능 지원
- **Vendor CDC**: Windows 서비스와의 양방향 통신, 고급 기능 처리

### 2.3 하드웨어 구성 요소 분석

#### 2.3.1 ESP32-S3 N16R8 보드

**개요:**
- **역할**: USB 복합 장치로 HID 프로토콜 처리 및 Vendor CDC 통신 담당
- **펌웨어 프레임워크**: ESP-IDF v5.5+
- **지원 보드**:
  - ESP32-S3-DevkitC-1-N16R8 (Espressif 공식 개발 보드)
  - YD-ESP32-S3 N16R8 / YD-ESP32-23 (VCC-GND Studio 호환 보드)
- **공통 특징**:
  - 듀얼 코어 32비트 Xtensa LX7 마이크로컨트롤러 (최대 240MHz)
  - 16MB Flash (N16), 8MB Octal SPI PSRAM (R8)
  - Native USB OTG 포트 (GPIO19/20)
  - WiFi (2.4GHz) 및 Bluetooth 5.0 LE 내장
  - 45개의 프로그래머블 GPIO

**보드별 차이점**:

| 항목 | ESP32-S3-DevkitC-1 | YD-ESP32-S3 N16R8 |
|------|-------------------|------------------|
| USB-UART 칩 | CP2102N | CH343P |
| Android 통신 UART | UART0 (GPIO43/44) | UART1 (GPIO17/18) |
| 드라이버 | CP210x | CH343 |
| 비고 | 공식 보드 | 저가 클론, 전압 확인 필요 |

**성능 최적화 포인트:**
- **USB 스택**: ESP-IDF TinyUSB 컴포넌트로 저수준 직접 제어
- **실시간 처리**: FreeRTOS 듀얼 코어 활용으로 HID/CDC 분산 처리
- **메모리 관리**: 512KB RAM으로 효율적인 정적 버퍼 할당
- **전력 최적화**: ESP-IDF 전력 관리 API로 동적 주파수 조절

**USB Composite 디바이스 구성** (불변):
```
Interface 0: HID Boot Keyboard (0x03/0x01/0x01) - BIOS/UEFI 호환
Interface 1: HID Boot Mouse    (0x03/0x01/0x02) - BIOS/UEFI 호환
Interface 2: CDC-ACM Comm      (0x02/0x02/0x00) - Windows 서버 통신
Interface 3: CDC-ACM Data      (0x0A/0x00/0x00) - Windows 서버 통신
```

**호환성 보장:**
- Android 앱 및 Windows 서버 코드 변경 없음
- 동일 USB 디스크립터 및 리포트 구조 유지
- 인터페이스 순서 고정으로 열거 일관성 보장

#### 2.3.2 하드웨어 간 통신 최적화 전략

**UART 통신 (Android ↔ ESP32-S3):**
- **속도**: 1Mbps, 8N1 (8비트 데이터, 패리티 없음, 1비트 정지)
- **프레임 구조**: 8바이트 델타 프레임으로 효율적인 데이터 전송
- **플로우 제어**: 소프트웨어 플로우 제어로 안정적 통신 보장
- **보드별 구성**:
  - ESP32-S3-DevkitC-1: UART0 (GPIO43/44, CP2102N 연결)
  - YD-ESP32-S3 N16R8: UART1 (GPIO17/18, Android 통신 전용)
- **장점**: 신호 무결성 향상, 안정적 통신

**USB 통신 (ESP32-S3 ↔ PC):**
- **인터페이스**: 복합 USB 장치 (HID + Vendor CDC)
- **HID 부트 프로토콜**: BIOS/UEFI 호환성 보장
- **HID 리포트 프로토콜**: 확장 기능 지원
- **Vendor CDC 인터페이스**: 고급 기능 및 양방향 통신 처리

**전력 관리:**
- **ESP32-S3**: Light-sleep 모드 지원으로 배터리 효율 극대화
- **동적 전력 조절**: 사용량에 따른 코어 주파수 조절
- **내장 USB 브릿지**: 외부 어댑터 대비 전력 소비 감소

### 2.4 통신 프로토콜 최적화 전략

#### 2.4.1 프로토콜 최적화

**Essential 모드 최적화:**
- **HID Boot Protocol 전용**: 불필요한 오버헤드 제거로 최소 지연 시간 달성
- **8바이트 델타 프레임**: 마우스/키보드 입력만 처리하여 대역폭 효율 극대화
- **폴링 레이트 조절**: 125Hz 폴링으로 전력 소비 최소화

**Standard 모드 최적화:**
- **HID Report Protocol 활용**: 확장 기능 지원과 함께 효율적인 데이터 전송
- **Vendor CDC 인터페이스**: JSON 기반 커맨드로 구조화된 고급 기능 처리
- **양방향 통신**: 비동기 응답 처리로 실시간 상호작용 보장

#### 2.4.2 데이터 전송 효율화

**프레임 구조 최적화:**
```typescript
// 8바이트 델타 프레임 구조
interface HIDFrame {
  buttons: uint8;    // 1바이트: 마우스 버튼 상태
  deltaX: int8;      // 1바이트: X축 이동 델타
  deltaY: int8;      // 1바이트: Y축 이동 델타
  wheel: int8;       // 1바이트: 휠 이동 델타
  modifiers: uint8;  // 1바이트: 키보드 모디파이어
  reserved: uint8;   // 1바이트: 예약
  key1: uint8;       // 1바이트: 키 코드 1
  key2: uint8;       // 1바이트: 키 코드 2
}
```

**압축 및 인코딩 전략:**
- **델타 압축**: 이전 프레임 대비 변화량만 전송하여 대역폭 절약
- **RLE (Run-Length Encoding)**: 연속된 동일 입력에 대한 효율적 처리
- **비트 패킹**: 플래그와 데이터를 효율적으로 패킹하여 공간 최적화

#### 2.4.3 실시간 처리 최적화

**FreeRTOS 태스크 설계:**
- **고우선도 HID 태스크**: 마우스/키보드 입력을 실시간으로 처리
- **중우선도 Vendor 태스크**: 고급 기능 명령을 비동기로 처리
- **저우선도 관리 태스크**: 연결 상태 모니터링 및 전력 관리

**버퍼 관리 전략:**
- **링 버퍼**: UART 수신 데이터를 효율적으로 저장
- **더블 버퍼링**: HID 프레임 처리 중 데이터 손실 방지
- **동적 할당**: 사용량에 따른 버퍼 크기 조절

#### 2.4.4 전력 및 성능 최적화

**동적 전력 관리:**
- **활성 모드**: 최대 성능 모드 (Standard 모드)
- **절전 모드**: 입력 없을 때 저전력 모드 전환
- **심플 모드**: Essential 모드에서 불필요한 기능 비활성화

**성능 모니터링:**
- **지연 시간 측정**: 각 통신 단계별 처리 시간 모니터링
- **처리량 측정**: 초당 처리 가능한 프레임 수 추적
- **에러율 모니터링**: 통신 오류 발생률 및 복구 시간 측정

#### 2.4.5 오류 처리 및 복구 전략

**연결 상태 모니터링:**
- **하트비트 패킷**: 5초 간격으로 연결 상태 확인
- **재연결 로직**: 연결 끊어짐 시 자동 복구 메커니즘
- **폴백 모드**: 오류 발생 시 Essential 모드로 자동 전환

**데이터 무결성 보장:**
- **체크섬**: 각 Vendor CDC 프레임에 CRC16 체크섬 추가
- **오류 복구**: CRC 오류 시 프레임 폐기, 타임아웃에 의해 해당 단계 자동 재시도
- **순서 보장**: 시퀀스 번호로 패킷 순서 관리

#### 2.4.6 인터페이스별 통신 최적화 전략

**개요:**
각 USB 인터페이스(HID Boot Mouse, HID Boot Keyboard, Vendor CDC)는 서로 다른 목적과 특성을 가지므로, 개별적인 최적화 전략이 필요합니다. 이 절에서는 각 인터페이스의 프로토콜 스펙, 데이터 구조, 성능 최적화, 오류 처리 전략을 상세히 다룹니다.

**인터페이스별 역할 구분:**
- **HID Boot Mouse**: Essential 모드에서 BIOS/UEFI 호환 마우스 입력 처리
- **HID Boot Keyboard**: Essential 모드에서 BIOS/UEFI 호환 키보드 입력 처리
- **Vendor CDC**: Standard 모드에서 고급 기능 및 양방향 통신 처리

**통합 최적화 원칙:**
1. **프로토콜 일관성**: 모든 인터페이스가 동일한 8바이트 프레임 구조 기반
2. **실시간 우선순위**: HID 인터페이스를 Vendor CDC보다 높은 우선순위로 처리
3. **상태 기반 전환**: Essential/Standard 모드에 따른 인터페이스 활성화/비활성화
4. **자원 공유**: 공통 버퍼와 메모리 풀을 효율적으로 활용

#### 2.4.6.1 HID Boot Mouse Interface 최적화

**프로토콜 스펙:**
```typescript
// HID Boot Mouse 프레임 구조 (8바이트)
interface HidBootMouseFrame {
  buttons: uint8;    // 버튼 상태 (좌/우/중앙 클릭)
  deltaX: int8;      // X축 상대 이동 (-127 ~ +127)
  deltaY: int8;      // Y축 상대 이동 (-127 ~ +127)
  wheel: int8;       // 휠 이동 (스크롤)
  reserved: uint8[4]; // 예약 영역 (0으로 채움)
}
```

**ESP-IDF TinyUSB 기반 구현 방식:**
```c
// ESP32-S3 ESP-IDF 프레임워크 기반 HID Mouse 구현
#include "tusb.h"

// BridgeOne 8바이트 프레임을 HID Mouse 리포트로 변환
void processMouseFrame(const bridge_frame_t* frame) {
    // HID Boot Mouse 리포트 구조 (4바이트, 불변)
    struct {
        uint8_t buttons;
        int8_t deltaX;
        int8_t deltaY;
        int8_t wheel;
    } __attribute__((packed)) mouse_report;
    
    // 리포트 생성
    mouse_report.buttons = frame->buttons;
    mouse_report.deltaX = frame->deltaX;
    mouse_report.deltaY = frame->deltaY;
    mouse_report.wheel = frame->wheel;
    
    // TinyUSB API로 전송 (Interface 1: HID Mouse)
    tud_hid_n_report(1, 0, &mouse_report, sizeof(mouse_report));
}
```

**HID Mouse 최적화 타이밍 상수:**
```c
#define MOUSE_MOVE_STEP           8    // 마우스 이동 단위
#define MOUSE_SCROLL_STEP         1    // 스크롤 단위
#define MOUSE_DEBOUNCE_MS        40    // 마우스 버튼 디바운싱
#define MOUSE_REPEAT_INITIAL_MS 300    // 초기 반복 지연
#define MOUSE_REPEAT_INTERVAL_MS 30    // 반복 간격
```

**최적화 전략:**

**성능 최적화:**
- **폴링 레이트**: 125Hz로 설정하여 전력 소모 최소화
- **델타 압축**: 이전 프레임 대비 변화량만 전송
- **지연 보상**: 프레임 처리 시 시스템 지연 시간 예측 및 보정
- **버퍼 관리**: 16프레임의 링 버퍼로 입력 버스트 처리

**정확성 최적화:**
- **보정 알고리즘**: DPI 설정에 따른 마우스 감도 자동 조정
- **안정화 필터**: 미세 떨림을 제거하는 노이즈 필터 적용
- **가속 곡선**: 사용자의 이동 패턴에 따른 속도 가속 처리
- **정지 감지**: 2프레임 연속 무이동 시 전송 중단

**오류 처리:**
- **프레임 검증**: 각 프레임의 유효성 검사 (체크섬 + 범위 검사)
- **오류 프레임 폐기**: CRC 불일치 시 해당 프레임 폐기, 타임아웃에 의해 해당 단계 자동 재시도
- **폴백 모드**: 오류 지속 시 절대 좌표 모드로 전환

#### 2.4.6.1.1 HID Absolute Mouse Interface (절대좌표 모드)

**개요:**
절대좌표 모드는 터치패드 영역의 터치 위치를 PC 화면 좌표에 1:1로 매핑하는 기능입니다. 기존 상대좌표(Boot Mouse) 인터페이스와 별도의 HID Report ID를 사용하여, 하나의 HID Mouse 인터페이스 내에서 상대/절대 리포트를 구분합니다.

**프로토콜 스펙:**
```typescript
// HID Absolute Mouse 프레임 구조 (7바이트)
interface HidAbsoluteMouseFrame {
  reportId: uint8;    // 리포트 ID (0x02: 절대좌표)
  buttons: uint8;     // 버튼 상태 (좌/우/중앙 클릭)
  absoluteX: uint16;  // X축 절대 좌표 (0~32767, Little-Endian)
  absoluteY: uint16;  // Y축 절대 좌표 (0~32767, Little-Endian)
  wheel: int8;        // 휠 이동 (스크롤)
}
```

**좌표 범위 및 매핑:**
- X축: 0 (화면 왼쪽 끝) ~ 32767 (화면 오른쪽 끝)
- Y축: 0 (화면 위쪽 끝) ~ 32767 (화면 아래쪽 끝)
- Android 터치 좌표 → 비율(0.0~1.0) → HID 절대좌표(0~32767) 변환
- 변환 공식: `absoluteCoord = (touchPos / touchpadSize) * 32767`

**Report ID 기반 리포트 구분:**
- Report ID 0x01: 상대좌표 리포트 (기존 Boot Mouse와 동일한 데이터, Report ID 추가)
- Report ID 0x02: 절대좌표 리포트

**ESP-IDF TinyUSB 기반 구현 방식:**
```c
// HID Absolute Mouse 리포트 전송
void processAbsoluteMouseFrame(uint8_t buttons, uint16_t absX, uint16_t absY, int8_t wheel) {
    struct {
        uint8_t buttons;
        uint16_t x;
        uint16_t y;
        int8_t wheel;
    } __attribute__((packed)) abs_report;

    abs_report.buttons = buttons;
    abs_report.x = absX;    // 0~32767
    abs_report.y = absY;    // 0~32767
    abs_report.wheel = wheel;

    // Report ID 0x02로 절대좌표 리포트 전송
    tud_hid_n_report(ITF_NUM_HID_MOUSE, REPORT_ID_ABS_MOUSE, &abs_report, sizeof(abs_report));
}
```

**BridgeOne UART 프레임 확장:**
절대좌표 전송 시 기존 8바이트 프레임 대신 확장 프레임을 사용합니다:
```typescript
// 절대좌표 UART 프레임 (8바이트)
interface AbsoluteCoordinateFrame {
  seq: uint8;          // [0] 시퀀스 번호 (0~255)
  frameType: uint8;    // [1] 0x80 (절대좌표 프레임 식별자)
  buttons: uint8;      // [2] 버튼 상태 (bit0=L, bit1=R, bit2=M)
  absoluteX_H: uint8;  // [3] X좌표 상위 바이트
  absoluteX_L: uint8;  // [4] X좌표 하위 바이트
  absoluteY_H: uint8;  // [5] Y좌표 상위 바이트
  absoluteY_L: uint8;  // [6] Y좌표 하위 바이트
  wheel: int8;         // [7] 휠 이동 (스크롤)
}
```
- `frameType == 0x80`: ESP32-S3가 절대좌표 프레임임을 식별
- 기존 프레임(`frameType != 0x80`): 상대좌표로 처리 (하위 호환성 유지)

**HID Report Descriptor (절대좌표 포함):**
```c
// 기존 상대좌표 + 절대좌표를 Report ID로 구분하는 통합 디스크립터
uint8_t const desc_hid_mouse_report[] = {
    HID_USAGE_PAGE(HID_USAGE_PAGE_DESKTOP),
    HID_USAGE(HID_USAGE_DESKTOP_MOUSE),
    HID_COLLECTION(HID_COLLECTION_APPLICATION),

        // === Report ID 0x01: 상대좌표 (기존 Boot Mouse 호환) ===
        HID_REPORT_ID(0x01),
        HID_USAGE(HID_USAGE_DESKTOP_POINTER),
        HID_COLLECTION(HID_COLLECTION_PHYSICAL),
            // Buttons (3 bits)
            HID_USAGE_PAGE(HID_USAGE_PAGE_BUTTON),
            HID_USAGE_MIN(1), HID_USAGE_MAX(3),
            HID_LOGICAL_MIN(0), HID_LOGICAL_MAX(1),
            HID_REPORT_COUNT(3), HID_REPORT_SIZE(1),
            HID_INPUT(HID_DATA | HID_VARIABLE | HID_ABSOLUTE),
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

        // === Report ID 0x02: 절대좌표 (Absolute Digitizer) ===
        HID_REPORT_ID(0x02),
        HID_USAGE(HID_USAGE_DESKTOP_POINTER),
        HID_COLLECTION(HID_COLLECTION_PHYSICAL),
            // Buttons (3 bits)
            HID_USAGE_PAGE(HID_USAGE_PAGE_BUTTON),
            HID_USAGE_MIN(1), HID_USAGE_MAX(3),
            HID_LOGICAL_MIN(0), HID_LOGICAL_MAX(1),
            HID_REPORT_COUNT(3), HID_REPORT_SIZE(1),
            HID_INPUT(HID_DATA | HID_VARIABLE | HID_ABSOLUTE),
            HID_REPORT_COUNT(1), HID_REPORT_SIZE(5),
            HID_INPUT(HID_CONSTANT),
            // X, Y (absolute, 0~32767)
            HID_USAGE_PAGE(HID_USAGE_PAGE_DESKTOP),
            HID_USAGE(HID_USAGE_DESKTOP_X), HID_USAGE(HID_USAGE_DESKTOP_Y),
            HID_LOGICAL_MIN(0), HID_LOGICAL_MAX_N(32767, 2),
            HID_REPORT_COUNT(2), HID_REPORT_SIZE(16),
            HID_INPUT(HID_DATA | HID_VARIABLE | HID_ABSOLUTE),
            // Wheel (relative)
            HID_USAGE(HID_USAGE_DESKTOP_WHEEL),
            HID_LOGICAL_MIN(-127), HID_LOGICAL_MAX(127),
            HID_REPORT_COUNT(1), HID_REPORT_SIZE(8),
            HID_INPUT(HID_DATA | HID_VARIABLE | HID_RELATIVE),
        HID_COLLECTION_END,

    HID_COLLECTION_END
};
```

**최적화 전략:**
- **즉시 응답**: 절대좌표는 델타 누적이 불필요하므로 터치 즉시 전송
- **좌표 캐싱**: 동일 좌표 연속 전송 방지 (이전 좌표와 동일하면 전송 스킵)
- **해상도 매핑**: 16비트 해상도(0~32767)로 PC 모니터 전체 해상도를 충분히 커버
- **모드 전환 지연 최소화**: 상대↔절대 전환 시 1프레임 이내 적용

**BIOS/UEFI 호환성 참고:**
- Report ID를 사용하는 HID 디스크립터는 Boot Protocol과 호환되지 않음
- BIOS/UEFI 환경에서는 상대좌표(Boot Mouse) 폴백 필요
- ESP32-S3는 호스트의 Set_Protocol 요청에 따라 Boot/Report Protocol 자동 전환

#### 2.4.6.1.2 줌 상태 Vendor CDC 메시지 (AbsolutePointingPad 연동)

AbsolutePointingPad의 줌 기능이 활성화되면, Android는 줌 영역 정보를 Windows 서버로 전송하여 PC 화면 위에 줌 영역 박스 오버레이를 렌더링합니다.

**통신 경로**:
```
Android → UART (0xFF 커스텀 명령) → ESP32 → Vendor CDC → Windows 서버
```

**Vendor CDC 명령 코드**: `VCDC_CMD_ZOOM_STATE (0x30)`

**JSON Payload 구조**:
```json
{
  "command": "ZOOM_STATE",
  "zoom_level": 2.0,
  "min_x": 8192,
  "min_y": 8192,
  "max_x": 24576,
  "max_y": 24576
}
```

| 필드 | 타입 | 범위 | 설명 |
|------|------|------|------|
| `zoom_level` | float | 1.0 ~ 8.0 | 줌 배율. 1.0 = 전체 화면 (오버레이 숨김) |
| `min_x` | int | 0 ~ 32767 | 매핑 범위 X축 최솟값 |
| `min_y` | int | 0 ~ 32767 | 매핑 범위 Y축 최솟값 |
| `max_x` | int | 0 ~ 32767 | 매핑 범위 X축 최댓값 |
| `max_y` | int | 0 ~ 32767 | 매핑 범위 Y축 최댓값 |

**전송 시점**:
- 줌 확정 시 (드래그 후 손 뗄 때): 1회 전송
- 줌 드래그 중: 30Hz 이하로 스로틀링하여 실시간 전송 (PC 오버레이 미리보기용)
- 줌 해제 시 (`zoom_level = 1.0`): 1회 전송 → Windows 서버가 오버레이 숨김

**ESP32 중계 동작**:
- ESP32는 Android로부터 UART로 수신한 줌 상태 커스텀 명령을 파싱하지 않고, Vendor CDC Frame으로 감싸서 Windows 서버로 그대로 전달 (투명 중계)
- Windows 서버 미연결 시 (Essential 모드): 줌 상태 명령은 ESP32에서 폐기

**Windows 서버 수신 처리**: `technical-specification-server.md` §3.6.1.4 참조

#### 2.4.6.2 HID Boot Keyboard Interface 최적화

**프로토콜 스펙:**
```typescript
// HID Boot Keyboard 프레임 구조 (8바이트)
interface HidBootKeyboardFrame {
  modifiers: uint8;   // 모디파이어 키 (Ctrl, Alt, Shift, GUI)
  reserved: uint8;    // 예약 (0)
  keyCodes: uint8[6]; // 동시 입력 가능한 키 코드 (최대 6키)
}
```

**ESP-IDF TinyUSB 기반 구현 방식:**
```c
// ESP32-S3 ESP-IDF 프레임워크 기반 HID Keyboard 구현
#include "tusb.h"

// BridgeOne 8바이트 프레임을 HID Keyboard 리포트로 변환
void processKeyboardFrame(const bridge_frame_t* frame) {
    static uint32_t lastKeyTime = 0;
    uint32_t currentTime = esp_timer_get_time() / 1000; // us → ms
    
    // 키보드 디바운싱 처리
    if (currentTime - lastKeyTime < KEYBOARD_DEBOUNCE_MS) {
        return;
    }
    
    // HID Boot Keyboard 리포트 구조 (8바이트, 불변)
    struct {
        uint8_t modifiers;
        uint8_t reserved;
        uint8_t keyCodes[6];
    } __attribute__((packed)) kb_report;
    
    // 리포트 생성
    kb_report.modifiers = frame->modifiers;
    kb_report.reserved = 0;
    kb_report.keyCodes[0] = frame->keyCode1;
    kb_report.keyCodes[1] = frame->keyCode2;
    kb_report.keyCodes[2] = 0;
    kb_report.keyCodes[3] = 0;
    kb_report.keyCodes[4] = 0;
    kb_report.keyCodes[5] = 0;
    
    // TinyUSB API로 전송 (Interface 0: HID Keyboard)
    tud_hid_n_report(0, 0, &kb_report, sizeof(kb_report));
    
    lastKeyTime = currentTime;
}
```

**HID Keyboard 최적화 타이밍 상수:**
```c
#define KEYBOARD_DEBOUNCE_MS        40    // 키보드 디바운싱
#define KEYBOARD_REPEAT_INITIAL_MS 300    // 초기 반복 지연
#define KEYBOARD_REPEAT_INTERVAL_MS 30    // 반복 간격
#define KEYBOARD_MAX_SIMULTANEOUS    6    // 최대 동시 입력 키 수 (HID Boot 표준)
```

**최적화 전략:**

**성능 최적화:**
- **키 매트릭스**: 8x8 키 상태 매트릭스로 동시 입력 처리
- **디바운싱**: 5ms 하드웨어 디바운싱 + 10ms 소프트웨어 디바운싱
- **타이핑 속도**: 초당 최대 1000키 처리 (롤오버 지원)
- **메모리 관리**: 키 상태를 비트맵으로 압축 저장

**기능 최적화:**
- **모디파이어 처리**: 동시 모디파이어 키 조합 지원 (Ctrl+Alt+Del 등)
- **키 맵핑**: Android 키코드와 HID 키코드 간 매핑 테이블
- **멀티탭 처리**: 더블탭, 트리플탭 등 제스처 인식
- **매크로 지원**: 자주 사용되는 키 조합을 단축키로 등록

**오류 처리:**
- **키 고착 방지**: 물리적 키보드의 키 고착 상태 감지 및 복구
- **입력 검증**: 유효하지 않은 키 조합 필터링
- **타이밍 제어**: 키 릴리즈 타이밍 정밀 제어로 고스트 키 방지

#### 2.4.6.3 Vendor CDC Interface 최적화

**프로토콜 스펙:**
```typescript
// Vendor CDC 명령 프레임 구조
interface VendorCdcFrame {
  header: uint8;      // 0xFF (벤더 명령 식별자)
  command: uint8;     // 명령 타입 (0x01-0xFF)
  length: uint16;     // 페이로드 길이 (Little-Endian, 최대 448 bytes)
  payload: uint8[];   // JSON 형식의 명령 데이터 (UTF-8 인코딩)
  checksum: uint16;   // CRC16 체크섬 (Little-Endian)
}
```

**프레임 크기 제한:**
- **최대 페이로드 크기**: 448 bytes
  - 계산 근거: 512 bytes (ESP32-S3 버퍼) - 1 (header) - 1 (command) - 2 (length) - 2 (checksum) - 58 (예비 영역) = 448 bytes
  - JSON 메시지는 UTF-8 인코딩 후 448 bytes를 초과할 수 없음
- **최소 프레임 크기**: 6 bytes (헤더 + command + length + checksum, payload 없음)
- **전체 프레임 최대 크기**: 454 bytes (1 + 1 + 2 + 448 + 2)

**JSON 페이로드 검증 요구사항:**
- Android 앱: JSON 직렬화 후 바이트 크기 검증 (448 bytes 이하 확인)
- ESP32-S3: `length` 필드 값이 448 이하인지 검증
- Windows 서버: 수신된 프레임의 `length` 필드 값이 실제 페이로드 크기와 일치하는지 검증

**0xFF 헤더 충돌 방지:**
- **length 필드 기반 프레임 추출**: 헤더 0xFF 감지 후 `length` 필드를 읽어 정확한 프레임 종료 위치 계산
- **프레임 구분 알고리즘**:
  1. UART/CDC 스트림에서 0xFF 바이트 검색
  2. 다음 1바이트를 `command` 필드로 읽기 (0x01-0xFE 범위 검증)
  3. 다음 2바이트를 `length` 필드로 읽기 (Little-Endian, 0-448 범위 검증)
  4. `length` 바이트만큼 `payload` 읽기
  5. 마지막 2바이트를 `checksum`으로 읽기
  6. CRC16 검증 수행
- **장점**: JSON 페이로드 내 0xFF 바이트가 있어도 프레임 구분 오류 없음
- **주의**: 잘못된 0xFF 바이트를 헤더로 오인할 경우 `command`나 `length` 검증 실패로 감지됨

**CRC16 불일치 오류 처리 프로토콜:**
- **오류 감지 및 진단 응답**:
  1. 수신측(ESP32-S3 또는 Windows 서버)에서 CRC16 불일치 감지
  2. 해당 프레임 즉시 폐기 (처리하지 않음)
  3. 진단용 `CRC_ERROR` 응답 JSON 전송 (로깅/디버깅 목적):
     ```json
     {
       "command": "ERROR_RESPONSE",
       "error_code": "CRC_MISMATCH",
       "error_message": "CRC16 checksum verification failed",
       "timestamp": "ISO-8601"
     }
     ```

- **복구 전략: 타임아웃 기반 절차 재시도**:
  - 별도의 재전송 요청 명령(CMD_RETRANSMIT)은 사용하지 않음
  - CRC 오류로 프레임이 폐기되면 송신측은 응답을 받지 못하게 되고, 기존 타임아웃 메커니즘에 의해 해당 단계가 자동으로 재시도됨
  - 핸드셰이크 단계: 1초 타임아웃 → 최대 3회 재시도 (지수 백오프: 1초 → 2초 → 4초)
  - Keep-alive: PONG 미수신 시 다음 PING 주기에서 자연스럽게 재시도
  - 3회 재시도 실패 시: 연결 끊김 판정 → Essential 모드 복귀

- **설계 근거**:
  - USB CDC 물리 계층의 CRC 오류율은 극히 낮음 (USB 자체 오류 검출 포함)
  - 별도 재전송 프로토콜은 복잡성 대비 실질적 이득이 적음
  - 기존 타임아웃/재시도 메커니즘이 CRC 오류 상황을 자연스럽게 커버

- **ESP32-S3 구현 요구사항**:
  - CRC16 불일치 시 프레임 폐기 + 로그 기록 + 진단용 `CRC_ERROR` 응답 전송
  - 상태 머신 변경 없음 (타임아웃에 의한 자동 복구에 위임)

- **Windows 서버 구현 요구사항**:
  - CRC16 불일치 시 프레임 폐기 + 로그 기록
  - 진단용 `CRC_ERROR` 응답 전송 (ESP32-S3가 수신하여 로그 기록)
  - 응답 타임아웃 발생 시 해당 단계 자동 재시도

**JSON 파싱 실패 복구 프로토콜:**
- **오류 감지 및 응답**:
  1. 수신측에서 JSON 파싱 실패 감지 (구문 오류, 필수 필드 누락 등)
  2. 즉시 `PARSE_ERROR` 응답 JSON 전송:
     ```json
     {
       "command": "ERROR_RESPONSE",
       "error_code": "JSON_PARSE_ERROR",
       "error_message": "Failed to parse JSON payload: <detail>",
       "timestamp": "ISO-8601"
     }
     ```
  
- **처리 요구사항**:
  - Android 앱: `PARSE_ERROR` 수신 시 재전송 없이 즉시 사용자에게 오류 알림
  - ESP32-S3: 파싱 실패 시 로그 기록 + `PARSE_ERROR` 응답 전송 (양방향)
  - Windows 서버: 파싱 실패 시 ESP32-S3로 `PARSE_ERROR` 응답 전송
  - 파싱 오류는 재전송으로 해결되지 않으므로 재전송 메커니즘 비활성화

**최적화 전략:**

**통신 최적화:**
- **JSON 압축**: 자주 사용되는 명령에 대한 단축 코드 할당
- **스트리밍 처리**: 큰 데이터는 청크 단위로 분할 전송
- **우선순위 큐**: 긴급 명령(시스템 상태 변경)은 우선 처리
- **연결 풀링**: 다중 연결 시 효율적인 연결 자원 관리

**고급 기능 지원:**
- **상태 동기화**: Android 앱과 Windows 서비스 간 실시간 상태 공유
- **이벤트 중계**: 마우스/키보드 이벤트의 Windows 서비스 전달
- **원격 제어**: Windows 서비스에서 Android 앱 기능 제어
- **데이터 캐싱**: 자주 요청되는 정보의 로컬 캐시 관리

**보안 최적화:**
- **인증 프로토콜**: 연결 시 챌린지-응답 인증 방식
- **암호화 통신**: 민감한 명령에 대한 AES256 암호화
- **접근 제어**: 명령별 권한 검사 및 로깅
- **무결성 검증**: 모든 프레임에 대한 디지털 서명 검증

#### 2.4.6.4 통합 최적화 전략

**리소스 관리:**
- **메모리 풀**: 각 인터페이스별 전용 메모리 영역 할당
- **태스크 우선순위**: HID 태스크 > Vendor 태스크 > 관리 태스크
- **전력 최적화**: 인터페이스별 동적 전력 모드 전환
- **자원 경합 방지**: 뮤텍스와 세마포어를 통한 동시성 제어

**성능 모니터링:**
- **실시간 메트릭스**: 각 인터페이스의 처리량, 지연시간, 오류율 추적
- **적응형 조절**: 부하 상황에 따른 폴링 레이트 자동 조정
- **상태 전이**: Essential ↔ Standard 모드 전환 시 무손실 처리
- **진단 로깅**: 상세한 통신 로그로 문제 진단 지원

**확장성 설계:**
- **플러그인 아키텍처**: 새로운 입력 장치 타입의 동적 추가 지원
- **프로토콜 버전 관리**: 하위 호환성을 유지한 프로토콜 업그레이드
- **구성 가능성**: 사용자 설정에 따른 인터페이스 동작 커스터마이징
- **테스트 프레임워크**: 각 인터페이스의 독립적 테스트 및 검증

### 2.5 ESP-IDF USB Device 아키텍처 상세

**ESP-IDF TinyUSB 컴포넌트 구조**:
```
ESP-IDF v5.5+
├── components/tinyusb/        # TinyUSB 스택 (ESP-IDF 내장)
│   ├── src/device/            # USB 디바이스 스택
│   ├── src/class/hid/         # HID 클래스 드라이버
│   └── src/class/cdc/         # CDC 클래스 드라이버
└── main/
    └── usb_descriptors.c      # 프로젝트별 디스크립터 (§0 불변 규칙)
```

**TinyUSB Composite 디바이스 초기화 시퀀스**:
```c
void app_main(void) {
    // 1. TinyUSB 초기화
    tud_init(BOARD_TUD_RHPORT);
    
    // 2. USB 태스크 시작 (tud_task 호출)
    xTaskCreate(usb_task, "USB", 4096, NULL, 5, NULL);
    
    // 3. HID/CDC 핸들러 태스크 시작
    xTaskCreate(hid_task, "HID", 4096, NULL, 9, NULL);
    xTaskCreate(cdc_task, "CDC", 4096, NULL, 8, NULL);
    
    // 4. UART 태스크 시작
    xTaskCreate(uart_task, "UART", 4096, NULL, 10, NULL);
}
```

**USB 디스크립터 구성 계약** (§0 불변 규칙):
- ESP-IDF 프로젝트의 `main/usb_descriptors.c`에서 정의
- TinyUSB 매크로(`TUD_HID_DESCRIPTOR`, `TUD_CDC_DESCRIPTOR`) 활용
- 인터페이스 번호 및 순서 고정으로 호스트 호환성 보장
- 컴파일 타임 디스크립터 검증 (크기, 순서, 클래스 코드)

**ESP-IDF 빌드 시스템 통합**:
```cmake
# main/CMakeLists.txt
idf_component_register(
    SRCS "main.c" "usb_descriptors.c" ...
    REQUIRES driver esp_timer freertos usb
)
```

**sdkconfig 필수 옵션** (§0 프로토콜 준수):
```ini
CONFIG_TINYUSB=y
CONFIG_TINYUSB_HID_COUNT=2        # Keyboard + Mouse
CONFIG_TINYUSB_CDC_COUNT=1        # Vendor CDC
CONFIG_TINYUSB_HID_ENABLED=y
CONFIG_TINYUSB_CDC_ENABLED=y
```

## 3. 상수/임계값 (Standardized Constants)

### 3.1 HID 처리 관련 상수 (ESP-IDF/TinyUSB 기반)

| 상수명 | 값 | 단위 | 설명 | 출처 |
|--------|-----|------|------|------|
| `HID_DEBOUNCE_MS` | 40 | ms | HID 입력 디바운싱 지연 | BridgeOne 프로토콜 표준 |
| `HID_INITIAL_REPEAT_MS` | 300 | ms | 홀드 입력 초기 반복 지연 | BridgeOne 프로토콜 표준 |
| `HID_REPEAT_INTERVAL_MS` | 30 | ms | 홀드 입력 반복 간격 | BridgeOne 프로토콜 표준 |
| `HID_PROCESSING_DELAY_MS` | 2 | ms | TinyUSB 태스크 호출 주기 | TinyUSB 권장 사항 |

**마우스 관련 상수:**

| 상수명 | 값 | 단위 | 설명 | 출처 |
|--------|-----|------|------|------|
| `MOUSE_MOVE_STEP` | 8 | pixel | 마우스 이동 기본 단위 | BridgeOne 프로토콜 표준 |
| `MOUSE_SCROLL_STEP` | 1 | tick | 마우스 휠 스크롤 단위 | BridgeOne 프로토콜 표준 |
| `MOUSE_DELTA_MAX` | 127 | - | 마우스 델타 최대값 (int8 범위) | HID Boot Protocol §0.2 |
| `MOUSE_DELTA_MIN` | -127 | - | 마우스 델타 최소값 (int8 범위) | HID Boot Protocol §0.2 |

**키보드 관련 상수:**

| 상수명 | 값 | 단위 | 설명 | 출처 |
|--------|-----|------|------|------|
| `KEYBOARD_MAX_KEYS` | 6 | 개 | 동시 입력 가능한 최대 키 수 | HID Boot Protocol 표준 |
| `KEYBOARD_MODIFIERS_MASK` | 0xFF | - | 모디파이어 키 비트 마스크 | HID Boot Protocol |

**UART 통신 관련 상수:**

| 상수명 | 값 | 단위 | 설명 | 적용 대상 |
|--------|-----|------|------|----------|
| `UART_BAUD_RATE` | 1000000 | bps | UART 통신 속도 | Android ↔ ESP32-S3 |
| `BRIDGE_FRAME_SIZE` | 8 | byte | BridgeOne 프레임 크기 | 프로토콜 표준 |
| `UART_BUFFER_SIZE` | 128 | byte | UART 링 버퍼 크기 (16프레임) | ESP32-S3 구현 |

**USB HID 관련 상수:**

| 상수명 | 값 | 단위 | 설명 | 적용 대상 |
|--------|-----|------|------|----------|
| `HID_POLLING_RATE` | 1 | ms | HID 인터페이스 폴링 간격 | USB HID Descriptor §0.1 |
| `HID_BOOT_MOUSE_REPORT_SIZE` | 4 | byte | HID Boot Mouse 리포트 크기 | HID Boot Protocol §0.2 |
| `HID_BOOT_KEYBOARD_REPORT_SIZE` | 8 | byte | HID Boot Keyboard 리포트 크기 | HID Boot Protocol §0.2 |
| `HID_ABS_MOUSE_REPORT_SIZE` | 6 | byte | HID 절대좌표 마우스 리포트 크기 (Report ID 제외) | HID Report Protocol |
| `USB_STABILIZATION_DELAY_MS` | 50 | ms | TinyUSB 초기화 후 안정화 지연 | TinyUSB 권장 사항 |

**절대좌표 관련 상수:**

| 상수명 | 값 | 단위 | 설명 | 적용 대상 |
|--------|-----|------|------|----------|
| `REPORT_ID_REL_MOUSE` | 0x01 | ID | 상대좌표 마우스 리포트 ID | HID Report Descriptor |
| `REPORT_ID_ABS_MOUSE` | 0x02 | ID | 절대좌표 마우스 리포트 ID | HID Report Descriptor |
| `ABS_COORDINATE_MAX` | 32767 | 정수 | 절대좌표 최대값 (16비트) | HID Absolute Report |
| `ABS_COORDINATE_MIN` | 0 | 정수 | 절대좌표 최소값 | HID Absolute Report |
| `ABS_FRAME_TYPE_MARKER` | 0x80 | uint8 | 절대좌표 UART 프레임 식별자 | UART 프레임 파싱 |

### 3.2 성능 임계값

**처리량 목표:**

| 지표 | 목표값 | 단위 | 측정 기준 | 검증 방법 |
|------|-------|------|----------|----------|
| HID 프레임 처리량 | ≥ 1000 | frame/sec | ESP32-S3 기준 | 소프트웨어 카운터 |
| UART 데이터 처리량 | ≥ 8000 | byte/sec | 1000 frame × 8 byte | 로직 애널라이저 |
| 엔드투엔드 지연시간 | ≤ 50 | ms | Android → PC 전체 | 하드웨어 측정 |
| ESP32-S3 기여 지연시간 | ≤ 5 | ms | ESP32-S3 내부 처리 | 소프트웨어 타이머 |

**리소스 사용량 제한:**

| 리소스 | 제한값 | 단위 | 기준 | 모니터링 방법 |
|--------|-------|------|------|--------------|
| CPU 사용률 | ≤ 30 | % | 240MHz 기준 | FreeRTOS 통계 |
| 메모리 사용률 | ≤ 70 | % | 512KB RAM 기준 | 힙 모니터링 |
| 전력 소모 (활성) | ≤ 150 | mA | 3.3V 기준 | 전력 측정기 |
| 전력 소모 (절전) | ≤ 10 | mA | Light-sleep 모드 | 전력 측정기 |

## 4. 플랫폼별 주요 로직

BridgeOne 시스템의 각 플랫폼별 핵심 로직과 구현 세부사항은 다음과 같은 전용 기술 명세서에서 상세히 정의되어 있습니다:

### 4.1 Android 앱 주요 로직

**참조 문서**: [`docs/android/technical-specification-app.md`]

**핵심 구현 영역**:
- **USB 통신 아키텍처**: CP2102 칩셋 기반 1Mbps UART 통신, BridgeOne 프로토콜 구현, HID/Vendor CDC 인터페이스 최적화
- **터치패드 알고리즘**: 자유 이동, 직각 이동, 데드존 보상, 스크롤 가이드라인, 멀티 커서 알고리즘
- **컴포넌트 설계**: 상태 알림 토스트, 페이지 인디케이터, 햅틱 피드백, KeyboardKeyButton, ShortcutButton, MacroButton, ContainerButton, DPad
- **UI 상태 제어**: 컴포넌트 비활성화 시스템, 강제 해제 메커니즘, Essential/Standard 모드 관리
- **성능 최적화**: 메모리 관리, UI 렌더링 최적화, 통신 성능 최적화, 배터리 최적화
- **상수 및 임계값**: 레이아웃/UI 상수, 색상 상수, 입력 인식 임계값, 터치패드 동작 상수, 애니메이션 타이밍 상수

### 4.2 ESP32-S3 펌웨어 주요 로직 (ESP-IDF)

**참조 문서**: [`docs/board/esp32s3-code-implementation-guide.md`]
- **§3.7 참고 구현**: esp32-cdc-keyboard 프로젝트 분석 및 BridgeOne 확장 가이드
- **실제 코드 예시**: USB Composite 디스크립터, CDC→HID 변환, TinyUSB 콜백 패턴

**핵심 구현 영역**:
- **UART 통신 모듈** (ESP-IDF): ESP-IDF UART 드라이버 기반 1Mbps 통신, 8바이트 BridgeOne 프레임 구조, 실시간 프레임 파싱 및 검증
- **USB HID 모듈** (TinyUSB): TinyUSB API 직접 제어로 HID Boot Mouse/Keyboard 인터페이스 구현, `tud_hid_n_report()` 기반 리포트 전송
- **Vendor CDC 모듈** (TinyUSB): TinyUSB CDC 콜백 기반 JSON 명령 처리, ESP-IDF `esp_crc` API로 CRC16 검증, 양방향 메시지 중계
- **FreeRTOS 태스크 구조**: 듀얼 코어 활용 (Core 0: UART/HID, Core 1: CDC/USB/Monitor), 우선순위 기반 실시간 처리
- **상태 관리 시스템**: 시스템 상태 정의, 연결 상태 추적, 성능 지표 수집, 오류 통계 관리
- **메시지 중계 시스템**: Android-Windows 양방향 통신 중계, 메시지 ID 관리, 요청-응답 매칭
- **성능 최적화**: ESP-IDF 정적 메모리 할당, ESP-IDF 전력 관리 API, 컴파일러 최적화 (-Os)

### 4.3 Windows 서버 주요 로직

**참조 문서**: [`docs/windows/technical-specification-server.md`]

**핵심 구현 영역**:
- **연결 관리 기술**: 서버 주도적 연결 신호, 3단계 연결 확립, 핸드셰이크 프로토콜, Keep-alive 정책
- **키보드 입력 처리**: 다층 입력 처리 시스템, 상태 기반 관리, Windows API 통합, 성능 최적화
- **멀티 커서 기술**: 멀티 커서 패러다임 (최대 4개), 텔레포트 메커니즘, 가상 커서 렌더링, 동적 커서 상태 반영
- **성능 모니터링**: 실시간 성능 지표 수집, 성능 데이터 처리, 임계값 기반 알림
- **설정 관리**: 계층적 설정 구조, 동적 설정 변경, 설정 동기화
- **스플래시 시스템**: 6단계 애니메이션 시퀀스, WPF/XAML 기반 설계, 성능 및 품질 목표
- **WPF UI 기술**: Fluent Design System 구현, MVVM 패턴, 접근성 지원, 애니메이션 시스템

### 4.4 플랫폼 간 연동 로직

**통합 아키텍처 원칙**:
- **프로토콜 일관성**: 모든 플랫폼에서 동일한 BridgeOne 프로토콜 구조 사용
- **상태 동기화**: Essential/Standard 모드 전환 시 모든 플랫폼 간 일관된 상태 유지
- **오류 처리**: Cancel and Restart 패턴 기반 안정적인 복구 메커니즘
- **성능 목표**: 전체 시스템 50ms 이하 엔드투엔드 지연시간 달성

**플랫폼별 책임 분담**:
- **Android 앱**: 사용자 입력 처리, 터치패드 알고리즘, UI 상태 관리
- **ESP32-S3**: 프로토콜 변환, USB 브릿지 역할, 메시지 중계
- **Windows 서버**: 고급 기능 처리, 멀티 커서 관리, 시스템 통합

#### 4.4.1 Software Macro 실행 (Orbit Mini 연동)

##### 4.4.1.0 MacroButton 타입 및 매크로 동적 할당

Android MacroButton은 두 가지 타입으로 구분됩니다. 타입은 버튼 생성 시 고정되며, 할당된 _매크로_는 롱프레스로 언제든 변경 가능합니다.

| 타입 | 식별 색상 | 실행 경로 | Windows 서버 필요 |
|------|---------|---------|----------------|
| **Orbit Mini** | `#7E57C2` (보라) | Android → ESP32 → Vendor CDC → Windows → Orbit Mini | O |
| **Native** | `#F57C00` (주황) | Android → ESP32 → USB HID 직접 | X |

**Orbit Mini 타입 매크로 목록 실시간 조회 플로우** (롱프레스 시):
```
Android MacroButton 롱프레스
  ↓ MACRO_LIST_REQUEST 전송 (UART → ESP32 → Vendor CDC → Windows)
Windows 서버: isOrbitConnected 확인 → Named Pipe로 MACRO_LIST 전송
Orbit Mini: 현재 등록된 활성 매크로 목록 반환 (MACRO_LIST_RESULT)
Windows 서버: MACRO_LIST_RESPONSE로 변환 → Vendor CDC → ESP32 → Android
Android: 팝업에 목록 표시 → 유저 선택 → SharedPreferences 저장
```

---

##### 4.4.1.1 매크로 실행 플로우 전체 시퀀스

```
1. Android 앱: MacroButton 탭 (UUID: 550e8400-...)
   ↓ UART 전송: {"command": "MACRO_START_REQUEST", "macro_id": "550e8400-..."}
   
2. ESP32-S3: UART 수신 → Vendor CDC 프레임 재구성 (CRC16 추가)
   ↓ USB CDC 전송
   
3. Windows 서버: Vendor CDC 수신 → JSON 파싱
   ↓ message_id 생성: "7a3b2f8c-..."
   ↓ pendingRequests 큐 등록 (30초 타이머 시작)
   ↓ isOrbitConnected 확인
   ↓ Named Pipe 전송: {"message_id": "7a3b2f8c-...", "command": "MACRO_EXECUTE", "payload": {"macro_id": "550e8400-..."}}
   
4. Orbit Mini: Named Pipe 수신 → JSON 파싱
   ↓ macro_id로 매크로 레지스트리 검색
   ↓ 매크로 실행 (키보드/마우스 조작 등)
   ↓ 실행 결과 생성: {"success": true/false, "error_message": "..."}
   ↓ Named Pipe 전송: {"message_id": "7a3b2f8c-...", "command": "MACRO_RESULT", "payload": {...}}
   
5. Windows 서버: Named Pipe 수신 → JSON 파싱
   ↓ message_id로 pendingRequests 큐 검색 → 원본 macro_id 매칭
   ↓ 타이머 취소 및 큐에서 제거
   ↓ Vendor CDC 프레임 재구성: {"command": "MACRO_RESULT", "macro_id": "550e8400-...", "payload": {...}}
   ↓ USB CDC 전송
   
6. ESP32-S3: Vendor CDC 수신 → JSON 파싱
   ↓ UART 전송
   
7. Android 앱: UART 수신 → JSON 파싱
   ↓ macro_id로 MacroButton 매칭
   ↓ UI 업데이트: 성공 시 녹색 토스트, 실패 시 빨간색 토스트 + error_message 표시
```

**플랫폼별 책임**:
- **Android 앱**: MacroButton 탭 감지, 요청 전송, UI 상태 업데이트
- **ESP32-S3**: UART-CDC 프레임 변환, 양방향 중계
- **Windows 서버**: message_id 관리, Orbit 연결 확인, 요청-응답 매칭
- **Orbit Mini**: 매크로 레지스트리 관리, 실제 매크로 실행

##### 4.4.1.2 매크로 실행 오류 처리

| 오류 유형 | 감지 위치 | 응답 시간 | 오류 메시지 | 복구 전략 |
|----------|----------|----------|-----------|----------|
| **Orbit Mini 미연결** | Windows 서버 | 즉시 | `ORBIT_NOT_CONNECTED` | Android 앱에 즉시 알림, Orbit Mini 시작 권장 |
| **매크로 타임아웃** | Windows 서버 | 30초 | `MACRO_TIMEOUT` | Android 앱에 타임아웃 알림, 매크로 재실행 옵션 제공 |
| **존재하지 않는 매크로** | Orbit Mini | 1초 이내 | `MACRO_NOT_FOUND` | Android 앱에 오류 알림, 매크로 ID 확인 권장 |
| **매크로 실행 실패** | Orbit Mini | 가변 | `EXECUTION_ERROR: {상세 메시지}` | Android 앱에 상세 오류 표시, 수동 해결 필요 |
| **목록 조회 타임아웃** | Windows 서버 | 5초 | `MACRO_LIST_TIMEOUT` | Android 앱에 오류 알림, Orbit Mini 연결 상태 확인 권장 |
| **JSON 파싱 오류** | ESP32-S3/Windows | 즉시 | `INVALID_FORMAT` | 로그 기록, Android 앱에 일반 오류 알림 |
| **Named Pipe 끊김** | Windows 서버 | 3초 | `PIPE_DISCONNECTED` | 자동 재연결 시도 (최대 3회), 실패 시 Orbit Mini 재시작 권장 |

각 플랫폼의 상세한 구현 명세와 기술적 요구사항은 해당 참조 문서에서 확인할 수 있습니다.

#### 4.4.2 Native Macro 실행 (HID 직접 전송)

> **개요**: Native Macro는 ESP32-S3가 미리 저장된 HID 입력 시퀀스를 USB HID 리포트로 직접 재생하는 방식입니다. PC 입장에서는 실제 USB HID 하드웨어 입력과 구분이 불가능하여, 커널 레벨 안티치트 시스템 등 소프트웨어 기반 입력 차단을 우회합니다.
>
> **절대좌표 지원**: `AbsolutePointingPad`의 터치 비율(0.0~1.0) → HID 범위(0~32767) 변환 구조를 매크로 액션에 그대로 활용합니다. `MOUSE_MOVE_ABS (0x15)` 액션 타입으로 12-bit 정밀도(0~4095, HID 0~32767로 스케일)의 절대 화면 좌표를 저장합니다. 기존 상대좌표(`MOUSE_MOVE`) 방식은 재생 시작 커서 위치에 따라 최종 위치가 달라지는 문제가 있었지만, 절대좌표 방식은 항상 동일한 화면 위치를 가리키므로 "특정 UI 버튼 클릭" 매크로를 안정적으로 구현할 수 있습니다.

##### 4.4.2.1 Native Macro 실행 플로우 전체 시퀀스

```
1. Android 앱: MacroButton 탭 (macro_id: 0~63)
   ↓ UART 확장 프레임 전송: [0xFF][0x01][macro_id][0x00][0x00][0x00][0x00][checksum]

2. ESP32-S3: seq=0xFF 감지 → 확장 명령 처리기로 분기
   ↓ macro_cmd_queue에 MACRO_EXECUTE 명령 전송

3. ESP32-S3 macro_task: NVS에서 매크로 데이터 로드
   ↓ 액션 시퀀스 순차 실행
   ↓ 각 액션마다 USB HID 리포트 직접 전송 (키/마우스)
   ↓ PC가 Raw Input으로 수신 (하드웨어 입력과 동일)

4. ESP32-S3: 실행 완료 → UART TX로 ACK 응답 전송
   ↓ [0xFF][0xF0][0x01][0x00][0x00][0x00][0x00][checksum]

5. Android 앱: ACK 수신 → UI 업데이트 (완료 햅틱 + 시각 피드백)
```

**플랫폼별 책임**:
- **Android 앱**: MacroButton 탭 감지, 확장 프레임 전송, ACK 수신 및 UI 업데이트
- **ESP32-S3**: 매크로 저장(NVS), 실행 엔진, USB HID 직접 전송
- **Windows 서버**: 관여하지 않음 (Windows 서버 없이도 동작)
- **PC**: Raw Input으로 수신 (하드웨어 입력과 동일하게 처리)

##### 4.4.2.2 Native Macro 오류 처리

| 오류 유형 | 감지 위치 | 응답 시간 | 오류 코드 | 복구 전략 |
|----------|----------|----------|----------|---------|
| **매크로 미존재** | ESP32-S3 | 즉시 | `0x01` (ACK status) | Android 앱에 즉시 알림, 업로드 권장 |
| **실행 중 중단 요청** | ESP32-S3 | 즉시 | - | 전체 키/버튼 해제 리포트 전송 후 중단 |
| **최대 실행 시간 초과** | ESP32-S3 | 300초 후 | `0x02` (ACK status) | 전체 키/버튼 해제 리포트 전송 후 종료 |
| **USB HID 전송 실패** | ESP32-S3 | 즉시 | `0x03` (ACK status) | HID 리포트 큐에 보관 후 재전송 |
| **확장 프레임 체크섬 오류** | ESP32-S3 | 즉시 | - | 프레임 무시, 로그 기록 |

##### 4.4.2.3 Software Macro와 Native Macro 비교

| 항목 | Software Macro | Native Macro |
|------|---------------|-------------|
| **실행 경로** | Android → ESP32 → Vendor CDC → Windows → Orbit → SendInput | Android → ESP32 → USB HID 직접 전송 |
| **저장 위치** | Orbit Mini | ESP32-S3 NVS Flash |
| **Windows 서버 필요** | 필수 | 불필요 |
| **안티치트 우회** | 불가 (소프트웨어 입력 주입) | 가능 (하드웨어 입력과 동일) |
| **지원 기능 범위** | 키보드/마우스 + OS 레벨 조작 (파일 실행 등) | 키보드/마우스 HID 입력 (상대·절대좌표 모두 지원) |
| **최대 매크로 수** | Orbit 저장소 제한 없음 | ESP32-S3 NVS (최대 64개) |
| **최대 액션 수** | Orbit 제한 | 매크로당 최대 255개 |
| **실행 지연** | 전체 경유 시 ~50ms + Orbit 처리 시간 | ESP32 내부 처리만 (~1ms) |
| **매크로 편집** | Orbit Mini에서 | Android 앱 내 편집 화면 |

각 플랫폼의 상세한 구현 명세는 해당 참조 문서에서 확인할 수 있습니다:
- **Android**: `docs/android/technical-specification-app.md` §2.9
- **ESP32-S3**: `docs/board/esp32s3-code-implementation-guide.md` §4.6
- **Windows (관계 설명)**: `docs/windows/technical-specification-server.md` §3.10.10

#### 4.4.3 멀티 커서 시스템 (최대 4개 커서)

> **개요**: 멀티 커서는 터치패드를 N개 영역(pad1~padN, 최대 4개)으로 분할하여 PC 화면의 N개 커서 위치를 독립적으로 제어하는 기능입니다. 실제 OS 커서는 항상 1개이며, Windows 서버가 **N-1개의 가상 커서 오버레이**를 표시하고 **텔레포트**(실제 커서의 순간 위치 이동)로 N개 커서를 시뮬레이션합니다. 커서 수(2~4개)는 멀티 커서 활성화 시점에 팝업에서 매번 선택합니다.
>
> **핵심 원리**: 멀티 커서는 기존 8바이트 HID 프레임 경로를 그대로 사용합니다. 활성 패드에서 발생한 터치 델타는 평소와 동일하게 HID Mouse Report로 전송되어 실제 커서를 움직입니다. 패드 전환 시에만 Vendor CDC JSON 명령이 추가로 전송되어 Windows 서버가 실제 커서를 텔레포트합니다.
>
> **레이아웃 모드**: 터치패드 영역 분할 방식은 설정에서 선택합니다.
> - **그리드 분할** (기본): 터치패드를 N개 영역으로 분할. 비활성 영역 탭으로 즉시 전환. 커서 수에 따라 자동 조정 (2개→1×2, 3개→1×3, 4개→2×2).
> - **직접 전환 버튼**: 터치패드 하단에 N개 버튼 배치. 탭 1번으로 어느 패드든 바로 점프. 터치패드 전체 면적 유지.
>
> **터치패드 면적과 멀티 커서**: 그리드 분할은 N이 커질수록 개별 셀 면적이 줄어듭니다. 직접 전환 버튼 모드는 터치패드 전체 면적을 유지하므로 N이 커져도 조작 정밀도가 일정합니다. `ControlButtonContainer`의 각 버튼은 터치패드 인스턴스별로 표시/숨김과 기본 상태를 독립 구성할 수 있으므로, 어떤 터치패드에 멀티 커서를 배치할지는 앱 레이아웃 설계 시 결정합니다.
>
> **패드 전환 속도**: 멀티 커서의 실용성은 전환 속도에 달려 있습니다. 그리드 분할의 비활성 패드 탭과 직접 전환 버튼 모두 탭 1번으로 즉각 전환됩니다. 선택적으로 소리 감지(마이크 기반, 설정 활성화 필요) 방식도 지원합니다.
>
> **패드 경계와 드래그 워크플로우**: 손가락이 패드 경계를 넘으면 BridgeOne 앱의 홀드 상태가 리셋됩니다. 드래그를 수반하는 작업(텍스트 선택, 파일 이동 등)은 BridgeOne 단독이 아니라 **외부 좌클릭/우클릭 보조 버튼과 조합**하여 수행합니다. 외부 버튼이 클릭 홀드를 독립적으로 유지하므로 OS 레벨에서는 드래그가 끊기지 않으며, BridgeOne은 커서 위치 제어(텔레포트 포함)만 담당합니다.
>
> **⚠️ 멀티 커서 상태는 앱 전역 싱글톤**: Windows 서버가 가상 커서를 전역 1세트로 유지하므로, `MultiCursorState`는 앱 수준에서 하나만 존재해야 합니다. 현재는 멀티 커서가 단일 페이지(Page 2)에만 있어 충돌이 없지만, 두 번째 페이지에 멀티 커서 터치패드를 추가할 때는 반드시 공유 ViewModel로 리팩토링해야 합니다. 그렇지 않으면 페이지 전환 시 각 터치패드의 pad 위치 정보가 Windows 서버의 실제 커서 위치와 어긋나는 버그가 발생합니다.

##### 4.4.3.1 멀티 커서 시스템 아키텍처

```
┌─────────────────────────────────────────────────────────────────────┐
│  Android 앱 (터치패드)                                              │
│                                                                     │
│  [그리드 분할 모드]          [직접 전환 버튼 모드]                     │
│  ┌────┬────┬────┬────┐      ┌──────────────────────┐               │
│  │pad1│pad2│pad3│pad4│      │   터치패드 전체 면적   │               │
│  └──┬─┴──┬─┴──┬─┴──┬─┘      ├──┬──┬──┬─────────────┤               │
│     │    │    │    │        │P1│P2│P3│P4 (전환 버튼)│               │
│  (N개 영역 분할, 최대 4개)    └──┴──┴──┴─────────────┘               │
│                                                                     │
│   [활성 패드의 터치 델타]     [패드 전환 감지]                          │
│         │                        │                                 │
│    8B HID 프레임            JSON 커스텀 명령                          │
│    (기존 경로)              (0xFF 헤더)                               │
└─────────┬───────────────────────┬───────────────────────────────────┘
          │                       │
          ▼                       ▼
┌─────────────────────────────────────────────────────────────────────┐
│  ESP32-S3 (하드웨어 동글)                                            │
│                                                                     │
│  UART RX → [8B 프레임] → USB HID Mouse Report → PC OS 커서 이동     │
│  UART RX → [JSON 명령] → Vendor CDC 중계 → Windows 서버             │
│  Vendor CDC ← [JSON ACK] ← Windows 서버 → UART TX → Android        │
└─────────────────────────────────────────────────────────────────────┘
          │                       │
          ▼                       ▼
┌─────────────────────────────────────────────────────────────────────┐
│  PC (Windows)                                                       │
│                                                                     │
│  OS 커서 ← HID Mouse Report (활성 패드의 델타로 이동)                 │
│                                                                     │
│  Windows 서버 ← Vendor CDC (multi_cursor_switch 명령)                │
│    ├─ 현재 커서 위치 저장 (이전 패드용)                                │
│    ├─ 실제 커서 텔레포트 (대상 패드의 저장 위치로)                      │
│    ├─ N-1개 가상 커서 오버레이 위치 업데이트 (비활성 패드 위치 표시)     │
│    └─ ACK 응답 → ESP32-S3 → Android                                 │
└─────────────────────────────────────────────────────────────────────┘
```

##### 4.4.3.2 HID 프레임과 멀티 커서의 관계

멀티 커서 모드에서도 **마우스 이동의 데이터 경로는 변하지 않습니다**:

| 동작 | 데이터 경로 | 프로토콜 | 비고 |
|------|-----------|---------|------|
| **커서 이동** (활성 패드 터치 드래그) | Android → UART → ESP32 → USB HID Mouse → PC OS | 8바이트 HID 프레임 | 싱글 커서와 완전히 동일 |
| **패드 전환** (비활성 패드 터치) | Android → UART → ESP32 → Vendor CDC → Windows 서버 | 0xFF + JSON | 멀티 커서 전용 |
| **모드 활성화/비활성화** | Android → UART → ESP32 → Vendor CDC → Windows 서버 | 0xFF + JSON | `show_virtual_cursor` / `hide_virtual_cursor` |
| **클릭/스크롤** (활성 패드) | Android → UART → ESP32 → USB HID Mouse → PC OS | 8바이트 HID 프레임 | 싱글 커서와 완전히 동일 |

**핵심**: PC OS는 멀티 커서의 존재를 모릅니다. OS 입장에서는 항상 1개의 마우스가 1개의 커서를 움직이는 것이며, Windows 서버가 텔레포트와 N-1개의 가상 커서 오버레이로 "N개 커서" 경험을 만들어냅니다.

##### 4.4.3.3 멀티 커서 활성화 플로우

```
1. Android 앱: CursorModeButton 탭 (싱글 → 멀티)
   ↓ 커서 수 선택 팝업 표시 (2개 / 3개 / 4개)
   ↓ 사용자가 커서 수(N) 선택 → 즉시 활성화 진행
   ↓ 설정된 레이아웃 모드 로드
   ↓ [그리드 분할 모드] 터치패드를 N개 영역으로 분할 (300ms ease-out 애니메이션)
      - N=2: 좌/우 50% 분할 (1×2)
      - N=3: 좌/중/우 33% 분할 (1×3)
      - N=4: 2×2 그리드 분할
   ↓ [직접 전환 버튼 모드] 하단에 N개 전환 버튼 표시 (터치패드 면적 유지)
   ↓ pad1을 기본 Selected 상태로 설정
   ↓ JSON 커스텀 명령 전송: show_virtual_cursor { cursor_count: N }

2. ESP32-S3: 0xFF 헤더 감지 → Vendor CDC로 중계

3. Windows 서버: show_virtual_cursor 수신
   ↓ pad1 커서 위치 = 현재 실제 커서 위치 (GetCursorPos)
   ↓ pad2~padN 커서 위치 = 현재 실제 커서 위치 (pad1과 동일한 위치에서 시작)
   ↓ N-1개의 가상 커서 오버레이 윈도우 생성 (투명, 클릭 관통)
   ↓ 각 가상 커서 이미지를 pad2~padN 위치에 표시
   ↓ ACK 응답 전송 → ESP32-S3 → Android

4. Android 앱: ACK 수신 → UI 확정 (테두리 색상, 버튼 상태)
   ↓ 이제 pad1 = 일반 HID 마우스 이동 (기존과 동일)
   ↓ 비활성 패드 탭(또는 전환 버튼) = 패드 전환 트리거
```

##### 4.4.3.4 패드 전환 (텔레포트) 플로우

```
상태: pad1이 Selected, 사용자가 pad1에서 커서 이동 중
      실제 커서 = pad1 영역의 PC 커서 위치
      가상 커서(N-1개) = pad2~padN의 저장된 위치에 오버레이 표시

1. Android 앱: 사용자가 padX(비활성) 영역 탭 또는 전환 버튼 탭
   ↓ 디바운스 확인 (50ms 이내 재전환 무시)
   ↓ 이전 상태 저장 (롤백용)
   ↓ Selected: 현재 패드 → padX 전환
   ↓ DeltaCalculator previousX/Y 초기화 (터치 점프 방지)
   ↓ JSON 전송: multi_cursor_switch { touchpad_id: "padX", cursor_position: padX 저장 위치 }
   ↓ ACK 타임아웃 타이머 시작 (500ms)

2. ESP32-S3: Vendor CDC로 중계

3. Windows 서버: multi_cursor_switch 수신
   ↓ 이전 활성 패드 위치 저장 = 현재 실제 커서 위치 (GetCursorPos)
   ↓ 실제 커서 텔레포트 = padX 저장 위치로 SetCursorPos
   ↓ 가상 커서 오버레이 재배치 (모든 비활성 패드 위치 표시)
   ↓ 텔레포트 애니메이션 (450ms: 페이드아웃 → 글로우 → 페이드인)
   ↓ ACK 응답 전송 { success: true, actual_position, latency_ms }

4. Android 앱: ACK 수신
   ↓ 타임아웃 타이머 취소
   ↓ UI 테두리 색상 업데이트 (padX = 보라색 #B552F6, 나머지 = 테두리 없음)
   ↓ 이제 padX 터치 = 일반 HID 마우스 이동
   ↓ 다른 패드 탭(또는 전환 버튼) = 패드 전환 트리거
```

##### 4.4.3.5 멀티 커서 비활성화 플로우

```
1. Android 앱: CursorModeButton 탭 (멀티 → 싱글)
   ↓ 터치패드 분할 해제 (200ms ease-in 통합 애니메이션)
   ↓ JSON 커스텀 명령 전송: hide_virtual_cursor

2. Windows 서버: hide_virtual_cursor 수신
   ↓ 가상 커서 오버레이 윈도우 전체 제거
   ↓ ACK 응답 전송

3. Android 앱: ACK 수신 → 싱글 커서 모드 확정
   ↓ 실제 커서는 마지막 활성 패드 위치에 그대로 유지
```

##### 4.4.3.6 기능 모드와 멀티 커서의 상호작용

**기능 모드는 패드별(per-pad) 독립입니다.** 각 패드는 자신만의 `PadModeState`를 가지며, 패드 전환 시 `ControlButtonContainer`의 버튼들이 새로 활성화된 패드의 상태를 즉시 반영합니다.

| 기능 모드 | 멀티 커서에서의 동작 | 설명 |
|----------|-------------------|------|
| **클릭 모드** (좌/우) | 패드별 독립 | 각 패드의 `PadModeState.clickMode` 참조. pad1=좌클릭, pad2=우클릭으로 설정 가능 |
| **이동 모드** (자유/직각) | 패드별 독립 | 각 패드의 `PadModeState.moveMode` 참조. 직각 이동이 필요한 패드와 그렇지 않은 패드를 독립 구성 가능 |
| **스크롤 모드** (일반/무한) | 패드별 독립 | 각 패드의 `PadModeState.scrollMode` 참조. 스크롤은 활성 패드의 실제 커서 위치 기준으로 발생 |
| **DPI / 스크롤 감도** | 패드별 독립 | 각 패드의 `PadModeState.dpi` 참조. 정밀 작업 패드와 빠른 이동 패드를 독립 설정 가능 |
| **포인터 다이나믹스 (가속)** | 전역 (모드 프리셋 전환 시 함께 교체 가능) | 가속 알고리즘 프리셋은 전역 설정 참조. 모드 프리셋에 포함되어 프리셋 전환 시 같이 변경됨 |

**`ControlButtonContainer` 위치 고정 원칙**: 멀티 커서 활성화 여부나 활성 패드 전환과 무관하게, 버튼 컨테이너는 항상 **원본 전체 터치패드(`TouchpadWrapper`) 기준 좌표에 고정**됩니다. 활성 패드가 분할된 셀 중 하나로 바뀌어도 버튼들은 이동하지 않으며, 이는 패드 전환 시 버튼 위치가 달라져 발생하는 조작 혼란을 방지합니다.

##### 4.4.3.7 멀티 커서 오류 처리

| 오류 유형 | 감지 위치 | 응답 | 복구 전략 |
|----------|----------|------|----------|
| **패드 전환 ACK 타임아웃** (500ms) | Android 앱 | WARNING 토스트 | 이전 패드 상태로 롤백, 1회 재시도 |
| **INVALID_POSITION** | Windows 서버 | 즉시 ACK | 이전 상태로 롤백, 오류 토스트 |
| **WINDOWS_SERVICE_DISCONNECTED** | Android 앱 | 모드 전환 | 멀티 커서 강제 비활성화, Essential 모드로 폴백 |
| **가상 커서 렌더링 실패** | Windows 서버 | 로그 기록 | 오버레이 없이 텔레포트만 동작 (기능 저하 허용) |
| **텔레포트 SetCursorPos 실패** | Windows 서버 | ACK에 에러 코드 | Android에 알림, 이전 상태로 롤백 |

##### 4.4.3.8 멀티 커서 성능 목표

| 지표 | 목표 | 근거 |
|------|------|------|
| **패드 전환 ~ 텔레포트 완료** | 50ms 이내 | 사용자가 "즉각적"으로 느끼는 임계값 |
| **ACK 타임아웃** | 500ms | 목표 시간의 10배 여유 |
| **가상 커서 위치 업데이트** | 16ms 주기 | 60fps 렌더링 기준 |
| **디바운스 간격** | 50ms | 의도치 않은 경계 왕복 방지 |
| **JSON 페이로드 크기** | 448B 이내 | Vendor CDC 최대 페이로드 제한 |

**플랫폼별 책임**:
- **Android 앱**: 터치패드 분할 UI (그리드/직접 전환 버튼), 패드 전환 감지, 델타 계산기 리셋, JSON 명령 전송/ACK 수신
- **ESP32-S3**: UART ↔ Vendor CDC 양방향 JSON 중계 (HID 프레임은 기존과 동일하게 처리)
- **Windows 서버**: N개 커서 위치 저장/복원, 텔레포트 실행, N-1개 가상 커서 오버레이 렌더링

각 플랫폼의 상세한 구현 명세는 해당 참조 문서에서 확인할 수 있습니다:
- **Android**: `docs/android/technical-specification-app.md` §2.2.6
- **Windows**: `docs/windows/technical-specification-server.md` §3.6
- **ESP32-S3**: `docs/board/esp32s3-code-implementation-guide.md` §11.1 (신호 매트릭스)
- **컴포넌트 UI**: `docs/android/component-touchpad.md` §1.2, §3.2.4

## 5. 외부 프레임워크/라이브러리 분석

### 5.1 android-hid-client 프로젝트 분석

**참조 저장소**: [Arian04/android-hid-client](https://github.com/Arian04/android-hid-client)

#### 5.1.1 개요 및 활용 가능성

`android-hid-client`는 Android 기기를 USB HID 키보드와 마우스로 변환하는 오픈소스 프로젝트입니다. 본 프로젝트는 루트 권한이 필요한 제약사항이 있지만, HID 프로토콜 처리와 입력 장치 에뮬레이션 측면에서 BridgeOne에 유용한 인사이트를 제공합니다.

**BridgeOne 적용 가능성**:
- Android 측면에서 터치 입력을 HID 프레임으로 변환하는 로직 참고
- ESP32의 USB 복합 장치 구현 시 Arduino USB 라이브러리 활용
- HID Boot Protocol과 HID Report Protocol 구분 방식 차용

#### 5.1.2 핵심 기술 요소 분석

**USB HID 프로토콜 구현**:
- **캐릭터 디바이스 활용**: `/dev/hidg0`, `/dev/hidg1`을 통한 HID 기능 추가
- **프로토콜 구분**: BIOS/UEFI 호환을 위한 Boot Protocol과 확장 기능을 위한 Report Protocol
- **프레임 구조**: 8바이트 델타 프레임을 통한 효율적인 입력 데이터 전송

**Android 측면 적용**:
```kotlin
// android-hid-client에서 참고할 수 있는 HID 처리 접근법
class HidInputProcessor {
    private val hidDevice: UsbDevice
    private val inputManager: InputManager

    fun processTouchInput(event: MotionEvent): HidFrame {
        // 터치 이벤트를 HID 마우스 프레임으로 변환
        return HidFrame(
            buttons = getButtonState(event),
            deltaX = calculateDeltaX(event),
            deltaY = calculateDeltaY(event),
            wheel = 0,
            modifiers = 0,
            key1 = 0,
            key2 = 0
        )
    }
}
```

**ESP32 측면 적용**:
- **Arduino USB 라이브러리**: 경량화된 USB 복합 장치 구현
- **FreeRTOS 기반 실시간 처리**: HID 프레임 처리 최적화
- **Vendor CDC 인터페이스**: 확장 기능 지원

#### 5.1.3 BridgeOne 적용 전략

**Android 앱에서 활용**:
1. **터치패드 입력 처리**: android-hid-client의 입력 변환 로직을 참고하여 터치 이벤트를 HID 프레임으로 변환
2. **USB OTG 통신**: Android의 USB Host API를 활용한 ESP32와의 HID 데이터 전송
3. **입력 모드 전환**: Essential/Standard 모드에 따른 프로토콜 선택

**ESP32 펌웨어에서 활용**:
1. **USB 복합 장치 구성**: Arduino USB 라이브러리 기반으로 HID + Vendor CDC 인터페이스 구현
2. **프로토콜 처리 최적화**: Boot Protocol과 Report Protocol을 상황에 맞게 전환
3. **실시간 성능 최적화**: FreeRTOS 태스크 설계를 통한 처리 지연 최소화

#### 5.1.4 한계점 및 대안

**현재 한계사항**:
- 루트 권한 필요: BridgeOne은 루트 권한 없이 동작하도록 설계되어야 함
- SELinux 정책: 보안 정책 우회 문제로 직접 적용 어려움
- 특정 루팅 방식 의존: Magisk/KernelSU에만 한정

**BridgeOne 적용 시 대안**:
1. **Android USB Host API 활용**: 루트 권한 없이 USB 통신 구현
2. **ADB USB 권한**: 개발자 모드에서 USB 디버깅 권한 활용
3. **Custom HID 드라이버**: Windows 측에서 커스텀 드라이버로 HID 기능 우회
4. **WebUSB API**: 브라우저 기반 USB 통신으로 루트 권한 우회

#### 5.1.5 추천 라이브러리 및 프레임워크

**Android 측**:
- **USB Serial Library**: `com.github.felHR85/UsbSerial` - USB 시리얼 통신
- **Android HID API**: `android.hardware.usb` - USB Host 기능 활용
- **Input Processing**: `android.view.InputEvent` - 터치/키보드 입력 처리

**ESP32 측**:
- **Arduino USB 라이브러리**: 경량화된 USB 프로토콜 스택
- **FreeRTOS**: 실시간 태스크 스케줄링
- **Arduino Framework**: ESP32 개발 편의성

**통합 솔루션**:
- **Protocol Buffers**: 효율적인 데이터 직렬화
- **libusb**: 크로스 플랫폼 USB 라이브러리
- **pyusb**: Python 기반 USB 통신 테스트

#### 5.1.6 성능 및 보안 고려사항

**성능 최적화**:
- 8바이트 델타 프레임 구조로 대역폭 효율 극대화
- 비동기 처리로 입력 지연 시간 최소화
- 버퍼 관리 전략으로 메모리 사용량 최적화

**보안 강화**:
- 입력 데이터 검증 및 필터링
- 권한 기반 접근 제어
- 안전한 프로토콜 설계로 중간자 공격 방지

**BridgeOne 특화 최적화**:
- 배터리 효율을 위한 동적 전력 관리
- 연결 상태 모니터링 및 자동 복구
- Essential/Standard 모드 전환 시 무손실 데이터 처리

### 5.2 usb-serial-for-android 프로젝트 분석

**참조 저장소**: [mik3y/usb-serial-for-android](https://github.com/mik3y/usb-serial-for-android)

#### 5.2.1 개요 및 활용 가능성

`usb-serial-for-android`는 Android 기기에서 USB 시리얼 통신을 위한 오픈소스 드라이버 라이브러리입니다. Android 3.1부터 지원되는 USB Host Mode를 활용하여 루트 권한 없이 다양한 USB-to-Serial 하드웨어와 통신할 수 있습니다.

**BridgeOne 적용 가능성**:
- Android와 ESP32 간 USB 시리얼 통신 구현
- CDC ACM 클래스 기반 ESP32 펌웨어와의 호환성
- 다양한 USB-to-Serial 칩셋 지원으로 확장성 확보
- 안정적인 데이터 전송을 위한 입출력 관리 기능 활용

#### 5.2.2 핵심 기술 요소 분석

**USB 시리얼 통신 아키텍처**:
- **Android USB Host API 활용**: `android.hardware.usb` 패키지 기반 구현
- **다중 드라이버 지원**: FTDI, Prolific, Silicon Labs, QinHeng 등 주요 칩셋 지원
- **CDC ACM 프로토콜**: USB Communication Device Class Abstract Control Model
- **비동기 입출력**: `SerialInputOutputManager`를 통한 이벤트 기반 데이터 처리

**주요 인터페이스 구조**:
```java
// usb-serial-for-android의 핵심 인터페이스 구조
public interface UsbSerialDriver {
    UsbDevice getDevice();
    List<UsbSerialPort> getPorts();
}

public interface UsbSerialPort {
    void open(UsbDeviceConnection connection);
    void setParameters(int baudRate, int dataBits, int stopBits, int parity);
    int write(byte[] buffer, int timeout);
    int read(byte[] buffer, int timeout);
    void close();
}
```

**ESP32 측면 적용**:
- **Arduino CDC ACM 클래스**: ESP32에서 구현 가능한 USB 시리얼 인터페이스
- **Vendor CDC 활용**: 확장 기능을 위한 벤더 정의 시리얼 통신
- **실시간 데이터 전송**: HID와 병행하여 보조 통신 채널로 활용

#### 5.2.3 BridgeOne 적용 전략

**Android 앱에서 활용**:
1. **USB 시리얼 통신 관리**: ESP32와의 양방향 데이터 전송
2. **연결 안정성 확보**: 자동 재연결 및 에러 복구 메커니즘
3. **다중 포트 지원**: ESP32의 복합 USB 장치 인터페이스 활용
4. **데이터 스트리밍**: 실시간 입력 데이터 전송을 위한 버퍼 관리

**ESP32 펌웨어에서 활용**:
1. **CDC ACM 구현**: Arduino USB 라이브러리 기반 USB 시리얼 포트 구성
2. **Vendor CDC 인터페이스**: HID 외 보조 통신 채널 제공
3. **데이터 프로토콜**: 구조화된 명령/응답 프레임워크 구현

**통합 통신 프로토콜**:
```kotlin
// BridgeOne에서 활용할 수 있는 USB 시리얼 통신 접근법
class Esp32SerialManager {
    private val usbSerialPort: UsbSerialPort
    private val inputOutputManager: SerialInputOutputManager

    fun initializeCommunication() {
        // ESP32 연결 및 포트 설정
        usbSerialPort.setParameters(
            baudRate = 115200,
            dataBits = UsbSerialPort.DATABITS_8,
            stopBits = UsbSerialPort.STOPBITS_1,
            parity = UsbSerialPort.PARITY_NONE
        )

        // 이벤트 기반 데이터 수신
        inputOutputManager = SerialInputOutputManager(usbSerialPort, dataListener)
        inputOutputManager.start()
    }

    private val dataListener = SerialInputOutputManager.Listener {
        override fun onNewData(data: ByteArray) {
            processEsp32Data(data)
        }

        override fun onRunError(e: Exception) {
            handleConnectionError(e)
        }
    }
}
```

#### 5.2.4 한계점 및 대안

**BridgeOne 적용 시 대안**:
1. **커스텀 VID/PID 지원**: ESP32의 벤더 ID/제품 ID 등록으로 안정성 향상
2. **연결 모니터링**: USB 연결 상태 실시간 감지 및 사용자 알림
3. **전력 최적화**: 연결 유휴 시 저전력 모드 전환

#### 5.2.5 추천 라이브러리 및 프레임워크

**Android 측**:
- **USB Serial Library**: `com.github.mik3y:usb-serial-for-android:3.9.0`
- **Android USB API**: `android.hardware.usb` - USB Host 기능 활용
- **Permissions**: `android.permission.USB_PERMISSION` - USB 권한 획득
- **Device Filter**: VID/PID 기반 자동 장치 인식

**ESP32 측**:
- **Arduino USB 라이브러리**: CDC ACM 클래스 구현
- **Arduino Framework**: ESP32 개발 편의성
- **FreeRTOS**: 실시간 데이터 처리

**통합 솔루션**:
- **Protocol Buffers**: 효율적인 데이터 직렬화
- **RxJava**: 비동기 통신 처리
- **OkHttp**: 백그라운드 서비스 내 HTTP 통신

#### 5.2.6 성능 및 보안 고려사항

**성능 최적화**:
- **버퍼 관리**: 입력/출력 버퍼 크기 최적화로 메모리 효율 극대화
- **비동기 처리**: `SerialInputOutputManager`를 통한 블록킹 없는 데이터 처리
- **플로우 제어**: 하드웨어/소프트웨어 플로우 제어로 데이터 손실 방지
- **연결 안정성**: 자동 재연결 메커니즘으로 통신 지속성 보장

**보안 강화**:
- **권한 검증**: USB 권한 획득 과정에서 사용자 의도 확인
- **데이터 검증**: 수신 데이터 무결성 검사 및 유효성 검증
- **연결 보안**: 인증된 ESP32 장치만 연결 허용
- **프라이버시**: 통신 데이터의 민감 정보 보호

**BridgeOne 특화 최적화**:
- **배터리 효율**: USB 연결 상태에 따른 전력 관리 전략
- **연결 복구**: 네트워크 변경 시 자동 재연결 로직
- **사용자 경험**: 연결 상태를 직관적으로 표시하는 UI 피드백
- **다중 프로토콜 지원**: HID + 시리얼 복합 통신으로 기능 확장

## 6. 플랫폼별 용어집

### 6.1 플랫폼 공통 용어집

#### 6.1.1 상태 및 모드 관련 용어

| 용어 | 설명 | 상태값 | 사용 맥락 |
|------|------|--------|----------|
| **Essential/Standard** | 시스템 운용 상태 | Essential (서버 미연결), Standard (서버 연결) | 모든 플랫폼에서 기능 제한 정도 표시 |
| **Selected/Unselected** | 시각적 선택 상태 | UI 컴포넌트의 선택/강조 여부 표시 | 모든 플랫폼의 UI 컴포넌트에서 사용 |
| **Enabled/Disabled** | 입력 가능 상태 | 상호작용 가능 여부 | 모든 플랫폼의 버튼, 설정 등에서 사용 |

#### 6.1.2 통신 프로토콜 관련 용어

| 용어 | 설명 | 프로토콜 | 사용 모드 |
|------|------|----------|----------|
| **Keep-alive** | 연결 상태 확인 패킷 | 500ms 주기 전송 | 모든 플랫폼에서 연결 모니터링 |
| **Cancel and Restart** | 오류 복구 패턴 | 전체 상태 재시작 | 모든 플랫폼에서 연결 오류 시 사용 |
| **Vendor CDC Interface** | 벤더 정의 통신 인터페이스 | JSON 기반 커스텀 명령 | 고급 기능 (매크로, 멀티 커서)에서 사용 |

#### 6.1.3 매크로 관련 용어

| 용어 | 설명 | 실행 방식 | 상태 관리 | 비활성화 전략 |
|------|------|----------|----------|--------------|
| **활성 매크로** | 키보드/마우스 조작 동반 매크로 | SendInput API | UI 차단 모드 | 전체 인터페이스 입력 차단 |
| **비활성 매크로** | 메시지 전송 기반 매크로 | SendMessage/PostMessage | UI 관리 모드 | 개별 컴포넌트 선택적 비활성화 (작업 완료 시 자동 복원) |
| **매크로 실행 타임아웃** | 매크로 실행 제한 시간 | 30,000ms | 타임아웃 타이머 | 작업 중단 + UI 자동 복원 + 오류 알림 |
| **매크로 강제 해제** | 실행 중 매크로 중단 | 재탭으로 취소 요청 | MACRO_CANCEL_REQUEST | UI_FORCE_ENABLE_ALL_TOUCHABLES_REQUEST 전송 |
| **Orbit Mini** | 외부 매크로 실행 엔진 (정식 명칭) | Windows Named Pipe | 독립 프로세스 | BridgeOne 서버와 IPC 통신 |
| **Orbit Mini 타입 MacroButton** | Orbit Mini 매크로를 실행하는 버튼 타입 | UUID v4 macro_id 기반 | 보라색 (`#7E57C2`), 배지 "OM" | 롱프레스로 실시간 목록 조회 후 매크로 변경 가능 |
| **Native 타입 MacroButton** | ESP32-S3 Native Macro를 실행하는 버튼 타입 | 정수 슬롯 번호 기반 | 주황색 (`#F57C00`), 배지 "NT" | 롱프레스로 앱 내 캐시 목록에서 매크로 변경 가능 |
| **매크로 ID** | UUID v4 기반 Orbit Mini 매크로 고유 식별자 | 예: `550e8400-e29b-41d4-a716-446655440000` | Android → ESP32-S3 → Windows → Orbit Mini 전달 | 요청-응답 매칭에 사용 |

#### 6.1.4 용어 사용 원칙

**플랫폼 공통 용어 사용 규칙**:
- **시각적 선택 상태**: `Selected/Unselected` (UI 강조, 네비게이션 선택 등)
- **입력 가능 상태**: `Enabled/Disabled` (상호작용 허용 여부, 버튼/설정 등)
- **운용 상태**: `Essential/Standard` (기능 제한 정도, 서버 연결 여부)
- **연결 상태 확인**: `Keep-alive` (주기적 연결 상태 확인)
- **오류 복구**: `Cancel and Restart` (전체 상태 재시작)

**플랫폼 간 용어 일관성 원칙**:
1. **표준화된 용어 우선 사용**: 플랫폼 공통 용어집에 정의된 용어 우선 사용
2. **상태 구분 명확성**: 상태의 성격에 맞는 적절한 용어 선택
3. **플랫폼 일관성**: Android와 Windows 간 용어 일관성 유지
4. **사용자 이해도**: 직관적이고 명확한 용어 선택

**참조 문서**:
- Android 앱 기술 명세: `technical-specification-app.md`
- Windows 서버 기술 명세: `technical-specification-server.md`

### 6.2 Android 플랫폼 용어집

#### 6.2.1 상태 및 모드 관련 용어

| 용어 | 설명 | 상태값 | 사용 맥락 |
|------|------|--------|----------|
| **TransportState** | USB 연결 상태를 나타내는 상태값 | `NoTransport`, `UsbOpening`, `UsbReady` | USB 장치 연결 및 통신 상태 관리 |
| **AppState** | Android 앱 전체 상태 | `WaitingForConnection`, `Essential`, `Standard` | 앱 화면 표시 및 기능 제한 결정 |

#### 6.2.2 통신 프로토콜 관련 용어

| 용어 | 설명 | 프로토콜 | 사용 모드 |
|------|------|----------|----------|
| **BridgeOne 프로토콜** | 8바이트 통합 프레임 구조 | UART 기반 양방향 통신 | 모든 통신에서 사용 |
| **HID Boot Protocol** | BIOS/UEFI 호환 프로토콜 | 마우스/키보드 입력 | Essential 모드 전용 |
| **HID Report Protocol** | 확장 기능 지원 프로토콜 | 마우스/키보드 + 고급 기능 | Standard 모드에서 사용 |
| **8바이트 델타 프레임** | 통합 입력 데이터 구조 | `seq + buttons + deltaX/Y + wheel + modifiers + key1/2` | 모든 HID 통신에서 사용 |

#### 6.2.3 하드웨어 및 구성 요소 관련 용어

| 용어 | 설명 | 사양 | 역할 |
|------|------|------|------|
| **ESP32-S3-DevkitC-1-N16R8** | 메인 마이크로컨트롤러 보드 | Espressif 공식 개발 보드, 듀얼 코어 240MHz | USB 복합 장치 및 HID 처리, UART 브릿지 |
| **내장 USB-to-UART 포트** | Android ↔ ESP32-S3 통신 인터페이스 | ESP32-S3 내장, 1Mbps, 8N1 | Android 스마트폰 연결, 8바이트 프레임 전송 |
| **USB OTG 포트** | ESP32-S3 ↔ PC 통신 인터페이스 | 복합 USB 장치 (HID + CDC) | PC와의 HID/Vendor CDC 연결 |
| **USB OTG Y 케이블** | 동시 충전 및 데이터 전송 케이블 | Charge-while-OTG 지원 | Android 충전 + USB 연결 |
| **Samsung Galaxy s10e** | 타겟 Android 디바이스 | 2280×1080, 5.8인치, Android 12 | 앱 실행 및 사용자 인터페이스 |

#### 6.2.4 UI 컴포넌트 관련 용어

| 용어 | 설명 | 구현 방식 | 주요 기능 |
|------|------|----------|----------|
| **TouchpadWrapper** | 최상위 터치패드 컨테이너 | 1:2 비율, 반응형 크기 | 모든 터치 입력 처리 |
| **TouchpadAreaWrapper** | 터치 인터페이스 영역 | 둥근 모서리, 영역별 색상 | 제스처 인식 및 좌표 변환 |
| **ControlButtonContainer** | 제어 버튼 컨테이너 | 상단 오버레이, 15% 높이 | 모드 전환 버튼들 |
| **KeyboardKeyButton** | 키보드 키 버튼 | KeyDown/KeyUp 전송 | 단일 키 입력 (BIOS 호환) |
| **ShortcutButton** | 단축키 버튼 | 키 조합 시퀀스 전송 | Ctrl+C, Alt+Tab 등 복합키 |
| **MacroButton** | 매크로 버튼 | PC Windows 서비스 실행 | 매크로 실행 및 상태 관리 |
| **ContainerButton** | 컨테이너 버튼 | 팝업 오버레이 | F1-F12 등 그룹 버튼 |
| **DPad** | 방향키 패드 | 8분할 섹터, VectorDrawable | 게임 및 방향키 입력 |
| **ScrollGuideline** | 스크롤 방향 가이드 | 선 패턴, 색상별 표시 | 스크롤 방향 및 속도 표시 |

#### 6.2.5 입력 처리 관련 용어

| 용어 | 설명 | 임계값/파라미터 | 적용 대상 | 관련 컴포넌트 |
|------|------|----------------|----------|--------------|
| **Dead Zone** | 입력 무시 영역 (미세 떨림 방지) | 15dp | 모든 터치 입력 | TouchpadWrapper, DPad |
| **DPI** | 마우스 감도 레벨 | 낮음(0.5x)/보통(1.0x)/높음(1.5x) | 커서 이동 속도 | DPIControlButton |
| **Sticky Hold** | 지속 입력 모드 | 500ms 롱프레스 | 방향키 연속 입력 | DPad, KeyboardKeyButton |
| **멀티 커서 모드** | 다중 커서 지원 모드 (최대 4개) | N개 영역 분할 또는 직접 전환 버튼 | N개 영역 독립 제어 | TouchpadAreaWrapper (그리드/전환 버튼) |
| **싱글 커서 모드** | 단일 커서 표준 모드 | 전체 영역 사용 | 표준 마우스 조작 | TouchpadAreaWrapper |
| **커서 이동 모드** | 마우스 포인터 이동 모드 | 자유/직각 이동 | 드래그 제스처 처리 | MoveModeButton |
| **스크롤 모드** | 휠 스크롤 처리 모드 | 일반/무한 스크롤 | 세로 드래그 → 스크롤 | ScrollModeButton |
| **일반 스크롤** | 정량적 스크롤 방식 | 50dp 단위, 선형 증가 | 표준 스크롤바 조작 | ScrollGuideline (초록색) |
| **무한 스크롤** | 관성 스크롤 방식 | 지수 감속 (0.95^배율) | 자연스러운 스크롤 | ScrollGuideline (빨간색) |
| **디바운스** | 중복 입력 방지 | 50-150ms | 모든 버튼/제스처 | 모든 Interactive 컴포넌트 |

#### 6.2.6 용어 사용 원칙

**Android 플랫폼 용어 사용 규칙**:
- **연결 상태**: `TransportState` (USB 연결 단계, NoTransport → UsbOpening → UsbReady)
- **앱 상태**: `AppState` (앱 화면 표시 및 기능 제한 결정)

**Android 플랫폼 금칙어 및 대체 표현**:
- ❌ "연결됨/연결안됨" → ✅ `TransportState` (USB 연결 상태)
- ❌ "앱 상태" → ✅ `AppState` (앱 화면 및 기능 상태)

**용어 정의 우선순위**:
1. **플랫폼 공통 용어**: 6.1 플랫폼 공통 용어집의 용어 우선 사용
2. **Android 플랫폼 용어**: Android 특화 용어는 6.2 Android 용어집에서 정의
3. **상태 구분 명확성**: 상태의 성격에 맞는 적절한 용어 선택
4. **사용자 이해도**: 직관적이고 명확한 용어 선택

**참조 문서**:
- Android 앱 기술 명세: `technical-specification-app.md`
- 컴포넌트 디자인 가이드: `component-design-guide-app.md`
- 터치패드 컴포넌트 명세: `component-touchpad.md`
- 디자인 가이드: `design-guide-app.md`

#### 6.2.7 추가 카테고리별 용어

**색상 시스템 용어**:
- **Primary 색상**: `#2196F3` (주요 액션, Selected 상태)
- **Success 색상**: `#4CAF50` (성공 상태, 완료 알림)
- **Error 색상**: `#F44336` (오류 상태, 실패 알림)
- **Warning 색상**: `#FF9800` (경고 상태, 진행 중 알림)

**애니메이션 및 타이밍 용어**:
- **Toast 등장/사라짐**: 350ms/300ms (등장/사라짐 지속시간)
- **버튼 스케일**: 200ms (터치 피드백 애니메이션)
- **모드 전환**: 250ms (상태 변경 트랜지션)

### 6.4 Windows 플랫폼 용어집

#### 6.4.1 상태 및 모드 관련 용어

| 용어 | 설명 | 상태값 | 사용 맥락 |
|------|------|--------|----------|
| **ServiceState** | Windows 서비스 상태 | `Stopped`, `Starting`, `Running`, `Stopping`, `Error` | 서비스 라이프사이클 관리 |
| **ConnectionState** | 연결 상태 | `Disconnected`, `Connecting`, `Connected`, `Reconnecting`, `Error` | Android 앱과의 연결 상태 |
| **ConnectionMode** | 서버-ESP32 연결 모드 | `Standard` (핸드셰이크 완료), `Disconnected` (연결 없음) | 서버 UI 상태 및 기능 활성화 범위 결정. Essential 모드는 서버 미실행 상태이므로 서버 측에는 해당 없음 |

#### 6.4.2 통신 프로토콜 관련 용어

| 용어 | 설명 | 프로토콜 | 사용 모드 |
|------|------|----------|----------|
| **핸드셰이크** | 연결 확립 및 상태 동기화 과정 | TLS 1.3 기반 보안 연결 | Android 앱과 Windows 서버 간 연결 |
| **HID Boot Mouse** | BIOS/UEFI 호환 마우스 프로토콜 | 표준 HID 마우스 입력 | Essential/Standard 모드 |
| **HID Boot Keyboard** | BIOS/UEFI 호환 키보드 프로토콜 | 표준 HID 키보드 입력 | Essential/Standard 모드 |

#### 6.4.3 하드웨어 및 구성 요소 관련 용어

| 용어 | 설명 | 사양 | 역할 |
|------|------|------|------|
| **Windows Service 아키텍처** | 백그라운드 서비스 실행 방식 | .NET 6+ 기반 | Windows 통합 및 백그라운드 실행 |
| **USB HID/CDC 통신** | USB 복합 장치 통신 | ESP32-S3 동글 | Android 앱과의 데이터 교환 |
| **시스템 레벨 입력 시뮬레이션** | Windows API 기반 입력 처리 | SendInput API | 키보드/마우스 입력 생성 |
| **성능 모니터링 시스템** | 실시간 시스템 지표 추적 | CPU/메모리/네트워크 사용량 | 성능 최적화 및 문제 진단 |

#### 6.4.4 UI 컴포넌트 관련 용어

| 용어 | 설명 | 구현 방식 | 주요 기능 |
|------|------|----------|----------|
| **Mica** | Windows 11 반투명 배경 효과 | Acrylic 표면 | 모던한 배경 시각 효과 |
| **Acrylic** | 블러 효과가 있는 반투명 재질 | 배경 블러 | 깊이감 있는 UI 표현 |
| **테마 감지** | 시스템 테마 자동 감지 | Dark/Light 모드 | 자동 테마 적용 |
| **심볼 아이콘** | Fluent System Icons 기반 | 벡터 아이콘 | 일관된 시각적 언어 |
| **네비게이션 뷰** | 앱 내 네비게이션 컨테이너 | 탭 기반 레이아웃 | 페이지 전환 및 탐색 |
| **플루언트 윈도우** | Fluent Design 스타일 윈도우 | Mica 배경 효과 | Windows 11 네이티브 느낌 |

#### 6.4.5 시스템 통합 관련 용어

| 용어 | 설명 | 구현 방식 | 목적 |
|------|------|----------|------|
| **시스템 트레이 관리** | 백그라운드 상태 표시 | 트레이 아이콘 및 컨텍스트 메뉴 | 빠른 접근 및 상태 모니터링 |
| **자동 시작** | Windows 부팅 시 자동 실행 | 서비스 등록 | 사용자 편의성 향상 |
| **보안 정책 적용** | EDR/방화벽 환경 적응 | 권한 관리 | 기업 환경 호환성 |
| **실시간 모니터링** | 성능 지표 실시간 추적 | 백그라운드 스레드 | 시스템 상태 파악 |

#### 6.4.6 설정 및 구성 관련 용어

| 용어 | 설명 | 설정 범위 | 적용 대상 |
|------|------|----------|----------|
| **연결 모니터링** | 연결 상태 폴링 주기 | 1-30초 | 연결 품질 추적 |
| **자동 복구** | 연결 실패 시 자동 재연결 | 활성화/비활성화 | 안정성 향상 |
| **성능 최적화 모드** | 시스템 리소스 최적화 | 저지연/배치 처리 | 성능 조정 |
| **로그 레벨** | 로깅 상세도 | 오류/경고/정보/디버그 | 문제 진단 |

#### 6.4.7 고급 기능 관련 용어

| 용어 | 설명 | 활성화 조건 | 기능 범위 |
|------|------|----------|----------|
| **멀티 커서 기능** | 다중 커서 시뮬레이션 | Standard 모드 | 생산성 향상 |
| **매크로 편집기** | 사용자 정의 작업 시퀀스 | Standard 모드 | 자동화 작업 |
| **커서 팩 관리** | 커스텀 커서 테마 | Standard 모드 | 개인화 |
| **가상 커서 표시** | 커서 상태 시각적 표현 | Standard 모드 | 멀티태스킹 지원 |

#### 6.4.8 용어 사용 원칙

**Windows 플랫폼 용어 사용 규칙**:
- **서비스 상태**: `ServiceState` (서비스 라이프사이클, Stopped → Running)
- **연결 상태**: `ConnectionState` (Android 앱과의 연결 상태)
- **앱 모드**: `AppMode` (기능 활성화 범위 결정)

**통신 용어 사용 규칙**:
- **연결 과정**: `핸드셰이크` (연결 확립 및 상태 동기화)

**Windows 플랫폼 금칙어 및 대체 표현**:
- ❌ "서비스 상태" → ✅ `ServiceState` (서비스 라이프사이클)
- ❌ "연결 상태" → ✅ `ConnectionState` (Android 앱 연결 상태)
- ❌ "앱 모드" → ✅ `AppMode` (기능 활성화 범위)

**용어 정의 우선순위**:
1. **플랫폼 공통 용어**: 6.1 플랫폼 공통 용어집의 용어 우선 사용
2. **Windows 플랫폼 용어**: Windows 특화 용어는 6.4 Windows 용어집에서 정의
3. **플랫폼 일관성**: Windows와 Android 간 용어 일관성 유지
4. **기술적 정확성**: 각 플랫폼의 기술적 맥락에 맞는 용어 사용

**참조 문서**:
- Windows 서버 기술 명세: `technical-specification-server.md`
- 디자인 가이드: `design-guide-server.md`
- 스타일프레임: `styleframe-server.md`
