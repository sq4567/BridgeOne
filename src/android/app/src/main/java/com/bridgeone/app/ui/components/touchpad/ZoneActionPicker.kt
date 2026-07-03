package com.bridgeone.app.ui.components.touchpad

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bridgeone.app.ui.common.CustomPointerDynamicsPreset
import com.bridgeone.app.ui.common.DYNAMICS_PRESETS
import com.bridgeone.app.ui.common.EdgeSwipeConstants
import com.bridgeone.app.ui.common.InputMode
import com.bridgeone.app.ui.common.MODE_PRESETS
import com.bridgeone.app.ui.common.ToastController
import com.bridgeone.app.ui.common.ToastType
import com.bridgeone.app.ui.common.customPresetIconOrNull
import com.bridgeone.app.ui.common.swipe.LocalSwipeFocusController
import com.bridgeone.app.ui.common.swipe.LocalSwipeFocused
import com.bridgeone.app.ui.common.swipe.SwipeFocusable
import com.bridgeone.app.ui.common.swipe.SwipeMode
import com.bridgeone.app.ui.components.DEFAULT_SHORTCUTS
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.roundToInt

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
    DomainGroup("MODE_NAV", "모드·탐색", "프리셋·되돌리기·페이지·멀티 커서", Icons.Filled.Tune,
        listOf(ActionDomain.MODE_PRESET, ActionDomain.HISTORY, ActionDomain.PAGE, ActionDomain.MULTI_CURSOR)),
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
        DomainInfo(ActionDomain.MULTI_CURSOR, "멀티 커서", "켜기·패드 전환·커서 수", Icons.Filled.Group,
            EdgeZoneAction.ToggleMultiCursor, "토글", "멀티 커서 켜고 끄기",
            buildList {
                add(ActionOption("레이아웃 전환", "그리드↔직접 버튼", Icons.Filled.Autorenew,
                    EdgeZoneAction.ToggleMultiCursorLayout))
                for (count in MULTI_CURSOR_COUNT_MIN..MULTI_CURSOR_COUNT_MAX) {
                    add(ActionOption("커서 ${count}개", "커서 수 ${count}로 설정", Icons.Filled.Tune,
                        EdgeZoneAction.SetCursorCount(count)))
                }
                add(ActionOption("다음 패드", "다음 커서로 전환", Icons.AutoMirrored.Filled.ArrowForward,
                    EdgeZoneAction.CyclePad(PageNav.NEXT)))
                add(ActionOption("이전 패드", "이전 커서로 전환", Icons.AutoMirrored.Filled.ArrowBack,
                    EdgeZoneAction.CyclePad(PageNav.PREV)))
                for (i in 0 until MULTI_CURSOR_COUNT_MAX) {
                    add(ActionOption("패드 ${i + 1}", "${i + 1}번 커서 활성화", Icons.Filled.Adjust,
                        EdgeZoneAction.ActivatePad(i)))
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
                        com.bridgeone.app.ui.common.CustomTrackSlider(
                            value = displayMultiplier,
                            onValueChange = displayOnChange,
                            valueRange = 0.1f..5.0f,
                            valueLabel = "×${"%.1f".format(displayMultiplier)}",
                            labelWidth = 40.dp,
                            snap = { (it * 10f).roundToInt() / 10f },
                            element = EdgeEditorElement.CustomMultiplierSlider,
                            gridRow = 35,
                            majorTickStep = 1.0f,
                            minorTickStep = 0.5f,
                        )
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
