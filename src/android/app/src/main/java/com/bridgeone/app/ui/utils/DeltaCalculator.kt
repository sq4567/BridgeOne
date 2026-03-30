package com.bridgeone.app.ui.utils

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.bridgeone.app.ui.components.touchpad.DynamicsAlgorithm
import com.bridgeone.app.ui.components.touchpad.PointerDynamicsPreset
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.exp

/**
 * 직각 이동 모드의 축 잠금 상태
 *
 * [DeltaCalculator.determineRightAngleAxis]로 판정하고,
 * [DeltaCalculator.applyRightAngleLock]으로 델타에 적용합니다.
 * 상태는 TouchpadWrapper의 제스처 루프에서 로컬 변수로 관리됩니다.
 */
enum class RightAngleAxis {
    UNDECIDED,   // 아직 주축 미결정
    HORIZONTAL,  // X축으로 잠금 (Y 이동량 = 0)
    VERTICAL     // Y축으로 잠금 (X 이동량 = 0)
}

/**
 * 터치 좌표 델타 계산 및 데드존 보상을 담당하는 유틸리티 클래스
 *
 * 이 클래스는 Jetpack Compose의 터치 이벤트 좌표를 기반으로:
 * 1. 이전 위치와 현재 위치의 상대 이동값(델타) 계산
 * 2. 화면 밀도(density)에 따른 dp → pixel 변환
 * 3. 작은 손떨림을 무시하는 데드존 보상
 * 4. 최종 델타 값을 -127 ~ 127 범위로 정규화
 *
 * 를 수행합니다.
 *
 * Context7 기반 정보:
 * - LocalDensity 사용: https://developer.android.com/develop/ui/compose
 * - dp → pixel 변환 패턴: with(LocalDensity.current) { dpValue.toPx() }
 */
object DeltaCalculator {
    /**
     * 터치 데드존 임계값 (dp 단위)
     *
     * 터치 시작점에서 이 값 이상 이동해야 드래그로 인식합니다.
     * 단위: 5dp (화면 밀도에 따라 pixel로 변환됨)
     *
     * 예시:
     * - 밀도 1.0x 장치: 5px
     * - 밀도 2.0x 장치: 10px
     * - 밀도 3.0x 장치: 15px
     */
    private val DEAD_ZONE_THRESHOLD = 5.dp

    /**
     * 델타 값의 최대 절댓값 (signed byte 범위)
     *
     * BridgeFrame의 deltaX, deltaY는 각각 1바이트 signed 정수이므로
     * 범위는 -128 ~ 127입니다.
     */
    private const val MAX_DELTA_VALUE = 127

    /**
     * 두 터치 위치 간의 상대 이동값(델타)을 계산합니다.
     *
     * 현재 위치에서 이전 위치를 뺀 상대 이동값을 반환하며,
     * 이를 통해 터치의 방향과 거리를 파악합니다.
     *
     * @param previousPosition 이전 터치 위치 (Offset, Compose 좌표계)
     * @param currentPosition 현재 터치 위치 (Offset, Compose 좌표계)
     * @return 상대 이동값 (현재 - 이전), X, Y 축이 분리되어 있음
     *
     * 예시:
     * ```
     * previousPosition = Offset(100f, 150f)
     * currentPosition = Offset(110f, 145f)
     * 결과 = Offset(10f, -5f)  // X축 +10, Y축 -5
     * ```
     */
    fun calculateDelta(
        previousPosition: Offset,
        currentPosition: Offset
    ): Offset = Offset(
        x = currentPosition.x - previousPosition.x,
        y = currentPosition.y - previousPosition.y
    )

    /**
     * dp 단위의 좌표 값을 pixel 단위로 변환합니다.
     *
     * Jetpack Compose의 포인터 이벤트는 dp 좌표계로 제공되므로,
     * BridgeFrame의 (-128 ~ 127) 범위로 매핑하기 전에 pixel 단위로 변환해야 합니다.
     *
     * LocalDensity.current를 통해 현재 화면의 밀도 정보를 접근할 수 있습니다.
     *
     * 사용 예:
     * ```kotlin
     * with(LocalDensity.current) {
     *     val pixelValue = deltaOffsetDp.x.toPx()
     * }
     * ```
     *
     * @param density 현재 화면의 밀도 정보
     * @param deltaOffsetDp dp 단위의 상대 이동값
     * @return pixel 단위의 상대 이동값
     */
    fun convertDpToPixels(
        density: Density,
        deltaOffsetDp: Offset
    ): Offset = with(density) {
        Offset(
            // 델타 값을 Float로 간주하고 Dp로 변환
            x = (deltaOffsetDp.x).dp.toPx(),
            y = (deltaOffsetDp.y).dp.toPx()
        )
    }

