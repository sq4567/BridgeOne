package com.bridgeone.app.ui.components.touchpad

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bridgeone.app.ui.common.EdgeSwipeConstants

/** 병합 선택 영역 강조 색(초록). EdgeZoneEditorPreviewCanvas의 선택 강조와 동일. 기본값: 0xFF4CAF50 */
private val MERGE_REGION_COLOR = Color(0xFF4CAF50)
/** 병합 선택 영역 채움 투명도. 기본값: 0.22f */
private const val MERGE_REGION_FILL_ALPHA = 0.22f
/** 병합 선택 영역 테두리 두께 (dp). 기본값: 2.5f */
private const val MERGE_REGION_BORDER_DP = 2.5f

/**
 * 병합(Merge) 모드의 연속 선택 구간을 하나의 영역 박스로 강조한다 (NORMAL/SWIPE 공통).
 *
 * 존별 개별 강조와 달리, 선택은 항상 인접한 연속 구간이므로 `[regionStartRatio, regionEndRatio]`
 * 하나의 박스로 표현한다. 존을 추가/삭제할 때 양 경계 비율이 [animateFloatAsState]로 보간되어
 * 영역이 부드럽게 늘어나고 줄어든다. 엣지 스트립 두께/정렬은 [ZoneCanvasResizeOverlay]와 동일하게
 * [EdgeSwipeConstants.EDGE_HIT_WIDTH_DP] 기준.
 *
 * @param regionStartRatio 선택 구간 시작 비율(0~1, along 축 최솟값)
 * @param regionEndRatio 선택 구간 끝 비율(0~1, along 축 최댓값)
 * @param canvasWidth/[canvasHeight] 호출부 `BoxWithConstraints`의 maxWidth/maxHeight
 */
@Composable
internal fun MergeSelectionOverlay(
    edge: EntryEdge,
    regionStartRatio: Float,
    regionEndRatio: Float,
    canvasWidth: Dp,
    canvasHeight: Dp,
    hasBottomLeft: Boolean = false,
    hasBottomRight: Boolean = false,
    blockedRatio: Float = EdgeSwipeConstants.CORNER_BUTTON_BLOCKED_RATIO,
) {
    val animSpec = tween<Float>(EdgeSwipeConstants.EDGE_ZONE_MORPH_MS, easing = FastOutSlowInEasing)
    val animStart by animateFloatAsState(regionStartRatio, animSpec, label = "mergeRegionStart")
    val animEnd by animateFloatAsState(regionEndRatio, animSpec, label = "mergeRegionEnd")

    val edgeDp = EdgeSwipeConstants.EDGE_HIT_WIDTH_DP.dp
    // 존 비율을 코너 버튼 차단 제외 유효 영역으로 매핑.
    val mStart = mapToValid(edge, animStart, hasBottomLeft, hasBottomRight, blockedRatio)
    val mEnd = mapToValid(edge, animEnd, hasBottomLeft, hasBottomRight, blockedRatio)
    val span = (mEnd - mStart).coerceAtLeast(0f)

    val (offX, offY, boxW, boxH) = edgeStripRect(edge, canvasWidth, canvasHeight, edgeDp, mStart, span)

    Box(
        Modifier
            .offset(x = offX, y = offY)
            .size(width = boxW, height = boxH)
            .background(MERGE_REGION_COLOR.copy(alpha = MERGE_REGION_FILL_ALPHA))
            .border(MERGE_REGION_BORDER_DP.dp, MERGE_REGION_COLOR)
    )
}
