package com.bridgeone.app.ui.splash

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min

// ============================================================
// Logo SVG Path 데이터 (viewBox: 0 0 669.6 669.6)
// ============================================================

/** 별 모양 Path: 4개 곡선 꼭짓점을 가진 별 (중심 334.8, 240) */
private const val STAR_PATH_DATA =
    "M334.8,465h0c0-124.3-100.7-225-225-225h0c124.3,0,225-100.7,225-225h0" +
    "c0,124.3,100.7,225,225,225h0c-124.3,0-225,100.7-225,225Z"

/** 브릿지 Path: Y=537.7 시작, 양쪽으로 확장되는 아치 구조 */
private const val BRIDGE_PATH_DATA =
    "M334.8,537.7c96,0,181.6,61.4,236.3,131.9h84.2" +
    "c-16.2-28.4-36.2-54.5-59.6-78-33.9-33.9-73.3-60.5-117.3-79.1" +
    "-45.5-19.3-93.8-29-143.6-29s-98.1,9.8-143.6,29" +
    "c-43.9,18.6-83.4,45.2-117.3,79.1-23.4,23.4-43.4,49.6-59.6,78h84.2" +
    "c54.7-70.6,140.3-131.9,236.3-131.9h0Z"

private const val VIEWBOX_SIZE = 669.6f

// 별 중심 좌표 (viewBox 기준)
private const val STAR_CENTER_X = 334.8f
private const val STAR_CENTER_Y = 240f

// 브릿지 중심 X 좌표 (clip 애니메이션 기준점)
private const val BRIDGE_CENTER_X = 334.8f

// 배경색: 스플래시 초기 배경 → Phase 5 전환 후 앱 배경
private val SPLASH_BG_COLOR = Color(0xFF6ACBFF)
private val APP_BG_COLOR = Color(0xFF2AA9FF)

// 로고 스케일: 화면 최소 변의 55%
private const val LOGO_SCALE_FACTOR = 0.55f

// ============================================================
// SplashScreen Composable
// ============================================================

