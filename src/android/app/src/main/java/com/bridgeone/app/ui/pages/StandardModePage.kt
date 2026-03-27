package com.bridgeone.app.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bridgeone.app.protocol.BridgeMode
import com.bridgeone.app.ui.common.KEY_BACKSPACE
import com.bridgeone.app.ui.common.KEY_DELETE
import com.bridgeone.app.ui.common.KEY_END
import com.bridgeone.app.ui.common.KEY_ENTER
import com.bridgeone.app.ui.common.KEY_ESC
import com.bridgeone.app.ui.common.KEY_HOME
import com.bridgeone.app.ui.common.KEY_SPACE
import com.bridgeone.app.ui.common.KEY_TAB
import com.bridgeone.app.ui.components.KeyboardKeyButton
import com.bridgeone.app.ui.components.TouchpadWrapper

// ============================================================
// Standard 모드 페이지 (Phase 4.2.1: 3페이지 네비게이션)
// ============================================================

/**
 * Standard 모드 메인 페이지 (완전 재작성)
 *
 * Phase 4.2.1: HorizontalPager 기반 3페이지 시스템
 * - Page 0: 터치패드 + Actions
 * - Page 1: 키보드 (Phase 4.4에서 구현)
 * - Page 2: 마인크래프트 (Phase 4.5에서 구현)
 * - 하단 페이지 인디케이터 (닷 3개)
 */
@Composable
fun StandardModePage() {
    val pagerState = rememberPagerState(pageCount = { 3 })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // ── 페이지 컨테이너 ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) { page ->
                when (page) {
                    0 -> Page1TouchpadActions()
                    1 -> Page2KeyboardPlaceholder()
                    2 -> Page3MinecraftPlaceholder()
                }
            }
        }

        // ── 페이지 인디케이터 (닷 3개) ──
        PageIndicator(
            pagerState = pagerState,
            pageCount = 3,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 16.dp)
        )
    }
}

// ============================================================
// 페이지 인디케이터
// ============================================================

@Composable
private fun PageIndicator(
    pagerState: PagerState,
    pageCount: Int,
    modifier: Modifier = Modifier
) {
    val dotSizeDp = 8.dp
    val dotSpacingDp = 16.dp
    val totalWidth = (dotSizeDp * pageCount) + (dotSpacingDp * (pageCount - 1))

    val density = LocalDensity.current
    val dotSizePx = with(density) { dotSizeDp.toPx() }
    val dotSpacingPx = with(density) { dotSpacingDp.toPx() }
    val dotStepPx = dotSizePx + dotSpacingPx  // 한 닷에서 다음 닷까지 거리

    // 실시간 스와이프 오프셋 (드래그하는 동안 연속적으로 변함)
    val currentPage = pagerState.currentPage
    val offsetFraction = pagerState.currentPageOffsetFraction  // -1.0 ~ 1.0

    val absOffset = kotlin.math.abs(offsetFraction)
    val direction = if (offsetFraction > 0) 1f else -1f

    // THIN_WORM 효과:
    // head(앞 가장자리)가 먼저 빠르게 도달하고, tail(뒤 가장자리)이 나중에 따라옴
    val headProgress = minOf(1f, absOffset * 2f)   // 0.0 → 0.5 구간에서 0→1
    val tailProgress = maxOf(0f, absOffset * 2f - 1f)  // 0.5 → 1.0 구간에서 0→1

    val currentOriginPx = currentPage * dotStepPx

    // tail: 후반부에 출발점을 이동
    val tailPx = currentOriginPx + tailProgress * direction * dotStepPx
    // head: 전반부에 도착점으로 이동
    val headPx = currentOriginPx + dotSizePx + headProgress * direction * dotStepPx

    val leftPx = minOf(tailPx, headPx)
    val widthPx = maxOf(dotSizePx, kotlin.math.abs(headPx - tailPx))

    val leftDp = with(density) { leftPx.toDp() }
    val widthDp = with(density) { widthPx.toDp() }

    Box(
        modifier = modifier
            .width(totalWidth)
            .height(dotSizeDp)
    ) {
        // ── 배경 닷들 (비활성, 회색) ──
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(dotSpacingDp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(pageCount) {
                Box(
                    modifier = Modifier
                        .size(dotSizeDp)
                        .background(Color(0xFFC2C2C2), CircleShape)
                )
            }
        }

        // ── THIN_WORM 슬라이더 (파란색, 늘어나는 캡슐 모양) ──
        Box(
            modifier = Modifier
                .offset(x = leftDp)
                .width(widthDp)
                .height(dotSizeDp)
                .clip(CircleShape)
                .background(Color(0xFF2196F3))
        )
    }
}

// ============================================================
// Page 1: 터치패드 + Actions (임시 구현)
// ============================================================

/**
 * Page 1: 터치패드 + Actions
 *
 * Phase 4.2.2: 정식 레이아웃 구현
 * - 좌측: 터치패드 (64%)
 * - 우측: Actions 패널 (36%, LazyColumn 기반)
 * - 반응형: 폭 < 360dp 일 때 좌 60% / 우 40% 조정
 */
