package com.bridgeone.app.ui.components.touchpad

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Row
import com.bridgeone.app.R
import com.bridgeone.app.ui.common.EdgeSwipeConstants

// ============================================================
// 진입 엣지
// ============================================================

/** 엣지 스와이프 팝업 조작 방식 */
enum class EdgePopupMode { SWIPE, DIRECT_TOUCH }

/** 엣지 스와이프가 시작된 터치패드 가장자리 방향 */
enum class EntryEdge { TOP, BOTTOM, LEFT, RIGHT }

// ============================================================
// 엣지 스와이프 모드
// ============================================================

/**
 * 엣지 스와이프로 토글 가능한 모드 종류.
 * 각각 ControlButtonContainer의 버튼 1개에 대응합니다.
 */
enum class EdgeSwipeMode { SCROLL, CLICK, MOVE, CURSOR }

// ============================================================
// EdgeSwipeOverlay
// ============================================================

/**
 * 엣지 스와이프 제스처로 팝업이 열린 후 표시되는 모드 선택 오버레이 (Phase 4.3.12).
 *
 * 팝업이 열리면 터치패드를 존(zone)으로 나눠 각 버튼에 매핑합니다.
 * - 스와이프: 손가락 위치 → 해당 존의 버튼이 하이라이트(선택)
 * - 탭 (이동 거리 < 임계값): 선택된 모드 버튼 ON/OFF 토글 (대기 상태 변경만)
 * - 확인 버튼 탭: 대기 상태를 실제 상태에 적용 + 팝업 닫기
 * 애니메이션은 Phase 4.3.13에서 추가됩니다.
 *
 * @param visible              표시 여부 (showEdgePopup 상태)
 * @param pendingState         표시할 대기 상태 (팝업 열릴 때 현재 상태로 초기화, 탭 토글로 갱신)
 * @param config               버튼 구성 설정 (비표시 모드는 그리드에서 제외)
 * @param selectedIndex        현재 선택(하이라이트)된 항목 인덱스
 *                             (0..visibleModes.size-1 = 모드 버튼, visibleModes.size = 확인 버튼, null = 없음)
 * @param isModeSelecting      팝업 모드 선택 단계 여부 — 스와이프/직접 터치 중 선택 중
 * @param selectedPopupMode    선택된(또는 선택 중인) 팝업 모드 — 모드 선택 UI 하이라이트 및 팝업 UI 분기에 사용
 * @param isEdgeCandidate      엣지 후보 상태 — Phase 4.3.13 물방울 애니메이션용
 * @param entryEdge            진입 가장자리 — 모드 선택 UI 방향 결정 및 Phase 4.3.13 물방울 기준점용
 * @param fingerAlongEdgePx    손가락의 엣지 축 위치 (px) — Phase 4.3.13용
 * @param inwardDistancePx     엣지에서 안쪽으로 이동한 거리 (px) — Phase 4.3.13용
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EdgeSwipeOverlay(
    visible: Boolean,
    pendingState: TouchpadState,
    config: ControlButtonConfig,
    selectedIndex: Int?,
    popupAnchorPx: Offset = Offset.Zero,
    isModeSelecting: Boolean = false,
    selectedPopupMode: EdgePopupMode? = null,
    // Phase 4.3.13용 파라미터
    isEdgeCandidate: Boolean = false,
    entryEdge: EntryEdge? = null,
    fingerAlongEdgePx: Float = 0f,
    inwardDistancePx: Float = 0f,
    modifier: Modifier = Modifier
) {
    if (!visible && !isModeSelecting) return

    // ═══ 모드 선택 단계 UI ═══
    if (isModeSelecting) {
        val hintText = if (entryEdge == EntryEdge.TOP || entryEdge == EntryEdge.BOTTOM)
            "왼쪽/오른쪽으로 선택 · 손을 놓으면 확정\n엣지로 되돌아오면 취소"
        else
            "위/아래로 선택 · 손을 놓으면 확정\n엣지로 되돌아오면 취소"
        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
        ) {
            val isHorizontalLayout = maxWidth >= 400.dp
            if (isHorizontalLayout) {
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    ModeSelectCard(
                        title = "직접 터치",
                        iconResId = R.drawable.ic_l_click,
                        isHighlighted = selectedPopupMode == EdgePopupMode.DIRECT_TOUCH
                    )
                    ModeSelectCard(
                        title = "스와이프",
                        iconResId = R.drawable.ic_scroll,
                        isHighlighted = selectedPopupMode == EdgePopupMode.SWIPE
                    )
                }
            } else {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ModeSelectCard(
                        title = "직접 터치",
                        iconResId = R.drawable.ic_l_click,
                        isHighlighted = selectedPopupMode == EdgePopupMode.DIRECT_TOUCH
                    )
                    ModeSelectCard(
                        title = "스와이프",
                        iconResId = R.drawable.ic_scroll,
                        isHighlighted = selectedPopupMode == EdgePopupMode.SWIPE
                    )
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF2A2A2A).copy(alpha = 0.9f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = hintText,
                    fontSize = 11.sp,
                    color = Color(0xFFCCCCCC),
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
            }
        }
        return
    }

    val visibleModes = buildList {
        if (config.showScrollMode) add(EdgeSwipeMode.SCROLL)
        if (config.showClickMode) add(EdgeSwipeMode.CLICK)
        if (config.showMoveMode) add(EdgeSwipeMode.MOVE)
        if (config.showCursorMode) add(EdgeSwipeMode.CURSOR)
    }
    if (visibleModes.isEmpty()) return

    val confirmIndex = visibleModes.size
    val isDirectTouch = selectedPopupMode == EdgePopupMode.DIRECT_TOUCH

    // 어두운 반투명 배경 — 즉시 표시 (fade-in 애니메이션은 Phase 4.3.13)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
    ) {
        if (isDirectTouch && popupAnchorPx != Offset.Zero) {
            // ═══ 직접 터치 모드: 앵커 위치 중심 작은 버튼 그리드 ═══
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val density = LocalDensity.current
                val buttonSizePx = with(density) { EdgeSwipeConstants.EDGE_POPUP_DIRECT_BUTTON_SIZE_DP.dp.toPx() }
                val gapPx = with(density) { EdgeSwipeConstants.EDGE_POPUP_DIRECT_BUTTON_GAP_DP.dp.toPx() }
                val confirmHeightPx = with(density) { EdgeSwipeConstants.EDGE_POPUP_DIRECT_CONFIRM_HEIGHT_DP.dp.toPx() }
                val containerW = constraints.maxWidth.toFloat()
                val containerH = constraints.maxHeight.toFloat()
                val buttonSizeDp = EdgeSwipeConstants.EDGE_POPUP_DIRECT_BUTTON_SIZE_DP.dp
                val confirmHeightDp = EdgeSwipeConstants.EDGE_POPUP_DIRECT_CONFIRM_HEIGHT_DP.dp

                val cols = if (visibleModes.size <= 1) 1 else 2
                val modeRows = (visibleModes.size + cols - 1) / cols
                val gridW = cols * buttonSizePx + (cols - 1) * gapPx
                val gridH = modeRows * buttonSizePx + modeRows * gapPx + confirmHeightPx

                val gridLeft = (popupAnchorPx.x - gridW / 2).coerceIn(0f, (containerW - gridW).coerceAtLeast(0f))
                val gridTop = (popupAnchorPx.y - gridH / 2).coerceIn(0f, (containerH - gridH).coerceAtLeast(0f))

                // 모드 버튼
                visibleModes.forEachIndexed { index, mode ->
                    val row = index / cols
                    val col = index % cols
                    val x = gridLeft + col * (buttonSizePx + gapPx)
                    val y = gridTop + row * (buttonSizePx + gapPx)

                    val info = modeCurrentInfo(mode, pendingState)
                    val contentColor = contentColorFor(info.color)
                    val isSel = selectedIndex == index

                    Box(
                        modifier = Modifier
                            .offset { IntOffset(x.toInt(), y.toInt()) }
                            .size(buttonSizeDp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(info.color.copy(alpha = if (isSel) 1.0f else 0.85f))
                            .then(
                                if (isSel) Modifier.border(2.dp, Color.White, RoundedCornerShape(8.dp))
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = info.iconResId),
                            contentDescription = info.label,
                            tint = contentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // 확인 버튼
                val confirmX = gridLeft + (gridW - buttonSizePx) / 2
                val confirmY = gridTop + modeRows * (buttonSizePx + gapPx)
                val isConfirmSel = selectedIndex == confirmIndex

                Box(
                    modifier = Modifier
                        .offset { IntOffset(confirmX.toInt(), confirmY.toInt()) }
                        .width(buttonSizeDp)
                        .height(confirmHeightDp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isConfirmSel) Color(0xFF3A5A3A) else Color(0xFF3A3A3A).copy(alpha = 0.95f)
                        )
                        .border(
                            width = if (isConfirmSel) 2.dp else 1.dp,
                            color = if (isConfirmSel) Color(0xFF88CC88) else Color(0xFF666666),
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "확인",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isConfirmSel) Color.White else Color(0xFFDDDDDD)
                    )
                }
            }
        } else if (!isDirectTouch) {
            // ═══ 스와이프 모드: 화면 중앙 큰 버튼 ═══
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    maxItemsInEachRow = 2
                ) {
                    visibleModes.forEachIndexed { index, mode ->
                        EdgeSwipeModeItem(
                            mode = mode,
                            pendingState = pendingState,
                            isSelected = selectedIndex == index
                        )
                    }
                }

                // 확인 버튼
                Box(
                    modifier = Modifier
                        .width(176.dp)
                        .height(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (selectedIndex == confirmIndex) Color(0xFF3A5A3A)
                            else Color(0xFF3A3A3A).copy(alpha = 0.95f)
                        )
                        .border(
                            width = if (selectedIndex == confirmIndex) 2.dp else 1.dp,
                            color = if (selectedIndex == confirmIndex) Color(0xFF88CC88)
                            else Color(0xFF666666),
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "확인",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selectedIndex == confirmIndex) Color.White else Color(0xFFDDDDDD)
                    )
                }
            }
        }
        else if (isDirectTouch && popupAnchorPx == Offset.Zero) {
            // ═══ 직접 터치 모드: 앵커 미설정 — 위치 선택 안내 ═══
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF2A2A2A).copy(alpha = 0.95f))
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "메뉴를 띄울 곳을\n터치하세요",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        }

        // ── 하단 안내 카드 ──
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF2A2A2A).copy(alpha = 0.9f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isDirectTouch)
                    "버튼을 직접 터치 · 확인으로 적용\n엣지로 밀어서 취소"
                else
                    "스와이프로 선택 · 탭으로 ON/OFF\n확인 선택 후 탭으로 적용",
                fontSize = 11.sp,
                color = Color(0xFFCCCCCC),
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
        }
    }
}

// ============================================================
// 모드 아이템 Composable
// ============================================================

/**
 * 엣지 스와이프 팝업 내 개별 모드 아이콘 셀.
 *
 * [pendingState]를 기반으로 현재 대기 상태(ON/OFF)에 해당하는 아이콘과 레이블을 표시합니다.
 * [isSelected]가 true이면 흰색 테두리로 선택 상태를 강조합니다.
 */
