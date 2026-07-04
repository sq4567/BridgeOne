package com.bridgeone.app.ui.components.touchpad

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
 * @param padLabels 패드별 표시 라벨 (인덱스 = pad1~padN, Phase 4.8.10). 번호 폴백은 호출부가 처리.
 * @param onPadSwitch 버튼 탭 시 호출되는 패드 전환 콜백
 * @param onPadLongPress 버튼 롱프레스 시 호출되는 콜백 (이름 편집 진입, Phase 4.8.10)
 * @param modifier 외부 Modifier
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun PadSwitchButtonPanel(
    cursorCount: Int,
    activePadIndex: Int,
    padLabels: List<String> = emptyList(),
    onPadSwitch: (Int) -> Unit,
    onPadLongPress: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val view = LocalView.current

    val panelShape = RoundedCornerShape(MultiCursorConstants.DIRECT_BUTTON_CORNER_RADIUS_DP.dp)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(MultiCursorConstants.DIRECT_BUTTON_PANEL_HEIGHT_DP.dp)
            .clip(panelShape)
            .background(Color(0xFF1A1A1A))
    ) {
        // 바닥 레이어: 비활성 색 셀 배경
        Row(modifier = Modifier.fillMaxSize()) {
            for (index in 0 until cursorCount) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .background(TouchpadColorBlue.copy(alpha = MultiCursorConstants.DIRECT_BUTTON_INACTIVE_ALPHA))
                )
            }
        }

        // 중간 레이어: 활성 패드를 나타내며 슬라이드하는 하이라이트
        val cellWidth = maxWidth / cursorCount
        val highlightOffset by animateDpAsState(
            targetValue = cellWidth * activePadIndex,
            animationSpec = tween(MultiCursorConstants.DIRECT_BUTTON_HIGHLIGHT_SLIDE_MS),
            label = "directButtonHighlightSlide"
        )
        Box(
            modifier = Modifier
                .offset(x = highlightOffset)
                .width(cellWidth)
                .fillMaxHeight()
                .background(TouchpadColorBlue)
        )

        // 상단 레이어: 텍스트 + 입력 (배경 투명, 햅틱 유지)
        Row(modifier = Modifier.fillMaxSize()) {
            for (index in 0 until cursorCount) {
                val interactionSource = remember { MutableInteractionSource() }
                Row(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .combinedClickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                onPadSwitch(index)
                            },
                            onLongClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                onPadLongPress(index)
                            }
                        ),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = padLabels.getOrNull(index)?.takeIf { it.isNotBlank() } ?: "${index + 1}",
                        color = TouchpadColorButtonText,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
