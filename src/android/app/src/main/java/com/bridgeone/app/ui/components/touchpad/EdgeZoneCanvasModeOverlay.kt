package com.bridgeone.app.ui.components.touchpad

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import com.bridgeone.app.ui.common.EdgeSwipeConstants
import com.bridgeone.app.ui.common.ToastController
import com.bridgeone.app.ui.common.ToastType
import com.bridgeone.app.ui.common.ZoneMoveMethod
import com.bridgeone.app.ui.common.InputMode
import com.bridgeone.app.ui.common.LocalInputMode
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMerge
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bridgeone.app.ui.common.swipe.LocalSwipeFocused
import com.bridgeone.app.ui.common.swipe.SwipeFocusable

/**
 * 캔버스 씬 모드 진입/진행 오버레이 (Phase 4.7.x).
 *
 * - [CanvasEditMode.None]: 중앙에 모드 진입 버튼 5개(아이콘+라벨 카드 2행)를 표시.
 * - 그 외 모드: 모드별 진행 UI. 단계 C에서는 안내 + 취소만(모드별 조작은 후속 단계에서 캔버스 위에 직접 구현).
 *
 * 모드 None일 때만 진입 버튼이 보이며, 진입 후엔 사라진다. NORMAL은 카드 탭, SWIPE는 [SwipeFocusable] 포커스+탭으로 진입.
 */
