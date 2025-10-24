---
title: "BridgeOne Phase 2: 통신 안정화"
description: "BridgeOne 프로젝트 Phase 2 - Android ↔ Board ↔ Windows 간 HID/CDC 통신 완전 안정화 (5-6주)"
tags: ["android", "esp32-s3", "devkitc-1", "windows", "communication", "hid", "cdc", "uart", "vibe-coding"]
version: "v1.0"
owner: "Chatterbones"
updated: "2025-10-23"
---

# BridgeOne Phase 2: 통신 안정화

**목표**: Android ↔ Board ↔ Windows 간 HID/CDC 통신 완전 안정화

**검증 전략**: 각 Phase별 개별 검증 + 최종 통합 검증

**핵심 목표**: HID 키보드/마우스 입력 + Vendor CDC JSON 쌍방향 통신 완벽 검증

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

## Phase 2.1: Android ↔ ESP32-S3 UART 통신

**목표**: Android 앱과 ESP32-S3 간 1Mbps UART 통신 구현 및 8바이트 델타 프레임 송수신 검증

#### Phase 2.1.1: Android 로깅 인프라 구축

**세부 목표**:
1. Logcat 기반 구조화된 로깅 시스템 구현
2. UART 통신 디버깅용 로그 레벨 정의 (VERBOSE, DEBUG, INFO, ERROR)
3. 프레임 송수신 로그 포맷 정의 (HEX dump + 타임스탬프)
4. 로그 필터링 및 검색 가이드 작성

**구현 파일**:
- `app/src/main/java/com/bridgeone/app/utils/Logger.kt`: 구조화된 로깅 유틸리티
- `app/src/main/java/com/bridgeone/app/utils/HexDump.kt`: HEX dump 포맷터

**참조 문서**:
- `docs/android/technical-specification-app.md` §1.1 통신 아키텍처 설계

**검증**:
- [ ] VERBOSE, DEBUG, INFO, ERROR 로그 레벨 구분 표시
- [ ] HEX dump 포맷으로 프레임 내용 출력 확인
- [ ] 타임스탬프가 밀리초 단위로 정확히 표시됨
- [ ] Logcat 필터로 UART 관련 로그만 검색 가능
- [ ] **로그 출력 예시 확인**:
  ```
  2025-10-23 12:34:56.789 V/UART: [RX] 8 bytes: 2A 01 0A FB 02 02 04 00
  2025-10-23 12:34:56.791 D/Frame: seq=42 btn=0x01 dx=10 dy=-5 wh=2 mod=0x02 k1=0x04 k2=0x00
  2025-10-23 12:34:56.792 I/Protocol: Frame processed successfully
  ```
- [ ] **Logcat 필터 명령어 테스트**:
  ```bash
  adb logcat -s UART:V Frame:D Protocol:I
  adb logcat | grep -E "UART|Frame|Protocol"
  ```
- [ ] HEX dump 포맷터 정확성: 8바이트를 "XX XX XX XX XX XX XX XX" 형식으로 출력

---

#### Phase 2.1.2: Android USB Serial 라이브러리 통합

**세부 목표**:
1. `usb-serial-for-android` 라이브러리 추가 (3.9.0+)
2. `app/build.gradle.kts`에 의존성 등록
3. USB 권한 요청 로직 구현
4. ESP32-S3 VID/PID 필터링 (VID: 0x303A)

**구현 파일**:
- `app/build.gradle.kts`: USB Serial 라이브러리 의존성
- `app/src/main/java/com/bridgeone/app/usb/UsbConnectionManager.kt`: USB 연결 관리
- `app/src/main/java/com/bridgeone/app/usb/DeviceFilter.kt`: VID/PID 필터

**참조 문서**:
- `docs/android/technical-specification-app.md` §1.1.1 하드웨어 연결 요구사항
- `docs/technical-specification.md` §5.2 usb-serial-for-android 프로젝트 분석

**검증**:
- [ ] USB Serial 라이브러리가 External Libraries에 표시됨
- [ ] ESP32-S3 연결 시 USB 권한 요청 대화상자 표시
- [ ] VID 0x303A 장치만 필터링되어 인식됨
- [ ] 장치 연결/해제 이벤트가 정상적으로 감지됨

---

#### Phase 2.1.3: UART 연결 관리 구현

**세부 목표**:
1. 1Mbps, 8N1 통신 파라미터 설정
2. `SerialInputOutputManager` 초기화
3. 연결 상태 관리 (`TransportState`: NoTransport → UsbOpening → UsbReady)
4. 연결/해제 이벤트 핸들러 구현

**구현 파일**:
- `app/src/main/java/com/bridgeone/app/usb/UartConnection.kt`: UART 통신 설정
- `app/src/main/java/com/bridgeone/app/state/TransportStateManager.kt`: 연결 상태 관리

**참조 문서**:
- `docs/android/technical-specification-app.md` §1.1.2 연결 관리 요구사항
- `docs/technical-specification.md` §3.1 HID 처리 관련 상수 (UART_BAUD_RATE: 1000000)

**검증**:
- [ ] 1Mbps 통신 속도로 포트 열기 성공
- [ ] `TransportState`가 NoTransport → UsbOpening → UsbReady 순서로 전환
- [ ] 연결/해제 이벤트 로그 출력 확인
- [ ] 로그에서 UART 통신 파라미터 확인 (1000000bps, 8N1)

---

#### Phase 2.1.4: 8바이트 델타 프레임 인코딩 구현

**세부 목표**:
1. `BridgeFrame` 데이터 클래스 정의 (8바이트 구조)
2. 순번 카운터 관리 (0~255 순환)
3. 마우스/키보드 입력을 프레임으로 인코딩
4. Little-Endian 바이트 순서 처리 및 검증

**구현 파일**:
- `app/src/main/java/com/bridgeone/app/protocol/BridgeFrame.kt`: 프레임 데이터 구조
- `app/src/main/java/com/bridgeone/app/protocol/FrameEncoder.kt`: 프레임 인코딩 로직

**프레임 구조** (`docs/technical-specification.md` §2.4.6):
```kotlin
data class BridgeFrame(
    val seq: UByte,        // [0] 순번 (0~255 순환)
    val buttons: UByte,    // [1] 마우스 버튼 (bit0=L, bit1=R, bit2=M)
    val deltaX: Byte,      // [2] X축 이동 (-127~127)
    val deltaY: Byte,      // [3] Y축 이동 (-127~127)
    val wheel: Byte,       // [4] 휠 스크롤 (-127~127)
    val modifiers: UByte,  // [5] 키보드 모디파이어
    val keyCode1: UByte,   // [6] 키 코드 1
    val keyCode2: UByte    // [7] 키 코드 2
)
```

**참조 문서**:
- `docs/android/technical-specification-app.md` §1.2.1 BridgeOne 프로토콜 명세
- `docs/board/esp32s3-code-implementation-guide.md` §1.3.3 BridgeOne UART 프로토콜

