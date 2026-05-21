package com.bridgeone.app.ui.components.touchpad

import android.annotation.SuppressLint
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bridgeone.app.ui.common.DYNAMICS_PRESETS
import com.bridgeone.app.ui.common.EdgeSwipeConstants
import com.bridgeone.app.ui.common.EdgeZonePreset
import com.bridgeone.app.ui.common.EdgeZonePresetsRepository
import com.bridgeone.app.ui.common.MODE_PRESETS
import com.bridgeone.app.ui.components.SwipeKeyboardOverlay

// ── 영역 비율 인라인 액션 팝업 상태 ──
private sealed class ZoneActionPopup {
    object None : ZoneActionPopup()
    data class Initial(val zone: EdgeZone, val anchor: Float) : ZoneActionPopup()
    data class MergeSelecting(val zone: EdgeZone) : ZoneActionPopup()
    data class SplitChoosing(val zone: EdgeZone, val anchor: Float) : ZoneActionPopup()
    data class DeleteConfirming(val zone: EdgeZone, val anchor: Float) : ZoneActionPopup()
}

/**
 * 엣지 존 풀스크린 편집기 (UI/UX 리디자인).
 *
 * @param initialConfig  편집 시작 시 로드할 설정
 * @param onSave         저장 확정 시 호출
 * @param onBack         뒤로/취소
 */
