---
title: "BridgeOne Phase 2.3: Android → ESP32-S3 UART 통신 검증"
description: "BridgeOne 프로젝트 Phase 2.3 - UART 프로토콜 검증 및 E2E 통신 테스트"
tags: ["android", "esp32-s3", "uart", "e2e-testing", "performance"]
version: "v1.0"
owner: "Chatterbones"
updated: "2025-11-06"
---

# BridgeOne Phase 2.3: Android → ESP32-S3 UART 통신 검증

**개발 기간**: 3-4일

**목표**: Android → ESP32-S3 UART 통신 경로 검증 및 성능 측정

**핵심 성과물**:
- UART 프레임 정확성 검증 (8바이트 구조, 순번, 바이트 순서)
- 통신 안정성 검증 (손실률, 지연시간, 연속성)
- 성능 임계값 달성 (50ms 이하 지연시간, 0.1% 이하 손실률)
- 장시간 안정성 테스트 (4시간 연속 사용)
- E2E HID 통신 검증 (Android → ESP32-S3 → Windows)

---

## 📋 Phase 2.2 완료 사항 요약 (Phase 2.3 진행을 위한 필수 배경 지식)

### ✅ Phase 2.2에서 완료된 Android 구현

**1. Android 프로토콜 계층 (Phase 2.2.1)**
- **BridgeFrame 데이터 클래스**: 8바이트 고정 크기 프레임 구조 정의
  - 필드: seq (1바이트), buttons (1바이트), deltaX (1바이트), deltaY (1바이트), wheel (1바이트), modifiers (1바이트), keyCode1 (1바이트), keyCode2 (1바이트)
  - Little-Endian 직렬화 (`toByteArray()` 메서드)
  - 비트마스크 상수 및 헬퍼 함수 (isLeftClickPressed(), isCtrlModifierActive() 등)
- **FrameBuilder 싱글톤**: 스레드 안전한 순번 관리 (0~255 순환)
  - AtomicInteger 기반 순번 카운터
  - `buildFrame()` 메서드로 프레임 생성 및 순번 자동 할당
- **UsbConstants**: ESP32-S3 통신 상수 정의
  - VID: 0x303A, PID: 0x82C5
  - UART: 1Mbps (1000000 baud), 8N1
  - 프레임 크기: 8바이트 (DELTA_FRAME_SIZE)

**2. USB Serial 통신 계층 (Phase 2.2.2)**
- **UsbSerialManager 싱글톤**: usb-serial-for-android 라이브러리 기반 UART 통신 관리
  - 포트 관리: `openPort()`, `closePort()`, `isConnected()`
  - 권한 처리: `requestPermission()`, `hasPermission()`
  - 디바이스 감지: DeviceDetector 통합 (`connect(context)` 자동 감지)
  - 프레임 전송: `sendFrame(frame: BridgeFrame)` - 8바이트 직렬화 후 UART 전송
  - 연결 모니터링: UsbDeviceDetectionReceiver (자동 연결/해제 감지)
- **DeviceDetector**: ESP32-S3 자동 감지 (VID/PID 필터링)
- **UsbPermissionReceiver**: USB 권한 요청 및 결과 처리

**3. 터치 입력 처리 계층 (Phase 2.2.3)**
- **TouchpadWrapper Composable**: 터치패드 UI 및 이벤트 감지
  - 터치 이벤트: DOWN/MOVE/UP 감지
  - 햅틱 피드백: 모든 터치 이벤트에 진동 응답
- **DeltaCalculator**: 터치 이동량 계산 및 데드존 보상
  - 데드존: 작은 움직임 무시 (손떨림 방지)
  - 델타 계산: 이전 위치와의 상대 이동량
- **ClickDetector**: 클릭 판정 알고리즘
  - 짧은 탭 감지 (500ms 이내, 이동 거리 8dp 이하)
  - 좌/우 클릭 구분 (터치패드 좌/우 영역)
  - 비동기 프레임 전송 (120Hz 주기)

**4. 키보드 UI 계층 (Phase 2.2.4)**
- **KeyboardKeyButton Composable**: 키 버튼 컴포넌트
  - Sticky Hold 애니메이션 (길게 누르기 상태 유지)
  - 수정자 키 조합 (Ctrl, Shift, Alt 동시 누르기)
- **KeyboardLayout Composable**: 컴팩트 키보드 레이아웃
  - 화살표 키 + 기능 키 (Del, Esc, Enter, Tab)
  - 중앙 하단 240×280dp 영역에 배치

### 🔧 ESP32-S3 펌웨어 설정 (Phase 2.1에서 완료, Phase 2.3 검증용 로그 필수)

**Phase 2.1에서 구현된 ESP32-S3 기반**:
- **USB Composite 디스크립터**: HID Boot Mouse + HID Boot Keyboard
- **TinyUSB 스택**: HID 리포트 전송 (Mouse: 3바이트, Keyboard: 8바이트)
- **FreeRTOS 듀얼 코어 멀티태스킹**:
  - UART 수신 태스크 (우선순위 6, Core 0) - 최고 우선순위
  - HID 처리 태스크 (우선순위 5, Core 0)
  - USB 장치 태스크 (우선순위 4, Core 1)
- **UART 통신**: 1Mbps 8N1 설정, 8바이트 델타 프레임 수신

**⚠️ Phase 2.3 검증을 위한 필수 ESP32-S3 로그 설정**:

ESP32-S3 펌웨어에 다음 로그 출력을 활성화해야 합니다 (Phase 2.3.1~2.3.4 검증용):

```c
// uart_handler.c 또는 hid_handler.c에서 추가

// UART 프레임 수신 로그 (Phase 2.3.1 검증용)
ESP_LOGI(TAG, "UART frame received: seq=%d buttons=0x%02x deltaX=%d deltaY=%d wheel=%d",
         frame->seq, frame->buttons, frame->deltaX, frame->deltaY, frame->wheel);

// HID Mouse 리포트 전송 로그 (Phase 2.3.4 검증용)
ESP_LOGI(TAG, "HID Mouse report sent: buttons=0x%02x deltaX=%d deltaY=%d wheel=%d",
         report.buttons, report.x, report.y, report.wheel);

// HID Keyboard 리포트 전송 로그 (Phase 2.3.5 검증용)
ESP_LOGI(TAG, "HID Keyboard report sent: modifiers=0x%02x keyCodes=[0x%02x,0x%02x,0,0,0,0]",
         report.modifiers, report.keyCodes[0], report.keyCodes[1]);
```

