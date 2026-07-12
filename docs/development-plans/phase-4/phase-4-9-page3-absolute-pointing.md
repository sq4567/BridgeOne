---
title: "BridgeOne Phase 4.9: Page 3 — 절대좌표 패드 페이지 (서버 중계 재설계)"
description: "BridgeOne 프로젝트 Phase 4.9 - Standard 전용 Page 3: AbsolutePointingPad, 서버 SetCursorPos 중계, 자유 비율, 드래그 앤 드롭, 멀티모니터"
tags: ["android", "absolute-pointing", "server-relay", "zoom", "multi-zone", "vendor-cdc", "multi-monitor", "ui"]
version: "v2.5"
owner: "Chatterbones"
updated: "2026-07-11"
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
- 줌 기능: 임의 종횡비 직사각형 ROI 정의(앱 내) + Vendor CDC 줌 상태 UART 전송(Android 측까지)
- Page 3 엣지존 편집 화면 연동
- 엣지존 줌/드래그 모드 토글 액션
- 멀티 존 모드: 임의 종횡비 직사각형 ROI 존별 매핑(모니터 배정 포함) + 자동/자유 배치 + 프리셋 저장/불러오기 + 활성 구성 존 추가/제거, 터치 즉시 판정
- 손떨림 보정 (원시 좌표 EMA 스무딩)
- 터치 시작 확정 디바운스 (스치는 접촉 필터링)
- 스크롤 모드 (커서 위치 고정 + 상대 델타 스크롤, 일반/무한 둘 다)

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
> LEGACY_POPUP은 배제하고 ZONE 모드만 지원. 로테이션 존은 4.9.8(편집 UI) 전까지 후보 없어 `candidates.firstOrNull()` 정적 처리(회전 코루틴 미이식) — **4.9.8에서 `TouchpadWrapper.kt` 패턴의 회전 코루틴(`rotationJob`/`rotationIndex`)을 이식 완료, `resolveAction(rotationIndex)`로 동적 후보 실행.**
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
- `ui/pages/StandardModePage.kt` (`standardTouchpadPages`에 `2` 추가, `page3Assignment` 별도 상태 제거 후 `standardAssignments[2]`로 통합, `EdgeZoneEditorScreen` 호출부, `zoneEditorDisabledEdges`, `assignmentRepo.migrateJumpToPageIndicesIfNeeded()` 호출)
- `ui/components/touchpad/EdgeZoneEditorScreen.kt` (`excludeDomains` 파라미터 추가, `ActionDomainPicker`/`RotationEditor` 양쪽에 배선)
- `ui/components/touchpad/ZoneRotationEditor.kt` (`RotationEditor`에 `excludeDomains` 파라미터 추가, 내부 `ActionDomainPicker` 호출에 병합 전달)
- `ui/components/touchpad/EdgeZoneActionResolver.kt` (`ActionDomain` enum을 `internal` → public으로 변경 — `EdgeZoneEditorScreen`이 public 함수라 시그니처에 `internal` 타입을 노출할 수 없어 발생한 컴파일 에러 해결)
- `ui/common/TouchpadEdgeZoneAssignmentRepository.kt` (`migrateJumpToPageIndicesIfNeeded()` 신규 — `EdgeZoneJson.kt`를 직접 건드리지 않고 기존 `load()`/`save()`의 디코드·인코드 왕복을 재사용해 저장된 모든 터치패드 ID를 순회하며 마이그레이션)
- `ui/components/AbsolutePointingPad.kt` (세부 목표 6: 로테이션 존 회전 코루틴을 `TouchpadWrapper.kt:901-930` 패턴으로 이식. `rotationIndex: MutableState<Int>`를 상위에서 hoisting해 `PointingArea`와 `EdgeZoneOverlay` 양쪽에 전달, armed 진입/존 전환/disarm/취소 시점에 `rotationJob` 시작·재시작·취소, release 시 `resolveAction(rotationIndex)`로 현재 회전 후보를 실행)

> **⚠️ 구현 중 확인된 사실(2026-07-10)**: 세부 목표 3(CURSOR 예외 처리)은 코드 조사 결과 불필요한 것으로 판명됐다. `ZoneActionPicker.kt`의 `ActionDomainPicker` 내부 `ActionDomain.CLICK` 도메인 옵션 목록(`relativeAction = ToggleMode(CLICK)`, `specificOptions = [SetClickMode(LEFT), SetClickMode(RIGHT)]`)에는 애초에 `ToggleMode(CURSOR)`가 포함되어 있지 않다 — CURSOR 모드는 레거시 팝업 전용 토글(`config.showCursorMode`)로 별도 경로를 타며 편집기에서 선택 가능한 액션으로 노출된 적이 없다. 따라서 `excludeActions` 신규 파라미터나 `ZoneActionPicker.kt` 수정 없이, `ActionDomain.CLICK`을 exclude 목록에 넣지 않는 것만으로 "CLICK 유지 + CURSOR 미노출" 요구사항이 그대로 충족된다.

**검증**: 빌드(`assembleDebug`) 및 관련 유닛 테스트(`EdgeZoneActionResolverTest`, `AbsolutePadActionFilterTest`, `EdgeZoneJsonTest`, `EdgeZoneEditorStateTest`, `EdgeZoneCanvasGeometryTest`)는 통과 확인. 아래 실기기 동작 검증도 유저 확인 완료(2026-07-11):
- [x] Page 6 설정에서 "페이지 3"(절대좌표) 존 편집 진입 가능
- [x] Page 3 편집기에서 MOVE/DPI/DYNAMICS/SCROLL/SCROLL_SPEED 액션 미노출
- [x] Page 3 편집기에서 CURSOR 관련 옵션 미노출, CLICK(좌/우클릭)은 정상 노출
- [x] Page 1/2 편집기는 기존과 동일하게 전체 액션 노출 (회귀 없음)
- [x] 존 구조 편집(분할/병합/이동/삭제/비율조정)은 Page 3에서도 다른 페이지와 동일하게 동작
- [x] 마이그레이션 전 저장된 `JumpToPage(4)`(구 "설정")가 마이그레이션 후 `JumpToPage(5)`로 이동해 여전히 설정 페이지로 점프하는지 확인
- [x] 마이그레이션이 앱 재시작 시 중복 실행되지 않는지 확인
- [x] Page 3에서 로테이션 존(후보 2개 이상)을 생성 후 armed 상태를 유지하면 `intervalMs` 간격으로 하이라이트가 실제로 순환하고, 손을 뗐을 때 순환이 멈춘 시점의 후보가 실행되는지 확인

> **⚠️ 실기기 검증 중 발견된 후속 버그 3건 수정(2026-07-10)**: 최초 구현 검증 중 유저가 세 가지 문제를 발견 — (1) 편집기에서 미할당 존인데도 라벨이 남아 할당된 것처럼 보임, (2) Page 3에 없는 코너 버튼(다이나믹스/모드 프리셋)의 크기 조절 슬라이더가 노출됨, (3) `excludeDomains` 목록이 런타임 화이트리스트(`isAbsolutePadAllowed`)와 어긋나 "멀티 커서"/"프리셋" 액션이 여전히 노출됨.
>
> **원인 및 수정**:
> 1. 편집기 미리보기(`EdgeZoneEditorPreviewCanvas.kt`/`EdgeStripEditor.kt`/`EdgeZoneEditorScreen.kt` 이동 프리뷰)의 `zone.label.ifEmpty{...}` 패턴이 라벨이 이미 비어있지 않으면 액션이 Unassigned인지 확인하지 않던 표시 로직 버그. `EdgeZone.kt`에 `displayLabel` 확장 프로퍼티(action이 Unassigned면 저장된 label과 무관하게 항상 빈 문자열)를 추가해 위 3개 파일 + `ZoneRotationEditor.kt`(로테이션 후보 목록 뷰)까지 통일. 추가로 `StandardModePage.kt`에서 Page 3 편집기 진입 시 `initialConfig`에 `filterConfigForAbsolutePad`를 미리 적용해, `EdgeZoneConfig.default()`에서 물려받은(이제는 선택 불가능한 도메인의) 이동/스크롤/DPI 액션이 처음부터 미할당으로 보이도록 정리.
> 2. `TouchpadButtonVisibility.kt`의 `defaultFor()`에 `standardPage(2)`(Page 3) 케이스가 없어 범용 `default()`(다이나믹스/모드 프리셋 버튼 true)로 폴백되던 게 원인. Page 1과 동일한 구조로 `standardPage(2)` 분기 추가(`showDynamicsButton`/`showModePresetButton`/`showScrollButtons` 모두 false).
>    - **추가 발견(2026-07-10, 실기기 재검증)**: 위 수정만으로는 안 고쳐짐 — 최초 구현 당시 `TouchpadButtonVisibilityRepository.load()`가 이미 잘못된 기본값을 `touchpad_button_visibility.json`에 "standard_page_2" 키로 저장해뒀고, `load()`는 파일에 값이 있으면 `defaultFor()`를 아예 안 타므로 새 분기가 무력화됨. `AbsolutePointingPad`는 애초에 `TouchpadButtonVisibility`를 파라미터로 받지도 않아(코너 버튼이 구조적으로 없음) 이 값이 실제 토글이 아니라 편집기 힌트 전용이므로, `StandardModePage.kt`에서 `selectedZonePage == 2`일 때는 `standardButtonVisibility` 맵(영속 데이터)을 거치지 않고 `TouchpadButtonVisibility.defaultFor(standardPage(2))`를 직접 사용하도록 변경 — 과거 저장분과 무관하게 항상 올바른 값을 반환.
> 3. `AbsolutePadActionFilter.kt`에 `ABSOLUTE_PAD_ALLOWED_DOMAINS` 상수(`isAbsolutePadAllowed`와 1:1 대응, `CLICK/PAGE/COMBO/MACRO/MOUSE_HOLD/HISTORY`만 허용) 신규 추가. `StandardModePage.kt`의 `zoneEditorExcludeDomains`를 하드코딩 목록 대신 `ActionDomain.entries - ABSOLUTE_PAD_ALLOWED_DOMAINS - UNASSIGNED`로 파생하도록 변경 — 향후 `ActionDomain` 추가 시 기본적으로 Page 3에서 제외되는 안전한 방향이라 같은 종류의 누락이 재발하지 않음.
>
> 빌드(`assembleDebug`) + 유닛 테스트 통과 확인. 실기기 재검증 완료(2026-07-11) — 3건 모두 해결 확인:
> - [x] Page 3 편집기에서 클릭 존을 제외한 나머지 존이 처음부터 완전히 빈 상태(미할당)로 표시됨
> - [x] Page 3 편집기에서 코너 버튼 크기 슬라이더가 더 이상 노출되지 않음
> - [x] Page 3 액션 목록에서 멀티 커서/프리셋 항목이 더 이상 노출되지 않음

---

## Phase 4.9.9: 절대좌표 패드 엣지존 줌/드래그 모드 액션

**목표**: 절대좌표 패드 엣지존 액션에 "줌 모드 토글"과 "드래그 앤 드롭 모드 토글"을 추가한다. 두 액션 모두 Page 3 전용이며, 실행 시 각각 ZoomButton 탭 / DragModeButton 탭과 동일한 상태 전환을 일으킨다.

**선행 조건**: Phase 4.9.4(드래그 앤 드롭 모드), Phase 4.9.6(줌 기능), Phase 4.9.8(엣지존 설정 화면 연동) 완료. 멀티 존 모드는 이 Phase 뒤인 4.9.10이므로, 이 Phase의 줌 토글은 단일 줌만 대상으로 한다.

**세부 목표**:
1. **신규 액션 타입 2개** — `EdgeZone.kt`의 `EdgeZoneAction` sealed class에 `object ToggleAbsoluteZoom`, `object ToggleAbsoluteDrag` 추가. exhaustive `when`이라 `categoryColor()`(줌=주황 계열, 드래그=초록 계열), `defaultIconKey()`, `displayName()`("줌 모드 토글"/"드래그 모드 토글") 세 함수에 분기 추가 필수
2. **도메인 신설** — `EdgeZoneActionResolver.kt`의 `ActionDomain` enum에 신규 값(예: `ABSOLUTE_MODE`) 추가, `domainOf()`에 두 액션 매핑, `actionEquals()`에 동일성 비교 분기 추가
3. **화이트리스트 반영** — `AbsolutePadActionFilter.kt`의 `ABSOLUTE_PAD_ALLOWED_DOMAINS`에 `ABSOLUTE_MODE` 추가, `isAbsolutePadAllowed()`에 두 액션 허용 분기 추가. `AbsolutePadActionFilterTest.kt` 갱신
4. **편집기 노출(Page 3 전용, 일반 터치패드 제외)**:
   - `ZoneActionPicker.kt`의 `DEFAULT_DOMAIN_GROUPS` 적절한 그룹에 `ABSOLUTE_MODE` 추가, `domains`(`DomainInfo`) 목록에 신규 항목 추가(액션 2개를 `specificOptions`로 노출, `buildActionTree()` auto-flatten 규칙 확인)
   - `StandardModePage.kt`의 Page 1/2 엣지존 편집기 호출부에 `excludeDomains = setOf(ActionDomain.ABSOLUTE_MODE)` 배선(일반 터치패드는 이 도메인 미노출). Page 3의 `zoneEditorExcludeDomains = ActionDomain.entries - ABSOLUTE_PAD_ALLOWED_DOMAINS - UNASSIGNED` 파생식은 3번 반영 후 자동으로 허용되므로 별도 수정 불필요