@Composable
internal fun EdgeZoneCanvasModeOverlay(
    canvasMode: CanvasEditMode,
    onModeChange: (CanvasEditMode) -> Unit,
    onConfirm: () -> Unit,
    config: EdgeZoneConfig,
    disabledEdges: Map<EntryEdge, String>,
    bottomLeftButtonLabel: String?,
    bottomRightButtonLabel: String?,
    onZoneInteract: (EdgeZone) -> Unit,
    // ── 이동 모드(NORMAL) ──
    moveMethod: ZoneMoveMethod = ZoneMoveMethod.TAP,
    onMovingPick: (EdgeZone) -> Unit = {},
    onMovingDropTap: (edge: EntryEdge, ratio: Float) -> Unit = { _, _ -> },
    onMovingDrag: (DropTarget) -> Unit = {},
    onMovingDragEnd: () -> Unit = {},
    onMovingCancel: () -> Unit = {},
    // NORMAL 이동 롱프레스: 세션 내 이동을 역순 애니메이션으로 모두 되돌린 뒤 모드 선택 복귀.
    onMovingLongCancel: () -> Unit = {},
    // 이동 롱프레스 되돌리기 진행 중이면 '확인' 버튼을 숨긴다(역순 복원 애니메이션과 겹침 방지).
    movingRevertInProgress: Boolean = false,
    onSplitInto: (Int) -> Unit,
    onResizeStart: () -> Unit,
    onResize: (edge: EntryEdge, leftIndex: Int, newRatio: Float) -> Unit,
    onApplyPreset: (edge: EntryEdge, ratios: List<Float>) -> Unit,
    // NORMAL 2단계 적용: 현재 미리보기로 선택된(armed) 프리셋의 비율. 해당 칩을 강조하고 "한 번 더 눌러 적용" 힌트를 띄운다.
    previewedRatios: List<Float>? = null,
    onResizeSessionDiscard: () -> Unit = {},
    onResizeModeConfirm: () -> Unit = {},
    onResizeModeCancel: () -> Unit = {},
    // SWIPE 경계 조작(MANIPULATION) 중이면 안내 카드를 숨겨 경계 이동 데모에 집중하게 한다.
    manipulating: Boolean = false,
    blockedRatio: Float = EdgeSwipeConstants.CORNER_BUTTON_BLOCKED_RATIO,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    // 모드별 NORMAL 입력 (SWIPE는 SwipeGestureLayer가 가로채므로 무효):
    // - 비율 조정: 경계 드래그
    // - 그 외 활성 모드: 가장자리 존 탭 선택
    val inputModifier = when {
        canvasMode is CanvasEditMode.Resizing -> Modifier
            .pointerInput(canvasMode) {
                var boundary: BoundaryHit? = null
                detectDragGestures(
                    onDragStart = { offset ->
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        val edgePx = with(density) { EdgeSwipeConstants.EDGE_HIT_WIDTH_DP.dp.toPx() }
                        val threshold = with(density) { EdgeSwipeConstants.ZONE_BOUNDARY_DRAG_HIT_DP.dp.toPx() }
                        boundary = findBoundaryAt(offset, config, w, h, edgePx, threshold, disabledEdges.keys, bottomLeftButtonLabel != null, bottomRightButtonLabel != null, blockedRatio)
                        if (boundary != null) onResizeStart()
                    },
                    onDrag = { change, _ ->
                        val b = boundary ?: return@detectDragGestures
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        val vertical = b.edge == EntryEdge.LEFT || b.edge == EntryEdge.RIGHT
                        val edgeLen = if (vertical) h else w
                        val along = if (vertical) change.position.y else change.position.x
                        onResize(b.edge, b.leftIndex, (along / edgeLen).coerceIn(0f, 1f))
                    },
                    onDragEnd = { boundary = null },
                    onDragCancel = { boundary = null },
                )
            }
            .pointerInput("resizeTap", canvasMode) {
                // 존 탭 → 비율 프리셋 대상 엣지 선택 (경계 드래그와 별도 제스처)
                detectTapGestures { offset ->
                    val w = size.width.toFloat()
                    val h = size.height.toFloat()
                    val edgePx = with(density) { EdgeSwipeConstants.EDGE_HIT_WIDTH_DP.dp.toPx() }
                    val zone = findZoneAt(
                        offset, config, w, h, edgePx,
                        disabledEdges.keys,
                        hasBottomLeft = bottomLeftButtonLabel != null,
                        hasBottomRight = bottomRightButtonLabel != null,
                    )
                    if (zone != null) {
                        onZoneInteract(zone)
                    } else {
                        // 비활성 엣지(예: 상단 제어 버튼) 탭 → 비율 조정 불가 안내.
                        // 차단 사유(disabledEdges 값)는 항상 정확하다고 보장되지 않으므로 단정하지 않고 일반 문구만 표시.
                        val band = edgeBandAt(offset, w, h, edgePx)
                        if (band != null && band in disabledEdges) {
                            val edgeName = when (band) {
                                EntryEdge.TOP    -> "상단"
                                EntryEdge.BOTTOM -> "하단"
                                EntryEdge.LEFT   -> "좌측"
                                EntryEdge.RIGHT  -> "우측"
                            }
                            ToastController.show("$edgeName 가장자리는 비율을 조정할 수 없어요", ToastType.WARNING)
                        }
                    }
                }
            }
        // 이동 모드 + 드래그 앤 드롭: 존을 잡아 끌면 실시간 미리보기, 릴리스 시 안착
        canvasMode is CanvasEditMode.Moving && moveMethod == ZoneMoveMethod.DRAG_AND_DROP -> Modifier
            // key는 config로 둔다. onMovingPick이 바꾸는 canvasMode를 key로 쓰면 들어올림 즉시
            // pointerInput이 재시작되어 진행 중인 detectDragGestures가 취소된다(자기참조 버그).
            // config는 드래그 도중엔 불변(commit 시점에만 갱신)이라 제스처가 유지된다.
            .pointerInput(config) {
                var picked = false
                detectDragGestures(
                    onDragStart = { offset ->
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        val edgePx = with(density) { EdgeSwipeConstants.EDGE_HIT_WIDTH_DP.dp.toPx() }
                        val zone = findZoneAt(
                            offset, config, w, h, edgePx, disabledEdges.keys,
                            hasBottomLeft = bottomLeftButtonLabel != null,
                            hasBottomRight = bottomRightButtonLabel != null,
                        )
                        if (zone != null) { onMovingPick(zone); picked = true }
                    },
                    onDrag = { change, _ ->
                        if (!picked) return@detectDragGestures
                        onMovingDrag(dropTargetAt(change.position, size.width.toFloat(), size.height.toFloat(), bottomLeftButtonLabel != null, bottomRightButtonLabel != null, blockedRatio, disabledEdges.keys))
                    },
                    onDragEnd = { onMovingDragEnd(); picked = false },
                    onDragCancel = { onMovingCancel(); picked = false },
                )
            }
        // 이동 모드 + 탭: picked 없으면 존 선택(들어올림), 있으면 경계/양 끝 탭으로 드롭. 롱프레스로 취소.
        canvasMode is CanvasEditMode.Moving -> Modifier.pointerInput(canvasMode) {
            detectTapGestures(
                onLongPress = { onMovingLongCancel() },
            ) { offset ->
                val w = size.width.toFloat()
                val h = size.height.toFloat()
                val edgePx = with(density) { EdgeSwipeConstants.EDGE_HIT_WIDTH_DP.dp.toPx() }
                val threshold = with(density) { EdgeSwipeConstants.ZONE_BOUNDARY_DRAG_HIT_DP.dp.toPx() }
                val moving = canvasMode as CanvasEditMode.Moving
                val picked = moving.picked
                if (picked == null) {
                    findZoneAt(
                        offset, config, w, h, edgePx, disabledEdges.keys,
                        hasBottomLeft = bottomLeftButtonLabel != null,
                        hasBottomRight = bottomRightButtonLabel != null,
                    )?.let { onMovingPick(it) }
                } else {
                    val b = findBoundaryAt(offset, config, w, h, edgePx, threshold, disabledEdges.keys, bottomLeftButtonLabel != null, bottomRightButtonLabel != null, blockedRatio)
                    if (b != null) {
                        val ratio = config.zonesFor(b.edge).getOrNull(b.leftIndex + 1)?.startRatio
                        if (ratio != null) onMovingDropTap(b.edge, ratio)
                    } else {
                        val ar = edgeAlongRatioAt(offset, w, h, edgePx, disabledEdges.keys, bottomLeftButtonLabel != null, bottomRightButtonLabel != null, blockedRatio)
                        val endRatio = EdgeSwipeConstants.EDGE_END_DROP_RATIO
                        if (ar != null && (ar.second < endRatio || ar.second > 1f - endRatio)) {
                            onMovingDropTap(ar.first, if (ar.second < 0.5f) 0f else 1f)
                        } else {
                            // 존 재탭(같은 존) = 취소
                            val z = findZoneAt(
                                offset, config, w, h, edgePx, disabledEdges.keys,
                                hasBottomLeft = bottomLeftButtonLabel != null,
                                hasBottomRight = bottomRightButtonLabel != null,
                            )
                            if (z != null && z.key() == picked) onMovingCancel()
                        }
                    }
                }
            }
        }
        canvasMode !is CanvasEditMode.None -> Modifier.pointerInput(canvasMode) {
            detectTapGestures { offset ->
                val w = size.width.toFloat()
                val h = size.height.toFloat()
                val edgePx = with(density) { EdgeSwipeConstants.EDGE_HIT_WIDTH_DP.dp.toPx() }
                val zone = findZoneAt(
                    offset, config, w, h, edgePx,
                    disabledEdges.keys,
                    hasBottomLeft = bottomLeftButtonLabel != null,
                    hasBottomRight = bottomRightButtonLabel != null,
                )
                if (zone != null) onZoneInteract(zone)
            }
        }
        else -> Modifier
    }
    val isNoneMode = canvasMode is CanvasEditMode.None
    val btnExitMs = EdgeSwipeConstants.EDGE_ZONE_MODE_BTN_EXIT_MS
    val btnEnterMs = EdgeSwipeConstants.EDGE_ZONE_MODE_BTN_ENTER_MS
    val staggerMs = EdgeSwipeConstants.EDGE_ZONE_MODE_BTN_STAGGER_MS
    val btnEnterBaseDelay = EdgeSwipeConstants.EDGE_ZONE_MODE_BTN_ENTER_BASE_DELAY_MS
    val uiEnterDelay = EdgeSwipeConstants.EDGE_ZONE_MODE_UI_ENTER_DELAY_MS
    val modeScale = EdgeSwipeConstants.EDGE_ZONE_MODE_SWITCH_SCALE

    // AnimatedVisibility exit 중에도 올바른 콘텐츠를 렌더링하기 위해 마지막 활성 모드를 보존.
    // canvasMode가 None으로 바뀌면 이 값이 exit 애니메이션 동안 "퇴장 중인" UI를 그린다.
    var lastActiveMode by remember { mutableStateOf<CanvasEditMode>(canvasMode) }
    if (canvasMode !is CanvasEditMode.None) lastActiveMode = canvasMode

    BoxWithConstraints(modifier = modifier.fillMaxSize().then(inputModifier), contentAlignment = Alignment.Center) {
        // 모드 진입 버튼 5개 — 순차 퇴장(exit), 기저 지연 후 순차 등장(enter)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(CanvasModeKind.MERGE to 0, CanvasModeKind.SPLIT to 1, CanvasModeKind.MOVE to 2)
                    .forEach { (kind, idx) ->
                        val exitDelay = idx * staggerMs
                        val enterDelay = btnEnterBaseDelay + idx * staggerMs
                        AnimatedVisibility(
                            visible = isNoneMode,
                            enter = scaleIn(tween(btnEnterMs, delayMillis = enterDelay, easing = FastOutSlowInEasing), initialScale = modeScale)
                                + fadeIn(tween(btnEnterMs, delayMillis = enterDelay, easing = FastOutSlowInEasing)),
                            exit = scaleOut(tween(btnExitMs, delayMillis = exitDelay, easing = FastOutSlowInEasing), targetScale = modeScale)
                                + fadeOut(tween(btnExitMs, delayMillis = exitDelay, easing = FastOutSlowInEasing)),
                        ) {
                            ModeCard(kind, onModeChange, sizeDp = 76.dp)
                        }
                    }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(CanvasModeKind.DELETE to 3, CanvasModeKind.RESIZE to 4)
                    .forEach { (kind, idx) ->
                        val exitDelay = idx * staggerMs
                        val enterDelay = btnEnterBaseDelay + idx * staggerMs
                        AnimatedVisibility(
                            visible = isNoneMode,
                            enter = scaleIn(tween(btnEnterMs, delayMillis = enterDelay, easing = FastOutSlowInEasing), initialScale = modeScale)
                                + fadeIn(tween(btnEnterMs, delayMillis = enterDelay, easing = FastOutSlowInEasing)),
                            exit = scaleOut(tween(btnExitMs, delayMillis = exitDelay, easing = FastOutSlowInEasing), targetScale = modeScale)
                                + fadeOut(tween(btnExitMs, delayMillis = exitDelay, easing = FastOutSlowInEasing)),
                        ) {
                            ModeCard(kind, onModeChange, sizeDp = 76.dp)
                        }
                    }
            }
        }

        // 모드 활성 UI — 버튼 퇴장 완료 후 등장, 취소 시 빠르게 퇴장
        AnimatedVisibility(
            visible = !isNoneMode,
            enter = scaleIn(tween(btnEnterMs, delayMillis = uiEnterDelay, easing = FastOutSlowInEasing), initialScale = modeScale)
                + fadeIn(tween(btnEnterMs, delayMillis = uiEnterDelay, easing = FastOutSlowInEasing)),
            exit = scaleOut(tween(btnExitMs, easing = FastOutSlowInEasing), targetScale = modeScale)
                + fadeOut(tween(btnExitMs, easing = FastOutSlowInEasing)),
        ) {
            val kind = lastActiveMode.kind ?: return@AnimatedVisibility
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val m = lastActiveMode
                if (m is CanvasEditMode.Resizing) {
                    // 비율 조정: 엣지 미선택 시 조작법 카드. 엣지 선택 후엔 칩이 엣지 옆에 표시되어 중앙 안내 불필요.
                    // 경계 조작(MANIPULATION) 중에는 안내 카드/버튼 대신 조작법 안내 메시지를 보여준다.
                    if (manipulating) {
                        ManipulationGuideBubble()
                    } else {
                        // 프리셋 바(엣지 선택)↔안내 카드(미선택) 전환을 부드럽게: 미선택 단계로 복귀 시 카드가 scale·fade in (프리셋 바 exit와 대칭)
                        AnimatedVisibility(
                            visible = m.edge == null,
                            enter = scaleIn(tween(btnEnterMs, delayMillis = staggerMs, easing = FastOutSlowInEasing), initialScale = modeScale)
                                + fadeIn(tween(btnEnterMs, delayMillis = staggerMs, easing = FastOutSlowInEasing)),
                            exit = scaleOut(tween(btnExitMs, easing = FastOutSlowInEasing), targetScale = modeScale)
                                + fadeOut(tween(btnExitMs, easing = FastOutSlowInEasing)),
                        ) {
                            ResizeGuideCard()
                        }
                    }
                } else {
                    val guide = guideText(m, moveMethod, LocalInputMode.current)
                    if (guide.isNotEmpty()) {
                        Surface(shape = RoundedCornerShape(8.dp), color = Color.Black.copy(alpha = 0.55f), modifier = Modifier.padding(horizontal = 32.dp)) {
                            Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                                GuideBulletText(guide.split("\n"))
                            }
                        }
                    }
                }
                when (m) {
                    is CanvasEditMode.Splitting -> {
                        val t = m.target
                        val targetZone = t?.let { tk ->
                            config.zonesFor(tk.edge).firstOrNull { it.startRatio == tk.startRatio }
                        }
                        val edgeZoneCount = t?.let { config.zonesFor(it.edge).size } ?: 0
                        SplitModeBar(
                            targetZone = targetZone,
                            edgeZoneCount = edgeZoneCount,
                            onSplit = onSplitInto,
                            onCancel = { onModeChange(CanvasEditMode.None) },
                        )
                    }
                    is CanvasEditMode.Resizing -> {
                        // 엣지 선택 후엔 프리셋 칩/취소가 엣지 옆 패널로 이동.
                        // 미선택(안내 카드) 단계에서는 중앙에 확인(변경 적용)·취소(원복) 버튼. 단, 경계 조작 중에는 숨긴다.
                        // 안내 카드와 함께 scale·fade in/out 시켜 프리셋 바 전환과 자연스럽게 동기화.
                        if (!manipulating) {
                            AnimatedVisibility(
                                visible = m.edge == null,
                                enter = scaleIn(tween(btnEnterMs, delayMillis = staggerMs, easing = FastOutSlowInEasing), initialScale = modeScale)
                                    + fadeIn(tween(btnEnterMs, delayMillis = staggerMs, easing = FastOutSlowInEasing)),
                                exit = scaleOut(tween(btnExitMs, easing = FastOutSlowInEasing), targetScale = modeScale)
                                    + fadeOut(tween(btnExitMs, easing = FastOutSlowInEasing)),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    ConfirmButton(CanvasModeKind.RESIZE.accentColor(), onResizeModeConfirm)
                                    CancelButton(CanvasModeKind.RESIZE.accentColor(), onResizeModeCancel)
                                }
                            }
                        }
                    }
                    // 이동 모드: 들어올린 존이 없을 때만 '확인'(현재 상태로 마치고 모드 선택 복귀) 노출.
                    // 되돌리기는 롱프레스가 담당하므로 취소 버튼은 두지 않는다.
                    is CanvasEditMode.Moving -> {
                        if (m.picked == null && !movingRevertInProgress) {
                            // 드래그 앤 드롭(NORMAL 전용)은 롱프레스 제스처가 없어 '취소' 버튼으로 역순 되돌리기를 제공.
                            // 탭/스와이프는 롱프레스가 되돌리기를 담당하므로 '확인'만 노출.
                            if (moveMethod == ZoneMoveMethod.DRAG_AND_DROP) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    ConfirmButton(CanvasModeKind.MOVE.accentColor(), onConfirm)
                                    CancelButton(CanvasModeKind.MOVE.accentColor(), onMovingLongCancel)
                                }
                            } else {
                                ConfirmButton(CanvasModeKind.MOVE.accentColor(), onConfirm)
                            }
                        }
                    }
                    else -> {
                        val selectedCount = when (m) {
                            is CanvasEditMode.Deleting -> m.selected.size
                            is CanvasEditMode.Merging -> m.selected.size + (if (m.base != null) 1 else 0)
                            else -> 0
                        }
                        // 병합/삭제는 다중 선택 후 중앙 확인 버튼으로 일괄 적용
                        val showConfirm = m is CanvasEditMode.Deleting || m is CanvasEditMode.Merging
                        ModeActiveBar(
                            kind = kind,
                            selectedCount = selectedCount,
                            showConfirm = showConfirm,
                            onConfirm = onConfirm,
                            onCancel = { onModeChange(CanvasEditMode.None) },
                        )
                    }
                }
            }
        }

        // ── 비율 조정: 엣지 선택 시 프리셋 칩을 대상 엣지 옆에 배치 (요청 3) ──
        // exit 애니메이션 동안 정렬/내용이 흔들리지 않도록 마지막 선택 엣지를 보존.
        var lastResizeEdge by remember { mutableStateOf<EntryEdge?>(null) }
        val curResizeEdge = (canvasMode as? CanvasEditMode.Resizing)?.edge
        if (curResizeEdge != null) lastResizeEdge = curResizeEdge
        val chipEdge = lastResizeEdge
        if (chipEdge != null) {
            val edgeInset = EdgeSwipeConstants.EDGE_HIT_WIDTH_DP.dp + 8.dp
            val chipAlign = when (chipEdge) {
                EntryEdge.TOP    -> Alignment.TopCenter
                EntryEdge.BOTTOM -> Alignment.BottomCenter
                EntryEdge.LEFT   -> Alignment.CenterStart
                EntryEdge.RIGHT  -> Alignment.CenterEnd
            }
            val chipPad = when (chipEdge) {
                EntryEdge.TOP    -> Modifier.padding(top = edgeInset)
                EntryEdge.BOTTOM -> Modifier.padding(bottom = edgeInset)
                EntryEdge.LEFT   -> Modifier.padding(start = edgeInset)
                EntryEdge.RIGHT  -> Modifier.padding(end = edgeInset)
            }
            // 가로 엣지(상/하)는 칩이 한 줄로 늘어나 화면 폭을 넘을 수 있으므로 패널 최대 폭을 제한해 FlowRow 줄바꿈 유도.
            val horizontal = chipEdge == EntryEdge.TOP || chipEdge == EntryEdge.BOTTOM
            val barMaxWidth = if (horizontal) maxWidth - 32.dp else null
            AnimatedVisibility(
                visible = curResizeEdge != null,
                modifier = Modifier.align(chipAlign).then(chipPad),
                enter = scaleIn(tween(btnEnterMs, delayMillis = staggerMs, easing = FastOutSlowInEasing), initialScale = modeScale)
                    + fadeIn(tween(btnEnterMs, delayMillis = staggerMs, easing = FastOutSlowInEasing)),
                exit = scaleOut(tween(btnExitMs, easing = FastOutSlowInEasing), targetScale = modeScale)
                    + fadeOut(tween(btnExitMs, easing = FastOutSlowInEasing)),
            ) {
                RatioPresetEdgeBar(
                    edge = chipEdge,
                    presets = EdgeZoneActionResolver.ratioPresetsFor(config.zonesFor(chipEdge).size),
                    previewedRatios = previewedRatios,
                    onApply = { ratios -> onApplyPreset(chipEdge, ratios) },
                    // 패널 취소: 이 엣지 세션의 프리셋 적용을 되돌리고, 모드는 유지한 채 엣지 선택만 해제 → 안내 카드 단계로 복귀
                    onCancel = {
                        onResizeSessionDiscard()
                        onModeChange(CanvasEditMode.Resizing())
                    },
                    maxWidth = barMaxWidth,
                )
            }
        }
    }
}

