package com.bridgeone.app.ui.components.touchpad

import com.bridgeone.app.ui.common.EdgeSwipeConstants
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * EdgeZoneEditorScreen의 편집 상태 홀더 (Phase 4.7.5-A).
 *
 * `StandardModePageState`와 동일하게 평범한 클래스 + `mutableStateOf` 컨벤션을 따른다.
 * config 변환·Undo 같은 순수 상태 전이만 담당하며, 팝업(`zonePopup`)·토스트 등 UI 사이드이펙트는
 * Composable이 담당한다. 예: `splitInto`/`tryMergeWith`는 `workConfig`/`selectedZone`만 갱신하고,
 * `zonePopup = ZoneActionPopup.None` 리셋은 호출부에서 수행한다.
 *
 * Composable은 기존 지역 변수명을 유지하기 위해 노출된 `MutableState`에 위임한다:
 * `var workConfig by state.workConfigState`.
 */
class EdgeZoneEditorState(
    initialConfig: EdgeZoneConfig,
    initialPresetId: String?,
) {
    val workConfigState: MutableState<EdgeZoneConfig> = mutableStateOf(initialConfig)
    val selectedZoneState: MutableState<EdgeZone?> = mutableStateOf(null)
    val currentPresetIdState: MutableState<String?> = mutableStateOf(initialPresetId)
    val undoStackState: MutableState<List<EdgeZoneConfig>> = mutableStateOf(emptyList())

    var workConfig by workConfigState
    var selectedZone by selectedZoneState
    var currentPresetId by currentPresetIdState
    var undoStack by undoStackState

    /** 비율 조정 경계 전환(되돌리기·프리셋 적용) 시 from→to 보간 애니메이션 트리거. null이면 애니메이션 없음. */
    var ratioMorph: ConfigMorph? by mutableStateOf(null)

    /** 이동 모드 롱프레스 '되돌리고 나가기' 요청 플래그. 화면이 역순 되돌리기 애니메이션을 구동하고 끝나면 false로 되돌린다. */
    var moveRevertRequested: Boolean by mutableStateOf(false)

    /** 현재 config를 Undo 스택 맨 앞에 push (최대 20개 유지). */
    fun pushUndo() {
        undoStack = (listOf(workConfig) + undoStack).take(20)
    }

    /**
     * 분할 N개 균등 분할 (첫 조각만 기존 trigger 유지).
     * @return 분할이 적용되면 true, 대상 존을 찾지 못해 무시되면 false.
     */
    fun splitInto(zone: EdgeZone, n: Int): Boolean {
        val zones = workConfig.zonesFor(zone.edge).toMutableList()
        val idx = zones.indexOfFirst { it.startRatio == zone.startRatio && it.edge == zone.edge }
        if (idx < 0) return false
        val w = (zone.endRatio - zone.startRatio) / n
        val parts = (0 until n).map { i ->
            val s = zone.startRatio + i * w
            val e = if (i == n - 1) zone.endRatio else s + w
            if (i == 0) zone.copy(endRatio = e)
            else EdgeZone(zone.edge, s, e, EdgeZoneTrigger.SingleAction(EdgeZoneAction.Unassigned, "", ""))
        }
        zones.removeAt(idx)
        zones.addAll(idx, parts)
        pushUndo()
        workConfig = workConfig.withZones(zone.edge, zones)
        currentPresetId = null
        selectedZone = parts.first()
        return true
    }

    /**
     * 인접 존 흡수 병합.
     * @return 병합이 적용되면 true, 엣지 불일치/비인접 등으로 무시되면 false.
     */
    fun tryMergeWith(base: EdgeZone, target: EdgeZone): Boolean {
        if (base.edge != target.edge) return false
        val zones = workConfig.zonesFor(base.edge).toMutableList()
        val bi = zones.indexOfFirst { it.startRatio == base.startRatio }
        val ti = zones.indexOfFirst { it.startRatio == target.startRatio }
        if (bi < 0 || ti < 0 || kotlin.math.abs(bi - ti) != 1) return false
        val merged = if (ti < bi) base.copy(startRatio = target.startRatio)
                     else base.copy(endRatio = target.endRatio)
        zones[bi] = merged
        zones.removeAt(ti)
        pushUndo()
        workConfig = workConfig.withZones(base.edge, zones)
        currentPresetId = null
        selectedZone = merged
        return true
    }

    /**
     * 여러 인접 존을 기준 존에 흡수 병합 (SWIPE 모드 다중 타겟 병합).
     * @param targetStartRatios 병합할 인접 존들의 startRatio 집합 (빈 집합이면 false)
     * @return 병합이 적용되면 true
     */
    fun tryMergeWithTargets(base: EdgeZone, targetStartRatios: Set<Float>): Boolean {
        if (targetStartRatios.isEmpty()) return false
        val zones = workConfig.zonesFor(base.edge).toMutableList()
        val bi = zones.indexOfFirst { it.startRatio == base.startRatio }
        if (bi < 0) return false
        val leftZone = if (bi > 0 && targetStartRatios.contains(zones[bi - 1].startRatio)) zones[bi - 1] else null
        val rightZone = if (bi < zones.size - 1 && targetStartRatios.contains(zones[bi + 1].startRatio)) zones[bi + 1] else null
        if (leftZone == null && rightZone == null) return false
        val merged = base.copy(
            startRatio = leftZone?.startRatio ?: base.startRatio,
            endRatio = rightZone?.endRatio ?: base.endRatio
        )
        if (rightZone != null) zones.removeAt(bi + 1)
        zones[bi] = merged
        if (leftZone != null) zones.removeAt(bi - 1)
        pushUndo()
        workConfig = workConfig.withZones(base.edge, zones)
        currentPresetId = null
        selectedZone = merged
        return true
    }

    // ── 캔버스 이동 모드 (CanvasEditMode.Moving) ──
    // 존을 출발 엣지에서 빼 다른(또는 같은) 엣지의 지정 위치로 옮긴다. cross-edge 지원.

    /** 이동 거부 사유. message는 토스트에 그대로 노출. */
    sealed class MoveRejection(val message: String) {
        object SourceLastZone : MoveRejection("이 존은 옮길 수 없어요")
        object TargetFull     : MoveRejection("가장자리가 가득 차 더 넣을 수 없어요")
        object TooSmall       : MoveRejection("존이 너무 작아져요")
        object DisabledEdge   : MoveRejection("이 엣지에는 놓을 수 없어요")
    }

    /** 존 캔버스 탭 상호작용 거부 사유. message는 토스트에 그대로 노출. */
    sealed class InteractRejection(val message: String) {
        object DeleteLastZone   : InteractRejection("엣지에 존이 하나뿐이라 삭제할 수 없어요")
        data class SplitFull(val max: Int) : InteractRejection("존이 ${max}개로 가득 차 더 나눌 수 없어요")
        object ResizeSingleZone : InteractRejection("존이 하나뿐인 가장자리는 비율을 나눌 수 없어요")
        object MergeCrossEdge   : InteractRejection("같은 가장자리의 인접한 존만 선택할 수 있어요")
        object MergeNonAdjacent : InteractRejection("인접한 존만 선택할 수 있어요")
    }

    /**
     * 드롭 지점 비율(0~1)을 [targetEdge]의 삽입 인덱스(0=맨 앞 … size=맨 끝)로 변환.
     * 같은 엣지로 옮길 때는 [excludePicked]를 제외한 리스트 기준으로 계산한다.
     * 경계(존 사이)와 양 끝을 단일 인덱스로 통일 표현.
     */
    fun dropInsertIndex(targetEdge: EntryEdge, ratio: Float, excludePicked: ZoneKey?): Int {
        val zones = workConfig.zonesFor(targetEdge).let { list ->
            if (excludePicked != null && excludePicked.edge == targetEdge)
                list.filterNot { it.startRatio == excludePicked.startRatio }
            else list
        }
        // 후보 위치 비율: [0f] + 내부 경계(각 존의 startRatio, 첫 존 제외) + [1f] → size+1개
        val posRatios = buildList {
            add(0f)
            zones.drop(1).forEach { add(it.startRatio) }
            add(1f)
        }
        var bestIdx = 0
        var bestDist = Float.MAX_VALUE
        posRatios.forEachIndexed { i, pos ->
            val d = kotlin.math.abs(pos - ratio)
            if (d < bestDist) { bestDist = d; bestIdx = i }
        }
        return bestIdx
    }

    /** 이동 가능 여부 검증. null이면 허용. */
    fun validateMove(
        picked: ZoneKey,
        targetEdge: EntryEdge,
        insertIndex: Int,
        disabledEdges: Set<EntryEdge>,
    ): MoveRejection? {
        if (workConfig.zonesFor(picked.edge).size <= 1) return MoveRejection.SourceLastZone
        if (targetEdge in disabledEdges) return MoveRejection.DisabledEdge
        if (targetEdge != picked.edge &&
            workConfig.zonesFor(targetEdge).size + 1 > EdgeSwipeConstants.MAX_ZONES_PER_EDGE.toInt()
        ) return MoveRejection.TargetFull
        val result = computeMove(picked, targetEdge, insertIndex) ?: return MoveRejection.SourceLastZone
        val minR = EdgeSwipeConstants.MIN_ZONE_RATIO
        val affected = if (targetEdge == picked.edge) setOf(targetEdge) else setOf(picked.edge, targetEdge)
        val tooSmall = affected.any { e ->
            result.zonesFor(e).any { (it.endRatio - it.startRatio) < minR - 1e-4f }
        }
        if (tooSmall) return MoveRejection.TooSmall
        return null
    }

    /**
     * 이동 결과 config를 계산하는 순수 함수 (미리보기·검증·모핑 프레임 공용, workConfig 불변).
     * - 같은 엣지: 순서만 바꾸고 폭 보존.
     * - cross-edge: 출발 엣지는 P 제거 후 [0,1] 재정규화, 도착 엣지는 P(폭 carry) 삽입 후 재정규화.
     * trigger(액션/라벨/아이콘/Rotation 후보)는 전부 보존. 대상 존을 못 찾으면 null.
     */
    fun computeMove(picked: ZoneKey, targetEdge: EntryEdge, insertIndex: Int): EdgeZoneConfig? {
        val sourceZones = workConfig.zonesFor(picked.edge)
        val p = sourceZones.firstOrNull { it.startRatio == picked.startRatio } ?: return null

        if (targetEdge == picked.edge) {
            // insertIndex는 picked를 제외한 리스트 기준(dropInsertIndex·드롭 슬롯과 동일 컨벤션) → 직접 삽입.
            val list = sourceZones.filterNot { it.startRatio == p.startRatio }.toMutableList()
            list.add(insertIndex.coerceIn(0, list.size), p)
            return workConfig.withZones(targetEdge, recomputeRatiosPreservingWidth(list))
        }

        // cross-edge
        val src = sourceZones.filterNot { it.startRatio == p.startRatio }
        val srcFilled = rescaleToFill(src)
        val tgt = workConfig.zonesFor(targetEdge).toMutableList()
        tgt.add(insertIndex.coerceIn(0, tgt.size), p.copy(edge = targetEdge))
        val tgtFilled = rescaleToFill(tgt)
        return workConfig.withZones(picked.edge, srcFilled).withZones(targetEdge, tgtFilled)
    }

    /**
     * 이동 확정. 검증 통과를 가정(호출부가 [validateMove]로 사전 확인).
     * pushUndo 후 적용하고 selectedZone을 해제(이동 직후 편집 씬 진입 방지, 병합/분할과 동일 컨벤션).
     * @return 출발/도착 엣지 모핑용 ZoneMorph 리스트(없으면 빈 리스트).
     */
    fun commitMove(picked: ZoneKey, targetEdge: EntryEdge, insertIndex: Int): List<ZoneMorph> {
        val before = workConfig
        val pickedZone = before.zonesFor(picked.edge).firstOrNull { it.startRatio == picked.startRatio } ?: return emptyList()
        val after = computeMove(picked, targetEdge, insertIndex) ?: return emptyList()
        if (after == before) return emptyList()  // 원위치 드롭 = no-op (Undo 미적립)
        pushUndo()
        workConfig = after
        currentPresetId = null
        selectedZone = null

        // 선택 존은 떠다니는 오버레이가 직접 이동시키므로, morph는 이웃(P 제외)만 보간한다.
        return buildMoveReflowMorphs(before, after, pickedZone.trigger, picked.edge, targetEdge)
    }

    /** 존 리스트를 폭 비율 보존하며 [0,1] 전체로 재분배(합이 1이 아니어도 정규화). */
    private fun rescaleToFill(zones: List<EdgeZone>): List<EdgeZone> {
        if (zones.isEmpty()) return zones
        val total = zones.sumOf { (it.endRatio - it.startRatio).toDouble() }.toFloat()
        if (total <= 0f) return zones
        var cum = 0f
        return zones.mapIndexed { idx, z ->
            val w = (z.endRatio - z.startRatio) / total
            val s = cum; cum += w
            val e = if (idx == zones.size - 1) 1f else cum
            z.copy(startRatio = s, endRatio = e)
        }
    }

    /** 리스트 순서대로 비율을 재배치, 각 존의 폭(end-start) 보존. */
    private fun recomputeRatiosPreservingWidth(zones: List<EdgeZone>): List<EdgeZone> {
        if (zones.isEmpty()) return zones
        // 파티션 왼쪽 끝에서 시작. 순서가 바뀌어도 startRatio 최솟값 = 원래 첫 존의 시작점(보통 0f).
        // 교환 후 zones.first()를 읽으면 다른 존의 startRatio라 비율이 통째로 밀리는 버그가 생긴다.
        var cum = zones.minOf { it.startRatio }
        return zones.mapIndexed { idx, z ->
            val width = z.endRatio - z.startRatio
            val s = cum; cum += width
            val e = if (idx == zones.size - 1) 1f else cum
            z.copy(startRatio = s, endRatio = e)
        }
    }

    /** 존 삭제. 마지막 1개는 삭제하지 않으며, 빈 비율은 인접 존이 흡수한다. */
    fun deleteZone(zone: EdgeZone) {
        val zones = workConfig.zonesFor(zone.edge).toMutableList()
        val idx = zones.indexOf(zone)
        if (idx < 0 || zones.size <= 1) return
        val removed = zones.removeAt(idx)
        if (idx < zones.size) zones[idx] = zones[idx].copy(startRatio = removed.startRatio)
        else zones[idx - 1] = zones[idx - 1].copy(endRatio = removed.endRatio)
        pushUndo()
        workConfig = workConfig.withZones(zone.edge, zones)
        if (selectedZone == zone) selectedZone = null
    }

    /**
     * 여러 존을 한 번의 Undo로 일괄 삭제 (캔버스 삭제 모드).
     * 엣지별로 startRatio 내림차순 삭제해 남은 선택 존의 startRatio가 흔들리지 않게 한다.
     * 각 엣지는 최소 1개 존을 보존한다(deleteZone과 동일 불변식). 빈 비율은 인접 존이 흡수.
     */
    fun deleteZones(keys: Set<ZoneKey>) {
        if (keys.isEmpty()) return
        pushUndo()
        var cfg = workConfig
        keys.groupBy { it.edge }.forEach { (edge, edgeKeys) ->
            val zones = cfg.zonesFor(edge).toMutableList()
            edgeKeys.sortedByDescending { it.startRatio }.forEach { key ->
                if (zones.size <= 1) return@forEach
                val idx = zones.indexOfFirst { it.startRatio == key.startRatio }
                if (idx < 0) return@forEach
                val removed = zones.removeAt(idx)
                if (idx < zones.size) zones[idx] = zones[idx].copy(startRatio = removed.startRatio)
                else zones[idx - 1] = zones[idx - 1].copy(endRatio = removed.endRatio)
            }
            cfg = cfg.withZones(edge, zones)
        }
        workConfig = cfg
        currentPresetId = null
        selectedZone = null
    }

    /**
     * 같은 엣지의 연속 인접 존들을 하나로 병합 (캔버스 병합 모드).
     * 병합 결과 존은 [baseStartRatio]가 가리키는 존의 trigger(액션/라벨/아이콘)를 유지하고
     * 선택 구간 전체(startRatios의 최좌~최우)로 범위를 확장한다.
     * @return 병합되면 true, 2개 미만/비연속/존 못 찾음이면 false.
     */
    fun mergeContiguous(edge: EntryEdge, baseStartRatio: Float, startRatios: Set<Float>): Boolean {
        if (startRatios.size < 2) return false
        val zones = workConfig.zonesFor(edge)
        val indices = startRatios.mapNotNull { sr ->
            zones.indexOfFirst { it.startRatio == sr }.takeIf { it >= 0 }
        }.sorted()
        if (indices.size != startRatios.size) return false
        // 연속 인접만 허용 (인덱스가 빈틈없이 이어져야 함)
        if (indices.last() - indices.first() != indices.size - 1) return false
        val baseZone = zones.firstOrNull { it.startRatio == baseStartRatio } ?: return false
        val first = indices.first()
        val last = indices.last()
        val merged = baseZone.copy(startRatio = zones[first].startRatio, endRatio = zones[last].endRatio)
        val newZones = zones.toMutableList()
        for (i in last downTo first) newZones.removeAt(i)
        newZones.add(first, merged)
        pushUndo()
        workConfig = workConfig.withZones(edge, newZones)
        currentPresetId = null
        selectedZone = merged
        return true
    }

    /**
     * 인접 두 존 사이 경계를 [newRatio]로 이동해 비율을 조정 (캔버스 비율 조정 모드, 드래그/manipulation).
     * MIN_ZONE_RATIO를 보장하도록 clamp한다. Undo는 드래그 시작 시 호출부가 [pushUndo]로 1회만 쌓는다(매 이동마다 X).
     * @param leftIndex 경계 왼쪽 존의 인덱스 (경계는 leftIndex와 leftIndex+1 사이)
     */
    fun adjustBoundary(edge: EntryEdge, leftIndex: Int, newRatio: Float) {
        val zones = workConfig.zonesFor(edge).toMutableList()
        if (leftIndex < 0 || leftIndex >= zones.size - 1) return
        val left = zones[leftIndex]
        val right = zones[leftIndex + 1]
        val minR = EdgeSwipeConstants.MIN_ZONE_RATIO
        val r = newRatio.coerceIn(left.startRatio + minR, right.endRatio - minR)
        zones[leftIndex] = left.copy(endRatio = r)
        zones[leftIndex + 1] = right.copy(startRatio = r)
        workConfig = workConfig.withZones(edge, zones)
        currentPresetId = null
    }

    // ── 비율 조정 세션 (취소 시 일괄 되돌리기용) ──
    // 두 레벨: 모드 세션(모드 진입~)과 엣지 세션(엣지 선택~). 안내 카드 취소는 모드 세션, 패널 취소는 엣지 세션을 되돌린다.
    private fun resizeSnapshot() = Triple(workConfig, undoStack, currentPresetId)
    private fun restoreResize(s: Triple<EdgeZoneConfig, List<EdgeZoneConfig>, String?>?) {
        s?.let { (cfg, stack, presetId) ->
            workConfig = cfg
            undoStack = stack
            currentPresetId = presetId
        }
    }

    private var resizeModeSnapshot: Triple<EdgeZoneConfig, List<EdgeZoneConfig>, String?>? = null
    private var resizeSessionSnapshot: Triple<EdgeZoneConfig, List<EdgeZoneConfig>, String?>? = null

    /** 비율 조정 모드 진입 시 — 안내 카드 단계의 '취소'로 모드 진입 이후 변경(경계/프리셋) 전체를 되돌릴 수 있게 기록. */
    fun beginResizeMode() { resizeModeSnapshot = resizeSnapshot() }

    /** 안내 카드 '취소' — 모드 진입 이후 변경을 모두 되돌린다(경계 복원 애니메이션 동반). */
    fun discardResizeMode() {
        val from = workConfig
        restoreResize(resizeModeSnapshot)
        resizeModeSnapshot = null
        resizeSessionSnapshot = null
        if (from != workConfig) ratioMorph = ConfigMorph(from, workConfig)
    }

    /** 안내 카드 '확인' — 변경을 확정하고 세션 스냅샷을 폐기. */
    fun commitResizeMode() {
        resizeModeSnapshot = null
        resizeSessionSnapshot = null
    }

    /** 비율 조정 엣지를 새로 선택할 때 — 패널 '취소'로 그 엣지의 프리셋/경계 변경을 되돌릴 수 있게 기록. */
    fun beginResizeSession() { resizeSessionSnapshot = resizeSnapshot() }

    /** 패널 '취소' — 엣지 세션 시작 이후의 변경을 되돌린다(경계 복원 애니메이션 동반). */
    fun discardResizeSession() {
        val from = workConfig
        restoreResize(resizeSessionSnapshot)
        resizeSessionSnapshot = null
        if (from != workConfig) ratioMorph = ConfigMorph(from, workConfig)
    }

    /**
     * SWIPE 경계 조작(MANIPULATION) 중 롱프레스 되돌리기 — 이번 이동을 1회 undo로 복원(경계 복원 애니메이션 동반).
     * 조작 진입 시 1회 pushUndo 되어 있으므로 undo 1회로 시작 위치로 돌아간다. undo 스택이 비면 no-op.
     */
    fun revertBoundaryManipulation() {
        val prev = undoStack.firstOrNull() ?: return
        val from = workConfig
        workConfig = prev
        undoStack = undoStack.drop(1)
        currentPresetId = null
        if (from != workConfig) ratioMorph = ConfigMorph(from, workConfig)
    }

    // ── 이동 모드 세션 (롱프레스 역순 되돌리기용) ──
    // 모드 진입 시점의 Undo 스택 크기를 기록해, 그 이후 커밋된 이동만 역순으로 되돌린다(이전 편집은 보존).
    private var moveModeStartStackSize: Int = 0

    /** 이동 모드 진입 시 — 이후 커밋된 이동을 롱프레스로 역순 되돌릴 수 있게 시작 지점을 기록. */
    fun beginMoveMode() { moveModeStartStackSize = undoStack.size }

    /** 이동 모드 세션 동안 역순으로 되돌릴 이동이 남아 있는지. */
    fun canRevertMove(): Boolean = undoStack.size > moveModeStartStackSize

    /**
     * 이동 모드 한 단계 역순 되돌리기 — 직전 config로 복원(morph 애니메이션은 호출부가 담당).
     * 세션 시작 지점까지만 되돌리며, 그 이전 편집은 건드리지 않는다.
     */
    fun popMoveUndo() {
        if (undoStack.size <= moveModeStartStackSize) return
        val prev = undoStack.firstOrNull() ?: return
        workConfig = prev
        undoStack = undoStack.drop(1)
        currentPresetId = null
    }

    /** 존 비율 프리셋 적용. ratios 개수가 존 개수와 다르면 무시. */
    fun applyRatioPreset(edge: EntryEdge, ratios: List<Float>) {
        val zones = workConfig.zonesFor(edge).toList()
        val newZones = computeRatioZones(zones, ratios) ?: return
        val curSel = selectedZone
        val selIdx = if (curSel != null) zones.indexOfFirst { it.startRatio == curSel.startRatio && it.edge == curSel.edge } else -1
        pushUndo()
        workConfig = workConfig.withZones(edge, newZones)
        currentPresetId = null
        selectedZone = if (selIdx >= 0) newZones[selIdx] else null
    }

    /**
     * 비율 프리셋을 zones 배열에 적용해 새 zones 목록을 반환하는 순수 함수.
     * workConfig/Undo를 건드리지 않으므로 미리보기에 사용 가능.
     * ratios 개수가 zones 개수와 다르면 null 반환.
     */
    fun computeRatioZones(zones: List<EdgeZone>, ratios: List<Float>): List<EdgeZone>? {
        if (zones.size != ratios.size) return null
        var cum = 0f
        return zones.mapIndexed { i, zone ->
            val s = cum
            cum += ratios[i]
            val e = if (i == zones.size - 1) 1f else cum
            zone.copy(startRatio = s, endRatio = e)
        }
    }

    /**
     * [zone]을 n등분한 edge 전체 존 목록을 반환하는 순수 함수 (첫 조각만 기존 trigger 유지).
     * workConfig/Undo를 건드리지 않으므로 분할 미리보기에 사용 가능. 대상 존을 못 찾으면 null.
     * [splitInto]와 동일한 분할 규칙.
     */
    fun computeSplitZones(zone: EdgeZone, n: Int): List<EdgeZone>? {
        val zones = workConfig.zonesFor(zone.edge).toMutableList()
        val idx = zones.indexOfFirst { it.startRatio == zone.startRatio && it.edge == zone.edge }
        if (idx < 0) return null
        val w = (zone.endRatio - zone.startRatio) / n
        val parts = (0 until n).map { i ->
            val s = zone.startRatio + i * w
            val e = if (i == n - 1) zone.endRatio else s + w
            if (i == 0) zone.copy(endRatio = e)
            else EdgeZone(zone.edge, s, e, EdgeZoneTrigger.SingleAction(EdgeZoneAction.Unassigned, "", ""))
        }
        zones.removeAt(idx)
        zones.addAll(idx, parts)
        return zones
    }

    // ── 캔버스 병합 모드 탭 판정 ──

    /** 병합 모드 탭 결과. [mergeTapDecision]의 반환 타입. */
    sealed class MergeTap {
        /** 구간 바깥 인접 → [startRatio]를 selected에 추가. */
        data class Add(val startRatio: Float)    : MergeTap()
        /** 구간 끝점(기준 제외) → [startRatio]를 selected에서 제거. */
        data class Remove(val startRatio: Float) : MergeTap()
        /** 비인접·구간 내부 → 거부 (InteractRejection.MergeNonAdjacent 토스트). */
        object Reject : MergeTap()
        /** tapStartRatio가 zones 리스트에 없음 → no-op. */
        object Ignore : MergeTap()
    }

    /**
     * 병합 모드에서 존 탭 시 인접성 판정 (순수 인덱스 계산).
     *
     * 현재 선택 구간 [lo, hi]에 대해 탭한 존이 추가/제거/거부/무시 중 어느 경우인지 반환한다.
     * 첫 선택·기준 재탭·엣지 교차 같은 상위 분기는 호출부가 처리한다.
     *
     * @param edge          대상 엣지
     * @param base          기준 존 startRatio
     * @param selected      현재 선택된 startRatio 집합 (base 미포함)
     * @param tapStartRatio 탭한 존의 startRatio
     */
    fun mergeTapDecision(
        edge: EntryEdge,
        base: Float,
        selected: Set<Float>,
        tapStartRatio: Float,
    ): MergeTap {
        val zones = workConfig.zonesFor(edge)
        val tapIdx = zones.indexOfFirst { it.startRatio == tapStartRatio }
        val selIndices = (selected + base)
            .mapNotNull { sr -> zones.indexOfFirst { it.startRatio == sr }.takeIf { it >= 0 } }
            .sorted()
        val lo = selIndices.firstOrNull() ?: -1
        val hi = selIndices.lastOrNull() ?: -1
        return when {
            tapIdx < 0                            -> MergeTap.Ignore
            tapIdx == lo - 1 || tapIdx == hi + 1 -> MergeTap.Add(tapStartRatio)
            tapIdx == lo || tapIdx == hi          -> MergeTap.Remove(tapStartRatio)
            else                                  -> MergeTap.Reject
        }
    }
}
