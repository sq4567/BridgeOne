---
title: "BridgeOne 바이브 코딩 개발 계획 체크리스트"
description: "BridgeOne 프로젝트의 4단계 체계적 개발 계획 - Android, ESP32-S3, Windows 서버 통합 개발을 위한 바이브 코딩 활용 가이드"
tags: ["android", "esp32-s3", "windows", "development", "checklist", "planning", "integration", "vibe-coding"]
version: "v1.0"
owner: "Chatterbones"
updated: "2025-10-08"
---

# BridgeOne 바이브 코딩 개발 계획 체크리스트

**개요**: 이 가이드는 Cursor 바이브 코딩을 활용하여 BridgeOne 프로젝트를 체계적으로 개발하기 위한 실무 중심 체크리스트입니다.

## ⚠️ 전체 프로젝트 공통 주의사항

### ESP32-S3 USB 초기화 관련
**중요**: `ARDUINO_USB_CDC_ON_BOOT=1` 설정 사용 시 USB.begin() 호출 금지!
- ❌ **하지 말 것**: `USB.begin()` 호출
- ✅ **해야 할 것**: `Mouse.begin()`, `Keyboard.begin()` 등 개별 인터페이스만 초기화
- **이유**: USB가 부팅 시 이미 자동 시작되므로, `USB.begin()` 재호출 시 USB 재열거 충돌로 bootloop 발생
- **영향 범위**: Phase 1.2.1.1, 1.2.1.2, 1.2.1.3 모두 해당

## Phase별 개발 체크리스트

### Phase 1: 점진적 통신 구축 및 검증

**목표**: Android ↔ Board ↔ Windows 간 통신을 단계별로 구축하며 각 단계마다 즉시 테스트
**예상 기간**: 3-4주
**완료 기준**: E2E 통신 완성, Essential/Standard 모드 전환, 지연시간 50ms 이하 달성

#### 1.1 Android ↔ Board 기본 통신 구축 및 테스트

**단계 목표**: Android 앱과 ESP32-S3 간 기본 USB 통신 완성 및 검증
**예상 기간**: 5-6일
**검증 기준**: 8바이트 프레임 송수신 성공, 순번 카운터 정상 동작

##### 1.1.1 Android 기본 USB 통신 구현

**참조 문서**:
- `@docs/Android/technical-specification-app.md` §1 (USB 통신)

###### 1.1.1.1 USB Serial 라이브러리 통합 및 기본 설정

**구현 목표**: usb-serial-for-android 라이브러리 추가 및 필수 권한 설정
**예상 시간**: 20-30분
**시작 상태**: `src/android/` Jetpack Compose 프로젝트 준비됨

**바이브 코딩 프롬프트**:
```
@src/android/app/build.gradle.kts 에 usb-serial-for-android 3.9.0 라이브러리 의존성을 추가해줘.
@src/android/app/src/main/AndroidManifest.xml 에 다음 권한들을 추가해:
1. USB 호스트 모드 권한 (android.hardware.usb.host)
2. USB 액세서리 권한 (android.hardware.usb.accessory)
3. 진동 권한 (android.permission.VIBRATE)

그리고 MainActivity에 USB 장치 연결 Intent Filter를 추가해줘 (action: android.hardware.usb.action.USB_DEVICE_ATTACHED).
```

**검증 방법**:
- [ ] Gradle sync 성공 확인
- [ ] AndroidManifest.xml에 3개 권한 추가 확인
- [ ] USB_DEVICE_ATTACHED Intent Filter 추가 확인
- [ ] 빌드 오류 없음 확인

###### 1.1.1.2 USB 장치 감지 및 권한 요청 구현

**구현 목표**: ESP32-S3 DevKitC-1 UART 포트 자동 감지 및 사용자 권한 요청 시스템
**예상 시간**: 40-50분
**참조**: `@docs/Android/technical-specification-app.md` §1.1.1-1.1.2

**바이브 코딩 프롬프트**:
```
@src/android/app/src/main/java/com/chatterbones/bridgeone/ 에 새 패키지 'usb'를 만들고, 
UsbConnectionManager.kt 클래스를 작성해:

1. UsbManager를 통해 연결된 USB 장치 목록 스캔
2. USB-Serial-for-Android 라이브러리의 범용 드라이버로 ESP32-S3 DevKitC-1 UART 포트 (내장 USB-Serial 칩) 자동 탐지
3. 장치 발견 시 PendingIntent로 권한 요청
4. 권한 승인/거부 BroadcastReceiver 처리
5. 연결 상태를 StateFlow로 관리 (Disconnected, Requesting, Connected, Error)

@docs/Android/technical-specification-app.md §1.1.2의 연결 관리 요구사항을 참고해서 구현해줘.
```

**검증 방법**:
- [ ] ESP32-S3 DevKitC-1 UART 포트 연결 시 자동 감지 확인
- [ ] 권한 요청 다이얼로그 표시 확인
- [ ] 권한 승인 시 Connected 상태 전환 확인
- [ ] 권한 거부 시 Error 상태 및 메시지 표시 확인

###### 1.1.1.3 USB Serial 포트 초기화 및 통신 설정

**구현 목표**: 1Mbps UART 통신 설정 및 포트 열기
**예상 시간**: 30-40분
**참조**: `@docs/Android/technical-specification-app.md` §1.1.1

**바이브 코딩 프롬프트**:
```
@src/android/app/src/main/java/com/chatterbones/bridgeone/usb/UsbConnectionManager.kt 에
openSerialPort() 메서드를 추가해:

1. UsbSerialDriver 인스턴스 생성 (usb-serial-for-android)
2. 통신 파라미터 설정:
   - baudRate: 1000000 (1Mbps)
   - dataBits: 8
   - stopBits: UsbSerialPort.STOPBITS_1
   - parity: UsbSerialPort.PARITY_NONE
3. 포트 열기 및 예외 처리 (IOException, SecurityException)
4. 연결 성공 시 읽기 타임아웃 200ms 설정

실패 시 적절한 오류 메시지와 함께 Error 상태로 전환해줘.
```

**검증 방법**:
- [ ] 포트 열기 성공 시 Connected 상태 유지
- [ ] 통신 파라미터 정확히 설정 확인 (로그 출력)
- [ ] 연결 실패 시 명확한 오류 메시지 표시
- [ ] 타임아웃 설정 확인

###### 1.1.1.4 BridgeOne 프레임 데이터 클래스 구현

**구현 목표**: 8바이트 프레임 구조 및 직렬화/역직렬화
**예상 시간**: 30-40분
**참조**: `@docs/Board/esp32s3-code-implementation-guide.md` §2.1

**바이브 코딩 프롬프트**:
```
@src/android/app/src/main/java/com/chatterbones/bridgeone/ 에 새 패키지 'protocol'을 만들고,
BridgeFrame.kt 데이터 클래스를 작성해:

1. 8바이트 구조 정의:
   data class BridgeFrame(
     val seq: UByte,         // [0] 순번 (0-255)
     val buttons: UByte,     // [1] 마우스 버튼 (bit0=L, bit1=R, bit2=M)
     val deltaX: Byte,       // [2] X축 이동 (-127~127)
     val deltaY: Byte,       // [3] Y축 이동 (-127~127)
     val wheel: Byte,        // [4] 휠 (-127~127)
     val modifiers: UByte,   // [5] 키보드 모디파이어
     val keyCode1: UByte,    // [6] 키 코드 1
     val keyCode2: UByte     // [7] 키 코드 2
   )

2. toByteArray() 메서드: Little-Endian으로 8바이트 직렬화
3. companion object fromByteArray() 메서드: 8바이트에서 역직렬화
4. 범위 검증 로직 포함

@docs/Board/esp32s3-code-implementation-guide.md §2.1의 프레임 구조를 정확히 따라줘.
```

**검증 방법**:
- [ ] BridgeFrame 객체 생성 성공
- [ ] toByteArray() 결과가 정확히 8바이트
- [ ] fromByteArray()로 원본 복원 확인
- [ ] Little-Endian 바이트 순서 확인 (단위 테스트)

###### 1.1.1.5 순번 카운터 관리 시스템 구현

**구현 목표**: 0-255 순환 순번 카운터 및 중복 제거
**예상 시간**: 20-30분

**바이브 코딩 프롬프트**:
```
@src/android/app/src/main/java/com/chatterbones/bridgeone/protocol/ 에
SequenceCounter.kt 클래스를 작성해:

1. 현재 순번 상태 관리 (0-255 순환)
2. next() 메서드: 순번 증가 및 255→0 순환
3. validate(receivedSeq: UByte) 메서드: 수신 순번 검증
4. 예상 순번과 불일치 시 경고 로그
5. 순번 통계 (전송/수신/손실 카운터)

순번 검증은 순차적 증가만 확인하고, 일시적 순번 불일치는 경고만 출력해줘.
```

**검증 방법**:
- [ ] next() 호출 시 순번 증가 확인
- [ ] 255 다음에 0으로 순환 확인
- [ ] validate() 정상 순번에서 true 반환
- [ ] 순번 불일치 시 로그 출력 확인

###### 1.1.1.6 기본 터치 입력 → 프레임 변환 구현

**구현 목표**: 터치 이벤트를 BridgeFrame으로 변환하는 기본 로직
**예상 시간**: 40-50분
**참조**: `@docs/Android/technical-specification-app.md` §2.2.1

**바이브 코딩 프롬프트**:
```
@src/android/app/src/main/java/com/chatterbones/bridgeone/ 에 새 패키지 'input'을 만들고,
TouchInputProcessor.kt 클래스를 작성해:

1. 터치 이벤트 수신 (MotionEvent.ACTION_DOWN, ACTION_MOVE, ACTION_UP)
2. 이전 터치 좌표와 현재 좌표로 deltaX/deltaY 계산
3. 델타값을 -127~127 범위로 클램핑
4. BridgeFrame 생성 (순번 카운터 자동 증가)
5. ACTION_DOWN 시 buttons=0x01 (좌클릭), ACTION_UP 시 buttons=0x00

간단한 좌클릭 드래그만 지원하고, 나머지 필드는 0으로 설정해줘.
@docs/Android/technical-specification-app.md §2.2의 터치패드 알고리즘은 나중에 구현할 거야.
```

**검증 방법**:
- [ ] 터치 드래그 시 deltaX/deltaY 계산 확인
- [ ] 델타값이 -127~127 범위 내 확인
- [ ] 터치 시작 시 buttons=0x01 확인
- [ ] 터치 종료 시 buttons=0x00 확인

##### 1.1.2 Board 기본 UART 수신 구현

**참조 문서**:
- `@docs/Board/esp32s3-code-implementation-guide.md` §2 (통신 프로토콜)

###### 1.1.2.1 UART 통신 초기화 및 기본 설정

**구현 목표**: ESP32-S3 UART2 포트 초기화 및 1Mbps 통신 설정
**예상 시간**: 30-40분
**시작 상태**: `src/board/BridgeOne/` PlatformIO 프로젝트 준비됨

**바이브 코딩 프롬프트**:
```
@src/board/BridgeOne/src/main.cpp 를 수정해:

1. HardwareSerial 인스턴스 생성 (UART2 사용)
2. setup() 함수에서 UART 초기화:
   - 1,000,000 bps (1Mbps)
   - 8N1 설정 (SERIAL_8N1)
   - RX 핀: GPIO44, TX 핀: GPIO43 (보드 내장 UART 포트, GPIO43/44)
   - 버퍼 크기: RX 256 bytes, TX 128 bytes
3. 시리얼 모니터 초기화 (115200 bps)
4. 초기화 완료 메시지 출력

@src/board/BridgeOne/platformio.ini 의 하드웨어 설정을 참고해줘.
```

**검증 방법**:
- [ ] 빌드 성공 확인
- [ ] 펌웨어 업로드 성공
- [ ] 시리얼 모니터에 초기화 메시지 출력 확인
- [ ] UART2 핀 할당 로그 확인

###### 1.1.2.2 BridgeOne 프레임 구조체 정의

**구현 목표**: C++ 프레임 구조체 및 파싱 함수
**예상 시간**: 20-30분
**참조**: `@docs/Board/esp32s3-code-implementation-guide.md` §2.1

**바이브 코딩 프롬프트**:
```
@src/board/BridgeOne/src/main.cpp 상단에 BridgeFrame 구조체를 정의해:

1. __attribute__((packed)) 속성으로 정확히 8바이트 크기 보장
2. 필드 정의:
   typedef struct __attribute__((packed)) {
     uint8_t seq;        // [0] 순번 (0-255)
     uint8_t buttons;    // [1] 마우스 버튼
     int8_t deltaX;      // [2] X축 이동
     int8_t deltaY;      // [3] Y축 이동
     int8_t wheel;       // [4] 휠
     uint8_t modifiers;  // [5] 키보드 모디파이어
     uint8_t keyCode1;   // [6] 키 코드 1
     uint8_t keyCode2;   // [7] 키 코드 2
   } BridgeFrame;

3. 전역 변수로 BridgeFrame 버퍼 선언
4. sizeof(BridgeFrame) == 8 컴파일 타임 체크 추가

@docs/Board/esp32s3-code-implementation-guide.md §2.1의 프레임 구조를 정확히 따라줘.
```

**검증 방법**:
- [ ] 빌드 시 sizeof(BridgeFrame) == 8 확인
- [ ] __attribute__((packed)) 적용 확인
- [ ] 컴파일 오류 없음

###### 1.1.2.3 UART 수신 및 프레임 파싱 구현

**구현 목표**: 8바이트 프레임 수신 및 파싱 로직
**예상 시간**: 40-50분

**바이브 코딩 프롬프트**:
```
@src/board/BridgeOne/src/main.cpp 에 receiveFrame() 함수를 작성해:

1. UART2.available() >= 8 체크
2. 8바이트를 BridgeFrame 구조체로 읽기
3. 읽기 성공 시 true 반환, 실패 시 false
4. loop() 함수에서 receiveFrame() 호출
5. 수신 성공 시 프레임 내용을 시리얼 모니터에 출력:
   - "RX: seq=%d, buttons=0x%02X, delta=(%d,%d), wheel=%d"
   
버퍼 오버플로우 방지를 위해 available() 체크를 철저히 해줘.
```

**검증 방법**:
- [ ] Android 앱에서 프레임 전송 시 ESP32-S3 수신 확인
- [ ] 시리얼 모니터에 프레임 내용 출력 확인
- [ ] 8바이트 미만일 때 대기하는지 확인
- [ ] 버퍼 오버플로우 없음

###### 1.1.2.4 순번 카운터 검증 로직 구현

**구현 목표**: 순번 검증 및 중복/손실 감지
**예상 시간**: 30-40분

**바이브 코딩 프롬프트**:
```
@src/board/BridgeOne/src/main.cpp 에 순번 검증 기능을 추가해:

1. 전역 변수: 
   - uint8_t lastSeq = 255 (초기값)
   - uint32_t frameCount = 0 (수신 카운터)
   - uint32_t lostFrames = 0 (손실 카운터)

2. validateSequence(uint8_t currentSeq) 함수:
   - 첫 프레임이면 lastSeq 업데이트하고 true 반환
   - 예상 순번: (lastSeq + 1) % 256
   - 순번 일치하면 lastSeq 업데이트, frameCount++
   - 불일치하면 lostFrames 계산, 경고 출력
   
3. receiveFrame() 후 validateSequence() 호출
4. 1초마다 통계 출력 (수신/손실 프레임 수)

255→0 순환을 정확히 처리해줘.
```

**검증 방법**:
- [x] 정상 순번에서 검증 통과 확인
- [x] 255→0 순환 정상 처리 확인
- [x] 순번 불일치 시 경고 메시지 출력
- [x] 통계 정보 1초마다 출력 확인

**완료 상태**: ✅ 완료
**완료 시간**: 2025-01-09

###### 1.1.2.5 FreeRTOS 태스크 구조 구현

**구현 목표**: UART 수신 및 디버그 출력을 별도 태스크로 분리
**예상 시간**: 40-50분
**참조**: `@docs/Board/esp32s3-code-implementation-guide.md` §4 (FreeRTOS)

**바이브 코딩 프롬프트**:
```
@src/board/BridgeOne/src/main.cpp 를 FreeRTOS 멀티태스킹 구조로 변경해:

1. TaskHandle_t 선언:
   - uartRxTaskHandle (UART 수신)
   - debugTaskHandle (디버그 출력)

2. uartRxTask() 함수 (우선순위 3, Core 1):
   - receiveFrame() 호출
   - 프레임 수신 시 큐에 추가 (나중에 HID 전송용)
   - 5ms vTaskDelay
   
3. debugTask() 함수 (우선순위 1, Core 0):
   - 1초마다 통계 정보 출력
   - 메모리 사용량, 수신 프레임 수, 손실률
   
4. setup()에서 xTaskCreatePinnedToCore()로 태스크 생성
5. loop()는 비워두고 vTaskDelete(NULL)

@docs/Board/esp32s3-code-implementation-guide.md §4의 태스크 설계를 참고해줘.
```

**검증 방법**:
- [x] 두 태스크 정상 생성 확인 (로그)
- [x] UART 수신이 별도 태스크에서 동작
- [x] 디버그 정보 1초마다 출력
- [x] CPU 코어 분산 확인 (Core 0, Core 1)

##### 1.1.3 Android ↔ Board 통신 테스트

###### 1.1.3.1 Android 앱 통신 레이어 통합 및 UI 구현

**구현 목표**: USB 통신, 프레임 전송, 터치 입력을 MainActivity에 통합
**예상 시간**: 50분-1시간
**참조**: 1.1.1.2~1.1.1.6에서 구현한 모듈들

**바이브 코딩 프롬프트**:
```
@src/android/app/src/main/java/com/chatterbones/bridgeone/MainActivity.kt 를 수정해:

1. UsbConnectionManager, TouchInputProcessor 인스턴스 생성
2. Compose UI 구성:
   - 상단: USB 연결 상태 표시 (Disconnected/Requesting/Connected/Error)
   - 중앙: 터치패드 영역 (전체 화면의 80%, 테두리 표시)
   - 하단: 통계 정보 (전송 프레임 수, 순번, 초당 전송률)
3. 터치패드 영역에 pointerInput 모디파이어 적용:
   - TouchInputProcessor로 터치 이벤트 전달
   - BridgeFrame 생성 → UsbConnectionManager로 전송
4. 연결 상태에 따른 UI 업데이트 (StateFlow 수집)
5. 터치 시 햅틱 피드백 (Vibrator 시스템 서비스, 10ms 진동)

간단한 Material3 디자인으로 구현하고, 실시간 통계를 1초마다 업데이트해줘.
```

**검증 방법**:
- [ ] 앱 실행 시 USB 장치 자동 감지
- [ ] 연결 상태 UI에 정확히 표시
- [ ] 터치패드 영역에서 터치 이벤트 감지
- [ ] 통계 정보 실시간 업데이트

###### 1.1.3.2 기본 연결 및 프레임 송수신 테스트

**구현 목표**: Android ↔ ESP32-S3 통신 End-to-End 검증
**예상 시간**: 30-40분 (수동 테스트)
**필요 장비**: Android 디바이스, ESP32-S3, USB OTG 케이블

**테스트 프롬프트**:
```
다음 수동 테스트를 진행하고 결과를 체크해:

1. **기본 연결 테스트**:
   - Android 앱 실행 → ESP32-S3 연결
   - USB 권한 요청 다이얼로그 승인
   - 앱에 "Connected" 상태 표시 확인
   - ESP32-S3 시리얼 모니터에 "UART initialized" 메시지 확인

2. **프레임 송신 테스트**:
   - Android 터치패드 영역을 여러 방향으로 드래그
   - ESP32-S3 시리얼 모니터에 프레임 수신 로그 확인
   - "RX: seq=0, 1, 2, ..." 순차 증가 확인
   - deltaX/Y 값이 드래그 방향과 일치하는지 확인

3. **순번 카운터 순환 테스트**:
   - 256번 이상 터치 이벤트 생성 (연속 드래그)
   - seq=254, 255, 0, 1 순환 확인
   - ESP32-S3 순번 검증 통과 확인

4. **1분 연속 전송 안정성**:
   - 1분간 계속 터치 입력
   - 프레임 손실률 <1% 확인
   - 앱 크래시 없음
   - ESP32-S3 메모리 누수 없음
```

**검증 방법**:
- [x] Android 앱에서 ESP32-S3 장치 자동 인식
- [x] USB 연결 성공 시 Connected 상태 표시
- [x] 터치 이벤트 → 프레임 전송 → ESP32-S3 수신 확인
- [x] 순번 카운터 순환 (0→255→0) 정상 동작
- [ ] 1분간 연속 전송 안정성 확인 (손실률 <1%) ⚠️ **실패 (91% 손실률)**

**테스트 결과**: ⚠️ **부분 통과** (2025-10-15)
- ✅ 기능 검증: 통신 인프라, 프로토콜, 순번 순환 정상 동작
- ❌ 성능 이슈: 프레임 손실률 91% (32 프레임마다 규칙적 손실)
- 📋 **보고서**: `docs/test-reports/Phase-1.1.3.2-Test-Report.md`
- 🔧 **조치 계획**: Phase 1.3.2 성능 최적화에서 해결 예정 (UART 버퍼 증가, 전송 빈도 제한)

#### 1.2 Board ↔ Windows 기본 통신 구축 및 테스트

**단계 목표**: ESP32-S3와 Windows 서버 간 HID/CDC 통신 완성 및 검증
**예상 기간**: 5-6일
**검증 기준**: HID 마우스/키보드 인식, Vendor CDC 명령 송수신 성공

##### 1.2.1 Board USB HID 출력 구현

**참조 문서**: `@docs/Board/esp32s3-code-implementation-guide.md` §3 (USB HID 출력)

###### 1.2.1.1 Arduino USB HID 라이브러리 초기화

**구현 목표**: ESP32-S3 Arduino HID 라이브러리 설정 및 USB 복합 장치 구성
**예상 시간**: 30-40분
**참조**: `@src/board/BridgeOne/platformio.ini` (TinyUSB 설정 이미 완료)

**⚠️ 중요: USB.begin() 호출 금지!**
- `ARDUINO_USB_CDC_ON_BOOT=1` 설정으로 USB는 부팅 시 자동 시작됨
- `USB.begin()`을 호출하면 USB 재열거(re-enumeration) 충돌로 **bootloop 발생**
- **Mouse.begin()과 Keyboard.begin()만 호출**하여 HID 인터페이스를 기존 USB에 추가

**바이브 코딩 프롬프트**:
```
@src/board/BridgeOne/src/main.cpp 에 USB HID 초기화를 추가해:

1. Arduino HID 라이브러리 포함 (USB.h는 불필요):
   #include "USBHIDMouse.h"
   #include "USBHIDKeyboard.h"

2. 전역 객체 생성:
   USBHIDMouse Mouse;
   USBHIDKeyboard Keyboard;

3. setup() 함수에 USB 초기화 (순서 중요!):
   a. Serial.begin(115200) 먼저 호출
   b. delay(2000) - CDC 안정화
   c. Mouse.begin()
   d. Keyboard.begin()
   e. delay(500) - HID 등록 안정화
   f. 초기화 성공 메시지 출력

4. ⚠️ 주의사항:
   - USB.begin() 호출 금지! (이미 CDC_ON_BOOT=1로 자동 시작됨)
   - Serial.begin()을 HID 초기화보다 먼저 호출
   - 충분한 delay로 각 단계 안정화

@src/board/BridgeOne/platformio.ini 의 TinyUSB 설정이 이미 활성화되어 있으니 그대로 사용해.
```

**검증 방법**:
- [x] 빌드 성공 확인
- [x] Windows PC 연결 시 HID 마우스/키보드 자동 인식
- [x] 장치 관리자에서 "HID-compliant mouse" 표시
- [x] 시리얼 모니터에 USB 초기화 메시지 출력
- [x] bootloop 없이 정상 부팅 확인

###### 1.2.1.2 BridgeFrame → HID 마우스 변환 구현

**구현 목표**: BridgeFrame을 HID 마우스 리포트로 변환
**예상 시간**: 40-50분
**참조**: `@docs/Board/esp32s3-code-implementation-guide.md` §3.1

**⚠️ 주의**: 이미 초기화된 Mouse 객체 사용 (재초기화 금지)

**바이브 코딩 프롬프트**:
```
@src/board/BridgeOne/src/main.cpp 에 processMouseInput() 함수를 추가해:

1. BridgeFrame에서 마우스 관련 필드 추출:
   - buttons (bit0=좌클릭, bit1=우클릭, bit2=중클릭)
   - deltaX, deltaY (커서 이동)
   - wheel (스크롤)

2. 버튼 처리:
   - 이전 버튼 상태와 비교하여 press/release 판정
   - Mouse.press(MOUSE_LEFT/RIGHT/MIDDLE)
   - Mouse.release(MOUSE_LEFT/RIGHT/MIDDLE)

3. 이동 처리:
   - deltaX나 deltaY가 0이 아니면 Mouse.move(deltaX, deltaY, wheel)

4. 40ms 디바운싱 적용 (lastMouseUpdate 타임스탬프)

⚠️ Mouse 객체는 Phase 1.2.1.1에서 이미 초기화됨 - 재초기화하지 말 것!
이전 버튼 상태를 전역 변수로 저장해서 변화 감지해줘.
```

**검증 방법**:
- [x] BridgeFrame 수신 시 마우스 이동 확인
- [x] 버튼 상태 변화 시 클릭 이벤트 발생
- [x] 휠 값에 따라 스크롤 동작
- [x] 40ms 디바운싱 동작 확인

**✅ 완료 상태**: ✅ **구현 완료** (2025-10-18)

**구현 내용**:
- `processMouseInput()` 함수 구현: BridgeFrame → HID Mouse 변환
- 버튼 상태 추적 (press/release 감지)
- `Mouse.move()` API를 통한 커서 이동 및 휠 스크롤
- 40ms 디바운싱 적용 (과도한 HID 전송 방지)
- UART 수신 태스크에서 실시간 HID 입력 처리
- Context7 공식 문서 기반 Arduino ESP32 HID API 활용

**빌드 검증**:
- ✅ PlatformIO 빌드 성공
- ✅ Linter 오류 없음
- ✅ RAM 사용량: 6.2% (20,348 bytes)
- ✅ Flash 사용량: 8.8% (295,761 bytes)


###### 1.2.1.3 BridgeFrame → HID 키보드 변환 구현

**구현 목표**: BridgeFrame을 HID 키보드 리포트로 변환
**예상 시간**: 40-50분
**참조**: `@docs/Board/esp32s3-code-implementation-guide.md` §3.2

**⚠️ 주의**: 이미 초기화된 Keyboard 객체 사용 (재초기화 금지)

**바이브 코딩 프롬프트**:
```
@src/board/BridgeOne/src/main.cpp 에 processKeyboardInput() 함수를 추가해:

1. BridgeFrame에서 키보드 관련 필드 추출:
   - modifiers (bit0=Ctrl, bit1=Shift, bit2=Alt, bit3=GUI)
   - keyCode1, keyCode2 (HID 키 코드)

2. 모디파이어 처리:
   - Keyboard.press(KEY_LEFT_CTRL) 등 모디파이어 키 press/release
   - 이전 상태와 비교하여 변화된 것만 처리

3. 일반 키 처리:
   - keyCode1/2가 0이 아니면 Keyboard.press(keyCode)
   - 0이면 Keyboard.releaseAll()

4. 키 입력 안정성을 위해 30ms 간격 유지

⚠️ Keyboard 객체는 Phase 1.2.1.1에서 이미 초기화됨 - 재초기화하지 말 것!
모디파이어와 일반 키를 동시에 처리할 수 있도록 해줘.
```

**검증 방법**:
- [ ] 모디파이어 키 조합 정상 동작 (Ctrl+C 등)
- [ ] 일반 키 입력 정상 동작
- [ ] keyCode=0 시 모든 키 해제 확인
- [ ] 30ms 간격 유지 확인

###### 1.2.1.4 홀드 입력 반복 처리 구현

**구현 목표**: 키 홀드 시 자동 반복 입력 구현
**예상 시간**: 30-40분

**바이브 코딩 프롬프트**:
```
@src/board/BridgeOne/src/main.cpp 에 홀드 입력 반복 로직을 추가해:

1. 홀드 상태 추적:
   - uint8_t holdKey = 0 (현재 홀드 중인 키)
   - uint32_t holdStartTime = 0 (홀드 시작 시간)
   - uint32_t lastRepeatTime = 0 (마지막 반복 시간)

2. processHoldInput() 함수:
   - 동일한 키가 300ms 이상 지속되면 홀드 상태 진입
   - 홀드 중일 때 30ms 간격으로 Keyboard.write() 반복
   - 키가 변경되거나 0이 되면 홀드 해제

3. processKeyboardInput()에서 홀드 로직 호출

화살표 키, 백스페이스 등 반복이 필요한 키에 유용하게 동작하도록 해줘.
```

