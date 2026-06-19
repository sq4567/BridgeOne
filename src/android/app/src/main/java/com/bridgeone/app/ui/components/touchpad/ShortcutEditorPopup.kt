package com.bridgeone.app.ui.components.touchpad

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bridgeone.app.ui.common.EdgeSwipeConstants
import com.bridgeone.app.ui.common.InputMode
import com.bridgeone.app.ui.common.ToastController
import com.bridgeone.app.ui.common.ToastType
import com.bridgeone.app.ui.common.loadLastShortcutSingleKeyMode
import com.bridgeone.app.ui.common.saveLastShortcutSingleKeyMode
import com.bridgeone.app.ui.common.swipe.LocalSwipeFocusController
import com.bridgeone.app.ui.common.swipe.LocalSwipeFocused
import com.bridgeone.app.ui.common.swipe.SwipeFocusable
import com.bridgeone.app.ui.common.swipe.SwipeGestureLayer
import com.bridgeone.app.ui.components.SwipeKeyboardOverlay
import com.bridgeone.app.ui.common.MOD_BIT_LCTRL
import com.bridgeone.app.ui.common.MOD_BIT_LSHIFT
import com.bridgeone.app.ui.common.MOD_BIT_LALT
import com.bridgeone.app.ui.common.MOD_BIT_LGUI
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput

