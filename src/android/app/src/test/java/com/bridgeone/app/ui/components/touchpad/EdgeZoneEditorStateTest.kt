package com.bridgeone.app.ui.components.touchpad

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    // ============================================================
    // deleteZones (Phase 4.7.8-D)
    // ============================================================

    @Test
    fun deleteZones_oneOfTwo_neighborAbsorbsRatio() {
        val z0 = zone(EntryEdge.TOP, 0f, 0.5f)
        val z1 = zone(EntryEdge.TOP, 0.5f, 1f)
        val state = newState(topConfig(listOf(z0, z1)))

        state.deleteZones(setOf(ZoneKey(EntryEdge.TOP, z0.startRatio)))

        val top = state.workConfig.topZones
        assertEquals(1, top.size)
        // z0.startRatio(0f)를 남은 z1이 흡수
        assertEquals(0f, top[0].startRatio, 1e-5f)
        assertEquals(1f, top[0].endRatio, 1e-5f)
        assertNull(state.currentPresetId)
        assertEquals(1, state.undoStack.size)
    }

    @Test
    fun deleteZones_bothOfTwo_preservesLast() {
        // 2개 중 2개 삭제 요청 → 최소 1개 보존 규칙으로 하나만 삭제됨
        val z0 = zone(EntryEdge.TOP, 0f, 0.5f)
        val z1 = zone(EntryEdge.TOP, 0.5f, 1f)
        val state = newState(topConfig(listOf(z0, z1)))

        state.deleteZones(setOf(
            ZoneKey(EntryEdge.TOP, z0.startRatio),
            ZoneKey(EntryEdge.TOP, z1.startRatio),
        ))

        // 내림차순 삭제: z1 먼저 시도(size>1 → 삭제 가능), z0 시도(size==1 → 보존)
        // 결과: z0 1개 남음
        assertEquals(1, state.workConfig.topZones.size)
    }

    @Test
    fun deleteZones_onlyZone_noOp() {
        val only = zone(EntryEdge.TOP, 0f, 1f)
        val state = newState(topConfig(listOf(only)))

        state.deleteZones(setOf(ZoneKey(EntryEdge.TOP, only.startRatio)))

        assertEquals(1, state.workConfig.topZones.size)
        // 최소 1 보존 규칙: undo는 pushUndo()가 먼저 호출되므로 1개 쌓임
        // (실제 삭제가 일어나지 않아도 undo는 push됨)
    }

    @Test
    fun deleteZones_emptySet_noOp() {
        val state = newState(topConfig(listOf(zone(EntryEdge.TOP, 0f, 1f))))
        state.deleteZones(emptySet())
        assertTrue(state.undoStack.isEmpty())
    }

    @Test
    fun deleteZones_threeOfThree_preservesOne() {
        val z0 = zone(EntryEdge.TOP, 0f, 0.33f, label = "A")
        val z1 = zone(EntryEdge.TOP, 0.33f, 0.66f, label = "B")
        val z2 = zone(EntryEdge.TOP, 0.66f, 1f, label = "C")
        val state = newState(topConfig(listOf(z0, z1, z2)))

        state.deleteZones(setOf(
            ZoneKey(EntryEdge.TOP, z0.startRatio),
            ZoneKey(EntryEdge.TOP, z1.startRatio),
            ZoneKey(EntryEdge.TOP, z2.startRatio),
        ))

        // 내림차순: z2→z1→z0 순 삭제 시도. z2 삭제(3→2), z1 삭제(2→1), z0 보존(1==1)
        assertEquals(1, state.workConfig.topZones.size)
        // 남은 존은 z0(startRatio=0, endRatio=1로 흡수됨)
        assertEquals(0f, state.workConfig.topZones[0].startRatio, 1e-5f)
        assertEquals(1f, state.workConfig.topZones[0].endRatio, 1e-5f)
    }

    // ============================================================
    // mergeContiguous (Phase 4.7.8-D)
    // ============================================================

    @Test
    fun mergeContiguous_twoAdjacent_mergesWithBaseTrigger() {
        val z0 = zone(EntryEdge.TOP, 0f, 0.5f, EdgeZoneAction.SendMacro(), label = "base")
        val z1 = zone(EntryEdge.TOP, 0.5f, 1f)
        val state = newState(topConfig(listOf(z0, z1)))

        val result = state.mergeContiguous(EntryEdge.TOP, z0.startRatio, setOf(z0.startRatio, z1.startRatio))

        assertTrue(result)
        val top = state.workConfig.topZones
        assertEquals(1, top.size)
        assertEquals(0f, top[0].startRatio, 1e-5f)
        assertEquals(1f, top[0].endRatio, 1e-5f)
        // base trigger(z0) 유지
        assertEquals(EdgeZoneAction.SendMacro(), (top[0].trigger as EdgeZoneTrigger.SingleAction).action)
        assertEquals("base", top[0].label)
        assertEquals(top[0], state.selectedZone)
        assertNull(state.currentPresetId)
    }

    @Test
    fun mergeContiguous_lessThanTwo_returnsFalse() {
        val z0 = zone(EntryEdge.TOP, 0f, 0.5f)
        val state = newState(topConfig(listOf(z0, zone(EntryEdge.TOP, 0.5f, 1f))))
        val result = state.mergeContiguous(EntryEdge.TOP, z0.startRatio, setOf(z0.startRatio))
        assertFalse(result)
        assertTrue(state.undoStack.isEmpty())
    }

    @Test
    fun mergeContiguous_nonContiguous_returnsFalse() {
        val z0 = zone(EntryEdge.TOP, 0f, 0.33f)
        val z1 = zone(EntryEdge.TOP, 0.33f, 0.66f)
        val z2 = zone(EntryEdge.TOP, 0.66f, 1f)
        val state = newState(topConfig(listOf(z0, z1, z2)))
        // z0(idx=0)와 z2(idx=2) → 인덱스 0,2 → last-first=2 ≠ size-1=1 → 거부
        val result = state.mergeContiguous(EntryEdge.TOP, z0.startRatio, setOf(z0.startRatio, z2.startRatio))
        assertFalse(result)
        assertEquals(3, state.workConfig.topZones.size)
    }

    @Test
    fun mergeContiguous_threeContiguous_mergesAll() {
        val z0 = zone(EntryEdge.TOP, 0f, 0.33f, label = "A")
        val z1 = zone(EntryEdge.TOP, 0.33f, 0.66f, label = "B")
        val z2 = zone(EntryEdge.TOP, 0.66f, 1f, label = "C")
        val state = newState(topConfig(listOf(z0, z1, z2)))

        val result = state.mergeContiguous(
            EntryEdge.TOP, z1.startRatio,
            setOf(z0.startRatio, z1.startRatio, z2.startRatio),
        )

        assertTrue(result)
        val top = state.workConfig.topZones
        assertEquals(1, top.size)
        assertEquals(0f, top[0].startRatio, 1e-5f)
        assertEquals(1f, top[0].endRatio, 1e-5f)
        assertEquals("B", top[0].label)  // base=z1 trigger
    }

    // ============================================================
    // adjustBoundary (Phase 4.7.8-D)
    // ============================================================

    @Test
    fun adjustBoundary_validMove_updatesBothZones() {
        val z0 = zone(EntryEdge.TOP, 0f, 0.5f)
        val z1 = zone(EntryEdge.TOP, 0.5f, 1f)
        val state = newState(topConfig(listOf(z0, z1)))

        state.adjustBoundary(EntryEdge.TOP, 0, 0.7f)

        val top = state.workConfig.topZones
        assertEquals(0.7f, top[0].endRatio, 1e-5f)
        assertEquals(0.7f, top[1].startRatio, 1e-5f)
        assertNull(state.currentPresetId)
        // adjustBoundary는 undo를 직접 push하지 않음 (호출부가 드래그 시작 시 1회 push)
        assertTrue(state.undoStack.isEmpty())
    }

    @Test
    fun adjustBoundary_clampedByMinZoneRatio_leftSide() {
        // MIN_ZONE_RATIO = 0.10f
        // z0.startRatio=0f → clamp 하한: 0f + 0.10f = 0.10f
        val z0 = zone(EntryEdge.TOP, 0f, 0.5f)
        val z1 = zone(EntryEdge.TOP, 0.5f, 1f)
        val state = newState(topConfig(listOf(z0, z1)))

        state.adjustBoundary(EntryEdge.TOP, 0, 0.02f)  // 너무 작음

        assertEquals(0.10f, state.workConfig.topZones[0].endRatio, 1e-5f)
        assertEquals(0.10f, state.workConfig.topZones[1].startRatio, 1e-5f)
    }

    @Test
    fun adjustBoundary_clampedByMinZoneRatio_rightSide() {
        // z1.endRatio=1f → clamp 상한: 1f - 0.10f = 0.90f
        val z0 = zone(EntryEdge.TOP, 0f, 0.5f)
        val z1 = zone(EntryEdge.TOP, 0.5f, 1f)
        val state = newState(topConfig(listOf(z0, z1)))

        state.adjustBoundary(EntryEdge.TOP, 0, 0.98f)  // 너무 큼

        assertEquals(0.90f, state.workConfig.topZones[0].endRatio, 1e-5f)
        assertEquals(0.90f, state.workConfig.topZones[1].startRatio, 1e-5f)
    }

    @Test
    fun adjustBoundary_invalidLeftIndex_noOp() {
        val z0 = zone(EntryEdge.TOP, 0f, 0.5f)
        val z1 = zone(EntryEdge.TOP, 0.5f, 1f)
        val state = newState(topConfig(listOf(z0, z1)))

        state.adjustBoundary(EntryEdge.TOP, -1, 0.6f)
        state.adjustBoundary(EntryEdge.TOP, 1, 0.6f)  // 경계는 0..size-2 = 0만 유효

        // 변경 없음
        assertEquals(0.5f, state.workConfig.topZones[0].endRatio, 1e-5f)
    }

    // ============================================================
    // computeSplitZones (Phase 4.7.8-D)
    // ============================================================

    @Test
    fun computeSplitZones_threeEqual_returnsThreeParts() {
        val original = zone(EntryEdge.TOP, 0f, 0.6f, EdgeZoneAction.SendMacro(), label = "원본")
        val other = zone(EntryEdge.TOP, 0.6f, 1f)
        val state = newState(topConfig(listOf(original, other)))

        val result = state.computeSplitZones(original, 3)!!

        // 전체 존 목록 반환(original 자리에 3개 삽입)
        assertEquals(4, result.size)
        // 첫 조각
        assertEquals(0f, result[0].startRatio, 1e-5f)
        assertEquals(0.2f, result[0].endRatio, 1e-5f)
        assertEquals(EdgeZoneAction.SendMacro(), (result[0].trigger as EdgeZoneTrigger.SingleAction).action)
        // 마지막 조각은 original.endRatio에 정확히 맞춤
        assertEquals(0.4f, result[2].startRatio, 1e-3f)
        assertEquals(0.6f, result[2].endRatio, 1e-5f)
        // workConfig는 변경되지 않음 (순수 계산)
        assertEquals(2, state.workConfig.topZones.size)
    }

    @Test
    fun computeSplitZones_unknownZone_returnsNull() {
        val state = newState(topConfig(listOf(zone(EntryEdge.TOP, 0f, 1f))))
        val foreign = zone(EntryEdge.TOP, 0.2f, 0.8f)
        assertNull(state.computeSplitZones(foreign, 2))
    }

    @Test
    fun computeSplitZones_intoOne_returnsUnchangedList() {
        val z = zone(EntryEdge.TOP, 0f, 0.5f, label = "A")
        val z2 = zone(EntryEdge.TOP, 0.5f, 1f, label = "B")
        val state = newState(topConfig(listOf(z, z2)))

        val result = state.computeSplitZones(z, 1)!!

        assertEquals(2, result.size)
        assertEquals("A", result[0].label)
        assertEquals(0f, result[0].startRatio, 1e-5f)
        assertEquals(0.5f, result[0].endRatio, 1e-5f)
    }

    // ============================================================
    // mergeTapDecision (4.7.8-E)
    // ============================================================

    /** TOP 5개 균등 존 config: A(0~0.2) B(0.2~0.4) C(0.4~0.6) D(0.6~0.8) E(0.8~1.0). */
    private fun top5Config(): EdgeZoneConfig = topConfig(listOf(
        zone(EntryEdge.TOP, 0.0f, 0.2f, label = "A"),
        zone(EntryEdge.TOP, 0.2f, 0.4f, label = "B"),
        zone(EntryEdge.TOP, 0.4f, 0.6f, label = "C"),
        zone(EntryEdge.TOP, 0.6f, 0.8f, label = "D"),
        zone(EntryEdge.TOP, 0.8f, 1.0f, label = "E"),
    ))

    @Test
    fun mergeTapDecision_adjacentAboveHi_returnsAdd() {
        // base=A(idx=0), selected={}, lo=hi=0 → tap B(idx=1) == hi+1 → Add
        val state = newState(top5Config())
        val result = state.mergeTapDecision(EntryEdge.TOP, base = 0.0f, selected = emptySet(), tapStartRatio = 0.2f)
        assertTrue(result is EdgeZoneEditorState.MergeTap.Add)
        assertEquals(0.2f, (result as EdgeZoneEditorState.MergeTap.Add).startRatio, 1e-5f)
    }

    @Test
    fun mergeTapDecision_adjacentBelowLo_returnsAdd() {
        // base=C(idx=2), selected={D(0.6)}, selIndices=[2,3] lo=2,hi=3 → tap B(idx=1)==lo-1 → Add
        val state = newState(top5Config())
        val result = state.mergeTapDecision(EntryEdge.TOP, base = 0.4f, selected = setOf(0.6f), tapStartRatio = 0.2f)
        assertTrue(result is EdgeZoneEditorState.MergeTap.Add)
        assertEquals(0.2f, (result as EdgeZoneEditorState.MergeTap.Add).startRatio, 1e-5f)
    }

    @Test
    fun mergeTapDecision_endpointHi_returnsRemove() {
        // base=C(idx=2), selected={D(0.6)}, lo=2,hi=3 → tap D(idx=3)==hi → Remove
        val state = newState(top5Config())
        val result = state.mergeTapDecision(EntryEdge.TOP, base = 0.4f, selected = setOf(0.6f), tapStartRatio = 0.6f)
        assertTrue(result is EdgeZoneEditorState.MergeTap.Remove)
        assertEquals(0.6f, (result as EdgeZoneEditorState.MergeTap.Remove).startRatio, 1e-5f)
    }

    @Test
    fun mergeTapDecision_endpointLo_returnsRemove() {
        // base=C(idx=2), selected={B(0.2)}, selIndices=[1,2] lo=1,hi=2 → tap B(idx=1)==lo → Remove
        val state = newState(top5Config())
        val result = state.mergeTapDecision(EntryEdge.TOP, base = 0.4f, selected = setOf(0.2f), tapStartRatio = 0.2f)
        assertTrue(result is EdgeZoneEditorState.MergeTap.Remove)
    }

    @Test
    fun mergeTapDecision_nonAdjacentFar_returnsReject() {
        // base=A(idx=0), selected={}, lo=hi=0 → tap C(idx=2), neither lo-1 nor hi+1 → Reject
        val state = newState(top5Config())
        val result = state.mergeTapDecision(EntryEdge.TOP, base = 0.0f, selected = emptySet(), tapStartRatio = 0.4f)
        assertEquals(EdgeZoneEditorState.MergeTap.Reject, result)
    }

    @Test
    fun mergeTapDecision_interior_returnsReject() {
        // base=A(idx=0), selected={B(0.2),C(0.4)}, selIndices=[0,1,2] lo=0,hi=2
        // tap B(idx=1): not lo-1, not hi+1, not lo, not hi → Reject
        val state = newState(top5Config())
        val result = state.mergeTapDecision(EntryEdge.TOP, base = 0.0f, selected = setOf(0.2f, 0.4f), tapStartRatio = 0.2f)
        assertEquals(EdgeZoneEditorState.MergeTap.Reject, result)
    }

    @Test
    fun mergeTapDecision_unknownRatio_returnsIgnore() {
        val state = newState(top5Config())
        val result = state.mergeTapDecision(EntryEdge.TOP, base = 0.0f, selected = emptySet(), tapStartRatio = 0.999f)
        assertEquals(EdgeZoneEditorState.MergeTap.Ignore, result)
    }

    // ============================================================
    // resolveDisplayConfig (4.7.8-E)
    // ============================================================

    @Test
    fun resolveDisplayConfig_ratioMorphPresent_returnsLerp() {
        val from = topConfig(listOf(zone(EntryEdge.TOP, 0f, 0.4f), zone(EntryEdge.TOP, 0.4f, 1f)))
        val to   = topConfig(listOf(zone(EntryEdge.TOP, 0f, 0.6f), zone(EntryEdge.TOP, 0.6f, 1f)))
        val morph = ConfigMorph(from, to)

        // p=0.5 → lerp(0.4, 0.6, 0.5) = 0.5
        val result = resolveDisplayConfig(morph, 0.5f, emptyList(), 0f, null, null, from)

        assertEquals(0.5f, result.topZones[0].endRatio, 1e-5f)
    }

    @Test
    fun resolveDisplayConfig_movingDisplayOnly_returnsMoving() {
        val workCfg = topConfig(listOf(zone(EntryEdge.TOP, 0f, 1f)))
        val moving  = topConfig(listOf(zone(EntryEdge.TOP, 0f, 0.3f), zone(EntryEdge.TOP, 0.3f, 1f)))

        val result = resolveDisplayConfig(null, 0f, emptyList(), 0f, moving, null, workCfg)

        assertEquals(0.3f, result.topZones[0].endRatio, 1e-5f)
    }

    @Test
    fun resolveDisplayConfig_previewConfigOnly_returnsPreview() {
        val workCfg = topConfig(listOf(zone(EntryEdge.TOP, 0f, 1f)))
        val preview = topConfig(listOf(zone(EntryEdge.TOP, 0f, 0.5f), zone(EntryEdge.TOP, 0.5f, 1f)))

        val result = resolveDisplayConfig(null, 0f, emptyList(), 0f, null, preview, workCfg)

        assertEquals(preview, result)
    }

    @Test
    fun resolveDisplayConfig_allNull_returnsWorkConfig() {
        val workCfg = topConfig(listOf(zone(EntryEdge.TOP, 0f, 1f)))

        val result = resolveDisplayConfig(null, 0f, emptyList(), 0f, null, null, workCfg)

        assertEquals(workCfg, result)
    }

    // ============================================================
    // liftedKeyFor (4.7.8-E)
    // ============================================================

    @Test
    fun liftedKeyFor_floatPath_returnsNull() {
        val cfg = topConfig(listOf(zone(EntryEdge.TOP, 0f, 1f)))
        assertNull(liftedKeyFor(cfg, cfg, null, useFloatMovePreview = true, movingDisplay = cfg))
    }

    @Test
    fun liftedKeyFor_movingDisplayNull_returnsNull() {
        val z = zone(EntryEdge.TOP, 0f, 1f)
        val cfg = topConfig(listOf(z))
        assertNull(liftedKeyFor(cfg, cfg, z.key(), useFloatMovePreview = false, movingDisplay = null))
    }

    @Test
    fun liftedKeyFor_triggerMatchInDisplayConfig_returnsKey() {
        val z  = zone(EntryEdge.TOP, 0f, 0.5f, label = "X")
        val z2 = zone(EntryEdge.TOP, 0.5f, 1f)
        val cfg = topConfig(listOf(z, z2))
        // displayConfig == workConfig → trigger === 일치, z.key() 반환
        val result = liftedKeyFor(cfg, cfg, z.key(), useFloatMovePreview = false, movingDisplay = cfg)
        assertEquals(z.key(), result)
    }

    // ============================================================
    // buildCommitFloat (4.7.8-E)
    // ============================================================

    @Test
    fun buildCommitFloat_triggerFoundInAfter_returnsFloatWithCorrectFields() {
        val z  = zone(EntryEdge.TOP, 0f, 0.5f, label = "X")
        val z2 = zone(EntryEdge.TOP, 0.5f, 1f)
        val before = topConfig(listOf(z, z2))
        // after: z가 RIGHT 엣지로 이동 (copy → 동일 trigger 인스턴스 === 보존)
        val zOnRight = z.copy(edge = EntryEdge.RIGHT, startRatio = 0f, endRatio = 1f)
        val after = EdgeZoneConfig(
            topZones    = listOf(z2),
            bottomZones = listOf(zone(EntryEdge.BOTTOM, 0f, 1f)),
            leftZones   = listOf(zone(EntryEdge.LEFT, 0f, 1f)),
            rightZones  = listOf(zOnRight),
        )

        val result = buildCommitFloat(before, after, z.key(), z)

        assertTrue(result != null)
        assertEquals(ZoneStrip(EntryEdge.TOP, 0f, 0.5f), result!!.source)
        assertEquals(ZoneStrip(EntryEdge.RIGHT, 0f, 1f), result.target)
        assertEquals(0, result.colorIndex)   // before.TOP에서 z가 idx=0
        assertEquals("X", result.label)
    }

    @Test
    fun buildCommitFloat_triggerNotInAfter_returnsNull() {
        val z  = zone(EntryEdge.TOP, 0f, 0.5f, label = "X")
        val z2 = zone(EntryEdge.TOP, 0.5f, 1f)
        val before = topConfig(listOf(z, z2))
        // after에 z의 trigger가 없음 (z2만 남음)
        val after = topConfig(listOf(z2))

        assertNull(buildCommitFloat(before, after, z.key(), z))
    }
}