@Composable
private fun EdgeSwipeModeItem(
    mode: EdgeSwipeMode,
    pendingState: TouchpadState,
    isSelected: Boolean
) {
    val info = modeCurrentInfo(mode, pendingState)

    // 배경 밝기에 따라 텍스트/아이콘 색상 자동 결정
    val contentColor = contentColorFor(info.color)

    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(info.color.copy(alpha = if (isSelected) 1.0f else 0.75f))
            .then(
                if (isSelected)
                    Modifier.border(3.dp, Color.White, RoundedCornerShape(12.dp))
                else
                    Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = info.iconResId),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = info.label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                textAlign = TextAlign.Center,
                lineHeight = 13.sp
            )
        }
    }
}

// ============================================================
// 모드 선택 카드 Composable
// ============================================================

/**
 * 엣지 스와이프 모드 선택 단계에서 표시되는 선택지 카드.
 * [isHighlighted]가 true이면 흰색 테두리로 선택 상태를 강조합니다.
 */
@Composable
private fun ModeSelectCard(
    title: String,
    iconResId: Int,
    isHighlighted: Boolean
) {
    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isHighlighted) Color(0xFF3A3A6A) else Color(0xFF2A2A2A).copy(alpha = 0.9f)
            )
            .then(
                if (isHighlighted)
                    Modifier.border(2.dp, Color.White, RoundedCornerShape(12.dp))
                else
                    Modifier.border(1.dp, Color(0xFF555555), RoundedCornerShape(12.dp))
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = iconResId),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ============================================================
// 배경색 기반 콘텐츠 색상 결정
// ============================================================

