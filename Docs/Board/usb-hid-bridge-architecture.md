---
title: "USB HID 브리지 아키텍처 및 구현 가이드"
description: "OTG Y 케이블 기반 Android ↔ USB-Serial ↔ ESP32-S3 ↔ PC(HID) 유선 경로 최종 구성"
tags: ["esp32-s3", "tinyusb", "usb", "hid", "otg-y-cable", "charge-while-otg"]
version: "v0.2"
owner: "Chatterbones"
updated: "2025-09-19"
note: "본 문서에 존재하는 모든 상수값 및 설정값은 초기 값으로, 확정된 고정값이 아님"
---

# USB HID 브리지 아키텍처 및 구현 가이드

## 시스템 아키텍쳐

> **상세 아키텍처**: 전체 시스템 아키텍처는 [`technical-specification.md` §3]를 참조하세요.

## 용어집/정의

- Essential/Standard: Windows 서버와 연결되지 않은 상태는 Essential(필수 기능), 연결된 상태는 Standard(모든 기능)입니다. 본 문서의 기본 전제는 Essential(부트 마우스)입니다.
- Selected/Unselected: 선택 상태. 본 문서에서는 UI 시각보다 프로토콜에 초점, 필요 시 참조 문서로 위임.
- Enabled/Disabled: 입력 가능 상태. 앱/UI 정책 참조.
- TransportState: NoTransport | UsbOpening | UsbReady.
- 상태 용어 사용 원칙(금칙어 포함): "활성/비활성" 대신 Selected/Unselected, Enabled/Disabled 사용 [[memory:5809234]].

---

## 1. 준비물

- **OTG Y 케이블**
  - **유형**: USB-C OTG Y 케이블 (Charge-while-OTG 지원)
  - **포트 구성**: 
    - USB-C 플러그: 스마트폰 연결용
    - USB-A 포트 (Device/OTG): USB-Serial 어댑터 연결용  
    - USB-C 포트 (Power/PD): 충전기 연결용
  - **핵심 기능: OTG + 동시 충전**
    - 스마트폰이 USB Host 역할을 수행하면서 동시에 충전을 가능하게 하는 필수 케이블입니다.
  - **프로젝트 내 역할**
    - 장시간 사용 시 배터리 소모를 방지하며 안정적인 연결을 보장합니다.
    - 하나의 케이블로 데이터 통신과 전원 공급을 동시에 해결하는 핵심 부품입니다.

- **USB‑Serial 어댑터 (3.3V TTL)**
  - **칩셋**: `CP2102`
  - **사양**: 3.3V 로직(TX/RX/GND), `VCC` 미사용
  - **VID/PID**: CP2102(`0x10C4`/`0xEA60`)
  - **핵심 기능: 신호 변환기 (번역가)**
    - 스마트폰이 사용하는 **USB 통신 방식**과 ESP32-S3 보드가 사용하는 **UART(시리얼) 통신 방식**을 변환합니다.
  - **프로젝트 내 역할**
    - OTG Y 케이블의 Device 포트에 연결되어 Android 앱의 마우스 데이터를 ESP32-S3로 전달합니다.

- **점퍼 케이블** (암‑암 Dupont 3가닥)
  - **핵심 기능: 데이터 통로 (핏줄)**
    - 전자 부품 간 신호를 주고받는 물리적인 연결선입니다.
  - **프로젝트 내 역할**
    - `USB-Serial 어댑터`와 `ESP32-S3 보드` 사이의 물리적인 데이터 통로를 만듭니다. 3가닥의 선은 각각 다음과 같은 역할을 합니다.
      - `TX` (송신): 데이터를 보내는 선
      - `RX` (수신): 데이터를 받는 선
      - `GND` (접지): 안정적인 통신을 위한 기준 전압을 맞춰주는 선 (필수)
    - 한쪽의 `TX`는 다른 쪽의 `RX`에 연결하여, 서로 데이터를 주고받을 수 있게 합니다.

