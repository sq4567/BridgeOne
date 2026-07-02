package com.bridgeone.app.ui.components.touchpad

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.bridgeone.app.ui.components.SwipeKeyboardOverlay
import com.bridgeone.app.ui.components.colorpicker.ColorPickerStage
import com.bridgeone.app.ui.components.colorpicker.ColorPickerSwipe
import com.bridgeone.app.ui.common.swipe.LocalSwipeFocusController
import com.bridgeone.app.ui.common.swipe.ROOT_SCOPE
import com.bridgeone.app.ui.common.swipe.SwipeGestureLayer
import com.bridgeone.app.ui.common.swipe.rememberSwipeFocusController
import com.bridgeone.app.ui.common.CUSTOM_PRESET_TEMPLATES
import com.bridgeone.app.ui.common.CurveEditorConstants
import com.bridgeone.app.ui.common.CustomPointerDynamicsPreset
import com.bridgeone.app.ui.common.addNodeAfter
import com.bridgeone.app.ui.common.defaultAccelerationCurve
import com.bridgeone.app.ui.common.defaultDecelerationCurve
import com.bridgeone.app.ui.common.deleteNodeAt
import com.bridgeone.app.ui.common.describeCurves
import com.bridgeone.app.ui.common.stepNodeX
import com.bridgeone.app.ui.common.stepNodeY
import java.util.UUID
import kotlin.math.roundToInt
import kotlin.math.sqrt

private const val NAME_MAX_LEN = 12
private const val DESC_MAX_LEN = 50
internal const val FIELD_NAME = "name"
internal const val FIELD_DESC = "desc"


// ─────────────────────────────────────────────────────────────
// 화면 상태 (Phase 4.5.18.4: Boolean 3개 → sealed class 1개)
// ─────────────────────────────────────────────────────────────

private sealed class EditorScreen {
    object Graph : EditorScreen()
    data class Keyboard(val target: String) : EditorScreen() // "name" | "desc"
    object IconPicker : EditorScreen()
    object TemplatePicker : EditorScreen()
    object ColorPicker : EditorScreen()
}

// ─────────────────────────────────────────────────────────────
// 그리드 컨텍스트 (Phase 4.5.18.7: Main / NodeEdit.Select / NodeEdit.Manipulate)
// ─────────────────────────────────────────────────────────────

private sealed class GridContext {
    object Main : GridContext()
    sealed class NodeEdit : GridContext() {
        object Select : NodeEdit()      // 그래프 위 좌우 스와이프로 노드 선택
        object Manipulate : NodeEdit()  // X±/Y±/CRUD 십자 패드 조작
    }
    object SaveConfirm : GridContext()  // 저장 전 확인 오버레이
}

// ── 메인 그리드 슬롯 배치
// Row 0 (Header): 7=취소, 8=저장
// Row 1 (MetaCard 상단): 0=아이콘, 10=색상, 1=이름, 3=템플릿
// Row 2 (MetaCard 하단): 2=설명, 3=템플릿
// Row 3 (노드 편집, 단독): 9=노드 편집
// Row 4 (CurveCard, 기준 행): 4=가속, 6=→복사, 5=감속
private val ACTION_ROW_SLOTS = listOf(
    listOf(7, 8),         // Row 0: Header (취소, 저장)
    listOf(0, 10, 1, 3),  // Row 1: MetaCard 상단 (아이콘, 색상, 이름, 템플릿)
    listOf(2, 3),         // Row 2: MetaCard 하단
    listOf(9),            // Row 3: 노드 편집 (단독)
    listOf(4, 6, 5),      // Row 4: CurveCard (기준 행)
)
private const val MAIN_START_ROW = 4  // CurveCard = 기준 행

// ── 노드 편집 그리드 슬롯 배치 (Phase 4.5.18.9: 탭 토글 그래프 라벨로 이동)
// Row 0 (헤더): 0=← 뒤로
// Row 1: 9=탭 전환 (ActiveTabLabel hover 트리거)
// Row 2: 11=노드 선택 재진입
// Row 3: 6=Y+ (패드 상단)
// Row 4 (기준 행): 3=X−, 10=스텝 정밀도, 4=X+ (패드 좌우·가운데)
// Row 5: 5=Y− (패드 하단)
// Row 6: 7=추가, 8=삭제 (CRUD)
private val NODE_EDIT_MANIPULATE_ROW_SLOTS = listOf(
    listOf(0),         // Row 0: 헤더 — 뒤로만
    listOf(9),         // Row 1: 탭 전환 (ActiveTabLabel hover)
    listOf(11),        // Row 2: 노드 선택 (선택 모드 재진입)
    listOf(6),         // Row 3: Y+ (패드 상단)
    listOf(3, 10, 4),  // Row 4 (기준 행): X− / 스텝 정밀도(중앙) / X+
    listOf(5),         // Row 5: Y− (패드 하단)
    listOf(7, 8)       // Row 6: 추가 / 삭제
)
private const val NODE_EDIT_START_ROW = 4  // X−/X+ 행 = 기준 (십자 패드 정중앙)
// 선택 모드(Select): 헤더 슬롯만 — 그리드 본문은 그래프에서 직접 처리
private val NODE_EDIT_SELECT_ROW_SLOTS = listOf(
    listOf(0)          // Row 0: 헤더만 (탭 토글은 그래프 내부 ActiveTabLabel)
)

