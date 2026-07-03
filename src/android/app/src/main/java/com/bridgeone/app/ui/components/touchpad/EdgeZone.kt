package com.bridgeone.app.ui.components.touchpad

import androidx.compose.ui.graphics.Color
import com.bridgeone.app.ui.common.DYNAMICS_PRESETS
import com.bridgeone.app.ui.common.MACRO_STEP_DELAY_DEFAULT_MS
import com.bridgeone.app.ui.common.MODE_PRESETS

/** 존 색상 팔레트. 편집기 미리보기와 런타임 오버레이가 공용 사용 — 수정 시 양쪽에 동시 반영됨. */
val ZONE_COLORS = listOf(
    Color(0xFF1E3A5F), Color(0xFF3A1E5F), Color(0xFF1E5F3A),
    Color(0xFF5F3A1E), Color(0xFF3A5F1E), Color(0xFF1E5F5F)
)

/** 인덱스에 맞는 존 색상 반환 (팔레트 순환). */
fun zoneColor(index: Int): Color = ZONE_COLORS[index % ZONE_COLORS.size]

/**
 * 액션 카테고리에 따른 기본 존 색상. 동일 기능 계열(클릭/이동/스크롤/DPI 등)을 같은 색으로 묶어
 * 편집기에서 colorHex를 지정하지 않아도 직관적으로 구별 가능하게 한다.
 *
 * - colorHex가 설정된 경우: 오버레이에서 이 함수 대신 colorHex 우선 적용
 * - Unassigned: 거의 보이지 않는 어두운 색 (블록 alpha 적용 후 무채색)
 */
fun EdgeZoneAction.categoryColor(): Color = when (this) {
    is EdgeZoneAction.SetClickMode,
    is EdgeZoneAction.MouseHoldToggle -> Color(0xFF8B1A1A)           // 클릭/마우스 홀드 → 딥 레드
    is EdgeZoneAction.SetMoveMode -> Color(0xFFB84A00)               // 이동 모드 → 딥 오렌지
    is EdgeZoneAction.SetScrollMode,
    is EdgeZoneAction.SwapScrollMode,
    is EdgeZoneAction.SetScrollSpeed,
    is EdgeZoneAction.SetCustomScrollSpeed -> Color(0xFF1A5C1A)      // 스크롤 계열 → 딥 그린
    is EdgeZoneAction.SetDpi,
    is EdgeZoneAction.SetCustomDpi -> Color(0xFF0A3D6B)              // DPI 계열 → 딥 블루
    is EdgeZoneAction.CyclePreset,
    is EdgeZoneAction.SetModePreset,
    is EdgeZoneAction.SetDynamicsPreset -> Color(0xFF3A1A5C)         // 프리셋 계열 → 딥 퍼플
    is EdgeZoneAction.CyclePage,
    is EdgeZoneAction.JumpToPage -> Color(0xFF1A2E6B)                // 페이지 이동 → 인디고
    is EdgeZoneAction.RestorePreviousMode -> Color(0xFF2A3540)       // 이전 설정 복원 → 블루그레이
    is EdgeZoneAction.SendShortcut -> Color(0xFF1A3A5C)             // 단축키 → 딥 네이비
    is EdgeZoneAction.SendMacro -> Color(0xFF2A1A5C)                // 매크로 → 딥 다크퍼플
    is EdgeZoneAction.ToggleMultiCursor,
    is EdgeZoneAction.ToggleMultiCursorLayout,
    is EdgeZoneAction.SetCursorCount,
    is EdgeZoneAction.ActivatePad,
    is EdgeZoneAction.CyclePad -> Color(0xFF1A5C5C)                  // 멀티 커서 계열 → 딥 시안
    is EdgeZoneAction.ToggleMode -> when (this.mode) {
        EdgeSwipeMode.CLICK -> Color(0xFF8B1A1A)                     // 클릭 토글 → 딥 레드
        EdgeSwipeMode.MOVE -> Color(0xFFB84A00)                      // 이동 토글 → 딥 오렌지
        EdgeSwipeMode.SCROLL -> Color(0xFF1A5C1A)                    // 스크롤 토글 → 딥 그린
        EdgeSwipeMode.DPI -> Color(0xFF0A3D6B)                       // DPI 토글 → 딥 블루
        EdgeSwipeMode.DYNAMICS -> Color(0xFF3A1A5C)                  // 다이나믹스 토글 → 딥 퍼플
        EdgeSwipeMode.SCROLL_SPEED -> Color(0xFF1A5C4A)              // 스크롤 속도 토글 → 딥 틸
        EdgeSwipeMode.CURSOR -> Color(0xFF1A3A5C)                    // 커서 토글 → 딥 네이비
    }
    is EdgeZoneAction.OpenSettings -> when (this.settingsType) {
        SettingsType.DPI -> Color(0xFF0A3D6B)                        // DPI 설정 → 딥 블루
        SettingsType.SCROLL_SPEED -> Color(0xFF1A5C4A)               // 스크롤 속도 설정 → 딥 틸
    }
    is EdgeZoneAction.Unassigned -> Color(0xFF1A1A1A)                // 미할당 → 거의 투명 (alpha 적용 후 무채색)
}