- **ESP32‑S3 보드** (Geekble nano ESP32‑S3)
  - **핵심 기능: 중앙 처리 장치 (두뇌)**
    - 이 프로젝트의 핵심 두뇌입니다. 두 가지 다른 통신 방식을 중개하고, 수신한 데이터를 가공하여 새로운 형태의 신호로 만들어 내보내는 모든 처리를 담당합니다.
  - **프로젝트 내 역할**
    - **입력 처리**: `USB-Serial 어댑터`로부터 UART 신호 형태로 마우스 데이터를 수신합니다.
    - **데이터 변환**: 수신한 데이터를 PC가 이해할 수 있는 표준 **USB HID(Human Interface Device) 마우스 프로토콜** 형식으로 변환합니다.
    - **출력 처리**: 변환된 HID 신호를 자신의 USB 포트를 통해 PC로 전송합니다. PC는 이 보드를 일반적인 USB 마우스로 인식하게 됩니다.

- **충전기** (5V 2A 이상 권장)
  - **사양**: USB-C PD 지원 권장 (30-60W)
  - **역할**: OTG Y 케이블의 Power 포트에 연결하여 스마트폰 충전
  - **주의사항**: 일부 스마트폰은 OTG 모드에서 충전 아이콘이 표시되지 않을 수 있으나 정상입니다.

- **USB 케이블** (ESP32‑S3↔PC)
  - **유형**: USB-C to USB-A 데이터 케이블 (충전 전용 케이블 사용 금지)
  - **역할**: ESP32-S3 보드에 전원을 공급하며, HID 마우스 신호를 PC에 전달
  - **주의사항**: 반드시 데이터 통신이 가능한 케이블을 사용해야 합니다.

---

## 2. 시스템 아키텍처

연결 구조:
```
스마트폰 ─USB-C─> OTG Y 케이블 ┬─Device포트─> USB-Serial ─UART─> ESP32-S3 ─USB─> PC
                              └─Power포트─> 충전기(PD)
   앱(8B 델타)                     TX/RX/GND           HID Boot Mouse    표준 HID
```

- **스마트폰**: OTG Y 케이블을 통해 Host 모드로 동작하며 동시에 충전
- **USB-Serial**: OTG Y 케이블의 Device 포트에 연결되어 USB↔UART 변환
- **ESP32-S3**: UART로 수신한 8바이트 프레임을 USB HID Boot Mouse로 변환
- **PC**: 부팅 전 단계(BIOS/BitLocker/UAC) 및 서버 미연결 상태에서 표준 마우스로 인식

지연 예측(참고): UART 직렬화 ≈ 0.08 ms(1 Mbps, 8B) + USB HID 폴링 1 ms → 체감 1–2 ms.

### 2.1 엔드투엔드 경로/신뢰성 가정
- 연결 경로: 유선(USB‑OTG) 우선. 본 문서는 USB 경로를 표준으로 정의합니다.
- 신뢰성 가정: PC 보안/부팅 단계에서 HID Boot Mouse로 인식되어야 하며, 앱/동글 분리 시에도 안전 정지(중립 프레임)가 보장되어야 합니다.
- 성능 목표: 전송 주기 4–8 ms(125–250 Hz), 엔드투엔드 입력 지연 50 ms 이하.

### 2.2 상태머신/핸드셰이크
- TransportState: {NoTransport, UsbOpening, UsbReady}
- Essential ↔ Standard 전환: Windows 서버와의 연결 신호 확인 시 Standard로 전환. 서버 미연결 시 Essential로 동작.
- Keep‑alive: 앱→동글 0.5초 주기. 누락 3회 시 연결 불안정으로 간주하고 재연결 백오프(1s→2s→4s→8s) 적용.

### 2.3 성능/지연 측정 방법
- 타임스탬프 기반 RTT: 앱 송신 시퀀스 번호(`seq`)와 동글 회신 로그 비교.
- 오실로스코프: 터치 이벤트 → UART TX 라인 → USB SOF 간 지연 샘플링.
- 목표: P95 < 20 ms(펌웨어/링크), 앱 포함 E2E < 50 ms.

---

## 3. 프로토콜

> 본 절은 요약입니다. 규범적 상세 명세는 `Docs/technical-specification.md`의 프로토콜 섹션을 단일 출처로 참조하십시오.

중앙 상수 표 참조: `Docs/technical-specification.md` §1.1(상수/임계값 표) — `FRAME_SIZE_BYTES`, `TX_PERIOD_MS`, `KEEP_ALIVE_INTERVAL_MS`, `HID_SPLIT_DELTA_LIMIT` 등.

프레임 형식은 아래와 같습니다. 모든 필드는 Little‑Endian 기준입니다.

