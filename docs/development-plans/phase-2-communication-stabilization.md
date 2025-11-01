---
title: "BridgeOne Phase 2: 통신 안정화"
description: "BridgeOne 프로젝트 Phase 2 - Android ↔ Board ↔ Windows 간 HID/CDC 통신 완전 안정화 (5-6주)"
tags: ["android", "esp32-s3", "devkitc-1", "windows", "communication", "hid", "cdc", "uart", "vibe-coding"]
version: "v1.0"
owner: "Chatterbones"
updated: "2025-10-27"
---

# BridgeOne Phase 2: 통신 안정화

**개발 기간**: 5-6주

**목표**: Android ↔ Board ↔ Windows 간 HID/CDC 통신 완전 안정화

**검증 전략**: 각 Phase별 개별 검증 + 최종 통합 검증

**핵심 목표**:
- **기능 목표**: 
  - Phase 2 전체: HID 키보드/마우스 입력 + Vendor CDC JSON 쌍방향 통신 완벽 검증
  - Phase 2.1: HID Boot Keyboard + HID Boot Mouse 통신 경로 완전 구축
  - Phase 2.2: Vendor CDC JSON 양방향 통신 경로 완전 구축
- **디바이스 인식 검증**: `Get-PnpDevice | Where-Object {$_.DeviceID -match "VID_303A"} | Format-Table FriendlyName, Status, DeviceID` PowerShell 명령어 실행 시 모든 ESP32-S3 디바이스 Status가 "OK"로 표시됨
- **성능 목표**: 50ms 이하 지연시간, 0.1% 이하 프레임 손실률, 1000Hz 폴링 레이트 달성
- **안정성 목표**: 4시간 연속 사용 무중단, 자동 재연결, USB 플러그 앤 플레이 지원
- **호환성 목표**: Windows 10/11, Android 8.0+ 크로스 플랫폼 지원, BIOS/UEFI 부트 호환성
- **사용성 목표**: USB 연결 즉시 자동 디바이스 인식, HID 기본 드라이버 활용으로 별도 드라이버 설치 불필요

## Phase 구조 설계 원칙

### 검증 전략

- **기본 원칙**: 각 하위 Phase 개발 완료 즉시 해당 내용을 바로 검증
- **예외**: 여러 하위 Phase를 모아서 통합 검증하는 게 더 효율적인 경우에만 별도 통합 검증 Phase 추가
  - 예: Phase 2.5 - 전체 통신 경로 End-to-End 지연시간 검증
  - 예: Phase 4.5 - PC 화면에서 실제 커서 이동 정확성 통합 테스트
- 최종 Phase에서 전체 시스템 E2E 테스트 수행

### Phase 명명 규칙

- **Phase X.Y**: 구현 및 검증 하위 Phase (구현 완료 즉시 검증)
- **Phase X.통합검증**: 통합 검증이 필요한 경우에만 추가 (예외적)

### 바이브 코딩 활용 방침

- 각 하위 Phase별 바이브 코딩 프롬프트는 별도 섹션에서 제공 예정
- 본 문서는 전체 개발 로드맵의 큰 틀을 제시

---

## Phase 2.1: HID 통신 개발

**개발 기간**: 2주

**목표**: ESP32-S3 USB Composite 디바이스 구현 및 Android ↔ ESP32-S3 ↔ Windows HID 통신 경로 완전 구축

**핵심 성과물**:
- ESP32-S3 TinyUSB 기반 HID Boot Mouse + HID Boot Keyboard 복합 디바이스
- Android 앱 8바이트 델타 프레임 생성 및 UART 전송
- Windows HID 기본 드라이버 자동 인식 및 입력 처리
- End-to-End 통신 경로 검증 (50ms 이하 지연시간, 0.1% 이하 손실률)

---

### Phase 2.1.1: ESP32-S3 USB Composite 디스크립터 구현

**목표**: TinyUSB 기반 HID Boot Mouse + HID Boot Keyboard 복합 디바이스 구성

**개발 기간**: 3-4일

**세부 목표**:
1. `usb_descriptors.c/.h` 파일 생성
2. USB Configuration Descriptor 작성 (Interface 0: Keyboard, Interface 1: Mouse)
3. HID Boot Keyboard Report Descriptor 구현 (8바이트)
4. HID Boot Mouse Report Descriptor 구현 (4바이트)
5. TinyUSB 콜백 함수 구현 (`tud_hid_get_report_cb`, `tud_hid_set_report_cb`)
6. Endpoint 번호 할당 및 VID/PID 설정

**참조 문서 및 섹션**:
- `docs/board/esp32s3-code-implementation-guide.md` §1.3 USB Composite 디바이스 설계 계약
- `docs/board/esp32s3-code-implementation-guide.md` §3.3 TinyUSB Composite 디바이스 구현
- `docs/technical-specification.md` §2.3.1 ESP32-S3-DevkitC-1-N16R8
- `.cursor/rules/tinyusb-descriptors.mdc` - TinyUSB 복합 디바이스 USB 디스크립터 구현 가이드

---

#### Phase 2.1.1.1: USB Device & Configuration Descriptor 정의

**목표**: VID/PID 설정 및 기본 USB Device/Configuration Descriptor 구성

**세부 목표**:
1. `usb_descriptors.h` 헤더 파일 생성
2. VID/PID 상수 정의 (VID: 0x303A 또는 0x1209)
3. USB Device Descriptor 정의
4. USB Configuration Descriptor 정의 (Interface 0: Keyboard, Interface 1: Mouse)
   - **주의**: CDC 인터페이스는 제외 (Phase 2.2에서 추가 예정)
   - HID Descriptor (9 bytes)를 Configuration Descriptor에 포함 (USB HID 인터페이스 완전 정의에 필수)
   - Configuration Descriptor 총 길이: 50 bytes (2개 interface + 2개 endpoint)
5. String Descriptor 정의 (Manufacturer, Product, Serial Number)
   - 향후 CDC 추가 시 재사용 가능하도록 미리 정의
6. `usb_descriptors.c` 파일 생성 및 기본 구조 작성

**참조 문서 및 섹션**:
- `docs/board/esp32s3-code-implementation-guide.md` §1.3 USB Composite 디바이스 설계 계약
- `docs/board/esp32s3-code-implementation-guide.md` §3.3.1 ESP-IDF 프로젝트 설정
- `docs/board/esp32s3-code-implementation-guide.md` §1.3.3 BridgeOne UART 프로토콜 (고정)
- TinyUSB 공식 문서: Device Descriptor, Configuration Descriptor 명세
- `.cursor/rules/tinyusb-descriptors.mdc` - TinyUSB 복합 디바이스 USB 디스크립터 구현 가이드

**검증**:
- [x] `src/board/BridgeOne/main/usb_descriptors.h` 파일 생성됨
- [x] `src/board/BridgeOne/main/usb_descriptors.c` 파일 생성됨
- [x] VID=0x303A (또는 0x1209) 정의됨
- [x] USB Device Descriptor 배열 정의됨 (`tusb_desc_device[]`)
- [x] USB Configuration Descriptor에 2개 Interface 정의됨 (HID Keyboard + Mouse)
- [x] **HID Descriptor (9 bytes)가 Configuration Descriptor에 포함됨**
- [x] **CDC 인터페이스는 제외됨 (Phase 2.2에서 추가 예정)**
- [x] **Configuration Descriptor 총 길이: 50 bytes 확인**
- [x] String Descriptor 정의됨 (3개 이상: Language, Manufacturer, Product, Serial)
- [x] Endpoint 번호 예약됨 (EPNUM_HID_KB=0x81, EPNUM_HID_MOUSE=0x82)
- [x] `idf.py build` 성공 (CMakeLists.txt 업데이트하여 컴파일 가능 상태)

**개발 과정 변경 사항 (Changes)**:

#### 1. HID Report Descriptor Typedef 제거
**계획**: `usb_descriptors.h`에서 `hid_keyboard_report_t`, `hid_mouse_report_t` typedef 정의
**변경**: 해당 typedef 제거 (참고용 주석만 유지)
**이유**: TinyUSB 헤더파일(`hid.h`)에서 이미 정의되어 있어 중복 정의로 인한 컴파일 오류 발생. 타입 충돌 방지 위해 외부 정의 사용.

#### 2. USB Descriptor 타입 선언 변경
**계획**: `desc_device`를 `uint8_t[]` 배열로 선언
**변경**: `tusb_desc_device_t` 구조체 타입으로 선언
**이유**: 
- 타입 안정성 강화 (구조체 정의로 정확한 메모리 레이아웃 명시)
- 콜백 함수 시그니처와 일치 (타입 캐스팅 시 안전성)
- 빌드 시 타입 충돌 오류 해결

#### 3. RHPORT 상수값 하드코딩
**계획**: `BOARD_TUD_RHPORT` 매크로 상수 사용
**변경**: USB 초기화 시 직접 `0`으로 지정
**이유**: ESP32-S3는 USB OTG 포트가 하나(포트 0)만 지원되므로 정적 값 사용 가능. `BOARD_TUD_RHPORT`는 ESP-IDF 프레임워크에서 미제공 상수로 정의되지 않음.

#### 4. TinyUSB 컴포넌트 의존성 추가
**계획**: TinyUSB가 자동으로 `tusb_config.h`를 찾을 것으로 예상
**변경**: `managed_components/espressif__tinyusb/CMakeLists.txt`에서 `main` 컴포넌트를 `PRIV_REQUIRES`에 추가
**이유**: TinyUSB 스택의 `tusb_option.h`가 `tusb_config.h`를 인클루드하는데, 이 파일이 `main` 컴포넌트에 위치하므로 명시적 의존성 필요. 빌드 시스템 링크 오류 해결.

#### 5. HID 콜백 함수 구현 시점 조정
**계획**: Phase 2.1.1.1에서 `tud_hid_get_report_cb`, `tud_hid_set_report_cb` 완전 구현
**변경**: 스켈레톤 구현만 제공하고 완전 구현은 Phase 2.1.1.2로 연기
**이유**: 
- Phase 구조의 명확화: 각 Phase는 단일 책임 원칙 준수
- Phase 2.1.1.1: USB 디스크립터 및 스택 초기화 (현재 단계)
- Phase 2.1.1.2: HID Report Descriptor 및 콜백 함수 완전 구현 (다음 단계)
- 빌드 완성도 우선 (컴파일 가능 상태 달성)

#### 6. tusb_config.h 추가 생성
**계획**: TinyUSB 설정이 자동으로 적용될 것으로 예상
**변경**: `main/tusb_config.h` 파일 명시적 생성
**이유**: TinyUSB는 `tusb_option.h`에서 `tusb_config.h` 포함을 요구하는데, 프로젝트별 커스텀 설정이 필요. HID, CDC 클래스 활성화, 버퍼 크기, FreeRTOS 통합 등 ESP-IDF 환경 맞춤 설정 필수.

