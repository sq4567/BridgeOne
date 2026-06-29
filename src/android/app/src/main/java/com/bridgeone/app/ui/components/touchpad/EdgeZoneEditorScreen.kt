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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Save
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.first
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bridgeone.app.ui.common.CustomPointerDynamicsPreset
import com.bridgeone.app.ui.common.CustomPresetsRepository
import com.bridgeone.app.ui.common.DYNAMICS_PRESETS
import com.bridgeone.app.ui.common.EdgeSwipeConstants
import com.bridgeone.app.ui.common.EdgeZonePresetsRepository
import com.bridgeone.app.ui.common.stripActions
import com.bridgeone.app.ui.common.InputMode
import com.bridgeone.app.ui.common.LocalInputMode
import com.bridgeone.app.ui.common.ToastController
import com.bridgeone.app.ui.common.ToastType
import com.bridgeone.app.ui.common.loadInputMode
import com.bridgeone.app.ui.common.loadSwipeWrapEdge
import com.bridgeone.app.ui.common.loadZoneMoveMethod
import com.bridgeone.app.ui.common.ZoneMoveMethod
import com.bridgeone.app.ui.pages.standard.loadCornerBlockedRatio
import com.bridgeone.app.ui.pages.standard.saveCornerBlockedRatio
import com.bridgeone.app.ui.common.CustomTrackSlider
import kotlin.math.roundToInt
import com.bridgeone.app.ui.common.swipe.LocalSwipeFocusController
import com.bridgeone.app.ui.common.swipe.LocalSwipeFocused
import com.bridgeone.app.ui.common.swipe.LocalSwipeFlashAlpha
import com.bridgeone.app.ui.common.swipe.SwipeFocusable
import com.bridgeone.app.ui.common.swipe.SwipeGestureLayer
import com.bridgeone.app.ui.common.swipe.SwipeMode
import com.bridgeone.app.ui.common.swipe.rememberSwipeFocusController


import com.bridgeone.app.ui.components.SwipeKeyboardOverlay
import androidx.compose.ui.draw.alpha

