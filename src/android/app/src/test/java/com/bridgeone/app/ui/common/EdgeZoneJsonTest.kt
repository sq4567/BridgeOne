package com.bridgeone.app.ui.common

import com.bridgeone.app.ui.components.touchpad.CornerOverlap
import com.bridgeone.app.ui.components.touchpad.EdgeSwipeMode
import com.bridgeone.app.ui.components.touchpad.EdgeZone
import com.bridgeone.app.ui.components.touchpad.EdgeZoneAction
import com.bridgeone.app.ui.components.touchpad.EdgeZoneConfig
import com.bridgeone.app.ui.components.touchpad.EdgeZoneTrigger
import com.bridgeone.app.ui.components.touchpad.EntryEdge
import com.bridgeone.app.ui.components.touchpad.PresetType
import com.bridgeone.app.ui.components.touchpad.SettingsType
import org.junit.Assert.*
import org.junit.Test

/**
 * EdgeZoneConfig JSON 직렬화/역직렬화 라운드트립 테스트
 *
 * Phase 4.7.2-B: EdgeZoneConfig ↔ JSON 직렬화 보존 고정.
 * org.json (testImplementation) 실구현 사용 — Android stub 아님.
 * `internal` 함수 접근: 동일 모듈 단일 :app, 동일 패키지 com.bridgeone.app.ui.common.
 */
class EdgeZoneJsonTest {

    // ======================================================
    // 기본 라운드트립
    // ======================================================

    @Test
    fun roundtrip_default_equalAfterRestore() {
        val original = EdgeZoneConfig.default()
        val json = edgeZoneConfigToJson(original)
        val restored = edgeZoneConfigFromJson(json)
        assertEquals("default() 라운드트립 동일", original, restored)
    }

    @Test
    fun roundtrip_entryEdge_restoredFromParentKey() {
        // edge 필드는 JSON에 저장되지 않고 zonesFromJsonArray 인자(EntryEdge.LEFT)로 복원
        val original = EdgeZoneConfig.default()
        val restored = edgeZoneConfigFromJson(edgeZoneConfigToJson(original))
        assertEquals("leftZone[0].edge = LEFT",
            EntryEdge.LEFT, restored.leftZones[0].edge)
        assertEquals("rightZone[0].edge = RIGHT",
            EntryEdge.RIGHT, restored.rightZones[0].edge)
        assertEquals("topZone[0].edge = TOP",
            EntryEdge.TOP, restored.topZones[0].edge)
        assertEquals("bottomZone[0].edge = BOTTOM",
            EntryEdge.BOTTOM, restored.bottomZones[0].edge)
    }

    @Test
    fun roundtrip_ratio_floatPrecisionPreserved() {
        val config = EdgeZoneConfig(
            topZones = listOf(
                EdgeZone(EntryEdge.TOP, 0.0f, 0.5f,
                    EdgeZoneTrigger.SingleAction(EdgeZoneAction.Unassigned, "", ""))
            ),
            bottomZones = emptyList(),
            leftZones = emptyList(),
            rightZones = emptyList()
        )
        val restored = edgeZoneConfigFromJson(edgeZoneConfigToJson(config))
        assertEquals("start=0.0f", 0.0f, restored.topZones[0].startRatio, 0.0001f)
        assertEquals("end=0.5f", 0.5f, restored.topZones[0].endRatio, 0.0001f)
    }

    // ======================================================
    // 다양한 액션 타입 라운드트립
    // ======================================================

    @Test
    fun roundtrip_toggleModeAction() {
        val config = singleZoneConfig(
            EdgeZoneAction.ToggleMode(EdgeSwipeMode.CLICK)
        )
        val restored = edgeZoneConfigFromJson(edgeZoneConfigToJson(config))
        val action = (restored.topZones[0].trigger as EdgeZoneTrigger.SingleAction).action
        assertEquals("ToggleMode 라운드트립",
            EdgeZoneAction.ToggleMode(EdgeSwipeMode.CLICK), action)
    }

    @Test
    fun roundtrip_cyclePresetAction() {
        val config = singleZoneConfig(
            EdgeZoneAction.CyclePreset(PresetType.DYNAMICS)
        )
        val restored = edgeZoneConfigFromJson(edgeZoneConfigToJson(config))
        val action = (restored.topZones[0].trigger as EdgeZoneTrigger.SingleAction).action
        assertEquals("CyclePreset 라운드트립",
            EdgeZoneAction.CyclePreset(PresetType.DYNAMICS), action)
    }

    @Test
    fun roundtrip_openSettingsAction() {
        val config = singleZoneConfig(
            EdgeZoneAction.OpenSettings(SettingsType.DPI)
        )
        val restored = edgeZoneConfigFromJson(edgeZoneConfigToJson(config))
        val action = (restored.topZones[0].trigger as EdgeZoneTrigger.SingleAction).action
        assertEquals("OpenSettings 라운드트립",
            EdgeZoneAction.OpenSettings(SettingsType.DPI), action)
    }

