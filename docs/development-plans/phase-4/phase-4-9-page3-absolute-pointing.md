---
title: "BridgeOne Phase 4.9: Page 3 — 절대좌표 패드 페이지"
description: "BridgeOne 프로젝트 Phase 4.9 - Standard 모드 Page 3: AbsolutePointingPad, 줌 기능, Vendor CDC 줌 오버레이"
tags: ["android", "absolute-pointing", "zoom", "vendor-cdc", "overlay", "ui"]
version: "v1.0"
owner: "Chatterbones"
updated: "2026-04-01"
---

# BridgeOne Phase 4.9: Page 3 — 절대좌표 패드 페이지

**개발 기간**: 3-4일 → **3.5-4일** (4.9.3 범위 축소 -0.5일, 4.9.4 신설 +0.5일)

**목표**: 터치한 위치가 곧 PC 커서 위치가 되는 절대좌표 포인팅 전용 Page 3를 구현합니다. 줌 기능으로 미세 조작을 지원하고, 줌 상태를 Android에서 UART로 전송하는 부분까지 구현합니다. PC 화면 줌 영역 박스 오버레이(ESP32 중계 + Windows 렌더링)는 후속 통합 Phase에서 완성됩니다.

**핵심 성과물**:
- AbsolutePointingPad Composable (PointingArea + CoordinateIndicator, 하단 전용 ControlBar 없음)
- 기존 `ControlButtonContainer`(상단 오버레이) + 엣지존/엣지스와이프 시스템(`EdgeZoneOverlay`, `EdgeSwipeOverlay` 등) 재사용 통합
- 절대좌표 변환 알고리즘 (터치 비율 → HID 0~32767)
- 줌 기능 (드래그 기반 줌 진입, 매핑 범위 축소, ControlButtonContainer DPI 슬롯 자리에 ZoomButton 배치)
- FrameBuilder.buildAbsoluteFrame() (0x80 프레임 타입)
- Vendor CDC 줌 상태 전송 (Android → ESP32 UART 커스텀 명령까지만, ESP32 중계/PC 오버레이는 후속 Phase)
- Page 3 엣지존 편집 화면 연동 (기존 `EdgeZoneEditorScreen` 재사용 + 절대좌표 무의미 액션/모드 필터링)

> **⚠️ 설계 변경(사용자 확정)**: 절대좌표 패드도 결국 "커서 이동 방식만 다른 터치패드"이므로, 전용 하단 ControlBar를 새로 만들지 않고 다른 페이지와 동일하게 **상단 `ControlButtonContainer` 오버레이** + **엣지존/엣지스와이프 시스템**을 재사용한다. 단, 모든 기능을 그대로 가져오지 않고 **절대좌표에서 실제로 쓸 수 있는 기능만** 남긴다: 제어 버튼은 ClickMode만(+ZoomButton 신규), 엣지 기능은 매크로/단축키/페이지 전환/클릭/마우스 홀드 등 좌표 무관 트리거만. Move/Scroll/Cursor/DPI/다이나믹스처럼 델타(상대 이동)에 의존하는 기능은 절대좌표에서 성립하지 않아 제외한다.

> **⚠️ 설계 변경(사용자 확정) — Phase 4.9.3 범위 축소**: Phase 4가 "Android 완성" 단계라는 원래 취지에 맞춰, Vendor CDC 줌 상태 관련 작업은 **Android가 UART로 전송하는 부분까지만** 이번 Phase에 포함한다. ESP32 투명 중계, Windows 서버 오버레이 렌더링, 실기기 통합 검증은 후속 통합 Phase로 이동 (상세는 4.9.3 섹션 참조).

**선행 조건**: Phase 4.8 (Page 2 풀 와이드 터치패드) 완료, Phase 4.3 (터치패드 고급 기능) 완료

**에뮬레이터 호환성**: AbsolutePointingPad 전체 UI, 줌 인터랙션, CoordinateIndicator 에뮬레이터에서 개발 가능. 절대좌표 HID 전송 및 줌 오버레이 Vendor CDC 연동은 실기기에서 별도 검증.

---

## 현재 상태 분석

