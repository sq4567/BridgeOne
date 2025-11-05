---
title: "BridgeOne Phase 2.3: Android ↔ ESP32-S3 UART 통신 검증"
description: "BridgeOne 프로젝트 Phase 2.3 - UART 프로토콜 검증 및 E2E 통신 테스트"
tags: ["android", "esp32-s3", "uart", "e2e-testing", "performance"]
version: "v2.0"
owner: "Chatterbones"
updated: "2025-11-04"
---

# BridgeOne Phase 2.3: Android ↔ ESP32-S3 UART 통신 검증

**개발 기간**: 3-4일

**목표**: Android → ESP32-S3 UART 통신 경로 검증 및 성능 측정

**핵심 성과물**:
- UART 프레임 정확성 검증 (8바이트 구조, 순번, 바이트 순서)
- 통신 안정성 검증 (손실률, 지연시간, 연속성)
- 성능 임계값 달성 (50ms 이하 지연시간, 0.1% 이하 손실률)
- 장시간 안정성 테스트 (4시간 연속 사용)
- E2E HID 통신 검증 (Android → ESP32-S3 → Windows)

---

## Phase 2.3.1: UART 프레임 정확성 검증

**목표**: Android에서 생성한 8바이트 델타 프레임이 ESP32-S3에 정확하게 전달되는지 검증

**사전 환경 설정**:
1. Android 기기를 ESP32-S3의 USB-UART 포트에 USB-C 케이블 연결
2. Windows PC: 시리얼 모니터 실행 (PuTTY, miniterm.py, 또는 Arduino IDE Serial Monitor)
   - 포트: ESP32-S3 시리얼 포트 (COM3, COM4 등 확인)
   - 속도: 115200 baud (ESP-IDF 초기 로그)
3. Android 앱 빌드 및 설치

**세부 목표**:
1. UART 프레임 수신 및 로그 검증
2. 8바이트 구조 정확성 확인
3. 순번(seq) 필드 연속성 검증
4. Little-Endian 바이트 순서 검증

**검증** (정성적):
- [ ] Android 앱 터치패드 드래그 시 시리얼 로그에 프레임 수신 메시지 출력
  ```
  UART frame received: seq=0 buttons=0x00 deltaX=50 deltaY=30 wheel=0
  UART frame received: seq=1 buttons=0x00 deltaX=45 deltaY=28 wheel=0
  UART frame received: seq=2 buttons=0x01 deltaX=0 deltaY=0 wheel=0
  ```

**검증** (정량적):
- [ ] **8바이트 구조**: 모든 프레임이 정확히 8바이트 (로그 확인)
- [ ] **순번 연속성**: seq 필드가 0→1→2→...→254→255→0으로 순환 증가 (100프레임 이상 확인)
- [ ] **바이트 순서**: deltaX, deltaY가 Little-Endian으로 올바르게 전달
  - 예: deltaX=256 → 바이트 순서: 0x00, 0x01 (not 0x01, 0x00)
- [ ] **버튼 상태**: buttons 필드에 0x00(누름 없음) 또는 0x01(좌클릭) 올바르게 전달
- [ ] **무손실 전달**: 일부러 드래그 중단 후 재시작 → 프레임 손실 없음 확인

---

## Phase 2.3.2: UART 통신 지연시간 및 손실률 측정

**목표**: UART 통신 성능 임계값 달성 검증 (50ms 이하 지연, 0.1% 이하 손실)

**측정 방법론**:
1. **지연시간 측정**:
   - Android 앱에 타임스탬프 기록 (터치 발생 시간)
   - ESP32-S3에 타임스탬프 기록 (UART 수신 시간)
   - 차이 계산: (ESP32-S3 수신 시간) - (Android 터치 시간)

2. **샘플링**:
   - 최소 100프레임 이상 측정 (약 4~5초)
   - 다양한 입력 패턴: 느린 드래그, 빠른 드래그, 클릭

3. **프레임 손실 측정**:
   - 시리얼 로그 seq 필드 분석
   - 손실된 seq 번호 식별 및 횟수 계산

**검증** (정량적):
- [ ] **평균 지연시간**: 100프레임 측정 후 평균값 < 50ms
- [ ] **최대 지연시간**: 99 percentile < 100ms
- [ ] **프레임 손실률**: (손실 프레임 / 전체 프레임) × 100% < 0.1%
  - 예: 1000프레임 중 1개 이하 손실
- [ ] **seq 연속성**: 0→1→2→...→254→255→0 연속 증가 (중단 없음)
- [ ] **5분 연속 전송**: 300초 이상 연속 전송 중 크래시 없음 (시리얼 로그 오류 없음)

---

## Phase 2.3.3: UART 안정성 및 스트레스 테스트

**목표**: 장시간 UART 통신 안정성 검증

