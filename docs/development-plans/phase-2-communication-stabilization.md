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
- `docs/board/esp32s3-code-implementation-guide.md` §3.7 참고 구현: esp32-cdc-keyboard 프로젝트 분석
- `docs/board/esp32s3-code-implementation-guide.md` §1.3 USB Composite 디바이스 설계 계약
- `docs/board/esp32s3-code-implementation-guide.md` §3.3 TinyUSB Composite 디바이스 구현
- `docs/technical-specification.md` §2.3.1 ESP32-S3-DevkitC-1-N16R8

**검증**:
- [ ] `src/board/BridgeOne/main/usb_descriptors.c` 파일 생성됨
- [ ] `src/board/BridgeOne/main/usb_descriptors.h` 파일 생성됨
- [ ] USB Configuration Descriptor에 Interface 0 (HID Keyboard), Interface 1 (HID Mouse) 정의됨
- [ ] HID Boot Keyboard Report Descriptor 8바이트 구조 정의됨 (modifiers + reserved + keyCodes[6])
- [ ] HID Boot Mouse Report Descriptor 4바이트 구조 정의됨 (buttons + deltaX + deltaY + wheel)
- [ ] Endpoint 번호 할당 (EPNUM_HID_KB=0x83, EPNUM_HID_MOUSE=0x84)
- [ ] VID/PID 설정 (`sdkconfig.defaults`에 VID=0x303A 또는 0x1209 설정)
- [ ] `tud_hid_get_report_cb()` 함수 구현 (Keyboard/Mouse 인스턴스 구분)
- [ ] `tud_hid_set_report_cb()` 함수 구현 (Keyboard LED 처리)
- [ ] `idf.py build` 성공 (컴파일 오류 없음)

---

#### Phase 2.1.1.1: USB Device & Configuration Descriptor 정의

**목표**: VID/PID 설정 및 기본 USB Device/Configuration Descriptor 구성

**세부 목표**:
1. `usb_descriptors.h` 헤더 파일 생성
2. VID/PID 상수 정의 (VID: 0x303A 또는 0x1209)
3. USB Device Descriptor 정의
4. USB Configuration Descriptor 정의 (Interface 0: Keyboard, Interface 1: Mouse)
5. String Descriptor 정의 (Manufacturer, Product, Serial Number)
6. `usb_descriptors.c` 파일 생성 및 기본 구조 작성

**참조 문서 및 섹션**:
- `docs/board/esp32s3-code-implementation-guide.md` §1.3 USB Composite 디바이스 설계 계약
- TinyUSB 공식 문서: Device Descriptor, Configuration Descriptor 명세

**검증**:
- [ ] `src/board/BridgeOne/main/usb_descriptors.h` 파일 생성됨
- [ ] `src/board/BridgeOne/main/usb_descriptors.c` 파일 생성됨
- [ ] VID=0x303A (또는 0x1209) 정의됨
- [ ] USB Device Descriptor 배열 정의됨 (`tusb_desc_device[]`)
- [ ] USB Configuration Descriptor에 2개 Interface 정의됨
- [ ] String Descriptor 정의됨 (3개 이상: Language, Manufacturer, Product, Serial)
- [ ] Endpoint 번호 예약됨 (EPNUM_HID_KB=0x83, EPNUM_HID_MOUSE=0x84)
- [ ] `idf.py build` 성공

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

**참조 문서 및 섹션**:
- `docs/board/esp32s3-code-implementation-guide.md` §3.3 TinyUSB Composite 디바이스 구현
- HID Specification: Boot Keyboard Report Descriptor, Boot Mouse Report Descriptor
- TinyUSB 예제: `examples/device/hid_composite`

**검증**:
- [ ] HID Report Descriptor for Keyboard 정의됨 (8바이트 구조)
- [ ] HID Report Descriptor for Mouse 정의됨 (4바이트 구조)
- [ ] `tusb_desc_hid_report_keyboard[]` 배열 정의됨
- [ ] `tusb_desc_hid_report_mouse[]` 배열 정의됨
- [ ] 각 Report Descriptor 크기 정확함 (Keyboard: 63바이트, Mouse: 52바이트 예상)
- [ ] Report ID 없음 (Boot mode: Report ID 제외)
- [ ] `idf.py build` 성공

---

#### Phase 2.1.1.3: TinyUSB 콜백 함수 구현

**목표**: HID Keyboard/Mouse 인스턴스 구분 및 TinyUSB 콜백 함수 구현

**세부 목표**:
1. `tud_hid_get_report_cb()` 함수 구현
   - HID Instance 구분 (Instance 0: Keyboard, Instance 1: Mouse)
   - 각 인스턴스에 대한 리포트 전송 준비
2. `tud_hid_set_report_cb()` 함수 구현
   - Keyboard LED 상태 처리 (Caps Lock, Num Lock, Scroll Lock)
3. HID 리포트 메모리 할당 및 초기화
4. Endpoint 핸들링 기본 구조 작성

