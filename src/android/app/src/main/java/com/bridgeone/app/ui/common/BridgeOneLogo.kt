package com.bridgeone.app.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.min

/** 별 모양 SVG Path (viewBox: 0 0 669.6 669.6, 중심 334.8, 240) */
const val STAR_PATH_DATA =
    "M334.8,465h0c0-124.3-100.7-225-225-225h0c124.3,0,225-100.7,225-225h0" +
    "c0,124.3,100.7,225,225,225h0c-124.3,0-225,100.7-225,225Z"

private const val VIEWBOX_SIZE = 669.6f
private const val STAR_CENTER_X = 334.8f
private const val STAR_CENTER_Y = 240f

/**
 * BridgeOne 별 로고 Composable.
 *
 * 스플래시에서 사용하는 4꼭짓점 별을 재사용하여 브랜드 연속성을 유지합니다.
 * 접근성 트리에서 제외됩니다 (장식적 요소).
 */
@Composable
fun BridgeOneLogo(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    color: Color = Color(0xFF2AA9FF),
    alpha: Float = 0.6f
) {
    val starPath = remember { PathParser().parsePathString(STAR_PATH_DATA).toPath() }

    Canvas(
        modifier = modifier
            .size(size)
            .clearAndSetSemantics { }
    ) {
        if (this.size.width <= 0f || this.size.height <= 0f) return@Canvas

        val canvasMin = min(this.size.width, this.size.height)
        val scale = canvasMin / VIEWBOX_SIZE

        // 별을 캔버스 중앙에 배치
        val offsetX = (this.size.width - VIEWBOX_SIZE * scale) / 2f
        val offsetY = (this.size.height - VIEWBOX_SIZE * scale) / 2f +
            (VIEWBOX_SIZE / 2f - STAR_CENTER_Y) * scale // 별 중심 보정

        withTransform({
            translate(offsetX, offsetY)
            scale(scale, scale, androidx.compose.ui.geometry.Offset.Zero)
        }) {
            drawPath(
                path = starPath,
                color = color.copy(alpha = alpha)
            )
        }
    }
}
