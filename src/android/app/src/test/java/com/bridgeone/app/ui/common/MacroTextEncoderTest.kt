package com.bridgeone.app.ui.common

import com.bridgeone.app.ui.components.touchpad.InputModeCheck
import org.junit.Assert.*
import org.junit.Test

/**
 * MacroTextEncoder 단위 테스트
 *
 * Phase 4.7.2-B: 한글 자모 분해·두벌식 키코드 매핑·한/영 전환 삽입 로직 고정.
 * 완전 순수 함수 — Android/Compose 의존성 없음.
 *
 * 두벌식 키코드 참조:
 *   KEY_A_CODE = 0x04 (HID 'a')
 *   ㄱ → "r" → 0x04 + 17 = 0x15
 *   ㅏ → "k" → 0x04 + 10 = 0x0E
 *   ㅎ → "g" → 0x04 + 6  = 0x0A
 *   ㄴ → "s" → 0x04 + 18 = 0x16
 */
class MacroTextEncoderTest {

    // ======================================================
    // 영문
    // ======================================================

    @Test
    fun encode_englishLowercase_singleStep() {
        val result = MacroTextEncoder.encode("a")
        assertEquals("1 step", 1, result.steps.size)
        assertEquals("modifierBits=0", 0, result.steps[0].modifierBits)
        assertEquals("keyCode=0x04 (a)", listOf(0x04), result.steps[0].keyCodes)
        assertEquals("startLanguage=ENGLISH", MacroTextEncoder.Lang.ENGLISH, result.startLanguage)
        assertEquals("inputModeCheck=ENGLISH", InputModeCheck.ENGLISH, result.inputModeCheck)
        assertEquals("hanYeong 0회", 0, result.hanYeongInsertCount)
    }

    @Test
    fun encode_englishUppercase_shiftApplied() {
        val result = MacroTextEncoder.encode("A")
        assertEquals("1 step", 1, result.steps.size)
        assertEquals("modifierBits=Shift(0x02)", 0x02, result.steps[0].modifierBits)
        assertEquals("keyCode=0x04 (A)", listOf(0x04), result.steps[0].keyCodes)
    }

    @Test
    fun encode_englishZ_lastLetterCode() {
        // 'z' = KEY_A_CODE + 25 = 0x04 + 25 = 0x1D
        val result = MacroTextEncoder.encode("z")
        assertEquals("keyCode=0x1D (z)", listOf(0x1D), result.steps[0].keyCodes)
        assertEquals("no shift", 0, result.steps[0].modifierBits)
    }

    // ======================================================
    // 한글 — 받침 없음
    // ======================================================

    @Test
    fun encode_hangul_ga_twoSteps() {
        // '가' = ㄱ(r=0x15) + ㅏ(k=0x0E)
        val result = MacroTextEncoder.encode("가")
        assertEquals("2 steps for 가", 2, result.steps.size)
        assertEquals("ㄱ → 0x15", listOf(0x15), result.steps[0].keyCodes)
        assertEquals("ㄱ mod=0", 0, result.steps[0].modifierBits)
        assertEquals("ㅏ → 0x0E", listOf(0x0E), result.steps[1].keyCodes)
        assertEquals("startLanguage=KOREAN", MacroTextEncoder.Lang.KOREAN, result.startLanguage)
        assertEquals("inputModeCheck=KOREAN", InputModeCheck.KOREAN, result.inputModeCheck)
    }

    // ======================================================
    // 한글 — 받침 있음
    // ======================================================

    @Test
    fun encode_hangul_han_threeSteps() {
        // '한' = ㅎ(g=0x0A) + ㅏ(k=0x0E) + ㄴ(s=0x16)
        val result = MacroTextEncoder.encode("한")
        assertEquals("3 steps for 한", 3, result.steps.size)
        assertEquals("ㅎ → 0x0A", listOf(0x0A), result.steps[0].keyCodes)
        assertEquals("ㅏ → 0x0E", listOf(0x0E), result.steps[1].keyCodes)
        assertEquals("ㄴ → 0x16", listOf(0x16), result.steps[2].keyCodes)
    }