**참조 문서 및 섹션**:
- TinyUSB 문서: `tud_hid_get_report_cb()`, `tud_hid_set_report_cb()` 구현
- `docs/board/esp32s3-code-implementation-guide.md` §3.3 TinyUSB Composite 디바이스 구현

**검증**:
- [ ] `tud_hid_get_report_cb()` 함수 구현됨
- [ ] `tud_hid_set_report_cb()` 함수 구현됨
- [ ] Instance 0 (Keyboard)에 대한 분기 처리 확인
- [ ] Instance 1 (Mouse)에 대한 분기 처리 확인
- [ ] LED 상태 버퍼 선언됨 (`hid_keyboard_led_status`)
- [ ] 함수 서명과 TinyUSB 요구사항 일치
- [ ] `idf.py build` 성공 (컴파일 오류 없음)

---

### Phase 2.1.2: ESP32-S3 UART → HID 변환 로직 구현

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

**검증**:
- [ ] `src/board/BridgeOne/main/uart_handler.c` 파일 생성됨
- [ ] `src/board/BridgeOne/main/uart_handler.h` 파일 생성됨
- [ ] UART 초기화 함수 구현 (1Mbps, 8N1 설정, UART_NUM_0)
- [ ] `bridge_frame_t` 구조체 정의 (seq, buttons, deltaX/Y, wheel, modifiers, keyCode1/2)
- [ ] UART 수신 태스크 함수 구현 (`uart_task`)
- [ ] 순번 검증 함수 구현 (`validateSequenceNumber`)
- [ ] `src/board/BridgeOne/main/hid_handler.c` 파일 생성됨
- [ ] `src/board/BridgeOne/main/hid_handler.h` 파일 생성됨
- [ ] HID 태스크 함수 구현 (`hid_task`)
- [ ] `processBridgeFrame()` 함수 구현 (Keyboard/Mouse 분리)
- [ ] `sendKeyboardReport()` 함수 구현 (`tud_hid_n_report()` 호출)
- [ ] `sendMouseReport()` 함수 구현 (`tud_hid_n_report()` 호출)
- [ ] `idf.py build` 성공

---

#### Phase 2.1.2.1: UART 초기화 및 bridge_frame_t 구조체 정의

**목표**: UART 통신 채널 설정 및 BridgeOne 프로토콜 데이터 구조 정의

**세부 목표**:
1. `uart_handler.h` 헤더 파일 작성
2. `bridge_frame_t` 구조체 정의 (8바이트)
   - seq: 1바이트 (시퀀스 번호)
   - buttons: 1바이트 (마우스 버튼 비트)
   - deltaX: 1바이트 (X축 이동값, signed)
   - deltaY: 1바이트 (Y축 이동값, signed)
   - wheel: 1바이트 (휠 값, signed)
   - modifiers: 1바이트 (키보드 modifier 키)
   - keyCode1: 1바이트 (첫 번째 키코드)
   - keyCode2: 1바이트 (두 번째 키코드)
3. UART 상수 정의 (UART_NUM_0, 1Mbps, 8N1)
4. `uart_handler.c` 파일 작성
5. UART 초기화 함수 구현 (`uart_init()`)

**참조 문서 및 섹션**:
- `docs/technical-specification.md` §2.1 UART 통신 (Android ↔ ESP32-S3)
- `docs/board/esp32s3-code-implementation-guide.md` §1.3.3 BridgeOne UART 프로토콜 (고정)
- ESP-IDF 문서: UART Driver

**검증**:
- [ ] `src/board/BridgeOne/main/uart_handler.h` 파일 생성됨
- [ ] `src/board/BridgeOne/main/uart_handler.c` 파일 생성됨
- [ ] `bridge_frame_t` 구조체 정의됨 (정확히 8바이트)
- [ ] 모든 필드 타입 정확함 (seq, buttons, deltaX/Y/wheel, modifiers, keyCode1/2)
- [ ] UART 설정 상수 정의됨 (UART_NUM_0, BAUDRATE=1000000)
- [ ] `uart_init()` 함수 구현됨
  - gpio_set_direction() 호출
  - uart_param_config() 호출 (1Mbps, 8N1)
  - uart_driver_install() 호출 (버퍼 크기 할당)
- [ ] `idf.py build` 성공

---

#### Phase 2.1.2.2: UART 수신 태스크 및 프레임 검증 로직

**목표**: UART 수신 태스크 구현 및 프레임 검증 로직 작성

**세부 목표**:
1. UART 수신 태스크 함수 구현 (`uart_task()`)
   - 8바이트 프레임 수신
   - 타임아웃 처리
   - 순번 검증
   - 프레임 큐 전송
2. 순번 검증 함수 구현 (`validateSequenceNumber()`)
   - 예상 시퀀스 번호와 비교
   - 순환 처리 (0~255)
   - 프레임 손실 감지
3. 프레임 검증 로직 (크기, 체크섬 등)
4. 에러 처리 및 로그 출력

**참조 문서 및 섹션**:
- `docs/board/esp32s3-code-implementation-guide.md` §4.1 UART 통신 모듈 구현 (ESP-IDF)
- ESP-IDF 문서: FreeRTOS Task 생성 및 관리

