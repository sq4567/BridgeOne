---
title: "BridgeOne Phase 4.9: Page 3 — 절대좌표 패드 페이지 (서버 중계 재설계)"
description: "BridgeOne 프로젝트 Phase 4.9 - Standard 전용 Page 3: AbsolutePointingPad, 서버 SetCursorPos 중계, 자유 비율, 드래그 앤 드롭, 멀티모니터"
tags: ["android", "absolute-pointing", "server-relay", "zoom", "vendor-cdc", "multi-monitor", "ui"]
version: "v2.1"
owner: "Chatterbones"
updated: "2026-07-06"
---

# BridgeOne Phase 4.9: Page 3 — 절대좌표 패드 페이지 (서버 중계 재설계)

**개발 기간**: 3.5-4.5일 (Android 파트만. 펌웨어/서버는 후속 통합 Phase)

**목표**: 터치한 위치가 곧 PC 커서 위치가 되는 절대좌표 포인팅 전용 Page 3를 구현합니다. 좌표를 서버로 중계해 서버가 `SetCursorPos`를 직접 호출하는 경로 하나로, 자유로운 패드 비율(stretch 매핑)과 멀티 모니터를 지원합니다. 드래그 앤 드롭 모드를 제어버튼으로 추가합니다.

> **⚠️ 대규모 설계 변경 이력(사용자 확정, 2026-07-06)**: 기존 v1.0 계획은 "PointingArea 16:9 강제 + HID 절대좌표 경로"를 전제로 했습니다. 이는 (1) PC 서버가 좌표를 재맵핑할 여지가 없고 (2) 멀티 모니터를 근본적으로 지원할 수 없다는 한계가 확인되어 재설계했습니다. 핵심 결정:
> 1. **서버 중계 경로**: 서버가 `SetCursorPos`로 직접 커서 이동(자유 비율/멀티모니터/정밀)
> 2. **stretch 매핑**: 패드 전 영역을 대상 화면 전체에 균등 매핑(letterbox 폐기) → 16:9 강제 폐기, 패드 비율 자유화
> 3. **멀티 모니터**: 단일 모니터 매핑 기본 + 전체 가상 데스크톱 토글 옵션
> 4. **드래그 앤 드롭 모드**: 제어버튼 토글, 기존 MouseHold 인프라 재사용
> 5. **Essential 경로 제거(2026-07-06 추가 확정)**: 최초 설계는 서버 미연결(Essential) 상태를 위한 HID 절대좌표 폴백 경로를 별도로 뒀으나, `AbsolutePointingPad`(Page 3)는 애초에 **Standard 모드 전용 페이지**임이 확인됐다. `BridgeOneApp.kt`가 `bridgeMode`에 따라 `EssentialModePage()`/`StandardModePage()`를 완전히 분리된 트리로 라우팅하므로(`EssentialModePage`는 자체 2열 레이아웃이며 페이지 로테이션이나 Page 3를 포함하지 않음), Essential 모드에서 이 페이지는 애초에 렌더링되지 않는다. 따라서 런타임 분기가 필요 없고 **서버 중계 경로 하나만 구현**하면 된다. HID Report ID 0x02(절대좌표)는 이 페이지가 아니라 별개 기능인 Native Macro 재생(`technical-specification.md` §4.4.2)이 사용한다.
>
> 설계 문서 보강이 이미 완료되었습니다: `technical-specification.md` §2.4.6.1.1~1.3, `technical-specification-server.md` §3.6.9, `technical-specification-app.md` §2.10, `styleframe-page3.md`, `component-design-guide-app.md` §4, `esp32s3-code-implementation-guide.md` §3.3.2. 본 Phase 문서는 그 설계를 구현 단위로 분해합니다.

**핵심 성과물 (Android 파트, 본 Phase 범위)**:
- AbsolutePointingPad Composable (PointingArea + CoordinateIndicator, 하단 전용 ControlBar 없음)
- 기존 `ControlButtonContainer`(상단 오버레이) 재사용 + 신규 DragModeButton/ZoomButton 슬롯
- 좌표 변환 알고리즘 (터치 비율 0.0~1.0)
- **서버 중계 전송**: `buildAbsolutePositionCommand`(0xFF/0x02) 하나로 좌표를 서버에 전달
- 드래그 앤 드롭 모드 (제스처 스코프 buttons bit0 유지)
- 모니터 셀렉터 + 역방향 모니터 개수 수신
- 줌 기능 (앱 내) + Vendor CDC 줌 상태 UART 전송(Android 측까지)
- Page 3 엣지존 편집 화면 연동

**패드 비율/배치는 본 Phase 범위 밖** — Phase 4.15 페이지 커스터마이징이 그리드 배치(`colSpan`/`rowSpan`)로 소유한다. 자유 비율(접근성 목적의 왜곡 배치)과 모니터 종횡비 일치 둘 다 그리드 셀 크기 조정으로 표현 가능하며, 4.15 완성 전까지 패드는 Fill 유지.

**펌웨어(ESP32)·서버(Windows) 구현은 본 Phase 범위 밖** — 후속 통합 Phase로 분리(하단 "펌웨어·서버 파트" 참조). Phase 4가 "Android 완성" 단계라는 원래 취지 유지.

**선행 조건**: Phase 4.8 (Page 2 풀 와이드 터치패드) 완료, Phase 4.3 (터치패드 고급 기능) 완료

**에뮬레이터 호환성**: UI 전체, 좌표 변환 단위테스트, 프레임 빌더 단위테스트는 에뮬레이터/빌드로 완결 가능. 실제 커서 이동(서버 SetCursorPos), 드래그앤드롭 end-to-end, 줌 오버레이 PC 렌더링, 역방향 모니터 개수 통지는 실기기(펌웨어·서버 완성 후) 검증 필요.

---

## 현재 상태 분석

### 기존 구현
- `FrameBuilder.kt`: 8바이트 프레임 생성 `buildFrame()` 구현 완료 (상대좌표 전용). 절대좌표 관련 함수 없음
- `UsbSerialManager.kt`: UART 프레임 전송 인프라 완료. `frameQueue` 원시 전송 경로 존재하나 public API 없음
- `StandardModePage.kt`: 현재 5페이지 HorizontalPager 구조(`PAGE_COUNT = 5`), 논리 인덱스 2=`Page3KeyboardPlaceholder()`, 3=`Page4MinecraftPlaceholder()`, 4=`Page5Settings()`. **절대좌표 페이지 슬롯이 아직 없음.** 본 Phase에서 `PAGE_COUNT`를 6으로 확장하여 논리 인덱스 2에 절대좌표 페이지를 신규 삽입하고, 기존 키보드/마인크래프트/설정 placeholder를 인덱스 3/4/5로 이동한다
- `BridgeOneApp.kt`: `bridgeMode`에 따라 `EssentialModePage()`/`StandardModePage()`를 완전히 분리해 라우팅함(276~278행 부근). `AbsolutePointingPad`는 `StandardModePage` 내부에만 배치되므로 Essential 모드에서 렌더링될 일이 없음 — 이 사실이 본 Phase가 서버 중계 경로 하나만 구현하면 되는 근거
- `StandardModePageState.kt`: `heldMouseButtons`(Set)/`toggleMouseHold()` 마우스 홀드 인프라 완료 — 드래그 앤 드롭 모드(4.9.4)에서 buttons-bit 배선 재사용
- `ClickDetector.kt`: `detectClick()`, `createMouseButtonFrame()` 구현 완료 — 재사용
- `ControlButtonContainer.kt` / `ControlButtonConfig`: 기존 터치패드 페이지의 상단 오버레이 컴포넌트, 슬롯 추가로 재사용
- HID Absolute Mouse Report Descriptor(Report ID 0x02): Native Macro 재생 전용으로 ESP32-S3 펌웨어에 문서화 완료(`esp32s3-code-implementation-guide.md` §3.3.2). 본 Phase(AbsolutePointingPad)와는 무관
- Vendor CDC 프레임 전송 인프라: Phase 3에서 구현 완료

**Phase 4.9 완료 후 목표 페이지 구조**:

| 논리 인덱스 | 페이지 | 상태 |
|---|---|---|
| 0 | Page 1 터치패드+액션 | 구현 완료 |
| 1 | Page 2 멀티커서 | 구현 완료 |
| **2** | **Page 3 절대좌표 (본 Phase)** | 4.9.1~4.9.4 완료(기본 포인팅·서버 중계·엣지존 통합·드래그 앤 드롭), 모니터/줌은 4.9.5~4.9.6. 패드 비율/배치는 본 Phase 범위 밖(Phase 4.15) |
| 3 | Page 4 키보드 | placeholder (이동) |
| 4 | Page 5 마인크래프트 | placeholder (이동) |
| 5 | Page 6 설정 | 구현 완료 (이동) |

> **⚠️ 인덱스 시프트 부작용 — `JumpToPage` 마이그레이션 필요**: 인덱스 삽입(2/3/4 → 3/4/5)으로 깨지는 곳은 `EdgeZoneAction.JumpToPage(pageIndex)` 하나뿐이다. `EdgeZoneJson.kt`가 이 액션을 페이지 논리 인덱스 정수 그대로 직렬화(`"page": N`)해서 영속화하므로, 이미 "페이지 5(설정, 구 index=4)로 점프"를 저장해둔 사용자가 있다면 시프트 후 `pageIndex=4`가 새 배치의 마인크래프트(구 index=3이 이동한 자리)를 가리키게 되어 엉뚱한 페이지로 점프하게 된다. 저장된 `JumpToPage` 값 중 `pageIndex >= 2`인 것을 +1 이동하는 1회성 마이그레이션이 필요(4.9.8 참조). 안전 항목(별도 조치 불필요): 엣지존 할당 저장 키(`TouchpadIds.standardPage`), `StandardModePrefs.kt`의 전역 설정, `PageIndicator`/`EdgeZoneEditorScreen.pageCount`(모두 `PAGE_COUNT` 상수로 자동 대응)

### 아키텍처 요약

```
Android: 터치 → 비율(0~1) → 0xFF/0x02 서버 중계 프레임 (8바이트, 바이너리)
  → ESP32는 파싱하지 않고 Vendor CDC로 그대로 중계
  → 서버가 stretch 매핑 + SetCursorPos 호출
```
상세 근거와 discriminator 표는 `technical-specification.md` §2.4.6.1.3, Android 구현 스펙은 `technical-specification-app.md` §2.10 참조. Page 3가 Standard 전용이라 런타임 모드 분기 코드는 필요 없다.

