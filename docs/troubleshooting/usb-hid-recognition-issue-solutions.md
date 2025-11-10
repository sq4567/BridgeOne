# ESP32-S3 USB HID 인식 문제 - 해결 시도 및 결과 기록

## 문서 정보
- **작성일**: 2025-11-10
- **관련 문서**: `usb-hid-recognition-issue-analysis.md`
- **문제 ID**: USB-HID-001
- **심각도**: 🔴 CRITICAL

---

## 해결 시도 기록 형식

각 해결 시도는 다음 형식으로 기록됩니다:

```markdown
## 시도 #N: [해결 방법 요약]

### 시도 정보
- **시도일**: YYYY-MM-DD HH:MM
- **목적**: [이 시도로 검증하려는 가설]
- **예상 결과**: [성공 시 기대되는 결과]

### 실행 명령어
[실행한 명령어들]

### 실행 결과
[명령어 출력 결과]

### 관찰 사항
[변화한 점, 로그 메시지, 에러 등]

### 검증 결과
- [ ] idf.py menuconfig에서 TinyUSB Stack 메뉴 표시
- [ ] Windows에서 VID:PID가 0x303A:0x4001로 인식
- [ ] HID Keyboard/Mouse 장치로 열거됨

### 결론
- **상태**: ✅ 성공 / ⚠️ 부분 성공 / ❌ 실패
- **학습 내용**: [이 시도를 통해 배운 점]
- **다음 단계**: [후속 조치]
```

---

## 시도 이력

### 시도 #0: 현재 상태 확인 (베이스라인)

#### 시도 정보
- **시도일**: 2025-11-10 (기준)
- **목적**: 문제 해결 전 현재 상태를 명확히 기록
- **예상 결과**: N/A (현황 파악)

#### 실행 명령어
```powershell
# Windows PowerShell
Get-PnpDevice | Where-Object {$_.DeviceID -match "VID_303A"} | Format-Table FriendlyName, Status, DeviceID

# ESP-IDF (실행 예정)
cd F:\C\Programming\MobileDevelopment\Projects\Android\BridgeOne\src\board\BridgeOne
idf.py -p COM3 monitor
```

#### 실행 결과
```
FriendlyName               Status DeviceID
------------               ------ --------
USB JTAG/serial debug unit OK     USB\VID_303A&PID_1001&MI_02\9&783726D&1&0002
USB 직렬 장치(COM3)        OK     USB\VID_303A&PID_1001&MI_00\9&783726D&1&0000
USB Composite Device       OK     USB\VID_303A&PID_1001\24:58:7C:DE:81:C8
```

#### 관찰 사항
- VID:PID = 0x303A:0x1001 (ESP32-S3 ROM 부트로더 기본값)
- 의도한 PID(0x4001) 아님
- HID 장치가 아닌 CDC 직렬 장치로만 인식됨

#### 검증 결과
- [ ] idf.py menuconfig에서 TinyUSB Stack 메뉴 표시 → **미확인**
- [ ] Windows에서 VID:PID가 0x303A:0x4001로 인식 → **❌ 실패** (0x1001로 인식)
- [ ] HID Keyboard/Mouse 장치로 열거됨 → **❌ 실패** (CDC만 인식)

#### 결론
- **상태**: ❌ 문제 존재 확인
- **학습 내용**: TinyUSB 스택이 전혀 동작하지 않고 ROM 부트로더 USB만 활성화됨
- **다음 단계**: 시도 #1부터 순차적으로 해결 시도

---

## 대기 중인 시도 목록

아래 시도들은 우선순위 순으로 실행될 예정입니다.

---

### 시도 #1: TinyUSB Kconfig 파일 생성 - 🔴 최우선

#### 시도 정보
- **시도일**: 2025-11-10 16:00-16:30
- **목적**: TinyUSB 컴포넌트에 Kconfig 파일이 존재하지 않아 수동으로 생성
- **예상 결과**: Kconfig 파일 생성으로 ESP-IDF 빌드 시스템과 TinyUSB 통합

#### 실행 명령어