**검증 방법**:
- [x] 키 300ms 홀드 시 자동 반복 시작
- [x] 30ms 간격으로 반복 입력 확인
- [x] 키 해제 시 반복 중단
- [x] 다른 키로 전환 시 이전 홀드 취소

**완료 일시**: 2025-01-18
**테스트 문서**: [Phase-1.2.1.4-Hold-Input-Test-Checklist.md](test-reports/Phase-1.2.1.4-Hold-Input-Test-Checklist.md)

**구현 내용**:
1. ✅ 홀드 상태 추적 변수 추가:
   - `g_holdKey`: 현재 홀드 중인 키 (0이면 홀드 없음)
   - `g_holdStartTime`: 홀드 시작 시간 (ms)
   - `g_lastRepeatTime`: 마지막 반복 시간 (ms)

2. ✅ 홀드 타이밍 설정:
   - `HOLD_THRESHOLD_MS = 300`: 홀드 상태 진입 임계값
   - `REPEAT_INTERVAL_MS = 30`: 반복 입력 간격

3. ✅ `processHoldInput()` 함수 구현:
   - 케이스 1: 키 입력 없음 → 홀드 완전히 해제
   - 케이스 2: 키 변경 → 이전 홀드 취소, 새 키 타이머 시작
   - 케이스 3: 동일 키 유지 → 홀드 시간 체크 및 반복 입력
   - `Keyboard.write()` 사용 (press + release 자동 수행)

4. ✅ `processKeyboardInput()`에서 홀드 로직 호출
5. ✅ 함수 프로토타입 선언 추가
6. ✅ 빌드 성공 확인 (RAM: 6.2%, Flash: 8.9%)

**Context7 공식 문서 교차 검증**:
- ✅ Arduino Keyboard API: https://docs.arduino.cc/language-reference/en/functions/usb/Keyboard
- ✅ ESP32 Arduino Core: https://github.com/espressif/arduino-esp32
- ✅ TinyUSB: https://github.com/hathach/tinyusb
- ✅ `Keyboard.write()` = press + release 자동 수행 확인
- ✅ 반복 입력은 애플리케이션 레벨에서 구현 (HID 레벨 미지원)

**다음 단계**: Phase 1.2.1.5 FreeRTOS HID 전송 태스크 통합

###### 1.2.1.5 FreeRTOS HID 전송 태스크 통합

**구현 목표**: UART 수신과 HID 전송을 FreeRTOS 큐로 연결
**예상 시간**: 50분-1시간
**참조**: 1.1.2.5의 FreeRTOS 구조

**바이브 코딩 프롬프트**:
```
@src/board/BridgeOne/src/main.cpp 의 FreeRTOS 구조를 확장해:

1. QueueHandle_t frameQueue 생성 (최대 32개 BridgeFrame)

2. uartRxTask() 수정:
   - 프레임 수신 시 frameQueue에 추가
   - 큐 가득 참 시 경고 로그

3. hidTxTask() 새로 생성 (우선순위 3, Core 1):
   - frameQueue에서 BridgeFrame 꺼내기
   - processMouseInput() 호출
   - processKeyboardInput() 호출
   - processHoldInput() 호출
   - 1ms vTaskDelay (1000 FPS 목표)

4. setup()에서 frameQueue 및 hidTxTask 생성

5. debugTask()에 HID 전송 통계 추가 (FPS, 큐 사용률)

큐를 사용해서 UART 수신과 HID 전송을 분리해줘.
```

**검증 방법**:
- [ ] frameQueue 정상 생성 확인
- [ ] UART 수신 → 큐 → HID 전송 파이프라인 동작
- [ ] HID 전송 FPS 1000Hz 근접 확인
- [ ] 큐 오버플로우 없음 확인

##### 1.2.2 Board Vendor CDC 구현 ✅

**참조 문서**:
- `@docs/Board/esp32s3-code-implementation-guide.md` §2.3 (Vendor CDC)
- `@docs/technical-specification.md` §3.2 (Vendor CDC 프로토콜)

**완료 상태**: ✅ 완료 (2025-01-18)
**전체 세부사항**:
- Phase 1.2.2.1: USB Serial CDC 인터페이스 초기화 완료
- Phase 1.2.2.2: JSON 메시지 프레임 구조 구현 완료
- Phase 1.2.2.3: 명령 처리 시스템 구현 완료
- ArduinoJson 기반 JSON 직렬화/역직렬화
- CRC16-CCITT 무결성 검증
- FreeRTOS CDC 수신 태스크 (Core 0, 우선순위 2)
- 명령 타입 enum 및 처리 시스템
- 메시지 ID 기반 요청-응답 매칭
- Context7 공식 문서 기반 구현 (Trust Score 9.0+ 출처 사용)

###### 1.2.2.1 USB Serial CDC 인터페이스 초기화

**구현 목표**: ESP32-S3에서 제공하는 USB Serial(CDC)를 Vendor CDC로 활용
**예상 시간**: 30-40분

**바이브 코딩 프롬프트**:
```
@src/board/BridgeOne/src/main.cpp 에 Vendor CDC 초기화를 추가해:

1. Serial 객체를 Vendor CDC로 활용 (별도 라이브러리 불필요)
2. setup()에서 Serial.begin(115200) - CDC는 baud rate 무시하지만 형식상 호출
3. Serial.setTimeout(100) - 100ms 읽기 타임아웃
4. CDC 초기화 완료 메시지 출력

ESP32-S3의 USB Serial은 자동으로 CDC 인터페이스로 노출되니, 
이를 Vendor CDC 용도로 사용하면 돼. Windows에서는 COM 포트로 인식될 거야.
```

**검증 방법**:
- [x] Windows에서 ESP32-S3 CDC 장치 인식 (COM 포트)
- [x] 장치 관리자에서 "USB Serial Device" 표시
- [x] Serial 통신 준비 완료

**구현 결과** (2025-01-XX):
- ✅ Context7 공식 문서 참조 (Arduino ESP32 Core, Trust Score: 9.1)
- ✅ Serial.begin(115200) 및 Serial.setTimeout(100) 설정 완료
- ✅ g_cdcInitialized 플래그 추가
- ✅ cdcRxTask (Core 0, Priority 2) 생성 및 에코 응답 구현
- ✅ 디버그 태스크에 CDC 통계 추가 (RX/TX 카운터)
- ✅ 빌드 성공 (RAM: 6.2%, Flash: 9.0%)


###### 1.2.2.2 JSON 메시지 프레임 구조 구현

**구현 목표**: 0xFF 헤더 + JSON + CRC16 프레임 구조
**예상 시간**: 40-50분
**참조**: ArduinoJson, CRC 라이브러리 (platformio.ini에 이미 포함)

**바이브 코딩 프롬프트**:
```
@src/board/BridgeOne/src/main.cpp 에 JSON 메시지 송수신 구조를 추가해:

1. ArduinoJson 및 CRC 라이브러리 포함:
   #include <ArduinoJson.h>
   #include <CRC.h>

2. 프레임 구조:
   [0xFF][길이 2바ytes][JSON payload][CRC16 2bytes]

3. sendJsonMessage(const JsonDocument& doc) 함수:
   - JSON을 String으로 직렬화
   - 프레임 조립 (0xFF, 길이, payload, CRC16)
   - Serial.write()로 전송

4. receiveJsonMessage(JsonDocument& doc) 함수:
   - 0xFF 헤더 대기 및 검증
   - 길이 읽기 (2바이트, Little-Endian)
   - JSON payload 읽기
   - CRC16 검증
   - JSON 파싱하여 doc에 저장

@docs/technical-specification.md §3.2의 Vendor CDC 프로토콜을 참고해줘.
```

**검증 방법**:
- [ ] JSON 메시지 직렬화 성공
- [ ] 프레임 조립 (0xFF + 길이 + payload + CRC16)
- [ ] CRC16 계산 및 검증 성공
- [ ] JSON 파싱 성공

###### 1.2.2.3 명령 처리 시스템 구현

**구현 목표**: Windows → Board 명령 수신 및 응답 전송
**예상 시간**: 50분-1시간

**바이브 코딩 프롬프트**:
```
@src/board/BridgeOne/src/main.cpp 에 명령 처리 시스템을 추가해:

1. 지원 명령 정의 (enum):
   - CMD_PING (0x01) - 연결 확인
   - CMD_GET_STATUS (0x02) - 상태 정보 요청
   - CMD_SET_CONFIG (0x03) - 설정 변경

2. processCommand(const JsonDocument& request, JsonDocument& response) 함수:
   - request["cmd"]로 명령 타입 확인
   - 각 명령별 처리 로직
   - response에 결과 저장 (status, data)

3. 명령 처리 예시:
   - PING: {"status": "ok", "timestamp": millis()}
   - GET_STATUS: {"fps": xxx, "queueSize": xxx, "uptime": millis()}
   - SET_CONFIG: 설정 값 업데이트 및 확인 응답

4. FreeRTOS cdcTask() 추가 (우선순위 2, Core 0):
   - receiveJsonMessage() 호출
   - 명령 수신 시 processCommand() 실행
   - 응답을 sendJsonMessage()로 전송

메시지 ID 기반 요청-응답 매칭도 구현해줘 (request["id"] → response["id"]).
```

**검증 방법**:
- [x] ESP32-S3 빌드 및 업로드 성공
- [x] Serial Monitor 초기화 로그 정상 출력
- [x] FreeRTOS CDC 태스크 정상 시작
- [x] 명령 처리 시스템 초기화 확인
- [ ] ~~Windows에서 PING 명령 전송~~ → Phase 1.2.4.5에서 수행
- [ ] ~~GET_STATUS로 상태 정보 조회~~ → Phase 1.2.4.5에서 수행
- [x] **Python 스크립트로 JSON 프레임 직접 전송 테스트** (Phase 1.2.3 이전 선행 검증)
  - [x] `test_command_system.py` 스크립트 작성 및 COM 포트 설정
  - [x] CMD_PING 테스트 통과 (메시지 ID 매칭 확인)
  - [x] CMD_GET_STATUS 테스트 통과 (시스템 상태 수신)
  - [x] CMD_SET_CONFIG 테스트 통과 (설정 변경 확인)
  - [x] CRC16 검증 정상 동작
  - [x] Serial Monitor에서 명령 처리 로그 확인
- [x] CRC16 검증 로직 구현 완료 (실제 테스트는 Python 스크립트로 완료)

**참고**:
- Windows 서버가 구현되기 전까지는 Python 스크립트(`test_command_system.py`)를 사용하여 Board 단독으로 명령 처리 로직을 검증할 수 있습니다.
- Python 테스트는 선택사항이지만, Windows 서버 구현 전에 Board 로직을 검증하여 개발 시간을 단축할 수 있습니다.
- Phase 1.2.4.5에서 Windows 서버와 통합하여 최종 검증을 수행합니다.

**향후 활용 계획**:
- **Phase 1.2.4.5**: Windows 서버 구현 전 선행 검증 도구로 활용
- **Phase 4.1.1.1**: 전체 사용자 시나리오 테스트에서 일부 자동화
- **유지보수**: 펌웨어 업데이트 후 회귀 테스트용
- **디버깅**: 통신 문제 발생 시 원인 파악 도구

**완료 상태**: ✅ 완료 (2025-01-18)
**완료 세부사항**:
- CommandType enum 정의 (CMD_PING, CMD_GET_STATUS, CMD_SET_CONFIG)
- CommandStatus enum 정의 (OK, ERROR, INVALID, MISSING_PARAM)
- parseCommandType() 함수 구현: JSON에서 명령 타입 파싱
- processCommand() 함수 구현:
  - 메시지 ID 기반 요청-응답 매칭 (request["id"] → response["id"])
  - CMD_PING: 연결 확인 응답 (timestamp 포함)
  - CMD_GET_STATUS: 시스템 상태 정보 반환 (fps, queueSize, uptime, frameCount, lostFrames, hidTxCount, freeHeap)
  - CMD_SET_CONFIG: 설정 변경 처리 (debugMode, logLevel 예시)
- cdcRxTask() 업데이트: processCommand() 통합
- ArduinoJson API 사용 (Context7 공식 문서 기반)
- FreeRTOS 태스크에서 안정적으로 동작
- 에러 처리 및 검증 로직 포함
- 상세한 디버그 로그 출력

**Context7 공식 문서 출처**:
- ArduinoJson: https://github.com/bblanchon/arduinojson (Trust Score: 9.6)
  - deserializeJson(), serializeJson() API
  - JsonDocument, JsonObject, containsKey() 사용
- FreeRTOS: https://github.com/freertos/freertos-kernel (Trust Score: 9.4)
  - Task management and queue APIs
- ESP32 Arduino: https://github.com/espressif/arduino-esp32 (Trust Score: 9.1)
  - USB CDC Serial communication

##### 1.2.3 Windows 기본 장치 인식 구현

**참조 문서**:
- `@docs/Windows/technical-specification-server.md` §3 (핵심 기능)

###### 1.2.3.1 .NET 8.0 콘솔 프로젝트 구조 및 로깅 시스템

**구현 목표**: 기본 콘솔 애플리케이션 및 Serilog 로깅 설정
**예상 시간**: 30-40분
**시작 상태**: `src/windows/BridgeOne/` .NET 8.0 프로젝트 준비됨

**바이브 코딩 프롬프트**:
```
@src/windows/BridgeOne/BridgeOne.csproj 에 NuGet 패키지 추가:
1. Serilog (로깅)
2. Serilog.Sinks.Console
3. Serilog.Sinks.File
4. HidLibrary (HID 장치 통신)
5. System.IO.Ports (CDC 통신)

@src/windows/BridgeOne/Program.cs 를 수정해:
1. Serilog 초기화 (콘솔 + 파일 출력, Debug 레벨)
2. Main() 함수 비동기 패턴으로 변경 (async Task Main)
3. 기본 로깅 테스트 메시지 출력
4. 예외 처리 및 정상 종료 로직

간단한 콘솔 애플리케이션 구조로 시작하고, WPF는 나중에 추가할 거야.
```

**검증 방법**:
- [x] NuGet 패키지 복원 성공
- [x] 빌드 및 실행 성공
- [x] 콘솔에 로그 메시지 출력 확인
- [x] logs/ 디렉토리에 로그 파일 생성 확인

###### 1.2.3.2 HID 장치 열거 및 ESP32-S3 감지

**구현 목표**: HidLibrary를 사용한 HID 장치 자동 감지
**예상 시간**: 40-50분

**바이브 코딩 프롬프트**:
```
@src/windows/BridgeOne/ 에 새 클래스 HidDeviceManager.cs를 생성해:

1. HidLibrary를 사용하여 연결된 HID 장치 목록 스캔
2. ESP32-S3 HID 장치 필터링 (VID/PID 기반 또는 제품명)
3. HID 마우스 및 키보드 장치 모두 감지
4. 장치 연결/분리 이벤트 감지 (폴링 방식, 1초 간격)
5. 감지된 장치 정보 로깅

@src/windows/BridgeOne/Program.cs 의 Main()에서 HidDeviceManager 초기화 및
장치 감지 시작 코드를 추가해줘.

ESP32-S3는 HID-compliant mouse/keyboard로 인식될 거야.
```

**검증 방법**:
- [ ] HID 장치 목록 스캔 성공
- [ ] ESP32-S3 HID 마우스 감지
- [ ] ESP32-S3 HID 키보드 감지
- [ ] 장치 정보 로깅 (VID, PID, 제품명)

###### 1.2.3.3 Raw Input API 기반 HID 입력 수신

**구현 목표**: Windows Raw Input API로 HID 마우스/키보드 입력 수신
**예상 시간**: 1시간-1시간 20분
**참조**: `@docs/Windows/technical-specification-server.md` §3.1

**바이브 코딩 프롬프트**:
```
@src/windows/BridgeOne/ 에 새 클래스 RawInputHandler.cs를 생성해:

1. Win32 Raw Input API P/Invoke 선언:
   - RegisterRawInputDevices
   - GetRawInputData
   - WM_INPUT 메시지 처리

2. RegisterRawInput() 메서드:
   - RIDEV_INPUTSINK 플래그로 백그라운드 입력 수신
   - 마우스 (UsagePage=0x01, Usage=0x02)
   - 키보드 (UsagePage=0x01, Usage=0x06)

3. ProcessRawInput(IntPtr lParam) 메서드:
   - GetRawInputData로 입력 데이터 읽기
   - 마우스: deltaX/Y, 버튼 상태
   - 키보드: VKey, ScanCode, 상태

4. 입력 이벤트 핸들러 (event EventHandler<MouseInputEventArgs> 등)

@src/windows/BridgeOne/Program.cs 에서 RawInputHandler 초기화하고
입력 이벤트 구독하여 로깅해줘.

참고: Raw Input은 메시지 루프가 필요하니, Application.Run() 또는 
간단한 메시지 루프를 추가해야 할 수도 있어.
```

**검증 방법**:
- [ ] Raw Input 등록 성공
- [ ] ESP32-S3 마우스 입력 수신 확인
- [ ] ESP32-S3 키보드 입력 수신 확인
- [ ] 입력 데이터 로깅 (deltaX/Y, 키 코드)

**대안 방법 (Raw Input API 실패 시)**:
- [ ] Zadig를 사용하여 WinUSB 드라이버 설치
  - Zadig 다운로드: https://zadig.akeo.ie/
  - Options > List All Devices 체크
  - ESP32-S3 HID 장치 선택 (VID: 0x303A)
  - WinUSB 드라이버 설치
  - HidLibrary로 직접 장치 열기 방식으로 전환

###### 1.2.3.4 Windows 커서/키 입력 시뮬레이션

**구현 목표**: 수신한 HID 입력을 실제 Windows 입력으로 변환
**예상 시간**: 40-50분

**바이브 코딩 프롬프트**:
```
@src/windows/BridgeOne/ 에 새 클래스 InputSimulator.cs를 생성해:

1. SendInput API P/Invoke 선언
2. SimulateMouseMove(int deltaX, int deltaY) 메서드:
   - MOUSEEVENTF_MOVE 플래그
   - 상대 좌표로 커서 이동
3. SimulateMouseClick(MouseButton button, bool isDown) 메서드:
   - MOUSEEVENTF_LEFTDOWN/LEFTUP 등
4. SimulateKeyPress(ushort vKey, bool isDown) 메서드:
   - KEYEVENTF_KEYDOWN/KEYUP 플래그

@src/windows/BridgeOne/Program.cs 에서 RawInputHandler의 이벤트를
InputSimulator로 연결해서 실제 입력을 시뮬레이션해줘.

이렇게 하면 ESP32-S3 HID 입력 → Windows 실제 커서/키 입력으로 연결돼.
```

**검증 방법**:
- [ ] HID 마우스 입력 → Windows 커서 이동
- [ ] HID 키보드 입력 → Windows 키 입력
- [ ] 입력 지연시간 측정 (로그 출력)
- [ ] 실제 애플리케이션에서 입력 동작 확인

###### 1.2.3.5 Vendor CDC 통신 초기화

**구현 목표**: System.IO.Ports로 ESP32-S3 CDC 장치와 통신
**예상 시간**: 40-50분

**바이브 코딩 프롬프트**:
```
@src/windows/BridgeOne/ 에 새 클래스 VendorCdcManager.cs를 생성해:

1. SerialPort 인스턴스 생성 및 자동 COM 포트 감지:
   - ManagementObjectSearcher로 USB 장치 열거
   - ESP32-S3 CDC 장치의 COM 포트 번호 찾기
   - 115200 baud rate, 8N1 설정

2. SendJsonCommand(string command, object data) 메서드:
   - JSON 직렬화 (System.Text.Json)
   - 프레임 조립 (0xFF + 길이 + JSON + CRC16)
   - SerialPort.Write()로 전송

3. ReceiveJsonResponse() 메서드:
   - 0xFF 헤더 대기
   - 프레임 수신 및 CRC16 검증
   - JSON 파싱

4. 비동기 수신 루프 (Task)

@src/windows/BridgeOne/Program.cs 에서 VendorCdcManager 초기화하고
PING 명령 전송 테스트를 추가해줘.
```

**검증 방법**:
- [ ] COM 포트 자동 감지 성공
- [ ] SerialPort 열기 성공
- [ ] PING 명령 전송 → ESP32-S3 응답 수신
- [ ] JSON 파싱 및 CRC16 검증 성공

##### 1.2.4 Board ↔ Windows 통신 테스트

###### 1.2.4.1 ESP32-S3 테스트 펌웨어 작성

**구현 목표**: HID 출력 및 CDC 명령 처리를 단독으로 테스트할 수 있는 펌웨어
**예상 시간**: 40-50분

**바이브 코딩 프롬프트**:
```
@src/board/BridgeOne/src/main.cpp 에 테스트 모드를 추가해:

1. 테스트 명령 모드 추가:
   - 시리얼 모니터에서 명령 입력 받기
   - 'm': 마우스 테스트 (우측으로 50픽셀 이동)
   - 'c': 클릭 테스트 (좌클릭 down → 100ms 대기 → up)
   - 'k': 키보드 테스트 ('Hello' 입력)
   - 's': 스크롤 테스트 (휠 ±3)
   - 'p': CDC PING 명령 전송

2. 자동 테스트 시퀀스:
   - setup() 완료 5초 후 자동으로 위 테스트 순차 실행
   - 각 테스트 간 2초 대기
   - 테스트 결과를 시리얼 모니터에 출력

3. HID 전송 성공/실패 로깅

이렇게 하면 Windows 연결 후 ESP32-S3 단독으로 HID 동작을 테스트할 수 있어.
```

**검증 방법**:
- [ ] 시리얼 모니터에서 'm', 'c', 'k' 명령으로 수동 테스트 가능
- [ ] 자동 테스트 시퀀스 5초 후 실행 확인
- [ ] 각 테스트마다 로그 출력 확인
- [ ] 테스트 실패 시 명확한 오류 메시지

###### 1.2.4.2 Windows 서버 통합 테스트 및 HID 장치 인식

**구현 목표**: ESP32-S3 연결 및 HID 입력 수신 통합 검증
**예상 시간**: 30-40분 (수동 테스트)
**필요 장비**: ESP32-S3, Windows PC, USB 케이블

**테스트 프롬프트**:
```
다음 수동 테스트를 진행하고 결과를 체크해:

1. **HID 장치 인식 테스트**:
   - ESP32-S3를 Windows PC에 USB 연결
   - Windows 시작 메뉴에서 "장치 관리자" 실행
   - "휴먼 인터페이스 장치" 카테고리에서 다음 확인:
     * "HID-compliant mouse" 항목 존재
     * "HID-compliant keyboard" 항목 존재
   - "포트(COM & LPT)" 카테고리에서:
     * "USB Serial Device (COMxx)" 항목 존재 (Vendor CDC)
   - 각 장치 속성에서 드라이버 정상 로딩 확인

2. **Windows 서버 자동 감지**:
   - Windows 서버 애플리케이션 실행
   - 로그에서 ESP32-S3 HID 장치 감지 메시지 확인
   - COM 포트 자동 감지 및 연결 확인
   - Raw Input 등록 성공 확인

3. **초기 연결 안정성**:
   - USB 재연결 3회 반복
   - 매번 정상 인식되는지 확인
   - 드라이버 오류 없음 확인
```

**검증 방법**:
- [ ] 장치 관리자에서 HID 마우스/키보드 인식
- [ ] Vendor CDC(COM 포트) 인식
- [ ] Windows 서버가 자동으로 장치 감지
- [ ] USB 재연결 시 안정적 인식

###### 1.2.4.3 HID 마우스 입력 E2E 테스트

**구현 목표**: ESP32-S3 마우스 출력 → Windows 커서 이동 검증
**예상 시간**: 20-30분 (수동 테스트)

**테스트 프롬프트**:
```
ESP32-S3 테스트 펌웨어와 Windows 서버를 사용하여:

1. **마우스 이동 테스트**:
   - ESP32-S3 시리얼 모니터에서 'm' 입력
   - Windows 커서가 우측으로 50픽셀 이동하는지 육안 확인
   - Windows 서버 로그에서 deltaX=50 입력 수신 확인
   - 4방향(상하좌우) 이동 테스트 (펌웨어 명령 추가)

2. **클릭 테스트**:
   - 메모장을 열어둔 상태에서 ESP32-S3에서 'c' 입력
   - Windows에서 좌클릭 이벤트 발생 확인 (메모장에 커서 focus)
   - 우클릭, 중클릭 테스트

3. **스크롤 테스트**:
   - 웹 브라우저 긴 페이지 열기
   - ESP32-S3에서 's' 입력
   - 페이지 스크롤 확인 (위/아래)

4. **연속 입력 테스트**:
   - 1초에 10회씩 마우스 이동 명령 전송 (펌웨어 수정)
   - 10초간 연속 전송
   - Windows에서 끊김 없이 커서 이동 확인
```

**검증 방법**:
- [ ] 마우스 이동 명령 → Windows 커서 이동
- [ ] 클릭 명령 → Windows 클릭 이벤트
- [ ] 스크롤 명령 → Windows 스크롤 동작
- [ ] 연속 입력 시 끊김 없음

###### 1.2.4.4 HID 키보드 입력 E2E 테스트

**구현 목표**: ESP32-S3 키보드 출력 → Windows 키 입력 검증
**예상 시간**: 20-30분 (수동 테스트)

**테스트 프롬프트**:
```
1. **일반 키 입력 테스트**:
   - 메모장 열기
   - ESP32-S3에서 'k' 입력 → "Hello" 텍스트 입력 확인
   - 알파벳, 숫자, 특수문자 입력 테스트

2. **모디파이어 키 테스트**:
   - Ctrl+C (복사), Ctrl+V (붙여넣기) 테스트
   - Shift+A (대문자 A) 테스트
   - Alt+F4 (창 닫기) 테스트 (신중히!)
   - GUI+R (실행 대화상자) 테스트

3. **특수 키 테스트**:
   - 방향키 (↑↓←→) 테스트
   - Function 키 (F1-F12) 테스트
   - Enter, Backspace, Delete 테스트
   - Home, End, PageUp, PageDown 테스트

4. **키 반복 입력 테스트**:
   - 동일한 키를 300ms 이상 홀드
   - 자동 반복 입력 확인 (30ms 간격)
```

**검증 방법**:
- [ ] 일반 키 입력 → 메모장에 텍스트 입력
- [ ] 모디파이어 조합 정상 동작
- [ ] 특수 키 정상 동작
- [ ] 키 홀드 시 자동 반복 (300ms 후, 30ms 간격)

###### 1.2.4.5 Vendor CDC 명령 송수신 테스트

**구현 목표**: Windows ↔ Board 양방향 CDC 통신 검증
**예상 시간**: 30-40분

**바이브 코딩 프롬프트**:
```
@src/windows/BridgeOne/Program.cs 에 CDC 테스트 명령을 추가해:

1. Main() 함수에 테스트 시퀀스 추가:
   - PING 명령 전송 → 응답 대기 → 성공/실패 로그
   - GET_STATUS 명령 전송 → FPS, 큐 크기, 업타임 수신 → 로그
   - SET_CONFIG 명령 전송 (테스트 설정) → 확인 응답 → 로그

2. 타임아웃 처리 (3초):
   - 응답 없으면 "Timeout" 로그 및 재시도

3. 연속 명령 테스트:
   - 1초 간격으로 10회 PING 전송
   - 응답률 계산 및 출력

4. JSON 파싱 오류 처리:
   - 잘못된 JSON 수신 시 오류 로그
   - CRC16 불일치 시 재전송 요청

**참고**: Windows 서버 구현 전까지는 Python 스크립트(`test_command_system.py`)로 선행 검증 가능:
- ESP32-S3 단독으로 모든 명령 처리 테스트
- CRC16 검증 및 메시지 ID 매칭 확인
- 실제 펌웨어 업데이트 후 회귀 테스트용
```

**검증 방법**:
- [ ] PING 명령 → ESP32-S3 응답 수신
- [ ] GET_STATUS → FPS, 큐 크기 등 상태 정보 수신
- [ ] SET_CONFIG → 설정 변경 확인 응답
- [ ] 타임아웃 처리 정상 동작
- [ ] 10회 연속 PING 응답률 100%
- [ ] **선택사항**: `test_command_system.py`로 Windows 서버 구현 전 선행 검증

###### 1.2.4.6 1분간 연속 HID 전송 안정성 테스트

**구현 목표**: 장시간 HID 전송 안정성 및 성능 검증
**예상 시간**: 20-30분 (테스트 실행 시간 포함)

**바이브 코딩 프롬프트**:
```
@src/board/BridgeOne/src/main.cpp 에 스트레스 테스트 모드를 추가해:

1. 스트레스 테스트 모드 ('t' 명령):
   - 60초간 120Hz로 마우스 이동 명령 전송 (원형 패턴)
   - deltaX = cos(angle) * 10
   - deltaY = sin(angle) * 10
   - angle은 매 프레임마다 증가

2. 통계 수집:
   - 전송 프레임 수 카운터
   - 실제 전송 FPS 계산 (1초마다)
   - HID 전송 실패 카운터
   - 메모리 사용량 모니터링

3. 60초 후 결과 출력:
   - 총 전송 프레임 수 (목표: 7200개)
   - 평균 FPS (목표: 120Hz)
   - 실패율 (목표: 0%)
   - 메모리 누수 여부
```

