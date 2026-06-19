package com.bridgeone.app.ui.components.touchpad

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.bridgeone.app.ui.common.CurveEditorConstants
import com.bridgeone.app.ui.common.CurveNode

// ─────────────────────────────────────────────────────────────
// 그래프 캔버스 (순수 렌더 전용, Phase 4.5.18.4: pointerInput 제거)
// (Phase 4.7.6-A: DynamicsCurveEditor.kt에서 분리)
// ─────────────────────────────────────────────────────────────

@Composable
internal fun CurveGraphCanvas(
    activeCurve: List<CurveNode>,
    inactiveCurve: List<CurveNode>,
    activeColor: Color,
    inactiveColor: Color,
    selectedNodeIndex: Int = -1,
    isSelectMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    val padLeft = with(density) { 36.dp.toPx() }
    val padBottom = with(density) { 32.dp.toPx() }
    val padTop = with(density) { 32.dp.toPx() }
    val padRight = with(density) { 36.dp.toPx() }

    fun plotWidth(w: Float) = w - padLeft - padRight
    fun plotHeight(h: Float) = h - padTop - padBottom

    fun velocityToX(v: Float, w: Float) =
        padLeft + (v / CurveEditorConstants.CURVE_VELOCITY_MAX) * plotWidth(w)

    val multRange = CurveEditorConstants.CURVE_MULTIPLIER_MAX - CurveEditorConstants.CURVE_MULTIPLIER_MIN

    fun multiplierToY(m: Float, h: Float) =
        padTop + (1f - (m - CurveEditorConstants.CURVE_MULTIPLIER_MIN) / multRange) * plotHeight(h)

    fun nodeCanvasOffset(node: CurveNode, w: Float, h: Float) =
        Offset(velocityToX(node.velocityDpMs, w), multiplierToY(node.multiplier, h))

    val nodeRadius = with(density) { 8.dp.toPx() }
    val selectedRadius = with(density) { if (isSelectMode) 14.dp.toPx() else 12.dp.toPx() }
    val ringStroke = with(density) { if (isSelectMode) 2.5.dp.toPx() else 1.5.dp.toPx() }
    val labelStyle = TextStyle(color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Medium)

    // C2: 선택 노드 반지름 pop-in 애니메이션 (selectedNodeIndex 변경 시 8dp → 12dp 부드럽게)
    val nodeRadiusAnim = remember(selectedNodeIndex) { Animatable(nodeRadius) }
    LaunchedEffect(selectedNodeIndex) {
        if (selectedNodeIndex >= 0) {
            nodeRadiusAnim.animateTo(selectedRadius, tween(180, easing = FastOutSlowInEasing))
        }
    }

    Canvas(
        modifier = modifier
            .background(SURFACE, RoundedCornerShape(8.dp))
            .border(1.5.dp, activeColor, RoundedCornerShape(8.dp))
    ) {
        val w = size.width
        val h = size.height

        drawGrid(w, h, padLeft, padRight, padTop, padBottom)

        drawCurve(inactiveCurve, w, h, inactiveColor.copy(alpha = 0.25f), ::velocityToX, ::multiplierToY)
        drawCurve(activeCurve, w, h, activeColor, ::velocityToX, ::multiplierToY)

        activeCurve.forEachIndexed { i, node ->
            val p = nodeCanvasOffset(node, w, h)
            val isFixed = i == 0 || i == activeCurve.lastIndex
            val isSelected = i == selectedNodeIndex
            val animRadius = if (isSelected) nodeRadiusAnim.value else nodeRadius

            // 채워진 원 + 속빈 내부 (고정=LABEL_COLOR, 가변=activeColor)
            drawCircle(color = if (isFixed) LABEL_COLOR else activeColor, radius = animRadius, center = p)
            drawCircle(color = BG, radius = animRadius - with(density) { 2.dp.toPx() }, center = p)
            // A5 + C2: 선택 노드 — 활성 탭 색 ring + 좌표 라벨
            if (isSelected) {
                drawCircle(
                    color = activeColor,
                    radius = animRadius,
                    center = p,
                    style = Stroke(width = ringStroke)
                )
                val label = "(%.1f, %.2f×)".format(node.velocityDpMs, node.multiplier)
                val measured = textMeasurer.measure(label, labelStyle)
                val labelX = (p.x - measured.size.width / 2f).coerceIn(0f, w - measured.size.width)
                val labelY = if (p.y - animRadius - measured.size.height - 4f >= padTop) {
                    p.y - animRadius - measured.size.height - 4f
                } else {
                    p.y + animRadius + 4f
                }
                drawText(measured, topLeft = Offset(labelX, labelY))
            }
        }

        drawAxisLabels(w, h, padLeft, padRight, padTop, padBottom, textMeasurer)
    }
}

private fun DrawScope.drawGrid(
    w: Float, h: Float,
    padL: Float, padR: Float, padT: Float, padB: Float
) {
    val plotW = w - padL - padR
    val plotH = h - padT - padB
    val gridCols = 6
    val gridRows = 5

    for (i in 0..gridCols) {
        val x = padL + i * plotW / gridCols
        drawLine(GRID_COLOR, Offset(x, padT), Offset(x, h - padB), strokeWidth = 1f)
    }
    for (i in 0..gridRows) {
        val y = padT + i * plotH / gridRows
        drawLine(GRID_COLOR, Offset(padL, y), Offset(w - padR, y), strokeWidth = 1f)
    }
}

private fun DrawScope.drawCurve(
    curve: List<CurveNode>,
    w: Float, h: Float,
    color: Color,
    velocityToX: (Float, Float) -> Float,
    multiplierToY: (Float, Float) -> Float
) {
    if (curve.size < 2) return
    val path = Path()
    curve.forEachIndexed { i, node ->
        val x = velocityToX(node.velocityDpMs, w)
        val y = multiplierToY(node.multiplier, h)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    drawPath(path, color, style = Stroke(width = 2.5f))
}

private fun DrawScope.drawAxisLabels(
    w: Float, h: Float,
    padL: Float, padR: Float, padT: Float, padB: Float,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {
    val plotW = w - padL - padR
    val plotH = h - padT - padB
    val style = TextStyle(color = LABEL_COLOR, fontSize = 9.sp)

    for (i in 0..6) {
        val x = padL + i * plotW / 6
        val measured = textMeasurer.measure("$i", style)
        drawText(measured, topLeft = Offset(x - measured.size.width / 2f, h - padB + 4f))
    }

    val multRange = CurveEditorConstants.CURVE_MULTIPLIER_MAX - CurveEditorConstants.CURVE_MULTIPLIER_MIN
    for (i in 0..6) {
        val m = CurveEditorConstants.CURVE_MULTIPLIER_MIN + i * multRange / 6
        val y = padT + (1f - i / 6f) * plotH
        val label = "%.0f×".format(m)
        val measured = textMeasurer.measure(label, style)
        drawText(measured, topLeft = Offset(18f, y - measured.size.height / 2f))
    }
}
