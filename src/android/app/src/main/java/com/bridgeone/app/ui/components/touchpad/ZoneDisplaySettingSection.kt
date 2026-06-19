package com.bridgeone.app.ui.components.touchpad

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bridgeone.app.ui.common.ColorCodec
import com.bridgeone.app.ui.common.swipe.LocalSwipeFocused
import com.bridgeone.app.ui.common.swipe.SwipeFocusable

/**
 * 선택 존의 표시 설정 섹션 (라벨 + 아이콘 + 컬러 + 되돌리기, Phase 4.7.5-D 추출).
 *
 * `SingleAction` 트리거의 표시 속성을 편집한다. 아이콘/컬러/라벨 박스는 각각 서랍/피커/키보드를
 * 여는 콜백을 호출하고, 사용자 지정 상태(라벨/아이콘/컬러 중 하나라도 수동 설정)일 때만 자동 복원
 * 버튼을 노출한다. SWIPE 포커스는 `SwipeFocusable`/[LocalSwipeFocused]가 CompositionLocal로 해석하므로
 * `inputMode` 파라미터는 불필요하다.
 *
 * @param sel 편집 대상 존 (`trigger`는 [EdgeZoneTrigger.SingleAction]이어야 함)
 * @param labelCursorAlpha 라벨 편집 중 커서 깜빡임 알파
 * @param showLabelKeyboard 라벨 키보드 활성 여부 (커서 표시 조건)
 */
@Composable
internal fun ZoneDisplaySettingSection(
    sel: EdgeZone,
    labelCursorAlpha: Float,
    showLabelKeyboard: Boolean,
    onRequestIconSheet: () -> Unit,
    onRequestColorPicker: () -> Unit,
    onRequestLabelKeyboard: () -> Unit,
    onIconBoxPositioned: (Offset) -> Unit,
    onColorBoxPositioned: (Offset) -> Unit,
    onRevertBoxPositioned: (Rect) -> Unit,
    onRevertToAuto: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Text("표시 설정", fontSize = 12.sp, color = cs.onSurfaceVariant)
    val trigger = sel.trigger as EdgeZoneTrigger.SingleAction
    val isAutoLabel = trigger.label.isEmpty()
    val isAutoIcon = trigger.iconKey.isEmpty()
    val displayLabel = trigger.label.ifEmpty { sel.action.defaultLabel() }
    val displayIconKey = trigger.iconKey.ifEmpty { sel.action.defaultIconKey() }
    val displayColorHex = trigger.colorHex
    val hasUserColor = displayColorHex.isNotEmpty()
    val hasUserCustom = !isAutoLabel || !isAutoIcon || hasUserColor
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 아이콘 박스
        SwipeFocusable(
            element = EdgeEditorElement.IconBox,
            shape = RoundedCornerShape(8.dp),
            showBorderHighlight = true,
            onActivate = { onRequestIconSheet() },
            gridRow = 36,
            modifier = Modifier.onGloballyPositioned { coords ->
                val b = coords.boundsInWindow()
                onIconBoxPositioned(
                    Offset(
                        (b.left + b.right) / 2f,
                        (b.top + b.bottom) / 2f,
                    )
                )
            },
        ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(cs.surfaceVariant)
                .border(0.5.dp, cs.outline.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                .clickable { onRequestIconSheet() },
            contentAlignment = Alignment.Center
        ) {
            if (displayIconKey.isNotEmpty()) {
                Icon(
                    imageVector = IconRegistry.get(displayIconKey),
                    contentDescription = null,
                    tint = if (isAutoIcon) cs.onSurface.copy(alpha = 0.6f) else cs.onSurface,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    tint = cs.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }
            // 자동 배지
            if (isAutoIcon && displayIconKey.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(3.dp)
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(cs.tertiaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text("A", fontSize = 7.sp, color = cs.onTertiaryContainer, fontWeight = FontWeight.Bold) // 기본값: 7.sp
                }
            }
        }
        } // SwipeFocusable(IconBox) 닫기

        // 컬러 박스
        SwipeFocusable(
            element = EdgeEditorElement.ColorBox,
            shape = RoundedCornerShape(8.dp),
            showBorderHighlight = true,
            onActivate = { onRequestColorPicker() },
            gridRow = 36,
            modifier = Modifier.onGloballyPositioned { coords ->
                val b = coords.boundsInWindow()
                onColorBoxPositioned(
                    Offset(
                        (b.left + b.right) / 2f,
                        (b.top + b.bottom) / 2f,
                    )
                )
            },
        ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(cs.surfaceVariant)
                .border(0.5.dp, cs.outline.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                .clickable { onRequestColorPicker() },
            contentAlignment = Alignment.Center
        ) {
            if (hasUserColor) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            ColorCodec.hexToColorOrNull(displayColorHex)
                                ?: cs.primary
                        )
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "색상 선택",
                    tint = cs.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        } // SwipeFocusable(ColorBox) 닫기

        // 라벨 박스
        SwipeFocusable(
            element = EdgeEditorElement.LabelBox,
            shape = RoundedCornerShape(8.dp),
            showBorderHighlight = true,
            onActivate = { onRequestLabelKeyboard() },
            gridRow = 36,
            modifier = Modifier.weight(1f),
        ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(cs.surfaceVariant)
                .border(0.5.dp, cs.outline.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                .clickable { onRequestLabelKeyboard() }
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 텍스트 + 커서 (남는 폭 차지, 텍스트만 말줄임)
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = displayLabel.ifEmpty { "라벨 입력..." },
                        fontSize = 14.sp,
                        color = if (displayLabel.isEmpty()) cs.onSurfaceVariant.copy(alpha = 0.5f)
                                else if (isAutoLabel) cs.onSurface.copy(alpha = 0.7f)
                                else cs.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    // 편집 중 커서
                    if (showLabelKeyboard) {
                        Box(
                            modifier = Modifier
                                .width(1.5.dp)
                                .height(16.dp)
                                .background(cs.primary.copy(alpha = labelCursorAlpha))
                        )
                    }
                }
                // 자동 배지 — 항상 오른쪽 고정
                if (isAutoLabel && displayLabel.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(cs.tertiaryContainer)
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text("자동", fontSize = 9.sp, color = cs.onTertiaryContainer) // 기본값: 9.sp
                    }
                }
            }
        }
        } // SwipeFocusable(LabelBox) 닫기

        // 자동으로 되돌리기 (사용자 지정 상태일 때만)
        if (hasUserCustom) {
            SwipeFocusable(
                element = EdgeEditorElement.RevertToAuto,
                shape = RoundedCornerShape(20.dp),
                onActivate = { onRevertToAuto() },
                gridRow = 36,
                modifier = Modifier.onGloballyPositioned { coords ->
                    onRevertBoxPositioned(coords.boundsInWindow())
                },
            ) {
                val revertFocused = LocalSwipeFocused.current
                IconButton(
                    onClick = { onRevertToAuto() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "자동으로 되돌리기",
                        tint = if (revertFocused) cs.primary else cs.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
