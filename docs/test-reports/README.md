# Phase 1.1.3.2: 기본 연결 및 프레임 송수신 테스트

## 📁 문서 구조

```
docs/test-reports/
├── README.md                           # 이 파일 (테스트 개요 및 실행 방법)
├── Phase-1.1.3.2-Test-Guide.md         # 상세 테스트 가이드 (30-40분)
└── Phase-1.1.3.2-Test-Checklist.md     # 빠른 체크리스트
```

## 🎯 테스트 목표

**Android ↔ ESP32-S3 통신 End-to-End 검증**

이 테스트는 다음을 검증합니다:
1. ✅ USB 장치 자동 인식 및 권한 요청
2. ✅ 터치 입력 → BridgeFrame 변환 → UART 전송
3. ✅ 순번 카운터 순환 (0→255→0)
4. ✅ 1분간 연속 전송 안정성 (손실률 <1%)

## 🚀 빠른 시작

### 1단계: 펌웨어 업로드

```bash
# ESP32-S3 보드 프로젝트로 이동
cd src/board/BridgeOne

# PlatformIO로 펌웨어 업로드
pio run --target upload

# 시리얼 모니터 실행 (디버그 로그 확인)
pio device monitor --baud 115200
```

**예상 출력:**
```
  BridgeOne ESP32-S3 Board
  Phase 1.1.2.5: FreeRTOS Task Structure
----------------------------------------
[UART2] Initialized: 1000000 bps, 8N1
[FreeRTOS] Task creation completed!
```

### 2단계: Android 앱 설치

```bash
# Android 프로젝트로 이동
cd src/android

# 앱 빌드 및 설치
./gradlew installDebug
```

또는 **Android Studio**에서:
- `Run > Run 'app'` (Shift+F10)

### 3단계: 하드웨어 연결

1. ESP32-S3의 **UART 포트** (왼쪽)에 USB-C 케이블 연결
2. Android 디바이스에 **USB OTG 어댑터** 연결
3. ESP32-S3 케이블을 OTG 어댑터에 연결

### 4단계: 테스트 수행

1. Android 앱 실행
2. USB 권한 승인
3. 터치패드 영역 터치 및 드래그
4. ESP32 시리얼 모니터에서 프레임 수신 로그 확인

## 📋 테스트 문서

### 📘 상세 가이드 (권장)
**파일**: [`Phase-1.1.3.2-Test-Guide.md`](./Phase-1.1.3.2-Test-Guide.md)

**내용**:
- 사전 준비사항
- 4가지 테스트 시나리오 (30-40분)
- 문제 해결 방법
- 테스트 결과 보고서 템플릿
- 공식 문서 참고 자료

**언제 사용?**
- 처음 테스트를 수행하는 경우
- 문제가 발생하여 디버깅이 필요한 경우
- 상세한 테스트 결과를 문서화해야 하는 경우

### ✅ 빠른 체크리스트
**파일**: [`Phase-1.1.3.2-Test-Checklist.md`](./Phase-1.1.3.2-Test-Checklist.md)

**내용**:
- 간단한 체크박스 형식
- 핵심 검증 항목만 포함
- 빠른 테스트 (10-15분)

**언제 사용?**
- 이미 테스트를 수행해본 경우
- 빠르게 재검증이 필요한 경우
- 회귀 테스트 (Regression Test)

## 🔍 예상 결과

### ✅ 성공 시나리오

**Android 앱:**
- 연결 상태: "✅ 연결됨"
- 총 전송: 1000+ 프레임 (1분 테스트 기준)
- 손실률: 0~1%

**ESP32-S3 시리얼 모니터:**
```
[RX #1] seq=0 ✓, buttons=0x01, delta=(0,0), wheel=0, ...
[RX #2] seq=1 ✓, buttons=0x01, delta=(+15,0), wheel=0, ...
[RX #3] seq=2 ✓, buttons=0x00, delta=(0,0), wheel=0, ...

========================================
[STATS] Uptime: 60 sec
[STATS] Received: 1243 frames
[STATS] Lost: 8 frames (0.64%)           <- 1% 미만 OK!
[STATS] Last Seq: 186
========================================
```

### ❌ 실패 시나리오

| 증상 | 원인 | 해결 방법 |
|------|------|-----------|
| "USB 장치를 찾을 수 없습니다" | OTG 케이블 불량 | 케이블 교체 |
| 권한 다이얼로그 없음 | VendorID 필터 오류 | `device_filter.xml` 확인 |
| 프레임 전송 안 됨 | UART 초기화 실패 | 펌웨어 재업로드 |
| 손실률 > 1% | USB 버퍼 오버플로우 | 버퍼 크기 증가 |

## 📊 테스트 결과 보고

테스트 완료 후 다음 중 하나를 선택하여 결과를 문서화하세요:

### 옵션 1: 상세 보고서
[`Phase-1.1.3.2-Test-Guide.md`](./Phase-1.1.3.2-Test-Guide.md)의 마지막 섹션에 있는 **"테스트 결과 보고서 템플릿"**을 작성합니다.

### 옵션 2: 간단 체크리스트
[`Phase-1.1.3.2-Test-Checklist.md`](./Phase-1.1.3.2-Test-Checklist.md)의 체크박스를 모두 체크하고 결과 요약을 작성합니다.

## 🔗 관련 문서

- **개발 계획**: [`/docs/development-plan-checklist.md`](../development-plan-checklist.md) - Phase 1.1.3.2
- **기술 명세 (Android)**: [`/docs/android/technical-specification-app.md`](../android/technical-specification-app.md)
- **기술 명세 (ESP32)**: [`/docs/board/esp32s3-code-implementation-guide.md`](../board/esp32s3-code-implementation-guide.md)
- **프로토콜 정의**: `BridgeFrame` - 8바이트 고정 크기 프레임

## 📝 다음 단계

테스트 통과 후:
1. ✅ [`development-plan-checklist.md`](../development-plan-checklist.md)의 **Phase 1.1.3.2** 체크박스 완료 처리
2. 📋 테스트 결과를 이 디렉토리에 저장 (예: `Phase-1.1.3.2-Test-Result-YYYYMMDD.md`)
3. 🚀 다음 Phase로 진행:
   - **Phase 1.2.1**: ESP32-S3 HID 입력 처리 구현
   - **Phase 1.2.2**: ESP32-S3 Vendor CDC 명령 인터페이스 구현

---

**작성일**: 2025-10-15  
**작성자**: AI Agent (Based on Context7 Official Documentation)  
**참고 문서**: 
- [espressif/arduino-esp32](https://github.com/espressif/arduino-esp32) (Trust: 9.1)
- [kshoji/usb-midi-driver](https://github.com/kshoji/usb-midi-driver) (Trust: 9.2)
- [mik3y/usb-serial-for-android](https://github.com/mik3y/usb-serial-for-android) (Trust: 9.5)

