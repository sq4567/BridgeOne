---
title: "BridgeOne Phase 4.4: 터치패드 다이나믹스 및 UI"
description: "BridgeOne 프로젝트 Phase 4.4 - 포인터 다이나믹스, 프리셋 팝업 애니메이션, 테두리 색상, 버튼 구성 설정, 엣지 스와이프, 모드 프리셋"
tags: ["android", "touchpad", "pointer-dynamics", "preset", "edge-swipe", "animation", "border-color", "ui"]
version: "v1.0"
owner: "Chatterbones"
updated: "2026-04-02"
---

# BridgeOne Phase 4.4: 터치패드 다이나믹스 및 UI

**목표**: 포인터 다이나믹스(커서 가속), 프리셋 팝업 애니메이션, 테두리 모드 색상, 버튼 구성 설정, 엣지 스와이프 제스처, 모드 프리셋 등 터치패드 UI/UX 고도화를 완성합니다.

**핵심 성과물**:
- 포인터 다이나믹스 (커서 가속 알고리즘 + 프리셋 전환)
- 프리셋 팝업 등장/소멸 애니메이션
- 터치패드 테두리 모드 색상 표시 (단색/그라데이션)
- ControlButtonContainer 버튼 구성 설정
- 엣지 스와이프 제스처 + 산봉우리 애니메이션
- 모드 프리셋 버튼 (모드 구성 스냅샷 즉시 전환)

**선행 조건**: Phase 4.3 완료

---

## Phase 4.4.1: 포인터 다이나믹스 (커서 가속 알고리즘)

> **⚠️ Phase 4.3.7 변경사항**: `AppIcons.kt` 신규 (`ui/common/`). `AppIconDef` 데이터 모델, `AppIcons` 중앙 객체, `AppIcon()` 래퍼 Composable 모두 포함. 다이나믹스 프리셋 버튼 구현 시 `Icons.*` 직접 사용 금지 — `AppIcons.DynamicsOff / DynamicsPrecision / DynamicsStandard / DynamicsFast` 를 `AppIcon(def = ...)` 으로 렌더링할 것. `NormalScrollButtons.kt`는 이미 `AppIcons.ScrollUp / ScrollDown` 으로 마이그레이션 완료됨.

> **⚠️ Phase 4.3.6 변경사항**: `TouchpadState`에 `customDpiMultiplier: Float?` 필드 및 `effectiveDpiMultiplier` 프로퍼티 추가됨. DPI 유효 배율은 `effectiveDpiMultiplier` (`customDpiMultiplier ?: dpiLevel.multiplier`)로 계산됨. DPI 곱수는 `TouchpadWrapper.kt`의 커서 이동 `else` 분기에서 `axisLockedDelta × effectiveDpiMultiplier` → `coerceIn(-127f, 127f)`로 적용됨 (MOVE 및 UP 이벤트 모두). 포인터 다이나믹스 배율은 DPI 배율 **이후** 순서로 적용할 것 — 최종 delta = `rawDelta × dpiMultiplier × dynamicsMultiplier` → `coerceIn(-127f, 127f)`.
>
> **⚠️ Phase 4.3.6 추가 변경사항**: `DpiAdjustPopup.kt` 신규 생성됨 — `DynamicsPresetPopup` 구현 시 동일 패턴(반투명 오버레이 + 스와이프 조절 + 탭 확정 + 경계 피드백) 참조. `StandardModePage.kt`의 `Page1TouchpadActions`에 블러 + 팝업 오버레이 구조 추가됨 (`dpiAdjustPopupVisible` 상태 → `animateDpAsState`로 blur 0↔8dp 전환 → 외부 Box 안에 블러 Box + DpiAdjustPopup 형제 배치). 프리셋 팝업도 이 구조에 통합하여 `blurRadius = if (dpiPopup || presetPopup) 8.dp else 0.dp`로 관리. `ControlButtonContainer`에 `onDpiLongPress` 파라미터 추가됨 — 프리셋 롱프레스도 동일 패턴으로 `onDynamicsLongPress` 추가하면 됨.

**목표**: 손가락 이동 속도에 따라 커서 이동량이 동적으로 변하는 포인터 가속 알고리즘 구현. 알고리즘 파라미터는 `PointerDynamicsConstants.kt`에 프리셋으로 정의하고, 커서 이동 모드에서 터치패드 왼쪽 하단 버튼으로 프리셋을 전환합니다. 향후 Windows 서버 연동 시 런타임 변경도 지원합니다.

**개발 기간**: 1.5-2일

**알고리즘 종류**:

| 알고리즘 | 설명 |
|---------|------|
| **None** | 가속 없음 (기본값) — 속도 무관하게 DPI 배율만 적용 |
| **Windows EPP** | Windows "포인터 정밀도 향상"과 유사 — 느리면 배율 그대로, 빠르면 비선형(S커브) 가속 |
| **Linear** | 속도에 비례하여 배율이 선형으로 증가 — 단순하고 예측 가능 |

**세부 목표**:
1. `PointerDynamicsConstants.kt` 신규 파일 (프리셋 정의):
   ```kotlin
   // PointerDynamicsConstants.kt
   // 프리셋 목록 — 코드에서 추가/수정/삭제합니다. (3~5개 권장)
   val DYNAMICS_PRESETS: List<PointerDynamicsPreset> = listOf(
       PointerDynamicsPreset(
           name = "Off",
           algorithm = DynamicsAlgorithm.NONE,
           intensityFactor = 1.0f,
           velocityThresholdDpMs = 0.5f,
           maxMultiplier = 1.0f
       ),
       PointerDynamicsPreset(
           name = "Precision",
           algorithm = DynamicsAlgorithm.WINDOWS_EPP,
           intensityFactor = 0.8f,
           velocityThresholdDpMs = 0.6f,
           maxMultiplier = 2.5f
       ),
       PointerDynamicsPreset(
           name = "Standard",
           algorithm = DynamicsAlgorithm.WINDOWS_EPP,
           intensityFactor = 1.2f,
           velocityThresholdDpMs = 0.5f,
           maxMultiplier = 3.0f
       ),
       PointerDynamicsPreset(
           name = "Fast",
           algorithm = DynamicsAlgorithm.LINEAR,
           intensityFactor = 1.5f,
           velocityThresholdDpMs = 0.4f,
           maxMultiplier = 4.0f
       ),
   )
   val DEFAULT_PRESET_INDEX = 0  // "Off" — 가속 없음
   ```
2. `PointerDynamicsPreset` 데이터 모델 (`TouchpadMode.kt`에 추가):
   - `DynamicsAlgorithm` enum: `NONE`, `WINDOWS_EPP`, `LINEAR`
   - `PointerDynamicsPreset` data class (name + algorithm + 파라미터 3개)
   - `TouchpadState`에 `dynamicsPresetIndex: Int` 필드 추가 (기본값: `DEFAULT_PRESET_INDEX`)
   - 현재 프리셋 접근: `DYNAMICS_PRESETS[touchpadState.dynamicsPresetIndex]`
3. 알고리즘 구현 (`DeltaCalculator.kt`):
   - `applyPointerDynamics(rawDelta: Float, velocityDpMs: Float, preset: PointerDynamicsPreset): Float` 추가
   - **NONE**: `rawDelta` 그대로 반환 (배율 = 1.0)
   - **WINDOWS_EPP**: `velocity < threshold` → 배율 1.0, 이후 sigmoid 근사 S커브 증가
     - `multiplier = 1.0 + intensityFactor × sigmoid((velocity / threshold − 1) × 2)`
   - **LINEAR**: `multiplier = 1.0 + intensityFactor × max(0f, (velocity − threshold) / threshold)`
   - 모든 알고리즘: 최종 배율 `coerceIn(1.0f, preset.maxMultiplier)` 적용
4. 속도 계산 및 `TouchpadWrapper.kt` 연동:
   - Historical 터치 샘플 기반 순간 속도(`velocityDpMs`) 계산 (이미 Historical 샘플 처리 코드 존재 → 재활용)
   - 적용 순서: `rawDelta × dpiMultiplier` 결과에 다이나믹스 배율 추가 적용 → `coerceIn(-127f, 127f)`
   - 스크롤 모드·직각 이동 모드 중에는 다이나믹스 배율 적용 **제외** (커서 이동 모드에만 적용)
5. **프리셋 선택 버튼 UI** (`DynamicsPresetButton.kt` 신규):
   - **위치**: 커서 이동 모드(`ScrollMode.OFF`)일 때 터치패드 왼쪽 하단 (`Alignment.BottomStart`, `padding(start = 8.dp, bottom = 8.dp)`) — `NormalScrollButtons`와 동일 위치, `AnimatedVisibility`로 교대 표시
   - **크기**: 40dp × 40dp (기존 스크롤 버튼과 동일)
   - **표시 내용**: 현재 프리셋 이름 약어 (예: "Off", "Pre", "Std", "Fst") — 텍스트 또는 아이콘
   - **원탭**: 다음 프리셋으로 사이클 전환 (`index = (index + 1) % DYNAMICS_PRESETS.size`)
   - **롱프레스**: 프리셋 목록 팝업 표시 → 탭으로 원하는 프리셋 직접 선택
   - **이벤트 소비**: `down.changes.forEach { it.consume() }` — 부모 터치패드 제스처에 전파 차단
   - **Essential 모드**: 숨김 (ControlButtonContainer와 동일하게)
