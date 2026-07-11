package com.bridgeone.app.ui.components.touchpad

/**
 * 액션 도메인 분류. ActionDomainPicker의 폴더 트리/그리드 그룹핑과
 * 현재 액션의 도메인 판정에 사용된다.
 */
enum class ActionDomain {
    CLICK, SCROLL, MOVE, DPI, SCROLL_SPEED, DYNAMICS, MODE_PRESET, HISTORY, COMBO, MACRO, MOUSE_HOLD, PAGE, MULTI_CURSOR, ABSOLUTE_MODE, UNASSIGNED
}

/**
 * EdgeZoneEditorScreen에서 분리한 순수 함수 모음 (Phase 4.7.5-A).
 * 모두 클로저 캡처가 없는 순수 함수로, Composable 상태와 무관하게 단위 테스트 가능하다.
 */
internal object EdgeZoneActionResolver {

    /** 액션이 속한 도메인을 반환한다. */
    fun domainOf(action: EdgeZoneAction): ActionDomain = when (action) {
        is EdgeZoneAction.ToggleMode -> when (action.mode) {
            EdgeSwipeMode.CLICK -> ActionDomain.CLICK
            EdgeSwipeMode.SCROLL -> ActionDomain.SCROLL
            EdgeSwipeMode.MOVE -> ActionDomain.MOVE
            EdgeSwipeMode.DPI -> ActionDomain.DPI
            EdgeSwipeMode.SCROLL_SPEED -> ActionDomain.SCROLL_SPEED
            EdgeSwipeMode.DYNAMICS -> ActionDomain.DYNAMICS
            EdgeSwipeMode.CURSOR -> ActionDomain.CLICK
        }
        is EdgeZoneAction.SetClickMode -> ActionDomain.CLICK
        is EdgeZoneAction.SetScrollMode -> ActionDomain.SCROLL
        is EdgeZoneAction.SetMoveMode -> ActionDomain.MOVE
        is EdgeZoneAction.SetDpi -> ActionDomain.DPI
        is EdgeZoneAction.OpenSettings -> when (action.settingsType) {
            SettingsType.DPI -> ActionDomain.DPI
            SettingsType.SCROLL_SPEED -> ActionDomain.SCROLL_SPEED
        }
        is EdgeZoneAction.SetScrollSpeed -> ActionDomain.SCROLL_SPEED
        is EdgeZoneAction.CyclePreset -> when (action.presetType) {
            PresetType.DYNAMICS -> ActionDomain.DYNAMICS
            PresetType.MODE -> ActionDomain.MODE_PRESET
        }
        is EdgeZoneAction.SetDynamicsPreset -> ActionDomain.DYNAMICS
        is EdgeZoneAction.SetModePreset -> ActionDomain.MODE_PRESET
        EdgeZoneAction.SwapScrollMode -> ActionDomain.SCROLL
        is EdgeZoneAction.SetCustomDpi -> ActionDomain.DPI
        is EdgeZoneAction.SetCustomScrollSpeed -> ActionDomain.SCROLL_SPEED
        EdgeZoneAction.RestorePreviousMode -> ActionDomain.HISTORY
        is EdgeZoneAction.SendShortcut -> ActionDomain.COMBO
        is EdgeZoneAction.SendMacro -> ActionDomain.MACRO
        is EdgeZoneAction.MouseHoldToggle -> ActionDomain.MOUSE_HOLD
        is EdgeZoneAction.CyclePage -> ActionDomain.PAGE
        is EdgeZoneAction.JumpToPage -> ActionDomain.PAGE
        EdgeZoneAction.ToggleMultiCursor -> ActionDomain.MULTI_CURSOR
        EdgeZoneAction.ToggleMultiCursorLayout -> ActionDomain.MULTI_CURSOR
        is EdgeZoneAction.SetCursorCount -> ActionDomain.MULTI_CURSOR
        is EdgeZoneAction.ActivatePad -> ActionDomain.MULTI_CURSOR
        is EdgeZoneAction.CyclePad -> ActionDomain.MULTI_CURSOR
        EdgeZoneAction.ToggleAbsoluteZoom -> ActionDomain.ABSOLUTE_MODE
        EdgeZoneAction.ToggleAbsoluteDrag -> ActionDomain.ABSOLUTE_MODE
        EdgeZoneAction.Unassigned -> ActionDomain.UNASSIGNED
    }