**세부 목표**:
1. 4시간 연속 마우스 + 키보드 입력 시뮬레이션
2. ESP32-S3 크래시 없음
3. Android 앱 크래시 없음
4. 메모리 누수 없음

**검증**:
- [ ] 4시간 연속 사용 중 ESP32-S3 UART 에러 없음 (시리얼 로그)
- [ ] 4시간 연속 사용 중 Android 앱 크래시 없음
- [ ] ESP32-S3 메모리 사용량 안정 (`esp_get_free_heap_size()` 일정 유지)
- [ ] CPU 사용률 < 30% (FreeRTOS 태스크 통계)
- [ ] USB 연결 해제 후 재연결 시 자동 복구 확인

---

## Phase 2.3.4: HID Mouse 경로 End-to-End 검증

**목표**: 터치 → UART → HID Mouse → Windows 마우스 이동 전체 경로 검증

**사전 환경 설정**:
1. Android 기기를 ESP32-S3의 USB-UART 포트에 USB-C 케이블 연결
2. ESP32-S3의 USB-OTG 포트를 Windows PC에 USB 케이블로 연결
3. Android 앱 빌드 및 설치
4. Windows: 시리얼 모니터 실행 (PuTTY, miniterm.py, 또는 Arduino IDE Serial Monitor)
   - 포트: ESP32-S3 시리얼 포트 (COM3, COM4 등 확인)
   - 속도: 115200 baud (ESP-IDF 초기 로그)

**세부 목표**:
1. 시리얼 모니터에서 UART 프레임 수신 로그 확인
2. 시리얼 모니터에서 HID Mouse 리포트 전송 로그 확인
3. Windows에서 마우스 포인터 이동 확인
4. 이동 거리 및 속도 정량적 검증

**검증** (정량적 기준):
- [ ] Android 앱 터치패드 드래그 시 Windows 마우스 포인터 이동 확인
- [ ] 시리얼 로그: "UART frame received: seq=X buttons=X deltaX=X deltaY=X" 표시
- [ ] 시리얼 로그: "HID Mouse report sent: buttons=X deltaX=X deltaY=X wheel=X" 표시
- [ ] **터치 이동 거리 검증** (정량적):
  - 터치 패드에서 100픽셀 드래그 → Windows 마우스 포인터 80~120픽셀 이동 (오차 범위: ±20%)
  - 측정 방법: 마우스 포인터 위치 캡처 (GetCursorPos() 또는 스크린샷 좌표)
- [ ] **마우스 이동 방향 검증** (정성적):
  - 위 방향 터치 → 마우스 위 이동
  - 아래 방향 터치 → 마우스 아래 이동
  - 좌측 방향 터치 → 마우스 좌측 이동
  - 우측 방향 터치 → 마우스 우측 이동
- [ ] **클릭 제스처 검증**: 500ms 이내 탭 → Windows 좌클릭 동작 (메모장에서 커서 이동 또는 버튼 클릭으로 확인)
- [ ] **지연시간 측정** (정량적, 평균):
  - 시리얼 로그 타임스탐프: "UART frame received at 1000ms"
  - 시리얼 로그 타임스탐프: "HID Mouse report sent at 1010ms"
  - 지연시간: 1010 - 1000 = 10ms (목표: < 50ms)
  - 10회 이상 반복 측정 후 평균값 계산
- [ ] **프레임 손실 검증**: 시리얼 로그에서 seq 필드 연속성 확인 (0→1→2→...→254→255→0 연속 증가)

---

## Phase 2.3.5: HID Keyboard 경로 End-to-End 검증

**목표**: 키 입력 → UART → HID Keyboard → Windows 키 입력 검증

**세부 목표**:
1. KeyboardKeyButton 컴포넌트 기본 구현 확인
2. 키 다운/업 이벤트 → 프레임 생성
3. Windows 메모장에서 키 입력 확인
4. BIOS 호환성 검증 (Del 키 테스트)

**참조 문서 및 섹션**:
- `docs/android/technical-specification-app.md` §2.3.2.1 KeyboardKeyButton 컴포넌트 설계 요구사항
- `docs/board/esp32s3-code-implementation-guide.md` §4.2 USB HID 모듈 구현
- `docs/android/component-design-guide-app.md` (KeyboardKeyButton 상세)

**검증**:
- [ ] `src/android/app/src/main/java/com/bridgeone/app/ui/components/KeyboardKeyButton.kt` 파일 생성됨
- [ ] 터치 다운 시 `keyCode` 포함한 프레임 전송 (modifiers=0, keyCode1=X, keyCode2=0)
- [ ] 터치 업 시 빈 프레임 전송 (keyCode1=0, keyCode2=0)
- [ ] ESP32-S3 시리얼 로그에 "HID Keyboard report sent: modifiers=X keyCodes=[X,X,0,0,0,0]" 메시지 표시
- [ ] Windows 메모장에서 키 입력 정상 작동 확인 (사용자 테스트)
- [ ] Del 키 정상 동작 확인
- [ ] Esc 키 정상 동작 확인
- [ ] Enter 키 정상 동작 확인
- [ ] BIOS 진입 테스트 (재부팅 시 Del 키 → BIOS 화면 진입 확인, 사용자 테스트)
- [ ] 키 입력 지연시간 10ms 이하 (사용자 체감 테스트)

