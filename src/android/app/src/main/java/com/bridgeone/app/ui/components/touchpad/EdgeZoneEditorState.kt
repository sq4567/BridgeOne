package com.bridgeone.app.ui.components.touchpad

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

    /**
     * 존을 인접 슬롯으로 한 칸 이동.
     * @param carryWidth true=폭째 이동(통째 자리 교환 후 비율 재계산), false=액션만 교환(경계 고정, trigger만 swap)
     * @return 이동 후 "원래 내용이 위치한 존"(없으면 null). 호출부가 selectedZone/popup 갱신에 사용
     */
    fun moveZone(zone: EdgeZone, toLeft: Boolean, carryWidth: Boolean): EdgeZone? {
        val zones = workConfig.zonesFor(zone.edge).toMutableList()
        val i = zones.indexOfFirst { it.startRatio == zone.startRatio && it.edge == zone.edge }
        if (i < 0) return null
        val j = if (toLeft) i - 1 else i + 1
        if (j < 0 || j >= zones.size) return null

        val resultZone: EdgeZone
        if (carryWidth) {
            // 리스트 순서 교환 후 비율을 순서대로 재계산(각 존 폭 보존)
            val tmp = zones[i]; zones[i] = zones[j]; zones[j] = tmp
            val recomputed = recomputeRatiosPreservingWidth(zones)
            resultZone = recomputed[j]
            pushUndo()
            workConfig = workConfig.withZones(zone.edge, recomputed)
        } else {
            // 경계 고정, trigger만 swap
            val zi = zones[i]; val zj = zones[j]
            zones[i] = zi.copy(trigger = zj.trigger)
            zones[j] = zj.copy(trigger = zi.trigger)
            resultZone = zones[j]   // 원래 내용이 이동해 간 칸
            pushUndo()
            workConfig = workConfig.withZones(zone.edge, zones)
        }
        currentPresetId = null
        selectedZone = resultZone
        return resultZone
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
}
