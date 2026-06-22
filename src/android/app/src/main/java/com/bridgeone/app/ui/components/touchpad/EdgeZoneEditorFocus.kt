package com.bridgeone.app.ui.components.touchpad

import com.bridgeone.app.ui.common.IconCategoryTab
import com.bridgeone.app.ui.common.swipe.FocusableElement

/**
 * EdgeZoneEditorScreen의 SWIPE 모드 포커스 대상 식별자.
 *
 * 데이터 클래스 인스턴스의 equals/hashCode가 controller의 entries 키로 사용되므로
 * 같은 의미의 요소는 같은 식별자 인스턴스를 사용해야 한다.
 *
 * 행/그래프 정보는 좌표 기반 nearest-neighbor traversal로 자동 결정되므로
 * 여기서는 식별자만 정의한다.
 */
sealed class EdgeEditorElement : FocusableElement {

    // ── TopAppBar ──
    object Back : EdgeEditorElement()
    object PresetBadge : EdgeEditorElement()
    object Undo : EdgeEditorElement()

    // ── 미리보기 캔버스 존별 hit (선택 존이 없을 때) ──
    // 비활성 엣지만 등록 생략 (Unassigned 존 포함).
    data class CanvasZone(val edge: EntryEdge, val index: Int) : EdgeEditorElement()

    // ── 존 스트립 (선택 존이 있을 때) ──
    data class StripZone(val index: Int) : EdgeEditorElement()
    data class StripBoundary(val index: Int) : EdgeEditorElement()
    object RatioPresetMenu : EdgeEditorElement()

    // ── 액션 편집 패널 ──
    object ActionTypeToggle : EdgeEditorElement()

    // 단일 액션 분기
    data class DomainChip(val domainKey: String) : EdgeEditorElement()
    data class ActionOptionCard(val key: String) : EdgeEditorElement()
    /** 폴더 계층 탐색 — 폴더 노드 카드 (nodeKey는 "GROUP:xxx" 또는 "DOMAIN:xxx"). */
    data class ActionFolderCard(val nodeKey: String) : EdgeEditorElement()
    /** 폴더 계층 탐색 — 브레드크럼 세그먼트 (depth=0: 루트, depth=n: 현재 폴더). */
    data class BreadcrumbSegment(val depth: Int) : EdgeEditorElement()
    object CustomMultiplierSlider : EdgeEditorElement()
    object IconBox : EdgeEditorElement()
    object ColorBox : EdgeEditorElement()
    object LabelBox : EdgeEditorElement()
    object RevertToAuto : EdgeEditorElement()

    // 로테이션 분기
    data class RotationCandidate(val index: Int) : EdgeEditorElement()
    data class RotationCandidateUp(val index: Int) : EdgeEditorElement()
    data class RotationCandidateDown(val index: Int) : EdgeEditorElement()
    data class RotationCandidateDelete(val index: Int) : EdgeEditorElement()
    data class RotationCandidateEdit(val index: Int) : EdgeEditorElement()
    object RotationIntervalSlider : EdgeEditorElement()
    data class RotationIntervalPreset(val ms: Int) : EdgeEditorElement()
    object RotationIntervalCustom : EdgeEditorElement()
    object RotationAddCandidate : EdgeEditorElement()
    object RotationCancelEdit : EdgeEditorElement()
    object RotationApplyEdit : EdgeEditorElement()
    object RotationCandidateIconBox : EdgeEditorElement()
    object RotationCandidateColorBox : EdgeEditorElement()
    object RotationCandidateLabelBox : EdgeEditorElement()
    object RotationCandidateRevertToAuto : EdgeEditorElement()

    // ── 하단 버튼 바 ──
    object Save : EdgeEditorElement()

    // ── 메뉴/팝업 항목 (각자 scope에서 사용) ──
    data class UndoHistoryItem(val index: Int) : EdgeEditorElement()
    data class PresetItem(val id: String) : EdgeEditorElement()

