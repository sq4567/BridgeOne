package com.bridgeone.app.ui.components.touchpad

import android.annotation.SuppressLint
import kotlinx.coroutines.delay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.widthIn

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bridgeone.app.ui.common.CustomPointerDynamicsPreset
import com.bridgeone.app.ui.common.CustomPresetsRepository
import com.bridgeone.app.ui.common.DYNAMICS_PRESETS
import com.bridgeone.app.ui.common.EdgeSwipeConstants
import com.bridgeone.app.ui.common.EdgeZonePreset
import com.bridgeone.app.ui.common.EdgeZonePresetsRepository
import com.bridgeone.app.ui.common.stripActions
import com.bridgeone.app.ui.common.InputMode
import com.bridgeone.app.ui.common.MODE_PRESETS
import com.bridgeone.app.ui.common.ToastController
import com.bridgeone.app.ui.common.ToastType
import com.bridgeone.app.ui.common.customPresetIconOrNull
import com.bridgeone.app.ui.common.loadInputMode
import com.bridgeone.app.ui.common.loadSwipeWrapEdge
import com.bridgeone.app.ui.common.loadLastShortcutSingleKeyMode
import com.bridgeone.app.ui.common.saveLastShortcutSingleKeyMode
import com.bridgeone.app.ui.common.swipe.LocalSwipeFocusController
import com.bridgeone.app.ui.common.swipe.LocalSwipeFocused
import com.bridgeone.app.ui.common.swipe.SwipeFocusController
import com.bridgeone.app.ui.common.swipe.SwipeFocusable
import com.bridgeone.app.ui.common.swipe.SwipeGestureLayer
import com.bridgeone.app.ui.common.swipe.SwipeMode
import com.bridgeone.app.ui.common.swipe.rememberSwipeFocusController
import com.bridgeone.app.ui.common.COLOR_SWATCH_PALETTE
import com.bridgeone.app.ui.common.ColorCodec
import com.bridgeone.app.ui.common.MACRO_STEP_LIST_RESERVED_DP
import com.bridgeone.app.ui.common.MACRO_DIALOG_MAX_SCREEN_FRACTION
import com.bridgeone.app.ui.common.MACRO_MAX_HELD_KEYS
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import com.bridgeone.app.ui.common.ColorPickerConstants


import com.bridgeone.app.ui.components.SwipeKeyboardOverlay
import com.bridgeone.app.ui.components.DEFAULT_SHORTCUTS
import com.bridgeone.app.ui.common.MOD_BIT_LCTRL
import com.bridgeone.app.ui.common.MOD_BIT_LSHIFT
import com.bridgeone.app.ui.common.MOD_BIT_LALT
import com.bridgeone.app.ui.common.MOD_BIT_LGUI
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlin.math.roundToInt

/** 커스텀 배율 슬라이더 트랙 높이 (dp). 기본값: 28f */
private const val CUSTOM_SLIDER_TRACK_HEIGHT_DP = 28f
/** 커스텀 배율 슬라이더 손잡이 세로선 너비 (dp). 기본값: 3f */
private const val CUSTOM_SLIDER_LINE_WIDTH_DP = 3f

// ── 영역 비율 인라인 액션 팝업 상태 ──
private sealed class ZoneActionPopup {
    object None : ZoneActionPopup()
    data class Initial(val zone: EdgeZone, val anchor: Float) : ZoneActionPopup()
    data class MergeSelecting(val zone: EdgeZone) : ZoneActionPopup()
    data class SplitChoosing(val zone: EdgeZone, val anchor: Float) : ZoneActionPopup()
    data class DeleteConfirming(val zone: EdgeZone, val anchor: Float) : ZoneActionPopup()
}

/** 커스텀 프리셋 수정/삭제 메뉴 대상 (다이나믹스, 단축키, 또는 매크로) */
private sealed class CustomPresetTarget {
    data class Dynamics(val preset: CustomPointerDynamicsPreset) : CustomPresetTarget()
    data class Shortcut(val preset: com.bridgeone.app.ui.common.CustomShortcutPreset) : CustomPresetTarget()
    data class Macro(val preset: com.bridgeone.app.ui.common.CustomMacroPreset) : CustomPresetTarget()
}

/**
 * 엣지 존 풀스크린 편집기 (UI/UX 리디자인).
 *
 * @param initialConfig  편집 시작 시 로드할 설정
 * @param onSave         저장 확정 시 호출
 * @param onBack         뒤로/취소
 */
