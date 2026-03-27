package com.bridgeone.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.bridgeone.app.protocol.BridgeMode
import com.bridgeone.app.ui.utils.DeltaCalculator
import com.bridgeone.app.ui.utils.ClickDetector
import com.bridgeone.app.ui.utils.getDistance

/**
 * TouchpadWrapper Composable
 *
 * 터치패드 UI 구성 및 터치 이벤트 감지를 담당합니다.
 * 1:2 비율의 직사각형으로 구성되며, 라운드 모서리가 적용됩니다.
 * 터치 이벤트(DOWN, MOVE, UP)를 감지하고, 자동으로 클릭 감지 및 프레임 생성/전송을 수행합니다.
 *
 * Phase 2.2.3.3 업데이트: 클릭 감지 및 프레임 생성/전송
 * - DOWN 이벤트: 터치 시간 및 위치 기록
 * - MOVE 이벤트: 델타 계산 및 데드존 보상 (Phase 2.2.3.2 통합)
 * - RELEASE 이벤트: 클릭 판정 → 프레임 생성 → UART 전송 (ClickDetector 활용)
 * - 자동 상태 초기화: 터치 완료 후 모든 상태 초기화
 *
 * **클릭 감지 로직:**
 * - LEFT_CLICK: 누르는 시간 < 500ms && 움직임 < 15dp
 * - RIGHT_CLICK: 누르는 시간 >= 500ms && 움직임 < 15dp (롱터치)
 * - NO_CLICK: 그 외 (드래그로 판정)
 *
 * **프레임 생성 흐름:**
 * 1. RELEASE 이벤트에서 클릭 판정 (ClickDetector.detectClick)
 * 2. 프레임 생성 with 보상된 델타 값 (ClickDetector.createFrame)
 * 3. UART 전송 (ClickDetector.sendFrame → UsbSerialManager.sendFrame)
 *
 * @param modifier 외부에서 추가할 수 있는 Modifier
 * @param onTouchEvent 터치 이벤트 콜백 함수 (선택사항, 외부 처리 필요 시 사용)
 *   - eventType: PointerEventType (PRESS, MOVE, RELEASE)
 *   - currentPosition: 현재 터치 위치
 *   - previousPosition: 이전 터치 위치 (델타 계산용)
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TouchpadWrapper(
    modifier: Modifier = Modifier,
    bridgeMode: BridgeMode = BridgeMode.ESSENTIAL,
    onTouchEvent: (
        eventType: PointerEventType,
        currentPosition: Offset,
        previousPosition: Offset
    ) -> Unit = { _, _, _ -> }
) {
    // 현재 터치 위치 및 이전 터치 위치 상태
    val currentTouchPosition = remember { mutableStateOf(Offset.Zero) }
    val previousTouchPosition = remember { mutableStateOf(Offset.Zero) }
    
    // Phase 2.2.3.3: 클릭 감지를 위한 DOWN 이벤트 시간 및 위치 기록
    val touchDownTime = remember { mutableStateOf(0L) }
    val touchDownPosition = remember { mutableStateOf(Offset.Zero) }
    
    // Phase 2.2.3.3: 보상된 델타 값 저장 (MOVE 이벤트에서 업데이트, RELEASE에서 사용)
    val compensatedDeltaX = remember { mutableStateOf(0f) }
    val compensatedDeltaY = remember { mutableStateOf(0f) }

    // 데드존 탈출 여부 (터치 시작점에서 누적 이동 거리가 임계값을 넘으면 true)
    val deadZoneEscaped = remember { mutableStateOf(false) }

    // Phase 2.2.3.2: 현재 화면의 밀도 정보
    val density = LocalDensity.current

    // 모서리 반경: 고정값 5dp (외부 크기와 무관하게 일정한 반경 유지)
    val CORNER_RADIUS = 5.dp

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(CORNER_RADIUS))
            .background(Color(0xFF1A1A1A))  // 다크 테마 배경색
            .pointerInput(Unit) {
                awaitEachGesture {
                    // 포인터 DOWN 이벤트 처리
                    val down = awaitPointerEvent()

                    // PRESS가 아닌 이벤트(MOVE, RELEASE 등)로 시작하면 제스처 무시
                    if (down.type != PointerEventType.Press) {
                        return@awaitEachGesture
                    }

                    // 터치 이벤트 소비: HorizontalPager 등 부모로 전파 방지
                    down.changes.forEach { it.consume() }

                    currentTouchPosition.value = down.changes.first().position
                    previousTouchPosition.value = currentTouchPosition.value

                    // Phase 2.2.3.3: DOWN 이벤트 시간 및 위치 기록 (클릭 감지용)
                    touchDownTime.value = System.currentTimeMillis()
                    touchDownPosition.value = currentTouchPosition.value

                    // Phase 2.2.3.3: 보상된 델타 값 초기화
                    compensatedDeltaX.value = 0f
                    compensatedDeltaY.value = 0f

                    // 데드존 상태 초기화
                    deadZoneEscaped.value = false

                    onTouchEvent(
                        PointerEventType.Press,
                        currentTouchPosition.value,
                        previousTouchPosition.value
                    )

                    // 포인터 MOVE 이벤트 처리
                    var moveEvent = awaitPointerEvent()
                    while (moveEvent.type == PointerEventType.Move) {
                        // 터치 이벤트 소비: HorizontalPager 등 부모로 전파 방지
                        moveEvent.changes.forEach { it.consume() }
                        val change = moveEvent.changes.first()

                        // Phase 2.3.6: historical 터치 샘플 + 현재 위치를 모두 처리
                        // Compose는 프레임 단위로 이벤트를 전달하므로, 프레임 사이의
                        // 중간 터치 샘플은 historical에 담겨 있음. 이를 모두 처리하면
                        // 터치 샘플링 레이트를 최대한 활용하여 부드러운 커서 이동 가능.
                        val allPositions = change.historical.map { it.position } + change.position

                        for (pos in allPositions) {
                            previousTouchPosition.value = currentTouchPosition.value
                            currentTouchPosition.value = pos

                            // Phase 2.2.3.2: 델타 계산
                            val rawDelta = DeltaCalculator.calculateDelta(
                                previousTouchPosition.value,
                                currentTouchPosition.value
                            )

                            // 데드존 체크: 터치 시작점에서 현재 위치까지의 누적 거리
                            if (!deadZoneEscaped.value) {
                                val distanceFromStart = (currentTouchPosition.value - touchDownPosition.value).getDistance()
                                val deadZoneThreshold = DeltaCalculator.getDeadZoneThresholdPx(density)
                                if (distanceFromStart >= deadZoneThreshold) {
                                    deadZoneEscaped.value = true
                                }
                            }

                            // 데드존 탈출 후에만 델타 전송
                            val finalDelta = if (deadZoneEscaped.value) {
                                DeltaCalculator.normalizeOnly(rawDelta)
                            } else {
                                Offset.Zero
                            }

                            // 보상된 델타 값 저장 (RELEASE에서 사용)
                            compensatedDeltaX.value = finalDelta.x
                            compensatedDeltaY.value = finalDelta.y

                            // 드래그 프레임 즉시 전송
                            if (finalDelta.x != 0f || finalDelta.y != 0f) {
                                val dragFrame = ClickDetector.createFrame(
                                    buttonState = 0x00u,
                                    deltaX = finalDelta.x,
                                    deltaY = finalDelta.y
                                )
                                ClickDetector.sendFrame(dragFrame)
                            }
                        }

                        onTouchEvent(
                            PointerEventType.Move,
                            currentTouchPosition.value,
                            previousTouchPosition.value
                        )
                        moveEvent = awaitPointerEvent()
                    }

                    // 포인터 UP 이벤트 처리
                    if (moveEvent.type == PointerEventType.Release) {
                        previousTouchPosition.value = currentTouchPosition.value
                        currentTouchPosition.value = moveEvent.changes.first().position
                        
                        // Phase 2.2.3.2: RELEASE 이벤트 시에도 델타 계산
                        // Compose pointerInput 좌표는 이미 px 단위이므로 dp→px 변환 불필요
                        val releaseDelta = DeltaCalculator.calculateDelta(
                            previousTouchPosition.value,
                            currentTouchPosition.value
                        )

                        // 데드존 탈출 여부에 따라 델타 처리
                        val releaseFinalDelta = if (deadZoneEscaped.value) {
                            DeltaCalculator.normalizeOnly(releaseDelta)
                        } else {
                            Offset.Zero
                        }

                        // Phase 2.2.3.3: RELEASE 이벤트에서 보상된 델타 값 업데이트
                        compensatedDeltaX.value = releaseFinalDelta.x
                        compensatedDeltaY.value = releaseFinalDelta.y
                        
                        // Phase 2.2.3.3: 클릭 감지 및 프레임 생성/전송
                        // 데드존을 벗어나 드래그가 발생했으면 클릭 무시
                        val buttonState = if (deadZoneEscaped.value) {
                            0x00u.toUByte()  // 드래그 → 클릭 아님
                        } else {
                            val pressDuration = System.currentTimeMillis() - touchDownTime.value
                            val movement = (currentTouchPosition.value - touchDownPosition.value).getDistance()
                            val detected = ClickDetector.detectClick(pressDuration, movement)
                            // Essential 모드: 롱프레스 우클릭 비활성 → 좌클릭으로 대체
                            if (bridgeMode == BridgeMode.ESSENTIAL && detected == 0x02u.toUByte()) {
                                0x01u.toUByte()  // RIGHT_CLICK → LEFT_CLICK
                            } else {
                                detected
                            }
                        }

                        // 프레임 생성 (자동 시퀀스 번호 할당)
                        val frame = ClickDetector.createFrame(
                            buttonState = buttonState,
                            deltaX = compensatedDeltaX.value,
                            deltaY = compensatedDeltaY.value
                        )

                        // UART로 프레임 전송
                        ClickDetector.sendFrame(frame)

                        // 클릭 시 버튼 해제 프레임 즉시 전송 (PC에서 홀드 방지)
                        if (buttonState != 0x00u.toUByte()) {
                            val releaseFrame = ClickDetector.createFrame(
                                buttonState = 0x00u.toUByte(),
                                deltaX = 0f,
                                deltaY = 0f
                            )
                            ClickDetector.sendFrame(releaseFrame)
                        }
                        
                        // 터치 상태 초기화
                        touchDownTime.value = 0L
                        touchDownPosition.value = Offset.Zero
                        compensatedDeltaX.value = 0f
                        compensatedDeltaY.value = 0f
                        deadZoneEscaped.value = false
                        
                        onTouchEvent(
                            PointerEventType.Release,
                            currentTouchPosition.value,
                            previousTouchPosition.value
                        )
                    }
                }
            }
    ) {
        // 터치패드 내용 (향후 UI 요소 추가)
    }
}

/**
 * TouchpadWrapper Preview
 *
 * Android Studio에서 UI를 시각적으로 확인하기 위한 Preview 함수입니다.
 * 1:2 비율, 둥근 모서리, 터치 이벤트 감지 기능을 표시합니다.
 */
@Preview(
    showBackground = true,
    backgroundColor = 0xFF0D0D0D,
    widthDp = 200,
    heightDp = 400
)
@Composable
fun TouchpadWrapperPreview() {
    TouchpadWrapper(
        modifier = Modifier
            .size(160.dp, 320.dp)
            .background(Color(0xFF0D0D0D))
    )
}