/**
 * 단축키(SendShortcut) 편집 팝업. EdgeZoneEditorScreen에서 분리 (Phase 4.7.5-B).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ShortcutEditorPopup(
    draft: EdgeZoneAction.SendShortcut,
    inputMode: InputMode,
    onDraftChange: (EdgeZoneAction.SendShortcut) -> Unit,
    onCancel: () -> Unit,
    onConfirm: (EdgeZoneAction.SendShortcut) -> Unit,
    onAddAsCandidate: ((iconKey: String, name: String) -> Unit)?,
    onNameKeyboardActiveChange: (Boolean) -> Unit = {},
    onRequestIconPicker: ((current: String, anchorCenter: androidx.compose.ui.geometry.Offset, onResult: (String) -> Unit) -> Unit)? = null,
) {
    val cs = MaterialTheme.colorScheme
    val swipeController = LocalSwipeFocusController.current

    var draftIconKey by remember { mutableStateOf("") }
    // 사용자가 직접 입력한 액션명만 보유 (빈 값이면 키 조합 미리보기를 placeholder로 표시). 기본값: ""
    var draftName by remember { mutableStateOf("") }
    // 현재 선택된 키 조합의 미리보기 문자열 (예: "H", "Ctrl+Shift+H"). 키가 없으면 "".
    // MutableState로 선언해야 SwipeFocusable onActivate 람다에서 호출 시점 값을 읽음 (plain val은 stale capture됨).
    var previewName by remember { mutableStateOf("") }
    previewName = if (draft.modifierBits == 0 && draft.keyCodes.isEmpty()) ""
        else formatShortcutCombo(draft.modifierBits, draft.keyCodes)
    // 액션명 입력 키보드 활성 여부 (SWIPE 전용). true 시 팝업 카드 바로 아래에 키보드가 확장됨. 기본값: false
    var nameKeyboardActive by remember { mutableStateOf(false) }
    LaunchedEffect(nameKeyboardActive) { onNameKeyboardActiveChange(nameKeyboardActive) }
    var normalIconSheetVisible by remember { mutableStateOf(false) }
    val normalIconSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var iconBtnCenter by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    val context = LocalContext.current
    // 단일 키 / 단축키 모드 (팝업 내부 토글로 전환).
    // draft가 실제 combo(모디파이어 있음 또는 키 2개+)이면 강제로 단축키 모드,
    // 그 외(빈 draft·단일 키)는 마지막 사용 모드 복원. 기본값: false (단축키 모드)
    var singleKeyMode by remember {
        val isComboContent = draft.modifierBits != 0 || draft.keyCodes.size > 1
        mutableStateOf(if (isComboContent) false else loadLastShortcutSingleKeyMode(context))
    }

    // ── 실제 풀사이즈 키보드 레이아웃 ──
    // 각 행: KeyDef(label, hidCode, widthWeight, modBit, modSecondary). modBit!=0은 수정자 키.
    // 모든 행의 총 weight를 15.0으로 통일 → 비율 일치.
    data class KeyDef(val label: String, val code: Int, val weight: Float = 1f, val modBit: Int = 0, val modSecondary: Boolean = false)
    fun spacer(w: Float) = KeyDef("", 0, w)
    fun modifier(label: String, bit: Int, weight: Float = 1f, secondary: Boolean = false) = KeyDef(label, 0, weight, bit, secondary)

    val kbRows = listOf(
        // Fn 행 (총 13키 → 각 1.0, Esc 약간 좁게 표현)
        listOf(
            KeyDef("Esc", 0x29, 1.0f),
            spacer(0.5f),
            KeyDef("F1", 0x3A), KeyDef("F2", 0x3B), KeyDef("F3", 0x3C), KeyDef("F4", 0x3D),
            spacer(0.25f),
            KeyDef("F5", 0x3E), KeyDef("F6", 0x3F), KeyDef("F7", 0x40), KeyDef("F8", 0x41),
            spacer(0.25f),
            KeyDef("F9", 0x42), KeyDef("F10", 0x43), KeyDef("F11", 0x44), KeyDef("F12", 0x45)
        ),
        // 숫자 행 (총 weight 15: ` 1-0 - = + BkSp×2)
        listOf(
            KeyDef("`", 0x35), KeyDef("1", 0x1E), KeyDef("2", 0x1F), KeyDef("3", 0x20),
            KeyDef("4", 0x21), KeyDef("5", 0x22), KeyDef("6", 0x23), KeyDef("7", 0x24),
            KeyDef("8", 0x25), KeyDef("9", 0x26), KeyDef("0", 0x27),
            KeyDef("-", 0x2D), KeyDef("=", 0x2E),
            KeyDef("BkSp", 0x2A, 2f)
        ),
        // QWERTY 행 (총 weight 15: Tab×1.5 + Q-] + \×1.5)
        listOf(
            KeyDef("Tab", 0x2B, 1.5f),
            KeyDef("Q", 0x14), KeyDef("W", 0x1A), KeyDef("E", 0x08), KeyDef("R", 0x15),
            KeyDef("T", 0x17), KeyDef("Y", 0x1C), KeyDef("U", 0x18), KeyDef("I", 0x0C),
            KeyDef("O", 0x12), KeyDef("P", 0x13),
            KeyDef("[", 0x2F), KeyDef("]", 0x30),
            KeyDef("\\", 0x31, 1.5f)
        ),
        // 홈 행 (총 weight 15: CapsLk×1.75 + A-' + Enter×2.25)
        listOf(
            KeyDef("CapsLk", 0x39, 1.75f),
            KeyDef("A", 0x04), KeyDef("S", 0x16), KeyDef("D", 0x07), KeyDef("F", 0x09),
            KeyDef("G", 0x0A), KeyDef("H", 0x0B), KeyDef("J", 0x0D), KeyDef("K", 0x0E),
            KeyDef("L", 0x0F), KeyDef(";", 0x33), KeyDef("'", 0x34),
            KeyDef("Enter", 0x28, 2.25f)
        ),
        // ZXCV 행 (총 weight 15: Shift×2.5 + Z-/ + Shift×2.5)
        listOf(
            modifier("Shift", MOD_BIT_LSHIFT.toInt(), 2.5f),
            KeyDef("Z", 0x1D), KeyDef("X", 0x1B), KeyDef("C", 0x06), KeyDef("V", 0x19),
            KeyDef("B", 0x05), KeyDef("N", 0x11), KeyDef("M", 0x10),
            KeyDef(",", 0x36), KeyDef(".", 0x37), KeyDef("/", 0x38),
            modifier("Shift", MOD_BIT_LSHIFT.toInt(), 2.5f, secondary = true)
        ),
        // 하단 행 (총 weight 15: Ctrl+Win+Alt + Space + Alt+Win+Ctrl)
        listOf(
            modifier("Ctrl", MOD_BIT_LCTRL.toInt(), 1.5f),
            modifier("Win", MOD_BIT_LGUI.toInt(), 1.25f),
            modifier("Alt", MOD_BIT_LALT.toInt(), 1.25f),
            KeyDef("Space", 0x2C, 7f),
            modifier("Alt", MOD_BIT_LALT.toInt(), 1.25f, secondary = true),
            modifier("Win", MOD_BIT_LGUI.toInt(), 1.25f, secondary = true),
            modifier("Ctrl", MOD_BIT_LCTRL.toInt(), 1.5f, secondary = true),
        )
    )
    // 내비게이션 클러스터: 2행 (3×2 블록 + 인버티드-T 화살표 나란히)
    // 총 weight 6.5 (3 + 0.5 + 3) — 두 행 동일하여 열 정렬 보장
    val navRows = listOf(
        listOf(
            KeyDef("Ins", 0x49), KeyDef("Home", 0x4A), KeyDef("PgUp", 0x4B),
            spacer(0.5f),
            spacer(1f), KeyDef("↑", 0x52), spacer(1f)
        ),
        listOf(
            KeyDef("Del", 0x4C), KeyDef("End", 0x4D), KeyDef("PgDn", 0x4E),
            spacer(0.5f),
            KeyDef("←", 0x50), KeyDef("↓", 0x51), KeyDef("→", 0x4F)
        )
    )

    // ── 팝업 컨텐츠 빌더 ──
    @Composable
    fun KeyCell(def: KeyDef, rowIndex: Int, weightMod: Modifier) {
        if (def.code == 0 && def.modBit == 0) { Spacer(weightMod); return }
        val isModifier = def.modBit != 0
        // 단일 키 모드에서 modifier 키는 비활성 렌더만 표시
        val disabled = singleKeyMode && isModifier
        val active = if (isModifier) draft.modifierBits and def.modBit != 0 else def.code in draft.keyCodes
        val bgColor = { focused: Boolean ->
            when {
                disabled -> cs.surface
                active -> cs.primary
                focused -> cs.primary.copy(alpha = 0.25f)
                else -> cs.surface
            }
        }
        val textColor = { focused: Boolean ->
            when {
                disabled -> cs.onSurface.copy(alpha = 0.3f)
                active -> cs.onPrimary
                focused -> cs.primary
                else -> cs.onSurface
            }
        }
        val onTap = {
            if (isModifier) {
                onDraftChange(draft.copy(modifierBits = draft.modifierBits xor def.modBit, presetLabel = ""))
            } else {
                val newKeyCodes = if (singleKeyMode) {
                    // 단일 키 모드: 이미 선택된 키 재탭 시 해제, 새 키 탭 시 교체
                    if (def.code in draft.keyCodes) emptyList() else listOf(def.code)
                } else {
                    when {
                        def.code in draft.keyCodes -> draft.keyCodes - def.code   // 재탭 해제
                        draft.keyCodes.isNotEmpty() -> {                           // 두 번째 일반 키 거부
                            ToastController.show("단축키는 보조키와 일반 키 1개만 조합할 수 있습니다", ToastType.ERROR)
                            draft.keyCodes
                        }
                        else -> listOf(def.code)
                    }
                }
                onDraftChange(draft.copy(keyCodes = newKeyCodes, presetLabel = ""))
            }
        }
        val element: EdgeEditorElement = if (isModifier) EdgeEditorElement.ShortcutModifier(def.modBit, def.modSecondary)
            else EdgeEditorElement.ShortcutKey(def.code)
        if (inputMode == InputMode.SWIPE) {
            if (disabled) {
                // 단일 키 모드: modifier는 SwipeFocusable 없이 비활성 Box만 렌더 (스와이프 포커스가 자동으로 건너뜀)
                Box(
                    modifier = weightMod
                        .padding(horizontal = 1.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(bgColor(false))
                        .padding(vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) { Text(def.label, fontSize = 10.sp, color = textColor(false), maxLines = 1) }
            } else {
                SwipeFocusable(
                    element = element,
                    scope = EdgeEditorScope.ShortcutPopup,
                    shape = RoundedCornerShape(3.dp),
                    showBorderHighlight = false,
                    onActivate = onTap,
                    gridRow = rowIndex,
                    modifier = weightMod,
                ) {
                    val focused = LocalSwipeFocused.current
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 1.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .then(if (focused) Modifier.border(1.dp, Color.White, RoundedCornerShape(3.dp)) else Modifier)
                            .background(bgColor(focused))
                            .padding(vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) { Text(def.label, fontSize = 10.sp, color = textColor(focused), maxLines = 1) }
                }
            }
        } else {
            Box(
                modifier = weightMod
                    .padding(horizontal = 1.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(bgColor(false))
                    .then(if (!disabled) Modifier.clickable(onClick = onTap) else Modifier)
                    .padding(vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) { Text(def.label, fontSize = 10.sp, color = textColor(false), maxLines = 1) }
        }
    }

    @Composable
    fun PopupContent() {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            shape = RoundedCornerShape(12.dp),
            color = cs.surfaceVariant,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    if (singleKeyMode) "키 입력 설정" else "단축키 설정",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = cs.onSurface,
                )

                // ── 단일 키 / 단축키 모드 토글 ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(cs.surface),
                ) {
                    listOf(false to "단일 키", true to "단축키").forEach { (isCombo, label) ->
                        val selected = if (isCombo) !singleKeyMode else singleKeyMode
                        val element = if (isCombo) EdgeEditorElement.ShortcutModeCombo else EdgeEditorElement.ShortcutModeSingleKey
                        val onToggle: () -> Unit = {
                            val newSingle = !isCombo
                            if (singleKeyMode != newSingle) {
                                singleKeyMode = newSingle
                                if (newSingle) onDraftChange(draft.copy(modifierBits = 0, keyCodes = draft.keyCodes.take(1)))
                            }
                        }
                        if (inputMode == InputMode.SWIPE) {
                            SwipeFocusable(
                                element = element,
                                scope = EdgeEditorScope.ShortcutPopup,
                                shape = RoundedCornerShape(6.dp),
                                showBorderHighlight = false,
                                gridRow = 0,
                                onActivate = onToggle,
                                modifier = Modifier.weight(1f),
                            ) {
                                val focused = LocalSwipeFocused.current
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            when {
                                                selected -> cs.primary
                                                focused -> cs.primary.copy(alpha = 0.15f)
                                                else -> Color.Transparent
                                            }
                                        )
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(label, fontSize = 12.sp, color = if (selected) cs.onPrimary else cs.onSurface)
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (selected) cs.primary else Color.Transparent)
                                    .clickable(onClick = onToggle)
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(label, fontSize = 12.sp, color = if (selected) cs.onPrimary else cs.onSurface)
                            }
                        }
                    }
                }

                // ── 현재 조합 시각화 ──
                val keyLabelMap: Map<Int, String> = remember {
                    (kbRows + navRows).flatten()
                        .filter { it.code != 0 && it.modBit == 0 }
                        .associate { it.code to it.label }
                }
                val modifierDefs = listOf(0x01 to "Ctrl", 0x02 to "Shift", 0x04 to "Alt", 0x08 to "Win")
                val comboParts: List<String> = buildList {
                    for ((bit, label) in modifierDefs) {
                        if (draft.modifierBits and bit != 0) add(label)
                    }
                    for (code in draft.keyCodes) {
                        keyLabelMap[code]?.let { add(it) }
                    }
                }

                // ── 콤보 칩 시각화 ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(cs.surface),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (comboParts.isEmpty()) {
                            Text(
                                if (singleKeyMode) "키를 눌러 설정하세요" else "키를 눌러 단축키를 설정하세요",
                                fontSize = 12.sp,
                                color = cs.onSurface.copy(alpha = 0.35f)
                            )
                        } else {
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                comboParts.forEachIndexed { i, label ->
                                    if (i > 0) {
                                        Text(
                                            "+",
                                            fontSize = 11.sp,
                                            color = cs.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .border(1.dp, cs.outline.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                            .background(cs.surfaceVariant)
                                            .padding(horizontal = 7.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            label,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = cs.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ── 풀사이즈 키보드 ──
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    kbRows.forEachIndexed { rowIdx, row ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            row.forEach { key -> KeyCell(key, rowIdx + 1, Modifier.weight(key.weight)) }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    // 내비게이션 클러스터
                    navRows.forEachIndexed { rowIdx, row ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            row.forEach { key -> KeyCell(key, 10 + rowIdx, Modifier.weight(key.weight)) }
                        }
                    }
                }

                HorizontalDivider(color = cs.outline.copy(alpha = 0.2f))

                // ── 아이콘 + 액션명 입력 행 (새 액션으로 추가 컨텍스트에서만 표시) ──
                // 키보드 아래 · 버튼 바 위에 배치 → 스와이프로 키보드 마지막 행에서 자연스럽게 접근 가능
                if (onAddAsCandidate != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // 아이콘 박스 48dp (표시설정 IconBox 패턴)
                        if (inputMode == InputMode.SWIPE) {
                            SwipeFocusable(
                                element = EdgeEditorElement.ShortcutIconButton,
                                scope = EdgeEditorScope.ShortcutPopup,
                                shape = RoundedCornerShape(8.dp),
                                showBorderHighlight = true,
                                onActivate = {
                                    onRequestIconPicker?.invoke(draftIconKey, iconBtnCenter) { draftIconKey = it }
                                },
                                gridRow = 79,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(cs.surface)
                                        .border(0.5.dp, cs.outline.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                        .onGloballyPositioned { coords ->
                                            val bounds = coords.boundsInWindow()
                                            iconBtnCenter = bounds.center
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (draftIconKey.isNotEmpty()) IconRegistry.get(draftIconKey) else Icons.Filled.Keyboard,
                                        contentDescription = "아이콘 선택",
                                        modifier = Modifier.size(if (draftIconKey.isNotEmpty()) 22.dp else 18.dp),
                                        tint = if (draftIconKey.isNotEmpty()) cs.onSurface else cs.onSurface.copy(alpha = 0.35f),
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(cs.surface)
                                    .border(0.5.dp, cs.outline.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .onGloballyPositioned { coords ->
                                        val bounds = coords.boundsInWindow()
                                        iconBtnCenter = bounds.center
                                    }
                                    .clickable { normalIconSheetVisible = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (draftIconKey.isNotEmpty()) IconRegistry.get(draftIconKey) else Icons.Filled.Keyboard,
                                    contentDescription = "아이콘 선택",
                                    modifier = Modifier.size(if (draftIconKey.isNotEmpty()) 22.dp else 18.dp),
                                    tint = if (draftIconKey.isNotEmpty()) cs.onSurface else cs.onSurface.copy(alpha = 0.35f),
                                )
                            }
                        }
                        // 액션명 필드 박스 48dp height (표시설정 LabelBox 패턴)
                        if (inputMode == InputMode.SWIPE) {
                            SwipeFocusable(
                                element = EdgeEditorElement.ShortcutNameField,
                                scope = EdgeEditorScope.ShortcutPopup,
                                shape = RoundedCornerShape(8.dp),
                                showBorderHighlight = true,
                                onActivate = { nameKeyboardActive = true },
                                gridRow = 79,
                                modifier = Modifier.weight(1f),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(cs.surface)
                                        .border(0.5.dp, cs.outline.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp),
                                    contentAlignment = Alignment.CenterStart,
                                ) {
                                    val caretTransition = rememberInfiniteTransition(label = "caret")
                                    val caretAlpha by caretTransition.animateFloat(
                                        initialValue = 1f, targetValue = 0f,
                                        animationSpec = infiniteRepeatable(
                                            animation = keyframes {
                                                durationMillis = 1000
                                                1f at 0
                                                1f at 500
                                                0f at 501
                                                0f at 1000
                                            },
                                            repeatMode = RepeatMode.Restart
                                        ),
                                        label = "caretAlpha"
                                    )
                                    if (draftName.isEmpty() && !nameKeyboardActive) {
                                        Text(
                                            text = previewName.ifEmpty { "액션명 입력..." },
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                            color = cs.onSurfaceVariant.copy(alpha = 0.5f),
                                        )
                                    } else {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = draftName,
                                                fontSize = 14.sp,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                                color = cs.onSurface,
                                                modifier = Modifier.weight(1f, fill = false),
                                            )
                                            if (nameKeyboardActive) {
                                                Text(
                                                    text = "|",
                                                    fontSize = 14.sp,
                                                    color = cs.primary.copy(alpha = caretAlpha),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            BasicTextField(
                                value = draftName,
                                onValueChange = { if (it.length <= 32) draftName = it },
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontSize = 14.sp,
                                    color = cs.onSurface,
                                ),
                                cursorBrush = SolidColor(cs.primary),
                                modifier = Modifier.weight(1f),
                                decorationBox = { innerTextField ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(cs.surface)
                                            .border(0.5.dp, cs.outline.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 12.dp),
                                        contentAlignment = Alignment.CenterStart,
                                    ) {
                                        if (draftName.isEmpty()) {
                                            Text(
                                                previewName.ifEmpty { "액션명 입력..." },
                                                fontSize = 14.sp,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                                color = cs.onSurfaceVariant.copy(alpha = 0.5f),
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                        }
                    }
                }

                // ── 버튼 바: 취소 / 확인 / 새 액션으로 추가 ──
                // 단축키 모드 유효 조건: 보조키 1개 이상 + 일반 키 정확히 1개
                val shortcutComboError: () -> String? = {
                    if (singleKeyMode) null
                    else when {
                        draft.modifierBits == 0 && draft.keyCodes.size == 1 ->
                            "단축키 모드에서는 단일 키를 할당할 수 없습니다"
                        draft.modifierBits != 0 && draft.keyCodes.isEmpty() ->
                            "일반 키를 하나 선택해주세요"
                        draft.keyCodes.size > 1 ->
                            "단축키는 보조키와 일반 키 1개만 조합할 수 있습니다"
                        else -> null
                    }
                }
                val onConfirmGuarded: () -> Unit = {
                    val err = shortcutComboError()
                    if (err != null) {
                        ToastController.show(err, ToastType.ERROR)
                    } else {
                        saveLastShortcutSingleKeyMode(context, singleKeyMode)
                        onConfirm(draft)
                    }
                }
                val onAddGuarded: () -> Unit = {
                    val finalName = draftName.trim().ifBlank { previewName }
                    val err = shortcutComboError()
                    when {
                        err != null ->
                            ToastController.show(err, ToastType.ERROR)
                        finalName.isBlank() ->
                            ToastController.show("키를 선택하거나 액션명을 입력해주세요", ToastType.WARNING)
                        else -> {
                            saveLastShortcutSingleKeyMode(context, singleKeyMode)
                            onAddAsCandidate?.invoke(draftIconKey, finalName)
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (inputMode == InputMode.SWIPE) {
                        SwipeFocusable(
                            element = EdgeEditorElement.ShortcutPopupCancel,
                            scope = EdgeEditorScope.ShortcutPopup,
                            shape = RoundedCornerShape(8.dp),
                            showBorderHighlight = false,
                            onActivate = onCancel,
                            gridRow = 80,
                            modifier = Modifier.weight(1f),
                        ) {
                            val focused = LocalSwipeFocused.current
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (focused) cs.error else Color.Transparent)
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "취소",
                                    fontSize = 13.sp,
                                    color = if (focused) cs.onError else cs.error,
                                )
                            }
                        }
                        SwipeFocusable(
                            element = EdgeEditorElement.ShortcutPopupConfirm,
                            scope = EdgeEditorScope.ShortcutPopup,
                            shape = RoundedCornerShape(8.dp),
                            showBorderHighlight = false,
                            onActivate = onConfirmGuarded,
                            gridRow = 80,
                            modifier = Modifier.weight(1f),
                        ) {
                            val focused = LocalSwipeFocused.current
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (focused) cs.primary else Color.Transparent)
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "확인",
                                    fontSize = 13.sp,
                                    color = if (focused) cs.onPrimary else cs.primary,
                                )
                            }
                        }
                        if (onAddAsCandidate != null) {
                            SwipeFocusable(
                                element = EdgeEditorElement.ShortcutPopupAddCandidate,
                                scope = EdgeEditorScope.ShortcutPopup,
                                shape = RoundedCornerShape(8.dp),
                                showBorderHighlight = false,
                                onActivate = onAddGuarded,
                                gridRow = 80,
                                modifier = Modifier.weight(2f),
                            ) {
                                val focused = LocalSwipeFocused.current
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (focused) cs.secondary else Color.Transparent)
                                        .padding(horizontal = 8.dp, vertical = 10.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        "새 액션으로 추가",
                                        fontSize = 13.sp,
                                        color = if (focused) cs.onSecondary else cs.secondary,
                                    )
                                }
                            }
                        }
                    } else {
                        TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                            Text("취소", fontSize = 13.sp)
                        }
                        Button(
                            onClick = onConfirmGuarded,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                        ) { Text("확인", fontSize = 13.sp) }
                        if (onAddAsCandidate != null) {
                            FilledTonalButton(
                                onClick = onAddGuarded,
                                modifier = Modifier.weight(2f),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = cs.secondaryContainer,
                                    contentColor = cs.onSecondaryContainer,
                                )
                            ) { Text("새 액션으로 추가", fontSize = 13.sp) }
                        }
                    }
                }
            }
        }
    }

    if (inputMode == InputMode.SWIPE) {
        // 인라인 Box 렌더링 — 호출자가 루트 Box에 배치하므로 fillMaxSize가 전체 화면을 채움
        // Popup 사용 금지: 별도 Android Window 생성으로 SwipeGestureLayer 터치 도달 불가
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
        ) {
            val screenH = maxHeight
            var popupCardHeightDp by remember { mutableStateOf(0.dp) }
            var kbActualHeightDp by remember { mutableStateOf(0.dp) }
            val density = LocalDensity.current

            // 팝업 카드 bottom이 키보드 top 바로 위에 오도록 offset 계산.
            // 측정 오차 보정: kbEffectiveH에 16dp 추가해 팝업이 키보드와 겹치지 않도록 함.
            val kbEffectiveH = (if (kbActualHeightDp > 0.dp) kbActualHeightDp
                               else EdgeSwipeConstants.EDGE_ZONE_LABEL_KEYBOARD_VISUAL_HEIGHT_DP.dp) + 16.dp
            val targetOffsetDp = if (popupCardHeightDp > 0.dp && nameKeyboardActive) {
                val kbTop = screenH - kbEffectiveH
                val popupBottomNoOffset = screenH / 2 + popupCardHeightDp / 2
                (kbTop - popupBottomNoOffset).coerceAtMost(0.dp)
            } else 0.dp
            val shortcutKbOffsetY by animateDpAsState(
                targetValue = targetOffsetDp,
                animationSpec = tween(220),
                label = "shortcutKbOffsetY",
            )

            // SwipeKeyboardOverlay는 항상 composition에 있음.
            // gestureFullHeight=true + overlay로 팝업 카드를 자식에 배치 →
            // 루트 Box의 pointerInput(parent)이 팝업 카드 영역 터치도 수신하여 제스처 감지.
            com.bridgeone.app.ui.components.SwipeKeyboardOverlay(
                initialText = draftName,
                maxLength = 32,
                suggestions = emptyList(),
                revertOnCancel = false,
                showScrim = false,
                gestureFullHeight = true,
                showGuide = false,
                showKeyboard = nameKeyboardActive,
                onTextChange = { draftName = it },
                onCancel = { nameKeyboardActive = false },
                onDone = { result -> draftName = result; nameKeyboardActive = false },
                onContentHeightMeasured = { px ->
                    with(density) { kbActualHeightDp = px.toDp() }
                },
                overlay = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(y = shortcutKbOffsetY),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier.onGloballyPositioned { coords ->
                                with(density) { popupCardHeightDp = coords.size.height.toDp() }
                            }
                        ) {
                            PopupContent()
                            if (nameKeyboardActive) {
                                Box(modifier = Modifier.matchParentSize().pointerInput(Unit) {
                                    awaitEachGesture {
                                        awaitFirstDown(requireUnconsumed = false).consume()
                                        while (true) {
                                            val e = awaitPointerEvent()
                                            e.changes.forEach { it.consume() }
                                            if (e.type == androidx.compose.ui.input.pointer.PointerEventType.Release) break
                                        }
                                    }
                                })
                            }
                        }
                    }
                },
            )
        }
    } else {
        Popup(
            alignment = Alignment.Center,
            properties = PopupProperties(focusable = true, dismissOnClickOutside = true),
            onDismissRequest = onCancel,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    ) { onCancel() },
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.clickable(
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                ) { /* consume click — don't dismiss */ }) {
                    PopupContent()
                }
            }
        }
        // NORMAL 모드 아이콘 선택 바텀시트
        if (normalIconSheetVisible && onAddAsCandidate != null) {
            NormalCategoryIconSheet(
                selectedIconKey = draftIconKey,
                sheetState = normalIconSheetState,
                onPick = { key ->
                    draftIconKey = key
                    normalIconSheetVisible = false
                },
                onDismiss = { normalIconSheetVisible = false },
            )
        }
    }
}
