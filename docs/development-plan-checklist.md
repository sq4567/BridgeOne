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

**구현 체크리스트**:
- [ ] **USB Serial 라이브러리 통합**
  - [ ] usb-serial-for-android 3.9.0 Gradle 의존성 추가
  - [ ] CP2102 VID/PID (0x10C4:0xEA60) 자동 인식 구현
  - [ ] USB OTG 권한 요청 및 관리 시스템
  - [ ] 1Mbps UART 통신 설정 (8N1)
  
- [ ] **BridgeOne 프로토콜 기본 구현**
  - [ ] 8바이트 기본 프레임 구조 (seq, buttons, deltaX/Y, wheel, modifiers, keyCode1/2)
  - [ ] Little-Endian 바이트 순서 처리
  - [ ] 순번 카운터 (0-255 순환) 구현
  - [ ] 기본 터치 입력 → 프레임 변환

##### 1.1.2 Board 기본 UART 수신 구현

**참조 문서**:
- `@docs/Board/esp32s3-code-implementation-guide.md` §2 (통신 프로토콜)

**구현 체크리스트**:
- [ ] **기본 UART 통신 모듈**
  - [ ] UART2 포트, 1Mbps 8N1 설정
  - [ ] 8바이트 프레임 수신 및 파싱
  - [ ] 순번 카운터 검증 및 중복 제거
  - [ ] 시리얼 모니터 디버그 출력

- [ ] **FreeRTOS 기본 태스크**
  - [ ] UART 수신 태스크 (최고 우선순위)
  - [ ] 디버그 출력 태스크 (낮은 우선순위)
  - [ ] 프레임 파싱 검증 로직

##### 1.1.3 Android ↔ Board 통신 테스트

**테스트 체크리스트**:
- [ ] **기본 연결 테스트**
  - [ ] Android 앱에서 ESP32-S3 장치 자동 인식 확인
  - [ ] USB 연결 성공 시 상태 표시
  - [ ] 연결 실패 시 오류 메시지 표시

- [ ] **프레임 송수신 테스트**
  - [ ] Android에서 터치 이벤트 → 8바이트 프레임 전송
  - [ ] ESP32-S3에서 프레임 수신 → 시리얼 모니터 출력 확인
  - [ ] 순번 카운터 순환 (0→255→0) 동작 확인
  - [ ] 1분간 연속 전송 안정성 테스트

#### 1.2 Board ↔ Windows 기본 통신 구축 및 테스트

**단계 목표**: ESP32-S3와 Windows 서버 간 HID/CDC 통신 완성 및 검증
**예상 기간**: 5-6일
**검증 기준**: HID 마우스/키보드 인식, Vendor CDC 명령 송수신 성공

##### 1.2.1 Board USB HID 출력 구현

**구현 체크리스트 (Arduino HID 기반)**:
- [ ] **Arduino HID 라이브러리 설정**
  - [ ] USBHIDMouse, USBHIDKeyboard 초기화
  - [ ] USB 복합 장치 자동 구성 (Windows 호환성 자동 보장)
  - [ ] PlatformIO Arduino USB 설정 최적화
  - [ ] 50ms USB 안정화 지연 적용

- [ ] **BridgeOne → Arduino HID 변환 엔진**
  - [ ] `processBridgeFrameToHID()` 함수 구현
  - [ ] 마우스 처리 (`HIDMouse.move()`, `HIDMouse.click()`)
  - [ ] 키보드 처리 (`HIDKeyboard.write()`, 모디파이어 조합)
  - [ ] Arduino 패턴 기반 디바운싱 (40ms) 적용

- [ ] **홀드 입력 및 반복 처리 (Arduino 예제 패턴)**
  - [ ] `processHoldInput()` 함수 구현
  - [ ] 초기 반복 지연 (300ms) 및 반복 간격 (30ms)
  - [ ] 홀드 상태 관리 및 해제 로직
  - [ ] 연속 입력 성능 최적화