**검증 방법**:
- [ ] 60초간 연속 전송 완료
- [ ] Windows 커서가 원형 패턴으로 이동 (육안 확인)
- [ ] 평균 FPS 120Hz 근접 (110-130Hz 허용)
- [ ] HID 전송 실패율 0%
- [ ] 메모리 누수 없음 (시작/종료 동일)
- [ ] Windows 서버 크래시 없음

#### 1.3 End-to-End 통신 연결 및 테스트

**단계 목표**: Android → Board → Windows 전체 경로 통신 완성
**예상 기간**: 4-5일
**검증 기준**: 터치 입력이 Windows 커서 이동으로 즉시 반영

##### 1.3.1 Android → Board → Windows 전체 파이프라인 통합

###### 1.3.1.1 통합 테스트 환경 구성 및 로깅 시스템

**구현 목표**: 3개 플랫폼 동시 실행 및 타임스탬프 기반 로깅
**예상 시간**: 40-50분

**바이브 코딩 프롬프트**:
```
각 플랫폼에 통합 로깅 시스템을 추가해:

1. @src/android/.../MainActivity.kt:
   - 터치 이벤트 발생 시 System.nanoTime() 타임스탬프 로깅
   - USB 전송 직전/직후 타임스탬프
   - "TOUCH_DOWN: ts=12345, x=100, y=200" 형식

2. @src/board/BridgeOne/src/main.cpp:
   - UART 수신 시 micros() 타임스탬프
   - HID 전송 시 타임스탬프
   - "UART_RX: ts=12345, seq=10, delta=(5,3)" 형식

3. @src/windows/BridgeOne/Program.cs:
   - HID 입력 수신 시 Stopwatch.GetTimestamp()
   - 커서 적용 후 타임스탬프
   - "HID_INPUT: ts=12345, deltaX=5, deltaY=3" 형식

4. 로그 레벨 설정:
   - DEBUG: 모든 프레임 상세 로그
   - INFO: 통계 정보 (1초마다)
   - WARN/ERROR: 오류 상황

각 플랫폼 로그에 공통 포맷을 사용해서 나중에 비교 분석하기 쉽게 해줘.
```

**검증 방법**:
- [ ] Android 로그에 터치 이벤트 타임스탬프 출력
- [ ] ESP32-S3 시리얼 모니터에 UART/HID 타임스탬프
- [ ] Windows 콘솔에 HID 입력 타임스탬프
- [ ] 공통 로그 포맷으로 출력

###### 1.3.1.2 Android 터치 → Windows 커서 E2E 첫 연결

**구현 목표**: Android 터치패드에서 Windows 커서까지 전체 경로 연결
**예상 시간**: 50분-1시간
**필요 장비**: Android 디바이스, ESP32-S3, Windows PC, USB OTG 케이블

**바이브 코딩 프롬프트**:
```
@src/android/.../MainActivity.kt 의 터치 입력을 실제 USB 전송과 연결해:

1. TouchInputProcessor와 UsbConnectionManager 연결:
   - 터치 이벤트 발생 → BridgeFrame 생성 → USB 전송
   - 1.1.3.1에서 만든 통합 UI에 실제 전송 로직 추가

2. 전송 통계 UI 업데이트:
   - 초당 전송 프레임 수 (FPS)
   - 현재 순번 카운터
   - USB 전송 성공/실패 카운터

3. 오류 처리:
   - USB 분리 시 재연결 시도
   - 전송 실패 시 재시도 (최대 3회)

이제 Android 터치 → ESP32-S3 UART → HID → Windows 커서로 전체 경로가 연결돼.
```

**수동 테스트 프롬프트**:
```
1. **전체 경로 연결 테스트**:
   - Android 디바이스를 ESP32-S3에 USB OTG로 연결
   - ESP32-S3를 Windows PC에 연결
   - Android 앱 실행, Windows 서버 실행
   - Android 터치패드를 터치하면 Windows 커서 이동 확인

2. **각 구간 로그 확인**:
   - Android: "TOUCH → USB" 로그
   - ESP32-S3: "UART → HID" 로그
   - Windows: "HID → Cursor" 로그

3. **기본 동작 검증**:
   - 상하좌우 드래그 → 커서 이동
   - 터치 해제 → 커서 정지
   - 클릭 동작 확인 (나중에 구현)
```

**검증 방법**:
  - [ ] Android 터치 → Windows 커서 이동 즉시 확인
- [ ] 3개 플랫폼 로그에 타임스탬프 출력
- [ ] 터치 방향과 커서 이동 방향 일치
- [ ] 터치 해제 시 커서 정지

###### 1.3.1.3 E2E 지연시간 측정 및 분석

**구현 목표**: 각 구간 지연시간 측정 및 50ms 이하 검증
**예상 시간**: 40-50분

**바이브 코딩 프롬프트**:
```
각 플랫폼에 지연시간 측정 로직을 추가해:

1. @src/android/.../TouchInputProcessor.kt:
   - 터치 이벤트 타임스탬프를 BridgeFrame에 포함 (확장 필드 또는 별도 매핑)
   - USB 전송 완료 후 지연시간 계산: (전송완료 - 터치발생)
   - 목표: 5-8ms

2. @src/board/BridgeOne/src/main.cpp:
   - UART 수신 타임스탬프 저장
   - HID 전송 완료 후 지연시간 계산: (HID전송 - UART수신)
   - 목표: ≤5ms
   - 1초마다 평균/최대/최소 지연시간 출력

3. @src/windows/BridgeOne/RawInputHandler.cs:
   - HID 입력 수신 타임스탬프
   - 커서 적용 후 지연시간 계산
   - 목표: 10-15ms

4. 통합 지연시간 분석 (Windows):
   - 3개 구간 지연시간 합산
   - 전체 E2E 지연시간: Android + Board + Windows
   - 목표: 총 50ms 이하
   - 1초마다 통계 출력

각 구간이 목표치를 초과하면 경고 로그를 출력해줘.
```

**검증 방법**:
- [ ] Android 구간: 5-8ms
- [ ] Board 구간: ≤5ms
- [ ] Windows 구간: 10-15ms
- [ ] 전체 E2E: 50ms 이하
- [ ] 1초마다 통계 로그 출력

###### 1.3.1.4 프레임 손실 및 순번 검증

**구현 목표**: 순번 카운터 순환 및 프레임 손실률 <0.1% 달성
**예상 시간**: 30-40분

**바이브 코딩 프롬프트**:
```
@src/windows/BridgeOne/Program.cs 에 순번 검증 로직을 추가해:

1. 순번 추적 시스템:
   - 이전 순번 저장 (lastSeq)
   - 수신 순번과 비교 (expectedSeq = (lastSeq + 1) % 256)
   - 불일치 시 손실 프레임 수 계산

2. 통계 수집:
   - 총 수신 프레임 수
   - 손실 프레임 수
   - 손실률 = (손실 / 총수신) * 100
   - 순번 순환 횟수 (255→0 전환 카운터)

3. 1초마다 통계 출력:
   - "Frames: 1200, Lost: 0, Loss Rate: 0.00%, Cycles: 4"

4. 경고 조건:
   - 손실률 >0.1%: WARN 로그
   - 손실률 >1%: ERROR 로그

ESP32-S3와 Android에도 유사한 순번 검증 추가해줘.
```

**수동 테스트 프롬프트**:
```
256회 이상 연속 터치 입력으로 순번 순환 테스트:

1. Android 터치패드를 원형으로 계속 드래그 (2분간)
2. 각 플랫폼 로그에서 순번 순환 확인:
   - "seq: 253, 254, 255, 0, 1, 2, ..."
3. 손실률 확인:
   - Windows 로그에서 "Loss Rate: <0.1%" 확인
4. 순번 불일치 경고가 없는지 확인
```

**검증 방법**:
- [ ] 순번 카운터 255→0 순환 확인
- [ ] 프레임 손실률 <0.1%
- [ ] 2분간 연속 입력 안정성
- [ ] 순번 불일치 경고 없음

##### 1.3.2 성능 최적화 및 E2E 검증

###### 1.3.2.1 Android 객체 풀링 구현

**구현 목표**: 가비지 컬렉션 최소화로 성능 향상
**예상 시간**: 40-50분

**바이브 코딩 프롬프트**:
```
@src/android/.../input/ 에 새 클래스 ObjectPool.kt를 생성해:

1. 제네릭 ObjectPool<T> 클래스:
   - 객체 생성 팩토리 함수
   - 객체 리셋 함수
   - acquire(): T - 풀에서 객체 가져오기 (없으면 생성)
   - release(obj: T) - 풀에 객체 반환
   - 최대 크기 제한

2. PointFPool 구현:
   - 최대 50개 PointF 객체
   - 리셋: x=0f, y=0f

3. BridgeFramePool 구현:
   - 최대 10개 BridgeFrame 객체
   - 리셋: 모든 필드 0

4. TouchInputProcessor 수정:
   - PointF 객체 new 대신 풀에서 acquire
   - 사용 후 release
   - BridgeFrame도 풀링 적용

5. 통계 수집:
   - 풀 히트율 (hit / (hit + miss))
   - 목표: >90%

객체 풀링으로 GC 발생을 최소화해서 60fps를 안정적으로 유지할 수 있어.
```

**검증 방법**:
- [ ] 풀 히트율 >90%
- [ ] Android Profiler에서 GC 빈도 감소 확인
- [ ] 프레임 드롭 없음
- [ ] 메모리 사용량 안정

###### 1.3.2.2 Board 프레임 큐 최적화

**구현 목표**: 원형 버퍼로 큐 오버플로우 방지
**예상 시간**: 30-40분

**바이브 코딩 프롬프트**:
```
@src/board/BridgeOne/src/main.cpp 의 frameQueue를 원형 버퍼로 개선해:

1. 원형 버퍼 구조:
   - BridgeFrame buffer[32] - 고정 크기 배열
   - head, tail 인덱스 (0-31 순환)
   - size 카운터

2. push(frame) 함수:
   - 큐가 가득 차면 가장 오래된 프레임 덮어쓰기 (오버런 경고)
   - tail 인덱스 증가 (% 32)
   - size++

3. pop() 함수:
   - 큐가 비어있으면 false 반환
   - head 인덱스에서 프레임 읽기
   - head 증가 (% 32)
   - size--

4. 통계:
   - 최대 큐 사용량 추적
   - 오버런 카운터
   - 1초마다 출력

5. FreeRTOS 큐 제거:
   - 기존 xQueueCreate 대신 원형 버퍼 사용
   - 뮤텍스로 동기화 (uartRxTask ↔ hidTxTask)

원형 버퍼는 동적 할당이 없어서 지연시간이 안정적이고 메모리 단편화가 없어.
```

**검증 방법**:
- [ ] 큐 오버플로우 없음
- [ ] 최대 큐 사용량 <80%
- [ ] Board 구간 지연시간 <5ms 유지
- [ ] 오버런 경고 없음

###### 1.3.2.3 전송 빈도 측정 및 최적화

**구현 목표**: 디바이스 성능에 따른 적응적 전송 빈도 (83-167Hz)
**예상 시간**: 40-50분

**바이브 코딩 프롬프트**:
```
@src/android/.../input/TouchInputProcessor.kt 에 전송 빈도 측정 및 조절 추가:

1. FPS 측정:
   - 마지막 전송 타임스탬프 저장
   - 현재 전송과의 시간차 계산
   - 1초 평균 FPS 계산

2. 디바이스 등급 자동 감지:
   - 첫 10초간 평균 FPS 측정
   - FPS >= 150: High (167Hz 목표)
   - FPS >= 100: Medium (120Hz 목표)
   - FPS < 100: Low (83Hz 목표)

3. 적응적 전송 간격:
   - High: 6ms (167Hz)
   - Medium: 8.3ms (120Hz)
   - Low: 12ms (83Hz)
   - 목표 간격보다 빠르면 throttle

4. 통계 UI:
   - 현재 FPS
   - 디바이스 등급 표시
   - 목표 FPS vs 실제 FPS

이렇게 하면 저사양 디바이스에서도 안정적으로 동작해.
```

**검증 방법**:
- [ ] FPS 측정 정확성 확인
- [ ] 디바이스 등급 자동 감지
- [ ] 목표 FPS 달성 (±10Hz 오차 허용)
- [ ] 저사양 디바이스에서도 안정 동작

###### 1.3.2.4 E2E 통합 테스트 및 최종 검증

**구현 목표**: 전체 시스템 통합 안정성 및 성능 검증
**예상 시간**: 1시간-1시간 30분 (수동 테스트)

**수동 테스트 프롬프트**:
```
최종 E2E 통합 테스트를 진행해:

1. **기본 동작 테스트 (15분)**:
   - Android 터치 → Windows 커서 즉시 반응 확인
   - 8방향 드래그 (상하좌우 + 대각선)
   - 빠른 드래그, 느린 드래그 모두 테스트
   - 터치 해제 시 커서 정지
   - 좌표 정확성 (화면 중앙 터치 → 커서 중앙 위치)

2. **좌표 정확성 테스트 (10분)**:
   - 윈도우 그림판 열기
   - Android에서 정사각형 그리기 시도
   - 직선, 원 그리기 시도
   - 좌표 왜곡 없는지 확인

3. **연속 조작 안정성 (30분)**:
   - 30분간 계속 터치 입력 (게임, 웹 브라우징 등)
   - FPS 안정적 유지 확인
   - 끊김, 지연 없음
   - 메모리 누수 없음 (Android Studio Profiler)

4. **성능 지표 확인 (15분)**:
   - Android FPS: 83-167Hz (디바이스 등급에 따라)
   - E2E 지연시간: <50ms
   - 프레임 손실률: <0.1%
   - CPU 사용률: <30%
   - 메모리: 안정 (증가 추세 없음)

5. **스트레스 테스트 (20분)**:
   - 빠른 연속 터치 (1초에 10회 이상)
   - 10분간 지속
   - 시스템 크래시 없음
   - 성능 저하 없음

각 테스트 결과를 체크리스트에 기록해줘.
```

**검증 방법**:
- [ ] Android 터치 → Windows 커서 즉시 반응
- [ ] 좌표 정확성 (정사각형, 직선 정상 그리기)
- [ ] 30분 연속 조작 안정성
- [ ] E2E 지연시간 <50ms
- [ ] FPS 83-167Hz 달성 (디바이스 등급별)
- [ ] 프레임 손실률 <0.1%
- [ ] 메모리 누수 없음
- [ ] 스트레스 테스트 통과

#### 1.4 고급 통신 기능 및 오류 처리 구현

**단계 목표**: Keep-alive, 모드 전환, 오류 복구 등 고급 기능 완성
**예상 기간**: 5-6일
**검증 기준**: 4시간 안정성, Essential/Standard 모드 전환

##### 1.4.1 Keep-alive 및 재연결 메커니즘

###### 1.4.1.1 Android Keep-alive 시스템 구현

**구현 목표**: 500ms 주기 Keep-alive로 연결 유지 확인
**예상 시간**: 40-50분

**바이브 코딩 프롬프트**:
```
@src/android/.../usb/UsbConnectionManager.kt 에 Keep-alive 시스템을 추가해:

1. Keep-alive 프레임 정의:
   - BridgeFrame의 특수 플래그 (예: buttons = 0xFF)
   - 또는 별도 Keep-alive 프레임 구조

2. 주기적 전송 (Coroutine):
   - 500ms 간격으로 Keep-alive 프레임 전송
   - delay(500) 사용
   - lifecycleScope에서 실행

3. 응답 타임아웃 처리:
   - 마지막 응답 타임스탬프 추적
   - 3초 이내 응답 없으면 타임아웃
   - 연속 3회 타임아웃 시 재연결 시도

4. 지수 백오프 재연결:
   - 1차 시도: 1초 대기
   - 2차 시도: 2초 대기
   - 3차 시도: 4초 대기
   - 4차 시도: 8초 대기
   - 이후 8초 유지

5. 상태 UI 표시:
   - "연결됨 (응답: 123ms)"
   - "재연결 중... (시도 2/4)"
   - "연결 끊김"

Keep-alive로 USB 케이블이 빠졌거나 Board가 멈췄을 때 빠르게 감지할 수 있어.
```

**검증 방법**:
- [ ] 500ms 간격으로 Keep-alive 전송
- [ ] ESP32-S3에서 Keep-alive 수신 확인 (로그)
- [ ] USB 분리 시 3초 이내 감지
- [ ] 재연결 지수 백오프 동작 확인

###### 1.4.1.2 Windows Keep-alive 응답 구현

**구현 목표**: Vendor CDC로 Keep-alive 응답 및 연결 품질 추적
**예상 시간**: 30-40분

**바이브 코딩 프롬프트**:
```
@src/windows/BridgeOne/VendorCdcManager.cs 에 Keep-alive 응답 시스템을 추가해:

1. Keep-alive 명령 처리:
   - "PING" 명령 수신
   - 즉시 "PONG" 응답 전송
   - 응답 시간 측정

2. Ping-Pong 통계:
   - 수신 PING 카운터
   - 평균 응답 시간
   - 최대/최소 응답 시간

3. 연결 품질 지표:
   - 응답률 = (응답 / 수신) * 100
   - 평균 지연시간
   - 품질 등급 (Excellent/Good/Fair/Poor)

4. 0.5초 주기 상태 브로드캐스트:
   - Android로 연결 품질 정보 전송
   - FPS, 큐 크기, 업타임 포함

5. 타임아웃 처리:
   - 5초 이상 PING 없으면 "No Keepalive" 경고

Windows 측에서도 연결 상태를 능동적으로 관리해줘.
```

**검증 방법**:
- [ ] PING 수신 → PONG 응답 (로그 확인)
- [ ] 평균 응답 시간 <10ms
- [ ] 연결 품질 지표 정확히 계산
- [ ] 0.5초마다 상태 브로드캐스트

###### 1.4.1.3 USB 분리/재연결 자동 처리

**구현 목표**: USB 분리 감지 및 Cancel and Restart 패턴
**예상 시간**: 40-50분

**바이브 코딩 프롬프트**:
```
@src/android/.../usb/UsbConnectionManager.kt 에 USB 분리 처리를 추가해:

1. USB 분리 감지:
   - UsbManager.ACTION_USB_DEVICE_DETACHED BroadcastReceiver
   - USB 읽기/쓰기 IOException 감지
   - Keep-alive 타임아웃

2. Cancel and Restart 패턴:
   - 현재 작업 모두 취소 (coroutine cancel)
   - USB 포트 닫기
   - 리소스 정리
   - 상태를 NoTransport로 전환

3. 자동 재연결 로직:
   - 1초마다 ESP32-S3 DevKitC-1 UART 포트 스캔
   - 장치 발견 시 자동 권한 요청
   - 권한 승인 시 연결 시도
   - 최대 10회 재시도

4. 순번 카운터 리셋:
   - 재연결 성공 시 순번 카운터 0으로 초기화
   - ESP32-S3에도 리셋 명령 전송

5. 사용자 알림:
   - "USB 연결이 끊어졌습니다" 토스트
   - "재연결 중..." 진행 표시
   - "연결됨" 성공 메시지

USB 케이블을 빼도 다시 꽂으면 자동으로 연결돼.
```

**검증 방법**:
- [ ] USB 분리 즉시 감지 (1초 이내)
- [ ] 리소스 정리 완료
- [ ] USB 재연결 시 자동 연결 (3초 이내)
- [ ] 순번 카운터 0으로 리셋
- [ ] 사용자 알림 표시

##### 1.4.2 Essential/Standard 모드 전환

###### 1.4.2.1 상태 관리 시스템 구현

**구현 목표**: 앱 상태 머신 및 StateFlow 기반 상태 관리
**예상 시간**: 40-50분

**바이브 코딩 프롬프트**:
```
@src/android/.../state/ 에 새 패키지를 만들고 상태 관리 시스템을 구현해:

1. TransportState enum:
   enum class TransportState {
       NoTransport,      // USB 연결 없음
       UsbOpening,       // USB 연결 시도 중
       UsbReady,         // USB 연결 완료
       UsbError          // USB 오류
   }

2. AppState enum:
   enum class AppState {
       WaitingForConnection,  // 초기 상태, 연결 대기
       Essential,             // 기본 모드 (HID만)
       Standard               // 고급 모드 (CDC 연동)
   }

3. StateManager 클래스:
   - transportState: StateFlow<TransportState>
   - appState: StateFlow<AppState>
   - updateTransportState(newState) 메서드
   - updateAppState(newState) 메서드
   - 상태 전환 로그 출력

4. 상태 전환 규칙:
   - NoTransport → UsbOpening → UsbReady
   - UsbReady & HID만 → Essential
   - UsbReady & CDC 핸드셰이크 완료 → Standard

5. MainActivity에서 상태 구독:
   - stateManager.appState.collect { state ->
       when (state) {
           Essential -> showEssentialUI()
           Standard -> showStandardUI()
       }
     }

StateFlow로 상태를 관리하면 UI가 자동으로 상태 변화에 반응해.
```

**검증 방법**:
- [ ] TransportState 전환 로그 출력
- [ ] AppState 전환 로그 출력
- [ ] UI가 상태 변화에 자동 반응
- [ ] 잘못된 상태 전환 방지

###### 1.4.2.2 핸드셰이크 프로토콜 구현

**구현 목표**: Windows ↔ Android CDC 핸드셰이크
**예상 시간**: 50분-1시간
**참조**: `@docs/technical-specification.md` §3.2

**바이브 코딩 프롬프트**:
```
Android와 Windows에 핸드셰이크 프로토콜을 구현해:

**Android 측 (@src/android/.../usb/HandshakeManager.kt)**:

1. Phase 1: Authentication
   - Windows로 "HELLO" 명령 전송 (앱 버전, 디바이스 정보)
   - Windows에서 "CHALLENGE" 수신 (랜덤 값)
   - Challenge 응답 계산 (간단한 해시)
   - "RESPONSE" 전송

2. Phase 2: State Sync
   - Windows에서 "AUTH_OK" 수신
   - "GET_CONFIG" 요청 전송
   - Windows 설정 수신 및 저장
   - "READY" 전송

3. 핸드셰이크 완료:
   - AppState를 Standard로 전환
   - "핸드셰이크 완료" 로그

**Windows 측 (@src/windows/BridgeOne/HandshakeManager.cs)**:

1. Phase 1:
   - Android "HELLO" 수신
   - Challenge 생성 및 전송
   - Response 검증
   - 성공 시 "AUTH_OK" 전송

2. Phase 2:
   - "GET_CONFIG" 수신
   - 현재 설정 전송
   - "READY" 수신 확인

3. 타임아웃:
   - 각 Phase 10초 타임아웃
   - 실패 시 Essential 모드 유지

TLS는 일단 Skip하고 간단한 Challenge-Response만 구현해. 나중에 보안 강화할 수 있어.
```

**검증 방법**:
- [ ] "HELLO" → "CHALLENGE" → "RESPONSE" 순서 확인
- [ ] Challenge 검증 성공
- [ ] 설정 동기화 완료
- [ ] Standard 모드로 전환
- [ ] 타임아웃 처리 동작

###### 1.4.2.3 모드별 UI 전환 구현

**구현 목표**: Essential/Standard 모드에 따른 UI 동적 전환
**예상 시간**: 50분-1시간

**바이브 코딩 프롬프트**:
```
@src/android/.../MainActivity.kt 에 모드별 UI 전환을 구현해:

1. Essential 모드 UI:
   - 1개 화면만 표시
   - Boot Keyboard (상단 50%)
   - 간소화 터치패드 (하단 50%, 좌클릭만)
   - 페이지 인디케이터 숨김
   - "Windows 서버 연결 대기 중..." 배너

2. Standard 모드 UI:
   - 4개 페이지 HorizontalPager
   - 전체 기능 활성화
   - 페이지 인디케이터 표시
   - "연결됨" 상태 배너

3. 모드 전환 애니메이션:
   - Essential → Standard: 페이드 인 + 슬라이드 (500ms)
   - Compose AnimatedContent 사용
   - 전환 중 터치 입력 비활성화

4. 상태 지속성:
   - SharedPreferences에 마지막 모드 저장
   - 앱 재시작 시 복원 (단, 핸드셰이크 재수행)

5. 모드 강제 전환 (디버그):
   - 설정 화면에 "Essential 모드로 전환" 버튼
   - 개발/테스트용

모드 전환이 부드럽게 보이도록 애니메이션을 신경 써서 구현해줘.
```

**검증 방법**:
- [ ] Essential 모드 UI 정상 표시
- [ ] Standard 모드 UI 정상 표시
- [ ] 모드 전환 애니메이션 부드러움
- [ ] 상태 지속성 (앱 재시작 후 복원)
- [ ] 강제 전환 버튼 동작

##### 1.4.3 오류 처리 및 복구

###### 1.4.3.1 USB 연결 오류 처리 및 진단

**구현 목표**: 다양한 USB 오류 상황 처리 및 사용자 가이드
**예상 시간**: 40-50분

**바이브 코딩 프롬프트**:
```
@src/android/.../usb/ErrorHandler.kt 를 생성하고 오류 처리를 구현해:

1. 오류 타입 enum:
   enum class UsbError {
       DeviceNotFound,        // ESP32-S3 DevKitC-1 UART 포트 없음
       PermissionDenied,      // 사용자가 권한 거부
       DriverNotCompatible,   // 드라이버 호환 문제
       ConnectionTimeout,     // 연결 타임아웃
       IOException,           // 읽기/쓰기 오류
       UnknownError          // 기타 오류
   }

2. 오류 진단 로직:
   - detectError(exception): UsbError
   - 예외 타입 및 메시지로 오류 분류
   - 로그에 상세 정보 출력

3. 사용자 친화적 메시지:
   - DeviceNotFound: "ESP32-S3 보드를 연결해주세요"
   - PermissionDenied: "USB 권한을 허용해주세요"
   - DriverNotCompatible: "USB 드라이버를 확인해주세요"
   - 각 오류별 해결 방법 가이드

4. 자동 복구 전략:
   - DeviceNotFound: 5초마다 재스캔
   - PermissionDenied: 권한 재요청 (3회까지)
   - IOException: Cancel and Restart
   - 기타: 사용자에게 재연결 안내

5. 오류 다이얼로그:
   - 오류 제목, 메시지, 해결 방법
   - "다시 시도" / "취소" 버튼
   - 오류 로그 복사 기능 (디버깅용)

사용자가 오류 상황을 쉽게 이해하고 대응할 수 있도록 해줘.
```

**검증 방법**:
- [ ] 각 오류 타입별 정확한 감지
- [ ] 사용자 친화적 메시지 표시
- [ ] 자동 복구 동작 확인
- [ ] 오류 다이얼로그 UI 정상 표시

###### 1.4.3.2 통신 프로토콜 오류 처리

**구현 목표**: 프레임 손실, JSON 오류 등 프로토콜 레벨 오류 처리
**예상 시간**: 40-50분

**바이브 코딩 프롬프트**:
```
각 플랫폼에 프로토콜 오류 처리를 추가해:

**Android 측**:

1. 프레임 손실 감지:
   - 순번 불일치 시 손실 프레임 수 계산
   - 손실률 >1%: 경고 토스트
   - 손실률 >5%: USB 재연결 제안

2. 재전송 요청 (선택적):
   - 중요 프레임(클릭)은 ACK 대기
   - 타임아웃 시 재전송 (최대 3회)

3. 프로토콜 버전 체크:
   - 핸드셰이크 시 버전 정보 교환
   - 불일치 시 경고 또는 호환 모드

**ESP32-S3 측**:

1. UART 오류 처리:
   - 프레이밍 에러 감지
   - 버퍼 오버런 감지
   - 오류 카운터 및 로그

2. HID 전송 실패 처리:
   - 전송 실패 시 재시도 (3회)
   - 연속 10회 실패 시 USB 리셋

**Windows 측**:

1. JSON 파싱 오류:
   - try-catch로 안전하게 처리
   - 잘못된 JSON 로그 출력
   - 다음 프레임 계속 처리

2. CRC16 검증 실패:
   - 해당 프레임 폐기
   - 재전송 요청 전송
   - 실패율 통계

3. 타임아웃 처리:
   - 명령 응답 3초 타임아웃
   - 자동 재시도 또는 실패 처리

모든 오류는 로그로 남겨서 나중에 분석할 수 있게 해줘.
```

**검증 방법**:
- [ ] 프레임 손실 감지 및 경고
- [ ] JSON 파싱 오류 안전 처리
- [ ] CRC16 실패 시 재전송
- [ ] 타임아웃 자동 복구
- [ ] 프로토콜 버전 체크

##### 1.4.4 실용적 안정성 테스트 (4시간 수동)

###### 1.4.4.1 기본 안정성 테스트 (2시간)

**구현 목표**: 장시간 동작 안정성 및 재연결 검증
**예상 시간**: 2시간 (실제 테스트 시간)

