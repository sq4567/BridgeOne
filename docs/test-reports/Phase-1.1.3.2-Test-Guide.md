# Phase 1.1.3.2: 기본 연결 및 프레임 송수신 테스트 가이드

## 📋 테스트 개요

**목표**: Android ↔ ESP32-S3 통신 End-to-End 검증  
**예상 시간**: 30-40분 (수동 테스트)  
**필요 장비**: 
- Android 디바이스 (Android 10 이상, USB OTG 지원)
- ESP32-S3 DevKitC-1 보드
- USB OTG 케이블 (USB-C to USB-A)
- USB-C to USB-C 케이블 (ESP32-S3 펌웨어 업로드용)
- PC (PlatformIO 설치, Android Studio 설치)

## 🛠️ 사전 준비

### 1. ESP32-S3 펌웨어 업로드

```bash
# BridgeOne 보드 프로젝트 디렉토리로 이동
cd src/board/BridgeOne

# PlatformIO를 사용하여 펌웨어 빌드 및 업로드
pio run --target upload

# 업로드 완료 후 시리얼 모니터 연결 (디버그 로그 확인용)
pio device monitor --baud 115200
```

**예상 출력:**
```
========================================
  BridgeOne ESP32-S3 Board
  Phase 1.1.2.5: FreeRTOS Task Structure
========================================

[BridgeFrame] Structure Verification:
  - Size: 8 bytes (expected: 8 bytes)
  ✓ BridgeFrame size verification: PASS

[UART2] Buffer Size Set: RX=256, TX=128
[UART2] Initialized: 1000000 bps, 8N1
[UART2] Pins: RX=GPIO44, TX=GPIO43 (Board UART Port)

[INIT] All systems initialized successfully!

[FreeRTOS] Creating tasks...
  ✓ UART RX Task created on Core 1 (Priority 3)
  ✓ Debug Task created on Core 0 (Priority 1)
[FreeRTOS] Task creation completed!

[UART RX Task] Started on Core 1
[Debug Task] Started on Core 0
```

### 2. Android 앱 빌드 및 설치

```bash
# Android 프로젝트 디렉토리로 이동
cd src/android

# Gradle을 사용하여 디버그 APK 빌드 및 설치
./gradlew installDebug

# 또는 Android Studio에서:
# Run > Run 'app' (Shift+F10)
```

### 3. 하드웨어 연결

1. **ESP32-S3 DevKitC-1 보드 준비**
   - USB 케이블을 PC에서 분리 (시리얼 모니터는 계속 실행 상태 유지)
   - 보드 왼쪽의 **UART 포트**에 USB-C to USB-A 케이블 연결

2. **Android 디바이스 연결**
   - USB OTG 어댑터를 Android 디바이스에 연결
   - ESP32-S3 보드의 UART 포트 케이블을 OTG 어댑터에 연결

3. **시리얼 모니터 확인**
   - PC에서 ESP32-S3의 USB 포트(오른쪽)를 다시 연결하여 디버그 로그 확인
   - 또는 WiFi 기반 무선 로깅 사용 (선택사항)

---

## ✅ 테스트 1: 기본 연결 테스트 (5분)

### 목표
Android 앱이 ESP32-S3 장치를 정상적으로 인식하고 USB 연결을 수립하는지 확인

### 테스트 절차

1. **Android 앱 실행**
   - 홈 화면에서 "BridgeOne" 앱 실행
   - 초기 화면 확인: "🔌 USB 연결 대기 중"

2. **ESP32-S3 연결**
   - USB OTG를 통해 ESP32-S3 연결
   - Android 시스템이 USB 장치를 자동으로 감지
   - **예상 동작**: 앱 화면이 "⏳ USB 권한 요청 중..."으로 변경
   - **USB 권한 다이얼로그 표시**: "BridgeOne에서 USB 장치에 접근하려고 합니다. 허용하시겠습니까?"

3. **권한 승인**
   - "확인" 버튼 클릭
   - **예상 동작**: 앱 화면이 "✅ 연결됨"으로 변경
   - 장치 이름 표시 (예: "USB Serial @ /dev/bus/usb/001/002")