@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EdgeZoneEditorScreen(
    initialConfig: EdgeZoneConfig,
    initialPresetId: String? = null,
    presetsRepo: EdgeZonePresetsRepository? = null,
    disabledEdges: Map<EntryEdge, String> = emptyMap(),
    bottomLeftButtonLabel: String? = "다이나믹스",
    bottomRightButtonLabel: String? = "모드 프리셋",
    customPresets: List<CustomPointerDynamicsPreset> = emptyList(),
    customPresetsRepo: CustomPresetsRepository? = null,
    onCustomPresetsChange: (List<CustomPointerDynamicsPreset>) -> Unit = {},
    customShortcutPresetsRepo: com.bridgeone.app.ui.common.CustomShortcutPresetsRepository? = null,
    customMacroPresetsRepo: com.bridgeone.app.ui.common.CustomMacroPresetsRepository? = null,
    onSave: (EdgeZoneConfig, presetId: String?) -> Unit,
    onBack: () -> Unit,
    pageCount: Int = 5
) {
    val cs = MaterialTheme.colorScheme
    val minRatio = EdgeSwipeConstants.MIN_ZONE_RATIO
    val maxZones = EdgeSwipeConstants.MAX_ZONES_PER_EDGE.toInt()

    // SWIPE 모드 인프라
    val context = LocalContext.current
    val inputMode = remember { loadInputMode(context) }
    val swipeController = rememberSwipeFocusController()
    if (inputMode == InputMode.SWIPE) {
        swipeController.wrapEdge = remember { loadSwipeWrapEdge(context) }
    }

    // Phase 4.7.5-A: 편집 상태 홀더. 기존 지역 변수명은 MutableState 위임으로 유지.
    val state = remember(initialConfig) { EdgeZoneEditorState(initialConfig, initialPresetId) }
    var workConfig by state.workConfigState
    var selectedZone by state.selectedZoneState
    var currentPresetId by state.currentPresetIdState
    var undoStack by state.undoStackState
    var savedRotationTrigger by remember { mutableStateOf<EdgeZoneTrigger.Rotation?>(null) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showIconSheet by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    // 커스텀 단축키 팝업 — SWIPE 모드에서는 Popup 대신 Box 인라인 오버레이로 렌더링
    var swipeShortcutVisible by remember { mutableStateOf(false) }
    var swipeShortcutDraft by remember { mutableStateOf(EdgeZoneAction.SendShortcut(0)) }
    var swipeShortcutOnConfirm: (EdgeZoneAction.SendShortcut) -> Unit by remember { mutableStateOf({}) }
    var swipeShortcutOnAddAsCandidate: ((draft: EdgeZoneAction.SendShortcut, iconKey: String, name: String) -> Unit)? by remember { mutableStateOf(null) }
    // 커스텀 매크로 편집기 — SWIPE 모드에서는 Box 인라인 오버레이로 렌더링 (NORMAL은 Popup)
    var macroEditorVisible by remember { mutableStateOf(false) }
    var macroEditorDraft by remember { mutableStateOf(EdgeZoneAction.SendMacro()) }
    var macroEditorOnConfirm: (EdgeZoneAction.SendMacro, String, String) -> Unit by remember { mutableStateOf({ _, _, _ -> }) }
    var macroEditorOnAddAsPreset: ((EdgeZoneAction.SendMacro, String, String) -> Unit)? by remember { mutableStateOf(null) }
    var macroEditorInitialIconKey by remember { mutableStateOf("") }
    var macroEditorInitialName by remember { mutableStateOf("") }
    // SWIPE 모드 매크로 이름/문자열 입력 키보드 활성 여부 (제스처 양보용). 기본값: false
    var macroNameKbActive by remember { mutableStateOf(false) }
    // NORMAL 모드 매크로 편집기 미니버튼 롱프레스 툴팁 상태 (호이스팅). 기본값: ""
    var normalMiniTooltipText by remember { mutableStateOf("") }
    var normalMiniTooltipAnchor by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
    var colorPickerStage by remember {
        mutableStateOf<com.bridgeone.app.ui.components.colorpicker.ColorPickerStage>(
            com.bridgeone.app.ui.components.colorpicker.ColorPickerStage.Category
        )
    }
    var showLabelKeyboard by remember { mutableStateOf(false) }
    var candidateLabelKeyboard by remember { mutableStateOf<((String) -> Unit)?>(null) }
    var candidateLabelCurrent by remember { mutableStateOf("") }
    var showPresetPopup by remember { mutableStateOf(false) }
    var presetPopupStage by remember { mutableStateOf(PopupStage.GRID) }
    // SWIPE 모드 프리셋 이름 입력 키보드 상태. null이면 비활성. 기본값: null
    var presetNameKeyboard by remember { mutableStateOf<((String) -> Unit)?>(null) }
    var presetNameSeed by remember { mutableStateOf("") }
    // SWIPE 모드 단축키 액션명 입력 키보드 활성 여부. 팝업 내부가 관리하고 본체는 scope 전환·제스처 양보에만 사용. 기본값: false
    var shortcutNameKbActive by remember { mutableStateOf(false) }
    var showUndoMenu by remember { mutableStateOf(false) }
    // Undo 버튼 하단 y 좌표 (px, in window). 드롭다운 위치 계산용. 기본값: 0
    var undoMenuAnchorBottom by remember { mutableIntStateOf(0) }
    // 커스텀 다이나믹스 프리셋 편집기 상태
    var localCustomPresets by remember(customPresets) { mutableStateOf(customPresets) }
    // 커스텀 단축키 프리셋 상태
    var localCustomShortcutPresets by remember { mutableStateOf<List<com.bridgeone.app.ui.common.CustomShortcutPreset>>(emptyList()) }
    LaunchedEffect(Unit) { customShortcutPresetsRepo?.let { localCustomShortcutPresets = it.loadAll() } }
    // 커스텀 매크로 프리셋 상태
    var localCustomMacroPresets by remember { mutableStateOf<List<com.bridgeone.app.ui.common.CustomMacroPreset>>(emptyList()) }
    LaunchedEffect(Unit) { customMacroPresetsRepo?.let { localCustomMacroPresets = it.loadAll() } }
    var dynamicsEditorVisible by remember { mutableStateOf(false) }
    // 편집 대상 다이나믹스 프리셋. null이면 신규 생성, non-null이면 해당 프리셋 편집. 기본값: null
    var dynamicsEditorInitial by remember { mutableStateOf<CustomPointerDynamicsPreset?>(null) }
    // SWIPE 모드 커스텀 프리셋 수정/삭제 메뉴 대상. null이면 닫힘. 기본값: null
    var swipeCustomMenuTarget by remember { mutableStateOf<CustomPresetTarget?>(null) }
    var zonePopup by remember { mutableStateOf<ZoneActionPopup>(ZoneActionPopup.None) }
    var canvasVisible by remember { mutableStateOf(true) }
    var selectedEdge by remember { mutableStateOf<EntryEdge?>(null) }
    var nextFocusOnZoneChange by remember { mutableStateOf<EdgeEditorElement?>(null) }
    val iconSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var iconBoxCenterInWindow by remember { mutableStateOf(Offset.Zero) }
    var colorBoxCenterInWindow by remember { mutableStateOf(Offset.Zero) }
    var ratioBtnBoundsInWindow by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
    var actionTypeBtnBoundsInWindow by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
    var revertBtnBoundsInWindow by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
    // 롱프레스 색 확정 후보 hex. null이면 확정 대상 없음(ExpandToggle 등). 기본값: null
    var colorCommitCandidate by remember { mutableStateOf<String?>(null) }
    // 아이콘 서랍 단계 (카테고리 ↔ 아이콘). BackHandler 분기를 위해 상위가 소유.
    var iconDrawerStage by remember { mutableStateOf<IconDrawerStage>(IconDrawerStage.Category) }
    // 단축키 팝업 아이콘 선택 서랍 (SWIPE 전용). 기본값: false
    var shortcutIconSheetVisible by remember { mutableStateOf(false) }
    var shortcutIconSeed by remember { mutableStateOf("") }
    var shortcutIconOnPick by remember { mutableStateOf<((String) -> Unit)?>(null) }
    var shortcutIconAnchor by remember { mutableStateOf(Offset.Zero) }
    // 서랍을 연 진입점 — 닫을 때 이 요소로 포커스 복원. 기본값: ShortcutIconButton
    var iconPickerReturnElement by remember { mutableStateOf<EdgeEditorElement>(EdgeEditorElement.ShortcutIconButton) }

    // ── 액션 순환 후보 편집 상태 (RotationEditor에서 hoist) ──
    // 라벨 키보드 오버레이가 RotationEditor를 컴포지션에서 제거해도 편집 상태가 소멸하지 않도록 상위가 소유.
    var rotationEditingEntry by remember { mutableStateOf<Pair<Int?, RotationCandidate>?>(null) }
    var rotationDraft by remember { mutableStateOf(RotationCandidate(EdgeZoneAction.Unassigned, "", "")) }
    var showCandidateIconSheet by remember { mutableStateOf(false) }
    var showCandidateColorPicker by remember { mutableStateOf(false) }
    var candidateIconBoxCenterInWindow by remember { mutableStateOf(Offset.Zero) }
    var candidateColorBoxCenterInWindow by remember { mutableStateOf(Offset.Zero) }

    // ── 라벨 박스 커서 애니메이션 (편집 중 깜빡임) ──
    val labelCursorTransition = rememberInfiniteTransition(label = "labelCursor")
    val labelCursorAlpha by labelCursorTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 900
                1f at 0
                1f at 449
                0f at 450
                0f at 899
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "labelCursor_alpha"
    )

    // 선택 존이 바뀌면(다른 존 선택/존 해제) 후보 편집 폼과 저장된 rotation을 초기화한다.
    // 후보 편집 중 아이콘/색/라벨 변경은 selectedZone을 건드리지 않으므로 폼이 유지된다.
    LaunchedEffect(selectedZone?.startRatio, selectedZone?.edge) {
        rotationEditingEntry = null
        savedRotationTrigger = null
    }

    // Phase 4.7.5-A: undoStack/pushUndo/splitInto/tryMergeWith → EdgeZoneEditorState로 이관
    // (splitInto/tryMergeWith의 zonePopup 리셋은 호출부로 이동)

    // ── SWIPE 모드 초기 포커스 시드 ──
    // 진입 시 어느 요소도 선택되지 않은 상태를 피하기 위해 Back 버튼에 포커스를 둔다.
    // selectedZone이 생기면 (존 설정 뷰 진입) 해당 존의 StripZone 인덱스로 재시드한다.
    if (inputMode == InputMode.SWIPE) {
        LaunchedEffect(Unit) {
            if (swipeController.currentFocus == null) {
                swipeController.setFocus(EdgeEditorElement.Back)
            }
        }
        LaunchedEffect(selectedZone) {
            val zone = selectedZone
            if (zone != null) {
                val override = nextFocusOnZoneChange
                if (override != null) {
                    nextFocusOnZoneChange = null
                    swipeController.setFocus(override)
                } else if (swipeController.currentFocus != EdgeEditorElement.CustomMultiplierSlider) {
                    // 슬라이더 조작 중 selectedZone이 반복 변경될 때 포커스를 빼앗지 않음
                    val zoneList = workConfig.zonesFor(zone.edge)
                    val idx = zoneList.indexOfFirst {
                        it.startRatio == zone.startRatio && it.edge == zone.edge
                    }.coerceAtLeast(0)
                    swipeController.setFocus(EdgeEditorElement.StripZone(idx))
                }
            }
        }
    }

    // ── SWIPE 모드 scope 트래킹 ──
    // 각 popup/menu가 열려 있는 동안 controller의 scope 스택에 푸시하여
    // 포커스 가능한 요소를 popup 내부로 한정한다.
    // showPresetPopup 열릴 때 stage 리셋 (SWIPE의 포커스는 아래 DisposableEffect에서 scope 직후 설정)
    LaunchedEffect(showPresetPopup) {
        if (showPresetPopup) {
            presetPopupStage = PopupStage.GRID
        }
    }
    // CONFIRM 단계 진입 시 적용 버튼 포커스
    LaunchedEffect(presetPopupStage) {
        if (presetPopupStage == PopupStage.CONFIRM) {
            swipeController.setFocus(EdgeEditorElement.PresetConfirmApply)
        }
    }

    if (inputMode == InputMode.SWIPE) {
        DisposableEffect(showPresetPopup) {
            val active = showPresetPopup
            if (active) {
                swipeController.pushScope(EdgeEditorScope.PresetPopup)
                // pushScope가 currentFocus를 null로 리셋하므로, scope 진입 직후 "기본" 포커스 지정
                val firstPreset = presetsRepo?.loadAll()?.firstOrNull()
                if (firstPreset != null) {
                    swipeController.setFocus(EdgeEditorElement.PresetItem(firstPreset.id))
                }
            }
            onDispose {
                if (active) {
                    swipeController.popScope()
                    swipeController.setFocus(EdgeEditorElement.PresetBadge)
                }
            }
        }
        // 프리셋 이름 입력 키보드: PresetPopup scope 안에서 LabelKeyboard scope로 전환
        DisposableEffect(presetNameKeyboard != null) {
            val active = presetNameKeyboard != null
            if (active) swipeController.pushScope(EdgeEditorScope.LabelKeyboard)
            onDispose { if (active) swipeController.popScope() }
        }
        DisposableEffect(showIconSheet) {
            val active = showIconSheet  // dispose 시점 재평가 방지 — Compose state는 onDispose에서 현재값을 읽음
            if (active) {
                swipeController.pushScope(EdgeEditorScope.IconSheet)
                swipeController.maxFocusStepsPerEvent = 1  // 기본값: Int.MAX_VALUE
            }
            onDispose {
                if (active) {
                    swipeController.popScope()
                    swipeController.maxFocusStepsPerEvent = Int.MAX_VALUE
                    // popScope()는 currentFocus를 null로 초기화하므로 즉시 IconBox로 복원.
                    // LaunchedEffect보다 onDispose가 먼저 실행되므로 여기가 가장 안전한 위치.
                    swipeController.setFocus(EdgeEditorElement.IconBox)
                }
            }
        }
        DisposableEffect(shortcutIconSheetVisible) {
            val active = shortcutIconSheetVisible
            val returnElement = iconPickerReturnElement  // 진입 시점에 캡처 — onDispose에서 state 재평가 방지
            if (active) {
                swipeController.pushScope(EdgeEditorScope.IconSheet)
                swipeController.maxFocusStepsPerEvent = 1
            }
            onDispose {
                if (active) {
                    swipeController.popScope()
                    swipeController.maxFocusStepsPerEvent = Int.MAX_VALUE
                    swipeController.setFocus(returnElement)
                }
            }
        }
        // 단축키 액션명 입력 키보드: ShortcutPopup scope 안에서 LabelKeyboard scope로 전환
        DisposableEffect(shortcutNameKbActive) {
            val active = shortcutNameKbActive
            if (active) swipeController.pushScope(EdgeEditorScope.LabelKeyboard)
            onDispose {
                if (active) {
                    swipeController.popScope()
                    swipeController.setFocus(EdgeEditorElement.ShortcutNameField)
                }
            }
        }
        // 매크로 이름/문자열 입력 키보드: MacroPopup scope 안에서 LabelKeyboard scope로 전환
        DisposableEffect(macroNameKbActive) {
            val active = macroNameKbActive
            if (active) swipeController.pushScope(EdgeEditorScope.LabelKeyboard)
            onDispose {
                if (active) {
                    swipeController.popScope()
                    swipeController.setFocus(EdgeEditorElement.MacroNameField)
                }
            }
        }
        DisposableEffect(showCandidateIconSheet) {
            val active = showCandidateIconSheet
            if (active) {
                swipeController.pushScope(EdgeEditorScope.IconSheet)
                swipeController.maxFocusStepsPerEvent = 1
            }
            onDispose {
                if (active) {
                    swipeController.popScope()
                    swipeController.maxFocusStepsPerEvent = Int.MAX_VALUE
                    swipeController.setFocus(EdgeEditorElement.RotationCandidateIconBox)
                }
            }
        }
        DisposableEffect(showColorPicker) {
            val active = showColorPicker
            if (active) {
                swipeController.pushScope(EdgeEditorScope.ColorPicker)
                swipeController.maxFocusStepsPerEvent = 1
            }
            onDispose {
                if (active) {
                    swipeController.popScope()
                    swipeController.maxFocusStepsPerEvent = Int.MAX_VALUE
                }
            }
        }
        val colorPickerHasOpened = remember { mutableStateOf(false) }
        LaunchedEffect(showColorPicker) {
            if (showColorPicker) {
                colorPickerHasOpened.value = true
                colorPickerStage = com.bridgeone.app.ui.components.colorpicker.ColorPickerStage.Category
            } else if (colorPickerHasOpened.value) {
                swipeController.setFocus(EdgeEditorElement.ColorBox)
            }
        }
        DisposableEffect(showCandidateColorPicker) {
            val active = showCandidateColorPicker
            if (active) {
                swipeController.pushScope(EdgeEditorScope.ColorPicker)
                swipeController.maxFocusStepsPerEvent = 1
            }
            onDispose {
                if (active) {
                    swipeController.popScope()
                    swipeController.maxFocusStepsPerEvent = Int.MAX_VALUE
                }
            }
        }
        val candidateColorPickerHasOpened = remember { mutableStateOf(false) }
        LaunchedEffect(showCandidateColorPicker) {
            if (showCandidateColorPicker) {
                candidateColorPickerHasOpened.value = true
                colorPickerStage = com.bridgeone.app.ui.components.colorpicker.ColorPickerStage.Category
            } else if (candidateColorPickerHasOpened.value) {
                swipeController.setFocus(EdgeEditorElement.RotationCandidateColorBox)
            }
        }
        DisposableEffect(swipeShortcutVisible) {
            val active = swipeShortcutVisible
            if (active) {
                swipeController.pushScope(EdgeEditorScope.ShortcutPopup)
                swipeController.maxFocusStepsPerEvent = 1
                swipeController.setFocus(EdgeEditorElement.ShortcutKey(0x0B))
            }
            onDispose {
                if (active) {
                    swipeController.popScope()
                    swipeController.maxFocusStepsPerEvent = Int.MAX_VALUE
                    swipeController.setFocus(EdgeEditorElement.ActionOptionCard("COMBO:추가"))
                }
            }
        }
        DisposableEffect(macroEditorVisible) {
            val active = macroEditorVisible
            if (active) {
                swipeController.pushScope(EdgeEditorScope.MacroPopup)
                swipeController.maxFocusStepsPerEvent = 1
                // 초기 포커스: 스텝 0개면 추가 버튼, 있으면 저장 버튼
                swipeController.setFocus(
                    if (macroEditorDraft.steps.isEmpty()) EdgeEditorElement.MacroAddStep
                    else EdgeEditorElement.MacroPopupConfirm
                )
            }
            onDispose {
                if (active) {
                    swipeController.popScope()
                    swipeController.maxFocusStepsPerEvent = Int.MAX_VALUE
                    swipeController.setFocus(EdgeEditorElement.ActionOptionCard("MACRO:추가"))
                }
            }
        }
        DisposableEffect(showDiscardDialog) {
            val active = showDiscardDialog
            if (active) {
                swipeController.pushScope(EdgeEditorScope.DiscardDialog)
                swipeController.setFocus(EdgeEditorElement.DiscardDialogCancel)
            }
            onDispose {
                if (active) {
                    swipeController.popScope()
                    swipeController.setFocus(EdgeEditorElement.Back)
                }
            }
        }
        DisposableEffect(showUndoMenu) {
            val active = showUndoMenu
            if (active) swipeController.pushScope(EdgeEditorScope.UndoMenu)
            onDispose {
                if (active) {
                    swipeController.popScope()
                    swipeController.setFocus(EdgeEditorElement.Undo)
                }
            }
        }
        val isZonePopupOpen = zonePopup !is ZoneActionPopup.None
        DisposableEffect(isZonePopupOpen) {
            val active = isZonePopupOpen
            if (active) swipeController.pushScope(EdgeEditorScope.ZoneActionPopup)
            onDispose { if (active) swipeController.popScope() }
        }
        DisposableEffect(showLabelKeyboard) {
            val active = showLabelKeyboard
            if (active) swipeController.pushScope(EdgeEditorScope.LabelKeyboard)
            onDispose { if (active) swipeController.popScope() }
        }
        val labelKeyboardHasOpened = remember { mutableStateOf(false) }
        LaunchedEffect(showLabelKeyboard) {
            if (showLabelKeyboard) labelKeyboardHasOpened.value = true
            else if (labelKeyboardHasOpened.value) swipeController.setFocus(EdgeEditorElement.LabelBox)
        }
        DisposableEffect(candidateLabelKeyboard != null) {
            val active = candidateLabelKeyboard != null
            if (active) swipeController.pushScope(EdgeEditorScope.LabelKeyboard)
            onDispose { if (active) swipeController.popScope() }
        }
        val candidateLabelKeyboardHasOpened = remember { mutableStateOf(false) }
        LaunchedEffect(candidateLabelKeyboard != null) {
            val isOpen = candidateLabelKeyboard != null
            if (isOpen) candidateLabelKeyboardHasOpened.value = true
            else if (candidateLabelKeyboardHasOpened.value) swipeController.setFocus(EdgeEditorElement.RotationCandidateLabelBox)
        }
        DisposableEffect(dynamicsEditorVisible) {
            val active = dynamicsEditorVisible
            val editedPresetName = if (active) dynamicsEditorInitial?.name else null
            if (active) swipeController.pushScope(EdgeEditorScope.DynamicsEditor)
            onDispose {
                if (active) {
                    swipeController.popScope()
                    if (editedPresetName != null) {
                        swipeController.setFocus(EdgeEditorElement.ActionOptionCard("DYNAMICS:$editedPresetName"))
                    }
                }
            }
        }
    }

    // 프리셋 팝업: 단계별 뒤로가기 / 닫기 (SWIPE, NORMAL 양쪽)
    BackHandler(enabled = showPresetPopup) {
        when (presetPopupStage) {
            PopupStage.GRID -> showPresetPopup = false
            PopupStage.CONFIRM, PopupStage.SAVE_NAME -> presetPopupStage = PopupStage.GRID
            PopupStage.EDIT_NAME -> presetPopupStage = PopupStage.CONFIRM
        }
    }

    // SWIPE 모드에서 서랍이 열려 있을 때 뒤로가기
    BackHandler(enabled = swipeShortcutVisible) { swipeShortcutVisible = false }
    // 아이콘 단계 → 카테고리 단계, 카테고리 단계 → 서랍 닫힘 (존 선택 유지)
    BackHandler(enabled = (showIconSheet || showCandidateIconSheet || shortcutIconSheetVisible) && inputMode == InputMode.SWIPE) {
        if (iconDrawerStage is IconDrawerStage.Icons) {
            iconDrawerStage = IconDrawerStage.Category
        } else {
            showIconSheet = false
            showCandidateIconSheet = false
            shortcutIconSheetVisible = false
        }
    }
    BackHandler(enabled = (showColorPicker || showCandidateColorPicker) && inputMode == InputMode.SWIPE) {
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
    }

    BackHandler(enabled = candidateLabelKeyboard != null || zonePopup !is ZoneActionPopup.None || selectedZone != null || !canvasVisible) {
        if (candidateLabelKeyboard != null) candidateLabelKeyboard = null
        else if (zonePopup !is ZoneActionPopup.None) zonePopup = ZoneActionPopup.None
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
    val canSave = hasChanges

    // Phase 4.7.5-A: deleteZone/applyRatioPreset → EdgeZoneEditorState로 이관

    // ── 선택 존 업데이트 ──
    fun updateSelectedZone(updated: EdgeZone) {
        val zones = workConfig.zonesFor(updated.edge).toMutableList()
        val idx = zones.indexOfFirst { it.startRatio == updated.startRatio && it.edge == updated.edge }
        if (idx >= 0) {
            zones[idx] = updated
            state.pushUndo()
            workConfig = workConfig.withZones(updated.edge, zones)
            currentPresetId = presetsRepo?.loadAll()?.find { it.config == workConfig }?.id
        }
        selectedZone = updated
    }

    CompositionLocalProvider(
        LocalSwipeFocusController provides if (inputMode == InputMode.SWIPE) swipeController else null
    ) {
    Box(modifier = Modifier.fillMaxSize()) {
    // 서랍 열림 시 본문 배경을 흐리게 (API 31+; 그 미만에서는 무시되어 크래시 없음)
    val editorBlurRadius by animateDpAsState(
        targetValue = if (inputMode == InputMode.SWIPE && (showIconSheet || showCandidateIconSheet || shortcutIconSheetVisible)) CATEGORY_DRAWER_BACKDROP_BLUR_DP.dp else 0.dp,
        animationSpec = tween(durationMillis = CATEGORY_DRAWER_OPEN_DURATION_MS),
        label = "editorBackdropBlur",
    )
    Column(
        modifier = Modifier.fillMaxSize().background(cs.background)
            .blur(editorBlurRadius, edgeTreatment = BlurredEdgeTreatment.Rectangle)
    ) {
        // ── TopAppBar ──
        Surface(
            color = cs.surfaceVariant,
            tonalElevation = 2.dp,
            modifier = Modifier.onGloballyPositioned { coords ->
                undoMenuAnchorBottom = coords.size.height
            }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val topBackAction: () -> Unit = {
                    if (selectedZone != null || !canvasVisible) { selectedZone = null; canvasVisible = true; selectedEdge = null }
                    else if (hasChanges) showDiscardDialog = true
                    else onBack()
                }
                SwipeFocusable(
                    element = EdgeEditorElement.Back,
                    shape = RoundedCornerShape(24.dp),
                    onActivate = topBackAction,
                    gridRow = 0,
                ) {
                    IconButton(onClick = topBackAction) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                }
                Text(
                    text = if (selectedZone != null) "존 설정" else "엣지 존 편집",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = cs.onSurface,
                    modifier = Modifier.weight(1f).padding(start = 4.dp)
                )
                if (presetsRepo != null) {
                    val activePreset = remember(currentPresetId) {
                        if (currentPresetId != null) presetsRepo.findById(currentPresetId) else null
                    }
                    SwipeFocusable(
                        element = EdgeEditorElement.PresetBadge,
                        shape = RoundedCornerShape(16.dp),
                        onActivate = { showPresetPopup = true },
                        gridRow = 0,
                    ) {
                        val focused = LocalSwipeFocused.current
                        if (activePreset != null) {
                            val bg = if (focused) cs.primary else cs.primaryContainer
                            val fg = if (focused) cs.onPrimary else cs.onPrimaryContainer
                            Row(
                                modifier = Modifier
                                    .height(32.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(bg)
                                    .clickable { showPresetPopup = true }
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = IconRegistry.get(activePreset.iconKey),
                                    contentDescription = null,
                                    tint = fg,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = activePreset.name,
                                    fontSize = 12.sp,
                                    color = fg,
                                    maxLines = 1
                                )
                            }
                        } else {
                            val bg = if (focused) cs.primaryContainer else cs.surfaceVariant
                            val fg = if (focused) cs.onPrimaryContainer else cs.onSurfaceVariant
                            val borderColor = if (focused) cs.primary.copy(alpha = 0.6f) else cs.outline.copy(alpha = 0.4f)
                            Row(
                                modifier = Modifier
                                    .height(32.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(bg)
                                    .border(0.5.dp, borderColor, RoundedCornerShape(16.dp))
                                    .clickable { showPresetPopup = true }
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Tune,
                                    contentDescription = null,
                                    tint = fg,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "커스텀",
                                    fontSize = 12.sp,
                                    color = fg,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
                Box {
                    val undoSingleAction: () -> Unit = {
                        val prev = undoStack.firstOrNull()
                        if (prev != null) {
                            if (inputMode == InputMode.SWIPE) nextFocusOnZoneChange = EdgeEditorElement.Undo
                            workConfig = prev
                            undoStack = undoStack.drop(1)
                            currentPresetId = null
                            val sel = selectedZone
                            selectedZone = if (sel != null) {
                                prev.zonesFor(sel.edge).firstOrNull { it.startRatio == sel.startRatio }
                                    ?: prev.zonesFor(sel.edge).firstOrNull()
                            } else null
                        }
                    }
                    val undoHistoryAction: () -> Unit = { if (undoStack.isNotEmpty()) showUndoMenu = true }
                    val undoEnabled = undoStack.isNotEmpty()
                    val undoIconBox: @Composable () -> Unit = {
                        val undoFocused = LocalSwipeFocused.current
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .combinedClickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = false),
                                    enabled = undoEnabled,
                                    onClick = undoSingleAction,
                                    onLongClick = undoHistoryAction
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Undo,
                                contentDescription = "실행 취소",
                                tint = if (undoFocused) cs.primary
                                       else if (undoEnabled) cs.onSurface
                                       else cs.onSurface.copy(alpha = 0.38f)
                            )
                        }
                    }
                    if (undoEnabled) {
                        SwipeFocusable(
                            element = EdgeEditorElement.Undo,
                            shape = RoundedCornerShape(24.dp),
                            onActivate = undoSingleAction,
                            onActivateAlt = undoHistoryAction,
                            gridRow = 0,
                        ) { undoIconBox() }
                    } else {
                        undoIconBox()
                    }
                    // NORMAL 모드: 기본 DropdownMenu
                    DropdownMenu(
                        expanded = showUndoMenu && inputMode == InputMode.NORMAL,
                        onDismissRequest = { showUndoMenu = false },
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        undoStack.forEachIndexed { idx, config ->
                            val newerConfig = if (idx == 0) workConfig else undoStack[idx - 1]
                            val desc = EdgeZoneActionResolver.describeUndoStep(from = config, to = newerConfig)
                            val undoItemAction: () -> Unit = {
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
                            DropdownMenuItem(
                                text = { Text(EdgeZoneActionResolver.describeUndoStep(from = config, to = newerConfig)) },
                                onClick = undoItemAction
                            )
                        }
                    }
                }

                // 저장 버튼 (변경사항 없으면 스와이프 선택 불가)
                val saveAction: () -> Unit = {
                    if (hasInvalidRotation) {
                        ToastController.show(
                            "후보가 ${EdgeSwipeConstants.EDGE_ZONE_ROTATION_MIN_CANDIDATES}개 이상이어야 저장됩니다",
                            ToastType.ERROR
                        )
                    } else {
                        onSave(workConfig, currentPresetId)
                    }
                }
                val saveButtonContent: @Composable () -> Unit = {
                    Button(
                        onClick = saveAction,
                        enabled = canSave,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = cs.primary,
                            contentColor = cs.onPrimary,
                            disabledContainerColor = cs.surfaceVariant,
                            disabledContentColor = cs.onSurfaceVariant
                        ),
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("저장", fontSize = 12.sp)
                    }
                }
                if (canSave) {
                    SwipeFocusable(
                        element = EdgeEditorElement.Save,
                        shape = RoundedCornerShape(8.dp),
                        onActivate = saveAction,
                        gridRow = 0,
                    ) {
                        val focused = LocalSwipeFocused.current
                        Button(
                            onClick = saveAction,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (focused) cs.primary else cs.surfaceVariant,
                                contentColor = if (focused) cs.onPrimary else cs.onSurfaceVariant,
                            ),
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("저장", fontSize = 12.sp)
                        }
                    }
                } else {
                    saveButtonContent()
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
            val labelKbActive = (showLabelKeyboard && selectedZone != null) || candidateLabelKeyboard != null
            val canvasH by animateDpAsState(
                targetValue = when {
                    labelKbActive -> 0.dp
                    canvasVisible -> if (isEditing) totalH * 0.55f else totalH
                    else -> 0.dp
                },
                animationSpec = tween(EdgeSwipeConstants.EDGE_ZONE_LABEL_KEYBOARD_ANIM_MS),
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
                            BoxWithConstraints(
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
                                        state.pushUndo()
                                        workConfig = workConfig.toggleCornerPriority(corner)
                                        currentPresetId = null
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )

                                // ── SWIPE 모드 캔버스 hit 영역 오버레이 (NORMAL에서는 미렌더) ──
                                // 존 단위로 분해. 비활성 엣지만 등록 생략 (Unassigned 존은 포함).
                                // 코너는 별도 hit 영역으로 제공하지 않는다.
                                if (inputMode == InputMode.SWIPE && !isEditing) {
                                    val edgeDp = EdgeSwipeConstants.EDGE_HIT_WIDTH_DP.dp
                                    val canvasWidth = maxWidth
                                    val canvasHeight = maxHeight
                                    val hasBottomLeft = bottomLeftButtonLabel != null
                                    val hasBottomRight = bottomRightButtonLabel != null
                                    val blockedRatio = EdgeSwipeConstants.CORNER_BUTTON_BLOCKED_RATIO
                                    fun cp(c: CornerOverlap) = workConfig.cornerPriority[c] ?: defaultCornerEdge(c)

                                    val selectZoneAction: (EdgeZone) -> () -> Unit = { zone ->
                                        {
                                            selectedZone = zone
                                            selectedEdge = zone.edge
                                            canvasVisible = false
                                        }
                                    }

                                    EntryEdge.entries.forEach { edge ->
                                        if (edge in disabledEdges.keys) return@forEach
                                        val zones = workConfig.zonesFor(edge)
                                        zones.forEachIndexed inner@{ idx, zone ->
                                            // 캔버스 렌더링과 동일한 클리핑 로직을 dp 공간에서 적용
                                            // Unassigned 존도 포함 — 액션 지정을 위해 선택할 수 있어야 함
                                            val rectOffsetX: androidx.compose.ui.unit.Dp
                                            val rectOffsetY: androidx.compose.ui.unit.Dp
                                            val rectWidth: androidx.compose.ui.unit.Dp
                                            val rectHeight: androidx.compose.ui.unit.Dp
                                            when (edge) {
                                                EntryEdge.TOP -> {
                                                    var l = canvasWidth * zone.startRatio
                                                    var r = canvasWidth * zone.endRatio
                                                    if (cp(CornerOverlap.TOP_LEFT) != EntryEdge.TOP) l = maxOf(l, edgeDp)
                                                    if (cp(CornerOverlap.TOP_RIGHT) != EntryEdge.TOP) r = minOf(r, canvasWidth - edgeDp)
                                                    rectOffsetX = l; rectOffsetY = 0.dp
                                                    rectWidth = r - l; rectHeight = edgeDp
                                                }
                                                EntryEdge.BOTTOM -> {
                                                    val minX = when {
                                                        hasBottomLeft -> canvasWidth * blockedRatio
                                                        cp(CornerOverlap.BOTTOM_LEFT) != EntryEdge.BOTTOM -> edgeDp
                                                        else -> 0.dp
                                                    }
                                                    val maxX = when {
                                                        hasBottomRight -> canvasWidth * (1f - blockedRatio)
                                                        cp(CornerOverlap.BOTTOM_RIGHT) != EntryEdge.BOTTOM -> canvasWidth - edgeDp
                                                        else -> canvasWidth
                                                    }
                                                    val l = maxOf(canvasWidth * zone.startRatio, minX)
                                                    val r = minOf(canvasWidth * zone.endRatio, maxX)
                                                    rectOffsetX = l; rectOffsetY = canvasHeight - edgeDp
                                                    rectWidth = r - l; rectHeight = edgeDp
                                                }
                                                EntryEdge.LEFT -> {
                                                    var t = canvasHeight * zone.startRatio
                                                    var b = canvasHeight * zone.endRatio
                                                    if (cp(CornerOverlap.TOP_LEFT) != EntryEdge.LEFT) t = maxOf(t, edgeDp)
                                                    if (hasBottomLeft) b = minOf(b, canvasHeight * (1f - blockedRatio))
                                                    else if (cp(CornerOverlap.BOTTOM_LEFT) != EntryEdge.LEFT) b = minOf(b, canvasHeight - edgeDp)
                                                    rectOffsetX = 0.dp; rectOffsetY = t
                                                    rectWidth = edgeDp; rectHeight = b - t
                                                }
                                                EntryEdge.RIGHT -> {
                                                    var t = canvasHeight * zone.startRatio
                                                    var b = canvasHeight * zone.endRatio
                                                    if (cp(CornerOverlap.TOP_RIGHT) != EntryEdge.RIGHT) t = maxOf(t, edgeDp)
                                                    if (hasBottomRight) b = minOf(b, canvasHeight * (1f - blockedRatio))
                                                    else if (cp(CornerOverlap.BOTTOM_RIGHT) != EntryEdge.RIGHT) b = minOf(b, canvasHeight - edgeDp)
                                                    rectOffsetX = canvasWidth - edgeDp; rectOffsetY = t
                                                    rectWidth = edgeDp; rectHeight = b - t
                                                }
                                            }
                                            if (rectWidth <= 0.dp || rectHeight <= 0.dp) return@inner

                                            val gridRow = when (edge) {
                                                EntryEdge.TOP -> 10
                                                EntryEdge.LEFT, EntryEdge.RIGHT -> 11 + idx
                                                EntryEdge.BOTTOM -> 50
                                            }

                                            SwipeFocusable(
                                                element = EdgeEditorElement.CanvasZone(edge, idx),
                                                shape = RoundedCornerShape(4.dp),
                                                showBorderHighlight = true,
                                                onActivate = selectZoneAction(zone),
                                                gridRow = gridRow,
                                                modifier = Modifier
                                                    .offset(x = rectOffsetX, y = rectOffsetY)
                                                    .size(width = rectWidth, height = rectHeight),
                                            ) {}
                                        }
                                    }
                                }
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
                            // 축소 애니메이션 중: 단순 배경만 표시
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
                    val labelKbActive = (showLabelKeyboard && sel != null) || candidateLabelKeyboard != null
                    val labelKbAnimMs = EdgeSwipeConstants.EDGE_ZONE_LABEL_KEYBOARD_ANIM_MS
                    val kbProgress by animateFloatAsState(
                        targetValue = if (labelKbActive) 1f else 0f,
                        animationSpec = tween(labelKbAnimMs, easing = FastOutSlowInEasing),
                        label = "labelKbProgress"
                    )

                    Column(modifier = Modifier.fillMaxSize()) {
                    // 기존 폼: sel/edge 있으면 항상 렌더 (라벨 키보드 활성 중에도 유지)
                    if (sel != null || edgeForStrip != null) {
                    val formScroll = rememberScrollState()
                    val density = LocalDensity.current
                    val kbPushPx = with(density) {
                        EdgeSwipeConstants.EDGE_ZONE_LABEL_KEYBOARD_VISUAL_HEIGHT_DP.dp.toPx()
                    }
                    LaunchedEffect(kbProgress, formScroll.maxValue) {
                        val target = (kbProgress * kbPushPx).toInt().coerceAtMost(formScroll.maxValue)
                        formScroll.scrollTo(target)
                    }
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.TopStart
                    ) {
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
                        val zoneIdx = if (sel != null) zoneList.indexOfFirst { it.startRatio == sel.startRatio && it.edge == sel.edge } else -1

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(formScroll)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // ── 1. 영역 비율 ──
                            val p = zonePopup
                            var presetMenuOpen by remember { mutableStateOf(false) }
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
                                        val firstLabel = EdgeZoneActionResolver.ratioPresetsFor(zoneList.size).firstOrNull()?.first
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
                                    modifier = Modifier.weight(1f)
                                )
                                if (zoneList.size >= 2) {
                                    Box {
                                        SwipeFocusable(
                                            element = EdgeEditorElement.RatioPresetMenu,
                                            shape = RoundedCornerShape(16.dp),
                                            onActivate = { presetMenuOpen = true },
                                            gridRow = 20,
                                            modifier = Modifier.onGloballyPositioned { coords ->
                                                ratioBtnBoundsInWindow = coords.boundsInWindow()
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
                                        // NORMAL 모드: 일반 DropdownMenu (focusable = true, 외부 탭으로 닫힘)
                                        DropdownMenu(
                                            expanded = presetMenuOpen && inputMode == InputMode.NORMAL,
                                            onDismissRequest = { presetMenuOpen = false }
                                        ) {
                                            EdgeZoneActionResolver.ratioPresetsFor(zoneList.size).forEach { (label, ratios) ->
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
                                                        state.applyRatioPreset(edgeForStrip, ratios)
                                                        presetMenuOpen = false
                                                    }
                                                )
                                            }
                                        }
                                        // SWIPE 모드: focusable=false Popup → 터치가 팝업을 통과하여
                                        // 아래 SwipeGestureLayer까지 전달되므로 화면 어디서든 스와이프 가능
                                        if (inputMode == InputMode.SWIPE && presetMenuOpen) {
                                            RatioPresetSwipePopup(
                                                presets = EdgeZoneActionResolver.ratioPresetsFor(zoneList.size),
                                                onSelect = { ratios ->
                                                    state.applyRatioPreset(edgeForStrip, ratios)
                                                    presetMenuOpen = false
                                                },
                                            )
                                        }
                                    }
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
                                    minRatio = minRatio,
                                    onZonesChanged = { newZones ->
                                        state.pushUndo()
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
                                            if (state.tryMergeWith(cur.zone, tapped)) zonePopup = ZoneActionPopup.None
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

                            // ── 팝업 ──
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
                                                val mergeAction: () -> Unit = { zonePopup = ZoneActionPopup.MergeSelecting(sel) }
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
                                                        showBorderHighlight = true,
                                                        onActivate = mergeAction,
                                                        gridRow = 0,
                                                    ) {
                                                    TextButton(
                                                        onClick = mergeAction,
                                                        enabled = hasAdj,
                                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                                    ) { Text("병합", fontSize = 12.sp) }
                                                    }
                                                    divider()
                                                    SwipeFocusable(
                                                        element = EdgeEditorElement.ZoneActionSplit,
                                                        scope = EdgeEditorScope.ZoneActionPopup,
                                                        shape = RoundedCornerShape(4.dp),
                                                        showBorderHighlight = true,
                                                        onActivate = splitAction,
                                                        gridRow = 0,
                                                    ) {
                                                    TextButton(
                                                        onClick = splitAction,
                                                        enabled = anySplitValid,
                                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                                    ) { Text("분할", fontSize = 12.sp) }
                                                    }
                                                    divider()
                                                    SwipeFocusable(
                                                        element = EdgeEditorElement.ZoneActionDelete,
                                                        scope = EdgeEditorScope.ZoneActionPopup,
                                                        shape = RoundedCornerShape(4.dp),
                                                        showBorderHighlight = true,
                                                        onActivate = deleteAction,
                                                        gridRow = 0,
                                                    ) {
                                                    TextButton(
                                                        onClick = deleteAction,
                                                        enabled = canDel,
                                                        colors = ButtonDefaults.textButtonColors(contentColor = cs.error),
                                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                                    ) { Text("삭제", fontSize = 12.sp) }
                                                    }
                                                }
                                            }
                                            is ZoneActionPopup.MergeSelecting -> {
                                                val mergeCancelAction: () -> Unit = { zonePopup = ZoneActionPopup.None }
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Text("병합할 존을 선택하세요", fontSize = 12.sp, color = cs.onSurfaceVariant)
                                                    SwipeFocusable(
                                                        element = EdgeEditorElement.ZoneActionMergeCancel,
                                                        scope = EdgeEditorScope.ZoneActionPopup,
                                                        shape = RoundedCornerShape(4.dp),
                                                        showBorderHighlight = true,
                                                        onActivate = mergeCancelAction,
                                                        gridRow = 0,
                                                    ) {
                                                    TextButton(
                                                        onClick = mergeCancelAction,
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                                    ) { Text("취소", fontSize = 12.sp) }
                                                    }
                                                }
                                            }
                                            is ZoneActionPopup.SplitChoosing -> {
                                                val splitCancelAction: () -> Unit = { zonePopup = ZoneActionPopup.None }
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
                                                        val splitNAction: () -> Unit = { if (state.splitInto(sel, n)) zonePopup = ZoneActionPopup.None }
                                                        SwipeFocusable(
                                                            element = EdgeEditorElement.ZoneActionSplitN(n),
                                                            scope = EdgeEditorScope.ZoneActionPopup,
                                                            shape = RoundedCornerShape(4.dp),
                                                            showBorderHighlight = true,
                                                            onActivate = splitNAction,
                                                            gridRow = 0,
                                                        ) {
                                                        TextButton(
                                                            onClick = splitNAction,
                                                            enabled = valid,
                                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                                        ) { Text("$n", fontSize = 13.sp) }
                                                        }
                                                    }
                                                    SwipeFocusable(
                                                        element = EdgeEditorElement.ZoneActionSplitCancel,
                                                        scope = EdgeEditorScope.ZoneActionPopup,
                                                        shape = RoundedCornerShape(4.dp),
                                                        showBorderHighlight = true,
                                                        onActivate = splitCancelAction,
                                                        gridRow = 0,
                                                    ) {
                                                    TextButton(
                                                        onClick = splitCancelAction,
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                                    ) { Text("취소", fontSize = 12.sp) }
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

                            if (sel != null) {
                                HorizontalDivider(color = cs.outline.copy(alpha = 0.2f))

                                // ── 2. 액션 타입 페이지 네비게이션 ──
                                val isSingleAction = sel.trigger is EdgeZoneTrigger.SingleAction
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        if (isSingleAction) "단일 액션" else "액션 순환",
                                        fontSize = 12.sp,
                                        color = cs.onSurfaceVariant,
                                        modifier = Modifier.weight(1f)
                                    )
                                    val toggleAction: () -> Unit = {
                                        if (inputMode == InputMode.SWIPE) {
                                            nextFocusOnZoneChange = EdgeEditorElement.ActionTypeToggle
                                        }
                                        if (isSingleAction) {
                                            val defaultInterval = EdgeSwipeConstants.EDGE_ZONE_ROTATION_INTERVAL_DEFAULT_MS
                                            val restored = savedRotationTrigger ?: EdgeZoneTrigger.Rotation(emptyList(), defaultInterval)
                                            savedRotationTrigger = null
                                            updateSelectedZone(sel.copy(trigger = restored))
                                        } else {
                                            savedRotationTrigger = sel.trigger as? EdgeZoneTrigger.Rotation
                                            updateSelectedZone(sel.copy(trigger = EdgeZoneTrigger.SingleAction(EdgeZoneAction.Unassigned, "", "")))
                                        }
                                    }
                                    SwipeFocusable(
                                        element = EdgeEditorElement.ActionTypeToggle,
                                        shape = RoundedCornerShape(16.dp),
                                        onActivate = toggleAction,
                                        gridRow = 30,
                                        modifier = Modifier.onGloballyPositioned { coords ->
                                            actionTypeBtnBoundsInWindow = coords.boundsInWindow()
                                        },
                                    ) {
                                    val toggleFocused = LocalSwipeFocused.current
                                    IconButton(
                                        onClick = toggleAction,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            if (isSingleAction) Icons.Filled.Loop else Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = if (isSingleAction) "액션 순환으로 전환" else "단일 액션으로 전환",
                                            tint = if (toggleFocused) cs.primary else cs.onSurface.copy(alpha = 0.75f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    }
                                }

                                if (isSingleAction) {
                                    // ── 3. 액션 선택 ──
                                    ActionDomainPicker(
                                        current = sel.action,
                                        onSelect = { action ->
                                            if (inputMode == InputMode.SWIPE) {
                                                nextFocusOnZoneChange =
                                                    if (action is EdgeZoneAction.SetCustomDpi || action is EdgeZoneAction.SetCustomScrollSpeed)
                                                        EdgeEditorElement.CustomMultiplierSlider
                                                    else
                                                        EdgeEditorElement.IconBox
                                            }
                                            val t = sel.trigger
                                            val updated = if (t is EdgeZoneTrigger.SingleAction)
                                                sel.copy(trigger = t.copy(action = action, label = "", iconKey = ""))
                                            else
                                                sel.withAction(action)
                                            updateSelectedZone(updated)
                                        },
                                        customPresets = localCustomPresets,
                                        onAddDynamicsPreset = if (customPresetsRepo != null) {
                                            { dynamicsEditorInitial = null; dynamicsEditorVisible = true }
                                        } else null,
                                        onEditCustomDynamics = if (customPresetsRepo != null) {
                                            { preset -> dynamicsEditorInitial = preset; dynamicsEditorVisible = true }
                                        } else null,
                                        onDeleteCustomDynamics = if (customPresetsRepo != null) {
                                            { preset ->
                                                val removedIdx = localCustomPresets.indexOfFirst { it.id == preset.id }
                                                if (removedIdx >= 0) {
                                                    val removedGlobalIdx = DYNAMICS_PRESETS.size + removedIdx
                                                    val prevSel = selectedZone
                                                    customPresetsRepo.delete(preset.id)
                                                    val updated = customPresetsRepo.loadAll()
                                                    localCustomPresets = updated
                                                    onCustomPresetsChange(updated)
                                                    val migratedConfig = EdgeZoneActionResolver.migrateDynamicsIndicesAfterDelete(workConfig, removedGlobalIdx)
                                                    workConfig = migratedConfig
                                                    // selectedZone을 새 workConfig 기준으로 재동기화
                                                    if (prevSel != null) {
                                                        selectedZone = migratedConfig.zonesFor(prevSel.edge)
                                                            .firstOrNull { it.startRatio == prevSel.startRatio }
                                                    }
                                                    ToastController.show("프리셋이 삭제되었습니다", ToastType.SUCCESS)
                                                }
                                            }
                                        } else null,
                                        onEditCustomShortcutConfirm = if (customShortcutPresetsRepo != null) {
                                            { oldPreset, confirmed ->
                                                val updatedPreset = oldPreset.copy(
                                                    modifierBits = confirmed.modifierBits,
                                                    keyCodes = confirmed.keyCodes,
                                                    hold = confirmed.hold,
                                                )
                                                customShortcutPresetsRepo.update(updatedPreset)
                                                localCustomShortcutPresets = customShortcutPresetsRepo.loadAll()
                                                ToastController.show("단축키 프리셋이 수정되었습니다", ToastType.SUCCESS)
                                            }
                                        } else null,
                                        onDeleteCustomShortcut = if (customShortcutPresetsRepo != null) {
                                            { preset ->
                                                customShortcutPresetsRepo.delete(preset.id)
                                                localCustomShortcutPresets = customShortcutPresetsRepo.loadAll()
                                                ToastController.show("단축키 프리셋이 삭제되었습니다", ToastType.SUCCESS)
                                            }
                                        } else null,
                                        customShortcutPresets = localCustomShortcutPresets,
                                        pageCount = pageCount,
                                        inputMode = inputMode,
                                        onAddAsCandidate = if (customShortcutPresetsRepo != null) {
                                            { shortcutAction, iconKey, name ->
                                                if (shortcutAction is EdgeZoneAction.SendShortcut) {
                                                    val combo = formatShortcutCombo(shortcutAction.modifierBits, shortcutAction.keyCodes)
                                                    val preset = com.bridgeone.app.ui.common.CustomShortcutPreset(
                                                        modifierBits = shortcutAction.modifierBits,
                                                        keyCodes = shortcutAction.keyCodes,
                                                        hold = shortcutAction.hold,
                                                        iconKey = iconKey,
                                                        name = name,
                                                    )
                                                    customShortcutPresetsRepo.add(preset)
                                                    localCustomShortcutPresets = customShortcutPresetsRepo.loadAll()
                                                    // 현재 존도 프리셋 버전으로 업데이트 (presetLabel + iconKey 설정)
                                                    val t = sel.trigger
                                                    if (t is EdgeZoneTrigger.SingleAction) {
                                                        val newAction = shortcutAction.copy(presetLabel = name.ifEmpty { combo })
                                                        val newTrigger = t.copy(action = newAction, iconKey = iconKey)
                                                        if (inputMode == InputMode.SWIPE) nextFocusOnZoneChange = EdgeEditorElement.IconBox
                                                        updateSelectedZone(sel.copy(trigger = newTrigger))
                                                    }
                                                    ToastController.show("단축키 프리셋으로 저장됨", ToastType.SUCCESS)
                                                }
                                            }
                                        } else null,
                                        customMacroPresets = localCustomMacroPresets,
                                        onEditCustomMacroConfirm = if (customMacroPresetsRepo != null) {
                                            { oldPreset, confirmed, iconKey, name ->
                                                val updatedPreset = oldPreset.copy(
                                                    steps = confirmed.steps,
                                                    stepDelayMs = confirmed.stepDelayMs,
                                                    iconKey = iconKey,
                                                    inputModeCheck = confirmed.inputModeCheck,
                                                    displayName = name,
                                                )
                                                customMacroPresetsRepo.update(updatedPreset)
                                                localCustomMacroPresets = customMacroPresetsRepo.loadAll()
                                                ToastController.show("매크로 프리셋이 수정되었습니다", ToastType.SUCCESS)
                                            }
                                        } else null,
                                        onDeleteCustomMacro = if (customMacroPresetsRepo != null) {
                                            { preset ->
                                                customMacroPresetsRepo.delete(preset.id)
                                                localCustomMacroPresets = customMacroPresetsRepo.loadAll()
                                                ToastController.show("매크로 프리셋이 삭제되었습니다", ToastType.SUCCESS)
                                            }
                                        } else null,
                                        onSwipeShortcutRequest = if (inputMode == InputMode.SWIPE) { draft, onConfirm, onAddAsCandidate ->
                                            swipeShortcutDraft = draft
                                            swipeShortcutOnConfirm = onConfirm
                                            swipeShortcutOnAddAsCandidate = onAddAsCandidate
                                            swipeShortcutVisible = true
                                        } else null,
                                        onSwipeMacroRequest = { draft, initIcon, initName, onConfirm ->
                                            macroEditorDraft = draft
                                            macroEditorInitialIconKey = initIcon
                                            macroEditorInitialName = initName
                                            macroEditorOnConfirm = onConfirm
                                            macroEditorOnAddAsPreset = if (customMacroPresetsRepo != null) {
                                                { macroDraft, iconKey, name ->
                                                    val preset = com.bridgeone.app.ui.common.CustomMacroPreset(
                                                        steps = macroDraft.steps,
                                                        stepDelayMs = macroDraft.stepDelayMs,
                                                        iconKey = iconKey,
                                                        inputModeCheck = macroDraft.inputModeCheck,
                                                        displayName = name,
                                                        groupNames = macroDraft.groupNames,
                                                    )
                                                    customMacroPresetsRepo.add(preset)
                                                    localCustomMacroPresets = customMacroPresetsRepo.loadAll()
                                                    ToastController.show("매크로 프리셋으로 저장됨", ToastType.SUCCESS)
                                                }
                                            } else null
                                            macroEditorVisible = true
                                        },
                                        swipeMenuTarget = swipeCustomMenuTarget,
                                        onSwipeMenuDismiss = { swipeCustomMenuTarget = null },
                                    )

                                    HorizontalDivider(color = cs.outline.copy(alpha = 0.2f))

                                    // ── 4. 표시 설정 (라벨 + 아이콘) ──
                                    Text("표시 설정", fontSize = 12.sp, color = cs.onSurfaceVariant)
                                    val trigger = sel.trigger as EdgeZoneTrigger.SingleAction
                                    val isAutoLabel = trigger.label.isEmpty()
                                    val isAutoIcon = trigger.iconKey.isEmpty()
                                    val displayLabel = trigger.label.ifEmpty { sel.action.defaultLabel() }
                                    val displayIconKey = trigger.iconKey.ifEmpty { sel.action.defaultIconKey() }
                                    val displayColorHex = trigger.colorHex
                                    val hasUserColor = displayColorHex.isNotEmpty()
                                    val hasUserCustom = !isAutoLabel || !isAutoIcon || hasUserColor
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // 아이콘 박스
                                        SwipeFocusable(
                                            element = EdgeEditorElement.IconBox,
                                            shape = RoundedCornerShape(8.dp),
                                            showBorderHighlight = true,
                                            onActivate = { iconDrawerStage = IconDrawerStage.Category; showIconSheet = true },
                                            gridRow = 36,
                                            modifier = Modifier.onGloballyPositioned { coords ->
                                                val b = coords.boundsInWindow()
                                                iconBoxCenterInWindow = Offset(
                                                    (b.left + b.right) / 2f,
                                                    (b.top + b.bottom) / 2f,
                                                )
                                            },
                                        ) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(cs.surfaceVariant)
                                                .border(0.5.dp, cs.outline.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                                .clickable { iconDrawerStage = IconDrawerStage.Category; showIconSheet = true },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (displayIconKey.isNotEmpty()) {
                                                Icon(
                                                    imageVector = IconRegistry.get(displayIconKey),
                                                    contentDescription = null,
                                                    tint = if (isAutoIcon) cs.onSurface.copy(alpha = 0.6f) else cs.onSurface,
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
                                            // 자동 배지
                                            if (isAutoIcon && displayIconKey.isNotEmpty()) {
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
                                        } // SwipeFocusable(IconBox) 닫기

                                        // 컬러 박스
                                        SwipeFocusable(
                                            element = EdgeEditorElement.ColorBox,
                                            shape = RoundedCornerShape(8.dp),
                                            showBorderHighlight = true,
                                            onActivate = { showColorPicker = true },
                                            gridRow = 36,
                                            modifier = Modifier.onGloballyPositioned { coords ->
                                                val b = coords.boundsInWindow()
                                                colorBoxCenterInWindow = Offset(
                                                    (b.left + b.right) / 2f,
                                                    (b.top + b.bottom) / 2f,
                                                )
                                            },
                                        ) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(cs.surfaceVariant)
                                                .border(0.5.dp, cs.outline.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                                .clickable { showColorPicker = true },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (hasUserColor) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(
                                                            com.bridgeone.app.ui.common.ColorCodec.hexToColorOrNull(displayColorHex)
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
                                        } // SwipeFocusable(ColorBox) 닫기

                                        // 라벨 박스
                                        SwipeFocusable(
                                            element = EdgeEditorElement.LabelBox,
                                            shape = RoundedCornerShape(8.dp),
                                            showBorderHighlight = true,
                                            onActivate = { showLabelKeyboard = true },
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
                                                .clickable { showLabelKeyboard = true }
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
                                                        text = displayLabel.ifEmpty { "라벨 입력..." },
                                                        fontSize = 14.sp,
                                                        color = if (displayLabel.isEmpty()) cs.onSurfaceVariant.copy(alpha = 0.5f)
                                                                else if (isAutoLabel) cs.onSurface.copy(alpha = 0.7f)
                                                                else cs.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f, fill = false)
                                                    )
                                                    // 편집 중 커서
                                                    if (showLabelKeyboard) {
                                                        Box(
                                                            modifier = Modifier
                                                                .width(1.5.dp)
                                                                .height(16.dp)
                                                                .background(cs.primary.copy(alpha = labelCursorAlpha))
                                                        )
                                                    }
                                                }
                                                // 자동 배지 — 항상 오른쪽 고정
                                                if (isAutoLabel && displayLabel.isNotEmpty()) {
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
                                        } // SwipeFocusable(LabelBox) 닫기

                                        // 자동으로 되돌리기 (사용자 지정 상태일 때만)
                                        if (hasUserCustom) {
                                            val revertAction: () -> Unit = {
                                                updateSelectedZone(sel.copy(trigger = trigger.copy(label = "", iconKey = "", colorHex = "")))
                                            }
                                            SwipeFocusable(
                                                element = EdgeEditorElement.RevertToAuto,
                                                shape = RoundedCornerShape(20.dp),
                                                onActivate = revertAction,
                                                gridRow = 36,
                                                modifier = Modifier.onGloballyPositioned { coords ->
                                                    revertBtnBoundsInWindow = coords.boundsInWindow()
                                                },
                                            ) {
                                                val revertFocused = LocalSwipeFocused.current
                                                IconButton(
                                                    onClick = revertAction,
                                                    modifier = Modifier.size(40.dp)
                                                ) {
                                                    Icon(
                                                        Icons.AutoMirrored.Filled.Undo,
                                                        contentDescription = "자동으로 되돌리기",
                                                        tint = if (revertFocused) cs.primary else cs.onSurface,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // ── 3. 액션 순환 후보 편집 ──
                                    val rotation = sel.trigger as EdgeZoneTrigger.Rotation
                                    RotationEditor(
                                        rotation = rotation,
                                        onRotationChanged = { updateSelectedZone(sel.copy(trigger = it)) },
                                        onRequestLabelKeyboard = { current, onResult ->
                                            candidateLabelCurrent = current
                                            candidateLabelKeyboard = onResult
                                        },
                                        inputMode = inputMode,
                                        editingEntry = rotationEditingEntry,
                                        onEditingEntryChange = { entry ->
                                            rotationEditingEntry = entry
                                            if (entry != null && inputMode == InputMode.SWIPE) {
                                                swipeController.setFocus(EdgeEditorElement.DomainChip(ActionDomain.CLICK.name))
                                            }
                                        },
                                        draftCandidate = rotationDraft,
                                        onDraftChange = { rotationDraft = it },
                                        onCandidateActionSelected = { action ->
                                            rotationDraft = rotationDraft.copy(action = action, label = "", iconKey = "")
                                            if (inputMode == InputMode.SWIPE) {
                                                swipeController.setFocus(
                                                    if (action is EdgeZoneAction.SetCustomDpi || action is EdgeZoneAction.SetCustomScrollSpeed)
                                                        EdgeEditorElement.CustomMultiplierSlider
                                                    else
                                                        EdgeEditorElement.RotationCandidateIconBox
                                                )
                                            }
                                        },
                                        onRequestCandidateIconPicker = {
                                            iconDrawerStage = IconDrawerStage.Category
                                            showCandidateIconSheet = true
                                        },
                                        onRequestCandidateColorPicker = { showCandidateColorPicker = true },
                                        iconBoxAnchorReport = { candidateIconBoxCenterInWindow = it },
                                        colorBoxAnchorReport = { candidateColorBoxCenterInWindow = it },
                                        isEditingLabel = candidateLabelKeyboard != null,
                                        labelCursorAlpha = labelCursorAlpha,
                                        onApply = if (inputMode == InputMode.SWIPE) {
                                            { nextFocusOnZoneChange = EdgeEditorElement.RotationAddCandidate }
                                        } else null,
                                        onBeforeIntervalChange = if (inputMode == InputMode.SWIPE) {
                                            { nextFocusOnZoneChange = swipeController.currentFocus as? EdgeEditorElement }
                                        } else null,
                                        pageCount = pageCount,
                                        onSwipeShortcutRequest = if (inputMode == InputMode.SWIPE) { draft, onConfirm, _ ->
                                            swipeShortcutDraft = draft
                                            swipeShortcutOnConfirm = onConfirm
                                            swipeShortcutOnAddAsCandidate = null
                                            swipeShortcutVisible = true
                                        } else null,
                                    )
                                }
                            }
                            if (labelKbActive || kbProgress > 0.001f) {
                                Spacer(Modifier.height(
                                    (EdgeSwipeConstants.EDGE_ZONE_LABEL_KEYBOARD_VISUAL_HEIGHT_DP * kbProgress).dp
                                ))
                            }
                        }
                    } // Box(weight 1f) for form
                    } // if (sel/edge)
                    } // Column(fillMaxSize)
                    // ── 라벨 키보드: edit panel Box 전체 오버레이 (스와이프 제스처 전체 영역 커버) ──
                    if (labelKbActive || kbProgress > 0.001f) {
                        val isCandidateLabel = candidateLabelKeyboard != null
                        val seed = if (isCandidateLabel) candidateLabelCurrent
                                   else (selectedZone?.trigger as? EdgeZoneTrigger.SingleAction)?.label ?: ""
                        val density = LocalDensity.current
                        val kbHeightPx = with(density) {
                            EdgeSwipeConstants.EDGE_ZONE_LABEL_KEYBOARD_VISUAL_HEIGHT_DP.dp.toPx()
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    translationY = kbHeightPx * (1f - kbProgress)
                                }
                        ) {
                        SwipeKeyboardOverlay(
                            initialText = seed,
                            maxLength = EdgeSwipeConstants.EDGE_ZONE_LABEL_MAX_LEN,
                            suggestions = EdgeSwipeConstants.EDGE_ZONE_LABEL_SUGGESTIONS,
                            revertOnCancel = false,
                            showScrim = false,
                            gestureFullHeight = true,
                            onTextChange = { text ->
                                if (isCandidateLabel) {
                                    candidateLabelCurrent = text
                                    candidateLabelKeyboard?.invoke(text)
                                } else if (selectedZone != null) updateSelectedZone(selectedZone!!.withLabel(text))
                            },
                            onCancel = {
                                if (isCandidateLabel) candidateLabelKeyboard = null
                                else showLabelKeyboard = false
                            },
                            onDone = { result ->
                                if (isCandidateLabel) {
                                    candidateLabelKeyboard?.invoke(result)
                                    candidateLabelKeyboard = null
                                } else {
                                    if (selectedZone != null) updateSelectedZone(selectedZone!!.withLabel(result))
                                    showLabelKeyboard = false
                                }
                            }
                        )
                        } // Box (graphicsLayer 슬라이드)
                    }
                }
            } // Column (BoxWithConstraints 내부)
        } // BoxWithConstraints

    }

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
        // ── 커스텀 다이나믹스 프리셋 편집기 오버레이 ──
        if (dynamicsEditorVisible) {
            val currentSel = selectedZone
            val editingPreset = dynamicsEditorInitial
            DynamicsCurveEditor(
                initialPreset = editingPreset,
                existingPresets = localCustomPresets,
                onSave = { preset ->
                    // 수정(id 기존)이면 update, 신규면 add
                    val isUpdate = editingPreset != null && localCustomPresets.any { it.id == preset.id }
                    if (isUpdate) {
                        customPresetsRepo?.update(preset)
                    } else {
                        customPresetsRepo?.add(preset)
                    }
                    val updated = customPresetsRepo?.loadAll() ?: localCustomPresets
                    localCustomPresets = updated
                    onCustomPresetsChange(updated)
                    // 신규 추가 시에만 현재 존에 자동 적용 (수정 시에는 인덱스 불변)
                    if (!isUpdate && currentSel != null) {
                        val newIndex = DYNAMICS_PRESETS.size + updated.indexOfFirst { it.id == preset.id }
                        if (newIndex >= DYNAMICS_PRESETS.size) {
                            val action = EdgeZoneAction.SetDynamicsPreset(newIndex)
                            val t = currentSel.trigger
                            val updatedZone = if (t is EdgeZoneTrigger.SingleAction)
                                currentSel.copy(trigger = t.copy(action = action, label = "", iconKey = ""))
                            else
                                currentSel.withAction(action)
                            if (inputMode == InputMode.SWIPE) nextFocusOnZoneChange = EdgeEditorElement.IconBox
                            updateSelectedZone(updatedZone)
                        }
                    }
                    dynamicsEditorVisible = false
                    dynamicsEditorInitial = null
                },
                onDismiss = { dynamicsEditorVisible = false; dynamicsEditorInitial = null },
                modifier = Modifier.fillMaxSize()
            )
        }
    } // Box 닫기

    // ── 아이콘 선택 바텀시트 (NORMAL 모드 전용; SWIPE 모드는 Box 안의 CategoryIconDrawer 사용) ──
    if (showIconSheet && inputMode == InputMode.NORMAL) {
        val displayIconKeyForSheet = run {
            val trigger = selectedZone?.trigger as? EdgeZoneTrigger.SingleAction
            trigger?.iconKey?.ifEmpty { selectedZone?.action?.defaultIconKey() ?: "" } ?: ""
        }
        NormalCategoryIconSheet(
            selectedIconKey = displayIconKeyForSheet,
            sheetState = iconSheetState,
            onPick = { key ->
                selectedZone?.let { updateSelectedZone(it.withIconKey(key)) }
                showIconSheet = false
            },
            onDismiss = { showIconSheet = false },
        )
    }

    // ── 액션 순환 후보 아이콘 선택 바텀시트 (NORMAL 모드 전용) ──
    if (showCandidateIconSheet && inputMode == InputMode.NORMAL) {
        val candidateIconKey = rotationDraft.iconKey.ifEmpty { rotationDraft.action.defaultIconKey() }
        NormalCategoryIconSheet(
            selectedIconKey = candidateIconKey,
            sheetState = iconSheetState,
            onPick = { key ->
                rotationDraft = rotationDraft.copy(iconKey = key)
                showCandidateIconSheet = false
            },
            onDismiss = { showCandidateIconSheet = false },
        )
    }

    // ── 컬러 피커 바텀시트 (NORMAL 모드 전용; SWIPE 모드는 Box 안의 ColorPickerSwipe 사용) ──
    if (showColorPicker && inputMode == InputMode.NORMAL) {
        val colorSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val currentColorHex = run {
            val trigger = selectedZone?.trigger as? EdgeZoneTrigger.SingleAction
            trigger?.colorHex ?: ""
        }
        NormalCategoryColorSheet(
            selectedColorHex = currentColorHex,
            sheetState = colorSheetState,
            onPick = { hex ->
                selectedZone?.let { updateSelectedZone(it.withColor(hex)) }
                showColorPicker = false
            },
            onDismiss = { showColorPicker = false },
        )
    }

    // ── 액션 순환 후보 컬러 피커 바텀시트 (NORMAL 모드 전용) ──
    if (showCandidateColorPicker && inputMode == InputMode.NORMAL) {
        val candidateColorSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        NormalCategoryColorSheet(
            selectedColorHex = rotationDraft.colorHex,
            sheetState = candidateColorSheetState,
            onPick = { hex ->
                rotationDraft = rotationDraft.copy(colorHex = hex)
                showCandidateColorPicker = false
            },
            onDismiss = { showCandidateColorPicker = false },
        )
    }

    // ── 미저장 변경 경고 다이얼로그 ──
    if (showDiscardDialog) {
        val discardSaveAction: () -> Unit = {
            if (hasInvalidRotation) {
                ToastController.show(
                    "후보가 ${EdgeSwipeConstants.EDGE_ZONE_ROTATION_MIN_CANDIDATES}개 이상이어야 저장됩니다",
                    ToastType.ERROR
                )
            } else {
                onSave(workConfig, currentPresetId)
            }
            showDiscardDialog = false
        }
        val discardDiscardAction: () -> Unit = { showDiscardDialog = false; onBack() }
        val discardCancelAction: () -> Unit = { showDiscardDialog = false }

        if (inputMode == InputMode.SWIPE) {
            // SWIPE 모드: non-focusable Popup — 스와이프 이벤트가 SwipeGestureLayer까지 통과함
            Popup(
                alignment = Alignment.Center,
                properties = PopupProperties(focusable = false),
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = cs.surfaceVariant,
                    tonalElevation = 6.dp,
                    shadowElevation = 12.dp,
                    modifier = Modifier
                        .widthIn(min = 240.dp)
                        .border(0.5.dp, cs.outline.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "변경사항이 있습니다",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = cs.onSurface,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                        Text(
                            "저장하지 않고 나가면 변경사항이 사라집니다.",
                            fontSize = 13.sp,
                            color = cs.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                        SwipeFocusable(
                            element = EdgeEditorElement.DiscardDialogCancel,
                            scope = EdgeEditorScope.DiscardDialog,
                            shape = RoundedCornerShape(8.dp),
                            showBorderHighlight = false,
                            onActivate = discardCancelAction,
                            gridRow = 0,
                            modifier = Modifier.weight(1f),
                        ) {
                            val cancelFocused = LocalSwipeFocused.current
                            FilledTonalButton(
                                onClick = discardCancelAction,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    contentColor = if (cancelFocused) cs.onPrimary else cs.onSurface,
                                    containerColor = if (cancelFocused) cs.primary else Color.Transparent,
                                )
                            ) { Text("취소", fontSize = 14.sp) }
                        }
                        SwipeFocusable(
                            element = EdgeEditorElement.DiscardDialogSave,
                            scope = EdgeEditorScope.DiscardDialog,
                            shape = RoundedCornerShape(8.dp),
                            showBorderHighlight = false,
                            onActivate = discardSaveAction,
                            gridRow = 0,
                            modifier = Modifier.weight(1f),
                        ) {
                            val saveFocused = LocalSwipeFocused.current
                            FilledTonalButton(
                                onClick = discardSaveAction,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    contentColor = if (saveFocused) cs.onPrimary else cs.primary,
                                    containerColor = if (saveFocused) cs.primary else Color.Transparent,
                                )
                            ) { Text("저장", fontSize = 14.sp) }
                        }
                        SwipeFocusable(
                            element = EdgeEditorElement.DiscardDialogDiscard,
                            scope = EdgeEditorScope.DiscardDialog,
                            shape = RoundedCornerShape(8.dp),
                            showBorderHighlight = false,
                            onActivate = discardDiscardAction,
                            gridRow = 0,
                            modifier = Modifier.weight(2f),
                        ) {
                            val discardFocused = LocalSwipeFocused.current
                            FilledTonalButton(
                                onClick = discardDiscardAction,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    contentColor = if (discardFocused) cs.onError else cs.error,
                                    containerColor = if (discardFocused) cs.error else Color.Transparent,
                                )
                            ) { Text("버리고 나가기", fontSize = 14.sp) }
                        }
                        }
                    }
                }
            }
        } else {
            AlertDialog(
                onDismissRequest = discardCancelAction,
                containerColor = cs.surfaceVariant,
                title = { Text("변경사항이 있습니다", color = cs.onSurface) },
                text = { Text("저장하지 않고 나가면 변경사항이 사라집니다.", color = cs.onSurfaceVariant) },
                confirmButton = {
                    TextButton(onClick = discardSaveAction) {
                        Text("저장", color = cs.primary)
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = discardDiscardAction) {
                            Text("버리고 나가기", color = cs.error)
                        }
                        TextButton(onClick = discardCancelAction) {
                            Text("취소")
                        }
                    }
                }
            )
        }
    }

    } // CompositionLocalProvider 닫기
}

