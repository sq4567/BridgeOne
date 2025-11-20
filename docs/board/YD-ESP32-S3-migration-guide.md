# YD-ESP32-S3 N16R8 마이그레이션 가이드

**작성일**: 2025-11-19
**대상 사용자**: ESP32-S3-DevkitC-1에서 YD-ESP32-S3 N16R8으로 마이그레이션하는 개발자
**프로젝트**: BridgeOne - Android-PC 입력 브릿지

---

## 개요

YD-ESP32-S3 N16R8 (YD-ESP32-23)은 ESP32-S3-DevkitC-1과 기능적으로 완전 호환되는 저가 클론 보드입니다. 본 가이드는 BridgeOne 프로젝트를 YD-ESP32-S3 보드에서 사용하기 위한 필수 변경사항을 안내합니다.

### 주요 변경사항 요약
- ✅ **펌웨어 코드**: 수정 완료 (UART0 → UART1)
- ✅ **USB HID 기능**: 변경 없음 (완전 호환)
- ⚠️ **하드웨어 연결**: GPIO17/18로 Android 통신
- ⚠️ **드라이버**: CH343 드라이버 필요 (CP210x 아님)

---

## 1. 하드웨어 차이점

### 1.1 UART 구성 비교

| 항목 | ESP32-S3-DevkitC-1 | YD-ESP32-S3 N16R8 |
|------|-------------------|------------------|
| **Android 통신 UART** | UART0 (GPIO43/44) | UART1 (GPIO17/18) |
| **USB-UART 칩** | CP2102N | CH343P |
| **UART0 사용처** | Android 통신 | CH343P 디버그 전용 |

### 1.2 GPIO 핀 할당 변경

```c
// ESP32-S3-DevkitC-1 (기존)
UART0: GPIO43 (TX), GPIO44 (RX) → Android 통신

// YD-ESP32-S3 N16R8 (새 구성)
UART0: GPIO43 (TX), GPIO44 (RX) → CH343P 디버깅용
UART1: GPIO17 (TX), GPIO18 (RX) → Android 통신용 ⭐
```

### 1.3 물리적 차이

| 구분 | DevkitC-1 | YD-ESP32-S3 |
|------|-----------|-------------|
| USB 포트 위치 | UART 좌측, USB 우측 | UART 우측, USB 좌측 (반대) |
| RGB LED | SK68xx (3.3V) | WS2812 (5V) |
| 점퍼 | 없음 | USB-OTG, RGB 점퍼 |
| 5V 핀 | 정상 | ⚠️ 일부 보드에서 3.3V 출력 보고됨 |

---

## 2. 소프트웨어 변경사항

### 2.1 펌웨어 코드 수정 (이미 완료됨)

#### uart_handler.h
```c
// UART 번호 및 핀 정의 변경
#define UART_NUM UART_NUM_1         // UART0 → UART1
#define UART_TX_PIN GPIO_NUM_17     // GPIO43 → GPIO17
#define UART_RX_PIN GPIO_NUM_18     // GPIO44 → GPIO18
```

#### uart_handler.c
```c
// uart_init() 함수에 uart_set_pin() 호출 추가
ret = uart_set_pin(
    UART_NUM,
    UART_TX_PIN,        // GPIO17
    UART_RX_PIN,        // GPIO18
    UART_PIN_NO_CHANGE,
    UART_PIN_NO_CHANGE
);
```

### 2.2 sdkconfig 설정 (변경 불필요)

현재 sdkconfig는 YD-ESP32-S3에서도 그대로 사용 가능합니다:
```ini
CONFIG_IDF_TARGET="esp32s3"
CONFIG_SPIRAM=y
CONFIG_SPIRAM_MODE_OCT=y        # Octal SPI PSRAM
CONFIG_TINYUSB_ENABLED=y        # USB HID 지원
```

### 2.3 Android 앱 코드 (변경 불필요)

Android 앱은 UART 통신 프로토콜만 준수하면 되므로, ESP32-S3 보드 변경과 무관하게 동작합니다.

---

## 3. 하드웨어 검증 절차

### 3.1 전원 전압 확인 (필수)

일부 YD-ESP32-S3 보드에서 5V 핀 이슈가 보고되었으므로, 사용 전 멀티미터로 전압을 확인하세요.

```bash
# 멀티미터 측정
1. USB 케이블을 연결합니다.
2. 3.3V 핀 측정 → 3.3V ±5% 확인
3. 5V 핀 측정 → 5V ±5% 확인 (중요!)
4. GND 연속성 테스트
```

⚠️ **주의**: 5V 핀이 3.3V로 측정되는 보드는 불량품입니다. 사용하지 마세요.

### 3.2 드라이버 설치

YD-ESP32-S3은 CH343P USB-UART 칩을 사용하므로, CP210x 드라이버가 아닌 **CH343 드라이버**가 필요합니다.

#### Windows
```
자동 설치: Windows 10/11은 자동으로 인식
수동 설치: WCH 공식 사이트에서 CH343 드라이버 다운로드
확인: 장치 관리자 → 포트(COM & LPT) → USB-SERIAL CH343
```

