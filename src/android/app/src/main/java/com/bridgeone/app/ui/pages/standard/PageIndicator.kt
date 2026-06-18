package com.bridgeone.app.ui.pages.standard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.abs

// ============================================================
// 페이지 인디케이터
// ============================================================

@Composable
internal fun PageIndicator(
    currentPage: Int,
    offsetFraction: Float,
    pageCount: Int,
    onPageClick: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val dotSizeDp = 8.dp
    val dotSpacingDp = 16.dp
    val totalWidth = (dotSizeDp * pageCount) + (dotSpacingDp * (pageCount - 1))

    val density = LocalDensity.current
    val dotSizePx = with(density) { dotSizeDp.toPx() }
    val dotSpacingPx = with(density) { dotSpacingDp.toPx() }
    val dotStepPx = dotSizePx + dotSpacingPx  // 한 닷에서 다음 닷까지 거리

    val absOffset = abs(offsetFraction)
    val direction = if (offsetFraction > 0) 1f else -1f

    // THIN_WORM 효과:
    // head(앞 가장자리)가 먼저 빠르게 도달하고, tail(뒤 가장자리)이 나중에 따라옴
    val headProgress = minOf(1f, absOffset * 2f)   // 0.0 → 0.5 구간에서 0→1
    val tailProgress = maxOf(0f, absOffset * 2f - 1f)  // 0.5 → 1.0 구간에서 0→1

    val currentOriginPx = currentPage * dotStepPx

    // tail: 후반부에 출발점을 이동
    val tailPx = currentOriginPx + tailProgress * direction * dotStepPx
    // head: 전반부에 도착점으로 이동
    val headPx = currentOriginPx + dotSizePx + headProgress * direction * dotStepPx

    val leftPx = minOf(tailPx, headPx)
    val widthPx = maxOf(dotSizePx, abs(headPx - tailPx))

    val leftDp = with(density) { leftPx.toDp() }
    val widthDp = with(density) { widthPx.toDp() }

    Box(
        modifier = modifier
            .width(totalWidth)
            .height(dotSizeDp)
    ) {
        // ── 배경 닷들 (비활성, 회색) ──
        Row(
            modifier = Modifier.matchParentSize(),
            horizontalArrangement = Arrangement.spacedBy(dotSpacingDp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(pageCount) { index ->
                Box(
                    modifier = Modifier
                        .size(dotSizeDp)
                        .background(Color(0xFFC2C2C2), CircleShape)
                        .clickable { onPageClick(index) }
                )
            }
        }

        // ── THIN_WORM 슬라이더 (파란색, 늘어나는 캡슐 모양) ──
        Box(
            modifier = Modifier
                .offset(x = leftDp)
                .width(widthDp)
                .height(dotSizeDp)
                .clip(CircleShape)
                .background(Color(0xFF2196F3))
        )
    }
}