// ============================================================
// 존 액션 보조 열거형
// ============================================================

enum class PresetType { DYNAMICS, MODE }
enum class SettingsType { DPI, SCROLL_SPEED }
enum class MouseButton { LEFT, RIGHT, MIDDLE }
enum class MouseHoldMode { HOLD, RELEASE, TOGGLE }
enum class PageNav { NEXT, PREV }

/** 두 엣지 스트립이 겹치는 코너 위치. */
enum class CornerOverlap { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

/** [corner] 의 기본 우선 엣지 (수평 엣지 우선). */
fun defaultCornerEdge(corner: CornerOverlap): EntryEdge = when (corner) {
    CornerOverlap.TOP_LEFT, CornerOverlap.TOP_RIGHT     -> EntryEdge.TOP
    CornerOverlap.BOTTOM_LEFT, CornerOverlap.BOTTOM_RIGHT -> EntryEdge.BOTTOM
}

// ============================================================
// 존 액션 sealed class
// ============================================================

/** 매크로 실행 전 PC 입력 모드 확인 다이얼로그 표시 여부. */
enum class InputModeCheck {
    NONE,    // 다이얼로그 없음. 기본값: NONE
    KOREAN,  // 실행 전 PC가 한글 모드인지 확인
    ENGLISH, // 실행 전 PC가 영어 모드인지 확인
}

sealed class EdgeZoneAction {
    data class ToggleMode(val mode: EdgeSwipeMode) : EdgeZoneAction()
    data class CyclePreset(val presetType: PresetType) : EdgeZoneAction()
    data class OpenSettings(val settingsType: SettingsType) : EdgeZoneAction()
    object Unassigned : EdgeZoneAction()

    // 값 지정 액션
    data class SetDpi(val level: DpiLevel) : EdgeZoneAction()
    data class SetScrollSpeed(val sensitivity: ScrollSensitivity) : EdgeZoneAction()
    data class SetModePreset(val index: Int) : EdgeZoneAction()
    data class SetDynamicsPreset(val index: Int) : EdgeZoneAction()
    data class SetClickMode(val mode: ClickMode) : EdgeZoneAction()
    data class SetMoveMode(val mode: MoveMode) : EdgeZoneAction()
    data class SetScrollMode(val mode: ScrollMode) : EdgeZoneAction()
    /** 일반 ↔ 무한 스크롤 전환 (끔 상태는 일반으로 진입) */
    object SwapScrollMode : EdgeZoneAction()
    /** 커스텀 DPI 배율 직접 지정. 기본값: 1.0 */
    data class SetCustomDpi(val multiplier: Float) : EdgeZoneAction()
    /** 커스텀 스크롤 속도 배율 직접 지정. 기본값: 1.0 */
    data class SetCustomScrollSpeed(val multiplier: Float) : EdgeZoneAction()
    /** 이전 모드 및 세팅 스냅샷으로 복원. */
    object RestorePreviousMode : EdgeZoneAction()

