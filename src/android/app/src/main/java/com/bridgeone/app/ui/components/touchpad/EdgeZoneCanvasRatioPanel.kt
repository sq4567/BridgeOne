package com.bridgeone.app.ui.components.touchpad

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bridgeone.app.ui.common.EdgeSwipeConstants
import com.bridgeone.app.ui.common.swipe.LocalSwipeFocused
import com.bridgeone.app.ui.common.swipe.SwipeFocusable

/** ResizeGuide 행 아이콘 강조색 (퍼플 계열 밝은 색). 기본값: 0xFFB39DDB */
private val RESIZE_GUIDE_ACCENT = Color(0xFFB39DDB)

/**
 * 비율 조정 모드의 조작법 안내 카드 (Phase 4.7.8-C 추출). 엣지 미선택 시 캔버스 중앙에 표시.
 * 두 조작법(경계 드래그 / 비율 프리셋)을 [아이콘] [용어] → [설명] 행으로 나열.
 */
@Composable
internal fun ResizeGuideCard() {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color.Black.copy(alpha = EdgeSwipeConstants.EDGE_ZONE_MODE_GUIDE_BG_ALPHA),
        // 양끝 엣지 스트립에 걸치지 않도록 좌우 여백 확보 (카드 가용 폭 축소)
        modifier = Modifier.padding(horizontal = 40.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
            ResizeGuideRow(Icons.Filled.SwapHoriz, "경계 드래그", "존 경계를 움직여 비율 조정")
            HorizontalDivider(
                color = Color.White.copy(alpha = EdgeSwipeConstants.EDGE_ZONE_MODE_DIVIDER_ALPHA),
                modifier = Modifier.padding(vertical = 6.dp),
            )
            ResizeGuideRow(Icons.Filled.Tune, "비율 프리셋", "엣지별 추천 비율 적용")
        }
    }
}

/** 경계 조작(MANIPULATION) 중 표시하는 조작법 안내 메시지 버블. */
@Composable
internal fun ManipulationGuideBubble() {
    Surface(shape = RoundedCornerShape(8.dp), color = Color.Black.copy(alpha = EdgeSwipeConstants.EDGE_ZONE_MODE_GUIDE_BG_ALPHA), modifier = Modifier.padding(horizontal = 32.dp)) {
        Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
            GuideBulletText(listOf("스와이프로 경계 이동", "탭으로 확정", "롱프레스로 되돌리기"))
        }
    }
}

/** 조작법 안내 카드의 한 행: 아이콘 · 굵은 용어 · → · 설명. */
@Composable
private fun ResizeGuideRow(icon: ImageVector, term: String, desc: String) {
    val accent = RESIZE_GUIDE_ACCENT
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(8.dp))
        Text(term, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(6.dp))
        Text("→", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
        Spacer(Modifier.width(6.dp))
        Text(desc, color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
    }
}