```powershell
# 1. TinyUSB 컴포넌트 디렉토리에서 Kconfig 파일 검색
cd F:\C\Programming\MobileDevelopment\Projects\Android\BridgeOne\src\board\BridgeOne
find components\espressif__tinyusb -maxdepth 2 -name "Kconfig*"
# 결과: Kconfig 파일 없음 확인

# 2. ESP-IDF 표준 Kconfig 파일 생성
# 파일 위치: components/espressif__tinyusb/Kconfig
# 내용: HID, CDC, MSC, MIDI 등 TinyUSB 클래스 설정 메뉴

# 3. menuconfig 재실행
idf.py menuconfig
```

#### 실행 결과
✅ Kconfig 파일 생성 완료
- 위치: `components/espressif__tinyusb/Kconfig`
- 크기: ~360 줄
- 내용:
  - TinyUSB Stack 활성화 옵션
  - USB Device 설정
  - HID Device Class (Keyboard, Mouse)
  - CDC Device Class (Serial)
  - MSC, MIDI, Vendor, DFU 클래스 (선택사항)

#### 관찰 사항
- ✅ Kconfig 파일이 ESP-IDF 표준을 따름
- ✅ HID Keyboard/Mouse 옵션 포함
- ✅ 메뉴 구조는 "Component config → TinyUSB Stack" 형식

#### 검증 결과
- [x] Kconfig 파일 생성 완료
- [x] HID Device Class 설정 포함
- [x] CDC Device Class 설정 포함

#### 결론
- **상태**: ✅ 완료
- **학습 내용**: TinyUSB 컴포넌트는 Kconfig 파일이 없어서 menuconfig 메뉴가 표시되지 않음. 표준 Kconfig를 생성하면 ESP-IDF와 통합 가능.
- **다음 단계**: 시도 #2 (sdkconfig 재생성) 진행

---

### 시도 #2: sdkconfig 재생성 및 menuconfig 설정 - ✅ 완료

#### 시도 정보
- **시도일**: 2025-11-10 16:30-17:00
- **목적**: Kconfig 생성 후 sdkconfig 재생성 및 TinyUSB menuconfig 설정
- **예상 결과**: TinyUSB Stack 메뉴가 표시되고 HID/CDC 설정 완료

#### 실행 명령어
```powershell
cd F:\C\Programming\MobileDevelopment\Projects\Android\BridgeOne\src\board\BridgeOne

# 1. Kconfig 파일 생성 (시도 #1에서 완료)
# 파일: components/espressif__tinyusb/Kconfig

# 2. 기존 설정 파일 삭제
rm -f sdkconfig sdkconfig.old

# 3. ESP32-S3 타겟 설정
idf.py set-target esp32s3

# 4. menuconfig 실행
idf.py menuconfig
# → Component config → TinyUSB Stack
#   - Use TinyUSB Stack = Y
#   - Enable USB Device mode = Y
#   - Enable HID Device Class = Y
#     - Number of HID interfaces = 2
#     - Enable HID Keyboard = Y
#     - Enable HID Mouse = Y
#   - Enable CDC Device Class = Y
#     - CDC interfaces = 1
```

#### 실행 결과
✅ menuconfig 설정 완료
- TinyUSB Stack 메뉴가 정상 표시됨
- HID Keyboard/Mouse 옵션 활성화됨
- CDC Serial 설정 완료됨
- **이전의 "unknown kconfig symbol" 경고 사라짐**

#### 관찰 사항
- ✅ Kconfig 파일 생성으로 ESP-IDF와 TinyUSB가 정상 통합됨
- ✅ menuconfig에서 "Component config → TinyUSB Stack" 메뉴 표시
- ✅ 모든 TinyUSB 설정이 정상적으로 로드됨
- ✅ sdkconfig에 CONFIG_TINYUSB_* 설정이 정상 저장됨

#### 검증 결과
- [x] TinyUSB Stack 메뉴 표시 ✅
- [x] HID Device Class 설정 완료 ✅
- [x] CDC Device Class 설정 완료 ✅
- [x] menuconfig 경고 없음 ✅

