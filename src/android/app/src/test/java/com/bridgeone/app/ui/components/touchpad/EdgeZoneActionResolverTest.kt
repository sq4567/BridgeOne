package com.bridgeone.app.ui.components.touchpad

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 4.7.5-A: EdgeZoneEditorScreen에서 추출한 순수 함수 테스트.
 */
class EdgeZoneActionResolverTest {

    // ── 테스트 헬퍼 ──
    private fun single(
        action: EdgeZoneAction,
        label: String = "",
        iconKey: String = "",
    ): EdgeZoneTrigger = EdgeZoneTrigger.SingleAction(action, label, iconKey)

    private fun zone(
        edge: EntryEdge,
        start: Float,
        end: Float,
        trigger: EdgeZoneTrigger = single(EdgeZoneAction.Unassigned),
    ): EdgeZone = EdgeZone(edge, start, end, trigger)

    private fun config(
        top: List<EdgeZone> = listOf(zone(EntryEdge.TOP, 0f, 1f)),
        bottom: List<EdgeZone> = listOf(zone(EntryEdge.BOTTOM, 0f, 1f)),
        left: List<EdgeZone> = listOf(zone(EntryEdge.LEFT, 0f, 1f)),
        right: List<EdgeZone> = listOf(zone(EntryEdge.RIGHT, 0f, 1f)),
    ): EdgeZoneConfig = EdgeZoneConfig(top, bottom, left, right)

    // ============================================================
    // ratioPresetsFor
    // ============================================================

    @Test
    fun ratioPresetsFor_n2_hasUniformAndStartHeavy() {
        val presets = EdgeZoneActionResolver.ratioPresetsFor(2)
        // 균등 + 왼쪽 크게 + 오른쪽 크게 = 3개 (양 끝/가운데 없음)
        assertEquals(3, presets.size)
        assertEquals("균등", presets[0].first)
        assertEquals(listOf("균등", "왼쪽 크게", "오른쪽 크게"), presets.map { it.first })
    }

    @Test
    fun ratioPresetsFor_n3_hasFivePresets() {
        val presets = EdgeZoneActionResolver.ratioPresetsFor(3)
        assertEquals(5, presets.size)
        assertEquals(listOf("균등", "왼쪽 크게", "오른쪽 크게", "양 끝 크게", "가운데 크게"), presets.map { it.first })
    }

    @Test
    fun ratioPresetsFor_n4_hasFivePresets() {
        val presets = EdgeZoneActionResolver.ratioPresetsFor(4)
        assertEquals(5, presets.size)
    }

    @Test
    fun ratioPresetsFor_allRatiosSumToOne() {
        for (n in 2..4) {
            EdgeZoneActionResolver.ratioPresetsFor(n).forEach { (name, ratios) ->
                assertEquals("$n-$name 비율 합", 1f, ratios.sum(), 1e-5f)
                assertEquals("$n-$name 개수", n, ratios.size)
            }
        }
    }

    @Test
    fun ratioPresetsFor_n5_onlyUniform() {
        // startHeavy null + 양 끝/가운데 분기 없음 → 균등만
        val presets = EdgeZoneActionResolver.ratioPresetsFor(5)
        assertEquals(1, presets.size)
        assertEquals("균등", presets[0].first)
        assertEquals(1f, presets[0].second.sum(), 1e-5f)
    }

    // ============================================================
    // domainOf
    // ============================================================

    @Test
    fun domainOf_toggleModeMapsByMode() {
        assertEquals(ActionDomain.CLICK, EdgeZoneActionResolver.domainOf(EdgeZoneAction.ToggleMode(EdgeSwipeMode.CLICK)))
        assertEquals(ActionDomain.SCROLL, EdgeZoneActionResolver.domainOf(EdgeZoneAction.ToggleMode(EdgeSwipeMode.SCROLL)))
        assertEquals(ActionDomain.MOVE, EdgeZoneActionResolver.domainOf(EdgeZoneAction.ToggleMode(EdgeSwipeMode.MOVE)))
        // CURSOR는 CLICK 도메인으로 매핑
        assertEquals(ActionDomain.CLICK, EdgeZoneActionResolver.domainOf(EdgeZoneAction.ToggleMode(EdgeSwipeMode.CURSOR)))
    }

