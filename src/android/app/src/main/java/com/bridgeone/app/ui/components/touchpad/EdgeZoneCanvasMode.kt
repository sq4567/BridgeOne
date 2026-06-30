package com.bridgeone.app.ui.components.touchpad

import com.bridgeone.app.ui.common.swipe.Direction
import com.bridgeone.app.ui.common.swipe.FocusableElement
import com.bridgeone.app.ui.common.swipe.SwipeFocusController

/**
 * 캔버스 씬에서 존 구조 변경(병합/분할/이동/삭제/비율조정)을 수행하는 모드 상태.
 *
 * 기존 [ZoneActionPopup](단일-zone 모델)은 다중 선택·드래그·cross-edge 드롭을 표현하지 못해
 * 캔버스 전용 모델로 분리했다. `EdgeZoneEditorScreen`이 `remember { mutableStateOf(...) }`로 보유하며
 * (기존 `zonePopupState`가 screen 소유인 컨벤션과 동일), config 변환·Undo는 [EdgeZoneEditorState]에 위임한다.
 */
internal sealed class CanvasEditMode {
    /** 기본 상태: 존 탭 → 편집 씬 진입. */
    object None : CanvasEditMode()

    /**
     * 병합 모드: 같은 엣지 내 인접 존을 다중 선택해 하나로 합친다.
     * @param edge   첫 선택 존의 엣지 (null = 아직 미선택). 다른 엣지 선택은 무시.
     * @param base   기준 존의 startRatio (null = 미선택). 인접 존만 토글 허용.
     * @param selected 병합 대상으로 토글된 인접 존들의 startRatio 집합.
     */
    data class Merging(
        val edge: EntryEdge? = null,
        val base: Float? = null,
        val selected: Set<Float> = emptySet(),
    ) : CanvasEditMode()

    /** 삭제 모드: 존(들)을 다중 선택해 일괄 삭제. cross-edge 다중 선택 허용. */
    data class Deleting(val selected: Set<ZoneKey> = emptySet()) : CanvasEditMode()

    /** 분할 모드: 대상 1개 선택 + 갯수 팝업. */
    data class Splitting(val target: ZoneKey? = null) : CanvasEditMode()

    /**
     * 이동 모드: 들어올린 존을 어느 엣지의 어느 위치로든 재배치.
     * @param picked     들어올린 존 (null = 아직 선택 전)
     * @param dropTarget 현재 가리키는 드롭 위치 (null = 미정)
     */
    data class Moving(
        val picked: ZoneKey? = null,
        val dropTarget: DropTarget? = null,
    ) : CanvasEditMode()

    /** 비율 조정 모드: 캔버스 경계 핸들 드래그 / 비율 프리셋. */
    data class Resizing(val edge: EntryEdge? = null) : CanvasEditMode()
}

/** 캔버스 모드 종류. 진입 버튼·SWIPE 포커스 식별·라벨에 공용 사용. */
enum class CanvasModeKind(val label: String) {
    MERGE("병합"), SPLIT("분할"), MOVE("이동"), DELETE("삭제"), RESIZE("비율")
}

/** 현재 모드의 종류 (None이면 null). */
internal val CanvasEditMode.kind: CanvasModeKind?
    get() = when (this) {
        is CanvasEditMode.Merging  -> CanvasModeKind.MERGE
        is CanvasEditMode.Splitting -> CanvasModeKind.SPLIT
        is CanvasEditMode.Moving   -> CanvasModeKind.MOVE
        is CanvasEditMode.Deleting -> CanvasModeKind.DELETE
        is CanvasEditMode.Resizing -> CanvasModeKind.RESIZE
        CanvasEditMode.None        -> null
    }

/** 종류로 빈 모드 상태 생성. */
internal fun CanvasModeKind.toMode(): CanvasEditMode = when (this) {
    CanvasModeKind.MERGE  -> CanvasEditMode.Merging()
    CanvasModeKind.SPLIT  -> CanvasEditMode.Splitting()
    CanvasModeKind.MOVE   -> CanvasEditMode.Moving()
    CanvasModeKind.DELETE -> CanvasEditMode.Deleting()
    CanvasModeKind.RESIZE -> CanvasEditMode.Resizing()
}

/** 존 동일성 식별 키 (edge + startRatio). 기존 코드의 startRatio 기반 식별 컨벤션과 동일. */
data class ZoneKey(val edge: EntryEdge, val startRatio: Float)

/** EdgeZone → ZoneKey 변환. */
fun EdgeZone.key(): ZoneKey = ZoneKey(edge, startRatio)

/** 이동 드롭 위치: target 엣지의 ratio 지점에 삽입. */
data class DropTarget(val edge: EntryEdge, val ratio: Float)

