---
title: "BridgeOne Android 앱 기술 명세서"
description: "Android 앱의 기술적 설계 요구사항과 명세 정의 문서"
tags: ["technical", "specification", "requirements", "usb-serial", "touchpad", "constants", "design"]
version: "v0.7"
owner: "Chatterbones"
updated: "2025-09-20"
---

# BridgeOne Android 앱 기술 명세서

## 시스템 아키텍처 개요

> **상세 아키텍처**: 전체 시스템 아키텍처는 [`technical-specification.md`]를 참조하세요.

## 개요

### 문서 목적 및 범위

본 문서는 BridgeOne Android 앱의 **구체적 구현 명세서**로, 다음 3개 주요 기술 영역에 대한 상세한 설계 요구사항과 구현 세부사항을 체계적으로 정의합니다:

1. **USB 통신 (HID/Vendor CDC)**: CP2102 칩셋 기반 고속 통신 아키텍처와 BridgeOne 프로토콜 구현
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
- **실시간 처리**: 167Hz 전송 빈도와 50ms 이하 지연시간 목표
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
- CP2102 USB-Serial 칩셋 자동 인식 필요
- VID: 0x10C4, PID: 0xEA60으로 장치 필터링
- USB OTG 호스트 모드 지원 필수

**통신 설정 요구사항:**
- 1Mbps 전송 속도로 고속 통신 구현
- 8N1 설정 (8비트 데이터, 패리티 없음, 1스톱비트)
- 양방향 통신 지원으로 PC 응답 수신 가능

#### 1.1.2 연결 관리 요구사항

**자동 탐지 요구사항:**
- 시스템 부팅 시 CP2102 장치 자동 스캔
- VID/PID 기반 정확한 장치 식별
- 다중 장치 환경에서 우선순위 처리

**권한 관리 요구사항:**
- USB 장치 접근 권한 자동 요청
- 사용자 권한 거부 시 적절한 안내 메시지
- 권한 상태 실시간 모니터링

**연결 설정 요구사항:**
- 1Mbps 통신 속도로 포트 열기
- 8N1 통신 파라미터 설정
- 양방향 데이터 스트림 초기화

### 1.2 통신 프로토콜 설계

#### 1.2.1 BridgeOne 프로토콜 명세

**프레임 구조 요구사항:**
- 8바이트 고정 크기 프레임으로 통신 효율성 확보
- Little-Endian 바이트 순서로 ESP32 호환성 보장
- 순번 필드로 패킷 순서 보장 및 손실 감지

| 바이트 | 필드 | 타입 | 설명 | 범위 |
|-------|------|-----|------|------|
| 0 | `seq` | u8 | 순번 (패킷 순서 보장) | 0~255 순환 |
| 1 | `buttons` | u8 | 버튼 상태 (bit0=L, bit1=R, bit2=M) | 0x00~0x07 |
| 2-3 | `dx` | i16 | 상대 X 이동량 (Little-Endian) | -32767~32767 |
| 4-5 | `dy` | i16 | 상대 Y 이동량 (Little-Endian) | -32767~32767 |
| 6 | `wheel` | i8 | 휠 스크롤량 | -127~127 |
| 7 | `flags` | u8 | 제어 플래그 (멀티커서, 모드 등) | 0x00~0xFF |

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

### 1.3 전송 제어 설계

#### 1.3.1 실시간 전송 요구사항

**전송 주기 요구사항:**
- 167Hz (6ms 주기) 목표 전송 빈도로 실시간성 보장
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

---

## 2. 핵심 기능 로직 및 구현 요구사항

### 2.1 터치패드 알고리즘

#### 2.1.1 터치패드 핵심 알고리즘 요구사항

##### 2.1.1.1 자유 이동 알고리즘 명세

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

#### 2.1.2 직각 이동 알고리즘 명세

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

#### 2.1.3 데드존 보상 알고리즘 명세

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

#### 2.1.4 스크롤 가이드라인 알고리즘 명세

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

#### 2.1.5 멀티 커서 알고리즘 명세

**멀티 커서 모드 전환 요구사항:**
- ESP32-S3 동글로 `multi_cursor_switch` 명령 전송
- 0xFF 헤더 + JSON 구조의 커스텀 명령으로 처리
- Vendor HID/CDC 인터페이스를 통한 Windows 서비스와의 양방향 통신
- 멀티 커서 모드 Enabled 시 `show_virtual_cursor` 요청 전송

**터치패드 영역 분할 요구사항:**
- 단일 터치패드를 논리적으로 `Touchpad1Area`/`Touchpad2Area`로 분할
- 영역 감지: `pad1` 터치 시 커서 A Selected, `pad2` 터치 시 커서 B Selected
- Selected 영역 전환 시 동글 펌웨어로 상태 전송
- 전환 지연시간: 목표 0.1초 이하로 최적화

