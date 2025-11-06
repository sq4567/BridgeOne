package com.bridgeone.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bridgeone.app.R
import com.bridgeone.app.ui.theme.BridgeOneTheme
import com.bridgeone.app.ui.theme.TextPrimary
import com.bridgeone.app.ui.components.TouchpadWrapper
import com.bridgeone.app.ui.components.KeyboardLayout

/**
 * BridgeOne 앱의 최상위 Composable 함수입니다.
 *
 * 이 함수는 앱의 전체 레이아웃과 테마를 정의합니다.
 * Material3 테마와 Pretendard 폰트가 적용되며, 다크 테마만 지원합니다.
 */
@Composable
fun BridgeOneApp() {
    // 전체 화면을 채우는 배경
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        // 메인 콘텐츠
        MainContent()
    }
}

/**
 * 앱의 메인 콘텐츠를 렌더링합니다.
 *
 * 중앙 하단 영역에 터치패드(160×320dp)와 키보드(240×280dp)를 배치합니다.
 * 터치패드는 마우스 입력, 키보드는 키 입력을 처리합니다.
 */
@Composable
private fun MainContent() {
    // 활성 키 상태 관리 (키보드의 다중 입력 시각화용)
    val activeKeys = remember { mutableStateOf(setOf<UByte>()) }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 메인 입력 영역: 터치패드 (좌측) + 키보드 (우측) 구성
        // 중앙 하단 배치: 가로 410dp (160 + 250) × 세로 320dp
        Row(
            modifier = Modifier,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // 터치패드 (좌측): 160×320dp
            TouchpadWrapper(
                modifier = Modifier
            )
            
            // 키보드 (우측): 240×280dp
            KeyboardLayout(
                onKeyPressed = { keyCode ->
                    activeKeys.value = activeKeys.value + keyCode
                },
                onKeyReleased = { keyCode ->
                    activeKeys.value = activeKeys.value - keyCode
                },
                activeKeys = activeKeys.value
            )
        }
        
        // 하단 여백
        Spacer(modifier = Modifier.size(16.dp))
    }
}


@Preview(showBackground = true)
@Composable
private fun BridgeOneAppPreview() {
    BridgeOneTheme {
        BridgeOneApp()
    }
}
