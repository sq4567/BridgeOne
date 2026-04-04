package com.bridgeone.app.ui.components.touchpad

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.bridgeone.app.ui.common.ScrollConstants.SCROLL_GUIDELINE_AXIS_ROTATION_MS
import com.bridgeone.app.ui.common.ScrollConstants.SCROLL_GUIDELINE_SPACING_DP
import com.bridgeone.app.ui.common.ScrollConstants.SCROLL_GUIDELINE_SPRING_DAMPING
import com.bridgeone.app.ui.common.ScrollConstants.SCROLL_GUIDELINE_SPRING_STIFFNESS
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.sqrt

/** 일반 스크롤 가이드라인 색상 (초록) — TouchpadColors.kt 참조 */
private val ScrollGuidelineColorNormal = TouchpadColorGreen

/** 무한 스크롤 가이드라인 색상 (빨강) — TouchpadColors.kt 참조 */
private val ScrollGuidelineColorInfinite = TouchpadColorRed

/** 일반 선 두께 */
private val GuidelineStrokeWidthThin = 1.dp

/** 굵은 선 두께 (매 MAJOR_LINE_INTERVAL번째) */
private val GuidelineStrokeWidthThick = 3.dp

/** 몇 번째 선마다 굵은 선을 그릴지 */
private const val MAJOR_LINE_INTERVAL = 4


/**
 * 스크롤 가이드라인 Composable (Phase 4.3.3 ~ 4.3.4)
 *
 * 스크롤 모드에서 확정된 축 방향으로 **등간격 다중 선**을 표시합니다.
 * 스크롤 단위 전송 시마다 [targetOffset]이 업데이트되면 spring 애니메이션으로
 * 전체 선 패턴이 함께 이동합니다.
 *
 * **표시 동작:**
 * - [isVisible] = true: fade-in (200ms)
 * - [isVisible] = false: fade-out (400ms)
 *
 * **선 패턴:**
 * - 간격: [SCROLL_GUIDELINE_SPACING_DP] dp
 * - 항상 수평선으로 그린 뒤 [scrollAxis]에 따라 회전 (VERTICAL=0°, HORIZONTAL=90°)
 * - 축 전환 시 90° 회전 tween 애니메이션 ([SCROLL_GUIDELINE_AXIS_ROTATION_MS]ms)
 * - 회전 중에도 화면을 완전히 채우기 위해 대각선 길이만큼 선을 확장
 *
 * **색상 (Phase 4.3.4):**
 * - [scrollMode] == NORMAL_SCROLL → 초록(#84E268)
 * - [scrollMode] == INFINITE_SCROLL → 빨강(#F32121)
 *
 * @param isVisible 가이드라인 표시 여부
 * @param scrollAxis 확정된 스크롤 축 (VERTICAL=수평선들, HORIZONTAL=수직선들)
 * @param targetOffset 목표 오프셋 (dp 단위, unbounded, 스크롤 틱마다 업데이트)
 * @param scrollMode 현재 스크롤 모드 (색상 결정)
 * @param modifier 외부 Modifier
 */
@Composable
fun ScrollGuideline(
    isVisible: Boolean,
    scrollAxis: ScrollAxis,
    targetOffset: Float,
    scrollMode: ScrollMode = ScrollMode.NORMAL_SCROLL,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(400)),
        modifier = modifier
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            if (scrollAxis == ScrollAxis.UNDECIDED) return@BoxWithConstraints

            // 축에 따른 목표 회전각: VERTICAL=0° (수평선), HORIZONTAL=90° (수직선)
            val targetAngle = if (scrollAxis == ScrollAxis.HORIZONTAL) 90f else 0f

            // spring으로 오프셋 추적 (dp 단위, unbounded)
            val animOffset = remember { Animatable(0f) }
            // 축 전환 회전각 애니메이션
            val animAngle = remember { Animatable(targetAngle) }

            val density = LocalDensity.current
            val strokeThinPx = with(density) { GuidelineStrokeWidthThin.toPx() }
            val strokeThickPx = with(density) { GuidelineStrokeWidthThick.toPx() }
            val spacingPx = SCROLL_GUIDELINE_SPACING_DP * density.density

            // 오프셋 애니메이션
            // 무한 스크롤: 드래그/관성 중 targetOffset이 매 프레임 변하므로 snapTo로 즉각 추적
            // 일반 스크롤: 단위별 업데이트이므로 spring으로 부드럽게 이동
            LaunchedEffect(targetOffset, scrollMode) {
                if (scrollMode == ScrollMode.INFINITE_SCROLL) {
                    animOffset.snapTo(targetOffset)
                } else {
                    animOffset.animateTo(
                        targetValue = targetOffset,
                        animationSpec = spring(
                            stiffness = SCROLL_GUIDELINE_SPRING_STIFFNESS,
                            dampingRatio = SCROLL_GUIDELINE_SPRING_DAMPING
                        )
                    )
                }
            }

            // 축 전환 회전 애니메이션
            LaunchedEffect(scrollAxis) {
                val newAngle = if (scrollAxis == ScrollAxis.HORIZONTAL) 90f else 0f
                animAngle.animateTo(
                    targetValue = newAngle,
                    animationSpec = tween(
                        durationMillis = SCROLL_GUIDELINE_AXIS_ROTATION_MS,
                        easing = FastOutSlowInEasing
                    )
                )
            }

            // 스크롤 모드에 따른 색상 선택
            val guidelineColor = if (scrollMode == ScrollMode.INFINITE_SCROLL) {
                ScrollGuidelineColorInfinite
            } else {
                ScrollGuidelineColorNormal
            }

            Canvas(modifier = Modifier.fillMaxSize()) {
                // 90° 시계 회전 시 Y↑ → X← 로 방향이 반전되므로 가로 스크롤은 부호 반전
                val sign = if (scrollAxis == ScrollAxis.HORIZONTAL) -1f else 1f
                val rawOffsetPx = animOffset.value * density.density * sign
                val offsetPx = ((rawOffsetPx % spacingPx) + spacingPx) % spacingPx
                val majorPhase = floor(rawOffsetPx / spacingPx).toInt()

                // 회전 중에도 화면을 완전히 덮기 위해 대각선 길이 사용
                val diagonal = sqrt(size.width * size.width + size.height * size.height)
                val halfDiag = diagonal / 2f

                rotate(degrees = animAngle.value, pivot = center) {
                    // 항상 수평선으로 그리고, 회전으로 방향 전환
                    val lineStartX = center.x - halfDiag
                    val lineEndX = center.x + halfDiag

                    // 대각선 범위만큼 선을 배치
                    val yMin = center.y - halfDiag
                    val yMax = center.y + halfDiag
                    val startIndex = floor((yMin - offsetPx) / spacingPx).toInt()
                    val endIndex = ceil((yMax - offsetPx) / spacingPx).toInt()

                    for (i in startIndex..endIndex) {
                        val y = i * spacingPx + offsetPx
                        val isMajor = Math.floorMod(i - majorPhase, MAJOR_LINE_INTERVAL) == 0
                        drawLine(
                            color = guidelineColor,
                            start = Offset(lineStartX, y),
                            end = Offset(lineEndX, y),
                            strokeWidth = if (isMajor) strokeThickPx else strokeThinPx
                        )
                    }
                }
            }
        }
    }
}