**상태 동기화 및 오류 처리 요구사항:**
- 동글 펌웨어 응답 확인 메커니즘 필요
- 응답 타임아웃 시 롤백 처리 (이전 상태로 복구)
- Windows 서비스 연결 끊김 시 자동 싱글 커서 모드로 폴백
- 연결 복구 시 이전 멀티 커서 설정 복원

**싱글 커서 모드 복원 요구사항:**
- 멀티 커서 해제 신호: `single_cursor_restore` 명령 ESP32-S3 동글로 전송
- 터치패드 통합 처리: 분할된 영역을 단일 터치패드로 복원
- 가상 커서 해제: Windows 서비스로 `hide_virtual_cursor` 명령 전송
- 상태 정리: 멀티 커서 관련 모든 상태 데이터 정리 및 메모리 해제

**모드 전환 일관성 요구사항:**
- 양방향 전환 보장: 싱글↔멀티 전환이 모두 동일한 안정성으로 처리
- 데이터 무결성: 전환 과정에서 터치 입력 데이터 손실 방지
- 동기화 완료: 모든 하위 시스템(동글, Windows 서비스)의 응답 확인 후 전환 완료 처리

**성능 최적화 요구사항:**
- 커서 전환 지연시간: 0.1초 이하 목표 (싱글↔멀티 양방향)
- 멀티태스킹 효율성: 기존 대비 80% 이상 시간 단축
- 메모리 사용량: 싱글 커서 모드 대비 20% 이하 증가
- 배터리 소모: 고급 기능 사용 시 15% 이하 추가 소모

**가상 커서 표시 요구사항:**
- Windows 서비스로 `show_virtual_cursor` 명령 전송
- 현재 Selected 터치패드 영역 정보 포함
- Unselected 커서 위치 정보 실시간 동기화
- PC 화면에서 가상 커서 시각적 표시

**상태 추적 요구사항:**
- 각 터치패드 영역별 독립적인 커서 상태 관리
- Selected/Unselected 전환 시 상태 정보 Windows 서비스와 동기화
- 터치패드 좌표와 PC 화면 좌표 매핑 정확성 보장

### 2.2 컴포넌트 설계 요구사항

#### 2.2.1 공통 컴포넌트 설계 요구사항

##### 2.2.1.1 상태 알림 토스트 구현 요구사항

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

##### 2.2.1.2 페이지 인디케이터 구현 요구사항

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

##### 2.2.1.3 햅틱 피드백 구현 요구사항

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

#### 2.2.2 특수 컴포넌트 설계 요구사항

##### 2.2.2.1 KeyboardKeyButton 컴포넌트 설계 요구사항

**KeyboardKeyButton 요구사항:**
- 터치 다운 시 즉시 키 다운 신호 전송
- 터치 업 시 키 업 신호 전송
- 0.95배 스케일 애니메이션으로 터치 피드백 제공
- 500ms 장기 누르기로 Sticky Hold 모드 진입
- Fill 애니메이션으로 진행 상태 시각적 표시
- Sticky Hold 상태에서는 다음 탭까지 키 유지
- 라치 해제 시 키 업 신호 전송
- 터치 시 햅틱 피드백 제공
- 200ms 스케일 애니메이션으로 반응성 향상

##### 2.2.2.2 MacroButton 컴포넌트 설계 요구사항

**MacroButton 요구사항:**
- 매크로 ID 기반 실행 요청 전송
- 150ms 디바운스로 중복 입력 방지
- 실행 중 버튼 비활성화로 중복 실행 방지
- Pending 상태에서 재탭 시 매크로 취소 요청
- 30초 타임아웃으로 무한 대기 방지
- 응답 수신 시 적절한 상태 복구
- 성공/오류/취소 상태별 토스트 메시지 표시
- 각 상태별 햅틱 피드백 제공
- 매크로 미발견 시 즉시 오류 처리

##### 2.2.2.3 DPad 컴포넌트 설계 요구사항

**DPad 컴포넌트 요구사항:**
- **8분할 섹터 판정**: 8방향 (UP, DOWN, LEFT, RIGHT, UP_LEFT, UP_RIGHT, DOWN_LEFT, DOWN_RIGHT)
- **중심 영역**: CENTER (컨테이너 반지름의 30%)
- **섹터 분할**: 각 45도 범위로 균등 분할, 0도 = 오른쪽, 시계방향 증가
- **방향 결정**: 중심점 벡터 계산, 10도 허용오차, 중심 영역은 입력 무시
- **방향 전환 처리**:
  - 터치 다운 시 방향 감지 및 키 다운 전송
  - 터치 이동 시 방향 변화 감지 및 전환 처리
  - 터치 업 시 모든 방향 키 업 전송
  - 이전 방향 키 업 후 새 방향 키 다운 전송
  - 50ms 디바운스로 빠른 전환 시 안정성 확보
  - CENTER 방향은 키 입력으로 처리하지 않음
  - 대각선 방향은 두 개의 직교 키 조합으로 처리 (UP_LEFT = UP + LEFT)
  - 키 다운/업 순서 보장으로 정확한 입력 전달

