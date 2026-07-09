package com.bridgeone.app.ui.common

import android.content.Context

/**
 * Page 3 절대좌표 모니터 셀렉터 선택값 영속화 (Phase 4.9.5).
 *
 * 저장값은 targetMonitor 바이트와 동일한 규약의 Int(0x00=전체, 0x01~N=특정 모니터).
 */

private const val MONITOR_SELECTOR_PREF_NAME = "monitor_selector_prefs"
private const val TARGET_MONITOR_KEY = "target_monitor"

/** 저장된 targetMonitor 선택값을 로드. 저장값 없으면 -1(폴백 판단은 호출측이 수행). */
fun loadTargetMonitor(context: Context): Int {
    return context.getSharedPreferences(MONITOR_SELECTOR_PREF_NAME, Context.MODE_PRIVATE)
        .getInt(TARGET_MONITOR_KEY, -1)
}

/** targetMonitor 선택값을 SharedPreferences에 저장. */
fun saveTargetMonitor(context: Context, value: Int) {
    context.getSharedPreferences(MONITOR_SELECTOR_PREF_NAME, Context.MODE_PRIVATE)
        .edit()
        .putInt(TARGET_MONITOR_KEY, value)
        .apply()
}