---

## Phase 2.3.6: 최종 지연시간 및 프레임 손실률 측정 (HID 경로)

**목표**: 성능 임계값 검증 (50ms 이하 평균 지연, 0.1% 이하 손실)

**측정 방법론**:
1. **지연시간 측정 (3가지 경로)**:
   - `Android 터치 → ESP32-S3 UART 수신`: 시리얼 로그 타임스탬프 기반
   - `ESP32-S3 UART 수신 → HID 전송`: ESP32-S3 내부 타임스탬프
   - `총 지연시간 = 안드로이드 터치 발생 ~ Windows HID 수신`

2. **샘플링**:
   - 최소 100프레임 이상 측정 (5초 이상)
   - 다양한 입력 패턴: 느린 드래그, 빠른 드래그, 클릭

3. **프레임 손실 측정**:
   - 시리얼 로그 seq 필드 분석
   - 손실된 seq 번호 식별 및 횟수 계산

**검증** (정량적):
- [ ] **평균 지연시간**: 100 프레임 측정 후 평균값 < 50ms
- [ ] **최대 지연시간**: 99 percentile (상위 1%) < 100ms
- [ ] **프레임 손실률**: (손실 프레임 / 전체 프레임) × 100% < 0.1%
  - 예: 1000프레임 중 1개 이하 손실
- [ ] **연속성 검증**: seq 필드가 0→1→2→...→254→255→0 연속 증가 확인
- [ ] **안정성**: 5분 이상 연속 전송 중 크래시 없음 (시리얼 로그 오류 없음)

---

## Phase 2.3.7: HID 안정성 및 스트레스 테스트

**목표**: 장시간 사용 안정성 검증

**세부 목표**:
1. 4시간 연속 마우스 + 키보드 입력
2. 크래시 없음 (ESP32-S3, Android 앱 모두)
3. 메모리 누수 없음
4. 자동 재연결 테스트

**검증**:
- [ ] 4시간 연속 사용 중 크래시 없음
- [ ] 마우스 이동 중 클릭 정상 동작
- [ ] 키보드 타이핑 중 마우스 조작 정상 동작
- [ ] 메모리 사용량 안정 (`esp_get_free_heap_size()` 일정 유지)
- [ ] CPU 사용률 < 30% (`vTaskGetRunTimeStats()`)
- [ ] Windows에서 입력 오류 없음 (마우스/키보드 정상 작동)
- [ ] Android 앱 배터리 소모 정상 범위 (4시간 사용 시 배터리 20% 이하 소모)

---

## Phase 2.3.8: 최종 통합 검증 및 문서화

**목표**: Phase 2.3 완료 및 최종 검증

**개발 기간**: 2-3일

**세부 목표**:
1. 모든 하위 Phase 검증 완료 확인
2. 성능 목표 달성 확인
3. 문서화 및 주석 최종 검토
4. 커밋 및 릴리스 노트 작성

---

### Phase 2.3.8.1: Phase 2.3 검증 항목 최종 확인

**목표**: Phase 2.3의 모든 세부 단계 검증 완료 및 성능 임계값 달성 확인

**세부 목표**:
1. Phase 2.3.1 ~ 2.3.4의 모든 검증 항목 재확인
2. 성능 임계값 달성 최종 검증
3. Phase 2.1/2.2와의 호환성 재검증 (HID 기능 여전히 정상)

**검증** (Phase 2.3 검증 항목):
- [ ] Phase 2.3.1 검증 완료: UART 프레임 정확성 검증
  - 8바이트 구조 정확
  - seq 필드 연속성 (0~255 순환)
  - Little-Endian 바이트 순서 정확
- [ ] Phase 2.3.2 검증 완료: UART 지연시간 및 손실률 측정
  - 평균 지연시간 < 50ms (100프레임 이상 측정)
  - 최대 지연시간 < 100ms (99 percentile)
  - 프레임 손실률 < 0.1%
- [ ] Phase 2.3.3 검증 완료: UART 안정성 및 스트레스 테스트
  - 4시간 연속 사용 무중단
  - CPU 사용률 < 30%
  - 메모리 누수 없음
- [ ] Phase 2.3.4 검증 완료: HID Mouse 경로 E2E 검증
  - Android 터치패드 드래그 → Windows 마우스 포인터 이동 정상
  - 마우스 방향 및 속도 정확