**요약**: 기존 계획의 이상적인 구조를 유지하면서도 TinyUSB의 실제 요구사항과 ESP-IDF 빌드 시스템의 제약을 반영하여 실질적인 구현 완성을 우선시함.

---

#### Phase 2.1.1.2: HID Report Descriptor (Keyboard + Mouse)

**목표**: HID Boot Keyboard(8바이트)와 HID Boot Mouse(4바이트) Report Descriptor 정의

**세부 목표**:
1. HID Boot Keyboard Report Descriptor 구현 (8바이트 구조)
   - Modifier keys (1바이트)
   - Reserved (1바이트)
   - Key codes array (6바이트)
2. HID Boot Mouse Report Descriptor 구현 (4바이트 구조)
   - Button bits (1바이트)
   - X axis (1바이트)
   - Y axis (1바이트)
   - Wheel (1바이트)
3. Report Descriptor 크기 및 배치 검증
   - **주의**: 표준 호환성을 위해 USB HID Specification v1.1.1의 모든 필수 Item을 포함해야 함
   - 실제 측정 크기: Keyboard 65바이트, Mouse 74바이트
4. Configuration Descriptor의 `wDescriptorLength` 필드를 실제 크기로 설정
   - Keyboard: `0x41` (65), Mouse: `0x4A` (74)

**참조 문서 및 섹션**:
- `docs/board/esp32s3-code-implementation-guide.md` §3.3 TinyUSB Composite 디바이스 구현
- HID Specification: Boot Keyboard Report Descriptor, Boot Mouse Report Descriptor
- TinyUSB 예제: `examples/device/hid_composite`
- `.cursor/rules/tinyusb-descriptors.mdc` - USB 디스크립터 구현 가이드
- `.cursor/rules/tinyusb-hid-implementation.mdc` - HID 키보드/마우스 구현 패턴 가이드

**⚠️ Phase 2.1.1.1에서의 변경사항 영향**:
- **Typedef 제거**: Phase 2.1.2.3에서 HID 리포트 전송 시 TinyUSB의 공식 타입(`hid_keyboard_report_t`, `hid_mouse_report_t`)을 직접 사용해야 함 (중복 정의 방지)
- **tusb_desc_device_t 타입 변경**: Phase 2.1.1.1에서 `usb_descriptors.c`의 디바이스 디스크립터 선언이 이미 `tusb_desc_device_t` 구조체로 정의됨. 이 변경은 콜백 함수의 타입 안정성을 높임
- **tusb_config.h 명시적 생성**: Phase 2.1.2.2 UART 수신 시 TinyUSB 설정이 올바르게 적용되려면 `tusb_config.h`의 버퍼 크기, FreeRTOS 통합 설정이 필수

**검증**:
- [x] HID Report Descriptor for Keyboard 정의됨 (8바이트 구조)
- [x] HID Report Descriptor for Mouse 정의됨 (4바이트 구조)
- [x] `tusb_desc_hid_report_keyboard[]` 배열 정의됨
- [x] `tusb_desc_hid_report_mouse[]` 배열 정의됨
- [x] 각 Report Descriptor 크기 정확함 (Keyboard: 65바이트, Mouse: 74바이트 측정)
- [x] **Configuration Descriptor의 `wDescriptorLength` 필드가 실제 크기로 설정됨 (Keyboard: 0x41, Mouse: 0x4A)**
- [x] Report ID 없음 (Boot mode: Report ID 제외)
- [x] `idf.py build` 성공
- [x] Keyboard Report Descriptor에 Modifier, Reserved, Key Codes 필드 포함 확인
- [x] Mouse Report Descriptor에 Buttons, X/Y, Wheel 필드 포함 확인

**개발 과정 변경 사항 (Changes - Phase 2.1.1.2)**:

1. **Report ID 제거 (Boot Protocol 준수)**
   - 계획: `HID_REPORT_ID(1)`, `HID_REPORT_ID(2)` 포함
   - 변경: `TUD_HID_REPORT_DESC_KEYBOARD()`, `TUD_HID_REPORT_DESC_MOUSE()` (Report ID 제외)
   - 이유: 요구사항 "Report ID 없음 (Boot mode)" 준수 및 BIOS/UEFI 호환성

2. **tusb_config.h 심볼 추가**
   - 계획: tusb_config.h 생성만으로 충분
   - 변경: `CFG_TUSB_RHPORT0_MODE OPT_MODE_DEVICE` 명시적 정의
   - 이유: TinyUSB 정적 검증 오류 해결 (_Static_assert 빌드 실패)

3. **Report Descriptor 크기 자동 계산**
   - 계획: `0x41`, `0x4A` 하드코딩
   - 변경: `sizeof(desc_hid_keyboard_report)`, `sizeof(desc_hid_mouse_report)` 사용
   - 이유: 유지보수성 강화 및 타입 안정성

**후속 Phase 영향 분석**:

- **Phase 2.1.1.3 (콜백 함수)**: 영향 없음. Report ID 제거는 `tud_hid_get_report_cb()`, `tud_hid_set_report_cb()` 로직에 영향을 주지 않음 (Instance 기반 구분)
- **Phase 2.1.2.3 (HID 리포트 전송)**: **영향 있음** - Report ID를 포함한 `tud_hid_n_report()` 호출 시 Report ID 파라미터를 0으로 설정하거나 보내지 않아야 함 (Boot Protocol에서 Report ID 불필요)
- **Phase 2.2 (CDC 구현)**: 영향 없음. HID와 독립적인 인터페이스

---

#### Phase 2.1.1.3: TinyUSB 콜백 함수 구현

**목표**: HID Keyboard/Mouse 인스턴스 구분 및 TinyUSB 콜백 함수 구현

**세부 목표**:
1. `tud_hid_get_report_cb()` 함수 구현
   - HID Instance 구분 (Instance 0: Keyboard, Instance 1: Mouse)
   - 각 인스턴스에 대한 리포트 전송 준비
2. `tud_hid_set_report_cb()` 함수 구현
   - Keyboard LED 상태 처리 (Caps Lock, Num Lock, Scroll Lock)
3. `tud_descriptor_string_cb()` 콜백 구현
   - **주의**: TinyUSB v0.19.0~1 요구사항에 따라 반환 타입을 `const uint16_t*`로 사용
4. HID 리포트 메모리 할당 및 초기화
5. Endpoint 핸들링 기본 구조 작성
6. Helper 함수 추가 구현
   - `hid_update_report_state()`: UART 수신 후 리포트 상태 업데이트용
   - `hid_get_keyboard_led_status()`: Keyboard LED 상태 조회용
7. **주의사항**:
   - `class/hid/hid.h` 헤더 파일을 include해야 `HID_REPORT_TYPE_*` 매크로 사용 가능
   - `espressif/esp_tinyusb` managed component 사용 시 Kconfig 심볼은 `CONFIG_TINYUSB_*` prefix를 사용
   - Kconfig 설정 파일 (`sdkconfig.defaults`)은 크로스 플랫폼 호환성을 위해 영문 주석만 사용 (UTF-8)

**⚠️ Phase 2.1.1.1에서의 변경사항 영향**:
- **콜백 함수 시그니처 일관성**: 디바이스 디스크립터 타입이 `tusb_desc_device_t`로 변경되었으므로, 이 콜백 함수들의 반환 타입도 TinyUSB 공식 타입과 완전히 일치해야 함 (타입 캐스팅 오류 방지)
- **스켈레톤 구현**: Phase 2.1.1.1의 변경사항에 따라 이 Phase는 **스켈레톤 구현만 제공**. 완전한 구현은 Phase 2.1.1.2로 연기됨 (HID Report Descriptor 정의 후 완성)

**참조 문서 및 섹션**:
- TinyUSB 문서: `tud_hid_get_report_cb()`, `tud_hid_set_report_cb()` 구현
- `docs/board/esp32s3-code-implementation-guide.md` §3.3 TinyUSB Composite 디바이스 구현
- `.cursor/rules/tinyusb-hid-implementation.mdc` - HID 키보드/마우스 구현 패턴 및 콜백 처리 가이드

**검증**:
- [x] `tud_hid_get_report_cb()` 함수 구현됨
- [x] `tud_hid_set_report_cb()` 함수 구현됨
- [x] **`tud_descriptor_string_cb()` 콜백 반환 타입이 `const uint16_t*`로 구현됨**
- [x] Instance 0 (Keyboard)에 대한 분기 처리 확인
- [x] Instance 1 (Mouse)에 대한 분기 처리 확인
- [x] LED 상태 버퍼 선언됨 (`hid_keyboard_led_status`)
- [x] 함수 서명과 TinyUSB 요구사항 일치
- [x] **Helper 함수 선언됨 (`hid_update_report_state()`, `hid_get_keyboard_led_status()`)**
- [x] **`class/hid/hid.h` 헤더 파일 include 확인**
- [x] `idf.py build` 성공 (컴파일 오류 없음)

**🔄 변경사항 분석 및 이유**:

**1. HID 리포트 구조체 재정의 방지**
   - **원래 계획**: `hid_handler.h`에서 `hid_keyboard_report_t`, `hid_mouse_report_t` 새로 정의
   - **실제 구현**: TinyUSB 헤더(`class/hid/hid.h`)에서 이미 정의된 구조체 사용
   - **변경 이유**: 중복 정의 시 컴파일 오류 발생. TinyUSB에서 제공하는 타입을 재사용하는 것이 모범 사례
   - **영향**: 빌드 성공, 타입 충돌 제거, 향후 유지보수 용이

**2. 구조체 필드명 수정 (중요)**
   - **원래 계획**: `modifiers`, `keyCodes`, `deltaX`, `deltaY` 사용
   - **실제 TinyUSB**: `modifier` (단수), `keycode` (단수), `x`, `y` 사용
   - **변경 이유**: TinyUSB 공식 헤더(`hid.h` 라인 360, 301)의 정의와 일치 필요
   - **영향**: Phase 2.1.2의 `bridge_frame_t` 구조체 필드명 수정 필수
   - **참고**: TinyUSB 공식 문서 미흡으로 인해 초기 계획과 실제 구현 불일치

**3. 콜백 함수 구현 위치 변경**
   - **원래 계획**: `usb_descriptors.c`에서 스켈레톤 구현 후 완전 구현은 Phase 2.1.1.2
   - **실제 구현**: `hid_handler.c`에서 완전 구현 (스켈레톤이 아님)
   - **변경 이유**: 
     - 관심사의 분리 원칙 (Separation of Concerns): USB 디스크립터 로직과 HID 핸들러 로직 분리
     - 테스트 용이성: hid_handler 단위 테스트 가능
     - 코드 재사용성: 다른 프로젝트에서 hid_handler.c 재사용 가능
   - **영향**: 유지보수성 향상, 모듈화 강화