    // ZoneActionPopup
    object ZoneActionMerge : EdgeEditorElement()
    object ZoneActionSplit : EdgeEditorElement()
    object ZoneActionMove : EdgeEditorElement()           // Initial 팝업의 "이동" 버튼
    object ZoneActionMoveModeToggle : EdgeEditorElement()  // 폭째/액션만 토글
    object ZoneActionMoveLeft : EdgeEditorElement()
    object ZoneActionMoveRight : EdgeEditorElement()
    object ZoneActionMoveConfirm : EdgeEditorElement()
    object ZoneActionDelete : EdgeEditorElement()
    object ZoneActionMergeCancel : EdgeEditorElement()
    object ZoneActionMergeConfirm : EdgeEditorElement()
    object ZoneActionMergeLeft : EdgeEditorElement()
    object ZoneActionMergeRight : EdgeEditorElement()
    data class ZoneActionSplitN(val n: Int) : EdgeEditorElement()
    object ZoneActionSplitCancel : EdgeEditorElement()
    object ZoneActionDeleteYes : EdgeEditorElement()
    object ZoneActionDeleteNo : EdgeEditorElement()

    // Dialog 버튼들
    object DiscardDialogSave : EdgeEditorElement()
    object DiscardDialogDiscard : EdgeEditorElement()
    object DiscardDialogCancel : EdgeEditorElement()

    // 커스텀 단축키 팝업
    data class ShortcutModifier(val bit: Int, val secondary: Boolean = false) : EdgeEditorElement()
    data class ShortcutKey(val code: Int) : EdgeEditorElement()
    object ShortcutPopupCancel : EdgeEditorElement()
    object ShortcutPopupConfirm : EdgeEditorElement()
    object ShortcutPopupAddCandidate : EdgeEditorElement()
    object ShortcutNameField : EdgeEditorElement()
    object ShortcutIconButton : EdgeEditorElement()
    object ShortcutModeSingleKey : EdgeEditorElement()
    object ShortcutModeCombo : EdgeEditorElement()

    // 매크로 편집기 팝업
    data class MacroStepChip(val index: Int) : EdgeEditorElement()
    data class MacroStepUp(val index: Int) : EdgeEditorElement()
    data class MacroStepDown(val index: Int) : EdgeEditorElement()
    data class MacroStepDelete(val index: Int) : EdgeEditorElement()
    data class MacroStepDelayExpand(val index: Int) : EdgeEditorElement()
    data class MacroStepDuplicate(val index: Int) : EdgeEditorElement()
    data class MacroStepSplitMerge(val index: Int) : EdgeEditorElement()
    object MacroAddStep : EdgeEditorElement()
    object MacroDelaySlider : EdgeEditorElement()
    object MacroPopupCancel : EdgeEditorElement()
    object MacroPopupConfirm : EdgeEditorElement()
    object MacroPopupAddPreset : EdgeEditorElement()
    // 스텝 추가 모드 선택 (단일 키 / 단축키 / 문자열 / 단축키 피커)
    object MacroStepModeSingleKey : EdgeEditorElement()
    object MacroStepModeCombo : EdgeEditorElement()
    object MacroStepModeText : EdgeEditorElement()
    object MacroAddFromShortcut : EdgeEditorElement()
    object MacroTextField : EdgeEditorElement()
    object MacroTextGenerate : EdgeEditorElement()
    object MacroStepApply : EdgeEditorElement()      // 스텝 저장 (MacroPopupConfirm과 분리)
    object MacroStepSaveContinue : EdgeEditorElement() // 스텝 저장 후 계속 추가
    // MacroStepDelayToggle은 미사용 (⏱ 버튼이 역할 대체)
    data class MacroStepDelaySlider(val index: Int) : EdgeEditorElement()
    data class MacroStepDragHandle(val index: Int) : EdgeEditorElement()
    // 반복 횟수 스테퍼 (입력 상태)
    object MacroStepRepeatMinus : EdgeEditorElement()
    object MacroStepRepeatPlus : EdgeEditorElement()
    // 단축키 피커 칩 (PICK 모드)
    data class MacroShortcutPick(val index: Int) : EdgeEditorElement()
    // 전체 키 강제 해제 스텝 추가 체크박스 (FINALIZE 페이지)
    object MacroForceReleaseToggle : EdgeEditorElement()
    // 아이콘 / 이름 / 입력 모드 설정 행
    object MacroIconButton : EdgeEditorElement()
    object MacroNameField : EdgeEditorElement()
    object MacroImeCheckNone : EdgeEditorElement()
    object MacroImeCheckKorean : EdgeEditorElement()
    object MacroImeCheckEnglish : EdgeEditorElement()
    // 위저드 페이지 전환 (스텝 구성 ↔ 저장 설정)
    object MacroGoFinalize : EdgeEditorElement()     // 스텝 구성 → 저장 설정 (다음)
    object MacroGoBackToSteps : EdgeEditorElement()  // 저장 설정 → 스텝 구성 (뒤로)