@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun EdgeZoneEditorScreen(
    initialConfig: EdgeZoneConfig,
    initialPresetId: String? = null,
    presetsRepo: EdgeZonePresetsRepository? = null,
    disabledEdges: Map<EntryEdge, String> = emptyMap(),
    bottomLeftButtonLabel: String? = "다이나믹스",
    bottomRightButtonLabel: String? = "모드 프리셋",
    onSave: (EdgeZoneConfig, presetId: String?) -> Unit,
    onBack: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val minRatio = EdgeSwipeConstants.MIN_ZONE_RATIO
    val maxZones = EdgeSwipeConstants.MAX_ZONES_PER_EDGE.toInt()

    var workConfig by remember(initialConfig) { mutableStateOf(initialConfig) }
    var selectedZone by remember { mutableStateOf<EdgeZone?>(null) }
    var currentPresetId by remember { mutableStateOf(initialPresetId) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showIconSheet by remember { mutableStateOf(false) }
    var showLabelKeyboard by remember { mutableStateOf(false) }
    var showPresetPopup by remember { mutableStateOf(false) }
    var showUndoMenu by remember { mutableStateOf(false) }
    var zonePopup by remember { mutableStateOf<ZoneActionPopup>(ZoneActionPopup.None) }
    var canvasVisible by remember { mutableStateOf(true) }
    var selectedEdge by remember { mutableStateOf<EntryEdge?>(null) }
    val iconSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var undoStack by remember { mutableStateOf(listOf<EdgeZoneConfig>()) }

    fun pushUndo() {
        undoStack = (listOf(workConfig) + undoStack).take(20)
    }

    // 분할 N개 균등 분할 (첫 조각만 기존 trigger 유지)
    fun splitInto(zone: EdgeZone, n: Int) {
        val zones = workConfig.zonesFor(zone.edge).toMutableList()
        val idx = zones.indexOfFirst { it.startRatio == zone.startRatio && it.edge == zone.edge }
        if (idx < 0) return
        val w = (zone.endRatio - zone.startRatio) / n
        val parts = (0 until n).map { i ->
            val s = zone.startRatio + i * w
            val e = if (i == n - 1) zone.endRatio else s + w
            if (i == 0) zone.copy(endRatio = e)
            else EdgeZone(zone.edge, s, e, EdgeZoneTrigger.SingleAction(EdgeZoneAction.Unassigned, "", ""))
        }
        zones.removeAt(idx)
        zones.addAll(idx, parts)
        pushUndo()
        workConfig = workConfig.withZones(zone.edge, zones)
        currentPresetId = null
        selectedZone = parts.first()
        zonePopup = ZoneActionPopup.None
    }

    // 인접 존 흡수 병합
    fun tryMergeWith(base: EdgeZone, target: EdgeZone) {
        if (base.edge != target.edge) return
        val zones = workConfig.zonesFor(base.edge).toMutableList()
        val bi = zones.indexOfFirst { it.startRatio == base.startRatio }
        val ti = zones.indexOfFirst { it.startRatio == target.startRatio }
        if (bi < 0 || ti < 0 || kotlin.math.abs(bi - ti) != 1) return
        val merged = if (ti < bi) base.copy(startRatio = target.startRatio)
                     else base.copy(endRatio = target.endRatio)
        zones[bi] = merged
        zones.removeAt(ti)
        pushUndo()
        workConfig = workConfig.withZones(base.edge, zones)
        currentPresetId = null
        selectedZone = merged
        zonePopup = ZoneActionPopup.None
    }

    BackHandler(enabled = zonePopup !is ZoneActionPopup.None || selectedZone != null || !canvasVisible) {
        if (zonePopup !is ZoneActionPopup.None) zonePopup = ZoneActionPopup.None
        else { selectedZone = null; canvasVisible = true; selectedEdge = null }
    }

    val hasChanges = workConfig != initialConfig
    val hasInvalidRotation = listOf(
        workConfig.topZones, workConfig.bottomZones,
        workConfig.leftZones, workConfig.rightZones
    ).flatten().any { zone ->
        val t = zone.trigger
        t is EdgeZoneTrigger.Rotation && t.candidates.size < EdgeSwipeConstants.EDGE_ZONE_ROTATION_MIN_CANDIDATES
    }
    val canSave = hasChanges && !hasInvalidRotation

    // ── 존 삭제 ──
    fun deleteZone(zone: EdgeZone) {
        val zones = workConfig.zonesFor(zone.edge).toMutableList()
        val idx = zones.indexOf(zone)
        if (idx < 0 || zones.size <= 1) return
        val removed = zones.removeAt(idx)
        if (idx < zones.size) zones[idx] = zones[idx].copy(startRatio = removed.startRatio)
        else zones[idx - 1] = zones[idx - 1].copy(endRatio = removed.endRatio)
        pushUndo()
        workConfig = workConfig.withZones(zone.edge, zones)
        if (selectedZone == zone) selectedZone = null
    }

    // ── 존 비율 프리셋 적용 ──
    fun applyRatioPreset(edge: EntryEdge, ratios: List<Float>) {
        val zones = workConfig.zonesFor(edge).toList()
        if (zones.size != ratios.size) return
        var cum = 0f
        val newZones = zones.mapIndexed { i, zone ->
            val s = cum
            cum += ratios[i]
            val e = if (i == zones.size - 1) 1f else cum
            zone.copy(startRatio = s, endRatio = e)
        }
        val curSel = selectedZone
        val selIdx = if (curSel != null) zones.indexOfFirst { it.startRatio == curSel.startRatio && it.edge == curSel.edge } else -1
        pushUndo()
        workConfig = workConfig.withZones(edge, newZones)
        currentPresetId = null
        selectedZone = if (selIdx >= 0) newZones[selIdx] else null
    }

    // ── 선택 존 업데이트 ──
    fun updateSelectedZone(updated: EdgeZone) {
        val zones = workConfig.zonesFor(updated.edge).toMutableList()
        val idx = zones.indexOfFirst { it.startRatio == updated.startRatio && it.edge == updated.edge }
        if (idx >= 0) {
            zones[idx] = updated
            pushUndo()
            workConfig = workConfig.withZones(updated.edge, zones)
            currentPresetId = presetsRepo?.loadAll()?.find { it.config == workConfig }?.id
        }
        selectedZone = updated
    }

    Column(
        modifier = Modifier.fillMaxSize().background(cs.background)
    ) {
        // ── TopAppBar ──
        Surface(
            color = cs.surfaceVariant,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (selectedZone != null || !canvasVisible) { selectedZone = null; canvasVisible = true; selectedEdge = null }
                    else if (hasChanges) showDiscardDialog = true
                    else onBack()
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                }
                Text(
                    text = "엣지 존 편집",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = cs.onSurface,
                    modifier = Modifier.weight(1f).padding(start = 4.dp)
                )
                if (presetsRepo != null) {
                    val activePreset = remember(currentPresetId) {
                        if (currentPresetId != null) presetsRepo.findById(currentPresetId) else null
                    }
                    if (activePreset != null) {
                        Row(
                            modifier = Modifier
                                .height(32.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(cs.primaryContainer)
                                .clickable { showPresetPopup = true }
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = IconRegistry.get(activePreset.iconKey),
                                contentDescription = null,
                                tint = cs.onPrimaryContainer,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = activePreset.name,
                                fontSize = 12.sp,
                                color = cs.onPrimaryContainer,
                                maxLines = 1
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .height(32.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(cs.surfaceVariant)
                                .border(0.5.dp, cs.outline.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                .clickable { showPresetPopup = true }
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Tune,
                                contentDescription = null,
                                tint = cs.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "커스텀",
                                fontSize = 12.sp,
                                color = cs.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
                Box {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .combinedClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = false),
                                enabled = undoStack.isNotEmpty(),
                                onClick = {
                                    val prev = undoStack.firstOrNull()
                                    if (prev != null) {
                                        workConfig = prev
                                        undoStack = undoStack.drop(1)
                                        currentPresetId = null
                                        val sel = selectedZone
                                        selectedZone = if (sel != null) {
                                            prev.zonesFor(sel.edge).firstOrNull { it.startRatio == sel.startRatio }
                                                ?: prev.zonesFor(sel.edge).firstOrNull()
                                        } else null
                                    }
                                },
                                onLongClick = { if (undoStack.size > 1) showUndoMenu = true }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "실행 취소",
                            tint = if (undoStack.isNotEmpty()) cs.onSurface else cs.onSurface.copy(alpha = 0.38f)
                        )
                    }
                    DropdownMenu(
                        expanded = showUndoMenu,
                        onDismissRequest = { showUndoMenu = false }
                    ) {
                        undoStack.forEachIndexed { idx, config ->
                            val newerConfig = if (idx == 0) workConfig else undoStack[idx - 1]
                            val desc = describeUndoStep(from = config, to = newerConfig)
                            DropdownMenuItem(
                                text = { Text(desc) },
                                onClick = {
                                    workConfig = config
                                    undoStack = undoStack.drop(idx + 1)
                                    currentPresetId = null
                                    val sel = selectedZone
                                    selectedZone = if (sel != null) {
                                        config.zonesFor(sel.edge).firstOrNull { it.startRatio == sel.startRatio }
                                            ?: config.zonesFor(sel.edge).firstOrNull()
                                    } else null
                                    showUndoMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // ── 메인 영역: 캔버스 + 편집 패널 ──
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().weight(1f)) {
            val totalH = maxHeight
            val touchpadAspectRatio = LocalConfiguration.current.let {
                it.screenWidthDp.toFloat() / it.screenHeightDp.toFloat()
            }
            val isEditing = selectedZone != null || selectedEdge != null
            val canvasH by animateDpAsState(
                targetValue = if (canvasVisible) {
                    if (isEditing) totalH * 0.55f else totalH
                } else 0.dp,
                animationSpec = tween(280),
                label = "canvasHeight"
            )
            Column(modifier = Modifier.fillMaxSize()) {
                if (canvasH > 0.dp) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(canvasH),
                        contentAlignment = Alignment.Center
                    ) {
                        if (canvasVisible) {
                            Box(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .aspectRatio(touchpadAspectRatio, matchHeightConstraintsFirst = true)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1A1A1A))
                            ) {
                                EdgeZoneEditorPreviewCanvas(
                                    config = workConfig,
                                    selectedZone = selectedZone,
                                    disabledEdges = disabledEdges,
                                    bottomLeftButtonLabel = bottomLeftButtonLabel,
                                    bottomRightButtonLabel = bottomRightButtonLabel,
                                    onZoneTapped = { selectedZone = it; selectedEdge = it.edge; canvasVisible = false },
                                    onCornerPriorityToggled = { corner ->
                                        pushUndo()
                                        workConfig = workConfig.toggleCornerPriority(corner)
                                        currentPresetId = null
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                                if (!isEditing) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.TouchApp,
                                                contentDescription = null,
                                                tint = cs.onSurfaceVariant.copy(alpha = 0.4f),
                                                modifier = Modifier.size(36.dp)
                                            )
                                            Text(
                                                text = "존을 탭해 편집",
                                                fontSize = 13.sp,
                                                color = cs.onSurfaceVariant,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            // 축소 애니메이션 중: 단순 배경만 표시 (aspectRatio 없음)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight()
                                    .padding(horizontal = 12.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1A1A1A))
                            )
                        }
                    }
                }

        // ── 선택된 존 편집 패널 ──
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            val sel = selectedZone
            val edgeForStrip = sel?.edge ?: selectedEdge

            if (showLabelKeyboard && sel != null) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 0.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(cs.surfaceVariant)
                                .border(0.5.dp, cs.outline.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (sel.iconKey.isNotEmpty()) {
                                Icon(
                                    imageVector = IconRegistry.get(sel.iconKey),
                                    contentDescription = null,
                                    tint = cs.onSurface,
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
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(cs.primary.copy(alpha = 0.08f))
                                .border(1.dp, cs.primary.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = sel.label.ifEmpty { "라벨 입력..." },
                                fontSize = 14.sp,
                                color = if (sel.label.isEmpty()) cs.onSurfaceVariant.copy(alpha = 0.5f) else cs.onSurface
                            )
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        SwipeKeyboardOverlay(
                            initialText = sel.label,
                            maxLength = EdgeSwipeConstants.EDGE_ZONE_LABEL_MAX_LEN,
                            suggestions = EdgeSwipeConstants.EDGE_ZONE_LABEL_SUGGESTIONS,
                            revertOnCancel = false,
                            onTextChange = { text -> updateSelectedZone(sel.withLabel(text)) },
                            onCancel = { showLabelKeyboard = false },
                            onDone = { result ->
                                updateSelectedZone(sel.withLabel(result))
                                showLabelKeyboard = false
                            }
                        )
                    }
                }
            } else if (sel != null || edgeForStrip != null) {
                val zoneList = workConfig.zonesFor(edgeForStrip!!)
                val blockedRatio = EdgeSwipeConstants.CORNER_BUTTON_BLOCKED_RATIO
                val cornerRatio = EdgeSwipeConstants.EDGE_CORNER_OVERLAP_RATIO
                fun cornerPriority(c: CornerOverlap) = workConfig.cornerPriority[c] ?: defaultCornerEdge(c)
                val stripBlockedStart = when (edgeForStrip) {
                    EntryEdge.BOTTOM -> if (bottomLeftButtonLabel != null) blockedRatio
                                        else if (cornerPriority(CornerOverlap.BOTTOM_LEFT) != EntryEdge.BOTTOM) cornerRatio
                                        else 0f
                    EntryEdge.TOP    -> if (cornerPriority(CornerOverlap.TOP_LEFT) != EntryEdge.TOP)   cornerRatio else 0f
                    EntryEdge.LEFT   -> if (cornerPriority(CornerOverlap.TOP_LEFT) != EntryEdge.LEFT)  cornerRatio else 0f
                    EntryEdge.RIGHT  -> if (cornerPriority(CornerOverlap.TOP_RIGHT) != EntryEdge.RIGHT) cornerRatio else 0f
                }
                val stripBlockedStartLabel = when (edgeForStrip) {
                    EntryEdge.BOTTOM -> if (bottomLeftButtonLabel != null) bottomLeftButtonLabel else if (stripBlockedStart > 0f) "코너" else null
                    else             -> if (stripBlockedStart > 0f) "코너" else null
                }
                val stripBlockedEnd = when (edgeForStrip) {
                    EntryEdge.LEFT   -> if (bottomLeftButtonLabel != null) blockedRatio
                                        else if (cornerPriority(CornerOverlap.BOTTOM_LEFT) != EntryEdge.LEFT) cornerRatio
                                        else 0f
                    EntryEdge.RIGHT  -> if (bottomRightButtonLabel != null) blockedRatio
                                        else if (cornerPriority(CornerOverlap.BOTTOM_RIGHT) != EntryEdge.RIGHT) cornerRatio
                                        else 0f
                    EntryEdge.BOTTOM -> if (bottomRightButtonLabel != null) blockedRatio
                                        else if (cornerPriority(CornerOverlap.BOTTOM_RIGHT) != EntryEdge.BOTTOM) cornerRatio
                                        else 0f
                    EntryEdge.TOP    -> if (cornerPriority(CornerOverlap.TOP_RIGHT) != EntryEdge.TOP) cornerRatio else 0f
                }
                val stripBlockedEndLabel = when (edgeForStrip) {
                    EntryEdge.LEFT   -> if (bottomLeftButtonLabel != null) bottomLeftButtonLabel else if (stripBlockedEnd > 0f) "코너" else null
                    EntryEdge.RIGHT  -> if (bottomRightButtonLabel != null) bottomRightButtonLabel else if (stripBlockedEnd > 0f) "코너" else null
                    EntryEdge.BOTTOM -> if (bottomRightButtonLabel != null) bottomRightButtonLabel else if (stripBlockedEnd > 0f) "코너" else null
                    EntryEdge.TOP    -> if (stripBlockedEnd > 0f) "코너" else null
                }
                var stripBounds by remember(edgeForStrip) { mutableStateOf(IntRect.Zero) }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (sel != null) {
                        // 라벨 + 아이콘 선택 한 줄
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(cs.surfaceVariant)
                                    .border(0.5.dp, cs.outline.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .clickable { showIconSheet = true },
                                contentAlignment = Alignment.Center
                            ) {
                                if (sel.iconKey.isNotEmpty()) {
                                    Icon(
                                        imageVector = IconRegistry.get(sel.iconKey),
                                        contentDescription = null,
                                        tint = cs.onSurface,
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
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(cs.surfaceVariant)
                                    .border(0.5.dp, cs.outline.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .clickable { showLabelKeyboard = true }
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = sel.label.ifEmpty { "라벨 입력..." },
                                    fontSize = 14.sp,
                                    color = if (sel.label.isEmpty()) cs.onSurfaceVariant.copy(alpha = 0.5f) else cs.onSurface
                                )
                            }
                        }
                    }

                    // ── 영역 비율: 스트립 + 길게 누름 팝업 ──
                    val zoneIdx = if (sel != null) zoneList.indexOfFirst { it.startRatio == sel.startRatio && it.edge == sel.edge } else -1
                    val highlightedZones: Set<Pair<EntryEdge, Float>> = run {
                        val p = zonePopup
                        if (sel != null && p is ZoneActionPopup.MergeSelecting) {
                            buildSet {
                                if (zoneIdx > 0) add(zoneList[zoneIdx - 1].edge to zoneList[zoneIdx - 1].startRatio)
                                if (zoneIdx in 0 until zoneList.size - 1) add(zoneList[zoneIdx + 1].edge to zoneList[zoneIdx + 1].startRatio)
                            }
                        } else emptySet()
                    }

                    Text("영역 비율", fontSize = 12.sp, color = cs.onSurfaceVariant)

                    val p = zonePopup
                    var presetMenuOpen by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // ── 스트립 ──
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .onGloballyPositioned { coords ->
                                    val pos = coords.positionInWindow()
                                    stripBounds = IntRect(
                                        left = pos.x.toInt(),
                                        top = pos.y.toInt(),
                                        right = (pos.x + coords.size.width).toInt(),
                                        bottom = (pos.y + coords.size.height).toInt()
                                    )
                                }
                        ) {
                            EdgeStripEditor(
                                zones = zoneList,
                                selectedZone = sel,
                                minRatio = minRatio,
                                onZonesChanged = { newZones ->
                                    pushUndo()
                                    workConfig = workConfig.withZones(edgeForStrip, newZones)
                                    currentPresetId = null
                                    val curSel = selectedZone
                                    selectedZone = if (curSel != null) {
                                        newZones.firstOrNull { curSel.startRatio in it.startRatio..it.endRatio }
                                            ?: newZones.firstOrNull()
                                    } else null
                                },
                                onZoneSelected = { tapped ->
                                    val cur = zonePopup
                                    if (cur is ZoneActionPopup.MergeSelecting) {
                                        tryMergeWith(cur.zone, tapped)
                                    } else {
                                        selectedZone = tapped
                                        zonePopup = ZoneActionPopup.None
                                    }
                                },
                                onZoneDeselected = {
                                    selectedZone = null
                                    zonePopup = ZoneActionPopup.None
                                },
                                onZoneLongPressed = { zone, cf ->
                                    selectedZone = zone
                                    zonePopup = ZoneActionPopup.Initial(zone, cf)
                                },
                                highlightedZones = highlightedZones,
                                blockedStartRatio = stripBlockedStart,
                                blockedStartLabel = stripBlockedStartLabel,
                                blockedEndRatio = stripBlockedEnd,
                                blockedEndLabel = stripBlockedEndLabel,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // ── 비율 프리셋 드롭다운 ──
                        if (zoneList.size >= 2) {
                            Box {
                                IconButton(
                                    onClick = { presetMenuOpen = true },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.BarChart,
                                        contentDescription = "비율 프리셋",
                                        tint = cs.onSurfaceVariant
                                    )
                                }
                                DropdownMenu(
                                    expanded = presetMenuOpen,
                                    onDismissRequest = { presetMenuOpen = false }
                                ) {
                                    ratioPresetsFor(zoneList.size).forEach { (label, ratios) ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    MiniRatioBar(
                                                        ratios = ratios,
                                                        modifier = Modifier
                                                            .width(40.dp)
                                                            .height(10.dp)
                                                    )
                                                    Text(label, fontSize = 13.sp)
                                                }
                                            },
                                            onClick = {
                                                applyRatioPreset(edgeForStrip, ratios)
                                                presetMenuOpen = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ── 팝업 (Popup 컴포저블로 부유 — 외부 탭 시 자동 해제) ──
                    if (sel != null && p !is ZoneActionPopup.None) {
                        val anchor = when (p) {
                            is ZoneActionPopup.Initial -> p.anchor
                            is ZoneActionPopup.SplitChoosing -> p.anchor
                            is ZoneActionPopup.DeleteConfirming -> p.anchor
                            else -> 0.5f
                        }
                        Popup(
                            popupPositionProvider = remember(anchor, stripBounds) {
                                object : PopupPositionProvider {
                                    override fun calculatePosition(
                                        anchorBounds: IntRect,
                                        windowSize: IntSize,
                                        layoutDirection: LayoutDirection,
                                        popupContentSize: IntSize
                                    ): IntOffset {
                                        val x = (stripBounds.left + anchor * stripBounds.width - popupContentSize.width / 2f)
                                            .toInt()
                                            .coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
                                        val y = stripBounds.top - popupContentSize.height - 4
                                        return IntOffset(x, y)
                                    }
                                }
                            },
                            onDismissRequest = { zonePopup = ZoneActionPopup.None },
                            properties = PopupProperties(
                                focusable = true,
                                dismissOnClickOutside = true,
                                dismissOnBackPress = false
                            )
                        ) {
                            Surface(
                                modifier = Modifier
                                    .wrapContentWidth()
                                    .border(0.5.dp, cs.outline.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                                shape = RoundedCornerShape(10.dp),
                                color = cs.surfaceVariant,
                                tonalElevation = 6.dp,
                                shadowElevation = 8.dp
                            ) {
                                when (p) {
                                    is ZoneActionPopup.Initial -> {
                                        val hasAdj = zoneList.size > 1
                                        val anySplitValid = (2..4).any { n ->
                                            zoneList.size + n - 1 <= maxZones &&
                                            (sel.endRatio - sel.startRatio) / n >= minRatio
                                        }
                                        val canDel = zoneList.size > 1
                                        val divider: @Composable () -> Unit = {
                                            Box(
                                                modifier = Modifier
                                                    .width(0.5.dp)
                                                    .height(16.dp)
                                                    .background(cs.onSurfaceVariant.copy(alpha = 0.25f))
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp),
                                            horizontalArrangement = Arrangement.spacedBy(0.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TextButton(
                                                onClick = { zonePopup = ZoneActionPopup.MergeSelecting(sel) },
                                                enabled = hasAdj,
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                            ) { Text("병합", fontSize = 12.sp) }
                                            divider()
                                            TextButton(
                                                onClick = { zonePopup = ZoneActionPopup.SplitChoosing(sel, p.anchor) },
                                                enabled = anySplitValid,
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                            ) { Text("분할", fontSize = 12.sp) }
                                            divider()
                                            TextButton(
                                                onClick = { zonePopup = ZoneActionPopup.DeleteConfirming(sel, p.anchor) },
                                                enabled = canDel,
                                                colors = ButtonDefaults.textButtonColors(contentColor = cs.error),
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                            ) { Text("삭제", fontSize = 12.sp) }
                                        }
                                    }
                                    is ZoneActionPopup.MergeSelecting -> {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text("병합할 존을 선택하세요", fontSize = 12.sp, color = cs.onSurfaceVariant)
                                            TextButton(
                                                onClick = { zonePopup = ZoneActionPopup.None },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                            ) { Text("취소", fontSize = 12.sp) }
                                        }
                                    }
                                    is ZoneActionPopup.SplitChoosing -> {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                            horizontalArrangement = Arrangement.spacedBy(0.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("분할:", fontSize = 12.sp, color = cs.onSurfaceVariant,
                                                modifier = Modifier.padding(start = 6.dp, end = 2.dp))
                                            (2..4).forEach { n ->
                                                val valid = zoneList.size + n - 1 <= maxZones &&
                                                    (sel.endRatio - sel.startRatio) / n >= minRatio
                                                TextButton(
                                                    onClick = { splitInto(sel, n) },
                                                    enabled = valid,
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                                ) { Text("$n", fontSize = 13.sp) }
                                            }
                                            TextButton(
                                                onClick = { zonePopup = ZoneActionPopup.None },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                            ) { Text("취소", fontSize = 12.sp) }
                                        }
                                    }
                                    is ZoneActionPopup.DeleteConfirming -> {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text("정말 삭제하시겠습니까?", fontSize = 12.sp, color = cs.onSurface)
                                            TextButton(
                                                onClick = { deleteZone(sel); zonePopup = ZoneActionPopup.None },
                                                colors = ButtonDefaults.textButtonColors(contentColor = cs.error),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                            ) { Text("예", fontSize = 12.sp) }
                                            TextButton(
                                                onClick = { zonePopup = ZoneActionPopup.None },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                            ) { Text("아니오", fontSize = 12.sp) }
                                        }
                                    }
                                    else -> {}
                                }
                            }
                        }
                    }

                    if (sel != null) {
                        // 트리거 방식 토글
                        val isSingleAction = sel.trigger is EdgeZoneTrigger.SingleAction
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(true to "단일 액션", false to "로테이션").forEach { (isSingle, label) ->
                                val selected = isSingleAction == isSingle
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) cs.primary.copy(alpha = 0.15f) else cs.surfaceVariant)
                                        .border(
                                            width = if (selected) 1.5.dp else 0.5.dp,
                                            color = if (selected) cs.primary else cs.outline.copy(alpha = 0.3f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            if (isSingle && !isSingleAction) {
                                                updateSelectedZone(sel.copy(trigger = EdgeZoneTrigger.SingleAction(EdgeZoneAction.Unassigned, sel.label, sel.iconKey)))
                                            } else if (!isSingle && isSingleAction) {
                                                val defaultInterval = EdgeSwipeConstants.EDGE_ZONE_ROTATION_INTERVAL_DEFAULT_MS
                                                updateSelectedZone(sel.copy(trigger = EdgeZoneTrigger.Rotation(emptyList(), defaultInterval)))
                                            }
                                        }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(label, fontSize = 12.sp, color = if (selected) cs.primary else cs.onSurface)
                                }
                            }
                        }

                        if (isSingleAction) {
                            Text("액션", fontSize = 12.sp, color = cs.onSurfaceVariant)
                            ActionCardGrid(
                                current = sel.action,
                                onSelect = { updateSelectedZone(sel.withAction(it)) }
                            )
                        } else {
                            val rotation = sel.trigger as EdgeZoneTrigger.Rotation
                            RotationEditor(
                                rotation = rotation,
                                onRotationChanged = { updateSelectedZone(sel.copy(trigger = it)) }
                            )
                        }
                    }
                }
            }
        }
            } // Column (BoxWithConstraints 내부)
        } // BoxWithConstraints

        // ── 하단 버튼 바 ──
        Surface(color = cs.surfaceVariant) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = { showResetDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = cs.surfaceVariant,
                        contentColor = cs.onSurface
                    ),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(4.dp)
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("기본값", fontSize = 12.sp)
                }
                Button(
                    onClick = { onSave(workConfig, currentPresetId) },
                    enabled = canSave,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = cs.primary,
                        contentColor = cs.onPrimary,
                        disabledContainerColor = cs.surfaceVariant,
                        disabledContentColor = cs.onSurfaceVariant
                    ),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(4.dp)
                ) {
                    Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("저장", fontSize = 12.sp)
                }
            }
        }
    }

    // ── 프리셋 팝업 ──
    if (showPresetPopup && presetsRepo != null) {
        EdgeZonePresetPopup(
            currentPresetId = currentPresetId,
            currentConfig = workConfig,
            presetsRepo = presetsRepo,
            onApply = { preset ->
                pushUndo()
                workConfig = preset.config
                currentPresetId = preset.id
                selectedZone = null
                showPresetPopup = false
            },
            onDismiss = { showPresetPopup = false }
        )
    }

    // ── 아이콘 선택 바텀시트 ──
    if (showIconSheet) {
        ModalBottomSheet(
            onDismissRequest = { showIconSheet = false },
            sheetState = iconSheetState,
            containerColor = cs.surfaceVariant
        ) {
            Text(
                text = "아이콘 선택",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = cs.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                contentPadding = PaddingValues(start = 12.dp, top = 0.dp, end = 12.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(IconRegistry.allKeys) { key ->
                    val isSelected = selectedZone?.iconKey == key
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) cs.secondary.copy(alpha = 0.2f) else cs.background)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) cs.secondary else cs.outline.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                selectedZone?.let { updateSelectedZone(it.withIconKey(key)) }
                                showIconSheet = false
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = IconRegistry.get(key),
                            contentDescription = key,
                            tint = if (isSelected) cs.secondary else cs.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }

    // ── 기본값 리셋 다이얼로그 ──
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            containerColor = cs.surfaceVariant,
            title = { Text("기본값으로 리셋", color = cs.onSurface) },
            text = { Text("모든 존 설정이 기본값으로 초기화됩니다.", color = cs.onSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = {
                    pushUndo()
                    workConfig = EdgeZoneConfig.default()
                    selectedZone = null
                    showResetDialog = false
                }) { Text("리셋", color = cs.error) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("취소") }
            }
        )
    }

    // ── 미저장 변경 경고 다이얼로그 ──
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            containerColor = cs.surfaceVariant,
            title = { Text("변경사항이 있습니다", color = cs.onSurface) },
            text = { Text("저장하지 않고 나가면 변경사항이 사라집니다.", color = cs.onSurfaceVariant) },
            confirmButton = {
                TextButton(
                    onClick = { if (!hasInvalidRotation) { onSave(workConfig, currentPresetId) }; showDiscardDialog = false },
                    enabled = !hasInvalidRotation
                ) {
                    Text("저장", color = if (!hasInvalidRotation) cs.primary else cs.onSurfaceVariant)
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { showDiscardDialog = false; onBack() }) {
                        Text("버리고 나가기", color = cs.error)
                    }
                    TextButton(onClick = { showDiscardDialog = false }) {
                        Text("취소")
                    }
                }
            }
        )
    }

}

