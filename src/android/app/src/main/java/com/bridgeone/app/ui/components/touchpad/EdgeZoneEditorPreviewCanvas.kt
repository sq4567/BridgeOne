package com.bridgeone.app.ui.components.touchpad

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bridgeone.app.ui.common.AppIcons
import com.bridgeone.app.ui.common.EdgeSwipeConstants
import com.bridgeone.app.ui.theme.PretendardFontFamily
import com.bridgeone.app.ui.components.touchpad.defaultCornerEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.util.lerp
import kotlin.math.roundToInt

// 존 색상 팔레트 — EdgeZone.kt의 ZONE_COLORS / zoneColor() 공용 함수 사용

/** 미리보기 강조 존 테두리 바깥 글로우의 겹 수. 기본값: 4 */
private const val PREVIEW_GLOW_LAYERS = 4
/** 미리보기 글로우 각 겹의 확장 간격 (dp). 기본값: 2f */
private const val PREVIEW_GLOW_STEP_DP = 2f
/** 미리보기 글로우 기본 투명도(안쪽 겹). 바깥 겹일수록 옅어진다. 기본값: 0.45f */
private const val PREVIEW_GLOW_BASE_ALPHA = 0.45f

/** 이동 모드 들림 고스트 채움 투명도. 기본값: 0.35f */
private const val LIFTED_GHOST_ALPHA = 0.35f
/** 들림 고스트 드롭섀도 오프셋 (dp). 기본값: 3f */
private const val LIFTED_GHOST_SHADOW_OFFSET_DP = 3f
/** 들림 고스트 드롭섀도 투명도. 기본값: 0.35f */
private const val LIFTED_GHOST_SHADOW_ALPHA = 0.35f
/** 이동 모드 강조색(주황, CanvasModeKind.MOVE accent와 동일). */
private val LIFTED_GHOST_BORDER_COLOR = Color(0xFFB84A00)

/**
 * 터치패드 모형을 Canvas로 렌더링하고 존 탭을 처리하는 편집기 미리보기.
 *
 * @param config                 현재 (편집 중) 존 설정
 * @param highlightKeys          강조(초록 테두리)할 존 키 집합. 단일 선택·다중 선택 모두 같은 렌더 경로로 처리.
 * @param bottomLeftButtonLabel  좌하 모서리 버튼 사유 (null = 버튼 없음)
 * @param bottomRightButtonLabel 우하 모서리 버튼 사유 (null = 버튼 없음)
 * @param disabledEdges          탭 불가 + 시스템 비활성 스타일로 표시할 엣지 → 사유 맵
 * @param structureOnly          true이면 존을 색 분할로만 표현 (미할당 회색/빗금 비적용, 라벨 미출력). 구조 미리보기용.
 * @param interactive            false이면 포인터 입력 처리 없음 (제스처가 하위 레이어로 통과). 썸네일/미리보기 표시용.
 * @param onZoneTapped               존이 탭됐을 때
 * @param onCornerPriorityToggled    코너 우선순위 표시 영역 탭 시 해당 코너를 전달
 */