private fun CanvasModeKind.icon(): ImageVector = when (this) {
    CanvasModeKind.MERGE  -> Icons.AutoMirrored.Filled.CallMerge
    CanvasModeKind.SPLIT  -> Icons.AutoMirrored.Filled.CallSplit
    CanvasModeKind.MOVE   -> Icons.Filled.OpenWith
    CanvasModeKind.DELETE -> Icons.Filled.DeleteOutline
    CanvasModeKind.RESIZE -> Icons.Filled.BarChart
}

/** 모드 활성 시 캔버스 중앙에 띄우는 안내 메시지. */
private fun guideText(
    mode: CanvasEditMode,
    moveMethod: ZoneMoveMethod = ZoneMoveMethod.TAP,
    inputMode: InputMode = InputMode.NORMAL,
): String = when (mode) {
    is CanvasEditMode.Merging  ->
        if (mode.base == null) "병합 대상으로 삼고 싶은 존을 선택하세요." else "병합할 인접한 존을 선택하세요."
    is CanvasEditMode.Deleting  -> "삭제할 존을 선택하세요."
    is CanvasEditMode.Splitting ->
        if (mode.target == null) "분할할 존을 선택하세요." else "분할 갯수를 선택하세요."
    is CanvasEditMode.Moving    -> when {
        moveMethod == ZoneMoveMethod.DRAG_AND_DROP && mode.picked != null -> ""  // 드래그 중엔 숨김
        moveMethod == ZoneMoveMethod.DRAG_AND_DROP -> "옮길 존을 끌어다 놓으세요."
        // SWIPE: 스와이프로 위치를 정하고 탭으로 확정 / NORMAL 탭: 경계·끝을 직접 탭
        inputMode == InputMode.SWIPE && mode.picked != null -> "스와이프로 위치를 정하고 탭으로 확정\n롱프레스로 되돌리고 나가기"
        inputMode == InputMode.SWIPE -> "스와이프로 옮길 존을 정하고 탭\n롱프레스로 되돌리고 나가기"
        mode.picked != null -> "놓을 위치(존 사이 경계 또는 끝)를 탭하세요.\n롱프레스로 되돌리고 나가기"
        else -> "이동할 존을 선택하세요.\n롱프레스로 되돌리고 나가기"
    }
    is CanvasEditMode.Resizing  ->
        if (mode.edge == null) "경계를 드래그하거나, 존을 탭해 프리셋을 선택하세요."
        else "비율 프리셋을 선택하거나 경계를 드래그하세요."
    CanvasEditMode.None         -> ""
}

