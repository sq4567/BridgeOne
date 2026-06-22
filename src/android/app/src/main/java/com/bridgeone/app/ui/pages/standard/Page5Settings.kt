package com.bridgeone.app.ui.pages.standard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.bridgeone.app.ui.common.InputMode
import com.bridgeone.app.ui.common.TouchpadButtonVisibility
import com.bridgeone.app.ui.common.TouchpadEdgeZoneAssignment
import com.bridgeone.app.ui.common.TouchpadIds
import com.bridgeone.app.ui.common.TtsGender
import com.bridgeone.app.ui.components.touchpad.EdgeInteractionMode
import com.bridgeone.app.ui.components.touchpad.TouchpadState

// ============================================================
// Page 5: 설정
// ============================================================

@Composable
internal fun Page5Settings(
    touchpadState: TouchpadState,
    onTouchpadStateChange: (TouchpadState) -> Unit,
    inputMode: InputMode = InputMode.NORMAL,
    onInputModeChange: (InputMode) -> Unit = {},
    swipeWrapEdge: Boolean = false,
    onSwipeWrapEdgeChange: (Boolean) -> Unit = {},
    audioFeedbackEnabled: Boolean = true,
    onAudioFeedbackEnabledChange: (Boolean) -> Unit = {},
    ttsRate: Float = 1.0f,
    onTtsRateChange: (Float) -> Unit = {},
    ttsGender: TtsGender = TtsGender.DEFAULT,
    onTtsGenderChange: (TtsGender) -> Unit = {},
    standardAssignments: Map<Int, TouchpadEdgeZoneAssignment> = emptyMap(),
    selectedZonePage: Int = 0,
    onSelectedZonePageChange: (Int) -> Unit = {},
    onOpenZoneEditor: () -> Unit = {},
    standardButtonVisibility: Map<Int, TouchpadButtonVisibility> = emptyMap(),
    onButtonVisibilityChange: (Int, TouchpadButtonVisibility) -> Unit = { _, _ -> }
) {
    val sortedPages = standardAssignments.keys.sorted()
    val currentAssignment = standardAssignments[selectedZonePage] ?: TouchpadEdgeZoneAssignment.default()
    val currentVisibility = standardButtonVisibility[selectedZonePage] ?: TouchpadButtonVisibility.defaultFor(TouchpadIds.standardPage(selectedZonePage))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E), RoundedCornerShape(12.dp))
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "환경 설정",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFFFFF)
                )
            }

            item {
                Text(
                    text = "조작 방식",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFAAAAAA),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            item {
                SettingsInputModeSection(
                    currentMode = inputMode,
                    onModeSelected = onInputModeChange
                )
            }

            if (inputMode == InputMode.SWIPE) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF2A2A2A), RoundedCornerShape(8.dp))
                            .clickable { onSwipeWrapEdgeChange(!swipeWrapEdge) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "끝점 순환",
                                fontSize = 14.sp,
                                color = Color(0xFFEEEEEE)
                            )
                            Text(
                                text = "행의 마지막 요소에서 첫 요소로, 첫 요소에서 마지막 요소로 이동합니다",
                                fontSize = 12.sp,
                                color = Color(0xFF888888)
                            )
                        }
                        androidx.compose.material3.Switch(
                            checked = swipeWrapEdge,
                            onCheckedChange = onSwipeWrapEdgeChange
                        )
                    }
                }
            }

            item {
                Text(
                    text = "엣지 조작 방식",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFAAAAAA),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            item {
                SettingsEdgeInteractionModeSection(
                    currentMode = touchpadState.edgeInteractionMode,
                    onModeSelected = { mode ->
                        onTouchpadStateChange(touchpadState.copy(edgeInteractionMode = mode))
                    }
                )
            }

            // 터치패드 페이지 셀렉터 (페이지가 2개 이상일 때만 표시, 엣지 존 + 버튼 표시 두 섹션이 공유)
            if (sortedPages.size > 1) {
                item {
                    Text(
                        text = "터치패드",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFAAAAAA)
                    )
                }
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        sortedPages.forEach { pageIdx ->
                            val isSelected = pageIdx == selectedZonePage
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFF2979FF).copy(alpha = 0.2f) else Color(0xFF2A2A2A))
                                    .border(
                                        width = if (isSelected) 1.5.dp else 0.5.dp,
                                        color = if (isSelected) Color(0xFF2979FF) else Color(0xFF444444),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onSelectedZonePageChange(pageIdx) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "페이지 ${pageIdx + 1}",
                                    fontSize = 13.sp,
                                    color = if (isSelected) Color(0xFF2979FF) else Color(0xFFCCCCCC)
                                )
                            }
                        }
                    }
                }
            }

            // 편집기 진입 항목 (ZONE 모드 선택 시에만 표시)
            if (touchpadState.edgeInteractionMode == EdgeInteractionMode.ZONE) {
                item {
                    val zoneCount = currentAssignment.config.run {
                        topZones.size + bottomZones.size + leftZones.size + rightZones.size
                    }
                    ZoneEditorEntryRow(
                        zoneCount = zoneCount,
                        onClick = onOpenZoneEditor
                    )
                }

                // 존 음성 안내 설정 (ZONE 모드에서만 유의미)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAudioFeedbackEnabledChange(!audioFeedbackEnabled) }
                            .padding(horizontal = 4.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "존 음성 안내", fontSize = 14.sp, color = Color(0xFFE0E0E0))
                            Text(
                                text = "존 진입 시 배정된 액션 이름을 읽어줌",
                                fontSize = 12.sp,
                                color = Color(0xFF888888)
                            )
                        }
                        androidx.compose.material3.Switch(
                            checked = audioFeedbackEnabled,
                            onCheckedChange = onAudioFeedbackEnabledChange
                        )
                    }
                }

                // TTS 말하기 속도 (음성 안내 켜진 경우에만 활성)
                if (audioFeedbackEnabled) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = "말하기 속도", fontSize = 14.sp, color = Color(0xFFE0E0E0))
                            com.bridgeone.app.ui.common.CustomTrackSlider(
                                value = ttsRate,
                                onValueChange = onTtsRateChange,
                                valueRange = 0.5f..3.0f,
                                valueLabel = "×${"%.1f".format(ttsRate)}",
                                labelWidth = 40.dp,
                                snap = { (it * 10f).roundToInt() / 10f },
                                majorTickStep = 0.5f,
                                minorTickStep = 0.1f,
                            )
                        }
                    }

                    // 음성 성별 선택
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "음성 성별", fontSize = 14.sp, color = Color(0xFFE0E0E0))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    TtsGender.FEMALE to "여성",
                                    TtsGender.MALE to "남성"
                                ).forEach { (g, label) ->
                                    val isSelected = ttsGender == g
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isSelected) Color(0xFF2979FF).copy(alpha = 0.2f)
                                                else Color(0xFF2A2A2A)
                                            )
                                            .border(
                                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                                color = if (isSelected) Color(0xFF2979FF) else Color(0xFF444444),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable { onTtsGenderChange(g) }
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 13.sp,
                                            color = if (isSelected) Color(0xFF2979FF) else Color(0xFFCCCCCC)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 버튼 표시 섹션
            item {
                Text(
                    text = "버튼 표시",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFAAAAAA),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            item {
                SettingsButtonVisibilitySection(
                    visibility = currentVisibility,
                    onVisibilityChange = { updated ->
                        onButtonVisibilityChange(selectedZonePage, updated)
                    }
                )
            }
        }
    }
}