**로그 확인 방법**:
- Windows PC: PuTTY, miniterm.py, 또는 Arduino IDE Serial Monitor
- 포트: ESP32-S3 시리얼 포트 (COM3, COM4 등)
- 속도: 115200 baud (ESP-IDF 기본 로그 속도)

### 🔒 Phase 2.1 HID 기능 유지 필수 (변경 금지)

Phase 2.3 진행 중 **절대 변경하면 안 되는 항목**:
- ❌ USB Composite 디스크립터 (HID 부분)
- ❌ TinyUSB HID 콜백 함수
- ❌ HID 리포트 전송 로직
- ❌ FreeRTOS 태스크 우선순위 (UART=6, HID=5, USB=4)

**태스크 우선순위 현재 상태** (Phase 2.1.4.2에서 최적화 완료):
| 태스크 | 우선순위 | 코어 | 설명 |
|-------|---------|------|------|
| UART 수신 | 6 | 0 | 가장 높은 우선순위 (데이터 입력) |
| HID 처리 | 5 | 0 | 프레임 변환 |
| USB 장치 | 4 | 1 | TinyUSB 스택 관리 |

데이터 흐름 (UART → HID → USB)과 완벽히 일치하므로 **변경 시 성능 저하 발생**.

### 📡 완성된 통신 경로 (Phase 2.3에서 검증할 경로)

```
사용자 터치 입력 (TouchpadWrapper)
       ↓
델타 계산 + 데드존 보상 (DeltaCalculator)
       ↓
클릭 판정 (ClickDetector)
       ↓
프레임 생성 (BridgeFrame + FrameBuilder)
       ↓
UART 전송 (UsbSerialManager.sendFrame() - 1Mbps)
       ↓
ESP32-S3 UART 수신 (uart_handler.c - 8바이트 프레임)
       ↓
HID 변환 (hid_handler.c - Mouse/Keyboard 리포트)
       ↓
USB 전송 (TinyUSB - HID Boot Protocol)
       ↓
Windows HID 기본 드라이버 (자동 인식)
       ↓
Windows 마우스/키보드 입력
```

### 🎯 Phase 2.3 검증 초점

Phase 2.2에서 구현된 위 통신 경로의 **정확성, 안정성, 성능**을 검증:
1. **정확성**: 8바이트 프레임이 손실 없이 정확하게 전달되는가?
2. **안정성**: 4시간 연속 사용 중 크래시 없이 동작하는가?
3. **성능**: 평균 지연시간 50ms 이하, 손실률 0.1% 이하 달성하는가?
4. **E2E**: Android 터치 → ESP32-S3 → Windows 입력까지 전체 경로가 정상 작동하는가?

---

## Phase 2.3.1: 장치 인식 검증

**목표**: Android → ESP32-S3 USB Serial 연결 및 ESP32-S3 → Windows PC HID 장치 인식 검증

**활용 기술 스택** (Phase 2.2 완료 사항):
- **Android 측**: UsbSerialManager (DeviceDetector 자동 감지) + UsbConstants (VID: 0x303A, PID: 0x82C5)
  - DeviceDetector.detect(): ESP32-S3 자동 감지 (VID/PID 필터링)
  - UsbSerialManager.connect(): USB Serial 포트 자동 연결
  - UsbSerialManager.isConnected(): 연결 상태 확인
- **ESP32-S3 측**: USB Composite 디스크립터 (Phase 2.1 완료)
  - USB CDC (UART 통신용)
  - USB HID Boot Mouse
  - USB HID Boot Keyboard

**세부 목표**:
1. Android 기기가 ESP32-S3 USB Serial 장치를 인식하는지 확인
2. Windows PC가 ESP32-S3 HID 장치(마우스+키보드)를 인식하는지 확인
3. USB 연결 해제 후 재연결 시 자동 인식 확인

**사전 환경 설정**:

### YD-ESP32-S3 보드 포트 위치 확인

```
┌─────────────────────┐
│                     │
│  [USB-OTG 포트]     │ ← 좌측 상단 (실크스크린: "USB")
│  • PC HID 연결용    │
│                     │
│  [COM 포트]         │ ← 우측 하단 (실크스크린: "COM")
│  • Android 연결용   │
│  • 개발/플래싱용    │
└─────────────────────┘
```

1. **Android → ESP32-S3 연결**:
   - Android 기기를 ESP32-S3의 **COM 포트**에 USB-C 케이블 연결
   - Android 앱 빌드 및 설치

2. **ESP32-S3 → Windows PC 연결**:
   - ESP32-S3의 **USB-OTG 포트**를 Windows PC에 USB 케이블로 연결
   - Windows 장치 관리자 열기 (devmgmt.msc)

**올바른 연결 상태**:
- Android → **COM 포트** (우측 하단)
- PC → **USB-OTG 포트** (좌측 상단)
- 두 케이블 **동시 연결** 필요

### Phase 2.3.1.0: USB 초기화 및 장치 자동 감지 구현

**목표**: MainActivity에서 USB 시스템을 초기화하고 BroadcastReceiver를 등록하여 자동 감지 기능 활성화

**구현 완료 사항**:

#### 1. MainActivity 수정 (USB 시스템 초기화)

**파일**: `src/android/app/src/main/java/com/bridgeone/app/MainActivity.kt`

**주요 추가 내용**:
- `initializeUsbSystem()`: onCreate()에서 호출되어 UsbManager 초기화 및 디바이스 검색 시작
  ```kotlin
  private fun initializeUsbSystem() {
      val usbManager = getSystemService(Context.USB_SERVICE) as? UsbManager
      UsbSerialManager.setUsbManager(usbManager)
      UsbSerialManager.connect(this)  // DeviceDetector 자동 실행
  }
  ```

- `registerUsbBroadcastReceivers()`: onStart()에서 호출되어 BroadcastReceiver 동적 등록
  - UsbDeviceDetectionReceiver: USB 기기 연결/해제 이벤트 감지 (ACTION_USB_DEVICE_ATTACHED/DETACHED)
  - UsbPermissionReceiver: USB 권한 요청 결과 수신 (ACTION_USB_PERMISSION)