/** 모드별 고유 강조 색 (병합=그린, 분할=블루, 이동=오렌지, 삭제=레드, 비율=퍼플). */
internal fun CanvasModeKind.accentColor(): Color = when (this) {
    CanvasModeKind.MERGE  -> Color(0xFF1A7A3A)
    CanvasModeKind.SPLIT  -> Color(0xFF1565A8)
    CanvasModeKind.MOVE   -> Color(0xFFB84A00)
    CanvasModeKind.DELETE -> Color(0xFF9E2A2A)
    CanvasModeKind.RESIZE -> Color(0xFF5E3A9E)
}

@Composable
private fun ModeCard(
    kind: CanvasModeKind,
    onModeChange: (CanvasEditMode) -> Unit,
    sizeDp: Dp = 92.dp,
) {
    val enter: () -> Unit = { onModeChange(kind.toMode()) }
    val modeColor = kind.accentColor()
    val compact = sizeDp < 88.dp
    // gridRow 미지정(null) → 캔버스 씬이 좌표 기반 traversal로 동작하여
    // 각 엣지 존에서 중앙 방향 스와이프 시 모드 버튼으로 포커스가 이동한다.
    SwipeFocusable(
        element = EdgeEditorElement.CanvasModeButton(kind),
        shape = RoundedCornerShape(14.dp),
        showBorderHighlight = false,
        onActivate = enter,
    ) {
        val focused = LocalSwipeFocused.current
        Surface(
            onClick = enter,
            shape = RoundedCornerShape(14.dp),
            color = if (focused) modeColor else modeColor.copy(alpha = 0.82f),
            contentColor = Color.White,
            tonalElevation = 3.dp,
            shadowElevation = if (focused) 6.dp else 2.dp,
            border = if (focused) BorderStroke(2.dp, Color.White) else null,
            modifier = Modifier.size(sizeDp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp, Alignment.CenterVertically),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (compact) 6.dp else 8.dp),
            ) {
                Icon(kind.icon(), contentDescription = kind.label, modifier = Modifier.size(if (compact) 24.dp else 28.dp))
                Text(kind.label, fontSize = if (compact) 12.sp else 13.sp)
            }
        }
    }
}

