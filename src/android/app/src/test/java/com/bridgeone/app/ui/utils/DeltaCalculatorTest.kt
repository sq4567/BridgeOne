package com.bridgeone.app.ui.utils

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.bridgeone.app.ui.common.AppIconDef
import com.bridgeone.app.ui.common.CurveNode
import com.bridgeone.app.ui.components.touchpad.DynamicsAlgorithm
import com.bridgeone.app.ui.components.touchpad.PointerDynamicsPreset
import org.junit.Assert.*
import org.junit.Test

/**
 * DeltaCalculator 단위 테스트
 *
 * Phase 4.7.2-B: 리팩토링 안전망. 이 테스트들이 그린 상태를 유지해야
 * 이후 파일 분해 단계에서 회귀가 없음을 자동으로 보증한다.
 */
class DeltaCalculatorTest {

    // --- 픽스처 ---

    /** applyPointerDynamics는 icon 필드에 접근하지 않으므로 최소한의 ImageVector로 구성 */
    private val testIcon = AppIconDef(
        ImageVector.Builder(
            name = "TestIcon",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).build()
    )

    private fun preset(
        algo: DynamicsAlgorithm,
        intensity: Float = 1.0f,
        threshold: Float = 10f,
        maxMult: Float = 3.0f
    ) = PointerDynamicsPreset(
        name = "test",
        algorithm = algo,
        intensityFactor = intensity,
        velocityThresholdDpMs = threshold,
        maxMultiplier = maxMult,
        icon = testIcon,
        description = ""
    )

    /** density=1f → 1dp = 1px 환경 */
    private val density1x = Density(1f)

    // ======================================================
    // calculateDelta
    // ======================================================

    @Test
    fun calculateDelta_positiveMovement() {
        val result = DeltaCalculator.calculateDelta(Offset(100f, 150f), Offset(110f, 145f))
        assertEquals("deltaX", 10f, result.x, 0.001f)
        assertEquals("deltaY", -5f, result.y, 0.001f)
    }

    @Test
    fun calculateDelta_zeroMovement() {
        val result = DeltaCalculator.calculateDelta(Offset(50f, 50f), Offset(50f, 50f))
        assertEquals("deltaX zero", 0f, result.x, 0.001f)
        assertEquals("deltaY zero", 0f, result.y, 0.001f)
    }

    @Test
    fun calculateDelta_negativeDirection() {
        val result = DeltaCalculator.calculateDelta(Offset(100f, 100f), Offset(90f, 110f))
        assertEquals("deltaX negative", -10f, result.x, 0.001f)
        assertEquals("deltaY positive", 10f, result.y, 0.001f)
    }

    // ======================================================
    // normalizeOnly
    // ======================================================

    @Test
    fun normalizeOnly_withinRange_unchanged() {
        val result = DeltaCalculator.normalizeOnly(Offset(50f, -50f))
        assertEquals("x within range", 50f, result.x, 0.001f)
        assertEquals("y within range", -50f, result.y, 0.001f)
    }

    @Test
    fun normalizeOnly_exactly127_boundary() {
        val result = DeltaCalculator.normalizeOnly(Offset(127f, -127f))
        assertEquals("x exactly 127", 127f, result.x, 0.001f)
        assertEquals("y exactly -127", -127f, result.y, 0.001f)
    }

    @Test
    fun normalizeOnly_above127_clamped() {
        val result = DeltaCalculator.normalizeOnly(Offset(200f, 128f))
        assertEquals("x clamped to 127", 127f, result.x, 0.001f)
        assertEquals("y clamped to 127", 127f, result.y, 0.001f)
    }

    @Test
    fun normalizeOnly_belowNeg127_clamped() {
        val result = DeltaCalculator.normalizeOnly(Offset(-200f, -128f))
        assertEquals("x clamped to -127", -127f, result.x, 0.001f)
        assertEquals("y clamped to -127", -127f, result.y, 0.001f)
    }

