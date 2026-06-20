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
import com.bridgeone.app.ui.common.swipe.LocalSwipeFocusController
import com.bridgeone.app.ui.common.swipe.LocalSwipeFocused
import com.bridgeone.app.ui.common.swipe.LocalSwipeFlashAlpha
import com.bridgeone.app.ui.common.swipe.SwipeFocusable
import com.bridgeone.app.ui.common.swipe.SwipeGestureLayer
import com.bridgeone.app.ui.common.swipe.SwipeMode
import com.bridgeone.app.ui.common.swipe.rememberSwipeFocusController


import com.bridgeone.app.ui.components.SwipeKeyboardOverlay
import androidx.compose.ui.draw.alpha

/** 커스텀 배율 슬라이더 트랙 높이 (dp). 기본값: 28f */
internal const val CUSTOM_SLIDER_TRACK_HEIGHT_DP = 28f
/** 커스텀 배율 슬라이더 손잡이 세로선 너비 (dp). 기본값: 3f */
internal const val CUSTOM_SLIDER_LINE_WIDTH_DP = 3f

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
    var canvasVisible by remember { mutableStateOf(true) }
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
            }
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
        // ZoneActionPopup.Initial 진입 시 가운데 "분할" 버튼으로 초기 포커스
        val isInitialPopup = zonePopup is ZoneActionPopup.Initial
        LaunchedEffect(isInitialPopup) {
            if (isInitialPopup) swipeController.setFocus(EdgeEditorElement.ZoneActionSplit)
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
                                    ZoneCanvasHitOverlay(
                                        workConfig = workConfig,
                                        disabledEdges = disabledEdges,
                                        bottomLeftButtonLabel = bottomLeftButtonLabel,
                                        bottomRightButtonLabel = bottomRightButtonLabel,
                                        canvasWidth = maxWidth,
                                        canvasHeight = maxHeight,
                                        onZoneSelected = { zone ->
                                            selectedZone = zone
                                            selectedEdge = zone.edge
                                            canvasVisible = false
                                        },
                                    )
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
                        val zoneIdx = if (sel != null) zoneList.indexOfFirst { it.startRatio == sel.startRatio && it.edge == sel.edge } else -1

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(formScroll)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {

                            ZoneRatioSection(
                                state = state,
                                zonePopupState = zonePopupState,
                                sel = sel,
                                edgeForStrip = edgeForStrip,
                                zoneList = zoneList,
                                zoneIdx = zoneIdx,
                                minRatio = minRatio,
                                maxZones = maxZones,
                                stripBlockedStart = stripBlockedStart,
                                stripBlockedStartLabel = stripBlockedStartLabel,
                                stripBlockedEnd = stripBlockedEnd,
                                stripBlockedEndLabel = stripBlockedEndLabel,
                                swipeController = swipeController,
                                onRatioBtnBoundsChange = { ratioBtnBoundsInWindow = it },
                            )

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
                }
            } // Column (BoxWithConstraints 내부)
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

