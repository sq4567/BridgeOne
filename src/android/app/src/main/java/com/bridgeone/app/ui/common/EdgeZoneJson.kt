package com.bridgeone.app.ui.common

import com.bridgeone.app.ui.components.touchpad.ClickMode
import com.bridgeone.app.ui.components.touchpad.CornerOverlap
import com.bridgeone.app.ui.components.touchpad.DpiLevel
import com.bridgeone.app.ui.components.touchpad.EdgeSwipeMode
import com.bridgeone.app.ui.components.touchpad.EdgeZone
import com.bridgeone.app.ui.components.touchpad.EdgeZoneAction
import com.bridgeone.app.ui.components.touchpad.EdgeZoneConfig
import com.bridgeone.app.ui.components.touchpad.InputModeCheck
import com.bridgeone.app.ui.components.touchpad.EdgeZoneTrigger
import com.bridgeone.app.ui.components.touchpad.EntryEdge
import com.bridgeone.app.ui.components.touchpad.MacroStep
import com.bridgeone.app.ui.components.touchpad.MacroStepKind
import com.bridgeone.app.ui.components.touchpad.MULTI_CURSOR_COUNT_MAX
import com.bridgeone.app.ui.components.touchpad.MULTI_CURSOR_COUNT_MIN
import com.bridgeone.app.ui.components.touchpad.MouseButton
import com.bridgeone.app.ui.components.touchpad.MouseHoldMode
import com.bridgeone.app.ui.components.touchpad.MoveMode
import com.bridgeone.app.ui.components.touchpad.PageNav
import com.bridgeone.app.ui.components.touchpad.PresetType
import com.bridgeone.app.ui.components.touchpad.RotationCandidate
import com.bridgeone.app.ui.components.touchpad.ScrollMode
import com.bridgeone.app.ui.components.touchpad.ScrollSensitivity
import com.bridgeone.app.ui.components.touchpad.SettingsType
import com.bridgeone.app.ui.components.touchpad.defaultCornerEdge
import org.json.JSONArray
import org.json.JSONObject

// EdgeZoneConfig JSON 직렬화/역직렬화 공용 유틸 (Phase 4.6.3)
// SendMacro 직렬화 추가
// StandardModePage와 EdgeZonePresetsRepository에서 함께 사용

internal fun edgeZoneConfigToJson(config: EdgeZoneConfig): String =
    edgeZoneConfigToJsonObject(config).toString()

internal fun edgeZoneConfigToJsonObject(config: EdgeZoneConfig): JSONObject {
    val obj = JSONObject()
    obj.put("top", zonesToJsonArray(config.topZones))
    obj.put("bottom", zonesToJsonArray(config.bottomZones))
    obj.put("left", zonesToJsonArray(config.leftZones))
    obj.put("right", zonesToJsonArray(config.rightZones))
    val cornerObj = JSONObject()
    config.cornerPriority.forEach { (corner, edge) -> cornerObj.put(corner.name, edge.name) }
    obj.put("cornerPriority", cornerObj)
    return obj
}

internal fun edgeZoneConfigFromJson(json: String): EdgeZoneConfig =
    edgeZoneConfigFromJsonObject(JSONObject(json))

internal fun edgeZoneConfigFromJsonObject(obj: JSONObject): EdgeZoneConfig {
    val cornerPriority: Map<CornerOverlap, EntryEdge> = if (obj.has("cornerPriority")) {
        val cObj = obj.getJSONObject("cornerPriority")
        CornerOverlap.entries.mapNotNull { corner ->
            val edgeName = cObj.optString(corner.name, "")
            if (edgeName.isEmpty()) null
            else try { corner to EntryEdge.valueOf(edgeName) } catch (_: IllegalArgumentException) { null }
        }.toMap()
    } else {
        CornerOverlap.entries.associateWith { defaultCornerEdge(it) }
    }
    return EdgeZoneConfig(
        topZones      = zonesFromJsonArray(obj.getJSONArray("top"),    EntryEdge.TOP),
        bottomZones   = zonesFromJsonArray(obj.getJSONArray("bottom"), EntryEdge.BOTTOM),
        leftZones     = zonesFromJsonArray(obj.getJSONArray("left"),   EntryEdge.LEFT),
        rightZones    = zonesFromJsonArray(obj.getJSONArray("right"),  EntryEdge.RIGHT),
        cornerPriority = cornerPriority
    )
}

private fun zonesToJsonArray(zones: List<EdgeZone>): JSONArray {
    val arr = JSONArray()
    zones.forEach { zone ->
        val z = JSONObject()
        z.put("edge", zone.edge.name)
        z.put("start", zone.startRatio)
        z.put("end", zone.endRatio)
        z.put("trigger", zoneTriggerToJson(zone.trigger))
        arr.put(z)
    }
    return arr
}

