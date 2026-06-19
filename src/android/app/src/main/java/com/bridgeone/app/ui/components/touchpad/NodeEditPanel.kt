package com.bridgeone.app.ui.components.touchpad

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bridgeone.app.ui.common.CurveEditorConstants
import com.bridgeone.app.ui.common.CurveNode

// ─────────────────────────────────────────────────────────────
// 노드 편집 패널 (Phase 4.7.6-B: DynamicsCurveEditor.kt에서 분리)
// ─────────────────────────────────────────────────────────────

@Composable
internal fun EditorActionGrid(
    slots: List<ActionSlot>,
    hoveredSlot: Int,
    awaitingConfirm: Boolean,
    activeTab: Int,
    modifier: Modifier = Modifier
) {
    CurveCard(
        slots = slots,
        hoveredSlot = hoveredSlot,
        awaitingConfirm = awaitingConfirm,
        activeTab = activeTab,
        modifier = modifier.padding(horizontal = 6.dp, vertical = 4.dp)
    )
}

/** 슬롯 9: 노드 편집 진입 버튼 (그래프 컨테이너 내 하단 오버레이) */
@Composable
internal fun Slot9Card(hoveredSlot: Int, awaitingConfirm: Boolean, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(10.dp)
    val isHovered = hoveredSlot == 9
    val isAwaiting = awaitingConfirm && isHovered
    val bg = when {
        isAwaiting  -> ACCENT_BLUE.copy(alpha = 0.85f)
        isHovered   -> ACCENT_BLUE.copy(alpha = 0.55f)
        else        -> SURFACE
    }
    val borderMod: Modifier = when {
        isAwaiting -> Modifier.border(2.dp, Color.White.copy(alpha = 0.9f), shape)
        isHovered  -> Modifier.border(1.5.dp, ACCENT_BLUE, shape)
        else       -> Modifier.border(1.dp, Color.White.copy(alpha = 0.45f), shape)
    }
    Box(
        modifier = modifier
            .height(34.dp)
            .background(bg, shape)
            .then(borderMod),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "노드 편집",
            color = if (isHovered || isAwaiting) Color.White else Color.White.copy(alpha = 0.55f),
            fontSize = 12.sp,
            fontWeight = if (isHovered || isAwaiting) FontWeight.Bold else FontWeight.Normal
        )
    }
}

/**
 * 노드 편집 헤더 strip (Phase 4.5.18.7)
 * NodeEdit 모드에서 MetaCard 자리에 렌더. 좌=뒤로(slot 0)만. 탭 토글은 그래프 내부 ActiveTabLabel로 이동.
 */