/**
 * 캔버스 씬 전용 방향 우선 공간 네비게이션 ([SwipeFocusController.moveInterceptor]에 설치).
 *
 * 기본 좌표 cone traversal(|dx|>=|dy| 45도)은 엣지 존↔중앙 모드 버튼처럼 대각선으로 떨어진
 * 요소를 놓치므로, 여기서는 "방향 반평면 안에서 방향축 거리 우선(수직 성분 2.5배 패널티) nearest"로
 * 잡는다. 덕분에 각 엣지 존에서 중앙 방향 스와이프 시 가장 가까운 모드 버튼이 정확히 포커스된다.
 *
 * @return 포커스를 이동했으면 true(일반 traversal 스킵), 해당 방향에 후보가 없으면 false(경계 처리로 위임).
 */
internal fun canvasSpatialNav(
    controller: SwipeFocusController,
    dir: Direction,
    filter: (FocusableElement) -> Boolean = { true },
): Boolean {
    val cur = controller.currentFocus ?: return false
    val curB = controller.boundsOf(cur) ?: return false
    val ccx = (curB.left + curB.right) / 2f
    val ccy = (curB.top + curB.bottom) / 2f
    val candidates = controller.activeEntries().filter {
        it.element != cur && it.bounds.width > 0f && it.bounds.height > 0f && filter(it.element)
    }
    val half = candidates.filter {
        val cx = (it.bounds.left + it.bounds.right) / 2f
        val cy = (it.bounds.top + it.bounds.bottom) / 2f
        when (dir) {
            Direction.LEFT  -> cx < ccx - 1f
            Direction.RIGHT -> cx > ccx + 1f
            Direction.UP    -> cy < ccy - 1f
            Direction.DOWN  -> cy > ccy + 1f
        }
    }
    val next = half.minByOrNull {
        val cx = (it.bounds.left + it.bounds.right) / 2f
        val cy = (it.bounds.top + it.bounds.bottom) / 2f
        val dx = kotlin.math.abs(cx - ccx)
        val dy = kotlin.math.abs(cy - ccy)
        when (dir) {
            Direction.LEFT, Direction.RIGHT -> dx + dy * 2.5f
            Direction.UP, Direction.DOWN    -> dy + dx * 2.5f
        }
    }
    return if (next != null) { controller.setFocus(next.element); true } else false
}

/**
 * 비율 조정 모드에서 엣지 선택 후 프리셋 패널이 열린 상태의 방향 네비게이션
 * ([SwipeFocusController.moveInterceptor]에 설치).
 *
 * 포커스를 프리셋 칩([EdgeEditorElement.CanvasRatioPreset])과 패널 취소 버튼
 * ([EdgeEditorElement.CanvasModeCancel])로만 한정한다. 캔버스 존 hit·경계 핸들로 새지
 * 않도록 **항상 true를 반환**해 기본 traversal 폴백을 차단한다.
 */
internal fun ratioPresetPanelNav(controller: SwipeFocusController, dir: Direction): Boolean {
    canvasSpatialNav(controller, dir) {
        it is EdgeEditorElement.CanvasRatioPreset || it == EdgeEditorElement.CanvasModeCancel
    }
    return true
}

/**
 * 엣지 축 정렬 네비게이션. 캔버스 내부 요소(존/드롭 슬롯)에만 포커스를 한정하고(TopAppBar 등 제외),
 * 현재 요소가 속한 엣지의 축과 **평행한** 방향(세로 엣지=상하, 가로 엣지=좌우)은 같은 엣지 안에서 이동하되,
 * 끝(맨 처음/마지막 슬롯)에 도달하면 그 방향의 **인접 엣지(코너 너머)로 전환**한다
 * (예: 우측 엣지 맨 아래에서 아래로 스와이프 → 하단 엣지). 축에 **수직인** 방향은 다른 엣지로 전환을 허용.
 *
 * 해당 방향에 대상이 없어도 **항상 true를 반환**해 기본 traversal로 폴백하지 않게 한다
 * (폴백 시 좌표 cone 탐색이 TopAppBar 등 캔버스 밖 요소를 포커스할 수 있으므로 차단).
 */
private fun axisAlignedNav(
    controller: SwipeFocusController,
    dir: Direction,
    curEdge: EntryEdge?,
    isTarget: (FocusableElement) -> Boolean,
    edgeOf: (FocusableElement) -> EntryEdge?,
): Boolean {
    if (curEdge == null) {
        canvasSpatialNav(controller, dir, isTarget)
        return true
    }
    val vertical = curEdge == EntryEdge.LEFT || curEdge == EntryEdge.RIGHT
    val parallel = if (vertical) dir == Direction.UP || dir == Direction.DOWN
                   else dir == Direction.LEFT || dir == Direction.RIGHT
    if (parallel) {
        // 같은 엣지 안에서 이동 → 끝에 도달하면 진행 방향의 인접(코너 너머) 엣지로 전환
        val moved = canvasSpatialNav(controller, dir) { isTarget(it) && edgeOf(it) == curEdge }
        if (!moved) {
            cornerAdjacentEdge(curEdge, dir)?.let { nextEdge ->
                focusNearestOnEdge(controller, nextEdge, isTarget, edgeOf)
            }
        }
    } else if (!isOutwardDirection(curEdge, dir)) {
        // 축에 수직이면서 화면 안쪽 방향 → 다른 엣지로 전환.
        // 바깥(엣지 너머) 방향은 공간이 없으므로 아무 동작 안 함(엉뚱한 엣지로 새지 않게).
        canvasSpatialNav(controller, dir, isTarget)
    }
    return true
}