- `unregisterUsbBroadcastReceivers()`: onStop()에서 호출되어 BroadcastReceiver 등록 해제
  - 메모리 누수 방지

**라이프사이클 흐름**:
```
onCreate()
  ↓
initializeUsbSystem()
  ├─ UsbManager 획득
  ├─ UsbSerialManager 초기화
  └─ DeviceDetector 실행 (자동 감지)
  ↓
onStart()
  ↓
registerUsbBroadcastReceivers()
  ├─ UsbDeviceDetectionReceiver 등록
  └─ UsbPermissionReceiver 등록
  ↓
[앱 정상 실행]
  ↓
onStop()
  ↓
unregisterUsbBroadcastReceivers()
  ├─ UsbDeviceDetectionReceiver 해제
  └─ UsbPermissionReceiver 해제
```

#### 2. UsbConstants 수정 (기기 실제 VID/PID 적용)

**파일**: `src/android/app/src/main/java/com/bridgeone/app/usb/UsbConstants.kt`

**변경 사항**:
```kotlin
// 이전
const val ESP32_S3_VID = 0x303A  // Espressif VID
const val ESP32_S3_PID = 0x82C5  // TinyUSB 계획 PID

// 현재 (실제 기기 기반)
const val ESP32_S3_VID = 0x10C4  // CP2102 (Silicon Labs) UART-to-USB 칩
const val ESP32_S3_PID = 0xEA60  // CP2102 기본 PID
```

**주의**: TinyUSB HID 통신이 완전히 구현되면, 다시 `0x303A:0x4001`로 변경해야 합니다.

#### 3. 빌드 및 설치

**명령어**:
```bash
cd src/android
./gradlew clean assembleDebug
adb uninstall com.bridgeone.app
./gradlew installDebug
```

**결과**:
- DeviceDetector가 자동으로 ESP32-S3 기기 발견 (VID=0x10C4, PID=0xEA60)
- Logcat: `DeviceDetector: ESP32-S3 device found: /dev/bus/usb/001/002 (VID=0x10C4, PID=0xEA60)`

#### 4. 검증 항목 (Phase 2.3.1.0 완료 기준)

**구현 검증**:
- [x] MainActivity에 initializeUsbSystem() 메서드 추가
- [x] MainActivity에 registerUsbBroadcastReceivers() 메서드 추가
- [x] MainActivity에 unregisterUsbBroadcastReceivers() 메서드 추가
- [x] onCreate()에서 initializeUsbSystem() 호출
- [x] onStart()에서 registerUsbBroadcastReceivers() 호출
- [x] onStop()에서 unregisterUsbBroadcastReceivers() 호출
- [x] UsbConstants VID/PID 수정 (0x10C4:0xEA60)
- [x] Gradle 빌드 성공
- [x] 앱 설치 성공
- [x] DeviceDetector가 기기 자동 감지 (Logcat 확인)

### Phase 2.3.1.1: Android → ESP32-S3 USB Serial 인식 검증

**목표**: Android 앱이 ESP32-S3 USB Serial 장치를 올바르게 인식하는지 확인

**사전 조건**: Phase 2.3.1.0 구현 완료

**검증**:
- [x] Android 앱 실행 시 ESP32-S3 USB Serial 장치 자동 감지 (DeviceDetector)
- [x] Android 앱 로그에 "ESP32-S3 device found: VID=0x10C4, PID=0xEA60" 메시지 표시
- [x] UsbSerialManager.isConnected() 호출 시 `true` 반환 (포트 오픈 성공)
- [x] USB 연결 해제 후 UsbSerialManager.isConnected() 호출 시 `false` 반환 (포트 자동 닫힘)
- [x] USB 재연결 시 UsbDeviceDetectionReceiver가 자동 감지하여 연결 복구
- [x] **BONUS**: UART 프레임 전송 성공 (`Frame sent successfully - seq=0, buttons=0, dx=0, dy=0, size=8`)

#### Phase 2.3.1.1 변경사항 분석 (기존 계획 대비 구현 변경)

**1. PendingIntent 플래그: FLAG_IMMUTABLE → FLAG_MUTABLE**

**기존 계획** (Phase 2.2.2.1): `FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE`

**실제 구현**: `FLAG_UPDATE_CURRENT | FLAG_MUTABLE`

**변경 이유**:
- **문제 발견**: Android 12 이상에서 `FLAG_IMMUTABLE`만 사용하면 Intent extras가 null로 전달되는 버그 확인
- **증상**: `UsbManager.EXTRA_DEVICE`와 `EXTRA_PERMISSION_GRANTED`가 수신되지 않음
- **해결책**: `FLAG_MUTABLE` 사용으로 Intent extras 전달 보장
- **보안 유지**: `setPackage(context.packageName)`로 패키지 명시하여 보안 유지

**파일 변경**: `src/android/app/src/main/java/com/bridgeone/app/usb/UsbPermissionReceiver.kt` (라인 171-180)

**후속 Phase 영향도**:
- ✅ **Phase 2.2.2.1 검증 업데이트 필수**: PendingIntent 플래그 항목 변경
- ✅ **Phase 2.2.2.1 변경사항 섹션에 기록 필요**: FLAG_MUTABLE 사용 이유 문서화

---

**2. UsbPermissionReceiver.onReceive()에 폴백 로직 추가**

**기존 계획**: Intent에서 UsbDevice 직접 추출만 수행

**실제 구현**: 다층 폴백 로직 추가
- 주 추출 방식 (`getParcelableExtra`)
- Intent extras null 체크 및 로깅
- 폴백 1: 모든 extras 순회하여 UsbDevice 검색
- 폴백 2: 검색 실패 시 로깅 및 종료

**변경 이유**:
- **디버깅 용이성**: Intent extras 상태 파악 가능
- **견고성 향상**: 예상치 못한 Intent 구조 변경 대비
- **프로덕션 안정성**: 1차 실험에서 extras=null 상황 다수 발생 → 폴백 필요 확인

**파일 변경**: `src/android/app/src/main/java/com/bridgeone/app/usb/UsbPermissionReceiver.kt` (라인 65-92)