// ============================================================
// 액션 도메인 피커 (도메인 행 + 옵션 행 2단 구조)
// ============================================================

private data class ActionOption(
    val label: String,
    val subtitle: String,
    val icon: ImageVector,
    val action: EdgeZoneAction,
    /** null이면 일반 액션 선택. non-null이면 이 콜백만 실행하고 액션 적용은 건너뜀 ("추가" 카드 등). */
    val onClick: (() -> Unit)? = null,
    /** 커스텀 다이나믹스 프리셋. non-null이면 수정/삭제 메뉴 대상. */
    val customDynamicsPreset: CustomPointerDynamicsPreset? = null,
    /** 커스텀 단축키 프리셋. non-null이면 수정/삭제 메뉴 대상. */
    val customShortcutPreset: com.bridgeone.app.ui.common.CustomShortcutPreset? = null,
    /** 커스텀 매크로 프리셋. non-null이면 수정/삭제 메뉴 대상. */
    val customMacroPreset: com.bridgeone.app.ui.common.CustomMacroPreset? = null,
)

// ── 폴더 계층 탐색 데이터 모델 ──

private sealed class ActionTreeNode {
    abstract val nodeKey: String
    data class Folder(
        override val nodeKey: String,
        val label: String,
        val subtitle: String,
        val icon: ImageVector,
        val children: List<ActionTreeNode>,
    ) : ActionTreeNode()
    data class Leaf(
        override val nodeKey: String,
        val option: ActionOption,
    ) : ActionTreeNode()
}

private data class DomainGroup(
    val key: String,
    val label: String,
    val subtitle: String,
    val icon: ImageVector,
    val domains: List<ActionDomain>,
)

/** 12개 도메인을 묶는 4개 그룹. 그룹화를 바꾸려면 이 상수만 교체. */
private val DEFAULT_DOMAIN_GROUPS = listOf(
    DomainGroup("MOUSE", "마우스 동작", "클릭·이동·홀드/릴리즈", Icons.Filled.Mouse,
        listOf(ActionDomain.CLICK, ActionDomain.MOVE, ActionDomain.MOUSE_HOLD)),
    DomainGroup("SCROLL_SENS", "스크롤·감도", "스크롤·속도·DPI·다이나믹스", Icons.Filled.SwapVert,
        listOf(ActionDomain.SCROLL, ActionDomain.SCROLL_SPEED, ActionDomain.DPI, ActionDomain.DYNAMICS)),
    DomainGroup("KEY", "키 입력", "단축키·매크로", Icons.Filled.Keyboard,
        listOf(ActionDomain.COMBO, ActionDomain.MACRO)),
    DomainGroup("MODE_NAV", "모드·탐색", "프리셋·되돌리기·페이지", Icons.Filled.Tune,
        listOf(ActionDomain.MODE_PRESET, ActionDomain.HISTORY, ActionDomain.PAGE)),
)

// Phase 4.7.5-A: ActionDomain enum + domainOf → EdgeZoneActionResolver.kt로 이관

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ActionDomainPicker(
    current: EdgeZoneAction,
    onSelect: (EdgeZoneAction) -> Unit,
    excludeDomains: Set<ActionDomain> = emptySet(),
    customPresets: List<CustomPointerDynamicsPreset> = emptyList(),
    onAddDynamicsPreset: (() -> Unit)? = null,
    onEditCustomDynamics: ((CustomPointerDynamicsPreset) -> Unit)? = null,
    onDeleteCustomDynamics: ((CustomPointerDynamicsPreset) -> Unit)? = null,
    onEditCustomShortcutConfirm: ((com.bridgeone.app.ui.common.CustomShortcutPreset, EdgeZoneAction.SendShortcut) -> Unit)? = null,
    onDeleteCustomShortcut: ((com.bridgeone.app.ui.common.CustomShortcutPreset) -> Unit)? = null,
    customShortcutPresets: List<com.bridgeone.app.ui.common.CustomShortcutPreset> = emptyList(),
    customMacroPresets: List<com.bridgeone.app.ui.common.CustomMacroPreset> = emptyList(),
    onEditCustomMacroConfirm: ((com.bridgeone.app.ui.common.CustomMacroPreset, EdgeZoneAction.SendMacro, String, String) -> Unit)? = null,
    onDeleteCustomMacro: ((com.bridgeone.app.ui.common.CustomMacroPreset) -> Unit)? = null,
    pageCount: Int = 5,
    inputMode: InputMode = InputMode.NORMAL,
    onAddAsCandidate: ((EdgeZoneAction, iconKey: String, name: String) -> Unit)? = null,
    // SWIPE 모드 전용: 팝업을 호출자가 인라인 오버레이로 렌더링하도록 요청
    onSwipeShortcutRequest: ((EdgeZoneAction.SendShortcut, (EdgeZoneAction.SendShortcut) -> Unit, ((draft: EdgeZoneAction.SendShortcut, iconKey: String, name: String) -> Unit)?) -> Unit)? = null,
    onSwipeMacroRequest: ((EdgeZoneAction.SendMacro, initIconKey: String, initName: String, (EdgeZoneAction.SendMacro, String, String) -> Unit) -> Unit)? = null,
    // SWIPE 모드 커스텀 프리셋 수정/삭제 메뉴 대상. null이면 닫힘. 기본값: null
    swipeMenuTarget: CustomPresetTarget? = null,
    onSwipeMenuDismiss: (() -> Unit)? = null,
) {
    val cs = MaterialTheme.colorScheme
    val swipeController = LocalSwipeFocusController.current
    // 브레드크럼 가로 스크롤 상태 (구: 도메인 칩 스크롤)
    val chipScrollState = rememberScrollState()
    var chipViewportWidthPx by remember { mutableIntStateOf(0) }
    // ── 도메인 분류 데이터 (순수 데이터, 상태 변수에 의존하지 않음) ──
    data class DomainInfo(
        val domain: ActionDomain,
        val label: String,
        val subtitle: String,
        val icon: ImageVector,
        val relativeAction: EdgeZoneAction?,
        val relativeLabel: String,
        val relativeSubtitle: String,
        val specificOptions: List<ActionOption>
    )

    val customDpiAction = if (current is EdgeZoneAction.SetCustomDpi) current else EdgeZoneAction.SetCustomDpi(1.0f)
    val customScrollAction = if (current is EdgeZoneAction.SetCustomScrollSpeed) current else EdgeZoneAction.SetCustomScrollSpeed(1.0f)

    val domains = listOf(
        DomainInfo(ActionDomain.CLICK, "클릭", "마우스 클릭 모드", Icons.Filled.Mouse,
            EdgeZoneAction.ToggleMode(EdgeSwipeMode.CLICK), "토글", "클릭 모드 켜고 끄기",
            listOf(
                ActionOption("좌클릭", "왼쪽 버튼", Icons.Filled.Mouse, EdgeZoneAction.SetClickMode(ClickMode.LEFT_CLICK)),
                ActionOption("우클릭", "오른쪽 버튼", Icons.Filled.Mouse, EdgeZoneAction.SetClickMode(ClickMode.RIGHT_CLICK))
            )),
        DomainInfo(ActionDomain.SCROLL, "스크롤", "스크롤 모드 전환 및 세부 선택", Icons.Filled.SwapVert,
            EdgeZoneAction.SwapScrollMode, "토글", "일반↔무한 전환",
            listOf(
                ActionOption("끔", "스크롤 비활성", Icons.Filled.SwapVert, EdgeZoneAction.SetScrollMode(ScrollMode.OFF)),
                ActionOption("일반", "일반 스크롤", Icons.Filled.SwapVert, EdgeZoneAction.SetScrollMode(ScrollMode.NORMAL_SCROLL)),
                ActionOption("무한", "관성 스크롤", Icons.Filled.SwapVert, EdgeZoneAction.SetScrollMode(ScrollMode.INFINITE_SCROLL))
            )),
        DomainInfo(ActionDomain.MOVE, "이동", "커서 이동 방식", Icons.Filled.OpenWith,
            EdgeZoneAction.ToggleMode(EdgeSwipeMode.MOVE), "토글", "이동 모드 켜고 끄기",
            listOf(
                ActionOption("자유", "전 방향 이동", Icons.Filled.OpenWith, EdgeZoneAction.SetMoveMode(MoveMode.FREE)),
                ActionOption("직각", "축 잠금 이동", Icons.Filled.OpenWith, EdgeZoneAction.SetMoveMode(MoveMode.RIGHT_ANGLE))
            )),
        DomainInfo(ActionDomain.DPI, "DPI", "마우스 감도(DPI) 변경", Icons.Filled.Speed,
            EdgeZoneAction.OpenSettings(SettingsType.DPI), "순환", "DPI 설정 순환",
            DpiLevel.entries.map { level ->
                val sub = when (level) {
                    DpiLevel.LOW -> "×0.5 감도"
                    DpiLevel.NORMAL -> "×1.0 감도"
                    DpiLevel.HIGH -> "×2.0 감도"
                }
                ActionOption(level.label, sub, Icons.Filled.Speed, EdgeZoneAction.SetDpi(level))
            }),
        DomainInfo(ActionDomain.SCROLL_SPEED, "속도", "스크롤 속도 변경", Icons.Filled.Loop,
            EdgeZoneAction.OpenSettings(SettingsType.SCROLL_SPEED), "순환", "속도 설정 순환",
            ScrollSensitivity.entries.map { sens ->
                val sub = when (sens) {
                    ScrollSensitivity.SLOW -> "×0.5 속도"
                    ScrollSensitivity.NORMAL -> "×1.0 속도"
                    ScrollSensitivity.FAST -> "×2.0 속도"
                }
                ActionOption(sens.label, sub, Icons.Filled.Loop, EdgeZoneAction.SetScrollSpeed(sens))
            }),
        DomainInfo(ActionDomain.DYNAMICS, "다이나믹스", "동작 곡선 프리셋", Icons.Filled.Timeline,
            EdgeZoneAction.CyclePreset(PresetType.DYNAMICS), "순환", "다이나믹스 프리셋 순환",
            buildList {
                DYNAMICS_PRESETS.forEachIndexed { i, p ->
                    add(ActionOption(p.name, p.description, p.icon.staticIcon, EdgeZoneAction.SetDynamicsPreset(i)))
                }
                customPresets.forEachIndexed { i, cp ->
                    val icon = customPresetIconOrNull(cp.iconKey)?.staticIcon ?: Icons.Filled.Timeline
                    add(ActionOption(
                        cp.name,
                        cp.description.ifEmpty { "커스텀 다이나믹스 프리셋" },
                        icon,
                        EdgeZoneAction.SetDynamicsPreset(DYNAMICS_PRESETS.size + i),
                        customDynamicsPreset = cp,
                    ))
                }
            }),
        DomainInfo(ActionDomain.MODE_PRESET, "프리셋", "전체 모드 조합 프리셋", Icons.Filled.Tune,
            EdgeZoneAction.CyclePreset(PresetType.MODE), "순환", "모드 프리셋 순환",
            MODE_PRESETS.mapIndexed { i, p ->
                ActionOption(p.name, p.description, p.icon.staticIcon, EdgeZoneAction.SetModePreset(i))
            }),
        DomainInfo(ActionDomain.HISTORY, "되돌리기", "이전 모드/세팅 복원", Icons.AutoMirrored.Filled.Undo,
            EdgeZoneAction.RestorePreviousMode, "복원", "직전 상태로",
            emptyList()),
        DomainInfo(ActionDomain.COMBO, "키 입력", "단일 키 또는 조합키 전송", Icons.Filled.Keyboard,
            null, "", "",
            buildList {
                DEFAULT_SHORTCUTS.forEach { shortcut ->
                    add(ActionOption(
                        shortcut.label,
                        shortcut.description,
                        shortcut.icon,
                        EdgeZoneAction.SendShortcut(
                            modifierBits = shortcut.combinedModifiers.toInt(),
                            keyCodes     = if (shortcut.key.toInt() != 0) listOf(shortcut.key.toInt()) else emptyList(),
                            hold         = false,
                            presetLabel  = shortcut.label
                        )
                    ))
                }
                customShortcutPresets.forEach { preset ->
                    val combo = formatShortcutCombo(preset.modifierBits, preset.keyCodes)
                    val label = preset.name.ifEmpty { combo }
                    val icon = if (preset.iconKey.isNotEmpty()) IconRegistry.get(preset.iconKey) else Icons.Filled.Keyboard
                    add(ActionOption(
                        label,
                        "커스텀 단축키",
                        icon,
                        EdgeZoneAction.SendShortcut(
                            modifierBits = preset.modifierBits,
                            keyCodes     = preset.keyCodes,
                            hold         = preset.hold,
                            presetLabel  = label
                        ),
                        customShortcutPreset = preset,
                    ))
                }
            }),
        DomainInfo(ActionDomain.MACRO, "매크로", "순차 키 입력 (딜레이 포함)", Icons.Filled.Keyboard,
            null, "", "",
            buildList {
                customMacroPresets.forEach { preset ->
                    val label = preset.displayName.ifEmpty { formatMacroSteps(preset.steps) }
                    val icon = if (preset.iconKey.isNotEmpty()) IconRegistry.get(preset.iconKey) else Icons.Filled.Keyboard
                    add(ActionOption(
                        label,
                        "매크로 (${preset.steps.size}스텝)",
                        icon,
                        EdgeZoneAction.SendMacro(
                            steps              = preset.steps,
                            stepDelayMs        = preset.stepDelayMs,
                            presetLabel        = label,
                            inputModeCheck = preset.inputModeCheck,
                        ),
                        customMacroPreset = preset,
                    ))
                }
            }),
        DomainInfo(ActionDomain.MOUSE_HOLD, "홀드/릴리즈", "마우스 버튼 홀드·릴리즈·전환", Icons.Filled.Mouse,
            null, "", "",
            listOf(
                ActionOption("좌클릭 홀드", "드래그 고정", Icons.Filled.Mouse,
                    EdgeZoneAction.MouseHoldToggle(MouseButton.LEFT, MouseHoldMode.HOLD)),
                ActionOption("우클릭 홀드", "우클릭 고정", Icons.Filled.Mouse,
                    EdgeZoneAction.MouseHoldToggle(MouseButton.RIGHT, MouseHoldMode.HOLD)),
                ActionOption("중간클릭 홀드", "중간클릭 고정", Icons.Filled.Mouse,
                    EdgeZoneAction.MouseHoldToggle(MouseButton.MIDDLE, MouseHoldMode.HOLD)),
                ActionOption("좌클릭 릴리즈", "드래그 해제", Icons.Filled.Mouse,
                    EdgeZoneAction.MouseHoldToggle(MouseButton.LEFT, MouseHoldMode.RELEASE)),
                ActionOption("우클릭 릴리즈", "우클릭 해제", Icons.Filled.Mouse,
                    EdgeZoneAction.MouseHoldToggle(MouseButton.RIGHT, MouseHoldMode.RELEASE)),
                ActionOption("중간클릭 릴리즈", "중간클릭 해제", Icons.Filled.Mouse,
                    EdgeZoneAction.MouseHoldToggle(MouseButton.MIDDLE, MouseHoldMode.RELEASE)),
                ActionOption("좌클릭 홀드/릴리즈 전환", "ON↔OFF 토글", Icons.Filled.Mouse,
                    EdgeZoneAction.MouseHoldToggle(MouseButton.LEFT, MouseHoldMode.TOGGLE)),
                ActionOption("우클릭 홀드/릴리즈 전환", "ON↔OFF 토글", Icons.Filled.Mouse,
                    EdgeZoneAction.MouseHoldToggle(MouseButton.RIGHT, MouseHoldMode.TOGGLE)),
                ActionOption("중간클릭 홀드/릴리즈 전환", "ON↔OFF 토글", Icons.Filled.Mouse,
                    EdgeZoneAction.MouseHoldToggle(MouseButton.MIDDLE, MouseHoldMode.TOGGLE))
            )),
        DomainInfo(ActionDomain.PAGE, "페이지", "앱 페이지 전환", Icons.AutoMirrored.Filled.ArrowForward,
            EdgeZoneAction.CyclePage(PageNav.NEXT), "다음", "다음 페이지로",
            buildList {
                add(ActionOption("이전", "이전 페이지로", Icons.AutoMirrored.Filled.ArrowBack,
                    EdgeZoneAction.CyclePage(PageNav.PREV)))
                for (i in 0 until pageCount) {
                    add(ActionOption("페이지 ${i + 1}", "${i + 1}번 페이지로 바로 이동",
                        Icons.AutoMirrored.Filled.ArrowForward, EdgeZoneAction.JumpToPage(i)))
                }
            }),
    )

    val filteredDomains = if (excludeDomains.isEmpty()) domains else domains.filter { it.domain !in excludeDomains }

    // ── 초기 경로 계산 (상태 변수 불필요 — current 파라미터와 filteredDomains만 사용) ──
    /** 현재 액션이 속한 도메인을 받아 트리 상의 경로(nodeKey 리스트)를 반환한다. */
    fun findInitialPathKeys(domain: ActionDomain): List<String> {
        if (domain == ActionDomain.UNASSIGNED) return emptyList()
        val domainInfo = filteredDomains.find { it.domain == domain } ?: return emptyList()
        // relativeAction + specificOptions만 계산 (add-card는 제외)
        val realOptionCount = (if (domainInfo.relativeAction != null) 1 else 0) + domainInfo.specificOptions.size
        // 실제 액션이 1개뿐(add-card 아님)이면 도메인 폴더 생략하고 그룹 레벨에 노출 (현재: HISTORY)
        val isFlattened = realOptionCount == 1 && domainInfo.relativeAction != null
        for (group in DEFAULT_DOMAIN_GROUPS) {
            if (domain in group.domains) {
                val groupKey = "GROUP:${group.key}"
                return if (isFlattened) listOf(groupKey) else listOf(groupKey, "DOMAIN:${domain.name}")
            }
        }
        return emptyList()
    }

    // 폴더 탐색 경로 스택 (nodeKey 문자열 리스트). current 변경 시 deep-link 경로로 리셋.
    var pathKeyStack by remember(current) {
        val domain = EdgeZoneActionResolver.domainOf(current).takeUnless { it == ActionDomain.UNASSIGNED || it in excludeDomains }
        mutableStateOf(if (domain != null) findInitialPathKeys(domain) else emptyList<String>())
    }

    // 커스텀 단축키 팝업 상태 (NORMAL 모드 전용 — SWIPE 모드는 호출자가 인라인 오버레이로 렌더링)
    var shortcutPopupOpen by remember { mutableStateOf(false) }
    var draftShortcut by remember { mutableStateOf(EdgeZoneAction.SendShortcut(0)) }
    // 편집 중인 단축키 프리셋 (null이면 신규 생성). 기본값: null
    var editingShortcutPreset by remember { mutableStateOf<com.bridgeone.app.ui.common.CustomShortcutPreset?>(null) }
    // NORMAL 모드 커스텀 프리셋 수정/삭제 메뉴 대상. null이면 닫힘. 기본값: null
    var normalMenuTarget by remember { mutableStateOf<CustomPresetTarget?>(null) }
    // 수정/삭제 인라인 버튼 — 삭제 확인 2단계 여부. 기본값: false
    var confirmingDelete by remember { mutableStateOf(false) }
    // combinedClickable에서 onLongClick 발생 후 onClick이 연달아 오는 경우 억제. 기본값: false
    var suppressNextClick by remember { mutableStateOf(false) }
    // SWIPE 모드 삭제 후 이동할 포커스 대상 (onDispose에서 소비)
    // computeAndStore는 currentChildren 이후에 매 리컴포지션마다 갱신됨
    val postDeleteFocusHolder = remember { object {
        var nodeKey: String? = null
        var isFolder: Boolean = false
        var computeAndStore: ((CustomPresetTarget) -> Unit)? = null
    } }

    // 메뉴 대상이 바뀌면 확인 상태 초기화
    LaunchedEffect(normalMenuTarget, swipeMenuTarget) { confirmingDelete = false }

    // SWIPE 모드: swipeMenuTarget이 설정될 때 CustomPresetMenu scope로 전환, 해제 시 복귀
    val activeSwipeMenu = if (inputMode == InputMode.SWIPE) swipeMenuTarget else null
    if (activeSwipeMenu != null && swipeController != null) {
        val returnKey = when (activeSwipeMenu) {
            is CustomPresetTarget.Dynamics -> "DYNAMICS:${activeSwipeMenu.preset.name}"
            is CustomPresetTarget.Shortcut -> "COMBO:${formatShortcutCombo(activeSwipeMenu.preset.modifierBits, activeSwipeMenu.preset.keyCodes)}"
            is CustomPresetTarget.Macro -> "MACRO:${formatMacroSteps(activeSwipeMenu.preset.steps)}"
        }
        DisposableEffect(activeSwipeMenu) {
            swipeController.pushScope(EdgeEditorScope.CustomPresetMenu)
            swipeController.setFocus(EdgeEditorElement.CustomMenuEdit)
            onDispose {
                swipeController.popScope()
                val nextKey = postDeleteFocusHolder.nodeKey
                postDeleteFocusHolder.nodeKey = null
                if (nextKey != null) {
                    if (postDeleteFocusHolder.isFolder) {
                        swipeController.setFocus(EdgeEditorElement.ActionFolderCard(nextKey))
                    } else {
                        swipeController.setFocus(EdgeEditorElement.ActionOptionCard(nextKey))
                    }
                } else {
                    swipeController.setFocus(EdgeEditorElement.ActionOptionCard(returnKey))
                }
            }
        }
    }

    // 현재 활성 메뉴 대상 (NORMAL/SWIPE 통합)
    val activeMenuTarget = if (inputMode == InputMode.NORMAL) normalMenuTarget else swipeMenuTarget

    // 수정 액션
    val menuEditAction: () -> Unit = {
        when (val mt = activeMenuTarget) {
            is CustomPresetTarget.Dynamics -> {
                onEditCustomDynamics?.invoke(mt.preset)
                if (inputMode == InputMode.NORMAL) normalMenuTarget = null else onSwipeMenuDismiss?.invoke()
            }
            is CustomPresetTarget.Shortcut -> {
                val preset = mt.preset
                val combo = formatShortcutCombo(preset.modifierBits, preset.keyCodes)
                val displayLabel = preset.name.ifEmpty { combo }
                if (inputMode == InputMode.NORMAL) {
                    editingShortcutPreset = preset
                    draftShortcut = EdgeZoneAction.SendShortcut(preset.modifierBits, preset.keyCodes, preset.hold, presetLabel = displayLabel)
                    shortcutPopupOpen = true
                    normalMenuTarget = null
                } else {
                    onSwipeShortcutRequest?.invoke(
                        EdgeZoneAction.SendShortcut(preset.modifierBits, preset.keyCodes, preset.hold, presetLabel = displayLabel),
                        { confirmed -> onEditCustomShortcutConfirm?.invoke(preset, confirmed) },
                        null
                    )
                    onSwipeMenuDismiss?.invoke()
                }
            }
            is CustomPresetTarget.Macro -> {
                val preset = mt.preset
                val draft = EdgeZoneAction.SendMacro(preset.steps, preset.stepDelayMs, formatMacroSteps(preset.steps), preset.inputModeCheck)
                onSwipeMacroRequest?.invoke(draft, preset.iconKey, preset.displayName) { confirmed, icon, nm ->
                    onEditCustomMacroConfirm?.invoke(preset, confirmed, icon, nm)
                }
                if (inputMode == InputMode.SWIPE) onSwipeMenuDismiss?.invoke() else normalMenuTarget = null
            }
            null -> {}
        }
    }

    // 삭제 액션
    val menuDeleteAction: () -> Unit = {
        val mt = activeMenuTarget
        // SWIPE 모드: currentChildren 이후에 등록된 계산 람다 호출
        if (inputMode == InputMode.SWIPE && mt != null) {
            postDeleteFocusHolder.computeAndStore?.invoke(mt)
        }
        when (mt) {
            is CustomPresetTarget.Dynamics -> onDeleteCustomDynamics?.invoke(mt.preset)
            is CustomPresetTarget.Shortcut -> onDeleteCustomShortcut?.invoke(mt.preset)
            is CustomPresetTarget.Macro -> onDeleteCustomMacro?.invoke(mt.preset)
            null -> {}
        }
        if (inputMode == InputMode.NORMAL) normalMenuTarget = null else onSwipeMenuDismiss?.invoke()
        confirmingDelete = false
    }

    // 시스템 뒤로가기: 팝업 닫기 > 폴더 계층 한 단계 복귀 순
    BackHandler(enabled = (shortcutPopupOpen || normalMenuTarget != null || pathKeyStack.isNotEmpty()) && inputMode == InputMode.NORMAL) {
        when {
            normalMenuTarget != null && confirmingDelete -> confirmingDelete = false
            normalMenuTarget != null -> normalMenuTarget = null
            shortcutPopupOpen -> shortcutPopupOpen = false
            pathKeyStack.isNotEmpty() -> pathKeyStack = pathKeyStack.dropLast(1)
        }
    }

    // ── 폴더 계층 트리 빌드 (상태 변수 폐쇄 포함 — 매 리컴포지션에서 재빌드) ──

    /** 도메인별 옵션 리스트 빌드 (add-card 포함). `optionsForDomain`으로 추출해 트리 빌드와 공유. */
    fun optionsForDomain(info: DomainInfo): List<ActionOption> = buildList {
        info.relativeAction?.let {
            add(ActionOption(info.relativeLabel, info.relativeSubtitle, info.icon, it))
        }
        addAll(info.specificOptions)
        when (info.domain) {
            ActionDomain.DPI -> add(ActionOption("커스텀", "직접 지정", Icons.Filled.Speed, customDpiAction))
            ActionDomain.SCROLL_SPEED -> add(ActionOption("커스텀", "직접 지정", Icons.Filled.Loop, customScrollAction))
            ActionDomain.DYNAMICS -> if (onAddDynamicsPreset != null) {
                add(ActionOption("추가", "커스텀 프리셋 생성", Icons.Filled.Add, EdgeZoneAction.Unassigned, onClick = onAddDynamicsPreset))
            }
            ActionDomain.COMBO -> {
                add(ActionOption("추가", "커스텀 키 입력 생성", Icons.Filled.Add, EdgeZoneAction.Unassigned,
                    onClick = {
                        val initDraft = if (current is EdgeZoneAction.SendShortcut && current.presetLabel.isEmpty()) current
                            else EdgeZoneAction.SendShortcut(0)
                        if (inputMode == InputMode.SWIPE && onSwipeShortcutRequest != null) {
                            onSwipeShortcutRequest(initDraft, onSelect,
                                if (onAddAsCandidate != null) { { draft, iconKey, name -> onAddAsCandidate(draft, iconKey, name) } } else null
                            )
                        } else {
                            draftShortcut = initDraft
                            shortcutPopupOpen = true
                        }
                    }
                ))
            }
            ActionDomain.MACRO -> add(ActionOption("추가", "새 매크로 생성", Icons.Filled.Add, EdgeZoneAction.Unassigned,
                onClick = {
                    val macroDraft = EdgeZoneAction.SendMacro()
                    onSwipeMacroRequest?.invoke(macroDraft, "", "") { confirmed, _, _ -> onSelect(confirmed) }
                }
            ))
            else -> {}
        }
    }

    /**
     * 전체 액션 트리를 빌드한다.
     * 구조: 루트 → 그룹 폴더 → 도메인 폴더 → 액션 Leaf.
     * 도메인 폴더가 실제 액션 1개뿐(add-card 제외)이면 폴더를 생략하고 그룹 레벨에 Leaf로 직접 배치한다.
     */
    fun buildActionTree(): List<ActionTreeNode> = DEFAULT_DOMAIN_GROUPS.mapNotNull { group ->
        val groupChildren: List<ActionTreeNode> = group.domains.mapNotNull { domain ->
            val domainInfo = filteredDomains.find { it.domain == domain } ?: return@mapNotNull null
            val options = optionsForDomain(domainInfo)
            if (options.isEmpty()) return@mapNotNull null
            val leaves = options.map { option ->
                ActionTreeNode.Leaf("${domain.name}:${option.label}", option)
            }
            // 실제 액션(add-card 제외)이 1개뿐이면 auto-flatten
            val realLeaves = leaves.filter { it.option.onClick == null }
            if (realLeaves.size == 1 && leaves.size == 1) {
                // 도메인 폴더 생략: leaf를 도메인 라벨로 노출
                ActionTreeNode.Leaf("DOMAIN:${domain.name}", realLeaves[0].option.copy(
                    label = domainInfo.label, subtitle = domainInfo.subtitle
                ))
            } else {
                ActionTreeNode.Folder(
                    nodeKey = "DOMAIN:${domain.name}",
                    label = domainInfo.label,
                    subtitle = domainInfo.subtitle,
                    icon = domainInfo.icon,
                    children = leaves,
                )
            }
        }
        if (groupChildren.isEmpty()) return@mapNotNull null
        ActionTreeNode.Folder(
            nodeKey = "GROUP:${group.key}",
            label = group.label,
            subtitle = group.subtitle,
            icon = group.icon,
            children = groupChildren,
        )
    }

    // 트리 (매 리컴포지션 재빌드 — 빠른 리스트 연산이므로 remember 불필요)
    val rootTree = buildActionTree()

    // 경로 키에서 실제 자식 목록 도출 (AnimatedContent content lambda 안에서도 재사용)
    fun childrenForPath(path: List<String>): List<ActionTreeNode> {
        var nodes: List<ActionTreeNode> = rootTree
        for (key in path) {
            val folder = nodes.filterIsInstance<ActionTreeNode.Folder>().find { it.nodeKey == key }
                ?: break
            nodes = folder.children
        }
        return nodes
    }
    val currentChildren: List<ActionTreeNode> = childrenForPath(pathKeyStack)

    // SWIPE 모드 삭제 시 다음 포커스 계산 람다 — currentChildren을 캡처해 매 리컴포지션마다 갱신
    postDeleteFocusHolder.computeAndStore = { mt ->
        val deletedNodeKey = when (mt) {
            is CustomPresetTarget.Dynamics -> "DYNAMICS:${mt.preset.name}"
            is CustomPresetTarget.Shortcut -> "COMBO:${formatShortcutCombo(mt.preset.modifierBits, mt.preset.keyCodes)}"
            is CustomPresetTarget.Macro -> "MACRO:${formatMacroSteps(mt.preset.steps)}"
        }
        val idx = currentChildren.indexOfFirst { it.nodeKey == deletedNodeKey }
        val nextNode = when {
            idx >= 0 && idx < currentChildren.size - 1 -> currentChildren[idx + 1]
            idx > 0 -> currentChildren[idx - 1]
            else -> null
        }
        postDeleteFocusHolder.nodeKey = nextNode?.nodeKey
        postDeleteFocusHolder.isFolder = nextNode is ActionTreeNode.Folder
    }

    // 브레드크럼 표시용 폴더 목록 (pathKeyStack에서 폴더 객체 재조회)
    val currentFolders: List<ActionTreeNode.Folder> = run {
        val result = mutableListOf<ActionTreeNode.Folder>()
        var nodes: List<ActionTreeNode> = rootTree
        for (key in pathKeyStack) {
            val folder = nodes.filterIsInstance<ActionTreeNode.Folder>().find { it.nodeKey == key }
                ?: break
            result.add(folder)
            nodes = folder.children
        }
        result
    }

    // ── 자동 스크롤 ──

    // 브레드크럼 세그먼트 포커스 이동 시 가로 스크롤 (구: 도메인 칩 가로 스크롤)
    LaunchedEffect(swipeController?.currentFocus) {
        val focus = swipeController?.currentFocus
        if (focus is EdgeEditorElement.BreadcrumbSegment) {
            when {
                focus.depth == 0 -> chipScrollState.animateScrollTo(0)
                focus.depth >= pathKeyStack.size -> chipScrollState.animateScrollTo(chipScrollState.maxValue)
                // 중간 세그먼트: 끝 방향으로 스크롤
                else -> chipScrollState.animateScrollTo(chipScrollState.maxValue)
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // ── 브레드크럼 (현재 폴더 경로 표시) ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { chipViewportWidthPx = it.width }
                .horizontalScroll(chipScrollState),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val breadcrumbLabels = listOf("루트") + currentFolders.map { it.label }
            breadcrumbLabels.forEachIndexed { depth, label ->
                val isCurrent = depth == breadcrumbLabels.lastIndex
                val segmentAction: () -> Unit = {
                    // depth만큼 잘라낸 경로로 이동
                    pathKeyStack = pathKeyStack.take(depth)
                    if (swipeController != null) {
                        // 이동 후 currentChildren에서 첫 자식 포커스
                        val newChildren: List<ActionTreeNode> = run {
                            var nodes: List<ActionTreeNode> = rootTree
                            for (key in pathKeyStack.take(depth)) {
                                val folder = nodes.filterIsInstance<ActionTreeNode.Folder>().find { it.nodeKey == key } ?: break
                                nodes = folder.children
                            }
                            nodes
                        }
                        val first = newChildren.firstOrNull()
                        when (first) {
                            is ActionTreeNode.Folder -> swipeController.setFocus(EdgeEditorElement.ActionFolderCard(first.nodeKey))
                            is ActionTreeNode.Leaf -> swipeController.setFocus(EdgeEditorElement.ActionOptionCard(first.nodeKey))
                            null -> {}
                        }
                    }
                }
                SwipeFocusable(
                    element = EdgeEditorElement.BreadcrumbSegment(depth),
                    shape = RoundedCornerShape(6.dp),
                    showBorderHighlight = !isCurrent,
                    onActivate = segmentAction,
                    gridRow = 31,
                    gridCol = depth,
                ) {
                val isFocused = LocalSwipeFocused.current
                Box(
                    modifier = Modifier
                        .height(28.dp) // 기본값: 32.dp
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            when {
                                isCurrent -> cs.primaryContainer.copy(alpha = 0.6f)
                                isFocused -> cs.surfaceVariant
                                else -> Color.Transparent
                            }
                        )
                        .then(
                            if (isCurrent && isFocused)
                                Modifier.border(2.dp, Color.White, RoundedCornerShape(6.dp))
                            else Modifier
                        )
                        .clickable(onClick = segmentAction)
                        .padding(horizontal = 6.dp, vertical = 2.dp), // 기본값: h=10, v=4
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp, // 기본값: 12.sp
                        fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isCurrent) cs.onPrimaryContainer else cs.onSurface.copy(alpha = 0.55f),
                        maxLines = 1,
                        style = androidx.compose.ui.text.TextStyle(
                            platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
                        ),
                    )
                }
                } // SwipeFocusable(BreadcrumbSegment) 닫기
                if (!isCurrent) {
                    Text(
                        text = "›",
                        fontSize = 12.sp,
                        color = cs.onSurface.copy(alpha = 0.35f),
                        modifier = Modifier.padding(horizontal = 2.dp),
                    )
                }
            }
        }

        HorizontalDivider(color = cs.outline.copy(alpha = 0.2f))

        // ── 카드 그리드 (폴더 + Leaf 혼합, 3열) ──
        AnimatedContent(
            targetState = pathKeyStack,
            transitionSpec = {
                val forward = targetState.size >= initialState.size
                val dur = EdgeSwipeConstants.EDGE_ZONE_FOLDER_NAV_ANIM_MS
                val f = EdgeSwipeConstants.EDGE_ZONE_FOLDER_NAV_SLIDE_FRACTION
                if (forward) {
                    (slideInHorizontally(tween(dur, easing = FastOutSlowInEasing)) { (it * f).toInt() } +
                        fadeIn(tween(dur, easing = FastOutSlowInEasing))) togetherWith
                    (slideOutHorizontally(tween(dur, easing = FastOutSlowInEasing)) { -(it * f).toInt() } +
                        fadeOut(tween(dur, easing = FastOutSlowInEasing)))
                } else {
                    (slideInHorizontally(tween(dur, easing = FastOutSlowInEasing)) { -(it * f).toInt() } +
                        fadeIn(tween(dur, easing = FastOutSlowInEasing))) togetherWith
                    (slideOutHorizontally(tween(dur, easing = FastOutSlowInEasing)) { (it * f).toInt() } +
                        fadeOut(tween(dur, easing = FastOutSlowInEasing)))
                }
            },
            label = "folderNavGrid",
        ) { targetPath ->
        val targetChildren = childrenForPath(targetPath)
        if (targetChildren.isNotEmpty()) {
            val gridState = rememberLazyGridState()
            // 카드(폴더·Leaf) 포커스 이동 시 그리드 영역 세로 자동 스크롤
            LaunchedEffect(swipeController?.currentFocus) {
                val focus = swipeController?.currentFocus
                val focusKey = when (focus) {
                    is EdgeEditorElement.ActionFolderCard -> focus.nodeKey
                    is EdgeEditorElement.ActionOptionCard -> focus.key
                    else -> null
                }
                if (focusKey != null) {
                    val idx = targetChildren.indexOfFirst { it.nodeKey == focusKey }
                    if (idx >= 0) {
                        val visible = gridState.layoutInfo.visibleItemsInfo.any { it.index == idx }
                        if (!visible) gridState.animateScrollToItem(idx)
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    state = gridState,
                    modifier = Modifier.heightIn(max = EdgeSwipeConstants.EDGE_ZONE_OPTION_GRID_MAX_HEIGHT_DP.dp), // 3행 상한
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(targetChildren, key = { _, node -> node.nodeKey }) { index, node ->
                        val chunkIdx = index / 3
                        val colIdx = index % 3
                        Box(Modifier.animateItem()) {
                            when (node) {
                                is ActionTreeNode.Folder -> {
                                    // ── 폴더 카드 ──
                                    val folderAction: () -> Unit = {
                                        pathKeyStack = pathKeyStack + node.nodeKey
                                        if (swipeController != null) {
                                            val first = node.children.firstOrNull()
                                            when (first) {
                                                is ActionTreeNode.Folder -> swipeController.setFocus(EdgeEditorElement.ActionFolderCard(first.nodeKey))
                                                is ActionTreeNode.Leaf -> swipeController.setFocus(EdgeEditorElement.ActionOptionCard(first.nodeKey))
                                                null -> {}
                                            }
                                        }
                                    }
                                    SwipeFocusable(
                                        element = EdgeEditorElement.ActionFolderCard(node.nodeKey),
                                        shape = RoundedCornerShape(12.dp),
                                        showBorderHighlight = true,
                                        onActivate = folderAction,
                                        gridRow = 32 + chunkIdx,
                                        gridCol = colIdx,
                                    ) {
                                    val noPad = androidx.compose.ui.text.TextStyle(
                                        platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(80.dp) // 기본값: 80.dp
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(cs.secondaryContainer.copy(alpha = 0.55f))
                                            .border(0.5.dp, cs.outline.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                            .clickable(onClick = folderAction)
                                    ) {
                                        Column(
                                            modifier = Modifier.align(Alignment.Center),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp) // 기본값: 4.dp
                                        ) {
                                            Icon(
                                                imageVector = node.icon,
                                                contentDescription = node.label,
                                                tint = cs.onSecondaryContainer,
                                                modifier = Modifier.size(26.dp) // 기본값: 26.dp
                                            )
                                            Text(
                                                node.label,
                                                fontSize = 12.sp, // 기본값: 12.sp
                                                fontWeight = FontWeight.SemiBold,
                                                color = cs.onSecondaryContainer,
                                                style = noPad,
                                                maxLines = 1,
                                            )
                                        }
                                        // 폴더 진입 표식 (우상단 ChevronRight)
                                        Icon(
                                            imageVector = Icons.Filled.KeyboardArrowRight,
                                            contentDescription = null,
                                            tint = cs.onSecondaryContainer.copy(alpha = 0.45f),
                                            modifier = Modifier
                                                .size(14.dp) // 기본값: 14.dp
                                                .align(Alignment.TopEnd)
                                                .offset(x = (-4).dp, y = 4.dp)
                                        )
                                    }
                                    } // SwipeFocusable(ActionFolderCard) 닫기
                                }
                                is ActionTreeNode.Leaf -> {
                                    // ── Leaf 카드 (기존 옵션 카드 로직 재사용) ──
                                    val option = node.option
                                    val isSelected = option.onClick == null && EdgeZoneActionResolver.actionEquals(current, option.action)
                                    val isAddCard = option.onClick != null
                                    val isCustom = option.customDynamicsPreset != null || option.customShortcutPreset != null || option.customMacroPreset != null
                                    val isMenuMode = activeMenuTarget?.let { mt ->
                                        when (mt) {
                                            is CustomPresetTarget.Dynamics -> option.customDynamicsPreset?.id == mt.preset.id
                                            is CustomPresetTarget.Shortcut -> option.customShortcutPreset?.id == mt.preset.id
                                            is CustomPresetTarget.Macro -> option.customMacroPreset?.id == mt.preset.id
                                        }
                                    } ?: false
                                    val optionAction: () -> Unit = {
                                        if (option.onClick != null) option.onClick.invoke()
                                        else if (isSelected) onSelect(EdgeZoneAction.Unassigned)
                                        else onSelect(option.action)
                                    }
                                    SwipeFocusable(
                                        element = EdgeEditorElement.ActionOptionCard(node.nodeKey),
                                        shape = RoundedCornerShape(12.dp),
                                        showBorderHighlight = !isSelected,
                                        onActivate = optionAction,
                                        gridRow = 32 + chunkIdx,
                                        gridCol = colIdx,
                                    ) {
                                    val isFocused = LocalSwipeFocused.current
                // ── Leaf 카드 본문 (SwipeFocusable 내부) ──
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(80.dp) // 기본값: 80.dp
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            when {
                                                isMenuMode -> cs.surfaceVariant
                                                isSelected -> cs.primaryContainer
                                                isAddCard -> cs.surface
                                                else -> cs.surfaceVariant
                                            }
                                        )
                                        .border(
                                            width = when {
                                                isMenuMode -> 1.5.dp
                                                isSelected && isFocused -> 2.dp
                                                isSelected -> 1.5.dp
                                                isAddCard && isFocused -> 1.5.dp
                                                isAddCard -> 1.dp
                                                else -> 0.5.dp
                                            },
                                            color = when {
                                                isMenuMode -> cs.primary.copy(alpha = 0.5f)
                                                isSelected && isFocused -> Color.White
                                                isSelected -> cs.primary
                                                isAddCard -> cs.primary.copy(alpha = if (isFocused) 0.8f else 0.45f)
                                                else -> cs.outline.copy(alpha = 0.25f)
                                            },
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .combinedClickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = ripple(bounded = true),
                                            onClick = {
                                                if (suppressNextClick) {
                                                    suppressNextClick = false
                                                } else if (isMenuMode && inputMode == InputMode.NORMAL) {
                                                    normalMenuTarget = null
                                                } else {
                                                    optionAction()
                                                }
                                            },
                                            onLongClick = if (!isAddCard && inputMode == InputMode.NORMAL) {
                                                {
                                                    suppressNextClick = true
                                                    if (isCustom) {
                                                        if (isMenuMode) {
                                                            normalMenuTarget = null
                                                        } else {
                                                            val target = option.customDynamicsPreset?.let { CustomPresetTarget.Dynamics(it) }
                                                                ?: option.customShortcutPreset?.let { CustomPresetTarget.Shortcut(it) }
                                                                ?: option.customMacroPreset?.let { CustomPresetTarget.Macro(it) }
                                                            if (target != null) normalMenuTarget = target
                                                        }
                                                    } else {
                                                        ToastController.show("커스텀 프리셋만 수정하거나 삭제할 수 있습니다", ToastType.INFO)
                                                    }
                                                }
                                            } else null,
                                        )
                                ) {
                                    val noPad = androidx.compose.ui.text.TextStyle(
                                        platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
                                    )
                                    if (isMenuMode) {
                                        val btnPad = PaddingValues(horizontal = 4.dp, vertical = 2.dp) // 기본값: h=4, v=2
                                        if (!confirmingDelete) {
                                            // 수정 / 삭제 버튼
                                            Row(
                                                modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 8.dp), // 기본값: h=6, v=8
                                                horizontalArrangement = Arrangement.spacedBy(4.dp), // 기본값: 4.dp
                                            ) {
                                                val editContent: @Composable () -> Unit = {
                                                    FilledTonalButton(
                                                        onClick = menuEditAction,
                                                        modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                                                        shape = RoundedCornerShape(8.dp),
                                                        contentPadding = btnPad,
                                                    ) {
                                                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                            Icon(Icons.Filled.Edit, null, Modifier.size(16.dp)) // 기본값: 16.dp
                                                            Text("수정", fontSize = 10.sp, style = noPad) // 기본값: 10.sp
                                                        }
                                                    }
                                                }
                                                val deleteContent: @Composable () -> Unit = {
                                                    FilledTonalButton(
                                                        onClick = { confirmingDelete = true },
                                                        modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                                                        shape = RoundedCornerShape(8.dp),
                                                        contentPadding = btnPad,
                                                        colors = ButtonDefaults.filledTonalButtonColors(
                                                            containerColor = cs.error.copy(alpha = 0.12f), // 기본값: 0.12f
                                                            contentColor = cs.error,
                                                        ),
                                                    ) {
                                                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                            Icon(Icons.Filled.Delete, null, Modifier.size(16.dp)) // 기본값: 16.dp
                                                            Text("삭제", fontSize = 10.sp, style = noPad) // 기본값: 10.sp
                                                        }
                                                    }
                                                }
                                                if (inputMode == InputMode.SWIPE && swipeController != null) {
                                                    SwipeFocusable(EdgeEditorElement.CustomMenuEdit, scope = EdgeEditorScope.CustomPresetMenu, shape = RoundedCornerShape(8.dp), showBorderHighlight = true, onActivate = menuEditAction, gridRow = 0, gridCol = 0, modifier = Modifier.weight(1f)) { editContent() }
                                                    SwipeFocusable(EdgeEditorElement.CustomMenuDelete, scope = EdgeEditorScope.CustomPresetMenu, shape = RoundedCornerShape(8.dp), showBorderHighlight = true, onActivate = { confirmingDelete = true; swipeController.setFocus(EdgeEditorElement.CustomMenuDeleteCancel) }, gridRow = 0, gridCol = 1, modifier = Modifier.weight(1f)) { deleteContent() }
                                                } else {
                                                    Box(modifier = Modifier.weight(1f)) { editContent() }
                                                    Box(modifier = Modifier.weight(1f)) { deleteContent() }
                                                }
                                            }
                                        } else {
                                            // 삭제 확인: 취소 / 확인 버튼
                                            Row(
                                                modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 8.dp), // 기본값: h=6, v=8
                                                horizontalArrangement = Arrangement.spacedBy(4.dp), // 기본값: 4.dp
                                            ) {
                                                val cancelContent: @Composable () -> Unit = {
                                                    FilledTonalButton(
                                                        onClick = { confirmingDelete = false },
                                                        modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                                                        shape = RoundedCornerShape(8.dp),
                                                        contentPadding = btnPad,
                                                    ) { Text("취소", fontSize = 10.sp, style = noPad) } // 기본값: 10.sp
                                                }
                                                val confirmContent: @Composable () -> Unit = {
                                                    FilledTonalButton(
                                                        onClick = menuDeleteAction,
                                                        modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                                                        shape = RoundedCornerShape(8.dp),
                                                        contentPadding = btnPad,
                                                        colors = ButtonDefaults.filledTonalButtonColors(
                                                            containerColor = cs.error,
                                                            contentColor = cs.onError,
                                                        ),
                                                    ) { Text("확인", fontSize = 10.sp, style = noPad) } // 기본값: 10.sp
                                                }
                                                if (inputMode == InputMode.SWIPE && swipeController != null) {
                                                    SwipeFocusable(EdgeEditorElement.CustomMenuDeleteCancel, scope = EdgeEditorScope.CustomPresetMenu, shape = RoundedCornerShape(8.dp), showBorderHighlight = true, onActivate = { confirmingDelete = false; swipeController.setFocus(EdgeEditorElement.CustomMenuEdit) }, gridRow = 0, gridCol = 0, modifier = Modifier.weight(1f)) { cancelContent() }
                                                    SwipeFocusable(EdgeEditorElement.CustomMenuDeleteConfirm, scope = EdgeEditorScope.CustomPresetMenu, shape = RoundedCornerShape(8.dp), showBorderHighlight = true, onActivate = menuDeleteAction, gridRow = 0, gridCol = 1, modifier = Modifier.weight(1f)) { confirmContent() }
                                                } else {
                                                    Box(modifier = Modifier.weight(1f)) { cancelContent() }
                                                    Box(modifier = Modifier.weight(1f)) { confirmContent() }
                                                }
                                            }
                                        }
                                    } else {
                                        var labelFontSize by remember(option.label) { mutableStateOf(12.sp) } // 기본값: 12.sp
                                        Column(
                                            modifier = Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal = 4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = option.icon,
                                                contentDescription = null,
                                                tint = when {
                                                    isSelected -> cs.onPrimaryContainer
                                                    isAddCard -> cs.primary.copy(alpha = 0.75f)
                                                    else -> cs.onSurface
                                                },
                                                modifier = Modifier.size(28.dp) // 기본값: 28.dp
                                            )
                                            Text(
                                                option.label,
                                                fontSize = labelFontSize,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                color = when {
                                                    isSelected -> cs.onPrimaryContainer
                                                    isAddCard -> cs.primary.copy(alpha = 0.75f)
                                                    else -> cs.onSurface
                                                },
                                                style = noPad,
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Clip,
                                                textAlign = TextAlign.Center,
                                                onTextLayout = { result ->
                                                    if (result.didOverflowWidth && labelFontSize > 7.sp) {
                                                        labelFontSize *= 0.85f
                                                    }
                                                }
                                            )
                                        }
                                        // '현재 할당' dot: 이 존에 현재 배치된 액션에만 표시 — 흰색 점
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp) // 기본값: 6.dp
                                                    .align(Alignment.TopEnd)
                                                    .offset(
                                                        x = if (!isCustom && !isAddCard) (-13).dp else (-5).dp,
                                                        y = 5.dp
                                                    )
                                                    .clip(CircleShape)
                                                    .background(cs.onPrimaryContainer)
                                            )
                                        }
                                        // '기본' 뱃지: 빌트인/고정 액션(커스텀 아님, 추가카드 아님)에만 표시 — 노란색 점
                                        // 기본 매크로 프리셋(id가 "default_"로 시작)은 내장으로 간주
                                        val isBuiltInMacro = option.customMacroPreset?.id?.startsWith("default_") == true
                                        if (!isAddCard && (!isCustom || isBuiltInMacro)) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp) // 기본값: 6.dp
                                                    .align(Alignment.TopEnd)
                                                    .offset(x = (-5).dp, y = 5.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFFFC107))
                                            )
                                        }
                                    }
                                }
                                } // SwipeFocusable(ActionOptionCard) 닫기
                            } // is ActionTreeNode.Leaf 닫기
                        } // when (node) 닫기
                        } // Box(animateItem) 닫기
                    } // itemsIndexed 닫기
                } // LazyVerticalGrid 닫기

                // ── 커스텀 배율 슬라이더 ──
                    val sliderMultiplier: Float?
                    val sliderOnChange: ((Float) -> Unit)?
                    when (current) {
                        is EdgeZoneAction.SetCustomDpi -> {
                            sliderMultiplier = current.multiplier
                            sliderOnChange = { onSelect(EdgeZoneAction.SetCustomDpi(it)) }
                        }
                        is EdgeZoneAction.SetCustomScrollSpeed -> {
                            sliderMultiplier = current.multiplier
                            sliderOnChange = { onSelect(EdgeZoneAction.SetCustomScrollSpeed(it)) }
                        }
                        else -> { sliderMultiplier = null; sliderOnChange = null }
                    }
                    // exit 애니메이션 중에도 콘텐츠가 렌더링되도록 마지막 유효 값 보존
                    var lastSliderMultiplier by remember { mutableStateOf(1.0f) }
                    var lastSliderOnChange: ((Float) -> Unit)? by remember { mutableStateOf(null) }
                    if (sliderMultiplier != null && sliderOnChange != null) {
                        lastSliderMultiplier = sliderMultiplier
                        lastSliderOnChange = sliderOnChange
                    }
                    AnimatedVisibility(
                        visible = (current is EdgeZoneAction.SetCustomDpi && pathKeyStack.lastOrNull() == "DOMAIN:${ActionDomain.DPI.name}") ||
                            (current is EdgeZoneAction.SetCustomScrollSpeed && pathKeyStack.lastOrNull() == "DOMAIN:${ActionDomain.SCROLL_SPEED.name}"),
                        enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                        exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
                    ) {
                        val displayMultiplier = lastSliderMultiplier
                        val displayOnChange = lastSliderOnChange ?: return@AnimatedVisibility
                        val swipeController = LocalSwipeFocusController.current
                        SwipeFocusable(
                            element = EdgeEditorElement.CustomMultiplierSlider,
                            shape = RoundedCornerShape(8.dp),
                            showBorderHighlight = true,
                            manipulatable = true,
                            onManipulate = { deltaPx, screenWidthPx ->
                                val rangeSpan = 5.0f - 0.1f
                                val deltaValue = (deltaPx / screenWidthPx) * rangeSpan
                                val newValue = (displayMultiplier + deltaValue).coerceIn(0.1f, 5.0f)
                                displayOnChange((newValue * 10f).roundToInt() / 10f)
                            },
                            gridRow = 35,
                        ) {
                        val isFocused = LocalSwipeFocused.current
                        val inManip = isFocused && swipeController?.mode == SwipeMode.MANIPULATION
                        // animateDpAsState를 isFocused/inManip과 같은 레벨에 선언 — mode 변경 시 즉시 리컴포지션
                        val lineColor = if (isFocused || inManip) Color.White else Color.White.copy(alpha = 0.7f)
                        val lineWidthDp by animateDpAsState(
                            targetValue = when {
                                inManip -> 6.dp
                                isFocused -> 4.dp
                                else -> CUSTOM_SLIDER_LINE_WIDTH_DP.dp
                            },
                            label = "sliderLineWidth",
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
                                    .pointerInput(displayOnChange) {
                                        awaitEachGesture {
                                            val down = awaitFirstDown(requireUnconsumed = false)
                                            down.consume()
                                            fun applyX(x: Float) {
                                                val fr = (x / size.width).coerceIn(0f, 1f)
                                                val v = 0.1f + fr * (5.0f - 0.1f)
                                                displayOnChange((v * 10f).roundToInt() / 10f)
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
                                val thumbFraction = ((displayMultiplier - 0.1f) / (5.0f - 0.1f)).coerceIn(0f, 1f)
                                // 배경 (미채움 구간) — 포커스/조작 상태에 따라 색상 변화
                                val trackBgColor = when {
                                    inManip -> cs.primaryContainer.copy(alpha = 0.5f)
                                    isFocused -> cs.primaryContainer.copy(alpha = 0.25f)
                                    else -> cs.surfaceVariant
                                }
                                Box(Modifier.matchParentSize().background(trackBgColor))
                                // 채움 구간 (primary)
                                Box(
                                    Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(thumbFraction)
                                        .background(cs.primary)
                                )
                                // 손잡이 (흰 세로 구분선)
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
                                        .background(lineColor)
                                )
                            }
                            Text(
                                "×${"%.1f".format(displayMultiplier)}",
                                fontSize = 13.sp,
                                color = cs.onSurface,
                                modifier = Modifier.width(40.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.End
                            )
                        }
                        }
                    }

            }
        }
        } // AnimatedContent 닫기

    }

    if (shortcutPopupOpen && inputMode == InputMode.NORMAL) {
        ShortcutEditorPopup(
            draft = draftShortcut,
            inputMode = inputMode,
            onDraftChange = { draftShortcut = it },
            onCancel = {
                shortcutPopupOpen = false
                editingShortcutPreset = null
            },
            onConfirm = { confirmed ->
                val editing = editingShortcutPreset
                if (editing != null) {
                    onEditCustomShortcutConfirm?.invoke(editing, confirmed)
                    editingShortcutPreset = null
                } else {
                    onSelect(confirmed)
                }
                shortcutPopupOpen = false
            },
            onAddAsCandidate = if (onAddAsCandidate != null && editingShortcutPreset == null) {
                { iconKey, name ->
                    onAddAsCandidate(draftShortcut, iconKey, name)
                    shortcutPopupOpen = false
                }
            } else null,
        )
    }

}