**수동 테스트 프롬프트**:
```
2시간 기본 안정성 테스트:

**1단계: 연속 1시간 동작 테스트 (60분)**
- Android 앱 실행 유지
- 주기적으로 터치 입력 (5분마다 1분씩)
- Windows에서 다양한 작업 수행
- 체크 항목:
  * 앱 크래시 없음
  * USB 연결 유지
  * 응답 속도 저하 없음
  * 메모리 사용량 안정 (Android Studio Profiler)

**2단계: Keep-alive 검증 (15분)**
- 로그에서 Keep-alive PING/PONG 확인
- 500ms 주기 정확성 확인
- 응답 시간 <10ms
- 5분간 터치 입력 없이 대기 → 연결 유지 확인

**3단계: USB 분리/재연결 테스트 (30분)**
- USB 케이블 분리 (10회):
  * 5초 대기 후 재연결
  * 자동 재연결 확인
  * 순번 카운터 리셋 확인
- 즉시 재연결 (5회):
  * 케이블 뽑자마자 재연결
  * 안정적 연결 확인
- ESP32-S3 재부팅 (3회):
  * Board 전원 재시작
  * Android 자동 재연결 확인

**4단계: 메모리 안정성 (15분)**
- Android Studio Profiler로 모니터링
- 메모리 증가 추세 없음 확인
- GC 빈도 정상 범위
- 힙 덤프 분석 (선택적)
```

**검증 방법**:
- [ ] 1시간 연속 동작 안정
- [ ] Keep-alive 정상 동작
- [ ] USB 분리/재연결 10회 성공
- [ ] 메모리 누수 없음

###### 1.4.4.2 프로토콜 무결성 테스트 (1시간)

**수동 테스트 프롬프트**:
```
프로토콜 무결성 1시간 테스트:

**1단계: 빠른 연속 터치 (20분)**
- 1초에 10회 이상 빠른 터치
- 다양한 패턴 (탭, 드래그, 스와이프)
- 프레임 손실률 <0.1% 확인

**2단계: 순번 카운터 순환 (20분)**
- 연속 터치로 256회 이상 순환
- 0→255→0 전환 정상 확인
- 순번 불일치 경고 없음

**3단계: 멀티터치 (선택적) (10분)**
- 두 손가락 터치 시도
- 시스템 반응 확인
- 오류 없이 처리

**4단계: CRC16 무결성 (10분)**
- Windows 로그에서 CRC16 검증 확인
- 검증 실패 없음
- 잘못된 프레임 폐기 확인
```

**검증 방법**:
- [ ] 빠른 연속 터치 안정
- [ ] 순번 순환 정상
- [ ] CRC16 검증 통과

###### 1.4.4.3 실제 사용 시나리오 테스트 (1시간)

**수동 테스트 프롬프트**:
```
실제 사용 시나리오 1시간 테스트:

**1단계: PC 제어 (30분)**
- 웹 브라우징 (스크롤, 클릭, 텍스트 입력)
- 문서 편집 (메모장, MS Word)
- 그림판 그리기
- 체크: 정확성, 반응성, 편의성

**2단계: Essential/Standard 모드 전환 (15분)**
- Windows 서버 종료 → Essential 모드 확인
- Windows 서버 시작 → Standard 모드 전환
- UI 전환 부드러움 확인
- 기능 차이 확인

**3단계: 다양한 제스처 (15분)**
- 8방향 드래그
- 긴 직선 그리기
- 원형 그리기
- 빠른 스와이프
- 느린 이동
```

**검증 방법**:
- [ ] 30분 실제 사용 가능
- [ ] 모드 전환 정상 동작
- [ ] 다양한 제스처 정확

**Phase 1 완료 조건**:
- ✅ Android ↔ Board ↔ Windows E2E 통신 완성
- ✅ Essential/Standard 모드 전환 동작  
- ✅ 전체 지연시간 50ms 이하
- ✅ 4시간 안정성 테스트 통과 (기본 2h + 프로토콜 1h + 실사용 1h)

---

### Phase 2: 핵심 입력 시스템 완성

**목표**: 터치패드 기반 정밀 제어 및 UI 컴포넌트 시스템 완전 구현
**예상 기간**: 4-5주  
**완료 기준**: 모든 터치패드 모드 동작, 60fps 유지, 컴포넌트 시스템 완성

#### 2.1 터치패드 입력 엔진 구현

**참조 문서**:
- `@docs/Android/component-touchpad.md` §1-3 (터치패드 구조, 알고리즘)
- `@docs/Android/technical-specification-app.md` §2.2 (터치패드 알고리즘)
- `@docs/Android/design-guide-app.md` §4.2 (터치 영역 최적화)

##### 2.1.1 터치패드 컴포넌트 UI 구조

###### 2.1.1.1 TouchpadWrapper 기본 구조

**구현 목표**: 1:2 비율 터치패드 컨테이너 Composable
**예상 시간**: 40-50분

**바이브 코딩 프롬프트**:
```
@src/android/.../components/touchpad/ 에 새 패키지를 만들고 TouchpadWrapper.kt를 생성해:

1. TouchpadWrapper Composable:
   @Composable
   fun TouchpadWrapper(
       modifier: Modifier = Modifier,
       onTouch: (MotionEvent) -> Unit,
       content: @Composable () -> Unit
   )

2. 1:2 비율 유지:
   - aspectRatio(1f / 2f) modifier
   - BoxWithConstraints로 부모 크기 기반 계산
   - 최대 너비 제한 (화면의 90%)

3. 둥근 모서리:
   - RoundedCornerShape(3.dp.toPx() / width * 100)
   - clip() modifier 적용

4. 테두리 및 배경:
   - 기본 배경: Color.DarkGray.copy(alpha = 0.1f)
   - 테두리: 2dp, 모드별 색상 (나중에 추가)

5. 반응형 크기 조정:
   - 세로/가로 모드 자동 대응
   - 화면 크기에 따라 적절한 크기

@docs/Android/component-touchpad.md §1.1을 참조해서 정확한 비율과 스타일을 적용해줘.
```

**검증 방법**:
- [ ] 1:2 비율 정확히 유지
- [ ] 둥근 모서리 3% 적용
- [ ] 세로/가로 모드 대응
- [ ] 화면 크기별 반응형 동작

###### 2.1.1.2 TouchpadAreaWrapper 터치 감지 영역

**구현 목표**: 터치 이벤트 감지 및 영역 밖 추적 지속
**예상 시간**: 40-50분

**바이브 코딩 프롬프트**:
```
@src/android/.../components/touchpad/TouchpadAreaWrapper.kt 를 생성해:

1. TouchpadAreaWrapper Composable:
   @Composable
   fun TouchpadAreaWrapper(
       modifier: Modifier = Modifier,
       onTouchStart: (Offset) -> Unit,
       onTouchMove: (Offset, Offset) -> Unit,  // current, delta
       onTouchEnd: (Offset) -> Unit
   )

2. pointerInput 터치 이벤트 감지:
   modifier.pointerInput(Unit) {
       detectDragGestures(
           onDragStart = { offset -> onTouchStart(offset) },
           onDrag = { change, dragAmount -> 
               onTouchMove(change.position, dragAmount)
           },
           onDragEnd = { onTouchEnd(lastPosition) }
       )
   }

3. 영역 밖 이동 추적 지속:
   - awaitPointerEventScope 사용
   - PointerEventPass.Main으로 이벤트 우선 처리
   - 터치가 영역 밖으로 나가도 계속 추적
   - 손가락 떼기 전까지 추적 유지

4. 터치 좌표 정규화:
   - 터치패드 크기 기준 상대 좌표 (0.0 ~ 1.0)
   - DPI 독립적 처리

5. 멀티터치 처리:
   - 첫 번째 포인터만 추적 (나머지 무시)
   - pointerId 저장 및 검증

@docs/Android/component-touchpad.md §1.2를 참조해서 터치 추적 연속성을 정확히 구현해줘.
```

**검증 방법**:
- [ ] 터치 시작/이동/종료 이벤트 정확히 발생
- [ ] 영역 밖으로 나가도 추적 지속
- [ ] 좌표 정규화 정확 (0.0-1.0 범위)
- [ ] 멀티터치 시 첫 포인터만 추적

###### 2.1.1.3 ControlButtonContainer 오버레이

**구현 목표**: 터치패드 상단 제어 버튼 오버레이
**예상 시간**: 30-40분

**바이브 코딩 프롬프트**:
```
@src/android/.../components/touchpad/ControlButtonContainer.kt 를 생성해:

1. ControlButtonContainer Composable:
   @Composable
   fun ControlButtonContainer(
       modifier: Modifier = Modifier,
       isVisible: Boolean,
       buttons: List<ControlButton>,
       onButtonClick: (ButtonType) -> Unit
   )

2. 레이아웃 구조:
   - Box로 터치패드에 오버레이
   - Alignment.TopCenter 정렬
   - 높이: 터치패드 높이의 15%
   - 배경: Color.Black.copy(alpha = 0.3f) 반투명

3. 버튼 배치:
   - Row로 수평 배치
   - Arrangement.SpaceEvenly 균등 간격
   - 각 버튼: 아이콘 + 레이블 (선택적)

4. 버튼 타입 enum:
   enum class ButtonType {
       LEFT_CLICK,
       RIGHT_CLICK,
       MOVE_MODE_TOGGLE,
       SCROLL_MODE_TOGGLE,
       CURSOR_MODE_TOGGLE
   }

5. 가시성 애니메이션:
   - AnimatedVisibility로 부드러운 전환
   - slideInVertically + fadeIn (300ms)
   - 모드 변경 시 자동 숨김/표시

6. 터치 이벤트 통과 방지:
   - clickable { } 또는 pointerInput { }로 이벤트 차단
   - 버튼 영역만 클릭 가능, 나머지는 터치패드로 전달

@docs/Android/component-touchpad.md §1.3을 참조해서 제어 버튼 레이아웃을 구현해줘.
```

**검증 방법**:
- [ ] 높이 15% 정확히 유지
- [ ] 반투명 배경 표시
- [ ] 버튼 클릭 정상 동작
- [ ] 가시성 애니메이션 부드러움
- [ ] 버튼 영역 외 터치패드로 이벤트 전달

##### 2.1.2 제스처 알고리즘 구현

###### 2.1.2.1 데드존 보상 알고리즘

**구현 목표**: 작은 터치 떨림 보정 및 의도적 이동 감지
**예상 시간**: 50분-1시간
**참조**: `@docs/Android/component-touchpad.md` §2.1

**바이브 코딩 프롬프트**:
```
@src/android/.../components/touchpad/gestures/DeadzoneCompensation.kt 를 생성해:

1. 상태 머신 enum:
   enum class DeadzoneState {
       IDLE,          // 터치 없음
       ACCUMULATING,  // 임계값 이하 이동 누적 중
       MOVING         // 임계값 초과, 실제 이동 중
   }

2. DeadzoneCompensator 클래스:
   class DeadzoneCompensator(
       private val threshold: Dp = 15.dp  // 임계값
   ) {
       private var state = DeadzoneState.IDLE
       private var accumulatedOffset = Offset.Zero
       private var startPosition = Offset.Zero
   }

3. 이동 처리 로직:
   fun process(currentPosition: Offset, delta: Offset): Offset? {
       when (state) {
           IDLE -> {
               startPosition = currentPosition
               state = ACCUMULATING
               return null
           }
           ACCUMULATING -> {
               accumulatedOffset += delta
               if (accumulatedOffset.getDistance() > threshold.toPx()) {
                   state = MOVING
                   return accumulatedOffset  // 누적된 이동량 한 번에 반환
               }
               return null  // 임계값 미만, 이동 없음
           }
           MOVING -> {
               return delta  // 실제 이동량 반환
           }
       }
   }

4. 리셋 로직:
   fun reset() {
       state = IDLE
       accumulatedOffset = Offset.Zero
   }

5. TouchpadAreaWrapper에 통합:
   - onTouchStart: reset()
   - onTouchMove: process() 결과로 deltaX/Y 계산
   - onTouchEnd: reset()

데드존 보상으로 손 떨림이 커서 이동으로 감지되지 않고, 의도적 이동만 전달돼.
```

**검증 방법**:
- [ ] 15dp 미만 이동 시 커서 정지
- [ ] 15dp 초과 시 누적 이동량 한 번에 적용
- [ ] 이후 이동은 실시간 전달
- [ ] 터치 해제 시 상태 리셋

###### 2.1.2.2 직각 이동 알고리즘

**구현 목표**: X 또는 Y 축 고정 이동 모드
**예상 시간**: 50분-1시간
**참조**: `@docs/Android/component-touchpad.md` §2.2

**바이브 코딩 프롬프트**:
```
@src/android/.../components/touchpad/gestures/RightAngleMovement.kt 를 생성해:

1. RightAngleMovement 클래스:
   class RightAngleMovement(
       private val activationDistance: Dp = 30.dp,  // 축 결정 거리
       private val angleThreshold: Float = 22.5f    // ±22.5° 허용
   ) {
       private var lockedAxis: Axis? = null
       private var totalMovement = Offset.Zero
   }

2. Axis enum:
   enum class Axis { X, Y }

3. 축 결정 로직:
   fun process(delta: Offset): Offset {
       totalMovement += delta
       
       // 축이 아직 결정되지 않았으면
       if (lockedAxis == null && totalMovement.getDistance() > activationDistance.toPx()) {
           val angle = atan2(abs(totalMovement.y), abs(totalMovement.x)).toDegrees()
           
           lockedAxis = when {
               angle < angleThreshold -> Axis.X           // 거의 수평
               angle > (90 - angleThreshold) -> Axis.Y    // 거의 수직
               else -> null  // 대각선, 축 고정 안 함
           }
       }
       
       // 축이 결정되었으면 해당 축만 이동
       return when (lockedAxis) {
           Axis.X -> Offset(delta.x, 0f)
           Axis.Y -> Offset(0f, delta.y)
           null -> delta  // 자유 이동
       }
   }

4. 리셋 로직:
   fun reset() {
       lockedAxis = null
       totalMovement = Offset.Zero
   }

5. MoveMode 통합:
   - FREE 모드: 직각 이동 비활성화
   - RIGHT_ANGLE 모드: 직각 이동 활성화
   - 모드 전환 시 reset()

직각 이동으로 정확한 수평/수직선 그리기가 쉬워져.
```

**검증 방법**:
- [ ] 30dp 수평 이동 → X축 고정
- [ ] 30dp 수직 이동 → Y축 고정
- [ ] ±22.5° 범위 내 이동 → 축 고정
- [ ] 대각선 이동 → 자유 이동 유지

###### 2.1.2.3 자유 이동 모드

**구현 목표**: DPI 독립적 커서 이동 계산
**예상 시간**: 40-50분

**바이브 코딩 프롬프트**:
```
@src/android/.../components/touchpad/gestures/FreeMovement.kt 를 생성해:

1. FreeMovement 클래스:
   class FreeMovement(
       private val density: Density,
       private val sensitivity: Float = 1.0f  // 감도 배율
   ) {
       fun calculateDelta(touchDelta: Offset): Pair<Byte, Byte> {
           // DPI 독립적 정규화
           val normalizedX = touchDelta.x / density.density * sensitivity
           val normalizedY = touchDelta.y / density.density * sensitivity
           
           // -127~127 범위로 클램핑
           val deltaX = normalizedX.coerceIn(-127f, 127f).toInt().toByte()
           val deltaY = normalizedY.coerceIn(-127f, 127f).toInt().toByte()
           
           return Pair(deltaX, deltaY)
       }
   }

2. 좌표 변환 매트릭스 (선택적):
   // 화면 방향에 따른 좌표 변환
   fun applyOrientation(delta: Offset, orientation: Int): Offset {
       return when (orientation) {
           Surface.ROTATION_0 -> delta
           Surface.ROTATION_90 -> Offset(-delta.y, delta.x)
           Surface.ROTATION_180 -> Offset(-delta.x, -delta.y)
           Surface.ROTATION_270 -> Offset(delta.y, -delta.x)
           else -> delta
       }
   }

3. 감도 조절 인터페이스:
   fun setSensitivity(level: Float) {
       sensitivity = level.coerceIn(0.1f, 5.0f)
   }

4. TouchInputProcessor 통합:
   - 터치 델타 → FreeMovement.calculateDelta()
   - 결과를 BridgeFrame.deltaX/deltaY에 설정

DPI 독립적 처리로 다양한 화면 해상도에서 일관된 커서 속도를 유지해.
```

**검증 방법**:
- [ ] 동일한 터치 이동 → 일관된 커서 이동
- [ ] deltaX/Y 범위 -127~127 유지
- [ ] 감도 조절 동작 확인
- [ ] 다양한 DPI 디바이스에서 일관성

###### 2.1.2.4 클릭 판정 로직

**구현 목표**: 탭과 드래그 구분
**예상 시간**: 30-40분

**바이브 코딩 프롬프트**:
```
@src/android/.../components/touchpad/gestures/ClickDetection.kt 를 생성해:

1. ClickDetector 클래스:
   class ClickDetector(
       private val timeThreshold: Long = 500L,  // 500ms
       private val distanceThreshold: Dp = 15.dp
   ) {
       private var touchStartTime = 0L
       private var touchStartPosition = Offset.Zero
       private var totalMovement = 0f
   }

2. 터치 시작 추적:
   fun onTouchStart(position: Offset, time: Long) {
       touchStartTime = time
       touchStartPosition = position
       totalMovement = 0f
   }

3. 이동 거리 누적:
   fun onTouchMove(currentPosition: Offset) {
       totalMovement += (currentPosition - touchStartPosition).getDistance()
   }

4. 클릭 판정:
   fun onTouchEnd(currentTime: Long): Boolean {
       val duration = currentTime - touchStartTime
       val isClick = duration < timeThreshold && 
                     totalMovement < distanceThreshold.toPx()
       return isClick
   }

5. TouchpadAreaWrapper 통합:
   - onTouchStart: clickDetector.onTouchStart()
   - onTouchMove: clickDetector.onTouchMove()
   - onTouchEnd: if (clickDetector.onTouchEnd()) { sendClick() }

6. 클릭 타입 처리:
   - ClickMode.LEFT → buttons = 0x01
   - ClickMode.RIGHT → buttons = 0x02
   - 햅틱 피드백 (HapticHelper.performHaptic(HapticCategory.MEDIUM))

클릭 판정으로 의도적 클릭과 드래그를 정확히 구분해.
```

**검증 방법**:
- [ ] 500ms 이내, 15dp 미만 → 클릭
- [ ] 500ms 초과 또는 15dp 초과 → 드래그
- [ ] 클릭 시 햅틱 피드백
- [ ] ClickMode에 따라 좌/우클릭 구분

##### 2.1.3 스크롤 시스템 구현

###### 2.1.3.1 일반 스크롤 모드

**구현 목표**: 50dp 단위 햅틱 동기화 스크롤
**예상 시간**: 40-50분
**참조**: `@docs/Android/component-touchpad.md` §2.3.1

**바이브 코딩 프롬프트**:
```
@src/android/.../components/touchpad/gestures/NormalScroll.kt 를 생성해:

1. NormalScroll 클래스:
   class NormalScroll(
       private val scrollUnit: Dp = 50.dp,
       private val hapticHelper: HapticHelper
   ) {
       private var accumulatedDistance = 0f
       private var lastHapticDistance = 0f
   }

2. 스크롤 처리 로직:
   fun process(delta: Offset): Byte {
       // 수직 방향만 사용 (deltaY)
       accumulatedDistance += delta.y
       
       // 50dp 단위로 스크롤
       val scrollAmount = (accumulatedDistance / scrollUnit.toPx()).toInt()
       
       if (scrollAmount != 0) {
           // 스크롤 발생 시 누적 거리 리셋
           accumulatedDistance %= scrollUnit.toPx()
           
           // 햅틱 피드백 (가벼운 틱)
           hapticHelper.performHaptic(HapticCategory.LIGHT)
           lastHapticDistance = 0f
       }
       
       // wheel 필드: ±1
       return scrollAmount.coerceIn(-1, 1).toByte()
   }

3. 햅틱 피드백 동기화:
   - 50dp 이동마다 정확히 1회 피드백
   - HapticCategory.LIGHT (5ms 가벼운 진동)

4. 리셋 로직:
   fun reset() {
       accumulatedDistance = 0f
   }

5. ScrollMode.NORMAL 활성화 시:
   - 터치 이동 → NormalScroll.process()
   - wheel 값을 BridgeFrame에 설정
   - deltaX/Y는 0으로 설정 (스크롤 중 커서 이동 없음)

햅틱 피드백으로 스크롤 단위를 촉각적으로 느낄 수 있어.
```

**검증 방법**:
- [ ] 50dp 이동마다 스크롤 1단위
- [ ] 햅틱 피드백 정확히 동기화
- [ ] wheel 값 ±1 범위 유지
- [ ] 스크롤 중 커서 이동 없음

###### 2.1.3.2 무한 스크롤 모드

**구현 목표**: 관성 기반 연속 스크롤
**예상 시간**: 50분-1시간
**참조**: `@docs/Android/component-touchpad.md` §2.3.2

**바이브 코딩 프롬프트**:
```
@src/android/.../components/touchpad/gestures/InfiniteScroll.kt 를 생성해:

1. InfiniteScroll 클래스:
   class InfiniteScroll(
       private val guidelineSpacing: Dp = 40.dp,
       private val speedMultiplier: Float = 2.0f,
       private val decayRate: Float = 0.95f
   ) {
       private var velocity = 0f
       private var accumulatedDistance = 0f
       private var lastFrameTime = 0L
   }

2. 관성 알고리즘:
   fun process(delta: Offset, currentTime: Long): Byte {
       // 터치 입력으로 속도 업데이트
       if (delta != Offset.Zero) {
           val dt = (currentTime - lastFrameTime) / 1000f
           velocity = (delta.y / dt) * speedMultiplier
           lastFrameTime = currentTime
       } else {
           // 터치 없을 때 지수 감속
           velocity *= decayRate
           if (abs(velocity) < 0.1f) velocity = 0f
       }
       
       // 속도 기반 스크롤량 계산
       accumulatedDistance += velocity * 0.016f  // 60fps 기준
       
       val scrollAmount = (accumulatedDistance / guidelineSpacing.toPx()).toInt()
       if (scrollAmount != 0) {
           accumulatedDistance %= guidelineSpacing.toPx()
       }
       
       return scrollAmount.coerceIn(-127, 127).toByte()
   }

3. 스크롤 가이드라인 시각화:
   @Composable
   fun ScrollGuidelines(modifier: Modifier) {
       Canvas(modifier) {
           var y = 0f
           while (y < size.height) {
               drawLine(
                   color = Color.White.copy(alpha = 0.2f),
                   start = Offset(0f, y),
                   end = Offset(size.width, y),
                   strokeWidth = 2.dp.toPx()
               )
               y += 40.dp.toPx()
           }
       }
   }

4. 더블탭으로 종료:
   var lastTapTime = 0L
   fun onTap(currentTime: Long): Boolean {
       val isDoubleTap = (currentTime - lastTapTime) < 300L
       lastTapTime = currentTime
       return isDoubleTap  // true면 스크롤 모드 종료
   }

5. ScrollMode.INFINITE 활성화 시:
   - 스크롤 가이드라인 표시
   - 관성 스크롤 활성화
   - 더블탭 감지

무한 스크롤로 긴 페이지를 빠르게 탐색할 수 있어.
```

**검증 방법**:
- [ ] 드래그 속도에 비례한 스크롤
- [ ] 터치 해제 후 관성 스크롤
- [ ] 지수 감속 0.95^배율 동작
- [ ] 더블탭으로 모드 종료
- [ ] 스크롤 가이드라인 40dp 간격

##### 2.1.4 터치패드 모드 시스템

###### 2.1.4.1 모드 Enum 및 상태 관리

**구현 목표**: 모든 터치패드 모드 정의 및 ViewModel 통합
**예상 시간**: 40-50분

**바이브 코딩 프롬프트**:
```
@src/android/.../components/touchpad/TouchpadViewModel.kt 를 생성해:

1. 모드 Enum 정의:
   enum class ClickMode { LEFT, RIGHT }
   enum class MoveMode { FREE, RIGHT_ANGLE }
   enum class CursorMode { SINGLE, MULTI }
   enum class ScrollMode { NONE, NORMAL, INFINITE }

2. TouchpadViewModel:
   class TouchpadViewModel : ViewModel() {
       private val _clickMode = MutableStateFlow(ClickMode.LEFT)
       val clickMode = _clickMode.asStateFlow()
       
       private val _moveMode = MutableStateFlow(MoveMode.FREE)
       val moveMode = _moveMode.asStateFlow()
       
       private val _cursorMode = MutableStateFlow(CursorMode.SINGLE)
       val cursorMode = _cursorMode.asStateFlow()
       
       private val _scrollMode = MutableStateFlow(ScrollMode.NONE)
       val scrollMode = _scrollMode.asStateFlow()
   }

3. 모드 전환 메서드:
   fun toggleClickMode() {
       _clickMode.value = when (_clickMode.value) {
           ClickMode.LEFT -> ClickMode.RIGHT
           ClickMode.RIGHT -> ClickMode.LEFT
       }
   }
   
   // 다른 모드들도 유사하게 toggle 메서드 추가

4. 모드 상태 지속성:
   fun saveToPreferences(context: Context) {
       val prefs = context.getSharedPreferences("touchpad", Context.MODE_PRIVATE)
       prefs.edit {
           putString("clickMode", clickMode.value.name)
           putString("moveMode", moveMode.value.name)
           // ... 나머지 모드
       }
   }
   
   fun restoreFromPreferences(context: Context) {
       val prefs = context.getSharedPreferences("touchpad", Context.MODE_PRIVATE)
       _clickMode.value = ClickMode.valueOf(
           prefs.getString("clickMode", ClickMode.LEFT.name)!!
       )
       // ... 나머지 모드
   }

5. TouchpadWrapper에서 ViewModel 사용:
   val viewModel: TouchpadViewModel = viewModel()
   val clickMode by viewModel.clickMode.collectAsState()
   // ... 나머지 모드 수집

모드를 ViewModel로 관리하면 상태가 일관되고 구성 변경 시에도 유지돼.
```

**검증 방법**:
- [ ] 모드 전환 메서드 정상 동작
- [ ] StateFlow로 UI 자동 업데이트
- [ ] 앱 재시작 후 모드 복원
- [ ] Configuration 변경 시 상태 유지

###### 2.1.4.2 제어 버튼 및 테두리 색상

**구현 목표**: 모드별 UI 시각화 시스템
**예상 시간**: 40-50분
**참조**: `@docs/Android/component-touchpad.md` §2.5

**바이브 코딩 프롬프트**:
```
@src/android/.../components/touchpad/TouchpadVisualState.kt 를 생성해:

1. 테두리 색상 우선순위 시스템:
   fun getTouchpadBorderColor(
       scrollMode: ScrollMode,
       cursorMode: CursorMode,
       moveMode: MoveMode,
       clickMode: ClickMode
   ): Color {
       return when {
           scrollMode != ScrollMode.NONE -> Color(0xFF00BCD4)  // Cyan (최우선)
           cursorMode == CursorMode.MULTI -> Color(0xFF9C27B0)  // Purple
           moveMode == MoveMode.RIGHT_ANGLE -> Color(0xFFFF9800)  // Orange
           clickMode == ClickMode.RIGHT -> Color(0xFFF44336)  // Red
           else -> Color(0xFF4CAF50)  // Green (기본)
       }
   }

2. 제어 버튼 가시성 관리:
   @Composable
   fun getVisibleButtons(
       scrollMode: ScrollMode,
       cursorMode: CursorMode
   ): List<ButtonType> {
       return buildList {
           // 스크롤 모드 활성 시 대부분 버튼 숨김
           if (scrollMode != ScrollMode.NONE) {
               add(ButtonType.SCROLL_MODE_TOGGLE)
               return@buildList
           }
           
           // 일반 모드: 모든 버튼 표시
           add(ButtonType.LEFT_CLICK)
           add(ButtonType.RIGHT_CLICK)
           add(ButtonType.MOVE_MODE_TOGGLE)
           if (cursorMode == CursorMode.MULTI) {
               add(ButtonType.CURSOR_MODE_TOGGLE)
           }
           add(ButtonType.SCROLL_MODE_TOGGLE)
       }
   }

3. 모드 아이콘 및 색상:
   @Composable
   fun getModeIcon(mode: Any): ImageVector {
       return when (mode) {
           ClickMode.LEFT -> Icons.Default.TouchApp
           ClickMode.RIGHT -> Icons.Default.TouchApp  // 다른 색상
           MoveMode.FREE -> Icons.Default.OpenWith
           MoveMode.RIGHT_ANGLE -> Icons.Default.GridOn
           ScrollMode.NORMAL -> Icons.Default.Swipe
           ScrollMode.INFINITE -> Icons.Default.AllInclusive
           else -> Icons.Default.Help
       }
   }

4. 애니메이션 전환:
   val borderColor by animateColorAsState(
       targetValue = getTouchpadBorderColor(...),
       animationSpec = tween(300)
   )

5. TouchpadWrapper 통합:
   - 모드 변경 시 테두리 색상 애니메이션
   - 제어 버튼 동적 표시/숨김
   - 모드 아이콘 자동 업데이트

테두리 색상으로 현재 모드를 한눈에 파악할 수 있어.
```