// ── 영역 비율 인라인 액션 팝업 상태 ──
internal sealed class ZoneActionPopup {
    object None : ZoneActionPopup()
    data class Initial(val zone: EdgeZone, val anchor: Float) : ZoneActionPopup()
    data class MergeSelecting(val zone: EdgeZone, val anchor: Float, val selectedTargets: Set<Float> = emptySet()) : ZoneActionPopup()
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
    // 존 이동 방식. SWIPE 레이어에서는 항상 TAP으로 강제(설정 UI 미노출).
    val effectiveMoveMethod = remember(inputMode) {
        if (inputMode == InputMode.SWIPE) ZoneMoveMethod.TAP else loadZoneMoveMethod(context)
    }
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
    // 코너 버튼 차단 영역 크기(전역 설정). 슬라이더는 로컬 상태만 갱신해 미리보기에 즉시 반영하고,
    // 실제 영속화는 '저장' 시점에만 수행 → initial 대비 변경은 hasChanges에 반영돼 미저장 이탈 시 다이얼로그가 뜬다.
    val initialCornerBlockedRatio = remember { loadCornerBlockedRatio(context) }
    var cornerBlockedRatio by remember { mutableStateOf(initialCornerBlockedRatio) }
    // Phase 4.7.5-D: 오버레이/팝업 UI 상태 홀더. 기존 지역 변수명은 MutableState 위임으로 유지.
    val overlayUi = remember { EdgeZoneOverlayUiState() }
    var savedRotationTrigger by remember { mutableStateOf<EdgeZoneTrigger.Rotation?>(null) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showIconSheet by overlayUi.showIconSheetState
    var showColorPicker by overlayUi.showColorPickerState
    // 커스텀 단축키 팝업 — SWIPE 모드에서는 Popup 대신 Box 인라인 오버레이로 렌더링
    var swipeShortcutVisible by overlayUi.swipeShortcutVisibleState
    var swipeShortcutDraft by overlayUi.swipeShortcutDraftState
    var swipeShortcutOnConfirm by overlayUi.swipeShortcutOnConfirmState
    var swipeShortcutOnAddAsCandidate by overlayUi.swipeShortcutOnAddAsCandidateState
    // 커스텀 매크로 편집기 — SWIPE 모드에서는 Box 인라인 오버레이로 렌더링 (NORMAL은 Popup)
    var macroEditorVisible by overlayUi.macroEditorVisibleState
    var macroEditorDraft by overlayUi.macroEditorDraftState
    var macroEditorOnConfirm by overlayUi.macroEditorOnConfirmState
    var macroEditorOnAddAsPreset by overlayUi.macroEditorOnAddAsPresetState
    var macroEditorInitialIconKey by overlayUi.macroEditorInitialIconKeyState
    var macroEditorInitialName by overlayUi.macroEditorInitialNameState
    // SWIPE 모드 매크로 이름/문자열 입력 키보드 활성 여부 (제스처 양보용). 기본값: false
    var macroNameKbActive by overlayUi.macroNameKbActiveState
    // NORMAL 모드 매크로 편집기 미니버튼 롱프레스 툴팁 상태 (호이스팅). 기본값: ""
    var normalMiniTooltipText by overlayUi.normalMiniTooltipTextState
    var normalMiniTooltipAnchor by overlayUi.normalMiniTooltipAnchorState
    var colorPickerStage by overlayUi.colorPickerStageState
    var showLabelKeyboard by overlayUi.showLabelKeyboardState
    var candidateLabelKeyboard by overlayUi.candidateLabelKeyboardState
    var candidateLabelCurrent by remember { mutableStateOf("") }
    var showPresetPopup by overlayUi.showPresetPopupState
    var presetPopupStage by overlayUi.presetPopupStageState
    // SWIPE 모드 프리셋 이름 입력 키보드 상태. null이면 비활성. 기본값: null
    var presetNameKeyboard by overlayUi.presetNameKeyboardState
    var presetNameSeed by overlayUi.presetNameSeedState
    // SWIPE 모드 단축키 액션명 입력 키보드 활성 여부. 팝업 내부가 관리하고 본체는 scope 전환·제스처 양보에만 사용. 기본값: false
    var shortcutNameKbActive by overlayUi.shortcutNameKbActiveState
    var showUndoMenu by overlayUi.showUndoMenuState
    // Undo 버튼 하단 y 좌표 (px, in window). 드롭다운 위치 계산용. 기본값: 0
    var undoMenuAnchorBottom by overlayUi.undoMenuAnchorBottomState
    // 커스텀 다이나믹스 프리셋 편집기 상태
    var localCustomPresets by remember(customPresets) { mutableStateOf(customPresets) }
    // 커스텀 단축키 프리셋 상태
    var localCustomShortcutPresets by overlayUi.localCustomShortcutPresetsState
    LaunchedEffect(Unit) { customShortcutPresetsRepo?.let { localCustomShortcutPresets = it.loadAll() } }
    // 커스텀 매크로 프리셋 상태
    var localCustomMacroPresets by overlayUi.localCustomMacroPresetsState
    LaunchedEffect(Unit) { customMacroPresetsRepo?.let { localCustomMacroPresets = it.loadAll() } }
    var dynamicsEditorVisible by remember { mutableStateOf(false) }
    // 편집 대상 다이나믹스 프리셋. null이면 신규 생성, non-null이면 해당 프리셋 편집. 기본값: null
    var dynamicsEditorInitial by remember { mutableStateOf<CustomPointerDynamicsPreset?>(null) }
    // SWIPE 모드 커스텀 프리셋 수정/삭제 메뉴 대상. null이면 닫힘. 기본값: null
    var swipeCustomMenuTarget by overlayUi.swipeCustomMenuTargetState
    val zonePopupState = remember { mutableStateOf<ZoneActionPopup>(ZoneActionPopup.None) }
    var zonePopup by zonePopupState
    // 캔버스 구조 변경 모드 (병합/분할/이동/삭제/비율) — 캔버스 씬 전용 UI 상태 (Phase 4.7.x)
    // MutableState로 노출(zonePopupState 패턴) — EdgeZoneOverlayLayer의 롱프레스 핸들러가 이동 취소에 사용.
    val canvasModeState = remember { mutableStateOf<CanvasEditMode>(CanvasEditMode.None) }
    var canvasMode by canvasModeState
    // SWIPE: 모드 퇴장(→None) 시 직전 모드의 진입 버튼으로 포커스. (모드 진입 시 초기 포커스는 isCanvasModeActive LaunchedEffect가 담당)
    var prevCanvasModeKind by remember { mutableStateOf<CanvasModeKind?>(null) }
    LaunchedEffect(canvasMode, inputMode) {
        if (inputMode == InputMode.SWIPE && canvasMode is CanvasEditMode.None) {
            prevCanvasModeKind?.let { swipeController.setFocus(EdgeEditorElement.CanvasModeButton(it)) }
        }
        prevCanvasModeKind = canvasMode.kind
    }
    var selectedEdge by remember { mutableStateOf<EntryEdge?>(null) }
    var nextFocusOnZoneChange by overlayUi.nextFocusOnZoneChangeState
    val iconSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var iconBoxCenterInWindow by overlayUi.iconBoxCenterInWindowState
    var colorBoxCenterInWindow by overlayUi.colorBoxCenterInWindowState
    var ratioBtnBoundsInWindow by overlayUi.ratioBtnBoundsInWindowState
    var actionTypeBtnBoundsInWindow by overlayUi.actionTypeBtnBoundsInWindowState
    var revertBtnBoundsInWindow by overlayUi.revertBtnBoundsInWindowState
    // 롱프레스 색 확정 후보 hex. null이면 확정 대상 없음(ExpandToggle 등). 기본값: null
    var colorCommitCandidate by overlayUi.colorCommitCandidateState
    // 아이콘 서랍 단계 (카테고리 ↔ 아이콘). BackHandler 분기를 위해 상위가 소유.
    var iconDrawerStage by overlayUi.iconDrawerStageState
    // 단축키 팝업 아이콘 선택 서랍 (SWIPE 전용). 기본값: false
    var shortcutIconSheetVisible by overlayUi.shortcutIconSheetVisibleState
    var shortcutIconSeed by overlayUi.shortcutIconSeedState
    var shortcutIconOnPick by overlayUi.shortcutIconOnPickState
    var shortcutIconAnchor by overlayUi.shortcutIconAnchorState
    // 서랍을 연 진입점 — 닫을 때 이 요소로 포커스 복원. 기본값: ShortcutIconButton
    var iconPickerReturnElement by overlayUi.iconPickerReturnElementState

    // ── 액션 순환 후보 편집 상태 (RotationEditor에서 hoist) ──
    // 라벨 키보드 오버레이가 RotationEditor를 컴포지션에서 제거해도 편집 상태가 소멸하지 않도록 상위가 소유.
    var rotationEditingEntry by remember { mutableStateOf<Pair<Int?, RotationCandidate>?>(null) }
    var rotationDraft by overlayUi.rotationDraftState
    var showCandidateIconSheet by overlayUi.showCandidateIconSheetState
    var showCandidateColorPicker by overlayUi.showCandidateColorPickerState
    var candidateIconBoxCenterInWindow by overlayUi.candidateIconBoxCenterInWindowState
    var candidateColorBoxCenterInWindow by overlayUi.candidateColorBoxCenterInWindowState

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
                // 캔버스 씬 진입 시 모드 진입 버튼(병합)에 포커스를 둔다
                swipeController.setFocus(EdgeEditorElement.CanvasModeButton(CanvasModeKind.MERGE))
            }
        }
        LaunchedEffect(selectedZone) {
            val zone = selectedZone
            if (zone != null) {
                val override = nextFocusOnZoneChange
                if (override != null) {
                    nextFocusOnZoneChange = null
                    swipeController.setFocus(override)
                } else if (swipeController.currentFocus != EdgeEditorElement.CustomMultiplierSlider
                    && swipeController.currentFocus !is EdgeEditorElement.StripBoundary
                    && swipeController.mode != SwipeMode.MANIPULATION) {
                    // 슬라이더 조작 중, 또는 구분선 비율 조정 중 포커스를 빼앗지 않음
                    val zoneList = workConfig.zonesFor(zone.edge)
                    val idx = zoneList.indexOfFirst {
                        it.startRatio == zone.startRatio && it.edge == zone.edge
                    }.coerceAtLeast(0)
                    swipeController.setFocus(EdgeEditorElement.StripZone(idx))
                }
            } else if (canvasMode is CanvasEditMode.None && swipeController.currentFocus !is EdgeEditorElement.CanvasZone) {
                // 편집 → 캔버스 복귀 시 모드 진입 버튼으로 포커스
                // (병합 직후처럼 이미 존에 포커스를 둔 경우는 유지)
                swipeController.setFocus(EdgeEditorElement.CanvasModeButton(CanvasModeKind.MERGE))
            }
        }

        // 캔버스 씬에서는 방향 우선 공간 네비게이션을 설치 (cone traversal의 대각선 누락 보정).
        // 이동 모드는 픽/드롭 단계 모두 캔버스 내부 요소(존/드롭 슬롯)에만 포커스를 한정해 캔버스 밖(TopAppBar 등)으로 새지 않게 한다.
        // 편집 씬 진입 시 해제하여 기존 폼 traversal 복원.
        val isCanvasScene = selectedZone == null && selectedEdge == null
        val movingForNav = canvasMode as? CanvasEditMode.Moving
        // 0=편집 씬, 1=일반 캔버스(전체 spatial), 2=이동 픽(존 전용), 3=이동 드롭(슬롯 전용)
        val navMode = when {
            !isCanvasScene -> 0
            movingForNav == null -> 1
            movingForNav.picked == null -> 2
            else -> 3
        }
        DisposableEffect(navMode) {
            swipeController.moveInterceptor = when (navMode) {
                1 -> { dir -> canvasSpatialNav(swipeController, dir) }
                2 -> { dir -> movingPickNav(swipeController, dir) }
                3 -> { dir -> movingDropNav(swipeController, dir) }
                else -> null  // 편집 씬: 기본 폼 traversal
            }
            onDispose { swipeController.moveInterceptor = null }
        }

    }

    // Undo 후 포커스 결정: 스택 남으면 Undo 유지, 비면 복원된 존(없으면 Back)
    // SWIPE 모드에서만 호출됨. swipeController는 항상 non-null.
    fun focusAfterUndo(remaining: List<EdgeZoneConfig>, restored: EdgeZone?, config: EdgeZoneConfig) {
        when {
            remaining.isNotEmpty() -> swipeController.setFocus(EdgeEditorElement.Undo)
            restored != null -> {
                val idx = config.zonesFor(restored.edge)
                    .indexOfFirst { it.startRatio == restored.startRatio }.coerceAtLeast(0)
                swipeController.setFocus(EdgeEditorElement.StripZone(idx))
            }
            else -> swipeController.setFocus(EdgeEditorElement.Back)
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
        // rememberUpdatedState: onDispose 시점의 최신 값을 읽기 위해 DisposableEffect보다 앞에 선언
        val latestSelectedZone by rememberUpdatedState(selectedZone)
        val latestWorkConfig by rememberUpdatedState(workConfig)
        val latestUndoStack by rememberUpdatedState(undoStack)
        DisposableEffect(showUndoMenu) {
            val active = showUndoMenu
            if (active) {
                swipeController.pushScope(EdgeEditorScope.UndoMenu)
                swipeController.setFocus(EdgeEditorElement.UndoHistoryItem(0))
            }
            onDispose {
                if (active) {
                    swipeController.popScope()
                    focusAfterUndo(latestUndoStack, latestSelectedZone, latestWorkConfig)
                }
            }
        }
        val isZonePopupOpen = zonePopup !is ZoneActionPopup.None
        DisposableEffect(isZonePopupOpen) {
            val active = isZonePopupOpen
            if (active) swipeController.pushScope(EdgeEditorScope.ZoneActionPopup)
            onDispose {
                if (active) {
                    swipeController.popScope()
                    val zone = latestSelectedZone
                    if (zone != null) {
                        val zones = latestWorkConfig.zonesFor(zone.edge)
                        val idx = zones.indexOfFirst { it.startRatio == zone.startRatio }.coerceAtLeast(0)
                        swipeController.setFocus(EdgeEditorElement.StripZone(idx))
                    }
                }
            }
        }
        // 캔버스 구조 변경/비율 조정 모드 진입 시 '취소' 버튼으로 초기 포커스 (SWIPE)
        // 병합/분할/삭제/비율은 모드 바에 CanvasModeCancel을 렌더하므로 진입 즉시 취소로 포커스.
        // 이동(Moving)은 취소 버튼이 없고 자체 포커스 시드(아래)를 쓰므로 제외.
        val isCanvasModeActive = canvasMode !is CanvasEditMode.None
        LaunchedEffect(isCanvasModeActive) {
            if (isCanvasModeActive && canvasMode !is CanvasEditMode.Moving) {
                swipeController.setFocus(EdgeEditorElement.CanvasModeCancel)
            }
        }
        // 비율 조정 manipulation(MANIPULATION 진입) 시 Undo 스택에 1회만 적립
        LaunchedEffect(swipeController.mode) {
            if (canvasMode is CanvasEditMode.Resizing && swipeController.mode == SwipeMode.MANIPULATION) {
                state.pushUndo()
            }
        }
        // 이동 모드 포커스 시드:
        //  - 픽 단계(picked==null): 취소로 복귀했으면 직전에 들어올렸던 존, 최초 진입이면 첫 존(CanvasZone)
        //  - 드롭 단계(picked!=null): 선택 존의 "원래 위치" 슬롯으로 → 고스트가 제자리, 아래 스와이프 시 인접 슬롯
        val movingMode = canvasMode as? CanvasEditMode.Moving
        val movingPickedKey = movingMode?.picked
        // 직전 picked 추적 — 취소(롱프레스) 후 원래 존으로 포커스 복원용. 모드 종료 시 리셋.
        var lastMovingPicked by remember { mutableStateOf<ZoneKey?>(null) }
        LaunchedEffect(movingPickedKey, movingMode != null) {
            if (movingMode == null) {
                lastMovingPicked = null
                return@LaunchedEffect
            }
            val picked = movingPickedKey
            if (picked == null) {
                // 취소 복귀: 직전 들어올렸던 존이 그대로 있으면 그 존으로 복원
                val restore = lastMovingPicked
                if (restore != null) {
                    val idx = workConfig.zonesFor(restore.edge).indexOfFirst { it.startRatio == restore.startRatio }
                    if (idx >= 0) {
                        swipeController.setFocus(EdgeEditorElement.CanvasZone(restore.edge, idx))
                        return@LaunchedEffect
                    }
                }
                // 최초 진입(또는 드롭 후 원위치 소멸): 첫 포커스 가능한 존
                val firstZoneEdge = EntryEdge.entries.firstOrNull {
                    it !in disabledEdges.keys && workConfig.zonesFor(it).isNotEmpty()
                }
                if (firstZoneEdge != null) {
                    swipeController.setFocus(EdgeEditorElement.CanvasZone(firstZoneEdge, 0))
                }
            } else {
                lastMovingPicked = picked
                val pi = workConfig.zonesFor(picked.edge)
                    .indexOfFirst { it.startRatio == picked.startRatio }.coerceAtLeast(0)
                swipeController.setFocus(EdgeEditorElement.CanvasDropSlot(picked.edge, pi))
            }
        }

        // ZoneActionPopup.Initial 진입 시 가운데 "분할" 버튼으로 초기 포커스
        val isInitialPopup = zonePopup is ZoneActionPopup.Initial
        LaunchedEffect(isInitialPopup) {
            if (isInitialPopup) swipeController.setFocus(EdgeEditorElement.ZoneActionSplit)
        }
        // 캔버스 분할 모드: 대상 존 선택 시 선택 가능한 첫 분할 갯수 버튼으로 포커스 (SplitModeBar의 valid 판정과 동일)
        val canvasSplitTarget = (canvasMode as? CanvasEditMode.Splitting)?.target
        LaunchedEffect(canvasSplitTarget) {
            if (canvasSplitTarget != null) {
                workConfig.zonesFor(canvasSplitTarget.edge)
                    .firstOrNull { it.startRatio == canvasSplitTarget.startRatio }
                    ?.let { zone ->
                        val edgeZoneCount = workConfig.zonesFor(canvasSplitTarget.edge).size
                        val width = zone.endRatio - zone.startRatio
                        val firstValidN = (2..5).firstOrNull { n ->
                            edgeZoneCount + n - 1 <= EdgeSwipeConstants.MAX_ZONES_PER_EDGE.toInt() &&
                                width / n >= EdgeSwipeConstants.MIN_ZONE_RATIO
                        }
                        if (firstValidN != null) {
                            swipeController.setFocus(EdgeEditorElement.CanvasSplitChoice(firstValidN))
                        }
                    }
            }
        }
        // Initial → SplitChoosing 전환 시 첫 분할 버튼(2분할)으로 초기 포커스 → 진입 즉시 미리보기 표시
        val isSplitChoosing = zonePopup is ZoneActionPopup.SplitChoosing
        LaunchedEffect(isSplitChoosing) {
            if (isSplitChoosing) swipeController.setFocus(EdgeEditorElement.ZoneActionSplitN(2))
        }
        // Initial → MergeSelecting 전환 시 왼쪽 인접 존 버튼으로 초기 포커스
        val isMergeSelecting = zonePopup is ZoneActionPopup.MergeSelecting
        LaunchedEffect(isMergeSelecting) {
            if (isMergeSelecting) {
                val ms = zonePopup as ZoneActionPopup.MergeSelecting
                val zones = workConfig.zonesFor(ms.zone.edge)
                val bi = zones.indexOfFirst { it.startRatio == ms.zone.startRatio }
                val initFocus = when {
                    bi > 0 -> EdgeEditorElement.ZoneActionMergeLeft
                    bi < zones.size - 1 -> EdgeEditorElement.ZoneActionMergeRight
                    else -> EdgeEditorElement.ZoneActionMergeConfirm
                }
                swipeController.setFocus(initFocus)
            }
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

    BackHandler(enabled = candidateLabelKeyboard != null || canvasMode !is CanvasEditMode.None || zonePopup !is ZoneActionPopup.None || selectedZone != null || selectedEdge != null) {
        val cm = canvasMode
        if (candidateLabelKeyboard != null) candidateLabelKeyboard = null
        // 이동 모드에서 존을 들어올린 상태면 먼저 picked만 해제(모드 유지)
        else if (cm is CanvasEditMode.Moving && cm.picked != null) canvasMode = CanvasEditMode.Moving()
        else if (canvasMode !is CanvasEditMode.None) canvasMode = CanvasEditMode.None
        else if (zonePopup !is ZoneActionPopup.None) zonePopup = ZoneActionPopup.None
        else { selectedZone = null; selectedEdge = null }
    }

    val hasChanges = workConfig != initialConfig || cornerBlockedRatio != initialCornerBlockedRatio
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
        LocalInputMode provides inputMode,
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
                    if (selectedZone != null || selectedEdge != null) { selectedZone = null; selectedEdge = null }
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
                            val remaining = undoStack.drop(1)
                            workConfig = prev
                            undoStack = remaining
                            currentPresetId = null
                            val sel = selectedZone
                            val newSel = if (sel != null) {
                                prev.zonesFor(sel.edge).firstOrNull { it.startRatio == sel.startRatio }
                                    ?: prev.zonesFor(sel.edge).firstOrNull()
                            } else null
                            selectedZone = newSel
                            if (inputMode == InputMode.SWIPE) {
                                // 스택 남으면 Undo override, 비면 LaunchedEffect 기본 StripZone 경로
                                nextFocusOnZoneChange = if (remaining.isNotEmpty()) EdgeEditorElement.Undo else null
                                // selectedZone 미변경(data class 동등) 대비 직접 setFocus
                                focusAfterUndo(remaining, newSel, prev)
                            }
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
                    // undoEnabled와 무관하게 항상 SwipeFocusable로 감쌈.
                    // 조건부 렌더 시 스택 소진으로 undoEnabled=false가 되면 Undo 요소가 dispose되어
                    // unregister→currentFocus 초기화로 포커스가 상실되는 버그를 방지.
                    SwipeFocusable(
                        element = EdgeEditorElement.Undo,
                        shape = RoundedCornerShape(24.dp),
                        onActivate = if (undoEnabled) undoSingleAction else fun() {},
                        onActivateAlt = if (undoEnabled) undoHistoryAction else fun() {},
                        gridRow = 0,
                    ) { undoIconBox() }
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
                        saveCornerBlockedRatio(context, cornerBlockedRatio)
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
            val touchpadAspectRatio = LocalConfiguration.current.let {
                it.screenWidthDp.toFloat() / it.screenHeightDp.toFloat()
            }
            val isEditing = selectedZone != null || selectedEdge != null

            AnimatedContent(
                targetState = isEditing,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    val dur = EdgeSwipeConstants.EDGE_ZONE_SCENE_TRANSITION_MS
                    val delay = EdgeSwipeConstants.EDGE_ZONE_EDIT_ENTER_DELAY_MS
                    val canvasScale = EdgeSwipeConstants.EDGE_ZONE_CANVAS_SCALE_MIN
                    val editScale = EdgeSwipeConstants.EDGE_ZONE_EDIT_ENTER_SCALE
                    if (targetState) {
                        // 캔버스 → 편집: 캔버스 축소·페이드아웃, 편집 진입(지연)
                        (scaleIn(
                            animationSpec = tween(dur, delayMillis = delay, easing = FastOutSlowInEasing),
                            initialScale = editScale,
                        ) + fadeIn(tween(dur, delayMillis = delay, easing = FastOutSlowInEasing))
                        ) togetherWith (
                            scaleOut(tween(dur, easing = FastOutSlowInEasing), targetScale = canvasScale)
                                + fadeOut(tween(dur, easing = FastOutSlowInEasing))
                        )
                    } else {
                        // 편집 → 캔버스: 편집 축소·페이드아웃, 캔버스 복귀(지연)
                        (scaleIn(
                            animationSpec = tween(dur, delayMillis = delay, easing = FastOutSlowInEasing),
                            initialScale = canvasScale,
                        ) + fadeIn(tween(dur, delayMillis = delay, easing = FastOutSlowInEasing))
                        ) togetherWith (
                            scaleOut(tween(dur, easing = FastOutSlowInEasing), targetScale = editScale)
                                + fadeOut(tween(dur, easing = FastOutSlowInEasing))
                        )
                    }
                },
                label = "edgeZoneScene",
            ) { editing ->
                if (!editing) {
                    // ── 캔버스 씬 ──
                    // 병합/분할/이동 stretch·shrink 보간 상태 (Phase 4.7.x).
                    // 이동(cross-edge)은 출발·도착 두 엣지를 동시에 모핑하므로 리스트로 일반화.
                    var zoneMorphs by remember { mutableStateOf<List<ZoneMorph>>(emptyList()) }
                    // 이동 커밋 시 출발→도착으로 떠다니는 선택 존(이웃 reflow morph와 같은 progress로 구동).
                    var moveFloat by remember { mutableStateOf<ZoneMoveFloat?>(null) }
                    val morphProgress = remember { Animatable(0f) }
                    LaunchedEffect(zoneMorphs) {
                        if (zoneMorphs.isEmpty()) return@LaunchedEffect
                        morphProgress.snapTo(0f)
                        morphProgress.animateTo(
                            1f,
                            tween(EdgeSwipeConstants.EDGE_ZONE_MORPH_MS, easing = FastOutSlowInEasing)
                        )
                        // 떠다니는 존 제거와 displayConfig→workConfig 폴백을 같은 프레임에 맞춤(매끄러운 핸드오프).
                        moveFloat = null
                        zoneMorphs = emptyList()
                    }
                    // 비율 조정 되돌리기(롱프레스/취소) 경계 복원 보간 상태
                    val revertProgress = remember { Animatable(0f) }
                    LaunchedEffect(state.ratioMorph) {
                        if (state.ratioMorph == null) return@LaunchedEffect
                        revertProgress.snapTo(0f)
                        revertProgress.animateTo(
                            1f,
                            tween(EdgeSwipeConstants.EDGE_ZONE_RATIO_MORPH_MS, easing = FastOutSlowInEasing)
                        )
                        state.ratioMorph = null
                    }
                    // 이동 모드 롱프레스 '되돌리고 나가기': 세션 내 이동을 한 단계씩 역순으로 되돌리며
                    // 각 단계 morph(ratioMorph→revertProgress)가 끝날 때까지 대기한 뒤, 모드 선택 화면으로 복귀.
                    LaunchedEffect(state.moveRevertRequested) {
                        if (!state.moveRevertRequested) return@LaunchedEffect
                        // 들어올린 존이 있으면 먼저 내려놓아 드롭 슬롯/고스트를 정리
                        (canvasMode as? CanvasEditMode.Moving)?.let {
                            if (it.picked != null) canvasMode = CanvasEditMode.Moving()
                        }
                        while (state.canRevertMove()) {
                            val from = workConfig
                            state.popMoveUndo()
                            if (from != workConfig) {
                                state.ratioMorph = ConfigMorph(from, workConfig)
                                // 위 ratioMorph LaunchedEffect가 애니메이션 후 null로 정리할 때까지 대기
                                snapshotFlow { state.ratioMorph }.first { it == null }
                            }
                        }
                        canvasMode = CanvasEditMode.None
                        state.moveRevertRequested = false
                    }
                    // NORMAL 비율 프리셋 2단계 적용: 첫 탭에 미리보기로 armed된 프리셋 비율(재탭 시 확정). 엣지가 바뀌면 해제.
                    var pendingPreviewRatios by remember { mutableStateOf<List<Float>?>(null) }
                    val resizeEdge = (canvasMode as? CanvasEditMode.Resizing)?.edge
                    LaunchedEffect(resizeEdge) { pendingPreviewRatios = null }
                    // 비율 조정 프리셋 미리보기: 대상 엣지에 미리보기 비율을 임시 적용해 경계 변형을 보여준다.
                    // SWIPE = 프리셋 칩 포커스 기반, NORMAL = 첫 탭으로 armed된 pendingPreviewRatios 기반.
                    val previewConfig: EdgeZoneConfig? = run {
                        when (val m = canvasMode) {
                            is CanvasEditMode.Resizing -> {
                                val e = m.edge ?: return@run null
                                val ratios = if (inputMode == InputMode.SWIPE) {
                                    val focus = swipeController.currentFocus as? EdgeEditorElement.CanvasRatioPreset ?: return@run null
                                    EdgeZoneActionResolver.ratioPresetsFor(workConfig.zonesFor(e).size)
                                        .firstOrNull { it.first == focus.label }?.second
                                } else {
                                    pendingPreviewRatios
                                } ?: return@run null
                                state.computeRatioZones(workConfig.zonesFor(e), ratios)?.let { workConfig.withZones(e, it) }
                            }
                            // SWIPE 분할: 분할 갯수 버튼 포커스 시 대상 존을 n등분한 미리보기
                            is CanvasEditMode.Splitting -> {
                                if (inputMode != InputMode.SWIPE) return@run null
                                val t = m.target ?: return@run null
                                val n = (swipeController.currentFocus as? EdgeEditorElement.CanvasSplitChoice)?.n ?: return@run null
                                val zone = workConfig.zonesFor(t.edge).firstOrNull { it.startRatio == t.startRatio } ?: return@run null
                                state.computeSplitZones(zone, n)?.let { workConfig.withZones(t.edge, it) }
                            }
                            else -> null
                        }
                    }
                    // 분할 미리보기 애니메이션(SWIPE): 포커스된 분할 갯수가 바뀔 때마다 대상 존이 n등분되는 모핑 재생.
                    // 모핑이 끝나면 zoneMorph는 null로 비워지고 previewConfig(n등분 결과)가 그 상태를 유지한다.
                    val splitPreviewN: Int? = (canvasMode as? CanvasEditMode.Splitting)?.let { m ->
                        if (inputMode != InputMode.SWIPE || m.target == null) null
                        else (swipeController.currentFocus as? EdgeEditorElement.CanvasSplitChoice)?.n
                    }
                    val splitTargetKey = (canvasMode as? CanvasEditMode.Splitting)?.target
                    // 직전 미리보기 (대상 키, 갯수). 같은 대상에서 갯수만 바뀌면 이전 갯수→새 갯수로 자연스럽게 증감.
                    val lastSplitPreview = remember { mutableStateOf<Pair<ZoneKey, Int>?>(null) }
                    LaunchedEffect(splitPreviewN, splitTargetKey) {
                        val n = splitPreviewN
                        val t = splitTargetKey
                        if (n == null || t == null) {
                            lastSplitPreview.value = null
                            return@LaunchedEffect
                        }
                        val zone = workConfig.zonesFor(t.edge).firstOrNull { it.startRatio == t.startRatio } ?: return@LaunchedEffect
                        val prev = lastSplitPreview.value
                        // 같은 대상의 이전 갯수에서 출발(없으면 통째 존=1에서 분할)
                        val fromN = if (prev != null && prev.first == t) prev.second else 1
                        if (fromN != n) {
                            buildSplitCountMorph(workConfig.zonesFor(t.edge), t.edge, zone.startRatio, zone.endRatio, fromN, n)
                                ?.let { zoneMorphs = listOf(it) }
                        }
                        lastSplitPreview.value = t to n
                    }
                    // ── 이동 중 실시간 미리보기(들림 고스트 + 밀림) ──
                    // 활성 드롭 대상을 (edge, insertIndex)로 통일: SWIPE=포커스된 드롭 슬롯, NORMAL 드래그=dropTarget.
                    // NORMAL 탭은 추적이 없으므로 미리보기를 만들지 않음(고스트 미적용, 후보 마커 유지).
                    // NORMAL 드래그 앤 드롭은 SWIPE 이동 미리보기 경로(이웃 reflow lerp + 떠다니는 float)에 합류한다.
                    val isDragMove = effectiveMoveMethod == ZoneMoveMethod.DRAG_AND_DROP
                    val useFloatMovePreview = inputMode == InputMode.SWIPE || isDragMove
                    val pickedKey = (canvasMode as? CanvasEditMode.Moving)?.picked
                    val activeMoveTarget: Pair<EntryEdge, Int>? = (canvasMode as? CanvasEditMode.Moving)?.let { mv ->
                        val picked = mv.picked ?: return@let null
                        if (inputMode == InputMode.SWIPE) {
                            (swipeController.currentFocus as? EdgeEditorElement.CanvasDropSlot)?.let { it.edge to it.insertIndex }
                        } else if (effectiveMoveMethod == ZoneMoveMethod.DRAG_AND_DROP) {
                            mv.dropTarget?.let { it.edge to state.dropInsertIndex(it.edge, it.ratio, picked) }
                        } else null
                    }
                    // 검증 통과 시 computeMove 결과(드래그 중 spring 금지 — 순수 계산 즉시 반영). 실패 시 null(원본 유지).
                    val movingPreview: EdgeZoneConfig? = activeMoveTarget?.let { (edge, insertIndex) ->
                        val picked = (canvasMode as? CanvasEditMode.Moving)?.picked ?: return@let null
                        if (state.validateMove(picked, edge, insertIndex, disabledEdges.keys) != null) null
                        else state.computeMove(picked, edge, insertIndex)
                    }
                    // ── SWIPE 이동 미리보기: picked는 떠다니는 오버레이가 직전 슬롯→새 슬롯으로 점프(translate),
                    //    이웃은 picked 제외 config를 직전→현재로 lerp해 공간을 열고 닫는다(단일 previewAnim 동기). ──
                    val previewAnim = remember { Animatable(1f) }
                    val prevNeighbor = remember { mutableStateOf<EdgeZoneConfig?>(null) }
                    val curNeighbor = remember { mutableStateOf<EdgeZoneConfig?>(null) }
                    val prevLanding = remember { mutableStateOf<ZoneStrip?>(null) }
                    val curLanding = remember { mutableStateOf<ZoneStrip?>(null) }
                    val floatMeta = remember { mutableStateOf<Pair<Int, String>?>(null) }  // colorIndex, label
                    // NORMAL 드래그 들어올림(pick)/내려놓기(settle) 진행도 + settle 완료까지 commit 보류.
                    val liftProgress = remember { Animatable(0f) }
                    val pendingDrop = remember { mutableStateOf<Triple<ZoneKey, EntryEdge, Int>?>(null) }
                    LaunchedEffect(movingPreview, pickedKey) {
                        val mv = movingPreview
                        val picked = (canvasMode as? CanvasEditMode.Moving)?.picked
                        val pz = if (picked != null) workConfig.zonesFor(picked.edge).firstOrNull { it.startRatio == picked.startRatio } else null
                        val meta: Pair<Int, String>? = if (picked != null && pz != null) {
                            val colorIndex = workConfig.zonesFor(picked.edge).indexOfFirst { it.startRatio == picked.startRatio }.coerceAtLeast(0)
                            val label = pz.label.ifEmpty {
                                if (pz.trigger is EdgeZoneTrigger.SingleAction && pz.action !is EdgeZoneAction.Unassigned) pz.action.defaultLabel() else ""
                            }
                            colorIndex to label
                        } else null
                        when {
                            // 드롭 후보 있음(SWIPE 포커스 슬롯 / NORMAL 드래그 dropTarget): 이웃 reflow + float 슬라이드
                            useFloatMovePreview && mv != null && picked != null && pz != null -> {
                                prevNeighbor.value = curNeighbor.value ?: stripPicked(workConfig, pz.trigger)
                                curNeighbor.value = stripPicked(mv, pz.trigger)
                                prevLanding.value = curLanding.value ?: ZoneStrip(picked.edge, pz.startRatio, pz.endRatio)
                                curLanding.value = landingStrip(mv, pz.trigger)
                                floatMeta.value = meta
                                previewAnim.snapTo(0f)
                                previewAnim.animateTo(1f, tween(EdgeSwipeConstants.EDGE_ZONE_MOVE_ANIM_MS, easing = FastOutSlowInEasing))
                            }
                            // NORMAL 드래그 pick 직후/무효 슬롯: picked를 현재 위치(rest)에 띄움(이웃은 gap 유지)
                            isDragMove && picked != null && pz != null -> {
                                val rest = ZoneStrip(picked.edge, pz.startRatio, pz.endRatio)
                                prevNeighbor.value = stripPicked(workConfig, pz.trigger)
                                curNeighbor.value = stripPicked(workConfig, pz.trigger)
                                prevLanding.value = rest
                                curLanding.value = rest
                                floatMeta.value = meta
                                previewAnim.snapTo(1f)
                            }
                            else -> {
                                prevNeighbor.value = null; curNeighbor.value = null
                                prevLanding.value = null; curLanding.value = null
                                previewAnim.snapTo(1f)
                            }
                        }
                    }
                    // 들어올림(pick): 0→1. 취소/거부/빈드래그: 즉시 0 (settle 중 1→0은 아래 effect가 소유).
                    LaunchedEffect(pickedKey, isDragMove) {
                        when {
                            pickedKey != null && isDragMove ->
                                liftProgress.animateTo(1f, tween(EdgeSwipeConstants.EDGE_ZONE_LIFT_MS, easing = FastOutSlowInEasing))
                            pickedKey == null && pendingDrop.value == null ->
                                liftProgress.snapTo(0f)
                        }
                    }
                    // 내려놓기(settle): 현재 슬롯에서 제자리 안착(lift 1→0) 후 commit (미리보기==최종 기하 → 무점프).
                    LaunchedEffect(pendingDrop.value) {
                        val d = pendingDrop.value ?: return@LaunchedEffect
                        previewAnim.animateTo(1f, tween(EdgeSwipeConstants.EDGE_ZONE_MOVE_ANIM_MS, easing = FastOutSlowInEasing))
                        liftProgress.animateTo(0f, tween(EdgeSwipeConstants.EDGE_ZONE_SETTLE_MS, easing = FastOutSlowInEasing))
                        state.commitMove(d.first, d.second, d.third)
                        canvasMode = CanvasEditMode.Moving()
                        pendingDrop.value = null
                    }
                    val previewNeighborConfig: EdgeZoneConfig? = when {
                        useFloatMovePreview && curNeighbor.value != null && prevNeighbor.value != null ->
                            lerpConfig(prevNeighbor.value!!, curNeighbor.value!!, previewAnim.value)
                        else -> curNeighbor.value
                    }
                    // 떠다니는 오버레이로 picked를 표시(직전 위치→현재 위치 translate). SWIPE + NORMAL 드래그 공용.
                    val previewFloat: ZoneMoveFloat? = run {
                        if (!useFloatMovePreview) return@run null
                        val tgt = curLanding.value ?: return@run null
                        val (ci, lbl) = floatMeta.value ?: (0 to "")
                        ZoneMoveFloat(source = prevLanding.value ?: tgt, target = tgt, colorIndex = ci, label = lbl)
                    }
                    // float 경로(SWIPE·NORMAL 드래그)=이웃만(picked는 떠다니는 오버레이), NORMAL 탭=picked 포함 미리보기(들림 고스트).
                    val movingDisplay: EdgeZoneConfig? = if (useFloatMovePreview) previewNeighborConfig else movingPreview
                    val displayConfig = state.ratioMorph
                        ?.let { lerpConfig(it.from, it.to, revertProgress.value) }
                        ?: zoneMorphs.takeIf { it.isNotEmpty() }
                            ?.fold(workConfig) { cfg, m -> cfg.withZones(m.edge, m.frame(morphProgress.value)) }
                        ?: movingDisplay
                        ?: previewConfig
                        ?: workConfig
                    // 들림 고스트 식별(NORMAL 탭 한정 — float 경로는 떠다니는 오버레이가 picked 표시).
                    val liftedKey: ZoneKey? = run {
                        if (useFloatMovePreview || movingDisplay == null) return@run null
                        val pk = (canvasMode as? CanvasEditMode.Moving)?.picked ?: return@run null
                        val trg = workConfig.zonesFor(pk.edge).firstOrNull { it.startRatio == pk.startRatio }?.trigger ?: return@run null
                        EntryEdge.entries.firstNotNullOfOrNull { e ->
                            displayConfig.zonesFor(e).firstOrNull { it.trigger === trg }?.key()
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            // 비율 프리셋 미리보기 중에는 대상 엣지 존 강조를 파랗게 표시 (SWIPE 포커스 / NORMAL 첫 탭 armed 공통).
                            // 분할 미리보기는 분할 기하 자체가 미리보기이므로 파란 강조는 적용하지 않음.
                            val previewActive = previewConfig != null && canvasMode is CanvasEditMode.Resizing
                            val canvasBorderColor = canvasMode.kind?.accentColor()
                            BoxWithConstraints(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .aspectRatio(touchpadAspectRatio, matchHeightConstraintsFirst = true)
                                    .border(2.dp, canvasBorderColor ?: Color.Transparent, RoundedCornerShape(12.dp))
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1A1A1A))
                            ) {
                                // ── 이동 모드 콜백 (Phase 4.7.x) ──
                                // 존 선택(들어올림). 출발 엣지가 1개뿐이면 거부. settle 진행 중이면 재터치 무시.
                                val movingPick: (EdgeZone) -> Unit = { zone ->
                                    if (pendingDrop.value != null) {
                                        // 직전 이동의 안착 애니메이션이 끝날 때까지 새 들어올림을 무시(in-flight 상태 보호)
                                    } else if (workConfig.zonesFor(zone.edge).size <= 1) {
                                        ToastController.show(EdgeZoneEditorState.MoveRejection.SourceLastZone.message, ToastType.WARNING)
                                    } else {
                                        canvasMode = CanvasEditMode.Moving(picked = zone.key())
                                    }
                                }
                                // 이동 커밋 공용: 이웃 reflow morph + 떠다니는 선택 존(출발→도착)을 동시 구동.
                                // 검증 통과를 가정(호출부가 validateMove로 사전 확인). 연속 이동을 위해 picked만 해제.
                                val runMoveCommit: (ZoneKey, EntryEdge, Int) -> Unit = { picked, edge, insertIndex ->
                                    val before = workConfig
                                    val pickedZone = before.zonesFor(picked.edge).firstOrNull { it.startRatio == picked.startRatio }
                                    val colorIndex = before.zonesFor(picked.edge).indexOfFirst { it.startRatio == picked.startRatio }
                                    val morphs = state.commitMove(picked, edge, insertIndex)
                                    if (workConfig != before && pickedZone != null) {
                                        zoneMorphs = morphs
                                        // 도착 위치는 trigger 참조(===)로 after(=workConfig)에서 탐색 (liftedKey와 동일 패턴)
                                        val tgtStrip = EntryEdge.entries.firstNotNullOfOrNull { e ->
                                            workConfig.zonesFor(e).firstOrNull { it.trigger === pickedZone.trigger }
                                                ?.let { ZoneStrip(e, it.startRatio, it.endRatio) }
                                        }
                                        if (tgtStrip != null) {
                                            val label = pickedZone.label.ifEmpty {
                                                if (pickedZone.trigger is EdgeZoneTrigger.SingleAction && pickedZone.action !is EdgeZoneAction.Unassigned)
                                                    pickedZone.action.defaultLabel() else ""
                                            }
                                            moveFloat = ZoneMoveFloat(
                                                source = ZoneStrip(picked.edge, pickedZone.startRatio, pickedZone.endRatio),
                                                target = tgtStrip,
                                                colorIndex = colorIndex.coerceAtLeast(0),
                                                label = label,
                                            )
                                        }
                                    }
                                    canvasMode = CanvasEditMode.Moving()
                                }
                                // 탭 드롭 확정 (경계/양 끝 → insertIndex).
                                val movingDropTap: (EntryEdge, Float) -> Unit = { edge, ratio ->
                                    val picked = (canvasMode as? CanvasEditMode.Moving)?.picked
                                    if (picked != null) {
                                        val insertIndex = state.dropInsertIndex(edge, ratio, picked)
                                        val rej = state.validateMove(picked, edge, insertIndex, disabledEdges.keys)
                                        if (rej != null) ToastController.show(rej.message, ToastType.WARNING)
                                        else runMoveCommit(picked, edge, insertIndex)
                                    }
                                }
                                // SWIPE 드롭 슬롯 확정 (insertIndex 직접).
                                // 미리보기 자체가 최종 상태(이웃 + 떠다니는 존)이므로 commit은 무점프 스냅. morph/float 미사용.
                                val movingSlotDrop: (EntryEdge, Int) -> Unit = { edge, insertIndex ->
                                    val picked = (canvasMode as? CanvasEditMode.Moving)?.picked
                                    if (picked != null) {
                                        val rej = state.validateMove(picked, edge, insertIndex, disabledEdges.keys)
                                        if (rej != null) ToastController.show(rej.message, ToastType.WARNING)
                                        else {
                                            state.commitMove(picked, edge, insertIndex)
                                            canvasMode = CanvasEditMode.Moving()
                                        }
                                    }
                                }
                                // 드래그 중 드롭 위치 갱신 (실시간 미리보기는 displayConfig가 반영).
                                val movingDrag: (DropTarget) -> Unit = { dt ->
                                    (canvasMode as? CanvasEditMode.Moving)?.let { canvasMode = it.copy(dropTarget = dt) }
                                }
                                // 드래그 릴리스: 유효하면 안착 애니메이션(settle effect)이 끝난 뒤 commit하도록 pendingDrop만 설정.
                                val movingDragEnd: () -> Unit = {
                                    val mv = canvasMode as? CanvasEditMode.Moving
                                    val picked = mv?.picked
                                    val dt = mv?.dropTarget
                                    if (picked != null && dt != null) {
                                        val insertIndex = state.dropInsertIndex(dt.edge, dt.ratio, picked)
                                        val rej = state.validateMove(picked, dt.edge, insertIndex, disabledEdges.keys)
                                        if (rej == null) pendingDrop.value = Triple(picked, dt.edge, insertIndex)
                                        else { ToastController.show(rej.message, ToastType.WARNING); canvasMode = CanvasEditMode.Moving() }
                                    } else canvasMode = CanvasEditMode.Moving()
                                }
                                val movingCancel: () -> Unit = { canvasMode = CanvasEditMode.Moving() }

                                // 모드별 존 상호작용: None=편집 진입, Deleting=선택 토글, Moving=픽
                                val onZoneInteract: (EdgeZone) -> Unit = { zone ->
                                    when (val m = canvasMode) {
                                        is CanvasEditMode.None -> { selectedZone = zone; selectedEdge = zone.edge }
                                        is CanvasEditMode.Deleting -> {
                                            val k = zone.key()
                                            if (k in m.selected) {
                                                canvasMode = m.copy(selected = m.selected - k)
                                            } else if (workConfig.zonesFor(zone.edge).size <= 1) {
                                                ToastController.show("엣지에 존이 하나뿐이라 삭제할 수 없어요", ToastType.WARNING)
                                            } else {
                                                canvasMode = m.copy(selected = m.selected + k)
                                            }
                                        }
                                        is CanvasEditMode.Splitting -> {
                                            // 이미 최대 존 개수인 엣지는 더 나눌 수 없으므로 갯수 선택 화면으로 진입하지 않고 안내 토스트
                                            if (workConfig.zonesFor(zone.edge).size >= EdgeSwipeConstants.MAX_ZONES_PER_EDGE.toInt()) {
                                                ToastController.show("존이 ${EdgeSwipeConstants.MAX_ZONES_PER_EDGE.toInt()}개로 가득 차 더 나눌 수 없어요", ToastType.WARNING)
                                            } else {
                                                // 분할 대상 존 지정 (재탭으로 변경 가능)
                                                canvasMode = m.copy(target = zone.key())
                                            }
                                        }
                                        is CanvasEditMode.Resizing -> {
                                            // 존이 2개 이상인 엣지만 비율 프리셋 대상으로 선택 가능
                                            if (workConfig.zonesFor(zone.edge).size >= 2) {
                                                // 새 엣지를 선택하는 순간 스냅샷 — 이후 프리셋 적용을 패널 취소로 일괄 되돌릴 수 있게
                                                if (m.edge != zone.edge) state.beginResizeSession()
                                                canvasMode = m.copy(edge = zone.edge)
                                            } else {
                                                ToastController.show("존이 하나뿐인 가장자리는 비율을 나눌 수 없어요", ToastType.WARNING)
                                            }
                                        }
                                        is CanvasEditMode.Merging -> {
                                            val e = m.edge
                                            val base = m.base
                                            when {
                                                // 첫 선택 = 기준(base) 존
                                                e == null || base == null -> canvasMode = m.copy(edge = zone.edge, base = zone.startRatio)
                                                // 다른 엣지 존은 선택 불가
                                                zone.edge != e -> ToastController.show("같은 가장자리의 인접한 존만 선택할 수 있어요", ToastType.WARNING)
                                                // 기준 존 재탭 → 선택 초기화
                                                zone.startRatio == base -> canvasMode = CanvasEditMode.Merging()
                                                else -> {
                                                    // 현재 선택 구간 [lo, hi]에 인접한 존만 확장/축소 허용
                                                    val zones = workConfig.zonesFor(e)
                                                    val tapIdx = zones.indexOfFirst { it.startRatio == zone.startRatio }
                                                    val selIndices = (m.selected + base)
                                                        .mapNotNull { sr -> zones.indexOfFirst { it.startRatio == sr }.takeIf { it >= 0 } }
                                                        .sorted()
                                                    val lo = selIndices.firstOrNull() ?: -1
                                                    val hi = selIndices.lastOrNull() ?: -1
                                                    when {
                                                        tapIdx < 0 -> {}
                                                        // 구간 양 끝 바깥 인접 → 추가
                                                        tapIdx == lo - 1 || tapIdx == hi + 1 ->
                                                            canvasMode = m.copy(selected = m.selected + zone.startRatio)
                                                        // 구간 양 끝(기준 제외) → 해제
                                                        tapIdx == lo || tapIdx == hi ->
                                                            canvasMode = m.copy(selected = m.selected - zone.startRatio)
                                                        // 비인접(멀거나 구간 내부) → 거부 + 안내
                                                        else -> ToastController.show("인접한 존만 선택할 수 있어요", ToastType.WARNING)
                                                    }
                                                }
                                            }
                                        }
                                        is CanvasEditMode.Moving -> {
                                            // 캔버스 탭(SWIPE hit overlay 포함): picked 없을 때만 들어올림. 드롭은 전용 경로.
                                            if (m.picked == null) movingPick(zone)
                                        }
                                        else -> {}
                                    }
                                }
                                EdgeZoneEditorPreviewCanvas(
                                    config = displayConfig,
                                    highlightKeys = when {
                                        // 병합 모드는 존별 개별 강조 대신 아래 MergeSelectionOverlay가 연속 영역을 애니메이션으로 강조
                                        canvasMode is CanvasEditMode.Merging -> emptySet()
                                        // 분할 미리보기: 대상 존이 나뉜 모든 조각을 녹색 강조 (part0만 강조되던 문제 해소)
                                        splitPreviewN != null && splitTargetKey != null -> {
                                            val t = splitTargetKey
                                            val orig = workConfig.zonesFor(t.edge).firstOrNull { it.startRatio == t.startRatio }
                                            if (orig != null) {
                                                displayConfig.zonesFor(t.edge)
                                                    .filter { val c = (it.startRatio + it.endRatio) / 2f; c > orig.startRatio && c < orig.endRatio }
                                                    .map { it.key() }.toSet()
                                            } else canvasHighlightKeys(canvasMode, selectedZone, displayConfig)
                                        }
                                        else -> canvasHighlightKeys(canvasMode, selectedZone, displayConfig)
                                    },
                                    resizeMode = canvasMode is CanvasEditMode.Resizing,
                                    highlightAsPreview = previewActive,
                                    liftedKey = liftedKey,
                                    interactive = zoneMorphs.isEmpty() && state.ratioMorph == null,
                                    disabledEdges = disabledEdges,
                                    bottomLeftButtonLabel = bottomLeftButtonLabel,
                                    bottomRightButtonLabel = bottomRightButtonLabel,
                                    onZoneTapped = onZoneInteract,
                                    onCornerPriorityToggled = { corner ->
                                        state.pushUndo()
                                        workConfig = workConfig.toggleCornerPriority(corner)
                                        currentPresetId = null
                                    },
                                    blockedRatio = cornerBlockedRatio,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // ── 떠다니는 선택 존 (캔버스 위에 그림) ──
                                // float 미리보기(SWIPE·NORMAL 드래그): 직전 슬롯→현재 슬롯 점프(previewAnim) + lift(들어올림/안착).
                                // NORMAL 탭 커밋: 출발→도착(morphProgress), lift 미적용.
                                (previewFloat?.let { Triple(it, previewAnim.value, if (isDragMove) liftProgress.value else 1f) }
                                    ?: moveFloat?.let { Triple(it, morphProgress.value, 1f) })
                                    ?.let { (f, p, lift) ->
                                        ZoneMoveFloatingOverlay(
                                            float = f,
                                            progress = p,
                                            canvasWidth = maxWidth,
                                            canvasHeight = maxHeight,
                                            cornerPriority = workConfig.cornerPriority,
                                            hasBottomLeft = bottomLeftButtonLabel != null,
                                            hasBottomRight = bottomRightButtonLabel != null,
                                            lift = lift,
                                            blockedRatio = cornerBlockedRatio,
                                        )
                                    }

                                // ── 병합 모드: 연속 선택 구간을 애니메이션 영역 박스로 강조 (NORMAL/SWIPE 공통) ──
                                (canvasMode as? CanvasEditMode.Merging)?.let { mm ->
                                    val mEdge = mm.edge
                                    val mBase = mm.base
                                    if (mEdge != null && mBase != null) {
                                        val sel = mm.selected + mBase
                                        val selZones = displayConfig.zonesFor(mEdge).filter { it.startRatio in sel }
                                        if (selZones.isNotEmpty()) {
                                            MergeSelectionOverlay(
                                                edge = mEdge,
                                                regionStartRatio = selZones.minOf { it.startRatio },
                                                regionEndRatio = selZones.maxOf { it.endRatio },
                                                canvasWidth = maxWidth,
                                                canvasHeight = maxHeight,
                                                hasBottomLeft = bottomLeftButtonLabel != null,
                                                hasBottomRight = bottomRightButtonLabel != null,
                                                blockedRatio = cornerBlockedRatio,
                                            )
                                        }
                                    }
                                }

                                // ── SWIPE 모드 캔버스 hit 영역 오버레이 (NORMAL에서는 미렌더) ──
                                // 존 단위로 분해. 비활성 엣지만 등록 생략 (Unassigned 존은 포함).
                                if (inputMode == InputMode.SWIPE && !editing) {
                                    if (canvasMode is CanvasEditMode.Resizing) {
                                        // 비율 조정: 존 hit(엣지 선택용)을 먼저, 경계 manipulation을 위에 렌더
                                        ZoneCanvasHitOverlay(
                                            workConfig = workConfig,
                                            disabledEdges = disabledEdges,
                                            bottomLeftButtonLabel = bottomLeftButtonLabel,
                                            bottomRightButtonLabel = bottomRightButtonLabel,
                                            canvasWidth = maxWidth,
                                            canvasHeight = maxHeight,
                                            onZoneSelected = onZoneInteract,
                                            focusBackground = true,
                                            blockedRatio = cornerBlockedRatio,
                                        )
                                        ZoneCanvasResizeOverlay(
                                            workConfig = displayConfig,
                                            disabledEdges = disabledEdges,
                                            canvasWidth = maxWidth,
                                            canvasHeight = maxHeight,
                                            onAdjust = { edge, leftIdx, ratio -> state.adjustBoundary(edge, leftIdx, ratio) },
                                            hasBottomLeft = bottomLeftButtonLabel != null,
                                            hasBottomRight = bottomRightButtonLabel != null,
                                            blockedRatio = cornerBlockedRatio,
                                        )
                                        // 경계 탭 확정(MANIPULATION) 시 해당 엣지 옆에 이동 데모(화살표+손가락)를 잠시 표시
                                        val fb = swipeController.currentFocus as? EdgeEditorElement.CanvasBoundary
                                        if (swipeController.mode == SwipeMode.MANIPULATION && fb != null) {
                                            workConfig.zonesFor(fb.edge).getOrNull(fb.leftIndex + 1)?.startRatio?.let { br ->
                                                // key: 경계가 바뀌면(새 MANIPULATION 진입) 힌트를 재생성해 타이머/이동감지 리셋
                                                androidx.compose.runtime.key(fb.edge, fb.leftIndex) {
                                                    BoundaryManipulationHint(
                                                        edge = fb.edge,
                                                        boundaryRatio = br,
                                                        canvasWidth = maxWidth,
                                                        canvasHeight = maxHeight,
                                                        hasBottomLeft = bottomLeftButtonLabel != null,
                                                        hasBottomRight = bottomRightButtonLabel != null,
                                                        blockedRatio = cornerBlockedRatio,
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        val movingPicked = (canvasMode as? CanvasEditMode.Moving)?.picked
                                        if (movingPicked != null) {
                                            // 이동 모드 2단계: 드롭 슬롯(경계+양 끝)을 포커스 대상으로 렌더
                                            ZoneCanvasDropOverlay(
                                                workConfig = workConfig,
                                                picked = movingPicked,
                                                disabledEdges = disabledEdges,
                                                canvasWidth = maxWidth,
                                                canvasHeight = maxHeight,
                                                isValidSlot = { edge, insertIndex ->
                                                    state.validateMove(movingPicked, edge, insertIndex, disabledEdges.keys) == null
                                                },
                                                onSlotDrop = movingSlotDrop,
                                                showMarker = false,  // 들림 고스트가 위치를 대신 표시
                                                hasBottomLeft = bottomLeftButtonLabel != null,
                                                hasBottomRight = bottomRightButtonLabel != null,
                                                blockedRatio = cornerBlockedRatio,
                                            )
                                        } else {
                                            // 1단계(존 선택) 또는 그 외 모드: 존 hit 오버레이
                                            ZoneCanvasHitOverlay(
                                                workConfig = workConfig,
                                                disabledEdges = disabledEdges,
                                                bottomLeftButtonLabel = bottomLeftButtonLabel,
                                                bottomRightButtonLabel = bottomRightButtonLabel,
                                                canvasWidth = maxWidth,
                                                canvasHeight = maxHeight,
                                                onZoneSelected = onZoneInteract,
                                                blockedRatio = cornerBlockedRatio,
                                            )
                                        }
                                    }
                                }
                                // ── NORMAL 이동 탭: 드롭 후보 시각 힌트 (탭 감지는 inputModifier가 담당) ──
                                // SwipeFocusable은 NORMAL에서 컨트롤러가 null이라 마커만 렌더되고 클릭은 통과한다.
                                if (inputMode == InputMode.NORMAL && effectiveMoveMethod == ZoneMoveMethod.TAP && !editing) {
                                    (canvasMode as? CanvasEditMode.Moving)?.picked?.let { picked ->
                                        ZoneCanvasDropOverlay(
                                            workConfig = workConfig,
                                            picked = picked,
                                            disabledEdges = disabledEdges,
                                            canvasWidth = maxWidth,
                                            canvasHeight = maxHeight,
                                            isValidSlot = { edge, insertIndex ->
                                                state.validateMove(picked, edge, insertIndex, disabledEdges.keys) == null
                                            },
                                            onSlotDrop = { _, _ -> },  // NORMAL은 inputModifier가 처리
                                            hasBottomLeft = bottomLeftButtonLabel != null,
                                            hasBottomRight = bottomRightButtonLabel != null,
                                            blockedRatio = cornerBlockedRatio,
                                        )
                                    }
                                }
                                if (!editing) {
                                    // 코너 버튼 차단 영역 크기 조절 (캔버스 씬, 모드 미진입 시). 조절 즉시 저장 + 미리보기 갱신.
                                    if (canvasMode is CanvasEditMode.None) {
                                        Column(
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .padding(bottom = 30.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                        ) {
                                            Text(
                                                "코너 버튼 크기",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                            )
                                            Box(modifier = Modifier.padding(top = 1.dp).width(150.dp)) {
                                                CustomTrackSlider(
                                                    value = cornerBlockedRatio,
                                                    onValueChange = { cornerBlockedRatio = it },
                                                    valueRange = 0.05f..0.30f,
                                                    valueLabel = "${(cornerBlockedRatio * 100).roundToInt()}%",
                                                    labelWidth = 34.dp,
                                                    snap = { (it * 100).roundToInt() / 100f },
                                                    minorTickStep = 0.05f,
                                                    labelFontWeight = FontWeight.Bold,
                                                    element = EdgeEditorElement.CornerBlockedSlider,
                                                )
                                            }
                                        }
                                    }
                                    EdgeZoneCanvasModeOverlay(
                                        canvasMode = canvasMode,
                                        blockedRatio = cornerBlockedRatio,
                                        manipulating = inputMode == InputMode.SWIPE &&
                                            swipeController.mode == SwipeMode.MANIPULATION &&
                                            swipeController.currentFocus is EdgeEditorElement.CanvasBoundary,
                                        onModeChange = { newMode ->
                                            // 비율 조정 모드 진입 시점 스냅샷 — 안내 카드 '취소'로 경계 드래그까지 일괄 원복
                                            if (canvasMode !is CanvasEditMode.Resizing && newMode is CanvasEditMode.Resizing) {
                                                state.beginResizeMode()
                                            }
                                            // 이동 모드 진입 시점 기록 — 롱프레스 '되돌리고 나가기'가 이 시점까지 역순 복원
                                            if (canvasMode is CanvasEditMode.None && newMode is CanvasEditMode.Moving) {
                                                state.beginMoveMode()
                                            }
                                            canvasMode = newMode
                                        },
                                        onConfirm = {
                                            when (val m = canvasMode) {
                                                is CanvasEditMode.Deleting -> {
                                                    state.deleteZones(m.selected)
                                                    canvasMode = CanvasEditMode.None
                                                }
                                                is CanvasEditMode.Merging -> {
                                                    val e = m.edge
                                                    val b = m.base
                                                    if (e != null && b != null) {
                                                        // 병합 직전 상태 캡처 (애니메이션 before 프레임)
                                                        val beforeZones = workConfig.zonesFor(e)
                                                        if (state.mergeContiguous(e, b, m.selected + b)) {
                                                            // mergeContiguous가 selectedZone에 병합 결과를 담음 → 포커스용으로 캡처
                                                            val merged = selectedZone
                                                            canvasMode = CanvasEditMode.None
                                                            // 병합 직후 편집 씬으로 전환되지 않도록 선택 해제
                                                            selectedZone = null
                                                            selectedEdge = null
                                                            // stretch 애니메이션 시작
                                                            buildMergeMorph(beforeZones, e, b, m.selected + b)?.let { zoneMorphs = listOf(it) }
                                                            // SWIPE: 방금 병합된 존에 포커스
                                                            if (merged != null) {
                                                                val idx = workConfig.zonesFor(e).indexOfFirst { it.startRatio == merged.startRatio }
                                                                if (idx >= 0) swipeController.setFocus(EdgeEditorElement.CanvasZone(e, idx))
                                                            }
                                                        }
                                                    }
                                                }
                                                // 이동 모드 '확인': 지금까지의 이동을 유지한 채 모드 선택 화면으로 복귀
                                                is CanvasEditMode.Moving -> {
                                                    canvasMode = CanvasEditMode.None
                                                }
                                                else -> {}
                                            }
                                        },
                                        config = workConfig,
                                        disabledEdges = disabledEdges,
                                        bottomLeftButtonLabel = bottomLeftButtonLabel,
                                        bottomRightButtonLabel = bottomRightButtonLabel,
                                        onZoneInteract = onZoneInteract,
                                        moveMethod = effectiveMoveMethod,
                                        onMovingPick = movingPick,
                                        onMovingDropTap = movingDropTap,
                                        onMovingDrag = movingDrag,
                                        onMovingDragEnd = movingDragEnd,
                                        onMovingCancel = movingCancel,
                                        onMovingLongCancel = {
                                            // 롱프레스: 세션 내 이동을 역순 애니메이션으로 모두 되돌린 뒤 모드 선택 화면으로 복귀
                                            state.moveRevertRequested = true
                                        },
                                        movingRevertInProgress = state.moveRevertRequested,
                                        onSplitInto = { n ->
                                            val t = (canvasMode as? CanvasEditMode.Splitting)?.target
                                            if (t != null) {
                                                val zone = workConfig.zonesFor(t.edge).firstOrNull { it.startRatio == t.startRatio }
                                                if (zone != null) {
                                                    // 분할 전 비율 캡처 (애니메이션 before 프레임)
                                                    val origStart = zone.startRatio
                                                    val origEnd = zone.endRatio
                                                    if (state.splitInto(zone, n)) {
                                                        canvasMode = CanvasEditMode.None
                                                        // 분할 직후 편집 씬 전환 방지
                                                        selectedZone = null
                                                        selectedEdge = null
                                                        // shrink→grow 애니메이션 시작
                                                        val afterZones = workConfig.zonesFor(t.edge)
                                                        buildSplitMorph(afterZones, t.edge, origStart, origEnd, n)?.let { zoneMorphs = listOf(it) }
                                                    }
                                                }
                                            }
                                        },
                                        onResizeStart = {
                                            // 경계 직접 드래그 시작 → armed된 프리셋 미리보기 해제
                                            pendingPreviewRatios = null
                                            state.pushUndo()
                                        },
                                        onResize = { edge, leftIdx, ratio -> state.adjustBoundary(edge, leftIdx, ratio) },
                                        previewedRatios = if (inputMode == InputMode.SWIPE) null else pendingPreviewRatios,
                                        onApplyPreset = { edge, ratios ->
                                            // 공통: 현재 보이는 config에서 적용 결과로 부드럽게 전환(미리보기가 이미 결과면 before==after → 점프 없음).
                                            val commit: () -> Unit = {
                                                val before = displayConfig
                                                state.applyRatioPreset(edge, ratios)
                                                if (before != workConfig) state.ratioMorph = ConfigMorph(before, workConfig)
                                            }
                                            if (inputMode == InputMode.SWIPE) {
                                                // SWIPE: 포커스가 미리보기 역할 → 탭이 곧 확정. 적용 후 안내 카드 단계로 이동하고 '확인' 포커스.
                                                commit()
                                                canvasMode = CanvasEditMode.Resizing()
                                                swipeController.setFocus(EdgeEditorElement.CanvasModeConfirm)
                                            } else if (pendingPreviewRatios == ratios) {
                                                // NORMAL 2단계: 같은 프리셋 재탭 → 확정 후 안내 카드 단계로 복귀
                                                commit()
                                                pendingPreviewRatios = null
                                                canvasMode = CanvasEditMode.Resizing()
                                            } else {
                                                // NORMAL 2단계: 첫 탭 → 미리보기(armed). 현재 표시 config에서 미리보기 비율로 부드럽게 전환.
                                                val before = displayConfig
                                                val previewCfg = state.computeRatioZones(workConfig.zonesFor(edge), ratios)
                                                    ?.let { workConfig.withZones(edge, it) }
                                                pendingPreviewRatios = ratios
                                                if (previewCfg != null && before != previewCfg) state.ratioMorph = ConfigMorph(before, previewCfg)
                                            }
                                        },
                                        onResizeSessionDiscard = { state.discardResizeSession() },
                                        onResizeModeConfirm = { state.commitResizeMode(); canvasMode = CanvasEditMode.None },
                                        onResizeModeCancel = { state.discardResizeMode(); canvasMode = CanvasEditMode.None },
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            }
                    }
                } else {
                    // ── 편집 씬 ──
                    Box(modifier = Modifier.fillMaxSize()) {
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
                        val zoneIdx = if (sel != null) zoneList.indexOfFirst { it.startRatio == sel.startRatio && it.edge == sel.edge } else -1

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(formScroll)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {

                            // 비율 섹션(EdgeStripEditor·프리셋·병합/분할/이동/삭제 팝업)은 캔버스 모드로 이전됨(Phase 4.7.x).
                            // 편집 씬에는 라벨/액션/아이콘 편집만 남긴다.

                            if (sel != null) {
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
                                    ZoneDisplaySettingSection(
                                        sel = sel,
                                        labelCursorAlpha = labelCursorAlpha,
                                        showLabelKeyboard = showLabelKeyboard,
                                        onRequestIconSheet = { iconDrawerStage = IconDrawerStage.Category; showIconSheet = true },
                                        onRequestColorPicker = { showColorPicker = true },
                                        onRequestLabelKeyboard = { showLabelKeyboard = true },
                                        onIconBoxPositioned = { iconBoxCenterInWindow = it },
                                        onColorBoxPositioned = { colorBoxCenterInWindow = it },
                                        onRevertBoxPositioned = { revertBtnBoundsInWindow = it },
                                        onRevertToAuto = {
                                            val t = sel.trigger as EdgeZoneTrigger.SingleAction
                                            updateSelectedZone(sel.copy(trigger = t.copy(label = "", iconKey = "", colorHex = "")))
                                        },
                                    )
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
                    } // Box(fillMaxSize) 편집 씬
                } // else (editing 씬)
            } // AnimatedContent
        } // BoxWithConstraints

    }

        // ── 오버레이/팝업 레이어 (SWIPE 힌트·툴팁·서랍·피커·팝업·제스처 + NORMAL 미니 툴팁·매크로) ──
        // Phase 4.7.5-D: 무조건 호출, 내부에서 inputMode 분기 유지(byte-identical). 모드 무관 프리셋 팝업과
        // NORMAL 매크로 편집기가 SWIPE 체인 중간에 섞여 있어 호출부 모드 가드 없이 통째로 분리.
        EdgeZoneOverlayLayer(
            overlayUi = overlayUi,
            state = state,
            swipeController = swipeController,
            presetsRepo = presetsRepo,
            localCustomPresets = localCustomPresets,
            updateSelectedZone = { updateSelectedZone(it) },
            zonePopupState = zonePopupState,
            canvasModeState = canvasModeState,
        )
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

    // ── NORMAL 모드 전용 바텀시트 레이어 ──
    if (inputMode == InputMode.NORMAL) {
        NormalSheetLayer(
            selectedZone = selectedZone,
            iconSheetState = iconSheetState,
            showIconSheet = showIconSheet,
            onShowIconSheetChange = { showIconSheet = it },
            showCandidateIconSheet = showCandidateIconSheet,
            onShowCandidateIconSheetChange = { showCandidateIconSheet = it },
            showColorPicker = showColorPicker,
            onShowColorPickerChange = { showColorPicker = it },
            showCandidateColorPicker = showCandidateColorPicker,
            onShowCandidateColorPickerChange = { showCandidateColorPicker = it },
            rotationDraft = rotationDraft,
            onRotationDraftChange = { rotationDraft = it },
            updateSelectedZone = { updateSelectedZone(it) },
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
                saveCornerBlockedRatio(context, cornerBlockedRatio)
                onSave(workConfig, currentPresetId)
            }
            showDiscardDialog = false
        }
        val discardDiscardAction: () -> Unit = { showDiscardDialog = false; onBack() }
        val discardCancelAction: () -> Unit = { showDiscardDialog = false }

        if (inputMode == InputMode.SWIPE) {
            // SWIPE 모드: Popup은 별도 Android 윈도우라 메인 SwipeGestureLayer에 터치가 도달하지 않음.
            // Box 최상단에 SwipeGestureLayer를 오버레이해 Popup 내 터치를 직접 처리.
            Popup(
                alignment = Alignment.Center,
                properties = PopupProperties(focusable = false),
            ) {
            Box {
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
                            showFlashOverlay = false,
                            onActivate = discardCancelAction,
                            gridRow = 0,
                            modifier = Modifier.weight(1f),
                        ) {
                            val cancelFocused = LocalSwipeFocused.current
                            val cancelFlash = LocalSwipeFlashAlpha.current
                            FilledTonalButton(
                                onClick = {},
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    contentColor = if (cancelFocused) cs.onPrimary else cs.onSurface,
                                    containerColor = lerp(
                                        if (cancelFocused) cs.primary else Color.Transparent,
                                        cs.error,
                                        cancelFlash,
                                    ),
                                )
                            ) { Text("취소", fontSize = 14.sp) }
                        }
                        SwipeFocusable(
                            element = EdgeEditorElement.DiscardDialogSave,
                            scope = EdgeEditorScope.DiscardDialog,
                            shape = RoundedCornerShape(8.dp),
                            showBorderHighlight = false,
                            showFlashOverlay = false,
                            onActivate = discardSaveAction,
                            gridRow = 0,
                            modifier = Modifier.weight(1f),
                        ) {
                            val saveFocused = LocalSwipeFocused.current
                            val saveFlash = LocalSwipeFlashAlpha.current
                            FilledTonalButton(
                                onClick = {},
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    contentColor = if (saveFocused) cs.onPrimary else cs.primary,
                                    containerColor = lerp(
                                        if (saveFocused) cs.primary else Color.Transparent,
                                        cs.error,
                                        saveFlash,
                                    ),
                                )
                            ) { Text("저장", fontSize = 14.sp) }
                        }
                        SwipeFocusable(
                            element = EdgeEditorElement.DiscardDialogDiscard,
                            scope = EdgeEditorScope.DiscardDialog,
                            shape = RoundedCornerShape(8.dp),
                            showBorderHighlight = false,
                            showFlashOverlay = false,
                            onActivate = discardDiscardAction,
                            gridRow = 0,
                            modifier = Modifier.weight(2f),
                        ) {
                            val discardFocused = LocalSwipeFocused.current
                            val discardFlash = LocalSwipeFlashAlpha.current
                            FilledTonalButton(
                                onClick = {},
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    contentColor = if (discardFocused) cs.onError else cs.error,
                                    containerColor = lerp(
                                        if (discardFocused) cs.error else Color.Transparent,
                                        Color.Black,
                                        discardFlash * 0.4f,
                                    ),
                                )
                            ) { Text("버리고 나가기", fontSize = 14.sp) }
                        }
                        }
                    }
                }
                SwipeGestureLayer(
                    controller = swipeController,
                    modifier = Modifier.matchParentSize()
                )
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

// Phase 4.7.5-A: migrateDynamicsIndicesAfterDelete → EdgeZoneActionResolver.kt로 이관

