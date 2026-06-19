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
}
