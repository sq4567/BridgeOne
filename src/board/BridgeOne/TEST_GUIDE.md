# Phase 1.2.4.1: ESP32-S3 테스트 펌웨어 가이드

## 개요

이 펌웨어는 ESP32-S3 보드의 HID 출력 및 CDC 명령 처리를 단독으로 테스트할 수 있도록 구현되었습니다.

**구현 단계**: Phase 1.2.4.1  
**작성 일자**: 2025-01-18  
**Context7 공식 문서 기반**: Arduino ESP32 (Trust Score: 9.1)

---

## 테스트 명령

시리얼 모니터에서 다음 명령을 입력하여 각 기능을 테스트할 수 있습니다:

| 명령 | 기능 | 설명 |
|------|------|------|
| `m` | 마우스 테스트 | 우측으로 50픽셀 이동 (5회 × 10픽셀) |
| `c` | 클릭 테스트 | 좌클릭 down → 100ms 대기 → up |
| `k` | 키보드 테스트 | 'Hello' 문자열 입력 |
| `s` | 스크롤 테스트 | 휠 +3, -3 방향으로 스크롤 |
| `p` | CDC PING 명령 | Windows 서버로 PING 명령 전송 및 응답 대기 |
| `a` | 자동 테스트 시퀀스 | 위 5개 테스트를 순차적으로 실행 (2초 간격) |

---

## 자동 테스트 시퀀스

펌웨어 업로드 후 **setup() 완료 5초 후**에 자동으로 전체 테스트 시퀀스가 실행됩니다:

1. **마우스 테스트** (1/5)
2. **클릭 테스트** (2/5)
3. **키보드 테스트** (3/5)
4. **스크롤 테스트** (4/5)
5. **CDC PING 테스트** (5/5)

각 테스트 간 **2초 대기** 시간이 있습니다.

---

## 빌드 및 업로드

### 1. 빌드

```bash
cd src/board/BridgeOne
pio run
```

### 2. 업로드

```bash
pio run --target upload
```

### 3. 시리얼 모니터 열기

```bash
pio device monitor
```

또는 Arduino IDE의 Serial Monitor를 사용합니다:
- **Baud Rate**: 115200
- **Line Ending**: Newline

---

## 테스트 절차

### 수동 테스트

1. ESP32-S3 보드를 Windows PC에 USB 연결
2. 시리얼 모니터 열기 (115200 baud)
3. setup() 완료 메시지 확인:
   ```
   ========================================
     Phase 1.2.4.1: Test Mode Enabled
   ========================================
   ```
4. 테스트 명령 입력 (m, c, k, s, p, a)
5. 각 테스트 결과 확인:
   - `✓`: 성공
   - `✗`: 실패

### 자동 테스트

1. ESP32-S3 보드 연결
2. 펌웨어 업로드
3. **5초 대기** (자동 테스트 시작 전)
4. 자동 테스트 시퀀스 실행 확인:
   ```
   ========================================
     Auto Test Sequence Started
   ========================================
   ```
5. 각 테스트 로그 확인
6. 완료 메시지 확인:
   ```
   ========================================
     Auto Test Sequence Completed
   ========================================
   ```

---

## Windows에서 HID 장치 확인

### 1. 장치 관리자 열기

- Windows 시작 메뉴 → "장치 관리자" 검색
- 또는 `Win + X` → "장치 관리자"

### 2. HID 장치 확인

**휴먼 인터페이스 장치** 카테고리에서 다음 항목 확인:
- `HID-compliant mouse`
- `HID-compliant keyboard`

### 3. COM 포트 확인

**포트 (COM & LPT)** 카테고리에서:
- `USB Serial Device (COMx)` 또는 `ESP32-S3 USB Serial (COMx)`

---

## 예상 출력 예시

### 마우스 테스트 (m)

```
[MANUAL TEST] Mouse Test
[MOUSE TEST] Moving right 50 pixels...
[MOUSE TEST] ✓ Mouse movement completed
```

### 클릭 테스트 (c)

```
[MANUAL TEST] Click Test
[CLICK TEST] Left click down...
[CLICK TEST] Waiting 100ms...
[CLICK TEST] ✓ Click test completed
```

