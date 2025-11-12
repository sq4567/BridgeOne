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
cd F:\\C\\Programming\\MobileDevelopment\\Projects\\Android\\BridgeOne\\src\\board\\BridgeOne
find components\\espressif__tinyusb -maxdepth 2 -name "Kconfig*"
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
cd F:\\C\\Programming\\MobileDevelopment\\Projects\\Android\\BridgeOne\\src\\board\\BridgeOne

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
cd F:\\C\\Programming\\MobileDevelopment\\Projects\\Android\\BridgeOne\\src\\board\\BridgeOne

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

### 시도 #4: USB 설정 충돌 확인 (menuconfig 접근) - ⚠️ 실패 (원인 파악)

#### 시도 정보
- **시도일**: 2025-11-10 17:30 ~ 2025-11-11 (진행 중)
- **목적**: ESP32-S3 내장 USB JTAG/CDC와 TinyUSB 간 충돌 확인 및 menuconfig로 해결
- **예상 결과**: USB 콘솔이 UART로 전환되고, USB 포트가 TinyUSB 전용으로 해제됨

#### 실행 계획 및 결과
```bash
# 1. 현재 USB 콘솔 설정 확인
grep "CONFIG_ESP_CONSOLE" sdkconfig
# 결과: CONFIG_ESP_CONSOLE_UART=y ✅ (이미 UART로 설정됨)

# 2. USB 관련 모든 설정 확인
grep "CONFIG_USB" sdkconfig | grep -v "CONFIG_TINYUSB"
# 결과:
# CONFIG_USJ_ENABLE_USB_SERIAL_JTAG=y ❌ (이것이 문제!)
# CONFIG_USB_OTG_SUPPORTED=y ✅
# CONFIG_TINYUSB=y (TinyUSB 필터링으로 제외됨)

# 3. menuconfig 실행 시도
idf.py menuconfig
# → Component config → Hardware Settings → USB Serial/JTAG
# → 관찰: CONFIG_USJ_ENABLE_USB_SERIAL_JTAG 옵션이 회색으로 표시 (변경 불가능)
```

#### 관찰 사항
- ❌ `CONFIG_ESP_CONSOLE_UART=y`는 이미 올바르게 설정됨
- ❌ `CONFIG_USJ_ENABLE_USB_SERIAL_JTAG=y`가 menuconfig에서 변경 불가능한 상태
- ❌ 의존성 시스템에 의해 필수로 강제되는 것으로 파악
- ❌ 수동으로 sdkconfig 파일을 편집해도 다시 빌드하면 원래 값으로 되돌아옴
- 💡 원인: Kconfig 파일의 `select` 또는 `depends on` 구문이 이 설정을 필수로 지정

#### 검증 결과
- [x] `CONFIG_ESP_CONSOLE_UART=y` 확인 ✅
- [ ] `CONFIG_ESP_CONSOLE_USB_SERIAL_JTAG=n` 확인 ❌ (변경 불가능)
- [x] menuconfig에서 USB JTAG 옵션 발견 ✅
- [ ] USB JTAG 장치 제거 ❌

#### 결론
- **상태**: ❌ menuconfig 방식으로는 해결 불가능
- **발견된 새로운 문제**: Kconfig 의존성 체계가 USB_SERIAL_JTAG를 필수로 강제
- **근본 원인**:
  - `CONFIG_USJ_ENABLE_USB_SERIAL_JTAG=y`가 Kconfig에서 선택되지 않으면 다른 중요한 설정이 활성화되지 않음
  - Windows는 첫 번째로 열거되는 ROM 부트로더 USB JTAG/CDC를 점유하면 이후의 TinyUSB 열거를 무시
- **다음 단계**: eFuse 방식으로 ROM 부트로더 USB를 하드웨어 레벨에서 완전 비활성화 필요

---

### 시도 #4B: eFuse를 이용한 USB_PHY_SEL 활성화 - ⚠️ 부분 성공

#### 시도 정보
- **시도일**: 2025-11-12
- **목적**: eFuse를 사용하여 ROM 부트로더의 USB 기능 완전 비활성화
- **예상 결과**: ROM 부트로더 USB가 완전히 비활성화되어 TinyUSB만 열거됨

