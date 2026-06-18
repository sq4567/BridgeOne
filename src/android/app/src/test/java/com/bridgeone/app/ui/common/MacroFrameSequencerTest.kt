package com.bridgeone.app.ui.common

import com.bridgeone.app.ui.components.touchpad.MacroStep
import com.bridgeone.app.ui.components.touchpad.MacroStepKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * MacroFrameSequencer 단위 테스트
 *
 * Phase 4.7.4-B: 매크로 시퀀싱 로직 순수 함수 추출 검증.
 * 완전 순수 함수 — Android/Compose 의존성 없음.
 *
 * 검증 항목:
 *   - TAP repeat: repeatCount=N → press/release 프레임 쌍 N개, INTRA 딜레이 N개
 *   - 홀드 합성: HOLD 후 TAP의 combinedMod/combinedKeys 2개 제한
 *   - dangling hold 안전 해제: HOLD로 끝나는 매크로 → 마지막에 0 프레임 추가
 *   - RELEASE 전체/특정 분기
 *   - estimatedMs = sumOf { delayAfterMs } 동등성
 *   - buildShortcut: press + release 2프레임
 */
class MacroFrameSequencerTest {

    private val STEP_DELAY = 80  // stepDelayMs 대표값

    // ──────────────────────────────────────────────
    // 헬퍼
    // ──────────────────────────────────────────────

    private fun tap(
        modifierBits: Int = 0,
        keyCodes: List<Int> = emptyList(),
        repeatCount: Int = 1,
        delayAfterMs: Int? = null
    ) = MacroStep(
        modifierBits = modifierBits,
        keyCodes = keyCodes,
        repeatCount = repeatCount,
        delayAfterMs = delayAfterMs,
        kind = MacroStepKind.TAP
    )

    private fun hold(
        modifierBits: Int = 0,
        keyCodes: List<Int> = emptyList()
    ) = MacroStep(
        modifierBits = modifierBits,
        keyCodes = keyCodes,
        kind = MacroStepKind.HOLD
    )

    private fun release(
        modifierBits: Int = 0,
        keyCodes: List<Int> = emptyList()
    ) = MacroStep(
        modifierBits = modifierBits,
        keyCodes = keyCodes,
        kind = MacroStepKind.RELEASE
    )

    // ──────────────────────────────────────────────
    // TAP 기본
    // ──────────────────────────────────────────────

    @Test
    fun tap_singleRepeat_producesTwoFrames() {
        // 단일 TAP → press 1 + release 1 = 2프레임
        val frames = MacroFrameSequencer.buildMacro(listOf(tap(keyCodes = listOf(0x04))), STEP_DELAY)
        assertEquals("단일 TAP 프레임 수", 2, frames.size)
    }

    @Test
    fun tap_singleRepeat_pressFrameHasKeyCode() {
        // press 프레임의 keyCode1이 지정한 키코드
        val frames = MacroFrameSequencer.buildMacro(listOf(tap(keyCodes = listOf(0x04))), STEP_DELAY)
        assertEquals("press 프레임 keyCode1", 0x04.toUByte(), frames[0].frame.keyCode1)
    }

    @Test
    fun tap_singleRepeat_releaseFrameIsZero() {
        // release 프레임의 모든 키가 0
        val frames = MacroFrameSequencer.buildMacro(listOf(tap(keyCodes = listOf(0x04))), STEP_DELAY)
        assertEquals("release 프레임 keyCode1=0", 0u.toUByte(), frames[1].frame.keyCode1)
        assertEquals("release 프레임 modifiers=0", 0u.toUByte(), frames[1].frame.modifiers)
    }

    @Test
    fun tap_singleRepeat_intraDealyOnPressFrame() {
        // press 프레임의 delayAfterMs = MACRO_INTRA_STEP_PRESS_RELEASE_MS
        val frames = MacroFrameSequencer.buildMacro(listOf(tap(keyCodes = listOf(0x04))), STEP_DELAY)
        assertEquals("press 프레임 INTRA 딜레이", MACRO_INTRA_STEP_PRESS_RELEASE_MS, frames[0].delayAfterMs)
    }

