# Phase Debugging: USB CDC Monitor 출력 문제 해결

## 문제 현황

### 증상
- **포트 2 (Micro-USB, Native USB OTG)에서 시리얼 모니터 출력이 작동하지 않음**
- 명령어: `idf.py -p COM3 monitor` 실행 시 "--- Quit: Ctrl+]..." 메시지 후 아무것도 출력 안 됨
- 보드: YD-ESP32-S3 N16R8
- 포트 1 (CH343P, COM3): Android 통신용 (정상 작동)
- 포트 2 (Native USB OTG): HID + CDC 디버그 로그용 (출력 안 됨)

### 제약 조건
1. **포트 2로만 출력 가능** (포트 1은 Android 통신 전용)
2. **UART 폴백 불가** (Android 통신 중 디버그 로그 확인 필요)
3. 기존 장치 기능 유지 필요 (HID 마우스/키보드, CDC 디버그 로그)

### 현재 코드 상태
- `usb_cdc_log.c/h` 모듈 구현됨
- `BridgeOne.c`에서 `usb_cdc_log_init()` 호출됨
- `tusb_config.h`에서 `CFG_TUD_CDC = 1` 설정됨
- USB 디스크립터에 CDC 인터페이스 정의됨

---

## 근본 원인 분석

### 핵심 문제
**TinyUSB CDC 인터페이스가 호스트에 정상적으로 열거되지만, 데이터 전송이 실제로 작동하지 않음**

### 원인 후보 (우선순위 순)

#### 원인 후보 1: TinyUSB CDC 콜백 함수 누락 (확률: 높음)
**근거**:
- TinyUSB CDC 클래스가 작동하려면 필수 콜백 함수들이 구현되어야 함
- 현재 `usb_cdc_log.c`에 콜백 함수가 구현되지 않음
- `tud_cdc_rx_cb()`, `tud_cdc_line_state_cb()` 등이 필요

**검증 방법**:
1. `usb_cdc_log.c` 파일에서 CDC 콜백 함수 검색
2. TinyUSB 공식 예제와 비교
3. 빌드 로그에서 "undefined reference to tud_cdc_*" 경고 확인

**해결 조치**:
- `usb_cdc_log.c`에 필수 TinyUSB CDC 콜백 함수 추가:
  - `void tud_cdc_line_state_cb(uint8_t itf, bool dtr, bool rts)`: DTR/RTS 상태 변경 감지
  - `void tud_cdc_rx_cb(uint8_t itf)`: 데이터 수신 처리 (디버그 로그는 수신 불필요하지만 콜백 필요)
  - `void tud_cdc_line_coding_cb(uint8_t itf, cdc_line_coding_t const* p_line_coding)`: 라인 코딩 설정 처리

#### 원인 후보 2: CFG_TUD_HID 설정 오류 (확률: 높음)
**근거**:
- `tusb_config.h`에서 `CFG_TUD_HID = 1`로 설정되어 있음
- 실제로는 2개의 HID 인터페이스(Keyboard + Mouse)가 있음
- TinyUSB는 이 값을 HID 인터페이스 개수로 사용함

**검증 방법**:
1. `tusb_config.h` 66번째 줄 확인
2. USB 디스크립터에서 HID 인터페이스 개수 확인 (2개: Keyboard, Mouse)
3. TinyUSB 소스코드에서 `CFG_TUD_HID` 사용처 확인

**해결 조치**:
- `tusb_config.h`의 `CFG_TUD_HID` 값을 1에서 2로 변경
- 이유: Keyboard + Mouse = 2개의 HID 인터페이스

#### 원인 후보 3: CDC 초기화 순서 문제 (확률: 중간)
**근거**:
- `usb_cdc_log_init()`가 TinyUSB 초기화 직후 호출됨 (BridgeOne.c:110)
- 하지만 TinyUSB가 호스트와 완전히 연결되기 전에 호출될 수 있음
- `tud_cdc_connected()` 함수가 false를 반환할 가능성

**검증 방법**:
1. `usb_cdc_log_init()`에서 `tud_cdc_connected()` 반환값 로깅
2. USB 연결 완료 후 지연 시간 추가하여 테스트
3. `tud_mount_cb()` 콜백에서 CDC 연결 상태 확인

