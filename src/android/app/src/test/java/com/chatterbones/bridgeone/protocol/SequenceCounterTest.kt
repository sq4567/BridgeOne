package com.chatterbones.bridgeone.protocol

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * SequenceCounter 클래스의 유닛 테스트
 * 
 * Phase 1.1.1.5의 검증 방법을 기반으로 작성되었습니다:
 * - next() 호출 시 순번 증가 확인
 * - 255 다음에 0으로 순환 확인
 * - validate() 정상 순번에서 true 반환
 * - 순번 불일치 시 로그 출력 확인
 */
class SequenceCounterTest {
    
    private lateinit var counter: SequenceCounter
    
    @Before
    fun setUp() {
        counter = SequenceCounter()
    }
    
    /**
     * 테스트 1: next() 호출 시 순번이 증가하는지 확인
     */
    @Test
    fun `next() should increment sequence number`() {
        // Given: 초기 상태의 카운터
        assertEquals(0.toUByte(), counter.currentSequence)
        
        // When: next()를 여러 번 호출
        val first = counter.next()
        val second = counter.next()
        val third = counter.next()
        
        // Then: 순번이 순차적으로 증가
        assertEquals(1.toUByte(), first)
        assertEquals(2.toUByte(), second)
        assertEquals(3.toUByte(), third)
        assertEquals(3.toUByte(), counter.currentSequence)
    }
    
    /**
     * 테스트 2: 255 다음에 0으로 순환하는지 확인
     */
    @Test
    fun `next() should wrap from 255 to 0`() {
        // Given: 순번을 254까지 증가
        repeat(254) {
            counter.next()
        }
        assertEquals(254.toUByte(), counter.currentSequence)
        
        // When: 다음 두 번 호출
        val seq255 = counter.next()
        val seq0 = counter.next()
        
        // Then: 255 다음에 0으로 순환
        assertEquals(255.toUByte(), seq255)
        assertEquals(0.toUByte(), seq0)
        assertEquals(0.toUByte(), counter.currentSequence)
    }
    
    /**
     * 테스트 3: validate() 정상 순번에서 true 반환
     */
    @Test
    fun `validate() should return true for expected sequence`() {
        // Given: 예상 순번 0, 1, 2, 3
        
        // When & Then: 순차적으로 검증
        assertTrue(counter.validate(0.toUByte()))
        assertTrue(counter.validate(1.toUByte()))
        assertTrue(counter.validate(2.toUByte()))
        assertTrue(counter.validate(3.toUByte()))
        
        // 수신 카운터 확인
        assertEquals(4L, counter.receivedCount)
        assertEquals(0L, counter.lostCount)
    }
    
    /**
     * 테스트 4: validate() 순번 불일치 시 false 반환 및 손실 카운트
     */
    @Test
    fun `validate() should return false for unexpected sequence and count lost frames`() {
        // Given: 예상 순번 0
        
        // When: 순번 0을 건너뛰고 순번 3 수신
        val result = counter.validate(3.toUByte())
        
        // Then: 검증 실패 및 손실 카운트 증가
        assertFalse(result)
        assertEquals(1L, counter.receivedCount)
        assertEquals(3L, counter.lostCount) // 0, 1, 2가 손실된 것으로 추정
    }
    
    /**
     * 테스트 5: validate() 순환 경계에서의 손실 카운트
     */
    @Test
    fun `validate() should handle sequence wrap-around correctly`() {
        // Given: 예상 순번을 254로 설정
        for (i in 0..253) {
            counter.validate(i.toUByte())
        }
        
        // When: 254를 건너뛰고 2 수신 (254, 255, 0, 1 손실)
        val result = counter.validate(2.toUByte())
        
        // Then: 손실 카운트가 올바르게 계산됨
        assertFalse(result)
        assertEquals(4L, counter.lostCount) // 254, 255, 0, 1이 손실
    }
    
    /**
     * 테스트 6: 전송 카운터 증가 확인
     */
    @Test
    fun `next() should increment sent counter`() {
        // Given: 초기 상태
        assertEquals(0L, counter.sentCount)
        
        // When: next()를 10번 호출
        repeat(10) {
            counter.next()
        }
        
        // Then: 전송 카운터가 10 증가
        assertEquals(10L, counter.sentCount)
    }
    
    /**
     * 테스트 7: reset() 기능 확인
     */
    @Test
    fun `reset() should clear all counters`() {
        // Given: 일부 데이터가 있는 상태
        repeat(10) { counter.next() }
        counter.validate(5.toUByte())
        
        // When: reset() 호출
        counter.reset()
        
        // Then: 모든 카운터가 0으로 초기화
        assertEquals(0.toUByte(), counter.currentSequence)
        assertEquals(0L, counter.sentCount)
        assertEquals(0L, counter.receivedCount)
        assertEquals(0L, counter.lostCount)
    }
    
    /**
     * 테스트 8: 통계 정보 출력 확인
     */
    @Test
    fun `getStatistics() should return correct statistics string`() {
        // Given: 일부 데이터 생성
        repeat(100) { counter.next() }
        repeat(95) { counter.validate(it.toUByte()) }
        
        // When: 통계 정보 요청
        val stats = counter.getStatistics()
        
        // Then: 통계 정보에 필요한 내용이 포함됨
        assertTrue(stats.contains("전송=100"))
        assertTrue(stats.contains("수신=95"))
        assertTrue(stats.contains("손실률="))
    }
    
    /**
     * 테스트 9: 다중 순환 확인
     */
    @Test
    fun `next() should handle multiple wrap-arounds correctly`() {
        // Given: 초기 상태
        
        // When: 512번 호출 (2번 순환)
        repeat(512) {
            counter.next()
        }
        
        // Then: 순번이 0으로 돌아옴 (256 * 2 % 256 = 0)
        assertEquals(0.toUByte(), counter.currentSequence)
        assertEquals(512L, counter.sentCount)
    }
    
    /**
     * 테스트 10: 연속된 순번 검증
     */
    @Test
    fun `validate() should accept continuous sequence correctly`() {
        // Given & When: 0-255까지 연속 검증
        repeat(256) {
            val result = counter.validate(it.toUByte())
            
            // Then: 모두 성공
            assertTrue("Sequence $it should be valid", result)
        }
        
        // Then: 손실 없음
        assertEquals(256L, counter.receivedCount)
        assertEquals(0L, counter.lostCount)
    }
}