5. **실행 배선**:
   - `EdgeZoneActionHandler.kt`의 `applyZoneAction()` — `ToggleAbsoluteDrag → state.copy(dragMode = !state.dragMode)`(순수 상태 변환 가능), `ToggleAbsoluteZoom → state` 그대로 반환(줌은 hoisted `AbsoluteZoomState`/`zoomArming`이라 부수효과형으로 위임)
   - `AbsolutePointingPad.kt`의 런타임 디스패처(`when(actionToApply)`) — `ToggleAbsoluteZoom`은 기존 ZoomButton `onClick` 토글 로직(isActive/arming이면 해제+`sendZoomStateFrame`, 아니면 `zoomArming = true`)을 로컬 헬퍼로 추출해 버튼과 엣지존이 공유. `ToggleAbsoluteDrag`는 `else` 분기(`applyZoneAction` 경유)로 자연 처리
   - `TouchpadWrapper.kt`(일반 터치패드 디스패처)는 이 액션들이 편집기에서 숨겨지므로 별도 분기 불필요(`else` 흡수로 안전)
6. **직렬화** — `ui/common/EdgeZoneJson.kt`의 encode/decode `when`에 `ToggleAbsoluteZoom`/`ToggleAbsoluteDrag` 타입 추가

**수정 파일**:
- `ui/components/touchpad/EdgeZone.kt` (액션 정의 + `categoryColor`/`defaultIconKey`/`displayName`)
- `ui/components/touchpad/EdgeZoneActionResolver.kt` (`ActionDomain` enum + `domainOf` + `actionEquals`)
- `ui/components/touchpad/AbsolutePadActionFilter.kt` (+ `AbsolutePadActionFilterTest.kt`)
- `ui/components/touchpad/ZoneActionPicker.kt` (도메인 그룹 + `DomainInfo`)
- `ui/components/touchpad/EdgeZoneActionHandler.kt` (`applyZoneAction`)
- `ui/components/AbsolutePointingPad.kt` (디스패처 + ZoomButton 토글 로직 헬퍼 추출)
- `ui/common/EdgeZoneJson.kt` (encode/decode)
- `ui/pages/StandardModePage.kt` (Page 1/2 `excludeDomains` 배선)

**참조 문서**:
- 본 문서 Phase 4.9.3(엣지존 통합, `isAbsolutePadAllowed` 원형), 4.9.4(드래그 모드), 4.9.6(줌), 4.9.8(편집기 필터 배선)
- `docs/android/technical-specification-app.md` §2.10 (Page 3 스펙) — 엣지존 액션 화이트리스트에 줌/드래그 토글 추가됨을 보강

**검증**:
- [x] `AbsolutePadActionFilterTest` — `ToggleAbsoluteZoom`/`ToggleAbsoluteDrag` 허용 확인 + Page 1/2 `excludeDomains` 회귀 없음
- [x] 편집기에서 Page 3만 "줌/드래그 토글" 액션 노출, Page 1/2 편집기엔 미노출(`StandardModePage.kt` excludeDomains 배선)
- [x] 엣지 스와이프로 줌 토글 실행 시 ZoomButton 탭과 동일 동작(arming 진입/해제) 확인 — `toggleAbsoluteZoom()` 헬퍼 공유로 코드 경로 동일성 보장(실기기 조작감은 아래 항목에서 별도 확인 필요)
- [x] 엣지 스와이프로 드래그 토글 실행 시 DragModeButton 탭과 동일 동작(테두리 초록 전환) 확인 — `EdgeZoneActionHandler.applyZoneAction`의 `dragMode` 반전이 `ControlButtonContainer`의 DragModeButton과 동일한 `localState.dragMode` 필드를 공유
- [x] 직렬화 왕복(저장→재로드) 후 두 액션 보존 확인 — `EdgeZoneJson.kt` encode/decode 대응 분기 추가
- [x] `assembleDebug` 빌드 성공 + 기존 단위테스트 회귀 없음
- [ ] 실기기 필요: 엣지 스와이프 줌/드래그 토글 체감, 컨트롤 버튼과의 상태 동기화 확인

---

## Phase 4.9.10: 멀티 존 데이터 모델 + 직사각형 ROI 좌표 변환

**목표**: 확대 매핑(단일 줌 + 멀티 존)을 임의 종횡비 **직사각형 ROI**로 통일하는 데이터 모델과 좌표 변환 순수 함수를 마련하고, 이미 완료된 단일 줌(4.9.6, `AbsoluteZoomState`의 중심점+배율 모델)을 새 모델로 마이그레이션한다. 이 Phase는 데이터 표현 교체까지만 다루고, 멀티 존 진입·정의 UI는 4.9.11, 4.9.13~4.9.15에서 순차로 얹는다(4.9.12는 단일 줌 전용 후속 작업).

> **⚠️ 설계 변경 배경**: 기존(v2.2) 계획은 존 매핑을 "중심점+배율"(`AbsoluteZoomState`)로 정의했으나, 이 모델은 모니터 종횡비에 고정되어 세로로 긴 존처럼 임의 종횡비 직사각형을 표현할 수 없다. 유저 요청(2026-07-11)에 따라 존 매핑을 **직사각형 ROI**로 재설계하고, 자유 배치(4.9.13)·프리셋(4.9.14)까지 4개 Phase로 분리했다. 이후 유저 요청(2026-07-11)에 따라 단일 줌도 동일한 직사각형 정의 UX를 쓰도록 4.9.12(단일 줌 직사각형 ROI 정의 UX 통합)를 추가 삽입했다.

**선행 조건**: Phase 4.9.5(모니터 셀렉터), Phase 4.9.6(줌 기능) 완료. `MultiCursorGridGeometry.kt`(Phase 4.8)는 셀 최대 4개까지의 분할 로직만 있어 8분할에는 재사용하지 않는다(아래 참조)

### 상태 구조

단일 줌과 멀티 존은 같은 트리거(ZoomButton 탭=단일 줌, 롱프레스=멀티존)에서 갈라지는 상호 배타적인 "확대 모드"의 두 변형이다. 별개 필드 두 개 + 런타임 강제 해제로 배타성을 관리하는 대신, `AbsoluteCoordinateCalculator.kt`에 이를 감싸는 sealed class를 신설한다:

```kotlin
// 0~1 모니터 비율, 임의 종횡비 허용
data class ZoneRect(val minX: Float, val minY: Float, val maxX: Float, val maxY: Float) {
    companion object { val FULL = ZoneRect(0f, 0f, 1f, 1f) }   // 미정의 = 전체(항등)
}

data class ZoneMapping(
    val pcRect: ZoneRect = ZoneRect.FULL,                // PC 대상 직사각형(임의 종횡비)
    val targetMonitor: Int = DEFAULT_TARGET_MONITOR,     // 존별 모니터 배정
    val padRect: ZoneRect? = null,                       // 자유배치 전용 Android 입력 영역(4.9.13), null=자동 그리드 셀
    val defined: Boolean = false                          // 미정의 존 = 항등 매핑
)

enum class ZonePlacement { AUTO, FREE }                  // 4.9.13에서 사용

data class MultiZoneState(
    val enabled: Boolean = false,
    val zoneCount: Int = MULTI_ZONE_COUNT_DEFAULT,       // 2~8
    val placement: ZonePlacement = ZonePlacement.AUTO,
    val zones: List<ZoneMapping> = List(MULTI_ZONE_COUNT_MAX) { ZoneMapping() }
)

sealed class MagnificationMode {
    data object Off : MagnificationMode()
    data class Single(val mapping: ZoneMapping = ZoneMapping()) : MagnificationMode()  // 직사각형 ROI로 통일
    data class Zone(val state: MultiZoneState = MultiZoneState()) : MagnificationMode()
}
```
- `zones`는 항상 최대(8)개를 보유하고 `zoneCount`로 앞에서 잘라 씀(멀티커서 `page2PadAssignments`와 동일 패턴) — 존 개수를 바꿔도 이미 정의한 매핑이 보존됨
- **단일 줌 마이그레이션**: 기존 `AbsoluteZoomState(level, centerX, centerY)`가 표현하던 정사각(모니터 종횡비 고정) 매핑을 `ZoneMapping.pcRect`(임의 종횡비 직사각형)로 흡수한다. `StandardModePage.kt:376`의 기존 `page3ZoomState: AbsoluteZoomState`(4.9.6) hoisted 상태를 `page3MagnificationMode: MagnificationMode`로 교체. 단일 줌은 `MagnificationMode.Single(mapping)`, 멀티존은 `MagnificationMode.Zone(state)`로 표현되므로 "동시 활성 불가"가 런타임 강제 해제가 아니라 타입 자체로 보장됨(한 값은 한 번에 하나의 case만 가짐)
- `Page3AbsolutePointing`/`AbsolutePointingPad` 시그니처의 `zoomState: AbsoluteZoomState` 파라미터를 `magnificationMode: MagnificationMode`로 교체(`onZoomStateChange` → `onMagnificationModeChange`). 단일 줌 렌더링/좌표 계산 경로는 `(magnificationMode as? MagnificationMode.Single)?.mapping ?: ZoneMapping()`로 파생. 제스처 루프는 `rememberUpdatedState`로 최신값 참조(기존 `currentZoomState` 패턴과 동일 — 빠뜨리면 실행 중 변경이 반영 안 됨)
- 단일 줌의 **정의 UX**(중심점 DOWN → 바깥 드래그로 배율 조절 → 확정 대기 → 원탭 확정, 4.9.6에서 구현된 2단계 흐름)는 이 Phase에서 직사각형 방식으로 갱신하지 않는다 — 배율(스칼라) 대신 직사각형(2축)을 드래그로 정의하는 제스처는 4.9.11의 `rectFromCenterDrag`를 단일 줌에도 동일 적용하는 후속 작업으로 넘기고, 이 Phase는 **데이터 표현 교체 + 좌표 계산 회귀 없음**까지만 다룬다

### 직사각형 ROI 좌표 변환 파이프라인

신규 순수 함수(아래 "신규 파일" 참조, `ui/components/touchpad/MultiZoneCalculator.kt`):
- `divideZoneAreas(width, height, zoneCount)`: 패드 전체 영역을 `zoneCount`(2~8)개 셀로 분할(자동 배치 그리드용, 4.9.11에서 사용). `MultiCursorGridGeometry.divideGridAreas`는 2/3/4 전용 하드코딩(`require`가 `MULTI_CURSOR_COUNT_MAX`=4로 고정)이라 8분할에 재사용 불가하므로 별도 구현: 2~4는 기존과 동일한 레이아웃(1×2/1×3/2×2), 5~8은 2행 그리드로 열을 `ceil(N/2)`/`floor(N/2)`로 나눠 배치(행 우선 번호: 윗줄 좌→우, 아랫줄 좌→우)
- `normalizeInZone(pos, padRect)`: 패드 절대 px 좌표를 해당 셀(또는 자유배치 `padRect`) 기준 0~1로 재정규화. **이 정규화를 빠뜨리면 좌표가 셀 오프셋만큼 어긋난다 — 구현 시 가장 주의할 지점.**
- `applyRoi(localRatio, pcRect)`: 셀 로컬 0~1 좌표를 `pcRect`(임의 종횡비 직사각형) 안으로 재매핑. 기존 단일 줌의 `applyZoom()`(정사각 배율+중심점 기반 축 독립 재매핑)을 일반화한 형태 — `x' = pcRect.minX + local.x * (pcRect.maxX - pcRect.minX)`, `y`도 동일하게 독립 계산(x/y 스케일이 다를 수 있어 임의 종횡비 표현 가능)
- `resolveZoneRatio(pos, padRect, mapping)`: `normalizeInZone` 후 `mapping.defined`면 `applyRoi(local, mapping.pcRect)` 적용, 아니면 항등(안전 동작, 확대 없이 셀 영역 그대로 stretch)
- `rectFromCenterDrag(center, finger)`: 존/영역 정의 제스처용(멀티 존 4.9.11, 단일 줌 4.9.12에서 사용) — `dx = |finger.x - center.x|`, `dy = |finger.y - center.y|`로 `[center∓dx, center∓dy]`(0~1 클램프) 직사각형을 실시간 계산. 손가락이 화면 밖으로 나가면 해당 축이 0 또는 1에서 클램프되어 모니터 끝까지 확장

DOWN/MOVE 처리(런타임 실시간 점프, 멀티 존 정의 UI는 4.9.11, 4.9.13~4.9.15 참조, 단일 줌 정의 UI는 4.9.12 참조):
1. 제스처 시작 시 `divideZoneAreas(areaWidth, areaHeight, zoneCount)`(AUTO) 또는 각 존의 `padRect`(FREE, 4.9.13)로 셀 Rect 목록 계산
2. 매 DOWN/MOVE마다 `MultiCursorGridGeometry.hitTestPad(pos, areas)`(AUTO) 또는 `hitTestByPadRect`(FREE, 4.9.13)로 존 인덱스 판정. 그대로 재사용 — 개수와 무관하게 주어진 Rect 목록에서 순수 히트테스트만 수행하므로 8분할에도 그대로 적용 가능(실시간, 활성 존 개념 없이 즉시 전환)
3. `resolveZoneRatio`로 최종 화면 비율 계산 → `zones[idx].targetMonitor`와 함께 전송

> `hitTestPad`는 `internal`이라 touchpad 패키지 밖에서 직접 재사용 불가 — 신규 파일을 같은 패키지(`ui/components/touchpad/`)에 두어 가시성 확보. `divideZoneAreas`는 이 신규 파일에 직접 정의되므로 가시성 문제 없음

> **프로토콜/서버 참고**: 앱이 `applyRoi`로 최종 절대좌표까지 계산해 기존 절대좌표 프레임(`0xFF/0x02`)으로 전송하므로 서버·펌웨어는 최종 좌표만 수신(변경 없음). 단, 기존 줌 상태 프레임(`0xFF/0x30`)은 `level+center` 스칼라 기반이라 임의 종횡비 ROI를 그대로 담지 못한다 — 이 Phase 시리즈는 앱 내 좌표 매핑 완결에 집중하고, 서버 측 존 영역 시각화용 프레임 규격 확장은 4.9.5/4.9.9와 동일하게 후속 통합 Phase로 미룬다

