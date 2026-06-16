package com.bridgeone.app.ui.common

import com.bridgeone.app.ui.components.touchpad.InputModeCheck
import com.bridgeone.app.ui.components.touchpad.MacroStep

/**
 * 문자열을 매크로 스텝 시퀀스로 변환.
 *
 * 한글은 PC 한글 IME가 켜진 상태에서 두벌식 자판 위치의 키를 누르는 방식으로 입력된다고 가정한다
 * (BridgeOne 한글 매크로 표준, CustomMacroPresetsRepository의 default_hello 패턴과 동일).
 * 한글 음절을 초성/중성/종성으로 분해해 각 자모를 두벌식 키 위치의 HID 키코드로 매핑하며,
 * 첫 언어 문자 기준으로 [EncodeResult.inputModeCheck]를 KOREAN 또는 ENGLISH로 보고한다.
 * 영문 대문자/Shift 기호는 Shift 비트를 자동으로 덧붙인다.
 *
 * 문자열 내 한↔영 전환을 감지하면 해당 위치에 한/영 전환 키 스텝을 자동 삽입한다.
 * 숫자·기호·공백·개행은 언어 중립(전환 트리거 안 함).
 */
object MacroTextEncoder {

    /** Shift 수정자 비트 (MOD_BIT_LSHIFT). */
    private const val MOD_SHIFT = 0x02

    /** Right Alt 수정자 비트 (MOD_BIT_RALT = 한/영 전환). */
    private const val MOD_RALT = 0x40

    /** 영문 키 'a'의 HID 키코드 (a=0x04 ~ z=0x1D). */
    private const val KEY_A_CODE = 0x04

    enum class Lang { KOREAN, ENGLISH }

    data class EncodeResult(
        val steps: List<MacroStep>,
        val inputModeCheck: InputModeCheck,
        val skipped: List<Char>,
        /** 문자열에서 첫 번째 언어 문자의 언어. 숫자·기호만 있으면 null. */
        val startLanguage: Lang?,
        /** 자동 삽입된 한/영 전환 스텝 수. */
        val hanYeongInsertCount: Int,
    )

    // 표준 유니코드 조합 순서 (호환 자모). 음절 = 0xAC00 + (초성*21 + 중성)*28 + 종성
    private val CHO = listOf('ㄱ','ㄲ','ㄴ','ㄷ','ㄸ','ㄹ','ㅁ','ㅂ','ㅃ','ㅅ','ㅆ','ㅇ','ㅈ','ㅉ','ㅊ','ㅋ','ㅌ','ㅍ','ㅎ')
    private val JUNG = listOf('ㅏ','ㅐ','ㅑ','ㅒ','ㅓ','ㅔ','ㅕ','ㅖ','ㅗ','ㅘ','ㅙ','ㅚ','ㅛ','ㅜ','ㅝ','ㅞ','ㅟ','ㅠ','ㅡ','ㅢ','ㅣ')
    private val JONG = listOf(' ','ㄱ','ㄲ','ㄳ','ㄴ','ㄵ','ㄶ','ㄷ','ㄹ','ㄺ','ㄻ','ㄼ','ㄽ','ㄾ','ㄿ','ㅀ','ㅁ','ㅂ','ㅄ','ㅅ','ㅆ','ㅇ','ㅈ','ㅊ','ㅋ','ㅌ','ㅍ','ㅎ')