**해결 조치**:
- `usb_cdc_log_init()`를 USB 마운트 콜백(`tud_mount_cb()`) 이후로 지연 호출
- 또는 `cdc_vprintf()` 함수에서 연결 대기 로직 추가

#### 원인 후보 4: Windows COM 포트 번호 오인 (확률: 중간)
**근거**:
- 사용자가 `idf.py -p COM3 monitor` 실행
- COM3는 포트 1 (CH343P, UART)일 가능성 높음
- 포트 2 (Native USB CDC)는 다른 COM 포트 번호를 사용할 수 있음

**검증 방법**:
1. Windows 장치 관리자 → 포트(COM & LPT) 확인
2. "USB Serial Device" 또는 "BridgeOne Vendor CDC" 항목 찾기
3. `idf.py list-ports` 명령어로 사용 가능한 포트 목록 확인

**해결 조치**:
- 장치 관리자에서 CDC 포트 번호 확인
- 올바른 COM 포트 번호로 `idf.py -p COMx monitor` 재실행
- 필요 시 USB 케이블을 포트 2 (Micro-USB)에 연결되어 있는지 확인

#### 원인 후보 5: ESP_CONSOLE 설정 충돌 (확률: 낮음)
**근거**:
- `sdkconfig`에서 `CONFIG_ESP_CONSOLE_UART_DEFAULT=y` 설정됨
- `CONFIG_ESP_CONSOLE_SECONDARY_USB_SERIAL_JTAG=y` 설정됨
- ESP-IDF 콘솔이 UART0와 USB Serial/JTAG를 동시에 사용하려고 시도
- 하지만 TinyUSB는 USB OTG를 사용하므로 USB Serial/JTAG와 충돌 가능

**검증 방법**:
1. `sdkconfig`에서 `CONFIG_ESP_CONSOLE_SECONDARY_USB_SERIAL_JTAG` 확인
2. ESP-IDF 문서에서 USB OTG와 USB Serial/JTAG 충돌 가능성 확인
3. 빌드 로그에서 USB PHY 충돌 경고 확인

**해결 조치**:
- `sdkconfig`에서 `CONFIG_ESP_CONSOLE_SECONDARY_USB_SERIAL_JTAG=n` 설정
- 또는 `idf.py menuconfig` → Component config → ESP System Settings → Channel for console secondary output → None 선택
- 재빌드 및 재플래시

#### 원인 후보 6: CDC 디스크립터 설정 오류 (확률: 낮음)
**근거**:
- USB 디스크립터에서 CDC 인터페이스가 정의되어 있음 (usb_descriptors.c:155-163)
- 하지만 엔드포인트 번호나 버퍼 크기 설정이 잘못되었을 가능성
- 특히 `EPNUM_CDC_NOTIF`, `EPNUM_CDC_OUT`, `EPNUM_CDC_IN` 값 확인 필요

**검증 방법**:
1. `usb_descriptors.h`에서 CDC 엔드포인트 번호 정의 확인
2. HID 엔드포인트와 충돌하지 않는지 확인 (예: EPNUM_HID_KB, EPNUM_HID_MOUSE)
3. Windows 장치 관리자에서 "알 수 없는 장치" 또는 "코드 10" 오류 확인

**해결 조치**:
- `usb_descriptors.h`에서 CDC 엔드포인트 번호를 HID와 겹치지 않게 재설정
- 예: EPNUM_HID_KB = 0x81, EPNUM_HID_MOUSE = 0x82, EPNUM_CDC_NOTIF = 0x83, EPNUM_CDC_IN = 0x84, EPNUM_CDC_OUT = 0x04
- 재빌드 및 재플래시

#### 원인 후보 7: TinyUSB 버전 또는 의존성 문제 (확률: 매우 낮음)
**근거**:
- ESP-IDF v5.5+에서 TinyUSB 통합 방식이 변경되었을 가능성
- `idf_component.yml`에서 TinyUSB 버전이 명시되지 않으면 최신 버전 사용
- 최신 버전에서 CDC 관련 API 변경 가능성

**검증 방법**:
1. `src/board/BridgeOne/main/idf_component.yml` 파일 확인
2. TinyUSB 버전 확인 (espressif/tinyusb)
3. 빌드 로그에서 TinyUSB 컴파일 경고 확인