- [ ] **FreeRTOS Arduino HID 태스크**
  - [ ] HID 전송 태스크 (우선순위 3) 구현
  - [ ] UART 큐 연동 및 실시간 변환
  - [ ] 성능 지표 모니터링 (1000 FPS 목표)
  - [ ] USB 연결 상태 자동 감지

##### 1.2.2 Board Vendor CDC 구현

**구현 체크리스트**:
- [ ] **Vendor CDC 인터페이스**
  - [ ] CDC-ACM 커스텀 인터페이스 구성
  - [ ] JSON 메시지 송수신 구조
  - [ ] 0xFF 헤더 + JSON 페이로드 프레임
  - [ ] CRC16 체크섬 검증

- [ ] **양방향 명령 처리**
  - [ ] Windows → Board 명령 수신 처리
  - [ ] Board → Windows 상태 전송
  - [ ] 메시지 ID 기반 요청-응답 매칭
  - [ ] CDC 처리 태스크 구현

##### 1.2.3 Windows 기본 장치 인식 구현

**참조 문서**:
- `@docs/Windows/technical-specification-server.md` §3 (핵심 기능)

**구현 체크리스트**:
- [ ] **.NET 8.0+ 기본 프로젝트 구조**
  - [ ] 콘솔 애플리케이션 기본 구조 (WPF는 나중에 추가)
  - [ ] ESP32-S3 HID 장치 열거 및 감지
  - [ ] Vendor CDC 장치 통신 초기화
  - [ ] 기본 로깅 시스템

- [ ] **HID 입력 처리**
  - [ ] HID 마우스 입력 수신 → Windows 커서 이동
  - [ ] HID 키보드 입력 수신 → Windows 키 입력
  - [ ] 입력 지연시간 측정 시스템
  - [ ] Raw Input API 활용

##### 1.2.4 Board ↔ Windows 통신 테스트

**테스트 체크리스트**:
- [ ] **HID 장치 인식 테스트**
  - [ ] Windows에서 ESP32-S3 HID 마우스 자동 인식
  - [ ] Windows에서 ESP32-S3 HID 키보드 자동 인식
  - [ ] Vendor CDC 장치 열거 확인
  - [ ] 장치 관리자에서 올바른 드라이버 로딩 확인

- [ ] **HID 입력 처리 테스트**
  - [ ] ESP32-S3에서 마우스 리포트 전송 → Windows 커서 이동 확인
  - [ ] ESP32-S3에서 키보드 리포트 전송 → Windows 키 입력 확인
  - [ ] Vendor CDC 명령 송수신 테스트
  - [ ] 1분간 연속 HID 전송 안정성 테스트

#### 1.3 End-to-End 통신 연결 및 테스트

**단계 목표**: Android → Board → Windows 전체 경로 통신 완성
**예상 기간**: 4-5일
**검증 기준**: 터치 입력이 Windows 커서 이동으로 즉시 반영

##### 1.3.1 프레임 릴레이 시스템 구현

**구현 체크리스트**:
- [ ] **Board 프레임 릴레이 로직**
  - [ ] UART 수신 프레임 → HID 리포트 변환 → 즉시 전송
  - [ ] 프레임 큐 관리 (32개 프레임 버퍼)
  - [ ] 지연시간 최소화 (<5ms 목표)
  - [ ] 프레임 손실 방지 메커니즘

- [ ] **Android 실시간 터치 처리**
  - [ ] 터치 이벤트 감지 → 즉시 프레임 생성 → UART 전송
  - [ ] 객체 풀링 (PointF, Frame 객체 재사용)
  - [ ] 배치 처리보다는 즉시 전송 우선
  - [ ] 터치 지연시간 측정 (5-8ms 목표)

##### 1.3.2 End-to-End 성능 측정 시스템

**구현 체크리스트**:
- [ ] **지연시간 측정 구현**
  - [ ] Android: 터치 이벤트 타임스탬프 → 프레임 전송
  - [ ] Board: UART 수신 → HID 전송 지연시간
  - [ ] Windows: HID 수신 → 커서 적용 지연시간
  - [ ] 전체 E2E 지연시간 계산