### 신규 파일
- `ui/components/touchpad/MultiZoneCalculator.kt` — `divideZoneAreas`, `normalizeInZone`, `applyRoi`, `resolveZoneRatio`, `rectFromCenterDrag` (순수 함수, `hitTestPad` 재사용을 위해 touchpad 패키지에 배치). `rectsOverlap`/`hitTestByPadRect`는 4.9.13에서 이 파일에 추가
- `src/android/app/src/test/.../MultiZoneCalculatorTest.kt` — `divideZoneAreas` 2~8분할 경계값(특히 5~8의 2행 그리드 배치), 셀 로컬 정규화 경계값, `applyRoi` 임의 종횡비 합성, 미정의 존 항등 매핑, `rectFromCenterDrag` 클램핑 경계값

### 수정 파일
- `ui/utils/AbsoluteCoordinateCalculator.kt` — `ZoneRect`/`ZoneMapping`/`ZonePlacement`/`MultiZoneState` 데이터 클래스 + `MagnificationMode` sealed class(`Off`/`Single`/`Zone`) 추가. 기존 `AbsoluteZoomState`는 `ZoneMapping.pcRect` 계산의 중간 표현으로 당분간 유지하거나(마이그레이션 헬퍼용) 완전 제거 — 착수 시점에 `applyZoom` 호출부 잔존 여부 확인 후 결정
- `ui/utils/AbsolutePointingConstants.kt` — `MULTI_ZONE_COUNT_MIN=2`/`MAX=8`/`DEFAULT=2`(기본값 주석 필수)
- `ui/components/AbsolutePointingPad.kt` — 단일 줌 좌표 계산 경로를 `applyRoi` 기반으로 교체, `zoomState: AbsoluteZoomState` 파라미터를 `magnificationMode: MagnificationMode`로 교체. 멀티존 DOWN/MOVE 브랜치·정의 UI는 이 Phase에서 배선하지 않음(4.9.11~)
- `ui/pages/StandardModePage.kt` — 기존 `page3ZoomState`(4.9.6)를 `page3MagnificationMode: MagnificationMode`로 교체 + `Page3AbsolutePointing` 파라미터 전달
- `ui/pages/standard/Page3AbsolutePointing.kt` — `magnificationMode`/`onMagnificationModeChange` 파라미터 관통 배선

### 재사용 (무변경)
`MultiCursorGridGeometry.hitTestPad`, `AbsoluteCoordinateCalculator.calculateTouchRatio`, `AbsolutePointingPad.sendAbsolutePosition`/`sendZoomStateFrame`, `ZoomStateCommand.buildFrame`, `FrameBuilder.buildAbsolutePositionCommand`

**참조 문서**:
- `docs/android/component-touchpad.md` §1.2.1 (멀티커서 그리드 분할 — 존 분할 형태의 근거, `hitTestPad` 재사용의 출처)
- 본 문서 Phase 4.9.6(줌 기능) — 마이그레이션 대상인 기존 `AbsoluteZoomState` 모델과 `page3ZoomState`
- 본 문서 Phase 4.9.5(모니터 셀렉터) — 존별 모니터 배정의 `targetMonitor` 규약

> **⚠️ 구현 확정 사항(2026-07-11)**: `AbsoluteZoomState`(줌 정의 제스처용 배율+중심점 스칼라 표현)는 제거하지
> 않고 그대로 유지했다 — `applyZoom`/`dragDistanceToZoomLevel`은 여전히 정의 제스처 계산에 쓰이고
> (4.9.12에서 직사각형 드래그로 교체 예정), `AbsoluteCoordinateCalculator.kt`에 양방향 변환 헬퍼
> `zoneRectFromZoomState(zoom)`/`zoomStateFromZoneMapping(mapping)`/`zoomLevelFromPcRect(pcRect)`를
> 신규 추가해 hoisted 상태(`MagnificationMode`)와 제스처 로컬 상태(`AbsoluteZoomState`) 사이를 매 프레임
> 변환한다. `AbsolutePointingPad.kt`의 `sendZoomed()`는 `applyZoom` 대신 `MultiZoneCalculator.applyRoi`를
> 호출하도록 교체했고, `toggleAbsoluteZoom()` 해제 시에는 `MagnificationMode.Off`를 내보낸다(Single(빈
> ZoneMapping) 대신 Off로 명확히 구분). `page3ZoomState` → `page3MagnificationMode`(`StandardModePage.kt`),
> `Page3AbsolutePointing.kt`의 `zoomState`/`onZoomStateChange` → `magnificationMode`/`onMagnificationModeChange`로
> 전량 배선 교체.

**검증**:
- [x] `MultiZoneCalculatorTest` — `divideZoneAreas` 2~8분할(5~8의 2행 그리드 포함) 경계값, 셀 로컬 정규화, `applyRoi` 임의 종횡비 합성, 미정의 존 항등, `rectFromCenterDrag` 클램핑
- [x] `page3ZoomState` → `page3MagnificationMode` 마이그레이션 후 기존 단일 줌(4.9.6) 좌표 계산 결과 회귀 없음(동일 배율/중심점 입력에 대해 `applyRoi` 결과가 기존 `applyZoom` 결과와 동치, `AbsoluteCoordinateCalculatorTest`에 회귀 테스트 추가)
- [x] `assembleDebug` 빌드 성공 + 기존 단위테스트(줌/모니터셀렉터) 회귀 없음

---

## Phase 4.9.11: 멀티 존 자동 배치 + PC 존 정의 UX

**목표**: 멀티 존 모드 진입 → 존 개수 선택 → 자동 그리드 분할 → 각 서브 패드(셀)의 PC 대상 영역을 직사각형으로 순차 정의 → 탭 확정/롱프레스로 재정의 → 정의 완료 시 실시간 점프 매핑으로 전환한다. 이 Phase는 배치 방식을 `ZonePlacement.AUTO`로 고정한다(자유 배치는 4.9.13).

**선행 조건**: Phase 4.9.10(데이터 모델 + 좌표 변환) 완료

### 진입 및 정의 흐름

1. **진입**: ZoomButton **롱프레스**로 멀티존 모드 진입(탭=단일 줌은 그대로 유지, 멀티커서 CursorModeButton의 탭=선택/롱프레스=전환 이원화 관용구 답습) → `CursorCountSelectionPopup` 재사용(문구만 "존 개수"로, 범위는 `MULTI_ZONE_COUNT_MIN~MAX`인 2~8 — 커서 개수 범위(2~4)보다 넓으므로 팝업에 `countRange: IntRange` 파라미터를 신규 추가해 호출측에서 범위를 지정. 기본값은 기존 `MULTI_CURSOR_COUNT_MIN..MULTI_CURSOR_COUNT_MAX`로 둬 Page 2(멀티커서) 호출부는 그대로 동작)
2. `definingZoneIndex`(0부터) 순서로 각 셀을 정의: **정의 중에는 대상 셀 하나가 풀사이즈 모드처럼 패드 전체를 차지**한다(그리드 분할선·나머지 셀 딤 표시 없음 — 다른 셀이 화면에 함께 보이지 않으므로). "존 k/N 정의 중" 안내 텍스트만 표시하며, **패드 전체 화면을 대상 모니터의 미리보기 캔버스로 간주**하고 그 위에서 PC 매핑 직사각형을 그린다. 그리드 분할(`divideZoneAreas`)은 이 정의 단계가 아니라 6번의 실시간 점프 단계에서 `hitTestPad`로 손가락이 속한 셀을 판정할 때만 쓰인다. 단, **이미 확정된 이전 존들의 PC 매핑 직사각형은 옅은 색으로 겹쳐 표시**해(현재 프리뷰보다 낮은 alpha/얇은 선) 새 존을 그릴 때 겹치지 않게 참고할 수 있게 한다
3. 패드 전체(=대상 셀) 안에서 중심점 DOWN → 손가락을 바깥으로 드래그(`rectFromCenterDrag(center, finger)`로 실시간 직사각형 프리뷰 계산·렌더 — 손가락을 아래로 내리면 세로로 긴 직사각형이 되는 등 종횡비 자유. 손가락이 패드 밖으로 나가면 해당 축이 모니터 끝까지 클램프) → 손 뗌(=**존 정의 완료 시점**) → **겹침 검증**: 방금 그린 직사각형이 이미 확정된 다른 존(`zones.filter { it.defined }`)과 `rectsOverlap`으로 겹치는지 즉시 확인. **겹치면 확정 대기로 넘어가지 않고 그 자리에서 거부**: `ToastController.show(..., ToastType.ERROR)` 에러 토스트 + REJECT 햅틱 후 프리뷰를 지우고 같은 `definingZoneIndex`를 "재정의 중" 상태로 되돌려(5번과 동일한 재정의 흐름) 처음부터 다시 그리게 한다. 겹치지 않으면 **확정 대기** 상태로 전환(4.9.6의 `zoomAwaitingConfirm` 2단계 확정 패턴 재사용: 확정 전 몇 번이든 재드래그로 직사각형 재조정 가능 — 재드래그로 완성한 새 직사각형도 다음 손 뗌 시점에 동일하게 겹침 검증). 좌표 정규화는 정의 제스처 내내 패드 전체 기준(`calculateTouchRatio`)이다. **중심점은 흰 점+오렌지 테두리로 오버레이에 계속 표시**해 기준점을 명확히 보여준다. `rectFromCenterDrag`는 dx/dy에 `MULTI_ZONE_MIN_RECT_SIZE_RATIO`(기본 0.1, 모니터 대비 비율)의 절반을 하한으로 강제해, 손가락을 거의 움직이지 않고 떼도 지나치게 작거나 0폭인 존이 만들어지지 않는다
4. 확정 대기 중 **원탭** → 겹침 검증은 이미 3번(정의 완료 시점)에서 끝났으므로 재검증 없이 바로 확정: `zones[definingZoneIndex] = ZoneMapping(pcRect = 정의된 직사각형, defined = true)`, `definingZoneIndex++`로 다음 셀로 이동
5. 확정 대기 중 **롱프레스** → `definingZoneIndex == 0`(첫 존, 아직 확정된 존 없음)이면 1번(개수 선택 팝업)으로 되돌아감(전체 재시작). `definingZoneIndex > 0`(이미 확정된 이전 존이 있음)이면 전체 재시작 대신 **이번 존만 재정의**: 직사각형 프리뷰만 지우고 같은 `definingZoneIndex`로 다시 3번부터(정의 전 상태로 복귀), 이미 확정된 이전 존들의 `zones`는 보존. 안내 텍스트를 "존 k/N 정의 중"에서 "존 k/N **재정의 중**"으로 전환해 재정의 상태임을 표시(존 커밋 시 원래 문구로 복귀)
6. 마지막 존까지 정의되면 `page3MagnificationMode`를 `MagnificationMode.Zone(state.copy(enabled = true))`로 전환, 이후 실시간 점프 모드로 동작. **enabled=true 전환 후에도 서브 패드 경계(`divideZoneAreas` 결과)를 옅은 그리드 선으로 계속 표시**해, 패드가 하나의 큰 영역이 아니라 균등 분할된 여러 서브 패드로 보이게 한다(정의 단계의 그리드 미표시 원칙과 별개 — 정의 단계는 "대상 셀=패드 전체"라 그리드가 무의미하지만, 실시간 점프 단계는 여러 셀이 동시에 화면에 존재하므로 경계 표시가 필요)
7. 멀티존 활성 중 ZoomButton 재탭 → `page3MagnificationMode = MagnificationMode.Off`로 전환 + 해제 프레임 1회 전송

미정의 존(`defined=false`)은 항등 매핑으로 취급(확대 없이 셀 영역을 그대로 stretch).

### 프로토콜 확장 (임의 종횡비 ZoneRect 인코딩 신설)

기존 `sendZoomStateFrame(zoom: AbsoluteZoomState, ...)`/`ZoomStateCommand.buildFrame(zoom, ...)`은 중심점+배율 기반 **정사각 윈도우 전용**이라 멀티존의 임의 종횡비 `pcRect`를 손실 없이 표현할 수 없다. 이 Phase에서 인코딩 경로를 신설한다:
- `AbsoluteCoordinateCalculator.zoneRectToMappingRange(pcRect: ZoneRect): ZoomMappingRange` — 4개 축을 각각 독립적으로 `(ratio * ABS_COORDINATE_MAX).roundToInt().coerceIn(0, ABS_COORDINATE_MAX)` 인코딩(FULL → 0,0,32767,32767)
- `ZoomStateCommand.buildFrame(pcRect: ZoneRect, targetMonitor: Int)` 오버로드 — 위 함수로 얻은 min/max와 `zoomLevelFromPcRect(pcRect)`(표시용 level)로 기존 `buildPayload(...)` 재사용. 기존 `buildFrame(zoom, ...)`는 단일 줌 회귀 방지를 위해 그대로 유지
- `AbsolutePointingPad.kt`에 파일 프라이빗 `sendZoneStateFrame(pcRect: ZoneRect, targetMonitor: Int)` 신설(`sendZoomStateFrame`과 동형)

### 실시간 점프 전송

- 존 전환 감지(`curZoneIdx != lastSentZoneIdx`) 시 그 존의 `sendZoneStateFrame(zone.mapping.pcRect, zone.mapping.targetMonitor)`를 스로틀 없이 1회 전송, `lastSentZoneIdx` 갱신으로 중복 억제
- `sendAbsolutePosition(ratio, buttons, zone.mapping.targetMonitor)`로 좌표 전송 — 이때 `targetMonitor`는 모니터 셀렉터 값이 아니라 **존별 배정값**을 사용. 단, 이 Phase엔 존별 모니터 셀렉터 UI가 없으므로 각 존 확정 시점의 모니터 셀렉터 값으로 배정(존별 개별 배정 UI는 후속 Phase)
- 존 정의 중 직사각형 드래그에는 기존 `ZOOM_STATE_THROTTLE_MS`(30Hz) 스로틀 그대로 적용, `sendZoneStateFrame`으로 전송

