package com.bridgeone.app.ui.common

import com.bridgeone.app.ui.components.touchpad.EdgeZoneConfig

/**
 * 특정 터치패드에 할당된 엣지 존 설정.
 * EdgeZonePresetsRepository의 프리셋 *정의* 라이브러리와 별개로,
 * 각 터치패드가 현재 어떤 config/presetId를 *적용*하고 있는지 표현한다.
 */
data class TouchpadEdgeZoneAssignment(
    val config: EdgeZoneConfig,
    /** 현재 config가 일치하는 프리셋 ID. null = 직접 편집하여 프리셋과 불일치 */
    val presetId: String?
) {
    /**
     * 제어 버튼과 겹쳐 TOP 엣지를 쓸 수 없는 터치패드용. 사용자가 한 번도 커스텀 편집하지
     * 않은(builtin_default) 경우에만 TOP 존을 비운다 — 이미 편집해 저장한 값은 건드리지 않는다.
     */
    fun withTopClearedIfDefault(): TouchpadEdgeZoneAssignment {
        if (presetId != "builtin_default" || config.topZones.isEmpty()) return this
        return copy(config = config.copy(topZones = emptyList()))
    }

    companion object {
        fun default() = TouchpadEdgeZoneAssignment(
            config = EdgeZoneConfig.default(),
            presetId = "builtin_default"
        )
    }
}