    /**
     * 단축키 1회 발송 (TAP 의미: press 직후 release).
     * @param modifierBits 수정자 비트플래그 OR 합산 (MOD_BIT_LCTRL 등). 기본값: 0
     * @param keyCodes HID 키코드 목록 (최대 5개). 빈 리스트이면 수정자만 전송. 기본값: emptyList()
     * @param hold 미래 대비 보존 (1차 구현은 TAP 통일). 기본값: false
     * @param presetLabel 프리셋 출처 라벨 (커스텀이면 ""). 기본값: ""
     */
    data class SendShortcut(
        val modifierBits: Int,
        val keyCodes: List<Int> = emptyList(),
        val hold: Boolean = false,
        val presetLabel: String = ""
    ) : EdgeZoneAction() {
        val keyCode: Int get() = keyCodes.firstOrNull() ?: 0
    }

    /**
     * 마우스 버튼 홀드/릴리즈 제어.
     * @param button 대상 버튼. 기본값: LEFT
     * @param mode HOLD=강제 홀드, RELEASE=강제 릴리즈, TOGGLE=현재 상태 반전. 기본값: TOGGLE
     */
    data class MouseHoldToggle(val button: MouseButton, val mode: MouseHoldMode = MouseHoldMode.TOGGLE) : EdgeZoneAction()

    /**
     * 페이지 순환 전환 (다음/이전).
     * @param direction 전환 방향. 기본값: NEXT
     */
    data class CyclePage(val direction: PageNav) : EdgeZoneAction()

    /**
     * 특정 논리 페이지로 점프.
     * @param pageIndex 0-based 논리 페이지 인덱스. 기본값: 0
     */
    data class JumpToPage(val pageIndex: Int) : EdgeZoneAction()

    /** 멀티 커서 활성/비활성 토글. 비활성→활성 시 마지막 커서 수를 복원(없으면 기본 2). */
    object ToggleMultiCursor : EdgeZoneAction()

    /**
     * 특정 멀티 커서 패드를 즉시 활성화.
     * @param index 0-based 패드 인덱스(pad1=0). 기본값: 0
     */
    data class ActivatePad(val index: Int) : EdgeZoneAction()

    /**
     * 멀티 커서 패드 순환 전환 (다음/이전).
     * @param direction 전환 방향. 기본값: NEXT
     */
    data class CyclePad(val direction: PageNav) : EdgeZoneAction()

    /**
     * 멀티 커서 수 직접 지정. 비활성 상태면 활성화, 활성 상태면 수만 변경.
     * @param count 커서 수 (2~4). 기본값: MULTI_CURSOR_COUNT_MIN(2)
     */
    data class SetCursorCount(val count: Int) : EdgeZoneAction()

    /** 멀티 커서 레이아웃 모드 토글 (그리드 분할 ↔ 직접 전환 버튼). */
    object ToggleMultiCursorLayout : EdgeZoneAction()

    /**
     * 매크로 순차 키 입력 (여러 스텝을 딜레이를 두고 순차 전송).
     * @param steps 각 스텝의 키 조합 목록. 기본값: emptyList()
     * @param stepDelayMs 스텝 간 딜레이 (ms). 기본값: MACRO_STEP_DELAY_DEFAULT_MS
     * @param presetLabel 프리셋 출처 라벨 (커스텀이면 ""). 기본값: ""
     * @param inputModeCheck 실행 전 PC 입력 모드 확인 다이얼로그 표시 여부. 기본값: NONE
     * @param groupNames 폴더 이름 맵 (groupId → 폴더명). 편집기 메타데이터로만 사용, 실행에 영향 없음. 기본값: emptyMap()
     */
    data class SendMacro(
        val steps: List<MacroStep> = emptyList(),
        val stepDelayMs: Int = MACRO_STEP_DELAY_DEFAULT_MS,
        val presetLabel: String = "",
        val inputModeCheck: InputModeCheck = InputModeCheck.NONE,
        val groupNames: Map<Int, String> = emptyMap(),  // 기본값: emptyMap()
    ) : EdgeZoneAction()

    /** 액션에 따른 기본 라벨. label이 빈 문자열인 경우 자동 표시에 사용. */
    fun defaultLabel(): String = displayName()

