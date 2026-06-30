package com.bridgeone.app.ui.components.touchpad

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.bridgeone.app.ui.common.EdgeSwipeConstants
import com.bridgeone.app.ui.common.ZoneMoveMethod

/**
 * 캔버스 씬 모드별 포인터 입력 Modifier (Phase 4.7.8-C — EdgeZoneCanvasModeOverlay에서 추출).
 *
 * NORMAL 모드 전용. SWIPE는 SwipeGestureLayer가 가로채므로 여기선 무효.
 * - [CanvasEditMode.Resizing]: 경계 드래그(onResize) + 존 탭(프리셋 대상 엣지 선택)
 * - [CanvasEditMode.Moving] + DRAG_AND_DROP: 존 드래그 앤 드롭
 * - [CanvasEditMode.Moving] + TAP: 존 탭 픽, 경계/끝 탭 드롭, 롱프레스 취소
 * - 그 외 활성 모드: 존 탭 → [onZoneInteract]
 * - [CanvasEditMode.None]: 투명 통과
 *
 * Geometry 함수([findZoneAt] 등)만 호출하므로 로직이 원본과 byte-identical하게 보존된다.
 * 단, 다음 두 가지 구조적 개선이 적용됨:
 *   - [findZoneAt] 5회 중복 호출을 지역 헬퍼 `zoneAt`으로 묶어 인라인 제거
 *   - [CanvasEditMode.Resizing] 탭 핸들러의 토스트 직접 호출 → [onEdgeBlocked] 콜백으로 호이스트
 */