**검증 방법**:
- [ ] 모드별 테두리 색상 정확히 표시
- [ ] 우선순위에 따른 색상 결정
- [ ] 모드 전환 시 부드러운 색상 애니메이션
- [ ] 제어 버튼 가시성 동적 관리

#### 2.2 UI 컴포넌트 시스템 구현

**참조 문서**: `@docs/Android/component-design-guide-app.md`, `@docs/Android/design-guide-app.md` §5

##### 2.2.1 버튼 컴포넌트 시스템

###### 2.2.1.1 KeyboardKeyButton 구현

**구현 목표**: 단일 키 입력 버튼 + Sticky Hold 기능
**예상 시간**: 40-50분
**참조**: `@docs/Android/component-design-guide-app.md` §2.1

**바이브 코딩 프롬프트**:
```
@src/android/.../components/buttons/KeyboardKeyButton.kt 를 생성해:

1. KeyboardKeyButton Composable:
   @Composable
   fun KeyboardKeyButton(
       keyCode: UByte,
       label: String,
       modifier: Modifier = Modifier,
       isEnabled: Boolean = true,
       onKeyPress: (UByte, Boolean) -> Unit  // keyCode, isHold
   )

2. Sticky Hold 로직:
   - 500ms 롱프레스 감지
   - 롱프레스 시 버튼 배경 변경 (활성 상태)
   - 다시 탭하면 해제
   - 홀드 상태: 계속 keyCode 전송 (30ms 간격)

3. 버튼 UI:
   - 최소 크기: 48×48dp
   - 배경: 라운드 코너 8dp
   - 레이블: Pretendard Medium 14sp
   - 비활성 시: alpha = 0.4f

4. 상태 표시:
   - Normal: 기본 배경색
   - Pressed: 밝은 배경색
   - Sticky Hold: 강조 배경색 + 테두리
   - Disabled: 회색 + alpha 0.4f

5. 햅틱 피드백:
   - 탭: HapticHelper.performHaptic(HapticCategory.MEDIUM)
   - 롱프레스 시작: HapticHelper.performHaptic(HapticCategory.MEDIUM)
   - Sticky Hold 활성화: HapticHelper.performHaptic(HapticCategory.STRONG)

예시: Esc, Enter, Backspace, Space 버튼들을 만들 때 사용해.
```

**검증 방법**:
- [ ] 탭 시 키 입력 1회 전송
- [ ] 500ms 롱프레스 → Sticky Hold 활성화
- [ ] Sticky Hold 중 30ms 간격 키 반복
- [ ] 재탭으로 Sticky Hold 해제
- [ ] 비활성 상태 UI 정상 표시

###### 2.2.1.2 ShortcutButton 구현

**구현 목표**: 키 조합 단축키 버튼
**예상 시간**: 30-40분

**바이브 코딩 프롬프트**:
```
@src/android/.../components/buttons/ShortcutButton.kt 를 생성해:

1. ShortcutButton Composable:
   @Composable
   fun ShortcutButton(
       keyCombination: List<UByte>,  // 예: [Ctrl, C]
       label: String,
       icon: ImageVector? = null,
       modifier: Modifier = Modifier,
       onShortcut: (List<UByte>) -> Unit
   )

2. 키 조합 전송 로직:
   - 모든 키를 동시에 Down (modifiers + keys)
   - 150ms 대기
   - 모든 키를 동시에 Up
   - 디바운스: 150ms 내 재클릭 방지

3. 버튼 UI:
   - 키 조합 레이블 (예: "Ctrl+C", "Alt+F4")
   - 아이콘 (선택적)
   - 48×48dp 최소 크기

4. 상태 표시:
   - Pressed: 200ms 시각적 피드백
   - Disabled: 회색 처리

5. 햅틱 피드백:
   - 클릭 시: HapticHelper.performHaptic(HapticCategory.MEDIUM)

예시: Ctrl+C(복사), Ctrl+V(붙여넣기), Alt+Tab(창 전환) 버튼들.
```

**검증 방법**:
- [ ] 키 조합 동시 Down/Up
- [ ] 150ms 디바운스 동작
- [ ] Windows에서 단축키 정상 동작
- [ ] 햅틱 피드백 발생

###### 2.2.1.3 ContainerButton 구현

**구현 목표**: 롱프레스로 팝업 오버레이 표시
**예상 시간**: 50분-1시간

**바이브 코딩 프롬프트**:
```
@src/android/.../components/buttons/ContainerButton.kt 를 생성해:

1. ContainerButton Composable:
   @Composable
   fun ContainerButton(
       label: String,
       icon: ImageVector,
       childButtons: List<ButtonData>,
       modifier: Modifier = Modifier,
       onChildButtonClick: (ButtonData) -> Unit
   )

2. 롱프레스 팝업:
   var isExpanded by remember { mutableStateOf(false) }
   
   modifier.pointerInput(Unit) {
       detectTapGestures(
           onLongPress = { isExpanded = true },
           onPress = {
               // 짧은 탭: 첫 번째 자식 버튼 기본 동작
               tryAwaitRelease()
               if (!isExpanded) {
                   onChildButtonClick(childButtons.first())
               }
           }
       )
   }

3. 팝업 오버레이 UI:
   if (isExpanded) {
       Dialog(onDismissRequest = { isExpanded = false }) {
           LazyVerticalGrid(columns = 3) {
               items(childButtons) { button ->
                   ChildButton(button) {
                       onChildButtonClick(button)
                       isExpanded = false
                   }
               }
           }
       }
   }

4. 지속 모드 (선택적):
   - 롱프레스 후 손가락 유지 → 드래그로 자식 버튼 선택
   - 손가락 떼면 선택된 버튼 실행

5. 애니메이션:
   - 팝업: scaleIn + fadeIn (200ms)
   - 자식 버튼들: staggered 애니메이션

예시: Function Keys(F1-F12), Special Keys(Home, End, PgUp, PgDn) 컨테이너.
```

**검증 방법**:
- [ ] 짧은 탭 → 기본 동작
- [ ] 롱프레스 → 팝업 표시
- [ ] 자식 버튼 클릭 → 팝업 닫힘
- [ ] 애니메이션 부드러움

##### 2.2.2 DPad 컴포넌트 구현

###### 2.2.2.1 8분할 섹터 판정 알고리즘

**구현 목표**: 방향 입력을 8개 섹터로 판정
**예상 시간**: 50분-1시간
**참조**: `@docs/Android/component-design-guide-app.md` §3.1

**바이브 코딩 프롬프트**:
```
@src/android/.../components/dpad/DPadSectorDetector.kt 를 생성해:

1. Direction enum:
   enum class Direction {
       UP, UP_RIGHT, RIGHT, DOWN_RIGHT,
       DOWN, DOWN_LEFT, LEFT, UP_LEFT,
       NONE
   }

2. 섹터 판정 로직:
   fun detectDirection(touchPosition: Offset, center: Offset): Direction {
       val dx = touchPosition.x - center.x
       val dy = touchPosition.y - center.y
       val distance = sqrt(dx * dx + dy * dy)
       
       // 데드존: 중심에서 20dp 이내는 NONE
       if (distance < 20.dp.toPx()) return Direction.NONE
       
       // 각도 계산 (-180 ~ 180)
       var angle = atan2(dy, dx).toDegrees()
       if (angle < 0) angle += 360
       
       // 8분할 섹터 (45° 단위)
       // UP: 67.5° ~ 112.5° (90° ± 22.5°)
       // RIGHT: 337.5° ~ 22.5° (0° ± 22.5°)
       // ... 나머지 방향
       
       return when {
           angle in 67.5..112.5 -> Direction.DOWN        // 화면 Y축 반전
           angle in 112.5..157.5 -> Direction.DOWN_RIGHT
           angle in 157.5..202.5 -> Direction.RIGHT
           angle in 202.5..247.5 -> Direction.UP_RIGHT
           angle in 247.5..292.5 -> Direction.UP
           angle in 292.5..337.5 -> Direction.UP_LEFT
           (angle in 337.5..360.0) || (angle in 0.0..22.5) -> Direction.LEFT
           angle in 22.5..67.5 -> Direction.DOWN_LEFT
           else -> Direction.NONE
       }
   }

3. 10° 허용오차 (선택적):
   - 섹터 경계에서 ±5° 히스테리시스
   - 빠른 방향 전환 시 떨림 방지

4. 방향 → 키 매핑:
   fun directionToKeys(direction: Direction): List<UByte> {
       return when (direction) {
           Direction.UP -> listOf(KEY_UP_ARROW)
           Direction.UP_RIGHT -> listOf(KEY_UP_ARROW, KEY_RIGHT_ARROW)
           Direction.RIGHT -> listOf(KEY_RIGHT_ARROW)
           // ... 나머지 방향
           Direction.NONE -> emptyList()
       }
   }

정확한 8방향 판정으로 DPad 조작이 정밀해져.
```

**검증 방법**:
- [ ] 8방향 정확히 판정 (45° 단위)
- [ ] 데드존 20dp 동작
- [ ] 대각선 방향 → 2키 매핑
- [ ] 섹터 경계 안정적 판정

###### 2.2.2.2 대각선 입력 처리

**구현 목표**: 대각선 방향 2키 동시 Down/Up
**예상 시간**: 30-40분

**바이브 코딩 프롬프트**:
```
@src/android/.../components/dpad/DPadInputHandler.kt 를 생성해:

1. 현재 활성 방향 추적:
   var activeDirection: Direction = Direction.NONE
   var activeKeys: List<UByte> = emptyList()

2. 방향 변경 처리:
   fun onDirectionChange(newDirection: Direction) {
       if (newDirection == activeDirection) return
       
       // 이전 키들 모두 Up
       activeKeys.forEach { keyCode ->
           sendKeyUp(keyCode)
       }
       
       // 새 방향의 키들 모두 Down
       activeKeys = directionToKeys(newDirection)
       activeKeys.forEach { keyCode ->
           sendKeyDown(keyCode)
       }
       
       activeDirection = newDirection
   }

3. 50ms 디바운스:
   - 방향 전환 후 50ms 이내 재전환 무시
   - 빠른 떨림 방지

4. 터치 해제 처리:
   fun onTouchEnd() {
       activeKeys.forEach { keyCode ->
           sendKeyUp(keyCode)
       }
       activeDirection = Direction.NONE
       activeKeys = emptyList()
   }

5. BridgeFrame 구성:
   - keyCode1: 첫 번째 키
   - keyCode2: 두 번째 키 (대각선인 경우)
   - 단일 방향: keyCode2 = 0

대각선 입력으로 게임에서 동시에 2개 방향키를 누를 수 있어.
```

**검증 방법**:
- [ ] 대각선 방향 → 2키 동시 Down
- [ ] 방향 전환 → 이전 키 Up, 새 키 Down
- [ ] 터치 해제 → 모든 키 Up
- [ ] 50ms 디바운스 동작

###### 2.2.2.3 Sticky Hold 기능

**구현 목표**: 더블탭으로 방향 고정
**예상 시간**: 30-40분

**바이브 코딩 프롬프트**:
```
@src/android/.../components/dpad/DPadStickyHold.kt 를 생성해:

1. Sticky Hold 상태:
   var isStickyActive = false
   var stickyDirection: Direction = Direction.NONE

2. 더블탭 감지:
   var lastTapTime = 0L
   var lastTapDirection: Direction = Direction.NONE
   
   fun onTap(direction: Direction, currentTime: Long) {
       val isDoubleTap = (currentTime - lastTapTime) < 300L &&
                         direction == lastTapDirection
       
      if (isDoubleTap && !isStickyActive) {
          // Sticky Hold 활성화
          isStickyActive = true
          stickyDirection = direction
          hapticHelper.performHaptic(HapticCategory.STRONG)
      } else if (isDoubleTap && isStickyActive) {
          // Sticky Hold 해제
          isStickyActive = false
          stickyDirection = Direction.NONE
          // 모든 키 Up
      }
       
       lastTapTime = currentTime
       lastTapDirection = direction
   }

3. Sticky Hold 중 키 유지:
   - 터치 이동해도 stickyDirection 키 유지
   - 터치 해제해도 키 유지
   - 더블탭으로만 해제

4. UI 표시:
   - Sticky Hold 활성 시 DPad에 "고정됨" 표시
   - 고정된 방향에 강조 표시

5. DPad Composable 통합:
   - 더블탭 감지
   - Sticky Hold 상태 UI 업데이트

Sticky Hold로 방향키를 계속 누르고 있는 효과를 낼 수 있어.
```

**검증 방법**:
- [ ] 300ms 내 동일 방향 더블탭 → Sticky Hold 활성화
- [ ] 터치 해제해도 키 유지
- [ ] 더블탭으로 해제
- [ ] UI에 고정 상태 표시

##### 2.2.3 공통 컴포넌트 시스템

###### 2.2.3.1 상태 알림 토스트

**구현 목표**: 5가지 카테고리 토스트 + 애니메이션
**예상 시간**: 40-50분

**바이브 코딩 프롬프트**:
```
@src/android/.../components/common/StatusToast.kt 를 생성해:

1. ToastCategory enum:
   enum class ToastCategory {
       SUCCESS,    // 녹색
       ERROR,      // 빨강
       WARNING,    // 주황
       INFO,       // 파랑
       DEBUG       // 회색
   }

2. StatusToast Composable:
   @Composable
   fun StatusToast(
       message: String,
       category: ToastCategory,
       duration: Long = 3000L,
       onDismiss: () -> Unit
   )

3. 토스트 UI:
   - 화면 하단에서 슬라이드 업
   - 카테고리별 배경색 및 아이콘
   - 메시지 텍스트 (Pretendard Regular 14sp)
   - 자동 dismiss (3초)

4. 애니메이션:
   LaunchedEffect(Unit) {
       // 슬라이드 업 + 페이드 인 (300ms)
       delay(duration)
       // 페이드 아웃 (200ms)
       onDismiss()
   }

5. ToastManager 싱글톤:
   object ToastManager {
       private val _toasts = MutableStateFlow<List<ToastData>>(emptyList())
       val toasts = _toasts.asStateFlow()
       
       fun show(message: String, category: ToastCategory) {
           _toasts.value += ToastData(message, category, UUID.randomUUID())
       }
   }

6. MainActivity에서 토스트 표시:
   Box(Modifier.fillMaxSize()) {
       // ... 메인 UI
       
       // 토스트 오버레이
       ToastContainer(
           toasts = toastManager.toasts.collectAsState().value
       )
   }

토스트로 연결 상태, 오류, 모드 전환 등을 사용자에게 알릴 수 있어.
```

**검증 방법**:
- [ ] 5가지 카테고리 색상 구분
- [ ] 슬라이드 업 애니메이션
- [ ] 3초 후 자동 사라짐
- [ ] 여러 토스트 동시 표시 가능

###### 2.2.3.2 페이지 인디케이터 및 햅틱 피드백

**구현 목표**: 페이지 인디케이터 + 햅틱 카테고리 시스템
**예상 시간**: 30-40분

**바이브 코딩 프롬프트**:
```
**페이지 인디케이터** (@src/android/.../components/common/PageIndicator.kt):

1. PageIndicator Composable:
   @Composable
   fun PageIndicator(
       currentPage: Int,
       totalPages: Int,
       modifier: Modifier = Modifier
   )

2. 인디케이터 UI:
   Row {
       repeat(totalPages) { index ->
           Box(
               modifier = Modifier
                   .size(if (index == currentPage) 12.dp else 8.dp)
                   .background(
                       color = if (index == currentPage) 
                           Color.White else Color.White.copy(alpha = 0.4f),
                       shape = CircleShape
                   )
           )
           Spacer(8.dp)
       }
   }

3. 선택 애니메이션:
   val size by animateDpAsState(
       targetValue = if (isSelected) 12.dp else 8.dp,
       animationSpec = spring(dampingRatio = 0.6f)
   )

**햅틱 피드백 시스템** (@src/android/.../utils/HapticHelper.kt):

1. HapticCategory enum과 진동 패턴:
   enum class HapticCategory(val durationMs: Long) {
       LIGHT(5),       // 터치 시작 (5ms)
       MEDIUM(10),     // 버튼 클릭 (10ms)
       STRONG(20),     // 모드 전환 (20ms)
       ERROR(50)       // 오류 발생 (50ms)
   }

2. HapticHelper class (Vibrator 시스템 서비스 기반):
   class HapticHelper(private val vibrator: Vibrator) {
       fun performHaptic(category: HapticCategory) {
           if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
               vibrator.vibrate(
                   VibrationEffect.createOneShot(
                       category.durationMs,
                       VibrationEffect.DEFAULT_AMPLITUDE
                   )
               )
           } else {
               @Suppress("DEPRECATION")
               vibrator.vibrate(category.durationMs)
           }
       }
   }

3. Compose에서 사용:
   val context = LocalContext.current
   val hapticHelper = remember {
       val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
           context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
               .defaultVibrator
       } else {
           @Suppress("DEPRECATION")
           context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
       }
       HapticHelper(vibrator)
   }
   
   // 사용 예시
   Button(onClick = { 
       hapticHelper.performHaptic(HapticCategory.MEDIUM)
   })

모든 UI 컴포넌트에서 HapticHelper를 사용해서 일관된 피드백을 제공해줘.
AndroidManifest.xml에 VIBRATE 권한이 이미 선언되어 있음.
```

**검증 방법**:
- [ ] 페이지 인디케이터 크기/색상 애니메이션
- [ ] 4가지 햅틱 카테고리 구분 가능
- [ ] 일관된 햅틱 피드백 제공

###### 2.2.3.3 컴포넌트 비활성화 시스템

**구현 목표**: 모드/상태별 컴포넌트 활성화 제어
**예상 시간**: 40-50분

**바이브 코딩 프롬프트**:
```
@src/android/.../components/common/ComponentState.kt 를 생성해:

1. 비활성화 규칙 시스템:
   interface DisableRule {
       fun shouldDisable(appState: AppState, touchpadMode: TouchpadMode): Boolean
   }
   
   class ScrollModeActiveRule : DisableRule {
       override fun shouldDisable(appState: AppState, touchpadMode: TouchpadMode): Boolean {
           return touchpadMode.scrollMode != ScrollMode.NONE
       }
   }

2. ComponentStateManager:
   class ComponentStateManager {
       private val rules = mutableListOf<DisableRule>()
       
       fun addRule(rule: DisableRule) {
           rules.add(rule)
       }
       
       fun isEnabled(appState: AppState, touchpadMode: TouchpadMode): Boolean {
           return rules.none { it.shouldDisable(appState, touchpadMode) }
       }
   }

3. Composable modifier 확장:
   fun Modifier.enabledIf(
       isEnabled: Boolean,
       disabledAlpha: Float = 0.4f
   ): Modifier {
       return this
           .alpha(if (isEnabled) 1.0f else disabledAlpha)
           .clickable(enabled = isEnabled) { }
   }

4. 강제 활성화 메커니즘:
   var forceEnableAll = false
   
   fun onForceEnableRequest() {
       forceEnableAll = true
       // 5초 후 자동 해제
       delay(5000)
       forceEnableAll = false
   }

5. 사용 예시:
   KeyboardKeyButton(
       modifier = Modifier.enabledIf(
           isEnabled = componentState.isEnabled() || forceEnableAll
       )
   )

컴포넌트 비활성화로 사용자가 잘못된 조작을 하는 것을 방지해.
```

**검증 방법**:
- [ ] 스크롤 모드 활성 시 다른 버튼 비활성화
- [ ] 비활성화 시 alpha 0.4f
- [ ] 강제 활성화 5초간 동작
- [ ] 규칙 기반 활성화 제어

#### 2.3 페이지 네비게이션 및 상태 관리

**참조 문서**: `@docs/Android/styleframe-*.md`, `@docs/Android/design-guide-app.md` §8.2

##### 2.3.1 4개 페이지 레이아웃 구현

###### 2.3.1.1 Essential 페이지 레이아웃

**구현 목표**: Boot Keyboard + 간소화 터치패드
**예상 시간**: 50분-1시간
**참조**: `@docs/Android/styleframe-essential.md`

**바이브 코딩 프롬프트**:
```
@src/android/.../pages/EssentialPage.kt 를 생성해:

1. EssentialPage Composable:
   @Composable
   fun EssentialPage(
       modifier: Modifier = Modifier,
       onKeyPress: (UByte) -> Unit,
       onTouchpadInput: (BridgeFrame) -> Unit
   )

2. 레이아웃 구조 (Column):
   - 상단 배너: "Windows 서버 연결 대기 중..." (10%)
   - Boot Keyboard: 4행 QWERTY (40%)
     * 1행: Q~P (10키)
     * 2행: A~L (9키)
     * 3행: Z~M (7키)
     * 4행: Space, Enter, Backspace
   - 간소화 터치패드 (50%)
     * 좌클릭만 지원
     * 제어 버튼 없음

3. Boot Keyboard 레이아웃:
   LazyVerticalGrid(columns = 10) {
       items(QWERTY_KEYS) { key ->
           KeyboardKeyButton(
               keyCode = key.code,
               label = key.label,
               onKeyPress = onKeyPress
           )
       }
   }

4. 간소화 터치패드:
   TouchpadWrapper(
       modifier = Modifier.weight(0.5f),
       clickMode = ClickMode.LEFT,  // 고정
       showControls = false  // 제어 버튼 숨김
   )

5. 상태 배너:
   - USB 연결 상태 표시
   - "Essential 모드" 레이블
   - 간단한 아이콘

@docs/Android/styleframe-essential.md 의 레이아웃을 정확히 따라줘.
```

**검증 방법**:
- [ ] Boot Keyboard 4행 정확히 배치
- [ ] 터치패드 좌클릭만 동작
- [ ] 제어 버튼 숨김
- [ ] 상태 배너 표시

###### 2.3.1.2 Page 1 레이아웃 (터치패드 + Actions)

**구현 목표**: 메인 페이지 - 터치패드 + Special Keys + Shortcuts
**예상 시간**: 50분-1시간
**참조**: `@docs/Android/styleframe-page1.md`

**바이브 코딩 프롬프트**:
```
@src/android/.../pages/Page1.kt 를 생성해:

1. Page1 Composable 레이아웃 (Column):
   - 상단: 터치패드 (70%)
   - 하단: Actions 섹션 (30%)
     * Special Keys (15%): Esc, Tab, Enter, Backspace
     * Shortcuts (15%): Ctrl+C, Ctrl+V, Ctrl+Z, Ctrl+Y

2. 터치패드:
   TouchpadWrapper(
       modifier = Modifier.weight(0.7f),
       showAllControls = true,  // 모든 제어 버튼 표시
       onFrameSend = onTouchpadInput
   )

3. Special Keys Row:
   Row(
       modifier = Modifier.fillMaxWidth().weight(0.15f),
       horizontalArrangement = Arrangement.SpaceEvenly
   ) {
       KeyboardKeyButton(keyCode = KEY_ESC, label = "Esc")
       KeyboardKeyButton(keyCode = KEY_TAB, label = "Tab")
       KeyboardKeyButton(keyCode = KEY_ENTER, label = "Enter")
       KeyboardKeyButton(keyCode = KEY_BACKSPACE, label = "BS")
   }

4. Shortcuts Row:
   Row(
       modifier = Modifier.fillMaxWidth().weight(0.15f),
       horizontalArrangement = Arrangement.SpaceEvenly
   ) {
       ShortcutButton(keys = listOf(KEY_CTRL, KEY_C), label = "복사")
       ShortcutButton(keys = listOf(KEY_CTRL, KEY_V), label = "붙여넣기")
       ShortcutButton(keys = listOf(KEY_CTRL, KEY_Z), label = "실행취소")
       ShortcutButton(keys = listOf(KEY_CTRL, KEY_Y), label = "다시실행")
   }

5. 반응형 레이아웃:
   - 세로 모드: 위 구조 그대로
   - 가로 모드: 터치패드 좌측, Actions 우측

@docs/Android/styleframe-page1.md 의 디자인을 따라줘.
```

**검증 방법**:
- [ ] 터치패드 70% 영역
- [ ] Special Keys 4개 배치
- [ ] Shortcuts 4개 배치
- [ ] 세로/가로 모드 대응

###### 2.3.1.3 Page 2 레이아웃 (키보드 중심)

**구현 목표**: 키보드 페이지 - Modifiers + Navigation + Function Keys
**예상 시간**: 40-50분
**참조**: `@docs/Android/styleframe-page2.md`

**바이브 코딩 프롬프트**:
```
@src/android/.../pages/Page2.kt 를 생성해:

1. Page2 레이아웃 (Column):
   - Modifiers (25%): Ctrl, Shift, Alt, GUI
   - Navigation Keys (25%): Home, End, PgUp, PgDn, 방향키
   - Function Keys (25%): F1-F12 (ContainerButton)
   - 터치패드 (25%): 축소 버전

2. Modifiers Section:
   LazyVerticalGrid(columns = 2, rows = 2) {
       item { KeyboardKeyButton(KEY_CTRL, "Ctrl", supportsStickyHold = true) }
       item { KeyboardKeyButton(KEY_SHIFT, "Shift", supportsStickyHold = true) }
       item { KeyboardKeyButton(KEY_ALT, "Alt", supportsStickyHold = true) }
       item { KeyboardKeyButton(KEY_GUI, "Win", supportsStickyHold = true) }
   }

3. Navigation Keys:
   Column {
       Row { /* 방향키 4개 */ }
       Row { 
           KeyboardKeyButton(KEY_HOME, "Home")
           KeyboardKeyButton(KEY_END, "End")
           KeyboardKeyButton(KEY_PGUP, "PgUp")
           KeyboardKeyButton(KEY_PGDN, "PgDn")
       }
   }

4. Function Keys Container:
   ContainerButton(
       label = "F1-F12",
       icon = Icons.Default.Functions,
       childButtons = (1..12).map { ButtonData(KEY_F1 + it - 1, "F$it") }
   )

5. 축소 터치패드:
   TouchpadWrapper(
       modifier = Modifier.weight(0.25f),
       showControls = false
   )

@docs/Android/styleframe-page2.md 를 참조해줘.
```

**검증 방법**:
- [ ] Modifiers 4개 정확히 배치
- [ ] Navigation Keys 동작
- [ ] Function Keys 컨테이너 팝업
- [ ] 축소 터치패드 동작

###### 2.3.1.4 Page 3 레이아웃 (Minecraft 특화)

**구현 목표**: Minecraft 게임용 특화 레이아웃
**예상 시간**: 50분-1시간
**참조**: `@docs/Android/styleframe-page3.md`

**바이브 코딩 프롬프트**:
```
@src/android/.../pages/Page3Minecraft.kt 를 생성해:

1. Page3 레이아웃 (Row):
   - 좌측 (40%): DPad + Combat
   - 우측 (60%): Hotbar + 터치패드

2. DPad Section:
   - DPad 컴포넌트 (WASD 또는 방향키)
   - Sticky Hold 지원
   - 대각선 입력 지원

3. Combat Section:
   Row {
       KeyboardKeyButton(KEY_SPACE, "점프")
       KeyboardKeyButton(MOUSE_LEFT, "공격")
       KeyboardKeyButton(MOUSE_RIGHT, "사용")
       KeyboardKeyButton(KEY_SHIFT, "웅크리기", supportsStickyHold = true)
   }

4. Hotbar Section (1-9 숫자키):
   LazyHorizontalGrid(rows = 1) {
       items(9) { index ->
           KeyboardKeyButton(
               keyCode = KEY_1 + index,
               label = "${index + 1}"
           )
       }
   }

5. 축소 터치패드:
   TouchpadWrapper(
       modifier = Modifier.fillMaxHeight(),
       clickMode = ClickMode.RIGHT,  // 우클릭 기본
       showLimitedControls = true
   )

Minecraft 플레이에 최적화된 레이아웃이야.
```

**검증 방법**:
- [ ] DPad WASD 입력 동작
- [ ] Combat 버튼 동작
- [ ] Hotbar 1-9 숫자키
- [ ] 터치패드 우클릭 기본

##### 2.3.2 페이지 전환 시스템

###### 2.3.2.1 HorizontalPager 구현 및 슬라이드 제스처

**구현 목표**: 좌우 스와이프로 페이지 전환
**예상 시간**: 40-50분

