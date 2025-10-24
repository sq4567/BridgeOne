package com.bridgeone.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.bridgeone.app.ui.BridgeOneApp
import com.bridgeone.app.ui.theme.BridgeOneTheme

/**
 * BridgeOne 안드로이드 앱의 메인 액티비티입니다.
 *
 * 이 액티비티는 BridgeOne 앱의 진입점으로, BridgeOneApp Composable을
 * 호출하여 UI를 구성합니다.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BridgeOneTheme {
                BridgeOneApp()
            }
        }
    }
}