    /** 액션에 따른 기본 아이콘 키. iconKey가 빈 문자열인 경우 자동 표시에 사용. */
    fun defaultIconKey(): String = when (this) {
        is ToggleMode -> when (mode) {
            EdgeSwipeMode.CLICK, EdgeSwipeMode.CURSOR -> "Mouse"
            EdgeSwipeMode.SCROLL -> "SwapVert"
            EdgeSwipeMode.MOVE -> "OpenWith"
            EdgeSwipeMode.DPI -> "Speed"
            EdgeSwipeMode.SCROLL_SPEED -> "Loop"
            EdgeSwipeMode.DYNAMICS -> "Timeline"
        }
        is SetClickMode -> "Mouse"
        is SetScrollMode -> "SwapVert"
        is SetMoveMode -> "OpenWith"
        is SetDpi -> "Speed"
        is SetScrollSpeed -> "Loop"
        is CyclePreset -> when (presetType) {
            PresetType.DYNAMICS -> "Timeline"
            PresetType.MODE -> "Tune"
        }
        is OpenSettings -> when (settingsType) {
            SettingsType.DPI -> "Speed"
            SettingsType.SCROLL_SPEED -> "Loop"
        }
        is SetDynamicsPreset -> "Timeline"
        is SetModePreset -> "Tune"
        SwapScrollMode -> "SwapVert"
        is SetCustomDpi -> "Speed"
        is SetCustomScrollSpeed -> "Loop"
        RestorePreviousMode -> "Undo"
        is SendShortcut -> "Keyboard"
        is SendMacro -> "Keyboard"
        is MouseHoldToggle -> "Mouse"
        is CyclePage -> if (direction == PageNav.NEXT) "ArrowForward" else "ArrowBack"
        is JumpToPage -> "Tune"
        ToggleMultiCursor -> "Group"
        ToggleMultiCursorLayout -> "Autorenew"
        is SetCursorCount -> "Tune"
        is ActivatePad -> "Adjust"
        is CyclePad -> if (direction == PageNav.NEXT) "ArrowForward" else "ArrowBack"
        Unassigned -> ""
    }

