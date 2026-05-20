package com.bridgeone.app.ui.common

import android.content.Context
import com.bridgeone.app.ui.components.touchpad.ControlButtonConfig
import org.json.JSONObject
import java.io.File

/**
 * 터치패드별 버튼 표시 설정(TouchpadButtonVisibility)을 앱 내부 저장소에 JSON으로 영속화.
 *
 * 저장 경로: {filesDir}/touchpad_button_visibility.json
 * 포맷: { "<touchpadId>": { "showControlButtons": ..., "showClickMode": ..., ... }, ... }
 */
class TouchpadButtonVisibilityRepository(context: Context) {

    private val file = File(context.filesDir, "touchpad_button_visibility.json")

    fun load(id: String): TouchpadButtonVisibility {
        val root = readRoot() ?: return TouchpadButtonVisibility.defaultFor(id)
        if (!root.has(id)) return TouchpadButtonVisibility.defaultFor(id)
        return try {
            val obj = root.getJSONObject(id)
            TouchpadButtonVisibility(
                showControlButtons = obj.optBoolean("showControlButtons", true),
                controlButtonConfig = ControlButtonConfig(
                    showClickMode = obj.optBoolean("showClickMode", true),
                    showMoveMode = obj.optBoolean("showMoveMode", true),
                    showScrollMode = obj.optBoolean("showScrollMode", true),
                    showCursorMode = obj.optBoolean("showCursorMode", false),
                    showDpi = obj.optBoolean("showDpi", true),
                    showScrollSensitivity = obj.optBoolean("showScrollSensitivity", true),
                ),
                showDynamicsButton = obj.optBoolean("showDynamicsButton", true),
                showModePresetButton = obj.optBoolean("showModePresetButton", true),
                showScrollButtons = obj.optBoolean("showScrollButtons", true),
            )
        } catch (_: Exception) {
            TouchpadButtonVisibility.defaultFor(id)
        }
    }

    fun save(id: String, visibility: TouchpadButtonVisibility) {
        val root = readRoot() ?: JSONObject()
        val obj = JSONObject()
        obj.put("showControlButtons", visibility.showControlButtons)
        obj.put("showClickMode", visibility.controlButtonConfig.showClickMode)
        obj.put("showMoveMode", visibility.controlButtonConfig.showMoveMode)
        obj.put("showScrollMode", visibility.controlButtonConfig.showScrollMode)
        obj.put("showCursorMode", visibility.controlButtonConfig.showCursorMode)
        obj.put("showDpi", visibility.controlButtonConfig.showDpi)
        obj.put("showScrollSensitivity", visibility.controlButtonConfig.showScrollSensitivity)
        obj.put("showDynamicsButton", visibility.showDynamicsButton)
        obj.put("showModePresetButton", visibility.showModePresetButton)
        obj.put("showScrollButtons", visibility.showScrollButtons)
        root.put(id, obj)
        file.writeText(root.toString())
    }

    private fun readRoot(): JSONObject? {
        if (!file.exists()) return null
        return try { JSONObject(file.readText()) } catch (_: Exception) { null }
    }
}
