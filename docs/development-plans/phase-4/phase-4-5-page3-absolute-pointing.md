---
title: "BridgeOne Phase 4.5: Page 3 — 절대좌표 패드 페이지"
description: "BridgeOne 프로젝트 Phase 4.5 - Standard 모드 Page 3: AbsolutePointingPad, 줌 기능, Vendor CDC 줌 오버레이"
tags: ["android", "absolute-pointing", "zoom", "vendor-cdc", "overlay", "ui"]
version: "v1.0"
owner: "Chatterbones"
updated: "2026-04-01"
---

# BridgeOne Phase 4.5: Page 3 — 절대좌표 패드 페이지

**개발 기간**: 3-4일

**목표**: 터치한 위치가 곧 PC 커서 위치가 되는 절대좌표 포인팅 전용 Page 3를 구현합니다. 줌 기능으로 미세 조작을 지원하고, Windows 서버 연동 시 PC 화면에 줌 영역 박스 오버레이를 렌더링합니다.

**핵심 성과물**:
- AbsolutePointingPad Composable (PointingArea + ControlBar + CoordinateIndicator)
- 절대좌표 변환 알고리즘 (터치 비율 → HID 0~32767)
- 줌 기능 (드래그 기반 줌 진입, 매핑 범위 축소)
- FrameBuilder.buildAbsoluteFrame() (0x80 프레임 타입)
- Vendor CDC 줌 상태 전송 (VCDC_CMD_ZOOM_STATE 0x30)

**선행 조건**: Phase 4.4 (Page 2 풀 와이드 터치패드) 완료, Phase 4.3 (터치패드 고급 기능) 완료

**에뮬레이터 호환성**: AbsolutePointingPad 전체 UI, 줌 인터랙션, CoordinateIndicator 에뮬레이터에서 개발 가능. 절대좌표 HID 전송 및 줌 오버레이 Vendor CDC 연동은 실기기에서 별도 검증.

---

## 현재 상태 분석

### 기존 구현
- `FrameBuilder.kt`: 상대좌표 프레임(`buildMouseFrame`) 구현 완료
- `UsbSerialManager.kt`: UART 프레임 전송 인프라 완료
- `StandardModePage.kt`: 5페이지 HorizontalPager 구조, Page 3는 `Page3AbsolutePointingPlaceholder()` 표시 중
- HID Absolute Mouse Report Descriptor: ESP32-S3 펌웨어에 Report ID 0x02 이미 정의 (`esp32s3-code-implementation-guide.md` §3.3.2)
- Vendor CDC 프레임 전송 인프라: Phase 3에서 구현 완료

### 목표 구조 (styleframe-page3.md 기준)
```
Page 3 — AbsolutePointingPad
├── PointingArea (16:9, 전체 화면 매핑)
│   ├── 터치 좌표 → 절대좌표 (0~32767) 변환
│   └── CoordinateIndicator (십자선 + 점)
├── ControlBar (하단)
│   ├── ClickModeButton (좌/우 클릭 전환)
│   ├── ScrollToggleButton (스크롤 모드)
│   └── ZoomButton (줌 모드 진입/해제)
└── 줌 시각 피드백
    ├── Android: 줌 레벨 텍스트 (PointingArea 내)
    └── PC: 줌 영역 박스 오버레이 (Windows 서버, Standard 전용)
```

---

## Phase 4.5.1: AbsolutePointingPad 기본 구현

**목표**: 절대좌표 패드 기본 포인팅 + 클릭 기능 구현

**개발 기간**: 1.5일

**세부 목표**:
1. `AbsolutePointingPad` Composable:
   - 단일 컴포넌트가 페이지 전체를 구성
   - PointingArea (16:9 비율 유지, letterbox/pillarbox) + ControlBar (하단 48dp)
   - 바깥 여백 16dp
2. `AbsoluteCoordinateCalculator`:
   - 터치 좌표를 PointingArea 내 비율(0.0~1.0)로 변환
   - 비율을 HID 절대좌표(0~32767)로 매핑
   - 영역 밖 터치 시 경계값 클램핑
