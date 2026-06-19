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
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bridgeone.app.ui.common.EdgeSwipeConstants
import com.bridgeone.app.ui.common.InputMode
import com.bridgeone.app.ui.common.ToastController
import com.bridgeone.app.ui.common.ToastType
import com.bridgeone.app.ui.common.swipe.LocalSwipeFocusController
import com.bridgeone.app.ui.common.swipe.LocalSwipeFocused
import com.bridgeone.app.ui.common.swipe.SwipeFocusable
import com.bridgeone.app.ui.common.swipe.SwipeMode
import com.bridgeone.app.ui.common.MACRO_STEP_LIST_RESERVED_DP
import com.bridgeone.app.ui.common.MACRO_DIALOG_MAX_SCREEN_FRACTION
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
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

/**
 * 매크로(SendMacro) 편집 팝업 + 딜레이 슬라이더/스텝 라벨 헬퍼. EdgeZoneEditorScreen에서 분리 (Phase 4.7.5-B).
 */
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
internal fun macroButtonLabel(element: EdgeEditorElement): String? = when (element) {
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
internal fun MacroEditorPopup(
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
