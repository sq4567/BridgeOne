---
title: "BridgeOne Android 앱 기술 명세서"
description: "Android 앱의 기술적 설계 요구사항과 명세 정의 문서"
tags: ["technical", "specification", "requirements", "usb-serial", "touchpad", "constants", "design"]
version: "v0.5"
owner: "Chatterbones"
updated: "2025-09-22"
---

# BridgeOne Android 앱 기술 명세서

## 시스템 아키텍처 개요

> **상세 아키텍처**: 전체 시스템 아키텍처는 [`technical-specification.md`]를 참조하세요.

## 개요

### 문서 목적 및 범위

본 문서는 BridgeOne Android 앱의 **구체적 구현 명세서**로, 다음 3개 주요 기술 영역에 대한 상세한 설계 요구사항과 구현 세부사항을 체계적으로 정의합니다:

1. **USB 통신 (HID/Vendor CDC)**: ESP32-S3 내장 USB-to-UART 기반 고속 통신 아키텍처와 BridgeOne 프로토콜 구현
2. **핵심 기능 로직**: 터치패드 알고리즘, 컴포넌트 설계, UI 상태 제어 시스템 구현
3. **상수 및 임계값 정의**: 모든 기술 영역에 대한 정량적 기준값과 설정값 정리

**"어떻게(How)"에 집중**하여 각 기술 영역의 구현 요구사항을 상세히 규정하며, 실제 개발 과정에서 코드 구현의 정확성을 검증할 수 있는 구체적인 기준을 제공합니다.

### 핵심 설계 원칙

**통신 및 프로토콜**:
- **고속 통신**: 1Mbps UART 기반 양방향 통신으로 실시간 입력 전송
- **안정적 프로토콜**: 8바이트 델타 프레임 구조로 패킷 순서 보장 및 손실 감지
- **강건한 연결**: Cancel and Restart 패턴 기반 오류 복구 메커니즘

**사용자 인터페이스**:
- **반응형 컴포넌트**: Compose 기반 모듈화된 UI 컴포넌트 아키텍처
- **직관적 상호작용**: 햅틱 피드백과 상태별 시각적 피드백 제공
- **상태 기반 설계**: 명확한 상태 전이와 컴포넌트 비활성화 시스템

**알고리즘 및 성능**:
- **실시간 처리**: 120Hz 기본 전송 빈도와 50ms 이하 지연시간 목표 (디바이스 성능에 따라 83-167Hz 적응적 조절)
- **메모리 최적화**: 객체 풀링과 배치 처리를 통한 효율적 자원 관리
- **배터리 효율**: 적응적 전송 빈도와 백그라운드 작업 최적화

### 관련 문서와의 관계

**전체 시스템 아키텍처**:
- **`technical-specification.md`**: 플랫폼 간 기술적 계약 및 프로토콜 정의 (본 문서의 기반)
- **`usb-hid-bridge-architecture.md`**: USB-HID 브릿지 시스템의 전체 아키텍처

**UX/UI 설계 문서**:
- **`design-guide-app.md`**: 사용자 경험, 화면 플로우, 상호작용 설계 원칙
- **`styleframe-*.md`**: 각 화면의 시각적 설계 및 사용자 시나리오
- **`component-*.md`**: 개별 UI 컴포넌트의 상세 설계 명세

**개발 참조 문서**:
- **`android-workspace-rule-generator.md`**: Android 개발 환경 설정 규칙
- **`documentation-guide.md`**: 프로젝트 문서화 지침

---

## 1. USB 통신 (HID / Vendor CDC)

### 1.1 통신 아키텍처 설계

#### 1.1.1 하드웨어 연결 요구사항

**장치 식별 요구사항:**
- ESP32-S3 내장 USB-to-UART 인식 필요
- VID: 0x303A (Espressif), PID: USB Serial 장치로 필터링
- USB OTG 호스트 모드 지원 필수

**통신 설정 요구사항:**
- 1Mbps 전송 속도로 고속 통신 구현
- 8N1 설정 (8비트 데이터, 패리티 없음, 1스톱비트)
- 양방향 통신 지원으로 PC 응답 수신 가능

#### 1.1.2 연결 관리 요구사항

**자동 탐지 요구사항:**
- 시스템 부팅 시 ESP32-S3 USB Serial 장치 자동 스캔
- VID/PID 기반 정확한 장치 식별 (VID: 0x303A)
- 다중 장치 환경에서 우선순위 처리

**권한 관리 요구사항:**
- USB 장치 접근 권한 자동 요청
- 사용자 권한 거부 시 적절한 안내 메시지
- 권한 상태 실시간 모니터링

**연결 설정 요구사항:**
- 1Mbps 통신 속도로 포트 열기
- 8N1 통신 파라미터 설정
- 양방향 데이터 스트림 초기화

#### 1.1.3 인터페이스별 최적화 전략

**개요:**
Android 앱은 ESP32-S3의 USB 복합 장치와 통신하며, Essential 모드와 Standard 모드에 따라 다른 인터페이스를 사용합니다. 각 인터페이스는 특정 목적과 최적화 전략을 가지며, 통합 관리되어야 합니다.

**인터페이스별 역할 구분:**
- **HID Boot Mouse**: Essential 모드에서 BIOS/UEFI 호환 마우스 입력 처리
- **HID Boot Keyboard**: Essential 모드에서 BIOS/UEFI 호환 키보드 입력 처리
- **Vendor CDC**: Standard 모드에서 고급 기능 및 양방향 통신 처리

**통합 최적화 원칙:**
1. **프로토콜 일관성**: 모든 인터페이스가 동일한 8바이트 프레임 구조 기반
2. **실시간 우선순위**: HID 인터페이스를 Vendor CDC보다 높은 우선순위로 처리
3. **상태 기반 전환**: Essential/Standard 모드에 따른 인터페이스 활성화/비활성화
4. **자원 공유**: 공통 버퍼와 메모리 풀을 효율적으로 활용

**Essential 모드 최적화:**
- **HID Boot Protocol 전용**: 불필요한 오버헤드 제거로 최소 지연 시간 달성
- **8바이트 델타 프레임**: 마우스/키보드 입력만 처리하여 대역폭 효율 극대화
- **폴링 레이트 조절**: 125Hz 폴링으로 전력 소비 최소화

**Standard 모드 최적화:**
- **HID Report Protocol 활용**: 확장 기능 지원과 함께 효율적인 데이터 전송
- **Vendor CDC 인터페이스**: JSON 기반 커맨드로 구조화된 고급 기능 처리
- **양방향 통신**: 비동기 응답 처리로 실시간 상호작용 보장

### 1.2 통신 프로토콜 설계

#### 1.2.1 BridgeOne 프로토콜 명세

**프레임 구조 요구사항:**
- 8바이트 고정 크기 프레임으로 통신 효율성 확보
- Little-Endian 바이트 순서로 ESP32 호환성 보장
- 순번 필드로 패킷 순서 보장 및 손실 감지
- 통합 마우스/키보드 입력 지원으로 효율적인 데이터 전송

**통합 프레임 구조 (8바이트):**
| 바이트 | 필드 | 타입 | 설명 | 범위 |
|-------|------|-----|------|------|
| 0 | `seq` | u8 | 순번 (패킷 순서 보장) | 0~255 순환 |
| 1 | `buttons` | u8 | 버튼 상태 (bit0=L, bit1=R, bit2=M) | 0x00~0x07 |
| 2 | `deltaX` | i8 | 상대 X 이동량 | -127~127 |
| 3 | `deltaY` | i8 | 상대 Y 이동량 | -127~127 |
| 4 | `wheel` | i8 | 휠 스크롤량 | -127~127 |
| 5 | `modifiers` | u8 | 키보드 모디파이어 (Ctrl, Alt, Shift, GUI) | 0x00~0xFF |
| 6 | `keyCode1` | u8 | 키 코드 1 (주요 키 입력) | 0x00~0xFF |
| 7 | `keyCode2` | u8 | 키 코드 2 (보조 키 입력) | 0x00~0xFF |

**프레임 구조 설명:**
- **seq**: 패킷 순서 보장 및 손실 감지를 위한 순번 카운터 (0~255 순환)
- **buttons**: 마우스 버튼 상태 (bit0=좌클릭, bit1=우클릭, bit2=중앙클릭)
- **deltaX/deltaY**: 마우스 상대 이동량 (i8 범위로 정밀도와 효율성 균형)
- **wheel**: 마우스 휠 스크롤량 (-127~127 범위로 충분한 스크롤 지원)
- **modifiers**: 키보드 모디파이어 키 상태 (Ctrl, Alt, Shift, GUI 키 조합)
- **keyCode1/keyCode2**: 키보드 키 코드 (최대 2키 동시 입력 지원)

**프레임 생성 원칙:**
1. **통합 처리**: 마우스와 키보드 입력을 단일 프레임으로 통합
2. **범위 최적화**: i8 범위로 정밀도와 대역폭 효율성 균형
3. **동시성 지원**: 마우스 + 키보드 동시 입력 처리 가능
4. **확장성**: 예약 영역 없이 최대 효율성 달성

#### 1.2.2 프레임 생성 요구사항

**데이터 인코딩 요구사항:**
- 순번 카운터로 패킷 순서 보장 (0~255 순환)
- 버튼 상태를 비트 플래그로 압축 표현
- 좌표 데이터를 i16 범위로 클램핑하여 오버플로우 방지
- Little-Endian 바이트 순서로 ESP32 호환성 보장

**데이터 무결성 요구사항:**
- 모든 수치 데이터의 범위 검증 필수
- 비트 플래그의 유효성 검사
- 프레임 크기 고정 유지

#### 1.2.3 인터페이스별 프로토콜 구현 가이드

**개요:**
각 USB 인터페이스는 BridgeOne 프로토콜을 기반으로 하되, 용도에 따라 다른 구현 전략을 사용합니다. Android 앱에서는 USB Serial 라이브러리를 통해 이 프로토콜을 구현하며, 각 인터페이스별 특성을 고려한 최적화가 필요합니다.

**HID Boot Mouse Interface 구현:**

**프로토콜 스펙:**
```kotlin
// BridgeOne 통합 프레임 구조 (8바이트) - 마우스 중심
data class BridgeOneFrame(
    val seq: UByte,        // 순번 (패킷 순서 보장)
    val buttons: UByte,    // 버튼 상태 (bit0=L, bit1=R, bit2=M)
    val deltaX: Byte,      // X축 상대 이동 (-127 ~ +127)
    val deltaY: Byte,      // Y축 상대 이동 (-127 ~ +127)
    val wheel: Byte,       // 휠 이동 (스크롤)
    val modifiers: UByte,  // 키보드 모디파이어 (0으로 설정)
    val keyCode1: UByte,   // 키 코드 1 (0으로 설정)
    val keyCode2: UByte    // 키 코드 2 (0으로 설정)
)
```

**성능 최적화 구현:**
- **델타 압축**: 이전 프레임 대비 변화량만 전송하여 대역폭 절약
- **지연 보상**: 프레임 처리 시 시스템 지연 시간 예측 및 보정
- **버퍼 관리**: 16프레임의 링 버퍼로 입력 버스트 처리

**정확성 최적화 구현:**
- **보정 알고리즘**: DPI 설정에 따른 마우스 감도 자동 조정
- **안정화 필터**: 미세 떨림을 제거하는 노이즈 필터 적용
- **가속 곡선**: 사용자의 이동 패턴에 따른 속도 가속 처리
- **정지 감지**: 2프레임 연속 무이동 시 전송 중단

**HID Boot Keyboard Interface 구현:**

**프로토콜 스펙:**
```kotlin
// BridgeOne 통합 프레임 구조 (8바이트) - 키보드 중심
data class BridgeOneFrame(
    val seq: UByte,        // 순번 (패킷 순서 보장)
    val buttons: UByte,    // 버튼 상태 (0으로 설정)
    val deltaX: Byte,      // X축 이동 (0으로 설정)
    val deltaY: Byte,      // Y축 이동 (0으로 설정)
    val wheel: Byte,       // 휠 이동 (0으로 설정)
    val modifiers: UByte,  // 키보드 모디파이어 (Ctrl, Alt, Shift, GUI)
    val keyCode1: UByte,   // 키 코드 1 (주요 키 입력)
    val keyCode2: UByte    // 키 코드 2 (보조 키 입력)
)
```

**성능 최적화 구현:**
- **키 매트릭스**: 8x8 키 상태 매트릭스로 동시 입력 처리
- **디바운싱**: 5ms 하드웨어 디바운싱 + 10ms 소프트웨어 디바운싱
- **타이핑 속도**: 초당 최대 1000키 처리 (롤오버 지원)
- **메모리 관리**: 키 상태를 비트맵으로 압축 저장

**기능 최적화 구현:**
- **모디파이어 처리**: 동시 모디파이어 키 조합 지원 (Ctrl+Alt+Del 등)
- **키 맵핑**: Android 키코드와 HID 키코드 간 매핑 테이블
- **멀티탭 처리**: 더블탭, 트리플탭 등 제스처 인식
- **매크로 지원**: 자주 사용되는 키 조합을 단축키로 등록

**Vendor CDC Interface 구현:**

**프로토콜 스펙:**
```kotlin
// Vendor CDC 명령 프레임 구조
data class VendorCdcFrame(
    val header: UByte,      // 0xFF (벤더 명령 식별자)
    val command: UByte,     // 명령 타입 (0x01-0xFF)
    val length: UShort,     // 페이로드 길이
    val payload: ByteArray, // JSON 형식의 명령 데이터
    val checksum: UShort    // CRC16 체크섬
)
```

**통신 최적화 구현:**
- **JSON 압축**: 자주 사용되는 명령에 대한 단축 코드 할당
- **스트리밍 처리**: 큰 데이터는 청크 단위로 분할 전송
- **우선순위 큐**: 긴급 명령(시스템 상태 변경)은 우선 처리
- **연결 풀링**: 다중 연결 시 효율적인 연결 자원 관리

**고급 기능 구현:**
- **상태 동기화**: Android 앱과 Windows 서비스 간 실시간 상태 공유
- **이벤트 중계**: 마우스/키보드 이벤트의 Windows 서비스 전달
- **원격 제어**: Windows 서비스에서 Android 앱 기능 제어
- **데이터 캐싱**: 자주 요청되는 정보의 로컬 캐시 관리

### 1.3 전송 제어 설계

#### 1.3.1 실시간 전송 요구사항

**전송 주기 요구사항:**
- 목표 전송 빈도: 120Hz (8.3ms 주기) - 기본
- 최대 전송 빈도: 167Hz (6ms 주기) - 고급 디바이스
- 최소 보장 빈도: 83Hz (12ms 주기) - 저전력 모드
- 디바이스 성능에 따른 적응적 빈도 조절
- 터치 입력 변화 시 즉시 프레임 전송
- 입력 없을 시 Keep-alive 프레임으로 연결 유지

**데이터 누적 요구사항:**
- 터치 이벤트를 프레임 단위로 누적 처리
- 전송 완료 후 프레임 상태 초기화
- 중복 전송 방지를 위한 상태 관리

### 1.4 양방향 통신 설계

#### 1.4.1 응답 수신 요구사항

**데이터 수신 요구사항:**
- PC에서 전송되는 응답 메시지 실시간 수신
- 비동기 데이터 스트림 처리로 UI 블로킹 방지
- 수신 데이터 파싱 및 검증 처리

**응답 처리 요구사항:**
- 매크로 실행 결과 응답 처리
- 연결 상태 모니터링 응답 처리
- 메인 스레드에서 UI 업데이트 보장

### 1.5 연결 상태 모니터링 설계

#### 1.5.1 Keep-alive 요구사항

**연결 유지 요구사항:**
- 500ms 주기로 Keep-alive 프레임 전송
- 특별한 플래그로 Keep-alive 프레임 식별
- 연결 상태 실시간 모니터링

**타임아웃 처리 요구사항:**
- 3초 응답 타임아웃 임계값 설정
- 최대 3회 응답 누락 허용
- 타임아웃 발생 시 자동 재연결 시도

### 1.6 오류 처리 및 복구 설계

#### 1.6.1 Cancel and Restart 패턴 구현

**패턴 개요:**
- USB Serial 라이브러리에서 검증된 안정적인 복구 방식
- 복잡한 상태 복구 대신 안전한 전체 재시작으로 구현 복잡도 60-70% 감소
- Race Condition 방지 및 상태 일관성 보장

**적용 시나리오:**
- USB 장치 분리/재연결 감지
- 권한 거부 후 재승인
- 서버 연결 실패 후 재시도
- 예상치 못한 오류 발생 시

#### 1.6.2 연결 복구 요구사항

**오류 분류 요구사항:**
- USB 하드웨어 오류 (IOException)
- 권한 관련 오류 (SecurityException)
- 기타 예상치 못한 오류

**안전 정지 요구사항:**
- 오류 발생 시 즉시 중립 프레임 전송
- 모든 진행 중인 작업 취소 (Cancel and Restart 적용)
- 연결 상태 초기화

