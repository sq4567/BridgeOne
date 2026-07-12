package com.bridgeone.app.ui.common

import android.content.Context

private const val ZONE_CROSS_BEHAVIOR_PREF_NAME = "zone_cross_behavior_prefs"
private const val ZONE_CROSS_BEHAVIOR_KEY = "zone_cross_behavior"

/**
 * 멀티 존 실시간 점프(Phase 4.9.11) 중 손을 떼지 않고 다른 서브 패드로 경계를 넘었을 때의 동작.
 *
 * - [OFF]: 기본값. 기존 동작 그대로 즉시 점프하며 별도 피드백 없음.
 * - [HAPTIC]: 점프는 그대로 허용하되, 경계를 넘는 순간 햅틱으로 알려준다.
 * - [BLOCK]: 점프 자체를 막는다 — 좌표가 터치가 시작된 존에 고정되어, 다른 존 쪽으로 손가락이
 *   넘어가도 커서는 시작 존의 매핑 경계에서 멈춘다.
 *
 * 세 값은 상호 배타적(라디오 그룹)이다. 환경 설정에서 변경하며 SharedPreferences에 영속화.
 */
enum class ZoneCrossBehavior { OFF, HAPTIC, BLOCK }

/** 저장된 [ZoneCrossBehavior]를 로드. 없으면 [ZoneCrossBehavior.OFF] 반환. 기본값: OFF */
fun loadZoneCrossBehavior(context: Context): ZoneCrossBehavior {
    val name = context.getSharedPreferences(ZONE_CROSS_BEHAVIOR_PREF_NAME, Context.MODE_PRIVATE)
        .getString(ZONE_CROSS_BEHAVIOR_KEY, ZoneCrossBehavior.OFF.name) ?: ZoneCrossBehavior.OFF.name
    return ZoneCrossBehavior.entries.firstOrNull { it.name == name } ?: ZoneCrossBehavior.OFF
}

/** [ZoneCrossBehavior]를 SharedPreferences에 저장. */
fun saveZoneCrossBehavior(context: Context, behavior: ZoneCrossBehavior) {
    context.getSharedPreferences(ZONE_CROSS_BEHAVIOR_PREF_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(ZONE_CROSS_BEHAVIOR_KEY, behavior.name)
        .apply()
}