6. **프리셋 선택 팝업 오버레이** (`DynamicsPresetPopup.kt` 신규) — `DpiAdjustPopup`과 동일 패턴:
   - **트리거**: `DynamicsPresetButton` 롱프레스 시 `showPresetPopup = true` → 팝업 표시
   - **구현 방식**: TouchpadWrapper Box 내부에 `AnimatedVisibility` 오버레이 (`DpiAdjustPopup`과 동일 패턴)
   - **영역**: 터치패드 **전체 영역**을 덮는 반투명 오버레이
   - **블러 배경**: DPI 팝업과 동일 — 팝업이 열리면 뒤쪽의 ControlButtonContainer + 터치패드 영역 전체에 블러 적용
     - `Modifier.blur(radius = 8.dp)` — 팝업 뒤 전체 콘텐츠에 적용
     - 블러 + 반투명 오버레이 `Color.Black.copy(alpha = 0.4f)` 중첩
     - 블러 진입/해제: `animateDpAsState`로 0.dp ↔ 8.dp 부드럽게 전환
     - **구현 공유**: DPI 팝업과 프리셋 팝업이 동일한 블러 로직을 사용하므로, 블러 상태 관리를 `showDpiPopup || showPresetPopup`으로 통합
   - **레이아웃** (선택 단계):
     ```
     ┌──────────────────────────────────────────────────────┐
     │       원하는 알고리즘 프리셋을 선택하세요.               │  ← 최상단 가운데
     │                                                      │
     │  ┌──────┐   ┌──────┐  ┌ ──────┐   ┌──────┐         │
     │  │ [🖱] │   │ [🖱] │  │  [🖱] │   │ [🖱] │         │
     │  └──────┘   └──────┘  └──────-┘   └──────┘         │
     │    Off        Pre    ┌──────────┐    Fst             │
     │                      │  [🖱]   │                    │
     │                      └──────────┘                   │
     │                        Standard                      │
     │                      ↑ 테두리가 현재 항목으로 이동      │
     │                                                      │
     │  상하 좌우 스와이프로 원하는 프리셋 선택 | 탭으로 결정   │  ← 하단 가운데, 작게
     └──────────────────────────────────────────────────────┘
     ```
   - **레이아웃** (탭 후 확인 단계):
     ```
     ┌──────────────────────────────────────────────────────┐
     │                                                      │
     │        ┌──────────────────────────────────┐          │
     │        │          Standard                │  ← 커진 항목
     │        │  [아이콘]                          │
     │        │  알고리즘: Windows EPP             │
     │        │  느리면 정밀, 빠르면 자동 가속       │  ← 설명
     │        └──────────────────────────────────┘          │
     │                                                      │
     │              ◄ 아니요      예 ►                      │  ← 좌우 스와이프로 선택
     │                  (탭으로 확정)                        │
     │                                                      │
     └──────────────────────────────────────────────────────┘
     ```
   - **그리드 구성**:
     - 프리셋 목록을 가로 한 줄 또는 2×N 그리드로 배치 (프리셋 수에 따라 결정)
     - 각 셀 구조: **네모 배경(Box, 64dp×64dp) 위에 아이콘** + 그 **아래에 프리셋 이름 텍스트** (12sp, `Color.White`)
       - 네모 배경: 둥근 모서리(`RoundedCornerShape(8.dp)`), 반투명 흰색(`Color.White.copy(alpha = 0.15f)`)
       - 아이콘: 배경 Box 중앙에 배치 (32dp)
       - 프리셋 이름: 배경 Box 바깥 아래쪽에 텍스트 배치
     - 현재 선택 항목은 흰색 테두리(`Border 2dp`)로 강조 — 스와이프 시 테두리가 해당 셀로 이동
     - 비선택 항목은 테두리 없음 + 약간 어둡게 처리 (`alpha = 0.6f`)
   - **스와이프 조작** (그리드 탐색):
     - 좌우 스와이프: 같은 행 내에서 프리셋 이동 (좌 = 이전, 우 = 다음)
     - 상하 스와이프: 위/아래 행으로 이동 (그리드 2행 이상일 때)
     - 전환 임계값: 30dp 드래그 시 1단계 이동
     - 범위 초과 시 피드백:
       - 테두리 셀 잠시 붉은색으로 변경 후 원래 색으로 복귀
       - 진동 피드백 (햅틱)
     - 순환하지 않음: 첫/마지막 셀 도달 시 경계 피드백
   - **탭 — 1단계 (확인 화면 진입)**:
     - 그리드 테두리 사라짐
     - 선택한 프리셋 셀이 확대되며 화면 중앙에 단독 표시
     - 셀 내부에 프리셋 이름(큰 텍스트, 20sp, Bold) + 한 줄 설명(14sp, `Color.Gray`) 표시
     - `예` / `아니요` 텍스트가 중앙 셀 아래에 나란히 표시 — 기본값 `아니요` 선택 상태
   - **확인 단계 조작**:
     - 좌우 스와이프로 `예` ↔ `아니요` 토글 (30dp 임계값, 선택된 항목 흰색으로 강조)
     - 탭 시:
       - `예` 선택 상태 → 해당 프리셋 확정, 팝업 닫힘, 햅틱 피드백
       - `아니요` 선택 상태 → 그리드 선택 화면으로 복귀 (프리셋 변경 없음)
   - **취소 조건**:
     - 페이지 전환 발생 시 → 프리셋 변경 없이 팝업 취소 (원래 프리셋 유지)
     - 스크롤 모드 전환 시 → 팝업 취소
   - **이벤트 차단**: 팝업 내 `pointerInput`에서 `event.changes.forEach { it.consume() }` — TouchpadWrapper의 `if (down.changes.any { it.isConsumed }) return@awaitEachGesture` 패턴으로 자동 차단 (Phase 4.3.4.5에서 확립된 패턴)
   - **Compose 구조 (의사 코드)**:
     ```kotlin
     @Composable
     fun DynamicsPresetPopup(
         currentIndex: Int,
         presets: List<PointerDynamicsPreset>,
         onPresetConfirmed: (Int) -> Unit,
         onDismiss: () -> Unit
     ) {
         // 선택 단계 vs 확인 단계
         var phase by remember { mutableStateOf(PopupPhase.GRID) }  // GRID | CONFIRM
         var tempIndex by remember { mutableIntStateOf(currentIndex) }
         var confirmChoice by remember { mutableStateOf(false) }  // false = 아니요, true = 예
         var accumulatedDragDp by remember { mutableFloatStateOf(0f) }

         Box(
             modifier = Modifier
                 .fillMaxSize()
                 .background(Color.Black.copy(alpha = 0.6f))
                 .pointerInput(phase) {
                     awaitEachGesture {
                         val down = awaitFirstDown()
                         down.consume()
                         var dragged = false
                         while (true) {
                             val event = awaitPointerEvent()
                             val change = event.changes.first()
                             if (change.pressed) {
                                 val delta = change.positionChange()
                                 accumulatedDragDp += when (phase) {
                                     PopupPhase.GRID -> delta.x.toDp()   // 좌우 = 프리셋 이동
                                     PopupPhase.CONFIRM -> delta.x.toDp() // 좌우 = 예/아니요 토글
                                 }
                                 val steps = (accumulatedDragDp / 30f).toInt()
                                 if (steps != 0) {
                                     when (phase) {
                                         PopupPhase.GRID -> {
                                             tempIndex = (tempIndex + steps)
                                                 .coerceIn(0, presets.lastIndex)
                                         }
                                         PopupPhase.CONFIRM -> {
                                             confirmChoice = steps > 0  // 오른쪽 = 예
                                         }
                                     }
                                     accumulatedDragDp -= steps * 30f
                                     dragged = true
                                 }
                                 change.consume()
                             } else {
                                 if (!dragged) {
                                     // 탭 처리
                                     when (phase) {
                                         PopupPhase.GRID -> {
                                             // 그리드에서 탭 → 확인 단계 진입
                                             confirmChoice = false
                                             phase = PopupPhase.CONFIRM
                                         }
                                         PopupPhase.CONFIRM -> {
                                             if (confirmChoice) {
                                                 onPresetConfirmed(tempIndex)  // 예 → 확정
                                             } else {
                                                 phase = PopupPhase.GRID  // 아니요 → 그리드 복귀
                                             }
                                         }
                                     }
                                 }
                                 break
                             }
                         }
                     }
                 }
         ) {
             Column(
                 modifier = Modifier.fillMaxSize(),
                 horizontalAlignment = Alignment.CenterHorizontally
             ) {
                 when (phase) {
                     PopupPhase.GRID -> {
                         Text("원하는 알고리즘 프리셋을 선택하세요.", fontSize = 16.sp, ...)
                         Spacer(Modifier.weight(1f))
                         // 프리셋 그리드 (LazyHorizontalGrid 또는 Row)
                         Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                             presets.forEachIndexed { idx, preset ->
                                 val isSelected = idx == tempIndex
                                 Column(
                                     horizontalAlignment = Alignment.CenterHorizontally,
                                     modifier = Modifier.alpha(if (isSelected) 1f else 0.6f)
                                 ) {
                                     // 네모 배경 위에 아이콘
                                     Box(
                                         modifier = Modifier
                                             .size(64.dp)
                                             .clip(RoundedCornerShape(8.dp))
                                             .background(Color.White.copy(alpha = 0.15f))
                                             .then(
                                                 if (isSelected) Modifier.border(2.dp, Color.White, RoundedCornerShape(8.dp))
                                                 else Modifier
                                             ),
                                         contentAlignment = Alignment.Center
                                     ) {
                                         Icon(preset.icon, contentDescription = preset.name, modifier = Modifier.size(32.dp), ...)
                                     }
                                     // 프리셋 이름 (배경 Box 아래)
                                     Text(preset.name, fontSize = 12.sp, ...)
                                 }
                             }
                         }
                         Spacer(Modifier.weight(1f))
                         Text(
                             "상하 좌우 스와이프로 원하는 프리셋 선택 | 탭으로 결정",
                             fontSize = 11.sp,
                             color = Color.White.copy(alpha = 0.6f),
                             textAlign = TextAlign.Center
                         )
                     }
                     PopupPhase.CONFIRM -> {
                         Spacer(Modifier.weight(1f))
                         // 선택 프리셋 확대 표시
                         Box(modifier = Modifier.border(2.dp, Color.White).padding(24.dp)) {
                             Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                 Icon(presets[tempIndex].icon, ...)
                                 Text(presets[tempIndex].name, fontSize = 20.sp, fontWeight = Bold, ...)
                                 Text(presets[tempIndex].description, fontSize = 14.sp, color = Color.Gray, ...)
                             }
                         }
                         Spacer(modifier = Modifier.height(24.dp))
                         // 예 / 아니요 선택
                         Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                             Text("아니요", color = if (!confirmChoice) Color.White else Color.Gray, ...)
                             Text("예", color = if (confirmChoice) Color.White else Color.Gray, ...)
                         }
                         Spacer(Modifier.weight(1f))
                     }
                 }
             }
         }
     }
     ```
