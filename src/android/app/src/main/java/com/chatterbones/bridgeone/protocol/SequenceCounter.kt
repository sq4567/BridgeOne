package com.chatterbones.bridgeone.protocol

import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

private val logger = KotlinLogging.logger {}

/**
 * 0-255 순환 순번 카운터 관리 클래스
 * 
 * BridgeOne 프로토콜의 순번 관리 및 검증을 담당합니다.
 * 순번은 0부터 255까지 순환하며, 프레임 송수신 통계를 제공합니다.
 * Thread-safe 하게 구현되어 멀티스레드 환경에서 안전하게 사용할 수 있습니다.
 * 
 * @constructor 초기 순번을 0으로 설정하여 카운터를 생성합니다.
 * 
 * @property currentSequence 현재 순번 값 (0-255 순환)
 * @property sentCount 전송된 프레임 수
 * @property receivedCount 수신된 프레임 수
 * @property lostCount 손실된 프레임 수 (순번 불일치로 감지)
 */
class SequenceCounter {
    // AtomicInteger를 사용하여 thread-safe한 순번 관리
    // UByte는 atomic 연산을 직접 지원하지 않으므로 Int로 관리
    private val sequence = AtomicInteger(0)
    
    // 예상 수신 순번 (validate 메서드에서 사용)
    private val expectedReceiveSequence = AtomicInteger(0)
    
    // 통계 카운터들
    private val sentCounter = AtomicLong(0)
    private val receivedCounter = AtomicLong(0)
    private val lostCounter = AtomicLong(0)
    
    /**
     * 현재 순번 값을 반환합니다.
     * 
     * @return 현재 순번 (0-255 범위의 UByte)
     */
    val currentSequence: UByte
        get() = sequence.get().toUByte()
    
    /**
     * 전송된 프레임 총 개수를 반환합니다.
     * 
     * @return 전송된 프레임 수
     */
    val sentCount: Long
        get() = sentCounter.get()
    
    /**
     * 수신된 프레임 총 개수를 반환합니다.
     * 
     * @return 수신된 프레임 수
     */
    val receivedCount: Long
        get() = receivedCounter.get()
    
    /**
     * 손실된 프레임 총 개수를 반환합니다.
     * 순번 불일치로 감지된 경우를 카운트합니다.
     * 
     * @return 손실된 프레임 수
     */
    val lostCount: Long
        get() = lostCounter.get()
    
    /**
     * 다음 순번을 생성하고 반환합니다.
     * 
     * 순번은 0-255 범위에서 순환합니다.
     * 255 다음에는 자동으로 0으로 돌아갑니다.
     * 호출 시마다 전송 카운터가 증가합니다.
     * 
     * @return 다음 순번 (0-255 범위의 UByte)
     */
    fun next(): UByte {
        // 현재 값을 가져오고 1 증가시킨 후, 256으로 나눈 나머지로 순환
        val nextValue = sequence.updateAndGet { current ->
            (current + 1) % 256
        }
        
        // 전송 카운터 증가
        sentCounter.incrementAndGet()
        
        logger.debug { "순번 생성: $nextValue" }
        return nextValue.toUByte()
    }
    
    /**
     * 수신된 순번을 검증합니다.
     * 
     * 예상 순번과 수신 순번을 비교하여 일치 여부를 확인합니다.
     * 순번이 불일치하는 경우 경고 로그를 출력하고 손실 카운터를 증가시킵니다.
     * 검증 결과와 관계없이 수신 카운터는 증가합니다.
     * 
     * @param receivedSeq 수신된 순번
     * @return 순번이 예상값과 일치하면 true, 불일치하면 false
     */
    fun validate(receivedSeq: UByte): Boolean {
        val expected = expectedReceiveSequence.get()
        val received = receivedSeq.toInt()
        
        // 수신 카운터 증가
        receivedCounter.incrementAndGet()
        
        val isValid = expected == received
        
        if (!isValid) {
            // 순번 불일치 시 경고 로그 출력
            val lost = calculateLostFrames(expected, received)
            lostCounter.addAndGet(lost.toLong())
            
            logger.warn { 
                "순번 불일치 감지! 예상: $expected, 수신: $received, 손실 추정: $lost 프레임" 
            }
        } else {
            logger.debug { "순번 검증 성공: $received" }
        }
        
        // 다음 예상 순번 업데이트 (수신된 순번의 다음 값)
        expectedReceiveSequence.set((received + 1) % 256)
        
        return isValid
    }
    
    /**
     * 손실된 프레임 수를 계산합니다.
     * 
     * 예상 순번과 수신 순번의 차이를 통해 손실된 프레임 수를 추정합니다.
     * 순환 카운터 특성을 고려하여 계산합니다 (예: 254, 255, 0, 1, ...)
     * 
     * @param expected 예상 순번
     * @param received 수신 순번
     * @return 손실 추정 프레임 수
     */
    private fun calculateLostFrames(expected: Int, received: Int): Int {
        // 순환 카운터를 고려한 차이 계산
        return if (received >= expected) {
            // 정상적인 순서 (예: 10 -> 15)
            received - expected
        } else {
            // 순환이 발생한 경우 (예: 254 -> 2)
            (256 - expected) + received
        }
    }
    
    /**
     * 순번과 통계를 초기화합니다.
     * 
     * 모든 카운터를 0으로 리셋합니다.
     * 연결이 재시작되거나 디버깅 시 사용할 수 있습니다.
     */
    fun reset() {
        sequence.set(0)
        expectedReceiveSequence.set(0)
        sentCounter.set(0)
        receivedCounter.set(0)
        lostCounter.set(0)
        logger.info { "순번 카운터 초기화 완료" }
    }
    
    /**
     * 현재 통계 정보를 문자열로 반환합니다.
     * 
     * 로깅 및 디버깅 시 사용할 수 있습니다.
     * 
     * @return 통계 정보 문자열
     */
    fun getStatistics(): String {
        return buildString {
            append("순번 통계: ")
            append("전송=$sentCount, ")
            append("수신=$receivedCount, ")
            append("손실=$lostCount, ")
            append("현재순번=$currentSequence, ")
            
            // 손실률 계산 (전송이 0이면 0.0으로 표시)
            val lossRate = if (sentCount > 0) {
                (lostCount.toDouble() / sentCount * 100)
            } else {
                0.0
            }
            append("손실률=${"%.2f".format(lossRate)}%")
        }
    }
    
    /**
     * toString 구현으로 객체 정보를 출력합니다.
     * 
     * @return 객체 정보 문자열
     */
    override fun toString(): String {
        return "SequenceCounter(${getStatistics()})"
    }
}

