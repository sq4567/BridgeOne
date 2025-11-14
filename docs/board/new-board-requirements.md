# BridgeOne 프로젝트 - 새 보드 구매 요구사항 명세서

## 문서 정보
- **작성일**: 2025-11-14
- **프로젝트**: BridgeOne ESP32-S3 펌웨어
- **목적**: 현재 보드 복구 불가능 상태로 인한 새 보드 구매 가이드
- **참고 문서**:
  - `usb-hid-recognition-issue-analysis.md` - 문제 원인 분석
  - `usb-hid-recognition-issue-solutions.md` - 해결 시도 기록

---

## 1. 문제 요약

### 1.1 현재 상황
- **보드**: ESP32-S3-DevkitC-1-N16R8
- **상태**: 🔴 **복구 불가능** (eFuse 설정 충돌로 인한 부팅 불가)
- **원인**: ROM 부트로더 버그 (eFuse 조합 제약)
- **결론**: **새 보드 구매 필수**

### 1.2 발생한 문제
```
eFuse 설정 충돌:
┌─────────────────────────────────────────────────┐
│ DIS_USB_JTAG = 1        (영구 설정)            │
│ DIS_USB_SERIAL_JTAG = 0 (변경 불가)            │
│ USB_PHY_SEL = 1         (영구 설정)            │
│                                                 │
│ ROM 부트로더 에러:                              │
│ "DIS_USB_JTAG and DIS_USB_SERIAL_JTAG          │
│  cannot be set together due to a bug           │
│  in the ROM bootloader!"                       │
└─────────────────────────────────────────────────┘
```

**결과**:
- ROM 부트로더 크래시 → 부팅 불가
- eFuse는 영구적이며 변경 불가능
- 현재 보드는 복구 방법 없음

---

## 2. 현재 보드 분석

### 2.1 사용 중인 보드
**ESP32-S3-DevkitC-1-N16R8**

| 항목 | 사양 | BridgeOne 사용 현황 |
|------|------|-------------------|
| **MCU** | ESP32-S3 | ✅ 적합 |
| **Flash** | 16MB | ✅ 충분 (펌웨어 ~230KB 사용) |
| **PSRAM** | 8MB | ✅ 충분 (현재 사용량 낮음) |
| **USB** | USB OTG + Serial JTAG | 🔴 eFuse 충돌 발생 |
| **GPIO** | 충분 | ✅ UART, USB 사용 중 |
| **디버깅** | USB Serial JTAG | 🔴 비활성화 필요 (충돌) |

### 2.2 BridgeOne 프로젝트 요구사항

#### 필수 기능
1. **USB HID (Keyboard + Mouse)**
   - TinyUSB 스택 사용
   - Boot Protocol 지원 (BIOS/BitLocker 호환)
   - 복합 장치 (HID Keyboard + HID Mouse + CDC)

2. **USB CDC (Serial)**
   - 멀티 커서 및 매크로 실행 요청
   - 양방향 통신 (Android ↔ ESP32-S3 ↔ Windows PC)

3. **UART 통신 (Android ↔ ESP32-S3)**
   - 속도: 1Mbps
   - 델타 프레임: 8바이트, 120Hz 전송
   - GPIO43 (TX), GPIO44 (RX)

4. **성능 요구사항**
   - 낮은 레이턴시 (<10ms)
   - 안정적인 USB 통신
   - FreeRTOS 멀티태스킹

#### 향후 확장 가능성
- **네이티브 매크로**: Android 앱 내에서 일련의 키 입력이나 클릭을 HID Raw Input 형태로 전달
  - 기존 매크로(PC 키보드/마우스 에뮬레이션)와 달리 ESP32-S3를 통한 직접 HID 전송
- **CDC 데이터 송수신 확장**: 멀티 커서, 매크로 외 추가 제어 명령 및 데이터 통신
- **Bluetooth 통신**: ESP32-S3 BLE를 활용한 무선 연결 옵션
- **Wi-Fi 기능**: 원격 제어 및 OTA 업데이트

---

## 3. 기술적 제약사항 및 교훈

### 3.1 eFuse 관련 교훈

