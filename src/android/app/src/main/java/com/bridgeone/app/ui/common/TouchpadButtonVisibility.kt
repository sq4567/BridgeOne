package com.bridgeone.app.ui.common

import com.bridgeone.app.ui.components.touchpad.ControlButtonConfig

/**
 * 특정 터치패드에서 각 버튼 그룹의 표시 여부.
 *
 * @param showControlButtons 제어 버튼 마스터 (전체 on/off). 기본값: true
 * @param controlButtonConfig 제어 버튼 개별 6개 토글 (마스터 ON일 때 적용). 기본값: ControlButtonConfig()
 * @param showDynamicsButton 포인트 다이나믹스 버튼 표시 여부. 기본값: true
 * @param showModePresetButton 모드 프리셋 버튼 표시 여부. 기본값: true
 * @param showScrollButtons 스크롤 위/아래 버튼 표시 여부(묶음). 기본값: true
 */
data class TouchpadButtonVisibility(
    val showControlButtons: Boolean = true,
    val controlButtonConfig: ControlButtonConfig = ControlButtonConfig(),
    val showDynamicsButton: Boolean = true,
    val showModePresetButton: Boolean = true,
    val showScrollButtons: Boolean = true,
) {
    companion object {
        fun default() = TouchpadButtonVisibility()

        /** 페이지별 초기 기본값 — 영속 데이터 없을 때 사용하며 기존 동작을 보존한다 */
        fun defaultFor(touchpadId: String): TouchpadButtonVisibility = when (touchpadId) {
            TouchpadIds.standardPage(1) -> TouchpadButtonVisibility(
                showControlButtons = true,
                controlButtonConfig = ControlButtonConfig(showCursorMode = true),
                showDynamicsButton = false,
                showModePresetButton = false,
                showScrollButtons = true,
            )
            // Phase 4.9.8 후속 수정: Page 3(절대좌표 패드)는 다이나믹스/모드 프리셋/스크롤 버튼이 없다.
            // 이 값이 true로 폴백되면 엣지존 편집기의 "코너 버튼 크기" 슬라이더가 실제로 없는
            // 코너 버튼을 근거로 잘못 노출된다.
            TouchpadIds.standardPage(2) -> TouchpadButtonVisibility(
                showControlButtons = true,
                showDynamicsButton = false,
                showModePresetButton = false,
                showScrollButtons = false,
            )
            else -> default()
        }
    }
}