- [ ] **성능 모니터링 시스템**
  - [ ] 실시간 FPS 측정 (목표: 120Hz 기본, 167Hz 최대)
  - [ ] 패킷 손실률 측정 (목표: <0.1%)
  - [ ] 지연시간 분포 히스토그램
  - [ ] 성능 경고 시스템

##### 1.3.3 End-to-End 통신 검증

**검증 체크리스트**:
- [ ] **기본 동작 테스트**
  - [ ] Android 터치 → Windows 커서 이동 즉시 확인
  - [ ] 좌표 정확성 (화면 비율 매칭)
  - [ ] 터치 해제 → 마우스 이동 중단
  - [ ] 키보드 입력 (가상 키보드) → Windows 텍스트 입력
  
- [ ] **성능 목표 달성 검증**
  - [ ] 전체 E2E 지연시간 50ms 이하 달성
  - [ ] Android: 5-8ms, Board: ≤5ms, Windows: 10-15ms
  - [ ] 디바이스 등급별 전송 빈도 안정적 달성 (83-167Hz 범위)
  - [ ] 1분간 연속 조작 안정성 확인

#### 1.4 고급 통신 기능 및 오류 처리 구현

**단계 목표**: Keep-alive, 모드 전환, 오류 복구 등 고급 기능 완성
**예상 기간**: 5-6일
**검증 기준**: 24시간 안정성, Essential/Standard 모드 전환

##### 1.4.1 Keep-alive 메커니즘 구현

**구현 체크리스트**:
- [ ] **Android Keep-alive 시스템**
  - [ ] 500ms 주기 Keep-alive 프레임 전송
  - [ ] 3초 응답 타임아웃 처리
  - [ ] 연속 3회 누락 시 재연결 로직
  - [ ] 지수 백오프 재연결 (1→2→4→8초)

- [ ] **Windows Keep-alive 응답**
  - [ ] Vendor CDC 통한 Keep-alive 응답
  - [ ] 0.5초 주기 Ping-Pong 방식
  - [ ] 연결 품질 지표 실시간 추적
  - [ ] 자동 복구 메커니즘

##### 1.4.2 Essential/Standard 모드 전환

**구현 체크리스트**:
- [ ] **상태 관리 시스템**
  - [ ] TransportState (NoTransport → UsbOpening → UsbReady)
  - [ ] AppState (WaitingForConnection → Essential → Standard)
  - [ ] Windows 서버 핸드셰이크 신호 처리
  - [ ] 모드별 기능 제한 정책

- [ ] **핸드셰이크 프로토콜**
  - [ ] Phase 1: Authentication (Challenge-Response)
  - [ ] Phase 2: State Sync (상태 동기화)
  - [ ] TLS 1.3 기반 보안 연결 (기본 구현)
  - [ ] 핸드셰이크 완료 → Standard 모드 전환

##### 1.4.3 오류 처리 및 복구 구현

**구현 체크리스트**:
- [ ] **USB 연결 오류 처리**
  - [ ] CP2102 장치 분리 감지 및 즉시 대응
  - [ ] 권한 거부 시 적절한 안내 메시지
  - [ ] USB 드라이버 문제 진단
  - [ ] Cancel and Restart 패턴 적용
  
- [ ] **통신 프로토콜 오류 처리**
  - [ ] 프레임 손실 감지 및 재전송 요청
  - [ ] JSON 파싱 오류 시 안전한 처리
  - [ ] 타임아웃 발생 시 자동 복구
  - [ ] 프로토콜 버전 불일치 대응

##### 1.4.4 실용적 안정성 테스트

**4시간 수동 테스트 체크리스트**:
- [ ] **1단계: 기본 안정성 확인 (2시간)**
  - [ ] 연속 1시간 동작 테스트 (Android ↔ Board ↔ Windows 통신 유지)
  - [ ] Keep-alive 패킷 정상 동작 (로그 확인)
  - [ ] 메모리 사용량 안정성 (작업관리자 모니터링)
  - [ ] 재연결 테스트: USB 분리/재연결 10회, WiFi 끊김/재연결 5회
  - [ ] 앱 백그라운드/포그라운드 전환 테스트