4. **ESP32-S3 시리얼 모니터 확인**
   - PC 시리얼 모니터에 다음 메시지 확인:
   ```
   [UART2] Initialized: 1000000 bps, 8N1
   ```
   - 1초마다 통계 정보 출력 시작:
   ```
   ========================================
   [STATS] Uptime: 5 sec
   [STATS] Received: 0 frames
   [STATS] Lost: 0 frames (0.00%)
   [STATS] Last Seq: 255
   ----------------------------------------
   [MEMORY] Free Heap: 284576 bytes
   [MEMORY] Min Free Heap: 284576 bytes
   ----------------------------------------
   [TASK] UART Rx Task: Stack Watermark = 3456 words
   ========================================
   ```

### ✅ 검증 항목

- [ ] Android 앱에서 ESP32-S3 장치 자동 인식
- [ ] USB 권한 다이얼로그 정상 표시
- [ ] 권한 승인 후 "✅ 연결됨" 상태 표시
- [ ] ESP32-S3 시리얼 모니터에 UART 초기화 메시지 출력
- [ ] 통계 정보 1초마다 출력 확인

### ❌ 문제 발생 시 해결 방법

| 문제 | 원인 | 해결 방법 |
|------|------|-----------|
| USB 장치를 찾을 수 없음 | OTG 케이블 불량 또는 연결 오류 | 케이블 교체, 연결 확인 |
| 권한 다이얼로그 표시 안 됨 | `device_filter.xml` 설정 오류 | VendorID/ProductID 확인 |
| 연결 후 즉시 끊김 | UART 초기화 실패 | 펌웨어 재업로드, 보드 리셋 |
| 시리얼 모니터 무응답 | USB CDC 포트 인식 실패 | CP2102 드라이버 재설치 |

---

## ✅ 테스트 2: 프레임 송신 테스트 (10분)

### 목표
Android 터치 입력이 BridgeFrame으로 변환되어 ESP32-S3로 정상 전송되는지 확인

### 테스트 절차

1. **터치패드 영역 확인**
   - Android 앱 중앙의 "터치패드 영역" 확인
   - 테두리 색상: 파란색 (연결됨 상태)
   - 배경 색상: 반투명 파란색

2. **단일 터치 테스트**
   - 터치패드 영역을 **한 번** 짧게 탭
   - **예상 동작**: 햅틱 피드백 발생
   - **Android 앱 통계**:
     - "총 전송": 2 (터치 시작 + 종료)
     - "현재 순번": 1
     - "초당 전송": 변동

3. **ESP32-S3 시리얼 모니터 확인**
   ```
   [SEQ] First frame received: seq=0
   [RX #1] seq=0 ✓, buttons=0x01, delta=(0,0), wheel=0, mods=0x00, keys=[0x00, 0x00]
     └─ Buttons: [LEFT] 
   
   [RX #2] seq=1 ✓, buttons=0x00, delta=(0,0), wheel=0, mods=0x00, keys=[0x00, 0x00]
   ```
   - **분석**:
     - Frame #1: `buttons=0x01` (왼쪽 버튼 눌림)
     - Frame #2: `buttons=0x00` (왼쪽 버튼 해제)
     - 순번 검증: `✓` (정상)

4. **드래그 이동 테스트**
   - 터치패드 영역을 **오른쪽으로 천천히 드래그** (약 100px)
   - **예상 동작**: 
     - 햅틱 피드백 발생 (터치 시작 시)
     - Android 앱 통계 증가: "총 전송" 10~20회 증가
   
5. **ESP32-S3 시리얼 모니터 확인**
   ```
   [RX #3] seq=2 ✓, buttons=0x01, delta=(0,0), wheel=0, mods=0x00, keys=[0x00, 0x00]
     └─ Buttons: [LEFT] 
   
   [RX #4] seq=3 ✓, buttons=0x01, delta=(+15,0), wheel=0, mods=0x00, keys=[0x00, 0x00]
     └─ Buttons: [LEFT] 
     └─ Mouse Move: X+15 Y+0
   
   [RX #5] seq=4 ✓, buttons=0x01, delta=(+18,0), wheel=0, mods=0x00, keys=[0x00, 0x00]
     └─ Buttons: [LEFT] 
     └─ Mouse Move: X+18 Y+0
   
   [RX #6] seq=5 ✓, buttons=0x01, delta=(+12,0), wheel=0, mods=0x00, keys=[0x00, 0x00]
     └─ Buttons: [LEFT] 
     └─ Mouse Move: X+12 Y+0
   
   [RX #7] seq=6 ✓, buttons=0x00, delta=(0,0), wheel=0, mods=0x00, keys=[0x00, 0x00]
   ```
   - **분석**:
     - `deltaX` 값이 양수 (오른쪽 이동)
     - `buttons=0x01` 유지 (드래그 중)
     - 마지막 프레임에서 `buttons=0x00` (드래그 종료)