// ============================================================
// 액션 섹션 카드 그리드
// ============================================================

private data class ActionOption(
    val label: String,
    val icon: ImageVector,
    val action: EdgeZoneAction
)

@Composable
private fun ActionCardGrid(
    current: EdgeZoneAction,
    onSelect: (EdgeZoneAction) -> Unit
) {
    val cs = MaterialTheme.colorScheme

    data class Section(val title: String, val items: List<ActionOption>)

    val sections = listOf(
        Section("모드 토글", listOf(
            ActionOption("클릭 모드", Icons.Filled.Mouse, EdgeZoneAction.ToggleMode(EdgeSwipeMode.CLICK)),
            ActionOption("스크롤 모드", Icons.Filled.SwapVert, EdgeZoneAction.ToggleMode(EdgeSwipeMode.SCROLL)),
            ActionOption("이동 모드", Icons.Filled.OpenWith, EdgeZoneAction.ToggleMode(EdgeSwipeMode.MOVE)),
        )),
        Section("프리셋 순환", listOf(
            ActionOption("다이나믹스", Icons.Filled.Timeline, EdgeZoneAction.CyclePreset(PresetType.DYNAMICS)),
            ActionOption("모드 프리셋", Icons.Filled.Tune, EdgeZoneAction.CyclePreset(PresetType.MODE)),
        )),
        Section("설정 순환", listOf(
            ActionOption("DPI", Icons.Filled.Speed, EdgeZoneAction.OpenSettings(SettingsType.DPI)),
            ActionOption("스크롤 속도", Icons.Filled.Loop, EdgeZoneAction.OpenSettings(SettingsType.SCROLL_SPEED)),
        )),
        Section("DPI 지정", DpiLevel.entries.map { level ->
            ActionOption(level.label, Icons.Filled.Speed, EdgeZoneAction.SetDpi(level))
        }),
        Section("스크롤 속도 지정", ScrollSensitivity.entries.map { sens ->
            ActionOption(sens.label, Icons.Filled.Loop, EdgeZoneAction.SetScrollSpeed(sens))
        }),
        Section("모드 프리셋 지정", MODE_PRESETS.mapIndexed { i, p ->
            ActionOption(p.name, p.icon.staticIcon, EdgeZoneAction.SetModePreset(i))
        }),
        Section("다이나믹스 지정", DYNAMICS_PRESETS.mapIndexed { i, p ->
            ActionOption(p.name, p.icon.staticIcon, EdgeZoneAction.SetDynamicsPreset(i))
        }),
        Section("클릭/이동/스크롤 지정", listOf(
            ActionOption("좌클릭", Icons.Filled.Mouse, EdgeZoneAction.SetClickMode(ClickMode.LEFT_CLICK)),
            ActionOption("우클릭", Icons.Filled.Mouse, EdgeZoneAction.SetClickMode(ClickMode.RIGHT_CLICK)),
            ActionOption("자유 이동", Icons.Filled.OpenWith, EdgeZoneAction.SetMoveMode(MoveMode.FREE)),
            ActionOption("직각 이동", Icons.Filled.OpenWith, EdgeZoneAction.SetMoveMode(MoveMode.RIGHT_ANGLE)),
            ActionOption("스크롤 끔", Icons.Filled.SwapVert, EdgeZoneAction.SetScrollMode(ScrollMode.OFF)),
            ActionOption("일반 스크롤", Icons.Filled.SwapVert, EdgeZoneAction.SetScrollMode(ScrollMode.NORMAL_SCROLL)),
            ActionOption("무한 스크롤", Icons.Filled.SwapVert, EdgeZoneAction.SetScrollMode(ScrollMode.INFINITE_SCROLL)),
        )),
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // 미할당 카드 (단독)
        val unassignedSelected = current is EdgeZoneAction.Unassigned
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (unassignedSelected) cs.secondary.copy(alpha = 0.12f) else cs.surfaceVariant)
                .border(
                    width = if (unassignedSelected) 1.5.dp else 0.5.dp,
                    color = if (unassignedSelected) cs.secondary else cs.outline.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp)
                )
                .clickable { onSelect(EdgeZoneAction.Unassigned) }
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                "미할당",
                fontSize = 13.sp,
                color = if (unassignedSelected) cs.secondary else cs.onSurface
            )
        }

        // 섹션별 2열 그리드
        sections.forEach { section ->
            Text(section.title, fontSize = 11.sp, color = cs.onSurfaceVariant)
            ActionCardRow(section.items, current, cs, onSelect)
        }
    }
}