@Composable
fun EdgeZoneEditorPreviewCanvas(
    config: EdgeZoneConfig,
    highlightKeys: Set<ZoneKey> = emptySet(),
    bottomLeftButtonLabel: String? = "다이나믹스",
    bottomRightButtonLabel: String? = "모드 프리셋",
    disabledEdges: Map<EntryEdge, String> = emptyMap(),
    structureOnly: Boolean = false,
    interactive: Boolean = true,
    resizeMode: Boolean = false,
    // true면 highlightKeys 존 테두리를 초록 대신 파랑(primary)으로 — SWIPE 프리셋 미리보기 표시용
    highlightAsPreview: Boolean = false,
    // 이동 모드에서 "들어올린" 존의 키. 해당 존은 반투명 + 드롭섀도 + 주황 보더로 떠 있는 듯 렌더.
    liftedKey: ZoneKey? = null,
    onZoneTapped: (EdgeZone) -> Unit = {},
    onCornerPriorityToggled: (CornerOverlap) -> Unit = {},
    blockedRatio: Float = EdgeSwipeConstants.CORNER_BUTTON_BLOCKED_RATIO,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val previewBorderColor = androidx.compose.material3.MaterialTheme.colorScheme.primary
    val textMeasurer = rememberTextMeasurer()
    val dynamicsPainter = rememberVectorPainter(AppIcons.DynamicsStandard.staticIcon)
    val modePresetPainter = rememberVectorPainter(AppIcons.ModePresetStandard.staticIcon)
    val edgeWidthDp = EdgeSwipeConstants.EDGE_HIT_WIDTH_DP.dp

    val interactionModifier = Modifier.pointerInput(config, bottomLeftButtonLabel, bottomRightButtonLabel, disabledEdges) {
        val edgePx = density.run { edgeWidthDp.toPx() }

        awaitEachGesture {
            val down = awaitPointerEvent()
            if (down.type != PointerEventType.Press) return@awaitEachGesture
            val downPos = down.changes.first().position
            down.changes.forEach { it.consume() }

            val w = size.width.toFloat()
            val h = size.height.toFloat()
            val tappedCorner = findCornerAt(
                downPos, w, h, edgePx, disabledEdges.keys,
                hasBottomLeft = bottomLeftButtonLabel != null,
                hasBottomRight = bottomRightButtonLabel != null
            )
            val tappedZone = if (tappedCorner == null) findZoneAt(
                downPos, config, w, h, edgePx,
                disabledEdges.keys,
                hasBottomLeft = bottomLeftButtonLabel != null,
                hasBottomRight = bottomRightButtonLabel != null,
                blockedRatio = blockedRatio,
            ) else null

            var ev = awaitPointerEvent()
            while (ev.type != PointerEventType.Release) {
                if (ev.type == PointerEventType.Move) ev.changes.forEach { it.consume() }
                ev = awaitPointerEvent()
            }

            if (tappedCorner != null) onCornerPriorityToggled(tappedCorner)
            else if (tappedZone != null) onZoneTapped(tappedZone)
        }
    }

    val activeInteractionModifier = if (interactive) interactionModifier else Modifier

    // 존별 선택 progress(0→1=선택, 0=비선택)를 animateFloatAsState로 보간.
    // highlightKeys만 바뀔 때(삭제/선택 토글 등)는 존 수가 유지되어 Compose 호출 위치가 안정적.
    val selAnimSpec = tween<Float>(EdgeSwipeConstants.EDGE_ZONE_MORPH_MS, easing = FastOutSlowInEasing)
    val allZones = EntryEdge.entries.flatMap { config.zonesFor(it) }
    val selectionProgress = HashMap<ZoneKey, Float>(allZones.size)
    for (zone in allZones) {
        val key = zone.key()
        val target = if (key != liftedKey && key in highlightKeys) 1f else 0f
        selectionProgress[key] =
            animateFloatAsState(target, selAnimSpec, label = "zoneSel_${key.edge}_${key.startRatio}").value
    }

    Canvas(modifier = modifier.then(activeInteractionModifier).fillMaxSize()) {
        val w = size.width
        val h = size.height
        val edgePx = density.run { edgeWidthDp.toPx() }
        val cornerRadius = 24f
        val innerCorner = (cornerRadius - edgePx / 2f).coerceAtLeast(4f)

        // 터치패드 배경 (outer rounded)
        drawRoundRect(color = Color(0xFF1A1A1A), cornerRadius = CornerRadius(cornerRadius))

        // 중앙 영역 (inner rounded — 외곽과 정합)
        drawRoundRect(
            color = Color(0xFF252525),
            topLeft = Offset(edgePx, edgePx),
            size = Size(w - edgePx * 2, h - edgePx * 2),
            cornerRadius = CornerRadius(innerCorner)
        )

        // 각 엣지 존 블록 렌더링 (코너 차단 구역으로 클리핑)
        val hasBottomLeft = bottomLeftButtonLabel != null
        val hasBottomRight = bottomRightButtonLabel != null
        EntryEdge.entries.forEach { edge ->
            val zones = config.zonesFor(edge)
            val isEdgeDisabled = edge in disabledEdges.keys
            zones.forEachIndexed { idx, zone ->
                val rawZr = zoneRect(zone, w, h, edgePx, hasBottomLeft, hasBottomRight, blockedRatio)
                val zr = clipZoneRect(rawZr, edge, w, h, config.cornerPriority, edgePx)
                if (zr.size.width <= 0f || zr.size.height <= 0f) return@forEachIndexed

                val isLifted = liftedKey != null && zone.key() == liftedKey
                val sel = selectionProgress[zone.key()] ?: 0f
                val isInactive = !isEdgeDisabled && zone.action is EdgeZoneAction.Unassigned

                if (structureOnly) {
                    // 구조 미리보기 모드: 색 분할만 표현, 미할당 회색/빗금 비적용, 라벨 미출력
                    val baseColor = if (isEdgeDisabled) Color(0xFF1E1E1E) else zoneColor(idx)
                    drawRect(
                        color = baseColor.copy(alpha = if (isEdgeDisabled) 0.95f else 0.65f),
                        topLeft = zr.topLeft,
                        size = zr.size
                    )
                    if (isEdgeDisabled) drawDisabledEdgeHatch(zr.topLeft, zr.size)
                    drawRect(
                        color = if (isEdgeDisabled) Color(0xFFCC4444).copy(alpha = 0.35f)
                                else Color.White.copy(alpha = 0.25f),
                        topLeft = zr.topLeft,
                        size = zr.size,
                        style = Stroke(width = 1f)
                    )
                } else {
                    // 일반 편집기 모드: 미할당 회색/빗금 + 라벨 포함
                    val baseColor = when {
                        isEdgeDisabled -> Color(0xFF1E1E1E)
                        isInactive -> Color(0xFF3A3A3A)
                        else -> zoneColor(idx)
                    }

                    // 들림 고스트: 존 rect 뒤에 오프셋 드롭섀도를 먼저 그려 "떠 있는" 느낌
                    if (isLifted) {
                        val off = LIFTED_GHOST_SHADOW_OFFSET_DP.dp.toPx()
                        drawRect(
                            color = Color.Black.copy(alpha = LIFTED_GHOST_SHADOW_ALPHA),
                            topLeft = Offset(zr.left + off, zr.top + off),
                            size = zr.size
                        )
                    }
                    drawRect(
                        color = baseColor.copy(alpha = when {
                            isEdgeDisabled -> 0.95f
                            isLifted -> LIFTED_GHOST_ALPHA
                            isInactive -> lerp(0.60f, 0.85f, sel)
                            else -> lerp(0.50f, 0.85f, sel)
                        }),
                        topLeft = zr.topLeft,
                        size = zr.size
                    )
                    // 시스템 비활성 엣지: 빨간 빗금 오버레이
                    if (isEdgeDisabled) {
                        drawDisabledEdgeHatch(zr.topLeft, zr.size)
                    }
                    // 선택 강조: 테두리 바깥으로 점점 옅어지는 글로우 (미리보기=파랑, 일반 선택=초록).
                    // sel(0→1)에 비례해 확장폭과 alpha가 함께 자라며 부드럽게 나타나고 사라진다.
                    if (sel > 0f) {
                        val glowColor = if (highlightAsPreview) previewBorderColor else Color(0xFF4CAF50)
                        val stepPx = PREVIEW_GLOW_STEP_DP.dp.toPx()
                        for (layer in PREVIEW_GLOW_LAYERS downTo 1) {
                            val e = layer * stepPx * sel
                            drawRect(
                                color = glowColor.copy(alpha = PREVIEW_GLOW_BASE_ALPHA / (layer + 1) * sel),
                                topLeft = Offset(zr.left - e, zr.top - e),
                                size = Size(zr.size.width + 2 * e, zr.size.height + 2 * e),
                                style = Stroke(width = stepPx * 1.5f),
                            )
                        }
                    }
                    // 보더: 선택(초록)·들림 고스트(주황) 테두리는 dp 기준으로 통일(px 직접 지정 시 밀도에 따라 얇아지는 문제 방지).
                    // sel(0→1)에 비례해 테두리 색·두께가 보간되어 부드럽게 강조된다.
                    val accentBorderColor = if (highlightAsPreview) previewBorderColor else Color(0xFF4CAF50)
                    val baseBorderColor = if (isInactive) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.20f)
                    val focusBorderPx = EdgeSwipeConstants.EDGE_ZONE_FOCUS_BORDER_DP.dp.toPx()
                    drawRect(
                        color = when {
                            isEdgeDisabled -> Color(0xFFCC4444).copy(alpha = 0.35f)
                            isLifted -> LIFTED_GHOST_BORDER_COLOR
                            else -> lerp(baseBorderColor, accentBorderColor, sel)
                        },
                        topLeft = zr.topLeft,
                        size = zr.size,
                        style = Stroke(width = if (isLifted) focusBorderPx else lerp(1f, focusBorderPx, sel))
                    )

                    val displayZoneLabel = zone.label.ifEmpty {
                        if (zone.trigger is EdgeZoneTrigger.SingleAction && zone.action !is EdgeZoneAction.Unassigned)
                            zone.action.defaultLabel()
                        else ""
                    }
                    if (!isEdgeDisabled && displayZoneLabel.isNotEmpty()) {
                        val isVerticalEdge = edge == EntryEdge.LEFT || edge == EntryEdge.RIGHT
                        val displayText = if (isVerticalEdge) {
                            displayZoneLabel.toList().joinToString("\n")
                        } else {
                            displayZoneLabel
                        }
                        // 가용 공간에 맞춰 말줄임(…) 처리: 가로 엣지는 너비, 세로 엣지는 높이 기준
                        val availWidthPx = (zr.size.width - 4f).coerceAtLeast(0f).toInt()
                        val availHeightPx = (zr.size.height - 2f).coerceAtLeast(0f).toInt()
                        val textLayout = textMeasurer.measure(
                            displayText,
                            style = TextStyle(
                                fontFamily = PretendardFontFamily,
                                fontSize = EdgeSwipeConstants.ZONE_LABEL_FONT_SIZE_SP.sp,
                                color = Color.White.copy(alpha = 0.85f),
                                textAlign = if (isVerticalEdge) TextAlign.Center else TextAlign.Start
                            ),
                            overflow = TextOverflow.Ellipsis,
                            maxLines = if (isVerticalEdge) displayText.count { it == '\n' } + 1 else 1,
                            constraints = if (isVerticalEdge) {
                                Constraints(maxHeight = availHeightPx)
                            } else {
                                Constraints(maxWidth = availWidthPx)
                            }
                        )
                        val tx = zr.center.x - textLayout.size.width / 2f
                        val ty = zr.center.y - textLayout.size.height / 2f
                        if (availWidthPx > 0 && availHeightPx > 0) {
                            drawText(textLayout, topLeft = Offset(tx, ty))
                        }
                    }
                }
            }
        }

        // 비활성 엣지 데포르메 오브젝트 블록
        disabledEdges.forEach { (edge, reason) ->
            val blockH = edgePx * EdgeSwipeConstants.DEFORMED_CONTROL_BLOCK_HEIGHT_RATIO
            val blockCornerPx = blockH * 0.45f
            val textLayout = textMeasurer.measure(
                reason,
                style = TextStyle(
                    fontFamily = PretendardFontFamily,
                    fontSize = EdgeSwipeConstants.ZONE_LABEL_FONT_SIZE_SP.sp,
                    color = Color(0xFF1E1E1E),
                    textAlign = TextAlign.Center
                )
            )
            val blockW = (textLayout.size.width + blockCornerPx * 4f)
                .coerceIn(edgePx, (w * 0.5f).coerceAtLeast(edgePx))
            when (edge) {
                EntryEdge.TOP -> drawDeformedControlBlock(
                    centerX = w / 2f, top = 0f,
                    blockW = blockW, blockH = blockH,
                    bgColor = Color(0xFF2196F3), textLayout = textLayout
                )
                EntryEdge.BOTTOM -> drawDeformedControlBlock(
                    centerX = w / 2f, top = h - blockH,
                    blockW = blockW, blockH = blockH,
                    bgColor = Color(0xFF2196F3), textLayout = textLayout
                )
                EntryEdge.LEFT, EntryEdge.RIGHT -> {}
            }
        }

        // 모서리 버튼 차단 영역
        val btnSidePx = density.run { EdgeSwipeConstants.DEFORMED_BUTTON_SIDE_DP.dp.toPx() }
        val btnCornerPx = density.run { EdgeSwipeConstants.DEFORMED_BUTTON_CORNER_DP.dp.toPx() }
        val btnInsetPx = density.run { 4.dp.toPx() }

        if (bottomLeftButtonLabel != null) {
            val leftRect   = Rect(Offset(0f, (1f - blockedRatio) * h), Size(edgePx, blockedRatio * h - edgePx))
            val bottomRect = Rect(Offset(0f, h - edgePx), Size(w * blockedRatio, edgePx))
            drawBlockedRegion(leftRect.topLeft, leftRect.size)
            drawBlockedRegion(bottomRect.topLeft, bottomRect.size)
            drawDeformedSquareButton(
                left = edgePx + btnInsetPx, top = h - edgePx - btnSidePx - btnInsetPx,
                side = btnSidePx, cornerRadiusPx = btnCornerPx,
                bgColor = Color(0xFF7C9EFF), iconPainter = dynamicsPainter,
                iconTint = Color(0xFF1E1E1E), iconRatio = EdgeSwipeConstants.DEFORMED_BUTTON_ICON_RATIO
            )
        }
        if (bottomRightButtonLabel != null) {
            val rightRect  = Rect(Offset(w - edgePx, (1f - blockedRatio) * h), Size(edgePx, blockedRatio * h - edgePx))
            val bottomRect = Rect(Offset((1f - blockedRatio) * w, h - edgePx), Size(blockedRatio * w, edgePx))
            drawBlockedRegion(rightRect.topLeft, rightRect.size)
            drawBlockedRegion(bottomRect.topLeft, bottomRect.size)
            drawDeformedSquareButton(
                left = w - edgePx - btnSidePx - btnInsetPx, top = h - edgePx - btnSidePx - btnInsetPx,
                side = btnSidePx, cornerRadiusPx = btnCornerPx,
                bgColor = Color(0xFFFFB74D), iconPainter = modePresetPainter,
                iconTint = Color(0xFF1E1E1E), iconRatio = EdgeSwipeConstants.DEFORMED_BUTTON_ICON_RATIO
            )
        }

        // 존 경계선 (차단 구역 내 선은 제외). 비율 조정 모드면 두껍게 강조해 드래그 핸들임을 명확히 표시.
        val boundaryColor = if (resizeMode) Color.White.copy(alpha = 0.95f) else Color.White.copy(alpha = 0.45f)
        val boundaryWidth = if (resizeMode) 7f else 1.5f
        EntryEdge.entries.forEach { edge ->
            val zones = config.zonesFor(edge)
            EdgeZoneDetector.boundaryRatios(zones).forEach { ratio ->
                // 존 경계 비율을 유효 영역으로 매핑(버튼 차단 제외). 코너 겹침(edgePx) 밴드만 별도로 제외.
                val mapped = mapToValid(edge, ratio, hasBottomLeft, hasBottomRight, blockedRatio)
                if (isInLostCornerBand(edge, mapped, config.cornerPriority, w, h, edgePx, hasBottomLeft, hasBottomRight)) return@forEach
                // 정수 픽셀 스냅 → 선폭이 sub-pixel 위치에 따라 달라 보이는 현상 방지
                when (edge) {
                    EntryEdge.LEFT   -> { val y = (mapped * h).roundToInt().toFloat(); drawLine(boundaryColor, Offset(0f, y), Offset(edgePx, y), boundaryWidth) }
                    EntryEdge.RIGHT  -> { val y = (mapped * h).roundToInt().toFloat(); drawLine(boundaryColor, Offset(w - edgePx, y), Offset(w, y), boundaryWidth) }
                    EntryEdge.TOP    -> { val x = (mapped * w).roundToInt().toFloat(); drawLine(boundaryColor, Offset(x, 0f), Offset(x, edgePx), boundaryWidth) }
                    EntryEdge.BOTTOM -> { val x = (mapped * w).roundToInt().toFloat(); drawLine(boundaryColor, Offset(x, h - edgePx), Offset(x, h), boundaryWidth) }
                }
            }
        }

        // 코너 우선순위 표시
        CornerOverlap.entries.forEach { corner ->
            val isBlocked = when (corner) {
                CornerOverlap.BOTTOM_LEFT  -> hasBottomLeft
                CornerOverlap.BOTTOM_RIGHT -> hasBottomRight
                else -> false
            }
            if (isBlocked) return@forEach
            val (edgeA, edgeB) = when (corner) {
                CornerOverlap.TOP_LEFT     -> EntryEdge.TOP to EntryEdge.LEFT
                CornerOverlap.TOP_RIGHT    -> EntryEdge.TOP to EntryEdge.RIGHT
                CornerOverlap.BOTTOM_LEFT  -> EntryEdge.BOTTOM to EntryEdge.LEFT
                CornerOverlap.BOTTOM_RIGHT -> EntryEdge.BOTTOM to EntryEdge.RIGHT
            }
            if (edgeA in disabledEdges.keys || edgeB in disabledEdges.keys) return@forEach
            val cx = when (corner) {
                CornerOverlap.TOP_LEFT, CornerOverlap.BOTTOM_LEFT -> edgePx / 2f
                CornerOverlap.TOP_RIGHT, CornerOverlap.BOTTOM_RIGHT -> w - edgePx / 2f
            }
            val cy = when (corner) {
                CornerOverlap.TOP_LEFT, CornerOverlap.TOP_RIGHT -> edgePx / 2f
                CornerOverlap.BOTTOM_LEFT, CornerOverlap.BOTTOM_RIGHT -> h - edgePx / 2f
            }
            val priority = config.cornerPriority[corner] ?: defaultCornerEdge(corner)
            drawCornerPriorityIndicator(cx, cy, priority, edgePx)
        }
    }
}