@Composable
private fun ModeActiveBar(
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
private fun CancelButton(modeColor: Color, onCancel: () -> Unit) {
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
private fun ConfirmButton(modeColor: Color, onConfirm: () -> Unit) {
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

/** 분할 모드 바: 대상 존 선택 후 분할 갯수(2~4) 버튼을 표시. */
@Composable
private fun SplitModeBar(
    targetZone: EdgeZone?,
    edgeZoneCount: Int,
    onSplit: (Int) -> Unit,
    onCancel: () -> Unit,
) {
    val modeColor = CanvasModeKind.SPLIT.accentColor()
    val btnEnterMs = EdgeSwipeConstants.EDGE_ZONE_MODE_BTN_ENTER_MS
    val btnExitMs = EdgeSwipeConstants.EDGE_ZONE_MODE_BTN_EXIT_MS
    val modeScale = EdgeSwipeConstants.EDGE_ZONE_MODE_SWITCH_SCALE
    val staggerDelay = EdgeSwipeConstants.EDGE_ZONE_MODE_BTN_STAGGER_MS
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // 대상 존 선택 후 갯수 버튼 Row가 fade+scale로 등장/퇴장
        AnimatedVisibility(
            visible = targetZone != null,
            enter = scaleIn(tween(btnEnterMs, delayMillis = staggerDelay, easing = FastOutSlowInEasing), initialScale = modeScale)
                + fadeIn(tween(btnEnterMs, delayMillis = staggerDelay, easing = FastOutSlowInEasing)),
            exit = scaleOut(tween(btnExitMs, easing = FastOutSlowInEasing), targetScale = modeScale)
                + fadeOut(tween(btnExitMs, easing = FastOutSlowInEasing)),
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

/**
 * 비율 조정 모드의 조작법 안내 카드 (요청 1). 엣지 미선택 시 캔버스 중앙에 표시.
 * 두 조작법(경계 드래그 / 비율 프리셋)을 [아이콘] [용어] → [설명] 행으로 나열.
 */
@Composable
private fun ResizeGuideCard() {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color.Black.copy(alpha = 0.55f),
        // 양끝 엣지 스트립에 걸치지 않도록 좌우 여백 확보 (카드 가용 폭 축소)
        modifier = Modifier.padding(horizontal = 40.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
            ResizeGuideRow(Icons.Filled.SwapHoriz, "경계 드래그", "존 경계를 움직여 비율 조정")
            HorizontalDivider(
                color = Color.White.copy(alpha = 0.15f),
                modifier = Modifier.padding(vertical = 6.dp),
            )
            ResizeGuideRow(Icons.Filled.Tune, "비율 프리셋", "엣지별 추천 비율 적용")
        }
    }
}

/** 경계 조작(MANIPULATION) 중 표시하는 조작법 안내 메시지 버블. */
@Composable
private fun ManipulationGuideBubble() {
    Surface(shape = RoundedCornerShape(8.dp), color = Color.Black.copy(alpha = 0.55f), modifier = Modifier.padding(horizontal = 32.dp)) {
        Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
            GuideBulletText(listOf("스와이프로 경계 이동", "탭으로 확정", "롱프레스로 되돌리기"))
        }
    }
}

/**
 * 안내 메시지를 불릿 목록으로 표시. 줄이 2개 이상이면 각 줄을 `•` 불릿으로 구분하고,
 * 불릿이 고정 첫 열 + 본문이 둘째 열이라 본문이 길어 줄바꿈돼도 들여쓰기가 유지된다(hanging indent).
 * 줄이 1개면 불릿 없이 중앙 정렬 단일 텍스트. 컨테이너는 IntrinsicSize.Max로 가장 긴 줄 폭에 맞춤.
 */
@Composable
private fun GuideBulletText(lines: List<String>) {
    if (lines.size <= 1) {
        Text(
            text = lines.firstOrNull().orEmpty(),
            color = Color.White,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            style = TextStyle(lineBreak = LineBreak.Heading),
        )
        return
    }
    Column(
        modifier = Modifier.width(IntrinsicSize.Max),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        lines.forEach { line ->
            Row(verticalAlignment = Alignment.Top) {
                Text("•", color = Color.White, fontSize = 13.sp)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = line,
                    color = Color.White,
                    fontSize = 13.sp,
                    style = TextStyle(lineBreak = LineBreak.Heading),
                )
            }
        }
    }
}

/** 조작법 안내 카드의 한 행: 아이콘 · 굵은 용어 · → · 설명. */
@Composable
private fun ResizeGuideRow(icon: ImageVector, term: String, desc: String) {
    val accent = Color(0xFFB39DDB)  // 비율 모드(퍼플) 톤의 밝은 강조색
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(8.dp))
        Text(term, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(6.dp))
        Text("→", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
        Spacer(Modifier.width(6.dp))
        Text(desc, color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
    }
}

/**
 * 비율 프리셋 패널 (요청 3 + 배치 개선). 칩을 반투명 카드로 묶고 하단 구분선 아래에 취소 버튼을 통합한다.
 * 칩은 좌측 정렬. 세로 엣지(좌/우)는 칩을 세로 나열, 가로 엣지(상/하)는 [FlowRow]로 나열(오버플로 줄바꿈).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RatioPresetEdgeBar(
    edge: EntryEdge,
    presets: List<Pair<String, List<Float>>>,
    onApply: (List<Float>) -> Unit,
    onCancel: () -> Unit,
    previewedRatios: List<Float>? = null,
    maxWidth: Dp? = null,
) {
    val vertical = edge == EntryEdge.LEFT || edge == EntryEdge.RIGHT
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.Black.copy(alpha = 0.45f),
        modifier = if (maxWidth != null) Modifier.widthIn(max = maxWidth) else Modifier,
    ) {
        // 세로 엣지: 구분선(fillMaxWidth)이 패널을 화면 폭까지 늘리지 않도록 내용물 최대 폭에 맞춤.
        // → 칩 폭만큼만 차지하고 그 안에서 칩이 좌측 정렬됨.
        val columnWidthMod = if (vertical) Modifier.width(IntrinsicSize.Max) else Modifier
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(3.dp),
            modifier = columnWidthMod.padding(8.dp),
        ) {
            if (vertical) {
                // 각 칩을 패널 폭(IntrinsicSize.Max)에 맞춰 엣지 쪽으로 정렬 — 좌측 엣지는 좌측, 우측 엣지는 우측.
                // (좁은 '균등'도 같은 라인에 정렬)
                val chipAlign = if (edge == EntryEdge.RIGHT) Alignment.CenterEnd else Alignment.CenterStart
                presets.forEach { (label, ratios) ->
                    Box(Modifier.fillMaxWidth(), contentAlignment = chipAlign) {
                        RatioPresetChip(label, ratios, onApply, armed = ratios == previewedRatios)
                    }
                }
            } else {
                // 가로 엣지(상/하): 칩을 가운데 정렬하고, 4개 이상이면 두 행에 균등 분배 (예: 5개 → 3+2)
                val maxPerRow = if (presets.size >= 4) (presets.size + 1) / 2 else presets.size
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    maxItemsInEachRow = maxPerRow,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    presets.forEach { (label, ratios) -> RatioPresetChip(label, ratios, onApply, armed = ratios == previewedRatios) }
                }
            }
            // NORMAL 미리보기 armed 상태: 한 번 더 눌러야 적용된다는 힌트
            if (previewedRatios != null) {
                Text(
                    "한 번 더 눌러 적용",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    modifier = Modifier.padding(start = 2.dp, top = 1.dp),
                )
            }
            HorizontalDivider(
                color = Color.White.copy(alpha = 0.15f),
                modifier = Modifier.padding(vertical = 3.dp),
            )
            // 취소 버튼은 패널 폭 중앙 배치
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                PanelCancelButton(onCancel)
            }
        }
    }
}

/** 프리셋 패널 하단 취소 칩. 프리셋(퍼플)과 구분되도록 중립색을 사용하고 ✕ 아이콘을 곁들인다. */
@Composable
private fun PanelCancelButton(onCancel: () -> Unit) {
    val modeColor = CanvasModeKind.RESIZE.accentColor()
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
            color = if (focused) Color.White else Color.White.copy(alpha = 0.12f),
            contentColor = if (focused) modeColor else Color.White.copy(alpha = 0.85f),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                Text("취소", fontSize = 11.sp)
            }
        }
    }
}

