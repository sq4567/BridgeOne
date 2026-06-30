package com.bridgeone.app.ui.components.touchpad

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bridgeone.app.ui.common.EdgeSwipeConstants
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 편집기 캔버스의 좌표 계산 공용 함수 (Phase 4.7.x — EdgeZoneEditorPreviewCanvas에서 추출).
 *
 * 캔버스 렌더(`EdgeZoneEditorPreviewCanvas`), NORMAL 드래그 hit-test, 모드 오버레이의 확인 버튼 위치 계산이
 * **동일한 좌표계**를 쓰도록 한 곳에 모았다. 모두 픽셀(px) 공간 기준이며, 엣지 폭은 `edgePx`로 받는다.
 */

/**
 * 코너 버튼 차단을 제외한 엣지의 유효 along 범위 (vs, ve). 존 비율 [0,1]이 이 범위에 매핑된다.
 * 코너 겹침(edgePx) 클리핑은 별개로 [clipZoneRect]가 처리한다.
 */
internal fun edgeValidRange(
    edge: EntryEdge, hasBottomLeft: Boolean, hasBottomRight: Boolean, blockedRatio: Float
): Pair<Float, Float> = when (edge) {
    EntryEdge.LEFT   -> 0f to (if (hasBottomLeft) 1f - blockedRatio else 1f)
    EntryEdge.RIGHT  -> 0f to (if (hasBottomRight) 1f - blockedRatio else 1f)
    EntryEdge.BOTTOM -> (if (hasBottomLeft) blockedRatio else 0f) to (if (hasBottomRight) 1f - blockedRatio else 1f)
    EntryEdge.TOP    -> 0f to 1f
}

/** 순방향: 존 비율 r∈[0,1] → 유효 범위 내 raw along 비율. */
internal fun mapToValid(
    edge: EntryEdge, r: Float, hasBottomLeft: Boolean, hasBottomRight: Boolean, blockedRatio: Float
): Float {
    val (vs, ve) = edgeValidRange(edge, hasBottomLeft, hasBottomRight, blockedRatio)
    return vs + r * (ve - vs)
}

/** 역방향: raw along 비율 → 존 비율. 유효 범위 밖(=차단 영역 터치)이면 null. */
internal fun unmapFromValid(
    edge: EntryEdge, rawRatio: Float, hasBottomLeft: Boolean, hasBottomRight: Boolean, blockedRatio: Float
): Float? {
    val (vs, ve) = edgeValidRange(edge, hasBottomLeft, hasBottomRight, blockedRatio)
    if (ve - vs <= 0f || rawRatio < vs || rawRatio > ve) return null
    return ((rawRatio - vs) / (ve - vs)).coerceIn(0f, 1f)
}

/** 역방향(clamp): 유효 범위 밖이면 가장 가까운 유효 비율로 보정(드롭 위치 결정용 — null 대신 항상 값). */
internal fun unmapClamped(
    edge: EntryEdge, rawRatio: Float, hasBottomLeft: Boolean, hasBottomRight: Boolean, blockedRatio: Float
): Float {
    val (vs, ve) = edgeValidRange(edge, hasBottomLeft, hasBottomRight, blockedRatio)
    if (ve - vs <= 0f) return 0f
    return ((rawRatio.coerceIn(vs, ve) - vs) / (ve - vs)).coerceIn(0f, 1f)
}

/**
 * 존의 비클리핑 사각형 (엣지 폭 × 존 비율 구간). 경계를 정수 픽셀로 스냅해 인접 존 간 두께 불일치를 방지.
 * 존 비율은 [mapToValid]로 코너 버튼 차단을 제외한 유효 범위에 매핑된다.
 */
internal fun zoneRect(
    zone: EdgeZone, w: Float, h: Float, edgePx: Float,
    hasBottomLeft: Boolean = false, hasBottomRight: Boolean = false,
    blockedRatio: Float = EdgeSwipeConstants.CORNER_BUTTON_BLOCKED_RATIO,
): Rect {
    val s = mapToValid(zone.edge, zone.startRatio, hasBottomLeft, hasBottomRight, blockedRatio)
    val e = mapToValid(zone.edge, zone.endRatio, hasBottomLeft, hasBottomRight, blockedRatio)
    return when (zone.edge) {
        EntryEdge.LEFT   -> {
            val y0 = (s * h).roundToInt().toFloat()
            val y1 = (e * h).roundToInt().toFloat()
            Rect(Offset(0f, y0), Size(edgePx, y1 - y0))
        }
        EntryEdge.RIGHT  -> {
            val y0 = (s * h).roundToInt().toFloat()
            val y1 = (e * h).roundToInt().toFloat()
            Rect(Offset(w - edgePx, y0), Size(edgePx, y1 - y0))
        }
        EntryEdge.TOP    -> {
            val x0 = (s * w).roundToInt().toFloat()
            val x1 = (e * w).roundToInt().toFloat()
            Rect(Offset(x0, 0f), Size(x1 - x0, edgePx))
        }
        EntryEdge.BOTTOM -> {
            val x0 = (s * w).roundToInt().toFloat()
            val x1 = (e * w).roundToInt().toFloat()
            Rect(Offset(x0, h - edgePx), Size(x1 - x0, edgePx))
        }
    }
}