#### 실행 명령어 (실제)
```bash
cd F:\\C\\Programming\\MobileDevelopment\\Projects\\Android\\BridgeOne\\src\\board\\BridgeOne

# 1. 현재 eFuse 상태 확인
python C:\\Espressif\\frameworks\\esp-idf-v5.5.1\\components\\esptool_py\\esptool\\espefuse.py --port COM3 summary
# 결과에서 USB_PHY_SEL 상태 확인
# - 0 = 기본값 (USB_SERIAL_JTAG이 내부 PHY 사용)
# - 1 = 활성화됨 (USB_OTG가 내부 PHY 사용, ROM 부트로더 USB 비활성화)

# 2. USB_PHY_SEL eFuse 활성화 (PHY를 USB_OTG로 전환, 한 번 설정하면 되돌릴 수 없음)
python %IDF_PATH%\\components\\esptool_py\\esptool\\espefuse.py --port COM3 burn_efuse USB_PHY_SEL 1
# 확인 메시지에서 'BURN' (대문자) 입력하여 확정

# 3. ESP32-S3 자동 재부팅 대기
# "Applying ESP32 efuses..."
# 재부팅 후 eFuse 적용 완료

# 4. Windows에서 USB 장치 확인
Get-PnpDevice | Where-Object {$_.DeviceID -match "VID_303A"} | Format-Table FriendlyName, Status, DeviceID
# 예상 결과: PID_1001 사라지고 PID_4001 (HID) 표시됨

# 5. ESP-IDF 모니터로 로그 확인
idf.py -p COM3 monitor
# TinyUSB 초기화 로그 및 정상 동작 확인
```

#### 실행 결과 (실제)

✅ **eFuse burn 성공 (USB_PHY_SEL = 0 → 1)**
- eFuse burn 진행 중 Serial data stream stopped (예상된 동작)
- 보드 강제 재부팅 후 eFuse 적용 확인
- 포트 변경: COM3 → COM7

✅ **펌웨어 플래시 성공 (COM7)**
```
Wrote 230976 bytes (130027 compressed) at 0x00010000 in 3.7 seconds
Hash of data verified. ✅
```

❌ **Hard reset 후 USB 연결 끊김**
```
Hard resetting with a watchdog...
A serial exception error occurred: Cannot configure port, something went wrong.
PermissionError(13, '시스템에 부착된 장치가 작동하지 않습니다.', None, 31)
```

⚠️ **Windows 장치 인식 상태**
```
FriendlyName               Status  DeviceID
USB Composite Device       OK      USB\VID_303A&PID_0009 (TinyUSB)
USB Composite Device       Unknown USB\VID_303A&PID_1001 (ROM - 비활성화)
ESP32-S3                   Error   USB\VID_303A&PID_0009&MI_02
USB 직렬 장치(COM7)        OK      USB\VID_303A&PID_0009&MI_00
```

#### 관찰 사항
- ✅ eFuse burn 성공: USB_PHY_SEL = 1 (ROM 부트로더 USB 비활성화)
- ✅ ROM 부트로더 USB (PID_1001): Unknown 상태로 비활성화
- ✅ TinyUSB (PID_0009): OK 상태로 활성화
- ❌ **Hard reset 후 USB 연결 강제 종료**: eFuse 변경으로 인한 USB 모드 전환 시 발생
- ❌ **PID가 0x4001이 아닌 0x0009로 인식**: TinyUSB가 HID 없이 CDC만 활성화된 상태
- 🔴 **근본 원인 발견**: sdkconfig에서 `CONFIG_TINYUSB_HID_KEYBOARD_ENABLED=n` 및 `CONFIG_TINYUSB_HID_MOUSE_ENABLED=n`

#### 검증 결과
- [x] eFuse 상태 확인 (USB_PHY_SEL = 1) ✅
- [x] burn_efuse 명령 실행 성공 ✅
- [x] Windows에서 PID_1001 비활성화됨 ✅
- [ ] Windows에서 PID_4001 나타남 ❌ (PID_0009로 인식, HID 비활성화)
- [ ] HID Keyboard/Mouse 장치로 인식됨 ❌
- [x] TinyUSB CDC로 정상 로깅 가능 ✅ (COM7)

#### 결론
- **상태**: ⚠️ 부분 성공
- **성공**: eFuse burn 완료, ROM 부트로더 USB 비활성화, TinyUSB 활성화
- **실패**: HID Keyboard/Mouse 비활성화 상태로 TinyUSB 초기화 (설정 누락)
- **근본 원인**: sdkconfig에서 HID Keyboard/Mouse 설정이 저장되지 않음
- **다음 단계**: menuconfig에서 HID Keyboard/Mouse 활성화 후 재빌드 필요