/**
 * 비율 프리셋 칩 1개. NORMAL 탭(onClick) / SWIPE 포커스(SwipeFocusable) 모두 지원.
 * @param armed NORMAL 2단계 적용에서 현재 미리보기로 선택된 칩. 흰 테두리로 강조해 재탭 시 적용됨을 알린다.
 */
@Composable
private fun RatioPresetChip(
    label: String,
    ratios: List<Float>,
    onApply: (List<Float>) -> Unit,
    armed: Boolean = false,
) {
    val modeColor = CanvasModeKind.RESIZE.accentColor()
    val apply: () -> Unit = { onApply(ratios) }
    SwipeFocusable(
        element = EdgeEditorElement.CanvasRatioPreset(label),
        shape = RoundedCornerShape(8.dp),
        showBorderHighlight = false,
        onActivate = apply,
    ) {
        val focused = LocalSwipeFocused.current
        // Surface(onClick)은 최소 터치 타겟(48dp)을 강제해 짧은 라벨('균등')이 중앙으로 밀린다.
        // Modifier.clickable로 처리해 강제를 피하고 칩이 시각 크기 그대로 좌측 정렬되게 한다.
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (focused) Color.White else modeColor,
            contentColor = if (focused) modeColor else Color.White,
            border = if (armed && !focused) BorderStroke(2.dp, Color.White) else null,
            modifier = Modifier.clickable(onClick = apply),
        ) {
            Text(label, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
        }
    }
}