/** 해당 엣지에서 화면 바깥(엣지 너머)을 향하는 방향인지. 좌엣지=왼쪽, 우엣지=오른쪽, 상엣지=위, 하엣지=아래. */
private fun isOutwardDirection(edge: EntryEdge, dir: Direction): Boolean = when (edge) {
    EntryEdge.LEFT   -> dir == Direction.LEFT
    EntryEdge.RIGHT  -> dir == Direction.RIGHT
    EntryEdge.TOP    -> dir == Direction.UP
    EntryEdge.BOTTOM -> dir == Direction.DOWN
}

/** 평행 방향 스와이프가 엣지 끝에 도달했을 때, 그 방향으로 코너를 공유하는 인접 엣지. 없으면 null. */
private fun cornerAdjacentEdge(curEdge: EntryEdge, dir: Direction): EntryEdge? = when (curEdge) {
    EntryEdge.LEFT, EntryEdge.RIGHT -> when (dir) {
        Direction.DOWN -> EntryEdge.BOTTOM
        Direction.UP   -> EntryEdge.TOP
        else -> null
    }
    EntryEdge.TOP, EntryEdge.BOTTOM -> when (dir) {
        Direction.RIGHT -> EntryEdge.RIGHT
        Direction.LEFT  -> EntryEdge.LEFT
        else -> null
    }
}

/** 지정 엣지의 대상 요소 중 현재 포커스 중심에 가장 가까운(공유 코너 최근접) 것으로 포커스 이동. */
private fun focusNearestOnEdge(
    controller: SwipeFocusController,
    edge: EntryEdge,
    isTarget: (FocusableElement) -> Boolean,
    edgeOf: (FocusableElement) -> EntryEdge?,
): Boolean {
    val cur = controller.currentFocus ?: return false
    val curB = controller.boundsOf(cur) ?: return false
    val ccx = (curB.left + curB.right) / 2f
    val ccy = (curB.top + curB.bottom) / 2f
    val next = controller.activeEntries()
        .filter {
            it.element != cur && it.bounds.width > 0f && it.bounds.height > 0f &&
                isTarget(it.element) && edgeOf(it.element) == edge
        }
        .minByOrNull {
            val cx = (it.bounds.left + it.bounds.right) / 2f
            val cy = (it.bounds.top + it.bounds.bottom) / 2f
            val dx = cx - ccx; val dy = cy - ccy
            dx * dx + dy * dy
        }
    return if (next != null) { controller.setFocus(next.element); true } else false
}

/** 이동 드롭 단계: 드롭 슬롯끼리, 엣지 축 정렬. */
internal fun movingDropNav(controller: SwipeFocusController, dir: Direction): Boolean {
    val cur = controller.currentFocus as? EdgeEditorElement.CanvasDropSlot
    return axisAlignedNav(
        controller, dir, cur?.edge,
        isTarget = { it is EdgeEditorElement.CanvasDropSlot },
        edgeOf = { (it as? EdgeEditorElement.CanvasDropSlot)?.edge },
    )
}

/** 이동 픽 단계: 존끼리(+중앙 '확인' 버튼), 엣지 축 정렬. 확인은 엣지에서 안쪽으로 스와이프하면 닿는다. */
internal fun movingPickNav(controller: SwipeFocusController, dir: Direction): Boolean {
    val cur = controller.currentFocus as? EdgeEditorElement.CanvasZone
    return axisAlignedNav(
        controller, dir, cur?.edge,
        isTarget = { it is EdgeEditorElement.CanvasZone || it is EdgeEditorElement.CanvasModeConfirm },
        edgeOf = { (it as? EdgeEditorElement.CanvasZone)?.edge },
    )
}

/** 현재 모드 상태에서 캔버스 Canvas에 강조(초록 테두리)할 존 키 집합. */
internal fun canvasHighlightKeys(mode: CanvasEditMode, selectedZone: EdgeZone?, config: EdgeZoneConfig): Set<ZoneKey> = when (mode) {
    is CanvasEditMode.None     -> selectedZone?.let { setOf(it.key()) } ?: emptySet()
    is CanvasEditMode.Deleting -> mode.selected
    is CanvasEditMode.Merging  -> {
        val e = mode.edge
        if (e == null) emptySet()
        else (mode.selected + (mode.base?.let { setOf(it) } ?: emptySet())).map { ZoneKey(e, it) }.toSet()
    }
    is CanvasEditMode.Splitting -> mode.target?.let { setOf(it) } ?: emptySet()
    // 이동: 선택 존은 들림 고스트(주황 반투명)로 표시하므로 초록 강조하지 않는다.
    is CanvasEditMode.Moving    -> emptySet()
    // 비율 조정: 대상 엣지 선택 시 그 엣지의 모든 존을 강조해 적용 대상을 명확히 표시
    is CanvasEditMode.Resizing  -> mode.edge?.let { e -> config.zonesFor(e).map { it.key() }.toSet() } ?: emptySet()
}
