package com.bridgeone.app.ui.components.touchpad

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bridgeone.app.ui.common.EdgeSwipeConstants
import com.bridgeone.app.ui.common.swipe.LocalSwipeFocused
import com.bridgeone.app.ui.common.swipe.SwipeFocusable

/** 포커스된 드롭 슬롯을 강조하는 선의 두께 (dp). 기본값: 3f */
private const val DROP_LINE_THICKNESS_DP = 3f

/**
 * SWIPE 이동 모드의 드롭 슬롯 오버레이 (Phase 4.7.x).
 *
 * picked 존이 정해진 뒤, 모든(비활성 제외) 엣지의 삽입 슬롯(존 사이 경계 + 양 끝)마다
 * [SwipeFocusable]을 배치한다. 슬롯 포커스 후 탭 → 그 위치로 이동 확정.
 * 같은 엣지로 옮길 때는 picked를 제외한 리스트 기준으로 슬롯을 계산한다.
 * [isValidSlot]이 false인 슬롯(가득 참/너무 작아짐)은 등록하지 않아 포커스 불가.
 *
 * @param showMarker false면 슬롯 라인 마커를 그리지 않음(포커스 hit 영역만 유지). 들림 고스트가 위치를 대신 표시하는 SWIPE 추적 흐름에서 사용.
 * @param onSlotDrop (edge, insertIndex) 드롭 확정 콜백
 */
@Composable
internal fun ZoneCanvasDropOverlay(
    workConfig: EdgeZoneConfig,
    picked: ZoneKey,
    disabledEdges: Map<EntryEdge, String>,
    canvasWidth: Dp,
    canvasHeight: Dp,
    isValidSlot: (edge: EntryEdge, insertIndex: Int) -> Boolean,
    onSlotDrop: (edge: EntryEdge, insertIndex: Int) -> Unit,
    showMarker: Boolean = true,
    hasBottomLeft: Boolean = false,
    hasBottomRight: Boolean = false,
    blockedRatio: Float = EdgeSwipeConstants.CORNER_BUTTON_BLOCKED_RATIO,
) {
    val edgeDp = EdgeSwipeConstants.EDGE_HIT_WIDTH_DP.dp
    val handleDp = EdgeSwipeConstants.ZONE_BOUNDARY_DRAG_HIT_DP.dp
    val accent = CanvasModeKind.MOVE.accentColor()

    EntryEdge.entries.forEach { edge ->
        if (edge in disabledEdges.keys) return@forEach
        // 같은 엣지면 picked 제외 후 슬롯 계산
        val effZones = workConfig.zonesFor(edge).let { list ->
            if (picked.edge == edge) list.filterNot { it.startRatio == picked.startRatio } else list
        }
        for (insertIndex in 0..effZones.size) {
            if (!isValidSlot(edge, insertIndex)) continue
            val rawRatio = when (insertIndex) {
                0 -> 0f
                effZones.size -> 1f
                else -> effZones[insertIndex].startRatio
            }
            // 슬롯 위치를 코너 버튼 차단 제외 유효 영역으로 매핑(캔버스 존 배치와 정합).
            val ratio = mapToValid(edge, rawRatio, hasBottomLeft, hasBottomRight, blockedRatio)
            val offsetX: Dp
            val offsetY: Dp
            val width: Dp
            val height: Dp
            when (edge) {
                EntryEdge.TOP -> {
                    offsetX = canvasWidth * ratio - handleDp / 2; offsetY = 0.dp
                    width = handleDp; height = edgeDp
                }
                EntryEdge.BOTTOM -> {
                    offsetX = canvasWidth * ratio - handleDp / 2; offsetY = canvasHeight - edgeDp
                    width = handleDp; height = edgeDp
                }
                EntryEdge.LEFT -> {
                    offsetX = 0.dp; offsetY = canvasHeight * ratio - handleDp / 2
                    width = edgeDp; height = handleDp
                }
                EntryEdge.RIGHT -> {
                    offsetX = canvasWidth - edgeDp; offsetY = canvasHeight * ratio - handleDp / 2
                    width = edgeDp; height = handleDp
                }
            }
            val vertical = edge == EntryEdge.LEFT || edge == EntryEdge.RIGHT
            SwipeFocusable(
                element = EdgeEditorElement.CanvasDropSlot(edge, insertIndex),
                shape = RoundedCornerShape(4.dp),
                showBorderHighlight = false,
                onActivate = { onSlotDrop(edge, insertIndex) },
                modifier = Modifier
                    .offset(x = offsetX, y = offsetY)
                    .size(width = width, height = height),
            ) {
                // showMarker=false면 라인 미표시(포커스 hit 영역만 유지) — 들림 고스트가 위치를 대신 표시.
                if (showMarker) {
                    // 포커스 시 더 밝게, 비포커스 슬롯도 옅은 마커로 위치를 안내
                    val focused = LocalSwipeFocused.current
                    val lineColor = if (focused) lerp(accent, Color.White, 0.5f) else accent.copy(alpha = 0.6f)
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Box(
                            (if (vertical) Modifier.fillMaxWidth().height(DROP_LINE_THICKNESS_DP.dp)
                             else Modifier.fillMaxHeight().width(DROP_LINE_THICKNESS_DP.dp))
                                .background(lineColor)
                        )
                    }
                }
            }
        }
    }
}
