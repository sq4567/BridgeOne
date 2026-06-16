package com.bridgeone.app.ui.common

// ============================================================
// IconCategory — 아이콘 카테고리 분류
// ============================================================

/**
 * 아이콘 카테고리. 아이콘 서랍 1단계(카테고리 선택)에서 사용된다.
 *
 * @param id               영속화/식별용 안정 키 (현재 직렬화 미사용, 향후 확장 대비)
 * @param displayName      UI 표시 이름
 * @param representativeKey 카테고리 그리드 셀에 표시할 대표 아이콘 키 (IconRegistry.entries에 반드시 존재)
 */
enum class IconCategory(
    val id: String,
    val displayName: String,
    val representativeKey: String,
) {
    POINTER("pointer", "포인터", "Mouse"),
    ARROWS("arrows", "화살표", "ArrowForward"),
    MEDIA("media", "미디어", "PlayArrow"),
    EDIT("edit", "편집", "Edit"),
    SYSTEM("system", "시스템", "Settings"),
    SHAPES("shapes", "도형", "Circle"),
    SYMBOLS("symbols", "기호", "Star"),
    COMMUNICATION("comm", "통신", "Chat"),
    FILES("files", "파일", "Folder"),
    DATA("data", "데이터", "BarChart"),
    WEATHER("weather", "날씨", "WbSunny"),
    EMOTION("emotion", "감정", "Mood"),
    DEVICE("device", "장치", "Keyboard"),
}

/**
 * 아이콘 서랍 1단계 카테고리 탭.
 * [All]은 enum에 없는 가상 카테고리로, 전체 아이콘을 한 번에 보여준다.
 */
sealed interface IconCategoryTab {
    /** 전체 아이콘 (가상 카테고리). 대표 아이콘은 IconRegistry에서 지정. */
    object All : IconCategoryTab

    /** 실제 카테고리 1개. */
    data class Real(val category: IconCategory) : IconCategoryTab
}