/** 시스템 비활성 엣지 빗금 패턴 (붉은 계열) */
private fun DrawScope.drawDisabledEdgeHatch(topLeft: Offset, size: Size) {
    val step = 5f
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

/** 비활성 존 빗금 패턴 */
private fun DrawScope.drawInactiveHatch(topLeft: Offset, size: Size) {
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

/** 코너 버튼 데포르메 미니어처 (정사각형 + 아이콘). 빨간 빗금 위에 그림. */
private fun DrawScope.drawDeformedSquareButton(
    left: Float, top: Float, side: Float, cornerRadiusPx: Float,
    bgColor: Color, iconPainter: Painter, iconTint: Color, iconRatio: Float
) {
    drawRoundRect(bgColor, topLeft = Offset(left, top), size = Size(side, side), cornerRadius = CornerRadius(cornerRadiusPx))
    val iconPx = side * iconRatio
    translate(left + (side - iconPx) / 2f, top + (side - iconPx) / 2f) {
        with(iconPainter) {
            draw(Size(iconPx, iconPx), colorFilter = ColorFilter.tint(iconTint))
        }
    }
}

/** 비활성 엣지 데포르메 묶음 블록 (위는 각지고 아래만 둥근 형태). */
private fun DrawScope.drawDeformedControlBlock(
    centerX: Float, top: Float, blockW: Float, blockH: Float,
    bgColor: Color, textLayout: TextLayoutResult
) {
    val left = centerX - blockW / 2f
    val cornerPx = blockH * 0.45f
    drawRoundRect(bgColor, topLeft = Offset(left, top), size = Size(blockW, blockH), cornerRadius = CornerRadius(cornerPx))
    drawRect(bgColor, topLeft = Offset(left, top), size = Size(blockW, cornerPx))
    drawText(textLayout, topLeft = Offset(centerX - textLayout.size.width / 2f, top + (blockH - textLayout.size.height) / 2f))
}

/** 빗금 패턴 차단 영역 (비활성 엣지와 동일한 붉은 스타일) */
private fun DrawScope.drawBlockedRegion(topLeft: Offset, size: Size) {
    drawRect(Color(0xFF1E1E1E).copy(alpha = 0.95f), topLeft, size)
    val step = 7f
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

private fun isInLostCornerBand(
    edge: EntryEdge,
    ratio: Float,
    priority: Map<CornerOverlap, EntryEdge>,
    w: Float, h: Float,
    edgePx: Float,
    hasBottomLeft: Boolean,
    hasBottomRight: Boolean
): Boolean {
    fun p(c: CornerOverlap) = priority[c] ?: defaultCornerEdge(c)
    return when (edge) {
        EntryEdge.TOP -> {
            (ratio <= edgePx / w && p(CornerOverlap.TOP_LEFT) != EntryEdge.TOP) ||
            (ratio >= 1f - edgePx / w && p(CornerOverlap.TOP_RIGHT) != EntryEdge.TOP)
        }
        EntryEdge.BOTTOM -> {
            (!hasBottomLeft  && ratio <= edgePx / w && p(CornerOverlap.BOTTOM_LEFT)  != EntryEdge.BOTTOM) ||
            (!hasBottomRight && ratio >= 1f - edgePx / w && p(CornerOverlap.BOTTOM_RIGHT) != EntryEdge.BOTTOM)
        }
        EntryEdge.LEFT -> {
            (ratio <= edgePx / h && p(CornerOverlap.TOP_LEFT) != EntryEdge.LEFT) ||
            (!hasBottomLeft && ratio >= 1f - edgePx / h && p(CornerOverlap.BOTTOM_LEFT) != EntryEdge.LEFT)
        }
        EntryEdge.RIGHT -> {
            (ratio <= edgePx / h && p(CornerOverlap.TOP_RIGHT) != EntryEdge.RIGHT) ||
            (!hasBottomRight && ratio >= 1f - edgePx / h && p(CornerOverlap.BOTTOM_RIGHT) != EntryEdge.RIGHT)
        }
    }
}

/** 코너 중심점에 우선 엣지 방향을 나타내는 쉐브론 화살표를 그린다. */
private fun DrawScope.drawCornerPriorityIndicator(
    cx: Float, cy: Float,
    priority: EntryEdge,
    edgePx: Float
) {
    val s = edgePx * 0.22f
    val sw = 1.8f
    val col = Color.White.copy(alpha = 0.80f)
    when (priority) {
        EntryEdge.TOP    -> { drawLine(col, Offset(cx - s, cy + s), Offset(cx, cy - s), sw); drawLine(col, Offset(cx, cy - s), Offset(cx + s, cy + s), sw) }
        EntryEdge.BOTTOM -> { drawLine(col, Offset(cx - s, cy - s), Offset(cx, cy + s), sw); drawLine(col, Offset(cx, cy + s), Offset(cx + s, cy - s), sw) }
        EntryEdge.LEFT   -> { drawLine(col, Offset(cx + s, cy - s), Offset(cx - s, cy), sw); drawLine(col, Offset(cx - s, cy), Offset(cx + s, cy + s), sw) }
        EntryEdge.RIGHT  -> { drawLine(col, Offset(cx - s, cy - s), Offset(cx + s, cy), sw); drawLine(col, Offset(cx + s, cy), Offset(cx - s, cy + s), sw) }
    }
}