### 목표 구조 (styleframe-page3.md 기준)
```
Page 3 — AbsolutePointingPad
├── PointingArea (자유 비율, Fill 기본, stretch 매핑)
│   ├── 터치 비율 변환 → 서버 중계 프레임 전송
│   ├── CoordinateIndicator (십자선 + 점)
│   └── 엣지존/엣지스와이프 시스템 재사용 (좌표 무관 기능만)
├── ControlButtonContainer (상단 오버레이, 기존 컴포넌트 재사용)
│   ├── ClickModeButton (좌/우 클릭 전환)
│   ├── ZoomButton (DPI 슬롯 자리 대체)
│   └── DragModeButton (신규, ScrollSensitivity 슬롯 자리 — 드래그 앤 드롭)
├── MonitorSelector (신규, 모니터 2개 이상 시만 노출)
└── 줌 시각 피드백
    ├── Android: 줌 레벨 텍스트 (PointingArea 내)
    └── PC: 줌 영역 박스 오버레이 (Windows 서버, 대상 모니터 rect 기준, 후속)
```

---

## Phase 4.9.1: AbsolutePointingPad 기본 구현 (자유 비율)

**목표**: 절대좌표 패드 기본 포인팅 + 클릭 기능 구현. 16:9 강제 없이 자유 비율(Fill 기본)로 동작.

**개발 기간**: 1.5일

**세부 목표**:
1. `AbsolutePointingPad` Composable:
   - 단일 컴포넌트가 페이지 전체를 구성
   - PointingArea(자유 비율, stretch 매핑) + 상단 `ControlButtonContainer` 오버레이
   - 바깥 여백 16dp
2. `AbsoluteCoordinateCalculator`:
   - 터치 좌표를 PointingArea 내 비율(0.0~1.0)로 변환(`TouchRatio`)
   - 영역 밖 터치 시 경계값 클램핑
   - stretch 매핑 원칙: letterbox/pillarbox 계산 없음
3. 터치 이벤트 처리:
   - ACTION_DOWN: 즉시 좌표 전송
   - ACTION_MOVE: 실시간 전송 (120Hz)
   - ACTION_UP: 클릭 판정 후 전송 중단(드래그 모드 시 4.9.4 참조)
4. 클릭 감지:
   - 터치 지속시간 ≤ 500ms AND 이동량 ≤ 5dp → 클릭 (`ClickDetector.detectClick` 재사용, 절대좌표 전용 임계값 별도 정의)
   - ClickModeButton 상태에 따라 좌/우 클릭
5. 전송 최적화: 동일 좌표 연속 전송 방지 (이전 비율과 비교)
6. CoordinateIndicator: 터치 중일 때 십자선 + 점 표시, 터치 종료 후 300ms 페이드 아웃
7. `ControlButtonContainer` 재사용, "쓸 수 있는 버튼만" 필터링:
   - `ControlButtonConfig(showClickMode = true, showMoveMode = false, showScrollMode = false, showCursorMode = false, showDpi = false, showScrollSensitivity = false, showZoom = true, showDrag = true)`
   - **근거**: MoveMode(축 잠금)·DPI(델타 배율)는 델타 벡터 연산이라 절대좌표에서 성립하지 않음. ScrollMode는 드래그 앤 드롭 모드(4.9.4)로 대체. CursorMode(멀티커서)는 Page 2 전용 상태 결합으로 배제
   - ClickModeButton: 좌↔우 토글 (기존 로직 재사용)
   - `showZoom`/`showDrag` 필드를 `ControlButtonConfig`에 신규 추가. 이 Phase에서는 둘 다 Disabled 상태(zoom은 4.9.6, drag는 4.9.4에서 각각 활성화)
8. ~~엣지존/엣지스와이프 시스템 통합~~ → **Phase 4.9.3으로 분리** (하단 참조)

> **⚠️ 구현 중 범위 조정(2026-07-06, 유저 확정)**: 세부 목표 8(엣지존/엣지스와이프 통합)은 `TouchpadWrapper.kt`의 제스처 상태머신(1700줄 이상, `TouchpadState`/`EdgeZoneConfig`에 강결합)을 복제해야 해서 4.9.1 범위에서 분리했다. 본 Phase는 PointingArea 핵심 포인팅·클릭·시각 피드백 + ControlButtonContainer 통합까지만 완료했고, 엣지존/엣지스와이프는 신규 하위 Phase **4.9.3**으로 넘겼다(서버 중계 전송(4.9.2)이 더 급한 핵심 경로라 먼저 두고, 엣지존은 그다음으로 배치 — 기존 4.9.3~4.9.8은 4.9.4~4.9.9로 한 칸씩 밀림).