@Composable
internal fun NodeEditHeader(
    hoveredSlot: Int,
    awaitingConfirm: Boolean,
    isSelectMode: Boolean = false,
    onTabLabelHoveredChange: ((Boolean) -> Unit)? = null,
    onTabToggle: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(10.dp)
    val view = LocalView.current
    Row(
        modifier = modifier
            .height(28.dp)
            .padding(horizontal = 4.dp)
            .then(
                if (isSelectMode && onTabLabelHoveredChange != null && onTabToggle != null)
                    Modifier.pointerInput(Unit) {
                        val downThreshPx = 40.dp.toPx()
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val startY = down.position.y
                            var hovering = false
                            var ev = awaitPointerEvent()
                            while (ev.type != PointerEventType.Release) {
                                if (ev.type == PointerEventType.Move) {
                                    ev.changes.forEach { it.consume() }
                                    val dy = ev.changes.first().position.y - startY
                                    val nowHovering = dy > downThreshPx
                                    if (nowHovering != hovering) {
                                        hovering = nowHovering
                                        onTabLabelHoveredChange(hovering)
                                        if (hovering) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    }
                                }
                                ev = awaitPointerEvent()
                            }
                            onTabLabelHoveredChange(false)
                            if (hovering) onTabToggle()
                        }
                    }
                else Modifier
            ),
        horizontalArrangement = Arrangement.Start
    ) {
        // 슬롯 0: ← (화살표만, 아주 작게)
        val hov0 = hoveredSlot == 0; val aw0 = awaitingConfirm && hov0
        AnimatedCellBox(
            isHovered = hov0, isAwaitingConfirm = aw0, enabled = true,
            shape = cardShape,
            modifier = Modifier.width(32.dp).fillMaxHeight()
        ) {
            Text(
                "←",
                color = if (hov0 || aw0) Color.White else Color.White.copy(alpha = 0.55f),
                fontSize = 13.sp,
                fontWeight = if (hov0 || aw0) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

/** 그래프 내부 활성 탭 버튼. 헤더에서 아래로 스와이프하여 선택. (Phase 4.5.18.9) */
@Composable
internal fun ActiveTabLabel(
    activeTab: Int,
    isHovered: Boolean = false,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = if (activeTab == 0) ACCENT_BLUE else ACCENT_ORANGE
    val label = if (activeTab == 0) "가속" else "감속"
    val shape = RoundedCornerShape(8.dp)
    val borderAlpha = if (isHovered) 0.75f else 0.25f
    val bgAlpha = if (isHovered) 0.25f else 0f
    Text(
        text = label,
        color = color.copy(alpha = if (isHovered) 1f else 0.9f),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .shadow(elevation = 6.dp, shape = shape)
            .clip(shape)
            .background(color.copy(alpha = bgAlpha), shape)
            .border(1.dp, Color.White.copy(alpha = borderAlpha), shape)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

/**
 * 노드 편집 본문 그리드 (Phase 4.5.18.7~8)
 * 3개 독립 카드로 분리(B1): 노드 네비 / 2D 십자 패드 / CRUD.
 * AnimatedCellBox(C1) 적용, slot10 원형화(A1), CRUD 대칭(A2).
 * NodeEdit 모드에서 EditorActionGrid 자리에 렌더.
 */
@Composable
internal fun NodeEditGrid(
    activeCurve: List<CurveNode>,
    selectedNodeIndex: Int,
    activeTab: Int,
    hoveredSlot: Int,
    awaitingConfirm: Boolean,
    isSlotEnabled: (Int) -> Boolean,
    nodeStepIndex: Int,
    isSelectMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(12.dp)
    val cellShape = RoundedCornerShape(8.dp)
    val halfLeftShape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
    val halfRightShape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)
    val dividerColor = Color.White.copy(alpha = 0.07f)

    // 슬롯 텍스트 색 헬퍼
    fun slotTextColor(hov: Boolean, aw: Boolean, en: Boolean, tintColor: Color = Color.White): Color = when {
        (hov || aw) && en -> Color.White
        en                -> tintColor.copy(alpha = 0.7f)
        else              -> tintColor.copy(alpha = 0.25f)
    }

    // ── B1: 3개 독립 카드 ────────────────────────────────────────────
    Column(
        modifier = modifier.padding(horizontal = 6.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {

        // 선택 모드에서는 카드 본문 숨김 — 그래프가 공간을 채우도록 애니메이션
        AnimatedVisibility(
            visible = !isSelectMode,
            enter = expandVertically(tween(250, easing = FastOutSlowInEasing)) + fadeIn(tween(200)),
            exit = shrinkVertically(tween(200, easing = FastOutSlowInEasing)) + fadeOut(tween(150))
        ) { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {

        // ── 슬롯 11: 노드 선택 — 선택 모드 재진입 ──────────────────────
        val activeColor = if (activeTab == 0) ACCENT_BLUE else ACCENT_ORANGE
        val nodeCount = activeCurve.size
        val isHov11 = hoveredSlot == 11; val isAw11 = awaitingConfirm && isHov11; val en11 = isSlotEnabled(11)
        AnimatedCellBox(
            isHovered = isHov11, isAwaitingConfirm = isAw11, enabled = en11,
            shape = RoundedCornerShape(8.dp),
            tintColor = activeColor,
            showAccentIdleBorder = true,
            modifier = Modifier.fillMaxWidth().height(40.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "◀▶",
                    color = when {
                        (isHov11 || isAw11) -> Color.White
                        else -> activeColor.copy(alpha = 0.75f)
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "노드 선택  ${selectedNodeIndex + 1} / $nodeCount",
                    color = when {
                        (isHov11 || isAw11) -> Color.White
                        else -> activeColor.copy(alpha = 0.75f)
                    },
                    fontSize = 12.sp,
                    fontWeight = if (isHov11 || isAw11) FontWeight.Bold else FontWeight.Normal
                )
            }
        }

        // ── 십자 패드 (슬롯 3·4·5·6 + slot10 원형) ──────────────────────
        // 네 방향 버튼: fillMaxHeight().aspectRatio(1f) → 36×36dp 정사각형
        // slot10: weight(1f).fillMaxHeight() → 형태 변경 없음
        val xStep = CurveEditorConstants.CURVE_STEP_VELOCITY * CurveEditorConstants.NODE_STEP_SCALES[nodeStepIndex]
        val yStep = CurveEditorConstants.CURVE_STEP_MULTIPLIER * CurveEditorConstants.NODE_STEP_SCALES[nodeStepIndex]
        // 전체 패드 중앙 정렬
        // 방향 버튼: 36×36dp 정사각형 / 가운데: 36×64dp 둥근 직사각형
        // 상하 Spacer = (btnSize + centerW) / 2 → Y▲·Y▼이 가운데 버튼 위·아래 중심 정렬
        val btnSize = 36.dp
        val centerW = 64.dp
        val sideSpacerW = (btnSize + centerW) / 2  // 50.dp
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column {
                // 상단 행: [50dp] [Y ▲ 36×36] [50dp]
                Row {
                    Spacer(Modifier.width(sideSpacerW).height(btnSize))
                    val isHov6 = hoveredSlot == 6; val isAw6 = awaitingConfirm && isHov6; val en6 = isSlotEnabled(6)
                    AnimatedCellBox(
                        isHovered = isHov6, isAwaitingConfirm = isAw6, enabled = en6,
                        shape = cellShape, modifier = Modifier.size(btnSize)
                    ) {
                        Text("Y ▲", color = slotTextColor(isHov6, isAw6, en6), fontSize = 13.sp,
                            fontWeight = if ((isHov6 || isAw6) && en6) FontWeight.Bold else FontWeight.Normal)
                    }
                    Spacer(Modifier.width(sideSpacerW).height(btnSize))
                }
                // 중간 행: [X ◀ 36×36] [slot10 36×64 둥근 직사각형] [X ▶ 36×36]
                Row {
                    val isHov3 = hoveredSlot == 3; val isAw3 = awaitingConfirm && isHov3; val en3 = isSlotEnabled(3)
                    AnimatedCellBox(
                        isHovered = isHov3, isAwaitingConfirm = isAw3, enabled = en3,
                        shape = cellShape, modifier = Modifier.size(btnSize)
                    ) {
                        Text("X ◀", color = slotTextColor(isHov3, isAw3, en3), fontSize = 13.sp,
                            fontWeight = if ((isHov3 || isAw3) && en3) FontWeight.Bold else FontWeight.Normal)
                    }
                    val isHov10 = hoveredSlot == 10; val isAw10 = awaitingConfirm && isHov10; val en10 = isSlotEnabled(10)
                    AnimatedCellBox(
                        isHovered = isHov10, isAwaitingConfirm = isAw10, enabled = en10,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.width(centerW).height(btnSize)
                    ) {
                        Text(
                            "X ±${"%.2f".format(xStep)}\nY ±${"%.2f".format(yStep)}",
                            color = slotTextColor(isHov10, isAw10, en10),
                            fontSize = 10.sp,
                            lineHeight = 11.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            fontWeight = if (isHov10 || isAw10) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    val isHov4 = hoveredSlot == 4; val isAw4 = awaitingConfirm && isHov4; val en4 = isSlotEnabled(4)
                    AnimatedCellBox(
                        isHovered = isHov4, isAwaitingConfirm = isAw4, enabled = en4,
                        shape = cellShape, modifier = Modifier.size(btnSize)
                    ) {
                        Text("X ▶", color = slotTextColor(isHov4, isAw4, en4), fontSize = 13.sp,
                            fontWeight = if ((isHov4 || isAw4) && en4) FontWeight.Bold else FontWeight.Normal)
                    }
                }
                // 하단 행: [50dp] [Y ▼ 36×36] [50dp]
                Row {
                    Spacer(Modifier.width(sideSpacerW).height(btnSize))
                    val isHov5 = hoveredSlot == 5; val isAw5 = awaitingConfirm && isHov5; val en5 = isSlotEnabled(5)
                    AnimatedCellBox(
                        isHovered = isHov5, isAwaitingConfirm = isAw5, enabled = en5,
                        shape = cellShape, modifier = Modifier.size(btnSize)
                    ) {
                        Text("Y ▼", color = slotTextColor(isHov5, isAw5, en5), fontSize = 13.sp,
                            fontWeight = if ((isHov5 || isAw5) && en5) FontWeight.Bold else FontWeight.Normal)
                    }
                    Spacer(Modifier.width(sideSpacerW).height(btnSize))
                }
            }
        }

        // ── CRUD: 노드 추가(7) / 노드 삭제(8) ───────────────────────────
        Row(
            Modifier.fillMaxWidth().height(48.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val isHov7 = hoveredSlot == 7; val isAw7 = awaitingConfirm && isHov7; val en7 = isSlotEnabled(7)
            AnimatedCellBox(
                isHovered = isHov7, isAwaitingConfirm = isAw7, enabled = en7,
                shape = cellShape,
                tintColor = ACCENT_BLUE,
                showAccentIdleBorder = true,
                modifier = Modifier.weight(1f).fillMaxHeight()
            ) {
                Text(
                    "+ 추가",
                    color = when {
                        (isHov7 || isAw7) && en7 -> Color.White
                        en7                       -> ACCENT_BLUE.copy(alpha = 0.8f)
                        else                      -> Color.White.copy(alpha = 0.22f)
                    },
                    fontSize = 12.sp,
                    fontWeight = if ((isHov7 || isAw7) && en7) FontWeight.Bold else FontWeight.Normal
                )
            }
            val isHov8 = hoveredSlot == 8; val isAw8 = awaitingConfirm && isHov8; val en8 = isSlotEnabled(8)
            AnimatedCellBox(
                isHovered = isHov8, isAwaitingConfirm = isAw8, enabled = en8,
                shape = cellShape,
                tintColor = ACCENT_RED,
                showAccentIdleBorder = true,
                modifier = Modifier.weight(1f).fillMaxHeight()
            ) {
                Text(
                    "− 삭제",
                    color = when {
                        (isHov8 || isAw8) && en8 -> Color.White
                        en8                       -> ACCENT_RED.copy(alpha = 0.8f)
                        else                      -> ACCENT_RED.copy(alpha = 0.25f)
                    },
                    fontSize = 12.sp,
                    fontWeight = if ((isHov8 || isAw8) && en8) FontWeight.Bold else FontWeight.Normal
                )
            }
        }

        } } // end AnimatedVisibility + inner Column
    }
}
