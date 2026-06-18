package com.bridgeone.app.ui.pages.standard

import android.content.Context
import com.bridgeone.app.ui.components.touchpad.DpiLevel
import com.bridgeone.app.ui.components.touchpad.EdgeInteractionMode

// ============================================================
// DPI 레벨 SharedPreferences 저장/복원 (Phase 4.3.6)
// ============================================================

internal const val PREF_NAME = "touchpad_prefs"
internal const val KEY_DPI_LEVEL = "dpi_level"

internal fun loadDpiLevel(context: Context): DpiLevel {
    val name = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        .getString(KEY_DPI_LEVEL, DpiLevel.NORMAL.name) ?: DpiLevel.NORMAL.name
    return DpiLevel.entries.firstOrNull { it.name == name } ?: DpiLevel.NORMAL
}

internal fun saveDpiLevel(context: Context, level: DpiLevel) {
    context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_DPI_LEVEL, level.name)
        .apply()
}

// ============================================================
// 엣지 조작 방식 SharedPreferences 저장/복원 (Phase 4.6.1)
// ============================================================

internal const val KEY_EDGE_INTERACTION_MODE = "edge_interaction_mode"

internal fun loadEdgeInteractionMode(context: Context): EdgeInteractionMode {
    val name = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        .getString(KEY_EDGE_INTERACTION_MODE, EdgeInteractionMode.LEGACY_POPUP.name)
        ?: EdgeInteractionMode.LEGACY_POPUP.name
    return EdgeInteractionMode.entries.firstOrNull { it.name == name } ?: EdgeInteractionMode.LEGACY_POPUP
}

internal fun saveEdgeInteractionMode(context: Context, mode: EdgeInteractionMode) {
    context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_EDGE_INTERACTION_MODE, mode.name)
        .apply()
}
