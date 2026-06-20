package com.bridgeone.app.ui.components.touchpad

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.bridgeone.app.ui.common.CustomPointerDynamicsPreset
import com.bridgeone.app.ui.common.EdgeZonePresetsRepository
import com.bridgeone.app.ui.common.InputMode
import com.bridgeone.app.ui.common.LocalInputMode
import com.bridgeone.app.ui.common.ToastController
import com.bridgeone.app.ui.common.ToastType
import com.bridgeone.app.ui.common.stripActions
import com.bridgeone.app.ui.common.swipe.SwipeFocusController
import com.bridgeone.app.ui.common.swipe.SwipeGestureLayer
import com.bridgeone.app.ui.common.swipe.SwipeMode

/**
 * EdgeZoneEditorScreen의 오버레이/팝업 레이어 (Phase 4.7.5-D 추출).
 *
 * 메인 Box 안, 편집 패널 위에 떠 있는 오버레이 묶음(SWIPE 힌트/툴팁, 아이콘 서랍, 컬러 피커,
 * Undo 드롭다운, 프리셋 팝업, 단축키/매크로 편집기, NORMAL 미니 툴팁, SWIPE 제스처 레이어)을
 * 한 곳에 모은다. 프리셋 팝업은 모드 무관(내부에서 `inputMode` 분기)이고 NORMAL 매크로 편집기가
 * SWIPE 체인 중간에 끼어 있어, 호출부가 모드로 가드하지 않고 **무조건 호출**하며 내부에 기존
 * `inputMode` 분기를 그대로 유지한다(byte-identical 보존). `inputMode`는 [LocalInputMode]로 읽는다.
 *
 * UI 상태는 [overlayUi] 홀더가 소유하며, 본 레이어는 화면과 동일한 `MutableState`에 위임해
 * 본문을 원본과 동일하게 유지한다. 다이나믹스 편집기·미저장 다이얼로그는 본 레이어에 포함하지 않는다.
 *
 * @param localCustomPresets `remember(customPresets)` 키를 가져 홀더가 아닌 화면이 소유하므로 값으로 전달
 */
