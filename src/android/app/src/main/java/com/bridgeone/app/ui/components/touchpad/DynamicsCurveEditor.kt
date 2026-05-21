package com.bridgeone.app.ui.components.touchpad

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bridgeone.app.ui.components.SwipeKeyboardOverlay
import com.bridgeone.app.ui.common.AppIcon
import com.bridgeone.app.ui.common.AppIconDef
import com.bridgeone.app.ui.common.CUSTOM_PRESET_ICON_OPTIONS
import com.bridgeone.app.ui.common.CUSTOM_PRESET_TEMPLATES
import com.bridgeone.app.ui.common.CurveEditorConstants
import com.bridgeone.app.ui.common.CurveEditorConstants.TEMPLATE_PICKER_SWIPE_STEP_DP
import com.bridgeone.app.ui.common.CurveNode
import com.bridgeone.app.ui.common.CustomPointerDynamicsPreset
import com.bridgeone.app.ui.common.addNodeAfter
import com.bridgeone.app.ui.common.customPresetIconOrNull
import com.bridgeone.app.ui.common.defaultAccelerationCurve
import com.bridgeone.app.ui.common.defaultDecelerationCurve
import com.bridgeone.app.ui.common.deleteNodeAt
import com.bridgeone.app.ui.common.describeCurves
import com.bridgeone.app.ui.common.CurveDescription
import com.bridgeone.app.ui.common.stepNodeX
import com.bridgeone.app.ui.common.stepNodeY
import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

private val BG = Color(0xFF0D0D0D)
private val SURFACE = Color(0xFF1A1A1A)
private val ACCENT_BLUE = Color(0xFF4F8EF7)
private val ACCENT_ORANGE = Color(0xFFFF9800)
private val ACCENT_RED = Color(0xFFFFB4B4)   // 파괴적 액션(삭제) 비활성 텍스트 색
private val GRID_COLOR = Color(0xFF2A2A2A)
private val LABEL_COLOR = Color(0xFF888888)

private const val NAME_MAX_LEN = 12
private const val DESC_MAX_LEN = 50
private const val FIELD_NAME = "name"
private const val FIELD_DESC = "desc"

// ─────────────────────────────────────────────────────────────
// 화면 상태 (Phase 4.5.18.4: Boolean 3개 → sealed class 1개)
// ─────────────────────────────────────────────────────────────