**4. Helper 함수 스켈레톤 제공**
   - **상태**: `hid_update_report_state()`, `hid_get_keyboard_led_status()` 스켈레톤 제공
   - **이유**: Phase 2.1.2에서 UART 처리 후 완전 구현 예정
   - **영향**: Phase 2.1.2 진행 시 함수 시그니처 변경 불필요

---

#### Phase 2.1.2: ESP32-S3 UART → HID 변환 로직 구현

**목표**: UART에서 수신한 8바이트 프레임을 HID 리포트로 변환하여 전송

**개발 기간**: 4-5일

**세부 목표**:
1. `uart_handler.c/.h` 파일 생성
2. UART 초기화 함수 구현 (1Mbps, 8N1)
3. `bridge_frame_t` 구조체 정의 (8바이트)
4. UART 수신 태스크 구현
5. 프레임 검증 로직 구현 (순번 카운터, 크기 검증)
6. `hid_handler.c/.h` 파일 생성
7. HID 태스크 구현 (프레임 큐 수신 → HID 리포트 전송)
8. `processBridgeFrame()` 함수 구현 (Keyboard + Mouse 분리 처리)

**참조 문서 및 섹션**:
- `docs/board/esp32s3-code-implementation-guide.md` §4.1 UART 통신 모듈 구현 (ESP-IDF)
- `docs/board/esp32s3-code-implementation-guide.md` §4.2 USB HID 모듈 구현 (ESP-IDF TinyUSB)
- `docs/board/esp32s3-code-implementation-guide.md` §3.4 BridgeOne 프레임 처리 로직
- `docs/technical-specification.md` §2.1 UART 통신 (Android ↔ ESP32-S3)
- `.cursor/rules/tinyusb-architecture.mdc` - TinyUSB 아키텍처 및 컴포넌트 구조 가이드
- `.cursor/rules/tinyusb-freertos-integration.mdc` - TinyUSB와 FreeRTOS 통합 패턴 가이드

---

#### Phase 2.1.2.1: UART 초기화 및 bridge_frame_t 구조체 정의

**목표**: UART 통신 채널 설정 및 BridgeOne 프로토콜 데이터 구조 정의

**세부 목표**:
1. `uart_handler.h` 헤더 파일 작성
2. `bridge_frame_t` 구조체 정의 (8바이트)
   - seq: 1바이트 (시퀀스 번호)
   - buttons: 1바이트 (마우스 버튼 비트)
   - x: 1바이트 (X축 이동값, signed)
   - y: 1바이트 (Y축 이동값, signed)
   - wheel: 1바이트 (휠 값, signed)
   - modifier: 1바이트 (키보드 modifier 키)
   - keycode1: 1바이트 (첫 번째 키코드)
   - keycode2: 1바이트 (두 번째 키코드)
3. UART 상수 정의 (UART_NUM_0, 1Mbps, 8N1)
4. `uart_handler.c` 파일 작성
5. UART 초기화 함수 구현 (`uart_init()`)
   - **주의**: ESP32-S3-DevkitC-1은 내장 USB-to-UART 브릿지(U0TXD: GPIO43, U0RXD: GPIO44)를 사용하므로 `uart_set_pin()` 또는 `gpio_set_direction()` 호출 불필요

**참조 문서 및 섹션**:
- `docs/technical-specification.md` §2.1 UART 통신 (Android ↔ ESP32-S3)
- `docs/board/esp32s3-code-implementation-guide.md` §1.3.3 BridgeOne UART 프로토콜 (고정)
- ESP-IDF 문서: UART Driver

**검증**:
- [x] `src/board/BridgeOne/main/uart_handler.h` 파일 생성됨
- [x] `src/board/BridgeOne/main/uart_handler.c` 파일 생성됨
- [x] `bridge_frame_t` 구조체 정의됨 (정확히 8바이트)
- [x] 모든 필드 타입 정확함 (seq, buttons, x/y/wheel, modifier, keycode1/2)
- [x] UART 설정 상수 정의됨 (UART_NUM_0, BAUDRATE=1000000)
- [x] `uart_init()` 함수 구현됨
  - [x] **`gpio_set_direction()` 또는 `uart_set_pin()` 호출 없음 (내장 USB-to-UART 사용)**
  - [x] `uart_param_config()` 호출 (1Mbps, 8N1)
  - [x] `uart_driver_install()` 호출 (버퍼 크기 할당)
- [x] `idf.py build` 성공

**⚠️ Phase 2.1.1.1에서의 변경사항 영향**:
- **tusb_config.h 명시적 생성 필수**: Phase 2.1.1.1에서 `main/tusb_config.h` 파일이 명시적으로 생성되어야 함. 이 파일에는 다음이 포함되어야 함:
  - `CFG_TUSB_RHPORT0_MODE = OPT_MODE_DEVICE` (Device 모드)
  - `CFG_TUD_HID = 2` (HID 2개 인스턴스: Keyboard, Mouse)
  - `CFG_TUD_HID_EP_BUFSIZE = 64` (HID 리포트 버퍼)
  - `CFG_TUSB_OS = OPT_OS_FREERTOS` (FreeRTOS 통합)
  - 기타 UART, CDC 설정 (선택적)
- **TinyUSB 의존성 추가**: Phase 2.1.1.1에서 `managed_components/espressif__tinyusb/CMakeLists.txt`에 `main` 컴포넌트가 `PRIV_REQUIRES`에 추가되어야 함
- **BridgeFrame 구조체의 의존성**: UART 수신 태스크(Phase 2.1.2.2)에서 이 구조체를 사용하므로, 정확한 8바이트 크기 정의 필수

**⚠️ Phase 2.1.1.3 변경사항 영향**:
- **구조체 필드명 수정 필수**: Phase 2.1.1.3에서 HID 콜백 함수 구현 시 TinyUSB 실제 필드명(`modifier`, `keycode`, `x`, `y`)이 발견되었음
  - `bridge_frame_t` 필드: `modifiers` → `modifier`, `keyCodes` → `keycode1`/`keycode2`, `deltaX` → `x`, `deltaY` → `y`
  - 영향: Phase 2.1.2.3의 `processBridgeFrame()` 함수에서 필드명 매핑 시 실제 필드명 사용 필수
- **주의**: bridge_frame_t는 UART 프로토콜 정의이므로 필드 순서와 크기 변경 불가. 필드명만 수정

**📝 구현 변경사항 및 이유**:

본 Phase에서 기존 개발 계획과 다르게 구현된 부분을 기록합니다.

**1. BridgeOne.c에서 UART 초기화 호출 (새로 추가)**
- **계획**: 개발 계획 문서에서는 UART 초기화 시점이 명시되지 않음
- **실제 구현**: `app_main()`에서 TinyUSB 초기화(tud_init) 직후 UART 초기화 실행
- **변경 이유**:
  - UART는 Android와의 통신을 담당하는 핵심 채널이므로 시스템 부팅 초기에 초기화 필수
  - TinyUSB(USB)와 UART는 독립적인 통신 채널이므로 순서는 중요하지 않지만, 명시적 초기화가 필요
  - Phase 2.1.2.2에서 uart_task()가 UART에 접근할 때 이미 초기화된 상태 필요
  - 초기화 오류를 app_main() 레벨에서 감지하여 시스템 부팅 실패 가능

**2. CMakeLists.txt 의존성 명시적 추가**
- **계획**: TinyUSB와 main 컴포넌트 간 의존성은 자동으로 해결될 것으로 가정했음
- **실제 변경사항**:
  1. `src/board/BridgeOne/main/CMakeLists.txt`에 `uart_handler.c` 명시적 추가
  2. `managed_components/espressif__tinyusb/CMakeLists.txt`의 `PRIV_REQUIRES`에 `main` 추가
- **변경 이유**:
  - TinyUSB의 `tusb_option.h`가 `tusb_config.h`를 #include하는데, 이 파일은 main 컴포넌트에 위치
  - ESP-IDF의 컴포넌트 시스템은 이러한 의존성을 명시적으로 선언해야 컴파일 시 올바른 인클루드 경로 설정
  - 명시적 선언 없으면 "tusb_config.h: No such file or directory" 컴파일 오류 발생
  - uart_handler.c도 CMakeLists.txt SRCS에 등록하지 않으면 컴파일되지 않음

**⚠️ 후속 Phase에 미치는 영향 분석 및 수정**:

**Phase 2.1.2.2 (UART 수신 태스크)에 미치는 영향**:
- UART는 app_main()에서 이미 초기화되므로, uart_task()에서 추가 초기화 불필요
  - uart_task()는 즉시 uart_read_bytes() 호출 가능
  - uart_init()에 의존하는 코드 재작성 불필요
- frame_queue는 uart_task() 또는 app_main()에서 별도로 생성/관리 필요
  - uart_init()에서는 UART 드라이버 설정만 수행하고 큐는 생성 안 함
- uart_handler.h의 extern 선언 사항 유지 필요

**Phase 2.1.2.3 (HID 태스크)에 미치는 영향**:
- frame_queue를 통한 BridgeFrame 수신 구조는 변경 없음
- processBridgeFrame()의 필드명 매핑은 bridge_frame_t의 필드명 사용 필수
  - `bridge_frame_t.modifier` (계획 문서의 modifiers 아님)
  - `bridge_frame_t.keycode1`, `bridge_frame_t.keycode2` (계획 문서의 keyCodes 아님)

**Phase 2.1.3 이후 (리팩토링)에 미치는 영향**:
- CMakeLists.txt 의존성 설정 패턴이 확립되었으므로, 향후 새로운 컴포넌트/모듈 추가 시 동일한 패턴 적용
  - 컴포넌트 간 의존성은 명시적으로 CMakeLists.txt에 선언
  - PRIV_REQUIRES vs REQUIRES 구분: tusb_config.h는 private이므로 PRIV_REQUIRES 사용
- UART 초기화 순서(app_main → uart_init → usb_task 생성)는 변경하지 말 것
  - Phase 3 이상에서 새로운 초기화 단계 추가 시 현재 순서 유지

---

#### Phase 2.1.2.2: UART 수신 태스크 및 프레임 검증 로직

**목표**: UART 수신 태스크 구현 및 프레임 검증 로직 작성

**세부 목표**:
1. UART 수신 태스크 함수 구현 (`uart_task()`)
   - 8바이트 프레임 수신 (`uart_read_bytes` 사용, 100ms 타임아웃)
   - **오류 처리**: 수신 바이트 수에 따라 오류(`len < 0`), 타임아웃(`len == 0`), 불완전 수신(`len != 8`) 분기 처리 및 로깅
   - 프레임 큐 전송 (`xQueueSend`)