### 기존 구현
- `FrameBuilder.kt`: 8바이트 프레임 생성 `buildFrame()` 구현 완료 (마우스 버튼 프레임은 `ClickDetector.createFrame`/`createMouseButtonFrame` 등이 담당)
- `UsbSerialManager.kt`: UART 프레임 전송 인프라 완료
- `StandardModePage.kt`: 현재 5페이지 HorizontalPager 구조(`PAGE_COUNT = 5`), 논리 인덱스 2=`Page3KeyboardPlaceholder()`, 3=`Page4MinecraftPlaceholder()`, 4=`Page5Settings()`. **절대좌표 페이지 슬롯이 아직 없음.** 본 Phase에서 `PAGE_COUNT`를 6으로 확장하여 논리 인덱스 2에 절대좌표 페이지를 신규 삽입하고, 기존 키보드/마인크래프트/설정 placeholder를 인덱스 3/4/5로 이동한다.
- HID Absolute Mouse Report Descriptor: ESP32-S3 펌웨어에 Report ID 0x02 이미 정의 (`esp32s3-code-implementation-guide.md` §3.3.2, "TinyUSB 디스크립터 설정" 섹션 내)
- Vendor CDC 프레임 전송 인프라: Phase 3에서 구현 완료

**Phase 4.9 완료 후 목표 페이지 구조**:

| 논리 인덱스 | 페이지 | 상태 |
|---|---|---|
| 0 | Page 1 터치패드+액션 | 구현 완료 |
| 1 | Page 2 멀티커서 | 구현 완료 |
| **2** | **Page 3 절대좌표 (본 Phase)** | 미구현 → 구현 |
| 3 | Page 4 키보드 | placeholder (이동) |
| 4 | Page 5 마인크래프트 | placeholder (이동) |
| 5 | Page 6 설정 | 구현 완료 (이동) |

> **⚠️ 인덱스 시프트 부작용 — `JumpToPage` 마이그레이션 필요**: 코드 조사 결과, 인덱스 삽입(2/3/4 → 3/4/5)으로 실제 깨지는 곳은 **`EdgeZoneAction.JumpToPage(pageIndex)` 하나뿐**이다. `EdgeZoneJson.kt`가 이 액션을 페이지 논리 인덱스 정수 그대로 직렬화(`"page": N`)해서 영속화하므로, 이미 "페이지 5(설정, 구 index=4)로 점프"를 저장해둔 사용자가 있다면 시프트 후 `pageIndex=4`가 새 배치의 마인크래프트(구 index=3이 이동한 자리)를 가리키게 되어 **엉뚱한 페이지로 점프**하게 된다(크래시는 아니지만 조용히 잘못된 동작). 이 Phase에서 저장된 `JumpToPage` 값 중 `pageIndex >= 2`인 것을 +1 이동하는 1회성 마이그레이션이 필요 (상세는 Phase 4.9.4 참조). 검증된 안전 항목(별도 조치 불필요): 엣지존 할당 저장 키(`TouchpadIds.standardPage(pageIndex)`, 삽입 지점보다 앞인 0/1만 사용해 영향 없음), `StandardModePrefs.kt`의 DPI/엣지조작방식 설정(페이지 인덱스 비의존 전역 설정), 마우스 홀드 세션(`StandardModePageState`, 원래부터 페이지 전환과 무관한 전역 상태), `PageIndicator`/`EdgeZoneEditorScreen.pageCount`(둘 다 `PAGE_COUNT` 상수를 파라미터로 전달받아 자동 대응).

### 목표 구조 (styleframe-page3.md 기준, ControlBar 통합 방식으로 갱신)
```
Page 3 — AbsolutePointingPad
├── PointingArea (16:9, 전체 화면 매핑)
│   ├── 터치 좌표 → 절대좌표 (0~32767) 변환
│   ├── CoordinateIndicator (십자선 + 점)
│   └── 엣지존/엣지스와이프 시스템 재사용 (EdgeZoneOverlay, EdgeSwipeOverlay 등)
├── ControlButtonContainer (상단 오버레이, 기존 컴포넌트 재사용, ClickMode만 노출)
│   ├── ClickModeButton (좌/우 클릭 전환 — 델타 무관, 유일하게 유효한 기존 버튼)
│   └── ZoomButton (DPI 슬롯 자리 대체 — 절대좌표는 DPI 개념이 없으므로)
└── 줌 시각 피드백
    ├── Android: 줌 레벨 텍스트 (PointingArea 내)
    └── PC: 줌 영역 박스 오버레이 (Windows 서버, Standard 전용)
```

---

## Phase 4.9.1: AbsolutePointingPad 기본 구현

**목표**: 절대좌표 패드 기본 포인팅 + 클릭 기능 구현

> **⚠️ Phase 4.16(가로 지원) 대비**: 실제 가로 레이아웃은 Phase 4.16에서 일괄 추가한다. 16:9 매핑 영역(PointingArea)의 letterbox/pillarbox 계산을 방향 무관하게 짜서 가로에서도 올바른 종횡비가 유지되도록 할 것. `AbsoluteCoordinateCalculator`의 비율→절대좌표 변환은 방향 중립적으로 유지.

