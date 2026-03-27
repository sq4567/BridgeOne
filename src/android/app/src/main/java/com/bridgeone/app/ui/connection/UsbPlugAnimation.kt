package com.bridgeone.app.ui.connection

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bridgeone.app.ui.theme.StateWarning

/**
 * USB 케이블을 핸드폰에 꽂는 애니메이션.
 *
 * 핸드폰 하단 실루엣과 USB-C 커넥터가 표시됩니다.
 * 커넥터가 아래에서 올라와 포트에 꽂히고, 연결 글로우 후 다시 빠지는 루프입니다.
 *
 * @param isAnimating true면 루프 애니메이션, false면 커넥터가 꽂힌 정지 상태
 * @param accentColor 연결 글로우 색상
 */
@Composable
fun UsbPlugAnimation(
    isAnimating: Boolean,
    modifier: Modifier = Modifier,
    accentColor: Color = StateWarning
) {
    val infiniteTransition = rememberInfiniteTransition(label = "usbPlug")
    val rawProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing)
        ),
        label = "plugProgress"
    )

    // 애니메이션 중이 아니면 꽂힌 상태(0.55f)에 고정
    val progress = if (isAnimating) rawProgress else 0.55f

    Canvas(
        modifier = modifier
            .size(width = 140.dp, height = 220.dp)
            .clearAndSetSemantics { }
    ) {
        // ── 치수 계산 ──
        val phoneW = 80.dp.toPx()
        val phoneH = 150.dp.toPx()
        val phoneCorner = 14.dp.toPx()
        val phoneLeft = (size.width - phoneW) / 2f
        val phoneTop = 8.dp.toPx()

        val strokeW = 1.5.dp.toPx()
        val outlineColor = Color.White.copy(alpha = 0.15f)

        // USB 포트 (폰 하단 중앙)
        val portW = 16.dp.toPx()
        val portH = 4.dp.toPx()
        val portLeft = (size.width - portW) / 2f
        val portTop = phoneTop + phoneH - 1.dp.toPx()

        // USB-C 커넥터
        val connW = 11.dp.toPx()
        val connH = 14.dp.toPx()
        val connLeft = (size.width - connW) / 2f
        val connCorner = 2.5.dp.toPx()

        // 케이블
        val cableW = 1.5.dp.toPx()

        // ── 커넥터 Y 위치 계산 ──
        val startY = size.height + 10.dp.toPx()           // 캔버스 아래 (보이지 않음)
        val approachY = portTop + portH + 4.dp.toPx()     // 포트 바로 아래 (접근)
        val pluggedY = portTop - connH + portH + 2.dp.toPx() // 포트에 삽입된 위치

        // ── 애니메이션 페이즈 ──
        val connTopY: Float
        val glowAlpha: Float

        when {
            // Phase 1: 하단 대기
            progress < 0.05f -> {
                connTopY = startY
                glowAlpha = 0f
            }
            // Phase 2: 상승 (빠르게 출발 → 포트 근처에서 감속)
            progress < 0.38f -> {
                val t = (progress - 0.05f) / 0.33f
                val e = FastOutSlowInEasing.transform(t)
                connTopY = startY + (approachY - startY) * e
                glowAlpha = 0f
            }
            // Phase 3: 포트에 꽂히는 순간 (짧고 빠르게)
            progress < 0.44f -> {
                val t = (progress - 0.38f) / 0.06f
                connTopY = approachY + (pluggedY - approachY) * t
                glowAlpha = t * 0.35f
            }
            // Phase 4: 꽂힌 상태 유지 + 글로우 페이드
            progress < 0.72f -> {
                connTopY = pluggedY
                val t = (progress - 0.44f) / 0.28f
                glowAlpha = 0.35f * (1f - t * 0.8f) // 0.35 → 0.07 서서히 감소
            }
            // Phase 5: 빠져나옴 (느리게 출발 → 빠르게 퇴장)
            progress < 0.95f -> {
                val t = (progress - 0.72f) / 0.23f
                val e = FastOutSlowInEasing.transform(t)
                connTopY = pluggedY + (startY - pluggedY) * e
                glowAlpha = 0f
            }
            // Phase 6: 하단 대기
            else -> {
                connTopY = startY
                glowAlpha = 0f
            }
        }

        // ── 렌더링 (외곽선만) ──
        // 그리기 순서: 글로우 → 폰 채움(가림) → 폰 외곽선 → 포트
        // 이렇게 하면 글로우가 폰 내부로 번지지 않습니다.

        // 1. 연결 글로우 (폰 채움보다 먼저 그려서 폰 뒤로 가림)
        if (glowAlpha > 0.01f) {
            val glowCenter = Offset(size.width / 2f, portTop + portH / 2f)
            val glowRadius = 40.dp.toPx()
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accentColor.copy(alpha = glowAlpha),
                        accentColor.copy(alpha = glowAlpha * 0.3f),
                        Color.Transparent
                    ),
                    center = glowCenter,
                    radius = glowRadius
                ),
                center = glowCenter,
                radius = glowRadius
            )
        }

        // 2. 폰 내부 채움 (배경색으로 커넥터·글로우 가림)
        drawRoundRect(
            color = Color(0xFF121212),
            topLeft = Offset(phoneLeft, phoneTop),
            size = Size(phoneW, phoneH),
            cornerRadius = CornerRadius(phoneCorner)
        )

        // 3. 폰 외곽선
        drawRoundRect(
            color = outlineColor,
            topLeft = Offset(phoneLeft, phoneTop),
            size = Size(phoneW, phoneH),
            cornerRadius = CornerRadius(phoneCorner),
            style = Stroke(width = strokeW)
        )

        // 4. USB 포트 (외곽선)
        drawRoundRect(
            color = outlineColor,
            topLeft = Offset(portLeft, portTop),
            size = Size(portW, portH),
            cornerRadius = CornerRadius(1.5.dp.toPx()),
            style = Stroke(width = strokeW)
        )

        // 4. 케이블 (커넥터 하단 → 캔버스 하단)
        val cableStartY = connTopY + connH
        if (cableStartY < size.height) {
            drawLine(
                color = outlineColor,
                start = Offset(size.width / 2f, cableStartY),
                end = Offset(size.width / 2f, size.height + 20.dp.toPx()),
                strokeWidth = cableW,
                cap = StrokeCap.Round
            )
        }

        // 5. 커넥터 (외곽선)
        if (connTopY < size.height) {
            drawRoundRect(
                color = outlineColor,
                topLeft = Offset(connLeft, connTopY),
                size = Size(connW, connH),
                cornerRadius = CornerRadius(connCorner),
                style = Stroke(width = strokeW)
            )
        }
    }
}

// ============================================================
// Preview
// ============================================================

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun UsbPlugAnimationPreview() {
    UsbPlugAnimation(isAnimating = true)
}

@Preview(showBackground = true, backgroundColor = 0xFF121212, name = "정지 (꽂힌 상태)")
@Composable
private fun UsbPlugAnimationStaticPreview() {
    UsbPlugAnimation(isAnimating = false)
}
