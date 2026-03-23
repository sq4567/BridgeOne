package com.bridgeone.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.util.Log
import com.bridgeone.app.protocol.BridgeMode

// ============================================================
// 행 높이 / 간격 상수
// ============================================================
private val ROW_HEIGHT = 44.dp
private val ROW_SPACING = 3.dp
private val BUTTON_SPACING = 2.dp
private val TAB_HEIGHT = 32.dp

// ============================================================
// HID 키 코드 상수 (모든 탭에서 공유)
// ============================================================

// 문자 키 (HID Usage Table 0x04-0x1D)
private val KEY_A = 0x04.toUByte(); private val KEY_B = 0x05.toUByte()
private val KEY_C = 0x06.toUByte(); private val KEY_D = 0x07.toUByte()
private val KEY_E = 0x08.toUByte(); private val KEY_F = 0x09.toUByte()
private val KEY_G = 0x0A.toUByte(); private val KEY_H = 0x0B.toUByte()
private val KEY_I = 0x0C.toUByte(); private val KEY_J = 0x0D.toUByte()
private val KEY_K = 0x0E.toUByte(); private val KEY_L = 0x0F.toUByte()
private val KEY_M = 0x10.toUByte(); private val KEY_N = 0x11.toUByte()
private val KEY_O = 0x12.toUByte(); private val KEY_P = 0x13.toUByte()
private val KEY_Q = 0x14.toUByte(); private val KEY_R = 0x15.toUByte()
private val KEY_S = 0x16.toUByte(); private val KEY_T = 0x17.toUByte()
private val KEY_U = 0x18.toUByte(); private val KEY_V = 0x19.toUByte()
private val KEY_W = 0x1A.toUByte(); private val KEY_X = 0x1B.toUByte()
private val KEY_Y = 0x1C.toUByte(); private val KEY_Z = 0x1D.toUByte()

// 숫자 키 (0x1E-0x27)
private val KEY_1 = 0x1E.toUByte(); private val KEY_2 = 0x1F.toUByte()
private val KEY_3 = 0x20.toUByte(); private val KEY_4 = 0x21.toUByte()
private val KEY_5 = 0x22.toUByte(); private val KEY_6 = 0x23.toUByte()
private val KEY_7 = 0x24.toUByte(); private val KEY_8 = 0x25.toUByte()
private val KEY_9 = 0x26.toUByte(); private val KEY_0 = 0x27.toUByte()

// 기능 키
private val KEY_ENTER = 0x28.toUByte()
private val KEY_ESC = 0x29.toUByte()
private val KEY_BACKSPACE = 0x2A.toUByte()
private val KEY_TAB = 0x2B.toUByte()
private val KEY_SPACE = 0x2C.toUByte()

// 기호 키
private val KEY_MINUS = 0x2D.toUByte()
private val KEY_EQUAL = 0x2E.toUByte()
private val KEY_LEFTBRACE = 0x2F.toUByte()
private val KEY_RIGHTBRACE = 0x30.toUByte()
private val KEY_BACKSLASH = 0x31.toUByte()
private val KEY_SEMICOLON = 0x33.toUByte()
private val KEY_APOSTROPHE = 0x34.toUByte()
private val KEY_GRAVE = 0x35.toUByte()       // ` ~
private val KEY_COMMA = 0x36.toUByte()
private val KEY_DOT = 0x37.toUByte()
private val KEY_SLASH = 0x38.toUByte()

// F 키 (0x3A-0x45)
private val KEY_F1 = 0x3A.toUByte(); private val KEY_F2 = 0x3B.toUByte()
private val KEY_F3 = 0x3C.toUByte(); private val KEY_F4 = 0x3D.toUByte()
private val KEY_F5 = 0x3E.toUByte(); private val KEY_F6 = 0x3F.toUByte()
private val KEY_F7 = 0x40.toUByte(); private val KEY_F8 = 0x41.toUByte()
private val KEY_F9 = 0x42.toUByte(); private val KEY_F10 = 0x43.toUByte()
private val KEY_F11 = 0x44.toUByte(); private val KEY_F12 = 0x45.toUByte()