private fun zoneTriggerToJson(trigger: EdgeZoneTrigger): JSONObject {
    val obj = JSONObject()
    when (trigger) {
        is EdgeZoneTrigger.SingleAction -> {
            obj.put("kind", "SingleAction")
            obj.put("action", zoneActionToJson(trigger.action))
            obj.put("label", trigger.label)
            obj.put("iconKey", trigger.iconKey)
            obj.put("colorHex", trigger.colorHex)
        }
        is EdgeZoneTrigger.Rotation -> {
            obj.put("kind", "Rotation")
            obj.put("intervalMs", trigger.intervalMs)
            val arr = JSONArray()
            trigger.candidates.forEach { c ->
                val cObj = JSONObject()
                cObj.put("action", zoneActionToJson(c.action))
                cObj.put("label", c.label)
                cObj.put("iconKey", c.iconKey)
                cObj.put("colorHex", c.colorHex)
                arr.put(cObj)
            }
            obj.put("candidates", arr)
        }
    }
    return obj
}

private fun zoneActionToJson(action: EdgeZoneAction): JSONObject {
    val obj = JSONObject()
    when (action) {
        is EdgeZoneAction.ToggleMode      -> { obj.put("type", "ToggleMode");      obj.put("mode",      action.mode.name) }
        is EdgeZoneAction.CyclePreset     -> { obj.put("type", "CyclePreset");     obj.put("preset",    action.presetType.name) }
        is EdgeZoneAction.OpenSettings    -> { obj.put("type", "OpenSettings");    obj.put("settings",  action.settingsType.name) }
        EdgeZoneAction.Unassigned         -> obj.put("type", "Unassigned")
        is EdgeZoneAction.SetDpi          -> { obj.put("type", "SetDpi");          obj.put("dpi",       action.level.name) }
        is EdgeZoneAction.SetScrollSpeed  -> { obj.put("type", "SetScrollSpeed");  obj.put("speed",     action.sensitivity.name) }
        is EdgeZoneAction.SetModePreset   -> { obj.put("type", "SetModePreset");   obj.put("index",     action.index) }
        is EdgeZoneAction.SetDynamicsPreset -> { obj.put("type", "SetDynamicsPreset"); obj.put("index", action.index) }
        is EdgeZoneAction.SetClickMode    -> { obj.put("type", "SetClickMode");    obj.put("click",     action.mode.name) }
        is EdgeZoneAction.SetMoveMode     -> { obj.put("type", "SetMoveMode");     obj.put("move",      action.mode.name) }
        is EdgeZoneAction.SetScrollMode   -> { obj.put("type", "SetScrollMode");   obj.put("scroll",    action.mode.name) }
        EdgeZoneAction.SwapScrollMode      -> obj.put("type", "SwapScrollMode")
        is EdgeZoneAction.SetCustomDpi     -> { obj.put("type", "SetCustomDpi");      obj.put("multiplier", action.multiplier) }
        is EdgeZoneAction.SetCustomScrollSpeed -> { obj.put("type", "SetCustomScrollSpeed"); obj.put("multiplier", action.multiplier) }
        EdgeZoneAction.RestorePreviousMode     -> obj.put("type", "RestorePreviousMode")
        is EdgeZoneAction.SendShortcut -> {
            obj.put("type", "SendShortcut")
            obj.put("mod", action.modifierBits)
            obj.put("keys", JSONArray(action.keyCodes))
            obj.put("hold", action.hold)
            obj.put("preset", action.presetLabel)
        }
        is EdgeZoneAction.SendMacro -> {
            obj.put("type", "SendMacro")
            val stepsArr = JSONArray()
            action.steps.forEach { step ->
                val stepObj = JSONObject()
                stepObj.put("mod", step.modifierBits)
                stepObj.put("keys", JSONArray(step.keyCodes))
                step.delayAfterMs?.let { stepObj.put("d", it) }
                if (step.repeatCount > 1) stepObj.put("r", step.repeatCount)
                step.groupId?.let { stepObj.put("g", it) }
                if (step.kind != MacroStepKind.TAP) stepObj.put("k", step.kind.name)
                stepsArr.put(stepObj)
            }
            obj.put("steps", stepsArr)
            obj.put("delay", action.stepDelayMs)
            obj.put("preset", action.presetLabel)
            if (action.inputModeCheck != InputModeCheck.NONE) obj.put("imeCheck", action.inputModeCheck.name)
            if (action.groupNames.isNotEmpty()) {
                val gn = JSONObject()
                action.groupNames.forEach { (k, v) -> gn.put(k.toString(), v) }
                obj.put("groups", gn)
            }
        }
        is EdgeZoneAction.MouseHoldToggle -> {
            obj.put("type", "MouseHoldToggle")
            obj.put("button", action.button.name)
            obj.put("holdMode", action.mode.name)
        }
        is EdgeZoneAction.CyclePage -> {
            obj.put("type", "CyclePage")
            obj.put("dir", action.direction.name)
        }
        is EdgeZoneAction.JumpToPage -> {
            obj.put("type", "JumpToPage")
            obj.put("page", action.pageIndex)
        }
        EdgeZoneAction.ToggleMultiCursor -> obj.put("type", "ToggleMultiCursor")
        EdgeZoneAction.ToggleMultiCursorLayout -> obj.put("type", "ToggleMultiCursorLayout")
        is EdgeZoneAction.SetCursorCount -> {
            obj.put("type", "SetCursorCount")
            obj.put("count", action.count)
        }
        is EdgeZoneAction.ActivatePad -> {
            obj.put("type", "ActivatePad")
            obj.put("index", action.index)
        }
        is EdgeZoneAction.CyclePad -> {
            obj.put("type", "CyclePad")
            obj.put("dir", action.direction.name)
        }
    }
    return obj
}

