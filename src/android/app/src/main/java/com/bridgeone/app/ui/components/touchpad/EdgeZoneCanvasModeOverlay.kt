package com.bridgeone.app.ui.components.touchpad

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bridgeone.app.ui.common.EdgeSwipeConstants
import com.bridgeone.app.ui.common.LocalInputMode
import com.bridgeone.app.ui.common.ToastController
import com.bridgeone.app.ui.common.ToastType
import com.bridgeone.app.ui.common.ZoneMoveMethod

/**
 * 캔버스 씬 모드 진입/진행 오버레이 (Phase 4.7.x).
 *
 * - [CanvasEditMode.None]: 중앙에 모드 진입 버튼 5개(아이콘+라벨 카드 2행)를 표시.
 * - 그 외 모드: 모드별 진행 UI. 안내 + 취소만(모드별 조작은 캔버스 위에 직접 구현).
 *
 * 모드 None일 때만 진입 버튼이 보이며, 진입 후엔 사라진다. NORMAL은 카드 탭, SWIPE는 [SwipeFocusable] 포커스+탭으로 진입.
 *
 * 렌더 컴포넌트는 각 전담 파일로 추출 (Phase 4.7.8-C):
 * - 제스처: [canvasModeInput] ([EdgeZoneCanvasGestures.kt])
 * - 모드 진입 버튼: [ModeEntryButtons], [ModeCard] ([EdgeZoneCanvasModeButtons.kt])
 * - 모드 바: [ModeActiveBar], [SplitModeBar], [CancelButton], [ConfirmButton] ([EdgeZoneCanvasModeBars.kt])
 * - 비율 패널: [RatioPresetEdgeBar], [ResizeGuideCard], [ManipulationGuideBubble] ([EdgeZoneCanvasRatioPanel.kt])
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
    // 모드별 NORMAL 입력 (SWIPE는 SwipeGestureLayer가 가로채므로 무효)
    val inputModifier = Modifier.canvasModeInput(
        density = density,
        canvasMode = canvasMode,
        config = config,
        disabledEdges = disabledEdges,
        bottomLeftButtonLabel = bottomLeftButtonLabel,
        bottomRightButtonLabel = bottomRightButtonLabel,
        blockedRatio = blockedRatio,
        moveMethod = moveMethod,
        onZoneInteract = onZoneInteract,
        onResizeStart = onResizeStart,
        onResize = onResize,
        onMovingPick = onMovingPick,
        onMovingDrag = onMovingDrag,
        onMovingDragEnd = onMovingDragEnd,
        onMovingCancel = onMovingCancel,
        onMovingLongCancel = onMovingLongCancel,
        onMovingDropTap = onMovingDropTap,
        onEdgeBlocked = { band ->
            // 차단 사유(disabledEdges 값)는 항상 정확하다고 보장되지 않으므로 단정하지 않고 일반 문구만 표시.
            val edgeName = when (band) {
                EntryEdge.TOP    -> "상단"
                EntryEdge.BOTTOM -> "하단"
                EntryEdge.LEFT   -> "좌측"
                EntryEdge.RIGHT  -> "우측"
            }
            ToastController.show("$edgeName 가장자리는 비율을 조정할 수 없어요", ToastType.WARNING)
        },
    )

    val isNoneMode = canvasMode is CanvasEditMode.None
    val uiEnterDelay = EdgeSwipeConstants.EDGE_ZONE_MODE_UI_ENTER_DELAY_MS
    val staggerMs = EdgeSwipeConstants.EDGE_ZONE_MODE_BTN_STAGGER_MS

    // AnimatedVisibility exit 중에도 올바른 콘텐츠를 렌더링하기 위해 마지막 활성 모드를 보존.
    // canvasMode가 None으로 바뀌면 이 값이 exit 애니메이션 동안 "퇴장 중인" UI를 그린다.
    var lastActiveMode by remember { mutableStateOf<CanvasEditMode>(canvasMode) }
    if (canvasMode !is CanvasEditMode.None) lastActiveMode = canvasMode

    BoxWithConstraints(modifier = modifier.fillMaxSize().then(inputModifier), contentAlignment = Alignment.Center) {
        // 모드 진입 버튼 5개 — 순차 퇴장(exit), 기저 지연 후 순차 등장(enter)
        ModeEntryButtons(isNoneMode = isNoneMode, onModeChange = onModeChange)

        // 모드 활성 UI — 버튼 퇴장 완료 후 등장, 취소 시 빠르게 퇴장
        AnimatedVisibility(
            visible = !isNoneMode,
            enter = canvasModeEnter(uiEnterDelay),
            exit = canvasModeExit(),
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
                            enter = canvasModeEnter(staggerMs),
                            exit = canvasModeExit(),
                        ) {
                            ResizeGuideCard()
                        }
                    }
                } else {
                    val guide = guideText(m, moveMethod, LocalInputMode.current)
                    if (guide.isNotEmpty()) {
                        Surface(shape = RoundedCornerShape(8.dp), color = Color.Black.copy(alpha = EdgeSwipeConstants.EDGE_ZONE_MODE_GUIDE_BG_ALPHA), modifier = Modifier.padding(horizontal = 32.dp)) {
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
                                enter = canvasModeEnter(staggerMs),
                                exit = canvasModeExit(),
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

        // ── 비율 조정: 엣지 선택 시 프리셋 칩을 대상 엣지 옆에 배치 ──
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
                enter = canvasModeEnter(staggerMs),
                exit = canvasModeExit(),
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

// ── AnimatedVisibility 공유 트랜지션 헬퍼 ──

/**
 * 캔버스 모드 UI 등장 트랜지션 (scale+fade). [delayMs] 지연 후 시작.
 * [EdgeZoneCanvasModeButtons], [EdgeZoneCanvasModeBars]에서도 사용하므로 internal.
 */
