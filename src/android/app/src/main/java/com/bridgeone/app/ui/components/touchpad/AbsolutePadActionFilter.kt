package com.bridgeone.app.ui.components.touchpad

// ============================================================
// AbsolutePointingPad(Page 3) 전용 엣지존 액션 화이트리스트 (Phase 4.9.3)
// ============================================================
//
// 절대좌표 패드는 델타(상대좌표) 벡터 연산이 성립하지 않으므로(축 잠금, DPI 배율,
// 스크롤 등) 좌표 무관 이산 액션만 노출한다. 도메인 단위가 아닌 액션 타입 단위로
// 필터링해 ToggleMode(CURSOR)가 ActionDomain.CLICK으로 매핑되는 함정을 회피한다.
//
// Reference: docs/development-plans/phase-4/phase-4-9-page3-absolute-pointing.md Phase 4.9.3

/**
 * 액션이 AbsolutePointingPad(Page 3)에서 허용되는지 판정합니다.
 *
 * 허용: SendMacro, SendShortcut, CyclePage, JumpToPage, SetClickMode,
 * ToggleMode(CLICK만), MouseHoldToggle, RestorePreviousMode.
 * 그 외(델타·스크롤·DPI·멀티커서·프리셋 계열)는 전부 배제.
 */
internal fun isAbsolutePadAllowed(action: EdgeZoneAction): Boolean = when (action) {
    is EdgeZoneAction.SendMacro -> true
    is EdgeZoneAction.SendShortcut -> true
    is EdgeZoneAction.CyclePage -> true
    is EdgeZoneAction.JumpToPage -> true
    is EdgeZoneAction.SetClickMode -> true
    is EdgeZoneAction.ToggleMode -> action.mode == EdgeSwipeMode.CLICK
    is EdgeZoneAction.MouseHoldToggle -> true
    EdgeZoneAction.RestorePreviousMode -> true
    else -> false
}

/**
 * 존 설정에서 [isAbsolutePadAllowed]를 통과하지 못하는 액션을 전부 Unassigned로 치환한
 * 새 config를 반환합니다. 원본 config는 변경하지 않습니다(저장·편집은 원본 사용).
 *
 * Rotation 트리거는 비허용 후보만 제거하고, 전 후보가 제거되면 Unassigned SingleAction으로
 * 대체합니다.
 */
internal fun filterConfigForAbsolutePad(config: EdgeZoneConfig): EdgeZoneConfig {
    fun filterZone(zone: EdgeZone): EdgeZone {
        val filteredTrigger: EdgeZoneTrigger = when (val trigger = zone.trigger) {
            is EdgeZoneTrigger.SingleAction ->
                if (isAbsolutePadAllowed(trigger.action)) trigger
                else EdgeZoneTrigger.SingleAction(EdgeZoneAction.Unassigned, "", "")
            is EdgeZoneTrigger.Rotation -> {
                val allowedCandidates = trigger.candidates.filter { isAbsolutePadAllowed(it.action) }
                if (allowedCandidates.isEmpty()) EdgeZoneTrigger.SingleAction(EdgeZoneAction.Unassigned, "", "")
                else trigger.copy(candidates = allowedCandidates)
            }
        }
        return zone.copy(trigger = filteredTrigger)
    }

    return config.copy(
        topZones = config.topZones.map(::filterZone),
        bottomZones = config.bottomZones.map(::filterZone),
        leftZones = config.leftZones.map(::filterZone),
        rightZones = config.rightZones.map(::filterZone),
    )
}
