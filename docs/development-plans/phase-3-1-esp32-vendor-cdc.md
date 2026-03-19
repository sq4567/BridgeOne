---
title: "BridgeOne Phase 3.1: ESP32-S3 Vendor CDC 프로토콜 핸들러 구현"
description: "BridgeOne 프로젝트 Phase 3.1 - ESP32-S3 펌웨어에 Vendor CDC 바이너리 프로토콜 수신/송신 기능 구현"
tags: ["esp32-s3", "vendor-cdc", "tinyusb", "protocol", "freertos"]
version: "v1.0"
owner: "Chatterbones"
updated: "2026-03-19"
---

# BridgeOne Phase 3.1: ESP32-S3 Vendor CDC 프로토콜 핸들러 구현

**개발 기간**: 3-5일

**목표**: ESP32-S3 펌웨어에 Vendor CDC 바이너리 프로토콜 수신/송신 기능을 추가하여, 기존 디버그 로깅과 공존시킵니다.

**핵심 성과물**:
- Vendor CDC 프레임 파서 및 CRC16 검증 로직
- CDC RX 콜백 멀티플렉싱 (디버그 텍스트 ↔ Vendor CDC 바이너리 공존)
- 명령 코드 디스패처 및 FreeRTOS 태스크 기반 처리
- 시퀀스 번호 범위 제한 (0~253, 0xFE/0xFF 예약)

---

## Phase 2.3 완료 사항 요약 (Phase 3.1 진행을 위한 필수 배경 지식)

### Phase 2에서 완료된 통신 기반

**1. Essential 모드 HID 통신 (Phase 2.1)**
- ESP32-S3 TinyUSB 기반 USB Composite 디바이스 완성
- Interface 0: HID Boot Keyboard, Interface 1: HID Boot Mouse
- Interface 2/3: CDC-ACM (현재 디버그 로깅 전용)
- Windows HID 기본 드라이버 자동 인식

**2. UART 통신 (Phase 2.2)**
- Android → ESP32-S3 UART 8바이트 프레임 전송
- 1Mbps (8N1), CH343P USB-to-UART 브릿지
- BridgeFrame: seq(1B) + buttons(1B) + deltaX(1B) + deltaY(1B) + wheel(1B) + modifiers(1B) + keyCode1(1B) + keyCode2(1B)

**3. 통신 검증 (Phase 2.3)**
- 평균 지연시간 ~16ms (목표 50ms 달성)
- 프레임 손실률 0% (목표 0.1% 달성)
- BIOS 호환성 검증 완료

### 현재 CDC 인터페이스 상태

현재 CDC 인터페이스는 `usb_cdc_log.c`에서 **디버그 로그 출력 전용**으로 사용 중입니다:
- `usb_cdc_log_init()`: ESP_LOG를 CDC로 리다이렉트
- `tud_cdc_rx_cb()`: 텍스트 명령 수신 처리 ("reset", "help")
- `tud_cdc_line_state_cb()`: DTR 신호로 연결/해제 감지
- LF → CRLF 자동 변환, `tud_cdc_write_flush()` 호출

Phase 3.1에서는 이 단일 CDC 인터페이스를 **디버그 로깅과 Vendor CDC 프로토콜이 공유**하도록 수정합니다.

---

## 사전 조치: 시퀀스 번호 범위 제한

### 배경

Phase 3.6에서 ESP32-S3 → Android 역방향 UART 알림 프레임(0xFE 헤더)을 도입합니다. 현재 UART 입력 프레임의 seq 필드는 0~255 전체를 순환하므로, seq=0xFE인 프레임과 역방향 알림 프레임이 **충돌**합니다.

### 조치

Phase 3.1 시작 시 seq 범위를 **0~253으로 제한**하여 0xFE와 0xFF를 프로토콜 예약 바이트로 확보합니다.

**수정 대상**:
- `src/android/app/src/main/java/com/bridgeone/app/protocol/FrameBuilder.kt`: `% 256` → `% 254`
- `src/board/BridgeOne/main/uart_handler.c`: seq 검증 시 0xFE/0xFF는 무효 프레임으로 처리

### 예약 바이트 정의
| 바이트 | 용도 |
|--------|------|
| 0x00~0xFD | 일반 시퀀스 번호 (입력 프레임) |
| 0xFE | 역방향 알림 프레임 헤더 (Phase 3.6) |
| 0xFF | 미래 예약 |

---

## Phase 3.1.1: Vendor CDC 프레임 구조 및 CRC16 구현

