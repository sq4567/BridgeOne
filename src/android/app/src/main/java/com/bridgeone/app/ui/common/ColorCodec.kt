package com.bridgeone.app.ui.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

// ============================================================
// ColorCodec — Color ↔ hex String / HSV 변환 유틸
// ============================================================

/**
 * Color ↔ hex String / HSV 변환 유틸.
 *
 * 색상 직렬화 포맷: "#AARRGGBB" hex 문자열 (8자리).
 * 이 포맷으로 JSON에 저장하면 org.json.optString("")으로 안전하게 읽을 수 있다.
 * (빈 문자열 → 기본색 폴백 패턴)
 */
object ColorCodec {

    /**
     * Color → "#AARRGGBB" 8자리 hex 문자열.
     * 예: `Color(0xFF2196F3)` → `"#FF2196F3"`
     */
    fun colorToHex(color: Color): String = "#%08X".format(color.toArgb())

    /**
     * "#AARRGGBB" 또는 "#RRGGBB" hex 문자열 → Color.
     * '#' 생략 가능. 파싱 실패 또는 길이 불일치 시 null.
     */
    fun hexToColorOrNull(hex: String): Color? = runCatching {
        val clean = hex.trimStart('#')
        when (clean.length) {
            6 -> Color(0xFF000000L or clean.toLong(16))
            8 -> Color(clean.toLong(16))
            else -> null
        }
    }.getOrNull()

    /**
     * HSV → Color.
     *
     * @param h Hue 0~360f
     * @param s Saturation 0~1f
     * @param v Value 0~1f
     */
    fun hsvToColor(h: Float, s: Float, v: Float): Color {
        val argb = android.graphics.Color.HSVToColor(floatArrayOf(h, s, v))
        return Color(argb)
    }

    /**
     * Color → FloatArray(size=3): [hue(0~360f), saturation(0~1f), value(0~1f)]
     * alpha 채널은 무시된다.
     */
    fun colorToHsv(color: Color): FloatArray {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(color.toArgb(), hsv)
        return hsv
    }
}
