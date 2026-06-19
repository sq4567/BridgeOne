package com.bridgeone.app.ui.components.touchpad

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bridgeone.app.ui.common.swipe.SwipeFocusable
import androidx.compose.ui.draw.alpha

/**
 * SWIPE 모드 비율 프리셋 드롭다운 + MiniRatioBar. EdgeZoneEditorScreen에서 분리 (Phase 4.7.5-B).
 */
/**
 * SWIPE 모드 전용 비율 프리셋 드롭다운 팝업.
 * [PopupProperties.focusable] = false로 터치가 팝업을 통과하여 SwipeGestureLayer에 전달됨.
 * 항목은 [EdgeEditorScope.RatioPresetMenu] scope의 SwipeFocusable로 등록됨.
 */
@Composable
internal fun RatioPresetSwipePopup(
    presets: List<Pair<String, List<Float>>>,
    onSelect: (List<Float>) -> Unit,
) {
    Popup(
        alignment = Alignment.BottomStart,
        properties = PopupProperties(focusable = false),
    ) {
        androidx.compose.material3.Surface(
            shape = RoundedCornerShape(4.dp),
            tonalElevation = 2.dp,
            shadowElevation = 8.dp,
        ) {
            Column {
                presets.forEachIndexed { presetIdx, (label, ratios) ->
                    SwipeFocusable(
                        element = EdgeEditorElement.RatioPresetItem(label),
                        scope = EdgeEditorScope.RatioPresetMenu,
                        shape = RoundedCornerShape(4.dp),
                        showBorderHighlight = true,
                        onActivate = { onSelect(ratios) },
                        gridRow = presetIdx,
                    ) {
                        Row(
                            modifier = Modifier
                                .widthIn(min = 112.dp)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MiniRatioBar(ratios = ratios, modifier = Modifier.width(40.dp).height(10.dp))
                            Text(label, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun MiniRatioBar(
    ratios: List<Float>,
    modifier: Modifier = Modifier
) {
    val colors = listOf(
        Color(0xFF1E3A5F), Color(0xFF3A1E5F), Color(0xFF1E5F3A),
        Color(0xFF5F3A1E)
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        var x = 0f
        ratios.forEachIndexed { i, r ->
            val segW = r * w
            drawRect(
                color = colors[i % colors.size].copy(alpha = 0.85f),
                topLeft = Offset(x, 0f),
                size = Size((segW - 1f).coerceAtLeast(0f), h)
            )
            x += segW
        }
    }
}
