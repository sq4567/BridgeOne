package com.bridgeone.app.ui.common

import android.content.Context
import androidx.compose.runtime.compositionLocalOf

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

/**
 * 현재 [InputMode]를 컴포지션 트리에 제공하는 CompositionLocal (Phase 4.7.5-D).
 *
 * 화면 진입 시 1회 provide되며 런타임에 변경되지 않는다(`loadInputMode`로 로드된 `val`).
 * 하위 Composable이 `inputMode` 파라미터를 일일이 받지 않고 `LocalInputMode.current`로 읽어
 * SWIPE/NORMAL 분기를 내부에 가둘 수 있게 한다.
 *
 * 기본값: NORMAL (provider 누락 시 안전 폴백 — `loadInputMode`의 기본값과 일치)
 */
val LocalInputMode = compositionLocalOf { InputMode.NORMAL }

/**
 * 존 이동 모드의 조작 방식.
 *
 * - [TAP]: 존을 탭해 선택(들어올림) 후, 다른 존 경계/엣지 양 끝을 탭해 그 위치로 이동
 * - [DRAG_AND_DROP]: 존을 잡고 끌면 다른 존이 밀려나며, 손가락을 떼면 가까운 경계에 안착
 *
 * SWIPE 레이어에서는 항상 [TAP]으로 강제되며 설정 UI에 노출되지 않는다.
 */
enum class ZoneMoveMethod { TAP, DRAG_AND_DROP }

private const val INPUT_MODE_PREF_NAME = "input_mode_prefs"
private const val INPUT_MODE_KEY = "input_mode"
private const val SWIPE_WRAP_EDGE_KEY = "swipe_wrap_edge"
private const val ZONE_MOVE_METHOD_KEY = "zone_move_method"

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

/** 저장된 존 이동 방식을 로드. 없으면 [ZoneMoveMethod.TAP] 반환. 기본값: TAP */
fun loadZoneMoveMethod(context: Context): ZoneMoveMethod {
    val name = context.getSharedPreferences(INPUT_MODE_PREF_NAME, Context.MODE_PRIVATE)
        .getString(ZONE_MOVE_METHOD_KEY, ZoneMoveMethod.TAP.name) ?: ZoneMoveMethod.TAP.name
    return ZoneMoveMethod.entries.firstOrNull { it.name == name } ?: ZoneMoveMethod.TAP
}

/** 존 이동 방식을 SharedPreferences에 저장. */
fun saveZoneMoveMethod(context: Context, method: ZoneMoveMethod) {
    context.getSharedPreferences(INPUT_MODE_PREF_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(ZONE_MOVE_METHOD_KEY, method.name)
        .apply()
}