2. 순번 검증 함수 구현 (`validateSequenceNumber()`)
   - 예상 시퀀스 번호와 비교
   - 순환 처리 (0~255)
   - 프레임 손실 감지 및 로그 출력
3. 프레임 유효성 검증 함수 구현 (`validateBridgeFrame()`)
   - 프레임 크기(8바이트) 및 `buttons` 필드 범위(0x00~0x07) 검증
   - 순번 검증과 유효성 검증을 분리하여 명확한 오류 진단
4. **주의사항**:
   - `uart_handler.h`에 `extern QueueHandle_t frame_queue;` 선언 추가 (의존성 역전 원칙)

**참조 문서 및 섹션**:
- `docs/board/esp32s3-code-implementation-guide.md` §4.1 UART 통신 모듈 구현 (ESP-IDF)
- ESP-IDF 문서: FreeRTOS Task 생성 및 관리

**검증**:
- [x] `uart_task()` 함수 구현됨 ✓ (src/board/BridgeOne/main/uart_handler.c)
- [x] `uart_read_bytes()` 호출로 8바이트 수신 로직 구현됨 ✓ (pdMS_TO_TICKS(100) 타임아웃 포함)
- [x] 타임아웃 설정됨 (100ms) ✓
- [x] **수신 바이트 수에 따른 상세한 오류 처리 로직 구현됨** ✓ (len < 0, len == 0, len != 8 확인)
- [x] `validateSequenceNumber()` 함수 구현됨 ✓ (src/board/BridgeOne/main/uart_handler.c)
- [x] 시퀀스 번호 순환 처리 (0→255→0) ✓ ((seq + 1) & 0xFF)
- [x] 프레임 손실 감지 및 로그 출력 ✓ (ESP_LOGW로 패킷 손실 수 출력)
- [x] `validateBridgeFrame()` 프레임 범위 검증 함수 구현됨 ✓ (buttons 0x00~0x07, 크기 8바이트)
- [x] FreeRTOS 큐에 프레임 전송 (`xQueueSend()`) ✓
- [x] **`uart_handler.h`에 `extern QueueHandle_t frame_queue` 선언 포함됨** ✓
- [x] 디버그 로그 출력 (수신한 프레임 정보) ✓ (DEBUG_FRAME_VERBOSE 매크로)
- [x] `idf.py build` 성공 ✓

**⚠️ Phase 2.1.2.1에서의 변경사항 영향**:
- **UART는 app_main()에서 이미 초기화됨**: 이 Phase에서는 uart_init()을 다시 호출할 필요 없음
  - app_main()이 uart_init()을 호출하므로, uart_task() 시작 시 UART 드라이버는 이미 준비 상태
  - uart_read_bytes()를 바로 호출 가능
- **frame_queue는 별도로 생성/초기화 필요**: uart_init()에서는 UART 드라이버 설정만 수행하고 큐는 생성하지 않음
  - app_main() 또는 uart_task()에서 xQueueCreate()로 frame_queue 생성 필수
  - 예: `frame_queue = xQueueCreate(UART_FRAME_QUEUE_SIZE, sizeof(bridge_frame_t));`
- **CMakeLists.txt 의존성 설정 완료**: 이미 uart_handler.c가 SRCS에 추가되었으므로 추가 설정 불필요

**✅ Phase 2.1.2.2 구현 완료**

