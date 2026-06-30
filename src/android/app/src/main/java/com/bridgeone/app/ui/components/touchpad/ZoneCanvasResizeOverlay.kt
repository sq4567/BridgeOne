package com.bridgeone.app.ui.components.touchpad

import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bridgeone.app.ui.common.EdgeSwipeConstants
import com.bridgeone.app.ui.common.swipe.LocalSwipeFocusController
import com.bridgeone.app.ui.common.swipe.LocalSwipeFocused
import com.bridgeone.app.ui.common.swipe.ManipulationAxis
import com.bridgeone.app.ui.common.swipe.SwipeFocusable
import com.bridgeone.app.ui.common.swipe.SwipeMode


/**
 * SWIPE 비율 조정 모드의 캔버스 경계 오버레이 (Phase 4.7.x).
 *
 * 각 엣지의 내부 존 경계마다 manipulatable [SwipeFocusable]을 배치한다.
 * 경계 포커스 후 탭 → MANIPULATION 진입, 스와이프(가로 성분)로 인접 두 존 비율을 조정한다.
 * (manipulate은 가로 delta만 전달하므로 세로 엣지도 가로 스와이프 기준으로 조정 — EdgeStripEditor와 동일 감도)
 *
 * @param onAdjust (edge, leftIndex, newRatio) 경계 이동 콜백. MIN_ZONE_RATIO clamp는 호출부(state)가 담당.
 */
@Composable
internal fun ZoneCanvasResizeOverlay(
    workConfig: EdgeZoneConfig,
    disabledEdges: Map<EntryEdge, String>,
    canvasWidth: Dp,
    canvasHeight: Dp,
    onAdjust: (edge: EntryEdge, leftIndex: Int, newRatio: Float) -> Unit,
    hasBottomLeft: Boolean = false,
    hasBottomRight: Boolean = false,
    blockedRatio: Float = EdgeSwipeConstants.CORNER_BUTTON_BLOCKED_RATIO,
) {
    val edgeDp = EdgeSwipeConstants.EDGE_HIT_WIDTH_DP.dp
    val handleDp = EdgeSwipeConstants.ZONE_BOUNDARY_DRAG_HIT_DP.dp

    EntryEdge.entries.forEach { edge ->
        if (edge in disabledEdges.keys) return@forEach
        val zones = workConfig.zonesFor(edge)
        // 경계 delta(화면 비율)를 존 비율로 환산할 유효 구간 폭(매핑으로 좁아진 만큼 감도 보정).
        val (vs, ve) = edgeValidRange(edge, hasBottomLeft, hasBottomRight, blockedRatio)
        val validSpan = (ve - vs).coerceAtLeast(0.0001f)
        for (i in 1 until zones.size) {
            val boundaryRatio = zones[i].startRatio
            val mapped = mapToValid(edge, boundaryRatio, hasBottomLeft, hasBottomRight, blockedRatio)
            val leftIndex = i - 1
            val (offsetX, offsetY, width, height) = edgeHandleRect(edge, canvasWidth, canvasHeight, edgeDp, handleDp, mapped)
            val vertical = edge == EntryEdge.LEFT || edge == EntryEdge.RIGHT
            SwipeFocusable(
                element = EdgeEditorElement.CanvasBoundary(edge, leftIndex),
                shape = RoundedCornerShape(EdgeSwipeConstants.CANVAS_OVERLAY_HANDLE_CORNER_DP.dp),
                manipulatable = true,
                // 세로 엣지(좌/우)는 위아래 스와이프, 가로 엣지(상/하)는 좌우 스와이프로 경계 이동
                manipulationAxis = if (vertical) ManipulationAxis.VERTICAL else ManipulationAxis.HORIZONTAL,
                // 박스 하이라이트 대신 포커스 시 경계선만 파랗게 강조
                showBorderHighlight = false,
                onManipulate = { deltaPx, screenWidthPx ->
                    // 화면 비율 delta를 유효 구간 폭으로 나눠 존 비율 delta로 환산(유효 영역이 좁아진 만큼 감도 보정).
                    val deltaRatio = (deltaPx / screenWidthPx) / validSpan
                    onAdjust(edge, leftIndex, boundaryRatio + deltaRatio)
                },
                modifier = Modifier
                    .offset(x = offsetX, y = offsetY)
                    .size(width = width, height = height),
            ) {
                if (LocalSwipeFocused.current) {
                    // 핸들 중앙(=실제 경계 위치)에 파란 선. 세로 엣지는 가로선, 가로 엣지는 세로선.
                    // 탭으로 확정(MANIPULATION 진입)해 경계를 움직일 수 있는 상태면 선을 약간 밝게 표시.
                    val manipulating = LocalSwipeFocusController.current?.mode == SwipeMode.MANIPULATION
                    val baseColor = MaterialTheme.colorScheme.primary
                    val lineColor = if (manipulating) brightenForState(baseColor, 0.45f) else baseColor
                    EdgeMarkerLine(vertical, EdgeSwipeConstants.CANVAS_OVERLAY_LINE_THICKNESS_DP.dp, lineColor)
                }
            }
        }
    }
}
