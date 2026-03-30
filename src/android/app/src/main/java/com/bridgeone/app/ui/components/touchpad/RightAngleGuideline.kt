package com.bridgeone.app.ui.components.touchpad

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bridgeone.app.ui.utils.RightAngleAxis
import kotlin.math.cos
import kotlin.math.sin

/**
 * 직각 이동 모드의 축 잠금 상태를 시각적으로 표시하는 오버레이
 *
 * 터치패드 중앙에 십자(크로스헤어) 형태의 양방향 화살표를 그립니다:
 * - 잠긴 축: 밝은 흰색 + 양쪽 화살촉
 * - 차단된 축: 매우 흐린 흰색 + 양쪽 화살촉
 *
 * [isVisible]이 false가 되면 animateFloatAsState로 부드럽게 페이드아웃됩니다.
 *
 * @param isVisible 표시 여부 (주축 확정 시 true, 손가락 뗄 때 false)
 * @param axis 현재 잠긴 축
 */
@Composable
fun RightAngleGuideline(
    isVisible: Boolean,
    axis: RightAngleAxis,
    modifier: Modifier = Modifier
) {
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 250),
        label = "RightAngleGuidelineAlpha"
    )

    if (alpha < 0.01f) return

    Canvas(modifier = modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val edgePad = 20.dp.toPx()
        val topEdgePad = 80.dp.toPx()   // 상단은 컨테이너 버튼 영역을 피해 더 아래에서 시작
        val strokePx = 1.5.dp.toPx()
        val headLen = 9.dp.toPx()
        val headAngleDeg = 28f

        val activeAlpha = 0.85f * alpha
        val dimAlpha = 0.13f * alpha

        val guidelineColor = Color(0xFFFF8A00)  // 직각 이동 버튼 배경색과 동일 (ControlButtonContainer.ColorOrange)

        val hAlpha = if (axis == RightAngleAxis.HORIZONTAL) activeAlpha else dimAlpha
        val vAlpha = if (axis == RightAngleAxis.VERTICAL) activeAlpha else dimAlpha

        // ── 가로 양방향 화살표 (←────────→) ──
        val hStart = Offset(edgePad, cy)
        val hEnd = Offset(size.width - edgePad, cy)
        drawLine(
            color = guidelineColor.copy(alpha = hAlpha),
            start = hStart,
            end = hEnd,
            strokeWidth = strokePx,
            cap = StrokeCap.Round
        )
        drawArrowhead(hStart, Offset(-1f, 0f), headLen, headAngleDeg, guidelineColor.copy(alpha = hAlpha), strokePx)
        drawArrowhead(hEnd,   Offset( 1f, 0f), headLen, headAngleDeg, guidelineColor.copy(alpha = hAlpha), strokePx)

        // ── 세로 양방향 화살표 (↑────────↓) ──
        val vStart = Offset(cx, topEdgePad)
        val vEnd = Offset(cx, size.height - edgePad)
        drawLine(
            color = guidelineColor.copy(alpha = vAlpha),
            start = vStart,
            end = vEnd,
            strokeWidth = strokePx,
            cap = StrokeCap.Round
        )
        drawArrowhead(vStart, Offset(0f, -1f), headLen, headAngleDeg, guidelineColor.copy(alpha = vAlpha), strokePx)
        drawArrowhead(vEnd,   Offset(0f,  1f), headLen, headAngleDeg, guidelineColor.copy(alpha = vAlpha), strokePx)
    }
}

/**
 * 화살촉을 그립니다.
 *
 * [tip]에서 [direction] 방향으로 뻗어나가는 화살의 끝에 두 개의 날개선을 그립니다.
 *
 * @param tip 화살촉 끝점
 * @param direction 화살 진행 방향 단위 벡터 (예: 오른쪽 → Offset(1f, 0f))
 * @param headLen 날개선 길이 (px)
 * @param angleDeg 날개선 각도 (°). 작을수록 뾰족, 클수록 넓음
 * @param color 선 색상
 * @param strokeWidth 선 두께 (px)
 */
private fun DrawScope.drawArrowhead(
    tip: Offset,
    direction: Offset,
    headLen: Float,
    angleDeg: Float,
    color: Color,
    strokeWidth: Float
) {
    val rad = Math.toRadians(angleDeg.toDouble())
    val cosA = cos(rad).toFloat()
    val sinA = sin(rad).toFloat()
    val dx = direction.x
    val dy = direction.y

    // direction을 ±angleDeg 회전한 단위 벡터
    val wing1 = Offset(dx * cosA - dy * sinA, dx * sinA + dy * cosA)
    val wing2 = Offset(dx * cosA + dy * sinA, -dx * sinA + dy * cosA)

    // 화살촉 끝에서 날개 방향 반대로 선을 그림
    drawLine(color, tip, tip - wing1 * headLen, strokeWidth, StrokeCap.Round)
    drawLine(color, tip, tip - wing2 * headLen, strokeWidth, StrokeCap.Round)
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFF1A1A1A,
    widthDp = 200,
    heightDp = 360
)
@Composable
private fun RightAngleGuidelineHorizontalPreview() {
    Box(
        modifier = Modifier
            .size(200.dp, 360.dp)
            .background(Color(0xFF1A1A1A))
    ) {
        RightAngleGuideline(
            isVisible = true,
            axis = RightAngleAxis.HORIZONTAL,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFF1A1A1A,
    widthDp = 200,
    heightDp = 360
)
@Composable
private fun RightAngleGuidelineVerticalPreview() {
    Box(
        modifier = Modifier
            .size(200.dp, 360.dp)
            .background(Color(0xFF1A1A1A))
    ) {
        RightAngleGuideline(
            isVisible = true,
            axis = RightAngleAxis.VERTICAL,
            modifier = Modifier.fillMaxSize()
        )
    }
}