**📝 구현 변경사항 및 후속 Phase 영향**:
- frame_queue 초기화: app_main() "1.6" 섹션에서 수행 (uart_init() 직후)
- uart_task 생성: 실제 구현은 Phase 2.1.2.2에서 BridgeOne.c app_main()으로 이동 (Priority 6, Core 0)
- 우선순위 조정: 원계획 Priority 10 → 실제 Priority 6 (USB 5보다 높게 조정)
- 헤더 의존성: freertos/*.h, esp_task_wdt.h 추가 (컴파일 오류 해결)
- **후속 변경 필요**: Phase 2.1.4.2 우선순위 값 변경, Phase 2.1.4.3 워치독 등록 추가, Phase 2.1.5.3 로그 검증 항목 추가

---

#### Phase 2.1.2.3: HID 태스크 및 프레임 처리 로직

**목표**: HID 태스크 구현 및 BridgeFrame을 HID 리포트로 변환

**세부 목표**:
1. `hid_handler.h` 헤더 파일 작성
2. `hid_handler.c` 파일 작성
3. HID 태스크 함수 구현 (`hid_task()`)
   - FreeRTOS 큐에서 프레임 수신
   - `processBridgeFrame()` 호출
4. `processBridgeFrame()` 함수 구현
   - Keyboard/Mouse 데이터 분리
   - Phase 2.1.1.3에서 구현한 `hid_update_report_state()` 헬퍼 함수 활용
   - `sendKeyboardReport()` 호출
   - `sendMouseReport()` 호출
5. `sendKeyboardReport()` 함수 구현
   - HID Keyboard 리포트 작성 (8바이트)
   - `tud_hid_n_report()` 호출 (Instance 0, Report ID: 0 - Boot Protocol)
6. `sendMouseReport()` 함수 구현
   - HID Mouse 리포트 작성 (4바이트)
   - `tud_hid_n_report()` 호출 (Instance 1, Report ID: 0 - Boot Protocol)

**참조 문서 및 섹션**:
- `docs/board/esp32s3-code-implementation-guide.md` §4.2 USB HID 모듈 구현 (ESP-IDF TinyUSB)
- `docs/board/esp32s3-code-implementation-guide.md` §3.4 BridgeOne 프레임 처리 로직
- TinyUSB 문서: `tud_hid_n_report()` 사용법
- Phase 2.1.1.3: 구현된 헬퍼 함수 (`hid_update_report_state()`)
- `.cursor/rules/tinyusb-hid-implementation.mdc` - HID 리포트 전송 API 사용법
- `.cursor/rules/tinyusb-freertos-integration.mdc` - HID 태스크 설계 및 큐 동기화 가이드

**검증**:
- [ ] `src/board/BridgeOne/main/hid_handler.h` 파일 생성됨
- [ ] `src/board/BridgeOne/main/hid_handler.c` 파일 생성됨
- [ ] `hid_task()` 함수 구현됨
- [ ] `xQueueReceive()` 호출로 프레임 수신
- [ ] `processBridgeFrame()` 함수 구현됨
- [ ] Keyboard/Mouse 필드 분리 로직 구현
- [ ] Phase 2.1.1.3에서 구현한 `hid_update_report_state()` 함수 활용 확인
- [ ] `sendKeyboardReport()` 함수 구현됨 (modifiers, keycode1/2 포함)
- [ ] `sendMouseReport()` 함수 구현됨 (buttons, x/y, wheel 포함)
- [ ] `tud_hid_n_report()` 호출 (Instance 구분)
- [ ] 에러 처리 (USB 연결 해제 시)
- [ ] 디버그 로그 출력 (리포트 전송 정보)
- [ ] `idf.py build` 성공

**⚠️ Phase 2.1.1.3 및 2.1.2.1 변경사항 영향**:
- **필드명 매핑 필수**: `processBridgeFrame()` 구현 시 실제 필드명 적용
  - `bridge_frame_t.modifier` → `hid_keyboard_report_t.modifier`
  - `bridge_frame_t.x` → `hid_mouse_report_t.x`
  - `bridge_frame_t.y` → `hid_mouse_report_t.y`

**📝 Phase 2.1.2.1 → 2.1.2.3 누적 변경사항 정리**:

| Phase | 변경사항 | 영향 범위 |
|-------|---------|---------|
| 2.1.2.1 | UART 초기화를 app_main()에서 수행 (TinyUSB 직후) | 2.1.2.2 (uart_task()는 초기화 불필요) |
| 2.1.2.1 | CMakeLists.txt 의존성 설정 (uart_handler.c 추가, TinyUSB PRIV_REQUIRES에 main 추가) | 빌드 시스템 (향후 컴포넌트 추가 시 동일 패턴 적용) |
| 2.1.2.2 (예상) | frame_queue는 app_main() 또는 uart_task()에서 생성 (uart_init()에서 생성 안 함) | 2.1.2.3 (frame_queue를 통해 프레임 수신) |
| 2.1.2.3 | 필드명: bridge_frame_t.modifier, keycode1/2 (UART 프로토콜 기준) | 없음 (이미 구조체에 반영됨) |

**✅ 후속 Phase 수정 완료 사항**:
- Phase 2.1.2.2: UART 초기화 시점 및 frame_queue 생성 위치 명시 추가
- Phase 2.1.2.3: Phase 2.1.2.1 변경사항 영향 문서화 (이미 반영됨)

---

### Phase 2.1.3: 코드 리팩토링 및 품질 개선

**목표**: Phase 2.1.2.3까지 구현된 코드에서 중복 구현 제거 및 모듈 책임 분리

**개발 기간**: 1-2일

**세부 목표**:
1. 중복 구현된 TinyUSB 콜백 함수 제거
   - `tud_hid_get_report_cb()`, `tud_hid_set_report_cb()`를 `hid_handler.c`에만 구현
   - `usb_descriptors.c`에서 제거 (USB 디스크립터 정의만 담당)
2. 중복 구현된 헬퍼 함수 제거
   - `hid_update_report_state()`, `hid_get_keyboard_led_status()` 함수 통합
3. HID 인터페이스 번호 상수 통합
   - `INTERFACE_HID_KB` → `ITF_NUM_HID_KEYBOARD` 로 이름 통일
   - Legacy 호환성 위해 별칭(alias) 정의
4. FreeRTOS 큐 핸들 선언 통합
   - `frame_queue` extern 선언을 `uart_handler.h`에 중앙화
5. 모듈 책임 분리 명확화
   - `usb_descriptors.c`: USB 디스크립터 정의만
   - `hid_handler.c`: HID 상태 관리 및 콜백 처리
   - `uart_handler.c`: UART 통신만

**참조 문서 및 섹션**:
- 이전 Phase 2.1.2.3의 모든 구현 파일
- `.cursor/rules/tinyusb-hid-implementation.mdc` - HID 콜백 함수 및 리포트 상태 관리 가이드
- `.cursor/rules/tinyusb-architecture.mdc` - 모듈 책임 분리 및 아키텍처 설계 원칙

**검증**:
- [ ] `src/board/BridgeOne/main/usb_descriptors.c` 중복 함수 제거됨
- [ ] `src/board/BridgeOne/main/usb_descriptors.h` 중복 선언 제거됨
- [ ] `src/board/BridgeOne/main/hid_handler.c` 통합 구현 완료
- [ ] `src/board/BridgeOne/main/hid_handler.h` 모든 함수 선언 추가됨
- [ ] `src/board/BridgeOne/main/uart_handler.h` frame_queue extern 선언 추가됨
- [ ] `src/board/BridgeOne/main/uart_handler.c` 중복 선언 제거됨
- [ ] HID 인터페이스 번호 상수 통합됨 (ITF_NUM_HID_KEYBOARD, ITF_NUM_HID_MOUSE)
- [ ] 누락된 헤더 파일 추가됨 (<string.h>, class/hid/hid.h)
- [ ] Linter 오류 없음 (모든 .c/.h 파일 검증)
- [ ] 모듈 책임 분리 명확화 (각 모듈의 역할 문서화)
- [ ] `idf.py build` 성공

---

### Phase 2.1.4: ESP32-S3 FreeRTOS 태스크 구조 구현

**목표**: 듀얼 코어 활용 FreeRTOS 멀티태스크 시스템 구축

**개발 기간**: 2-3일

**세부 목표**:
1. app_main() 초기화 순서 설계
2. 각 태스크 생성 및 시작
3. TWDT 설정 및 검증

**참조 문서 및 섹션**:
- `docs/board/esp32s3-code-implementation-guide.md` §3.5 FreeRTOS 태스크 구조
- `docs/board/esp32s3-code-implementation-guide.md` §3.6 데이터 플로우 설계
- `.cursor/rules/tinyusb-freertos-integration.mdc` - TinyUSB와 FreeRTOS 멀티태스크 통합 패턴

---

#### Phase 2.1.4.1: app_main() 초기화 함수 작성

**목표**: TinyUSB 및 UART 초기화, 큐 생성 구현

**세부 목표**:
1. TinyUSB 초기화 (`tusb_init` API 사용)
   - **주의**: `tud_init()` 대신 ESP-IDF 프레임워크 권장 API인 `tusb_init(BOARD_TUD_RHPORT, &dev_init)` 사용
   - `tusb_rhport_init_t` 구조체를 통해 TUSB_ROLE_DEVICE, TUSB_SPEED_FULL 설정
2. UART 초기화 호출 (`uart_init()`)
3. 프레임 큐 생성 (`xQueueCreate()`)
4. 로그 메시지 출력
5. **주의사항**:
   - BridgeOne은 특정 보드 기능(LED, 버튼)에 의존하지 않으므로, BSP 의존성을 제거하고 `board_init()` 및 `board_init_after_tusb()` 함수 호출을 생략
   - `bsp/board_api.h` 헤더를 제거하고 `tusb.h`와 ESP-IDF 표준 헤더만 사용하여 의존성 최소화

**⚠️ Phase 2.1.1.1에서의 변경사항 영향**:
- **RHPORT 상수값 하드코딩**: Phase 2.1.1.1에서 `BOARD_TUD_RHPORT` 매크로 대신 직접 `0`으로 지정됨. 따라서 이 Phase에서도 `tusb_init(0, &dev_init)` 형태로 호출해야 함 (ESP32-S3는 USB OTG 포트가 1개만 지원)
- **tusb_config.h 필수 존재**: Phase 2.1.1.1에서 생성된 `main/tusb_config.h` 파일이 TinyUSB 스택 초기화 시 자동으로 include되어야 함. 이 파일이 없으면 빌드 오류 발생

**참조 문서 및 섹션**:
- `docs/board/esp32s3-code-implementation-guide.md` §3.3.4 app_main() 초기화 순서

**검증**:
- [ ] `main.c`에서 `tusb_init(BOARD_TUD_RHPORT, &dev_init)` 호출
- [ ] **`board_init()` 및 `board_init_after_tusb()` 함수 호출 없음**
- [ ] **`bsp/board_api.h` 헤더 include 없음**
- [ ] `uart_init()` 호출 확인
- [ ] `frame_queue = xQueueCreate(32, sizeof(bridge_frame_t))` 호출
- [ ] 초기화 로그 메시지 출력 (ESP_LOGI)
- [ ] `idf.py build` 성공

---

#### Phase 2.1.4.2: UART 및 HID 태스크 생성 (Core 0)

**목표**: Core 0에서 UART 및 HID 태스크 생성

**세부 목표**:
1. UART 태스크 생성 (우선순위 10)
2. HID 태스크 생성 (우선순위 9)
3. 각 태스크 시작 로그 확인
4. 스택 크기 할당 (4096 바이트)

**참조 문서 및 섹션**:
- `.cursor/rules/tinyusb-freertos-integration.mdc` - 태스크 우선순위 및 코어 할당 가이드

**검증**:
- [ ] `xTaskCreatePinnedToCore(uart_task, "UART", 4096, NULL, 10, NULL, 0)` 호출
- [ ] `xTaskCreatePinnedToCore(hid_task, "HID", 4096, NULL, 9, NULL, 0)` 호출
- [ ] 태스크 생성 성공 확인 (nullptr 아님)
- [ ] `idf.py build` 성공

---

#### Phase 2.1.4.3: USB 태스크 생성 (Core 1) 및 TWDT 설정

**목표**: Core 1에서 USB 태스크 생성 및 워치독 설정

**세부 목표**:
1. USB 태스크 생성 (우선순위 5, Core 1)
2. TWDT (Task Watch Dog Timer) 초기화
3. 각 태스크에 TWDT 구독 설정
4. 각 태스크에서 `esp_task_wdt_reset()` 호출 확인

**참조 문서 및 섹션**:
- `docs/board/esp32s3-code-implementation-guide.md` §3.5 FreeRTOS 태스크 구조
- ESP-IDF 문서: Task Watchdog Timer

**검증**:
- [ ] `xTaskCreatePinnedToCore(usb_task, "USB", 4096, NULL, 5, NULL, 1)` 호출
- [ ] `esp_task_wdt_init()` 호출 (타임아웃 5초)
- [ ] `sdkconfig`에 `CONFIG_ESP_TASK_WDT=y` 설정
- [ ] `sdkconfig`에 `CONFIG_ESP_TASK_WDT_TIMEOUT_S=5` 설정
- [ ] `main/CMakeLists.txt`에 모든 소스 파일 등록 완료
- [ ] `idf.py build` 성공

---

### Phase 2.1.5: ESP32-S3 펌웨어 플래싱 및 HID 디바이스 인식 검증

**목표**: 구현된 펌웨어를 ESP32-S3에 플래싱하고 Windows에서 HID 디바이스 인식 확인

**유저 사전 작업** (LLM 실행 전 필수):
1. ESP32-S3을 PC에 USB 케이블로 연결 (USB-OTG 포트 사용)

**LLM 검증 작업**:
1. `idf.py build` 실행 및 빌드 성공 확인
2. COM 포트 자동 감지
3. `idf.py -p COMx flash` 실행
4. `idf.py -p COMx monitor` 실행하여 정상 부팅 확인
5. Windows Device Manager에서 HID 디바이스 2개 인식 확인

**참조 문서 및 섹션**:
- `docs/board/esp32s3-code-implementation-guide.md` §7.3 빌드, 플래시, 모니터링
- Phase 1.2.4 빌드 환경 검증 및 하드웨어 통합 테스트
- `.cursor/rules/tinyusb-debugging.mdc` - TinyUSB 빌드 오류, 런타임 문제 디버깅 가이드

**검증**:
- [] `idf.py build` 성공 (메시지: "BUILD SUCCESSFUL")
- [] `build/BridgeOne.bin` 파일 생성됨
- [] 플래싱 성공 ("Hash of data verified" 메시지 표시)
- [] 시리얼 모니터에서 "TinyUSB initialized" 또는 "USB initialized" 메시지 확인
- [] 시리얼 모니터에서 "UART task started" 메시지 확인
- [] 시리얼 모니터에서 "HID task started" 메시지 확인
- [] 시리얼 모니터에서 "USB task started" 메시지 확인
- [] Windows Device Manager에서 "USB Input Device" 2개 확인 (Keyboard, Mouse)
- [] 드라이버 오류 없음 (노란색 느낌표 미표시)
- [] PowerShell 명령 실행: `Get-PnpDevice | Where-Object {$_.DeviceID -match "VID_303A"} | Format-Table FriendlyName, Status, DeviceID`로 2개 디바이스 확인 (Status: OK)

---

### Phase 2.1.6: Windows HID 디바이스 인식 및 기본 검증

**목표**: ESP32-S3이 Windows에서 HID 디바이스로 인식되는 것 확인

**개발 기간**: 1일

**세부 목표**:
1. Device Manager에서 HID 디바이스 확인
2. 드라이버 오류 없음
3. PowerShell로 디바이스 상태 확인

**참조 문서 및 섹션**:
- `docs/board/esp32s3-code-implementation-guide.md` §9 성능 벤치마크 및 품질 기준

---

#### Phase 2.1.6.1: Device Manager에서 HID 디바이스 확인

**목표**: Windows Device Manager에서 ESP32-S3 HID 디바이스 2개 인식 확인

**유저 사전 작업**:
1. ESP32-S3을 PC의 USB-OTG 포트에 연결 (앞의 Phase 2.1.5.2와는 다른 포트)

**세부 목표**:
1. Device Manager 열기
2. "Human Interface Devices" 섹션 확인
3. "USB Input Device" 또는 "BridgeOne HID" 2개 디바이스 확인
4. 노란색 느낌표 없음 (드라이버 오류 없음)

**검증**:
- [ ] Device Manager에서 2개의 HID 디바이스 표시
- [ ] 첫 번째 디바이스: "USB Input Device" 또는 "HID Keyboard Boot Device"
- [ ] 두 번째 디바이스: "USB Input Device" 또는 "HID Mouse Boot Device"
- [ ] 각 디바이스 드라이버 상태: "정상" (노란색 느낌표 없음)
- [ ] Device ID에 "VID_303A" 포함 확인

---

#### Phase 2.1.6.2: PowerShell 디바이스 상태 확인

**목표**: PowerShell에서 ESP32-S3 HID 디바이스 상태 조회

**세부 목표**:
1. PowerShell 명령어 실행: 
   ```powershell
   Get-PnpDevice | Where-Object {$_.DeviceID -match "VID_303A"} | Format-Table FriendlyName, Status, DeviceID
   ```
2. 모든 디바이스의 Status가 "OK" 확인
3. 2개 이상의 디바이스 목록 표시

**검증**:
- [ ] PowerShell 명령 실행 성공
- [ ] 2개 이상 디바이스 표시
- [ ] 모든 디바이스의 Status 필드: "OK"
- [ ] 드라이버 에러 없음

---

### Phase 2.1.7: Android 프로토콜 구현 (BridgeFrame)

**목표**: BridgeOne 프로토콜 정의 및 프레임 생성 로직 구현

**개발 기간**: 3-4일

**세부 목표**:
1. BridgeFrame 데이터 클래스 정의
2. FrameBuilder 클래스 구현
3. 순번 관리 (0~255 순환)
4. Little-Endian 직렬화
5. 단위 테스트

**참조 문서 및 섹션**:
- `docs/android/technical-specification-app.md` §1.2 BridgeOne 프로토콜 명세
- `docs/technical-specification.md` §2.1 UART 통신 (Android ↔ ESP32-S3)

---

#### Phase 2.1.7.1: BridgeFrame 데이터 클래스 정의

**목표**: 8바이트 BridgeOne 프레임 데이터 클래스 정의

**세부 목표**:
1. `com.bridgeone.app.protocol` 패키지 생성
2. `BridgeFrame.kt` 데이터 클래스 작성
3. 8바이트 필드 정의 (seq, buttons, deltaX, deltaY, wheel, modifiers, keyCode1, keyCode2)
4. Docstring 및 주석 추가

**검증**:
- [ ] `src/android/app/src/main/java/com/bridgeone/app/protocol/` 디렉터리 생성됨
- [ ] `BridgeFrame.kt` 파일 생성됨
- [ ] 데이터 클래스 선언됨 (data class)
- [ ] 8개 필드 정의:
  - [ ] seq: UByte (시퀀스 번호)
  - [ ] buttons: UByte (마우스 버튼 비트)
  - [ ] deltaX: Byte (X축 이동값)
  - [ ] deltaY: Byte (Y축 이동값)
  - [ ] wheel: Byte (휠 값)
  - [ ] modifiers: UByte (키보드 modifier)
  - [ ] keyCode1: UByte (첫 번째 키코드)
  - [ ] keyCode2: UByte (두 번째 키코드)
- [ ] Docstring 작성됨
- [ ] Gradle 빌드 성공

---

#### Phase 2.1.7.2: FrameBuilder 및 순번 관리

**목표**: 프레임 생성 및 바이트 직렬화 구현

**세부 목표**:
1. `FrameBuilder.kt` 클래스 작성
2. 순번 카운터 관리 (0~255 순환)
3. `toByteArray()` 메서드 구현 (Little-Endian)
4. 기본값 초기화 함수 제공

**검증**:
- [ ] `FrameBuilder.kt` 파일 생성됨
- [ ] object 싱글톤 또는 companion object로 구현
- [ ] 순번 카운터 변수 (volatile 또는 thread-safe)
- [ ] `toByteArray(): ByteArray` 메서드 구현
- [ ] 바이트 직렬화 순서: seq, buttons, deltaX, deltaY, wheel, modifiers, keyCode1, keyCode2
- [ ] Little-Endian 확인 (각 필드 1바이트 순서)
- [ ] Gradle 빌드 성공

---

#### Phase 2.1.7.3: 단위 테스트 및 검증

**목표**: BridgeFrame 프레임 구조 및 직렬화 테스트

**세부 목표**:
1. `BridgeFrameTest.kt` 테스트 클래스 작성
2. 프레임 크기 검증 (8바이트)
3. 바이트 순서 검증
4. 순번 순환 검증 (255 → 0)
5. 값 범위 검증

**검증**:
- [ ] `BridgeFrameTest.kt` 파일 생성됨
- [ ] 테스트 케이스: 프레임 크기 == 8바이트
- [ ] 테스트 케이스: 바이트 순서 정확성
- [ ] 테스트 케이스: 순번 순환 (255 → 0)
- [ ] 테스트 케이스: 모든 필드 값 범위 (UByte: 0~255, Byte: -128~127)
- [ ] 모든 테스트 통과
- [ ] Gradle 빌드 성공

---

### Phase 2.1.8: Android USB Serial 통신 구현

**목표**: USB Serial 라이브러리를 통한 ESP32-S3 UART 통신 구현

**개발 기간**: 4-5일

**세부 목표**:
1. usb-serial-for-android 라이브러리 추가
2. UsbSerialManager 싱글톤 클래스 구현
3. ESP32-S3 자동 감지
4. UART 통신 설정 (1Mbps, 8N1)
5. 연결 상태 모니터링

**참조 문서 및 섹션**:
- `docs/android/technical-specification-app.md` §1.1 통신 아키텍처 설계
- `docs/technical-specification.md` §5.2 usb-serial-for-android 프로젝트 분석

---

#### Phase 2.1.8.1: 라이브러리 의존성 추가 및 기본 구조

**목표**: usb-serial-for-android 라이브러리 추가 및 UsbSerialManager 기본 구조 작성

**세부 목표**:
1. `gradle/libs.versions.toml`에 라이브러리 버전 추가
2. `app/build.gradle.kts`에 의존성 선언
3. Gradle 동기화 및 라이브러리 다운로드
4. `UsbSerialManager.kt` 싱글톤 클래스 생성
5. 기본 데이터 멤버 선언

**검증**:
- [ ] `gradle/libs.versions.toml`에 `usb-serial-for-android = "3.7.3"` 추가
- [ ] `app/build.gradle.kts`에 `libs.usb.serial.for.android` 의존성 추가됨
- [ ] Gradle 동기화 성공 (라이브러리 다운로드 완료)
- [ ] `src/android/app/src/main/java/com/bridgeone/app/usb/` 디렉터리 생성됨
- [ ] `UsbSerialManager.kt` 파일 생성됨 (object 싱글톤)
- [ ] UsbManager, UsbSerialPort, isConnected 멤버 변수 선언됨
- [ ] Gradle 빌드 성공

---

#### Phase 2.1.8.2: ESP32-S3 자동 감지 및 USB 권한 요청

**목표**: VID 필터링을 통한 ESP32-S3 자동 감지 및 USB 권한 요청

**세부 목표**:
1. VID/PID 상수 정의 (VID: 0x303A)
2. `findEsp32s3Device()` 함수 구현
3. `requestPermission()` 함수 구현
4. Intent 필터 및 BroadcastReceiver 설정
5. AndroidManifest.xml 권한 추가

**검증**:
- [ ] VID/PID 상수 정의됨
- [ ] `findEsp32s3Device()` 함수 구현됨
- [ ] `requestPermission()` 함수 구현됨
- [ ] PendingIntent 생성 및 등록
- [ ] AndroidManifest.xml에 권한 추가 (USB_DEVICE_ATTACH, DETACH)
- [ ] BroadcastReceiver 등록됨
- [ ] Gradle 빌드 성공

---

#### Phase 2.1.8.3: UART 통신 설정 및 포트 관리

**목표**: 1Mbps 8N1 통신 설정 및 포트 열기/닫기 구현

**세부 목표**:
1. `openPort()` 함수 구현 (1Mbps, 8N1 설정)
2. `closePort()` 함수 구현
3. `isConnected()` 함수 구현
4. 리소스 해제 및 예외 처리
5. 디버그 로그 추가

**검증**:
- [ ] `openPort()` 함수 구현됨
- [ ] `setParameters(1000000, 8, 1, 0)` 호출 확인
- [ ] `closePort()` 함수 구현됨
- [ ] `isConnected()` 함수 구현됨
- [ ] 예외 처리 (IOException, 연결 실패)
- [ ] 디버그 로그 출력 (연결/해제 시점)
- [ ] Gradle 빌드 성공

---

#### Phase 2.1.8.4: 프레임 전송 및 연결 모니터링

**목표**: BridgeFrame 전송 함수 및 연결 상태 모니터링 구현

**세부 목표**:
1. `sendFrame(frame: BridgeFrame)` 함수 구현
2. 바이트 변환 및 UART 전송
3. 전송 완료 확인 (return 값 체크)
4. 연결 상태 모니터링 (BroadcastReceiver)
5. 재연결 로직 기본 구조

**검증**:
- [ ] `sendFrame()` 함수 구현됨
- [ ] `frame.toByteArray()` 호출로 직렬화
- [ ] `write()` 호출로 전송
- [ ] 반환값 체크 (전송 바이트 수)
- [ ] 예외 처리 (USB 연결 해제 시)
- [ ] BroadcastReceiver에서 연결/해제 감지
- [ ] 디버그 로그 (프레임 전송 정보)
- [ ] Gradle 빌드 성공

---

### Phase 2.1.9: Android 터치 입력 처리 (TouchpadWrapper)

**목표**: 터치 이벤트를 8바이트 프레임으로 변환하는 로직 구현

**개발 기간**: 4-5일

**세부 목표**:
1. TouchpadWrapper Composable 기본 UI
2. 터치 이벤트 감지
3. 델타 계산 및 데드존 보상
4. 클릭 감지
5. 프레임 생성 및 전송

**참조 문서 및 섹션**:
- `docs/android/technical-specification-app.md` §2.2 터치패드 알고리즘
- `docs/android/component-touchpad.md`

---

#### Phase 2.1.9.1: 기본 UI 및 터치 이벤트 감지

**목표**: TouchpadWrapper UI 구성 및 터치 이벤트 감지 구현

**세부 목표**:
1. `TouchpadWrapper.kt` Composable 생성
2. 1:2 비율 직사각형 UI
3. 둥근 모서리 적용
4. `Modifier.pointerInput()` 구현
5. 터치 좌표 저장 (이전/현재)

**검증**:
- [ ] `src/android/app/src/main/java/com/bridgeone/app/ui/components/TouchpadWrapper.kt` 생성됨
- [ ] Composable 함수 선언됨
- [ ] Box 또는 Surface로 UI 구성
- [ ] 1:2 비율 적용 (가로:세로 = 1:2)
- [ ] 최소 크기 160dp×320dp 이상
- [ ] 둥근 모서리: 너비의 3%
- [ ] `Modifier.pointerInput()` 구현됨
- [ ] ACTION_DOWN, ACTION_MOVE, ACTION_UP 처리
- [ ] Preview 함수 작성 및 렌더링
- [ ] Gradle 빌드 성공

---

#### Phase 2.1.9.2: 델타 계산 및 데드존 보상

**목표**: 터치 좌표 델타 계산 및 데드존 보상 알고리즘 구현

**세부 목표**:
1. `calculateDelta()` 함수 구현
2. dp → pixel 변환 (LocalDensity 사용)
3. `applyDeadZone()` 함수 구현
4. DEAD_ZONE_THRESHOLD = 15dp 적용
5. 델타 범위 정규화 (-127 ~ 127)

**검증**:
- [ ] `calculateDelta()` 함수 구현됨
- [ ] 좌표 계산 정확 (current - previous)
- [ ] dp → pixel 변환 고려됨 (LocalDensity 사용)
- [ ] X, Y 축 분리 처리됨
- [ ] `applyDeadZone()` 함수 구현됨
- [ ] DEAD_ZONE_THRESHOLD = 15dp 정의됨
- [ ] 임계값 이하 → 0 처리
- [ ] 임계값 초과 → 정규화 적용
- [ ] 델타 범위 -127 ~ 127 확인
- [ ] 디버그 로그 (원본/적용 후 값)
- [ ] Gradle 빌드 성공

---

#### Phase 2.1.9.3: 클릭 감지 및 프레임 생성

**목표**: 클릭 감지 로직 및 프레임 생성/전송 구현

**세부 목표**:
1. `detectClick()` 함수 구현
2. CLICK_MAX_DURATION = 500ms, CLICK_MAX_MOVEMENT = 15dp
3. `getButtonState()` 함수 구현
4. `createFrame()` 함수 구현
5. `sendFrame()` 함수 (비동기 처리)
6. 상태 초기화

**검증**:
- [ ] `detectClick()` 함수 구현됨
- [ ] CLICK_MAX_DURATION = 500ms 정의됨
- [ ] CLICK_MAX_MOVEMENT = 15dp 정의됨
- [ ] 터치 시작 시간 기록
- [ ] 경과시간 및 이동거리 계산
- [ ] 클릭 판정 로직 정확 (시간 AND 거리)
- [ ] `getButtonState()` 함수 구현됨
- [ ] LEFT_CLICK 비트 처리 (0x01)
- [ ] `createFrame()` 함수 구현됨
- [ ] FrameBuilder 연동
- [ ] `sendFrame()` 함수 구현됨 (LaunchedEffect 또는 viewModelScope)
- [ ] UsbSerialManager.sendFrame() 호출
- [ ] 예외 처리 및 로그
- [ ] 상태 초기화 로직
- [ ] Compose Preview 렌더링 확인
- [ ] Gradle 빌드 성공

---

### Phase 2.1.10: End-to-End 검증 및 성능 테스트

**목표**: Android ↔ ESP32-S3 ↔ Windows 전체 경로 검증

**개발 기간**: 3-4일

**세부 목표**:
1. HID Mouse 경로 검증
2. HID Keyboard 경로 검증
3. 지연시간 측정
4. 프레임 손실률 측정
5. 안정성 테스트

**참조 문서 및 섹션**:
- `docs/board/esp32s3-code-implementation-guide.md` §9 성능 벤치마크 및 품질 기준
- `docs/technical-specification.md` §3.2 성능 임계값

---

#### Phase 2.1.10.1: HID Mouse 경로 End-to-End 검증

**목표**: 터치 → UART → HID Mouse → Windows 마우스 이동 전체 경로 검증

**유저 사전 작업**:
1. Android 기기를 ESP32-S3의 USB-UART 포트에 USB-C 케이블 연결
2. ESP32-S3의 USB-OTG 포트를 Windows PC에 USB 케이블로 연결
3. Android 앱 빌드 및 설치
4. Android 앱 실행

**세부 목표**:
1. 시리얼 모니터에서 UART 프레임 수신 로그 확인
2. 시리얼 모니터에서 HID Mouse 리포트 전송 로그 확인
3. Windows에서 마우스 포인터 이동 확인
4. 이동 방향 및 속도 검증

**검증**:
- [ ] Android 앱 터치패드 드래그 시 Windows 마우스 포인터 이동 확인
- [ ] 시리얼 로그: "UART frame received: seq=X buttons=X deltaX=X deltaY=X"
- [ ] 시리얼 로그: "HID Mouse report sent: buttons=X deltaX=X deltaY=X wheel=X"
- [ ] 마우스 이동 방향이 터치 제스처 방향과 일치
- [ ] 마우스 이동 속도가 터치 제스처 속도와 비례
- [ ] 클릭 제스처 (500ms 이내 탭) → Windows 좌클릭 동작 확인
- [ ] 지연시간 50ms 이하 (사용자 체감 테스트)
- [ ] 프레임 손실 없음 (시리얼 로그에서 순번 카운터 연속성 확인: 0→1→2→...→255→0)

---

#### Phase 2.1.10.2: HID Keyboard 경로 End-to-End 검증

**목표**: 키 입력 → UART → HID Keyboard → Windows 키 입력 검증

**세부 목표**:
1. KeyboardKeyButton 컴포넌트 구현
2. 키 다운/업 이벤트 → 프레임 생성
3. Windows 메모장에서 키 입력 확인
4. BIOS 호환성 검증 (Del 키 테스트)

**참조 문서 및 섹션**:
- `docs/android/technical-specification-app.md` §2.3.2.1 KeyboardKeyButton 컴포넌트 설계 요구사항
- `docs/board/esp32s3-code-implementation-guide.md` §4.2 USB HID 모듈 구현
- `docs/android/component-design-guide-app.md` (KeyboardKeyButton 상세)

**검증**:
- [ ] `src/android/app/src/main/java/com/bridgeone/app/ui/components/KeyboardKeyButton.kt` 파일 생성됨
- [ ] 터치 다운 시 `keyCode` 포함한 프레임 전송 (modifiers=0, keyCode1=X, keyCode2=0)
- [ ] 터치 업 시 빈 프레임 전송 (keyCode1=0, keyCode2=0)
- [ ] ESP32-S3 시리얼 로그에 "HID Keyboard report sent: modifiers=X keyCodes=[X,X,0,0,0,0]" 메시지 표시
- [ ] Windows 메모장에서 키 입력 정상 작동 확인 (사용자 테스트)
- [ ] Del 키 정상 동작 확인
- [ ] Esc 키 정상 동작 확인
- [ ] Enter 키 정상 동작 확인
- [ ] BIOS 진입 테스트 (재부팅 시 Del 키 → BIOS 화면 진입 확인, 사용자 테스트)
- [ ] 키 입력 지연시간 10ms 이하 (사용자 체감 테스트)

---

#### Phase 2.1.10.3: 지연시간 및 프레임 손실률 측정

**목표**: 성능 임계값 검증 (50ms 이하 지연, 0.1% 이하 손실)

**세부 목표**:
1. 시리얼 로그 타임스탬프 분석
2. Android → ESP32-S3 전송 시간
3. ESP32-S3 → Windows HID 전송 시간
4. 총 지연시간 측정
5. 연속 프레임 손실률 계산

**검증**:
- [ ] 평균 지연시간 < 50ms
- [ ] 최대 지연시간 < 100ms (99 percentile)
- [ ] 프레임 손실률 < 0.1% (시리얼 로그 순번 분석)
- [ ] 100프레임 이상 테스트 (프레임 손실 < 1개)

---

#### Phase 2.1.10.4: 안정성 및 스트레스 테스트

**목표**: 장시간 사용 안정성 검증

**세부 목표**:
1. 4시간 연속 마우스 + 키보드 입력
2. 크래시 없음 (ESP32-S3, Android 앱 모두)
3. 메모리 누수 없음
4. 자동 재연결 테스트

**검증**:
- [ ] 4시간 연속 사용 중 크래시 없음
- [ ] 마우스 이동 중 클릭 정상 동작
- [ ] 키보드 타이핑 중 마우스 조작 정상 동작
- [ ] 메모리 사용량 안정 (`esp_get_free_heap_size()` 일정 유지)
- [ ] CPU 사용률 < 30% (`vTaskGetRunTimeStats()`)
- [ ] Windows에서 입력 오류 없음 (마우스/키보드 정상 작동)
- [ ] Android 앱 배터리 소모 정상 범위 (4시간 사용 시 배터리 20% 이하 소모)

---

### Phase 2.1.11: Android 키보드 UI 구현 및 완성도 개선

**목표**: HID Keyboard 입력을 위한 Android UI 완성 및 추가 기능 구현

**개발 기간**: 3-4일

**세부 목표**:
1. KeyboardKeyButton 컴포넌트 구현
2. 키보드 레이아웃 구성
3. 수정자 키 (Shift, Ctrl, Alt) 구현
4. 예측 입력 또는 매크로 기능 (옵션)

**참조 문서 및 섹션**:
- `docs/android/component-design-guide-app.md` - KeyboardKeyButton 설계
- `docs/android/technical-specification-app.md` §2.3.2 키보드 컴포넌트

---

#### Phase 2.1.11.1: KeyboardKeyButton 컴포넌트 구현

**목표**: 개별 키 입력을 처리하는 KeyboardKeyButton 컴포넌트 구현

**세부 목표**:
1. `KeyboardKeyButton.kt` Composable 생성
2. 키 코드 및 레이블 정의
3. 누르기/떼기 상태 관리
4. 시각적 피드백 (색상 변화)
5. 프레임 생성 및 전송

**검증**:
- [ ] `src/android/app/src/main/java/com/bridgeone/app/ui/components/KeyboardKeyButton.kt` 생성됨
- [ ] Composable 함수 구현됨
- [ ] 터치 다운 시 keyCode 포함 프레임 전송
- [ ] 터치 업 시 빈 프레임 전송
- [ ] 시각적 피드백 구현 (색상 변화)
- [ ] 여러 키 동시 입력 지원 (키 조합)
- [ ] Gradle 빌드 성공

---

#### Phase 2.1.11.2: 키보드 레이아웃 및 UI 구성

**목표**: 실제 키보드와 유사한 레이아웃 구성

**세부 목표**:
1. `KeyboardLayout.kt` Composable 생성
2. 주요 키 배열 (숫자, 문자, 특수 문자)
3. 기능 키 (Enter, Backspace, Tab 등)
4. 수정자 키 영역 (Shift, Ctrl, Alt)
5. 반응형 레이아웃

**검증**:
- [ ] `KeyboardLayout.kt` 파일 생성됨
- [ ] 숫자 키 행: 1~9, 0 표시
- [ ] 주요 문자 키 행 (QWERTY 또는 한글 배열)
- [ ] 기능 키 영역 (Enter, Backspace, Tab, Esc)
- [ ] 수정자 키 (Shift, Ctrl, Alt)
- [ ] 여러 화면 크기 지원 (반응형)
- [ ] Compose Preview 렌더링 확인
- [ ] Gradle 빌드 성공

---

#### Phase 2.1.11.3: 수정자 키 및 키 조합 지원

**목표**: Shift, Ctrl, Alt 등 수정자 키 구현

**세부 목표**:
1. 수정자 키 상태 관리 (눌렸음/떨어짐)
2. 키 조합 지원 (Shift+A → 'A' 대문자)
3. 멀티 터치 감지 (동시 여러 키 입력)
4. 프레임에 수정자 정보 포함

**검증**:
- [ ] 수정자 키 상태 저장됨 (mutableStateOf)
- [ ] Shift+A 입력 시 수정자=0x02, keyCode1=0x04
- [ ] Ctrl+C 입력 시 수정자=0x01, keyCode1=0x06
- [ ] 동시 여러 키 입력 지원 (최대 6개 키)
- [ ] 프레임의 modifiers 필드 정확한 값 포함
- [ ] Gradle 빌드 성공

---

### Phase 2.1.12: 최종 통합 검증 및 문서화

**목표**: Phase 2.1 전체 완료 및 최종 검증

**개발 기간**: 2-3일

**세부 목표**:
1. 모든 하위 Phase 검증 완료 확인
2. 성능 목표 달성 확인
3. 문서화 및 주석 최종 검토
4. 커밋 및 릴리스 노트 작성

---

#### Phase 2.1.12.1: 검증 체크리스트 최종 확인

**목표**: Phase 2.1의 모든 검증 항목 완료 확인

**세부 목표**:
1. Phase 2.1.1 ~ 2.1.11의 모든 검증 항목 체크
2. 성능 임계값 달성 확인
3. 호환성 검증 (Windows 10/11, Android 8.0+)

**검증**:
- [ ] Phase 2.1.1 검증 완료: USB 디스크립터
- [ ] Phase 2.1.2 검증 완료: UART 태스크
- [ ] Phase 2.1.3 검증 완료: 코드 리팩토링
- [ ] Phase 2.1.4 검증 완료: FreeRTOS 태스크
- [ ] Phase 2.1.5 검증 완료: 펌웨어 빌드/플래싱
- [ ] Phase 2.1.6 검증 완료: Windows HID 인식
- [ ] Phase 2.1.7 검증 완료: Android 프로토콜
- [ ] Phase 2.1.8 검증 완료: Android USB Serial
- [ ] Phase 2.1.9 검증 완료: Android 터치
- [ ] Phase 2.1.10 검증 완료: E2E 검증
- [ ] Phase 2.1.11 검증 완료: 키보드 UI
- [ ] 평균 지연시간 < 50ms
- [ ] 프레임 손실률 < 0.1%
- [ ] 4시간 연속 사용 무중단

---

#### Phase 2.1.12.2: 문서 및 코드 주석 최종 검토

**목표**: 모든 코드 파일 주석 및 Docstring 완성도 검증

**세부 목표**:
1. 모든 함수에 Google 스타일 Docstring 확인
2. 복잡한 로직에 한국어 주석 확인
3. 상수명 대문자 규칙 확인 (예: MAX_RETRY_COUNT)
4. Boolean 변수명 규칙 확인 (is, has, can으로 시작)
5. README 업데이트

**검증**:
- [ ] 모든 public 함수에 Docstring 포함
- [ ] 복잡한 로직에 한국어 주석
- [ ] 상수명 모두 UPPER_CASE
- [ ] Boolean 변수명 규칙 준수
- [ ] README 또는 PHASE_NOTES 문서에 Phase 2.1 완료 기록
- [ ] Linter 오류 없음 (모든 파일)

---

#### Phase 2.1.12.3: 최종 커밋 및 릴리스 노트

**목표**: Phase 2.1 완료 커밋 및 정리

**세부 목표**:
1. 모든 변경사항 커밋
2. 커밋 메시지 작성 (작가 가이드라인 준수)
3. Phase 2.1 완료 요약 문서 작성
4. 다음 Phase 2.2 준비

**검증**:
- [ ] 모든 파일 커밋 완료
- [ ] 커밋 메시지: "chore): Phase 2.1 HID 통신 경로 완전 구축"
- [ ] Phase 2.1 완료 요약 문서 작성
- [ ] Phase 2.2 시작 조건 확인

---

### Phase 2.1 완료 요약

**목표 달성**: HID 통신 경로 완전 구축 

**완료 항목**:
- Phase 2.1.1: ESP32-S3 USB Composite 디스크립터 구현
  - Phase 2.1.1.1: USB Device & Configuration Descriptor 정의
  - Phase 2.1.1.2: HID Report Descriptor (Keyboard + Mouse)
  - Phase 2.1.1.3: TinyUSB 콜백 함수 구현
- Phase 2.1.2: ESP32-S3 UART → HID 변환 로직 구현
  - Phase 2.1.2.1: UART 초기화 및 bridge_frame_t 구조체 정의
  - Phase 2.1.2.2: UART 수신 태스크 및 프레임 검증 로직
  - Phase 2.1.2.3: HID 태스크 및 프레임 처리 로직
- Phase 2.1.3: 코드 리팩토링 및 품질 개선
- Phase 2.1.4: ESP32-S3 FreeRTOS 태스크 구조 구현
  - Phase 2.1.4.1: app_main() 초기화 함수 작성
  - Phase 2.1.4.2: UART 및 HID 태스크 생성 (Core 0)
  - Phase 2.1.4.3: USB 태스크 생성 (Core 1) 및 TWDT 설정
- Phase 2.1.5: ESP32-S3 펌웨어 빌드 및 기본 검증
  - Phase 2.1.5.1: CMakeLists.txt 업데이트 및 빌드
  - Phase 2.1.5.2: 펌웨어 플래싱 및 시리얼 연결
  - Phase 2.1.5.3: 초기화 로그 검증
- Phase 2.1.6: Windows HID 디바이스 인식 및 기본 검증
  - Phase 2.1.6.1: Device Manager에서 HID 디바이스 확인
  - Phase 2.1.6.2: PowerShell 디바이스 상태 확인
- Phase 2.1.7: Android 프로토콜 구현 (BridgeFrame)
  - Phase 2.1.7.1: BridgeFrame 데이터 클래스 정의
  - Phase 2.1.7.2: FrameBuilder 및 순번 관리
  - Phase 2.1.7.3: 단위 테스트 및 검증
- Phase 2.1.8: Android USB Serial 통신 구현
  - Phase 2.1.8.1: 라이브러리 의존성 추가 및 기본 구조
  - Phase 2.1.8.2: ESP32-S3 자동 감지 및 USB 권한 요청
  - Phase 2.1.8.3: UART 통신 설정 및 포트 관리
  - Phase 2.1.8.4: 프레임 전송 및 연결 모니터링
- Phase 2.1.9: Android 터치 입력 처리 (TouchpadWrapper)
  - Phase 2.1.9.1: 기본 UI 및 터치 이벤트 감지
  - Phase 2.1.9.2: 델타 계산 및 데드존 보상
  - Phase 2.1.9.3: 클릭 감지 및 프레임 생성
- Phase 2.1.10: End-to-End 검증 및 성능 테스트
  - Phase 2.1.10.1: HID Mouse 경로 E2E 검증
  - Phase 2.1.10.2: HID Keyboard 경로 E2E 검증
  - Phase 2.1.10.3: 지연시간 및 프레임 손실률 측정
  - Phase 2.1.10.4: 안정성 및 스트레스 테스트
- Phase 2.1.11: Android 키보드 UI 구현 및 완성도 개선
  - Phase 2.1.11.1: KeyboardKeyButton 컴포넌트 구현
  - Phase 2.1.11.2: 키보드 레이아웃 및 UI 구성
  - Phase 2.1.11.3: 수정자 키 및 키 조합 지원
- Phase 2.1.12: 최종 통합 검증 및 문서화
  - Phase 2.1.12.1: 검증 체크리스트 최종 확인
  - Phase 2.1.12.2: 문서 및 코드 주석 최종 검토
  - Phase 2.1.12.3: 최종 커밋 및 릴리스 노트

**구성된 통신 경로**:
- ESP32-S3 TinyUSB 기반 HID Boot Mouse + HID Boot Keyboard 복합 디바이스
- FreeRTOS 듀얼 코어 멀티태스크 시스템 (UART/HID/USB 태스크)
- Android 앱 8바이트 델타 프레임 생성 및 UART 전송
- USB Serial 라이브러리 기반 ESP32-S3 통신
- Windows HID 기본 드라이버 자동 인식
- End-to-End 통신 경로 검증 완료 (50ms 이하 지연시간, 0.1% 이하 손실률)

**핵심 성과물**:
- ESP32-S3 펌웨어: `usb_descriptors.c`, `uart_handler.c`, `hid_handler.c`, `main.c`
- Android 앱: `BridgeFrame.kt`, `FrameBuilder.kt`, `UsbSerialManager.kt`, `TouchpadWrapper.kt`, `KeyboardKeyButton.kt`
- 검증 완료: Windows에서 마우스/키보드 정상 작동, BIOS 호환성 확인

**다음 단계**: Phase 2.2 (Vendor CDC 통신 개발) - ESP32-S3와 Windows 서버 간 양방향 JSON 통신 구현

**⚠️ Phase 2.2 시작 시 필수 작업**:
- **Phase 2.2.1: USB Configuration Descriptor에 CDC 인터페이스 추가**
  - Phase 2.1.1에서는 HID Boot Keyboard + Mouse만 구현 (Interface 0, 1)
  - Phase 2.2부터 CDC 통신이 필요하므로 Interface 2 (CDC Control), Interface 3 (CDC Data)를 추가해야 함
  - 최종 USB Composite 디바이스 구성:
    - Interface 0: HID Boot Keyboard (Endpoint 0x81)
    - Interface 1: HID Boot Mouse (Endpoint 0x82)
    - Interface 2: CDC Control Interface (Endpoint 0x83 - Notification)
    - Interface 3: CDC Data Interface (Endpoint 0x04 - OUT, 0x84 - IN)
  - Configuration Descriptor 총 길이 업데이트 필요
  - TinyUSB CDC 콜백 함수 구현 필요 (`tud_cdc_rx_cb()`, `tud_cdc_line_state_cb()`)
  - sdkconfig에 `CONFIG_TINYUSB_CDC_ENABLED=y` 설정 추가 필요
  - **참고**: `.cursor/rules/tinyusb-cdc-implementation.mdc` 참조 - CDC-ACM 가상 시리얼 포트 구현 패턴 가이드
  - **참고**: `.cursor/rules/tinyusb-descriptors.mdc` 참조 - 복합 디바이스 디스크립터 업데이트 가이드

---

### Phase 2.2 시작 전 점검사항

Phase 2.2 (Vendor CDC 통신)를 시작하기 전에 다음을 확인하십시오:

1. **Phase 2.1 변경사항이 모든 구현에 반영되었는가?**
   - tusb_config.h 파일 생성 확인
   - TinyUSB 의존성 추가 확인
   - Typedef 제거 확인
   - RHPORT 0으로 하드코딩 확인

2. **tusb_config.h 업데이트 필요**:
   ```c
   // Phase 2.2 시작 시 추가해야 할 설정
   #define CFG_TUD_CDC 1              // CDC 클래스 활성화
   #define CFG_TUD_CDC_RX_BUFSIZE 256  // CDC 수신 버퍼
   #define CFG_TUD_CDC_TX_BUFSIZE 256  // CDC 송신 버퍼
   ```

3. **USB Configuration Descriptor 업데이트 필수**:
   - Phase 2.1에서 구성된 Descriptor는 HID만 포함
   - CDC Interface 2, 3 추가 필요
   - Endpoint 0x83 (CDC Notification), 0x04/0x84 (CDC Data) 할당

4. **CMakeLists.txt 확인**:
   - CDC 콜백 함수를 포함할 새로운 모듈 (예: `cdc_handler.c`) 준비
   - 기존 TinyUSB 의존성 설정 유지

---

