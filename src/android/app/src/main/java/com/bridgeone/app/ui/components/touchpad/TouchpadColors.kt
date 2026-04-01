package com.bridgeone.app.ui.components.touchpad

import androidx.compose.ui.graphics.Color

// ============================================================
// 터치패드 UI 공용 색상 팔레트 (component-touchpad.md §2.1)
// ============================================================

internal val TouchpadColorBlue = Color(0xFF2196F3)        // 기본 (좌클릭, 자유이동, 보통 DPI/감도)
internal val TouchpadColorYellow = Color(0xFFF3D021)      // 우클릭
internal val TouchpadColorGreen = Color(0xFF84E268)       // 일반 스크롤
internal val TouchpadColorTeal = Color(0xFF20D8AD)        // 느림 (DPI 낮음, 스크롤 감도 느림)
internal val TouchpadColorLightPurple = Color(0xFF818BFF) // 빠름/높음 (DPI 높음, 스크롤 감도 빠름)
internal val TouchpadColorPurple = Color(0xFFB552F6)      // 멀티 커서
internal val TouchpadColorOrange = Color(0xFFFF8A00)      // 직각 이동
internal val TouchpadColorRed = Color(0xFFF32121)         // 무한 스크롤
internal val TouchpadColorButtonText = Color(0xFF1E1E1E)  // 버튼 텍스트/아이콘 색상

// ============================================================
// 테두리 색상 결정 함수 (component-touchpad.md §2.2)
// ============================================================

/**
 * 현재 터치패드 상태에 따른 테두리 좌/우 색상 쌍을 반환합니다.
 *
 * 단색의 경우 left == right, 그라데이션의 경우 left ≠ right.
 * 호출부에서 animateColorAsState로 각각 애니메이션한 뒤
 * Brush.horizontalGradient(listOf(animatedLeft, animatedRight))로 적용합니다.
 *
 * Essential 모드 여부는 호출부에서 Color.Transparent 대입으로 처리합니다.
 *
 * 결정 우선순위 (§2.2.1):
 * - 1순위: 무한 스크롤 활성 → 빨간색 단색
 * - 2순위: 일반 스크롤 활성 → 초록색 단색
 * - 3순위: 커서 이동 모드 → 클릭×이동 조합 규칙 (§2.2.2)
 *
 * 조합 규칙 (§2.2.2):
 * - 좌클릭 + 자유이동 → 파란색 단색
 * - 좌클릭 + 직각이동 → 주황색 단색
 * - 우클릭 + 자유이동 → 노란색 단색
 * - 우클릭 + 직각이동 → 노란색(좌)→주황색(우) 그라데이션
 */
internal fun touchpadBorderColors(state: TouchpadState): Pair<Color, Color> = when {
    state.scrollMode == ScrollMode.INFINITE_SCROLL ->
        TouchpadColorRed to TouchpadColorRed
    state.scrollMode == ScrollMode.NORMAL_SCROLL ->
        TouchpadColorGreen to TouchpadColorGreen
    state.clickMode == ClickMode.LEFT_CLICK && state.moveMode == MoveMode.FREE ->
        TouchpadColorBlue to TouchpadColorBlue
    state.clickMode == ClickMode.LEFT_CLICK && state.moveMode == MoveMode.RIGHT_ANGLE ->
        TouchpadColorOrange to TouchpadColorOrange
    state.clickMode == ClickMode.RIGHT_CLICK && state.moveMode == MoveMode.FREE ->
        TouchpadColorYellow to TouchpadColorYellow
    else -> // RIGHT_CLICK + RIGHT_ANGLE → 좌=노란색, 우=주황색 그라데이션
        TouchpadColorYellow to TouchpadColorOrange
}
