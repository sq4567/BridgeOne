package com.bridgeone.app.ui.common

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bridgeone.app.ui.common.swipe.FocusableElement
import com.bridgeone.app.ui.common.swipe.LocalSwipeFocusController
import com.bridgeone.app.ui.common.swipe.LocalSwipeFocused
import com.bridgeone.app.ui.common.swipe.ROOT_SCOPE
import com.bridgeone.app.ui.common.swipe.SwipeFocusable
import com.bridgeone.app.ui.common.swipe.SwipeMode
import kotlin.math.ceil
import kotlin.math.roundToInt

/** 커스텀 슬라이더 트랙 높이 (dp). 기본값: 28f */
internal const val CUSTOM_SLIDER_TRACK_HEIGHT_DP = 28f
/** 커스텀 슬라이더 손잡이 세로선 너비 (dp). 기본값: 3f */
internal const val CUSTOM_SLIDER_LINE_WIDTH_DP = 3f
/** NORMAL 레이어 손잡이 세로선 너비 (dp). 기본값: 8f */
internal const val CUSTOM_SLIDER_NORMAL_LINE_WIDTH_DP = 8f
/** 큰 눈금 높이 비율(트랙 높이 대비). 기본값: 0.7f */
private const val MAJOR_TICK_HEIGHT_FRACTION = 0.7f
/** 작은 눈금 높이 비율(트랙 높이 대비). 기본값: 0.4f */
private const val MINOR_TICK_HEIGHT_FRACTION = 0.4f
/** 큰 눈금 선 굵기 (dp). 기본값: 1.5f */
private const val MAJOR_TICK_WIDTH_DP = 1.5f
/** 작은 눈금 선 굵기 (dp). 기본값: 1f */
private const val MINOR_TICK_WIDTH_DP = 1f

/** [min]~[max] 범위에서 [step] 간격마다의 눈금 위치를 0~1 fraction 리스트로 반환. step이 null/0이면 빈 리스트. */
private fun tickFractions(min: Float, max: Float, step: Float?): List<Float> {
    if (step == null || step <= 0f || max <= min) return emptyList()
    val out = ArrayList<Float>()
    var t = ceil(min / step) * step
    while (t <= max + 1e-4f) {
        out.add(((t - min) / (max - min)).coerceIn(0f, 1f))
        t += step
    }
    return out
}

/**
 * BridgeOne 공통 커스텀 슬라이더.
 *
 * BoxWithConstraints + pointerInput 트랙, primaryContainer 미채움 배경, primary 채움, 흰 세로선 손잡이,
 * 우측 값 라벨로 구성된 표준 디자인. 말하기 속도/속도/DPI/매크로 딜레이/회전 간격 등 모든 값 슬라이더가 공유한다.
 *
 * - 미채움 배경은 포커스 상태에 따라 primaryContainer 0.12f/0.25f/0.5f (기본/focus/manip)
 * - [element]가 null이면 [SwipeFocusable] 래핑을 생략 (NORMAL 전용). 비-null이면 SWIPE 포커스 대상으로 등록
 * - [onManipulate]는 [valueRange] 기반으로 내부 자동 계산되어 호출처가 줄 필요 없음
 *
 * @param value 현재 값 (항상 Float 기반. Int 슬라이더는 호출처에서 변환)
 * @param onValueChange 값 변경 콜백. [snap]을 통과한 값이 전달됨
 * @param valueRange 값 범위
 * @param valueLabel 우측에 표시할 완성된 라벨 문자열 ("100ms", "×1.5" 등)
 * @param labelWidth 라벨 너비
 * @param snap 드래그/조작 시 값 양자화 람다 (정수 반올림, 0.1 step 등). 기본값: 항등
 * @param onBeforeChange 값 변경 직전 호출 (회전 간격의 undo 스냅샷 등). 기본값: null
 * @param element SWIPE 포커스 식별자. null이면 래핑 생략. 기본값: null
 * @param scope SWIPE scope. 기본값: ROOT_SCOPE
 * @param gridRow SWIPE 2D 그리드 행. 기본값: null
 * @param gridCol SWIPE 2D 그리드 열. 기본값: null
 * @param majorTickStep 큰 눈금 간격 (값 단위). null이면 큰 눈금 없음. 기본값: null
 * @param minorTickStep 작은 눈금 간격 (값 단위). null이면 작은 눈금 없음. 기본값: null
 */
