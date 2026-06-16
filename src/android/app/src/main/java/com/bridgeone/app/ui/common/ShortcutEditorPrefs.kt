package com.bridgeone.app.ui.common

import android.content.Context

private const val SHORTCUT_EDITOR_PREF_NAME = "shortcut_editor_prefs"
private const val LAST_SINGLE_KEY_MODE_KEY = "last_single_key_mode"

/** 마지막으로 할당/추가에 사용한 단일 키 모드 여부 로드. 기본값: false (단축키 모드) */
fun loadLastShortcutSingleKeyMode(context: Context): Boolean =
    context.getSharedPreferences(SHORTCUT_EDITOR_PREF_NAME, Context.MODE_PRIVATE)
        .getBoolean(LAST_SINGLE_KEY_MODE_KEY, false)  // 기본값: false (단축키 모드)

/** 단일 키 모드 여부를 SharedPreferences에 저장. */
fun saveLastShortcutSingleKeyMode(context: Context, singleKey: Boolean) {
    context.getSharedPreferences(SHORTCUT_EDITOR_PREF_NAME, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(LAST_SINGLE_KEY_MODE_KEY, singleKey)
        .apply()
}