@Composable
private fun ActionCardRow(
    items: List<ActionOption>,
    current: EdgeZoneAction,
    cs: androidx.compose.material3.ColorScheme,
    onSelect: (EdgeZoneAction) -> Unit
) {
    // 2열 배치
    val rows = items.chunked(2)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        rows.forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                rowItems.forEach { option ->
                    val isSelected = actionEquals(current, option.action)
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) cs.secondary.copy(alpha = 0.12f) else cs.surfaceVariant)
                            .border(
                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                color = if (isSelected) cs.secondary else cs.outline.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onSelect(option.action) }
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = option.icon,
                            contentDescription = null,
                            tint = if (isSelected) cs.secondary else cs.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            option.label,
                            fontSize = 12.sp,
                            color = if (isSelected) cs.secondary else cs.onSurface
                        )
                    }
                }
                // 홀수 아이템이면 빈 셀 추가
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private fun ratioPresetsFor(n: Int): List<Pair<String, List<Float>>> = buildList {
    add("균등" to List(n) { 1f / n })
    val startHeavy: List<Float>? = when (n) {
        2 -> listOf(0.65f, 0.35f)
        3 -> listOf(0.50f, 0.30f, 0.20f)
        4 -> listOf(0.40f, 0.30f, 0.20f, 0.10f)
        else -> null
    }
    if (startHeavy != null) {
        add("왼쪽 크게" to startHeavy)
        add("오른쪽 크게" to startHeavy.reversed())
    }
    when (n) {
        3 -> {
            add("양 끝 크게" to listOf(0.40f, 0.20f, 0.40f))
            add("가운데 크게" to listOf(0.20f, 0.60f, 0.20f))
        }
        4 -> {
            add("양 끝 크게" to listOf(0.35f, 0.15f, 0.15f, 0.35f))
            add("가운데 크게" to listOf(0.15f, 0.35f, 0.35f, 0.15f))
        }
    }
}

