package com.bridgeone.app.ui.components.touchpad

import androidx.compose.ui.util.lerp

/**
 * 캔버스 존 병합/분할 stretch·shrink 보간용 2프레임 홀더 (Phase 4.7.x).
 *
 * startFrame(p=0)과 endFrame(p=1)은 동일 길이·동일 순서의 존 리스트.
 * 인접 존의 end==start 공유 경계를 양 프레임에서 일치시켜, 보간 중에도 빈틈/겹침 없이 타일링된다.
 */
data class ZoneMorph(
    val edge: EntryEdge,
    val startFrame: List<EdgeZone>,   // p=0 상태 (edge 전체 존)
    val endFrame: List<EdgeZone>,     // p=1 상태 (같은 길이/순서)
) {
    /** 진행도 p(0~1)에서 edge 존 리스트를 반환. 비율만 lerp, trigger·edge는 startFrame 유지. */
    fun frame(p: Float): List<EdgeZone> =
        startFrame.mapIndexed { i, s ->
            val e = endFrame[i]
            s.copy(
                startRatio = lerp(s.startRatio, e.startRatio, p),
                endRatio   = lerp(s.endRatio,   e.endRatio,   p),
            )
        }
}

/**
 * 병합 morph 생성.
 *
 * startFrame = beforeZones 그대로.
 * endFrame = 병합 범위 안 존들을 수축/확장:
 *   - base 존: [S, E] (범위 최좌~최우로 확장)
 *   - 범위 내 다른 존: base 왼쪽은 [S, S], 오른쪽은 [E, E] (zero-width로 수축)
 *   - 범위 밖 존: 변화 없음
 *
 * @param beforeZones 병합 직전 edge 존 리스트
 * @param edge 대상 엣지
 * @param baseStartRatio trigger를 유지할 기준 존의 startRatio
 * @param mergeStartRatios 병합할 모든 존의 startRatio 집합 (base 포함, 2개 이상)
 * @return 유효한 범위면 ZoneMorph, 그 외 null
 */
fun buildMergeMorph(
    beforeZones: List<EdgeZone>,
    edge: EntryEdge,
    baseStartRatio: Float,
    mergeStartRatios: Set<Float>,
): ZoneMorph? {
    if (mergeStartRatios.size < 2) return null

    // 각 startRatio → 인덱스 매핑, 정렬
    val indices = mergeStartRatios
        .mapNotNull { sr -> beforeZones.indexOfFirst { it.startRatio == sr }.takeIf { it >= 0 } }
        .sorted()
    if (indices.size != mergeStartRatios.size) return null

    // 연속 인접 확인
    if (indices.last() - indices.first() != indices.size - 1) return null

    val first = indices.first()
    val last  = indices.last()
    val S = beforeZones[first].startRatio
    val E = beforeZones[last].endRatio
    val baseIdx = beforeZones.indexOfFirst { it.startRatio == baseStartRatio }
    if (baseIdx < 0) return null

    val endFrame = beforeZones.mapIndexed { i, zone ->
        when {
            i == baseIdx -> zone.copy(startRatio = S, endRatio = E)
            i in first..last && i < baseIdx -> zone.copy(startRatio = S, endRatio = S)
            i in first..last && i > baseIdx -> zone.copy(startRatio = E, endRatio = E)
            else -> zone
        }
    }

    return ZoneMorph(edge, startFrame = beforeZones, endFrame = endFrame)
}

/**
 * 분할 morph 생성.
 *
 * startFrame = afterZones를 collapse 초기 상태로 매핑:
 *   - part0(기존 trigger 유지): 원본 전체 폭 [originalStart, originalEnd] 유지
 *   - part1..partN-1: zero-width [originalEnd, originalEnd] (우측 끝에 적층)
 *   - 범위 밖 존: 변화 없음
 * endFrame = afterZones 그대로.
 *
 * @param afterZones 분할 직후 edge 존 리스트
 * @param edge 대상 엣지
 * @param originalStartRatio 분할 전 존의 startRatio
 * @param originalEndRatio 분할 전 존의 endRatio
 * @param n 분할 개수
 * @return 유효하면 ZoneMorph, part0을 찾지 못하면 null
 */