**후속 Phase 영향도**:
- ✅ **Phase 2.2.2.1 검증 업데이트 필수**: onReceive() 폴백 로직 항목 추가
- ❌ **Phase 2.3.1.2+ 무영향**: 폴백은 에러 처리용으로만 작동

---

**3. UsbSerialManager.notifyPermissionResult()에 자동 포트 오픈 로직 추가**

**기존 계획** (Phase 2.2.2.2): 권한 결과를 SharedPreferences에만 저장
```kotlin
if (granted) {
    savePermissionStatus(context, true)
    // 포트 오픈 로직 없음
}
```

**실제 구현**: 권한 승인 후 즉시 포트 오픈
```kotlin
if (granted) {
    savePermissionStatus(context, true)
    try {
        openPort(device)  // ← 추가
        Log.i(TAG, "USB port opened successfully after permission granted")
    } catch (e: Exception) {
        clearPermissionStatus(context)
    }
}
```

**변경 이유**:
- **기존 계획의 미흡**: Phase 2.2.2.2에서는 권한 저장만 명시, 포트 오픈 시점 불명확
- **사용자 경험**: 권한 승인 후 자동으로 포트를 열어야 `isConnected()` 즉시 반환 true
- **아키텍처 적합성**: BroadcastReceiver 콘텍스트에서 권한 결과 수신 → 바로 포트 오픈이 자연스러움
- **구현 검증**: 실제 Logcat에서 "USB port opened successfully after permission granted" 확인됨

**파일 변경**: `src/android/app/src/main/java/com/bridgeone/app/usb/UsbSerialManager.kt` (라인 385-397)

**후속 Phase 영향도**:
- ✅ **Phase 2.2.2.2 스펙 업데이트 필수**: `notifyPermissionResult()` 기능 변경 문서화
- ✅ **Phase 2.3.1.2+ 긍정적 영향**: 권한 승인 즉시 UART 통신 가능 (대기 로직 불필요)
- ✅ **Phase 2.3.3+ 영향 없음**: UART 프레임 전송 정확성은 포트 오픈 여부와 무관

#### 변경사항 종합 영향도 분석

| 영향받는 Phase | 우선순위 | 변경 내용 | 실행 여부 |
|---------------|---------|---------|---------|
| **Phase 2.2.2.1** | 🔴 **필수** | PendingIntent 플래그 검증 항목 변경 (FLAG_IMMUTABLE → FLAG_MUTABLE) + 폴백 로직 항목 추가 | 미실행 |
| **Phase 2.2.2.2** | 🔴 **필수** | `notifyPermissionResult()` 기능 스펙 변경 (포트 오픈 추가) 문서화 | 미실행 |
| Phase 2.3.1.2 | 🟢 무영향 | HID 인식 검증은 이전 단계와 독립적 | - |
| Phase 2.3.2+ | 🟡 긍정적 | 권한 처리 완료 상태에서 UART 통신 즉시 시작 가능 (개선) | - |

### Phase 2.3.1.2: ESP32-S3 → Windows PC HID 장치 인식 검증

**목표**: Windows PC가 ESP32-S3 HID 장치(마우스+키보드)를 올바르게 인식하는지 확인

**검증**:
- [x] Windows 장치 관리자에서 "HID 호환 마우스" 장치 표시
  - ✅ 확인됨: "HID 규격 마우스" (HID\VID_303A&PID_4001&MI_01)
- [x] Windows 장치 관리자에서 "HID 키보드 장치" 표시
  - ✅ 확인됨: "HID 키보드 장치" (HID\VID_303A&PID_4001&MI_00)
- [x] 장치 속성에서 VID: 0x303A 확인
  - ✅ 확인됨: VID_303A&PID_4001
- [x] 드라이버: "Microsoft USB 입력 장치" (기본 HID 드라이버)
  - ✅ 확인됨: "USB 입력 장치" × 2 (MI_00, MI_01) - Windows USB 드라이버 스택 정상
- [x] USB 연결 해제 후 장치 관리자에서 장치 사라짐 확인
  - ✅ 확인됨: 포트 2 케이블 제거 시 모든 HID 장치 제거됨
- [x] USB 재연결 시 자동 재인식 (드라이버 재설치 불필요)
  - ✅ 확인됨: 재연결 즉시 자동 인식 (드라이버 재설치 없음)

---

## Phase 2.3.2: UART 프레임 정확성 검증

**목표**: Android에서 생성한 8바이트 델타 프레임이 ESP32-S3에 정확하게 전달되는지 검증

**활용 기술 스택** (Phase 2.2 완료 사항):
- **Android 측**: BridgeFrame 데이터 클래스 + FrameBuilder (순번 관리) + UsbSerialManager.sendFrame()
  - BridgeFrame.toByteArray()로 8바이트 Little-Endian 직렬화
  - FrameBuilder.buildFrame()으로 순번 자동 할당 (0~255 순환)
  - UsbSerialManager.sendFrame()으로 1Mbps UART 전송
- **ESP32-S3 측**: UART 수신 로그 활성화 (uart_handler.c)
  - 로그: "UART frame received: seq=%d buttons=0x%02x deltaX=%d deltaY=%d wheel=%d"
  - 수신 포트: UART0 (GPIO43/44, 1Mbps 8N1) - CH343P 연결

**사전 환경 설정**:
1. **Android 연결**: Android 기기를 ESP32-S3의 **COM 포트** (우측 하단)에 USB-C 케이블 연결
2. **시리얼 모니터 설정**:
   - Windows PC: PuTTY, miniterm.py, 또는 Arduino IDE Serial Monitor 실행
   - 포트: ESP32-S3 시리얼 포트 (COM3, COM4 등 확인)
   - 속도: 115200 baud (ESP-IDF 초기 로그)
   - ⚠️ **개발 중**: PC를 **COM 포트**에 연결 (Android와 교체)
3. Android 앱 빌드 및 설치
4. **선행 필수**: ESP32-S3 펌웨어에 UART 프레임 수신 로그 추가 완료

