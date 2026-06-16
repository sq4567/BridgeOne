package com.bridgeone.app.ui.common

import android.content.Context

/**
 * 앱 전체 UI 조작 방식.
 *
 * 두 방식은 런타임에 배타적이다 (동시 활성 불가).
 * - [NORMAL]: 직접 터치/드래그 (요소 위를 절대 좌표로 직접 조작)
 * - [SWIPE]: 화면 어디서나 스와이프로 포커스 이동, 어디서나 터치로 선택 요소 조작 (상대 좌표)
 *
 * 환경 설정에서 변경하며, SharedPreferences에 영속화.
 */
enum class InputMode {
    NORMAL,
    SWIPE,
}

private const val INPUT_MODE_PREF_NAME = "input_mode_prefs"
private const val INPUT_MODE_KEY = "input_mode"
private const val SWIPE_WRAP_EDGE_KEY = "swipe_wrap_edge"

/** 저장된 [InputMode]를 로드. 없으면 [InputMode.NORMAL] 반환. 기본값: NORMAL */
fun loadInputMode(context: Context): InputMode {
    val name = context.getSharedPreferences(INPUT_MODE_PREF_NAME, Context.MODE_PRIVATE)
        .getString(INPUT_MODE_KEY, InputMode.NORMAL.name) ?: InputMode.NORMAL.name
    return InputMode.entries.firstOrNull { it.name == name } ?: InputMode.NORMAL
}

/** [InputMode]를 SharedPreferences에 저장. */
fun saveInputMode(context: Context, mode: InputMode) {
    context.getSharedPreferences(INPUT_MODE_PREF_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(INPUT_MODE_KEY, mode.name)
        .apply()
}

/** 스와이프 끝점 wrap 여부를 로드. 기본값: false */
fun loadSwipeWrapEdge(context: Context): Boolean =
    context.getSharedPreferences(INPUT_MODE_PREF_NAME, Context.MODE_PRIVATE)
        .getBoolean(SWIPE_WRAP_EDGE_KEY, false)  // 기본값: false

/** 스와이프 끝점 wrap 여부를 SharedPreferences에 저장. */
fun saveSwipeWrapEdge(context: Context, enabled: Boolean) {
    context.getSharedPreferences(INPUT_MODE_PREF_NAME, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(SWIPE_WRAP_EDGE_KEY, enabled)
        .apply()
}
