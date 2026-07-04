package com.bridgeone.app.ui.common

object TouchpadIds {
    fun standardPage(pageIndex: Int) = "standard_page_$pageIndex"
    // Phase 4.8.9: Page 2 멀티 커서 패드별 엣지 존 할당 키 (패드 인덱스 0-based)
    fun standardPage2Pad(padIndex: Int) = "standard_page2_pad$padIndex"
    const val ESSENTIAL_PRIMARY = "essential_primary"
}