// ── 저장 확인 오버레이 슬롯 배치
// Row 0: 0=편집 계속, 1=이대로 저장
internal val SAVE_CONFIRM_ROW_SLOTS = listOf(
    listOf(0, 1)
)
internal const val SAVE_CONFIRM_START_ROW = 0

/**
 * 가변 슬롯 매핑. 드래그 시작점 기준 상대 이동량(dragDelta)으로 슬롯 결정.
 * 화면 절대 좌표와 무관하므로 스와이프 시작 위치에 관계없이 동일하게 동작.
 * @param dragDelta  fingerPos - startPos (드래그 시작 기준 상대 이동량)
 * @param rowSlots   ACTION_ROW_SLOTS 또는 NODE_EDIT_ROW_SLOTS
 * @param startRow   기준 행 인덱스 (MAIN_START_ROW 또는 NODE_EDIT_START_ROW)
 */
internal fun resolveSlot(
    dragDelta: Offset,
    stepPx: Float,
    rowSlots: List<List<Int>>,
    startRow: Int,
    startCol: Int? = null
): Int {
    val sourceCols = rowSlots[startRow].size
    val resolvedStartCol = startCol ?: (sourceCols - 1) / 2
    val row = (startRow + (dragDelta.y / stepPx).roundToInt()).coerceIn(0, rowSlots.lastIndex)
    val targetCols = rowSlots[row].size
    val mappedStartCol = when {
        row == startRow || startCol == null -> resolvedStartCol
        sourceCols <= 1 -> (targetCols - 1) / 2
        targetCols > sourceCols -> {
            // 좁은 행 → 넓은 행: 오른쪽 편향 (+0.5 후 반올림)
            ((resolvedStartCol.toFloat() / (sourceCols - 1)) * (targetCols - 1) + 0.5f)
                .roundToInt().coerceIn(0, targetCols - 1)
        }
        else -> {
            // 넓은 행 → 좁은 행: 왼쪽 편향 (내림)
            ((resolvedStartCol.toFloat() / (sourceCols - 1)) * (targetCols - 1))
                .toInt().coerceIn(0, targetCols - 1)
        }
    }
    val col = (mappedStartCol + (dragDelta.x / stepPx).roundToInt()).coerceIn(0, targetCols - 1)
    return rowSlots[row][col]
}

/**
 * 커스텀 포인터 다이나믹스 프리셋 그래프 편집기
 *
 * 전체 화면 오버레이 Composable. null preset = 신규 생성, non-null = 편집.
 *
 * @param initialPreset   편집 대상 프리셋 (null이면 신규 생성)
 * @param onSave          저장 콜백 (완성된 프리셋 전달)
 * @param onDismiss       취소 콜백
 */