**검증**:
- [ ] `uart_task()` 함수 구현됨
- [ ] `uart_read_bytes()` 호출로 8바이트 수신 로직 구현됨
- [ ] 타임아웃 설정됨 (예: 100ms)
- [ ] `validateSequenceNumber()` 함수 구현됨
- [ ] 시퀀스 번호 순환 처리 (0→255→0)
- [ ] 프레임 손실 감지 및 로그 출력
- [ ] 예외 상황 처리 (수신 오류, 타임아웃)
- [ ] FreeRTOS 큐에 프레임 전송 (`xQueueSend()`)
- [ ] 디버그 로그 출력 (수신한 프레임 정보)
- [ ] `idf.py build` 성공

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
   - `sendKeyboardReport()` 호출
   - `sendMouseReport()` 호출
5. `sendKeyboardReport()` 함수 구현
   - HID Keyboard 리포트 작성 (8바이트)
   - `tud_hid_n_report()` 호출 (Instance 0)
6. `sendMouseReport()` 함수 구현
   - HID Mouse 리포트 작성 (4바이트)
   - `tud_hid_n_report()` 호출 (Instance 1)

**참조 문서 및 섹션**:
- `docs/board/esp32s3-code-implementation-guide.md` §4.2 USB HID 모듈 구현 (ESP-IDF TinyUSB)
- `docs/board/esp32s3-code-implementation-guide.md` §3.4 BridgeOne 프레임 처리 로직
- TinyUSB 문서: `tud_hid_n_report()` 사용법

**검증**:
- [ ] `src/board/BridgeOne/main/hid_handler.h` 파일 생성됨
- [ ] `src/board/BridgeOne/main/hid_handler.c` 파일 생성됨
- [ ] `hid_task()` 함수 구현됨
- [ ] `xQueueReceive()` 호출로 프레임 수신
- [ ] `processBridgeFrame()` 함수 구현됨
- [ ] Keyboard/Mouse 필드 분리 로직 구현
- [ ] `sendKeyboardReport()` 함수 구현됨 (modifiers, keyCode1/2 포함)
- [ ] `sendMouseReport()` 함수 구현됨 (buttons, deltaX/Y, wheel 포함)
- [ ] `tud_hid_n_report()` 호출 (Instance 구분)
- [ ] 에러 처리 (USB 연결 해제 시)
- [ ] 디버그 로그 출력 (리포트 전송 정보)
- [ ] `idf.py build` 성공

---

### Phase 2.1.3: ESP32-S3 FreeRTOS 태스크 구조 구현

**목표**: 듀얼 코어 활용 FreeRTOS 멀티태스크 시스템 구축

**세부 목표**:
1. `main.c` 파일의 `app_main()` 함수 수정
2. TinyUSB 초기화 (`tud_init()`)
3. UART 초기화 호출
4. 프레임 큐 생성 (`xQueueCreate`)
5. UART 태스크 생성 (Core 0, 우선순위 10)
6. HID 태스크 생성 (Core 0, 우선순위 9)
7. USB 태스크 생성 (Core 1, 우선순위 5) - `tud_task()` 호출
8. 태스크 워치독 타이머(TWDT) 설정

**참조 문서 및 섹션**:
- `docs/board/esp32s3-code-implementation-guide.md` §3.5 FreeRTOS 태스크 구조
- `docs/board/esp32s3-code-implementation-guide.md` §3.6 데이터 플로우 설계
- `docs/board/esp32s3-code-implementation-guide.md` §4.2 USB HID 모듈 구현

**검증**:
- [ ] `main.c`의 `app_main()` 함수에서 `tud_init(BOARD_TUD_RHPORT)` 호출
- [ ] `uart_init()` 함수 호출
- [ ] 프레임 큐 생성 (`xQueueCreate(32, sizeof(bridge_frame_t))`)
- [ ] UART 태스크 생성 (`xTaskCreatePinnedToCore(uart_task, "UART", 4096, NULL, 10, NULL, 0)`)
- [ ] HID 태스크 생성 (`xTaskCreatePinnedToCore(hid_task, "HID", 4096, NULL, 9, NULL, 0)`)
- [ ] USB 태스크 생성 (`xTaskCreatePinnedToCore(usb_task, "USB", 4096, NULL, 5, NULL, 1)`)
- [ ] 각 태스크에서 `esp_task_wdt_reset()` 호출 확인
- [ ] `sdkconfig`에 `CONFIG_ESP_TASK_WDT=y` 설정 확인
- [ ] `main/CMakeLists.txt`에 모든 소스 파일 등록 (main.c, usb_descriptors.c, uart_handler.c, hid_handler.c)
- [ ] `idf.py build` 성공

---

### Phase 2.1.4: ESP32-S3 펌웨어 플래싱 및 HID 디바이스 인식 검증

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