    @Test
    fun normalizeOnly_axisIndependent() {
        // X 넘침, Y 정상 → X만 클램프
        val result = DeltaCalculator.normalizeOnly(Offset(300f, 10f))
        assertEquals("x clamped", 127f, result.x, 0.001f)
        assertEquals("y untouched", 10f, result.y, 0.001f)
    }

    // ======================================================
    // applyDeadZone  (density 1f → DEAD_ZONE = 5dp = 5px)
    // ======================================================

    @Test
    fun applyDeadZone_belowThreshold_zeroed() {
        // 4.9px < 5px deadzone → 0
        val result = DeltaCalculator.applyDeadZone(density1x, Offset(4.9f, -4.9f))
        assertEquals("x below deadzone", 0f, result.x, 0.001f)
        assertEquals("y below deadzone", 0f, result.y, 0.001f)
    }

    @Test
    fun applyDeadZone_aboveThreshold_passes() {
        // 10px > 5px → 통과
        val result = DeltaCalculator.applyDeadZone(density1x, Offset(10f, -10f))
        assertEquals("x passes deadzone", 10f, result.x, 0.001f)
        assertEquals("y passes deadzone", -10f, result.y, 0.001f)
    }

    @Test
    fun applyDeadZone_axisIndependent() {
        // X 초과, Y 미달 → Y만 0
        val result = DeltaCalculator.applyDeadZone(density1x, Offset(20f, 3f))
        assertEquals("x passes", 20f, result.x, 0.001f)
        assertEquals("y zeroed", 0f, result.y, 0.001f)
    }

    @Test
    fun applyDeadZone_largeValue_clamped() {
        val result = DeltaCalculator.applyDeadZone(density1x, Offset(200f, -200f))
        assertEquals("x clamped to 127", 127f, result.x, 0.001f)
        assertEquals("y clamped to -127", -127f, result.y, 0.001f)
    }

    // ======================================================
    // determineRightAngleAxis
    // ======================================================

    @Test
    fun determineRightAngleAxis_belowLockDist_undecided() {
        val axis = DeltaCalculator.determineRightAngleAxis(5f, 5f, lockDistPx = 20f, deadbandDeg = 10f)
        assertEquals("below lockDist → UNDECIDED", RightAngleAxis.UNDECIDED, axis)
    }

    @Test
    fun determineRightAngleAxis_pureHorizontal() {
        // angle = 0° (atan2(0, 100)) → HORIZONTAL
        val axis = DeltaCalculator.determineRightAngleAxis(100f, 0f, lockDistPx = 10f, deadbandDeg = 10f)
        assertEquals("pure horizontal → HORIZONTAL", RightAngleAxis.HORIZONTAL, axis)
    }

    @Test
    fun determineRightAngleAxis_pureVertical() {
        // angle = 90° (atan2(100, 0)) → VERTICAL
        val axis = DeltaCalculator.determineRightAngleAxis(0f, 100f, lockDistPx = 10f, deadbandDeg = 10f)
        assertEquals("pure vertical → VERTICAL", RightAngleAxis.VERTICAL, axis)
    }

    @Test
    fun determineRightAngleAxis_diagonal_undecided() {
        // angle = 45° → deadband(10°) 내 [35°~55°] → UNDECIDED
        val axis = DeltaCalculator.determineRightAngleAxis(100f, 100f, lockDistPx = 10f, deadbandDeg = 10f)
        assertEquals("diagonal 45° in deadband → UNDECIDED", RightAngleAxis.UNDECIDED, axis)
    }

    @Test
    fun determineRightAngleAxis_nearlyHorizontal_horizontal() {
        // angle ≈ 10° < 45-10=35° → HORIZONTAL
        val axis = DeltaCalculator.determineRightAngleAxis(100f, 18f, lockDistPx = 10f, deadbandDeg = 10f)
        assertEquals("nearly horizontal → HORIZONTAL", RightAngleAxis.HORIZONTAL, axis)
    }