**바이브 코딩 프롬프트**:
```
@src/android/.../MainActivity.kt 에 페이지 전환 시스템을 추가해:

1. HorizontalPager 설정:
   val pagerState = rememberPagerState(initialPage = 0) { 4 }  // 4페이지
   
   HorizontalPager(
       state = pagerState,
       modifier = Modifier.fillMaxSize()
   ) { page ->
       when (page) {
           0 -> EssentialPage()
           1 -> Page1()
           2 -> Page2()
           3 -> Page3Minecraft()
       }
   }

2. 슬라이드 제스처:
   - 화면 폭 20% 이상 스와이프 → 페이지 전환
   - fling 속도 기반 자동 스냅
   - 부드러운 스크롤 애니메이션

3. 프로그래매틱 페이지 전환:
   val scope = rememberCoroutineScope()
   
   fun navigateToPage(page: Int) {
       scope.launch {
           pagerState.animateScrollToPage(page)
       }
   }

4. 페이지 전환 감지:
   LaunchedEffect(pagerState.currentPage) {
       // 페이지 변경 시 로직
       onPageChanged(pagerState.currentPage)
       hapticHelper.performHaptic(HapticCategory.STRONG)
   }

5. Essential/Standard 모드 연동:
   - Essential 모드: pager 비활성화, 0페이지 고정
   - Standard 모드: pager 활성화, 1-3페이지 전환 가능

HorizontalPager로 네이티브 앱처럼 부드러운 페이지 전환을 구현해.
```

**검증 방법**:
- [ ] 좌우 스와이프로 페이지 전환
- [ ] 20% 임계값 동작
- [ ] 부드러운 애니메이션
- [ ] 페이지 전환 시 햅틱 피드백

###### 2.3.2.2 페이지 전환 애니메이션

**구현 목표**: 스프링 애니메이션 및 페이지 인디케이터 동기화
**예상 시간**: 30-40분

**바이브 코딩 프롬프트**:
```
@src/android/.../MainActivity.kt 에 페이지 애니메이션을 추가해:

1. 커스텀 페이지 전환 애니메이션:
   HorizontalPager(
       pageSpacing = 16.dp,
       contentPadding = PaddingValues(horizontal = 32.dp)
   ) { page ->
       // 페이지 스케일 효과
       val pageOffset = (pagerState.currentPage - page) + 
                        pagerState.currentPageOffsetFraction
       
       Box(
           modifier = Modifier
               .graphicsLayer {
                   // 현재 페이지는 크게, 양옆은 작게
                   val scale = lerp(0.85f, 1.0f, 1f - abs(pageOffset).coerceIn(0f, 1f))
                   scaleX = scale
                   scaleY = scale
                   
                   // 투명도 효과
                   alpha = lerp(0.5f, 1.0f, 1f - abs(pageOffset).coerceIn(0f, 1f))
               }
       ) {
           // 페이지 내용
       }
   }

2. 스프링 애니메이션 스펙:
   animateScrollToPage(
       page = targetPage,
       animationSpec = spring(
           dampingRatio = 0.8f,
           stiffness = 300f
       )
   )
   
   // 총 400ms 소요 목표

3. 페이지 인디케이터 동기화:
   PageIndicator(
       currentPage = pagerState.currentPage,
       totalPages = 4,
       modifier = Modifier
           .align(Alignment.BottomCenter)
           .padding(bottom = 16.dp)
   )
   
   // pagerState.currentPage가 변경되면 자동 업데이트

4. 전환 중 터치 입력 처리:
   val isTransitioning = pagerState.isScrollInProgress
   // 전환 중에는 터치패드 입력 비활성화 (선택적)

페이지 전환이 iOS 네이티브처럼 부드럽게 보이도록 해줘.
```

**검증 방법**:
- [ ] 스프링 애니메이션 400ms
- [ ] 페이지 스케일 효과 (0.85-1.0)
- [ ] 페이지 인디케이터 동기화
- [ ] 전환 중 햅틱 피드백

##### 2.3.3 상태 관리 시스템

###### 2.3.3.1 페이지별 상태 지속성

**구현 목표**: SharedPreferences로 페이지 상태 저장/복원
**예상 시간**: 30-40분

**바이브 코딩 프롬프트**:
```
@src/android/.../data/PreferencesManager.kt 를 생성해:

1. PreferencesManager 클래스:
   class PreferencesManager(private val context: Context) {
       private val prefs = context.getSharedPreferences(
           "bridgeone_prefs", 
           Context.MODE_PRIVATE
       )
   }

2. 페이지 상태 저장:
   fun savePageState(page: Int, state: PageState) {
       prefs.edit {
           putInt("lastPage", page)
           putString("page${page}_clickMode", state.clickMode.name)
           putString("page${page}_moveMode", state.moveMode.name)
           // ... 다른 모드들
       }
   }

3. 페이지 상태 복원:
   fun restorePageState(page: Int): PageState {
       return PageState(
           clickMode = ClickMode.valueOf(
               prefs.getString("page${page}_clickMode", "LEFT")!!
           ),
           moveMode = MoveMode.valueOf(
               prefs.getString("page${page}_moveMode", "FREE")!!
           ),
           // ... 다른 모드들
       )
   }

4. 앱 상태 저장:
   fun saveAppState(appState: AppState) {
       prefs.edit {
           putString("lastAppState", appState.name)
       }
   }

5. MainActivity에서 사용:
   LaunchedEffect(Unit) {
       val lastPage = prefsManager.getInt("lastPage", 0)
       pagerState.scrollToPage(lastPage)
       
       val pageState = prefsManager.restorePageState(lastPage)
       viewModel.applyPageState(pageState)
   }

앱 재시작 후에도 마지막 페이지와 설정이 유지돼.
```

**검증 방법**:
- [ ] 앱 재시작 후 마지막 페이지로 복원
- [ ] 페이지별 모드 설정 유지
- [ ] 앱 상태 복원
- [ ] SharedPreferences 저장 확인

#### 2.4 성능 최적화 및 메모리 관리

##### 2.4.1 렌더링 최적화

###### 2.4.1.1 Compose Recomposition 최소화

**구현 목표**: remember, derivedStateOf로 불필요한 recomposition 방지
**예상 시간**: 40-50분

**바이브 코딩 프롬프트**:
```
각 Composable에 recomposition 최적화를 적용해:

1. remember로 계산 결과 캐싱:
   @Composable
   fun TouchpadWrapper() {
       val density = LocalDensity.current
       val touchpadSize = remember(density) {
           // 크기 계산을 한 번만 수행
           calculateTouchpadSize(density)
       }
   }

2. derivedStateOf로 파생 상태:
   val isScrollActive = remember {
       derivedStateOf {
           scrollMode.value != ScrollMode.NONE
       }
   }.value

3. key 매개변수로 불필요한 재구성 방지:
   items(buttons, key = { it.id }) { button ->
       KeyboardKeyButton(button)
   }

4. 안정적 매개변수:
   @Stable
   data class TouchpadConfig(
       val clickMode: ClickMode,
       val moveMode: MoveMode
   )
   
   // 매번 새 객체 생성 대신 안정적 데이터 클래스 사용

5. Compose Compiler Metrics 확인:
   - 빌드 시 recomposition 횟수 측정
   - 목표: 터치 이벤트당 <5회 recomposition

@Composable 함수들을 검토하고 불필요한 recomposition을 제거해줘.
```

**검증 방법**:
- [ ] Layout Inspector로 recomposition 확인
- [ ] 터치 이벤트당 recomposition <5회
- [ ] UI 응답성 유지
- [ ] CPU 사용률 감소

###### 2.4.1.2 GPU 가속 및 하드웨어 레이어

**구현 목표**: 하드웨어 가속으로 60fps 유지
**예상 시간**: 30-40분

**바이브 코딩 프롬프트**:
```
@src/android/.../components/ 에 GPU 가속 최적화를 적용해:

1. graphicsLayer modifier 활용:
   TouchpadWrapper(
       modifier = Modifier.graphicsLayer {
           // 하드웨어 레이어로 오프스크린 렌더링
           compositingStrategy = CompositingStrategy.Offscreen
           // 또는 CompositingStrategy.ModulateAlpha
       }
   )

2. 애니메이션 최적화:
   val scale by animateFloatAsState(
       targetValue = if (isPressed) 0.95f else 1.0f,
       animationSpec = spring(stiffness = 300f)
   )
   
   Modifier.graphicsLayer {
       scaleX = scale
       scaleY = scale
   }

3. Canvas 하드웨어 가속:
   Canvas(modifier = Modifier.fillMaxSize()) {
       // drawPath, drawCircle 등은 자동으로 GPU 가속
       drawCircle(
           color = Color.White,
           radius = 50f
       )
   }

4. 과도한 레이어 방지:
   - 필요한 곳에만 graphicsLayer 적용
   - 정적 콘텐츠는 레이어 불필요

5. Android Manifest에 하드웨어 가속 활성화 확인:
   <application android:hardwareAccelerated="true">

GPU 가속으로 복잡한 UI도 60fps를 안정적으로 유지해.
```

**검증 방법**:
- [ ] GPU Profiler에서 렌더링 시간 <16ms
- [ ] 60fps 안정 유지
- [ ] 프레임 드롭 없음
- [ ] 과도한 overdraw 없음

###### 2.4.1.3 Canvas 기반 고성능 그리기

**구현 목표**: 복잡한 UI를 Canvas로 직접 그리기
**예상 시간**: 40-50분

**바이브 코딩 프롬프트**:
```
@src/android/.../components/touchpad/TouchpadCanvas.kt 를 생성해:

1. 스크롤 가이드라인을 Canvas로:
   @Composable
   fun ScrollGuidelinesCanvas(
       modifier: Modifier = Modifier,
       spacing: Dp = 40.dp,
       isVisible: Boolean
   ) {
       Canvas(modifier) {
           if (!isVisible) return@Canvas
           
           val spacingPx = spacing.toPx()
           var y = 0f
           
           while (y < size.height) {
               drawLine(
                   color = Color.White.copy(alpha = 0.2f),
                   start = Offset(0f, y),
                   end = Offset(size.width, y),
                   strokeWidth = 2.dp.toPx()
               )
               y += spacingPx
           }
       }
   }

2. DPad를 Canvas로:
   @Composable
   fun DPadCanvas(
       modifier: Modifier = Modifier,
       currentDirection: Direction,
       isStickyHold: Boolean
   ) {
       Canvas(modifier) {
           val center = Offset(size.width / 2, size.height / 2)
           val radius = size.minDimension / 2
           
           // 8개 섹터 그리기
           for (i in 0..7) {
               val startAngle = i * 45f - 22.5f
               val isActive = currentDirection.ordinal == i
               
               drawArc(
                   color = if (isActive) Color.Green else Color.Gray,
                   startAngle = startAngle,
                   sweepAngle = 45f,
                   useCenter = true,
                   topLeft = Offset(center.x - radius, center.y - radius),
                   size = Size(radius * 2, radius * 2)
               )
           }
       }
   }

3. 성능 최적화:
   - drawPath보다 drawCircle, drawRect 사용
   - 미리 계산된 Path 객체 재사용
   - remember로 불변 객체 캐싱

Canvas로 직접 그리면 Composable 오버헤드 없이 고성능 렌더링이 가능해.
```

**검증 방법**:
- [ ] Canvas 렌더링 <5ms
- [ ] 60fps 유지
- [ ] 스크롤 가이드라인 부드러움
- [ ] DPad 섹터 정확히 그려짐

##### 2.4.2 메모리 관리

###### 2.4.2.1 객체 풀링 확장

**구현 목표**: PointF, Frame, MotionEvent 풀링
**예상 시간**: 30-40분
**참조**: 1.3.2.1에서 구현한 ObjectPool

**바이브 코딩 프롬프트**:
```
@src/android/.../input/ObjectPool.kt 를 확장해:

1. MotionEventPool 추가:
   object MotionEventPool {
       private val pool = mutableListOf<MotionEventData>()
       private const val MAX_SIZE = 20
       
       data class MotionEventData(
           var action: Int = 0,
           var x: Float = 0f,
           var y: Float = 0f,
           var eventTime: Long = 0L
       )
       
       fun acquire(): MotionEventData {
           return pool.removeFirstOrNull() ?: MotionEventData()
       }
       
       fun release(event: MotionEventData) {
           if (pool.size < MAX_SIZE) {
               event.action = 0
               event.x = 0f
               event.y = 0f
               event.eventTime = 0L
               pool.add(event)
           }
       }
   }

2. 풀 통계:
   object PoolStatistics {
       var pointFHits = 0
       var pointFMisses = 0
       var frameHits = 0
       var frameMisses = 0
       
       fun getHitRate(): Float {
           val totalHits = pointFHits + frameHits
           val totalAttempts = totalHits + pointFMisses + frameMisses
           return if (totalAttempts > 0) totalHits.toFloat() / totalAttempts else 0f
       }
   }

3. TouchInputProcessor에 적용:
   - MotionEventData 풀링
   - 통계 수집
   - 1초마다 히트율 로그

4. 메모리 압박 시 풀 축소:
   fun trimPool() {
       if (pool.size > MAX_SIZE / 2) {
           pool.subList(MAX_SIZE / 2, pool.size).clear()
       }
   }

객체 풀링 확장으로 GC를 거의 발생시키지 않아.
```

**검증 방법**:
- [ ] 풀 히트율 >90%
- [ ] Android Profiler에서 GC 빈도 <1회/초
- [ ] 메모리 사용량 안정
- [ ] 객체 생성 수 최소화

###### 2.4.2.2 배치 처리 시스템

**구현 목표**: 4프레임 단위 배치 처리
**예상 시간**: 40-50분

**바이브 코딩 프롬프트**:
```
@src/android/.../input/FrameBatcher.kt 를 생성해:

1. FrameBatcher 클래스:
   class FrameBatcher(
       private val batchSize: Int = 4,
       private val timeoutMs: Long = 8L
   ) {
       private val frameBuffer = mutableListOf<BridgeFrame>()
       private var lastFlushTime = System.currentTimeMillis()
   }

2. 배치 로직:
   fun addFrame(frame: BridgeFrame): List<BridgeFrame>? {
       frameBuffer.add(frame)
       
       val shouldFlush = frameBuffer.size >= batchSize ||
                         (System.currentTimeMillis() - lastFlushTime) > timeoutMs
       
       if (shouldFlush) {
           val batch = frameBuffer.toList()
           frameBuffer.clear()
           lastFlushTime = System.currentTimeMillis()
           return batch
       }
       
       return null  // 아직 flush하지 않음
   }

3. USB 전송 최적화:
   fun sendBatch(frames: List<BridgeFrame>) {
       // 4개 프레임을 한 번에 32바이트로 전송
       val batchData = ByteArray(frames.size * 8)
       frames.forEachIndexed { index, frame ->
           frame.toByteArray().copyInto(batchData, index * 8)
       }
       usbPort.write(batchData, timeout)
   }

4. 타임아웃 처리:
   - 8ms 이내 4프레임 미만이면 즉시 flush
   - 지연시간 보장

5. 통계:
   - 평균 배치 크기
   - flush 빈도
   - 타임아웃 발생 횟수

배치 처리로 USB 전송 오버헤드를 줄이고 처리량을 높여.
```

**검증 방법**:
- [ ] 4프레임 단위 배치 전송
- [ ] 8ms 타임아웃 동작
- [ ] 평균 배치 크기 ≥3
- [ ] 지연시간 증가 없음 (<10ms)

###### 2.4.2.3 스레드 풀 관리

**구현 목표**: UI/통신/계산 스레드 분리
**예상 시간**: 30-40분

**바이브 코딩 프롬프트**:
```
@src/android/.../threading/ThreadPoolManager.kt 를 생성해:

1. 스레드 풀 정의:
   object ThreadPoolManager {
       // UI 스레드: Dispatchers.Main (기본)
       
       // 통신 스레드: 단일 스레드 (순서 보장)
       val usbDispatcher = Executors.newSingleThreadExecutor {
           Thread(it, "USB-Thread").apply { priority = Thread.MAX_PRIORITY }
       }.asCoroutineDispatcher()
       
       // 계산 스레드: CPU 코어 수만큼
       val computeDispatcher = Dispatchers.Default
   }

2. 작업 분리:
   // USB 전송 (통신 스레드)
   viewModelScope.launch(ThreadPoolManager.usbDispatcher) {
       usbPort.write(frameData)
   }
   
   // 프레임 생성 (계산 스레드)
   viewModelScope.launch(ThreadPoolManager.computeDispatcher) {
       val frame = createBridgeFrame(touchData)
       withContext(Dispatchers.Main) {
           updateUI(frame)
       }
   }

3. 우선순위 관리:
   - USB 전송: 최고 우선순위 (Thread.MAX_PRIORITY)
   - 계산: 기본 우선순위
   - UI: 메인 스레드

4. 스레드 풀 통계:
   - 활성 작업 수
   - 대기 작업 수
   - 평균 처리 시간

5. 정리 로직:
   fun shutdown() {
       usbDispatcher.close()
       // 모든 작업 완료 대기
   }

스레드를 명확히 분리해서 USB 전송이 UI에 영향을 주지 않아.
```

**검증 방법**:
- [ ] USB 전송이 UI 블록하지 않음
- [ ] 스레드 풀 통계 정상
- [ ] CPU 코어 효율적 사용
- [ ] 앱 종료 시 정상 shutdown

**Phase 2 완료 조건**: 
- ✅ 모든 터치패드 모드 및 제스처 동작
- ✅ 60fps 애니메이션 안정 유지
- ✅ 4개 페이지 완전 구현 및 부드러운 전환
- ✅ 메모리 최적화 (GC <1회/초, 풀 히트율 >90%)
- ✅ Compose recomposition 최소화

---

### Phase 3: Windows 서버 고급 기능 구현

**목표**: 멀티 커서 등 Windows 서버 기반 고급 기능 완전 구현
**예상 기간**: 4-5주
**완료 기준**: 멀티 커서 텔레포트, 커서 팩 연동 완료

#### 3.1 Windows 서버 WPF GUI 구조

**참조 문서**: `@docs/Windows/technical-specification-server.md`, `@docs/Windows/design-guide-server.md`

##### 3.1.1 WPF 프로젝트 구조 및 Fluent Design

###### 3.1.1.1 WPF UI 라이브러리 통합

**구현 목표**: lepoco/wpfui 통합 및 Fluent Design System 적용
**예상 시간**: 50분-1시간
**참조**: `@docs/Windows/design-guide-server.md` §10.2

**바이브 코딩 프롬프트**:
```
@src/windows/BridgeOne/BridgeOne.csproj 에 NuGet 패키지 추가:
1. WPF-UI (lepoco/wpfui) - Fluent Design
2. Microsoft.Extensions.DependencyInjection
3. Microsoft.Extensions.Hosting
4. CommunityToolkit.Mvvm

@src/windows/BridgeOne/ 에 App.xaml 및 MainWindow.xaml 생성:

1. App.xaml에서 WPF-UI 테마 적용:
   <Application.Resources>
       <ResourceDictionary>
           <ResourceDictionary.MergedDictionaries>
               <ui:ThemesDictionary Theme="Dark" />
               <ui:ControlsDictionary />
           </ResourceDictionary.MergedDictionaries>
       </ResourceDictionary>
   </Application.Resources>

2. MainWindow.xaml에 NavigationView:
   <ui:FluentWindow>
       <ui:NavigationView>
           <ui:NavigationView.MenuItems>
               <ui:NavigationViewItem Content="대시보드" Icon="Home24" />
               <ui:NavigationViewItem Content="설정" Icon="Settings24" />
               <ui:NavigationViewItem Content="로그" Icon="Document24" />
           </ui:NavigationView.MenuItems>
       </ui:NavigationView>
   </ui:FluentWindow>

3. Mica 배경 효과:
   Background = ui:UiWindow.GetBackdropType() == BackdropType.Mica

@docs/Windows/design-guide-server.md §10.2의 디자인을 따라줘.
```

**검증 방법**:
- [ ] WPF-UI 라이브러리 정상 로드
- [ ] Fluent Design 테마 적용
- [ ] NavigationView 표시
- [ ] Mica 배경 효과 (Windows 11)

###### 3.1.1.2 MVVM 패턴 및 의존성 주입

**구현 목표**: MVVM 아키텍처 및 DI 컨테이너 구성
**예상 시간**: 50분-1시간

**바이브 코딩 프롬프트**:
```
@src/windows/BridgeOne/ViewModels/MainViewModel.cs 를 생성해:

1. MainViewModel (ObservableObject 상속):
   public partial class MainViewModel : ObservableObject
   {
       [ObservableProperty]
       private string connectionStatus = "연결 대기 중";
       
       [ObservableProperty]
       private int receivedFrames = 0;
       
       [RelayCommand]
       private void StartServer() { /* ... */ }
       
       [RelayCommand]
       private void StopServer() { /* ... */ }
   }

2. @src/windows/BridgeOne/Services/ 에 서비스 인터페이스:
   public interface IHidInputService { /* ... */ }
   public interface IVendorCdcService { /* ... */ }
   public interface ICursorPackService { /* ... */ }

3. @src/windows/BridgeOne/App.xaml.cs 에 DI 컨테이너:
   private IHost _host;
   
   protected override void OnStartup(StartupEventArgs e)
   {
       _host = Host.CreateDefaultBuilder()
           .ConfigureServices((context, services) =>
           {
               services.AddSingleton<IHidInputService, HidInputService>();
               services.AddSingleton<IVendorCdcService, VendorCdcService>();
               services.AddSingleton<MainViewModel>();
               services.AddSingleton<MainWindow>();
           })
           .Build();
       
       var mainWindow = _host.Services.GetRequiredService<MainWindow>();
       mainWindow.Show();
   }

4. MainWindow.xaml.cs에서 ViewModel 주입:
   public MainWindow(MainViewModel viewModel)
   {
       InitializeComponent();
       DataContext = viewModel;
   }

MVVM과 DI로 테스트 가능하고 유지보수하기 쉬운 구조를 만들어.
```

**검증 방법**:
- [ ] ViewModel 바인딩 동작
- [ ] 의존성 주입 정상 동작
- [ ] ObservableProperty 자동 업데이트
- [ ] RelayCommand 실행

###### 3.1.1.3 시스템 트레이 및 로딩 스플래시

**구현 목표**: 시스템 트레이 아이콘 + 2.5초 스플래시
**예상 시간**: 40-50분

**바이브 코딩 프롬프트**:
```
**시스템 트레이** (@src/windows/BridgeOne/Services/TrayIconService.cs):

1. NotifyIcon 생성:
   private NotifyIcon _trayIcon;
   
   public void Initialize()
   {
       _trayIcon = new NotifyIcon
       {
           Icon = new Icon("Resources/BridgeOne-Success.ico"),
           Text = "BridgeOne Server",
           Visible = true
       };
       
       var contextMenu = new ContextMenuStrip();
       contextMenu.Items.Add("열기", null, OnOpen);
       contextMenu.Items.Add("종료", null, OnExit);
       _trayIcon.ContextMenuStrip = contextMenu;
   }

2. 아이콘 상태별 변경:
   - Success: 녹색 (연결됨)
   - Connecting: 주황 (연결 중)
   - Disconnected: 회색 (연결 끊김)
   - Error: 빨강 (오류)

**로딩 스플래시** (@src/windows/BridgeOne/Views/SplashWindow.xaml):

1. 6단계 애니메이션 (총 2.5초):
   - Stage 1 (0-0.4s): 로고 페이드 인
   - Stage 2 (0.4-0.8s): 브릿지 Wipe-in
   - Stage 3 (0.8-1.2s): 별 회전
   - Stage 4 (1.2-1.8s): 방사형 배경
   - Stage 5 (1.8-2.3s): "BridgeOne" 텍스트
   - Stage 6 (2.3-2.5s): 전체 페이드 아웃

2. Storyboard 애니메이션:
   <Storyboard>
       <DoubleAnimation Storyboard.TargetName="Logo"
                        Storyboard.TargetProperty="Opacity"
                        From="0" To="1" Duration="0:0:0.4" />
       <!-- ... 나머지 단계 -->
   </Storyboard>

3. 백그라운드 초기화:
   - 스플래시 표시 중 서비스 초기화
   - 2.5초 후 MainWindow로 전환

@resources/ico/ 의 아이콘들을 사용해줘.
```

**검증 방법**:
- [ ] 시스템 트레이 아이콘 표시
- [ ] 컨텍스트 메뉴 동작
- [ ] 로딩 스플래시 2.5초 재생
- [ ] 6단계 애니메이션 순차 실행

##### 3.1.2 Windows 서비스 및 설정 시스템

###### 3.1.2.1 백그라운드 서비스 등록

**구현 목표**: Windows Service로 등록 및 자동 시작
**예상 시간**: 50분-1시간

**바이브 코딩 프롬프트**:
```
@src/windows/BridgeOne/Services/BridgeOneWindowsService.cs 를 생성해:

1. BackgroundService 상속:
   public class BridgeOneWindowsService : BackgroundService
   {
       protected override async Task ExecuteAsync(CancellationToken stoppingToken)
       {
           while (!stoppingToken.IsCancellationRequested)
           {
               // HID 입력 처리
               // CDC 명령 처리
               await Task.Delay(1, stoppingToken);
           }
       }
   }

2. 서비스 등록 (Program.cs 또는 App.xaml.cs):
   services.AddHostedService<BridgeOneWindowsService>();

3. Windows Service 설치:
   sc.exe create BridgeOneServer binPath="경로\BridgeOne.exe" start=auto

4. 서비스 제어:
   public void StartService() {
       // 서비스 시작 로직
   }
   
   public void StopService() {
       // 정상 종료
   }

5. 자동 시작 설정:
   - 레지스트리: HKEY_CURRENT_USER\Software\Microsoft\Windows\CurrentVersion\Run
   - 또는 시작프로그램 바로가기

개발 중에는 일반 애플리케이션으로 실행하고, 배포 시 서비스로 전환 가능하게 해줘.
```

**검증 방법**:
- [ ] 백그라운드 서비스 실행
- [ ] Windows 시작 시 자동 실행
- [ ] 작업 관리자에서 프로세스 확인
- [ ] 정상 종료 동작

###### 3.1.2.2 설정 저장소 및 백업/복원

**구현 목표**: JSON 기반 설정 관리
**예상 시간**: 30-40분

**바이브 코딩 프롬프트**:
```
@src/windows/BridgeOne/Data/SettingsManager.cs 를 생성해:

1. Settings 데이터 모델:
   public class AppSettings
   {
       public CursorSettings Cursor { get; set; }
       public ConnectionSettings Connection { get; set; }
       public UiSettings Ui { get; set; }
   }

2. 설정 저장/로드:
   private const string SettingsPath = "appsettings.json";
   
   public void Save(AppSettings settings)
   {
       var json = JsonSerializer.Serialize(settings, new JsonSerializerOptions 
       { 
           WriteIndented = true 
       });
       File.WriteAllText(SettingsPath, json);
   }
   
   public AppSettings Load()
   {
       if (File.Exists(SettingsPath))
       {
           var json = File.ReadAllText(SettingsPath);
           return JsonSerializer.Deserialize<AppSettings>(json);
       }
       return GetDefaultSettings();
   }

3. 백업/복원:
   public void Backup()
   {
       var backupPath = $"appsettings.backup.{DateTime.Now:yyyyMMdd}.json";
       File.Copy(SettingsPath, backupPath);
   }
   
   public void Restore(string backupPath)
   {
       File.Copy(backupPath, SettingsPath, overwrite: true);
   }

4. 앱 시작 시 설정 로드:
   LaunchedEffect(Unit) {
       val settings = settingsManager.Load()
       viewModel.ApplySettings(settings)
   }

설정을 JSON 파일로 저장해서 사용자가 직접 편집할 수도 있어.
```

**검증 방법**:
- [ ] 설정 저장 후 파일 생성 확인
- [ ] 앱 재시작 후 설정 복원
- [ ] 백업 파일 생성
- [ ] 백업에서 복원 동작

#### 3.2 커서 팩 연동 시스템

**참조 문서**: `@docs/Windows/technical-specification-server.md` §3.6, `@docs/PRD.md` §5.3.8

##### 3.2.1 자동 커서 팩 감지

###### 3.2.1.1 레지스트리 기반 현재 커서 스킴 감지

**구현 목표**: Windows 레지스트리에서 현재 사용 중인 커서 스킴 읽기
**예상 시간**: 40-50분

**바이브 코딩 프롬프트**:
```
@src/windows/BridgeOne/Services/CursorPackDetector.cs 를 생성해:

1. 레지스트리 경로 상수:
   private const string CursorRegKey = @"Control Panel\Cursors";
   private const string SchemeRegKey = @"Control Panel\Cursors\Schemes";

2. 현재 커서 스킴 감지:
   public CursorScheme DetectCurrentScheme()
   {
       using var key = Registry.CurrentUser.OpenSubKey(CursorRegKey);
       
       var scheme = new CursorScheme
       {
           Arrow = key.GetValue("Arrow") as string,
           Help = key.GetValue("Help") as string,
           AppStarting = key.GetValue("AppStarting") as string,
           Wait = key.GetValue("Wait") as string,
           // ... 13+5개 커서 타입
       };
       
       return scheme;
   }

3. 커서 파일 경로 추출:
   - 레지스트리 값에서 .cur/.ani 파일 경로 파싱
   - 환경 변수 확장 (예: %SystemRoot%)
   - 파일 존재 여부 확인

4. 스킴 이름 감지:
   public string GetSchemeSourceName()
   {
       // Install.inf 파일 위치 추론
       var cursorPath = Path.GetDirectoryName(scheme.Arrow);
       var infPath = Path.Combine(cursorPath, "Install.inf");
       
       if (File.Exists(infPath))
       {
           return ParseSchemeNameFromInf(infPath);
       }
       
       return "Unknown Scheme";
   }

레지스트리 읽기로 사용자가 현재 어떤 커서를 쓰는지 자동으로 감지해.
```