- [ ] Phase 2.3.5 검증 완료: HID Keyboard 경로 E2E 검증
  - Windows 메모장에서 키 입력 정상 작동
  - BIOS 호환성 확인 (Del 키 → BIOS 진입)
  - 지연시간 10ms 이하
- [ ] Phase 2.3.6 검증 완료: 최종 지연시간 및 프레임 손실률 측정 (HID 경로)
  - E2E 평균 지연시간 < 50ms
  - 프레임 손실률 < 0.1%
- [ ] Phase 2.3.7 검증 완료: HID 안정성 및 스트레스 테스트
  - 4시간 연속 마우스 + 키보드 사용 무중단
  - Windows HID 입력 오류 없음

**검증** (성능 임계값):
- [ ] 평균 지연시간 < 50ms
- [ ] 최대 지연시간 < 100ms (99 percentile)
- [ ] 프레임 손실률 < 0.1%
- [ ] 4시간 연속 사용 무중단 (크래시 없음)
- [ ] CPU 사용률 < 30%

**검증** (호환성):
- [ ] Windows 10 HID 인식 정상
- [ ] Windows 11 HID 인식 정상
- [ ] Android 8.0 이상 USB Serial 통신 정상
- [ ] Phase 2.1/2.2 기능 (마우스/키보드) 여전히 정상 작동

---

### Phase 2.3.8.2: 문서 및 코드 주석 최종 검토

**목표**: 모든 코드 파일 주석 및 Docstring 완성도 검증

**세부 목표**:
1. 모든 함수에 Google 스타일 Docstring 확인
2. 복잡한 로직에 한국어 주석 확인
3. 상수명 대문자 규칙 확인 (예: MAX_RETRY_COUNT)
4. Boolean 변수명 규칙 확인 (is, has, can으로 시작)
5. 테스트 관련 문서 업데이트

**검증**:
- [ ] 모든 public 함수에 Docstring 포함
- [ ] 복잡한 로직에 한국어 주석
- [ ] 상수명 모두 UPPER_CASE
- [ ] Boolean 변수명 규칙 준수
- [ ] README 또는 PHASE_NOTES 문서에 Phase 2.3 완료 기록
- [ ] Linter 오류 없음 (모든 파일)

---

### Phase 2.3.8.3: 최종 커밋 및 릴리스 노트

**목표**: Phase 2.3 완료 커밋 및 정리

**세부 목표**:
1. 모든 변경사항 커밋
2. 커밋 메시지 작성 (작가 가이드라인 준수)
3. Phase 2.3 완료 요약 문서 작성
4. 다음 Phase 2.4 준비

**검증**:
- [ ] 모든 파일 커밋 완료
- [ ] 커밋 메시지: "feat: Phase 2.3 HID 통신 E2E 검증 및 성능 테스트 완료"
- [ ] Phase 2.3 완료 요약 문서 작성
- [ ] Phase 2.4 시작 조건 확인

---

## 📊 Phase 2.3 성과물

**검증 완료 항목**:
- ✅ UART 프레임 정확성: 8바이트 구조, seq 필드 연속성, Little-Endian 바이트 순서
- ✅ UART 성능: 50ms 이하 지연시간, 0.1% 이하 손실률 달성
- ✅ UART 안정성: 4시간 연속 사용 무중단, CPU < 30%
- ✅ HID Mouse 경로: Android 터치 → ESP32-S3 UART → Windows 마우스 제어 완전 검증
- ✅ HID Keyboard 경로: Android 키입력 → ESP32-S3 UART → Windows 키보드 제어 완전 검증
- ✅ BIOS 호환성: Del 키를 통한 BIOS 진입 확인

**구성된 통신 경로**:
- Android Jetpack Compose UI (TouchpadWrapper + KeyboardKeyButton)
- Android USB Serial 라이브러리 (usb-serial-for-android)
- ESP32-S3 UART 수신 (1Mbps, 8바이트 델타 프레임)
- ESP32-S3 HID 변환 (UART → HID Mouse/Keyboard)
- ESP32-S3 TinyUSB (HID Boot Mouse + Keyboard)
- Windows HID 기본 드라이버 (자동 인식)

**핵심 성과물**:
- Android 앱: `TouchpadWrapper.kt`, `KeyboardKeyButton.kt`, `BridgeFrame.kt`, `FrameBuilder.kt`, `UsbSerialManager.kt`
- ESP32-S3 펌웨어: `uart_handler.c`, `hid_handler.c`, `usb_descriptors.c`, `main.c`
- 검증 완료: UART 프로토콜, E2E 성능 측정, BIOS 호환성, 4시간 안정성 테스트

**다음 단계**: Phase 2.4 (Windows 양방향 통신) - CDC Vendor 통신 구현 또는 Keyboard UI 완성도 개선

---
