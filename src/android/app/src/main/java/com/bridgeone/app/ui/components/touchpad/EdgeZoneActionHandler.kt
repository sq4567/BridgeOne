package com.bridgeone.app.ui.components.touchpad

import com.bridgeone.app.ui.common.DYNAMICS_PRESETS
import com.bridgeone.app.ui.common.MODE_PRESETS

/**
 * EdgeZoneAction을 TouchpadState에 적용하여 새 상태를 반환한다.
 */
object EdgeZoneActionHandler {

    fun applyZoneAction(
        state: TouchpadState,
        action: EdgeZoneAction,
        customPresetsCount: Int = 0
    ): TouchpadState = when (action) {
        is EdgeZoneAction.ToggleMode -> applyToggle(state, action.mode, customPresetsCount)
        is EdgeZoneAction.CyclePreset -> applyCyclePreset(state, action.presetType)
        is EdgeZoneAction.OpenSettings -> applyCycleSettings(state, action.settingsType)
        EdgeZoneAction.Unassigned -> state
        // 실제 복원은 TouchpadWrapper에서 onRestorePrevious 콜백으로 처리
        EdgeZoneAction.RestorePreviousMode -> state
        is EdgeZoneAction.SetDpi -> state.copy(dpiLevel = action.level, customDpiMultiplier = null)
        is EdgeZoneAction.SetScrollSpeed -> state.copy(scrollSensitivity = action.sensitivity)
        is EdgeZoneAction.SetClickMode -> state.copy(clickMode = action.mode)
        is EdgeZoneAction.SetMoveMode -> state.copy(moveMode = action.mode)
        is EdgeZoneAction.SetScrollMode -> applySetScrollMode(state, action.mode)
        EdgeZoneAction.SwapScrollMode -> applySwapScrollMode(state)
        is EdgeZoneAction.SetCustomDpi -> state.copy(customDpiMultiplier = action.multiplier)
        is EdgeZoneAction.SetCustomScrollSpeed -> state.copy(customScrollSensitivityMultiplier = action.multiplier)
        is EdgeZoneAction.SetDynamicsPreset -> state.copy(dynamicsPresetIndex = action.index)
        is EdgeZoneAction.SetModePreset -> applySetModePreset(state, action.index)
        // 부수효과형 — 실제 동작은 TouchpadWrapper의 콜백으로 위임
        is EdgeZoneAction.SendShortcut -> state
        is EdgeZoneAction.SendMacro -> state
        is EdgeZoneAction.MouseHoldToggle -> state
        is EdgeZoneAction.CyclePage -> state
        is EdgeZoneAction.JumpToPage -> state
    }

    private fun applyToggle(state: TouchpadState, mode: EdgeSwipeMode, customPresetsCount: Int): TouchpadState =
        when (mode) {
            EdgeSwipeMode.SCROLL -> when (state.scrollMode) {
                ScrollMode.OFF             -> state.copy(
                    scrollMode = ScrollMode.NORMAL_SCROLL,
                    lastScrollMode = ScrollMode.NORMAL_SCROLL,
                    customDpiMultiplier = null
                )
                ScrollMode.NORMAL_SCROLL   -> state.copy(
                    scrollMode = ScrollMode.INFINITE_SCROLL,
                    lastScrollMode = ScrollMode.INFINITE_SCROLL
                )
                ScrollMode.INFINITE_SCROLL -> state.copy(
                    scrollMode = ScrollMode.NORMAL_SCROLL,
                    lastScrollMode = ScrollMode.NORMAL_SCROLL
                )
            }
            EdgeSwipeMode.CLICK -> state.copy(
                clickMode = if (state.clickMode == ClickMode.LEFT_CLICK) ClickMode.RIGHT_CLICK else ClickMode.LEFT_CLICK
            )
            EdgeSwipeMode.MOVE -> state.copy(
                moveMode = if (state.moveMode == MoveMode.FREE) MoveMode.RIGHT_ANGLE else MoveMode.FREE
            )
            EdgeSwipeMode.CURSOR -> state.copy(
                cursorMode = if (state.cursorMode == CursorMode.SINGLE) CursorMode.MULTI else CursorMode.SINGLE
            )
            EdgeSwipeMode.DPI -> state.copy(
                dpiLevel = state.dpiLevel.next(),
                customDpiMultiplier = null
            )
            EdgeSwipeMode.SCROLL_SPEED -> state.copy(
                scrollSensitivity = state.scrollSensitivity.next()
            )
            EdgeSwipeMode.DYNAMICS -> state.copy(
                dynamicsPresetIndex = (state.dynamicsPresetIndex + 1) % (DYNAMICS_PRESETS.size + customPresetsCount)
            )
        }

    private fun applyCyclePreset(state: TouchpadState, presetType: PresetType): TouchpadState =
        when (presetType) {
            PresetType.DYNAMICS -> state.copy(
                dynamicsPresetIndex = (state.dynamicsPresetIndex + 1) % DYNAMICS_PRESETS.size
            )
            PresetType.MODE -> state.copy(
                modePresetIndex = (state.modePresetIndex + 1) % MODE_PRESETS.size
            ).let { newState ->
                val preset = MODE_PRESETS[newState.modePresetIndex]
                newState.copy(
                    clickMode = preset.padModeState.clickMode,
                    moveMode = preset.padModeState.moveMode,
                    scrollMode = preset.padModeState.scrollMode,
                    dpiLevel = preset.padModeState.dpi,
                    dynamicsPresetIndex = preset.dynamicsPresetIndex,
                    customDpiMultiplier = null
                )
            }
        }

    private fun applyCycleSettings(state: TouchpadState, settingsType: SettingsType): TouchpadState =
        when (settingsType) {
            SettingsType.DPI -> state.copy(
                dpiLevel = state.dpiLevel.next(),
                customDpiMultiplier = null
            )
            SettingsType.SCROLL_SPEED -> state.copy(
                scrollSensitivity = state.scrollSensitivity.next()
            )
        }

    private fun applySwapScrollMode(state: TouchpadState): TouchpadState {
        val next = if (state.scrollMode == ScrollMode.INFINITE_SCROLL) ScrollMode.NORMAL_SCROLL else ScrollMode.INFINITE_SCROLL
        return state.copy(scrollMode = next, lastScrollMode = next, customDpiMultiplier = null)
    }

    private fun applySetScrollMode(state: TouchpadState, mode: ScrollMode): TouchpadState =
        when (mode) {
            ScrollMode.OFF -> state.copy(scrollMode = ScrollMode.OFF)
            else -> state.copy(
                scrollMode = mode,
                lastScrollMode = mode,
                customDpiMultiplier = null
            )
        }

    private fun applySetModePreset(state: TouchpadState, index: Int): TouchpadState {
        val safeIndex = index.coerceIn(0, MODE_PRESETS.lastIndex)
        val preset = MODE_PRESETS[safeIndex]
        return state.copy(
            modePresetIndex = safeIndex,
            clickMode = preset.padModeState.clickMode,
            moveMode = preset.padModeState.moveMode,
            scrollMode = preset.padModeState.scrollMode,
            dpiLevel = preset.padModeState.dpi,
            dynamicsPresetIndex = preset.dynamicsPresetIndex,
            customDpiMultiplier = null
        )
    }
}