**개발 기간**: 1.5일

**세부 목표**:
1. `AbsolutePointingPad` Composable:
   - 단일 컴포넌트가 페이지 전체를 구성
   - PointingArea (16:9 비율 유지, letterbox/pillarbox) + 상단 `ControlButtonContainer` 오버레이(하단 전용 ControlBar 없음)
   - 바깥 여백 16dp
2. `AbsoluteCoordinateCalculator`:
   - 터치 좌표를 PointingArea 내 비율(0.0~1.0)로 변환
   - 비율을 HID 절대좌표(0~32767)로 매핑
   - 영역 밖 터치 시 경계값 클램핑
3. `FrameBuilder.buildAbsoluteFrame()`:
   - 8바이트 프레임, 레이아웃(`technical-specification.md` §2.4.6.1.1 기준): `[0]seq [1]0x80(절대좌표 식별자) [2]buttons(bit0=L,bit1=R,bit2=M) [3]absX_H [4]absX_L [5]absY_H [6]absY_L [7]wheel`
   - absX/absY는 각 16비트를 상위/하위 바이트로 분리해 Big-Endian 순서로 인코딩
   - ⚠️ 기존 `BridgeFrame` data class(Little-Endian, `[seq,buttons,deltaX,deltaY,wheel,modifiers,keyCode1,keyCode2]`)와 바이트 레이아웃이 다르므로 `BridgeFrame`을 재사용할 수 없다. `buildAbsoluteFrame()`은 별도 `ByteArray`(또는 신규 프레임 표현)를 직접 반환하도록 구현할 것
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
8. `ControlButtonContainer` 재사용, "쓸 수 있는 버튼만" 필터링 (신규 하단 ControlBar 없음):
   - `ControlButtonConfig(showClickMode = true, showMoveMode = false, showScrollMode = false, showCursorMode = false, showDpi = false, showScrollSensitivity = false)`로 구성 — ClickMode 외 전부 비활성화
     - **근거**: `MoveMode`(축 잠금 `applyRightAngleLock`)·`DPI`(델타 배율 곱)는 델타 벡터 연산이라 절대좌표(터치=위치)에서 성립하지 않음. `ScrollMode`/`ScrollSensitivity`는 현재 "델타 누적→휠 틱" 방식이라 절대좌표 흐름에 그대로 붙지 않아 배제(사용자 결정, 후속 Phase에서 별도 방식으로 재검토 가능). `CursorMode`(멀티커서)는 Page 2 전용 `MultiCursorState`에 결합돼 있어 배제(사용자 결정)
   - ClickModeButton: 좌↔우 토글 (기존 로직 그대로 재사용, 델타 무관이라 유일하게 그대로 유효)
   - ZoomButton: 신규 버튼. `ControlButtonConfig`에 `showZoom: Boolean` 필드 추가해 DPI/ScrollSensitivity가 비운 슬롯 자리에 배치. 이 Phase에서는 Disabled 상태 (Phase 4.9.2에서 활성화)
9. 엣지존/엣지스와이프 시스템 통합, "좌표 무관 기능만" 필터링:
   - `EdgeZoneOverlay`/`EdgeSwipeOverlay`/`EdgeZoneDetector`/`EdgeZoneActionHandler`를 `TouchpadWrapper.kt`의 통합 방식을 참고해 PointingArea에 동일하게 연결
   - **엣지 스와이프 모드 필터링(런타임 팝업)**: `TouchpadWrapper.kt`의 `visibleModes` 구성 로직(약 495~503줄, `buildList<EdgeSwipeMode>`)을 참고해 절대좌표 컨텍스트에서는 `MOVE`/`DPI`/`DYNAMICS`/`SCROLL`/`SCROLL_SPEED`/`CURSOR` 모드를 제외하고 `CLICK`만 노출. 특히 `DYNAMICS`는 기존 로직에서 config 플래그 없이 무조건 노출되므로(`:502`), 절대좌표 페이지 전용으로 이 지점에 조건 분기 신설 필요
   - ⚠️ 이 항목은 "터치패드 위 엣지 스와이프 팝업" 런타임 동작 필터링이다. Page 3를 **엣지존 설정 화면**(`EdgeZoneEditorScreen`, 존별 액션 할당 편집기) 대상에 포함시키고 그 화면에서도 동일한 기준으로 필터링하는 작업은 별도 단계 **Phase 4.9.4**에서 다룬다(현재 Page 3는 `standardTouchpadPages`에 없어 편집 대상 자체가 아님).
   - **엣지존 액션 필터링**: `EdgeZoneAction`(`EdgeZone.kt:93-288`) 중 좌표 무관 이산 트리거(`SendMacro`, `SendShortcut`, `CyclePage`, `JumpToPage`, `SetClickMode`, `MouseHoldToggle`, `RestorePreviousMode` 등)만 허용하고, 델타 상태를 바꾸는 액션(`SetDpi`, `SetCustomDpi`, `SetMoveMode`, `SetDynamicsPreset`, `CyclePreset(DYNAMICS)`, `OpenSettings(DPI)`, 스크롤·커서 계열)은 절대좌표 페이지의 엣지존 프리셋/에디터에서 배제하거나 선택 시 무시되도록 처리

