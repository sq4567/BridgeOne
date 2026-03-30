package com.bridgeone.app.ui.pages

import android.content.Context
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import com.bridgeone.app.ui.components.DEFAULT_SHORTCUTS
import com.bridgeone.app.ui.components.KeyboardKeyButton
import com.bridgeone.app.ui.components.ShortcutButton
import com.bridgeone.app.ui.components.TouchpadWrapper
import com.bridgeone.app.ui.components.touchpad.ControlButtonContainer
import com.bridgeone.app.ui.components.touchpad.DpiAdjustPopup
import com.bridgeone.app.ui.components.touchpad.DpiLevel
import com.bridgeone.app.ui.components.touchpad.ScrollMode
import com.bridgeone.app.ui.components.touchpad.TouchpadState
import com.bridgeone.app.ui.utils.ClickDetector
import kotlin.math.abs

// ============================================================
// Standard 모드 페이지 (Phase 4.2.1: 3페이지 네비게이션)
// ============================================================

/**
 * Standard 모드 메인 페이지 (완전 재작성)
 *
 * Phase 4.2.1: HorizontalPager 기반 4페이지 시스템
 * - Page 0: 터치패드 + Actions (상대좌표)
 * - Page 1: 절대좌표 패드 (Phase 4.4에서 구현)
 * - Page 2: 키보드 (Phase 4.5에서 구현)
 * - Page 3: 마인크래프트 (Phase 4.6에서 구현)
 * - 하단 페이지 인디케이터 (닷 4개)
 */