    @Test
    fun determineRightAngleAxis_nearlyVertical_vertical() {
        // angle ≈ 80° > 45+10=55° → VERTICAL
        val axis = DeltaCalculator.determineRightAngleAxis(18f, 100f, lockDistPx = 10f, deadbandDeg = 10f)
        assertEquals("nearly vertical → VERTICAL", RightAngleAxis.VERTICAL, axis)
    }

    // ======================================================
    // applyRightAngleLock
    // ======================================================

    @Test
    fun applyRightAngleLock_horizontal_zerosY() {
        val result = DeltaCalculator.applyRightAngleLock(Offset(10f, 5f), RightAngleAxis.HORIZONTAL)
        assertEquals("x preserved", 10f, result.x, 0.001f)
        assertEquals("y zeroed", 0f, result.y, 0.001f)
    }

    @Test
    fun applyRightAngleLock_vertical_zerosX() {
        val result = DeltaCalculator.applyRightAngleLock(Offset(10f, 5f), RightAngleAxis.VERTICAL)
        assertEquals("x zeroed", 0f, result.x, 0.001f)
        assertEquals("y preserved", 5f, result.y, 0.001f)
    }

    @Test
    fun applyRightAngleLock_undecided_unchanged() {
        val result = DeltaCalculator.applyRightAngleLock(Offset(10f, 5f), RightAngleAxis.UNDECIDED)
        assertEquals("x unchanged", 10f, result.x, 0.001f)
        assertEquals("y unchanged", 5f, result.y, 0.001f)
    }

    // ======================================================
    // applyPointerDynamics
    // ======================================================

    @Test
    fun applyPointerDynamics_none_noAcceleration() {
        val result = DeltaCalculator.applyPointerDynamics(10f, 50f, preset(DynamicsAlgorithm.NONE))
        assertEquals("NONE: multiplier=1.0", 10f, result, 0.001f)
    }

    @Test
    fun applyPointerDynamics_windowsEpp_belowThreshold_noAcceleration() {
        // velocity(5) < threshold(10) → multiplier = 1.0
        val p = preset(DynamicsAlgorithm.WINDOWS_EPP, intensity = 2.0f, threshold = 10f)
        val result = DeltaCalculator.applyPointerDynamics(10f, 5f, p)
        assertEquals("WINDOWS_EPP below threshold → 1.0", 10f, result, 0.001f)
    }

    @Test
    fun applyPointerDynamics_windowsEpp_aboveThreshold_accelerates() {
        // velocity(30) > threshold(10) → sigmoid 계산 → result > rawDelta
        val p = preset(DynamicsAlgorithm.WINDOWS_EPP, intensity = 2.0f, threshold = 10f, maxMult = 10f)
        val result = DeltaCalculator.applyPointerDynamics(10f, 30f, p)
        assertTrue("WINDOWS_EPP above threshold accelerates", result > 10f)
    }

    @Test
    fun applyPointerDynamics_linear_belowThreshold_noAcceleration() {
        // velocity(10) <= threshold(20) → excess = max(0, (10-20)/20) = 0 → multiplier = 1.0
        val p = preset(DynamicsAlgorithm.LINEAR, intensity = 1.0f, threshold = 20f)
        val result = DeltaCalculator.applyPointerDynamics(10f, 10f, p)
        assertEquals("LINEAR below threshold → 1.0", 10f, result, 0.001f)
    }

    @Test
    fun applyPointerDynamics_linear_aboveThreshold_proportional() {
        // velocity=30, threshold=10 → excess=(30-10)/10=2.0, intensity=1.0 → multiplier=3.0
        val p = preset(DynamicsAlgorithm.LINEAR, intensity = 1.0f, threshold = 10f, maxMult = 10f)
        val result = DeltaCalculator.applyPointerDynamics(10f, 30f, p)
        assertEquals("LINEAR multiplier=3.0 → result=30", 30f, result, 0.1f)
    }

