package com.bridgeone.app.ui.components.touchpad

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import com.bridgeone.app.ui.theme.PretendardFontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import com.bridgeone.app.ui.common.EdgeSwipeConstants

/** 떠다니는 존 채움 투명도. 기본값: 0.85f */
private const val FLOAT_FILL_ALPHA = 0.85f
/** 떠다니는 존 그림자 높이 (dp). 기본값: 6f */
private const val FLOAT_SHADOW_DP = 6f

/** 엣지 스트립의 한 구간(존 1개분)을 along 축 비율 구간으로 표현. */
data class ZoneStrip(val edge: EntryEdge, val startRatio: Float, val endRatio: Float)

/**
 * 이동 커밋 애니메이션에서 출발→도착으로 떠다니는 선택 존.
 *
 * @param source     이동 전 위치(출발 엣지의 비율 구간)
 * @param target     이동 후 위치(도착 엣지의 비율 구간)
 * @param colorIndex 출발 엣지에서의 인덱스 — [zoneColor] 연속성 유지용
 * @param label      표시 라벨(빈 문자열이면 미표시)
 */
data class ZoneMoveFloat(
    val source: ZoneStrip,
    val target: ZoneStrip,
    val colorIndex: Int,
    val label: String,
)

/** 클리핑 적용된 스트립의 dp offset/size (ZoneCanvasHitOverlay와 동일 좌표계). */
private data class StripRect(val offX: Dp, val offY: Dp, val w: Dp, val h: Dp)

/**
 * 스트립을 dp rect로 변환하되, 캔버스 렌더러([EdgeZoneEditorPreviewCanvas]·[ZoneCanvasHitOverlay])와
 * **동일한 코너 차단 클리핑**을 적용한다. 클리핑을 빼면 코너 인접 존이 캔버스보다 크게 그려져
 * "선택 시 존이 커지는" 현상이 생긴다.
 */
private fun clippedStripDpRect(
    strip: ZoneStrip,
    canvasWidth: Dp, canvasHeight: Dp, edgeDp: Dp,
    cornerPriority: Map<CornerOverlap, EntryEdge>,
    hasBottomLeft: Boolean, hasBottomRight: Boolean,
    blockedRatio: Float,
): StripRect {
    fun cp(c: CornerOverlap) = cornerPriority[c] ?: defaultCornerEdge(c)
    // 버튼 차단은 유효 영역 매핑이 처리. 여기선 코너 겹침(edgeDp)만 추가 클리핑.
    val s = mapToValid(strip.edge, strip.startRatio, hasBottomLeft, hasBottomRight, blockedRatio)
    val e = mapToValid(strip.edge, strip.endRatio, hasBottomLeft, hasBottomRight, blockedRatio)
    return when (strip.edge) {
        EntryEdge.TOP -> {
            var l = canvasWidth * s
            var r = canvasWidth * e
            if (cp(CornerOverlap.TOP_LEFT) != EntryEdge.TOP) l = maxOf(l, edgeDp)
            if (cp(CornerOverlap.TOP_RIGHT) != EntryEdge.TOP) r = minOf(r, canvasWidth - edgeDp)
            StripRect(l, 0.dp, (r - l).coerceAtLeast(0.dp), edgeDp)
        }
        EntryEdge.BOTTOM -> {
            var l = canvasWidth * s
            var r = canvasWidth * e
            if (cp(CornerOverlap.BOTTOM_LEFT) != EntryEdge.BOTTOM) l = maxOf(l, edgeDp)
            if (cp(CornerOverlap.BOTTOM_RIGHT) != EntryEdge.BOTTOM) r = minOf(r, canvasWidth - edgeDp)
            StripRect(l, canvasHeight - edgeDp, (r - l).coerceAtLeast(0.dp), edgeDp)
        }
        EntryEdge.LEFT -> {
            var t = canvasHeight * s
            var b = canvasHeight * e
            if (cp(CornerOverlap.TOP_LEFT) != EntryEdge.LEFT) t = maxOf(t, edgeDp)
            if (cp(CornerOverlap.BOTTOM_LEFT) != EntryEdge.LEFT) b = minOf(b, canvasHeight - edgeDp)
            StripRect(0.dp, t, edgeDp, (b - t).coerceAtLeast(0.dp))
        }
        EntryEdge.RIGHT -> {
            var t = canvasHeight * s
            var b = canvasHeight * e
            if (cp(CornerOverlap.TOP_RIGHT) != EntryEdge.RIGHT) t = maxOf(t, edgeDp)
            if (cp(CornerOverlap.BOTTOM_RIGHT) != EntryEdge.RIGHT) b = minOf(b, canvasHeight - edgeDp)
            StripRect(canvasWidth - edgeDp, t, edgeDp, (b - t).coerceAtLeast(0.dp))
        }
    }
}