    // 자모(호환 자모) → 두벌식 키스트로크. 소문자=일반 키, 대문자=Shift+키. 복합 자모는 2글자 연속 입력.
    private val JAMO_TO_KEYS: Map<Char, String> = mapOf(
        // ── 자음 ──
        'ㄱ' to "r", 'ㄲ' to "R", 'ㄴ' to "s", 'ㄷ' to "e", 'ㄸ' to "E",
        'ㄹ' to "f", 'ㅁ' to "a", 'ㅂ' to "q", 'ㅃ' to "Q", 'ㅅ' to "t",
        'ㅆ' to "T", 'ㅇ' to "d", 'ㅈ' to "w", 'ㅉ' to "W", 'ㅊ' to "c",
        'ㅋ' to "z", 'ㅌ' to "x", 'ㅍ' to "v", 'ㅎ' to "g",
        // ── 겹받침 (두 자음 연속 입력 → IME 조합기가 결합) ──
        'ㄳ' to "rt", 'ㄵ' to "sw", 'ㄶ' to "sg", 'ㄺ' to "fr", 'ㄻ' to "fa",
        'ㄼ' to "fq", 'ㄽ' to "ft", 'ㄾ' to "fx", 'ㄿ' to "fv", 'ㅀ' to "fg", 'ㅄ' to "qt",
        // ── 모음 (복합 모음은 두 키 연속) ──
        'ㅏ' to "k", 'ㅐ' to "o", 'ㅑ' to "i", 'ㅒ' to "O", 'ㅓ' to "j",
        'ㅔ' to "p", 'ㅕ' to "u", 'ㅖ' to "P", 'ㅗ' to "h", 'ㅘ' to "hk",
        'ㅙ' to "ho", 'ㅚ' to "hl", 'ㅛ' to "y", 'ㅜ' to "n", 'ㅝ' to "nj",
        'ㅞ' to "np", 'ㅟ' to "nl", 'ㅠ' to "b", 'ㅡ' to "m", 'ㅢ' to "ml", 'ㅣ' to "l",
    )

    private data class KeyStroke(val code: Int, val shift: Boolean)

    // 비-한글 문자 → (HID 키코드, Shift 여부). HidConstants 키코드 / 미국 QWERTY shifted 기준.
    private val SYMBOL: Map<Char, KeyStroke> = mapOf(
        ' ' to KeyStroke(0x2C, false), '\n' to KeyStroke(0x28, false), '\t' to KeyStroke(0x2B, false),
        // 숫자행 (unshifted)
        '1' to KeyStroke(0x1E, false), '2' to KeyStroke(0x1F, false), '3' to KeyStroke(0x20, false),
        '4' to KeyStroke(0x21, false), '5' to KeyStroke(0x22, false), '6' to KeyStroke(0x23, false),
        '7' to KeyStroke(0x24, false), '8' to KeyStroke(0x25, false), '9' to KeyStroke(0x26, false),
        '0' to KeyStroke(0x27, false),
        // 숫자행 (shifted)
        '!' to KeyStroke(0x1E, true), '@' to KeyStroke(0x1F, true), '#' to KeyStroke(0x20, true),
        '$' to KeyStroke(0x21, true), '%' to KeyStroke(0x22, true), '^' to KeyStroke(0x23, true),
        '&' to KeyStroke(0x24, true), '*' to KeyStroke(0x25, true), '(' to KeyStroke(0x26, true),
        ')' to KeyStroke(0x27, true),
        // 기호키 (unshifted / shifted)
        '-' to KeyStroke(0x2D, false), '_' to KeyStroke(0x2D, true),
        '=' to KeyStroke(0x2E, false), '+' to KeyStroke(0x2E, true),
        '[' to KeyStroke(0x2F, false), '{' to KeyStroke(0x2F, true),
        ']' to KeyStroke(0x30, false), '}' to KeyStroke(0x30, true),
        '\\' to KeyStroke(0x31, false), '|' to KeyStroke(0x31, true),
        ';' to KeyStroke(0x33, false), ':' to KeyStroke(0x33, true),
        '\'' to KeyStroke(0x34, false), '"' to KeyStroke(0x34, true),
        '`' to KeyStroke(0x35, false), '~' to KeyStroke(0x35, true),
        ',' to KeyStroke(0x36, false), '<' to KeyStroke(0x36, true),
        '.' to KeyStroke(0x37, false), '>' to KeyStroke(0x37, true),
        '/' to KeyStroke(0x38, false), '?' to KeyStroke(0x38, true),
    )