**검증 방법**:
- [ ] 현재 커서 스킴 감지 성공
- [ ] 13+5개 커서 경로 모두 읽기
- [ ] 파일 존재 여부 확인
- [ ] 스킴 이름 추출

###### 3.2.1.2 Install.inf 파싱 및 커서 타입 매핑

**구현 목표**: Install.inf에서 메타데이터 추출 및 타입 매핑
**예상 시간**: 50분-1시간

**바이브 코딩 프롬프트**:
```
@src/windows/BridgeOne/Services/InstallInfParser.cs 를 생성해:

1. Install.inf 파싱:
   public CursorPackMetadata ParseInf(string infPath)
   {
       var lines = File.ReadAllLines(infPath);
       var metadata = new CursorPackMetadata();
       
       foreach (var line in lines)
       {
           if (line.StartsWith("SchemeName="))
           {
               metadata.Name = line.Substring(11).Trim();
           }
           // ... 다른 메타데이터
       }
       
       return metadata;
   }

2. 13+5개 커서 타입 정의:
   public enum CursorType
   {
       // Primary 13
       Arrow, Help, AppStarting, Wait, Crosshair,
       IBeam, NWPen, No, SizeNS, SizeWE,
       SizeNWSE, SizeNESW, SizeAll,
       
       // Extended 5
       Hand, Up, Move, Alternate, Link
   }

3. 파일명 패턴 매핑:
   private Dictionary<string, CursorType> filePatterns = new()
   {
       { "arrow", CursorType.Arrow },
       { "help", CursorType.Help },
       { "appstarting", CursorType.AppStarting },
       { "wait", CursorType.Wait },
       // ... 나머지
   };
   
   public CursorType MapFileToCursorType(string filename)
   {
       var lower = filename.ToLower();
       foreach (var pattern in filePatterns)
       {
           if (lower.Contains(pattern.Key))
               return pattern.Value;
       }
       return CursorType.Unknown;
   }

4. 품질 평가:
   public CursorPackQuality EvaluateQuality(CursorScheme scheme)
   {
       int foundCount = scheme.GetFoundCursorsCount();
       
       return foundCount switch
       {
           >= 18 => CursorPackQuality.Excellent,  // 13+5 전체
           >= 13 => CursorPackQuality.Good,       // Primary 전체
           >= 8 => CursorPackQuality.Basic,       // 기본 커서들
           _ => CursorPackQuality.Incomplete
       };
   }

Install.inf 파싱으로 커서 팩의 상세 정보를 알 수 있어.
```

**검증 방법**:
- [ ] Install.inf 파싱 성공
- [ ] 스킴 이름 추출
- [ ] 18개 커서 타입 매핑
- [ ] 품질 평가 정확

##### 3.2.2 커서 팩 관리

###### 3.2.2.1 실시간 스킴 변경 감지

**구현 목표**: 레지스트리 변경 이벤트로 커서 스킴 자동 갱신
**예상 시간**: 40-50분

**바이브 코딩 프롬프트**:
```
@src/windows/BridgeOne/Services/CursorPackMonitor.cs 를 생성해:

1. 레지스트리 변경 감지:
   private RegistryKey _cursorKey;
   private ManualResetEvent _regChangeEvent = new ManualResetEvent(false);
   
   public void StartMonitoring()
   {
       _cursorKey = Registry.CurrentUser.OpenSubKey(CursorRegKey, false);
       
       Task.Run(() =>
       {
           while (true)
           {
               RegNotifyChangeKeyValue(_cursorKey.Handle, ...);
               _regChangeEvent.WaitOne();
               
               // 스킴 변경 감지됨
               OnCursorSchemeChanged();
               _regChangeEvent.Reset();
           }
       });
   }

2. 스킴 변경 처리:
   private void OnCursorSchemeChanged()
   {
       var newScheme = cursorPackDetector.DetectCurrentScheme();
       
       if (newScheme != currentScheme)
       {
           currentScheme = newScheme;
           CursorSchemeChanged?.Invoke(this, newScheme);
           
           // Android에 변경 알림
           vendorCdc.SendCommand("CURSOR_PACK_UPDATE", newScheme);
       }
   }

3. 파일 일관성 분석:
   public bool AnalyzeConsistency(CursorScheme scheme)
   {
       var directories = scheme.GetAllCursorPaths()
           .Select(Path.GetDirectoryName)
           .Distinct()
           .ToList();
       
       // 80% 이상이 동일한 경로면 일관적
       var mostCommonDir = directories
           .GroupBy(d => d)
           .OrderByDescending(g => g.Count())
           .First();
       
       var consistency = mostCommonDir.Count() / (float)directories.Count;
       return consistency >= 0.8f;
   }

4. 이벤트:
   public event EventHandler<CursorScheme> CursorSchemeChanged;

사용자가 Windows 설정에서 커서를 변경하면 자동으로 감지돼.
```

**검증 방법**:
- [ ] 레지스트리 변경 감지
- [ ] 커서 스킴 변경 시 이벤트 발생
- [ ] 파일 일관성 분석 정확
- [ ] Android로 변경 알림 전송

#### 3.3 멀티 커서 관리 시스템

**참조 문서**: `@docs/Android/component-touchpad.md` §2.2.6, `@docs/Windows/design-guide-server.md` §6.2

##### 3.3.1 가상 커서 렌더링

###### 3.3.1.1 전체화면 투명 오버레이 윈도우

**구현 목표**: WS_EX_LAYERED, WS_EX_TRANSPARENT 투명 윈도우
**예상 시간**: 50분-1시간
**참조**: `@docs/Windows/design-guide-server.md` §6.2.1

**바이브 코딩 프롬프트**:
```
@src/windows/BridgeOne/Overlays/VirtualCursorOverlay.cs 를 생성해:

1. 투명 오버레이 윈도우 생성:
   public class VirtualCursorOverlay : Window
   {
       protected override void OnSourceInitialized(EventArgs e)
       {
           base.OnSourceInitialized(e);
           
           var hwnd = new WindowInteropHelper(this).Handle;
           
           // 투명 레이어드 윈도우
           int exStyle = GetWindowLong(hwnd, GWL_EXSTYLE);
           exStyle |= WS_EX_LAYERED | WS_EX_TRANSPARENT | WS_EX_TOPMOST;
           SetWindowLong(hwnd, GWL_EXSTYLE, exStyle);
           
           // 전체화면 크기
           Width = SystemParameters.VirtualScreenWidth;
           Height = SystemParameters.VirtualScreenHeight;
           Left = SystemParameters.VirtualScreenLeft;
           Top = SystemParameters.VirtualScreenTop;
       }
   }

2. 투명도 설정:
   SetLayeredWindowAttributes(hwnd, 0, 255, LWA_ALPHA);

3. 이벤트 통과 처리:
   - WS_EX_TRANSPARENT 플래그로 모든 마우스/키보드 이벤트 통과
   - 하위 윈도우 상호작용 보장

4. AllowsTransparency:
   <Window AllowsTransparency="True" 
           Background="Transparent"
           WindowStyle="None" />

5. XAML Canvas:
   <Canvas x:Name="CursorCanvas" />

전체화면 투명 윈도우로 가상 커서를 어디든 그릴 수 있어.
```

**검증 방법**:
- [ ] 투명 윈도우 생성
- [ ] 전체 화면 크기
- [ ] 이벤트 통과 (하위 윈도우 클릭 가능)
- [ ] 항상 최상위 (TOPMOST)

###### 3.3.1.2 6가지 가상 커서 표시 타입

**구현 목표**: 외곽선, 반투명, 틴트, 글로우, 크기, 점선
**예상 시간**: 1시간-1시간 20분

**바이브 코딩 프롬프트**:
```
@src/windows/BridgeOne/Rendering/VirtualCursorRenderer.cs 를 생성해:

1. VirtualCursorStyle enum:
   public enum VirtualCursorStyle
   {
       Outline,         // 외곽선
       SemiTransparent, // 반투명 오버레이
       ColorTint,       // 색상 틴트
       Glow,            // 글로우 효과
       Scaled,          // 크기 조절 (1.2x)
       Dashed           // 점선 테두리
   }

2. 각 스타일별 렌더링:
   public void RenderCursor(DrawingContext dc, Point position, BitmapSource cursorImage, VirtualCursorStyle style)
   {
       switch (style)
       {
           case Outline:
               // 외곽선 그리기
               dc.DrawImage(cursorImage, new Rect(position, cursorImage.Size));
               var pen = new Pen(Brushes.Cyan, 3);
               dc.DrawRectangle(null, pen, new Rect(position, cursorImage.Size));
               break;
               
           case SemiTransparent:
               // 반투명 (opacity 0.5)
               dc.PushOpacity(0.5);
               dc.DrawImage(cursorImage, new Rect(position, cursorImage.Size));
               dc.Pop();
               break;
               
           case ColorTint:
               // 색상 틴트 (보라색)
               var tintedImage = ApplyColorTint(cursorImage, Colors.Purple);
               dc.DrawImage(tintedImage, new Rect(position, tintedImage.Size));
               break;
               
           case Glow:
               // 글로우 효과 (DropShadowEffect)
               var glowEffect = new DropShadowEffect
               {
                   Color = Colors.Cyan,
                   BlurRadius = 20,
                   ShadowDepth = 0,
                   Opacity = 0.8
               };
               // ... 효과 적용
               break;
               
           case Scaled:
               // 1.2배 크기
               var scaledSize = new Size(
                   cursorImage.Width * 1.2,
                   cursorImage.Height * 1.2
               );
               dc.DrawImage(cursorImage, new Rect(position, scaledSize));
               break;
               
           case Dashed:
               // 점선 테두리
               var dashedPen = new Pen(Brushes.Yellow, 2)
               {
                   DashStyle = DashStyles.Dash
               };
               dc.DrawRectangle(null, dashedPen, new Rect(position, cursorImage.Size));
               break;
       }
   }

3. GPU 가속 렌더링:
   RenderOptions.SetBitmapScalingMode(this, BitmapScalingMode.HighQuality);
   RenderOptions.SetEdgeMode(this, EdgeMode.Aliased);

4. 60fps 목표:
   CompositionTarget.Rendering += OnRendering;
   // 16ms마다 렌더링

다양한 스타일로 가상 커서를 구분하기 쉽게 만들어.
```

**검증 방법**:
- [ ] 6가지 스타일 모두 렌더링
- [ ] 60fps 유지
- [ ] GPU 가속 활성화
- [ ] 스타일 전환 부드러움

##### 3.3.2 텔레포트 기능

###### 3.3.2.1 커서 전환 신호 수신 및 이동

**구현 목표**: Android 신호로 실제 커서 위치 50ms 내 이동
**예상 시간**: 40-50분

**바이브 코딩 프롬프트**:
```
@src/windows/BridgeOne/Services/CursorTeleportService.cs 를 생성해:

1. 가상 커서 위치 저장:
   private Dictionary<int, Point> virtualCursorPositions = new();
   // Key: 커서 ID (0=메인, 1-5=가상)

2. Android 전환 신호 수신 (CDC):
   public void OnMultiCursorSwitchRequest(int targetCursorId)
   {
       if (!virtualCursorPositions.ContainsKey(targetCursorId))
           return;
       
       var targetPosition = virtualCursorPositions[targetCursorId];
       TeleportCursor(targetPosition);
   }

3. 커서 텔레포트 (SetCursorPos):
   private void TeleportCursor(Point target)
   {
       // 현재 커서 위치 저장
       GetCursorPos(out var currentPos);
       SaveCursorPosition(currentCursorId, currentPos);
       
       // 새 위치로 이동 (50ms 이내)
       SetCursorPos((int)target.X, (int)target.Y);
       
       // 애니메이션 시작
       StartTeleportAnimation();
   }

4. 위치 저장/복원:
   public void SaveCursorPosition(int cursorId, Point position)
   {
       virtualCursorPositions[cursorId] = position;
   }

5. 16ms 주기 가상 커서 위치 업데이트:
   private void UpdateVirtualCursorPositions()
   {
       foreach (var kvp in virtualCursorPositions)
       {
           // 가상 커서 렌더링
           virtualCursorOverlay.UpdateCursor(kvp.Key, kvp.Value);
       }
   }

Android에서 커서 전환 버튼을 누르면 즉시 해당 커서 위치로 이동해.
```

**검증 방법**:
- [ ] Android 신호 수신
- [ ] 커서 위치 50ms 내 이동
- [ ] 이전 위치 저장
- [ ] 가상 커서 위치 업데이트

###### 3.3.2.2 3단계 텔레포트 애니메이션

**구현 목표**: 페이드 아웃 → 글로우 → 페이드 인 (150ms)
**예상 시간**: 50분-1시간

**바이브 코딩 프롬프트**:
```
@src/windows/BridgeOne/Animations/TeleportAnimation.cs 를 생성해:

1. 3단계 애니메이션 정의:
   public async Task PlayTeleportAnimation(Point from, Point to)
   {
       // Stage 1: 페이드 아웃 (50ms)
       await AnimateFadeOut(from, duration: 50);
       
       // Stage 2: 글로우 (50ms)
       await AnimateGlow(from, to, duration: 50);
       
       // Stage 3: 페이드 인 (50ms)
       await AnimateFadeIn(to, duration: 50);
   }

2. 페이드 아웃 구현:
   private async Task AnimateFadeOut(Point position, int duration)
   {
       var storyboard = new Storyboard();
       var opacityAnim = new DoubleAnimation
       {
           From = 1.0,
           To = 0.0,
           Duration = TimeSpan.FromMilliseconds(duration),
           EasingFunction = new QuadraticEase()
       };
       
       Storyboard.SetTarget(opacityAnim, cursorElement);
       Storyboard.SetTargetProperty(opacityAnim, new PropertyPath("Opacity"));
       storyboard.Children.Add(opacityAnim);
       
       storyboard.Begin();
       await Task.Delay(duration);
   }

3. 글로우 애니메이션:
   private async Task AnimateGlow(Point from, Point to, int duration)
   {
       // from 위치에서 to 위치로 파티클 효과
       for (int i = 0; i < 10; i++)
       {
           var particle = CreateGlowParticle();
           var t = i / 10f;
           var position = Point.Lerp(from, to, t);
           
           AnimateParticle(particle, position);
           await Task.Delay(duration / 10);
       }
   }

4. 페이드 인 구현:
   private async Task AnimateFadeIn(Point position, int duration)
   {
       // 페이드 아웃 역순
       var opacityAnim = new DoubleAnimation
       {
           From = 0.0,
           To = 1.0,
           Duration = TimeSpan.FromMilliseconds(duration)
       };
       // ...
   }

5. 총 150ms 타이밍 검증:
   Stopwatch.StartNew();
   await PlayTeleportAnimation(from, to);
   var elapsed = stopwatch.ElapsedMilliseconds;
   Debug.Assert(elapsed <= 155);  // 150ms + 5ms 여유

텔레포트 애니메이션으로 커서 전환을 시각적으로 표현해.
```

**검증 방법**:
- [ ] 3단계 애니메이션 순차 실행
- [ ] 총 150ms 소요
- [ ] 부드러운 전환
- [ ] 글로우 효과 표시

##### 3.3.3 실시간 커서 상태 동기화

###### 3.3.3.1 커서 상태 모니터링 및 이미지 업데이트

**구현 목표**: GetCursorInfo로 실시간 커서 상태 추적
**예상 시간**: 40-50분

**바이브 코딩 프롬프트**:
```
@src/windows/BridgeOne/Services/CursorStateMonitor.cs 를 생성해:

1. 커서 상태 모니터링 (16ms 주기):
   private DispatcherTimer _monitorTimer;
   
   public void StartMonitoring()
   {
       _monitorTimer = new DispatcherTimer
       {
           Interval = TimeSpan.FromMilliseconds(16)  // 60Hz
       };
       
       _monitorTimer.Tick += (s, e) =>
       {
           UpdateCursorState();
       };
       
       _monitorTimer.Start();
   }

2. GetCursorInfo 호출:
   [DllImport("user32.dll")]
   static extern bool GetCursorInfo(out CURSORINFO pci);
   
   private void UpdateCursorState()
   {
       CURSORINFO cursorInfo = new CURSORINFO();
       cursorInfo.cbSize = Marshal.SizeOf(cursorInfo);
       
       if (GetCursorInfo(out cursorInfo))
       {
           var newState = MapHandleToCursorType(cursorInfo.hCursor);
           
           if (newState != currentCursorState)
           {
               currentCursorState = newState;
               OnCursorStateChanged(newState);
           }
       }
   }

3. 커서 핸들 → 타입 매핑:
   private CursorType MapHandleToCursorType(IntPtr hCursor)
   {
       // 시스템 커서 핸들과 타입 매핑
       // OCR_NORMAL, OCR_HAND, OCR_IBEAM 등
       
       if (hCursor == LoadCursor(IntPtr.Zero, IDC_ARROW))
           return CursorType.Arrow;
       else if (hCursor == LoadCursor(IntPtr.Zero, IDC_HAND))
           return CursorType.Hand;
       // ... 나머지
       
       return CursorType.Unknown;
   }

4. 가상 커서 이미지 업데이트:
   private void OnCursorStateChanged(CursorType newType)
   {
       var cursorImage = cursorPackService.GetCursorImage(newType);
       
       foreach (var virtualCursor in virtualCursors)
       {
           virtualCursor.UpdateImage(cursorImage);
       }
   }

16ms 주기로 실제 커서 상태를 감지해서 가상 커서도 동기화해.
```

**검증 방법**:
- [ ] 16ms 주기 모니터링
- [ ] 커서 상태 변화 감지 (Arrow, Hand, IBeam 등)
- [ ] 가상 커서 이미지 자동 업데이트
- [ ] CPU 사용률 <5%

#### 3.4 고급 통신 프로토콜

##### 3.4.1 Vendor CDC 고급 명령

###### 3.4.1.1 MULTI_CURSOR_SWITCH 명령 구현

**구현 목표**: Android에서 Windows로 커서 전환 요청
**예상 시간**: 40-50분

**바이브 코딩 프롬프트**:
```
Android와 Windows에 멀티 커서 전환 명령을 구현해:

**Android 측** (@src/android/.../components/touchpad/CursorSwitchButton.kt):

1. 커서 전환 버튼 UI:
   @Composable
   fun CursorSwitchButton(
       cursorId: Int,
       currentCursorId: Int,
       onSwitch: (Int) -> Unit
   ) {
       Button(
           onClick = { onSwitch(cursorId) },
           colors = if (cursorId == currentCursorId) 
               ButtonDefaults.filledTonalButtonColors() 
               else ButtonDefaults.outlinedButtonColors()
       ) {
           Text("커서 $cursorId")
       }
   }

2. CDC 명령 전송:
   fun switchToMultiCursor(targetCursorId: Int) {
       val command = JsonObject().apply {
           addProperty("cmd", "MULTI_CURSOR_SWITCH")
           addProperty("targetCursorId", targetCursorId)
           addProperty("timestamp", System.currentTimeMillis())
       }
       
       vendorCdcManager.sendJsonCommand(command)
   }

**Windows 측** (@src/windows/BridgeOne/Services/MultiCursorCommandHandler.cs):

1. 명령 처리:
   public void HandleMultiCursorSwitch(JsonElement data)
   {
       var targetCursorId = data.GetProperty("targetCursorId").GetInt32();
       
       // CursorTeleportService 호출
       cursorTeleportService.TeleportToCursor(targetCursorId);
       
       // 응답 전송
       SendResponse("MULTI_CURSOR_SWITCH", new { status = "ok" });
   }

2. 명령 라우팅:
   switch (command)
   {
       case "MULTI_CURSOR_SWITCH":
           HandleMultiCursorSwitch(data);
           break;
       // ...
   }

Android에서 버튼 클릭 → CDC 명령 → Windows 커서 텔레포트.
```

**검증 방법**:
- [ ] 커서 전환 버튼 클릭 → CDC 명령 전송
- [ ] Windows에서 명령 수신
- [ ] 커서 텔레포트 실행
- [ ] 응답 수신

###### 3.4.1.2 UI_FORCE_ENABLE 명령 구현

**구현 목표**: Windows에서 Android UI 강제 활성화 요청
**예상 시간**: 30-40분

**바이브 코딩 프롬프트**:
```
**Windows 측** (@src/windows/BridgeOne/Commands/ForceEnableCommand.cs):

1. 강제 활성화 요청:
   public void RequestForceEnableAll()
   {
       var command = new
       {
           cmd = "UI_FORCE_ENABLE_ALL_TOUCHABLES_REQUEST",
           duration = 5000  // 5초간
       };
       
       vendorCdcManager.SendJsonCommand(command);
   }

2. UI 버튼:
   <Button Content="모든 UI 활성화"
           Command="{Binding ForceEnableCommand}" />

**Android 측** (@src/android/.../usb/CommandHandler.kt):

1. 명령 수신 처리:
   fun handleForceEnableRequest(duration: Long) {
       componentStateManager.forceEnableAll = true
       
       // duration 후 자동 해제
       viewModelScope.launch {
           delay(duration)
           componentStateManager.forceEnableAll = false
       }
       
       // 토스트 알림
       toastManager.show(
           "모든 UI 5초간 활성화됨",
           ToastCategory.WARNING
       )
   }

2. 명령 라우팅:
   when (cmd) {
       "UI_FORCE_ENABLE_ALL_TOUCHABLES_REQUEST" -> 
           handleForceEnableRequest(data.duration)
   }

Windows에서 디버깅 시 모든 UI를 강제로 활성화할 수 있어.
```

**검증 방법**:
- [ ] Windows 버튼 클릭 → CDC 명령 전송
- [ ] Android에서 명령 수신
- [ ] 모든 UI 5초간 활성화
- [ ] 5초 후 자동 해제

###### 3.4.1.3 Android ↔ Windows 설정 동기화

**구현 목표**: 양방향 설정 동기화 시스템
**예상 시간**: 50분-1시간

**바이브 코딩 프롬프트**:
```
양방향 설정 동기화 시스템을 구현해:

**공통 설정 데이터 구조**:

1. JSON 스키마 정의:
   {
       "cursorSettings": {
           "sensitivity": 1.0,
           "acceleration": true
       },
       "multiCursorSettings": {
           "maxCursors": 5,
           "defaultStyle": "Outline"
       },
       "uiSettings": {
           "hapticEnabled": true,
           "theme": "Dark"
       }
   }

**Android 측**:

1. 설정 변경 시 Windows로 전송:
   fun onSettingsChanged(settings: AppSettings) {
       val command = JsonObject().apply {
           addProperty("cmd", "SYNC_SETTINGS")
           add("settings", settings.toJson())
       }
       vendorCdcManager.sendJsonCommand(command)
   }

2. Windows 설정 수신:
   fun handleSyncSettings(settings: JsonElement) {
       val newSettings = AppSettings.fromJson(settings)
       preferencesManager.saveSettings(newSettings)
       viewModel.applySettings(newSettings)
   }

**Windows 측**:

1. Android 설정 수신 및 적용:
   public void HandleSyncSettings(JsonElement settings)
   {
       var appSettings = JsonSerializer.Deserialize<AppSettings>(settings);
       settingsManager.Save(appSettings);
       
       // 설정 적용
       ApplySettings(appSettings);
       
       // 확인 응답
       SendResponse("SYNC_SETTINGS", new { status = "ok" });
   }

2. Windows 설정 변경 시 Android로 전송:
   public void OnSettingsChanged(AppSettings settings)
   {
       var command = new
       {
           cmd = "SYNC_SETTINGS",
           settings = settings
       };
       
       vendorCdcManager.SendJsonCommand(command);
   }

3. 충돌 해결:
   - 타임스탬프 기반 최신 설정 우선
   - 또는 사용자에게 선택 프롬프트

양방향 동기화로 Android와 Windows 설정이 항상 일치해.
```

**검증 방법**:
- [ ] Android 설정 변경 → Windows 동기화
- [ ] Windows 설정 변경 → Android 동기화
- [ ] 충돌 해결 동작
- [ ] 설정 일관성 유지

**Phase 3 완료 조건**: 
- ✅ 멀티 커서 텔레포트 150ms 완료
- ✅ 커서 팩 자동 감지 및 연동
- ✅ WPF GUI 완전 구현
- ✅ CDC 고급 명령 3개 구현

---

### Phase 4: 안정성 및 품질 보증

**목표**: 전체 시스템 통합 안정성 확보 및 사용자 경험 완성
**예상 기간**: 2-3주
**완료 기준**: 모든 KPI 달성, 접근성 준수, 배포 준비 완료

#### 4.1 통합 테스트 및 품질 검증

**참조 문서**: `@docs/Android/technical-specification-app.md` §2.8, `@docs/PRD.md` §7

##### 4.1.1 End-to-End 시나리오 테스트

###### 4.1.1.1 전체 사용자 시나리오 테스트 스크립트

**구현 목표**: 초기 연결부터 고급 기능까지 체계적 테스트
**예상 시간**: 2시간 (수동 테스트)

**테스트 프롬프트**:
```
다음 E2E 시나리오를 순서대로 테스트해:

**시나리오 1: 초기 연결 및 Essential 모드 (15분)**
1. Android 앱 설치 및 실행
2. ESP32-S3 연결 (USB OTG)
3. USB 권한 승인
4. Essential 모드 진입 확인
5. Boot Keyboard로 텍스트 입력
6. 터치패드로 커서 이동

**시나리오 2: Windows 서버 연결 및 Standard 모드 (15분)**
1. Windows 서버 실행
2. ESP32-S3 HID 장치 인식 확인
3. Vendor CDC 핸드셰이크 시작
4. Standard 모드 전환 확인
5. 4개 페이지 네비게이션 테스트
6. 모든 기능 활성화 확인

**시나리오 3: 터치패드 고급 기능 (30분)**
1. 데드존 보상 테스트 (작은 떨림 무시)
2. 직각 이동 모드 (수평/수직선 그리기)
3. 클릭 판정 (탭 vs 드래그)
4. 일반 스크롤 (50dp 단위, 햅틱)
5. 무한 스크롤 (관성, 가이드라인)
6. 모드 전환 (LEFT/RIGHT 클릭)

**시나리오 4: UI 컴포넌트 (30분)**
1. KeyboardKeyButton Sticky Hold
2. ShortcutButton (Ctrl+C, Ctrl+V)
3. ContainerButton (F1-F12 팝업)
4. DPad 8방향 + Sticky Hold
5. 상태 토스트 표시
6. 페이지 전환 애니메이션

**시나리오 5: 오류 복구 (15분)**
1. USB 분리 → 자동 재연결
2. Windows 서버 종료 → Essential 모드
3. Keep-alive 타임아웃 → 재연결
4. 권한 거부 → 재요청

**시나리오 6: 실제 사용 케이스 (15분)**
1. 웹 브라우징 (스크롤, 클릭, 뒤로가기)
2. 문서 편집 (MS Word)
3. 게임 (Minecraft Page 3)
4. 그림 그리기 (Paint)

각 시나리오마다 체크리스트를 작성하고 통과/실패를 기록해줘.

**참고**: Phase 1.2.2.3에서 구현한 `test_command_system.py` 스크립트로 일부 시나리오 자동화 가능:
- 초기 연결 검증 (시나리오 1 일부)
- 명령 처리 시스템 테스트 (시나리오 2 일부)
- 펌웨어 업데이트 후 회귀 테스트용
```

**검증 방법**:
- [ ] 6개 시나리오 모두 통과
- [ ] 각 시나리오 재현 가능
- [ ] 오류 없이 완료
- [ ] 사용자 경험 만족스러움
- [ ] **선택사항**: `test_command_system.py`로 일부 시나리오 자동화 테스트

###### 4.1.1.2 멀티 커서 고급 기능 테스트

**구현 목표**: 멀티 커서 모든 기능 검증
**예상 시간**: 1시간 (수동 테스트)

