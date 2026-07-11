package com.bridgeone.app.ui.components.touchpad

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.ChangeHistory
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Hexagon
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.MoodBad
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Pentagon
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Rectangle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Square
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.ui.graphics.vector.ImageVector
import com.bridgeone.app.ui.common.AppIconDef
import com.bridgeone.app.ui.common.IconCategory
import com.bridgeone.app.ui.common.IconCategoryTab

/**
 * 앱 전역 아이콘 단일 소스. 모든 String key ↔ AppIconDef 매핑을 관리한다.
 * EdgeZone.iconKey, 커스텀 프리셋 iconKey, 아이콘 서랍(CategoryIconDrawer) 등이 이 레지스트리의 키를 사용한다.
 *
 * 정규 키는 PascalCase (기존 EdgeZone JSON 저장 파일 호환).
 * snake_case 키(커스텀 프리셋 기존 저장 키)는 [aliases]를 통해 읽기 시점에 정규화 → 저장 파일 변환 불요.
 *
 * 각 아이콘은 [IconCategory]에 1:1로 소속된다(중복 노출 없음). "전체" 탭은 [allKeys]로 합성한다.
 * 향후 아이콘 추가 시 [entries]에 항목을 추가하면 전체 피커 그리드에 자동 반영된다.
 */
object IconRegistry {