    fun displayName(): String = when (this) {
        is ToggleMode -> when (mode) {
            EdgeSwipeMode.CLICK -> "클릭 토글"
            EdgeSwipeMode.SCROLL -> "스크롤 토글"
            EdgeSwipeMode.MOVE -> "이동 토글"
            EdgeSwipeMode.CURSOR -> "커서 토글"
            EdgeSwipeMode.DPI -> "DPI 토글"
            EdgeSwipeMode.SCROLL_SPEED -> "스크롤 속도 토글"
            EdgeSwipeMode.DYNAMICS -> "다이나믹스 토글"
        }
        is CyclePreset -> when (presetType) {
            PresetType.DYNAMICS -> "다이나믹스 순환"
            PresetType.MODE -> "모드 프리셋 순환"
        }
        is OpenSettings -> when (settingsType) {
            SettingsType.DPI -> "DPI 순환"
            SettingsType.SCROLL_SPEED -> "스크롤 속도 순환"
        }
        Unassigned -> "미할당"
        is SetDpi -> "DPI: ${level.label}"
        is SetScrollSpeed -> "스크롤 속도: ${sensitivity.label}"
        is SetModePreset -> "모드 프리셋: ${MODE_PRESETS.getOrNull(index)?.name ?: "?"}"
        is SetDynamicsPreset -> "다이나믹스: ${DYNAMICS_PRESETS.getOrNull(index)?.name ?: "?"}"
        is SetClickMode -> "클릭: ${if (mode == ClickMode.LEFT_CLICK) "좌클릭" else "우클릭"}"
        is SetMoveMode -> "이동: ${if (mode == MoveMode.FREE) "자유" else "직각"}"
        is SetScrollMode -> "스크롤: ${when (mode) { ScrollMode.OFF -> "끔"; ScrollMode.NORMAL_SCROLL -> "일반"; ScrollMode.INFINITE_SCROLL -> "무한" }}"
        SwapScrollMode -> "스크롤 토글"
        is SetCustomDpi -> "DPI: ×${"%.1f".format(multiplier)}"
        is SetCustomScrollSpeed -> "속도: ×${"%.1f".format(multiplier)}"
        RestorePreviousMode -> "이전 모드로 되돌리기"
        is SendShortcut -> presetLabel.ifEmpty { formatShortcutCombo(modifierBits, keyCodes) }
        is SendMacro -> presetLabel.ifEmpty { formatMacroSteps(steps) }
        is MouseHoldToggle -> {
            val btn = when (button) {
                MouseButton.LEFT -> "좌클릭"
                MouseButton.RIGHT -> "우클릭"
                MouseButton.MIDDLE -> "중간클릭"
            }
            when (mode) {
                MouseHoldMode.HOLD -> "$btn 홀드"
                MouseHoldMode.RELEASE -> "$btn 릴리즈"
                MouseHoldMode.TOGGLE -> "$btn 홀드/릴리즈 전환"
            }
        }
        is CyclePage -> if (direction == PageNav.NEXT) "다음 페이지" else "이전 페이지"
        is JumpToPage -> "페이지 ${pageIndex + 1}"
        ToggleMultiCursor -> "멀티 커서 토글"
        ToggleMultiCursorLayout -> "커서 레이아웃 전환"
        is SetCursorCount -> "커서 ${count}개"
        is ActivatePad -> "패드 ${index + 1}"
        is CyclePad -> if (direction == PageNav.NEXT) "다음 패드" else "이전 패드"
    }
}

// ============================================================
// 매크로 스텝
// ============================================================

/**
 * 매크로 스텝 종류.
 * - TAP: press 직후 release (기존 동작, 레거시 기본값)
 * - HOLD: 키를 누른 채 유지 (release 없음). 이후 TAP 스텝은 홀드 상태와 합성되어 전송
 * - RELEASE: 키를 뗌. keyCodes/modifierBits가 비어있으면 전체 릴리즈, 값이 있으면 해당 키만 릴리즈
 */
enum class MacroStepKind { TAP, HOLD, RELEASE }

/**
 * 매크로 단일 스텝.
 * 실행 시 BridgeFrame 제약(최대 2키)으로 keyCodes[0..1]만 전송.
 *
 * @param modifierBits 수정자 비트플래그 OR 합산. 기본값: 0
 * @param keyCodes HID 키코드 목록 (최대 5개 저장, 실행 시 2개 제한). 기본값: emptyList()
 * @param delayAfterMs 이 스텝 후 개별 딜레이 (ms). null이면 매크로 공통 stepDelayMs 사용. 기본값: null
 * @param repeatCount 이 스텝을 연속 반복하는 횟수 (TAP만 유효). 기본값: 1
 * @param groupId 소속 폴더 식별자. 같은 값이 연속된 스텝들이 한 폴더로 묶임. null이면 미소속. 기본값: null
 * @param kind 스텝 종류 (TAP/HOLD/RELEASE). 기본값: TAP (레거시 호환)
 */
data class MacroStep(
    val modifierBits: Int = 0,
    val keyCodes: List<Int> = emptyList(),
    val delayAfterMs: Int? = null,
    val repeatCount: Int = 1,
    val groupId: Int? = null,   // 기본값: null
    val kind: MacroStepKind = MacroStepKind.TAP,   // 기본값: TAP
)

/**
 * 매크로를 끝까지 실행했을 때 해제되지 않고 눌린 채 남는 키(잔여 홀드)가 있는지 판정.
 * true면 "홀드로 끝나는 매크로"로, 생성을 차단한다.
 * 판정 로직은 StandardModePage.onSendMacro의 누적 홀드 상태 추적과 동일하다.
 */
fun List<MacroStep>.endsWithDanglingHold(): Boolean {
    var heldMod = 0
    val heldKeys = mutableListOf<Int>()
    for (step in this) {
        when (step.kind) {
            MacroStepKind.HOLD -> {
                heldMod = heldMod or step.modifierBits
                for (code in step.keyCodes) {
                    if (code != 0 && !heldKeys.contains(code)) heldKeys.add(code)
                }
            }
            MacroStepKind.RELEASE -> {
                if (step.keyCodes.isEmpty() && step.modifierBits == 0) {
                    heldMod = 0
                    heldKeys.clear()
                } else {
                    heldMod = heldMod and step.modifierBits.inv()
                    heldKeys.removeAll(step.keyCodes.toSet())
                }
            }
            MacroStepKind.TAP -> { /* 누적 상태 불변 */ }
        }
    }
    return heldMod != 0 || heldKeys.isNotEmpty()
}

// ============================================================
// 로테이션 후보
// ============================================================

data class RotationCandidate(
    val action: EdgeZoneAction,
    val label: String,
    val iconKey: String,
    val colorHex: String = ""
)

// ============================================================
// 존 트리거 방식
// ============================================================

sealed class EdgeZoneTrigger {
    /** 단일 액션: 손을 뗄 때 고정 액션 1회 실행 */
    data class SingleAction(
        val action: EdgeZoneAction,
        val label: String,
        val iconKey: String,
        val colorHex: String = ""
    ) : EdgeZoneTrigger()