@Composable
fun SplashScreen(onSplashComplete: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val onComplete by rememberUpdatedState(onSplashComplete)

    // 접근성: 애니메이션 비활성 설정 확인
    val animationsEnabled = remember {
        try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.TRANSITION_ANIMATION_SCALE,
                1.0f
            ) > 0f
        } catch (_: Exception) { true }
    }

    // 애니메이션 경과 시간 (0 ~ TOTAL_DURATION_MS)
    val elapsed = remember { Animatable(0f) }
    var splashDone by remember { mutableStateOf(false) }

    // SVG Path → Compose Path 변환 (최초 1회)
    val starPath = remember { PathParser().parsePathString(STAR_PATH_DATA).toPath() }
    val bridgePath = remember { PathParser().parsePathString(BRIDGE_PATH_DATA).toPath() }

    // 텍스트 레이아웃 사전 측정 (매 프레임 재측정 방지)
    val textMeasurer = rememberTextMeasurer()
    val textLayout = remember(textMeasurer) {
        textMeasurer.measure(
            AnnotatedString("BridgeOne"),
            TextStyle(
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }

    // 스플래시 완료 처리 (중복 호출 방지)
    fun finishSplash() {
        if (!splashDone) {
            splashDone = true
            onComplete()
        }
    }

    // 애니메이션 실행
    LaunchedEffect(Unit) {
        if (!animationsEnabled) {
            // 접근성: 애니메이션 비활성 시 최종 상태를 500ms 표시 후 완료
            elapsed.snapTo(SplashConstants.TOTAL_DURATION_MS.toFloat())
            delay(500L)
            finishSplash()
        } else {
            elapsed.animateTo(
                targetValue = SplashConstants.TOTAL_DURATION_MS.toFloat(),
                animationSpec = tween(
                    durationMillis = SplashConstants.TOTAL_DURATION_MS,
                    easing = LinearEasing
                )
            )
            finishSplash()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SPLASH_BG_COLOR)
            .pointerInput(Unit) {
                detectTapGestures {
                    // 500ms 이후 탭으로 스킵 가능
                    if (elapsed.value >= SplashConstants.SKIP_TAP_THRESHOLD_MS && !splashDone) {
                        scope.launch {
                            elapsed.snapTo(SplashConstants.TOTAL_DURATION_MS.toFloat())
                        }
                        finishSplash()
                    }
                }
            }
            .semantics { contentDescription = "BridgeOne 스플래시 스크린" }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (size.width <= 0f || size.height <= 0f) return@Canvas

            val ms = elapsed.value

            // viewBox → 화면 좌표 변환
            val canvasMin = min(size.width, size.height)
            val scale = canvasMin * LOGO_SCALE_FACTOR / VIEWBOX_SIZE
            val offsetX = (size.width - VIEWBOX_SIZE * scale) / 2f
            val offsetY = (size.height - VIEWBOX_SIZE * scale) / 2f

            // Phase 시작 시점 (상수 기반 누적 계산)
            val p1 = SplashConstants.PHASE_1_DURATION_MS.toFloat()
            val p2 = p1 + SplashConstants.PHASE_2_DURATION_MS
            val p3 = p2 + SplashConstants.PHASE_3_DURATION_MS
            val p4 = p3 + SplashConstants.PHASE_4_DURATION_MS
            val p5 = p4 + SplashConstants.PHASE_5_DURATION_MS
            val d2 = SplashConstants.PHASE_2_DURATION_MS.toFloat()
            val d3 = SplashConstants.PHASE_3_DURATION_MS.toFloat()
            val d5 = SplashConstants.PHASE_5_DURATION_MS.toFloat()
            val d6 = SplashConstants.PHASE_6_DURATION_MS.toFloat()

            // Phase 2: 브릿지 wipe 진행도
            val rawBridgeProg = ((ms - p1) / d2).coerceIn(0f, 1f)
            val bridgeProgress = FastOutSlowInEasing.transform(rawBridgeProg)

            // Phase 3: 별 회전
            val rawRotProg = ((ms - p2) / d3).coerceIn(0f, 1f)
            val starRotation = FastOutSlowInEasing.transform(rawRotProg) *
                SplashConstants.STAR_ROTATION_DEGREES

            // 별 스케일: 0 → 0.85 (Phase 3) → 0.85 (Phase 4) → 1.0 (Phase 5)
            val starScale = when {
                ms < p2 -> 0f
                ms < p3 -> {
                    val t = ((ms - p2) / d3).coerceIn(0f, 1f)
                    FastOutSlowInEasing.transform(t) * SplashConstants.STAR_SCALE_RATIO
                }
                ms < p4 -> SplashConstants.STAR_SCALE_RATIO
                ms < p5 -> {
                    val t = ((ms - p4) / d5).coerceIn(0f, 1f)
                    SplashConstants.STAR_SCALE_RATIO +
                        (1f - SplashConstants.STAR_SCALE_RATIO) * t
                }
                else -> 1f
            }

            // Phase 5: 방사형 배경 전환
            val bgTransition = ((ms - p4) / d5).coerceIn(0f, 1f)

            // Phase 6: 텍스트 알파
            val textAlpha = ((ms - p5) / d6).coerceIn(0f, 1f)

            // ── 렌더링 시작 ──

            // Phase 5: 방사형 배경 전환 (별 중심에서 확산)
            if (bgTransition > 0f) {
                val center = Offset(
                    offsetX + STAR_CENTER_X * scale,
                    offsetY + STAR_CENTER_Y * scale
                )
                val maxRadius = maxOf(size.width, size.height) * 1.5f
                drawCircle(
                    color = APP_BG_COLOR,
                    radius = maxRadius * bgTransition,
                    center = center
                )
            }

            // viewBox 좌표계에서 로고 렌더링
            withTransform({
                translate(offsetX, offsetY)
                scale(scale, scale, Offset.Zero)
            }) {
                // Phase 2: 브릿지 — 양쪽 끝에서 중앙으로 wipe-in
                if (ms >= p1 && bridgeProgress > 0f) {
                    val revealX = bridgeProgress * BRIDGE_CENTER_X

                    // 왼쪽 절반: 좌측 끝 → 중앙 방향
                    clipRect(
                        left = 0f,
                        top = 0f,
                        right = revealX,
                        bottom = VIEWBOX_SIZE
                    ) {
                        drawPath(path = bridgePath, color = Color.White)
                    }

                    // 오른쪽 절반: 우측 끝 → 중앙 방향
                    clipRect(
                        left = VIEWBOX_SIZE - revealX,
                        top = 0f,
                        right = VIEWBOX_SIZE,
                        bottom = VIEWBOX_SIZE
                    ) {
                        drawPath(path = bridgePath, color = Color.White)
                    }
                }

                // Phase 3-6: 별 — 회전 + 스케일
                if (starScale > 0f) {
                    withTransform({
                        translate(STAR_CENTER_X, STAR_CENTER_Y)
                        rotate(starRotation, Offset.Zero)
                        scale(starScale, starScale, Offset.Zero)
                        translate(-STAR_CENTER_X, -STAR_CENTER_Y)
                    }) {
                        drawPath(path = starPath, color = Color.White)
                    }
                }
            }

            // Phase 6: "BridgeOne" 텍스트 페이드인 (화면 좌표계)
            if (textAlpha > 0f) {
                val textX = (size.width - textLayout.size.width) / 2f
                val textY = offsetY + VIEWBOX_SIZE * scale + 16.dp.toPx()
                drawText(
                    textLayoutResult = textLayout,
                    color = Color.White.copy(alpha = textAlpha),
                    topLeft = Offset(textX, textY)
                )
            }
        }
    }
}

// ============================================================
// Preview
// ============================================================

@Preview(showBackground = true, backgroundColor = 0xFF2AA9FF)
@Composable
private fun SplashScreenPreview() {
    SplashScreen(onSplashComplete = {})
}
