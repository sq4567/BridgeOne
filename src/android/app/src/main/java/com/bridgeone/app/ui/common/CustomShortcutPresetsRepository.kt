package com.bridgeone.app.ui.common

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class CustomShortcutPreset(
    val id: String = "",
    val modifierBits: Int,
    val keyCodes: List<Int>,
    val hold: Boolean = false,
    val iconKey: String = "",
    val name: String = "",
)

/**
 * 커스텀 단축키 프리셋을 앱 내부 저장소에 JSON으로 저장/불러오기/관리
 *
 * 저장 경로: {filesDir}/shortcut_presets.json
 */
class CustomShortcutPresetsRepository(context: Context) {

    private val file = File(context.filesDir, "shortcut_presets.json")

    fun loadAll(): List<CustomShortcutPreset> {
        if (!file.exists()) return emptyList()
        return try {
            val arr = JSONArray(file.readText())
            val all = (0 until arr.length()).map { parse(arr.getJSONObject(it)) }
            val valid = all.filter { it.modifierBits != 0 || it.keyCodes.isNotEmpty() }
            if (valid.size != all.size) saveAll(valid)
            valid
        } catch (_: Exception) { emptyList() }
    }

    fun saveAll(presets: List<CustomShortcutPreset>) {
        val arr = JSONArray()
        presets.forEach { arr.put(serialize(it)) }
        file.writeText(arr.toString())
    }

    fun add(preset: CustomShortcutPreset): CustomShortcutPreset? {
        if (preset.modifierBits == 0 && preset.keyCodes.isEmpty()) return null
        val withId = if (preset.id.isBlank()) preset.copy(id = UUID.randomUUID().toString()) else preset
        saveAll(loadAll() + withId)
        return withId
    }

    fun update(preset: CustomShortcutPreset) {
        saveAll(loadAll().map { if (it.id == preset.id) preset else it })
    }

    fun delete(id: String) {
        saveAll(loadAll().filter { it.id != id })
    }

    private fun serialize(p: CustomShortcutPreset): JSONObject = JSONObject().apply {
        put("id", p.id)
        put("mod", p.modifierBits)
        put("keys", JSONArray(p.keyCodes))
        put("hold", p.hold)
        if (p.iconKey.isNotEmpty()) put("icon", p.iconKey)
        if (p.name.isNotEmpty()) put("name", p.name)
    }

    private fun parse(obj: JSONObject): CustomShortcutPreset {
        val keysArr = obj.getJSONArray("keys")
        return CustomShortcutPreset(
            id = obj.getString("id"),
            modifierBits = obj.optInt("mod", 0),
            keyCodes = List(keysArr.length()) { i -> keysArr.getInt(i) },
            hold = obj.optBoolean("hold", false),
            iconKey = obj.optString("icon", ""),
            name = obj.optString("name", ""),
        )
    }
}