**검증**:
- [ ] `idf.py build` 성공 (메시지: "BUILD SUCCESSFUL")
- [ ] `build/BridgeOne.bin` 파일 생성됨
- [ ] 플래싱 성공 ("Hash of data verified" 메시지 표시)
- [ ] 시리얼 모니터에서 "TinyUSB initialized" 또는 "USB initialized" 메시지 확인
- [ ] 시리얼 모니터에서 "UART task started" 메시지 확인
- [ ] 시리얼 모니터에서 "HID task started" 메시지 확인
- [ ] 시리얼 모니터에서 "USB task started" 메시지 확인
- [ ] Windows Device Manager에서 "USB Input Device" 2개 확인 (Keyboard, Mouse)
- [ ] 드라이버 오류 없음 (노란색 느낌표 미표시)
- [ ] PowerShell 명령 실행: `Get-PnpDevice | Where-Object {$_.DeviceID -match "VID_303A"} | Format-Table FriendlyName, Status, DeviceID`로 2개 디바이스 확인 (Status: OK)

---

### Phase 2.1.5: Android 앱 8바이트 프레임 생성 로직 구현

**목표**: 터치 입력을 8바이트 BridgeOne 프레임으로 변환하는 로직 구현

**세부 목표**:
1. `com.bridgeone.app.protocol` 패키지 생성
2. `BridgeFrame.kt` 데이터 클래스 정의 (8바이트)
3. `FrameBuilder.kt` 클래스 구현 (터치 → 프레임 변환)
4. 순번 카운터 관리 (0~255 순환)
5. Little-Endian 바이트 직렬화 구현 (`toByteArray()` 메서드)
6. 단위 테스트 작성 (`BridgeFrameTest.kt`)

**참조 문서 및 섹션**:
- `docs/android/technical-specification-app.md` §1.2.1 BridgeOne 프로토콜 명세
- `docs/android/technical-specification-app.md` §1.2.2 프레임 생성 요구사항
- `docs/technical-specification.md` §2.1 UART 통신 (Android ↔ ESP32-S3)
- `docs/board/esp32s3-code-implementation-guide.md` §1.3.3 BridgeOne UART 프로토콜 (고정)

**검증**:
- [ ] `src/android/app/src/main/java/com/bridgeone/app/protocol/` 디렉터리 생성됨
- [ ] `BridgeFrame.kt` 파일 생성됨
- [ ] `BridgeFrame` 데이터 클래스 8바이트 구조 정의 (seq: UByte, buttons: UByte, deltaX: Byte, deltaY: Byte, wheel: Byte, modifiers: UByte, keyCode1: UByte, keyCode2: UByte)
- [ ] `FrameBuilder.kt` 파일 생성됨
- [ ] `toByteArray(): ByteArray` 메서드 구현 (Little-Endian 직렬화)
- [ ] 순번 카운터 순환 로직 구현 (0→255→0)
- [ ] 단위 테스트 파일 `BridgeFrameTest.kt` 생성됨
- [ ] 단위 테스트 통과 (프레임 크기, 바이트 순서, 순번 순환 검증)
- [ ] Gradle 빌드 성공

---

### Phase 2.1.6: Android 앱 USB Serial 통신 구현

**목표**: USB Serial 라이브러리를 사용한 ESP32-S3와의 UART 통신 구현

**개발 기간**: 3-4일

**세부 목표**:
1. `usb-serial-for-android` 라이브러리 의존성 추가 (`libs.versions.toml`)
2. `com.bridgeone.app.usb` 패키지 생성
3. `UsbSerialManager.kt` 싱글톤 클래스 구현
4. ESP32-S3 디바이스 자동 감지 (VID: 0x303A)
5. USB 권한 요청 로직 구현
6. 1Mbps, 8N1 통신 설정
7. 프레임 전송 함수 구현 (`sendFrame()`)
8. 연결 상태 모니터링 (`BroadcastReceiver`)

**참조 문서 및 섹션**:
- `docs/android/technical-specification-app.md` §1.1 통신 아키텍처 설계
- `docs/android/technical-specification-app.md` §1.6 오류 처리 및 복구 설계
- `docs/technical-specification.md` §5.2 usb-serial-for-android 프로젝트 분석
- Phase 1.1.2 Jetpack Compose 및 핵심 의존성 설정

**검증**:
- [ ] `gradle/libs.versions.toml`에 `usb-serial-for-android = "3.7.3"` 추가
- [ ] `app/build.gradle.kts`에 `implementation(libs.usb.serial.for.android)` 의존성 추가됨
- [ ] Gradle 동기화 성공 (라이브러리 다운로드 완료)
- [ ] `src/android/app/src/main/java/com/bridgeone/app/usb/` 디렉터리 생성됨
- [ ] `UsbSerialManager.kt` 파일 생성됨 (object 싱글톤)
- [ ] ESP32-S3 VID 필터 구현 (VID: 0x303A)
- [ ] USB 권한 요청 로직 구현 (`requestPermission()`)
- [ ] 1Mbps 통신 파라미터 설정 구현 (`setParameters(1000000, 8, 1, 0)`)
- [ ] `sendFrame(frame: BridgeFrame)` 함수 구현
- [ ] `BroadcastReceiver` 등록 (USB_DEVICE_ATTACHED/DETACHED)
- [ ] Gradle 빌드 성공

