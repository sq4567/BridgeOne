package com.bridgeone.app.ui.pages.standard

import android.content.Context
import com.bridgeone.app.ui.common.EdgeSwipeConstants
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

// ============================================================
// 코너 버튼 차단 영역 크기 SharedPreferences 저장/복원 (전역)
// ============================================================

internal const val KEY_CORNER_BLOCKED_RATIO = "corner_blocked_ratio"

/** 코너 버튼(다이나믹스/모드프리셋)이 차지하는 엣지 끝 차단 비율. 기본값: CORNER_BUTTON_BLOCKED_RATIO(0.15). */
internal fun loadCornerBlockedRatio(context: Context): Float =
    context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        .getFloat(KEY_CORNER_BLOCKED_RATIO, EdgeSwipeConstants.CORNER_BUTTON_BLOCKED_RATIO)

internal fun saveCornerBlockedRatio(context: Context, ratio: Float) {
    context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        .edit()
        .putFloat(KEY_CORNER_BLOCKED_RATIO, ratio.coerceIn(0.05f, 0.30f))
        .apply()
}