**재연결 전략 요구사항:**
- Cancel and Restart 패턴으로 안전한 재시작
- 지수 백오프 방식으로 재연결 시도 (1초, 2초, 4초, 8초)
- 최대 4회 재연결 시도
- 재연결 실패 시 사용자에게 알림

#### 1.6.3 실시간 오류 감지 및 대응

**BroadcastReceiver 기반 감지:**
- USB 장치 분리/재연결 이벤트 실시간 감지
- 즉시 Cancel and Restart 실행
- 연결 상태 실시간 모니터링

**응답 타임아웃 감지:**
- Keep-alive 응답 3회 연속 누락 시 자동 Cancel and Restart
- 연결 상태 실시간 모니터링
- 비정상 상황 즉시 감지 및 대응

#### 1.6.4 성능 최적화 구현 세부사항

**리소스 관리 최적화:**
- **메모리 풀**: 각 인터페이스별 전용 메모리 영역 할당으로 메모리 단편화 방지
- **태스크 우선순위**: HID 태스크 > Vendor 태스크 > 관리 태스크로 실시간성 보장
- **전력 최적화**: 인터페이스별 동적 전력 모드 전환으로 배터리 효율 극대화
- **자원 경합 방지**: 뮤텍스와 세마포어를 통한 동시성 제어

**성능 모니터링 구현:**
- **실시간 메트릭스**: 각 인터페이스의 처리량, 지연시간, 오류율 추적
- **적응형 조절**: 부하 상황에 따른 폴링 레이트 자동 조정
- **상태 전이**: Essential ↔ Standard 모드 전환 시 무손실 처리
- **진단 로깅**: 상세한 통신 로그로 문제 진단 지원

**동적 전력 관리 구현:**
- **활성 모드**: 최대 성능 모드 (Standard 모드에서 HID + Vendor CDC 동시 사용)
- **절전 모드**: 입력 없을 때 저전력 모드 전환 (CPU 주파수 조절)
- **심플 모드**: Essential 모드에서 불필요한 기능 비활성화
- **자동 모드 전환**: 사용량에 따른 모드 자동 전환

**데이터 전송 효율화 구현:**
- **프레임 구조 최적화**: 8바이트 델타 프레임으로 대역폭 효율 극대화
- **압축 및 인코딩**: RLE와 비트 패킹을 통한 데이터 압축
- **실시간 처리**: FreeRTOS 태스크 기반 실시간 프레임 처리
- **버퍼 관리**: 링 버퍼와 더블 버퍼링으로 데이터 손실 방지

#### 1.6.5 보안 및 안정성 강화

**보안 최적화 구현:**
- **인증 프로토콜**: 연결 시 챌린지-응답 인증 방식으로 무단 접근 방지
- **암호화 통신**: 민감한 명령에 대한 AES256 암호화 적용
- **접근 제어**: 명령별 권한 검사 및 로깅으로 보안 강화
- **무결성 검증**: 모든 프레임에 대한 디지털 서명 검증

**안정성 강화 구현:**
- **하트비트 패킷**: 5초 간격으로 연결 상태 확인
- **재전송 메커니즘**: 오류 프레임 자동 재전송
- **순서 보장**: 시퀀스 번호로 패킷 순서 관리
- **폴백 모드**: 오류 지속 시 Essential 모드로 자동 전환

### 1.7 통합 테스트 및 검증 전략

#### 1.7.1 테스트 환경 구축

**하드웨어 테스트 환경:**
- **실제 장치 테스트**: Samsung Galaxy s10e + ESP32-S3-DevkitC-1 + Windows PC
- **에뮬레이터 테스트**: Android Emulator + Virtual USB Serial Device
- **CI/CD 파이프라인**: 자동화된 통합 테스트 환경 구축
- **테스트 장비**: 오실로스코프, 로직 애널라이저, USB 프로토콜 분석기

**소프트웨어 테스트 환경:**
- **단위 테스트**: 각 인터페이스별 독립적 기능 테스트
- **통합 테스트**: 전체 USB 통신 스택 테스트
- **성능 테스트**: 실시간 전송 및 응답 시간 테스트
- **스트레스 테스트**: 고부하 상황에서의 안정성 테스트

#### 1.7.2 인터페이스별 테스트 전략

**HID Boot Mouse Interface 테스트:**
- **정확성 테스트**: 마우스 이동 및 클릭 정확도 검증 (정밀도 ±1픽셀)
- **응답성 테스트**: 입력 지연 시간 측정 (목표: ≤8ms)
- **연속성 테스트**: 장시간 연속 사용 시 안정성 검증 (최소 1시간)
- **버스트 테스트**: 빠른 입력 연속 발생 시 프레임 손실률 측정 (목표: ≤0.1%)

**HID Boot Keyboard Interface 테스트:**
- **키 매핑 테스트**: 모든 키 코드의 정확한 전송 검증
- **동시 입력 테스트**: 다중 키 동시 입력 처리 능력 테스트 (최대 6키)
- **타이핑 속도 테스트**: 초당 최대 키 입력 처리량 측정 (목표: ≥100키/초)
- **특수키 테스트**: 모디파이어 키 조합 및 특수키 전송 정확성 검증

**Vendor CDC Interface 테스트:**
- **명령 전송 테스트**: JSON 명령의 정확한 전송 및 수신 검증
- **양방향 통신 테스트**: 요청-응답 패턴의 안정성 테스트
- **스트리밍 테스트**: 대량 데이터 전송 시 성능 및 안정성 검증
- **동시성 테스트**: 다중 명령 동시 처리 능력 테스트

#### 1.7.3 성능 벤치마크 및 검증

**실시간 성능 벤치마크:**
- **지연 시간 측정**: 각 인터페이스의 평균/최대 지연 시간 모니터링
- **처리량 측정**: 초당 처리 가능한 프레임 수 추적
- **에러율 모니터링**: 통신 오류 발생률 및 복구 시간 측정
- **전력 소비 측정**: 각 모드별 배터리 사용량 프로파일링

**자동화된 검증 프로세스:**
- **연속 모니터링**: 4시간 연속 동작 시 시스템 안정성 검증
- **부하 테스트**: 최대 부하 상황에서의 성능 저하율 측정
- **회귀 테스트**: 코드 변경 시 기존 기능의 안정성 검증
- **사용자 시나리오 테스트**: 실제 사용 패턴 시뮬레이션

---

## 2. 핵심 기능 로직 및 구현 요구사항

### 2.1 스플래시 스크린 구현 요구사항

**관련 문서**:
- **플로우 설계**: `design-guide-app.md` 8.1.1 앱 실행 및 초기 상태 감지 참조
- **스타일프레임**: `styleframe-loading-splash.md` 참조

#### 2.1.1 Jetpack Compose 기반 설계 요구사항

**상태 관리 구조**:
- **상태 정의**: 상태 관리 시스템을 활용한 단계별 상태 정의
  - 정수형 단계값 (0: 초기, 1: 브릿지 등장, 2: 별 회전, 3: 회전 안정화, 4: 별 확장, 5: 텍스트 등장)
- **상태 전환**: 효과 기반 순차적 전환 메커니즘 활용
- **상태 보존**: 컴포지션 재구성 시에도 애니메이션 연속성 보장

**Canvas 기반 렌더링 설계**:
- **벡터 경로**: Path 객체 기반 브릿지와 별 구조 정의
- **경로 애니메이션**: 경로 세그먼트 기반 Wipe-in 애니메이션 구현
- **좌표 시스템**: 정규화된 좌표계 사용으로 해상도 독립성 확보
- **렌더링 최적화**: 경로 및 텍스트 드로잉 조합으로 효율적 렌더링

**애니메이션 시스템 설계**:
- **값 기반 애니메이션**: 상태 기반 애니메이션 시스템으로 진행률 제어
- **타이밍 제어**: 이징 함수와 정확한 지속시간 설정
- **전환 효과**: 크로스페이드 기반 부드러운 단계별 전환
- **상태 동기화**: 애니메이션 완료와 상태 전환의 정확한 타이밍 매칭

**생명주기 통합**:
- **시작 조건**: 액티비티 생성 완료 후 자동 시작
- **완료 처리**: 애니메이션 완료 시 콜백을 통한 메인 화면 전환
- **중단 처리**: 백그라운드 전환 시 애니메이션 일시정지 및 복구
- **메모리 관리**: 컴포지션 해제 시 리소스 자동 정리

#### 2.1.2 성능 및 품질 목표

**렌더링 성능**:
- **프레임률**: 60fps 안정적 유지 (16.67ms 프레임 시간)
- **GPU 가속**: Compose의 하드웨어 가속 렌더링 파이프라인 활용
- **메모리 효율**: 벡터 그래픽 기반으로 메모리 사용량 최소화
- **배터리 최적화**: 효율적인 애니메이션으로 전력 소모 제한

**호환성 목표**:
- **API 레벨**: API 24+ (Android 7.0 이상, 90% 커버리지)
- **화면 밀도**: mdpi부터 xxxhdpi까지 모든 밀도 지원
- **화면 크기**: 다양한 화면 크기에서 일관된 비율 유지
- **디바이스 성능**: 중급 사양 디바이스에서도 원활한 동작

**품질 검증 기준**:
- **시각적 정확성**: 디자인 스펙과 픽셀 단위 일치
- **타이밍 정확성**: 각 단계별 시간 오차 ±50ms 이내
- **부드러움**: 프레임 드롭 없는 연속적 애니메이션
- **일관성**: 여러 실행 간 동일한 결과 보장

#### 2.1.3 접근성 요구사항

**모션 접근성**:
- **모션 감소**: `Settings.Global.TRANSITION_ANIMATION_SCALE` 확인을 통한 사용자 애니메이션 설정 감지
- **대체 모드**: 모션 감소 설정 시 정적 로고 표시 또는 단순화된 애니메이션
- **사용자 제어**: 애니메이션 스킵 옵션 제공 (탭으로 즉시 완료)

**스크린 리더 지원**:
- **콘텐츠 설명**: "BridgeOne 로고 애니메이션 재생 중" 설명 제공
- **진행 상태**: 애니메이션 단계별 진행 상황 안내
- **완료 알림**: 애니메이션 완료 시 "로딩 완료" 알림

**시각적 접근성**:
- **고대비 지원**: `AccessibilityManager.isHighTextContrastEnabled()` API를 통한 고대비 모드 감지 및 대응
- **색약 고려**: 흑백 기반 디자인으로 색맹 사용자도 인식 가능
- **크기 조정**: 시스템 글꼴 크기 설정에 따른 텍스트 크기 조정

#### 2.1.4 구현 시 고려사항

**상태 관리 전략**:
- **Compose State**: 상태 관리 시스템으로 애니메이션 상태 관리
- **상태 복구**: 프로세스 재생성 시 적절한 상태 복구 메커니즘
- **동기화**: 여러 애니메이션 요소 간 정확한 타이밍 동기화

**메모리 및 성능 최적화**:
- **리소스 관리**: 애니메이션 완료 후 즉시 리소스 해제
- **가비지 컬렉션**: 임시 객체 생성 최소화
- **프리로딩**: 필요한 리소스 사전 준비

**오류 처리 및 복구**:
- **애니메이션 실패**: 예외 발생 시 정적 로고로 대체
- **타임아웃**: 과도한 대기 시간 방지를 위한 최대 시간 제한
- **복구 메커니즘**: 중단된 애니메이션의 안전한 복구

#### 2.1.5 품질 검증 방법론

**자동화 테스트**:
- **애니메이션 타이밍**: 각 단계별 정확한 지속시간 검증
- **상태 전환**: 애니메이션 단계 전환의 정확성 확인
- **메모리 누수**: 애니메이션 완료 후 리소스 해제 검증

**수동 테스트**:
- **시각적 검증**: 디자인 스펙과의 일치도 확인
- **성능 모니터링**: 다양한 디바이스에서 프레임률 측정
- **접근성 검증**: 스크린 리더와 고대비 모드 테스트

**사용자 테스트**:
- **첫인상 평가**: 브랜드 인식도와 전문성 인상 측정
- **로딩 체감시간**: 실제 로딩 시간 대비 체감 시간 평가
- **모션 만족도**: 애니메이션 품질에 대한 사용자 만족도 조사

**관련 문서**:
- **플로우 설계**: `design-guide-app.md` 8.1.1 앱 실행 및 초기 상태 감지 참조
- **스타일프레임**: `styleframe-loading-splash.md` 참조

#### 2.1.6 스플래시 스크린 상수 및 임계값 정의

**애니메이션 타이밍 상수**:
```
PHASE_1_DURATION_MS = 300
PHASE_2_DURATION_MS = 700
PHASE_3_DURATION_MS = 500
PHASE_4_DURATION_MS = 300
PHASE_5_DURATION_MS = 100
PHASE_6_DURATION_MS = 600
TOTAL_ANIMATION_DURATION_MS = 2500
```

**성능 관련 상수**:
```
TARGET_FPS = 60
MAX_FRAME_TIME_MS = 16.67
MEMORY_THRESHOLD_MB = 50
ANIMATION_EASING = FastOutSlowIn
```

### 2.2 터치패드 알고리즘

#### 2.2.1 터치패드 UI 구조와 알고리즘 연계

**UI 구조 개요:**
- **TouchpadWrapper**: 1:2 비율의 최상위 컨테이너 (최소 160dp×320dp, 최대 화면 크기)
- **TouchpadAreaWrapper**: 터치 인터페이스 영역, 둥근 모서리 (터치패드 너비의 3%)
- **ControlButtonContainer**: TouchpadAreaWrapper 상단 오버레이 (터치패드 높이의 15%)

**알고리즘 연계 요구사항:**
- **좌표 변환**: TouchpadAreaWrapper 내의 모든 터치 이벤트가 TouchpadWrapper 기준 좌표로 변환
- **영역 검증**: TouchpadWrapper 경계를 벗어나는 터치는 무시, 영역 내 연속 추적은 허용
- **제어 버튼 연동**: ControlButtonContainer 내 버튼 상태가 TouchpadAreaWrapper 동작에 실시간 반영
- **시각적 피드백**: 모든 알고리즘 상태 변경이 UI 색상/테두리/가이드라인으로 즉시 표시

**반응형 처리:**
- **크기 조정**: TouchpadWrapper 크기 변경 시 모든 내부 요소 비례적 스케일링
- **터치 좌표 보정**: 화면 DPI와 터치패드 크기에 따른 정규화 적용
- **히트 영역**: 모든 제어 버튼의 최소 48×48dp 히트 영역 보장

#### 2.2.2 터치패드 핵심 알고리즘: 자유 이동 알고리즘 명세

**좌표 변환 요구사항:**
- 터치 시작점이 터치패드 영역 내부인지 검증
- 영역 외부 시작 터치는 무시하여 의도치 않은 입력 방지
- 원시 터치 좌표를 마우스 델타로 변환
- 터치패드 민감도 설정에 따른 스케일링 적용
- DPI 독립적 정규화로 다양한 화면 크기 지원
- 터치패드와 화면 크기 비율을 반영한 보정 적용

**좌표 변환 공식:**
```
delta = (current_pos - previous_pos) * sensitivity_scale
normalized_delta = delta * (160.0 / screen_dpi) * (touchpad_size / screen_size)
final_delta = clamp(normalized_delta, -32767, 32767)
```

**데이터 범위 요구사항:**
- i16 범위(-32767~32767)로 클램핑하여 프로토콜 호환성 보장
- 오버플로우 방지를 위한 범위 검증 필수
- 터치패드 크기 대비 화면 크기 보정으로 일관된 민감도 제공

**터치 추적 연속성 요구사항:**
- 터치 시작점이 터치패드 영역 내부인지 검증
- 제스처 활성화 상태로 전환하여 연속성 보장
- 시작점 좌표 저장으로 기준점 설정
- 활성 제스처 상태에서만 터치 이동 처리
- 터치 시작점이 내부라면 영역 밖 이동도 유효하게 처리
- 실시간 좌표 변화를 마우스 델타로 변환
- 제스처 종료 지점은 영역 밖이어도 유효
- 제스처 완료 후 상태 초기화

**모드별 플로우 연계:**
- **커서 이동 모드**: 모든 드래그 제스처가 실시간 마우스 이동으로 변환
- **스크롤 모드**: 드래그 제스처가 스크롤 신호로 전환, 클릭은 비활성화
- **Essential 모드**: 기본 HID 마우스 기능만 사용, 고급 기능 제한
- **Standard 모드**: 모든 기능 활성화, Vendor-specific Interface 활용

#### 2.2.3 직각 이동 알고리즘 명세

**축 결정 요구사항:**
- X축(수평), Y축(수직), 미결정 상태로 분류
- 데드존 보상 후 누적 이동 거리 30dp 달성 시에만 축 결정
- 각도 기반 축 분류로 정확한 방향 인식
- 축 결정 전까지는 데드존 보상 알고리즘의 누적 모드 적용

**각도 허용 범위 요구사항:**
- 수평축: 0도 ±22.5도, 180도 ±22.5도
- 수직축: 90도 ±22.5도, 270도 ±22.5도
- 허용 범위 외 각도는 미결정 상태로 처리

**축 고정 요구사항:**
- 축 결정 후 해당 축으로만 이동 허용
- 다른 축 성분은 무시하여 직각 이동 보장
- 축 미결정 상태에서는 모든 이동 신호 억제