**해결 조치**:
- `idf_component.yml`에서 TinyUSB 버전을 명시적으로 고정
- 예: `espressif/tinyusb: "^0.15.0"`
- `idf.py reconfigure` 및 재빌드

---

## 순차적 해결 시도 계획

### Attempt 1: CFG_TUD_HID 설정 정정 (가장 간단, 높은 성공률)
**목표**: HID 인터페이스 개수를 올바르게 설정

**단계**:
1. `tusb_config.h` 66번째 줄 수정: `#define CFG_TUD_HID 1` → `#define CFG_TUD_HID 2`
2. `idf.py fullclean` 실행
3. `idf.py build` 실행
4. `idf.py -p COM3 flash` 실행
5. 보드 수동 리셋 (RESET 버튼 또는 전원 재연결)
6. Windows 장치 관리자에서 포트 목록 확인
7. 올바른 COM 포트 번호로 `idf.py -p COMx monitor` 실행

**성공 기준**: 시리얼 모니터에 부팅 로그 및 "USB CDC logging initialized" 메시지 출력

---

### Attempt 2: TinyUSB CDC 콜백 함수 추가 (핵심 수정)
**목표**: CDC 인터페이스가 호스트와 정상적으로 통신하도록 필수 콜백 함수 구현

**단계**:
1. `usb_cdc_log.c` 파일 끝에 다음 콜백 함수 추가:

```c
/**
 * TinyUSB CDC Line State 콜백 함수.
 *
 * 호스트가 DTR(Data Terminal Ready) 또는 RTS(Request To Send) 신호를 변경하면 호출됩니다.
 * DTR=true는 보통 호스트의 시리얼 터미널이 연결되었음을 의미합니다.
 *
 * @param itf CDC 인터페이스 번호 (0-based)
 * @param dtr Data Terminal Ready 상태 (true: 연결됨, false: 연결 해제)
 * @param rts Request To Send 상태 (일반적으로 사용 안 함)
 */
void tud_cdc_line_state_cb(uint8_t itf, bool dtr, bool rts) {
    (void) rts;  // 사용하지 않는 매개변수

    if (dtr) {
        // 호스트가 CDC 포트를 열었음 (시리얼 터미널 연결)
        ESP_LOGI(TAG, "CDC interface %d connected (DTR=true)", itf);
        usb_cdc_log_write("\n\n=== BridgeOne USB CDC Debug Log ===\n");
        usb_cdc_log_write("Connected successfully. Logs will appear below.\n\n");
    } else {
        // 호스트가 CDC 포트를 닫았음 (시리얼 터미널 연결 해제)
        ESP_LOGI(TAG, "CDC interface %d disconnected (DTR=false)", itf);
    }
}

/**
 * TinyUSB CDC RX 콜백 함수.
 *
 * 호스트로부터 데이터를 수신하면 호출됩니다.
 * BridgeOne은 디버그 로그 출력만 하므로 수신 데이터를 처리하지 않지만,
 * 콜백 함수는 반드시 구현되어야 합니다.
 *
 * @param itf CDC 인터페이스 번호 (0-based)
 */
void tud_cdc_rx_cb(uint8_t itf) {
    // 수신된 데이터를 버리기 (읽어서 버퍼 비우기)
    uint8_t buf[64];
    while (tud_cdc_available()) {
        tud_cdc_read(buf, sizeof(buf));
    }
}

/**
 * TinyUSB CDC Line Coding 콜백 함수.
 *
 * 호스트가 시리얼 통신 설정(baud rate, data bits, stop bits, parity)을 변경하면 호출됩니다.
 * BridgeOne은 가상 CDC이므로 이 설정을 무시하지만, 콜백 함수는 반드시 구현되어야 합니다.
 *
 * @param itf CDC 인터페이스 번호 (0-based)
 * @param p_line_coding 라인 코딩 설정 구조체 포인터
 */
void tud_cdc_line_coding_cb(uint8_t itf, cdc_line_coding_t const* p_line_coding) {
    // 가상 CDC이므로 설정을 무시 (로그만 출력)
    ESP_LOGD(TAG, "CDC line coding changed: baud=%u, bits=%u, stop=%u, parity=%u",
             p_line_coding->bit_rate, p_line_coding->data_bits,
             p_line_coding->stop_bits, p_line_coding->parity);
}
```