/**
 * 코너 겹침(edgePx) 영역으로 존 사각형을 잘라낸 표시 영역.
 * 버튼 차단(blockedRatio)은 [zoneRect]의 유효 영역 매핑이 이미 처리하므로 여기선 코너 겹침만 클리핑한다.
 */
internal fun clipZoneRect(
    raw: Rect,
    edge: EntryEdge,
    w: Float,
    h: Float,
    cornerPriority: Map<CornerOverlap, EntryEdge> = emptyMap(),
    edgePx: Float = 0f
): Rect {
    fun p(c: CornerOverlap) = cornerPriority[c] ?: defaultCornerEdge(c)
    return when (edge) {
        EntryEdge.TOP -> {
            var l = raw.left
            var r = raw.right
            if (edgePx > 0f && p(CornerOverlap.TOP_LEFT) != EntryEdge.TOP)  l = l.coerceAtLeast(edgePx)
            if (edgePx > 0f && p(CornerOverlap.TOP_RIGHT) != EntryEdge.TOP) r = r.coerceAtMost(w - edgePx)
            Rect(Offset(l, raw.top), Size((r - l).coerceAtLeast(0f), raw.height))
        }
        EntryEdge.LEFT -> {
            var t = raw.top
            var b = raw.bottom
            if (edgePx > 0f && p(CornerOverlap.TOP_LEFT) != EntryEdge.LEFT) t = t.coerceAtLeast(edgePx)
            if (edgePx > 0f && p(CornerOverlap.BOTTOM_LEFT) != EntryEdge.LEFT) b = b.coerceAtMost(h - edgePx)
            Rect(Offset(raw.left, t), Size(raw.width, (b - t).coerceAtLeast(0f)))
        }
        EntryEdge.RIGHT -> {
            var t = raw.top
            var b = raw.bottom
            if (edgePx > 0f && p(CornerOverlap.TOP_RIGHT) != EntryEdge.RIGHT) t = t.coerceAtLeast(edgePx)
            if (edgePx > 0f && p(CornerOverlap.BOTTOM_RIGHT) != EntryEdge.RIGHT) b = b.coerceAtMost(h - edgePx)
            Rect(Offset(raw.left, t), Size(raw.width, (b - t).coerceAtLeast(0f)))
        }
        EntryEdge.BOTTOM -> {
            val minX = if (edgePx > 0f && p(CornerOverlap.BOTTOM_LEFT) != EntryEdge.BOTTOM) edgePx else 0f
            val maxX = if (edgePx > 0f && p(CornerOverlap.BOTTOM_RIGHT) != EntryEdge.BOTTOM) w - edgePx else w
            val l = raw.left.coerceAtLeast(minX)
            val r = raw.right.coerceAtMost(maxX)
            Rect(Offset(l, raw.top), Size((r - l).coerceAtLeast(0f), raw.height))
        }
    }
}

// ── Dp 공간 헬퍼 (오버레이 배치용) ──

/** Dp 공간에서 엣지 위의 사각형 위치·크기 (오버레이 배치용). */
data class EdgeZoneDpRect(val offsetX: Dp, val offsetY: Dp, val width: Dp, val height: Dp)

/**
 * 핸들 사각형 Dp 위치: 경계/슬롯 ratio 지점을 중심으로 [handleDp] × [edgeDp] 사각형.
 * [ratio]는 [mapToValid]로 이미 유효 범위로 매핑된 값(0~1).
 */
internal fun edgeHandleRect(
    edge: EntryEdge,
    canvasWidth: Dp, canvasHeight: Dp,
    edgeDp: Dp, handleDp: Dp,
    ratio: Float,
): EdgeZoneDpRect = when (edge) {
    EntryEdge.TOP    -> EdgeZoneDpRect(canvasWidth * ratio - handleDp / 2, 0.dp, handleDp, edgeDp)
    EntryEdge.BOTTOM -> EdgeZoneDpRect(canvasWidth * ratio - handleDp / 2, canvasHeight - edgeDp, handleDp, edgeDp)
    EntryEdge.LEFT   -> EdgeZoneDpRect(0.dp, canvasHeight * ratio - handleDp / 2, edgeDp, handleDp)
    EntryEdge.RIGHT  -> EdgeZoneDpRect(canvasWidth - edgeDp, canvasHeight * ratio - handleDp / 2, edgeDp, handleDp)
}

