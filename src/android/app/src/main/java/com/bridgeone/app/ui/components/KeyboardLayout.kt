package com.bridgeone.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
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

/**
 * KeyboardLayout 컴포넌트
 *
 * 접근성 우선 설계에 맞춘 컴팩트 키보드 레이아웃입니다.
 * 중앙 하단 240×280dp 영역에 배치되며, 한손 조작 최적화를 위해 설계되었습니다.
 *
 * **기능**:
 * - 기본 탭: 문자/숫자 배치
 * - 화살표 키: ↑, ↓, ←, → (커서 이동)
 * - 기능 키: Tab, Enter, Backspace, Esc
 * - 수정자 키: Shift (좌측 하단), Ctrl (우측 하단), Alt (중앙 하단)
 * - 탭 전환: "숫자/기호" 및 "기능" 탭 (옵션)
 *
 * **레이아웃 구조**:
 * - 전체 크기: 240×280dp (중앙 하단)
 * - 탭 바: 30dp (상단)
 * - 키보드 영역: 250dp (나머지)
 * - 버튼 간격: 4dp (접근성 고려)
 * - 버튼 크기: 50×50dp (터치 최적화)
 *
 * **색상 시스템**:
 * - 배경: #0D0D0D (거의 검은색, 어두운 테마)
 * - 탭 인디케이터: #2196F3 (Material Blue 500)
 *
 * @param onKeyPressed 키 누르기 콜백 ((keyCode: UByte) -> Unit)
 * @param onKeyReleased 키 해제 콜백 ((keyCode: UByte) -> Unit)
 * @param activeKeys 현재 활성화된 키 코드 Set (시각적 피드백용)
 * @param modifier 외부에서 추가할 수 있는 Modifier
 */
@Composable
fun KeyboardLayout(
    onKeyPressed: (keyCode: UByte) -> Unit = {},
    onKeyReleased: (keyCode: UByte) -> Unit = {},
    activeKeys: Set<UByte> = emptySet(),
    modifier: Modifier = Modifier
) {
    // 탭 상태 관리 (0: 문자, 1: 숫자/기호, 2: 기능 키)
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    
    val TAB_LABELS = listOf("문자", "숫자/기호", "기능")
    val TAG = "KeyboardLayout"
    
    // HID 키 코드 정의 (Boot Protocol 기준, UByte로 타입 변환)
    // 기본 문자 키
    val KEY_A = 0x04.toUByte()
    val KEY_B = 0x05.toUByte()
    val KEY_C = 0x06.toUByte()
    val KEY_D = 0x07.toUByte()
    val KEY_E = 0x08.toUByte()
    val KEY_F = 0x09.toUByte()
    val KEY_G = 0x0A.toUByte()
    val KEY_H = 0x0B.toUByte()
    val KEY_I = 0x0C.toUByte()
    val KEY_J = 0x0D.toUByte()
    val KEY_K = 0x0E.toUByte()
    val KEY_L = 0x0F.toUByte()
    val KEY_M = 0x10.toUByte()
    val KEY_N = 0x11.toUByte()
    val KEY_O = 0x12.toUByte()
    val KEY_P = 0x13.toUByte()
    val KEY_Q = 0x14.toUByte()
    val KEY_R = 0x15.toUByte()
    val KEY_S = 0x16.toUByte()
    val KEY_T = 0x17.toUByte()
    val KEY_U = 0x18.toUByte()
    val KEY_V = 0x19.toUByte()
    val KEY_W = 0x1A.toUByte()
    val KEY_X = 0x1B.toUByte()
    val KEY_Y = 0x1C.toUByte()
    val KEY_Z = 0x1D.toUByte()
    
    // 숫자 키
    val KEY_1 = 0x1E.toUByte()
    val KEY_2 = 0x1F.toUByte()
    val KEY_3 = 0x20.toUByte()
    val KEY_4 = 0x21.toUByte()
    val KEY_5 = 0x22.toUByte()
    val KEY_6 = 0x23.toUByte()
    val KEY_7 = 0x24.toUByte()
    val KEY_8 = 0x25.toUByte()
    val KEY_9 = 0x26.toUByte()
    val KEY_0 = 0x27.toUByte()
    
    // 기능 키
    val KEY_ENTER = 0x28.toUByte()
    val KEY_BACKSPACE = 0x2A.toUByte()
    val KEY_TAB = 0x2B.toUByte()
    val KEY_SPACE = 0x2C.toUByte()
    val KEY_ESC = 0x29.toUByte()
    
    // 화살표 키
    val KEY_LEFT = 0x50.toUByte()
    val KEY_RIGHT = 0x4F.toUByte()
    val KEY_UP = 0x52.toUByte()
    val KEY_DOWN = 0x51.toUByte()
    
    // 수정자 키
    val KEY_SHIFT_LEFT = 0x02.toUByte()  // HID Modifier: Left Shift
    val KEY_CTRL_LEFT = 0x01.toUByte()   // HID Modifier: Left Ctrl
    val KEY_ALT_LEFT = 0x04.toUByte()    // HID Modifier: Left Alt
    
    // 메인 컨테이너
    Box(
        modifier = modifier
            .size(width = 240.dp, height = 280.dp)
            .background(color = Color(0xFF0D0D0D))
            .padding(8.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .size(width = 224.dp, height = 264.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // 탭 바 (상단)
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier
                    .height(36.dp)
                    .width(224.dp),
                containerColor = Color(0xFF1A1A1A),
                contentColor = Color(0xFF2196F3),
                edgePadding = 0.dp,
                divider = {}
            ) {
                TAB_LABELS.forEachIndexed { index, label ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = {
                            selectedTabIndex = index
                            Log.d(TAG, "Switched to tab: $label (index=$index)")
                        },
                        modifier = Modifier
                            .height(36.dp)
                            .padding(horizontal = 4.dp),
                        text = {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    )
                }
            }
            
            // 키보드 콘텐츠 영역 (탭 바 아래)
            Box(
                modifier = Modifier
                    .size(width = 224.dp, height = 228.dp)
                    .background(color = Color(0xFF0D0D0D))
            ) {
                when (selectedTabIndex) {
                    0 -> KeyboardTabCharacters(
                        onKeyPressed = onKeyPressed,
                        onKeyReleased = onKeyReleased,
                        activeKeys = activeKeys
                    )
                    1 -> KeyboardTabSymbols(
                        onKeyPressed = onKeyPressed,
                        onKeyReleased = onKeyReleased,
                        activeKeys = activeKeys
                    )
                    2 -> KeyboardTabFunction(
                        onKeyPressed = onKeyPressed,
                        onKeyReleased = onKeyReleased,
                        activeKeys = activeKeys
                    )
                }
            }
        }
    }
}