2. `idf.py fullclean` 실행
3. `idf.py build` 실행 (콜백 함수 컴파일 확인)
4. `idf.py -p COM3 flash` 실행
5. 보드 수동 리셋
6. Windows 장치 관리자에서 CDC 포트 번호 확인
7. `idf.py -p COMx monitor` 실행

**성공 기준**: 시리얼 모니터에 "=== BridgeOne USB CDC Debug Log ===" 메시지 및 연속적인 로그 출력

---

### Attempt 3: Windows COM 포트 번호 재확인 및 재연결 (환경 검증)
**목표**: 올바른 COM 포트 번호로 연결하고 있는지 확인

**단계**:
1. Windows 장치 관리자 열기 (Win+X → Device Manager)
2. "포트 (COM & LPT)" 섹션 확장
3. 다음 항목 찾기:
   - "USB Serial Device (COMx)" 또는
   - "BridgeOne Vendor CDC (COMx)" 또는
   - 설명에 "CDC" 포함된 장치
4. 해당 COM 포트 번호 확인 (예: COM4, COM5 등)
5. 보드의 Micro-USB 포트 (포트 2)가 PC에 연결되어 있는지 물리적으로 확인
6. `idf.py list-ports` 실행하여 사용 가능한 포트 목록 확인
7. 올바른 COM 포트 번호로 재시도:
   ```bash
   idf.py -p COM[확인된번호] monitor
   ```

**성공 기준**:
- 장치 관리자에서 CDC 장치가 정상적으로 표시됨 (노란색 느낌표 없음)
- `idf.py monitor`로 로그 출력 확인

---

### Attempt 4: ESP_CONSOLE 설정 정정 (충돌 제거)
**목표**: USB Serial/JTAG 보조 콘솔 비활성화하여 USB OTG와 충돌 방지

**단계**:
1. `idf.py menuconfig` 실행
2. Component config → ESP System Settings → Channel for console secondary output 이동
3. "USB_SERIAL_JTAG" 대신 "None" 선택
4. 저장 (S 키) 및 종료 (Q 키)
5. 또는 `sdkconfig.defaults` 파일에 다음 추가:
   ```
   CONFIG_ESP_CONSOLE_SECONDARY_NONE=y
   # CONFIG_ESP_CONSOLE_SECONDARY_USB_SERIAL_JTAG is not set
   ```
6. `idf.py fullclean` 실행
7. `idf.py build` 실행
8. `idf.py -p COM3 flash` 실행
9. 보드 수동 리셋
10. `idf.py -p COMx monitor` 실행 (올바른 CDC 포트 번호)

**성공 기준**: USB PHY 충돌 없이 CDC 로그 정상 출력

---

### Attempt 5: USB 드라이버 재설정 (Windows 환경 초기화)
**목표**: Windows USB 드라이버 캐시 문제 해결

**단계**:
1. 보드의 모든 USB 케이블 분리 (포트 1, 포트 2 모두)
2. Windows 장치 관리자 열기
3. "포트 (COM & LPT)" 섹션에서 BridgeOne 관련 장치 모두 우클릭 → "장치 제거" 선택
4. "이 장치의 드라이버 소프트웨어 삭제" 체크박스 선택 (있으면)
5. "범용 직렬 버스 컨트롤러" 섹션에서도 BridgeOne 관련 장치 제거
6. 시스템 재부팅
7. 보드 포트 2 (Micro-USB)를 PC에 재연결
8. Windows가 자동으로 드라이버 설치하도록 대기 (30초~1분)
9. 장치 관리자에서 새로 인식된 COM 포트 확인
10. `idf.py list-ports` 실행
11. `idf.py -p COMx monitor` 실행 (올바른 CDC 포트)

**성공 기준**:
- 장치 관리자에서 "알 수 없는 장치" 없음
- CDC 포트가 정상적으로 표시됨

---

### Attempt 6: CDC 디스크립터 엔드포인트 검증 (구조적 검증)
**목표**: USB 디스크립터에서 엔드포인트 충돌 제거

**단계**:
1. `src/board/BridgeOne/main/usb_descriptors.h` 파일 확인
2. 다음 매크로 정의 확인:
   - `EPNUM_HID_KB`
   - `EPNUM_HID_MOUSE`
   - `EPNUM_CDC_NOTIF`
   - `EPNUM_CDC_IN`
   - `EPNUM_CDC_OUT`