    /** 로테이션: armed 동안 intervalMs 간격으로 후보 순환, 손을 뗄 때 현재 후보 실행 */
    data class Rotation(
        val candidates: List<RotationCandidate>,
        val intervalMs: Int
    ) : EdgeZoneTrigger()
}

// ============================================================
// 존 정의
// ============================================================

/**
 * 엣지의 특정 구간에 배정된 트리거 단위.
 *
 * @param edge       해당 엣지 (TOP/BOTTOM/LEFT/RIGHT)
 * @param startRatio 구간 시작 비율 (0.0~1.0), TOP/BOTTOM: 좌→우, LEFT/RIGHT: 위→아래
 * @param endRatio   구간 끝 비율 (startRatio < endRatio)
 * @param trigger    트리거 방식 및 실행 액션
 */
data class EdgeZone(
    val edge: EntryEdge,
    val startRatio: Float,
    val endRatio: Float,
    val trigger: EdgeZoneTrigger
) {
    // SingleAction 호환 편의 프로퍼티 (오버레이/프리뷰 등 읽기 전용 용도)
    val action: EdgeZoneAction
        get() = when (val t = trigger) {
            is EdgeZoneTrigger.SingleAction -> t.action
            is EdgeZoneTrigger.Rotation -> EdgeZoneAction.Unassigned
        }
    val label: String
        get() = when (val t = trigger) {
            is EdgeZoneTrigger.SingleAction -> t.label
            is EdgeZoneTrigger.Rotation -> t.candidates.firstOrNull()?.label ?: ""
        }
    val iconKey: String
        get() = when (val t = trigger) {
            is EdgeZoneTrigger.SingleAction -> t.iconKey
            is EdgeZoneTrigger.Rotation -> t.candidates.firstOrNull()?.iconKey ?: ""
        }
    val colorHex: String
        get() = when (val t = trigger) {
            is EdgeZoneTrigger.SingleAction -> t.colorHex
            is EdgeZoneTrigger.Rotation -> t.candidates.firstOrNull()?.colorHex ?: ""
        }

    fun withLabel(label: String): EdgeZone = copy(
        trigger = when (val t = trigger) {
            is EdgeZoneTrigger.SingleAction -> t.copy(label = label)
            is EdgeZoneTrigger.Rotation -> t
        }
    )

    fun withIconKey(iconKey: String): EdgeZone = copy(
        trigger = when (val t = trigger) {
            is EdgeZoneTrigger.SingleAction -> t.copy(iconKey = iconKey)
            is EdgeZoneTrigger.Rotation -> t
        }
    )

    fun withColor(colorHex: String): EdgeZone = copy(
        trigger = when (val t = trigger) {
            is EdgeZoneTrigger.SingleAction -> t.copy(colorHex = colorHex)
            is EdgeZoneTrigger.Rotation -> t
        }
    )

    fun withAction(action: EdgeZoneAction): EdgeZone = copy(
        trigger = when (val t = trigger) {
            is EdgeZoneTrigger.SingleAction -> t.copy(action = action)
            is EdgeZoneTrigger.Rotation -> t
        }
    )
}

// ============================================================
// 존 설정 컨테이너
// ============================================================

/**
 * 4개 엣지의 존 목록 전체를 담는 컨테이너.
 * SharedPreferences에 JSON으로 영속화된다.
 */
data class EdgeZoneConfig(
    val topZones: List<EdgeZone>,
    val bottomZones: List<EdgeZone>,
    val leftZones: List<EdgeZone>,
    val rightZones: List<EdgeZone>,
    /** 코너 겹침 영역에서 어느 엣지가 우선 작동할지. 기본값: 수평 엣지(TOP/BOTTOM) 우선. */
    val cornerPriority: Map<CornerOverlap, EntryEdge> = mapOf(
        CornerOverlap.TOP_LEFT     to EntryEdge.TOP,
        CornerOverlap.TOP_RIGHT    to EntryEdge.TOP,
        CornerOverlap.BOTTOM_LEFT  to EntryEdge.BOTTOM,
        CornerOverlap.BOTTOM_RIGHT to EntryEdge.BOTTOM
    )
) {
    fun zonesFor(edge: EntryEdge): List<EdgeZone> = when (edge) {
        EntryEdge.TOP    -> topZones
        EntryEdge.BOTTOM -> bottomZones
        EntryEdge.LEFT   -> leftZones
        EntryEdge.RIGHT  -> rightZones
    }

    fun withZones(edge: EntryEdge, zones: List<EdgeZone>): EdgeZoneConfig = when (edge) {
        EntryEdge.TOP    -> copy(topZones = zones)
        EntryEdge.BOTTOM -> copy(bottomZones = zones)
        EntryEdge.LEFT   -> copy(leftZones = zones)
        EntryEdge.RIGHT  -> copy(rightZones = zones)
    }

    fun withCornerPriority(corner: CornerOverlap, edge: EntryEdge): EdgeZoneConfig =
        copy(cornerPriority = cornerPriority + (corner to edge))

    fun toggleCornerPriority(corner: CornerOverlap): EdgeZoneConfig {
        val current = cornerPriority[corner] ?: defaultCornerEdge(corner)
        val toggled = when (corner) {
            CornerOverlap.TOP_LEFT     -> if (current == EntryEdge.TOP)    EntryEdge.LEFT  else EntryEdge.TOP
            CornerOverlap.TOP_RIGHT    -> if (current == EntryEdge.TOP)    EntryEdge.RIGHT else EntryEdge.TOP
            CornerOverlap.BOTTOM_LEFT  -> if (current == EntryEdge.BOTTOM) EntryEdge.LEFT  else EntryEdge.BOTTOM
            CornerOverlap.BOTTOM_RIGHT -> if (current == EntryEdge.BOTTOM) EntryEdge.RIGHT else EntryEdge.BOTTOM
        }
        return withCornerPriority(corner, toggled)
    }

    companion object {
        fun default(): EdgeZoneConfig = EdgeZoneConfig(
            topZones = listOf(
                EdgeZone(EntryEdge.TOP, 0f, 0.5f, EdgeZoneTrigger.SingleAction(EdgeZoneAction.CyclePreset(PresetType.DYNAMICS), "다이나믹스", "Timeline")),
                EdgeZone(EntryEdge.TOP, 0.5f, 1f, EdgeZoneTrigger.SingleAction(EdgeZoneAction.CyclePreset(PresetType.MODE), "프리셋", "Tune"))
            ),
            bottomZones = listOf(
                EdgeZone(EntryEdge.BOTTOM, 0f, 1f, EdgeZoneTrigger.SingleAction(EdgeZoneAction.Unassigned, "", ""))
            ),
            leftZones = listOf(
                EdgeZone(EntryEdge.LEFT, 0f, 0.5f, EdgeZoneTrigger.SingleAction(EdgeZoneAction.ToggleMode(EdgeSwipeMode.CLICK), "클릭", "Mouse")),
                EdgeZone(EntryEdge.LEFT, 0.5f, 1f, EdgeZoneTrigger.SingleAction(EdgeZoneAction.ToggleMode(EdgeSwipeMode.SCROLL), "스크롤", "SwapVert"))
            ),
            rightZones = listOf(
                EdgeZone(EntryEdge.RIGHT, 0f, 0.5f, EdgeZoneTrigger.SingleAction(EdgeZoneAction.ToggleMode(EdgeSwipeMode.MOVE), "이동", "OpenWith")),
                EdgeZone(EntryEdge.RIGHT, 0.5f, 1f, EdgeZoneTrigger.SingleAction(EdgeZoneAction.OpenSettings(SettingsType.DPI), "DPI", "Speed"))
            )
        )
    }
}

// ============================================================
// SendShortcut 표시 헬퍼
// ============================================================

/** 수정자 비트 → 라벨 매핑 (MOD_BIT_LCTRL=0x01, LSHIFT=0x02, LALT=0x04, LGUI=0x08) */
internal val MODIFIER_LABELS = listOf(
    0x01 to "Ctrl",
    0x02 to "Shift",
    0x04 to "Alt",
    0x08 to "Win"
)

/** HID 키코드(Int) → 사람이 읽기 쉬운 라벨 역매핑 */
internal val KEY_CODE_LABELS: Map<Int, String> = buildMap {
    // A-Z (0x04-0x1D)
    for (i in 0 until 26) put(0x04 + i, ('A' + i).toString())
    // 숫자 (1-9: 0x1E-0x26, 0: 0x27)
    for (i in 1..9) put(0x1D + i, i.toString())
    put(0x27, "0")
    // 제어 키
    put(0x28, "Enter"); put(0x29, "Esc"); put(0x2A, "BkSp")
    put(0x2B, "Tab"); put(0x2C, "Space")
    // 기호 키
    put(0x35, "`"); put(0x2D, "-"); put(0x2E, "=")
    put(0x2F, "["); put(0x30, "]"); put(0x31, "\\")
    put(0x33, ";"); put(0x34, "'")
    put(0x36, ","); put(0x37, "."); put(0x38, "/")
    put(0x39, "CapsLk")
    // 펑션 키 (F1-F12: 0x3A-0x45)
    for (i in 1..12) put(0x39 + i, "F$i")
    // 방향/편집 키
    put(0x4F, "→"); put(0x50, "←"); put(0x51, "↓"); put(0x52, "↑")
    put(0x4A, "Home"); put(0x4D, "End")
    put(0x4B, "PgUp"); put(0x4E, "PgDn")
    put(0x4C, "Del"); put(0x49, "Ins")
    put(0x46, "PrtSc"); put(0x47, "ScrlLk"); put(0x48, "Pause")
    put(0x2C, "Space")
}

/**
 * modifierBits + keyCode를 "Ctrl+Shift+C" 형태의 표시 문자열로 변환.
 * 기본값: "단축키"
 */
fun formatShortcutCombo(modifierBits: Int, keyCodes: List<Int> = emptyList()): String {
    val parts = mutableListOf<String>()
    for ((bit, label) in MODIFIER_LABELS) {
        if (modifierBits and bit != 0) parts.add(label)
    }
    for (code in keyCodes) {
        val label = KEY_CODE_LABELS[code]
        if (label != null) parts.add(label)
    }
    return if (parts.isEmpty()) "단축키" else parts.joinToString("+")
}

/**
 * 매크로 스텝 목록을 "Ctrl+V → F" 형태의 표시 문자열로 변환.
 * HOLD는 "⬇ Shift", RELEASE는 "⬆ Shift" / "⬆ 전체" 접두. 기본값: "매크로"
 */
fun formatMacroSteps(steps: List<MacroStep>): String {
    if (steps.isEmpty()) return "매크로"
    return steps.joinToString(" → ") { step ->
        when (step.kind) {
            MacroStepKind.HOLD -> {
                val combo = formatShortcutCombo(step.modifierBits, step.keyCodes)
                "⬇ $combo"
            }
            MacroStepKind.RELEASE -> {
                if (step.keyCodes.isEmpty() && step.modifierBits == 0) "⬆ 전체"
                else "⬆ ${formatShortcutCombo(step.modifierBits, step.keyCodes)}"
            }
            MacroStepKind.TAP -> {
                val combo = formatShortcutCombo(step.modifierBits, step.keyCodes)
                if (step.repeatCount > 1) "$combo ×${step.repeatCount}" else combo
            }
        }
    }
}
