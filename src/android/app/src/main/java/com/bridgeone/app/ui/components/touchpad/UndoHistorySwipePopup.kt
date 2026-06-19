package com.bridgeone.app.ui.components.touchpad

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bridgeone.app.ui.common.swipe.LocalSwipeFocusController
import com.bridgeone.app.ui.common.swipe.SwipeFocusable

/**
 * SWIPE 모드 Undo 히스토리 드롭다운. EdgeZoneEditorScreen에서 분리 (Phase 4.7.5-B).
 */
/**
 * SWIPE 모드 전용 Undo 히스토리 드롭다운.
 * 항목은 [EdgeEditorScope.UndoMenu] scope의 SwipeFocusable로 등록되어 위아래 스와이프로 탐색 가능.
 * 메인 Box 내부 인라인 오버레이로 렌더링해야 SwipeGestureLayer가 터치를 수신할 수 있음.
 * 항목이 많으면 스크롤되며 포커스된 항목이 뷰포트 안으로 자동 스크롤됨.
 */
@Composable
internal fun UndoHistorySwipePopup(
    undoStack: List<EdgeZoneConfig>,
    workConfig: EdgeZoneConfig,
    onApply: (config: EdgeZoneConfig, stackIdx: Int) -> Unit,
) {
    val swipeController = LocalSwipeFocusController.current
    val scrollState = rememberScrollState()
    val itemHeights = remember { mutableStateMapOf<Int, Int>() }
    var scrollableHeight by remember { mutableIntStateOf(0) }

    // 포커스된 항목이 뷰포트 밖이면 자동 스크롤
    LaunchedEffect(swipeController?.currentFocus) {
        val focus = swipeController?.currentFocus as? EdgeEditorElement.UndoHistoryItem ?: return@LaunchedEffect
        val itemTop = (0 until focus.index).sumOf { itemHeights[it] ?: 0 }
        val itemBottom = itemTop + (itemHeights[focus.index] ?: 0)
        val viewportTop = scrollState.value
        val viewportBottom = viewportTop + scrollableHeight
        when {
            itemTop < viewportTop -> scrollState.animateScrollTo(itemTop)
            itemBottom > viewportBottom && scrollableHeight > 0 ->
                scrollState.animateScrollTo(itemBottom - scrollableHeight)
        }
    }

    val cs = MaterialTheme.colorScheme
    androidx.compose.material3.Surface(
        modifier = Modifier.widthIn(min = 200.dp, max = 280.dp),
        shape = RoundedCornerShape(12.dp),
        color = cs.surfaceContainerHigh,
        shadowElevation = 12.dp,
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            // 스크롤 가능한 undo 항목 영역
            Box(
                modifier = Modifier
                    .heightIn(max = 400.dp)
                    .onSizeChanged { scrollableHeight = it.height }
            ) {
                Column(modifier = Modifier.verticalScroll(scrollState, enabled = false)) {
                    undoStack.forEachIndexed { idx, config ->
                        val newerConfig = if (idx == 0) workConfig else undoStack[idx - 1]
                        val desc = EdgeZoneActionResolver.describeUndoStep(from = config, to = newerConfig)
                        SwipeFocusable(
                            element = EdgeEditorElement.UndoHistoryItem(idx),
                            scope = EdgeEditorScope.UndoMenu,
                            shape = RoundedCornerShape(8.dp),
                            showBorderHighlight = true,
                            onActivate = { onApply(config, idx) },
                            gridRow = idx,
                            modifier = Modifier.onSizeChanged { size -> itemHeights[idx] = size.height },
                        ) {
                            Text(
                                text = desc,
                                fontSize = 14.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
