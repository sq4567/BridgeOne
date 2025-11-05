package com.bridgeone.app.ui.utils

import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.bridgeone.app.protocol.BridgeFrame
import com.bridgeone.app.protocol.FrameBuilder
import kotlin.math.sqrt

/**
 * 터치 이벤트 기반 클릭 감지 및 프레임 생성 유틸리티
 *
 * Phase 2.2.3.3에서 구현된 클릭 감지 로직 및 프레임 생성 기능을 제공합니다.
 * 터치 다운(DOWN) 시간을 기록하고, 터치 업(UP) 이벤트 시 누르는 시간과 이동 거리를 계산하여
 * 클릭 타입(LEFT_CLICK, RIGHT_CLICK, NO_CLICK)을 판정합니다.
 *
 * **클릭 판정 기준:**
 * - LEFT_CLICK: 누르는 시간 < 500ms && 움직임 < 15dp
 * - RIGHT_CLICK (롱터치): 누르는 시간 >= 500ms && 움직임 < 15dp
 * - NO_CLICK (드래그): 그 외
 *
 * **사용 예시:**
 * ```kotlin
 * // 터치 이벤트 핸들러에서 사용
 * var touchDownTime = 0L
 * var touchDownPosition = Offset.Zero
 * var compensatedDeltaX = 0f
 * var compensatedDeltaY = 0f
 *
 * TouchpadWrapper(
 *     onTouchEvent = { eventType, currentPos, previousPos ->
 *         when (eventType) {
 *             PointerEventType.Press -> {
 *                 touchDownTime = System.currentTimeMillis()
 *                 touchDownPosition = currentPos
 *             }
 *             PointerEventType.Move -> {
 *                 // MOVE 이벤트 처리 (DeltaCalculator와 통합)
 *                 val compensatedDelta = DeltaCalculator.calculateAndCompensate(
 *                     density, previousPos, currentPos
 *                 )
 *                 compensatedDeltaX = compensatedDelta.x
 *                 compensatedDeltaY = compensatedDelta.y
 *             }
 *             PointerEventType.Release -> {
 *                 val pressDuration = System.currentTimeMillis() - touchDownTime
 *                 val movement = (currentPos - touchDownPosition).getDistance()
 *
 *                 val buttonState = ClickDetector.detectClick(pressDuration, movement)
 *                 val frame = ClickDetector.createFrame(
 *                     buttonState,
 *                     compensatedDeltaX,
 *                     compensatedDeltaY
 *                 )
 *                 ClickDetector.sendFrame(frame)
 *             }
 *         }
 *     }
 * )
 * ```
 *
 * **Reference:**
 * - Phase 2.2.3.1: TouchpadWrapper 및 터치 이벤트 감지
 * - Phase 2.2.3.2: DeltaCalculator 델타 계산 및 데드존 보상
 * - Phase 2.2.1.2: FrameBuilder 프레임 생성 및 순번 관리
 * - Phase 2.2.2.4: UsbSerialManager 프레임 전송
 */
object ClickDetector {

    // ========== 상수 정의 ==========

    /**
     * 클릭 최대 누르는 시간 (밀리초)
     * 이 시간 이내에 손가락을 떼면 LEFT_CLICK, 초과하면 RIGHT_CLICK 판정
     */
    const val CLICK_MAX_DURATION: Long = 500L

    /**
     * 클릭 최대 이동 거리 (dp)
     * 손가락 이동이 이 거리 이내여야 클릭으로 판정됨 (손떨림 보상)
     */
    const val CLICK_MAX_MOVEMENT_DP: Float = 15f

    /**
     * 디버그 로그 태그
     */
    private const val TAG = "ClickDetector"

    // ========== 마우스 버튼 비트 상수 ==========

    /**
     * 마우스 버튼 비트: NO_CLICK (드래그 또는 유효하지 않은 클릭)
     */
    private const val NO_CLICK: UByte = 0x00u

    /**
     * 마우스 버튼 비트: LEFT_CLICK (짧은 터치)
     * BridgeFrame 프로토콜: bit 0
     */
    private const val LEFT_CLICK: UByte = 0x01u

    /**
     * 마우스 버튼 비트: RIGHT_CLICK (롱터치)
     * BridgeFrame 프로토콜: bit 1
     */
    private const val RIGHT_CLICK: UByte = 0x02u

    /**
     * 마우스 버튼 비트: MIDDLE_CLICK (더블터치 - Phase 2.2.3.3에서는 미구현)
     * BridgeFrame 프로토콜: bit 2
     * 참고: Phase 2.2.4에서 더블터치 감지 로직 추가 예정
     */
    private const val MIDDLE_CLICK: UByte = 0x04u

    // ========== 공개 함수 ==========