#### Linux
```bash
# 대부분 커널 4.x 이상에서 자동 인식
# /dev/ttyACM0 또는 /dev/ttyUSB0으로 인식

# 권한 설정
sudo usermod -a -G dialout $USER
# 재로그인 필요
```

#### macOS
```bash
# macOS 11 (Big Sur) 이상: 자동 인식
# /dev/cu.usbserial-* 또는 /dev/cu.wchusbserial*로 인식
```

### 3.3 USB 포트 식별

YD-ESP32-S3은 **2개의 USB 포트**를 가지고 있습니다:

1. **CH343P UART 포트** (플래싱/디버깅용)
   - Windows: `COMx` (예: COM3)
   - Linux: `/dev/ttyACM0`
   - 용도: `idf.py flash`, `idf.py monitor`

2. **Native USB OTG 포트** (HID 통신용)
   - Android와 연결하여 USB HID 통신
   - PC에 연결하면 HID 마우스/키보드로 인식

---

## 4. 빌드 및 플래싱

### 4.1 빌드 (변경 없음)

```bash
# ESP-IDF 환경 활성화
cd <ESP-IDF 설치 경로>
export.bat  # Windows
# source export.sh  # Linux/macOS

# BridgeOne 펌웨어 디렉토리
cd f:\C\Programming\MobileDevelopment\Projects\Android\BridgeOne\src\board\BridgeOne

# 빌드
idf.py build
```

### 4.2 플래싱

```bash
# CH343P UART 포트로 플래싱
idf.py -p COM3 flash

# 포트 자동 감지
idf.py flash

# 플래싱 후 모니터링
idf.py -p COM3 monitor
```

### 4.3 예상 출력 확인

플래싱 후 시리얼 모니터에서 다음 로그를 확인하세요:

```
I (XXX) BridgeOne: TinyUSB device stack initialized
I (XXX) UART_HANDLER: UART parameter configured: 1000000 bps, 8N1
I (XXX) UART_HANDLER: UART pins configured: TX=17, RX=18  ← 중요!
I (XXX) UART_HANDLER: UART driver installed with RX buffer (256) and TX buffer (256)
I (XXX) BridgeOne: Hardware: ESP32-S3 N16R8 (DevkitC-1 or YD-ESP32-S3 compatible)
```

✅ **확인 포인트**: `TX=17, RX=18` 로그가 출력되어야 합니다.

---

## 5. 하드웨어 연결

### 5.1 Android ↔ ESP32-S3 연결

```
Android 스마트폰 (USB-OTG)
    |
    | USB-C 케이블
    ↓
YD-ESP32-S3 Native USB 포트 (GPIO19/20)
    |
    | ESP32-S3 내부 UART1
    ↓
GPIO17 (TX) → Android (UART 수신)
GPIO18 (RX) ← Android (UART 송신)
```

⚠️ **주의**: YD-ESP32-S3의 **Native USB 포트**를 Android에 연결하세요. CH343P UART 포트가 아닙니다!

### 5.2 ESP32-S3 ↔ PC 연결

```
YD-ESP32-S3 Native USB (GPIO19/20)
    |
    | USB HID (TinyUSB)
    ↓
PC (Windows/Linux/macOS)
```

PC는 ESP32-S3를 **마우스 및 키보드 장치**로 인식합니다.

---

## 6. 테스트 및 검증

### 6.1 UART 통신 검증

#### 루프백 테스트 (선택 사항)
```c
// GPIO17과 GPIO18을 직접 연결하여 루프백 테스트
// 송신한 데이터가 수신되는지 확인
```

#### Android 앱 연결 테스트
```bash
1. Android 앱 실행
2. YD-ESP32-S3 Native USB 포트에 연결
3. 시리얼 모니터에서 프레임 수신 로그 확인:
   I (XXX) UART_HANDLER: Frame received and queued: seq=XX, buttons=0xXX
```

### 6.2 USB HID 검증

#### PC 마우스 테스트
```bash
1. YD-ESP32-S3 Native USB를 PC에 연결
2. PC가 HID 마우스 장치로 인식하는지 확인:
   - Windows: 장치 관리자 → 마우스 및 기타 포인팅 장치
   - Linux: lsusb | grep 303A
3. Android 앱에서 터치 입력 → PC 마우스 커서 이동 확인
```

### 6.3 End-to-End 통합 테스트

```
Android 터치 → UART1 (GPIO17/18) → ESP32-S3 처리 → USB HID → PC 마우스 이동

예상 레이턴시: 6-18ms (목표 <16ms 충족)
```

---

## 7. 문제 해결 (Troubleshooting)

### 7.1 플래싱 실패

**증상**: `idf.py flash` 실패
**원인**: 포트 인식 오류 또는 드라이버 미설치
**해결**:
```bash
# 1. 포트 확인
idf.py list-ports

# 2. 수동 포트 지정
idf.py -p COM3 flash

# 3. 수동 다운로드 모드 진입
# - Boot 버튼 누르기
# - Reset 버튼 누르기
# - Reset 버튼 떼기
# - Boot 버튼 떼기
# - idf.py flash 실행
```

### 7.2 UART 통신 실패

