package com.bridgeone.app.ui.components.touchpad

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bridgeone.app.ui.common.EdgeSwipeConstants
import com.bridgeone.app.ui.common.swipe.LocalSwipeFocused
import com.bridgeone.app.ui.common.swipe.SwipeFocusable

/**
 * 병합·삭제 모드 바: 선택 카운트 + 확인(다중 선택 후 일괄 적용) + 취소 (Phase 4.7.8-C 추출).
 */
@Composable
internal fun ModeActiveBar(
    kind: CanvasModeKind,
    selectedCount: Int,
    showConfirm: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val modeColor = kind.accentColor()
    // 삭제는 1개 이상, 병합은 2개 이상 선택해야 확정 가능
    val minToConfirm = if (kind == CanvasModeKind.MERGE) 2 else 1
    val canConfirm = selectedCount >= minToConfirm
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (showConfirm) {
            val confirm: () -> Unit = { if (canConfirm) onConfirm() }
            val confirmButton: @Composable () -> Unit = {
                val focused = LocalSwipeFocused.current
                Surface(
                    onClick = confirm,
                    enabled = canConfirm,
                    shape = RoundedCornerShape(8.dp),
                    color = if (focused && canConfirm) Color.White else if (canConfirm) modeColor else modeColor.copy(alpha = 0.25f),
                    contentColor = if (focused && canConfirm) modeColor else Color.White.copy(alpha = if (canConfirm) 1f else 0.5f),
                ) {
                    Text("확인", fontSize = 12.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp))
                }
            }
            // 비활성(선택 부족) 시 SwipeFocusable로 등록하지 않아 포커스 불가
            if (canConfirm) {
                SwipeFocusable(
                    element = EdgeEditorElement.CanvasModeConfirm,
                    shape = RoundedCornerShape(8.dp),
                    showBorderHighlight = false,
                    onActivate = confirm,
                ) { confirmButton() }
            } else {
                confirmButton()
            }
        }
        CancelButton(modeColor, onCancel)
    }
}

/** 모드 바 공용 취소 버튼. */
@Composable
internal fun CancelButton(modeColor: Color, onCancel: () -> Unit) {
    SwipeFocusable(
        element = EdgeEditorElement.CanvasModeCancel,
        shape = RoundedCornerShape(8.dp),
        showBorderHighlight = false,
        onActivate = onCancel,
    ) {
        val focused = LocalSwipeFocused.current
        Surface(
            onClick = onCancel,
            shape = RoundedCornerShape(8.dp),
            color = if (focused) Color.White else modeColor.copy(alpha = 0.85f),
            contentColor = if (focused) modeColor else Color.White,
        ) {
            Text("취소", fontSize = 12.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp))
        }
    }
}

/** 모드 바 공용 '확인' 버튼 (비율 조정 변경 적용 / 이동 모드 마치기 등). */
@Composable
internal fun ConfirmButton(modeColor: Color, onConfirm: () -> Unit) {
    SwipeFocusable(
        element = EdgeEditorElement.CanvasModeConfirm,
        shape = RoundedCornerShape(8.dp),
        showBorderHighlight = false,
        onActivate = onConfirm,
    ) {
        val focused = LocalSwipeFocused.current
        Surface(
            onClick = onConfirm,
            shape = RoundedCornerShape(8.dp),
            color = if (focused) Color.White else modeColor,
            contentColor = if (focused) modeColor else Color.White,
        ) {
            Text("확인", fontSize = 12.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp))
        }
    }
}

/** 분할 모드 바: 대상 존 선택 후 분할 갯수(2~5) 버튼을 표시. */
@Composable
internal fun SplitModeBar(
    targetZone: EdgeZone?,
    edgeZoneCount: Int,
    onSplit: (Int) -> Unit,
    onCancel: () -> Unit,
) {
    val modeColor = CanvasModeKind.SPLIT.accentColor()
    val staggerDelay = EdgeSwipeConstants.EDGE_ZONE_MODE_BTN_STAGGER_MS
    androidx.compose.foundation.layout.Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // 대상 존 선택 후 갯수 버튼 Row가 fade+scale로 등장/퇴장
        androidx.compose.animation.AnimatedVisibility(
            visible = targetZone != null,
            enter = canvasModeEnter(staggerDelay),
            exit = canvasModeExit(),
        ) {
            // AnimatedVisibility 내부에서 targetZone이 non-null임이 보장되지 않으므로 안전하게 접근
            val zone = targetZone ?: return@AnimatedVisibility
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val width = zone.endRatio - zone.startRatio
                (2..5).forEach { n ->
                    val valid = edgeZoneCount + n - 1 <= EdgeSwipeConstants.MAX_ZONES_PER_EDGE.toInt() &&
                                width / n >= EdgeSwipeConstants.MIN_ZONE_RATIO
                    val split: () -> Unit = { if (valid) onSplit(n) }
                    val numberButton: @Composable () -> Unit = {
                        val focused = LocalSwipeFocused.current
                        Surface(
                            onClick = split,
                            enabled = valid,
                            shape = RoundedCornerShape(8.dp),
                            color = if (focused && valid) Color.White else if (valid) modeColor else modeColor.copy(alpha = 0.25f),
                            contentColor = if (focused && valid) modeColor else Color.White.copy(alpha = if (valid) 1f else 0.4f),
                        ) {
                            Text("$n", fontSize = 14.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp))
                        }
                    }
                    // 비활성 갯수는 SwipeFocusable로 등록하지 않아 포커스 불가
                    if (valid) {
                        SwipeFocusable(
                            element = EdgeEditorElement.CanvasSplitChoice(n),
                            shape = RoundedCornerShape(8.dp),
                            showBorderHighlight = false,
                            onActivate = split,
                        ) { numberButton() }
                    } else {
                        numberButton()
                    }
                }
            }
        }
        CancelButton(modeColor, onCancel)
    }
}
