package com.bridgeone.app.ui.components.touchpad

import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.graphics.vector.ImageVector
import com.bridgeone.app.R
import com.bridgeone.app.ui.common.AppIcons
import com.bridgeone.app.ui.common.DYNAMICS_PRESETS
import com.bridgeone.app.ui.common.EdgeSwipeConstants
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ============================================================
// 진입 엣지
// ============================================================

/** 엣지 스와이프 메뉴 조작 방식 */
enum class EdgePopupMode { SWIPE, DIRECT_TOUCH }

/** 엣지 조작 방식 (Phase 4.6.1) */
enum class EdgeInteractionMode { LEGACY_POPUP, ZONE }

/** 엣지 스와이프가 시작된 터치패드 가장자리 방향 */
enum class EntryEdge { TOP, BOTTOM, LEFT, RIGHT }

// ============================================================
// 엣지 스와이프 모드
// ============================================================

/**
 * 엣지 스와이프로 토글 가능한 모드 종류.
 * 각각 ControlButtonContainer의 버튼 1개에 대응합니다.
 */
enum class EdgeSwipeMode { SCROLL, CLICK, MOVE, CURSOR, DPI, SCROLL_SPEED, DYNAMICS }

// ============================================================
// EdgeSwipeOverlay
// ============================================================

/**
 * 엣지 스와이프 제스처로 팝업이 열린 후 표시되는 모드 선택 오버레이 (Phase 4.3.12).
 * 팝업 열기 전 중간 단계에서 스와이프/직접 터치 중 하나를 선택하는 UI를
 * **EdgePopupModeSelector (팝업 모드 선택기)** 라 부릅니다.
 *
 * 팝업이 열리면 터치패드를 존(zone)으로 나눠 각 버튼에 매핑합니다.
 * - 스와이프: 손가락 위치 → 해당 존의 버튼이 하이라이트(선택)
 * - 탭 (이동 거리 < 임계값): 선택된 모드 버튼 ON/OFF 토글 (대기 상태 변경만)
 * - 확인 버튼 탭: 대기 상태를 실제 상태에 적용 + 팝업 닫기
 *
 * @param visible              표시 여부 (showEdgePopup 상태)
 * @param pendingState         표시할 대기 상태 (팝업 열릴 때 현재 상태로 초기화, 탭 토글로 갱신)
 * @param config               버튼 구성 설정 (비표시 모드는 그리드에서 제외)
 * @param selectedIndex        현재 선택(하이라이트)된 항목 인덱스
 *                             (0..visibleModes.size-1 = 모드 버튼, visibleModes.size = 확인 버튼, null = 없음)
 * @param isModeSelecting      EdgePopupModeSelector(팝업 모드 선택기) 활성 여부 — 스와이프/직접 터치 중 선택 중
 * @param selectedPopupMode    선택된(또는 선택 중인) 팝업 모드 — 팝업 모드 선택기 하이라이트 및 팝업 UI 분기에 사용
 * @param isEdgeCandidate      엣지 후보 상태 — Phase 4.3.13 물방울 애니메이션용
 * @param entryEdge            진입 가장자리 — 모드 선택 UI 방향 결정 및 Phase 4.3.13 물방울 기준점용
 * @param fingerAlongEdgePx    손가락의 엣지 축 위치 (px) — Phase 4.3.13용
 * @param inwardDistancePx     엣지에서 안쪽으로 이동한 거리 (px) — Phase 4.3.13용
 */
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
    // Phase 4.5.9: 2단계 팝업 고정 상태
    isPopupPinned: Boolean = false,
    pinnedBorderColor: Color = Color.White,
    pinnedShakeOffsetDp: Float = 0f,
    // 제어 버튼 표시 여부 — 스와이프 그리드 상단 여백 조건부 적용
    hasControlButtons: Boolean = false,
    modifier: Modifier = Modifier
) {
    // ── 표시 가능한 모드 목록 (애니메이션 개수 계산에도 사용) ──
    // configuredModes: config 기반 전체 목록 (스크롤 필터 없음)
    // visibleModes: 스크롤 모드 활성 시 CLICK/MOVE 제외
    val configuredModes = buildList {
        if (config.showScrollMode) add(EdgeSwipeMode.SCROLL)
        if (config.showClickMode) add(EdgeSwipeMode.CLICK)
        if (config.showMoveMode) add(EdgeSwipeMode.MOVE)
        if (config.showCursorMode) add(EdgeSwipeMode.CURSOR)
        add(EdgeSwipeMode.DPI)
        add(EdgeSwipeMode.SCROLL_SPEED)
        add(EdgeSwipeMode.DYNAMICS)
    }
    val isScrolling = pendingState.scrollMode != ScrollMode.OFF
    val visibleModes = configuredModes.filter { mode ->
        when (mode) {
            EdgeSwipeMode.CLICK, EdgeSwipeMode.MOVE, EdgeSwipeMode.DPI -> !isScrolling
            EdgeSwipeMode.SCROLL_SPEED -> isScrolling
            else -> true
        }
    }

    // ── 등장/소멸 애니메이션 상태 (Phase 4.4.7) ──
    val bgAlpha = remember { Animatable(0f) }
    val maxAnimItems = 9 // 최대 7 모드(SCROLL+CLICK+MOVE+CURSOR+DPI+SCROLL_SPEED+DYNAMICS) + 확인 + 여유
    val itemOffsets = remember { List(maxAnimItems) { Animatable(40f) } }
    val itemAlphas = remember { List(maxAnimItems) { Animatable(0f) } }
    val hintAlpha = remember { Animatable(0f) }
    var isActive by remember { mutableStateOf(false) }
    // 실제 렌더링에 사용하는 목록 — 애니메이션 완료 후 visibleModes와 동기화됨
    var displayedModes by remember { mutableStateOf(visibleModes) }
    // 현 세션에서 showEdgePopup(visible)이 실제로 true였는지 추적 (Phase 4.5.9)
    // 모드 선택기만 보이다가 취소할 때 팝업 UI가 잠깐 렌더링되는 현상 방지용
    var isPopupShowing by remember { mutableStateOf(false) }
    if (visible) isPopupShowing = true
    if (!isActive) isPopupShowing = false

    val shouldShow = visible || isModeSelecting

    // 소멸 애니메이션 중 resetPopup()으로 상태가 리셋되어도
    // 이전 값을 기억하여 올바른 UI 분기를 유지 (Phase 4.5.3)
    var lastPopupMode by remember { mutableStateOf(selectedPopupMode) }
    if (selectedPopupMode != null) {
        lastPopupMode = selectedPopupMode
    }
    var lastAnchorPx by remember { mutableStateOf(popupAnchorPx) }
    if (popupAnchorPx != Offset.Zero) {
        lastAnchorPx = popupAnchorPx
    }

    LaunchedEffect(isModeSelecting, visible) {
        if (shouldShow) {
            isActive = true
            itemAlphas.forEach { it.snapTo(0f) }
            hintAlpha.snapTo(0f)

            // 배경 fade-in (이전 단계에서 이미 올라가 있으면 건너뜀)
            if (bgAlpha.value < 0.3f) {
                launch { bgAlpha.animateTo(0.6f, tween(200)) }
            }

            // 아이템 개수: 모드 선택기는 카드 2개, 팝업은 모드 버튼 + 확인
            displayedModes = visibleModes
            val count = if (isModeSelecting) 2 else (displayedModes.size + 1)

            if (isModeSelecting) {
                // 모드 선택기 카드: scale 0.7 → 1.0 + fade-in
                itemOffsets.forEach { it.snapTo(0.7f) }
                repeat(2) { i ->
                    launch {
                        delay(i * 30L)
                        launch { itemOffsets[i].animateTo(1f, tween(250, easing = FastOutSlowInEasing)) }
                        launch { itemAlphas[i].animateTo(1f, tween(200)) }
                    }
                }
            } else {
                // 팝업 아이콘: scale 0.7 → 1.0 + fade-in
                itemOffsets.forEach { it.snapTo(0.7f) }
                repeat(count.coerceAtMost(maxAnimItems)) { i ->
                    launch {
                        delay(i * 30L)
                        launch { itemOffsets[i].animateTo(1f, tween(250, easing = FastOutSlowInEasing)) }
                        launch { itemAlphas[i].animateTo(1f, tween(200)) }
                    }
                }
            }

            // 힌트 카드 fade-in
            delay(count.coerceAtMost(maxAnimItems) * 30L + 150L)
            hintAlpha.animateTo(1f, tween(150))
        } else {
            if (!isActive) return@LaunchedEffect
            // 소멸 애니메이션: 전체 fade-out
            launch { hintAlpha.animateTo(0f, tween(100)) }
            itemAlphas.forEach { anim -> launch { anim.animateTo(0f, tween(150)) } }
            delay(100)
            bgAlpha.animateTo(0f, tween(200))
            isActive = false
            lastPopupMode = null
            lastAnchorPx = Offset.Zero
        }
    }

    // 팝업이 열린 상태에서 스크롤 모드 토글 시 아이템 등장/소멸 애니메이션 (Phase 4.4.7)
    LaunchedEffect(visibleModes) {
        if (!isActive) {
            displayedModes = visibleModes
            return@LaunchedEffect
        }

        val oldDisplayed = displayedModes
        val removedModes = oldDisplayed.filter { it !in visibleModes }

        // 1단계: 제거될 항목을 먼저 fade-out
        if (removedModes.isNotEmpty()) {
            removedModes.forEach { mode ->
                val idx = oldDisplayed.indexOf(mode)
                launch { itemAlphas[idx].animateTo(0f, tween(200)) }
                launch { itemOffsets[idx].animateTo(0.7f, tween(200, easing = FastOutSlowInEasing)) }
            }
            delay(220)
        }

        // 2단계: 남은 항목과 확인 버튼이 새 인덱스로 이동할 때 alpha/offset 동기화
        val newDisplayed = visibleModes
        newDisplayed.forEachIndexed { newIdx, mode ->
            if (mode in oldDisplayed) {
                itemAlphas[newIdx].snapTo(1f)
                itemOffsets[newIdx].snapTo(1f)
            }
        }
        val newConfirmIdx = newDisplayed.size
        if (newConfirmIdx < maxAnimItems) {
            itemAlphas[newConfirmIdx].snapTo(1f)
            itemOffsets[newConfirmIdx].snapTo(1f)
        }

        displayedModes = newDisplayed

        // 3단계: 새로 추가된 항목 fade-in
        val addedModes = newDisplayed.filter { it !in oldDisplayed }
        addedModes.forEach { mode ->
            val newIdx = newDisplayed.indexOf(mode)
            itemAlphas[newIdx].snapTo(0f)
            itemOffsets[newIdx].snapTo(0.7f)
            launch { itemAlphas[newIdx].animateTo(1f, tween(200)) }
            launch { itemOffsets[newIdx].animateTo(1f, tween(200, easing = FastOutSlowInEasing)) }
        }
    }

    if (!isActive) return

    // ═══ EdgePopupModeSelector (팝업 모드 선택기) ═══
    if (isModeSelecting) {
        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = bgAlpha.value))
        ) {
            val isHorizontalLayout = maxWidth >= 400.dp
            // 힌트 텍스트: 고정 전/후 전환 (Phase 4.5.9)
            val hintText = if (isPopupPinned) {
                if (isHorizontalLayout)
                    "스와이프로 선택 · 탭으로 확정\n바깥쪽 스와이프로 취소"
                else
                    "스와이프로 선택 · 탭으로 확정\n바깥쪽 스와이프로 취소"
            } else {
                if (isHorizontalLayout)
                    "왼쪽/오른쪽으로 선택 · 손을 놓으면 확정\n엣지로 되돌아오면 취소"
                else
                    "위/아래로 선택 · 손을 놓으면 확정\n엣지로 되돌아오면 취소"
            }
            // 경계 피드백 흔들림 오프셋 (Phase 4.5.9)
            val shakeModifier = if (isPopupPinned)
                Modifier.offset(x = pinnedShakeOffsetDp.dp)
            else
                Modifier
            if (isHorizontalLayout) {
                Row(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .then(shakeModifier),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    ModeSelectCard(
                        title = "직접 터치",
                        iconResId = R.drawable.ic_l_click,
                        isHighlighted = selectedPopupMode == EdgePopupMode.DIRECT_TOUCH,
                        borderColor = if (isPopupPinned) pinnedBorderColor else Color.White,
                        modifier = Modifier
                            .alpha(itemAlphas[0].value)
                            .scale(itemOffsets[0].value)
                    )
                    ModeSelectCard(
                        title = "스와이프",
                        iconResId = R.drawable.ic_scroll,
                        isHighlighted = selectedPopupMode == EdgePopupMode.SWIPE,
                        borderColor = if (isPopupPinned) pinnedBorderColor else Color.White,
                        modifier = Modifier
                            .alpha(itemAlphas[1].value)
                            .scale(itemOffsets[1].value)
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .then(shakeModifier),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ModeSelectCard(
                        title = "직접 터치",
                        iconResId = R.drawable.ic_l_click,
                        isHighlighted = selectedPopupMode == EdgePopupMode.DIRECT_TOUCH,
                        borderColor = if (isPopupPinned) pinnedBorderColor else Color.White,
                        modifier = Modifier
                            .alpha(itemAlphas[0].value)
                            .scale(itemOffsets[0].value)
                    )
                    ModeSelectCard(
                        title = "스와이프",
                        iconResId = R.drawable.ic_scroll,
                        isHighlighted = selectedPopupMode == EdgePopupMode.SWIPE,
                        borderColor = if (isPopupPinned) pinnedBorderColor else Color.White,
                        modifier = Modifier
                            .alpha(itemAlphas[1].value)
                            .scale(itemOffsets[1].value)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                    .alpha(hintAlpha.value)
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

    if (configuredModes.isEmpty()) return
    // 팝업(showEdgePopup)이 실제로 열린 적 없이 모드 선택기만 소멸 중 → 팝업 UI 건너뜀 (Phase 4.5.9)
    if (!isPopupShowing) return

    val confirmIndex = displayedModes.size
    // 소멸 애니메이션 중에도 이전 값 기준으로 올바른 UI 분기 (Phase 4.5.3)
    val effectiveMode = selectedPopupMode ?: lastPopupMode
    val isDirectTouch = effectiveMode == EdgePopupMode.DIRECT_TOUCH
    val effectiveAnchor = if (popupAnchorPx != Offset.Zero) popupAnchorPx else lastAnchorPx

    // 어두운 반투명 배경 — fade-in 애니메이션 적용 (Phase 4.4.7)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = bgAlpha.value))
    ) {
        if (isDirectTouch && effectiveAnchor != Offset.Zero) {
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

                val cols = if (displayedModes.size <= 1) 1 else 2
                val modeRows = (displayedModes.size + cols - 1) / cols
                val gridW = cols * buttonSizePx + (cols - 1) * gapPx
                val gridH = modeRows * buttonSizePx + modeRows * gapPx + confirmHeightPx

                val gridLeft = (effectiveAnchor.x - gridW / 2).coerceIn(0f, (containerW - gridW).coerceAtLeast(0f))
                val gridTop = (effectiveAnchor.y - gridH / 2).coerceIn(0f, (containerH - gridH).coerceAtLeast(0f))

                // 모드 버튼
                displayedModes.forEachIndexed { index, mode ->
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
                            .alpha(itemAlphas[index].value)
                            .scale(itemOffsets[index].value)
                            .size(buttonSizeDp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(info.color.copy(alpha = if (isSel) 1.0f else 0.85f))
                            .then(
                                if (isSel) Modifier.border(2.dp, Color.White, RoundedCornerShape(8.dp))
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (info.imageVector != null) {
                            Icon(
                                imageVector = info.imageVector,
                                contentDescription = info.label,
                                tint = contentColor,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Icon(
                                painter = painterResource(id = info.iconResId),
                                contentDescription = info.label,
                                tint = contentColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // 확인 버튼
                val confirmX = gridLeft + (gridW - buttonSizePx) / 2
                val confirmY = gridTop + modeRows * (buttonSizePx + gapPx)
                val isConfirmSel = selectedIndex == confirmIndex

                Box(
                    modifier = Modifier
                        .offset { IntOffset(confirmX.toInt(), confirmY.toInt()) }
                        .alpha(itemAlphas[confirmIndex].value)
                        .scale(itemOffsets[confirmIndex].value)
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
            // ═══ 스와이프 모드: 폭 기반 반응형 그리드 ═══
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                // 제어 버튼 높이 (ControlButtonContainer와 동일 공식)
                val ctrlH = (maxHeight * 0.15f).coerceIn(48.dp, 72.dp)
                // 제어 버튼이 실제로 표시될 때만 상단 여백 확보
                val topReserve = if (hasControlButtons) ctrlH else 0.dp
                val hPad = 24.dp
                val itemGap = 12.dp
                val confirmH = 40.dp
                val vPad = 12.dp
                val hintH = 44.dp  // 안내 2줄 + 패딩 예상 높이. 기본값: 44dp

                // 터치패드 폭에 따라 동적으로 열 수 결정
                val colsByWidth = when {
                    maxWidth >= 440.dp -> 4   // 가로/초광폭
                    maxWidth >= 300.dp -> 3   // Page 2 풀스크린(~360-411dp)
                    else -> 2                 // Page 1(~210-245dp)
                }
                val cols = colsByWidth.coerceAtMost(displayedModes.size.coerceAtLeast(1)).coerceAtLeast(1)
                val rowCount = (displayedModes.size + cols - 1) / cols
                // 너비 기반 아이템 크기: (전체 너비 - 좌우 패딩 - 아이템 사이 간격) / 열 수
                val wSize = (maxWidth - hPad * 2 - itemGap * (cols - 1)) / cols
                // 높이 기반 아이템 크기: 상단 예약 공간 + 안내 높이를 제외한 나머지에서 역산
                val totalGaps = itemGap * (rowCount + 2)  // 행 사이 + 확인 버튼 위 + 안내 위
                val hSize = (maxHeight - topReserve - vPad * 2 - totalGaps - confirmH - hintH) / rowCount
                val itemSize = minOf(wSize, hSize).coerceIn(52.dp, 80.dp)

                // 제어 버튼이 있을 때는 그 아래 공간에, 없을 때는 화면 전체에서 중앙 배치
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = topReserve),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = hPad),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(itemGap)
                    ) {
                        displayedModes.chunked(cols).forEachIndexed { rowIdx, rowModes ->
                            Row(horizontalArrangement = Arrangement.spacedBy(itemGap)) {
                                rowModes.forEachIndexed { colIdx, mode ->
                                    val index = rowIdx * cols + colIdx
                                    EdgeSwipeModeItem(
                                        mode = mode,
                                        pendingState = pendingState,
                                        isSelected = selectedIndex == index,
                                        itemSize = itemSize,
                                        modifier = Modifier
                                            .alpha(itemAlphas[index].value)
                                            .scale(itemOffsets[index].value)
                                    )
                                }
                                if (rowModes.size < cols) {
                                    repeat(cols - rowModes.size) {
                                        Spacer(modifier = Modifier.size(itemSize))
                                    }
                                }
                            }
                        }

                        // 확인 버튼
                        val confirmWidth = itemSize * cols + itemGap * (cols - 1)
                        Box(
                            modifier = Modifier
                                .alpha(itemAlphas[confirmIndex].value)
                                .scale(itemOffsets[confirmIndex].value)
                                .width(confirmWidth)
                                .height(confirmH)
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

                        // 안내 메시지 — 확인 버튼 바로 아래, 묶음의 일부로 중앙 정렬
                        Box(
                            modifier = Modifier
                                .alpha(hintAlpha.value)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF2A2A2A).copy(alpha = 0.9f))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "스와이프로 선택 · 탭으로 ON/OFF\n확인 선택 후 탭으로 적용",
                                fontSize = 11.sp,
                                color = Color(0xFFCCCCCC),
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
        else if (isDirectTouch && effectiveAnchor == Offset.Zero) {
            // ═══ 직접 터치 모드: 앵커 미설정 — 위치 선택 안내 ═══
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .alpha(itemAlphas[0].value)
                    .scale(itemOffsets[0].value)
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF2A2A2A).copy(alpha = 0.95f))
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "메뉴를 띄울 곳을 터치하세요",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    lineHeight = 17.sp
                )
            }
        }

        // ── 하단 안내 카드 (직접 터치 모드 전용) ──
        // 스와이프 모드 안내는 묶음 Column 안으로 이동됨
        if (isDirectTouch) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                    .alpha(hintAlpha.value)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF2A2A2A).copy(alpha = 0.9f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "직접 터치 · 확인으로 적용\n엣지로 밀어서 취소",
                    fontSize = 11.sp,
                    color = Color(0xFFCCCCCC),
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

// ============================================================
// 모드 아이템 Composable
// ============================================================

/**
 * 엣지 스와이프 메뉴 내 개별 모드 아이콘 셀.
 *
 * [pendingState]를 기반으로 현재 대기 상태(ON/OFF)에 해당하는 아이콘과 레이블을 표시합니다.
 * [isSelected]가 true이면 흰색 테두리로 선택 상태를 강조합니다.
 */
@Composable
private fun EdgeSwipeModeItem(
    mode: EdgeSwipeMode,
    pendingState: TouchpadState,
    isSelected: Boolean,
    itemSize: Dp = 80.dp,
    modifier: Modifier = Modifier
) {
    val info = modeCurrentInfo(mode, pendingState)
    val iconSize = (itemSize * 0.35f).coerceIn(18.dp, 28.dp)
    val fontSize = (itemSize.value * 0.125f).coerceIn(7f, 11f)

    // 배경 밝기에 따라 텍스트/아이콘 색상 자동 결정
    val contentColor = contentColorFor(info.color)

    Box(
        modifier = modifier
            .size(itemSize)
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
            if (info.imageVector != null) {
                Icon(
                    imageVector = info.imageVector,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(iconSize)
                )
            } else {
                Icon(
                    painter = painterResource(id = info.iconResId),
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(iconSize)
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = info.label,
                fontSize = fontSize.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                textAlign = TextAlign.Center,
                lineHeight = (fontSize * 1.3f).sp
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
    isHighlighted: Boolean,
    borderColor: Color = Color.White,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(100.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isHighlighted) Color(0xFF3A3A6A) else Color(0xFF2A2A2A).copy(alpha = 0.9f)
            )
            .then(
                if (isHighlighted)
                    Modifier.border(2.dp, borderColor, RoundedCornerShape(12.dp))
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
    val iconResId: Int = 0,
    val label: String,
    val color: Color,
    val imageVector: ImageVector? = null
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
    EdgeSwipeMode.DPI -> ModeDisplayInfo(
        imageVector = when (state.dpiLevel) {
            DpiLevel.LOW    -> AppIcons.DpiLow.staticIcon
            DpiLevel.NORMAL -> AppIcons.DpiNormal.staticIcon
            DpiLevel.HIGH   -> AppIcons.DpiHigh.staticIcon
        },
        label = "DPI\n${state.dpiLevel.label}",
        color = when (state.dpiLevel) {
            DpiLevel.LOW    -> TouchpadColorTeal
            DpiLevel.NORMAL -> TouchpadColorBlue
            DpiLevel.HIGH   -> TouchpadColorLightPurple
        }
    )
    EdgeSwipeMode.SCROLL_SPEED -> ModeDisplayInfo(
        imageVector = AppIcons.ScrollMode.staticIcon,
        label = "스크롤\n${state.scrollSensitivity.label}",
        color = when (state.scrollSensitivity) {
            ScrollSensitivity.SLOW   -> TouchpadColorTeal
            ScrollSensitivity.NORMAL -> TouchpadColorBlue
            ScrollSensitivity.FAST   -> TouchpadColorLightPurple
        }
    )
    EdgeSwipeMode.DYNAMICS -> {
        val preset = DYNAMICS_PRESETS.getOrNull(state.dynamicsPresetIndex) ?: DYNAMICS_PRESETS.first()
        ModeDisplayInfo(
            imageVector = preset.icon.staticIcon,
            label = "다이나믹스\n${preset.name}",
            color = Color(0xFF5A7A5A)
        )
    }
}

// ============================================================
// 산봉우리(Bump) 오버레이 (Phase 4.4.6)
// ============================================================

/**
 * 엣지 스와이프 제스처 시 손가락 위치에 따라 둥근 산봉우리를 Canvas로 렌더링합니다.
 *
 * @param entryEdge          진입 가장자리 방향
 * @param fingerAlongEdgePx  손가락의 엣지 축 위치 (px)
 * @param inwardDistancePx   엣지에서 안쪽으로 이동한 거리 (px) — 수축 애니메이션 시 Animatable 값
 * @param maxPeakHeightPx    피크 높이 상한 (px)
 * @param baseHalfSizePx     기저부 반폭 (px)
 * @param strokeWidthPx      테두리 두께 (px)
 * @param entryAlongEdgePx   진입 시점의 엣지 방향 위치 (px) — 베이스(발) 고정 기준점
 * @param glowRadiusPx       glow 기본 반경 (px)
 * @param glowMaxRadiusPx    MAX 도달 시 glow 반경 (px)
 * @param borderColors       터치패드 테두리 그라데이션 색상 쌍 (left, right)
 */
@Composable
fun EdgeBumpOverlay(
    entryEdge: EntryEdge,
    fingerAlongEdgePx: Float,
    entryAlongEdgePx: Float,
    inwardDistancePx: Float,
    maxPeakHeightPx: Float,
    baseHalfSizePx: Float,
    strokeWidthPx: Float,
    glowRadiusPx: Float,
    glowMaxRadiusPx: Float,
    borderColors: Pair<Color, Color>,
    modifier: Modifier = Modifier
) {
    val dropletThresholdPx = with(LocalDensity.current) {
        EdgeSwipeConstants.DROPLET_APPEAR_THRESHOLD_DP.dp.toPx()
    }
    if (inwardDistancePx < dropletThresholdPx) return

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // 피크 높이: inwardDistancePx에 비례하되 상한 적용
        val peakHeight = inwardDistancePx.coerceAtMost(maxPeakHeightPx)
        val isAtMax = inwardDistancePx >= maxPeakHeightPx

        // 그라데이션 Brush (터치패드 전체 크기 기준)
        val gradientBrush = Brush.linearGradient(
            colors = listOf(borderColors.first, borderColors.second),
            start = Offset(0f, 0f),
            end = Offset(w, h)
        )

        // 엣지별 Path 좌표 계산
        // along: 현재 손가락 위치 (피크 추종), entryAlong: 진입점 고정 (베이스 기준)
        val path = Path()
        val along = fingerAlongEdgePx
        val entryAlong = entryAlongEdgePx
        val base = baseHalfSizePx

        when (entryEdge) {
            EntryEdge.LEFT -> {
                // 기저부(발): 진입점(entryAlong) 고정 / 피크: 손가락 현재 위치(along) 추종
                val topY = entryAlong - base
                val bottomY = entryAlong + base
                val peakX = peakHeight
                val peakY = along
                path.moveTo(0f, topY)
                path.cubicTo(0f, topY + base * 0.3f, peakX, peakY - base * 0.4f, peakX, peakY)
                path.cubicTo(peakX, peakY + base * 0.4f, 0f, bottomY - base * 0.3f, 0f, bottomY)
                path.close()
            }
            EntryEdge.RIGHT -> {
                val topY = entryAlong - base
                val bottomY = entryAlong + base
                val peakX = w - peakHeight
                val peakY = along
                path.moveTo(w, topY)
                path.cubicTo(w, topY + base * 0.3f, peakX, peakY - base * 0.4f, peakX, peakY)
                path.cubicTo(peakX, peakY + base * 0.4f, w, bottomY - base * 0.3f, w, bottomY)
                path.close()
            }
            EntryEdge.BOTTOM -> {
                val leftX = entryAlong - base
                val rightX = entryAlong + base
                val peakY = h - peakHeight
                val peakX = along
                path.moveTo(leftX, h)
                path.cubicTo(leftX + base * 0.3f, h, peakX - base * 0.4f, peakY, peakX, peakY)
                path.cubicTo(peakX + base * 0.4f, peakY, rightX - base * 0.3f, h, rightX, h)
                path.close()
            }
            EntryEdge.TOP -> {
                val leftX = entryAlong - base
                val rightX = entryAlong + base
                val peakY = peakHeight
                val peakX = along
                path.moveTo(leftX, 0f)
                path.cubicTo(leftX + base * 0.3f, 0f, peakX - base * 0.4f, peakY, peakX, peakY)
                path.cubicTo(peakX + base * 0.4f, peakY, rightX - base * 0.3f, 0f, rightX, 0f)
                path.close()
            }
        }

        // 피크 끝점 좌표 (glow 위치 계산) — 손가락 현재 위치(along) 기준 유지
        val peakPoint = when (entryEdge) {
            EntryEdge.LEFT -> Offset(peakHeight, along)
            EntryEdge.RIGHT -> Offset(w - peakHeight, along)
            EntryEdge.BOTTOM -> Offset(along, h - peakHeight)
            EntryEdge.TOP -> Offset(along, peakHeight)
        }

        // 현재 위치에 해당하는 그라데이션 색상 보간
        val gradientT = when (entryEdge) {
            EntryEdge.LEFT, EntryEdge.RIGHT -> {
                // 수평 그라데이션이므로 x 기준 + y 기준 혼합
                val tx = if (w > 0f) peakPoint.x / w else 0f
                val ty = if (h > 0f) peakPoint.y / h else 0f
                (tx + ty) / 2f
            }
            EntryEdge.TOP, EntryEdge.BOTTOM -> {
                val tx = if (w > 0f) peakPoint.x / w else 0f
                val ty = if (h > 0f) peakPoint.y / h else 0f
                (tx + ty) / 2f
            }
        }
        val interpolatedColor = lerp(borderColors.first, borderColors.second, gradientT.coerceIn(0f, 1f))

        drawIntoCanvas { canvas ->
            // 1) Fill — 반투명
            val fillPaint = Paint().apply {
                style = PaintingStyle.Fill
                color = interpolatedColor.copy(alpha = 0.2f)
            }
            canvas.drawPath(path, fillPaint)

            // 2) Stroke — 테두리
            val strokePaint = Paint().apply {
                style = PaintingStyle.Stroke
                this.strokeWidth = strokeWidthPx
                color = interpolatedColor
            }
            canvas.drawPath(path, strokePaint)

            // 3) Glow — 피크 발광
            val glowRadius = if (isAtMax) glowMaxRadiusPx else glowRadiusPx
            val glowAlpha = if (isAtMax) 0.6f else 0.35f
            val nativePaint = android.graphics.Paint().apply {
                this.color = interpolatedColor.copy(alpha = glowAlpha).toArgb()
                maskFilter = BlurMaskFilter(glowRadius, BlurMaskFilter.Blur.NORMAL)
                style = android.graphics.Paint.Style.FILL
            }
            canvas.nativeCanvas.drawCircle(
                peakPoint.x, peakPoint.y, glowRadius * 0.8f, nativePaint
            )
        }
    }
}

/** 두 Color 사이를 t(0..1) 비율로 선형 보간 */
private fun lerp(a: Color, b: Color, t: Float): Color = Color(
    red = a.red + (b.red - a.red) * t,
    green = a.green + (b.green - a.green) * t,
    blue = a.blue + (b.blue - a.blue) * t,
    alpha = a.alpha + (b.alpha - a.alpha) * t
)