    /** 선택 가능한 모든 아이콘 (PascalCase 정규 키 → AppIconDef). 카테고리 순서대로 정렬. */
    private val entries: Map<String, AppIconDef> = linkedMapOf(
        // ── 포인터 ──
        "Mouse"         to AppIconDef(Icons.Filled.Mouse, IconCategory.POINTER),
        "TouchApp"      to AppIconDef(Icons.Filled.TouchApp, IconCategory.POINTER),
        "OpenWith"      to AppIconDef(Icons.Filled.OpenWith, IconCategory.POINTER),
        "ZoomIn"        to AppIconDef(Icons.Filled.ZoomIn, IconCategory.POINTER),
        "SwapVert"      to AppIconDef(Icons.Filled.SwapVert, IconCategory.POINTER),
        "Gesture"       to AppIconDef(Icons.Filled.Gesture, IconCategory.POINTER),
        "PanTool"       to AppIconDef(Icons.Filled.PanTool, IconCategory.POINTER),
        "Adjust"        to AppIconDef(Icons.Filled.Adjust, IconCategory.POINTER),
        "GpsFixed"      to AppIconDef(Icons.Filled.GpsFixed, IconCategory.POINTER),
        "MyLocation"    to AppIconDef(Icons.Filled.MyLocation, IconCategory.POINTER),
        "CenterFocus"   to AppIconDef(Icons.Filled.CenterFocusStrong, IconCategory.POINTER),
        "FilterAlt"     to AppIconDef(Icons.Filled.FilterAlt, IconCategory.POINTER),
        "Tune"          to AppIconDef(Icons.Filled.Tune, IconCategory.POINTER),

        // ── 화살표 ──
        "ArrowForward"  to AppIconDef(Icons.AutoMirrored.Filled.ArrowForward, IconCategory.ARROWS),
        "ArrowBack"     to AppIconDef(Icons.AutoMirrored.Filled.ArrowBack, IconCategory.ARROWS),
        "ArrowUpward"   to AppIconDef(Icons.Filled.ArrowUpward, IconCategory.ARROWS),
        "ArrowDownward" to AppIconDef(Icons.Filled.ArrowDownward, IconCategory.ARROWS),
        "KeyboardArrowUp"    to AppIconDef(Icons.Filled.KeyboardArrowUp, IconCategory.ARROWS),
        "KeyboardArrowDown"  to AppIconDef(Icons.Filled.KeyboardArrowDown, IconCategory.ARROWS),
        "KeyboardArrowLeft"  to AppIconDef(Icons.AutoMirrored.Filled.KeyboardArrowLeft, IconCategory.ARROWS),
        "KeyboardArrowRight" to AppIconDef(Icons.AutoMirrored.Filled.KeyboardArrowRight, IconCategory.ARROWS),
        "FastForward"   to AppIconDef(Icons.Filled.FastForward, IconCategory.ARROWS),
        "FastRewind"    to AppIconDef(Icons.Filled.FastRewind, IconCategory.ARROWS),
        "Undo"          to AppIconDef(Icons.AutoMirrored.Filled.Undo, IconCategory.ARROWS),
        "Redo"          to AppIconDef(Icons.AutoMirrored.Filled.Redo, IconCategory.ARROWS),

        // ── 미디어 ──
        "PlayArrow"     to AppIconDef(Icons.Filled.PlayArrow, IconCategory.MEDIA),
        "Pause"         to AppIconDef(Icons.Filled.Pause, IconCategory.MEDIA),
        "Stop"          to AppIconDef(Icons.Filled.Stop, IconCategory.MEDIA),
        "SkipNext"      to AppIconDef(Icons.Filled.SkipNext, IconCategory.MEDIA),
        "SkipPrevious"  to AppIconDef(Icons.Filled.SkipPrevious, IconCategory.MEDIA),
        "VolumeUp"      to AppIconDef(Icons.AutoMirrored.Filled.VolumeUp, IconCategory.MEDIA),
        "VolumeDown"    to AppIconDef(Icons.AutoMirrored.Filled.VolumeDown, IconCategory.MEDIA),
        "VolumeOff"     to AppIconDef(Icons.AutoMirrored.Filled.VolumeOff, IconCategory.MEDIA),
        "Mic"           to AppIconDef(Icons.Filled.Mic, IconCategory.MEDIA),
        "MicOff"        to AppIconDef(Icons.Filled.MicOff, IconCategory.MEDIA),
        "MusicNote"     to AppIconDef(Icons.Filled.MusicNote, IconCategory.MEDIA),
        "Headphones"    to AppIconDef(Icons.Filled.Headphones, IconCategory.MEDIA),

        // ── 편집 ──
        "Edit"          to AppIconDef(Icons.Filled.Edit, IconCategory.EDIT),
        "Delete"        to AppIconDef(Icons.Filled.Delete, IconCategory.EDIT),
        "ContentCopy"   to AppIconDef(Icons.Filled.ContentCopy, IconCategory.EDIT),
        "ContentPaste"  to AppIconDef(Icons.Filled.ContentPaste, IconCategory.EDIT),
        "ContentCut"    to AppIconDef(Icons.Filled.ContentCut, IconCategory.EDIT),
        "Save"          to AppIconDef(Icons.Filled.Save, IconCategory.EDIT),
        "Add"           to AppIconDef(Icons.Filled.Add, IconCategory.EDIT),
        "Remove"        to AppIconDef(Icons.Outlined.Remove, IconCategory.EDIT),   // EdgeZonePresetConstants builtin_minimal 호환
        "Check"         to AppIconDef(Icons.Filled.Check, IconCategory.EDIT),
        "Close"         to AppIconDef(Icons.Filled.Close, IconCategory.EDIT),
        "Search"        to AppIconDef(Icons.Filled.Search, IconCategory.EDIT),
        "Brush"         to AppIconDef(Icons.Filled.Brush, IconCategory.EDIT),

        // ── 시스템 ──
        "Settings"      to AppIconDef(Icons.Filled.Settings, IconCategory.SYSTEM),
        "Build"         to AppIconDef(Icons.Filled.Build, IconCategory.SYSTEM),
        "Extension"     to AppIconDef(Icons.Filled.Extension, IconCategory.SYSTEM),
        "Explore"       to AppIconDef(Icons.Filled.Explore, IconCategory.SYSTEM),
        "Autorenew"     to AppIconDef(Icons.Filled.Autorenew, IconCategory.SYSTEM),
        "Loop"          to AppIconDef(Icons.Filled.Loop, IconCategory.SYSTEM),
        "Power"         to AppIconDef(Icons.Filled.Power, IconCategory.SYSTEM),
        "Refresh"       to AppIconDef(Icons.Filled.Refresh, IconCategory.SYSTEM),
        "Sync"          to AppIconDef(Icons.Filled.Sync, IconCategory.SYSTEM),
        "Lock"          to AppIconDef(Icons.Filled.Lock, IconCategory.SYSTEM),
        "LockOpen"      to AppIconDef(Icons.Filled.LockOpen, IconCategory.SYSTEM),
        "Visibility"    to AppIconDef(Icons.Filled.Visibility, IconCategory.SYSTEM),
        "Home"          to AppIconDef(Icons.Filled.Home, IconCategory.SYSTEM),
        "Menu"          to AppIconDef(Icons.Filled.Menu, IconCategory.SYSTEM),

        // ── 도형 ──
        "Circle"        to AppIconDef(Icons.Filled.Circle, IconCategory.SHAPES),
        "Square"        to AppIconDef(Icons.Filled.Square, IconCategory.SHAPES),
        "Rectangle"     to AppIconDef(Icons.Filled.Rectangle, IconCategory.SHAPES),
        "Pentagon"      to AppIconDef(Icons.Filled.Pentagon, IconCategory.SHAPES),
        "Hexagon"       to AppIconDef(Icons.Filled.Hexagon, IconCategory.SHAPES),
        "ChangeHistory" to AppIconDef(Icons.Filled.ChangeHistory, IconCategory.SHAPES),
        "StarBorder"    to AppIconDef(Icons.Filled.StarBorder, IconCategory.SHAPES),
        "CropSquare"    to AppIconDef(Icons.Filled.CropSquare, IconCategory.SHAPES),
        "RadioButtonUnchecked" to AppIconDef(Icons.Filled.RadioButtonUnchecked, IconCategory.SHAPES),

        // ── 기호 ──
        "Star"          to AppIconDef(Icons.Filled.Star, IconCategory.SYMBOLS),
        "Favorite"      to AppIconDef(Icons.Filled.Favorite, IconCategory.SYMBOLS),
        "Bolt"          to AppIconDef(Icons.Filled.Bolt, IconCategory.SYMBOLS),
        "FlashOn"       to AppIconDef(Icons.Filled.FlashOn, IconCategory.SYMBOLS),
        "Whatshot"      to AppIconDef(Icons.Filled.Whatshot, IconCategory.SYMBOLS),
        "DirectionsRun" to AppIconDef(Icons.AutoMirrored.Filled.DirectionsRun, IconCategory.SYMBOLS),
        "PriorityHigh"  to AppIconDef(Icons.Filled.PriorityHigh, IconCategory.SYMBOLS),
        "QuestionMark"  to AppIconDef(Icons.Filled.QuestionMark, IconCategory.SYMBOLS),
        "Block"         to AppIconDef(Icons.Filled.Block, IconCategory.SYMBOLS),
        "Verified"      to AppIconDef(Icons.Filled.Verified, IconCategory.SYMBOLS),
        "Bookmark"      to AppIconDef(Icons.Filled.Bookmark, IconCategory.SYMBOLS),
        "Flag"          to AppIconDef(Icons.Filled.Flag, IconCategory.SYMBOLS),
        "Label"         to AppIconDef(Icons.AutoMirrored.Filled.Label, IconCategory.SYMBOLS),

        // ── 통신 ──
        "Chat"          to AppIconDef(Icons.AutoMirrored.Filled.Chat, IconCategory.COMMUNICATION),
        "Message"       to AppIconDef(Icons.AutoMirrored.Filled.Message, IconCategory.COMMUNICATION),
        "Email"         to AppIconDef(Icons.Filled.Email, IconCategory.COMMUNICATION),
        "Call"          to AppIconDef(Icons.Filled.Call, IconCategory.COMMUNICATION),
        "Notifications" to AppIconDef(Icons.Filled.Notifications, IconCategory.COMMUNICATION),
        "Share"         to AppIconDef(Icons.Filled.Share, IconCategory.COMMUNICATION),
        "Send"          to AppIconDef(Icons.AutoMirrored.Filled.Send, IconCategory.COMMUNICATION),
        "Forum"         to AppIconDef(Icons.Filled.Forum, IconCategory.COMMUNICATION),
        "Group"         to AppIconDef(Icons.Filled.Group, IconCategory.COMMUNICATION),
        "Person"        to AppIconDef(Icons.Filled.Person, IconCategory.COMMUNICATION),
        "Link"          to AppIconDef(Icons.Filled.Link, IconCategory.COMMUNICATION),
        "Campaign"      to AppIconDef(Icons.Filled.Campaign, IconCategory.COMMUNICATION),

        // ── 파일 ──
        "Folder"        to AppIconDef(Icons.Filled.Folder, IconCategory.FILES),
        "FolderOpen"    to AppIconDef(Icons.Filled.FolderOpen, IconCategory.FILES),
        "InsertDriveFile" to AppIconDef(Icons.AutoMirrored.Filled.InsertDriveFile, IconCategory.FILES),
        "Description"   to AppIconDef(Icons.Filled.Description, IconCategory.FILES),
        "Article"       to AppIconDef(Icons.AutoMirrored.Filled.Article, IconCategory.FILES),
        "Image"         to AppIconDef(Icons.Filled.Image, IconCategory.FILES),
        "Download"      to AppIconDef(Icons.Filled.Download, IconCategory.FILES),
        "Upload"        to AppIconDef(Icons.Filled.CloudUpload, IconCategory.FILES),
        "CloudUpload"   to AppIconDef(Icons.Filled.CloudUpload, IconCategory.FILES),
        "CloudDownload" to AppIconDef(Icons.Filled.CloudDownload, IconCategory.FILES),
        "Print"         to AppIconDef(Icons.Filled.Print, IconCategory.FILES),
        "Storage"       to AppIconDef(Icons.Filled.Storage, IconCategory.FILES),

        // ── 데이터 ──
        "BarChart"      to AppIconDef(Icons.Filled.BarChart, IconCategory.DATA),
        "ShowChart"     to AppIconDef(Icons.AutoMirrored.Filled.ShowChart, IconCategory.DATA),
        "Timeline"      to AppIconDef(Icons.Filled.Timeline, IconCategory.DATA),
        "TrendingUp"    to AppIconDef(Icons.AutoMirrored.Filled.TrendingUp, IconCategory.DATA),
        "TrendingDown"  to AppIconDef(Icons.AutoMirrored.Filled.TrendingDown, IconCategory.DATA),
        "PieChart"      to AppIconDef(Icons.Filled.PieChart, IconCategory.DATA),
        "BubbleChart"   to AppIconDef(Icons.Filled.BubbleChart, IconCategory.DATA),
        "Analytics"     to AppIconDef(Icons.Filled.Analytics, IconCategory.DATA),
        "Insights"      to AppIconDef(Icons.Filled.Insights, IconCategory.DATA),
        "DataUsage"     to AppIconDef(Icons.Filled.DataUsage, IconCategory.DATA),
        "Speed"         to AppIconDef(Icons.Filled.Speed, IconCategory.DATA),
        "Timer"         to AppIconDef(Icons.Filled.Timer, IconCategory.DATA),

        // ── 날씨 ──
        "WbSunny"       to AppIconDef(Icons.Filled.WbSunny, IconCategory.WEATHER),
        "WbCloudy"      to AppIconDef(Icons.Filled.WbCloudy, IconCategory.WEATHER),
        "Cloud"         to AppIconDef(Icons.Filled.Cloud, IconCategory.WEATHER),
        "Thunderstorm"  to AppIconDef(Icons.Filled.Thunderstorm, IconCategory.WEATHER),
        "AcUnit"        to AppIconDef(Icons.Filled.AcUnit, IconCategory.WEATHER),
        "WaterDrop"     to AppIconDef(Icons.Filled.WaterDrop, IconCategory.WEATHER),
        "Air"           to AppIconDef(Icons.Filled.Air, IconCategory.WEATHER),
        "NightsStay"    to AppIconDef(Icons.Filled.NightsStay, IconCategory.WEATHER),
        "Umbrella"      to AppIconDef(Icons.Filled.Umbrella, IconCategory.WEATHER),
        "Waves"         to AppIconDef(Icons.Filled.Waves, IconCategory.WEATHER),

        // ── 감정 ──
        "Mood"          to AppIconDef(Icons.Filled.Mood, IconCategory.EMOTION),
        "MoodBad"       to AppIconDef(Icons.Filled.MoodBad, IconCategory.EMOTION),
        "FavoriteBorder" to AppIconDef(Icons.Filled.FavoriteBorder, IconCategory.EMOTION),
        "SentimentSatisfied"     to AppIconDef(Icons.Filled.SentimentSatisfied, IconCategory.EMOTION),
        "SentimentDissatisfied"  to AppIconDef(Icons.Filled.SentimentDissatisfied, IconCategory.EMOTION),
        "SentimentVerySatisfied" to AppIconDef(Icons.Filled.SentimentVerySatisfied, IconCategory.EMOTION),
        "ThumbUp"       to AppIconDef(Icons.Filled.ThumbUp, IconCategory.EMOTION),
        "ThumbDown"     to AppIconDef(Icons.Filled.ThumbDown, IconCategory.EMOTION),
        "EmojiEmotions" to AppIconDef(Icons.Filled.EmojiEmotions, IconCategory.EMOTION),
        "Celebration"   to AppIconDef(Icons.Filled.Celebration, IconCategory.EMOTION),

        // ── 장치 ──
        "Keyboard"      to AppIconDef(Icons.Filled.Keyboard, IconCategory.DEVICE),
        "Gamepad"       to AppIconDef(Icons.Filled.Gamepad, IconCategory.DEVICE),
        "Headset"       to AppIconDef(Icons.Filled.Headset, IconCategory.DEVICE),
        "Smartphone"    to AppIconDef(Icons.Filled.Smartphone, IconCategory.DEVICE),
        "Computer"      to AppIconDef(Icons.Filled.Computer, IconCategory.DEVICE),
        "Watch"         to AppIconDef(Icons.Filled.Watch, IconCategory.DEVICE),
        "Tv"            to AppIconDef(Icons.Filled.Tv, IconCategory.DEVICE),
        "Speaker"       to AppIconDef(Icons.Filled.Speaker, IconCategory.DEVICE),
        "Memory"        to AppIconDef(Icons.Filled.Memory, IconCategory.DEVICE),
        "Usb"           to AppIconDef(Icons.Filled.Usb, IconCategory.DEVICE),
        "Vibration"     to AppIconDef(Icons.Filled.Vibration, IconCategory.DEVICE),
    )

