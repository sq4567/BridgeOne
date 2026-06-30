package com.bridgeone.app.ui.components.touchpad

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bridgeone.app.ui.common.EdgeSwipeConstants
import com.bridgeone.app.ui.common.swipe.LocalSwipeFocused
import com.bridgeone.app.ui.common.swipe.SwipeFocusable

/**
 * 캔버스 씬 모드 진입 버튼 2행 (Phase 4.7.8-C — EdgeZoneCanvasModeOverlay에서 추출).
 * [isNoneMode]가 true일 때만 scale+fade로 등장, false이면 순차 퇴장.
 */
@Composable
internal fun ModeEntryButtons(
    isNoneMode: Boolean,
    onModeChange: (CanvasEditMode) -> Unit,
) {
    val staggerMs = EdgeSwipeConstants.EDGE_ZONE_MODE_BTN_STAGGER_MS
    val baseDelay = EdgeSwipeConstants.EDGE_ZONE_MODE_BTN_ENTER_BASE_DELAY_MS
    val sizeDp = EdgeSwipeConstants.EDGE_ZONE_MODE_CARD_SIZE_DP.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(CanvasModeKind.MERGE to 0, CanvasModeKind.SPLIT to 1, CanvasModeKind.MOVE to 2)
                .forEach { (kind, idx) ->
                    val exitDelay = idx * staggerMs
                    val enterDelay = baseDelay + idx * staggerMs
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isNoneMode,
                        enter = canvasModeEnter(enterDelay),
                        exit = canvasModeExit(exitDelay),
                    ) {
                        ModeCard(kind, onModeChange, sizeDp)
                    }
                }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(CanvasModeKind.DELETE to 3, CanvasModeKind.RESIZE to 4)
                .forEach { (kind, idx) ->
                    val exitDelay = idx * staggerMs
                    val enterDelay = baseDelay + idx * staggerMs
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isNoneMode,
                        enter = canvasModeEnter(enterDelay),
                        exit = canvasModeExit(exitDelay),
                    ) {
                        ModeCard(kind, onModeChange, sizeDp)
                    }
                }
        }
    }
}

/**
 * 단일 모드 진입 카드 (아이콘 + 라벨, SwipeFocusable 지원).
 * NORMAL은 탭, SWIPE는 포커스+탭으로 진입.
 */
@Composable
internal fun ModeCard(
    kind: CanvasModeKind,
    onModeChange: (CanvasEditMode) -> Unit,
    sizeDp: Dp = 92.dp,
) {
    val enter: () -> Unit = { onModeChange(kind.toMode()) }
    val modeColor = kind.accentColor()
    val compact = sizeDp < 88.dp
    // gridRow 미지정(null) → 캔버스 씬이 좌표 기반 traversal로 동작하여
    // 각 엣지 존에서 중앙 방향 스와이프 시 모드 버튼으로 포커스가 이동한다.
    SwipeFocusable(
        element = EdgeEditorElement.CanvasModeButton(kind),
        shape = RoundedCornerShape(EdgeSwipeConstants.EDGE_ZONE_MODE_CARD_CORNER_DP.dp),
        showBorderHighlight = false,
        onActivate = enter,
    ) {
        val focused = LocalSwipeFocused.current
        Surface(
            onClick = enter,
            shape = RoundedCornerShape(EdgeSwipeConstants.EDGE_ZONE_MODE_CARD_CORNER_DP.dp),
            color = if (focused) modeColor else modeColor.copy(alpha = 0.82f),
            contentColor = Color.White,
            tonalElevation = 3.dp,
            shadowElevation = if (focused) 6.dp else 2.dp,
            border = if (focused) BorderStroke(2.dp, Color.White) else null,
            modifier = Modifier.size(sizeDp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp, Alignment.CenterVertically),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (compact) 6.dp else 8.dp),
            ) {
                Icon(kind.icon(), contentDescription = kind.label, modifier = Modifier.size(if (compact) 24.dp else 28.dp))
                Text(kind.label, fontSize = if (compact) 12.sp else 13.sp)
            }
        }
    }
}