#### 결론
- **상태**: ✅ 완료
- **학습 내용**: TinyUSB Kconfig 파일 생성으로 ESP-IDF와 TinyUSB 완전 통합. menuconfig에서 모든 설정 가능.
- **다음 단계**: 시도 #3 (빌드 및 플래시)

---

### 시도 #3: 펌웨어 빌드 및 플래시 - ✅ 완료

#### 시도 정보
- **시도일**: 2025-11-10 17:00-17:30
- **목적**: 설정된 TinyUSB 펌웨어를 빌드하고 ESP32-S3에 플래시
- **예상 결과**: 펌웨어 플래시 성공 및 부팅 로그에서 TinyUSB 초기화 확인

#### 실행 명령어
```bash
cd F:\C\Programming\MobileDevelopment\Projects\Android\BridgeOne\src\board\BridgeOne

# 1. 빌드 실행
idf.py build

# 2. 플래시
idf.py -p COM3 flash

# 3. 모니터링
idf.py -p COM3 monitor
```

#### 실행 결과
✅ 빌드 성공
✅ 플래시 성공
✅ 부팅 로그 정상
```
I (771) BridgeOne: TinyUSB device stack initialized
I (778) BridgeOne: UART initialized (1Mbps, 8N1)
I (780) BridgeOne: USB Descriptor: VID=0x303A, PID=0x4001
I (780) BridgeOne: Interfaces: HID Keyboard(0), HID Mouse(1), CDC(2,3)
I (785) BridgeOne: BridgeOne USB Bridge Ready - Waiting for host connection...
```

#### 관찰 사항
- ✅ TinyUSB 초기화 성공
- ✅ USB 디스크립터: VID=0x303A, PID=0x4001
- ✅ HID Keyboard, HID Mouse, CDC 인터페이스 정상
- ✅ 모든 태스크(UART, HID, USB) 정상 시작
- ❌ **Windows가 여전히 VID_303A&PID_1001 표시** (ROM 부트로더)

#### 검증 결과
- [x] 빌드 성공 ✅
- [x] 플래시 성공 ✅
- [x] TinyUSB 초기화 성공 ✅
- [x] 부팅 로그 정상 ✅
- [ ] Windows HID 장치 인식 ❌

#### 결론
- **상태**: ⚠️ 부분 성공
- **발견된 새로운 문제**: 펌웨어는 정상 부팅되지만 Windows가 ROM 부트로더 USB(PID_1001)를 계속 인식
- **근본 원인 (가설)**: USB JTAG/CDC가 여전히 활성화되어 있어 TinyUSB와 충돌, 또는 Windows 장치 캐시 문제
- **다음 단계**: 시도 #4 (USB 설정 충돌 확인 및 해결)

---

### 시도 #4: USB 설정 충돌 확인 및 해결 - ⏳ 대기 중

#### 시도 정보
- **시도일**: [대기 중]
- **목적**: ESP32-S3 내장 USB JTAG/CDC와 TinyUSB 간 충돌 확인 및 해결
- **예상 결과**: USB 콘솔이 UART로 전환되고, USB 포트가 TinyUSB 전용으로 해제됨

#### 실행 계획
```bash
# 1. 현재 USB 콘솔 설정 확인
grep "CONFIG_ESP_CONSOLE" sdkconfig

# 2. USB 관련 모든 설정 확인
grep "CONFIG_USB" sdkconfig | grep -v "CONFIG_TINYUSB"

# 예상되는 문제 설정:
# CONFIG_ESP_CONSOLE_USB_SERIAL_JTAG=y
# CONFIG_ESP_CONSOLE_USB_CDC=y

# 3. menuconfig로 USB 콘솔 비활성화
idf.py menuconfig
# → Component config → ESP System Settings → Channel for console output
# → 선택: "UART" (기본 UART0, GPIO43/GPIO44)

# 4. USB JTAG 기능 비활성화 확인
# → Component config → Hardware Settings → USB-OTG
# → "USB OTG" 활성화, "USB Serial JTAG" 비활성화

# 5. 설정 저장 및 빌드
idf.py build

# 6. 플래시 및 모니터
idf.py -p COM3 flash monitor
```