@Composable
fun StandardModePage() {
    val pagerState = rememberPagerState(pageCount = { 4 })
    val context = LocalContext.current

    // Phase 4.3.3: 터치패드 상태를 페이지 레벨로 호이스팅
    // DpiLevel은 SharedPreferences에서 복원 (Phase 4.3.6)
    var touchpadState by remember { mutableStateOf(TouchpadState(dpiLevel = loadDpiLevel(context))) }

    // DPI 레벨(사전 정의 값)이 변경될 때 SharedPreferences에 저장
    LaunchedEffect(touchpadState.dpiLevel) {
        saveDpiLevel(context, touchpadState.dpiLevel)
    }

    // Phase 4.3.6: DPI 세밀 조절 팝업 상태
    var dpiAdjustPopupVisible by remember { mutableStateOf(false) }

    // 페이지 전환 시 팝업 취소 (커스텀 값 미적용)
    LaunchedEffect(pagerState.currentPage) {
        if (dpiAdjustPopupVisible) dpiAdjustPopupVisible = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // ── 페이지 컨테이너 ──
        // 스크롤 모드 활성 시: HorizontalPager보다 먼저 Initial 패스에서
        // Move 이벤트를 소비하여 페이저의 수평 드래그 감지를 원천 차단
        val isScrollActive = touchpadState.scrollMode != ScrollMode.OFF
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(isScrollActive) {
                    if (!isScrollActive) return@pointerInput
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            if (event.type == PointerEventType.Move) {
                                event.changes.forEach { it.consume() }
                            }
                        }
                    }
                }
        ) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = touchpadState.scrollMode == ScrollMode.OFF,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) { page ->
                when (page) {
                    0 -> Page1TouchpadActions(
                        touchpadState = touchpadState,
                        onTouchpadStateChange = { touchpadState = it },
                        dpiAdjustPopupVisible = dpiAdjustPopupVisible,
                        onDpiLongPress = { dpiAdjustPopupVisible = true },
                        onDpiAdjustConfirm = { value ->
                            dpiAdjustPopupVisible = false
                            // 사전 정의 값과 일치 시 해당 레벨로 매핑, 아니면 커스텀
                            val matchedLevel = DpiLevel.entries.firstOrNull {
                                abs(it.multiplier - value) < 0.001f
                            }
                            touchpadState = if (matchedLevel != null) {
                                touchpadState.copy(
                                    dpiLevel = matchedLevel,
                                    customDpiMultiplier = null
                                )
                            } else {
                                touchpadState.copy(customDpiMultiplier = value)
                            }
                        },
                        onDpiAdjustDismiss = { dpiAdjustPopupVisible = false }
                    )
                    1 -> Page2AbsolutePointingPlaceholder()
                    2 -> Page3KeyboardPlaceholder()
                    3 -> Page4MinecraftPlaceholder()
                }
            }
        }

        // ── 페이지 인디케이터 (닷 3개) ──
        PageIndicator(
            pagerState = pagerState,
            pageCount = 4,
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
private fun Page1TouchpadActions(
    touchpadState: TouchpadState,
    onTouchpadStateChange: (TouchpadState) -> Unit,
    dpiAdjustPopupVisible: Boolean = false,
    onDpiLongPress: () -> Unit = {},
    onDpiAdjustConfirm: (Float) -> Unit = {},
    onDpiAdjustDismiss: () -> Unit = {}
) {
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
            // Phase 4.3.1: Box 내부에 ControlButtonContainer 오버레이 추가
            // Phase 4.3.6: DpiAdjustPopup 표시 시 배경 블러 적용
            val blurRadius by animateDpAsState(
                targetValue = if (dpiAdjustPopupVisible) 8.dp else 0.dp,
                animationSpec = tween(200),
                label = "dpiBlur"
            )
            Box(
                modifier = Modifier
                    .weight(touchpadWeight)
                    .fillMaxHeight()
                    .align(Alignment.CenterVertically)
            ) {
                // 팝업 표시 시 블러 처리되는 배경 영역
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .blur(blurRadius)
                ) {
                    TouchpadWrapper(
                        bridgeMode = BridgeMode.STANDARD,
                        touchpadState = touchpadState,
                        onTouchpadStateChange = onTouchpadStateChange,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                color = Color(0xFF1A1A1A),
                                shape = RoundedCornerShape(12.dp)
                            )
                    )

                    // Phase 4.3.1: ControlButtonContainer 오버레이 (상단 15%)
                    ControlButtonContainer(
                        touchpadState = touchpadState,
                        onStateChange = onTouchpadStateChange,
                        onDpiLongPress = onDpiLongPress,
                        modifier = Modifier
                            .fillMaxSize()
                            .align(Alignment.TopCenter)
                    )
                }

                // Phase 4.3.6: DPI 세밀 조절 팝업 오버레이
                // 팝업 내 pointerInput이 이벤트를 소비 → TouchpadWrapper 제스처 자동 차단
                if (dpiAdjustPopupVisible) {
                    DpiAdjustPopup(
                        initialMultiplier = touchpadState.effectiveDpiMultiplier,
                        onConfirm = onDpiAdjustConfirm,
                        onDismiss = onDpiAdjustDismiss,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                    )
                }
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
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // ── Special Keys 그룹 ──
        item {
            Text(
                text = "특수 키",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFFFFF),
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }
        item {
            SpecialKeysGrid()
        }

        // ── Shortcuts 그룹 ──
        item {
            Text(
                text = "단축키",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFFFFF),
                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
            )
        }
        item {
            ShortcutsGrid()
        }

        // ── Macros 그룹 ──
        item {
            Text(
                text = "매크로",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFFFFF),
                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
            )
        }
        item {
            MacrosPlaceholder()
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
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        keys.chunked(2).forEach { rowKeys ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rowKeys.forEach { (label, keyCode) ->
                    KeyboardKeyButton(
                        keyLabel = label,
                        keyCode = keyCode,
                        stickyHoldEnabled = false,
                        onKeyPressed = { code ->
                            val frame = ClickDetector.createKeyboardFrame(
                                activeModifierKeys = emptySet(),
                                keyCode1 = code
                            )
                            ClickDetector.sendFrame(frame)
                        },
                        onKeyReleased = { _ ->
                            val frame = ClickDetector.createKeyboardFrame(
                                activeModifierKeys = emptySet(),
                                keyCode1 = 0u
                            )
                            ClickDetector.sendFrame(frame)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
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
// Shortcuts 그룹 (Phase 4.2.4)
// ============================================================

/**
 * Shortcuts 2열 그리드
 *
 * 8개 단축키: Ctrl+C, Ctrl+V, Ctrl+S, Ctrl+Z, Ctrl+Shift+Z, Ctrl+X, Alt+Tab, Win+D
 * - TAP 모드: 탭 → Modifier↓ → Key↓ → Key↑ → Modifier↑ 순차 전송
 * - HOLD 모드: Alt+Tab — 누름 동안 유지, 뗌 시 해제
 * - 150ms 디바운스 (Win+D는 500ms)
 *
 */
@Composable
private fun ShortcutsGrid() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        DEFAULT_SHORTCUTS.chunked(2).forEach { rowShortcuts ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rowShortcuts.forEach { shortcutDef ->
                    ShortcutButton(
                        shortcutDef = shortcutDef,
                        onShortcutTriggered = { mod, key ->
                            val frame = ClickDetector.createKeyboardFrame(
                                activeModifierKeys = if (mod != 0u.toUByte()) setOf(mod) else emptySet(),
                                keyCode1 = key
                            )
                            ClickDetector.sendFrame(frame)
                        },
                        onShortcutReleased = { _, _ ->
                            val frame = ClickDetector.createKeyboardFrame(
                                activeModifierKeys = emptySet(),
                                keyCode1 = 0u
                            )
                            ClickDetector.sendFrame(frame)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                    )
                }
                if (rowShortcuts.size < 2) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// ============================================================
// Macros Placeholder (Phase 4.2.5)
// ============================================================

/**
 * Macros 세로 리스트 (Disabled 상태)
 *
 * 3개 매크로 버튼: Macro 1, Macro 2, Macro 3
 * - 항상 Disabled 상태 (#C2C2C2, alpha 0.6)
 * - 탭 시 아무 동작 없음
 * - PlayArrow 아이콘 표시
 */
@Composable
private fun MacrosPlaceholder() {
    val macros = listOf("Macro 1", "Macro 2", "Macro 3")

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        macros.forEach { label ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .alpha(0.6f)
                    .background(Color(0xFF2A2A2A), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Color(0xFFC2C2C2),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        color = Color(0xFFC2C2C2)
                    )
                }
            }
        }
    }
}

// ============================================================
// Page 2: 절대좌표 패드 (Placeholder - AbsolutePointingPad)
// ============================================================

@Composable
private fun Page2AbsolutePointingPlaceholder() {
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
                color = Color(0xFFE91E63)
            )
            Text(
                text = "절대좌표 패드",
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
// Page 3: 키보드 (Placeholder - Phase 4.5에서 구현)
// ============================================================

@Composable
private fun Page3KeyboardPlaceholder() {
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
                text = "키보드 중심 레이아웃",
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

// ============================================================
// Page 4: 마인크래프트 (Placeholder - Phase 4.6에서 구현)
// ============================================================

@Composable
private fun Page4MinecraftPlaceholder() {
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
                text = "Page 4",
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
                text = "(Phase 4.6에서 구현 예정)",
                fontSize = 12.sp,
                color = Color(0xFF888888),
                fontWeight = FontWeight.Light
            )
        }
    }
}

// ============================================================
// DPI 레벨 SharedPreferences 저장/복원 (Phase 4.3.6)
// ============================================================

private const val PREF_NAME = "touchpad_prefs"
private const val KEY_DPI_LEVEL = "dpi_level"

private fun loadDpiLevel(context: Context): DpiLevel {
    val name = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        .getString(KEY_DPI_LEVEL, DpiLevel.NORMAL.name) ?: DpiLevel.NORMAL.name
    return DpiLevel.entries.firstOrNull { it.name == name } ?: DpiLevel.NORMAL
}

private fun saveDpiLevel(context: Context, level: DpiLevel) {
    context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_DPI_LEVEL, level.name)
        .apply()
}
