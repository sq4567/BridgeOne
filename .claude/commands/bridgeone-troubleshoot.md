# BridgeOne Troubleshooting Guide

BridgeOne 개발 시 발생할 수 있는 문제와 해결 방법을 안내합니다.

---

## Windows USB 성능 최적화

### USB Selective Suspend 비활성화

Windows 11에서 ESP32-S3 USB CDC 통신 시 연결 끊김이 발생하면 다음 레지스트리 설정이 필요합니다.

**경로** (시리얼 번호는 각 기기마다 다름):
```
HKEY_LOCAL_MACHINE\SYSTEM\CurrentControlSet\Enum\USB\VID_303A&PID_4001\[시리얼번호]\Device Parameters
```

**설정 항목**:
```
이름: IdleUsbSelectiveSuspendPolicy
타입: DWORD (32-bit)
값: 0x00000001
```

**적용 방법**:
1. `레지스트리 편집기` 열기 (regedit.exe)
2. 위 경로로 이동 (시리얼 번호는 각 USB 장치마다 다름)
3. `Device Parameters` 폴더 우클릭 → 새로 만들기 → DWORD (32-bit) 값
4. 이름: `IdleUsbSelectiveSuspendPolicy`
5. 값: `0x00000001` 입력
6. 시스템 재부팅

---

## 시리얼 모니터 도구

### ⚠️ 중요: `idf.py monitor` 대신 Tera Term 사용 권장

`idf.py monitor`는 Windows 환경에서 출력 누락이 발생할 수 있습니다.

### Tera Term 설정

**다운로드**: https://ttssh2.osdn.jp/index.html.en

**설정 (Setup → Serial port...)**:
```
Port: COMx (ESP32-S3 포트)
Baud rate: 115200
Data: 8 bit
Parity: none
Stop: 1 bit
Flow control: none
```

### 용도별 도구 선택

| 용도 | 권장 도구 | 비고 |
|------|----------|------|
| 일반 디버그 로그 | **Tera Term** | 안정적, 로그 파일 저장 |
| HEX/바이너리 분석 | RealTerm | UART 프레임 디버깅 |
| 다중 포트 모니터링 | CoolTerm | Android + PC 동시 확인 |
| 플래시 직후 확인 | `idf.py monitor` | ESP-IDF 통합 필요시만 |

### Tera Term 로그 저장

1. File → Log... 선택
2. 저장 경로 및 파일명 지정
3. 로그 시작

---

## 개발 환경 설정

### 필수 소프트웨어

| 구성요소 | 버전 요구사항 | 확인 명령어 |
|---------|-------------|------------|
| Android Studio | 최신 버전 권장 | - |
| ESP-IDF | v5.5+ | `idf.py --version` |
| Python | 3.8+ | `python --version` |
| Java | JDK 17+ | `java -version` |

### ESP-IDF 설치 확인

```bash
# ESP-IDF 버전 확인
idf.py --version

# 환경 변수 확인
echo $IDF_PATH          # Linux/macOS
echo $env:IDF_PATH      # PowerShell

# Python 패키지 확인
pip list | Select-String esp
```

### Android 환경 확인

```bash
# JAVA_HOME 확인
echo $env:JAVA_HOME

# ANDROID_HOME 확인
echo $env:ANDROID_HOME

# Gradle 버전
./gradlew --version
```

---

## 테스트 환경

### 필요 장비

- **Android 디바이스**: USB-OTG 지원 필수
- **ESP32-S3 보드**: YD-ESP32-S3 N16R8 권장
- **PC**: Windows 10/11 (HID 테스트용)
- **케이블**: USB-C to USB-A, Micro-USB to USB-A

### 테스트 시나리오별 구성

| 테스트 유형 | 필요 장비 | 연결 방식 |
|-----------|---------|----------|
| Android 앱 개발 | Android + ESP32-S3 | 1차 연결 |
| 펌웨어 개발 | PC + ESP32-S3 | 플래시용 연결 |
| HID 동작 테스트 | PC + ESP32-S3 | 2차 연결 |
| 통합 테스트 | Android + ESP32-S3 + PC | 풀 연결 |

---

## 보드별 주의사항

### YD-ESP32-S3 N16R8

**기본 정보**:
- UART: UART1 (GPIO17/18) 또는 UART0 (GPIO43/44)
- USB 브릿지: CH343 (드라이버 필요)
- Native USB: GPIO19/20

