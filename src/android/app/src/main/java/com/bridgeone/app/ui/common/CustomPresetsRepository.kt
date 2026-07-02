package com.bridgeone.app.ui.common

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * 커스텀 포인터 다이나믹스 프리셋을 앱 내부 저장소에 JSON으로 저장/불러오기/관리
 *
 * 저장 경로: {filesDir}/dynamics_presets.json
 */
class CustomPresetsRepository(context: Context) {

    private val file = File(context.filesDir, "dynamics_presets.json")

    fun loadAll(): List<CustomPointerDynamicsPreset> {
        if (!file.exists()) {
            // 최초 실행: 커스텀 프리셋 없음 (균형/정밀 우선/빠른 이동/손 떨림 방지는
            // 템플릿 선택 화면에서만 제공, 자동 주입하지 않음)
            return emptyList()
        }
        val loaded = try {
            val arr = JSONArray(file.readText())
            (0 until arr.length()).map { parsePreset(arr.getJSONObject(it)) }
        } catch (_: Exception) {
            emptyList()
        }

        // 마이그레이션: 과거 버전에서 자동 주입되어 저장된 템플릿 프리셋 정리
        val templateIds = CUSTOM_PRESET_TEMPLATES.map { it.id }.toSet()
        val migrated = loaded.filterNot { it.id in templateIds }
        if (migrated.size != loaded.size) saveAll(migrated)
        return migrated
    }

    fun saveAll(presets: List<CustomPointerDynamicsPreset>) {
        val arr = JSONArray()
        presets.forEach { arr.put(serializePreset(it)) }
        file.writeText(arr.toString())
    }

    fun add(preset: CustomPointerDynamicsPreset): CustomPointerDynamicsPreset {
        val withId = if (preset.id.isBlank()) preset.copy(id = UUID.randomUUID().toString()) else preset
        saveAll(loadAll() + withId)
        return withId
    }

    fun update(preset: CustomPointerDynamicsPreset) {
        saveAll(loadAll().map { if (it.id == preset.id) preset else it })
    }

    fun delete(id: String) {
        saveAll(loadAll().filter { it.id != id })
    }

    // ──────────────────────────────────────────
    // JSON 직렬화 / 역직렬화
    // ──────────────────────────────────────────

    private fun serializePreset(p: CustomPointerDynamicsPreset): JSONObject = JSONObject().apply {
        put("id", p.id)
        put("name", p.name)
        put("accel", serializeCurve(p.accelerationCurve))
        put("decel", serializeCurve(p.decelerationCurve))
        if (p.description.isNotEmpty()) put("description", p.description)
        if (p.iconKey.isNotEmpty()) put("iconKey", p.iconKey)
        if (p.colorHex.isNotEmpty()) put("colorHex", p.colorHex)
    }

    private fun serializeCurve(curve: List<CurveNode>): JSONArray {
        val arr = JSONArray()
        curve.forEach { node ->
            arr.put(JSONObject().apply {
                put("v", node.velocityDpMs.toDouble())
                put("m", node.multiplier.toDouble())
            })
        }
        return arr
    }

    private fun parsePreset(obj: JSONObject): CustomPointerDynamicsPreset = CustomPointerDynamicsPreset(
        id = obj.getString("id"),
        name = obj.getString("name"),
        accelerationCurve = parseCurve(obj.getJSONArray("accel")),
        decelerationCurve = parseCurve(obj.getJSONArray("decel")),
        description = obj.optString("description", ""),
        iconKey = obj.optString("iconKey", ""),
        colorHex = obj.optString("colorHex", "")
    )

    private fun parseCurve(arr: JSONArray): List<CurveNode> =
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            CurveNode(o.getDouble("v").toFloat(), o.getDouble("m").toFloat())
        }
}
