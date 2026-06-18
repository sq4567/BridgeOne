package com.bridgeone.app.ui.common

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import com.bridgeone.app.ui.common.ScrollConstants.INFINITE_SCROLL_HAPTIC_AMPLITUDE_MAX
import com.bridgeone.app.ui.common.ScrollConstants.INFINITE_SCROLL_HAPTIC_AMPLITUDE_MIN
import com.bridgeone.app.ui.common.ScrollConstants.INFINITE_SCROLL_HAPTIC_DURATION_MS
import com.bridgeone.app.ui.common.ScrollConstants.INFINITE_SCROLL_HAPTIC_MAX_VELOCITY_DP_MS

/**
 * 무한 스크롤 속도 비례 연속 진동을 담당하는 햅틱 헬퍼 (Phase 4.7.3-B).
 *
 * 기존에 TouchpadWrapper에서 터치 드래그 중(1242행)과 관성 코루틴 중(1400행)
 * 두 곳에 완전히 중복된 vibrator.vibrate(...) 호출 블록을 단일화한다.
 * amplitude 계산식과 VibrationEffect.createOneShot 생성이 동일하므로
 * [vibrateByVelocity]로 흡수한다. SDK 가드도 헬퍼 내부에서 처리한다.
 *
 * Phase 4.15.2에서 HAPTIC_MIN_INTERVAL_MS 시간 게이트를 이 클래스에 추가하면
 * 호출부 수정 없이 전체 햅틱 호출 빈도가 일괄 제어된다.
 */
class HapticFeedbackHelper(private val vibrator: Vibrator) {

    /**
     * 속도([velocity] dp/ms)에 비례한 단발 진동을 실행합니다.
     * SDK O 미만에서는 무시됩니다.
     *
     * amplitude = (velocity / MAX_VELOCITY * AMPLITUDE_MAX).coerceIn(MIN, MAX)
     */
    fun vibrateByVelocity(velocity: Float) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val amplitude = (velocity / INFINITE_SCROLL_HAPTIC_MAX_VELOCITY_DP_MS * INFINITE_SCROLL_HAPTIC_AMPLITUDE_MAX)
            .toInt().coerceIn(INFINITE_SCROLL_HAPTIC_AMPLITUDE_MIN, INFINITE_SCROLL_HAPTIC_AMPLITUDE_MAX)
        vibrator.vibrate(VibrationEffect.createOneShot(INFINITE_SCROLL_HAPTIC_DURATION_MS, amplitude))
    }
}
