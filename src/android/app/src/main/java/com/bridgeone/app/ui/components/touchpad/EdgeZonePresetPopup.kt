package com.bridgeone.app.ui.components.touchpad

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.aspectRatio
import com.bridgeone.app.ui.common.EdgeSwipeConstants
import com.bridgeone.app.ui.common.EdgeZonePreset
import com.bridgeone.app.ui.common.EdgeZonePresetsRepository
import com.bridgeone.app.ui.common.InputMode
import com.bridgeone.app.ui.common.stripActions
import com.bridgeone.app.ui.common.swipe.LocalSwipeFocused
import com.bridgeone.app.ui.common.swipe.SwipeFocusable

// private 제거: EdgeZoneEditorScreen에서 stage 호이스팅 위해 패키지 공개
enum class PopupStage { GRID, CONFIRM, SAVE_NAME, EDIT_NAME }

/**
 * 엣지 존 프리셋 선택 팝업 (Phase 4.6.3)
 *
 * DynamicsPresetPopup과 동일한 GRID→CONFIRM 2단계 패턴.
 * SWIPE 모드에서 완전 조작 지원:
 * - 스와이프: 항목 이동
 * - 탭: 선택 확정
 * - 롱프레스: onLongPress(EdgeZoneEditorScreen)에서 단계별 뒤로가기/닫기 처리
 *
 * 스크림 처리:
 * - NORMAL: 스크림 탭으로 닫기 (clickable 활성)
 * - SWIPE: 스크림 clickable 제거 → 터치가 SwipeGestureLayer로 통과
 *
 * @param inputMode           현재 입력 모드
 * @param stage               현재 단계 (호이스팅)
 * @param onStageChange       단계 변경 콜백 (호이스팅)
 * @param currentPresetId     현재 적용 중인 프리셋 ID (강조 표시)
 * @param currentConfig       현재 WorkConfig (프리셋 적용 시 덮어쓰기 확인에 사용)
 * @param presetsRepo         커스텀 저장소
 * @param onApply             프리셋 적용 확정 시 콜백
 * @param onDismiss           팝업 닫기
 * @param onRequestTextKeyboard SWIPE 모드 텍스트 입력 요청 (initial, onResult)
 */
