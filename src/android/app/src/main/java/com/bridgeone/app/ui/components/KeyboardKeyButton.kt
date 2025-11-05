package com.bridgeone.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bridgeone.app.ui.utils.ClickDetector
import android.util.Log

/**
 * KeyboardKeyButton 컴포넌트
 *
 * 단일 키 입력을 담당하는 버튼으로, 탭 시 KEY_DOWN/KEY_UP 신호를 전송합니다.
 * Sticky Hold 기능을 지원하여 길게 누르기로 키를 유지할 수 있습니다.
 *
 * **기능**:
 * - 기본 탭: KEY_DOWN → KEY_UP 즉시 전송
 * - 롱프레스 (500ms): Sticky Hold 모드 진입, 다음 탭에서 해제
 * - 시각적 피드백: 누르기 상태에 따른 배경색/테두리 변화
 * - 접근성: 60×60dp 터치 영역, 14sp 폰트, WCAG AA 색상 대비
 *
 * **상태 관리**:
 * - isPressed: 현재 누르기 상태 (InteractionSource 기반)
 * - isStickyLatched: Sticky Hold 활성 상태
 * - isEnabled: 버튼 사용 가능 여부
 *
 * **색상 시스템**:
 * - 활성화 기본: #2196F3 (Material Blue 500)
 * - 누르기/Sticky Hold: #1976D2 (Material Blue 600)
 * - 비활성화: #C2C2C2 (Gray 300)
 * - 텍스트: #121212 (Near Black)
 * - 테두리 (Sticky Hold): #FF9800 (Orange 500)
 *
 * @param keyLabel 버튼에 표시될 키 레이블 (예: "Shift", "Ctrl", "A")
 * @param keyCode HID 키 코드 (0x00~0xFF)
 * @param modifier 외부에서 추가할 수 있는 Modifier
 * @param isEnabled 버튼 사용 가능 여부 (기본값: true)
 * @param onKeyPressed 키 누르기 시 호출되는 콜백
 * @param onKeyReleased 키 해제 시 호출되는 콜백
 */
