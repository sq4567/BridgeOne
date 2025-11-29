package com.bridgeone.app.ui.utils

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.math.abs

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

