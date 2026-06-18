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
import com.bridgeone.app.ui.common.KEY_BACKSPACE
import com.bridgeone.app.ui.common.KEY_DELETE
import com.bridgeone.app.ui.common.KEY_END
import com.bridgeone.app.ui.common.KEY_ENTER
import com.bridgeone.app.ui.common.KEY_ESC
import com.bridgeone.app.ui.common.KEY_HOME
import com.bridgeone.app.ui.common.KEY_SPACE
import com.bridgeone.app.ui.common.KEY_TAB
import com.bridgeone.app.ui.components.KeyboardKeyButton
import com.bridgeone.app.ui.utils.ClickDetector

// ============================================================
// Special Keys 그룹 (Phase 4.2.3)
// ============================================================

/**
 * Special Keys 2열 그리드
 *
 * 8개 키: Esc, Tab, Enter, Backspace, Delete, Space, Home, End
 * - 모두 stickyHoldEnabled=false (자연 홀드)
 * - 길게 누르면 PC OS가 자체적으로 키 반복 처리 (물리 키보드와 동일)
 */
@Composable
internal fun SpecialKeysGrid() {
    val keys = listOf(
        "Esc" to KEY_ESC,
        "Tab" to KEY_TAB,
        "Enter" to KEY_ENTER,
        "⌫" to KEY_BACKSPACE,
        "Del" to KEY_DELETE,
        "Space" to KEY_SPACE,
        "Home" to KEY_HOME,
        "End" to KEY_END
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        keys.chunked(2).forEach { rowKeys ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rowKeys.forEach { (label, keyCode) ->
                    KeyboardKeyButton(
                        keyLabel = label,
                        keyCode = keyCode,
                        stickyHoldEnabled = false,
                        onKeyPressed = { code ->
                            val frame = ClickDetector.createKeyboardFrame(
                                activeModifierKeys = emptySet(),
                                keyCode1 = code
                            )
                            ClickDetector.sendFrame(frame)
                        },
                        onKeyReleased = { _ ->
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
                if (rowKeys.size < 2) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