**모드별 처리:**
- **커서 이동 모드**: 자유/직각 이동 모드 전환으로 동작 방식 변경
- **스크롤 모드**: 직각 이동 무효화, 스크롤 방향으로만 동작
- **Essential 모드**: 기본 HID 마우스 기능만, 직각 이동 제한
- **Standard 모드**: 모든 직각 이동 기능 활성화

**UI 피드백 연계:**
- 축 결정 시 선택된 축의 테두리 색상으로 시각적 피드백 제공
- 직각 이동 중 다른 축 성분 무시 상태를 실시간으로 표시
- 제어 버튼의 현재 모드 상태와 동기화

#### 2.2.4 데드존 보상 알고리즘 명세

**상태 머신 요구사항:**
- IDLE: 대기 상태
- ACCUMULATING: 데드존 내 누적 상태
- MOVING: 임계값 초과 후 이동 상태
- CLICK_CANDIDATE: 클릭 후보 상태

**데드존 처리 요구사항:**
- 15dp 임계값 미만 이동은 누적 처리
- 임계값 초과 시 누적된 벡터를 한번에 보상 적용
- 보상 적용 후 실시간 이동 처리로 전환
- 직각 이동 모드에서는 데드존 해제 후 축 결정 프로세스 시작

**클릭 판정 요구사항:**
- 터치 지속시간 500ms 이내
- 총 이동거리 15dp 이내
- 두 조건 모두 만족 시 클릭으로 판정

**직각 이동과의 통합 처리 순서:**
1. **데드존 누적 단계**: 터치 시작부터 15dp 임계값까지 벡터 누적
2. **축 결정 단계**: 데드존 해제 후 누적 벡터로 초기 축 방향 결정
3. **축 고정 단계**: 30dp 도달 시 최종 축 확정, 다른 축 성분 무시
4. **실시간 이동 단계**: 확정된 축으로만 마우스 델타 전송

**Essential/Standard 모드별 처리:**
- **Essential 모드**: 기본 HID 마우스 클릭만 처리, 고급 제스처 제한
- **Standard 모드**: 모든 데드존 보상 및 클릭 판정 로직 활성화
- **재연결 처리**: 연결 끊김 시 Essential 모드로 폴백, 클릭 기능만 유지

**입력 차단 처리:**
- **국소 비활성화**: 특정 제스처 영역만 비활성화 시 해당 영역 입력 무시
- **페이지 일괄 비활성화**: 전체 터치패드 입력 차단, 상태 머신 IDLE로 전환
- **강제 해제**: 상위 정책에 따른 즉시 비활성화 해제 및 상태 복원

#### 2.2.5 스크롤 가이드라인 알고리즘 명세

**가이드라인 표시 요구사항:**
- 가로 스크롤: 세로선 패턴 (40dp 간격)
- 세로 스크롤: 가로선 패턴 (40dp 간격)
- 화면 경계 순환 처리로 무한 스크롤 효과

**속도 동기화 요구사항:**
- 사용자 드래그 속도와 2.0배 배율로 동기화
- 실시간 속도 반영으로 자연스러운 시각적 피드백
- 스크롤 단위 달성 시 햅틱 피드백 제공

**관성 처리 요구사항:**
- 드래그 종료 후 프레임레이트 독립적 지수 감속 적용
- 1.0dp/s 이하 속도에서 가이드라인 자동 숨김
- 관성 구간에서도 연속적인 가이드라인 표시

**관성 감속 공식:**
```
// 프레임레이트 독립적 감속 (60fps 기준 0.95배 감속률)
velocity *= pow(0.95, deltaTime * 60.0)
```