internal fun canvasModeEnter(delayMs: Int = 0): EnterTransition {
    val enterMs = EdgeSwipeConstants.EDGE_ZONE_MODE_BTN_ENTER_MS
    val scale = EdgeSwipeConstants.EDGE_ZONE_MODE_SWITCH_SCALE
    return scaleIn(tween(enterMs, delayMillis = delayMs, easing = FastOutSlowInEasing), initialScale = scale) +
           fadeIn(tween(enterMs, delayMillis = delayMs, easing = FastOutSlowInEasing))
}

/**
 * 캔버스 모드 UI 퇴장 트랜지션 (scale+fade). [delayMs] 지연 후 시작.
 * [EdgeZoneCanvasModeButtons], [EdgeZoneCanvasModeBars]에서도 사용하므로 internal.
 */
internal fun canvasModeExit(delayMs: Int = 0): ExitTransition {
    val exitMs = EdgeSwipeConstants.EDGE_ZONE_MODE_BTN_EXIT_MS
    val scale = EdgeSwipeConstants.EDGE_ZONE_MODE_SWITCH_SCALE
    return scaleOut(tween(exitMs, delayMillis = delayMs, easing = FastOutSlowInEasing), targetScale = scale) +
           fadeOut(tween(exitMs, delayMillis = delayMs, easing = FastOutSlowInEasing))
}

// ── 공유 렌더 헬퍼 ──

/**
 * 안내 메시지를 불릿 목록으로 표시. 줄이 2개 이상이면 각 줄을 `•` 불릿으로 구분하고,
 * 불릿이 고정 첫 열 + 본문이 둘째 열이라 본문이 길어 줄바꿈돼도 들여쓰기가 유지된다(hanging indent).
 * 줄이 1개면 불릿 없이 중앙 정렬 단일 텍스트. 컨테이너는 IntrinsicSize.Max로 가장 긴 줄 폭에 맞춤.
 * [ManipulationGuideBubble]([EdgeZoneCanvasRatioPanel])에서도 사용하므로 internal.
 */
@Composable
internal fun GuideBulletText(lines: List<String>) {
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
