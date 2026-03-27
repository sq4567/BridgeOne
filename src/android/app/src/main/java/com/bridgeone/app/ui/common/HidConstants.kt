package com.bridgeone.app.ui.common

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

// ============================================================
// HID 키 코드 상수 (Essential/Standard 공용)
// USB HID Usage Tables — Keyboard/Keypad Page (0x07)
// ============================================================

// ── 문자 키 (a=0x04 ~ z=0x1D) ──
val KEY_A = 0x04.toUByte(); val KEY_B = 0x05.toUByte()
val KEY_C = 0x06.toUByte(); val KEY_D = 0x07.toUByte()
val KEY_E = 0x08.toUByte(); val KEY_F = 0x09.toUByte()
val KEY_G = 0x0A.toUByte(); val KEY_H = 0x0B.toUByte()
val KEY_I = 0x0C.toUByte(); val KEY_J = 0x0D.toUByte()
val KEY_K = 0x0E.toUByte(); val KEY_L = 0x0F.toUByte()
val KEY_M = 0x10.toUByte(); val KEY_N = 0x11.toUByte()
val KEY_O = 0x12.toUByte(); val KEY_P = 0x13.toUByte()
val KEY_Q = 0x14.toUByte(); val KEY_R = 0x15.toUByte()
val KEY_S = 0x16.toUByte(); val KEY_T = 0x17.toUByte()
val KEY_U = 0x18.toUByte(); val KEY_V = 0x19.toUByte()
val KEY_W = 0x1A.toUByte(); val KEY_X = 0x1B.toUByte()
val KEY_Y = 0x1C.toUByte(); val KEY_Z = 0x1D.toUByte()

// ── 숫자 키 (1=0x1E ~ 0=0x27) ──
val KEY_1 = 0x1E.toUByte(); val KEY_2 = 0x1F.toUByte()
val KEY_3 = 0x20.toUByte(); val KEY_4 = 0x21.toUByte()
val KEY_5 = 0x22.toUByte(); val KEY_6 = 0x23.toUByte()
val KEY_7 = 0x24.toUByte(); val KEY_8 = 0x25.toUByte()
val KEY_9 = 0x26.toUByte(); val KEY_0 = 0x27.toUByte()

// ── 제어 키 ──
val KEY_ENTER = 0x28.toUByte()
val KEY_ESC = 0x29.toUByte()
val KEY_BACKSPACE = 0x2A.toUByte()
val KEY_TAB = 0x2B.toUByte()
val KEY_SPACE = 0x2C.toUByte()

// ── 기호 키 ──
val KEY_MINUS = 0x2D.toUByte()         // - _
val KEY_EQUAL = 0x2E.toUByte()         // = +
val KEY_LEFTBRACE = 0x2F.toUByte()     // [ {
val KEY_RIGHTBRACE = 0x30.toUByte()    // ] }
val KEY_BACKSLASH = 0x31.toUByte()     // \ |
val KEY_SEMICOLON = 0x33.toUByte()     // ; :
val KEY_APOSTROPHE = 0x34.toUByte()    // ' "
val KEY_GRAVE = 0x35.toUByte()         // ` ~
val KEY_COMMA = 0x36.toUByte()         // , <
val KEY_DOT = 0x37.toUByte()           // . >
val KEY_SLASH = 0x38.toUByte()         // / ?
val KEY_CAPSLOCK = 0x39.toUByte()

// ── 펑션 키 (F1=0x3A ~ F12=0x45) ──
val KEY_F1 = 0x3A.toUByte(); val KEY_F2 = 0x3B.toUByte()
val KEY_F3 = 0x3C.toUByte(); val KEY_F4 = 0x3D.toUByte()
val KEY_F5 = 0x3E.toUByte(); val KEY_F6 = 0x3F.toUByte()
val KEY_F7 = 0x40.toUByte(); val KEY_F8 = 0x41.toUByte()
val KEY_F9 = 0x42.toUByte(); val KEY_F10 = 0x43.toUByte()
val KEY_F11 = 0x44.toUByte(); val KEY_F12 = 0x45.toUByte()