@Composable
private fun _RemovedPlaceholder_PresetEditDeleteMenu_UNUSED(
    targetLabel: String,
    inputMode: InputMode,
    swipeController: SwipeFocusController? = null,
    swipeScope: Any = EdgeEditorScope.CustomPresetMenu,
    returnFocusKey: String? = null,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    var confirmingDelete by remember { mutableStateOf(false) }

    // SWIPE 모드: 진입 시 scope 전환, 종료 시 복귀
    if (inputMode == InputMode.SWIPE && swipeController != null) {
        DisposableEffect(Unit) {
            swipeController.pushScope(swipeScope)
            swipeController.setFocus(EdgeEditorElement.CustomMenuEdit)
            onDispose {
                swipeController.popScope()
                if (returnFocusKey != null) {
                    swipeController.setFocus(EdgeEditorElement.ActionOptionCard(returnFocusKey))
                }
            }
        }
    }

    // 반투명 dim 배경 + 중앙 카드
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f)) // 기본값: 0.45f
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.75f) // 기본값: 0.75f
                .clip(RoundedCornerShape(16.dp))
                .background(cs.surface)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}, // 카드 내부 탭은 dim 닫기 차단
                )
                .padding(20.dp), // 기본값: 20.dp
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 헤더
                Text(
                    targetLabel,
                    fontSize = 14.sp, // 기본값: 14.sp
                    fontWeight = FontWeight.Bold,
                    color = cs.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                HorizontalDivider(color = cs.outline.copy(alpha = 0.2f))

                if (!confirmingDelete) {
                    // 수정 버튼
                    val editFocusable = @Composable { content: @Composable () -> Unit ->
                        if (inputMode == InputMode.SWIPE && swipeController != null) {
                            SwipeFocusable(
                                element = EdgeEditorElement.CustomMenuEdit,
                                scope = swipeScope,
                                shape = RoundedCornerShape(8.dp),
                                showBorderHighlight = true,
                                onActivate = onEdit,
                                gridRow = 0,
                                modifier = Modifier.fillMaxWidth(),
                            ) { content() }
                        } else {
                            Box(modifier = Modifier.fillMaxWidth()) { content() }
                        }
                    }
                    editFocusable {
                        FilledTonalButton(
                            onClick = onEdit,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                        ) { Text("수정") }
                    }

                    // 삭제 버튼
                    val deleteFocusable = @Composable { content: @Composable () -> Unit ->
                        if (inputMode == InputMode.SWIPE && swipeController != null) {
                            SwipeFocusable(
                                element = EdgeEditorElement.CustomMenuDelete,
                                scope = swipeScope,
                                shape = RoundedCornerShape(8.dp),
                                showBorderHighlight = true,
                                onActivate = { confirmingDelete = true; swipeController.setFocus(EdgeEditorElement.CustomMenuDeleteCancel) },
                                gridRow = 1,
                                modifier = Modifier.fillMaxWidth(),
                            ) { content() }
                        } else {
                            Box(modifier = Modifier.fillMaxWidth()) { content() }
                        }
                    }
                    deleteFocusable {
                        FilledTonalButton(
                            onClick = { confirmingDelete = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = cs.error.copy(alpha = 0.12f),
                                contentColor = cs.error,
                            ),
                        ) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("삭제")
                        }
                    }
                } else {
                    // 삭제 확인 단계
                    Text(
                        "삭제하면 되돌릴 수 없습니다.",
                        fontSize = 12.sp, // 기본값: 12.sp
                        color = cs.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // 취소
                        val cancelFocusable = @Composable { content: @Composable () -> Unit ->
                            if (inputMode == InputMode.SWIPE && swipeController != null) {
                                SwipeFocusable(
                                    element = EdgeEditorElement.CustomMenuDeleteCancel,
                                    scope = swipeScope,
                                    shape = RoundedCornerShape(8.dp),
                                    showBorderHighlight = true,
                                    onActivate = { confirmingDelete = false; swipeController.setFocus(EdgeEditorElement.CustomMenuEdit) },
                                    gridRow = 0,
                                    gridCol = 0,
                                    modifier = Modifier.weight(1f),
                                ) { content() }
                            } else {
                                Box(modifier = Modifier.weight(1f)) { content() }
                            }
                        }
                        cancelFocusable {
                            FilledTonalButton(
                                onClick = { confirmingDelete = false },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                            ) { Text("취소") }
                        }
                        // 확인(삭제)
                        val confirmFocusable = @Composable { content: @Composable () -> Unit ->
                            if (inputMode == InputMode.SWIPE && swipeController != null) {
                                SwipeFocusable(
                                    element = EdgeEditorElement.CustomMenuDeleteConfirm,
                                    scope = swipeScope,
                                    shape = RoundedCornerShape(8.dp),
                                    showBorderHighlight = true,
                                    onActivate = onDelete,
                                    gridRow = 0,
                                    gridCol = 1,
                                    modifier = Modifier.weight(1f),
                                ) { content() }
                            } else {
                                Box(modifier = Modifier.weight(1f)) { content() }
                            }
                        }
                        confirmFocusable {
                            FilledTonalButton(
                                onClick = onDelete,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = cs.error,
                                    contentColor = cs.onError,
                                ),
                            ) { Text("삭제 확인") }
                        }
                    }
                }
            }
        }
    }
}