    /** 문자의 언어 분류. null = 숫자·기호·공백 등 언어 중립. */
    private fun charLang(ch: Char): Lang? = when {
        ch.code in 0xAC00..0xD7A3 -> Lang.KOREAN           // 완성형 한글 음절
        JAMO_TO_KEYS.containsKey(ch) -> Lang.KOREAN         // 단독 자모
        ch in 'a'..'z' || ch in 'A'..'Z' -> Lang.ENGLISH
        else -> null
    }

    fun encode(text: String): EncodeResult {
        val steps = mutableListOf<MacroStep>()
        val skipped = mutableListOf<Char>()
        var currentLang: Lang? = null   // 마지막 확정 언어 (null = 아직 없음)
        var startLanguage: Lang? = null
        var hanYeongInsertCount = 0

        fun insertHanYeong() {
            // 한/영 전환 키: Right Alt(modifierBits=0x40, keyCodes=empty)
            steps.add(MacroStep(modifierBits = MOD_RALT, keyCodes = emptyList()))
            hanYeongInsertCount++
        }

        for (ch in text) {
            val lang = charLang(ch)
            if (lang != null) {
                // 시작 언어 결정
                if (startLanguage == null) startLanguage = lang
                // 언어 전환 감지 → 한/영 전환 키 삽입
                if (currentLang != null && currentLang != lang) {
                    insertHanYeong()
                }
                currentLang = lang
            }

            when {
                // 완성형 한글 음절 → 초/중/종성 분해
                ch.code in 0xAC00..0xD7A3 -> {
                    val s = ch.code - 0xAC00
                    appendJamoSteps(JAMO_TO_KEYS[CHO[s / 588]], steps)
                    appendJamoSteps(JAMO_TO_KEYS[JUNG[(s % 588) / 28]], steps)
                    val jong = s % 28
                    if (jong != 0) appendJamoSteps(JAMO_TO_KEYS[JONG[jong]], steps)
                }
                // 단독 호환 자모 (ㄱ, ㅏ, ㅎㅎ 등)
                JAMO_TO_KEYS.containsKey(ch) -> {
                    appendJamoSteps(JAMO_TO_KEYS[ch], steps)
                }
                // 영문 소문자 / 대문자(Shift)
                ch in 'a'..'z' -> steps.add(MacroStep(0, listOf(KEY_A_CODE + (ch - 'a'))))
                ch in 'A'..'Z' -> steps.add(MacroStep(MOD_SHIFT, listOf(KEY_A_CODE + (ch - 'A'))))
                // 숫자 / 기호 / 공백
                SYMBOL.containsKey(ch) -> {
                    val ks = SYMBOL.getValue(ch)
                    steps.add(MacroStep(if (ks.shift) MOD_SHIFT else 0, listOf(ks.code)))
                }
                else -> skipped.add(ch)
            }
        }
        val inputModeCheck = when (startLanguage) {
            Lang.KOREAN -> InputModeCheck.KOREAN
            Lang.ENGLISH -> InputModeCheck.ENGLISH
            null -> InputModeCheck.NONE
        }
        return EncodeResult(steps, inputModeCheck, skipped, startLanguage, hanYeongInsertCount)
    }

    // 키스트로크 문자열의 각 글자를 MacroStep으로 추가 (대문자면 Shift+키).
    private fun appendJamoSteps(keys: String?, out: MutableList<MacroStep>) {
        if (keys == null) return
        for (k in keys) {
            when (k) {
                in 'a'..'z' -> out.add(MacroStep(0, listOf(KEY_A_CODE + (k - 'a'))))
                in 'A'..'Z' -> out.add(MacroStep(MOD_SHIFT, listOf(KEY_A_CODE + (k - 'A'))))
            }
        }
    }
}
