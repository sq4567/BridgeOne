package com.bridgeone.app.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material.icons.filled.Whatshot
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
 *
 * @param category 아이콘 카테고리. 기본값 [IconCategory.SYSTEM] (기존 AppIcons object 호출 호환용).
 */
data class AppIconDef(
    val staticIcon: ImageVector,
    val category: IconCategory = IconCategory.SYSTEM,
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

    // 모드 프리셋 (Phase 4.4.8)
    val ModePresetStandard = AppIconDef(Icons.Filled.Tune)
    val ModePresetPrecise  = AppIconDef(Icons.Filled.GpsFixed)
    val ModePresetFast     = AppIconDef(Icons.AutoMirrored.Filled.DirectionsRun)

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

    // 커스텀 프리셋 아이콘 선택지 (Phase 4.5.16)
    val PickStar       = AppIconDef(Icons.Filled.Star)
    val PickFlash      = AppIconDef(Icons.Filled.FlashOn)
    val PickSpeed      = AppIconDef(Icons.Filled.Speed)
    val PickTune       = AppIconDef(Icons.Filled.Tune)
    val PickAdjust     = AppIconDef(Icons.Filled.Adjust)
    val PickTrendingUp = AppIconDef(Icons.Filled.TrendingUp)
    val PickBarChart   = AppIconDef(Icons.Filled.BarChart)
    val PickShowChart       = AppIconDef(Icons.AutoMirrored.Filled.ShowChart)
    val PickBolt            = AppIconDef(Icons.Filled.Bolt)
    val PickFavorite        = AppIconDef(Icons.Filled.Favorite)
    val PickFilter          = AppIconDef(Icons.Filled.FilterAlt)
    val PickExplore         = AppIconDef(Icons.Filled.Explore)
    val PickTimeline        = AppIconDef(Icons.Filled.Timeline)
    val PickMyLocation      = AppIconDef(Icons.Filled.MyLocation)
    val PickGpsFixed        = AppIconDef(Icons.Filled.GpsFixed)
    val PickGamepad         = AppIconDef(Icons.Filled.Gamepad)
    val PickSettings        = AppIconDef(Icons.Filled.Settings)
    val PickBuild           = AppIconDef(Icons.Filled.Build)
    val PickExtension       = AppIconDef(Icons.Filled.Extension)
    val PickWaves           = AppIconDef(Icons.Filled.Waves)
    val PickLoop            = AppIconDef(Icons.Filled.Loop)
    val PickFastForward     = AppIconDef(Icons.Filled.FastForward)
    val PickCenterFocus     = AppIconDef(Icons.Filled.CenterFocusStrong)
    val PickWhatshot        = AppIconDef(Icons.Filled.Whatshot)
    val PickDirectionsRun   = AppIconDef(Icons.AutoMirrored.Filled.DirectionsRun)
    val PickMouse           = AppIconDef(Icons.Filled.Mouse)
    val PickTouchApp        = AppIconDef(Icons.Filled.TouchApp)
    val PickTimer           = AppIconDef(Icons.Filled.Timer)
    val PickAutorenew       = AppIconDef(Icons.Filled.Autorenew)
    val PickVibration       = AppIconDef(Icons.Filled.Vibration)
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