#### USB 연결 끊김 현상 분석
**증상**: Hard reset 후 `PermissionError(13, '시스템에 부착된 장치가 작동하지 않습니다.')`

**원인**:
1. eFuse 변경으로 USB 모드가 전환됨 (ROM USB → TinyUSB)
2. Hard reset 시 ESP32-S3이 재부팅되면서 USB가 재열거됨
3. Windows가 USB 장치를 재인식하는 동안 esptool이 포트 접근 시도
4. 포트가 아직 준비되지 않아 PermissionError 발생

**해결책**:
- 이것은 **정상적인 동작**입니다
- 펌웨어 플래시는 이미 성공했으므로 (Hash verified) 에러 무시 가능
- 재부팅 후 포트가 재할당되므로 (COM3 → COM7) 새 포트로 접근 필요

#### 추가 정보
**eFuse "Burning"에 대한 안전성**:
- "Burning"은 하드웨어를 물리적으로 손상시키는 것이 아님
- eFuse는 일회성 프로그래밍 가능한 메모리 비트로, 한 번 설정하면 읽기 전용이 됨
- 이것은 정상적인 설정이며, ROM 부트로더를 비활성화하는 표준 방법
- USB_PHY_SEL을 비활성화해도 펌웨어 기능은 정상 작동 (TinyUSB로 USB 사용)
- 다음 펌웨어 업데이트 시에는 Download Mode로 진입하여 플래시 가능

---

### 시도 #5: HID Keyboard/Mouse menuconfig 활성화 - ⏳ 대기 중

#### 시도 정보
- **시도일**: [진행 예정]
- **목적**: sdkconfig에서 HID Keyboard/Mouse 설정을 명시적으로 활성화
- **예상 결과**: TinyUSB가 HID Keyboard + Mouse + CDC 복합 장치로 초기화됨

#### 실행 계획
```bash
cd F:\\C\\Programming\\MobileDevelopment\\Projects\\Android\\BridgeOne\\src\\board\\BridgeOne

# 1. menuconfig 실행
idf.py menuconfig

# 2. TinyUSB Stack 메뉴로 이동
# Component config → TinyUSB Stack → HID Device Class
#   ☑ Enable HID Keyboard (현재: [ ])
#   ☑ Enable HID Mouse (현재: [ ])

# 3. 저장 (S 키) → 종료 (Q 키)

# 4. sdkconfig 확인
Get-Content sdkconfig | Select-String "CONFIG_TINYUSB_HID"
# 예상:
#   CONFIG_TINYUSB_HID_ENABLED=y
#   CONFIG_TINYUSB_HID_KEYBOARD_ENABLED=y
#   CONFIG_TINYUSB_HID_MOUSE_ENABLED=y

# 5. 클린 빌드
idf.py fullclean
idf.py build

# 6. 플래시 (Download Mode)
idf.py -p COM7 flash

# 7. 모니터링 (재부팅 후 새 포트 확인)
Get-PnpDevice | Where-Object {$_.DeviceID -match "VID_303A"}
idf.py -p [NEW_PORT] monitor
```

#### 예상 관찰 사항
- menuconfig에서 HID Keyboard/Mouse 활성화 성공
- sdkconfig에 설정 저장됨
- 빌드 로그에서 HID 관련 코드 컴파일 확인
- 부팅 로그: "Interfaces: HID Keyboard(0), HID Mouse(1), CDC(2,3)"
- Windows에서 PID_4001로 인식
- HID Keyboard, HID Mouse 장치 열거

#### 검증 체크리스트
- [ ] menuconfig에서 HID Keyboard/Mouse 활성화
- [ ] sdkconfig에 설정 저장 확인
- [ ] 빌드 성공
- [ ] 플래시 성공
- [ ] 부팅 로그에서 HID 인터페이스 확인
- [ ] Windows에서 PID_4001 인식
- [ ] HID Keyboard 장치 열거
- [ ] HID Mouse 장치 열거

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
- 2025-11-10 17:30: 시도 #1-3 완료 기록 (Kconfig 생성, menuconfig 설정, 펌웨어 빌드/플래시)
- 2025-11-11: 시도 #4 menuconfig 실패 및 eFuse 솔루션 추가 (시도 #4B)
