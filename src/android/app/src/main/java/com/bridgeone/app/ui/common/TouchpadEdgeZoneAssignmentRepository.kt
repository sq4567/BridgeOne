package com.bridgeone.app.ui.common

import android.content.Context
import org.json.JSONObject
import java.io.File

/**
 * 터치패드별 엣지 존 할당(TouchpadEdgeZoneAssignment)을 앱 내부 저장소에 JSON으로 영속화.
 *
 * 저장 경로: {filesDir}/touchpad_edge_zone_assignments.json
 * 포맷: { "<touchpadId>": { "presetId": "...", "config": { ... } }, ... }
 */
class TouchpadEdgeZoneAssignmentRepository(context: Context) {

    private val file = File(context.filesDir, "touchpad_edge_zone_assignments.json")

    fun load(id: String): TouchpadEdgeZoneAssignment {
        val root = readRoot() ?: return TouchpadEdgeZoneAssignment.default()
        if (!root.has(id)) return TouchpadEdgeZoneAssignment.default()
        return try {
            val obj = root.getJSONObject(id)
            val presetId = if (obj.isNull("presetId")) null else obj.getString("presetId")
            val config = edgeZoneConfigFromJsonObject(obj.getJSONObject("config"))
            TouchpadEdgeZoneAssignment(config, presetId)
        } catch (_: Exception) {
            TouchpadEdgeZoneAssignment.default()
        }
    }

    fun save(id: String, assignment: TouchpadEdgeZoneAssignment) {
        val root = readRoot() ?: JSONObject()
        val obj = JSONObject()
        if (assignment.presetId != null) obj.put("presetId", assignment.presetId) else obj.put("presetId", JSONObject.NULL)
        obj.put("config", edgeZoneConfigToJsonObject(assignment.config))
        root.put(id, obj)
        file.writeText(root.toString())
    }

    /**
     * 구 SharedPreferences 키(edge_zone_config, edge_zone_preset_id)가 있으면
     * standard_page_0으로 이전 후 구 키를 제거한다. 신규 파일이 이미 있으면 스킵.
     */
    fun migrateLegacyIfNeeded(context: Context) {
        if (file.exists()) return
        val prefs = context.getSharedPreferences("touchpad_prefs", Context.MODE_PRIVATE)
        val legacyJson = prefs.getString("edge_zone_config", null) ?: return
        val legacyPresetId = prefs.getString("edge_zone_preset_id", "builtin_default")
        try {
            val config = edgeZoneConfigFromJson(legacyJson)
            save(TouchpadIds.standardPage(0), TouchpadEdgeZoneAssignment(config, legacyPresetId))
            prefs.edit()
                .remove("edge_zone_config")
                .remove("edge_zone_preset_id")
                .apply()
        } catch (_: Exception) {
            // 마이그레이션 실패 시 기본값 사용, 구 키는 그대로 방치
        }
    }

    /**
     * 구 "standard_primary" 키를 "standard_page_0"으로 이전.
     * TouchpadIds.STANDARD_PRIMARY → standardPage(0) 변경 이후 한 번만 실행됨.
     */
    fun migrateStandardPrimaryKeyIfNeeded() {
        val root = readRoot() ?: return
        val oldKey = "standard_primary"
        val newKey = TouchpadIds.standardPage(0)
        if (!root.has(oldKey) || root.has(newKey)) return
        root.put(newKey, root.getJSONObject(oldKey))
        root.remove(oldKey)
        file.writeText(root.toString())
    }

    private fun readRoot(): JSONObject? {
        if (!file.exists()) return null
        return try { JSONObject(file.readText()) } catch (_: Exception) { null }
    }
}
