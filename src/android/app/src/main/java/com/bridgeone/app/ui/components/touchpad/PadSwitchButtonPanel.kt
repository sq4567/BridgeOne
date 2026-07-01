package com.bridgeone.app.ui.components.touchpad

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import com.bridgeone.app.ui.common.MultiCursorConstants

/**
 * 직접 전환 버튼 레이아웃 모드의 하단 패드 전환 버튼 패널 (Phase 4.8.4).
 *
 * `component-touchpad.md` §1.2.2 기준. 터치패드 전체 면적을 유지한 채 하단에 N개
 * 버튼(pad1~padN)을 배치하고, 탭 1번으로 어느 패드든 즉시 전환한다.
 *
 * @param cursorCount 커서(패드) 수
 * @param activePadIndex 현재 활성 패드 인덱스
 * @param onPadSwitch 버튼 탭 시 호출되는 패드 전환 콜백
 * @param modifier 외부 Modifier
 */
@Composable
internal fun PadSwitchButtonPanel(
    cursorCount: Int,
    activePadIndex: Int,
    onPadSwitch: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current

    val panelShape = RoundedCornerShape(MultiCursorConstants.DIRECT_BUTTON_CORNER_RADIUS_DP.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(MultiCursorConstants.DIRECT_BUTTON_PANEL_HEIGHT_DP.dp)
            .clip(panelShape)
            .background(Color(0xFF1A1A1A))
    ) {
        for (index in 0 until cursorCount) {
            val isActive = index == activePadIndex
            val interactionSource = remember { MutableInteractionSource() }
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .background(
                        if (isActive) TouchpadColorBlue
                        else TouchpadColorBlue.copy(alpha = MultiCursorConstants.DIRECT_BUTTON_INACTIVE_ALPHA)
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onPadSwitch(index)
                    },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${index + 1}",
                    color = TouchpadColorButtonText,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