// ── 시스템/편집 키 ──
val KEY_PRINTSCREEN = 0x46.toUByte()
val KEY_SCROLLLOCK = 0x47.toUByte()
val KEY_PAUSE = 0x48.toUByte()
val KEY_INSERT = 0x49.toUByte()
val KEY_HOME = 0x4A.toUByte()
val KEY_PAGEUP = 0x4B.toUByte()
val KEY_DELETE = 0x4C.toUByte()
val KEY_END = 0x4D.toUByte()
val KEY_PAGEDOWN = 0x4E.toUByte()

// ── 방향 키 ──
val KEY_RIGHT = 0x4F.toUByte()
val KEY_LEFT = 0x50.toUByte()
val KEY_DOWN = 0x51.toUByte()
val KEY_UP = 0x52.toUByte()

// ── 넘패드 키 ──
val KEY_NUMLOCK = 0x53.toUByte()
val KEY_KP_SLASH = 0x54.toUByte()
val KEY_KP_ASTERISK = 0x55.toUByte()
val KEY_KP_MINUS = 0x56.toUByte()
val KEY_KP_PLUS = 0x57.toUByte()
val KEY_KP_ENTER = 0x58.toUByte()
val KEY_KP_1 = 0x59.toUByte(); val KEY_KP_2 = 0x5A.toUByte()
val KEY_KP_3 = 0x5B.toUByte(); val KEY_KP_4 = 0x5C.toUByte()
val KEY_KP_5 = 0x5D.toUByte(); val KEY_KP_6 = 0x5E.toUByte()
val KEY_KP_7 = 0x5F.toUByte(); val KEY_KP_8 = 0x60.toUByte()
val KEY_KP_9 = 0x61.toUByte(); val KEY_KP_0 = 0x62.toUByte()
val KEY_KP_DOT = 0x63.toUByte()

// ── 미디어/시스템 키 (Consumer Page 0x0C — 별도 HID Report 필요) ──
// 참고: 이 키들은 Keyboard Page가 아닌 Consumer Page에 해당하며,
// 실제 전송 시 별도 Consumer HID Report를 사용해야 합니다.
// 여기서는 앱 내 식별용 상수로만 정의합니다.
val KEY_MEDIA_PLAYPAUSE = 0xE8.toUByte()
val KEY_MEDIA_STOP = 0xE9.toUByte()
val KEY_MEDIA_NEXT = 0xEA.toUByte()
val KEY_MEDIA_PREV = 0xEB.toUByte()
val KEY_MEDIA_VOLUP = 0xEC.toUByte()
val KEY_MEDIA_VOLDN = 0xED.toUByte()
val KEY_MEDIA_MUTE = 0xEE.toUByte()

// ── 수정자 키 코드 (HID Usage 0xE0-0xE7) ──
val MOD_KEY_LCTRL = 0xE0.toUByte()
val MOD_KEY_LSHIFT = 0xE1.toUByte()
val MOD_KEY_LALT = 0xE2.toUByte()
val MOD_KEY_LGUI = 0xE3.toUByte()
val MOD_KEY_RCTRL = 0xE4.toUByte()    // 한자
val MOD_KEY_RSHIFT = 0xE5.toUByte()
val MOD_KEY_RALT = 0xE6.toUByte()     // 한/영
val MOD_KEY_RGUI = 0xE7.toUByte()

// 수정자 키 별칭 (KeyboardLayout 등 레거시 호환)
val KEY_CTRL_LEFT = MOD_KEY_LCTRL
val KEY_SHIFT_LEFT = MOD_KEY_LSHIFT
val KEY_ALT_LEFT = MOD_KEY_LALT
val KEY_GUI_LEFT = MOD_KEY_LGUI
val KEY_HAN_YEONG = MOD_KEY_RALT      // 한/영 전환
val KEY_HANJA = MOD_KEY_RCTRL         // 한자 변환

// ── 수정자 비트플래그 (프레임 내 modifier 바이트용) ──
val MOD_BIT_LCTRL = 0x01.toUByte()
val MOD_BIT_LSHIFT = 0x02.toUByte()
val MOD_BIT_LALT = 0x04.toUByte()
val MOD_BIT_LGUI = 0x08.toUByte()
val MOD_BIT_RCTRL = 0x10.toUByte()
val MOD_BIT_RSHIFT = 0x20.toUByte()
val MOD_BIT_RALT = 0x40.toUByte()
val MOD_BIT_RGUI = 0x80.toUByte()

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
