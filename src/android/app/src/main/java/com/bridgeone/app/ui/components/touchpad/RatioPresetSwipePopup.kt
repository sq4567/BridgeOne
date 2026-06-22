package com.bridgeone.app.ui.components.touchpad

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bridgeone.app.ui.common.EdgeSwipeConstants
import com.bridgeone.app.ui.common.swipe.LocalSwipeFocused
import com.bridgeone.app.ui.common.swipe.SwipeFocusable

/**
 * SWIPE 모드 전용 비율 프리셋 가로 서랍 팝업.
 * 호출부([EdgeZoneOverlayLayer])가 인라인 렌더 + 위치/애니메이션을 담당하며,
 * 이 Composable은 Surface + Row 본문만 렌더한다.
 * 항목은 [EdgeEditorScope.RatioPresetMenu] scope의 SwipeFocusable로 등록됨.
 * 가로 배열이므로 gridRow=0 고정, gridCol=presetIdx 사용.
 *
 * LazyRow 대신 일반 Row + horizontalScroll 사용:
 * - LazyRow는 화면 밖 항목을 compose하지 않아 SwipeFocusable이 미등록됨 → 스와이프 이동 불가
 * - 일반 Row는 모든 항목이 항상 compose되어 스와이프 컨트롤러가 전체 항목을 인식
 * - BringIntoViewRequester로 포커스 항목을 자동 스크롤해 화면에 표시
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun RatioPresetSwipePopup(
    presets: List<Pair<String, List<Float>>>,
    onSelect: (List<Float>) -> Unit,
    maxWidthDp: Dp = Dp.Unspecified,
) {
    val cs = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()
    Surface(
        shape = RoundedCornerShape(EdgeSwipeConstants.RATIO_DRAWER_CORNER_RADIUS_DP.dp),
        color = cs.surfaceVariant,
        shadowElevation = EdgeSwipeConstants.RATIO_DRAWER_ELEVATION_DP.dp,
        tonalElevation = EdgeSwipeConstants.RATIO_DRAWER_ELEVATION_DP.dp,
    ) {
        Row(
            // 메뉴 전체(손잡이 + 항목)를 maxWidthDp로 제한 → 우측이 헤더 영역 오른쪽 끝에 정렬되고 넘침 방지
            modifier = (if (maxWidthDp != Dp.Unspecified)
                Modifier.widthIn(max = maxWidthDp)
            else
                Modifier)
                .padding(EdgeSwipeConstants.RATIO_DRAWER_CONTENT_PADDING_DP.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RatioDrawerHandle()
            // 항목은 horizontalScroll 영역(손잡이는 고정). 항목이 화면 밖이면 BringIntoViewRequester가 스크롤한다.
            Row(
                modifier = Modifier.horizontalScroll(scrollState, enabled = false),
                horizontalArrangement = Arrangement.spacedBy(EdgeSwipeConstants.RATIO_DRAWER_ITEM_SPACING_DP.dp),
            ) {
                presets.forEachIndexed { presetIdx, (label, ratios) ->
                    val bringIntoViewRequester = remember { BringIntoViewRequester() }
                    SwipeFocusable(
                        element = EdgeEditorElement.RatioPresetItem(label),
                        scope = EdgeEditorScope.RatioPresetMenu,
                        shape = RoundedCornerShape(EdgeSwipeConstants.RATIO_DRAWER_ITEM_CORNER_RADIUS_DP.dp),
                        showBorderHighlight = false,
                        onActivate = { onSelect(ratios) },
                        gridRow = 0,
                        gridCol = presetIdx,
                        modifier = Modifier.bringIntoViewRequester(bringIntoViewRequester),
                    ) {
                        val focused = LocalSwipeFocused.current
                        LaunchedEffect(focused) {
                            if (focused) bringIntoViewRequester.bringIntoView()
                        }
                        RatioPresetDrawerItem(
                            label = label,
                            ratios = ratios,
                            focused = focused,
                            pending = false,
                        )
                    }
                }
            }
        }
    }
}

/**
 * 비율 프리셋 서랍 좌측 손잡이 (NORMAL/SWIPE 공용).
 * 서랍을 좌→우로 당겨 여는 시각적 어포던스용 세로 그립 막대.
 */
@Composable
internal fun RatioDrawerHandle(modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = modifier.padding(horizontal = EdgeSwipeConstants.RATIO_DRAWER_HANDLE_PADDING_DP.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(
                    width = EdgeSwipeConstants.RATIO_DRAWER_HANDLE_WIDTH_DP.dp,
                    height = EdgeSwipeConstants.RATIO_DRAWER_HANDLE_HEIGHT_DP.dp,
                )
                .clip(RoundedCornerShape(EdgeSwipeConstants.RATIO_DRAWER_HANDLE_CORNER_DP.dp))
                .background(cs.onSurfaceVariant.copy(alpha = EdgeSwipeConstants.RATIO_DRAWER_HANDLE_ALPHA))
        )
    }
}

/**
 * 비율 프리셋 서랍의 개별 항목 (NORMAL/SWIPE 공용).
 * [focused]: SWIPE 포커스 강조. [pending]: NORMAL 1탭 미리보기 강조.
 */