    @Test
    fun domainOf_variousActions() {
        assertEquals(ActionDomain.DPI, EdgeZoneActionResolver.domainOf(EdgeZoneAction.OpenSettings(SettingsType.DPI)))
        assertEquals(ActionDomain.SCROLL_SPEED, EdgeZoneActionResolver.domainOf(EdgeZoneAction.OpenSettings(SettingsType.SCROLL_SPEED)))
        assertEquals(ActionDomain.DYNAMICS, EdgeZoneActionResolver.domainOf(EdgeZoneAction.CyclePreset(PresetType.DYNAMICS)))
        assertEquals(ActionDomain.MODE_PRESET, EdgeZoneActionResolver.domainOf(EdgeZoneAction.CyclePreset(PresetType.MODE)))
        assertEquals(ActionDomain.COMBO, EdgeZoneActionResolver.domainOf(EdgeZoneAction.SendShortcut(0)))
        assertEquals(ActionDomain.MACRO, EdgeZoneActionResolver.domainOf(EdgeZoneAction.SendMacro()))
        assertEquals(ActionDomain.HISTORY, EdgeZoneActionResolver.domainOf(EdgeZoneAction.RestorePreviousMode))
        assertEquals(ActionDomain.MOUSE_HOLD, EdgeZoneActionResolver.domainOf(EdgeZoneAction.MouseHoldToggle(MouseButton.LEFT)))
        assertEquals(ActionDomain.PAGE, EdgeZoneActionResolver.domainOf(EdgeZoneAction.JumpToPage(0)))
        assertEquals(ActionDomain.UNASSIGNED, EdgeZoneActionResolver.domainOf(EdgeZoneAction.Unassigned))
    }

    // ============================================================
    // actionEquals
    // ============================================================

    @Test
    fun actionEquals_sameTypeSameValue_true() {
        assertTrue(EdgeZoneActionResolver.actionEquals(EdgeZoneAction.SetDynamicsPreset(1), EdgeZoneAction.SetDynamicsPreset(1)))
        assertTrue(EdgeZoneActionResolver.actionEquals(EdgeZoneAction.Unassigned, EdgeZoneAction.Unassigned))
        assertTrue(EdgeZoneActionResolver.actionEquals(EdgeZoneAction.JumpToPage(2), EdgeZoneAction.JumpToPage(2)))
        assertTrue(
            EdgeZoneActionResolver.actionEquals(
                EdgeZoneAction.MouseHoldToggle(MouseButton.LEFT, MouseHoldMode.HOLD),
                EdgeZoneAction.MouseHoldToggle(MouseButton.LEFT, MouseHoldMode.HOLD),
            )
        )
    }

    @Test
    fun actionEquals_sameTypeDifferentValue_false() {
        assertFalse(EdgeZoneActionResolver.actionEquals(EdgeZoneAction.SetDynamicsPreset(1), EdgeZoneAction.SetDynamicsPreset(2)))
        assertFalse(EdgeZoneActionResolver.actionEquals(EdgeZoneAction.JumpToPage(1), EdgeZoneAction.JumpToPage(2)))
    }

    @Test
    fun actionEquals_differentType_false() {
        assertFalse(EdgeZoneActionResolver.actionEquals(EdgeZoneAction.Unassigned, EdgeZoneAction.SetDynamicsPreset(0)))
    }

    @Test
    fun actionEquals_sendShortcut_presetComparedByLabel() {
        // 둘 다 presetLabel 있으면 label로 비교 (키 조합이 달라도 같은 프리셋이면 true)
        val a = EdgeZoneAction.SendShortcut(modifierBits = 1, keyCodes = listOf(4), presetLabel = "복사")
        val b = EdgeZoneAction.SendShortcut(modifierBits = 2, keyCodes = listOf(5), presetLabel = "복사")
        assertTrue(EdgeZoneActionResolver.actionEquals(a, b))
    }

    @Test
    fun actionEquals_sendShortcut_customComparedByModAndKeys() {
        // presetLabel 비어있으면 mod+key로 비교
        val a = EdgeZoneAction.SendShortcut(modifierBits = 1, keyCodes = listOf(4))
        val same = EdgeZoneAction.SendShortcut(modifierBits = 1, keyCodes = listOf(4))
        val diff = EdgeZoneAction.SendShortcut(modifierBits = 1, keyCodes = listOf(5))
        assertTrue(EdgeZoneActionResolver.actionEquals(a, same))
        assertFalse(EdgeZoneActionResolver.actionEquals(a, diff))
    }

    // ============================================================
    // describeUndoStep
    // ============================================================

    @Test
    fun describeUndoStep_cornerPriorityChange() {
        val from = config()
        val to = from.copy(
            cornerPriority = from.cornerPriority + (CornerOverlap.TOP_LEFT to EntryEdge.LEFT)
        )
        assertEquals("코너 우선순위 변경", EdgeZoneActionResolver.describeUndoStep(from, to))
    }

    @Test
    fun describeUndoStep_zoneSplitAndMerge() {
        val one = config(top = listOf(zone(EntryEdge.TOP, 0f, 1f)))
        val two = config(top = listOf(zone(EntryEdge.TOP, 0f, 0.5f), zone(EntryEdge.TOP, 0.5f, 1f)))
        assertEquals("상단 존 분할", EdgeZoneActionResolver.describeUndoStep(one, two))
        assertEquals("상단 존 병합/삭제", EdgeZoneActionResolver.describeUndoStep(two, one))
    }