### 존 경계 이동 동작 설정 (계획 외 추가, Phase 4.9.11)

실시간 점프 중 **손을 떼지 않고 다른 서브 패드로 경계를 넘었을 때**의 동작을 환경 설정(Page 5)에서 3지선다 라디오 그룹으로 고를 수 있다. `ZoneCrossBehavior`(`ui/common/ZoneCrossBehaviorPrefs.kt`, SharedPreferences 영속화) enum:
- `OFF`(기본값): 기존 동작 그대로 즉시 점프, 별도 피드백 없음
- `HAPTIC`: 점프는 그대로 허용하되 경계를 넘는 순간 `HapticFeedbackConstants.CLOCK_TICK` 햅틱으로 알림
- `BLOCK`: 점프 자체를 막음 — 좌표가 터치가 시작된 존(`zoneTouchStartIdx`)에 고정되어, 손가락이 다른 존 화면 영역으로 넘어가도 `normalizeInZone`의 0~1 클램프에 의해 커서가 시작 존의 매핑 경계에서 멈춘다(손을 떼고 다시 터치해야 새 존으로 진입)

**감지 지점**: `liveJump` MOVE 브랜치에서 `hitTestPad`로 구한 현재 존(`rawIdx`)이 `lastSentZoneIdx`(이번 연속 터치 중 마지막으로 전송한 존, DOWN 시 -1로 초기화)와 다르면 "경계 이동"으로 판정(`crossedZone`). DOWN에서의 최초 진입은 `lastSentZoneIdx == -1`이라 크로스로 취급하지 않는다.

**상태 전파**: `StandardModePage`가 `loadZoneCrossBehavior`/`saveZoneCrossBehavior`로 hoisting(다른 환경설정 항목과 동일한 `var state by remember + LaunchedEffect(state) { save... }` 패턴) → `Page5Settings`(편집 UI, `SettingsZoneCrossBehaviorSection`)와 `Page3AbsolutePointing → AbsolutePointingPad`(소비, `rememberUpdatedState`로 실행 중인 제스처 루프에도 즉시 반영) 양쪽에 전달.

### 모드 배타성

단일 줌 / 멀티 존 / 드래그 앤 드롭은 상호 배타. 단일 줌과 멀티 존 사이의 배타는 `MagnificationMode` sealed class 자체가 보장(한 값은 Single 또는 Zone 중 하나만 가짐, 강제 해제 코드 불필요). 드래그 앤 드롭은 별개 상태(`localState.dragMode`)라 여전히 진입 지점(ZoomButton 탭·롱프레스, 커서수팝업 확정)에서 명시적으로 차단해야 한다. 멀티존은 "즉시 점프 포인팅"이라 드래그 홀드와 개념이 충돌(존 경계를 넘으면 좌표가 순간이동해 드래그 궤적이 깨짐) — 멀티존 활성 시 드래그 앤 드롭 진입 차단. 엣지존(좌표 무관 이산 액션)은 기존 gate 로직 그대로 공존.

### 수정 파일
- `ui/components/AbsolutePointingPad.kt` — 제스처 루프에 멀티존 DOWN/MOVE/UP 정의·실시간 점프 브랜치, 존 정의 세션 상태(`multiZonePopupVisible`, `definingZoneIndex`, `zoneRectAwaitingConfirm`, `zoneRectPreview`, `zoneCenterPoint`, `zoneRedefining`), 직사각형 프리뷰 오버레이 렌더(그리드 분할선·딤은 정의 단계엔 없음, 대신 이미 확정된 이전 존들을 옅게 겹쳐 표시 + 정의 중심점을 점으로 표시), 실시간 점프 단계(enabled=true) 서브 패드 그리드 경계 오버레이(`divideZoneAreas` 렌더링), ZoomButton 롱프레스 배선, 확정 대기 중 롱프레스 재시작 브랜치(`onRequestRestartDefinition` — 첫 존은 팝업 재시작, 이후 존은 해당 존만 재정의), `sendZoneStateFrame` 헬퍼, 원탭 확정 시 `rectsOverlap` 겹침 검증 + `ToastController` 에러 토스트 브랜치
- `ui/components/touchpad/MultiZoneCalculator.kt` — 두 `ZoneRect`의 AABB 교차 판정 순수 함수 `rectsOverlap(a, b)` 신설(경계선만 맞닿은 경우는 겹침 아님으로 처리). `rectFromCenterDrag`에 `MULTI_ZONE_MIN_RECT_SIZE_RATIO` 기반 dx/dy 하한(`coerceAtLeast`) 추가
- `ui/components/touchpad/CursorCountSelectionPopup.kt` — 개수 선택 루프의 범위를 신규 `countRange: IntRange` 파라미터로 파라미터화(기본값은 기존 `MULTI_CURSOR_COUNT_MIN..MULTI_CURSOR_COUNT_MAX`라 Page 2 동작은 그대로 유지). `skipPreset: Boolean` 파라미터 신규 추가(멀티존은 프리셋 개념이 없어 PRESET 단계 우회). 멀티존 호출측은 `countRange=MULTI_ZONE_COUNT_MIN..MULTI_ZONE_COUNT_MAX`(2~8), `skipPreset=true` 전달
- `ui/components/touchpad/ControlButtonContainer.kt` — ZoomButton 슬롯에 `onZoomLongClick` 파라미터 신규 추가 및 `combinedClickable`의 `onLongClick`에 배선
- `protocol/ZoomStateCommand.kt`, `ui/utils/AbsoluteCoordinateCalculator.kt` — 임의 종횡비 `ZoneRect` → 프레임 인코딩 경로 신설(`zoneRectToMappingRange`, `buildFrame(pcRect, targetMonitor)` 오버로드)
- `ui/utils/AbsolutePointingConstants.kt` — 정의 오버레이/제스처 상수(직사각형 프리뷰 굵기/alpha, 이전 존 겹침 참고 오버레이 굵기/alpha, 안내 텍스트 크기, 확정 대기 중 롱프레스 재시작 임계, 실시간 점프 그리드 경계선 굵기/alpha, 정의 직사각형 최소 크기 비율, 중심점 표시 반지름/테두리 굵기, 모두 기본값 주석 필수)
- `ui/common/ZoneCrossBehaviorPrefs.kt` **(신규 파일, 계획 외 추가)** — `ZoneCrossBehavior` enum(OFF/HAPTIC/BLOCK) + `loadZoneCrossBehavior`/`saveZoneCrossBehavior` (기존 `InputMode.kt`/`AudioFeedbackPrefs.kt`와 동일한 `enum.name` 문자열 저장 패턴)
- `ui/pages/StandardModePage.kt` **(계획 외 추가)** — `zoneCrossBehavior` hoisted 상태(`var + LaunchedEffect` 저장 패턴) 추가, `Page5Settings`/`Page3AbsolutePointing` 양쪽에 전달
- `ui/pages/standard/Page5Settings.kt` **(계획 외 추가)** — `zoneCrossBehavior`/`onZoneCrossBehaviorChange` 파라미터 추가, "멀티 존 경계 이동 동작" 섹션(`SettingsZoneCrossBehaviorSection`, `SettingsInputModeSection`과 동일한 RadioButton+설명 패턴) 신설
- `ui/pages/standard/Page3AbsolutePointing.kt` **(계획 외 추가)** — `zoneCrossBehavior` 파라미터를 `AbsolutePointingPad`로 그대로 전달하는 위임 배선 추가

**참조 문서**:
- 본 문서 Phase 4.9.10(데이터 모델) — `MultiZoneCalculator`의 `divideZoneAreas`/`rectFromCenterDrag`/`resolveZoneRatio`
- 본 문서 Phase 4.9.6(줌 기능) — 2단계 확정(드래그→확정 대기→원탭) UX 패턴의 원형(`zoomDefining`/`zoomAwaitingConfirm`/`zoomAdjusting`)
- 본 문서 Phase 4.9.5(모니터 셀렉터) — 존별 모니터 배정의 `targetMonitor` 규약

**검증**:
- [x] `zoneRectToMappingRange`/`buildFrame(pcRect, ...)` 단위테스트(FULL 전체범위, 임의 종횡비 축 독립 인코딩, 범위 밖 클램핑) — `AbsoluteCoordinateCalculatorTest`, `ZoomStateCommandTest`
- [x] `assembleDebug` 빌드 성공 + 기존 단위테스트 회귀 없음(`MultiZoneCalculatorTest`, `MultiCursorGridGeometryTest` 등 포함 전체 통과)
- [x] `rectsOverlap` 단위테스트(겹침/분리/경계 맞닿음/완전 포함) — `MultiZoneCalculatorTest`
- [x] `rectFromCenterDrag` 최소 크기 하한 단위테스트(중심과 손가락 동일/모서리 중심점/최소 크기보다 큰 드래그는 클램프 미적용) — `MultiZoneCalculatorTest`
- [x] 이미 확정된 존과 겹치게 새 존을 정의하고 **드래그 손을 떼는 즉시**(확정 대기로 넘어가기 전) 에러 토스트 + REJECT 햅틱이 뜨고, 확정 대기 상태 없이 곧바로 같은 존을 "재정의 중"으로 다시 그릴 수 있는지 (실기기)
- [x] 정의 중심점(흰 점)이 DOWN 위치에 표시되고, 확정/재정의/재시작 시 사라지는지 (실기기)
- [x] 손가락을 거의 움직이지 않고 손을 떼도 서브 패드가 최소 크기 이상으로 정의되는지 (실기기)
- [x] 정의 중 두 번째 존부터 이미 확정된 이전 존들의 PC 매핑 영역이 옅게 겹쳐 보이는지, 현재 그리는 프리뷰와 시각적으로 구분되는지 (실기기)
- [x] 실시간 점프 모드(enabled=true) 진입 후 서브 패드 그리드 경계선이 계속 보이는지(하나의 큰 패드처럼 보이지 않는지) (실기기)
- [x] 환경 설정 "멀티 존 경계 이동 동작"에서 OFF/HAPTIC/BLOCK 전환이 즉시 반영되는지, 앱 재시작 후에도 선택값이 유지되는지 (실기기)
- [x] HAPTIC 선택 시: 손을 떼지 않고 다른 서브 패드로 넘어가는 순간 진동이 오고, 같은 존 안에서는 반복 진동하지 않는지 (실기기)
- [x] BLOCK 선택 시: 손을 떼지 않고 다른 서브 패드 쪽으로 계속 밀어도 커서가 점프하지 않고 시작 존의 매핑 경계에서 멈추는지, 손을 뗐다가 다시 터치하면 새 존으로 정상 진입하는지 (실기기)
- [x] 2~8분할 각각에서 존 판정(`hitTestPad`)이 셀 경계를 정확히 구분 (실기기)
- [x] 정의 세션: N개 존을 순차 정의(탭 확정) 후 실시간 점프 모드 정상 전환 (실기기)
- [x] 확정 대기 중 재드래그로 직사각형이 몇 번이든 재조정되는지 확인 (실기기)
- [x] 확정 대기 중 롱프레스: 첫 존(idx==0)에서는 개수 선택 팝업으로 전체 재시작, 두 번째 존부터는 이미 확정된 이전 존을 잃지 않고 해당 존만 "재정의 중"으로 재정의되는지 확인 (실기기)
- [x] 손가락을 패드 밖으로 밀었을 때 해당 축이 모니터 끝까지 클램프되는지(임의 종횡비 직사각형 생성 확인) (실기기)
- [x] 미정의 존 진입 시 항등 매핑(확대 없음) 확인 (실기기)
- [x] 존 전환 시 `ZOOM_STATE`+`targetMonitor` 프레임이 스로틀 없이 1회 전송(중복 없음) (실기기)
- [x] 단일 줌/드래그 앤 드롭과 상호 배타 동작(동시 활성 불가) 확인 (실기기)
- [x] 실기기 필요: 존 경계를 넘나드는 실제 조작감, 직사각형 정의 드래그 체감, 서버 측 존 전환 시 커서 텔레포트 체감 지연(펌웨어/서버 완성 후 후속 통합 Phase에서 검증)

---

## Phase 4.9.12: 단일 줌 직사각형 ROI 정의 UX 통합

**목표**: 단일 줌(4.9.6)의 정의 제스처를 "드래그 거리 → 배율 스칼라"(모니터 종횡비 고정)에서 4.9.11의 `rectFromCenterDrag` 기반 직사각형 정의로 교체해, 단일 줌도 멀티 존과 동일하게 임의 종횡비 PC 영역을 그릴 수 있게 한다. 배율 스칼라가 사라지는 데 따른 UI 피드백(진입/확정 시 화면 전환 애니메이션, 안내 문구, 롱프레스 취소)도 함께 정비한다.

> **⚠️ 설계 배경**: 4.9.10에서 데이터 모델(`MagnificationMode.Single(mapping: ZoneMapping)`)은 이미 직사각형 ROI(`ZoneMapping.pcRect`)를 담을 수 있게 통일됐지만, 그 시점에는 "데이터 표현 교체 + 좌표 계산 회귀 없음"까지만 다루고 정의 제스처 자체는 갱신하지 않은 채 이 Phase로 미뤘다(4.9.10 "상태 구조" 참조). 이 Phase는 그 후속 작업이다.

**선행 조건**: Phase 4.9.11(자동 배치 + PC 존 정의 UX, `rectFromCenterDrag` 신설) 완료

### 제스처 교체