3. 엔드포인트 번호가 중복되지 않는지 확인:
   - IN 엔드포인트: 0x81, 0x82, 0x83, 0x84 등 (최상위 비트 1)
   - OUT 엔드포인트: 0x01, 0x02, 0x03, 0x04 등 (최상위 비트 0)
4. 필요 시 충돌 제거:
   ```c
   // 예: usb_descriptors.h
   #define EPNUM_HID_KB       0x81
   #define EPNUM_HID_MOUSE    0x82
   #define EPNUM_CDC_NOTIF    0x83
   #define EPNUM_CDC_IN       0x84
   #define EPNUM_CDC_OUT      0x04
   ```
5. `idf.py fullclean && idf.py build` 실행
6. `idf.py -p COM3 flash` 실행
7. 보드 수동 리셋
8. Windows 장치 관리자에서 "코드 10" 오류 없는지 확인
9. `idf.py -p COMx monitor` 실행

**성공 기준**:
- Windows에서 장치 오류 없음
- CDC 포트 정상 작동

---

### Attempt 7: UART 폴백 테스트 (임시 검증)
**목표**: Android를 잠시 분리하고 UART 출력으로 시스템 정상 작동 확인

**단계**:
1. 포트 1 (CH343P)에 연결된 Android 케이블 분리
2. 포트 1을 PC에 연결 (USB-C 케이블)
3. `idf.py -p COM3 monitor` 실행 (포트 1의 COM 번호)
4. 부팅 로그 및 초기화 메시지 확인:
   - "BridgeOne Board - USB Composite Device Initialization"
   - "TinyUSB driver installed (USB PHY switched to USB OTG)"
   - "USB CDC logging initialized"
   - "UART task started"
   - "USB task running (loop_count=...)"
5. 로그가 정상적으로 출력되면 ESP32-S3 펌웨어 자체는 정상 작동 중임을 확인
6. Windows 이벤트 뷰어 열기:
   - Win+X → Event Viewer
   - Windows Logs → System
   - 최근 USB 장치 연결/분리 이벤트 확인
7. Android 케이블 다시 포트 1에 연결
8. 포트 2 (Micro-USB)로 CDC 로깅 재시도

**성공 기준**:
- UART 출력으로 펌웨어 정상 작동 확인
- USB 이벤트 로그에서 이상 징후 파악

---

## 검증 체크리스트

해결책 수행 후 다음 항목을 모두 확인해야 합니다:

### 포트 2 (Micro-USB) CDC 출력 정상 작동
- [ ] Windows 장치 관리자에서 CDC 장치가 "USB Serial Device (COMx)" 또는 "BridgeOne Vendor CDC"로 표시됨
- [ ] 장치에 노란색 느낌표나 오류 코드 없음
- [ ] `idf.py -p COMx monitor` (CDC 포트)로 로그 출력 확인
- [ ] 부팅 로그: "BridgeOne Board - USB Composite Device Initialization" 출력
- [ ] CDC 초기화 로그: "Debug logs redirected to USB CDC (Port 2, Micro-USB)" 출력
- [ ] 실시간 로그: "USB task running (loop_count=...)" 주기적 출력

### 포트 1 (USB-C) Android 통신 유지
- [ ] Android 스마트폰을 포트 1에 연결 시 정상 인식
- [ ] Android 앱에서 ESP32-S3 장치 감지 및 연결 성공
- [ ] 터치패드 입력이 PC로 정상 전달 (마우스 커서 이동, 키보드 입력)
- [ ] UART 통신 속도 1Mbps 유지

### HID 기능 유지
- [ ] Windows 장치 관리자 → "Human Interface Devices" 섹션에 다음 장치 표시:
  - "HID 호환 마우스" (BridgeOne Mouse)
  - "HID 키보드 장치" (BridgeOne Keyboard)
- [ ] PC에서 마우스 커서 이동 정상 작동
- [ ] PC에서 키보드 입력 정상 작동

### 두 포트 동시 작동 확인
- [ ] 포트 1에 Android 연결 + 포트 2에 PC 연결 (동시)
- [ ] CDC 로그 출력 정상 (포트 2)
- [ ] Android 터치패드 입력 정상 (포트 1 → ESP32-S3 → 포트 2 HID → PC)
- [ ] 3가지 기능 모두 동시 작동: UART (포트 1), CDC 로그 (포트 2), HID (포트 2)

