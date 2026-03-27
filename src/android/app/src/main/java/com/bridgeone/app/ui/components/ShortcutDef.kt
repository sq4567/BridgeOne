package com.bridgeone.app.ui.components

import com.bridgeone.app.ui.common.*

/**
 * 단축키 동작 모드
 */
enum class ShortcutHoldBehavior {
    /** 탭: Modifier↓ → Key↓ → Key↑ → Modifier↑ 즉시 전송 */
    TAP,
    /** 홀드: 누름 동안 키 유지, 뗌 시 해제 (Alt+Tab 등) */
    HOLD
}

/**
 * 단축키 정의 데이터 클래스
 *
 * @param label 단축키 식별 레이블 (예: "Ctrl+C")
 * @param modifiers HID 수정자 비트플래그 목록 (예: [MOD_BIT_LCTRL])
 * @param key 주 키 HID 코드 (UByte)
 * @param displayChips UI 표시용 칩 목록 (예: ["Ctrl", "C"])
 * @param holdBehavior 동작 모드 (TAP 또는 HOLD)
 * @param debounceDurationMs 디바운스 시간 (ms)
 * @param description 접근성 설명 (예: "복사")
 */
data class ShortcutDef(
    val label: String,
    val modifiers: List<UByte>,
    val key: UByte,
    val displayChips: List<String>,
    val holdBehavior: ShortcutHoldBehavior = ShortcutHoldBehavior.TAP,
    val debounceDurationMs: Long = 150L,
    val description: String = ""
) {
    /** 수정자 비트플래그를 합산한 단일 바이트 */
    val combinedModifiers: UByte
        get() = modifiers.fold(0u.toUByte()) { acc, m ->
            (acc.toInt() or m.toInt()).toUByte()
        }
}

/**
 * 기본 단축키 세트 (8개)
 */
val DEFAULT_SHORTCUTS = listOf(
    ShortcutDef(
        label = "Ctrl+C",
        modifiers = listOf(MOD_BIT_LCTRL),
        key = KEY_C,
        displayChips = listOf("Ctrl", "C"),
        description = "복사"
    ),
    ShortcutDef(
        label = "Ctrl+V",
        modifiers = listOf(MOD_BIT_LCTRL),
        key = KEY_V,
        displayChips = listOf("Ctrl", "V"),
        description = "붙여넣기"
    ),
    ShortcutDef(
        label = "Ctrl+S",
        modifiers = listOf(MOD_BIT_LCTRL),
        key = KEY_S,
        displayChips = listOf("Ctrl", "S"),
        description = "저장"
    ),
    ShortcutDef(
        label = "Ctrl+Z",
        modifiers = listOf(MOD_BIT_LCTRL),
        key = KEY_Z,
        displayChips = listOf("Ctrl", "Z"),
        description = "실행 취소"
    ),
    ShortcutDef(
        label = "Ctrl+Shift+Z",
        modifiers = listOf(MOD_BIT_LCTRL, MOD_BIT_LSHIFT),
        key = KEY_Z,
        displayChips = listOf("Ctrl", "Shift", "Z"),
        description = "다시 실행"
    ),
    ShortcutDef(
        label = "Ctrl+X",
        modifiers = listOf(MOD_BIT_LCTRL),
        key = KEY_X,
        displayChips = listOf("Ctrl", "X"),
        description = "잘라내기"
    ),
    ShortcutDef(
        label = "Alt+Tab",
        modifiers = listOf(MOD_BIT_LALT),
        key = KEY_TAB,
        displayChips = listOf("Alt", "Tab"),
        holdBehavior = ShortcutHoldBehavior.HOLD,
        description = "창 전환"
    ),
    ShortcutDef(
        label = "Win+D",
        modifiers = listOf(MOD_BIT_LGUI),
        key = KEY_D,
        displayChips = listOf("Win", "D"),
        debounceDurationMs = 500L,
        description = "바탕화면 보기"
    )
)