    /**
     * snake_case 키 → PascalCase 정규 키 변환 맵.
     * CUSTOM_PRESET_ICON_OPTIONS의 기존 저장 키 호환.
     */
    private val aliases: Map<String, String> = mapOf(
        "star"         to "Star",
        "flash"        to "FlashOn",
        "bolt"         to "Bolt",
        "whatshot"     to "Whatshot",
        "fast_forward" to "FastForward",
        "run"          to "DirectionsRun",
        "speed"        to "Speed",
        "trending_up"  to "TrendingUp",
        "bar_chart"    to "BarChart",
        "show_chart"   to "ShowChart",
        "timeline"     to "Timeline",
        "waves"        to "Waves",
        "tune"         to "Tune",
        "adjust"       to "Adjust",
        "filter"       to "FilterAlt",
        "center_focus" to "CenterFocus",
        "gps_fixed"    to "GpsFixed",
        "my_location"  to "MyLocation",
        "explore"      to "Explore",
        "loop"         to "Loop",
        "favorite"     to "Favorite",
        "gamepad"      to "Gamepad",
        "extension"    to "Extension",
        "settings"     to "Settings",
        "build"        to "Build",
        "mouse"        to "Mouse",
        "touch"        to "TouchApp",
        "timer"        to "Timer",
        "autorenew"    to "Autorenew",
        "vibration"    to "Vibration",
    )