3. `FrameBuilder.buildAbsoluteFrame()`:
   - 8바이트 프레임, `frame[1] = 0x80` (절대좌표 식별자)
   - absX/absY를 Big-Endian 16비트로 인코딩
4. 터치 이벤트 처리:
   - ACTION_DOWN: 즉시 절대좌표 전송
   - ACTION_MOVE: 실시간 전송 (120Hz)
   - ACTION_UP: 클릭 판정 후 전송 중단
5. 클릭 감지:
   - 터치 지속시간 ≤ 500ms AND 이동량 ≤ 5dp → 클릭
   - ClickModeButton 상태에 따라 좌/우 클릭
6. 전송 최적화:
   - 동일 좌표 연속 전송 방지 (이전 좌표와 비교)
7. CoordinateIndicator:
   - 터치 중일 때 십자선 + 점 표시, 터치 종료 후 300ms 페이드 아웃
8. ControlBar:
   - ClickModeButton: 좌↔우 토글
   - ScrollToggleButton: 스크롤 모드 진입/해제
   - ZoomButton: 이 Phase에서는 Disabled 상태 (Phase 4.5.2에서 활성화)

**신규 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/AbsolutePointingPad.kt`
- `src/android/app/src/main/java/com/bridgeone/app/ui/utils/AbsoluteCoordinateCalculator.kt`

**수정 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/protocol/FrameBuilder.kt` (buildAbsoluteFrame 추가)
- `src/android/app/src/main/java/com/bridgeone/app/ui/pages/StandardModePage.kt` (placeholder → AbsolutePointingPad 교체)

**참조 문서**:
- `docs/android/component-design-guide-app.md` §4 (AbsolutePointingPad 컴포넌트 설계)
- `docs/android/styleframe-page2.md` §2 (레이아웃 구조)
- `docs/android/technical-specification-app.md` §2.10 (구현 요구사항)
- `docs/technical-specification.md` §2.4.6.1.1 (HID Absolute Mouse Interface)

> **⚠️ Phase 4.1.7 변경사항**: Page 2 레이아웃은 `AppState.Active` 박스 내 `padding(top=40dp, bottom=40dp)` 적용 영역 안에서 렌더링됨. PointingArea 16:9 비율 계산 시 유효 화면 높이 = 전체 높이 − 80dp 기준 사용.

> **⚠️ Phase 4.1.8 변경사항**: 커스텀 토스트 시스템 도입. `android.widget.Toast` 사용 금지. 모든 알림은 `ToastController.show(message, ToastType, durationMs)` 로 표시.

**검증**:
- [ ] PointingArea 16:9 비율 유지 (letterbox/pillarbox 정상)
- [ ] 터치 위치 → 절대좌표 변환 정확성
- [ ] CoordinateIndicator 표시/페이드 아웃
- [ ] 클릭 감지 (짧은 탭 → 클릭 이벤트)
- [ ] ClickMode 좌↔우 전환
- [ ] 스크롤 모드 진입/해제
- [ ] 동일 좌표 전송 스킵 동작
- [ ] 테두리 색상 상태별 전환 (핑크/노란/초록)

---

## Phase 4.5.2: 줌 기능 구현

**목표**: 드래그 기반 줌 진입 + 줌 상태 좌표 변환 + 줌 해제

**개발 기간**: 1일

**세부 목표**:
1. **ZoomButton 활성화**:
   - 탭: 줌 모드 진입 (줌 활성 시 재탭 → 1x 해제)
   - Selected 상태: 배율 배지 표시 (예: "2x")
2. **줌 진입 인터랙션**:
   - ZoomButton 탭 → 줌 모드 진입
   - PointingArea 위에서 중심점 터치
   - 터치 유지한 채 바깥으로 드래그 → 드래그 거리 비례 줌 레벨 증가
   - 손 떼기 → 줌 레벨 확정, 일반 포인팅 복귀
3. **드래그 거리 → 줌 레벨 매핑**:
   - 0dp → 1x, 50dp → 2x, 100dp → 4x, 150dp+ → 8x (최대)
   - 선형 보간으로 중간 값 허용