**신규 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/AbsolutePointingPad.kt`
- `src/android/app/src/main/java/com/bridgeone/app/ui/utils/AbsoluteCoordinateCalculator.kt`

**수정 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/protocol/FrameBuilder.kt` (buildAbsoluteFrame 추가)
- `src/android/app/src/main/java/com/bridgeone/app/ui/pages/StandardModePage.kt` — `PAGE_COUNT` 5→6 확장, `when(page % PAGE_COUNT)` 분기(line 538 부근)에 논리 인덱스 2 케이스로 `AbsolutePointingPad` 신규 삽입, 기존 인덱스 2/3/4(키보드ph/마인ph/설정)를 3/4/5로 시프트
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/touchpad/ControlButtonContainer.kt` — `ControlButtonConfig`에 `showZoom: Boolean = false` 필드 추가, DPI/ScrollSensitivity 슬롯 위치에 ZoomButton 렌더링 분기 추가
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/TouchpadWrapper.kt` — `visibleModes` 필터링 로직(약 495~503줄) 참고해 절대좌표 페이지 전용 모드 필터(CLICK만 허용) 신설 지점 확인
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/touchpad/EdgeZoneOverlay.kt` 등 엣지존 관련 파일 — 절대좌표 페이지에서도 동작하도록 연동 지점 확인, 델타 상태 변경 액션 배제 처리

**참조 문서**:
- `docs/android/component-design-guide-app.md` §4 (AbsolutePointingPad 컴포넌트 설계)
- `docs/android/styleframe-page3.md` §2 (레이아웃 구조)
- `docs/android/technical-specification-app.md` §2.10 (구현 요구사항)
- `docs/technical-specification.md` §2.4.6.1.1 (HID Absolute Mouse Interface)
- `docs/android/component-touchpad.md` §1.3 (ControlButtonContainer 설계, 재사용 대상)

> **⚠️ Phase 4.1.7 변경사항**: Page 3 레이아웃은 `AppState.Active` 박스 내 `padding(top=40dp, bottom=40dp)` 적용 영역 안에서 렌더링됨. PointingArea 16:9 비율 계산 시 유효 화면 높이 = 전체 높이 − 80dp 기준 사용.

> **⚠️ Phase 4.1.8 변경사항**: 커스텀 토스트 시스템 도입. `android.widget.Toast` 사용 금지. 모든 알림은 `ToastController.show(message, ToastType, durationMs)` 로 표시.

> **⚠️ Phase 4.7.4-A / 4.7.2 변경사항**: 절대좌표 페이지는 `StandardModePage.kt` 인라인 함수가 아니라 독립 파일로 추가한다(`ui/pages/standard/`; 기존 키보드/마인크래프트/설정 placeholder도 이미 이 디렉토리의 독립 파일임 — `Page3KeyboardPlaceholder.kt`, `Page4MinecraftPlaceholder.kt`, `Page5Settings.kt`, 본 Phase에서 인덱스 이동에 따라 이 파일들 자체의 이름 변경은 선택사항). 신규 `AbsolutePointingPad.kt`는 `ui/components/`에 둘 것. `AbsoluteCoordinateCalculator`는 순수 함수이므로 추출과 **동시에 단위 테스트** 작성(`EdgeGeometryTest` 선례). 줌 레벨/전송 스로틀(30Hz) 등 신규 상수는 인라인 금지, 별도 `*Constants.kt`에 기본값 주석과 함께 중앙화(4.7.1).
>
> **⚠️ 설계 변경(사용자 확정) — 하단 ControlBar 폐기, 기존 컴포넌트 재사용 + "쓸 수 있는 것만" 필터링**: 당초 계획된 전용 하단 ControlBar는 만들지 않는다. `ClickModeButton`은 기존 `ui/components/touchpad/ControlButtonContainer.kt`(Phase 4.3.1, `ClickMode.LEFT↔RIGHT` 토글)를 **그대로 재사용**한다(상단 오버레이 배치 그대로 유지). ZoomButton은 이 컨테이너에 신규 슬롯으로 추가한다. 반면 `ScrollModeButton`/`MoveModeButton`/`DpiControlButton`/`ScrollSensitivityButton`/`CursorModeButton`은 모두 `showXxx = false`로 숨긴다 — 코드 조사 결과(`ControlButtonContainer.kt:190-366`, `TouchpadWrapper.kt:1167,1194,995-1092`) MoveMode/DPI는 델타 벡터 연산, ScrollMode/ScrollSensitivity는 델타 누적 기반이라 절대좌표에서 성립하지 않고, CursorMode는 Page 2 전용 상태에 결합돼 있기 때문(사용자 결정으로 Scroll/Cursor는 이번 Phase에서 완전 배제). 클릭 판정도 기존 `ClickDetector.detectClick(pressDuration, movement)` 재사용하되, 기존 임계값(이동량 <15dp)과 본 Phase 세부목표(이동량 ≤5dp)가 다르므로 절대좌표 전용 임계값을 별도 상수로 정의할 것.
>
> **⚠️ 설계 변경(사용자 확정) — 엣지존/엣지스와이프 통합, "좌표 무관 기능만" 필터링**: 매크로/단축키/페이지 전환/클릭/마우스 홀드 등 좌표와 무관한 엣지존 기능(`EdgeZoneOverlay`/`EdgeSwipeOverlay`/`EdgeZoneDetector`/`EdgeZoneActionHandler`, `ui/common/EdgeSwipeConstants.kt`)은 다른 터치패드 페이지와 동일하게 절대좌표 패드에서도 사용 가능해야 한다. 통합 방식은 `TouchpadWrapper.kt`가 상대좌표 터치패드에 이 요소들을 엮는 구조를 참고한다. 단, `TouchpadWrapper.kt` 자체는 `TouchpadState`(델타 기반 이동) 모델에 강하게 결합돼 있어 그대로 재사용할 수 없고, `AbsolutePointingPad`에서 동일한 엣지존 컴포넌트들을 절대좌표 터치 흐름에 맞게 새로 엮어야 한다. 이때 엣지 스와이프 팝업의 모드 목록(`EdgeSwipeMode`: SCROLL/CLICK/MOVE/CURSOR/DPI/SCROLL_SPEED/DYNAMICS)과 엣지존 액션(`EdgeZoneAction`)도 델타 상태를 바꾸는 것들(MOVE/DPI/DYNAMICS/SCROLL/SCROLL_SPEED/CURSOR 계열, `SetDpi`/`SetMoveMode`/`SetDynamicsPreset` 등)은 배제하고 `CLICK` 및 좌표 무관 액션만 노출한다.
>
> **⚠️ 제스처 충돌 규칙(줌 드래그 vs 엣지 스와이프)**: 줌 진입 드래그(4.9.2, 중심점 터치 후 바깥으로 드래그)가 화면 가장자리 `EdgeSwipeConstants.EDGE_HIT_WIDTH_DP` 폭 이내에 진입하면 엣지 스와이프 인식과 충돌할 수 있다. 우선순위 규칙: 줌 모드가 이미 활성 상태로 드래그 중일 때는 엣지존 트리거를 일시 무시하고, 반대로 엣지존이 먼저 트리거된 경우(엣지 히트 영역에서 터치 시작) 줌 드래그 인식을 시작하지 않는다.

**검증**:
- [ ] PointingArea 16:9 비율 유지 (letterbox/pillarbox 정상)
- [ ] 터치 위치 → 절대좌표 변환 정확성
- [ ] CoordinateIndicator 표시/페이드 아웃
- [ ] 클릭 감지 (짧은 탭 → 클릭 이벤트)
- [ ] ClickMode 좌↔우 전환 (ControlButtonContainer 재사용)
- [ ] Move/Scroll/Cursor/DPI/ScrollSensitivity 버튼 미노출 확인 (ControlButtonConfig 필터링)
- [ ] 동일 좌표 전송 스킵 동작
- [ ] 테두리 색상 상태별 전환 (핑크/노란/초록)
- [ ] 엣지존/엣지스와이프 좌표 무관 기능만 동작 (매크로/단축키/페이지 전환/클릭/마우스 홀드), MOVE/DPI/DYNAMICS/SCROLL/SCROLL_SPEED/CURSOR 모드·액션 미노출 확인

---

## Phase 4.9.2: 줌 기능 구현

**목표**: 드래그 기반 줌 진입 + 줌 상태 좌표 변환 + 줌 해제

**개발 기간**: 1일

**세부 목표**:
1. **ZoomButton 활성화**:
   - `ControlButtonContainer`의 DPI 슬롯 자리에 배치된 ZoomButton 활성화 (`showZoom = true`)
   - 탭: 줌 모드 진입 (줌 활성 시 재탭 → 1x 해제)
   - Selected 상태: 배율 배지 표시 (예: "2x")
2. **줌 진입 인터랙션**:
   - ZoomButton 탭 → 줌 모드 진입
   - PointingArea 위에서 중심점 터치
   - 터치 유지한 채 바깥으로 드래그 → 드래그 거리 비례 줌 레벨 증가
   - 손 떼기 → 줌 레벨 확정, 일반 포인팅 복귀
   - ⚠️ 제스처 충돌 규칙(4.9.1 노트 참조): 드래그가 `EdgeSwipeConstants.EDGE_HIT_WIDTH_DP` 이내의 엣지 히트 영역에 들어가도 엣지존 트리거로 전환되지 않도록 줌 드래그 중에는 엣지존 인식을 억제. 반대로 엣지 히트 영역에서 터치가 시작된 경우는 줌 드래그로 인식하지 않고 엣지존이 우선
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
- `docs/android/component-design-guide-app.md` §4.5 (줌 기능, Region Zoom)

**검증**:
- [ ] ZoomButton 탭 → 줌 모드 진입
- [ ] 드래그 거리에 비례한 줌 레벨 증가
- [ ] 줌 상태에서 포인팅 정밀도 향상 확인
- [ ] 줌 해제 (1x 복귀)
- [ ] 테두리 주황색 전환
- [ ] 줌 레벨 텍스트 표시

---

## Phase 4.9.3: Vendor CDC 줌 상태 전송 (Android 측)

> **⚠️ 설계 변경(사용자 확정) — 범위 축소**: 당초 이 Phase는 "ESP32 투명 중계 + Windows PC 오버레이 연동"까지 포함했으나, Phase 4가 "Android 완성" 단계라는 원래 취지와 맞지 않아 **Android가 UART로 줌 상태를 전송하는 부분까지만** 이 Phase에서 다룬다. ESP32 투명 중계 구현, Windows 서버 오버레이 렌더링, 그리고 실기기 기반 통합 검증(PC 화면에 박스가 실제로 뜨는지 확인)은 ESP32/Windows 작업이 진행되는 **후속 통합 Phase**로 넘긴다 (`docs/development-plans/phase-n-integration-testing.md`가 이미 "Phase 4.9 - PC 화면에서 실제 커서 이동 정확성 통합 테스트"를 통합 검증 대상으로 예시하고 있어, 이 통합 검증 Phase에 줌 오버레이 검증도 함께 포함시키는 것이 자연스럽다).

**목표**: Android에서 줌 상태(zoom_level, 매핑 범위)를 UART 커스텀 명령으로 ESP32에 전송하는 부분까지 구현. ESP32 중계 및 PC 오버레이는 범위 밖.

**개발 기간**: 0.5일

**세부 목표**:
1. **Android → ESP32 줌 상태 전송**:
   - 0xFF 커스텀 명령으로 UART 전송
   - JSON payload: `zoom_level`, `min_x`, `min_y`, `max_x`, `max_y`
   - 전송 시점: 줌 확정 시 1회, 드래그 중 30Hz 스로틀, 해제 시 1회
2. **Essential 모드 처리**:
   - 서버 미연결 → 줌 상태 전송 스킵, 앱 내 텍스트만 표시 (Android 단독으로 확인 가능)

> **참고(범위 밖, 후속 Phase)**: ESP32의 `VCDC_CMD_ZOOM_STATE (0x30)` 투명 중계와 Windows 서버 측 오버레이 렌더링(`UpdateZoomOverlay`)은 별도 후속 Phase에서 구현. 명령 코드/JSON 스펙은 `technical-specification.md` §2.4.6.1.2에 이미 정의돼 있으나, `esp32s3-code-implementation-guide.md`에는 아직 반영되지 않았음 — 해당 후속 Phase 착수 전 펌웨어 가이드 보강 필요.

**수정 파일**:
- `AbsolutePointingPad.kt` (줌 상태 변경 시 전송 트리거)
- `src/android/app/src/main/java/com/bridgeone/app/protocol/FrameBuilder.kt` (줌 상태 커스텀 명령 생성)

**참조 문서**:
- `docs/technical-specification.md` §2.4.6.1.2 (줌 상태 Vendor CDC 메시지, JSON payload 스펙)

**검증** (Android 단독으로 완결 가능):
- [ ] 줌 확정 시 UART로 줌 상태 전송
- [ ] 줌 해제 시 zoom_level=1.0 전송
- [ ] Essential 모드에서 전송 스킵 (크래시 없음)

**후속 통합 Phase에서 검증할 항목** (Phase 4 범위 밖):
- [ ] ESP32가 UART 수신 → Vendor CDC Frame으로 투명 중계
- [ ] Windows 서버 연동 시 PC 화면에 줌 영역 박스 표시 (실기기 검증)

---

## Phase 4.9.4: 엣지존 설정 화면 연동 (필터링 적용)

**목표**: 절대좌표 패드(Page 3)를 엣지존 편집 대상에 추가하고, 편집기에서 절대좌표에 무의미한 모드/액션이 노출되지 않도록 필터링한다.

**개발 기간**: 0.5일

> **⚠️ 코드 조사 결과 요약**: 현재 `StandardModePage.kt:357`의 `standardTouchpadPages = listOf(0, 1)`에는 Page 3(인덱스 2)가 빠져 있어 엣지존 할당(`standardAssignments`) 대상 자체가 아니다. 또한 액션/모드 필터 파라미터(`ZoneActionPicker.kt:174`의 `excludeDomains: Set<ActionDomain>`)는 이미 존재하지만 `EdgeZoneEditorScreen.kt`(159-174행 시그니처, 1961행 호출부)에 배선돼 있지 않아 현재는 12개 `ActionDomain` 전부가 항상 노출된다. 이 Phase는 신규 UI 컴포넌트를 만드는 게 아니라, **기존 필터 파라미터를 관통 배선**하는 작업이다.
>
> **캔버스 재사용 확인(수정 불필요)**: `EdgeZoneEditorPreviewCanvas.kt`/`EdgeZoneCanvasGeometry.kt`/`EdgeZoneCanvasGestures.kt`/`EdgeZoneCanvasModeButtons.kt`/`EdgeZoneCanvasModeBars.kt`/`EdgeZoneCanvasRatioPanel.kt`/`EdgeZoneCanvasModeOverlay.kt` 7개 파일은 존 분할/병합/이동/삭제/비율조정 등 "편집기 UI 조작"(`CanvasModeKind`)만 다루고 `EdgeSwipeMode`/`EdgeZoneAction`을 전혀 참조하지 않는다. Page 3 전용 수정 없이 그대로 재사용 가능.

**세부 목표**:
1. **Page 3를 엣지존 편집 대상에 포함**:
   - `StandardModePage.kt:357` `standardTouchpadPages`에 `2` 추가
   - `standardAssignments` 초기화/로드 로직을 Page 3까지 확장 (이미 `Page5Settings.kt:83`의 `sortedPages = standardAssignments.keys.sorted()`는 제너릭하게 동작하므로 별도 수정 불필요, 자동 포함)
   - Phase 4.9.1에서 `AbsolutePointingPad`가 `edgeZoneAssignment`/`onEdgeZoneAssignmentChange` 파라미터를 받도록 배선하는 부분(4.9.1 세부목표 9)과 이어지는 작업
2. **`excludeDomains` 필터를 `EdgeZoneEditorScreen`까지 관통 배선**:
   - `EdgeZoneEditorScreen.kt` 시그니처(159-174행)에 `excludeDomains: Set<ActionDomain> = emptySet()` 파라미터 추가
   - 내부 `ActionDomainPicker` 호출부(1961행)와 순환 액션 편집용 `ZoneRotationEditor` 호출부 양쪽에 `excludeDomains` 전달
   - `StandardModePage.kt:774`의 `EdgeZoneEditorScreen(...)` 호출부에서 `selectedZonePage == 2`(Page 3)일 때 `excludeDomains = setOf(ActionDomain.MOVE, ActionDomain.DPI, ActionDomain.DYNAMICS, ActionDomain.SCROLL, ActionDomain.SCROLL_SPEED)` 전달
3. **CURSOR 예외 처리**:
   - `EdgeZoneActionResolver.kt:26`에서 `EdgeSwipeMode.CURSOR → ActionDomain.CLICK`로 매핑돼 있어, `ActionDomain.CLICK`을 통째로 제외하면 좌/우클릭까지 함께 사라짐
   - CursorMode는 4.9.1에서 이미 완전 배제하기로 결정했으므로(Page 2 전용 결합), 도메인 단위가 아닌 더 세밀한 필터(예: `ActionDomainPicker`에 `excludeActions: Set<EdgeZoneAction>` 신규 파라미터 추가, 또는 CLICK 도메인 내부에서 커서 관련 옵션만 걸러내는 분기)가 필요 — 구현 시점에 세부 방식 결정
4. **`zoneEditorDisabledEdges` 검토**:
   - `StandardModePage.kt:768-772`에 Page 3 전용 분기가 필요한지 확인 (예: 절대좌표 패드가 제어버튼과 겹치는 엣지가 있다면 해당 엣지 비활성화)
5. **`JumpToPage` 저장값 마이그레이션** (현재 상태 분석의 "인덱스 시프트 부작용" 노트 참조):
   - `EdgeZoneJson.kt`가 `EdgeZoneAction.JumpToPage(pageIndex)`를 정수 그대로 직렬화하므로, PAGE_COUNT 5→6 확장 + 인덱스 2 삽입 시 기존에 저장된 `pageIndex >= 2`인 값은 의미가 어긋난다(예: 구 "설정=4"가 신 배치에서 "마인크래프트=4"를 가리킴)
   - 1회성 마이그레이션 로직 추가: 앱이 로드한 기존 `standardAssignments`/커스텀 프리셋의 `JumpToPage` 액션 중 `pageIndex >= 2`인 것을 `pageIndex + 1`로 이동
   - 마이그레이션이 중복 실행되지 않도록 버전 플래그(예: SharedPreferences에 "page_index_migrated_v1" 같은 1회성 마커) 필요
   - 대상: 현재 저장돼 있을 수 있는 Page 1/2의 엣지존 할당(Phase 4.3/4.6/4.8에서 이미 사용 가능했던 기능이므로 실사용 데이터 존재 가능성 있음)

**신규 파일**: 없음 (기존 파일 배선만)

**수정 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/pages/StandardModePage.kt` (`standardTouchpadPages`, `EdgeZoneEditorScreen` 호출부, `zoneEditorDisabledEdges`, `JumpToPage` 마이그레이션 로직)
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/touchpad/EdgeZoneEditorScreen.kt` (`excludeDomains` 파라미터 추가 및 관통 배선)
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/touchpad/ZoneActionPicker.kt` (CURSOR 예외 처리를 위한 추가 필터 파라미터, 필요 시)