---

#### Phase 2.1.6.1: 라이브러리 의존성 추가 및 기본 구조 작성

**목표**: usb-serial-for-android 라이브러리 추가 및 UsbSerialManager 싱글톤 클래스 기본 구조 작성

**세부 목표**:
1. `gradle/libs.versions.toml`에 usb-serial-for-android 라이브러리 의존성 추가
2. `app/build.gradle.kts`에 라이브러리 import 추가
3. Gradle 동기화 및 라이브러리 다운로드 확인
4. `com.bridgeone.app.usb` 패키지 생성
5. `UsbSerialManager.kt` 싱글톤 클래스 생성
6. 기본 데이터 멤버 및 프로퍼티 정의
   - UsbManager 인스턴스
   - UsbSerialPort 인스턴스
   - 연결 상태 플래그
7. Android Manifest 권한 추가 (USB_DEVICE_ATTACH, USB_DEVICE_DETACH)

**참조 문서 및 섹션**:
- `docs/technical-specification.md` §5.2 usb-serial-for-android 프로젝트 분석
- Phase 1.1.2 Jetpack Compose 및 핵심 의존성 설정
- usb-serial-for-android 공식 문서

**검증**:
- [ ] `gradle/libs.versions.toml`에 `usb-serial-for-android = "3.7.3"` 버전 추가됨
- [ ] `app/build.gradle.kts`에 `implementation(libs.usb.serial.for.android)` 선언됨
- [ ] `gradle sync` 성공 (라이브러리 다운로드 완료)
- [ ] `src/android/app/src/main/java/com/bridgeone/app/usb/` 디렉터리 생성됨
- [ ] `UsbSerialManager.kt` 파일 생성됨
- [ ] `object UsbSerialManager` 싱글톤 선언됨
- [ ] UsbManager, UsbSerialPort 멤버 변수 선언됨
- [ ] AndroidManifest.xml에 권한 추가됨
- [ ] Gradle 빌드 성공 (컴파일 오류 없음)

---

#### Phase 2.1.6.2: ESP32-S3 자동 감지 및 USB 권한 요청

**목표**: ESP32-S3 디바이스 자동 감지 및 USB 권한 요청 로직 구현

**세부 목표**:
1. VID/PID 상수 정의 (VID: 0x303A, PID: 0x0109)
2. `findEsp32s3Device()` 함수 구현
   - UsbManager에서 사용 가능한 디바이스 열거
   - VID 필터링 (0x303A)
   - 첫 번째 일치 디바이스 반환
3. `requestPermission()` 함수 구현
   - PendingIntent 생성 (USB_DEVICE_ATTACHED)
   - requestPermission() 호출
   - Context를 통한 권한 요청
4. 권한 콜백 처리 (`onPermissionResult()`)
   - 권한 승인 시 포트 열기
   - 권한 거부 시 에러 처리
5. Intent 필터 정의 (AndroidManifest.xml)

**참조 문서 및 섹션**:
- usb-serial-for-android 공식 문서: Device Discovery
- Android 문서: USB Device Discovery
- `docs/technical-specification.md` §2.3.1 ESP32-S3-DevkitC-1-N16R8 (VID/PID)

**검증**:
- [ ] VID/PID 상수 정의됨 (VID=0x303A)
- [ ] `findEsp32s3Device()` 함수 구현됨
- [ ] `requestPermission()` 함수 구현됨
- [ ] PendingIntent 생성 확인
- [ ] Permission 콜백 핸들러 구현됨
- [ ] AndroidManifest.xml에 intent-filter 추가됨
- [ ] 권한 거부 시 에러 메시지 처리
- [ ] Gradle 빌드 성공

---

#### Phase 2.1.6.3: UART 통신 설정 및 프레임 전송

**목표**: 1Mbps 8N1 통신 설정 및 BridgeFrame 전송 함수 구현

**세부 목표**:
1. `openPort()` 함수 구현
   - UsbSerialPort 객체 생성
   - UsbDeviceConnection 오픈
   - 1Mbps, 8N1, no parity 설정 (`setParameters(1000000, 8, 1, 0)`)
2. `sendFrame(frame: BridgeFrame)` 함수 구현
   - BridgeFrame을 ByteArray로 변환 (`frame.toByteArray()`)
   - write() 호출로 전송
   - 전송 완료 확인 (return 값 체크)
   - 에러 처리 및 로그
3. `closePort()` 함수 구현
   - USB 연결 종료
   - 리소스 해제
4. 연결 상태 확인 함수 (`isConnected()`)
5. 에러 처리 및 재연결 로직

**참조 문서 및 섹션**:
- usb-serial-for-android 공식 문서: Serial Communication
- `docs/android/technical-specification-app.md` §1.6 오류 처리 및 복구 설계
- `docs/technical-specification.md` §2.1 UART 통신 (Android ↔ ESP32-S3)