/**
 * 스트립 사각형 Dp 위치: [alongStart]~[alongStart]+[alongLen] 비율 구간의 엣지 밴드.
 * 머지 선택 오버레이처럼 연속 구간 표시에 사용.
 */
internal fun edgeStripRect(
    edge: EntryEdge,
    canvasWidth: Dp, canvasHeight: Dp,
    edgeDp: Dp,
    alongStart: Float, alongLen: Float,
): EdgeZoneDpRect = when (edge) {
    EntryEdge.TOP    -> EdgeZoneDpRect(canvasWidth * alongStart, 0.dp, canvasWidth * alongLen, edgeDp)
    EntryEdge.BOTTOM -> EdgeZoneDpRect(canvasWidth * alongStart, canvasHeight - edgeDp, canvasWidth * alongLen, edgeDp)
    EntryEdge.LEFT   -> EdgeZoneDpRect(0.dp, canvasHeight * alongStart, edgeDp, canvasHeight * alongLen)
    EntryEdge.RIGHT  -> EdgeZoneDpRect(canvasWidth - edgeDp, canvasHeight * alongStart, edgeDp, canvasHeight * alongLen)
}

/** 코너 겹침 영역 탭 감지. 차단되지 않은 코너이고 두 엣지 모두 활성인 경우에만 반환. */
internal fun findCornerAt(
    pos: Offset,
    w: Float,
    h: Float,
    edgePx: Float,
    disabledEdges: Set<EntryEdge>,
    hasBottomLeft: Boolean,
    hasBottomRight: Boolean
): CornerOverlap? {
    val inLeft   = pos.x < edgePx
    val inRight  = pos.x > w - edgePx
    val inTop    = pos.y < edgePx
    val inBottom = pos.y > h - edgePx
    return when {
        inLeft && inTop    && EntryEdge.LEFT !in disabledEdges && EntryEdge.TOP    !in disabledEdges -> CornerOverlap.TOP_LEFT
        inRight && inTop   && EntryEdge.RIGHT !in disabledEdges && EntryEdge.TOP   !in disabledEdges -> CornerOverlap.TOP_RIGHT
        inLeft && inBottom && !hasBottomLeft  && EntryEdge.LEFT !in disabledEdges && EntryEdge.BOTTOM !in disabledEdges -> CornerOverlap.BOTTOM_LEFT
        inRight && inBottom && !hasBottomRight && EntryEdge.RIGHT !in disabledEdges && EntryEdge.BOTTOM !in disabledEdges -> CornerOverlap.BOTTOM_RIGHT
        else -> null
    }
}

/** 엣지 hit band 안의 포인터 위치에 해당하는 활성 존을 반환. 차단 구역이면 null. */
internal fun findZoneAt(
    pos: Offset,
    config: EdgeZoneConfig,
    w: Float,
    h: Float,
    edgePx: Float,
    disabledEdges: Set<EntryEdge> = emptySet(),
    hasBottomLeft: Boolean = false,
    hasBottomRight: Boolean = false,
    blockedRatio: Float = EdgeSwipeConstants.CORNER_BUTTON_BLOCKED_RATIO,
): EdgeZone? {
    val (edge, zoneRatio) = edgeAlongRatioAt(pos, w, h, edgePx, disabledEdges, hasBottomLeft, hasBottomRight, blockedRatio)
        ?: return null
    return EdgeZoneDetector.findActiveZone(config, edge, zoneRatio)
}

/**
 * 좌표가 속한 엣지 밴드(비활성 엣지 포함). 코너 우선순위는 무시하며 밴드 밖이면 null.
 * 비활성 엣지(예: 상단 제어 버튼) 탭을 감지해 안내하는 용도. TOP/BOTTOM을 먼저 판정해 상단 제어 버튼이 코너에서도 우선 잡히게 한다.
 */
internal fun edgeBandAt(pos: Offset, w: Float, h: Float, edgePx: Float): EntryEdge? = when {
    pos.y < edgePx     -> EntryEdge.TOP
    pos.y > h - edgePx -> EntryEdge.BOTTOM
    pos.x < edgePx     -> EntryEdge.LEFT
    pos.x > w - edgePx -> EntryEdge.RIGHT
    else               -> null
}

/** 비율 조정 모드에서 드래그할 존 경계 식별 (엣지 + 왼쪽 존 인덱스, 경계는 leftIndex와 leftIndex+1 사이). */
data class BoundaryHit(val edge: EntryEdge, val leftIndex: Int)

