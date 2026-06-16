package com.bridgeone.app.ui.common

import android.content.Context
import com.bridgeone.app.ui.components.touchpad.InputModeCheck
import com.bridgeone.app.ui.components.touchpad.MacroStep
import com.bridgeone.app.ui.components.touchpad.MacroStepKind
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class CustomMacroPreset(
    val id: String = "",
    val steps: List<MacroStep>,
    val stepDelayMs: Int = MACRO_STEP_DELAY_DEFAULT_MS,
    val iconKey: String = "",
    val inputModeCheck: InputModeCheck = InputModeCheck.NONE,
    val displayName: String = "",
    val groupNames: Map<Int, String> = emptyMap(),  // 기본값: emptyMap()
)

/**
 * 커스텀 매크로 프리셋을 앱 내부 저장소에 JSON으로 저장/불러오기/관리.
 *
 * 저장 경로: {filesDir}/macro_presets.json
 */
class CustomMacroPresetsRepository(context: Context) {

    private val file = File(context.filesDir, "macro_presets.json")

    fun loadAll(): List<CustomMacroPreset> {
        if (!file.exists()) return defaultPresets()
        return try {
            val arr = JSONArray(file.readText())
            (0 until arr.length()).map { parse(arr.getJSONObject(it)) }
        } catch (_: Exception) { emptyList() }
    }

    companion object {
        private fun step(modBits: Int, vararg keys: Int) = MacroStep(modBits, keys.toList())

        fun defaultPresets(): List<CustomMacroPreset> = listOf(
            // "안녕하세요!" — 두벌식: ㅇ(d) ㅏ(k) ㄴ(s) / ㄴ(s) ㅕ(u) ㅇ(d) / ㅎ(g) ㅏ(k) / ㅅ(t) ㅔ(p) / ㅇ(d) ㅛ(y) / Shift+1
            CustomMacroPreset(
                id = "default_hello",
                steps = listOf(
                    step(0,    0x07), step(0, 0x0E), step(0, 0x16),  // 안
                    step(0,    0x16), step(0, 0x18), step(0, 0x07),  // 녕
                    step(0,    0x0A), step(0, 0x0E),                  // 하
                    step(0,    0x17), step(0, 0x13),                  // 세
                    step(0,    0x07), step(0, 0x1C),                  // 요
                    step(0x02, 0x1E),                                  // !
                ),
                iconKey = "Chat",
                inputModeCheck = InputModeCheck.KOREAN,
                displayName = "안녕하세요!",
            ),
            // "감사합니다 ㅎㅎ" — 두벌식: ㄱ(r) ㅏ(k) ㅁ(a) / ㅅ(t) ㅏ(k) / ㅎ(g) ㅏ(k) ㅂ(q) / ㄴ(s) ㅣ(l) / ㄷ(e) ㅏ(k) / space / ㅎ(g) ㅎ(g)
            CustomMacroPreset(
                id = "default_thanks",
                steps = listOf(
                    step(0, 0x15), step(0, 0x0E), step(0, 0x04),  // 감
                    step(0, 0x17), step(0, 0x0E),                   // 사
                    step(0, 0x0A), step(0, 0x0E), step(0, 0x14),  // 합
                    step(0, 0x16), step(0, 0x0F),                   // 니
                    step(0, 0x08), step(0, 0x0E),                   // 다
                    step(0, 0x2C),                                   // space
                    step(0, 0x0A), step(0, 0x0A),                   // ㅎㅎ
                ),
                iconKey = "Favorite",
                inputModeCheck = InputModeCheck.KOREAN,
                displayName = "감사합니다 ㅎㅎ",
            ),
            // 전체 선택 후 잘라내기: Ctrl+A → Ctrl+X
            CustomMacroPreset(
                id = "default_cut_all",
                steps = listOf(step(0x01, 0x04), step(0x01, 0x1B)),
                iconKey = "ContentCut",
                displayName = "전체 선택 후 잘라내기",
            ),
            // 전체 선택 후 복사: Ctrl+A → Ctrl+C
            CustomMacroPreset(
                id = "default_copy_all",
                steps = listOf(step(0x01, 0x04), step(0x01, 0x06)),
                iconKey = "ContentCopy",
                displayName = "전체 선택 후 복사",
            ),
            // 실행 취소 × 3: Ctrl+Z → Ctrl+Z → Ctrl+Z
            CustomMacroPreset(
                id = "default_undo3",
                steps = listOf(step(0x01, 0x1D), step(0x01, 0x1D), step(0x01, 0x1D)),
                iconKey = "Undo",
                displayName = "실행 취소 ×3",
            ),
            // 화면 영역 캡처: Win+Shift+S
            CustomMacroPreset(
                id = "default_snip",
                steps = listOf(step(0x0A, 0x16)),
                iconKey = "Image",
                displayName = "화면 영역 캡처",
            ),
        )
    }