- [ ] **2단계: 프로토콜 무결성 확인 (1시간)**
  - [ ] 빠른 연속 터치 패턴 테스트 (1분간)
  - [ ] 긴 드래그 동작 (여러 방향)
  - [ ] 멀티터치 동작 확인
  - [ ] 순번 카운터 순환 정상 동작 확인
  - [ ] CRC16 체크섬 무결성 유지 확인

- [ ] **3단계: 실제 사용 시나리오 (1시간)**
  - [ ] 30분간 실제 PC 제어 (다양한 앱 사용)
  - [ ] Essential/Standard 모드 전환 동작 확인
  - [ ] 다양한 터치패드 제스처 테스트
  - [ ] 오류 복구 메커니즘 검증

**Phase 1 완료 조건**:
- Android ↔ Board ↔ Windows 전체 통신 경로 완성
- Essential/Standard 모드 자동 전환 동작  
- 전체 입력 지연시간 50ms 이하 달성  
- 4시간 연속 테스트 통과 (재연결 포함)
- Keep-alive 패킷 정상 동작 확인
- 모든 오류 상황에서 자동 복구 성공

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

**구현 체크리스트**:
- [ ] **터치패드 컴포넌트 구조**
  - [ ] TouchpadWrapper (1:2 비율, 반응형 크기 조정)
  - [ ] TouchpadAreaWrapper (터치 감지 영역, 둥근 모서리 3%)
  - [ ] ControlButtonContainer (상단 오버레이, 높이 15%)
  - [ ] 터치 추적 연속성 (영역 밖 이동도 추적 지속)

- [ ] **제스처 알고리즘 구현**
  - [ ] 데드존 보상 알고리즘 (15dp 임계값, 상태 머신 IDLE→ACCUMULATING→MOVING)
  - [ ] 직각 이동 알고리즘 (30dp 축 결정, ±22.5° 각도 허용)
  - [ ] 자유 이동 모드 (DPI 독립적 정규화, 좌표 변환)
  - [ ] 클릭 판정 로직 (500ms, 15dp 임계값)

- [ ] **스크롤 시스템 구현**
  - [ ] 일반 스크롤 (50dp 단위, 햅틱 동기화)
  - [ ] 무한 스크롤 (관성 알고리즘, 지수 감속 0.95^배율)
  - [ ] 스크롤 가이드라인 (40dp 간격, 2.0x 속도 동기화)
  - [ ] 더블탭 제스처로 스크롤 모드 종료

- [ ] **터치패드 모드 시스템**
  - [ ] ClickMode (LEFT/RIGHT), MoveMode (FREE/RIGHT_ANGLE)
  - [ ] CursorMode (SINGLE/MULTI), ScrollMode (NORMAL/INFINITE)
  - [ ] 제어 버튼 가시성 관리 (모드별 숨김/표시)
  - [ ] 테두리 색상 우선순위 시스템 구현

#### 2.2 UI 컴포넌트 시스템 구현

**참조 문서**:
- `@docs/Android/component-design-guide-app.md` §2-4 (버튼, DPad 컴포넌트)
- `@docs/Android/design-guide-app.md` §5 (공통 컴포넌트)
- `@docs/Android/technical-specification-app.md` §2.3 (컴포넌트 설계)

**구현 체크리스트**:
- [ ] **버튼 컴포넌트 시스템**
  - [ ] KeyboardKeyButton (키 입력, Sticky Hold 500ms)
  - [ ] ShortcutButton (키 조합, 150ms 디바운스)
  - [ ] ContainerButton (팝업 오버레이, 롱프레스 지속 모드)

- [ ] **DPad 컴포넌트 구현**
  - [ ] 8분할 섹터 판정 (45° 단위, 10° 허용오차)
  - [ ] 대각선 입력 (2키 동시 Down/Up)
  - [ ] Sticky Hold 기능 (300ms 더블탭)
  - [ ] 방향 전환 디바운스 (50ms)