**증상**: Android 앱에서 프레임 전송했지만 ESP32-S3에서 수신 안 됨
**원인**: GPIO 핀 연결 오류 또는 UART 설정 오류
**해결**:
```bash
# 1. 시리얼 모니터에서 UART 초기화 로그 확인
I (XXX) UART_HANDLER: UART pins configured: TX=17, RX=18

# 2. GPIO17/18 핀이 다른 장치와 충돌하지 않는지 확인
# 3. Android 앱에서 정확한 UART 설정 확인 (1Mbps, 8N1)
```

### 7.3 USB HID 인식 안 됨

**증상**: PC에서 ESP32-S3를 인식하지 못함
**원인**: Native USB 포트 대신 CH343P 포트 연결
**해결**:
```
1. YD-ESP32-S3의 두 USB 포트 중 Native USB 포트에 연결했는지 확인
2. Windows: 장치 관리자에서 "알 수 없는 장치" 확인
3. Linux: dmesg | tail로 USB 열거 로그 확인
```

### 7.4 전원 이슈

**증상**: 보드가 불안정하게 동작하거나 재부팅 반복
**원인**: 전원 공급 부족 또는 5V 핀 불량
**해결**:
```
1. 멀티미터로 3.3V 및 5V 핀 전압 재확인
2. 다른 USB 포트 또는 전원 어댑터 사용
3. 불량 보드는 사용 중단 및 교체
```

---

## 8. 체크리스트

### 마이그레이션 완료 체크리스트

#### Phase 1: 하드웨어 검증
- [ ] YD-ESP32-S3 보드 전원 전압 측정 (3.3V, 5V 확인)
- [ ] CH343 드라이버 설치 및 포트 인식 확인
- [ ] 기본 펌웨어 플래싱 테스트 (blink 예제)

#### Phase 2: 펌웨어 빌드 및 플래싱
- [ ] BridgeOne 펌웨어 빌드 성공
- [ ] CH343P UART 포트로 플래싱 성공
- [ ] 시리얼 모니터에서 `TX=17, RX=18` 로그 확인

#### Phase 3: UART 통신 검증
- [ ] GPIO17/18 핀 토글 테스트 (오실로스코프 확인, 선택 사항)
- [ ] Android 앱 연결 및 UART 프레임 송수신 확인
- [ ] 시리얼 모니터에서 프레임 수신 로그 확인

#### Phase 4: USB HID 검증
- [ ] PC에서 HID 마우스 장치 인식 확인
- [ ] Boot Protocol 모드 동작 확인 (BIOS 진입 테스트, 선택 사항)
- [ ] 마우스 이동 및 버튼 클릭 테스트

#### Phase 5: 통합 테스트
- [ ] Android ↔ ESP32-S3 UART 통신 안정성 확인
- [ ] ESP32-S3 ↔ PC USB HID 통신 안정성 확인
- [ ] End-to-End 테스트 (Android 터치 → PC 마우스 이동)
- [ ] 레이턴시 측정 (<16ms 목표 달성 확인)

---

## 9. 추가 리소스

### 문서
- [YD-ESP32-S3-N16R8-analysis.md](YD-ESP32-S3-N16R8-analysis.md): 상세 보드 분석 및 호환성 평가
- [esp32s3-code-implementation-guide.md](esp32s3-code-implementation-guide.md): ESP32-S3 펌웨어 구현 가이드
- [CLAUDE.md](../../CLAUDE.md): 프로젝트 전체 개요

### GitHub 저장소
- [YD-ESP32-23 GitHub](https://github.com/rtek1000/YD-ESP32-23): YD-ESP32-S3 공식 저장소
- [VCC-GND YD-ESP32-S3 Pinout](https://mischianti.org/vcc-gnd-studio-yd-esp32-s3-devkitc-1-clone-high-resolution-pinout-and-specs/): 상세 핀아웃

### ESP-IDF 문서
- [ESP32-S3 USB Device Stack](https://docs.espressif.com/projects/esp-idf/en/stable/esp32s3/api-reference/peripherals/usb_device.html)
- [UART API Reference](https://docs.espressif.com/projects/esp-idf/en/stable/esp32s3/api-reference/peripherals/uart.html)

---

## 10. 결론

YD-ESP32-S3 N16R8은 BridgeOne 프로젝트와 **완전 호환**되며, 다음 항목만 주의하면 문제없이 사용할 수 있습니다:

✅ **장점**:
- 저렴한 가격 (DevkitC-1 대비)
- 기능적으로 동일 (USB HID, TinyUSB, PSRAM)
- 듀얼 USB 포트로 디버깅 편의성 향상

⚠️ **주의사항**:
- UART1 (GPIO17/18) 사용 필수
- CH343 드라이버 설치 필요
- 전원 전압 확인 필수 (5V 핀 이슈)

📋 **권장 사항**:
- 프로토타입 개발: YD-ESP32-S3 사용 권장 (경제적)
- 최종 제품: 품질 검증 후 결정 또는 커스텀 PCB 제작

---

**문서 작성자**: Claude (Anthropic)
**최종 업데이트**: 2025-11-19
**검토 필요**: ESP32-S3 하드웨어 전문가
