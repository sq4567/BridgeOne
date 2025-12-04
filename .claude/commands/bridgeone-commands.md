# BridgeOne Development Commands Reference

BridgeOne 프로젝트의 빌드, 배포, 테스트 관련 개발 명령어 모음입니다.

---

## Android 앱 개발

### 프로젝트 디렉토리
```bash
cd src/android
```

### Gradle 명령어

#### 빌드
```bash
# Gradle 동기화
./gradlew sync

# Debug 빌드
./gradlew assembleDebug

# Release 빌드
./gradlew assembleRelease

# 클린 빌드
./gradlew clean

# 클린 후 빌드
./gradlew clean assembleDebug
```

#### 설치 및 실행
```bash
# 디바이스에 Debug APK 설치
./gradlew installDebug

# 디바이스에 Release APK 설치
./gradlew installRelease

# 설치 후 앱 실행 (ADB 사용)
adb shell am start -n com.bridgeone.app/.MainActivity
```

#### 테스트
```bash
# 유닛 테스트 실행
./gradlew test

# 계측 테스트 실행 (실제 디바이스 필요)
./gradlew connectedAndroidTest

# 특정 테스트 클래스만 실행
./gradlew test --tests "com.bridgeone.app.ExampleUnitTest"
```

#### 코드 품질
```bash
# Lint 검사
./gradlew lint

# Lint 검사 (HTML 리포트)
./gradlew lintDebug

# Ktlint 검사 (설정된 경우)
./gradlew ktlintCheck
```

#### 정보 확인
```bash
# 사용 가능한 모든 태스크 목록
./gradlew tasks

# 의존성 트리 확인
./gradlew dependencies

# 빌드 변형 목록
./gradlew :app:tasks --group="build"
```

### ADB 명령어

```bash
# 연결된 디바이스 목록
adb devices

# 앱 로그 확인 (BridgeOne 태그)
adb logcat -s BridgeOne

# 앱 강제 종료
adb shell am force-stop com.bridgeone.app

# 앱 데이터 삭제
adb shell pm clear com.bridgeone.app

# APK 설치
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 스크린샷 캡처
adb exec-out screencap -p > screenshot.png
```

---

## ESP32-S3 펌웨어 개발

### ESP-IDF 환경 설정

#### Windows
```powershell
# ESP-IDF 환경 활성화
cd <ESP-IDF 설치 경로>
.\export.bat

# 또는 ESP-IDF PowerShell 프로필 사용
# (설치 시 바로가기 생성된 경우)
```

#### Linux/macOS
```bash
# ESP-IDF 환경 활성화
source <ESP-IDF 설치 경로>/export.sh

# 또는 alias 설정 (권장)
alias get_idf='. $HOME/esp/esp-idf/export.sh'
get_idf
```

### 프로젝트 디렉토리
```bash
cd src/board/BridgeOne
```

### idf.py 명령어

#### 빌드
```bash
# 빌드
idf.py build

# 클린 빌드 (전체 재빌드)
idf.py fullclean
idf.py build

# 빌드 설정 GUI
idf.py menuconfig
```

#### 플래시
```bash
# 펌웨어 플래시 (포트 자동 감지)
idf.py flash

# 특정 포트로 플래시
idf.py -p COM3 flash          # Windows
idf.py -p /dev/ttyUSB0 flash  # Linux

# 플래시 속도 지정
idf.py -p COM3 -b 921600 flash
```

#### 모니터링
```bash
# 시리얼 모니터 (Ctrl+] 종료)
idf.py -p COM3 monitor

# 빌드 + 플래시 + 모니터 (한 번에)
idf.py -p COM3 flash monitor

# 모니터 종료: Ctrl+]
```

#### 포트 관리
```bash
# 사용 가능한 포트 목록
idf.py list-ports

# 포트 정보 상세 확인 (Windows)
# 장치 관리자 → 포트(COM & LPT)
```

#### 설정 관리
```bash
# menuconfig (TUI 설정 도구)
idf.py menuconfig

# 설정 저장/백업
cp sdkconfig sdkconfig.backup

# 설정 복원
cp sdkconfig.backup sdkconfig

# 기본 설정으로 초기화
idf.py set-target esp32s3
```

#### 디버깅
```bash
# 빌드 크기 분석
idf.py size

# 컴포넌트별 크기 분석
idf.py size-components

# 파일별 크기 분석
idf.py size-files
```

#### 컴포넌트 관리
```bash
# 컴포넌트 업데이트
idf.py reconfigure

# 관리되는 컴포넌트 업데이트
idf.py update-dependencies
```

### esptool.py 명령어

```bash
# 칩 정보 확인
esptool.py --port COM3 chip_id

# Flash 정보 확인
esptool.py --port COM3 flash_id

# Flash 전체 삭제
esptool.py --port COM3 erase_flash

# 펌웨어 백업 (4MB)
esptool.py --port COM3 read_flash 0 0x400000 backup.bin

# 펌웨어 복원
esptool.py --port COM3 write_flash 0 backup.bin

# eFuse 정보 확인
python %IDF_PATH%/components/esptool_py/esptool/espefuse.py --port COM3 summary
```

---

## Git 워크플로우 명령어

### 브랜치 관리
```bash
# 현재 브랜치 확인
git branch

# 새 기능 브랜치 생성 및 전환
git checkout -b feature/새기능이름

# main 브랜치로 전환
git checkout main

# 브랜치 삭제
git branch -d feature/완료된기능
```

### 변경사항 관리
```bash
# 상태 확인
git status

# 변경사항 확인
git diff

# 스테이징
git add .
git add 특정파일.kt

# 커밋
git commit -m "feat: 새 기능 추가"

# 커밋 히스토리
git log --oneline -10
```

### 동기화
```bash
# 원격 변경사항 가져오기
git fetch origin

# 원격 브랜치와 병합
git pull origin main

# 푸시
git push origin feature/새기능
```

---

## 유용한 조합 명령어

### Android 개발 워크플로우
```bash
# 클린 빌드 후 설치
./gradlew clean assembleDebug && ./gradlew installDebug

# 테스트 후 빌드
./gradlew test && ./gradlew assembleDebug
```

### ESP32 개발 워크플로우
```bash
# 클린 빌드 후 플래시 및 모니터
idf.py fullclean && idf.py -p COM3 flash monitor

# 빌드 후 바로 플래시
idf.py build && idf.py -p COM3 flash
```

---

## 환경 변수

### Android
```bash
# JAVA_HOME 확인
echo $env:JAVA_HOME        # PowerShell
echo $JAVA_HOME            # Bash

# ANDROID_HOME 확인
echo $env:ANDROID_HOME     # PowerShell
echo $ANDROID_HOME         # Bash
```

### ESP-IDF
```bash
# IDF_PATH 확인
echo $env:IDF_PATH         # PowerShell
echo $IDF_PATH             # Bash

# Python 환경 확인
python --version
pip list | grep esp
```