@Composable
private fun Page1TouchpadActions() {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp

    // 반응형 비율 계산
    val (touchpadWeight, actionsPanelWeight) = if (screenWidthDp < 360) {
        0.60f to 0.40f
    } else {
        0.64f to 0.36f
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 2열 레이아웃: 좌측 터치패드 + 우측 Actions 패널
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── 좌측: 터치패드 (64% / 60%) ──
            Box(
                modifier = Modifier
                    .weight(touchpadWeight)
                    .fillMaxHeight()
                    .align(Alignment.CenterVertically)
            ) {
                TouchpadWrapper(
                    bridgeMode = BridgeMode.STANDARD,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = Color(0xFF1A1A1A),
                            shape = RoundedCornerShape(12.dp)
                        )
                )
            }

            // ── 우측: Actions 패널 (36% / 40%) ──
            ActionsPanel(
                modifier = Modifier
                    .weight(actionsPanelWeight)
                    .fillMaxHeight()
            )
        }
    }
}

// ============================================================
// Actions 패널 (우측, LazyColumn 기반)
// ============================================================

/**
 * Actions 패널: 특수 키, 단축키, 매크로
 *
 * Phase 4.2.2: 기본 구조 구현 (그룹 헤더만)
 * Phase 4.2.3+: 각 그룹의 실제 버튼 구현
 */
@Composable
private fun ActionsPanel(
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .background(Color(0xFF121212), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Special Keys 그룹 ──
        item {
            Text(
                text = "Special Keys",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFFFFF),
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
        item {
            SpecialKeysGrid()
        }

        // ── 그룹 간 간격 ──
        item {
            Spacer(modifier = Modifier.height(4.dp))
        }

        // ── Shortcuts 그룹 ──
        item {
            Text(
                text = "Shortcuts",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFFFFF),
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
        item {
            // Phase 4.2.4에서 구현: 8개 단축키 2열 그리드
            // 임시 placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Shortcuts (Phase 4.2.4)",
                    fontSize = 11.sp,
                    color = Color(0xFFA0A0A0)
                )
            }
        }

        // ── 그룹 간 간격 ──
        item {
            Spacer(modifier = Modifier.height(4.dp))
        }

        // ── Macros 그룹 ──
        item {
            Text(
                text = "Macros",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFFFFF),
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
        item {
            // Phase 4.2.5에서 구현: 3개 disabled 버튼
            // 임시 placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Macros (Phase 4.2.5)",
                    fontSize = 11.sp,
                    color = Color(0xFFA0A0A0)
                )
            }
        }
    }
}

// ============================================================
// Special Keys 그룹 (Phase 4.2.3)
// ============================================================

/**
 * Special Keys 2열 그리드
 *
 * 8개 키: Esc, Tab, Enter, Backspace, Delete, Space, Home, End
 * - 모두 stickyHoldEnabled=false (자연 홀드)
 * - 길게 누르면 PC OS가 자체적으로 키 반복 처리 (물리 키보드와 동일)
 *
 * ClickDetector 연결은 Phase 4.3 이후 실기기 검증 시 추가 예정.
 * 현재는 Log만 출력.
 */
@Composable
private fun SpecialKeysGrid() {
    val keys = listOf(
        "Esc" to KEY_ESC,
        "Tab" to KEY_TAB,
        "Enter" to KEY_ENTER,
        "⌫" to KEY_BACKSPACE,
        "Del" to KEY_DELETE,
        "Space" to KEY_SPACE,
        "Home" to KEY_HOME,
        "End" to KEY_END
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        keys.chunked(2).forEach { rowKeys ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowKeys.forEach { (label, keyCode) ->
                    KeyboardKeyButton(
                        keyLabel = label,
                        keyCode = keyCode,
                        stickyHoldEnabled = false,
                        onKeyPressed = { code ->
                            android.util.Log.d("SpecialKeys", "KeyDown: $label (0x${code.toString(16)})")
                        },
                        onKeyReleased = { code ->
                            android.util.Log.d("SpecialKeys", "KeyUp: $label (0x${code.toString(16)})")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    )
                }
                if (rowKeys.size < 2) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// ============================================================
// Page 2: 키보드 (Placeholder - Phase 4.4에서 구현)
// ============================================================

@Composable
private fun Page2KeyboardPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Page 2",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2196F3)
            )
            Text(
                text = "키보드 중심 레이아웃",
                fontSize = 14.sp,
                color = Color(0xFFC2C2C2)
            )
            Text(
                text = "(Phase 4.4에서 구현 예정)",
                fontSize = 12.sp,
                color = Color(0xFF888888),
                fontWeight = FontWeight.Light
            )
        }
    }
}

// ============================================================
// Page 3: 마인크래프트 (Placeholder - Phase 4.5에서 구현)
// ============================================================

@Composable
private fun Page3MinecraftPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Page 3",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2196F3)
            )
            Text(
                text = "마인크래프트 특화",
                fontSize = 14.sp,
                color = Color(0xFFC2C2C2)
            )
            Text(
                text = "(Phase 4.5에서 구현 예정)",
                fontSize = 12.sp,
                color = Color(0xFF888888),
                fontWeight = FontWeight.Light
            )
        }
    }
}

