package com.bridgeone.app.ui.components.touchpad

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 4.7.5-A: EdgeZoneEditorScreen에서 추출한 상태 홀더 테스트.
 * config 변환·Undo가 클로저 밖으로 나와 단위 테스트 가능해졌다.
 */
class EdgeZoneEditorStateTest {

    private fun zone(
        edge: EntryEdge,
        start: Float,
        end: Float,
        action: EdgeZoneAction = EdgeZoneAction.Unassigned,
        label: String = "",
    ): EdgeZone = EdgeZone(edge, start, end, EdgeZoneTrigger.SingleAction(action, label, ""))

    /** TOP 엣지에 n개 균등 존, 나머지 엣지는 단일 존인 config. */
    private fun topConfig(zones: List<EdgeZone>): EdgeZoneConfig = EdgeZoneConfig(
        topZones = zones,
        bottomZones = listOf(zone(EntryEdge.BOTTOM, 0f, 1f)),
        leftZones = listOf(zone(EntryEdge.LEFT, 0f, 1f)),
        rightZones = listOf(zone(EntryEdge.RIGHT, 0f, 1f)),
    )

    private fun newState(config: EdgeZoneConfig, presetId: String? = "preset-1") =
        EdgeZoneEditorState(config, presetId)

    // ============================================================
    // splitInto
    // ============================================================

    @Test
    fun splitInto_dividesZoneEvenly_firstKeepsTrigger() {
        val original = zone(EntryEdge.TOP, 0f, 1f, EdgeZoneAction.SendMacro(), label = "원본")
        val state = newState(topConfig(listOf(original)))

        state.splitInto(original, 2)

        val top = state.workConfig.topZones
        assertEquals(2, top.size)
        assertEquals(0f, top[0].startRatio, 1e-5f)
        assertEquals(0.5f, top[0].endRatio, 1e-5f)
        assertEquals(0.5f, top[1].startRatio, 1e-5f)
        assertEquals(1f, top[1].endRatio, 1e-5f)
        // 첫 조각은 기존 trigger 유지, 나머지는 Unassigned
        assertEquals(EdgeZoneAction.SendMacro(), (top[0].trigger as EdgeZoneTrigger.SingleAction).action)
        assertEquals(EdgeZoneAction.Unassigned, (top[1].trigger as EdgeZoneTrigger.SingleAction).action)
        // 부수 효과: 선택은 첫 조각, presetId 무효화, undo push
        assertEquals(top[0], state.selectedZone)
        assertNull(state.currentPresetId)
        assertEquals(1, state.undoStack.size)
    }

    @Test
    fun splitInto_unknownZone_noOp() {
        val state = newState(topConfig(listOf(zone(EntryEdge.TOP, 0f, 1f))))
        val foreign = zone(EntryEdge.TOP, 0.3f, 0.7f)
        state.splitInto(foreign, 2)
        assertEquals(1, state.workConfig.topZones.size)
        assertTrue(state.undoStack.isEmpty())
    }

    // ============================================================
    // tryMergeWith
    // ============================================================

    @Test
    fun tryMergeWith_adjacentZones_merges() {
        val z0 = zone(EntryEdge.TOP, 0f, 0.5f)
        val z1 = zone(EntryEdge.TOP, 0.5f, 1f)
        val state = newState(topConfig(listOf(z0, z1)))

        state.tryMergeWith(z1, z0)

        val top = state.workConfig.topZones
        assertEquals(1, top.size)
        assertEquals(0f, top[0].startRatio, 1e-5f)
        assertEquals(1f, top[0].endRatio, 1e-5f)
        assertEquals(top[0], state.selectedZone)
        assertNull(state.currentPresetId)
    }

    @Test
    fun tryMergeWith_nonAdjacent_noOp() {
        val z0 = zone(EntryEdge.TOP, 0f, 0.33f)
        val z1 = zone(EntryEdge.TOP, 0.33f, 0.66f)
        val z2 = zone(EntryEdge.TOP, 0.66f, 1f)
        val state = newState(topConfig(listOf(z0, z1, z2)))

        state.tryMergeWith(z2, z0)  // index 2와 0 → 인접 아님

        assertEquals(3, state.workConfig.topZones.size)
        assertTrue(state.undoStack.isEmpty())
    }

    @Test
    fun tryMergeWith_differentEdge_noOp() {
        val top = zone(EntryEdge.TOP, 0f, 1f)
        val bottom = zone(EntryEdge.BOTTOM, 0f, 1f)
        val state = newState(topConfig(listOf(top)))
        state.tryMergeWith(top, bottom)
        assertTrue(state.undoStack.isEmpty())
    }

    // ============================================================
    // deleteZone
    // ============================================================

