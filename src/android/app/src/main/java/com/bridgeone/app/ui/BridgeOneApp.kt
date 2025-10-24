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
 * BridgeOne - Hello World 텍스트와 테스트 아이콘을 중앙에 배치합니다.
 */
@Composable
private fun MainContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        // Hello World 텍스트 (Pretendard 폰트 사용)
        Text(
            text = "BridgeOne - Hello World",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.size(32.dp))

        // 테스트 아이콘 행
        TestIconsRow()
    }
}

/**
 * 테스트용 아이콘들을 가로로 배치합니다.
 *
 * 여러 drawable 리소스를 테스트하여 앱의 아이콘 시스템이 정상적으로
 * 작동하는지 확인합니다.
 */
@Composable
private fun TestIconsRow() {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.width(280.dp)
    ) {
        // Touchpad 아이콘
        Icon(
            painter = painterResource(id = R.drawable.ic_touchpad),
            contentDescription = "Touchpad Icon",
            modifier = Modifier.size(48.dp),
            tint = TextPrimary
        )

        Spacer(modifier = Modifier.width(24.dp))

        // USB 아이콘
        Icon(
            painter = painterResource(id = R.drawable.ic_usb),
            contentDescription = "USB Icon",
            modifier = Modifier.size(48.dp),
            tint = TextPrimary
        )

        Spacer(modifier = Modifier.width(24.dp))

        // Keyboard 아이콘
        Icon(
            painter = painterResource(id = R.drawable.ic_keyboard),
            contentDescription = "Keyboard Icon",
            modifier = Modifier.size(48.dp),
            tint = TextPrimary
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BridgeOneAppPreview() {
    BridgeOneTheme {
        BridgeOneApp()
    }
}