6. **다방향 드래그 테스트**
   - 상하좌우 대각선 등 여러 방향으로 드래그
   - **예상 동작**:
     - `deltaX`, `deltaY` 값이 이동 방향에 따라 변화
     - 위로 드래그: `deltaY < 0`
     - 아래로 드래그: `deltaY > 0`
     - 왼쪽 드래그: `deltaX < 0`
     - 오른쪽 드래그: `deltaX > 0`

### ✅ 검증 항목

- [ ] 터치 시작 시 `buttons=0x01` (왼쪽 버튼) 프레임 전송
- [ ] 터치 종료 시 `buttons=0x00` (버튼 해제) 프레임 전송
- [ ] 드래그 시 `deltaX`, `deltaY` 값이 이동 방향과 일치
- [ ] ESP32-S3 시리얼 모니터에 프레임 수신 로그 실시간 출력
- [ ] 순번 검증 ✓ 표시 (순차 증가)
- [ ] Android 앱 통계가 실시간 업데이트

### ❌ 문제 발생 시 해결 방법

| 문제 | 원인 | 해결 방법 |
|------|------|-----------|
| 프레임 전송 안 됨 | USB Serial 포트 닫힘 | 앱 재시작, USB 재연결 |
| deltaX/Y 값 반대 | 좌표계 매핑 오류 | 펌웨어 좌표 변환 검증 필요 |
| 프레임 수신 지연 | UART 버퍼 오버플로우 | 버퍼 크기 증가, 전송 빈도 감소 |
| 순번 검증 실패 | USB 패킷 손실 | USB 케이블 교체, 전송 속도 확인 |

---

## ✅ 테스트 3: 순번 카운터 순환 테스트 (10분)

### 목표
256번 이상 터치 이벤트를 생성하여 순번 카운터가 0→255→0으로 정상 순환하는지 확인

### 테스트 절차

1. **연속 터치 입력 생성**
   - 터치패드 영역에서 **작은 원을 그리면서 연속 드래그** (약 2분)
   - 또는 **좌우로 빠르게 여러 번 드래그**
   - **목표**: 총 전송 프레임 수 300개 이상

2. **Android 앱 통계 모니터링**
   - "총 전송" 값이 300 이상 증가 확인
   - "현재 순번" 값 관찰:
     - 0 → 255까지 증가
     - 255 → 0으로 순환 (롤오버)

3. **ESP32-S3 시리얼 모니터 확인**
   - 순번 순환 지점 확인:
   ```
   [RX #254] seq=253 ✓, buttons=0x01, delta=(+5,-3), ...
   [RX #255] seq=254 ✓, buttons=0x01, delta=(+7,-2), ...
   [RX #256] seq=255 ✓, buttons=0x01, delta=(+4,+1), ...
   [RX #257] seq=0 ✓, buttons=0x01, delta=(+6,+2), ...    <- 순환!
   [RX #258] seq=1 ✓, buttons=0x01, delta=(+3,-1), ...
   ```

4. **통계 정보 확인**
   - 1초 통계 출력 확인:
   ```
   ========================================
   [STATS] Uptime: 120 sec
   [STATS] Received: 312 frames
   [STATS] Lost: 0 frames (0.00%)
   [STATS] Last Seq: 55
   ----------------------------------------
   ```
   - **손실률 0%** 확인 필수

### ✅ 검증 항목

- [ ] 순번 카운터가 0 → 255까지 순차 증가
- [ ] 순번 255 → 0으로 정상 순환 (롤오버)
- [ ] ESP32-S3 순번 검증 로직 통과 (✓ 표시)
- [ ] 순환 지점에서 손실 프레임 없음
- [ ] Android 앱 "현재 순번"이 0~255 범위 내에서 증가

### ❌ 문제 발생 시 해결 방법

