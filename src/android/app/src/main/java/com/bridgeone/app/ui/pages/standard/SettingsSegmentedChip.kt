package com.bridgeone.app.ui.pages.standard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 설정 페이지 세그먼트 칩 선택 색. 기본값: 0xFF2979FF */
private val SETTINGS_CHIP_ACCENT = Color(0xFF2979FF)
/** 비선택 칩 배경. 기본값: 0xFF2A2A2A */
private val SETTINGS_CHIP_BG = Color(0xFF2A2A2A)
/** 비선택 칩 테두리. 기본값: 0xFF444444 */
private val SETTINGS_CHIP_BORDER = Color(0xFF444444)
/** 비선택 칩 레이블 색. 기본값: 0xFFCCCCCC */
private val SETTINGS_CHIP_LABEL = Color(0xFFCCCCCC)

/**
 * 설정 페이지 세그먼트 칩 선택기.
 *
 * [options]를 한 행으로 배치하고 [selected]에 해당하는 칩을 파란 테두리/배경으로 강조한다.
 * `Page5Settings.kt`에서 3회 중복된 칩 Row 패턴을 단일 컴포저블로 통합 (Phase 4.7.8-D).
 *
 * @param options (값, 레이블) 쌍 목록
 * @param selected 현재 선택된 값 ([T.equals]로 비교)
 * @param onSelect 칩 탭 시 호출
 * @param modifier Row에 전달할 Modifier (예: `Modifier.fillMaxWidth()`)
 * @param chipSpacing 칩 간 수평 간격. 기본값: 8.dp
 * @param chipPaddingH 칩 수평 안쪽 여백. 기본값: 16.dp
 * @param chipPaddingV 칩 수직 안쪽 여백. 기본값: 8.dp
 */
@Composable
internal fun <T> SegmentedChipSelector(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    chipSpacing: Dp = 8.dp,
    chipPaddingH: Dp = 16.dp,
    chipPaddingV: Dp = 8.dp,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(chipSpacing),
        modifier = modifier,
    ) {
        options.forEach { (value, label) ->
            val isSelected = value == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) SETTINGS_CHIP_ACCENT.copy(alpha = 0.2f) else SETTINGS_CHIP_BG)
                    .border(
                        width = if (isSelected) 1.5.dp else 0.5.dp,
                        color = if (isSelected) SETTINGS_CHIP_ACCENT else SETTINGS_CHIP_BORDER,
                        shape = RoundedCornerShape(8.dp),
                    )
                    .clickable { onSelect(value) }
                    .padding(horizontal = chipPaddingH, vertical = chipPaddingV),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    color = if (isSelected) SETTINGS_CHIP_ACCENT else SETTINGS_CHIP_LABEL,
                )
            }
        }
    }
}
