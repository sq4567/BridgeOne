package com.bridgeone.app.ui.components.touchpad

import com.bridgeone.app.ui.common.EdgeSwipeConstants

/**
 * (entryEdge, alongEdgeRatio) 쌍을 받아 활성 EdgeZone을 반환한다.
 */
object EdgeZoneDetector {

    /**
     * @param config       현재 존 설정
     * @param edge         진입 엣지 방향
     * @param alongRatio   손가락의 엣지 축 위치 (0.0~1.0)
     *                     TOP/BOTTOM: 좌→우, LEFT/RIGHT: 위→아래
     * @return 해당 위치의 EdgeZone, 없으면 null
     */
    fun findActiveZone(config: EdgeZoneConfig, edge: EntryEdge, alongRatio: Float): EdgeZone? {
        val zones = config.zonesFor(edge)
        return zones.firstOrNull { zone ->
            alongRatio >= zone.startRatio && alongRatio <= zone.endRatio
        }
    }

    /**
     * 존 경계를 따라 드래그 가능한 경계 비율 목록을 반환한다.
     * 첫 번째 존의 startRatio(0.0)와 마지막 존의 endRatio(1.0)는 제외.
     */
    fun boundaryRatios(zones: List<EdgeZone>): List<Float> {
        if (zones.size <= 1) return emptyList()
        return zones.dropLast(1).map { it.endRatio }
    }

    /**
     * 비율이 모서리 버튼 차단 구간에 속하는지 확인한다.
     * 차단 구간: 0.0~CORNER_BUTTON_BLOCKED_RATIO 또는 (1-RATIO)~1.0
     */
    fun isBlockedByCornerButton(
        edge: EntryEdge,
        ratio: Float,
        hasBottomLeftButton: Boolean,
        hasBottomRightButton: Boolean
    ): Boolean {
        val blocked = EdgeSwipeConstants.CORNER_BUTTON_BLOCKED_RATIO
        return when (edge) {
            EntryEdge.LEFT   -> hasBottomLeftButton && ratio > (1f - blocked)
            EntryEdge.RIGHT  -> hasBottomRightButton && ratio > (1f - blocked)
            EntryEdge.BOTTOM -> (hasBottomLeftButton && ratio < blocked) ||
                                (hasBottomRightButton && ratio > (1f - blocked))
            EntryEdge.TOP    -> false
        }
    }
}