@Composable
private fun MiniRatioBar(
    ratios: List<Float>,
    modifier: Modifier = Modifier
) {
    val colors = listOf(
        Color(0xFF1E3A5F), Color(0xFF3A1E5F), Color(0xFF1E5F3A),
        Color(0xFF5F3A1E)
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        var x = 0f
        ratios.forEachIndexed { i, r ->
            val segW = r * w
            drawRect(
                color = colors[i % colors.size].copy(alpha = 0.85f),
                topLeft = Offset(x, 0f),
                size = Size((segW - 1f).coerceAtLeast(0f), h)
            )
            x += segW
        }
    }
}

private fun actionEquals(a: EdgeZoneAction, b: EdgeZoneAction): Boolean = when {
    a is EdgeZoneAction.Unassigned && b is EdgeZoneAction.Unassigned -> true
    a is EdgeZoneAction.ToggleMode && b is EdgeZoneAction.ToggleMode -> a.mode == b.mode
    a is EdgeZoneAction.CyclePreset && b is EdgeZoneAction.CyclePreset -> a.presetType == b.presetType
    a is EdgeZoneAction.OpenSettings && b is EdgeZoneAction.OpenSettings -> a.settingsType == b.settingsType
    a is EdgeZoneAction.SetDpi && b is EdgeZoneAction.SetDpi -> a.level == b.level
    a is EdgeZoneAction.SetScrollSpeed && b is EdgeZoneAction.SetScrollSpeed -> a.sensitivity == b.sensitivity
    a is EdgeZoneAction.SetModePreset && b is EdgeZoneAction.SetModePreset -> a.index == b.index
    a is EdgeZoneAction.SetDynamicsPreset && b is EdgeZoneAction.SetDynamicsPreset -> a.index == b.index
    a is EdgeZoneAction.SetClickMode && b is EdgeZoneAction.SetClickMode -> a.mode == b.mode
    a is EdgeZoneAction.SetMoveMode && b is EdgeZoneAction.SetMoveMode -> a.mode == b.mode
    a is EdgeZoneAction.SetScrollMode && b is EdgeZoneAction.SetScrollMode -> a.mode == b.mode
    else -> false
}