// 네비게이션 키
private val KEY_PRINTSCREEN = 0x46.toUByte()
private val KEY_PAUSE = 0x48.toUByte()
private val KEY_DELETE = 0x4C.toUByte()
private val KEY_INSERT = 0x49.toUByte()
private val KEY_HOME = 0x4A.toUByte()
private val KEY_PAGEUP = 0x4B.toUByte()
private val KEY_END = 0x4D.toUByte()
private val KEY_PAGEDOWN = 0x4E.toUByte()

// 화살표 키
private val KEY_RIGHT = 0x4F.toUByte()
private val KEY_LEFT = 0x50.toUByte()
private val KEY_DOWN = 0x51.toUByte()
private val KEY_UP = 0x52.toUByte()

// 수정자 키 (HID Usage 0xE0-0xE7, 비트 플래그가 아닌 고유 식별자)
// ⚠️ 절대 0x01-0x08 사용 금지! 문자 키코드와 충돌함 (A=0x04, E=0x08 등)
private val KEY_CTRL_LEFT = 0xE0.toUByte()   // Left Control
private val KEY_SHIFT_LEFT = 0xE1.toUByte()  // Left Shift
private val KEY_ALT_LEFT = 0xE2.toUByte()    // Left Alt
private val KEY_GUI_LEFT = 0xE3.toUByte()    // Left GUI (Win)
private val KEY_HAN_YEONG = 0xE6.toUByte()   // Right Alt → 한/영 전환
private val KEY_HANJA = 0xE4.toUByte()       // Right Ctrl → 한자 변환

// ============================================================
// KeyboardLayout 메인 컴포넌트
// ============================================================

/**
 * KeyboardLayout 컴포넌트
 *
 * 반응형 키보드 레이아웃. fillMaxWidth()로 화면 너비에 맞게 자동 조절됩니다.
 *
 * **모드별 동작**:
 * - Essential 모드: Boot Keyboard Cluster만 표시 (Del, Esc, Enter, F1-F12, 방향키)
 * - Standard 모드: 전체 3탭 레이아웃 (문자/숫자기호/기능키)
 *
 * **탭 구성 (Standard 모드)**:
 * - 탭 0: 문자 (QWERTY + Shift/Ctrl/Alt + Space + Enter + 한/영)
 * - 탭 1: 숫자/기호 (0-9, 특수문자, Tab, Esc, Del, 한자, Home/End 등)
 * - 탭 2: 기능 (F1-F12, 화살표)
 */
@Composable
fun KeyboardLayout(
    onKeyPressed: (keyCode: UByte) -> Unit = {},
    onKeyReleased: (keyCode: UByte) -> Unit = {},
    activeKeys: Set<UByte> = emptySet(),
    bridgeMode: BridgeMode = BridgeMode.ESSENTIAL,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val TAB_LABELS = listOf("문자", "숫자/기호", "기능")
    // 콘텐츠 영역 높이 (최대 5행 기준 — 기능 탭이 5행)
    val contentHeight = ROW_HEIGHT * 5 + ROW_SPACING * 4

    Column(
        modifier = modifier
            .background(Color(0xFF0D0D0D))
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        if (bridgeMode == BridgeMode.ESSENTIAL) {
            // Essential 모드: Boot Cluster만 표시 (탭 없이)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(contentHeight)
            ) {
                KeyboardBootCluster(onKeyPressed, onKeyReleased, activeKeys)
            }
        } else {
            // Standard 모드: 전체 레이아웃 (3탭)
            TabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(TAB_HEIGHT),
                containerColor = Color(0xFF1A1A1A),
                contentColor = Color(0xFF2196F3),
                indicator = { tabPositions ->
                    if (selectedTabIndex < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = Color(0xFF2196F3)
                        )
                    }
                },
                divider = {}
            ) {
                TAB_LABELS.forEachIndexed { index, label ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = {
                            selectedTabIndex = index
                            Log.d("KeyboardLayout", "Tab: $label ($index)")
                        },
                        modifier = Modifier.height(TAB_HEIGHT),
                        text = {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(contentHeight)
            ) {
                when (selectedTabIndex) {
                    0 -> KeyboardTabCharacters(onKeyPressed, onKeyReleased, activeKeys)
                    1 -> KeyboardTabSymbols(onKeyPressed, onKeyReleased, activeKeys)
                    2 -> KeyboardTabFunction(onKeyPressed, onKeyReleased, activeKeys)
                }
            }
        }
    }
}

