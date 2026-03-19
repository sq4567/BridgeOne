---
title: "BridgeOne Phase 3.6: Android 모드 인지 (역방향 UART 통신)"
description: "BridgeOne 프로젝트 Phase 3.6 - ESP32-S3→Android 역방향 UART 알림 프레임으로 모드 변경을 Android에 전달"
tags: ["android", "uart", "reverse-communication", "mode-awareness", "esp32-s3"]
version: "v1.0"
owner: "Chatterbones"
updated: "2026-03-19"
---

# BridgeOne Phase 3.6: Android 모드 인지 (역방향 UART 통신)

**개발 기간**: 3-4일

**목표**: ESP32-S3 → Android 역방향 UART 통신을 구현하여, Android 앱이 현재 모드(Essential/Standard)를 인지하고 UI를 적응시킵니다.

**핵심 성과물**:
- 역방향 UART 알림 프레임 프로토콜 정의 (0xFE 헤더, 8바이트)
- ESP32-S3 UART TX를 통한 모드 변경 알림 전송
- Android UsbSerialManager의 UART 수신 파싱 추가
- Android 모드 상태 관리 (StateFlow) 및 UI 전환

**선행 조건**: Phase 3.5 (모드 전환 시스템) 완료 + Phase 3.1 사전 조치 (seq 범위 0~253 제한)

---

## 📋 Phase 3.5 완료 사항 요약

- Essential ↔ Standard 자동 모드 전환 동작
- Standard 모드: wheel, drag, right_click 활성화
- Essential 모드: wheel=0 강제, Boot 키만 허용
- 모드 전환 시 입력 손실 없음

### ⚠️ 사전 조치 확인 (Phase 3.1에서 완료)

Phase 3.1에서 seq 범위를 0~253으로 제한하여 0xFE와 0xFF를 예약 바이트로 확보했습니다:
- `FrameBuilder.kt`: `% 256` → `% 254`
- `uart_handler.c`: seq=0xFE/0xFF 수신 시 무효 프레임 처리

---

## 역방향 UART 프로토콜

### 기존 통신 vs 역방향 통신

| 구분 | 기존 (Android → ESP32-S3) | 역방향 (ESP32-S3 → Android) |
|------|--------------------------|---------------------------|
| 방향 | Android → ESP32-S3 | ESP32-S3 → Android |
| 헤더 | seq (0x00~0xFD) | **0xFE** (고정) |
| 크기 | 8바이트 | 8바이트 (동일) |
| 용도 | 입력 프레임 (마우스/키보드) | 이벤트 알림 (모드 변경 등) |
| 빈도 | 120Hz (입력 시) | 이벤트 발생 시만 |

### 역방향 알림 프레임 구조 (8바이트)

```
┌────────┬────────────┬────────┬──────────────────────────┐
│ Header │ Event Type │  Data  │      Reserved (5B)       │
│  0xFE  │    1B      │   1B   │  0x00 0x00 0x00 0x00 0x00│
└────────┴────────────┴────────┴──────────────────────────┘
```

| 필드 | 크기 | 설명 |
|------|------|------|
| Header | 1B | **0xFE** 고정 (알림 프레임 식별자) |
| Event Type | 1B | 이벤트 종류 |
| Data | 1B | 이벤트별 데이터 |
| Reserved | 5B | 예약 (0x00 패딩, 향후 확장용) |

### 이벤트 타입 정의

| Event Type | 이름 | Data 필드 |
|-----------|------|-----------|
| 0x01 | MODE_CHANGED | 0x00=Essential, 0x01=Standard |
| 0x02 | CONNECTION_STATE | 0x00=Disconnected, 0x01=Connecting, 0x02=Connected |
| 0x03~0x0F | 예약 | 향후 확장용 |

### 프레임 구분 로직 (Android 수신 측)

Android가 UART에서 8바이트를 수신했을 때:
1. 첫 바이트가 `0xFE`이면 → **역방향 알림 프레임**
2. 첫 바이트가 `0x00~0xFD`이면 → **일반 입력 프레임** (무시, Android→ESP 단방향이므로 에코 아님)

---

## Phase 3.6.1: ESP32-S3 역방향 UART 전송

**목표**: ESP32-S3가 모드 변경 시 UART TX를 통해 Android에 알림 프레임 전송

**개발 기간**: 1일

**세부 목표**:
1. `uart_handler.c/h`에 역방향 전송 함수 추가:
   ```c
   /**
    * ESP32-S3 → Android 알림 프레임 전송
    * @param event_type 이벤트 종류 (EVENT_MODE_CHANGED 등)
    * @param data 이벤트 데이터
    */
   void uart_send_notification(uint8_t event_type, uint8_t data);
   ```