private sealed class EditorScreen {
    object Graph : EditorScreen()
    data class Keyboard(val target: String) : EditorScreen() // "name" | "desc"
    object IconPicker : EditorScreen()
    object TemplatePicker : EditorScreen()
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

// ─────────────────────────────────────────────────────────────
// 액션 그리드 슬롯 모델
// ─────────────────────────────────────────────────────────────

private enum class SlotStyle { NORMAL, PRIMARY, SECONDARY, SEGMENT_LEFT, SEGMENT_RIGHT }

private data class ActionSlot(
    val label: String,
    val enabled: Boolean,
    val isCurrent: Boolean = false,
    val previewText: String = "",
    val iconKey: String = "",
    val style: SlotStyle = SlotStyle.NORMAL
)

// ── 메인 그리드 슬롯 배치
// Row 0 (Header): 7=취소, 8=저장
// Row 1 (MetaCard 상단): 0=아이콘, 1=이름, 3=템플릿
// Row 2 (MetaCard 하단): 2=설명, 3=템플릿
// Row 3 (노드 편집, 단독): 9=노드 편집
// Row 4 (CurveCard, 기준 행): 4=가속, 6=→복사, 5=감속
private val ACTION_ROW_SLOTS = listOf(
    listOf(7, 8),      // Row 0: Header (취소, 저장)
    listOf(0, 1, 3),   // Row 1: MetaCard 상단
    listOf(2, 3),      // Row 2: MetaCard 하단
    listOf(9),         // Row 3: 노드 편집 (단독)
    listOf(4, 6, 5),   // Row 4: CurveCard (기준 행)
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
private val SAVE_CONFIRM_ROW_SLOTS = listOf(
    listOf(0, 1)
)
private const val SAVE_CONFIRM_START_ROW = 0

// ─────────────────────────────────────────────────────────────
// 아이콘 선택 — 셀 모델
// ─────────────────────────────────────────────────────────────

private data class IconCell(val key: String, val def: AppIconDef)

private data class IconRow(val cells: List<IconCell>)

private data class IconCellPos(val row: Int, val col: Int)

private enum class HeaderZone { BACK, NONE }

/** 5개씩 6행, 총 30개 아이콘 */
private fun buildIconLayout(): List<IconRow> =
    CUSTOM_PRESET_ICON_OPTIONS.chunked(5).map { chunk ->
        IconRow(cells = chunk.map { (key, def) -> IconCell(key, def) })
    }

/** selectedIconKey에 해당하는 초기 셀 위치 반환. 매칭 없으면 (0,0). */
private fun findInitialCell(selectedIconKey: String, layout: List<IconRow>): IconCellPos {
    layout.forEachIndexed { rowIdx, row ->
        row.cells.forEachIndexed { colIdx, cell ->
            if (cell.key == selectedIconKey) return IconCellPos(rowIdx, colIdx)
        }
    }
    return IconCellPos(0, 0)
}

/** 범위 클램프 */
private fun resolveIconCell(target: IconCellPos, layout: List<IconRow>): IconCellPos {
    val row = target.row.coerceIn(0, layout.size - 1)
    val col = target.col.coerceIn(0, layout[row].cells.size - 1)
    return IconCellPos(row, col)
}

/** 셀 중심의 분수(0~1) 가로 위치 (균등 weight 전제) */
private fun computeFracX(cell: IconCellPos, layout: List<IconRow>): Float {
    val count = layout[cell.row].cells.size.toFloat()
    return (cell.col + 0.5f) / count
}

/** fracX에 해당하는 컬럼 인덱스 반환 (균등 weight 전제) */
private fun findColAtFracX(fracX: Float, row: IconRow): Int =
    (fracX * row.cells.size).toInt().coerceIn(0, row.cells.lastIndex)

/**
 * 가변 슬롯 매핑. 드래그 시작점 기준 상대 이동량(dragDelta)으로 슬롯 결정.
 * 화면 절대 좌표와 무관하므로 스와이프 시작 위치에 관계없이 동일하게 동작.
 * @param dragDelta  fingerPos - startPos (드래그 시작 기준 상대 이동량)
 * @param rowSlots   ACTION_ROW_SLOTS 또는 NODE_EDIT_ROW_SLOTS
 * @param startRow   기준 행 인덱스 (MAIN_START_ROW 또는 NODE_EDIT_START_ROW)
 */
private fun resolveSlot(
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

    var currentScreen by remember { mutableStateOf<EditorScreen>(EditorScreen.Graph) }
    var hoveredSlot by remember { mutableIntStateOf(0) }
    var awaitingConfirm by remember { mutableStateOf(true) }
    var gridContext by remember { mutableStateOf<GridContext>(GridContext.Main) }
    var selectedNodeIndex by remember { mutableIntStateOf(0) }
    var nodeStepIndex by remember { mutableIntStateOf(CurveEditorConstants.NODE_STEP_DEFAULT_INDEX) }
    var stepPickerVisible by remember { mutableStateOf(false) }
    val presetId = remember { initialPreset?.id ?: UUID.randomUUID().toString() }
    val nodeStepScale = CurveEditorConstants.NODE_STEP_SCALES[nodeStepIndex]
    var tabLabelHovered by remember { mutableStateOf(false) }

    // 서브메뉴 재진입 시 위치 유지
    val iconLayout = remember { buildIconLayout() }
    var savedIconCell by remember { mutableStateOf<IconCellPos?>(null) }
    var savedTemplateIndex by remember { mutableIntStateOf(0) }

    val isDuplicate = name.isNotBlank() &&
            existingPresets.any { it.name == name && it.id != (initialPreset?.id ?: "") }
    val nameValid = name.isNotBlank() && !isDuplicate

    // 탭 전환 시 선택 노드 초기화
    LaunchedEffect(activeTab) { selectedNodeIndex = 0 }

    // 슬롯 인덱스: 0=아이콘, 1=이름, 2=설명, 3=템플릿
    //              4=가속, 5=감속, 6=→감속복사
    //              7=취소, 8=저장, 9=노드 편집
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
        ActionSlot("노드 편집", enabled = true, style = SlotStyle.NORMAL)
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
        description = description, iconKey = selectedIconKey
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
                            name = name,
                            description = description,
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
                    is EditorScreen.Keyboard -> {
                        val isName = screen.target == FIELD_NAME
                        val initialText = if (isName) name else description
                        val maxLen = if (isName) NAME_MAX_LEN else DESC_MAX_LEN
                        val targetSlot = if (isName) 1 else 2
                        key(screen.target) {
                            SwipeKeyboardOverlay(
                                initialText = initialText,
                                maxLength = maxLen,
                                showScrim = false,
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
        }

        // ── 오버레이 레이어: 서브메뉴 활성 시 풀스크린으로 렌더 ──
        // Box 의 두 번째 자식 → z-order 상위. 제스처가 화면 어디서든 흡수됨.
        when (currentScreen) {
            is EditorScreen.IconPicker -> {
                IconPickerContent(
                    selectedIconKey = selectedIconKey,
                    initialCell = savedIconCell ?: findInitialCell(selectedIconKey, iconLayout),
                    onCellChange = { savedIconCell = it },
                    onClose = {
                        hoveredSlot = 0; awaitingConfirm = true
                        currentScreen = EditorScreen.Graph
                    },
                    onSelect = { key ->
                        selectedIconKey = key
                        hoveredSlot = 0; awaitingConfirm = true
                        currentScreen = EditorScreen.Graph
                    }
                )
            }
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
            is EditorScreen.Graph, is EditorScreen.Keyboard -> {}
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

// ─────────────────────────────────────────────────────────────
// 액션 그리드 (커브 카드 + 액션 카드)
// ─────────────────────────────────────────────────────────────

private fun cellBgColor(
    isHovered: Boolean, isAwaitingConfirm: Boolean, enabled: Boolean, isCurrent: Boolean = false
): Color = when {
    isAwaitingConfirm && enabled -> ACCENT_BLUE.copy(alpha = 0.55f)
    isAwaitingConfirm            -> Color.White.copy(alpha = 0.12f)
    isHovered && enabled         -> ACCENT_BLUE.copy(alpha = 0.38f)
    isHovered                    -> Color.White.copy(alpha = 0.06f)
    isCurrent                    -> ACCENT_BLUE.copy(alpha = 0.18f)
    else                         -> Color.Transparent
}

private fun Modifier.cellBorder(
    isHovered: Boolean, isAwaitingConfirm: Boolean, enabled: Boolean,
    isCurrent: Boolean = false, shape: RoundedCornerShape = RoundedCornerShape(6.dp)
): Modifier = this.then(when {
    isAwaitingConfirm && enabled -> Modifier.border(2.dp, Color.White.copy(alpha = 0.9f), shape)
    isAwaitingConfirm            -> Modifier.border(2.dp, Color.White.copy(alpha = 0.4f), shape)
    isHovered && enabled         -> Modifier.border(1.5.dp, ACCENT_BLUE, shape)
    isCurrent                    -> Modifier.border(1.dp, ACCENT_BLUE, shape)
    else                         -> Modifier
})

/**
 * 노드 편집 그리드/헤더 전용 셀 박스. 상태 색·보더를 120ms 트랜지션으로 부드럽게 전환.
 * - tintColor: 호버/awaiting 강조 색 (기본 ACCENT_BLUE; 삭제 슬롯은 ACCENT_RED 전달)
 * - showAccentIdleBorder: true → idle enabled 보더를 tintColor 0.45 alpha로 (추가/삭제/탭토글 전용)
 */
@Composable
private fun AnimatedCellBox(
    isHovered: Boolean,
    isAwaitingConfirm: Boolean,
    enabled: Boolean,
    shape: Shape = RoundedCornerShape(8.dp),
    tintColor: Color = ACCENT_BLUE,
    showAccentIdleBorder: Boolean = false,
    contentAlignment: Alignment = Alignment.Center,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val bgTarget = when {
        isAwaitingConfirm && enabled -> tintColor.copy(alpha = 0.55f)
        isAwaitingConfirm            -> Color.White.copy(alpha = 0.12f)
        isHovered && enabled         -> tintColor.copy(alpha = 0.38f)
        isHovered                    -> Color.White.copy(alpha = 0.06f)
        !enabled                     -> Color.White.copy(alpha = 0.03f)
        else                         -> Color.Transparent
    }
    val animBg by animateColorAsState(bgTarget, tween(120, easing = FastOutSlowInEasing), label = "cellBg")

    val borderColorTarget = when {
        isAwaitingConfirm && enabled -> Color.White.copy(alpha = 0.9f)
        isAwaitingConfirm            -> Color.White.copy(alpha = 0.4f)
        isHovered && enabled         -> tintColor
        showAccentIdleBorder && enabled -> tintColor.copy(alpha = 0.45f)
        enabled                      -> Color.White.copy(alpha = 0.10f)
        else                         -> Color.Transparent
    }
    val animBorderColor by animateColorAsState(borderColorTarget, tween(120, easing = FastOutSlowInEasing), label = "cellBorderColor")

    val borderWidth = when {
        isAwaitingConfirm            -> 2.dp
        isHovered && enabled         -> 1.5.dp
        showAccentIdleBorder && enabled -> 1.dp
        enabled                      -> 0.5.dp
        else                         -> 0.dp
    }

    Box(
        modifier = modifier
            .background(animBg, shape)
            .border(borderWidth, animBorderColor, shape),
        contentAlignment = contentAlignment,
        content = content
    )
}

@Composable
private fun EditorActionGrid(
    slots: List<ActionSlot>,
    hoveredSlot: Int,
    awaitingConfirm: Boolean,
    activeTab: Int,
    modifier: Modifier = Modifier
) {
    CurveCard(
        slots = slots,
        hoveredSlot = hoveredSlot,
        awaitingConfirm = awaitingConfirm,
        activeTab = activeTab,
        modifier = modifier.padding(horizontal = 6.dp, vertical = 4.dp)
    )
}

/** 슬롯 9: 노드 편집 진입 버튼 (그래프 컨테이너 내 하단 오버레이) */
@Composable
private fun Slot9Card(hoveredSlot: Int, awaitingConfirm: Boolean, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(10.dp)
    val isHovered = hoveredSlot == 9
    val isAwaiting = awaitingConfirm && isHovered
    val bg = when {
        isAwaiting  -> ACCENT_BLUE.copy(alpha = 0.85f)
        isHovered   -> ACCENT_BLUE.copy(alpha = 0.55f)
        else        -> SURFACE
    }
    val borderMod: Modifier = when {
        isAwaiting -> Modifier.border(2.dp, Color.White.copy(alpha = 0.9f), shape)
        isHovered  -> Modifier.border(1.5.dp, ACCENT_BLUE, shape)
        else       -> Modifier.border(1.dp, Color.White.copy(alpha = 0.45f), shape)
    }
    Box(
        modifier = modifier
            .height(34.dp)
            .background(bg, shape)
            .then(borderMod),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "노드 편집",
            color = if (isHovered || isAwaiting) Color.White else Color.White.copy(alpha = 0.55f),
            fontSize = 12.sp,
            fontWeight = if (isHovered || isAwaiting) FontWeight.Bold else FontWeight.Normal
        )
    }
}

/**
 * 노드 편집 헤더 strip (Phase 4.5.18.7)
 * NodeEdit 모드에서 MetaCard 자리에 렌더. 좌=뒤로(slot 0)만. 탭 토글은 그래프 내부 ActiveTabLabel로 이동.
 */
@Composable
private fun NodeEditHeader(
    hoveredSlot: Int,
    awaitingConfirm: Boolean,
    isSelectMode: Boolean = false,
    onTabLabelHoveredChange: ((Boolean) -> Unit)? = null,
    onTabToggle: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(10.dp)
    val view = LocalView.current
    Row(
        modifier = modifier
            .height(28.dp)
            .padding(horizontal = 4.dp)
            .then(
                if (isSelectMode && onTabLabelHoveredChange != null && onTabToggle != null)
                    Modifier.pointerInput(Unit) {
                        val downThreshPx = 40.dp.toPx()
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val startY = down.position.y
                            var hovering = false
                            var ev = awaitPointerEvent()
                            while (ev.type != PointerEventType.Release) {
                                if (ev.type == PointerEventType.Move) {
                                    ev.changes.forEach { it.consume() }
                                    val dy = ev.changes.first().position.y - startY
                                    val nowHovering = dy > downThreshPx
                                    if (nowHovering != hovering) {
                                        hovering = nowHovering
                                        onTabLabelHoveredChange(hovering)
                                        if (hovering) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    }
                                }
                                ev = awaitPointerEvent()
                            }
                            onTabLabelHoveredChange(false)
                            if (hovering) onTabToggle()
                        }
                    }
                else Modifier
            ),
        horizontalArrangement = Arrangement.Start
    ) {
        // 슬롯 0: ← (화살표만, 아주 작게)
        val hov0 = hoveredSlot == 0; val aw0 = awaitingConfirm && hov0
        AnimatedCellBox(
            isHovered = hov0, isAwaitingConfirm = aw0, enabled = true,
            shape = cardShape,
            modifier = Modifier.width(32.dp).fillMaxHeight()
        ) {
            Text(
                "←",
                color = if (hov0 || aw0) Color.White else Color.White.copy(alpha = 0.55f),
                fontSize = 13.sp,
                fontWeight = if (hov0 || aw0) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

/** 그래프 내부 활성 탭 버튼. 헤더에서 아래로 스와이프하여 선택. (Phase 4.5.18.9) */
@Composable
private fun ActiveTabLabel(
    activeTab: Int,
    isHovered: Boolean = false,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = if (activeTab == 0) ACCENT_BLUE else ACCENT_ORANGE
    val label = if (activeTab == 0) "가속" else "감속"
    val shape = RoundedCornerShape(8.dp)
    val borderAlpha = if (isHovered) 0.75f else 0.25f
    val bgAlpha = if (isHovered) 0.25f else 0f
    Text(
        text = label,
        color = color.copy(alpha = if (isHovered) 1f else 0.9f),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .shadow(elevation = 6.dp, shape = shape)
            .clip(shape)
            .background(color.copy(alpha = bgAlpha), shape)
            .border(1.dp, Color.White.copy(alpha = borderAlpha), shape)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

/**
 * 노드 편집 본문 그리드 (Phase 4.5.18.7~8)
 * 3개 독립 카드로 분리(B1): 노드 네비 / 2D 십자 패드 / CRUD.
 * AnimatedCellBox(C1) 적용, slot10 원형화(A1), CRUD 대칭(A2).
 * NodeEdit 모드에서 EditorActionGrid 자리에 렌더.
 */
@Composable
private fun NodeEditGrid(
    activeCurve: List<CurveNode>,
    selectedNodeIndex: Int,
    activeTab: Int,
    hoveredSlot: Int,
    awaitingConfirm: Boolean,
    isSlotEnabled: (Int) -> Boolean,
    nodeStepIndex: Int,
    isSelectMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(12.dp)
    val cellShape = RoundedCornerShape(8.dp)
    val halfLeftShape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
    val halfRightShape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)
    val dividerColor = Color.White.copy(alpha = 0.07f)

    // 슬롯 텍스트 색 헬퍼
    fun slotTextColor(hov: Boolean, aw: Boolean, en: Boolean, tintColor: Color = Color.White): Color = when {
        (hov || aw) && en -> Color.White
        en                -> tintColor.copy(alpha = 0.7f)
        else              -> tintColor.copy(alpha = 0.25f)
    }

    // ── B1: 3개 독립 카드 ────────────────────────────────────────────
    Column(
        modifier = modifier.padding(horizontal = 6.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {

        // 선택 모드에서는 카드 본문 숨김 — 그래프가 공간을 채우도록 애니메이션
        AnimatedVisibility(
            visible = !isSelectMode,
            enter = expandVertically(tween(250, easing = FastOutSlowInEasing)) + fadeIn(tween(200)),
            exit = shrinkVertically(tween(200, easing = FastOutSlowInEasing)) + fadeOut(tween(150))
        ) { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {

        // ── 슬롯 11: 노드 선택 — 선택 모드 재진입 ──────────────────────
        val activeColor = if (activeTab == 0) ACCENT_BLUE else ACCENT_ORANGE
        val nodeCount = activeCurve.size
        val isHov11 = hoveredSlot == 11; val isAw11 = awaitingConfirm && isHov11; val en11 = isSlotEnabled(11)
        AnimatedCellBox(
            isHovered = isHov11, isAwaitingConfirm = isAw11, enabled = en11,
            shape = RoundedCornerShape(8.dp),
            tintColor = activeColor,
            showAccentIdleBorder = true,
            modifier = Modifier.fillMaxWidth().height(40.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "◀▶",
                    color = when {
                        (isHov11 || isAw11) -> Color.White
                        else -> activeColor.copy(alpha = 0.75f)
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "노드 선택  ${selectedNodeIndex + 1} / $nodeCount",
                    color = when {
                        (isHov11 || isAw11) -> Color.White
                        else -> activeColor.copy(alpha = 0.75f)
                    },
                    fontSize = 12.sp,
                    fontWeight = if (isHov11 || isAw11) FontWeight.Bold else FontWeight.Normal
                )
            }
        }

        // ── 십자 패드 (슬롯 3·4·5·6 + slot10 원형) ──────────────────────
        // 네 방향 버튼: fillMaxHeight().aspectRatio(1f) → 36×36dp 정사각형
        // slot10: weight(1f).fillMaxHeight() → 형태 변경 없음
        val xStep = CurveEditorConstants.CURVE_STEP_VELOCITY * CurveEditorConstants.NODE_STEP_SCALES[nodeStepIndex]
        val yStep = CurveEditorConstants.CURVE_STEP_MULTIPLIER * CurveEditorConstants.NODE_STEP_SCALES[nodeStepIndex]
        // 전체 패드 중앙 정렬
        // 방향 버튼: 36×36dp 정사각형 / 가운데: 36×64dp 둥근 직사각형
        // 상하 Spacer = (btnSize + centerW) / 2 → Y▲·Y▼이 가운데 버튼 위·아래 중심 정렬
        val btnSize = 36.dp
        val centerW = 64.dp
        val sideSpacerW = (btnSize + centerW) / 2  // 50.dp
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column {
                // 상단 행: [50dp] [Y ▲ 36×36] [50dp]
                Row {
                    Spacer(Modifier.width(sideSpacerW).height(btnSize))
                    val isHov6 = hoveredSlot == 6; val isAw6 = awaitingConfirm && isHov6; val en6 = isSlotEnabled(6)
                    AnimatedCellBox(
                        isHovered = isHov6, isAwaitingConfirm = isAw6, enabled = en6,
                        shape = cellShape, modifier = Modifier.size(btnSize)
                    ) {
                        Text("Y ▲", color = slotTextColor(isHov6, isAw6, en6), fontSize = 13.sp,
                            fontWeight = if ((isHov6 || isAw6) && en6) FontWeight.Bold else FontWeight.Normal)
                    }
                    Spacer(Modifier.width(sideSpacerW).height(btnSize))
                }
                // 중간 행: [X ◀ 36×36] [slot10 36×64 둥근 직사각형] [X ▶ 36×36]
                Row {
                    val isHov3 = hoveredSlot == 3; val isAw3 = awaitingConfirm && isHov3; val en3 = isSlotEnabled(3)
                    AnimatedCellBox(
                        isHovered = isHov3, isAwaitingConfirm = isAw3, enabled = en3,
                        shape = cellShape, modifier = Modifier.size(btnSize)
                    ) {
                        Text("X ◀", color = slotTextColor(isHov3, isAw3, en3), fontSize = 13.sp,
                            fontWeight = if ((isHov3 || isAw3) && en3) FontWeight.Bold else FontWeight.Normal)
                    }
                    val isHov10 = hoveredSlot == 10; val isAw10 = awaitingConfirm && isHov10; val en10 = isSlotEnabled(10)
                    AnimatedCellBox(
                        isHovered = isHov10, isAwaitingConfirm = isAw10, enabled = en10,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.width(centerW).height(btnSize)
                    ) {
                        Text(
                            "X ±${"%.2f".format(xStep)}\nY ±${"%.2f".format(yStep)}",
                            color = slotTextColor(isHov10, isAw10, en10),
                            fontSize = 10.sp,
                            lineHeight = 11.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            fontWeight = if (isHov10 || isAw10) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    val isHov4 = hoveredSlot == 4; val isAw4 = awaitingConfirm && isHov4; val en4 = isSlotEnabled(4)
                    AnimatedCellBox(
                        isHovered = isHov4, isAwaitingConfirm = isAw4, enabled = en4,
                        shape = cellShape, modifier = Modifier.size(btnSize)
                    ) {
                        Text("X ▶", color = slotTextColor(isHov4, isAw4, en4), fontSize = 13.sp,
                            fontWeight = if ((isHov4 || isAw4) && en4) FontWeight.Bold else FontWeight.Normal)
                    }
                }
                // 하단 행: [50dp] [Y ▼ 36×36] [50dp]
                Row {
                    Spacer(Modifier.width(sideSpacerW).height(btnSize))
                    val isHov5 = hoveredSlot == 5; val isAw5 = awaitingConfirm && isHov5; val en5 = isSlotEnabled(5)
                    AnimatedCellBox(
                        isHovered = isHov5, isAwaitingConfirm = isAw5, enabled = en5,
                        shape = cellShape, modifier = Modifier.size(btnSize)
                    ) {
                        Text("Y ▼", color = slotTextColor(isHov5, isAw5, en5), fontSize = 13.sp,
                            fontWeight = if ((isHov5 || isAw5) && en5) FontWeight.Bold else FontWeight.Normal)
                    }
                    Spacer(Modifier.width(sideSpacerW).height(btnSize))
                }
            }
        }

        // ── CRUD: 노드 추가(7) / 노드 삭제(8) ───────────────────────────
        Row(
            Modifier.fillMaxWidth().height(48.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val isHov7 = hoveredSlot == 7; val isAw7 = awaitingConfirm && isHov7; val en7 = isSlotEnabled(7)
            AnimatedCellBox(
                isHovered = isHov7, isAwaitingConfirm = isAw7, enabled = en7,
                shape = cellShape,
                tintColor = ACCENT_BLUE,
                showAccentIdleBorder = true,
                modifier = Modifier.weight(1f).fillMaxHeight()
            ) {
                Text(
                    "+ 추가",
                    color = when {
                        (isHov7 || isAw7) && en7 -> Color.White
                        en7                       -> ACCENT_BLUE.copy(alpha = 0.8f)
                        else                      -> Color.White.copy(alpha = 0.22f)
                    },
                    fontSize = 12.sp,
                    fontWeight = if ((isHov7 || isAw7) && en7) FontWeight.Bold else FontWeight.Normal
                )
            }
            val isHov8 = hoveredSlot == 8; val isAw8 = awaitingConfirm && isHov8; val en8 = isSlotEnabled(8)
            AnimatedCellBox(
                isHovered = isHov8, isAwaitingConfirm = isAw8, enabled = en8,
                shape = cellShape,
                tintColor = ACCENT_RED,
                showAccentIdleBorder = true,
                modifier = Modifier.weight(1f).fillMaxHeight()
            ) {
                Text(
                    "− 삭제",
                    color = when {
                        (isHov8 || isAw8) && en8 -> Color.White
                        en8                       -> ACCENT_RED.copy(alpha = 0.8f)
                        else                      -> ACCENT_RED.copy(alpha = 0.25f)
                    },
                    fontSize = 12.sp,
                    fontWeight = if ((isHov8 || isAw8) && en8) FontWeight.Bold else FontWeight.Normal
                )
            }
        }

        } } // end AnimatedVisibility + inner Column
    }
}

/** 메타 카드: 아이콘(0) / 이름(1)+설명(2) / 템플릿(3) — 양 끝이 전체 높이 차지 */
@Composable
private fun MetaCard(
    slots: List<ActionSlot>,
    hoveredSlot: Int,
    awaitingConfirm: Boolean,
    selectedIconKey: String,
    name: String,
    description: String,
    editingTarget: String? = null,
    modifier: Modifier = Modifier
) {
    val caretAlpha by rememberInfiniteTransition(label = "metaCaret").animateFloat(
        initialValue = 1f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1000
                1f at 0
                1f at 499
                0f at 500
                0f at 999
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "caretAlpha"
    )
    val cardShape = RoundedCornerShape(10.dp)
    val cellShape = RoundedCornerShape(7.dp)
    val dividerColor = Color.White.copy(alpha = 0.09f)
    Row(
        modifier = modifier
            .height(56.dp)
            .background(SURFACE, cardShape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // 슬롯 0: 아이콘 (좌측, 전체 높이 정사각형)
        val hovIcon = hoveredSlot == 0
        val awIcon = awaitingConfirm && hovIcon
        val enIcon = slots.getOrNull(0)?.enabled ?: true
        Box(
            modifier = Modifier
                .aspectRatio(1f).fillMaxHeight()
                .background(cellBgColor(hovIcon, awIcon, enIcon), cellShape)
                .cellBorder(hovIcon, awIcon, enIcon, shape = cellShape),
            contentAlignment = Alignment.Center
        ) {
            val iconDef = customPresetIconOrNull(selectedIconKey)
            if (iconDef != null) {
                AppIcon(
                    def = iconDef, contentDescription = null,
                    tint = if (hovIcon || awIcon) ACCENT_BLUE else Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(26.dp)
                )
            } else {
                Text("+", color = LABEL_COLOR, fontSize = 36.sp, fontWeight = FontWeight.Light)
            }
        }

        Box(Modifier.width(1.dp).fillMaxHeight().background(dividerColor))

        // 중앙: 이름(1, 상단) + 설명(2, 하단)
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // 슬롯 1: 이름
            val hovName = hoveredSlot == 1
            val awName = awaitingConfirm && hovName
            val enName = slots.getOrNull(1)?.enabled ?: true
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth()
                    .background(cellBgColor(hovName, awName, enName), cellShape)
                    .cellBorder(hovName, awName, enName, shape = cellShape)
                    .padding(horizontal = 7.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val isEditingName = editingTarget == FIELD_NAME
                    Text(
                        text = if (isEditingName) name else name.ifBlank { "이름 없음" },
                        color = when {
                            (hovName || awName) && enName -> Color.White
                            name.isBlank() && !isEditingName -> LABEL_COLOR
                            else                          -> Color.White.copy(alpha = 0.85f)
                        },
                        fontSize = 12.sp,
                        fontWeight = if ((hovName || awName) && enName) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isEditingName) {
                        Text("|", color = Color.White.copy(alpha = caretAlpha), fontSize = 12.sp)
                    }
                }
            }

            Box(Modifier.fillMaxWidth().height(1.dp).background(dividerColor))

            // 슬롯 2: 설명
            val hovDesc = hoveredSlot == 2
            val awDesc = awaitingConfirm && hovDesc
            val enDesc = slots.getOrNull(2)?.enabled ?: true
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth()
                    .background(cellBgColor(hovDesc, awDesc, enDesc), cellShape)
                    .cellBorder(hovDesc, awDesc, enDesc, shape = cellShape)
                    .padding(horizontal = 7.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val isEditingDesc = editingTarget == FIELD_DESC
                    Text(
                        text = if (isEditingDesc) description
                               else if (description.isBlank()) "설명 추가하기..." else description,
                        color = when {
                            (hovDesc || awDesc) && enDesc -> Color.White
                            description.isBlank() && !isEditingDesc -> LABEL_COLOR.copy(alpha = 0.6f)
                            else                          -> Color.White.copy(alpha = 0.55f)
                        },
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isEditingDesc) {
                        Text("|", color = Color.White.copy(alpha = caretAlpha), fontSize = 11.sp)
                    }
                }
            }
        }

        Box(Modifier.width(1.dp).fillMaxHeight().background(dividerColor))

        // 슬롯 3: 템플릿 (우측, 전체 높이)
        val hovTmpl = hoveredSlot == 3
        val awTmpl = awaitingConfirm && hovTmpl
        val enTmpl = slots.getOrNull(3)?.enabled ?: true
        val tmplColor = when {
            (hovTmpl || awTmpl) && enTmpl -> ACCENT_BLUE
            else                           -> LABEL_COLOR
        }
        Box(
            modifier = Modifier.width(52.dp).fillMaxHeight()
                .background(cellBgColor(hovTmpl, awTmpl, enTmpl), cellShape)
                .cellBorder(hovTmpl, awTmpl, enTmpl, shape = cellShape),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("▤", color = tmplColor, fontSize = 18.sp)
                Text("템플릿", color = tmplColor, fontSize = 9.sp)
            }
        }
    }
}

/** 커브 카드: [가속(4)] [→복사(6)] [감속(5)] — 단일 행 세그먼트 */
@Composable
private fun CurveCard(
    slots: List<ActionSlot>,
    hoveredSlot: Int,
    awaitingConfirm: Boolean,
    activeTab: Int,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(10.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp)
            .background(SURFACE, cardShape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // 슬롯 4: 가속 (segment left)
        val hovAccel = hoveredSlot == 4
        val awAccel = awaitingConfirm && hovAccel
        val enAccel = slots.getOrNull(4)?.enabled ?: true
        val accelCurrent = activeTab == 0
        val accelShape = RoundedCornerShape(topStart = 7.dp, bottomStart = 7.dp)
        val accelBg = when {
            awAccel && enAccel  -> ACCENT_BLUE.copy(alpha = 0.55f)
            awAccel             -> Color.White.copy(alpha = 0.12f)
            hovAccel && enAccel -> ACCENT_BLUE.copy(alpha = 0.38f)
            hovAccel            -> Color.White.copy(alpha = 0.06f)
            accelCurrent        -> ACCENT_BLUE.copy(alpha = 0.20f)
            else                -> Color.Transparent
        }
        val accelBorder: Modifier = when {
            awAccel && enAccel  -> Modifier.border(2.dp, Color.White.copy(alpha = 0.9f), accelShape)
            hovAccel && enAccel -> Modifier.border(1.5.dp, ACCENT_BLUE, accelShape)
            accelCurrent        -> Modifier.border(1.dp, ACCENT_BLUE, accelShape)
            else                -> Modifier
        }
        Box(
            modifier = Modifier.weight(1f).fillMaxHeight()
                .background(accelBg, accelShape).then(accelBorder),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "가속",
                color = when {
                    (hovAccel || awAccel) && enAccel -> Color.White
                    accelCurrent                     -> ACCENT_BLUE
                    enAccel                          -> Color.White.copy(alpha = 0.7f)
                    else                             -> Color.White.copy(alpha = 0.3f)
                },
                fontSize = 12.sp,
                fontWeight = if (accelCurrent || (hovAccel && enAccel) || awAccel) FontWeight.Bold else FontWeight.Normal
            )
        }

        Box(Modifier.width(1.dp).fillMaxHeight().background(Color.White.copy(alpha = 0.09f)))

        // 슬롯 6: →복사 (segment middle, 좁은 폭)
        val hovCopy = hoveredSlot == 6
        val awCopy = awaitingConfirm && hovCopy
        val enCopy = slots.getOrNull(6)?.enabled ?: false
        val copyColor = when {
            (hovCopy || awCopy) && enCopy -> Color.White
            enCopy                        -> Color.White.copy(alpha = 0.55f)
            else                          -> Color.White.copy(alpha = 0.18f)
        }
        val copyShape = RoundedCornerShape(0.dp)
        val copyBg = when {
            awCopy && enCopy  -> ACCENT_BLUE.copy(alpha = 0.40f)
            awCopy            -> Color.White.copy(alpha = 0.08f)
            hovCopy && enCopy -> ACCENT_BLUE.copy(alpha = 0.22f)
            hovCopy           -> Color.White.copy(alpha = 0.04f)
            else              -> Color.Transparent
        }
        val copyBorder: Modifier = when {
            awCopy && enCopy  -> Modifier.border(2.dp, Color.White.copy(alpha = 0.9f), copyShape)
            hovCopy && enCopy -> Modifier.border(1.5.dp, ACCENT_BLUE, copyShape)
            else              -> Modifier
        }
        Box(
            modifier = Modifier.width(44.dp).fillMaxHeight()
                .background(copyBg, copyShape).then(copyBorder),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = null,
                    tint = copyColor,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "→",
                    color = copyColor,
                    fontSize = 10.sp
                )
            }
        }

        Box(Modifier.width(1.dp).fillMaxHeight().background(Color.White.copy(alpha = 0.09f)))

        // 슬롯 5: 감속 (segment right)
        val hovDecel = hoveredSlot == 5
        val awDecel = awaitingConfirm && hovDecel
        val enDecel = slots.getOrNull(5)?.enabled ?: true
        val decelCurrent = activeTab == 1
        val decelShape = RoundedCornerShape(topEnd = 7.dp, bottomEnd = 7.dp)
        val decelBg = when {
            awDecel && enDecel  -> ACCENT_ORANGE.copy(alpha = 0.55f)
            awDecel             -> Color.White.copy(alpha = 0.12f)
            hovDecel && enDecel -> ACCENT_ORANGE.copy(alpha = 0.38f)
            hovDecel            -> Color.White.copy(alpha = 0.06f)
            decelCurrent        -> ACCENT_ORANGE.copy(alpha = 0.20f)
            else                -> Color.Transparent
        }
        val decelBorder: Modifier = when {
            awDecel && enDecel  -> Modifier.border(2.dp, Color.White.copy(alpha = 0.9f), decelShape)
            hovDecel && enDecel -> Modifier.border(1.5.dp, ACCENT_ORANGE, decelShape)
            decelCurrent        -> Modifier.border(1.dp, ACCENT_ORANGE, decelShape)
            else                -> Modifier
        }
        Box(
            modifier = Modifier.weight(1f).fillMaxHeight()
                .background(decelBg, decelShape).then(decelBorder),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "감속",
                color = when {
                    (hovDecel || awDecel) && enDecel -> Color.White
                    decelCurrent                     -> ACCENT_ORANGE
                    enDecel                          -> Color.White.copy(alpha = 0.7f)
                    else                             -> Color.White.copy(alpha = 0.3f)
                },
                fontSize = 12.sp,
                fontWeight = if (decelCurrent || (hovDecel && enDecel) || awDecel) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

/** 액션 카드: 취소(7) / 저장(8) */
@Composable
private fun ActionCard(
    slots: List<ActionSlot>,
    hoveredSlot: Int,
    awaitingConfirm: Boolean
) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier.fillMaxWidth().height(46.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // 슬롯 7: 취소 (secondary)
        val hovCancel = hoveredSlot == 7
        val awCancel = awaitingConfirm && hovCancel
        val enCancel = slots.getOrNull(7)?.enabled ?: true
        val cancelBg = when {
            awCancel && enCancel  -> Color.White.copy(alpha = 0.18f)
            hovCancel && enCancel -> Color.White.copy(alpha = 0.10f)
            else                  -> Color.Transparent
        }
        val cancelBorder: Modifier = when {
            awCancel && enCancel  -> Modifier.border(2.dp, Color.White.copy(alpha = 0.9f), shape)
            hovCancel && enCancel -> Modifier.border(1.5.dp, Color.White.copy(alpha = 0.6f), shape)
            else                  -> Modifier.border(1.dp, Color.White.copy(alpha = 0.25f), shape)
        }
        Box(
            modifier = Modifier.weight(1f).fillMaxHeight()
                .background(cancelBg, shape).then(cancelBorder),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "취소",
                color = if ((hovCancel || awCancel) && enCancel) Color.White else Color.White.copy(alpha = 0.6f),
                fontSize = 13.sp,
                fontWeight = if (awCancel || (hovCancel && enCancel)) FontWeight.Bold else FontWeight.Normal
            )
        }

        // 슬롯 8: 저장 (primary)
        val hovSave = hoveredSlot == 8
        val awSave = awaitingConfirm && hovSave
        val enSave = slots.getOrNull(8)?.enabled ?: false
        val saveBg = when {
            awSave && enSave  -> ACCENT_BLUE
            hovSave && enSave -> ACCENT_BLUE.copy(alpha = 0.85f)
            enSave            -> ACCENT_BLUE.copy(alpha = 0.55f)
            else              -> SURFACE
        }
        val saveBorder: Modifier = when {
            awSave && enSave -> Modifier.border(2.dp, Color.White.copy(alpha = 0.9f), shape)
            !enSave          -> Modifier.border(1.dp, Color.White.copy(alpha = 0.12f), shape)
            else             -> Modifier
        }
        Box(
            modifier = Modifier.weight(2f).fillMaxHeight()
                .background(saveBg, shape).then(saveBorder),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "저장",
                color = if (enSave) Color.White else Color.White.copy(alpha = 0.25f),
                fontSize = 14.sp,
                fontWeight = if (enSave) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

/** 편집기 상단 헤더: 뒤로 가기(취소) / 저장 */
@Composable
private fun EditorHeader(
    nameValid: Boolean,
    hoveredSlot: Int,
    awaitingConfirm: Boolean,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 슬롯 7: 취소 (뒤로 가기)
        val hovCancel = hoveredSlot == 7
        val awCancel = awaitingConfirm && hovCancel
        val btnShape = RoundedCornerShape(8.dp)
        val cancelBg = when {
            awCancel  -> Color.White.copy(alpha = 0.18f)
            hovCancel -> Color.White.copy(alpha = 0.10f)
            else      -> Color.Transparent
        }
        val cancelBorderMod: Modifier = when {
            awCancel  -> Modifier.border(2.dp, Color.White.copy(alpha = 0.9f), btnShape)
            hovCancel -> Modifier.border(1.5.dp, Color.White.copy(alpha = 0.6f), btnShape)
            else      -> Modifier.border(1.dp, Color.White.copy(alpha = 0.25f), btnShape)
        }
        Box(
            modifier = Modifier
                .size(width = 60.dp, height = 36.dp)
                .clip(btnShape)
                .background(cancelBg, btnShape)
                .then(cancelBorderMod)
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = if (hovCancel) Color.White else Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(18.dp)
            )
        }

        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "커스텀 프리셋",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // 슬롯 8: 저장
        val hovSave = hoveredSlot == 8
        val awSave = awaitingConfirm && hovSave
        val saveBg = when {
            awSave && nameValid  -> ACCENT_BLUE
            hovSave && nameValid -> ACCENT_BLUE.copy(alpha = 0.85f)
            nameValid            -> ACCENT_BLUE.copy(alpha = 0.55f)
            else                 -> SURFACE
        }
        val saveBorderMod: Modifier = when {
            awSave && nameValid -> Modifier.border(2.dp, Color.White.copy(alpha = 0.9f), btnShape)
            !nameValid          -> Modifier.border(1.dp, Color.White.copy(alpha = 0.12f), btnShape)
            else                -> Modifier
        }
        Box(
            modifier = Modifier
                .size(width = 60.dp, height = 36.dp)
                .clip(btnShape)
                .background(saveBg, btnShape)
                .then(saveBorderMod)
                .clickable(enabled = nameValid, onClick = onSave),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "저장",
                color = if (nameValid) Color.White else Color.White.copy(alpha = 0.3f),
                fontSize = 14.sp,
                fontWeight = if (nameValid) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 아이콘 선택 컨텐츠 (그래프 영역 대체, 스와이프 방식)
// ─────────────────────────────────────────────────────────────

@Composable
private fun IconPickerContent(
    selectedIconKey: String,
    initialCell: IconCellPos,
    onCellChange: (IconCellPos) -> Unit,
    onClose: () -> Unit,
    onSelect: (String) -> Unit
) {
    val view = LocalView.current
    val layout = remember { buildIconLayout() }
    val currentCell = remember(selectedIconKey) { findInitialCell(selectedIconKey, layout) }
    var selectedCell by remember { mutableStateOf(initialCell) }
    var headerZone by remember { mutableStateOf<HeaderZone?>(null) }
    var awaitingConfirm by remember { mutableStateOf(false) }
    var gridWidthPx by remember { mutableIntStateOf(0) }
    var gridHeightPx by remember { mutableIntStateOf(0) }

    val hoveredLabel = when (headerZone) {
        HeaderZone.BACK -> "← 뒤로"
        HeaderZone.NONE -> "없음"
        null -> layout[selectedCell.row].cells[selectedCell.col].key
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(layout) {
                val tapThreshPx = 10.dp.toPx()
                awaitEachGesture {
                    val totalRows = layout.size
                    val rowH = if (gridHeightPx > 0) gridHeightPx.toFloat() / totalRows
                               else size.height.toFloat() / totalRows
                    val totalW = if (gridWidthPx > 0) gridWidthPx.toFloat() else size.width.toFloat()

                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startPos = down.position
                    val startCell = selectedCell
                    var moved = false

                    var ev = awaitPointerEvent()
                    while (ev.type != PointerEventType.Release) {
                        if (ev.type == PointerEventType.Move) {
                            ev.changes.forEach { it.consume() }
                            val pos = ev.changes.first().position
                            val dx = pos.x - startPos.x
                            val dy = pos.y - startPos.y
                            if (sqrt(dx * dx + dy * dy) > tapThreshPx) moved = true

                            val rowDelta = (dy / rowH).roundToInt()
                            val rawNewRow = startCell.row + rowDelta
                            val startFracX = computeFracX(startCell, layout)
                            val newFracX = (startFracX + dx / totalW).coerceIn(0f, 1f)

                            if (rawNewRow < 0) {
                                val newZone = if (newFracX < 0.35f) HeaderZone.BACK else HeaderZone.NONE
                                if (headerZone != newZone) {
                                    headerZone = newZone
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                }
                            } else {
                                val newRow = rawNewRow.coerceIn(0, totalRows - 1)
                                val newCol = findColAtFracX(newFracX, layout[newRow])
                                val resolved = resolveIconCell(IconCellPos(newRow, newCol), layout)
                                if (headerZone != null) {
                                    headerZone = null
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                }
                                if (resolved != selectedCell) {
                                    selectedCell = resolved
                                    onCellChange(resolved)
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                }
                            }
                        }
                        ev = awaitPointerEvent()
                    }

                    if (!moved && awaitingConfirm) {
                        when (headerZone) {
                            HeaderZone.BACK -> onClose()
                            HeaderZone.NONE -> onSelect("")
                            null -> onSelect(layout[selectedCell.row].cells[selectedCell.col].key)
                        }
                    } else {
                        awaitingConfirm = true
                    }
                }
            }
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 64.dp, bottom = 98.dp)
            .background(SURFACE, RoundedCornerShape(8.dp))
    ) {
        // ── 헤더: 뒤로 | 제목 | 선택 중 | 없음 chip ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val backActive = headerZone == HeaderZone.BACK
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        when {
                            backActive && awaitingConfirm -> ACCENT_BLUE.copy(alpha = 0.38f)
                            backActive -> ACCENT_BLUE.copy(alpha = 0.22f)
                            else -> Color.White.copy(alpha = 0.06f)
                        },
                        RoundedCornerShape(8.dp)
                    )
                    .then(when {
                        backActive && awaitingConfirm -> Modifier.border(2.dp, Color.White.copy(alpha = 0.9f), RoundedCornerShape(8.dp))
                        backActive -> Modifier.border(1.5.dp, ACCENT_BLUE, RoundedCornerShape(8.dp))
                        else -> Modifier
                    })
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            val up = awaitPointerEvent()
                            if (up.type == PointerEventType.Release) onClose()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = if (backActive) ACCENT_BLUE else Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "아이콘 선택",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = hoveredLabel,
                color = ACCENT_BLUE.copy(alpha = 0.8f),
                fontSize = 11.sp
            )
            Spacer(Modifier.width(8.dp))
            val noneIsActive = selectedIconKey.isEmpty()
            val noneHovered = headerZone == HeaderZone.NONE
            Box(
                modifier = Modifier
                    .background(
                        when {
                            noneHovered && awaitingConfirm -> ACCENT_BLUE.copy(alpha = 0.38f)
                            noneHovered -> ACCENT_BLUE.copy(alpha = 0.22f)
                            noneIsActive -> ACCENT_BLUE.copy(alpha = 0.15f)
                            else -> Color.White.copy(alpha = 0.07f)
                        },
                        RoundedCornerShape(10.dp)
                    )
                    .border(
                        when {
                            noneHovered && awaitingConfirm -> 2.dp
                            noneHovered -> 1.5.dp
                            noneIsActive -> 1.dp
                            else -> 1.dp
                        },
                        when {
                            noneHovered && awaitingConfirm -> Color.White.copy(alpha = 0.9f)
                            noneHovered -> ACCENT_BLUE
                            noneIsActive -> ACCENT_BLUE.copy(alpha = 0.6f)
                            else -> Color.White.copy(alpha = 0.18f)
                        },
                        RoundedCornerShape(10.dp)
                    )
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            val up = awaitPointerEvent()
                            if (up.type == PointerEventType.Release) onSelect("")
                        }
                    }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "없음",
                    color = when {
                        noneHovered -> ACCENT_BLUE
                        noneIsActive -> ACCENT_BLUE
                        else -> Color.White.copy(alpha = 0.6f)
                    },
                    fontSize = 11.sp,
                    fontWeight = if (noneHovered || noneIsActive) FontWeight.Bold else FontWeight.Normal
                )
            }
        }

        // ── 그리드 영역 ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .onSizeChanged { gridWidthPx = it.width; gridHeightPx = it.height }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                layout.forEachIndexed { rowIdx, row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        row.cells.forEachIndexed { colIdx, cell ->
                            val pos = IconCellPos(rowIdx, colIdx)
                            val isSelected = headerZone == null && selectedCell == pos
                            val isCurrent = currentCell == pos

                            val bgColor = when {
                                isSelected && awaitingConfirm -> ACCENT_BLUE.copy(alpha = 0.38f)
                                isSelected && isCurrent -> ACCENT_BLUE.copy(alpha = 0.30f)
                                isSelected -> ACCENT_BLUE.copy(alpha = 0.28f)
                                isCurrent -> ACCENT_BLUE.copy(alpha = 0.12f)
                                else -> Color.White.copy(alpha = 0.06f)
                            }
                            val borderMod: Modifier = when {
                                isSelected && awaitingConfirm -> Modifier.border(2.dp, Color.White.copy(alpha = 0.9f), RoundedCornerShape(6.dp))
                                isSelected && isCurrent -> Modifier.border(2.dp, ACCENT_BLUE, RoundedCornerShape(6.dp))
                                isSelected -> Modifier.border(1.5.dp, ACCENT_BLUE, RoundedCornerShape(6.dp))
                                isCurrent -> Modifier.border(1.dp, ACCENT_BLUE.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                else -> Modifier
                            }
                            val iconTint = if (isSelected || isCurrent) ACCENT_BLUE else Color.White

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(bgColor, RoundedCornerShape(6.dp))
                                    .then(borderMod),
                                contentAlignment = Alignment.Center
                            ) {
                                AppIcon(
                                    def = cell.def,
                                    contentDescription = cell.key,
                                    tint = iconTint,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── 하단 조작 안내 ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 3.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(horizontalArrangement = Arrangement.Center) {
                Text("↔ 드래그하여 선택", color = LABEL_COLOR, fontSize = 10.sp)
                Text("  |  ", color = LABEL_COLOR, fontSize = 10.sp)
                Text(
                    text = if (awaitingConfirm) "⊙ 탭하면 확정" else "⊙ 손을 떼면 선택",
                    color = if (awaitingConfirm) ACCENT_BLUE else LABEL_COLOR,
                    fontSize = 10.sp
                )
            }
            Text(
                text = "↑ 위로 스와이프하면 뒤로·없음 선택 가능",
                color = LABEL_COLOR,
                fontSize = 10.sp
            )
        }
    }
    } // end outer Box
}

// ─────────────────────────────────────────────────────────────
// 템플릿 선택 컨텐츠 (그래프 영역 대체, 정사각 카드 + 확정 단계)
// ─────────────────────────────────────────────────────────────

private enum class TemplatePhase { GRID, CONFIRM }

private data class TemplateAccent(
    val tint: Color,
    val bgIdle: Color,
    val bgSelected: Color
)

private val TEMPLATE_ACCENTS: Map<String, TemplateAccent> = mapOf(
    "template_balanced"  to TemplateAccent(Color(0xFF4F8EF7), Color(0xFF4F8EF7).copy(alpha = 0.18f), Color(0xFF4F8EF7).copy(alpha = 0.45f)),
    "template_precision" to TemplateAccent(Color(0xFF4CAF50), Color(0xFF4CAF50).copy(alpha = 0.18f), Color(0xFF4CAF50).copy(alpha = 0.45f)),
    "template_fast"      to TemplateAccent(Color(0xFFFF9800), Color(0xFFFF9800).copy(alpha = 0.18f), Color(0xFFFF9800).copy(alpha = 0.45f)),
    "template_stable"    to TemplateAccent(Color(0xFF26A69A), Color(0xFF26A69A).copy(alpha = 0.18f), Color(0xFF26A69A).copy(alpha = 0.45f))
)

private fun templateAccent(id: String): TemplateAccent =
    TEMPLATE_ACCENTS[id] ?: TemplateAccent(ACCENT_BLUE, ACCENT_BLUE.copy(alpha = 0.18f), ACCENT_BLUE.copy(alpha = 0.45f))

@Composable
private fun TemplatePickerContent(
    templates: List<CustomPointerDynamicsPreset>,
    initialSelectedIndex: Int,
    onIndexChange: (Int) -> Unit,
    onClose: () -> Unit,
    onSelect: (CustomPointerDynamicsPreset) -> Unit
) {
    val view = LocalView.current
    var phase by remember { mutableStateOf(TemplatePhase.GRID) }
    var selectedIndex by remember { mutableIntStateOf(initialSelectedIndex) }
    var confirmOptionIndex by remember { mutableIntStateOf(1) }

    val backHighlighted = phase == TemplatePhase.GRID && selectedIndex < 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(templates, phase, selectedIndex) {
                val tapThreshPx = 10.dp.toPx()
                when (phase) {
                    TemplatePhase.GRID -> {
                        val stepPx = TEMPLATE_PICKER_SWIPE_STEP_DP.dp.toPx()
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val startPos = down.position
                            val startIndex = selectedIndex
                            var moved = false

                            var ev = awaitPointerEvent()
                            while (ev.type != PointerEventType.Release) {
                                if (ev.type == PointerEventType.Move) {
                                    ev.changes.forEach { it.consume() }
                                    val pos = ev.changes.first().position
                                    val dx = pos.x - startPos.x
                                    val dy = pos.y - startPos.y
                                    if (sqrt(dx * dx + dy * dy) > tapThreshPx) moved = true

                                    val cols = 2
                                    val rows = (templates.size + cols - 1) / cols
                                    val startRow = if (startIndex < 0) -1 else startIndex / cols
                                    val startCol = if (startIndex < 0) 0 else startIndex % cols
                                    val rawNewRow = startRow + (dy / stepPx).roundToInt()
                                    val newRow = rawNewRow.coerceIn(-1, rows - 1)
                                    val newIndex = if (newRow < 0) {
                                        -1
                                    } else {
                                        val newCol = (startCol + (dx / stepPx).roundToInt()).coerceIn(0, cols - 1)
                                        newRow * cols + newCol
                                    }
                                    if (newIndex != selectedIndex) {
                                        selectedIndex = newIndex
                                        if (newIndex >= 0) onIndexChange(newIndex)
                                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    }
                                }
                                ev = awaitPointerEvent()
                            }

                            if (!moved) {
                                if (selectedIndex < 0) {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    onClose()
                                } else {
                                    phase = TemplatePhase.CONFIRM
                                    confirmOptionIndex = 1
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                }
                            }
                        }
                    }
                    TemplatePhase.CONFIRM -> {
                        val confirmStepPx = 30.dp.toPx()
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val startX = down.position.x
                            var lastX = startX
                            var accumDrag = 0f
                            var hasDragged = false

                            var ev = awaitPointerEvent()
                            while (ev.type != PointerEventType.Release) {
                                if (ev.type == PointerEventType.Move) {
                                    ev.changes.forEach { it.consume() }
                                    val pos = ev.changes.first().position
                                    if (abs(pos.x - startX) > tapThreshPx) hasDragged = true

                                    val dx = pos.x - lastX
                                    lastX = pos.x
                                    accumDrag += dx

                                    val steps = (accumDrag / confirmStepPx).toInt()
                                    if (steps != 0) {
                                        accumDrag -= steps * confirmStepPx
                                        val proposed = confirmOptionIndex + steps
                                        if (proposed in 0..1) {
                                            confirmOptionIndex = proposed
                                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                        } else {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                                view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                                            } else {
                                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                            }
                                            accumDrag = 0f
                                        }
                                    }
                                }
                                ev = awaitPointerEvent()
                            }

                            if (!hasDragged) {
                                when (confirmOptionIndex) {
                                    0 -> {
                                        phase = TemplatePhase.GRID
                                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    }
                                    1 -> {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                        } else {
                                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                        }
                                        onSelect(templates[selectedIndex])
                                    }
                                }
                            }
                        }
                    }
                }
            }
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 64.dp, bottom = 98.dp)
            .background(SURFACE, RoundedCornerShape(8.dp))
    ) {
        // ── 헤더 ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        if (backHighlighted) Color.White.copy(alpha = 0.18f)
                        else Color.White.copy(alpha = 0.06f),
                        RoundedCornerShape(8.dp)
                    )
                    .then(
                        if (backHighlighted) Modifier.border(1.5.dp, Color.White, RoundedCornerShape(8.dp))
                        else Modifier
                    )
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            val up = awaitPointerEvent()
                            if (up.type == PointerEventType.Release) {
                                if (phase == TemplatePhase.CONFIRM) phase = TemplatePhase.GRID
                                else onClose()
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "템플릿 선택",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        AnimatedContent(
            targetState = phase,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            transitionSpec = {
                if (targetState == TemplatePhase.CONFIRM) {
                    val origin = when (selectedIndex) {
                        0    -> TransformOrigin(0.25f, 0.25f)
                        1    -> TransformOrigin(0.75f, 0.25f)
                        2    -> TransformOrigin(0.25f, 0.75f)
                        else -> TransformOrigin(0.75f, 0.75f)
                    }
                    (fadeIn(tween(250)) + scaleIn(
                        initialScale = 0.3f,
                        transformOrigin = origin,
                        animationSpec = tween(300)
                    )) togetherWith (fadeOut(tween(180)) + scaleOut(
                        targetScale = 1.3f,
                        animationSpec = tween(200)
                    ))
                } else {
                    (fadeIn(tween(200)) + scaleIn(
                        initialScale = 0.88f,
                        animationSpec = tween(220)
                    )) togetherWith (fadeOut(tween(150)) + scaleOut(
                        targetScale = 0.88f,
                        animationSpec = tween(150)
                    ))
                }
            },
            label = "templatePhase"
        ) { currentPhase ->
            when (currentPhase) {
                TemplatePhase.GRID -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val cols = 2
                            Column(
                                modifier = Modifier.fillMaxWidth(0.72f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                templates.chunked(cols).forEachIndexed { rowIdx, rowTemplates ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        rowTemplates.forEachIndexed { colIdx, template ->
                                            val idx = rowIdx * cols + colIdx
                                            TemplateSquareCard(
                                                template = template,
                                                isSelected = selectedIndex == idx,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        repeat(cols - rowTemplates.size) {
                                            Spacer(Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                TemplatePhase.CONFIRM -> {
                    val template = templates[selectedIndex]
                    val accent = templateAccent(template.id)

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .background(accent.bgSelected, RoundedCornerShape(16.dp))
                                    .border(2.dp, accent.tint, RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                val iconDef = customPresetIconOrNull(template.iconKey)
                                if (iconDef != null) {
                                    AppIcon(
                                        def = iconDef,
                                        contentDescription = null,
                                        tint = accent.tint,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                            Text(
                                text = template.name,
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = template.description,
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(20.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(28.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "취소",
                                    color = if (confirmOptionIndex == 0) Color.White else Color.White.copy(alpha = 0.35f),
                                    fontSize = if (confirmOptionIndex == 0) 16.sp else 14.sp,
                                    fontWeight = if (confirmOptionIndex == 0) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    text = "확정",
                                    color = if (confirmOptionIndex == 1) accent.tint else accent.tint.copy(alpha = 0.35f),
                                    fontSize = if (confirmOptionIndex == 1) 16.sp else 14.sp,
                                    fontWeight = if (confirmOptionIndex == 1) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── 하단 안내 ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            when (phase) {
                TemplatePhase.GRID -> {
                    HintLine("↔", "좌우 스와이프로 카드 선택")
                    HintLine("↑", "맨 윗줄에서 위로 스와이프해 뒤로 가기 선택")
                    HintLine("⊙", "탭하면 확정 화면으로 이동")
                }
                TemplatePhase.CONFIRM -> {
                    HintLine("◀▶", "좌우 스와이프로 옵션 선택")
                    HintLine("⊙", "탭하면 현재 옵션 적용", accentColor = ACCENT_BLUE)
                }
            }
        }
    }
    } // end outer Box
}

@Composable
private fun HintLine(symbol: String, text: String, accentColor: Color? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = symbol,
            color = accentColor ?: LABEL_COLOR,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(22.dp)
        )
        Text(
            text = text,
            color = accentColor ?: LABEL_COLOR,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun TemplateSquareCard(
    template: CustomPointerDynamicsPreset,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val accent = templateAccent(template.id)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(
                    if (isSelected) accent.bgSelected else accent.bgIdle,
                    RoundedCornerShape(12.dp)
                )
                .then(
                    if (isSelected) Modifier.border(2.dp, accent.tint, RoundedCornerShape(12.dp))
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            val iconDef = customPresetIconOrNull(template.iconKey)
            if (iconDef != null) {
                AppIcon(
                    def = iconDef,
                    contentDescription = null,
                    tint = accent.tint,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = template.name,
            color = if (isSelected) Color.White else LABEL_COLOR,
            fontSize = 15.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center
        )
    }
}

// ─────────────────────────────────────────────────────────────
// 그래프 캔버스 (순수 렌더 전용, Phase 4.5.18.4: pointerInput 제거)
// ─────────────────────────────────────────────────────────────

@Composable
private fun CurveGraphCanvas(
    activeCurve: List<CurveNode>,
    inactiveCurve: List<CurveNode>,
    activeColor: Color,
    inactiveColor: Color,
    selectedNodeIndex: Int = -1,
    isSelectMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    val padLeft = with(density) { 36.dp.toPx() }
    val padBottom = with(density) { 32.dp.toPx() }
    val padTop = with(density) { 32.dp.toPx() }
    val padRight = with(density) { 36.dp.toPx() }

    fun plotWidth(w: Float) = w - padLeft - padRight
    fun plotHeight(h: Float) = h - padTop - padBottom

    fun velocityToX(v: Float, w: Float) =
        padLeft + (v / CurveEditorConstants.CURVE_VELOCITY_MAX) * plotWidth(w)

    val multRange = CurveEditorConstants.CURVE_MULTIPLIER_MAX - CurveEditorConstants.CURVE_MULTIPLIER_MIN

    fun multiplierToY(m: Float, h: Float) =
        padTop + (1f - (m - CurveEditorConstants.CURVE_MULTIPLIER_MIN) / multRange) * plotHeight(h)

    fun nodeCanvasOffset(node: CurveNode, w: Float, h: Float) =
        Offset(velocityToX(node.velocityDpMs, w), multiplierToY(node.multiplier, h))

    val nodeRadius = with(density) { 8.dp.toPx() }
    val selectedRadius = with(density) { if (isSelectMode) 14.dp.toPx() else 12.dp.toPx() }
    val ringStroke = with(density) { if (isSelectMode) 2.5.dp.toPx() else 1.5.dp.toPx() }
    val labelStyle = TextStyle(color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Medium)

    // C2: 선택 노드 반지름 pop-in 애니메이션 (selectedNodeIndex 변경 시 8dp → 12dp 부드럽게)
    val nodeRadiusAnim = remember(selectedNodeIndex) { Animatable(nodeRadius) }
    LaunchedEffect(selectedNodeIndex) {
        if (selectedNodeIndex >= 0) {
            nodeRadiusAnim.animateTo(selectedRadius, tween(180, easing = FastOutSlowInEasing))
        }
    }

    Canvas(
        modifier = modifier
            .background(SURFACE, RoundedCornerShape(8.dp))
            .border(1.5.dp, activeColor, RoundedCornerShape(8.dp))
    ) {
        val w = size.width
        val h = size.height

        drawGrid(w, h, padLeft, padRight, padTop, padBottom)

        drawCurve(inactiveCurve, w, h, inactiveColor.copy(alpha = 0.25f), ::velocityToX, ::multiplierToY)
        drawCurve(activeCurve, w, h, activeColor, ::velocityToX, ::multiplierToY)

        activeCurve.forEachIndexed { i, node ->
            val p = nodeCanvasOffset(node, w, h)
            val isFixed = i == 0 || i == activeCurve.lastIndex
            val isSelected = i == selectedNodeIndex
            val animRadius = if (isSelected) nodeRadiusAnim.value else nodeRadius

            // 채워진 원 + 속빈 내부 (고정=LABEL_COLOR, 가변=activeColor)
            drawCircle(color = if (isFixed) LABEL_COLOR else activeColor, radius = animRadius, center = p)
            drawCircle(color = BG, radius = animRadius - with(density) { 2.dp.toPx() }, center = p)
            // A5 + C2: 선택 노드 — 활성 탭 색 ring + 좌표 라벨
            if (isSelected) {
                drawCircle(
                    color = activeColor,
                    radius = animRadius,
                    center = p,
                    style = Stroke(width = ringStroke)
                )
                val label = "(%.1f, %.2f×)".format(node.velocityDpMs, node.multiplier)
                val measured = textMeasurer.measure(label, labelStyle)
                val labelX = (p.x - measured.size.width / 2f).coerceIn(0f, w - measured.size.width)
                val labelY = if (p.y - animRadius - measured.size.height - 4f >= padTop) {
                    p.y - animRadius - measured.size.height - 4f
                } else {
                    p.y + animRadius + 4f
                }
                drawText(measured, topLeft = Offset(labelX, labelY))
            }
        }

        drawAxisLabels(w, h, padLeft, padRight, padTop, padBottom, textMeasurer)
    }
}

private fun DrawScope.drawGrid(
    w: Float, h: Float,
    padL: Float, padR: Float, padT: Float, padB: Float
) {
    val plotW = w - padL - padR
    val plotH = h - padT - padB
    val gridCols = 6
    val gridRows = 5

    for (i in 0..gridCols) {
        val x = padL + i * plotW / gridCols
        drawLine(GRID_COLOR, Offset(x, padT), Offset(x, h - padB), strokeWidth = 1f)
    }
    for (i in 0..gridRows) {
        val y = padT + i * plotH / gridRows
        drawLine(GRID_COLOR, Offset(padL, y), Offset(w - padR, y), strokeWidth = 1f)
    }
}

private fun DrawScope.drawCurve(
    curve: List<CurveNode>,
    w: Float, h: Float,
    color: Color,
    velocityToX: (Float, Float) -> Float,
    multiplierToY: (Float, Float) -> Float
) {
    if (curve.size < 2) return
    val path = Path()
    curve.forEachIndexed { i, node ->
        val x = velocityToX(node.velocityDpMs, w)
        val y = multiplierToY(node.multiplier, h)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    drawPath(path, color, style = Stroke(width = 2.5f))
}

// ─────────────────────────────────────────────────────────────
// 저장 확인 오버레이 (Phase 4.5.18.7)
// ─────────────────────────────────────────────────────────────

@Composable
private fun SaveConfirmOverlay(
    curveDesc: CurveDescription,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    val view = LocalView.current
    var hoveredSlot by remember { mutableIntStateOf(1) }   // 1 = 저장 기본 호버
    var awaitingConfirm by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BG)
            .pointerInput(Unit) {
                val tapThreshPx = 10.dp.toPx()
                val stepPx = CurveEditorConstants.ACTION_GRID_SWIPE_STEP_DP.dp.toPx()
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startPos = down.position
                    var hasMoved = false

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
                                val newSlot = resolveSlot(dragDelta, stepPx, SAVE_CONFIRM_ROW_SLOTS, SAVE_CONFIRM_START_ROW)
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
                    } else if (awaitingConfirm) {
                        when (hoveredSlot) {
                            0 -> {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                else view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                onBack()
                            }
                            1 -> {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                else view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                onSave()
                            }
                        }
                    }
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 헤더
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SURFACE),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "저장 확인",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(12.dp))

            // 자연어 요약 카드
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SURFACE)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = curveDesc.summary,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 26.sp
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.08f))
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "저속 (0.5 dp/ms): ×${"%.2f".format(curveDesc.lowSpeedMultiplier)}",
                        color = LABEL_COLOR, fontSize = 12.sp
                    )
                    Text(
                        text = "중속 (3.0 dp/ms): ×${"%.2f".format(curveDesc.midSpeedMultiplier)}",
                        color = LABEL_COLOR, fontSize = 12.sp
                    )
                    Text(
                        text = "고속 (6.0 dp/ms): ×${"%.2f".format(curveDesc.highSpeedMultiplier)}",
                        color = LABEL_COLOR, fontSize = 12.sp
                    )
                    Text(
                        text = "가/감속 비대칭: ${curveDesc.asymmetryLabel}",
                        color = LABEL_COLOR, fontSize = 12.sp
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // 슬롯 행: [편집 계속] [이대로 저장]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 슬롯 0: 편집 계속
                val slot0Hovered = hoveredSlot == 0
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(10.dp))
                        .background(cellBgColor(slot0Hovered, awaitingConfirm && slot0Hovered, true))
                        .then(
                            if (slot0Hovered && !awaitingConfirm)
                                Modifier.border(1.dp, ACCENT_BLUE.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                            else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "편집 계속",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                // 슬롯 1: 이대로 저장
                val slot1Hovered = hoveredSlot == 1
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(10.dp))
                        .background(cellBgColor(slot1Hovered, awaitingConfirm && slot1Hovered, true))
                        .border(
                            1.dp,
                            if (slot1Hovered) ACCENT_BLUE.copy(alpha = if (awaitingConfirm) 0.55f else 0.9f)
                            else ACCENT_BLUE.copy(alpha = 0.25f),
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "이대로 저장",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 스텝 정밀도 피커 오버레이 (Phase 4.5.18.6)
// ─────────────────────────────────────────────────────────────

@Composable
private fun NodeStepScalePickerOverlay(
    currentIndex: Int,
    onConfirm: (Int) -> Unit,
) {
    val view = LocalView.current
    val maxIndex = CurveEditorConstants.NODE_STEP_SCALES.size - 1

    var hoveredIndex by remember { mutableIntStateOf(currentIndex) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .pointerInput(Unit) {
                val tapThreshPx = 10.dp.toPx()
                val stepPx = CurveEditorConstants.NODE_STEP_PICKER_SWIPE_STEP_DP.dp.toPx()
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startPos = down.position
                    val startHoveredIndex = hoveredIndex
                    var hasMoved = false

                    var ev = awaitPointerEvent()
                    while (ev.type != PointerEventType.Release) {
                        if (ev.type == PointerEventType.Move) {
                            ev.changes.forEach { it.consume() }
                            val pos = ev.changes.first().position
                            val dx = pos.x - startPos.x
                            val dy = pos.y - startPos.y
                            if (!hasMoved && sqrt(dx * dx + dy * dy) > tapThreshPx) hasMoved = true
                            if (hasMoved) {
                                val newIdx = (startHoveredIndex + (dx / stepPx).roundToInt())
                                    .coerceIn(0, maxIndex)
                                if (newIdx != hoveredIndex) {
                                    hoveredIndex = newIdx
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                }
                            }
                        }
                        ev = awaitPointerEvent()
                    }

                    if (!hasMoved) {
                        // 탭 → 확정 (현재 적용값과 동일하면 취소와 같음)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        else
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        onConfirm(hoveredIndex)
                    }
                    // 가로 스와이프 → 선택만 이동, 피커 유지
                }
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "스텝 정밀도",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(24.dp))

            // B3: 5칸 가로 스와이프 피커 — 상단 의미 라벨(NODE_STEP_SCALE_LABELS) + 하단 절대 수치
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CurveEditorConstants.NODE_STEP_SCALES.forEachIndexed { idx, scale ->
                    val isHovered = idx == hoveredIndex
                    val isCurrent = idx == currentIndex
                    val xVal = CurveEditorConstants.CURVE_STEP_VELOCITY * scale
                    val yVal = CurveEditorConstants.CURVE_STEP_MULTIPLIER * scale
                    val label = CurveEditorConstants.NODE_STEP_SCALE_LABELS.getOrElse(idx) { "" }
                    Box(
                        modifier = Modifier
                            .width(CurveEditorConstants.NODE_STEP_PICKER_SWIPE_STEP_DP.dp)
                            .height(88.dp)
                            .background(
                                if (isHovered) ACCENT_BLUE.copy(alpha = 0.55f)
                                else Color.White.copy(alpha = 0.08f),
                                RoundedCornerShape(8.dp)
                            )
                            .then(when {
                                isHovered -> Modifier.border(1.dp, ACCENT_BLUE, RoundedCornerShape(8.dp))
                                isCurrent -> Modifier.border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                else -> Modifier
                            }),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // 의미 라벨 (상단, Bold)
                            Text(
                                text = label,
                                color = if (isHovered) Color.White else Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                fontWeight = if (isHovered || isCurrent) FontWeight.Bold else FontWeight.Normal
                            )
                            // 절대 수치 (하단, 보조)
                            Text(
                                text = "X ±${"%.2f".format(xVal)}",
                                color = if (isHovered) Color.White.copy(alpha = 0.85f) else LABEL_COLOR,
                                fontSize = 9.sp
                            )
                            Text(
                                text = "Y ±${"%.2f".format(yVal)}",
                                color = if (isHovered) Color.White.copy(alpha = 0.85f) else LABEL_COLOR,
                                fontSize = 9.sp
                            )
                        }
                    }
                    if (idx < maxIndex) Spacer(Modifier.width(3.dp))
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = "탭하여 확정  /  현재 적용값 선택 시 취소",
                color = LABEL_COLOR,
                fontSize = 11.sp,
            )
        }
    }
}

private fun DrawScope.drawAxisLabels(
    w: Float, h: Float,
    padL: Float, padR: Float, padT: Float, padB: Float,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {
    val plotW = w - padL - padR
    val plotH = h - padT - padB
    val style = TextStyle(color = LABEL_COLOR, fontSize = 9.sp)

    for (i in 0..6) {
        val x = padL + i * plotW / 6
        val measured = textMeasurer.measure("$i", style)
        drawText(measured, topLeft = Offset(x - measured.size.width / 2f, h - padB + 4f))
    }

    val multRange = CurveEditorConstants.CURVE_MULTIPLIER_MAX - CurveEditorConstants.CURVE_MULTIPLIER_MIN
    for (i in 0..6) {
        val m = CurveEditorConstants.CURVE_MULTIPLIER_MIN + i * multRange / 6
        val y = padT + (1f - i / 6f) * plotH
        val label = "%.0f×".format(m)
        val measured = textMeasurer.measure(label, style)
        drawText(measured, topLeft = Offset(18f, y - measured.size.height / 2f))
    }
}
