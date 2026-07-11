package com.bridgeone.app.ui.components.touchpad

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 4.9.3: AbsolutePointingPad(Page 3) 엣지존 화이트리스트 필터 테스트.
 */
class AbsolutePadActionFilterTest {

    // ============================================================
    // isAbsolutePadAllowed
    // ============================================================

    @Test
    fun isAbsolutePadAllowed_allowsWhitelistedActions() {
        assertTrue(isAbsolutePadAllowed(EdgeZoneAction.SendMacro()))
        assertTrue(isAbsolutePadAllowed(EdgeZoneAction.SendShortcut(modifierBits = 0x01, keyCodes = listOf(0x04))))
        assertTrue(isAbsolutePadAllowed(EdgeZoneAction.CyclePage(PageNav.NEXT)))
        assertTrue(isAbsolutePadAllowed(EdgeZoneAction.JumpToPage(0)))
        assertTrue(isAbsolutePadAllowed(EdgeZoneAction.SetClickMode(ClickMode.LEFT_CLICK)))
        assertTrue(isAbsolutePadAllowed(EdgeZoneAction.ToggleMode(EdgeSwipeMode.CLICK)))
        assertTrue(isAbsolutePadAllowed(EdgeZoneAction.MouseHoldToggle(MouseButton.LEFT)))
        assertTrue(isAbsolutePadAllowed(EdgeZoneAction.RestorePreviousMode))
        assertTrue(isAbsolutePadAllowed(EdgeZoneAction.ToggleAbsoluteZoom))
        assertTrue(isAbsolutePadAllowed(EdgeZoneAction.ToggleAbsoluteDrag))
    }

    @Test
    fun isAbsolutePadAllowed_rejectsCursorToggleMode() {
        // CURSOR는 EdgeZoneActionResolver.domainOf에서 ActionDomain.CLICK으로 매핑되므로
        // 도메인 단위가 아닌 액션 타입 단위 필터로 이 함정을 회피해야 한다.
        assertFalse(isAbsolutePadAllowed(EdgeZoneAction.ToggleMode(EdgeSwipeMode.CURSOR)))
    }

    @Test
    fun isAbsolutePadAllowed_rejectsDeltaAndScrollAndDpiActions() {
        assertFalse(isAbsolutePadAllowed(EdgeZoneAction.ToggleMode(EdgeSwipeMode.SCROLL)))
        assertFalse(isAbsolutePadAllowed(EdgeZoneAction.ToggleMode(EdgeSwipeMode.MOVE)))
        assertFalse(isAbsolutePadAllowed(EdgeZoneAction.ToggleMode(EdgeSwipeMode.DPI)))
        assertFalse(isAbsolutePadAllowed(EdgeZoneAction.ToggleMode(EdgeSwipeMode.SCROLL_SPEED)))
        assertFalse(isAbsolutePadAllowed(EdgeZoneAction.ToggleMode(EdgeSwipeMode.DYNAMICS)))
        assertFalse(isAbsolutePadAllowed(EdgeZoneAction.SetMoveMode(MoveMode.FREE)))
        assertFalse(isAbsolutePadAllowed(EdgeZoneAction.SetDpi(DpiLevel.entries.first())))
        assertFalse(isAbsolutePadAllowed(EdgeZoneAction.SetCustomDpi(1.5f)))
        assertFalse(isAbsolutePadAllowed(EdgeZoneAction.SetScrollMode(ScrollMode.NORMAL_SCROLL)))
        assertFalse(isAbsolutePadAllowed(EdgeZoneAction.SetScrollSpeed(ScrollSensitivity.entries.first())))
        assertFalse(isAbsolutePadAllowed(EdgeZoneAction.SetCustomScrollSpeed(1.5f)))
        assertFalse(isAbsolutePadAllowed(EdgeZoneAction.SwapScrollMode))
    }

    @Test
    fun isAbsolutePadAllowed_rejectsMultiCursorAndPresetActions() {
        assertFalse(isAbsolutePadAllowed(EdgeZoneAction.ToggleMultiCursor))
        assertFalse(isAbsolutePadAllowed(EdgeZoneAction.ToggleMultiCursorLayout))
        assertFalse(isAbsolutePadAllowed(EdgeZoneAction.SetCursorCount(2)))
        assertFalse(isAbsolutePadAllowed(EdgeZoneAction.ActivatePad(0)))
        assertFalse(isAbsolutePadAllowed(EdgeZoneAction.CyclePad(PageNav.NEXT)))
        assertFalse(isAbsolutePadAllowed(EdgeZoneAction.CyclePreset(PresetType.DYNAMICS)))
        assertFalse(isAbsolutePadAllowed(EdgeZoneAction.OpenSettings(SettingsType.DPI)))
        assertFalse(isAbsolutePadAllowed(EdgeZoneAction.SetDynamicsPreset(0)))
        assertFalse(isAbsolutePadAllowed(EdgeZoneAction.SetModePreset(0)))
    }