@Composable
internal fun RatioPresetDrawerItem(
    label: String,
    ratios: List<Float>,
    focused: Boolean,
    pending: Boolean,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val itemShape = RoundedCornerShape(EdgeSwipeConstants.RATIO_DRAWER_ITEM_CORNER_RADIUS_DP.dp)
    Row(
        modifier = modifier
            .clip(itemShape)
            .background(
                when {
                    focused -> cs.primary.copy(alpha = 0.18f)
                    pending -> cs.tertiary.copy(alpha = 0.18f)
                    else -> cs.background
                }
            )
            .border(
                width = EdgeSwipeConstants.RATIO_DRAWER_ITEM_BORDER_WIDTH_DP.dp,
                color = when {
                    focused -> cs.primary.copy(alpha = EdgeSwipeConstants.RATIO_DRAWER_ITEM_BORDER_ALPHA)
                    pending -> cs.tertiary.copy(alpha = EdgeSwipeConstants.RATIO_DRAWER_ITEM_BORDER_ALPHA)
                    else -> cs.outline.copy(alpha = EdgeSwipeConstants.RATIO_DRAWER_ITEM_BORDER_ALPHA)
                },
                shape = itemShape,
            )
            .padding(
                horizontal = EdgeSwipeConstants.RATIO_DRAWER_ITEM_PADDING_HORIZONTAL_DP.dp,
                vertical = EdgeSwipeConstants.RATIO_DRAWER_ITEM_PADDING_VERTICAL_DP.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(EdgeSwipeConstants.RATIO_DRAWER_ITEM_SPACING_DP.dp),
    ) {
        MiniRatioBar(
            ratios = ratios,
            modifier = Modifier
                .width(EdgeSwipeConstants.RATIO_DRAWER_ITEM_MINI_BAR_WIDTH_DP.dp)
                .height(EdgeSwipeConstants.RATIO_DRAWER_ITEM_MINI_BAR_HEIGHT_DP.dp),
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = when {
                focused -> cs.primary
                pending -> cs.tertiary
                else -> cs.onSurface
            },
        )
    }
}

@Composable
internal fun MiniRatioBar(
    ratios: List<Float>,
    modifier: Modifier = Modifier
) {
    val colors = listOf(
        Color(0xFF1E3A5F), Color(0xFF3A1E5F), Color(0xFF1E5F3A),
        Color(0xFF5F3A1E)
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        var x = 0f
        ratios.forEachIndexed { i, r ->
            val segW = r * w
            drawRect(
                color = colors[i % colors.size].copy(alpha = 0.85f),
                topLeft = Offset(x, 0f),
                size = Size((segW - 1f).coerceAtLeast(0f), h)
            )
            x += segW
        }
    }
}

/**
 * NORMAL 모드 비율 프리셋 가로 서랍 Popup 콘텐츠.
 * RowScope 외부에 선언해 AnimatedVisibility 수신자 해석 오염을 방지한다.
 * 호출부(ZoneRatioSection)의 Popup 람다에서 호출됨.
 */
@Composable
internal fun RatioPresetNormalDrawerContent(
    visibleState: MutableTransitionState<Boolean>,
    presets: List<Pair<String, List<Float>>>,
    maxWidthDp: androidx.compose.ui.unit.Dp,
    cs: ColorScheme,
    pendingPreviewIdx: Int?,
    onItemTap: (idx: Int, ratios: List<Float>) -> Unit,
) {
    AnimatedVisibility(
        visibleState = visibleState,
        // 왼쪽 손잡이를 기준점으로 좌→우 펼침(enter) / 우→좌 접힘(exit)
        enter = expandHorizontally(
            expandFrom = Alignment.Start,
            animationSpec = tween(EdgeSwipeConstants.RATIO_DRAWER_OPEN_DURATION_MS),
        ) + fadeIn(tween(EdgeSwipeConstants.RATIO_DRAWER_OPEN_DURATION_MS)),
        // exit는 fade 없이 shrink만 (페이드 아웃이 shrink를 가려 닫기 애니가 안 보였음)
        exit = shrinkHorizontally(
            shrinkTowards = Alignment.Start,
            animationSpec = tween(EdgeSwipeConstants.RATIO_DRAWER_CLOSE_DURATION_MS),
        ),
    ) {
        Surface(
            shape = RoundedCornerShape(EdgeSwipeConstants.RATIO_DRAWER_CORNER_RADIUS_DP.dp),
            color = cs.surfaceVariant,
            shadowElevation = EdgeSwipeConstants.RATIO_DRAWER_ELEVATION_DP.dp,
            tonalElevation = EdgeSwipeConstants.RATIO_DRAWER_ELEVATION_DP.dp,
        ) {
            Row(
                // 메뉴 전체(손잡이 + 항목)를 maxWidthDp로 제한 → 우측이 헤더 영역 오른쪽 끝에 정렬되고 넘침 방지
                modifier = Modifier
                    .widthIn(max = maxWidthDp)
                    .padding(EdgeSwipeConstants.RATIO_DRAWER_CONTENT_PADDING_DP.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RatioDrawerHandle()
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(EdgeSwipeConstants.RATIO_DRAWER_ITEM_SPACING_DP.dp),
                    userScrollEnabled = true,
                ) {
                    itemsIndexed(presets) { idx, (label, ratios) ->
                        RatioPresetDrawerItem(
                            label = label,
                            ratios = ratios,
                            focused = false,
                            pending = pendingPreviewIdx == idx,
                            modifier = Modifier.clickable { onItemTap(idx, ratios) },
                        )
                    }
                }
            }
        }
    }
}
