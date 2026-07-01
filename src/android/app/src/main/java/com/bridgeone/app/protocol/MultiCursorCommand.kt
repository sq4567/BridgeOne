package com.bridgeone.app.protocol

import org.json.JSONObject

/**
 * 멀티 커서 서버(Windows) 명령 페이로드 빌더 (Phase 4.8.5)
 *
 * Phase 5(Windows 서버) 준비 단계로, 멀티 커서 활성/비활성/패드 전환 시 서버에 보낼
 * JSON 명령 페이로드를 구성하는 순수 함수 모음이다. 실제 UART 전송은 Phase 5에서 완성되며,
 * 이 단계에서는 페이로드 형식만 확정한다.
 *
 * 참조: `docs/android/technical-specification-app.md` §2.2.6,
 * `docs/windows/technical-specification-server.md` §3.6
 */
object MultiCursorCommand {

    const val COMMAND_SHOW_VIRTUAL_CURSOR = "show_virtual_cursor"
    const val COMMAND_HIDE_VIRTUAL_CURSOR = "hide_virtual_cursor"
    const val COMMAND_MULTI_CURSOR_SWITCH = "multi_cursor_switch"

    /**
     * 패드 인덱스(0-based)를 서버 프로토콜의 touchpad_id("pad1", "pad2", ...)로 변환한다.
     */
    fun padIndexToTouchpadId(index: Int): String = "pad${index + 1}"

    /**
     * 멀티 커서 활성화 명령을 생성한다.
     *
     * @param cursorCount 활성화할 커서 수 (2~4)
     */
    fun buildShowVirtualCursor(cursorCount: Int): String = JSONObject().apply {
        put("command", COMMAND_SHOW_VIRTUAL_CURSOR)
        put("cursor_count", cursorCount)
        put("timestamp", currentTimestamp())
    }.toString()

    /** 멀티 커서 비활성화 명령을 생성한다. */
    fun buildHideVirtualCursor(): String = JSONObject().apply {
        put("command", COMMAND_HIDE_VIRTUAL_CURSOR)
        put("timestamp", currentTimestamp())
    }.toString()

    /**
     * 패드 전환 명령을 생성한다.
     *
     * @param touchpadId 전환 대상 패드 ID ("pad1" 등)
     * @param cursorPosition PC 화면상의 저장된 커서 좌표. Android는 PC 화면 크기를 알지 못해
     *   현재는 값을 채울 수 없다(Phase 5에서 `show_virtual_cursor` ACK로 수신 후 채움).
     *   null이면 `cursor_position` 필드는 좌표 없이 자리만 마련한다.
     */
    fun buildMultiCursorSwitch(touchpadId: String, cursorPosition: Pair<Int, Int>?): String =
        JSONObject().apply {
            put("command", COMMAND_MULTI_CURSOR_SWITCH)
            put("touchpad_id", touchpadId)
            put(
                "cursor_position",
                cursorPosition?.let { (x, y) ->
                    JSONObject().apply {
                        put("x", x)
                        put("y", y)
                    }
                } ?: JSONObject.NULL
            )
            put("timestamp", currentTimestamp())
        }.toString()

    // minSdk 24는 java.time.Instant(API 26+)를 desugaring 없이 사용할 수 없어
    // epoch millis 문자열로 대체한다. 로그 수준 타임스탬프이므로 ISO8601 엄밀성보다 호환성 우선.
    private fun currentTimestamp(): String = System.currentTimeMillis().toString()
}