    /** 두 액션이 (선택 표시 목적상) 동일한 액션인지 비교한다. */
    fun actionEquals(a: EdgeZoneAction, b: EdgeZoneAction): Boolean = when {
        a is EdgeZoneAction.Unassigned && b is EdgeZoneAction.Unassigned -> true
        a is EdgeZoneAction.ToggleMode && b is EdgeZoneAction.ToggleMode -> a.mode == b.mode
        a is EdgeZoneAction.CyclePreset && b is EdgeZoneAction.CyclePreset -> a.presetType == b.presetType
        a is EdgeZoneAction.OpenSettings && b is EdgeZoneAction.OpenSettings -> a.settingsType == b.settingsType
        a is EdgeZoneAction.SetDpi && b is EdgeZoneAction.SetDpi -> a.level == b.level
        a is EdgeZoneAction.SetScrollSpeed && b is EdgeZoneAction.SetScrollSpeed -> a.sensitivity == b.sensitivity
        a is EdgeZoneAction.SetModePreset && b is EdgeZoneAction.SetModePreset -> a.index == b.index
        a is EdgeZoneAction.SetDynamicsPreset && b is EdgeZoneAction.SetDynamicsPreset -> a.index == b.index
        a is EdgeZoneAction.SetClickMode && b is EdgeZoneAction.SetClickMode -> a.mode == b.mode
        a is EdgeZoneAction.SetMoveMode && b is EdgeZoneAction.SetMoveMode -> a.mode == b.mode
        a is EdgeZoneAction.SetScrollMode && b is EdgeZoneAction.SetScrollMode -> a.mode == b.mode
        a is EdgeZoneAction.SwapScrollMode && b is EdgeZoneAction.SwapScrollMode -> true
        a is EdgeZoneAction.SetCustomDpi && b is EdgeZoneAction.SetCustomDpi -> true
        a is EdgeZoneAction.SetCustomScrollSpeed && b is EdgeZoneAction.SetCustomScrollSpeed -> true
        a is EdgeZoneAction.RestorePreviousMode && b is EdgeZoneAction.RestorePreviousMode -> true
        // SendShortcut: 프리셋은 label로 비교, 커스텀은 mod+key 조합으로 비교
        a is EdgeZoneAction.SendShortcut && b is EdgeZoneAction.SendShortcut ->
            if (a.presetLabel.isNotEmpty() && b.presetLabel.isNotEmpty()) a.presetLabel == b.presetLabel
            else a.modifierBits == b.modifierBits && a.keyCodes == b.keyCodes
        a is EdgeZoneAction.MouseHoldToggle && b is EdgeZoneAction.MouseHoldToggle -> a.button == b.button && a.mode == b.mode
        a is EdgeZoneAction.CyclePage && b is EdgeZoneAction.CyclePage -> a.direction == b.direction
        a is EdgeZoneAction.JumpToPage && b is EdgeZoneAction.JumpToPage -> a.pageIndex == b.pageIndex
        a is EdgeZoneAction.ToggleMultiCursor && b is EdgeZoneAction.ToggleMultiCursor -> true
        a is EdgeZoneAction.ToggleMultiCursorLayout && b is EdgeZoneAction.ToggleMultiCursorLayout -> true
        a is EdgeZoneAction.SetCursorCount && b is EdgeZoneAction.SetCursorCount -> a.count == b.count
        a is EdgeZoneAction.ActivatePad && b is EdgeZoneAction.ActivatePad -> a.index == b.index
        a is EdgeZoneAction.CyclePad && b is EdgeZoneAction.CyclePad -> a.direction == b.direction
        a is EdgeZoneAction.ToggleAbsoluteZoom && b is EdgeZoneAction.ToggleAbsoluteZoom -> true
        a is EdgeZoneAction.ToggleAbsoluteDrag && b is EdgeZoneAction.ToggleAbsoluteDrag -> true
        else -> false
    }

