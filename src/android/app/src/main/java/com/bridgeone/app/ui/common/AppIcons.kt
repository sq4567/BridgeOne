package com.bridgeone.app.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.outlined.Mouse
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

// ============================================================
// AppIconDef — 아이콘 정의 모델
// ============================================================

/**
 * 앱 전역 아이콘 정의.
 *
 * 현재: staticIcon(Material Icons ImageVector)만 사용.
 * 향후: animation 필드를 추가해 Lottie/AVD 애니메이션 아이콘으로 교체 가능.
 */
data class AppIconDef(
    val staticIcon: ImageVector,
    // val animation: AppIconAnimation? = null  // 향후 Lottie/AVD 확장용 (현재 미구현)
)

// ============================================================
// AppIcons — 앱 전역 아이콘 목록표
// ============================================================

/**
 * 앱에서 사용하는 모든 Material Icons를 한 곳에서 관리합니다.
 *
 * 아이콘 추가·교체는 이 파일만 수정하면 전체에 반영됩니다.
 * 이름은 기능 기준으로 작명합니다 (컴포넌트명·위치 기준 아님).
 */
object AppIcons {
    // 포인터 다이나믹스 프리셋 (Phase 4.3.8)
    val DynamicsOff       = AppIconDef(Icons.Outlined.Remove)
    val DynamicsPrecision = AppIconDef(Icons.Filled.Adjust)
    val DynamicsStandard  = AppIconDef(Icons.Filled.Speed)
    val DynamicsFast      = AppIconDef(Icons.Filled.FlashOn)

    // DPI
    val DpiLow    = AppIconDef(Icons.Outlined.Mouse)
    val DpiNormal = AppIconDef(Icons.Filled.Mouse)
    val DpiHigh   = AppIconDef(Icons.Filled.KeyboardDoubleArrowRight)

    // 스크롤
    val ScrollUp   = AppIconDef(Icons.Filled.KeyboardArrowUp)
    val ScrollDown = AppIconDef(Icons.Filled.KeyboardArrowDown)

    // 모드
    val ScrollMode = AppIconDef(Icons.Filled.SwapVert)
    val CursorMode = AppIconDef(Icons.Filled.OpenWith)
}

// ============================================================
// AppIcon — 아이콘 래퍼 Composable
// ============================================================

/**
 * AppIconDef를 받아 아이콘을 렌더링하는 래퍼 Composable.
 *
 * 현재: 정적 아이콘(staticIcon)만 렌더링.
 * 향후: def.animation != null 조건으로 LottieAnimation 분기 추가 예정.
 */
@Composable
fun AppIcon(
    def: AppIconDef,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current
) {
    // 향후: if (def.animation != null && animatedIconsEnabled) → LottieAnimation
    Icon(
        imageVector = def.staticIcon,
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint
    )
}
