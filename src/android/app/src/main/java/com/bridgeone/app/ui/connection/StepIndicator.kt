package com.bridgeone.app.ui.connection

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bridgeone.app.ui.theme.BridgeOneTheme
import com.bridgeone.app.ui.theme.PretendardFontFamily

private val ActiveColor = Color(0xFFFF9800)
private val InactiveColor = Color(0xFF404040)
private val InactiveTextColor = Color(0xFF555555)

private val StepLabels = listOf("USB 연결", "서버 탐색", "준비 완료")

/** 각 라벨/점 영역의 폭 */
private val StepItemWidth = 64.dp
/** 라벨 사이 간격 (= 점선이 차지하는 가로 폭) */
private val LineGapWidth = 28.dp

/**
 * 연결 단계 스텝 인디케이터.
 *
 * 라벨 행과 점+점선 행을 분리하여 점선이 점과 점 사이에 정확히 위치합니다.
 * 현재 활성 단계의 점선 위를 펄스 점이 흘러갑니다.
 *
 * @param currentStep 현재 단계 (1~3)
 */
@Composable
fun StepIndicator(
    currentStep: Int,
    modifier: Modifier = Modifier
) {
    val step = currentStep.coerceIn(1, 3)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.semantics { contentDescription = "연결 단계 $step/3" }
    ) {
        // ── 라벨 행 ──
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Bottom
        ) {
            for (i in 0..2) {
                StepLabel(
                    label = StepLabels[i],
                    isActive = step >= i + 1,
                    isCurrent = step == i + 1,
                    modifier = Modifier.width(StepItemWidth)
                )
                if (i < 2) {
                    Spacer(modifier = Modifier.width(LineGapWidth))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── 점 + 점선 행 (단일 Canvas) ──
        DotsAndLines(currentStep = step)
    }
}

/**
 * 개별 스텝 라벨.
 */
@Composable
private fun StepLabel(
    label: String,
    isActive: Boolean,
    isCurrent: Boolean,
    modifier: Modifier = Modifier
) {
    val textColor by animateColorAsState(
        targetValue = when {
            isCurrent -> ActiveColor
            isActive -> Color(0xFFC2C2C2)
            else -> InactiveTextColor
        },
        animationSpec = tween(300),
        label = "textColor"
    )
    val fontSize by animateFloatAsState(
        targetValue = if (isCurrent) 13f else 11f,
        animationSpec = tween(300),
        label = "fontSize"
    )
    val fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal

    Text(
        text = label,
        fontFamily = PretendardFontFamily,
        fontWeight = fontWeight,
        fontSize = fontSize.sp,
        color = textColor,
        textAlign = TextAlign.Center,
        maxLines = 1,
        modifier = modifier
    )
}

/**
 * 점 3개 + 점선 2개 + 펄스를 하나의 Canvas로 렌더링.
 *
 * 점은 각 라벨 중앙 하단에 정렬되고, 점선은 점과 점 사이를 정확히 연결합니다.
 */
@Composable
private fun DotsAndLines(
    currentStep: Int,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val step = currentStep.coerceIn(1, 3)

    // 점 색상 애니메이션
    val dotColors = arrayOf(
        animateColorAsState(if (step >= 1) ActiveColor else InactiveColor, tween(300), label = "d1").value,
        animateColorAsState(if (step >= 2) ActiveColor else InactiveColor, tween(300), label = "d2").value,
        animateColorAsState(if (step >= 3) ActiveColor else InactiveColor, tween(300), label = "d3").value
    )

    // 점선 색상 애니메이션
    val lineColors = arrayOf(
        animateColorAsState(if (step >= 2) ActiveColor else InactiveColor, tween(300), label = "l1").value,
        animateColorAsState(if (step >= 3) ActiveColor else InactiveColor, tween(300), label = "l2").value
    )

    // 점 반지름 (현재 단계 = 더 큼)
    val dotRadii = arrayOf(
        animateFloatAsState(if (step == 1) 5f else 4f, tween(300), label = "r1").value,
        animateFloatAsState(if (step == 2) 5f else 4f, tween(300), label = "r2").value,
        animateFloatAsState(if (step == 3) 5f else 4f, tween(300), label = "r3").value
    )

    // 펄스 애니메이션 (0→1 반복)
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "pulseProgress"
    )

    val totalWidth = StepItemWidth * 3 + LineGapWidth * 2

    Canvas(
        modifier = modifier
            .width(totalWidth)
            .height(14.dp)
    ) {
        val itemW = with(density) { StepItemWidth.toPx() }
        val gapW = with(density) { LineGapWidth.toPx() }
        val centerY = size.height / 2f

        // 점 중심 X좌표 (각 라벨 영역의 중앙)
        val dotX = floatArrayOf(
            itemW / 2f,
            itemW + gapW + itemW / 2f,
            (itemW + gapW) * 2f + itemW / 2f
        )

        // 점 반지름 (dp → px)
        val radii = floatArrayOf(
            with(density) { dotRadii[0].dp.toPx() },
            with(density) { dotRadii[1].dp.toPx() },
            with(density) { dotRadii[2].dp.toPx() }
        )

        val dashLen = with(density) { 4.dp.toPx() }
        val gapLen = with(density) { 3.dp.toPx() }
        val strokeW = with(density) { 1.dp.toPx() }
        val dotPad = with(density) { 4.dp.toPx() }

        // ── 점선 + 펄스 ──
        for (i in 0..1) {
            val startX = dotX[i] + radii[i] + dotPad
            val endX = dotX[i + 1] - radii[i + 1] - dotPad

            // 점선
            drawLine(
                color = lineColors[i],
                start = Offset(startX, centerY),
                end = Offset(endX, centerY),
                strokeWidth = strokeW,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashLen, gapLen))
            )

            // 현재 단계 → 다음 단계로 흐르는 펄스
            if (step == i + 1) {
                val lineLen = endX - startX
                val px = startX + lineLen * pulseProgress
                val alpha = when {
                    pulseProgress < 0.12f -> pulseProgress / 0.12f
                    pulseProgress > 0.88f -> (1f - pulseProgress) / 0.12f
                    else -> 1f
                }
                // 글로우
                drawCircle(
                    color = ActiveColor.copy(alpha = alpha * 0.25f),
                    radius = with(density) { 6.dp.toPx() },
                    center = Offset(px, centerY)
                )
                // 코어 점
                drawCircle(
                    color = ActiveColor.copy(alpha = alpha * 0.85f),
                    radius = with(density) { 2.5.dp.toPx() },
                    center = Offset(px, centerY)
                )
            }
        }

        // ── 점 (선 위에 그려서 겹침 방지) ──
        for (i in 0..2) {
            drawCircle(
                color = dotColors[i],
                radius = radii[i],
                center = Offset(dotX[i], centerY)
            )
        }
    }
}

// ============================================================
// Preview
// ============================================================

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun StepIndicatorStep1Preview() {
    BridgeOneTheme {
        StepIndicator(currentStep = 1)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun StepIndicatorStep2Preview() {
    BridgeOneTheme {
        StepIndicator(currentStep = 2)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun StepIndicatorStep3Preview() {
    BridgeOneTheme {
        StepIndicator(currentStep = 3)
    }
}
