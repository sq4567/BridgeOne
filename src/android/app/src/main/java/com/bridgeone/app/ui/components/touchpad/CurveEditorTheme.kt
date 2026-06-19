package com.bridgeone.app.ui.components.touchpad

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────────────────────
// 곡선 편집기 공유 테마: 색상·슬롯 모델·셀 스타일 헬퍼
// (Phase 4.7.6-A: DynamicsCurveEditor.kt에서 분리)
// ─────────────────────────────────────────────────────────────

internal val BG = Color(0xFF0D0D0D)
internal val SURFACE = Color(0xFF1A1A1A)
internal val ACCENT_BLUE = Color(0xFF4F8EF7)
internal val ACCENT_ORANGE = Color(0xFFFF9800)
internal val ACCENT_RED = Color(0xFFFFB4B4)   // 파괴적 액션(삭제) 비활성 텍스트 색
internal val GRID_COLOR = Color(0xFF2A2A2A)
internal val LABEL_COLOR = Color(0xFF888888)

// ─────────────────────────────────────────────────────────────
// 액션 그리드 슬롯 모델
// ─────────────────────────────────────────────────────────────

internal enum class SlotStyle { NORMAL, PRIMARY, SECONDARY, SEGMENT_LEFT, SEGMENT_RIGHT }

internal data class ActionSlot(
    val label: String,
    val enabled: Boolean,
    val isCurrent: Boolean = false,
    val previewText: String = "",
    val iconKey: String = "",
    val style: SlotStyle = SlotStyle.NORMAL
)

// ─────────────────────────────────────────────────────────────
// 셀 스타일 헬퍼
// ─────────────────────────────────────────────────────────────

internal fun cellBgColor(
    isHovered: Boolean, isAwaitingConfirm: Boolean, enabled: Boolean, isCurrent: Boolean = false
): Color = when {
    isAwaitingConfirm && enabled -> ACCENT_BLUE.copy(alpha = 0.55f)
    isAwaitingConfirm            -> Color.White.copy(alpha = 0.12f)
    isHovered && enabled         -> ACCENT_BLUE.copy(alpha = 0.38f)
    isHovered                    -> Color.White.copy(alpha = 0.06f)
    isCurrent                    -> ACCENT_BLUE.copy(alpha = 0.18f)
    else                         -> Color.Transparent
}

internal fun Modifier.cellBorder(
    isHovered: Boolean, isAwaitingConfirm: Boolean, enabled: Boolean,
    isCurrent: Boolean = false, shape: RoundedCornerShape = RoundedCornerShape(6.dp)
): Modifier = this.then(when {
    isAwaitingConfirm && enabled -> Modifier.border(2.dp, Color.White.copy(alpha = 0.9f), shape)
    isAwaitingConfirm            -> Modifier.border(2.dp, Color.White.copy(alpha = 0.4f), shape)
    isHovered && enabled         -> Modifier.border(1.5.dp, ACCENT_BLUE, shape)
    isCurrent                    -> Modifier.border(1.dp, ACCENT_BLUE, shape)
    else                         -> Modifier
})

/**
 * 노드 편집 그리드/헤더 전용 셀 박스. 상태 색·보더를 120ms 트랜지션으로 부드럽게 전환.
 * - tintColor: 호버/awaiting 강조 색 (기본 ACCENT_BLUE; 삭제 슬롯은 ACCENT_RED 전달)
 * - showAccentIdleBorder: true → idle enabled 보더를 tintColor 0.45 alpha로 (추가/삭제/탭토글 전용)
 */
@Composable
internal fun AnimatedCellBox(
    isHovered: Boolean,
    isAwaitingConfirm: Boolean,
    enabled: Boolean,
    shape: Shape = RoundedCornerShape(8.dp),
    tintColor: Color = ACCENT_BLUE,
    showAccentIdleBorder: Boolean = false,
    contentAlignment: Alignment = Alignment.Center,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val bgTarget = when {
        isAwaitingConfirm && enabled -> tintColor.copy(alpha = 0.55f)
        isAwaitingConfirm            -> Color.White.copy(alpha = 0.12f)
        isHovered && enabled         -> tintColor.copy(alpha = 0.38f)
        isHovered                    -> Color.White.copy(alpha = 0.06f)
        !enabled                     -> Color.White.copy(alpha = 0.03f)
        else                         -> Color.Transparent
    }
    val animBg by animateColorAsState(bgTarget, tween(120, easing = FastOutSlowInEasing), label = "cellBg")

    val borderColorTarget = when {
        isAwaitingConfirm && enabled -> Color.White.copy(alpha = 0.9f)
        isAwaitingConfirm            -> Color.White.copy(alpha = 0.4f)
        isHovered && enabled         -> tintColor
        showAccentIdleBorder && enabled -> tintColor.copy(alpha = 0.45f)
        enabled                      -> Color.White.copy(alpha = 0.10f)
        else                         -> Color.Transparent
    }
    val animBorderColor by animateColorAsState(borderColorTarget, tween(120, easing = FastOutSlowInEasing), label = "cellBorderColor")

    val borderWidth = when {
        isAwaitingConfirm            -> 2.dp
        isHovered && enabled         -> 1.5.dp
        showAccentIdleBorder && enabled -> 1.dp
        enabled                      -> 0.5.dp
        else                         -> 0.dp
    }

    Box(
        modifier = modifier
            .background(animBg, shape)
            .border(borderWidth, animBorderColor, shape),
        contentAlignment = contentAlignment,
        content = content
    )
}