**색상 시스템 연계:**
- **일반 스크롤**: 초록색 가이드라인 (#84E268) + 초록색 테두리
- **무한 스크롤**: 빨간색 가이드라인 (#F32121) + 빨간색 테두리
- **스크롤 모드 전환**: 버튼 배경색과 테두리 색상 동기화

**모드별 처리:**
- **Essential 모드**: 기본 스크롤만 지원, 무한 스크롤 제한
- **Standard 모드**: 일반/무한 스크롤 모두 활성화, 관성 처리 지원
- **스크롤 감도**: 느림/보통/빠름 옵션에 따른 속도 배율 조정

#### 2.2.6 멀티 커서 알고리즘 명세

**멀티 커서 모드 전환 요구사항:**
- ESP32-S3 동글로 `multi_cursor_switch` 명령 전송
- 0xFF 헤더 + JSON 구조의 커스텀 명령으로 처리
- Vendor HID/CDC 인터페이스를 통한 Windows 서비스와의 양방향 통신
- 멀티 커서 모드 Enabled 시 `show_virtual_cursor` 요청 전송

##### 2.2.6.1 터치패드 영역 분할 알고리즘 상세 명세

**영역 분할 좌표 계산**:
```kotlin
// TouchpadAreaWrapper 기준 좌표계
data class TouchpadArea(
    val id: String,          // "pad1" or "pad2"
    val bounds: RectF,       // 영역 경계
    val isActive: Boolean    // 현재 활성 상태
)

fun divideTouchpadAreas(touchpadWidth: Float, touchpadHeight: Float): Pair<TouchpadArea, TouchpadArea> {
    val pad1 = TouchpadArea(
        id = "pad1",
        bounds = RectF(0f, 0f, touchpadWidth / 2f, touchpadHeight),
        isActive = false
    )
    
    val pad2 = TouchpadArea(
        id = "pad2",
        bounds = RectF(touchpadWidth / 2f, 0f, touchpadWidth, touchpadHeight),
        isActive = false
    )
    
    return Pair(pad1, pad2)
}
```

**터치 영역 판정 알고리즘**:
```kotlin
fun determineTouchpadArea(touchX: Float, touchpadWidth: Float): String {
    val midpoint = touchpadWidth / 2f
    
    return when {
        touchX < midpoint -> "pad1"
        touchX > midpoint -> "pad2"
        else -> {
            // 정확히 50% 경계선 → 마지막 활성 영역 유지
            _lastActivePad ?: "pad1" // 기본값: pad1
        }
    }
}
```

**경계 처리 및 디바운스**:
```kotlin
private var _lastPadSwitchTime: Long = 0
private const val PAD_SWITCH_DEBOUNCE_MS = 50L

fun handleTouchEvent(event: MotionEvent): Boolean {
    val touchX = event.x
    val detectedPad = determineTouchpadArea(touchX, touchpadWidth)
    
    // 영역 전환 디바운스
    if (detectedPad != _currentSelectedPad) {
        val currentTime = SystemClock.elapsedRealtime()
        
        if (currentTime - _lastPadSwitchTime < PAD_SWITCH_DEBOUNCE_MS) {
            // 디바운스 기간 내 → 전환 무시
            return false
        }
        
        // 영역 전환 처리
        _lastPadSwitchTime = currentTime
        onPadSwitched(detectedPad)
    }
    
    return true
}
```

**상태 데이터 구조**:
```kotlin
data class MultiCursorState(
    var isEnabled: Boolean = false,
    var currentSelectedPad: String? = null,  // "pad1" or "pad2"
    var pad1CursorPosition: Point? = null,   // PC 화면 좌표
    var pad2CursorPosition: Point? = null,
    var lastSwitchTimestamp: Long = 0,
    var isPendingAck: Boolean = false        // 응답 대기 중
)
```

##### 2.2.6.2 영역 전환 감지 및 신호 생성 로직

**터치 이벤트 처리 플로우**:
```kotlin
override fun onTouchEvent(event: MotionEvent): Boolean {
    if (!_multiCursorState.isEnabled) {
        // 멀티 커서 비활성 시 일반 처리
        return super.onTouchEvent(event)
    }
    
    when (event.actionMasked) {
        MotionEvent.ACTION_DOWN -> {
            val touchX = event.x
            val detectedPad = determineTouchpadArea(touchX, width.toFloat())
            
            // 이전 영역과 비교
            if (detectedPad != _multiCursorState.currentSelectedPad) {
                // 영역 전환 발생
                handlePadSwitch(detectedPad)
            }
        }
        
        MotionEvent.ACTION_MOVE -> {
            // 드래그 중에도 영역 전환 감지
            val touchX = event.x
            val detectedPad = determineTouchpadArea(touchX, width.toFloat())
            
            if (detectedPad != _multiCursorState.currentSelectedPad) {
                handlePadSwitch(detectedPad)
            }
        }
    }
    
    return true
}
```

**Selected/Unselected 상태 관리**:
```kotlin
private fun handlePadSwitch(newSelectedPad: String) {
    // 디바운스 확인
    if (!checkDebounce()) return
    
    // 이전 상태 저장 (롤백용)
    val previousPad = _multiCursorState.currentSelectedPad
    
    // 상태 업데이트
    _multiCursorState.currentSelectedPad = newSelectedPad
    _multiCursorState.lastSwitchTimestamp = SystemClock.elapsedRealtime()
    _multiCursorState.isPendingAck = true
    
    // UI 테두리 색상 즉시 업데이트
    updateTouchpadBorderColors(newSelectedPad)
    
    // 커서 전환 JSON 메시지 생성 및 전송
    val targetPosition = if (newSelectedPad == "pad1") {
        _multiCursorState.pad1CursorPosition
    } else {
        _multiCursorState.pad2CursorPosition
    }
    
    if (targetPosition != null) {
        sendMultiCursorSwitchMessage(newSelectedPad, targetPosition)
    } else {
        // 목표 위치가 없으면 현재 커서 위치 사용
        val currentPos = getCurrentCursorPosition()
        sendMultiCursorSwitchMessage(newSelectedPad, currentPos)
    }
    
    // 타임아웃 타이머 시작 (500ms)
    startAckTimeoutTimer(previousPad)
}

private fun updateTouchpadBorderColors(selectedPad: String) {
    // Selected 영역: 보라색 (#B552F6) 테두리
    // Unselected 영역: 테두리 없음
    
    if (selectedPad == "pad1") {
        _pad1BorderView.visibility = View.VISIBLE
        _pad1BorderView.setBackgroundColor(Color.parseColor("#B552F6"))
        _pad2BorderView.visibility = View.GONE
    } else {
        _pad1BorderView.visibility = View.GONE
        _pad2BorderView.visibility = View.VISIBLE
        _pad2BorderView.setBackgroundColor(Color.parseColor("#B552F6"))
    }
}
```

##### 2.2.6.3 JSON 메시지 생성 및 전송 로직

**메시지 구조 정의**:
```kotlin
data class MultiCursorSwitchRequest(
    @SerializedName("command")
    val command: String = "multi_cursor_switch",
    
    @SerializedName("touchpad_id")
    val touchpadId: String,  // "pad1" or "pad2"
    
    @SerializedName("cursor_position")
    val cursorPosition: CursorPosition,
    
    @SerializedName("timestamp")
    val timestamp: String = Instant.now().toString()
)

data class CursorPosition(
    @SerializedName("x")
    val x: Int,
    
    @SerializedName("y")
    val y: Int
)
```

**JSON 직렬화 및 전송**:
```kotlin
private fun sendMultiCursorSwitchMessage(touchpadId: String, position: Point) {
    try {
        // JSON 객체 생성
        val request = MultiCursorSwitchRequest(
            touchpadId = touchpadId,
            cursorPosition = CursorPosition(x = position.x, y = position.y)
        )
        
        // Gson을 사용한 JSON 직렬화
        val gson = Gson()
        val jsonString = gson.toJson(request)
        
        // UTF-8 바이트 배열로 변환
        val jsonBytes = jsonString.toByteArray(Charsets.UTF_8)
        
        // JSON 페이로드 크기 검증 (Phase 2 통신 안정화)
        if (jsonBytes.size > MAX_VENDOR_CDC_PAYLOAD_SIZE) {
            Logger.error("JSON payload size exceeds limit: ${jsonBytes.size} > $MAX_VENDOR_CDC_PAYLOAD_SIZE bytes")
            handleSendFailure()
            return
        }
        
        // UART 전송 (0xFF 헤더는 ESP32-S3에서 추가)
        _usbSerialPort?.write(jsonBytes, USB_WRITE_TIMEOUT_MS)
        
        Logger.debug("Multi-cursor switch message sent: $jsonString (${jsonBytes.size} bytes)")
        
        // 성능 모니터링
        _performanceMonitor.recordMessageSendTime(SystemClock.elapsedRealtime())
        
    } catch (e: IOException) {
        Logger.error("Failed to send multi-cursor switch message: ${e.message}")
        handleSendFailure()
    } catch (e: JsonSyntaxException) {
        Logger.error("JSON serialization failed: ${e.message}")
        handleSendFailure()
    }
}

private const val USB_WRITE_TIMEOUT_MS = 100
private const val MAX_VENDOR_CDC_PAYLOAD_SIZE = 448  // Phase 2 통신 안정화: ESP32-S3 버퍼 제한
```

**전송 실패 처리**:
```kotlin
private fun handleSendFailure() {
    // 상태 롤백
    rollbackToPreviousState()
    
    // 사용자 알림
    showToast("커서 전환 실패", ToastColor.ERROR)
    
    // 햅틱 피드백
    performHapticFeedback(HapticFeedbackConstants.REJECT)
}
```

##### 2.2.6.4 응답 처리 및 상태 동기화

**응답 메시지 구조**:
```kotlin
data class MultiCursorSwitchAck(
    @SerializedName("command")
    val command: String,  // "multi_cursor_switch_ack"
    
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("error_code")
    val errorCode: String?,  // "TIMEOUT", "INVALID_POSITION", null
    
    @SerializedName("actual_position")
    val actualPosition: CursorPosition?,
    
    @SerializedName("latency_ms")
    val latencyMs: Long?,
    
    @SerializedName("timestamp")
    val timestamp: String
)
```

**응답 수신 및 처리**:
```kotlin
private fun onMultiCursorSwitchAckReceived(ack: MultiCursorSwitchAck) {
    // 타임아웃 타이머 취소
    cancelAckTimeoutTimer()
    
    // 대기 플래그 해제
    _multiCursorState.isPendingAck = false
    
    if (ack.success) {
        // 성공 처리
        handleSwitchSuccess(ack)
    } else {
        // 실패 처리
        handleSwitchFailure(ack.errorCode)
    }
}

private fun handleSwitchSuccess(ack: MultiCursorSwitchAck) {
    // 실제 커서 위치 업데이트
    if (ack.actualPosition != null) {
        val position = Point(ack.actualPosition.x, ack.actualPosition.y)
        
        if (_multiCursorState.currentSelectedPad == "pad1") {
            _multiCursorState.pad1CursorPosition = position
        } else {
            _multiCursorState.pad2CursorPosition = position
        }
    }
    
    // 성공 햅틱 피드백
    performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    
    // 성능 지표 기록
    if (ack.latencyMs != null) {
        _performanceMonitor.recordSwitchLatency(ack.latencyMs)
        
        // 목표 초과 시 경고
        if (ack.latencyMs > 100) {
            Logger.warn("Switch latency exceeded target: ${ack.latencyMs}ms > 100ms")
        }
    }
    
    Logger.info("Multi-cursor switch succeeded")
}

private fun handleSwitchFailure(errorCode: String?) {
    when (errorCode) {
        "TIMEOUT" -> {
            // Windows 서버 응답 시간 초과
            showToast("커서 전환 시간 초과", ToastColor.WARNING)
            
            // 재시도 1회
            retryLastSwitch()
        }
        
        "INVALID_POSITION" -> {
            // 좌표 범위 초과
            showToast("잘못된 커서 위치", ToastColor.ERROR)
            
            // 이전 상태로 롤백
            rollbackToPreviousState()
        }
        
        "WINDOWS_SERVICE_DISCONNECTED" -> {
            // Windows 서비스 연결 끊김
            showToast("Windows 서버 연결 끊김", ToastColor.ERROR)
            
            // Essential 모드로 폴백
            fallbackToEssentialMode()
        }
        
        else -> {
            // 알 수 없는 오류
            showToast("커서 전환 실패", ToastColor.ERROR)
            rollbackToPreviousState()
        }
    }
    
    // 오류 햅틱 피드백
    performHapticFeedback(HapticFeedbackConstants.REJECT)
}
```

**타임아웃 처리**:
```kotlin
private var _ackTimeoutJob: Job? = null
private var _previousPadBeforeSwitch: String? = null

private fun startAckTimeoutTimer(previousPad: String?) {
    _previousPadBeforeSwitch = previousPad
    
    _ackTimeoutJob = viewModelScope.launch {
        delay(500) // 500ms 타임아웃
        
        if (_multiCursorState.isPendingAck) {
            // 응답 없음 → 타임아웃 처리
            onAckTimeout()
        }
    }
}

private fun cancelAckTimeoutTimer() {
    _ackTimeoutJob?.cancel()
    _ackTimeoutJob = null
}

private fun onAckTimeout() {
    Logger.warn("Multi-cursor switch ACK timeout")
    
    // 상태 롤백
    rollbackToPreviousState()
    
    // 사용자 알림
    showToast("커서 전환 응답 없음", ToastColor.WARNING)
    
    // 햅틱 피드백
    performHapticFeedback(HapticFeedbackConstants.REJECT)
    
    // 재시도 1회
    retryLastSwitch()
}
```

**타임아웃 시간 설정 근거** (Phase 2 통신 안정화):
- **목표 처리 시간**: 50ms (Windows 서버의 멀티 커서 처리 목표)
- **타임아웃 시간**: 500ms = 50ms × 10 (목표 처리 시간의 10배)
- **근거**:
  - 일반적인 경우: 50ms 이내 응답 (목표치)
  - 시스템 부하 상황: 100-200ms 응답 (허용 범위)
  - 네트워크 지연: 추가 100ms 여유
  - 안전 마진: 2배 여유 (500ms = 250ms × 2)
- **튜닝 가능**: 실제 측정된 지연 시간에 따라 조정 가능

**CRC 오류 처리 및 재전송** (Phase 2 통신 안정화):
```kotlin
// 오류 응답 데이터 클래스
data class ErrorResponse(
    @SerializedName("command")
    val command: String,  // "ERROR_RESPONSE"
    
    @SerializedName("error_code")
    val errorCode: String,  // "CRC_MISMATCH", "JSON_PARSE_ERROR"
    
    @SerializedName("error_message")
    val errorMessage: String,
    
    @SerializedName("timestamp")
    val timestamp: String
)

// 재전송 상태 관리
private data class RetryState(
    val originalMessage: ByteArray,
    var retryCount: Int = 0,
    val maxRetries: Int = 3
)

private var _currentRetryState: RetryState? = null

// 오류 응답 수신 핸들러
private fun onErrorResponseReceived(errorResponse: ErrorResponse) {
    when (errorResponse.errorCode) {
        "CRC_MISMATCH" -> {
            // CRC16 불일치 → 재전송
            handleCrcError()
        }
        
        "JSON_PARSE_ERROR" -> {
            // JSON 파싱 오류 → 재전송 없이 사용자에게 알림
            handleJsonParseError(errorResponse.errorMessage)
        }
        
        else -> {
            Logger.error("Unknown error code: ${errorResponse.errorCode}")
            showToast("통신 오류", ToastColor.ERROR)
        }
    }
}

private fun handleCrcError() {
    val retryState = _currentRetryState
    
    if (retryState == null) {
        Logger.error("CRC error received but no retry state exists")
        showToast("통신 오류", ToastColor.ERROR)
        return
    }
    
    if (retryState.retryCount < retryState.maxRetries) {
        // 재전송 실행
        retryState.retryCount++
        
        val backoffDelay = 100L * (1 shl (retryState.retryCount - 1))  // 지수 백오프: 100ms, 200ms, 400ms
        
        Logger.warn("CRC error detected, retrying (${retryState.retryCount}/${retryState.maxRetries}) after ${backoffDelay}ms")
        
        viewModelScope.launch {
            delay(backoffDelay)
            
            try {
                _usbSerialPort?.write(retryState.originalMessage, USB_WRITE_TIMEOUT_MS)
                Logger.info("Message retransmitted (attempt ${retryState.retryCount})")
            } catch (e: IOException) {
                Logger.error("Retransmission failed: ${e.message}")
                handleRetransmissionFailure()
            }
        }
    } else {
        // 최대 재전송 횟수 초과
        Logger.error("Max retries exceeded for CRC error")
        handleRetransmissionFailure()
    }
}

private fun handleJsonParseError(errorMessage: String) {
    Logger.error("JSON parse error from ESP32-S3: $errorMessage")
    
    // JSON 파싱 오류는 재전송으로 해결되지 않으므로 즉시 사용자에게 알림
    showToast("데이터 형식 오류", ToastColor.ERROR)
    
    // 상태 롤백
    rollbackToPreviousState()
    
    // 재전송 상태 초기화
    _currentRetryState = null
}

private fun handleRetransmissionFailure() {
    // 재전송 실패 → 사용자에게 알림 및 상태 롤백
    showToast("통신 오류: 재전송 실패", ToastColor.ERROR)
    rollbackToPreviousState()
    
    // 재전송 상태 초기화
    _currentRetryState = null
}

// sendMultiCursorSwitchMessage에서 재전송 상태 초기화
private fun sendMultiCursorSwitchMessageWithRetry(touchpadId: String, position: Point) {
    try {
        val request = MultiCursorSwitchRequest(
            touchpadId = touchpadId,
            cursorPosition = CursorPosition(x = position.x, y = position.y)
        )
        
        val gson = Gson()
        val jsonString = gson.toJson(request)
        val jsonBytes = jsonString.toByteArray(Charsets.UTF_8)
        
        if (jsonBytes.size > MAX_VENDOR_CDC_PAYLOAD_SIZE) {
            Logger.error("JSON payload size exceeds limit: ${jsonBytes.size} > $MAX_VENDOR_CDC_PAYLOAD_SIZE bytes")
            handleSendFailure()
            return
        }
        
        // 재전송 상태 초기화
        _currentRetryState = RetryState(
            originalMessage = jsonBytes,
            retryCount = 0,
            maxRetries = 3
        )
        
        _usbSerialPort?.write(jsonBytes, USB_WRITE_TIMEOUT_MS)
        Logger.debug("Multi-cursor switch message sent: $jsonString (${jsonBytes.size} bytes)")
        
        _performanceMonitor.recordMessageSendTime(SystemClock.elapsedRealtime())
        
    } catch (e: IOException) {
        Logger.error("Failed to send multi-cursor switch message: ${e.message}")
        handleSendFailure()
    } catch (e: JsonSyntaxException) {
        Logger.error("JSON serialization failed: ${e.message}")
        handleSendFailure()
    }
}
```

**상태 롤백 메커니즘**:
```kotlin
private fun rollbackToPreviousState() {
    // 이전 Selected Pad로 복원
    if (_previousPadBeforeSwitch != null) {
        _multiCursorState.currentSelectedPad = _previousPadBeforeSwitch
        updateTouchpadBorderColors(_previousPadBeforeSwitch!!)
    }
    
    // 대기 플래그 해제
    _multiCursorState.isPendingAck = false
    
    Logger.info("Rolled back to previous pad: $_previousPadBeforeSwitch")
}

private fun retryLastSwitch() {
    if (_retryCount < MAX_RETRY_COUNT) {
        _retryCount++
        
        // 100ms 후 재시도
        viewModelScope.launch {
            delay(100)
            
            val targetPad = _multiCursorState.currentSelectedPad
            val targetPos = if (targetPad == "pad1") {
                _multiCursorState.pad1CursorPosition
            } else {
                _multiCursorState.pad2CursorPosition
            }
            
            if (targetPos != null) {
                sendMultiCursorSwitchMessage(targetPad!!, targetPos)
            }
        }
    } else {
        // 최대 재시도 횟수 초과
        Logger.error("Max retry count exceeded")
        rollbackToPreviousState()
        _retryCount = 0
    }
}

private var _retryCount = 0
private const val MAX_RETRY_COUNT = 1
```

##### 2.2.6.5 멀티 커서 모드 활성화/비활성화 로직

**멀티 커서 모드 활성화**:
```kotlin
fun enableMultiCursorMode() {
    if (_multiCursorState.isEnabled) {
        Logger.warn("Multi-cursor mode already enabled")
        return
    }
    
    try {
        // 1. 터치패드 영역 분할
        val (pad1, pad2) = divideTouchpadAreas(width.toFloat(), height.toFloat())
        _pad1Area = pad1
        _pad2Area = pad2
        
        // 2. 초기 커서 위치 저장
        val currentPos = getCurrentCursorPosition()
        _multiCursorState.pad1CursorPosition = currentPos
        _multiCursorState.pad2CursorPosition = currentPos.copy() // 동일 위치로 초기화
        
        // 3. 기본 활성 영역 설정 (pad1)
        _multiCursorState.currentSelectedPad = "pad1"
        
        // 4. UI 업데이트
        showPadDivider() // 중앙 분할선 표시
        updateTouchpadBorderColors("pad1")
        
        // 5. Windows 서비스에 가상 커서 표시 요청
        sendShowVirtualCursorMessage()
        
        // 6. 상태 플래그 활성화
        _multiCursorState.isEnabled = true
        
        // 7. 사용자 피드백
        showToast("멀티 커서 활성화", ToastColor.INFO)
        performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        
        // 8. 분할 애니메이션 재생 (300ms ease-out)
        playPadDivisionAnimation()
        
        Logger.info("Multi-cursor mode enabled")
        
    } catch (e: Exception) {
        Logger.error("Failed to enable multi-cursor mode: ${e.message}")
        showToast("멀티 커서 활성화 실패", ToastColor.ERROR)
    }
}

private fun sendShowVirtualCursorMessage() {
    val message = mapOf(
        "command" to "show_virtual_cursor",
        "initial_position" to mapOf(
            "x" to _multiCursorState.pad2CursorPosition?.x,
            "y" to _multiCursorState.pad2CursorPosition?.y
        ),
        "timestamp" to Instant.now().toString()
    )
    
    val jsonString = Gson().toJson(message)
    val jsonBytes = jsonString.toByteArray(Charsets.UTF_8)
    _usbSerialPort?.write(jsonBytes, USB_WRITE_TIMEOUT_MS)
}
```

**멀티 커서 모드 비활성화**:
```kotlin
fun disableMultiCursorMode() {
    if (!_multiCursorState.isEnabled) {
        Logger.warn("Multi-cursor mode already disabled")
        return
    }
    
    try {
        // 1. Windows 서비스에 가상 커서 숨김 요청
        sendHideVirtualCursorMessage()
        
        // 2. 터치패드 통합 (분할 해제)
        hidePadDivider()
        _pad1Area = null
        _pad2Area = null
        
        // 3. UI 초기화
        _pad1BorderView.visibility = View.GONE
        _pad2BorderView.visibility = View.GONE
        
        // 4. 상태 데이터 정리
        _multiCursorState.currentSelectedPad = null
        _multiCursorState.pad1CursorPosition = null
        _multiCursorState.pad2CursorPosition = null
        _multiCursorState.isPendingAck = false
        
        // 5. 상태 플래그 비활성화
        _multiCursorState.isEnabled = false
        
        // 6. 사용자 피드백
        showToast("싱글 커서 복원", ToastColor.INFO)
        performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        
        // 7. 통합 애니메이션 재생 (200ms ease-in)
        playPadMergeAnimation()
        
        Logger.info("Multi-cursor mode disabled")
        
    } catch (e: Exception) {
        Logger.error("Failed to disable multi-cursor mode: ${e.message}")
        showToast("싱글 커서 복원 실패", ToastColor.ERROR)
    }
}

private fun sendHideVirtualCursorMessage() {
    val message = mapOf(
        "command" to "hide_virtual_cursor",
        "timestamp" to Instant.now().toString()
    )
    
    val jsonString = Gson().toJson(message)
    val jsonBytes = jsonString.toByteArray(Charsets.UTF_8)
    _usbSerialPort?.write(jsonBytes, USB_WRITE_TIMEOUT_MS)
}
```

**Essential 모드 폴백 처리**:
```kotlin
private fun fallbackToEssentialMode() {
    // 1. 멀티 커서 모드 즉시 비활성화
    if (_multiCursorState.isEnabled) {
        disableMultiCursorMode()
    }
    
    // 2. AppState를 Essential로 전환
    _appState.value = AppState.Essential
    
    // 3. 고급 기능 모두 비활성화
    disableAdvancedFeatures()
    
    // 4. 사용자 알림
    showToast("Essential 모드로 전환", ToastColor.WARNING)
    
    // 5. 멀티 커서 설정 보존 (재연결 시 복원용)
    saveMultiCursorSettingsForRestore()
    
    Logger.warn("Fallback to Essential mode due to server disconnection")
}

private fun saveMultiCursorSettingsForRestore() {
    // SharedPreferences에 멀티 커서 설정 저장
    val prefs = context.getSharedPreferences("multi_cursor_prefs", Context.MODE_PRIVATE)
    prefs.edit {
        putBoolean("was_enabled", _multiCursorState.isEnabled)
        putInt("pad1_x", _multiCursorState.pad1CursorPosition?.x ?: 0)
        putInt("pad1_y", _multiCursorState.pad1CursorPosition?.y ?: 0)
        putInt("pad2_x", _multiCursorState.pad2CursorPosition?.x ?: 0)
        putInt("pad2_y", _multiCursorState.pad2CursorPosition?.y ?: 0)
    }
}

fun restoreMultiCursorSettingsAfterReconnect() {
    val prefs = context.getSharedPreferences("multi_cursor_prefs", Context.MODE_PRIVATE)
    val wasEnabled = prefs.getBoolean("was_enabled", false)
    
    if (wasEnabled) {
        // 멀티 커서 설정 복원
        enableMultiCursorMode()
        
        _multiCursorState.pad1CursorPosition = Point(
            prefs.getInt("pad1_x", 0),
            prefs.getInt("pad1_y", 0)
        )
        _multiCursorState.pad2CursorPosition = Point(
            prefs.getInt("pad2_x", 0),
            prefs.getInt("pad2_y", 0)
        )
        
        Logger.info("Multi-cursor settings restored after reconnect")
    }
}
```

**성능 상수 및 임계값:**
- **전환 지연시간**: 0.1초 이하 목표 (싱글↔멀티 양방향)
- **메모리 사용량**: 싱글 커서 모드 대비 20% 이하 증가
- **배터리 소모**: 고급 기능 사용 시 15% 이하 추가 소모
- **응답 타임아웃**: 500ms 내 ACK 대기
- **재연결 타임아웃**: 3초 내 자동 폴백 처리
- **재시도 횟수**: 최대 1회

**UI 구조 상수:**
- **TouchpadWrapper**: 1:2 비율, 최소 160dp×320dp
- **터치패드 영역**: 싱글 모드 전체, 멀티 모드 50% 분할
- **제어 버튼**: 터치패드 너비의 8% × 16%, 최소 24dp×48dp
- **테두리 두께**: 터치패드 너비의 0.8% (최소 2dp, 최대 4dp)
- **분할선 두께**: 2dp, 색상 #C2C2C2 (alpha 0.3)

### 2.3 컴포넌트 설계 요구사항

#### 2.3.1 공통 컴포넌트 설계 요구사항

##### 2.3.1.1 상태 알림 토스트 구현 요구사항

**위치 및 크기 구현 사항:**
- 화면 상단 중앙 배치 (상단에서 15% 지점, 상태표시줄 아래)
- 너비: 화면 폭의 90% (최대 400dp)
- 높이: 고정 56dp
- 여백: 화면 좌우 가장자리에서 16dp

**배경 및 스타일 구현 사항:**
- 배경색: 상태별 동적 색상 적용 (#2196F3, #4CAF50, #FF9800, #F44336)
- 모서리: 완전 둥근 모서리 (28dp radius)
- 그림자: #000000 (30% 투명도, 6dp blur, 2dp offset)
- 형태: 알약 모양 (양쪽 끝이 완전히 둥근 직사각형)

**내부 구성 구현 사항:**
- 아이콘 (좌측): 24dp × 24dp, 좌측 여백 16dp
- 텍스트 (중앙): 16sp, #121212, Pretendard-Medium
- 여백: 아이콘과 텍스트 간 12dp
- 패딩: 상하 16dp, 좌우 20dp
- 정렬: 수직 중앙 정렬

**애니메이션 구현 사항:**
- 등장 애니메이션: 화면 상단에서 아래로 (translateY: -100dp → 0dp), 350ms, EaseOutBack
- 사라짐 애니메이션: 화면 상단으로 위로 (translateY: 0dp → -100dp), 300ms, EaseInBack
- 상태 전환: 250ms 트랜지션 (EaseInOut)

**음성 알림 구현 사항:**
- TTS 엔진: Android 기본 Text-to-Speech 엔진 활용
- 재생 제어: 중복 재생 방지 및 시스템 볼륨 설정 존중
- 음소거 모드 우회: AudioManager.STREAM_ALARM 사용으로 시스템 음소거 모드에서도 배터리 부족 알림 재생
- 성능 최적화: 음성 알림 재생 시 메인 스레드 블로킹 방지

##### 2.3.1.2 페이지 인디케이터 구현 요구사항

**위치 및 크기 구현 사항:**
- 화면 상단 중앙 배치 (상단에서 15% 지점, 상태표시줄 아래)
- 전체 너비: 자동 조절 (페이지 수에 따라, 최대 300dp)
- 높이: 고정 12dp (인디케이터 간 여백 포함)
- 여백: 화면 좌우 가장자리에서 16dp, 상단 8dp, 하단 16dp
- 배치 우선순위: 상태 알림 토스트 아래, 다른 컴포넌트 위

**배경 및 스타일 구현 사항:**
- 배경: 투명 배경 (컨테이너에만 적용)
- 컨테이너 형태: 완전한 직사각형 (모서리 4dp radius)
- 그림자: #000000 (20% 투명도, 2dp blur, 1dp offset)
- 인디케이터 형태: 완전한 원형 (CircleShape)
- 색상 시스템: 디자인 가이드 색상 토큰 활용 (#C2C2C2, #2196F3)

**내부 구성 구현 사항:**
- 인디케이터 타입: 점(dot) 형태로 통일
- Unselected 상태: 지름 8dp, 60% 투명도
- Selected 상태: 지름 12dp, 100% 투명도
- 간격: 인디케이터 간 8dp
- 정렬: 수평 중앙 정렬 (Row 컴포넌트 사용)
- 최대 표시 개수: 7개 (초과 시 양쪽에 페이드 효과)

**애니메이션 구현 사항:**
- 크기 전환: 200ms Spring 애니메이션 (dampingRatio = 0.6f, stiffness = 300f)
- 색상 전환: 150ms Tween 트랜지션 (EaseInOut)
- 투명도 전환: 100ms Tween 트랜지션 (EaseOut)
- 위치 전환: 300ms Spring 애니메이션 (전체 인디케이터 컨테이너)
- 페이지 전환 시: 인디케이터 간 자연스러운 전환 애니메이션

**컴포넌트 연동 구현 사항:**
- ViewPager2 연동: ViewPager2.OnPageChangeCallback 구현
- TabLayout 연동: TabLayout.OnTabSelectedListener 구현
- 상태 관리: ViewModel을 통한 선택 상태 관리
- 페이지 수 동적 처리: FragmentStateAdapter 연동
- 초기화: ViewPager2.adapter 등록 후 인디케이터 설정

**성능 최적화 구현 사항:**
- 메모리 누수 방지: OnPageChangeCallback 적절한 해제
- 불필요한 리렌더링 방지: 선택 상태 변경 시에만 업데이트
- 애니메이션 최적화: 하드웨어 가속 활용 (LayerType.HARDWARE)
- 배치 최적화: ConstraintLayout 또는 Compose 사용 권장
- 대용량 페이지 처리: 가상화 기법 적용 고려

##### 2.3.1.3 햅틱 피드백 구현 요구사항

**API 및 엔진 구현 사항:**
- Vibrator API 활용: VibrationEffect.createOneShot(), createWaveform() 사용
- Android 버전 호환성: API 26 이상에서 VibrationEffect 우선 사용, 하위 버전은 vibrate() 폴백
- HapticFeedbackConstants: performHapticFeedback() 메서드 활용
- 진동 권한: VIBRATE 권한 Manifest 선언 및 런타임 권한 요청
- 서비스 관리: 별도 HapticFeedbackManager 싱글톤 클래스 구현

**피드백 카테고리별 구현 사항:**

**가벼운 피드백 (Light) 구현 사항:**
- 진동 패턴: 15ms 지속, 50% 진폭 (VibrationEffect.createOneShot(15, 128))
- 용도: 일반 버튼 터치, 토글 스위치, 경미한 상태 변화
- 사용 시나리오: 기본적인 터치 조작, 설정 토글, 메뉴 선택
- 트리거: performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)

**중간 피드백 (Medium) 구현 사항:**
- 진동 패턴: 30ms 지속, 80% 진폭 (VibrationEffect.createOneShot(30, 200))
- 용도: 모드 전환, 설정 변경 확정, 페이지 인디케이터 점프
- 사용 시나리오: 중요한 설정 변경, 모드 전환, 확인 액션
- 트리거: performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

**강한 피드백 (Strong) 구현 사항:**
- 진동 패턴: 50ms 지속, 100% 진폭 (VibrationEffect.createOneShot(50, 255))
- 용도: 페이지 전환, 중요 실행, 연결 성공
- 사용 시나리오: 페이지 전환, 중요한 액션 실행, 성공 알림
- 트리거: performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)

**오류 피드백 (Error) 구현 사항:**
- 진동 패턴: 100ms-200ms-100ms 패턴 (VibrationEffect.createWaveform(long[]{100, 200, 100}, -1))
- 용도: 입력 무효, 연결 실패, 경고
- 사용 시나리오: 잘못된 입력, 연결 실패, 유효성 검사 실패
- 트리거: performHapticFeedback(HapticFeedbackConstants.REJECT)

**진행 피드백 (Progressive/Scrolling) 구현 사항:**
- 진동 패턴: 10ms 지속, 30% 진폭, 100ms 간격 (VibrationEffect.createWaveform(long[]{10}, new int[]{75}, 0))
- 용도: 스크롤/드래그 진행감, 무한 스크롤 임계 진입
- 사용 시나리오: 스크롤 진행, 드래그 조작, 로딩 상태
- 트리거: 스크롤 이벤트 감지 시 조건부 실행

**사용자 설정 및 권한 구현 사항:**
- 설정 토글: SharedPreferences를 통한 사용자별 햅틱 피드백 활성화 상태 저장
- 시스템 설정 연동: Settings.System.VIBRATE_ON 설정 확인
- 런타임 권한: POST_NOTIFICATIONS 권한 요청 (Android 13 이상)
- 접근성 고려: 진동 강도 조절 옵션 제공 (약함/중간/강함)

**성능 및 배터리 최적화 구현 사항:**
- 디바운스 처리: 50ms 이내 반복 입력 시 진동 스로틀링
- 배터리 모드 감지: BatteryManager.BATTERY_STATUS_LOW 시 진동 생략
- 백그라운드 처리: 앱 포그라운드 상태에서만 진동 실행
- 메모리 효율: 진동 패턴 상수화를 통한 메모리 절약
- 전력 소비 최적화: 불필요한 진동 호출 방지

**접근성 및 호환성 구현 사항:**
- 스크린 리더 대응: TalkBack 활성 시 진동 강도 50% 감소
- 방해 금지 모드: DND 모드 감지 및 진동 생략 (NotificationManager.INTERRUPTION_FILTER_ALL)
- 고령자 모드: 접근성 설정에서 진동 비활성화 옵션 제공
- 기기 호환성: hasVibrator() 메서드로 진동 지원 여부 확인
- 대체 수단: 진동 불가 시 시각적 피드백(잠깐 깜빡임) 제공

**테스트 및 검증 구현 사항:**
- 단위 테스트: 각 피드백 카테고리별 진동 실행 테스트
- 통합 테스트: UI 상호작용 시 적절한 햅틱 피드백 발생 확인
- 성능 테스트: 진동 실행 시 프레임 드랍 측정
- 배터리 영향도 테스트: 연속 진동 시 배터리 소모량 측정
- 사용자 피드백 수집: A/B 테스트를 통한 최적 진동 패턴 도출

#### 2.3.2 특수 컴포넌트 설계 요구사항

##### 2.3.2.1 KeyboardKeyButton 컴포넌트 설계 요구사항

**KeyboardKeyButton 핵심 구현 요구사항:**

**키 입력 처리:**
- **즉시 키 전송**: 터치 다운 시 `KEY_DOWN` 신호를 USB-Serial을 통해 ESP32-S3 동글 → HID Boot Keyboard로 전송
- **즉시 키 해제**: 터치 업 시 `KEY_UP` 신호를 동일 경로로 전송
- **BIOS/BitLocker 호환성**: Boot Protocol에서 A-Z, 0-9, 특수키만 지원 (Report Protocol에서 모든 키 지원)

**Sticky Hold 기능:**
- **롱프레스 감지**: `KEY_STICKY_HOLD_LONG_PRESS_THRESHOLD_MS = 500ms` 임계시간으로 길게 누르기 감지
- **Fill 애니메이션**: 진행률에 따라 배경이 좌측에서 우측으로 `#1976D2` 색상으로 채워지는 애니메이션
- **상태 전이**: 임계시간 도달 시 `isStickyLatched = true`로 전환, 키 다운 상태 유지
- **라치 해제**: 다음 탭 시 키 업 전송 후 `isStickyLatched = false`로 복귀
- **시각적 피드백**: 버튼 배경이 `#1976D2`로 완전히 채워진 상태로 Sticky Hold 표시

**상태 모델:**
```kotlin
data class KeyboardKeyButtonState(
    val isEnabled: Boolean = true,
    val isStickyLatched: Boolean = false,
    val longPressProgress: Float = 0.0f,  // 0.0f to 1.0f
    val isLongPressInProgress: Boolean = false
)
```

**애니메이션 처리:**
- **스케일 애니메이션**: 200ms 지속, 0.98배 → 1.0배 스케일 변화
- **Fill 애니메이션**: 500ms 지속, 진행률에 따른 수평 채우기 효과
- **상태 전환**: 150-250ms `tween` 애니메이션으로 부드러운 색상 전이

**접근성 처리:**
- **내용 설명**: `contentDescription`에 "(Sticky Hold 가능)" 부가 정보 포함
- **상태 알림**: Sticky Hold 전이 시 토스트로 상태 변경 알림

##### 2.3.2.2 ShortcutButton 컴포넌트 설계 요구사항

**ShortcutButton 핵심 구현 요구사항:**

**키 조합 시퀀스 처리:**
- **순차 키 전송**: 정의된 키 조합을 순서대로 KeyDown/KeyUp 처리
- **표준 시퀀스**: "모디파이어 Down → 일반키 Down → 일반키 Up → 모디파이어 Up"
- **예시 구현**: `Ctrl+C` = `MOD_CTRL` Down → `KEY_C` Down → `KEY_C` Up → `MOD_CTRL` Up

**디바운스 처리:**
- **중복 입력 방지**: `REINPUT_DEBOUNCE_MS = 150ms` 내 재탭 무시
- **타이머 관리**: 각 단축키별 독립적 디바운스 타이머 운영
- **상태 플래그**: `isDebouncing` 플래그로 중복 실행 방지

**상태 모델:**
```kotlin
data class ShortcutButtonState(
    val isEnabled: Boolean = true,
    val isDebouncing: Boolean = false,
    val lastExecutionTime: Long = 0L
)
```

**키 매핑 처리:**
- **플랫폼 매핑**: `windows.vk` Virtual-Key 코드로 Windows 키보드 이벤트 생성
- **HID 매핑**: 필요 시 HID Usage Page/Usage ID로 USB HID 이벤트 생성
- **Android 매핑**: Android KeyCode로 로컬 테스트 및 검증

**오류 처리:**
- **권한 부족**: UAC 권한 필요한 시퀀스 실행 실패 시 사용자 알림
- **타임아웃**: 개별 키 전송 간격 10ms로 신뢰성 보장

##### 2.3.2.3 MacroButton 컴포넌트 설계 요구사항

**MacroButton 핵심 구현 요구사항:**

**매크로 ID 관리:**
- **UUID v4 형식**: 각 매크로는 UUID v4 형식의 고유 식별자를 가짐 (예: `550e8400-e29b-41d4-a716-446655440000`)
- **하드코딩 방식**: MacroButton 생성 시 `macroId` 파라미터로 UUID 전달
- **Orbit 프로그램 연동**: 동일한 UUID로 Orbit에서 매크로 등록 및 실행

**매크로 실행 플로우:**
- **실행 요청**: `MACRO_START_REQUEST(macroId)`를 JSON 형식으로 생성
- **JSON 크기 검증** (Phase 2 통신 안정화): UTF-8 인코딩 후 448 bytes 이하 확인
- **UART 전송**: 0xFF 헤더로 시작하는 커스텀 명령 프레임으로 ESP32-S3에 전송
- **JSON 구조**: `{"command": "MACRO_START_REQUEST", "macro_id": "uuid-v4", "timestamp": "ISO-8601"}`
- **즉시 비활성화**: 요청 전송 후 `isEnabled = false`로 전환하여 중복 실행 방지
- **응답 대기**: `onNewData()` 콜백으로 `MACRO_RESULT` 수신 대기
- **오류 처리** (Phase 2 통신 안정화): 
  - `CRC_MISMATCH` 수신 시 자동 재전송 (최대 3회, 지수 백오프)
  - `JSON_PARSE_ERROR` 수신 시 즉시 사용자에게 오류 알림 (재전송 없음)
  - 재전송 실패 시 "통신 오류: 재전송 실패" 토스트 표시

**통신 경로 상세:**
```
MacroButton 탭
  ↓ JSON 생성: MACRO_START_REQUEST
  ↓ UART 전송 (0xFF 헤더)
ESP32-S3 중계
  ↓ Vendor CDC 프레임 재구성 (CRC16 추가)
Windows 서버 중계
  ↓ Named Pipe 전송 (\\.\pipe\BridgeOne_Orbit)
Orbit 프로그램 실행
  ↓ Named Pipe 응답: MACRO_RESULT
Windows 서버 중계
  ↓ Vendor CDC 프레임 전송
ESP32-S3 중계
  ↓ JSON 파싱 및 UART 전송
MacroButton 응답 처리
  ↓ UI 상태 업데이트
```

**타임아웃 처리:**
- **실행 타임아웃**: `MACRO_TIMEOUT_MS = 30000ms` (30초)로 무한 대기 방지
- **타이머 관리**: 실행 시작 시점부터 독립적 타임아웃 타이머 시작
- **자동 복구**: 타임아웃 발생 시 `isEnabled = true`로 자동 복원
- **타임아웃 응답**: ESP32-S3 또는 Windows 서버에서 `MACRO_TIMEOUT` 응답 수신 시에도 동일 처리

**강제 해제 처리:**
- **재탭 감지**: 비활성화 상태에서 동일 버튼 재탭 시 강제 해제 모드 진입
- **취소 요청**: `MACRO_CANCEL_REQUEST(macroId)` JSON 생성 및 전송
- **강제 활성화**: `UI_FORCE_ENABLE_ALL_TOUCHABLES_REQUEST` 전송으로 전체 UI 복원
- **취소 경로**: Android → ESP32-S3 → Windows → Orbit (실행 중단)

**오류 처리 및 사용자 피드백:**

| 응답 코드 | 토스트 색상 | 메시지 예시 | 햅틱 | UI 복원 |
|----------|-----------|-----------|------|---------|
| `MACRO_RESULT (success=true)` | 녹색 (#4CAF50) | "매크로 실행 완료" | Success (40ms) | 자동 |
| `MACRO_RESULT (success=false)` | 빨간색 (#F44336) | "매크로 실행 실패: {error_message}" | Error (100ms-200ms-100ms) | 자동 |
| `ORBIT_NOT_CONNECTED` | 빨간색 (#F44336) | "Orbit 프로그램이 실행되지 않았습니다" | Error | 즉시 |
| `MACRO_TIMEOUT` | 주황색 (#FF9800) | "매크로 실행 시간 초과 (30초)" | Medium (30ms) | 자동 |
| `MACRO_NOT_FOUND` | 빨간색 (#F44336) | "존재하지 않는 매크로입니다" | Error | 즉시 |
| `MACRO_CANCEL_REQUEST` | 주황색 (#FF9800) | "매크로 실행 취소됨" | Medium (30ms) | 즉시 |
| **진행 중** | 파란색 (#2196F3) | "매크로 실행 중..." | - | 1초 간격 갱신 |

**상태 모델:**
```kotlin
data class MacroButtonState(
    val isEnabled: Boolean = true,
    val isExecuting: Boolean = false,
    val executionStartTime: Long = 0L,
    val macroId: String = "", // UUID v4 형식
    val lastErrorMessage: String? = null
)
```

**JSON 메시지 구조 상세:**

**Android → ESP32-S3 (MACRO_START_REQUEST)**:
```json
{
  "command": "MACRO_START_REQUEST",
  "macro_id": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2025-10-21T12:34:56.789Z"
}
```

**ESP32-S3 → Android (MACRO_RESULT)**:
```json
{
  "command": "MACRO_RESULT",
  "macro_id": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2025-10-21T12:35:01.234Z",
  "payload": {
    "success": true,
    "error_message": null
  }
}
```

**ESP32-S3 → Android (오류 응답)**:
```json
{
  "command": "MACRO_RESULT",
  "macro_id": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2025-10-21T12:35:01.234Z",
  "payload": {
    "success": false,
    "error_message": "ORBIT_NOT_CONNECTED"
  }
}
```

##### 2.3.2.4 ContainerButton 컴포넌트 설계 요구사항

**ContainerButton 핵심 구현 요구사항:**

**팝업 오버레이 처리:**
- **일반 모드**: 탭 시 `isPopupOpen = true`로 팝업 표시, 하위 버튼 선택 시 자동 닫힘
- **지속 모드**: 롱프레스 `CONTAINER_POPUP_LONG_PRESS_THRESHOLD_MS = 500ms`로 진입
- **스크림**: `#121212` (alpha 0.4) 배경으로 다른 영역 클릭 시 팝업 닫힘
- **컨테이너**: `#2196F3` 테두리, `#2A2A2A` 배경, 둥근 모서리 12dp

**롱프레스 Fill 애니메이션:**
- **진행률 표시**: 버튼 배경이 좌측에서 우측으로 `#1976D2` 색상으로 채워지는 효과
- **지속 모드 전이**: 임계시간 도달 시 `isPersistentMode = true`로 전환
- **애니메이션 중단**: 터치 해제 시 즉시 기본 배경으로 복귀

**하위 버튼 관리:**
- **동적 생성**: `childButtons: List<ChildButton>`으로 런타임 버튼 구성
- **선택 처리**: `selectedChildButton`으로 현재 선택된 하위 버튼 추적
- **그리드 배치**: 하위 버튼들을 컨테이너 내 그리드 형태로 자동 배치

**상태 모델:**
```kotlin
data class ContainerButtonState(
    val isEnabled: Boolean = true,
    val isPopupOpen: Boolean = false,
    val isPersistentMode: Boolean = false,
    val childButtons: List<ChildButton> = emptyList(),
    val selectedChildButton: ChildButton? = null,
    val longPressProgress: Float = 0.0f,
    val isLongPressInProgress: Boolean = false
)
```

**접근성 처리:**
- **키보드 네비게이션**: Tab 키로 하위 버튼 간 이동 가능
- **화면 리더**: 팝업 열림/닫힘 상태 음성 안내
- **포커스 관리**: 선택된 하위 버튼에 포커스 자동 이동

##### 2.3.2.5 DPad 컴포넌트 설계 요구사항

**DPad 핵심 구현 요구사항:**

**8분할 섹터 판정 알고리즘:**
- **섹터 정의**: 8방향 (0°, 45°, 90°, 135°, 180°, 225°, 270°, 315°)
- **중심 영역**: 컨테이너 반지름의 30% (무시 영역)
- **각도 계산**: `atan2(dy, dx)`로 중심점 기준 벡터 각도 계산
- **허용오차**: ±10°로 섹터 경계에서 안정적 판정

**방향 전환 처리 로직:**
```kotlin
enum class Direction {
    UP, DOWN, LEFT, RIGHT,
    UP_LEFT, UP_RIGHT, DOWN_LEFT, DOWN_RIGHT, CENTER
}

fun calculateDirection(touchPoint: PointF, center: PointF): Direction {
    val dx = touchPoint.x - center.x
    val dy = touchPoint.y - center.y
    val angle = Math.toDegrees(atan2(dy, dx)).toFloat()

    return when {
        distance < centerRadius * 0.3 -> Direction.CENTER
        angle in -22.5..22.5 -> Direction.RIGHT
        angle in 22.5..67.5 -> Direction.UP_RIGHT
        angle in 67.5..112.5 -> Direction.UP
        angle in 112.5..157.5 -> Direction.UP_LEFT
        angle in 157.5..202.5 -> Direction.LEFT
        angle in 202.5..247.5 -> Direction.DOWN_LEFT
        angle in 247.5..292.5 -> Direction.DOWN
        angle in 292.5..337.5 -> Direction.DOWN_RIGHT
        else -> Direction.RIGHT  // 337.5..360/-22.5..0
    }
}
```

**대각선 입력 처리:**
- **키 조합**: `UP_LEFT = UP + LEFT` 동시 키 다운/업 전송
- **순서 보장**: 이전 방향 키 업 → 새 방향 키 다운 순서 엄격 준수
- **레이턴시**: 방향 전환 시 최대 1프레임(6ms) 내 처리

**상태 관리:**
```kotlin
data class DPadState(
    val isEnabled: Boolean = true,
    val currentDirection: Direction? = null,
    val activePointerId: Int? = null,
    val touchStartPoint: PointF? = null,
    val currentStickyDirection: Direction? = null,
    val lastTapTime: Long = 0L,
    val lastTapDirection: Direction? = null
)
```

**Sticky Hold 상태 분류:**
- 일반 상태: `currentStickyDirection == null`
- 홀드 상태: `currentStickyDirection != null` (단일 방향 홀드)

**디바운스 처리:**
- **방향 전환**: `DIRECTION_DEBOUNCE_MS = 50ms`로 빠른 전환 시 안정성 확보
- **중복 입력**: 동일 방향 연속 탭 시 50ms 내 무시

**Sticky Hold 처리:**
- **더블탭 감지**: `STICKY_HOLD_DOUBLE_TAP_MS = 300ms`로 길게 누르기와 구분
- **홀드 상태 관리**: `currentStickyDirection`으로 단일 방향 홀드 관리
- **키 매핑**: 홀드 진입 시 키다운 상태 유지, 해제 시 키업 전송
- **시각적 피드백**: VectorDrawable 기반 방향 하이라이트와 색상 구분
- **해제 조건**: 홀드 중인 방향을 탭하거나 다른 방향을 터치 시 홀드 해제

**성능 요구사항:**
- **입력 지연**: 50ms 이하
- **60fps 애니메이션**: Sticky Hold 전환 시 부드러운 애니메이션 유지
- **터치 추적 연속성**: 드래그 전환 시 포인터 추적 안정성 보장

### 2.4 UI 상태 제어 시스템

#### 2.4.1 컴포넌트 비활성화 시스템 설계

**상태 관리 아키텍처 요구사항:**
- 사용자 작업 진행 중 UI 요소의 일시적 비활성화를 통한 의도치 않은 입력 방지
- 작업 완료 또는 사용자 취소 시 자동 복구를 통한 원활한 사용자 경험 제공
- 다양한 비활성화 범위(개별/전체)와 복구 메커니즘 지원

**상태 전이 요구사항:**
- 활성 상태에서 개별 컴포넌트 비활성화로 전환 가능
- 활성 상태에서 전체 페이지 비활성화로 전환 가능
- 작업 완료 신호 수신 시 자동 활성화 복구
- 강제 해제 요청 시 즉시 활성화 복구

**상태 관리 요구사항:**
- 개별 컴포넌트별 비활성화 상태 독립적 관리
- 전체 비활성화 상태와 개별 상태의 우선순위 처리
- 비활성화 원인 및 소스 컴포넌트 정보 보존
- 스레드 안전한 상태 업데이트 보장

**상태 데이터 구조 요구사항:**
- 비활성화된 컴포넌트 ID 목록 관리
- 전체 비활성화 여부 플래그 관리
- 비활성화 사유 및 소스 컴포넌트 추적
- 상태 변경 이력 및 복구 정보 보존

#### 2.4.2 비활성화 표현 시스템 요구사항

**시각적 표현 요구사항:**
- 각 컴포넌트가 사전 정의된 Disabled 상태 스타일을 자체적으로 적용
- 컴포넌트별 비활성화 색상 및 투명도 설정 (예: 버튼 `#C2C2C2`, Alpha 0.6)
- 일관된 비활성화 표현을 위한 컴포넌트별 스타일 가이드라인 준수
- 부드러운 전환 애니메이션으로 자연스러운 상태 변화

**컴포넌트 자체 상태 관리 요구사항:**
- 각 컴포넌트가 개별적으로 Disabled 상태 시각적 표현 적용
- 컴포넌트 수준에서 입력 처리 차단 및 시각적 피드백 제공
- 정보성 요소(페이지 인디케이터, 토스트)는 정상 표시 유지
- 시각적 계층 구조 유지로 사용자 혼란 방지
- 성능 최적화를 위한 효율적인 렌더링 구조

**DPad Disabled 상태 특화 처리 요구사항:**
- **강제 재연결 예외**: 기본 HID 키보드 기능(방향키)이므로 재연결 중에도 정상 동작
- **Disabled 상태로 전환하지 않음**: `UI_DISABLE_ALL_TOUCHABLES_REQUEST` 수신 시에도 DPad 기능 유지
- **상태 정리**: Disabled 진입 시 현재 선택 방향과 Sticky Hold 상태 즉시 정리
- **입력 차단**: 터치 입력은 차단하되, 방향키 HID 전송은 정상 유지
- **시각적 표현**: `#C2C2C2` (alpha 0.6) 오버레이로 비활성 상태 표시

**입력 차단 요구사항:**
- 비활성화된 요소에 대한 터치 입력 완전 차단
- 키보드 포커스 이동 제한
- 접근성 도구와의 호환성 유지
- 예외 상황에서의 안전한 입력 차단 해제

#### 2.4.3 강제 해제 메커니즘 요구사항

**강제 해제 트리거 조건:**
- 전체 비활성화 상태에서 비활성화를 유발한 동일 컴포넌트 재클릭 시 해제
- 소스 컴포넌트 ID와 현재 터치된 컴포넌트 ID 일치 검증 필요
- 즉시 해제 요청 전송 및 상태 초기화 보장

**사용자 의도 파악 요구사항:**
- 동일 컴포넌트 재클릭을 통한 명확한 취소 의도 인식
- 의도치 않은 해제 방지를 위한 정확한 컴포넌트 매칭
- 사용자 피드백을 통한 해제 동작 확인

**강제 해제 처리 요구사항:**
1. 서버에 강제 해제 요청 전송
2. 로컬 상태 즉시 초기화 (서버 응답 대기 없이)
3. 모든 터치 가능한 컴포넌트 활성화
4. 취소 상태 사용자 알림 표시

**예외 상황 처리 요구사항:**
- 네트워크 오류 시에도 로컬 상태는 즉시 해제
- 서버 응답 타임아웃 무시 (로컬 우선 정책)
- 중복 요청 방지를 위한 적절한 디바운스 적용
- 사용자 경험 우선의 안정적인 해제 보장

#### 2.4.4 예외 상황 처리 요구사항

**앱 생명주기 관리 요구사항:**
- 앱 최소화 시 작업 상태 백그라운드 유지
- 영구 저장소에 현재 비활성화 상태 저장
- 작업 진행 시간 및 타임스탬프 기록
- 복원 시 저장된 상태 확인 및 검증
- 작업 시간 초과 여부 판단 (30초 기준)
- 초과 시 자동 상태 해제 및 사용자 확인 다이얼로그
- 진행 중인 경우 상태 유지 및 진행 상황 안내

**연결 상태 변화 처리 요구사항:**
- USB 연결 끊김 감지 시 즉시 비활성화 해제
- 서버 연결 끊김 감지 시 즉시 비활성화 해제
- 모든 진행 중인 작업 강제 중단
- 연결 복구 후 비활성화 해제 상태 유지
- 재실행 옵션 제공 (사용자 선택)
- 이전 작업 상태 정보 제공

**시스템 오류 대응 요구사항:**
- 예상치 못한 예외 발생 시 안전한 작업 중단
- 비활성화 상태 강제 해제
- 오류 정보 로그 저장
- 모든 컴포넌트 즉시 활성화
- 사용자에게 오류 상황 안내
- 앱 재시작 권장 메시지 표시

#### 2.4.5 접근성 및 사용성 요구사항

**접근성 지원 요구사항:**
- 비활성화 상태 변화를 음성으로 안내
- 상태 변화 알림을 위한 Live Region 활용
- 포커스 이동 제한으로 예측 가능한 내비게이션 제공
- 진행 상태를 간단하고 명확하게 음성 안내
- 비활성화된 컴포넌트는 포커스 수신 불가
- Tab 순서에서 제외 처리로 논리적 내비게이션 유지
- 활성 컴포넌트로만 포커스 이동 허용

**사용자 피드백 요구사항:**
- 비활성화 시작 시: Medium 햅틱 (25ms)
- 강제 해제 시: Light 햅틱 (15ms)
- 작업 완료 시: Success 햅틱 (40ms)
- "작업이 취소되었습니다": 2-3초 표시
- "작업을 완료했습니다": 2초 표시
- "작업 중입니다...": 진행 중 상태 표시

### 2.5 Essential/Standard 모드 관리 요구사항

#### 2.5.1 모드 전환 요구사항

**Essential 모드 요구사항:**
- USB 장치 연결 성공 후 서버 핸드셰이크 대기 상태
- TransportState가 UsbReady로 전환되면 자동 진입
- AppState: WaitingForConnection → Essential 전환
- 기본값: 모든 USB 연결 시 먼저 Essential 페이지 표시
- 허용 기능: 일반적인 키보드/마우스 입력
- 터치패드: 탭 → 좌클릭 전송, 드래그 → 포인터 이동 전송
- Del 키: BIOS 진입용 단발 전송
- F1-F12 키: 컨테이너 버튼 탭 → F키 팝업 표시 → 선택 후 자동 닫힘
- 메인 키: Esc/Enter 키 단발 전송
- D-Pad: 방향키 단발 전송

**Standard 모드 요구사항:**
- Windows 서버 실행 및 연결 신호 수신 확인
- 서버-앱 간 핸드셰이크 완료 시에만 Standard 모드로 전환
- 허용 기능: Essential 모드 허용 기능 + 모든 기능
- UI 표시: 모든 컴포넌트 `Enabled` 상태
- 모드 전환 알림: "모든 기능이 활성화되었습니다" (녹색, 3초)

**모드 전환 조건:**
- Essential → Standard: 서버 핸드셰이크 성공 시 자동 전환
- Essential → WaitingForConnection: USB 분리 시 즉시 대기 화면으로 전환
- Standard → Essential: 서버 연결 끊김(Windows 서비스 종료) 시
- 모든 상태 → WaitingForConnection: USB 분리 감지

#### 2.5.2 모드별 기능 제한 요구사항

**Essential 모드 제한:**
- 휠/스크롤 기능 비활성화
- 매크로 실행 기능 비활성화
- 고급 설정 기능 비활성화
- 확장 기능 비활성화
- 서버 의존 기능 모두 비활성화

**Standard 모드 기능:**
- 모든 기능 활성화
- 멀티 커서 모드 지원
- 무한 스크롤 모드 지원
- 매크로 실행 지원
- 고급 설정 지원
- 확장 기능 지원

**강제 재연결 시 특별 처리:**
- Standard 모드 유지
- 서버 의존 기능만 일시 비활성화
- 기본 터치패드, 키보드 입력은 계속 사용 가능
- 재연결 완료 시 모든 기능 자동 활성화

### 2.6 성능 및 품질 요구사항

#### 2.6.1 메모리 관리 요구사항

**객체 풀링 요구사항:**
- 8바이트 프레임 객체 (10개 풀)
- PointF 좌표 객체 (50개 풀)
- 자주 생성/소멸되는 임시 객체들
- 동기화된 풀 사용으로 스레드 안전성 보장
- 객체 사용 후 적절한 초기화로 재사용 가능 상태 유지
- 풀 고갈 시 새 객체 생성으로 안정성 확보

**가비지 컬렉션 최소화 요구사항:**
- 자주 사용되는 객체들을 미리 생성하여 재사용
- 새 객체 생성 대신 기존 객체 초기화 후 재사용
- 사용 후 즉시 초기화로 다음 사용 준비
- 루프 내 객체 생성 금지
- 임시 객체 생성 최소화
- 불필요한 문자열 생성 방지

#### 2.6.2 UI 렌더링 최적화 요구사항

**Compose 최적화 요구사항:**
- State 변경 최소화로 불필요한 recomposition 방지
- derivedStateOf 사용으로 안정적인 State 생성
- Immutable 데이터 클래스로 State 불변성 보장
- Canvas 기반 고성능 그리기 활용
- 하드웨어 가속 레이어 적극 활용
- 자주 변경되지 않는 UI 요소만 Compose로 구성

**애니메이션 최적화 요구사항:**
- 60fps 목표 유지 (16ms 프레임 타임)
- 프레임 드롭 방지를 위한 적응적 업데이트
- GPU 가속 활용으로 부드러운 애니메이션 보장
- 하드웨어 레이어 활용으로 CPU 부하 감소
- 적절한 easing 함수 사용으로 자연스러운 움직임
- 불필요한 애니메이션 중복 방지

#### 2.6.3 통신 성능 최적화 요구사항

**배치 처리 요구사항:**
- 4개 프레임 단위로 배치 처리
- 8ms 타임아웃으로 지연 최소화
- 동기화된 큐 관리로 스레드 안전성 보장
- 여러 프레임을 하나의 전송으로 통합
- 메모리 복사 최소화로 성능 향상
- 배치 크기 달성 시 즉시 전송

**스레드 풀 관리 요구사항:**
- UI 업데이트: 메인 스레드 전용
- 통신 처리: 2개 스레드 풀
- 계산 작업: (코어 수 - 2)개 스레드 풀
- 작업 유형별 적절한 스레드 풀 할당
- CPU 코어 수 기반 최적화
- 스레드 경합 최소화

#### 2.6.4 배터리 최적화 요구사항

**적응적 전송 빈도 요구사항:**
- 디바이스 성능 + 배터리 수준 기반 동적 조절
- 고급 디바이스 (터치 스캔 ≥120Hz) + 배터리 50% 이상: 167Hz 최대 성능 모드
- 중급 디바이스 (터치 스캔 60-120Hz) + 배터리 30% 이상: 120Hz 기본 모드
- 배터리 20% 이하: 83Hz 저전력 모드
- 배터리 10% 이하: 60Hz 배터리 절약 모드
- 배터리 레벨에 따른 전송 빈도 자동 조절
- 고성능/균형/절전 모드별 설정 제공
- 실시간 배터리 상태 모니터링

**디바이스 성능 등급별 목표 설정:**
- **고급 디바이스** (터치 스캔 ≥120Hz): 167Hz 최대 성능 목표
- **중급 디바이스** (터치 스캔 60-120Hz): 120Hz 기본 목표
- **저전력 모드**: 83Hz 안정적 목표
- **배터리 절약 모드**: 60Hz 최소 목표

**적응적 빈도 조절 구현 전략:**
```kotlin
// 디바이스 성능 기반 동적 빈도 설정
class AdaptiveTransmissionRate {
    fun determineOptimalFrequency(): Int {
        val touchScanRate = getDeviceTouchScanRate()
        val batteryLevel = getBatteryLevel()

        return when {
            touchScanRate >= 120 && batteryLevel > 50 -> 167 // 고급 + 충분한 배터리
            touchScanRate >= 90 && batteryLevel > 30 -> 120  // 중급 디바이스
            batteryLevel > 20 -> 83                          // 저전력 모드
            else -> 60                                        // 배터리 절약 모드
        }
    }
}
```

### 2.7 오류 처리 요구사항

#### 2.7.1 오류 타입 정의 및 분류 요구사항

**서버 연결 오류 분류 요구사항:**
- 타임아웃 기반 분류: 연결 시간 초과 상황 구분
- 네트워크 기반 분류: 물리적 연결 문제 구분  
- 프로토콜 기반 분류: 애플리케이션 레벨 문제 구분
- 폴백 액션: 각 오류별 적절한 대안 동작 정의

**서버 연결 오류 타입:**
- Timeout: 서버 응답 시간 초과 (3-5초 타임아웃)
- NetworkError: 네트워크 연결 실패 (Wi-Fi, 데이터 문제)
- HandshakeError: 서버 인증 실패 (프로토콜 불일치)
- UnknownError: 서버 연결 관련 예상치 못한 오류

**폴백 액션 체계:**
- SWITCH_TO_ESSENTIAL: Essential 모드로 안전한 전환
- CHECK_NETWORK: 네트워크 상태 확인 안내
- UPDATE_APP: 앱 업데이트 안내 및 버전 확인
- RETRY_CONNECTION: 서버 재연결 시도

**서버 연결 끊어짐 감지 및 사용자 알림**:
- **연결 끊어짐 감지**: Keep-alive 메커니즘을 통한 실시간 연결 상태 모니터링
- **즉시 알림**: "서버 연결이 끊어졌습니다" (빨간색, 3초) 토스트 표시
- **상태 표시**: 상단 상태 바에서 연결 상태를 실시간으로 업데이트
- **사용자 경험**: 작업 중단 없이 부드러운 상태 전환으로 혼란 최소화

**AppState 전환 및 기능 제한**:
- **Standard → Essential 전환**: 서버 연결 끊어짐 감지 시 즉시 Essential 모드로 전환
- **Essential 전용 페이지 표시**: `styleframe-essential.md`에 정의된 Essential 전용 페이지로 화면 전환
  - 상세 UI/UX 설계: `docs/Android/styleframe-essential.md` 참조
  - 필수 입력 기능: 터치패드(좌클릭, 커서 이동), Boot Keyboard Cluster(Del, Esc, Enter, F1-F12, 방향키)
  - 제한된 기능: 휠/스크롤, 매크로, 고급 설정, 확장 기능 등 서버 의존 기능 비활성화
- **기능 제한 안내**: "Essential 모드로 전환되었습니다" (주황색, 2초) 토스트
- **예외 상황**: Windows 서버의 수동 재연결(§6.1.3.2)은 Essential 모드 전환 없이 별도 처리
- **강제 재연결과의 차이**: 서버 주도 강제 재연결(§8.7)은 화면 전환 없이 현재 페이지 유지

**재연결 시도 정책**:
- **자동 재연결**: 백그라운드에서 지수 백오프 방식으로 재연결 시도 (1 → 2 → 4 → 8초 간격)
- **재연결 진행 표시**: "서버 재연결을 시도하는 중..." (주황색, 무제한) 토스트
- **재연결 성공**: "서버 연결이 복구되었습니다" (녹색, 3초) → Standard 모드 자동 복원
- **재연결 실패**: "서버 연결에 실패했습니다" (빨간색, 4초) → Essential 모드 유지
- **수동 재시도**: 사용자가 앱을 재시작하거나 USB 재연결 시 재시도

**사용자 경험 최적화**:
- **작업 연속성**: 기본 입력 기능은 계속 사용 가능하여 작업 중단 최소화
- **명확한 상태 안내**: 현재 모드와 사용 가능한 기능을 명확히 표시
- **투명한 복구 과정**: 재연결 시도 과정을 사용자에게 투명하게 안내
- **자동 복구**: 재연결 성공 시 이전 설정과 상태 자동 복원
- **예외 상황 처리**:
  - **강제 재연결 시나리오**: 서버 주도 재연결 요청 시 특별한 처리 (§8.7 참조)
  - **USB 연결 끊어짐**: USB 분리 시 Essential 모드도 해제하고 WaitingForConnection으로 전환
  - **앱 최소화/복원**: 백그라운드에서도 재연결 시도 지속, 복원 시 현재 상태 안내

#### 2.7.2 비정상 종료 복구 요구사항

**비정상 종료 감지 요구사항:**
- SharedPreferences 기반 앱 종료 상태 관리
- 정상 종료 플래그: normal_shutdown 불린 값 저장
- 종료 시간 기록: last_shutdown_time 타임스탬프 저장
- 비정상 종료 판정: 플래그 확인 + 시간 경과 검증

**비정상 종료 판정 로직:**
- 정상 종료 플래그 미설정 시 비정상 종료로 판정
- 최대 정상 종료 간격 초과 시 비정상 종료로 판정
- 앱 시작 시점에서 이전 세션 상태 검증
- 종료 플래그 초기화로 새로운 세션 시작

**상태 관리 요구사항:**
- 정상 종료 시 플래그 설정 및 시간 기록
- 비정상 종료 감지 시 복구 모드 진입
- 복구 완료 후 플래그 초기화

**비정상 종료 경우의 수:**

**예측 가능한 상황:**
- **배터리 부족 상황:**
  - **발생 조건**: 배터리 레벨 15% 이하 감지
  - **감지 방법**: 시스템 배터리 상태 브로드캐스트 수신
  - **사용자 알림**:
    - 20% 이하: "배터리 20%" (주황색 토스트) + 음성 알림
    - 10% 이하: "배터리 10%" (빨간색 토스트) + 음성 알림
    - 5% 이하: "배터리 5%" (빨간색 토스트) + 음성 알림
  - **음성 알림 세부사항** (1회 재생):
    - 20% 이하: "배터리 20%입니다"
    - 10% 이하: "배터리 10%입니다"
    - 5% 이하: "배터리 5%입니다"
    - 재생 조건: 토스트 표시와 동시에 음성 알림 재생
- **메모리 부족 (OOM):**
  - **발생 조건**: 사용 가능 메모리 50MB 이하
  - **감지 방법**: 시스템 메모리 상태 모니터링
  - **사용자 알림**: "메모리 부족" (주황색 토스트)

**예측 불가능한 상황:**
- **예상치 못한 오류:**
  - **네트워크 오류**:
    - **발생 조건**: 서버 통신 실패, 타임아웃
    - **사용자 알림**: "네트워크 오류: 재시도 중입니다" (주황색 토스트)
    - **대응 조치**: 자동 재시도 (최대 3회), 연결 상태 확인
  - **파일 시스템 오류**:
    - **발생 조건**: 설정 파일 읽기/쓰기 실패
    - **사용자 알림**: "설정 오류: 기본값으로 복구합니다" (주황색 토스트)
    - **대응 조치**: 기본 설정으로 복구, 백업 파일 복원 시도
  - **권한 오류**:
    - **발생 조건**: 필요한 권한이 거부됨
    - **사용자 알림**: "권한 오류: 설정에서 권한을 확인하세요" (빨간색 토스트)
    - **대응 조치**: 권한 요청, 대체 방법 제시
- **크래시 발생 시:**
  - **NullPointerException**:
    - **발생 조건**: null 객체 참조 시도
    - **크래시 로그**: 스택 트레이스 수집 및 저장
    - **사용자 알림**: "앱이 예기치 않게 종료되었습니다" (빨간색 토스트)
  - **OutOfMemoryError**:
    - **발생 조건**: 메모리 할당 실패
    - **크래시 로그**: 메모리 사용량 정보 포함
    - **사용자 알림**: "메모리 부족으로 앱이 종료되었습니다" (빨간색 토스트)
  - **NetworkException**:
    - **발생 조건**: 네트워크 통신 중 예외 발생
    - **크래시 로그**: 네트워크 상태 및 연결 정보 포함
    - **사용자 알림**: "네트워크 문제로 앱이 종료되었습니다" (빨간색 토스트)
- **외부 요인:**
  - **하드웨어 오류**: USB 연결 불안정, 센서 오류
  - **시스템 종료**: 기기 재시작, 앱 강제 종료
  - **앱 충돌**: 다른 앱과의 리소스 경합
  - **기기 과열**: CPU/GPU 과부하로 인한 성능 저하

**크래시 복구 플로우:**
- **앱 크래시 직후 재시작 시**:
  - 중립 프레임 2~3회 전송(모든 버튼 해제, dx/dy=0)
  - 8.1 앱 실행 및 초기화 플로우와 동일하게 진행
  - 사용자에게 크래시 알림

#### 2.7.3 재시도 메커니즘 요구사항

**지수 백오프 재시도 전략 요구사항:**
- 재시도 지연 시간: 1초 → 2초 → 4초 → 8초 (지수적 증가)
- 최대 시도 횟수: 4회 (총 15초간 재시도)
- 제네릭 작업 지원: 모든 타입의 비동기 작업 재시도 가능
- 결과 타입: Success/Failure 명확한 구분

**재시도 로직 요구사항:**
- 각 시도별 예외 처리 및 로깅
- 최대 시도 횟수 도달 시 최종 실패 처리
- 지연 시간 정확한 적용
- 재시도 과정의 진행 상황 로깅

**적용 대상:**
- USB 연결 재시도
- 서버 연결 재시도
- 프레임 전송 실패 시 재시도

### 2.8 테스트 및 검증 요구사항

#### 2.8.1 단위 테스트 요구사항

**터치패드 알고리즘 테스트 요구사항:**
- 터치 좌표를 마우스 델타로 변환하는 정확성 검증
- DPI 독립적 정규화 동작 확인
- 좌표 범위 클램핑 (-32767~32767) 검증
- 민감도 설정에 따른 스케일링 정확성 확인
- 15dp 임계값 미만 이동의 누적 처리 검증
- 임계값 초과 시 누적 벡터 보상 적용 확인
- 클릭 판정 로직 (500ms, 15dp) 정확성 검증
- 직각 이동 모드와의 통합 처리 검증

**USB 통신 테스트 요구사항:**
- 8바이트 프레임 구조 정확성 검증
- Little-Endian 바이트 순서 확인
- 순번 카운터 순환 (0~255) 검증
- 버튼 상태 비트 플래그 정확성 확인
- ESP32-S3 USB Serial 장치 자동 인식 정확성 검증
- 권한 요청 및 응답 처리 검증
- Keep-alive 메커니즘 동작 확인
- 연결 끊김 감지 및 복구 검증

#### 2.8.2 통합 테스트 요구사항

**End-to-End 테스트 요구사항:**
- 앱 시작부터 PC 입력 인식까지 전체 플로우 검증
- USB 연결 → 서버 핸드셰이크 → 터치 입력 → PC 반영 검증
- Essential/Standard 모드 전환 동작 확인
- 연결 끊김 및 복구 시나리오 검증

**성능 테스트 요구사항:**
- 디바이스 등급별 전송 빈도 달성 확인 (83Hz 최소 보장, 120Hz 기본 목표, 167Hz 최대 목표)
- 엔드투엔드 지연 50ms 이하 검증
- 메모리 사용량 및 배터리 소모 측정
- 장시간 사용 시 안정성 검증
- 적응적 전송 빈도 전환 테스트

#### 2.8.3 사용자 시나리오 테스트 요구사항

**사용자 경험 테스트 요구사항:**
- 근육장애 사용자를 위한 단일 터치 최적화 검증
- 시각적 피드백 및 햅틱 피드백 정확성 확인
- PC 모니터 집중 시 곁눈질로 상태 파악 가능성 검증
- 직관적 조작 가능성 확인
- 복잡한 학습 없이 즉시 사용 가능성 검증
- 오류 상황에서의 사용자 안내 정확성 확인

---

## 3. 상수 및 임계값 정의

### 3.1 레이아웃 및 UI 상수

| 상수명 | 값 | 단위 | 설명 | 사용 컴포넌트 |
|--------|----|----|------|-------------|
| `TOUCHPAD_ASPECT_RATIO` | 1:2 | 비율 | 터치패드 가로:세로 비율 (고정) | Touchpad |
| `TOUCHPAD_MIN_SIZE` | 160×320 | dp | 터치패드 최소 크기 | Touchpad |
| `CONTROL_BUTTON_SIZE_RATIO` | 8%×16% | 비율 | 터치패드 너비 대비 제어 버튼 크기 | Touchpad Controls |
| `CONTROL_BUTTON_MIN_SIZE` | 24×48 | dp | 제어 버튼 최소 크기 | All Buttons |
| `TOUCH_TARGET_MIN_SIZE` | 48×48 | dp | 모든 터치 요소의 최소 히트 영역 | All Interactive |
| `SCREEN_MARGIN` | 16 | dp | 화면 가장자리 여백 | Layout |
| `COMPONENT_SPACING` | 12 | dp | 컴포넌트 간 간격 | Layout |
| `BUTTON_CORNER_RADIUS` | 8 | dp | 버튼 둥근 모서리 | All Buttons |
| `CONTAINER_CORNER_RADIUS` | 12 | dp | 컨테이너 둥근 모서리 | Containers |
| `TOAST_POSITION_TOP_OFFSET` | 15% | 비율 | 토스트 화면 상단 오프셋 | Status Toast |
| `TOAST_WIDTH_RATIO` | 90% | 비율 | 토스트 너비 비율 | Status Toast |
| `TOAST_MAX_WIDTH` | 400 | dp | 토스트 최대 너비 | Status Toast |
| `TOAST_HEIGHT` | 56 | dp | 토스트 높이 | Status Toast |
| `TOAST_CORNER_RADIUS` | 28 | dp | 토스트 둥근 모서리 | Status Toast |
| `TOAST_ICON_SIZE` | 24 | dp | 토스트 아이콘 크기 | Status Toast |
| `TOAST_TEXT_SIZE` | 16 | sp | 토스트 텍스트 크기 | Status Toast |
| `TOAST_PADDING_VERTICAL` | 16 | dp | 토스트 상하 패딩 | Status Toast |
| `TOAST_PADDING_HORIZONTAL` | 20 | dp | 토스트 좌우 패딩 | Status Toast |
| `TOAST_ICON_MARGIN` | 16 | dp | 토스트 아이콘 좌측 여백 | Status Toast |
| `TOAST_ICON_TEXT_SPACING` | 12 | dp | 아이콘과 텍스트 간격 | Status Toast |
| `PAGE_INDICATOR_HEIGHT` | 8 | dp | 페이지 인디케이터 높이 | Page Indicator |
| `PAGE_INDICATOR_DOT_UNSELECTED_DIAMETER` | 8 | dp | 선택되지 않은 닷 지름 | Page Indicator |
| `PAGE_INDICATOR_DOT_SELECTED_DIAMETER` | 12 | dp | 선택된 닷 지름 | Page Indicator |
| `PAGE_INDICATOR_DOT_SPACING` | 8 | dp | 닷 간 간격 | Page Indicator |
| `TOAST_MAX_WIDTH` | 400 | dp | 토스트 최대 너비 | Status Toast |
| `TOAST_HEIGHT` | 56 | dp | 토스트 높이 | Status Toast |
| `TOAST_CORNER_RADIUS` | 28 | dp | 토스트 둥근 모서리 | Status Toast |
| `TOAST_ICON_SIZE` | 24 | dp | 토스트 아이콘 크기 | Status Toast |
| `TOAST_TEXT_SIZE` | 16 | sp | 토스트 텍스트 크기 | Status Toast |
| `TOAST_PADDING_VERTICAL` | 16 | dp | 토스트 상하 패딩 | Status Toast |
| `TOAST_PADDING_HORIZONTAL` | 20 | dp | 토스트 좌우 패딩 | Status Toast |
| `TOAST_ICON_MARGIN` | 16 | dp | 토스트 아이콘 좌측 여백 | Status Toast |
| `TOAST_ICON_TEXT_SPACING` | 12 | dp | 아이콘과 텍스트 간격 | Status Toast |
| `PAGE_INDICATOR_HEIGHT` | 8 | dp | 페이지 인디케이터 높이 | Page Indicator |
| `PAGE_INDICATOR_DOT_UNSELECTED_DIAMETER` | 8 | dp | 선택되지 않은 닷 지름 | Page Indicator |
| `PAGE_INDICATOR_DOT_SELECTED_DIAMETER` | 12 | dp | 선택된 닷 지름 | Page Indicator |
| `PAGE_INDICATOR_DOT_SPACING` | 8 | dp | 닷 간 간격 | Page Indicator |

### 3.2 색상 상수

| 상수명 | 값 | 설명 | 사용 위치 |
|--------|----|----|--------|
| `COLOR_BACKGROUND` | #121212 | 앱 전체 배경 | Global |
| `COLOR_TEXT_PRIMARY` | #FFFFFF | 기본 텍스트 | Global |
| `COLOR_PRIMARY` | #2196F3 | 주요 액션 버튼, Selected 상태 | Buttons, Selected |
| `COLOR_DISABLED` | #C2C2C2 | Disabled/Unselected 상태 | Disabled States |
| `COLOR_SUCCESS` | #4CAF50 | 성공 상태 | Success Toast |
| `COLOR_ERROR` | #F44336 | 오류 상태 | Error Toast |
| `COLOR_WARNING` | #FF9800 | 경고/진행 상태 | Warning Toast |
| `COLOR_YELLOW_RIGHT_CLICK` | #F3D021 | 우클릭 모드 | Touchpad |
| `COLOR_GREEN_NORMAL_SCROLL` | #84E268 | 일반 스크롤 모드 | Touchpad |
| `COLOR_RED_INFINITE_SCROLL` | #F32121 | 무한 스크롤 모드 | Touchpad |
| `COLOR_PURPLE_MULTI_CURSOR` | #B552F6 | 멀티 커서 모드 | Touchpad |
| `COLOR_ORANGE_PERPENDICULAR` | #FF8A00 | 직각 이동 모드 | Touchpad |

### 3.3 입력 인식 임계값

| 상수명 | 값 | 단위 | 설명 | 사용 위치 |
|--------|----|----|------|--------|
| `DEAD_ZONE_THRESHOLD` | 15 | dp | 터치 입력 데드존 임계값 | DeadZone Compensation |
| `CLICK_MAX_DURATION` | 500 | ms | 클릭 인식 최대 시간 | Touch Processing |
| `DOUBLE_TAP_MAX_INTERVAL` | 300 | ms | 더블탭 인식 최대 간격 | Touch Processing |
| `LONG_PRESS_THRESHOLD` | 500 | ms | 길게 누르기 인식 임계값 | Button Components |
| `REINPUT_DEBOUNCE_MS` | 150 | ms | 재입력 방지 디바운스 시간 | All Interactive |
| `AXIS_DETERMINATION_MIN_DISTANCE` | 30 | dp | 축 결정을 위한 최소 이동 거리 | Perpendicular Movement |
| `AXIS_CLASSIFICATION_ANGLE_RANGE` | ±22.5 | 도 | 축 분류 각도 허용 범위 | Perpendicular Movement |
| `HAPTIC_FEEDBACK_LIGHT_DURATION` | 15 | ms | 가벼운 햅틱 피드백 지속시간 | Light Feedback |
| `HAPTIC_FEEDBACK_MEDIUM_DURATION` | 30 | ms | 중간 햅틱 피드백 지속시간 | Medium Feedback |
| `HAPTIC_FEEDBACK_STRONG_DURATION` | 50 | ms | 강한 햅틱 피드백 지속시간 | Strong Feedback |
| `HAPTIC_FEEDBACK_TICK_DURATION` | 10 | ms | 진행 햅틱 피드백 틱 지속시간 | Progressive Feedback |
| `HAPTIC_FEEDBACK_DEBOUNCE_MS` | 50 | ms | 햅틱 피드백 디바운스 시간 | Haptic System |
| `HAPTIC_FEEDBACK_PROGRESSIVE_MAX_FREQUENCY` | 10 | Hz | 진행 햅틱 피드백 최대 주파수 | Progressive Feedback |

### 3.4 터치패드 동작 상수

| 상수명 | 값 | 단위 | 설명 | 사용 위치 |
|--------|----|----|------|--------|
| `SENSITIVITY_MULTIPLIER_LOW` | 0.5 | 배수 | DPI 낮음 민감도 배율 | Movement Processing |
| `SENSITIVITY_MULTIPLIER_NORMAL` | 1.0 | 배수 | DPI 보통 민감도 배율 | Movement Processing |
| `SENSITIVITY_MULTIPLIER_HIGH` | 1.5 | 배수 | DPI 높음 민감도 배율 | Movement Processing |
| `COORDINATE_RANGE_MIN` | -32767 | 정수 | i16 최소 범위 (프로토콜 호환) | Protocol Frame |
| `COORDINATE_RANGE_MAX` | 32767 | 정수 | i16 최대 범위 (프로토콜 호환) | Protocol Frame |
| `SCROLL_UNIT_DISTANCE` | 50 | dp | 일반 스크롤 단위 거리 | Scroll Processing |
| `SCROLL_INERTIA_DECAY_RATE` | 0.95 | 계수 | 무한 스크롤 관성 감속 비율 | Infinite Scroll |
| `GUIDELINE_LINE_SPACING` | 40 | dp | 스크롤 가이드라인 라인 간격 | Scroll Guideline |
| `GUIDELINE_SYNC_MULTIPLIER` | 2.0 | 배수 | 가이드라인 속도 동기화 배율 | Scroll Guideline |

### 3.4.1 버튼 컴포넌트 상수

| 상수명 | 값 | 단위 | 설명 | 사용 위치 |
|--------|----|----|------|--------|
| `BUTTON_HEIGHT` | 48 | dp | 버튼 표준 높이 | All Buttons |
| `BUTTON_CORNER_RADIUS` | 8 | dp | 버튼 둥근 모서리 | All Buttons |
| `BUTTON_MIN_TOUCH_TARGET` | 48×48 | dp | 버튼 최소 터치 영역 | All Interactive |
| `BUTTON_SCALE_ANIMATION_MIN` | 0.98 | 비율 | 버튼 터치 시 최소 스케일 | Button Press |
| `BUTTON_SCALE_ANIMATION_MAX` | 1.0 | 비율 | 버튼 터치 시 최대 스케일 | Button Press |
| `BUTTON_SCALE_ANIMATION_DURATION` | 200 | ms | 버튼 스케일 애니메이션 지속시간 | Button Press |
| `KEYBOARD_BUTTON_STICKY_HOLD_THRESHOLD` | 500 | ms | 키보드 버튼 Sticky Hold 임계시간 | KeyboardKeyButton |
| `SHORTCUT_BUTTON_DEBOUNCE_MS` | 150 | ms | 단축키 버튼 디바운스 시간 | ShortcutButton |
| `MACRO_BUTTON_TIMEOUT_MS` | 30000 | ms | 매크로 버튼 실행 타임아웃 | MacroButton |
| `CONTAINER_BUTTON_LONG_PRESS_THRESHOLD` | 500 | ms | 컨테이너 버튼 롱프레스 임계시간 | ContainerButton |
| `DPAD_MIN_SIZE` | 220 | dp | DPad 최소 크기 | DPad Component |
| `DPAD_CENTER_RADIUS_RATIO` | 0.3 | 비율 | DPad 중심 영역 반지름 비율 | DPad Detection |
| `DPAD_DIRECTION_DEBOUNCE_MS` | 50 | ms | DPad 방향 전환 디바운스 시간 | DPad Processing |

### 3.5 애니메이션 및 타이밍 상수

| 상수명 | 값 | 단위 | 설명 | 사용 위치 |
|--------|----|----|------|--------|
| `ANIMATION_DURATION_SHORT` | 150 | ms | 짧은 애니메이션 지속시간 | UI Transitions |
| `ANIMATION_DURATION_NORMAL` | 200 | ms | 일반 애니메이션 지속시간 | UI Transitions |
| `ANIMATION_DURATION_LONG` | 300 | ms | 긴 애니메이션 지속시간 | Page Transitions |
| `SCALE_ANIMATION_MIN` | 0.95 | 비율 | 버튼 터치 시 최소 스케일 | Button Press |
| `SCALE_ANIMATION_MAX` | 1.0 | 비율 | 버튼 터치 시 최대 스케일 | Button Press |
| `TOAST_DURATION_SHORT` | 2000 | ms | 짧은 토스트 표시 시간 | Toast |
| `TOAST_DURATION_NORMAL` | 3000 | ms | 일반 토스트 표시 시간 | Toast |
| `TOAST_DURATION_LONG` | 5000 | ms | 긴 토스트 표시 시간 | Toast |
| `TOAST_ANIMATION_APPEAR_DURATION` | 350 | ms | 토스트 등장 애니메이션 지속시간 | Toast Animation |
| `TOAST_ANIMATION_DISAPPEAR_DURATION` | 300 | ms | 토스트 사라짐 애니메이션 지속시간 | Toast Animation |
| `TOAST_STATE_TRANSITION_DURATION` | 250 | ms | 토스트 상태 전환 지속시간 | Toast Animation |
| `PAGE_INDICATOR_TRANSITION_DURATION` | 200 | ms | 페이지 인디케이터 전환 지속시간 | Page Indicator |
| `TOAST_APPEAR_TRANSLATION_Y` | -100 | dp | 토스트 등장 시 Y축 이동 거리 | Toast Animation |
| `TOAST_DISAPPEAR_TRANSLATION_Y` | -100 | dp | 토스트 사라짐 시 Y축 이동 거리 | Toast Animation |
| `TOAST_APPEAR_SCALE_FROM` | 0.8 | 비율 | 토스트 등장 시 초기 스케일 | Toast Animation |
| `TOAST_APPEAR_SCALE_TO` | 1.0 | 비율 | 토스트 등장 시 최종 스케일 | Toast Animation |

### 3.6 통신 및 프로토콜 상수

| 상수명 | 값 | 단위 | 설명 | 사용 위치 |
|--------|----|----|------|--------|
| `USB_BAUD_RATE` | 1000000 | bps | USB 시리얼 통신 속도 | USB Communication |
| `USB_DATA_BITS` | 8 | bit | 데이터 비트 수 | USB Communication |
| `USB_STOP_BITS` | 1 | bit | 스톱 비트 수 | USB Communication |
| `USB_PARITY` | NONE | - | 패리티 설정 | USB Communication |
| `TRANSMISSION_TARGET_FREQUENCY` | 120 | Hz | 기본 목표 전송 주기 | Frame Transmission |
| `TRANSMISSION_FRAME_INTERVAL` | 6 | ms | 프레임 간격 | Frame Transmission |
| `PROTOCOL_FRAME_SIZE` | 8 | byte | BridgeOne 프로토콜 프레임 크기 | Protocol |
| `USB_WRITE_TIMEOUT` | 100 | ms | USB 전송 타임아웃 | USB Communication |
| `KEEPALIVE_INTERVAL` | 500 | ms | Keep-alive 프레임 전송 간격 | Connection Monitoring |
| `SEQUENCE_COUNTER_MAX` | 255 | 정수 | 순번 카운터 최대값 | Protocol |
| `RECONNECTION_BACKOFF_DELAYS` | [1000, 2000, 4000, 8000] | ms | 재연결 지수 백오프 지연시간 | Connection Recovery |
| `CONNECTION_TIMEOUT_THRESHOLD` | 3000 | ms | 연결 타임아웃 임계값 | Connection Monitoring |
| `MAX_MISSED_RESPONSES` | 3 | 회 | 응답 누락 최대 허용 횟수 | Connection Monitoring |

### 3.7 DPad 및 방향 입력 상수

| 상수명 | 값 | 단위 | 설명 | 사용 위치 |
|--------|----|----|------|--------|
| `DPAD_MIN_SIZE` | 220 | dp | DPad 최소 크기 | DPad Component |
| `DPAD_CENTER_RADIUS_RATIO` | 0.3 | 비율 | 중심 영역 반지름 비율 | DPad Detection |
| `DPAD_SECTOR_ANGLE` | 45 | 도 | 각 섹터 각도 (8분할) | DPad Detection |
| `DPAD_ANGLE_TOLERANCE` | 10 | 도 | 섹터 경계 허용오차 | DPad Detection |
| `DIRECTION_DEBOUNCE_MS` | 50 | ms | 방향 전환 디바운스 시간 | DPad Processing |
| `STICKY_HOLD_DOUBLE_TAP_MS` | 300 | ms | Sticky Hold 더블탭 타이밍 | DPad Processing |
| `STICKY_HOLD_ANIMATION_DURATION` | 200 | ms | Sticky Hold 진입 애니메이션 | DPad Animation |
| `STICKY_HOLD_RELEASE_ANIMATION_DURATION` | 150 | ms | Sticky Hold 해제 애니메이션 | DPad Animation |

### 3.8 매크로 상수

| 상수명 | 값 | 단위 | 설명 | 사용 위치 |
|--------|----|----|------|--------|
| `MACRO_TIMEOUT_MS` | 30000 | ms | 매크로 실행 타임아웃 | Macro Execution |

### 3.9 성능 및 품질 상수

| 상수명 | 값 | 단위 | 설명 | 사용 위치 |
|--------|----|----|------|--------|
| `FRAME_POOL_SIZE` | 10 | 개 | 8바이트 프레임 객체 풀 크기 | Memory Management |
| `POINTF_POOL_SIZE` | 50 | 개 | PointF 좌표 객체 풀 크기 | Memory Management |
| `BATCH_SIZE` | 4 | 개 | 배치 전송 프레임 수 | Communication |
| `BATCH_TIMEOUT_MS` | 8 | ms | 배치 전송 타임아웃 | Communication |
| `UI_THREAD_POOL_SIZE` | 1 | 개 | UI 업데이트 스레드 수 | Thread Management |
| `COMMUNICATION_THREAD_POOL_SIZE` | 2 | 개 | 통신 처리 스레드 수 | Thread Management |
| `COMPUTATION_THREAD_POOL_SIZE` | (코어 수 - 2) | 개 | 계산 작업 스레드 수 | Thread Management |
| `BATTERY_HIGH_THRESHOLD` | 50 | % | 고성능 모드 배터리 임계값 | Battery Optimization |
| `BATTERY_MEDIUM_THRESHOLD` | 30 | % | 기본 모드 배터리 임계값 | Battery Optimization |
| `BATTERY_LOW_FREQUENCY` | 83 | Hz | 저전력 모드 전송 빈도 | Battery Optimization |
| `BATTERY_MEDIUM_FREQUENCY` | 125 | Hz | 절전 모드 전송 빈도 | Battery Optimization |
| `BATTERY_HIGH_FREQUENCY` | 167 | Hz | 최대 성능 모드 전송 빈도 (고급 디바이스) | Battery Optimization |
`BATTERY_LOW_THRESHOLD` | 20 | % | 저전력 모드 배터리 임계값 | Battery Optimization |
`BATTERY_CRITICAL_THRESHOLD` | 10 | % | 배터리 절약 모드 배터리 임계값 | Battery Optimization |
`BATTERY_BATTERY_SAVER_FREQUENCY` | 60 | Hz | 배터리 절약 모드 전송 빈도 | Battery Optimization |
`DEVICE_TOUCH_SCAN_HIGH` | 120 | Hz | 고급 디바이스 터치 스캔 주파수 | Device Performance |
`DEVICE_TOUCH_SCAN_MEDIUM` | 60 | Hz | 중급 디바이스 터치 스캔 주파수 | Device Performance |

### 3.10 오류 처리 상수

| 상수명 | 값 | 단위 | 설명 | 사용 위치 |
|--------|----|----|------|--------|
| `MAX_RETRY_ATTEMPTS` | 4 | 회 | 최대 재시도 횟수 | Error Recovery |
| `RETRY_BASE_DELAY_MS` | 1000 | ms | 재시도 기본 지연시간 | Error Recovery |
| `RETRY_MULTIPLIER` | 2.0 | 배수 | 재시도 지연시간 배수 | Error Recovery |
| `NORMAL_SHUTDOWN_TIMEOUT_MS` | 30000 | ms | 정상 종료 타임아웃 | Crash Recovery |
| `CRASH_RECOVERY_DELAY_MS` | 2000 | ms | 크래시 복구 지연시간 | Crash Recovery |
| `LOG_RETENTION_DAYS` | 7 | 일 | 로그 보존 기간 | Logging |

### 3.11 테스트 및 검증 상수

| 상수명 | 값 | 단위 | 설명 | 사용 위치 |
|--------|----|----|------|--------|
| `TEST_COORDINATE_RANGE_MIN` | -32767 | 정수 | 테스트용 좌표 최소값 | Unit Testing |
| `TEST_COORDINATE_RANGE_MAX` | 32767 | 정수 | 테스트용 좌표 최대값 | Unit Testing |
| `TEST_DEAD_ZONE_THRESHOLD` | 15 | dp | 테스트용 데드존 임계값 | Unit Testing |
| `TEST_CLICK_DURATION_MAX` | 500 | ms | 테스트용 클릭 최대 지속시간 | Unit Testing |
| `TEST_AXIS_DETERMINATION_DISTANCE` | 30 | dp | 테스트용 축 결정 거리 | Unit Testing |
| `TEST_ANGLE_TOLERANCE` | 22.5 | 도 | 테스트용 각도 허용오차 | Unit Testing |
| `PERFORMANCE_TARGET_FREQUENCY` | 120 | Hz | 기본 성능 테스트 목표 주파수 | Performance Testing |
| `PERFORMANCE_TARGET_LATENCY_MS` | 50 | ms | 성능 테스트 목표 지연시간 | Performance Testing |
| `STRESS_TEST_DURATION_HOURS` | 24 | 시간 | 스트레스 테스트 지속시간 | Stress Testing |
| `MEMORY_LEAK_THRESHOLD_MB` | 100 | MB | 메모리 누수 임계값 | Memory Testing |

---

이 기술 명세서는 BridgeOne Android 앱의 모든 기술적 설계 요구사항과 명세를 정의합니다. 실제 개발 시에는 각 섹션의 요구사항과 상수값을 참조하여 일관된 구현을 보장하시기 바랍니다.
