package com.chatterbones.bridgeone

import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chatterbones.bridgeone.protocol.BridgeFrame
import com.chatterbones.bridgeone.protocol.SequenceCounter
import com.chatterbones.bridgeone.ui.theme.BridgeOneTheme
import com.chatterbones.bridgeone.usb.UsbConnectionManager
import com.chatterbones.bridgeone.usb.UsbConnectionState
import kotlinx.coroutines.delay

/**
 * BridgeOne 메인 액티비티
 * 
 * USB 연결 관리 및 앱의 주요 UI를 담당합니다.
 * Phase 1.1.3.1: 통신 레이어 통합 및 터치패드 UI 구현
 * 
 * @see [technical-specification-app.md §1.1.2 연결 관리 요구사항]
 * @see [development-plan-checklist.md Phase 1.1.3.1]
 */
class MainActivity : ComponentActivity() {
    
    // USB 연결 관리자
    private lateinit var usbConnectionManager: UsbConnectionManager
    
    // 순번 카운터
    private val sequenceCounter = SequenceCounter()
    
    // Vibrator 서비스 (햅틱 피드백용)
    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // USB 연결 관리자 초기화
        usbConnectionManager = UsbConnectionManager(applicationContext)
        usbConnectionManager.initialize()
        
        enableEdgeToEdge()
        setContent {
            BridgeOneTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        usbConnectionManager = usbConnectionManager,
                        sequenceCounter = sequenceCounter,
                        vibrator = vibrator,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // USB 연결 관리자 정리
        usbConnectionManager.cleanup()
    }
}

/**
 * 메인 화면 Composable
 * 
 * Phase 1.1.3.1 요구사항에 따라 다음을 포함합니다:
 * - 상단: USB 연결 상태 표시
 * - 중앙: 터치패드 영역 (80% 높이, 테두리)
 * - 하단: 통계 정보 (전송 프레임 수, 순번, 초당 전송률)
 * 
 * @param usbConnectionManager USB 연결 관리자
 * @param sequenceCounter 순번 카운터
 * @param vibrator 햅틱 피드백용 Vibrator
 * @param modifier Compose Modifier
 */