    // 매크로 스텝 폴더 그룹화
    data class MacroGroupHeader(val groupId: Int) : EdgeEditorElement()   // 접기/펼치기
    data class MacroGroupUp(val groupId: Int) : EdgeEditorElement()
    data class MacroGroupDown(val groupId: Int) : EdgeEditorElement()
    data class MacroGroupDuplicate(val groupId: Int) : EdgeEditorElement()
    data class MacroGroupDelete(val groupId: Int) : EdgeEditorElement()
    data class MacroGroupUngroup(val groupId: Int) : EdgeEditorElement()
    data class MacroGroupDragHandle(val groupId: Int) : EdgeEditorElement()
    data class MacroGroupRename(val groupId: Int) : EdgeEditorElement()  // 폴더명 편집 필드
    object MacroStepGroupSelectToggle : EdgeEditorElement()   // 선택 모드 on/off
    data class MacroStepSelectCheck(val index: Int) : EdgeEditorElement()   // 선택 체크
    object MacroStepGroupConfirm : EdgeEditorElement()        // 묶기 확정

    // 비율 프리셋 항목 (ROOT scope의 DropdownMenu)
    data class RatioPresetItem(val label: String) : EdgeEditorElement()

    // IconSheet 카테고리 항목 (서랍 1단계)
    data class IconCategoryItem(val tab: IconCategoryTab) : EdgeEditorElement()

    // IconSheet 아이콘 항목 (서랍 2단계)
    data class IconSheetItem(val key: String) : EdgeEditorElement()

    // 커스텀 프리셋 수정/삭제 메뉴
    object CustomMenuEdit : EdgeEditorElement()
    object CustomMenuDelete : EdgeEditorElement()
    object CustomMenuDeleteConfirm : EdgeEditorElement()
    object CustomMenuDeleteCancel : EdgeEditorElement()

    // PresetPopup 추가 식별자
    object PresetSaveNew : EdgeEditorElement()
    object PresetConfirmBack : EdgeEditorElement()
    object PresetConfirmApply : EdgeEditorElement()
    object PresetConfirmRename : EdgeEditorElement()
    object PresetConfirmDelete : EdgeEditorElement()
    object PresetConfirmCancel : EdgeEditorElement()
    object PresetSaveConfirm : EdgeEditorElement()
    object PresetSaveCancel : EdgeEditorElement()
    object PresetEditConfirm : EdgeEditorElement()
    object PresetEditCancel : EdgeEditorElement()
    object PresetOverwriteConfirm : EdgeEditorElement()
    object PresetOverwriteCancel : EdgeEditorElement()
}

/**
 * EdgeZoneEditorScreen의 SWIPE 모드 scope 식별자.
 *
 * 메뉴/팝업이 열려 있는 동안 controller가 해당 scope로 전환하여
 * 포커스 가능한 요소를 scope 내부로 한정한다.
 */
sealed class EdgeEditorScope {
    object Root : EdgeEditorScope()
    object UndoMenu : EdgeEditorScope()
    object PresetPopup : EdgeEditorScope()
    object RatioPresetMenu : EdgeEditorScope()
    object IconSheet : EdgeEditorScope()
    object ColorPicker : EdgeEditorScope()
    /** SwipeKeyboardOverlay에 위임된 상태 (hex 색상 입력). controller는 이 scope에서 휴면. */
    object ColorKeyboard : EdgeEditorScope()
    object ZoneActionPopup : EdgeEditorScope()
    object DiscardDialog : EdgeEditorScope()
    /** SwipeKeyboardOverlay에 위임된 상태. controller는 이 scope에서 휴면. */
    object LabelKeyboard : EdgeEditorScope()
    /** 커스텀 다이나믹스 프리셋 편집기가 풀스크린으로 표시 중. 에디터 내부가 자체 포커스를 관리. */
    object DynamicsEditor : EdgeEditorScope()
    /** 커스텀 단축키 팝업. controller는 이 scope 안에서 키/버튼만 포커스. */
    object ShortcutPopup : EdgeEditorScope()
    /** 매크로 편집기 팝업. controller는 스텝 칩/키보드/버튼만 포커스. */
    object MacroPopup : EdgeEditorScope()
    /** 커스텀 프리셋 수정/삭제 메뉴. controller는 메뉴 버튼만 포커스. */
    object CustomPresetMenu : EdgeEditorScope()
}