    @Test
    fun tap_singleRepeat_lastReleaseDelayIsZero() {
        // 마지막 스텝·반복의 release 프레임은 딜레이 0
        val frames = MacroFrameSequencer.buildMacro(listOf(tap(keyCodes = listOf(0x04))), STEP_DELAY)
        assertEquals("마지막 release 딜레이=0", 0L, frames[1].delayAfterMs)
    }

    @Test
    fun tap_repeatCount3_producesSixFrames() {
        // repeatCount=3 → press+release 쌍 3개 = 6프레임
        val frames = MacroFrameSequencer.buildMacro(listOf(tap(keyCodes = listOf(0x04), repeatCount = 3)), STEP_DELAY)
        assertEquals("repeat=3 프레임 수", 6, frames.size)
    }

    @Test
    fun tap_repeatCount3_intraDealyOnAllPressFrames() {
        // 모든 press 프레임(인덱스 0, 2, 4)의 딜레이는 INTRA
        val frames = MacroFrameSequencer.buildMacro(listOf(tap(keyCodes = listOf(0x04), repeatCount = 3)), STEP_DELAY)
        for (i in listOf(0, 2, 4)) {
            assertEquals("press[$i] INTRA 딜레이", MACRO_INTRA_STEP_PRESS_RELEASE_MS, frames[i].delayAfterMs)
        }
    }

    @Test
    fun tap_repeatCount3_releaseDelayIsStepDelayExceptLast() {
        // 중간 release(인덱스 1, 3)는 stepDelayMs, 마지막(인덱스 5)는 0
        val frames = MacroFrameSequencer.buildMacro(listOf(tap(keyCodes = listOf(0x04), repeatCount = 3)), STEP_DELAY)
        assertEquals("release[1] step 딜레이", STEP_DELAY.toLong(), frames[1].delayAfterMs)
        assertEquals("release[3] step 딜레이", STEP_DELAY.toLong(), frames[3].delayAfterMs)
        assertEquals("release[5] 딜레이=0", 0L, frames[5].delayAfterMs)
    }

    // ──────────────────────────────────────────────
    // HOLD 합성
    // ──────────────────────────────────────────────

    @Test
    fun holdThenTap_combinedModIsOrOfBoth() {
        // HOLD mod=0x02, TAP mod=0x01 → press 프레임의 modifiers = 0x03
        val steps = listOf(hold(modifierBits = 0x02), tap(modifierBits = 0x01, keyCodes = listOf(0x04)))
        val frames = MacroFrameSequencer.buildMacro(steps, STEP_DELAY)
        // frame 0 = HOLD 상태 프레임, frame 1 = TAP press, frame 2 = TAP hold-복귀 release
        val tapPressFrame = frames[1].frame
        assertEquals("TAP press combinedMod", 0x03.toUByte(), tapPressFrame.modifiers)
    }

    @Test
    fun holdThenTap_combinedKeysLimitedToMacroMaxHeldKeys() {
        // HOLD 키 [0x04, 0x05], TAP 키 [0x06] → combinedKeys는 2개 제한
        val steps = listOf(
            hold(keyCodes = listOf(0x04, 0x05)),
            tap(keyCodes = listOf(0x06))
        )
        val frames = MacroFrameSequencer.buildMacro(steps, STEP_DELAY)
        val tapPressFrame = frames[1].frame  // frame 0 = HOLD, frame 1 = TAP press
        // keyCode1=0x04, keyCode2=0x05 (HOLD 키 우선, 2개 제한으로 TAP 키 0x06 제외)
        assertEquals("keyCode1=0x04", 0x04.toUByte(), tapPressFrame.keyCode1)
        assertEquals("keyCode2=0x05", 0x05.toUByte(), tapPressFrame.keyCode2)
    }