// Phase 4.7.5-A: migrateDynamicsIndicesAfterDelete → EdgeZoneActionResolver.kt로 이관

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShortcutEditorPopup(
    draft: EdgeZoneAction.SendShortcut,
    inputMode: InputMode,
    onDraftChange: (EdgeZoneAction.SendShortcut) -> Unit,
    onCancel: () -> Unit,
    onConfirm: (EdgeZoneAction.SendShortcut) -> Unit,
    onAddAsCandidate: ((iconKey: String, name: String) -> Unit)?,
    onNameKeyboardActiveChange: (Boolean) -> Unit = {},
    onRequestIconPicker: ((current: String, anchorCenter: androidx.compose.ui.geometry.Offset, onResult: (String) -> Unit) -> Unit)? = null,
) {
    val cs = MaterialTheme.colorScheme
    val swipeController = LocalSwipeFocusController.current

    var draftIconKey by remember { mutableStateOf("") }
    // 사용자가 직접 입력한 액션명만 보유 (빈 값이면 키 조합 미리보기를 placeholder로 표시). 기본값: ""
    var draftName by remember { mutableStateOf("") }
    // 현재 선택된 키 조합의 미리보기 문자열 (예: "H", "Ctrl+Shift+H"). 키가 없으면 "".
    // MutableState로 선언해야 SwipeFocusable onActivate 람다에서 호출 시점 값을 읽음 (plain val은 stale capture됨).
    var previewName by remember { mutableStateOf("") }
    previewName = if (draft.modifierBits == 0 && draft.keyCodes.isEmpty()) ""
        else formatShortcutCombo(draft.modifierBits, draft.keyCodes)
    // 액션명 입력 키보드 활성 여부 (SWIPE 전용). true 시 팝업 카드 바로 아래에 키보드가 확장됨. 기본값: false
    var nameKeyboardActive by remember { mutableStateOf(false) }
    LaunchedEffect(nameKeyboardActive) { onNameKeyboardActiveChange(nameKeyboardActive) }
    var normalIconSheetVisible by remember { mutableStateOf(false) }
    val normalIconSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var iconBtnCenter by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    val context = LocalContext.current
    // 단일 키 / 단축키 모드 (팝업 내부 토글로 전환).
    // draft가 실제 combo(모디파이어 있음 또는 키 2개+)이면 강제로 단축키 모드,
    // 그 외(빈 draft·단일 키)는 마지막 사용 모드 복원. 기본값: false (단축키 모드)
    var singleKeyMode by remember {
        val isComboContent = draft.modifierBits != 0 || draft.keyCodes.size > 1
        mutableStateOf(if (isComboContent) false else loadLastShortcutSingleKeyMode(context))
    }

    // ── 실제 풀사이즈 키보드 레이아웃 ──
    // 각 행: KeyDef(label, hidCode, widthWeight, modBit, modSecondary). modBit!=0은 수정자 키.
    // 모든 행의 총 weight를 15.0으로 통일 → 비율 일치.
    data class KeyDef(val label: String, val code: Int, val weight: Float = 1f, val modBit: Int = 0, val modSecondary: Boolean = false)
    fun spacer(w: Float) = KeyDef("", 0, w)
    fun modifier(label: String, bit: Int, weight: Float = 1f, secondary: Boolean = false) = KeyDef(label, 0, weight, bit, secondary)

    val kbRows = listOf(
        // Fn 행 (총 13키 → 각 1.0, Esc 약간 좁게 표현)
        listOf(
            KeyDef("Esc", 0x29, 1.0f),
            spacer(0.5f),
            KeyDef("F1", 0x3A), KeyDef("F2", 0x3B), KeyDef("F3", 0x3C), KeyDef("F4", 0x3D),
            spacer(0.25f),
            KeyDef("F5", 0x3E), KeyDef("F6", 0x3F), KeyDef("F7", 0x40), KeyDef("F8", 0x41),
            spacer(0.25f),
            KeyDef("F9", 0x42), KeyDef("F10", 0x43), KeyDef("F11", 0x44), KeyDef("F12", 0x45)
        ),
        // 숫자 행 (총 weight 15: ` 1-0 - = + BkSp×2)
        listOf(
            KeyDef("`", 0x35), KeyDef("1", 0x1E), KeyDef("2", 0x1F), KeyDef("3", 0x20),
            KeyDef("4", 0x21), KeyDef("5", 0x22), KeyDef("6", 0x23), KeyDef("7", 0x24),
            KeyDef("8", 0x25), KeyDef("9", 0x26), KeyDef("0", 0x27),
            KeyDef("-", 0x2D), KeyDef("=", 0x2E),
            KeyDef("BkSp", 0x2A, 2f)
        ),
        // QWERTY 행 (총 weight 15: Tab×1.5 + Q-] + \×1.5)
        listOf(
            KeyDef("Tab", 0x2B, 1.5f),
            KeyDef("Q", 0x14), KeyDef("W", 0x1A), KeyDef("E", 0x08), KeyDef("R", 0x15),
            KeyDef("T", 0x17), KeyDef("Y", 0x1C), KeyDef("U", 0x18), KeyDef("I", 0x0C),
            KeyDef("O", 0x12), KeyDef("P", 0x13),
            KeyDef("[", 0x2F), KeyDef("]", 0x30),
            KeyDef("\\", 0x31, 1.5f)
        ),
        // 홈 행 (총 weight 15: CapsLk×1.75 + A-' + Enter×2.25)
        listOf(
            KeyDef("CapsLk", 0x39, 1.75f),
            KeyDef("A", 0x04), KeyDef("S", 0x16), KeyDef("D", 0x07), KeyDef("F", 0x09),
            KeyDef("G", 0x0A), KeyDef("H", 0x0B), KeyDef("J", 0x0D), KeyDef("K", 0x0E),
            KeyDef("L", 0x0F), KeyDef(";", 0x33), KeyDef("'", 0x34),
            KeyDef("Enter", 0x28, 2.25f)
        ),
        // ZXCV 행 (총 weight 15: Shift×2.5 + Z-/ + Shift×2.5)
        listOf(
            modifier("Shift", MOD_BIT_LSHIFT.toInt(), 2.5f),
            KeyDef("Z", 0x1D), KeyDef("X", 0x1B), KeyDef("C", 0x06), KeyDef("V", 0x19),
            KeyDef("B", 0x05), KeyDef("N", 0x11), KeyDef("M", 0x10),
            KeyDef(",", 0x36), KeyDef(".", 0x37), KeyDef("/", 0x38),
            modifier("Shift", MOD_BIT_LSHIFT.toInt(), 2.5f, secondary = true)
        ),
        // 하단 행 (총 weight 15: Ctrl+Win+Alt + Space + Alt+Win+Ctrl)
        listOf(
            modifier("Ctrl", MOD_BIT_LCTRL.toInt(), 1.5f),
            modifier("Win", MOD_BIT_LGUI.toInt(), 1.25f),
            modifier("Alt", MOD_BIT_LALT.toInt(), 1.25f),
            KeyDef("Space", 0x2C, 7f),
            modifier("Alt", MOD_BIT_LALT.toInt(), 1.25f, secondary = true),
            modifier("Win", MOD_BIT_LGUI.toInt(), 1.25f, secondary = true),
            modifier("Ctrl", MOD_BIT_LCTRL.toInt(), 1.5f, secondary = true),
        )
    )
    // 내비게이션 클러스터: 2행 (3×2 블록 + 인버티드-T 화살표 나란히)
    // 총 weight 6.5 (3 + 0.5 + 3) — 두 행 동일하여 열 정렬 보장
    val navRows = listOf(
        listOf(
            KeyDef("Ins", 0x49), KeyDef("Home", 0x4A), KeyDef("PgUp", 0x4B),
            spacer(0.5f),
            spacer(1f), KeyDef("↑", 0x52), spacer(1f)
        ),
        listOf(
            KeyDef("Del", 0x4C), KeyDef("End", 0x4D), KeyDef("PgDn", 0x4E),
            spacer(0.5f),
            KeyDef("←", 0x50), KeyDef("↓", 0x51), KeyDef("→", 0x4F)
        )
    )

    // ── 팝업 컨텐츠 빌더 ──
    @Composable
    fun KeyCell(def: KeyDef, rowIndex: Int, weightMod: Modifier) {
        if (def.code == 0 && def.modBit == 0) { Spacer(weightMod); return }
        val isModifier = def.modBit != 0
        // 단일 키 모드에서 modifier 키는 비활성 렌더만 표시
        val disabled = singleKeyMode && isModifier
        val active = if (isModifier) draft.modifierBits and def.modBit != 0 else def.code in draft.keyCodes
        val bgColor = { focused: Boolean ->
            when {
                disabled -> cs.surface
                active -> cs.primary
                focused -> cs.primary.copy(alpha = 0.25f)
                else -> cs.surface
            }
        }
        val textColor = { focused: Boolean ->
            when {
                disabled -> cs.onSurface.copy(alpha = 0.3f)
                active -> cs.onPrimary
                focused -> cs.primary
                else -> cs.onSurface
            }
        }
        val onTap = {
            if (isModifier) {
                onDraftChange(draft.copy(modifierBits = draft.modifierBits xor def.modBit, presetLabel = ""))
            } else {
                val newKeyCodes = if (singleKeyMode) {
                    // 단일 키 모드: 이미 선택된 키 재탭 시 해제, 새 키 탭 시 교체
                    if (def.code in draft.keyCodes) emptyList() else listOf(def.code)
                } else {
                    when {
                        def.code in draft.keyCodes -> draft.keyCodes - def.code   // 재탭 해제
                        draft.keyCodes.isNotEmpty() -> {                           // 두 번째 일반 키 거부
                            ToastController.show("단축키는 보조키와 일반 키 1개만 조합할 수 있습니다", ToastType.ERROR)
                            draft.keyCodes
                        }
                        else -> listOf(def.code)
                    }
                }
                onDraftChange(draft.copy(keyCodes = newKeyCodes, presetLabel = ""))
            }
        }
        val element: EdgeEditorElement = if (isModifier) EdgeEditorElement.ShortcutModifier(def.modBit, def.modSecondary)
            else EdgeEditorElement.ShortcutKey(def.code)
        if (inputMode == InputMode.SWIPE) {
            if (disabled) {
                // 단일 키 모드: modifier는 SwipeFocusable 없이 비활성 Box만 렌더 (스와이프 포커스가 자동으로 건너뜀)
                Box(
                    modifier = weightMod
                        .padding(horizontal = 1.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(bgColor(false))
                        .padding(vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) { Text(def.label, fontSize = 10.sp, color = textColor(false), maxLines = 1) }
            } else {
                SwipeFocusable(
                    element = element,
                    scope = EdgeEditorScope.ShortcutPopup,
                    shape = RoundedCornerShape(3.dp),
                    showBorderHighlight = false,
                    onActivate = onTap,
                    gridRow = rowIndex,
                    modifier = weightMod,
                ) {
                    val focused = LocalSwipeFocused.current
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 1.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .then(if (focused) Modifier.border(1.dp, Color.White, RoundedCornerShape(3.dp)) else Modifier)
                            .background(bgColor(focused))
                            .padding(vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) { Text(def.label, fontSize = 10.sp, color = textColor(focused), maxLines = 1) }
                }
            }
        } else {
            Box(
                modifier = weightMod
                    .padding(horizontal = 1.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(bgColor(false))
                    .then(if (!disabled) Modifier.clickable(onClick = onTap) else Modifier)
                    .padding(vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) { Text(def.label, fontSize = 10.sp, color = textColor(false), maxLines = 1) }
        }
    }

    @Composable
    fun PopupContent() {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            shape = RoundedCornerShape(12.dp),
            color = cs.surfaceVariant,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    if (singleKeyMode) "키 입력 설정" else "단축키 설정",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = cs.onSurface,
                )

                // ── 단일 키 / 단축키 모드 토글 ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(cs.surface),
                ) {
                    listOf(false to "단일 키", true to "단축키").forEach { (isCombo, label) ->
                        val selected = if (isCombo) !singleKeyMode else singleKeyMode
                        val element = if (isCombo) EdgeEditorElement.ShortcutModeCombo else EdgeEditorElement.ShortcutModeSingleKey
                        val onToggle: () -> Unit = {
                            val newSingle = !isCombo
                            if (singleKeyMode != newSingle) {
                                singleKeyMode = newSingle
                                if (newSingle) onDraftChange(draft.copy(modifierBits = 0, keyCodes = draft.keyCodes.take(1)))
                            }
                        }
                        if (inputMode == InputMode.SWIPE) {
                            SwipeFocusable(
                                element = element,
                                scope = EdgeEditorScope.ShortcutPopup,
                                shape = RoundedCornerShape(6.dp),
                                showBorderHighlight = false,
                                gridRow = 0,
                                onActivate = onToggle,
                                modifier = Modifier.weight(1f),
                            ) {
                                val focused = LocalSwipeFocused.current
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            when {
                                                selected -> cs.primary
                                                focused -> cs.primary.copy(alpha = 0.15f)
                                                else -> Color.Transparent
                                            }
                                        )
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(label, fontSize = 12.sp, color = if (selected) cs.onPrimary else cs.onSurface)
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (selected) cs.primary else Color.Transparent)
                                    .clickable(onClick = onToggle)
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(label, fontSize = 12.sp, color = if (selected) cs.onPrimary else cs.onSurface)
                            }
                        }
                    }
                }

                // ── 현재 조합 시각화 ──
                val keyLabelMap: Map<Int, String> = remember {
                    (kbRows + navRows).flatten()
                        .filter { it.code != 0 && it.modBit == 0 }
                        .associate { it.code to it.label }
                }
                val modifierDefs = listOf(0x01 to "Ctrl", 0x02 to "Shift", 0x04 to "Alt", 0x08 to "Win")
                val comboParts: List<String> = buildList {
                    for ((bit, label) in modifierDefs) {
                        if (draft.modifierBits and bit != 0) add(label)
                    }
                    for (code in draft.keyCodes) {
                        keyLabelMap[code]?.let { add(it) }
                    }
                }

                // ── 콤보 칩 시각화 ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(cs.surface),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (comboParts.isEmpty()) {
                            Text(
                                if (singleKeyMode) "키를 눌러 설정하세요" else "키를 눌러 단축키를 설정하세요",
                                fontSize = 12.sp,
                                color = cs.onSurface.copy(alpha = 0.35f)
                            )
                        } else {
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                comboParts.forEachIndexed { i, label ->
                                    if (i > 0) {
                                        Text(
                                            "+",
                                            fontSize = 11.sp,
                                            color = cs.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .border(1.dp, cs.outline.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                            .background(cs.surfaceVariant)
                                            .padding(horizontal = 7.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            label,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = cs.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ── 풀사이즈 키보드 ──
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    kbRows.forEachIndexed { rowIdx, row ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            row.forEach { key -> KeyCell(key, rowIdx + 1, Modifier.weight(key.weight)) }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    // 내비게이션 클러스터
                    navRows.forEachIndexed { rowIdx, row ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            row.forEach { key -> KeyCell(key, 10 + rowIdx, Modifier.weight(key.weight)) }
                        }
                    }
                }

                HorizontalDivider(color = cs.outline.copy(alpha = 0.2f))

                // ── 아이콘 + 액션명 입력 행 (새 액션으로 추가 컨텍스트에서만 표시) ──
                // 키보드 아래 · 버튼 바 위에 배치 → 스와이프로 키보드 마지막 행에서 자연스럽게 접근 가능
                if (onAddAsCandidate != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // 아이콘 박스 48dp (표시설정 IconBox 패턴)
                        if (inputMode == InputMode.SWIPE) {
                            SwipeFocusable(
                                element = EdgeEditorElement.ShortcutIconButton,
                                scope = EdgeEditorScope.ShortcutPopup,
                                shape = RoundedCornerShape(8.dp),
                                showBorderHighlight = true,
                                onActivate = {
                                    onRequestIconPicker?.invoke(draftIconKey, iconBtnCenter) { draftIconKey = it }
                                },
                                gridRow = 79,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(cs.surface)
                                        .border(0.5.dp, cs.outline.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                        .onGloballyPositioned { coords ->
                                            val bounds = coords.boundsInWindow()
                                            iconBtnCenter = bounds.center
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (draftIconKey.isNotEmpty()) IconRegistry.get(draftIconKey) else Icons.Filled.Keyboard,
                                        contentDescription = "아이콘 선택",
                                        modifier = Modifier.size(if (draftIconKey.isNotEmpty()) 22.dp else 18.dp),
                                        tint = if (draftIconKey.isNotEmpty()) cs.onSurface else cs.onSurface.copy(alpha = 0.35f),
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(cs.surface)
                                    .border(0.5.dp, cs.outline.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .onGloballyPositioned { coords ->
                                        val bounds = coords.boundsInWindow()
                                        iconBtnCenter = bounds.center
                                    }
                                    .clickable { normalIconSheetVisible = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (draftIconKey.isNotEmpty()) IconRegistry.get(draftIconKey) else Icons.Filled.Keyboard,
                                    contentDescription = "아이콘 선택",
                                    modifier = Modifier.size(if (draftIconKey.isNotEmpty()) 22.dp else 18.dp),
                                    tint = if (draftIconKey.isNotEmpty()) cs.onSurface else cs.onSurface.copy(alpha = 0.35f),
                                )
                            }
                        }
                        // 액션명 필드 박스 48dp height (표시설정 LabelBox 패턴)
                        if (inputMode == InputMode.SWIPE) {
                            SwipeFocusable(
                                element = EdgeEditorElement.ShortcutNameField,
                                scope = EdgeEditorScope.ShortcutPopup,
                                shape = RoundedCornerShape(8.dp),
                                showBorderHighlight = true,
                                onActivate = { nameKeyboardActive = true },
                                gridRow = 79,
                                modifier = Modifier.weight(1f),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(cs.surface)
                                        .border(0.5.dp, cs.outline.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp),
                                    contentAlignment = Alignment.CenterStart,
                                ) {
                                    val caretTransition = rememberInfiniteTransition(label = "caret")
                                    val caretAlpha by caretTransition.animateFloat(
                                        initialValue = 1f, targetValue = 0f,
                                        animationSpec = infiniteRepeatable(
                                            animation = keyframes {
                                                durationMillis = 1000
                                                1f at 0
                                                1f at 500
                                                0f at 501
                                                0f at 1000
                                            },
                                            repeatMode = RepeatMode.Restart
                                        ),
                                        label = "caretAlpha"
                                    )
                                    if (draftName.isEmpty() && !nameKeyboardActive) {
                                        Text(
                                            text = previewName.ifEmpty { "액션명 입력..." },
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                            color = cs.onSurfaceVariant.copy(alpha = 0.5f),
                                        )
                                    } else {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = draftName,
                                                fontSize = 14.sp,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                                color = cs.onSurface,
                                                modifier = Modifier.weight(1f, fill = false),
                                            )
                                            if (nameKeyboardActive) {
                                                Text(
                                                    text = "|",
                                                    fontSize = 14.sp,
                                                    color = cs.primary.copy(alpha = caretAlpha),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            BasicTextField(
                                value = draftName,
                                onValueChange = { if (it.length <= 32) draftName = it },
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontSize = 14.sp,
                                    color = cs.onSurface,
                                ),
                                cursorBrush = SolidColor(cs.primary),
                                modifier = Modifier.weight(1f),
                                decorationBox = { innerTextField ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(cs.surface)
                                            .border(0.5.dp, cs.outline.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 12.dp),
                                        contentAlignment = Alignment.CenterStart,
                                    ) {
                                        if (draftName.isEmpty()) {
                                            Text(
                                                previewName.ifEmpty { "액션명 입력..." },
                                                fontSize = 14.sp,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                                color = cs.onSurfaceVariant.copy(alpha = 0.5f),
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                        }
                    }
                }

                // ── 버튼 바: 취소 / 확인 / 새 액션으로 추가 ──
                // 단축키 모드 유효 조건: 보조키 1개 이상 + 일반 키 정확히 1개
                val shortcutComboError: () -> String? = {
                    if (singleKeyMode) null
                    else when {
                        draft.modifierBits == 0 && draft.keyCodes.size == 1 ->
                            "단축키 모드에서는 단일 키를 할당할 수 없습니다"
                        draft.modifierBits != 0 && draft.keyCodes.isEmpty() ->
                            "일반 키를 하나 선택해주세요"
                        draft.keyCodes.size > 1 ->
                            "단축키는 보조키와 일반 키 1개만 조합할 수 있습니다"
                        else -> null
                    }
                }
                val onConfirmGuarded: () -> Unit = {
                    val err = shortcutComboError()
                    if (err != null) {
                        ToastController.show(err, ToastType.ERROR)
                    } else {
                        saveLastShortcutSingleKeyMode(context, singleKeyMode)
                        onConfirm(draft)
                    }
                }
                val onAddGuarded: () -> Unit = {
                    val finalName = draftName.trim().ifBlank { previewName }
                    val err = shortcutComboError()
                    when {
                        err != null ->
                            ToastController.show(err, ToastType.ERROR)
                        finalName.isBlank() ->
                            ToastController.show("키를 선택하거나 액션명을 입력해주세요", ToastType.WARNING)
                        else -> {
                            saveLastShortcutSingleKeyMode(context, singleKeyMode)
                            onAddAsCandidate?.invoke(draftIconKey, finalName)
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (inputMode == InputMode.SWIPE) {
                        SwipeFocusable(
                            element = EdgeEditorElement.ShortcutPopupCancel,
                            scope = EdgeEditorScope.ShortcutPopup,
                            shape = RoundedCornerShape(8.dp),
                            showBorderHighlight = false,
                            onActivate = onCancel,
                            gridRow = 80,
                            modifier = Modifier.weight(1f),
                        ) {
                            val focused = LocalSwipeFocused.current
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (focused) cs.error else Color.Transparent)
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "취소",
                                    fontSize = 13.sp,
                                    color = if (focused) cs.onError else cs.error,
                                )
                            }
                        }
                        SwipeFocusable(
                            element = EdgeEditorElement.ShortcutPopupConfirm,
                            scope = EdgeEditorScope.ShortcutPopup,
                            shape = RoundedCornerShape(8.dp),
                            showBorderHighlight = false,
                            onActivate = onConfirmGuarded,
                            gridRow = 80,
                            modifier = Modifier.weight(1f),
                        ) {
                            val focused = LocalSwipeFocused.current
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (focused) cs.primary else Color.Transparent)
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "확인",
                                    fontSize = 13.sp,
                                    color = if (focused) cs.onPrimary else cs.primary,
                                )
                            }
                        }
                        if (onAddAsCandidate != null) {
                            SwipeFocusable(
                                element = EdgeEditorElement.ShortcutPopupAddCandidate,
                                scope = EdgeEditorScope.ShortcutPopup,
                                shape = RoundedCornerShape(8.dp),
                                showBorderHighlight = false,
                                onActivate = onAddGuarded,
                                gridRow = 80,
                                modifier = Modifier.weight(2f),
                            ) {
                                val focused = LocalSwipeFocused.current
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (focused) cs.secondary else Color.Transparent)
                                        .padding(horizontal = 8.dp, vertical = 10.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        "새 액션으로 추가",
                                        fontSize = 13.sp,
                                        color = if (focused) cs.onSecondary else cs.secondary,
                                    )
                                }
                            }
                        }
                    } else {
                        TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                            Text("취소", fontSize = 13.sp)
                        }
                        Button(
                            onClick = onConfirmGuarded,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                        ) { Text("확인", fontSize = 13.sp) }
                        if (onAddAsCandidate != null) {
                            FilledTonalButton(
                                onClick = onAddGuarded,
                                modifier = Modifier.weight(2f),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = cs.secondaryContainer,
                                    contentColor = cs.onSecondaryContainer,
                                )
                            ) { Text("새 액션으로 추가", fontSize = 13.sp) }
                        }
                    }
                }
            }
        }
    }

    if (inputMode == InputMode.SWIPE) {
        // 인라인 Box 렌더링 — 호출자가 루트 Box에 배치하므로 fillMaxSize가 전체 화면을 채움
        // Popup 사용 금지: 별도 Android Window 생성으로 SwipeGestureLayer 터치 도달 불가
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
        ) {
            val screenH = maxHeight
            var popupCardHeightDp by remember { mutableStateOf(0.dp) }
            var kbActualHeightDp by remember { mutableStateOf(0.dp) }
            val density = LocalDensity.current

            // 팝업 카드 bottom이 키보드 top 바로 위에 오도록 offset 계산.
            // 측정 오차 보정: kbEffectiveH에 16dp 추가해 팝업이 키보드와 겹치지 않도록 함.
            val kbEffectiveH = (if (kbActualHeightDp > 0.dp) kbActualHeightDp
                               else EdgeSwipeConstants.EDGE_ZONE_LABEL_KEYBOARD_VISUAL_HEIGHT_DP.dp) + 16.dp
            val targetOffsetDp = if (popupCardHeightDp > 0.dp && nameKeyboardActive) {
                val kbTop = screenH - kbEffectiveH
                val popupBottomNoOffset = screenH / 2 + popupCardHeightDp / 2
                (kbTop - popupBottomNoOffset).coerceAtMost(0.dp)
            } else 0.dp
            val shortcutKbOffsetY by animateDpAsState(
                targetValue = targetOffsetDp,
                animationSpec = tween(220),
                label = "shortcutKbOffsetY",
            )

            // SwipeKeyboardOverlay는 항상 composition에 있음.
            // gestureFullHeight=true + overlay로 팝업 카드를 자식에 배치 →
            // 루트 Box의 pointerInput(parent)이 팝업 카드 영역 터치도 수신하여 제스처 감지.
            com.bridgeone.app.ui.components.SwipeKeyboardOverlay(
                initialText = draftName,
                maxLength = 32,
                suggestions = emptyList(),
                revertOnCancel = false,
                showScrim = false,
                gestureFullHeight = true,
                showGuide = false,
                showKeyboard = nameKeyboardActive,
                onTextChange = { draftName = it },
                onCancel = { nameKeyboardActive = false },
                onDone = { result -> draftName = result; nameKeyboardActive = false },
                onContentHeightMeasured = { px ->
                    with(density) { kbActualHeightDp = px.toDp() }
                },
                overlay = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(y = shortcutKbOffsetY),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier.onGloballyPositioned { coords ->
                                with(density) { popupCardHeightDp = coords.size.height.toDp() }
                            }
                        ) {
                            PopupContent()
                            if (nameKeyboardActive) {
                                Box(modifier = Modifier.matchParentSize().pointerInput(Unit) {
                                    awaitEachGesture {
                                        awaitFirstDown(requireUnconsumed = false).consume()
                                        while (true) {
                                            val e = awaitPointerEvent()
                                            e.changes.forEach { it.consume() }
                                            if (e.type == androidx.compose.ui.input.pointer.PointerEventType.Release) break
                                        }
                                    }
                                })
                            }
                        }
                    }
                },
            )
        }
    } else {
        Popup(
            alignment = Alignment.Center,
            properties = PopupProperties(focusable = true, dismissOnClickOutside = true),
            onDismissRequest = onCancel,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    ) { onCancel() },
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.clickable(
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                ) { /* consume click — don't dismiss */ }) {
                    PopupContent()
                }
            }
        }
        // NORMAL 모드 아이콘 선택 바텀시트
        if (normalIconSheetVisible && onAddAsCandidate != null) {
            NormalCategoryIconSheet(
                selectedIconKey = draftIconKey,
                sheetState = normalIconSheetState,
                onPick = { key ->
                    draftIconKey = key
                    normalIconSheetVisible = false
                },
                onDismiss = { normalIconSheetVisible = false },
            )
        }
    }
}

// ============================================================
// 매크로 편집기 팝업
// ============================================================

/**
 * 매크로 딜레이 커스텀 슬라이더 (속도 칩 슬라이더와 동일한 비주얼).
 * BoxWithConstraints + pointerInput 트랙, primary 채움, 흰 세로선 손잡이, 우측 ms 라벨.
 */