@Composable
internal fun BoxScope.EdgeZoneOverlayLayer(
    overlayUi: EdgeZoneOverlayUiState,
    state: EdgeZoneEditorState,
    swipeController: SwipeFocusController,
    presetsRepo: EdgeZonePresetsRepository?,
    localCustomPresets: List<CustomPointerDynamicsPreset>,
    updateSelectedZone: (EdgeZone) -> Unit,
    zonePopupState: MutableState<ZoneActionPopup>,
) {
    val inputMode = LocalInputMode.current
    val cs = MaterialTheme.colorScheme

    // 화면 config 상태 위임 (EdgeZoneEditorState)
    var workConfig by state.workConfigState
    var selectedZone by state.selectedZoneState
    var currentPresetId by state.currentPresetIdState
    var undoStack by state.undoStackState

    // 오버레이 UI 상태 위임 (EdgeZoneOverlayUiState) — 본문은 원본 지역 변수명을 그대로 사용
    var showIconSheet by overlayUi.showIconSheetState
    var showColorPicker by overlayUi.showColorPickerState
    var showCandidateIconSheet by overlayUi.showCandidateIconSheetState
    var showCandidateColorPicker by overlayUi.showCandidateColorPickerState
    var showLabelKeyboard by overlayUi.showLabelKeyboardState
    var candidateLabelKeyboard by overlayUi.candidateLabelKeyboardState
    var iconDrawerStage by overlayUi.iconDrawerStageState
    var colorPickerStage by overlayUi.colorPickerStageState
    var colorCommitCandidate by overlayUi.colorCommitCandidateState
    var iconBoxCenterInWindow by overlayUi.iconBoxCenterInWindowState
    var colorBoxCenterInWindow by overlayUi.colorBoxCenterInWindowState
    var candidateIconBoxCenterInWindow by overlayUi.candidateIconBoxCenterInWindowState
    var candidateColorBoxCenterInWindow by overlayUi.candidateColorBoxCenterInWindowState
    var ratioBtnBoundsInWindow by overlayUi.ratioBtnBoundsInWindowState
    var actionTypeBtnBoundsInWindow by overlayUi.actionTypeBtnBoundsInWindowState
    var revertBtnBoundsInWindow by overlayUi.revertBtnBoundsInWindowState
    var showPresetPopup by overlayUi.showPresetPopupState
    var presetPopupStage by overlayUi.presetPopupStageState
    var presetNameKeyboard by overlayUi.presetNameKeyboardState
    var presetNameSeed by overlayUi.presetNameSeedState
    var showUndoMenu by overlayUi.showUndoMenuState
    var undoMenuAnchorBottom by overlayUi.undoMenuAnchorBottomState
    var swipeShortcutVisible by overlayUi.swipeShortcutVisibleState
    var swipeShortcutDraft by overlayUi.swipeShortcutDraftState
    var swipeShortcutOnConfirm by overlayUi.swipeShortcutOnConfirmState
    var swipeShortcutOnAddAsCandidate by overlayUi.swipeShortcutOnAddAsCandidateState
    var shortcutNameKbActive by overlayUi.shortcutNameKbActiveState
    var macroEditorVisible by overlayUi.macroEditorVisibleState
    var macroEditorDraft by overlayUi.macroEditorDraftState
    var macroEditorOnConfirm by overlayUi.macroEditorOnConfirmState
    var macroEditorOnAddAsPreset by overlayUi.macroEditorOnAddAsPresetState
    var macroEditorInitialIconKey by overlayUi.macroEditorInitialIconKeyState
    var macroEditorInitialName by overlayUi.macroEditorInitialNameState
    var macroNameKbActive by overlayUi.macroNameKbActiveState
    var normalMiniTooltipText by overlayUi.normalMiniTooltipTextState
    var normalMiniTooltipAnchor by overlayUi.normalMiniTooltipAnchorState
    var shortcutIconSheetVisible by overlayUi.shortcutIconSheetVisibleState
    var shortcutIconSeed by overlayUi.shortcutIconSeedState
    var shortcutIconOnPick by overlayUi.shortcutIconOnPickState
    var shortcutIconAnchor by overlayUi.shortcutIconAnchorState
    var iconPickerReturnElement by overlayUi.iconPickerReturnElementState
    var rotationDraft by overlayUi.rotationDraftState
    var nextFocusOnZoneChange by overlayUi.nextFocusOnZoneChangeState
    var swipeCustomMenuTarget by overlayUi.swipeCustomMenuTargetState
    var localCustomShortcutPresets by overlayUi.localCustomShortcutPresetsState
    var localCustomMacroPresets by overlayUi.localCustomMacroPresetsState
    var zonePopup by zonePopupState

        // ── SWIPE 모드: StripBoundary 조작 안내 힌트 ──
        if (inputMode == InputMode.SWIPE) {
            val isBoundaryManipulating = swipeController.currentFocus is EdgeEditorElement.StripBoundary
                && swipeController.mode == SwipeMode.MANIPULATION
            AnimatedVisibility(
                visible = isBoundaryManipulating,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp)
            ) {
                androidx.compose.material3.Surface(
                    color = cs.inverseSurface.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        text = "← → 스와이프로 비율 조정  •  탭으로 확정",
                        color = cs.inverseOnSurface,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                    )
                }
            }
        }

        // ── SWIPE 모드: 아이콘 전용 버튼 이름 툴팁 ──
        if (inputMode == InputMode.SWIPE) {
            val rawIconButtonTooltip = when (val cf = swipeController.currentFocus) {
                EdgeEditorElement.RatioPresetMenu -> "영역 비율 설정"
                EdgeEditorElement.ActionTypeToggle ->
                    if (selectedZone?.trigger is EdgeZoneTrigger.SingleAction) "액션 순환으로 전환"
                    else "단일 액션으로 전환"
                EdgeEditorElement.RevertToAuto -> "표시 설정 초기화"
                else -> if (cf is EdgeEditorElement) macroButtonLabel(cf) else null
            }
            val tooltipAnchorBounds = when (val cf = swipeController.currentFocus) {
                EdgeEditorElement.RatioPresetMenu -> ratioBtnBoundsInWindow
                EdgeEditorElement.ActionTypeToggle -> actionTypeBtnBoundsInWindow
                EdgeEditorElement.RevertToAuto -> revertBtnBoundsInWindow
                else -> swipeController.boundsOf(cf)
            }
            var lastIconButtonTooltip by remember { mutableStateOf("") }
            var lastTooltipAnchor by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
            if (rawIconButtonTooltip != null) {
                lastIconButtonTooltip = rawIconButtonTooltip
                if (tooltipAnchorBounds != null) lastTooltipAnchor = tooltipAnchorBounds
            }
            if (rawIconButtonTooltip != null && lastTooltipAnchor != androidx.compose.ui.geometry.Rect.Zero) {
                val density = LocalDensity.current
                val anchorTopPx = lastTooltipAnchor.top.toInt()
                val anchorCenterXPx = ((lastTooltipAnchor.left + lastTooltipAnchor.right) / 2f).toInt()
                Popup(
                    popupPositionProvider = remember(anchorTopPx, anchorCenterXPx) {
                        object : PopupPositionProvider {
                            override fun calculatePosition(
                                anchorBounds: IntRect,
                                windowSize: IntSize,
                                layoutDirection: LayoutDirection,
                                popupContentSize: IntSize
                            ): IntOffset {
                                val tooltipGapPx = with(density) { 6.dp.roundToPx() }
                                val x = (anchorCenterXPx - popupContentSize.width / 2)
                                    .coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
                                val y = anchorTopPx - popupContentSize.height - tooltipGapPx
                                return IntOffset(x, y)
                            }
                        }
                    },
                    properties = PopupProperties(focusable = false),
                ) {
                    Surface(
                        color = cs.inverseOnSurface.copy(alpha = 0.92f),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            text = lastIconButtonTooltip,
                            color = cs.inverseSurface,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        // ── NORMAL 모드: 미니버튼 롱프레스 툴팁 ──
        if (inputMode == InputMode.NORMAL && normalMiniTooltipText.isNotEmpty() && normalMiniTooltipAnchor != androidx.compose.ui.geometry.Rect.Zero) {
            val densityNt = LocalDensity.current
            val anchorTopPxNt = normalMiniTooltipAnchor.top.toInt()
            val anchorCenterXPxNt = ((normalMiniTooltipAnchor.left + normalMiniTooltipAnchor.right) / 2f).toInt()
            Popup(
                popupPositionProvider = remember(anchorTopPxNt, anchorCenterXPxNt) {
                    object : PopupPositionProvider {
                        override fun calculatePosition(
                            anchorBounds: IntRect,
                            windowSize: IntSize,
                            layoutDirection: LayoutDirection,
                            popupContentSize: IntSize
                        ): IntOffset {
                            val gapPx = with(densityNt) { 6.dp.roundToPx() }
                            val x = (anchorCenterXPxNt - popupContentSize.width / 2)
                                .coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
                            val y = anchorTopPxNt - popupContentSize.height - gapPx
                            return IntOffset(x, y)
                        }
                    }
                },
                properties = PopupProperties(focusable = false),
            ) {
                Surface(
                    color = cs.inverseOnSurface.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        text = normalMiniTooltipText,
                        color = cs.inverseSurface,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }
        }

        // ── SWIPE 모드: 애플워치 서랍 아이콘 선택 패널 ──
        // SwipeGestureLayer 직전 z-order에 배치 → 제스처 레이어가 최상단을 유지
        if (inputMode == InputMode.SWIPE) {
            AnimatedVisibility(
                visible = showIconSheet,
                enter = fadeIn(tween(CATEGORY_DRAWER_OPEN_DURATION_MS)) +
                    scaleIn(tween(CATEGORY_DRAWER_OPEN_DURATION_MS), initialScale = 0.92f),
                exit = fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.92f),
                modifier = Modifier.matchParentSize(),
            ) {
                val displayIconKeyForDrawer = run {
                    val trigger = selectedZone?.trigger as? EdgeZoneTrigger.SingleAction
                    trigger?.iconKey?.ifEmpty { selectedZone?.action?.defaultIconKey() ?: "" } ?: ""
                }
                CategoryIconDrawer(
                    controller = swipeController,
                    stage = iconDrawerStage,
                    onStageChange = { iconDrawerStage = it },
                    selectedIconKey = displayIconKeyForDrawer,
                    anchorCenterInWindow = iconBoxCenterInWindow,
                    scope = EdgeEditorScope.IconSheet,
                    onPick = { key ->
                        nextFocusOnZoneChange = EdgeEditorElement.IconBox
                        selectedZone?.let { updateSelectedZone(it.withIconKey(key)) }
                        showIconSheet = false
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        // ── SWIPE 모드: 액션 순환 후보 아이콘 서랍 (단일 액션과 동일한 CategoryIconDrawer) ──
        if (inputMode == InputMode.SWIPE) {
            AnimatedVisibility(
                visible = showCandidateIconSheet,
                enter = fadeIn(tween(CATEGORY_DRAWER_OPEN_DURATION_MS)) +
                    scaleIn(tween(CATEGORY_DRAWER_OPEN_DURATION_MS), initialScale = 0.92f),
                exit = fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.92f),
                modifier = Modifier.matchParentSize(),
            ) {
                val displayIconKeyForCandidate =
                    rotationDraft.iconKey.ifEmpty { rotationDraft.action.defaultIconKey() }
                CategoryIconDrawer(
                    controller = swipeController,
                    stage = iconDrawerStage,
                    onStageChange = { iconDrawerStage = it },
                    selectedIconKey = displayIconKeyForCandidate,
                    anchorCenterInWindow = candidateIconBoxCenterInWindow,
                    scope = EdgeEditorScope.IconSheet,
                    onPick = { key ->
                        rotationDraft = rotationDraft.copy(iconKey = key)
                        swipeController.setFocus(EdgeEditorElement.RotationCandidateIconBox)
                        showCandidateIconSheet = false
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        // ── SWIPE 모드: 컬러 피커 패널 ──
        if (inputMode == InputMode.SWIPE && showColorPicker) {
            val currentColorHex = run {
                val trigger = selectedZone?.trigger as? EdgeZoneTrigger.SingleAction
                trigger?.colorHex ?: ""
            }
            com.bridgeone.app.ui.components.colorpicker.ColorPickerSwipe(
                controller = swipeController,
                pickerScope = EdgeEditorScope.ColorPicker,
                selectedColorHex = currentColorHex,
                anchorCenterInWindow = colorBoxCenterInWindow,
                stage = colorPickerStage,
                onStageChange = { colorPickerStage = it },
                onPick = { hex ->
                    selectedZone?.let { updateSelectedZone(it.withColor(hex)) }
                    showColorPicker = false
                },
                onCommitCandidateChange = { colorCommitCandidate = it },
                modifier = Modifier.matchParentSize(),
            )
        }

        // ── SWIPE 모드: 액션 순환 후보 컬러 피커 (단일 액션과 동일한 ColorPickerSwipe) ──
        if (inputMode == InputMode.SWIPE && showCandidateColorPicker) {
            com.bridgeone.app.ui.components.colorpicker.ColorPickerSwipe(
                controller = swipeController,
                pickerScope = EdgeEditorScope.ColorPicker,
                selectedColorHex = rotationDraft.colorHex,
                anchorCenterInWindow = candidateColorBoxCenterInWindow,
                stage = colorPickerStage,
                onStageChange = { colorPickerStage = it },
                onPick = { hex ->
                    rotationDraft = rotationDraft.copy(colorHex = hex)
                    showCandidateColorPicker = false
                },
                onCommitCandidateChange = { colorCommitCandidate = it },
                modifier = Modifier.matchParentSize(),
            )
        }

        // ── SWIPE 모드 Undo 히스토리 드롭다운 오버레이 ──
        // Popup 대신 메인 Box 내부 인라인으로 렌더링해야 SwipeGestureLayer가 터치를 수신함.
        // Popup은 별도 Window를 생성해서 Popup 영역 내 터치가 메인 Window를 우회하므로 스와이프 불가.
        // ⚠️ SwipeGestureLayer보다 먼저 배치해야 함: Material3 Surface가 포인터를 소비하므로
        //    이 블록이 제스처 레이어보다 위(나중)에 그려지면 드롭다운 내부에서 시작한 스와이프가
        //    제스처 레이어에 도달하지 못한다 (아이콘 서랍/컬러 피커와 동일한 순서 규칙).
        if (inputMode == InputMode.SWIPE) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = 4.dp),
                contentAlignment = Alignment.TopEnd,
            ) {
                AnimatedVisibility(
                    visible = showUndoMenu,
                    modifier = Modifier.offset { IntOffset(0, undoMenuAnchorBottom) },
                    enter = fadeIn(tween(180)) +
                        expandVertically(
                            animationSpec = tween(200, easing = FastOutSlowInEasing),
                            expandFrom = Alignment.Top,
                        ),
                    exit = fadeOut(tween(120)) +
                        shrinkVertically(
                            animationSpec = tween(160),
                            shrinkTowards = Alignment.Top,
                        ),
                ) {
                    UndoHistorySwipePopup(
                        undoStack = undoStack,
                        workConfig = workConfig,
                        onApply = { config, idx ->
                            nextFocusOnZoneChange = EdgeEditorElement.Undo
                            workConfig = config
                            undoStack = undoStack.drop(idx + 1)
                            currentPresetId = null
                            val sel = selectedZone
                            selectedZone = if (sel != null) {
                                config.zonesFor(sel.edge).firstOrNull { it.startRatio == sel.startRatio }
                                    ?: config.zonesFor(sel.edge).firstOrNull()
                            } else null
                            showUndoMenu = false
                        },
                    )
                }
            }
        }
        // ── 프리셋 팝업 오버레이 (SWIPE 모드 인라인 배치) ──
        // 메인 Box 안, SwipeGestureLayer보다 먼저 배치 → 제스처 레이어가 터치를 수신.
        // SWIPE: scrim clickable 없어서 터치가 제스처 레이어로 통과.
        // NORMAL: scrim clickable로 닫기 (팝업 내부에서 inputMode로 분기).
        // AnimatedVisibility로 등장/닫기 페이드+스케일 애니메이션.
        presetsRepo?.let { repo ->
            AnimatedVisibility(
                visible = showPresetPopup,
                enter = fadeIn(tween(180)) + scaleIn(initialScale = 0.92f, animationSpec = tween(180)),
                exit = fadeOut(tween(140)) + scaleOut(targetScale = 0.92f, animationSpec = tween(140)),
            ) {
                EdgeZonePresetPopup(
                    inputMode = inputMode,
                    stage = presetPopupStage,
                    onStageChange = { presetPopupStage = it },
                    currentPresetId = currentPresetId,
                    currentConfig = workConfig,
                    presetsRepo = repo,
                    onApply = { preset ->
                        state.pushUndo()
                        workConfig = preset.config.stripActions()
                        currentPresetId = preset.id
                        selectedZone = null
                        showPresetPopup = false
                    },
                    onDismiss = { showPresetPopup = false },
                    onRequestTextKeyboard = { initial, onResult ->
                        presetNameSeed = initial
                        presetNameKeyboard = onResult
                    },
                )
            }
        }
        // ── SWIPE 모드 프리셋 이름 입력 키보드 오버레이 ──
        // PresetPopup의 '새 프리셋 저장'·'이름 변경' 시 호출. 라벨 키보드와 동일 패턴.
        if (inputMode == InputMode.SWIPE && presetNameKeyboard != null) {
            Box(modifier = Modifier.fillMaxSize()) {
                com.bridgeone.app.ui.components.SwipeKeyboardOverlay(
                    initialText = presetNameSeed,
                    maxLength = 32,
                    suggestions = emptyList(),
                    revertOnCancel = false,
                    showScrim = true,
                    gestureFullHeight = true,
                    onTextChange = {},
                    onCancel = { presetNameKeyboard = null },
                    onDone = { result ->
                        presetNameKeyboard?.invoke(result)
                        presetNameKeyboard = null
                    }
                )
            }
        }
        // ── 커스텀 단축키 팝업 인라인 오버레이 (SWIPE 모드) ──
        // Popup은 별도 Android Window를 생성하므로 팝업 영역 내 터치가 SwipeGestureLayer에 도달하지 못함.
        // Box 인라인으로 배치하면 같은 Window 안에서 SwipeGestureLayer가 모든 터치를 수신함.
        if (inputMode == InputMode.SWIPE && swipeShortcutVisible) {
            ShortcutEditorPopup(
                draft = swipeShortcutDraft,
                inputMode = InputMode.SWIPE,
                onDraftChange = { swipeShortcutDraft = it },
                onCancel = { swipeShortcutVisible = false },
                onConfirm = { confirmed ->
                    swipeShortcutOnConfirm(confirmed)
                    swipeShortcutVisible = false
                },
                onAddAsCandidate = if (swipeShortcutOnAddAsCandidate != null) {
                    { iconKey, name -> swipeShortcutOnAddAsCandidate?.invoke(swipeShortcutDraft, iconKey, name); swipeShortcutVisible = false }
                } else null,
                onNameKeyboardActiveChange = { shortcutNameKbActive = it },
                onRequestIconPicker = { current, anchor, onResult ->
                    iconPickerReturnElement = EdgeEditorElement.ShortcutIconButton
                    shortcutIconSeed = current
                    shortcutIconAnchor = anchor
                    shortcutIconOnPick = onResult
                    iconDrawerStage = IconDrawerStage.Category
                    shortcutIconSheetVisible = true
                },
            )
        }
        if (inputMode == InputMode.SWIPE && macroEditorVisible) {
            MacroEditorPopup(
                draft = macroEditorDraft,
                inputMode = InputMode.SWIPE,
                onDraftChange = { macroEditorDraft = it },
                onCancel = { macroEditorVisible = false; macroNameKbActive = false },
                onConfirm = { confirmed, icon, name ->
                    macroEditorOnConfirm(confirmed, icon, name)
                    macroEditorVisible = false; macroNameKbActive = false
                },
                onAddAsPreset = macroEditorOnAddAsPreset?.let {
                    { draft, icon, name -> it(draft, icon, name) }
                },
                onNameKeyboardActiveChange = { macroNameKbActive = it },
                onRequestIconPicker = { current, anchor, onResult ->
                    iconPickerReturnElement = EdgeEditorElement.MacroIconButton
                    shortcutIconSeed = current
                    shortcutIconAnchor = anchor
                    shortcutIconOnPick = onResult
                    iconDrawerStage = IconDrawerStage.Category
                    shortcutIconSheetVisible = true
                },
                initialIconKey = macroEditorInitialIconKey,
                initialName = macroEditorInitialName,
                customShortcutPresets = localCustomShortcutPresets,
                onSetNormalTooltip = { text, anchor -> normalMiniTooltipText = text; normalMiniTooltipAnchor = anchor },
            )
        }
        // NORMAL 모드 매크로 편집기 Popup
        if (inputMode == InputMode.NORMAL && macroEditorVisible) {
            MacroEditorPopup(
                draft = macroEditorDraft,
                inputMode = InputMode.NORMAL,
                onDraftChange = { macroEditorDraft = it },
                onCancel = { macroEditorVisible = false },
                onConfirm = { confirmed, icon, name ->
                    macroEditorOnConfirm(confirmed, icon, name)
                    macroEditorVisible = false
                },
                onAddAsPreset = macroEditorOnAddAsPreset?.let {
                    { draft, icon, name -> it(draft, icon, name) }
                },
                initialIconKey = macroEditorInitialIconKey,
                initialName = macroEditorInitialName,
                customShortcutPresets = localCustomShortcutPresets,
                onSetNormalTooltip = { text, anchor -> normalMiniTooltipText = text; normalMiniTooltipAnchor = anchor },
            )
        }
        // ── SWIPE 모드: 단축키/매크로 팝업 아이콘 선택 서랍 ──
        // MacroEditorPopup/ShortcutEditorPopup보다 나중에 그려야 서랍이 두 팝업 위(z-order 최상위)에 표시됨.
        if (inputMode == InputMode.SWIPE) {
            AnimatedVisibility(
                visible = shortcutIconSheetVisible,
                enter = fadeIn(tween(CATEGORY_DRAWER_OPEN_DURATION_MS)) +
                    scaleIn(tween(CATEGORY_DRAWER_OPEN_DURATION_MS), initialScale = 0.92f),
                exit = fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.92f),
                modifier = Modifier.matchParentSize(),
            ) {
                CategoryIconDrawer(
                    controller = swipeController,
                    stage = iconDrawerStage,
                    onStageChange = { iconDrawerStage = it },
                    selectedIconKey = shortcutIconSeed,
                    anchorCenterInWindow = shortcutIconAnchor,
                    scope = EdgeEditorScope.IconSheet,
                    onPick = { key ->
                        shortcutIconOnPick?.invoke(key)
                        shortcutIconSheetVisible = false
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        // ── SWIPE 모드 제스처 오버레이 (화면 최상단) ──
        // 라벨 키보드 또는 프리셋 이름 키보드 활성 시에는 키보드 자체 스와이프 시스템에 입력을 양보
        val isLabelKeyboardActive = showLabelKeyboard || candidateLabelKeyboard != null || presetNameKeyboard != null || shortcutNameKbActive || macroNameKbActive
        if (inputMode == InputMode.SWIPE && !isLabelKeyboardActive) {
            SwipeGestureLayer(
                controller = swipeController,
                modifier = Modifier.matchParentSize(),
                onLongPress = {
                    when {
                        showPresetPopup -> {
                            // 프리셋 팝업: 단계별 뒤로가기 / 닫기
                            when (presetPopupStage) {
                                PopupStage.GRID -> showPresetPopup = false
                                PopupStage.CONFIRM, PopupStage.SAVE_NAME -> presetPopupStage = PopupStage.GRID
                                PopupStage.EDIT_NAME -> presetPopupStage = PopupStage.CONFIRM
                            }
                            true
                        }
                        showUndoMenu -> {
                            // Undo 히스토리 메뉴가 열린 상태에서 롱프레스 → 선택 없이 닫기
                            showUndoMenu = false
                            true
                        }
                        shortcutIconSheetVisible -> {
                            // 단축키 팝업 아이콘 서랍: 아이콘 단계 → 카테고리 복귀, 카테고리 → 닫기
                            if (iconDrawerStage is IconDrawerStage.Icons) {
                                iconDrawerStage = IconDrawerStage.Category
                            } else {
                                shortcutIconSheetVisible = false
                            }
                            true
                        }
                        showIconSheet -> {
                            // 아이콘 서랍: 아이콘 단계 → 카테고리 복귀, 카테고리 → 닫기
                            if (iconDrawerStage is IconDrawerStage.Icons) {
                                iconDrawerStage = IconDrawerStage.Category
                            } else {
                                showIconSheet = false
                            }
                            true
                        }
                        showCandidateIconSheet -> {
                            // 후보 아이콘 서랍: 아이콘 단계 → 카테고리 복귀, 카테고리 → 닫기
                            if (iconDrawerStage is IconDrawerStage.Icons) {
                                iconDrawerStage = IconDrawerStage.Category
                            } else {
                                showCandidateIconSheet = false
                            }
                            true
                        }
                        showColorPicker || showCandidateColorPicker -> {
                            // 단계별 뒤로가기: DirectInput→Swatches(or Category), Swatches→Category, Category→닫기
                            when (val s = colorPickerStage) {
                                is com.bridgeone.app.ui.components.colorpicker.ColorPickerStage.DirectInput ->
                                    colorPickerStage = if (s.sourceTab != null)
                                        com.bridgeone.app.ui.components.colorpicker.ColorPickerStage.Swatches(s.sourceTab)
                                    else
                                        com.bridgeone.app.ui.components.colorpicker.ColorPickerStage.Category
                                is com.bridgeone.app.ui.components.colorpicker.ColorPickerStage.Swatches ->
                                    colorPickerStage = com.bridgeone.app.ui.components.colorpicker.ColorPickerStage.Category
                                is com.bridgeone.app.ui.components.colorpicker.ColorPickerStage.Category -> {
                                    showColorPicker = false
                                    showCandidateColorPicker = false
                                }
                            }
                            true
                        }
                        swipeController.currentFocus == EdgeEditorElement.Undo -> {
                            // Undo 버튼이 포커스된 상태에서 롱프레스 → 히스토리 메뉴 열기
                            swipeController.activateAlt()
                            true
                        }
                        swipeController.currentFocus == EdgeEditorElement.MacroStepSaveContinue -> {
                            // 연속 추가 버튼 롱프레스 → 반복 횟수 팝업 열기
                            swipeController.activateAlt()
                            true
                        }
                        // 커스텀 프리셋 수정/삭제 메뉴가 열린 상태에서 롱프레스 → 닫기
                        swipeCustomMenuTarget != null -> {
                            swipeCustomMenuTarget = null
                            true
                        }
                        // 개별 딜레이 슬라이더 포커스 + 롱프레스 → ⏱ 토글 OFF (delayAfterMs = null)
                        swipeController.currentFocus is EdgeEditorElement.MacroStepDelaySlider -> {
                            val sliderIdx = (swipeController.currentFocus as EdgeEditorElement.MacroStepDelaySlider).index
                            val curSteps = macroEditorDraft.steps
                            if (sliderIdx in curSteps.indices) {
                                val newSteps = curSteps.toMutableList()
                                newSteps[sliderIdx] = curSteps[sliderIdx].copy(delayAfterMs = null)
                                macroEditorDraft = macroEditorDraft.copy(steps = newSteps)
                                swipeController.setFocus(EdgeEditorElement.MacroStepDelayExpand(sliderIdx))
                            }
                            true
                        }
                        // Initial 팝업에서 롱프레스 → 팝업 종료
                        zonePopup is ZoneActionPopup.Initial -> {
                            zonePopup = ZoneActionPopup.None
                            true
                        }
                        // MergeSelecting 상태에서 롱프레스 → Initial 복귀
                        zonePopup is ZoneActionPopup.MergeSelecting -> {
                            val ms = zonePopup as ZoneActionPopup.MergeSelecting
                            zonePopup = ZoneActionPopup.Initial(ms.zone, ms.anchor)
                            swipeController.setFocus(EdgeEditorElement.ZoneActionMerge)
                            true
                        }
                        // SplitChoosing 상태에서 분할 개수 버튼 포커스 + 롱프레스 → Initial 복귀
                        swipeController.currentFocus is EdgeEditorElement.ZoneActionSplitN -> {
                            swipeController.activateAlt()
                            true
                        }
                        // StripZone 포커스 + 롱프레스 → 3버튼 팝업(Initial) 열기
                        swipeController.currentFocus is EdgeEditorElement.StripZone -> {
                            swipeController.activateAlt()
                            true
                        }
                        // 커스텀 ActionOptionCard 포커스 → 수정/삭제 메뉴 열기
                        swipeController.currentFocus is EdgeEditorElement.ActionOptionCard -> {
                            val key = (swipeController.currentFocus as EdgeEditorElement.ActionOptionCard).key
                            val parts = key.split(":", limit = 2)
                            val domain = parts.getOrNull(0)
                            val label = parts.getOrNull(1)
                            val target: CustomPresetTarget? = when (domain) {
                                "DYNAMICS" -> localCustomPresets.firstOrNull { it.name == label }
                                    ?.let { CustomPresetTarget.Dynamics(it) }
                                "COMBO" -> localCustomShortcutPresets.firstOrNull {
                                    formatShortcutCombo(it.modifierBits, it.keyCodes) == label
                                }?.let { CustomPresetTarget.Shortcut(it) }
                                "MACRO" -> localCustomMacroPresets.firstOrNull {
                                    formatMacroSteps(it.steps) == label
                                }?.let { CustomPresetTarget.Macro(it) }
                                else -> null
                            }
                            if (target != null) {
                                swipeCustomMenuTarget = target
                                true
                            } else {
                                ToastController.show("커스텀 프리셋만 수정하거나 삭제할 수 있습니다", ToastType.INFO)
                                true
                            }
                        }
                        else -> false
                    }
                },
            )
        }
}