7. Windows 서버 연동 준비:
   - `TouchpadState.dynamicsPresetIndex`는 런타임에 교체 가능한 구조
   - 향후 서버에서 프리셋 인덱스를 전달받아 `onTouchpadStateChange`로 업데이트하면 즉시 적용됨

**신규 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/common/PointerDynamicsConstants.kt`
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/touchpad/DynamicsPresetButton.kt`
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/touchpad/DynamicsPresetPopup.kt`

**수정 파일**:
- `TouchpadMode.kt` (`DynamicsAlgorithm` enum + `PointerDynamicsPreset` data class + `TouchpadState` 확장)
- `DeltaCalculator.kt` (`applyPointerDynamics()` 추가)
- `TouchpadWrapper.kt` (속도 계산 + 다이나믹스 배율 적용 + `DynamicsPresetButton` 오버레이 추가)

**참조 문서**:
- `docs/android/technical-specification-app.md` §2.2 (터치패드 알고리즘)
- `NormalScrollButtons.kt` (동일 위치·패턴 참조)

> **⚠️ Phase 4.4.2 참고**: `TouchpadWrapper.kt`에서 delta 적용 순서(`dpiMultiplier` → `dynamicsMultiplier`)가 테두리 색상 Modifier와 독립적으로 구현됨. 두 기능의 코드 위치 충돌 없음. `DynamicsPresetButton`은 `NormalScrollButtons`와 동일 위치에 `AnimatedVisibility`로 교대 표시되므로 오버레이 충돌 없음.

**검증 (UX — 에뮬레이터/실기기 UI 확인)**:
- [x] 프리셋 버튼 — 커서 이동 모드에서 왼쪽 하단에 표시 확인
- [x] 프리셋 버튼 — 원탭으로 프리셋 사이클 전환 (Off → Precision → Standard → Fast → Off)
- [x] 프리셋 버튼 — 탭 시 프리셋 이름 라벨 표시 후 자동 소멸 확인
- [x] 프리셋 버튼 — 롱프레스 시 터치패드 전체 영역 팝업 오버레이 표시 확인
- [x] 팝업 — 반투명 배경 + 프리셋 그리드 (FlowRow 아이콘 + 이름) 표시 확인
- [x] 팝업 — 현재 활성 프리셋 강조 (파란 배경 + 볼드) 확인
- [x] 팝업 — 반투명 카드 안내 텍스트 표시 확인
- [x] 팝업 — 좌우 스와이프 시 테두리가 해당 프리셋 셀로 이동 확인 (30dp당 1단계)
- [x] 팝업 — 범위 초과 시 붉은색 테두리 + 햅틱 피드백 확인
- [x] 팝업 — 현재 프리셋 탭 → 팝업 닫힘 (취소) 확인
- [x] 팝업 — 다른 프리셋 탭 → 확인 단계 진입 (아이콘 확대 + 이름 + 설명) 확인
- [x] 팝업 — 확인 단계: 좌우 스와이프로 예/아니요 전환 확인
- [x] 팝업 — 확인 단계: 탭(예) → 프리셋 확정 + 팝업 닫힘 + 햅틱 피드백 확인
- [x] 팝업 — 확인 단계: 탭(아니요) → 그리드 선택 화면으로 복귀 확인
- [x] 프리셋 버튼 — 스크롤 모드 진입 시 버튼 페이드아웃 확인
- [x] 프리셋 버튼 탭 시 터치패드 커서 이동 미발생 (이벤트 소비 확인)

> 하드웨어 의존 검증 (커서 가속 동작, DPI 중첩, 배율 상한 등)은 **Phase 4.4.9 E2E 하드웨어 테스트**에서 수행

---

## Phase 4.4.2: 포인터 다이나믹스 프리셋 팝업 애니메이션

**목표**: Phase 4.4.1에서 구현한 포인터 다이나믹스 프리셋 팝업에 전반적인 애니메이션을 추가하여 전환이 매끄럽고 직관적으로 느껴지게 합니다.

**개발 기간**: 1일

**쉬운 설명**: 프리셋 선택 팝업을 열고 닫을 때, 그리고 확인 화면으로 넘어갈 때 애니메이션이 없어서 화면이 갑자기 바뀌는 느낌이 납니다. 이 Phase에서는 팝업이 열릴 때 아이콘들이 화면 바깥에서 중앙으로 모여드는 효과, 닫힐 때 아이콘들이 화면 밖으로 흩어지는 효과, 프리셋을 고를 때 선택된 아이콘이 커지면서 확인 화면으로 자연스럽게 전환되는 효과 등을 추가합니다.

**세부 목표**:

1. **팝업 등장 애니메이션** (GRID 단계 enter):
   - 배경 dimming: `alpha 0 → 0.6` fade-in (200ms)
   - 프리셋 아이콘 각각: 화면 아래쪽 밖에서 위로 올라오며 등장 (offset slide-up + fade-in)
   - 각 아이콘에 stagger 딜레이(30ms) 적용 → 첫 번째 아이콘부터 순서대로 등장
   - 안내 텍스트/카드: 아이콘들 등장 직후 fade-in

2. **팝업 닫힘 애니메이션** (취소 시):
   - 배경 dimming: `alpha 0.6 → 0` fade-out (200ms)
   - 프리셋 아이콘 각각: 화면 아래 밖으로 내려가며 퇴장 (offset slide-down + fade-out)
   - stagger 딜레이 적용
   - 애니메이션 완료 후 실제 팝업 제거

3. **GRID → CONFIRM 전환** (다른 프리셋 탭 시):
   - 선택된 프리셋 아이콘: 그리드 크기(56dp)에서 확인 화면 크기(80dp)로 scale-up되며 중앙 이동
   - 비선택 아이콘들 + 안내 텍스트: fade-out
   - CONFIRM 텍스트(이름, 설명, 예/아니요): fade-in + slide-up
   - `AnimatedContent(targetState = phase)` 활용

4. **CONFIRM → GRID 복귀** (아니요 선택 시):
   - CONFIRM 내용: fade-out
   - 아이콘들: 그리드 크기로 축소되며 원래 위치로 복귀 (fade-in)

5. **DynamicsPresetButton 롱프레스 트리거 애니메이션**:
   - 롱프레스 발동 시 버튼 아이콘이 잠깐 scale-up(1.0 → 1.3 → 1.0)되며 팝업이 열림을 시각적으로 표시

**구현 방법**:
- `DynamicsPresetPopup`에 `visible: Boolean` 파라미터 추가
- `LaunchedEffect(visible)` + `Animatable` 조합으로 배경 dimming alpha 제어
- 각 아이콘의 stagger offset: `List<Animatable<Float>>` (아이콘 수만큼) + `LaunchedEffect` 내 순차 launch
- GRID ↔ CONFIRM 전환: `AnimatedContent(targetState = phase, transitionSpec = { ... })`
- 닫힘 시 exit 애니메이션 완료 후 `onDismiss` 콜백 호출 (직접 호출 대신 `coroutineScope.launch { animate(); onDismiss() }`)
- `StandardModePage.kt`: 기존 `if (showPopup) DynamicsPresetPopup(...)` 방식을 `visible` 파라미터 전달 방식으로 변경

**수정 파일**:
- `DynamicsPresetPopup.kt` — visible 파라미터 추가, 등장/닫힘/단계 전환 애니메이션 전반
- `StandardModePage.kt` — visible 파라미터 전달 방식으로 호출부 변경
- `DynamicsPresetButton.kt` — 롱프레스 시 scale-up 트리거 애니메이션 추가

**검증**:
- [x] 팝업 열릴 때 배경 dimming fade-in 확인
- [x] 팝업 열릴 때 아이콘들이 아래에서 위로 stagger 슬라이드 확인
- [x] 팝업 닫힐 때 아이콘들이 아래로 내려가며 사라짐 확인
- [x] 팝업 닫힘 후 배경 완전히 사라짐 (alpha 0) 확인
- [x] GRID → CONFIRM: AnimatedContent fade+slide 전환 확인 (slide-up 방향, icon scale-up은 AnimatedContent 전환으로 대체)
- [x] CONFIRM → GRID: AnimatedContent fade+slide 복귀 확인
- [x] DynamicsPresetButton 롱프레스 시 scale-up 효과 확인

---

## Phase 4.4.3: 터치패드 테두리 모드 색상 표시

> **⚠️ 이전 Phase 변경사항 요약**
>
> **신규 파일**
> - `ScrollGuideline.kt` — 등간격 다중 선 패턴 (단일 선 아님), `DrawScope.rotate()` 기반 축 전환 회전 애니메이션. `scrollMode: ScrollMode` 파라미터로 색상 분기
> - `PointerDynamicsConstants.kt` — 포인터 다이나믹스 상수/프리셋 정의
> - `DynamicsPresetPopup.kt` — 프리셋 선택 팝업 (`StandardModePage.kt`에서 터치패드 Box 위에 배치)
>
> **TouchpadWrapper.kt — Box 구조 변경 (⚠️ 핵심)**
> - 외부 Box(unclipped) + 내부 Box(clipped) **2중 구조**로 변경됨
> - 테두리 Modifier는 **내부 Box에 적용**해야 함. ~~현재 `CORNER_RADIUS = 5.dp`~~ → Phase 4.4.3에서 `12.dp`로 수정됨
> - `DynamicsPresetButton` 오버레이 추가 (커서 이동 모드 시 `Alignment.BottomStart`, `NormalScrollButtons`와 교대 표시)
> - `NormalScrollButtons` 오버레이 추가 (NORMAL_SCROLL 모드 시 `Alignment.BottomStart`, `padding(start=8.dp, bottom=8.dp)`, 40dp×84dp 영역 점유)
> - `Vibrator` 인스턴스 추가 (무한 스크롤 속도 비례 연속 진동용)
>
> **TouchpadMode.kt**
> - `DynamicsAlgorithm` enum, `PointerDynamicsPreset` data class 추가
> - `TouchpadState`에 `dynamicsPresetIndex: Int` 필드 추가 — 테두리 색상 로직에는 영향 없음
>
> **색상 상수 현황 (`TouchpadColors.kt` 추출 시 참고)**
> - `ControlButtonContainer.kt`: 색상 상수 및 아이콘 헬퍼(`dpiButtonIcon`, `scrollModeButtonIcon`, `scrollSensitivityButtonIcon`) 모두 private — 추출 시 아이콘 헬퍼는 ControlButtonContainer에 유지
> - `ScrollGuideline.kt`: `ScrollGuidelineColorNormal = Color(0xFF84E268)` (초록), `ScrollGuidelineColorInfinite = Color(0xFFF32121)` (빨강) — 추출 시 두 상수 모두 이동 후 참조 변경 필요
>
> **기타**
> - `AppIcons.kt`: `DynamicsOff/Precision/Standard/Fast` 아이콘 추가
> - `ScrollConstants.kt`: `INFINITE_SCROLL_HAPTIC_MAX_VELOCITY_DP_MS = 2.0f` 추가
> - `StandardModePage.kt`: 스크롤 모드 활성 시 부모 Box에 `PointerEventPass.Initial` Move 이벤트 선제 소비 적용 — 테두리 Modifier와 충돌 없음

**목표**: 현재 활성 모드 조합에 따라 터치패드 테두리를 단색 또는 좌→우 그라데이션으로 표시

**개발 기간**: 0.5-1일

**세부 목표**:
1. 테두리 사양:
   - 너비: 터치패드 너비의 0.8% (최소 2dp, 최대 4dp)
   - 적용 대상: `TouchpadAreaWrapper` (싱글 커서 모드에서 `Touchpad1Area`)
   - 라운드 코너: 기존 12dp 코너에 맞춤
2. 색상 결정 우선순위 (§2.2.1):
   - **1순위** 무한 스크롤 활성: 빨간색 `#F32121` 단색
   - **2순위** 일반 스크롤 활성: 초록색 `#84E268` 단색
   - **3순위** 커서 이동 모드: 클릭×이동 모드 조합 (§2.2.2 조합 규칙)
