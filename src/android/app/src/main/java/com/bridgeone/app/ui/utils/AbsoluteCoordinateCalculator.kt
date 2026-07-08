package com.bridgeone.app.ui.utils

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
}