---

## 예상 해결 시간

작업 복잡도에 따라 달라지지만, 단계별 시간 배분은 다음과 같습니다:

| 작업 단계 | 예상 시간 |
|----------|----------|
| 코드 검토 및 원인 분석 | 완료 |
| Attempt 1 (CFG_TUD_HID 수정) | 5분 |
| Attempt 2 (CDC 콜백 함수 추가) | 15분 |
| Attempt 3 (COM 포트 확인) | 5분 |
| Attempt 4 (ESP_CONSOLE 수정) | 10분 |
| Attempt 5 (드라이버 재설정) | 10분 |
| Attempt 6 (디스크립터 검증) | 15분 |
| Attempt 7 (UART 폴백 테스트) | 10분 |
| **최종 검증 및 문서화** | 10분 |
| **총 예상 시간** | **1시간 20분** |

---

## 향후 권장사항

문제 해결 후 다음 조치를 통해 유사 문제 재발을 방지하세요:

### 1. USB CDC 로깅 자동 테스트 추가
- CI/CD 파이프라인에 USB CDC 출력 검증 단계 추가
- 펌웨어 플래시 후 자동으로 CDC 포트 연결 및 로그 출력 확인

### 2. TinyUSB 콜백 함수 체크리스트 작성
- `docs/board/` 디렉토리에 "TinyUSB 필수 콜백 함수 목록" 문서 작성
- 새로운 USB 클래스 추가 시 필수 콜백 함수 구현 확인

### 3. USB 디스크립터 검증 스크립트 작성
- Python 스크립트로 엔드포인트 충돌 자동 검사
- `idf.py build` 후 자동 실행하여 디스크립터 유효성 검증

### 4. 개발 환경 설정 가이드 업데이트
- `CLAUDE.md` 또는 `docs/board/esp32s3-code-implementation-guide.md`에 다음 추가:
  - Windows COM 포트 확인 방법
  - USB 드라이버 재설정 절차
  - TinyUSB CDC 초기화 순서

### 5. 디버깅 로그 레벨 조정
- 개발 단계: `CONFIG_LOG_DEFAULT_LEVEL_DEBUG=y` (현재 설정)
- 프로덕션: `CONFIG_LOG_DEFAULT_LEVEL_INFO=y`로 변경하여 성능 최적화

### 6. USB 포트별 역할 명확화
- 각 소스 파일 헤더 주석에 포트 역할 명시:
  ```c
  /**
   * 포트 1 (USB-C, CH343P): Android UART 통신 전용
   * 포트 2 (Micro-USB, Native USB OTG): PC HID + CDC 디버그 로그
   */
  ```

### 7. 에러 핸들링 강화
- `usb_cdc_log_init()` 함수에서 초기화 실패 시 재시도 로직 추가
- `tud_cdc_connected()` 반환값 주기적 확인 및 연결 복구 로직 추가

### 8. TinyUSB 버전 고정
- `idf_component.yml`에 TinyUSB 버전 명시적으로 지정:
  ```yaml
  dependencies:
    espressif/tinyusb: "^0.15.0"
  ```
- 의도하지 않은 버전 업그레이드로 인한 호환성 문제 방지

---

## 참고 자료

- [ESP-IDF USB Device Stack 공식 문서](https://docs.espressif.com/projects/esp-idf/en/stable/esp32s3/api-reference/peripherals/usb_device.html)
- [TinyUSB CDC Class 구현 가이드](https://github.com/hathach/tinyusb/blob/master/docs/reference/classes/cdc.md)
- [ESP32-S3 USB OTG 하드웨어 가이드](https://docs.espressif.com/projects/esp-idf/en/stable/esp32s3/hw-reference/esp32s3/user-guide-devkitc-1.html#usb)
- [YD-ESP32-S3 N16R8 보드 분석](f:\C\Programming\MobileDevelopment\Projects\Android\BridgeOne\docs\board\YD-ESP32-S3-N16R8-analysis.md)
- [BridgeOne ESP32-S3 구현 가이드](f:\C\Programming\MobileDevelopment\Projects\Android\BridgeOne\docs\board\esp32s3-code-implementation-guide.md)
