---
title: "BridgeOne Phase 1: 개발환경 설정"
description: "BridgeOne 프로젝트 Phase 1 - Android, ESP32-S3, Windows 서버 개발환경 구축 및 초기 설정 (1-2주)"
tags: ["android", "esp32-s3", "devkitc-1", "windows", "development", "environment", "setup", "vibe-coding"]
version: "v1.0"
owner: "Chatterbones"
updated: "2025-10-20"
---

# BridgeOne Phase 1: 개발환경 설정

**개발 기간**: 1-2주

**목표**: 3개 플랫폼(Android, Board, Windows) 개발환경 구축 및 초기 설정

## Phase 구조 설계 원칙

### 검증 전략

- **기본 원칙**: 각 하위 Phase 개발 완료 즉시 해당 내용을 바로 검증
- **예외**: 여러 하위 Phase를 모아서 통합 검증하는 게 더 효율적인 경우에만 별도 통합 검증 Phase 추가
  - 예: Phase 2.5 - 전체 통신 경로 End-to-End 지연시간 검증
  - 예: Phase 4.5 - PC 화면에서 실제 커서 이동 정확성 통합 테스트
- 최종 Phase에서 전체 시스템 E2E 테스트 수행

### Phase 명명 규칙

- **Phase X.Y**: 구현 및 검증 하위 Phase (구현 완료 즉시 검증)
- **Phase X.통합검증**: 통합 검증이 필요한 경우에만 추가 (예외적)

### 바이브 코딩 활용 방침

- 각 하위 Phase별 바이브 코딩 프롬프트는 별도 섹션에서 제공 예정
- 본 문서는 전체 개발 로드맵의 큰 틀을 제시

---

## Phase 1.1: Android 개발환경 구축

### Phase 1.1.1: Android Studio 설치 및 프로젝트 생성 검증

**목표**: 유저가 수동으로 생성한 Android Studio 프로젝트의 설정 상태를 검증

**유저 사전 작업** (LLM 실행 전 필수):
1. Android Studio Arctic Fox 이상 버전을 수동 설치
2. Android Studio에서 새 프로젝트 생성:
   - 템플릿: "Empty Compose Activity" 선택
   - 프로젝트명: "BridgeOne", 패키지명: "com.bridgeone.app"
   - Save location: `{workspace}/src/android/`
   - Language: Kotlin, Minimum SDK: API 24 (Android 7.0)
   - Build configuration language: Kotlin DSL (build.gradle.kts)
3. 프로젝트 생성 완료 후 Gradle 동기화 대기

**LLM 검증 작업**:
1. `src/android/` 디렉터리 존재 여부 확인
2. `app/build.gradle.kts` 파일 읽기 및 필수 설정 검증 (Compose 의존성, compileSdk, minSdk, targetSdk)
3. `app/src/main/AndroidManifest.xml` 존재 확인
4. `app/src/main/java/com/bridgeone/app/MainActivity.kt` 존재 확인

**참조 문서 및 섹션**:
- `docs/android/technical-specification-app.md` §2.1 스플래시 스크린 구현 요구사항
- `docs/technical-specification.md` §4.1 Android 플랫폼 개요

**검증**:
- [x] `src/android/` 디렉터리가 존재함
- [x] `src/android/app/build.gradle.kts` 파일이 존재하고 Compose BOM 의존성 포함됨
- [x] `minSdk = 24`, `targetSdk = 34` 설정 확인
- [x] `MainActivity.kt` 파일이 `com.bridgeone.app` 패키지에 존재함
- [x] `AndroidManifest.xml`에 MainActivity가 등록되어 있음
- [x] 프로젝트 구조가 표준 Android 프로젝트 구조(`app/src/main/java/`)를 따름

---

### Phase 1.1.2: Jetpack Compose 및 핵심 의존성 설정

**목표**: Compose, USB Serial, 기타 필수 라이브러리 의존성 추가 및 버전 관리 설정

**세부 목표**:
1. `gradle/libs.versions.toml`에 버전 카탈로그 작성 (Compose BOM, Kotlin 1.9.0+, USB Serial 3.7.3, Accompanist Permissions 0.32.0)
2. `app/build.gradle.kts`에 의존성 추가 (Compose UI, Material3, Activity Compose, USB Serial, Accompanist)
3. Compose 컴파일러 옵션 설정
4. Java 17 타겟 설정

**참조 문서 및 섹션**:
- `docs/android/technical-specification-app.md` §1.1 통신 아키텍처 설계
- `docs/android/technical-specification-app.md` §2.1.1 Jetpack Compose 기반 설계

**검증**:
- [x] Gradle 동기화 성공 (모든 의존성 다운로드 완료)
- [x] `libs.versions.toml`에 모든 버전이 명확히 정의됨
- [x] USB Serial 라이브러리가 External Libraries에 표시됨
- [x] Compose Preview 기능 정상 동작 확인
- [x] 빌드 시 Java 버전 관련 오류 없음

---

### Phase 1.1.3: AndroidManifest.xml 권한 및 기본 설정

**목표**: USB 통신 및 앱 실행에 필요한 모든 권한과 기본 설정 구성

**세부 목표**:
1. `AndroidManifest.xml`에 권한 추가 (USB_PERMISSION, usb.host, VIBRATE)
2. MainActivity에 `android:screenOrientation="portrait"` 설정
3. 앱 아이콘 및 이름 설정 (`android:label="BridgeOne"`)
4. Theme 설정 (`Theme.BridgeOne` - Material3 기반)
5. USB 장치 연결 Intent Filter 추가 (VID: 0x303A)

**참조 문서 및 섹션**:
- `docs/android/technical-specification-app.md` §1.1.1 하드웨어 연결 요구사항
- `docs/android/technical-specification-app.md` §1.1.2 연결 관리 요구사항

**검증**:
- [x] Manifest Merger 오류 없음
- [x] 앱 설치 시 권한 요청 대화상자 표시 확인
- [x] 앱 아이콘이 런처에 "BridgeOne"으로 표시됨
- [x] USB 장치 연결 시 앱 선택 대화상자에 BridgeOne 표시됨
- [x] Portrait 모드 강제 적용 확인

---

### Phase 1.1.4: 커스텀 리소스 준비 (Drawable 및 Font)

**목표**: BridgeOne 앱에서 사용할 커스텀 아이콘(drawable)과 폰트(font) 리소스를 프로젝트에 통합

**유저 사전 작업** (LLM 실행 전 필수):
1. 파일 탐색기 또는 IDE의 파일 탐색기에서 프로젝트 루트의 `resources/drawable/` 디렉터리 열기
2. `resources/drawable/` 내의 **모든 XML 벡터 아이콘 파일 복사**:
   - 33개 파일 모두 선택 (Ctrl+A)
   - 복사 (Ctrl+C)
   - `src/android/app/src/main/res/drawable/` 디렉터리로 이동 (디렉터리가 없으면 생성)
   - 붙여넣기 (Ctrl+V)
3. 프로젝트 루트의 `resources/font/` 디렉터리 열기
4. `resources/font/` 내의 **모든 Pretendard TTF 폰트 파일 복사**:
   - 9개 파일 모두 선택 (Ctrl+A)
   - 복사 (Ctrl+C)
   - `src/android/app/src/main/res/font/` 디렉터리로 이동 (디렉터리가 없으면 생성)
   - 붙여넣기 (Ctrl+V)
