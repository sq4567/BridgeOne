---
title: "BridgeOne 프로젝트 문서 가이드"
description: "docs/ 및 .cursor/rules/ 디렉터리의 모든 문서에 대한 목적과 내용을 요약한 안내서입니다."
tags: ["documentation", "guide", "onboarding", "cross-platform"]
version: "v0.6"
owner: "Chatterbones"
updated: "2025-09-20"
---

# BridgeOne 프로젝트 문서 가이드

이 문서는 BridgeOne 프로젝트의 설계, 아키텍처, 구현에 관련된 모든 핵심 문서들을 이해하기 쉽게 요약한 안내서입니다. 각 문서의 목적과 주요 내용을 파악하여 신규 참여자가 프로젝트 구조를 빠르게 이해하고 필요한 정보를 효율적으로 찾을 수 있도록 돕습니다.

## 문서 체계 개요

BridgeOne 프로젝트의 문서들은 크게 두 가지 목적으로 구분됩니다:

- **`docs/`**:
  - **"무엇을(What)"과 "왜(Why)"**를 정의하는 순수한 설계 및 명세 문서
  - 개발 관련 예제 코드, 함수 및 API명은 일체 작성하지 않음(pseudocode, 상수/임계값은 작성 허용)
  - Rule 파일 생성을 위한 설계 의도와 요구사항만 포함된 컨텍스트 제공용 문서
- **`.cursor/rules/`**:
  - `docs/` 문서 내용을 **"어떻게(How)"** 구현할 것인지를 안내하는 실행 가이드 문서
  - 구체적인 API 사용법, 구현 패턴, 예제 코드를 포함한 개발 가이드
  - 모든 내용은 공식 문서, 믿을만한 프레임워크/라이브러리에서 기반해 작성되어야 함

---

## 1. 제품 기획 및 요구사항

### `PRD.md`: 제품 요구사항 정의서

프로젝트의 목표, 해결하려는 문제, 사용자 프로필, 핵심 기능 요구사항을 정의하는 **최상위 기획 문서**

**주요 내용:**
- 프로젝트 비전 및 핵심 가치 제안
- 타겟 사용자 정의 및 사용 시나리오
- MVP 기능 범위 및 우선순위
- 성공 지표 및 평가 기준

### `development-plan.md`: 개발 계획서

**주요 내용:**
- 단계별 개발 마일스톤 정의
- 기능별 완료 상태 추적
- 의존성 및 우선순위 관리
- 품질 보증 및 테스트 계획

---

## 2. 시스템 아키텍처 및 기술 명세

### `technical-specification.md`: 전체 시스템의 기술적 계약을 정의하는 **최상위 명세 문서**

**주요 내용:**
- 플랫폼 간 통신 프로토콜 정의 및 메시지 형식
- 전체 시스템 상태 모델 및 데이터 구조
- 성능 목표 및 기술적 제약사항
- 시스템 전반의 상수 값 및 구성 파라미터

---

## 3. 디자인 및 브랜딩

### `bridgeone-logo-concepts.md`: BridgeOne의 브랜드 아이덴티티와 로고 디자인 컨셉을 정의하는 로고 컨셉 및 브랜딩 문서

**주요 내용:**
- 브랜드 철학: 연결성, 희망성, 단순함
- 오작교(Magpie Bridge) 컨셉 기반 로고 디자인
- 상태별 색상 시스템 (연결됨/연결중/끊김/비활성)
- 다양한 사이즈별 최적화 방안 (앱 아이콘 ~ 트레이 아이콘)

### `splash-screen-guide-app.md`: Android 앱 애니메이션 스플래시 스크린

**주요 내용:**
- **6단계 애니메이션 시퀀스**: 초기 검은 화면 → 브릿지 등장 → 별 회전 → 확장 → 텍스트 등장 (총 2.5초)
- **Android 기술 구현**: Jetpack Compose Canvas 기반 애니메이션
- **성능 최적화**: 60fps 유지, 메모리 효율성, 배터리 최적화
- **접근성 지원**: 시각적 애니메이션과 대체 텍스트 제공

---

## 4. Android

#### 4.1. 전체 설계 가이드