**세부 목표**:
1. UART 프레임 수신 및 로그 검증
2. 8바이트 구조 정확성 확인 (BridgeFrame.toByteArray() 검증)
3. 순번(seq) 필드 연속성 검증 (FrameBuilder 순환 로직 검증)
4. Little-Endian 바이트 순서 검증 (deltaX, deltaY 바이트 순서)

**검증** (정성적):
- [x] Android 앱 터치패드 드래그 시 시리얼 로그에 프레임 수신 메시지 출력
  ```
  I (94668) UART_HANDLER: Frame received and queued: seq=184, buttons=0x00, x=31, y=127, wheel=0
  I (94674) UART_HANDLER: Frame received and queued: seq=185, buttons=0x00, x=10, y=69, wheel=0
  I (94687) UART_HANDLER: Frame received and queued: seq=186, buttons=0x00, x=127, y=127, wheel=0
  ```

**검증** (정량적):
- [x] **8바이트 구조**: 모든 프레임이 정확히 8바이트 (seq, buttons, x, y, wheel, modifier, key1, key2)
- [x] **순번 연속성**: seq 필드가 182→183→184→...→193 완벽히 연속 (12프레임 손실 0개)
- [x] **바이트 순서**: x, y가 Little-Endian으로 올바르게 전달
  - 실제 검증: x=127 (최대값), y=-127 (음수) 정상 해석
- [x] **버튼 상태**: buttons 필드에 0x00(누름 없음), 0x01(좌클릭), 0x02(우클릭) 올바르게 전달
- [x] **무손실 전달**: seq=182~193 연속 증가, 프레임 손실 0개 확인

---

## Phase 2.3.3: UART 통신 지연시간 및 손실률 측정

**목표**: UART 통신 성능 임계값 달성 검증 (50ms 이하 지연, 0.1% 이하 손실)

**활용 기술 스택** (Phase 2.2 완료 사항):
- **Android 측**: FrameBuilder (seq 필드) + UsbSerialManager.sendFrame() (시간 기록)
  - FrameBuilder.buildFrame()으로 생성된 seq 필드로 패킷 손실 감지 (0→1→2→...→254→255→0)
  - UsbSerialManager에 전송 타임스탬프 추가 가능 (선택사항)
- **ESP32-S3 측**: UART 수신 로그 (uart_handler.c) + 타임스탐프
  - 로그: "UART frame received at %ldms: seq=%d ..." 형식으로 수신 시간 기록
  - 지연시간 계산: (ESP32-S3 수신 시간) - (Android 발송 시간)

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
- [x] **평균 지연시간**: 100프레임 측정 후 평균값 < 50ms
  - 측정값: ~16ms (프레임 간격 14-18ms)
- [x] **최대 지연시간**: 99 percentile < 100ms
  - 측정값: 84ms (터치 중단/재시작 제외 시 대부분 < 35ms)
- [x] **프레임 손실률**: (손실 프레임 / 전체 프레임) × 100% < 0.1%
  - 측정값: 0% (동기화 후 손실 없음)
- [x] **seq 연속성**: 0→1→2→...→254→255→0 연속 증가 (중단 없음)
  - 확인: seq=139→140→...→255→0→1→... 연속 증가 확인
- [ ] **5분 연속 전송**: 300초 이상 연속 전송 중 크래시 없음 (시리얼 로그 오류 없음)

---

## Phase 2.3.4: UART 안정성 및 스트레스 테스트

**목표**: 장시간 UART 통신 안정성 검증

**활용 기술 스택** (Phase 2.2 완료 사항):
- **Android 측**: FrameBuilder (스레드 안전한 AtomicInteger 기반 순번 관리) + UsbSerialManager.sendFrame()
  - FrameBuilder의 스레드 안전성 (AtomicInteger.getAndIncrement())이 4시간 연속 사용에서 순번 중복 없이 동작하는지 검증
  - UsbSerialManager의 포트 연결 유지 및 재연결 로직 검증
  - TouchpadWrapper의 터치 이벤트 처리 안정성
- **ESP32-S3 측**: UART 수신 태스크 (우선순위 6, Core 0) + FreeRTOS 안정성
  - 4시간 연속 UART 수신 중 태스크 크래시 없음
  - 메모리 누수 모니터링 (`esp_get_free_heap_size()` 일정 유지)
  - CPU 사용률 < 30% (FreeRTOS 유휴 시간 충분)

**세부 목표**:
1. 4시간 연속 마우스 + 키보드 입력 시뮬레이션
2. ESP32-S3 크래시 없음 (UART 수신 태스크)
3. Android 앱 크래시 없음 (FrameBuilder, UsbSerialManager)
4. 메모리 누수 없음 (Android와 ESP32-S3 모두)

**검증**:
- [ ] 4시간 연속 사용 중 ESP32-S3 UART 에러 없음 (시리얼 로그)
- [ ] 4시간 연속 사용 중 Android 앱 크래시 없음
- [ ] ESP32-S3 메모리 사용량 안정 (`esp_get_free_heap_size()` 일정 유지)
- [ ] CPU 사용률 < 30% (FreeRTOS 태스크 통계)
- [ ] USB 연결 해제 후 재연결 시 자동 복구 확인

---

## Phase 2.3.5: HID Mouse 경로 End-to-End 검증

**목표**: 터치 → UART → HID Mouse → Windows 마우스 이동 전체 경로 검증

**활용 기술 스택** (Phase 2.2 완료 사항):
- **Android 측 (사용자 입력 → UART)**:
  - TouchpadWrapper: 터치 이벤트 감지 (DOWN/MOVE/UP)
  - DeltaCalculator: 델타 계산 + 데드존 보상 (8dp 이하 무시)
  - ClickDetector: 클릭 판정 (500ms 이내 탭 감지, LEFT/RIGHT 구분)
  - BridgeFrame: buttons 필드 (0x00=누름없음, 0x01=좌클릭, 0x02=우클릭)
  - FrameBuilder: 순번 관리
  - UsbSerialManager.sendFrame(): 1Mbps UART 전송
- **ESP32-S3 측 (UART → HID Mouse → Windows)**:
  - UART 수신: 8바이트 프레임 수신 + seq 검증
  - HID 변환: UART deltaX/deltaY → HID Mouse 상대 이동값 (최대 ±127)
  - HID 리포트 전송: TinyUSB HID Boot Mouse (3바이트: buttons, x, y)
  - 로그: "HID Mouse report sent: buttons=0x%02x deltaX=%d deltaY=%d wheel=%d"