**목표**: Vendor CDC 바이너리 프레임의 구조체 정의 및 CRC16-CCITT 검증 함수 구현

**개발 기간**: 1-2일

**세부 목표**:
1. `vendor_cdc_handler.h` 헤더 파일 생성
2. Vendor CDC 프레임 구조체 정의:
   ```
   ┌────────┬─────────┬────────────┬──────────┬──────────┐
   │ Header │ Command │ Length(LE) │ Payload  │ CRC16(LE)│
   │  0xFF  │   1B    │    2B      │ 0~448B   │   2B     │
   └────────┴─────────┴────────────┴──────────┴──────────┘
   ```
3. CRC16-CCITT 계산 함수 구현 (다항식 0x1021, 초기값 0x0000)
   - CRC 계산 범위: **payload만** (header, command, length 제외)
   - Windows 서버의 C# 구현과 정확히 동일한 알고리즘
4. 프레임 조립 함수 구현 (`vendor_cdc_send_frame`)
   - command, payload, payload_length를 받아 완전한 프레임 구성
   - CRC16 자동 계산 및 부착
   - `tud_cdc_write()` + `tud_cdc_write_flush()`로 전송

**명령 코드 정의**:
```c
typedef enum {
    VCDC_CMD_AUTH_CHALLENGE  = 0x01,  // Server→ESP: 인증 챌린지
    VCDC_CMD_AUTH_RESPONSE   = 0x02,  // ESP→Server: 인증 응답
    VCDC_CMD_STATE_SYNC      = 0x03,  // Server→ESP: 상태 동기화 요청
    VCDC_CMD_STATE_SYNC_ACK  = 0x04,  // ESP→Server: 상태 동기화 확인
    VCDC_CMD_PING            = 0x10,  // Server→ESP: Keep-alive ping
    VCDC_CMD_PONG            = 0x11,  // ESP→Server: Keep-alive pong
    VCDC_CMD_MODE_NOTIFY     = 0x20,  // ESP→Server: 모드 변경 알림
    VCDC_CMD_ERROR           = 0xFE,  // 양방향: 오류 응답
} vendor_cdc_cmd_t;
```

**신규 파일**:
- `src/board/BridgeOne/main/vendor_cdc_handler.h`
- `src/board/BridgeOne/main/vendor_cdc_handler.c`

