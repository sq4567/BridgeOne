package com.bridgeone.app.ui.pages.standard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bridgeone.app.ui.components.DEFAULT_SHORTCUTS
import com.bridgeone.app.ui.components.ShortcutButton
import com.bridgeone.app.ui.utils.ClickDetector

// ============================================================
// Shortcuts 그룹 (Phase 4.2.4)
// ============================================================

/**
 * Shortcuts 2열 그리드
 *
 * 8개 단축키: Ctrl+C, Ctrl+V, Ctrl+S, Ctrl+Z, Ctrl+Shift+Z, Ctrl+X, Alt+Tab, Win+D
 * - TAP 모드: 탭 → Modifier↓ → Key↓ → Key↑ → Modifier↑ 순차 전송
 * - HOLD 모드: Alt+Tab — 누름 동안 유지, 뗌 시 해제
 * - 150ms 디바운스 (Win+D는 500ms)
 */
@Composable
internal fun ShortcutsGrid() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        DEFAULT_SHORTCUTS.chunked(2).forEach { rowShortcuts ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rowShortcuts.forEach { shortcutDef ->
                    ShortcutButton(
                        shortcutDef = shortcutDef,
                        onShortcutTriggered = { mod, key ->
                            val frame = ClickDetector.createKeyboardFrame(
                                activeModifierKeys = if (mod != 0u.toUByte()) setOf(mod) else emptySet(),
                                keyCode1 = key
                            )
                            ClickDetector.sendFrame(frame)
                        },
                        onShortcutReleased = { _, _ ->
                            val frame = ClickDetector.createKeyboardFrame(
                                activeModifierKeys = emptySet(),
                                keyCode1 = 0u
                            )
                            ClickDetector.sendFrame(frame)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                    )
                }
                if (rowShortcuts.size < 2) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
