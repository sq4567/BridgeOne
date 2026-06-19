package com.bridgeone.app.ui.components.touchpad

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * resolveSlot 단위 테스트
 *
 * Phase 4.7.6-D: DynamicsCurveEditor.kt의 순수 슬롯 매핑 함수 resolveSlot을 고정.
 * 드래그 시작점 기준 상대 이동량(dragDelta)으로 행/열 슬롯을 결정하는 로직 검증:
 * 행/열 이동·좁은행↔넓은행 편향·coerce 경계.
 */
class ResolveSlotTest {

    // DynamicsCurveEditor.kt의 ACTION_ROW_SLOTS 구조 복제 (private이라 직접 참조 불가)
    private val actionRows = listOf(
        listOf(7, 8),         // Row 0
        listOf(0, 10, 1, 3),  // Row 1
        listOf(2, 3),         // Row 2
        listOf(9),            // Row 3
        listOf(4, 6, 5),      // Row 4 (기준 행)
    )
    private val startRow = 4
    private val step = 10f

    @Test
    fun `이동 없음 - 기준 행 가운데 슬롯`() {
        // sourceCols=3 → resolvedStartCol=1 → ACTION_ROWS[4][1]=6
        assertEquals(6, resolveSlot(Offset(0f, 0f), step, actionRows, startRow))
    }

    @Test
    fun `좌우 이동 - 같은 행 내 인접 슬롯`() {
        assertEquals(4, resolveSlot(Offset(-10f, 0f), step, actionRows, startRow))
        assertEquals(5, resolveSlot(Offset(10f, 0f), step, actionRows, startRow))
    }

    @Test
    fun `위로 한 칸 - 단독 슬롯 행으로 이동`() {
        // row=3 (1칸 행) → col coerce 0 → ACTION_ROWS[3][0]=9
        assertEquals(9, resolveSlot(Offset(0f, -10f), step, actionRows, startRow))
    }

    @Test
    fun `가로 coerce 경계 - 오른쪽 끝 클램프`() {
        // col=(1+10).coerceIn(0,2)=2 → ACTION_ROWS[4][2]=5
        assertEquals(5, resolveSlot(Offset(100f, 0f), step, actionRows, startRow))
    }

    @Test
    fun `세로 coerce 경계 - 맨 위 행 가운데로 클램프`() {
        // row=(4-10).coerceIn(0,4)=0, 2칸 행 → resolvedStartCol=1 → ACTION_ROWS[0][1]=8
        assertEquals(8, resolveSlot(Offset(0f, -100f), step, actionRows, startRow))
    }

    @Test
    fun `넓은 행에서 좁은 행으로 - 왼쪽 편향`() {
        // startRow=1(4칸), startCol=3, 아래로 1칸 → row=2(2칸)
        // (3/3)*1=1.0 → toInt=1 → ACTION_ROWS[2][1]=3
        assertEquals(3, resolveSlot(Offset(0f, 10f), step, actionRows, startRow = 1, startCol = 3))
    }

    @Test
    fun `단일 슬롯 행에서 넓은 행으로 - 가운데 매핑`() {
        // startRow=3(1칸), startCol=0, 아래로 1칸 → row=4(3칸)
        // sourceCols<=1 → (3-1)/2=1 → ACTION_ROWS[4][1]=6
        assertEquals(6, resolveSlot(Offset(0f, 10f), step, actionRows, startRow = 3, startCol = 0))
    }

    @Test
    fun `저장 확인 슬롯 배치 - 2칸 단일 행 좌우 선택`() {
        // SAVE_CONFIRM_ROW_SLOTS = [[0, 1]], START_ROW=0
        assertEquals(0, resolveSlot(Offset(0f, 0f), step, SAVE_CONFIRM_ROW_SLOTS, SAVE_CONFIRM_START_ROW))
        assertEquals(1, resolveSlot(Offset(10f, 0f), step, SAVE_CONFIRM_ROW_SLOTS, SAVE_CONFIRM_START_ROW))
    }
}