3. 조합 규칙 (§2.2.2 — 커서 이동 모드 상태에서):
   - 좌클릭(파랑) + 자유이동(파랑) → 파란색 `#2196F3` 단색
   - 좌클릭(파랑) + 직각이동(주황) → 주황색 `#FF8A00` 단색 (파랑 아닌 쪽 사용)
   - 우클릭(노랑) + 자유이동(파랑) → 노란색 `#F3D021` 단색 (파랑 아닌 쪽 사용)
   - 우클릭(노랑) + 직각이동(주황) → 좌=노란색, 우=주황색 **좌→우 그라데이션**
4. 테두리 색상 함수:
   - `TouchpadState`를 입력으로 받아 단색(`Color`) 또는 그라데이션(`Brush`)을 반환
   - 색상 상수: `ControlButtonContainer.kt`의 private 색상을 공용 파일로 추출 (또는 별도 `TouchpadColors.kt` 생성)
5. 그라데이션 구현:
   - `Brush.horizontalGradient(listOf(leftColor, rightColor))` 활용
   - `Modifier.border(width, brush, shape)` 으로 테두리 적용
6. Essential 모드:
   - 테두리 완전 비표시 (ControlButtonContainer와 동일하게 숨김)

**신규 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/touchpad/TouchpadColors.kt` — 터치패드 관련 공용 색상 상수 및 테두리 색상 결정 함수

**수정 파일**:
- `TouchpadWrapper.kt` — 테두리 Modifier 적용
- `ControlButtonContainer.kt` — private 색상 상수를 `TouchpadColors.kt`로 이동 후 참조 변경

**참조 문서**:
- `docs/android/component-touchpad.md` §2.2 (테두리 색상 적용 규칙)
- `docs/android/component-touchpad.md` §2.2.1 (결정 기준)
- `docs/android/component-touchpad.md` §2.2.2 (조합 규칙)

**검증**:
- [x] 기본 상태(좌클릭 + 자유이동): 파란색 단색 테두리
- [x] 우클릭 전환 시: 노란색 단색 테두리
- [x] 직각 이동 전환 시: 주황색 단색 테두리
- [x] 우클릭 + 직각 이동: 노란색→주황색 그라데이션 테두리
- [x] 일반 스크롤 모드: 초록색 단색 테두리
- [x] 무한 스크롤 모드: 빨간색 단색 테두리 (최우선)
- [x] 모드 전환 시 테두리 색상 즉시 반영
- [x] Essential 모드에서 테두리 비표시
- [x] 테두리 너비 2~4dp 범위 준수

---

## Phase 4.4.4: ControlButtonContainer 버튼 구성 설정

> **⚠️ Phase 4.4.3 변경사항**
>
> **신규 파일**
> - `TouchpadColors.kt` — 터치패드 공용 색상 팔레트 9개 + `touchpadBorderColors(TouchpadState): Pair<Color, Color>` 함수. ControlButtonContainer.kt는 이미 이 파일 참조로 변경 완료.
>
> **TouchpadWrapper.kt — 테두리 구현 (⚠️ 핵심)**
> - 테두리 색상: `touchpadBorderColors(state)` + `animateColorAsState` (300ms) 좌/우 독립 애니메이션
> - 테두리 Brush: `Brush.linearGradient` + 3초 주기 글로우 애니메이션 (계획의 단순 `horizontalGradient` 아님)
>   - 중심점 범위: `(-widthPx) → (+2*widthPx)` (off-screen 기법, seamless loop)
> - `CORNER_RADIUS = 12.dp` (이전 노트의 5.dp는 오기 — 실제 12dp로 구현됨)
> - Modifier 순서: `.clip(RoundedCornerShape(CORNER_RADIUS))` → `.border(...)` (clip 먼저)
> - Essential 모드: `Color.Transparent` 대입으로 테두리 비표시
>
> **ControlButtonContainer.kt**
> - private 색상 상수 제거 → `TouchpadColors.kt` 상수 참조 (`private val ColorBlue = TouchpadColorBlue` 등 alias 유지)
>
> **ScrollGuideline.kt**
> - `ScrollGuidelineColorNormal/Infinite` → `TouchpadColorGreen/Red` 참조로 교체

**목표**: `component-touchpad.md` §1.3 버튼 구성 독립성 원칙에 따라, 터치패드 인스턴스별로 특정 제어 버튼을 완전히 제외할 수 있도록 `ControlButtonContainer`에 버튼 구성 파라미터를 추가합니다. 비표시로 설정된 버튼은 UI에서 완전히 제거되고, 나머지 버튼들의 간격이 자연스럽게 재배치됩니다.

**개발 기간**: 0.5일

**배경**: 현재 `CursorModeButton`은 Page 1에서 이미 비표시로 구성되어 있습니다(§1.3 현재 배치 구성 참조). 이 Phase에서는 이 구성 방식을 나머지 버튼들에도 동일하게 확장합니다.

**세부 목표**:
1. **`ControlButtonConfig` 데이터 클래스** 신규 정의 (`ControlButtonContainer.kt` 또는 `TouchpadMode.kt`):
   ```kotlin
   data class ControlButtonConfig(
       val showClickMode: Boolean = true,
       val showMoveMode: Boolean = true,
       val showScrollMode: Boolean = true,
       val showCursorMode: Boolean = false,  // Page 1 기본값: 비표시
       val showDpi: Boolean = true,
       val showScrollSensitivity: Boolean = true  // 슬롯 공유이므로 DPI와 함께 관리
   )
   ```
   - 기본값은 현재 Page 1 구성과 동일하게 설정
2. **`ControlButtonContainer` 파라미터 추가**:
   - `config: ControlButtonConfig = ControlButtonConfig()` 파라미터 추가
   - 기존 `CursorModeButton` 조건부 렌더링 로직을 `config.showCursorMode`로 교체
3. **레이아웃 동적 재배치**:
   - 비표시 버튼은 고정 크기 Box 슬롯 포함 완전히 제외 (기존 `AnimatedVisibility`와 구분)
   - `Modifier.weight(1f)` 기반으로 활성 버튼 수에 따라 너비 자동 균등 분배
   - Phase 4.3.2에서 도입된 ClickMode/MoveMode 고정 Box는 `config.showClickMode/showMoveMode = true`인 경우에만 렌더링
4. **모드 상태 정합성**:
   - `showClickMode = false`: `PadModeState.clickMode = ClickMode.LEFT_CLICK`으로 강제
   - `showMoveMode = false`: `PadModeState.moveMode = MoveMode.FREE`로 강제
   - `showScrollMode = false`: `PadModeState.scrollMode = ScrollMode.NONE`으로 강제
5. **애니메이션 미적용**:
   - 구성(config) 기반 비표시는 애니메이션 없이 즉시 반영
   - Phase 4.3.2의 모드 전환 애니메이션(스크롤 ON/OFF 시 버튼 교체)은 `config.showScrollMode = true`인 경우에만 동작

**수정 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/touchpad/ControlButtonContainer.kt`
  — `ControlButtonConfig` data class 추가, `config` 파라미터 추가, 레이아웃 동적 재배치 적용