    fun saveAll(presets: List<CustomMacroPreset>) {
        val arr = JSONArray()
        presets.forEach { arr.put(serialize(it)) }
        file.writeText(arr.toString())
    }

    fun add(preset: CustomMacroPreset): CustomMacroPreset {
        val withId = if (preset.id.isBlank()) preset.copy(id = UUID.randomUUID().toString()) else preset
        saveAll(loadAll() + withId)
        return withId
    }

    fun update(preset: CustomMacroPreset) {
        saveAll(loadAll().map { if (it.id == preset.id) preset else it })
    }

    fun delete(id: String) {
        saveAll(loadAll().filter { it.id != id })
    }

    private fun serializeStep(step: MacroStep): JSONObject = JSONObject().apply {
        put("mod", step.modifierBits)
        put("keys", JSONArray(step.keyCodes))
        step.delayAfterMs?.let { put("d", it) }
        if (step.repeatCount > 1) put("r", step.repeatCount)
        step.groupId?.let { put("g", it) }
        if (step.kind != MacroStepKind.TAP) put("k", step.kind.name)
    }

    private fun parseStep(obj: JSONObject): MacroStep {
        val keysArr = obj.getJSONArray("keys")
        return MacroStep(
            modifierBits = obj.optInt("mod", 0),
            keyCodes = List(keysArr.length()) { i -> keysArr.getInt(i) },
            delayAfterMs = if (obj.has("d")) obj.getInt("d") else null,
            repeatCount = obj.optInt("r", 1),
            groupId = if (obj.has("g")) obj.getInt("g") else null,
            kind = obj.optString("k", "").takeIf { it.isNotEmpty() }?.let { runCatching { MacroStepKind.valueOf(it) }.getOrNull() } ?: MacroStepKind.TAP,
        )
    }

    private fun serialize(p: CustomMacroPreset): JSONObject = JSONObject().apply {
        put("id", p.id)
        val stepsArr = JSONArray()
        p.steps.forEach { stepsArr.put(serializeStep(it)) }
        put("steps", stepsArr)
        put("delay", p.stepDelayMs)
        if (p.iconKey.isNotEmpty()) put("icon", p.iconKey)
        if (p.inputModeCheck != InputModeCheck.NONE) put("imeCheck", p.inputModeCheck.name)
        if (p.displayName.isNotEmpty()) put("name", p.displayName)
        if (p.groupNames.isNotEmpty()) {
            val gn = JSONObject()
            p.groupNames.forEach { (k, v) -> gn.put(k.toString(), v) }
            put("groups", gn)
        }
    }

    private fun parse(obj: JSONObject): CustomMacroPreset {
        val stepsArr = obj.getJSONArray("steps")
        return CustomMacroPreset(
            id = obj.getString("id"),
            steps = List(stepsArr.length()) { i -> parseStep(stepsArr.getJSONObject(i)) },
            stepDelayMs = obj.optInt("delay", MACRO_STEP_DELAY_DEFAULT_MS),
            iconKey = obj.optString("icon", ""),
            inputModeCheck = when {
                obj.has("imeCheck") -> runCatching { InputModeCheck.valueOf(obj.getString("imeCheck")) }.getOrDefault(InputModeCheck.NONE)
                obj.optBoolean("korean", false) -> InputModeCheck.KOREAN
                else -> InputModeCheck.NONE
            },
            displayName = obj.optString("name", ""),
            groupNames = obj.optJSONObject("groups")?.let { gn ->
                gn.keys().asSequence().associate { it.toInt() to gn.getString(it) }
            } ?: emptyMap(),
        )
    }
}
