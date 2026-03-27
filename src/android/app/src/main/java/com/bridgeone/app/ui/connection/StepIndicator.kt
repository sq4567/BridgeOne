package com.bridgeone.app.ui.connection

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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

/**
 * 연결 단계 스텝 인디케이터.
 *
 * 3개의 점 위에 단계 라벨이 표시되며, 현재 단계의 라벨이 커지고 강조됩니다.
 *
 * @param currentStep 현재 단계 (1~3)
 */
@Composable
fun StepIndicator(
    currentStep: Int,
    modifier: Modifier = Modifier
) {
    val step = currentStep.coerceIn(1, 3)

    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .semantics { contentDescription = "연결 단계 $step/3" }
    ) {
        // 스텝 1
        StepItem(
            label = StepLabels[0],
            isActive = step >= 1,
            isCurrent = step == 1
        )

        // 연결선 1
        StepLine(modifier = Modifier.align(Alignment.CenterVertically))

        // 스텝 2
        StepItem(
            label = StepLabels[1],
            isActive = step >= 2,
            isCurrent = step == 2
        )

        // 연결선 2
        StepLine(modifier = Modifier.align(Alignment.CenterVertically))

        // 스텝 3
        StepItem(
            label = StepLabels[2],
            isActive = step >= 3,
            isCurrent = step == 3
        )
    }
}

/**
 * 개별 스텝 아이템 (라벨 + 점).
 */
@Composable
private fun StepItem(
    label: String,
    isActive: Boolean,
    isCurrent: Boolean
) {
    val dotColor by animateColorAsState(
        targetValue = if (isActive) ActiveColor else InactiveColor,
        animationSpec = tween(300),
        label = "dotColor"
    )
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
    val dotSize by animateDpAsState(
        targetValue = if (isCurrent) 10.dp else 8.dp,
        animationSpec = tween(300),
        label = "dotSize"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(64.dp)
    ) {
        // 라벨
        Text(
            text = label,
            fontFamily = PretendardFontFamily,
            fontWeight = fontWeight,
            fontSize = fontSize.sp,
            color = textColor,
            textAlign = TextAlign.Center,
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(6.dp))

        // 점
        val density = LocalDensity.current
        Canvas(
            modifier = Modifier
                .width(dotSize)
                .height(dotSize)
        ) {
            drawCircle(
                color = dotColor,
                radius = size.minDimension / 2f
            )
        }
    }
}

/**
 * 스텝 간 연결선.
 */
@Composable
private fun StepLine(modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    Canvas(
        modifier = modifier
            .width(28.dp)
            .height(1.dp)
    ) {
        drawLine(
            color = InactiveColor,
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = with(density) { 1.dp.toPx() }
        )
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