#### 예상 관찰 사항
- `CONFIG_ESP_CONSOLE_UART=y` 활성화
- `CONFIG_ESP_CONSOLE_USB_SERIAL_JTAG=n` 비활성화
- 빌드 로그에서 USB JTAG 관련 경고 사라짐
- TinyUSB가 USB 포트를 독점적으로 사용 가능

#### 검증 체크리스트
- [ ] `CONFIG_ESP_CONSOLE_UART=y` 확인
- [ ] `CONFIG_ESP_CONSOLE_USB_SERIAL_JTAG=n` 확인
- [ ] 빌드 성공 여부
- [ ] Windows에서 USB JTAG 장치 사라짐
- [ ] HID Keyboard/Mouse 장치 나타남

---

### 시도 #3: TinyUSB 컴포넌트 명시적 활성화 (가설 2 검증) - 이후 단계

#### 시도 정보
- **시도일**: [대기 중]
- **목적**: ESP-IDF의 TinyUSB 컴포넌트가 제대로 등록되어 있는지 확인 및 강제 활성화
- **예상 결과**: TinyUSB 컴포넌트가 빌드 시스템에 정상 등록됨

#### 실행 계획
```bash
# 1. ESP-IDF 컴포넌트 목록 확인
idf.py reconfigure --verbose

# 출력에서 "tinyusb" 문자열 검색
# 예상: "Component: tinyusb" 메시지 확인

# 2. 빌드 디렉토리에서 TinyUSB 오브젝트 파일 확인
ls build/esp-idf/tinyusb/

# 3. sdkconfig.defaults에 명시적 추가 (이미 있지만 재확인)
# CONFIG_TINYUSB=y
# CONFIG_TINYUSB_DEVICE_ENABLED=y

# 4. 강제 재빌드
idf.py fullclean
idf.py reconfigure
idf.py build
```

#### 예상 관찰 사항
- `idf.py reconfigure` 출력에 "Component: tinyusb" 메시지 확인
- `build/esp-idf/tinyusb/` 디렉토리에 다수의 .o 파일 존재
- 빌드 로그에서 TinyUSB 소스 파일 컴파일 메시지 출력

---

### 시도 #4: 런타임 초기화 디버깅 (가설 3 검증) - 이후 단계

#### 시도 정보
- **시도일**: [대기 중]
- **목적**: `tusb_init()` 호출이 성공적으로 실행되는지 확인
- **예상 결과**: 시리얼 모니터에서 TinyUSB 초기화 성공 메시지 및 디버그 로그 확인

#### 실행 계획
```bash
# 1. TinyUSB 디버그 레벨 상승
idf.py menuconfig
# → Component config → TinyUSB Stack → TinyUSB log level
# → 선택: "Debug" 또는 "Verbose"

# 또는 tusb_config.h 수정:
# #define CFG_TUSB_DEBUG 2

# 2. 빌드 및 플래시
idf.py build
idf.py -p COM3 flash monitor

# 3. 시리얼 모니터 출력 확인
# 예상 로그:
# I (xxx) BridgeOne: TinyUSB device stack initialized
# I (xxx) TinyUSB: Device descriptor requested
# I (xxx) TinyUSB: Configuration descriptor requested
```

#### 예상 관찰 사항
- TinyUSB 초기화 로그 출력
- 호스트(Windows)의 디스크립터 요청 로그 출력
- USB 열거 과정의 상세 로그 출력

#### 검증 체크리스트
- [ ] `tusb_init()` 호출 성공 메시지 확인
- [ ] 호스트의 Device Descriptor 요청 확인
- [ ] 호스트의 Configuration Descriptor 요청 확인
- [ ] HID Report Descriptor 요청 확인
- [ ] USB 열거 완료 메시지 확인

---

### 시도 #5: USB 디스크립터 검증 및 수정 - 이후 단계

#### 시도 정보
- **시도일**: [대기 중]
- **목적**: USB 디스크립터 정의가 표준을 준수하는지 검증
- **예상 결과**: 디스크립터 오류 수정 및 Windows 인식 성공

