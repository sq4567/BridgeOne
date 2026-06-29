package com.bridgeone.app.ui.components.touchpad

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bridgeone.app.ui.common.EdgeSwipeConstants
import kotlinx.coroutines.delay

/** 데모 양방향 화살표 길이 (dp). 기본값: 100f */
private const val HINT_ARROW_LEN_DP = 100f
/** 데모 축 수직 방향(화살표 폭) 박스 두께 (dp). 기본값: 48f */
private const val HINT_BOX_THICKNESS_DP = 48f
/** 데모 화살표 선 두께 (dp). 기본값: 3f */
private const val HINT_ARROW_THICKNESS_DP = 3f
/** 데모 손가락 아이콘 크기 (dp). 기본값: 32f */
private const val HINT_FINGER_SIZE_DP = 32f
/** 데모 손가락 왕복 1회 주기 (ms). 기본값: 850 */
private const val HINT_FINGER_CYCLE_MS = 850

/**
 * SWIPE 비율 조정 모드에서 경계를 탭으로 확정(MANIPULATION 진입)했을 때, 해당 엣지 옆에
 * "이 경계를 (양방향으로) 움직일 수 있다"를 알리는 데모를 잠시 표시한다.
 *
 * 긴 양방향 화살표(조작 축 방향) + 그 화살표를 따라 왕복하는 손가락 아이콘으로 구성되며,
 * [EdgeSwipeConstants.ZONE_BOUNDARY_HINT_VISIBLE_MS] 후 fade out 된다.
 *
 * @param boundaryRatio 조작 중인 경계의 along 비율(0~1)
 * @param canvasWidth/[canvasHeight] 호출부 `BoxWithConstraints`의 maxWidth/maxHeight
 */
@Composable
internal fun BoundaryManipulationHint(
    edge: EntryEdge,
    boundaryRatio: Float,
    canvasWidth: Dp,
    canvasHeight: Dp,
    hasBottomLeft: Boolean = false,
    hasBottomRight: Boolean = false,
    blockedRatio: Float = EdgeSwipeConstants.CORNER_BUTTON_BLOCKED_RATIO,
) {
    val vertical = edge == EntryEdge.LEFT || edge == EntryEdge.RIGHT
    val edgeDp = EdgeSwipeConstants.EDGE_HIT_WIDTH_DP.dp
    val arrowLen = HINT_ARROW_LEN_DP.dp
    val thickness = HINT_BOX_THICKNESS_DP.dp
    val gap = 8.dp

    val boxW = if (vertical) thickness else arrowLen
    val boxH = if (vertical) arrowLen else thickness

    // 위치는 진입 시점 비율로 고정 — 경계가 등장한 자리에 표시하되, 이후 경계가 움직여도 따라가지 않음
    val anchorRatio = remember { boundaryRatio }
    // 존 비율을 코너 버튼 차단 제외 유효 영역으로 매핑.
    val mappedAnchor = mapToValid(edge, anchorRatio, hasBottomLeft, hasBottomRight, blockedRatio)

    // 박스 좌상단 offset (경계 위치 기준, 엣지 안쪽으로 gap 만큼 들여 배치)
    val offX: Dp
    val offY: Dp
    when (edge) {
        EntryEdge.LEFT -> { offX = edgeDp + gap; offY = canvasHeight * mappedAnchor - boxH / 2f }
        EntryEdge.RIGHT -> { offX = canvasWidth - edgeDp - gap - boxW; offY = canvasHeight * mappedAnchor - boxH / 2f }
        EntryEdge.TOP -> { offX = canvasWidth * mappedAnchor - boxW / 2f; offY = edgeDp + gap }
        EntryEdge.BOTTOM -> { offX = canvasWidth * mappedAnchor - boxW / 2f; offY = canvasHeight - edgeDp - gap - boxH }
    }

    // 표시 시간 경과 후, 또는 유저가 경계를 한 번이라도 움직이면(비율 변화) fade out
    var visible by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(EdgeSwipeConstants.ZONE_BOUNDARY_HINT_VISIBLE_MS.toLong())
        visible = false
    }
    LaunchedEffect(boundaryRatio) {
        if (boundaryRatio != anchorRatio) visible = false
    }
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(400), label = "hintAlpha")
    if (alpha <= 0.01f) return

    val color = MaterialTheme.colorScheme.primary

    // 손가락 왕복 (0~1 reverse)
    val infinite = rememberInfiniteTransition(label = "hintFinger")
    val t by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(HINT_FINGER_CYCLE_MS, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "hintT",
    )

    Box(
        Modifier
            .offset(x = offX, y = offY)
            .size(width = boxW, height = boxH)
            .alpha(alpha)
    ) {
        // 양방향 화살표 (조작 축 방향)
        Canvas(Modifier.matchParentSize()) {
            val lineThick = HINT_ARROW_THICKNESS_DP.dp.toPx()
            val head = 9.dp.toPx()
            if (vertical) {
                val cx = size.width / 2f
                drawLine(color, Offset(cx, head), Offset(cx, size.height - head), lineThick, cap = StrokeCap.Round)
                drawLine(color, Offset(cx, 0f), Offset(cx - head, head), lineThick, cap = StrokeCap.Round)
                drawLine(color, Offset(cx, 0f), Offset(cx + head, head), lineThick, cap = StrokeCap.Round)
                drawLine(color, Offset(cx, size.height), Offset(cx - head, size.height - head), lineThick, cap = StrokeCap.Round)
                drawLine(color, Offset(cx, size.height), Offset(cx + head, size.height - head), lineThick, cap = StrokeCap.Round)
            } else {
                val cy = size.height / 2f
                drawLine(color, Offset(head, cy), Offset(size.width - head, cy), lineThick, cap = StrokeCap.Round)
                drawLine(color, Offset(0f, cy), Offset(head, cy - head), lineThick, cap = StrokeCap.Round)
                drawLine(color, Offset(0f, cy), Offset(head, cy + head), lineThick, cap = StrokeCap.Round)
                drawLine(color, Offset(size.width, cy), Offset(size.width - head, cy - head), lineThick, cap = StrokeCap.Round)
                drawLine(color, Offset(size.width, cy), Offset(size.width - head, cy + head), lineThick, cap = StrokeCap.Round)
            }
        }
        // 손가락 (화살표 축을 따라 왕복)
        val fingerSize = HINT_FINGER_SIZE_DP.dp
        val travel = arrowLen - fingerSize
        val shift = travel * (t - 0.5f)
        Icon(
            Icons.Filled.TouchApp,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = if (vertical) 0.dp else shift, y = if (vertical) shift else 0.dp)
                .size(fingerSize),
        )
    }
}