@Composable
fun KeyboardKeyButton(
    keyLabel: String,
    keyCode: UByte,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    isActive: Boolean = false,
    onKeyPressed: ((keyCode: UByte) -> Unit)? = null,
    onKeyReleased: ((keyCode: UByte) -> Unit)? = null
) {
    // 상태 관리
    var isStickyLatched by remember { mutableStateOf(false) }
    var longPressElapsedTime by remember { mutableStateOf(0L) }
    var longPressStartTime by remember { mutableStateOf(0L) }
    var stickyHoldProgressInternal by remember { mutableStateOf(0f) }
    
    // InteractionSource로 누르기 상태 추적 (공식 Jetpack Compose Context7 패턴)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Sticky Hold 임계시간 (500ms)
    val STICKY_HOLD_THRESHOLD_MS = 500L
    
    // Sticky Hold 진행률 애니메이션 (0.0 ~ 1.0)
    val stickyHoldProgress by animateFloatAsState(
        targetValue = if (isPressed && !isStickyLatched) (stickyHoldProgressInternal) else 0f,
        animationSpec = tween(durationMillis = STICKY_HOLD_THRESHOLD_MS.toInt()),
        label = "StickyHoldProgress"
    )
    
    // Sticky Hold 진행 타이머 (롱프레스 감지)
    LaunchedEffect(isPressed) {
        if (isPressed && !isStickyLatched) {
            // 롱프레스 시작
            longPressStartTime = System.currentTimeMillis()
            val startTime = longPressStartTime
            
            while (isPressed && System.currentTimeMillis() - startTime < STICKY_HOLD_THRESHOLD_MS) {
                val elapsed = System.currentTimeMillis() - startTime
                stickyHoldProgressInternal = (elapsed.toFloat() / STICKY_HOLD_THRESHOLD_MS).coerceIn(0f, 1f)
                kotlinx.coroutines.delay(16L)  // ~60fps 업데이트
            }
            
            if (isPressed && System.currentTimeMillis() - startTime >= STICKY_HOLD_THRESHOLD_MS) {
                // Sticky Hold 활성화
                isStickyLatched = true
                onKeyPressed?.invoke(keyCode)  // KEY_DOWN 전송
                Log.d("KeyboardKeyButton", "Sticky Hold latched for key=$keyLabel (code=0x${keyCode.toString(16)})")
            }
            
            stickyHoldProgressInternal = 0f
        } else if (!isPressed) {
            stickyHoldProgressInternal = 0f
            longPressStartTime = 0L
        }
    }
    
    // 색상 애니메이션 (누르기 상태에 따른 부드러운 색상 전이)
    val backgroundColor by animateColorAsState(
        targetValue = when {
            !isEnabled -> Color(0xFFC2C2C2)  // 비활성화: Gray 300
            isStickyLatched -> Color(0xFF1976D2)  // Sticky Hold: Material Blue 600
            isPressed -> Color(0xFF1976D2)  // 누르기: Material Blue 600
            isActive -> Color(0xFF42A5F5)  // 다중 입력 활성화: Light Blue 400
            else -> Color(0xFF2196F3)  // 기본: Material Blue 500
        },
        label = "KeyboardKeyButtonBackgroundColor"
    )
    
    // 테두리 색상 및 너비 (Sticky Hold 상태)
    val borderColor = if (isStickyLatched) Color(0xFFFF9800) else Color.Transparent
    val borderWidth = if (isStickyLatched) 3.dp else 0.dp
    
    val TAG = "KeyboardKeyButton"
    
    Box(
        modifier = modifier
            .size(60.dp, 60.dp)  // 접근성: 최소 터치 영역 (60×60dp, Android 권장 48×48 초과)
            .border(
                width = borderWidth,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(8.dp)
            )
            .drawWithContent {
                // Fill 애니메이션: 좌측에서 우측으로 채우기 (Sticky Hold 진행률)
                drawContent()
                
                if (stickyHoldProgress > 0f && !isStickyLatched) {
                    // 진행 중인 Sticky Hold 시각화 (반투명 오버레이)
                    val fillWidth = size.width * stickyHoldProgress
                    drawRect(
                        color = Color(0xFF1976D2).copy(alpha = 0.3f),
                        topLeft = androidx.compose.ui.geometry.Offset.Zero,
                        size = androidx.compose.ui.geometry.Size(fillWidth, size.height)
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // 버튼의 눌리는 영역과 시각적 표현을 분리 (Material Design 패턴)
        Button(
            onClick = {
                // Sticky Hold 상태 토글
                if (isStickyLatched) {
                    // Sticky Hold 해제: KEY_UP 전송
                    onKeyReleased?.invoke(keyCode)
                    isStickyLatched = false
                    longPressStartTime = 0L
                    Log.d(TAG, "Sticky Hold released for key=$keyLabel (code=0x${keyCode.toString(16)})")
                } else {
                    // 일반 탭 (즉시 KEY_DOWN/UP 전송)
                    onKeyPressed?.invoke(keyCode)
                    onKeyReleased?.invoke(keyCode)
                    Log.d(TAG, "Key tapped for key=$keyLabel (code=0x${keyCode.toString(16)})")
                }
                stickyHoldProgressInternal = 0f
            },
            modifier = Modifier
                .size(60.dp, 60.dp)
                .background(
                    color = Color.Transparent,
                    shape = RoundedCornerShape(8.dp)
                ),
            enabled = isEnabled,
            interactionSource = interactionSource,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = Color(0xFF121212),  // Text color: Near Black (WCAG AA 대비)
                disabledContainerColor = Color.Transparent,
                disabledContentColor = Color(0xFF666666)  // Gray 600 (비활성화)
            ),
            shape = RoundedCornerShape(8.dp),
            elevation = ButtonDefaults.elevatedButtonElevation(
                defaultElevation = 2.dp,
                pressedElevation = 4.dp
            )
        ) {
            // 이 영역은 Button의 내용 영역으로, onClick과 함께 처리됨
            // 실제 텍스트와 시각적 피드백은 외부 Box에서 처리
        }
        
        // 텍스트 (버튼 위에 오버레이)
        Text(
            text = keyLabel,
            fontSize = 14.sp,  // 접근성: 최소 14sp
            fontWeight = FontWeight.Medium,
            color = when {
                !isEnabled -> Color(0xFF666666)  // 비활성화: Gray 600
                isStickyLatched || isPressed || isActive -> Color.White  // 활성화: 흰색 (WCAG AAA 대비)
                else -> Color(0xFF121212)  // 기본: Near Black (WCAG AA 대비)
            },
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

/**
 * KeyboardKeyButton Preview
 *
 * 다양한 상태의 키보드 버튼을 시각적으로 확인하기 위한 Preview입니다.
 * - 기본 상태
 * - 누르기 상태 (시뮬레이션)
 * - Sticky Hold 상태
 * - 비활성화 상태
 */
@Preview(
    showBackground = true,
    backgroundColor = 0xFF0D0D0D,
    widthDp = 480,
    heightDp = 400
)
@Composable
fun KeyboardKeyButtonPreview() {
    Box(
        modifier = Modifier
            .size(480.dp, 400.dp)
            .background(Color(0xFF0D0D0D)),
        contentAlignment = Alignment.TopCenter
    ) {
        // 다양한 상태의 버튼들을 그리드로 배치
        Column(
            modifier = Modifier
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 섹션 1: 기본 상태
            Column(
                modifier = Modifier,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "기본 상태 (Enabled)",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KeyboardKeyButton(
                        keyLabel = "Shift",
                        keyCode = 0x02u,
                        isEnabled = true,
                        isActive = false,
                        onKeyPressed = { Log.d("Preview", "Shift pressed") },
                        onKeyReleased = { Log.d("Preview", "Shift released") }
                    )
                    KeyboardKeyButton(
                        keyLabel = "Ctrl",
                        keyCode = 0x01u,
                        isEnabled = true,
                        isActive = false,
                        onKeyPressed = { Log.d("Preview", "Ctrl pressed") },
                        onKeyReleased = { Log.d("Preview", "Ctrl released") }
                    )
                    KeyboardKeyButton(
                        keyLabel = "Alt",
                        keyCode = 0x04u,
                        isEnabled = true,
                        isActive = false,
                        onKeyPressed = { Log.d("Preview", "Alt pressed") },
                        onKeyReleased = { Log.d("Preview", "Alt released") }
                    )
                }
            }
            
            // 섹션 2: 다중 입력 활성화 상태
            Column(
                modifier = Modifier,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "다중 입력 활성화 (isActive=true)",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KeyboardKeyButton(
                        keyLabel = "A",
                        keyCode = 0x04u,
                        isEnabled = true,
                        isActive = true,  // 다중 입력 활성화
                        onKeyPressed = { Log.d("Preview", "A pressed") },
                        onKeyReleased = { Log.d("Preview", "A released") }
                    )
                    KeyboardKeyButton(
                        keyLabel = "B",
                        keyCode = 0x05u,
                        isEnabled = true,
                        isActive = true,  // 다중 입력 활성화
                        onKeyPressed = { Log.d("Preview", "B pressed") },
                        onKeyReleased = { Log.d("Preview", "B released") }
                    )
                    KeyboardKeyButton(
                        keyLabel = "C",
                        keyCode = 0x06u,
                        isEnabled = true,
                        isActive = true,  // 다중 입력 활성화
                        onKeyPressed = { Log.d("Preview", "C pressed") },
                        onKeyReleased = { Log.d("Preview", "C released") }
                    )
                }
            }
            
            // 섹션 3: 비활성화 상태
            Column(
                modifier = Modifier,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "비활성화 (Disabled)",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KeyboardKeyButton(
                        keyLabel = "↑",
                        keyCode = 0x52u,
                        isEnabled = false,  // 비활성화
                        isActive = false,
                        onKeyPressed = { Log.d("Preview", "Up pressed") },
                        onKeyReleased = { Log.d("Preview", "Up released") }
                    )
                    KeyboardKeyButton(
                        keyLabel = "↓",
                        keyCode = 0x51u,
                        isEnabled = false,  // 비활성화
                        isActive = false,
                        onKeyPressed = { Log.d("Preview", "Down pressed") },
                        onKeyReleased = { Log.d("Preview", "Down released") }
                    )
                    KeyboardKeyButton(
                        keyLabel = "Space",
                        keyCode = 0x2Cu,
                        isEnabled = false,  // 비활성화
                        isActive = false,
                        onKeyPressed = { Log.d("Preview", "Space pressed") },
                        onKeyReleased = { Log.d("Preview", "Space released") }
                    )
                }
            }
            
            // 섹션 4: 특수 키
            Column(
                modifier = Modifier,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "특수 키 (Enter, Escape, Backspace)",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KeyboardKeyButton(
                        keyLabel = "Ent",
                        keyCode = 0x28u,
                        isEnabled = true,
                        isActive = false,
                        onKeyPressed = { Log.d("Preview", "Enter pressed") },
                        onKeyReleased = { Log.d("Preview", "Enter released") }
                    )
                    KeyboardKeyButton(
                        keyLabel = "Esc",
                        keyCode = 0x29u,
                        isEnabled = true,
                        isActive = false,
                        onKeyPressed = { Log.d("Preview", "Escape pressed") },
                        onKeyReleased = { Log.d("Preview", "Escape released") }
                    )
                    KeyboardKeyButton(
                        keyLabel = "⌫",
                        keyCode = 0x2Au,
                        isEnabled = true,
                        isActive = false,
                        onKeyPressed = { Log.d("Preview", "Backspace pressed") },
                        onKeyReleased = { Log.d("Preview", "Backspace released") }
                    )
                }
            }
        }
    }
}