#### 실행 계획
```bash
# 1. USB 디스크립터 덤프 (Windows)
# USB Device Tree Viewer 등 도구 사용하여 현재 디스크립터 확인

# 2. usb_descriptors.c 재검토
# - VID/PID 확인
# - bNumInterfaces 확인 (4개 맞는지)
# - 엔드포인트 번호 중복 확인
# - 디스크립터 길이 계산 확인

# 3. 표준 준수 여부 확인
# - USB 2.0 Specification 준수
# - HID Boot Protocol 준수
# - CDC-ACM Class 준수

# 4. 필요 시 수정 후 재빌드
```

---

### 시도 #6: USB 케이블 및 포트 확인 - 이후 단계

#### 시도 정보
- **시도일**: [대기 중]
- **목적**: 하드웨어적 문제 배제 (USB 케이블, 포트 불량)
- **예상 결과**: 다른 USB 포트 또는 케이블로 동일 증상 재현

#### 실행 계획
```bash
# 1. 다른 USB 포트에 연결 시도
# 2. 다른 USB 케이블 사용 시도
# 3. 다른 PC에 연결 시도 (가능 시)
# 4. ESP32-S3 보드 리셋 버튼 눌러 재부팅 후 확인
```

---

## 시도 후 업데이트 방법

각 시도 후, 위 "시도 #N" 섹션을 다음과 같이 업데이트합니다:

1. **시도일** 필드를 실제 시도한 날짜/시간으로 업데이트
2. **실행 명령어** 섹션에 실제 실행한 명령어 기록
3. **실행 결과** 섹션에 명령어 출력 복사
4. **관찰 사항** 섹션에 변화한 점 기록
5. **검증 결과** 체크리스트 업데이트
6. **결론** 섹션에 성공/실패 여부 및 학습 내용 기록
7. 필요 시 새로운 시도 추가

---

## 성공 기준 (최종 목표)

다음 모든 항목이 충족되면 문제가 완전히 해결된 것으로 간주합니다:

### ✅ ESP-IDF 빌드 시스템
- [ ] `idf.py menuconfig`에서 "Component config → TinyUSB Stack" 메뉴 표시
- [ ] `grep "CONFIG_TINYUSB=y" sdkconfig` 결과 존재
- [ ] 빌드 로그에 TinyUSB 소스 파일 컴파일 메시지 출력

### ✅ 시리얼 모니터 로그
- [ ] "TinyUSB device stack initialized" 메시지 출력
- [ ] "USB task started" 메시지 출력
- [ ] "HID task created" 메시지 출력
- [ ] TinyUSB 디버그 로그에서 호스트 요청 처리 확인

### ✅ Windows 장치 인식
- [ ] VID:PID가 0x303A:0x4001로 표시
- [ ] "BridgeOne USB Bridge" 이름으로 장치 열거
- [ ] HID Keyboard 장치 별도 표시
- [ ] HID Mouse 장치 별도 표시
- [ ] CDC-ACM (선택사항) 장치 표시 (COM 포트)

### ✅ 기능 검증
- [ ] Windows에서 ESP32-S3를 HID 키보드/마우스로 인식
- [ ] (추가 테스트) Android 앱에서 ESP32-S3로 마우스 입력 전송 성공
- [ ] (추가 테스트) ESP32-S3에서 PC로 HID 리포트 전송 성공 (마우스 커서 이동)

---

## 참고 정보

### 유사 문제 해결 사례
- ESP-IDF GitHub Issues에서 "TinyUSB not recognized" 검색
- ESP32 Forum에서 "HID device not enumerated" 검색

### 디버깅 도구
- **Windows**: USB Device Tree Viewer, USBDeview
- **Linux**: `lsusb -v`, `dmesg | grep usb`
- **ESP-IDF**: `idf.py monitor` (시리얼 로그)

### 관련 문서
- ESP-IDF TinyUSB Examples: `$IDF_PATH/examples/peripherals/usb/device/`
- TinyUSB Device Examples: https://github.com/hathach/tinyusb/tree/master/examples/device

---

**작성자**: Claude Code (SuperClaude Framework)
**업데이트 이력**:
- 2025-11-10: 초기 작성, 시도 #0 기록