fun buildSplitMorph(
    afterZones: List<EdgeZone>,
    edge: EntryEdge,
    originalStartRatio: Float,
    originalEndRatio: Float,
    n: Int,
): ZoneMorph? {
    val idx = afterZones.indexOfFirst { it.startRatio == originalStartRatio }
    if (idx < 0 || idx + n > afterZones.size) return null

    val startFrame = afterZones.mapIndexed { i, zone ->
        when {
            i == idx     -> zone.copy(startRatio = originalStartRatio, endRatio = originalEndRatio)
            i in (idx + 1) until (idx + n) ->
                zone.copy(startRatio = originalEndRatio, endRatio = originalEndRatio)
            else -> zone
        }
    }

    return ZoneMorph(edge, startFrame = startFrame, endFrame = afterZones)
}

/**
 * 분할 미리보기 갯수 변경(예: 2→3, 4→2) 모핑 생성.
 *
 * 대상 존 `[origStartRatio, origEndRatio]` 구간을 [fromN]등분 상태에서 [toN]등분 상태로 보간한다.
 * 양 프레임을 `max(fromN, toN)`개의 존으로 맞추고, 부족한 쪽은 구간 오른쪽 끝의 zero-width 존으로 채워
 * 갯수가 늘면 오른쪽에서 새 조각이 자라나고, 줄면 여분 조각이 오른쪽 끝으로 수축한다.
 * fromN=1이면 통째 존에서 분할되는 효과([buildSplitMorph]와 동일).
 *
 * @param baseZones 분할 전 edge 존 리스트(대상 존이 단일 존으로 존재)
 * @return 유효하면 ZoneMorph, 대상 존을 못 찾으면 null
 */
fun buildSplitCountMorph(
    baseZones: List<EdgeZone>,
    edge: EntryEdge,
    origStartRatio: Float,
    origEndRatio: Float,
    fromN: Int,
    toN: Int,
): ZoneMorph? {
    val idx = baseZones.indexOfFirst { it.startRatio == origStartRatio }
    if (idx < 0) return null
    val origZone = baseZones[idx]
    val k = maxOf(fromN, toN)

    // count개 균등 분할을 k개 슬롯에 채운다(나머지는 구간 끝의 zero-width).
    fun parts(count: Int): List<EdgeZone> {
        val w = (origEndRatio - origStartRatio) / count
        return (0 until k).map { i ->
            if (i < count) {
                val s = origStartRatio + i * w
                val e = if (i == count - 1) origEndRatio else s + w
                if (i == 0) origZone.copy(startRatio = s, endRatio = e)
                else EdgeZone(edge, s, e, EdgeZoneTrigger.SingleAction(EdgeZoneAction.Unassigned, "", ""))
            } else {
                EdgeZone(edge, origEndRatio, origEndRatio, EdgeZoneTrigger.SingleAction(EdgeZoneAction.Unassigned, "", ""))
            }
        }
    }

    val startFrame = baseZones.toMutableList().apply { removeAt(idx); addAll(idx, parts(fromN)) }
    val endFrame = baseZones.toMutableList().apply { removeAt(idx); addAll(idx, parts(toN)) }
    return ZoneMorph(edge, startFrame = startFrame, endFrame = endFrame)
}

/**
 * 이동 모드 이웃 reflow morph 생성 (same-edge·cross-edge 공용).
 *
 * 선택 존(P)은 떠다니는 오버레이([ZoneMoveFloatingOverlay])가 출발→도착으로 직접 이동시키므로,
 * 이 morph는 **P를 제외한 나머지 존(이웃)만** 보간한다.
 *   - 출발 엣지: P가 빠진 자리를 이웃들이 모여 메운다(빈틈 닫힘).
 *   - 도착 엣지: 이웃들이 벌어져 P가 들어갈 공간을 연다(공간 열림).
 *   - 같은 엣지: P를 양 config에서 빼면 실제로 자리가 바뀐 이웃만 비율이 달라져 보이게 슬라이드.
 *
 * P를 양 프레임에서 trigger 참조(===)로 걸러내면 이웃 순서·개수가 보존돼 인덱스별 lerp가 정합한다.
 *
 * @param before        이동 전 전체 config
 * @param after         이동 후 전체 config
 * @param pickedTrigger 선택 존 식별용 trigger 참조 (config 간 === 로 보존됨)
 * @param sourceEdge    출발 엣지
 * @param targetEdge    도착 엣지 (sourceEdge와 같으면 단일 엣지만 처리)
 * @return 영향 엣지별 ZoneMorph 리스트 (이웃 개수가 양쪽에서 일치하는 엣지만 포함)
 */