@Composable
fun DynamicsCurveEditor(
    initialPreset: CustomPointerDynamicsPreset?,
    existingPresets: List<CustomPointerDynamicsPreset> = emptyList(),
    onSave: (CustomPointerDynamicsPreset) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current

    var name by remember {
        mutableStateOf(initialPreset?.name ?: run {
            val existing = existingPresets.map { it.name }.toSet()
            var i = 1
            while ("내 설정 $i" in existing) i++
            "내 설정 $i"
        })
    }
    var description by remember { mutableStateOf(initialPreset?.description ?: "") }
    var accelCurve by remember {
        mutableStateOf(initialPreset?.accelerationCurve ?: defaultAccelerationCurve())
    }
    var decelCurve by remember {
        mutableStateOf(initialPreset?.decelerationCurve ?: defaultDecelerationCurve())
    }
    var activeTab by remember { mutableIntStateOf(0) } // 0=가속, 1=감속
    var selectedIconKey by remember { mutableStateOf(initialPreset?.iconKey ?: "") }
    var selectedColorHex by remember { mutableStateOf(initialPreset?.colorHex ?: "") }

    var currentScreen by remember { mutableStateOf<EditorScreen>(EditorScreen.Graph) }

    // 아이콘 피커: 카테고리 2단계 애플워치 서랍 (자체 SwipeFocusController + 제스처 레이어)
    val iconPickerController = rememberSwipeFocusController()
    // 빠른 스와이프 시 한 이벤트당 1칸만 이동 (셀렉션 링 그리드 이탈 방지)
    LaunchedEffect(iconPickerController) { iconPickerController.maxFocusStepsPerEvent = 1 }
    var iconPickerStage by remember { mutableStateOf<IconDrawerStage>(IconDrawerStage.Category) }
    var iconPickerAnchor by remember { mutableStateOf(Offset.Zero) }
    LaunchedEffect(currentScreen) {
        if (currentScreen is EditorScreen.IconPicker) iconPickerStage = IconDrawerStage.Category
    }

    val colorPickerController = rememberSwipeFocusController()
    LaunchedEffect(colorPickerController) { colorPickerController.maxFocusStepsPerEvent = 1 }
    var colorPickerStage by remember { mutableStateOf<ColorPickerStage>(ColorPickerStage.Category) }
    var colorPickerAnchor by remember { mutableStateOf(Offset.Zero) }
    // 롱프레스 색 확정 후보 hex. null이면 확정 대상 없음(ExpandToggle 등). 기본값: null
    var colorCommitCandidate by remember { mutableStateOf<String?>(null) }

    var hoveredSlot by remember { mutableIntStateOf(0) }
    var awaitingConfirm by remember { mutableStateOf(true) }

    // 아이콘 피커 뒤로가기: 아이콘 단계 → 카테고리 단계, 카테고리 단계 → 그래프 복귀
    BackHandler(enabled = currentScreen is EditorScreen.IconPicker) {
        if (iconPickerStage is IconDrawerStage.Icons) {
            iconPickerStage = IconDrawerStage.Category
        } else {
            hoveredSlot = 0; awaitingConfirm = true
            currentScreen = EditorScreen.Graph
        }
    }
    BackHandler(enabled = currentScreen is EditorScreen.ColorPicker) {
        when (val s = colorPickerStage) {
            is ColorPickerStage.DirectInput -> colorPickerStage = if (s.sourceTab != null)
                ColorPickerStage.Swatches(s.sourceTab) else ColorPickerStage.Category
            is ColorPickerStage.Swatches    -> colorPickerStage = ColorPickerStage.Category
            is ColorPickerStage.Category    -> {
                hoveredSlot = 10; awaitingConfirm = true
                currentScreen = EditorScreen.Graph
            }
        }
    }
    var gridContext by remember { mutableStateOf<GridContext>(GridContext.Main) }
    var selectedNodeIndex by remember { mutableIntStateOf(0) }
    var nodeStepIndex by remember { mutableIntStateOf(CurveEditorConstants.NODE_STEP_DEFAULT_INDEX) }
    var stepPickerVisible by remember { mutableStateOf(false) }
    val presetId = remember { initialPreset?.id ?: UUID.randomUUID().toString() }
    val nodeStepScale = CurveEditorConstants.NODE_STEP_SCALES[nodeStepIndex]
    var tabLabelHovered by remember { mutableStateOf(false) }
    // 키보드 히트영역 확장 시, 그래프Box 상단이 새 컨테이너 기준 몇 px 아래인지 기록 (키보드 시각적 위치 고정용)
    var graphBoxTopOffsetPx by remember { mutableFloatStateOf(0f) }

    // 서브메뉴 재진입 시 위치 유지
    var savedTemplateIndex by remember { mutableIntStateOf(0) }

    val isDuplicate = name.isNotBlank() &&
            existingPresets.any { it.name == name && it.id != (initialPreset?.id ?: "") }
    val nameValid = name.isNotBlank() && !isDuplicate

    // 탭 전환 시 선택 노드 초기화
    LaunchedEffect(activeTab) { selectedNodeIndex = 0 }

    // 슬롯 인덱스: 0=아이콘, 1=이름, 2=설명, 3=템플릿
    //              4=가속, 5=감속, 6=→감속복사
    //              7=취소, 8=저장, 9=노드 편집, 10=색상
    val actionSlots = listOf(
        ActionSlot("아이콘", enabled = true,
            iconKey = selectedIconKey, style = SlotStyle.NORMAL),
        ActionSlot("이름", enabled = true,
            previewText = name, style = SlotStyle.NORMAL),
        ActionSlot("설명", enabled = true,
            previewText = description, style = SlotStyle.NORMAL),
        ActionSlot("템플릿", enabled = true, style = SlotStyle.NORMAL),
        ActionSlot("가속", enabled = activeTab != 0, isCurrent = activeTab == 0,
            style = SlotStyle.SEGMENT_LEFT),
        ActionSlot("감속", enabled = activeTab != 1, isCurrent = activeTab == 1,
            style = SlotStyle.SEGMENT_RIGHT),
        ActionSlot("→감속\n복사", enabled = activeTab == 0, style = SlotStyle.NORMAL),
        ActionSlot("취소", enabled = true, style = SlotStyle.SECONDARY),
        ActionSlot("저장", enabled = nameValid, style = SlotStyle.PRIMARY),
        ActionSlot("노드 편집", enabled = true, style = SlotStyle.NORMAL),
        ActionSlot("색상", enabled = true, previewText = selectedColorHex, style = SlotStyle.NORMAL),
    )

    // 노드 편집 슬롯 활성 여부 (dry-run 방식)
    fun isNodeEditSlotEnabled(slot: Int): Boolean {
        val curve = if (activeTab == 0) accelCurve else decelCurve
        val i = selectedNodeIndex.coerceIn(0, curve.lastIndex)
        return when (slot) {
            0 -> true  // ← 뒤로
            1 -> i > 0
            2 -> i < curve.lastIndex
            3 -> stepNodeX(curve, i, -CurveEditorConstants.CURVE_STEP_VELOCITY * nodeStepScale) != null
            4 -> stepNodeX(curve, i, +CurveEditorConstants.CURVE_STEP_VELOCITY * nodeStepScale) != null
            5 -> stepNodeY(curve, i, -CurveEditorConstants.CURVE_STEP_MULTIPLIER * nodeStepScale) != null
            6 -> stepNodeY(curve, i, +CurveEditorConstants.CURVE_STEP_MULTIPLIER * nodeStepScale) != null
            7 -> if (i == curve.lastIndex) addNodeAfter(curve, (i - 1).coerceAtLeast(0)) != null
                 else addNodeAfter(curve, i) != null
            8 -> deleteNodeAt(curve, i) != null
            9 -> true   // 가속↔감속
            10 -> true  // 스텝 정밀도 피커
            11 -> true  // 노드 선택 모드로 복귀
            else -> false
        }
    }

    fun hapticConfirm() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }

    fun hapticReject() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }

    fun executeNodeEditSlot(slot: Int) {
        val curve = if (activeTab == 0) accelCurve else decelCurve
        val i = selectedNodeIndex.coerceIn(0, curve.lastIndex)
        when (slot) {
            0 -> { // ← 뒤로
                gridContext = GridContext.Main
                hoveredSlot = 9
                awaitingConfirm = true
            }
            1 -> selectedNodeIndex = (i - 1).coerceAtLeast(0)
            2 -> selectedNodeIndex = (i + 1).coerceAtMost(curve.lastIndex)
            3 -> { val r = stepNodeX(curve, i, -CurveEditorConstants.CURVE_STEP_VELOCITY * nodeStepScale)
                   if (r != null) { if (activeTab == 0) accelCurve = r else decelCurve = r }
                   else hapticReject() }
            4 -> { val r = stepNodeX(curve, i, +CurveEditorConstants.CURVE_STEP_VELOCITY * nodeStepScale)
                   if (r != null) { if (activeTab == 0) accelCurve = r else decelCurve = r }
                   else hapticReject() }
            5 -> { val r = stepNodeY(curve, i, -CurveEditorConstants.CURVE_STEP_MULTIPLIER * nodeStepScale)
                   if (r != null) { if (activeTab == 0) accelCurve = r else decelCurve = r }
                   else hapticReject() }
            6 -> { val r = stepNodeY(curve, i, +CurveEditorConstants.CURVE_STEP_MULTIPLIER * nodeStepScale)
                   if (r != null) { if (activeTab == 0) accelCurve = r else decelCurve = r }
                   else hapticReject() }
            7 -> { val insertAt = if (i == curve.lastIndex) (i - 1).coerceAtLeast(0) else i
                   val r = addNodeAfter(curve, insertAt)
                   if (r != null) { if (activeTab == 0) accelCurve = r else decelCurve = r
                                    selectedNodeIndex = insertAt + 1
                                    hoveredSlot = 3; awaitingConfirm = true }
                   else hapticReject() }
            8 -> { val r = deleteNodeAt(curve, i)
                   if (r != null) { if (activeTab == 0) accelCurve = r else decelCurve = r
                                    selectedNodeIndex = (i - 1).coerceAtLeast(0)
                                    hoveredSlot = 3; awaitingConfirm = true }
                   else hapticReject() }
            9 -> { activeTab = 1 - activeTab; selectedNodeIndex = 0 }
            10 -> { stepPickerVisible = true }
            11 -> { // 노드 선택 모드로 복귀
                gridContext = GridContext.NodeEdit.Select
            }
        }
    }

    fun buildPresetSnapshot() = CustomPointerDynamicsPreset(
        id = presetId, name = name,
        accelerationCurve = accelCurve, decelerationCurve = decelCurve,
        description = description, iconKey = selectedIconKey,
        colorHex = selectedColorHex
    )

    fun executeSlot(index: Int) {
        when (index) {
            0 -> currentScreen = EditorScreen.IconPicker
            1 -> currentScreen = EditorScreen.Keyboard(FIELD_NAME)
            2 -> currentScreen = EditorScreen.Keyboard(FIELD_DESC)
            3 -> currentScreen = EditorScreen.TemplatePicker
            4 -> { activeTab = 0; selectedNodeIndex = 0 }
            5 -> { activeTab = 1; selectedNodeIndex = 0 }
            6 -> if (activeTab == 0) { decelCurve = accelCurve.toList(); activeTab = 1 }
            7 -> onDismiss()
            8 -> if (nameValid) {
                gridContext = GridContext.SaveConfirm
                hoveredSlot = 1
                awaitingConfirm = true
            }
            9 -> { // 노드 편집 진입 — 선택 모드부터 시작
                gridContext = GridContext.NodeEdit.Select
                selectedNodeIndex = 0
            }
            10 -> { // 색상 피커
                colorPickerStage = ColorPickerStage.Category
                currentScreen = EditorScreen.ColorPicker
            }
        }
    }

    val isGraphScreen = currentScreen is EditorScreen.Graph
    val isKeyboardScreen = currentScreen is EditorScreen.Keyboard

    LaunchedEffect(isGraphScreen) {
        if (!isGraphScreen) {
            hoveredSlot = -1
            awaitingConfirm = false
            gridContext = GridContext.Main
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BG)
            .pointerInput(isGraphScreen, nameValid, activeTab, gridContext, stepPickerVisible) {
                if (!isGraphScreen || stepPickerVisible) return@pointerInput
                // 선택 모드는 그래프 Box에서 별도 처리
                if (gridContext is GridContext.NodeEdit.Select) return@pointerInput
                // 저장 확인 오버레이는 오버레이 자체에서 처리
                if (gridContext is GridContext.SaveConfirm) return@pointerInput
                val tapThreshPx = 10.dp.toPx()
                val stepPx = CurveEditorConstants.ACTION_GRID_SWIPE_STEP_DP.dp.toPx()
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startPos = down.position
                    var hasMoved = false
                    val gestureStartSlot = hoveredSlot

                    var ev = awaitPointerEvent()
                    while (ev.type != PointerEventType.Release) {
                        if (ev.type == PointerEventType.Move) {
                            ev.changes.forEach { it.consume() }
                            val pos = ev.changes.first().position
                            val dragDelta = pos - startPos
                            if (!hasMoved && sqrt(dragDelta.x * dragDelta.x + dragDelta.y * dragDelta.y) > tapThreshPx) {
                                hasMoved = true
                                awaitingConfirm = false
                            }
                            if (hasMoved) {
                                val (rowSlots, defaultStartRow) = if (gridContext is GridContext.NodeEdit.Manipulate)
                                    NODE_EDIT_MANIPULATE_ROW_SLOTS to NODE_EDIT_START_ROW
                                else
                                    ACTION_ROW_SLOTS to MAIN_START_ROW
                                var gestureStartRow = defaultStartRow
                                var gestureStartCol: Int? = null
                                for ((r, rowList) in rowSlots.withIndex()) {
                                    val c = rowList.indexOf(gestureStartSlot)
                                    if (c >= 0) { gestureStartRow = r; gestureStartCol = c; break }
                                }
                                val newSlot = resolveSlot(dragDelta, stepPx, rowSlots, gestureStartRow, gestureStartCol)
                                if (newSlot != hoveredSlot) {
                                    hoveredSlot = newSlot
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                }
                            }
                        }
                        ev = awaitPointerEvent()
                    }

                    if (hasMoved) {
                        awaitingConfirm = true
                    } else if (awaitingConfirm && hoveredSlot >= 0) {
                        val isNodeEdit = gridContext is GridContext.NodeEdit
                        val enabled = if (isNodeEdit) {
                            isNodeEditSlotEnabled(hoveredSlot)
                        } else {
                            actionSlots.getOrNull(hoveredSlot)?.enabled ?: false
                        }
                        if (enabled) {
                            if (isNodeEdit) executeNodeEditSlot(hoveredSlot)
                            else executeSlot(hoveredSlot)
                            hapticConfirm()
                        } else {
                            hapticReject()
                        }
                        // 실행 후 컨텍스트 기준 판정 (executeSlot(9)이 NodeEdit으로 전환한 경우 포함)
                        if (gridContext is GridContext.NodeEdit) {
                            awaitingConfirm = true
                        } else {
                            hoveredSlot = -1
                            awaitingConfirm = false
                        }
                    }
                }
            }
    ) {
        // ── 베이스 레이어: 항상 렌더 (서브메뉴 활성 시 외부 카드 dim) ──
        Column(modifier = Modifier.fillMaxSize()) {

            EditorHeader(
                nameValid = nameValid,
                hoveredSlot = hoveredSlot,
                awaitingConfirm = awaitingConfirm,
                onDismiss = onDismiss,
                onSave = {
                    gridContext = GridContext.SaveConfirm
                    hoveredSlot = 1
                    awaitingConfirm = true
                }
            )

            // ── EditorHeader 아래 전체 영역. 내부 콘텐츠(Column)와 키보드 히트영역
            //    오버레이(isKeyboardScreen일 때만)가 BoxScope 자식으로 겹쳐진다.
            //    EditorHeader는 이 Box 바깥이라 오버레이에 덮이지 않음(뒤로가기/저장 버튼 보호). ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
            Column(modifier = Modifier.fillMaxSize()) {

            // NodeEdit 모드: MetaCard 대신 NodeEditHeader (헤더 strip)
            // C3: gridContext 전환 시 fade 트랜지션
            if (isGraphScreen) {
                AnimatedContent(
                    targetState = gridContext is GridContext.NodeEdit,
                    transitionSpec = {
                        fadeIn(tween(200)) togetherWith fadeOut(tween(150))
                    },
                    label = "headerCtx"
                ) { isNodeEdit ->
                    if (isNodeEdit) {
                        NodeEditHeader(
                            hoveredSlot = hoveredSlot,
                            awaitingConfirm = awaitingConfirm,
                            isSelectMode = gridContext is GridContext.NodeEdit.Select,
                            onTabLabelHoveredChange = { tabLabelHovered = it },
                            onTabToggle = {
                                activeTab = 1 - activeTab
                                selectedNodeIndex = 0
                                tabLabelHovered = false
                                hapticConfirm()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    } else {
                        MetaCard(
                            slots = actionSlots,
                            hoveredSlot = hoveredSlot,
                            awaitingConfirm = awaitingConfirm,
                            selectedIconKey = selectedIconKey,
                            selectedColorHex = selectedColorHex,
                            name = name,
                            description = description,
                            onIconSlotPositioned = { iconPickerAnchor = it },
                            onColorSlotPositioned = { colorPickerAnchor = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }
                }
            } else {
                MetaCard(
                    slots = actionSlots,
                    hoveredSlot = hoveredSlot,
                    awaitingConfirm = awaitingConfirm,
                    selectedIconKey = selectedIconKey,
                    selectedColorHex = selectedColorHex,
                    name = name,
                    description = description,
                    editingTarget = (currentScreen as? EditorScreen.Keyboard)?.target,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                        .then(if (!isKeyboardScreen) Modifier.alpha(0.35f) else Modifier)
                )
            }

            // 그래프 화면일 때만 Canvas 렌더. 서브메뉴는 아래 오버레이 레이어에서 렌더.
            val currentCurve = if (activeTab == 0) accelCurve else decelCurve
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 8.dp)
                    .onGloballyPositioned { coords ->
                        // 이 Box의 부모(위 새 컨테이너 Box) 기준 y 오프셋 — 키보드 오버레이 위치 고정용
                        graphBoxTopOffsetPx = coords.positionInParent().y
                    }
                    .pointerInput(gridContext, activeTab, currentCurve.size) {
                        // 선택 모드일 때만 그래프 위 직접 스와이프+탭 처리
                        if (gridContext !is GridContext.NodeEdit.Select) return@pointerInput
                        val tapThreshPx = 10.dp.toPx()
                        val stepPx = CurveEditorConstants.NODE_SELECT_SWIPE_STEP_DP.dp.toPx()
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val startPos = down.position
                            val startIdx = selectedNodeIndex
                            var hasMoved = false

                            var ev = awaitPointerEvent()
                            while (ev.type != PointerEventType.Release) {
                                if (ev.type == PointerEventType.Move) {
                                    ev.changes.forEach { it.consume() }
                                    val pos = ev.changes.first().position
                                    val dx = pos.x - startPos.x
                                    val dy = pos.y - startPos.y
                                    if (!hasMoved && sqrt(dx * dx + dy * dy) > tapThreshPx) {
                                        hasMoved = true
                                    }
                                    if (hasMoved) {
                                        val steps = (dx / stepPx).roundToInt()
                                        val newIdx = (startIdx + steps)
                                            .coerceIn(0, currentCurve.lastIndex)
                                        if (newIdx != selectedNodeIndex) {
                                            selectedNodeIndex = newIdx
                                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                        }
                                    }
                                }
                                ev = awaitPointerEvent()
                            }

                            if (!hasMoved) {
                                // 탭 → 조작 모드 진입
                                gridContext = GridContext.NodeEdit.Manipulate
                                hoveredSlot = 10
                                awaitingConfirm = true
                                hapticConfirm()
                            }
                        }
                    }
            ) {
                when (val screen = currentScreen) {
                    is EditorScreen.Graph -> {
                        val isNodeEdit = gridContext is GridContext.NodeEdit
                        AnimatedContent(
                            targetState = activeTab,
                            transitionSpec = {
                                val dir = if (targetState > initialState) 1 else -1
                                (slideInHorizontally(tween(250)) { it * dir } + fadeIn(tween(200))) togetherWith
                                (slideOutHorizontally(tween(200)) { -it * dir } + fadeOut(tween(150)))
                            },
                            label = "tabTransition"
                        ) { tab ->
                            CurveGraphCanvas(
                                activeCurve = if (tab == 0) accelCurve else decelCurve,
                                inactiveCurve = if (tab == 0) decelCurve else accelCurve,
                                activeColor = if (tab == 0) ACCENT_BLUE else ACCENT_ORANGE,
                                inactiveColor = if (tab == 0) ACCENT_ORANGE else ACCENT_BLUE,
                                selectedNodeIndex = if (isNodeEdit) selectedNodeIndex else -1,
                                isSelectMode = gridContext is GridContext.NodeEdit.Select,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        // 슬롯 9 "노드 편집" — 그래프 컨테이너 내 하단 오버레이 (Main 모드만)
                        if (gridContext is GridContext.Main) {
                            Slot9Card(
                                hoveredSlot = hoveredSlot,
                                awaitingConfirm = awaitingConfirm,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth(0.30f)
                                    .padding(bottom = 36.dp)
                            )
                        }
                        // 활성 탭 라벨 — 우상단 오버레이 (NodeEdit 모드만)
                        if (gridContext is GridContext.NodeEdit) ActiveTabLabel(
                            activeTab = activeTab,
                            isHovered = tabLabelHovered || (hoveredSlot == 9 && gridContext is GridContext.NodeEdit.Manipulate),
                            onToggle = {
                                activeTab = 1 - activeTab
                                selectedNodeIndex = 0
                                hapticConfirm()
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(end = 10.dp, top = 6.dp)
                        )
                    }
                    // EditorScreen.Keyboard는 이 Box에서 더 이상 렌더하지 않음 — 아래 오버레이에서 처리
                    else -> {}
                }
            }

            // C3: gridContext 전환 시 슬라이드+페이드 트랜지션
            if (isGraphScreen) {
                AnimatedContent(
                    targetState = gridContext is GridContext.NodeEdit,
                    transitionSpec = {
                        val enterSlide = if (targetState) slideInVertically(tween(200)) { it / 10 }
                                        else slideInVertically(tween(200)) { -it / 10 }
                        val exitSlide  = if (targetState) slideOutVertically(tween(150)) { -it / 10 }
                                        else slideOutVertically(tween(150)) { it / 10 }
                        (fadeIn(tween(200)) + enterSlide) togetherWith (fadeOut(tween(150)) + exitSlide)
                    },
                    label = "gridCtx"
                ) { isNodeEdit ->
                    if (isNodeEdit) {
                        NodeEditGrid(
                            activeCurve = if (activeTab == 0) accelCurve else decelCurve,
                            selectedNodeIndex = selectedNodeIndex,
                            activeTab = activeTab,
                            hoveredSlot = hoveredSlot,
                            awaitingConfirm = awaitingConfirm,
                            isSlotEnabled = ::isNodeEditSlotEnabled,
                            nodeStepIndex = nodeStepIndex,
                            isSelectMode = gridContext is GridContext.NodeEdit.Select,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        EditorActionGrid(
                            slots = actionSlots,
                            hoveredSlot = hoveredSlot,
                            awaitingConfirm = awaitingConfirm,
                            activeTab = activeTab,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else if (!isKeyboardScreen) {
                EditorActionGrid(
                    slots = actionSlots,
                    hoveredSlot = hoveredSlot,
                    awaitingConfirm = awaitingConfirm,
                    activeTab = activeTab,
                    modifier = Modifier.fillMaxWidth().alpha(0.35f)
                )
            }
            } // end 내부 Column

            // ── 키보드 히트영역 오버레이: 컨테이너 전체(fillMaxSize)를 히트영역으로 쓰되,
            //    시각적으로는 그래프Box와 동일한 y위치/가로패딩으로 그려짐 ──
            if (isKeyboardScreen) {
                val screen = currentScreen as EditorScreen.Keyboard
                val isName = screen.target == FIELD_NAME
                val initialText = if (isName) name else description
                val maxLen = if (isName) NAME_MAX_LEN else DESC_MAX_LEN
                val targetSlot = if (isName) 1 else 2
                val density = LocalDensity.current
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp)
                ) {
                    key(screen.target) {
                        SwipeKeyboardOverlay(
                            initialText = initialText,
                            maxLength = maxLen,
                            showScrim = false,
                            expandHitAreaOnly = true,
                            contentTopOffsetDp = with(density) { graphBoxTopOffsetPx.toDp() },
                            suggestions = if (isName)
                                CurveEditorConstants.CURVE_NAME_SUGGESTIONS
                            else
                                CurveEditorConstants.CURVE_DESC_SUGGESTIONS,
                            onTextChange = { text ->
                                if (isName) name = text else description = text
                            },
                            onNext = if (isName) { _ ->
                                currentScreen = EditorScreen.Keyboard(FIELD_DESC)
                            } else null,
                            onPrev = if (!isName) { _ ->
                                currentScreen = EditorScreen.Keyboard(FIELD_NAME)
                            } else null,
                            onCancel = {
                                hoveredSlot = targetSlot; awaitingConfirm = true
                                currentScreen = EditorScreen.Graph
                            },
                            onDone = { result ->
                                if (isName) name = result else description = result
                                hoveredSlot = targetSlot; awaitingConfirm = true
                                currentScreen = EditorScreen.Graph
                            }
                        )
                    }
                }
            }
            } // end 컨테이너 Box
        }

        // ── 오버레이 레이어: 서브메뉴 활성 시 풀스크린으로 렌더 ──
        // Box 의 두 번째 자식 → z-order 상위. 제스처가 화면 어디서든 흡수됨.
        when (currentScreen) {
            is EditorScreen.TemplatePicker -> {
                TemplatePickerContent(
                    templates = CUSTOM_PRESET_TEMPLATES,
                    initialSelectedIndex = savedTemplateIndex,
                    onIndexChange = { savedTemplateIndex = it },
                    onClose = {
                        hoveredSlot = 3; awaitingConfirm = true
                        currentScreen = EditorScreen.Graph
                    },
                    onSelect = { template ->
                        accelCurve = template.accelerationCurve
                        decelCurve = template.decelerationCurve
                        hoveredSlot = 3; awaitingConfirm = true
                        currentScreen = EditorScreen.Graph
                    }
                )
            }
            else -> {}
        }

        // ── 아이콘 피커(카테고리 서랍): 진입/이탈 fade + scale ──
        AnimatedVisibility(
            visible = currentScreen is EditorScreen.IconPicker,
            enter = fadeIn(tween(200)) + scaleIn(tween(200), initialScale = 0.92f),
            exit = fadeOut(tween(150)) + scaleOut(tween(150), targetScale = 0.92f),
            modifier = Modifier.matchParentSize(),
        ) {
            CompositionLocalProvider(LocalSwipeFocusController provides iconPickerController) {
                Box(modifier = Modifier.fillMaxSize()) {
                    CategoryIconDrawer(
                        controller = iconPickerController,
                        stage = iconPickerStage,
                        onStageChange = { iconPickerStage = it },
                        selectedIconKey = selectedIconKey,
                        anchorCenterInWindow = iconPickerAnchor,
                        scope = ROOT_SCOPE,
                        onPick = { key ->
                            selectedIconKey = key
                            hoveredSlot = 0; awaitingConfirm = true
                            currentScreen = EditorScreen.Graph
                        },
                        modifier = Modifier.matchParentSize(),
                    )
                    SwipeGestureLayer(
                        controller = iconPickerController,
                        modifier = Modifier.matchParentSize(),
                        onLongPress = {
                            // 롱프레스: 아이콘 단계 → 카테고리 단계 복귀, 카테고리 단계 → 닫기(변경 취소)
                            if (iconPickerStage is IconDrawerStage.Icons) {
                                iconPickerStage = IconDrawerStage.Category
                            } else {
                                hoveredSlot = 0; awaitingConfirm = true
                                currentScreen = EditorScreen.Graph
                            }
                            true
                        },
                    )
                }
            }
        }

        // ── 컬러 피커 오버레이 ──
        AnimatedVisibility(
            visible = currentScreen is EditorScreen.ColorPicker,
            enter = fadeIn(tween(200)) + scaleIn(tween(200), initialScale = 0.92f),
            exit = fadeOut(tween(150)) + scaleOut(tween(150), targetScale = 0.92f),
            modifier = Modifier.matchParentSize(),
        ) {
            CompositionLocalProvider(LocalSwipeFocusController provides colorPickerController) {
                Box(modifier = Modifier.fillMaxSize()) {
                    ColorPickerSwipe(
                        controller = colorPickerController,
                        pickerScope = ROOT_SCOPE,
                        selectedColorHex = selectedColorHex,
                        anchorCenterInWindow = colorPickerAnchor,
                        stage = colorPickerStage,
                        onStageChange = { colorPickerStage = it },
                        onPick = { hex ->
                            selectedColorHex = hex
                            hoveredSlot = 10; awaitingConfirm = true
                            currentScreen = EditorScreen.Graph
                        },
                        onCommitCandidateChange = { colorCommitCandidate = it },
                        modifier = Modifier.matchParentSize(),
                    )
                    SwipeGestureLayer(
                        controller = colorPickerController,
                        modifier = Modifier.matchParentSize(),
                        onLongPress = {
                            // 단계별 뒤로가기: DirectInput→Swatches(or Category), Swatches→Category, Category→닫기
                            when (val s = colorPickerStage) {
                                is ColorPickerStage.DirectInput -> colorPickerStage = if (s.sourceTab != null)
                                    ColorPickerStage.Swatches(s.sourceTab) else ColorPickerStage.Category
                                is ColorPickerStage.Swatches    -> colorPickerStage = ColorPickerStage.Category
                                is ColorPickerStage.Category    -> {
                                    hoveredSlot = 10; awaitingConfirm = true
                                    currentScreen = EditorScreen.Graph
                                }
                            }
                            true
                        },
                    )
                }
            }
        }

        // ── 스텝 정밀도 피커 오버레이 (C5: AnimatedVisibility 페이드+스케일) ──
        AnimatedVisibility(
            visible = stepPickerVisible,
            enter = fadeIn(tween(200)) + scaleIn(tween(200), initialScale = 0.92f),
            exit  = fadeOut(tween(150)) + scaleOut(tween(150), targetScale = 0.92f)
        ) {
            NodeStepScalePickerOverlay(
                currentIndex = nodeStepIndex,
                onConfirm = { idx -> nodeStepIndex = idx; stepPickerVisible = false },
            )
        }

        // ── 저장 확인 오버레이 ──
        AnimatedVisibility(
            visible = gridContext is GridContext.SaveConfirm,
            enter = fadeIn(tween(200)) + scaleIn(tween(200), initialScale = 0.96f),
            exit  = fadeOut(tween(150)) + scaleOut(tween(150), targetScale = 0.96f)
        ) {
            SaveConfirmOverlay(
                curveDesc = describeCurves(accelCurve, decelCurve),
                onSave = { onSave(buildPresetSnapshot()) },
                onBack = {
                    gridContext = GridContext.Main
                    hoveredSlot = 8
                    awaitingConfirm = true
                }
            )
        }
    }
}
