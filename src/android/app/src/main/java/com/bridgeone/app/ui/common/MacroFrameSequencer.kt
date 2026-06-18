package com.bridgeone.app.ui.common

import com.bridgeone.app.protocol.BridgeFrame
import com.bridgeone.app.ui.components.touchpad.MacroStep
import com.bridgeone.app.ui.components.touchpad.MacroStepKind
import com.bridgeone.app.ui.utils.ClickDetector

/**
 * 매크로/단축키 스텝 시퀀스를 BridgeFrame 리스트로 변환하는 순수 함수 오브젝트.
 *
 * 사이드이펙트(sendFrame/코루틴/오버레이) 없음 → JVM 단위 테스트 가능.
 * StandardModePage의 onSendMacro·onSendShortcut 내 시퀀싱 로직을 1:1 추출.
 */
object MacroFrameSequencer {

    /**
     * 전송할 프레임 + 전송 후 대기 시간.
     * delayAfterMs=0이면 즉시 다음 프레임 전송(마지막 프레임 포함).
     */
    data class TimedFrame(val frame: BridgeFrame, val delayAfterMs: Long)

    /**
     * 매크로 스텝 리스트를 [TimedFrame] 시퀀스로 변환 (순수).
     *
     * TAP: combinedMod/combinedKeys로 press → INTRA 딜레이 → 홀드 복귀/0 프레임,
     *      마지막 스텝·반복이 아니면 step 딜레이 추가.
     * HOLD: 누적 홀드 상태 갱신 후 갱신 프레임 1개. 마지막 스텝이면 딜레이 0.
     * RELEASE 전체: 누적 초기화 + 0 프레임. RELEASE 특정: 해당 키/mod 제거 + 갱신 프레임.
     * dangling hold 안전 해제: 순회 후 잔여 홀드 있으면 0 프레임(delayAfterMs=0) 추가.
     */
    fun buildMacro(steps: List<MacroStep>, stepDelayMs: Int): List<TimedFrame> {
        val result = mutableListOf<TimedFrame>()
        var heldMod = 0
        val heldKeys = mutableListOf<Int>()

        steps.forEachIndexed { i, step ->
            val delayMs = (step.delayAfterMs ?: stepDelayMs).toLong()
            val isLastStep = i == steps.lastIndex

            when (step.kind) {
                MacroStepKind.TAP -> {
                    val count = step.repeatCount.coerceAtLeast(1)
                    val combinedMod = (heldMod or step.modifierBits).toUByte()
                    val combinedKeys = (heldKeys + step.keyCodes).take(MACRO_MAX_HELD_KEYS)
                    val tapKey1 = (combinedKeys.getOrNull(0) ?: 0).toUByte()
                    val tapKey2 = (combinedKeys.getOrNull(1) ?: 0).toUByte()
                    val holdMod = heldMod.toUByte()
                    val holdKey1 = (heldKeys.getOrNull(0) ?: 0).toUByte()
                    val holdKey2 = (heldKeys.getOrNull(1) ?: 0).toUByte()
                    val hasHold = heldMod != 0 || heldKeys.isNotEmpty()

                    repeat(count) { r ->
                        val isLastRepeat = r == count - 1
                        // press 프레임 → INTRA 딜레이
                        result.add(TimedFrame(
                            ClickDetector.createKeyboardFrame(setOf(combinedMod), tapKey1, tapKey2),
                            MACRO_INTRA_STEP_PRESS_RELEASE_MS
                        ))
                        // 홀드 복귀 또는 0 프레임
                        val releaseFrame = if (hasHold) {
                            ClickDetector.createKeyboardFrame(setOf(holdMod), holdKey1, holdKey2)
                        } else {
                            ClickDetector.createKeyboardFrame(emptySet(), 0u)
                        }
                        val releaseDelay = if (isLastStep && isLastRepeat) 0L else delayMs
                        result.add(TimedFrame(releaseFrame, releaseDelay))
                    }
                }
                MacroStepKind.HOLD -> {
                    heldMod = heldMod or step.modifierBits
                    for (code in step.keyCodes) {
                        if (code != 0 && !heldKeys.contains(code) && heldKeys.size < MACRO_MAX_HELD_KEYS) {
                            heldKeys.add(code)
                        }
                    }
                    val key1 = (heldKeys.getOrNull(0) ?: 0).toUByte()
                    val key2 = (heldKeys.getOrNull(1) ?: 0).toUByte()
                    val holdDelay = if (isLastStep) 0L else delayMs
                    result.add(TimedFrame(
                        ClickDetector.createKeyboardFrame(setOf(heldMod.toUByte()), key1, key2),
                        holdDelay
                    ))
                }
                MacroStepKind.RELEASE -> {
                    val isReleaseAll = step.keyCodes.isEmpty() && step.modifierBits == 0
                    val releaseDelay = if (isLastStep) 0L else delayMs
                    if (isReleaseAll) {
                        heldMod = 0
                        heldKeys.clear()
                        result.add(TimedFrame(
                            ClickDetector.createKeyboardFrame(emptySet(), 0u),
                            releaseDelay
                        ))
                    } else {
                        heldMod = heldMod and step.modifierBits.inv()
                        heldKeys.removeAll(step.keyCodes.toSet())
                        val frame = if (heldMod != 0 || heldKeys.isNotEmpty()) {
                            val key1 = (heldKeys.getOrNull(0) ?: 0).toUByte()
                            val key2 = (heldKeys.getOrNull(1) ?: 0).toUByte()
                            ClickDetector.createKeyboardFrame(setOf(heldMod.toUByte()), key1, key2)
                        } else {
                            ClickDetector.createKeyboardFrame(emptySet(), 0u)
                        }
                        result.add(TimedFrame(frame, releaseDelay))
                    }
                }
            }
        }

        // dangling hold 안전 해제: 잔여 홀드가 있으면 0 프레임 추가
        if (heldMod != 0 || heldKeys.isNotEmpty()) {
            result.add(TimedFrame(
                ClickDetector.createKeyboardFrame(emptySet(), 0u),
                delayAfterMs = 0L
            ))
        }

        return result
    }

    /**
     * 단축키 TAP press+release 프레임 2개 생성 (순수).
     * keyCodes[0..1]만 사용 (프로토콜 최대 2키).
     */
    fun buildShortcut(modifierBits: Int, keyCodes: List<Int>): List<BridgeFrame> {
        val mod = modifierBits.toUByte()
        val key1 = (keyCodes.getOrNull(0) ?: 0).toUByte()
        val key2 = (keyCodes.getOrNull(1) ?: 0).toUByte()
        return listOf(
            ClickDetector.createKeyboardFrame(if (mod != 0u.toUByte()) setOf(mod) else emptySet(), key1, key2),
            ClickDetector.createKeyboardFrame(emptySet(), 0u)
        )
    }
}
