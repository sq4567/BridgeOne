package com.chatterbones.bridgeone.input

import android.view.MotionEvent
import com.chatterbones.bridgeone.protocol.BridgeFrame
import com.chatterbones.bridgeone.protocol.SequenceCounter
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * 터치 입력을 BridgeFrame으로 변환하는 프로세서 클래스
 * 
 * Android 터치 이벤트(MotionEvent)를 수신하여 BridgeOne 프로토콜 프레임으로 변환합니다.
 * 이전 터치 좌표를 기억하여 상대적 델타(deltaX/deltaY)를 계산하고,
 * 터치 액션(DOWN, MOVE, UP)에 따라 적절한 마우스 버튼 상태를 설정합니다.
 * 
 * 현재는 기본적인 좌클릭 드래그만 지원하며, 나머지 필드(wheel, modifiers, keyCode)는
 * 0으로 설정됩니다. 고급 터치패드 알고리즘은 Phase 2에서 구현될 예정입니다.
 * 
 * @constructor 순번 카운터를 주입받아 프로세서를 생성합니다.
 * @param sequenceCounter BridgeFrame 생성 시 사용할 순번 카운터
 * 
 * @see BridgeFrame
 * @see SequenceCounter
 */
class TouchInputProcessor(
    private val sequenceCounter: SequenceCounter
) {
    // 이전 터치 좌표 저장 (델타 계산용)
    private var previousX: Float = 0f
    private var previousY: Float = 0f
    
    // 현재 터치 상태 추적
    private var isTouchActive: Boolean = false
    
    companion object {
        /**
         * 델타 값의 최소/최대 범위 상수
         * BridgeOne 프로토콜 명세에 따라 -127~127 범위로 제한됩니다.
         */
        private const val DELTA_MIN = -127
        private const val DELTA_MAX = 127
        
        /**
         * 좌클릭 버튼 비트 마스크
         */
        private val BUTTON_LEFT = BridgeFrame.BUTTON_LEFT
        
        /**
         * 버튼 없음 상태
         */
        private const val BUTTON_NONE: UByte = 0x00u
    }
    
    /**
     * 터치 이벤트를 처리하고 BridgeFrame으로 변환합니다.
     * 
     * 이 메서드는 Android View의 onTouchEvent에서 호출됩니다.
     * 터치 액션에 따라 적절한 BridgeFrame을 생성하여 반환합니다.
     * 
     * 지원되는 액션:
     * - ACTION_DOWN: 터치 시작 (좌클릭 버튼 활성화)
     * - ACTION_MOVE: 터치 드래그 (델타 계산 및 좌클릭 유지)
     * - ACTION_UP: 터치 종료 (좌클릭 버튼 해제)
     * 
     * @param event Android MotionEvent 객체
     * @return 변환된 BridgeFrame 객체, 처리할 수 없는 액션인 경우 null
     */
    fun processTouch(event: MotionEvent): BridgeFrame? {
        return when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> handleActionDown(event)
            MotionEvent.ACTION_MOVE -> handleActionMove(event)
            MotionEvent.ACTION_UP -> handleActionUp(event)
            MotionEvent.ACTION_CANCEL -> handleActionCancel(event)
            else -> {
                logger.debug { "처리되지 않은 터치 액션: ${event.actionMasked}" }
                null
            }
        }
    }
    
    /**
     * ACTION_DOWN 처리: 터치 시작
     * 
     * 현재 좌표를 저장하고 좌클릭 버튼이 활성화된 프레임을 생성합니다.
     * 델타 값은 0으로 설정됩니다 (이동 없음).
     * 
     * @param event MotionEvent 객체
     * @return 좌클릭 버튼이 활성화된 BridgeFrame
     */
    private fun handleActionDown(event: MotionEvent): BridgeFrame {
        // 터치 시작 좌표 저장
        previousX = event.x
        previousY = event.y
        isTouchActive = true
        
        logger.debug { "터치 시작: ($previousX, $previousY)" }
        
        // 이동 없이 좌클릭만 활성화된 프레임 생성
        return BridgeFrame(
            seq = sequenceCounter.next(),
            buttons = BUTTON_LEFT,
            deltaX = 0,
            deltaY = 0,
            wheel = 0,
            modifiers = 0u,
            keyCode1 = 0u,
            keyCode2 = 0u
        )
    }
    
    /**
     * ACTION_MOVE 처리: 터치 드래그
     * 
     * 이전 좌표와 현재 좌표의 차이를 계산하여 델타 값을 구합니다.
     * 델타 값은 -127~127 범위로 클램핑되며, 좌클릭 버튼이 유지됩니다.
     * 
     * @param event MotionEvent 객체
     * @return 델타 이동과 좌클릭이 포함된 BridgeFrame
     */
    private fun handleActionMove(event: MotionEvent): BridgeFrame {
        val currentX = event.x
        val currentY = event.y
        
        // 델타 계산 (현재 좌표 - 이전 좌표)
        val rawDeltaX = (currentX - previousX).toInt()
        val rawDeltaY = (currentY - previousY).toInt()
        
        // -127~127 범위로 클램핑
        val clampedDeltaX = rawDeltaX.coerceIn(DELTA_MIN, DELTA_MAX).toByte()
        val clampedDeltaY = rawDeltaY.coerceIn(DELTA_MIN, DELTA_MAX).toByte()
        
        // 좌표 업데이트 (다음 이벤트를 위해)
        previousX = currentX
        previousY = currentY
        
        logger.debug { 
            "터치 이동: raw=($rawDeltaX, $rawDeltaY), clamped=($clampedDeltaX, $clampedDeltaY)" 
        }
        
        // 델타 이동과 좌클릭이 포함된 프레임 생성
        return BridgeFrame(
            seq = sequenceCounter.next(),
            buttons = BUTTON_LEFT,
            deltaX = clampedDeltaX,
            deltaY = clampedDeltaY,
            wheel = 0,
            modifiers = 0u,
            keyCode1 = 0u,
            keyCode2 = 0u
        )
    }
    
    /**
     * ACTION_UP 처리: 터치 종료
     * 
     * 터치를 종료하고 좌클릭 버튼을 해제한 프레임을 생성합니다.
     * 델타 값은 0으로 설정됩니다 (이동 없음).
     * 
     * @param event MotionEvent 객체
     * @return 좌클릭 버튼이 해제된 BridgeFrame
     */
    private fun handleActionUp(event: MotionEvent): BridgeFrame {
        isTouchActive = false
        
        logger.debug { "터치 종료: (${event.x}, ${event.y})" }
        
        // 이동 없이 버튼만 해제된 프레임 생성
        return BridgeFrame(
            seq = sequenceCounter.next(),
            buttons = BUTTON_NONE,
            deltaX = 0,
            deltaY = 0,
            wheel = 0,
            modifiers = 0u,
            keyCode1 = 0u,
            keyCode2 = 0u
        )
    }
    
    /**
     * ACTION_CANCEL 처리: 터치 취소
     * 
     * 터치가 시스템에 의해 취소된 경우 (예: 다른 제스처 감지)
     * ACTION_UP과 동일하게 처리하여 버튼을 해제합니다.
     * 
     * @param event MotionEvent 객체
     * @return 좌클릭 버튼이 해제된 BridgeFrame
     */
    private fun handleActionCancel(event: MotionEvent): BridgeFrame {
        logger.debug { "터치 취소: (${event.x}, ${event.y})" }
        return handleActionUp(event)
    }
    
    /**
     * 프로세서 상태를 초기화합니다.
     * 
     * 연결이 끊기거나 재시작될 때 이전 터치 상태를 리셋하는 데 사용됩니다.
     */
    fun reset() {
        previousX = 0f
        previousY = 0f
        isTouchActive = false
        logger.info { "TouchInputProcessor 초기화 완료" }
    }
    
    /**
     * 현재 터치 상태를 반환합니다.
     * 
     * @return 터치가 활성화되어 있으면 true, 그렇지 않으면 false
     */
    fun isActive(): Boolean = isTouchActive
}

