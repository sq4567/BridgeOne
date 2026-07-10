package com.bridgeone.app.ui.components.touchpad

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.bridgeone.app.ui.common.InputMode
import com.bridgeone.app.ui.common.swipe.LocalSwipeFocusController
import com.bridgeone.app.ui.common.swipe.SwipeFocusable
import com.bridgeone.app.ui.common.swipe.ROOT_SCOPE
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bridgeone.app.ui.common.EdgeSwipeConstants
import kotlin.math.abs
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment

private val STRIP_ZONE_COLORS = listOf(
    Color(0xFF1E3A5F), Color(0xFF3A1E5F), Color(0xFF1E5F3A),
    Color(0xFF5F3A1E), Color(0xFF3A5F1E), Color(0xFF1E5F5F)
)

private fun stripZoneColor(index: Int): Color = STRIP_ZONE_COLORS[index % STRIP_ZONE_COLORS.size]

/** long-press 판정 시간 (ms). 기본값: 450 */
private const val LONG_PRESS_TIMEOUT_MS = 450L

/**
 * 엣지의 모든 존을 가로 한 줄로 시각화하는 스트립 에디터.
 *
 * - 선택된 존 강조 (green border)
 * - 경계 핸들 드래그: 인접 두 존의 비율 조정
 * - 선택된 존 탭: 선택 해제
 * - 비선택 존 탭: 선택 이동
 * - 존 길게 누름: onZoneLongPressed 콜백 (팝업 진입)
 * - highlightedZones: 병합 모드 등에서 파란 테두리로 강조할 존 집합
 * - blockedStartRatio/EndRatio: 코너 버튼 등으로 차단된 구간 비율 (0f = 없음)
 */
