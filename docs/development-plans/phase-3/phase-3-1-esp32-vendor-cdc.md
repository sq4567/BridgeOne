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

## ~~사전 조치: 시퀀스 번호 범위 제한~~ ✅ 완료 (커밋 2dfcde6)

> **이 작업은 이미 완료되었습니다.**
> - `FrameBuilder.kt`: `SEQ_MODULUS = 254` (0~253 순환) 적용됨
> - `uart_handler.c`: 0xFE/0xFF 예약 바이트 무효 프레임 처리 적용됨

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
    VCDC_CMD_ZOOM_STATE      = 0x30,  // ESP→Server: AbsolutePointingPad 줌 상태
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
- [x] `vendor_cdc_handler.h` 파일 생성됨
- [x] `vendor_cdc_handler.c` 파일 생성됨
- [x] Vendor CDC 프레임 구조체 정의됨 (header, command, length, payload, crc16)
- [x] 명령 코드 열거형 정의됨 (8개 명령)
- [x] CRC16-CCITT 함수 구현됨 (다항식 0x1021, 초기값 0x0000)
- [x] CRC16 계산 범위가 payload만인 것을 확인
- [x] `vendor_cdc_send_frame()` 함수 구현됨
- [x] 최대 페이로드 크기 448B 제한 검증 로직 포함
- [x] `idf.py build` 성공

---

## ~~Phase 3.1.2: 프레임 파싱 상태 머신 구현~~ ✅ 완료

> **이 작업은 이미 완료되었습니다.**
> - `vendor_cdc_handler.c`: 5단계 상태 머신 (`WAIT_HEADER` → `READ_COMMAND` → `READ_LENGTH` → `READ_PAYLOAD` → `READ_CRC`) 구현됨
> - `vendor_cdc_handler.h`: 파서 API 4개 함수 선언 (`vendor_cdc_parser_init`, `vendor_cdc_parser_feed`, `vendor_cdc_parser_reset`, `vendor_cdc_parser_is_active`)
> - `vendor_cdc_frame_queue`: FreeRTOS 큐 (5개 프레임, 정적 컨텍스트) 생성됨
> - CRC 불일치 시 `VCDC_CMD_ERROR` 에러 응답 프레임 자동 전송
> - 500ms 타임아웃 감지 (`esp_timer_get_time()` 기반)
> - 모든 버퍼 정적 할당 (동적 메모리 할당 없음)
>
> **계획 대비 변경 사항**:
> - `FRAME_COMPLETE`는 별도 상태가 아닌 `READ_CRC` 완료 시 즉시 처리하는 방식으로 구현 (불필요한 상태 전이 제거)
> - `vendor_cdc_parser_init()` 함수가 큐 생성을 담당하므로, Phase 3.1.4에서 `app_main()`에 `vendor_cdc_parser_init()` 호출을 추가해야 함
> - 파서는 `vendor_cdc_parser_feed()` 함수를 통해 바이트 배열을 받으므로, Phase 3.1.3에서 `tud_cdc_rx_cb()`가 수신 바이트를 이 함수에 전달하면 됨

**검증**:
- [x] 파싱 상태 머신 5단계 구현됨
- [x] 올바른 프레임 수신 시 FreeRTOS 큐에 전달됨
- [x] length > 448 시 에러 처리 및 상태 리셋
- [x] CRC 불일치 시 에러 응답 프레임 전송
- [x] 파싱 중 타임아웃 시 상태 리셋
- [x] 정적 버퍼 사용 (동적 할당 없음)
- [x] `idf.py build` 성공

---

## ~~Phase 3.1.3: CDC RX 콜백 멀티플렉싱~~ ✅ 완료