// ============================================================
// Essential 모드: Boot Keyboard Cluster
// ============================================================
/**
 * Essential 모드 키보드 레이아웃
 *
 * 서버 미연결 상태(Essential 모드)에서 표시되는 최소 키 클러스터.
 * 탭 없이 단일 레이아웃으로 표시됩니다.
 *
 * 레이아웃:
 * ```
 * Del  Esc  Enter
 * F1  F2  F3  F4  F5  F6
 * F7  F8  F9  F10 F11 F12
 *             ↑
 *         ←   ↓   →
 * ```
 */
@Composable
private fun KeyboardBootCluster(
    onKeyPressed: (keyCode: UByte) -> Unit,
    onKeyReleased: (keyCode: UByte) -> Unit,
    activeKeys: Set<UByte>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(horizontal = 2.dp),
        verticalArrangement = Arrangement.spacedBy(ROW_SPACING)
    ) {
        // Row 1: Del Esc Enter
        KeyRow(
            keys = listOf(
                "Del" to KEY_DELETE, "Esc" to KEY_ESC, "Enter" to KEY_ENTER
            ),
            onKeyPressed = onKeyPressed, onKeyReleased = onKeyReleased, activeKeys = activeKeys,
            modifier = Modifier.weight(1f)
        )

        // Row 2: F1-F6
        KeyRow(
            keys = listOf(
                "F1" to KEY_F1, "F2" to KEY_F2, "F3" to KEY_F3,
                "F4" to KEY_F4, "F5" to KEY_F5, "F6" to KEY_F6
            ),
            onKeyPressed = onKeyPressed, onKeyReleased = onKeyReleased, activeKeys = activeKeys,
            modifier = Modifier.weight(1f)
        )

        // Row 3: F7-F12
        KeyRow(
            keys = listOf(
                "F7" to KEY_F7, "F8" to KEY_F8, "F9" to KEY_F9,
                "F10" to KEY_F10, "F11" to KEY_F11, "F12" to KEY_F12
            ),
            onKeyPressed = onKeyPressed, onKeyReleased = onKeyReleased, activeKeys = activeKeys,
            modifier = Modifier.weight(1f)
        )

        // Row 4: ↑ (가운데 정렬)
        Row(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            KeyboardKeyButton(
                keyLabel = "↑", keyCode = KEY_UP,
                isActive = KEY_UP in activeKeys,
                onKeyPressed = { onKeyPressed(KEY_UP) },
                onKeyReleased = { onKeyReleased(KEY_UP) },
                modifier = Modifier.size(52.dp).fillMaxHeight()
            )
        }

        // Row 5: ← ↓ →
        Row(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            KeyboardKeyButton(
                keyLabel = "←", keyCode = KEY_LEFT,
                isActive = KEY_LEFT in activeKeys,
                onKeyPressed = { onKeyPressed(KEY_LEFT) },
                onKeyReleased = { onKeyReleased(KEY_LEFT) },
                modifier = Modifier.size(52.dp).fillMaxHeight()
            )
            Spacer(modifier = Modifier.width(4.dp))
            KeyboardKeyButton(
                keyLabel = "↓", keyCode = KEY_DOWN,
                isActive = KEY_DOWN in activeKeys,
                onKeyPressed = { onKeyPressed(KEY_DOWN) },
                onKeyReleased = { onKeyReleased(KEY_DOWN) },
                modifier = Modifier.size(52.dp).fillMaxHeight()
            )
            Spacer(modifier = Modifier.width(4.dp))
            KeyboardKeyButton(
                keyLabel = "→", keyCode = KEY_RIGHT,
                isActive = KEY_RIGHT in activeKeys,
                onKeyPressed = { onKeyPressed(KEY_RIGHT) },
                onKeyReleased = { onKeyReleased(KEY_RIGHT) },
                modifier = Modifier.size(52.dp).fillMaxHeight()
            )
        }
    }
}

// ============================================================
// 탭 0: 문자 (QWERTY)
// ============================================================
/**
 * 레이아웃:
 * ```
 * Q  W  E  R  T  Y  U  I  O  P
 * A  S  D  F  G  H  J  K  L
 * ⇧  Z  X  C  V  B  N  M  ⌫
 * Ctrl Alt [   Space   ] ⏎ 한/영
 * ```
 */