@Composable
private fun MacroDelaySliderRow(
    value: Int,
    onValueChange: (Int) -> Unit,
    element: EdgeEditorElement,
    inputMode: InputMode,
    gridRow: Int,
    gridCol: Int? = null,
) {
    val cs = MaterialTheme.colorScheme
    val swipeController = LocalSwipeFocusController.current
    val minMs = com.bridgeone.app.ui.common.MACRO_STEP_DELAY_MIN_MS.toFloat()
    val maxMs = com.bridgeone.app.ui.common.MACRO_STEP_DELAY_MAX_MS.toFloat()

    val content: @Composable (Boolean) -> Unit = { isFocused ->
        val inManip = isFocused && swipeController?.mode == SwipeMode.MANIPULATION
        val lineColor = if (isFocused || inManip) Color.White else Color.White.copy(alpha = 0.7f)
        val lineWidthDp by animateDpAsState(
            targetValue = when {
                inManip -> 6.dp
                isFocused -> 4.dp
                else -> CUSTOM_SLIDER_LINE_WIDTH_DP.dp
            },
            label = "macroDelayLineWidth",
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 2.dp, end = 6.dp, top = 2.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .height(CUSTOM_SLIDER_TRACK_HEIGHT_DP.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .pointerInput(onValueChange) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()
                            fun applyX(x: Float) {
                                val fr = (x / size.width).coerceIn(0f, 1f)
                                val v = minMs + fr * (maxMs - minMs)
                                onValueChange(v.roundToInt())
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
                val thumbFraction = ((value - minMs) / (maxMs - minMs)).coerceIn(0f, 1f)
                val trackBgColor = when {
                    inManip -> cs.primaryContainer.copy(alpha = 0.5f)
                    isFocused -> cs.primaryContainer.copy(alpha = 0.25f)
                    else -> cs.primaryContainer.copy(alpha = 0.12f)
                }
                Box(Modifier.matchParentSize().background(trackBgColor))
                Box(
                    Modifier.fillMaxHeight().fillMaxWidth(thumbFraction).background(cs.primary)
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
                        .background(lineColor)
                )
            }
            Text(
                "${value}ms",
                fontSize = 13.sp,
                color = cs.onSurface,
                modifier = Modifier.width(44.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
            )
        }
    }

    if (inputMode == InputMode.SWIPE) {
        SwipeFocusable(
            element = element,
            scope = EdgeEditorScope.MacroPopup,
            shape = RoundedCornerShape(8.dp),
            showBorderHighlight = true,
            manipulatable = true,
            onManipulate = { deltaPx, screenWidthPx ->
                val rangeSpan = maxMs - minMs
                val deltaValue = (deltaPx / screenWidthPx) * rangeSpan
                val newValue = (value.toFloat() + deltaValue).coerceIn(minMs, maxMs)
                onValueChange(newValue.roundToInt())
            },
            gridRow = gridRow,
            gridCol = gridCol,
        ) { content(LocalSwipeFocused.current) }
    } else {
        content(false)
    }
}

/** 스텝 입력 모드. 키 모드는 SINGLE_KEY/COMBO, 문자열 일괄 추가는 TEXT, 단축키 피커는 PICK. */
private enum class MacroStepEditMode { SINGLE_KEY, COMBO, TEXT, PICK }

/** 매크로 편집기 페이지. STEPS=스텝 구성, STEP_INPUT=스텝 입력, FINALIZE=저장 설정. */
private enum class MacroEditorPage { STEPS, STEP_INPUT, FINALIZE }

/** SWIPE 모드에서 단일 SwipeKeyboardOverlay를 공유하는 텍스트 입력 대상. */
private enum class MacroKbTarget { NONE, NAME, STEP_TEXT, GROUP_NAME }

/** 매크로 편집기 내 버튼 element → 한글 레이블 (툴팁용). 없으면 null. */
private fun macroButtonLabel(element: EdgeEditorElement): String? = when (element) {
    is EdgeEditorElement.MacroStepDragHandle, is EdgeEditorElement.MacroGroupDragHandle -> "순서 이동"
    is EdgeEditorElement.MacroStepUp -> "위로"
    is EdgeEditorElement.MacroStepDown -> "아래로"
    is EdgeEditorElement.MacroStepDelayExpand -> "개별 딜레이"
    is EdgeEditorElement.MacroStepSplitMerge -> "분리/병합"
    is EdgeEditorElement.MacroStepDuplicate, is EdgeEditorElement.MacroGroupDuplicate -> "복제"
    is EdgeEditorElement.MacroStepChip -> "편집"
    is EdgeEditorElement.MacroStepDelete, is EdgeEditorElement.MacroGroupDelete -> "삭제"
    is EdgeEditorElement.MacroGroupHeader -> "폴더 접기/펼치기"
    is EdgeEditorElement.MacroGroupUngroup -> "폴더 해제"
    is EdgeEditorElement.MacroGroupUp -> "위로"
    is EdgeEditorElement.MacroGroupDown -> "아래로"
    EdgeEditorElement.MacroForceReleaseToggle -> "전체 키 강제 해제 스텝 추가"
    else -> null
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun MacroEditorPopup(
    draft: EdgeZoneAction.SendMacro,
    inputMode: InputMode,
    onDraftChange: (EdgeZoneAction.SendMacro) -> Unit,
    onCancel: () -> Unit,
    onConfirm: (draft: EdgeZoneAction.SendMacro, iconKey: String, displayName: String) -> Unit,
    onAddAsPreset: ((draft: EdgeZoneAction.SendMacro, iconKey: String, displayName: String) -> Unit)?,
    onNameKeyboardActiveChange: (Boolean) -> Unit = {},
    onRequestIconPicker: ((current: String, anchorCenter: androidx.compose.ui.geometry.Offset, onResult: (String) -> Unit) -> Unit)? = null,
    initialIconKey: String = "",
    initialName: String = "",
    customShortcutPresets: List<com.bridgeone.app.ui.common.CustomShortcutPreset> = emptyList(),
    /** NORMAL 모드 미니버튼 롱프레스 시 툴팁 요청 콜백. (text, anchorBounds) 전달. */
    onSetNormalTooltip: (String, androidx.compose.ui.geometry.Rect) -> Unit = { _, _ -> },
) {
    val cs = MaterialTheme.colorScheme
    val swipeController = LocalSwipeFocusController.current

    // 현재 페이지 (스텝 구성 / 스텝 입력 / 저장 설정). 기본값: STEPS
    var currentPage by remember { mutableStateOf(MacroEditorPage.STEPS) }
    // 현재 편집 중인 스텝 인덱스 (null이면 개요 상태, 아니면 입력 상태)
    var editingStepIndex by remember { mutableStateOf<Int?>(null) }
    var stepEditMode by remember { mutableStateOf(MacroStepEditMode.SINGLE_KEY) }
    // 마지막 사용 키 모드 (추가 시 복원). 키 모드(SINGLE_KEY/COMBO)만 추적. 기본값: SINGLE_KEY
    var lastStepMode by remember { mutableStateOf(MacroStepEditMode.SINGLE_KEY) }
    // 픽업&드롭: 들어올린 스텝 인덱스(null=비픽업). 기본값: null
    var pickedStepIndex by remember { mutableStateOf<Int?>(null) }
    // 픽업&드롭: 들어올린 폴더 그룹 ID(null=비픽업). 기본값: null
    var pickedGroupId by remember { mutableStateOf<Int?>(null) }
    var selectedStepIndices by remember { mutableStateOf<Set<Int>>(emptySet()) }
    // 범위 그룹화용 앵커 인덱스 (첫 탭=시작, 두 번째 탭=끝). 기본값: null
    var groupAnchor by remember { mutableStateOf<Int?>(null) }
    // 프리셋 조합 스테이징 스택 (확인 전까지 draft에 반영 안 됨). 기본값: emptyList()
    var pickStagedSteps by remember { mutableStateOf<List<Pair<String, MacroStep>>>(emptyList()) }
    // 접혀있는 폴더 그룹 ID 집합. 기본값: emptySet()
    var collapsedGroups by remember { mutableStateOf<Set<Int>>(emptySet()) }
    // SWIPE 모드에서 현재 폴더명을 편집 중인 그룹 ID. 기본값: null
    var editingGroupNameId by remember { mutableStateOf<Int?>(null) }
    // SWIPE 폴더명 편집 중 타이핑 버퍼 (onDone 시에만 draft에 반영). 기본값: ""
    var tempGroupName by remember { mutableStateOf("") }
    // 현재 스텝 편집용 임시 키보드 상태 (modifierBits + keyCodes + 개별 딜레이 + 반복 횟수)
    var stepDraftMod by remember { mutableIntStateOf(0) }
    var stepDraftKeys by remember { mutableStateOf<List<Int>>(emptyList()) }
    var stepDraftDelay by remember { mutableStateOf<Int?>(null) }
    var stepDraftRepeat by remember { mutableIntStateOf(1) }
    // 키 종류 팝오버: 단일 키 모드에서 키 탭 후 탭/홀드/릴리즈 방향 선택 팝업
    // code = 선택된 키코드(일반 키), null = 팝오버 닫힘
    // isModifier = true이면 modifier 키 단독 홀드/릴리즈 (code 대신 modBit 사용)
    var stepKindPopoverCode by remember { mutableStateOf<Int?>(null) }
    var stepKindPopoverModBit by remember { mutableIntStateOf(0) }
    // 단일 키 모드: 선택한 동작 타입 임시 보관 (null = 미선택). 확정은 하단 버튼으로.
    var stepDraftKind by remember { mutableStateOf<MacroStepKind?>(null) }
    // SWIPE 모드: 팝오버 열린 동안 방향 포커스 (null=H 중앙, 각 방향=해당 버튼)
    var kindFocusDir by remember { mutableStateOf<com.bridgeone.app.ui.common.swipe.Direction?>(null) }
    // SWIPE 모드: 경계 점멸 트리거 (값 변경마다 점멸 재시작). 기본값: 0
    var kindFlashKey by remember { mutableIntStateOf(0) }
    // SWIPE 모드: 점멸 대상 방향 (null=H 셀, 각 방향=해당 팝오버 버튼). 기본값: null
    var kindFlashTarget by remember { mutableStateOf<com.bridgeone.app.ui.common.swipe.Direction?>(null) }
    // 현재 팝오버 셀의 홀드 방향 (LEFT or DOWN), 락 키 여부를 moveInterceptor가 읽기 위한 공유 상태
    var popoverHoldDir by remember { mutableStateOf(com.bridgeone.app.ui.common.swipe.Direction.LEFT) }
    var popoverReleaseDir by remember { mutableStateOf(com.bridgeone.app.ui.common.swipe.Direction.RIGHT) }
    var popoverIsLock by remember { mutableStateOf(false) }
    // SWIPE 모드: 연속 추가 버튼 롱프레스 시 반복 횟수 조절 팝업 표시 여부. 기본값: false
    var repeatPopupOpen by remember { mutableStateOf(false) }
    // 반복 팝업 열기 직전 횟수 백업 (롱프레스 취소 시 복원용). 기본값: 1
    var repeatBackup by remember { mutableIntStateOf(1) }
    // TEXT 모드 입력 버퍼
    var stepTextInput by remember { mutableStateOf("") }
    // SWIPE 모드 텍스트 입력 대상 (이름 / 스텝 문자열). 둘 다 동일 SwipeKeyboardOverlay 공유.
    var kbTarget by remember { mutableStateOf(MacroKbTarget.NONE) }
    LaunchedEffect(kbTarget) { onNameKeyboardActiveChange(kbTarget != MacroKbTarget.NONE) }
    // 측정된 스와이프 키보드 높이. BoxWithConstraints 밖 maxStepListHeight 계산에서도 사용하므로 함수 레벨에 호이스트.
    var kbActualHeightDp by remember { mutableStateOf(0.dp) }

    // 스텝 목록 자동 확장 + 자동 스크롤 (STEPS 페이지)
    val stepListScreenHeightDp = LocalConfiguration.current.screenHeightDp.dp
    // 다이얼로그 카드 최대 높이: 화면 비율로 제한하여 상하 여백 확보
    val dialogCardMaxHeight = stepListScreenHeightDp * MACRO_DIALOG_MAX_SCREEN_FRACTION
    // 키보드 활성 시 스텝 목록에서 추가로 차감할 높이 (카드를 짧게 만들어 키보드 위에 온전히 표시)
    val kbActive = kbTarget != MacroKbTarget.NONE
    val swipeKbReserve = if (inputMode == InputMode.SWIPE && kbActive) {
        (if (kbActualHeightDp > 0.dp) kbActualHeightDp
         else EdgeSwipeConstants.EDGE_ZONE_LABEL_KEYBOARD_VISUAL_HEIGHT_DP.dp) + 16.dp
    } else 0.dp
    val normalKbReserve = if (inputMode == InputMode.NORMAL) {
        with(LocalDensity.current) { WindowInsets.ime.getBottom(this).toDp() }
    } else 0.dp
    val keyboardReserve = swipeKbReserve + normalKbReserve
    // 스텝 목록 최대 높이: 카드 최대 높이에서 하단 고정 섹션(버튼 등) 및 키보드 reserve 차감 (키보드 등장/사라질 때 애니메이션)
    val maxStepListHeight by animateDpAsState(
        targetValue = (dialogCardMaxHeight - MACRO_STEP_LIST_RESERVED_DP.dp - keyboardReserve)
            .coerceAtLeast(if (keyboardReserve > 0.dp) 100.dp else 200.dp),
        animationSpec = tween(220),
        label = "maxStepListHeight",
    )
    val stepScrollState = rememberScrollState()
    val stepItemHeights = remember { mutableStateMapOf<Int, Int>() }
    val groupHeaderHeights = remember { mutableStateMapOf<Int, Int>() }
    var stepViewportHeight by remember { mutableIntStateOf(0) }
    // 자동 스크롤 여백 버퍼 (포커스 테두리+반올림 오차 보정, 도메인 칩/옵션 카드 패턴과 동일)
    val stepScrollBufferPx = with(LocalDensity.current) { 4.dp.toPx() }.toInt()
    // 스텝 목록 Column의 spacedBy 간격 픽셀값 (itemTop 위치 계산 시 누적 오차 보정용). 기본값: 4dp
    val stepItemSpacingPx = with(LocalDensity.current) { 4.dp.toPx() }.toInt()

    // 프리셋 조합(PICK) 그리드 스크롤 상태 (인라인 rememberScrollState 대신 호이스트)
    val pickScrollState = rememberScrollState()
    var pickViewportHeight by remember { mutableIntStateOf(0) }
    // PICK 그리드 자동 스크롤용 픽셀 상수 (LaunchedEffect 안에서 @Composable 호출 불가 → 미리 계산)
    // 셀 높이: 수직 패딩 10dp*2 + 텍스트 약 20dp = 40dp, 행 간격 4dp → 행 step 44dp
    val pickCellHeightPx = with(LocalDensity.current) { 40.dp.toPx() }
    val pickRowStepPx = with(LocalDensity.current) { (40.dp + 4.dp).toPx() }
    val pickScrollBufferPx = with(LocalDensity.current) { 4.dp.toPx() }.toInt()

    // 포커스된 프리셋 행이 뷰포트 밖이면 자동 스크롤 (SWIPE 전용, STEP_INPUT/PICK 페이지)
    LaunchedEffect(swipeController?.currentFocus) {
        val focus = swipeController?.currentFocus
        if (focus !is EdgeEditorElement.MacroShortcutPick) return@LaunchedEffect
        if (currentPage != MacroEditorPage.STEP_INPUT || stepEditMode != MacroStepEditMode.PICK || inputMode != InputMode.SWIPE) return@LaunchedEffect
        val rowIdx = focus.index / 3
        val rowStart = (rowIdx * pickRowStepPx).toInt()
        val rowEnd = kotlin.math.ceil(rowIdx * pickRowStepPx + pickCellHeightPx).toInt()
        val scrollOffset = pickScrollState.value
        val viewportEnd = scrollOffset + pickViewportHeight
        if (rowStart < scrollOffset) {
            pickScrollState.animateScrollTo((rowStart - pickScrollBufferPx).coerceAtLeast(0))
        } else if (rowEnd > viewportEnd && pickViewportHeight > 0) {
            pickScrollState.animateScrollTo(rowEnd - pickViewportHeight + pickScrollBufferPx)
        }
    }

    // 포커스된 스텝 행이 뷰포트 밖이면 자동 스크롤 (SWIPE 전용, STEPS 페이지)
    LaunchedEffect(swipeController?.currentFocus) {
        if (currentPage != MacroEditorPage.STEPS || inputMode != InputMode.SWIPE) return@LaunchedEffect
        val focus = swipeController?.currentFocus ?: return@LaunchedEffect
        // 첫 스텝 인덱스 → groupId 맵 (폴더 헤더 위치 계산용)
        val groupLeaderMap: Map<Int, Int> = buildMap {
            draft.steps.forEachIndexed { i, step ->
                val gid = step.groupId ?: return@forEachIndexed
                if (i == 0 || draft.steps[i - 1].groupId != gid) put(i, gid)
            }
        }
        // 스텝 idx의 Column 내 픽셀 위치를 폴더 헤더 높이 포함하여 계산
        fun stepPos(idx: Int): Pair<Int, Int> {
            var childCount = 0; var px = 0
            for (i in 0 until idx) {
                groupLeaderMap[i]?.let { gid -> px += groupHeaderHeights[gid] ?: 0; childCount++ }
                px += stepItemHeights[i] ?: 0; childCount++
            }
            groupLeaderMap[idx]?.let { gid -> px += groupHeaderHeights[gid] ?: 0; childCount++ }
            val top = px + childCount * stepItemSpacingPx
            return top to top + (stepItemHeights[idx] ?: 0)
        }
        // 그룹 헤더 자체의 픽셀 위치 계산
        fun groupHeaderPos(gid: Int, firstIdx: Int): Pair<Int, Int> {
            var childCount = 0; var px = 0
            for (i in 0 until firstIdx) {
                groupLeaderMap[i]?.let { g -> px += groupHeaderHeights[g] ?: 0; childCount++ }
                px += stepItemHeights[i] ?: 0; childCount++
            }
            val top = px + childCount * stepItemSpacingPx
            return top to top + (groupHeaderHeights[gid] ?: 0)
        }
        val (itemTop, itemBottom) = when (focus) {
            is EdgeEditorElement.MacroStepChip -> stepPos(focus.index)
            is EdgeEditorElement.MacroStepUp -> stepPos(focus.index)
            is EdgeEditorElement.MacroStepDown -> stepPos(focus.index)
            is EdgeEditorElement.MacroStepDelete -> stepPos(focus.index)
            is EdgeEditorElement.MacroStepDelayExpand -> stepPos(focus.index)
            is EdgeEditorElement.MacroStepDuplicate -> stepPos(focus.index)
            is EdgeEditorElement.MacroStepSplitMerge -> stepPos(focus.index)
            is EdgeEditorElement.MacroStepDelaySlider -> stepPos(focus.index)
            is EdgeEditorElement.MacroStepDragHandle -> stepPos(focus.index)
            is EdgeEditorElement.MacroStepSelectCheck -> stepPos(focus.index)
            is EdgeEditorElement.MacroGroupHeader -> {
                val fi = draft.steps.indexOfFirst { it.groupId == focus.groupId }
                if (fi < 0) return@LaunchedEffect
                groupHeaderPos(focus.groupId, fi)
            }
            is EdgeEditorElement.MacroGroupDragHandle -> {
                val fi = draft.steps.indexOfFirst { it.groupId == focus.groupId }
                if (fi < 0) return@LaunchedEffect
                groupHeaderPos(focus.groupId, fi)
            }
            is EdgeEditorElement.MacroGroupRename -> {
                val fi = draft.steps.indexOfFirst { it.groupId == focus.groupId }
                if (fi < 0) return@LaunchedEffect
                groupHeaderPos(focus.groupId, fi)
            }
            is EdgeEditorElement.MacroGroupDuplicate -> {
                val fi = draft.steps.indexOfFirst { it.groupId == focus.groupId }
                if (fi < 0) return@LaunchedEffect
                groupHeaderPos(focus.groupId, fi)
            }
            is EdgeEditorElement.MacroGroupUngroup -> {
                val fi = draft.steps.indexOfFirst { it.groupId == focus.groupId }
                if (fi < 0) return@LaunchedEffect
                groupHeaderPos(focus.groupId, fi)
            }
            is EdgeEditorElement.MacroGroupDelete -> {
                val fi = draft.steps.indexOfFirst { it.groupId == focus.groupId }
                if (fi < 0) return@LaunchedEffect
                groupHeaderPos(focus.groupId, fi)
            }
            is EdgeEditorElement.MacroStepGroupConfirm -> {
                val minIdx = selectedStepIndices.minOrNull() ?: return@LaunchedEffect
                stepPos(minIdx)
            }
            else -> return@LaunchedEffect
        }
        val vTop = stepScrollState.value
        val vBottom = vTop + stepViewportHeight
        when {
            itemTop < vTop -> stepScrollState.animateScrollTo((itemTop - stepScrollBufferPx).coerceAtLeast(0))
            itemBottom > vBottom && stepViewportHeight > 0 ->
                stepScrollState.animateScrollTo(itemBottom - stepViewportHeight + stepScrollBufferPx)
        }
    }

    // 아이콘 / 이름 / 한글 모드 (항상 표시)
    var draftIconKey by remember { mutableStateOf(initialIconKey) }
    var draftName by remember { mutableStateOf(initialName) }
    var iconBtnCenter by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var normalIconSheetVisible by remember { mutableStateOf(false) }
    val normalIconSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun resetStepDraft() {
        stepDraftMod = 0
        stepDraftKeys = emptyList()
        stepDraftDelay = null
        stepDraftRepeat = 1
        stepDraftKind = null
        kindFocusDir = null
        kindFlashKey = 0
        kindFlashTarget = null
        popoverHoldDir = com.bridgeone.app.ui.common.swipe.Direction.LEFT
        popoverReleaseDir = com.bridgeone.app.ui.common.swipe.Direction.RIGHT
        popoverIsLock = false
        stepTextInput = ""
        stepKindPopoverCode = null
        stepKindPopoverModBit = 0
    }

    // 스텝 편집 시작 (기존 스텝): 보조키 유무/키 개수로 SINGLE_KEY vs COMBO 자동 감지.
    // HOLD/RELEASE 스텝은 SINGLE_KEY 모드로 진입하고 팝오버 상태를 선택된 키로 복원.
    fun startEditStep(index: Int) {
        val step = draft.steps.getOrNull(index)
        stepDraftMod = 0
        stepDraftKeys = emptyList()
        stepDraftDelay = step?.delayAfterMs
        stepDraftRepeat = step?.repeatCount?.coerceAtLeast(1) ?: 1
        stepTextInput = ""
        stepKindPopoverCode = null
        stepKindPopoverModBit = 0
        if (step != null && step.kind != MacroStepKind.TAP) {
            // HOLD/RELEASE: SINGLE_KEY 모드로 진입, 키·동작 타입 임시 저장으로 복원 (팝오버 불필요)
            stepEditMode = MacroStepEditMode.SINGLE_KEY
            stepDraftKeys = step.keyCodes
            stepDraftMod = step.modifierBits
            stepDraftKind = step.kind
        } else {
            stepDraftMod = step?.modifierBits ?: 0
            stepDraftKeys = step?.keyCodes ?: emptyList()
            stepEditMode = if ((step?.modifierBits ?: 0) != 0 || (step?.keyCodes?.size ?: 0) > 1)
                MacroStepEditMode.COMBO else MacroStepEditMode.SINGLE_KEY
        }
        editingStepIndex = index
        currentPage = MacroEditorPage.STEP_INPUT
        if (inputMode == InputMode.SWIPE) swipeController?.setFocus(
            if (stepEditMode == MacroStepEditMode.COMBO) EdgeEditorElement.MacroStepModeCombo
            else EdgeEditorElement.MacroStepModeSingleKey
        )
    }

    // 새 스텝 추가: 마지막 사용 키 모드(SINGLE_KEY/COMBO)로 바로 진입
    fun startAddStep() {
        resetStepDraft()
        stepEditMode = lastStepMode  // lastStepMode는 항상 SINGLE_KEY 또는 COMBO
        editingStepIndex = draft.steps.size // 마지막 다음 index
        currentPage = MacroEditorPage.STEP_INPUT
        if (inputMode == InputMode.SWIPE) {
            swipeController?.setFocus(
                if (lastStepMode == MacroStepEditMode.SINGLE_KEY) EdgeEditorElement.MacroStepModeSingleKey
                else EdgeEditorElement.MacroStepModeCombo
            )
        }
    }

    // 문자열 → 여러 스텝 일괄 추가 모드로 진입 (개요 화면의 별도 버튼으로 진입)
    fun startAddText() {
        resetStepDraft()
        stepEditMode = MacroStepEditMode.TEXT
        editingStepIndex = draft.steps.size
        currentPage = MacroEditorPage.STEP_INPUT
        if (inputMode == InputMode.SWIPE) {
            kbTarget = MacroKbTarget.STEP_TEXT
            swipeController?.setFocus(EdgeEditorElement.MacroTextField)
        }
    }

    // 세그먼트 탭으로 입력 모드 전환
    fun selectStepMode(target: MacroStepEditMode) {
        if (stepEditMode == target) return
        when (target) {
            MacroStepEditMode.SINGLE_KEY -> {
                kbTarget = MacroKbTarget.NONE
                // 단일 키 모드 전환: 보조키 초기화, 키 여러 개면 첫 번째만 유지
                stepDraftMod = 0
                stepDraftKeys = stepDraftKeys.take(1)
            }
            MacroStepEditMode.COMBO -> { kbTarget = MacroKbTarget.NONE }
            MacroStepEditMode.TEXT -> { if (inputMode == InputMode.SWIPE) kbTarget = MacroKbTarget.STEP_TEXT }
            MacroStepEditMode.PICK -> { kbTarget = MacroKbTarget.NONE }
        }
        stepEditMode = target
        if (target != MacroStepEditMode.TEXT && target != MacroStepEditMode.PICK) lastStepMode = target
    }

    // 단축키 피커 모드로 진입
    fun startAddShortcutPick() {
        resetStepDraft()
        pickStagedSteps = emptyList()
        stepEditMode = MacroStepEditMode.PICK
        editingStepIndex = draft.steps.size
        currentPage = MacroEditorPage.STEP_INPUT
        if (inputMode == InputMode.SWIPE) {
            swipeController?.setFocus(EdgeEditorElement.MacroAddFromShortcut)
        }
    }

    // 키 스텝(단일 키/단축키) 저장. continueAdding이면 개요로 안 돌아가고 다음 스텝 입력 준비.
    fun applyStep(continueAdding: Boolean) {
        val kind: MacroStepKind
        if (stepEditMode == MacroStepEditMode.SINGLE_KEY) {
            val hasKey = stepDraftKeys.isNotEmpty() || stepDraftMod != 0
            if (!hasKey) {
                ToastController.show("키를 하나 선택하세요", ToastType.WARNING)
                return
            }
            if (stepDraftKind == null) {
                ToastController.show("동작 타입을 선택하세요 (탭 / 홀드 / 릴리즈)", ToastType.INFO)
                return
            }
            kind = stepDraftKind!!
        } else {
            if (stepDraftKeys.isEmpty() && stepDraftMod == 0) {
                ToastController.show("키를 하나 이상 선택하세요", ToastType.WARNING)
                return
            }
            kind = MacroStepKind.TAP
        }
        val newStep = MacroStep(
            modifierBits = stepDraftMod,
            keyCodes = stepDraftKeys,
            delayAfterMs = stepDraftDelay,
            repeatCount = if (kind == MacroStepKind.TAP) stepDraftRepeat.coerceIn(1, 99) else 1,
            kind = kind,
        )
        val idx = editingStepIndex ?: return
        val newSteps = if (idx < draft.steps.size) {
            draft.steps.toMutableList().also { it[idx] = newStep }
        } else {
            draft.steps + newStep
        }
        onDraftChange(draft.copy(steps = newSteps))
        lastStepMode = stepEditMode
        if (continueAdding) {
            resetStepDraft()
            editingStepIndex = newSteps.size // 다음 신규 위치
            if (inputMode == InputMode.SWIPE) swipeController?.setFocus(EdgeEditorElement.ShortcutKey(0x0B)) // H 키
        } else {
            editingStepIndex = null
            currentPage = MacroEditorPage.STEPS
            kbTarget = MacroKbTarget.NONE
            if (inputMode == InputMode.SWIPE) swipeController?.setFocus(EdgeEditorElement.MacroAddStep)
        }
    }

    // 키 종류(TAP/HOLD/RELEASE) 임시 저장 (팝오버 방향 선택용).
    // 실제 스텝 추가는 applyStep()에서 수행. 팝오버는 닫히고 상단바에 표시됨.
    fun selectStepKind(kind: MacroStepKind, code: Int, modBit: Int) {
        if (code != 0) {
            stepDraftKeys = listOf(code)
            stepDraftMod = 0
        } else {
            stepDraftMod = modBit
            stepDraftKeys = emptyList()
        }
        stepDraftKind = kind
        // 팝오버 닫기
        stepKindPopoverCode = null
        stepKindPopoverModBit = 0
    }

    // 문자열 → 스텝들 자동 생성. continueAdding이면 입력 영역 유지.
    fun generateFromText(continueAdding: Boolean) {
        val r = com.bridgeone.app.ui.common.MacroTextEncoder.encode(stepTextInput)
        if (r.steps.isEmpty()) {
            ToastController.show("변환할 문자를 입력하세요", ToastType.WARNING)
            return
        }
        onDraftChange(draft.copy(
            steps = draft.steps + r.steps,
            inputModeCheck = if (draft.inputModeCheck != InputModeCheck.NONE) draft.inputModeCheck else r.inputModeCheck,
        ))
        if (r.hanYeongInsertCount > 0) {
            ToastController.show("한/영 전환 ${r.hanYeongInsertCount}회 자동 삽입됨", ToastType.INFO)
        }
        if (r.skipped.isNotEmpty()) {
            ToastController.show("변환 불가 문자 생략: ${r.skipped.joinToString("")}", ToastType.WARNING)
        }
        stepTextInput = ""
        if (continueAdding) {
            editingStepIndex = draft.steps.size + r.steps.size
            if (inputMode == InputMode.SWIPE) {
                kbTarget = MacroKbTarget.STEP_TEXT
                swipeController?.setFocus(EdgeEditorElement.MacroTextField)
            }
        } else {
            editingStepIndex = null
            currentPage = MacroEditorPage.STEPS
            kbTarget = MacroKbTarget.NONE
            if (inputMode == InputMode.SWIPE) swipeController?.setFocus(EdgeEditorElement.MacroAddStep)
        }
    }

    // 입력 취소 → 개요로 복귀 (스텝 저장 안 함)
    fun cancelStepEdit() {
        resetStepDraft()
        editingStepIndex = null
        currentPage = MacroEditorPage.STEPS
        kbTarget = MacroKbTarget.NONE
        if (inputMode == InputMode.SWIPE) swipeController?.setFocus(EdgeEditorElement.MacroAddStep)
    }

    // 스텝 순서 재정렬
    fun moveStep(from: Int, to: Int) {
        if (to < 0 || to >= draft.steps.size) return
        val l = draft.steps.toMutableList()
        l.add(to, l.removeAt(from))
        onDraftChange(draft.copy(steps = l))
    }

    // ── 폴더 그룹 헬퍼 ──

    /** idx 스텝이 속한 groupId 연속 구간 [start..end]. groupId=null이면 idx..idx. */
    fun groupRange(steps: List<com.bridgeone.app.ui.components.touchpad.MacroStep>, idx: Int): IntRange {
        val gid = steps[idx].groupId ?: return idx..idx
        var s = idx; while (s > 0 && steps[s - 1].groupId == gid) s--
        var e = idx; while (e < steps.lastIndex && steps[e + 1].groupId == gid) e++
        return s..e
    }

    /** 연속 동일 groupId를 세그먼트 리스트(groupId? to IntRange)로 분할 (렌더용). */
    fun macroSegments(steps: List<com.bridgeone.app.ui.components.touchpad.MacroStep>): List<Pair<Int?, IntRange>> {
        if (steps.isEmpty()) return emptyList()
        val result = mutableListOf<Pair<Int?, IntRange>>()
        var start = 0
        while (start < steps.size) {
            val gid = steps[start].groupId
            var end = start
            while (end + 1 < steps.size && steps[end + 1].groupId == gid) end++
            result.add(gid to start..end)
            start = end + 1
        }
        return result
    }

    /** 폴더(groupId 구간)를 통째로 위 또는 아래로 이동. */
    fun moveGroup(groupId: Int, up: Boolean) {
        val steps = draft.steps.toMutableList()
        val firstIdx = steps.indexOfFirst { it.groupId == groupId }
        if (firstIdx < 0) return
        val range = groupRange(steps, firstIdx)
        if (up) {
            if (range.first == 0) return
            val prevRange = groupRange(steps, range.first - 1)
            val block = steps.subList(range.first, range.last + 1).toList()
            repeat(block.size) { steps.removeAt(range.first) }
            steps.addAll(prevRange.first, block)
        } else {
            if (range.last >= steps.lastIndex) return
            val nextRange = groupRange(steps, range.last + 1)
            val block = steps.subList(range.first, range.last + 1).toList()
            repeat(block.size) { steps.removeAt(range.first) }
            steps.addAll(range.first + (nextRange.last - nextRange.first + 1), block)
        }
        onDraftChange(draft.copy(steps = steps))
    }

    /** 폴더(groupId 구간)를 복제. 새 groupId를 부여. */
    fun duplicateGroup(groupId: Int) {
        val steps = draft.steps.toMutableList()
        val firstIdx = steps.indexOfFirst { it.groupId == groupId }
        if (firstIdx < 0) return
        val range = groupRange(steps, firstIdx)
        val newId = (steps.mapNotNull { it.groupId }.maxOrNull() ?: 0) + 1
        val block = steps.subList(range.first, range.last + 1).map { it.copy(groupId = newId) }
        steps.addAll(range.last + 1, block)
        val origName = draft.groupNames[groupId] ?: "폴더"
        onDraftChange(draft.copy(
            steps = steps,
            groupNames = draft.groupNames + (newId to "$origName 복사"),
        ))
        if (inputMode == InputMode.SWIPE) {
            swipeController?.setFocus(EdgeEditorElement.MacroGroupHeader(newId))
        }
    }

    /** 폴더(groupId 구간) 삭제. */
    fun deleteGroup(groupId: Int) {
        val steps = draft.steps.toMutableList()
        val firstIdx = steps.indexOfFirst { it.groupId == groupId }
        if (firstIdx < 0) return
        val range = groupRange(steps, firstIdx)
        val focusIdx = if (range.first > 0) range.first - 1 else range.last + 1 - (range.last - range.first + 1)
        repeat(range.last - range.first + 1) { steps.removeAt(range.first) }
        onDraftChange(draft.copy(
            steps = steps,
            groupNames = draft.groupNames.filterKeys { it != groupId },
        ))
        if (inputMode == InputMode.SWIPE) {
            val targetIdx = focusIdx.coerceIn(0, steps.lastIndex)
            if (steps.isNotEmpty()) swipeController?.setFocus(EdgeEditorElement.MacroStepSelectCheck(targetIdx))
        }
    }

    /** 폴더 해제 (groupId → null, groupNames 정리). */
    fun ungroupGroup(groupId: Int) {
        val firstIdx = draft.steps.indexOfFirst { it.groupId == groupId }
        val newSteps = draft.steps.map { if (it.groupId == groupId) it.copy(groupId = null) else it }
        onDraftChange(draft.copy(
            steps = newSteps,
            groupNames = draft.groupNames.filterKeys { it != groupId },
        ))
        if (inputMode == InputMode.SWIPE && firstIdx >= 0) {
            swipeController?.setFocus(EdgeEditorElement.MacroStepSelectCheck(firstIdx))
        }
    }

    /** 선택된 인덱스를 새 폴더로 묶기. 연속 인덱스가 아니면 토스트 후 중단. */
    fun createGroupFromSelected() {
        val sorted = selectedStepIndices.sorted()
        if (sorted.isEmpty()) return
        val isContiguous = sorted.zipWithNext().all { (a, b) -> b == a + 1 }
        if (!isContiguous) {
            ToastController.show("연속된 스텝만 폴더로 묶을 수 있습니다", ToastType.WARNING)
            return
        }
        val newId = (draft.steps.mapNotNull { it.groupId }.maxOrNull() ?: 0) + 1
        val newName = "폴더 ${newId}"
        val newSteps = draft.steps.mapIndexed { i, step ->
            if (i in sorted) step.copy(groupId = newId) else step
        }
        // 고아 groupNames 정리
        val cleanedNames = draft.groupNames.filterKeys { id -> newSteps.any { it.groupId == id } }
        onDraftChange(draft.copy(
            steps = newSteps,
            groupNames = cleanedNames + (newId to newName),
        ))
        selectedStepIndices = emptySet()
        groupAnchor = null
        if (inputMode == InputMode.SWIPE) {
            swipeController?.setFocus(EdgeEditorElement.MacroGroupHeader(newId))
        }
    }

    /** 미니버튼 아이콘 이름 (툴팁용). */
    data class KeyDef(val label: String, val code: Int, val weight: Float = 1f, val modBit: Int = 0, val modSecondary: Boolean = false)
    fun spacer(w: Float) = KeyDef("", 0, w)
    fun modifier(label: String, bit: Int, weight: Float = 1f, secondary: Boolean = false) = KeyDef(label, 0, weight, bit, secondary)

    val kbRows = listOf(
        listOf(
            KeyDef("Esc", 0x29, 1.0f), spacer(0.5f),
            KeyDef("F1", 0x3A), KeyDef("F2", 0x3B), KeyDef("F3", 0x3C), KeyDef("F4", 0x3D), spacer(0.25f),
            KeyDef("F5", 0x3E), KeyDef("F6", 0x3F), KeyDef("F7", 0x40), KeyDef("F8", 0x41), spacer(0.25f),
            KeyDef("F9", 0x42), KeyDef("F10", 0x43), KeyDef("F11", 0x44), KeyDef("F12", 0x45)
        ),
        listOf(
            KeyDef("`", 0x35), KeyDef("1", 0x1E), KeyDef("2", 0x1F), KeyDef("3", 0x20),
            KeyDef("4", 0x21), KeyDef("5", 0x22), KeyDef("6", 0x23), KeyDef("7", 0x24),
            KeyDef("8", 0x25), KeyDef("9", 0x26), KeyDef("0", 0x27),
            KeyDef("-", 0x2D), KeyDef("=", 0x2E), KeyDef("BkSp", 0x2A, 2f)
        ),
        listOf(
            KeyDef("Tab", 0x2B, 1.5f),
            KeyDef("Q", 0x14), KeyDef("W", 0x1A), KeyDef("E", 0x08), KeyDef("R", 0x15),
            KeyDef("T", 0x17), KeyDef("Y", 0x1C), KeyDef("U", 0x18), KeyDef("I", 0x0C),
            KeyDef("O", 0x12), KeyDef("P", 0x13), KeyDef("[", 0x2F), KeyDef("]", 0x30),
            KeyDef("\\", 0x31, 1.5f)
        ),
        listOf(
            KeyDef("CapsLk", 0x39, 1.75f),
            KeyDef("A", 0x04), KeyDef("S", 0x16), KeyDef("D", 0x07), KeyDef("F", 0x09),
            KeyDef("G", 0x0A), KeyDef("H", 0x0B), KeyDef("J", 0x0D), KeyDef("K", 0x0E),
            KeyDef("L", 0x0F), KeyDef(";", 0x33), KeyDef("'", 0x34), KeyDef("Enter", 0x28, 2.25f)
        ),
        listOf(
            modifier("Shift", MOD_BIT_LSHIFT.toInt(), 2.5f),
            KeyDef("Z", 0x1D), KeyDef("X", 0x1B), KeyDef("C", 0x06), KeyDef("V", 0x19),
            KeyDef("B", 0x05), KeyDef("N", 0x11), KeyDef("M", 0x10),
            KeyDef(",", 0x36), KeyDef(".", 0x37), KeyDef("/", 0x38),
            modifier("Shift", MOD_BIT_LSHIFT.toInt(), 2.5f, secondary = true)
        ),
        listOf(
            modifier("Ctrl", MOD_BIT_LCTRL.toInt(), 1.5f),
            modifier("Win", MOD_BIT_LGUI.toInt(), 1.25f),
            modifier("Alt", MOD_BIT_LALT.toInt(), 1.25f),
            KeyDef("Space", 0x2C, 7f),
            modifier("Alt", MOD_BIT_LALT.toInt(), 1.25f, secondary = true),
            modifier("Win", MOD_BIT_LGUI.toInt(), 1.25f, secondary = true),
            modifier("Ctrl", MOD_BIT_LCTRL.toInt(), 1.5f, secondary = true),
        )
    )
    val navRows = listOf(
        listOf(KeyDef("Ins", 0x49), KeyDef("Home", 0x4A), KeyDef("PgUp", 0x4B), spacer(0.5f), spacer(1f), KeyDef("↑", 0x52), spacer(1f)),
        listOf(KeyDef("Del", 0x4C), KeyDef("End", 0x4D), KeyDef("PgDn", 0x4E), spacer(0.5f), KeyDef("←", 0x50), KeyDef("↓", 0x51), KeyDef("→", 0x4F))
    )

    @Composable
    fun MacroKeyCell(def: KeyDef, rowIndex: Int, weightMod: Modifier) {
        if (def.code == 0 && def.modBit == 0) { Spacer(weightMod); return }
        val isModifier = def.modBit != 0
        val singleKey = stepEditMode == MacroStepEditMode.SINGLE_KEY
        // 락 키: 홀드·릴리즈 의미 없음 (탭만 표시)
        val isLockKey = !isModifier && def.code in setOf(0x39 /*CapsLk*/, 0x53 /*NumLock*/, 0x47 /*ScrollLock*/)
        val active = when {
            isModifier -> stepDraftMod and def.modBit != 0 || stepKindPopoverModBit and def.modBit != 0
            else -> def.code in stepDraftKeys || def.code == stepKindPopoverCode
        }
        val isPopoverCell = singleKey && (
            (!isModifier && def.code != 0 && def.code == stepKindPopoverCode) ||
            (isModifier && def.modBit != 0 && def.modBit == stepKindPopoverModBit)
        )
        // 팝오버가 열린 상태에서 이 셀이 팝오버 셀이 아니면 딤 처리
        val anyPopoverOpen = singleKey && (stepKindPopoverCode != null || stepKindPopoverModBit != 0)
        val dimmed = anyPopoverOpen && !isPopoverCell
        // 방향 포커스 중이면 H를 흐리게 (팝오버 버튼에 주목)
        val isThisPopoverCellWithDirFocus = kindFocusDir != null && isPopoverCell
        val bgColor = { focused: Boolean ->
            when {
                active && isThisPopoverCellWithDirFocus -> cs.primary.copy(alpha = 0.45f)
                active -> cs.primary
                focused -> cs.primary.copy(alpha = 0.25f)
                else -> cs.surface
            }
        }
        val textColor = { focused: Boolean ->
            when {
                active -> cs.onPrimary
                focused -> cs.primary
                else -> cs.onSurface
            }
        }
        val onTap = {
            if (singleKey) {
                // 단일 키 모드: 탭하면 팝오버 열기, 재탭 시 포커스 방향 확정 또는 닫기
                if (isModifier) {
                    if (stepKindPopoverModBit == def.modBit) {
                        // 재탭: 포커스된 방향에 따라 확정, 없으면 닫기
                        val m = def.modBit
                        when {
                            kindFocusDir == com.bridgeone.app.ui.common.swipe.Direction.UP ->
                                selectStepKind(MacroStepKind.TAP, 0, m)
                            kindFocusDir == popoverHoldDir && !popoverIsLock ->
                                selectStepKind(MacroStepKind.HOLD, 0, m)
                            kindFocusDir == popoverReleaseDir && !popoverIsLock ->
                                selectStepKind(MacroStepKind.RELEASE, 0, m)
                        }
                        stepKindPopoverModBit = 0
                        stepKindPopoverCode = null
                        kindFocusDir = null
                    } else {
                        if (stepDraftMod and def.modBit == 0) {
                            stepDraftKeys = emptyList()
                            stepDraftKind = null
                        }
                        stepKindPopoverCode = null
                        stepKindPopoverModBit = def.modBit
                        kindFocusDir = null
                    }
                } else {
                    if (stepKindPopoverCode == def.code) {
                        // 재탭: 포커스된 방향에 따라 확정, 없으면 닫기
                        val c = def.code
                        when {
                            kindFocusDir == com.bridgeone.app.ui.common.swipe.Direction.UP ->
                                selectStepKind(MacroStepKind.TAP, c, 0)
                            kindFocusDir == popoverHoldDir && !popoverIsLock ->
                                selectStepKind(MacroStepKind.HOLD, c, 0)
                            kindFocusDir == popoverReleaseDir && !popoverIsLock ->
                                selectStepKind(MacroStepKind.RELEASE, c, 0)
                        }
                        stepKindPopoverCode = null
                        stepKindPopoverModBit = 0
                        kindFocusDir = null
                    } else {
                        if (def.code !in stepDraftKeys) {
                            stepDraftKeys = emptyList()
                            stepDraftKind = null
                        }
                        stepKindPopoverModBit = 0
                        stepKindPopoverCode = def.code
                        kindFocusDir = null
                    }
                }
            } else {
                // 단축키(COMBO) 모드: 기존 동작 유지
                if (isModifier) {
                    stepDraftMod = stepDraftMod xor def.modBit
                } else {
                    stepDraftKeys = when {
                        def.code in stepDraftKeys -> stepDraftKeys - def.code
                        stepDraftKeys.isNotEmpty() -> {
                            ToastController.show("단축키는 보조키와 일반 키 1개만 조합할 수 있습니다", ToastType.ERROR)
                            stepDraftKeys
                        }
                        else -> listOf(def.code)
                    }
                }
            }
        }
        val element: EdgeEditorElement = if (isModifier) EdgeEditorElement.ShortcutModifier(def.modBit, def.modSecondary)
            else EdgeEditorElement.ShortcutKey(def.code)
        val density = LocalDensity.current
        val config = LocalConfiguration.current
        var keyCellWidthPx by remember { mutableStateOf(0) }
        var cellLeftPx by remember { mutableStateOf(0f) }
        var holdBtnWidthPx by remember { mutableStateOf(0) }
        var releaseBtnWidthPx by remember { mutableStateOf(0) }
        // 경계 점멸 애니메이션 (팝오버 버튼 없는 방향 스와이프 시)
        val flashAlpha = remember { Animatable(0f) }
        val myFlashKey = if (isPopoverCell) kindFlashKey else 0
        LaunchedEffect(myFlashKey) {
            if (myFlashKey > 0) {
                flashAlpha.snapTo(0.45f)
                flashAlpha.animateTo(0f, animationSpec = tween(durationMillis = 350))
            }
        }
        // 키 종류 팝오버: 각 버튼을 키 셀 바깥(상/좌/우/하)에 독립 Popup으로 배치
        // 홀드·릴리즈 방향은 화면 경계 기준 동적 결정. 락 키는 탭만.
        val kindPopup: @Composable () -> Unit = {
            if (isPopoverCell) {
                val popCode = stepKindPopoverCode ?: 0
                val popMod = stepKindPopoverModBit
                val kindBtnShape = RoundedCornerShape(6.dp)
                val btnPadH = 9.dp
                val btnPadV = 6.dp
                val btnFontSize = 11.sp
                val popupProps = PopupProperties(focusable = false)
                val gapPx = with(density) { 4.dp.toPx().toInt() }
                val screenWpx = with(density) { config.screenWidthDp.dp.toPx() }
                // 홀드: 기본 좌측, 좌측 공간 부족 시 하단
                val holdToBottom = holdBtnWidthPx > 0 && cellLeftPx - gapPx - holdBtnWidthPx < 0f
                // 릴리즈: 기본 우측, 우측 공간 부족 시 하단
                val releaseToBottom = releaseBtnWidthPx > 0 && cellLeftPx + keyCellWidthPx + gapPx + releaseBtnWidthPx > screenWpx
                // 방향 결정 및 공유 (moveInterceptor가 읽음)
                val holdDir = if (holdToBottom) com.bridgeone.app.ui.common.swipe.Direction.DOWN
                              else com.bridgeone.app.ui.common.swipe.Direction.LEFT
                val releaseDir = if (releaseToBottom) com.bridgeone.app.ui.common.swipe.Direction.DOWN
                                 else com.bridgeone.app.ui.common.swipe.Direction.RIGHT
                // 공유 상태에 기록 (SideEffect: Composable 스코프에서 상태 갱신)
                if (popoverHoldDir != holdDir) popoverHoldDir = holdDir
                if (popoverReleaseDir != releaseDir) popoverReleaseDir = releaseDir
                if (popoverIsLock != isLockKey) popoverIsLock = isLockKey
                // 오프셋 계산
                val holdOffsetX = if (holdBtnWidthPx > 0) -holdBtnWidthPx - gapPx
                                  else with(density) { (-58).dp.toPx().toInt() }
                val releaseOffsetX = if (keyCellWidthPx > 0) keyCellWidthPx + gapPx
                                     else with(density) { 30.dp.toPx().toInt() }
                val verticalOffsetPx = with(density) { 34.dp.toPx().toInt() }
                // SWIPE 모드 포커스 상태
                val isTapFocused = inputMode == InputMode.SWIPE &&
                    kindFocusDir == com.bridgeone.app.ui.common.swipe.Direction.UP
                val isHoldFocused = inputMode == InputMode.SWIPE && !isLockKey &&
                    kindFocusDir == holdDir
                val isReleaseFocused = inputMode == InputMode.SWIPE && !isLockKey &&
                    kindFocusDir == releaseDir
                // H 셀 점멸: kindFlashTarget == null (H 방향) 일 때
                val hFlashing = isPopoverCell && kindFlashTarget == null && flashAlpha.value > 0f

                @Composable fun KindBtn(
                    text: String,
                    baseColor: Color,
                    highlighted: Boolean,
                    flashDir: com.bridgeone.app.ui.common.swipe.Direction?,
                    onClick: (() -> Unit)?
                ) {
                    val btnBgColor = if (highlighted) cs.primary else cs.surfaceVariant
                    val fgColor = if (highlighted) cs.onPrimary else baseColor
                    // 이 버튼의 점멸 여부
                    val btnFlashing = kindFlashTarget == flashDir && flashAlpha.value > 0f
                    Box(
                        modifier = Modifier
                            .clip(kindBtnShape)
                            .background(btnBgColor)
                            .then(if (highlighted) Modifier.border(1.5.dp, cs.primary, kindBtnShape) else Modifier)
                            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                            .padding(horizontal = btnPadH, vertical = btnPadV),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text, fontSize = btnFontSize, color = fgColor)
                        if (btnFlashing) {
                            Box(Modifier.matchParentSize().clip(kindBtnShape)
                                .background(cs.error.copy(alpha = flashAlpha.value)))
                        }
                    }
                }

                // 탭 — 항상 키 위
                Popup(
                    alignment = Alignment.TopCenter,
                    offset = IntOffset(0, -verticalOffsetPx),
                    properties = popupProps,
                ) {
                    KindBtn(
                        text = "탭",
                        baseColor = cs.onSurface,
                        highlighted = isTapFocused,
                        flashDir = com.bridgeone.app.ui.common.swipe.Direction.UP,
                        onClick = if (inputMode == InputMode.NORMAL) ({ selectStepKind(MacroStepKind.TAP, popCode, popMod) }) else null
                    )
                }
                // 홀드 — 기본 키 왼쪽, 공간 없으면 키 아래 (락 키 제외)
                if (!isLockKey) {
                    if (holdToBottom) {
                        Popup(
                            alignment = Alignment.BottomCenter,
                            offset = IntOffset(0, verticalOffsetPx),
                            properties = popupProps,
                        ) {
                            Box(modifier = Modifier.onGloballyPositioned {
                                if (it.size.width > 0) holdBtnWidthPx = it.size.width
                            }) {
                                KindBtn(
                                    text = "홀드",
                                    baseColor = cs.tertiary,
                                    highlighted = isHoldFocused,
                                    flashDir = com.bridgeone.app.ui.common.swipe.Direction.DOWN,
                                    onClick = if (inputMode == InputMode.NORMAL) ({ selectStepKind(MacroStepKind.HOLD, popCode, popMod) }) else null
                                )
                            }
                        }
                    } else {
                        Popup(
                            alignment = Alignment.CenterStart,
                            offset = IntOffset(holdOffsetX, 0),
                            properties = popupProps,
                        ) {
                            Box(modifier = Modifier.onGloballyPositioned {
                                if (it.size.width > 0) holdBtnWidthPx = it.size.width
                            }) {
                                KindBtn(
                                    text = "홀드",
                                    baseColor = cs.tertiary,
                                    highlighted = isHoldFocused,
                                    flashDir = com.bridgeone.app.ui.common.swipe.Direction.LEFT,
                                    onClick = if (inputMode == InputMode.NORMAL) ({ selectStepKind(MacroStepKind.HOLD, popCode, popMod) }) else null
                                )
                            }
                        }
                    }
                }
                // 릴리즈 — 기본 키 오른쪽, 공간 없으면 키 아래 (락 키 제외)
                if (!isLockKey) {
                    if (releaseToBottom) {
                        Popup(
                            alignment = Alignment.BottomCenter,
                            offset = IntOffset(0, verticalOffsetPx),
                            properties = popupProps,
                        ) {
                            Box(modifier = Modifier.onGloballyPositioned {
                                if (it.size.width > 0) releaseBtnWidthPx = it.size.width
                            }) {
                                KindBtn(
                                    text = "릴리즈",
                                    baseColor = cs.secondary,
                                    highlighted = isReleaseFocused,
                                    flashDir = com.bridgeone.app.ui.common.swipe.Direction.DOWN,
                                    onClick = if (inputMode == InputMode.NORMAL) ({ selectStepKind(MacroStepKind.RELEASE, popCode, popMod) }) else null
                                )
                            }
                        }
                    } else {
                        Popup(
                            alignment = Alignment.CenterStart,
                            offset = IntOffset(releaseOffsetX, 0),
                            properties = popupProps,
                        ) {
                            Box(modifier = Modifier.onGloballyPositioned {
                                if (it.size.width > 0) releaseBtnWidthPx = it.size.width
                            }) {
                                KindBtn(
                                    text = "릴리즈",
                                    baseColor = cs.secondary,
                                    highlighted = isReleaseFocused,
                                    flashDir = com.bridgeone.app.ui.common.swipe.Direction.RIGHT,
                                    onClick = if (inputMode == InputMode.NORMAL) ({ selectStepKind(MacroStepKind.RELEASE, popCode, popMod) }) else null
                                )
                            }
                        }
                    }
                }
            }
        }
        if (inputMode == InputMode.SWIPE) {
            SwipeFocusable(
                element = element, scope = EdgeEditorScope.MacroPopup,
                shape = RoundedCornerShape(3.dp), showBorderHighlight = false,
                onActivate = onTap, gridRow = rowIndex,
                modifier = weightMod
                    .alpha(if (dimmed) 0.3f else 1f)
                    .onGloballyPositioned {
                        if (it.size.width > 0) keyCellWidthPx = it.size.width
                        cellLeftPx = it.boundsInWindow().left
                    },
            ) {
                val focused = LocalSwipeFocused.current
                Box(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 1.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .then(if (focused) Modifier.border(1.dp, Color.White, RoundedCornerShape(3.dp)) else Modifier)
                        .background(bgColor(focused)).padding(vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(def.label, fontSize = 10.sp, color = textColor(focused), maxLines = 1)
                    // H 셀 점멸 오버레이
                    if (isPopoverCell && flashAlpha.value > 0f && kindFlashTarget == null) {
                        Box(Modifier.matchParentSize().clip(RoundedCornerShape(3.dp))
                            .background(cs.error.copy(alpha = flashAlpha.value)))
                    }
                }
                kindPopup()
            }
        } else {
            Box(
                modifier = weightMod
                    .alpha(if (dimmed) 0.3f else 1f)
                    .onGloballyPositioned {
                        if (it.size.width > 0) keyCellWidthPx = it.size.width
                        cellLeftPx = it.boundsInWindow().left
                    }
                    .padding(horizontal = 1.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(bgColor(false))
                    .clickable(onClick = onTap)
                    .padding(vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(def.label, fontSize = 10.sp, color = textColor(false), maxLines = 1)
                kindPopup()
            }
        }
    }

    // 포커스 wrapper: SWIPE는 SwipeFocusable, NORMAL은 clickable Box
    @Composable
    fun Focusable(
        element: EdgeEditorElement,
        onActivate: () -> Unit,
        gridRow: Int,
        gridCol: Int? = null,
        onActivateAlt: () -> Unit = onActivate,
        showBorder: Boolean = false,
        shape: Shape = RoundedCornerShape(8.dp),
        modifier: Modifier = Modifier,
        content: @Composable (focused: Boolean) -> Unit,
    ) {
        if (inputMode == InputMode.SWIPE) {
            SwipeFocusable(element, scope = EdgeEditorScope.MacroPopup, shape = shape, showBorderHighlight = showBorder, onActivate = onActivate, onActivateAlt = onActivateAlt, gridRow = gridRow, gridCol = gridCol, modifier = modifier) {
                content(LocalSwipeFocused.current)
            }
        } else {
            Box(modifier = modifier.clickable(onClick = onActivate)) { content(false) }
        }
    }

    @Composable
    fun PopupContent() {
        val noPad = androidx.compose.ui.text.TextStyle(
            platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
        )
        Surface(
            modifier = Modifier.fillMaxWidth()
                .then(if (inputMode == InputMode.NORMAL) Modifier.heightIn(max = dialogCardMaxHeight) else Modifier)
                .padding(horizontal = 4.dp),
            shape = RoundedCornerShape(12.dp),
            color = cs.surfaceVariant,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
        ) {
            val outerScroll = if (inputMode == InputMode.NORMAL) Modifier.verticalScroll(rememberScrollState()) else Modifier
            Column(
                modifier = Modifier.fillMaxWidth().then(outerScroll).padding(horizontal = 10.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (currentPage == MacroEditorPage.STEPS) {
                    // ════════ 스텝 구성 상태 ════════
                    // ── 스텝 목록 ──
                    if (draft.steps.isNotEmpty()) {
                        // 픽업&드롭 인터셉터 설치/해제
                        DisposableEffect(pickedStepIndex) {
                            if (pickedStepIndex != null && inputMode == InputMode.SWIPE) {
                                swipeController?.moveInterceptor = { dir ->
                                    val from = pickedStepIndex
                                    if (from == null) {
                                        false
                                    } else {
                                        when (dir) {
                                            com.bridgeone.app.ui.common.swipe.Direction.UP -> {
                                                if (from > 0) { moveStep(from, from - 1); pickedStepIndex = from - 1 }
                                                true
                                            }
                                            com.bridgeone.app.ui.common.swipe.Direction.DOWN -> {
                                                if (from < draft.steps.lastIndex) { moveStep(from, from + 1); pickedStepIndex = from + 1 }
                                                true
                                            }
                                            com.bridgeone.app.ui.common.swipe.Direction.LEFT,
                                            com.bridgeone.app.ui.common.swipe.Direction.RIGHT -> true
                                        }
                                    }
                                }
                            } else {
                                if (pickedGroupId == null) swipeController?.moveInterceptor = null
                            }
                            onDispose { if (pickedGroupId == null) swipeController?.moveInterceptor = null }
                        }
                        // 픽업&드롭: 폴더 그룹 이동 인터셉터 설치/해제
                        // draft도 키에 포함: 이동마다 draft가 바뀌면 인터셉터를 최신 moveGroup으로 재설치
                        DisposableEffect(pickedGroupId, draft) {
                            if (pickedGroupId != null && inputMode == InputMode.SWIPE) {
                                swipeController?.moveInterceptor = { dir ->
                                    val gid = pickedGroupId
                                    if (gid == null) {
                                        false
                                    } else {
                                        when (dir) {
                                            com.bridgeone.app.ui.common.swipe.Direction.UP -> {
                                                moveGroup(gid, true)
                                                swipeController?.setFocus(EdgeEditorElement.MacroGroupDragHandle(gid))
                                                true
                                            }
                                            com.bridgeone.app.ui.common.swipe.Direction.DOWN -> {
                                                moveGroup(gid, false)
                                                swipeController?.setFocus(EdgeEditorElement.MacroGroupDragHandle(gid))
                                                true
                                            }
                                            com.bridgeone.app.ui.common.swipe.Direction.LEFT,
                                            com.bridgeone.app.ui.common.swipe.Direction.RIGHT -> true
                                        }
                                    }
                                }
                            } else {
                                if (pickedStepIndex == null) swipeController?.moveInterceptor = null
                            }
                            onDispose { if (pickedStepIndex == null) swipeController?.moveInterceptor = null }
                        }
                        Box(modifier = Modifier.heightIn(max = maxStepListHeight).onSizeChanged { stepViewportHeight = it.height }) {
                        Column(
                            modifier = Modifier.verticalScroll(stepScrollState, enabled = inputMode == InputMode.NORMAL),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // 세그먼트 계산 (폴더 그룹화 지원)
                            val segments = macroSegments(draft.steps)
                            // 폴더 첫 스텝 인덱스 → (groupId, range) 맵
                            val groupLeaders: Map<Int, Pair<Int, IntRange>> = segments
                                .filter { (gid, _) -> gid != null }
                                .associate { (gid, range) -> range.first to Pair(gid!!, range) }
                            // 접힌 폴더 그룹에 속한 인덱스 (숨김)
                            val hiddenIndices: Set<Int> = segments
                                .filter { (gid, _) -> gid != null && gid in collapsedGroups }
                                .flatMap { (_, range) -> range.toList() }
                                .toSet()
                            draft.steps.forEachIndexed { idx, step ->
                                // 폴더 첫 스텝 앞에 폴더 헤더 Row 렌더
                                groupLeaders[idx]?.let { (gid, range) ->
                                    val isCollapsed = gid in collapsedGroups
                                    val folderName = draft.groupNames[gid] ?: "폴더"
                                    val isFirstGroup = range.first == 0 || draft.steps.getOrNull(range.first - 1)?.groupId != gid
                                    if (isFirstGroup) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
                                                .background(cs.primaryContainer.copy(alpha = 0.2f))
                                                .onSizeChanged { groupHeaderHeights[gid] = it.height }
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                // 접기/펼치기 chevron
                                                val headerElement = EdgeEditorElement.MacroGroupHeader(gid)
                                                val foldAction = { collapsedGroups = if (isCollapsed) collapsedGroups - gid else collapsedGroups + gid }
                                                if (inputMode == InputMode.SWIPE) {
                                                    SwipeFocusable(headerElement, scope = EdgeEditorScope.MacroPopup, shape = RoundedCornerShape(4.dp), showBorderHighlight = false, onActivate = foldAction, gridRow = 100 + idx * 2, gridCol = 0, modifier = Modifier.size(24.dp)) {
                                                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                            Icon(if (isCollapsed) Icons.Filled.KeyboardArrowDown else Icons.Filled.FolderOpen, null, Modifier.size(16.dp), tint = if (LocalSwipeFocused.current) cs.primary else cs.primary.copy(alpha = 0.7f))
                                                        }
                                                    }
                                                } else {
                                                    Box(modifier = Modifier.size(24.dp).clickable(onClick = foldAction), contentAlignment = Alignment.Center) {
                                                        Icon(if (isCollapsed) Icons.Filled.KeyboardArrowDown else Icons.Filled.FolderOpen, null, Modifier.size(16.dp), tint = cs.primary.copy(alpha = 0.7f))
                                                    }
                                                }
                                                // 폴더명 편집 필드
                                                if (inputMode == InputMode.SWIPE) {
                                                    val groupNameCaretTransition = rememberInfiniteTransition(label = "groupNameCaret_$gid")
                                                    val groupNameCaretAlpha by groupNameCaretTransition.animateFloat(
                                                        initialValue = 1f, targetValue = 0f,
                                                        animationSpec = infiniteRepeatable(
                                                            animation = keyframes {
                                                                durationMillis = 1000
                                                                1f at 0; 1f at 500; 0f at 501; 0f at 1000
                                                            },
                                                            repeatMode = RepeatMode.Restart
                                                        ),
                                                        label = "groupNameCaretAlpha_$gid"
                                                    )
                                                    val isEditingThisName = kbTarget == MacroKbTarget.GROUP_NAME && editingGroupNameId == gid
                                                    SwipeFocusable(
                                                        EdgeEditorElement.MacroGroupRename(gid),
                                                        scope = EdgeEditorScope.MacroPopup,
                                                        shape = RoundedCornerShape(4.dp),
                                                        showBorderHighlight = false,
                                                        onActivate = { editingGroupNameId = gid; tempGroupName = draft.groupNames[gid] ?: ""; kbTarget = MacroKbTarget.GROUP_NAME },
                                                        gridRow = 100 + idx * 2, gridCol = 1,
                                                        modifier = Modifier.weight(0.55f)
                                                    ) {
                                                        val renameFieldFocused = LocalSwipeFocused.current
                                                        val showFieldStyle = renameFieldFocused || isEditingThisName
                                                        Box(
                                                            modifier = Modifier.fillMaxWidth().height(24.dp)
                                                                .then(
                                                                    if (showFieldStyle) Modifier
                                                                        .clip(RoundedCornerShape(4.dp))
                                                                        .background(cs.surface.copy(alpha = 0.5f))
                                                                        .border(0.5.dp, cs.outline.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                                                    else Modifier
                                                                )
                                                                .padding(horizontal = 6.dp),
                                                            contentAlignment = Alignment.CenterStart
                                                        ) {
                                                            if (folderName.isEmpty() && !isEditingThisName) {
                                                                Text("폴더 이름...", fontSize = 11.sp, color = cs.primary.copy(alpha = 0.4f), maxLines = 1)
                                                            } else if (isEditingThisName) {
                                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                                    Text(tempGroupName, fontSize = 11.sp, color = cs.primary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                                                                    Text("|", fontSize = 11.sp, color = cs.primary.copy(alpha = groupNameCaretAlpha))
                                                                }
                                                            } else {
                                                                Text(folderName, fontSize = 11.sp, color = cs.primary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth())
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    BasicTextField(
                                                        value = folderName,
                                                        onValueChange = { v -> if (v.length <= 32) onDraftChange(draft.copy(groupNames = draft.groupNames + (gid to v))) },
                                                        singleLine = true,
                                                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = cs.primary),
                                                        cursorBrush = SolidColor(cs.primary),
                                                        modifier = Modifier.weight(1f),
                                                        decorationBox = { inner ->
                                                            Box(
                                                                modifier = Modifier.fillMaxWidth().height(24.dp)
                                                                    .clip(RoundedCornerShape(4.dp))
                                                                    .background(cs.surface.copy(alpha = 0.5f))
                                                                    .border(0.5.dp, cs.outline.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                                                    .padding(horizontal = 6.dp),
                                                                contentAlignment = Alignment.CenterStart
                                                            ) {
                                                                if (folderName.isEmpty()) Text("폴더 이름...", fontSize = 11.sp, color = cs.primary.copy(alpha = 0.4f))
                                                                inner()
                                                            }
                                                        }
                                                    )
                                                }
                                                // 폴더 미니버튼: 위/아래/복제/해제/삭제
                                                val foldMiniBtn: @Composable (icon: ImageVector, label: String, element: EdgeEditorElement, col: Int, enabled: Boolean, onClick: () -> Unit) -> Unit = { icon, _, element, col, enabled, onClick ->
                                                    if (!enabled) {
                                                        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                                                            Icon(icon, null, Modifier.size(12.dp), tint = cs.onSurface.copy(alpha = 0.2f))
                                                        }
                                                    } else if (inputMode == InputMode.SWIPE) {
                                                        SwipeFocusable(element, scope = EdgeEditorScope.MacroPopup, shape = RoundedCornerShape(4.dp), showBorderHighlight = false, onActivate = onClick, gridRow = 100 + idx * 2, gridCol = col, modifier = Modifier.size(24.dp)) {
                                                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                                Icon(icon, null, Modifier.size(12.dp), tint = if (LocalSwipeFocused.current) cs.primary else cs.onSurface.copy(alpha = 0.7f))
                                                            }
                                                        }
                                                    } else {
                                                        var fBounds by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
                                                        Box(modifier = Modifier.size(24.dp).onGloballyPositioned { fBounds = it.boundsInWindow() }.combinedClickable(
                                                            onClick = onClick,
                                                            onLongClick = {
                                                                val lbl = macroButtonLabel(element)
                                                                if (lbl != null) { onSetNormalTooltip(lbl, fBounds) }
                                                            }
                                                        ), contentAlignment = Alignment.Center) {
                                                            Icon(icon, null, Modifier.size(12.dp), tint = cs.onSurface.copy(alpha = 0.7f))
                                                        }
                                                    }
                                                }
                                                if (inputMode == InputMode.SWIPE) {
                                                    val isGroupPicked = pickedGroupId == gid
                                                    val handleElement = EdgeEditorElement.MacroGroupDragHandle(gid)
                                                    SwipeFocusable(handleElement, scope = EdgeEditorScope.MacroPopup, shape = RoundedCornerShape(4.dp), showBorderHighlight = false, onActivate = {
                                                        pickedGroupId = if (isGroupPicked) null else gid
                                                        pickedStepIndex = null
                                                    }, gridRow = 100 + idx * 2, gridCol = 2, modifier = Modifier.size(24.dp)) {
                                                        val focused = LocalSwipeFocused.current
                                                        Box(
                                                            modifier = Modifier.fillMaxSize()
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(if (isGroupPicked) cs.primary.copy(alpha = 0.2f) else Color.Transparent),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(Icons.Filled.DragHandle, null, Modifier.size(12.dp), tint = if (isGroupPicked || focused) cs.primary else cs.onSurface.copy(alpha = 0.7f))
                                                        }
                                                    }
                                                } else {
                                                    foldMiniBtn(Icons.Filled.KeyboardArrowUp, "위로", EdgeEditorElement.MacroGroupUp(gid), 1, range.first > 0, { moveGroup(gid, true) })
                                                    foldMiniBtn(Icons.Filled.KeyboardArrowDown, "아래로", EdgeEditorElement.MacroGroupDown(gid), 2, range.last < draft.steps.lastIndex, { moveGroup(gid, false) })
                                                }
                                                foldMiniBtn(Icons.Filled.ContentCopy, "복제", EdgeEditorElement.MacroGroupDuplicate(gid), 3, true, { duplicateGroup(gid) })
                                                foldMiniBtn(Icons.Filled.FolderSpecial, "폴더 해제", EdgeEditorElement.MacroGroupUngroup(gid), 4, true, { ungroupGroup(gid) })
                                                foldMiniBtn(Icons.Filled.Delete, "삭제", EdgeEditorElement.MacroGroupDelete(gid), 5, true, { deleteGroup(gid) })
                                            }
                                        }
                                    }
                                }
                                // 접힌 그룹 내 스텝은 숨김
                                if (idx in hiddenIndices) return@forEachIndexed
                                val inGroup = step.groupId != null
                                // kind별 표시 레이블 (HOLD/RELEASE는 접두 아이콘 포함)
                                val stepCombo = when (step.kind) {
                                    MacroStepKind.HOLD -> "⬇ ${formatShortcutCombo(step.modifierBits, step.keyCodes).ifEmpty { "홀드" }}"
                                    MacroStepKind.RELEASE -> if (step.keyCodes.isEmpty() && step.modifierBits == 0) "⬆ 전체" else "⬆ ${formatShortcutCombo(step.modifierBits, step.keyCodes)}"
                                    MacroStepKind.TAP -> formatShortcutCombo(step.modifierBits, step.keyCodes).ifEmpty { "(빈 스텝)" }
                                }
                                val repeatBadge = if (step.kind == MacroStepKind.TAP && step.repeatCount > 1) " ×${step.repeatCount}" else ""
                                val stepLabel = stepCombo + repeatBadge
                                // delayText: 딜레이 값만 (접두 "·" 제거, 우측 별도 Text로 표시)
                                val delayText = step.delayAfterMs?.let { "${it}ms" } ?: "공통(${draft.stepDelayMs}ms)"
                                val individualOn = step.delayAfterMs != null
                                val isPicked = pickedStepIndex == idx
                                // 분리/병합 가능 여부 판단
                                val canSplit = step.repeatCount > 1
                                val nextStep = draft.steps.getOrNull(idx + 1)
                                val canMerge = step.kind == MacroStepKind.TAP && step.repeatCount == 1 &&
                                    nextStep != null && nextStep.kind == MacroStepKind.TAP &&
                                    nextStep.modifierBits == step.modifierBits && nextStep.keyCodes == step.keyCodes
                                val splitMergeEnabled = canSplit || canMerge
                                val isSelected = idx in selectedStepIndices
                                val isContiguousGroupReady = selectedStepIndices.size >= 2 && run {
                                    val sorted = selectedStepIndices.sorted()
                                    sorted.zipWithNext().all { (a, b) -> b == a + 1 }
                                }
                                val checkAction = {
                                    val anchor = groupAnchor
                                    when {
                                        anchor == null -> {
                                            groupAnchor = idx
                                            selectedStepIndices = setOf(idx)
                                            ToastController.show("폴더로 묶을 마지막 스텝을 선택하세요", ToastType.INFO)
                                        }
                                        anchor == idx && selectedStepIndices == setOf(idx) -> {
                                            groupAnchor = null
                                            selectedStepIndices = emptySet()
                                        }
                                        else -> {
                                            val range = if (idx >= anchor) anchor..idx else idx..anchor
                                            selectedStepIndices = range.toSet()
                                            if (inputMode == InputMode.SWIPE && range.last > range.first) {
                                                swipeController?.setFocus(EdgeEditorElement.MacroStepGroupConfirm)
                                            }
                                        }
                                    }
                                }
                                Column(
                                    modifier = Modifier.fillMaxWidth()
                                        .then(if (inGroup) Modifier.padding(start = 12.dp) else Modifier)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            when {
                                                isPicked -> cs.primaryContainer.copy(alpha = 0.4f)
                                                isSelected -> cs.primary.copy(alpha = 0.08f)
                                                step.kind == MacroStepKind.HOLD -> cs.tertiary.copy(alpha = 0.08f)
                                                step.kind == MacroStepKind.RELEASE -> cs.secondary.copy(alpha = 0.08f)
                                                else -> cs.surface
                                            }
                                        )
                                        .then(if (isPicked) Modifier.border(1.dp, cs.primary, RoundedCornerShape(6.dp)) else Modifier)
                                        .onSizeChanged { stepItemHeights[idx] = it.height }
                                ) {
                                    // 선택된 범위의 첫 스텝에 "폴더로 묶기" 인라인 버튼 표시
                                    if (isContiguousGroupReady && idx == selectedStepIndices.min()) {
                                        val groupBtnContent: @Composable (Boolean) -> Unit = { focused ->
                                            Box(
                                                modifier = Modifier.fillMaxWidth()
                                                    .background(cs.primary.copy(alpha = if (focused) 0.25f else 0.15f))
                                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Filled.CreateNewFolder, null, Modifier.size(13.dp), tint = cs.primary)
                                                    Spacer(Modifier.width(4.dp))
                                                    Text("${selectedStepIndices.size}개 폴더로 묶기", fontSize = 11.sp, color = cs.primary, style = noPad)
                                                }
                                            }
                                        }
                                        if (inputMode == InputMode.SWIPE) {
                                            SwipeFocusable(
                                                element = EdgeEditorElement.MacroStepGroupConfirm,
                                                scope = EdgeEditorScope.MacroPopup,
                                                shape = RoundedCornerShape(0.dp),
                                                showBorderHighlight = false,
                                                onActivate = { createGroupFromSelected() },
                                                gridRow = 100 + idx * 2,
                                                gridCol = -2,
                                                modifier = Modifier.fillMaxWidth(),
                                            ) { groupBtnContent(LocalSwipeFocused.current) }
                                        } else {
                                            Box(modifier = Modifier.fillMaxWidth().clickable { createGroupFromSelected() }) {
                                                groupBtnContent(false)
                                            }
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        // 항상 표시되는 체크박스 (탭으로 범위 선택)
                                        if (inputMode == InputMode.SWIPE) {
                                            SwipeFocusable(
                                                element = EdgeEditorElement.MacroStepSelectCheck(idx),
                                                scope = EdgeEditorScope.MacroPopup,
                                                shape = RoundedCornerShape(4.dp),
                                                showBorderHighlight = false,
                                                onActivate = checkAction,
                                                gridRow = 100 + idx * 2 + 1,
                                                gridCol = -1,
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                val focused = LocalSwipeFocused.current
                                                Box(
                                                    modifier = Modifier.size(24.dp)
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(if (isSelected) cs.primary.copy(alpha = 0.2f) else Color.Transparent),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        if (isSelected) Icons.Filled.CheckBox else Icons.Outlined.CheckBoxOutlineBlank,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp),
                                                        tint = if (isSelected || focused) cs.primary else cs.onSurface.copy(alpha = 0.5f)
                                                    )
                                                }
                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier.size(24.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .clickable(onClick = checkAction),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    if (isSelected) Icons.Filled.CheckBox else Icons.Outlined.CheckBoxOutlineBlank,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp),
                                                    tint = if (isSelected) cs.primary else cs.onSurface.copy(alpha = 0.5f)
                                                )
                                            }
                                        }
                                        Text("${idx + 1}.", fontSize = 11.sp, color = cs.onSurface.copy(alpha = 0.5f), modifier = Modifier.width(20.dp))
                                        Text(stepLabel, fontSize = 12.sp,
                                            color = when (step.kind) {
                                                MacroStepKind.HOLD -> cs.tertiary
                                                MacroStepKind.RELEASE -> cs.secondary
                                                MacroStepKind.TAP -> cs.onSurface
                                            },
                                            modifier = Modifier.weight(1f), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                        Text(delayText, fontSize = 10.sp, color = cs.onSurface.copy(alpha = 0.45f),
                                            maxLines = 1)
                                        // 미니버튼 헬퍼
                                        val miniBtn: @Composable (icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, bg: Color, enabled: Boolean, onClick: () -> Unit, element: EdgeEditorElement, col: Int) -> Unit = { icon, tint, bg, enabled, onClick, element, col ->
                                            val box: @Composable (Boolean) -> Unit = { focused ->
                                                Box(
                                                    modifier = Modifier.size(28.dp).clip(RoundedCornerShape(4.dp))
                                                        .then(if (focused) Modifier.border(1.dp, cs.primary, RoundedCornerShape(4.dp)) else Modifier)
                                                        .background(bg),
                                                    contentAlignment = Alignment.Center
                                                ) { Icon(icon, null, Modifier.size(14.dp), tint = tint) }
                                            }
                                            if (!enabled) {
                                                Box(modifier = Modifier.size(28.dp).clip(RoundedCornerShape(4.dp)).background(cs.surfaceVariant.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                                                    Icon(icon, null, Modifier.size(14.dp), tint = cs.onSurface.copy(alpha = 0.2f))
                                                }
                                            } else if (inputMode == InputMode.SWIPE) {
                                                SwipeFocusable(element, scope = EdgeEditorScope.MacroPopup, shape = RoundedCornerShape(4.dp), showBorderHighlight = false, onActivate = onClick, gridRow = 100 + idx * 2 + 1, gridCol = col, modifier = Modifier.size(28.dp)) { box(LocalSwipeFocused.current) }
                                            } else {
                                                var btnBounds by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
                                                Box(
                                                    modifier = Modifier
                                                        .onGloballyPositioned { coords ->
                                                            btnBounds = coords.boundsInWindow()
                                                        }
                                                        .combinedClickable(
                                                            onClick = onClick,
                                                            onLongClick = {
                                                                val label = macroButtonLabel(element)
                                                                if (label != null) {
                                                                    onSetNormalTooltip(label, btnBounds)
                                                                }
                                                            }
                                                        )
                                                ) { box(false) }
                                            }
                                        }
                                        if (inputMode == InputMode.SWIPE) {
                                            // SWIPE: 드래그 핸들(col 0) + ⏱(col 1) + 분리/병합(col 2) + 복제(col 3) + ✎(col 4) + 🗑(col 5)
                                            val handleTint = if (isPicked) cs.primary else cs.onSurface.copy(alpha = 0.7f)
                                            val handleBg = if (isPicked) cs.primary.copy(alpha = 0.2f) else cs.surfaceVariant
                                            miniBtn(Icons.Filled.DragHandle, handleTint, handleBg, true, {
                                                pickedStepIndex = if (isPicked) null else idx
                                            }, EdgeEditorElement.MacroStepDragHandle(idx), 0)
                                        } else {
                                            // NORMAL: ↑(col 0) / ↓(col 1) / ⏱(col 2) / 분리/병합(col 3) / 복제(col 4) / ✎(col 5) / 🗑(col 6)
                                            // 그룹 내 스텝: 경계(groupRange) 내로 이동 제한
                                            val groupBound = if (inGroup) groupRange(draft.steps, idx) else null
                                            val canMoveUp = if (groupBound != null) idx > groupBound.first else idx > 0
                                            val canMoveDown = if (groupBound != null) idx < groupBound.last else idx < draft.steps.lastIndex
                                            miniBtn(Icons.Filled.KeyboardArrowUp, cs.onSurface.copy(alpha = 0.7f), cs.surfaceVariant, canMoveUp, { moveStep(idx, idx - 1) }, EdgeEditorElement.MacroStepUp(idx), 0)
                                            miniBtn(Icons.Filled.KeyboardArrowDown, cs.onSurface.copy(alpha = 0.7f), cs.surfaceVariant, canMoveDown, { moveStep(idx, idx + 1) }, EdgeEditorElement.MacroStepDown(idx), 1)
                                        }
                                        // ⏱ 개별 딜레이 ON/OFF 토글
                                        val delayBtnBg = if (individualOn) cs.primary.copy(alpha = 0.15f) else cs.surfaceVariant
                                        val delayBtnTint = if (individualOn) cs.primary else cs.onSurface.copy(alpha = 0.7f)
                                        val timerCol = if (inputMode == InputMode.SWIPE) 1 else 2
                                        miniBtn(Icons.Filled.Timer, delayBtnTint, delayBtnBg, true, {
                                            val newSteps = draft.steps.toMutableList()
                                            if (individualOn) {
                                                newSteps[idx] = step.copy(delayAfterMs = null)
                                                onDraftChange(draft.copy(steps = newSteps))
                                            } else {
                                                newSteps[idx] = step.copy(delayAfterMs = draft.stepDelayMs)
                                                onDraftChange(draft.copy(steps = newSteps))
                                                if (inputMode == InputMode.SWIPE)
                                                    swipeController?.setFocus(EdgeEditorElement.MacroStepDelaySlider(idx))
                                            }
                                        }, EdgeEditorElement.MacroStepDelayExpand(idx), timerCol)
                                        // 분리/병합 버튼
                                        val splitMergeCol = if (inputMode == InputMode.SWIPE) 2 else 3
                                        val splitMergeIcon = if (canSplit) Icons.Filled.CallSplit else Icons.Filled.CallMerge
                                        val splitMergeTint = if (splitMergeEnabled) cs.onSurface.copy(alpha = 0.7f) else cs.onSurface.copy(alpha = 0.2f)
                                        miniBtn(splitMergeIcon, splitMergeTint, cs.surfaceVariant, splitMergeEnabled, {
                                            val newSteps = draft.steps.toMutableList()
                                            if (canSplit) {
                                                // 분리: repeatCount개의 독립 스텝으로 펼침
                                                val expanded = List(step.repeatCount) { step.copy(repeatCount = 1) }
                                                newSteps.removeAt(idx)
                                                newSteps.addAll(idx, expanded)
                                            } else if (canMerge && nextStep != null) {
                                                // 병합: 현재 + 다음 스텝 repeatCount 합산
                                                newSteps[idx] = step.copy(repeatCount = step.repeatCount + nextStep.repeatCount)
                                                newSteps.removeAt(idx + 1)
                                            }
                                            onDraftChange(draft.copy(steps = newSteps))
                                        }, EdgeEditorElement.MacroStepSplitMerge(idx), splitMergeCol)
                                        // 복제 버튼
                                        val dupCol = if (inputMode == InputMode.SWIPE) 3 else 4
                                        miniBtn(Icons.Filled.ContentCopy, cs.onSurface.copy(alpha = 0.7f), cs.surfaceVariant, true, {
                                            val newSteps = draft.steps.toMutableList()
                                            newSteps.add(idx + 1, step.copy())
                                            onDraftChange(draft.copy(steps = newSteps))
                                        }, EdgeEditorElement.MacroStepDuplicate(idx), dupCol)
                                        val editCol = if (inputMode == InputMode.SWIPE) 4 else 5
                                        val deleteCol = if (inputMode == InputMode.SWIPE) 5 else 6
                                        miniBtn(Icons.Filled.Edit, cs.onSurface.copy(alpha = 0.7f), cs.surfaceVariant, true, { startEditStep(idx) }, EdgeEditorElement.MacroStepChip(idx), editCol)
                                        miniBtn(Icons.Filled.Delete, cs.error, cs.error.copy(alpha = 0.1f), true, { onDraftChange(draft.copy(steps = draft.steps.toMutableList().also { it.removeAt(idx) })) }, EdgeEditorElement.MacroStepDelete(idx), deleteCol)
                                    }
                                    // ── 인라인 개별 딜레이 슬라이더 (⏱ ON 시 표시) ──
                                    if (individualOn) {
                                        MacroDelaySliderRow(
                                            value = step.delayAfterMs ?: draft.stepDelayMs,
                                            onValueChange = { newDelay ->
                                                val newSteps = draft.steps.toMutableList()
                                                newSteps[idx] = step.copy(delayAfterMs = newDelay)
                                                onDraftChange(draft.copy(steps = newSteps))
                                            },
                                            element = EdgeEditorElement.MacroStepDelaySlider(idx),
                                            inputMode = inputMode,
                                            gridRow = 100 + idx * 2 + 1,
                                            gridCol = 6,
                                        )
                                    }
                                }
                            }
                        }
                        } // Box (스텝 목록 스크롤 컨테이너)
                    }

                    // ── 스텝 추가 주 버튼 (row 5000): 단일 스텝 작성 ──
                    Focusable(EdgeEditorElement.MacroAddStep, { startAddStep() }, gridRow = 5000, modifier = Modifier.fillMaxWidth()) { focused ->
                        Box(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                .then(if (focused) Modifier.border(1.dp, cs.primary, RoundedCornerShape(8.dp)) else Modifier)
                                .background(cs.primary.copy(alpha = 0.15f)).padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Add, null, Modifier.size(16.dp), tint = cs.primary)
                                Spacer(Modifier.width(4.dp))
                                Text("스텝 추가", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = cs.primary, style = noPad)
                            }
                        }
                    }
                    // ── 일괄 추가 보조 칩 행 (row 5001): 여러 스텝을 한 번에 생성하는 방법들 ──
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Focusable(EdgeEditorElement.MacroStepModeText, { startAddText() }, gridRow = 5001, gridCol = 0, modifier = Modifier.weight(1f)) { focused ->
                            Box(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                    .then(if (focused) Modifier.border(1.dp, cs.primary, RoundedCornerShape(8.dp)) else Modifier)
                                    .background(cs.surfaceVariant.copy(alpha = 0.5f)).padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Keyboard, null, Modifier.size(12.dp), tint = cs.onSurface.copy(alpha = 0.65f))
                                    Spacer(Modifier.width(4.dp))
                                    Text("문자열로 일괄 추가", fontSize = 11.sp, color = cs.onSurface.copy(alpha = 0.65f), style = noPad)
                                }
                            }
                        }
                        Focusable(EdgeEditorElement.MacroAddFromShortcut, { startAddShortcutPick() }, gridRow = 5001, gridCol = 1, modifier = Modifier.weight(1f)) { focused ->
                            Box(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                    .then(if (focused) Modifier.border(1.dp, cs.primary, RoundedCornerShape(8.dp)) else Modifier)
                                    .background(cs.surfaceVariant.copy(alpha = 0.5f)).padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.ContentPaste, null, Modifier.size(12.dp), tint = cs.onSurface.copy(alpha = 0.65f))
                                    Spacer(Modifier.width(4.dp))
                                    Text("프리셋 조합", fontSize = 11.sp, color = cs.onSurface.copy(alpha = 0.65f), style = noPad)
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = cs.outline.copy(alpha = 0.2f))

                    // ── 하단 버튼 바 (row 5002): 취소 / 다음 (저장 설정 페이지로) ──
                    val onGoFinalize: () -> Unit = {
                        if (draft.steps.isEmpty()) {
                            ToastController.show("스텝을 하나 이상 추가하세요", ToastType.ERROR)
                        } else {
                            currentPage = MacroEditorPage.FINALIZE
                            if (inputMode == InputMode.SWIPE) swipeController?.setFocus(EdgeEditorElement.MacroDelaySlider)
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Focusable(EdgeEditorElement.MacroPopupCancel, onCancel, gridRow = 5002, gridCol = 0, modifier = Modifier.weight(1f)) { focused ->
                            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(if (focused) cs.error else Color.Transparent).padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                Text("취소", fontSize = 13.sp, color = if (focused) cs.onError else cs.error)
                            }
                        }
                        Focusable(EdgeEditorElement.MacroGoFinalize, onGoFinalize, gridRow = 5002, gridCol = 1, modifier = Modifier.weight(1f)) { focused ->
                            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(if (focused) cs.primary else Color.Transparent).padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                Text("다음 →", fontSize = 13.sp, color = if (focused) cs.onPrimary else cs.primary)
                            }
                        }
                    }
                } else if (currentPage == MacroEditorPage.FINALIZE) {
                    // ════════ 저장 설정 상태 ════════
                    // ── 매크로 옵션: 공통 딜레이 + 한글 모드 ──
                    Text("스텝 간 공통 딜레이", fontSize = 12.sp, color = cs.onSurface.copy(alpha = 0.7f))
                    MacroDelaySliderRow(
                        value = draft.stepDelayMs,
                        onValueChange = { onDraftChange(draft.copy(stepDelayMs = it)) },
                        element = EdgeEditorElement.MacroDelaySlider,
                        inputMode = inputMode,
                        gridRow = 52,
                    )
                    HorizontalDivider(color = cs.outline.copy(alpha = 0.2f))

                    // ── 한/영 상태 확인 다이얼로그 3-옵션 선택 (row 55) ──
                    Text("실행 전 입력 모드 확인", fontSize = 12.sp, color = cs.onSurface.copy(alpha = 0.7f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(cs.surface),
                    ) {
                        listOf(
                            Triple(InputModeCheck.NONE,    "없음", EdgeEditorElement.MacroImeCheckNone as EdgeEditorElement),
                            Triple(InputModeCheck.KOREAN,  "한글", EdgeEditorElement.MacroImeCheckKorean),
                            Triple(InputModeCheck.ENGLISH, "영어", EdgeEditorElement.MacroImeCheckEnglish),
                        ).forEachIndexed { idx, (mode, label, element) ->
                            val selected = draft.inputModeCheck == mode
                            val onSelect = { onDraftChange(draft.copy(inputModeCheck = mode)) }
                            if (inputMode == InputMode.SWIPE) {
                                SwipeFocusable(
                                    element = element,
                                    scope = EdgeEditorScope.MacroPopup,
                                    shape = RoundedCornerShape(6.dp),
                                    showBorderHighlight = false,
                                    gridRow = 55,
                                    gridCol = idx,
                                    onActivate = onSelect,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    val focused = LocalSwipeFocused.current
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(when { selected -> cs.primary; focused -> cs.primary.copy(alpha = 0.15f); else -> Color.Transparent })
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(label, fontSize = 12.sp, color = if (selected) cs.onPrimary else cs.onSurface)
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (selected) cs.primary else Color.Transparent)
                                        .clickable(onClick = onSelect)
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(label, fontSize = 12.sp, color = if (selected) cs.onPrimary else cs.onSurface)
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = cs.outline.copy(alpha = 0.2f))

                    // ── 전체 키 강제 해제 스텝 추가 체크박스 (row 53) ──
                    val hasForceReleaseAtEnd = draft.steps.lastOrNull()?.let { last ->
                        last.keyCodes.isEmpty() && last.modifierBits == 0 && last.kind == MacroStepKind.RELEASE
                    } ?: false
                    val onToggleForceRelease: () -> Unit = {
                        if (hasForceReleaseAtEnd) {
                            onDraftChange(draft.copy(steps = draft.steps.dropLast(1)))
                        } else {
                            onDraftChange(draft.copy(steps = draft.steps + MacroStep(
                                modifierBits = 0,
                                keyCodes = emptyList(),
                                delayAfterMs = null,
                                repeatCount = 1,
                                kind = MacroStepKind.RELEASE,
                            )))
                        }
                    }
                    Focusable(
                        element = EdgeEditorElement.MacroForceReleaseToggle,
                        onActivate = onToggleForceRelease,
                        gridRow = 53,
                    ) { focused ->
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .then(if (focused) Modifier.border(1.dp, cs.primary, RoundedCornerShape(8.dp)) else Modifier)
                                .background(if (focused) cs.primary.copy(alpha = 0.08f) else Color.Transparent)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(
                                modifier = Modifier.size(18.dp).clip(RoundedCornerShape(3.dp))
                                    .background(if (hasForceReleaseAtEnd) cs.primary else Color.Transparent)
                                    .border(1.5.dp, if (hasForceReleaseAtEnd) cs.primary else cs.outline, RoundedCornerShape(3.dp)),
                                contentAlignment = Alignment.Center,
                            ) {}
                            Text(
                                "전체 키 강제 해제 스텝 추가",
                                fontSize = 13.sp,
                                color = if (focused) cs.primary else cs.onSurface,
                            )
                        }
                    }

                    HorizontalDivider(color = cs.outline.copy(alpha = 0.2f))

                    // ── 명명 섹션: 이름과 아이콘 (row 57) ──
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val iconBox: @Composable () -> Unit = {
                            Box(
                                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(cs.surface)
                                    .border(0.5.dp, cs.outline.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .onGloballyPositioned { iconBtnCenter = it.boundsInWindow().center },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (draftIconKey.isNotEmpty()) IconRegistry.get(draftIconKey) else Icons.Filled.Keyboard,
                                    contentDescription = "아이콘 선택",
                                    modifier = Modifier.size(if (draftIconKey.isNotEmpty()) 22.dp else 18.dp),
                                    tint = if (draftIconKey.isNotEmpty()) cs.onSurface else cs.onSurface.copy(alpha = 0.35f),
                                )
                            }
                        }
                        if (inputMode == InputMode.SWIPE) {
                            SwipeFocusable(EdgeEditorElement.MacroIconButton, scope = EdgeEditorScope.MacroPopup, shape = RoundedCornerShape(8.dp), showBorderHighlight = true, onActivate = { onRequestIconPicker?.invoke(draftIconKey, iconBtnCenter) { draftIconKey = it } }, gridRow = 57, gridCol = 0) { iconBox() }
                        } else {
                            Box(modifier = Modifier.clickable { normalIconSheetVisible = true }) { iconBox() }
                        }
                        if (inputMode == InputMode.SWIPE) {
                            SwipeFocusable(EdgeEditorElement.MacroNameField, scope = EdgeEditorScope.MacroPopup, shape = RoundedCornerShape(8.dp), showBorderHighlight = true, onActivate = { kbTarget = MacroKbTarget.NAME }, gridRow = 57, gridCol = 1, modifier = Modifier.weight(1f)) {
                                Box(modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(8.dp)).background(cs.surface).border(0.5.dp, cs.outline.copy(alpha = 0.4f), RoundedCornerShape(8.dp)).padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
                                    val nameCaretTransition = rememberInfiniteTransition(label = "nameCaret")
                                    val nameCaretAlpha by nameCaretTransition.animateFloat(
                                        initialValue = 1f, targetValue = 0f,
                                        animationSpec = infiniteRepeatable(
                                            animation = keyframes {
                                                durationMillis = 1000
                                                1f at 0; 1f at 500; 0f at 501; 0f at 1000
                                            },
                                            repeatMode = RepeatMode.Restart
                                        ),
                                        label = "nameCaretAlpha"
                                    )
                                    val nameKbActive = kbTarget == MacroKbTarget.NAME
                                    if (draftName.isEmpty() && !nameKbActive) {
                                        Text("매크로 이름 입력...", fontSize = 14.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, color = cs.onSurfaceVariant.copy(alpha = 0.5f))
                                    } else {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(draftName, fontSize = 14.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, color = cs.onSurface, modifier = Modifier.weight(1f, fill = false))
                                            if (nameKbActive) {
                                                Text("|", fontSize = 14.sp, color = cs.primary.copy(alpha = nameCaretAlpha))
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            BasicTextField(
                                value = draftName, onValueChange = { if (it.length <= 32) draftName = it }, singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = cs.onSurface),
                                cursorBrush = SolidColor(cs.primary), modifier = Modifier.weight(1f),
                                decorationBox = { inner ->
                                    Box(modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(8.dp)).background(cs.surface).border(0.5.dp, cs.outline.copy(alpha = 0.4f), RoundedCornerShape(8.dp)).padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
                                        if (draftName.isEmpty()) Text("매크로 이름 입력...", fontSize = 14.sp, color = cs.onSurfaceVariant.copy(alpha = 0.5f))
                                        inner()
                                    }
                                }
                            )
                        }
                    }

                    HorizontalDivider(color = cs.outline.copy(alpha = 0.2f))

                    // ── 최종 버튼 바 (row 60): 뒤로 / 완료 ──
                    // 완료: 존 적용 + 프리셋 저장 동시 수행 (onAddAsPreset null이면 스킵)
                    val onConfirmGuarded: () -> Unit = {
                        when {
                            draft.steps.isEmpty() -> ToastController.show("스텝을 하나 이상 추가하세요", ToastType.ERROR)
                            draft.steps.endsWithDanglingHold() -> ToastController.show("모든 키가 해제된 상태로 매크로가 끝나도록 설계하세요", ToastType.ERROR)
                            draftName.isBlank() -> ToastController.show("매크로 이름을 입력해주세요", ToastType.WARNING)
                            else -> {
                                onAddAsPreset?.invoke(draft, draftIconKey, draftName.trim())
                                onConfirm(draft, draftIconKey, draftName.trim())
                            }
                        }
                    }
                    val onGoBackToSteps: () -> Unit = {
                        currentPage = MacroEditorPage.STEPS
                        if (inputMode == InputMode.SWIPE) swipeController?.setFocus(EdgeEditorElement.MacroAddStep)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Focusable(EdgeEditorElement.MacroGoBackToSteps, onGoBackToSteps, gridRow = 60, gridCol = 0, modifier = Modifier.weight(1f)) { focused ->
                            Box(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                    .then(if (focused) Modifier.border(1.dp, cs.outline, RoundedCornerShape(8.dp)) else Modifier)
                                    .background(if (focused) cs.surface else Color.Transparent).padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("← 뒤로", fontSize = 13.sp, color = cs.onSurface.copy(alpha = 0.7f))
                            }
                        }
                        Focusable(EdgeEditorElement.MacroPopupConfirm, onConfirmGuarded, gridRow = 60, gridCol = 1, modifier = Modifier.weight(1f)) { focused ->
                            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(if (focused) cs.primary else Color.Transparent).padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                Text("완료", fontSize = 13.sp, color = if (focused) cs.onPrimary else cs.primary)
                            }
                        }
                    }
                } else {
                    // ════════ 입력 상태 ════════
                    // 키 종류 팝오버 moveInterceptor: 팝오버 열린 동안 H 중앙↔버튼 방향 이동
                    // 핵심 모델:
                    //  - H(null) 상태: 방향에 버튼 있으면 포커스 이동, 없으면 경계 점멸+햅틱
                    //  - 버튼 상태: 반대 방향이면 H 복귀, 그 외 경계 점멸+햅틱
                    //  - DOWN→닫기 없음. 닫기는 H 재탭(onTap)으로만. 항상 true 반환해 이탈 차단.
                    val kindPopoverActive = stepEditMode == MacroStepEditMode.SINGLE_KEY &&
                        (stepKindPopoverCode != null || stepKindPopoverModBit != 0)
                    val haptic = LocalHapticFeedback.current
                    DisposableEffect(kindPopoverActive) {
                        if (kindPopoverActive && inputMode == InputMode.SWIPE) {
                            swipeController?.moveInterceptor = { dir ->
                                // opposite: 반대 방향 반환
                                fun opposite(d: com.bridgeone.app.ui.common.swipe.Direction): com.bridgeone.app.ui.common.swipe.Direction = when (d) {
                                    com.bridgeone.app.ui.common.swipe.Direction.UP -> com.bridgeone.app.ui.common.swipe.Direction.DOWN
                                    com.bridgeone.app.ui.common.swipe.Direction.DOWN -> com.bridgeone.app.ui.common.swipe.Direction.UP
                                    com.bridgeone.app.ui.common.swipe.Direction.LEFT -> com.bridgeone.app.ui.common.swipe.Direction.RIGHT
                                    com.bridgeone.app.ui.common.swipe.Direction.RIGHT -> com.bridgeone.app.ui.common.swipe.Direction.LEFT
                                }
                                // 현재 팝오버의 유효 방향 집합 (락 키면 UP만, 아니면 UP+holdDir+releaseDir)
                                val validDirs: Set<com.bridgeone.app.ui.common.swipe.Direction> = if (popoverIsLock)
                                    setOf(com.bridgeone.app.ui.common.swipe.Direction.UP)
                                    else setOf(com.bridgeone.app.ui.common.swipe.Direction.UP, popoverHoldDir, popoverReleaseDir)
                                val cur = kindFocusDir
                                if (cur == null) {
                                    // H 중앙 상태
                                    if (dir in validDirs) {
                                        kindFocusDir = dir
                                    } else {
                                        // 버튼 없는 방향 → H 점멸+햅틱
                                        kindFlashTarget = null
                                        kindFlashKey++
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                } else {
                                    // 버튼 포커스 상태
                                    if (dir == opposite(cur)) {
                                        // 반대 방향 → H 복귀
                                        kindFocusDir = null
                                    } else {
                                        // 그 외 방향 → 현재 버튼 점멸+햅틱
                                        kindFlashTarget = cur
                                        kindFlashKey++
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                }
                                true // 항상 소비: 다른 키로 이탈 차단
                            }
                        } else {
                            swipeController?.moveInterceptor = null
                        }
                        onDispose { swipeController?.moveInterceptor = null; kindFocusDir = null }
                    }
                    // 스와이프 모드 반복 팝업 moveInterceptor: repeatPopupOpen 시 방향을 가로채 반복 횟수 조절
                    DisposableEffect(repeatPopupOpen) {
                        if (repeatPopupOpen && inputMode == InputMode.SWIPE) {
                            swipeController?.moveInterceptor = { dir ->
                                when (dir) {
                                    com.bridgeone.app.ui.common.swipe.Direction.RIGHT,
                                    com.bridgeone.app.ui.common.swipe.Direction.UP ->
                                        stepDraftRepeat = (stepDraftRepeat + 1).coerceAtMost(99)
                                    com.bridgeone.app.ui.common.swipe.Direction.LEFT,
                                    com.bridgeone.app.ui.common.swipe.Direction.DOWN ->
                                        stepDraftRepeat = (stepDraftRepeat - 1).coerceAtLeast(1)
                                }
                                true
                            }
                        } else {
                            swipeController?.moveInterceptor = null
                        }
                        onDispose { swipeController?.moveInterceptor = null }
                    }
                    // ── 모드 세그먼트 탭 (row 1): 키/단축키/피커 모드 탭 (TEXT·PICK 모드일 때 숨김) ──
                    if (stepEditMode != MacroStepEditMode.TEXT && stepEditMode != MacroStepEditMode.PICK) {
                        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(cs.surface)) {
                            val tabs = listOf(
                                Triple("단일 키", EdgeEditorElement.MacroStepModeSingleKey, MacroStepEditMode.SINGLE_KEY),
                                Triple("단축키", EdgeEditorElement.MacroStepModeCombo, MacroStepEditMode.COMBO),
                            )
                            tabs.forEachIndexed { i, triple ->
                                val (label, element, mode) = triple
                                val selected = stepEditMode == mode
                                val tabContent: @Composable (Boolean) -> Unit = { focused ->
                                    Box(
                                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                            .then(if (focused) Modifier.border(1.dp, cs.primary, RoundedCornerShape(8.dp)) else Modifier)
                                            .background(if (selected) cs.primary else Color.Transparent).padding(vertical = 9.dp),
                                        contentAlignment = Alignment.Center
                                    ) { Text(label, fontSize = 12.sp, color = if (selected) cs.onPrimary else cs.onSurface, style = noPad) }
                                }
                                if (inputMode == InputMode.SWIPE) {
                                    SwipeFocusable(element, scope = EdgeEditorScope.MacroPopup, shape = RoundedCornerShape(8.dp), showBorderHighlight = false, onActivate = { selectStepMode(mode) }, gridRow = 1, gridCol = i, modifier = Modifier.weight(1f)) { tabContent(LocalSwipeFocused.current) }
                                } else {
                                    Box(modifier = Modifier.weight(1f).clickable { selectStepMode(mode) }) { tabContent(false) }
                                }
                            }
                        }
                    }

                    if (stepEditMode == MacroStepEditMode.PICK) {
                        // ── 단축키 피커 ──
                        Text("탭하여 스텝으로 추가 (연속 추가 가능)", fontSize = 11.sp, color = cs.onSurface.copy(alpha = 0.5f))
                        val allShortcutsForPick: List<Pair<String, MacroStep>> = remember(customShortcutPresets) {
                            buildList {
                                DEFAULT_SHORTCUTS.forEach { s ->
                                    add(s.label to MacroStep(
                                        modifierBits = s.combinedModifiers.toInt(),
                                        keyCodes = if (s.key.toInt() != 0) listOf(s.key.toInt()) else emptyList(),
                                    ))
                                }
                                customShortcutPresets.forEach { p ->
                                    val combo = formatShortcutCombo(p.modifierBits, p.keyCodes)
                                    val label = p.name.ifEmpty { combo }
                                    add(label to MacroStep(modifierBits = p.modifierBits, keyCodes = p.keyCodes))
                                }
                            }
                        }
                        // ── 3열 그리드 (행 단위 Row, 마지막 행 빈 칸 Spacer 채움) ──
                        Column(
                            modifier = Modifier
                                .heightIn(max = 200.dp)
                                .verticalScroll(pickScrollState)
                                .onSizeChanged { pickViewportHeight = it.height },
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            allShortcutsForPick.chunked(3).forEachIndexed { rowIdx, rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    rowItems.forEachIndexed { colIdx, (label, step) ->
                                        val i = rowIdx * 3 + colIdx
                                        val chipContent: @Composable (Boolean) -> Unit = { focused ->
                                            Box(
                                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
                                                    .then(if (focused) Modifier.border(1.dp, cs.primary, RoundedCornerShape(6.dp)) else Modifier)
                                                    .background(cs.surface).padding(horizontal = 8.dp, vertical = 10.dp),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Text(label, fontSize = 12.sp, color = cs.onSurface, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                            }
                                        }
                                        val onPick: () -> Unit = {
                                            pickStagedSteps = pickStagedSteps + (label to step)
                                            ToastController.show("'${label}' 추가됨", ToastType.SUCCESS)
                                        }
                                        if (inputMode == InputMode.SWIPE) {
                                            SwipeFocusable(EdgeEditorElement.MacroShortcutPick(i), scope = EdgeEditorScope.MacroPopup, shape = RoundedCornerShape(6.dp), showBorderHighlight = false, onActivate = onPick, gridRow = 2 + rowIdx, gridCol = colIdx, modifier = Modifier.weight(1f)) { chipContent(LocalSwipeFocused.current) }
                                        } else {
                                            Box(modifier = Modifier.weight(1f).clickable(onClick = onPick)) { chipContent(false) }
                                        }
                                    }
                                    // 마지막 행이 3개 미만이면 빈 칸으로 채워 정렬 유지
                                    repeat(3 - rowItems.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    } else if (stepEditMode == MacroStepEditMode.TEXT) {
                        // ── 문자열 입력 ──
                        Text("입력한 문자열이 순서대로 스텝이 됩니다 (한글 자동 지원)", fontSize = 11.sp, color = cs.onSurface.copy(alpha = 0.5f))
                        if (inputMode == InputMode.SWIPE) {
                            val stepTextCaretTransition = rememberInfiniteTransition(label = "stepTextCaret")
                            val stepTextCaretAlpha by stepTextCaretTransition.animateFloat(
                                initialValue = 1f, targetValue = 0f,
                                animationSpec = infiniteRepeatable(
                                    animation = keyframes {
                                        durationMillis = 1000
                                        1f at 0; 1f at 500; 0f at 501; 0f at 1000
                                    },
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "stepTextCaretAlpha"
                            )
                            val isStepTextKbActive = kbTarget == MacroKbTarget.STEP_TEXT
                            SwipeFocusable(EdgeEditorElement.MacroTextField, scope = EdgeEditorScope.MacroPopup, shape = RoundedCornerShape(8.dp), showBorderHighlight = true, onActivate = { kbTarget = MacroKbTarget.STEP_TEXT }, gridRow = 2, gridCol = 0, modifier = Modifier.fillMaxWidth()) {
                                Box(modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(8.dp)).background(cs.surface).border(0.5.dp, cs.outline.copy(alpha = 0.4f), RoundedCornerShape(8.dp)).padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
                                    if (stepTextInput.isEmpty() && !isStepTextKbActive) {
                                        Text("문자열 입력...", fontSize = 14.sp, maxLines = 1, color = cs.onSurfaceVariant.copy(alpha = 0.5f))
                                    } else {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(stepTextInput, fontSize = 14.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, color = cs.onSurface, modifier = Modifier.weight(1f, fill = false))
                                            if (isStepTextKbActive) {
                                                Text("|", fontSize = 14.sp, color = cs.primary.copy(alpha = stepTextCaretAlpha))
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            BasicTextField(
                                value = stepTextInput, onValueChange = { stepTextInput = it }, singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = cs.onSurface),
                                cursorBrush = SolidColor(cs.primary), modifier = Modifier.fillMaxWidth(),
                                decorationBox = { inner ->
                                    Box(modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(8.dp)).background(cs.surface).border(0.5.dp, cs.outline.copy(alpha = 0.4f), RoundedCornerShape(8.dp)).padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
                                        if (stepTextInput.isEmpty()) Text("문자열 입력...", fontSize = 14.sp, color = cs.onSurfaceVariant.copy(alpha = 0.5f))
                                        inner()
                                    }
                                }
                            )
                        }
                    } else {
                        // ── 단일 키 / 단축키 키보드 ──
                        val stepKeyLabelMap: Map<Int, String> = remember {
                            (kbRows + navRows).flatten()
                                .filter { it.code != 0 && it.modBit == 0 }
                                .associate { it.code to it.label }
                        }
                        val stepModifierDefs = listOf(0x01 to "Ctrl", 0x02 to "Shift", 0x04 to "Alt", 0x08 to "Win")
                        // 단일 키 모드: 동작 타입 아이콘/색상 (스텝 목록과 동일 규칙)
                        val isSingleKeyMode = stepEditMode == MacroStepEditMode.SINGLE_KEY
                        val kindColor: Color = when {
                            !isSingleKeyMode || stepDraftKind == null -> cs.onSurface
                            stepDraftKind == MacroStepKind.HOLD -> cs.tertiary
                            stepDraftKind == MacroStepKind.RELEASE -> cs.secondary
                            else -> cs.onSurface
                        }
                        val kindPrefix: String = when {
                            !isSingleKeyMode || stepDraftKind == null -> ""
                            stepDraftKind == MacroStepKind.HOLD -> "⬇ "
                            stepDraftKind == MacroStepKind.RELEASE -> "⬆ "
                            else -> ""
                        }
                        val stepComboParts: List<String> = buildList {
                            for ((bit, label) in stepModifierDefs) {
                                if (stepDraftMod and bit != 0) add(label)
                            }
                            for (code in stepDraftKeys) {
                                stepKeyLabelMap[code]?.let { add(it) }
                            }
                        }
                        // 칩 배경/테두리: 동작 타입 선택 시 틴트, 미선택 시 중립
                        val chipBg: Color = if (isSingleKeyMode && stepDraftKind != null && stepDraftKind != MacroStepKind.TAP)
                            kindColor.copy(alpha = 0.12f) else cs.surfaceVariant
                        val chipBorder: Color = if (isSingleKeyMode && stepDraftKind != null && stepDraftKind != MacroStepKind.TAP)
                            kindColor.copy(alpha = 0.5f) else cs.outline.copy(alpha = 0.5f)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(cs.surface),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (stepComboParts.isEmpty()) {
                                    Text(
                                        "키를 눌러 선택하세요",
                                        fontSize = 12.sp,
                                        color = cs.onSurface.copy(alpha = 0.35f)
                                    )
                                } else {
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        stepComboParts.forEachIndexed { i, label ->
                                            if (i > 0) {
                                                Text(
                                                    "+",
                                                    fontSize = 11.sp,
                                                    color = cs.onSurface.copy(alpha = 0.5f)
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .border(1.dp, chipBorder, RoundedCornerShape(4.dp))
                                                    .background(chipBg)
                                                    .padding(horizontal = 7.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    "$kindPrefix$label",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = cs.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            kbRows.forEachIndexed { rowIdx, row ->
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    row.forEach { key -> MacroKeyCell(key, rowIdx + 2, Modifier.weight(key.weight)) }
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            navRows.forEachIndexed { rowIdx, row ->
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    row.forEach { key -> MacroKeyCell(key, 8 + rowIdx, Modifier.weight(key.weight)) }
                                }
                            }
                        }
                    }

                    // ── 반복 횟수 스테퍼 (row 19): 키 모드 + NORMAL 모드에서만 표시. SWIPE는 연속 추가 롱프레스 팝업으로 대체. ──
                    if ((stepEditMode == MacroStepEditMode.SINGLE_KEY || stepEditMode == MacroStepEditMode.COMBO) && inputMode == InputMode.NORMAL) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(cs.surface).padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("반복", fontSize = 12.sp, color = cs.onSurface.copy(alpha = 0.6f), modifier = Modifier.weight(1f))
                            val stepperBtnMod: @Composable (EdgeEditorElement, Int, () -> Unit) -> Unit = { element, col, onClick ->
                                if (inputMode == InputMode.SWIPE) {
                                    SwipeFocusable(element, scope = EdgeEditorScope.MacroPopup, shape = RoundedCornerShape(4.dp), showBorderHighlight = false, onActivate = onClick, gridRow = 19, gridCol = col, modifier = Modifier.size(28.dp)) {
                                        val focused = LocalSwipeFocused.current
                                        Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(4.dp)).then(if (focused) Modifier.border(1.dp, cs.primary, RoundedCornerShape(4.dp)) else Modifier).background(cs.surfaceVariant), contentAlignment = Alignment.Center) {
                                            Icon(if (col == 0) Icons.Filled.Remove else Icons.Filled.Add, null, Modifier.size(14.dp), tint = cs.onSurface.copy(alpha = 0.7f))
                                        }
                                    }
                                } else {
                                    Box(modifier = Modifier.size(28.dp).clip(RoundedCornerShape(4.dp)).background(cs.surfaceVariant).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
                                        Icon(if (col == 0) Icons.Filled.Remove else Icons.Filled.Add, null, Modifier.size(14.dp), tint = cs.onSurface.copy(alpha = 0.7f))
                                    }
                                }
                            }
                            stepperBtnMod(EdgeEditorElement.MacroStepRepeatMinus, 0) {
                                stepDraftRepeat = (stepDraftRepeat - 1).coerceAtLeast(1)
                            }
                            Text(
                                "×$stepDraftRepeat",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (stepDraftRepeat > 1) cs.primary else cs.onSurface,
                                modifier = Modifier.widthIn(min = 28.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                            stepperBtnMod(EdgeEditorElement.MacroStepRepeatPlus, 1) {
                                stepDraftRepeat = (stepDraftRepeat + 1).coerceAtMost(99)
                            }
                        }
                    }

                    // TEXT + SWIPE 모드: 입력 필드가 카드 최하단 요소가 되도록 divider 생략
                    // (SWIPE는 취소/완료 버튼도 없으므로 divider 이하가 비어 있음)
                    if (!(stepEditMode == MacroStepEditMode.TEXT && inputMode == InputMode.SWIPE)) {
                        HorizontalDivider(color = cs.outline.copy(alpha = 0.2f))
                    }

                    // ── 스텝 버튼 바 (row 20) ──
                    if (stepEditMode == MacroStepEditMode.PICK) {
                        // PICK 모드: 취소(스택 비움) / 확인(스택 일괄 커밋 후 개요로)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Focusable(EdgeEditorElement.MacroPopupCancel, { pickStagedSteps = emptyList(); cancelStepEdit() }, gridRow = 20, gridCol = 0, modifier = Modifier.weight(1f)) { focused ->
                                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(if (focused) cs.error else Color.Transparent).padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                    Text("취소", fontSize = 13.sp, color = if (focused) cs.onError else cs.error)
                                }
                            }
                            Focusable(EdgeEditorElement.MacroStepApply, {
                                onDraftChange(draft.copy(steps = draft.steps + pickStagedSteps.map { it.second }))
                                pickStagedSteps = emptyList()
                                editingStepIndex = null
                                currentPage = MacroEditorPage.STEPS
                                kbTarget = MacroKbTarget.NONE
                            }, gridRow = 20, gridCol = 1, modifier = Modifier.weight(1f)) { focused ->
                                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(if (focused) cs.primary else Color.Transparent).padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        if (pickStagedSteps.isEmpty()) "확인" else "확인 (${pickStagedSteps.size})",
                                        fontSize = 13.sp, color = if (focused) cs.onPrimary else cs.primary
                                    )
                                }
                            }
                        }
                    } else if (stepEditMode == MacroStepEditMode.TEXT) {
                        // TEXT 모드: NORMAL에서만 취소/추가 버튼 표시. SWIPE는 키보드 CANCEL/DONE으로 처리.
                        if (inputMode == InputMode.NORMAL) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Focusable(EdgeEditorElement.MacroPopupCancel, { cancelStepEdit() }, gridRow = 20, gridCol = 0, modifier = Modifier.weight(1f)) { focused ->
                                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(if (focused) cs.error else Color.Transparent).padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                        Text("취소", fontSize = 13.sp, color = if (focused) cs.onError else cs.error)
                                    }
                                }
                                Focusable(EdgeEditorElement.MacroStepApply, { generateFromText(false) }, gridRow = 20, gridCol = 1, modifier = Modifier.weight(1f)) { focused ->
                                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(if (focused) cs.primary else Color.Transparent).padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                        Text("완료", fontSize = 13.sp, color = if (focused) cs.onPrimary else cs.primary)
                                    }
                                }
                            }
                        }
                    } else {
                        // 키 모드: 취소 / 스텝 추가 / 연속 추가
                        // SWIPE 모드: 연속 추가 롱프레스 시 반복 횟수 팝업 표시
                        if (repeatPopupOpen && inputMode == InputMode.SWIPE) {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = cs.surfaceVariant,
                                    tonalElevation = 8.dp,
                                    shadowElevation = 16.dp,
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("×$stepDraftRepeat", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = cs.primary)
                                        Text("좌우·상하로 조절 • 탭으로 추가", fontSize = 11.sp, color = cs.onSurface.copy(alpha = 0.6f))
                                    }
                                }
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Focusable(EdgeEditorElement.MacroPopupCancel, { cancelStepEdit() }, gridRow = 20, gridCol = 0, modifier = Modifier.weight(1f)) { focused ->
                                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(if (focused) cs.error else Color.Transparent).padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                    Text("취소", fontSize = 13.sp, color = if (focused) cs.onError else cs.error)
                                }
                            }
                            Focusable(EdgeEditorElement.MacroStepApply, { applyStep(false) }, gridRow = 20, gridCol = 1, modifier = Modifier.weight(1f)) { focused ->
                                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(if (focused) cs.primary else Color.Transparent).padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                    Text("스텝 추가", fontSize = 13.sp, color = if (focused) cs.onPrimary else cs.primary)
                                }
                            }
                            Focusable(
                                EdgeEditorElement.MacroStepSaveContinue,
                                onActivate = { applyStep(true); repeatPopupOpen = false },
                                gridRow = 20, gridCol = 2,
                                onActivateAlt = {
                                    if (repeatPopupOpen) {
                                        // 개선 4: 팝업이 열린 상태 → 롱프레스로 취소, 열기 전 값 복원
                                        stepDraftRepeat = repeatBackup
                                        repeatPopupOpen = false
                                    } else {
                                        val noKey = if (stepEditMode == MacroStepEditMode.SINGLE_KEY)
                                            stepDraftKeys.isEmpty()
                                        else
                                            stepDraftKeys.isEmpty() && stepDraftMod == 0
                                        if (noKey) {
                                            // 개선 5: 키 미할당 시 토스트, 팝업 열지 않음
                                            ToastController.show("키를 먼저 선택하세요", ToastType.WARNING)
                                        } else {
                                            repeatBackup = stepDraftRepeat
                                            repeatPopupOpen = true
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1.4f)
                            ) { focused ->
                                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(if (focused) cs.secondary else Color.Transparent).padding(horizontal = 6.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
                                    Text("연속 추가", fontSize = 13.sp, color = if (focused) cs.onSecondary else cs.secondary, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (inputMode == InputMode.SWIPE) {
        // 이름/문자열 입력용 SwipeKeyboardOverlay 통합 (ShortcutEditorPopup 패턴)
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
        ) {
            val screenH = maxHeight
            var popupCardHeightDp by remember { mutableStateOf(0.dp) }
            // 타이틀 + 카드 Column 전체 높이 (TEXT 모드 키보드 위치 계산용)
            var popupColumnHeightDp by remember { mutableStateOf(0.dp) }
            val density = LocalDensity.current

            val kbEffectiveH = (if (kbActualHeightDp > 0.dp) kbActualHeightDp
                               else EdgeSwipeConstants.EDGE_ZONE_LABEL_KEYBOARD_VISUAL_HEIGHT_DP.dp) + 16.dp
            val targetOffsetDp = if (popupCardHeightDp > 0.dp && kbActive) {
                val kbTop = screenH - kbEffectiveH
                val popupBottomNoOffset = screenH / 2 + popupCardHeightDp / 2
                (kbTop - popupBottomNoOffset).coerceAtMost(0.dp)
            } else 0.dp
            // TEXT 모드: 카드는 중앙 유지, 키보드를 (타이틀+카드) Column 바로 밑으로 올림
            val textModeKbPadding = if (
                currentPage == MacroEditorPage.STEP_INPUT &&
                stepEditMode == MacroStepEditMode.TEXT &&
                kbActive &&
                popupColumnHeightDp > 0.dp
            ) {
                (screenH / 2 - kbEffectiveH - popupColumnHeightDp / 2).coerceAtLeast(0.dp)
            } else 0.dp
            val macroKbOffsetY by animateDpAsState(
                targetValue = targetOffsetDp,
                animationSpec = tween(220),
                label = "macroKbOffsetY",
            )

            com.bridgeone.app.ui.components.SwipeKeyboardOverlay(
                initialText = when (kbTarget) {
                    MacroKbTarget.NAME -> draftName
                    MacroKbTarget.GROUP_NAME -> draft.groupNames[editingGroupNameId] ?: ""
                    else -> stepTextInput
                },
                maxLength = when (kbTarget) {
                    MacroKbTarget.NAME, MacroKbTarget.GROUP_NAME -> 32
                    else -> 200
                },
                suggestions = emptyList(),
                revertOnCancel = false,
                showScrim = false,
                gestureFullHeight = true,
                showGuide = false,
                showKeyboard = kbActive,
                keyboardBottomPadding = textModeKbPadding,
                onTextChange = { text ->
                    when (kbTarget) {
                        MacroKbTarget.NAME -> draftName = text
                        MacroKbTarget.GROUP_NAME -> tempGroupName = text
                        else -> stepTextInput = text
                    }
                },
                onCancel = {
                    when (kbTarget) {
                        MacroKbTarget.STEP_TEXT -> { kbTarget = MacroKbTarget.NONE; cancelStepEdit() }
                        MacroKbTarget.GROUP_NAME -> { kbTarget = MacroKbTarget.NONE; editingGroupNameId = null; tempGroupName = "" }
                        else -> kbTarget = MacroKbTarget.NONE
                    }
                },
                onDone = { result ->
                    when (kbTarget) {
                        MacroKbTarget.NAME -> { draftName = result; kbTarget = MacroKbTarget.NONE }
                        MacroKbTarget.GROUP_NAME -> {
                            editingGroupNameId?.let { id -> onDraftChange(draft.copy(groupNames = draft.groupNames + (id to result))) }
                            kbTarget = MacroKbTarget.NONE
                            editingGroupNameId = null
                            tempGroupName = ""
                        }
                        MacroKbTarget.STEP_TEXT -> {
                            stepTextInput = result
                            kbTarget = MacroKbTarget.NONE
                            generateFromText(false)
                        }
                        else -> kbTarget = MacroKbTarget.NONE
                    }
                },
                onContentHeightMeasured = { px -> with(density) { kbActualHeightDp = px.toDp() } },
                overlay = {
                    Box(
                        modifier = Modifier.fillMaxSize().offset(y = macroKbOffsetY),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.Start,
                            modifier = Modifier.onSizeChanged { size ->
                                with(density) { popupColumnHeightDp = size.height.toDp() }
                            },
                        ) {
                            Text(
                                when (currentPage) {
                                MacroEditorPage.STEP_INPUT -> "스텝 추가"
                                MacroEditorPage.FINALIZE -> "매크로 편집 · 저장"
                                else -> "매크로 편집 · 스텝 구성"
                            },
                                fontSize = 14.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 6.dp).offset(x = 2.dp, y = 4.dp),
                            )
                            Box(modifier = Modifier
                                .heightIn(max = dialogCardMaxHeight)
                                .onGloballyPositioned { coords ->
                                    with(density) { popupCardHeightDp = coords.size.height.toDp() }
                                }) {
                                PopupContent()
                                if (kbActive) {
                                    Box(modifier = Modifier.matchParentSize().pointerInput(Unit) {
                                        awaitEachGesture {
                                            awaitFirstDown(requireUnconsumed = false).consume()
                                            while (true) {
                                                val e = awaitPointerEvent()
                                                e.changes.forEach { it.consume() }
                                                if (e.type == androidx.compose.ui.input.pointer.PointerEventType.Release) break
                                            }
                                        }
                                    })
                                }
                            }
                            if (currentPage == MacroEditorPage.STEP_INPUT) {
                                Text(
                                    "'연속 추가'를 꾹 누르면 여러 번 반복 추가",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.45f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                                )
                            }
                        }
                    }
                },
            )
        }
    } else {
        Popup(
            alignment = Alignment.Center,
            properties = PopupProperties(focusable = true, dismissOnClickOutside = true),
            onDismissRequest = onCancel,
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f))
                    .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) { onCancel() },
                // 개선 1 NORMAL: TEXT 입력 시 카드를 하단 기준으로 정렬 → 시스템 IME가 버튼 바로 위에 위치
                contentAlignment = if (currentPage == MacroEditorPage.STEP_INPUT && stepEditMode == MacroStepEditMode.TEXT)
                    Alignment.BottomCenter else Alignment.Center
            ) {
                Box(modifier = Modifier.clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) {}) {
                    Column(
                        horizontalAlignment = Alignment.Start,
                        // imePadding: Popup 내 IME 인셋 처리 (immersive 환경에서 best-effort)
                        modifier = Modifier.imePadding(),
                    ) {
                        Text(
                            when (currentPage) {
                                MacroEditorPage.STEP_INPUT -> "스텝 추가"
                                MacroEditorPage.FINALIZE -> "매크로 편집 · 저장"
                                else -> "매크로 편집 · 스텝 구성"
                            },
                            fontSize = 14.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 6.dp).offset(x = 2.dp, y = 4.dp),
                        )
                        PopupContent()
                    }
                }
            }
        }
        // NORMAL 모드 아이콘 선택 바텀시트
        if (normalIconSheetVisible) {
            NormalCategoryIconSheet(
                selectedIconKey = draftIconKey,
                sheetState = normalIconSheetState,
                onPick = { key -> draftIconKey = key; normalIconSheetVisible = false },
                onDismiss = { normalIconSheetVisible = false },
            )
        }
    }
}