internal fun Modifier.canvasModeInput(
    density: Density,
    canvasMode: CanvasEditMode,
    config: EdgeZoneConfig,
    disabledEdges: Map<EntryEdge, String>,
    bottomLeftButtonLabel: String?,
    bottomRightButtonLabel: String?,
    blockedRatio: Float,
    moveMethod: ZoneMoveMethod,
    onZoneInteract: (EdgeZone) -> Unit,
    onResizeStart: () -> Unit,
    onResize: (edge: EntryEdge, leftIndex: Int, newRatio: Float) -> Unit,
    onMovingPick: (EdgeZone) -> Unit,
    onMovingDrag: (DropTarget) -> Unit,
    onMovingDragEnd: () -> Unit,
    onMovingCancel: () -> Unit,
    onMovingLongCancel: () -> Unit,
    onMovingDropTap: (edge: EntryEdge, ratio: Float) -> Unit,
    // 비활성 엣지(예: 상단 제어 버튼) 탭 시 호출. 차단 사유 문구는 호출자가 결정(사이드이펙트 격리).
    onEdgeBlocked: (EntryEdge) -> Unit,
): Modifier {
    val hasBottomLeft = bottomLeftButtonLabel != null
    val hasBottomRight = bottomRightButtonLabel != null

    /** findZoneAt 공통 인자 묶음. blockedRatio는 findZoneAt 기본값(CORNER_BUTTON_BLOCKED_RATIO) 유지. */
    fun zoneAt(offset: Offset, w: Float, h: Float): EdgeZone? {
        val edgePx = with(density) { EdgeSwipeConstants.EDGE_HIT_WIDTH_DP.dp.toPx() }
        return findZoneAt(offset, config, w, h, edgePx, disabledEdges.keys, hasBottomLeft, hasBottomRight)
    }

    return when {
        canvasMode is CanvasEditMode.Resizing -> this
            .pointerInput(canvasMode) {
                var boundary: BoundaryHit? = null
                detectDragGestures(
                    onDragStart = { offset ->
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        val edgePx = with(density) { EdgeSwipeConstants.EDGE_HIT_WIDTH_DP.dp.toPx() }
                        val threshold = with(density) { EdgeSwipeConstants.ZONE_BOUNDARY_DRAG_HIT_DP.dp.toPx() }
                        boundary = findBoundaryAt(offset, config, w, h, edgePx, threshold, disabledEdges.keys, hasBottomLeft, hasBottomRight, blockedRatio)
                        if (boundary != null) onResizeStart()
                    },
                    onDrag = { change, _ ->
                        val b = boundary ?: return@detectDragGestures
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        val vertical = b.edge == EntryEdge.LEFT || b.edge == EntryEdge.RIGHT
                        val edgeLen = if (vertical) h else w
                        val along = if (vertical) change.position.y else change.position.x
                        onResize(b.edge, b.leftIndex, (along / edgeLen).coerceIn(0f, 1f))
                    },
                    onDragEnd = { boundary = null },
                    onDragCancel = { boundary = null },
                )
            }
            .pointerInput("resizeTap", canvasMode) {
                // 존 탭 → 비율 프리셋 대상 엣지 선택 (경계 드래그와 별도 제스처)
                detectTapGestures { offset ->
                    val w = size.width.toFloat()
                    val h = size.height.toFloat()
                    val zone = zoneAt(offset, w, h)
                    if (zone != null) {
                        onZoneInteract(zone)
                    } else {
                        // 비활성 엣지(예: 상단 제어 버튼) 탭 → 비율 조정 불가 안내.
                        val edgePx = with(density) { EdgeSwipeConstants.EDGE_HIT_WIDTH_DP.dp.toPx() }
                        val band = edgeBandAt(offset, w, h, edgePx)
                        if (band != null && band in disabledEdges) {
                            onEdgeBlocked(band)
                        }
                    }
                }
            }
        // 이동 모드 + 드래그 앤 드롭: 존을 잡아 끌면 실시간 미리보기, 릴리스 시 안착
        canvasMode is CanvasEditMode.Moving && moveMethod == ZoneMoveMethod.DRAG_AND_DROP -> this
            // key는 config로 둔다. onMovingPick이 바꾸는 canvasMode를 key로 쓰면 들어올림 즉시
            // pointerInput이 재시작되어 진행 중인 detectDragGestures가 취소된다(자기참조 버그).
            // config는 드래그 도중엔 불변(commit 시점에만 갱신)이라 제스처가 유지된다.
            .pointerInput(config) {
                var picked = false
                detectDragGestures(
                    onDragStart = { offset ->
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        val zone = zoneAt(offset, w, h)
                        if (zone != null) { onMovingPick(zone); picked = true }
                    },
                    onDrag = { change, _ ->
                        if (!picked) return@detectDragGestures
                        onMovingDrag(dropTargetAt(change.position, size.width.toFloat(), size.height.toFloat(), hasBottomLeft, hasBottomRight, blockedRatio, disabledEdges.keys))
                    },
                    onDragEnd = { onMovingDragEnd(); picked = false },
                    onDragCancel = { onMovingCancel(); picked = false },
                )
            }
        // 이동 모드 + 탭: picked 없으면 존 선택(들어올림), 있으면 경계/양 끝 탭으로 드롭. 롱프레스로 취소.
        canvasMode is CanvasEditMode.Moving -> this.pointerInput(canvasMode) {
            detectTapGestures(
                onLongPress = { onMovingLongCancel() },
            ) { offset ->
                val w = size.width.toFloat()
                val h = size.height.toFloat()
                val edgePx = with(density) { EdgeSwipeConstants.EDGE_HIT_WIDTH_DP.dp.toPx() }
                val threshold = with(density) { EdgeSwipeConstants.ZONE_BOUNDARY_DRAG_HIT_DP.dp.toPx() }
                val moving = canvasMode as CanvasEditMode.Moving
                val picked = moving.picked
                if (picked == null) {
                    zoneAt(offset, w, h)?.let { onMovingPick(it) }
                } else {
                    val b = findBoundaryAt(offset, config, w, h, edgePx, threshold, disabledEdges.keys, hasBottomLeft, hasBottomRight, blockedRatio)
                    if (b != null) {
                        val ratio = config.zonesFor(b.edge).getOrNull(b.leftIndex + 1)?.startRatio
                        if (ratio != null) onMovingDropTap(b.edge, ratio)
                    } else {
                        val ar = edgeAlongRatioAt(offset, w, h, edgePx, disabledEdges.keys, hasBottomLeft, hasBottomRight, blockedRatio)
                        val endRatio = EdgeSwipeConstants.EDGE_END_DROP_RATIO
                        if (ar != null && (ar.second < endRatio || ar.second > 1f - endRatio)) {
                            onMovingDropTap(ar.first, if (ar.second < 0.5f) 0f else 1f)
                        } else {
                            // 존 재탭(같은 존) = 취소
                            val z = zoneAt(offset, w, h)
                            if (z != null && z.key() == picked) onMovingCancel()
                        }
                    }
                }
            }
        }
        canvasMode !is CanvasEditMode.None -> this.pointerInput(canvasMode) {
            detectTapGestures { offset ->
                val w = size.width.toFloat()
                val h = size.height.toFloat()
                val zone = zoneAt(offset, w, h)
                if (zone != null) onZoneInteract(zone)
            }
        }
        else -> this
    }
}