- `Android/design-guide-app.md`: Android 앱의 전반적인 디자인 원칙, 공통 컴포넌트, 화면 레이아웃, 전체 유저 플로우를 정의하는 최상위 UX 가이드
- `Android/component-design-guide-app.md`: 터치패드, 버튼, D패드 등 앱의 특수 UI 컴포넌트에 대한 상세한 디자인 명세와 상호작용 규칙 정의
- `Android/component-touchpad.md`: 가장 중요하고도 복잡한 컴포넌트인 터치패드의 구조, 모드, 제스처, 색상 시스템, 상태 변화 등 모든 세부 사항을 기술한 심층 명세 문서

#### 4.2. 페이지별 스타일프레임

- `Android/styleframe-essential.md`: Windows 서버 미연결 상태에서 사용되는 필수 기능 페이지의 UI 레이아웃과 스타일 (BIOS/로그인 등 포함)
- `Android/styleframe-connection-waiting.md`: 연결 대기 화면(USB 동글 + 서버 연결)의 UI 디자인 및 상태 표시 방식
- `Android/styleframe-page1.md`: 마우스 기능 중심의 메인 페이지 (터치패드 + 주요 액션) UI 레이아웃과 스타일
- `Android/styleframe-page2.md`: 키보드 기능 중심 페이지의 UI 레이아웃과 스타일
- `Android/styleframe-page3.md`: Minecraft 게임에 특화된 페이지의 UI 레이아웃과 스타일

#### 4.3. 기술 명세 및 구현 가이드

- `Android/technical-specification-app.md`: Android 앱의 기술적 설계 요구사항과 명세를 정의하는 **구현 중심 문서**
  - **역할**: `design-guide-app.md`에 정의된 앱 전체 설계 원칙과 공통 컴포넌트, 그리고 `Android/styleframe-*.md`에 기술된 각 화면 및 페이지별 UI/UX가 실제로 구현되기 위해 반드시 충족해야 하는 **구체적 설계 명세**와 **구현 세부사항**을 체계적으로 정리한 문서

- `Android/splash-screen-guide-app.md`: Android 앱의 브랜드 아이덴티티를 반영한 6단계 애니메이션 스플래시 스크린 구현 가이드 (총 2.5초, 브릿지 등장 → 별 회전 → 방사형 배경 전환)
  - **Android**: Jetpack Compose Canvas 기반 애니메이션 구현
  - **성능 최적화**: 60fps 유지, 메모리 효율성, 배터리 최적화
  - **접근성 지원**: 시각적 애니메이션과 대체 텍스트 제공

---

## 5. Board (Geekble nano)

### `Board/esp32s3-code-implementation-guide.md`: ESP32-S3 (Geekble nano) 설계 명세서

**주요 내용:**
- ESP32-S3의 USB 브릿지 역할 및 시스템 내 위치 정의
- UART ↔ USB HID/CDC 프로토콜 변환 시스템 설계
- FreeRTOS 기반 실시간 처리 아키텍처 (4개 태스크 구조)
- 핵심 기능 모듈 요구사항 (UART 통신, USB HID, Vendor CDC)
- 성능 최적화 전략 (실시간 처리, 메모리 관리, 전력 관리)
- 오류 처리 및 복구 메커니즘
- 테스트 및 검증 방법론

**BridgeOne 활용**: ESP32-S3 펌웨어 개발의 완전한 설계 기반 제공, 50ms 이하 엔드투엔드 지연시간 목표 달성을 위한 아키텍처 원칙

---

## 6. Windows 서버 설계

### **Windows 서버 설계 문서군**

- **`Windows/technical-specification-server.md`**: Windows 서버 프로그램의 아키텍처, 기술 스택, 핵심 기능(멀티 커서 등)의 동작 원리 및 구현 방식을 설명하는 기술 중심 문서
- **`Windows/design-guide-server.md`**: Windows 서버 GUI의 전체적인 디자인 원칙, UX 흐름, 각 기능의 구조 및 흐름 등을 정의하는 UX 가이드
- **`Windows/styleframe-server.md`**: 서버 GUI의 각 기능 및 메뉴별 세뷰 UI 표시 사항 등 구체적인 구조 정의
- **스플래시 스크린 애니메이션**: `styleframe-server.md` §3.1 및 `technical-specification-server.md` §3.9에서 상세 구현 내용 확인

---

## 7. 외부 참고 문서 및 개발 자료

BridgeOne 프로젝트 개발에 필요한 외부 공식 문서 및 개발 자료들을 개발 단계별로 정리한 참고 가이드입니다.

### 7.1 하드웨어 관련 문서

#### 7.1.1 ESP32-S3-DevkitC-1 공식 문서 (Espressif)