@Composable
fun MainScreen(
    usbConnectionManager: UsbConnectionManager,
    sequenceCounter: SequenceCounter,
    vibrator: Vibrator,
    modifier: Modifier = Modifier
) {
    // USB 연결 상태 구독
    val connectionState by usbConnectionManager.connectionState.collectAsState()
    
    // 통계 상태
    var totalFramesSent by remember { mutableLongStateOf(0L) }
    var currentSequence by remember { mutableLongStateOf(0L) }
    var framesPerSecond by remember { mutableFloatStateOf(0f) }
    
    // 1초마다 통계 업데이트
    LaunchedEffect(Unit) {
        var lastFrameCount = 0L
        while (true) {
            delay(16L)
            val currentFrameCount = sequenceCounter.sentCount
            framesPerSecond = (currentFrameCount - lastFrameCount).toFloat()
            lastFrameCount = currentFrameCount
            totalFramesSent = currentFrameCount
            currentSequence = sequenceCounter.currentSequence.toLong()
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 상단: USB 연결 상태 표시
        ConnectionStatusCard(connectionState)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 중앙: 터치패드 영역 (80% 높이)
        TouchpadArea(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .weight(1f),
            usbConnectionManager = usbConnectionManager,
            sequenceCounter = sequenceCounter,
            vibrator = vibrator,
            isConnected = connectionState is UsbConnectionState.Connected
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 하단: 통계 정보
        StatisticsCard(
            totalFrames = totalFramesSent,
            currentSequence = currentSequence,
            framesPerSecond = framesPerSecond
        )
    }
}

/**
 * USB 연결 상태를 표시하는 카드
 * 
 * @param connectionState 현재 USB 연결 상태
 */
@Composable
fun ConnectionStatusCard(connectionState: UsbConnectionState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (connectionState) {
                is UsbConnectionState.Connected -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                is UsbConnectionState.Error -> Color(0xFFF44336).copy(alpha = 0.1f)
                is UsbConnectionState.Requesting -> Color(0xFFFF9800).copy(alpha = 0.1f)
                is UsbConnectionState.Disconnected -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = when (connectionState) {
                    is UsbConnectionState.Disconnected -> "🔌 USB 연결 대기 중"
                    is UsbConnectionState.Requesting -> "⏳ USB 권한 요청 중..."
                    is UsbConnectionState.Connected -> "✅ 연결됨"
                    is UsbConnectionState.Error -> "❌ 연결 오류"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            if (connectionState is UsbConnectionState.Connected) {
                Text(
                    text = connectionState.deviceName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            } else if (connectionState is UsbConnectionState.Error) {
                Text(
                    text = connectionState.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * 터치패드 영역 Composable
 * 
 * pointerInput 모디파이어를 사용하여 터치 이벤트를 감지하고
 * BridgeFrame으로 변환하여 USB를 통해 전송합니다.
 * 
 * @param modifier Compose Modifier
 * @param usbConnectionManager USB 연결 관리자
 * @param sequenceCounter 순번 카운터
 * @param vibrator 햅틱 피드백용 Vibrator
 * @param isConnected USB 연결 여부
 */
@Composable
fun TouchpadArea(
    modifier: Modifier = Modifier,
    usbConnectionManager: UsbConnectionManager,
    sequenceCounter: SequenceCounter,
    vibrator: Vibrator,
    isConnected: Boolean
) {
    
    // 이전 터치 좌표 저장 (델타 계산용)
    var previousPosition by remember { mutableStateOf(0f to 0f) }
    
    Box(
        modifier = modifier
            .border(
                width = 3.dp,
                color = if (isConnected) MaterialTheme.colorScheme.primary else Color.Gray,
                shape = RoundedCornerShape(16.dp)
            )
            .background(
                color = if (isConnected) 
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f) 
                else 
                    Color.Gray.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp)
            )
            .pointerInput(isConnected) {
                if (!isConnected) return@pointerInput
                
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: continue
                        
                        when {
                            // 터치 시작 (ACTION_DOWN)
                            change.pressed && change.previousPressed.not() -> {
                                previousPosition = change.position.x to change.position.y
                                
                                // 햅틱 피드백 (10ms 짧은 진동)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    vibrator.vibrate(
                                        VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE)
                                    )
                                } else {
                                    @Suppress("DEPRECATION")
                                    vibrator.vibrate(10)
                                }
                                
                                // 좌클릭 버튼 활성화 프레임 전송 (이동 없음)
                                val frame = BridgeFrame(
                                    seq = sequenceCounter.next(),
                                    buttons = BridgeFrame.BUTTON_LEFT,
                                    deltaX = 0,
                                    deltaY = 0,
                                    wheel = 0,
                                    modifiers = 0u,
                                    keyCode1 = 0u,
                                    keyCode2 = 0u
                                )
                                usbConnectionManager.sendFrame(frame)
                                
                                change.consume()
                            }
                            
                            // 터치 이동 (ACTION_MOVE)
                            change.pressed && change.previousPressed -> {
                                val currentPos = change.position
                                val (prevX, prevY) = previousPosition
                                
                                // 델타 계산
                                val rawDeltaX = (currentPos.x - prevX).toInt()
                                val rawDeltaY = (currentPos.y - prevY).toInt()
                                
                                // -127~127 범위로 클램핑
                                val clampedDeltaX = rawDeltaX.coerceIn(-127, 127).toByte()
                                val clampedDeltaY = rawDeltaY.coerceIn(-127, 127).toByte()
                                
                                // 델타가 0이 아닌 경우에만 전송
                                if (clampedDeltaX != 0.toByte() || clampedDeltaY != 0.toByte()) {
                                    // 좌클릭 유지하면서 이동 프레임 전송
                                    val frame = BridgeFrame(
                                        seq = sequenceCounter.next(),
                                        buttons = BridgeFrame.BUTTON_LEFT,
                                        deltaX = clampedDeltaX,
                                        deltaY = clampedDeltaY,
                                        wheel = 0,
                                        modifiers = 0u,
                                        keyCode1 = 0u,
                                        keyCode2 = 0u
                                    )
                                    usbConnectionManager.sendFrame(frame)
                                    
                                    previousPosition = currentPos.x to currentPos.y
                                }
                                
                                change.consume()
                            }
                            
                            // 터치 종료 (ACTION_UP)
                            change.pressed.not() && change.previousPressed -> {
                                // 좌클릭 버튼 해제 프레임 전송
                                val frame = BridgeFrame(
                                    seq = sequenceCounter.next(),
                                    buttons = 0u,
                                    deltaX = 0,
                                    deltaY = 0,
                                    wheel = 0,
                                    modifiers = 0u,
                                    keyCode1 = 0u,
                                    keyCode2 = 0u
                                )
                                usbConnectionManager.sendFrame(frame)
                                
                                change.consume()
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isConnected) "터치패드 영역\n여기를 터치하여 조작하세요" else "USB를 연결해주세요",
            style = MaterialTheme.typography.bodyLarge,
            color = if (isConnected) 
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) 
            else 
                Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 통계 정보를 표시하는 카드
 * 
 * @param totalFrames 전송된 총 프레임 수
 * @param currentSequence 현재 순번
 * @param framesPerSecond 초당 전송 프레임 수
 */
@Composable
fun StatisticsCard(
    totalFrames: Long,
    currentSequence: Long,
    framesPerSecond: Float
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "📊 전송 통계",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatisticsItem(label = "총 전송", value = totalFrames.toString())
                StatisticsItem(label = "현재 순번", value = currentSequence.toString())
                StatisticsItem(label = "초당 전송", value = "${framesPerSecond.toInt()} fps")
            }
        }
    }
}

/**
 * 개별 통계 항목 Composable
 * 
 * @param label 항목 이름
 * @param value 항목 값
 */
@Composable
fun StatisticsItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    BridgeOneTheme {
        Text("BridgeOne Preview")
    }
}