- 기존(4.9.6) 단일 줌 정의 흐름: ZoomButton 탭 → PointingArea 중심점 DOWN → 드래그 거리 비례 배율 증가(`dragDistanceToZoomLevel`, 0dp→1x, 50dp→2x, 100dp→4x, 150dp+→8x 선형 보간) → 손 뗌 → 확정 대기(`zoomAwaitingConfirm`) → 원탭 확정. 이 2단계 확정 골격(드래그 → 손 뗌 → 확정 대기 → 원탭, 확정 전 몇 번이든 재드래그 가능) 자체는 유지
- 신규 흐름: 배율 스칼라 계산 대신 `rectFromCenterDrag(center, finger)`로 직사각형을 실시간 계산·프리뷰. 손가락을 세로/가로로만 밀면 그 축만 늘어나는 임의 종횡비 직사각형(4.9.11 멀티존 정의와 동일한 조작 감각). 손가락이 패드 밖으로 나가면 해당 축이 모니터 끝까지 클램프(4.9.11과 동일 규칙). `rectFromCenterDrag`에 4.9.11에서 추가된 `MULTI_ZONE_MIN_RECT_SIZE_RATIO` 기반 최소 크기 하한도 그대로 상속되므로(별도 구현 불필요) 단일 줌도 손가락을 거의 움직이지 않고 떼면 자동으로 최소 크기가 보장됨. 정의 중 중심점 표시(흰 점+오렌지 테두리, 4.9.11 `zoneCenterPoint` 패턴)도 이 Phase에서 단일 줌 오버레이에 동일하게 적용할지 착수 시점에 결정
- 확정 시 `page3MagnificationMode = MagnificationMode.Single(ZoneMapping(pcRect = 정의된 직사각형, defined = true))`로 반영

### 정리 대상

- `dragDistanceToZoomLevel`/`updateZoomLevelFromDrag`(4.9.6)는 이 Phase 이후 단일 줌 경로에서 더 이상 호출되지 않는다 — 멀티존 자동 배치 정의(4.9.11)도 이미 `rectFromCenterDrag`를 쓰므로, 이 시점부터 두 함수는 코드베이스 전체에서 미사용 상태가 된다. 규모가 작으므로 즉시 삭제 권장(단위테스트 포함)
- 줌 레벨 텍스트 오버레이(PointingArea 우상단, "2x" 등)는 배율이라는 스칼라 값이 사라지므로 표시할 값이 없다 — 제거하거나, `pcRect` 면적 비율로 근사 배율을 역산해 표시할지는 착수 시점에 결정(유저 확인 필요 항목)

### 진입/확정 UI 피드백

배율 스칼라 텍스트가 사라지는 대신, 정의 진입부터 확정까지 화면 전환을 명확히 보여주는 애니메이션과 안내 문구를 둔다.

- ZoomButton 탭으로 arming 진입 시 제어 버튼(`ControlButtonContainer`)·`PointingArea` 테두리·`EdgeZoneOverlay`를 공용 알파(`zoomDefineElementsAlpha`)로 즉시 fade-out(150ms) — 정의 모드 진입을 명확히 알리고 배경 UI가 시선을 뺏지 않게 한다. `PointingArea` 테두리는 `background`/`pointerInput`과 한 modifier 체인이라 Box 전체 알파 대신 `borderColor.copy(alpha = ...)`로 색상 알파만 조절
- 안내 문구: arming 진입 직후(확정 대기 전)엔 "드래그하여 확대할 영역을 지정하세요", 확정 대기 중엔 "탭하여 확정 · 길게 눌러 취소"(멀티 존 "존 N/M 정의 중" · "탭하여 확정 · 길게 눌러 재시작"과 동형이나, 단일 줌은 재시작할 이전 존이 없으므로 취소=전체 해제). 두 문구 모두 정의 중인 rect의 세로 중심이 상/하 어느 절반에 있는지 계산해(`guideTextAlignment`) 반대쪽 절반에 배치 — rect와 겹치지 않게 하고, rect가 아직 없으면 상단에 고정
- 확정 탭 시 마지막 정의 프리뷰 직사각형이 패드 전체 경계로 확대(4변 `Animatable` + 모서리 반경 보간, 250ms, `Page2MultiCursorTouchpad.kt`의 슬라이드 하이라이트 패턴 재사용)되며 패드 테두리 자리를 차지하는 것처럼 보이게 한다. 확대 완료 시점부터 제어 버튼·실제 패드 테두리·엣지존이 200ms fade-in — 확대 오버레이는 `1 - zoomDefineElementsAlpha`로 유도해 별도 상태 없이 자동 크로스페이드되며 사라진다
- 확정 대기 중 재터치가 롱프레스로 판정되면(500ms, `ZOOM_DEFINE_CANCEL_LONGPRESS_MS`) 줌 모드를 완전히 해제(`MagnificationMode.Off`) — 멀티 존의 "롱프레스 재시작"(`zoneRestartJob`)과 동형 타이머(`zoomCancelJob`)이나, 단일 줌은 재시작할 이전 존이 없으므로 취소로 귀결
- 이 UI 피드백 일체는 단일 줌 전용이며 멀티 존에는 적용하지 않는다(`!isZoneMode` 가드) — 멀티 존은 존이 여러 개라 "하나의 존이 커져서 테두리가 된다"는 그림이 맞지 않는다

### 수정 파일
- `ui/components/AbsolutePointingPad.kt` — 단일 줌 제스처 루프(`zoomDefining`/`zoomAwaitingConfirm`/`zoomAdjusting`)의 내부 계산을 `dragDistanceToZoomLevel` 대신 `rectFromCenterDrag` 호출로 교체. 줌 레벨 텍스트 오버레이 제거, 진입/확정 UI 전환 애니메이션(`zoomDefineElementsAlpha`, `zoomExpandLeft/Top/Right/Bottom`, `zoomExpandCornerRadiusPx`, `isZoomExpanding`) 신설, 안내 문구 배치 헬퍼(`guideTextAlignment`) 추가, 확정 대기 롱프레스 취소 타이머(`zoomCancelJob`) 추가
- `ui/utils/AbsoluteCoordinateCalculator.kt` — `dragDistanceToZoomLevel`/`updateZoomLevelFromDrag` 제거(코드베이스 전체 참조 없음 확인 후)
- `ui/utils/AbsolutePointingConstants.kt` — `ZOOM_DEFINE_FADE_OUT_MS`/`ZOOM_DEFINE_EXPAND_MS`/`ZOOM_DEFINE_FADE_IN_MS`/`GUIDE_TEXT_EDGE_PADDING_DP`/`ZOOM_DEFINE_CANCEL_LONGPRESS_MS` 신설
- `src/android/app/src/test/.../AbsoluteCoordinateCalculatorTest.kt` — 제거되는 함수의 기존 단위테스트 정리

**참조 문서**:
- 본 문서 Phase 4.9.6(기존 단일 줌 정의 UX 원형, 2단계 확정 골격)
- 본 문서 Phase 4.9.10(데이터 모델, `ZoneMapping.pcRect`)
- 본 문서 Phase 4.9.11(`rectFromCenterDrag` 신설)

> **착수 시점 결정 완료**: 줌 레벨 텍스트 오버레이는 완전 제거(배율 스칼라 개념 소멸). 정의 드래그 중심점 표시(흰 점+오렌지 테두리)는 단일 줌에도 추가. 텍스트 제거로 정의 대상이 안 보이는 문제는 직사각형 실시간 프리뷰 오버레이를 단일 줌에 신설해 해결(멀티존 §799 오버레이 패턴과 동형). 프레임 전송은 `sendZoomStateFrame`(정사각 윈도우 강제) 대신 `sendZoneStateFrame`(4축 독립 인코딩)으로 통일 — `rectFromCenterDrag`는 비정사각 직사각형을 만들 수 있어 정사각 강제 전송 시 손실이 발생하기 때문. 두 전송 경로는 동일 와이어 프레임 스키마([0xFF][0x30][len][JSON][CRC16])라 서버·펌웨어 변경 불필요.

**검증**:
- [x] 단일 줌 정의 시 손가락을 세로로만 밀면 세로로 긴 직사각형이, 가로로만 밀면 가로로 긴 직사각형이 만들어지는지(임의 종횡비 확인) — `rectFromCenterDrag` 재사용, 4.9.11 `MultiZoneCalculatorTest`에서 이미 검증된 순수함수라 회귀 없음
- [x] 확정 전 재드래그로 몇 번이든 재조정 가능한지(4.9.6 2단계 확정 UX 회귀 없음) — `zoomDefining`/`zoomAdjusting`/`zoomAwaitingConfirm` 상태 전이 골격 유지, 계산만 교체
- [x] 손가락이 패드 밖으로 나갔을 때 해당 축이 모니터 끝까지 클램프되는지 — `rectFromCenterDrag` 내부 `coerceIn(0f, 1f)` 상속
- [x] 줌 레벨 텍스트 오버레이 처리 방식이 착수 시점 결정대로 반영됐는지 — 완전 제거 + 직사각형 프리뷰·중심점 오버레이 신설
- [x] `dragDistanceToZoomLevel`/`updateZoomLevelFromDrag` 제거 후 참조 잔존 없음, 관련 단위테스트 정리 확인 — Grep 확인 완료, `zoneRectFromZoomState`/`zoomStateFromZoneMapping`도 연쇄적으로 dead가 되어 함께 제거
- [x] `assembleDebug` 빌드 성공 + 기존 단위테스트 회귀 없음
- [x] arming 진입 시 제어 버튼/패드 테두리/엣지존이 fade-out되고 안내 문구가 표시되는지, 확정 시 프리뷰 rect가 패드 테두리로 확대되며 fade-in되는지 — 코드 반영 완료
- [x] 안내 문구가 정의 중인 rect와 겹치지 않고 반대쪽 절반에 배치되는지
- [x] 확정 대기 중 롱프레스 시 줌 모드가 완전히 해제되는지, 재드래그/탭 확정 시 타이머가 정상적으로 취소되는지
- [x] 실기기 검증 완료: 단일 줌 직사각형 정의 조작감(의미 반전 — 이제 finger가 center에서 멀어질수록 저배율), 진입/확정 UI 전환 애니메이션·안내 문구 배치·롱프레스 취소의 실제 체감

---

## Phase 4.9.13: 멀티 존 자유 배치 (Android 입력 영역 직사각형)

**목표**: 존 개수 선택 직후 `자동 배치`(4.9.11, 균등 그리드) vs `자유 배치`(이 Phase) 선택 단계를 추가하고, 자유 배치를 고르면 PC 대상 영역뿐 아니라 **Android 쪽 입력 영역**도 임의 직사각형으로 정의할 수 있게 한다(다른 서브 패드와 겹칠 수 없음).

**선행 조건**: Phase 4.9.11(자동 배치 + PC 존 정의 UX) 완료

### 배치 방식 선택

- `CursorCountSelectionPopup`(개수 선택) 확정 직후, `ZonePlacement` 선택 팝업(신규 Composable) 노출: "자동 배치"(기본, 4.9.11 흐름) / "자유 배치"
- 선택값을 `MultiZoneState.placement`에 저장

### 자유 배치 정의 흐름

존마다 두 단계로 정의(순서: PC 영역 먼저, 그다음 Android 영역):
1. **PC 대상 영역 정의**: 4.9.11과 동일한 흐름(패드 전체를 모니터 캔버스로 간주, 중심점 DOWN → 바깥 드래그 → 확정 대기 → 원탭 확정 / 롱프레스 재시작)으로 `pcRect` 정의
2. **Android 입력 영역 정의**: PC 영역 확정 직후, Android 화면 전체가 입력 영역 정의 캔버스로 전환. 중심점 DOWN → 바깥 드래그(`rectFromCenterDrag` 재사용) → 손 뗌(=정의 완료 시점) → **겹침 검사**(`rectsOverlap(신규 padRect, 이미 확정된 존들의 padRect)` — `rectsOverlap`은 4.9.11에서 이미 신설되어 재사용만 하면 됨, 신규 구현 아님) — 겹치면 그 자리에서 확정을 거부하고 재드래그를 유도, 겹치지 않으면 확정 대기 → 원탭 확정 → `zones[definingZoneIndex].padRect` 갱신, 다음 존으로 이동. **피드백 스타일은 4.9.11의 `pcRect` 겹침 검증(`ToastController.show(..., ToastType.ERROR)` + REJECT 햅틱, 정의 완료 시점 즉시 판정)과 통일 권장** — 애초 계획했던 "붉은 점멸 + 롱프레스 햅틱"(엣지존 경계 히트 패턴) 대신, 같은 정의 세션 안에서 PC 영역과 Android 영역 겹침 피드백이 다르게 보이면 혼란을 줄 수 있으므로 착수 시점에 재검토

자동 배치와 달리 자유 배치는 `padRect`가 셀 자동 분할이 아니라 유저가 그린 임의 영역이므로, 정의 완료 후에도 패드에 매핑되지 않은 여백이 남을 수 있다(무매핑 영역, 아래 참조).

### 실시간 점프 히트테스트 분기

- `placement == AUTO`: 4.9.11과 동일하게 `divideZoneAreas` + `hitTestPad`
- `placement == FREE`: `hitTestByPadRect(pos, zones)`(신규)로 터치 위치가 속한 `padRect`를 가진 존을 찾음. 어느 `padRect`에도 속하지 않는 영역은 무매핑으로 취급(좌표 전송 억제 — 존이 정의되지 않은 패드 여백을 실수로 건드려도 커서가 움직이지 않도록)