2. 알림 프레임 조립:
   - 8바이트 버퍼: `{0xFE, event_type, data, 0x00, 0x00, 0x00, 0x00, 0x00}`
   - `uart_write_bytes()` (ESP-IDF UART API)로 전송
3. 모드 변경 콜백 연동:
   - `connection_state.c`의 모드 변경 콜백에서 `uart_send_notification()` 호출
   - Essential → Standard: `uart_send_notification(0x01, 0x01)`
   - Standard → Essential: `uart_send_notification(0x01, 0x00)`
4. 알림 전송 신뢰성:
   - 모드 변경 시 알림을 **3회 반복 전송** (50ms 간격)
   - Android가 1개라도 수신하면 충분 (중복 수신은 Android에서 처리)

**수정 파일**:
- `src/board/BridgeOne/main/uart_handler.c`: `uart_send_notification()` 함수 추가
- `src/board/BridgeOne/main/uart_handler.h`: 함수 선언 및 이벤트 타입 정의
- `src/board/BridgeOne/main/connection_state.c`: 모드 변경 시 알림 전송 호출

**참조 문서 및 섹션**:
- `src/board/BridgeOne/main/uart_handler.c` - 기존 UART 수신 코드 참조
- ESP-IDF UART 문서: `uart_write_bytes()`
- `docs/board/esp32s3-code-implementation-guide.md` §1.3.3 UART 프로토콜

**⚠️ UART 방향 주의**:
- 기존: Android(TX) → ESP32-S3(RX) 단방향
- 추가: ESP32-S3(TX) → Android(RX) 역방향
- UART0 (GPIO43=TX, GPIO44=RX)는 양방향 지원이므로 물리적 변경 불필요
- Android USB Serial 라이브러리(usb-serial-for-android)는 수신 기능 내장

**검증**:
- [ ] `uart_send_notification()` 함수 구현됨
- [ ] 모드 변경 시 알림 프레임 3회 전송 (50ms 간격)
- [ ] 알림 프레임이 8바이트, 0xFE 헤더 확인
- [ ] 로직 분석기 또는 시리얼 터미널로 알림 프레임 캡처 확인
- [ ] `idf.py build` 성공

---

## Phase 3.6.2: Android UART 수신 및 알림 파싱

**목표**: Android 앱에서 UART 수신 스레드를 추가하여 ESP32-S3의 알림 프레임을 파싱

**개발 기간**: 1-1.5일

**세부 목표**:
1. `UsbSerialManager`에 UART 수신 기능 추가:
   - `usb-serial-for-android` 라이브러리의 `SerialInputOutputManager` 활용
   - 또는 직접 `UsbDeviceConnection.bulkTransfer()` 기반 수신 스레드 구현
   - 수신 데이터를 8바이트 단위로 프레임 정렬
2. 프레임 분류:
   - 첫 바이트 == 0xFE → 알림 프레임으로 파싱
   - 그 외 → 무시 (Android가 보낸 데이터의 에코가 아닌 이상 의미 없음)
3. 알림 프레임 파서:
   ```kotlin
   data class NotificationFrame(
       val eventType: UByte,  // 이벤트 종류
       val data: UByte        // 이벤트 데이터
   ) {
       companion object {
           const val HEADER: UByte = 0xFEu
           const val EVENT_MODE_CHANGED: UByte = 0x01u
           const val EVENT_CONNECTION_STATE: UByte = 0x02u

           fun parse(bytes: ByteArray): NotificationFrame? {
               if (bytes.size < 3 || bytes[0].toUByte() != HEADER) return null
               return NotificationFrame(
                   eventType = bytes[1].toUByte(),
                   data = bytes[2].toUByte()
               )
           }
       }
   }
   ```
4. 중복 알림 처리:
   - ESP32-S3가 3회 반복 전송하므로 동일 이벤트 중복 수신 가능
   - 마지막 수신 이벤트와 동일하면 무시 (디바운스)

**신규 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/protocol/NotificationFrame.kt`

**수정 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/usb/UsbSerialManager.kt`: 수신 스레드 추가

**참조 문서 및 섹션**:
- `src/android/app/src/main/java/com/bridgeone/app/usb/UsbSerialManager.kt` - 기존 USB Serial 코드
- usb-serial-for-android 라이브러리 문서: `SerialInputOutputManager`