#### ⚠️ 절대 하지 말아야 할 것
1. **DIS_USB_JTAG와 DIS_USB_SERIAL_JTAG를 동시에 설정하지 말 것**
   - ROM 부트로더 버그로 인해 부팅 불가
   - Espressif 공식 문서 확인 필수

2. **eFuse BURN 전 충분한 검증**
   - eFuse는 영구적이며 되돌릴 수 없음
   - 시뮬레이션 가능한 경우 먼저 테스트
   - 공식 문서 및 커뮤니티 확인

3. **USB Serial JTAG를 비활성화하지 않을 것 (권장)**
   - TinyUSB와 USB Serial JTAG는 공존 불가
   - 디버깅 편의성을 위해 USB Serial JTAG 유지 권장
   - 대신 TinyUSB는 USB OTG만 사용

### 3.2 새 보드에서 피해야 할 설정

#### 🔴 절대 금지
```bash
# 절대 실행하지 말 것!
espefuse.py burn_efuse DIS_USB_JTAG 1
espefuse.py burn_efuse USB_PHY_SEL 1

# 이유: ROM 부트로더 버그로 인해 복구 불가능
```

#### ✅ 권장 설정
```bash
# 새 보드는 eFuse 기본값 그대로 사용
# - DIS_USB_JTAG = 0 (활성화)
# - DIS_USB_SERIAL_JTAG = 0 (활성화)
# - USB_PHY_SEL = 0 (기본값)

# TinyUSB는 sdkconfig에서 설정
CONFIG_TINYUSB=y
CONFIG_TINYUSB_CDC_ENABLED=y
CONFIG_TINYUSB_HID_ENABLED=y
```

---

## 4. 새 보드 요구사항 명세

### 4.1 필수 요구사항 (Must Have)

| 항목 | 요구사항 | 이유 |
|------|---------|------|
| **MCU** | ESP32-S3 (any variant) | TinyUSB 지원, BridgeOne 펌웨어 호환성 |
| **Flash** | ≥ 16MB | 현재 사용량 ~230KB, 향후 확장 고려 |
| **PSRAM** | ≥ 8MB | 멀티태스킹, 버퍼 관리 |
| **USB** | USB OTG 지원 | TinyUSB HID/CDC 기능 필수 |
| **GPIO** | UART 핀 노출 (TX/RX) | Android 통신용 |
| **전원** | USB 전원 공급 | PC와 단일 USB 케이블 연결 |
| **리셋** | BOOT + RST 버튼 | Download Mode 진입 필수 |
| **USB 통신** | HID (단방향) + CDC (쌍방향) 동시 작동 | Android/Windows PC와 동시 통신 |
| **CDC 버퍼** | 큰 버퍼 사이즈 | 멀티 커서/매크로 데이터 송수신 |
| **USB 포트** | 2개 포트 간격 충분 | Android/PC 케이블 간섭 방지 |

### 4.2 권장 요구사항 (Should Have)

| 항목 | 권장 사양 | 이유 |
|------|----------|------|
| **Flash** | 32MB | 향후 기능 확장 (Wi-Fi, Bluetooth 등) |
| **PSRAM** | 16MB | 복잡한 데이터 처리 가능 |
| **GPIO** | 확장 핀헤더 | 센서, LED, 추가 입력 장치 |
| **디버그** | JTAG 핀 노출 | 하드웨어 디버깅 (OpenOCD) |
| **LED** | 상태 표시 LED | 시각적 피드백 |
| **폼팩터** | DevKit 형태 | 브레드보드 호환성 |

### 4.3 선택 요구사항 (Nice to Have)

| 항목 | 선택 사양 | 용도 |
|------|----------|------|
| **Wi-Fi** | ESP32-S3 내장 | 원격 제어, OTA 업데이트 |
| **Bluetooth** | ESP32-S3 BLE | 무선 연결 옵션 |
| **센서** | 온보드 센서 | 모션 감지, 환경 모니터링 |
| **화면** | SPI/I2C 디스플레이 연결 | 상태 표시, 설정 UI |
| **확장 슬롯** | microSD 카드 슬롯 | 로깅, 설정 저장 |

---

## 5. 권장 보드 목록 (ESP32-S3 계열)