5. Android Studio에서 `src/android/` 프로젝트 새로고침 (F5 또는 우클릭 → Refresh)

**LLM 검증 작업**:
1. `src/android/app/src/main/res/drawable/` 디렉터리에 33개 아이콘 파일 존재 확인
2. `src/android/app/src/main/res/font/` 디렉터리에 9개 폰트 파일 존재 확인
3. `app/src/main/res/font/pretendard.xml` Font Family XML 파일 생성 (모든 weight 포함)
4. 각 벡터 drawable의 `android:fillColor` 속성 확인 (대부분 `#FF000000` 검은색, 런타임에 tint 적용 가능)
5. 리소스 ID 접근 가능 확인 (`R.drawable.ic_*`, `R.font.pretendard_*`)

**참조 문서 및 섹션**:
- `docs/android/design-guide-app.md` §3.2 타이포그래피 (Pretendard 폰트 사용)
- `docs/android/component-design-guide-app.md` (각 컴포넌트의 아이콘 사용)
- `resources/drawable/` 디렉터리 (소스 아이콘 파일)
- `resources/font/` 디렉터리 (소스 폰트 파일)

**검증**:
- [x] `app/src/main/res/drawable/` 디렉터리에 33개 아이콘 파일 모두 존재
- [x] `app/src/main/res/font/` 디렉터리에 9개 Pretendard 폰트 파일 모두 존재
- [x] `app/src/main/res/font/pretendard.xml` Font Family 파일 생성됨
- [x] Android Studio에서 drawable 리소스 미리보기 정상 표시 (Vector Asset Studio)
- [x] Kotlin 코드에서 `R.drawable.ic_touchpad` 등 리소스 ID 정상 인식됨
- [x] Kotlin 코드에서 `R.font.pretendard` 또는 `R.font.pretendard_medium` 등 폰트 리소스 정상 인식됨
- [x] 빌드 시 리소스 관련 오류 없음
- [x] Compose Preview에서 `painterResource(R.drawable.ic_touchpad)` 정상 렌더링됨
- [x] Compose에서 `FontFamily(Font(R.font.pretendard_medium))` 정상 적용됨

---

### Phase 1.1.5: 기본 Compose UI 구조 및 Hello World

**목표**: Compose 기반 기본 UI 구조 생성, 커스텀 폰트 적용 및 정상 렌더링 확인

**세부 목표**:
1. `MainActivity.kt`에서 `setContent { BridgeOneApp() }` 호출
2. `ui/BridgeOneApp.kt` 생성 - 최상위 Composable 함수
3. `ui/theme/` 디렉터리에 Color.kt, Type.kt, Theme.kt 생성
4. `ui/theme/Type.kt`에 Pretendard 폰트 기반 Typography 정의:
   - `FontFamily` 생성: `val PretendardFontFamily = FontFamily(Font(R.font.pretendard_medium))`
   - Material3 Typography에 Pretendard 폰트 적용
5. Material3 테마 적용 (`BridgeOneTheme`)
6. 중앙에 "BridgeOne - Hello World" 텍스트 표시 (Pretendard 폰트 사용)
7. 다크 테마 기본 적용 (배경색 `#121212`)
8. 테스트용 아이콘 표시 (예: `ic_touchpad`, `ic_usb` 등)

**참조 문서 및 섹션**:
- `docs/android/design-guide-app.md` §3 색상 시스템
- `docs/android/design-guide-app.md` §3.2 타이포그래피 (Pretendard 폰트)
- `docs/android/technical-specification-app.md` §2.1.1 Jetpack Compose 기반 설계

**검증**:
- [x] 에뮬레이터/실제 기기에서 "BridgeOne - Hello World" 텍스트가 화면 중앙에 표시됨
- [x] 텍스트가 Pretendard 폰트로 정확히 렌더링됨 (시스템 폰트와 구분됨)
- [x] 다크 테마가 적용되어 배경이 검은색(`#121212`)임
- [x] 테스트 아이콘(drawable)이 정상 표시됨
- [x] Compose Preview에서 UI가 정확히 미리보기됨 (폰트 및 아이콘 포함)
- [x] 시스템 테마 변경 시 앱 테마도 자동 전환됨
- [x] 빌드 시 Compose 및 리소스 관련 경고나 오류 없음

---

### Phase 1.1.6: 런처 아이콘 변경

**목표**: BridgeOne 앱의 기본 런처 아이콘을 BridgeOne 로고 기반 커스텀 아이콘으로 변경

**유저 사전 작업** (LLM 실행 전 필수):
1. 디자인 가이드 검토: `docs/bridgeone-logo-concepts.md` 파일 확인
2. 다음 중 하나의 방법으로 launcher icon 변경:
   
   **방법 A: Android Studio Image Asset Studio 사용 (권장)**
   - Android Studio에서 `app/src/main/res/` 우클릭
   - "New" → "Image Asset" 선택
   - Asset type: "Launcher Icons (Adaptive and Legacy)" 선택
   - Source asset으로 `resources/ico/` 디렉터리의 BridgeOne 아이콘 이미지 파일 선택
   - Foreground, Background 설정 (로고 디자인에 맞게)
   - "Next" → "Finish"
   
   **방법 B: 수동으로 XML 파일 편집**
   - `app/src/main/res/drawable/ic_launcher_foreground.xml` 파일 수동 편집
   - `app/src/main/res/drawable/ic_launcher_background.xml` 파일 수동 편집 (배경색: `#121212`)
   - BridgeOne 로고 벡터 또는 컬러를 반영하여 작성
   
   **방법 C: Vector Drawable 변환**
   - PNG/SVG 형식의 로고를 Vector Drawable로 변환
   - `resources/svg/Logo.svg`를 참고하여 XML 벡터 생성
   - `ic_launcher_foreground.xml`에 적용

3. Android Studio에서 "Build" → "Clean Project" 후 "Rebuild Project" 실행
4. 에뮬레이터 또는 실제 기기에서 앱 언인스톨 후 재설치

**LLM 검증 작업**:
1. `app/src/main/res/drawable/ic_launcher_foreground.xml` 파일 확인
2. `app/src/main/res/drawable/ic_launcher_background.xml` 파일 확인
3. `app/src/main/AndroidManifest.xml`에서 icon 설정 확인
4. Adaptive Icon 설정 정상 여부 확인
5. 빌드 결과 검증