/**
 * 두 config를 비교해 `from → to` 사이에 무엇이 바뀌었는지 한 줄 설명 반환.
 * undo 히스토리 목록의 레이블로 사용됨.
 */
private fun describeUndoStep(from: EdgeZoneConfig, to: EdgeZoneConfig): String {
    if (from.cornerPriority != to.cornerPriority) return "코너 우선순위 변경"
    for (edge in EntryEdge.entries) {
        val f = from.zonesFor(edge)
        val t = to.zonesFor(edge)
        if (f == t) continue
        val edgeName = when (edge) {
            EntryEdge.TOP    -> "상단"
            EntryEdge.BOTTOM -> "하단"
            EntryEdge.LEFT   -> "좌측"
            EntryEdge.RIGHT  -> "우측"
        }
        if (t.size > f.size) return "$edgeName 존 분할"
        if (t.size < f.size) return "$edgeName 존 병합/삭제"
        for (i in f.indices) {
            val fz = f[i]; val tz = t[i]
            if (fz.startRatio != tz.startRatio || fz.endRatio != tz.endRatio) return "$edgeName 비율 조정"
            if (fz.label != tz.label) {
                val newLabel = tz.label.ifEmpty { "(없음)" }
                return "$edgeName 라벨 → \"$newLabel\""
            }
            if (fz.iconKey != tz.iconKey) return "$edgeName 아이콘 변경"
            if (fz.trigger != tz.trigger) {
                val ft = fz.trigger; val tt = tz.trigger
                return when {
                    ft is EdgeZoneTrigger.SingleAction && tt is EdgeZoneTrigger.SingleAction ->
                        "$edgeName 액션 → ${tt.action.displayName()}"
                    tt is EdgeZoneTrigger.Rotation -> "$edgeName 로테이션 설정"
                    ft is EdgeZoneTrigger.Rotation -> "$edgeName 단일 액션으로 변경"
                    else -> "$edgeName 트리거 변경"
                }
            }
        }
    }
    return "설정 변경"
}