- [ ] **공통 컴포넌트 시스템**
  - [ ] 상태 알림 토스트 (5가지 카테고리, 애니메이션)
  - [ ] 페이지 인디케이터 (Selected/Unselected 상태)
  - [ ] 햅틱 피드백 (Light/Medium/Strong/Error 카테고리)
  - [ ] 컴포넌트 비활성화 시스템

#### 2.3 페이지 네비게이션 및 상태 관리

**참조 문서**:
- `@docs/Android/styleframe-page1.md`, `@styleframe-page2.md`, `@styleframe-page3.md`
- `@docs/Android/styleframe-essential.md` (Essential 모드 페이지)
- `@docs/Android/design-guide-app.md` §8.2 (페이지 네비게이션)

**구현 체크리스트**:
- [ ] **4개 페이지 구현**
  - [ ] Essential 페이지 (Boot Keyboard, 간소화 터치패드)
  - [ ] Page 1: 터치패드 + Actions (Special Keys, Shortcuts)
  - [ ] Page 2: 키보드 중심 (Modifiers, Navigation, Function Keys)
  - [ ] Page 3: Minecraft 특화 (DPad, Combat, Hotbar)

- [ ] **페이지 전환 시스템**
  - [ ] 좌우 슬라이드 제스처 (화면 폭 20% 임계값)
  - [ ] 스프링 애니메이션 (400ms, 햅틱 Strong)
  - [ ] 페이지 인디케이터 동기화 (200ms 애니메이션)
  - [ ] 상태 저장/복원 (SharedPreferences)

- [ ] **상태 관리 시스템**
  - [ ] AppState 머신 (WaitingForConnection/Essential/Standard)
  - [ ] 컴포넌트별 Enabled/Disabled 상태 관리
  - [ ] 페이지별 상태 지속성 (모드, 설정값 유지)
  - [ ] 강제 해제 메커니즘 (페이지 일괄 비활성화)

#### 2.4 성능 최적화 및 메모리 관리

**성능 목표 달성**:
- [ ] **렌더링 최적화**
  - [ ] Jetpack Compose 60fps 안정 유지
  - [ ] GPU 가속 활용 및 하드웨어 레이어 적용
  - [ ] State 변경 최소화로 recomposition 방지
  - [ ] Canvas 기반 고성능 그리기 활용

- [ ] **메모리 관리 최적화**
  - [ ] 객체 풀링 (PointF: 50개, Frame: 10개)
  - [ ] 가비지 컬렉션 최소화 (객체 재사용)
  - [ ] 배치 처리 (4프레임 단위, 8ms 타임아웃)
  - [ ] 스레드 풀 관리 (UI/통신/계산 분리)

**Phase 2 완료 조건**:
- 모든 터치패드 모드 및 제스처 완벽 동작  
- 60fps 애니메이션 안정 유지  
- 4개 페이지 네비게이션 완전 구현  
- 메모리 사용량 최적화 달성

---

### Phase 3: Windows 서버 고급 기능 구현

**목표**: 멀티 커서 등 Windows 서버 기반 고급 기능 완전 구현
**예상 기간**: 4-5주
**완료 기준**: 멀티 커서 텔레포트, 커서 팩 연동 완료

#### 3.1 Windows 서버 기본 구조 구현

**참조 문서**:
- `@docs/Windows/technical-specification-server.md` §2 (기술 스택), §10 (GUI 구현)
- `@docs/Windows/design-guide-server.md` §7.1 (서버 실행), §10.2 (WPF UI)

**구현 체크리스트**:
- [ ] **.NET 8.0+ WPF 프로젝트 구조**
  - [ ] WPF UI 라이브러리 (lepoco/wpfui) 통합
  - [ ] Fluent Design System 및 Mica 배경 효과
  - [ ] MVVM 패턴 및 의존성 주입 구조
  - [ ] NavigationView 기반 메인 윈도우