**테스트 프롬프트**:
```
멀티 커서 기능 전체 테스트:

**테스트 1: 가상 커서 표시 (15분)**
1. Standard 모드에서 멀티 커서 활성화
2. 최대 5개 가상 커서 생성
3. 각 커서별 다른 표시 스타일:
   - 커서 1: 외곽선 (Outline)
   - 커서 2: 반투명 (SemiTransparent)
   - 커서 3: 색상 틴트 (ColorTint)
   - 커서 4: 글로우 (Glow)
   - 커서 5: 크기 조절 (Scaled)
4. 각 스타일 육안 확인
5. 60fps 렌더링 확인

**테스트 2: 텔레포트 기능 (20분)**
1. 실제 커서를 화면 좌상단에 위치
2. 가상 커서 1을 화면 우하단에 배치
3. Android에서 커서 1로 전환 버튼 클릭
4. 텔레포트 애니메이션 관찰:
   - 페이드 아웃 (50ms)
   - 글로우 트레일 (50ms)
   - 페이드 인 (50ms)
5. 실제 커서가 우하단으로 이동 확인
6. 총 150ms 내 완료 확인

**테스트 3: 커서 상태 동기화 (15분)**
1. 링크 위에 커서 → Hand 커서로 변경 확인
2. 가상 커서들도 Hand 이미지로 동기화 확인
3. 텍스트 위 → IBeam 커서 변경
4. 16ms 주기 업데이트 확인 (로그)

**테스트 4: 커서 팩 연동 (10분)**
1. Windows 설정에서 커서 스킴 변경
2. BridgeOne이 자동 감지 확인
3. 가상 커서 이미지 자동 업데이트
4. 새 커서 팩 품질 평가 확인
```

**검증 방법**:
- [ ] 6가지 표시 스타일 모두 동작
- [ ] 텔레포트 150ms 내 완료
- [ ] 커서 상태 실시간 동기화
- [ ] 커서 팩 자동 감지 및 적용

###### 4.1.1.3 24시간 연속 사용 안정성 테스트

**구현 목표**: 장시간 안정성 검증
**예상 시간**: 24시간 + 1시간 모니터링

**테스트 프롬프트**:
```
24시간 연속 사용 안정성 테스트:

**준비**:
1. Android 충전기 연결 (배터리 방전 방지)
2. Windows 서버 자동 시작 설정
3. 모니터링 도구 설정:
   - Android: adb logcat 자동 저장
   - Windows: Serilog 파일 로깅
   - ESP32-S3: 시리얼 모니터 로그

**테스트 진행 (24시간)**:
1. 시작 시간 기록
2. 주기적 조작 (2시간마다 10분씩):
   - 터치패드 조작
   - 키보드 입력
   - 페이지 전환
3. 나머지 시간: 대기 상태 유지

**모니터링 항목 (매 4시간)**:
- [ ] 앱 크래시 없음
- [ ] USB 연결 유지
- [ ] Keep-alive 정상 동작
- [ ] 메모리 사용량 (Android Studio Profiler)
  * 시작: ___MB
  * 4h: ___MB
  * 8h: ___MB
  * 12h: ___MB
  * 16h: ___MB
  * 20h: ___MB
  * 24h: ___MB
- [ ] Windows 서버 메모리
- [ ] ESP32-S3 메모리 (FreeRTOS 힙)

**종료 후 분석**:
1. 로그 파일 검토
2. 오류/경고 카운트
3. 메모리 누수 여부 (선형 증가 확인)
4. 성능 저하 여부
5. 재연결 횟수

**합격 기준**:
- 크래시 0회
- 메모리 증가 <10%
- 재연결 성공률 100%
```

**검증 방법**:
- [ ] 24시간 연속 동작
- [ ] 메모리 누수 없음 (<10% 증가)
- [ ] 크래시 0회
- [ ] 성능 저하 없음

##### 4.1.2 성능 벤치마크 검증

###### 4.1.2.1 디바이스 등급별 전송 빈도 벤치마크

**구현 목표**: 3가지 등급 디바이스에서 FPS 검증
**예상 시간**: 1시간 (3개 디바이스 테스트)

**테스트 프롬프트**:
```
다양한 디바이스에서 전송 빈도 테스트:

**High 등급 디바이스** (예: Pixel 7, Galaxy S23):
- 목표 FPS: 167Hz
- 테스트: 1분간 연속 터치 입력
- 측정:
  * 평균 FPS: ___Hz
  * 최소 FPS: ___Hz
  * 최대 FPS: ___Hz
  * 프레임 드롭: ___회
- 합격: 평균 ≥150Hz, 최소 ≥140Hz

**Medium 등급 디바이스** (예: Pixel 5, Galaxy A52):
- 목표 FPS: 120Hz
- 측정 (동일)
- 합격: 평균 ≥110Hz, 최소 ≥100Hz

**Low 등급 디바이스** (예: 저사양 Android 10):
- 목표 FPS: 83Hz
- 측정 (동일)
- 합격: 평균 ≥80Hz, 최소 ≥75Hz

**자동 등급 감지 테스트**:
- 각 디바이스에서 앱 실행
- 10초 후 자동 감지된 등급 확인
- UI에 등급 표시 확인

모든 등급에서 안정적으로 목표 FPS를 달성해야 해.
```

**검증 방법**:
- [ ] High: 평균 ≥150Hz
- [ ] Medium: 평균 ≥110Hz
- [ ] Low: 평균 ≥80Hz
- [ ] 자동 등급 감지 정확

###### 4.1.2.2 E2E 지연시간 최종 검증

**구현 목표**: 전체 구간 지연시간 50ms 이하 달성 확인
**예상 시간**: 30-40분

**테스트 프롬프트**:
```
E2E 지연시간 정밀 측정:

**측정 도구 준비**:
1. Android: 터치 이벤트 타임스탬프 로깅
2. ESP32-S3: UART/HID 타임스탬프
3. Windows: HID 수신 타임스탬프

**100회 측정 테스트**:
1. Android 터치패드를 100회 터치 (1초 간격)
2. 각 터치마다 3개 구간 지연시간 기록:
   - Android 구간: 터치 → USB 전송
   - Board 구간: UART 수신 → HID 전송
   - Windows 구간: HID 수신 → 커서 적용

**통계 계산**:
- Android 구간:
  * 평균: ___ms (목표: 5-8ms)
  * 최대: ___ms
- Board 구간:
  * 평균: ___ms (목표: ≤5ms)
  * 최대: ___ms
- Windows 구간:
  * 평균: ___ms (목표: 10-15ms)
  * 최대: ___ms
- **전체 E2E**:
  * 평균: ___ms (목표: <50ms)
  * 최대: ___ms
  * 95 percentile: ___ms

**합격 기준**:
- 평균 E2E <50ms
- 95 percentile <60ms
- 최대 <100ms
```

**검증 방법**:
- [ ] 평균 E2E <50ms
- [ ] 95 percentile <60ms
- [ ] 각 구간 목표치 달성
- [ ] 100회 측정 완료

###### 4.1.2.3 렌더링 및 메모리 벤치마크

**구현 목표**: 60fps 유지 및 메모리 최적화 검증
**예상 시간**: 40-50분

**테스트 프롬프트**:
```
**렌더링 성능 테스트**:

1. Android GPU Profiler 활용:
   - Settings → Developer options → GPU rendering
   - Profile GPU rendering 활성화
   - 녹색 수평선(16ms) 이하 유지 확인

2. 1분간 연속 조작:
   - 터치패드 드래그
   - 페이지 전환
   - 버튼 클릭
   - 스크롤

3. 프레임 타임 측정:
   - 평균 프레임 시간: ___ms (목표: <16ms)
   - 프레임 드롭 횟수: ___회 (목표: 0)
   - Jank (>16ms) 비율: ___% (목표: <1%)

**메모리 벤치마크**:

1. Android Studio Profiler:
   - Memory 탭 열기
   - 10분간 연속 조작
   - 메모리 프로파일 캡처

2. 측정 항목:
   - 시작 메모리: ___MB
   - 10분 후: ___MB
   - 증가량: ___MB (목표: <10MB)
   - GC 빈도: ___회/분 (목표: <1회/분)
   - 풀 히트율: ___% (목표: >90%)

3. 힙 덤프 분석:
   - Leak Canary로 메모리 누수 검사
   - 누수 0건 확인

**합격 기준**:
- 60fps 안정 유지 (프레임 드롭 0)
- 메모리 증가 <10MB
- GC <1회/분
```

**검증 방법**:
- [ ] 평균 프레임 시간 <16ms
- [ ] 프레임 드롭 0회
- [ ] 메모리 증가 <10MB
- [ ] GC <1회/분
- [ ] 메모리 누수 없음

#### 4.2 접근성 및 사용성

**참조 문서**: `@docs/Android/design-guide-app.md` §7, `@docs/Windows/design-guide-server.md` §8

##### 4.2.1 Android 앱 접근성

###### 4.2.1.1 TalkBack 지원 구현

**구현 목표**: 스크린 리더 완전 지원
**예상 시간**: 50분-1시간
**참조**: `@docs/Android/design-guide-app.md` §7

**바이브 코딩 프롬프트**:
```
모든 Composable에 TalkBack 지원을 추가해:

1. contentDescription 추가:
   KeyboardKeyButton(
       modifier = Modifier.semantics {
           contentDescription = "Enter 키, 길게 눌러 고정"
           role = Role.Button
       }
   )
   
   TouchpadWrapper(
       modifier = Modifier.semantics {
           contentDescription = "터치패드, 드래그하여 커서 이동"
       }
   )

2. 상태 변화 알림:
   var isScrollActive by remember { mutableStateOf(false) }
   
   LaunchedEffect(isScrollActive) {
       if (isScrollActive) {
           announceForAccessibility("스크롤 모드 활성화됨")
       }
   }

3. 커스텀 액션:
   Modifier.semantics {
       customActions = listOf(
           CustomAccessibilityAction("모드 전환") {
               toggleMode()
               true
           }
       )
   }

4. 그룹화:
   Row(Modifier.semantics(mergeDescendants = true) {
       contentDescription = "Special Keys: Esc, Tab, Enter, Backspace"
   })

5. TalkBack 테스트:
   - 모든 버튼 읽기 가능
   - 상태 변화 알림
   - 액션 실행 가능
   - 논리적 순서로 네비게이션

TalkBack으로 시각장애인도 앱을 사용할 수 있게 해줘.
```

**검증 방법**:
- [ ] TalkBack으로 모든 요소 읽기 가능
- [ ] 상태 변화 음성 알림
- [ ] 논리적 네비게이션 순서
- [ ] 커스텀 액션 실행

###### 4.2.1.2 터치 영역 및 시각적 접근성

**구현 목표**: 최소 터치 영역 및 색상 대비 보장
**예상 시간**: 30-40분

**바이브 코딩 프롬프트**:
```
접근성 가이드라인 준수를 확인하고 수정해:

1. 최소 터치 영역 (48×48dp):
   모든 버튼/클릭 가능 요소를 검토하고 최소 크기 보장:
   
   KeyboardKeyButton(
       modifier = Modifier
           .size(48.dp)  // 최소 크기
           .padding(4.dp)  // 시각적 간격
   )

2. 색상 독립적 정보 전달:
   // ❌ 나쁜 예: 색상만으로 정보 전달
   Box(color = if (isActive) Green else Red)
   
   // ✅ 좋은 예: 색상 + 아이콘/텍스트
   Row {
       Icon(if (isActive) Icons.Check else Icons.Close)
       Text(if (isActive) "활성" else "비활성")
       Box(color = if (isActive) Green else Red)
   }

3. 대비율 확인 (4.5:1 이상):
   - 텍스트와 배경: 4.5:1
   - 중요 UI 요소: 3:1
   - Color Contrast Analyzer 도구 사용

4. 포커스 표시:
   Modifier.indication(
       interactionSource = remember { MutableInteractionSource() },
       indication = rememberRipple()
   )

5. 체크리스트:
   - [ ] 모든 버튼 ≥48×48dp
   - [ ] 색상 + 아이콘/텍스트 조합
   - [ ] 대비율 ≥4.5:1
   - [ ] 명확한 포커스 표시

@docs/Android/design-guide-app.md §7의 접근성 가이드라인을 모두 적용해줘.
```

**검증 방법**:
- [ ] 모든 터치 영역 ≥48×48dp
- [ ] 색상 대비율 ≥4.5:1
- [ ] 색상 + 아이콘/텍스트 조합
- [ ] 포커스 시각적 표시

##### 4.2.2 Windows 서버 접근성

###### 4.2.2.1 WCAG 2.1 AA 준수

**구현 목표**: 대비율 및 키보드 네비게이션
**예상 시간**: 40-50분

**바이브 코딩 프롬프트**:
```
Windows 서버 WPF UI에 접근성을 적용해:

1. 색상 대비율 (4.5:1):
   <SolidColorBrush x:Key="PrimaryText" Color="#FFFFFF" />
   <SolidColorBrush x:Key="Background" Color="#1E1E1E" />
   <!-- 대비율: 15.8:1 (합격) -->
   
   모든 색상 조합을 검토하고 4.5:1 이상 보장

2. AutomationProperties:
   <Button AutomationProperties.Name="서버 시작"
           AutomationProperties.HelpText="BridgeOne 서버를 시작합니다" />
   
   <TextBlock AutomationProperties.LiveSetting="Polite"
              Text="{Binding ConnectionStatus}" />

3. 키보드 네비게이션:
   <Button TabIndex="1" />
   <Button TabIndex="2" IsTabStop="True" />
   
   // 논리적 Tab 순서 설정

4. 포커스 시각화:
   <Style TargetType="Button">
       <Setter Property="FocusVisualStyle">
           <Setter.Value>
               <Style>
                   <Setter Property="Control.Template">
                       <Setter.Value>
                           <ControlTemplate>
                               <Rectangle Stroke="Cyan" 
                                         StrokeThickness="2"
                                         Margin="-2" />
                           </ControlTemplate>
                       </Setter.Value>
                   </Setter>
               </Style>
           </Setter.Value>
       </Setter>
   </Style>

5. 최소 클릭 영역 (44×44px):
   <Button MinWidth="44" MinHeight="44" />

WCAG 2.1 AA 기준을 준수해서 접근성을 보장해줘.
```

**검증 방법**:
- [ ] 모든 대비율 ≥4.5:1
- [ ] AutomationProperties 설정
- [ ] Tab 키로 전체 네비게이션 가능
- [ ] 포커스 명확히 표시
- [ ] 최소 클릭 영역 44×44px

#### 4.3 개발 완료 및 로컬 배포

##### 4.3.1 빌드 및 배포 준비

###### 4.3.1.1 Android APK 디버그 빌드

**구현 목표**: 개발용 APK 빌드 및 권한 확인
**예상 시간**: 30-40분

**바이브 코딩 프롬프트**:
```
Android 디버그 빌드를 생성해:

1. build.gradle.kts 디버그 설정 확인:
   buildTypes {
       debug {
           isMinifyEnabled = false
           isDebuggable = true
           applicationIdSuffix = ".debug"
           versionNameSuffix = "-DEBUG"
       }
   }

2. 필수 권한 최종 확인:
   <uses-permission android:name="android.permission.VIBRATE" />
   <uses-feature android:name="android.hardware.usb.host" />

3. APK 빌드:
   ./gradlew assembleDebug
   
   출력: app/build/outputs/apk/debug/app-debug.apk

4. APK 검증:
   - aapt dump badging app-debug.apk
   - 권한 확인
   - minSdk, targetSdk 확인

5. USB 디버깅 설치:
   adb install -r app-debug.apk

6. README_INSTALL.md 작성:
   # BridgeOne Android 앱 설치 가이드
   
   ## 요구사항
   - Android 10 (API 29) 이상
   - USB OTG 지원
   
   ## 설치 방법
   1. 개발자 옵션 활성화
   2. USB 디버깅 활성화
   3. adb install app-debug.apk
   
   ## 권한
   - USB 호스트 모드
   - 진동

개발자 모드로 직접 설치할 수 있는 APK를 준비해줘.
```

**검증 방법**:
- [ ] APK 빌드 성공
- [ ] 파일 크기 확인
- [ ] 권한 정확히 포함
- [ ] USB 디버깅 설치 성공
- [ ] 설치 가이드 작성

###### 4.3.1.2 Windows 서버 Release 빌드

**구현 목표**: Release 빌드 및 자동 실행 스크립트
**예상 시간**: 40-50분

**바이브 코딩 프롬프트**:
```
Windows Release 빌드 및 배포 준비:

1. Release 빌드 설정:
   <PropertyGroup Condition="'$(Configuration)'=='Release'">
       <Optimize>true</Optimize>
       <DebugType>none</DebugType>
       <PublishSingleFile>true</PublishSingleFile>
       <SelfContained>false</SelfContained>
   </PropertyGroup>

2. Release 빌드:
   dotnet publish -c Release -r win-x64
   
   출력: bin/Release/net8.0/win-x64/publish/

3. 배치 스크립트 (start_bridgeone.bat):
   @echo off
   echo BridgeOne 서버를 시작합니다...
   
   REM .NET 8.0 Runtime 확인
   dotnet --version >nul 2>&1
   if errorlevel 1 (
       echo .NET 8.0 Runtime이 필요합니다.
       echo https://dotnet.microsoft.com/download
       pause
       exit /b 1
   )
   
   REM 서버 시작
   start "" "BridgeOne.exe"
   echo 서버가 시작되었습니다.
   timeout /t 3
   exit

4. 자동 시작 레지스트리 스크립트 (autostart.reg):
   Windows Registry Editor Version 5.00
   
   [HKEY_CURRENT_USER\Software\Microsoft\Windows\CurrentVersion\Run]
   "BridgeOne"="C:\\Path\\To\\BridgeOne.exe"

5. README_WINDOWS.md 작성:
   # BridgeOne Windows 서버 설치
   
   ## 요구사항
   - Windows 10/11 (64bit)
   - .NET 8.0 Runtime
   
   ## 설치
   1. Release 폴더 압축 해제
   2. start_bridgeone.bat 실행
   
   ## 자동 시작 (선택)
   - autostart.reg 더블클릭

Release 빌드와 실행 스크립트를 준비해줘.
```

**검증 방법**:
- [ ] Release 빌드 성공
- [ ] 단일 파일 실행 가능
- [ ] 배치 스크립트 동작
- [ ] 자동 시작 레지스트리 동작
- [ ] 설치 가이드 작성

###### 4.3.1.3 ESP32-S3 Release 펌웨어

**구현 목표**: 최적화된 Release 펌웨어 빌드
**예상 시간**: 20-30분

**바이브 코딩 프롬프트**:
```
ESP32-S3 Release 펌웨어 빌드:

1. platformio.ini에 Release 환경 추가:
   [env:release]
   extends = env:esp32s3_wroom
   build_flags = 
       ${env:esp32s3_wroom.build_flags}
       -O2                     ; 최적화 레벨 2
       -DCORE_DEBUG_LEVEL=0    ; 디버그 로그 비활성화
       -DNDEBUG                ; assert 비활성화

2. Release 빌드:
   pio run -e release
   
   출력: .pio/build/release/firmware.bin

3. 펌웨어 업로드 방법 문서:
   # ESP32-S3 펌웨어 업로드 가이드
   
   ## PlatformIO 사용
   ```
   pio run -e release -t upload
   ```
   
   ## Arduino IDE 사용
   1. Tools → Board → ESP32S3 Dev Module
   2. Tools → Upload Speed → 921600
   3. Sketch → Upload

4. 핀아웃 문서 (PINOUT.md):
   # ESP32-S3-WROOM-1-N16R8 연결
   
   ## UART (보드 내장 USB-Serial)
   - RX: GPIO44 (보드 UART 포트와 연결)
   - TX: GPIO43 (보드 UART 포트와 연결)
   - Android 폰에서 OTG 케이블로 보드의 UART 포트에 직접 연결
   
   ## USB
   - PC 직접 연결 (HID + CDC) - Native USB OTG 사용

5. 펌웨어 버전 정보:
   const char* FIRMWARE_VERSION = "1.0.0";
   Serial.printf("BridgeOne Firmware v%s\n", FIRMWARE_VERSION);

Release 펌웨어와 업로드 가이드를 준비해줘.
```

**검증 방법**:
- [ ] Release 빌드 성공
- [ ] 펌웨어 크기 확인
- [ ] 최적화 적용 확인
- [ ] 업로드 가이드 작성
- [ ] 핀아웃 문서 작성

##### 4.3.2 설치 가이드 문서

###### 4.3.2.1 통합 설치 가이드 작성

**구현 목표**: 초보자도 따라할 수 있는 상세 가이드
**예상 시간**: 1시간-1시간 30분

**바이브 코딩 프롬프트**:
```
@Docs/INSTALLATION_GUIDE.md 를 작성해:

# BridgeOne 설치 및 설정 가이드

## 목차
1. 하드웨어 준비
2. ESP32-S3 펌웨어 업로드
3. Android 앱 설치
4. Windows 서버 설치
5. 초기 연결 테스트

## 1. 하드웨어 준비

**필요한 장비**:
- [ ] Android 디바이스 (Android 10 이상, USB OTG 지원)
- [ ] ESP32-S3-WROOM-1-N16R8 보드
- [ ] Windows PC (Windows 10/11 64bit)
- [ ] USB OTG 케이블 (Android ↔ ESP32-S3)
- [ ] USB 케이블 (ESP32-S3 ↔ PC)

**연결 구성**:
```
Android ↔ [USB OTG] ↔ ESP32-S3 ↔ [USB] ↔ Windows PC
```

## 2. ESP32-S3 펌웨어 업로드

### 2.1 PlatformIO 방법 (권장)

1. VSCode + PlatformIO 설치
2. 프로젝트 열기: `src/board/BridgeOne/`
3. ESP32-S3를 PC에 USB 연결
4. PlatformIO: Upload (→ 아이콘)
5. 시리얼 모니터 확인: "Firmware initialized"

### 2.2 Arduino IDE 방법

1. Arduino IDE 설치
2. ESP32 보드 패키지 설치
3. Tools → Board → ESP32S3 Dev Module
4. src/board/BridgeOne/src/main.cpp 열기
5. Upload 클릭

**문제 해결**:
- Upload 실패: Boot 버튼 누른 채로 Upload
- COM 포트 없음: USB 드라이버 설치

## 3. Android 앱 설치

1. 개발자 옵션 활성화:
   - 설정 → 휴대전화 정보 → 빌드 번호 7회 탭

2. USB 디버깅 활성화:
   - 설정 → 개발자 옵션 → USB 디버깅

3. APK 설치:
   ```
   adb install app-debug.apk
   ```

4. 권한 확인:
   - 앱 실행 → USB 권한 승인

## 4. Windows 서버 설치

1. .NET 8.0 Runtime 설치:
   https://dotnet.microsoft.com/download

2. BridgeOne 압축 해제

3. start_bridgeone.bat 실행

4. 시스템 트레이 아이콘 확인

## 5. 초기 연결 테스트

1. ESP32-S3를 Windows PC에 연결
2. Windows 서버 실행 확인
3. ESP32-S3를 Android에 USB OTG로 연결
4. Android 앱 실행
5. USB 권한 승인
6. Essential 모드 진입 확인
7. 터치패드 터치 → Windows 커서 이동 확인
8. 핸드셰이크 완료 → Standard 모드 전환

**축하합니다! 설치 완료!** 🎉

초보자도 따라할 수 있도록 스크린샷과 함께 상세히 작성해줘.
```

**검증 방법**:
- [ ] 설치 가이드 단계별 정확
- [ ] 문제 해결 섹션 포함
- [ ] 초보자 친화적
- [ ] 스크린샷 포함 (선택)

#### 4.4 문서화

###### 4.4.1.1 사용자 가이드 작성

**구현 목표**: 기능별 상세 사용법
**예상 시간**: 1시간-1시간 30분

**바이브 코딩 프롬프트**:
```
@Docs/USER_GUIDE.md 를 작성해:

# BridgeOne 사용자 가이드

## 기본 사용법

### 터치패드로 커서 이동
1. 터치패드 영역을 손가락으로 드래그
2. Windows PC의 커서가 이동
3. 터치 해제 시 커서 정지

### 클릭하기
- **좌클릭**: 터치패드를 500ms 이내 짧게 탭
- **우클릭**: 우클릭 버튼 탭 → 터치패드 탭

### 스크롤하기
1. 스크롤 버튼 탭
2. 터치패드를 상하로 드래그
3. 50dp마다 햅틱 피드백

## 고급 기능

### 직각 이동 모드
1. 이동 모드 버튼 → "직각"
2. 수평/수직 방향으로만 커서 이동
3. 정확한 선 그리기에 유용

### 무한 스크롤
1. 스크롤 버튼 길게 누르기
2. 빠르게 드래그 → 관성 스크롤
3. 더블탭으로 종료

### Sticky Hold
1. 키 버튼 500ms 길게 누르기
2. 버튼 강조 표시
3. 해제할 때까지 키 입력 유지
4. 다시 탭하여 해제

### 멀티 커서 (Standard 모드)
1. 커서 모드 버튼 → "멀티"
2. 가상 커서 5개 표시
3. 커서 전환 버튼으로 텔레포트
4. 각 커서 위치 자동 저장

## 페이지 가이드

### Page 1: 범용
- 터치패드 (70%)
- Special Keys + Shortcuts (30%)

### Page 2: 키보드 중심
- Modifiers (Ctrl, Shift, Alt)
- Navigation (방향키, Home, End)
- Function Keys (F1-F12)

### Page 3: Minecraft
- WASD DPad
- 전투 버튼 (점프, 공격, 사용)
- Hotbar (1-9)

## 문제 해결

### USB 연결 안 됨
1. USB OTG 케이블 확인
2. Android USB 디버깅 설정
3. ESP32-S3 전원 확인

### 커서 이동 안 됨
1. Windows 서버 실행 확인
2. ESP32-S3 HID 장치 인식 확인
3. Android 로그 확인

실제 사용 케이스와 스크린샷을 포함해서 작성해줘.
```

**검증 방법**:
- [ ] 모든 기능 설명 포함
- [ ] 초보자도 이해 가능
- [ ] 문제 해결 섹션 포함
- [ ] 스크린샷 포함 (선택)

###### 4.4.2.1 개발자 문서 작성

**구현 목표**: API 레퍼런스 및 아키텍처 설명
**예상 시간**: 1시간-1시간 30분

**바이브 코딩 프롬프트**:
```
@Docs/DEVELOPER_GUIDE.md 를 작성해:

# BridgeOne 개발자 가이드

## 아키텍처 개요

### 시스템 구성
```
┌─────────────┐  UART 1Mbps  ┌──────────┐  USB HID/CDC  ┌──────────┐
│   Android   │──────────────→│ ESP32-S3 │──────────────→│ Windows  │
│     App     │←──────────────│  Bridge  │←──────────────│  Server  │
└─────────────┘  Vendor CDC   └──────────┘               └──────────┘
```

### 프로토콜 레이어
1. **Physical**: USB OTG (Android ↔ Board), USB (Board ↔ Windows)
2. **Data Link**: UART 1Mbps, HID Boot Protocol, CDC-ACM
3. **Transport**: BridgeFrame (8바이트), JSON over CDC
4. **Application**: 터치 입력, 커서 제어, 상태 관리

## API 레퍼런스

### Android

#### BridgeFrame
```kotlin
data class BridgeFrame(
    val seq: UByte,         // 순번 (0-255)
    val buttons: UByte,     // 마우스 버튼
    val deltaX: Byte,       // X축 이동 (-127~127)
    val deltaY: Byte,       // Y축 이동
    val wheel: Byte,        // 휠
    val modifiers: UByte,   // 키보드 모디파이어
    val keyCode1: UByte,    // 키 코드 1
    val keyCode2: UByte     // 키 코드 2
)
```

#### TouchInputProcessor
```kotlin
class TouchInputProcessor {
    fun processTouch(event: MotionEvent): BridgeFrame
    fun reset()
}
```

### ESP32-S3

#### BridgeFrame 구조체
```c
typedef struct __attribute__((packed)) {
    uint8_t seq;
    uint8_t buttons;
    int8_t deltaX;
    int8_t deltaY;
    int8_t wheel;
    uint8_t modifiers;
    uint8_t keyCode1;
    uint8_t keyCode2;
} BridgeFrame;
```

### Windows

#### IHidInputService
```csharp
public interface IHidInputService
{
    void StartListening();
    void StopListening();
    event EventHandler<HidInputEventArgs> InputReceived;
}
```

## 확장 개발 가이드

### 새 페이지 추가
1. pages/ 에 새 Composable 생성
2. HorizontalPager에 추가
3. 페이지 인디케이터 업데이트

### 새 버튼 추가
1. KeyboardKeyButton 또는 ShortcutButton 사용
2. keyCode 정의
3. 레이아웃에 배치

### 새 제스처 추가
1. gestures/ 에 새 클래스 생성
2. TouchInputProcessor에 통합
3. 모드 enum 확장

개발자가 BridgeOne을 확장할 수 있도록 상세히 작성해줘.
```

**검증 방법**:
- [ ] 아키텍처 다이어그램 포함
- [ ] API 레퍼런스 완성
- [ ] 확장 가이드 포함
- [ ] 코드 예시 포함

**Phase 4 완료 조건**: 
- ✅ E2E 시나리오 테스트 통과
- ✅ 성능 벤치마크 목표 달성
- ✅ 접근성 기준 충족
- ✅ Release 빌드 3개 준비 (APK, EXE, 펌웨어)
- ✅ 설치 가이드 및 사용자 매뉴얼 완성
- ✅ 개발자 문서 완성