4. **줌 상태 좌표 변환**:
   - `absX = zoomMinX + ratio * (zoomMaxX - zoomMinX)`
   - zoomMin/Max는 줌 중심점과 줌 레벨로 계산
   - 경계 클램핑 (0~32767)
5. **줌 해제**: ZoomButton 재탭 → 1x 복귀
6. **시각 피드백 (Android 앱 내)**:
   - 테두리 주황색 (`#FF9800`) 전환
   - 줌 레벨 텍스트 (PointingArea 우상단)
7. **상태 보존**: 페이지 전환 시 줌 레벨/중심점 유지

**수정 파일**:
- `AbsolutePointingPad.kt`
- `AbsoluteCoordinateCalculator.kt` (줌 매핑 범위 계산 추가)

**참조 문서**:
- `docs/android/component-design-guide-app.md` §4.5 (줌 기능 설계)

**검증**:
- [ ] ZoomButton 탭 → 줌 모드 진입
- [ ] 드래그 거리에 비례한 줌 레벨 증가
- [ ] 줌 상태에서 포인팅 정밀도 향상 확인
- [ ] 줌 해제 (1x 복귀)
- [ ] 테두리 주황색 전환
- [ ] 줌 레벨 텍스트 표시
- [ ] 줌 중 스크롤 모드 동시 사용

---

## Phase 4.5.3: Vendor CDC 줌 상태 전송 및 PC 오버레이 연동

**목표**: 줌 상태를 Windows 서버로 전송하여 PC 화면에 줌 영역 박스 오버레이 표시

**개발 기간**: 0.5-1일

**세부 목표**:
1. **Android → ESP32 줌 상태 전송**:
   - 0xFF 커스텀 명령으로 UART 전송
   - JSON payload: `zoom_level`, `min_x`, `min_y`, `max_x`, `max_y`
   - 전송 시점: 줌 확정 시 1회, 드래그 중 30Hz 스로틀, 해제 시 1회
2. **ESP32 투명 중계**:
   - UART 수신 → `VCDC_CMD_ZOOM_STATE (0x30)` Vendor CDC Frame으로 감싸서 Windows 전달
   - Windows 서버 미연결 시 폐기
3. **Essential 모드 처리**:
   - 서버 미연결 → 줌 상태 전송 스킵, 앱 내 텍스트만 표시

> **참고**: Windows 서버 측 오버레이 렌더링(`UpdateZoomOverlay`)은 Windows 서버 Phase에서 구현. 이 Phase에서는 Android 전송 측만 담당.

**수정 파일**:
- `AbsolutePointingPad.kt` (줌 상태 변경 시 전송 트리거)
- `src/android/app/src/main/java/com/bridgeone/app/protocol/FrameBuilder.kt` (줌 상태 커스텀 명령 생성)

**참조 문서**:
- `docs/technical-specification.md` §2.4.6.1.2 (줌 상태 Vendor CDC 메시지)
- `docs/windows/technical-specification-server.md` §3.6.1.4 (줌 영역 오버레이)

**검증**:
- [ ] 줌 확정 시 UART로 줌 상태 전송
- [ ] 줌 해제 시 zoom_level=1.0 전송
- [ ] Essential 모드에서 전송 스킵 (크래시 없음)
- [ ] Windows 서버 연동 시 PC 화면에 줌 영역 박스 표시 (실기기 검증)

---

## Phase 4.5 완료 후 Page 2 구조

```
Page 3 — AbsolutePointingPad
├── PointingArea (16:9)
│   ├── 터치 → 절대좌표 (0~32767) 변환
│   ├── 줌 시 매핑 범위 축소 (zoomMin~zoomMax)
│   └── CoordinateIndicator (십자선 + 점)
├── ControlBar
│   ├── ClickModeButton (좌/우 토글)
│   ├── ScrollToggleButton (스크롤 모드)
│   └── ZoomButton (줌 진입/해제, 배율 배지)
└── 시각 피드백
    ├── 테두리 색상 (핑크/노란/초록/주황)
    ├── 줌 레벨 텍스트 (앱 내)
    └── 줌 영역 박스 (PC 화면, Standard 전용)
```
