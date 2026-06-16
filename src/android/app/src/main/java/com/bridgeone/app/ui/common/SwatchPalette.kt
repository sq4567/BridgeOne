package com.bridgeone.app.ui.common

import androidx.compose.ui.graphics.Color

// ============================================================
// COLOR_SWATCH_PALETTE — 컬러 피커 팔레트 스와치 목록
// ============================================================

/**
 * 컬러 피커에 표시할 기본 색상 팔레트 (6열 × 5행 = 30색).
 *
 * 구성:
 * - Row 0: 따뜻한 계열 (레드·오렌지·옐로)
 * - Row 1: 시원한 계열 (그린·틸·블루)
 * - Row 2: 보라 계열 + 블루그레이
 * - Row 3: 무채색 (흰색 → 검정)
 * - Row 4: 앱 전용 팔레트 (TouchpadColors 9색 중 대표)
 *
 * 앱 색상 기반: TouchpadColors.kt, theme/Color.kt
 */
val COLOR_SWATCH_PALETTE: List<Color> = listOf(
    // Row 0: 따뜻한 계열
    Color(0xFFF32121), // 앱 레드 (무한 스크롤)
    Color(0xFFF44336), // Material Red-500
    Color(0xFFE91E63), // Pink
    Color(0xFFFF5722), // Deep Orange
    Color(0xFFFF8A00), // 앱 오렌지 (직각이동)
    Color(0xFFF3D021), // 앱 옐로 (우클릭)

    // Row 1: 시원한 계열
    Color(0xFF84E268), // 앱 그린 (일반 스크롤)
    Color(0xFF4CAF50), // Material Green-500
    Color(0xFF8BC34A), // Light Green
    Color(0xFF20D8AD), // 앱 틸 (느림)
    Color(0xFF00BCD4), // Cyan
    Color(0xFF2196F3), // 앱 블루 / StateInfo (좌클릭)

    // Row 2: 보라 계열 + 기타
    Color(0xFF1565C0), // Blue-800
    Color(0xFF3F51B5), // Indigo
    Color(0xFF818BFF), // 앱 라이트퍼플 (빠름)
    Color(0xFF673AB7), // Deep Purple
    Color(0xFFB552F6), // 앱 퍼플 (멀티커서)
    Color(0xFF607D8B), // Blue Grey

    // Row 3: 무채색
    Color(0xFFFFFFFF), // White
    Color(0xFFC2C2C2), // 앱 TextDisabled
    Color(0xFF9E9E9E), // Grey-500
    Color(0xFF616161), // Grey-700
    Color(0xFF1E1E1E), // 앱 서피스 (버튼 텍스트 bg)
    Color(0xFF121212), // 앱 BackgroundPrimary

    // Row 4: 앱 선택 확장 + 상태색
    Color(0xFFFF9800), // StateWarning / Amber
    Color(0xFF9C27B0), // Purple
    Color(0xFF009688), // Teal-500
    Color(0xFF795548), // Brown
    Color(0xFF4CAF50), // StateSuccess
    Color(0xFFF44336), // StateError
)

// ============================================================
// ColorPickerConstants — 컬러 피커 상수
// ============================================================

/** 컬러 피커 UI 상수 */
object ColorPickerConstants {
    /** 팔레트 스와치 그리드 열 수. 기본값: 6 */
    const val SWATCH_COLS = 6

    /** 각 스와치 셀 크기 (dp). 기본값: 40f */
    const val SWATCH_CELL_DP = 40f

    /** 스와치 셀 간격 (dp). 기본값: 4f */
    const val SWATCH_CELL_GAP_DP = 4f

    /** 스와치 셀 모서리 반경 (dp). 기본값: 8f */
    const val SWATCH_CORNER_DP = 8f

    /** HSV 패널 최대 너비 (dp). 기본값: 280f */
    const val HSV_PANEL_MAX_WIDTH_DP = 280f

    /** 색상 미리보기 박스 크기 (dp). 기본값: 40f */
    const val COLOR_PREVIEW_SIZE_DP = 40f

    /** Hue 슬라이더 한 스텝 (도). 기본값: 2f */
    const val HUE_STEP_DEG = 2f

    /** Saturation/Value 슬라이더 한 스텝. 기본값: 0.01f */
    const val SV_STEP = 0.01f

    /** Hex 입력 최대 길이 ('#' 포함 9자 = "#AARRGGBB"). 기본값: 9 */
    const val HEX_MAX_LEN = 9

    /** 피커 패널 카드 모서리 반경 (dp). 기본값: 20f */
    const val PANEL_CORNER_DP = 20f

    /** 피커 패널 외부 패딩 (dp). 기본값: 12f */
    const val PANEL_PADDING_DP = 12f

    /** 진입 scale 애니메이션 duration (ms). 기본값: 220 */
    const val OPEN_DURATION_MS = 220

    /** 셀렉터 링 테두리 두께 (dp). 기본값: 2.5f */
    const val SELECTOR_BORDER_DP = 2.5f
}