**검증**:
- [ ] `BridgeFrame` 클래스가 정확히 8바이트로 직렬화됨
- [ ] 순번 카운터가 0→255→0 순환하며 증가
- [ ] 마우스 이동 입력이 deltaX/deltaY 필드에 정확히 인코딩됨
- [ ] 키보드 입력이 modifiers/keyCode1/keyCode2 필드에 정확히 인코딩됨
- [ ] **Little-Endian 바이트 순서 구체적 검증**:
  - 테스트 프레임: `deltaX=10 (0x0A)`, `deltaY=-5 (0xFB)` 생성
  - HEX dump 분석: `[seq] [btn] [0A] [FB] [wheel] [mod] [k1] [k2]`
  - signed 값 검증: `deltaY=-5` → `0xFB` (2's complement) 확인
  - 로그 출력 예시: `[UART TX] seq=42 btn=01 dx=0A dy=FB wh=02 mod=02 k1=04 k2=00`

---

#### Phase 2.1.5: UART 프레임 송신 구현 및 테스트

**세부 목표**:
1. 프레임 송신 함수 구현 (`sendFrame()`)
2. 링 버퍼 관리 (16프레임)
3. 송신 큐 오버플로우 처리
4. 로그 기반 송신 검증 (100개 연속 프레임 테스트)

**구현 파일**:
- `app/src/main/java/com/bridgeone/app/protocol/FrameTransmitter.kt`: 프레임 송신

**참조 문서**:
- `docs/android/technical-specification-app.md` §1.2.2 프레임 생성 요구사항
- `docs/technical-specification.md` §3.1 UART_BUFFER_SIZE: 128 byte (16프레임)

**검증**:
- [ ] 테스트 프레임 송신 성공 (로그 확인)
- [ ] 링 버퍼가 정상적으로 동작 (버퍼 오버플로우 없음)
- [ ] 연속 100개 프레임 송신 시 오류 없음
- [ ] HEX dump 로그로 송신 데이터 확인 가능
- [ ] **성능 기준선 측정 (Android UART 송신)**:
  - 100개 프레임 연속 송신 시간 측정
  - 평균 프레임당 송신 시간 계산 (목표: < 1ms)
  - 처리량 계산: frames/sec (목표: ≥ 1000 fps)
  - 기준선 기록: "Baseline Android TX: 0.8ms/frame, 1250 fps"
  - Logcat 타임스탬프 분석으로 프레임 간격 측정

---

#### Phase 2.1.6: UART 프레임 수신 구현 및 파싱

**세부 목표**:
1. 프레임 수신 핸들러 구현
2. 8바이트 프레임 추출 로직
3. 순번 카운터 검증 (패킷 손실 감지)
4. 수신 응답 로깅

**구현 파일**:
- `app/src/main/java/com/bridgeone/app/protocol/FrameReceiver.kt`: 프레임 수신

**참조 문서**:
- `docs/android/technical-specification-app.md` §1.2.2 프레임 생성 요구사항

**검증**:
- [ ] ESP32-S3에서 보낸 응답 프레임 수신 성공
- [ ] 순번 카운터로 패킷 손실 감지 확인
- [ ] 수신 프레임 HEX dump 로그 출력
- [ ] 불완전 프레임 처리 (8바이트 미만 거부)

---

#### Phase 2.1.7: UART 오류 복구 메커니즘 구현

**세부 목표**:
1. 자동 재연결 로직 구현 (최대 3회)
2. 연결 끊김 감지 및 복구
3. 타임아웃 처리 (3초)
4. 오류 상태 UI 피드백 연동

**구현 파일**:
- `app/src/main/java/com/bridgeone/app/usb/ConnectionRecovery.kt`: 오류 복구 로직
- `app/src/main/java/com/bridgeone/app/state/ErrorStateManager.kt`: 오류 상태 관리

**참조 문서**:
- `docs/android/technical-specification-app.md` §1.1.2 연결 관리 요구사항

**검증**:
- [ ] 연결 끊김 감지 시 자동 재연결 시도 (최대 3회)
- [ ] 3초 타임아웃 동작 확인
- [ ] 재연결 실패 시 오류 상태 UI 표시
- [ ] 재연결 성공 시 정상 상태 복귀

---

## Phase 2.2: ESP32-S3 HID 펌웨어 구현

**목표**: ESP32-S3에서 HID Boot Mouse/Keyboard 인터페이스 구현 및 PC에서 정상 인식 검증

#### Phase 2.2.1: ESP32-S3 로깅 인프라 구축

**세부 목표**:
1. ESP-IDF 로깅 시스템 설정 (`esp_log.h`)
2. UART 모니터 출력 포맷 정의
3. 로그 레벨별 색상 구분 (ERROR: 빨강, WARN: 노랑 등)
4. 프레임 처리 타이밍 측정 로그 추가

**구현 파일**:
- `main/logging.c`: 로깅 유틸리티 함수
- `main/sdkconfig`: 로그 레벨 설정

**참조 문서**:
- `docs/board/esp32s3-code-implementation-guide.md` §2.1 로깅 및 디버깅

**검증**:
- [ ] ESP-IDF 로그 레벨별 색상 구분 표시
- [ ] 프레임 처리 시간 마이크로초 단위로 출력
- [ ] UART 모니터에서 읽기 쉬운 포맷으로 로그 표시
- [ ] 로그 필터 기능 동작 확인
- [ ] **ESP-IDF 로그 출력 예시 확인**:
  ```
  I (1234) BridgeOne: USB initialized
  D (1256) UART: Received 8 bytes: 2A 01 0A FB 02 02 04 00
  D (1257) Frame: seq=42 buttons=0x01 dx=10 dy=-5 wheel=2
  I (1258) HID: Mouse report sent (processing time: 1523 us)
  W (2000) CDC: Frame dropped (queue full)
  E (3000) UART: Communication error: FIFO overflow
  ```
- [ ] **ESP-IDF 로그 필터 명령어 테스트**:
  ```bash
  idf.py monitor --print-filter "BridgeOne:I UART:D Frame:D HID:I"
  idf.py monitor | grep -E "UART|Frame|HID"
  ```

---

#### Phase 2.2.2: TinyUSB Composite 디바이스 디스크립터 설계

**세부 목표**:
1. `main/usb_descriptors.c` 파일 생성
2. TinyUSB 매크로로 Composite 디바이스 구성
3. 인터페이스 순서 고정 (Keyboard → Mouse → CDC Comm → CDC Data)
4. VID/PID 설정 (VID: 0x303A, PID: 커스텀)

**디바이스 구성** (`docs/board/esp32s3-code-implementation-guide.md` §1.3.1):
```
Interface 0: HID Boot Keyboard (0x03/0x01/0x01)
Interface 1: HID Boot Mouse    (0x03/0x01/0x02)
Interface 2: CDC-ACM Comm      (0x02/0x02/0x00)
Interface 3: CDC-ACM Data      (0x0A/0x00/0x00)
```

**구현 파일**:
- `main/usb_descriptors.c`: USB 디스크립터 정의
- `main/CMakeLists.txt`: TinyUSB 컴포넌트 추가

**참조 문서**:
- `docs/board/esp32s3-code-implementation-guide.md` §1.3 USB Composite 디바이스 설계 계약
- `docs/technical-specification.md` §2.3.1 ESP32-S3-DevkitC-1-N16R8

**검증**:
- [ ] `idf.py build` 성공 (디스크립터 컴파일 오류 없음)
- [ ] 빌드 로그에서 TinyUSB 컴포넌트 정상 링크 확인
- [ ] USB 디스크립터 바이트 배열 정확성 확인
- [ ] 인터페이스 순서 고정 확인 (로그 출력)
- [ ] **빌드 상세 검증**:
  - 빌드 로그: "Linking bridgeone_board.elf" 성공 확인
  - 펌웨어 크기 확인: 예상 ~500KB 이하
  - Flash 사용량: `Total sizes: Used static DRAM: XXX bytes (< 10% of 16MB)`
  - SRAM 사용량: `Used static IRAM: XXX bytes (< 70% of 512KB)`
  - 컴파일 경고: 0개 확인 (Warning 없음)
  - TinyUSB 관련 심볼 링크 확인: `tud_hid_n_report`, `tud_cdc_rx_cb` 등

---

#### Phase 2.2.3: USB 디스크립터 검증 및 Windows 인식 테스트

**세부 목표**:
1. 펌웨어 플래싱 및 Windows 연결
2. Windows Device Manager에서 4개 인터페이스 인식 확인
3. "BridgeOne USB Keyboard/Mouse" 장치명 표시 검증
4. 드라이버 오류 없음 확인 (노란색 느낌표 없음)

**테스트 도구**:
- Windows Device Manager
- USBDeview (USB 장치 세부 정보 확인)

**참조 문서**:
- `docs/board/esp32s3-code-implementation-guide.md` §1.3.1 USB Composite 디바이스 구성

**검증**:
- [ ] Windows Device Manager에서 4개 인터페이스 모두 인식됨
- [ ] "BridgeOne USB Keyboard" 장치명으로 표시
- [ ] "BridgeOne USB Mouse" 장치명으로 표시
- [ ] "BridgeOne CDC" 장치명으로 표시
- [ ] 드라이버 오류 없음 (노란색 느낌표 없음)
- [ ] **PowerShell 터미널에서 장치 상태 확인**:
  ```powershell
  # 모든 BridgeOne 장치 상태 확인
  Get-PnpDevice -FriendlyName "*BridgeOne*" | Format-Table -AutoSize
  
  # 각 인터페이스별 Status가 모두 "OK"인지 확인
  Get-PnpDevice -FriendlyName "*BridgeOne*" | Where-Object {$_.Status -ne "OK"}
  # (출력 없음 = 모두 정상)
  ```
- [ ] **각 인터페이스별 세부 정보 확인**:
  - HID Keyboard: `Status = OK`, `Class = HIDClass`, `InstanceId`에 `VID_303A` 포함
  - HID Mouse: `Status = OK`, `Class = HIDClass`, `InstanceId`에 `VID_303A` 포함
  - CDC Serial Port: `Status = OK`, `Class = Ports`, COM 포트 번호 할당됨
- [ ] **USBView 도구로 인터페이스 순서 확인**:
  - Interface 0: HID Keyboard (bInterfaceClass=0x03, bInterfaceProtocol=0x01)
  - Interface 1: HID Mouse (bInterfaceClass=0x03, bInterfaceProtocol=0x02)
  - Interface 2: CDC Comm (bInterfaceClass=0x02, bInterfaceSubClass=0x02)
  - Interface 3: CDC Data (bInterfaceClass=0x0A)

---

#### Phase 2.2.4: UART 수신 태스크 구현

**세부 목표**:
1. FreeRTOS 태스크 생성 (Core 0, 우선순위 10)
2. UART에서 8바이트 프레임 수신
3. 수신 큐에 프레임 저장 (큐 크기: 16)
4. 수신 오류 처리 (타임아웃, 불완전 프레임)

**구현 파일**:
- `main/task_uart.c`: UART 수신 태스크
- `main/main.c`: 태스크 초기화

**참조 문서**:
- `docs/board/esp32s3-code-implementation-guide.md` §2.5 FreeRTOS 태스크 구조

**검증**:
- [ ] `idf.py monitor`에서 UART 태스크 시작 확인
- [ ] 8바이트 프레임 정상 수신 (로그 출력)
- [ ] 큐 오버플로우 처리 확인 (16개 초과 시)
- [ ] 불완전 프레임 거부 확인 (8바이트 미만)
- [ ] **성능 기준선 측정 (ESP32-S3 UART 수신)**:
  - UART ISR → 프레임 큐 전달 시간 측정 (목표: < 2ms)
  - 큐 대기 시간 측정 (목표: < 1ms)
  - 로그: "UART frame queued (ISR time: 567 us, queue depth: 2/32)"

---

#### Phase 2.2.5: HID Boot Mouse 리포트 변환 및 전송

**세부 목표**:
1. UART 프레임을 HID Mouse 리포트(4바이트)로 변환
2. `tud_hid_n_report(1, ...)` API로 전송
3. 폴링 레이트 125Hz 설정
4. 버튼/deltaX/deltaY/wheel 필드 매핑

**HID Mouse 리포트 구조** (`docs/board/esp32s3-code-implementation-guide.md` §1.3.2):
```c
struct HIDBootMouseReport {
    uint8_t buttons; // [0] bit0=L, bit1=R, bit2=M
    int8_t deltaX;   // [1] X축 이동 (-127~127)
    int8_t deltaY;   // [2] Y축 이동 (-127~127)
    int8_t wheel;    // [3] 휠 스크롤 (-127~127)
} __attribute__((packed));
```

**구현 파일**:
- `main/hid_mouse.c`: HID Mouse 리포트 처리
- `main/task_hid.c`: HID 처리 태스크

**참조 문서**:
- `docs/board/esp32s3-code-implementation-guide.md` §2.4.6.1 HID Boot Mouse Interface 최적화
- `docs/technical-specification.md` §3.1 MOUSE_MOVE_STEP: 8, MOUSE_SCROLL_STEP: 1

**검증**:
- [ ] HID 리포트가 정확히 4바이트로 생성됨 (로그 확인)
- [ ] 버튼 비트맵 정확히 매핑됨 (bit0=L, bit1=R, bit2=M)
- [ ] deltaX/deltaY 범위 제한 확인 (-127~127)
- [ ] 125Hz 폴링 레이트로 전송 확인
- [ ] **성능 기준선 측정 (ESP32-S3 HID Mouse)**:
  - UART 수신 → HID 전송 처리 시간: `esp_timer_get_time()` 측정 (목표: < 5ms)
  - 로그 출력 예시: "HID Mouse report sent (processing time: 1523 us)"
  - 초당 HID 리포트 전송 횟수 측정 (목표: ≥ 125 reports/sec)

---

#### Phase 2.2.5.5: HID Mouse 단독 동작 검증 (인터페이스 격리)

**세부 목표**:
1. HID Keyboard 비활성화 상태에서 Mouse만 테스트
2. USB 트래픽 분석으로 Mouse 리포트만 전송 확인
3. 다른 인터페이스와의 간섭 없음 검증
4. 성능 기준선 측정 (Mouse 단독)

**테스트 방법**:
- ESP32-S3 펌웨어에서 HID Keyboard 리포트 전송 일시 비활성화
- Mouse 입력만 포함된 프레임 전송
- USB 프로토콜 분석 도구로 트래픽 모니터링

**검증**:
- [ ] HID Keyboard 리포트 전송 0건 확인 (USB 트래픽 분석)
- [ ] HID Mouse 리포트만 정상 전송 (초당 125회)
- [ ] PC에서 마우스 커서 정상 이동
- [ ] Mouse 단독 CPU 사용률 측정 (기준선: < 10%)
- [ ] Mouse 처리 지연시간 측정 (기준선: < 3ms)

---

#### Phase 2.2.6: HID Mouse 동작 검증 (PC 환경)

**세부 목표**:
1. PC에서 마우스 커서 이동 확인
2. 좌/우/중앙 클릭 동작 확인
3. 휠 스크롤 동작 확인
4. 연속 이동 시 부드러운 동작 검증

**테스트 환경**:
- Windows 11 메모장
- 마우스 속성 설정에서 포인터 속도 확인

**검증**:
- [ ] PC에서 마우스 커서가 정상적으로 움직임
- [ ] 좌클릭 동작 확인 (텍스트 선택)
- [ ] 우클릭 동작 확인 (컨텍스트 메뉴)
- [ ] 중앙클릭 동작 확인 (브라우저 탭 닫기)
- [ ] 휠 스크롤 동작 확인 (페이지 스크롤)
- [ ] 연속 이동 시 부드러운 동작 검증

---

#### Phase 2.2.6.5: USB 트래픽 분석 및 프로토콜 검증

**세부 목표**:
1. USB 프로토콜 분석 도구로 HID 리포트 구조 검증
2. 폴링 레이트 125Hz 실제 측정
3. 리포트 크기 및 데이터 무결성 확인
4. BIOS Boot Protocol 준수 검증

**테스트 도구**:
- Wireshark + USBPcap (Windows)
- usbhid-dump (Linux)
- USB Protocol Analyzer

**Wireshark USB 캡처 방법**:
```bash
# Windows 환경
1. USBPcap 드라이버 설치 (Wireshark 설치 시 포함)
2. Wireshark 실행 → Capture → USBPcap 선택
3. 필터 설정: usb.bInterfaceClass == 0x03 && usb.bInterfaceProtocol == 0x02
4. ESP32-S3 연결 후 마우스 이동 → 캡처 시작
```

**usbhid-dump 사용법 (Linux)**:
```bash
# HID 디스크립터 덤프
sudo usbhid-dump -d 303a: -e descriptor

# 실시간 HID 리포트 모니터링
sudo usbhid-dump -d 303a: -e stream
```

**검증**:
- [ ] **HID Mouse 리포트 구조 확인** (Wireshark):
  - 리포트 크기: 정확히 4바이트
  - 필드 순서: [buttons] [deltaX] [deltaY] [wheel]
  - 값 범위: deltaX/deltaY/wheel = -127 ~ 127
- [ ] **폴링 레이트 측정**:
  - USB 리포트 전송 간격: 8ms (125Hz)
  - 100개 리포트 캡처 → 평균 간격 계산
- [ ] **Boot Protocol 준수 확인**:
  - bInterfaceSubClass = 0x01 (Boot Interface)
  - bInterfaceProtocol = 0x02 (Mouse)
  - HID Descriptor에 Boot Protocol 표시
- [ ] USB 트래픽 분석 결과 스크린샷 저장

---

#### Phase 2.2.7: HID Boot Keyboard 리포트 변환 및 전송

**세부 목표**:
1. UART 프레임을 HID Keyboard 리포트(8바이트)로 변환
2. `tud_hid_n_report(0, ...)` API로 전송
3. 디바운싱 처리 (40ms)
4. 모디파이어 키 + 키 코드 매핑

**HID Keyboard 리포트 구조** (`docs/board/esp32s3-code-implementation-guide.md` §1.3.2):
```c
struct HIDBootKeyboardReport {
    uint8_t modifiers;   // [0] Ctrl/Alt/Shift/GUI 비트맵
    uint8_t reserved;    // [1] 0x00 고정
    uint8_t keyCodes[6]; // [2-7] 동시 입력 키 코드 (6KRO)
} __attribute__((packed));
```

**구현 파일**:
- `main/hid_keyboard.c`: HID Keyboard 리포트 처리
- `main/task_hid.c`: HID 처리 태스크

**참조 문서**:
- `docs/board/esp32s3-code-implementation-guide.md` §2.4.6.2 HID Boot Keyboard Interface 최적화
- `docs/technical-specification.md` §3.1 KEYBOARD_DEBOUNCE_MS: 40

**검증**:
- [ ] HID 리포트가 정확히 8바이트로 생성됨 (로그 확인)
- [ ] 모디파이어 비트맵 정확히 매핑됨
- [ ] 키 코드 배열 정확히 채워짐 (최대 6개 동시 입력)
- [ ] 40ms 디바운싱 동작 확인

---

#### Phase 2.2.8: HID Keyboard 동작 검증 (PC 환경)

**세부 목표**:
1. PC에서 키보드 입력 확인 (메모장 테스트)
2. Ctrl+C, Ctrl+V 등 모디파이어 조합 확인
3. 디바운싱 정상 동작 확인 (키 반복 없음)
4. 연속 입력 시 순서 보장 검증

**테스트 환경**:
- Windows 11 메모장
- 키보드 속성 설정에서 반복 속도 확인

**검증**:
- [ ] PC에서 키보드 입력 정상 동작 (a-z, 0-9, 특수키)
- [ ] Ctrl+C 복사 동작 확인
- [ ] Ctrl+V 붙여넣기 동작 확인
- [ ] Shift+문자 대문자 입력 확인
- [ ] 디바운싱 정상 동작 (키 반복 없음)
- [ ] 연속 입력 시 순서 보장 검증

---

#### Phase 2.2.7.5: HID Keyboard 단독 동작 검증 (인터페이스 격리)

**세부 목표**:
1. HID Mouse 비활성화 상태에서 Keyboard만 테스트
2. USB 트래픽 분석으로 Keyboard 리포트만 전송 확인
3. 다른 인터페이스와의 간섭 없음 검증
4. 성능 기준선 측정 (Keyboard 단독)

**테스트 방법**:
- ESP32-S3 펌웨어에서 HID Mouse 리포트 전송 일시 비활성화
- Keyboard 입력만 포함된 프레임 전송
- USB 프로토콜 분석 도구로 트래픽 모니터링

**Wireshark 필터 설정**:
```
usb.bInterfaceClass == 0x03 && usb.bInterfaceProtocol == 0x01
```

**검증**:
- [ ] HID Mouse 리포트 전송 0건 확인 (USB 트래픽 분석)
- [ ] HID Keyboard 리포트만 정상 전송
- [ ] 리포트 크기: 정확히 8바이트
- [ ] 필드 구조: [modifiers] [reserved=0x00] [key1-6]
- [ ] PC에서 키보드 입력 정상 동작
- [ ] Keyboard 단독 CPU 사용률 측정 (기준선: < 10%)
- [ ] 40ms 디바운싱 동작 확인 (연속 입력 간격 측정)

---

#### Phase 2.2.9: BIOS 환경 HID Boot Protocol 검증

**세부 목표**:
1. PC를 BIOS 화면으로 부팅
2. 마우스로 BIOS 메뉴 항목 선택 확인
3. 키보드로 BIOS 설정 변경 확인 (F1, Esc, Enter 등)
4. Essential 모드 동작 완전 검증

**테스트 환경**:
- PC BIOS 화면 (F2 또는 Del 키로 진입)
- UEFI 설정 화면

**참조 문서**:
- `docs/PRD.md` §3.2.1 Essential 모드 전용 페이지

**검증**:
- [ ] BIOS 화면에서 마우스 커서 이동 확인
- [ ] BIOS 메뉴 항목 마우스로 선택 확인
- [ ] **BIOS 진입 및 네비게이션 검증**:
  - Del 또는 F2 키로 BIOS 진입 성공
  - 방향키 상/하/좌/우 모두 정상 동작
  - Enter 키로 메뉴 선택 확인
  - Esc 키로 이전 메뉴 복귀 확인
  - F1, F10 등 BIOS 기능키 동작 확인
- [ ] **UEFI 환경 추가 검증** (지원하는 경우):
  - 마우스로 UEFI 메뉴 항목 클릭
  - 드래그 앤 드롭 기능 테스트 (부팅 순서 변경)
- [ ] BIOS 설정 변경 가능 확인 (예: 부팅 순서)
- [ ] Essential 모드 완전 동작 검증 (Windows 서버 없이)
- [ ] **BIOS 종료 및 부팅 검증**:
  - F10 (저장 후 종료) 동작 확인
  - Esc (저장 없이 종료) 동작 확인

---

## Phase 2.3: Windows 서버 기본 인프라

**목표**: Windows 서버 프로젝트 생성 및 Vendor CDC 통신 기본 인프라 구축

#### Phase 2.3.1: .NET 프로젝트 생성 및 초기 구조 설정

**세부 목표**:
1. .NET 8+ Console Application 프로젝트 생성
2. 솔루션 구조 설계 (Communication, Protocol, Utils 폴더)
3. NuGet 패키지 추가 (System.IO.Ports, Newtonsoft.Json)
4. 프로젝트 빌드 및 실행 확인

**구현 파일**:
- `src/windows/BridgeOne/BridgeOne.csproj`: 프로젝트 파일
- `src/windows/BridgeOne/Program.cs`: 메인 진입점
- `src/windows/BridgeOne/BridgeOne.sln`: 솔루션 파일

**참조 문서**:
- `docs/windows/technical-specification-server.md` §2.1 USB 인터페이스 계약

**검증**:
- [ ] .NET 8 프로젝트 정상 생성
- [ ] Communication, Protocol, Utils 폴더 생성 확인
- [ ] NuGet 패키지 복원 성공
- [ ] `dotnet build` 성공
- [ ] `dotnet run` 실행 확인

---

#### Phase 2.3.2: Windows 서버 로깅 인프라 구축

**세부 목표**:
1. 구조화된 로깅 시스템 구현 (Serilog 또는 NLog)
2. 로그 레벨 정의 (Verbose, Debug, Info, Warning, Error)
3. 파일 로그 출력 설정 (로그 로테이션)
4. 콘솔 색상 구분 로그 출력

**구현 파일**:
- `Utils/Logger.cs`: 로깅 유틸리티
- `Utils/LoggerConfiguration.cs`: 로그 설정

**참조 문서**:
- `docs/windows/technical-specification-server.md` §2.2 시스템 아키텍처 설계

**검증**:
- [ ] Serilog 또는 NLog 패키지 추가 확인
- [ ] 로그 레벨별 색상 구분 콘솔 출력 확인
- [ ] 파일 로그 출력 확인 (logs/bridgeone-YYYYMMDD.log)
- [ ] 로그 로테이션 동작 확인 (일별)
- [ ] 구조화된 로그 포맷 확인 (타임스탬프, 레벨, 메시지)

---

#### Phase 2.3.3: COM 포트 탐지 및 Vendor CDC 연결

**세부 목표**:
1. `System.IO.Ports.SerialPort` 클래스로 COM 포트 열기
2. ESP32-S3 Vendor CDC 포트 자동 탐지 (VID/PID 기반)
3. 연결 상태 관리 (`Disconnected → Connecting → Connected`)
4. 연결 실패 시 오류 로깅 및 재시도 메커니즘

**구현 파일**:
- `Communication/ComPortManager.cs`: COM 포트 관리
- `Communication/DeviceDetector.cs`: ESP32-S3 장치 탐지

**참조 문서**:
- `docs/windows/technical-specification-server.md` §2.3.3 확장 경로: Vendor CDC

**검증**:
- [ ] 사용 가능한 COM 포트 목록 출력 확인
- [ ] ESP32-S3 Vendor CDC 포트 자동 탐지 (VID: 0x303A)
- [ ] COM 포트 열기 성공 (9600bps 기본 설정)
- [ ] 연결 상태 로그 출력 확인
- [ ] 연결 실패 시 3초 후 재시도 확인
- [ ] **PowerShell로 COM 포트 매핑 확인**:
  ```powershell
  # BridgeOne CDC 포트 찾기
  Get-WmiObject Win32_SerialPort | Where-Object {$_.Caption -like "*BridgeOne*"} | Select-Object DeviceID, Caption, Description
  
  # 예상 출력:
  # DeviceID: COM5
  # Caption: BridgeOne CDC (COM5)
  # Description: USB Serial Device
  ```
- [ ] **Device Manager에서 COM 포트 번호 확인**:
  - "포트(COM & LPT)" → "BridgeOne CDC (COMX)" 표시
  - 속성 → 포트 설정 → "초당 비트" = 9600 확인
- [ ] **VID/PID 필터링 로그 검증**:
  ```
  [INFO] Scanning for ESP32-S3 devices (VID: 0x303A)...
  [INFO] Found device: VID=0x303A, PID=0x1001, Port=COM5
  [INFO] Device filter matched: BridgeOne CDC
  [INFO] Opening COM5...
  [INFO] COM5 opened successfully (9600 8N1)
  ```

---

#### Phase 2.3.4: CRC16 체크섬 유틸리티 구현

**세부 목표**:
1. CRC16-CCITT 알고리즘 구현 (`Crc16.cs`)
2. Little-Endian 바이트 순서 처리
3. 체크섬 계산 및 검증 함수
4. 단위 테스트 (알려진 테스트 벡터로 검증)

**구현 파일**:
- `Utils/Crc16.cs`: CRC16 계산 유틸리티

**참조 문서**:
- `docs/windows/technical-specification-server.md` §2.3.3 확장 경로: Vendor CDC

**검증**:
- [ ] CRC16-CCITT 알고리즘 구현 완료
- [ ] 알려진 테스트 벡터로 정확성 검증 (예: "123456789" → 0x29B1)
- [ ] Little-Endian 바이트 순서 변환 확인
- [ ] 체크섬 계산 성능 측정 (1000 bytes → ≤1ms)

---

#### Phase 2.3.5: 기본 핸드셰이크 메시지 정의

**세부 목표**:
1. `HANDSHAKE_REQUEST` JSON 메시지 정의
2. `HANDSHAKE_RESPONSE` JSON 메시지 정의
3. 메시지 직렬화/역직렬화 유틸리티
4. 타임스탬프 및 고유 ID 생성 로직

**구현 파일**:
- `Protocol/Messages/HandshakeRequest.cs`: 핸드셰이크 요청 메시지
- `Protocol/Messages/HandshakeResponse.cs`: 핸드셰이크 응답 메시지
- `Protocol/MessageSerializer.cs`: JSON 직렬화 유틸리티

**참조 문서**:
- `docs/windows/technical-specification-server.md` §3.1 연결 관리 기술

**검증**:
- [ ] `HANDSHAKE_REQUEST` JSON 직렬화 확인
- [ ] `HANDSHAKE_RESPONSE` JSON 역직렬화 확인
- [ ] UUID v4 고유 ID 생성 확인
- [ ] ISO 8601 타임스탬프 포맷 확인

---

## Phase 2.4: ESP32-S3 ↔ Windows Vendor CDC

**목표**: ESP32-S3와 Windows 서버 간 Vendor CDC 인터페이스로 양방향 JSON 메시지 통신 구현

#### Phase 2.4.1: ESP32-S3 Vendor CDC 프레임 수신 구현

**세부 목표**:
1. TinyUSB CDC 콜백 함수 구현 (`tud_cdc_rx_cb()`)
2. 0xFF 헤더 기반 프레임 파싱
3. `length` 필드 추출 (Little-Endian, 최대 448 bytes)
4. 불완전 프레임 처리 (버퍼링)

**Vendor CDC 프레임 구조** (`docs/board/esp32s3-code-implementation-guide.md` §1.3.4):
```c
struct VendorCDCFrame {
    uint8_t header;      // [0] 0xFF
    uint8_t command;     // [1] 0x01-0xFE
    uint16_t length;     // [2-3] Little-Endian, ≤448
    uint8_t payload[];   // [4+] JSON (UTF-8)
    uint16_t checksum;   // [끝-2, 끝-1] CRC16 (Little-Endian)
} __attribute__((packed));
```

**구현 파일**:
- `main/vendor_cdc.c`: Vendor CDC 프레임 처리
- `main/cdc_buffer.c`: CDC 수신 버퍼 관리

**참조 문서**:
- `docs/technical-specification.md` §2.4.6.3 Vendor CDC Interface 최적화

**검증**:
- [ ] `tud_cdc_rx_cb()` 콜백 함수 정상 호출 확인
- [ ] 0xFF 헤더 프레임 정상 파싱 (로그 확인)
- [ ] `length` 필드 Little-Endian 추출 확인
- [ ] 불완전 프레임 버퍼링 동작 확인
- [ ] **length 필드 경계값 테스트**:
  - length=0 (페이로드 없음): 6바이트 프레임 정상 처리
  - length=1: 최소 페이로드 프레임 정상 처리
  - length=448: 최대 페이로드 프레임 정상 처리
  - length=449: 프레임 거부 확인 (로그: "Payload length exceeds limit: 449 > 448")

---

#### Phase 2.4.2: ESP32-S3 CRC16 검증 및 오류 응답

**세부 목표**:
1. `esp_crc16_le()` 기반 체크섬 검증
2. CRC16 불일치 시 `CRC_ERROR` 응답 전송
3. `length` 필드 초과 시 프레임 거부 (448 bytes 제한)
4. 오류 통계 로깅 (CRC 오류 횟수, 길이 오류 횟수)

**구현 파일**:
- `main/crc16.c`: CRC16 계산 함수
- `main/vendor_cdc.c`: CRC 검증 로직

**참조 문서**:
- `docs/board/esp32s3-code-implementation-guide.md` §1.3.4 Vendor CDC 프레임 구조

**검증**:
- [ ] CRC16 정상 계산 확인 (테스트 벡터)
- [ ] CRC16 불일치 시 `CRC_ERROR` 응답 전송
- [ ] `length` 필드 448 초과 시 거부됨
- [ ] 오류 통계 로그 출력 확인
- [ ] **CRC16 알고리즘 테스트 벡터 검증**:
  ```c
  // 알려진 테스트 벡터
  const char* test_data = "123456789";
  uint16_t expected_crc = 0x29B1;  // CRC16-CCITT 표준값
  uint16_t calculated = esp_crc16_le(0, (uint8_t*)test_data, 9);
  assert(calculated == expected_crc);
  ```
- [ ] **의도적 CRC 변조 테스트**:
  - 정상 프레임의 checksum 필드 1바이트 변경 → `CRC_ERROR` 응답 확인
  - 로그 출력: `CRC mismatch: received 0xXXXX, calculated 0xYYYY`
  - 오류 통계 증가: `crc_errors: 1`

---

#### Phase 2.4.3: ESP32-S3 JSON 페이로드 파싱

**세부 목표**:
1. cJSON 라이브러리 통합 (ESP-IDF 컴포넌트)
2. JSON 페이로드 파싱 (`cJSON_Parse()`)
3. `command` 필드 추출 및 검증
4. JSON 파싱 오류 시 `PARSE_ERROR` 응답

**JSON 메시지 예시**:
```json
{
  "command": "HANDSHAKE_REQUEST",
  "timestamp": "2025-10-21T12:34:56.789Z"
}
```

**구현 파일**:
- `main/json_handler.c`: JSON 파싱 및 핸들러
- `main/CMakeLists.txt`: cJSON 컴포넌트 추가

**참조 문서**:
- `docs/technical-specification.md` §4.4.1 매크로 실행 플로우

**검증**:
- [ ] cJSON 라이브러리 빌드 성공
- [ ] JSON 페이로드 파싱 성공 (로그에 `command` 필드 출력)
- [ ] 잘못된 JSON 수신 시 `PARSE_ERROR` 응답 전송
- [ ] 448 bytes 크기의 JSON 메시지 정상 처리
- [ ] **JSON 파싱 오류 시나리오 테스트**:
  - 닫는 괄호 누락: `{"command": "TEST"` → `PARSE_ERROR` 응답
  - 필수 필드 누락: `{"timestamp": "..."}` (command 없음) → `PARSE_ERROR`
  - 잘못된 인코딩: 비-UTF8 바이트 → `PARSE_ERROR`
  - 로그 출력: `JSON parse error: Missing required field: 'command'`

---

#### Phase 2.4.4: ESP32-S3 핸드셰이크 핸들러 구현

**세부 목표**:
1. `HANDSHAKE_REQUEST` 수신 처리
2. `HANDSHAKE_RESPONSE` 응답 전송
3. Android 앱에 `SERVER_CONNECTED` 알림 전송 (UART)
4. 핸드셰이크 완료 로깅

**핸드셰이크 시퀀스**:
```
1. Windows 서버: HANDSHAKE_REQUEST → ESP32-S3 (Vendor CDC)
2. ESP32-S3: HANDSHAKE_RESPONSE → Windows 서버 (Vendor CDC)
3. ESP32-S3: SERVER_CONNECTED 알림 → Android 앱 (UART)
```

**구현 파일**:
- `main/handshake.c`: ESP32-S3 핸드셰이크 핸들러

**참조 문서**:
- `docs/windows/technical-specification-server.md` §3.1 연결 관리 기술

**검증**:
- [ ] `HANDSHAKE_REQUEST` 수신 확인 (로그)
- [ ] `HANDSHAKE_RESPONSE` JSON 생성 및 전송 확인
- [ ] UART로 `SERVER_CONNECTED` 알림 전송 확인
- [ ] 핸드셰이크 완료 로그 출력

---

#### Phase 2.4.4.5: Vendor CDC 단독 통신 검증 (인터페이스 격리)

**세부 목표**:
1. HID 인터페이스 비활성화 상태에서 CDC만 테스트
2. Windows COM 포트로 JSON 메시지만 송수신 확인
3. CDC 통신 성능 기준선 측정
4. 다른 인터페이스와의 간섭 없음 검증

**테스트 방법**:
- ESP32-S3 펌웨어에서 HID 리포트 전송 일시 비활성화
- Windows 터미널 프로그램 (PuTTY, RealTerm 등)으로 COM 포트 직접 연결
- JSON 메시지 수동 전송 및 응답 확인

**PuTTY 설정**:
```
Serial line: COMX (BridgeOne CDC 포트)
Speed: 9600
Data bits: 8
Stop bits: 1
Parity: None
Flow control: None
```

**테스트 JSON 메시지**:
```json
{"command": "TEST_PING", "timestamp": "2025-10-23T12:34:56.789Z", "data": "Hello from Windows"}
```

**검증**:
- [ ] PuTTY로 COM 포트 열기 성공
- [ ] HID 리포트 전송 0건 확인
- [ ] JSON 메시지 전송 → ESP32-S3 로그에서 수신 확인
- [ ] ESP32-S3에서 응답 JSON 전송 → PuTTY에서 수신 확인
- [ ] CDC 단독 처리 지연시간 측정 (기준선: < 5ms)
- [ ] CDC 통신 시 HID 인터페이스 영향 없음 확인

---

#### Phase 2.4.5: Windows 서버 Vendor CDC 프레임 송신 구현

**세부 목표**:
1. Vendor CDC 프레임 직렬화 (`VendorCdcFrame.cs`)
2. 0xFF 헤더 + command + length + JSON 페이로드 + CRC16 구조
3. Little-Endian 바이트 순서 처리
4. 프레임 송신 함수 (`SendVendorCdcFrameAsync()`)

**구현 파일**:
- `Protocol/VendorCdcFrame.cs`: 프레임 직렬화/역직렬화
- `Communication/VendorCdcManager.cs`: Vendor CDC 통신 관리

**참조 문서**:
- `docs/windows/technical-specification-server.md` §2.3.3 확장 경로: Vendor CDC

**검증**:
- [ ] Vendor CDC 프레임 정확히 직렬화됨 (HEX dump 확인)
- [ ] 0xFF 헤더 + command + length 순서 확인
- [ ] CRC16 체크섬 정확히 계산됨 (Little-Endian)
- [ ] COM 포트로 프레임 송신 성공
- [ ] **Vendor CDC 프레임 구조 HEX dump 분석**:
  ```
  예시 프레임: {"command": "TEST", "data": "Hello"}
  페이로드 크기: 38 bytes (0x0026)
  
  HEX dump:
  FF                    // [0] Header
  01                    // [1] Command
  26 00                 // [2-3] Length (Little-Endian: 0x0026 = 38)
  7B 22 63 6F 6D 6D...  // [4-41] Payload (JSON UTF-8)
  B1 29                 // [42-43] CRC16 (Little-Endian: 0x29B1)
  
  총 크기: 44 bytes (1+1+2+38+2)
  ```
- [ ] **length 필드 Little-Endian 검증**:
  - 38 bytes → 0x0026 → [26 00] 바이트 순서 확인
  - 456 bytes → 0x01C8 → [C8 01] 바이트 순서 확인
- [ ] **CRC16 Little-Endian 검증**:
  - CRC=0x29B1 → [B1 29] 바이트 순서 확인
  - 로그: "CRC16 calculated: 0x29B1 (bytes: B1 29)"

---

#### Phase 2.4.6: Windows 서버 Vendor CDC 프레임 수신 및 파싱

**세부 목표**:
1. COM 포트 수신 버퍼 관리
2. 0xFF 헤더 기반 프레임 동기화
3. `length` 필드 기반 프레임 추출
4. CRC16 검증 및 재전송 메커니즘 (최대 3회)

**구현 파일**:
- `Communication/VendorCdcReceiver.cs`: 프레임 수신 및 파싱

**참조 문서**:
- `docs/windows/technical-specification-server.md` §2.3.3 확장 경로: Vendor CDC

**검증**:
- [ ] 0xFF 헤더 동기화 확인
- [ ] `length` 필드 기반 프레임 정확히 추출됨
- [ ] CRC16 검증 성공 (정상 프레임)
- [ ] CRC16 오류 발생 시 자동 재전송 동작 (최대 3회)

---

#### Phase 2.4.7: 핸드셰이크 프로토콜 End-to-End 테스트

**세부 목표**:
1. Windows 서버 시작 → `HANDSHAKE_REQUEST` 전송
2. ESP32-S3에서 `HANDSHAKE_RESPONSE` 수신 확인
3. Android 앱 `AppState` 전환 (WaitingForConnection → Standard)
4. 핸드셰이크 3초 이내 완료 검증

**테스트 시나리오**:
1. Windows 서버 시작
2. ESP32-S3 Vendor CDC 포트 연결
3. 핸드셰이크 프로토콜 실행
4. Android 앱 UI 모드 전환 확인

**참조 문서**:
- `docs/windows/technical-specification-server.md` §3.1 연결 관리 기술

**검증**:
- [ ] Windows 서버 시작 → 3초 이내 핸드셰이크 완료
- [ ] ESP32-S3 로그에서 `HANDSHAKE_REQUEST` 수신 확인
- [ ] Windows 서버 로그에서 `HANDSHAKE_RESPONSE` 수신 확인
- [ ] Android 앱 화면이 Essential → Standard 모드로 전환
- [ ] **핸드셰이크 시퀀스 상세 로그 검증**:
  ```
  [Windows] 12:34:56.000 - Sending HANDSHAKE_REQUEST
  [ESP32-S3] 12:34:56.050 - Received HANDSHAKE_REQUEST (50ms latency)
  [ESP32-S3] 12:34:56.055 - Sending HANDSHAKE_RESPONSE
  [Windows] 12:34:56.105 - Received HANDSHAKE_RESPONSE (55ms latency)
  [ESP32-S3] 12:34:56.110 - Sending SERVER_CONNECTED to Android (UART)
  [Android] 12:34:56.120 - Received SERVER_CONNECTED, switching to Standard mode
  
  Total handshake time: 120ms (< 3000ms ✓)
  ```
- [ ] **타임스탬프 동기화 확인**:
  - 각 로그의 타임스탬프 순서 일치
  - 전체 핸드셰이크 지연시간 계산 및 기록

---

#### Phase 2.4.8: Vendor CDC 기본 JSON 송수신 검증

**세부 목표**:
1. Windows → ESP32-S3 → Android 단방향 테스트
2. Android → ESP32-S3 → Windows 단방향 테스트
3. 실제 JSON 예시 데이터로 데이터 무결성 검증
4. 각 구간별 지연시간 측정

**테스트 JSON 메시지**:

**Windows → Android (TEST_PING)**:
```json
{
  "command": "TEST_PING",
  "timestamp": "2025-10-23T12:34:56.789Z",
  "data": "Hello from Windows Server",
  "sequence": 1
}
```

**Android → Windows (TEST_PONG)**:
```json
{
  "command": "TEST_PONG",
  "timestamp": "2025-10-23T12:34:57.123Z",
  "data": "Hello from Android App",
  "original_sequence": 1
}
```

**448 bytes 최대 크기 테스트 메시지**:
```json
{
  "command": "TEST_MAX_SIZE",
  "timestamp": "2025-10-23T12:34:58.000Z",
  "data": "Lorem ipsum dolor sit amet, consectetur adipiscing elit... (총 448 bytes까지 채움)"
}
```

**검증**:
- [ ] **Windows → ESP32-S3 → Android 경로**:
  - Windows 서버 로그: "Sending TEST_PING via Vendor CDC"
  - ESP32-S3 로그: "Received Vendor CDC frame, forwarding to UART"
  - Android Logcat: "Received TEST_PING: Hello from Windows Server"
  - 데이터 무결성: "Hello from Windows Server" 문자열 정확히 일치
- [ ] **Android → ESP32-S3 → Windows 경로**:
  - Android Logcat: "Sending TEST_PONG via UART"
  - ESP32-S3 로그: "Received UART JSON, forwarding to Vendor CDC"
  - Windows 서버 로그: "Received TEST_PONG: Hello from Android App"
  - 데이터 무결성: original_sequence=1 정확히 전달
- [ ] **448 bytes 최대 크기 JSON 메시지**:
  - JSON 직렬화 후 바이트 크기 확인: 정확히 448 bytes
  - Vendor CDC 프레임 총 크기: 454 bytes (6 bytes 헤더 + 448 bytes 페이로드)
  - 양방향 전송 성공 (Windows ↔ Android)
  - UTF-8 인코딩 정확성 확인 (한글, 특수문자 포함 시)
- [ ] **각 구간별 지연시간 측정**:
  - Windows 송신 → ESP32-S3 수신: < 10ms
  - ESP32-S3 중계 처리: < 5ms
  - ESP32-S3 송신 → Android 수신: < 10ms
  - 전체 단방향 지연시간: < 30ms

---

#### Phase 2.4.9: Vendor CDC 양방향 JSON 통신 End-to-End 검증

**세부 목표**:
1. 완전한 요청-응답 사이클 테스트
2. CRC 오류 시뮬레이션 및 재전송 검증
3. JSON 파싱 오류 복구 검증
4. 동시 다중 메시지 처리 테스트

**테스트 시나리오**:

**시나리오 1: 정상 요청-응답 사이클**
1. Windows에서 TEST_REQUEST 전송
2. ESP32-S3 중계 → Android 수신
3. Android에서 TEST_RESPONSE 생성 및 전송
4. ESP32-S3 중계 → Windows 수신
5. 전체 왕복 시간 측정

**시나리오 2: CRC 오류 및 재전송**
1. Windows에서 정상 메시지 전송
2. ESP32-S3에서 의도적으로 checksum 필드 변조
3. Android에서 CRC_ERROR 응답 전송
4. ESP32-S3에서 원본 메시지 재전송 (1차)
5. 최대 3회 재전송 후 성공 확인

**시나리오 3: JSON 파싱 오류**
1. Windows에서 잘못된 JSON 전송 (닫는 괄호 누락)
2. ESP32-S3에서 JSON 파싱 실패
3. PARSE_ERROR 응답 즉시 전송 (재전송 없음)
4. Windows에서 오류 수신 및 로그 기록

**검증**:
- [ ] **Windows → ESP32-S3 → Android → ESP32-S3 → Windows 완전한 왕복**:
  - 전체 왕복 시간: < 100ms
  - 데이터 무결성: 원본 데이터와 100% 일치
  - 메시지 ID 매칭 정확성
- [ ] **CRC 오류 시뮬레이션 및 재전송**:
  - 1차 재전송: 100ms 지연 후 (지수 백오프)
  - 2차 재전송: 200ms 지연 후
  - 3차 재전송: 400ms 지연 후
  - 재전송 성공률: 100% (3회 내 성공)
  - 로그: "CRC error, retrying (1/3)", "Retransmission successful"
- [ ] **JSON 파싱 오류 처리**:
  - PARSE_ERROR 응답 즉시 전송 (< 10ms)
  - 재전송 없이 오류 알림만 전송
  - 로그: "JSON parse error: syntax error at position 42"
- [ ] **동시 다중 메시지 처리**:
  - 5개 JSON 메시지 동시 전송 (Windows → Android)
  - 모든 메시지 순서대로 정확히 전달
  - 메시지 손실 0%
  - 큐 오버플로우 없음

---

#### Phase 2.4.10: Vendor CDC 프레임 시리얼 모니터 실시간 검증

**세부 목표**:
1. COM 포트 시리얼 모니터로 Vendor CDC 프레임 실시간 확인
2. 프레임 구조 육안 검증
3. length 필드와 실제 페이로드 크기 일치 확인
4. CRC16 체크섬 수동 계산 및 검증

**테스트 도구**:
- RealTerm Serial Capture (HEX 모드)
- PuTTY (Raw 바이너리 모드)
- CoolTerm (HEX Display)

**RealTerm 설정**:
```
Port: COMX (BridgeOne CDC)
Baud: 9600
Data: 8 bit
Parity: None
Stop: 1
Display: Hex (space)
```

**검증**:
- [ ] RealTerm HEX 모드로 프레임 실시간 확인
- [ ] 0xFF 헤더로 프레임 시작점 식별
- [ ] length 필드 읽기 (바이트 [2-3], Little-Endian)
- [ ] length 값만큼 페이로드 바이트 카운트 일치 확인
- [ ] CRC16 위치 계산: `4 + length` 오프셋
- [ ] **실제 캡처 예시**:
  ```
  FF 01 26 00 7B 22 63 6F 6D 6D 61 6E 64 22 3A 22
  54 45 53 54 22 2C 22 64 61 74 61 22 3A 22 48 65
  6C 6C 6F 22 7D B1 29
  
  분석:
  - Header: FF
  - Command: 01
  - Length: 26 00 → 0x0026 = 38 bytes
  - Payload: 38 bytes (JSON)
  - CRC16: B1 29 → 0x29B1
  ```

---

## Phase 2.5: End-to-End 통합 검증 및 안정화 (1주)

**목표**: 전체 통신 경로의 안정성, 성능, 오류 복구 메커니즘 검증

#### Phase 2.5.1: Essential 모드 전체 경로 통합 테스트

**세부 목표**:
1. Android → UART → ESP32-S3 → HID → PC 전체 경로 테스트
2. BIOS 화면에서 마우스/키보드 동작 확인
3. 100개 연속 입력 처리 시 패킷 손실 0% 검증
4. 연결 끊김 후 3초 이내 자동 재연결 확인

**테스트 시나리오**:
1. PC를 BIOS 화면으로 부팅
2. Android 앱에서 마우스 이동 → BIOS 메뉴 항목 선택 확인
3. 키보드 입력 → BIOS 설정 변경 확인
4. USB 케이블 제거 → 재연결 → 3초 이내 복구 확인

**참조 문서**:
- `docs/PRD.md` §3.2.1 Essential 모드 전용 페이지

**검증**:
- [ ] BIOS 화면에서 마우스 커서 이동 확인
- [ ] BIOS 화면에서 키보드 입력 확인 (F1, Esc, Enter 등)
- [ ] 100개 연속 마우스 이동 프레임 처리 시 패킷 손실 0%
- [ ] 연결 끊김 후 3초 이내 자동 재연결 성공
- [ ] **프레임 손실 검증 상세**:
  - 의도적 프레임 드롭 시뮬레이션: 100개 중 1개 전송 생략
  - ESP32-S3 로그: "Sequence gap detected: expected=5, got=6"
  - 순번 오류 통계 로그 출력: "Sequence errors: 1/100 (1.0%)"
  - 정상 전송 시 패킷 손실 0% 달성 확인
- [ ] **연속 프레임 처리 성능 측정**:
  - 100개 프레임 전송 시간 측정 (목표: < 1초)
  - 평균 프레임당 처리 시간 계산 (목표: < 10ms)
  - CPU 사용률 측정 (ESP32-S3: < 30%, Android: < 20%)

---

#### Phase 2.5.2: Standard 모드 전체 경로 통합 테스트

**세부 목표**:
1. Android → UART → ESP32-S3 → Vendor CDC → Windows 전체 경로 테스트
2. 핸드셰이크 프로토콜 정상 동작 확인
3. 테스트 JSON 메시지 양방향 통신 검증
4. Windows 서버 종료 시 Essential 모드 복귀 확인

**테스트 시나리오**:
1. Windows 서버 시작 → 핸드셰이크 완료 → Android 앱 Standard 모드 전환
2. Android 앱에서 테스트 버튼 탭 → Windows 서버에서 요청 수신 확인
3. Windows 서버에서 응답 전송 → Android 앱 UI 업데이트
4. Windows 서버 종료 → Android 앱 Essential 모드로 복귀 확인

**참조 문서**:
- `docs/technical-specification.md` §4.4.1 매크로 실행 플로우

**검증**:
- [ ] 핸드셰이크 3초 이내 완료
- [ ] Android 앱에서 Standard 모드 UI 표시
- [ ] 테스트 JSON 메시지 양방향 전달 확인
- [ ] Windows 서버 종료 시 Android 앱 즉시 Essential 모드로 복귀

---

#### Phase 2.5.3: 성능 벤치마크 및 지연시간 측정

**세부 목표**:
1. 엔드투엔드 지연시간 측정 (목표: ≤50ms)
2. 각 구간별 지연시간 분해 분석 (Android 터치 → UART → HID → PC)
3. 처리량 측정 (초당 프레임 수, 목표: ≥1000 frame/sec)
4. CPU 사용률 및 메모리 사용량 측정

**측정 항목**:
- Android 터치 → UART 송신: ≤10ms
- UART → ESP32-S3 HID 전송: ≤5ms
- HID → PC 입력 처리: ≤20ms
- **전체 지연시간**: ≤50ms

**측정 도구**:
- Android: Logcat 타임스탬프 분석
- ESP32-S3: `esp_timer_get_time()` 기반 시간 측정
- Windows: `Stopwatch` 클래스 사용

**참조 문서**:
- `docs/PRD.md` §7.1 성능 목표
- `docs/technical-specification.md` §3.2 성능 임계값

**검증**:
- [ ] 엔드투엔드 지연시간 ≤50ms 달성
- [ ] HID 프레임 처리량 ≥1000 frame/sec
- [ ] ESP32-S3 CPU 사용률 ≤30%
- [ ] Android 앱 메모리 사용량 ≤100MB
- [ ] **각 구간별 지연시간 분해 분석**:
  ```
  구간별 측정 결과 예시:
  1. Android 터치 감지 → UART 송신: 8.2ms (목표: ≤10ms) ✓
  2. UART 전송 시간: 0.08ms (1Mbps, 8바이트)
  3. ESP32-S3 UART 수신 → HID 큐: 1.5ms (목표: ≤5ms) ✓
  4. HID 큐 → USB 전송: 2.3ms
  5. USB HID → PC 드라이버: 15.8ms (목표: ≤20ms) ✓
  6. PC 입력 처리 → 화면 반영: 18.4ms
  
  총 엔드투엔드 지연시간: 46.3ms (목표: ≤50ms) ✓
  ```
- [ ] **병목 구간 식별**:
  - 가장 긴 구간 식별 및 최적화 여지 분석
  - 각 구간별 개선 목표 설정
- [ ] **HID 프레임 처리량 상세 측정**:
  - 1초간 전송된 총 프레임 수 카운트
  - 평균: 1250 frame/sec (목표: ≥1000) ✓
  - 최대: 1500 frame/sec (버스트 시)
  - 최소: 980 frame/sec (저전력 모드)

---

#### Phase 2.5.4: 오류 복구 시나리오 테스트

**세부 목표**:
1. CRC 오류 시뮬레이션 → 재전송 확인
2. JSON 파싱 오류 시뮬레이션 → `PARSE_ERROR` 응답 확인
3. USB 케이블 제거 → 자동 재연결 검증
4. 연속 3회 재연결 실패 → 오류 상태 UI 표시 확인

**테스트 시나리오**:
1. **CRC 오류 시뮬레이션**: 의도적으로 체크섬 변조 → 재전송 확인
2. **JSON 오류 시뮬레이션**: 잘못된 JSON 전송 → `PARSE_ERROR` 응답 확인
3. **연결 끊김 시뮬레이션**: USB 케이블 제거 → 자동 재연결 확인
4. **재연결 실패**: 연속 3회 재연결 실패 → 오류 상태 UI 표시

**참조 문서**:
- `docs/technical-specification.md` §2.4.6 인터페이스별 통신 최적화 전략

**검증**:
- [ ] CRC 오류 발생 시 3회 재전송 후 성공
- [ ] JSON 파싱 오류 시 `PARSE_ERROR` 응답 즉시 전송
- [ ] USB 케이블 제거 → 3초 이내 자동 재연결
- [ ] 연속 3회 재연결 실패 → 오류 상태 UI 표시 확인
- [ ] **경계값 테스트 추가**:
  - `deltaX/deltaY = ±127` 최대값 테스트: HEX dump에서 `7F` (127), `81` (-127) 확인
  - `deltaX/deltaY = 0` 테스트: 이동 없음 정상 처리
  - `wheel = ±127` 최대값 테스트: 최대 스크롤 속도 확인
- [ ] **Vendor CDC length 필드 경계값 테스트**:
  - length=0: 6바이트 최소 프레임 (페이로드 없음) 정상 처리
  - length=1: 7바이트 프레임 (1바이트 페이로드) 정상 처리
  - length=447: 최대 크기 직전 프레임 정상 처리
  - length=448: 최대 크기 프레임 정상 처리
  - length=449: 프레임 거부 및 오류 로그 확인
- [ ] **불완전 프레임 테스트**:
  - 8바이트 프레임 중 5바이트만 전송 → 거부 확인
  - Vendor CDC 프레임 헤더만 전송 (3바이트) → 타임아웃 후 거부
  - 페이로드 중간에서 전송 중단 → 버퍼링 후 타임아웃

---

#### Phase 2.5.5: 장시간 안정성 테스트

**세부 목표**:
1. 1시간 연속 사용 테스트 (마우스/키보드 입력 반복)
2. 메모리 누수 검증 (Android/ESP32-S3/Windows)
3. 크래시 없음 확인
4. 로그 파일 분석 (오류 발생 빈도, 재연결 횟수)

**테스트 시나리오**:
1. 1시간 동안 연속 마우스 이동 (100회/분)
2. 1시간 동안 연속 키보드 입력 (50회/분)
3. 메모리 사용량 5분 간격으로 측정
4. 로그 파일 수집 및 분석

**참조 문서**:
- `docs/PRD.md` §7.1 성능 목표

**검증**:
- [ ] 1시간 연속 사용 시 메모리 누수 없음
- [ ] 1시간 연속 사용 시 크래시 없음
- [ ] 오류 발생 빈도 ≤1% (총 입력 대비)
- [ ] 자동 재연결 성공률 ≥95%
- [ ] **메모리 사용량 추적**:
  - Android 앱: 5분 간격 측정 (목표: < 100MB, 증가율 < 5%)
  - ESP32-S3: 힙 메모리 모니터링 (`esp_get_free_heap_size()`)
  - Windows 서버: Task Manager 모니터링 (목표: < 256MB)
- [ ] **로그 파일 분석**:
  - 총 입력 횟수 집계
  - 오류 발생 횟수 및 유형별 분류
  - 재연결 시도 횟수 및 성공률 계산
  - 평균/최대/최소 지연시간 통계

---

#### Phase 2.5.6: Phase 2 최종 검증 체크리스트

**세부 목표**:
1. HID Boot Mouse/Keyboard 완벽 동작 (BIOS 포함)
2. Vendor CDC 양방향 JSON 통신 안정성 검증
3. 엔드투엔드 지연시간 ≤50ms 달성 확인
4. 패킷 손실률 0% 달성 확인
5. 자동 재연결 성공률 ≥95% 확인
6. 전체 통신 경로 문서화 및 개발 노트 정리

**최종 검증 항목**:

**HID 통신**:
- [ ] HID Boot Mouse 완벽 동작 (BIOS 포함)
- [ ] HID Boot Keyboard 완벽 동작 (BIOS 포함)
- [ ] Essential 모드 완전 검증

**Vendor CDC 통신**:
- [ ] Vendor CDC 양방향 JSON 통신 안정성 검증
- [ ] 핸드셰이크 프로토콜 완벽 동작
- [ ] CRC16 오류 복구 메커니즘 검증

**성능 목표**:
- [ ] 엔드투엔드 지연시간 ≤50ms 달성 확인
- [ ] HID 프레임 처리량 ≥1000 frame/sec
- [ ] 패킷 손실률 0% 달성 확인

**안정성**:
- [ ] 자동 재연결 성공률 ≥95% 확인
- [ ] 1시간 연속 사용 안정성 확인
- [ ] 메모리 누수 없음 확인

**문서화**:
- [ ] 전체 통신 경로 다이어그램 작성
- [ ] 개발 과정 중 발견한 이슈 및 해결 방법 기록
- [ ] 성능 측정 결과 정리
- [ ] **성능 기준선 문서 작성**:
  - Phase별 누적 성능 데이터 정리
  - 각 구간별 지연시간 분해 분석 결과
  - 병목 구간 식별 및 최적화 방안
- [ ] **검증 도구 사용법 가이드 작성**:
  - PowerShell 스크립트 모음 (USB 상태 확인, COM 포트 탐지)
  - Wireshark 필터 설정 및 캡처 방법
  - ESP-IDF 로그 분석 방법
  - Android Logcat 필터링 가이드

---

## Phase 2 검증 도구 및 명령어 요약

### Android 개발 환경

**Logcat 필터링**:
```bash
# UART 통신 로그만 보기
adb logcat -s UART:V Frame:D Protocol:I

# HEX dump 포함 로그
adb logcat | grep -E "UART.*\[RX\]|\[TX\]"

# 실시간 성능 측정
adb logcat | grep -E "latency|processing time"
```

**프레임 송신 성능 측정**:
```bash
# 100개 프레임 연속 전송 시간 측정
adb logcat -c && adb logcat -s FrameTransmitter:D | head -100
```

### ESP32-S3 개발 환경

**ESP-IDF 모니터링**:
```bash
# 로그 레벨별 필터링
idf.py monitor --print-filter "BridgeOne:I UART:D Frame:D HID:I CDC:D"

# 성능 측정 로그만 보기
idf.py monitor | grep -E "processing time|latency|fps"

# HEX dump 로그만 보기
idf.py monitor | grep "bytes:"
```

**성능 프로파일링**:
```bash
# CPU 사용률 및 태스크 상태
# idf.py monitor에서 입력:
# tasks (FreeRTOS 태스크 목록)
# heap (힙 메모리 정보)
```

### Windows 서버 환경

**PowerShell 장치 상태 확인**:
```powershell
# BridgeOne 장치 전체 상태
Get-PnpDevice -FriendlyName "*BridgeOne*" | Format-Table FriendlyName, Status, InstanceId -AutoSize

# COM 포트 매핑
Get-WmiObject Win32_SerialPort | Where-Object {$_.Caption -like "*BridgeOne*"} | Select-Object DeviceID, Caption

# USB 장치 세부 정보 (VID/PID 확인)
Get-PnpDevice -FriendlyName "*BridgeOne*" | Get-PnpDeviceProperty -KeyName DEVPKEY_Device_HardwareIds
```

**Vendor CDC 테스트 (PuTTY)**:
```
Serial line: COMX
Speed: 9600
Connection type: Serial
Terminal → Local echo: Force on
Terminal → Local line editing: Force on
```

### USB 트래픽 분석

**Wireshark USB 캡처 (Windows)**:
```
1. Capture → Options → USBPcap 선택
2. 필터 설정:
   - HID Mouse: usb.bInterfaceClass == 0x03 && usb.bInterfaceProtocol == 0x02
   - HID Keyboard: usb.bInterfaceClass == 0x03 && usb.bInterfaceProtocol == 0x01
   - CDC: usb.bInterfaceClass == 0x02
3. USB 장치 재연결 후 캡처 시작
```

**usbhid-dump (Linux)**:
```bash
# HID 디스크립터 덤프
sudo usbhid-dump -d 303a: -e descriptor

# 실시간 HID 리포트 모니터링
sudo usbhid-dump -d 303a:1001 -e stream | xxd

# 폴링 레이트 측정
sudo usbhid-dump -d 303a:1001 -e stream | ts '[%Y-%m-%d %H:%M:%S]'
```

---

## Phase 2 핵심 성과

**통신 경로 구축 완료** (41개 하위 Phase):
- ✅ Android ↔ ESP32-S3 UART 통신 (1Mbps, 8N1) - 7개 Phase
- ✅ ESP32-S3 HID 펌웨어 (Boot Mouse/Keyboard) - 12개 Phase (단독 검증 3개 추가)
- ✅ Windows 서버 기본 인프라 (.NET 8+) - 5개 Phase
- ✅ ESP32-S3 ↔ Windows Vendor CDC (JSON 통신) - 11개 Phase (단독 검증 1개, 양방향 E2E 1개, 시리얼 모니터 1개 추가)
- ✅ End-to-End 통합 검증 및 안정화 - 6개 Phase

**HID 키보드/마우스 입력 검증**:
- HID Boot Mouse 완벽 동작 (BIOS 환경 포함)
- HID Boot Keyboard 완벽 동작 (BIOS 환경 포함)
- Essential 모드 완전 검증 (Windows 서버 없이 동작)
- 125Hz 폴링 레이트, 40ms 디바운싱 적용

**Vendor CDC JSON 쌍방향 통신 검증**:
- 0xFF 헤더 기반 프레임 파싱
- CRC16 체크섬 기반 데이터 무결성 검증
- 핸드셰이크 프로토콜 완벽 동작 (3초 이내 완료)
- 최대 448 bytes JSON 메시지 양방향 전달
- Windows → ESP32-S3 → Android 전체 경로 확인

**성능 목표 달성**:
- 엔드투엔드 지연시간 ≤50ms
- HID 프레임 처리량 ≥1000 frame/sec
- 패킷 손실률 0%
- ESP32-S3 CPU 사용률 ≤30%
- Android 앱 메모리 사용량 ≤100MB

**오류 복구 메커니즘**:
- CRC16 기반 데이터 무결성 검증
- 자동 재전송 메커니즘 (최대 3회)
- Cancel and Restart 패턴 기반 연결 복구
- 자동 재연결 성공률 ≥95%

**로깅 및 디버깅 인프라**:
- Android: Logcat 기반 구조화된 로깅 (HEX dump)
- ESP32-S3: ESP-IDF 로깅 (색상 구분, 타이밍 측정)
- Windows: Serilog/NLog 기반 로깅 (파일 + 콘솔)

**모드 기반 동작**:
- Essential 모드: HID Boot Protocol만 사용 (BIOS 호환)
- Standard 모드: HID + Vendor CDC 통합 (고급 기능)

**문서화**:
- 전체 통신 경로 다이어그램
- 개발 과정 중 발견한 이슈 및 해결 방법 기록
- 성능 측정 결과 정리

---

## 다음 단계

Phase 2 완료 후 Phase 3: 고급 기능 및 최적화로 진행

**Phase 3 목표**: 매크로 시스템, 고급 UI, 시스템 최적화
**개발 기간**: 4-5주
**핵심 목표**: 사용자 경험 향상 및 시스템 안정성 강화