**검증**:
- [ ] UART 수신 스레드가 정상 동작
- [ ] 0xFE 헤더 알림 프레임 파싱 성공
- [ ] 중복 알림 디바운스 처리
- [ ] 알림 이외의 데이터 수신 시 무시
- [ ] Android Studio에서 빌드 및 실행 성공

---

## Phase 3.6.3: Android 모드 상태 관리 및 UI 전환

**목표**: Android 앱에서 현재 모드를 StateFlow로 관리하고, UI를 모드에 따라 적응

**개발 기간**: 1-1.5일

**세부 목표**:
1. `BridgeMode` 모드 정의:
   ```kotlin
   enum class BridgeMode {
       ESSENTIAL,  // 기본 모드 (서버 미연결)
       STANDARD    // 서버 연결 모드
   }
   ```
2. 모드 상태 관리:
   - `StateFlow<BridgeMode>`로 현재 모드 관리
   - 알림 프레임 수신 시 모드 업데이트
   - 앱 시작 시 기본값: `ESSENTIAL`
3. UI 모드 전환:
   - **Essential → Standard 전환 시**:
     - 토스트 알림: "Standard 모드로 전환되었습니다" (2초)
     - 터치패드: wheel 스크롤 제스처 활성화 (Phase 4+ 구현)
     - 키보드: 확장 키보드 레이아웃 활성화 (Phase 4+ 구현)
     - 현재 Phase 3에서는 상태 표시만 변경
   - **Standard → Essential 복귀 시**:
     - 토스트 알림: "Essential 모드로 전환되었습니다" (2초)
     - 터치패드: 기본 모드 (이동 + 좌클릭만)
     - 키보드: Boot Keyboard Cluster만 표시
4. 모드 변경 시 프레임 생성 변경:
   - Essential: `FrameBuilder.buildFrame()`에서 wheel=0 강제
   - Standard: wheel 값 그대로 전달

**신규 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/protocol/BridgeMode.kt`

**수정 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/usb/UsbSerialManager.kt`: 모드 StateFlow 노출
- `src/android/app/src/main/java/com/bridgeone/app/ui/BridgeOneApp.kt`: 모드 구독 및 UI 전환
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/TouchpadWrapper.kt`: 모드별 동작

**참조 문서 및 섹션**:
- `docs/android/styleframe-essential.md` §3 허용/비활성 기능
- `docs/android/styleframe-essential.md` §7 구현 메모
- `docs/android/design-guide-app.md` - 모드 전환 UI 가이드

**검증**:
- [ ] `BridgeMode` enum 정의됨
- [ ] `StateFlow<BridgeMode>` 모드 상태 관리 동작
- [ ] 알림 수신 시 모드 상태 업데이트
- [ ] Essential → Standard 전환 시 토스트 표시
- [ ] Standard → Essential 복귀 시 토스트 표시
- [ ] Essential 모드에서 wheel=0 강제 (Android 측)
- [ ] Android Studio에서 빌드 및 실행 성공

---

## Phase 3.6 E2E 검증

1. **전체 플로우**: Android 앱 시작 (Essential) → 서버 연결 → ESP32-S3 알림 → Android Standard 전환 → 서버 종료 → ESP32-S3 알림 → Android Essential 복귀
2. **알림 신뢰성**: 모드 변경 시 3회 반복 전송 → Android가 최소 1회 수신 확인
3. **UI 반응성**: 모드 변경 알림 수신 → UI 전환까지 500ms 이내
4. **중복 처리**: 3회 반복 알림 중 첫 번째만 처리, 나머지 무시

---

## Phase 3.6 핵심 성과

**Phase 3.6 완료 시 달성되는 상태**:
- ✅ 3개 컴포넌트(Android, ESP32-S3, Windows) 모두 현재 모드 인지
- ✅ 모드 변경이 전체 시스템에 자동 전파
- ✅ Android UI가 모드에 따라 적응
- ✅ 역방향 UART 통신 기반 구축 (향후 다른 알림 유형 확장 가능)

---

## Phase 3 전체 완료 성과

**Phase 3 (Standard 모드 통신 기반) 전체 완료 시**:
- ✅ Windows 서버 ↔ ESP32-S3 Vendor CDC 양방향 통신
- ✅ 2단계 핸드셰이크로 안전한 연결 확립
- ✅ Keep-alive로 실시간 연결 모니터링 및 자동 복구
- ✅ Essential ↔ Standard 자동 모드 전환
- ✅ Android 앱이 모드를 인지하고 UI 적응
- ✅ Standard 모드에서 wheel, drag, right_click 활성화
- ✅ Phase 4 (고급 기능: 멀티 커서, 매크로, 확장 키보드)의 통신 기반 완성
