package com.bridgeone.app.ui.components.touchpad

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bridgeone.app.ui.common.CurveDescription
import com.bridgeone.app.ui.common.CurveEditorConstants
import kotlin.math.roundToInt
import kotlin.math.sqrt

// ─────────────────────────────────────────────────────────────
// 저장 확인 오버레이 (Phase 4.5.18.7)
// (Phase 4.7.6-C: DynamicsCurveEditor.kt에서 분리)
// ─────────────────────────────────────────────────────────────

@Composable
internal fun SaveConfirmOverlay(
    curveDesc: CurveDescription,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    val view = LocalView.current
    var hoveredSlot by remember { mutableIntStateOf(1) }   // 1 = 저장 기본 호버
    var awaitingConfirm by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BG)
            .pointerInput(Unit) {
                val tapThreshPx = 10.dp.toPx()
                val stepPx = CurveEditorConstants.ACTION_GRID_SWIPE_STEP_DP.dp.toPx()
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startPos = down.position
                    var hasMoved = false

                    var ev = awaitPointerEvent()
                    while (ev.type != PointerEventType.Release) {
                        if (ev.type == PointerEventType.Move) {
                            ev.changes.forEach { it.consume() }
                            val pos = ev.changes.first().position
                            val dragDelta = pos - startPos
                            if (!hasMoved && sqrt(dragDelta.x * dragDelta.x + dragDelta.y * dragDelta.y) > tapThreshPx) {
                                hasMoved = true
                                awaitingConfirm = false
                            }
                            if (hasMoved) {
                                val newSlot = resolveSlot(dragDelta, stepPx, SAVE_CONFIRM_ROW_SLOTS, SAVE_CONFIRM_START_ROW)
                                if (newSlot != hoveredSlot) {
                                    hoveredSlot = newSlot
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                }
                            }
                        }
                        ev = awaitPointerEvent()
                    }

                    if (hasMoved) {
                        awaitingConfirm = true
                    } else if (awaitingConfirm) {
                        when (hoveredSlot) {
                            0 -> {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                else view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                onBack()
                            }
                            1 -> {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                else view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                onSave()
                            }
                        }
                    }
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 헤더
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SURFACE),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "저장 확인",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(12.dp))

            // 자연어 요약 카드
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SURFACE)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = curveDesc.summary,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 26.sp
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.08f))
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "저속 (0.5 dp/ms): ×${"%.2f".format(curveDesc.lowSpeedMultiplier)}",
                        color = LABEL_COLOR, fontSize = 12.sp
                    )
                    Text(
                        text = "중속 (3.0 dp/ms): ×${"%.2f".format(curveDesc.midSpeedMultiplier)}",
                        color = LABEL_COLOR, fontSize = 12.sp
                    )
                    Text(
                        text = "고속 (6.0 dp/ms): ×${"%.2f".format(curveDesc.highSpeedMultiplier)}",
                        color = LABEL_COLOR, fontSize = 12.sp
                    )
                    Text(
                        text = "가/감속 비대칭: ${curveDesc.asymmetryLabel}",
                        color = LABEL_COLOR, fontSize = 12.sp
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // 슬롯 행: [편집 계속] [이대로 저장]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 슬롯 0: 편집 계속
                val slot0Hovered = hoveredSlot == 0
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(10.dp))
                        .background(cellBgColor(slot0Hovered, awaitingConfirm && slot0Hovered, true))
                        .then(
                            if (slot0Hovered && !awaitingConfirm)
                                Modifier.border(1.dp, ACCENT_BLUE.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                            else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "편집 계속",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                // 슬롯 1: 이대로 저장
                val slot1Hovered = hoveredSlot == 1
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(10.dp))
                        .background(cellBgColor(slot1Hovered, awaitingConfirm && slot1Hovered, true))
                        .border(
                            1.dp,
                            if (slot1Hovered) ACCENT_BLUE.copy(alpha = if (awaitingConfirm) 0.55f else 0.9f)
                            else ACCENT_BLUE.copy(alpha = 0.25f),
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "이대로 저장",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 스텝 정밀도 피커 오버레이 (Phase 4.5.18.6)
// ─────────────────────────────────────────────────────────────

@Composable
internal fun NodeStepScalePickerOverlay(
    currentIndex: Int,
    onConfirm: (Int) -> Unit,
) {
    val view = LocalView.current
    val maxIndex = CurveEditorConstants.NODE_STEP_SCALES.size - 1

    var hoveredIndex by remember { mutableIntStateOf(currentIndex) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .pointerInput(Unit) {
                val tapThreshPx = 10.dp.toPx()
                val stepPx = CurveEditorConstants.NODE_STEP_PICKER_SWIPE_STEP_DP.dp.toPx()
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startPos = down.position
                    val startHoveredIndex = hoveredIndex
                    var hasMoved = false

                    var ev = awaitPointerEvent()
                    while (ev.type != PointerEventType.Release) {
                        if (ev.type == PointerEventType.Move) {
                            ev.changes.forEach { it.consume() }
                            val pos = ev.changes.first().position
                            val dx = pos.x - startPos.x
                            val dy = pos.y - startPos.y
                            if (!hasMoved && sqrt(dx * dx + dy * dy) > tapThreshPx) hasMoved = true
                            if (hasMoved) {
                                val newIdx = (startHoveredIndex + (dx / stepPx).roundToInt())
                                    .coerceIn(0, maxIndex)
                                if (newIdx != hoveredIndex) {
                                    hoveredIndex = newIdx
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                }
                            }
                        }
                        ev = awaitPointerEvent()
                    }

                    if (!hasMoved) {
                        // 탭 → 확정 (현재 적용값과 동일하면 취소와 같음)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        else
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        onConfirm(hoveredIndex)
                    }
                    // 가로 스와이프 → 선택만 이동, 피커 유지
                }
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "스텝 정밀도",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(24.dp))

            // B3: 5칸 가로 스와이프 피커 — 상단 의미 라벨(NODE_STEP_SCALE_LABELS) + 하단 절대 수치
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CurveEditorConstants.NODE_STEP_SCALES.forEachIndexed { idx, scale ->
                    val isHovered = idx == hoveredIndex
                    val isCurrent = idx == currentIndex
                    val xVal = CurveEditorConstants.CURVE_STEP_VELOCITY * scale
                    val yVal = CurveEditorConstants.CURVE_STEP_MULTIPLIER * scale
                    val label = CurveEditorConstants.NODE_STEP_SCALE_LABELS.getOrElse(idx) { "" }
                    Box(
                        modifier = Modifier
                            .width(CurveEditorConstants.NODE_STEP_PICKER_SWIPE_STEP_DP.dp)
                            .height(88.dp)
                            .background(
                                if (isHovered) ACCENT_BLUE.copy(alpha = 0.55f)
                                else Color.White.copy(alpha = 0.08f),
                                RoundedCornerShape(8.dp)
                            )
                            .then(when {
                                isHovered -> Modifier.border(1.dp, ACCENT_BLUE, RoundedCornerShape(8.dp))
                                isCurrent -> Modifier.border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                else -> Modifier
                            }),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // 의미 라벨 (상단, Bold)
                            Text(
                                text = label,
                                color = if (isHovered) Color.White else Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                fontWeight = if (isHovered || isCurrent) FontWeight.Bold else FontWeight.Normal
                            )
                            // 절대 수치 (하단, 보조)
                            Text(
                                text = "X ±${"%.2f".format(xVal)}",
                                color = if (isHovered) Color.White.copy(alpha = 0.85f) else LABEL_COLOR,
                                fontSize = 9.sp
                            )
                            Text(
                                text = "Y ±${"%.2f".format(yVal)}",
                                color = if (isHovered) Color.White.copy(alpha = 0.85f) else LABEL_COLOR,
                                fontSize = 9.sp
                            )
                        }
                    }
                    if (idx < maxIndex) Spacer(Modifier.width(3.dp))
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = "탭하여 확정  /  현재 적용값 선택 시 취소",
                color = LABEL_COLOR,
                fontSize = 11.sp,
            )
        }
    }
}