// ============================================================
// 설정 하위 섹션 컴포넌트
// ============================================================

@Composable
private fun SettingsInputModeSection(
    currentMode: InputMode,
    onModeSelected: (InputMode) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        InputMode.entries.forEach { mode ->
            val isSelected = currentMode == mode
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isSelected) Color(0xFF2A2A2A) else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onModeSelected(mode) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick = { onModeSelected(mode) }
                )
                Column {
                    Text(
                        text = when (mode) {
                            InputMode.NORMAL -> "일반"
                            InputMode.SWIPE -> "스와이프"
                        },
                        fontSize = 14.sp,
                        color = Color(0xFFEEEEEE)
                    )
                    Text(
                        text = when (mode) {
                            InputMode.NORMAL -> "요소를 직접 터치하고 드래그합니다"
                            InputMode.SWIPE -> "화면 어디서나 스와이프로 선택, 어디서나 터치로 조작합니다"
                        },
                        fontSize = 12.sp,
                        color = Color(0xFF888888)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsEdgeInteractionModeSection(
    currentMode: EdgeInteractionMode,
    onModeSelected: (EdgeInteractionMode) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        EdgeInteractionMode.entries.forEach { mode ->
            val isSelected = currentMode == mode
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isSelected) Color(0xFF2A2A2A) else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick = { onModeSelected(mode) }
                )
                Text(
                    text = when (mode) {
                        EdgeInteractionMode.LEGACY_POPUP -> "기존 팝업 방식 (5단계)"
                        EdgeInteractionMode.ZONE -> "엣지 존 방식"
                    },
                    fontSize = 14.sp,
                    color = Color(0xFFEEEEEE)
                )
            }
        }
    }
}