    /** 선택 가능한 모든 아이콘 키 목록 ("전체" 카테고리). */
    val allKeys: List<String> = entries.keys.toList()

    /** entries에 실제 아이콘이 존재하는 카테고리만 (enum 순서 유지). */
    val categories: List<IconCategory> =
        IconCategory.entries.filter { c -> entries.values.any { it.category == c } }

    /** [category]에 소속된 아이콘 키 목록 (entries 정의 순서 유지). */
    fun keysIn(category: IconCategory): List<String> =
        entries.filter { it.value.category == category }.keys.toList()

    /** 탭에 해당하는 아이콘 키 목록. [IconCategoryTab.All]이면 전체. */
    fun keysFor(tab: IconCategoryTab): List<String> = when (tab) {
        is IconCategoryTab.All  -> allKeys
        is IconCategoryTab.Real -> keysIn(tab.category)
    }

    /**
     * iconKey 정규화: snake_case alias → PascalCase 정규 키.
     * 이미 PascalCase이거나 alias에 없으면 그대로 반환.
     */
    fun normalizeIconKey(key: String): String =
        if (entries.containsKey(key)) key else aliases[key] ?: key

    /**
     * iconKey → AppIconDef. 매핑 없으면 null (커스텀 프리셋 텍스트 폴백용).
     */
    fun defOrNull(key: String): AppIconDef? = entries[normalizeIconKey(key)]

    /**
     * iconKey → AppIconDef. 매핑 없으면 Mouse 폴백.
     */
    fun def(key: String): AppIconDef = defOrNull(key) ?: AppIconDef(Icons.Filled.Mouse, IconCategory.POINTER)

    /** iconKey → ImageVector 변환. 매핑 없으면 Mouse 아이콘 반환. */
    fun get(key: String): ImageVector = def(key).staticIcon
}
