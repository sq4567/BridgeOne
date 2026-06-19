package com.bridgeone.app.ui.components.touchpad

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

/**
 * EdgeZoneEditorScreen의 오버레이/팝업 UI 상태 홀더 (Phase 4.7.5-D).
 *
 * [EdgeZoneEditorState]가 config 변환·Undo 같은 순수 상태 전이를 담당하는 것과 분리하여,
 * 본 홀더는 SWIPE 오버레이 레이어(`SwipeOverlayLayer`)와 편집 패널이 공유하는 **UI 사이드이펙트
 * 상태**(서랍/피커/키보드 표시 여부, 앵커 좌표, 임시 draft 등)를 한데 모은다.
 *
 * 도입 목적: 540줄 규모의 SWIPE 오버레이 체인을 별도 Composable로 분리할 때 30개 이상의 개별
 * 파라미터를 넘기는 대신 이 홀더 1개를 전달해 파라미터 수를 줄인다. Composable은 기존 지역 변수명을
 * 유지하기 위해 노출된 `MutableState`에 위임한다: `var showIconSheet by overlayUi.showIconSheetState`.
 *
 * 주의: `localCustomPresets`는 `remember(customPresets)` 키를 가지므로 본 홀더에 넣지 않고
 * 화면이 소유한다. `localCustomShortcutPresets`/`localCustomMacroPresets`는 키 없는 일반 상태라
 * 본 홀더가 소유하되, 로더(`LaunchedEffect`)는 화면에 남아 위임 var로 값을 채운다.
 */
internal class EdgeZoneOverlayUiState {
    // ── 아이콘/컬러/라벨 서랍·피커·키보드 표시 ──
    val showIconSheetState: MutableState<Boolean> = mutableStateOf(false)
    val showColorPickerState: MutableState<Boolean> = mutableStateOf(false)
    val showCandidateIconSheetState: MutableState<Boolean> = mutableStateOf(false)
    val showCandidateColorPickerState: MutableState<Boolean> = mutableStateOf(false)
    val showLabelKeyboardState: MutableState<Boolean> = mutableStateOf(false)
    val candidateLabelKeyboardState: MutableState<((String) -> Unit)?> = mutableStateOf(null)
    val iconDrawerStageState: MutableState<IconDrawerStage> = mutableStateOf(IconDrawerStage.Category)
    val colorPickerStageState: MutableState<com.bridgeone.app.ui.components.colorpicker.ColorPickerStage> =
        mutableStateOf(com.bridgeone.app.ui.components.colorpicker.ColorPickerStage.Category)
    val colorCommitCandidateState: MutableState<String?> = mutableStateOf(null)

    // ── 앵커 좌표 (in window) ──
    val iconBoxCenterInWindowState: MutableState<Offset> = mutableStateOf(Offset.Zero)
    val colorBoxCenterInWindowState: MutableState<Offset> = mutableStateOf(Offset.Zero)
    val candidateIconBoxCenterInWindowState: MutableState<Offset> = mutableStateOf(Offset.Zero)
    val candidateColorBoxCenterInWindowState: MutableState<Offset> = mutableStateOf(Offset.Zero)
    val ratioBtnBoundsInWindowState: MutableState<Rect> = mutableStateOf(Rect.Zero)
    val actionTypeBtnBoundsInWindowState: MutableState<Rect> = mutableStateOf(Rect.Zero)
    val revertBtnBoundsInWindowState: MutableState<Rect> = mutableStateOf(Rect.Zero)

    // ── 프리셋 팝업 / Undo 메뉴 ──
    val showPresetPopupState: MutableState<Boolean> = mutableStateOf(false)
    val presetPopupStageState: MutableState<PopupStage> = mutableStateOf(PopupStage.GRID)
    val presetNameKeyboardState: MutableState<((String) -> Unit)?> = mutableStateOf(null)
    val presetNameSeedState: MutableState<String> = mutableStateOf("")
    val showUndoMenuState: MutableState<Boolean> = mutableStateOf(false)
    val undoMenuAnchorBottomState: MutableState<Int> = mutableStateOf(0)

    // ── 커스텀 단축키 팝업 ──
    val swipeShortcutVisibleState: MutableState<Boolean> = mutableStateOf(false)
    val swipeShortcutDraftState: MutableState<EdgeZoneAction.SendShortcut> =
        mutableStateOf(EdgeZoneAction.SendShortcut(0))
    val swipeShortcutOnConfirmState: MutableState<(EdgeZoneAction.SendShortcut) -> Unit> =
        mutableStateOf({})
    val swipeShortcutOnAddAsCandidateState: MutableState<((EdgeZoneAction.SendShortcut, String, String) -> Unit)?> =
        mutableStateOf(null)
    val shortcutNameKbActiveState: MutableState<Boolean> = mutableStateOf(false)

    // ── 커스텀 매크로 편집기 ──
    val macroEditorVisibleState: MutableState<Boolean> = mutableStateOf(false)
    val macroEditorDraftState: MutableState<EdgeZoneAction.SendMacro> =
        mutableStateOf(EdgeZoneAction.SendMacro())
    val macroEditorOnConfirmState: MutableState<(EdgeZoneAction.SendMacro, String, String) -> Unit> =
        mutableStateOf({ _, _, _ -> })
    val macroEditorOnAddAsPresetState: MutableState<((EdgeZoneAction.SendMacro, String, String) -> Unit)?> =
        mutableStateOf(null)
    val macroEditorInitialIconKeyState: MutableState<String> = mutableStateOf("")
    val macroEditorInitialNameState: MutableState<String> = mutableStateOf("")
    val macroNameKbActiveState: MutableState<Boolean> = mutableStateOf(false)

    // ── NORMAL 미니버튼 롱프레스 툴팁 ──
    val normalMiniTooltipTextState: MutableState<String> = mutableStateOf("")
    val normalMiniTooltipAnchorState: MutableState<Rect> = mutableStateOf(Rect.Zero)

    // ── 단축키/매크로 팝업 아이콘 선택 서랍 (SWIPE 전용) ──
    val shortcutIconSheetVisibleState: MutableState<Boolean> = mutableStateOf(false)
    val shortcutIconSeedState: MutableState<String> = mutableStateOf("")
    val shortcutIconOnPickState: MutableState<((String) -> Unit)?> = mutableStateOf(null)
    val shortcutIconAnchorState: MutableState<Offset> = mutableStateOf(Offset.Zero)
    val iconPickerReturnElementState: MutableState<EdgeEditorElement> =
        mutableStateOf(EdgeEditorElement.ShortcutIconButton)

    // ── 액션 순환 후보 편집 draft ──
    val rotationDraftState: MutableState<RotationCandidate> =
        mutableStateOf(RotationCandidate(EdgeZoneAction.Unassigned, "", ""))

    // ── 포커스 예약 / 커스텀 프리셋 수정·삭제 메뉴 ──
    val nextFocusOnZoneChangeState: MutableState<EdgeEditorElement?> = mutableStateOf(null)
    val swipeCustomMenuTargetState: MutableState<CustomPresetTarget?> = mutableStateOf(null)

    // ── 커스텀 단축키/매크로 프리셋 목록 (로더는 화면에 잔류, 본 홀더가 값 보관) ──
    val localCustomShortcutPresetsState: MutableState<List<com.bridgeone.app.ui.common.CustomShortcutPreset>> =
        mutableStateOf(emptyList())
    val localCustomMacroPresetsState: MutableState<List<com.bridgeone.app.ui.common.CustomMacroPreset>> =
        mutableStateOf(emptyList())
}