// Phase 4.7.5-A: ratioPresetsFor → EdgeZoneActionResolver.kt로 이관

/**
 * SWIPE 모드 전용 Undo 히스토리 드롭다운.
 * 항목은 [EdgeEditorScope.UndoMenu] scope의 SwipeFocusable로 등록되어 위아래 스와이프로 탐색 가능.
 * 메인 Box 내부 인라인 오버레이로 렌더링해야 SwipeGestureLayer가 터치를 수신할 수 있음.
 * 항목이 많으면 스크롤되며 포커스된 항목이 뷰포트 안으로 자동 스크롤됨.
 */
@Composable
private fun UndoHistorySwipePopup(
    undoStack: List<EdgeZoneConfig>,
    workConfig: EdgeZoneConfig,
    onApply: (config: EdgeZoneConfig, stackIdx: Int) -> Unit,
) {
    val swipeController = LocalSwipeFocusController.current
    val scrollState = rememberScrollState()
    val itemHeights = remember { mutableStateMapOf<Int, Int>() }
    var scrollableHeight by remember { mutableIntStateOf(0) }

    // 포커스된 항목이 뷰포트 밖이면 자동 스크롤
    LaunchedEffect(swipeController?.currentFocus) {
        val focus = swipeController?.currentFocus as? EdgeEditorElement.UndoHistoryItem ?: return@LaunchedEffect
        val itemTop = (0 until focus.index).sumOf { itemHeights[it] ?: 0 }
        val itemBottom = itemTop + (itemHeights[focus.index] ?: 0)
        val viewportTop = scrollState.value
        val viewportBottom = viewportTop + scrollableHeight
        when {
            itemTop < viewportTop -> scrollState.animateScrollTo(itemTop)
            itemBottom > viewportBottom && scrollableHeight > 0 ->
                scrollState.animateScrollTo(itemBottom - scrollableHeight)
        }
    }

    val cs = MaterialTheme.colorScheme
    androidx.compose.material3.Surface(
        modifier = Modifier.widthIn(min = 200.dp, max = 280.dp),
        shape = RoundedCornerShape(12.dp),
        color = cs.surfaceContainerHigh,
        shadowElevation = 12.dp,
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            // 스크롤 가능한 undo 항목 영역
            Box(
                modifier = Modifier
                    .heightIn(max = 400.dp)
                    .onSizeChanged { scrollableHeight = it.height }
            ) {
                Column(modifier = Modifier.verticalScroll(scrollState, enabled = false)) {
                    undoStack.forEachIndexed { idx, config ->
                        val newerConfig = if (idx == 0) workConfig else undoStack[idx - 1]
                        val desc = EdgeZoneActionResolver.describeUndoStep(from = config, to = newerConfig)
                        SwipeFocusable(
                            element = EdgeEditorElement.UndoHistoryItem(idx),
                            scope = EdgeEditorScope.UndoMenu,
                            shape = RoundedCornerShape(8.dp),
                            showBorderHighlight = true,
                            onActivate = { onApply(config, idx) },
                            gridRow = idx,
                            modifier = Modifier.onSizeChanged { size -> itemHeights[idx] = size.height },
                        ) {
                            Text(
                                text = desc,
                                fontSize = 14.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * SWIPE 모드 전용 비율 프리셋 드롭다운 팝업.
 * [PopupProperties.focusable] = false로 터치가 팝업을 통과하여 SwipeGestureLayer에 전달됨.
 * 항목은 [EdgeEditorScope.RatioPresetMenu] scope의 SwipeFocusable로 등록됨.
 */
@Composable
private fun RatioPresetSwipePopup(
    presets: List<Pair<String, List<Float>>>,
    onSelect: (List<Float>) -> Unit,
) {
    Popup(
        alignment = Alignment.BottomStart,
        properties = PopupProperties(focusable = false),
    ) {
        androidx.compose.material3.Surface(
            shape = RoundedCornerShape(4.dp),
            tonalElevation = 2.dp,
            shadowElevation = 8.dp,
        ) {
            Column {
                presets.forEachIndexed { presetIdx, (label, ratios) ->
                    SwipeFocusable(
                        element = EdgeEditorElement.RatioPresetItem(label),
                        scope = EdgeEditorScope.RatioPresetMenu,
                        shape = RoundedCornerShape(4.dp),
                        showBorderHighlight = true,
                        onActivate = { onSelect(ratios) },
                        gridRow = presetIdx,
                    ) {
                        Row(
                            modifier = Modifier
                                .widthIn(min = 112.dp)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MiniRatioBar(ratios = ratios, modifier = Modifier.width(40.dp).height(10.dp))
                            Text(label, fontSize = 13.sp)
                        }
                    }
                }
            }
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

// Phase 4.7.5-A: actionEquals + describeUndoStep → EdgeZoneActionResolver.kt로 이관

// ============================================================
// 로테이션(액션 순환) 존 편집기
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RotationEditor(
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