    /**
     * 데드존 임계값을 pixel 단위로 변환합니다.
     *
     * 화면 밀도에 따라 데드존 임계값(15dp)을 pixel로 변환하여 반환합니다.
     *
     * @param density 현재 화면의 밀도 정보
     * @return pixel 단위의 데드존 임계값
     */
    fun getDeadZoneThresholdPx(density: Density): Float = with(density) {
        DEAD_ZONE_THRESHOLD.toPx()
    }

    /**
     * 데드존 보상 알고리즘을 적용합니다.
     *
     * 작은 손떨림을 무시하기 위해 다음 로직을 수행합니다:
     *
     * 1. **임계값 비교**: 상대 이동값의 절댓값이 임계값(15dp)보다 작으면 0으로 처리
     * 2. **범위 정규화**: 남은 값을 -127 ~ 127 범위로 제한
     * 3. **축 분리**: X, Y 축을 독립적으로 처리
     *
     * 수학식:
     * ```
     * if (|deltaPixel| < DEAD_ZONE_THRESHOLD) {
     *     return 0
     * } else {
     *     return clamp(deltaPixel, -127, 127)
     * }
     * ```
     *
     * 예시:
     * ```
     * 밀도: 1.0x (dp = px)
     *
     * deltaPixel = Offset(5f, -3f)
     * → 모두 15px 미만이므로 Offset(0, 0) 반환
     *
     * deltaPixel = Offset(20f, -10f)
     * → X축만 임계값 초과 (20px > 15px)
     * → Offset(20, 0) 반환 (Y는 여전히 작아서 0)
     *
     * deltaPixel = Offset(200f, -250f)
     * → 범위 초과, 정규화 필요
     * → Offset(127, -127) 반환 (범위 제한)
     * ```
     *
     * @param density 현재 화면의 밀도 정보
     * @param deltaPixel pixel 단위의 상대 이동값
     * @return 데드존 보상 및 정규화된 상대 이동값
     */
    fun applyDeadZone(
        density: Density,
        deltaPixel: Offset
    ): Offset {
        val deadZonePx = getDeadZoneThresholdPx(density)

        // X축 데드존 처리
        val compensatedX = if (abs(deltaPixel.x) < deadZonePx) {
            0f
        } else {
            deltaPixel.x
        }

        // Y축 데드존 처리
        val compensatedY = if (abs(deltaPixel.y) < deadZonePx) {
            0f
        } else {
            deltaPixel.y
        }

        // 범위 정규화 (-127 ~ 127)
        val normalizedX = compensatedX.toInt()
            .coerceIn(-MAX_DELTA_VALUE, MAX_DELTA_VALUE)
            .toFloat()
        val normalizedY = compensatedY.toInt()
            .coerceIn(-MAX_DELTA_VALUE, MAX_DELTA_VALUE)
            .toFloat()

        return Offset(normalizedX, normalizedY)
    }

    /**
     * 데드존 처리 없이 범위 정규화만 수행합니다.
     *
     * 데드존을 이미 탈출한 상태에서 델타 값을 -127 ~ 127 범위로 제한합니다.
     *
     * @param deltaPixel pixel 단위의 상대 이동값
     * @return 범위 정규화된 상대 이동값 (-127 ~ 127)
     */
    fun normalizeOnly(deltaPixel: Offset): Offset {
        val normalizedX = deltaPixel.x.toInt()
            .coerceIn(-MAX_DELTA_VALUE, MAX_DELTA_VALUE)
            .toFloat()
        val normalizedY = deltaPixel.y.toInt()
            .coerceIn(-MAX_DELTA_VALUE, MAX_DELTA_VALUE)
            .toFloat()

        return Offset(normalizedX, normalizedY)
    }

    /**
     * 직각 이동 모드의 주축을 판정합니다.
     *
     * 누적 이동 벡터의 크기가 [lockDistPx]를 초과하면 atan2 각도를 분석하여
     * 주축을 결정합니다. 대각선 데드밴드([deadbandDeg]) 내이면 UNDECIDED를 유지합니다.
     *
     * 각도 기준: 0° = 완전 수평(X축), 90° = 완전 수직(Y축)
     *
     * @param accumX 축 판정용 누적 X 이동량 (px)
     * @param accumY 축 판정용 누적 Y 이동량 (px)
     * @param lockDistPx 주축 확정을 시도할 누적 이동 임계값 (px)
     * @param deadbandDeg 대각선 데드밴드 각도 (°). 45° ± 이 범위 = UNDECIDED 유지
     * @return 판정된 축 (임계값 미달이면 항상 UNDECIDED)
     */
    fun determineRightAngleAxis(
        accumX: Float,
        accumY: Float,
        lockDistPx: Float,
        deadbandDeg: Float
    ): RightAngleAxis {
        val accumDist = Offset(accumX, accumY).getDistance()
        if (accumDist < lockDistPx) return RightAngleAxis.UNDECIDED

        // atan2(|ΔY|, |ΔX|): 0°=완전가로, 90°=완전세로
        val angleRad = atan2(abs(accumY), abs(accumX))
        val angleDeg = Math.toDegrees(angleRad.toDouble()).toFloat()

        return when {
            angleDeg < 45f - deadbandDeg -> RightAngleAxis.HORIZONTAL
            angleDeg > 45f + deadbandDeg -> RightAngleAxis.VERTICAL
            else -> RightAngleAxis.UNDECIDED  // 대각선 구간: 계속 누적
        }
    }