    @Test
    fun deleteZone_absorbsRatioIntoNeighbor() {
        val z0 = zone(EntryEdge.TOP, 0f, 0.5f)
        val z1 = zone(EntryEdge.TOP, 0.5f, 1f)
        val state = newState(topConfig(listOf(z0, z1)))
        state.selectedZone = z0

        state.deleteZone(z0)

        val top = state.workConfig.topZones
        assertEquals(1, top.size)
        // 삭제된 z0의 startRatio(0f)를 인접 존이 흡수
        assertEquals(0f, top[0].startRatio, 1e-5f)
        assertEquals(1f, top[0].endRatio, 1e-5f)
        // 선택 존이 삭제 대상이면 null
        assertNull(state.selectedZone)
    }

    @Test
    fun deleteZone_lastZone_noOp() {
        val only = zone(EntryEdge.TOP, 0f, 1f)
        val state = newState(topConfig(listOf(only)))
        state.deleteZone(only)
        assertEquals(1, state.workConfig.topZones.size)
        assertTrue(state.undoStack.isEmpty())
    }

    // ============================================================
    // applyRatioPreset
    // ============================================================

    @Test
    fun applyRatioPreset_appliesCumulativeRatios() {
        val z0 = zone(EntryEdge.TOP, 0f, 0.5f)
        val z1 = zone(EntryEdge.TOP, 0.5f, 1f)
        val state = newState(topConfig(listOf(z0, z1)))

        state.applyRatioPreset(EntryEdge.TOP, listOf(0.7f, 0.3f))

        val top = state.workConfig.topZones
        assertEquals(0f, top[0].startRatio, 1e-5f)
        assertEquals(0.7f, top[0].endRatio, 1e-5f)
        assertEquals(0.7f, top[1].startRatio, 1e-5f)
        // 마지막 존의 end는 항상 1f
        assertEquals(1f, top[1].endRatio, 1e-5f)
        assertNull(state.currentPresetId)
    }

    @Test
    fun applyRatioPreset_sizeMismatch_noOp() {
        val z0 = zone(EntryEdge.TOP, 0f, 0.5f)
        val z1 = zone(EntryEdge.TOP, 0.5f, 1f)
        val state = newState(topConfig(listOf(z0, z1)))
        state.applyRatioPreset(EntryEdge.TOP, listOf(0.5f, 0.3f, 0.2f))
        assertTrue(state.undoStack.isEmpty())
        assertEquals(0.5f, state.workConfig.topZones[0].endRatio, 1e-5f)
    }

    // ============================================================
    // pushUndo
    // ============================================================

    @Test
    fun pushUndo_latestAtFront_capsAt20() {
        val cfgA = topConfig(listOf(zone(EntryEdge.TOP, 0f, 1f, label = "A")))
        val cfgB = topConfig(listOf(zone(EntryEdge.TOP, 0f, 1f, label = "B")))
        val state = newState(cfgA)

        state.pushUndo()              // [A]
        state.workConfig = cfgB
        state.pushUndo()              // [B, A]

        assertEquals(cfgB, state.undoStack[0])  // 최신이 맨 앞
        assertEquals(cfgA, state.undoStack[1])

        // 20개 상한
        repeat(30) { state.pushUndo() }
        assertEquals(20, state.undoStack.size)
    }

    // ============================================================
    // 캔버스 이동 (computeMove / validateMove / dropInsertIndex / commitMove)
    // ============================================================

    /** 모든 엣지에 동일 존 리스트를 둘 수 없으니, TOP 3분할 + 다른 엣지 단일 구성 사용. */
    private fun topTriConfig(): EdgeZoneConfig = topConfig(
        listOf(
            zone(EntryEdge.TOP, 0f, 0.33f, label = "A"),
            zone(EntryEdge.TOP, 0.33f, 0.66f, label = "B"),
            zone(EntryEdge.TOP, 0.66f, 1f, label = "C"),
        )
    )

    @Test
    fun dropInsertIndex_picksNearestSlot_includingEnds() {
        val state = newState(topTriConfig())
        // 슬롯 위치: 0f, 0.33f, 0.66f, 1f (인덱스 0..3)
        assertEquals(0, state.dropInsertIndex(EntryEdge.TOP, 0.02f, null))
        assertEquals(1, state.dropInsertIndex(EntryEdge.TOP, 0.30f, null))
        assertEquals(3, state.dropInsertIndex(EntryEdge.TOP, 0.98f, null))
    }

    @Test
    fun computeMove_sameEdge_reordersPreservingWidth() {
        val state = newState(topTriConfig())
        val a = state.workConfig.topZones[0]  // A: 0~0.33
        // insertIndex는 picked(A) 제외 리스트[B,C] 기준 → 맨 끝 = 2. 순서 B, C, A. 폭(0.33) 보존.
        val result = state.computeMove(ZoneKey(EntryEdge.TOP, a.startRatio), EntryEdge.TOP, 2)!!
        val top = result.topZones
        assertEquals(3, top.size)
        assertEquals(listOf("B", "C", "A"), top.map { it.label })
        // 각 폭 보존(약 0.33)
        top.forEach { assertEquals(0.33f, it.endRatio - it.startRatio, 1e-2f) }
        assertEquals(1f, top.last().endRatio, 1e-5f)
    }

