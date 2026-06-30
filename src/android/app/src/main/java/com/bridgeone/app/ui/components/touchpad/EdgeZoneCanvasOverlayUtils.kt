package com.bridgeone.app.ui.components.touchpad

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp

/**
 * 캔버스 오버레이 공통 렌더 유틸 (Phase 4.7.8-B).
 *
 * [ZoneCanvasDropOverlay]/[ZoneCanvasResizeOverlay] 양쪽에 byte-identical로 존재하던
 * 마커 선·색 밝기 계산을 단일 함수로 통합했다.
 */

/**
 * 수직 또는 수평 마커 선.
 *
 * @param vertical true이면 좌우 전폭 가로선(세로 엣지 핸들), false이면 상하 전고 세로선(가로 엣지 핸들).
 * @param thicknessDp 선 두께 (dp).
 * @param color 선 색상.
 */
@Composable
internal fun EdgeMarkerLine(vertical: Boolean, thicknessDp: Dp, color: Color) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            (if (vertical) Modifier.fillMaxWidth().height(thicknessDp)
             else Modifier.fillMaxHeight().width(thicknessDp))
                .background(color)
        )
    }
}

/**
 * 기본 색을 흰색 방향으로 [factor] 비율만큼 밝힌다.
 *
 * @param base   기준 색 (포커스 비활성 상태에서 적용되는 accent 색).
 * @param factor 0.0(변화 없음) ~ 1.0(완전 흰색). 포커스/조작 상태에 따라 달리 전달.
 */
internal fun brightenForState(base: Color, factor: Float): Color = lerp(base, Color.White, factor)