### 신규 파일 / 수정 파일
- `ui/components/touchpad/MultiZoneCalculator.kt`(수정) — `hitTestByPadRect(pos, mappings)` 추가. `rectsOverlap(a, b)`는 4.9.11에서 이미 신설되어 그대로 재사용(추가 구현 불필요)
- `src/android/app/src/test/.../MultiZoneCalculatorTest.kt`(수정) — `hitTestByPadRect` 판정 + 무매핑 영역 검증(`rectsOverlap` 단위테스트는 4.9.11에서 이미 작성됨)
- `ui/components/touchpad/ZonePlacementSelectionPopup.kt`(신규) — AUTO/FREE 선택 팝업(`CursorCountSelectionPopup` 옆 배치, 유사 스타일)
- `ui/components/AbsolutePointingPad.kt`(수정) — 배치 선택 단계 배선, 자유 배치 시 `padRect` 정의 세션 상태, 겹침 검사·경계 피드백, FREE 히트테스트 분기, 무매핑 영역 처리

**참조 문서**:
- 본 문서 Phase 4.9.11(자동 배치 + PC 존 정의 UX) — 직사각형 정의 제스처(2단계 확정) 재사용 대상
- `docs/development-plans/`의 엣지존 경계 히트 피드백 선례(`feedback_swipe_boundary_feedback` 패턴 — 붉은 점멸 + 롱프레스 햅틱)

**검증**:
- [ ] 개수 선택 후 자동/자유 배치 선택이 정상 노출·분기되는지
- [ ] 자유 배치에서 PC 영역 → Android 영역 순서로 두 단계 정의가 정상 동작하는지
- [ ] 새 `padRect`가 기존 확정 존과 겹칠 때 확정이 차단되고 붉은 피드백이 표시되는지, 겹치지 않는 위치로 재드래그하면 확정되는지
- [ ] 자유 배치 완료 후 어느 `padRect`에도 속하지 않는 패드 영역 터치 시 좌표 전송이 억제되는지(무매핑)
- [ ] 자동 배치(4.9.11) 흐름에 회귀가 없는지
- [ ] `assembleDebug` 빌드 성공 + 기존 단위테스트 회귀 없음
- [ ] 실기기 필요: 자유 배치 영역 정의 조작감, 겹침 판정 체감

---

## Phase 4.9.14: 멀티 존 프리셋

**목표**: 정의를 마친 멀티 존 구성(개수/배치 방식/존별 PC·Android 영역·모니터 배정)을 이름을 붙여 저장하고, 다음 진입 시 프리셋을 불러와 개수 선택부터 정의까지 전 과정을 생략할 수 있게 한다.

**선행 조건**: Phase 4.9.13(자유 배치) 완료

### 진입 흐름 변경

- ZoomButton 롱프레스 진입 지점이 바로 개수 선택 팝업으로 가지 않고, 먼저 **"멀티 존 프리셋" 팝업**을 띄운다: 저장된 프리셋 목록(있으면) + "새로 정의" 옵션
  - 프리셋 선택 → 해당 `MultiZoneState`를 그대로 불러와 `page3MagnificationMode = MagnificationMode.Zone(state.copy(enabled = true))`로 즉시 활성화(개수 선택~정의 전 과정 생략)
  - "새로 정의" 선택 → 기존 흐름(4.9.11 개수 선택 → 4.9.13 배치 방식 선택 → 정의) 그대로 진행

> **▶ 순방향 참조(4.9.15에서 확장)**: 멀티존이 이미 활성화된 상태에서 ZoomButton을 롱프레스하면, 이 팝업에 "현재 구성 편집" 옵션이 하나 더 추가되어 존 추가/제거 재편집 세션으로 진입한다(4.9.15 참조). 이 Phase(4.9.14) 시점에는 아직 이 옵션이 없다.

### 저장 흐름

- 4.9.11/4.9.13의 정의 흐름이 모든 존 확정으로 끝나면(마지막 존 정의 완료 직후, 실시간 점프 모드 진입 전) "이 구성을 프리셋에 저장하시겠습니까?" 확인 → 저장 선택 시 이름 입력 → `MultiZoneState`(개수/배치/`zones` 전체)를 이름과 함께 저장. 저장하지 않으면 이번 세션에서만 인메모리로 사용(기존 4.9.11/4.9.13 동작과 동일)

### 영속화

- `ui/common/MultiZonePresetsRepository.kt`(신규) — `EdgeZonePresetsRepository.kt`의 JSON + SharedPreferences 영속화 패턴, top-level 함수 헬퍼 컨벤션(`loadPresets`/`savePreset`/`deletePreset` 등)을 그대로 답습
- `ui/common/MultiZonePresetConstants.kt`(신규) — `EdgeZonePresetConstants.kt`와 동일하게 SharedPreferences 키/기본값 상수 분리(기본값 주석 필수)
- 직렬화: `ZoneRect`/`ZoneMapping`(`pcRect`/`padRect`/`targetMonitor`/`defined`)/`ZonePlacement`/`zoneCount`가 왕복 보존돼야 함. 기존 `EdgeZoneJson.kt`의 encode/decode `when` 패턴 참고(신규 파일 또는 `MultiZonePresetsRepository.kt` 내부에 직렬화 함수 배치)

### 신규 파일
- `ui/common/MultiZonePresetsRepository.kt`
- `ui/common/MultiZonePresetConstants.kt`
- `ui/components/touchpad/MultiZonePresetPopup.kt`(신규) — 프리셋 목록/새로 정의 선택 UI + 저장 시 이름 입력 UI
- 직렬화 단위테스트(신규, 파일 위치는 착수 시점 확정)

### 수정 파일
- `ui/components/AbsolutePointingPad.kt` — 진입 지점을 프리셋 팝업 경유로 변경, 정의 완료 후 저장 프롬프트 배선

**참조 문서**:
- `ui/common/EdgeZonePresetsRepository.kt`/`EdgeZonePresetConstants.kt` — 영속화 패턴 원형
- 본 문서 Phase 4.9.11/4.9.13 — 저장 대상이 되는 정의 완료 시점과 `MultiZoneState` 구조

**검증**:
- [ ] 정의 완료 후 프리셋 저장 시 이름과 함께 `MultiZoneState` 전체(개수/배치/존별 영역/모니터 배정)가 보존되는지
- [ ] 저장한 프리셋을 다음 진입 시 불러오면 개수 선택~정의 없이 즉시 실시간 점프 모드로 활성화되는지
- [ ] "새로 정의" 선택 시 기존 4.9.11/4.9.13 흐름이 회귀 없이 그대로 동작하는지
- [ ] 직렬화 왕복(저장→재로드) 단위테스트 — `ZoneRect`/`padRect`/`targetMonitor`/`placement` 값 손실 없음
- [ ] `assembleDebug` 빌드 성공 + 기존 단위테스트 회귀 없음
- [ ] 실기기 필요: 프리셋 저장/불러오기 체감, 여러 프리셋 간 전환

---

## Phase 4.9.15: 멀티 존 추가/제거

**목표**: 이미 활성화된 멀티 존 구성(`MagnificationMode.Zone`, `enabled=true`)을 재편집 모드로 열어 존을 추가하거나 제거할 수 있게 한다. 최초 정의(4.9.11/4.9.13)는 이 Phase의 대상이 아니다 — 새 존은 정의 흐름 도중이 아니라 **이미 활성화되어 실시간 점프 중인 멀티존을 다시 열었을 때만** 추가/제거할 수 있다.

**선행 조건**: Phase 4.9.11(자동 배치), 4.9.13(자유 배치), 4.9.14(프리셋) 완료

### 재편집 진입

- 멀티존 활성 중(`MagnificationMode.Zone`) ZoomButton 롱프레스 재진입 시, 4.9.14의 "멀티 존 프리셋" 팝업에 **"현재 구성 편집"** 옵션을 추가(프리셋 목록/새로 정의 옵션과 나란히 노출, 멀티존이 비활성 상태일 때는 이 옵션 자체가 숨김)
- "현재 구성 편집" 선택 → 재편집 세션(`multiZoneEditing = true`) 진입, 그리드/맵 오버레이에 현재 `zones`를 확정 상태(하이라이트 없이)로 렌더링

### 존 추가

- 오버레이에 "+ 존 추가" 버튼(신규) 노출. `zoneCount == MULTI_ZONE_COUNT_MAX`(8)이면 비활성화
- 탭 시 `zoneCount + 1`로 갱신:
  - **AUTO**: `divideZoneAreas`를 새 `zoneCount`로 재계산해 Android 쪽 그리드 셀 배치 전체를 다시 나눈다(예: 4→5분할은 2×2에서 2행 비대칭 그리드로 레이아웃 자체가 바뀜). 이미 `defined=true`였던 존들의 **`pcRect`(PC 매핑)는 그대로 유지** — 재그리드로 바뀌는 것은 "그 존이 Android 화면의 어느 칸을 차지하느냐"뿐이고 "그 칸이 PC 어디를 가리키느냐"는 바뀌지 않는다. 새로 늘어난 마지막 슬롯(`defined=false`)에 대해서만 4.9.11과 동일한 정의 UX(중심점 DOWN → 바깥 드래그 → 확정 대기 → 원탭 확정 / 롱프레스 재시작)로 진입
  - **FREE**: 그리드 개념이 없으므로 재계산 불필요. 새 존만 `padRect=null, defined=false`로 추가되고, 4.9.13와 동일한 흐름(PC 영역 정의 → Android 영역 정의 → 겹침 검사)으로 새 존만 정의
- 새 존 정의(탭 확정) 완료 시 `zones[zoneCount-1]`에 반영, 재편집 세션은 종료되지 않고 유지(추가/제거를 연속으로 반복 가능)

### 존 제거

- 그리드/맵 오버레이에서 제거할 존을 **탭**하면 선택 하이라이트(빨간 테두리 등, 오조작 방지를 위해 탭만으로는 삭제되지 않음), 그 상태에서 **롱프레스**하면 확인 후 제거
- 삭제 로직(순수 함수, `MultiZoneCalculator.kt`): 해당 인덱스를 리스트에서 제거하고 뒤 인덱스를 앞으로 시프트, 맨 뒤에 빈 `ZoneMapping()`을 채워 리스트 길이(8)를 유지, `zoneCount - 1`
  - **AUTO**: 시프트 후 남은 존들은 `divideZoneAreas`로 재계산된 새 그리드 셀 위치를 받되, 각 존의 `pcRect`(정의된 PC 매핑)는 유지 — 추가 때와 동일하게 "칸 위치"만 바뀌고 "매핑 대상"은 유지
  - **FREE**: `padRect`는 좌표 자체이며 인덱스에 종속되지 않으므로 시프트해도 값 그대로 유지, 겹침 재검사 불필요(원래 겹치지 않던 것들이므로)
- `zoneCount`가 `MULTI_ZONE_COUNT_MIN`(2) 미만으로 줄어들지 않도록 방지 — 존이 2개 남은 상태에서는 제거 대상 선택 자체를 막거나(탭 무시) 롱프레스 확인 단계에서 차단

### 재편집 종료

- 재편집 세션에서 별도 종료 조작(예: 오버레이 바깥 롱프레스 또는 "완료" 버튼)으로 나가면 실시간 점프 모드로 복귀
- 종료 시 4.9.14과 동일한 저장 프롬프트("이 구성을 프리셋에 저장하시겠습니까?")를 재노출 — 새 이름으로 저장하거나, 원래 불러온 프리셋이 있었다면 덮어쓸지 선택하는 옵션도 포함(덮어쓰기 UX는 착수 시점에 4.9.14 저장 흐름과 함께 구체화)

### 신규/수정 파일
- `ui/components/touchpad/MultiZoneCalculator.kt`(수정) — `insertZone(zones, zoneCount)`/`removeZoneAt(zones, idx, zoneCount)` 순수 함수(리스트 시프트 + 빈 슬롯 패딩), AUTO 재그리드 후 기존 `pcRect` 보존 로직
- `src/android/app/src/test/.../MultiZoneCalculatorTest.kt`(수정) — 추가/제거 시 리스트 시프트, `pcRect` 보존, `zoneCount` 최소/최대 클램프 단위테스트
- `ui/components/AbsolutePointingPad.kt`(수정) — 재편집 세션 상태(`multiZoneEditing`, 존 선택/롱프레스 삭제 제스처), "+ 존 추가" 버튼 UI, 재편집 종료 시 저장 프롬프트 재노출 배선
- `ui/components/touchpad/MultiZonePresetPopup.kt`(수정, 4.9.14에서 신설) — 멀티존 활성 중일 때 "현재 구성 편집" 옵션 추가

**참조 문서**:
- 본 문서 Phase 4.9.11(자동 배치 + PC 존 정의 UX) — 새 존 정의에 재사용하는 2단계 확정 UX
- 본 문서 Phase 4.9.13(자유 배치) — FREE 배치에서 새 존 정의·겹침 검사 흐름
- 본 문서 Phase 4.9.14(멀티 존 프리셋) — 재진입 팝업과 저장 프롬프트의 확장 대상

**검증**:
- [ ] AUTO에서 존 추가 시 재그리드 후 기존 존들의 `pcRect`가 유지되고 새 슬롯만 정의를 요구하는지
- [ ] AUTO에서 존 제거 시 재그리드 + 리스트 시프트 후 남은 존들의 `pcRect`가 유지되는지
- [ ] FREE에서 존 추가/제거 시 기존 `padRect`와 겹침 규칙이 유지되고 새 존만 정의를 요구하는지
- [ ] `zoneCount`가 2 미만으로 줄어들지 않는지(최소 2개 강제)
- [ ] `zoneCount`가 8을 초과해 늘어나지 않는지(최대 8개 강제, "+ 존 추가" 버튼 비활성화)
- [ ] 오버레이에서 존 탭(선택)만으로는 삭제되지 않고, 탭 후 롱프레스라는 2단계를 거쳐야 삭제되는지
- [ ] 재편집 종료 후 프리셋 저장 프롬프트가 재노출되는지
- [ ] `assembleDebug` 빌드 성공 + 기존 단위테스트 회귀 없음
- [ ] 실기기 필요: 재그리드 후 셀 위치 변경 체감, 탭→롱프레스 제거 제스처 조작감

---