    @Test
    fun computeMove_sameEdge_swapTwoZones() {
        // 2분할 엣지에서 첫 존을 끝(insertIndex=1, picked 제외 리스트 기준)으로 → 자리 교환
        val state = newState(topConfig(listOf(
            zone(EntryEdge.TOP, 0f, 0.5f, label = "이동"),
            zone(EntryEdge.TOP, 0.5f, 1f, label = "DPI"),
        )))
        val first = state.workConfig.topZones[0]  // 이동
        val result = state.computeMove(ZoneKey(EntryEdge.TOP, first.startRatio), EntryEdge.TOP, 1)!!
        assertEquals(listOf("DPI", "이동"), result.topZones.map { it.label })
    }

    @Test
    fun computeMove_crossEdge_removesFromSourceInsertsToTarget_preservesTrigger() {
        val state = newState(topTriConfig())
        val a = state.workConfig.topZones[0]  // A
        // A(TOP)를 LEFT 엣지 맨 앞(insertIndex=0)으로
        val result = state.computeMove(ZoneKey(EntryEdge.TOP, a.startRatio), EntryEdge.LEFT, 0)!!
        // 출발 엣지: A 제거 → B, C 두 개로 [0,1] 재분배
        assertEquals(2, result.topZones.size)
        assertEquals(listOf("B", "C"), result.topZones.map { it.label })
        assertEquals(0f, result.topZones.first().startRatio, 1e-5f)
        assertEquals(1f, result.topZones.last().endRatio, 1e-5f)
        // 도착 엣지: 기존 단일 존 + A = 2개, A가 맨 앞, trigger(label) 보존
        assertEquals(2, result.leftZones.size)
        assertEquals("A", result.leftZones[0].label)
        assertEquals(EntryEdge.LEFT, result.leftZones[0].edge)
        assertEquals(0f, result.leftZones.first().startRatio, 1e-5f)
        assertEquals(1f, result.leftZones.last().endRatio, 1e-5f)
    }

    @Test
    fun validateMove_sourceSingleZone_rejected() {
        // BOTTOM은 단일 존 → 옮길 수 없음
        val state = newState(topTriConfig())
        val rej = state.validateMove(ZoneKey(EntryEdge.BOTTOM, 0f), EntryEdge.LEFT, 0, emptySet())
        assertTrue(rej is EdgeZoneEditorState.MoveRejection.SourceLastZone)
    }

    @Test
    fun validateMove_disabledTargetEdge_rejected() {
        val state = newState(topTriConfig())
        val a = state.workConfig.topZones[0]
        val rej = state.validateMove(
            ZoneKey(EntryEdge.TOP, a.startRatio), EntryEdge.LEFT, 0, setOf(EntryEdge.LEFT)
        )
        assertTrue(rej is EdgeZoneEditorState.MoveRejection.DisabledEdge)
    }

    @Test
    fun validateMove_targetFull_rejected() {
        // LEFT 엣지를 MAX(5)개로 채우고, TOP의 존을 LEFT로 옮기려 하면 거부
        val full = (0 until 5).map { i ->
            zone(EntryEdge.LEFT, i * 0.2f, if (i == 4) 1f else (i + 1) * 0.2f)
        }
        val cfg = EdgeZoneConfig(
            topZones = listOf(zone(EntryEdge.TOP, 0f, 0.5f, label = "A"), zone(EntryEdge.TOP, 0.5f, 1f, label = "B")),
            bottomZones = listOf(zone(EntryEdge.BOTTOM, 0f, 1f)),
            leftZones = full,
            rightZones = listOf(zone(EntryEdge.RIGHT, 0f, 1f)),
        )
        val state = newState(cfg)
        val rej = state.validateMove(ZoneKey(EntryEdge.TOP, 0f), EntryEdge.LEFT, 0, emptySet())
        assertTrue(rej is EdgeZoneEditorState.MoveRejection.TargetFull)
    }

    @Test
    fun commitMove_crossEdge_pushesUndo_clearsSelection_returnsMorphs() {
        val state = newState(topTriConfig())
        val a = state.workConfig.topZones[0]
        val morphs = state.commitMove(ZoneKey(EntryEdge.TOP, a.startRatio), EntryEdge.LEFT, 0)
        assertEquals(2, state.workConfig.topZones.size)
        assertEquals(2, state.workConfig.leftZones.size)
        assertEquals(1, state.undoStack.size)
        assertNull(state.selectedZone)
        assertNull(state.currentPresetId)
        // cross-edge는 출발/도착 두 엣지 모핑
        assertEquals(2, morphs.size)
    }

    @Test
    fun commitMove_noOp_doesNotPushUndo() {
        val state = newState(topTriConfig())
        val a = state.workConfig.topZones[0]  // index 0
        // insertIndex=0 → A가 제자리. 변화 없음.
        val morphs = state.commitMove(ZoneKey(EntryEdge.TOP, a.startRatio), EntryEdge.TOP, 0)
        assertTrue(morphs.isEmpty())
        assertTrue(state.undoStack.isEmpty())
    }
}
