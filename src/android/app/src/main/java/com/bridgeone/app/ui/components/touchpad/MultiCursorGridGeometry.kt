package com.bridgeone.app.ui.components.touchpad

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

// ============================================================
// 멀티 커서 그리드 분할 헬퍼 함수 (Phase 4.8.3)
// ============================================================

/**
 * 터치패드 전체 영역([width] x [height])을 [cursorCount]개의 패드 영역으로 분할합니다.
 * 좌표는 px 단위이며 반환 리스트의 인덱스가 곧 패드 인덱스(pad1 = 0)입니다.
 *
 * 분할 방식 (`technical-specification-app.md` §2.2.6.1):
 * - N=2: 좌/우 50% (1×2)
 * - N=3: 좌/중/우 33% (1×3)
 * - N=4: 2×2 그리드, 행 우선 번호(좌상→우상→좌하→우하)
 */
internal fun divideGridAreas(width: Float, height: Float, cursorCount: Int): List<Rect> {
    require(cursorCount in MULTI_CURSOR_COUNT_MIN..MULTI_CURSOR_COUNT_MAX) {
        "cursorCount는 $MULTI_CURSOR_COUNT_MIN~$MULTI_CURSOR_COUNT_MAX 범위여야 함: $cursorCount"
    }
    return when (cursorCount) {
        2 -> listOf(
            Rect(0f, 0f, width / 2f, height),
            Rect(width / 2f, 0f, width, height)
        )
        3 -> listOf(
            Rect(0f, 0f, width / 3f, height),
            Rect(width / 3f, 0f, width * 2f / 3f, height),
            Rect(width * 2f / 3f, 0f, width, height)
        )
        else -> listOf( // 4
            Rect(0f, 0f, width / 2f, height / 2f),
            Rect(width / 2f, 0f, width, height / 2f),
            Rect(0f, height / 2f, width / 2f, height),
            Rect(width / 2f, height / 2f, width, height)
        )
    }
}

/**
 * [pos]가 속한 패드의 인덱스를 반환합니다. 어느 영역에도 속하지 않으면(경계 밖) -1을 반환합니다.
 * 경계선 위(오른쪽/아래쪽 끝 포함)는 마지막으로 매칭되는 영역에 귀속됩니다.
 */
internal fun hitTestPad(pos: Offset, areas: List<Rect>): Int {
    var matched = -1
    areas.forEachIndexed { index, rect ->
        val inX = pos.x >= rect.left && pos.x <= rect.right
        val inY = pos.y >= rect.top && pos.y <= rect.bottom
        if (inX && inY) matched = index
    }
    return matched
}