**신규 파일** (실제 구현):
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/AbsolutePointingPad.kt` — `AbsolutePointingPad` + 내부 `PointingArea`/`CoordinateIndicator` private Composable. `ControlButtonContainer`는 Page 2와 동일한 풀 와이드 예외 규칙(`component-touchpad.md` §1.3)을 적용해 화면 폭 비율(360dp 미만 60%, 이상 64%)로 축소·중앙 정렬 — 최초 구현 시 이 규칙이 누락되어 버튼이 과도하게 넓었던 것을 실기기 확인 후 수정
- `src/android/app/src/main/java/com/bridgeone/app/ui/utils/AbsoluteCoordinateCalculator.kt` (+ `AbsoluteCoordinateCalculatorTest.kt`)
- `src/android/app/src/main/java/com/bridgeone/app/ui/utils/AbsolutePointingConstants.kt` (신규 상수 중앙화, 기본값 주석 포함)
- `src/android/app/src/main/java/com/bridgeone/app/ui/pages/standard/Page3AbsolutePointing.kt` (독립 파일, 파라미터 없이 `AbsolutePointingPad()` 호출)
- `res/drawable/ic_zoom.xml`, `res/drawable/ic_drag_mode.xml` (ZoomButton/DragModeButton 아이콘, Disabled 슬롯이지만 각각 4.9.6/4.9.4에서 그대로 재사용)

**수정 파일** (실제 구현):
- `ui/pages/StandardModePage.kt` — `PAGE_COUNT` 5→6, 논리 인덱스 2에 `Page3AbsolutePointing()` 삽입, 기존 키보드/마인크래프트/설정을 3/4/5로 시프트 (⚠️ 저장된 `JumpToPage(pageIndex>=2)` 값은 4.9.8 마이그레이션 전까지 엉뚱한 페이지를 가리킴 — 문서 상단 경고와 동일)
- `ui/components/touchpad/ControlButtonContainer.kt` — `ControlButtonConfig`에 `showZoom`/`showDrag: Boolean = false` 필드 추가, `hasRightSlot`(DPI/ScrollSensitivity 공유 슬롯)과 별도로 ZoomButton/DragModeButton을 각각 독립 슬롯(6·7번째)으로 렌더링(둘 다 동시 노출 가능해야 하므로 슬롯 공유 안 함). 이 Phase에서는 `enabled = false`로 렌더링만(클릭 무반응)
- `ui/components/touchpad/TouchpadColors.kt` — `TouchpadColorPink(#E91E63)` 추가 (AbsolutePointingPad 기본 테두리, 좌클릭 상태)

**미착수 항목** (엣지존/엣지스와이프 — Phase 4.9.3으로 이동):
- `ui/components/touchpad/EdgeZoneOverlay.kt`/`EdgeZoneDetector.kt`/`EdgeZoneActionHandler.kt`/`EdgeGeometry.kt` 재사용 연결
- `TouchpadWrapper.kt`의 `visibleModes` 필터링 로직 참고한 CLICK 전용 모드 필터

**참조 문서**:
- `docs/android/component-design-guide-app.md` §4 (AbsolutePointingPad 컴포넌트 설계)
- `docs/android/styleframe-page3.md` §2 (레이아웃 구조)
- `docs/android/technical-specification-app.md` §2.10.1, §2.10.4 (좌표 변환/클릭 감지)
- `docs/android/component-touchpad.md` §1.3 (ControlButtonContainer 설계, 재사용 대상)

> **⚠️ Phase 4.1.7 변경사항**: Page 3 레이아웃은 `AppState.Active` 박스 내 `padding(top=40dp, bottom=40dp)` 적용 영역 안에서 렌더링됨. 유효 화면 높이 = 전체 높이 − 80dp 기준 사용.
>
> **⚠️ Phase 4.1.8 변경사항**: `android.widget.Toast` 사용 금지. 모든 알림은 `ToastController.show(message, ToastType, durationMs)`로 표시. (본 Phase는 토스트를 사용하지 않음 — 클릭/좌표 계산에 토스트 알림 대상 이벤트 없음)
>
> **⚠️ Phase 4.7.4-A / 4.7.2 변경사항**: 절대좌표 페이지는 `StandardModePage.kt` 인라인 함수가 아니라 독립 파일로 추가(`ui/pages/standard/`). `AbsolutePointingPad.kt`는 `ui/components/`에 둘 것. `AbsoluteCoordinateCalculator`는 순수 함수이므로 추출과 동시에 단위 테스트 작성(`EdgeGeometryTest` 선례). 신규 상수는 인라인 금지, `AbsolutePointingConstants.kt`에 기본값 주석과 함께 중앙화(4.7.1).

**검증**:
- [x] 터치 위치 → 비율 변환 정확성 (단위테스트, `AbsoluteCoordinateCalculatorTest` 10건 통과)
- [x] 동일 좌표 전송 스킵 동작 (`AbsoluteCoordinateCalculator.shouldTransmit` 단위테스트 통과)
- [x] 빌드 성공 (컴파일 에러 없음)
- [x] PointingArea 자유 비율 렌더링 (Fill 기본, letterbox/pillarbox 없음) — 실기기 확인 완료
- [x] CoordinateIndicator 표시/페이드 아웃 — 실기기 확인 완료
- [x] 클릭 감지 (짧은 탭 → 클릭 이벤트) — 실기기 확인 완료
- [x] ClickMode 좌↔우 전환 (ControlButtonContainer 재사용) — 실기기 확인 완료
- [x] Move/Scroll/Cursor/DPI/ScrollSensitivity 버튼 미노출 확인 (config에서 전부 false) — 실기기 확인 완료
- [x] 테두리 색상 상태별 전환 (핑크/노란) — 실기기 확인 완료
- [x] ControlButtonContainer 폭이 터치패드 페이지와 동일(Page 1 컬럼 폭 비율 60%/64%) — 실기기 확인 완료

---

## Phase 4.9.2: 서버 중계 전송 경로 구현 (핵심 신규)

**목표**: 절대좌표 프레임 빌더 + 원시 바이트 전송 API 구현

**개발 기간**: 0.5일

**세부 목표**:
1. `FrameBuilder.buildAbsolutePositionCommand()` (신규):
   - 8바이트, `[0xFF][0x02][absX_H][absX_L][absY_H][absY_L][buttons][targetMonitor]`
   - JSON이 아닌 고정 바이너리(120Hz 고빈도 스트림이므로) — 기존 매크로 트리거(§4.4.2.1)와 동일 패턴
2. `UsbSerialManager.sendCommandBytes(ByteArray)` public API 신규:
   - 기존 `frameQueue`는 private이므로 이 Phase에서 public 전송 진입점 추가
3. `AbsolutePointingPad`에서 매 터치 이벤트마다 `sendCommandBytes(buildAbsolutePositionCommand(...))` 호출 — 런타임 모드 분기 없음(Page 3 자체가 Standard 전용이므로)
4. discriminator 충돌 없음 검증: `0x00~0xFD`(상대), `0xFE`(역방향), `0xFF/0x01`(매크로), `0xFF/0x02`(본 Phase 신규), `0xFF/기타`(JSON 커스텀)

**신규/수정 파일**:
- `protocol/FrameBuilder.kt` — `buildAbsolutePositionCommand()` 신규
- `usb/UsbSerialManager.kt` — `sendCommandBytes(ByteArray)` public API 신규
- `ui/components/AbsolutePointingPad.kt` — 전송 호출

**참조 문서**:
- `docs/technical-specification.md` §2.4.6.1.1~1.3 (전체 아키텍처, discriminator 표)
- `docs/android/technical-specification-app.md` §2.10.2 (프레임 빌더 상세 코드)

**검증**:
- [x] `buildAbsolutePositionCommand()` 바이트 레이아웃 단위테스트 (`FrameBuilderTest` 4건 통과: 헤더, 중간값, 경계값, buttons/targetMonitor 위치)
- [x] 터치 이벤트마다 서버 중계 프레임이 전송되는지 배선 완료 (`AbsolutePointingPad.kt`: DOWN 즉시 전송 + MOVE `shouldTransmit` 통과 시 전송, 별도 단위테스트는 Compose 제스처 특성상 실기기/계측 테스트 영역으로 판단해 생략)
- [x] 빌드 성공 (`assembleDebug` 컴파일 에러 없음, 신규 경고 없음)
- 실기기 필요: 실제 커서 이동 정확도(펌웨어·서버 완성 후, 후속 통합 Phase)

---

## Phase 4.9.3: 엣지존/엣지스와이프 시스템 통합 (Page 3, CLICK 전용)

> **신규 하위 Phase(2026-07-06, 유저 확정)**: 원래 4.9.1의 세부 목표 8이었으나, 구현 규모(TouchpadWrapper.kt의 1700줄 이상 제스처 상태머신 재사용/축약)를 고려해 별도 하위 Phase로 분리했다. 서버 중계 전송(4.9.2)이 더 급한 핵심 경로라 먼저 배치하고, 엣지존 통합을 그 다음인 4.9.3으로 뒀다.

**목표**: AbsolutePointingPad(Page 3)에 엣지존/엣지스와이프 시스템을 연결하되, 델타(상대좌표) 기반 액션을 모두 배제하고 좌표 무관 이산 액션(매크로/단축키/페이지 전환/클릭 모드/마우스 홀드 등)만 노출한다.

**개발 기간**: 완료 (2026-07-09)

> **⚠️ 구현 확정 사항(2026-07-09, 유저 확정)**: 절대좌표 패드는 "터치 위치 = 커서 위치"라서, 엣지존 예약 구간이 좌표 클릭 도달성을 침해하면 안 된다. 이를 위해 당초 계획(변 단위 엣지 예약)보다 정밀한 두 겹 처리로 구현했다:
> 1. **존 단위 gate**: DOWN 지점이 filtered config 상 화이트리스트 통과 존(Unassigned가 아닌 존)의 alongRatio 범위 안에 있을 때만 엣지 제스처 후보로 인식. 그 외 모든 위치(미할당 구간, TOP 전체)는 DOWN 즉시 일반 절대 포인팅 — 화면 4변 가장자리(예: PC 시작 버튼이 있는 왼쪽 아래 코너) 도달성 보존
> 2. **엣지 띠 탭=좌표클릭**: 엣지 후보 제스처라도 UP 시 armed가 아니고 탭 조건(이동 ≤5dp)을 만족하면 DOWN 지점 좌표로 절대 클릭 전송. 안쪽 스와이프(armed)만 엣지 액션 실행. 탭 임계값과 armed 임계값(28dp)이 겹치지 않아 상호배타 → 예약 구간 위에서도 좌표 클릭 손실 0
>
> LEGACY_POPUP은 배제하고 ZONE 모드만 지원. 로테이션 존은 4.9.8(편집 UI) 전까지 후보 없어 `candidates.firstOrNull()` 정적 처리(회전 코루틴 미이식).
>
> **⚠️ 추가 반영(2026-07-09, 유저 요청)**: 최초 구현에는 기존 터치패드(`TouchpadWrapper.kt`)의 "산봉우리(Bump)" 시각 피드백이 누락되어 있었다. 엣지 안쪽으로 들어올수록 그라데이션 봉우리가 커지는 `EdgeBumpOverlay`를 동일 패턴으로 이식했다 — 진입 엣지 고정, 최대 피크 36dp 상한, release/취소 시 spring 수축 애니메이션(`LaunchedEffect(isEdgeCandidate)`), 색상은 현재 클릭모드(핑크/노랑) 연동.

**세부 목표**:
1. `EdgeGeometry.kt`(`detectEntryEdge`/`getInwardDistance`/`getAlongEdgePosition`) 순수 함수를 `PointingArea`의 `pointerInput`에 연결해 엣지 진입/이탈 판정 ✅
2. `EdgeZoneOverlay`(ZONE 모드 시각화) 연결. `EdgeSwipeOverlay`(LEGACY_POPUP)는 배제(대상 모드가 CLICK 하나뿐) ✅
3. `EdgeZoneDetector.findActiveZone()` + `EdgeZoneActionHandler.applyZoneAction()` 재사용 ✅
4. 노출 액션 화이트리스트: `SendMacro`, `SendShortcut`, `CyclePage`, `JumpToPage`, `SetClickMode`, `ToggleMode(CLICK)`, `MouseHoldToggle`, `RestorePreviousMode` 등 좌표 무관 이산 트리거만 허용. `SetMoveMode`/`SetDpi`/`SetCustomDpi`/`SetScrollMode`/`SetScrollSpeed`/`ToggleMultiCursor` 등 델타·스크롤·멀티커서 계열은 전부 배제. **도메인 단위가 아닌 액션 타입 단위 필터**(`isAbsolutePadAllowed`)로 구현 — `EdgeZoneActionResolver.domainOf(ToggleMode(CURSOR))`가 `ActionDomain.CLICK`으로 매핑되는 함정 회피 ✅
5. `TouchpadEdgeZoneAssignmentRepository`를 통한 Page 3 전용 존 할당 영속화(`page3Assignment`, 키 `standard_page_2`). **설정 화면 연동은 4.9.8에서 별도 처리** — `standardTouchpadPages`에는 여전히 2를 추가하지 않아 Page 3는 편집 대상이 아님 ✅

**실제 구현 파일**:
- `ui/components/touchpad/AbsolutePadActionFilter.kt` (신규) — `isAbsolutePadAllowed()`/`filterConfigForAbsolutePad()` 순수 함수. **4.9.8에서 편집기 필터에 재사용 가능**
- `ui/components/AbsolutePointingPad.kt` — `PointingArea`의 `pointerInput`에 존 단위 gate + ZONE 파이프라인(armed/disarm/취소) + 엣지 띠 탭=좌표클릭 로직 추가, `EdgeZoneOverlay` 배치(BoxWithConstraints), `EdgeBumpOverlay` 배치(진입 엣지 고정 + release/취소 spring 수축, `TouchpadWrapper.kt:290-362, 1615-1644` 패턴 이식), 시그니처에 `edgeZoneAssignment`/`onEdgeZoneAssignmentChange`/6개 액션 콜백 추가. **4.9.4(드래그)/4.9.6(모니터 셀렉터)/4.9.7(줌)이 이 시그니처를 추가 확장할 예정이므로 해당 Phase 착수 시 현재 파라미터 목록 확인 필요**
- `ui/pages/standard/Page3AbsolutePointing.kt` — 8개 파라미터로 확장, `AbsolutePointingPad`에 그대로 전달
- `ui/pages/StandardModePage.kt` — `page3Assignment` 상태 + 저장 `LaunchedEffect` 추가, `2 -> Page3AbsolutePointing(...)` 호출부에 배선. `standardTouchpadPages`는 `listOf(0, 1)` 그대로 유지(회귀 방지 핵심)
- `ui/components/touchpad/AbsolutePadActionFilterTest.kt` (신규 테스트)

**참조 문서**:
- `docs/android/technical-specification-app.md` §2.10 (Page 3 전체 스펙)
- 기존 `TouchpadWrapper.kt`의 엣지 감지/오버레이 연결부 (라인 768~975, 1260~1325 부근)

**검증**:
- [x] `isAbsolutePadAllowed` 화이트리스트 8종 허용 + 대표 배제군 거부 단위테스트 통과(`ToggleMode(CURSOR)` 거부 회귀 테스트 포함)
- [x] `filterConfigForAbsolutePad(EdgeZoneConfig.default())` — LEFT 위쪽 절반만 `ToggleMode(CLICK)` 유지, 나머지 전 존 Unassigned 확인. Rotation 부분 필터/전멸→Unassigned 케이스 단위테스트 통과
- [x] 빌드 성공(`assembleDebug` 컴파일 에러 없음, 신규 경고 없음)
- [x] 엣지존 CLICK 액션(좌/우클릭 전환) 동작 — 실기기 확인 완료
- [x] 매크로/단축키/페이지 전환/마우스 홀드 액션 동작 — 실기기 확인 완료
- [x] MOVE/DPI/DYNAMICS/SCROLL/SCROLL_SPEED/CURSOR 모드·액션 미노출 확인 — 실기기 확인 완료
- [x] 엣지 띠(LEFT 위쪽 절반) 탭 → 좌표 클릭, 안쪽 스와이프 → 클릭모드 토글 상호배타 확인 — 실기기 확인 완료
- [x] 왼쪽 아래 코너(BOTTOM, 미할당) 탭 → PC 시작 버튼 등 가장자리 요소 정상 클릭 — 실기기 확인 완료
- [x] Page 1/2 엣지존 동작에 회귀 없음 — 실기기 확인 완료
- [x] 엣지 스와이프 중 산봉우리(Bump) 시각 피드백 등장/수축 애니메이션 동작 — 실기기 확인 완료

---

## Phase 4.9.4: 드래그 앤 드롭 모드

**목표**: 제어버튼 토글로 "커서 이동만" vs "누른 채 이동(드래그 앤 드롭)"을 구분

**세부 목표**:
1. `DragModeButton` 활성화 (`showDrag = true`, ScrollSensitivity 슬롯 자리)
2. 동작 규칙:
   - OFF(기본): `ACTION_DOWN`~`ACTION_UP` 동안 `buttons` bit0 항상 0. 클릭 판정은 4.9.1 로직 그대로
   - ON: `ACTION_DOWN` 시 bit0=1(press)로 최초 프레임부터 전송, `ACTION_MOVE` 동안 bit0 유지, `ACTION_UP` 시 bit0=0(release) 프레임 1회 전송(drop)
3. **제스처 스코프 transient**: 영구 홀드가 아니라 터치 업에서 자동 release. 기존 `heldMouseButtons`/`toggleMouseHold`/`createMouseButtonFrame`의 buttons-bit 배선 패턴을 참고하되 별도 transient 상태로 구현(엣지존 MouseHoldToggle의 영구 홀드와 혼동 금지)
4. 서버가 buttons diff로 SendInput 처리(서버 구현은 후속, Android는 buttons 비트 유지 전송까지만 책임)
5. UI: 테두리 초록색 전환(드래그 모드 ON)

**수정 파일**:
- `ui/components/touchpad/ControlButtonContainer.kt` — `showDrag` 활성화, DragModeButton UI
- `ui/components/AbsolutePointingPad.kt` — press/release 시퀀스 로직

> **⚠️ Phase 4.9.2 변경사항**: `AbsolutePointingPad.kt`에 private 헬퍼 `sendAbsolutePosition(ratio: TouchRatio)`가 이미 있으며, `buttons`를 항상 `0x00u`로 하드코딩해 `FrameBuilder.buildAbsolutePositionCommand()`를 호출한다. 본 Phase에서 드래그 모드 상태(bit0)를 반영하려면 `sendAbsolutePosition(ratio: TouchRatio, buttons: UByte)`처럼 시그니처를 확장하고, DOWN/MOVE 호출부(PointingArea `awaitEachGesture` 내부) 양쪽에 드래그 모드 ON 시의 bit0 값을 전달하도록 수정해야 한다. `targetMonitor`는 `AbsolutePointingConstants.DEFAULT_TARGET_MONITOR`를 그대로 사용(4.9.6 전까지).
>
> **⚠️ Phase 4.9.3 변경사항**: `PointingArea`의 `pointerInput` 안에 엣지존 파이프라인(존 단위 gate, armed/disarm, 엣지 띠 탭=좌표클릭)이 추가되어 클릭 버튼 프레임 전송 지점이 두 곳(엣지 후보 탭 / 일반 탭)으로 늘었다. 드래그 모드 buttons 배선 시 이 두 지점 모두 반영해야 한다. `AbsolutePointingPad`/`PointingArea` 시그니처에 `edgeZoneAssignment`/`onEdgeZoneAssignmentChange`/`onRestorePrevious`/`onSendShortcut`/`onSendMacro`/`onMouseHoldToggle`/`onCyclePage`/`onJumpToPage`가 이미 추가됐으므로, 드래그 모드 상태(트랜지언트, `heldMouseButtons`와 별개)는 이 파라미터들과 별도로 `AbsolutePointingPad` 내부 로컬 상태로 추가하면 된다. 엣지 액션의 `MouseHoldToggle`(영구 홀드)과 드래그 모드(제스처 스코프 트랜지언트)는 서로 다른 상태이므로 혼동 주의.

**개발 기간**: 완료 (2026-07-09)

> **⚠️ 구현 확정 사항(2026-07-09, 유저 확정)**: 엣지존(4.9.3) 상호작용 — 드래그 ON이어도 엣지존 gate는 그대로 유지한다. DOWN이 엣지 존(화이트리스트 통과 존)이면 기존 엣지 파이프라인(전송 억제)이 우선하고, DOWN이 일반 영역일 때만 드래그 press를 시작한다. 엣지 후보가 도중 취소(`inwardMoved < 0f`/`perpMoved` 조건)되어 일반 포인팅으로 전환되면 그 전환 시점부터 press(`dragPressed = true`)를 시작한다. 엣지 띠 탭(UP 시 armed 아니고 탭 조건)은 드래그 상태와 무관하게 기존 좌표클릭 경로 그대로(드래그 press는 "일반 영역 DOWN"에서만 발생하므로 상호 충돌 없음).
>
> `sendAbsolutePosition(ratio)`는 이 Phase에서 `sendAbsolutePosition(ratio, buttons: UByte = 0x00u)`로 확장됐다(`AbsolutePointingPad.kt`). 4.9.5(모니터 셀렉터)에서 `targetMonitor` 파라미터가 추가될 때 이 시그니처를 다시 확장하게 된다.
>
> `TouchpadState`(`TouchpadMode.kt`)에 `dragMode: Boolean = false` 필드 신규 추가(제스처 스코프 트랜지언트, `heldMouseButtons`의 영구 홀드와 별개). 테두리/EdgeBumpOverlay 색상 우선순위는 `dragMode(초록) > clickMode(핑크/노랑)`로 확정 — 4.9.6(줌, 주황) 구현 시 이 우선순위 체인에 줌을 추가로 끼워 넣어야 한다.

**실제 구현 파일**:
- `ui/components/touchpad/TouchpadMode.kt` — `TouchpadState.dragMode: Boolean = false` 필드 추가
- `ui/components/touchpad/ControlButtonContainer.kt` — DragModeButton 슬롯(7번째) 활성화: `enabled` 제거, `backgroundColor`를 `dragMode ? ColorGreen : ColorBlue`로, `onClick`에서 `onStateChange(touchpadState.copy(dragMode = !touchpadState.dragMode))`
- `ui/components/AbsolutePointingPad.kt` — `sendAbsolutePosition(ratio, buttons)` 시그니처 확장, `PointingArea`의 `pointerInput` 키에 `localState.dragMode` 추가, 제스처 로컬 `dragPressed` 상태로 DOWN(일반 영역)/MOVE(엣지 취소 전환 포함)/UP 세 지점에서 buttons bit0 press/유지/release 시퀀스 배선, UP 분기에서 `dragPressed` 우선 처리로 클릭 판정과 상호배타, 테두리/EdgeBumpOverlay 색상에 `dragMode` 우선순위 추가

> **⚠️ 실기기 확인 후 UI 조정(2026-07-09)**: 초기 구현의 라벨 "드래그"(1줄)가 다른 활성 버튼(전부 2줄 라벨)과 달리 텍스트-아이콘 간격이 벌어져 보이는 문제 발견 → OFF일 때 "이동\n모드", ON일 때 "드래그\n모드"로 2줄 토글 텍스트로 변경. 아이콘도 기존 `ic_drag_mode.xml`(드래그 핸들 점 4개, 재정렬 아이콘에 더 가까움)이 드래그 의미를 직관적으로 전달하지 못해 4방향 화살표 교차 아이콘(Material "open_with" 스타일, 24dp 벡터)으로 교체(파일명은 유지, 내용만 변경). 기본(OFF, "이동 모드") 배경색도 일반 터치패드 제어 버튼의 기본색(파랑)을 그대로 물려받은 것이었으나, 절대좌표 패드 고유 팔레트(핑크/노랑/초록)와 통일감을 위해 빨강(`TouchpadColorRed`, 기존 무한 스크롤 색상 재사용)으로 변경. 이후 DragModeButton뿐 아니라 절대좌표 패드의 모든 제어 버튼(ClickModeButton의 "좌클릭 모드" 기본 상태, ZoomButton)도 동일하게 빨강이어야 한다는 요청에 따라, `ControlButtonContainer`에 `baseColor: Color = ColorBlue` 파라미터를 신규 추가(일반 터치패드 페이지는 기본값 파랑 유지, 회귀 없음) — ClickModeButton의 `else` 분기/ZoomButton/DragModeButton의 `ColorBlue`/`ColorRed` 리터럴을 모두 `baseColor`로 교체하고, `AbsolutePointingPad.kt`의 `ControlButtonContainer` 호출부에서 `baseColor = TouchpadColorRed` 전달.
>
> **⚠️ 텍스트/색상 로직 정정(2026-07-09)**: 최초 구현이 DragModeButton에 "현재 상태"를 표시(OFF일 때 "이동 모드")하도록 되어 있었으나, `ClickModeButton`은 "지금 누르면 전환될 목적지 모드"를 라벨/색으로 미리보기하는 것이 이 앱의 제어 버튼 컨벤션(예: LEFT_CLICK 상태일 때 버튼은 "우클릭 모드"를 노랑으로 표시)이다. DragModeButton도 동일 컨벤션에 맞춰 정정: OFF(이동 모드, 테두리 빨강)일 때 버튼은 목적지인 "드래그\n모드"를 초록으로, ON(드래그 모드, 테두리 초록)일 때 버튼은 목적지인 "이동\n모드"를 `baseColor`(빨강)로 표시하도록 텍스트·색상 분기를 서로 뒤집었다.
>
> **⚠️ 아이콘 분화(2026-07-09)**: 아이콘도 텍스트/색상과 동일한 목적지 조건으로 분기하도록 `ic_move_mode.xml`(단순 커서/포인터 화살표, 신규) 추가 — OFF(목적지 "드래그 모드")일 때는 기존 4방향 화살표 `ic_drag_mode.xml`, ON(목적지 "이동 모드")일 때는 `ic_move_mode.xml`을 표시.

**참조 문서**:
- `docs/android/technical-specification-app.md` §2.10.5 (드래그 앤 드롭 모드 상세)
- `docs/windows/technical-specification-server.md` §3.6.9.4 (서버 측 buttons diff 처리, 후속 구현 참고용)

**검증**:
- [x] DragModeButton 탭 → 테두리 초록색 전환
- [x] 드래그 모드 OFF에서 buttons 항상 0 (기존 `sendAbsolutePosition` 기본값 유지로 회귀 없음)
- [x] 드래그 모드 ON에서 제스처별 buttons 시퀀스(down=1, move=1, up=0) 코드 배선 완료
- [x] 드래그 모드와 클릭 판정 상호 배타 확인 (UP 분기에서 `dragPressed` 최우선 처리)
- [x] 빌드 성공(`assembleDebug` 컴파일 에러 없음, 신규 경고 없음)
- [x] 실기기: 실제 drag&drop 동작, 테두리 전환, 엣지존 회귀 없음 확인 완료

---

## Phase 4.9.5: 모니터 셀렉터 + 역방향 개수 수신 (Android 측)

**목표**: 유저가 매핑 대상 모니터를 선택할 수 있는 셀렉터 UI + 역방향 모니터 개수 수신

**개발 기간**: 0.5일

**세부 목표**:
1. `MonitorSelector` UI: "전체" 칩 + 모니터 개수만큼 번호 칩. 모니터 개수 ≥ 2일 때만 노출. 상시 노출되는 라이브 셀렉터로, ClickModeButton처럼 사용 중 언제든 전환 가능(사전 선택 게이트 아님)
2. 선택값을 `buildAbsolutePositionCommand()`의 `targetMonitor` 바이트로 전달
3. `NotificationFrame`에 `EVENT_MONITOR_COUNT (0x03)` 이벤트 타입 추가, `[0xFE, 0x03, monitor_count]` 파싱
4. `UsbSerialManager` 역방향 파서에 신규 이벤트 처리 추가, `monitor_count: StateFlow<Int>` 노출
5. `monitor_count` 미수신 시 단일 모니터로 가정하고 셀렉터 숨김
6. **기본값 및 영속화**(사용자 확정): 마지막 선택값을 SharedPreferences에 저장하고 다음 진입 시 복원. 저장값 없음(최초 진입) 또는 저장된 인덱스가 현재 `monitor_count`를 초과(모니터 구성 변경)하면 `targetMonitor = 0x01`(주 모니터)로 폴백

**신규/수정 파일**:
- `protocol/NotificationFrame.kt` — `EVENT_MONITOR_COUNT` 추가
- `usb/UsbSerialManager.kt` — 역방향 파서 확장, `monitorCount: StateFlow<Int>` 노출
- `ui/components/AbsolutePointingPad.kt` — MonitorSelector 배치 + 선택값 영속화 + `sendAbsolutePosition`/`PointingArea` targetMonitor 파라미터 배선
- `ui/components/MonitorSelector.kt` (신규) — 칩 UI(전체/1..N), 커스텀 칩(선택=`TouchpadColorYellow`/비선택=`TouchpadColorRed`)
- `ui/common/MonitorSelectorPrefs.kt` (신규) — `loadTargetMonitor`/`saveTargetMonitor` (SharedPreferences, `PadLabelPrefs.kt`와 동일 top-level 함수 헬퍼 컨벤션)
- `ui/utils/MonitorSelectorLogic.kt` (신규) — `resolveTargetMonitor()` 순수 함수로 폴백 규칙 분리(단위테스트 목적)
- `ui/utils/AbsolutePointingConstants.kt` — `TARGET_MONITOR_ALL = 0x00u` 추가, `DEFAULT_TARGET_MONITOR` 주석에서 "Phase 4.9.6" 오기 정정(폴백 값으로 계속 사용)

> **✅ Phase 4.9.2 변경사항 반영 완료**: `sendAbsolutePosition(ratio, buttons, targetMonitor)`로 시그니처 확정. `targetMonitor` 파라미터는 기본값 없이 필수이며, 호출측(`PointingArea`)이 `rememberUpdatedState(targetMonitor)`로 캡처한 최신 셀렉터 선택값을 항상 명시적으로 전달한다.
>
> **UI 배치(사용자 확정)**: `ControlButtonContainer`와 같은 행(`Row`) 우측에 배치. `ControlButtonContainer`는 `Modifier.weight(1f)`로 남은 폭을 채우고, `MonitorSelector`는 `monitorCount >= 2`일 때만 그 오른쪽에 노출된다.

**참조 문서**:
- `docs/android/technical-specification-app.md` §2.10.6 (모니터 셀렉터 설계, 기본값/영속화 규칙)
- `docs/technical-specification.md` §2.4.6.1.3 (`EVENT_MONITOR_COUNT` 프레임 규격)

**검증**:
- [x] `EVENT_MONITOR_COUNT` 프레임 파싱 단위테스트 (`NotificationFrameTest.kt`)
- [x] 모니터 개수 1일 때 셀렉터 숨김, 2 이상일 때 노출 (`monitorCount >= 2` 조건부 렌더링)
- [x] 칩 선택 시 `targetMonitor` 값이 다음 전송 프레임에 반영되는지 단위테스트 (`FrameBuilderTest.testBuildAbsolutePositionCommandButtonsAndTargetMonitor`, Phase 4.9.2에서 이미 검증된 f[7] 배선을 그대로 재사용)
- [x] 최초 진입(저장값 없음) 시 주 모니터(0x01)로 폴백되는지 단위테스트 (`MonitorSelectorLogicTest.testNoSavedValueFallsBackToPrimaryMonitor`)
- [x] 마지막 선택값이 앱 재시작 후에도 복원되는지 단위테스트 (`MonitorSelectorLogicTest.testSavedValidIndexRestored`, 모니터 구성 변경 시 폴백은 `testSavedIndexExceedingMonitorCountFallsBack`)
- [x] 빌드 성공(`assembleDebug` 컴파일 에러 없음, 신규 경고 없음)
- [ ] 실기기 필요: 실제 모니터 개수 통지(펌웨어·서버 완성 후) — Android 측 수신·UI·영속화 경로는 완성, 실동작 검증은 보류

---

## Phase 4.9.6: 줌 기능 구현

**목표**: 드래그 기반 줌 진입 + 줌 상태 좌표 변환 + 줌 해제

**개발 기간**: 1일

**세부 목표**:
1. **ZoomButton 활성화** (`showZoom = true`, DPI 슬롯 자리):
   - 탭: 줌 모드 진입 (줌 활성 시 재탭 → 1x 해제)
   - Selected 상태: 배율 배지 표시 (예: "2x")
2. **줌 진입 인터랙션**:
   - ZoomButton 탭 → 줌 모드 진입 → PointingArea 위 중심점 터치 → 유지한 채 바깥으로 드래그 → 드래그 거리 비례 줌 레벨 증가 → 손 떼기 → 확정
   - ⚠️ 제스처 충돌 규칙: 드래그가 `EdgeSwipeConstants.EDGE_HIT_WIDTH_DP` 이내의 엣지 히트 영역에 들어가도 엣지존 트리거로 전환되지 않도록 줌 드래그 중에는 엣지존 인식 억제. 반대로 엣지 히트 영역에서 터치가 시작된 경우는 줌 드래그로 인식하지 않고 엣지존이 우선
3. **드래그 거리 → 줌 레벨 매핑**: 0dp→1x, 50dp→2x, 100dp→4x, 150dp+→8x(최대), 선형 보간
4. **줌 상태 좌표 변환**: `ratio' = zoomMinRatio + ratio * (zoomMaxRatio - zoomMinRatio)`, 경계 클램핑(0.0~1.0)
5. **줌 해제**: ZoomButton 재탭 → 1x 복귀
6. **시각 피드백**: 테두리 주황색(`#FF9800`), 줌 레벨 텍스트(PointingArea 우상단)
7. **상태 보존**: 페이지 전환 시 줌 레벨/중심점 유지

**개발 기간**: 완료 (2026-07-10)

**실제 구현 파일**:
- `ui/utils/AbsolutePointingConstants.kt` — 줌 상수 추가(`ZOOM_LEVEL_MIN/MAX`, `ZOOM_DRAG_DP_2X/4X/8X`, `ZOOM_LEVEL_TEXT_SIZE_SP`, `ZOOM_LEVEL_TEXT_PADDING_DP`)
- `ui/utils/AbsoluteCoordinateCalculator.kt` — `data class AbsoluteZoomState(level, centerX, centerY)`(신규, `isActive` 프로퍼티 포함), `dragDistanceToZoomLevel(dp)`(구간별 선형 보간), `applyZoom(ratio, zoom)`(축 독립 재매핑, 경계 클램핑) 추가
- `ui/components/touchpad/TouchpadColors.kt` — `TouchpadColorZoom = Color(0xFFFF9800)` 추가(기존 `TouchpadColorOrange` #FF8A00, 직각이동용과는 별개 색상)
- `ui/components/touchpad/ControlButtonContainer.kt` — `zoomLevel: Float = 1f`, `onZoomClick: (() -> Unit)? = null` 파라미터 추가. ZoomButton 슬롯을 Disabled 스텁에서 활성화(DragModeButton과 동일한 목적지 미리보기 패턴): OFF(1x)일 때 주황 + "줌\n모드"(`ic_zoom`), ON(>1x)일 때 `baseColor` + "풀사이즈\n모드"(`ic_fullscreen`, 신규 아이콘)
- `res/drawable/ic_fullscreen.xml`(신규) — 줌 ON 상태 목적지("풀사이즈 모드") 아이콘. 모서리 4방향 확장 화살표(Material "fullscreen" 스타일), `ic_zoom.xml`/`ic_move_mode.xml`과 동일한 24dp 벡터 컨벤션

> **⚠️ 실기기 확인 후 텍스트/아이콘 조정(2026-07-10, 유저 확정)**:
> 1. 초기 구현은 OFF 라벨을 1줄("줌")로 뒀으나, `ControlButton` 내부는 `Text`(실제 줄 수만큼만 높이
>    차지) + `Spacer(weight=1f)` + `Icon` 순서 Column이라, 1줄 라벨은 Spacer가 커져 아이콘이 다른 2줄
>    라벨 버튼(우클릭 모드/드래그 모드)보다 아래로 밀려 보이는 문제가 있었다(Phase 4.9.4 DragModeButton
>    초기 구현과 동일 이슈, §288 참조). OFF 라벨을 "줌\n모드"(2줄)로 통일해 해결.
> 2. ON 상태 라벨을 설계 문서 §4.5.6(배율 배지 "2x")이 아닌 **목적지 모드 이름**으로 확정. 처음
>    "전체화면"(§4.5.1 "패드 = PC 전체 화면" 표현과 일치)을 제안했으나, 다른 제어 버튼이 전부 "~모드"
>    접미사를 쓰는 것과 통일하기 위해 최종 "풀사이즈 모드"로 확정(유저 선택). 정확한 배율 수치는 이미
>    `AbsolutePointingPad.kt`의 PointingArea 우상단 오버레이가 실시간으로 표시하므로, 버튼 자체는 다른
>    제어 버튼(우클릭 모드/드래그 모드)과 동일하게 "탭하면 갈 목적지 모드 이름"만 표시하는 것으로
>    일관성을 맞췄다. 아이콘도 목적지에 맞춰 `ic_fullscreen.xml`(신규)로 분리(DragModeButton의
>    `ic_move_mode.xml` 선례와 동일 패턴).
- `ui/components/AbsolutePointingPad.kt` — `zoomState: AbsoluteZoomState`, `onZoomStateChange` 파라미터 추가(hoisted). 로컬 트랜지언트 `zoomArming`(ZoomButton 탭 후 패드 터치 대기 상태). `PointingArea`의 DOWN/MOVE/UP 세 지점에 줌 정의 모드(`zoomDefining`) 배선 — DOWN 시 arming이면 중심점 기록·좌표 전송 억제(엣지 후보면 엣지존이 우선이라 줌 진입 안 함), MOVE 시 드래그 거리→레벨 실시간 갱신, UP 시 확정(arming 해제). 좌표 전송 6개 호출부를 로컬 헬퍼 `sendZoomed(ratio, buttons)`(줌 매핑 적용 후 전송)로 교체. `borderColor`/`bumpColor` when 체인을 설계 §4.5.7 우선순위(드래그 초록 > 우클릭 노랑 > 줌 주황 > 기본 핑크)로 확장. PointingArea 우상단에 줌 레벨 텍스트 오버레이 추가(`zoomState.isActive`일 때만 표시)

> **⚠️ 실기기 확인 후 피드백 추가(2026-07-10)**: ZoomButton을 눌러도 "모드가 안 바뀐다"는 문제 발견 —
> 원인은 로직 버그가 아니라 시각 피드백 누락. `zoomArming`이 true가 돼도(패드 터치 대기 상태)
> `borderColor`/`bumpColor`는 `zoomState.isActive`만 봐서 실제로 패드를 터치·드래그하기 전까지는
> 화면에 아무 변화가 없었다. 두 when 체인의 줌 분기 조건을 `zoomState.isActive || zoomArming`으로
> 확장해, 버튼을 누르는 즉시 테두리가 주황으로 바뀌어 "줌 대기 중"임을 알 수 있게 했다.
>
> **⚠️ 후속 실기기 확인(2026-07-10)**: 테두리는 바뀌는데 "제어 버튼 자체가 안 바뀐다"는 재확인 —
> `ControlButtonContainer.kt`의 ZoomButton 슬롯은 `zoomLevel > 1f`만으로 ON/OFF를 판정해서, arming
> 상태(탭 직후, 아직 패드 미터치)에서는 버튼 배경/라벨이 그대로였다. 다른 제어 버튼(드래그 모드 등)은
> 탭 즉시 버튼 자체가 바뀌는 것과 대비돼 혼동을 줬다. `ControlButtonContainer`에 `zoomArming: Boolean
> = false` 파라미터를 추가하고 `zoomActive = zoomLevel > 1f || zoomArming`으로 판정을 통일(`onZoomClick`
> 토글 로직이 이미 `isActive || arming`을 하나로 취급하는 것과 대칭). `AbsolutePointingPad.kt`의
> `ControlButtonContainer` 호출부에 `zoomArming = zoomArming` 전달.
- `ui/pages/standard/Page3AbsolutePointing.kt` / `ui/pages/StandardModePage.kt` — `zoomState`/`onZoomStateChange` 관통 배선. `page3Assignment`와 동일하게 페이저 바깥 `remember`로 hoisting(`page3ZoomState`). **SharedPreferences 영속화는 하지 않음** — 페이지 전환에만 유지, 앱 재시작 시 1x로 리셋(인메모리, 유저 미확정 사항이라 최소 범위로 구현)
- `app/src/test/.../AbsoluteCoordinateCalculatorTest.kt` — `dragDistanceToZoomLevel`/`applyZoom`/`AbsoluteZoomState.isActive` 단위테스트 추가

> **⚠️ 확정 흐름 변경(2026-07-10, 유저 확정)**: 원래 설계(§4.5.2, §361)는 "드래그 유지 → 손 떼기 →
> 즉시 확정"이었으나, 손 떼는 타이밍을 정밀 제어하기 어려운 근육장애 사용자 접근성을 고려해 **"드래그로
> 정의 → 손 떼면 확정 대기 → 별도의 원탭으로 확정"** 2단계 흐름으로 변경. `AbsolutePointingPad.kt`에
> `zoomAwaitingConfirm: Boolean`(hoisted, `zoomArming`과 함께 존재) 신규 추가:
> - 1번째 터치(정의 드래그): DOWN에서 중심점 기록, MOVE에서 드래그 거리→레벨 실시간 갱신(기존과 동일),
>   UP에서는 더 이상 즉시 확정하지 않고 `zoomAwaitingConfirm = true`로 전환(arming은 유지, 레벨/중심은
>   그 시점 값으로 고정)
> - 2번째 이후 터치(확정 대기 중 재터치): `zoomArming && zoomAwaitingConfirm`일 때 새 DOWN은
>   `zoomAdjusting`(제스처 로컬)으로만 표시하고 좌표/커서 전송을 억제, 기존 확정 후보 레벨은 유지.
>   MOVE에서 이동 거리가 `CLICK_MAX_MOVEMENT_DP`를 넘으면 즉시 `zoomDefining`으로 전환해 재정의 드래그로
>   취급(이 터치의 DOWN 위치를 새 중심점으로, 레벨을 처음부터 다시 계산 — **확정 전에는 몇 번이든
>   다시 드래그해서 조절 가능**, 유저 확정 2026-07-10). 반대로 임계값을 넘지 못한 채 UP에 도달하면
>   탭으로 판정(`isTapGesture`, 기존 클릭 판정 재사용)해 그 시점의 후보 레벨을 그대로 확정
>   (`zoomArming = false`, `zoomAwaitingConfirm = false`). 탭도 재정의 드래그도 아니면(짧은 이동 없는
>   롱프레스 등) 무시하고 대기 상태 유지
> - ZoomButton 재탭으로 인한 취소(`onZoomClick`)는 arming/확정 대기 여부와 무관하게 항상 전체 리셋
> - 줌 레벨 텍스트: `zoomArming`이 true인 동안(정의 중 + 확정 대기 중 전부)은 화면 정가운데 큰 글씨
>   (`ZOOM_LEVEL_CENTER_TEXT_SIZE_SP=48sp`)로 표시, 확정 대기 중에는 그 아래 "탭하여 확정" 안내 문구
>   추가. 확정 완료 후(정상 사용 중, `zoomState.isActive && !zoomArming`)에는 기존 설계(§4.5.4)대로
>   우상단 작은 텍스트(`ZOOM_LEVEL_TEXT_SIZE_SP=14f`)로 전환
> - `AbsolutePointingConstants.kt`에 `ZOOM_LEVEL_CENTER_TEXT_SIZE_SP`, `ZOOM_CONFIRM_HINT_TEXT_SIZE_SP` 추가

> **⚠️ 색상 우선순위 정정**: 이 섹션 상단(§281, Phase 4.9.4 기록)에는 "줌을 최상위 우선순위로 끼워 넣는다"는 모호한 표현이 있었으나, 설계 문서(`component-design-guide-app.md` §4.5.7)가 정확한 순서를 명시한다 — **드래그 ON(초록) > 우클릭(노랑) > 줌 활성(주황) > 기본 좌클릭(핑크)**. 구현은 설계 문서 기준을 따랐다.

> **⚠️ Phase 4.9.4 변경사항**: `ControlButtonContainer`에 `baseColor: Color = ColorBlue` 파라미터가 추가됐고, `AbsolutePointingPad.kt`는 `baseColor = TouchpadColorRed`를 전달한다(절대좌표 패드 고유 팔레트 통일). `ZoomButton` 슬롯은 이미 `backgroundColor = baseColor`로 배선되어 있으니(Disabled 스텁 상태) 활성화 시 그대로 재사용하면 된다. 또한 이 앱의 제어 버튼은 "지금 누르면 전환될 목적지 모드"를 라벨/색으로 미리보기하는 컨벤션(`ClickModeButton`/`DragModeButton` 선례)을 따른다 — ZoomButton도 OFF(1x) 상태에서는 진입 목적지를 암시하는 라벨/색(예: 주황 계열 배지 예고)을, ON(줌 활성) 상태에서는 해제(복귀) 목적지를 `baseColor`(빨강)로 보여주는 방향으로 설계할 것. 테두리는 버튼과 반대로 "현재 상태"를 표시(클릭모드 보더와 동일 원칙)하므로 혼동 주의.

**참조 문서**:
- `docs/android/component-design-guide-app.md` §4.5 (줌 기능, Region Zoom)

**검증**:
- [x] ZoomButton 탭 → 줌 모드 진입(arming), 재탭/줌 활성 중 재탭 → 1x 해제(코드 배선 완료)
- [x] 드래그 거리에 비례한 줌 레벨 증가(`dragDistanceToZoomLevel` 단위테스트 검증)
- [x] 줌 상태 좌표 변환(`applyZoom` 단위테스트 검증, 경계 클램핑 포함)
- [x] 줌 해제 (1x 복귀) 코드 배선 완료
- [x] 테두리 주황색 전환, 줌 레벨 텍스트 표시 코드 배선 완료
- [x] 빌드 성공(`assembleDebug` 컴파일 에러 없음, 신규 경고 없음)
- [ ] 실기기: 줌 진입/드래그/확정/해제 실동작, 포인팅 정밀도 향상 체감, 페이지 전환 후 줌 유지, 엣지존 회귀 없음 확인 필요

---

## Phase 4.9.7: Vendor CDC 줌 상태 UART 전송 (Android 측)

**목표**: Android에서 줌 상태(zoom_level, 매핑 범위, targetMonitor)를 UART 커스텀 명령으로 ESP32에 전송하는 부분까지 구현. ESP32 중계 및 PC 오버레이는 범위 밖.

**개발 기간**: 완료 (2026-07-10)

**세부 목표**:
1. **Android → ESP32 줌 상태 전송**:
   - 0xFF 커스텀 명령(`VCDC_CMD_ZOOM_STATE = 0x30`)으로 UART 전송 ✅
   - JSON payload: `zoom_level`, `min_x`, `min_y`, `max_x`, `max_y`, `target_monitor` ✅
   - 전송 시점: 줌 확정 시 1회, 드래그 중 30Hz 스로틀, 해제 시 1회 ✅

> **⚠️ 구현 확정 사항(2026-07-10)**: 조사 결과 `UsbSerialManager`의 sender 스레드는 큐에서 꺼낸 `ByteArray`를 통짜 `port.write()`로 전송하며 8바이트 가정 로직이 전혀 없었다. 8바이트 제약은 진입 API(`sendCommandBytes`/`sendFrame`)의 `check`에만 있었으므로, 기존 API를 완화하는 대신 **신규 진입점 `UsbSerialManager.sendVendorCdcFrame(ByteArray)`를 추가**했다(같은 `frameQueue` 공유, 크기 상한만 `UsbConstants.VENDOR_CDC_MAX_FRAME_SIZE=454`로 검증). 이렇게 8바이트 델타/절대좌표 프레임의 안전장치(정확히 8바이트 강제)를 그대로 유지했다.
>
> **⚠️ 계획 문서와 다른 구현(빌더 위치)**: 아래 원래 "수정 파일" 목록은 `protocol/FrameBuilder.kt`에 줌 커스텀 명령 생성 로직을 넣는 것으로 계획했으나, `FrameBuilder`는 8바이트 바이너리+시퀀스 관리 전용이라 JSON/CRC 로직이 전혀 없었고, 프로젝트에는 이미 JSON 커맨드 페이로드를 `protocol/` 하위 독립 파일에 두는 선례(`protocol/MultiCursorCommand.kt`, org.json 사용)가 있어 유저 확정 하에 **신규 `protocol/ZoomStateCommand.kt`**로 구현했다. `FrameBuilder.kt`는 이 Phase에서 수정하지 않았다.
>
> CRC16-CCITT(다항식 0x1021, 초기값 0x0000, payload만 대상)는 펌웨어 `src/board/BridgeOne/main/vendor_cdc_handler.c`의 `vendor_cdc_crc16()`을 그대로 Kotlin 포팅했다(`ZoomStateCommand.crc16Ccitt`).

**실제 구현 파일**:
- `protocol/ZoomStateCommand.kt` (신규) — `buildPayload()`(JSON, org.json), `crc16Ccitt()`, `frame()`(`[0xFF][0x30][len LE][payload][CRC16 LE]` 조립), `buildFrame(zoom, targetMonitor)`(호출측 편의 함수)
- `usb/UsbSerialManager.kt` — `sendVendorCdcFrame(ByteArray)` 신규 public API (`sendCommandBytes` 아래, 동일 `frameQueue` 공유)
- `usb/UsbConstants.kt` — `VENDOR_CDC_MAX_FRAME_SIZE = 454` 신규 상수
- `ui/utils/AbsoluteCoordinateCalculator.kt` — `ZoomMappingRange` data class + `calculateZoomMappingRange(zoom)` 순수 함수 신규(`applyZoom`과 동일 윈도우 계산을 절대좌표 스케일 0~32767 정수로 인코딩), `ABS_COORDINATE_MAX=32767` 상수
- `ui/utils/AbsolutePointingConstants.kt` — `ZOOM_STATE_THROTTLE_MS = 33L`(30Hz 상한) 신규
- `ui/components/AbsolutePointingPad.kt` — top-level 헬퍼 `sendZoomStateFrame(zoom, targetMonitor)` 신규. 전송 3지점 배선: (A) `zoomDefining` UP 분기(확정 1회, 스로틀 무시), (B) `updateZoomLevelFromDrag()` 내부(드래그 중 `System.currentTimeMillis()` 기반 30Hz 스로틀, 제스처 로컬 `lastZoomTxMs`/`pendingZoomState`), (C) `onZoomClick` 해제 분기(1x 해제 1회). `onZoomStateChange` 자체는 전역 래핑하지 않음(정의 시작 콜백은 전송 대상 아님)
- `protocol/ZoomStateCommandTest.kt` (신규 테스트) — CRC16 골든 벡터(CRC-16/XMODEM 표준 체크값 `0x31C3`), 프레임 레이아웃, payload 필드, 크기 상한 예외
- `ui/utils/AbsoluteCoordinateCalculatorTest.kt` (테스트 추가) — `calculateZoomMappingRange` 4건(1x 전체범위/level 2·8/경계 클램핑)

**참조 문서**:
- `docs/technical-specification.md` §2.4.6.1.2 (줌 상태 Vendor CDC 메시지, JSON payload 스펙, `target_monitor` 필드 추가됨)

**검증** (Android 단독으로 완결 가능):
- [x] 줌 확정 시 UART로 줌 상태 전송(`target_monitor` 포함) — 코드 배선 완료
- [x] 줌 해제 시 zoom_level=1.0 전송 — 코드 배선 완료
- [x] CRC16-CCITT 펌웨어 구현과 정합성(골든 벡터 단위테스트 통과)
- [x] 프레임 레이아웃/payload 필드 단위테스트 통과 (`ZoomStateCommandTest` 10건)
- [x] min/max 매핑 범위 인코딩 단위테스트 통과 (`AbsoluteCoordinateCalculatorTest` 4건 추가)
- [x] 빌드 성공(`assembleDebug` 컴파일 에러 없음, 신규 경고 없음, `testDebugUnitTest` 전체 통과)

**후속 통합 Phase에서 검증할 항목** (범위 밖):
- [ ] ESP32가 UART 수신 → Vendor CDC Frame으로 투명 중계
- [ ] Windows 서버 연동 시 PC 화면에 대상 모니터 기준 줌 영역 박스 표시 (실기기 검증)
- [ ] 실기기: logcat 기준 드래그 중 0xFF/0x30 프레임 ~30Hz, 확정/해제 시 각 1회 전송 동작 확인

---

## Phase 4.9.8: 엣지존 설정 화면 연동 (필터링 적용)

**목표**: 절대좌표 패드(Page 3)를 엣지존 편집 대상에 추가하고, 편집기에서 절대좌표에 무의미한 모드/액션이 노출되지 않도록 필터링. `JumpToPage` 인덱스 시프트 마이그레이션 포함.

**개발 기간**: 0.5일

> **코드 조사 결과 요약**: 현재 `StandardModePage.kt`의 `standardTouchpadPages = listOf(0, 1)`에는 Page 3(인덱스 2)가 빠져 있어 엣지존 할당 대상 자체가 아니다. 액션/모드 필터 파라미터(`ZoneActionPicker.kt`의 `excludeDomains: Set<ActionDomain>`)는 이미 존재하지만 `EdgeZoneEditorScreen.kt`에 배선돼 있지 않아 현재는 12개 `ActionDomain` 전부가 항상 노출된다. 이 Phase는 신규 UI 컴포넌트를 만드는 게 아니라 **기존 필터 파라미터를 관통 배선**하는 작업이다.
>
> **캔버스 재사용 확인(수정 불필요)**: `EdgeZoneEditorPreviewCanvas.kt`/`EdgeZoneCanvasGeometry.kt`/`EdgeZoneCanvasGestures.kt`/`EdgeZoneCanvasModeButtons.kt`/`EdgeZoneCanvasModeBars.kt`/`EdgeZoneCanvasRatioPanel.kt`/`EdgeZoneCanvasModeOverlay.kt` 7개 파일은 존 분할/병합/이동/삭제/비율조정 등 "편집기 UI 조작"만 다루고 `EdgeSwipeMode`/`EdgeZoneAction`을 전혀 참조하지 않는다. Page 3 전용 수정 없이 그대로 재사용 가능.
>
> **⚠️ Phase 4.9.3 변경사항**: `ui/components/touchpad/AbsolutePadActionFilter.kt`(신규)에 런타임용 `isAbsolutePadAllowed(action: EdgeZoneAction): Boolean` 액션 타입 단위 화이트리스트가 이미 구현되어 있다(`ToggleMode(CURSOR)` false 처리 포함, 단위테스트 검증됨). 본 Phase 세부목표 3의 "CURSOR 예외 처리 필터"는 이 함수를 그대로 재사용하거나 최소 수정으로 활용할 것 — 새로 만들 필요 없음. 단, 런타임 필터(`filterConfigForAbsolutePad`)는 Unassigned 치환 방식이라 편집기의 "선택 옵션에서 아예 숨기기" 요구사항(`excludeActions`/`excludeDomains`)과는 용도가 다르므로 `isAbsolutePadAllowed` 판정 로직만 가져오고 배선은 별도로 한다.

**세부 목표**:
1. **Page 3를 엣지존 편집 대상에 포함**: `standardTouchpadPages`에 `2` 추가. `standardAssignments` 초기화/로드 로직은 이미 제너릭하게 동작해 자동 포함
2. **`excludeDomains` 필터를 `EdgeZoneEditorScreen`까지 관통 배선**: 시그니처에 `excludeDomains: Set<ActionDomain> = emptySet()` 추가, `ActionDomainPicker`/`ZoneRotationEditor` 호출부 양쪽에 전달. `StandardModePage.kt`의 호출부에서 `selectedZonePage == 2`일 때 `excludeDomains = setOf(ActionDomain.MOVE, ActionDomain.DPI, ActionDomain.DYNAMICS, ActionDomain.SCROLL, ActionDomain.SCROLL_SPEED)` 전달
3. **CURSOR 예외 처리**: `EdgeZoneActionResolver.kt`에서 `EdgeSwipeMode.CURSOR → ActionDomain.CLICK` 매핑이라 `ActionDomain.CLICK`을 통째로 제외하면 좌/우클릭까지 사라짐. CursorMode는 이미 완전 배제 결정이므로 도메인 단위가 아닌 세밀한 필터(`excludeActions: Set<EdgeZoneAction>` 신규 파라미터 등) 필요 — 구현 시점 결정
4. **`zoneEditorDisabledEdges` 검토**: Page 3 전용 분기 필요 여부 확인
5. **`JumpToPage` 저장값 마이그레이션**: `EdgeZoneJson.kt`가 정수 그대로 직렬화하므로, PAGE_COUNT 5→6 확장 + 인덱스 2 삽입 시 기존 `pageIndex >= 2`인 값은 의미가 어긋난다. 1회성 마이그레이션 로직(`pageIndex + 1` 이동) + 중복 실행 방지 버전 플래그(예: "page_index_migrated_v1")
6. **로테이션 존 회전 코루틴 이식**(Phase 4.9.3에서 미이식): 4.9.3의 `PointingArea` 제스처 루프는 armed 시 로테이션 트리거를 `candidates.firstOrNull()`로 정적 처리한다(기본 config에 로테이션 존이 없어 영향 없었음). 본 Phase에서 편집 UI로 로테이션 존을 만들 수 있게 되면 `TouchpadWrapper.kt:901-913`(회전 코루틴: `rotationJob`/`rotationIndex`, `intervalMs` 간격 순환 + `CLOCK_TICK` 햅틱)을 `AbsolutePointingPad.kt`에도 이식해야 한다.

**신규 파일**: 없음 (기존 파일 배선만)

**수정 파일**:
- `ui/pages/StandardModePage.kt` (`standardTouchpadPages`, `EdgeZoneEditorScreen` 호출부, `zoneEditorDisabledEdges`, `JumpToPage` 마이그레이션 로직)
- `ui/components/touchpad/EdgeZoneEditorScreen.kt` (`excludeDomains` 파라미터 추가 및 관통 배선)
- `ui/components/touchpad/ZoneActionPicker.kt` (CURSOR 예외 처리 필터 파라미터, 필요 시)

**검증**:
- [ ] Page 6 설정에서 "페이지 3"(절대좌표) 존 편집 진입 가능
- [ ] Page 3 편집기에서 MOVE/DPI/DYNAMICS/SCROLL/SCROLL_SPEED 액션 미노출
- [ ] Page 3 편집기에서 CURSOR 관련 옵션 미노출, CLICK(좌/우클릭)은 정상 노출
- [ ] Page 1/2 편집기는 기존과 동일하게 전체 액션 노출 (회귀 없음)
- [ ] 존 구조 편집(분할/병합/이동/삭제/비율조정)은 Page 3에서도 다른 페이지와 동일하게 동작
- [ ] 마이그레이션 전 저장된 `JumpToPage(4)`(구 "설정")가 마이그레이션 후 `JumpToPage(5)`로 이동해 여전히 설정 페이지로 점프하는지 확인
- [ ] 마이그레이션이 앱 재시작 시 중복 실행되지 않는지 확인

---

## Phase 4.9.9: 다중 모드 그라디언트 테두리

> **신규 하위 Phase(2026-07-09, 유저 확정)**: 절대좌표 패드는 여러 제어 상태(클릭모드/드래그모드/줌)가 동시에 활성화될 수 있는데, 지금은 우선순위 하나(`dragMode > clickMode`, 4.9.4/4.9.6에서 각각 확정)로 단색 테두리만 표시한다. 일반 터치패드(`TouchpadColors.kt`의 `touchpadBorderColors(state: TouchpadState): Pair<Color, Color>`)처럼 여러 모드가 겹칠 때 테두리를 그라디언트로 보여주는 편이 상태 조합을 더 정확히 전달한다. 단, 이 작업은 그라디언트에 들어갈 색상 후보(핑크/노랑/초록/주황)가 전부 구현되어야 의미가 있으므로, 줌(4.9.6)까지 끝난 뒤 마지막 하위 Phase로 진행한다.

**목표**: 클릭모드/드래그모드/줌이 동시에 활성화됐을 때 PointingArea 테두리를 단색이 아닌 그라디언트로 표시해 상태 조합을 시각적으로 구분

**선행 조건**: Phase 4.9.6(줌) 완료 — 그라디언트에 들어갈 마지막 색상(주황)이 이 시점에 확정됨

> **⚠️ Phase 4.9.6 변경사항**: `borderColor`/`bumpColor` when 체인이 4분기로 확정됐다 —
> `dragMode(초록) > clickMode==RIGHT_CLICK(노랑) > zoomState.isActive(주황, TouchpadColorZoom) > else(핑크)`.
> 색상 우선순위는 설계 §4.5.7을 그대로 채택(이전 §469 기록의 "dragMode > clickMode" 표현은 줌 분기가 그
> 사이에 끼워진 것으로 갱신 필요). 그라디언트 교체 시 이 4색이 stop 후보이며, 판정 소스는
> `localState.dragMode`(Boolean), `clickMode == ClickMode.RIGHT_CLICK`(Boolean), `zoomState.isActive`
> (Boolean, `AbsoluteZoomState`) 세 개의 독립 불리언 조합이다.

**세부 목표**:
1. 기존 우선순위 단색 로직(`AbsolutePointingPad.kt`의 `borderColor`/`bumpColor` when 체인, 4.9.4/4.9.6에서 누적)을 다중 색상 그라디언트로 교체
2. `TouchpadColors.kt`의 `touchpadBorderColors(state: TouchpadState): Pair<Color, Color>` 패턴 재사용/확장 검토 — 절대좌표 패드는 클릭(2색)×드래그(2색)×줌(1색, on/off) 조합이라 기존 2색 페어보다 많은 동시 활성 색이 나올 수 있어 그라디언트 stop 개수/순서 규칙 별도 정의 필요
3. 활성 상태 우선순위 → 그라디언트 색상 순서 매핑 규칙 확정(예: 클릭모드 색이 기본 stop, 드래그 ON/줌 ON이 추가되면 보조 stop으로 삽입)
4. `EdgeBumpOverlay`의 `borderColors: Pair<Color, Color>` 파라미터도 동일 그라디언트 규칙과 일관되게 갱신
5. 애니메이션: 모드 전환 시 그라디언트가 즉시 전환될지 짧게 크로스페이드할지 결정(터치패드 기존 전환 애니메이션 유무 확인 후 맞춤)

**참조 문서**:
- `docs/android/component-design-guide-app.md` §4.3 (테두리 색상 규칙, 현재는 단일 색상표만 정의됨 — 그라디언트 조합표 보강 필요)
- `ui/components/touchpad/TouchpadColors.kt`의 `touchpadBorderColors()` (일반 터치패드 다중 모드 그라디언트 선례)

**검증**:
- [ ] 클릭모드만 활성(드래그/줌 OFF) — 기존과 동일하게 단색(핑크/노랑)
- [ ] 드래그모드 ON + 클릭모드 조합 — 그라디언트 표시
- [ ] 줌 ON + 클릭모드/드래그모드 조합 — 그라디언트 표시
- [ ] 세 상태 모두 ON — 그라디언트에 3색 모두 반영
- [ ] EdgeBumpOverlay 색상도 동일 그라디언트 규칙으로 갱신되어 시각적 불일치 없음
- [ ] Page 1/2 일반 터치패드 그라디언트 로직에 회귀 없음

---

## Phase 4.9.10: 리팩토링

> **신규 하위 Phase(2026-07-09, 유저 확정)**: Page 3(절대좌표 패드) 관련 코드 전체를 마지막에 한 번 정리한다. 4.9.1~4.9.9에 걸쳐 `AbsolutePointingPad.kt`/`ControlButtonContainer.kt` 등에 기능이 순차적으로 누적되면서 생겼을 중복·비대해진 파일·임시방편 구조를 이 시점에 재검토한다.
>
> **의도적으로 세부 계획을 비워둠**: 이 Phase의 구체적인 리팩토링 항목(어떤 파일을 어떻게 나눌지, 어떤 중복을 제거할지 등)은 지금 미리 정하지 않는다. Page 3의 모든 기능(4.9.1~4.9.9)이 실제로 구현된 이후에야 코드의 최종 형태를 볼 수 있으므로, 이 Phase에 착수하는 세션에서 그 시점의 코드를 직접 읽고 리팩토링 범위와 방법을 그때 계획한다(`bridgeone-refactoring` 스킬 활용).

**목표**: Page 3 구현 완료 시점의 코드를 검토해 중복 제거·함수 분리·상수 정리 등 리팩토링 수행

**세부 목표**: 착수 시점에 코드를 읽고 구체화

**참조 문서**: 없음 (착수 시점에 코드 기반으로 판단)

**검증**: 착수 시점에 리팩토링 항목이 정해진 후 구체화. 공통 기준은 `assembleDebug` 빌드 성공 + 기존 단위테스트 통과 + 기능 회귀 없음

---

## 펌웨어·서버 파트 (후속 통합 Phase, 본 문서 범위 밖)

Phase 4가 "Android 완성" 단계라는 원래 취지에 맞춰, 아래 항목은 ESP32/Windows 서버 작업이 진행되는 후속 통합 Phase로 이동한다. 설계는 이미 완료됨(참조 문서 표시).

### ESP32 (`esp32s3-code-implementation-guide.md` §3.3.2, §4.3)
- `0xFF/0x02`(절대좌표 서버 중계) · `0xFF/0x30`(줌 상태) → Vendor CDC 바이너리/JSON 패스스루
- `EVENT_MONITOR_COUNT` 역방향 통지 중계
- (참고) HID Report 0x02 절대좌표 디스크립터는 Native Macro 재생 전용으로 별도 관리 — 본 Phase와 무관

### Windows 서버 (`technical-specification-server.md` §3.6.9)
- `AbsolutePositionMessage` 파싱 핸들러
- stretch 매핑 함수(`MapRatioToScreenPoint`) + `ResolveTargetMonitorRect`
- 기존 텔레포트 인프라(`ValidateAndClampCursorPosition`, `SetCursorPos`) 재사용
- buttons diff → `SendInput` 드래그 앤 드롭 처리
- 모니터 열거(`Screen.AllScreens`) → 역방향 개수 통지(`NotifyMonitorCount`)
- 줌 오버레이 대상 모니터 반영(`UpdateZoomOverlay` 확장, §3.6.1.4)

### 실기기 통합 검증
- `phase-n-integration-testing.md`에 편입: 서버 SetCursorPos + stretch + 멀티모니터 정확도, 드래그앤드롭 end-to-end, 줌 오버레이 PC 렌더링, 역방향 모니터 개수 통지, 120Hz 지연 튜닝

---

## Phase 4.9 완료 후 Page 3 구조

```
Page 3 — AbsolutePointingPad
├── PointingArea (자유 비율, Fill 기본)
│   ├── 터치 → 비율(0.0~1.0) 변환 → 서버 중계 프레임 전송
│   ├── 줌 시 매핑 범위 축소 (비율 기반)
│   ├── CoordinateIndicator (십자선 + 점)
│   └── 엣지존/엣지스와이프 시스템 (좌표 무관 기능만)
├── ControlButtonContainer (상단 오버레이, 기존 컴포넌트 재사용)
│   ├── ClickModeButton (좌/우 토글)
│   ├── ZoomButton (DPI 슬롯 자리)
│   └── DragModeButton (ScrollSensitivity 슬롯 자리, 드래그 앤 드롭)
├── MonitorSelector (모니터 2개 이상 시)
└── 시각 피드백
    ├── 테두리 색상 (핑크/노란/초록/주황, 다중 모드 동시 활성 시 그라디언트)
    ├── 줌 레벨 텍스트 (앱 내)
    └── 줌 영역 박스 (PC 화면 — Android는 UART 전송까지만, ESP32 중계/Windows 렌더링은 후속 통합 Phase)
```