fun buildMoveReflowMorphs(
    before: EdgeZoneConfig,
    after: EdgeZoneConfig,
    pickedTrigger: EdgeZoneTrigger,
    sourceEdge: EntryEdge,
    targetEdge: EntryEdge,
): List<ZoneMorph> {
    val edges = if (sourceEdge == targetEdge) listOf(sourceEdge) else listOf(sourceEdge, targetEdge)
    return edges.mapNotNull { edge ->
        val beforeNeighbors = before.zonesFor(edge).filterNot { it.trigger === pickedTrigger }
        val afterNeighbors  = after.zonesFor(edge).filterNot { it.trigger === pickedTrigger }
        if (beforeNeighbors.size != afterNeighbors.size) null
        else ZoneMorph(edge, startFrame = beforeNeighbors, endFrame = afterNeighbors)
    }
}

/**
 * picked(trigger 식별)를 모든 엣지에서 1개씩 제거한 config (rescale 안 함, 빠진 자리는 gap 유지).
 *
 * SWIPE 이동 미리보기에서 picked는 떠다니는 오버레이가 표시하므로 캔버스 config에서 빼낸다.
 * gap을 유지해(재정규화 없이) 직전↔현재 이웃 config를 [lerpConfig]로 부드럽게 보간할 수 있다.
 */
fun stripPicked(config: EdgeZoneConfig, pickedTrigger: EdgeZoneTrigger): EdgeZoneConfig {
    var cfg = config
    EntryEdge.entries.forEach { edge ->
        val zones = config.zonesFor(edge)
        val filtered = zones.filterNot { it.trigger === pickedTrigger }
        if (filtered.size != zones.size) cfg = cfg.withZones(edge, filtered)
    }
    return cfg
}

/** picked(trigger 식별)의 현재 위치를 [ZoneStrip]으로 반환. 어느 엣지에도 없으면 null. */
fun landingStrip(config: EdgeZoneConfig, pickedTrigger: EdgeZoneTrigger): ZoneStrip? =
    EntryEdge.entries.firstNotNullOfOrNull { edge ->
        config.zonesFor(edge).firstOrNull { it.trigger === pickedTrigger }
            ?.let { ZoneStrip(edge, it.startRatio, it.endRatio) }
    }

/**
 * 비율 조정 되돌리기용: from→to 전체 config 보간 홀더 (Phase 4.7.x).
 *
 * 병합/분할의 단일 엣지 [ZoneMorph]와 달리, 안내 카드 '취소'처럼 여러 엣지에 걸친 되돌리기를
 * 한 번에 보간하기 위해 전체 [EdgeZoneConfig]를 대상으로 한다. 되돌리기는 비율만 바뀌고
 * 존 개수·순서는 보존되는 전제(adjustBoundary/applyRatioPreset만 사용)에서만 사용한다.
 */
data class ConfigMorph(val from: EdgeZoneConfig, val to: EdgeZoneConfig)

/** p(0~1): p=0 → from, p=1 → to. 엣지별 존 개수가 다르면 그 엣지는 to 그대로(보간 생략). */
fun lerpConfig(from: EdgeZoneConfig, to: EdgeZoneConfig, p: Float): EdgeZoneConfig {
    var cfg = to
    EntryEdge.entries.forEach { edge ->
        val f = from.zonesFor(edge)
        val t = to.zonesFor(edge)
        if (f.size != t.size) return@forEach
        cfg = cfg.withZones(edge, t.mapIndexed { i, tz ->
            tz.copy(
                startRatio = lerp(f[i].startRatio, tz.startRatio, p),
                endRatio   = lerp(f[i].endRatio,   tz.endRatio,   p),
            )
        })
    }
    return cfg
}
