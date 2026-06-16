package com.bridgeone.app.ui.components.touchpad

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bridgeone.app.ui.common.IconCategoryTab

/**
 * NORMAL 모드 전용 카테고리 → 아이콘 2단계 바텀시트.
 *
 * SWIPE 모드는 [CategoryIconDrawer]를 사용하고, NORMAL 모드는 터치 기반인 이 컴포넌트를 쓴다.
 * 1단계: 카테고리 격자(전체 + 카테고리들), 2단계: 선택 카테고리의 아이콘 격자.
 *
 * @param selectedIconKey 현재 선택된 아이콘 키 (2단계에서 선택 표시)
 * @param onPick          아이콘 선택 콜백 (선택 후 [onDismiss]는 내부에서 호출하지 않으므로 호출부에서 닫기 처리)
 * @param onDismiss       바텀시트 닫기 콜백
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NormalCategoryIconSheet(
    selectedIconKey: String,
    sheetState: SheetState,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    var stage by remember { mutableStateOf<IconDrawerStage>(IconDrawerStage.Category) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = cs.surfaceVariant,
    ) {
        when (val s = stage) {
            is IconDrawerStage.Category -> {
                Text(
                    text = "카테고리 선택",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = cs.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    contentPadding = PaddingValues(start = 12.dp, top = 0.dp, end = 12.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    item {
                        CategoryTile("전체", Icons.Filled.Apps) {
                            stage = IconDrawerStage.Icons(IconCategoryTab.All)
                        }
                    }
                    items(IconRegistry.categories) { cat ->
                        CategoryTile(cat.displayName, IconRegistry.get(cat.representativeKey)) {
                            stage = IconDrawerStage.Icons(IconCategoryTab.Real(cat))
                        }
                    }
                }
            }

            is IconDrawerStage.Icons -> {
                val title = when (val tab = s.tab) {
                    is IconCategoryTab.All  -> "전체"
                    is IconCategoryTab.Real -> tab.category.displayName
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
                            .clickable { stage = IconDrawerStage.Category },
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
                val keys = IconRegistry.keysFor(s.tab)
                val normalized = IconRegistry.normalizeIconKey(selectedIconKey)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    contentPadding = PaddingValues(start = 12.dp, top = 0.dp, end = 12.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(keys) { key ->
                        val isSelected = selectedIconKey.isNotEmpty() && key == normalized
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) cs.secondary.copy(alpha = 0.2f) else cs.background)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) cs.secondary else cs.outline.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(8.dp),
                                )
                                .clickable { onPick(key) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = IconRegistry.get(key),
                                contentDescription = key,
                                tint = if (isSelected) cs.secondary else cs.onSurface,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryTile(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(width = 80.dp, height = 64.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(cs.background)
            .border(1.dp, cs.outline.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = cs.onSurface,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = label,
                fontSize = 10.sp,
                color = cs.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}