// ============================================================
// 존 편집기 진입 행 (Phase 4.6.2)
// ============================================================

@Composable
private fun ZoneEditorEntryRow(
    zoneCount: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF2A2A2A), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("엣지 존 편집", fontSize = 14.sp, color = Color(0xFFEEEEEE))
            Text("${zoneCount}개 존 설정됨", fontSize = 12.sp, color = Color(0xFF888888))
        }
        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = null,
            tint = Color(0xFF888888),
            modifier = Modifier.size(20.dp)
        )
    }
}

// ============================================================
// 버튼 표시 설정 섹션
// ============================================================

@Composable
private fun SettingsButtonVisibilitySection(
    visibility: TouchpadButtonVisibility,
    onVisibilityChange: (TouchpadButtonVisibility) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF2A2A2A), RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // 제어 버튼 마스터 토글
        SettingsToggleRow(
            label = "제어 버튼",
            checked = visibility.showControlButtons,
            onCheckedChange = { onVisibilityChange(visibility.copy(showControlButtons = it)) }
        )
        // 제어 버튼 마스터 ON일 때만 개별 토글 노출
        if (visibility.showControlButtons) {
            val config = visibility.controlButtonConfig
            SettingsToggleRow(
                label = "  클릭 모드 버튼",
                checked = config.showClickMode,
                onCheckedChange = {
                    onVisibilityChange(visibility.copy(controlButtonConfig = config.copy(showClickMode = it)))
                }
            )
            SettingsToggleRow(
                label = "  이동 모드 버튼",
                checked = config.showMoveMode,
                onCheckedChange = {
                    onVisibilityChange(visibility.copy(controlButtonConfig = config.copy(showMoveMode = it)))
                }
            )
            SettingsToggleRow(
                label = "  스크롤 모드 버튼",
                checked = config.showScrollMode,
                onCheckedChange = {
                    onVisibilityChange(visibility.copy(controlButtonConfig = config.copy(showScrollMode = it)))
                }
            )
            SettingsToggleRow(
                label = "  커서 모드 버튼",
                checked = config.showCursorMode,
                onCheckedChange = {
                    onVisibilityChange(visibility.copy(controlButtonConfig = config.copy(showCursorMode = it)))
                }
            )
            SettingsToggleRow(
                label = "  DPI 버튼",
                checked = config.showDpi,
                onCheckedChange = {
                    onVisibilityChange(visibility.copy(controlButtonConfig = config.copy(showDpi = it)))
                }
            )
            SettingsToggleRow(
                label = "  스크롤 감도 버튼",
                checked = config.showScrollSensitivity,
                onCheckedChange = {
                    onVisibilityChange(visibility.copy(controlButtonConfig = config.copy(showScrollSensitivity = it)))
                }
            )
        }
        // 기타 버튼 토글
        SettingsToggleRow(
            label = "포인트 다이나믹스 버튼",
            checked = visibility.showDynamicsButton,
            onCheckedChange = { onVisibilityChange(visibility.copy(showDynamicsButton = it)) }
        )
        SettingsToggleRow(
            label = "모드 프리셋 버튼",
            checked = visibility.showModePresetButton,
            onCheckedChange = { onVisibilityChange(visibility.copy(showModePresetButton = it)) }
        )
        SettingsToggleRow(
            label = "스크롤 위/아래 버튼",
            checked = visibility.showScrollButtons,
            onCheckedChange = { onVisibilityChange(visibility.copy(showScrollButtons = it)) }
        )
    }
}

@Composable
private fun SettingsToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color(0xFFEEEEEE)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
