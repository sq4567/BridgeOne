---
title: "BridgeOne Phase 4.11: 버튼 컴포넌트 고급 기능"
description: "BridgeOne 프로젝트 Phase 4.11 - KeyboardKeyButton Sticky Hold, ContainerButton, Essential 모드 UI 재정비"
tags: ["android", "button", "sticky-hold", "container-button", "essential", "ui"]
version: "v1.0"
owner: "Chatterbones"
updated: "2026-03-26"
---

# BridgeOne Phase 4.11: 버튼 컴포넌트 고급 기능

**개발 기간**: 2-3일

**목표**: 버튼 컴포넌트의 고급 기능을 구현하여, ContainerButton, Essential 모드 UI 재정비 등 스타일프레임에 정의된 미구현 컴포넌트 기능을 완성합니다.

**핵심 성과물**:
- KeyboardKeyButton Sticky Hold
- ContainerButton (하위 버튼 그룹화, 팝업 오버레이, 일반/지속 모드)
- Essential 모드 EssentialModePage 재정비 (스타일프레임 완전 준수)

**선행 조건**: Phase 4.9 (Page 4 키보드) 완료

**에뮬레이터 호환성**: ContainerButton 팝업 오버레이, Essential 모드 UI 재정비 전체 에뮬레이터에서 개발 가능.

---

## Phase 4.11.1: KeyboardKeyButton Sticky Hold

**개발 기간**: 0.5-1일

**목표**: KeyboardKeyButton의 Sticky Hold 기능을 구현하여, 사용자가 클릭하고 있을 때 시각적 피드백을 명확히 제공

**구현 항목**:
1. 500ms 롱프레스 Sticky Hold 감지 (LaunchedEffect 기반 타이머)
2. Fill 애니메이션 (좌→우, `drawWithContent` + `stickyHoldProgress`)
   - 500ms 동안 버튼 너비에 걸쳐 색상 채움 애니메이션
   - Sticky Hold 완성 시각적 피드백의 핵심 요소
3. 오렌지 테두리 (`#FF9800`, 2dp border)
4. 색상 전환: 기본 `#2196F3` → 누름 `#1976D2` → Latched `#1565C0`
5. `stickyActivatedDuringPress` 플래그로 손 뗌 시 해제 방지
6. 재탭 시 `onKeyReleased` → `isStickyLatched = false`

**별도 처리 항목** (Phase 4.8.3에서 함께 추가):
- `stickyHoldEnabled` 파라미터 (현재는 항상 활성)
- 햅틱 피드백 (Hold 진입/해제 시)

**검증**:
- [ ] 500ms 동안 버튼 좌→우 Fill 애니메이션 실행
- [ ] Hold 완료 후 오렌지 테두리 유지
- [ ] 재탭 시 Sticky Hold 해제

---

## Phase 4.11.2: ContainerButton 컴포넌트

**목표**: 하위 버튼을 그룹화하여 팝업 오버레이로 표시하는 ContainerButton

**개발 기간**: 1.5일

**세부 목표**:
1. ContainerButton 기본 구조:
   - 탭: 팝업 오버레이 표시 (일반 모드)
     - 하위 버튼 탭 → 기능 실행 후 팝업 자동 닫힘
     - 팝업 외부 탭 / Back 키 → 팝업 닫힘
   - 롱프레스: 팝업 오버레이 표시 (지속 모드)
     - 하위 버튼 탭 → 기능 실행 후에도 팝업 유지
     - 팝업 외부 터치 / Back 키로만 닫힘
2. 팝업 오버레이 스타일:
   - 어두운 스크림 배경 (반투명 검정)
   - 둥근 사각형 컨테이너 박스
   - 내부: 하위 버튼 그리드 배치
3. 시각적 피드백:
   - 기본: `#2196F3`
   - 팝업 열림 중: `#1976D2`
   - Disabled: `#C2C2C2` (alpha 0.6)
   - 롱프레스 Fill 애니메이션 (좌→우)
   - 햅틱: Light 1회 (팝업 열기/닫기, 하위 버튼 선택)
4. 디바운스: 150ms 내 재탭 무시
5. Disabled 시: 팝업 열려있으면 즉시 닫힘
6. 접근성:
   - 키보드 네비게이션 (하위 버튼 간 이동)
   - `contentDescription` 팝업 모드 구분 포함

