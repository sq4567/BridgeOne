package com.bridgeone.app.ui.common

import com.bridgeone.app.ui.components.touchpad.ClickMode
import com.bridgeone.app.ui.components.touchpad.DpiLevel
import com.bridgeone.app.ui.components.touchpad.ModePreset
import com.bridgeone.app.ui.components.touchpad.MoveMode
import com.bridgeone.app.ui.components.touchpad.PadModeState
import com.bridgeone.app.ui.components.touchpad.ScrollMode

/**
 * 모드 프리셋 목록 (Phase 4.4.8)
 *
 * 프리셋 추가/수정/삭제는 이 파일만 변경하면 전체에 반영됩니다.
 * 인덱스 0은 항상 기본(Standard)으로 유지하세요.
 */
val MODE_PRESETS: List<ModePreset> = listOf(
    ModePreset(
        name = "Standard",
        icon = AppIcons.ModePresetStandard,
        description = "기본 설정",
        padModeState = PadModeState(),
        dynamicsPresetIndex = 0  // Off
    ),
    ModePreset(
        name = "Precise",
        icon = AppIcons.ModePresetPrecise,
        description = "저DPI + 직각 이동 — 정밀 작업용",
        padModeState = PadModeState(
            clickMode = ClickMode.LEFT_CLICK,
            moveMode = MoveMode.RIGHT_ANGLE,
            scrollMode = ScrollMode.OFF,
            dpi = DpiLevel.LOW
        ),
        dynamicsPresetIndex = 1  // Precision
    ),
    ModePreset(
        name = "Fast",
        icon = AppIcons.ModePresetFast,
        description = "고DPI + 자유 이동 — 빠른 탐색용",
        padModeState = PadModeState(
            clickMode = ClickMode.LEFT_CLICK,
            moveMode = MoveMode.FREE,
            scrollMode = ScrollMode.OFF,
            dpi = DpiLevel.HIGH
        ),
        dynamicsPresetIndex = 3  // Fast
    ),
)

/** 기본 모드 프리셋 인덱스 (Standard). TouchpadState.modePresetIndex 초기값과 일치해야 합니다. */
const val DEFAULT_MODE_PRESET_INDEX = 0