    @Test
    fun holdThenTap_releaseFrameRestorestHoldState() {
        // HOLD 키 [0x04] → TAP press 후 release 프레임은 홀드 상태 복귀
        val steps = listOf(hold(keyCodes = listOf(0x04)), tap(keyCodes = listOf(0x05)))
        val frames = MacroFrameSequencer.buildMacro(steps, STEP_DELAY)
        // frame 0 = HOLD, frame 1 = TAP press, frame 2 = TAP release(홀드 복귀)
        val releaseFrame = frames[2].frame
        assertEquals("홀드 복귀 release의 keyCode1=0x04", 0x04.toUByte(), releaseFrame.keyCode1)
    }

    // ──────────────────────────────────────────────
    // dangling hold 안전 해제
    // ──────────────────────────────────────────────

    @Test
    fun holdOnly_appendsZeroFrameAtEnd() {
        // HOLD만으로 끝나는 매크로 → 마지막에 0 프레임(dangling 해제) 추가
        val steps = listOf(hold(keyCodes = listOf(0x04)))
        val frames = MacroFrameSequencer.buildMacro(steps, STEP_DELAY)
        assertEquals("HOLD+dangling 해제 = 2프레임", 2, frames.size)
        val danglingFrame = frames[1].frame
        assertEquals("dangling keyCode1=0", 0u.toUByte(), danglingFrame.keyCode1)
        assertEquals("dangling modifiers=0", 0u.toUByte(), danglingFrame.modifiers)
        assertEquals("dangling delayAfterMs=0", 0L, frames[1].delayAfterMs)
    }

    @Test
    fun tapOnly_noDanglingFrame() {
        // TAP만이면 dangling 없음 (release 프레임이 0이므로 추가 불필요)
        val steps = listOf(tap(keyCodes = listOf(0x04)))
        val frames = MacroFrameSequencer.buildMacro(steps, STEP_DELAY)
        assertEquals("TAP만 = 2프레임 (dangling 없음)", 2, frames.size)
    }

    // ──────────────────────────────────────────────
    // RELEASE 분기
    // ──────────────────────────────────────────────

    @Test
    fun releaseAll_clearsHoldAndEmitsZeroFrame() {
        // HOLD → RELEASE(전체) → 0 프레임 + dangling 없음
        val steps = listOf(hold(keyCodes = listOf(0x04)), release())
        val frames = MacroFrameSequencer.buildMacro(steps, STEP_DELAY)
        // frame 0 = HOLD, frame 1 = RELEASE(전체) → 0 프레임. dangling 없음(홀드가 이미 해제됨)
        assertEquals("HOLD+RELEASE 전체 = 2프레임", 2, frames.size)
        val releaseFrame = frames[1].frame
        assertEquals("전체 release keyCode1=0", 0u.toUByte(), releaseFrame.keyCode1)
        assertEquals("전체 release modifiers=0", 0u.toUByte(), releaseFrame.modifiers)
    }

    @Test
    fun releaseSpecific_removesOnlyTargetKey() {
        // HOLD [0x04, 0x05] → RELEASE [0x04] → 남은 홀드 상태로 업데이트
        val steps = listOf(
            hold(keyCodes = listOf(0x04, 0x05)),
            release(keyCodes = listOf(0x04))
        )
        val frames = MacroFrameSequencer.buildMacro(steps, STEP_DELAY)
        // frame 0 = HOLD[0x04,0x05], frame 1 = RELEASE[0x04] → [0x05] 남음, frame 2 = dangling(0x05 해제)
        assertEquals("HOLD+RELEASE 특정+dangling = 3프레임", 3, frames.size)
        val updateFrame = frames[1].frame
        assertEquals("특정 release 후 남은 keyCode1=0x05", 0x05.toUByte(), updateFrame.keyCode1)
        assertEquals("특정 release 후 keyCode2=0", 0u.toUByte(), updateFrame.keyCode2)
    }