**참조 문서**:
- `docs/android/component-touchpad.md` §1.3 (버튼 구성 독립성)

**실제 구현 내용 (계획과 다른 부분)**:
- **레이아웃 방식**: 계획의 `Modifier.weight(1f)` 균등 너비 분배 대신 **버튼 크기 고정 + extraInset 센터링** 방식으로 구현
  - 버튼 너비는 항상 5슬롯 기준으로 고정 (버튼이 줄어도 크기 불변)
  - 비표시 슬롯 수에 비례해 `extraInset`을 양쪽에 균등 분배 → 남은 버튼들이 안쪽으로 센터링
  - 버튼 간 간격은 항상 `buttonSpacing(2dp)` 고정
  - 수식: `extraInset = (availableWidth - visibleCount*buttonWidth - (visibleCount-1)*buttonSpacing) / 2`
- **후속 Phase 영향**: `ControlButtonContainer` 레이아웃 비율 변경 없음 — 버튼 크기·위치 기준은 동일

**검증**:
- [x] `ControlButtonConfig` 기본값으로 호출 시 현재 Page 1 구성과 동일하게 렌더링
- [x] `showMoveMode = false`: MoveModeButton 슬롯 완전 제거, 나머지 버튼 고정 너비·균등 간격 유지
- [x] `showClickMode = false`: ClickModeButton 슬롯 완전 제거, 나머지 버튼 고정 너비·균등 간격 유지
- [x] `showScrollMode = false`: ScrollModeButton 슬롯 완전 제거, 스크롤 모드 전환 애니메이션 미발생 확인
- [x] `showScrollMode = false` 시 `DPIControlButton ↔ ScrollSensitivityButton` 교체 애니메이션 미발생 확인
- [x] 비표시 버튼에 해당하는 모드 상태 강제값 적용 확인 (FREE 이동, 좌클릭 고정 등)
- [x] 모드 기반 일시 숨김(스크롤 ON/OFF 시 ClickMode 교체)은 `showClickMode = true`인 경우 기존대로 정상 동작 확인

---

## Phase 4.4.5: 터치패드 엣지 스와이프 제스처

**목표**: 터치패드 상단에 고정된 `ControlButtonContainer`에 손이 닿기 어렵다는 접근성 문제를 해결합니다. **어느 가장자리에서든** 동일한 제스처로 모드 선택 팝업을 열 수 있어, 상단 버튼에 손이 닿지 않아도 불편함 없이 모드를 전환할 수 있습니다.

**개발 기간**: 0.5-1일

**에뮬레이터 호환성**: 에뮬레이터에서 기능 확인 가능. 실기기에서 느낌 검증 필요.

**세부 목표**:
1. **`EdgeSwipeConstants` 상수 추가** (`ui/common/ScrollConstants.kt` 내 또는 별도 파일):
   ```kotlin
   object EdgeSwipeConstants {
       const val EDGE_HIT_WIDTH_DP           = 24f  // 가장자리에서 이 폭 이내 시작해야 엣지 스와이프 후보
       const val TRIGGER_DISTANCE_DP         = 28f  // 이 이상 안쪽으로 이동 시 팝업 등장
       const val CANCEL_THRESHOLD_DP         = 12f  // 팝업 등장 후, 진입 엣지에서 이 폭 이내로 되돌아오면 취소 (EDGE_HIT_WIDTH_DP보다 엄격)
       const val BUMP_APPEAR_THRESHOLD_DP =  4f  // 이 이상 안쪽으로 이동 시 산봉우리 애니메이션 등장 (4.3.13)
   }
   ```
2. **`EdgeSwipeOverlay.kt` 신규** — 모드 선택 팝업 오버레이 (애니메이션 없는 기본 구현):
   - **팝업**: `DynamicsPresetPopup`과 동일한 스타일, 애니메이션 없이 즉시 표시
     - 터치패드 전체 영역에 어두운 반투명 배경 (alpha 0.6, 즉시 표시 — fade-in 애니메이션은 Phase 4.4.7)
     - 중앙에 모드 아이콘 그리드 — `ControlButtonConfig` 기준으로 `showXxx = true`인 모드만 렌더링 (최대 4개, 최소 1개)
     - 아이콘 그리드 즉시 표시 (stagger slide-up 등장 애니메이션은 Phase 4.4.7)
     - 손가락 현재 위치에서 가장 가까운 아이콘에 흰 테두리 강조
     - 하단 안내 카드: "원하는 모드로 이동 후 손 떼기 / 들어온 가장자리로 되돌리면 취소"
   - **파라미터**:
     ```kotlin
     @Composable
     fun EdgeSwipeOverlay(
         touchpadWidthPx: Float,
         touchpadHeightPx: Float,
         touchpadState: TouchpadState,
         config: ControlButtonConfig,
         onModeToggle: (EdgeSwipeMode) -> Unit,
         // 산봉우리 애니메이션용 (Phase 4.4.6)
         isEdgeCandidate: Boolean = false,
         entryEdge: EntryEdge? = null,
         fingerAlongEdgePx: Float = 0f,
         inwardDistancePx: Float = 0f,
         modifier: Modifier = Modifier
     )
     ```
3. **`EdgeSwipeMode` sealed class 또는 enum** (파일 위치: `EdgeSwipeOverlay.kt` 내):
   ```kotlin
   enum class EdgeSwipeMode {
       SCROLL,   // ScrollMode: OFF ↔ lastScrollMode 토글
       CLICK,    // ClickMode: LEFT_CLICK ↔ RIGHT_CLICK
       MOVE,     // MoveMode: FREE ↔ RIGHT_ANGLE
       CURSOR    // CursorMode: SINGLE ↔ MULTI
   }
   ```
4. **모드 토글 동작**:
   | EdgeSwipeMode | 토글 동작 |
   |--------------|----------|
   | `SCROLL` | `scrollMode == OFF` → `lastScrollMode` 복원 / `scrollMode != OFF` → OFF |
   | `CLICK` | `LEFT_CLICK` ↔ `RIGHT_CLICK` |
   | `MOVE` | `FREE` ↔ `RIGHT_ANGLE` |
   | `CURSOR` | `SINGLE` ↔ `MULTI` (상태만 변경, Phase 4.4 이전 UI 미반응) |
5. **`ControlButtonConfig` 연동**: 팝업에 표시되는 모드 항목은 `config`의 `showXxx` 필드로 필터링
   - `showScrollMode = false` → 스크롤 아이콘 팝업 미표시
   - `showClickMode = false` → 클릭 아이콘 팝업 미표시
   - `showMoveMode = false` → 이동 아이콘 팝업 미표시
   - `showCursorMode = false` → 커서 아이콘 팝업 미표시 (Page 1 기본값)
   - 표시되는 모드 수에 따라 그리드 `FlowRow` 자동 재배치
6. **제스처 인식 — `TouchpadWrapper.kt` 수정**:
   - 기존 `pointerInput` 파이프라인 **앞**에서 엣지 진입 여부를 먼저 판별
   - 엣지 후보 상태(`isEdgeCandidate`), 진입 엣지(`entryEdge: EntryEdge?`), 손가락의 엣지 축 위치(`fingerAlongEdgePx`), 안쪽 이동량(`inwardDistancePx`) 로컬 상태 관리
   ```kotlin
   // DOWN: 시작점이 엣지 영역(24dp 이내) → isEdgeCandidate = true, entryEdge 저장, fingerAlongEdgePx 초기화
   // MOVE (isEdgeCandidate == true, showEdgePopup == false):
   //   ① 안쪽 이동량 >= TRIGGER_DISTANCE_PX → showEdgePopup = true (팝업 등장)
   //   ② 안쪽 이동량 < 0 (엣지 방향으로 되돌아감) → isEdgeCandidate = false (팝업 등장 전 취소)
   //   ③ 진입 방향과 수직 이동이 먼저 TRIGGER_DISTANCE_PX 초과 → isEdgeCandidate = false, 일반 커서 처리
   //   → 위 조건 해당 없으면: fingerAlongEdgePx, inwardDistancePx 업데이트 (산봉우리 추적용)
   // MOVE (showEdgePopup == true):
   //   ① 손가락이 진입 엣지(entryEdge) 방향으로 되돌아가 CANCEL_THRESHOLD_DP 이내 진입 → showEdgePopup = false, isEdgeCandidate = false (팝업 등장 후 취소)
   //      ※ 다른 엣지로 이동해도 팝업 유지 (맞은편 모드 아이콘 선택 중 실수 취소 방지)
   // UP (showEdgePopup == true): 선택된 모드 있으면 onModeToggle 호출, 팝업 닫기
   // UP (showEdgePopup == false): 일반 커서 이동 처리
   ```
   - `showEdgePopup`, `isEdgeCandidate`, `entryEdge`, `fingerAlongEdgePx`, `inwardDistancePx` 상태를 `EdgeSwipeOverlay`에 파라미터로 전달