/**
 * 지각 명도(perceived luminance)를 기반으로 배경색에 대비되는 콘텐츠 색상을 반환합니다.
 * lum = 0.299R + 0.587G + 0.114B > 0.5 이면 검정, 이하이면 흰색.
 */
private fun contentColorFor(bg: Color): Color {
    val lum = 0.299f * bg.red + 0.587f * bg.green + 0.114f * bg.blue
    return if (lum > 0.5f) Color.Black else Color.White
}

// ============================================================
// 모드 현재 상태 표시 정보 계산
// ============================================================

private data class ModeDisplayInfo(
    val iconResId: Int,
    val label: String,
    val color: Color
)

/**
 * [state](대기 상태)를 보고, [mode]의 현재 상태에 해당하는 아이콘/레이블/색상을 반환합니다.
 * "현재 이 모드가 어떤 상태인가"를 표시합니다.
 * - 스크롤 OFF: 마지막 스크롤 종류 아이콘으로 어떤 스크롤이 켜질지 암시, 어두운 색
 * - ON 상태: 해당 모드의 밝은 색
 */
private fun modeCurrentInfo(mode: EdgeSwipeMode, state: TouchpadState): ModeDisplayInfo = when (mode) {
    EdgeSwipeMode.SCROLL -> when (state.scrollMode) {
        ScrollMode.OFF -> ModeDisplayInfo(
            iconResId = if (state.lastScrollMode == ScrollMode.INFINITE_SCROLL)
                R.drawable.ic_infinite_scroll else R.drawable.ic_normal_scroll,
            label = "스크롤\nOFF",
            color = Color(0xFF505050)
        )
        ScrollMode.NORMAL_SCROLL -> ModeDisplayInfo(
            iconResId = R.drawable.ic_normal_scroll,
            label = "일반\n스크롤",
            color = TouchpadColorGreen
        )
        ScrollMode.INFINITE_SCROLL -> ModeDisplayInfo(
            iconResId = R.drawable.ic_infinite_scroll,
            label = "무한\n스크롤",
            color = TouchpadColorRed
        )
    }
    EdgeSwipeMode.CLICK -> when (state.clickMode) {
        ClickMode.LEFT_CLICK -> ModeDisplayInfo(
            iconResId = R.drawable.ic_l_click,
            label = "좌클릭\n모드",
            color = TouchpadColorBlue
        )
        ClickMode.RIGHT_CLICK -> ModeDisplayInfo(
            iconResId = R.drawable.ic_rclick,
            label = "우클릭\n모드",
            color = TouchpadColorYellow
        )
    }
    EdgeSwipeMode.MOVE -> when (state.moveMode) {
        MoveMode.FREE -> ModeDisplayInfo(
            iconResId = R.drawable.ic_free_move,
            label = "자유\n이동",
            color = TouchpadColorBlue
        )
        MoveMode.RIGHT_ANGLE -> ModeDisplayInfo(
            iconResId = R.drawable.ic_right_angle_move,
            label = "직각\n이동",
            color = TouchpadColorOrange
        )
    }
    EdgeSwipeMode.CURSOR -> when (state.cursorMode) {
        CursorMode.SINGLE -> ModeDisplayInfo(
            iconResId = R.drawable.ic_single_cursor,
            label = "싱글\n커서",
            color = TouchpadColorBlue
        )
        CursorMode.MULTI -> ModeDisplayInfo(
            iconResId = R.drawable.ic_multi_cursor,
            label = "멀티\n커서",
            color = TouchpadColorPurple
        )
    }
}