    // ======================================================
    // 한↔영 전환
    // ======================================================

    @Test
    fun encode_engThenKorean_insertsHanYeong() {
        // "a가": 영문 → 한글 → 한/영 전환 스텝 자동 삽입
        val result = MacroTextEncoder.encode("a가")
        assertEquals("hanYeong 1회", 1, result.hanYeongInsertCount)
        // steps: [a] [한영전환] [ㄱ] [ㅏ] = 4
        assertEquals("총 4 steps", 4, result.steps.size)
        val hanYeongStep = result.steps[1]
        assertEquals("한영전환 mod=0x40", 0x40, hanYeongStep.modifierBits)
        assertTrue("한영전환 keyCodes empty", hanYeongStep.keyCodes.isEmpty())
    }

    @Test
    fun encode_korThenEnglish_insertsHanYeong() {
        // "가a": 한글 → 영문 → 한/영 전환 스텝 삽입
        val result = MacroTextEncoder.encode("가a")
        assertEquals("hanYeong 1회", 1, result.hanYeongInsertCount)
        assertEquals("startLanguage=KOREAN", MacroTextEncoder.Lang.KOREAN, result.startLanguage)
    }

    @Test
    fun encode_spaceIsLangNeutral_noExtraHanYeong() {
        // "a 가": 공백은 언어 중립 → 전환 여전히 1회
        val result = MacroTextEncoder.encode("a 가")
        assertEquals("공백 있어도 hanYeong 1회", 1, result.hanYeongInsertCount)
    }

    // ======================================================
    // 숫자 / 기호 / 공백
    // ======================================================

    @Test
    fun encode_numberAndSymbol_noLangInfo() {
        val result = MacroTextEncoder.encode("1!")
        assertNull("startLanguage null", result.startLanguage)
        assertEquals("inputModeCheck=NONE", InputModeCheck.NONE, result.inputModeCheck)
        assertEquals("hanYeong 0회", 0, result.hanYeongInsertCount)
        assertEquals("2 steps", 2, result.steps.size)
        // '1' → 0x1E, unshifted
        assertEquals("'1' keyCode=0x1E", listOf(0x1E), result.steps[0].keyCodes)
        assertEquals("'1' no shift", 0, result.steps[0].modifierBits)
        // '!' → 0x1E, shifted
        assertEquals("'!' keyCode=0x1E", listOf(0x1E), result.steps[1].keyCodes)
        assertEquals("'!' shift=0x02", 0x02, result.steps[1].modifierBits)
    }

    @Test
    fun encode_space_langNeutralNoSwitch() {
        val result = MacroTextEncoder.encode(" ")
        assertNull("space startLanguage null", result.startLanguage)
        assertEquals("space 1 step", 1, result.steps.size)
        assertEquals("hanYeong 0회", 0, result.hanYeongInsertCount)
    }

    // ======================================================
    // 매핑 불가 문자
    // ======================================================

    @Test
    fun encode_unmappableChar_addedToSkipped() {
        val result = MacroTextEncoder.encode("€")
        assertTrue("steps empty", result.steps.isEmpty())
        assertEquals("skipped contains '€'", listOf('€'), result.skipped)
    }

    // ======================================================
    // 빈 문자열
    // ======================================================

    @Test
    fun encode_emptyString_allEmpty() {
        val result = MacroTextEncoder.encode("")
        assertTrue("steps empty", result.steps.isEmpty())
        assertTrue("skipped empty", result.skipped.isEmpty())
        assertNull("startLanguage null", result.startLanguage)
        assertEquals("inputModeCheck=NONE", InputModeCheck.NONE, result.inputModeCheck)
        assertEquals("hanYeong 0회", 0, result.hanYeongInsertCount)
    }
}
