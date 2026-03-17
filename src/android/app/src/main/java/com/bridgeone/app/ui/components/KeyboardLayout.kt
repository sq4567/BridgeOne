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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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

// 행 높이 상수
private val ROW_HEIGHT = 42.dp
private val ROW_SPACING = 3.dp
private val BUTTON_SPACING = 2.dp
private val TAB_HEIGHT = 32.dp

/**
 * KeyboardLayout 컴포넌트
 *
 * 접근성 우선 설계에 맞춘 반응형 키보드 레이아웃입니다.
 * fillMaxWidth()로 화면 너비에 맞게 자동 조절됩니다.
 *
 * **탭 구성**:
 * - 탭 0: 문자 (QWERTY + Space + 수정자)
 * - 탭 1: 숫자/기호 (0-9, 특수문자, Backspace, Tab)
 * - 탭 2: 기능 (화살표, Enter, Del, Esc)
 *
 * @param onKeyPressed 키 누르기 콜백
 * @param onKeyReleased 키 해제 콜백
 * @param activeKeys 현재 활성화된 키 코드 Set
 * @param modifier 외부 Modifier
 */
@Composable
fun KeyboardLayout(
    onKeyPressed: (keyCode: UByte) -> Unit = {},
    onKeyReleased: (keyCode: UByte) -> Unit = {},
    activeKeys: Set<UByte> = emptySet(),
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val TAB_LABELS = listOf("문자", "숫자/기호", "기능")

    Column(
        modifier = modifier
            .background(Color(0xFF0D0D0D))
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        // 탭 바
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

        // 콘텐츠 영역 (모든 탭 동일 높이: 5행 기준)
        val contentHeight = ROW_HEIGHT * 5 + ROW_SPACING * 4
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

/**
 * 탭 0: 문자 키보드 (QWERTY)
 *
 * 레이아웃:
 * ```
 * Q W E R T Y U I O P
 * A S D F G H J K L
 * Z X C V B N M
 * [      Space      ]
 * Shift  Alt    Ctrl
 * ```
 */
@Composable
private fun KeyboardTabCharacters(
    onKeyPressed: (keyCode: UByte) -> Unit,
    onKeyReleased: (keyCode: UByte) -> Unit,
    activeKeys: Set<UByte>
) {
    val KEY_Q = 0x14.toUByte(); val KEY_W = 0x1A.toUByte(); val KEY_E = 0x08.toUByte()
    val KEY_R = 0x15.toUByte(); val KEY_T = 0x17.toUByte(); val KEY_Y = 0x1C.toUByte()
    val KEY_U = 0x18.toUByte(); val KEY_I = 0x0C.toUByte(); val KEY_O = 0x12.toUByte()
    val KEY_P = 0x13.toUByte()

    val KEY_A = 0x04.toUByte(); val KEY_S = 0x16.toUByte(); val KEY_D = 0x07.toUByte()
    val KEY_F = 0x09.toUByte(); val KEY_G = 0x0A.toUByte(); val KEY_H = 0x0B.toUByte()
    val KEY_J = 0x0D.toUByte(); val KEY_K = 0x0E.toUByte(); val KEY_L = 0x0F.toUByte()

    val KEY_Z = 0x1D.toUByte(); val KEY_X = 0x1B.toUByte(); val KEY_C = 0x06.toUByte()
    val KEY_V = 0x19.toUByte(); val KEY_B = 0x05.toUByte(); val KEY_N = 0x11.toUByte()
    val KEY_M = 0x10.toUByte()

    val KEY_SPACE = 0x2C.toUByte()
    val KEY_SHIFT_LEFT = 0x02.toUByte()
    val KEY_CTRL_LEFT = 0x01.toUByte()
    val KEY_ALT_LEFT = 0x04.toUByte()

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
            onKeyPressed = onKeyPressed,
            onKeyReleased = onKeyReleased,
            activeKeys = activeKeys,
            modifier = Modifier.weight(1f)
        )

        // Row 2: A S D F G H J K L
        KeyRow(
            keys = listOf(
                "A" to KEY_A, "S" to KEY_S, "D" to KEY_D, "F" to KEY_F, "G" to KEY_G,
                "H" to KEY_H, "J" to KEY_J, "K" to KEY_K, "L" to KEY_L
            ),
            onKeyPressed = onKeyPressed,
            onKeyReleased = onKeyReleased,
            activeKeys = activeKeys,
            modifier = Modifier.weight(1f)
        )

        // Row 3: Z X C V B N M
        KeyRow(
            keys = listOf(
                "Z" to KEY_Z, "X" to KEY_X, "C" to KEY_C, "V" to KEY_V,
                "B" to KEY_B, "N" to KEY_N, "M" to KEY_M
            ),
            onKeyPressed = onKeyPressed,
            onKeyReleased = onKeyReleased,
            activeKeys = activeKeys,
            modifier = Modifier.weight(1f)
        )

        // Row 4: Space
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            KeyboardKeyButton(
                keyLabel = "Space",
                keyCode = KEY_SPACE,
                isActive = KEY_SPACE in activeKeys,
                onKeyPressed = { onKeyPressed(KEY_SPACE) },
                onKeyReleased = { onKeyReleased(KEY_SPACE) },
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
            )
        }

        // Row 5: Shift + Alt + Ctrl
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            KeyboardKeyButton(
                keyLabel = "Shift",
                keyCode = KEY_SHIFT_LEFT,
                isActive = shiftActive || KEY_SHIFT_LEFT in activeKeys,
                onKeyPressed = { shiftActive = true; onKeyPressed(it) },
                onKeyReleased = { shiftActive = false; onKeyReleased(it) },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            KeyboardKeyButton(
                keyLabel = "Alt",
                keyCode = KEY_ALT_LEFT,
                isActive = altActive || KEY_ALT_LEFT in activeKeys,
                onKeyPressed = { altActive = true; onKeyPressed(it) },
                onKeyReleased = { altActive = false; onKeyReleased(it) },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            KeyboardKeyButton(
                keyLabel = "Ctrl",
                keyCode = KEY_CTRL_LEFT,
                isActive = ctrlActive || KEY_CTRL_LEFT in activeKeys,
                onKeyPressed = { ctrlActive = true; onKeyPressed(it) },
                onKeyReleased = { ctrlActive = false; onKeyReleased(it) },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }
    }
}

/**
 * 탭 1: 숫자/기호 키보드
 *
 * 레이아웃:
 * ```
 * 1 2 3 4 5 6 7 8 9 0
 * - = [ ] \ ; ' , . /
 * [  BkSp  ] [ Tab  ]
 * ```
 */
@Composable
private fun KeyboardTabSymbols(
    onKeyPressed: (keyCode: UByte) -> Unit,
    onKeyReleased: (keyCode: UByte) -> Unit,
    activeKeys: Set<UByte>
) {
    val KEY_1 = 0x1E.toUByte(); val KEY_2 = 0x1F.toUByte(); val KEY_3 = 0x20.toUByte()
    val KEY_4 = 0x21.toUByte(); val KEY_5 = 0x22.toUByte(); val KEY_6 = 0x23.toUByte()
    val KEY_7 = 0x24.toUByte(); val KEY_8 = 0x25.toUByte(); val KEY_9 = 0x26.toUByte()
    val KEY_0 = 0x27.toUByte()

    val KEY_MINUS = 0x2D.toUByte(); val KEY_EQUAL = 0x2E.toUByte()
    val KEY_LEFTBRACE = 0x2F.toUByte(); val KEY_RIGHTBRACE = 0x30.toUByte()
    val KEY_SEMICOLON = 0x33.toUByte(); val KEY_APOSTROPHE = 0x34.toUByte()
    val KEY_COMMA = 0x36.toUByte(); val KEY_DOT = 0x37.toUByte()
    val KEY_SLASH = 0x38.toUByte(); val KEY_BACKSLASH = 0x31.toUByte()

    val KEY_BACKSPACE = 0x2A.toUByte()
    val KEY_TAB = 0x2B.toUByte()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(horizontal = 2.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Row 1: 1 2 3 4 5 6 7 8 9 0
        KeyRow(
            keys = listOf(
                "1" to KEY_1, "2" to KEY_2, "3" to KEY_3, "4" to KEY_4, "5" to KEY_5,
                "6" to KEY_6, "7" to KEY_7, "8" to KEY_8, "9" to KEY_9, "0" to KEY_0
            ),
            onKeyPressed = onKeyPressed,
            onKeyReleased = onKeyReleased,
            activeKeys = activeKeys,
            modifier = Modifier.height(ROW_HEIGHT)
        )

        Spacer(modifier = Modifier.height(ROW_SPACING))

        // Row 2: - = [ ] \ ; ' , . /
        KeyRow(
            keys = listOf(
                "-" to KEY_MINUS, "=" to KEY_EQUAL, "[" to KEY_LEFTBRACE,
                "]" to KEY_RIGHTBRACE, "\\" to KEY_BACKSLASH, ";" to KEY_SEMICOLON,
                "'" to KEY_APOSTROPHE, "," to KEY_COMMA, "." to KEY_DOT, "/" to KEY_SLASH
            ),
            onKeyPressed = onKeyPressed,
            onKeyReleased = onKeyReleased,
            activeKeys = activeKeys,
            modifier = Modifier.height(ROW_HEIGHT)
        )

        Spacer(modifier = Modifier.height(ROW_SPACING))

        // Row 3: BkSp + Tab
        Row(
            modifier = Modifier
                .height(ROW_HEIGHT)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(BUTTON_SPACING)
        ) {
            KeyboardKeyButton(
                keyLabel = "BkSp",
                keyCode = KEY_BACKSPACE,
                isActive = KEY_BACKSPACE in activeKeys,
                onKeyPressed = { onKeyPressed(KEY_BACKSPACE) },
                onKeyReleased = { onKeyReleased(KEY_BACKSPACE) },
                modifier = Modifier.weight(1.2f).fillMaxHeight()
            )
            KeyboardKeyButton(
                keyLabel = "Tab",
                keyCode = KEY_TAB,
                isActive = KEY_TAB in activeKeys,
                onKeyPressed = { onKeyPressed(KEY_TAB) },
                onKeyReleased = { onKeyReleased(KEY_TAB) },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }
    }
}

/**
 * 탭 2: 기능 키 키보드
 *
 * 레이아웃:
 * ```
 *       ↑
 *   ←   ↓   →
 * Enter Del Esc
 * ```
 */
@Composable
private fun KeyboardTabFunction(
    onKeyPressed: (keyCode: UByte) -> Unit,
    onKeyReleased: (keyCode: UByte) -> Unit,
    activeKeys: Set<UByte>
) {
    val KEY_UP = 0x52.toUByte()
    val KEY_DOWN = 0x51.toUByte()
    val KEY_LEFT = 0x50.toUByte()
    val KEY_RIGHT = 0x4F.toUByte()
    val KEY_ENTER = 0x28.toUByte()
    val KEY_ESC = 0x29.toUByte()
    val KEY_DELETE = 0x4C.toUByte()

    val arrowSize = 56.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(horizontal = 2.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Row 1: ↑ (가운데 정렬)
        Row(
            modifier = Modifier
                .height(arrowSize)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            KeyboardKeyButton(
                keyLabel = "↑",
                keyCode = KEY_UP,
                isActive = KEY_UP in activeKeys,
                onKeyPressed = { onKeyPressed(KEY_UP) },
                onKeyReleased = { onKeyReleased(KEY_UP) },
                modifier = Modifier.size(arrowSize)
            )
        }

        Spacer(modifier = Modifier.height(ROW_SPACING))

        // Row 2: ← ↓ →
        Row(
            modifier = Modifier
                .height(arrowSize)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            KeyboardKeyButton(
                keyLabel = "←",
                keyCode = KEY_LEFT,
                isActive = KEY_LEFT in activeKeys,
                onKeyPressed = { onKeyPressed(KEY_LEFT) },
                onKeyReleased = { onKeyReleased(KEY_LEFT) },
                modifier = Modifier.size(arrowSize)
            )
            Spacer(modifier = Modifier.width(4.dp))
            KeyboardKeyButton(
                keyLabel = "↓",
                keyCode = KEY_DOWN,
                isActive = KEY_DOWN in activeKeys,
                onKeyPressed = { onKeyPressed(KEY_DOWN) },
                onKeyReleased = { onKeyReleased(KEY_DOWN) },
                modifier = Modifier.size(arrowSize)
            )
            Spacer(modifier = Modifier.width(4.dp))
            KeyboardKeyButton(
                keyLabel = "→",
                keyCode = KEY_RIGHT,
                isActive = KEY_RIGHT in activeKeys,
                onKeyPressed = { onKeyPressed(KEY_RIGHT) },
                onKeyReleased = { onKeyReleased(KEY_RIGHT) },
                modifier = Modifier.size(arrowSize)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Row 3: Enter + Del + Esc
        Row(
            modifier = Modifier
                .height(arrowSize)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            KeyboardKeyButton(
                keyLabel = "Enter",
                keyCode = KEY_ENTER,
                isActive = KEY_ENTER in activeKeys,
                onKeyPressed = { onKeyPressed(KEY_ENTER) },
                onKeyReleased = { onKeyReleased(KEY_ENTER) },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            KeyboardKeyButton(
                keyLabel = "Del",
                keyCode = KEY_DELETE,
                isActive = KEY_DELETE in activeKeys,
                onKeyPressed = { onKeyPressed(KEY_DELETE) },
                onKeyReleased = { onKeyReleased(KEY_DELETE) },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            KeyboardKeyButton(
                keyLabel = "Esc",
                keyCode = KEY_ESC,
                isActive = KEY_ESC in activeKeys,
                onKeyPressed = { onKeyPressed(KEY_ESC) },
                onKeyReleased = { onKeyReleased(KEY_ESC) },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }
    }
}

/**
 * 공통 키 행 컴포넌트
 *
 * 동일 크기의 키를 가로로 균등 배치합니다.
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
                keyLabel = label,
                keyCode = keyCode,
                isActive = keyCode in activeKeys,
                onKeyPressed = { onKeyPressed(keyCode) },
                onKeyReleased = { onKeyReleased(keyCode) },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFF0D0D0D,
    widthDp = 360,
    heightDp = 280
)
@Composable
fun KeyboardLayoutPreview() {
    KeyboardLayout(
        onKeyPressed = { Log.d("Preview", "Pressed: 0x${it.toString(16)}") },
        onKeyReleased = { Log.d("Preview", "Released: 0x${it.toString(16)}") },
        modifier = Modifier.fillMaxWidth()
    )
}