### 2.3 UI 상태 제어 시스템

#### 2.3.1 컴포넌트 비활성화 시스템 설계

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

#### 2.3.2 비활성화 표현 시스템 요구사항

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

**입력 차단 요구사항:**
- 비활성화된 요소에 대한 터치 입력 완전 차단
- 키보드 포커스 이동 제한
- 접근성 도구와의 호환성 유지
- 예외 상황에서의 안전한 입력 차단 해제

#### 2.3.3 강제 해제 메커니즘 요구사항

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

#### 2.3.4 예외 상황 처리 요구사항

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

#### 2.3.5 접근성 및 사용성 요구사항

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

### 2.4 Essential/Standard 모드 관리 요구사항

#### 2.4.1 모드 전환 요구사항

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

#### 2.4.2 모드별 기능 제한 요구사항

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

### 2.5 성능 및 품질 요구사항

#### 2.5.1 메모리 관리 요구사항

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

#### 2.5.2 UI 렌더링 최적화 요구사항

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

#### 2.5.3 통신 성능 최적화 요구사항

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

#### 2.5.4 배터리 최적화 요구사항

**적응적 전송 빈도 요구사항:**
- 50% 이상: 167Hz 최대 성능 모드
- 20-50%: 125Hz 절전 모드
- 20% 이하: 83Hz 저전력 모드
- 배터리 레벨에 따른 전송 빈도 자동 조절
- 고성능/균형/절전 모드별 설정 제공
- 실시간 배터리 상태 모니터링

### 2.6 오류 처리 요구사항

#### 2.6.1 오류 타입 정의 및 분류 요구사항

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
  - 상세 UI/UX 설계: `Docs/Android/styleframe-essential.md` 참조
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

#### 2.6.2 비정상 종료 복구 요구사항

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

#### 2.6.3 재시도 메커니즘 요구사항

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

### 2.7 테스트 및 검증 요구사항

#### 2.7.1 단위 테스트 요구사항

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
- CP2102 장치 자동 인식 정확성 검증
- 권한 요청 및 응답 처리 검증
- Keep-alive 메커니즘 동작 확인
- 연결 끊김 감지 및 복구 검증

#### 2.7.2 통합 테스트 요구사항

**End-to-End 테스트 요구사항:**
- 앱 시작부터 PC 입력 인식까지 전체 플로우 검증
- USB 연결 → 서버 핸드셰이크 → 터치 입력 → PC 반영 검증
- Essential/Standard 모드 전환 동작 확인
- 연결 끊김 및 복구 시나리오 검증

**성능 테스트 요구사항:**
- 167Hz 전송 빈도 달성 확인
- 엔드투엔드 지연 50ms 이하 검증
- 메모리 사용량 및 배터리 소모 측정
- 장시간 사용 시 안정성 검증

#### 2.7.3 사용자 시나리오 테스트 요구사항

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
| `TRANSMISSION_TARGET_FREQUENCY` | 167 | Hz | 목표 전송 주기 | Frame Transmission |
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
| `BATTERY_MEDIUM_THRESHOLD` | 20 | % | 절전 모드 배터리 임계값 | Battery Optimization |
| `BATTERY_LOW_FREQUENCY` | 83 | Hz | 저전력 모드 전송 빈도 | Battery Optimization |
| `BATTERY_MEDIUM_FREQUENCY` | 125 | Hz | 절전 모드 전송 빈도 | Battery Optimization |
| `BATTERY_HIGH_FREQUENCY` | 167 | Hz | 고성능 모드 전송 빈도 | Battery Optimization |

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
| `PERFORMANCE_TARGET_FREQUENCY` | 167 | Hz | 성능 테스트 목표 주파수 | Performance Testing |
| `PERFORMANCE_TARGET_LATENCY_MS` | 50 | ms | 성능 테스트 목표 지연시간 | Performance Testing |
| `STRESS_TEST_DURATION_HOURS` | 24 | 시간 | 스트레스 테스트 지속시간 | Stress Testing |
| `MEMORY_LEAK_THRESHOLD_MB` | 100 | MB | 메모리 누수 임계값 | Memory Testing |

---

이 기술 명세서는 BridgeOne Android 앱의 모든 기술적 설계 요구사항과 명세를 정의합니다. 실제 개발 시에는 각 섹션의 요구사항과 상수값을 참조하여 일관된 구현을 보장하시기 바랍니다.