| 바이트 | 필드    | 형식  | 설명                                  |
|------:|---------|-------|---------------------------------------|
| 0     | seq     | u8    | 순번(유실/역전 로깅용)                |
| 1     | buttons | u8    | bit0 L, bit1 R, bit2 M                |
| 2..3  | dx      | i16   | 상대 X                                |
| 4..5  | dy      | i16   | 상대 Y                                |
| 6     | wheel   | i8    | 휠(부트 단계 비사용, OS 진입 후 선택) |
| 7     | flags   | u8    | 예약                                   |

- 전송 주기: 4–8 ms(125–250 Hz) 권장
- 큰 델타는 동글(ESP32‑S3)에서 8비트 한계(−127…+127)에 맞게 분할 처리(클리핑 방지)

### 3.1 `seq`/유실 판정 및 재동기화
- `seq`는 0..255 래핑. 수신측은 `(seq - prev_seq) mod 256`이 1이 아니면 유실/역전으로 로깅합니다.
- 재동기화(resync): UART 스트림에서 8바이트 정렬이 깨질 수 있으므로 8바이트 윈도우로 전진하며 유효성(keep‑alive 또는 합리적 델타) 검사를 통과하는 경계에서 재정렬합니다.

### 3.2 `flags` 비트 정의
- bit0 `IS_KEEPALIVE`: 헬스체크 프레임(버튼/델타 0). 수신측은 상태만 갱신.
- bit1 `EMERGENCY_STOP`: 즉시 모든 버튼 Up, 이동 0으로 중립화.
- bit2 `ACTIVE_CURSOR_ID`: 0=A, 1=B(멀티 커서 전환 힌트).
- bit3 `RESERVED_FOR_SCROLLING`: 무한 스크롤 내부 상태 힌트(선택).
- bit4 `RESERVED_RIGHT_ANGLE_MOVE`: 축 스냅 활성 힌트(선택).
- bit5..7 예약.

---

## 4. 하드웨어 연결

### 4.1 스마트폰 측 연결

#### 확정 구성: OTG Y 케이블 연결
1. **스마트폰 ↔ OTG Y 케이블**: USB-C 플러그를 스마트폰에 연결
2. **OTG Y 케이블 Device 포트 ↔ USB-Serial 어댑터**: 데이터 통신용
3. **OTG Y 케이블 Power 포트 ↔ 충전기**: 5V 2A 이상 (PD 30-60W 권장)

#### 연결 확인 사항:
- OTG Y 케이블의 Device/Power 포트를 혼동하지 않도록 라벨 확인
- 충전기 연결 후에도 OTG 기능이 정상 동작하는지 확인
- Android 앱에서 USB-Serial 어댑터가 정상 인식되는지 확인

### 4.2 ESP32-S3 측 연결 (UART 배선)

#### 점퍼 케이블 연결:
- **USB-Serial TX → ESP32-S3 RX** (예: GPIO17)
- **USB-Serial RX → ESP32-S3 TX** (예: GPIO18)  
- **USB-Serial GND ↔ ESP32-S3 GND** (공통 접지 필수)
- **VCC 미연결**: 전원 충돌 방지를 위해 연결하지 않음

#### 기술 사양:
- **UART 포트**: UART1 사용 (UART0는 플래싱/디버그용으로 예약)
- **시리얼 설정**: 1,000,000 bps, 8N1, No parity, No flow control
- **양방향 통신**: Android ↔ PC 간 확장 기능을 위한 양방향 데이터 전송 지원

### 4.3 전체 연결 다이어그램

```
[스마트폰]
    │
[OTG Y 케이블]
    ├─Device포트─[USB-Serial]─┬─TX──→RX─┐
    │                         ├─RX←──TX─┤ [ESP32-S3]──USB──[PC]
    │                         └─GND──GND─┘
    └─Power포트───[충전기]
```

---

## 5. ESP32‑S3 펌웨어

목표는 TinyUSB로 HID Boot Mouse를 구성하고, 1ms 간격에서 USB를 서비스하면서 UART로 들어오는 8바이트 프레임을 수신 즉시 변환해 `tud_hid_report()`로 주입하는 것입니다.
**또한, PC의 Windows 서비스와 양방향 통신을 위해 Vendor-Defined 또는 CDC-ACM 인터페이스를 함께 구성해야 합니다.**

### 5.1 HID Report Descriptor(부트 마우스)

3버튼 + X/Y 상대 이동만 포함된 Boot Protocol 호환 디스크립터를 사용합니다. OS 단계에서 휠이 필요하면 Report Protocol 확장으로 별도 리포트를 추가할 수 있습니다.