/**
 * 이동 커밋 시 선택 존을 출발 rect → 도착 rect로 이동(translate)·reshape하는 떠다니는 오버레이.
 *
 * 캔버스 morph(이웃 reflow)와 같은 [progress]로 구동돼 프레임 동기. 같은 엣지 이동은 폭 보존으로
 * 순수 translate, cross-edge는 cross축 두께(edgeDp)를 유지한 채 along 길이만 보간해 세로↔가로 바로
 * 자연스럽게 reshape된다.
 *
 * @param progress 0(출발)→1(도착)
 * @param lift     들어올림 정도 0(바닥)→1(완전히 떠 있음). 그림자 높이를 스케일. NORMAL 드래그 pick/settle 전용(기본 1f).
 * @param canvasWidth/[canvasHeight] 호출부 `BoxWithConstraints`의 maxWidth/maxHeight
 */
@Composable
internal fun ZoneMoveFloatingOverlay(
    float: ZoneMoveFloat,
    progress: Float,
    canvasWidth: Dp,
    canvasHeight: Dp,
    cornerPriority: Map<CornerOverlap, EntryEdge>,
    hasBottomLeft: Boolean,
    hasBottomRight: Boolean,
    lift: Float = 1f,
    blockedRatio: Float = EdgeSwipeConstants.CORNER_BUTTON_BLOCKED_RATIO,
) {
    val edgeDp = EdgeSwipeConstants.EDGE_HIT_WIDTH_DP.dp
    val src = clippedStripDpRect(float.source, canvasWidth, canvasHeight, edgeDp, cornerPriority, hasBottomLeft, hasBottomRight, blockedRatio)
    val tgt = clippedStripDpRect(float.target, canvasWidth, canvasHeight, edgeDp, cornerPriority, hasBottomLeft, hasBottomRight, blockedRatio)

    val offX = lerp(src.offX, tgt.offX, progress)
    val offY = lerp(src.offY, tgt.offY, progress)
    val boxW = lerp(src.w, tgt.w, progress)
    val boxH = lerp(src.h, tgt.h, progress)

    Box(
        modifier = Modifier
            .offset(x = offX, y = offY)
            .size(width = boxW, height = boxH)
            .shadow((FLOAT_SHADOW_DP * lift).dp, RoundedCornerShape(2.dp))
            .background(zoneColor(float.colorIndex).copy(alpha = FLOAT_FILL_ALPHA))
            .border(EdgeSwipeConstants.EDGE_ZONE_FOCUS_BORDER_DP.dp, CanvasModeKind.MOVE.accentColor()),
        contentAlignment = Alignment.Center,
    ) {
        if (float.label.isNotEmpty()) {
            // 세로로 긴 스트립이면 글자를 한 줄씩 세로 배열(캔버스 라벨과 동일 규칙).
            val verticalText = boxH >= boxW
            // style로 통째 전달해 LocalTextStyle(테마) 머지를 차단 → 캔버스 라벨(textMeasurer)과 동일한 폰트 기본 lineHeight 사용.
            Text(
                text = if (verticalText) float.label.toList().joinToString("\n") else float.label,
                style = TextStyle(
                    fontFamily = PretendardFontFamily,
                    fontSize = EdgeSwipeConstants.ZONE_LABEL_FONT_SIZE_SP.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                ),
            )
        }
    }
}
