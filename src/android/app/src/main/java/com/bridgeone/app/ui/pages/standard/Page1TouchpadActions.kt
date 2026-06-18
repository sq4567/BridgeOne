package com.bridgeone.app.ui.pages.standard

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.bridgeone.app.protocol.BridgeMode
import com.bridgeone.app.ui.common.CustomPointerDynamicsPreset
import com.bridgeone.app.ui.common.PAGE1_TOUCHPAD_BOTTOM_TEST_OFFSET
import com.bridgeone.app.ui.common.TouchpadButtonVisibility
import com.bridgeone.app.ui.common.TouchpadEdgeZoneAssignment
import com.bridgeone.app.ui.common.TouchpadIds
import com.bridgeone.app.ui.components.TouchpadWrapper
import com.bridgeone.app.ui.components.touchpad.ControlButtonContainer
import com.bridgeone.app.ui.components.touchpad.DpiAdjustPopup
import com.bridgeone.app.ui.components.touchpad.DynamicsPresetPopup
import com.bridgeone.app.ui.components.touchpad.MacroStep
import com.bridgeone.app.ui.components.touchpad.ModePresetPopup
import com.bridgeone.app.ui.components.touchpad.MouseButton
import com.bridgeone.app.ui.components.touchpad.MouseHoldMode
import com.bridgeone.app.ui.components.touchpad.PageNav
import com.bridgeone.app.ui.components.touchpad.TouchpadState
import com.bridgeone.app.ui.pages.standard.components.ActionsPanel

// ============================================================
// Page 1: 터치패드 + Actions (임시 구현)
// ============================================================

/**
 * Page 1: 터치패드 + Actions
 *
 * Phase 4.2.2: 정식 레이아웃 구현
 * - 좌측: 터치패드 (64%)
 * - 우측: Actions 패널 (36%, LazyColumn 기반)
 * - 반응형: 폭 < 360dp 일 때 좌 60% / 우 40% 조정
 */
