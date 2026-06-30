package com.bridgeone.app.ui.components.touchpad

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * EdgeZoneCanvasGeometry Dp 헬퍼 단위 테스트 (Phase 4.7.8-B).
 *
 * [edgeHandleRect] — 핸들 사각형 Dp 위치/크기
 * [edgeStripRect]  — 스트립 사각형 Dp 위치/크기
 *
 * 두 함수 모두 순수 Dp 산술이므로 Robolectric 없이 JVM에서 실행 가능.
 */
class EdgeZoneCanvasGeometryTest {

    // 공통 치수
    private val canvasW = 400.dp
    private val canvasH = 600.dp
    private val edgeDp  = 60.dp
    private val handleDp = 40.dp

    // ── edgeHandleRect ───────────────────────────────────────────

    @Test
    fun `edgeHandleRect TOP - 중간 ratio`() {
        // ratio=0.5 → offsetX = 400*0.5 - 40/2 = 200 - 20 = 180
        val rect = edgeHandleRect(EntryEdge.TOP, canvasW, canvasH, edgeDp, handleDp, 0.5f)
        assertEquals(EdgeZoneDpRect(180.dp, 0.dp, 40.dp, 60.dp), rect)
    }

    @Test
    fun `edgeHandleRect TOP - ratio 0 (왼쪽 끝)`() {
        // offsetX = 0 - 20 = -20 (핸들이 절반 걸쳐 나감, 정상)
        val rect = edgeHandleRect(EntryEdge.TOP, canvasW, canvasH, edgeDp, handleDp, 0f)
        assertEquals(EdgeZoneDpRect((-20).dp, 0.dp, 40.dp, 60.dp), rect)
    }

    @Test
    fun `edgeHandleRect BOTTOM - 1_4 ratio`() {
        // ratio=0.25 → offsetX=400*0.25-20=80, offsetY=600-60=540
        val rect = edgeHandleRect(EntryEdge.BOTTOM, canvasW, canvasH, edgeDp, handleDp, 0.25f)
        assertEquals(EdgeZoneDpRect(80.dp, 540.dp, 40.dp, 60.dp), rect)
    }

    @Test
    fun `edgeHandleRect BOTTOM - ratio 1 (오른쪽 끝)`() {
        // offsetX=400-20=380, offsetY=540
        val rect = edgeHandleRect(EntryEdge.BOTTOM, canvasW, canvasH, edgeDp, handleDp, 1f)
        assertEquals(EdgeZoneDpRect(380.dp, 540.dp, 40.dp, 60.dp), rect)
    }

    @Test
    fun `edgeHandleRect LEFT - 중간 ratio`() {
        // ratio=0.5 → offsetX=0, offsetY=600*0.5-20=280
        val rect = edgeHandleRect(EntryEdge.LEFT, canvasW, canvasH, edgeDp, handleDp, 0.5f)
        assertEquals(EdgeZoneDpRect(0.dp, 280.dp, 60.dp, 40.dp), rect)
    }

    @Test
    fun `edgeHandleRect RIGHT - 중간 ratio`() {
        // offsetX=400-60=340, offsetY=600*0.5-20=280
        val rect = edgeHandleRect(EntryEdge.RIGHT, canvasW, canvasH, edgeDp, handleDp, 0.5f)
        assertEquals(EdgeZoneDpRect(340.dp, 280.dp, 60.dp, 40.dp), rect)
    }

    @Test
    fun `edgeHandleRect RIGHT - ratio 1 (아래 끝)`() {
        // offsetY=600*1-20=580
        val rect = edgeHandleRect(EntryEdge.RIGHT, canvasW, canvasH, edgeDp, handleDp, 1f)
        assertEquals(EdgeZoneDpRect(340.dp, 580.dp, 60.dp, 40.dp), rect)
    }

    // ── edgeStripRect ─────────────────────────────────────────────

    @Test
    fun `edgeStripRect TOP - 중간 구간`() {
        // alongStart=0.25, alongLen=0.5 → offsetX=100, width=200, offsetY=0, height=60
        val rect = edgeStripRect(EntryEdge.TOP, canvasW, canvasH, edgeDp, 0.25f, 0.5f)
        assertEquals(EdgeZoneDpRect(100.dp, 0.dp, 200.dp, 60.dp), rect)
    }

    @Test
    fun `edgeStripRect TOP - 전체 구간`() {
        val rect = edgeStripRect(EntryEdge.TOP, canvasW, canvasH, edgeDp, 0f, 1f)
        assertEquals(EdgeZoneDpRect(0.dp, 0.dp, 400.dp, 60.dp), rect)
    }

    @Test
    fun `edgeStripRect TOP - 길이 0 (단일 점)`() {
        val rect = edgeStripRect(EntryEdge.TOP, canvasW, canvasH, edgeDp, 0.5f, 0f)
        assertEquals(EdgeZoneDpRect(200.dp, 0.dp, 0.dp, 60.dp), rect)
    }

    @Test
    fun `edgeStripRect BOTTOM - 중간 구간`() {
        // offsetX=100, offsetY=600-60=540, width=200, height=60
        val rect = edgeStripRect(EntryEdge.BOTTOM, canvasW, canvasH, edgeDp, 0.25f, 0.5f)
        assertEquals(EdgeZoneDpRect(100.dp, 540.dp, 200.dp, 60.dp), rect)
    }

    @Test
    fun `edgeStripRect LEFT - 중간 구간`() {
        // offsetX=0, offsetY=600*0.25=150, width=60, height=600*0.5=300
        val rect = edgeStripRect(EntryEdge.LEFT, canvasW, canvasH, edgeDp, 0.25f, 0.5f)
        assertEquals(EdgeZoneDpRect(0.dp, 150.dp, 60.dp, 300.dp), rect)
    }

    @Test
    fun `edgeStripRect RIGHT - 전체 구간`() {
        // offsetX=400-60=340, offsetY=0, width=60, height=600
        val rect = edgeStripRect(EntryEdge.RIGHT, canvasW, canvasH, edgeDp, 0f, 1f)
        assertEquals(EdgeZoneDpRect(340.dp, 0.dp, 60.dp, 600.dp), rect)
    }

    @Test
    fun `edgeStripRect RIGHT - 하단 절반`() {
        // offsetX=340, offsetY=600*0.5=300, width=60, height=600*0.5=300
        val rect = edgeStripRect(EntryEdge.RIGHT, canvasW, canvasH, edgeDp, 0.5f, 0.5f)
        assertEquals(EdgeZoneDpRect(340.dp, 300.dp, 60.dp, 300.dp), rect)
    }
}