> **이 작업은 이미 완료되었습니다.**
> - `usb_cdc_log.c`: `vendor_cdc_handler.h` 인클루드 추가, `tud_cdc_rx_cb()` 멀티플렉싱 구현
> - 바이트별로 파서 상태를 확인하여 바이너리/텍스트 자동 분류
> - 기존 텍스트 명령 처리 ("reset", "help"), 에코백, Backspace 처리 모두 유지
> - 디버그 로그 출력 (ESP_LOG → CDC) 변경 없음
>
> **계획 대비 변경 사항**:
> - `vendor_cdc_parser_is_active()` API를 `vendor_cdc_handler.h/.c`에 추가함
>   - 파서가 바이너리 프레임 수신 중인지 외부에서 확인할 수 있는 함수
>   - 이 함수가 없으면 바이너리 프레임의 payload 바이트가 텍스트로 에코/버퍼링되는 문제 발생
>   - Phase 3.1.4 등 후속 작업에서 이 API를 활용할 수 있음
> - 원래 계획은 전체 버퍼를 파서에 전달하는 방식이었으나, 바이트별 분류 방식으로 변경
>   - 이유: 바이너리 프레임 중간의 바이트가 텍스트 에코되는 것을 정확히 방지하기 위함

**검증**:
- [x] 0xFF로 시작하는 바이너리 → `vendor_cdc_parser_feed()`로 전달됨
- [x] "help" 텍스트 명령 → 기존 텍스트 처리로 전달됨
- [x] "reset" 텍스트 명령 → 기존 텍스트 처리로 전달됨
- [x] 디버그 로그 출력 (ESP_LOG → CDC)이 정상 동작
- [x] 혼합 데이터 (텍스트 + 바이너리) 수신 시 정상 분류
- [x] `idf.py build` 성공

---

## ~~Phase 3.1.4: FreeRTOS 태스크 및 명령 디스패처~~ ✅ 완료

> **이 작업은 이미 완료되었습니다.**
> - `vendor_cdc_handler.c`: `vendor_cdc_task()` FreeRTOS 태스크 함수, cJSON 파싱, 명령 디스패처 구현
> - `vendor_cdc_handler.h`: `vendor_cdc_task()` 함수 선언 추가
> - `BridgeOne.c`: `vendor_cdc_parser_init()` 호출 (섹션 1.3) 및 `vendor_cdc_task` 생성 (Priority 3, Core 0, Stack 4096B)
> - `CMakeLists.txt`: REQUIRES에 `json` (cJSON 라이브러리) 추가
> - 스켈레톤 핸들러 4개 구현: PING(에코백 PONG), AUTH_CHALLENGE(에코백), STATE_SYNC(에코백 ACK), ERROR(로그)
> - 함수 포인터 테이블 기반 디스패처 (`cmd_dispatch_table[]`)
> - 미지원 명령 수신 시 에러 응답 자동 전송 (에러 코드 0x01)
>
> **계획 대비 변경 사항**:
> - JSON 파싱 실패 시 에러 응답 대신 `ESP_LOGD` 경고만 출력하도록 변경
>   - 이유: 모든 payload가 JSON인 것은 아님 (PING 등은 바이너리 payload 가능)
>   - JSON 파싱 실패는 오류가 아닌 정상 케이스일 수 있으므로 에러 응답 불필요
>   - 후속 Phase에서 JSON이 필수인 명령은 개별 핸들러에서 JSON null 검사 추가 가능
> - `vendor_cdc_task` 생성 위치를 UART/HID 태스크 생성 이전으로 배치
>   - 이유: 파서 초기화와 태스크가 CDC 데이터 수신 전에 준비되어야 함
>   - 태스크 자체는 큐 대기(portMAX_DELAY)이므로 생성 순서가 동작에 영향 없음

**검증**:
- [x] `vendor_cdc_parser_init()` 호출이 `app_main()`에 추가됨
- [x] `vendor_cdc_task` FreeRTOS 태스크 생성됨 (Priority 3)
- [x] `vendor_cdc_frame_queue`에서 파싱된 프레임이 태스크에 전달됨
- [x] JSON 페이로드 파싱 성공 (cJSON 사용)
- [x] 명령 디스패처가 command 코드별로 핸들러 호출
- [x] 지원하지 않는 command 수신 시 에러 응답
- [x] JSON 파싱 실패 시 디버그 로그 출력 (바이너리 payload 허용)
- [x] PC에서 시리얼 터미널로 0xFF 바이너리 프레임 수동 전송 → ESP32-S3 파싱 성공 (디버그 로그 확인)
- [x] 기존 텍스트 명령 ("help", "reset") 정상 동작 유지
- [x] CRC 오류 시 에러 응답 프레임이 CDC로 전송됨
- [x] `idf.py build` 성공

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