**알려진 이슈**:
- 일부 보드에서 5V 핀 전압 이슈 보고됨
- **권장**: 사용 전 멀티미터로 전압 확인
- 상세 정보: `docs/board/YD-ESP32-S3-N16R8-analysis.md` 참조

**CH343 드라이버**:
- Windows: 자동 설치 또는 WCH 공식 사이트에서 다운로드
- 다운로드: http://www.wch.cn/downloads/CH343SER_EXE.html

---

## 자주 발생하는 문제

### Android 앱

#### USB 장치 인식 안 됨
```
증상: ESP32-S3가 연결되어도 앱에서 인식하지 못함
원인: USB 권한 미승인 또는 VID/PID 필터 문제

해결:
1. USB 권한 다이얼로그에서 "허용" 선택
2. "항상 이 앱 사용" 체크
3. 케이블 및 USB-OTG 어댑터 점검
4. 다른 USB 포트 시도
```

#### UART 통신 실패
```
증상: 연결은 되지만 데이터 전송/수신 안 됨
원인: Baud rate 불일치 또는 UART 설정 오류

해결:
1. Android 앱과 ESP32-S3 모두 1Mbps 확인
2. 8N1 설정 확인 (8 data bits, No parity, 1 stop bit)
3. TX/RX 핀 연결 확인 (크로스 연결)
```

### ESP32-S3 펌웨어

#### 플래시 실패
```
증상: "A fatal error occurred: Failed to connect" 등
원인: 포트 충돌, 드라이버 문제, 부트 모드 진입 실패

해결:
1. 다른 프로그램이 COM 포트 사용 중인지 확인
2. BOOT 버튼 누른 상태에서 RST 버튼 눌렀다 떼기
3. CH343 드라이버 재설치
4. 다른 USB 케이블 시도
```

#### HID 장치 인식 안 됨
```
증상: PC에서 마우스/키보드 장치로 인식되지 않음
원인: TinyUSB 초기화 실패, USB 디스크립터 오류

해결:
1. Native USB 포트(포트 2) 사용 확인
2. 장치 관리자에서 USB 장치 상태 확인
3. CDC 로그로 초기화 과정 확인
4. sdkconfig에서 USB 관련 설정 확인
```

#### 부팅 루프
```
증상: 계속 재부팅되며 정상 동작하지 않음
원인: 스택 오버플로우, 워치독 타임아웃, 전원 문제

해결:
1. 포트 1(COM)에서 부팅 로그 확인
2. 리셋 원인 메시지 확인 (panic, watchdog 등)
3. 태스크 스택 크기 증가 시도
4. 전원 공급 안정성 확인
```

### PC HID 통신

#### 마우스 커서 움직임 없음
```
증상: HID 장치로 인식되지만 커서가 움직이지 않음
원인: 리포트 전송 실패, 델타 값 0

해결:
1. CDC 로그로 HID 리포트 전송 확인
2. UART 프레임 수신 확인
3. deltaX/deltaY 값 확인
4. Boot Protocol 모드 확인
```

#### 키보드 입력 안 됨
```
증상: 키보드 장치로 인식되지만 키 입력이 안 됨
원인: 키코드 매핑 오류, 수정자 키 문제

해결:
1. HID 키코드 값 확인 (프로토콜 문서 참조)
2. 수정자 키 비트맵 확인
3. 키 릴리스 리포트 전송 확인
```

---

## 디버깅 체크리스트

### 연결 문제 발생 시

- [ ] 케이블 및 어댑터 점검
- [ ] 올바른 포트 사용 확인 (포트 1 vs 포트 2)
- [ ] 드라이버 설치 상태 확인
- [ ] 장치 관리자에서 장치 인식 확인
- [ ] 다른 USB 포트 시도
- [ ] 시스템 재부팅

### 통신 문제 발생 시

- [ ] Baud rate 일치 확인 (1Mbps)
- [ ] UART 설정 일치 확인 (8N1)
- [ ] 프레임 구조 확인 (8바이트)
- [ ] 시리얼 모니터로 로그 확인
- [ ] TX/RX 핀 연결 확인

### 성능 문제 발생 시

- [ ] CPU/메모리 사용량 확인
- [ ] 태스크 우선순위 확인
- [ ] 버퍼 오버플로우 확인
- [ ] USB Selective Suspend 설정 확인
- [ ] 전송 주기 확인 (4-8ms)