- [ ] **Windows 서비스 통합**
  - [ ] 백그라운드 서비스 등록 및 자동 시작
  - [ ] 시스템 트레이 아이콘 및 컨텍스트 메뉴
  - [ ] 애플리케이션 로딩 스플래시 (2.5초, 6단계)
  - [ ] 설정 저장소 및 백업/복원 시스템

#### 3.2 커서 팩 연동 시스템 구현

**참조 문서**:
- `@docs/Windows/technical-specification-server.md` §3.6 (멀티 커서), §8.4 (멀티 커서 상수)
- `@docs/Windows/design-guide-server.md` §6.2 (멀티 커서 기능)
- `@docs/PRD.md` §5.3.8 (커서 팩 연동 시스템)

**구현 체크리스트**:
- [ ] **자동 커서 팩 감지**
  - [ ] 레지스트리 기반 현재 커서 스킴 자동 감지
  - [ ] Install.inf 파일 파싱 및 메타데이터 추출
  - [ ] 파일명 패턴 기반 13+5개 커서 타입 매핑
  - [ ] 품질 평가 시스템 (Excellent/Good/Basic/Incomplete)

- [ ] **커서 팩 관리**
  - [ ] 실시간 커서 스킴 변경 감지
  - [ ] 파일 일관성 분석 (80% 이상 동일 경로)
  - [ ] 커서 이미지 캐싱 및 메모리 최적화
  - [ ] 호환성 기반 자동 설정 제안

#### 3.3 멀티 커서 관리 시스템 구현

**참조 문서**:
- `@docs/Android/component-touchpad.md` §2.2.6 (멀티 커서 알고리즘)
- `@docs/Windows/design-guide-server.md` §6.2 (멀티 커서 기능)

**구현 체크리스트**:
- [ ] **가상 커서 렌더링 시스템**
  - [ ] 전체화면 투명 오버레이 윈도우
  - [ ] 6가지 표시 타입 (외곽선, 반투명, 색상 틴트, 글로우, 크기 조절, 점선)
  - [ ] GPU 가속 렌더링 (60fps 목표)
  - [ ] 이벤트 통과 처리 (하위 윈도우 상호작용 보장)

- [ ] **텔레포트 기능 구현**
  - [ ] Android 앱 터치패드 전환 신호 수신
  - [ ] 실제 커서 위치 이동 (50ms 이내)
  - [ ] 3단계 애니메이션 (페이드 아웃→글로우→페이드 인, 총 150ms)
  - [ ] 커서 위치 저장 및 복원

- [ ] **실시간 커서 상태 동기화**
  - [ ] 커서 상태 실시간 모니터링 (Normal, Hand, IBeam 등)
  - [ ] 가상 커서 이미지 동적 업데이트
  - [ ] 커서 팩 연동 기반 이미지 자동 적용
  - [ ] 16ms 주기 가상 커서 위치 업데이트

#### 3.4 고급 통신 프로토콜 구현

**구현 체크리스트**:
- [ ] **Vendor CDC 고급 명령**
  - [ ] MULTI_CURSOR_SWITCH (커서 전환 요청)
  - [ ] UI_FORCE_ENABLE_ALL_TOUCHABLES_REQUEST
  - [ ] CURSOR_PACK_UPDATE_NOTIFICATION

- [ ] **상태 동기화 시스템**
  - [ ] Android ↔ Windows 설정 동기화
  - [ ] 실시간 상태 정보 교환
  - [ ] 설정 충돌 해결 메커니즘
  - [ ] 세션 복구 및 상태 복원

**Phase 3 완료 조건**:
- 멀티 커서 텔레포트 150ms 애니메이션 완료  
- 커서 팩 자동 감지 및 가상 커서 적용  
- Windows 서버 GUI 완전 구현

---

### Phase 4: 안정성 및 품질 보증

**목표**: 전체 시스템 통합 안정성 확보 및 사용자 경험 완성
**예상 기간**: 2-3주
**완료 기준**: 모든 KPI 달성, 접근성 준수, 배포 준비 완료

