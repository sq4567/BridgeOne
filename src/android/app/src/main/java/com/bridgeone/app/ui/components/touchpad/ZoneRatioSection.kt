package com.bridgeone.app.ui.components.touchpad

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.compose.runtime.MutableState
import androidx.compose.ui.graphics.Color
import com.bridgeone.app.ui.common.EdgeSwipeConstants
import com.bridgeone.app.ui.common.InputMode
import com.bridgeone.app.ui.common.LocalInputMode
import com.bridgeone.app.ui.common.swipe.LocalSwipeFocused
import com.bridgeone.app.ui.common.swipe.SwipeFocusController
import com.bridgeone.app.ui.common.swipe.SwipeFocusable

/**
 * 선택 존의 영역 비율 편집 섹션 (Phase 4.7.5-D 추출).
 *
 * 비율 프리셋 메뉴(NORMAL DropdownMenu / SWIPE Popup), 스트립 에디터, 존 액션 팝업
 * ([ZoneActionPopup] Initial/Merge/Split/Delete)을 담는다. `inputMode`는 [LocalInputMode]로 읽으며,
 * config 변환·Undo는 [state]에 위임한다. `zonePopup`은 화면과 공유하는 [zonePopupState]에 위임해
 * 본문을 원본과 동일하게 유지한다. `stripBounds`는 본 섹션 전용이라 내부에서 소유한다.
 *
 * @param sel 현재 선택 존 스냅샷(없으면 null) — 렌더 분기에 사용. 실시간 변경은 [state]를 통한다.
 * @param edgeForStrip 편집 대상 엣지(non-null)
 * @param onRatioBtnBoundsChange 비율 프리셋 버튼 바운즈 보고(화면의 SWIPE 툴팁 앵커 갱신)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ZoneRatioSection(
    state: EdgeZoneEditorState,
    overlayUi: EdgeZoneOverlayUiState,
    zonePopupState: MutableState<ZoneActionPopup>,
    sel: EdgeZone?,
    edgeForStrip: EntryEdge,
    zoneList: List<EdgeZone>,
    zoneIdx: Int,
    minRatio: Float,
    maxZones: Int,
    stripBlockedStart: Float,
    stripBlockedStartLabel: String?,
    stripBlockedEnd: Float,
    stripBlockedEndLabel: String?,
    swipeController: SwipeFocusController,
    onRatioBtnBoundsChange: (Rect) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val inputMode = LocalInputMode.current
    var workConfig by state.workConfigState
    var selectedZone by state.selectedZoneState
    var currentPresetId by state.currentPresetIdState
    var zonePopup by zonePopupState
    var stripBounds by remember(edgeForStrip) { mutableStateOf(IntRect.Zero) }

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // ── 1. 영역 비율 ──
                            val p = zonePopup
                            var presetMenuOpen by overlayUi.showRatioPresetMenuState
                            var pendingPreviewIdx by remember { mutableStateOf<Int?>(null) }
                            val ratioPreviewRatios by overlayUi.ratioPreviewRatiosState

                            // 서랍 닫힐 때 NORMAL 미리보기 인덱스 리셋
                            LaunchedEffect(presetMenuOpen) {
                                if (!presetMenuOpen) pendingPreviewIdx = null
                            }

                            // 미리보기 존 파생 (workConfig/Undo 비오염, 렌더 전용)
                            val previewZones: List<EdgeZone>? = when {
                                inputMode == InputMode.NORMAL && pendingPreviewIdx != null -> {
                                    val presets = EdgeZoneActionResolver.ratioPresetsFor(zoneList.size)
                                    presets.getOrNull(pendingPreviewIdx!!)?.second
                                        ?.let { state.computeRatioZones(zoneList, it) }
                                }
                                inputMode == InputMode.SWIPE -> {
                                    ratioPreviewRatios?.let { state.computeRatioZones(zoneList, it) }
                                }
                                else -> null
                            }

                            if (inputMode == InputMode.SWIPE) {
                                DisposableEffect(presetMenuOpen) {
                                    val active = presetMenuOpen
                                    if (active) swipeController.pushScope(EdgeEditorScope.RatioPresetMenu)
                                    onDispose { if (active) swipeController.popScope() }
                                }
                                val presetMenuHasOpened = remember { mutableStateOf(false) }
                                LaunchedEffect(presetMenuOpen) {
                                    if (presetMenuOpen) {
                                        presetMenuHasOpened.value = true
                                        val presets = EdgeZoneActionResolver.ratioPresetsFor(zoneList.size)
                                        overlayUi.ratioPresetItemsState.value = presets
                                        overlayUi.ratioPresetOnSelectState.value = { ratios ->
                                            state.applyRatioPreset(edgeForStrip, ratios)
                                            presetMenuOpen = false
                                        }
                                        val firstLabel = presets.firstOrNull()?.first
                                        if (firstLabel != null) {
                                            swipeController.setFocus(EdgeEditorElement.RatioPresetItem(firstLabel))
                                        }
                                    } else if (presetMenuHasOpened.value) {
                                        // LaunchedEffect 실행 순서(코드 순)가 selectedZone effect(line ~253) 다음이므로
                                        // StripZone 포커스를 덮어쓰고 RatioPresetMenu로 최종 포커스됨
                                        swipeController.setFocus(EdgeEditorElement.RatioPresetMenu)
                                    }
                                }
                            }
                            val highlightedZones: Set<Pair<EntryEdge, Float>> = run {
                                if (sel != null && p is ZoneActionPopup.MergeSelecting) {
                                    buildSet {
                                        if (zoneIdx > 0) add(zoneList[zoneIdx - 1].edge to zoneList[zoneIdx - 1].startRatio)
                                        if (zoneIdx in 0 until zoneList.size - 1) add(zoneList[zoneIdx + 1].edge to zoneList[zoneIdx + 1].startRatio)
                                    }
                                } else emptySet()
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "영역 비율",
                                    fontSize = 12.sp,
                                    color = cs.onSurfaceVariant,
                                )
                                if (zoneList.size >= 2) {
                                    val presets = remember(zoneList.size) {
                                        EdgeZoneActionResolver.ratioPresetsFor(zoneList.size)
                                    }
                                    var normalDrawerBoxWidthPx by remember { mutableStateOf(0) }
                                    // NORMAL 서랍 enter/exit 애니메이션 상태 — exit가 끝날 때까지 Popup을 유지
                                    val normalMenuVisible = remember { MutableTransitionState(false) }
                                    normalMenuVisible.targetState = inputMode == InputMode.NORMAL && presetMenuOpen
                                    val normalMenuPresent = normalMenuVisible.currentState || normalMenuVisible.targetState
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 32.dp) // 아이콘 숨김 시 높이 유지
                                            .onGloballyPositioned {
                                                normalDrawerBoxWidthPx = it.size.width
                                                overlayUi.ratioDrawerMaxWidthPxState.value = it.size.width
                                            },
                                    ) {
                                        // 아이콘 버튼: 서랍 표시(애니메이션 포함) 중에는 숨김 (겹침 방지)
                                        val showIcon = if (inputMode == InputMode.NORMAL)
                                            !normalMenuPresent
                                        else
                                            // 닫기 애니(exit)가 끝날 때까지 아이콘 복귀 지연(NORMAL과 동일):
                                            // 열림(targetState=presetMenuOpen) 또는 exit 진행 중(currentState)이면 숨김
                                            !(presetMenuOpen || overlayUi.ratioPresetMenuVisibleState.currentState)
                                        if (showIcon) {
                                            Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                                                SwipeFocusable(
                                                    element = EdgeEditorElement.RatioPresetMenu,
                                                    shape = RoundedCornerShape(16.dp),
                                                    onActivate = { presetMenuOpen = true },
                                                    gridRow = 20,
                                                    modifier = Modifier.onGloballyPositioned { coords ->
                                                        onRatioBtnBoundsChange(coords.boundsInWindow())
                                                    },
                                                ) {
                                                    val ratioFocused = LocalSwipeFocused.current
                                                    IconButton(
                                                        onClick = { presetMenuOpen = true },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Filled.BarChart,
                                                            contentDescription = "비율 프리셋",
                                                            tint = if (ratioFocused) cs.primary else cs.onSurface.copy(alpha = 0.75f),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        // NORMAL 모드: 가로 서랍 Popup (외부 탭으로 닫힘). exit 애니메이션 동안 유지
                                        if (inputMode == InputMode.NORMAL && normalMenuPresent) {
                                            val density = LocalDensity.current
                                            Popup(
                                                popupPositionProvider = remember {
                                                    object : PopupPositionProvider {
                                                        override fun calculatePosition(
                                                            anchorBounds: IntRect,
                                                            windowSize: IntSize,
                                                            layoutDirection: LayoutDirection,
                                                            popupContentSize: IntSize
                                                        ): IntOffset {
                                                            // 화면 오른쪽 끝에 우측 정렬 (SWIPE 오버레이의 TopEnd와 동일, 폼 패딩 무시)
                                                            val x = windowSize.width - popupContentSize.width
                                                            // 헤더 행 세로 중앙 정렬 → 컴팩트해진 메뉴가 헤더 행 안에 들어가 스트립/라벨을 가리지 않음
                                                            val y = anchorBounds.top +
                                                                (anchorBounds.height - popupContentSize.height) / 2
                                                            return IntOffset(x.coerceAtLeast(0), y.coerceAtLeast(0))
                                                        }
                                                    }
                                                },
                                                onDismissRequest = {
                                                    presetMenuOpen = false
                                                    pendingPreviewIdx = null
                                                },
                                                properties = PopupProperties(
                                                    focusable = true,
                                                    dismissOnClickOutside = true,
                                                ),
                                            ) {
                                                val maxDrawerWidthDp = with(density) {
                                                    normalDrawerBoxWidthPx.toDp()
                                                }
                                                RatioPresetNormalDrawerContent(
                                                    visibleState = normalMenuVisible,
                                                    presets = presets,
                                                    maxWidthDp = maxDrawerWidthDp,
                                                    cs = cs,
                                                    pendingPreviewIdx = pendingPreviewIdx,
                                                    onItemTap = { idx, ratios ->
                                                        if (pendingPreviewIdx == idx) {
                                                            state.applyRatioPreset(edgeForStrip, ratios)
                                                            presetMenuOpen = false
                                                            pendingPreviewIdx = null
                                                        } else {
                                                            pendingPreviewIdx = idx
                                                        }
                                                    },
                                                )
                                            }
                                        }
                                        // SWIPE 모드: EdgeZoneOverlayLayer에서 인라인 렌더링
                                        // → SwipeGestureLayer가 터치를 수신하므로 화면 어디서든 스와이프 가능
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
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
                                    inputMode = inputMode,
                                    zones = zoneList,
                                    selectedZone = sel,
                                    previewZones = previewZones,
                                    minRatio = minRatio,
                                    onZonesChanged = { newZones ->
                                        state.pushUndo()
                                        workConfig = workConfig.withZones(edgeForStrip, newZones)
                                        currentPresetId = null
                                        val curSel = selectedZone
                                        selectedZone = if (curSel != null) {
                                            // 인덱스 기반 추적: 구분선 이동으로 startRatio가 바뀌어도
                                            // 동일 인덱스의 존으로 갱신 (다른 존으로 점프 방지)
                                            val curIdx = zoneList.indexOfFirst {
                                                it.startRatio == curSel.startRatio && it.edge == curSel.edge
                                            }
                                            if (curIdx >= 0) newZones.getOrNull(curIdx) else newZones.firstOrNull()
                                        } else null
                                    },
                                    onZoneSelected = { tapped ->
                                        val cur = zonePopup
                                        if (cur is ZoneActionPopup.MergeSelecting) {
                                            val bi = zoneList.indexOfFirst { it.startRatio == cur.zone.startRatio }
                                            val isAdj = tapped.startRatio == zoneList.getOrNull(bi - 1)?.startRatio ||
                                                        tapped.startRatio == zoneList.getOrNull(bi + 1)?.startRatio
                                            if (isAdj) {
                                                zonePopup = cur.copy(
                                                    selectedTargets = if (tapped.startRatio in cur.selectedTargets)
                                                        cur.selectedTargets - tapped.startRatio
                                                    else cur.selectedTargets + tapped.startRatio
                                                )
                                            }
                                        } else {
                                            selectedZone = tapped
                                            zonePopup = ZoneActionPopup.None
                                        }
                                    },
                                    onZoneDeselected = {
                                        if (zonePopup !is ZoneActionPopup.MergeSelecting) {
                                            selectedZone = null
                                            zonePopup = ZoneActionPopup.None
                                        }
                                    },
                                    onZoneLongPressed = { zone, cf ->
                                        val cur = zonePopup
                                        if (cur is ZoneActionPopup.MergeSelecting) {
                                            zonePopup = ZoneActionPopup.Initial(cur.zone, cur.anchor)
                                        } else {
                                            selectedZone = zone
                                            zonePopup = ZoneActionPopup.Initial(zone, cf)
                                        }
                                    },
                                    highlightedZones = highlightedZones,
                                    mergeBaseZone = (p as? ZoneActionPopup.MergeSelecting)?.zone,
                                    mergeTargetRatios = (p as? ZoneActionPopup.MergeSelecting)?.selectedTargets ?: emptySet(),
                                    onMergeTargetToggle = { tapped ->
                                        val cur = zonePopup
                                        if (cur is ZoneActionPopup.MergeSelecting) {
                                            zonePopup = cur.copy(
                                                selectedTargets = if (tapped.startRatio in cur.selectedTargets)
                                                    cur.selectedTargets - tapped.startRatio
                                                else cur.selectedTargets + tapped.startRatio
                                            )
                                        }
                                    },
                                    onMergeConfirm = {
                                        val cur = zonePopup
                                        if (cur is ZoneActionPopup.MergeSelecting) {
                                            if (cur.selectedTargets.isNotEmpty()) {
                                                if (state.tryMergeWithTargets(cur.zone, cur.selectedTargets)) zonePopup = ZoneActionPopup.None
                                            } else {
                                                zonePopup = ZoneActionPopup.Initial(cur.zone, cur.anchor)
                                            }
                                        }
                                    },
                                    blockedStartRatio = stripBlockedStart,
                                    blockedStartLabel = stripBlockedStartLabel,
                                    blockedEndRatio = stripBlockedEnd,
                                    blockedEndLabel = stripBlockedEndLabel,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // ── 팝업 ──
                            if (sel != null && p !is ZoneActionPopup.None && p !is ZoneActionPopup.MergeSelecting) {
                                val anchor = when (p) {
                                    is ZoneActionPopup.Initial -> p.anchor
                                    is ZoneActionPopup.MergeSelecting -> p.anchor
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
                                        focusable = inputMode == InputMode.NORMAL && p !is ZoneActionPopup.MergeSelecting,
                                        dismissOnClickOutside = inputMode == InputMode.NORMAL && p !is ZoneActionPopup.MergeSelecting,
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
                                                val mergeAction: () -> Unit = {
                                                    val leftZone = zoneList.getOrNull(zoneIdx - 1)
                                                    val rightZone = zoneList.getOrNull(zoneIdx + 1)
                                                    when {
                                                        leftZone != null && rightZone == null -> {
                                                            if (state.tryMergeWith(sel, leftZone)) zonePopup = ZoneActionPopup.None
                                                        }
                                                        leftZone == null && rightZone != null -> {
                                                            if (state.tryMergeWith(sel, rightZone)) zonePopup = ZoneActionPopup.None
                                                        }
                                                        else -> zonePopup = ZoneActionPopup.MergeSelecting(sel, p.anchor)
                                                    }
                                                }
                                                val splitAction: () -> Unit = { zonePopup = ZoneActionPopup.SplitChoosing(sel, p.anchor) }
                                                val deleteAction: () -> Unit = { zonePopup = ZoneActionPopup.DeleteConfirming(sel, p.anchor) }
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    SwipeFocusable(
                                                        element = EdgeEditorElement.ZoneActionMerge,
                                                        scope = EdgeEditorScope.ZoneActionPopup,
                                                        shape = RoundedCornerShape(4.dp),
                                                        showBorderHighlight = false,
                                                        onActivate = mergeAction,
                                                        gridRow = 0,
                                                    ) {
                                                        val focused = LocalSwipeFocused.current
                                                        Surface(
                                                            onClick = mergeAction,
                                                            enabled = hasAdj,
                                                            shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 4.dp, bottomEnd = 4.dp),
                                                            color = if (focused && hasAdj) cs.primary else Color.Transparent,
                                                            contentColor = when {
                                                                !hasAdj -> cs.onSurface.copy(alpha = 0.38f)
                                                                focused -> Color.White
                                                                else -> cs.primary
                                                            },
                                                        ) {
                                                            Text("병합", fontSize = 12.sp,
                                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp))
                                                        }
                                                    }
                                                    divider()
                                                    SwipeFocusable(
                                                        element = EdgeEditorElement.ZoneActionSplit,
                                                        scope = EdgeEditorScope.ZoneActionPopup,
                                                        shape = RoundedCornerShape(4.dp),
                                                        showBorderHighlight = false,
                                                        onActivate = splitAction,
                                                        gridRow = 0,
                                                    ) {
                                                        val focused = LocalSwipeFocused.current
                                                        Surface(
                                                            onClick = splitAction,
                                                            enabled = anySplitValid,
                                                            shape = RoundedCornerShape(4.dp),
                                                            color = if (focused && anySplitValid) cs.primary else Color.Transparent,
                                                            contentColor = when {
                                                                !anySplitValid -> cs.onSurface.copy(alpha = 0.38f)
                                                                focused -> Color.White
                                                                else -> cs.primary
                                                            },
                                                        ) {
                                                            Text("분할", fontSize = 12.sp,
                                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp))
                                                        }
                                                    }
                                                    divider()
                                                    SwipeFocusable(
                                                        element = EdgeEditorElement.ZoneActionDelete,
                                                        scope = EdgeEditorScope.ZoneActionPopup,
                                                        shape = RoundedCornerShape(4.dp),
                                                        showBorderHighlight = false,
                                                        onActivate = deleteAction,
                                                        gridRow = 0,
                                                    ) {
                                                        val focused = LocalSwipeFocused.current
                                                        Surface(
                                                            onClick = deleteAction,
                                                            enabled = canDel,
                                                            shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 8.dp, bottomEnd = 8.dp),
                                                            color = if (focused && canDel) cs.error else Color.Transparent,
                                                            contentColor = when {
                                                                !canDel -> cs.onSurface.copy(alpha = 0.38f)
                                                                focused -> Color.White
                                                                else -> cs.error
                                                            },
                                                        ) {
                                                            Text("삭제", fontSize = 12.sp,
                                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp))
                                                        }
                                                    }
                                                }
                                            }
                                            is ZoneActionPopup.SplitChoosing -> {
                                                val backToInitial: () -> Unit = { zonePopup = ZoneActionPopup.Initial(sel, p.anchor) }
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    (2..4).forEach { n ->
                                                        val valid = zoneList.size + n - 1 <= maxZones &&
                                                            (sel.endRatio - sel.startRatio) / n >= minRatio
                                                        val splitNAction: () -> Unit = { if (state.splitInto(sel, n)) zonePopup = ZoneActionPopup.None }
                                                        SwipeFocusable(
                                                            element = EdgeEditorElement.ZoneActionSplitN(n),
                                                            scope = EdgeEditorScope.ZoneActionPopup,
                                                            shape = RoundedCornerShape(4.dp),
                                                            showBorderHighlight = true,
                                                            onActivate = if (valid) splitNAction else fun() {},
                                                            onActivateAlt = backToInitial,
                                                            gridRow = 0,
                                                        ) {
                                                            Box(
                                                                contentAlignment = Alignment.Center,
                                                                modifier = Modifier.combinedClickable(
                                                                    enabled = true,
                                                                    onClick = { if (valid) splitNAction() },
                                                                    onLongClick = backToInitial,
                                                                )
                                                            ) {
                                                                Text(
                                                                    "$n",
                                                                    fontSize = 13.sp,
                                                                    color = if (valid) cs.primary else cs.onSurface.copy(alpha = 0.38f),
                                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            is ZoneActionPopup.DeleteConfirming -> {
                                                val deleteYesAction: () -> Unit = { state.deleteZone(sel); zonePopup = ZoneActionPopup.None }
                                                val deleteNoAction: () -> Unit = { zonePopup = ZoneActionPopup.None }
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Text("정말 삭제하시겠습니까?", fontSize = 12.sp, color = cs.onSurface)
                                                    SwipeFocusable(
                                                        element = EdgeEditorElement.ZoneActionDeleteYes,
                                                        scope = EdgeEditorScope.ZoneActionPopup,
                                                        shape = RoundedCornerShape(4.dp),
                                                        showBorderHighlight = true,
                                                        onActivate = deleteYesAction,
                                                        gridRow = 0,
                                                    ) {
                                                    TextButton(
                                                        onClick = deleteYesAction,
                                                        colors = ButtonDefaults.textButtonColors(contentColor = cs.error),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                                    ) { Text("예", fontSize = 12.sp) }
                                                    }
                                                    SwipeFocusable(
                                                        element = EdgeEditorElement.ZoneActionDeleteNo,
                                                        scope = EdgeEditorScope.ZoneActionPopup,
                                                        shape = RoundedCornerShape(4.dp),
                                                        showBorderHighlight = true,
                                                        onActivate = deleteNoAction,
                                                        gridRow = 0,
                                                    ) {
                                                    TextButton(
                                                        onClick = deleteNoAction,
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                                    ) { Text("아니오", fontSize = 12.sp) }
                                                    }
                                                }
                                            }
                                            else -> {}
                                        }
                                    }
                                }
                            }
                            } // Column (영역 비율)
}
