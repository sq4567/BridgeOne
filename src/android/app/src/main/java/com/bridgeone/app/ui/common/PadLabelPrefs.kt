package com.bridgeone.app.ui.common

import android.content.Context
import com.bridgeone.app.ui.components.touchpad.MULTI_CURSOR_COUNT_MAX
import org.json.JSONArray

/**
 * 멀티 커서 패드 커스텀 라벨 영속화 (Phase 4.8.10).
 *
 * 라벨은 커서 수·활성 상태와 무관하게 항상 4칸(pad1~pad4) 고정 크기로 저장된다.
 */

private const val PAD_LABEL_PREF_NAME = "pad_label_prefs"
private const val PAD_LABEL_KEY = "pad_labels"

/** 저장된 패드 라벨을 로드. 없거나 파싱 실패 시 전부 null(번호 폴백). */
fun loadPadLabels(context: Context): List<String?> {
    val json = context.getSharedPreferences(PAD_LABEL_PREF_NAME, Context.MODE_PRIVATE)
        .getString(PAD_LABEL_KEY, null) ?: return List(MULTI_CURSOR_COUNT_MAX) { null }
    return try {
        val arr = JSONArray(json)
        List(MULTI_CURSOR_COUNT_MAX) { i -> arr.optString(i, "").ifBlank { null } }
    } catch (e: Exception) {
        List(MULTI_CURSOR_COUNT_MAX) { null }
    }
}

/** 패드 라벨을 SharedPreferences에 저장. */
fun savePadLabels(context: Context, labels: List<String?>) {
    val arr = JSONArray()
    labels.forEach { arr.put(it ?: "") }
    context.getSharedPreferences(PAD_LABEL_PREF_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(PAD_LABEL_KEY, arr.toString())
        .apply()
}