    @Test
    fun describeUndoStep_ratioAdjust() {
        val from = config(top = listOf(zone(EntryEdge.TOP, 0f, 0.5f), zone(EntryEdge.TOP, 0.5f, 1f)))
        val to = config(top = listOf(zone(EntryEdge.TOP, 0f, 0.7f), zone(EntryEdge.TOP, 0.7f, 1f)))
        assertEquals("상단 비율 조정", EdgeZoneActionResolver.describeUndoStep(from, to))
    }

    @Test
    fun describeUndoStep_labelAndIconAndAction() {
        val base = config(top = listOf(zone(EntryEdge.TOP, 0f, 1f, single(EdgeZoneAction.Unassigned, label = "A", iconKey = "Mouse"))))
        val labelChanged = config(top = listOf(zone(EntryEdge.TOP, 0f, 1f, single(EdgeZoneAction.Unassigned, label = "B", iconKey = "Mouse"))))
        assertEquals("상단 라벨 → \"B\"", EdgeZoneActionResolver.describeUndoStep(base, labelChanged))

        val iconChanged = config(top = listOf(zone(EntryEdge.TOP, 0f, 1f, single(EdgeZoneAction.Unassigned, label = "A", iconKey = "Keyboard"))))
        assertEquals("상단 아이콘 변경", EdgeZoneActionResolver.describeUndoStep(base, iconChanged))

        val actionChanged = config(top = listOf(zone(EntryEdge.TOP, 0f, 1f, single(EdgeZoneAction.SendMacro(), label = "A", iconKey = "Mouse"))))
        val desc = EdgeZoneActionResolver.describeUndoStep(base, actionChanged)
        assertTrue("액션 변경 설명: $desc", desc.startsWith("상단 액션 → "))
    }

    @Test
    fun describeUndoStep_noChange() {
        val c = config()
        assertEquals("설정 변경", EdgeZoneActionResolver.describeUndoStep(c, c.copy()))
    }

    // ============================================================
    // migrateDynamicsIndicesAfterDelete
    // ============================================================

    @Test
    fun migrate_removedIndexBecomesUnassigned() {
        val cfg = config(top = listOf(zone(EntryEdge.TOP, 0f, 1f, single(EdgeZoneAction.SetDynamicsPreset(2)))))
        val migrated = EdgeZoneActionResolver.migrateDynamicsIndicesAfterDelete(cfg, removedGlobalIndex = 2)
        val action = (migrated.topZones[0].trigger as EdgeZoneTrigger.SingleAction).action
        assertEquals(EdgeZoneAction.Unassigned, action)
    }

    @Test
    fun migrate_higherIndexShiftsDown_lowerStays() {
        val cfg = config(
            top = listOf(zone(EntryEdge.TOP, 0f, 1f, single(EdgeZoneAction.SetDynamicsPreset(3)))),
            bottom = listOf(zone(EntryEdge.BOTTOM, 0f, 1f, single(EdgeZoneAction.SetDynamicsPreset(1)))),
        )
        val migrated = EdgeZoneActionResolver.migrateDynamicsIndicesAfterDelete(cfg, removedGlobalIndex = 2)
        val top = (migrated.topZones[0].trigger as EdgeZoneTrigger.SingleAction).action
        val bottom = (migrated.bottomZones[0].trigger as EdgeZoneTrigger.SingleAction).action
        assertEquals(EdgeZoneAction.SetDynamicsPreset(2), top)  // 3 → 2
        assertEquals(EdgeZoneAction.SetDynamicsPreset(1), bottom)  // 1 유지
    }

    @Test
    fun migrate_rotationCandidatesAlsoMigrated() {
        val rotation = EdgeZoneTrigger.Rotation(
            candidates = listOf(
                RotationCandidate(EdgeZoneAction.SetDynamicsPreset(3), "", ""),
                RotationCandidate(EdgeZoneAction.SetDynamicsPreset(2), "", ""),
            ),
            intervalMs = 500,
        )
        val cfg = config(top = listOf(zone(EntryEdge.TOP, 0f, 1f, rotation)))
        val migrated = EdgeZoneActionResolver.migrateDynamicsIndicesAfterDelete(cfg, removedGlobalIndex = 2)
        val cands = (migrated.topZones[0].trigger as EdgeZoneTrigger.Rotation).candidates
        assertEquals(EdgeZoneAction.SetDynamicsPreset(2), cands[0].action)  // 3 → 2
        assertEquals(EdgeZoneAction.Unassigned, cands[1].action)  // 2 → Unassigned
    }
}