### 5.1 개발용 보드 (DevKit)

#### 🏆 최우선 추천: ESP32-S3-DevKitC-1-N32R8V
**이유**: 현재 보드의 상위 스펙 (Flash 32MB, PSRAM 8MB)

| 스펙 | 값 | BridgeOne 적합성 |
|------|-----|-----------------|
| MCU | ESP32-S3-WROOM-1-N32R8V | ✅ 완벽 |
| Flash | **32MB** (2배 증가) | ✅ 향후 확장 여유 |
| PSRAM | 8MB (Octal) | ✅ 충분 |
| USB | USB OTG + Serial JTAG | ✅ 적합 |
| GPIO | 전체 핀 노출 | ✅ 확장 가능 |
| 디버깅 | USB Serial JTAG | ✅ 편리함 |
| 가격 | ~$10-15 | ✅ 합리적 |
| 구매처 | Mouser, DigiKey, AliExpress | ✅ 쉽게 구매 가능 |

**장점**:
- ✅ 현재 펌웨어 그대로 사용 가능 (호환성 100%)
- ✅ Flash 용량 2배 (16MB → 32MB)
- ✅ 안정적인 공급망 (Espressif 공식 보드)
- ✅ 풍부한 개발 자료

**단점**:
- ⚠️ PSRAM은 동일 (8MB)
- ⚠️ 특별한 추가 기능 없음

---

#### ⭐ 추천 2순위: ESP32-S3-DevKitC-1-N16R16V
**이유**: PSRAM 2배 증가 (8MB → 16MB)

| 스펙 | 값 | BridgeOne 적합성 |
|------|-----|-----------------|
| MCU | ESP32-S3-WROOM-1-N16R16V | ✅ 완벽 |
| Flash | 16MB (현재와 동일) | ✅ 충분 |
| PSRAM | **16MB** (2배 증가, Octal) | ✅ 향후 확장 여유 |
| USB | USB OTG + Serial JTAG | ✅ 적합 |
| GPIO | 전체 핀 노출 | ✅ 확장 가능 |
| 디버깅 | USB Serial JTAG | ✅ 편리함 |
| 가격 | ~$12-18 | ✅ 합리적 |

**장점**:
- ✅ PSRAM 2배 (8MB → 16MB)
- ✅ 복잡한 데이터 처리 가능 (버퍼, 캐시)
- ✅ 호환성 100%

**단점**:
- ⚠️ Flash는 동일 (16MB)
- ⚠️ 가격 약간 상승

---

#### 🚀 최고 스펙: ESP32-S3-DevKitC-1-N32R16V (존재 시)
**이유**: Flash + PSRAM 모두 최대 (32MB + 16MB)

| 스펙 | 값 | BridgeOne 적합성 |
|------|-----|-----------------|
| MCU | ESP32-S3-WROOM-1-N32R16V | ✅ 완벽 |
| Flash | **32MB** (2배) | ✅ 최대 여유 |
| PSRAM | **16MB** (2배) | ✅ 최대 여유 |
| USB | USB OTG + Serial JTAG | ✅ 적합 |
| GPIO | 전체 핀 노출 | ✅ 확장 가능 |

**장점**:
- ✅ 모든 스펙 최대화
- ✅ 장기간 사용 가능
- ✅ 미래 보장성 최고

**단점**:
- ⚠️ 가격 상승 (~$15-20)
- ⚠️ 현재 BridgeOne에서는 오버스펙

**참고**: 2025년 11월 기준, ESP32-S3-WROOM-1-N32R16V 모듈의 공식 DevKit 존재 여부 확인 필요.
없다면 모듈만 구매하여 직접 제작하거나, N32R8V 또는 N16R16V 선택 권장.

---

### 5.2 고급 개발 보드

#### ESP32-S3-DevKitM-1
- **특징**: Mini 폼팩터, ESP32-S3-MINI-1 모듈 사용
- **Flash**: 8MB (BridgeOne에 부족할 수 있음)
- **PSRAM**: 없음 (8MB PSRAM 없는 버전 존재)
- **결론**: ❌ **비추천** (Flash, PSRAM 부족)