| 문제 | 원인 | 해결 방법 |
|------|------|-----------|
| 순번 불일치 (✗) | 프레임 손실 또는 중복 | USB 케이블 확인, 버퍼 크기 증가 |
| 순환 후 검증 실패 | 순환 로직 오류 | 펌웨어 `validateSequence()` 검증 |
| 통계에서 손실 프레임 발생 | 전송 속도 과다 | 터치 입력 빈도 감소 |

---

## ✅ 테스트 4: 1분 연속 전송 안정성 테스트 (15분)

### 목표
1분간 연속으로 터치 입력을 전송하여 시스템 안정성 및 프레임 손실률 <1% 확인

### 테스트 절차

1. **타이머 준비**
   - 스마트폰 타이머 1분 설정

2. **연속 터치 입력 시작**
   - 터치패드 영역에서 **원형, 지그재그, 랜덤** 등 다양한 패턴으로 연속 드래그
   - **중단 없이** 1분간 계속 터치 입력
   - 가능한 한 많은 프레임 생성 (목표: 1000+ 프레임)

3. **Android 앱 통계 모니터링**
   - "총 전송" 값 실시간 증가 확인
   - "초당 전송" 값 관찰 (평균 15~30 fps 예상)
   - **앱 크래시 없음** 확인

4. **ESP32-S3 시리얼 모니터 확인**
   - 프레임 수신 로그 실시간 출력 확인
   - **손실 프레임 경고 메시지** 발생 여부 확인:
   ```
   [WARN] Sequence mismatch! Expected=125, Received=127, Lost=2 frames
   ```
   - 메모리 누수 확인:
     - "Free Heap" 값이 지속적으로 감소하지 않는지 확인
     - "Min Free Heap" 값이 안정적인지 확인

5. **1분 경과 후 통계 확인**
   - Android 앱:
     - "총 전송": 1000 이상
     - 앱 상태: "✅ 연결됨" 유지
   - ESP32-S3:
   ```
   ========================================
   [STATS] Uptime: 180 sec
   [STATS] Received: 1243 frames
   [STATS] Lost: 8 frames (0.64%)              <- 1% 미만 확인!
   [STATS] Last Seq: 186
   ----------------------------------------
   [MEMORY] Free Heap: 282112 bytes
   [MEMORY] Min Free Heap: 281856 bytes
   ----------------------------------------
   [TASK] UART Rx Task: Stack Watermark = 3421 words
   ========================================
   ```

6. **메모리 누수 검증**
   - 테스트 종료 후 "Free Heap"과 "Min Free Heap" 비교
   - **허용 범위**: 초기 대비 5KB 이내 감소 (정상)
   - **비정상**: 10KB 이상 감소 (메모리 누수 의심)

### ✅ 검증 항목

- [ ] 1분간 연속 터치 입력 전송 성공
- [ ] 프레임 손실률 < 1% (허용 범위)
- [ ] Android 앱 크래시 없음
- [ ] ESP32-S3 메모리 누수 없음 (Free Heap 안정)
- [ ] UART RX Task 스택 오버플로우 없음
- [ ] 연결 상태 유지 (재연결 없음)

### ❌ 문제 발생 시 해결 방법

| 문제 | 원인 | 해결 방법 |
|------|------|-----------|
| 손실률 > 1% | USB 버퍼 오버플로우 | UART 버퍼 크기 증가 (256 → 512) |
| 앱 크래시 | 메모리 부족 | 객체 풀링 구현 필요 (Phase 1.3.2.1) |
| ESP32 메모리 감소 | 메모리 누수 | FreeRTOS 태스크 스택 크기 검토 |
| USB 연결 끊김 | 케이블 불량 또는 전력 부족 | 케이블 교체, Android OTG 전원 확인 |

---

## 📊 테스트 결과 보고서 템플릿

### 테스트 환경

| 항목 | 정보 |
|------|------|
| Android 디바이스 | (예: Samsung Galaxy S21, Android 12) |
| ESP32-S3 보드 | ESP32-S3 DevKitC-1 |
| USB OTG 케이블 | (제조사/모델) |
| 펌웨어 버전 | Phase 1.1.2.5 |
| Android 앱 버전 | Phase 1.1.3.1 |
| 테스트 일시 | YYYY-MM-DD HH:MM |

### 테스트 1: 기본 연결 테스트

- [ ] 통과 / [ ] 실패
- USB 연결 시간: ___초
- 권한 요청 정상 동작: [ ] 예 / [ ] 아니오
- ESP32 UART 초기화 확인: [ ] 예 / [ ] 아니오
- 비고: ___________

