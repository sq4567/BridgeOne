package com.bridgeone.app.ui.common

import com.bridgeone.app.ui.components.touchpad.EdgeZone
import com.bridgeone.app.ui.components.touchpad.EdgeZoneAction
import com.bridgeone.app.ui.components.touchpad.EdgeZoneConfig
import com.bridgeone.app.ui.components.touchpad.EdgeZoneTrigger
import com.bridgeone.app.ui.components.touchpad.EntryEdge

/**
 * 엣지 존 구조(레이아웃) 프리셋 데이터 클래스 (Phase 4.6.3)
 *
 * 프리셋은 터치패드의 존 분할 방식(어떤 엣지를 몇 개로 나눌지)만 결정한다.
 * 각 존의 액션 할당은 프리셋에 포함되지 않으며, 프리셋 적용 시 모든 존은 미할당(Unassigned)으로 초기화된다.
 *
 * @param id          고유 식별자. 빌트인은 "builtin_*", 커스텀은 UUID 문자열
 * @param name        사용자에게 표시되는 이름
 * @param description 한 줄 설명
 * @param iconKey     IconRegistry 키
 * @param config      적용할 EdgeZoneConfig (모든 존은 Unassigned 트리거)
 */
data class EdgeZonePreset(
    val id: String,
    val name: String,
    val description: String,
    val iconKey: String,
    val config: EdgeZoneConfig
)

// ============================================================
// 구조 프리셋 빌더 헬퍼
// ============================================================

/** 미할당(Unassigned) 단일 액션 트리거 */
private fun unassigned() = EdgeZoneTrigger.SingleAction(EdgeZoneAction.Unassigned, "", "")

/**
 * 엣지를 n등분 균등 분할한 존 목록을 반환한다. 모든 존은 미할당 트리거.
 * n <= 0 이면 빈 리스트(엣지 미사용)를 반환한다.
 */
private fun splitEdge(edge: EntryEdge, n: Int): List<EdgeZone> {
    if (n <= 0) return emptyList()
    return List(n) { i ->
        EdgeZone(
            edge = edge,
            startRatio = i.toFloat() / n,
            endRatio = (i + 1).toFloat() / n,
            trigger = unassigned()
        )
    }
}

/**
 * 4엣지 분할 수로 EdgeZoneConfig를 생성한다. 기본 cornerPriority 유지.
 * top/bottom/left/right 에 0 이하를 넣으면 해당 엣지가 비활성(빈 리스트)이 된다.
 */
private fun layoutConfig(top: Int, bottom: Int, left: Int, right: Int): EdgeZoneConfig =
    EdgeZoneConfig(
        topZones    = splitEdge(EntryEdge.TOP,    top),
        bottomZones = splitEdge(EntryEdge.BOTTOM, bottom),
        leftZones   = splitEdge(EntryEdge.LEFT,   left),
        rightZones  = splitEdge(EntryEdge.RIGHT,  right)
    )

/**
 * 이 EdgeZoneConfig의 모든 존 trigger를 미할당(Unassigned)으로 초기화한 새 config를 반환한다.
 * 엣지별 존 개수·비율·코너 우선순위는 그대로 보존된다. 빈 엣지(emptyList)도 그대로 유지.
 */
fun EdgeZoneConfig.stripActions(): EdgeZoneConfig = copy(
    topZones    = topZones.map    { it.copy(trigger = unassigned()) },
    bottomZones = bottomZones.map { it.copy(trigger = unassigned()) },
    leftZones   = leftZones.map   { it.copy(trigger = unassigned()) },
    rightZones  = rightZones.map  { it.copy(trigger = unassigned()) }
)

// ============================================================
// 빌트인 구조 프리셋 목록
// ============================================================

val BUILT_IN_EDGE_ZONE_PRESETS: List<EdgeZonePreset> = listOf(

    EdgeZonePreset(
        id = "builtin_default",
        name = "기본",
        description = "위 2칸, 아래 1칸, 좌·우 각 2칸",
        iconKey = "Tune",
        config = EdgeZoneConfig.default().stripActions()
    ),

    EdgeZonePreset(
        id = "builtin_single",
        name = "각 1존",
        description = "네 엣지를 각각 하나의 존으로",
        iconKey = "TouchApp",
        config = layoutConfig(top = 1, bottom = 1, left = 1, right = 1)
    ),

    EdgeZonePreset(
        id = "builtin_bisect",
        name = "각 2분할",
        description = "네 엣지를 각각 둘로 나눔",
        iconKey = "SwapVert",
        config = layoutConfig(top = 2, bottom = 2, left = 2, right = 2)
    ),

    EdgeZonePreset(
        id = "builtin_trisect",
        name = "각 3분할",
        description = "네 엣지를 각각 셋으로 나눔",
        iconKey = "Adjust",
        config = layoutConfig(top = 3, bottom = 3, left = 3, right = 3)
    ),

    EdgeZonePreset(
        id = "builtin_horizontal",
        name = "상하만",
        description = "위·아래 엣지만 사용, 좌·우 미사용",
        iconKey = "Remove",
        config = layoutConfig(top = 1, bottom = 1, left = 0, right = 0)
    ),

    EdgeZonePreset(
        id = "builtin_vertical",
        name = "좌우만",
        description = "좌·우 엣지만 사용, 위·아래 미사용",
        iconKey = "PanTool",
        config = layoutConfig(top = 0, bottom = 0, left = 1, right = 1)
    ),

    EdgeZonePreset(
        id = "builtin_no_top",
        name = "위 미사용",
        description = "위 엣지를 사용하지 않고 나머지 3면만 사용",
        iconKey = "ArrowDownward",
        config = layoutConfig(top = 0, bottom = 1, left = 1, right = 1)
    )
)
