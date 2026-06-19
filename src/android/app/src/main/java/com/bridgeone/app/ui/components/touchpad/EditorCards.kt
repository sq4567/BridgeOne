package com.bridgeone.app.ui.components.touchpad

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bridgeone.app.ui.common.AppIcon
import com.bridgeone.app.ui.common.ColorCodec
import com.bridgeone.app.ui.common.customPresetIconOrNull

// ─────────────────────────────────────────────────────────────
// 메타/커브/헤더 카드 (Phase 4.7.6-B: DynamicsCurveEditor.kt에서 분리)
// ─────────────────────────────────────────────────────────────

/** 메타 카드: 아이콘(0) / 색상(10) / 이름(1)+설명(2) / 템플릿(3) — 양 끝이 전체 높이 차지 */
@Composable
internal fun MetaCard(
    slots: List<ActionSlot>,
    hoveredSlot: Int,
    awaitingConfirm: Boolean,
    selectedIconKey: String,
    selectedColorHex: String = "",
    name: String,
    description: String,
    editingTarget: String? = null,
    onIconSlotPositioned: (Offset) -> Unit = {},
    onColorSlotPositioned: (Offset) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val caretAlpha by rememberInfiniteTransition(label = "metaCaret").animateFloat(
        initialValue = 1f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1000
                1f at 0
                1f at 499
                0f at 500
                0f at 999
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "caretAlpha"
    )
    val cardShape = RoundedCornerShape(10.dp)
    val cellShape = RoundedCornerShape(7.dp)
    val dividerColor = Color.White.copy(alpha = 0.09f)
    Row(
        modifier = modifier
            .height(56.dp)
            .background(SURFACE, cardShape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // 슬롯 0: 아이콘 (좌측, 전체 높이 정사각형)
        val hovIcon = hoveredSlot == 0
        val awIcon = awaitingConfirm && hovIcon
        val enIcon = slots.getOrNull(0)?.enabled ?: true
        Box(
            modifier = Modifier
                .aspectRatio(1f).fillMaxHeight()
                .onGloballyPositioned { coords ->
                    val b = coords.boundsInWindow()
                    onIconSlotPositioned(Offset((b.left + b.right) / 2f, (b.top + b.bottom) / 2f))
                }
                .background(cellBgColor(hovIcon, awIcon, enIcon), cellShape)
                .cellBorder(hovIcon, awIcon, enIcon, shape = cellShape),
            contentAlignment = Alignment.Center
        ) {
            val iconDef = customPresetIconOrNull(selectedIconKey)
            if (iconDef != null) {
                AppIcon(
                    def = iconDef, contentDescription = null,
                    tint = if (hovIcon || awIcon) ACCENT_BLUE else Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(26.dp)
                )
            } else {
                Text("+", color = LABEL_COLOR, fontSize = 36.sp, fontWeight = FontWeight.Light)
            }
        }

        Box(Modifier.width(1.dp).fillMaxHeight().background(dividerColor))

        // 슬롯 10: 색상
        val hovColor = hoveredSlot == 10
        val awColor = awaitingConfirm && hovColor
        val enColor = slots.getOrNull(10)?.enabled ?: true
        val parsedSwatchColor = ColorCodec.hexToColorOrNull(selectedColorHex)
        Box(
            modifier = Modifier
                .width(44.dp).fillMaxHeight()
                .onGloballyPositioned { coords ->
                    val b = coords.boundsInWindow()
                    onColorSlotPositioned(Offset((b.left + b.right) / 2f, (b.top + b.bottom) / 2f))
                }
                .background(cellBgColor(hovColor, awColor, enColor), cellShape)
                .cellBorder(hovColor, awColor, enColor, shape = cellShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(parsedSwatchColor ?: Color.White.copy(alpha = 0.08f))
                    .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
            )
        }

        Box(Modifier.width(1.dp).fillMaxHeight().background(dividerColor))

        // 중앙: 이름(1, 상단) + 설명(2, 하단)
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // 슬롯 1: 이름
            val hovName = hoveredSlot == 1
            val awName = awaitingConfirm && hovName
            val enName = slots.getOrNull(1)?.enabled ?: true
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth()
                    .background(cellBgColor(hovName, awName, enName), cellShape)
                    .cellBorder(hovName, awName, enName, shape = cellShape)
                    .padding(horizontal = 7.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val isEditingName = editingTarget == FIELD_NAME
                    Text(
                        text = if (isEditingName) name else name.ifBlank { "이름 없음" },
                        color = when {
                            (hovName || awName) && enName -> Color.White
                            name.isBlank() && !isEditingName -> LABEL_COLOR
                            else                          -> Color.White.copy(alpha = 0.85f)
                        },
                        fontSize = 12.sp,
                        fontWeight = if ((hovName || awName) && enName) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isEditingName) {
                        Text("|", color = Color.White.copy(alpha = caretAlpha), fontSize = 12.sp)
                    }
                }
            }

            Box(Modifier.fillMaxWidth().height(1.dp).background(dividerColor))

            // 슬롯 2: 설명
            val hovDesc = hoveredSlot == 2
            val awDesc = awaitingConfirm && hovDesc
            val enDesc = slots.getOrNull(2)?.enabled ?: true
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth()
                    .background(cellBgColor(hovDesc, awDesc, enDesc), cellShape)
                    .cellBorder(hovDesc, awDesc, enDesc, shape = cellShape)
                    .padding(horizontal = 7.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val isEditingDesc = editingTarget == FIELD_DESC
                    Text(
                        text = if (isEditingDesc) description
                               else if (description.isBlank()) "설명 추가하기..." else description,
                        color = when {
                            (hovDesc || awDesc) && enDesc -> Color.White
                            description.isBlank() && !isEditingDesc -> LABEL_COLOR.copy(alpha = 0.6f)
                            else                          -> Color.White.copy(alpha = 0.55f)
                        },
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isEditingDesc) {
                        Text("|", color = Color.White.copy(alpha = caretAlpha), fontSize = 11.sp)
                    }
                }
            }
        }

        Box(Modifier.width(1.dp).fillMaxHeight().background(dividerColor))

        // 슬롯 3: 템플릿 (우측, 전체 높이)
        val hovTmpl = hoveredSlot == 3
        val awTmpl = awaitingConfirm && hovTmpl
        val enTmpl = slots.getOrNull(3)?.enabled ?: true
        val tmplColor = when {
            (hovTmpl || awTmpl) && enTmpl -> ACCENT_BLUE
            else                           -> LABEL_COLOR
        }
        Box(
            modifier = Modifier.width(52.dp).fillMaxHeight()
                .background(cellBgColor(hovTmpl, awTmpl, enTmpl), cellShape)
                .cellBorder(hovTmpl, awTmpl, enTmpl, shape = cellShape),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("▤", color = tmplColor, fontSize = 18.sp)
                Text("템플릿", color = tmplColor, fontSize = 9.sp)
            }
        }
    }
}