**신규 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/components/ContainerButton.kt`

**참조 문서**:
- `docs/android/component-design-guide-app.md` §2.3.4 (ContainerButton)
- `docs/android/technical-specification-app.md` §2.3.2.4 (설계 요구사항)

**활용 위치**:
- Essential 모드: F1-F12 컨테이너 버튼 (기존 `FKeyPopupDialog` 대체)
- Standard 모드: 추후 그룹화 필요한 버튼 영역

**검증**:
- [ ] 탭 → 팝업 표시, 하위 버튼 선택 후 자동 닫힘
- [ ] 롱프레스 → 지속 모드 팝업, 하위 선택 후 유지
- [ ] 외부 터치 / Back 키로 팝업 닫힘
- [ ] 스크림 배경 렌더링
- [ ] Disabled 시 팝업 즉시 닫힘

---

## Phase 4.11.3: Essential 모드 UI 재정비

**목표**: Essential 모드 페이지를 스타일프레임에 완전히 준수하도록 재정비

**개발 기간**: 1일

**세부 목표**:
1. `EssentialModePage.kt` 재정비:
   - 기존 구현과 `styleframe-essential.md` 비교 → 차이점 수정
   - 페이지 인디케이터/탭/스와이프 완전 비활성 확인
   - 상태 안내: 상단 토스트로만 제공
2. Essential 진입 토스트 (`ToastController` 사용):
   ```kotlin
   ToastController.show("PC: Essential 모드", ToastType.INFO, 3000L)
   delay(300L)
   ToastController.show("Essential 페이지 표시", ToastType.INFO, 3000L)
   ```
   - 두 토스트 순차 표시, 사이 간격 300ms
3. Boot Keyboard Cluster 재정비:
   - `FKeyPopupDialog` → Phase 4.11.2의 `ContainerButton`으로 교체
   - Del 키: 최상단 독립 배치 (BIOS 진입용 강조)
   - F1-F12: ContainerButton으로 그룹화 (3×4 그리드 팝업)
   - Esc/Enter: 한 줄 배치
   - DPad: 3×3 중앙 비움
4. 터치패드 간이 규칙 확인:
   - `ControlButtonContainer` 숨김 확인
   - 모드 고정: MOVE, FREE, LEFT, SINGLE
   - 더블탭/롱프레스/스크롤/우클릭: 비활성 확인
   - 무효 입력 시: `ToastController.show("고급 기능은 Windows 서버 연결 시 사용 가능합니다", ToastType.INFO, 2000L)`
5. 반응형:
   - 폭 < 360dp: 우측 패널 아이콘형 단일 열
   - 폭 ≥ 600dp: 터치패드 확대, 키 클러스터 2열

**수정 파일**:
- `src/android/app/src/main/java/com/bridgeone/app/ui/pages/EssentialModePage.kt`

**참조 문서**:
- `docs/android/styleframe-essential.md` (전체)
- `docs/android/design-guide-app.md` §8.6 (Essential 유저 플로우)

> **⚠️ Phase 4.1.7 변경사항**: Safe Zone은 `BridgeOneApp.kt`의 `AppState.Active` 박스에서 이미 적용됨(`TOP/BOTTOM_SAFE_ZONE = 40.dp`) → `EssentialModePage` 자체에 safe zone 패딩 추가 불필요. 기존 프로토타입 코드에 safe zone 관련 패딩이 있다면 중복 적용 방지를 위해 제거. DPad 크기·버튼 간격 등 새 레이아웃 상수 추가 시 `ui/common/LayoutConstants.kt`에 정의. BridgeOne 별 로고가 필요하면 `ui/common/BridgeOneLogo.kt` 재사용.

> **⚠️ Phase 4.1.8 변경사항**: 커스텀 토스트 시스템 도입. `android.widget.Toast` 사용 금지. 모든 알림은 `ToastController.show(message, ToastType, durationMs)` 로 표시. 타입: `INFO`(파란색) · `SUCCESS`(초록색) · `WARNING`(주황색, 검은 텍스트) · `ERROR`(빨간색). 무제한 표시: `TOAST_DURATION_INFINITE`.

> **⚠️ StatusToast 개선 사항 (Phase 4.2 이후 반영)**: `StatusToast.kt` 대폭 업데이트 — 호출 측 API(`ToastController.show`) 변경 없음.
> 1. **타이머 테두리**: 유한 `durationMs` 토스트는 자동으로 남은 시간 비례 shrinking border 표시 — 호출 측 코드 변경 불필요.
> 2. **다중 토스트 스태킹**: 기존 토스트 표시 중 새 토스트 표시 시 → 새 토스트가 위에서 아래로 슬라이드인(350ms) → 완료 후 기존 토스트가 위로 슬라이드아웃(300ms). 두 토스트가 잠시 동시에 표시됨.
> 3. **중복 제거**: `ToastMessage.equals()`가 message·type·durationMs 내용 기준(내부 id 제외). 동일 내용의 `show()` 연속 호출 → StateFlow 충돌 방지 → 단일 토스트만 표시.
> 4. **Essential 진입 두 토스트 동작**: 아래 순차 코드는 두 토스트 내용이 다르므로 중복 제거에 해당 없음. 300ms 간격으로 show() 두 번 → 첫 토스트 슬라이드인 중에 두 번째 토스트 등장 → 첫 토스트 자동 위로 퇴장. 계획대로 동작함.

**검증**:
- [ ] F1-F12 ContainerButton 팝업 정상 동작
- [ ] Del 키 최상단 강조 배치
- [ ] Essential 진입 시 순차 토스트 2개
- [ ] 무효 입력 시 안내 토스트
- [ ] ControlButtonContainer 완전 숨김
- [ ] 페이지 네비게이션 완전 비활성

---

## Phase 4.11 완료 후 컴포넌트 기능 매트릭스

| 컴포넌트 | 기본 기능 | 고급 기능 | 구현 Phase |
|----------|----------|----------|-----------|
| KeyboardKeyButton | ✅ 탭 KeyDown/KeyUp | ⏳ Sticky Hold (4.8.1), Key Repeat (4.6.3) | 4.8.1 + 4.6.3 |
| ShortcutButton | ✅ 키 조합 순차 전송 | ✅ 디바운스, Alt+Tab 홀드 | 4.2.4 |
| ContainerButton | — | ✅ 일반/지속 팝업 모드 | 4.8.2 |
| MacroButton | — | ⏳ Phase 5+ (Disabled placeholder) | 4.2.5 (placeholder) |
| DPad | ✅ 4방향+대각선 탭 | ✅ Sticky Hold, 드래그 전환 | 4.7.1 + 4.7.2 |
| ModifiersBar | — | ✅ 3단계 Sticky (탭/더블탭/롱프레스) | 4.6.1 |
| MediaControlButton | — | ✅ Play/Pause 토글, Stop | 4.6.4 |
| LockKeyButton | — | ✅ HID LED 동기화 | 4.6.4 |