## Phase 4.9.16: 다중 모드 그라디언트 테두리

**목표**: 클릭모드/드래그모드/확대모드(단일 줌 또는 멀티존)가 동시에 활성화됐을 때 PointingArea 테두리를 단색이 아닌 그라디언트로 표시해 상태 조합을 시각적으로 구분

**선행 조건**: Phase 4.9.6(줌), Phase 4.9.10(멀티 존 모드, `MagnificationMode` sealed class 도입) 완료

> **⚠️ Phase 4.9.12 변경사항**: 단일 줌 정의 진입/확정 UI 전환 애니메이션이 추가되며 `PointingArea`의 `.border(...)` 색상이 `borderColor.copy(alpha = borderColor.alpha * borderFadeAlpha)`로 감싸졌다(`borderFadeAlpha`는 상위에서 계산해 파라미터로 전달, arming 중 0으로 fade-out). 그라디언트로 교체할 때 이 `borderFadeAlpha` 곱셈을 함께 이관해야 단일 줌 정의 진입 시 테두리 fade-out이 유지된다 — 그라디언트 각 stop 색상에 동일하게 곱하거나, 그라디언트 Brush 자체를 감싸는 alpha로 처리할지 착수 시점에 결정.

`borderColor`/`bumpColor` when 체인은 현재 4분기다 — `dragMode(초록) > clickMode==RIGHT_CLICK(노랑) > magnificationMode !is Off(주황, TouchpadColorZoom) > else(핑크)`. 색상 우선순위는 설계 §4.5.7을 그대로 채택. 판정 소스는 `localState.dragMode`(Boolean), `clickMode == ClickMode.RIGHT_CLICK`(Boolean), `magnificationMode !is MagnificationMode.Off`(Boolean, 4.9.10에서 확대 모드가 `MagnificationMode` sealed class로 통합됨에 따라 단일 줌·멀티존이 이 하나의 불리언으로 합쳐짐 — 별도 stop 불필요) 세 개의 독립 불리언 조합이므로, 그라디언트 역시 이 4색(핑크/노랑/초록/주황) 안에서 조합된다.

**세부 목표**:
1. 기존 우선순위 단색 로직(`AbsolutePointingPad.kt`의 `borderColor`/`bumpColor` when 체인, 4.9.4/4.9.6에서 누적)을 다중 색상 그라디언트로 교체
2. `TouchpadColors.kt`의 `touchpadBorderColors(state: TouchpadState): Pair<Color, Color>` 패턴 재사용/확장 검토 — 절대좌표 패드는 클릭(2색)×드래그(2색)×확대모드(1색, on/off) 조합이라 기존 2색 페어보다 많은 동시 활성 색이 나올 수 있어 그라디언트 stop 개수/순서 규칙 별도 정의 필요
3. 활성 상태 우선순위 → 그라디언트 색상 순서 매핑 규칙 확정(예: 클릭모드 색이 기본 stop, 드래그 ON/확대모드 ON이 추가되면 보조 stop으로 삽입)
4. `EdgeBumpOverlay`의 `borderColors: Pair<Color, Color>` 파라미터도 동일 그라디언트 규칙과 일관되게 갱신
5. 애니메이션: 모드 전환 시 그라디언트가 즉시 전환될지 짧게 크로스페이드할지 결정(터치패드 기존 전환 애니메이션 유무 확인 후 맞춤)

**참조 문서**:
- `docs/android/component-design-guide-app.md` §4.3 (테두리 색상 규칙, 현재는 단일 색상표만 정의됨 — 그라디언트 조합표 보강 필요)
- `ui/components/touchpad/TouchpadColors.kt`의 `touchpadBorderColors()` (일반 터치패드 다중 모드 그라디언트 선례)

**검증**:
- [ ] 클릭모드만 활성(드래그/확대모드 OFF) — 기존과 동일하게 단색(핑크/노랑)
- [ ] 드래그모드 ON + 클릭모드 조합 — 그라디언트 표시
- [ ] 확대모드(단일 줌) ON + 클릭모드/드래그모드 조합 — 그라디언트 표시
- [ ] 확대모드(멀티존) ON + 클릭모드/드래그모드 조합 — 단일 줌과 동일한 색으로 그라디언트 표시(전용 색 없음)
- [ ] 세 상태 모두 ON — 그라디언트에 3색 모두 반영
- [ ] EdgeBumpOverlay 색상도 동일 그라디언트 규칙으로 갱신되어 시각적 불일치 없음
- [ ] Page 1/2 일반 터치패드 그라디언트 로직에 회귀 없음

---

## Phase 4.9.17: 손떨림 보정

**목표**: 원시 터치 좌표에 지수이동평균(EMA) 스무딩을 적용해 미세한 떨림이 커서 위치에 그대로 반영되지 않도록 한다

**선행 조건**: 없음(Phase 4.9.1의 터치 파이프라인만 있으면 적용 가능) — 문서 순서상 그라디언트 테두리(4.9.16)가 끝난 뒤로 배치

### 스무딩 알고리즘

`AbsoluteCoordinateCalculator.kt`에 추가:
```kotlin
fun smoothRatio(previous: TouchRatio?, current: TouchRatio, alpha: Float): TouchRatio
```
- EMA: `smoothed = previous + alpha * (current - previous)`. `alpha`가 작을수록 더 부드럽지만 지연이 커짐
- `previous == null`(새 터치 세션의 첫 프레임)이면 `current`를 그대로 반환 — 초기 프레임에서 이전 세션 값과 보간되어 튀는 것을 방지

### 적용 위치

- `AbsolutePointingPad.kt`의 PointingArea 제스처 루프에서 DOWN 시 `smoothedRatio: TouchRatio? = null`로 리셋, 매 MOVE마다 `smoothRatio(smoothedRatio, rawRatio, alpha)`로 갱신
- **zone/zoom 매핑보다 먼저** 원시 좌표 단계에 적용한다. 확대모드(`MagnificationMode.Single`/`Zone`)가 켜져 있으면 배율이 커질수록 화면상 떨림도 함께 증폭되므로, 확대모드 활성 중에도 동일하게(오히려 더 중요하게) 적용

### 설정 저장/UI

- `ui/common/AudioFeedbackPrefs.kt` 패턴(SharedPreferences, 클래스 없이 최상위 `loadXxx`/`saveXxx` 함수 쌍)을 그대로 재사용한 신규 `ui/common/TremorSmoothingPrefs.kt`:
  - `loadTremorSmoothingEnabled`/`saveTremorSmoothingEnabled`(기본값 true)
  - `loadTremorSmoothingAlpha`/`saveTremorSmoothingAlpha`(기본값은 `AbsolutePointingConstants.TREMOR_SMOOTHING_ALPHA`)
- `ui/pages/standard/Page5Settings.kt`에 기존 "존 음성 안내" Switch + "말하기 속도" `CustomTrackSlider` 블록과 동일한 배치 패턴으로 "손떨림 보정" Switch + 활성 시 강도 `CustomTrackSlider` 추가. 정확한 섹션 위치(Page 3 전용 섹션 신설 여부)는 착수 시점에 `Page5Settings.kt` 구조를 다시 확인해 결정

### 신규 상수
- `AbsolutePointingConstants.kt` — `TREMOR_SMOOTHING_ALPHA: Float`(기본값 주석 필수, 예: 0.5f)

### 신규 파일
- `ui/common/TremorSmoothingPrefs.kt`

### 수정 파일
- `ui/utils/AbsoluteCoordinateCalculator.kt` — `smoothRatio` 추가
- `ui/utils/AbsolutePointingConstants.kt` — `TREMOR_SMOOTHING_ALPHA` 추가
- `ui/components/AbsolutePointingPad.kt` — 제스처 루프에 스무딩 단계 삽입
- `ui/pages/standard/Page5Settings.kt` — 손떨림 보정 Switch + 강도 Slider UI

**참조 문서**: 없음(기존 `DeltaCalculator.applyDeadZone`은 상대좌표 전용이라 직접 재사용 불가, 개념만 참고)

**검증**:
- [ ] `smoothRatio` 단위테스트 — `alpha=0`(고정, 변화 없음)/`alpha=1`(필터 없음과 동일)/`previous=null`(초기 점프 방지) 경계값
- [ ] 확대모드(단일 줌/멀티존) 활성 중에도 스무딩이 정상 적용되어 회귀 없음
- [ ] 설정 OFF 시 기존(필터 없는) 동작과 동일
- [ ] `assembleDebug` 빌드 성공 + 기존 단위테스트 회귀 없음
- [ ] 실기기 필요: 체감 지연 대 보정 강도 트레이드오프 확인, 기본 alpha 값 튜닝

---

## Phase 4.9.18: 터치 시작 확정 디바운스

**목표**: 패드에 손가락이 닿는 순간의 스치는 접촉(의도치 않은 짧은 터치)이 즉시 커서 이동/클릭으로 이어지지 않도록, DOWN 후 아주 짧은 시간 유지되는지 확인한 뒤에만 포인팅을 확정한다

**선행 조건**: 없음(Phase 4.9.1의 터치 파이프라인만 있으면 적용 가능)

### 동작 방식

- DOWN 발생 시 좌표 전송/`CoordinateIndicator` 표시를 즉시 시작하지 않고 대기
- `AbsolutePointingConstants.TOUCH_START_CONFIRM_MS`(기본값 예: 30L)가 지날 때까지 터치가 유지되면 그 시점부터 기존 파이프라인(좌표 전송, 4.9.17 손떨림 보정 등)을 정상 시작
- 그 전에 UP이 발생하면 스치는 접촉으로 간주해 클릭/드래그/좌표 전송 없이 전부 무시
- 손떨림 보정(4.9.17)이 "이동 중" 떨림을 다루는 것과 달리, 이 기능은 "터치 시작 순간"의 오조작을 다뤄 상호 보완적 — 적용 순서는 디바운스 통과 후에만 스무딩이 시작됨
- 기본값(30ms)은 의도된 가장 빠른 탭(사람의 최소 반응/모터 타이밍상 80ms 이상)보다 충분히 작게 잡아, 진짜 스치는 접촉(통상 20ms 미만)만 걸러내도록 함 — `CLICK_MAX_DURATION_MS`(500ms)와는 별개 단계

### 신규 상수
- `AbsolutePointingConstants.kt` — `TOUCH_START_CONFIRM_MS: Long`(기본값 주석 필수, 예: 30L)

### 수정 파일
- `ui/utils/AbsolutePointingConstants.kt` — `TOUCH_START_CONFIRM_MS` 추가
- `ui/components/AbsolutePointingPad.kt` — DOWN 핸들러에 확정 대기 단계 삽입

**참조 문서**: 없음

**검증**:
- [ ] `TOUCH_START_CONFIRM_MS` 미만 접촉은 좌표 전송/클릭/CoordinateIndicator 표시 없이 무시
- [ ] `TOUCH_START_CONFIRM_MS` 이상 유지된 접촉은 기존과 동일하게 동작(클릭/드래그/좌표 전송 회귀 없음)
- [ ] 손떨림 보정(4.9.17)과 조합 시 정상 동작(디바운스 통과 후 스무딩 시작)
- [ ] `assembleDebug` 빌드 성공 + 기존 단위테스트 회귀 없음
- [ ] 실기기 필요: 의도된 빠른 탭이 걸러지지 않는지, 스치는 접촉이 실제로 걸러지는지 체감 확인

---

## Phase 4.9.19: 스크롤 모드

**목표**: 절대좌표로 커서를 원하는 위치에 둔 채로, 그 지점의 창을 상대(델타) 방식으로 스크롤할 수 있게 한다(일반 스크롤 + 무한 스크롤 둘 다 지원)

**선행 조건**: Phase 4.9.4(드래그 앤 드롭 모드) — 버튼 토글로 "터치의 의미"를 전환하는 동일 패턴을 스크롤에도 적용

### 진입/구조

- `ControlButtonConfig.showScrollMode`(4.9.1에서 Page 3용으로 false 처리했던 슬롯)를 true로 전환, 기존 ScrollModeButton UI(OFF→NORMAL_SCROLL→INFINITE_SCROLL 3단 순환 토글, `ControlButtonContainer.kt:607~676`의 라벨/아이콘/색상/전이 로직)를 그대로 재사용
- `AbsolutePointingPad`/`Page3AbsolutePointing`에 `scrollMode: ScrollMode`/`onScrollModeChange` 파라미터 추가(hoisted, `page3ScrollMode` in `StandardModePage.kt`)
- 드래그 앤 드롭 모드와 상호 배타 — 둘 다 "터치가 곧 좌표 지정"이라는 절대좌표 패드의 기본 전제를 깨는 축이라 동시 존재 불가. ScrollModeButton 진입 시 dragMode 강제 OFF, 반대도 마찬가지
- 테두리 그라디언트(4.9.16)는 드래그모드와 동일한 초록 계열을 재사용(둘 다 "터치가 위치 지정이 아님"을 의미하는 상호 배타 상태라 별도 색 불필요)

> **⚠️ 색상 충돌**: `AbsolutePointingPad.kt`의 `ControlButtonContainer` 호출부는 4.9.4에서 `baseColor = TouchpadColorRed`로 확정됐다(ClickModeButton 기본/ZoomButton OFF/DragModeButton "이동 모드"가 전부 이 빨강을 씀). 그런데 일반 터치패드의 `ScrollMode.INFINITE_SCROLL` 버튼 색도 동일한 `TouchpadColorRed`(`ControlButtonContainer.kt`의 `ColorRed`)라, ScrollModeButton을 그대로 재사용하면 "무한 스크롤 활성" 색이 페이지 기본색과 구분되지 않는다. `baseColor`를 `TouchpadColorRed` → `TouchpadColorPink`로 변경해 해결한다 — 핑크는 이미 PointingArea 테두리의 기본(else) 색이라 "아무 모드도 없는 기본 상태"라는 의미가 버튼과 테두리 양쪽에서 일관되게 맞고, 현재 다른 버튼 색(노랑/초록/주황)과도 겹치지 않는다. `MonitorSelector.kt`의 비선택 칩 색도 `TouchpadColorRed`를 쓰고 있어(4.9.5) 이것도 무한 스크롤 빨강과 같은 화면에 동시에 보일 수 있는지 착수 시점에 재확인