#### 5.1.1 Boot ↔ Report 프로토콜 전환
- 동글은 호스트의 `SET_PROTOCOL(BOOT|REPORT)` 요청을 처리해 현재 프로토콜 상태를 유지해야 합니다.
- BOOT 모드: wheel=0 강제, 3버튼+X/Y만 보고.
- REPORT 모드: 별도 리포트 또는 확장 필드로 휠 보고 허용.

### 5.2 전송 루프 설계

- FreeRTOS 주기 태스크(1 ms)에서 `tud_task()` 호출 유지
- UART 수신 버퍼 크기는 8바이트 단위, 부분 수신은 다음 주기로 보완
- 입력 우선순위: Click > Wheel > Move

#### 5.2.1 재동기화 및 분할 주입
- 8바이트 정렬이 깨진 경우 1바이트씩 전진하며 유효 경계에서 재정렬(§3.1).
- `i16` 델타는 −127..127 범위로 분할하여 다중 HID 리포트로 주입(§5.3).

### 5.3 CDC/Vendor 인터페이스 구성 및 커스텀 명령 처리

목표: Android에서 전송된 커스텀 명령(매크로, 멀티 커서 등)을 ESP32-S3에서 수신하고, TinyUSB Vendor/CDC 인터페이스를 통해 PC Windows 서버로 전달하는 구현 가이드를 제공합니다.

핵심 기능 요구사항:
- 복합 USB 장치 설정: HID + Vendor 또는 CDC 인터페이스 구성
- Vendor/CDC 인터페이스: 양방향 데이터 전송 지원
- UART 처리: 바이트 읽기/쓰기, 버퍼 관리
- JSON 파싱: 커스텀 명령 구문 분석 및 처리
- FreeRTOS 큐: 태스크 간 안전한 데이터 전달
- 태스크 관리: 비동기 작업 생성 및 관리

디스크립터-구현 요구사항:
- 복합 장치 구성: HID(인터페이스 0) + Vendor(인터페이스 1) 또는 HID + CDC-ACM
- 엔드포인트 할당: HID IN, Vendor IN/OUT 또는 CDC Data IN/OUT
- 버퍼 크기: Vendor 64바이트, CDC 512바이트, UART 수신 1024바이트 권장
- 타이밍: USB 태스크 1ms 주기, UART 폴링 10ms, 커스텀 명령 응답 최대 2초

명령 처리 표준(Android ↔ PC 브리지):
- UART 수신 프레임 구분:
  - 0xFF 헤더: 커스텀 명령(Android → PC)
  - 일반 8바이트: HID 마우스 프레임
  - 파싱 실패 시 1바이트 전진 후 재시도
- 커스텀 명령 JSON 구조 예시: 매크로 시작 요청
- 양방향 통신: PC → ESP32-S3 → Android 경로로 응답 중계
- 오류 처리: JSON 파싱 실패 시 오류 응답, 타임아웃 시 자동 정리

구현 요구사항:
- 복합 장치 설정: HID와 Vendor/CDC 인터페이스 활성화
- 커스텀 명령 처리: UART 버퍼에서 데이터 수신 및 처리 루프
- JSON 명령 파싱: 수신된 명령을 JSON으로 파싱 후 PC로 전달
- Vendor 인터페이스 전송: 파싱된 명령을 Vendor 채널로 PC 전송

접근성/안정성:
- UART 오버런 방지: 하드웨어 플로우 컨트롤 비활성화, 소프트웨어 버퍼링
- JSON 메모리 관리: 메모리 누수 방지, 스택 기반 파싱 선호
- USB 연결 감지: 연결 확인 후 데이터 전송

### 5.4 양방향 통신 관리 (Android ↔ PC) - usb-serial-for-android 라이브러리 통합

**⚠️ 라이브러리 도입 개선**: Android 측에서 `usb-serial-for-android` 라이브러리의 `SerialInputOutputManager`를 활용한 완벽한 양방향 통신 구현으로 개발 복잡도를 대폭 감소시킵니다.

목표: ESP32-S3를 중계점으로 하여 Android 앱과 PC Windows 서버 간의 안정적인 양방향 통신을 구현합니다.