**참조 문서 및 섹션**:
- `docs/windows/technical-specification-server.md` §2.3.3 확장 경로: Vendor CDC
- `docs/windows/technical-specification-server.md` CRC16 계산 코드 (C# 참조 구현, 246~261행)
- `docs/board/esp32s3-code-implementation-guide.md` §1.3 USB Composite 디바이스 설계 계약

**검증**:
- [ ] `vendor_cdc_handler.h` 파일 생성됨
- [ ] `vendor_cdc_handler.c` 파일 생성됨
- [ ] Vendor CDC 프레임 구조체 정의됨 (header, command, length, payload, crc16)
- [ ] 명령 코드 열거형 정의됨 (8개 명령)
- [ ] CRC16-CCITT 함수 구현됨 (다항식 0x1021, 초기값 0x0000)
- [ ] CRC16 계산 범위가 payload만인 것을 확인
- [ ] `vendor_cdc_send_frame()` 함수 구현됨
- [ ] 최대 페이로드 크기 448B 제한 검증 로직 포함
- [ ] `idf.py build` 성공

---

## Phase 3.1.2: 프레임 파싱 상태 머신 구현

**목표**: CDC 수신 스트림에서 Vendor CDC 바이너리 프레임을 정확히 추출하는 상태 머신 구현

**개발 기간**: 1일

**세부 목표**:
1. 프레임 파싱 상태 머신 구현:
   ```
   WAIT_HEADER → READ_COMMAND → READ_LENGTH → READ_PAYLOAD → READ_CRC → FRAME_COMPLETE
       ↑                                                                      │
       └──────────────────────── (검증 후 리셋) ──────────────────────────────┘
   ```
   - `WAIT_HEADER`: 0xFF 바이트 대기. 0xFF가 아닌 바이트는 무시 (디버그 텍스트로 간주)
   - `READ_COMMAND`: command 바이트 1개 읽기
   - `READ_LENGTH`: 2바이트 length 읽기 (Little-Endian)
   - `READ_PAYLOAD`: length만큼 payload 읽기 (최대 448B)
   - `READ_CRC`: 2바이트 CRC16 읽기 (Little-Endian) 및 검증
   - `FRAME_COMPLETE`: 검증 성공 시 FreeRTOS 큐에 프레임 전달
2. 파싱 실패 처리:
   - length > 448: 에러 로그 + 상태 리셋
   - CRC 불일치: 에러 응답 프레임 전송 + 상태 리셋
   - 타임아웃 (프레임 수신 중 500ms 이상 데이터 없음): 상태 리셋
3. 파싱 버퍼:
   - 정적 버퍼 사용 (최대 프레임 크기 454B)
   - 메모리 동적 할당 금지 (임베디드 환경 안정성)

**참조 문서 및 섹션**:
- `docs/windows/technical-specification-server.md` §2.3.3 Vendor CDC 오류 처리
- `src/board/BridgeOne/main/uart_handler.c` - 기존 UART 프레임 파싱 패턴 참조

**검증**:
- [ ] 파싱 상태 머신 5단계 구현됨
- [ ] 올바른 프레임 수신 시 FreeRTOS 큐에 전달됨
- [ ] length > 448 시 에러 처리 및 상태 리셋
- [ ] CRC 불일치 시 에러 응답 프레임 전송
- [ ] 파싱 중 타임아웃 시 상태 리셋
- [ ] 정적 버퍼 사용 (동적 할당 없음)
- [ ] `idf.py build` 성공

---

## Phase 3.1.3: CDC RX 콜백 멀티플렉싱

**목표**: 기존 `tud_cdc_rx_cb` 콜백을 수정하여 디버그 텍스트 명령과 Vendor CDC 바이너리 프레임을 자동 분류

**개발 기간**: 0.5-1일

**세부 목표**:
1. `usb_cdc_log.c`의 `tud_cdc_rx_cb()` 수정:
   - CDC 수신 버퍼에서 데이터 읽기
   - 첫 바이트가 `0xFF`이면 → `vendor_cdc_handler`의 프레임 파서로 위임
   - 첫 바이트가 `0xFF`가 아니면 → 기존 텍스트 명령 처리 유지 ("reset", "help")
2. 혼합 데이터 처리:
   - CDC 버퍼에 텍스트와 바이너리가 연속으로 들어올 수 있음
   - 바이트 단위로 분류하여 적절한 핸들러로 전달
3. 기존 디버그 로그 출력(ESP_LOG → CDC)은 변경 없이 유지

**수정 파일**:
- `src/board/BridgeOne/main/usb_cdc_log.c`: `tud_cdc_rx_cb()` 수정
- `src/board/BridgeOne/main/usb_cdc_log.h`: `vendor_cdc_handler.h` 인클루드 추가

**참조 문서 및 섹션**:
- `src/board/BridgeOne/main/usb_cdc_log.c` - 기존 CDC RX 처리 코드
- TinyUSB CDC API: `tud_cdc_read()`, `tud_cdc_available()`

**검증**:
- [ ] 0xFF로 시작하는 바이너리 → vendor_cdc_handler로 전달됨
- [ ] "help" 텍스트 명령 → 기존 텍스트 처리로 전달됨
- [ ] "reset" 텍스트 명령 → 기존 텍스트 처리로 전달됨
- [ ] 디버그 로그 출력 (ESP_LOG → CDC)이 정상 동작
- [ ] 혼합 데이터 (텍스트 + 바이너리) 수신 시 정상 분류
- [ ] `idf.py build` 성공

---

## Phase 3.1.4: FreeRTOS 태스크 및 명령 디스패처

**목표**: Vendor CDC 프레임을 처리하는 전용 FreeRTOS 태스크와 명령별 핸들러 디스패처 구현

**개발 기간**: 1-1.5일

**세부 목표**:
1. `vendor_cdc_task` FreeRTOS 태스크 생성:
   - Priority 3 (기존: UART=6, HID=5, USB=4)
   - Core 0에서 실행
   - 스택 크기: 4096 bytes (JSON 파싱을 위한 여유 확보)
   - FreeRTOS 큐에서 파싱된 프레임 수신 대기
2. JSON 페이로드 파싱:
   - ESP-IDF 내장 `cJSON` 라이브러리 활용
   - payload를 null-terminate 후 `cJSON_Parse()` 호출
   - 파싱 실패 시 에러 응답 프레임 전송
3. 명령 디스패처 구현:
   - command 코드에 따라 적절한 핸들러 함수 호출
   - 핸들러 함수 포인터 테이블 방식
   - Phase 3.1에서는 스켈레톤 핸들러만 구현 (로그 출력 + 에코백)
   - 실제 핸들러 로직은 Phase 3.3~3.5에서 구현
4. `BridgeOne.c`의 `app_main()`에서 태스크 생성 추가

**신규/수정 파일**:
- `src/board/BridgeOne/main/vendor_cdc_handler.c`: 태스크 및 디스패처 구현
- `src/board/BridgeOne/main/BridgeOne.c`: `vendor_cdc_task` 생성 코드 추가
- `src/board/BridgeOne/main/CMakeLists.txt`: SRCS에 `vendor_cdc_handler.c` 추가, REQUIRES에 `json` (cJSON) 추가 여부 확인

**참조 문서 및 섹션**:
- `src/board/BridgeOne/main/BridgeOne.c` - 기존 태스크 생성 패턴
- ESP-IDF cJSON 문서: `cJSON_Parse()`, `cJSON_GetObjectItem()`, `cJSON_Delete()`
- `docs/windows/technical-specification-server.md` §3.4 메시지 형식

**⚠️ 주의: TinyUSB 콜백 내 무거운 처리 금지**
- `tud_cdc_rx_cb()`는 TinyUSB 컨텍스트에서 호출됨
- JSON 파싱 등 시간이 오래 걸리는 작업은 콜백 내에서 수행하면 안 됨
- 콜백에서는 FreeRTOS 큐에 데이터를 넣기만 하고, 실제 처리는 `vendor_cdc_task`에서 수행

**검증**:
- [ ] `vendor_cdc_task` FreeRTOS 태스크 생성됨 (Priority 3)
- [ ] 파싱된 프레임이 큐를 통해 태스크에 전달됨
- [ ] JSON 페이로드 파싱 성공 (cJSON 사용)
- [ ] 명령 디스패처가 command 코드별로 핸들러 호출
- [ ] 지원하지 않는 command 수신 시 에러 응답
- [ ] JSON 파싱 실패 시 에러 응답 프레임 전송
- [ ] PC에서 시리얼 터미널로 0xFF 바이너리 프레임 수동 전송 → ESP32-S3 파싱 성공 (디버그 로그 확인)
- [ ] 기존 텍스트 명령 ("help", "reset") 정상 동작 유지
- [ ] CRC 오류 시 에러 응답 프레임이 CDC로 전송됨
- [ ] `idf.py build` 성공

---

## Phase 3.1 기술 참고사항

### CDC 버퍼 크기 제약
- `tusb_config.h`: `CFG_TUD_CDC_RX_BUFSIZE=512`, `CFG_TUD_CDC_TX_BUFSIZE=512`
- 최대 Vendor CDC 프레임 크기: 1+1+2+448+2 = **454바이트**
- 버퍼 여유: 512 - 454 = **58바이트** (매우 적음)
- 연속 전송 시 반드시 `tud_cdc_write_flush()` 호출하여 오버플로우 방지
- 필요 시 `CFG_TUD_CDC_TX_BUFSIZE`를 1024로 증가 고려

### ESP32-S3 메모리 영향
- cJSON 라이브러리: ESP-IDF에 내장, 추가 메모리 영향 최소
- Vendor CDC 정적 버퍼: ~454B (프레임 파싱용)
- FreeRTOS 큐: 프레임 5개 × ~460B = ~2.3KB
- vendor_cdc_task 스택: 4096B
- **총 추가 메모리**: ~7KB (N16R8 보드의 8MB PSRAM 대비 무시 가능)

### FreeRTOS 태스크 우선순위 체계 (Phase 3.1 이후)
| 우선순위 | 태스크 | 역할 |
|---------|--------|------|
| 6 | uart_task | Android UART 프레임 수신 |
| 5 | hid_task | HID 리포트 생성 및 전송 |
| 4 | usb_task | TinyUSB 스택 폴링 |
| 3 | vendor_cdc_task | **Vendor CDC 프레임 처리 (신규)** |

---

## Phase 3.1 핵심 성과

**Phase 3.1 완료 시 달성되는 상태**:
- ✅ ESP32-S3가 CDC를 통해 Vendor CDC 바이너리 프레임을 수신/파싱 가능
- ✅ 기존 디버그 로깅과 Vendor CDC 프로토콜이 단일 CDC 인터페이스에서 공존
- ✅ CRC16 검증으로 데이터 무결성 보장
- ✅ 명령 디스패처 프레임워크 구축 (Phase 3.3~3.5에서 실제 핸들러 추가)
- ✅ seq 범위 제한으로 Phase 3.6 역방향 UART 준비 완료
- ✅ Phase 3.2 (Windows CDC 연결)의 선행 조건 충족