7. **시각 피드백**:
   - 모드 확정 시: `HapticFeedbackConstants.CONFIRM` + `ToastController.show("스크롤 ON", ToastType.INFO, 1500)`
   - 멀티 커서 확정 시: `ToastController.show("멀티 커서 ON", ToastType.INFO, 1500)` (Phase 4.4 이전 유일한 피드백)
   - 취소 시: 팝업 즉시 닫기, 햅틱/토스트 없음 (fade-out 애니메이션은 Phase 4.4.7에서 추가)
8. **`TouchpadWrapper.kt`에 `EdgeSwipeOverlay` 배치**:
   - `DynamicsPresetPopup`, `DpiAdjustPopup`과 동일하게 `Box` 내 최상단 레이어로 추가
   - `StandardModePage`에서 `config`를 `TouchpadWrapper`에 전달 (현재 `ControlButtonContainer`에 전달하는 것과 동일 방식)

**신규 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/touchpad/EdgeSwipeOverlay.kt`

**수정 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/TouchpadWrapper.kt`
  — 엣지 후보 판별 로직 추가, `EdgeSwipeOverlay` 배치, `config` 파라미터 추가
- `src/android/app/src/main/java/com/bridgeone/app/ui/common/ScrollConstants.kt`
  — `EdgeSwipeConstants` 추가 (또는 신규 `EdgeSwipeConstants.kt` 파일)

**참조 코드**:
- `DynamicsPresetPopup.kt` — 팝업 레이아웃·애니메이션 구조 그대로 재사용
- `ControlButtonContainer.kt` — `ControlButtonConfig` 구조 참조

**실제 구현과 계획의 차이**:
- `EdgeSwipeOverlay` 파라미터에 `visible: Boolean`과 `hoveredMode: EdgeSwipeMode?` 추가 (스펙의 `onModeToggle` 대신 TouchpadWrapper에서 직접 처리)
- 모드 아이콘 가장 가까운 것 계산(`calculateHoveredEdgeMode`)은 TouchpadWrapper 내부에서 수행 후 `hoveredMode`로 전달
- `touchpadWidthPx` / `touchpadHeightPx` 파라미터 제거 (overlay 내부에서 `fillMaxSize` 사용으로 불필요)
- **EdgePopupModeSelector(팝업 모드 선택기) UX 추가**: `EdgePopupMode { SWIPE, DIRECT_TOUCH }` enum 신규. 팝업 열기 전 중간 단계(isModeSelecting)에서 엣지 축 방향 스와이프로 두 모드 중 선택. `EDGE_POPUP_DIRECT_TOUCH` 상수 삭제 → 런타임 제스처로 대체
- **팝업 모드 선택기 제스처**: `navStepPx` (30dp) step 기반 즉시 토글 — dead zone 없이 delta ≥ 0 → SWIPE, delta < 0 → DIRECT_TOUCH. `EDGE_MODE_SELECT_PERP_THRESHOLD_DP` 미사용
- **팝업 모드 선택기 카드 UI**: `ModeSelectCard(title, iconResId, isHighlighted)` — 직접 터치 아이콘(`ic_l_click`), 스와이프 아이콘(`ic_scroll`). 설명(subtitle) 없음
- **팝업 모드 선택기 레이아웃**: `entryEdge` 기준이 아닌 터치패드 폭 기준 (`BoxWithConstraints`의 `maxWidth >= 400.dp` → Row, 미만 → Column)

**검증 (UX — 에뮬레이터/실기기 UI 확인)**:
- [x] 팝업 내 손가락 이동에 따라 가장 가까운 아이콘 선택 강조 확인
- [x] 원하는 아이콘 위에서 손 떼기 → 해당 모드 토글 + 햅틱 + 토스트 확인
- [x] 아무 엣지(24dp 이내)로 스와이프 → 팝업 취소, 모드 변경 없음 확인
- [x] 멀티 커서 토글 시 `CursorMode.MULTI` 상태 변경 + 토스트 확인 (UI 변화 없음)
- [x] 엣지 영역(24dp) 밖에서 시작한 터치는 엣지 스와이프 미발동 확인
- [x] 엣지 영역 안 시작이어도 수직 이동이 먼저 28dp 초과 시 일반 커서 이동 처리 확인
- [x] `ControlButtonConfig`에서 비표시 설정된 모드는 팝업 미표시 확인 (Page 1: 커서 모드 미표시)
- [x] 팝업 표시 모드 수 변화 시 그리드 재배치 확인 (3개, 2개 등)
- [x] 기존 `ControlButtonContainer` 버튼으로 모드 변경 시 팝업 상태와 동기화 확인

---

## Phase 4.4.6: 엣지 스와이프 산봉우리 애니메이션

> **⚠️ Phase 4.4.5 변경사항**:
> - `EdgeSwipeOverlay` 실제 시그니처: `visible`, `pendingState`, `config`, `selectedIndex`, `popupAnchorPx`, `isModeSelecting`, `selectedPopupMode`, `isEdgeCandidate`, `entryEdge`, `fingerAlongEdgePx`, `inwardDistancePx`, `modifier`
> - `EdgePopupMode { SWIPE, DIRECT_TOUCH }` enum 추가 — 팝업 모드 선택 중간 단계 존재
> - 모드 선택 UI: `BoxWithConstraints`로 터치패드 폭 기준 Row/Column 자동 전환 (`maxWidth >= 400.dp`)
> - 산봉우리 Canvas 그리기 시 터치패드 크기는 `BoxWithConstraints` 또는 `onGloballyPositioned`로 직접 측정해서 사용
> - `TouchpadWrapper`에 `config: ControlButtonConfig = ControlButtonConfig()` 파라미터 추가됨
> - `ScrollConstants.kt`에 `EdgeSwipeConstants` object 추가됨 (`EDGE_HIT_WIDTH_DP=24`, `TRIGGER_DISTANCE_DP=28`, `CANCEL_THRESHOLD_DP=12`, `BUMP_APPEAR_THRESHOLD_DP=4`)

**목표**: 손가락이 엣지에서 안쪽으로 이동할 때 산봉우리가 둥글게 솟아오르는 Canvas 애니메이션을 추가합니다.

**개발 기간**: 0.5일

**에뮬레이터 호환성**: 에뮬레이터에서 애니메이션 확인 가능. 실기기에서 자연스러움 검증 필요.

**전제 조건**: Phase 4.4.5 완료 (기본 제스처 인식 및 팝업 기능 동작 중)