양방향 통신 요구사항:
- 큐 기반 통신: 큐 생성, 송수신, ISR 안전 함수 지원
- 큐 셋 관리: 다중 큐 관리 및 선택적 대기
- 태스크 간 동기화: 뮤텍스 기반 안전한 데이터 공유
- USB 상태 확인: 연결 상태 모니터링
- 타이머 관리: 주기적 작업 스케줄링

아키텍처 구성:
- **송신 플로우**: Android → UART → 큐1 → 처리 태스크 → Vendor/CDC → PC
- **응답 플로우**: PC → Vendor/CDC → 큐2 → 처리 태스크 → UART → Android
- 큐 구성: 명령 큐(32개 항목), 응답 큐(16개 항목), 각 항목 최대 512바이트
- 태스크 우선도: UART 수신(우선도 5), USB 처리(우선도 4), 명령 처리(우선도 3)

동시성 처리 표준:
- UART 수신 태스크: ISR에서 큐로 데이터 전달, 오버플로우 시 오래된 데이터 드롭
- USB 송수신 태스크: 논블로킹 호출, 안전한 큐 전달, 연결 상태 모니터링
- 명령 처리 태스크: 다중 큐 대기, JSON 파싱, 타임아웃 처리

메모리 관리:
- 정적 할당 우선: 큐와 태스크 스택을 컴파일 타임에 할당
- 동적 JSON 객체: 파싱 후 즉시 메모리 해제
- 순환 버퍼: UART 데이터를 위한 링 버퍼 사용

오류 복구:
- USB 연결 해제: 큐 내용 정리 후 재연결 대기
- UART 오버런: 버퍼 리셋 및 동기화 프레임 전송
- JSON 파싱 실패: 오류 응답 전송 후 다음 프레임으로 이동
- 큐 포화: 오래된 항목 드롭 정책 적용



## 6. 하드웨어 연결 검증 가이드

### 6.1 단계별 연결 확인

#### 1단계: OTG Y 케이블 연결 확인
```
✓ 스마트폰에 OTG Y 케이블 연결
✓ Device 포트에 USB-Serial 어댑터 연결  
✓ Power 포트에 충전기 연결
```

**확인 방법:**
- Android 알림창에서 "USB 액세서리 연결됨" 또는 "OTG 장치 연결" 메시지 확인
- 설정 > 연결된 장치에서 USB 장치 인식 여부 확인
- 충전 표시등 또는 배터리 아이콘 변화 확인 (일부 기기는 변화 없을 수 있음)

#### 2단계: USB-Serial 어댑터 인식 확인
**Android에서:**
- 터미널 앱에서 장치 파일 확인
- USB 장치 목록 조회

**확인되어야 할 것:**
- `/dev/ttyUSB0` (또는 유사한 장치 파일) 존재
- VID/PID: `10C4:EA60` (CP2102) 인식

#### 3단계: ESP32-S3 UART 통신 확인
**방법 1: 시리얼 모니터 사용**
- ESP32-S3를 PC에 연결 후 개발 환경 모니터 실행

**방법 2: 간단한 에코 테스트**
- ESP32-S3 펌웨어에 에코 테스트 함수 임시 추가
- UART로 데이터 수신 시 "OK" 응답 전송

#### 4단계: ESP32-S3 HID 장치 인식 확인
**Windows에서:**
1. 장치 관리자 (`devmgmt.msc`) 실행
2. "휴먼 인터페이스 장치" 섹션 확인
3. "HID-compliant mouse" 항목 존재 여부 확인

**확인되어야 할 것:**
- HID-compliant mouse (부트 마우스)
- USB 복합 장치 또는 직렬 포트 (CDC/Vendor 인터페이스)

#### 5단계: 전체 시스템 통신 테스트
**최종 검증:**
1. Android 앱에서 마우스 이동 명령 전송
2. PC에서 커서 이동 확인
3. 클릭 동작 확인

### 6.2 Android 앱에서 확인하는 방법

#### USB 장치 인식 확인
**Android 앱에서 USB 장치 확인 절차**:
- USB 관리자를 통한 장치 목록 조회
- CP2102 VID/PID (0x10C4/0xEA60) 매칭 확인
- 장치 인식 시 권한 요청 및 연결 시도

#### 데이터 전송 테스트
**간단한 테스트 프레임 전송**:
- 8바이트 테스트 프레임 구성 (seq, buttons, dx, dy, wheel, flags)
- 작은 오른쪽 이동 테스트 (dx = 16)
- 전송 성공 여부 확인 및 로깅

### 6.3 문제 발생 시 단계별 진단