**출처**: [ESP32-S3 공식 문서](https://docs.espressif.com/projects/esp-idf/en/latest/esp32s3/)

**필수 문서**:
- **ESP32-S3 데이터시트**: GPIO, UART, USB 기능 상세 사양
- **ESP-IDF 프로그래밍 가이드**: UART 통신, USB HID 구현 방법
- **ESP32-S3-DevkitC-1 보드 설명서**: 핀 배치, USB-to-UART 포트, USB-OTG 포트 설명

**BridgeOne 활용**: 
- ESP32-S3-DevkitC-1-N16R8 보드 기반 개발
- 내장 USB-to-UART 포트를 통한 Android 연결
- USB-OTG 포트를 통한 Windows PC 연결 (HID + CDC 복합 장치)
- UART 1Mbps 통신, USB HID Boot Protocol 구현

#### 7.1.2 Android USB OTG 개발 가이드

**출처**: [Android USB Host API 공식 문서](https://developer.android.com/develop/connectivity/usb/host?hl=ko)

**필수 내용**:
- USB 장치 인식 및 권한 요청
- USB 장치 연결 및 통신
- USB 장치 분리 감지 및 처리

**BridgeOne 활용**: ESP32-S3 내장 USB-to-UART 포트 인식, OTG 케이블 호환성, UART 통신 연결 안정성 확보

#### 7.1.3 USB-Serial-for-Android 라이브러리

**출처**: [USB-Serial-for-Android GitHub](https://github.com/mik3y/usb-serial-for-android)

**필수 문서**:
- **라이브러리 문서**: ESP32-S3 USB-to-UART 포트 통신 구현
- **API 참조**: 양방향 데이터 전송 방법
- **예제 코드**: 실제 구현 사례

**BridgeOne 활용**: ESP32-S3와의 안정적인 UART 통신(1Mbps), 8바이트 프레임 전송/수신, 시리얼 포트 자동 감지

#### 7.1.4 Android 접근성 개발 가이드

**출처**: [Android 접근성 서비스 가이드](https://developer.android.com/guide/topics/ui/accessibility)

**필수 내용**:
- 터치 이벤트 처리 및 접근성
- 사용자 인터페이스 접근성
- WCAG 2.1 AA 준수 가이드

**BridgeOne 활용**: 근육장애 사용자를 위한 터치패드 최적화, 접근성 기능 구현

### 7.2 Windows 개발 관련 문서

#### 7.2.1 Windows HID API 문서

**출처**: [Windows HID API 공식 문서](https://docs.microsoft.com/en-us/windows/win32/inputdev/human-interface-devices)

**필수 내용**:
- HID 장치 인식 및 처리
- HID 입력 데이터 처리
- 백그라운드 서비스 구현

**BridgeOne 활용**: ESP32-S3 HID 마우스 인식, 멀티 커서 관리, 매크로 실행

#### 7.2.2 C# .NET USB 통신 자료

**출처**: [.NET USB 통신 라이브러리](https://github.com/MelbourneDeveloper/Usb.Net)

**필수 문서**:
- **USB.NET 라이브러리**: USB 장치 통신
- **WPF UI 개발 가이드**: 가상 커서 시각화
- **GPU 가속 렌더링**: 성능 최적화

**BridgeOne 활용**: Windows 서버 개발, 가상 커서 시각화, 매크로 시스템

### 7.3 표준 및 프로토콜 문서

#### 7.3.1 USB 표준 문서

**출처**: [USB 2.0 사양서](https://www.usb.org/documents)

**필수 문서**:
- **USB 2.0 사양서**: 전기적 특성, 프로토콜 정의
- **USB HID Usage Tables**: 마우스, 키보드 HID 용도 정의
- **USB OTG 사양서**: Host/Device 역할 전환

**BridgeOne 활용**: USB 통신 프로토콜 이해, HID Boot Protocol 구현

#### 7.3.2 UART 통신 표준

**출처**: [RS-232 표준](https://www.eia.org/standards/)

**필수 내용**:
- UART 통신 프로토콜
- 보레이트, 패리티, 플로우 컨트롤
- 신호 무결성 및 타이밍

**BridgeOne 활용**: Android 앱과 ESP32-S3 간 UART 통신 최적화 (1Mbps, 8N1 설정)

*이 가이드는 BridgeOne 프로젝트의 문서 생태계를 종합적으로 안내하여, 모든 참여자가 필요한 정보를 빠르게 찾고 효율적으로 개발할 수 있도록 돕기 위해 작성되었습니다.*