**세부 목표**:
1. **산봉우리 Canvas 애니메이션 추가** (`EdgeSwipeOverlay.kt` 수정):
   - **등장 조건**: `isEdgeCandidate == true` AND `inwardDistancePx >= BUMP_APPEAR_THRESHOLD_PX` (4dp 변환값)
   - **모양**: `Canvas`에 `Path.cubicTo()`로 **둥근 산봉우리** 형태 그리기 (피크가 뾰족하지 않고 둥글게 솟아오르는 형태)
     ```
     엣지 기저부 상단 (fingerAlongEdgePx - baseHalfSize)
           ↓ cubicTo (제어점: 엣지에서 안쪽으로 향하되, 피크에서 둥글게 꺾이도록 배치)
     피크 포인트 (둥근 꼭대기 — inwardDistancePx에 비례하되 MAX_PEAK_HEIGHT_PX 이하로 제한)
           ↓ cubicTo (제어점: 피크에서 둥글게 꺾여 엣지로 내려오도록 배치)
     엣지 기저부 하단 (fingerAlongEdgePx + baseHalfSize)
     ```
   - **피크 높이 상한**: `MAX_PEAK_HEIGHT_DP` 상수 추가 (`EdgeSwipeConstants`에 정의). 손가락이 아무리 멀리 이동해도 산봉우리 피크는 이 값을 초과하지 않음 (`coerceAtMost(MAX_PEAK_HEIGHT_PX)`)
     - **제약**: `MAX_PEAK_HEIGHT_DP >= TRIGGER_DISTANCE_DP(28dp)` 이상으로 설정. 트리거 전에 피크가 멈추는 구간이 없도록 하여 시각 피드백이 끊기지 않음
   - **즉시 추적 (Spring 없음)**: 기저부 중심(`fingerAlongEdgePx`)과 피크 높이(`inwardDistancePx`) 모두 손가락 위치를 spring 없이 즉시 반영. 별도의 `Animatable`/spring 불필요 — 터치 이벤트에서 받은 raw 값을 그대로 Canvas에 전달
   - **손 떼기 시 수축 애니메이션**: 트리거 거리 도달 전에 손을 떼면 `Animatable.animateTo(0f, spring(...))` 한 번만 재생 → 산봉우리가 엣지 안으로 쏙 빨려 들어가는 효과. 드래그 중에는 spring을 사용하지 않고 수축 시에만 사용
   - **드래그 중 엣지 쪽으로 되돌아오기**: 손을 떼지 않고 `inwardDistancePx`가 0 방향으로 감소하면 산봉우리가 raw 값 그대로 줄어듦. `coerceAtLeast(0f)`로 음수 방지만 처리하고 별도 spring 불필요
   - **산봉우리 렌더링 — 반투명 fill + 테두리 분리**:
     - **Fill (안쪽)**: 동일 Path를 `Paint(style = Fill)`로 먼저 그리되 `color.copy(alpha = 0.2f)` 수준의 반투명 색상 적용. 터치패드 UI가 뒤에 비쳐 보여 덜 방해됨
     - **Stroke (테두리)**: 동일 Path를 `Paint(style = Stroke)`로 덧그리되 터치패드 테두리와 동일한 `Brush.linearGradient`를 공유. 산봉우리 윤곽을 선명하게 잡아줌
     - **그라데이션 좌표계**: 터치패드 전체 크기 기준 `Brush.linearGradient(colors, start=Offset(0,0), end=Offset(width,height))` — 산봉우리 위치에 관계없이 자동으로 해당 위치의 그라데이션 색상 적용
     - 단색 모드일 때는 `listOf(color, color)`로 동일하게 처리 (분기 불필요)
   - **피크 Glow 효과**: 반투명 fill 덕분에 피크의 glow가 어두운 터치패드 배경 위에서 직접 발광하여 잘 보임
     - 피크 끝점 근처에 `Paint(blurMaskFilter = BlurMaskFilter(radius, NORMAL))`로 발광 레이어 추가
     - **MAX 도달 강조**: `inwardDistancePx >= MAX_PEAK_HEIGHT_PX`이면 glow 반경을 더 크게 키워 "준비 완료" 상태를 시각적으로 강조
   - **팝업 전환**: `inwardDistancePx >= TRIGGER_DISTANCE_PX` 도달 시 즉시 팝업 표시. 팝업이 열린 동안에도 **산봉우리는 계속 렌더링되며 `inwardDistancePx`를 그대로 추적**함 — 산봉우리를 수축하지 않으므로 팝업과 산봉우리가 동시에 보임. 이 상태에서 손가락을 엣지 쪽으로 되돌리면(`inwardDistancePx < TRIGGER_DISTANCE_PX`) 산봉우리가 줄어들면서 팝업이 닫힘 — 손가락을 떼지 않고 되집어 넣는 동작으로 직관적으로 취소 가능
   - **트리거 시 햅틱 피드백**: 팝업이 열리는 순간 `LocalHapticFeedback.current.performHapticFeedback(HapticFeedbackType.LongPress)` 1회 호출. 팝업 닫힘(취소) 시에는 진동 없음
   - **참조**: [Wolt Overscroll](https://careers.wolt.com/en/blog/tech/jetpack-compose-custom-overscroll-effect) (bezier bump + snapTo/animateTo 패턴), [Flutter Fluid Tab](https://github.com/amalChandran/flutter-fluid-tab) (cubicTo 2개로 hill 구성)

**수정 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/touchpad/EdgeSwipeOverlay.kt`
  — 산봉우리 Canvas 렌더링 추가
- `src/android/app/src/main/java/com/bridgeone/app/ui/common/ScrollConstants.kt`
  — `EdgeSwipeConstants`에 `MAX_PEAK_HEIGHT_DP` 상수 추가

**참조 코드**:
- [Wolt - Custom Overscroll Effect](https://careers.wolt.com/en/blog/tech/jetpack-compose-custom-overscroll-effect) — 엣지에서 bezier curve로 curved bump 그리기 + onPull 즉시 추적/onRelease snap back 패턴 (가장 근접한 레퍼런스)
- [Android 공식 - Advanced Animation: Gestures](https://developer.android.com/develop/ui/compose/animation/advanced) — `Animatable.snapTo()`(드래그 즉시 추적) + `animateTo(0f, spring())`(릴리즈 수축) 공식 패턴
- [Flutter Fluid Tab](https://github.com/amalChandran/flutter-fluid-tab) — cubicTo 2개로 엣지에 둥근 hill 형태 구성하는 구조 (Flutter이지만 bezier control point 배치 참고)
- [Cubic Bezier Curves in Compose](https://medium.com/design-bootcamp/cubic-bezier-curves-in-compose-b8f93a3ce4b4) — Compose Canvas에서 `cubicTo()` control point가 curve 형태를 어떻게 결정하는지 실전 예제

**검증 (UX — 에뮬레이터/실기기 UI 확인)**:
- [x] 왼쪽/오른쪽/하단 엣지에서 4dp 이상 안쪽으로 이동 시 산봉우리 등장 확인 (4dp 미만에서는 미등장)
- [x] 손가락이 엣지를 따라 이동 시 산봉우리 기저부가 지연 없이 즉시 따라오는지 확인
- [x] 안쪽 이동에 비례해 피크가 **둥글게** 솟아오르는 유기적 bezier 곡선 확인 (뾰족한 teardrop이 아닌 둥근 산봉우리 형태)
- [x] 피크 높이가 `MAX_PEAK_HEIGHT_DP` 이상으로 늘어나지 않는 상한 제한 확인
- [x] 산봉우리 안쪽이 반투명 fill로 채워지고 윤곽은 테두리 Stroke으로 구분되는지 확인 (터치패드 UI가 뒤에 비쳐 보여야 함)
- [x] 피크 끝에 glow 발광 효과 확인, MAX 도달 시 glow가 더 커지며 "준비 완료" 상태 강조 확인
- [x] 트리거 거리 도달 전 손을 떼면 산봉우리가 엣지 안으로 쏙 수축되는 spring 애니메이션 확인
- [x] 트리거 거리 도달 시 산봉우리 수축 없이 **즉시 팝업으로 전환**되는 확인
- [x] 팝업 열림 순간 진동 1회 발생, 취소 시에는 진동 없음 확인
- [x] 팝업 표시 중 엣지 쪽으로 되돌아가면 팝업이 닫히고 산봉우리가 다시 등장하는 취소 흐름 확인

---

## Phase 4.4.7: 엣지 스와이프 팝업 등장/소멸 애니메이션

> **⚠️ Phase 4.4.6 변경사항**:
> - 산봉우리 렌더링은 `EdgeSwipeOverlay` 내부가 아닌 별도 `EdgeBumpOverlay` Composable로 분리 구현됨
> - `EdgeSwipeConstants`에 산봉우리 관련 상수 7개 추가: `MAX_PEAK_HEIGHT_DP(36)`, `BUMP_BASE_HALF_SIZE_DP(40)`, `BUMP_STROKE_WIDTH_DP(2)`, `BUMP_GLOW_RADIUS_DP(8)`, `BUMP_GLOW_MAX_RADIUS_DP(16)`, `BUMP_SHRINK_SPRING_STIFFNESS(800)`, `BUMP_SHRINK_SPRING_DAMPING(0.7)`
> - `TouchpadWrapper`에서 산봉우리 위치는 plain state (`lastBumpInwardPx`, `lastBumpAlongPx`, `lastBumpEntryEdge`) + 수축 전용 `bumpShrinkAnimatable` (Animatable 1개)로 관리. 드래그 중에는 raw 값 직접 전달(Animatable 불사용), 릴리즈/모드선택 시에만 spring 수축
> - `lastBump*` 값은 entry edge 기준 상대 이동량이 아닌, **현재 손가락에서 가장 가까운 엣지(nearest edge) 기준 절대 거리**. `findNearestEdge()` private 함수가 TouchpadWrapper 하단에 추가됨
> - 트리거 시 `view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)` 1회 호출 추가됨
> - **모드 선택 직후** (스와이프/직접터치 중 하나 선택 후 손 뗌) 팝업 등장과 **동시에** 산봉우리 spring 수축 애니메이션이 재생됨 → 팝업 fade-in 애니메이션(Phase 4.4.7)과 타이밍이 겹치므로 시각적 조화 고려 필요

**목표**: Phase 4.4.5에서 즉시 표시/닫기로 구현된 엣지 스와이프 팝업에 부드러운 등장/소멸 전환 효과를 추가합니다.

**개발 기간**: 0.5일

**전제 조건**: Phase 4.4.6 완료 (산봉우리 애니메이션 동작 중)

**세부 목표**:
1. **팝업 등장 애니메이션 추가** (`EdgeSwipeOverlay.kt` 수정):
   - 배경 fade-in: alpha 0 → 0.6, 200ms
   - 아이콘 그리드 stagger slide-up 등장: 아이콘마다 30ms 간격
2. **팝업 소멸 애니메이션 추가** (`EdgeSwipeOverlay.kt` 수정):
   - 취소/확정 시: 팝업 전체 fade-out (즉시 닫기 → 부드러운 소멸로 교체)

**수정 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/touchpad/EdgeSwipeOverlay.kt`
  — 팝업 fade-in/out, 아이콘 stagger 애니메이션 추가

**참조 코드**:
- `DynamicsPresetPopup.kt` — 팝업 fade-in/out 애니메이션 구조 참조

**검증 (UX — 에뮬레이터/실기기 UI 확인)**:
- [ ] 팝업 배경 fade-in (200ms) 확인
- [ ] 아이콘 stagger slide-up 등장 (30ms 간격) 확인
- [ ] 취소/확정 시 팝업 fade-out 확인

---

## Phase 4.4.8: 모드 프리셋 버튼

**목표**: 여러 개의 모드 구성 스냅샷(프리셋)을 저장해두고 버튼 하나로 즉시 전환하는 `ModePresetButton`을 구현합니다. 탭으로 순환, 롱프레스로 팝업 선택 방식이며, `DynamicsPresetButton` (Phase 4.4.1)과 동일한 패턴을 재사용합니다.