### 터치 의미 전환

- `scrollMode != ScrollMode.OFF`인 동안 PointingArea의 터치/드래그는 `sendAbsolutePosition`을 호출하지 않는다 — 커서는 스크롤 진입 직전 마지막 위치에 그대로 고정(스크롤 휠 이벤트는 OS가 현재 커서 위치의 창으로 라우팅하므로 좌표/줌/모니터 계산이 별도로 필요 없음)
- 대신 raw drag delta를 축 판정(수직/수평) → 누적 → `ClickDetector.createWheelFrame`/`createHorizontalWheelFrame`로 프레임 생성 → 전송하는 파이프라인 적용
- `CoordinateIndicator`는 스크롤 모드 중 숨김(좌표가 갱신되지 않으므로) — 대신 기존 `ScrollGuideline` 컴포넌트를 재사용해 스크롤 방향/양 시각 피드백 표시

### 재사용 vs 추출 필요

- `ClickDetector.createWheelFrame`/`createHorizontalWheelFrame`/`sendFrame`은 이미 순수 유틸리티라 그대로 재사용 가능
- 하지만 축 판정(`ScrollAxis` 결정), 누적/스로틀(`scrollAccum`, `SCROLL_FRAME_MIN_INTERVAL_MS`, `SCROLL_MAX_FRAMES_PER_EVENT`), 방향 배율(`ScrollDirectionBoost`), 무한 스크롤 관성(`velocitySamples` 기반 지수 감쇠 코루틴, `INFINITE_SCROLL_TIME_CONSTANT_MS`)은 전부 `TouchpadWrapper.kt`의 거대한 제스처 상태머신(1700줄 이상) 내부에 인라인으로 구현돼 있어 그대로 가져다 쓸 수 없다(4.9.1에서 엣지존을 못 붙이고 분리한 것과 동일한 이유)
- 이 로직을 `ScrollEngine`(가칭)으로 `TouchpadWrapper.kt`에서 추출해 `ui/components/touchpad/`에 위치시키고, 상대좌표 터치패드와 절대좌표 패드가 공통으로 호출하도록 리팩토링하는 것이 이 Phase의 핵심이자 가장 리스크가 큰 작업
- `ScrollGuideline.kt`는 이미 `scrollMode`/좁은 파라미터만 받는 구조라 그대로 재사용 시도(생성자는 착수 시점에 재확인)

### 신규 파일
- `ui/components/touchpad/ScrollEngine.kt`(가칭) — `TouchpadWrapper.kt`에서 추출한 델타→휠 변환 + 무한 스크롤 관성 로직
- `src/android/app/src/test/.../ScrollEngineTest.kt`

### 수정 파일
- `ui/components/touchpad/TouchpadWrapper.kt` — 인라인 스크롤 로직을 `ScrollEngine` 호출로 교체(동작 회귀 없이)
- `ui/components/AbsolutePointingPad.kt` — `ScrollEngine` 사용해 스크롤 모드 브랜치 추가, 드래그모드와 상호 배타 배선, `ControlButtonContainer` 호출부의 `baseColor`를 `TouchpadColorRed` → `TouchpadColorPink`로 변경(색상 충돌 해결)
- `ui/components/touchpad/ControlButtonContainer.kt` — Page 3용 `showScrollMode = true` 활성화
- `ui/pages/StandardModePage.kt` — `page3ScrollMode` hoisting + 파라미터 전달

**참조 문서**: 본 문서 Phase 4.9.4(드래그 앤 드롭 모드) — 터치 의미 전환 패턴 선례

**검증**:
- [ ] 일반 스크롤: 절대좌표로 위치를 잡은 뒤 스크롤 모드 진입 시 커서 위치가 고정된 채 대상 창이 스크롤됨
- [ ] 무한 스크롤: 관성 감쇠가 상대좌표 터치패드와 동일한 느낌으로 동작
- [ ] 스크롤 모드 중 좌표 전송이 발생하지 않아 커서가 이동하지 않음 확인
- [ ] 드래그 앤 드롭 모드와 상호 배타 확인
- [ ] `baseColor`를 핑크로 바꾼 뒤 ClickModeButton 기본/ZoomButton OFF/DragModeButton "이동 모드" 색이 무한 스크롤 빨강과 명확히 구분됨 확인
- [ ] `ScrollEngine` 추출 후 기존 상대좌표 터치패드(Page 1/2) 스크롤 동작 회귀 없음
- [ ] `assembleDebug` 빌드 성공 + 기존 단위테스트 통과
- [ ] 실기기 필요: 스크롤 체감, 관성 감쇠 튜닝

---

## Phase 4.9.20: 리팩토링

> **의도적으로 세부 계획을 비워둠**: 이 Phase의 구체적인 리팩토링 항목(어떤 파일을 어떻게 나눌지, 어떤 중복을 제거할지 등)은 지금 미리 정하지 않는다. Page 3의 모든 기능(4.9.1~4.9.19)이 실제로 구현된 이후에야 코드의 최종 형태를 볼 수 있으므로, 이 Phase에 착수하는 세션에서 그 시점의 코드를 직접 읽고 리팩토링 범위와 방법을 그때 계획한다(`bridgeone-refactoring` 스킬 활용).
>
> **⚠️ Phase 4.9.9 변경사항**: 엣지존에 줌/드래그 모드 토글 액션(`ToggleAbsoluteZoom`/`ToggleAbsoluteDrag`)이 추가되며 `AbsolutePointingPad.kt`의 ZoomButton 토글 로직이 엣지존 디스패처와 공유하도록 헬퍼로 추출됐다. 이 헬퍼가 이후 Phase(멀티존 등)의 줌 진입 로직과 자연스럽게 합쳐지는지 이 시점에 재확인 대상.
>
> **⚠️ Phase 4.9.10~4.9.15 변경사항**: 확대 매핑이 직사각형 ROI(`ZoneRect`/`ZoneMapping`/`MagnificationMode`, 4.9.10)로 통일되고, 멀티 존 자동 배치 정의 UX(4.9.11)·단일 줌 직사각형 정의 UX 통합(4.9.12)·자유 배치(4.9.13)·프리셋 영속화(4.9.14)·존 추가/제거 재편집(4.9.15)이 순차로 추가됐다. `AbsoluteCoordinateCalculator.kt`(모델), `MultiZoneCalculator.kt`(`divideZoneAreas`/`normalizeInZone`/`applyRoi`/`resolveZoneRatio`/`rectFromCenterDrag`/`rectsOverlap`/`hitTestByPadRect`/`insertZone`/`removeZoneAt`), `AbsolutePointingPad.kt`의 존 정의·재편집 세션 상태(단일 줌 + 자동/자유 배치 + 추가/제거)가 이 여섯 Phase에 걸쳐 누적됐다. 저장 상태(`MagnificationMode`)와 정의 제스처(`rectFromCenterDrag` 기반 2단계 확정)가 4.9.12부터 단일 줌·멀티존 양쪽에서 이미 같은 함수를 쓰지만, 실제 호출부(`AbsolutePointingPad.kt`의 제스처 루프)는 각자 다른 세션 상태 변수(`zoomDefining` 계열 vs `multiZoneDefining` 계열)로 병렬 구현돼 있을 가능성이 높으므로, 공통 상태머신으로 더 묶을 여지가 있는지 검토 대상. 자동 배치(4.9.11)·자유 배치(4.9.13)·추가/제거(4.9.15)의 세션 상태가 한 컴포넌트에 분기로 섞여 있어 분리 여지가 있는지, `CursorCountSelectionPopup.kt`의 `countRange` 파라미터가 멀티커서 전용 PRESET 단계와 섞여 있어 분리 여지가 있는지도 검토 대상. 4.9.12에는 단일 줌 전용 진입/확정 UI 전환 애니메이션(`zoomDefineElementsAlpha`/`zoomExpandLeft`~`Bottom`/`zoomExpandCornerRadiusPx`/`isZoomExpanding`, 안내 문구 배치 헬퍼 `guideTextAlignment`, 확정 대기 롱프레스 취소 타이머 `zoomCancelJob`)도 함께 추가됐다 — 멀티 존의 대응 상태(`zoneRectPreview`/`zoneCenterPoint`/`zoneRestartJob` 등)와 이름·구조가 유사한 짝을 이루므로, 공통 상태머신 검토 시 이 UI 전환 계열도 함께 묶을 여지가 있는지 포함해서 본다.
>
> **⚠️ Phase 4.9.19 변경사항**: `ScrollEngine` 추출로 `TouchpadWrapper.kt`의 스크롤 로직과 `AbsolutePointingPad.kt`의 스크롤 브랜치가 공통 코드를 쓰게 됐다. 추출이 깔끔하게 끝났는지, 두 호출부에 여전히 남은 중복이 있는지 이 시점에 재확인 대상.
>
> **⚠️ Phase 4.9.17, 4.9.18 변경사항**: 손떨림 보정(4.9.17)으로 `AbsoluteCoordinateCalculator.kt`에 `smoothRatio`, 신규 `TremorSmoothingPrefs.kt`가 추가됐다. 원래 별도 하위 Phase(존 경계 진동 피드백)로 계획했던 멀티존 전환 햅틱은 4.9.11 구현 도중 앞당겨져 `ZoneCrossBehavior`(끄기/진동/점프 금지 3지선다, `ui/common/ZoneCrossBehaviorPrefs.kt`)로 흡수됐고, 이미 코드에 반영되어 있어 해당 계획은 문서에서 삭제했다(항상 켜짐이 아니라 설정 가능한 형태로 대체 구현). 터치 시작 확정 디바운스(4.9.18)는 `AbsolutePointingPad.kt`의 같은 DOWN/MOVE 제스처 루프에 손을 대므로, 이 시점에 `ZoneCrossBehavior` 분기와 얽혀 있지 않은지 함께 검토 대상에 포함한다.

**목표**: Page 3 구현 완료 시점의 코드를 검토해 중복 제거·함수 분리·상수 정리 등 리팩토링 수행

**세부 목표**: 착수 시점에 코드를 읽고 구체화

**참조 문서**: 없음 (착수 시점에 코드 기반으로 판단)

**검증**: 착수 시점에 리팩토링 항목이 정해진 후 구체화. 공통 기준은 `assembleDebug` 빌드 성공 + 기존 단위테스트 통과 + 기능 회귀 없음

---

## 펌웨어·서버 파트 (후속 통합 Phase, 본 문서 범위 밖)

Phase 4가 "Android 완성" 단계라는 원래 취지에 맞춰, 아래 항목은 ESP32/Windows 서버 작업이 진행되는 후속 통합 Phase로 이동한다. 설계는 이미 완료됨(참조 문서 표시).

> **⚠️ 임시 게이팅 상수 — 후속 통합 Phase에서 제거 필요**: 실기기 테스트 중 절대좌표 패드 사용 시 PC에서 이상 키 입력이 발생하는 문제가 있고(재현 조건 미파악), Windows 서버가 아직 이 프레임을 처리하지 못하는 상태라 `UsbConstants.kt`의 `INPUT_TRANSMISSION_ENABLED = false`로 모든 PC 전송(`UsbSerialManager.sendFrame`/`sendCommandBytes`/`sendVendorCdcFrame`)을 임시 차단해 두었다. 아래 Windows 서버 파트(`AbsolutePositionMessage` 파싱 핸들러 등) 구현 및 실기기 통합 검증이 끝나면 `INPUT_TRANSMISSION_ENABLED`를 `true`로 되돌리고 이 주석과 상수를 제거할 것.

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
│   ├── 줌 시 직사각형 ROI로 매핑 범위 축소 (임의 종횡비, 비율 기반)
│   ├── 멀티 존 시 자동(그리드)/자유 배치(2~8) + 존별 독립 직사각형 ROI 매핑, 터치 즉시 판정, 활성 구성 재편집으로 존 추가/제거
│   ├── CoordinateIndicator (십자선 + 점)
│   └── 엣지존/엣지스와이프 시스템 (좌표 무관 기능만, 줌/드래그 모드 토글 포함)
├── ControlButtonContainer (상단 오버레이, 기존 컴포넌트 재사용)
│   ├── ClickModeButton (좌/우 토글)
│   ├── ZoomButton (탭=단일 줌, 롱프레스=멀티 존 진입, DPI 슬롯 자리)
│   ├── DragModeButton (드래그 앤 드롭)
│   └── ScrollModeButton (OFF→일반→무한 순환 토글, 커서 위치 고정 + 델타 스크롤)
├── MonitorSelector (모니터 2개 이상 시)
├── 멀티 존 프리셋 (저장/불러오기, SharedPreferences 영속화)
├── 시각 피드백
│   ├── 테두리 색상 (핑크/노란/초록/주황, 다중 모드 동시 활성 시 그라디언트)
│   ├── 줌 레벨 텍스트 (앱 내, 4.9.12에서 배율 스칼라 폐지 후 유지/대체 여부 결정)
│   ├── 멀티 존 정의 오버레이 (그리드 분할선/직사각형 프리뷰 + 정의 중 셀 하이라이트, 앱 내)
│   └── 줌 영역 박스 (PC 화면 — Android는 UART 전송까지만, ESP32 중계/Windows 렌더링은 후속 통합 Phase)
└── 조작 보조 기능
    ├── 손떨림 보정 (원시 좌표 EMA 스무딩)
    └── 터치 시작 확정 디바운스 (스치는 접촉 필터링)
```