    /**
     * 터치 이벤트를 분석하여 클릭 타입을 판정합니다.
     *
     * **판정 기준:**
     * - 누르는 시간 < 500ms && 움직임 < 15dp → LEFT_CLICK (0x01)
     * - 누르는 시간 >= 500ms && 움직임 < 15dp → RIGHT_CLICK (0x02) (롱터치)
     * - 그 외 (움직임이 15dp 이상) → NO_CLICK (0x00) (드래그로 판정, 버튼 없음)
     *
     * **주의사항:**
     * - movement 파라미터는 이미 DeltaCalculator의 데드존 보상을 거친 값으로 전달받아야 함
     * - System.currentTimeMillis() 사용으로 밀리초 단위 정확도 보장
     *
     * @param pressDuration 터치 누르는 시간 (밀리초)
     *   - 계산식: System.currentTimeMillis() - touchDownTime
     * @param movement 터치 이동 거리 (dp 단위)
     *   - 계산식: (currentPos - touchDownPosition).getDistance()
     *   - Offset.getDistance()는 유클리드 거리(Euclidean distance) 계산
     *
     * @return 판정된 마우스 버튼 비트 (UByte)
     *   - 0x01: LEFT_CLICK
     *   - 0x02: RIGHT_CLICK (롱터치)
     *   - 0x00: NO_CLICK (드래그)
     *
     * **구현 패턴 (when 식 사용):**
     * ```kotlin
     * return when {
     *     duration < 500 && movement < 15 → LEFT_CLICK
     *     duration >= 500 && movement < 15 → RIGHT_CLICK
     *     else → NO_CLICK
     * }
     * ```
     *
     * **Reference (Context7 검증됨):**
     * - System.currentTimeMillis() 안정성: Android 공식 문서 확인
     * - Offset.getDistance(): Jetpack Compose UI Geometry API
     */
    fun detectClick(pressDuration: Long, movement: Float): UByte {
        // movement를 pixel에서 dp로 변환 (이미 dp 단위로 들어옴)
        return when {
            // 짧은 터치 + 적은 이동 = LEFT_CLICK
            pressDuration < CLICK_MAX_DURATION && movement < CLICK_MAX_MOVEMENT_DP -> {
                Log.d(TAG, "detectClick: LEFT_CLICK (duration=$pressDuration ms, movement=${movement.toInt()} dp)")
                LEFT_CLICK
            }
            // 긴 터치 + 적은 이동 = RIGHT_CLICK (롱터치)
            pressDuration >= CLICK_MAX_DURATION && movement < CLICK_MAX_MOVEMENT_DP -> {
                Log.d(TAG, "detectClick: RIGHT_CLICK (duration=$pressDuration ms, movement=${movement.toInt()} dp)")
                RIGHT_CLICK
            }
            // 그 외 = NO_CLICK (드래그로 판정)
            else -> {
                Log.d(TAG, "detectClick: NO_CLICK (duration=$pressDuration ms, movement=${movement.toInt()} dp)")
                NO_CLICK
            }
        }
    }

    /**
     * 현재 클릭 상태를 기반으로 버튼 상태를 반환합니다.
     *
     * detectClick()의 결과를 그대로 반환하는 헬퍼 함수입니다.
     * 향후 더블클릭 감지 등이 추가될 때 확장 가능합니다.
     *
     * **현재 구현:**
     * - LEFT_CLICK (0x01): 좌측 마우스 버튼
     * - RIGHT_CLICK (0x02): 우측 마우스 버튼 (롱터치)
     * - MIDDLE_CLICK (0x04): 미들 마우스 버튼 (더블터치, 미구현)
     *
     * **Phase 2.2.3.3에서 구현되는 버튼:**
     * - ✅ LEFT_CLICK (0x01)
     * - ✅ RIGHT_CLICK (0x02) (롱터치)
     * - ⏳ MIDDLE_CLICK (0x04) (더블터치, Phase 2.2.4에서 추가 예정)
     *
     * @param pressDuration 터치 누르는 시간 (밀리초)
     * @param movement 터치 이동 거리 (dp)
     * @return 판정된 마우스 버튼 상태 (0x00 ~ 0x07)
     */
    fun getButtonState(pressDuration: Long, movement: Float): UByte {
        return detectClick(pressDuration, movement)
    }

