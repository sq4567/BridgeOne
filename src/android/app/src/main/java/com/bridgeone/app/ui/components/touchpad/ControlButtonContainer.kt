package com.bridgeone.app.ui.components.touchpad

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ============================================================
// 색상 팔레트 (component-touchpad.md §2.1)
// ============================================================

private val ColorBlue = Color(0xFF2196F3)       // 기본 (좌클릭, 자유이동, 보통 DPI/감도)
private val ColorYellow = Color(0xFFF3D021)      // 우클릭
private val ColorGreen = Color(0xFF84E268)       // 일반 스크롤
private val ColorTeal = Color(0xFF20D8AD)        // 느림 (DPI 낮음, 스크롤 감도 느림)
private val ColorLightPurple = Color(0xFF818BFF) // 빠름/높음 (DPI 높음, 스크롤 감도 빠름)
private val ColorPurple = Color(0xFFB552F6)      // 멀티 커서
private val ColorOrange = Color(0xFFFF8A00)      // 직각 이동
private val ColorRed = Color(0xFFF32121)         // 무한 스크롤
private val ColorButtonText = Color(0xFF1E1E1E)  // 버튼 텍스트/아이콘 색상

// ============================================================
// ControlButtonContainer
// ============================================================

/**
 * 터치패드 상단 15% 오버레이 제어 버튼 컨테이너
 *
 * Phase 4.3.1: component-touchpad.md §1.3 기반 구현
 * - 좌측: 모드 제어 버튼 4개 (Click, Move, Scroll, Cursor)
 * - 우측: 옵션 버튼 2개 (DPI, ScrollSensitivity)
 * - 가시성 규칙: 스크롤 모드 시 Click/DPI 숨김, ScrollSensitivity 표시
 *
 * @param touchpadState 현재 터치패드 상태
 * @param onStateChange 상태 변경 콜백
 * @param modifier 외부 Modifier
 */