#### ESP32-S3-USB-OTG
- **특징**: USB OTG 전용 보드, LCD, 배터리 충전 회로
- **Flash**: 8MB
- **PSRAM**: 8MB (Octal)
- **추가 기능**: 1.3" LCD, USB-A 포트, 배터리 관리
- **결론**: ⚠️ **선택사항** (BridgeOne에는 LCD 불필요, 하지만 디버깅 편리)

---

### 5.3 서드파티 보드

#### LilyGO T-Display-S3
- **특징**: 1.9" LCD, 배터리, USB-C
- **Flash**: 16MB
- **PSRAM**: 8MB
- **추가 기능**: 디스플레이, 터치, 배터리
- **결론**: ⚠️ **선택사항** (디스플레이로 상태 표시 가능, 하지만 가격 상승)

#### Unexpected Maker FeatherS3
- **특징**: Feather 폼팩터, LiPo 충전
- **Flash**: 16MB
- **PSRAM**: 8MB
- **추가 기능**: Stemma QT/Qwiic 커넥터
- **결론**: ⚠️ **선택사항** (확장성 좋음, 하지만 가격 상승)

---

## 6. 최종 권장 사항

### 6.1 예산별 추천

#### 💰 예산 최우선 (~$10)
**ESP32-S3-DevKitC-1-N16R8** (현재 보드와 동일 스펙)
- 현재 펌웨어 그대로 사용 가능
- 가장 저렴
- 단, Flash/PSRAM 확장 여유 없음

#### ⚖️ 균형 잡힌 선택 (~$12-15)
**ESP32-S3-DevKitC-1-N32R8V** (✅ **최우선 추천**)
- Flash 2배 (16MB → 32MB)
- 향후 확장 가능
- 안정적인 공급
- 합리적인 가격

#### 🚀 미래 보장성 (~$15-20)
**ESP32-S3-DevKitC-1-N16R16V** 또는 **N32R16V**
- PSRAM 2배 (8MB → 16MB)
- Flash 최대 32MB
- 장기간 사용 보장
- 복잡한 기능 추가 가능

---

### 6.2 구매 전 체크리스트

#### ✅ 필수 확인 사항
- [ ] **ESP32-S3 MCU** 확인 (ESP32-S2 아님!)
- [ ] **Flash ≥ 16MB** 확인
- [ ] **PSRAM ≥ 8MB** 확인 (Octal PSRAM 권장)
- [ ] **USB OTG 지원** 확인
- [ ] **BOOT + RST 버튼** 있는지 확인
- [ ] **UART 핀 (GPIO43/44)** 노출 확인
- [ ] **공식 판매처** 구매 (위조품 주의)

#### ⚠️ 주의 사항
- [ ] **eFuse 기본값 확인** (새 보드는 eFuse가 설정되지 않아야 함)
- [ ] **반품 정책 확인** (문제 발생 시 대비)
- [ ] **납기 확인** (일부 모델은 재고 부족 가능성)
- [ ] **기술 지원 여부** (Espressif 공식 보드 권장)

#### 📦 구매 시 함께 구매 권장
- [ ] **USB-C 케이블** (양질의 케이블, 데이터 통신 지원)
- [ ] **USB-to-UART 어댑터** (디버깅용, CH340/CP2102/FT232)
- [ ] **브레드보드 + 점퍼 와이어** (프로토타이핑)
- [ ] **예비 보드 1개** (향후 eFuse 실험용)

---

## 7. 구매 가이드

### 7.1 공식 판매처

#### 국제 판매처
1. **Mouser Electronics**
   - URL: https://www.mouser.com
   - 검색어: "ESP32-S3-DevKitC-1-N32R8V"
   - 장점: 공식 재고, 빠른 배송, 기술 지원
   - 배송: 한국 배송 가능 (DHL/FedEx)

2. **DigiKey**
   - URL: https://www.digikey.com
   - 검색어: "ESP32-S3-DevKitC-1"
   - 장점: 재고 풍부, 상세한 스펙 시트
   - 배송: 한국 배송 가능

3. **Espressif 공식 스토어** (타오바오/AliExpress)
   - URL: https://www.aliexpress.com/store/1101356076
   - 장점: 최저가, 공식 보증
   - 단점: 배송 시간 길 수 있음 (2-4주)