private fun zonesFromJsonArray(arr: JSONArray, edge: EntryEdge): List<EdgeZone> =
    (0 until arr.length()).map { i ->
        val z = arr.getJSONObject(i)
        val trigger = if (z.has("trigger")) {
            zoneTriggerFromJson(z.getJSONObject("trigger"))
        } else {
            // 레거시 형식 (Phase 4.6.2 이전 저장 파일) 호환
            EdgeZoneTrigger.SingleAction(
                action   = zoneActionFromJson(z.getJSONObject("action")),
                label    = z.optString("label", ""),
                iconKey  = z.optString("iconKey", ""),
                colorHex = z.optString("colorHex", "")
            )
        }
        EdgeZone(
            edge       = edge,
            startRatio = z.getDouble("start").toFloat(),
            endRatio   = z.getDouble("end").toFloat(),
            trigger    = trigger
        )
    }

private fun zoneTriggerFromJson(obj: JSONObject): EdgeZoneTrigger = when (obj.getString("kind")) {
    "Rotation" -> {
        val arr = obj.getJSONArray("candidates")
        val candidates = (0 until arr.length()).map { i ->
            val c = arr.getJSONObject(i)
            RotationCandidate(
                action   = zoneActionFromJson(c.getJSONObject("action")),
                label    = c.optString("label", ""),
                iconKey  = c.optString("iconKey", ""),
                colorHex = c.optString("colorHex", "")
            )
        }
        EdgeZoneTrigger.Rotation(
            candidates  = candidates,
            intervalMs  = obj.optInt("intervalMs", com.bridgeone.app.ui.common.EdgeSwipeConstants.EDGE_ZONE_ROTATION_INTERVAL_DEFAULT_MS)
        )
    }
    else -> EdgeZoneTrigger.SingleAction(
        action   = zoneActionFromJson(obj.getJSONObject("action")),
        label    = obj.optString("label", ""),
        iconKey  = obj.optString("iconKey", ""),
        colorHex = obj.optString("colorHex", "")
    )
}