    @Test
    fun applyPointerDynamics_maxMultiplierClamp() {
        // maxMult=2.0 → 고속에서도 rawDelta × 2.0이 상한
        val p = preset(DynamicsAlgorithm.LINEAR, intensity = 10f, threshold = 1f, maxMult = 2.0f)
        val result = DeltaCalculator.applyPointerDynamics(10f, 100f, p)
        assertEquals("clamped to maxMultiplier=2.0", 20f, result, 0.001f)
    }

    // ======================================================
    // interpolateCurve
    // ======================================================

    @Test
    fun interpolateCurve_emptyList_returns1() {
        assertEquals("empty list → 1.0", 1.0f, DeltaCalculator.interpolateCurve(emptyList(), 50f), 0.001f)
    }

    @Test
    fun interpolateCurve_belowFirstNode_returnsFirst() {
        val curve = listOf(CurveNode(10f, 2f), CurveNode(20f, 3f))
        assertEquals("below first node → first.multiplier", 2.0f, DeltaCalculator.interpolateCurve(curve, 5f), 0.001f)
    }

    @Test
    fun interpolateCurve_aboveLastNode_returnsLast() {
        val curve = listOf(CurveNode(10f, 2f), CurveNode(20f, 4f))
        assertEquals("above last node → last.multiplier", 4.0f, DeltaCalculator.interpolateCurve(curve, 30f), 0.001f)
    }

    @Test
    fun interpolateCurve_midpoint_linearInterpolation() {
        // velocity=10, curve: [0→1.0, 20→3.0] → t=0.5 → 1 + 0.5*(3-1) = 2.0
        val curve = listOf(CurveNode(0f, 1f), CurveNode(20f, 3f))
        assertEquals("midpoint t=0.5 → 2.0", 2.0f, DeltaCalculator.interpolateCurve(curve, 10f), 0.001f)
    }

    @Test
    fun interpolateCurve_singleNode_alwaysReturnsIt() {
        val curve = listOf(CurveNode(10f, 2.5f))
        assertEquals("single node below", 2.5f, DeltaCalculator.interpolateCurve(curve, 5f), 0.001f)
        assertEquals("single node above", 2.5f, DeltaCalculator.interpolateCurve(curve, 20f), 0.001f)
    }

    // ======================================================
    // applyCustomDynamics (히스테리시스)
    // ======================================================

    @Test
    fun applyCustomDynamics_accelerating_usesAccelCurve() {
        val accel = listOf(CurveNode(0f, 3f))  // 항상 3x
        val decel = listOf(CurveNode(0f, 1f))  // 항상 1x
        // velocity(20) > previous(10) → 가속 곡선 → 10 * 3 = 30
        val result = DeltaCalculator.applyCustomDynamics(10f, 20f, 10f, accel, decel)
        assertEquals("accelerating → accel curve", 30f, result, 0.001f)
    }

    @Test
    fun applyCustomDynamics_decelerating_usesDecelCurve() {
        val accel = listOf(CurveNode(0f, 3f))
        val decel = listOf(CurveNode(0f, 1f))
        // velocity(5) < previous(10) → 감속 곡선 → 10 * 1 = 10
        val result = DeltaCalculator.applyCustomDynamics(10f, 5f, 10f, accel, decel)
        assertEquals("decelerating → decel curve", 10f, result, 0.001f)
    }

    @Test
    fun applyCustomDynamics_equalVelocity_usesDecelCurve() {
        val accel = listOf(CurveNode(0f, 3f))
        val decel = listOf(CurveNode(0f, 1f))
        // velocity == previous → strictly NOT greater → 감속 곡선 선택
        val result = DeltaCalculator.applyCustomDynamics(10f, 10f, 10f, accel, decel)
        assertEquals("equal velocity → decel curve", 10f, result, 0.001f)
    }
}