**검증**:
- [ ] `openPort()` 함수 구현됨
- [ ] `setParameters(1000000, 8, 1, 0)` 호출 확인
- [ ] `sendFrame()` 함수 구현됨
- [ ] frame.toByteArray() 호출로 바이트 변환
- [ ] write() 호출로 UART 전송
- [ ] 반환값 체크 (전송 바이트 수 확인)
- [ ] `closePort()` 함수 구현됨
- [ ] `isConnected()` 함수 구현됨
- [ ] 예외 처리 (IOException, USB 연결 해제 시)
- [ ] 디버그 로그 출력 (프레임 전송 정보)
- [ ] Gradle 빌드 성공

---

### Phase 2.1.7: Android 앱 터치 입력 → 프레임 변환 로직 구현

**목표**: TouchpadWrapper 컴포넌트에서 터치 이벤트를 8바이트 프레임으로 변환

**개발 기간**: 4-5일

**세부 목표**:
1. `ui/components/TouchpadWrapper.kt` Composable 함수 생성
2. 터치 이벤트 리스너 구현 (`Modifier.pointerInput()`)
3. 터치 좌표 → 마우스 델타 변환 알고리즘 구현
4. 데드존 보상 알고리즘 구현 (15dp 임계값)
5. 클릭 감지 로직 구현 (500ms, 15dp 임계값)
6. 프레임 생성 및 전송 연동
7. 기본 UI 구성 (1:2 비율, 둥근 모서리)

**참조 문서 및 섹션**:
- `docs/android/technical-specification-app.md` §2.2 터치패드 알고리즘
- `docs/android/technical-specification-app.md` §2.2.2 터치패드 핵심 알고리즘: 자유 이동 알고리즘 명세
- `docs/android/technical-specification-app.md` §2.2.4 데드존 보상 알고리즘 명세
- `docs/android/component-touchpad.md`

**검증**:
- [ ] `src/android/app/src/main/java/com/bridgeone/app/ui/components/TouchpadWrapper.kt` 파일 생성됨
- [ ] `Modifier.pointerInput()` 구현 (ACTION_DOWN, ACTION_MOVE, ACTION_UP 처리)
- [ ] 터치 좌표 델타 계산 로직 구현 (`delta = current_pos - previous_pos`)
- [ ] 데드존 보상 알고리즘 구현 (DEAD_ZONE_THRESHOLD=15dp)
- [ ] 클릭 판정 로직 구현 (CLICK_MAX_DURATION=500ms, 총 이동거리 15dp 이내)
- [ ] `FrameBuilder` 연동하여 프레임 생성
- [ ] `UsbSerialManager.sendFrame()` 호출
- [ ] 1:2 비율 터치패드 UI 구성 (최소 160dp×320dp)
- [ ] 둥근 모서리 적용 (터치패드 너비의 3%)
- [ ] Compose Preview 정상 렌더링
- [ ] Gradle 빌드 성공

---

#### Phase 2.1.7.1: 기본 UI 및 터치 이벤트 감지

**목표**: TouchpadWrapper 기본 UI 구성 및 터치 이벤트 감지 로직 구현

**세부 목표**:
1. `TouchpadWrapper.kt` Composable 함수 생성
2. 기본 UI 구성
   - 1:2 비율 직사각형 (너비: 160dp 이상)
   - 둥근 모서리 (너비의 3%)
   - 배경색 (어두운 회색 또는 테마 색상)
3. 터치 이벤트 감지 (`Modifier.pointerInput()`)
   - PointerEventPass.Main 사용
   - ACTION_DOWN 처리 (터치 시작)
   - ACTION_MOVE 처리 (드래그 중)
   - ACTION_UP 처리 (터치 종료)
4. 터치 좌표 저장 (이전 좌표, 현재 좌표)
5. State 관리
   - mutableStateOf()로 터치 상태 관리
   - 이벤트 핸들러 콜백 정의

**참조 문서 및 섹션**:
- `docs/android/component-touchpad.md` (UI 스타일)
- Jetpack Compose 문서: Modifier.pointerInput()
- `docs/android/technical-specification-app.md` §2.2.1 터치패드 UI

**검증**:
- [ ] `src/android/app/src/main/java/com/bridgeone/app/ui/components/TouchpadWrapper.kt` 파일 생성됨
- [ ] Composable 함수 선언됨
- [ ] Box 또는 Surface로 기본 UI 구성
- [ ] 1:2 비율 적용됨 (width:height = 1:2)
- [ ] 최소 크기 160dp×320dp 확인
- [ ] 둥근 모서리 적용됨 (`cornerRadius = width * 0.03`)
- [ ] Modifier.pointerInput() 구현됨
- [ ] PointerEventPass.Main 사용 확인
- [ ] onPointerEvent 콜백 구현
- [ ] 터치 좌표 저장 로직 구현 (previous, current)
- [ ] Preview 함수 작성 및 렌더링 확인
- [ ] Gradle 빌드 성공

---

#### Phase 2.1.7.2: 델타 계산 및 데드존 보상 알고리즘

**목표**: 터치 좌표 델타 계산 및 데드존 보상 알고리즘 구현

