package com.bridgeone.app.ui.utils

import kotlin.math.roundToInt

/**
 * AbsolutePointingPad(Page 3)의 터치 좌표 ↔ 비율 변환 순수 함수 모음.
 *
 * stretch 매핑 원칙: PointingArea 비율과 대상 화면 비율이 달라도 letterbox/pillarbox
 * 계산 없이 전체 범위를 1:1로 매핑한다(실제 stretch는 서버가 수행, 여기서는 비율만 계산).
 *
 * Reference: docs/development-plans/phase-4/phase-4-9-page3-absolute-pointing.md Phase 4.9.1
 */

/** PointingArea 내 터치 위치 비율 (0.0~1.0) */
data class TouchRatio(val x: Float, val y: Float)

/**
 * 절대좌표 패드 줌 상태 (Phase 4.9.6). [level] 1x(해제)~8x, [centerX]/[centerY]는 줌 중심점 비율(0.0~1.0).
 * 페이지 전환에도 유지되어야 하므로 트랜지언트가 아닌 hoisted 상태로 다룬다(StandardModePage 참조).
 */
data class AbsoluteZoomState(
    val level: Float = AbsolutePointingConstants.ZOOM_LEVEL_MIN,
    val centerX: Float = 0.5f,
    val centerY: Float = 0.5f
) {
    val isActive: Boolean get() = level > AbsolutePointingConstants.ZOOM_LEVEL_MIN
}

/**
 * 절대좌표 스케일(0~32767) 최대값. FrameBuilder.buildAbsolutePositionCommand()의 동일 값과 일치.
 * 기본값: 32767
 */
const val ABS_COORDINATE_MAX = 32767

/**
 * 줌 매핑 범위를 절대좌표 스케일(0~32767) 정수로 인코딩한 결과 (Phase 4.9.7).
 * Vendor CDC 줌 상태 명령(min_x/min_y/max_x/max_y)에 그대로 사용된다.
 */
data class ZoomMappingRange(val minX: Int, val minY: Int, val maxX: Int, val maxY: Int)

object AbsoluteCoordinateCalculator {

    /**
     * 터치 좌표를 PointingArea 크기 기준 비율(0.0~1.0)로 변환합니다.
     * 영역 밖 터치는 경계값(0.0 또는 1.0)으로 클램핑됩니다.
     */
    fun calculateTouchRatio(
        touchX: Float,
        touchY: Float,
        areaWidth: Float,
        areaHeight: Float
    ): TouchRatio {
        val ratioX = if (areaWidth > 0f) (touchX / areaWidth).coerceIn(0f, 1f) else 0f
        val ratioY = if (areaHeight > 0f) (touchY / areaHeight).coerceIn(0f, 1f) else 0f
        return TouchRatio(ratioX, ratioY)
    }

    /**
     * 동일 좌표 연속 전송을 방지하기 위한 비교 함수.
     * [previous]가 null(최초 전송)이면 항상 true.
     */
    fun shouldTransmit(current: TouchRatio, previous: TouchRatio?): Boolean {
        if (previous == null) return true
        return current.x != previous.x || current.y != previous.y
    }

    /**
     * 줌 진입 드래그 거리(dp)를 줌 배율로 변환합니다 (Phase 4.9.6).
     * 구간별 선형 보간: 0dp→1x, 50dp→2x, 100dp→4x, 150dp+→8x(최대).
     */
    fun dragDistanceToZoomLevel(dragDistanceDp: Float): Float {
        val d = dragDistanceDp.coerceAtLeast(0f)
        return when {
            d <= 0f -> AbsolutePointingConstants.ZOOM_LEVEL_MIN
            d <= AbsolutePointingConstants.ZOOM_DRAG_DP_2X ->
                lerp(1f, 2f, d / AbsolutePointingConstants.ZOOM_DRAG_DP_2X)
            d <= AbsolutePointingConstants.ZOOM_DRAG_DP_4X -> lerp(
                2f, 4f,
                (d - AbsolutePointingConstants.ZOOM_DRAG_DP_2X) /
                    (AbsolutePointingConstants.ZOOM_DRAG_DP_4X - AbsolutePointingConstants.ZOOM_DRAG_DP_2X)
            )
            d <= AbsolutePointingConstants.ZOOM_DRAG_DP_8X -> lerp(
                4f, AbsolutePointingConstants.ZOOM_LEVEL_MAX,
                (d - AbsolutePointingConstants.ZOOM_DRAG_DP_4X) /
                    (AbsolutePointingConstants.ZOOM_DRAG_DP_8X - AbsolutePointingConstants.ZOOM_DRAG_DP_4X)
            )
            else -> AbsolutePointingConstants.ZOOM_LEVEL_MAX
        }
    }

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    /**
     * 줌 상태를 반영해 터치 비율을 PC 좌표 매핑 범위 비율로 재매핑합니다 (Phase 4.9.6).
     * 축(x/y) 독립적으로 처리하며, 줌 윈도우는 중심점 기준 0.0~1.0 범위 안에 클램핑됩니다.
     * level이 1x 이하이면 원본 ratio를 그대로 반환합니다.
     */
    fun applyZoom(ratio: TouchRatio, zoom: AbsoluteZoomState): TouchRatio {
        if (!zoom.isActive) return ratio
        val windowSize = 1f / zoom.level
        val minX = (zoom.centerX - windowSize / 2f).coerceIn(0f, 1f - windowSize)
        val minY = (zoom.centerY - windowSize / 2f).coerceIn(0f, 1f - windowSize)
        val outX = (minX + ratio.x * windowSize).coerceIn(0f, 1f)
        val outY = (minY + ratio.y * windowSize).coerceIn(0f, 1f)
        return TouchRatio(outX, outY)
    }

    /**
     * 줌 상태의 매핑 윈도우를 절대좌표 스케일(0~32767) 정수로 산출합니다 (Phase 4.9.7).
     * applyZoom()과 동일한 윈도우 계산(windowSize=1/level, 중심점 기준 0..1-windowSize 클램핑)을
     * 절대좌표 스케일로 인코딩한 것으로, Vendor CDC 줌 상태 명령의 min/max 필드에 사용된다.
     * 1x(비활성)이면 전체 범위(0, 0, 32767, 32767)를 반환한다.
     */
    fun calculateZoomMappingRange(zoom: AbsoluteZoomState): ZoomMappingRange {
        if (!zoom.isActive) {
            return ZoomMappingRange(0, 0, ABS_COORDINATE_MAX, ABS_COORDINATE_MAX)
        }
        val windowSize = 1f / zoom.level
        val minXRatio = (zoom.centerX - windowSize / 2f).coerceIn(0f, 1f - windowSize)
        val minYRatio = (zoom.centerY - windowSize / 2f).coerceIn(0f, 1f - windowSize)
        fun encode(ratio: Float) = (ratio * ABS_COORDINATE_MAX).roundToInt().coerceIn(0, ABS_COORDINATE_MAX)
        return ZoomMappingRange(
            minX = encode(minXRatio),
            minY = encode(minYRatio),
            maxX = encode(minXRatio + windowSize),
            maxY = encode(minYRatio + windowSize)
        )
    }
}