/**
 * 비율 프리셋 패널 (Phase 4.7.8-C 추출). 칩을 반투명 카드로 묶고 하단 구분선 아래에 취소 버튼을 통합한다.
 * 칩은 좌측 정렬. 세로 엣지(좌/우)는 칩을 세로 나열, 가로 엣지(상/하)는 [FlowRow]로 나열(오버플로 줄바꿈).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RatioPresetEdgeBar(
    edge: EntryEdge,
    presets: List<Pair<String, List<Float>>>,
    onApply: (List<Float>) -> Unit,
    onCancel: () -> Unit,
    previewedRatios: List<Float>? = null,
    maxWidth: Dp? = null,
) {
    val vertical = edge == EntryEdge.LEFT || edge == EntryEdge.RIGHT
    Surface(
        shape = RoundedCornerShape(EdgeSwipeConstants.EDGE_ZONE_MODE_CARD_CORNER_DP.dp),
        color = Color.Black.copy(alpha = 0.45f),
        modifier = if (maxWidth != null) Modifier.widthIn(max = maxWidth) else Modifier,
    ) {
        // 세로 엣지: 구분선(fillMaxWidth)이 패널을 화면 폭까지 늘리지 않도록 내용물 최대 폭에 맞춤.
        // → 칩 폭만큼만 차지하고 그 안에서 칩이 좌측 정렬됨.
        val columnWidthMod = if (vertical) Modifier.width(IntrinsicSize.Max) else Modifier
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(3.dp),
            modifier = columnWidthMod.padding(8.dp),
        ) {
            if (vertical) {
                // 각 칩을 패널 폭(IntrinsicSize.Max)에 맞춰 엣지 쪽으로 정렬 — 좌측 엣지는 좌측, 우측 엣지는 우측.
                val chipAlign = if (edge == EntryEdge.RIGHT) Alignment.CenterEnd else Alignment.CenterStart
                presets.forEach { (label, ratios) ->
                    Box(Modifier.fillMaxWidth(), contentAlignment = chipAlign) {
                        RatioPresetChip(label, ratios, onApply, armed = ratios == previewedRatios)
                    }
                }
            } else {
                // 가로 엣지(상/하): 칩을 가운데 정렬하고, 4개 이상이면 두 행에 균등 분배 (예: 5개 → 3+2)
                val maxPerRow = if (presets.size >= 4) (presets.size + 1) / 2 else presets.size
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    maxItemsInEachRow = maxPerRow,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    presets.forEach { (label, ratios) -> RatioPresetChip(label, ratios, onApply, armed = ratios == previewedRatios) }
                }
            }
            // NORMAL 미리보기 armed 상태: 한 번 더 눌러야 적용된다는 힌트
            if (previewedRatios != null) {
                Text(
                    "한 번 더 눌러 적용",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    modifier = Modifier.padding(start = 2.dp, top = 1.dp),
                )
            }
            HorizontalDivider(
                color = Color.White.copy(alpha = EdgeSwipeConstants.EDGE_ZONE_MODE_DIVIDER_ALPHA),
                modifier = Modifier.padding(vertical = 3.dp),
            )
            // 취소 버튼은 패널 폭 중앙 배치
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                PanelCancelButton(onCancel)
            }
        }
    }
}

/** 프리셋 패널 하단 취소 칩. 프리셋(퍼플)과 구분되도록 중립색을 사용하고 ✕ 아이콘을 곁들인다. */
@Composable
private fun PanelCancelButton(onCancel: () -> Unit) {
    val modeColor = CanvasModeKind.RESIZE.accentColor()
    SwipeFocusable(
        element = EdgeEditorElement.CanvasModeCancel,
        shape = RoundedCornerShape(8.dp),
        showBorderHighlight = false,
        onActivate = onCancel,
    ) {
        val focused = LocalSwipeFocused.current
        Surface(
            onClick = onCancel,
            shape = RoundedCornerShape(8.dp),
            color = if (focused) Color.White else Color.White.copy(alpha = 0.12f),
            contentColor = if (focused) modeColor else Color.White.copy(alpha = 0.85f),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                Text("취소", fontSize = 11.sp)
            }
        }
    }
}

/**
 * 비율 프리셋 칩 1개. NORMAL 탭(onClick) / SWIPE 포커스(SwipeFocusable) 모두 지원.
 * @param armed NORMAL 2단계 적용에서 현재 미리보기로 선택된 칩. 흰 테두리로 강조해 재탭 시 적용됨을 알린다.
 */
@Composable
private fun RatioPresetChip(
    label: String,
    ratios: List<Float>,
    onApply: (List<Float>) -> Unit,
    armed: Boolean = false,
) {
    val modeColor = CanvasModeKind.RESIZE.accentColor()
    val apply: () -> Unit = { onApply(ratios) }
    SwipeFocusable(
        element = EdgeEditorElement.CanvasRatioPreset(label),
        shape = RoundedCornerShape(8.dp),
        showBorderHighlight = false,
        onActivate = apply,
    ) {
        val focused = LocalSwipeFocused.current
        // Surface(onClick)은 최소 터치 타겟(48dp)을 강제해 짧은 라벨('균등')이 중앙으로 밀린다.
        // Modifier.clickable로 처리해 강제를 피하고 칩이 시각 크기 그대로 좌측 정렬되게 한다.
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (focused) Color.White else modeColor,
            contentColor = if (focused) modeColor else Color.White,
            border = if (armed && !focused) BorderStroke(2.dp, Color.White) else null,
            modifier = Modifier.clickable(onClick = apply),
        ) {
            Text(label, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
        }
    }
}