### 키보드 테스트 (k)

```
[MANUAL TEST] Keyboard Test
[KEYBOARD TEST] Typing 'Hello'...
[KEYBOARD TEST] ✓ Keyboard test completed
```

### 스크롤 테스트 (s)

```
[MANUAL TEST] Scroll Test
[SCROLL TEST] Scroll up (+3)...
[SCROLL TEST] Scroll down (-3)...
[SCROLL TEST] ✓ Scroll test completed
```

### CDC PING 테스트 (p)

```
[MANUAL TEST] PING Test
[PING TEST] Sending PING command to Windows...
[PING TEST] ✓ PING command sent successfully
[PING TEST] Waiting for response (3 seconds)...
[PING TEST] ✓ Response received:
{
  "status": "ok",
  "cmd": 1,
  "timestamp": 12345,
  "id": 12345
}
```

---

## 문제 해결

### HID 장치가 인식되지 않음

1. USB 케이블이 데이터 전송을 지원하는지 확인 (충전 전용 케이블 제외)
2. ESP32-S3 보드가 USB 모드로 부팅되었는지 확인
3. Windows 장치 관리자에서 "알 수 없는 장치" 확인
4. 드라이버 업데이트 또는 재설치

### 시리얼 모니터에 출력이 없음

1. 올바른 COM 포트 선택 확인
2. Baud Rate를 115200으로 설정
3. ESP32-S3 보드 리셋 버튼 누르기

### 테스트 실패

1. 시리얼 모니터에서 오류 메시지 확인
2. Windows PC에서 커서 이동, 클릭, 키보드 입력 동작 확인
3. 메모장이나 텍스트 에디터를 열고 키보드 테스트 실행

### CDC PING 응답이 없음

1. Windows 서버가 실행 중인지 확인
2. COM 포트가 Windows 서버에서 열려 있는지 확인
3. 방화벽 설정 확인

---

## 기술 스펙

### 사용된 라이브러리

- **Arduino ESP32** (v3.20017.241212) - Trust Score: 9.1
  - `USBHIDMouse`: 마우스 HID 리포트 전송
  - `USBHIDKeyboard`: 키보드 HID 리포트 전송
  - `Serial` (USB CDC): Windows와 CDC 통신
- **ArduinoJson** (v6.21.5): JSON 메시지 파싱 및 생성
- **CRC** (v1.0.3): CRC16 체크섬 계산

### 메모리 사용량

- **RAM**: 6.2% (20,396 bytes / 327,680 bytes)
- **Flash**: 9.4% (314,821 bytes / 3,342,336 bytes)

### FreeRTOS 태스크

1. **UART_RX** (Core 1, Priority 3): UART 수신
2. **HID_TX** (Core 1, Priority 3): HID 전송
3. **CDC_RX** (Core 0, Priority 2): CDC 수신
4. **DEBUG** (Core 0, Priority 1): 디버그 출력

---

## 다음 단계

이 테스트 펌웨어로 HID 출력 및 CDC 명령 처리가 정상 동작하는 것을 확인한 후:

1. **Phase 1.2.4.2**: Windows 서버 통합 테스트 및 HID 장치 인식
2. **Phase 1.2.4.3**: HID 마우스 입력 E2E 테스트
3. **Phase 1.2.4.4**: HID 키보드 입력 E2E 테스트
4. **Phase 1.2.4.5**: Vendor CDC 명령 송수신 테스트
5. **Phase 1.2.4.6**: 1분간 연속 HID 전송 안정성 테스트

---

## 참고 문서

- **개발 계획 체크리스트**: `Docs/development-plan-checklist.md` §1.2.4.1
- **ESP32-S3 구현 가이드**: `Docs/Board/esp32s3-code-implementation-guide.md`
- **Arduino ESP32 공식 문서**: https://docs.espressif.com/projects/arduino-esp32
- **Context7 라이브러리 정보**: https://github.com/espressif/arduino-esp32 (Trust Score: 9.1)

---

## 라이선스

이 코드는 BridgeOne 프로젝트의 일부이며, MIT 라이선스를 따릅니다.