@Composable
fun EdgeStripEditor(
    zones: List<EdgeZone>,
    selectedZone: EdgeZone?,
    minRatio: Float,
    onZonesChanged: (List<EdgeZone>) -> Unit,
    onZoneSelected: (EdgeZone) -> Unit,
    onZoneDeselected: () -> Unit,
    onZoneLongPressed: (zone: EdgeZone, anchorCenterFraction: Float) -> Unit = { _, _ -> },
    previewZones: List<EdgeZone>? = null,
    highlightedZones: Set<Pair<EntryEdge, Float>> = emptySet(),
    blockedStartRatio: Float = 0f,
    blockedStartLabel: String? = null,
    blockedEndRatio: Float = 0f,
    blockedEndLabel: String? = null,
    inputMode: InputMode = InputMode.NORMAL,
    swipeScope: Any = ROOT_SCOPE,
    mergeBaseZone: EdgeZone? = null,
    mergeTargetRatios: Set<Float> = emptySet(),
    onMergeTargetToggle: (EdgeZone) -> Unit = {},
    onMergeConfirm: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val swipeController = LocalSwipeFocusController.current
    val focusedBoundaryIdx: Int? = when (val f = swipeController?.currentFocus) {
        is EdgeEditorElement.StripBoundary -> f.index
        else -> null
    }
    val focusedSplitN: Int? = when (val f = swipeController?.currentFocus) {
        is EdgeEditorElement.ZoneActionSplitN -> f.n
        else -> null
    }

    val currentZones by rememberUpdatedState(zones)
    val currentSelected by rememberUpdatedState(selectedZone)
    val currentOnZonesChanged by rememberUpdatedState(onZonesChanged)
    val currentOnZoneSelected by rememberUpdatedState(onZoneSelected)
    val currentOnZoneDeselected by rememberUpdatedState(onZoneDeselected)
    val currentOnZoneLongPressed by rememberUpdatedState(onZoneLongPressed)
    val currentHighlightedZones by rememberUpdatedState(highlightedZones)

    val stripHeight = EdgeSwipeConstants.EDGE_STRIP_HEIGHT_DP.dp
    val handleHitDp = EdgeSwipeConstants.EDGE_STRIP_HANDLE_HIT_DP
    val tapThresholdDp = EdgeSwipeConstants.EDGE_STRIP_TAP_THRESHOLD_DP

    // 드래그 중 임시 존 상태 (실시간 렌더링용)
    var draggingZones by remember { mutableStateOf<List<EdgeZone>?>(null) }
    val renderZones = draggingZones ?: previewZones ?: zones

    // ── 존 이동(재배치) 슬라이드 애니메이션 ──
    // 같은 엣지에서 존 개수·트리거 멀티셋은 같은데 순서만 바뀐 변경(=이동)에만 적용.
    // 비율 조정/드래그/프리셋은 즉시 반영(애니메이션 없음).
    val moveAnim = remember { Animatable(1f) }
    var animFrom by remember { mutableStateOf<List<EdgeZone>?>(null) }
    var animTo by remember { mutableStateOf(zones) }
    LaunchedEffect(zones) {
        val prev = animTo
        animTo = zones
        val sameEdge = prev.firstOrNull()?.edge == zones.firstOrNull()?.edge
        val triggersPrev = prev.map { it.trigger }
        val triggersNew = zones.map { it.trigger }
        val isReorder = sameEdge && prev.size == zones.size &&
            triggersPrev != triggersNew &&
            triggersPrev.groupingBy { it }.eachCount() == triggersNew.groupingBy { it }.eachCount()
        if (isReorder) {
            animFrom = prev
            moveAnim.snapTo(0f)
            moveAnim.animateTo(1f, tween(EdgeSwipeConstants.EDGE_ZONE_MOVE_ANIM_MS, easing = FastOutSlowInEasing))
            animFrom = null
        } else {
            animFrom = null
            moveAnim.snapTo(1f)
        }
    }
    // 애니메이션 중 각 타겟 존의 그릴 위치(시작/끝 비율)를 소스→타겟으로 보간.
    // renderZones가 base zones일 때만(드래그/프리셋 아님) 적용.
    val animRects: List<Pair<Float, Float>>? = run {
        val from = animFrom
        if (from != null && draggingZones == null && previewZones == null && moveAnim.value < 1f) {
            interpolateZoneRects(from, zones, moveAnim.value)
        } else null
    }

    val gestureModifier = if (inputMode != InputMode.NORMAL) Modifier else Modifier.pointerInput(Unit) {
        val handleHitPx = density.run { handleHitDp.dp.toPx() }
        val tapThresholdPx = density.run { tapThresholdDp.dp.toPx() }

        awaitEachGesture {
            val down = awaitPointerEvent()
            if (down.type != PointerEventType.Press) return@awaitEachGesture
            val downPos = down.changes.first().position
            down.changes.forEach { it.consume() }

            val w = size.width.toFloat()
            val zoneList = currentZones

            // 경계 핸들 히트 테스트
            val boundaryRatios = (1 until zoneList.size).map { zoneList[it].startRatio }
            val hitBoundaryIdx = boundaryRatios.indexOfFirst { ratio ->
                abs(downPos.x - ratio * w) <= handleHitPx / 2f
            }

            if (hitBoundaryIdx >= 0) {
                val leftIdx = hitBoundaryIdx
                val rightIdx = hitBoundaryIdx + 1
                val leftZone = zoneList[leftIdx]
                val rightZone = zoneList[rightIdx]
                val leftMin = leftZone.startRatio + minRatio
                val rightMax = rightZone.endRatio - minRatio

                var ev = awaitPointerEvent()
                while (ev.type != PointerEventType.Release) {
                    if (ev.type == PointerEventType.Move) {
                        ev.changes.forEach { it.consume() }
                        val newRatio = (ev.changes.first().position.x / w).coerceIn(leftMin, rightMax)
                        draggingZones = zoneList.toMutableList().also { z ->
                            z[leftIdx] = leftZone.copy(endRatio = newRatio)
                            z[rightIdx] = rightZone.copy(startRatio = newRatio)
                        }
                    }
                    ev = awaitPointerEvent()
                }

                val finalZones = draggingZones ?: zoneList
                draggingZones = null
                currentOnZonesChanged(finalZones)
                return@awaitEachGesture
            }

            // 핸들 밖 영역 — 어느 존을 눌렀나
            val tappedIdx = zoneList.indexOfFirst { z ->
                downPos.x in (z.startRatio * w)..(z.endRatio * w)
            }
            if (tappedIdx < 0) return@awaitEachGesture

            val tappedZone = zoneList[tappedIdx]
            var totalMove = 0f

            // long-press 타임아웃 안에 Up 또는 의미 있는 이동이 오면 정상 완료(non-null),
            // 타임아웃 초과 시 null → long-press 판정
            val completed = withTimeoutOrNull(LONG_PRESS_TIMEOUT_MS) {
                while (true) {
                    val ev = awaitPointerEvent()
                    when (ev.type) {
                        PointerEventType.Release -> return@withTimeoutOrNull true
                        PointerEventType.Move -> {
                            val dx = abs(ev.changes.first().position.x - downPos.x)
                            val dy = abs(ev.changes.first().position.y - downPos.y)
                            totalMove += dx + dy
                            ev.changes.forEach { it.consume() }
                            if (totalMove > tapThresholdPx) return@withTimeoutOrNull true
                        }
                        else -> {}
                    }
                }
                @Suppress("UNREACHABLE_CODE")
                true
            }

            if (completed == null) {
                // Long-press 발생 → 팝업 진입 콜백
                val cf = (tappedZone.startRatio + tappedZone.endRatio) / 2f
                currentOnZoneLongPressed(tappedZone, cf)
                // 손가락을 뗄 때까지 이벤트 소비
                var ev = awaitPointerEvent()
                while (ev.type != PointerEventType.Release) {
                    ev.changes.forEach { it.consume() }
                    ev = awaitPointerEvent()
                }
                return@awaitEachGesture
            }

            // 일반 탭 처리
            if (totalMove < tapThresholdPx) {
                val sel = currentSelected
                if (sel != null && sel.startRatio == tappedZone.startRatio && sel.edge == tappedZone.edge) {
                    currentOnZoneDeselected()
                } else {
                    currentOnZoneSelected(tappedZone)
                }
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth().height(stripHeight)
    ) {
        val stripWidthDp = maxWidth

        Canvas(
            modifier = Modifier.fillMaxSize().then(gestureModifier)
        ) {
        val w = size.width
        val h = size.height
        val activeStartPx = blockedStartRatio * w
        val activeEndPx = (1f - blockedEndRatio) * w

        drawRoundRect(color = Color(0xFF1A1A1A), cornerRadius = CornerRadius(8f))

        // 차단 구역 — 존 렌더링 전에 배경 먼저
        fun drawBlockedSection(x0: Float, x1: Float, label: String?) {
            val bw = x1 - x0
            if (bw <= 0f) return
            drawRect(color = Color(0xFF141414), topLeft = Offset(x0, 0f), size = Size(bw, h))
            drawStripBlockedHatch(Offset(x0, 0f), Size(bw, h))
            val borderX = if (x0 <= 0f) x1 else x0
            drawLine(color = Color(0xAACC4444), start = Offset(borderX, 0f), end = Offset(borderX, h), strokeWidth = 1.5f)
            val displayText = if (label != null) "비활성\n$label" else "비활성"
            val tl = textMeasurer.measure(displayText, style = TextStyle(
                fontSize = EdgeSwipeConstants.ZONE_LABEL_FONT_SIZE_SP.sp,
                color = Color(0xAACC4444),
                textAlign = TextAlign.Center
            ))
            if (bw > tl.size.width + 4f && h > tl.size.height + 4f) {
                val cx = x0 + bw / 2f
                drawText(tl, topLeft = Offset(cx - tl.size.width / 2f, h / 2f - tl.size.height / 2f))
            }
        }

        if (blockedStartRatio > 0f) drawBlockedSection(0f, activeStartPx, blockedStartLabel)
        if (blockedEndRatio > 0f) drawBlockedSection(activeEndPx, w, blockedEndLabel)

        // 존 렌더링 — 활성 구역으로 클리핑
        val sel = currentSelected
        val highlights = currentHighlightedZones

        renderZones.forEachIndexed { idx, zone ->
            // 그릴 위치는 애니메이션 보간값, 선택/하이라이트 판정은 타겟 비율(zone.startRatio) 유지
            val drawStart = animRects?.getOrNull(idx)?.first ?: zone.startRatio
            val drawEnd = animRects?.getOrNull(idx)?.second ?: zone.endRatio
            val x0 = (drawStart * w).coerceAtLeast(activeStartPx)
            val x1 = (drawEnd * w).coerceAtMost(activeEndPx)
            val zoneW = x1 - x0
            if (zoneW <= 0f) return@forEachIndexed

            val isSelected = sel != null && zone.startRatio == sel.startRatio && zone.edge == sel.edge
            val isHighlighted = highlights.contains(zone.edge to zone.startRatio)
            val isInactive = zone.action is EdgeZoneAction.Unassigned
            val isMergeTarget = mergeBaseZone != null && zone.edge == mergeBaseZone.edge && zone.startRatio in mergeTargetRatios

            val baseColor = if (isInactive) Color(0xFF3A3A3A) else stripZoneColor(idx)
            drawRect(
                color = baseColor.copy(alpha = if (isSelected) 0.85f else 0.50f),
                topLeft = Offset(x0, 0f),
                size = Size(zoneW, h)
            )

            if (isInactive) drawStripHatch(Offset(x0, 0f), Size(zoneW, h))

            // 선택된 병합 대상: 옅은 초록 오버레이
            if (isMergeTarget) {
                drawRect(
                    color = Color(0x334CAF50),
                    topLeft = Offset(x0, 0f),
                    size = Size(zoneW, h)
                )
            }

            // 테두리: isMergeTarget(초록) > highlighted(파랑) > selected(초록) > 기본
            val borderColor = when {
                isMergeTarget -> Color(0xFF4CAF50)
                isHighlighted -> Color(0xFF2196F3)
                isSelected -> Color(0xFF4CAF50)
                isInactive -> Color.White.copy(alpha = 0.10f)
                else -> Color.White.copy(alpha = 0.20f)
            }
            val borderWidth = when {
                isMergeTarget || isHighlighted || isSelected -> 2.5f
                else -> 1f
            }
            drawRect(
                color = borderColor,
                topLeft = Offset(x0, 0f),
                size = Size(zoneW, h),
                style = Stroke(width = borderWidth)
            )

            val displayZoneLabel = zone.displayLabel
            if (displayZoneLabel.isNotEmpty()) {
                val displayText = displayZoneLabel
                // 존 폭이 좁아 라벨이 다 안 들어가면 말줄임(…) 처리
                val availWidthPx = (zoneW - 4f).coerceAtLeast(0f).toInt()
                val textLayout = textMeasurer.measure(
                    displayText,
                    style = TextStyle(
                        fontSize = EdgeSwipeConstants.ZONE_LABEL_FONT_SIZE_SP.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center
                    ),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    constraints = Constraints(maxWidth = availWidthPx)
                )
                val tx = x0 + zoneW / 2f - textLayout.size.width / 2f
                val ty = h / 2f - textLayout.size.height / 2f
                if (availWidthPx > 0) {
                    drawText(textLayout, topLeft = Offset(tx, ty))
                }
            }
        }

        // 경계 핸들 — 활성 구역 내부만
        val handleWidthPx = density.run { EdgeSwipeConstants.EDGE_STRIP_HANDLE_WIDTH_DP.dp.toPx() }
        (1 until renderZones.size).forEach { i ->
            val x = (animRects?.getOrNull(i)?.first ?: renderZones[i].startRatio) * w
            if (x <= activeStartPx + 1f || x >= activeEndPx - 1f) return@forEach
            val isFocused = focusedBoundaryIdx == i - 1
            drawLine(
                color = if (isFocused) Color(0xFF2196F3) else Color.White.copy(alpha = 0.55f),
                start = Offset(x, 4f),
                end = Offset(x, h - 4f),
                strokeWidth = handleWidthPx
            )
        }
        // 분할 미리보기 — SplitChoosing에서 분할 개수 버튼 포커스 시 n등분 점선/해치 표시
        val splitPreviewSel = currentSelected
        val splitN = focusedSplitN
        if (splitPreviewSel != null && splitN != null && splitN >= 2) {
            val rangeW = splitPreviewSel.endRatio - splitPreviewSel.startRatio
            val partRatio = rangeW / splitN
            // 새로 생길 조각(인덱스 1..n-1) — Unassigned 해치 오버레이
            for (i in 1 until splitN) {
                val px0 = ((splitPreviewSel.startRatio + i * partRatio) * w).coerceIn(activeStartPx, activeEndPx)
                val px1 = ((splitPreviewSel.startRatio + (i + 1) * partRatio) * w).coerceAtMost(activeEndPx)
                if (px1 > px0) drawStripHatch(Offset(px0, 0f), Size(px1 - px0, h))
            }
            // 분할 경계선 — amber 점선
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
            for (i in 1 until splitN) {
                val bx = ((splitPreviewSel.startRatio + i * partRatio) * w).coerceIn(activeStartPx, activeEndPx)
                drawLine(
                    color = Color(0xFFFFC107),
                    start = Offset(bx, 0f),
                    end = Offset(bx, h),
                    strokeWidth = 2f,
                    pathEffect = dashEffect
                )
            }
        }
        } // Canvas 닫기

        // ── 미리보기 amber 보더 오버레이 ──
        if (previewZones != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = EdgeSwipeConstants.RATIO_PREVIEW_BORDER_WIDTH_DP.dp,
                        color = Color(0xFFFFC107).copy(alpha = EdgeSwipeConstants.RATIO_PREVIEW_BORDER_ALPHA),
                        shape = RoundedCornerShape(8.dp)
                    )
            )
        }

        // ── SWIPE 모드 오버레이: 존 + 경계선 ──
        if (inputMode == InputMode.SWIPE) {
            // 각 존 — 탭으로 선택, 더블탭으로 액션 팝업
            zones.forEachIndexed { idx, zone ->
                val zoneLeftDp = stripWidthDp * zone.startRatio
                val zoneWidthDp = stripWidthDp * (zone.endRatio - zone.startRatio)
                SwipeFocusable(
                    element = EdgeEditorElement.StripZone(idx),
                    scope = swipeScope,
                    shape = RoundedCornerShape(4.dp),
                    highlightBorderWidth = 1.5.dp,
                    showBorderHighlight = true,
                    onActivate = { onZoneSelected(zone) },
                    onActivateAlt = {
                        val cf = (zone.startRatio + zone.endRatio) / 2f
                        onZoneLongPressed(zone, cf)
                    },
                    gridRow = 21,
                    modifier = Modifier
                        .offset(x = zoneLeftDp)
                        .size(width = zoneWidthDp, height = stripHeight),
                ) {}
            }

            // 경계선 — 탭으로 조작 모드 진입, 스와이프로 비율 조정
            val handleDp = handleHitDp.dp
            (1 until zones.size).forEach { i ->
                val leftIdx = i - 1
                val rightIdx = i
                val boundaryRatio = zones[i].startRatio
                val centerDp = stripWidthDp * boundaryRatio
                SwipeFocusable(
                    element = EdgeEditorElement.StripBoundary(leftIdx),
                    scope = swipeScope,
                    shape = RoundedCornerShape(4.dp),
                    manipulatable = true,
                    gridRow = 21,
                    onManipulate = { deltaPx, screenWidthPx ->
                        // 화면 너비 기준 적응적 — 스와이프 deltaPx를 비율로 변환
                        val deltaRatio = deltaPx / screenWidthPx
                        val leftZone = zones[leftIdx]
                        val rightZone = zones[rightIdx]
                        val newRatio = (boundaryRatio + deltaRatio).coerceIn(
                            leftZone.startRatio + minRatio,
                            rightZone.endRatio - minRatio
                        )
                        val newZones = zones.toMutableList().also { z ->
                            z[leftIdx] = leftZone.copy(endRatio = newRatio)
                            z[rightIdx] = rightZone.copy(startRatio = newRatio)
                        }
                        onZonesChanged(newZones)
                    },
                    modifier = Modifier
                        .offset(x = centerDp - handleDp / 2)
                        .size(width = handleDp, height = stripHeight),
                ) {}
            }
        }

        // ── 병합 모드 인라인 오버레이 (NORMAL·SWIPE 공통) ──
        if (mergeBaseZone != null) {
            val baseIdx = zones.indexOfFirst { it.startRatio == mergeBaseZone.startRatio && it.edge == mergeBaseZone.edge }
            if (baseIdx >= 0) {
                val baseZone = zones[baseIdx]
                val leftZone = zones.getOrNull(baseIdx - 1)
                val rightZone = zones.getOrNull(baseIdx + 1)
                val cs = MaterialTheme.colorScheme
                val baseLeftDp = stripWidthDp * baseZone.startRatio
                val baseWidthDp = stripWidthDp * (baseZone.endRatio - baseZone.startRatio)
                val showText = baseWidthDp >= 48.dp

                // 확인 버튼 (기준 존 칸 중앙)
                if (inputMode == InputMode.SWIPE) {
                    SwipeFocusable(
                        element = EdgeEditorElement.ZoneActionMergeConfirm,
                        scope = EdgeEditorScope.ZoneActionPopup,
                        shape = RoundedCornerShape(4.dp),
                        showBorderHighlight = true,
                        onActivate = onMergeConfirm,
                        gridRow = 0,
                        modifier = Modifier
                            .offset(x = baseLeftDp)
                            .size(width = baseWidthDp, height = stripHeight),
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = cs.primaryContainer,
                                contentColor = cs.onPrimaryContainer,
                                modifier = Modifier.padding(4.dp),
                            ) {
                                if (showText) {
                                    Text("확인", fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                } else {
                                    Icon(Icons.Filled.Check, contentDescription = "확인", modifier = Modifier.size(14.dp).padding(3.dp))
                                }
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .offset(x = baseLeftDp)
                            .size(width = baseWidthDp, height = stripHeight),
                        contentAlignment = Alignment.Center,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = cs.primaryContainer,
                            contentColor = cs.onPrimaryContainer,
                            modifier = Modifier.padding(4.dp).clickable(onClick = onMergeConfirm),
                        ) {
                            if (showText) {
                                Text("확인", fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                            } else {
                                Icon(Icons.Filled.Check, contentDescription = "확인", modifier = Modifier.size(14.dp).padding(3.dp))
                            }
                        }
                    }
                }

                // SWIPE 모드: 인접 존 포커스 오버레이 (탭=토글)
                if (inputMode == InputMode.SWIPE) {
                    leftZone?.let { lz ->
                        val leftDp = stripWidthDp * lz.startRatio
                        val leftWidthDp = stripWidthDp * (lz.endRatio - lz.startRatio)
                        SwipeFocusable(
                            element = EdgeEditorElement.ZoneActionMergeLeft,
                            scope = EdgeEditorScope.ZoneActionPopup,
                            shape = RoundedCornerShape(4.dp),
                            showBorderHighlight = true,
                            onActivate = { onMergeTargetToggle(lz) },
                            gridRow = 0,
                            modifier = Modifier
                                .offset(x = leftDp)
                                .size(width = leftWidthDp, height = stripHeight),
                        ) {}
                    }
                    rightZone?.let { rz ->
                        val rightDp = stripWidthDp * rz.startRatio
                        val rightWidthDp = stripWidthDp * (rz.endRatio - rz.startRatio)
                        SwipeFocusable(
                            element = EdgeEditorElement.ZoneActionMergeRight,
                            scope = EdgeEditorScope.ZoneActionPopup,
                            shape = RoundedCornerShape(4.dp),
                            showBorderHighlight = true,
                            onActivate = { onMergeTargetToggle(rz) },
                            gridRow = 0,
                            modifier = Modifier
                                .offset(x = rightDp)
                                .size(width = rightWidthDp, height = stripHeight),
                        ) {}
                    }
                }
            }
        }
    } // BoxWithConstraints 닫기
}

/**
 * 재배치 애니메이션용: 타겟 존(`to`) 순서대로, 각 존의 그릴 rect(시작/끝 비율)를
 * 소스(`from`)에서의 같은 트리거 위치 → 타겟 위치로 보간한 값을 반환.
 *
 * 트리거 동일 존을 greedily 매칭하므로 폭째 이동(블록이 자리째 슬라이드)과
 * 액션만 교환(내용 블록이 슬롯을 가로질러 교차)이 모두 자연스럽게 보간된다.
 */
private fun interpolateZoneRects(
    from: List<EdgeZone>,
    to: List<EdgeZone>,
    t: Float,
): List<Pair<Float, Float>> {
    val pool = from.toMutableList()
    return to.map { nz ->
        val srcIdx = pool.indexOfFirst { it.trigger == nz.trigger }
        val src = if (srcIdx >= 0) pool.removeAt(srcIdx) else nz
        val s = src.startRatio + (nz.startRatio - src.startRatio) * t
        val e = src.endRatio + (nz.endRatio - src.endRatio) * t
        s to e
    }
}

private fun DrawScope.drawStripBlockedHatch(topLeft: Offset, size: Size) {
    val step = 6f
    val lineCount = ((size.width + size.height) / step).toInt() + 2
    for (i in 0..lineCount) {
        val x0 = topLeft.x + i * step
        val x1 = x0 - size.height
        drawLine(
            color = Color(0x44CC4444),
            start = Offset(x0.coerceIn(topLeft.x, topLeft.x + size.width), topLeft.y),
            end = Offset(x1.coerceIn(topLeft.x, topLeft.x + size.width), topLeft.y + size.height),
            strokeWidth = 1f
        )
    }
}

private fun DrawScope.drawStripHatch(topLeft: Offset, size: Size) {
    val step = 6f
    val lineCount = ((size.width + size.height) / step).toInt() + 2
    for (i in 0..lineCount) {
        val x0 = topLeft.x + i * step
        val x1 = x0 - size.height
        drawLine(
            color = Color(0x22FFFFFF),
            start = Offset(x0.coerceIn(topLeft.x, topLeft.x + size.width), topLeft.y),
            end = Offset(x1.coerceIn(topLeft.x, topLeft.x + size.width), topLeft.y + size.height),
            strokeWidth = 1f
        )
    }
}

private val EdgeZone.action: EdgeZoneAction
    get() = when (val t = trigger) {
        is EdgeZoneTrigger.SingleAction -> t.action
        is EdgeZoneTrigger.Rotation -> t.candidates.firstOrNull()?.action ?: EdgeZoneAction.Unassigned
    }
