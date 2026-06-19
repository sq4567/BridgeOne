package com.bridgeone.app.ui.components.touchpad

import android.annotation.SuppressLint
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.OpenWith
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.bridgeone.app.ui.common.EdgeZonePresetsRepository
import com.bridgeone.app.ui.common.stripActions
import com.bridgeone.app.ui.common.InputMode
import com.bridgeone.app.ui.common.MODE_PRESETS
import com.bridgeone.app.ui.common.ToastController
import com.bridgeone.app.ui.common.ToastType
import com.bridgeone.app.ui.common.customPresetIconOrNull
import com.bridgeone.app.ui.common.loadInputMode
import com.bridgeone.app.ui.common.loadSwipeWrapEdge
import com.bridgeone.app.ui.common.swipe.LocalSwipeFocusController
import com.bridgeone.app.ui.common.swipe.LocalSwipeFocused
import com.bridgeone.app.ui.common.swipe.SwipeFocusController
import com.bridgeone.app.ui.common.swipe.SwipeFocusable
import com.bridgeone.app.ui.common.swipe.SwipeGestureLayer
import com.bridgeone.app.ui.common.swipe.SwipeMode
import com.bridgeone.app.ui.common.swipe.rememberSwipeFocusController
import com.bridgeone.app.ui.common.ColorCodec


import com.bridgeone.app.ui.components.SwipeKeyboardOverlay
import com.bridgeone.app.ui.components.DEFAULT_SHORTCUTS
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.roundToInt

/** 커스텀 배율 슬라이더 트랙 높이 (dp). 기본값: 28f */
internal const val CUSTOM_SLIDER_TRACK_HEIGHT_DP = 28f
/** 커스텀 배율 슬라이더 손잡이 세로선 너비 (dp). 기본값: 3f */
internal const val CUSTOM_SLIDER_LINE_WIDTH_DP = 3f

// ── 영역 비율 인라인 액션 팝업 상태 ──
private sealed class ZoneActionPopup {
    object None : ZoneActionPopup()
    data class Initial(val zone: EdgeZone, val anchor: Float) : ZoneActionPopup()
    data class MergeSelecting(val zone: EdgeZone) : ZoneActionPopup()
    data class SplitChoosing(val zone: EdgeZone, val anchor: Float) : ZoneActionPopup()
    data class DeleteConfirming(val zone: EdgeZone, val anchor: Float) : ZoneActionPopup()
}

/** 커스텀 프리셋 수정/삭제 메뉴 대상 (다이나믹스, 단축키, 또는 매크로) */
internal sealed class CustomPresetTarget {
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
internal fun ActionDomainPicker(
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

