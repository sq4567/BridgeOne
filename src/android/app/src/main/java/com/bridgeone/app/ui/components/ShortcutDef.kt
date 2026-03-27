package com.bridgeone.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.ui.graphics.vector.ImageVector
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
 * @param icon 버튼에 표시할 아이콘
 * @param displayChips UI 표시용 칩 목록 (예: ["Ctrl", "C"]) — 아이콘 없을 때 폴백
 * @param holdBehavior 동작 모드 (TAP 또는 HOLD)
 * @param debounceDurationMs 디바운스 시간 (ms)
 * @param description 접근성 설명 (예: "복사")
 */
data class ShortcutDef(
    val label: String,
    val modifiers: List<UByte>,
    val key: UByte,
    val icon: ImageVector,
    val displayChips: List<String> = emptyList(),
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
        icon = Icons.Filled.ContentCopy,
        description = "복사"
    ),
    ShortcutDef(
        label = "Ctrl+V",
        modifiers = listOf(MOD_BIT_LCTRL),
        key = KEY_V,
        icon = Icons.Filled.ContentPaste,
        description = "붙여넣기"
    ),
    ShortcutDef(
        label = "Ctrl+S",
        modifiers = listOf(MOD_BIT_LCTRL),
        key = KEY_S,
        icon = Icons.Filled.Save,
        description = "저장"
    ),
    ShortcutDef(
        label = "Ctrl+Z",
        modifiers = listOf(MOD_BIT_LCTRL),
        key = KEY_Z,
        icon = Icons.AutoMirrored.Filled.Undo,
        description = "실행 취소"
    ),
    ShortcutDef(
        label = "Ctrl+Shift+Z",
        modifiers = listOf(MOD_BIT_LCTRL, MOD_BIT_LSHIFT),
        key = KEY_Z,
        icon = Icons.AutoMirrored.Filled.Redo,
        description = "다시 실행"
    ),
    ShortcutDef(
        label = "Ctrl+X",
        modifiers = listOf(MOD_BIT_LCTRL),
        key = KEY_X,
        icon = Icons.Filled.ContentCut,
        description = "잘라내기"
    ),
    ShortcutDef(
        label = "Alt+Tab",
        modifiers = listOf(MOD_BIT_LALT),
        key = KEY_TAB,
        icon = Icons.Filled.SwapHoriz,
        holdBehavior = ShortcutHoldBehavior.HOLD,
        description = "창 전환"
    ),
    ShortcutDef(
        label = "Win+D",
        modifiers = listOf(MOD_BIT_LGUI),
        key = KEY_D,
        icon = Icons.Filled.DesktopWindows,
        debounceDurationMs = 500L,
        description = "바탕화면 보기"
    )
)
