package com.bridgeone.app.ui.components.touchpad

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bridgeone.app.ui.common.AppIcon
import com.bridgeone.app.ui.common.CurveEditorConstants.TEMPLATE_PICKER_SWIPE_STEP_DP
import com.bridgeone.app.ui.common.CustomPointerDynamicsPreset
import com.bridgeone.app.ui.common.customPresetIconOrNull
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

// ─────────────────────────────────────────────────────────────
// 템플릿 선택 컨텐츠 (그래프 영역 대체, 정사각 카드 + 확정 단계)
// (Phase 4.7.6-C: DynamicsCurveEditor.kt에서 분리)
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
internal fun TemplatePickerContent(
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
