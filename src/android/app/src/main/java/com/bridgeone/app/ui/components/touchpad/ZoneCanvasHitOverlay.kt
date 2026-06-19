package com.bridgeone.app.ui.components.touchpad

import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bridgeone.app.ui.common.EdgeSwipeConstants
import com.bridgeone.app.ui.common.swipe.SwipeFocusable

/**
 * SWIPE 모드 캔버스 hit 영역 오버레이 (Phase 4.7.5-D, EdgeZoneEditorScreen에서 추출).
 *
 * 존 단위로 분해해 각 존에 포커스 가능한 [SwipeFocusable] 히트 영역을 렌더한다.
 * 비활성 엣지만 등록을 생략하고(Unassigned 존은 포함), 코너는 별도 hit 영역으로 제공하지 않는다.
 * NORMAL 모드에서는 호출하지 않으며(미렌더), `isEditing` 가드도 호출부가 담당한다.
 *
 * @param canvasWidth/[canvasHeight] 호출부 `BoxWithConstraints`의 `maxWidth`/`maxHeight`
 * @param onZoneSelected 존 활성화 시 호출 (selectedZone/selectedEdge 설정 + 캔버스 닫기)
 */
@Composable
internal fun ZoneCanvasHitOverlay(
    workConfig: EdgeZoneConfig,
    disabledEdges: Map<EntryEdge, String>,
    bottomLeftButtonLabel: String?,
    bottomRightButtonLabel: String?,
    canvasWidth: Dp,
    canvasHeight: Dp,
    onZoneSelected: (EdgeZone) -> Unit,
) {
    val edgeDp = EdgeSwipeConstants.EDGE_HIT_WIDTH_DP.dp
    val hasBottomLeft = bottomLeftButtonLabel != null
    val hasBottomRight = bottomRightButtonLabel != null
    val blockedRatio = EdgeSwipeConstants.CORNER_BUTTON_BLOCKED_RATIO
    fun cp(c: CornerOverlap) = workConfig.cornerPriority[c] ?: defaultCornerEdge(c)

    val selectZoneAction: (EdgeZone) -> () -> Unit = { zone ->
        {
            onZoneSelected(zone)
        }
    }

    EntryEdge.entries.forEach { edge ->
        if (edge in disabledEdges.keys) return@forEach
        val zones = workConfig.zonesFor(edge)
        zones.forEachIndexed inner@{ idx, zone ->
            // 캔버스 렌더링과 동일한 클리핑 로직을 dp 공간에서 적용
            // Unassigned 존도 포함 — 액션 지정을 위해 선택할 수 있어야 함
            val rectOffsetX: androidx.compose.ui.unit.Dp
            val rectOffsetY: androidx.compose.ui.unit.Dp
            val rectWidth: androidx.compose.ui.unit.Dp
            val rectHeight: androidx.compose.ui.unit.Dp
            when (edge) {
                EntryEdge.TOP -> {
                    var l = canvasWidth * zone.startRatio
                    var r = canvasWidth * zone.endRatio
                    if (cp(CornerOverlap.TOP_LEFT) != EntryEdge.TOP) l = maxOf(l, edgeDp)
                    if (cp(CornerOverlap.TOP_RIGHT) != EntryEdge.TOP) r = minOf(r, canvasWidth - edgeDp)
                    rectOffsetX = l; rectOffsetY = 0.dp
                    rectWidth = r - l; rectHeight = edgeDp
                }
                EntryEdge.BOTTOM -> {
                    val minX = when {
                        hasBottomLeft -> canvasWidth * blockedRatio
                        cp(CornerOverlap.BOTTOM_LEFT) != EntryEdge.BOTTOM -> edgeDp
                        else -> 0.dp
                    }
                    val maxX = when {
                        hasBottomRight -> canvasWidth * (1f - blockedRatio)
                        cp(CornerOverlap.BOTTOM_RIGHT) != EntryEdge.BOTTOM -> canvasWidth - edgeDp
                        else -> canvasWidth
                    }
                    val l = maxOf(canvasWidth * zone.startRatio, minX)
                    val r = minOf(canvasWidth * zone.endRatio, maxX)
                    rectOffsetX = l; rectOffsetY = canvasHeight - edgeDp
                    rectWidth = r - l; rectHeight = edgeDp
                }
                EntryEdge.LEFT -> {
                    var t = canvasHeight * zone.startRatio
                    var b = canvasHeight * zone.endRatio
                    if (cp(CornerOverlap.TOP_LEFT) != EntryEdge.LEFT) t = maxOf(t, edgeDp)
                    if (hasBottomLeft) b = minOf(b, canvasHeight * (1f - blockedRatio))
                    else if (cp(CornerOverlap.BOTTOM_LEFT) != EntryEdge.LEFT) b = minOf(b, canvasHeight - edgeDp)
                    rectOffsetX = 0.dp; rectOffsetY = t
                    rectWidth = edgeDp; rectHeight = b - t
                }
                EntryEdge.RIGHT -> {
                    var t = canvasHeight * zone.startRatio
                    var b = canvasHeight * zone.endRatio
                    if (cp(CornerOverlap.TOP_RIGHT) != EntryEdge.RIGHT) t = maxOf(t, edgeDp)
                    if (hasBottomRight) b = minOf(b, canvasHeight * (1f - blockedRatio))
                    else if (cp(CornerOverlap.BOTTOM_RIGHT) != EntryEdge.RIGHT) b = minOf(b, canvasHeight - edgeDp)
                    rectOffsetX = canvasWidth - edgeDp; rectOffsetY = t
                    rectWidth = edgeDp; rectHeight = b - t
                }
            }
            if (rectWidth <= 0.dp || rectHeight <= 0.dp) return@inner

            val gridRow = when (edge) {
                EntryEdge.TOP -> 10
                EntryEdge.LEFT, EntryEdge.RIGHT -> 11 + idx
                EntryEdge.BOTTOM -> 50
            }

            SwipeFocusable(
                element = EdgeEditorElement.CanvasZone(edge, idx),
                shape = RoundedCornerShape(4.dp),
                showBorderHighlight = true,
                onActivate = selectZoneAction(zone),
                gridRow = gridRow,
                modifier = Modifier
                    .offset(x = rectOffsetX, y = rectOffsetY)
                    .size(width = rectWidth, height = rectHeight),
            ) {}
        }
    }
}