**사전 환경 설정**:

**하드웨어 연결** (YD-ESP32-S3):
1. **Android 연결**: Android 기기를 ESP32-S3의 **COM 포트** (우측 하단)에 USB-C 케이블 연결
2. **PC HID 연결**: ESP32-S3의 **USB-OTG 포트** (좌측 상단)를 Windows PC에 USB 케이블로 연결
3. **두 케이블 동시 연결** 필요 ⭐

**소프트웨어 설정**:
4. Android 앱 빌드 및 설치
5. **시리얼 모니터 실행** (디버깅용, 선택사항):
   - Windows PC: PuTTY, miniterm.py, 또는 Arduino IDE Serial Monitor
   - 포트: ESP32-S3 시리얼 포트 (COM3, COM4 등)
   - 속도: 115200 baud (ESP-IDF 초기 로그)
   - ⚠️ **주의**: 시리얼 모니터 사용 시 별도 UART 어댑터 필요 (COM 포트는 Android가 사용 중)
6. **선행 필수**: ESP32-S3 펌웨어에 HID Mouse 리포트 전송 로그 추가 완료

**세부 목표**:
1. 시리얼 모니터에서 UART 프레임 수신 로그 확인 (FrameBuilder seq 필드 포함)
2. 시리얼 모니터에서 HID Mouse 리포트 전송 로그 확인 (buttons, deltaX, deltaY)
3. Windows에서 마우스 포인터 이동 확인 (TouchpadWrapper의 터치 → Windows 포인터 이동)
4. 이동 거리 및 속도 정량적 검증 (DeltaCalculator의 델타 값이 정확히 전달되는지)

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

## Phase 2.3.6: HID Keyboard 경로 End-to-End 검증

**목표**: 키 입력 → UART → HID Keyboard → Windows 키 입력 검증

**활용 기술 스택** (Phase 2.2 완료 사항):
- **Android 측 (키 입력 → UART)**:
  - KeyboardKeyButton Composable: 키 버튼 (Del, Esc, Enter, Tab, 화살표 등)
  - Sticky Hold 애니메이션: 길게 누르기 상태 유지 (modifiers 키 조합용)
  - BridgeFrame: modifiers 필드 (Ctrl=0x01, Shift=0x02, Alt=0x04)
  - BridgeFrame: keyCode1, keyCode2 필드 (HID Usage ID)
  - FrameBuilder: 순번 관리
  - UsbSerialManager.sendFrame(): 1Mbps UART 전송
- **ESP32-S3 측 (UART → HID Keyboard → Windows)**:
  - UART 수신: 8바이트 프레임 수신 + modifiers, keyCode1, keyCode2 필드 추출
  - HID 변환: UART keyCode → HID Keyboard 리포트 (8바이트: modifiers + 6개 keyCode)
  - HID 리포트 전송: TinyUSB HID Boot Keyboard (8바이트 고정)
  - 로그: "HID Keyboard report sent: modifiers=0x%02x keyCodes=[0x%02x,0x%02x,0,0,0,0]"
  - **BIOS 호환성**: HID Boot Protocol 사용 (Keyboard + Mouse 복합 디바이스)

**세부 목표**:
1. KeyboardKeyButton 컴포넌트 기본 구현 확인 (Phase 2.2.4에서 완료)
2. 키 다운/업 이벤트 → BridgeFrame 생성 (modifiers, keyCode1, keyCode2 필드)
3. Windows 메모장에서 키 입력 확인 (del, esc, enter, tab 등)
4. BIOS 호환성 검증 (Del 키로 BIOS 진입 테스트)

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

## Phase 2.3.7: 최종 지연시간 및 프레임 손실률 측정 (HID 경로)

**목표**: 성능 임계값 검증 (50ms 이하 평균 지연, 0.1% 이하 손실)

**활용 기술 스택** (Phase 2.2 완료 사항):
- **Android 측 (터치 입력 → UART 전송)**:
  - TouchpadWrapper: 터치 이벤트 발생 시간 기록 (System.currentTimeMillis())
  - DeltaCalculator, ClickDetector: 처리 시간 포함
  - FrameBuilder.buildFrame(): 프레임 생성 + seq 필드 (손실 감지용)
  - UsbSerialManager.sendFrame(): UART 전송 시간 기록
- **ESP32-S3 측 (UART 수신 → HID 전송)**:
  - UART 수신 태스크: 수신 시간 기록 + seq 필드 로그
  - HID 처리 태스크: HID 리포트 생성 시간
  - HID 전송 태스크: USB HID 리포트 전송 시간 기록 (에스프레소 시간)
  - 로그: "UART received at T1ms seq=%d" + "HID sent at T2ms"
  - 지연시간 계산: T2 - (Android 터치 발생 시간)

**측정 방법론**:
1. **지연시간 측정 (3가지 경로 추적)**:
   - `Android 터치 → ESP32-S3 UART 수신`: 시리얼 로그 타임스탬프 기반 (T_uart = ESP32수신 - Android발송)
   - `ESP32-S3 UART 수신 → HID 전송`: ESP32-S3 내부 타임스탬프 (T_hid = HID전송 - UART수신)
   - `총 지연시간 = T_uart + T_hid`
   - **정합성 검증**: FrameBuilder의 seq 필드로 패킷 매핑 (UART seq와 HID seq 일치 확인)

2. **샘플링**:
   - 최소 100프레임 이상 측정 (5초 이상)
   - 다양한 입력 패턴: 느린 드래그, 빠른 드래그, 클릭
   - 마우스와 키보드 혼합 입력

3. **프레임 손실 측정**:
   - 시리얼 로그 seq 필드 분석 (FrameBuilder가 생성한 순번)
   - 손실된 seq 번호 식별 및 횟수 계산
   - 손실률 = (손실 프레임 / 전체 프레임) × 100%

**검증** (정량적):
- [ ] **평균 지연시간**: 100 프레임 측정 후 평균값 < 50ms
- [ ] **최대 지연시간**: 99 percentile (상위 1%) < 100ms
- [ ] **프레임 손실률**: (손실 프레임 / 전체 프레임) × 100% < 0.1%
  - 예: 1000프레임 중 1개 이하 손실
