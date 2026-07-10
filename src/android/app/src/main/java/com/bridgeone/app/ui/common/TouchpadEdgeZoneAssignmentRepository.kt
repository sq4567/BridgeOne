package com.bridgeone.app.ui.common

import android.content.Context
import com.bridgeone.app.ui.components.touchpad.EdgeZone
import com.bridgeone.app.ui.components.touchpad.EdgeZoneAction
import com.bridgeone.app.ui.components.touchpad.EdgeZoneConfig
import com.bridgeone.app.ui.components.touchpad.EdgeZoneTrigger
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

    /**
     * PAGE_COUNT 5→6 확장(Phase 4.9.1)으로 논리 인덱스 2에 신규 페이지가 삽입되며,
     * 기존에 저장된 pageIndex>=2 JumpToPage 값이 한 칸씩 밀린 엉뚱한 페이지를 가리키게 된다.
     * 저장된 모든 터치패드의 존 설정을 순회하며 pageIndex>=2인 JumpToPage를 +1 이동한다.
     * SharedPreferences 플래그로 중복 실행을 막는다.
     */
    fun migrateJumpToPageIndicesIfNeeded(context: Context) {
        val prefs = context.getSharedPreferences("touchpad_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("page_index_migrated_v1", false)) return
        val root = readRoot()
        if (root != null) {
            root.keys().asSequence().toList().forEach { id ->
                val assignment = load(id)
                val migratedConfig = migrateJumpToPageInConfig(assignment.config)
                if (migratedConfig != assignment.config) {
                    save(id, assignment.copy(config = migratedConfig))
                }
            }
        }
        prefs.edit().putBoolean("page_index_migrated_v1", true).apply()
    }

    private fun migrateJumpToPageInConfig(config: EdgeZoneConfig): EdgeZoneConfig {
        fun migrateAction(action: EdgeZoneAction): EdgeZoneAction =
            if (action is EdgeZoneAction.JumpToPage && action.pageIndex >= 2)
                action.copy(pageIndex = action.pageIndex + 1)
            else action

        fun migrateTrigger(trigger: EdgeZoneTrigger): EdgeZoneTrigger = when (trigger) {
            is EdgeZoneTrigger.SingleAction -> trigger.copy(action = migrateAction(trigger.action))
            is EdgeZoneTrigger.Rotation -> trigger.copy(
                candidates = trigger.candidates.map { it.copy(action = migrateAction(it.action)) }
            )
        }

        fun migrateZones(zones: List<EdgeZone>): List<EdgeZone> =
            zones.map { it.copy(trigger = migrateTrigger(it.trigger)) }

        return config.copy(
            topZones = migrateZones(config.topZones),
            bottomZones = migrateZones(config.bottomZones),
            leftZones = migrateZones(config.leftZones),
            rightZones = migrateZones(config.rightZones)
        )
    }

    private fun readRoot(): JSONObject? {
        if (!file.exists()) return null
        return try { JSONObject(file.readText()) } catch (_: Exception) { null }
    }
}