#### 문제: OTG 장치가 인식되지 않음
**확인 사항:**
1. OTG Y 케이블의 Device/Power 포트 올바른 연결
2. 스마트폰 OTG 기능 활성화 (설정 > 연결된 장치 > OTG)
3. USB 디버깅 활성화
4. "USB를 통한 전원 공급" 설정 확인

#### 문제: ESP32-S3가 HID로 인식되지 않음
**확인 사항:**
1. ESP32-S3 전원 공급 (LED 점등 확인)
2. USB 케이블이 데이터 전송 가능한지 확인
3. TinyUSB 설정에서 HID 활성화 여부
4. 펌웨어 정상 플래시 여부

#### 문제: UART 통신이 안됨
**확인 사항:**
1. TX-RX 크로스 연결 (TX ↔ RX)
2. GND 공통 접지 연결
3. VCC 미연결 상태 확인
4. 보레이트 설정 일치 (1,000,000 bps)

### 6.4 성공 지표

✅ **모든 연결이 성공했다면:**
- Android: USB 장치 목록에 CP2102 표시
- ESP32-S3: 시리얼 모니터에서 UART 데이터 수신 로그
- Windows: 장치 관리자에 "HID-compliant mouse" 표시
- 최종: Android 터치/드래그 → PC 커서 이동

---

## 7. 빠른 시작

### 준비 단계
1. **OTG Y 케이블 연결**:
   - 스마트폰에 OTG Y 케이블 연결
   - Device 포트에 USB-Serial 어댑터 연결
   - Power 포트에 충전기 연결 (5V 2A 이상)

2. **UART 배선**:
   - USB-Serial TX → ESP32-S3 RX (GPIO17)
   - USB-Serial RX → ESP32-S3 TX (GPIO18)
   - USB-Serial GND ↔ ESP32-S3 GND

### 펌웨어 설정
1. ESP-IDF로 TinyUSB **복합 장치(HID + Vendor/CDC)** 예제 빌드
2. ESP32-S3에 플래시 후 PC에 연결
3. 장치 관리자에서 "HID-compliant mouse" 및 "직렬 포트(COMx)" 확인

### 동작 테스트
1. **Android → PC**: 
   - Android 앱에서 USB-Serial 어댑터 인식 확인
   - 8바이트 마우스 프레임 전송
   - PC에서 커서 이동/클릭 동작 확인

2. **PC → Android** (양방향 통신):
   - PC 터미널(PuTTY 등)에서 ESP32-S3의 COM 포트 연결
   - 데이터 송신 후 Android 앱에서 수신 확인

### 확인 사항
- OTG 모드 정상 동작 (USB 장치 인식)
- 충전 동시 진행 (배터리 소모 방지)
- 양방향 데이터 통신 무결성

---

## 8. 테스트 체크리스트

### OTG Y 케이블 연결 테스트
- [ ] OTG Y 케이블 정상 인식 (Device/Power 포트 구분)
- [ ] 충전과 OTG 동시 동작 확인
- [ ] USB-Serial 어댑터 안정적 인식
- [ ] 장시간(≥8h) 연속 사용 시 연결 안정성

### HID 마우스 기능 테스트
- [ ] BIOS/UEFI 환경에서 마우스 동작
- [ ] BitLocker PIN 입력 화면에서 동작
- [ ] Windows 로그인 화면에서 동작
- [ ] UAC Secure Desktop에서 동작
- [ ] 좌/우/중간 버튼 클릭 정상 동작
- [ ] 커서 이동 정확도 및 반응 속도

### 성능 및 안정성 테스트
- [ ] 장시간(≥24h) 연속 송신 시 프레임 드랍/지터 없음
- [ ] 최대 델타 값 처리 (분할 주입 동작 확인)
- [ ] 긴급 정지 제스처 (양 버튼 동시 3초) 동작
- [ ] 재연결 시 안정적 복구

### 양방향 통신 테스트
- [ ] Android → PC: 마우스 데이터 전송
- [ ] PC → Android: Vendor/CDC 채널 데이터 수신
- [ ] 양방향 동시 통신 시 데이터 무결성
- [ ] 확장 기능 명령어 처리

### 전원 관리 테스트
- [ ] 충전 중 OTG 기능 유지
- [ ] 배터리 소모율 측정 (충전 없이 vs 충전 병행)
- [ ] 다양한 충전기 호환성 (5V 2A, PD 30W, PD 60W)