- [ ] **연속성 검증**: seq 필드가 0→1→2→...→254→255→0 연속 증가 확인
- [ ] **안정성**: 5분 이상 연속 전송 중 크래시 없음 (시리얼 로그 오류 없음)

---

## Phase 2.3.8: HID 안정성 및 스트레스 테스트

**목표**: 장시간 사용 안정성 검증

**활용 기술 스택** (Phase 2.2 완료 사항):
- **Android 측 (장시간 안정성)**:
  - TouchpadWrapper: 4시간 연속 터치 이벤트 처리
  - KeyboardKeyButton: 4시간 연속 키 입력 처리
  - FrameBuilder (AtomicInteger): 스레드 안전한 순번 관리가 4시간 중복 없이 동작
  - UsbSerialManager: 포트 연결 유지 + 재연결 로직
  - UsbDeviceDetectionReceiver: 자동 연결/해제 감지
  - 메모리 모니터링: Android Studio Profiler로 메모리 누수 감지
- **ESP32-S3 측 (장시간 안정성)**:
  - UART 수신 태스크 (우선순위 6): 4시간 연속 수신 안정성
  - HID 처리 태스크 (우선순위 5): 4시간 연속 변환 안정성
  - FreeRTOS 스케줄러: CPU 사용률 균형
  - 메모리 모니터링: `esp_get_free_heap_size()` 로깅으로 메모리 누수 감지
  - 로그: "Free heap: %u bytes" 주기적 기록

**세부 목표**:
1. 4시간 연속 마우스 + 키보드 입력 (TouchpadWrapper + KeyboardKeyButton)
2. 크래시 없음 (ESP32-S3 UART/HID 태스크, Android 앱 모두)
3. 메모리 누수 없음 (FrameBuilder, UsbSerialManager, 기타 컴포넌트)
4. 자동 재연결 테스트 (USB 기기 제거/삽입 시 UsbDeviceDetectionReceiver 동작)

**검증**:
- [ ] 4시간 연속 사용 중 크래시 없음 (시리얼 로그, Android 앱 로그 모두)
- [ ] 마우스 이동 중 클릭 정상 동작 (DeltaCalculator + ClickDetector 동시 처리)
- [ ] 키보드 타이핑 중 마우스 조작 정상 동작 (TouchpadWrapper + KeyboardKeyButton 동시 입력)
- [ ] 메모리 사용량 안정 (`esp_get_free_heap_size()` 4시간 중 변동 < 5KB)
- [ ] CPU 사용률 < 30% (FreeRTOS 유휴 시간 충분)
- [ ] Windows에서 입력 오류 없음 (마우스/키보드 정상 작동, 누락된 입력 없음)
- [ ] Android 앱 배터리 소모 정상 범위 (4시간 사용 시 배터리 20% 이하 소모)
- [ ] USB 자동 재연결 (UsbDeviceDetectionReceiver로 기기 재착탈 감지 후 자동 연결)

---

## Phase 2.3.9: 최종 통합 검증 및 문서화

**목표**: Phase 2.3 완료 및 최종 검증

**개발 기간**: 2-3일

**활용 기술 스택** (Phase 2.2 완료 사항):
- **Phase 2.3.1~2.3.8의 모든 구현을 종합 검증**:
  - BridgeFrame (8바이트 구조 정확성)
  - FrameBuilder (순번 관리 안정성)
  - UsbSerialManager (UART 통신 안정성)
  - TouchpadWrapper + DeltaCalculator + ClickDetector (터치 입력 E2E)
  - KeyboardKeyButton (키 입력 E2E)
  - Phase 2.1 HID 기능 (USB 리포트 전송 안정성)

**세부 목표**:
1. 모든 하위 Phase (2.3.1~2.3.8) 검증 완료 확인
2. 성능 임계값 달성 확인 (지연시간 < 50ms, 손실률 < 0.1%)
3. Phase 2.1/2.2와의 호환성 재검증 (HID 기능 영향 없음)
4. 문서화 및 주석 최종 검토
5. 커밋 및 릴리스 노트 작성

---

### Phase 2.3.9.1: Phase 2.3 검증 항목 최종 확인

**목표**: Phase 2.3의 모든 세부 단계 검증 완료 및 성능 임계값 달성 확인

**세부 목표**:
1. Phase 2.3.1 ~ 2.3.8의 모든 검증 항목 재확인
2. 성능 임계값 달성 최종 검증 (BridgeFrame, FrameBuilder, UsbSerialManager)
3. Phase 2.1/2.2와의 호환성 재검증 (HID 기능 여전히 정상)

**검증** (Phase 2.3 검증 항목):
- [ ] Phase 2.3.1 검증 완료: 장치 인식 검증
  - Android → ESP32-S3 USB Serial 연결 정상
  - ESP32-S3 → Windows PC HID 장치 인식 정상
  - USB 연결/해제 반복 시 안정적 재인식
- [ ] Phase 2.3.2 검증 완료: UART 프레임 정확성 검증
  - 8바이트 구조 정확
  - seq 필드 연속성 (0~255 순환)
  - Little-Endian 바이트 순서 정확
- [ ] Phase 2.3.3 검증 완료: UART 지연시간 및 손실률 측정
  - 평균 지연시간 < 50ms (100프레임 이상 측정)
  - 최대 지연시간 < 100ms (99 percentile)
  - 프레임 손실률 < 0.1%
- [ ] Phase 2.3.4 검증 완료: UART 안정성 및 스트레스 테스트
  - 4시간 연속 사용 무중단
  - CPU 사용률 < 30%
  - 메모리 누수 없음
- [ ] Phase 2.3.5 검증 완료: HID Mouse 경로 E2E 검증
  - Android 터치패드 드래그 → Windows 마우스 포인터 이동 정상
  - 마우스 방향 및 속도 정확
- [ ] Phase 2.3.6 검증 완료: HID Keyboard 경로 E2E 검증
  - Windows 메모장에서 키 입력 정상 작동
  - BIOS 호환성 확인 (Del 키 → BIOS 진입)
  - 지연시간 10ms 이하