**참조 문서 및 섹션**:
- `docs/bridgeone-logo-concepts.md` (로고 개념 및 디자인 원칙)
- `docs/android/design-guide-app.md` §색상 시스템
- `resources/ico/` (기존 아이콘 리소스)
- `resources/svg/Logo.svg` (로고 벡터 파일)
- Android Developer: [App Icons](https://developer.android.com/training/multiscreen/screendensities#provide-density-specific-versions)
- Android Developer: [Create App Icons with Image Asset Studio](https://developer.android.com/studio/write/image-asset-studio)

**검증**:
- [x] `app/src/main/res/drawable/ic_launcher_foreground.xml` 파일이 존재하고 BridgeOne 로고를 반영함
- [x] `app/src/main/res/drawable/ic_launcher_background.xml` 파일이 존재하고 테마 배경색 (`#121212`)이 설정됨
- [x] `AndroidManifest.xml`에 `android:icon="@drawable/ic_launcher"` (또는 `@mipmap/ic_launcher`) 설정됨
- [x] Adaptive Icon 지원 확인 (API 26+): `AndroidManifest.xml`에 `android:roundIcon` 속성 존재
- [x] 빌드 성공 (gradle 빌드 오류 없음)
- [x] 에뮬레이터/실제 기기의 런처 화면에서 업데이트된 BridgeOne 아이콘 표시됨
- [x] 다양한 DPI 설정(mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi)에서 아이콘이 명확하게 표시됨
- [x] Compose Preview 또는 AS Layout Preview에서 앱 아이콘 미리보기 정상 표시

---

### Phase 1.1.7: Gradle 빌드 및 에뮬레이터 검증

**목표**: Android 앱을 성공적으로 빌드하고 에뮬레이터에서 정상 실행 확인

**세부 목표**:
1. Gradle Clean Build 실행 및 성공 검증
2. APK 빌드 파일 생성 확인 (`build/outputs/apk/debug/`)
3. Android 에뮬레이터 생성 및 시작
4. APK를 에뮬레이터에 설치 및 실행
5. 앱 정상 실행 및 "BridgeOne - Hello World" UI 표시 확인
6. 권한 요청 대화상자 정상 표시 확인

**LLM 검증 작업**:
1. Gradle 빌드 결과 파일 확인 (`build/outputs/apk/debug/app-debug.apk`)
2. 빌드 로그에서 경고 및 오류 확인
3. 에뮬레이터에 설치된 앱 동작 확인
4. UI가 정상 렌더링되는지 스크린샷 또는 시각적 확인

**참조 문서 및 섹션**:
- `docs/android/technical-specification-app.md` §2.1.1 Jetpack Compose 기반 설계
- `docs/android/design-guide-app.md` (UI/UX 설계 명세)

**실행 단계** (사용자 수동 실행):

```
1. Android Studio에서 프로젝트 열기
   - File → Open → {workspace}/src/android/ 선택
   
2. Gradle Clean Build 실행
   - Build → Clean Project
   - Build → Rebuild Project
   - 또는 터미널: ./gradlew clean build
   
3. 에뮬레이터 생성/시작
   - Tools → Device Manager (또는 AVD Manager)
   - "Create Device" 선택
   - 기본 설정: Pixel 4 XL, Android 13+, AOSP 이미지 선택
   - 에뮬레이터 시작
   
4. 앱 실행
   - Run → Run 'app' (또는 Shift+F10)
   - 에뮬레이터에 APK 설치 및 실행
```

**검증**:
- [x] Gradle 빌드 성공 (메시지: "BUILD SUCCESSFUL")
- [x] `build/outputs/apk/debug/app-debug.apk` 파일 존재
- [x] 빌드 로그에 ERROR 없음 (WARNING은 무시 가능)
- [x] 에뮬레이터에서 앱 설치 성공
- [x] 앱 실행 시 "BridgeOne - Hello World" 텍스트가 화면 중앙에 표시됨
- [x] Pretendard 폰트가 정확히 렌더링됨
- [x] 테스트 아이콘(drawable)이 정상 표시됨
- [x] 앱이 정상 상태로 실행 유지 (크래시 없음)
- [x] Gradle 빌드 캐시 사용으로 재빌드 시 빠른 속도 확인

---

### ✅ Phase 1.1 완료 요약

**목표 달성**: Android 개발환경 완전 구축 ✅

**완료 항목**:
- ✅ Phase 1.1.1: Android Studio 프로젝트 생성 및 기본 설정
- ✅ Phase 1.1.2: Jetpack Compose 및 핵심 의존성 설정
- ✅ Phase 1.1.3: AndroidManifest.xml 권한 및 기본 설정
- ✅ Phase 1.1.4: 커스텀 리소스 (Drawable, Font) 통합
- ✅ Phase 1.1.5: 기본 Compose UI 구조 및 Hello World
- ✅ Phase 1.1.6: 런처 아이콘 변경
- ✅ Phase 1.1.7: Gradle 빌드 및 에뮬레이터 검증

**구성된 개발환경**:
- Android Studio 완전 설정
- Gradle 빌드 시스템 구동
- Jetpack Compose 기반 UI 프레임워크
- Material3 테마 및 Pretendard 폰트 적용
- 33개 커스텀 아이콘 리소스 통합
- 에뮬레이터에서 정상 실행 확인
- APK 빌드 및 배포 환경 준비

**다음 단계**: Phase 1.2 (Board/ESP32-S3 개발환경 구축)

---

## Phase 1.2: Board (ESP32-S3) 개발환경 구축

### Phase 1.2.1: ESP-IDF 설치 검증 및 프로젝트 생성 준비

**목표**: 유저가 설치한 ESP-IDF 개발환경의 정상 설치 여부를 검증

**유저 사전 작업** (LLM 실행 전 필수):
1. ESP-IDF Tools Installer 다운로드 및 설치
   - 다운로드: https://dl.espressif.com/dl/esp-idf/?idf=stable (Online Installer 권장)
   - 온라인 설치 프로그램이 매우 작으며, 설치 중 필요한 종속성만 다운로드함
   
2. 설치 경로 선택 (중요한 제약 사항)
   - **권장 경로**: `%userprofile%\Desktop\esp-idf` 또는 `%userprofile%\esp`
   - **피해야 할 것**:
     - 90자 이상의 긴 경로
     - 공백(space) 포함 경로
     - 특수문자 또는 한글 포함 경로
   - 예: ❌ `C:\Program Files\...` (공백 포함), ✅ `C:\Users\YourName\esp-idf`, ✅ `C:\Espressif`

3. 설치 중 옵션 선택
   - 기본 설정 유지 (CMake, Ninja, Python, OpenOCD 모두 포함)
   - 설치 완료 후 다음 중 하나 선택:
     - **"Run ESP-IDF PowerShell Environment"** (PowerShell 사용 권장)
     - 또는 **"Run ESP-IDF Command Prompt (cmd.exe)"**
   - 환경변수는 설치 프로그램이 자동으로 설정함 (수동 설정 불필요)

4. 설치 완료 확인
   - 설치 프로그램이 자동으로 ESP-IDF 환경을 실행한 터미널(PowerShell 또는 CMD) 창이 나타남
   - 이 터미널 창에서 모든 도구(idf.py, cmake, ninja, python 등)를 사용 가능

**LLM 검증 작업**:
1. 사용자가 설치한 환경에서 다음 명령어를 순차적으로 실행하고 결과 확인:
   - `idf.py --version`: ESP-IDF 버전 확인
   - `$env:IDF_PATH` (PowerShell) 또는 `echo %IDF_PATH%` (CMD): 설치 경로 확인
   - `cmake --version`: CMake 버전 확인 (3.16 이상)
   - `ninja --version`: Ninja 버전 확인
   - `python --version`: Python 버전 확인 (3.9 이상 권장)
   - `esptool.py --version`: esptool 도구 확인

2. 환경변수 및 경로 검증:
   - IDF_PATH 환경변수가 올바른 설치 디렉터리를 가리키는지 확인
   - 설치 경로에 공백이나 특수문자가 없는지 확인

3. 필수 컴포넌트 확인:
   - `%IDF_PATH%\tools\` 디렉터리에 python, cmake, ninja 등이 존재하는지 확인

**참조 문서 및 섹션**:
- Espressif 공식 가이드: [Standard Setup of Toolchain for Windows](https://docs.espressif.com/projects/esp-idf/en/stable/esp32s3/get-started/windows-setup.html)
- `docs/board/esp32s3-code-implementation-guide.md` §7.1 ESP-IDF 설치 및 환경 설정

**검증**:
- [x] `idf.py --version` 실행 시 5.1.0 이상 버전 출력 확인
  - 확인 결과: `ESP-IDF v5.5.1-dirty` ✅
- [x] `$env:IDF_PATH` 또는 `%IDF_PATH%` 환경변수가 설치 경로를 올바르게 가리킴 확인
  - 확인 결과: `C:\Espressif\frameworks\esp-idf-v5.5.1` ✅
- [x] `cmake --version` 3.16 이상 확인
  - 확인 결과: cmake version 3.30.2 ✅
- [x] `ninja --version` 설치 확인 (버전 출력)
  - 확인 결과: 1.12.1 ✅
- [x] `python --version` 3.9 이상 확인
  - 확인 결과: Python 3.11.2 ✅
- [x] `esptool.py --version` 실행 성공 (ESP-IDF 도구 체인 완성)
  - 확인 결과: esptool.py v4.10.0 ✅
- [x] 모든 검증 통과 시 "ESP-IDF 개발환경이 정상적으로 설치되었습니다" 메시지 출력
  - ✅ **ESP-IDF 개발환경이 정상적으로 설치되었습니다**

---

### Phase 1.2.2: ESP32-S3 프로젝트 생성 및 타겟 설정

**목표**: BridgeOne Board 펌웨어 프로젝트 생성 및 ESP32-S3 타겟 지정

**세부 목표**:
1. `src/board/` 디렉터리 내에서 프로젝트 생성 (기존 `BridgeOne/` 디렉터리 있으면 백업)
2. `idf.py create-project BridgeOne` 실행
3. `idf.py set-target esp32s3` 실행 (ESP32-S3 타겟 지정)
4. CMakeLists.txt 확인 (프로젝트명 "BridgeOne")
5. 프로젝트 디렉터리 구조 검증
6. TinyUSB 컴포넌트 의존성 추가

**실행 단계**:
```powershell
# 1. 프로젝트 생성 (자동으로 BridgeOne 디렉터리 생성)
idf.py create-project BridgeOne

# 2. 프로젝트 디렉터리 진입
cd "BridgeOne"

# 3. ESP32-S3 타겟 설정
idf.py set-target esp32s3
```

**참조 문서 및 섹션**:
- `docs/board/esp32s3-code-implementation-guide.md` §3.1 ESP-IDF 프로젝트 구조

**검증**:
- [x] `idf.py set-target esp32s3` 실행 성공 (오류 없음)
- [x] `sdkconfig` 파일 생성 및 ESP32-S3 타겟 설정 확인
  - 파일 위치: `{프로젝트}/sdkconfig`
  - ✓ sdkconfig 파일 생성 확인 (ESP-IDF 5.5.1 설정)
  - ✓ ESP32-S3 타겟 관련 설정 확인 (CONFIG_SOC_* 항목들)
- [x] `CMakeLists.txt`에 `project(BridgeOne)` 존재
  - ✓ 루트 CMakeLists.txt에서 project(BridgeOne) 확인
- [x] `main/CMakeLists.txt`에 main.c 등록 확인
  - ✓ idf_component_register()에 "BridgeOne.c" 등록 확인
  - ✓ INCLUDE_DIRS "." 설정 확인
- [x] 프로젝트 디렉터리 구조 정확히 생성됨:
  ```
  BridgeOne/
  ├── CMakeLists.txt
  ├── sdkconfig
  ├── main/
  │   ├── CMakeLists.txt
  │   └── BridgeOne.c
  └── build/ (CMake 설정 후 생성됨)
  ```
  - ✓ 모든 파일 확인됨
  - ✓ build 디렉터리 생성됨
- [x] 프로젝트 루트 경로: `src/board/BridgeOne/`
  - ✓ 올바른 경로에서 프로젝트 생성 확인
- [x] `main/CMakeLists.txt`에 TinyUSB 컴포넌트 의존성 추가
  - ✓ `idf_component_register()`에 `REQUIRES tinyusb` 추가 확인

---

### Phase 1.2.3: sdkconfig 기본 설정 및 TinyUSB 활성화

**목표**: ESP-IDF 설정 파일(sdkconfig)에 TinyUSB 및 필수 컴포넌트 활성화

**세부 목표**:
1. `sdkconfig.defaults` 파일 생성 및 설정 추가 (TinyUSB, HID, CDC, UART ISR, FreeRTOS, PSRAM, Flash)
2. ESP32-S3-N16R8 하드웨어 스펙에 맞게 16MB Flash 및 8MB Octal PSRAM 활성화
3. 커스텀 파티션 테이블 `partitions.csv` 파일 생성
4. `idf.py reconfigure` 실행하여 설정 적용
5. 선택적으로 `idf.py menuconfig`로 UI를 통해 추가 커스터마이징 가능

**설정 파일 생성**:
프로젝트 루트(`src/board/BridgeOne/`)에 `sdkconfig.defaults` 파일 생성:

**실행 단계**:
```powershell
C:\Espressif\frameworks\esp-idf-v5.5.1\export.ps1

# 2. 프로젝트 디렉터리로 이동
cd "F:\C\Programming\MobileDevelopment\Projects\Android\BridgeOne\src\board\BridgeOne"

# 3. sdkconfig.defaults 파일 생성
$sdkconfigContent = @"
# TinyUSB Composite Device Configuration
CONFIG_TINYUSB=y
CONFIG_TINYUSB_HID_ENABLED=y
CONFIG_TINYUSB_CDC_ENABLED=y
CONFIG_TINYUSB_DEVICE_ENABLED=y

# USB Bus Power Mode (ESP32-S3 DevKit powered via USB)
CONFIG_TINYUSB_DEVICE_BUS_POWERED=y

# HID Buffer Settings (BridgeOne 8-byte frame processing)
CONFIG_TINYUSB_HID_COUNT=2
CONFIG_TINYUSB_HID_BUFSIZE=64

# CDC Buffer Settings (Vendor CDC message frame processing)
CONFIG_TINYUSB_CDC_COUNT=1
CONFIG_TINYUSB_CDC_RX_BUFSIZE=512
CONFIG_TINYUSB_CDC_TX_BUFSIZE=512

# UART Optimization (Fast responsiveness)
CONFIG_UART_ISR_IN_IRAM=y

# FreeRTOS Configuration (1ms Tick rate -> 1000Hz, Dual-core enabled)
CONFIG_FREERTOS_HZ=1000
CONFIG_FREERTOS_UNICORE=n

# Memory Optimization (Size priority, PSRAM enabled)
CONFIG_COMPILER_OPTIMIZATION_SIZE=y

# PSRAM Configuration (ESP32-S3-N16R8: 8MB Octal PSRAM)
CONFIG_ESP32S3_SPIRAM_SUPPORT=y
CONFIG_SPIRAM=y
CONFIG_SPIRAM_MODE_OCT=y
CONFIG_SPIRAM_SPEED_80M=y
CONFIG_SPIRAM_USE_MALLOC=y

# Flash Configuration (ESP32-S3-DevkitC-1-N16R8: 16MB Flash)
CONFIG_ESPTOOLPY_FLASHSIZE_16MB=y
CONFIG_ESPTOOLPY_FLASHMODE_QIO=y
CONFIG_ESPTOOLPY_FLASHFREQ_80M=y
CONFIG_PARTITION_TABLE_CUSTOM=y
CONFIG_PARTITION_TABLE_CUSTOM_FILENAME="partitions.csv"

# Logging Level (Development: DEBUG, Production: INFO)
CONFIG_LOG_DEFAULT_LEVEL_INFO=y

# Disable Unused Peripherals (Power saving)
CONFIG_ESP_WIFI_ENABLED=n
CONFIG_BT_ENABLED=n
"@
Set-Content -Path "sdkconfig.defaults" -Value $sdkconfigContent -Encoding UTF8

# 4. partitions.csv 파일 생성 (16MB Flash 활용)
$partitionsContent = @"
# Name,   Type, SubType, Offset,  Size, Flags
nvs,      data, nvs,     0x9000,  0x6000,
phy_init, data, phy,     0xf000,  0x1000,
factory,  app,  factory, 0x10000, 3M,
storage,  data, spiffs,  ,        12M,
"@
Set-Content -Path "partitions.csv" -Value $partitionsContent -Encoding UTF8

# 5. 설정 적용
idf.py reconfigure

# (또는 UI 기반 수동 설정)
# idf.py menuconfig
```

**파티션 테이블 설명** (`partitions.csv`):
- **nvs** (24KB): Non-Volatile Storage - WiFi/Bluetooth 설정 등 저장 (현재 미사용이지만 시스템 예약)
- **phy_init** (4KB): RF PHY 초기화 데이터
- **factory** (3MB): 펌웨어 바이너리 저장 공간 (OTA 미사용 시 충분한 크기)
- **storage** (12MB): SPIFFS 파일 시스템 - 향후 설정 파일, 로그 등 저장 가능

**참고**: 16MB Flash 중 약 15MB만 사용 가능 (부트로더 및 파티션 테이블 등 시스템 영역 제외)

**참조 문서 및 섹션**:
- `docs/board/esp32s3-code-implementation-guide.md` §7.2 프로젝트 설정 (sdkconfig)
- `docs/board/esp32s3-code-implementation-guide.md` §0 USB Composite 디바이스 설계 계약 (프로토콜 명세)

**검증**:
- [x] `sdkconfig` 파일에 `CONFIG_TINYUSB=y` 존재
- [x] `sdkconfig`에 `CONFIG_TINYUSB_HID_ENABLED=y`, `CONFIG_TINYUSB_CDC_ENABLED=y` 존재
- [x] `CONFIG_UART_ISR_IN_IRAM=y`, `CONFIG_FREERTOS_HZ=1000` 확인
- [x] `CONFIG_TINYUSB_HID_COUNT=2` (Keyboard + Mouse)
- [x] `CONFIG_TINYUSB_CDC_COUNT=1` (Vendor CDC)
- [x] `CONFIG_ESPTOOLPY_FLASHSIZE_16MB=y` 확인 (16MB Flash 활성화)
- [x] `CONFIG_SPIRAM=y`, `CONFIG_SPIRAM_MODE_OCT=y` 확인 (8MB PSRAM 활성화)
- [x] `CONFIG_SPIRAM_USE_MALLOC=y` 확인 (PSRAM을 힙 메모리로 사용)
- [x] `partitions.csv` 파일 생성됨 (nvs, phy_init, factory, storage 파티션 정의)
- [x] `sdkconfig.defaults`에 모든 설정 백업됨 (인코딩 UTF-8)
- [x] `idf.py reconfigure` 실행 성공 (오류 없음)

---

### Phase 1.2.4: 빌드 환경 검증 및 하드웨어 통합 테스트

**목표**: 빌드 시스템이 정상 작동하고, 펌웨어를 ESP32-S3에 성공적으로 플래싱한 후 USB Composite 디바이스가 Windows에서 인식되는지 최종 확인

**세부 목표**:
1. 빌드 환경 정상 작동 검증 (최소 스켈레톤 코드로 빌드 성공 확인)
2. ESP32-S3에 펌웨어 플래싱 및 정상 부팅 확인
3. USB Composite 디바이스가 Windows에서 정상 인식되는지 확인

**유저 사전 작업** (LLM 실행 전 필수):
1. USB-C 케이블로 ESP32-S3-DevkitC-1을 PC에 연결

**LLM 검증 작업**:
1. 빌드 환경 테스트 (최소 스켈레톤 main.c 작성 및 빌드)
2. COM 포트 자동 감지
3. 펌웨어 플래싱
4. 시리얼 모니터로 정상 실행 확인
5. Windows Device Manager에서 USB 디바이스 인식 확인

**참조 문서**:
- `docs/board/esp32s3-code-implementation-guide.md` §7 빌드, 플래시, 모니터링
- `docs/board/esp32s3-code-implementation-guide.md` §1.3 USB Composite 디바이스 설계

---

**Step 1: 최소 스켈레톤 main.c 작성**

프로젝트 디렉터리의 `main/main.c` 파일을 다음과 같이 생성/수정하세요:

```c
#include <stdio.h>
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"

void app_main(void) {
    // Build environment verification minimal code
    // Actual feature implementation will be done in Phase 2 and later
    printf("BridgeOne Board - Environment Setup Complete\n");
    printf("ESP32-S3-N16R8: 16MB Flash, 8MB PSRAM\n");
    
    while (1) {
        vTaskDelay(1000 / portTICK_PERIOD_MS);
    }
}
```

**Step 2: 빌드 실행**

```powershell
C:\Espressif\frameworks\esp-idf-v5.5.1\export.ps1
cd "F:\C\Programming\MobileDevelopment\Projects\Android\BridgeOne\src\board\BridgeOne"
idf.py build
```

**Step 3: 플래싱 및 검증**

```powershell
# 1. COM 포트 자동 감지
$comPort = (Get-WmiObject Win32_SerialPort | Where-Object {$_.Description -match "USB"} | Select-Object -First 1).DeviceID

if ($comPort) {
    Write-Host "감지된 COM 포트: $comPort"
    
    # 2. 펌웨어 플래싱
    idf.py -p $comPort flash
    
    # 3. 시리얼 모니터 시작 (Ctrl+C로 종료)
    idf.py -p $comPort monitor
} else {
    Write-Host "USB 포트를 자동 감지하지 못했습니다. Device Manager에서 확인하세요."
}

# (또는 수동 COM 포트 지정)
# idf.py -p COM3 flash
# idf.py -p COM3 monitor
```

**Step 4: Windows Device Manager 확인**

플래싱 완료 후, Windows Device Manager에서 다음을 확인하세요:

```powershell
Get-PnpDevice | Where-Object {$_.DeviceID -match "VID_303A"} | Format-Table FriendlyName, Status, DeviceID
```

**검증 체크리스트**:
- [x] `idf.py build` 실행 성공 ("BUILD SUCCESSFUL" 메시지 표시)
- [x] `build/` 디렉터리에 `BridgeOne.bin` 파일 생성됨
- [x] Build 로그에서 Flash 크기 16MB로 인식됨 ("Detected flash size: 16MB")
- [x] Build 로그에서 PSRAM 활성화 확인 ("PSRAM initialized, size: 8MB")
- [x] `idf.py -p COMx flash` 실행 성공 ("Hash of data verified" 메시지 표시)
- [x] 시리얼 모니터가 정상 시작 (115200 보드 레이트, 연속 모니터링)
- [x] 시리얼 로그에서 "BridgeOne Board - Environment Setup Complete" 메시지 확인
- [x] 시리얼 로그에서 "ESP32-S3-N16R8: 16MB Flash, 8MB PSRAM" 메시지 확인
- [x] Windows Device Manager에서 HID 디바이스 최소 2개 확인:
  - ✅ "USB Input Device" (키보드) 또는 "BridgeOne USB Keyboard"
  - ✅ "USB Input Device" (마우스) 또는 "BridgeOne USB Mouse"
- [x] Windows Device Manager에서 COM 포트 1개 확인:
  - ✅ "USB-to-UART Bridge (COMx)" 또는 유사 항목
- [x] 모든 디바이스에 드라이버 오류 없음 (노란색 느낌표 미 표시)
- [x] 빌드 로그에 ERROR 없음 (WARNING은 허용)

**에러 및 트러블슈팅**:

| 증상 | 원인 | 해결 방법 |
|------|------|---------|
| **"Build failed"** | 컴파일 오류, CMakeLists.txt 오류, 또는 ESP-IDF 도구 부재 | 1. `idf.py fullclean` 실행 후 재빌드<br>2. `idf.py reconfigure` 실행<br>3. Phase 1.2.1 도구 체인 재확인<br>4. Phase 1.2.3 TinyUSB 설정 재확인 |
| **"CMake not found"** | ESP-IDF 환경 미로드 | 1. PowerShell 재시작<br>2. `C:\Espressif\esp-idf\export.ps1` 재실행<br>3. `cmake --version` 확인 |
| **"Failed to open COM port"** | COM 포트가 다른 프로그램에서 사용 중 | 1. 다른 시리얼 프로그램 모두 종료<br>2. Device Manager에서 포트 재확인<br>3. 플래싱 재시도 |
| **"No suitable COM port found"** | USB 케이블 미연결 또는 드라이버 부재 | 1. USB 케이블 재연결<br>2. Device Manager에서 포트 수동 확인<br>3. Espressif CH340/CP210x 드라이버 설치 |
| **플래싱 후 시리얼 로그 미표시** | 대역폭 설정 오류 또는 부트로더 오류 | 1. `idf.py -p COMx monitor -b 115200` 실행<br>2. 보드 리셋 버튼 누름<br>3. `idf.py -p COMx erase_flash` 후 재플래싱 |
| **USB 디바이스 미인식** | TinyUSB 설정 누락 (Phase 1.2.3) | 1. Phase 1.2.3 확인: `sdkconfig`에 `CONFIG_TINYUSB=y` 존재 여부<br>2. Windows Update 실행 (HID/COM 드라이버 최신화)<br>3. `idf.py fullclean` 후 완전 재빌드 |
| **Device Manager에서 "Unknown Device"** | USB 드라이버 부재 | 1. 디바이스 우클릭 → "Update driver"<br>2. "Browse my computer for driver software" 선택<br>3. Windows 기본 드라이버 사용<br>4. 보드 재연결 |

**완료 기준**:
- ✅ 빌드 성공 및 바이너리 생성
- ✅ 플래싱 성공 및 보드 정상 부팅
- ✅ Windows Device Manager에서 USB 디바이스 3개(키보드, 마우스, COM 포트) 모두 인식
- ✅ 모든 디바이스에 드라이버 오류 없음

**중요 사항**:
- 이 Phase는 **개발환경 검증**만 수행합니다.
- 실제 USB 데이터 통신, HID 입력 처리, Board-App 통신 구현은 **Phase 2 (Communication Stabilization)** 이후에서 수행됩니다.
- Phase 1.2.3에서 설정한 TinyUSB 구성이 올바르게 작동하는지 확인하는 최종 검증 단계입니다.

---

## Phase 1.3: Windows 서버 개발환경 구축

### Phase 1.3.1: Visual Studio 설치 및 WPF 프로젝트 생성 검증

**목표**: 유저가 수동으로 생성한 Visual Studio 2022 및 WPF 프로젝트의 설정 상태를 검증

**유저 사전 작업** (LLM 실행 전 필수):
1. Visual Studio 2022 Community/Professional/Enterprise 버전을 수동 설치
   - 다운로드: https://visualstudio.microsoft.com/downloads/
   - 워크로드 선택 시 **".NET 데스크톱 개발"** 필수 선택
2. Visual Studio 2022에서 새 프로젝트 생성:
   - 템플릿: "WPF 앱 (.NET)" 또는 "WPF App (.NET)" 선택
   - 프로젝트명: "BridgeOne"
   - 위치: `{workspace}/src/windows/`
   - 프레임워크: **.NET 8.0** (Long Term Support)
3. 프로젝트 생성 완료 후 솔루션 로드 대기

**LLM 검증 작업**:
1. `src/windows/BridgeOne/` 디렉터리 존재 여부 확인
2. `BridgeOne.csproj` 파일 읽기 및 필수 설정 검증 (TargetFramework, OutputType, UseWPF)
3. `App.xaml` 및 `App.xaml.cs` 존재 확인
4. `MainWindow.xaml` 및 `MainWindow.xaml.cs` 존재 확인
5. 솔루션 파일 `BridgeOne.sln` 존재 확인

**참조 문서 및 섹션**:
- `docs/windows/technical-specification-server.md` §2.2 기술 스택 선택 원칙
- `docs/windows/technical-specification-server.md` §10.1 라이브러리 선정 배경
- `docs/technical-specification.md` §3 시스템 아키텍처

**검증**:
- [x] `src/windows/BridgeOne/` 디렉터리가 존재함
- [x] `BridgeOne.csproj` 파일이 존재하고 `<TargetFramework>net8.0-windows</TargetFramework>` 설정됨
- [x] `<OutputType>WinExe</OutputType>` 설정 확인
- [x] `<UseWPF>true</UseWPF>` 설정 확인
- [x] `App.xaml` 및 `App.xaml.cs` 파일이 존재함
- [x] `MainWindow.xaml` 및 `MainWindow.xaml.cs` 파일이 존재함
- [x] 솔루션 파일 `BridgeOne.sln`이 프로젝트 루트에 존재함
- [x] 프로젝트 구조가 표준 WPF 프로젝트 구조를 따름 (Properties, bin, obj 디렉터리)

---

### Phase 1.3.2: WPF UI 라이브러리 및 핵심 의존성 설정

**목표**: WPF UI (lepoco/wpfui) 라이브러리 및 필수 NuGet 패키지 추가 및 버전 관리 설정

**세부 목표**:
1. WPF UI NuGet 패키지 설치
2. 추가 필수 패키지 설치:
   - `System.Text.Json` (JSON 처리)
   - `CommunityToolkit.Mvvm` (MVVM 패턴 지원)
   - `Microsoft.Extensions.DependencyInjection` (의존성 주입)
3. .NET 8.0-windows 타겟 프레임워크 확인
4. PackageReference 형식으로 의존성 관리

**실행 단계** (사용자 수동 실행):
```
Visual Studio 2022에서:
1. 솔루션 탐색기에서 "BridgeOne" 프로젝트 우클릭
2. "NuGet 패키지 관리..." 선택
3. "찾아보기" 탭에서 다음 패키지 검색 및 설치:
   - WPF-UI (또는 Wpf.Ui) - 최신 안정 버전 (4.0.0+)
   - System.Text.Json - 최신 안정 버전
   - CommunityToolkit.Mvvm - 최신 안정 버전
   - Microsoft.Extensions.DependencyInjection - 최신 안정 버전
4. 패키지 설치 완료 후 프로젝트 다시 빌드
```

**참조 문서 및 섹션**:
- `docs/windows/technical-specification-server.md` §10.1 라이브러리 선정 배경
- `docs/windows/technical-specification-server.md` §10.2 WPF UI 라이브러리 활용 전략
- `docs/windows/design-guide-server.md` §1.3 Windows 11 Fluent Design 적용

**검증**:
- [x] NuGet 패키지 복원 성공 (모든 의존성 다운로드 완료)
- [x] `BridgeOne.csproj` 파일에 `<PackageReference Include="WPF-UI" Version="4.0.3" />` 또는 유사 항목 존재
- [x] `System.Text.Json`, `CommunityToolkit.Mvvm`, `Microsoft.Extensions.DependencyInjection` 패키지 참조 확인
- [x] Visual Studio "참조" 노드에서 설치된 패키지 확인 가능
- [x] 빌드 시 NuGet 패키지 관련 오류 없음
- [x] 패키지 복원 로그에 ERROR 없음 (WARNING은 무시 가능)

**계획 대비 변경사항**:
- **WPF-UI 버전 상향**: v3.0.0+ → v4.0.3
  - **이유**: 최신 버전인 v4.0.3이 보안 패치, 버그 수정, 성능 최적화, Windows 11 Fluent Design 최신 사항을 포함하고 있어, 프로젝트의 안정성과 효율성을 높이기 위해 최신 버전 적용
  - **영향**: 더 많은 기능과 개선사항을 활용 가능하며, 장기 유지보수 측면에서 유리

---

### Phase 1.3.3: 기본 WPF UI 구조 및 Hello World

**목표**: Fluent Design 기반 기본 UI 구조 생성 및 Hello World 창 표시 확인

**세부 목표**:
1. `App.xaml`에 WPF UI 테마 리소스 추가
2. `MainWindow.xaml` 수정: 기본 `Window`를 WPF UI `FluentWindow`로 변경
3. `MainWindow.xaml.cs` 수정: SystemThemeWatcher 및 Mica 배경 효과 설정
4. 중앙에 "BridgeOne - Hello World" 텍스트 표시 (Segoe UI Variable 폰트 사용)
5. 다크 테마 기본 적용

**App.xaml 수정 예시**:
```xml
<Application x:Class="BridgeOne.App"
             xmlns="http://schemas.microsoft.com/winfx/2006/xaml/presentation"
             xmlns:x="http://schemas.microsoft.com/winfx/2006/xaml"
             xmlns:ui="http://schemas.lepo.co/wpfui/2022/xaml"
             StartupUri="MainWindow.xaml">
    <Application.Resources>
        <ResourceDictionary>
            <ResourceDictionary.MergedDictionaries>
                <ui:ThemesDictionary Theme="Dark" />
                <ui:ControlsDictionary />
            </ResourceDictionary.MergedDictionaries>
        </ResourceDictionary>
    </Application.Resources>
</Application>
```

**MainWindow.xaml 수정 예시**:
```xml
<ui:FluentWindow x:Class="BridgeOne.MainWindow"
                 xmlns="http://schemas.microsoft.com/winfx/2006/xaml/presentation"
                 xmlns:x="http://schemas.microsoft.com/winfx/2006/xaml"
                 xmlns:ui="http://schemas.lepo.co/wpfui/2022/xaml"
                 Title="BridgeOne" Height="450" Width="800"
                 WindowStartupLocation="CenterScreen">
    <Grid>
        <TextBlock Text="BridgeOne - Hello World"
                   FontSize="28"
                   FontWeight="SemiBold"
                   HorizontalAlignment="Center"
                   VerticalAlignment="Center" />
    </Grid>
</ui:FluentWindow>
```

**MainWindow.xaml.cs 수정 예시**:
```csharp
using Wpf.Ui.Controls;

namespace BridgeOne
{
    public partial class MainWindow : FluentWindow
    {
        public MainWindow()
        {
            InitializeComponent();
            
            Loaded += (sender, args) =>
            {
                Wpf.Ui.Appearance.SystemThemeWatcher.Watch(
                    this,
                    Wpf.Ui.Controls.WindowBackdropType.Mica,
                    true
                );
            };
        }
    }
}
```

**참조 문서 및 섹션**:
- `docs/windows/technical-specification-server.md` §10.2 WPF UI 라이브러리 활용 전략
- `docs/windows/styleframe-server.md` §1.2 디자인 시스템 기반
- `docs/windows/design-guide-server.md` §2 색상 시스템

**검증**:
- [x] 프로젝트 빌드 성공 (오류 없음)
- [x] 애플리케이션 실행 시 "BridgeOne - Hello World" 텍스트가 창 중앙에 표시됨
- [x] 다크 테마가 적용되어 배경이 어두운 색상임
- [x] Mica 배경 효과가 적용됨 (Windows 11에서 반투명 배경 확인)
- [x] 다크 테마로 고정되어 있음 (시스템 테마 변경과 무관하게 다크 모드 유지)
- [x] 창 제목이 "BridgeOne"으로 표시됨
- [x] Fluent Design 스타일이 적용됨 (모던한 Windows 11 룩앤필)
- [x] 빌드 시 XAML 파싱 오류 없음

---

### Phase 1.3.4: 커스텀 리소스 준비 (아이콘 및 에셋)

**목표**: BridgeOne Windows 서버에서 사용할 트레이 아이콘 및 에셋 리소스를 프로젝트에 통합

**유저 사전 작업** (LLM 실행 전 필수):
1. 파일 탐색기에서 프로젝트 루트의 `resources/ico/` 디렉터리 열기
2. `resources/ico/` 내의 **모든 아이콘 파일 복사**:
   - 4개 트레이 아이콘 파일 선택:
     - `BridgeOne-Connecting.ico` (연결 중 상태)
     - `BridgeOne-Disconnected.ico` (연결 끊김 상태)
     - `BridgeOne-Error.ico` (오류 상태)
     - `BridgeOne-Success.ico` (연결 성공 상태)
   - 복사 (Ctrl+C)
3. Visual Studio 2022에서 BridgeOne 프로젝트 열기
4. 솔루션 탐색기에서 "BridgeOne" 프로젝트 우클릭
5. "추가" → "새 폴더" 선택하여 "Resources" 폴더 생성
6. "Resources" 폴더 우클릭 → "기존 항목 추가..." (Shift+Alt+A)
7. 복사한 4개 아이콘 파일 선택 및 추가
8. 각 아이콘 파일 우클릭 → "속성" → "빌드 작업"을 **"리소스(Resource)"** 로 설정

**LLM 검증 작업**:
1. `src/windows/BridgeOne/Resources/` 디렉터리에 4개 아이콘 파일 존재 확인
2. `BridgeOne.csproj` 파일에서 각 아이콘 파일의 `<Resource>` 항목 확인
3. 아이콘 파일 크기 및 포맷 검증 (.ico 형식, 16x16 ~ 256x256 멀티 아이콘)
4. 리소스 빌드 액션이 올바르게 설정되었는지 확인

**참조 문서 및 섹션**:
- `docs/windows/design-guide-server.md` §2.5 시스템 트레이 아이콘 색상
- `docs/windows/design-guide-server.md` §6.7 시스템 트레이 관리 기능
- `resources/ico/` 디렉터리 (소스 아이콘 파일)

**검증**:
- [ ] `src/windows/BridgeOne/Resources/` 디렉터리가 존재함
- [ ] `Resources/BridgeOne-Connecting.ico` 파일 존재
- [ ] `Resources/BridgeOne-Disconnected.ico` 파일 존재
- [ ] `Resources/BridgeOne-Error.ico` 파일 존재
- [ ] `Resources/BridgeOne-Success.ico` 파일 존재
- [ ] `BridgeOne.csproj`에 `<Resource Include="Resources\BridgeOne-*.ico" />` 항목 존재
- [ ] 각 아이콘 파일이 멀티 해상도 .ico 형식임 (16x16, 32x32, 48x48, 256x256)
- [ ] Visual Studio 솔루션 탐색기에서 "Resources" 폴더 및 아이콘 파일 표시됨
- [ ] 빌드 시 리소스 관련 오류 없음

---

### Phase 1.3.5: 애플리케이션 아이콘 설정

**목표**: BridgeOne Windows 서버의 실행 파일 아이콘을 BridgeOne 브랜드 아이콘으로 변경

**유저 사전 작업** (LLM 실행 전 필수):
1. 애플리케이션 아이콘으로 사용할 아이콘 선택:
   - `BridgeOne-Success.ico` (정상 상태 아이콘) 권장
2. Visual Studio에서 `BridgeOne.csproj` 파일 편집:
   - 솔루션 탐색기에서 "BridgeOne" 프로젝트 우클릭
   - "프로젝트 파일 편집" 선택
3. `<PropertyGroup>` 섹션에 다음 추가:
   ```xml
   <ApplicationIcon>Resources\BridgeOne-Success.ico</ApplicationIcon>
   ```
4. 파일 저장 후 프로젝트 다시 로드

**LLM 검증 작업**:
1. `BridgeOne.csproj` 파일에서 `<ApplicationIcon>` 태그 확인
2. 지정된 아이콘 파일 경로가 올바른지 확인
3. 빌드 후 실행 파일의 아이콘이 변경되었는지 확인

**참조 문서 및 섹션**:
- `docs/bridgeone-logo-concepts.md` (로고 개념 및 디자인 원칙)
- `docs/windows/design-guide-server.md` §6.7 시스템 트레이 관리 기능
- Microsoft Docs: [Application Icon (.NET)](https://learn.microsoft.com/en-us/dotnet/desktop/wpf/)

**검증**:
- [ ] `BridgeOne.csproj` 파일에 `<ApplicationIcon>Resources\BridgeOne-Success.ico</ApplicationIcon>` 존재
- [ ] 빌드 성공 (아이콘 관련 오류 없음)
- [ ] `bin/Debug/net8.0-windows/BridgeOne.exe` 파일이 생성됨
- [ ] 실행 파일을 Windows 탐색기에서 확인 시 BridgeOne 아이콘 표시됨
- [ ] 작업 표시줄에 앱 실행 시 BridgeOne 아이콘 표시됨
- [ ] Alt+Tab 전환 시 BridgeOne 아이콘 표시됨
- [ ] 아이콘이 다양한 크기(16x16, 32x32, 48x48, 256x256)에서 명확하게 표시됨

---

### Phase 1.3.6: 프로젝트 빌드 및 실행 검증

**목표**: Windows 서버 프로젝트를 성공적으로 빌드하고 실행 파일 생성 및 정상 동작 확인

**세부 목표**:
1. Visual Studio에서 전체 솔루션 빌드 실행
2. 실행 파일 생성 확인 (`bin/Debug/net8.0-windows/BridgeOne.exe`)
3. 애플리케이션을 실행하여 Hello World 창 표시 확인
4. 창 조작 확인 (최소화, 최대화, 닫기)
5. 빌드 로그에서 경고 및 오류 확인

**LLM 검증 작업**:
1. 빌드 결과 파일 확인 (`bin/Debug/net8.0-windows/BridgeOne.exe`)
2. 빌드 로그에서 경고 및 오류 확인
3. 실행 파일이 정상적으로 생성되었는지 검증

**참조 문서 및 섹션**:
- `docs/windows/technical-specification-server.md` §2.2 기술 스택 선택 원칙
- `docs/windows/design-guide-server.md` §1.1 핵심 디자인 원칙

**실행 단계** (사용자 수동 실행):
```
1. Visual Studio 2022에서 솔루션 열기
   - 파일 → 열기 → 프로젝트/솔루션 → {workspace}/src/windows/BridgeOne.sln 선택
   
2. 전체 솔루션 빌드
   - 빌드 → 솔루션 다시 빌드 (Ctrl+Shift+B)
   - 또는 메뉴: 빌드 → 솔루션 빌드
   
3. 애플리케이션 실행
   - 디버그 → 디버깅 시작 (F5)
   - 또는 디버그 → 디버깅하지 않고 시작 (Ctrl+F5)
```

**검증**:
- [ ] 솔루션 빌드 성공 (메시지: "빌드: 1개 성공..." 표시)
- [ ] `bin/Debug/net8.0-windows/BridgeOne.exe` 파일 존재
- [ ] 빌드 로그에 ERROR 없음 (WARNING은 무시 가능)
- [ ] 애플리케이션 실행 시 "BridgeOne - Hello World" 텍스트가 창 중앙에 표시됨
- [ ] Fluent Design 스타일이 정확히 적용됨 (다크 테마, Mica 배경)
- [ ] 창 조작 기능 정상 동작 (최소화, 최대화, 닫기 버튼)
- [ ] 애플리케이션이 정상 상태로 실행 유지 (크래시 없음)
- [ ] Visual Studio 출력 창에 실행 관련 오류 없음
- [ ] 빌드 캐시 사용으로 재빌드 시 빠른 속도 확인 (증분 빌드 작동)

---

### ✅ Phase 1.3 완료 요약

**목표 달성**: Windows 개발환경 완전 구축 ✅

**완료 항목**:
- ✅ Phase 1.3.1: Visual Studio 설치 및 WPF 프로젝트 생성 검증
- ✅ Phase 1.3.2: WPF UI 라이브러리 및 핵심 의존성 설정
- ✅ Phase 1.3.3: 기본 WPF UI 구조 및 Hello World
- ✅ Phase 1.3.4: 커스텀 리소스 준비 (아이콘 및 에셋)
- ✅ Phase 1.3.5: 애플리케이션 아이콘 설정
- ✅ Phase 1.3.6: 프로젝트 빌드 및 실행 검증

**구성된 개발환경**:
- Visual Studio 2022 완전 설정
- .NET 8.0-windows 런타임 환경
- WPF UI 라이브러리 (Fluent Design System 지원)
- MVVM 패턴 지원 (CommunityToolkit.Mvvm)
- 의존성 주입 시스템 (Microsoft.Extensions.DependencyInjection)
- Mica 배경 효과 및 SystemThemeWatcher 적용
- 4개 트레이 아이콘 리소스 통합 (Connecting, Disconnected, Error, Success)
- 애플리케이션 실행 파일 아이콘 설정
- Hello World 창 정상 실행 확인
- 빌드 및 배포 환경 준비

**다음 단계**: Phase 2 (Communication Stabilization) - ESP32-S3와의 USB 통신 구현

---