/** 커브 카드: [가속(4)] [→복사(6)] [감속(5)] — 단일 행 세그먼트 */
@Composable
internal fun CurveCard(
    slots: List<ActionSlot>,
    hoveredSlot: Int,
    awaitingConfirm: Boolean,
    activeTab: Int,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(10.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp)
            .background(SURFACE, cardShape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // 슬롯 4: 가속 (segment left)
        val hovAccel = hoveredSlot == 4
        val awAccel = awaitingConfirm && hovAccel
        val enAccel = slots.getOrNull(4)?.enabled ?: true
        val accelCurrent = activeTab == 0
        val accelShape = RoundedCornerShape(topStart = 7.dp, bottomStart = 7.dp)
        val accelBg = when {
            awAccel && enAccel  -> ACCENT_BLUE.copy(alpha = 0.55f)
            awAccel             -> Color.White.copy(alpha = 0.12f)
            hovAccel && enAccel -> ACCENT_BLUE.copy(alpha = 0.38f)
            hovAccel            -> Color.White.copy(alpha = 0.06f)
            accelCurrent        -> ACCENT_BLUE.copy(alpha = 0.20f)
            else                -> Color.Transparent
        }
        val accelBorder: Modifier = when {
            awAccel && enAccel  -> Modifier.border(2.dp, Color.White.copy(alpha = 0.9f), accelShape)
            hovAccel && enAccel -> Modifier.border(1.5.dp, ACCENT_BLUE, accelShape)
            accelCurrent        -> Modifier.border(1.dp, ACCENT_BLUE, accelShape)
            else                -> Modifier
        }
        Box(
            modifier = Modifier.weight(1f).fillMaxHeight()
                .background(accelBg, accelShape).then(accelBorder),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "가속",
                color = when {
                    (hovAccel || awAccel) && enAccel -> Color.White
                    accelCurrent                     -> ACCENT_BLUE
                    enAccel                          -> Color.White.copy(alpha = 0.7f)
                    else                             -> Color.White.copy(alpha = 0.3f)
                },
                fontSize = 12.sp,
                fontWeight = if (accelCurrent || (hovAccel && enAccel) || awAccel) FontWeight.Bold else FontWeight.Normal
            )
        }

        Box(Modifier.width(1.dp).fillMaxHeight().background(Color.White.copy(alpha = 0.09f)))

        // 슬롯 6: →복사 (segment middle, 좁은 폭)
        val hovCopy = hoveredSlot == 6
        val awCopy = awaitingConfirm && hovCopy
        val enCopy = slots.getOrNull(6)?.enabled ?: false
        val copyColor = when {
            (hovCopy || awCopy) && enCopy -> Color.White
            enCopy                        -> Color.White.copy(alpha = 0.55f)
            else                          -> Color.White.copy(alpha = 0.18f)
        }
        val copyShape = RoundedCornerShape(0.dp)
        val copyBg = when {
            awCopy && enCopy  -> ACCENT_BLUE.copy(alpha = 0.40f)
            awCopy            -> Color.White.copy(alpha = 0.08f)
            hovCopy && enCopy -> ACCENT_BLUE.copy(alpha = 0.22f)
            hovCopy           -> Color.White.copy(alpha = 0.04f)
            else              -> Color.Transparent
        }
        val copyBorder: Modifier = when {
            awCopy && enCopy  -> Modifier.border(2.dp, Color.White.copy(alpha = 0.9f), copyShape)
            hovCopy && enCopy -> Modifier.border(1.5.dp, ACCENT_BLUE, copyShape)
            else              -> Modifier
        }
        Box(
            modifier = Modifier.width(44.dp).fillMaxHeight()
                .background(copyBg, copyShape).then(copyBorder),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = null,
                    tint = copyColor,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "→",
                    color = copyColor,
                    fontSize = 10.sp
                )
            }
        }

        Box(Modifier.width(1.dp).fillMaxHeight().background(Color.White.copy(alpha = 0.09f)))

        // 슬롯 5: 감속 (segment right)
        val hovDecel = hoveredSlot == 5
        val awDecel = awaitingConfirm && hovDecel
        val enDecel = slots.getOrNull(5)?.enabled ?: true
        val decelCurrent = activeTab == 1
        val decelShape = RoundedCornerShape(topEnd = 7.dp, bottomEnd = 7.dp)
        val decelBg = when {
            awDecel && enDecel  -> ACCENT_ORANGE.copy(alpha = 0.55f)
            awDecel             -> Color.White.copy(alpha = 0.12f)
            hovDecel && enDecel -> ACCENT_ORANGE.copy(alpha = 0.38f)
            hovDecel            -> Color.White.copy(alpha = 0.06f)
            decelCurrent        -> ACCENT_ORANGE.copy(alpha = 0.20f)
            else                -> Color.Transparent
        }
        val decelBorder: Modifier = when {
            awDecel && enDecel  -> Modifier.border(2.dp, Color.White.copy(alpha = 0.9f), decelShape)
            hovDecel && enDecel -> Modifier.border(1.5.dp, ACCENT_ORANGE, decelShape)
            decelCurrent        -> Modifier.border(1.dp, ACCENT_ORANGE, decelShape)
            else                -> Modifier
        }
        Box(
            modifier = Modifier.weight(1f).fillMaxHeight()
                .background(decelBg, decelShape).then(decelBorder),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "감속",
                color = when {
                    (hovDecel || awDecel) && enDecel -> Color.White
                    decelCurrent                     -> ACCENT_ORANGE
                    enDecel                          -> Color.White.copy(alpha = 0.7f)
                    else                             -> Color.White.copy(alpha = 0.3f)
                },
                fontSize = 12.sp,
                fontWeight = if (decelCurrent || (hovDecel && enDecel) || awDecel) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

/** 편집기 상단 헤더: 뒤로 가기(취소) / 저장 */
@Composable
internal fun EditorHeader(
    nameValid: Boolean,
    hoveredSlot: Int,
    awaitingConfirm: Boolean,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 슬롯 7: 취소 (뒤로 가기)
        val hovCancel = hoveredSlot == 7
        val awCancel = awaitingConfirm && hovCancel
        val btnShape = RoundedCornerShape(8.dp)
        val cancelBg = when {
            awCancel  -> Color.White.copy(alpha = 0.18f)
            hovCancel -> Color.White.copy(alpha = 0.10f)
            else      -> Color.Transparent
        }
        val cancelBorderMod: Modifier = when {
            awCancel  -> Modifier.border(2.dp, Color.White.copy(alpha = 0.9f), btnShape)
            hovCancel -> Modifier.border(1.5.dp, Color.White.copy(alpha = 0.6f), btnShape)
            else      -> Modifier.border(1.dp, Color.White.copy(alpha = 0.25f), btnShape)
        }
        Box(
            modifier = Modifier
                .size(width = 60.dp, height = 36.dp)
                .clip(btnShape)
                .background(cancelBg, btnShape)
                .then(cancelBorderMod)
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = if (hovCancel) Color.White else Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(18.dp)
            )
        }

        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "커스텀 프리셋",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // 슬롯 8: 저장
        val hovSave = hoveredSlot == 8
        val awSave = awaitingConfirm && hovSave
        val saveBg = when {
            awSave && nameValid  -> ACCENT_BLUE
            hovSave && nameValid -> ACCENT_BLUE.copy(alpha = 0.85f)
            nameValid            -> ACCENT_BLUE.copy(alpha = 0.55f)
            else                 -> SURFACE
        }
        val saveBorderMod: Modifier = when {
            awSave && nameValid -> Modifier.border(2.dp, Color.White.copy(alpha = 0.9f), btnShape)
            !nameValid          -> Modifier.border(1.dp, Color.White.copy(alpha = 0.12f), btnShape)
            else                -> Modifier
        }
        Box(
            modifier = Modifier
                .size(width = 60.dp, height = 36.dp)
                .clip(btnShape)
                .background(saveBg, btnShape)
                .then(saveBorderMod)
                .clickable(enabled = nameValid, onClick = onSave),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "저장",
                color = if (nameValid) Color.White else Color.White.copy(alpha = 0.3f),
                fontSize = 14.sp,
                fontWeight = if (nameValid) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
