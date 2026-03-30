package com.bridgeone.app.ui.common

import com.bridgeone.app.ui.components.touchpad.DynamicsAlgorithm
import com.bridgeone.app.ui.components.touchpad.PointerDynamicsPreset

/**
 * 포인터 다이나믹스 프리셋 목록 (Phase 4.3.8)
 *
 * 프리셋 추가/수정/삭제는 이 파일만 변경하면 전체에 반영됩니다.
 * 3~5개 권장. 인덱스 0은 항상 가속 없음(Off)으로 유지하세요.
 */
val DYNAMICS_PRESETS: List<PointerDynamicsPreset> = listOf(
    PointerDynamicsPreset(
        name = "Off",
        algorithm = DynamicsAlgorithm.NONE,
        intensityFactor = 1.0f,
        velocityThresholdDpMs = 0.5f,
        maxMultiplier = 1.0f,
        icon = AppIcons.DynamicsOff,
        description = "속도와 관계없이 일정하게 이동"
    ),
    PointerDynamicsPreset(
        name = "Precision",
        algorithm = DynamicsAlgorithm.WINDOWS_EPP,
        intensityFactor = 0.8f,
        velocityThresholdDpMs = 0.6f,
        maxMultiplier = 2.5f,
        icon = AppIcons.DynamicsPrecision,
        description = "느리면 그대로, 빠르면 약한 S커브 가속"
    ),
    PointerDynamicsPreset(
        name = "Standard",
        algorithm = DynamicsAlgorithm.WINDOWS_EPP,
        intensityFactor = 1.2f,
        velocityThresholdDpMs = 0.5f,
        maxMultiplier = 3.0f,
        icon = AppIcons.DynamicsStandard,
        description = "Windows EPP와 유사한 자연스러운 가속"
    ),
    PointerDynamicsPreset(
        name = "Fast",
        algorithm = DynamicsAlgorithm.LINEAR,
        intensityFactor = 1.5f,
        velocityThresholdDpMs = 0.4f,
        maxMultiplier = 4.0f,
        icon = AppIcons.DynamicsFast,
        description = "속도에 비례하여 커서 이동량 선형 증가"
    ),
)

/** 기본 프리셋 인덱스 (Off). TouchpadState.dynamicsPresetIndex 초기값과 일치해야 합니다. */
const val DEFAULT_PRESET_INDEX = 0