@Composable
private fun KeyboardTabCharacters(
    onKeyPressed: (keyCode: UByte) -> Unit,
    onKeyReleased: (keyCode: UByte) -> Unit,
    activeKeys: Set<UByte>
) {
    var shiftActive by remember { mutableStateOf(false) }
    var ctrlActive by remember { mutableStateOf(false) }
    var altActive by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(horizontal = 2.dp),
        verticalArrangement = Arrangement.spacedBy(ROW_SPACING)
    ) {
        // Row 1: Q W E R T Y U I O P
        KeyRow(
            keys = listOf(
                "Q" to KEY_Q, "W" to KEY_W, "E" to KEY_E, "R" to KEY_R, "T" to KEY_T,
                "Y" to KEY_Y, "U" to KEY_U, "I" to KEY_I, "O" to KEY_O, "P" to KEY_P
            ),
            onKeyPressed = onKeyPressed, onKeyReleased = onKeyReleased, activeKeys = activeKeys,
            modifier = Modifier.weight(1f)
        )

        // Row 2: A S D F G H J K L
        KeyRow(
            keys = listOf(
                "A" to KEY_A, "S" to KEY_S, "D" to KEY_D, "F" to KEY_F, "G" to KEY_G,
                "H" to KEY_H, "J" to KEY_J, "K" to KEY_K, "L" to KEY_L
            ),
            onKeyPressed = onKeyPressed, onKeyReleased = onKeyReleased, activeKeys = activeKeys,
            modifier = Modifier.weight(1f)
        )

        // Row 3: Shift Z X C V B N M Backspace
        Row(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(BUTTON_SPACING)
        ) {
            KeyboardKeyButton(
                keyLabel = "⇧", keyCode = KEY_SHIFT_LEFT,
                isActive = shiftActive || KEY_SHIFT_LEFT in activeKeys,
                onKeyPressed = { shiftActive = true; onKeyPressed(it) },
                onKeyReleased = { shiftActive = false; onKeyReleased(it) },
                modifier = Modifier.weight(1.3f).fillMaxHeight()
            )
            listOf(
                "Z" to KEY_Z, "X" to KEY_X, "C" to KEY_C, "V" to KEY_V,
                "B" to KEY_B, "N" to KEY_N, "M" to KEY_M
            ).forEach { (label, keyCode) ->
                KeyboardKeyButton(
                    keyLabel = label, keyCode = keyCode,
                    isActive = keyCode in activeKeys,
                    onKeyPressed = { onKeyPressed(keyCode) },
                    onKeyReleased = { onKeyReleased(keyCode) },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
            KeyboardKeyButton(
                keyLabel = "⌫", keyCode = KEY_BACKSPACE,
                isActive = KEY_BACKSPACE in activeKeys,
                onKeyPressed = { onKeyPressed(KEY_BACKSPACE) },
                onKeyReleased = { onKeyReleased(KEY_BACKSPACE) },
                modifier = Modifier.weight(1.3f).fillMaxHeight()
            )
        }

        // Row 4: Ctrl Alt [Space] Enter 한/영
        Row(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(BUTTON_SPACING)
        ) {
            KeyboardKeyButton(
                keyLabel = "Ctrl", keyCode = KEY_CTRL_LEFT,
                isActive = ctrlActive || KEY_CTRL_LEFT in activeKeys,
                onKeyPressed = { ctrlActive = true; onKeyPressed(it) },
                onKeyReleased = { ctrlActive = false; onKeyReleased(it) },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            KeyboardKeyButton(
                keyLabel = "Alt", keyCode = KEY_ALT_LEFT,
                isActive = altActive || KEY_ALT_LEFT in activeKeys,
                onKeyPressed = { altActive = true; onKeyPressed(it) },
                onKeyReleased = { altActive = false; onKeyReleased(it) },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            KeyboardKeyButton(
                keyLabel = "Space", keyCode = KEY_SPACE,
                isActive = KEY_SPACE in activeKeys,
                onKeyPressed = { onKeyPressed(KEY_SPACE) },
                onKeyReleased = { onKeyReleased(KEY_SPACE) },
                modifier = Modifier.weight(3f).fillMaxHeight()
            )
            KeyboardKeyButton(
                keyLabel = "⏎", keyCode = KEY_ENTER,
                isActive = KEY_ENTER in activeKeys,
                onKeyPressed = { onKeyPressed(KEY_ENTER) },
                onKeyReleased = { onKeyReleased(KEY_ENTER) },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            KeyboardKeyButton(
                keyLabel = "한/영", keyCode = KEY_HAN_YEONG,
                isActive = KEY_HAN_YEONG in activeKeys,
                onKeyPressed = { onKeyPressed(KEY_HAN_YEONG) },
                onKeyReleased = { onKeyReleased(KEY_HAN_YEONG) },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }
    }
}

// ============================================================
// 탭 1: 숫자/기호
// ============================================================
/**
 * 레이아웃:
 * ```
 * 1  2  3  4  5  6  7  8  9  0
 * -  =  [  ]  \  ;  '  ,  .  /
 * `  Tab  Esc  Del  한자
 * Home End PgUp PgDn Ins
 * ```
 */
@Composable
private fun KeyboardTabSymbols(
    onKeyPressed: (keyCode: UByte) -> Unit,
    onKeyReleased: (keyCode: UByte) -> Unit,
    activeKeys: Set<UByte>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(horizontal = 2.dp),
        verticalArrangement = Arrangement.spacedBy(ROW_SPACING)
    ) {
        // Row 1: 1-0
        KeyRow(
            keys = listOf(
                "1" to KEY_1, "2" to KEY_2, "3" to KEY_3, "4" to KEY_4, "5" to KEY_5,
                "6" to KEY_6, "7" to KEY_7, "8" to KEY_8, "9" to KEY_9, "0" to KEY_0
            ),
            onKeyPressed = onKeyPressed, onKeyReleased = onKeyReleased, activeKeys = activeKeys,
            modifier = Modifier.weight(1f)
        )

        // Row 2: - = [ ] \ ; ' , . /
        KeyRow(
            keys = listOf(
                "-" to KEY_MINUS, "=" to KEY_EQUAL, "[" to KEY_LEFTBRACE,
                "]" to KEY_RIGHTBRACE, "\\" to KEY_BACKSLASH, ";" to KEY_SEMICOLON,
                "'" to KEY_APOSTROPHE, "," to KEY_COMMA, "." to KEY_DOT, "/" to KEY_SLASH
            ),
            onKeyPressed = onKeyPressed, onKeyReleased = onKeyReleased, activeKeys = activeKeys,
            modifier = Modifier.weight(1f)
        )

        // Row 3: ` Tab Esc Del 한자
        KeyRow(
            keys = listOf(
                "`" to KEY_GRAVE, "Tab" to KEY_TAB, "Esc" to KEY_ESC,
                "Del" to KEY_DELETE, "한자" to KEY_HANJA
            ),
            onKeyPressed = onKeyPressed, onKeyReleased = onKeyReleased, activeKeys = activeKeys,
            modifier = Modifier.weight(1f)
        )

        // Row 4: Home End PgUp PgDn Ins
        KeyRow(
            keys = listOf(
                "Home" to KEY_HOME, "End" to KEY_END, "PgUp" to KEY_PAGEUP,
                "PgDn" to KEY_PAGEDOWN, "Ins" to KEY_INSERT
            ),
            onKeyPressed = onKeyPressed, onKeyReleased = onKeyReleased, activeKeys = activeKeys,
            modifier = Modifier.weight(1f)
        )
    }
}

// ============================================================
// 탭 2: 기능 키 (F1-F12 + 화살표)
// ============================================================
/**
 * 레이아웃:
 * ```
 * F1  F2  F3  F4  F5  F6
 * F7  F8  F9  F10 F11 F12
 *          ↑
 *      ←   ↓   →
 * Win  PrtSc  Pause
 * ```
 */
@Composable
private fun KeyboardTabFunction(
    onKeyPressed: (keyCode: UByte) -> Unit,
    onKeyReleased: (keyCode: UByte) -> Unit,
    activeKeys: Set<UByte>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(horizontal = 2.dp),
        verticalArrangement = Arrangement.spacedBy(ROW_SPACING)
    ) {
        // Row 1: F1-F6
        KeyRow(
            keys = listOf(
                "F1" to KEY_F1, "F2" to KEY_F2, "F3" to KEY_F3,
                "F4" to KEY_F4, "F5" to KEY_F5, "F6" to KEY_F6
            ),
            onKeyPressed = onKeyPressed, onKeyReleased = onKeyReleased, activeKeys = activeKeys,
            modifier = Modifier.weight(1f)
        )

        // Row 2: F7-F12
        KeyRow(
            keys = listOf(
                "F7" to KEY_F7, "F8" to KEY_F8, "F9" to KEY_F9,
                "F10" to KEY_F10, "F11" to KEY_F11, "F12" to KEY_F12
            ),
            onKeyPressed = onKeyPressed, onKeyReleased = onKeyReleased, activeKeys = activeKeys,
            modifier = Modifier.weight(1f)
        )

        // Row 3: ↑ (가운데 정렬)
        Row(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            KeyboardKeyButton(
                keyLabel = "↑", keyCode = KEY_UP,
                isActive = KEY_UP in activeKeys,
                onKeyPressed = { onKeyPressed(KEY_UP) },
                onKeyReleased = { onKeyReleased(KEY_UP) },
                modifier = Modifier.size(52.dp).fillMaxHeight()
            )
        }

        // Row 4: ← ↓ →
        Row(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            KeyboardKeyButton(
                keyLabel = "←", keyCode = KEY_LEFT,
                isActive = KEY_LEFT in activeKeys,
                onKeyPressed = { onKeyPressed(KEY_LEFT) },
                onKeyReleased = { onKeyReleased(KEY_LEFT) },
                modifier = Modifier.size(52.dp).fillMaxHeight()
            )
            Spacer(modifier = Modifier.width(4.dp))
            KeyboardKeyButton(
                keyLabel = "↓", keyCode = KEY_DOWN,
                isActive = KEY_DOWN in activeKeys,
                onKeyPressed = { onKeyPressed(KEY_DOWN) },
                onKeyReleased = { onKeyReleased(KEY_DOWN) },
                modifier = Modifier.size(52.dp).fillMaxHeight()
            )
            Spacer(modifier = Modifier.width(4.dp))
            KeyboardKeyButton(
                keyLabel = "→", keyCode = KEY_RIGHT,
                isActive = KEY_RIGHT in activeKeys,
                onKeyPressed = { onKeyPressed(KEY_RIGHT) },
                onKeyReleased = { onKeyReleased(KEY_RIGHT) },
                modifier = Modifier.size(52.dp).fillMaxHeight()
            )
        }

        // Row 5: Win + PrtSc + Pause
        KeyRow(
            keys = listOf(
                "Win" to KEY_GUI_LEFT, "PrtSc" to KEY_PRINTSCREEN, "Pause" to KEY_PAUSE
            ),
            onKeyPressed = onKeyPressed, onKeyReleased = onKeyReleased, activeKeys = activeKeys,
            modifier = Modifier.weight(1f)
        )
    }
}

// ============================================================
// 공통 컴포넌트
// ============================================================

/**
 * 공통 키 행: 동일 크기의 키를 가로로 균등 배치
 */
@Composable
private fun KeyRow(
    keys: List<Pair<String, UByte>>,
    onKeyPressed: (keyCode: UByte) -> Unit,
    onKeyReleased: (keyCode: UByte) -> Unit,
    activeKeys: Set<UByte>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(BUTTON_SPACING)
    ) {
        keys.forEach { (label, keyCode) ->
            KeyboardKeyButton(
                keyLabel = label, keyCode = keyCode,
                isActive = keyCode in activeKeys,
                onKeyPressed = { onKeyPressed(keyCode) },
                onKeyReleased = { onKeyReleased(keyCode) },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }
    }
}

// ============================================================
// Preview
// ============================================================

@Preview(
    showBackground = true,
    backgroundColor = 0xFF0D0D0D,
    widthDp = 360,
    heightDp = 240
)
@Composable
fun KeyboardLayoutPreview() {
    KeyboardLayout(
        onKeyPressed = { Log.d("Preview", "Pressed: 0x${it.toString(16)}") },
        onKeyReleased = { Log.d("Preview", "Released: 0x${it.toString(16)}") },
        modifier = Modifier.fillMaxWidth()
    )
}