**세부 목표**:
1. 델타 계산 함수 구현 (`calculateDelta()`)
   - 현재 좌표 - 이전 좌표
   - 화면 밀도 고려 (dp → pixel 변환)
   - X, Y 축 분리
2. 데드존 보상 알고리즘 구현 (`applyDeadZone()`)
   - DEAD_ZONE_THRESHOLD = 15dp (상수)
   - 델타 크기 < 임계값 → 0 반환
   - 델타 크기 ≥ 임계값 → 정규화 적용
   - 스넵 감도 조정 가능하도록 설정
3. 델타 값 범위 정규화 (-127 ~ 127)
4. 로그 출력 (디버그용)
   - 원본 델타 값
   - 데드존 적용 후 델타 값

**참조 문서 및 섹션**:
- `docs/android/technical-specification-app.md` §2.2.4 데드존 보상 알고리즘 명세
- `docs/android/technical-specification-app.md` §2.2.2 터치패드 핵심 알고리즘: 자유 이동 알고리즘 명세

**검증**:
- [ ] `calculateDelta()` 함수 구현됨
- [ ] 좌표 계산 로직 정확 (current - previous)
- [ ] dp → pixel 변환 고려됨 (LocalDensity 사용)
- [ ] X, Y 축 분리 처리됨
- [ ] `applyDeadZone()` 함수 구현됨
- [ ] DEAD_ZONE_THRESHOLD = 15dp 정의됨
- [ ] 임계값 이하 → 0 처리
- [ ] 임계값 초과 → 정규화 적용
- [ ] 델타 범위 정규화 (-127 ~ 127)
- [ ] 스넵 감도 상수 정의됨 (튜닝 가능)
- [ ] 디버그 로그 출력 (원본/적용 후 값)
- [ ] Gradle 빌드 성공

---

#### Phase 2.1.7.3: 클릭 감지 및 프레임 생성/전송

**목표**: 클릭 감지 로직 구현 및 프레임 생성/전송 연동

**세부 목표**:
1. 클릭 감지 로직 구현 (`detectClick()`)
   - CLICK_MAX_DURATION = 500ms (상수)
   - CLICK_MAX_MOVEMENT = 15dp (상수)
   - 터치 시작 시간 기록
   - 터치 종료 시간 계산
   - 총 이동 거리 계산
   - 시간 & 거리 모두 임계값 이내 → 클릭 판정
2. 버튼 상태 결정 함수 (`getButtonState()`)
   - 클릭 감지 시 LEFT_CLICK 비트 설정
   - 드래그 중 유지 또는 릴리스
3. 프레임 생성 함수 (`createFrame()`)
   - FrameBuilder 사용
   - deltaX, deltaY 설정
   - buttons 설정 (클릭 여부)
   - seq, modifiers, keyCode 초기화
4. USB Serial 전송 연동 (`sendFrame()`)
   - UsbSerialManager.sendFrame() 호출
   - 비동기 처리 (coroutine 또는 thread)
   - 전송 오류 처리
5. 상태 초기화
   - 터치 종료 시 상태 초기화
   - deltaX/Y, buttons 리셋

**참조 문서 및 섹션**:
- `docs/android/technical-specification-app.md` §2.2.3 클릭 판정 알고리즘
- `docs/android/technical-specification-app.md` §1.2 BridgeOne 프로토콜 명세
- BridgeFrame 데이터 클래스 (Phase 2.1.5)

**검증**:
- [ ] `detectClick()` 함수 구현됨
- [ ] CLICK_MAX_DURATION = 500ms 정의됨
- [ ] CLICK_MAX_MOVEMENT = 15dp 정의됨
- [ ] 터치 시작 시간 기록 (ACTION_DOWN)
- [ ] 터치 종료 시 경과시간 계산
- [ ] 총 이동 거리 계산 로직 구현
- [ ] 클릭 판정 로직 정확 (시간 & 거리 AND 조건)
- [ ] `getButtonState()` 함수 구현됨
- [ ] LEFT_CLICK 비트 처리 (0x01)
- [ ] `createFrame()` 함수 구현됨
- [ ] FrameBuilder 연동 확인
- [ ] seq, buttons, deltaX/Y 설정됨
- [ ] `sendFrame()` 함수 구현됨
- [ ] UsbSerialManager.sendFrame() 호출 확인
- [ ] 비동기 처리 (LaunchedEffect 또는 viewModelScope)
- [ ] 오류 처리 및 로그 출력
- [ ] 상태 초기화 로직 구현
- [ ] Compose Preview 렌더링 확인
- [ ] Gradle 빌드 성공

---

### Phase 2.1.8: HID Boot Mouse 통신 End-to-End 검증

**목표**: Android 터치 입력 → ESP32-S3 → Windows 마우스 이동 전체 경로 검증

**유저 사전 작업** (LLM 실행 전 필수):
1. Android 기기를 ESP32-S3 USB-UART 포트에 USB-C 케이블로 연결
2. ESP32-S3 USB-OTG 포트를 Windows PC에 USB 케이블로 연결
3. Android 스마트폰에 충전기 연결 (5V 2A 이상)
4. Android 앱 빌드 및 설치
5. Android 앱 실행