    @Test
    fun isAbsolutePadAllowed_rejectsUnassigned() {
        assertFalse(isAbsolutePadAllowed(EdgeZoneAction.Unassigned))
    }

    // ============================================================
    // filterConfigForAbsolutePad
    // ============================================================

    @Test
    fun filterConfigForAbsolutePad_default_onlyLeftUpperClickSurvives() {
        val filtered = filterConfigForAbsolutePad(EdgeZoneConfig.default())

        // TOP: CyclePreset(DYNAMICS/MODE) 둘 다 배제 → Unassigned
        filtered.topZones.forEach { zone ->
            assertTrue(zone.trigger is EdgeZoneTrigger.SingleAction)
            assertTrue((zone.trigger as EdgeZoneTrigger.SingleAction).action is EdgeZoneAction.Unassigned)
        }

        // BOTTOM: 원래 Unassigned
        filtered.bottomZones.forEach { zone ->
            assertTrue((zone.trigger as EdgeZoneTrigger.SingleAction).action is EdgeZoneAction.Unassigned)
        }

        // LEFT: 위쪽 절반만 ToggleMode(CLICK) 유지, 아래쪽 절반(SCROLL)은 배제
        val leftUpper = filtered.leftZones[0].trigger as EdgeZoneTrigger.SingleAction
        assertEquals(EdgeZoneAction.ToggleMode(EdgeSwipeMode.CLICK), leftUpper.action)
        val leftLower = filtered.leftZones[1].trigger as EdgeZoneTrigger.SingleAction
        assertTrue(leftLower.action is EdgeZoneAction.Unassigned)

        // RIGHT: MOVE/DPI 둘 다 배제
        filtered.rightZones.forEach { zone ->
            assertTrue((zone.trigger as EdgeZoneTrigger.SingleAction).action is EdgeZoneAction.Unassigned)
        }
    }

    @Test
    fun filterConfigForAbsolutePad_preservesCornerPriority() {
        val original = EdgeZoneConfig.default()
        val filtered = filterConfigForAbsolutePad(original)
        assertEquals(original.cornerPriority, filtered.cornerPriority)
    }

    @Test
    fun filterConfigForAbsolutePad_doesNotMutateOriginalConfig() {
        val original = EdgeZoneConfig.default()
        filterConfigForAbsolutePad(original)
        // 원본은 여전히 필터되지 않은 상태(불변성 확인)
        val leftUpper = original.leftZones[0].trigger as EdgeZoneTrigger.SingleAction
        assertEquals(EdgeZoneAction.ToggleMode(EdgeSwipeMode.CLICK), leftUpper.action)
        val leftLower = original.leftZones[1].trigger as EdgeZoneTrigger.SingleAction
        assertEquals(EdgeZoneAction.ToggleMode(EdgeSwipeMode.SCROLL), leftLower.action)
    }

    @Test
    fun filterConfigForAbsolutePad_rotationPartialFilter_keepsAllowedCandidatesOnly() {
        val rotation = EdgeZoneTrigger.Rotation(
            candidates = listOf(
                RotationCandidate(EdgeZoneAction.SendMacro(), "매크로", "Keyboard"),
                RotationCandidate(EdgeZoneAction.SetDpi(DpiLevel.entries.first()), "DPI", "Speed"),
            ),
            intervalMs = 500
        )
        val config = EdgeZoneConfig(
            topZones = emptyList(),
            bottomZones = emptyList(),
            leftZones = listOf(EdgeZone(EntryEdge.LEFT, 0f, 1f, rotation)),
            rightZones = emptyList()
        )

        val filtered = filterConfigForAbsolutePad(config)
        val filteredTrigger = filtered.leftZones[0].trigger as EdgeZoneTrigger.Rotation
        assertEquals(1, filteredTrigger.candidates.size)
        assertTrue(filteredTrigger.candidates[0].action is EdgeZoneAction.SendMacro)
    }

    @Test
    fun filterConfigForAbsolutePad_rotationAllRejected_becomesUnassigned() {
        val rotation = EdgeZoneTrigger.Rotation(
            candidates = listOf(
                RotationCandidate(EdgeZoneAction.SetDpi(DpiLevel.entries.first()), "DPI", "Speed"),
                RotationCandidate(EdgeZoneAction.SetScrollMode(ScrollMode.NORMAL_SCROLL), "스크롤", "SwapVert"),
            ),
            intervalMs = 500
        )
        val config = EdgeZoneConfig(
            topZones = emptyList(),
            bottomZones = emptyList(),
            leftZones = listOf(EdgeZone(EntryEdge.LEFT, 0f, 1f, rotation)),
            rightZones = emptyList()
        )

        val filtered = filterConfigForAbsolutePad(config)
        val filteredTrigger = filtered.leftZones[0].trigger as EdgeZoneTrigger.SingleAction
        assertTrue(filteredTrigger.action is EdgeZoneAction.Unassigned)
    }
}
