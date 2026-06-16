package com.bridgeone.app.ui.common

// ============================================================
// ColorCategory — 색상 카테고리 분류
// ============================================================

/**
 * 컬러 피커 카테고리. 컬러 피커 1단계(카테고리 선택)에서 사용된다.
 *
 * @param id                 영속화/식별용 안정 키 (향후 확장 대비)
 * @param displayName        UI 표시 이름
 * @param representativeArgb 카테고리 셀에 표시할 대표색 ARGB 값 (Long, 0xFFRRGGBB 형식)
 */
enum class ColorCategory(
    val id: String,
    val displayName: String,
    val representativeArgb: Long,
) {
    WARM("warm", "따뜻한 색", 0xFFF32121),
    COOL("cool", "시원한 색", 0xFF2196F3),
    PASTEL("pastel", "파스텔", 0xFFFFC1CC),
    SPRING("spring", "봄", 0xFF84E268),
    SUMMER("summer", "여름", 0xFF00BCD4),
    AUTUMN("autumn", "가을", 0xFFD2691E),
    WINTER("winter", "겨울", 0xFFB0C4DE),
    MONO("mono", "무채색", 0xFF9E9E9E),
    METAL("metal", "메탈", 0xFFB8B8C0),
    SPACE("space", "우주", 0xFF3F51B5),
    APP("app", "BridgeOne", 0xFF20D8AD),
}

/**
 * 컬러 피커 1단계 카테고리 탭.
 * [All]은 enum에 없는 가상 카테고리로, 전체 색상을 한 번에 보여준다.
 */
sealed interface ColorCategoryTab {
    /** 전체 색상 (가상 카테고리). */
    object All : ColorCategoryTab

    /** 실제 카테고리 1개. */
    data class Real(val category: ColorCategory) : ColorCategoryTab
}