@Composable
fun ControlButtonContainer(
    touchpadState: TouchpadState,
    onStateChange: (TouchpadState) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val containerWidth = maxWidth
        val containerHeight = maxHeight

        // 컨테이너 높이: 터치패드 높이의 15% (최소 48dp, 최대 72dp)
        val controlHeight = (containerHeight * 0.15f).coerceIn(48.dp, 72.dp)
        // 여백: 터치패드 너비의 2% (최소 4dp, 최대 8dp)
        val containerPadding = (containerWidth * 0.02f).coerceIn(4.dp, 8.dp)
        // 버튼 간격: 고정 2dp
        val buttonSpacing = 2.dp

        // 버튼 크기: 너비는 6개 버튼 + 간격 + 패딩을 고려하여 자동 계산
        // 좌4개 + 우2개 = 6버튼, gap 5개, 패딩 2개
        val availableWidth = containerWidth - containerPadding * 2
        val buttonWidth = ((availableWidth - buttonSpacing * 5) / 6).coerceAtLeast(20.dp)
        val buttonHeight = (buttonWidth * 2f).coerceAtMost(controlHeight) // 1:2 비율

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(controlHeight)
                .padding(horizontal = containerPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // ── 좌측: 모드 제어 버튼 ──
            Row(
                horizontalArrangement = Arrangement.spacedBy(buttonSpacing),
                verticalAlignment = Alignment.Top
            ) {
                // 1. ClickModeButton: 스크롤 모드에서 투명 처리 (자리 유지 → Scroll 버튼 위치 고정)
                Box(modifier = Modifier.alpha(if (touchpadState.isCursorMoveActive) 1f else 0f)) {
                    ControlButton(
                        text = if (touchpadState.clickMode == ClickMode.LEFT_CLICK)
                            "우클릭\n모드" else "좌클릭\n모드",
                        backgroundColor = if (touchpadState.clickMode == ClickMode.LEFT_CLICK)
                            ColorYellow else ColorBlue,
                        buttonWidth = buttonWidth,
                        buttonHeight = buttonHeight,
                        enabled = touchpadState.isCursorMoveActive,
                        onClick = {
                            val newMode = if (touchpadState.clickMode == ClickMode.LEFT_CLICK)
                                ClickMode.RIGHT_CLICK else ClickMode.LEFT_CLICK
                            onStateChange(touchpadState.copy(clickMode = newMode))
                        }
                    )
                }

                // 2. MoveModeButton: 스크롤 모드에서 투명 처리 (자리 유지 → Scroll 버튼 위치 고정)
                Box(modifier = Modifier.alpha(if (touchpadState.isCursorMoveActive) 1f else 0f)) {
                    ControlButton(
                        text = if (touchpadState.moveMode == MoveMode.FREE)
                            "직각\n이동" else "자유\n이동",
                        backgroundColor = if (touchpadState.moveMode == MoveMode.FREE)
                            ColorOrange else ColorBlue,
                        buttonWidth = buttonWidth,
                        buttonHeight = buttonHeight,
                        enabled = touchpadState.isCursorMoveActive,
                        onClick = {
                            val newMode = if (touchpadState.moveMode == MoveMode.FREE)
                                MoveMode.RIGHT_ANGLE else MoveMode.FREE
                            onStateChange(touchpadState.copy(moveMode = newMode))
                        }
                    )
                }

                // 3. ScrollModeButton: 항상 표시
                // 탭 = 켜기/끄기 토글, 롱프레스 = NORMAL ↔ INFINITE 전환 (스크롤 ON 중만)
                ControlButton(
                    text = scrollModeButtonText(touchpadState),
                    backgroundColor = scrollModeButtonColor(touchpadState),
                    buttonWidth = buttonWidth,
                    buttonHeight = buttonHeight,
                    onClick = {
                        onStateChange(toggleScrollMode(touchpadState))
                    },
                    onLongClick = if (touchpadState.isScrollActive) {
                        { onStateChange(switchScrollMode(touchpadState)) }
                    } else null
                )

                // 4. CursorModeButton: Disabled (Phase 4+ 멀티 커서 미구현)
                ControlButton(
                    text = if (touchpadState.cursorMode == CursorMode.SINGLE)
                        "멀티\n커서" else "싱글\n커서",
                    backgroundColor = if (touchpadState.cursorMode == CursorMode.SINGLE)
                        ColorPurple else ColorBlue,
                    buttonWidth = buttonWidth,
                    buttonHeight = buttonHeight,
                    enabled = false,
                    onClick = { /* Phase 4+: 멀티 커서 미구현 */ }
                )
            }

            // ── 우측: 옵션 수치 제어 버튼 ──
            Row(
                horizontalArrangement = Arrangement.spacedBy(buttonSpacing),
                verticalAlignment = Alignment.Top
            ) {
                // DPIControlButton: 스크롤 모드에서 숨김
                AnimatedVisibility(
                    visible = touchpadState.isCursorMoveActive,
                    enter = fadeIn(tween(200)),
                    exit = fadeOut(tween(200))
                ) {
                    ControlButton(
                        text = "DPI\n${touchpadState.dpiLevel.label}",
                        backgroundColor = dpiButtonColor(touchpadState.dpiLevel),
                        buttonWidth = buttonWidth,
                        buttonHeight = buttonHeight,
                        onClick = {
                            onStateChange(touchpadState.copy(
                                dpiLevel = touchpadState.dpiLevel.next()
                            ))
                        }
                    )
                }

                // ScrollSensitivityButton: 스크롤 모드에서만 표시
                AnimatedVisibility(
                    visible = touchpadState.isScrollActive,
                    enter = fadeIn(tween(200)),
                    exit = fadeOut(tween(200))
                ) {
                    ControlButton(
                        text = "스크롤\n${touchpadState.scrollSensitivity.label}",
                        backgroundColor = scrollSensitivityButtonColor(
                            touchpadState.scrollSensitivity
                        ),
                        buttonWidth = buttonWidth,
                        buttonHeight = buttonHeight,
                        onClick = {
                            onStateChange(touchpadState.copy(
                                scrollSensitivity = touchpadState.scrollSensitivity.next()
                            ))
                        }
                    )
                }
            }
        }
    }
}

// ============================================================
// 개별 제어 버튼 Composable
// ============================================================

/**
 * 제어 버튼 공통 구조 (component-touchpad.md §1.4)
 *
 * - 배경: 하단 둥근 모서리 직사각형
 * - 텍스트: 버튼 높이의 30%, 중앙 정렬
 * - 터치 피드백: 햅틱 + 스케일 (1.0 -> 0.95 -> 1.0)
 * - 히트 영역: 최소 48×48dp 보장
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ControlButton(
    text: String,
    backgroundColor: Color,
    buttonWidth: Dp,
    buttonHeight: Dp,
    enabled: Boolean = true,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // 스케일 애니메이션: 1.0 -> 0.95 -> 1.0
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = tween(durationMillis = 100),
        label = "controlButtonScale"
    )

    // 기본 텍스트 크기: 버튼 높이의 20% (7sp~10sp)
    val density = LocalDensity.current
    val textSizeRawSp = with(density) { (buttonHeight * 0.20f).toSp().value }
        .coerceIn(7f, 10f)
    val textSizeSmallSp = textSizeRawSp * 0.78f

    // 실제 렌더링 너비 측정으로 줄 초과 여부 판단
    val textMeasurer = rememberTextMeasurer()
    val buttonWidthPx = with(density) { buttonWidth.toPx() }

    val lines = text.split("\n")
    val annotatedText = buildAnnotatedString {
        lines.forEachIndexed { index, line ->
            val measuredWidth = textMeasurer.measure(
                text = line,
                style = TextStyle(fontSize = textSizeRawSp.sp, fontWeight = FontWeight.Bold)
            ).size.width
            val lineFontSize = if (measuredWidth > buttonWidthPx) textSizeSmallSp else textSizeRawSp
            withStyle(SpanStyle(fontSize = lineFontSize.sp, fontWeight = FontWeight.Bold)) {
                append(line)
            }
            if (index < lines.size - 1) append("\n")
        }
    }

    // Disabled 상태 투명도
    val alpha = if (enabled) 1f else 0.4f

    Box(
        modifier = Modifier
            // 히트 영역: 세로만 최소 48dp 보장 (가로는 버튼 너비에 맞춤)
            .heightIn(min = 48.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .width(buttonWidth)
                .height(buttonHeight)
                .scale(scale)
                .shadow(elevation = 6.dp, shape = RoundedCornerShape(
                    topStart = 0.dp,
                    topEnd = 0.dp,
                    bottomStart = 8.dp,
                    bottomEnd = 8.dp
                ))
                .clip(
                    RoundedCornerShape(
                        topStart = 0.dp,
                        topEnd = 0.dp,
                        bottomStart = 8.dp,
                        bottomEnd = 8.dp
                    )
                )
                .background(backgroundColor.copy(alpha = alpha))
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        } else {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        }
                        onClick()
                    },
                    onLongClick = onLongClick?.let { longClick ->
                        {
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            longClick()
                        }
                    }
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = annotatedText,
                color = ColorButtonText,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Clip,
                lineHeight = (textSizeRawSp * 1.15f).sp
            )
        }
    }
}

// ============================================================
// 스크롤 모드 버튼 상태 계산
// ============================================================

/**
 * ScrollModeButton 텍스트
 * - OFF: lastScrollMode 기반으로 켜질 모드 표시
 * - ON: 현재 활성 모드 표시 (탭 = 끄기, 롱프레스 = 전환)
 */
