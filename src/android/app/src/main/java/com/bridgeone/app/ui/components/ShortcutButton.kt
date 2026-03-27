package com.bridgeone.app.ui.components

import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

private const val TAG = "ShortcutButton"

/**
 * ShortcutButton 컴포넌트
 *
 * 키 조합(예: Ctrl+C)을 원터치로 실행하는 버튼.
 * - TAP 모드: 탭 시 Modifier↓ → Key↓ → Key↑ → Modifier↑ 순차 전송
 * - HOLD 모드: 누름 동안 키 유지, 뗌 시 해제 (Alt+Tab 등)
 * - 디바운스: 기본 150ms (Win+D는 500ms)
 * - 스케일 피드백: 누름 시 0.98, 뗌 시 1.0 (200ms)
 *
 * @param shortcutDef 단축키 정의
 * @param modifier 외부 Modifier
 * @param isEnabled 사용 가능 여부
 * @param onShortcutTriggered 단축키 전송 콜백 (combinedModifiers, keyCode)
 * @param onShortcutReleased HOLD 모드 해제 콜백 (combinedModifiers, keyCode)
 */
@Composable
fun ShortcutButton(
    shortcutDef: ShortcutDef,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    onShortcutTriggered: ((modifiers: UByte, keyCode: UByte) -> Unit)? = null,
    onShortcutReleased: ((modifiers: UByte, keyCode: UByte) -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // 디바운스 제어
    var lastTriggerTime by remember { mutableLongStateOf(0L) }

    // HOLD 모드 상태 추적
    var isHolding by remember { mutableStateOf(false) }

    // 스케일 피드백 (0.98 → 1.0, 200ms)
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1.0f,
        animationSpec = tween(durationMillis = 200),
        label = "ShortcutScale"
    )

    val backgroundColor by animateColorAsState(
        targetValue = when {
            !isEnabled -> Color(0xFF3A3A3A)
            isHolding -> Color(0xFF1565C0)
            isPressed -> Color(0xFF1976D2)
            else -> Color(0xFF2196F3)
        },
        label = "ShortcutBgColor"
    )

    // HOLD 모드: 누름/뗌 감지
    LaunchedEffect(isPressed) {
        if (shortcutDef.holdBehavior == ShortcutHoldBehavior.HOLD) {
            if (isPressed && !isHolding) {
                isHolding = true
                onShortcutTriggered?.invoke(shortcutDef.combinedModifiers, shortcutDef.key)
                Log.d(TAG, "Hold start: ${shortcutDef.label}")
            } else if (!isPressed && isHolding) {
                isHolding = false
                onShortcutReleased?.invoke(shortcutDef.combinedModifiers, shortcutDef.key)
                Log.d(TAG, "Hold end: ${shortcutDef.label}")
            }
        }
    }

    val shape = RoundedCornerShape(6.dp)
    val accessibilityDesc = "단축키 ${shortcutDef.label}, ${shortcutDef.description}"

    Box(
        modifier = modifier
            .scale(scale)
            .clip(shape)
            .background(backgroundColor)
            .semantics { contentDescription = accessibilityDesc }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = isEnabled
            ) {
                // TAP 모드에서만 onClick 처리
                if (shortcutDef.holdBehavior == ShortcutHoldBehavior.TAP) {
                    val now = System.currentTimeMillis()
                    if (now - lastTriggerTime >= shortcutDef.debounceDurationMs) {
                        lastTriggerTime = now
                        onShortcutTriggered?.invoke(shortcutDef.combinedModifiers, shortcutDef.key)
                        Log.d(TAG, "Tap: ${shortcutDef.label} (mod=0x${shortcutDef.combinedModifiers.toString(16)}, key=0x${shortcutDef.key.toString(16)})")
                    } else {
                        Log.d(TAG, "Debounced: ${shortcutDef.label}")
                    }
                }
                // HOLD 모드: onClick에서 아무것도 안 함 (LaunchedEffect가 처리)
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = shortcutDef.icon,
            contentDescription = shortcutDef.description,
            tint = if (isEnabled) Color.White else Color(0xFF666666),
            modifier = Modifier.size(20.dp)
        )
    }
}


@Preview(
    showBackground = true,
    backgroundColor = 0xFF121212,
    widthDp = 200,
    heightDp = 300
)
@Composable
fun ShortcutButtonPreview() {
    Column(
        modifier = Modifier
            .size(200.dp, 300.dp)
            .background(Color(0xFF121212))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ShortcutButton(
                shortcutDef = DEFAULT_SHORTCUTS[0], // Ctrl+C
                modifier = Modifier.weight(1f).size(width = 80.dp, height = 44.dp)
            )
            ShortcutButton(
                shortcutDef = DEFAULT_SHORTCUTS[1], // Ctrl+V
                modifier = Modifier.weight(1f).size(width = 80.dp, height = 44.dp)
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ShortcutButton(
                shortcutDef = DEFAULT_SHORTCUTS[4], // Ctrl+Shift+Z
                modifier = Modifier.weight(1f).size(width = 80.dp, height = 44.dp)
            )
            ShortcutButton(
                shortcutDef = DEFAULT_SHORTCUTS[6], // Alt+Tab
                modifier = Modifier.weight(1f).size(width = 80.dp, height = 44.dp)
            )
        }
        // Disabled
        ShortcutButton(
            shortcutDef = DEFAULT_SHORTCUTS[0],
            isEnabled = false,
            modifier = Modifier.size(width = 80.dp, height = 44.dp)
        )
    }
}
