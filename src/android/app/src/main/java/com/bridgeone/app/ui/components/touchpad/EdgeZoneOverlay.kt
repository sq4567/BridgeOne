package com.bridgeone.app.ui.components.touchpad

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bridgeone.app.ui.common.ColorCodec
import com.bridgeone.app.ui.common.EdgeSwipeConstants

/**
 * ZONE 모드에서 터치패드에 렌더링되는 시각 오버레이.
 *
 * - idle: 각 존을 색상 블록 + 기능 아이콘으로 상시 표시 (곁눈질만으로 배치 인지)
 * - 엣지 진입 중: 활성 존을 흰색 fill로 강조 + 라벨 추가 표시
 * - 로테이션 존 armed 시: 현재 후보 라벨 + 점 인디케이터
 */
@Composable
fun EdgeZoneOverlay(
    config: EdgeZoneConfig,
    isEdgeCandidate: Boolean,
    entryEdge: EntryEdge?,
    fingerAlongEdgePx: Float,
    inwardDistancePx: Float,
    touchpadWidthPx: Float,
    touchpadHeightPx: Float,
    isZoneArmed: Boolean = false,
    rotationIndex: Int = 0,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    // 전체 아이콘 키에 대해 VectorPainter를 사전 빌드.
    // for 루프는 Compose 슬롯 관점에서 안전: IconRegistry.allKeys는 고정 순서/고정 크기이므로
    // 재컴포즈 간 composable 호출 순서가 일정하게 유지됨.
    val iconPainters = buildIconPainterMap()

    // 활성 존 사전 계산: 진입 중인 존의 아이콘은 idle 렌더링에서 숨겨 라벨 가림 방지
    val isEntryActive = isEdgeCandidate && entryEdge != null &&
        inwardDistancePx >= EdgeSwipeConstants.DROPLET_APPEAR_THRESHOLD_DP
    val activeZoneInfo: Pair<EntryEdge, EdgeZone>? = if (isEntryActive && entryEdge != null) {
        val edgeLen = if (entryEdge == EntryEdge.LEFT || entryEdge == EntryEdge.RIGHT)
            touchpadHeightPx else touchpadWidthPx
        val alongRatio = if (edgeLen > 0f) fingerAlongEdgePx / edgeLen else 0f
        EdgeZoneDetector.findActiveZone(config, entryEdge, alongRatio)?.let { entryEdge to it }
    } else null

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val edgePx = with(density) { EdgeSwipeConstants.EDGE_HIT_WIDTH_DP.dp.toPx() }
        val idleIconSizePx = with(density) { EdgeSwipeConstants.EDGE_ZONE_IDLE_ICON_SIZE_DP.dp.toPx() }
        val idleIconInsetPx = with(density) { EdgeSwipeConstants.EDGE_ZONE_IDLE_ICON_INSET_DP.dp.toPx() }
        val lineAlpha = EdgeSwipeConstants.EDGE_ZONE_HINT_BASE_ALPHA * 3f

        // ── idle: 전 존 색상 블록 + 아이콘 상시 표시 ──
        EntryEdge.entries.forEach { edge ->
            val zones = config.zonesFor(edge)

            zones.forEachIndexed { idx, zone ->
                val zStart = zone.startRatio
                val zEnd = zone.endRatio
                val zoneTopLeft: Offset
                val zoneSize: Size
                when (edge) {
                    EntryEdge.LEFT   -> { zoneTopLeft = Offset(0f, zStart * h);         zoneSize = Size(edgePx, (zEnd - zStart) * h) }
                    EntryEdge.RIGHT  -> { zoneTopLeft = Offset(w - edgePx, zStart * h); zoneSize = Size(edgePx, (zEnd - zStart) * h) }
                    EntryEdge.TOP    -> { zoneTopLeft = Offset(zStart * w, 0f);         zoneSize = Size((zEnd - zStart) * w, edgePx) }
                    EntryEdge.BOTTOM -> { zoneTopLeft = Offset(zStart * w, h - edgePx); zoneSize = Size((zEnd - zStart) * w, edgePx) }
                }

                // 색상 블록 (colorHex 사용자 커스텀 우선, 없으면 액션 카테고리 색상)
                val blockColor = ColorCodec.hexToColorOrNull(zone.colorHex) ?: zone.action.categoryColor()
                drawRect(
                    color = blockColor.copy(alpha = EdgeSwipeConstants.EDGE_ZONE_IDLE_BLOCK_ALPHA),
                    topLeft = zoneTopLeft,
                    size = zoneSize
                )

                // 아이콘 (할당된 존만, 존 크기가 아이콘보다 클 때만, 활성 진입 중인 존은 제외)
                val isAssignedZone = zone.action !is EdgeZoneAction.Unassigned
                val isActiveZone = activeZoneInfo?.first == edge &&
                    activeZoneInfo.second.startRatio == zone.startRatio &&
                    activeZoneInfo.second.endRatio == zone.endRatio
                if (isAssignedZone && !isActiveZone) {
                    val iconKey = zone.iconKey.ifEmpty { zone.action.defaultIconKey() }
                    if (iconKey.isNotEmpty()) {
                        val painter = iconPainters[IconRegistry.normalizeIconKey(iconKey)]
                        if (painter != null && zoneSize.width >= idleIconSizePx && zoneSize.height >= idleIconSizePx) {
                            // 외곽 테두리에서 inset만큼 안쪽으로 중심을 이동해 아이콘이 경계에 너무 붙지 않게 함
                            val cx = when (edge) {
                                EntryEdge.LEFT  -> zoneTopLeft.x + zoneSize.width / 2f + idleIconInsetPx
                                EntryEdge.RIGHT -> zoneTopLeft.x + zoneSize.width / 2f - idleIconInsetPx
                                else -> zoneTopLeft.x + zoneSize.width / 2f
                            }
                            val cy = when (edge) {
                                EntryEdge.TOP    -> zoneTopLeft.y + zoneSize.height / 2f + idleIconInsetPx
                                EntryEdge.BOTTOM -> zoneTopLeft.y + zoneSize.height / 2f - idleIconInsetPx
                                else -> zoneTopLeft.y + zoneSize.height / 2f
                            }
                            translate(cx - idleIconSizePx / 2f, cy - idleIconSizePx / 2f) {
                                with(painter) {
                                    draw(
                                        size = Size(idleIconSizePx, idleIconSizePx),
                                        colorFilter = ColorFilter.tint(
                                            Color.White.copy(alpha = EdgeSwipeConstants.EDGE_ZONE_IDLE_ICON_ALPHA)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 존 경계선 (alpha 상향)
            zones.dropLast(1).forEach { zone ->
                val ratio = zone.endRatio
                when (edge) {
                    EntryEdge.LEFT   -> drawLine(Color.White.copy(alpha = lineAlpha), Offset(0f, ratio * h), Offset(edgePx, ratio * h), 1.5f)
                    EntryEdge.RIGHT  -> drawLine(Color.White.copy(alpha = lineAlpha), Offset(w - edgePx, ratio * h), Offset(w, ratio * h), 1.5f)
                    EntryEdge.TOP    -> drawLine(Color.White.copy(alpha = lineAlpha), Offset(ratio * w, 0f), Offset(ratio * w, edgePx), 1.5f)
                    EntryEdge.BOTTOM -> drawLine(Color.White.copy(alpha = lineAlpha), Offset(ratio * w, h - edgePx), Offset(ratio * w, h), 1.5f)
                }
            }
        }

        // ── 활성 존 하이라이트 + 라벨 (진입 중) ──
        if (!isEdgeCandidate || entryEdge == null) return@Canvas
        if (inwardDistancePx < EdgeSwipeConstants.DROPLET_APPEAR_THRESHOLD_DP) return@Canvas

        val edgeLen = if (entryEdge == EntryEdge.LEFT || entryEdge == EntryEdge.RIGHT) h else w
        val alongRatio = if (edgeLen > 0f) fingerAlongEdgePx / edgeLen else 0f
        val activeZone = EdgeZoneDetector.findActiveZone(config, entryEdge, alongRatio) ?: return@Canvas

        val trigger = activeZone.trigger
        val displayLabel: String
        val rotationCandidateCount: Int
        val isAssigned: Boolean

        when (trigger) {
            is EdgeZoneTrigger.SingleAction -> {
                displayLabel = trigger.label.ifEmpty { trigger.action.defaultLabel() }
                isAssigned = trigger.action !is EdgeZoneAction.Unassigned
                rotationCandidateCount = 0
            }
            is EdgeZoneTrigger.Rotation -> {
                val idx = if (isZoneArmed) rotationIndex.coerceIn(0, (trigger.candidates.size - 1).coerceAtLeast(0)) else 0
                val candidate = trigger.candidates.getOrNull(idx)
                displayLabel = candidate?.label ?: ""
                isAssigned = trigger.candidates.isNotEmpty()
                rotationCandidateCount = trigger.candidates.size
            }
        }

        // 활성 존 스트립 영역
        val zStart = activeZone.startRatio
        val zEnd = activeZone.endRatio
        val activeTopLeft: Offset
        val activeSize: Size
        when (entryEdge) {
            EntryEdge.LEFT   -> { activeTopLeft = Offset(0f, zStart * h);         activeSize = Size(edgePx, (zEnd - zStart) * h) }
            EntryEdge.RIGHT  -> { activeTopLeft = Offset(w - edgePx, zStart * h); activeSize = Size(edgePx, (zEnd - zStart) * h) }
            EntryEdge.TOP    -> { activeTopLeft = Offset(zStart * w, 0f);         activeSize = Size((zEnd - zStart) * w, edgePx) }
            EntryEdge.BOTTOM -> { activeTopLeft = Offset(zStart * w, h - edgePx); activeSize = Size((zEnd - zStart) * w, edgePx) }
        }

        // 흰색 fill 강조 (기존 색상 블록 위에 덮음)
        drawRect(color = Color.White.copy(alpha = 0.20f), topLeft = activeTopLeft, size = activeSize)
        drawRect(color = Color.White.copy(alpha = 0.55f), topLeft = activeTopLeft, size = activeSize, style = Stroke(width = 1.5f))

        if (!isAssigned || displayLabel.isEmpty()) return@Canvas

        val isVertical = entryEdge == EntryEdge.LEFT || entryEdge == EntryEdge.RIGHT
        val textToDraw = if (isVertical) displayLabel.toList().joinToString("\n") else displayLabel

        val textLayout = textMeasurer.measure(
            textToDraw,
            style = TextStyle(
                fontSize = EdgeSwipeConstants.ZONE_LABEL_FONT_SIZE_SP.sp,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = if (isVertical) TextAlign.Center else TextAlign.Start
            )
        )

        val cx = activeTopLeft.x + activeSize.width / 2f
        val cy = activeTopLeft.y + activeSize.height / 2f
        val tx = cx - textLayout.size.width / 2f
        val ty = cy - textLayout.size.height / 2f

        if (activeSize.width > textLayout.size.width + 2f && activeSize.height > textLayout.size.height + 2f) {
            drawText(textLayout, topLeft = Offset(tx, ty))
        }

        // 로테이션 점 인디케이터 (armed 시)
        if (rotationCandidateCount > 1 && isZoneArmed) {
            val dotR = with(density) { 2.dp.toPx() }
            val dotSpacing = with(density) { 5.dp.toPx() }
            val totalDotsW = rotationCandidateCount * dotR * 2 + (rotationCandidateCount - 1) * (dotSpacing - dotR * 2)
            val dotY = activeTopLeft.y + activeSize.height - dotR - with(density) { 2.dp.toPx() }
            val dotStartX = cx - totalDotsW / 2f + dotR
            repeat(rotationCandidateCount) { i ->
                val dotX = dotStartX + i * dotSpacing
                drawCircle(
                    color = if (i == rotationIndex % rotationCandidateCount) Color.White else Color.White.copy(alpha = 0.35f),
                    radius = dotR,
                    center = Offset(dotX, dotY)
                )
            }
        }
    }
}

/**
 * IconRegistry 전체 키에 대해 VectorPainter 맵을 빌드.
 *
 * for 루프로 [rememberVectorPainter]를 호출한다. [IconRegistry.allKeys]는 LinkedHashMap 기반 고정
 * 순서/고정 크기이므로 재컴포즈 간 composable 슬롯 순서가 일정하게 유지 — Compose 슬롯 규칙 충족.
 */
@Composable
private fun buildIconPainterMap(): Map<String, VectorPainter> {
    val keys = IconRegistry.allKeys
    val paintersList = mutableListOf<VectorPainter>()
    for (key in keys) {
        paintersList.add(rememberVectorPainter(IconRegistry.get(key)))
    }
    return keys.zip(paintersList).toMap()
}