#### 국내 판매처
1. **디바이스마트**
   - URL: https://www.devicemart.co.kr
   - 검색어: "ESP32-S3"
   - 장점: 빠른 배송, 한글 지원
   - 단점: 재고 부족 가능성

2. **eleparts**
   - URL: https://www.eleparts.co.kr
   - 검색어: "ESP32-S3 DevKit"
   - 장점: 국내 재고
   - 단점: 가격 약간 높음

---

### 7.2 스펙 비교표 (구매 참고용)

| 모델 | Flash | PSRAM | 가격 | BridgeOne 적합성 | 추천도 |
|------|-------|-------|------|-----------------|--------|
| **ESP32-S3-DevKitC-1-N16R8** | 16MB | 8MB | ~$10 | ✅ 기본 | ⭐⭐⭐ |
| **ESP32-S3-DevKitC-1-N32R8V** | **32MB** | 8MB | ~$12 | ✅ **최적** | ⭐⭐⭐⭐⭐ |
| **ESP32-S3-DevKitC-1-N16R16V** | 16MB | **16MB** | ~$15 | ✅ 고성능 | ⭐⭐⭐⭐ |
| **ESP32-S3-DevKitC-1-N32R16V** | **32MB** | **16MB** | ~$18 | ✅ 최고 | ⭐⭐⭐⭐⭐ |
| ESP32-S3-DevKitM-1 | 8MB | 0MB | ~$8 | ❌ 부족 | ⭐ |
| ESP32-S3-USB-OTG | 8MB | 8MB | ~$20 | ⚠️ 선택 | ⭐⭐⭐ |

**범례**:
- ✅ 기본: 현재 요구사항 충족
- ✅ 최적: 향후 확장 고려한 최적 선택
- ✅ 고성능: 고급 기능 추가 시 유리
- ✅ 최고: 장기간 사용 보장
- ❌ 부족: 요구사항 미충족
- ⚠️ 선택: 추가 기능 있으나 BridgeOne에 불필요

---

## 8. 새 보드 도착 후 체크리스트

### 8.1 하드웨어 검증
```bash
# 1. 보드 연결 (USB-C 케이블)
# 2. Windows 장치 확인
Get-PnpDevice | Where-Object {$_.DeviceID -match "VID_303A"}

# 예상 결과:
# FriendlyName: USB Serial JTAG
# PID: 0x1001 (Download Mode) 또는 0x1002 (CDC)
```

### 8.2 eFuse 상태 확인 (필수!)
```bash
# ESP-IDF 환경 활성화
cd <ESP-IDF 설치 경로>
export.bat

# eFuse 전체 확인
espefuse.py --port COM? summary

# ⚠️ 중요: 다음 값들이 기본값이어야 함
# - DIS_USB_JTAG = 0 (False)
# - DIS_USB_SERIAL_JTAG = 0 (False)
# - USB_PHY_SEL = 0

# 만약 이미 설정되어 있다면 반품 고려!
```

### 8.3 펌웨어 플래시 테스트
```bash
cd F:\C\Programming\MobileDevelopment\Projects\Android\BridgeOne\src\board\BridgeOne

# 1. 클린 빌드
idf.py fullclean
idf.py build

# 2. 플래시
idf.py -p COM? flash

# 3. 모니터링
idf.py -p COM? monitor

# 예상 로그:
# I (771) BridgeOne: [BOOT STAGE 1] app_main() started
# I (778) BridgeOne: [BOOT STAGE 2] Calling tusb_init()
# I (780) BridgeOne: [BOOT STAGE 3] TinyUSB device stack initialized
```

### 8.4 기능 검증
- [ ] **부팅 성공**: `[BOOT STAGE 1]` 로그 출력
- [ ] **TinyUSB 초기화**: USB 장치 인식 (PID_0009 또는 PID_4001)
- [ ] **CDC 통신**: 시리얼 모니터로 로그 수신
- [ ] **HID 기능**: (활성화 시) Windows에서 HID 장치 인식
- [ ] **UART 통신**: Android 앱과 통신 테스트