private fun zoneActionFromJson(obj: JSONObject): EdgeZoneAction = when (obj.getString("type")) {
    "ToggleMode"       -> EdgeZoneAction.ToggleMode(EdgeSwipeMode.valueOf(obj.getString("mode")))
    "CyclePreset"      -> EdgeZoneAction.CyclePreset(PresetType.valueOf(obj.getString("preset")))
    "OpenSettings"     -> EdgeZoneAction.OpenSettings(SettingsType.valueOf(obj.getString("settings")))
    "SetDpi"           -> EdgeZoneAction.SetDpi(DpiLevel.valueOf(obj.getString("dpi")))
    "SetScrollSpeed"   -> EdgeZoneAction.SetScrollSpeed(ScrollSensitivity.valueOf(obj.getString("speed")))
    "SetModePreset"    -> EdgeZoneAction.SetModePreset(obj.optInt("index", 0).coerceIn(0, MODE_PRESETS.lastIndex))
    "SetDynamicsPreset"-> EdgeZoneAction.SetDynamicsPreset(obj.optInt("index", 0).coerceIn(0, DYNAMICS_PRESETS.lastIndex))
    "SetClickMode"     -> EdgeZoneAction.SetClickMode(ClickMode.valueOf(obj.getString("click")))
    "SetMoveMode"      -> EdgeZoneAction.SetMoveMode(MoveMode.valueOf(obj.getString("move")))
    "SetScrollMode"    -> EdgeZoneAction.SetScrollMode(ScrollMode.valueOf(obj.getString("scroll")))
    "SwapScrollMode"        -> EdgeZoneAction.SwapScrollMode
    "SetCustomDpi"          -> EdgeZoneAction.SetCustomDpi(obj.optDouble("multiplier", 1.0).toFloat())
    "SetCustomScrollSpeed"  -> EdgeZoneAction.SetCustomScrollSpeed(obj.optDouble("multiplier", 1.0).toFloat())
    "RestorePreviousMode"   -> EdgeZoneAction.RestorePreviousMode
    "SendShortcut"    -> {
        val keysArr = obj.optJSONArray("keys")
        val keyCodes = if (keysArr != null) {
            List(keysArr.length()) { i -> keysArr.getInt(i) }.filter { it != 0 }
        } else {
            // 이전 포맷 호환: "key" 단일 필드
            val legacy = obj.optInt("key", 0)
            if (legacy != 0) listOf(legacy) else emptyList()
        }
        EdgeZoneAction.SendShortcut(
            modifierBits = obj.optInt("mod", 0),
            keyCodes     = keyCodes,
            hold         = obj.optBoolean("hold", false),
            presetLabel  = obj.optString("preset", "")
        )
    }
    "SendMacro"       -> {
        val stepsArr = obj.optJSONArray("steps") ?: JSONArray()
        val steps = List(stepsArr.length()) { i ->
            val stepObj = stepsArr.getJSONObject(i)
            val keysArr = stepObj.getJSONArray("keys")
            MacroStep(
                modifierBits = stepObj.optInt("mod", 0),
                keyCodes = List(keysArr.length()) { j -> keysArr.getInt(j) }.filter { it != 0 },
                delayAfterMs = if (stepObj.has("d")) stepObj.getInt("d") else null,
                repeatCount = stepObj.optInt("r", 1),
                groupId = if (stepObj.has("g")) stepObj.getInt("g") else null,
                kind = stepObj.optString("k", "").takeIf { it.isNotEmpty() }?.let { runCatching { MacroStepKind.valueOf(it) }.getOrNull() } ?: MacroStepKind.TAP,
            )
        }
        EdgeZoneAction.SendMacro(
            steps = steps,
            stepDelayMs = obj.optInt("delay", MACRO_STEP_DELAY_DEFAULT_MS),
            presetLabel = obj.optString("preset", ""),
            inputModeCheck = when {
                obj.has("imeCheck") -> runCatching { InputModeCheck.valueOf(obj.getString("imeCheck")) }.getOrDefault(InputModeCheck.NONE)
                obj.optBoolean("korean", false) -> InputModeCheck.KOREAN
                else -> InputModeCheck.NONE
            },
            groupNames = obj.optJSONObject("groups")?.let { gn ->
                gn.keys().asSequence().associate { it.toInt() to gn.getString(it) }
            } ?: emptyMap(),
        )
    }
    "MouseHoldToggle" -> EdgeZoneAction.MouseHoldToggle(
        button = runCatching { MouseButton.valueOf(obj.getString("button")) }.getOrDefault(MouseButton.LEFT),
        mode = runCatching { MouseHoldMode.valueOf(obj.getString("holdMode")) }.getOrDefault(MouseHoldMode.TOGGLE)
    )
    "CyclePage"       -> EdgeZoneAction.CyclePage(
        direction = runCatching { PageNav.valueOf(obj.getString("dir")) }.getOrDefault(PageNav.NEXT)
    )
    "JumpToPage"      -> EdgeZoneAction.JumpToPage(
        pageIndex = obj.optInt("page", 0).coerceAtLeast(0)
    )
    "ToggleMultiCursor"       -> EdgeZoneAction.ToggleMultiCursor
    "ToggleMultiCursorLayout" -> EdgeZoneAction.ToggleMultiCursorLayout
    "SetCursorCount"  -> EdgeZoneAction.SetCursorCount(
        obj.optInt("count", MULTI_CURSOR_COUNT_MIN).coerceIn(MULTI_CURSOR_COUNT_MIN, MULTI_CURSOR_COUNT_MAX)
    )
    "ActivatePad"     -> EdgeZoneAction.ActivatePad(
        obj.optInt("index", 0).coerceIn(0, MULTI_CURSOR_COUNT_MAX - 1)
    )
    "CyclePad"        -> EdgeZoneAction.CyclePad(
        direction = runCatching { PageNav.valueOf(obj.getString("dir")) }.getOrDefault(PageNav.NEXT)
    )
    else              -> EdgeZoneAction.Unassigned
}