@Composable
internal fun Page1TouchpadActions(
    touchpadState: TouchpadState,
    edgeZoneAssignment: TouchpadEdgeZoneAssignment = TouchpadEdgeZoneAssignment.default(),
    onEdgeZoneAssignmentChange: (TouchpadEdgeZoneAssignment) -> Unit = {},
    customPresets: List<CustomPointerDynamicsPreset> = emptyList(),
    onTouchpadStateChange: (TouchpadState) -> Unit,
    onRestorePrevious: () -> Unit = {},
    onSendShortcut: (Int, List<Int>, Boolean) -> Unit = { _, _, _ -> },
    onSendMacro: (List<MacroStep>, Int) -> Unit = { _, _ -> },
    onMouseHoldToggle: (MouseButton, MouseHoldMode) -> Unit = { _, _ -> },
    onCyclePage: (PageNav) -> Unit = {},
    onJumpToPage: (Int) -> Unit = {},
    buttonVisibility: TouchpadButtonVisibility = TouchpadButtonVisibility.default(),
    onButtonVisibilityChange: (TouchpadButtonVisibility) -> Unit = {},
    dpiAdjustPopupVisible: Boolean = false,
    dynamicsPresetPopupVisible: Boolean = false,
    modePresetPopupVisible: Boolean = false,
    onDpiLongPress: () -> Unit = {},
    onDynamicsLongPress: () -> Unit = {},
    onModePresetLongPress: () -> Unit = {},
    onDpiAdjustConfirm: (Float) -> Unit = {},
    onDpiAdjustDismiss: () -> Unit = {},
    onDynamicsPresetConfirmed: (Int) -> Unit = {},
    onDynamicsPresetDismiss: () -> Unit = {},
    onAddCustomPreset: () -> Unit = {},
    onEditCustomPreset: (CustomPointerDynamicsPreset) -> Unit = {},
    onDeleteCustomPreset: (String) -> Unit = {},
    onModePresetConfirmed: (Int) -> Unit = {},
    onModePresetDismiss: () -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp

    // 반응형 비율 계산
    val (touchpadWeight, actionsPanelWeight) = if (screenWidthDp < 360) {
        0.60f to 0.40f
    } else {
        0.64f to 0.36f
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 2열 레이아웃: 좌측 터치패드 + 우측 Actions 패널
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── 좌측: 터치패드 (64% / 60%) ──
            // Phase 4.3.1: Box 내부에 ControlButtonContainer 오버레이 추가
            // Phase 4.3.6 / 4.3.8 / 4.4.8: DPI 팝업, 다이나믹스 팝업, 모드 프리셋 팝업 표시 시 배경 블러 적용
            val blurRadius by animateDpAsState(
                targetValue = if (dpiAdjustPopupVisible || dynamicsPresetPopupVisible || modePresetPopupVisible) 8.dp else 0.dp,
                animationSpec = tween(200),
                label = "popupBlur"
            )
            Box(
                modifier = Modifier
                    .weight(touchpadWeight)
                    .fillMaxHeight()
                    .align(Alignment.CenterVertically)
                    .padding(bottom = PAGE1_TOUCHPAD_BOTTOM_TEST_OFFSET)
            ) {
                // 팝업 표시 시 블러 처리되는 배경 영역
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .blur(blurRadius)
                ) {
                    TouchpadWrapper(
                        touchpadId = TouchpadIds.standardPage(0),
                        bridgeMode = BridgeMode.STANDARD,
                        touchpadState = touchpadState,
                        edgeZoneAssignment = edgeZoneAssignment,
                        onEdgeZoneAssignmentChange = onEdgeZoneAssignmentChange,
                        customPresets = customPresets,
                        onTouchpadStateChange = onTouchpadStateChange,
                        onRestorePrevious = onRestorePrevious,
                        onSendShortcut = onSendShortcut,
                        onSendMacro = onSendMacro,
                        onMouseHoldToggle = onMouseHoldToggle,
                        onCyclePage = onCyclePage,
                        onJumpToPage = onJumpToPage,
                        onDynamicsLongPress = onDynamicsLongPress,
                        onModePresetLongPress = onModePresetLongPress,
                        buttonVisibility = buttonVisibility,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                color = Color(0xFF1A1A1A),
                                shape = RoundedCornerShape(12.dp)
                            )
                    )

                    // Phase 4.3.1: ControlButtonContainer 오버레이 (상단 15%, 마스터 ON일 때만)
                    if (buttonVisibility.showControlButtons) {
                        ControlButtonContainer(
                            touchpadState = touchpadState,
                            onStateChange = onTouchpadStateChange,
                            onDpiLongPress = onDpiLongPress,
                            config = buttonVisibility.controlButtonConfig,
                            modifier = Modifier
                                .fillMaxSize()
                                .align(Alignment.TopCenter)
                        )
                    }
                }

                // Phase 4.3.6: DPI 세밀 조절 팝업 오버레이
                // 팝업 내 pointerInput이 이벤트를 소비 → TouchpadWrapper 제스처 자동 차단
                if (dpiAdjustPopupVisible) {
                    DpiAdjustPopup(
                        initialMultiplier = touchpadState.effectiveDpiMultiplier,
                        onConfirm = onDpiAdjustConfirm,
                        onDismiss = onDpiAdjustDismiss,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                    )
                }

                // Phase 4.3.8 / 4.3.9 / 4.5.16: 다이나믹스 프리셋 팝업 오버레이
                // 항상 렌더링하고 visible 파라미터로 제어 (exit 애니메이션 보장)
                DynamicsPresetPopup(
                    visible = dynamicsPresetPopupVisible,
                    currentIndex = touchpadState.dynamicsPresetIndex,
                    customPresets = customPresets,
                    onPresetConfirmed = onDynamicsPresetConfirmed,
                    onDismiss = onDynamicsPresetDismiss,
                    onAddCustomPreset = onAddCustomPreset,
                    onEditCustomPreset = onEditCustomPreset,
                    onDeleteCustomPreset = onDeleteCustomPreset,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                )

                // Phase 4.4.8: 모드 프리셋 팝업 오버레이
                // 항상 렌더링하고 visible 파라미터로 제어 (exit 애니메이션 보장)
                ModePresetPopup(
                    visible = modePresetPopupVisible,
                    currentIndex = touchpadState.modePresetIndex,
                    onPresetConfirmed = onModePresetConfirmed,
                    onDismiss = onModePresetDismiss,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                )

            }

            // ── 우측: Actions 패널 (36% / 40%) ──
            ActionsPanel(
                modifier = Modifier
                    .weight(actionsPanelWeight)
                    .fillMaxHeight()
            )
        }
    }
}