**검증**:
- [ ] Page 5 설정에서 "페이지 3"(절대좌표) 존 편집 진입 가능
- [ ] Page 3 편집기에서 MOVE/DPI/DYNAMICS/SCROLL/SCROLL_SPEED 액션 미노출
- [ ] Page 3 편집기에서 CURSOR 관련 옵션 미노출, CLICK(좌/우클릭)은 정상 노출
- [ ] Page 1/2 편집기는 기존과 동일하게 전체 액션 노출 (회귀 없음)
- [ ] 존 구조 편집(분할/병합/이동/삭제/비율조정)은 Page 3에서도 다른 페이지와 동일하게 동작
- [ ] 마이그레이션 전 저장된 `JumpToPage(4)`(구 "설정")가 마이그레이션 후 `JumpToPage(5)`로 이동해 여전히 설정 페이지로 점프하는지 확인
- [ ] 마이그레이션이 앱 재시작 시 중복 실행되지 않는지 확인

---

## Phase 4.9 완료 후 Page 3 구조

```
Page 3 — AbsolutePointingPad
├── PointingArea (16:9)
│   ├── 터치 → 절대좌표 (0~32767) 변환
│   ├── 줌 시 매핑 범위 축소 (zoomMin~zoomMax)
│   ├── CoordinateIndicator (십자선 + 점)
│   └── 엣지존/엣지스와이프 시스템 (좌표 무관 기능만: 매크로/단축키/페이지 전환/클릭/마우스 홀드)
├── ControlButtonContainer (상단 오버레이, 기존 컴포넌트 재사용, ClickMode만 노출)
│   ├── ClickModeButton (좌/우 토글)
│   └── ZoomButton (DPI 슬롯 자리, 줌 진입/해제, 배율 배지)
└── 시각 피드백
    ├── 테두리 색상 (핑크/노란/초록/주황)
    ├── 줌 레벨 텍스트 (앱 내)
    └── 줌 영역 박스 (PC 화면 — Android는 UART 전송까지만 담당, ESP32 중계/Windows 렌더링은 후속 통합 Phase)
```