    /**
     * 직각 이동 모드의 축 잠금을 델타에 적용합니다.
     *
     * 잠긴 축과 반대 방향의 이동량을 0으로 만들어 커서가
     * 수평 또는 수직으로만 이동하도록 합니다.
     *
     * @param delta 원본 델타 (px)
     * @param axis 현재 잠긴 축
     * @return 축 잠금이 적용된 델타 (잠금 축 외의 성분 = 0)
     */
    fun applyRightAngleLock(delta: Offset, axis: RightAngleAxis): Offset {
        return when (axis) {
            RightAngleAxis.HORIZONTAL -> Offset(delta.x, 0f)
            RightAngleAxis.VERTICAL -> Offset(0f, delta.y)
            RightAngleAxis.UNDECIDED -> delta
        }
    }

    /**
     * 포인터 다이나믹스(커서 가속) 배율을 단일 축 델타에 적용합니다. (Phase 4.3.8)
     *
     * DPI 배율 적용 이후에 호출합니다.
     * 최종 파이프라인: rawDelta × dpiMultiplier → applyPointerDynamics → coerceIn(-127f, 127f)
     *
     * @param rawDelta      DPI 배율이 이미 적용된 단일 축 델타 (px)
     * @param velocityDpMs  현재 손가락 이동 속도 (dp/ms, 절댓값 0 이상)
     * @param preset        적용할 다이나믹스 프리셋
     * @return 다이나믹스 배율이 추가로 적용된 델타 (px, coerceIn 미포함)
     */
    fun applyPointerDynamics(
        rawDelta: Float,
        velocityDpMs: Float,
        preset: PointerDynamicsPreset
    ): Float {
        val multiplier = when (preset.algorithm) {
            DynamicsAlgorithm.NONE -> 1.0f

            DynamicsAlgorithm.WINDOWS_EPP -> {
                if (velocityDpMs < preset.velocityThresholdDpMs) {
                    1.0f
                } else {
                    // S커브 근사: sigmoid((v/threshold - 1) × 2)
                    val x = (velocityDpMs / preset.velocityThresholdDpMs - 1f) * 2f
                    val sigmoid = 1f / (1f + exp(-x))
                    1.0f + preset.intensityFactor * sigmoid
                }
            }

            DynamicsAlgorithm.LINEAR -> {
                val excess = maxOf(
                    0f,
                    (velocityDpMs - preset.velocityThresholdDpMs) / preset.velocityThresholdDpMs
                )
                1.0f + preset.intensityFactor * excess
            }
        }
        return rawDelta * multiplier.coerceIn(1.0f, preset.maxMultiplier)
    }

    /**
     * 델타 계산과 데드존 보상을 한 번에 수행하는 통합 함수입니다.
     *
     * 다음 순서로 처리합니다:
     * 1. 현재 위치 - 이전 위치의 델타 계산
     * 2. dp → pixel 변환
     * 3. 데드존 보상 및 범위 정규화
     *
     * @param density 현재 화면의 밀도 정보
     * @param previousPosition 이전 터치 위치 (Offset, dp)
     * @param currentPosition 현재 터치 위치 (Offset, dp)
     * @return 최종 보정된 상대 이동값 (pixel, -127 ~ 127 범위)
     *
     * 사용 예:
     * ```kotlin
     * val compensatedDelta = DeltaCalculator.calculateAndCompensate(
     *     density = LocalDensity.current,
     *     previousPosition = Offset(100f, 150f),
     *     currentPosition = Offset(115f, 140f)
     * )
     * // 결과: Offset(15f, -10f) 또는 Offset(0f, 0f) (임계값에 따라)
     * ```
     */
    fun calculateAndCompensate(
        density: Density,
        previousPosition: Offset,
        currentPosition: Offset
    ): Offset {
        // Step 1: 델타 계산
        val deltaOffsetDp = calculateDelta(previousPosition, currentPosition)

        // Step 2: dp → pixel 변환
        val deltaOffsetPx = convertDpToPixels(density, deltaOffsetDp)

        // Step 3: 데드존 보상
        return applyDeadZone(density, deltaOffsetPx)
    }
}

