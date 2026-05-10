package com.bridgeone.app.ui.components.touchpad

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import com.bridgeone.app.ui.common.AppIcon
import com.bridgeone.app.ui.common.CUSTOM_PRESET_ICON_OPTIONS
import com.bridgeone.app.ui.common.CUSTOM_PRESET_TEMPLATES
import com.bridgeone.app.ui.common.CurveEditorConstants
import com.bridgeone.app.ui.common.CurveNode
import com.bridgeone.app.ui.common.CustomPointerDynamicsPreset
import com.bridgeone.app.ui.common.customPresetIconOrNull
import com.bridgeone.app.ui.common.defaultAccelerationCurve
import com.bridgeone.app.ui.common.defaultDecelerationCurve
import java.util.UUID
import kotlin.math.abs
import kotlin.math.sqrt

private val BG = Color(0xFF0D0D0D)
private val SURFACE = Color(0xFF1A1A1A)
private val ACCENT_BLUE = Color(0xFF4F8EF7)
private val ACCENT_ORANGE = Color(0xFFFF9800)
private val GRID_COLOR = Color(0xFF2A2A2A)
private val LABEL_COLOR = Color(0xFF888888)

private const val NAME_MAX_LEN = 12
private const val DESC_MAX_LEN = 50

/**
 * 커스텀 포인터 다이나믹스 프리셋 그래프 편집기 (Phase 4.5.16)
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
    var deleteTargetIndex by remember { mutableIntStateOf(-1) }
    var showTemplatePicker by remember { mutableStateOf(false) }
    var showIconPicker by remember { mutableStateOf(false) }

    val activeCurve = if (activeTab == 0) accelCurve else decelCurve
    fun setActiveCurve(c: List<CurveNode>) {
        if (activeTab == 0) accelCurve = c else decelCurve = c
    }

    // 이름 유효성 (비어있거나, 다른 프리셋과 이름 중복 시 무효)
    val isDuplicate = name.isNotBlank() && existingPresets.any { it.name == name && it.id != (initialPreset?.id ?: "") }
    val nameValid = name.isNotBlank() && !isDuplicate

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BG)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── 상단 바 ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "취소", tint = Color.White)
                }
                Text(
                    text = "커스텀 프리셋 편집",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { showTemplatePicker = true }) {
                    Text("템플릿", color = ACCENT_BLUE, fontSize = 13.sp)
                }
                IconButton(
                    onClick = {
                        if (!nameValid) return@IconButton
                        val id = initialPreset?.id ?: UUID.randomUUID().toString()
                        onSave(CustomPointerDynamicsPreset(id, name, accelCurve, decelCurve, description = description, iconKey = selectedIconKey))
                    },
                    enabled = nameValid
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = "저장",
                        tint = if (nameValid) ACCENT_BLUE else Color.Gray
                    )
                }
            }

            // ── 이름 입력 ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .background(SURFACE, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("이름:", color = LABEL_COLOR, fontSize = 13.sp)
                Spacer(Modifier.width(8.dp))
                BasicTextField(
                    value = name,
                    onValueChange = { if (it.length <= NAME_MAX_LEN) name = it },
                    textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                if (name.length >= NAME_MAX_LEN) {
                    Text("${name.length}/$NAME_MAX_LEN", color = LABEL_COLOR, fontSize = 11.sp)
                }
            }
            if (isDuplicate) {
                Text(
                    text = "이미 같은 이름의 프리셋이 있습니다.",
                    color = Color(0xFFFF5252),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )
            }

            Spacer(Modifier.height(6.dp))

            // ── 설명 입력 ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .background(SURFACE, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text("설명:", color = LABEL_COLOR, fontSize = 13.sp, modifier = Modifier.padding(top = 1.dp))
                Spacer(Modifier.width(8.dp))
                BasicTextField(
                    value = description,
                    onValueChange = { if (it.length <= DESC_MAX_LEN) description = it },
                    textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                    modifier = Modifier.weight(1f),
                    maxLines = 2
                )
                if (description.length >= DESC_MAX_LEN - 10) {
                    Text("${description.length}/$DESC_MAX_LEN", color = LABEL_COLOR, fontSize = 11.sp, modifier = Modifier.padding(top = 1.dp))
                }
            }

            Spacer(Modifier.height(6.dp))

            // ── 아이콘 선택 ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .background(SURFACE, RoundedCornerShape(8.dp))
                    .clickable { showIconPicker = true }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("아이콘:", color = LABEL_COLOR, fontSize = 13.sp)
                val iconDef = customPresetIconOrNull(selectedIconKey)
                if (iconDef != null) {
                    AppIcon(def = iconDef, contentDescription = null, tint = ACCENT_BLUE, modifier = Modifier.size(22.dp))
                } else {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("AB", color = LABEL_COLOR, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Text(
                    text = if (selectedIconKey.isEmpty()) "없음 (이름 2자 표시)" else CUSTOM_PRESET_ICON_OPTIONS.firstOrNull { it.first == selectedIconKey }?.first ?: "",
                    color = if (selectedIconKey.isEmpty()) LABEL_COLOR else Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
                Text("변경 ›", color = ACCENT_BLUE, fontSize = 12.sp)
            }

            Spacer(Modifier.height(6.dp))

            // ── 탭 전환 ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TabButton(
                    label = "가속 곡선",
                    color = ACCENT_BLUE,
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    modifier = Modifier.weight(1f)
                )
                TabButton(
                    label = "감속 곡선",
                    color = ACCENT_ORANGE,
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    modifier = Modifier.weight(1f)
                )
            }

            // 감속 탭에서만 "가속 곡선 복사" 버튼
            if (activeTab == 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { decelCurve = accelCurve.toList() }) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = ACCENT_BLUE, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("가속 곡선 복사", color = ACCENT_BLUE, fontSize = 12.sp)
                    }
                }
            } else {
                Spacer(Modifier.height(4.dp))
            }

            // ── 그래프 캔버스 ──
            CurveGraphCanvas(
                activeCurve = activeCurve,
                inactiveCurve = if (activeTab == 0) decelCurve else accelCurve,
                activeColor = if (activeTab == 0) ACCENT_BLUE else ACCENT_ORANGE,
                inactiveColor = if (activeTab == 0) ACCENT_ORANGE else ACCENT_BLUE,
                onCurveChanged = { setActiveCurve(it) },
                onDeleteRequest = { idx -> deleteTargetIndex = idx },
                onHaptic = { constant ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        view.performHapticFeedback(constant)
                    } else {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            )

            // ── 조작 안내 ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .background(SURFACE, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Text("빈 곳 탭 = 노드 추가", color = LABEL_COLOR, fontSize = 11.sp)
                Text("|", color = LABEL_COLOR, fontSize = 11.sp)
                Text("노드 롱프레스 = 삭제", color = LABEL_COLOR, fontSize = 11.sp)
            }
        }
    }

    // ── 아이콘 선택 다이얼로그 ──
    if (showIconPicker) {
        AlertDialog(
            onDismissRequest = { showIconPicker = false },
            title = { Text("아이콘 선택", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // "없음" 옵션
                    TextButton(
                        onClick = { selectedIconKey = ""; showIconPicker = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier.size(28.dp).background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp)),
                                contentAlignment = Alignment.Center
                            ) { Text("AB", color = LABEL_COLOR, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
                            Text("없음 (이름 2자 표시)", color = if (selectedIconKey.isEmpty()) ACCENT_BLUE else LABEL_COLOR, fontSize = 13.sp)
                        }
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().height(280.dp)
                    ) {
                        items(CUSTOM_PRESET_ICON_OPTIONS) { (key, iconDef) ->
                            val isSelected = selectedIconKey == key
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        if (isSelected) ACCENT_BLUE.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.06f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .then(if (isSelected) Modifier.border(1.5.dp, ACCENT_BLUE, RoundedCornerShape(8.dp)) else Modifier)
                                    .clickable { selectedIconKey = key; showIconPicker = false },
                                contentAlignment = Alignment.Center
                            ) {
                                AppIcon(def = iconDef, contentDescription = key, tint = if (isSelected) ACCENT_BLUE else Color.White, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showIconPicker = false }) {
                    Text("취소", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF222222)
        )
    }

    // ── 템플릿 선택 다이얼로그 ──
    if (showTemplatePicker) {
        AlertDialog(
            onDismissRequest = { showTemplatePicker = false },
            title = { Text("템플릿 불러오기", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "가속·감속 곡선이 모두 교체됩니다.",
                        color = LABEL_COLOR,
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    CUSTOM_PRESET_TEMPLATES.forEach { template ->
                        TextButton(
                            onClick = {
                                accelCurve = template.accelerationCurve
                                decelCurve = template.decelerationCurve
                                showTemplatePicker = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(template.name, color = ACCENT_BLUE, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                if (template.description.isNotEmpty()) {
                                    Text(template.description, color = LABEL_COLOR, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showTemplatePicker = false }) {
                    Text("취소", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF222222)
        )
    }

    // ── 노드 삭제 확인 다이얼로그 ──
    if (deleteTargetIndex >= 0) {
        AlertDialog(
            onDismissRequest = { deleteTargetIndex = -1 },
            title = { Text("노드 삭제", color = Color.White) },
            text = { Text("이 노드를 삭제하시겠습니까?", color = LABEL_COLOR) },
            confirmButton = {
                TextButton(onClick = {
                    val mutable = activeCurve.toMutableList()
                    mutable.removeAt(deleteTargetIndex)
                    setActiveCurve(mutable)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    }
                    deleteTargetIndex = -1
                }) { Text("삭제", color = Color(0xFFFF5252)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTargetIndex = -1 }) {
                    Text("취소", color = Color.White)
                }
            },
            containerColor = Color(0xFF222222)
        )
    }
}

// ─────────────────────────────────────────────────────────────
// 탭 버튼
// ─────────────────────────────────────────────────────────────

@Composable
private fun TabButton(
    label: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                if (selected) color.copy(alpha = 0.18f) else SURFACE,
                RoundedCornerShape(8.dp)
            )
            .then(if (selected) Modifier.border(1.dp, color, RoundedCornerShape(8.dp)) else Modifier)
            .padding(vertical = 6.dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    val up = awaitPointerEvent()
                    if (up.type == PointerEventType.Release) onClick()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) color else LABEL_COLOR,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// ─────────────────────────────────────────────────────────────
// 그래프 캔버스 (Canvas + pointerInput)
// ─────────────────────────────────────────────────────────────

@Composable
private fun CurveGraphCanvas(
    activeCurve: List<CurveNode>,
    inactiveCurve: List<CurveNode>,
    activeColor: Color,
    inactiveColor: Color,
    onCurveChanged: (List<CurveNode>) -> Unit,
    onDeleteRequest: (Int) -> Unit,
    onHaptic: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val snapThresholdPx = with(density) { CurveEditorConstants.CURVE_SNAP_THRESHOLD_DP.dp.toPx() }
    val addMinPx = with(density) { CurveEditorConstants.CURVE_ADD_MIN_DP.dp.toPx() }
    val nodeDragRadius = with(density) { 8.dp.toPx() }       // 일반 노드 반지름
    val nodeDragRadiusActive = with(density) { 12.dp.toPx() } // 드래그 중 확대 반지름
    val textMeasurer = rememberTextMeasurer()

    // 드래그 상태
    var draggingIndex by remember { mutableIntStateOf(-1) }
    var canvasWidth by remember { mutableFloatStateOf(1f) }
    var canvasHeight by remember { mutableFloatStateOf(1f) }

    // 좌표 변환 헬퍼 (내부 padding 포함)
    val padLeft = with(density) { 32.dp.toPx() }
    val padBottom = with(density) { 24.dp.toPx() }
    val padTop = with(density) { 12.dp.toPx() }
    val padRight = with(density) { 12.dp.toPx() }

    fun plotWidth(w: Float) = w - padLeft - padRight
    fun plotHeight(h: Float) = h - padTop - padBottom

    fun velocityToX(v: Float, w: Float) =
        padLeft + (v / CurveEditorConstants.CURVE_VELOCITY_MAX) * plotWidth(w)

    val multRange = CurveEditorConstants.CURVE_MULTIPLIER_MAX - CurveEditorConstants.CURVE_MULTIPLIER_MIN

    fun multiplierToY(m: Float, h: Float) =
        padTop + (1f - (m - CurveEditorConstants.CURVE_MULTIPLIER_MIN) / multRange) * plotHeight(h)

    fun xToVelocity(x: Float, w: Float) =
        ((x - padLeft) / plotWidth(w) * CurveEditorConstants.CURVE_VELOCITY_MAX)
            .coerceIn(0f, CurveEditorConstants.CURVE_VELOCITY_MAX)

    fun yToMultiplier(y: Float, h: Float) =
        (CurveEditorConstants.CURVE_MULTIPLIER_MIN + (1f - (y - padTop) / plotHeight(h)) * multRange)
            .coerceIn(CurveEditorConstants.CURVE_MULTIPLIER_MIN, CurveEditorConstants.CURVE_MULTIPLIER_MAX)

    fun nodeCanvasOffset(node: CurveNode, w: Float, h: Float) =
        Offset(velocityToX(node.velocityDpMs, w), multiplierToY(node.multiplier, h))

    fun findNearestNode(pos: Offset, curve: List<CurveNode>, w: Float, h: Float): Int {
        var bestIdx = -1
        var bestDist = snapThresholdPx
        curve.forEachIndexed { i, node ->
            val p = nodeCanvasOffset(node, w, h)
            val dist = sqrt((pos.x - p.x).let { it * it } + (pos.y - p.y).let { it * it })
            if (dist < bestDist) { bestDist = dist; bestIdx = i }
        }
        return bestIdx
    }

    Canvas(
        modifier = modifier
            .background(SURFACE, RoundedCornerShape(8.dp))
            .pointerInput(activeCurve) {
                val w = size.width.toFloat()
                val h = size.height.toFloat()

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val pos = down.position
                    val nearIdx = findNearestNode(pos, activeCurve, w, h)

                    if (nearIdx >= 0) {
                        // 기존 노드 근처: 드래그 또는 롱프레스
                        draggingIndex = nearIdx
                        var longPressTriggered = false
                        var movedSinceDown = false

                        // 롱프레스 임계값: 500ms
                        val longPressThresholdMs = 500L
                        val downTime = System.currentTimeMillis()

                        var ev = awaitPointerEvent()
                        while (ev.type != PointerEventType.Release) {
                            if (ev.type == PointerEventType.Move) {
                                ev.changes.forEach { it.consume() }
                                val newPos = ev.changes.first().position
                                val dist = sqrt((newPos.x - pos.x).let { it * it } + (newPos.y - pos.y).let { it * it })
                                if (dist > 4f) movedSinceDown = true

                                if (movedSinceDown && !longPressTriggered) {
                                    // 드래그: 양 끝 노드는 X 이동 불가
                                    val isFixed = nearIdx == 0 || nearIdx == activeCurve.lastIndex
                                    val newV = if (isFixed) activeCurve[nearIdx].velocityDpMs
                                    else {
                                        val rawV = xToVelocity(newPos.x, w)
                                        val prevV = activeCurve.getOrNull(nearIdx - 1)?.velocityDpMs ?: 0f
                                        val nextV = activeCurve.getOrNull(nearIdx + 1)?.velocityDpMs
                                            ?: CurveEditorConstants.CURVE_VELOCITY_MAX
                                        rawV.coerceIn(
                                            prevV + CurveEditorConstants.CURVE_MIN_VELOCITY_GAP,
                                            nextV - CurveEditorConstants.CURVE_MIN_VELOCITY_GAP
                                        )
                                    }
                                    val newM = yToMultiplier(newPos.y, h)
                                    val mutable = activeCurve.toMutableList()
                                    mutable[nearIdx] = CurveNode(newV, newM)
                                    onCurveChanged(mutable)
                                }
                            }

                            // 롱프레스 판정 (고정 노드 제외)
                            val elapsed = System.currentTimeMillis() - downTime
                            val isFixed = nearIdx == 0 || nearIdx == activeCurve.lastIndex
                            if (!movedSinceDown && !isFixed && elapsed >= longPressThresholdMs && !longPressTriggered) {
                                longPressTriggered = true
                                onHaptic(HapticFeedbackConstants.LONG_PRESS)
                                onDeleteRequest(nearIdx)
                            }

                            ev = awaitPointerEvent()
                        }
                        draggingIndex = -1
                    } else {
                        // 빈 곳 탭: 노드 추가
                        var hasMoved = false
                        var ev = awaitPointerEvent()
                        while (ev.type != PointerEventType.Release) {
                            if (ev.type == PointerEventType.Move) {
                                ev.changes.forEach { it.consume() }
                                val newPos = ev.changes.first().position
                                val dist = sqrt((newPos.x - pos.x).let { it * it } + (newPos.y - pos.y).let { it * it })
                                if (dist > 4f) hasMoved = true
                            }
                            ev = awaitPointerEvent()
                        }

                        if (!hasMoved) {
                            // 탭: 최대 노드 수 미만이고 인접 노드와 충분한 거리 시 추가
                            if (activeCurve.size < CurveEditorConstants.CURVE_MAX_NODES) {
                                val newV = xToVelocity(pos.x, w)
                                val newM = yToMultiplier(pos.y, h)
                                val tooClose = activeCurve.any { node ->
                                    abs(velocityToX(node.velocityDpMs, w) - pos.x) < addMinPx
                                }
                                if (!tooClose) {
                                    val mutable = (activeCurve + CurveNode(newV, newM))
                                        .sortedBy { it.velocityDpMs }
                                    onCurveChanged(mutable)
                                    onHaptic(HapticFeedbackConstants.CLOCK_TICK)
                                }
                            }
                        }
                    }
                }
            }
    ) {
        canvasWidth = size.width
        canvasHeight = size.height
        val w = size.width
        val h = size.height

        // ── 격자 ──
        drawGrid(w, h, padLeft, padRight, padTop, padBottom)

        // ── 비활성 곡선 (흐리게) ──
        drawCurve(inactiveCurve, w, h, inactiveColor.copy(alpha = 0.25f),
            ::velocityToX, ::multiplierToY)

        // ── 활성 곡선 ──
        drawCurve(activeCurve, w, h, activeColor,
            ::velocityToX, ::multiplierToY)

        // ── 노드 원 ──
        activeCurve.forEachIndexed { i, node ->
            val p = nodeCanvasOffset(node, w, h)
            val r = if (i == draggingIndex) nodeDragRadiusActive else nodeDragRadius
            val isFixed = i == 0 || i == activeCurve.lastIndex
            drawCircle(
                color = if (isFixed) LABEL_COLOR else activeColor,
                radius = r,
                center = p
            )
            drawCircle(
                color = BG,
                radius = r - with(density) { 2.dp.toPx() },
                center = p
            )
        }

        // ── 축 레이블 ──
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

    // 수직선
    for (i in 0..gridCols) {
        val x = padL + i * plotW / gridCols
        drawLine(GRID_COLOR, Offset(x, padT), Offset(x, h - padB), strokeWidth = 1f)
    }
    // 수평선
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

private fun DrawScope.drawAxisLabels(
    w: Float, h: Float,
    padL: Float, padR: Float, padT: Float, padB: Float,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {
    val plotW = w - padL - padR
    val plotH = h - padT - padB
    val style = TextStyle(color = LABEL_COLOR, fontSize = 9.sp)

    // X축 (속도 dp/ms): 0, 1, 2, 3, 4, 5, 6
    for (i in 0..6) {
        val x = padL + i * plotW / 6
        val measured = textMeasurer.measure("$i", style)
        drawText(measured, topLeft = Offset(x - measured.size.width / 2f, h - padB + 4f))
    }

    // Y축 (배율 ×): 0×, 1×, 2×, 3×, 4×, 5×, 6×
    val multRange = CurveEditorConstants.CURVE_MULTIPLIER_MAX - CurveEditorConstants.CURVE_MULTIPLIER_MIN
    for (i in 0..6) {
        val m = CurveEditorConstants.CURVE_MULTIPLIER_MIN + i * multRange / 6
        val y = padT + (1f - i / 6f) * plotH
        val label = "%.0f×".format(m)
        val measured = textMeasurer.measure(label, style)
        drawText(measured, topLeft = Offset(2f, y - measured.size.height / 2f))
    }
}