/** 좌표가 속한 엣지와 그 엣지의 존 비율(0~1). 엣지 hit band 밖이거나 차단 영역이면 null. */
internal fun edgeAlongRatioAt(
    pos: Offset, w: Float, h: Float, edgePx: Float,
    disabledEdges: Set<EntryEdge> = emptySet(),
    hasBottomLeft: Boolean = false, hasBottomRight: Boolean = false,
    blockedRatio: Float = EdgeSwipeConstants.CORNER_BUTTON_BLOCKED_RATIO,
): Pair<EntryEdge, Float>? {
    for (edge in EntryEdge.entries) {
        if (edge in disabledEdges) continue
        val inEdge = when (edge) {
            EntryEdge.LEFT   -> pos.x < edgePx
            EntryEdge.RIGHT  -> pos.x > w - edgePx
            EntryEdge.TOP    -> pos.y < edgePx
            EntryEdge.BOTTOM -> pos.y > h - edgePx
        }
        if (inEdge) {
            val edgeLen = if (edge == EntryEdge.LEFT || edge == EntryEdge.RIGHT) h else w
            val along = if (edge == EntryEdge.LEFT || edge == EntryEdge.RIGHT) pos.y else pos.x
            val zoneRatio = unmapFromValid(edge, along / edgeLen, hasBottomLeft, hasBottomRight, blockedRatio) ?: return null
            return edge to zoneRatio
        }
    }
    return null
}

/** 좌표 근처([thresholdPx] 이내)의 존 경계를 찾는다. 없으면 null. */
internal fun findBoundaryAt(
    pos: Offset, config: EdgeZoneConfig, w: Float, h: Float, edgePx: Float, thresholdPx: Float,
    disabledEdges: Set<EntryEdge> = emptySet(),
    hasBottomLeft: Boolean = false, hasBottomRight: Boolean = false,
    blockedRatio: Float = EdgeSwipeConstants.CORNER_BUTTON_BLOCKED_RATIO,
): BoundaryHit? {
    val (edge, ratio) = edgeAlongRatioAt(pos, w, h, edgePx, disabledEdges, hasBottomLeft, hasBottomRight, blockedRatio) ?: return null
    val zones = config.zonesFor(edge)
    if (zones.size < 2) return null
    val edgeLen = if (edge == EntryEdge.LEFT || edge == EntryEdge.RIGHT) h else w
    var bestIdx = -1
    var bestDist = Float.MAX_VALUE
    for (i in 1 until zones.size) {
        val d = abs(zones[i].startRatio - ratio) * edgeLen
        if (d < bestDist) { bestDist = d; bestIdx = i }
    }
    return if (bestIdx >= 0 && bestDist <= thresholdPx) BoundaryHit(edge, bestIdx - 1) else null
}

/**
 * 포인터를 가장 가까운 엣지로 스냅하고 그 엣지의 축 비율(0~1)을 계산한다.
 * 이동 모드의 cross-edge 드롭 위치 결정에 쓰인다 (포인터가 중앙부에 있어도 가장 가까운 엣지를 고른다).
 */
internal fun dropTargetAt(
    pos: Offset, w: Float, h: Float,
    hasBottomLeft: Boolean = false, hasBottomRight: Boolean = false,
    blockedRatio: Float = EdgeSwipeConstants.CORNER_BUTTON_BLOCKED_RATIO,
    disabledEdges: Set<EntryEdge> = emptySet(),
): DropTarget {
    val dist = mapOf(
        EntryEdge.LEFT to pos.x,
        EntryEdge.RIGHT to (w - pos.x),
        EntryEdge.TOP to pos.y,
        EntryEdge.BOTTOM to (h - pos.y),
    )
    // 비활성 엣지는 드롭 대상에서 제외(모두 비활성이면 전체에서 선택). 비활성 엣지 코너(예: TOP_LEFT) 근처에서
    // raw 거리만으로 비활성 엣지에 스냅돼 미리보기가 끊기는(애니메이션 없는) 문제 방지.
    val edge = dist.filterKeys { it !in disabledEdges }.ifEmpty { dist }.minByOrNull { it.value }!!.key
    val along = when (edge) {
        EntryEdge.LEFT, EntryEdge.RIGHT -> (pos.y / h).coerceIn(0f, 1f)
        EntryEdge.TOP, EntryEdge.BOTTOM -> (pos.x / w).coerceIn(0f, 1f)
    }
    // raw 비율을 존 비율로 역매핑(차단 영역이면 가장 가까운 유효 비율로 clamp).
    return DropTarget(edge, unmapClamped(edge, along, hasBottomLeft, hasBottomRight, blockedRatio))
}