---

## 9. 참고 자료

### 9.1 공식 문서
- **ESP32-S3 시리즈 데이터시트**: https://www.espressif.com/sites/default/files/documentation/esp32-s3_datasheet_en.pdf
- **ESP32-S3-WROOM-1/1U 데이터시트**: https://www.espressif.com/sites/default/files/documentation/esp32-s3-wroom-1_wroom-1u_datasheet_en.pdf
- **ESP32-S3-DevKitC-1 사용 가이드**: https://docs.espressif.com/projects/esp-idf/en/latest/esp32s3/hw-reference/esp32s3/user-guide-devkitc-1.html
- **eFuse 프로그래밍 가이드**: https://docs.espressif.com/projects/esp-idf/en/latest/esp32s3/api-reference/system/efuse.html

### 9.2 관련 문서 (이 프로젝트)
- `docs/troubleshooting/usb-hid-recognition-issue-analysis.md` - 문제 원인 정밀 분석
- `docs/troubleshooting/usb-hid-recognition-issue-solutions.md` - 해결 시도 기록
- `docs/board/esp32s3-code-implementation-guide.md` - 펌웨어 구현 가이드
- `CLAUDE.md` - 프로젝트 개요 및 개발 가이드

### 9.3 커뮤니티 자료
- **ESP32 공식 포럼**: https://esp32.com
- **ESP-IDF GitHub Issues**: https://github.com/espressif/esp-idf/issues
- **TinyUSB GitHub**: https://github.com/hathach/tinyusb
- **Reddit r/esp32**: https://www.reddit.com/r/esp32

---

## 10. 요약 및 결론

### 10.1 핵심 요약

#### 🔴 현재 상황
- **현재 보드**: ESP32-S3-DevKitC-1-N16R8
- **상태**: 복구 불가능 (eFuse ROM 부트로더 버그)
- **원인**: DIS_USB_JTAG + DIS_USB_SERIAL_JTAG 충돌

#### ✅ 최우선 추천
**ESP32-S3-DevKitC-1-N32R8V**
- Flash: 32MB (2배 증가)
- PSRAM: 8MB (동일)
- 가격: ~$12-15
- 이유: 향후 확장 가능, 합리적 가격, 안정적 공급

#### 🚀 미래 보장성 추천
**ESP32-S3-DevKitC-1-N16R16V** 또는 **N32R16V**
- Flash: 16MB 또는 32MB
- PSRAM: 16MB (2배 증가)
- 가격: ~$15-20
- 이유: 장기간 사용, 고급 기능 추가 가능

### 10.2 주의사항 재강조

#### ⚠️ eFuse 관련 (절대 지키기!)
1. **새 보드에서 eFuse 설정하지 말 것**
2. **DIS_USB_JTAG 및 DIS_USB_SERIAL_JTAG BURN 금지**
3. **USB_PHY_SEL 기본값 유지**
4. **eFuse 기본값 상태로 TinyUSB 사용**

#### ✅ 권장 설정 (sdkconfig만 사용)
```ini
CONFIG_TINYUSB=y
CONFIG_TINYUSB_CDC_ENABLED=y
CONFIG_TINYUSB_HID_ENABLED=y
```

### 10.3 구매 체크리스트 (최종)

- [ ] **ESP32-S3-DevKitC-1-N32R8V** (또는 N16R16V/N32R16V) 선택
- [ ] **공식 판매처**에서 구매 (Mouser, DigiKey, 또는 Espressif 공식)
- [ ] **USB-C 케이블** + **USB-to-UART 어댑터** 함께 구매
- [ ] **예비 보드 1개** 추가 구매 (선택사항)
- [ ] 도착 후 **eFuse 기본값 확인** 필수
- [ ] **펌웨어 플래시 테스트** 후 사용 시작

---

**작성자**: Claude Code (SuperClaude Framework)
**작성 방법**: Ultrathink mode - 문제 분석 문서 종합 검토 → 기술적 제약사항 도출 → 요구사항 명세 작성 → 권장 보드 선정
**최종 검토일**: 2025-11-14
**상태**: ✅ 완료 (새 보드 구매 즉시 사용 가능)
