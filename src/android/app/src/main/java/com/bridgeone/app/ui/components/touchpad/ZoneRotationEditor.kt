package com.bridgeone.app.ui.components.touchpad

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bridgeone.app.ui.common.EdgeSwipeConstants
import com.bridgeone.app.ui.common.InputMode
import com.bridgeone.app.ui.common.ToastController
import com.bridgeone.app.ui.common.ToastType
import com.bridgeone.app.ui.common.swipe.LocalSwipeFocusController
import com.bridgeone.app.ui.common.swipe.LocalSwipeFocused
import com.bridgeone.app.ui.common.swipe.SwipeFocusable
import com.bridgeone.app.ui.common.swipe.SwipeMode
import com.bridgeone.app.ui.common.ColorCodec
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.roundToInt

/**
 * 로테이션(액션 순환) 존 편집기. EdgeZoneEditorScreen에서 분리 (Phase 4.7.5-B).
 */
// ============================================================
// 로테이션(액션 순환) 존 편집기
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RotationEditor(
    rotation: EdgeZoneTrigger.Rotation,
    onRotationChanged: (EdgeZoneTrigger.Rotation) -> Unit,
    onRequestLabelKeyboard: (current: String, onResult: (String) -> Unit) -> Unit,
    inputMode: InputMode,
    editingEntry: Pair<Int?, RotationCandidate>?,
    onEditingEntryChange: (Pair<Int?, RotationCandidate>?) -> Unit,
    draftCandidate: RotationCandidate,
    onDraftChange: (RotationCandidate) -> Unit,
    onCandidateActionSelected: (EdgeZoneAction) -> Unit,
    onRequestCandidateIconPicker: () -> Unit,
    onRequestCandidateColorPicker: () -> Unit,
    iconBoxAnchorReport: (Offset) -> Unit,
    colorBoxAnchorReport: (Offset) -> Unit,
    isEditingLabel: Boolean = false,
    labelCursorAlpha: Float = 1f,
    onApply: (() -> Unit)? = null,
    onBeforeIntervalChange: (() -> Unit)? = null,
    pageCount: Int = 5,
    onSwipeShortcutRequest: ((EdgeZoneAction.SendShortcut, (EdgeZoneAction.SendShortcut) -> Unit, ((draft: EdgeZoneAction.SendShortcut, iconKey: String, name: String) -> Unit)?) -> Unit)? = null,
) {
    val cs = MaterialTheme.colorScheme
    val rotationSwipeCtrl = LocalSwipeFocusController.current
    val minCandidates = EdgeSwipeConstants.EDGE_ZONE_ROTATION_MIN_CANDIDATES
    val minMs = EdgeSwipeConstants.EDGE_ZONE_ROTATION_INTERVAL_MIN_MS.toFloat()
    val maxMs = EdgeSwipeConstants.EDGE_ZONE_ROTATION_INTERVAL_MAX_MS.toFloat()

    val intervalSteps = ((maxMs - minMs) / 50f).toInt() - 1 // 기본값: 50f 단위

    // 인터벌 프리셋
    data class IntervalPreset(val label: String, val ms: Int)
    val intervalPresets = listOf(
        IntervalPreset("빠름", EdgeSwipeConstants.EDGE_ZONE_ROTATION_INTERVAL_FAST_MS),
        IntervalPreset("보통", EdgeSwipeConstants.EDGE_ZONE_ROTATION_INTERVAL_NORMAL_MS),
        IntervalPreset("느림", EdgeSwipeConstants.EDGE_ZONE_ROTATION_INTERVAL_SLOW_MS)
    )
    val presetMsSet = intervalPresets.map { it.ms }.toSet()
    var showCustomSlider by remember { mutableStateOf(rotation.intervalMs !in presetMsSet) }

    LaunchedEffect(showCustomSlider) {
        if (showCustomSlider && inputMode == InputMode.SWIPE) {
            rotationSwipeCtrl?.setFocus(EdgeEditorElement.RotationIntervalSlider)
        }
    }

    BackHandler(enabled = editingEntry != null) { onEditingEntryChange(null) }

    if (editingEntry == null) {
        // ── 후보 목록 뷰 ──
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {

            rotation.candidates.forEachIndexed { idx, candidate ->
                val isAutoLabel = candidate.label.isEmpty()
                val displayLabel = candidate.label.ifEmpty { candidate.action.defaultLabel() }
                val displayIconKey = candidate.iconKey.ifEmpty { candidate.action.defaultIconKey() }

                val rowGrid = 40 + idx
                val upAction: () -> Unit = {
                    if (idx > 0) {
                        val list = rotation.candidates.toMutableList()
                        list.add(idx - 1, list.removeAt(idx))
                        onRotationChanged(rotation.copy(candidates = list))
                    }
                }
                val editAction: () -> Unit = {
                    onDraftChange(candidate)
                    onEditingEntryChange(Pair(idx, candidate))
                }
                val downAction: () -> Unit = {
                    if (idx < rotation.candidates.size - 1) {
                        val list = rotation.candidates.toMutableList()
                        list.add(idx + 1, list.removeAt(idx))
                        onRotationChanged(rotation.copy(candidates = list))
                    }
                }
                val deleteAction: () -> Unit = {
                    val list = rotation.candidates.toMutableList()
                    list.removeAt(idx)
                    onRotationChanged(rotation.copy(candidates = list))
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(cs.surfaceVariant),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 위로 버튼 (48dp)
                    SwipeFocusable(
                        element = EdgeEditorElement.RotationCandidateUp(idx),
                        shape = RoundedCornerShape(8.dp),
                        onActivate = upAction,
                        gridRow = rowGrid,
                    ) {
                    val upFocused = LocalSwipeFocused.current
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clickable(
                                enabled = idx > 0,
                                onClick = upAction
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.KeyboardArrowUp,
                            contentDescription = "위로",
                            modifier = Modifier.size(20.dp),
                            tint = if (upFocused) cs.primary
                                   else if (idx > 0) cs.onSurface
                                   else cs.onSurface.copy(alpha = 0.3f)
                        )
                    }
                    }

                    // 카드 본문 (탭 → 편집)
                    SwipeFocusable(
                        element = EdgeEditorElement.RotationCandidateEdit(idx),
                        shape = RoundedCornerShape(8.dp),
                        showBorderHighlight = true,
                        onActivate = editAction,
                        gridRow = rowGrid,
                        modifier = Modifier.weight(1f),
                    ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = editAction)
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 번호 원형 배지
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(cs.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${idx + 1}", fontSize = 11.sp, color = cs.primary, fontWeight = FontWeight.Bold)
                        }
                        // 아이콘
                        if (displayIconKey.isNotEmpty()) {
                            Icon(
                                imageVector = IconRegistry.get(displayIconKey),
                                contentDescription = null,
                                tint = if (isAutoLabel) cs.onSurface.copy(alpha = 0.6f) else cs.onSurface,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        // 액션명 + 라벨
                        Column(modifier = Modifier.weight(1f)) {
                            Text(candidate.action.displayName(), fontSize = 12.sp, color = cs.onSurface)
                            if (displayLabel.isNotEmpty() && displayLabel != candidate.action.displayName()) {
                                Text(displayLabel, fontSize = 10.sp, color = cs.onSurfaceVariant)
                            }
                        }
                        // 자동 배지
                        if (isAutoLabel) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(cs.tertiaryContainer)
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text("자동", fontSize = 8.sp, color = cs.onTertiaryContainer) // 기본값: 8.sp
                            }
                        }
                    }
                    } // SwipeFocusable(RotationCandidateEdit) 닫기

                    // 아래로 버튼 (48dp)
                    SwipeFocusable(
                        element = EdgeEditorElement.RotationCandidateDown(idx),
                        shape = RoundedCornerShape(8.dp),
                        onActivate = downAction,
                        gridRow = rowGrid,
                    ) {
                    val downFocused = LocalSwipeFocused.current
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clickable(
                                enabled = idx < rotation.candidates.size - 1,
                                onClick = downAction
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.KeyboardArrowDown,
                            contentDescription = "아래로",
                            modifier = Modifier.size(20.dp),
                            tint = if (downFocused) cs.primary
                                   else if (idx < rotation.candidates.size - 1) cs.onSurface
                                   else cs.onSurface.copy(alpha = 0.3f)
                        )
                    }
                    }

                    // 삭제 버튼 (48dp)
                    SwipeFocusable(
                        element = EdgeEditorElement.RotationCandidateDelete(idx),
                        shape = RoundedCornerShape(8.dp),
                        showBorderHighlight = true,
                        onActivate = deleteAction,
                        gridRow = rowGrid,
                    ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clickable(onClick = deleteAction),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "삭제",
                            modifier = Modifier.size(18.dp),
                            tint = cs.error.copy(alpha = 0.7f)
                        )
                    }
                    }
                }
            }

            val addCandidateAction: () -> Unit = {
                val newCandidate = RotationCandidate(EdgeZoneAction.SetClickMode(ClickMode.LEFT_CLICK), "", "")
                onDraftChange(newCandidate)
                onEditingEntryChange(Pair(null, newCandidate))
            }
            SwipeFocusable(
                element = EdgeEditorElement.RotationAddCandidate,
                shape = RoundedCornerShape(8.dp),
                showBorderHighlight = false,
                onActivate = addCandidateAction,
                gridRow = 60,
            ) {
            val addFocused = LocalSwipeFocused.current
            Button(
                onClick = addCandidateAction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (addFocused) cs.primary else cs.primary.copy(alpha = 0.12f),
                    contentColor = if (addFocused) cs.onPrimary else cs.primary
                ),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                Icon(Icons.Filled.Add, null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("후보 추가", fontSize = 12.sp)
            }
            }

            // ── 인터벌 설정 (프리셋 칩 + 직접 슬라이더) ──
            Text("후보 1개당 머무는 시간", fontSize = 12.sp, color = cs.onSurfaceVariant)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                intervalPresets.forEach { preset ->
                    val isSelected = rotation.intervalMs == preset.ms && !showCustomSlider
                    val presetAction: () -> Unit = {
                        onBeforeIntervalChange?.invoke()
                        onRotationChanged(rotation.copy(intervalMs = preset.ms))
                        showCustomSlider = false
                    }
                    SwipeFocusable(
                        element = EdgeEditorElement.RotationIntervalPreset(preset.ms),
                        shape = RoundedCornerShape(8.dp),
                        showBorderHighlight = false,
                        onActivate = presetAction,
                        gridRow = 61,
                        modifier = Modifier.weight(1f),
                    ) {
                    val presetFocused = LocalSwipeFocused.current
                    val bgColor = when {
                        isSelected -> cs.primary
                        presetFocused -> cs.primary.copy(alpha = 0.25f)
                        else -> cs.surfaceVariant
                    }
                    val labelColor = when {
                        isSelected -> cs.onPrimary
                        presetFocused -> cs.primary
                        else -> cs.onSurface
                    }
                    val subColor = when {
                        isSelected -> cs.onPrimary.copy(alpha = 0.8f)
                        presetFocused -> cs.primary.copy(alpha = 0.8f)
                        else -> cs.onSurfaceVariant
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(bgColor)
                            .clickable(onClick = presetAction)
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(preset.label, fontSize = 11.sp, color = labelColor)
                            Text("${preset.ms}ms", fontSize = 9.sp, color = subColor) // 기본값: 9.sp
                        }
                    }
                    }
                }
                // 직접 입력 칩
                val isCustomSelected = showCustomSlider
                val customChipAction: () -> Unit = { showCustomSlider = true }
                SwipeFocusable(
                    element = EdgeEditorElement.RotationIntervalCustom,
                    shape = RoundedCornerShape(8.dp),
                    showBorderHighlight = false,
                    onActivate = customChipAction,
                    gridRow = 61,
                    modifier = Modifier.weight(1f),
                ) {
                val customFocused = LocalSwipeFocused.current
                val customBgColor = when {
                    isCustomSelected -> cs.primary
                    customFocused -> cs.primary.copy(alpha = 0.25f)
                    else -> cs.surfaceVariant
                }
                val customLabelColor = when {
                    isCustomSelected -> cs.onPrimary
                    customFocused -> cs.primary
                    else -> cs.onSurface
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(customBgColor)
                        .clickable(onClick = customChipAction)
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("직접", fontSize = 11.sp, color = customLabelColor)
                        if (isCustomSelected) {
                            Text("${rotation.intervalMs}ms", fontSize = 9.sp, color = cs.onPrimary.copy(alpha = 0.8f)) // 기본값: 9.sp
                        }
                    }
                }
                }
            }
            if (showCustomSlider) {
                val intervalSwipeController = LocalSwipeFocusController.current
                SwipeFocusable(
                    element = EdgeEditorElement.RotationIntervalSlider,
                    shape = RoundedCornerShape(8.dp),
                    showBorderHighlight = true,
                    manipulatable = true,
                    onManipulate = { deltaPx, screenWidthPx ->
                        val rangeSpan = maxMs - minMs
                        val deltaValue = (deltaPx / screenWidthPx) * rangeSpan
                        val newValue = (rotation.intervalMs + deltaValue).coerceIn(minMs, maxMs)
                        onBeforeIntervalChange?.invoke()
                        onRotationChanged(rotation.copy(intervalMs = newValue.toInt()))
                    },
                    gridRow = 62,
                ) {
                val isFocused = LocalSwipeFocused.current
                val inManip = isFocused && intervalSwipeController?.mode == SwipeMode.MANIPULATION
                val lineWidthDp by animateDpAsState(
                    targetValue = when {
                        inManip -> 6.dp
                        isFocused -> 4.dp
                        else -> CUSTOM_SLIDER_LINE_WIDTH_DP.dp
                    },
                    label = "intervalSliderLineWidth",
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 2.dp, end = 6.dp, top = 2.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .weight(1f)
                            .height(CUSTOM_SLIDER_TRACK_HEIGHT_DP.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .pointerInput(onRotationChanged) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    down.consume()
                                    fun applyX(x: Float) {
                                        val fr = (x / size.width).coerceIn(0f, 1f)
                                        val v = minMs + fr * (maxMs - minMs)
                                        onBeforeIntervalChange?.invoke()
                                        onRotationChanged(rotation.copy(intervalMs = v.toInt()))
                                    }
                                    applyX(down.position.x)
                                    drag(down.id) { change ->
                                        change.consume()
                                        applyX(change.position.x)
                                    }
                                }
                            }
                    ) {
                        val trackWidthPx = constraints.maxWidth
                        val thumbFraction = ((rotation.intervalMs - minMs) / (maxMs - minMs)).coerceIn(0f, 1f)
                        val trackBgColor = when {
                            inManip -> cs.primaryContainer.copy(alpha = 0.5f)
                            isFocused -> cs.primaryContainer.copy(alpha = 0.25f)
                            else -> cs.surfaceVariant
                        }
                        Box(Modifier.matchParentSize().background(trackBgColor))
                        Box(
                            Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(thumbFraction)
                                .background(cs.primary)
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .offset {
                                    val lineWidthPx = lineWidthDp.roundToPx()
                                    IntOffset(
                                        (thumbFraction * trackWidthPx - lineWidthPx / 2f)
                                            .roundToInt()
                                            .coerceIn(0, (trackWidthPx - lineWidthPx).coerceAtLeast(0)),
                                        0,
                                    )
                                }
                                .fillMaxHeight()
                                .width(lineWidthDp)
                                .background(if (isFocused || inManip) Color.White else Color.White.copy(alpha = 0.7f))
                        )
                    }
                    Text(
                        "${rotation.intervalMs}ms",
                        fontSize = 13.sp,
                        color = cs.onSurface,
                        modifier = Modifier.width(52.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                    )
                }
                }
            }
        }
    } else {
        // ── 후보 편집 폼 (액션 → 라벨/아이콘 순서) ──
        val (editIdx, _) = editingEntry!!
        val draftIsAutoLabel = draftCandidate.label.isEmpty()
        val draftIsAutoIcon = draftCandidate.iconKey.isEmpty()
        val draftDisplayLabel = draftCandidate.label.ifEmpty { draftCandidate.action.defaultLabel() }
        val draftDisplayIconKey = draftCandidate.iconKey.ifEmpty { draftCandidate.action.defaultIconKey() }
        val draftDisplayColorHex = draftCandidate.colorHex
        val draftHasUserColor = draftDisplayColorHex.isNotEmpty()
        val draftHasUserCustom = !draftIsAutoLabel || !draftIsAutoIcon || draftHasUserColor

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                if (editIdx == null) "후보 추가" else "후보 편집",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = cs.onSurface
            )

            // 1. 액션 선택
            ActionDomainPicker(
                current = draftCandidate.action,
                onSelect = onCandidateActionSelected,
                excludeDomains = setOf(ActionDomain.UNASSIGNED),
                pageCount = pageCount,
                inputMode = inputMode,
                onAddAsCandidate = null,
                onSwipeShortcutRequest = onSwipeShortcutRequest,
            )

            HorizontalDivider(color = cs.outline.copy(alpha = 0.2f))

            // 2. 표시 설정 (라벨 + 아이콘)
            Text("표시 설정", fontSize = 12.sp, color = cs.onSurfaceVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 아이콘 박스
                val iconBoxAction: () -> Unit = { onRequestCandidateIconPicker() }
                SwipeFocusable(
                    element = EdgeEditorElement.RotationCandidateIconBox,
                    shape = RoundedCornerShape(8.dp),
                    showBorderHighlight = true,
                    onActivate = iconBoxAction,
                    gridRow = 36,
                    modifier = Modifier.onGloballyPositioned { coords ->
                        val b = coords.boundsInWindow()
                        iconBoxAnchorReport(
                            Offset((b.left + b.right) / 2f, (b.top + b.bottom) / 2f)
                        )
                    },
                ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(cs.surfaceVariant)
                        .border(0.5.dp, cs.outline.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .clickable(onClick = iconBoxAction),
                    contentAlignment = Alignment.Center
                ) {
                    if (draftDisplayIconKey.isNotEmpty()) {
                        Icon(
                            imageVector = IconRegistry.get(draftDisplayIconKey),
                            contentDescription = null,
                            tint = if (draftIsAutoIcon) cs.onSurface.copy(alpha = 0.6f) else cs.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            tint = cs.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    if (draftIsAutoIcon && draftDisplayIconKey.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(3.dp)
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(cs.tertiaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("A", fontSize = 7.sp, color = cs.onTertiaryContainer, fontWeight = FontWeight.Bold) // 기본값: 7.sp
                        }
                    }
                }
                }

                // 컬러 박스
                val colorBoxAction: () -> Unit = { onRequestCandidateColorPicker() }
                SwipeFocusable(
                    element = EdgeEditorElement.RotationCandidateColorBox,
                    shape = RoundedCornerShape(8.dp),
                    showBorderHighlight = true,
                    onActivate = colorBoxAction,
                    gridRow = 36,
                    modifier = Modifier.onGloballyPositioned { coords ->
                        val b = coords.boundsInWindow()
                        colorBoxAnchorReport(
                            Offset((b.left + b.right) / 2f, (b.top + b.bottom) / 2f)
                        )
                    },
                ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(cs.surfaceVariant)
                        .border(0.5.dp, cs.outline.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .clickable(onClick = colorBoxAction),
                    contentAlignment = Alignment.Center
                ) {
                    if (draftHasUserColor) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    com.bridgeone.app.ui.common.ColorCodec.hexToColorOrNull(draftDisplayColorHex)
                                        ?: cs.primary
                                )
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "색상 선택",
                            tint = cs.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                }

                // 라벨 박스
                val labelBoxAction: () -> Unit = {
                    onRequestLabelKeyboard(draftCandidate.label) { result ->
                        onDraftChange(draftCandidate.copy(label = result))
                    }
                }
                SwipeFocusable(
                    element = EdgeEditorElement.RotationCandidateLabelBox,
                    shape = RoundedCornerShape(8.dp),
                    showBorderHighlight = true,
                    onActivate = labelBoxAction,
                    gridRow = 36,
                    modifier = Modifier.weight(1f),
                ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(cs.surfaceVariant)
                        .border(0.5.dp, cs.outline.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .clickable(onClick = labelBoxAction)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 텍스트 + 커서 (남는 폭 차지, 텍스트만 말줄임)
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = draftDisplayLabel.ifEmpty { "라벨 입력..." },
                                fontSize = 14.sp,
                                color = if (draftDisplayLabel.isEmpty()) cs.onSurfaceVariant.copy(alpha = 0.5f)
                                        else if (draftIsAutoLabel) cs.onSurface.copy(alpha = 0.7f)
                                        else cs.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            // 편집 중 커서
                            if (isEditingLabel) {
                                Box(
                                    modifier = Modifier
                                        .width(1.5.dp)
                                        .height(16.dp)
                                        .background(cs.primary.copy(alpha = labelCursorAlpha))
                                )
                            }
                        }
                        // 자동 배지 — 항상 오른쪽 고정
                        if (draftIsAutoLabel && draftDisplayLabel.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(cs.tertiaryContainer)
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text("자동", fontSize = 9.sp, color = cs.onTertiaryContainer) // 기본값: 9.sp
                            }
                        }
                    }
                }
                } // SwipeFocusable(RotationCandidateLabelBox) 닫기

                // 자동으로 되돌리기 (사용자 지정 상태일 때만)
                if (draftHasUserCustom) {
                    val revertCandidateAction: () -> Unit = {
                        onDraftChange(draftCandidate.copy(label = "", iconKey = "", colorHex = ""))
                    }
                    SwipeFocusable(
                        element = EdgeEditorElement.RotationCandidateRevertToAuto,
                        shape = RoundedCornerShape(20.dp),
                        onActivate = revertCandidateAction,
                        gridRow = 36,
                    ) {
                    val revertCandidateFocused = LocalSwipeFocused.current
                    IconButton(
                        onClick = revertCandidateAction,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "자동으로 되돌리기",
                            tint = if (revertCandidateFocused) cs.primary else cs.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val cancelAction: () -> Unit = { onEditingEntryChange(null) }
                val applyAction: () -> Unit = {
                    val isDuplicate = rotation.candidates.withIndex().any { (idx, c) ->
                        idx != editIdx && c.action == draftCandidate.action
                    }
                    if (isDuplicate) {
                        ToastController.show("이미 추가된 액션입니다", ToastType.ERROR)
                    } else {
                        val newList = rotation.candidates.toMutableList()
                        if (editIdx == null) newList.add(draftCandidate) else newList[editIdx] = draftCandidate
                        onApply?.invoke()
                        onRotationChanged(rotation.copy(candidates = newList))
                        onEditingEntryChange(null)
                    }
                }
                SwipeFocusable(
                    element = EdgeEditorElement.RotationCancelEdit,
                    shape = RoundedCornerShape(8.dp),
                    showBorderHighlight = true,
                    onActivate = cancelAction,
                    gridRow = 95,
                    modifier = Modifier.weight(1f),
                ) {
                TextButton(
                    onClick = cancelAction,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("취소") }
                }
                SwipeFocusable(
                    element = EdgeEditorElement.RotationApplyEdit,
                    shape = RoundedCornerShape(8.dp),
                    showBorderHighlight = true,
                    onActivate = applyAction,
                    gridRow = 95,
                    modifier = Modifier.weight(1f),
                ) {
                FilledTonalButton(
                    onClick = applyAction,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (editIdx == null) "추가" else "적용") }
                }
            }
        }
    }
}