private fun scrollModeButtonText(state: TouchpadState): String = when (state.scrollMode) {
    ScrollMode.OFF -> {
        if (state.lastScrollMode == ScrollMode.INFINITE_SCROLL)
            "무한\n스크롤" else "일반\n스크롤"
    }
    ScrollMode.NORMAL_SCROLL -> "일반\n스크롤"
    ScrollMode.INFINITE_SCROLL -> "무한\n스크롤"
}

/**
 * ScrollModeButton 배경 색상
 * - OFF: 켜질 모드 색상 (초록=일반, 빨강=무한)
 * - ON: 현재 활성 모드 색상
 */
private fun scrollModeButtonColor(state: TouchpadState): Color = when (state.scrollMode) {
    ScrollMode.OFF -> {
        if (state.lastScrollMode == ScrollMode.INFINITE_SCROLL)
            ColorRed else ColorGreen
    }
    ScrollMode.NORMAL_SCROLL -> ColorGreen
    ScrollMode.INFINITE_SCROLL -> ColorRed
}

/**
 * 스크롤 버튼 탭: 켜기/끄기 토글
 * - OFF → lastScrollMode 복원 (일반 or 무한)
 * - NORMAL/INFINITE → OFF (lastScrollMode 저장 후 종료)
 */
private fun toggleScrollMode(state: TouchpadState): TouchpadState = when (state.scrollMode) {
    ScrollMode.OFF -> state.copy(scrollMode = state.lastScrollMode)
    ScrollMode.NORMAL_SCROLL -> state.copy(
        scrollMode = ScrollMode.OFF,
        lastScrollMode = ScrollMode.NORMAL_SCROLL
    )
    ScrollMode.INFINITE_SCROLL -> state.copy(
        scrollMode = ScrollMode.OFF,
        lastScrollMode = ScrollMode.INFINITE_SCROLL
    )
}

/**
 * 스크롤 버튼 롱프레스: NORMAL ↔ INFINITE 전환 (스크롤 ON 상태에서만 호출됨)
 */
private fun switchScrollMode(state: TouchpadState): TouchpadState = when (state.scrollMode) {
    ScrollMode.NORMAL_SCROLL -> state.copy(
        scrollMode = ScrollMode.INFINITE_SCROLL,
        lastScrollMode = ScrollMode.INFINITE_SCROLL
    )
    ScrollMode.INFINITE_SCROLL -> state.copy(
        scrollMode = ScrollMode.NORMAL_SCROLL,
        lastScrollMode = ScrollMode.NORMAL_SCROLL
    )
    ScrollMode.OFF -> state  // no-op
}

// ============================================================
// 옵션 버튼 색상 계산
// ============================================================

/** DPI 버튼 배경 색상 (§2.3.2) */
private fun dpiButtonColor(level: DpiLevel): Color = when (level) {
    DpiLevel.LOW -> ColorTeal
    DpiLevel.NORMAL -> ColorBlue
    DpiLevel.HIGH -> ColorLightPurple
}

/** 스크롤 감도 버튼 배경 색상 (§2.3.2) */
private fun scrollSensitivityButtonColor(level: ScrollSensitivity): Color = when (level) {
    ScrollSensitivity.SLOW -> ColorTeal
    ScrollSensitivity.NORMAL -> ColorBlue
    ScrollSensitivity.FAST -> ColorLightPurple
}
