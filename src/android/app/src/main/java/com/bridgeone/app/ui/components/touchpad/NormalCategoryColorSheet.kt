package com.bridgeone.app.ui.components.touchpad

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bridgeone.app.ui.common.ColorCategory
import com.bridgeone.app.ui.common.ColorCategoryTab
import com.bridgeone.app.ui.common.ColorCodec
import com.bridgeone.app.ui.common.ColorPickerConstants
import com.bridgeone.app.ui.common.ColorRegistry

private sealed interface ColorSheetStage {
    object Category : ColorSheetStage
    data class Swatches(val tab: ColorCategoryTab) : ColorSheetStage
    object DirectInput : ColorSheetStage
}

/**
 * NORMAL 모드 전용 카테고리 → 색상 2단계 바텀시트.
 *
 * SWIPE 모드는 [ColorPickerSwipe]를 사용하고, NORMAL 모드는 터치 기반인 이 컴포넌트를 쓴다.
 * 1단계: 카테고리 격자(전체 + 카테고리들), 2단계: 선택 카테고리의 색상 격자.
 *
 * @param selectedColorHex 현재 선택된 색의 hex 문자열 (2단계에서 선택 표시)
 * @param onPick           색상 선택 콜백 — hex 문자열 전달
 * @param onDismiss        바텀시트 닫기 콜백
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NormalCategoryColorSheet(
    selectedColorHex: String,
    sheetState: SheetState,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    var stage by remember { mutableStateOf<ColorSheetStage>(ColorSheetStage.Category) }

    val initialColor = remember(selectedColorHex) { ColorCodec.hexToColorOrNull(selectedColorHex) ?: Color.Gray }
    val initialHsv = remember(selectedColorHex) { ColorCodec.colorToHsv(initialColor) }
    var hsv by remember { mutableStateOf(initialHsv) }
    val previewColor = ColorCodec.hsvToColor(hsv[0], hsv[1], hsv[2])
    val previewHex = ColorCodec.colorToHex(previewColor)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = cs.surfaceVariant,
    ) {
        when (val s = stage) {
            is ColorSheetStage.Category -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "카테고리 선택",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = cs.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { stage = ColorSheetStage.DirectInput },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Tune,
                            contentDescription = "직접 입력",
                            tint = cs.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    contentPadding = PaddingValues(start = 12.dp, top = 0.dp, end = 12.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    item {
                        ColorCategoryTile(
                            tab = ColorCategoryTab.All,
                            label = "전체",
                            onClick = { stage = ColorSheetStage.Swatches(ColorCategoryTab.All) },
                        )
                    }
                    items(ColorRegistry.categories) { cat ->
                        ColorCategoryTile(
                            tab = ColorCategoryTab.Real(cat),
                            label = cat.displayName,
                            onClick = { stage = ColorSheetStage.Swatches(ColorCategoryTab.Real(cat)) },
                        )
                    }
                }
            }

            is ColorSheetStage.DirectInput -> {
                @OptIn(ExperimentalFoundationApi::class)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {},
                            onLongClick = { stage = ColorSheetStage.Category },
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "직접 입력",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = cs.onSurface,
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(previewColor)
                                .border(1.dp, cs.outline.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        )
                        Text(
                            text = previewHex.let { if (it.startsWith("#FF") && it.length == 9) "#${it.substring(3)}" else it },
                            fontSize = 14.sp,
                            color = cs.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f),
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(cs.primary)
                                .clickable { onPick(previewHex) }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "적용",
                                fontSize = 13.sp,
                                color = cs.onPrimary,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }

                    HsvSliderRow(
                        label = "H",
                        value = hsv[0] / 360f,
                        trackBrush = Brush.horizontalGradient(
                            (0..12).map { i ->
                                ColorCodec.hsvToColor(i * 30f, hsv[1].coerceAtLeast(0.5f), hsv[2].coerceAtLeast(0.5f))
                            }
                        ),
                        onValueChange = { v -> hsv = floatArrayOf(v * 360f, hsv[1], hsv[2]) },
                    )
                    HsvSliderRow(
                        label = "S",
                        value = hsv[1],
                        trackBrush = Brush.horizontalGradient(
                            listOf(
                                ColorCodec.hsvToColor(hsv[0], 0f, hsv[2]),
                                ColorCodec.hsvToColor(hsv[0], 1f, hsv[2]),
                            )
                        ),
                        onValueChange = { v -> hsv = floatArrayOf(hsv[0], v, hsv[2]) },
                    )
                    HsvSliderRow(
                        label = "V",
                        value = hsv[2],
                        trackBrush = Brush.horizontalGradient(
                            listOf(Color.Black, ColorCodec.hsvToColor(hsv[0], hsv[1], 1f))
                        ),
                        onValueChange = { v -> hsv = floatArrayOf(hsv[0], hsv[1], v) },
                    )
                }
            }

            is ColorSheetStage.Swatches -> {
                val title = when (val tab = s.tab) {
                    is ColorCategoryTab.All  -> "전체"
                    is ColorCategoryTab.Real -> tab.category.displayName
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { stage = ColorSheetStage.Category },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "카테고리로",
                            tint = cs.onSurface,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = cs.onSurface,
                    )
                }
                val colors = ColorRegistry.colorsFor(s.tab)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(ColorPickerConstants.SWATCH_COLS),
                    contentPadding = PaddingValues(start = 12.dp, top = 0.dp, end = 12.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(colors) { color ->
                        val hex = ColorCodec.colorToHex(color)
                        val isSelected = hex == selectedColorHex
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(color)
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = if (isSelected) cs.onSurface else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp),
                                )
                                .clickable { onPick(hex) },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isSelected) {
                                Box(
                                    Modifier
                                        .size(10.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(
                                            if (color.luminance() > 0.5f) Color.Black.copy(0.6f)
                                            else Color.White.copy(0.8f)
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorCategoryTile(
    tab: ColorCategoryTab,
    label: String,
    onClick: () -> Unit,
) {
    val background: Brush = when (tab) {
        is ColorCategoryTab.All -> Brush.horizontalGradient(
            listOf(
                Color(0xFFF32121), Color(0xFF2196F3), Color(0xFF84E268), Color(0xFFB552F6),
            )
        )
        is ColorCategoryTab.Real -> Brush.linearGradient(
            listOf(
                Color(tab.category.representativeArgb),
                Color(tab.category.representativeArgb),
            )
        )
    }
    Box(
        modifier = Modifier
            .size(width = 80.dp, height = 64.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .clickable(onClick = onClick),
    )
}

private fun Color.luminance(): Float = 0.2126f * red + 0.7152f * green + 0.0722f * blue

@Composable
private fun HsvSliderRow(
    label: String,
    value: Float,
    trackBrush: Brush,
    onValueChange: (Float) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = cs.onSurfaceVariant,
            modifier = Modifier.width(14.dp),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(trackBrush),
        ) {
            Slider(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