    // ──────────────────────────────────────────────
    // estimatedMs 동등성
    // ──────────────────────────────────────────────

    @Test
    fun estimatedMs_equalsSumOfDelays_tapRepeat() {
        // TAP repeatCount=3: estimatedMs = foldIndexed 결과 == sumOf { delayAfterMs }
        val steps = listOf(tap(keyCodes = listOf(0x04), repeatCount = 3))
        val frames = MacroFrameSequencer.buildMacro(steps, STEP_DELAY)
        val sumOfDelays = frames.sumOf { it.delayAfterMs }
        // 기존 foldIndexed 계산: count * INTRA + (count-1) * stepDelay
        val expected = 3 * MACRO_INTRA_STEP_PRESS_RELEASE_MS + (3 - 1) * STEP_DELAY.toLong()
        assertEquals("estimatedMs 동등성 (TAP repeat)", expected, sumOfDelays)
    }

    @Test
    fun estimatedMs_equalsSumOfDelays_multiStep() {
        // TAP + TAP (2스텝): sumOf가 기존 foldIndexed와 동일
        val steps = listOf(
            tap(keyCodes = listOf(0x04), repeatCount = 2),
            tap(keyCodes = listOf(0x05), repeatCount = 1)
        )
        val frames = MacroFrameSequencer.buildMacro(steps, STEP_DELAY)
        val sumOfDelays = frames.sumOf { it.delayAfterMs }
        // 스텝1(count=2): 2*INTRA + 2*stepDelay (마지막 repeat 아님이므로 마지막에도 stepDelay)
        // 스텝2(count=1): 1*INTRA + 0 (마지막 스텝 마지막 repeat → 0)
        val expected = 2 * MACRO_INTRA_STEP_PRESS_RELEASE_MS + 2 * STEP_DELAY.toLong() +
            1 * MACRO_INTRA_STEP_PRESS_RELEASE_MS + 0L
        assertEquals("estimatedMs 동등성 (멀티스텝)", expected, sumOfDelays)
    }

    // ──────────────────────────────────────────────
    // buildShortcut
    // ──────────────────────────────────────────────

    @Test
    fun buildShortcut_producesTwoFrames() {
        val frames = MacroFrameSequencer.buildShortcut(modifierBits = 0x01, keyCodes = listOf(0x04))
        assertEquals("단축키 = 2프레임", 2, frames.size)
    }

    @Test
    fun buildShortcut_pressFrameHasModAndKey() {
        val frames = MacroFrameSequencer.buildShortcut(modifierBits = 0x01, keyCodes = listOf(0x04))
        assertEquals("press modifiers=0x01", 0x01.toUByte(), frames[0].modifiers)
        assertEquals("press keyCode1=0x04", 0x04.toUByte(), frames[0].keyCode1)
    }

    @Test
    fun buildShortcut_releaseFrameIsZero() {
        val frames = MacroFrameSequencer.buildShortcut(modifierBits = 0x01, keyCodes = listOf(0x04))
        assertEquals("release modifiers=0", 0u.toUByte(), frames[1].modifiers)
        assertEquals("release keyCode1=0", 0u.toUByte(), frames[1].keyCode1)
    }

    @Test
    fun buildShortcut_noModifier_pressFrameModIsZero() {
        // modifier=0이면 press 프레임의 modifiers도 0
        val frames = MacroFrameSequencer.buildShortcut(modifierBits = 0, keyCodes = listOf(0x04))
        assertEquals("modifier=0 press modifiers=0", 0u.toUByte(), frames[0].modifiers)
    }

    @Test
    fun buildShortcut_emptyKeyCodes_keyCode1IsZero() {
        // keyCodes 비어있으면 keyCode1=0
        val frames = MacroFrameSequencer.buildShortcut(modifierBits = 0, keyCodes = emptyList())
        assertEquals("빈 keyCodes keyCode1=0", 0u.toUByte(), frames[0].keyCode1)
    }
}