    /**
     * 두 config를 비교해 `from → to` 사이에 무엇이 바뀌었는지 한 줄 설명 반환.
     */
    fun describeUndoStep(from: EdgeZoneConfig, to: EdgeZoneConfig): String {
        if (from.cornerPriority != to.cornerPriority) return "코너 우선순위 변경"
        for (edge in EntryEdge.entries) {
            val f = from.zonesFor(edge)
            val t = to.zonesFor(edge)
            if (f == t) continue
            val edgeName = when (edge) {
                EntryEdge.TOP    -> "상단"
                EntryEdge.BOTTOM -> "하단"
                EntryEdge.LEFT   -> "좌측"
                EntryEdge.RIGHT  -> "우측"
            }
            if (t.size > f.size) return "$edgeName 존 분할"
            if (t.size < f.size) return "$edgeName 존 병합/삭제"
            // 순서 재정렬 감지: 존 개수 동일 + 트리거 멀티셋 동일이지만 순서가 다름
            run {
                val fTriggers = f.map { it.trigger }
                val tTriggers = t.map { it.trigger }
                if (fTriggers != tTriggers &&
                    fTriggers.groupingBy { it }.eachCount() == tTriggers.groupingBy { it }.eachCount()
                ) {
                    val ratiosSame = f.indices.all {
                        f[it].startRatio == t[it].startRatio && f[it].endRatio == t[it].endRatio
                    }
                    return if (ratiosSame) "$edgeName 액션 교환" else "$edgeName 존 순서 변경"
                }
            }
            for (i in f.indices) {
                val fz = f[i]; val tz = t[i]
                if (fz.startRatio != tz.startRatio || fz.endRatio != tz.endRatio) return "$edgeName 비율 조정"
                if (fz.label != tz.label) {
                    val newLabel = tz.label.ifEmpty { "(없음)" }
                    return "$edgeName 라벨 → \"$newLabel\""
                }
                if (fz.iconKey != tz.iconKey) return "$edgeName 아이콘 변경"
                if (fz.trigger != tz.trigger) {
                    val ft = fz.trigger; val tt = tz.trigger
                    return when {
                        ft is EdgeZoneTrigger.SingleAction && tt is EdgeZoneTrigger.SingleAction ->
                            "$edgeName 액션 → ${tt.action.displayName()}"
                        tt is EdgeZoneTrigger.Rotation -> "$edgeName 액션 순환 설정"
                        ft is EdgeZoneTrigger.Rotation -> "$edgeName 단일 액션으로 변경"
                        else -> "$edgeName 트리거 변경"
                    }
                }
            }
        }
        return "설정 변경"
    }

    /**
     * 커스텀 다이나믹스 프리셋 삭제 후, 삭제된 글로벌 인덱스를 참조하던 액션을 보정한다.
     *
     * @param config           보정할 설정
     * @param removedGlobalIndex  `DYNAMICS_PRESETS.size + customPresets.indexOfFirst{it.id==deleted.id}` 값
     */
    fun migrateDynamicsIndicesAfterDelete(
        config: EdgeZoneConfig,
        removedGlobalIndex: Int,
    ): EdgeZoneConfig {
        fun migrateAction(action: EdgeZoneAction): EdgeZoneAction = when {
            action is EdgeZoneAction.SetDynamicsPreset && action.index == removedGlobalIndex -> EdgeZoneAction.Unassigned
            action is EdgeZoneAction.SetDynamicsPreset && action.index > removedGlobalIndex -> EdgeZoneAction.SetDynamicsPreset(action.index - 1)
            else -> action
        }

        fun migrateTrigger(trigger: EdgeZoneTrigger): EdgeZoneTrigger = when (trigger) {
            is EdgeZoneTrigger.SingleAction -> trigger.copy(action = migrateAction(trigger.action))
            is EdgeZoneTrigger.Rotation -> trigger.copy(
                candidates = trigger.candidates.map { c -> c.copy(action = migrateAction(c.action)) }
            )
        }

        fun migrateZone(zone: EdgeZone): EdgeZone = zone.copy(trigger = migrateTrigger(zone.trigger))

        return config.copy(
            topZones = config.topZones.map(::migrateZone),
            bottomZones = config.bottomZones.map(::migrateZone),
            leftZones = config.leftZones.map(::migrateZone),
            rightZones = config.rightZones.map(::migrateZone),
        )
    }

    /** 존 개수 n에 대한 비율 프리셋 목록(이름 → 비율 리스트). 비율의 합은 항상 1.0. */
    fun ratioPresetsFor(n: Int): List<Pair<String, List<Float>>> = buildList {
        add("균등" to List(n) { 1f / n })
        val startHeavy: List<Float>? = when (n) {
            2 -> listOf(0.65f, 0.35f)
            3 -> listOf(0.50f, 0.30f, 0.20f)
            4 -> listOf(0.40f, 0.30f, 0.20f, 0.10f)
            5 -> listOf(0.30f, 0.25f, 0.20f, 0.15f, 0.10f)
            else -> null
        }
        if (startHeavy != null) {
            add("왼쪽 크게" to startHeavy)
            add("오른쪽 크게" to startHeavy.reversed())
        }
        when (n) {
            3 -> {
                add("양 끝 크게" to listOf(0.40f, 0.20f, 0.40f))
                add("가운데 크게" to listOf(0.20f, 0.60f, 0.20f))
            }
            4 -> {
                add("양 끝 크게" to listOf(0.35f, 0.15f, 0.15f, 0.35f))
                add("가운데 크게" to listOf(0.15f, 0.35f, 0.35f, 0.15f))
            }
            5 -> {
                add("양 끝 크게" to listOf(0.30f, 0.13f, 0.14f, 0.13f, 0.30f))
                add("가운데 크게" to listOf(0.20f, 0.10f, 0.40f, 0.10f, 0.20f))
            }
        }
    }
}
