package com.bridgeone.app.ui.common

import android.content.Context

private const val AUDIO_FEEDBACK_PREF_NAME = "audio_feedback_prefs"
private const val AUDIO_FEEDBACK_ENABLED_KEY = "audio_feedback_enabled"
private const val TTS_RATE_KEY = "tts_rate"
private const val TTS_GENDER_KEY = "tts_gender"

/** TTS 음성 성별 선택. DEFAULT는 시스템 기본 음성 사용. */
enum class TtsGender { DEFAULT, FEMALE, MALE }

/** 존 음성 안내 ON/OFF를 로드. 기본값: true */
fun loadAudioFeedbackEnabled(context: Context): Boolean =
    context.getSharedPreferences(AUDIO_FEEDBACK_PREF_NAME, Context.MODE_PRIVATE)
        .getBoolean(AUDIO_FEEDBACK_ENABLED_KEY, true)  // 기본값: true

/** 존 음성 안내 ON/OFF를 SharedPreferences에 저장. */
fun saveAudioFeedbackEnabled(context: Context, enabled: Boolean) {
    context.getSharedPreferences(AUDIO_FEEDBACK_PREF_NAME, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(AUDIO_FEEDBACK_ENABLED_KEY, enabled)
        .apply()
}

/** TTS 말하기 속도를 로드. 기본값: 1.0f */
fun loadTtsRate(context: Context): Float =
    context.getSharedPreferences(AUDIO_FEEDBACK_PREF_NAME, Context.MODE_PRIVATE)
        .getFloat(TTS_RATE_KEY, 1.0f)  // 기본값: 1.0f

/** TTS 말하기 속도를 SharedPreferences에 저장. */
fun saveTtsRate(context: Context, rate: Float) {
    context.getSharedPreferences(AUDIO_FEEDBACK_PREF_NAME, Context.MODE_PRIVATE)
        .edit()
        .putFloat(TTS_RATE_KEY, rate)
        .apply()
}

/** TTS 음성 성별을 로드. 기본값: TtsGender.DEFAULT */
fun loadTtsGender(context: Context): TtsGender {
    val name = context.getSharedPreferences(AUDIO_FEEDBACK_PREF_NAME, Context.MODE_PRIVATE)
        .getString(TTS_GENDER_KEY, TtsGender.DEFAULT.name)  // 기본값: DEFAULT
    return TtsGender.entries.firstOrNull { it.name == name } ?: TtsGender.DEFAULT
}

/** TTS 음성 성별을 SharedPreferences에 저장. */
fun saveTtsGender(context: Context, gender: TtsGender) {
    context.getSharedPreferences(AUDIO_FEEDBACK_PREF_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(TTS_GENDER_KEY, gender.name)
        .apply()
}