    /**
     * BridgeFrame 프로토콜에 따라 프레임을 생성합니다.
     *
     * FrameBuilder.buildFrame()을 호출하여 자동 시퀀스 번호 할당 및 프레임 생성을 수행합니다.
     *
     * **프레임 구조 (8바이트):**
     * ```
     * 구조: [seq(1)] [buttons(1)] [deltaX(1)] [deltaY(1)] [wheel(1)] [modifiers(1)] [keyCode1(1)] [keyCode2(1)]
     * 예시: [0x42  ] [0x01      ] [0x0A     ] [0xF6     ] [0x00    ] [0x00       ] [0x00      ] [0x00      ]
     *       seq=66  LEFT_CLICK    dx=10      dy=-10     wheel=0   no modifier  no key1     no key2
     * ```
     *
     * **주의사항:**
     * - deltaX, deltaY는 명시적으로 .toByte() 변환 필수 (Type Safety)
     * - Phase 2.2.1.3에서 검증된 stread-safe FrameBuilder 사용
     * - 프레임 시퀀스 번호는 0~255 범위에서 자동 순환
     *
     * @param buttonState 마우스 버튼 상태 (0x00 ~ 0x07)
     *   - getButtonState()의 반환값 직접 전달 가능
     * @param deltaX X축 상대 이동값 (pixel, -128 ~ 127 범위로 정규화됨)
     *   - DeltaCalculator.applyDeadZone()에서 반환된 보상된 델타 값 사용
     * @param deltaY Y축 상대 이동값 (pixel, -128 ~ 127 범위로 정규화됨)
     *   - DeltaCalculator.applyDeadZone()에서 반환된 보상된 델타 값 사용
     *
     * @return BridgeFrame 객체 (8바이트)
     *
     * **Reference:**
     * - BridgeFrame.kt: 프레임 데이터 구조
     * - FrameBuilder.kt: buildFrame() 구현 및 시퀀스 번호 관리
     * - Phase 2.2.1.3: FrameBuilder 멀티 스레드 안전성 검증
     * - Phase 2.2.3.2: DeltaCalculator 델타 보상 값 사용
     */
    fun createFrame(
        buttonState: UByte,
        deltaX: Float,
        deltaY: Float
    ): BridgeFrame {
        // 델타 값을 -128 ~ 127 범위의 Byte로 변환 (정규화)
        // applyDeadZone()에서 이미 범위 정규화되었으므로 toInt()로 변환 후 toByte()
        val normalizedDeltaX = deltaX.toInt().coerceIn(-128, 127).toByte()
        val normalizedDeltaY = deltaY.toInt().coerceIn(-128, 127).toByte()

        // FrameBuilder를 사용하여 프레임 생성 (자동 시퀀스 번호 할당)
        // Phase 2.2.1.2/2.2.1.3에서 구현된 스레드 안전한 FrameBuilder 활용
        return FrameBuilder.buildFrame(
            buttons = buttonState,              // 마우스 버튼 상태
            deltaX = normalizedDeltaX,          // X축 상대 이동값
            deltaY = normalizedDeltaY,          // Y축 상대 이동값
            wheel = 0.toByte(),                 // Boot 모드에서는 0으로 고정
            modifiers = 0u,                     // Phase 2.2.4에서 키보드 수정자 키 추가
            keyCode1 = 0u,                      // Phase 2.2.4에서 첫 번째 키코드 추가
            keyCode2 = 0u                       // Phase 2.2.4에서 두 번째 키코드 추가
        ).also { frame ->
            // 생성된 프레임 정보 로깅 (디버그)
            Log.d(
                TAG,
                "createFrame: seq=${frame.seq}, buttons=0x${frame.buttons.toString(16).padStart(2, '0')}, " +
                    "dx=${frame.deltaX}, dy=${frame.deltaY}"
            )
        }
    }

    /**
     * 생성된 BridgeFrame을 UART로 비동기로 전송합니다.
     *
     * UsbSerialManager.sendFrame()을 호출하여 USB Serial 포트를 통해
     * 프레임 데이터를 ESP32-S3로 전송합니다.
     *
     * **전송 흐름:**
     * 1. UsbSerialManager.sendFrame(frame) 호출
     * 2. frame.toByteArray()로 8바이트 데이터 직렬화
     * 3. USB Serial 포트로 1Mbps에서 전송
     * 4. IOException 발생 시 예외 처리 및 로그 기록
     * 5. 포트 오류 시 자동 연결 해제
     *
     * **주의사항:**
     * - 이 함수는 동기식으로 호출되며, 내부에서 IOException 처리
     * - Phase 2.2.2.4에서 구현된 UsbSerialManager 활용
     * - 전송 실패 시 log에 기록되지만, 예외는 발생하지 않음 (UI 블로킹 방지)
     *
     * @param frame 전송할 BridgeFrame 객체
     *
     * **Reference:**
     * - UsbSerialManager.kt: sendFrame() 구현
     * - BridgeFrame.kt: toByteArray() 직렬화
     * - Phase 2.2.2.4: USB Serial 통신 구현
     */
    fun sendFrame(frame: BridgeFrame) {
        try {
            // Phase 2.2.2.4: UsbSerialManager를 통해 UART로 전송
            // sendFrame() 내부에서 IOException 처리 및 포트 자동 종료 수행
            com.bridgeone.app.usb.UsbSerialManager.sendFrame(frame)

            Log.d(TAG, "Frame sent successfully - seq=${frame.seq}")

        } catch (e: IllegalStateException) {
            // USB 포트가 연결되지 않았거나 전송 실패
            Log.e(TAG, "Failed to send frame: ${e.message}", e)

        } catch (e: Exception) {
            // 예상치 못한 예외
            Log.e(TAG, "Unexpected error while sending frame: ${e.message}", e)
        }
    }
}

/**
 * Offset 확장 함수: 거리 계산
 *
 * 두 점 사이의 유클리드 거리를 계산합니다.
 * √((x1-x2)² + (y1-y2)²) 공식 사용
 *
 * **Reference (Context7 검증됨):**
 * - Jetpack Compose Offset API
 * - 안드로이드 UI Geometry 라이브러리
 *
 * @return 두 점 사이의 거리
 */
fun Offset.getDistance(): Float {
    // Offset의 x, y 속성에 직접 접근하여 거리 계산
    return sqrt(x * x + y * y)
}

