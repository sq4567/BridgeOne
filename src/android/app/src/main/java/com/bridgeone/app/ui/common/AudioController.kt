package com.bridgeone.app.ui.common

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Log
import java.util.Locale

private const val TAG = "AudioController"

/**
 * 존 음성 안내를 담당하는 전역 TTS 컨트롤러.
 *
 * - `ToastController`(StatusToast.kt) 와 같은 전역 object 싱글톤 패턴
 * - Context 주입은 `initialize(context)`로 1회만 수행 (UsbSerialManager.setUsbManager 패턴)
 * - `MainActivity.onCreate()`에서 초기화, `onDestroy()`에서 `shutdown()` 호출
 */
object AudioController {

    private var tts: TextToSpeech? = null
    private var ready = false
    /** 존 음성 안내 활성화 여부. 기본값: true */
    private var enabled = true
    /** TTS 말하기 속도. 기본값: 1.0f */
    private var rate = 1.0f
    /** TTS 음성 성별. 기본값: TtsGender.DEFAULT */
    private var gender = TtsGender.DEFAULT

    /**
     * TTS 엔진을 초기화한다.
     *
     * `MainActivity.onCreate()`에서 `applicationContext`로 호출해야 함.
     * TTS 초기화는 비동기이므로 콜백에서 `ready = true`가 설정된 이후부터 발화 가능.
     * 이미 초기화된 경우 무시 (재진입 안전).
     */
    fun initialize(context: Context) {
        if (tts != null) return

        enabled = loadAudioFeedbackEnabled(context)
        rate = loadTtsRate(context)
        gender = loadTtsGender(context)

        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val langResult = tts?.setLanguage(Locale.KOREAN)
                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.w(TAG, "Korean TTS not supported, falling back to system default")
                }
                tts?.setSpeechRate(rate)
                applyVoice()
                ready = true
                Log.d(TAG, "TTS initialized (enabled=$enabled, rate=$rate, gender=$gender)")
            } else {
                Log.e(TAG, "TTS initialization failed: status=$status")
            }
        }
    }

    /** 음성 안내 ON/OFF를 업데이트. */
    fun setEnabled(value: Boolean) {
        enabled = value
        if (!value) tts?.stop()
    }

    /** TTS 말하기 속도를 업데이트. 기본값: 1.0f */
    fun setRate(value: Float) {
        rate = value
        tts?.setSpeechRate(value)
    }

    /**
     * TTS 음성 성별을 업데이트.
     *
     * 한국어 음성 목록에서 성별 힌트(음성 이름 끝 글자 f/m)로 후보를 필터링한다.
     * 매칭 음성이 없으면 현재 음성을 그대로 유지.
     */
    fun setGender(value: TtsGender) {
        gender = value
        if (ready) applyVoice()
    }

    /**
     * 텍스트를 음성으로 읽는다.
     *
     * - `enabled=false`이거나 TTS가 준비 안 됐으면 무시
     * - `QUEUE_FLUSH`로 이전 발화 즉시 중단 후 새 발화 (빠른 재진입 시 겹침 방지)
     */
    fun speak(text: String) {
        if (!enabled || !ready || text.isBlank()) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "bridgeone_zone")
    }

    /** 현재 발화를 중단. */
    fun stop() {
        tts?.stop()
    }

    /**
     * TTS 엔진을 해제한다.
     *
     * `MainActivity.onDestroy()`에서 호출해야 함.
     */
    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
        Log.d(TAG, "TTS shut down")
    }

    /**
     * 현재 gender 설정에 맞는 한국어 음성을 선택해 적용한다.
     *
     * Google TTS 한국어 음성 이름 패턴 예시:
     * - "ko-kr-x-iof-local" → 마지막 세그먼트 "iof" 끝 'f' → 여성
     * - "ko-kr-x-ism-local" → 마지막 세그먼트 "ism" 끝 'm' → 남성
     * 표준 API에 gender 필드가 없으므로 이름 휴리스틱으로 판별.
     * 매칭 음성이 없으면 시스템 기본 음성 유지.
     */
    private fun applyVoice() {
        val t = tts ?: return
        if (gender == TtsGender.DEFAULT) return

        val koreanVoices = t.voices
            ?.filter { it.locale.language == Locale.KOREAN.language }
            ?: return
        if (koreanVoices.isEmpty()) return

        val matched = koreanVoices
            .filter { v -> if (gender == TtsGender.FEMALE) v.likelyFemale() else v.likelyMale() }
            .maxByOrNull { it.quality }

        if (matched != null) {
            t.voice = matched
            Log.d(TAG, "Voice selected: ${matched.name} for gender=$gender")
        } else {
            Log.w(TAG, "No ${gender.name} voice found for Korean, keeping current voice")
        }
    }
}

/**
 * 음성 이름 끝 글자로 여성 음성인지 판별.
 * "-local"/"-network"/"-embedded" 접미사를 제거한 뒤 마지막 세그먼트의 끝 문자가 'f'이면 여성.
 */
private fun Voice.likelyFemale(): Boolean {
    val stem = name.lowercase()
        .removeSuffix("-local").removeSuffix("-network").removeSuffix("-embedded")
    val last = stem.substringAfterLast("-")
    return last.endsWith("f") || name.lowercase().contains("female")
}

/**
 * 음성 이름 끝 글자로 남성 음성인지 판별.
 * "-local"/"-network"/"-embedded" 접미사를 제거한 뒤 마지막 세그먼트의 끝 문자가 'm'이면 남성.
 */
private fun Voice.likelyMale(): Boolean {
    val stem = name.lowercase()
        .removeSuffix("-local").removeSuffix("-network").removeSuffix("-embedded")
    val last = stem.substringAfterLast("-")
    return last.endsWith("m") || name.lowercase().contains("male")
}