@Composable
internal fun CustomTrackSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    labelWidth: Dp,
    modifier: Modifier = Modifier,
    snap: (Float) -> Float = { it },
    onBeforeChange: (() -> Unit)? = null,
    element: FocusableElement? = null,
    scope: Any = ROOT_SCOPE,
    gridRow: Int? = null,
    gridCol: Int? = null,
    majorTickStep: Float? = null,
    minorTickStep: Float? = null,
    labelFontWeight: FontWeight? = null,
) {
    val cs = MaterialTheme.colorScheme
    val controller = LocalSwipeFocusController.current
    val min = valueRange.start
    val max = valueRange.endInclusive
    val normalLayer = controller == null

    // pointerInput key를 안정값(Unit)으로 고정하고 최신 콜백을 참조 → 드래그 중 리컴포지션으로 인한 제스처 취소 방지
    // onManipulate(SWIPE 조작)는 onGloballyPositioned 시점에 컨트롤러로 등록돼 조작 중엔 재등록되지 않으므로,
    // value를 직접 캡처하면 조작 시작 시점 값에 고정된다. latestValue로 매번 최신 값을 읽어 증분 누적이 끊기지 않게 함.
    val latestValue by rememberUpdatedState(value)
    val latestOnValueChange by rememberUpdatedState(onValueChange)
    val latestSnap by rememberUpdatedState(snap)
    val latestOnBefore by rememberUpdatedState(onBeforeChange)

    val minorFractions = tickFractions(min, max, minorTickStep)
    val majorFractions = tickFractions(min, max, majorTickStep)

    val body: @Composable () -> Unit = {
        val isFocused = LocalSwipeFocused.current
        val inManip = isFocused && controller?.mode == SwipeMode.MANIPULATION
        val lineColor = if (isFocused || inManip) Color.White else Color.White.copy(alpha = 0.7f)
        val baseThumbWidth = if (normalLayer) CUSTOM_SLIDER_NORMAL_LINE_WIDTH_DP.dp
                             else CUSTOM_SLIDER_LINE_WIDTH_DP.dp
        val lineWidthDp by animateDpAsState(
            targetValue = when {
                inManip -> 6.dp
                isFocused -> 4.dp
                else -> baseThumbWidth
            },
            label = "customSliderLineWidth",
        )
        val minorTickColor = Color.White.copy(alpha = 0.22f)
        val majorTickColor = Color.White.copy(alpha = 0.5f)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 2.dp, end = 6.dp, top = 2.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .height(CUSTOM_SLIDER_TRACK_HEIGHT_DP.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()
                            fun applyX(x: Float) {
                                val fr = (x / size.width).coerceIn(0f, 1f)
                                latestOnBefore?.invoke()
                                latestOnValueChange(latestSnap(min + fr * (max - min)))
                            }
                            applyX(down.position.x)
                            drag(down.id) { change ->
                                change.consume()
                                applyX(change.position.x)
                            }
                        }
                    },
            ) {
                val trackWidthPx = constraints.maxWidth
                val thumbFraction = ((value - min) / (max - min)).coerceIn(0f, 1f)
                val trackBgColor = when {
                    inManip -> cs.primaryContainer.copy(alpha = 0.5f)
                    isFocused -> cs.primaryContainer.copy(alpha = 0.25f)
                    else -> cs.primaryContainer.copy(alpha = 0.12f)
                }
                Box(Modifier.matchParentSize().background(trackBgColor))
                Box(
                    Modifier.fillMaxHeight().fillMaxWidth(thumbFraction).background(cs.primary),
                )
                // 눈금: 작은 눈금 먼저, 큰 눈금이 같은 위치를 덮도록 나중에 그림
                if (minorFractions.isNotEmpty() || majorFractions.isNotEmpty()) {
                    Canvas(Modifier.matchParentSize()) {
                        val w = size.width
                        val h = size.height
                        val minorW = MINOR_TICK_WIDTH_DP.dp.toPx()
                        val majorW = MAJOR_TICK_WIDTH_DP.dp.toPx()
                        val minorHalf = h * MINOR_TICK_HEIGHT_FRACTION / 2f
                        val majorHalf = h * MAJOR_TICK_HEIGHT_FRACTION / 2f
                        val cy = h / 2f
                        minorFractions.forEach { fr ->
                            val x = fr * w
                            drawLine(
                                color = minorTickColor,
                                start = Offset(x, cy - minorHalf),
                                end = Offset(x, cy + minorHalf),
                                strokeWidth = minorW,
                            )
                        }
                        majorFractions.forEach { fr ->
                            val x = fr * w
                            drawLine(
                                color = majorTickColor,
                                start = Offset(x, cy - majorHalf),
                                end = Offset(x, cy + majorHalf),
                                strokeWidth = majorW,
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset {
                            val lineWidthPx = lineWidthDp.roundToPx()
                            IntOffset(
                                (thumbFraction * trackWidthPx - lineWidthPx / 2f)
                                    .roundToInt()
                                    .coerceIn(0, (trackWidthPx - lineWidthPx).coerceAtLeast(0)),
                                0,
                            )
                        }
                        .fillMaxHeight()
                        .width(lineWidthDp)
                        .background(lineColor),
                )
            }
            Text(
                valueLabel,
                fontSize = 13.sp,
                fontWeight = labelFontWeight,
                color = cs.onSurface,
                modifier = Modifier.width(labelWidth),
                textAlign = TextAlign.End,
            )
        }
    }

    if (element != null) {
        SwipeFocusable(
            element = element,
            scope = scope,
            shape = RoundedCornerShape(8.dp),
            showBorderHighlight = true,
            manipulatable = true,
            onManipulate = { deltaPx, screenWidthPx ->
                val delta = (deltaPx / screenWidthPx) * (max - min)
                latestOnBefore?.invoke()
                latestOnValueChange(latestSnap((latestValue + delta).coerceIn(min, max)))
            },
            gridRow = gridRow,
            gridCol = gridCol,
            modifier = modifier,
        ) { body() }
    } else {
        Box(modifier) { body() }
    }
}