**쉬운 설명**: 포토샵 같은 작업에서 "정밀하게 움직이는 마우스 모드"와 "빠르게 이동하는 마우스 모드"를 반복적으로 오갈 때, 지금은 DPI 버튼, 이동 모드 버튼 등을 하나씩 탭해야 합니다. 이 Phase에서는 여러 설정을 묶은 "프리셋"을 미리 만들어두고, 터치패드 오른쪽 아래 버튼 한 번으로 전부 한꺼번에 바꿀 수 있게 합니다.

**개발 기간**: 1-1.5일

**세부 목표**:
1. **`ModePreset` 데이터 모델 추가** (`TouchpadMode.kt`):
   ```kotlin
   data class ModePreset(
       val name: String,
       val icon: AppIconDef,
       val description: String,
       val padModeState: PadModeState,
       val dynamicsPresetIndex: Int = DEFAULT_PRESET_INDEX
   )
   ```
2. **`ModePresetConstants.kt` 신규 파일** (기본 프리셋 정의):
   ```kotlin
   val MODE_PRESETS: List<ModePreset> = listOf(
       ModePreset(
           name = "Standard",
           icon = AppIcons.ModePresetStandard,
           description = "기본 설정",
           padModeState = PadModeState(),
           dynamicsPresetIndex = 0  // Off
       ),
       ModePreset(
           name = "Precise",
           icon = AppIcons.ModePresetPrecise,
           description = "저DPI + 직각 이동 — 정밀 작업용",
           padModeState = PadModeState(
               dpi = DpiLevel.LOW,
               moveMode = MoveMode.ORTHOGONAL,
               clickMode = ClickMode.LEFT_CLICK,
               scrollMode = ScrollMode.NONE
           ),
           dynamicsPresetIndex = 1  // Precision
       ),
       ModePreset(
           name = "Fast",
           icon = AppIcons.ModePresetFast,
           description = "고DPI + 자유 이동 — 빠른 탐색용",
           padModeState = PadModeState(
               dpi = DpiLevel.HIGH,
               moveMode = MoveMode.FREE,
               clickMode = ClickMode.LEFT_CLICK,
               scrollMode = ScrollMode.NONE
           ),
           dynamicsPresetIndex = 3  // Fast
       ),
   )
   val DEFAULT_MODE_PRESET_INDEX = 0
   ```
3. **`TouchpadState`에 `modePresetIndex: Int` 필드 추가** (기본값: `DEFAULT_MODE_PRESET_INDEX`)
4. **`AppIcons`에 모드 프리셋 아이콘 추가** (`AppIcons.kt`):
   - `ModePresetStandard`, `ModePresetPrecise`, `ModePresetFast`
   - 대응 VectorDrawable: `ic_mode_standard.xml`, `ic_mode_precise.xml`, `ic_mode_fast.xml`
5. **`ModePresetButton.kt` 신규** — `DynamicsPresetButton.kt`와 동일 구조:
   - **위치**: `Alignment.BottomEnd`, `padding(end = 8.dp, bottom = 8.dp)`
   - **크기**: 40dp × 40dp
   - **표시 조건**: Standard 모드에서 **항상 표시** (스크롤 모드에서도 숨기지 않음)
     - `DynamicsPresetButton`은 스크롤 모드에서 숨겨지지만, `ModePresetButton`은 `AnimatedVisibility` 조건 없이 항상 표시
   - **탭**: `modePresetIndex = (modePresetIndex + 1) % MODE_PRESETS.size` 순환
   - **롱프레스**: `showModePresetPopup = true`
   - **이벤트 소비**: `down.changes.forEach { it.consume() }`
6. **`ModePresetPopup.kt` 신규** — `DynamicsPresetPopup.kt`와 동일한 2단계 구조(그리드 선택 → 확인 단계):
   - 블러 상태 통합: `blurRadius = if (dpiPopup || dynamicsPopup || modePresetPopup) 8.dp else 0.dp`
   - 프리셋 확정 시 적용 순서:
     1. 현재 활성 패드의 `PadModeState` 교체 (클릭/이동/스크롤/DPI 모두)
     2. `TouchpadState.dynamicsPresetIndex` 전역 업데이트
     3. `ControlButtonContainer` 버튼 상태 즉시 갱신
7. **`TouchpadWrapper.kt` 수정**: `ModePresetButton` 오버레이 추가 (`DynamicsPresetButton`과 동일한 방식으로 `Box` 내부에 배치)

**신규 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/common/ModePresetConstants.kt`
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/touchpad/ModePresetButton.kt`
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/touchpad/ModePresetPopup.kt`

**수정 파일**:
- `TouchpadMode.kt` (`ModePreset` data class 추가, `TouchpadState`에 `modePresetIndex` 추가)
- `AppIcons.kt` (모드 프리셋 아이콘 3종 추가)
- `TouchpadWrapper.kt` (`ModePresetButton` 오버레이 추가, 블러 상태 통합)

**참조 문서**:
- `docs/android/component-touchpad.md` §1.7
- `docs/android/technical-specification-app.md` §2.2.7
- `DynamicsPresetButton.kt`, `DynamicsPresetPopup.kt` (Phase 4.4.1에서 구현한 동일 패턴)

**검증 (UX — 에뮬레이터/실기기 UI 확인)**:
- [ ] `ModePresetButton` — 터치패드 우측 하단에 항상 표시 확인 (스크롤 모드에서도)
- [ ] `ModePresetButton` — 원탭으로 프리셋 순환 전환 확인
- [ ] `ModePresetButton` — 탭 시 프리셋 이름 라벨 표시 후 자동 소멸 확인
- [ ] `ModePresetButton` — 롱프레스 시 팝업 오버레이 표시 확인
- [ ] 팝업 — 그리드 선택 + 확인 단계 동작 확인 (`DynamicsPresetPopup`과 동일)
- [ ] 프리셋 적용 시 `ControlButtonContainer` 버튼 상태 즉시 반영 확인 (DPI, 클릭, 이동, 스크롤)
- [ ] 프리셋 적용 시 다이나믹스 전역 업데이트 확인
- [ ] 탭 시 터치패드 커서 이동 미발생 (이벤트 소비 확인)
- [ ] 멀티 커서 모드: 프리셋 적용이 활성 패드에만 적용되고 다른 패드는 유지 확인

---

## Phase 4.4.9: 터치패드 E2E 하드웨어 테스트

**목표**: Phase 4.3에서 구현한 터치패드 기능 전체를 실기기 + 실제 PC 연결 환경에서 하나씩 검증하여 하드웨어 수준의 이상 없음을 확인

**개발 기간**: 0.5-1일

**쉬운 설명**: 지금까지 만든 터치패드의 모든 기능을 실제 스마트폰과 PC를 연결한 상태에서 하나씩 눌러보고 움직여봄으로써, 에뮬레이터에서는 알 수 없었던 실제 하드웨어 동작 문제를 직접 눈으로 확인하는 최종 점검 단계입니다.

**테스트 환경 요건**:
- Android 실기기 (USB-OTG 지원) + ESP32-S3 동글 연결
- PC에 ESP32-S3 USB HID 연결 확인
- Standard 모드 활성

### 검증 항목 체크리스트

#### 포인터 다이나믹스 (Phase 4.4.1에서 이관)
- [ ] None 프리셋: 속도와 무관하게 일정한 커서 이동 (기존과 동일)
- [ ] Precision 프리셋 (Windows EPP): 느린 드래그 시 정밀 이동, 빠른 드래그 시 커서 가속 확인
- [ ] Standard 프리셋 (Windows EPP): Precision보다 강한 가속 적용 확인
- [ ] Fast 프리셋 (Linear): 속도 증가에 비례하여 커서 이동량 선형 증가 확인
- [ ] DPI 배율과 다이나믹스 배율 중첩 적용 정상 동작 (Low DPI + 가속 프리셋 조합)
- [ ] 최대 배율 상한 초과 방지 (`coerceIn` 확인)
- [ ] 스크롤 모드 중 다이나믹스 배율 미적용 확인 (커서 이동 모드 전용)

> (이후 Phase 완료 시 추가 항목 계속 추가)

---

## Phase 4.4 완료 후 터치패드 기능 매트릭스

| 기능 | Essential | Standard |
|------|----------|----------|
| 커서 이동 (자유) | ✅ | ✅ |
| 좌클릭 (탭) | ✅ | ✅ |
| 우클릭 | ❌ (좌클릭으로 강제) | ✅ (ClickModeButton) |
| 직각 이동 | ❌ | ✅ (MoveModeButton) |
| 일반 스크롤 | ❌ | ✅ (ScrollModeButton) |
| 무한 스크롤 | ❌ | ✅ (ScrollModeButton) |
| DPI 조절 | ❌ | ✅ (DPIControlButton) |
| 포인터 다이나믹스 | ❌ | ✅ (프리셋 버튼: 원탭 사이클 / 롱프레스 팝업 선택) |
| 모드 프리셋 | ❌ | ✅ (ModePresetButton: 원탭 사이클 / 롱프레스 팝업 선택) |
| 스크롤 감도 | ❌ | ✅ (ScrollSensitivityButton) |
| 멀티 커서 | ❌ | ⏳ Phase 4.5 (Page 2 풀 와이드 터치패드, CursorModeButton 활성화) |
| ControlButtonContainer | ❌ 숨김 | ✅ 표시 |
| 스크롤 가이드라인 | ❌ | ✅ (스크롤 모드 시) |
| 모드별 테두리 색상 | ❌ | ✅ (모드 조합에 따른 단색/그라데이션) |
| 버튼 구성 설정 (인스턴스별 비표시) | N/A | ⏳ Phase 4.4.4 |
| 엣지 스와이프 | ❌ | ⏳ Phase 4.4.5 |