### 테스트 2: 프레임 송신 테스트

- [ ] 통과 / [ ] 실패
- 단일 터치 프레임 수: ___개 (예상: 2개)
- 드래그 테스트 프레임 수: ___개 (예상: 10~20개)
- deltaX/Y 방향 일치: [ ] 예 / [ ] 아니오
- 순번 검증 통과: [ ] 예 / [ ] 아니오
- 비고: ___________

### 테스트 3: 순번 카운터 순환 테스트

- [ ] 통과 / [ ] 실패
- 총 전송 프레임 수: ___개 (목표: 300개 이상)
- 순번 순환 확인: [ ] 예 / [ ] 아니오 (255 → 0)
- 순환 지점 손실 프레임: ___개 (목표: 0개)
- 손실률: ___%
- 비고: ___________

### 테스트 4: 1분 연속 전송 안정성 테스트

- [ ] 통과 / [ ] 실패
- 총 전송 프레임 수: ___개 (목표: 1000개 이상)
- 손실 프레임 수: ___개
- 손실률: ___% (목표: < 1%)
- 앱 크래시: [ ] 없음 / [ ] 발생
- ESP32 메모리 누수: [ ] 없음 / [ ] 발생
  - 초기 Free Heap: _____bytes
  - 종료 Free Heap: _____bytes
  - 감소량: _____bytes
- 비고: ___________

### 종합 평가

- **전체 테스트 결과**: [ ] 통과 / [ ] 실패
- **주요 발견사항**:
  - ___________
  - ___________
  - ___________
- **개선 필요 사항**:
  - ___________
  - ___________
  - ___________
- **다음 단계 (Phase 1.2.x)**:
  - ___________

---

## 🔍 공식 문서 참고 자료

### ESP32 Arduino Core - UART 통신
- **출처**: [espressif/arduino-esp32](https://github.com/espressif/arduino-esp32)
- **Trust Score**: 9.1
- **핵심 내용**: 
  - `Serial2.begin(baud, config, rxPin, txPin)` - UART 초기화
  - `Serial2.available()` - 수신 버퍼 크기 확인
  - `Serial2.readBytes(buffer, length)` - 바이트 배열 읽기
  - ESP32-S3는 UART2 (GPIO43/44) 지원

### Android USB Host - USB Serial 통신
- **출처**: [kshoji/usb-midi-driver](https://github.com/kshoji/usb-midi-driver) (USB Serial 참고)
- **Trust Score**: 9.2
- **핵심 내용**:
  - USB 장치 권한 요청: `UsbManager.requestPermission(device, pendingIntent)`
  - USB Serial 포트 열기: `UsbSerialPort.open(connection)`
  - 데이터 전송: `UsbSerialPort.write(data, timeout)`
  - 장치 필터링: `res/xml/device_filter.xml` 설정

### USB Serial for Android 라이브러리
- **GitHub**: [mik3y/usb-serial-for-android](https://github.com/mik3y/usb-serial-for-android)
- **버전**: 3.9.0
- **핵심 기능**:
  - CP210x, FTDI, CH34x, PL2303 등 다양한 USB-Serial 칩 지원
  - 범용 USB CDC-ACM 드라이버
  - 1Mbps 이상 고속 전송 지원

---

## 📝 작성자 노트

이 테스트 가이드는 **Phase 1.1.3.2: 기본 연결 및 프레임 송수신 테스트**를 위해 작성되었습니다. 
Context7에서 검색한 공식 문서 (ESP32 Arduino Core, Android USB Host API, USB Serial for Android)를 기반으로 작성되었으며, 
각 테스트 항목은 개발 계획 체크리스트의 요구사항을 충족하도록 설계되었습니다.

**테스트 완료 후**: 
- 본 문서의 "테스트 결과 보고서 템플릿"을 작성하여 `docs/test-reports/` 디렉토리에 저장해주세요.
- Phase 1.1.3.2 체크박스를 완료 처리하고 다음 Phase로 진행해주세요.

**문의사항**: 
- ESP32-S3 펌웨어: `src/board/BridgeOne/src/main.cpp`
- Android 앱: `src/android/app/src/main/java/com/chatterbones/bridgeone/`
- 기술 문서: `docs/android/technical-specification-app.md`, `docs/board/esp32s3-code-implementation-guide.md`

