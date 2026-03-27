package com.bridgeone.app.ui.common

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

// ============================================================
// HID 키 코드 상수 (Essential/Standard 공용)
// ============================================================
val KEY_ENTER = 0x28.toUByte()
val KEY_ESC = 0x29.toUByte()
val KEY_BACKSPACE = 0x2A.toUByte()
val KEY_TAB = 0x2B.toUByte()
val KEY_SPACE = 0x2C.toUByte()
val KEY_HOME = 0x4A.toUByte()
val KEY_DELETE = 0x4C.toUByte()
val KEY_END = 0x4D.toUByte()
val KEY_F1 = 0x3A.toUByte(); val KEY_F2 = 0x3B.toUByte()
val KEY_F3 = 0x3C.toUByte(); val KEY_F4 = 0x3D.toUByte()
val KEY_F5 = 0x3E.toUByte(); val KEY_F6 = 0x3F.toUByte()
val KEY_F7 = 0x40.toUByte(); val KEY_F8 = 0x41.toUByte()
val KEY_F9 = 0x42.toUByte(); val KEY_F10 = 0x43.toUByte()
val KEY_F11 = 0x44.toUByte(); val KEY_F12 = 0x45.toUByte()
val KEY_RIGHT = 0x4F.toUByte()
val KEY_LEFT = 0x50.toUByte()
val KEY_DOWN = 0x51.toUByte()
val KEY_UP = 0x52.toUByte()

// ============================================================
// 공통 유틸리티
// ============================================================

/**
 * 햅틱 피드백 (50ms 진동)
 */
fun triggerHaptic(context: Context) {
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(50)
    }
}

/**
 * 수정자 키 코드 판별 (HID Usage 0xE0-0xE7)
 */
fun isModifierKeyCode(keyCode: UByte): Boolean =
    keyCode.toInt() in 0xE0..0xE7

/**
 * 수정자 키 코드(HID Usage) → HID 수정자 비트 플래그 변환
 */
fun modifierBitFlag(keyCode: UByte): UByte = when (keyCode.toInt()) {
    0xE0 -> 0x01  // Left Ctrl
    0xE1 -> 0x02  // Left Shift
    0xE2 -> 0x04  // Left Alt
    0xE3 -> 0x08  // Left GUI (Win)
    0xE4 -> 0x10  // Right Ctrl (한자)
    0xE5 -> 0x20  // Right Shift
    0xE6 -> 0x40  // Right Alt (한/영)
    0xE7 -> 0x80  // Right GUI
    else -> 0x00
}.toUByte()