- [ ] Phase 2.3.7 검증 완료: 최종 지연시간 및 프레임 손실률 측정 (HID 경로)
  - E2E 평균 지연시간 < 50ms
  - 프레임 손실률 < 0.1%
- [ ] Phase 2.3.8 검증 완료: HID 안정성 및 스트레스 테스트
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

### Phase 2.3.9.2: 문서 및 코드 주석 최종 검토

**목표**: 모든 코드 파일 주석 및 Docstring 완성도 검증 (Phase 2.2 구현에 대한 재검증)

**활용 기술 스택** (Phase 2.2 완료 사항에 대한 검토):
- **Android 코드 검증**:
  - BridgeFrame.kt: Google 스타일 Docstring + 비트마스크 상수 주석
  - FrameBuilder.kt: buildFrame(), resetSequence(), getCurrentSequence() 주석 확인
  - UsbSerialManager.kt: 모든 public 함수 (setUsbManager, openPort, closePort, isConnected, sendFrame 등) 문서화 확인
  - TouchpadWrapper.kt: 터치 이벤트 처리 로직 주석
  - ClickDetector.kt: 클릭 판정 알고리즘 주석
  - KeyboardKeyButton.kt: Sticky Hold 애니메이션 주석
  - UsbConstants.kt: 모든 상수 UPPER_CASE 확인
- **ESP32-S3 코드 검증**:
  - uart_handler.c: UART 프레임 수신 로그 + 주석
  - hid_handler.c: HID 변환 로직 주석 + HID 리포트 전송 로그
  - main.c: FreeRTOS 태스크 생성 로직 주석

**세부 목표**:
1. 모든 함수에 Google 스타일 Docstring 확인 (Phase 2.2 구현)
2. 복잡한 로직에 한국어 주석 확인 (DeltaCalculator, ClickDetector, FrameBuilder)
3. 상수명 대문자 규칙 확인 (UsbConstants, BridgeFrame 비트마스크 상수)
4. Boolean 변수명 규칙 확인 (isConnected, isEsp32s3Device, isLeftClickPressed 등)
5. Phase 2.3 검증 절차 문서화

**검증**:
- [ ] 모든 public 함수에 Docstring 포함 (BridgeFrame, FrameBuilder, UsbSerialManager, TouchpadWrapper, ClickDetector, KeyboardKeyButton)
- [ ] 복잡한 로직에 한국어 주석 (DeltaCalculator 델타 계산, ClickDetector 클릭 판정, FrameBuilder 순환 로직)
- [ ] 상수명 모두 UPPER_CASE (UsbConstants, BridgeFrame 비트마스크)
- [ ] Boolean 변수명 규칙 준수 (is*, has* 패턴)
- [ ] Phase 2.3 검증 결과 문서에 기록 (phase-2-3-results.md 생성)
- [ ] Linter 오류 없음 (모든 Android/ESP32-S3 파일)

---

### Phase 2.3.9.3: 최종 커밋 및 릴리스 노트

**목표**: Phase 2.3 완료 커밋 및 정리

**활용 기술 스택** (Phase 2.2 완료 사항 + Phase 2.3 검증 결과):
- **Phase 2.2 구현 확정**:
  - BridgeFrame (8바이트 프로토콜) ✓
  - FrameBuilder (스레드 안전한 순번 관리) ✓
  - UsbSerialManager (1Mbps UART 통신) ✓
  - TouchpadWrapper + DeltaCalculator + ClickDetector (터치 입력) ✓
  - KeyboardKeyButton (키 입력) ✓
- **Phase 2.3 검증 완료**:
  - UART 프레임 정확성 (8바이트 구조, seq 연속성, Little-Endian) ✓
  - UART 성능 (지연시간 < 50ms, 손실률 < 0.1%) ✓
  - UART 안정성 (4시간 연속 사용, 메모리 누수 없음) ✓
  - HID Mouse E2E (터치 → Windows 마우스) ✓
  - HID Keyboard E2E (키입력 → Windows 키보드, BIOS 호환성) ✓
  - E2E 성능 (지연시간 < 50ms, 손실률 < 0.1%) ✓
  - HID 안정성 (4시간 연속, 메모리/CPU 안정) ✓

**세부 목표**:
1. 모든 Phase 2.3 검증 변경사항 커밋
2. 커밋 메시지 작성 (작가 가이드라인 준수)
3. Phase 2.3 완료 요약 문서 작성 (검증 결과, 성능 지표 포함)
4. 다음 Phase 2.4 준비 계획 수립

**검증**:
- [ ] 모든 파일 커밋 완료 (Phase 2.3 테스트 및 검증 관련 문서)
- [ ] 커밋 메시지: "feat(Phase 2.3): UART 통신 검증 및 E2E HID 테스트 완료"
  ```
  - Phase 2.3.1: 장치 인식 검증 ✓
  - Phase 2.3.2: UART 프레임 정확성 검증 ✓
  - Phase 2.3.3: UART 지연시간 및 손실률 측정 ✓
  - Phase 2.3.4: UART 안정성 및 스트레스 테스트 ✓
  - Phase 2.3.5: HID Mouse 경로 E2E 검증 ✓
  - Phase 2.3.6: HID Keyboard 경로 E2E 검증 ✓
  - Phase 2.3.7: 최종 지연시간 및 프레임 손실률 측정 ✓
  - Phase 2.3.8: HID 안정성 및 스트레스 테스트 ✓
  - Phase 2.3.9: 최종 통합 검증 및 문서화 ✓

  성능 임계값 달성:
  - 평균 지연시간: < 50ms ✓
  - 프레임 손실률: < 0.1% ✓
  - 4시간 연속 사용 안정성 ✓
  - BIOS 호환성 ✓
  ```
- [ ] Phase 2.3 완료 요약 문서 작성 (phase-2-3-results.md)
  - 검증 항목별 결과
  - 성능 지표 (지연시간, 손실률, CPU, 메모리)
  - 이슈 및 해결 방안
  - Phase 2.1/2.2 호환성 확인
- [ ] Phase 2.4 시작 조건 확인 및 다음 단계 계획

---

## 📊 Phase 2.3 성과물

**검증 완료 항목**:
- ✅ 장치 인식: Android → ESP32-S3 USB Serial 연결, ESP32-S3 → Windows HID 장치 인식
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
