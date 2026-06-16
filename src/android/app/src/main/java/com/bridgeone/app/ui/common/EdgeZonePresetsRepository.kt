package com.bridgeone.app.ui.common

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * 커스텀 엣지 존 프리셋을 앱 내부 저장소에 JSON으로 저장/불러오기/관리 (Phase 4.6.3)
 *
 * 저장 경로: {filesDir}/edge_zone_presets.json
 * 빌트인 프리셋(BUILT_IN_EDGE_ZONE_PRESETS)은 저장하지 않고 상수에서 로드.
 */
class EdgeZonePresetsRepository(context: Context) {

    private val file = File(context.filesDir, "edge_zone_presets.json")

    /** 빌트인 + 커스텀 전체 반환 */
    fun loadAll(): List<EdgeZonePreset> = BUILT_IN_EDGE_ZONE_PRESETS + loadCustom()

    /** 커스텀 프리셋만 반환 */
    fun loadCustom(): List<EdgeZonePreset> {
        if (!file.exists()) return emptyList()
        return try {
            val arr = JSONArray(file.readText())
            (0 until arr.length()).map { parsePreset(arr.getJSONObject(it)) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** 커스텀 프리셋 추가 (id 미지정 시 UUID 자동 할당) */
    fun add(preset: EdgeZonePreset): EdgeZonePreset {
        val withId = if (preset.id.isBlank()) preset.copy(id = UUID.randomUUID().toString()) else preset
        saveCustom(loadCustom() + withId)
        return withId
    }

    /** 커스텀 프리셋 업데이트 (이름 변경 등) */
    fun update(preset: EdgeZonePreset) {
        saveCustom(loadCustom().map { if (it.id == preset.id) preset else it })
    }

    /** 커스텀 프리셋 삭제 */
    fun delete(id: String) {
        saveCustom(loadCustom().filter { it.id != id })
    }

    /** ID로 프리셋 조회 (빌트인 + 커스텀 모두 검색) */
    fun findById(id: String?): EdgeZonePreset? =
        if (id == null) null else loadAll().find { it.id == id }

    /** 커스텀 프리셋 전체 저장 */
    private fun saveCustom(presets: List<EdgeZonePreset>) {
        val arr = JSONArray()
        presets.forEach { arr.put(serializePreset(it)) }
        file.writeText(arr.toString())
    }

    private fun serializePreset(p: EdgeZonePreset): JSONObject = JSONObject().apply {
        put("id",          p.id)
        put("name",        p.name)
        put("description", p.description)
        put("iconKey",     p.iconKey)
        put("config",      edgeZoneConfigToJsonObject(p.config))
    }

    private fun parsePreset(obj: JSONObject): EdgeZonePreset = EdgeZonePreset(
        id          = obj.getString("id"),
        name        = obj.getString("name"),
        description = obj.optString("description", ""),
        iconKey     = obj.optString("iconKey", ""),
        config      = edgeZoneConfigFromJsonObject(obj.getJSONObject("config"))
    )
}