// ============================================================
// 로테이션 존 편집기
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RotationEditor(
    rotation: EdgeZoneTrigger.Rotation,
    onRotationChanged: (EdgeZoneTrigger.Rotation) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val minCandidates = EdgeSwipeConstants.EDGE_ZONE_ROTATION_MIN_CANDIDATES
    val minMs = EdgeSwipeConstants.EDGE_ZONE_ROTATION_INTERVAL_MIN_MS.toFloat()
    val maxMs = EdgeSwipeConstants.EDGE_ZONE_ROTATION_INTERVAL_MAX_MS.toFloat()

    // 후보 편집 시트 상태: null=닫힘, Pair(index or null, candidate)
    var editingEntry by remember { mutableStateOf<Pair<Int?, RotationCandidate>?>(null) }
    val candidateSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showIconPickerInSheet by remember { mutableStateOf(false) }
    val iconSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // 간격 슬라이더: 50ms 단위 스냅
    val intervalSteps = ((maxMs - minMs) / 50f).toInt() - 1

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

        // 후보 최소 경고
        if (rotation.candidates.size < minCandidates) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(cs.error.copy(alpha = 0.08f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    "후보가 ${minCandidates}개 이상이어야 저장됩니다 (현재 ${rotation.candidates.size}개)",
                    fontSize = 11.sp,
                    color = cs.error
                )
            }
        }

        // 후보 목록
        rotation.candidates.forEachIndexed { idx, candidate ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(cs.surfaceVariant)
                    .clickable {
                        editingEntry = Pair(idx, candidate)
                        showIconPickerInSheet = false
                    }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (candidate.iconKey.isNotEmpty()) {
                    Icon(
                        imageVector = IconRegistry.get(candidate.iconKey),
                        contentDescription = null,
                        tint = cs.onSurface,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(candidate.action.displayName(), fontSize = 12.sp, color = cs.onSurface)
                    if (candidate.label.isNotEmpty()) {
                        Text(candidate.label, fontSize = 10.sp, color = cs.onSurfaceVariant)
                    }
                }
                // 순서 이동
                IconButton(
                    onClick = {
                        if (idx > 0) {
                            val list = rotation.candidates.toMutableList()
                            list.add(idx - 1, list.removeAt(idx))
                            onRotationChanged(rotation.copy(candidates = list))
                        }
                    },
                    modifier = Modifier.size(28.dp),
                    enabled = idx > 0
                ) {
                    Icon(Icons.Filled.KeyboardArrowUp, null, modifier = Modifier.size(16.dp),
                        tint = if (idx > 0) cs.onSurface else cs.onSurface.copy(alpha = 0.3f))
                }
                IconButton(
                    onClick = {
                        if (idx < rotation.candidates.size - 1) {
                            val list = rotation.candidates.toMutableList()
                            list.add(idx + 1, list.removeAt(idx))
                            onRotationChanged(rotation.copy(candidates = list))
                        }
                    },
                    modifier = Modifier.size(28.dp),
                    enabled = idx < rotation.candidates.size - 1
                ) {
                    Icon(Icons.Filled.KeyboardArrowDown, null, modifier = Modifier.size(16.dp),
                        tint = if (idx < rotation.candidates.size - 1) cs.onSurface else cs.onSurface.copy(alpha = 0.3f))
                }
                IconButton(
                    onClick = {
                        val list = rotation.candidates.toMutableList()
                        list.removeAt(idx)
                        onRotationChanged(rotation.copy(candidates = list))
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Filled.Delete, null, modifier = Modifier.size(14.dp), tint = cs.error.copy(alpha = 0.7f))
                }
            }
        }

        // 후보 추가 버튼
        Button(
            onClick = { editingEntry = Pair(null, RotationCandidate(EdgeZoneAction.Unassigned, "", "")) },
            colors = ButtonDefaults.buttonColors(
                containerColor = cs.primary.copy(alpha = 0.12f),
                contentColor = cs.primary
            ),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            Icon(Icons.Filled.Add, null, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text("후보 추가", fontSize = 12.sp)
        }

        // 간격 슬라이더
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("후보 1개당 머무는 시간", fontSize = 12.sp, color = cs.onSurfaceVariant)
                Text("${rotation.intervalMs}ms", fontSize = 12.sp, color = cs.onSurfaceVariant)
            }
            Slider(
                value = rotation.intervalMs.toFloat(),
                valueRange = minMs..maxMs,
                steps = intervalSteps,
                onValueChange = { onRotationChanged(rotation.copy(intervalMs = it.toInt())) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // 후보 편집 바텀시트
    editingEntry?.let { (editIdx, candidate) ->
        var draftCandidate by remember(editIdx) { mutableStateOf(candidate) }

        ModalBottomSheet(
            onDismissRequest = { editingEntry = null },
            sheetState = candidateSheetState,
            containerColor = cs.surfaceVariant
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    if (editIdx == null) "후보 추가" else "후보 편집",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = cs.onSurface
                )

                // 라벨
                OutlinedTextField(
                    value = draftCandidate.label,
                    onValueChange = { draftCandidate = draftCandidate.copy(label = it) },
                    label = { Text("라벨", color = cs.onSurfaceVariant) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = cs.onSurface,
                        unfocusedTextColor = cs.onSurface,
                        focusedBorderColor = cs.primary,
                        unfocusedBorderColor = cs.outline
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // 아이콘 선택
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(cs.background)
                        .clickable { showIconPickerInSheet = true }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("아이콘", fontSize = 13.sp, color = cs.onSurfaceVariant, modifier = Modifier.weight(1f))
                    if (draftCandidate.iconKey.isNotEmpty()) {
                        Icon(IconRegistry.get(draftCandidate.iconKey), null, tint = cs.onSurface, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("변경", fontSize = 12.sp, color = cs.primary)
                }

                // 액션 선택
                Text("액션", fontSize = 12.sp, color = cs.onSurfaceVariant)
                ActionCardGrid(
                    current = draftCandidate.action,
                    onSelect = { draftCandidate = draftCandidate.copy(action = it) }
                )

                // 확인 버튼
                FilledTonalButton(
                    onClick = {
                        val newList = rotation.candidates.toMutableList()
                        if (editIdx == null) newList.add(draftCandidate) else newList[editIdx] = draftCandidate
                        onRotationChanged(rotation.copy(candidates = newList))
                        editingEntry = null
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (editIdx == null) "추가" else "적용")
                }

                Spacer(Modifier.height(24.dp))
            }
        }

        // 아이콘 선택 (후보 편집 내)
        if (showIconPickerInSheet) {
            ModalBottomSheet(
                onDismissRequest = { showIconPickerInSheet = false },
                sheetState = iconSheetState,
                containerColor = cs.surfaceVariant
            ) {
                Text(
                    "아이콘 선택",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = cs.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    contentPadding = PaddingValues(start = 12.dp, top = 0.dp, end = 12.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(IconRegistry.allKeys) { key ->
                        val isSelected = draftCandidate.iconKey == key
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) cs.secondary.copy(alpha = 0.2f) else cs.background)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) cs.secondary else cs.outline.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    draftCandidate = draftCandidate.copy(iconKey = key)
                                    showIconPickerInSheet = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(IconRegistry.get(key), key, tint = if (isSelected) cs.secondary else cs.onSurface, modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }
        }
    }
}
