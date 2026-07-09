package com.bridgeone.app.ui.components

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bridgeone.app.ui.components.touchpad.TouchpadColorRed
import com.bridgeone.app.ui.components.touchpad.TouchpadColorYellow

/**
 * Page 3 절대좌표 패드 모니터 셀렉터 (Phase 4.9.5).
 *
 * "전체" 칩 + 모니터 개수만큼 번호 칩으로 구성된 상시 노출 라이브 셀렉터.
 * ClickModeButton처럼 사전 선택 게이트가 아니라 사용 중 언제든 전환 가능하다.
 * 선택값은 targetMonitor 바이트 규약(0x00=전체, 0x01~N=특정 모니터)의 Int로 다룬다.
 *
 * @param monitorCount 현재 모니터 개수. 2 미만이면 호출측에서 노출하지 않는다(component-touchpad.md §2.2b).
 * @param selectedMonitor 현재 선택된 targetMonitor 값(0=전체, 1~N=모니터 인덱스)
 * @param onSelect 칩 선택 콜백
 */
@Composable
fun MonitorSelector(
    monitorCount: Int,
    selectedMonitor: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        MonitorSelectorChip(
            label = "전체",
            contentDescriptionText = "매핑 대상: 전체 화면",
            selected = selectedMonitor == 0,
            onClick = {
                view.performHapticFeedback(hapticConfirmConstant())
                onSelect(0)
            }
        )
        for (index in 1..monitorCount) {
            MonitorSelectorChip(
                label = index.toString(),
                contentDescriptionText = "매핑 대상: 모니터 $index",
                selected = selectedMonitor == index,
                onClick = {
                    view.performHapticFeedback(hapticConfirmConstant())
                    onSelect(index)
                }
            )
        }
    }
}

private fun hapticConfirmConstant(): Int =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        HapticFeedbackConstants.CONFIRM
    } else {
        HapticFeedbackConstants.KEYBOARD_TAP
    }

@Composable
private fun MonitorSelectorChip(
    label: String,
    contentDescriptionText: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val backgroundColor: Color = if (selected) TouchpadColorYellow else TouchpadColorRed

    Text(
        text = label,
        color = Color.White,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .sizeIn(minWidth = 32.dp, minHeight = 32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .semantics { contentDescription = contentDescriptionText }
            .wrapContentSize(Alignment.Center)
            .padding(horizontal = 4.dp)
    )
}