/**
 * 탭 0: 문자 키보드
 *
 * 기본 문자 배치 + 수정자 키 (Shift, Ctrl, Alt, Space)
 * 한손 조작 최적화: Shift 좌측 하단, Ctrl 우측 하단
 *
 * **레이아웃**:
 * ```
 * QWERTY 행
 * ASDFGH 행
 * ZXCVBN 행 + Space (중앙)
 * Shift + Ctrl + Alt (하단)
 * ```
 */
@Composable
private fun KeyboardTabCharacters(
    onKeyPressed: (keyCode: UByte) -> Unit,
    onKeyReleased: (keyCode: UByte) -> Unit,
    activeKeys: Set<UByte>
) {
    val KEY_Q = 0x14.toUByte()
    val KEY_W = 0x1A.toUByte()
    val KEY_E = 0x08.toUByte()
    val KEY_R = 0x15.toUByte()
    val KEY_T = 0x17.toUByte()
    val KEY_Y = 0x1C.toUByte()
    val KEY_U = 0x18.toUByte()
    val KEY_I = 0x0C.toUByte()
    val KEY_O = 0x12.toUByte()
    val KEY_P = 0x13.toUByte()
    
    val KEY_A = 0x04.toUByte()
    val KEY_S = 0x16.toUByte()
    val KEY_D = 0x07.toUByte()
    val KEY_F = 0x09.toUByte()
    val KEY_G = 0x0A.toUByte()
    val KEY_H = 0x0B.toUByte()
    val KEY_J = 0x0D.toUByte()
    val KEY_K = 0x0E.toUByte()
    val KEY_L = 0x0F.toUByte()
    
    val KEY_Z = 0x1D.toUByte()
    val KEY_X = 0x1B.toUByte()
    val KEY_C = 0x06.toUByte()
    val KEY_V = 0x19.toUByte()
    val KEY_B = 0x05.toUByte()
    val KEY_N = 0x11.toUByte()
    val KEY_M = 0x10.toUByte()
    
    val KEY_SPACE = 0x2C.toUByte()
    val KEY_SHIFT_LEFT = 0x02.toUByte()
    val KEY_CTRL_LEFT = 0x01.toUByte()
    val KEY_ALT_LEFT = 0x04.toUByte()
    
    // 상태: 수정자 키 활성화 추적
    var shiftActive by remember { mutableStateOf(false) }
    var ctrlActive by remember { mutableStateOf(false) }
    var altActive by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .size(224.dp, 228.dp)
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Row 1: Q W E R T Y U I O P
        Row(
            modifier = Modifier
                .height(50.dp)
                .width(220.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            listOf(
                "Q" to KEY_Q, "W" to KEY_W, "E" to KEY_E, "R" to KEY_R,
                "T" to KEY_T, "Y" to KEY_Y, "U" to KEY_U, "I" to KEY_I,
                "O" to KEY_O, "P" to KEY_P
            ).forEach { (label, keyCode) ->
                KeyboardKeyButton(
                    keyLabel = label,
                    keyCode = keyCode,
                    isActive = keyCode in activeKeys,
                    onKeyPressed = { onKeyPressed(keyCode) },
                    onKeyReleased = { onKeyReleased(keyCode) },
                    modifier = Modifier.size(20.dp, 50.dp)
                )
            }
        }
        
        // Row 2: A S D F G H J K L
        Row(
            modifier = Modifier
                .height(50.dp)
                .width(220.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            listOf(
                "A" to KEY_A, "S" to KEY_S, "D" to KEY_D, "F" to KEY_F,
                "G" to KEY_G, "H" to KEY_H, "J" to KEY_J, "K" to KEY_K,
                "L" to KEY_L
            ).forEach { (label, keyCode) ->
                KeyboardKeyButton(
                    keyLabel = label,
                    keyCode = keyCode,
                    isActive = keyCode in activeKeys,
                    onKeyPressed = { onKeyPressed(keyCode) },
                    onKeyReleased = { onKeyReleased(keyCode) },
                    modifier = Modifier.size(22.dp, 50.dp)
                )
            }
        }
        
        // Row 3: Z X C V B N M (7개, 균등 배치)
        Row(
            modifier = Modifier
                .height(50.dp)
                .width(220.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            listOf(
                "Z" to KEY_Z, "X" to KEY_X, "C" to KEY_C, "V" to KEY_V,
                "B" to KEY_B, "N" to KEY_N, "M" to KEY_M
            ).forEach { (label, keyCode) ->
                KeyboardKeyButton(
                    keyLabel = label,
                    keyCode = keyCode,
                    isActive = keyCode in activeKeys,
                    onKeyPressed = { onKeyPressed(keyCode) },
                    onKeyReleased = { onKeyReleased(keyCode) },
                    modifier = Modifier.size(30.dp, 50.dp)
                )
            }
        }
        
        // Row 4: Space (중앙)
        Row(
            modifier = Modifier
                .height(50.dp)
                .width(220.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            KeyboardKeyButton(
                keyLabel = "Space",
                keyCode = KEY_SPACE,
                isActive = KEY_SPACE in activeKeys,
                onKeyPressed = { onKeyPressed(KEY_SPACE) },
                onKeyReleased = { onKeyReleased(KEY_SPACE) },
                modifier = Modifier.size(160.dp, 50.dp)
            )
        }
        
        // Row 5: Shift (좌측) + Alt (중앙) + Ctrl (우측)
        Row(
            modifier = Modifier
                .height(50.dp)
                .width(220.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            KeyboardKeyButton(
                keyLabel = "Shift",
                keyCode = KEY_SHIFT_LEFT,
                isActive = shiftActive || KEY_SHIFT_LEFT in activeKeys,
                onKeyPressed = {
                    shiftActive = true
                    onKeyPressed(it)
                },
                onKeyReleased = {
                    shiftActive = false
                    onKeyReleased(it)
                },
                modifier = Modifier.size(50.dp, 50.dp)
            )
            
            KeyboardKeyButton(
                keyLabel = "Alt",
                keyCode = KEY_ALT_LEFT,
                isActive = altActive || KEY_ALT_LEFT in activeKeys,
                onKeyPressed = {
                    altActive = true
                    onKeyPressed(it)
                },
                onKeyReleased = {
                    altActive = false
                    onKeyReleased(it)
                },
                modifier = Modifier.size(50.dp, 50.dp)
            )
            
            KeyboardKeyButton(
                keyLabel = "Ctrl",
                keyCode = KEY_CTRL_LEFT,
                isActive = ctrlActive || KEY_CTRL_LEFT in activeKeys,
                onKeyPressed = {
                    ctrlActive = true
                    onKeyPressed(it)
                },
                onKeyReleased = {
                    ctrlActive = false
                    onKeyReleased(it)
                },
                modifier = Modifier.size(50.dp, 50.dp)
            )
        }
    }
}

/**
 * 탭 1: 숫자/기호 키보드
 *
 * 숫자 (0-9) 및 기본 기호 키
 *
 * **레이아웃**:
 * ```
 * 1 2 3 4 5 6 7 8 9 0
 * ! @ # $ % ^ & * ( )
 * - = [ ] ; ' , . / \
 * Backspace + Tab
 * ```
 */
@Composable
private fun KeyboardTabSymbols(
    onKeyPressed: (keyCode: UByte) -> Unit,
    onKeyReleased: (keyCode: UByte) -> Unit,
    activeKeys: Set<UByte>
) {
    val KEY_1 = 0x1E.toUByte()
    val KEY_2 = 0x1F.toUByte()
    val KEY_3 = 0x20.toUByte()
    val KEY_4 = 0x21.toUByte()
    val KEY_5 = 0x22.toUByte()
    val KEY_6 = 0x23.toUByte()
    val KEY_7 = 0x24.toUByte()
    val KEY_8 = 0x25.toUByte()
    val KEY_9 = 0x26.toUByte()
    val KEY_0 = 0x27.toUByte()
    
    val KEY_MINUS = 0x2D.toUByte()
    val KEY_EQUAL = 0x2E.toUByte()
    val KEY_LEFTBRACE = 0x2F.toUByte()
    val KEY_RIGHTBRACE = 0x30.toUByte()
    val KEY_SEMICOLON = 0x33.toUByte()
    val KEY_APOSTROPHE = 0x34.toUByte()
    val KEY_COMMA = 0x36.toUByte()
    val KEY_DOT = 0x37.toUByte()
    val KEY_SLASH = 0x38.toUByte()
    val KEY_BACKSLASH = 0x31.toUByte()
    
    val KEY_BACKSPACE = 0x2A.toUByte()
    val KEY_TAB = 0x2B.toUByte()
    
    Column(
        modifier = Modifier
            .size(224.dp, 228.dp)
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Row 1: 1 2 3 4 5 6 7 8 9 0
        Row(
            modifier = Modifier
                .height(50.dp)
                .width(220.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            listOf(
                "1" to KEY_1, "2" to KEY_2, "3" to KEY_3, "4" to KEY_4,
                "5" to KEY_5, "6" to KEY_6, "7" to KEY_7, "8" to KEY_8,
                "9" to KEY_9, "0" to KEY_0
            ).forEach { (label, keyCode) ->
                KeyboardKeyButton(
                    keyLabel = label,
                    keyCode = keyCode,
                    isActive = keyCode in activeKeys,
                    onKeyPressed = { onKeyPressed(keyCode) },
                    onKeyReleased = { onKeyReleased(keyCode) },
                    modifier = Modifier.size(20.dp, 50.dp)
                )
            }
        }
        
        // Row 2: - = [ ] \ ; ' , . /
        Row(
            modifier = Modifier
                .height(50.dp)
                .width(220.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            listOf(
                "-" to KEY_MINUS, "=" to KEY_EQUAL, "[" to KEY_LEFTBRACE,
                "]" to KEY_RIGHTBRACE, "\\" to KEY_BACKSLASH, ";" to KEY_SEMICOLON,
                "'" to KEY_APOSTROPHE, "," to KEY_COMMA, "." to KEY_DOT,
                "/" to KEY_SLASH
            ).forEach { (label, keyCode) ->
                KeyboardKeyButton(
                    keyLabel = label,
                    keyCode = keyCode,
                    isActive = keyCode in activeKeys,
                    onKeyPressed = { onKeyPressed(keyCode) },
                    onKeyReleased = { onKeyReleased(keyCode) },
                    modifier = Modifier.size(20.dp, 50.dp)
                )
            }
        }
        
        // Row 3: Backspace (길이 120dp) + Tab (길이 100dp)
        Row(
            modifier = Modifier
                .height(50.dp)
                .width(220.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            KeyboardKeyButton(
                keyLabel = "BkSp",
                keyCode = KEY_BACKSPACE,
                isActive = KEY_BACKSPACE in activeKeys,
                onKeyPressed = { onKeyPressed(KEY_BACKSPACE) },
                onKeyReleased = { onKeyReleased(KEY_BACKSPACE) },
                modifier = Modifier.size(120.dp, 50.dp)
            )
            
            Spacer(modifier = Modifier.width(0.dp))
            
            KeyboardKeyButton(
                keyLabel = "Tab",
                keyCode = KEY_TAB,
                isActive = KEY_TAB in activeKeys,
                onKeyPressed = { onKeyPressed(KEY_TAB) },
                onKeyReleased = { onKeyReleased(KEY_TAB) },
                modifier = Modifier.size(98.dp, 50.dp)
            )
        }
        
        // Row 4: 빈 공간 (UI 균형)
        Spacer(modifier = Modifier.height(68.dp))
    }
}

/**
 * 탭 2: 기능 키 키보드
 *
 * 화살표 키 + 기능 키 (F1~F12는 제한으로 기본 기능 키만 포함)
 *
 * **레이아웃**:
 * ```
 *       ↑
 * ← ↓ →
 * Enter Esc
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
    
    Column(
        modifier = Modifier
            .size(224.dp, 228.dp)
            .padding(4.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Row 1: Up Arrow (중앙)
        Row(
            modifier = Modifier
                .height(60.dp)
                .width(220.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            KeyboardKeyButton(
                keyLabel = "↑",
                keyCode = KEY_UP,
                isActive = KEY_UP in activeKeys,
                onKeyPressed = { onKeyPressed(KEY_UP) },
                onKeyReleased = { onKeyReleased(KEY_UP) },
                modifier = Modifier.size(60.dp, 60.dp)
            )
        }
        
        // Row 2: Left, Down, Right
        Row(
            modifier = Modifier
                .height(60.dp)
                .width(220.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            KeyboardKeyButton(
                keyLabel = "←",
                keyCode = KEY_LEFT,
                isActive = KEY_LEFT in activeKeys,
                onKeyPressed = { onKeyPressed(KEY_LEFT) },
                onKeyReleased = { onKeyReleased(KEY_LEFT) },
                modifier = Modifier.size(60.dp, 60.dp)
            )
            
            Spacer(modifier = Modifier.width(4.dp))
            
            KeyboardKeyButton(
                keyLabel = "↓",
                keyCode = KEY_DOWN,
                isActive = KEY_DOWN in activeKeys,
                onKeyPressed = { onKeyPressed(KEY_DOWN) },
                onKeyReleased = { onKeyReleased(KEY_DOWN) },
                modifier = Modifier.size(60.dp, 60.dp)
            )
            
            Spacer(modifier = Modifier.width(4.dp))
            
            KeyboardKeyButton(
                keyLabel = "→",
                keyCode = KEY_RIGHT,
                isActive = KEY_RIGHT in activeKeys,
                onKeyPressed = { onKeyPressed(KEY_RIGHT) },
                onKeyReleased = { onKeyReleased(KEY_RIGHT) },
                modifier = Modifier.size(60.dp, 60.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Row 3: Enter + Esc
        Row(
            modifier = Modifier
                .height(60.dp)
                .width(220.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            KeyboardKeyButton(
                keyLabel = "Enter",
                keyCode = KEY_ENTER,
                isActive = KEY_ENTER in activeKeys,
                onKeyPressed = { onKeyPressed(KEY_ENTER) },
                onKeyReleased = { onKeyReleased(KEY_ENTER) },
                modifier = Modifier.size(106.dp, 60.dp)
            )
            
            Spacer(modifier = Modifier.width(2.dp))
            
            KeyboardKeyButton(
                keyLabel = "Esc",
                keyCode = KEY_ESC,
                isActive = KEY_ESC in activeKeys,
                onKeyPressed = { onKeyPressed(KEY_ESC) },
                onKeyReleased = { onKeyReleased(KEY_ESC) },
                modifier = Modifier.size(106.dp, 60.dp)
            )
        }
    }
}

/**
 * KeyboardLayout Preview
 *
 * 키보드 레이아웃의 다양한 탭을 시각적으로 확인하기 위한 Preview입니다.
 */
@Preview(
    showBackground = true,
    backgroundColor = 0xFF0D0D0D,
    widthDp = 240,
    heightDp = 280
)
@Composable
fun KeyboardLayoutPreview() {
    KeyboardLayout(
        onKeyPressed = { keyCode ->
            Log.d("KeyboardLayout", "Key pressed: 0x${keyCode.toString(16)}")
        },
        onKeyReleased = { keyCode ->
            Log.d("KeyboardLayout", "Key released: 0x${keyCode.toString(16)}")
        },
        activeKeys = emptySet()
    )
}