**LLM 검증 작업**:
1. ESP32-S3 시리얼 모니터 시작 (`idf.py -p COMx monitor`)
2. UART 프레임 수신 로그 확인
3. HID Mouse 리포트 전송 로그 확인
4. Windows에서 마우스 포인터 이동 확인 (사용자 보고 기반)

**참조 문서 및 섹션**:
- `docs/board/esp32s3-code-implementation-guide.md` §9.1 ESP-IDF 기반 성능 목표
- `docs/technical-specification.md` §3.2 성능 임계값

**검증**:
- [ ] Android 앱에서 터치패드 드래그 시 Windows 마우스 포인터 이동 확인 (사용자 보고)
- [ ] ESP32-S3 시리얼 로그에 "UART frame received: seq=X buttons=X deltaX=X deltaY=X" 메시지 표시
- [ ] ESP32-S3 시리얼 로그에 "HID Mouse report sent: buttons=X deltaX=X deltaY=X wheel=X" 메시지 표시
- [ ] 마우스 이동 방향이 터치 제스처 방향과 일치
- [ ] 마우스 이동 속도가 터치 제스처 속도와 비례
- [ ] 클릭 제스처 (500ms 이내 탭) → Windows 좌클릭 동작 확인
- [ ] 지연시간 50ms 이하 (사용자 체감 테스트)
- [ ] 프레임 손실 없음 (시리얼 로그에서 순번 카운터 연속성 확인: 0→1→2→...→255→0)

---

### Phase 2.1.9: HID Boot Keyboard 통신 End-to-End 검증

**목표**: Android 키보드 버튼 입력 → ESP32-S3 → Windows 키 입력 전체 경로 검증

**세부 목표**:
1. `ui/components/KeyboardKeyButton.kt` Composable 생성
2. 키 다운/업 이벤트 → 프레임 생성 로직 구현
3. ESP32-S3에서 HID Keyboard 리포트 전송 확인
4. Windows에서 키 입력 인식 확인
5. BIOS 호환성 검증 (Del 키 테스트)

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

### Phase 2.1.10: HID 통합 검증 및 스트레스 테스트

**목표**: 마우스 + 키보드 동시 입력 및 장시간 안정성 검증

**검증 시나리오**:
1. 마우스 이동 + 클릭 동시 입력
2. 키보드 타이핑 + 마우스 조작 동시 입력
3. 4시간 연속 사용 안정성 테스트
4. 프레임 손실률 측정

**참조 문서 및 섹션**:
- `docs/board/esp32s3-code-implementation-guide.md` §9 성능 벤치마크 및 품질 기준
- `docs/board/esp32s3-code-implementation-guide.md` §9.2 ESP-IDF 기반 품질 검증
- `docs/technical-specification.md` §3.2 성능 임계값

**검증**:
- [ ] 마우스 이동 중 클릭 정상 동작 (동시 입력 테스트)
- [ ] 키보드 타이핑 중 마우스 이동 정상 동작 (동시 입력 테스트)
- [ ] 4시간 연속 사용 시 크래시 없음 (ESP32-S3, Android 앱 모두)
- [ ] 프레임 손실률 < 0.1% (시리얼 로그 순번 카운터 분석)
- [ ] 평균 지연시간 < 50ms (사용자 체감 + 로그 타임스탬프 분석)
- [ ] ESP32-S3 메모리 사용량 안정 (힙 크기 일정 유지, `esp_get_free_heap_size()` 로그 확인)
- [ ] ESP32-S3 CPU 사용률 < 30% (`vTaskGetRunTimeStats()` 로그 확인)
- [ ] Windows에서 입력 오류 없음 (마우스/키보드 정상 작동)
- [ ] Android 앱 배터리 소모 정상 범위 (4시간 사용 시 배터리 20% 이하 소모)

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
- Phase 2.1.3: ESP32-S3 FreeRTOS 태스크 구조 구현
- Phase 2.1.4: ESP32-S3 펌웨어 플래싱 및 HID 디바이스 인식 검증
- Phase 2.1.5: Android 앱 8바이트 프레임 생성 로직 구현
- Phase 2.1.6: Android 앱 USB Serial 통신 구현
  - Phase 2.1.6.1: 라이브러리 의존성 추가 및 기본 구조 작성
  - Phase 2.1.6.2: ESP32-S3 자동 감지 및 USB 권한 요청
  - Phase 2.1.6.3: UART 통신 설정 및 프레임 전송
- Phase 2.1.7: Android 앱 터치 입력 → 프레임 변환 로직 구현
  - Phase 2.1.7.1: 기본 UI 및 터치 이벤트 감지
  - Phase 2.1.7.2: 델타 계산 및 데드존 보상 알고리즘
  - Phase 2.1.7.3: 클릭 감지 및 프레임 생성/전송
- Phase 2.1.8: HID Boot Mouse 통신 End-to-End 검증
- Phase 2.1.9: HID Boot Keyboard 통신 End-to-End 검증
- Phase 2.1.10: HID 통합 검증 및 스트레스 테스트

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

---