    @Test
    fun roundtrip_sendShortcutAction() {
        val config = singleZoneConfig(
            EdgeZoneAction.SendShortcut(modifierBits = 0x02, keyCodes = listOf(0x04))
        )
        val restored = edgeZoneConfigFromJson(edgeZoneConfigToJson(config))
        val action = (restored.topZones[0].trigger as EdgeZoneTrigger.SingleAction).action
        assertTrue("SendShortcut 타입", action is EdgeZoneAction.SendShortcut)
        action as EdgeZoneAction.SendShortcut
        assertEquals("modifierBits=0x02", 0x02, action.modifierBits)
        assertEquals("keyCodes=[0x04]", listOf(0x04), action.keyCodes)
    }

    @Test
    fun roundtrip_unassignedAction() {
        val config = singleZoneConfig(EdgeZoneAction.Unassigned)
        val restored = edgeZoneConfigFromJson(edgeZoneConfigToJson(config))
        val action = (restored.topZones[0].trigger as EdgeZoneTrigger.SingleAction).action
        assertEquals("Unassigned 라운드트립", EdgeZoneAction.Unassigned, action)
    }

    // ======================================================
    // cornerPriority
    // ======================================================

    @Test
    fun roundtrip_cornerPriority_preserved() {
        val original = EdgeZoneConfig.default()
        val restored = edgeZoneConfigFromJson(edgeZoneConfigToJson(original))
        assertEquals("cornerPriority 맵 동일", original.cornerPriority, restored.cornerPriority)
    }

    @Test
    fun roundtrip_cornerPriority_missing_usesDefault() {
        // cornerPriority 키가 없는 JSON (레거시) → defaultCornerEdge 로 복원
        val jsonWithoutCorner = """{"top":[],"bottom":[],"left":[],"right":[]}"""
        val restored = edgeZoneConfigFromJson(jsonWithoutCorner)
        assertEquals("TOP_LEFT → TOP",
            EntryEdge.TOP, restored.cornerPriority[CornerOverlap.TOP_LEFT])
        assertEquals("BOTTOM_RIGHT → BOTTOM",
            EntryEdge.BOTTOM, restored.cornerPriority[CornerOverlap.BOTTOM_RIGHT])
    }

    // ======================================================
    // 레거시 호환: trigger 없이 action만 있는 구버전 JSON
    // ======================================================

    @Test
    fun legacyFormat_actionOnly_parsedAsSingleAction() {
        // Phase 4.6.2 이전 형식: "action", "label", "iconKey" 직접 존재 (trigger 래퍼 없음)
        val legacyJson = """
        {
          "top": [{
            "edge": "TOP",
            "start": 0.0,
            "end": 1.0,
            "action": {"type": "Unassigned"},
            "label": "레거시",
            "iconKey": ""
          }],
          "bottom": [], "left": [], "right": []
        }
        """.trimIndent()
        val restored = edgeZoneConfigFromJson(legacyJson)
        assertEquals("레거시 1개 존", 1, restored.topZones.size)
        val trigger = restored.topZones[0].trigger
        assertTrue("trigger가 SingleAction", trigger is EdgeZoneTrigger.SingleAction)
        val sa = trigger as EdgeZoneTrigger.SingleAction
        assertEquals("레거시 label 복원", "레거시", sa.label)
        assertEquals("레거시 action 복원", EdgeZoneAction.Unassigned, sa.action)
    }

    // ======================================================
    // 알 수 없는 type → Unassigned fallback
    // ======================================================

    @Test
    fun unknownActionType_fallbackToUnassigned() {
        val jsonUnknown = """
        {
          "top": [{
            "edge": "TOP", "start": 0.0, "end": 1.0,
            "trigger": {
              "kind": "SingleAction",
              "action": {"type": "NonExistentFutureAction"},
              "label": "", "iconKey": "", "colorHex": ""
            }
          }],
          "bottom": [], "left": [], "right": []
        }
        """.trimIndent()
        val restored = edgeZoneConfigFromJson(jsonUnknown)
        val action = (restored.topZones[0].trigger as EdgeZoneTrigger.SingleAction).action
        assertEquals("알 수 없는 type → Unassigned", EdgeZoneAction.Unassigned, action)
    }

    // ======================================================
    // 헬퍼
    // ======================================================

    /** topZones에만 단일 존을 가진 최소 EdgeZoneConfig */
    private fun singleZoneConfig(action: EdgeZoneAction) = EdgeZoneConfig(
        topZones = listOf(
            EdgeZone(EntryEdge.TOP, 0f, 1f,
                EdgeZoneTrigger.SingleAction(action, "테스트", ""))
        ),
        bottomZones = emptyList(),
        leftZones = emptyList(),
        rightZones = emptyList()
    )
}