@Composable
fun EdgeZonePresetPopup(
    inputMode: InputMode,
    stage: PopupStage,
    onStageChange: (PopupStage) -> Unit,
    currentPresetId: String?,
    currentConfig: EdgeZoneConfig,
    presetsRepo: EdgeZonePresetsRepository,
    onApply: (EdgeZonePreset) -> Unit,
    onDismiss: () -> Unit,
    onRequestTextKeyboard: (initial: String, onResult: (String) -> Unit) -> Unit = { _, _ -> },
) {
    val cs = MaterialTheme.colorScheme
    val isSwipe = inputMode == InputMode.SWIPE

    var allPresets by remember { mutableStateOf(presetsRepo.loadAll()) }
    var selected by remember { mutableStateOf<EdgeZonePreset?>(null) }
    var saveNameInput by remember { mutableStateOf("") }
    var editNameInput by remember { mutableStateOf("") }

    fun refresh() { allPresets = presetsRepo.loadAll() }

    // ── 풀스크린 스크림 ──
    // NORMAL: 탭으로 닫기 / SWIPE: clickable 없어서 터치가 SwipeGestureLayer로 통과
    val scrimModifier = if (isSwipe) {
        Modifier.fillMaxSize().background(Color(0xCC000000))
    } else {
        Modifier.fillMaxSize().background(Color(0xCC000000)).clickable { onDismiss() }
    }

    Box(modifier = scrimModifier) {
        // ── 조작법 안내 메시지 (카드 바깥, 중앙 하단) ──
        // 카드가 세로 중앙 정렬이므로 안내는 카드 아래 고정 여백으로 배치
        if (isSwipe) {
            val hintText = when (stage) {
                PopupStage.GRID -> "스와이프: 이동  ·  탭: 선택  ·  길게: 닫기"
                else -> "스와이프: 이동  ·  탭: 선택  ·  길게: 뒤로"
            }
            Text(
                text = hintText,
                fontSize = 11.sp,
                color = cs.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp),
            )
        }
        // ── 팝업 카드 ──
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .align(Alignment.Center),
            shape = RoundedCornerShape(16.dp),
            color = cs.surfaceContainerHigh,
            shadowElevation = 12.dp,
            tonalElevation = 2.dp,
        ) {
            Column(
                modifier = Modifier
                    .clickable(enabled = false) {}
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                // ── stage 전환 AnimatedContent ──
                AnimatedContent(
                    targetState = stage,
                    transitionSpec = {
                        (slideInHorizontally { it / 4 } + fadeIn(tween(180))) togetherWith
                        (slideOutHorizontally { -it / 4 } + fadeOut(tween(120)))
                    },
                    label = "presetStage",
                ) { currentStage ->
                    when (currentStage) {

                        // ────────────────────────────────────────
                        // GRID 단계: 프리셋 목록
                        // ────────────────────────────────────────
                        PopupStage.GRID -> {
                            Column {
                                Column(
                                    modifier = Modifier.verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // 3열 그리드
                                    allPresets.chunked(3).forEachIndexed { chunkIdx, rowItems ->
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            rowItems.forEach { preset ->
                                                val isActive = preset.id == currentPresetId
                                                val isCustom = !preset.id.startsWith("builtin_")
                                                val cellAction: () -> Unit = { selected = preset; onStageChange(PopupStage.CONFIRM) }
                                                SwipeFocusable(
                                                    element = EdgeEditorElement.PresetItem(preset.id),
                                                    scope = EdgeEditorScope.PresetPopup,
                                                    shape = RoundedCornerShape(10.dp),
                                                    onActivate = cellAction,
                                                    gridRow = chunkIdx,
                                                    modifier = Modifier.weight(1f),
                                                ) {
                                                PresetCell(
                                                    preset = preset,
                                                    isActive = isActive,
                                                    isCustom = isCustom,
                                                    onClick = cellAction,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                                }
                                            }
                                            // 빈 셀 채우기
                                            repeat(3 - rowItems.size) {
                                                Spacer(Modifier.weight(1f))
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(4.dp))

                                    // 현재 설정을 새 프리셋으로 저장
                                    val saveNewAction: () -> Unit = {
                                        if (isSwipe) {
                                            onRequestTextKeyboard("") { result ->
                                                if (result.isNotBlank()) {
                                                    presetsRepo.add(EdgeZonePreset(
                                                        id = "", name = result.trim(),
                                                        description = "", iconKey = "Tune",
                                                        config = currentConfig.stripActions()
                                                    ))
                                                    refresh()
                                                }
                                            }
                                        } else {
                                            saveNameInput = ""
                                            onStageChange(PopupStage.SAVE_NAME)
                                        }
                                    }
                                    SwipeFocusable(
                                        element = EdgeEditorElement.PresetSaveNew,
                                        scope = EdgeEditorScope.PresetPopup,
                                        shape = RoundedCornerShape(8.dp),
                                        onActivate = saveNewAction,
                                        gridRow = ((allPresets.size + 2) / 3),
                                    ) {
                                    val saveNewFocused = LocalSwipeFocused.current
                                    val saveNewBg by animateColorAsState(
                                        if (saveNewFocused) cs.primary else cs.surfaceVariant,
                                        tween(50), label = "saveNewBg"
                                    )
                                    val saveNewContent by animateColorAsState(
                                        if (saveNewFocused) cs.onPrimary else cs.onSurface,
                                        tween(50), label = "saveNewContent"
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(saveNewBg)
                                            .clickable(onClick = saveNewAction)
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Filled.Add, contentDescription = null, tint = saveNewContent, modifier = Modifier.size(18.dp))
                                        Text("현재 설정을 새 프리셋으로 저장…", fontSize = 13.sp, color = saveNewContent)
                                    }
                                    }
                                }

                            }
                        }

                        // ────────────────────────────────────────
                        // CONFIRM 단계: 선택된 프리셋 확인
                        // ────────────────────────────────────────
                        PopupStage.CONFIRM -> {
                            val preset = selected
                            if (preset != null) {
                                val isCustom = !preset.id.startsWith("builtin_")
                                val confirmBackAction: () -> Unit = { onStageChange(PopupStage.GRID) }

                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    // 큰 터치패드 미리보기 + 이름/설명
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(cs.surfaceVariant)
                                            .padding(vertical = 12.dp, horizontal = 12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(EdgeSwipeConstants.PRESET_PREVIEW_ASPECT_RATIO)
                                                .clip(RoundedCornerShape(6.dp))
                                        ) {
                                            EdgeZoneEditorPreviewCanvas(
                                                config = preset.config,
                                                bottomLeftButtonLabel = null,
                                                bottomRightButtonLabel = null,
                                                structureOnly = true,
                                                interactive = false,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                        Text(preset.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = cs.onSurface)
                                        if (preset.description.isNotEmpty()) {
                                            Text(preset.description, fontSize = 13.sp, color = cs.onSurfaceVariant, maxLines = 2)
                                        }
                                    }

                                    // 액션 버튼
                                    val applyConfirmAction: () -> Unit = { onApply(preset) }
                                    val renameAction: () -> Unit = {
                                        if (isSwipe) {
                                            onRequestTextKeyboard(preset.name) { result ->
                                                if (result.isNotBlank()) {
                                                    val updated = preset.copy(name = result.trim())
                                                    presetsRepo.update(updated)
                                                    selected = updated
                                                    refresh()
                                                }
                                            }
                                        } else {
                                            editNameInput = preset.name
                                            onStageChange(PopupStage.EDIT_NAME)
                                        }
                                    }
                                    val deleteAction: () -> Unit = {
                                        presetsRepo.delete(preset.id)
                                        refresh()
                                        onStageChange(PopupStage.GRID)
                                    }
                                    val confirmCancelAction: () -> Unit = { onStageChange(PopupStage.GRID) }
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        // 취소 / 적용 가로 배치
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            SwipeFocusable(
                                                element = EdgeEditorElement.PresetConfirmCancel,
                                                scope = EdgeEditorScope.PresetPopup,
                                                shape = RoundedCornerShape(8.dp),
                                                onActivate = confirmCancelAction,
                                                gridRow = 1,
                                                modifier = Modifier.weight(1f),
                                            ) {
                                            val cancelFocused = LocalSwipeFocused.current
                                            val cancelBg by animateColorAsState(
                                                if (cancelFocused) cs.error else cs.surfaceVariant,
                                                tween(50), label = "cancelBg"
                                            )
                                            val cancelContent by animateColorAsState(
                                                if (cancelFocused) cs.onError else cs.onSurfaceVariant,
                                                tween(50), label = "cancelContent"
                                            )
                                            Button(
                                                onClick = confirmCancelAction,
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = cancelBg,
                                                    contentColor = cancelContent
                                                )
                                            ) { Text("취소") }
                                            }
                                            SwipeFocusable(
                                                element = EdgeEditorElement.PresetConfirmApply,
                                                scope = EdgeEditorScope.PresetPopup,
                                                shape = RoundedCornerShape(8.dp),
                                                onActivate = applyConfirmAction,
                                                gridRow = 1,
                                                modifier = Modifier.weight(1f),
                                            ) {
                                            val applyFocused = LocalSwipeFocused.current
                                            val applyBg by animateColorAsState(
                                                if (applyFocused) cs.secondary else cs.surfaceVariant,
                                                tween(50), label = "applyBg"
                                            )
                                            val applyContent by animateColorAsState(
                                                if (applyFocused) cs.onSecondary else cs.onSurface,
                                                tween(50), label = "applyContent"
                                            )
                                            Button(
                                                onClick = applyConfirmAction,
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = applyBg,
                                                    contentColor = applyContent
                                                )
                                            ) {
                                                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text("적용")
                                            }
                                            }
                                        }
                                        if (isCustom) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                SwipeFocusable(
                                                    element = EdgeEditorElement.PresetConfirmRename,
                                                    scope = EdgeEditorScope.PresetPopup,
                                                    shape = RoundedCornerShape(8.dp),
                                                    showBorderHighlight = true,
                                                    onActivate = renameAction,
                                                    gridRow = 2,
                                                    modifier = Modifier.weight(1f),
                                                ) {
                                                Button(
                                                    onClick = renameAction,
                                                    modifier = Modifier.fillMaxWidth(),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = cs.surfaceVariant,
                                                        contentColor = cs.onSurface
                                                    )
                                                ) {
                                                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(Modifier.width(4.dp))
                                                    Text("이름 변경", fontSize = 12.sp)
                                                }
                                                }
                                                SwipeFocusable(
                                                    element = EdgeEditorElement.PresetConfirmDelete,
                                                    scope = EdgeEditorScope.PresetPopup,
                                                    shape = RoundedCornerShape(8.dp),
                                                    showBorderHighlight = true,
                                                    onActivate = deleteAction,
                                                    gridRow = 2,
                                                    modifier = Modifier.weight(1f),
                                                ) {
                                                Button(
                                                    onClick = deleteAction,
                                                    modifier = Modifier.fillMaxWidth(),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = cs.error.copy(alpha = 0.15f),
                                                        contentColor = cs.error
                                                    )
                                                ) {
                                                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(Modifier.width(4.dp))
                                                    Text("삭제", fontSize = 12.sp)
                                                }
                                                }
                                            }
                                        }
                                    }

                                }
                            }
                        }

                        // ────────────────────────────────────────
                        // SAVE_NAME: 현재 설정을 새 프리셋으로 저장 (NORMAL 모드 전용)
                        // SWIPE에서는 GRID의 saveNewAction에서 직접 키보드 호출
                        // ────────────────────────────────────────
                        PopupStage.SAVE_NAME -> {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("새 프리셋 이름", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = cs.onSurface)
                                OutlinedTextField(
                                    value = saveNameInput,
                                    onValueChange = { saveNameInput = it },
                                    label = { Text("이름") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = cs.onSurface,
                                        unfocusedTextColor = cs.onSurface,
                                        focusedBorderColor = cs.primary,
                                        unfocusedBorderColor = cs.outline
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                val saveConfirmAction: () -> Unit = {
                                    if (saveNameInput.isNotBlank()) {
                                        presetsRepo.add(EdgeZonePreset(
                                            id = "", name = saveNameInput.trim(),
                                            description = "", iconKey = "Tune",
                                            config = currentConfig.stripActions()
                                        ))
                                        refresh()
                                        onStageChange(PopupStage.GRID)
                                    }
                                }
                                val saveCancelAction: () -> Unit = { onStageChange(PopupStage.GRID) }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    SwipeFocusable(
                                        element = EdgeEditorElement.PresetSaveConfirm,
                                        scope = EdgeEditorScope.PresetPopup,
                                        shape = RoundedCornerShape(8.dp),
                                        showBorderHighlight = true,
                                        onActivate = saveConfirmAction,
                                        gridRow = 0,
                                        modifier = Modifier.weight(1f),
                                    ) {
                                    Button(
                                        onClick = saveConfirmAction,
                                        enabled = saveNameInput.isNotBlank(),
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = cs.primary)
                                    ) { Text("저장") }
                                    }
                                    SwipeFocusable(
                                        element = EdgeEditorElement.PresetSaveCancel,
                                        scope = EdgeEditorScope.PresetPopup,
                                        shape = RoundedCornerShape(4.dp),
                                        showBorderHighlight = true,
                                        onActivate = saveCancelAction,
                                        gridRow = 0,
                                        modifier = Modifier.weight(1f),
                                    ) {
                                    TextButton(onClick = saveCancelAction, modifier = Modifier.fillMaxWidth()) {
                                        Text("취소", color = cs.onSurfaceVariant)
                                    }
                                    }
                                }

                            }
                        }

                        // ────────────────────────────────────────
                        // EDIT_NAME: 커스텀 프리셋 이름 변경 (NORMAL 모드 전용)
                        // SWIPE에서는 CONFIRM의 renameAction에서 직접 키보드 호출
                        // ────────────────────────────────────────
                        PopupStage.EDIT_NAME -> {
                            val preset = selected
                            if (preset != null) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("이름 변경", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = cs.onSurface)
                                    OutlinedTextField(
                                        value = editNameInput,
                                        onValueChange = { editNameInput = it },
                                        label = { Text("새 이름") },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = cs.onSurface,
                                            unfocusedTextColor = cs.onSurface,
                                            focusedBorderColor = cs.primary,
                                            unfocusedBorderColor = cs.outline
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    val editConfirmAction: () -> Unit = {
                                        if (editNameInput.isNotBlank()) {
                                            val updated = preset.copy(name = editNameInput.trim())
                                            presetsRepo.update(updated)
                                            selected = updated
                                            refresh()
                                            onStageChange(PopupStage.CONFIRM)
                                        }
                                    }
                                    val editCancelAction: () -> Unit = { onStageChange(PopupStage.CONFIRM) }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        SwipeFocusable(
                                            element = EdgeEditorElement.PresetEditConfirm,
                                            scope = EdgeEditorScope.PresetPopup,
                                            shape = RoundedCornerShape(8.dp),
                                            showBorderHighlight = true,
                                            onActivate = editConfirmAction,
                                            gridRow = 0,
                                            modifier = Modifier.weight(1f),
                                        ) {
                                        Button(
                                            onClick = editConfirmAction,
                                            enabled = editNameInput.isNotBlank(),
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = cs.primary)
                                        ) { Text("변경") }
                                        }
                                        SwipeFocusable(
                                            element = EdgeEditorElement.PresetEditCancel,
                                            scope = EdgeEditorScope.PresetPopup,
                                            shape = RoundedCornerShape(4.dp),
                                            showBorderHighlight = true,
                                            onActivate = editCancelAction,
                                            gridRow = 0,
                                            modifier = Modifier.weight(1f),
                                        ) {
                                        TextButton(onClick = editCancelAction, modifier = Modifier.fillMaxWidth()) {
                                            Text("취소", color = cs.onSurfaceVariant)
                                        }
                                        }
                                    }

                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetCell(
    preset: EdgeZonePreset,
    isActive: Boolean,
    isCustom: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val focused = LocalSwipeFocused.current

    // 선택(isActive) = secondary(녹색), 포커스(focused) = primary(파랑), 기본 = 회색
    val borderColor by animateColorAsState(
        targetValue = when {
            isActive -> cs.secondary
            else -> cs.outline.copy(alpha = 0.4f)
        },
        animationSpec = tween(durationMillis = 50),
        label = "presetCellBorder",
    )
    val bgColor by animateColorAsState(
        targetValue = when {
            focused -> cs.primary
            isActive -> cs.secondary.copy(alpha = 0.15f)
            else -> cs.surfaceVariant
        },
        animationSpec = tween(durationMillis = 50),
        label = "presetCellBg",
    )
    val contentColor by animateColorAsState(
        targetValue = when {
            isActive -> cs.secondary
            else -> cs.onSurface
        },
        animationSpec = tween(durationMillis = 50),
        label = "presetCellContent",
    )
    val textColor by animateColorAsState(
        targetValue = when {
            isActive -> cs.secondary
            else -> cs.onSurface
        },
        animationSpec = tween(durationMillis = 50),
        label = "presetCellText",
    )
    val borderWidth = if (isActive) 2.dp else 0.5.dp

    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(
                width = borderWidth,
                color = borderColor,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = preset.name,
            fontSize = 13.sp,
            color = textColor,
            maxLines = 1,
            modifier = Modifier.align(Alignment.Center)
        )
        if (isCustom) {
            Text(
                text = "커스텀",
                fontSize = 9.sp,
                color = cs.onSurfaceVariant,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