#### 4.1 통합 테스트 및 품질 검증

**참조 문서**:
- `@docs/Android/technical-specification-app.md` §2.8 (테스트 및 검증)
- `@docs/Board/esp32s3-code-implementation-guide.md` §7 (테스트 및 검증)
- `@docs/PRD.md` §7 (성능 요구사항)

**테스트 체크리스트**:
- [ ] **End-to-End 시나리오 테스트**
  - [ ] 초기 연결부터 고급 기능까지 전체 사용자 시나리오
  - [ ] 멀티 커서 고급 기능 테스트
  - [ ] 24시간 연속 사용 안정성 테스트
  - [ ] 다양한 Windows 환경 호환성 테스트

- [ ] **성능 벤치마크 검증**
  - [ ] 디바이스 등급별 전송 빈도 달성 확인 (83Hz 최소 보장, 120Hz 기본 목표, 167Hz 최대 목표)
  - [ ] E2E 지연시간 50ms 이하 검증
  - [ ] 60fps 애니메이션 프레임 드롭 없음 확인
  - [ ] 메모리 사용량 최적화 달성

#### 4.2 접근성 및 사용성 완성

**참조 문서**:
- `@docs/Android/design-guide-app.md` §7 (접근성 가이드라인)
- `@docs/Windows/design-guide-server.md` §8 (접근성 및 사용성)
- `@docs/Android/technical-specification-app.md` §2.3.1.3 (햅틱 피드백)

**접근성 체크리스트**:
- [ ] **Android 앱 접근성**
  - [ ] TalkBack 지원 및 contentDescription 정의
  - [ ] 최소 48×48dp 터치 영역 보장
  - [ ] 색상 독립적 정보 전달 (아이콘+텍스트)
  - [ ] 햅틱 피드백 카테고리별 구현

- [ ] **Windows 서버 접근성**
  - [ ] WCAG 2.1 AA 기준 준수 (4.5:1 대비율)
  - [ ] AutomationProperties 스크린 리더 지원
  - [ ] 키보드 네비게이션 완전 구현
  - [ ] 44×44px 최소 클릭 영역 보장

#### 4.3 개발 완료 및 로컬 배포

**개인 프로젝트 완성 체크리스트**:
- [ ] **Android APK 개발 빌드**
  - [ ] 디버그 빌드 최적화 및 테스트
  - [ ] USB 디버깅을 통한 직접 설치 가능
  - [ ] 필요 권한 확인 (USB_PERMISSION, VIBRATE)
  - [ ] 개발자 모드 설치 가이드 작성

- [ ] **Windows 서버 실행 파일**
  - [ ] Release 모드 빌드 생성
  - [ ] 자동 실행 배치 스크립트 작성
  - [ ] 필요 의존성 확인 (.NET 8.0+ Runtime)
  - [ ] 로컬 설치 및 실행 가이드

- [ ] **ESP32-S3 펌웨어 개발 완성**
  - [ ] Release 펌웨어 빌드 (-O2 최적화)
  - [ ] PlatformIO/Arduino IDE 업로드 가능
  - [ ] 하드웨어 핀아웃 및 연결 가이드
  - [ ] 펌웨어 업로드 방법 문서화

#### 4.4 문서화 및 지원 시스템

**완성 체크리스트**:
- [ ] **사용자 가이드 문서**
  - [ ] 설치 및 초기 설정 가이드
  - [ ] 기능별 상세 사용법 가이드
  - [ ] 문제 해결 FAQ 및 트러블슈팅
  - [ ] 고급 기능 활용 팁

- [ ] **개발자 문서**
  - [ ] API 레퍼런스 문서 완성
  - [ ] 아키텍처 설명서 업데이트
  - [ ] 확장 기능 개발 가이드
  - [ ] 코드 기여 가이드라인

**Phase 4 완료 조건**:
- 접근성 기본 요구사항 충족
- 개발 완료된 실행 파일들 준비
- 개인용 설치/사용 가이드 문서
- 로컬 개발 환경에서 완전 동작
