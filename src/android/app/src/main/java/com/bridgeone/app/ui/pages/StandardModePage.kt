package com.bridgeone.app.ui.pages

import android.content.Context
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bridgeone.app.protocol.BridgeMode
import com.bridgeone.app.ui.common.PAGE1_TOUCHPAD_BOTTOM_TEST_OFFSET
import com.bridgeone.app.ui.common.KEY_BACKSPACE
import com.bridgeone.app.ui.common.KEY_DELETE
import com.bridgeone.app.ui.common.KEY_END
import com.bridgeone.app.ui.common.KEY_ENTER
import com.bridgeone.app.ui.common.KEY_ESC
import com.bridgeone.app.ui.common.KEY_HOME
import com.bridgeone.app.ui.common.KEY_SPACE
import com.bridgeone.app.ui.common.KEY_TAB
import com.bridgeone.app.ui.components.DEFAULT_SHORTCUTS
import com.bridgeone.app.ui.components.KeyboardKeyButton
import com.bridgeone.app.ui.components.ShortcutButton
import com.bridgeone.app.ui.components.TouchpadWrapper
import com.bridgeone.app.ui.components.touchpad.ControlButtonConfig
import com.bridgeone.app.ui.components.touchpad.ControlButtonContainer
import com.bridgeone.app.ui.components.touchpad.EdgeInteractionMode
import com.bridgeone.app.ui.components.touchpad.DpiAdjustPopup
import com.bridgeone.app.ui.components.touchpad.DpiLevel
import com.bridgeone.app.ui.common.CustomPointerDynamicsPreset
import com.bridgeone.app.ui.common.CustomPresetsRepository
import com.bridgeone.app.ui.common.DYNAMICS_PRESETS
import com.bridgeone.app.ui.common.EdgeZonePresetsRepository
import com.bridgeone.app.ui.common.MODE_PRESETS
import com.bridgeone.app.ui.common.TouchpadButtonVisibility
import com.bridgeone.app.ui.common.TouchpadButtonVisibilityRepository
import com.bridgeone.app.ui.common.TouchpadEdgeZoneAssignment
import com.bridgeone.app.ui.common.TouchpadEdgeZoneAssignmentRepository
import com.bridgeone.app.ui.common.TouchpadIds
import com.bridgeone.app.ui.components.touchpad.DynamicsCurveEditor
import com.bridgeone.app.ui.components.touchpad.DynamicsPresetPopup
import com.bridgeone.app.ui.components.touchpad.EdgeZonePresetPopup
import com.bridgeone.app.ui.components.touchpad.ModePresetPopup
import com.bridgeone.app.ui.components.touchpad.ScrollMode
import com.bridgeone.app.ui.components.touchpad.TouchpadState
import com.bridgeone.app.ui.utils.ClickDetector
import kotlin.math.abs

// ============================================================
// Standard 모드 페이지 (Phase 4.2.1: 3페이지 네비게이션)
// ============================================================

/**
 * Standard 모드 메인 페이지 (완전 재작성)
 *
 * Phase 4.2.1: HorizontalPager 기반 4페이지 시스템
 * - Page 0: 터치패드 + Actions (상대좌표)
 * - Page 1: 절대좌표 패드 (Phase 4.4에서 구현)
 * - Page 2: 키보드 (Phase 4.5에서 구현)
 * - Page 3: 마인크래프트 (Phase 4.6에서 구현)
 * - 하단 페이지 인디케이터 (닷 4개)
 */
private const val PAGE_COUNT = 5
// Int.MAX_VALUE / 2를 PAGE_COUNT의 배수로 내림 → 논리 페이지 0에서 시작, 양방향 무한 스크롤 가능
private val PAGER_INITIAL_PAGE = (Int.MAX_VALUE / 2).let { mid -> mid - (mid % PAGE_COUNT) }

@Composable
fun StandardModePage(onCurveEditorVisibleChange: (Boolean) -> Unit = {}) {
    val pagerState = rememberPagerState(initialPage = PAGER_INITIAL_PAGE, pageCount = { Int.MAX_VALUE })
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Phase 4.3.3: 터치패드 상태를 페이지 레벨로 호이스팅
    // DpiLevel, EdgeInteractionMode는 SharedPreferences에서 복원
    var touchpadState by remember {
        mutableStateOf(
            TouchpadState(
                dpiLevel = loadDpiLevel(context),
                edgeInteractionMode = loadEdgeInteractionMode(context)
            )
        )
    }

    // 터치패드별 엣지 존 할당 (Phase 4.6.2+)
    val assignmentRepo = remember {
        TouchpadEdgeZoneAssignmentRepository(context).also {
            it.migrateLegacyIfNeeded(context)
            it.migrateStandardPrimaryKeyIfNeeded()
        }
    }
    // 페이지 인덱스(0-based)를 터치패드 ID로 사용. 터치패드가 있는 페이지만 포함.
    val standardTouchpadPages = remember { listOf(0, 1) }
    var standardAssignments by remember {
        mutableStateOf(standardTouchpadPages.associateWith { assignmentRepo.load(TouchpadIds.standardPage(it)) })
    }
    // Page 5 설정에서 현재 선택된 페이지 인덱스 (엣지 존 + 버튼 표시 공유)
    var selectedZonePage by remember { mutableStateOf(0) }

    // 터치패드별 버튼 표시 설정
    val buttonVisibilityRepo = remember { TouchpadButtonVisibilityRepository(context) }
    var standardButtonVisibility by remember {
        mutableStateOf(standardTouchpadPages.associateWith { buttonVisibilityRepo.load(TouchpadIds.standardPage(it)) })
    }

    // DPI 레벨(사전 정의 값)이 변경될 때 SharedPreferences에 저장
    LaunchedEffect(touchpadState.dpiLevel) {
        saveDpiLevel(context, touchpadState.dpiLevel)
    }

    // 엣지 조작 방식이 변경될 때 SharedPreferences에 저장 (Phase 4.6.1)
    LaunchedEffect(touchpadState.edgeInteractionMode) {
        saveEdgeInteractionMode(context, touchpadState.edgeInteractionMode)
    }

    // 엣지 존 할당이 변경될 때 파일에 저장 (Phase 4.6.2+)
    LaunchedEffect(standardAssignments) {
        standardAssignments.forEach { (pageIdx, assignment) ->
            assignmentRepo.save(TouchpadIds.standardPage(pageIdx), assignment)
        }
    }

    // 버튼 표시 설정이 변경될 때 파일에 저장
    LaunchedEffect(standardButtonVisibility) {
        standardButtonVisibility.forEach { (pageIdx, visibility) ->
            buttonVisibilityRepo.save(TouchpadIds.standardPage(pageIdx), visibility)
        }
    }

    // Phase 4.6.3: 엣지 존 프리셋 저장소
    val edgeZonePresetsRepo = remember { EdgeZonePresetsRepository(context) }

    // 존 편집기 표시 상태 (Phase 4.6.2)
    var showZoneEditor by remember { mutableStateOf(false) }

    // 존 프리셋 팝업 표시 상태 (Phase 4.6.3)
    var showZonePresetPopup by remember { mutableStateOf(false) }

    // Phase 4.5.16: 커스텀 다이나믹스 프리셋 상태
    val customPresetsRepo = remember { CustomPresetsRepository(context) }
    var customPresets by remember { mutableStateOf<List<CustomPointerDynamicsPreset>>(emptyList()) }
    LaunchedEffect(Unit) { customPresets = customPresetsRepo.loadAll() }

    // Phase 4.5.16: 그래프 편집기 상태
    var curveEditorVisible by remember { mutableStateOf(false) }
    var editingPreset by remember { mutableStateOf<CustomPointerDynamicsPreset?>(null) }
    LaunchedEffect(curveEditorVisible) { onCurveEditorVisibleChange(curveEditorVisible) }

    // Phase 4.3.6: DPI 세밀 조절 팝업 상태
    var dpiAdjustPopupVisible by remember { mutableStateOf(false) }

    // Phase 4.3.8: 다이나믹스 프리셋 팝업 상태
    var dynamicsPresetPopupVisible by remember { mutableStateOf(false) }

    // Phase 4.4.8: 모드 프리셋 팝업 상태
    var modePresetPopupVisible by remember { mutableStateOf(false) }

    // 페이지 전환 시 팝업 취소 (커스텀 값 미적용)
    LaunchedEffect(pagerState.currentPage) {
        if (dpiAdjustPopupVisible) dpiAdjustPopupVisible = false
        if (dynamicsPresetPopupVisible) dynamicsPresetPopupVisible = false
        if (modePresetPopupVisible) modePresetPopupVisible = false
        if (curveEditorVisible) curveEditorVisible = false
    }

    // 스크롤 모드 전환 시 다이나믹스 팝업 취소 (Phase 4.3.8)
    LaunchedEffect(touchpadState.scrollMode) {
        if (dynamicsPresetPopupVisible) dynamicsPresetPopupVisible = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // ── 페이지 컨테이너 ──
        // 스크롤 모드 활성 시: HorizontalPager보다 먼저 Initial 패스에서
        // Move 이벤트를 소비하여 페이저의 수평 드래그 감지를 원천 차단
        val isScrollActive = touchpadState.scrollMode != ScrollMode.OFF
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(isScrollActive) {
                    if (!isScrollActive) return@pointerInput
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            if (event.type == PointerEventType.Move) {
                                event.changes.forEach { it.consume() }
                            }
                        }
                    }
                }
        ) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = touchpadState.scrollMode == ScrollMode.OFF,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp, vertical = 8.dp)
            ) { page ->
                when (page % PAGE_COUNT) {
                    0 -> Page1TouchpadActions(
                        touchpadState = touchpadState,
                        edgeZoneAssignment = standardAssignments[0] ?: TouchpadEdgeZoneAssignment.default(),
                        onEdgeZoneAssignmentChange = { updated -> standardAssignments = standardAssignments + (0 to updated) },
                        customPresets = customPresets,
                        onTouchpadStateChange = { touchpadState = it },
                        buttonVisibility = standardButtonVisibility[0] ?: TouchpadButtonVisibility.default(),
                        onButtonVisibilityChange = { updated -> standardButtonVisibility = standardButtonVisibility + (0 to updated) },
                        dpiAdjustPopupVisible = dpiAdjustPopupVisible,
                        dynamicsPresetPopupVisible = dynamicsPresetPopupVisible,
                        modePresetPopupVisible = modePresetPopupVisible,
                        onDpiLongPress = { dpiAdjustPopupVisible = true },
                        onDynamicsLongPress = { dynamicsPresetPopupVisible = true },
                        onModePresetLongPress = { modePresetPopupVisible = true },
                        onDpiAdjustConfirm = { value ->
                            dpiAdjustPopupVisible = false
                            val matchedLevel = DpiLevel.entries.firstOrNull {
                                abs(it.multiplier - value) < 0.001f
                            }
                            touchpadState = if (matchedLevel != null) {
                                touchpadState.copy(dpiLevel = matchedLevel, customDpiMultiplier = null)
                            } else {
                                touchpadState.copy(customDpiMultiplier = value)
                            }
                        },
                        onDpiAdjustDismiss = { dpiAdjustPopupVisible = false },
                        onDynamicsPresetConfirmed = { index ->
                            dynamicsPresetPopupVisible = false
                            touchpadState = touchpadState.copy(dynamicsPresetIndex = index)
                        },
                        onDynamicsPresetDismiss = { dynamicsPresetPopupVisible = false },
                        onAddCustomPreset = {
                            editingPreset = null
                            curveEditorVisible = true
                        },
                        onEditCustomPreset = { preset ->
                            editingPreset = preset
                            curveEditorVisible = true
                        },
                        onDeleteCustomPreset = { id ->
                            customPresetsRepo.delete(id)
                            customPresets = customPresetsRepo.loadAll()
                            // 삭제된 프리셋이 선택중이면 Off(0)으로 초기화
                            if (touchpadState.dynamicsPresetIndex >= DYNAMICS_PRESETS.size + customPresets.size) {
                                touchpadState = touchpadState.copy(dynamicsPresetIndex = 0)
                            }
                        },
                        onModePresetConfirmed = { index ->
                            modePresetPopupVisible = false
                            val preset = MODE_PRESETS[index]
                            touchpadState = touchpadState.copy(
                                clickMode = preset.padModeState.clickMode,
                                moveMode = preset.padModeState.moveMode,
                                scrollMode = preset.padModeState.scrollMode,
                                dpiLevel = preset.padModeState.dpi,
                                customDpiMultiplier = null,
                                dynamicsPresetIndex = preset.dynamicsPresetIndex,
                                modePresetIndex = index
                            )
                        },
                        onModePresetDismiss = { modePresetPopupVisible = false }
                    )
                    1 -> Page2TestTouchpad(
                        touchpadState = touchpadState,
                        edgeZoneAssignment = standardAssignments[1] ?: TouchpadEdgeZoneAssignment.default(),
                        onEdgeZoneAssignmentChange = { updated -> standardAssignments = standardAssignments + (1 to updated) },
                        customPresets = customPresets,
                        onTouchpadStateChange = { touchpadState = it },
                        buttonVisibility = standardButtonVisibility[1] ?: TouchpadButtonVisibility.defaultFor(TouchpadIds.standardPage(1)),
                        onDpiLongPress = { dpiAdjustPopupVisible = true }
                    )
                    2 -> Page3KeyboardPlaceholder()
                    3 -> Page4MinecraftPlaceholder()
                    4 -> Page5Settings(
                        touchpadState = touchpadState,
                        onTouchpadStateChange = { touchpadState = it },
                        standardAssignments = standardAssignments,
                        selectedZonePage = selectedZonePage,
                        onSelectedZonePageChange = { selectedZonePage = it },
                        onOpenZoneEditor = { showZoneEditor = true },
                        onOpenZonePresetPopup = { showZonePresetPopup = true },
                        standardButtonVisibility = standardButtonVisibility,
                        onButtonVisibilityChange = { pageIdx, updated ->
                            standardButtonVisibility = standardButtonVisibility + (pageIdx to updated)
                        }
                    )
                }
            }
        }

        // ── 페이지 인디케이터 ──
        // wrap-around 전환(0→4, 4→0) 시 worm이 화면 밖으로 튀지 않도록 offset 고정
        val logicalPage = pagerState.currentPage % PAGE_COUNT
        val rawOffset = pagerState.currentPageOffsetFraction
        val indicatorOffset = when {
            logicalPage == 0 && rawOffset < 0 -> 0f  // 0→4 wrap
            logicalPage == PAGE_COUNT - 1 && rawOffset > 0 -> 0f  // 4→0 wrap
            else -> rawOffset
        }
        PageIndicator(
            currentPage = logicalPage,
            offsetFraction = indicatorOffset,
            pageCount = PAGE_COUNT,
            onPageClick = { targetLogicalPage ->
                val targetPage = pagerState.currentPage - (pagerState.currentPage % PAGE_COUNT) + targetLogicalPage
                coroutineScope.launch { pagerState.animateScrollToPage(targetPage) }
            },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 16.dp)
        )
    }

    // ── Phase 4.6.2: 존 편집기 오버레이 ──
    if (showZoneEditor) {
        val targetAssignment = standardAssignments[selectedZonePage] ?: TouchpadEdgeZoneAssignment.default()
        val zoneEditorDisabledEdges: Map<com.bridgeone.app.ui.components.touchpad.EntryEdge, String> =
            if (selectedZonePage == 0) mapOf(com.bridgeone.app.ui.components.touchpad.EntryEdge.TOP to "제어 버튼")
            else emptyMap()
        com.bridgeone.app.ui.components.touchpad.EdgeZoneEditorScreen(
            initialConfig = targetAssignment.config,
            initialPresetId = targetAssignment.presetId,
            presetsRepo = edgeZonePresetsRepo,
            disabledEdges = zoneEditorDisabledEdges,
            onSave = { newConfig, presetId ->
                standardAssignments = standardAssignments + (selectedZonePage to TouchpadEdgeZoneAssignment(newConfig, presetId))
                showZoneEditor = false
            },
            onBack = { showZoneEditor = false }
        )
    }

    // ── Phase 4.6.3: 존 프리셋 팝업 ──
    if (showZonePresetPopup) {
        val targetAssignment = standardAssignments[selectedZonePage] ?: TouchpadEdgeZoneAssignment.default()
        EdgeZonePresetPopup(
            currentPresetId = targetAssignment.presetId,
            currentConfig = targetAssignment.config,
            presetsRepo = edgeZonePresetsRepo,
            onApply = { preset ->
                standardAssignments = standardAssignments + (selectedZonePage to TouchpadEdgeZoneAssignment(preset.config, preset.id))
                showZonePresetPopup = false
            },
            onDismiss = { showZonePresetPopup = false }
        )
    }

    // ── Phase 4.5.16: 커스텀 프리셋 그래프 편집기 오버레이 ──
    if (curveEditorVisible) {
        DynamicsCurveEditor(
            initialPreset = editingPreset,
            existingPresets = customPresets,
            onSave = { preset ->
                if (editingPreset == null) {
                    val saved = customPresetsRepo.add(preset)
                    customPresets = customPresetsRepo.loadAll()
                    // 새 프리셋을 즉시 선택
                    val newIndex = DYNAMICS_PRESETS.size + customPresets.indexOfFirst { it.id == saved.id }
                    if (newIndex >= DYNAMICS_PRESETS.size) {
                        touchpadState = touchpadState.copy(dynamicsPresetIndex = newIndex)
                    }
                } else {
                    customPresetsRepo.update(preset)
                    customPresets = customPresetsRepo.loadAll()
                }
                curveEditorVisible = false
                editingPreset = null
            },
            onDismiss = {
                curveEditorVisible = false
                editingPreset = null
            },
            modifier = Modifier.fillMaxSize()
        )
    }
    } // Box 닫기
}

// ============================================================
// 페이지 인디케이터
// ============================================================

@Composable
private fun PageIndicator(
    currentPage: Int,
    offsetFraction: Float,
    pageCount: Int,
    onPageClick: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val dotSizeDp = 8.dp
    val dotSpacingDp = 16.dp
    val totalWidth = (dotSizeDp * pageCount) + (dotSpacingDp * (pageCount - 1))

    val density = LocalDensity.current
    val dotSizePx = with(density) { dotSizeDp.toPx() }
    val dotSpacingPx = with(density) { dotSpacingDp.toPx() }
    val dotStepPx = dotSizePx + dotSpacingPx  // 한 닷에서 다음 닷까지 거리

    val absOffset = kotlin.math.abs(offsetFraction)
    val direction = if (offsetFraction > 0) 1f else -1f

    // THIN_WORM 효과:
    // head(앞 가장자리)가 먼저 빠르게 도달하고, tail(뒤 가장자리)이 나중에 따라옴
    val headProgress = minOf(1f, absOffset * 2f)   // 0.0 → 0.5 구간에서 0→1
    val tailProgress = maxOf(0f, absOffset * 2f - 1f)  // 0.5 → 1.0 구간에서 0→1

    val currentOriginPx = currentPage * dotStepPx

    // tail: 후반부에 출발점을 이동
    val tailPx = currentOriginPx + tailProgress * direction * dotStepPx
    // head: 전반부에 도착점으로 이동
    val headPx = currentOriginPx + dotSizePx + headProgress * direction * dotStepPx

    val leftPx = minOf(tailPx, headPx)
    val widthPx = maxOf(dotSizePx, kotlin.math.abs(headPx - tailPx))

    val leftDp = with(density) { leftPx.toDp() }
    val widthDp = with(density) { widthPx.toDp() }

    Box(
        modifier = modifier
            .width(totalWidth)
            .height(dotSizeDp)
    ) {
        // ── 배경 닷들 (비활성, 회색) ──
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(dotSpacingDp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(pageCount) { index ->
                Box(
                    modifier = Modifier
                        .size(dotSizeDp)
                        .background(Color(0xFFC2C2C2), CircleShape)
                        .clickable { onPageClick(index) }
                )
            }
        }

        // ── THIN_WORM 슬라이더 (파란색, 늘어나는 캡슐 모양) ──
        Box(
            modifier = Modifier
                .offset(x = leftDp)
                .width(widthDp)
                .height(dotSizeDp)
                .clip(CircleShape)
                .background(Color(0xFF2196F3))
        )
    }
}

// ============================================================
// Page 1: 터치패드 + Actions (임시 구현)
// ============================================================

/**
 * Page 1: 터치패드 + Actions
 *
 * Phase 4.2.2: 정식 레이아웃 구현
 * - 좌측: 터치패드 (64%)
 * - 우측: Actions 패널 (36%, LazyColumn 기반)
 * - 반응형: 폭 < 360dp 일 때 좌 60% / 우 40% 조정
 */
@Composable
private fun Page1TouchpadActions(
    touchpadState: TouchpadState,
    edgeZoneAssignment: TouchpadEdgeZoneAssignment = TouchpadEdgeZoneAssignment.default(),
    onEdgeZoneAssignmentChange: (TouchpadEdgeZoneAssignment) -> Unit = {},
    customPresets: List<CustomPointerDynamicsPreset> = emptyList(),
    onTouchpadStateChange: (TouchpadState) -> Unit,
    buttonVisibility: TouchpadButtonVisibility = TouchpadButtonVisibility.default(),
    onButtonVisibilityChange: (TouchpadButtonVisibility) -> Unit = {},
    dpiAdjustPopupVisible: Boolean = false,
    dynamicsPresetPopupVisible: Boolean = false,
    modePresetPopupVisible: Boolean = false,
    onDpiLongPress: () -> Unit = {},
    onDynamicsLongPress: () -> Unit = {},
    onModePresetLongPress: () -> Unit = {},
    onDpiAdjustConfirm: (Float) -> Unit = {},
    onDpiAdjustDismiss: () -> Unit = {},
    onDynamicsPresetConfirmed: (Int) -> Unit = {},
    onDynamicsPresetDismiss: () -> Unit = {},
    onAddCustomPreset: () -> Unit = {},
    onEditCustomPreset: (CustomPointerDynamicsPreset) -> Unit = {},
    onDeleteCustomPreset: (String) -> Unit = {},
    onModePresetConfirmed: (Int) -> Unit = {},
    onModePresetDismiss: () -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp

    // 반응형 비율 계산
    val (touchpadWeight, actionsPanelWeight) = if (screenWidthDp < 360) {
        0.60f to 0.40f
    } else {
        0.64f to 0.36f
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 2열 레이아웃: 좌측 터치패드 + 우측 Actions 패널
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── 좌측: 터치패드 (64% / 60%) ──
            // Phase 4.3.1: Box 내부에 ControlButtonContainer 오버레이 추가
            // Phase 4.3.6 / 4.3.8 / 4.4.8: DPI 팝업, 다이나믹스 팝업, 모드 프리셋 팝업 표시 시 배경 블러 적용
            val blurRadius by animateDpAsState(
                targetValue = if (dpiAdjustPopupVisible || dynamicsPresetPopupVisible || modePresetPopupVisible) 8.dp else 0.dp,
                animationSpec = tween(200),
                label = "popupBlur"
            )
            Box(
                modifier = Modifier
                    .weight(touchpadWeight)
                    .fillMaxHeight()
                    .align(Alignment.CenterVertically)
                    .padding(bottom = PAGE1_TOUCHPAD_BOTTOM_TEST_OFFSET)
            ) {
                // 팝업 표시 시 블러 처리되는 배경 영역
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .blur(blurRadius)
                ) {
                    TouchpadWrapper(
                        touchpadId = TouchpadIds.standardPage(0),
                        bridgeMode = BridgeMode.STANDARD,
                        touchpadState = touchpadState,
                        edgeZoneAssignment = edgeZoneAssignment,
                        onEdgeZoneAssignmentChange = onEdgeZoneAssignmentChange,
                        customPresets = customPresets,
                        onTouchpadStateChange = onTouchpadStateChange,
                        onDynamicsLongPress = onDynamicsLongPress,
                        onModePresetLongPress = onModePresetLongPress,
                        buttonVisibility = buttonVisibility,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                color = Color(0xFF1A1A1A),
                                shape = RoundedCornerShape(12.dp)
                            )
                    )

                    // Phase 4.3.1: ControlButtonContainer 오버레이 (상단 15%, 마스터 ON일 때만)
                    if (buttonVisibility.showControlButtons) {
                        ControlButtonContainer(
                            touchpadState = touchpadState,
                            onStateChange = onTouchpadStateChange,
                            onDpiLongPress = onDpiLongPress,
                            config = buttonVisibility.controlButtonConfig,
                            modifier = Modifier
                                .fillMaxSize()
                                .align(Alignment.TopCenter)
                        )
                    }
                }

                // Phase 4.3.6: DPI 세밀 조절 팝업 오버레이
                // 팝업 내 pointerInput이 이벤트를 소비 → TouchpadWrapper 제스처 자동 차단
                if (dpiAdjustPopupVisible) {
                    DpiAdjustPopup(
                        initialMultiplier = touchpadState.effectiveDpiMultiplier,
                        onConfirm = onDpiAdjustConfirm,
                        onDismiss = onDpiAdjustDismiss,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                    )
                }

                // Phase 4.3.8 / 4.3.9 / 4.5.16: 다이나믹스 프리셋 팝업 오버레이
                // 항상 렌더링하고 visible 파라미터로 제어 (exit 애니메이션 보장)
                DynamicsPresetPopup(
                    visible = dynamicsPresetPopupVisible,
                    currentIndex = touchpadState.dynamicsPresetIndex,
                    customPresets = customPresets,
                    onPresetConfirmed = onDynamicsPresetConfirmed,
                    onDismiss = onDynamicsPresetDismiss,
                    onAddCustomPreset = onAddCustomPreset,
                    onEditCustomPreset = onEditCustomPreset,
                    onDeleteCustomPreset = onDeleteCustomPreset,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                )

                // Phase 4.4.8: 모드 프리셋 팝업 오버레이
                // 항상 렌더링하고 visible 파라미터로 제어 (exit 애니메이션 보장)
                ModePresetPopup(
                    visible = modePresetPopupVisible,
                    currentIndex = touchpadState.modePresetIndex,
                    onPresetConfirmed = onModePresetConfirmed,
                    onDismiss = onModePresetDismiss,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                )

            }

            // ── 우측: Actions 패널 (36% / 40%) ──
            ActionsPanel(
                modifier = Modifier
                    .weight(actionsPanelWeight)
                    .fillMaxHeight()
            )
        }
    }
}

// ============================================================
// Actions 패널 (우측, LazyColumn 기반)
// ============================================================

/**
 * Actions 패널: 특수 키, 단축키, 매크로
 *
 * Phase 4.2.2: 기본 구조 구현 (그룹 헤더만)
 * Phase 4.2.3+: 각 그룹의 실제 버튼 구현
 */
@Composable
private fun ActionsPanel(
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .background(Color(0xFF121212), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // ── Special Keys 그룹 ──
        item {
            Text(
                text = "특수 키",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFFFFF),
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }
        item {
            SpecialKeysGrid()
        }

        // ── Shortcuts 그룹 ──
        item {
            Text(
                text = "단축키",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFFFFF),
                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
            )
        }
        item {
            ShortcutsGrid()
        }

        // ── Macros 그룹 ──
        item {
            Text(
                text = "매크로",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFFFFF),
                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
            )
        }
        item {
            MacrosPlaceholder()
        }
    }
}

// ============================================================
// Special Keys 그룹 (Phase 4.2.3)
// ============================================================

/**
 * Special Keys 2열 그리드
 *
 * 8개 키: Esc, Tab, Enter, Backspace, Delete, Space, Home, End
 * - 모두 stickyHoldEnabled=false (자연 홀드)
 * - 길게 누르면 PC OS가 자체적으로 키 반복 처리 (물리 키보드와 동일)
 *
 */
@Composable
private fun SpecialKeysGrid() {
    val keys = listOf(
        "Esc" to KEY_ESC,
        "Tab" to KEY_TAB,
        "Enter" to KEY_ENTER,
        "⌫" to KEY_BACKSPACE,
        "Del" to KEY_DELETE,
        "Space" to KEY_SPACE,
        "Home" to KEY_HOME,
        "End" to KEY_END
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        keys.chunked(2).forEach { rowKeys ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rowKeys.forEach { (label, keyCode) ->
                    KeyboardKeyButton(
                        keyLabel = label,
                        keyCode = keyCode,
                        stickyHoldEnabled = false,
                        onKeyPressed = { code ->
                            val frame = ClickDetector.createKeyboardFrame(
                                activeModifierKeys = emptySet(),
                                keyCode1 = code
                            )
                            ClickDetector.sendFrame(frame)
                        },
                        onKeyReleased = { _ ->
                            val frame = ClickDetector.createKeyboardFrame(
                                activeModifierKeys = emptySet(),
                                keyCode1 = 0u
                            )
                            ClickDetector.sendFrame(frame)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                    )
                }
                if (rowKeys.size < 2) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// ============================================================
// Shortcuts 그룹 (Phase 4.2.4)
// ============================================================

/**
 * Shortcuts 2열 그리드
 *
 * 8개 단축키: Ctrl+C, Ctrl+V, Ctrl+S, Ctrl+Z, Ctrl+Shift+Z, Ctrl+X, Alt+Tab, Win+D
 * - TAP 모드: 탭 → Modifier↓ → Key↓ → Key↑ → Modifier↑ 순차 전송
 * - HOLD 모드: Alt+Tab — 누름 동안 유지, 뗌 시 해제
 * - 150ms 디바운스 (Win+D는 500ms)
 *
 */
@Composable
private fun ShortcutsGrid() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        DEFAULT_SHORTCUTS.chunked(2).forEach { rowShortcuts ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rowShortcuts.forEach { shortcutDef ->
                    ShortcutButton(
                        shortcutDef = shortcutDef,
                        onShortcutTriggered = { mod, key ->
                            val frame = ClickDetector.createKeyboardFrame(
                                activeModifierKeys = if (mod != 0u.toUByte()) setOf(mod) else emptySet(),
                                keyCode1 = key
                            )
                            ClickDetector.sendFrame(frame)
                        },
                        onShortcutReleased = { _, _ ->
                            val frame = ClickDetector.createKeyboardFrame(
                                activeModifierKeys = emptySet(),
                                keyCode1 = 0u
                            )
                            ClickDetector.sendFrame(frame)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                    )
                }
                if (rowShortcuts.size < 2) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// ============================================================
// Macros Placeholder (Phase 4.2.5)
// ============================================================

/**
 * Macros 세로 리스트 (Disabled 상태)
 *
 * 3개 매크로 버튼: Macro 1, Macro 2, Macro 3
 * - 항상 Disabled 상태 (#C2C2C2, alpha 0.6)
 * - 탭 시 아무 동작 없음
 * - PlayArrow 아이콘 표시
 */
@Composable
private fun MacrosPlaceholder() {
    val macros = listOf("Macro 1", "Macro 2", "Macro 3")

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        macros.forEach { label ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .alpha(0.6f)
                    .background(Color(0xFF2A2A2A), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Color(0xFFC2C2C2),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        color = Color(0xFFC2C2C2)
                    )
                }
            }
        }
    }
}

// ============================================================
// Page 2: 테스트 터치패드 (제어 버튼 없는 풀스크린 터치패드)
// ============================================================

@Composable
private fun Page2TestTouchpad(
    touchpadState: TouchpadState,
    edgeZoneAssignment: TouchpadEdgeZoneAssignment = TouchpadEdgeZoneAssignment.default(),
    onEdgeZoneAssignmentChange: (TouchpadEdgeZoneAssignment) -> Unit = {},
    customPresets: List<CustomPointerDynamicsPreset> = emptyList(),
    onTouchpadStateChange: (TouchpadState) -> Unit = {},
    buttonVisibility: TouchpadButtonVisibility = TouchpadButtonVisibility.defaultFor(TouchpadIds.standardPage(1)),
    onDpiLongPress: () -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxSize()) {
        TouchpadWrapper(
            touchpadId = TouchpadIds.standardPage(1),
            bridgeMode = BridgeMode.STANDARD,
            touchpadState = touchpadState,
            edgeZoneAssignment = edgeZoneAssignment,
            onEdgeZoneAssignmentChange = onEdgeZoneAssignmentChange,
            customPresets = customPresets,
            onTouchpadStateChange = onTouchpadStateChange,
            buttonVisibility = buttonVisibility,
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = Color(0xFF1A1A1A),
                    shape = RoundedCornerShape(12.dp)
                )
        )
        if (buttonVisibility.showControlButtons) {
            ControlButtonContainer(
                touchpadState = touchpadState,
                onStateChange = onTouchpadStateChange,
                onDpiLongPress = onDpiLongPress,
                config = buttonVisibility.controlButtonConfig,
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.TopCenter)
            )
        }
    }
}

// ============================================================
// Page 3: 키보드 (Placeholder - Phase 4.5에서 구현)
// ============================================================

@Composable
private fun Page3KeyboardPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Page 3",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2196F3)
            )
            Text(
                text = "키보드 중심 레이아웃",
                fontSize = 14.sp,
                color = Color(0xFFC2C2C2)
            )
            Text(
                text = "(Phase 4.5에서 구현 예정)",
                fontSize = 12.sp,
                color = Color(0xFF888888),
                fontWeight = FontWeight.Light
            )
        }
    }
}

// ============================================================
// Page 4: 마인크래프트 (Placeholder - Phase 4.6에서 구현)
// ============================================================

@Composable
private fun Page4MinecraftPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Page 4",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2196F3)
            )
            Text(
                text = "마인크래프트 특화",
                fontSize = 14.sp,
                color = Color(0xFFC2C2C2)
            )
            Text(
                text = "(Phase 4.6에서 구현 예정)",
                fontSize = 12.sp,
                color = Color(0xFF888888),
                fontWeight = FontWeight.Light
            )
        }
    }
}

// ============================================================
// Page 5: 설정
// ============================================================

@Composable
private fun Page5Settings(
    touchpadState: TouchpadState,
    onTouchpadStateChange: (TouchpadState) -> Unit,
    standardAssignments: Map<Int, TouchpadEdgeZoneAssignment> = emptyMap(),
    selectedZonePage: Int = 0,
    onSelectedZonePageChange: (Int) -> Unit = {},
    onOpenZoneEditor: () -> Unit = {},
    onOpenZonePresetPopup: () -> Unit = {},
    standardButtonVisibility: Map<Int, TouchpadButtonVisibility> = emptyMap(),
    onButtonVisibilityChange: (Int, TouchpadButtonVisibility) -> Unit = { _, _ -> }
) {
    val sortedPages = standardAssignments.keys.sorted()
    val currentAssignment = standardAssignments[selectedZonePage] ?: TouchpadEdgeZoneAssignment.default()
    val currentVisibility = standardButtonVisibility[selectedZonePage] ?: TouchpadButtonVisibility.defaultFor(TouchpadIds.standardPage(selectedZonePage))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E), RoundedCornerShape(12.dp))
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "환경 설정",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFFFFF)
                )
            }

            item {
                Text(
                    text = "엣지 조작 방식",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFAAAAAA),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            item {
                SettingsEdgeInteractionModeSection(
                    currentMode = touchpadState.edgeInteractionMode,
                    onModeSelected = { mode ->
                        onTouchpadStateChange(touchpadState.copy(edgeInteractionMode = mode))
                    }
                )
            }

            // 터치패드 페이지 셀렉터 (페이지가 2개 이상일 때만 표시, 엣지 존 + 버튼 표시 두 섹션이 공유)
            if (sortedPages.size > 1) {
                item {
                    Text(
                        text = "터치패드",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFAAAAAA)
                    )
                }
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        sortedPages.forEach { pageIdx ->
                            val isSelected = pageIdx == selectedZonePage
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFF2979FF).copy(alpha = 0.2f) else Color(0xFF2A2A2A))
                                    .border(
                                        width = if (isSelected) 1.5.dp else 0.5.dp,
                                        color = if (isSelected) Color(0xFF2979FF) else Color(0xFF444444),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onSelectedZonePageChange(pageIdx) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "페이지 ${pageIdx + 1}",
                                    fontSize = 13.sp,
                                    color = if (isSelected) Color(0xFF2979FF) else Color(0xFFCCCCCC)
                                )
                            }
                        }
                    }
                }
            }

            // 존 프리셋 + 편집기 진입 항목 (ZONE 모드 선택 시에만 표시)
            if (touchpadState.edgeInteractionMode == EdgeInteractionMode.ZONE) {
                item {
                    ZonePresetSelectorRow(
                        currentPresetId = currentAssignment.presetId,
                        onClick = onOpenZonePresetPopup
                    )
                }
                item {
                    val zoneCount = currentAssignment.config.run {
                        topZones.size + bottomZones.size + leftZones.size + rightZones.size
                    }
                    ZoneEditorEntryRow(
                        zoneCount = zoneCount,
                        onClick = onOpenZoneEditor
                    )
                }
            }

            // 버튼 표시 섹션
            item {
                Text(
                    text = "버튼 표시",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFAAAAAA),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            item {
                SettingsButtonVisibilitySection(
                    visibility = currentVisibility,
                    onVisibilityChange = { updated ->
                        onButtonVisibilityChange(selectedZonePage, updated)
                    }
                )
            }
        }
    }
}

@Composable
private fun SettingsEdgeInteractionModeSection(
    currentMode: EdgeInteractionMode,
    onModeSelected: (EdgeInteractionMode) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        EdgeInteractionMode.entries.forEach { mode ->
            val isSelected = currentMode == mode
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isSelected) Color(0xFF2A2A2A) else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick = { onModeSelected(mode) }
                )
                Text(
                    text = when (mode) {
                        EdgeInteractionMode.LEGACY_POPUP -> "기존 팝업 방식 (5단계)"
                        EdgeInteractionMode.ZONE -> "엣지 존 방식"
                    },
                    fontSize = 14.sp,
                    color = Color(0xFFEEEEEE)
                )
            }
        }
    }
}

// ============================================================
// DPI 레벨 SharedPreferences 저장/복원 (Phase 4.3.6)
// ============================================================

private const val PREF_NAME = "touchpad_prefs"
private const val KEY_DPI_LEVEL = "dpi_level"

private fun loadDpiLevel(context: Context): DpiLevel {
    val name = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        .getString(KEY_DPI_LEVEL, DpiLevel.NORMAL.name) ?: DpiLevel.NORMAL.name
    return DpiLevel.entries.firstOrNull { it.name == name } ?: DpiLevel.NORMAL
}

private fun saveDpiLevel(context: Context, level: DpiLevel) {
    context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_DPI_LEVEL, level.name)
        .apply()
}

// ============================================================
// 엣지 조작 방식 SharedPreferences 저장/복원 (Phase 4.6.1)
// ============================================================

private const val KEY_EDGE_INTERACTION_MODE = "edge_interaction_mode"

private fun loadEdgeInteractionMode(context: Context): EdgeInteractionMode {
    val name = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        .getString(KEY_EDGE_INTERACTION_MODE, EdgeInteractionMode.LEGACY_POPUP.name)
        ?: EdgeInteractionMode.LEGACY_POPUP.name
    return EdgeInteractionMode.entries.firstOrNull { it.name == name } ?: EdgeInteractionMode.LEGACY_POPUP
}

private fun saveEdgeInteractionMode(context: Context, mode: EdgeInteractionMode) {
    context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_EDGE_INTERACTION_MODE, mode.name)
        .apply()
}


// ============================================================
// 존 프리셋 선택 행 (Phase 4.6.3)
// ============================================================

@Composable
private fun ZonePresetSelectorRow(
    currentPresetId: String?,
    onClick: () -> Unit
) {
    val presetLabel = if (currentPresetId == null) {
        "사용자 정의 (직접 편집)"
    } else {
        val preset = com.bridgeone.app.ui.common.BUILT_IN_EDGE_ZONE_PRESETS.find { it.id == currentPresetId }
        if (preset != null) preset.name else "커스텀"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF2A2A2A), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("엣지 존 프리셋", fontSize = 14.sp, color = Color(0xFFEEEEEE))
            Text(presetLabel, fontSize = 12.sp, color = Color(0xFF888888))
        }
        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = null,
            tint = Color(0xFF888888),
            modifier = Modifier.size(20.dp)
        )
    }
}

// ============================================================
// 존 편집기 진입 행 (Phase 4.6.2)
// ============================================================

@Composable
private fun ZoneEditorEntryRow(
    zoneCount: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF2A2A2A), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("엣지 존 편집", fontSize = 14.sp, color = Color(0xFFEEEEEE))
            Text("${zoneCount}개 존 설정됨", fontSize = 12.sp, color = Color(0xFF888888))
        }
        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = null,
            tint = Color(0xFF888888),
            modifier = Modifier.size(20.dp)
        )
    }
}

// ============================================================
// 버튼 표시 설정 섹션
// ============================================================

@Composable
private fun SettingsButtonVisibilitySection(
    visibility: TouchpadButtonVisibility,
    onVisibilityChange: (TouchpadButtonVisibility) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF2A2A2A), RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // 제어 버튼 마스터 토글
        SettingsToggleRow(
            label = "제어 버튼",
            checked = visibility.showControlButtons,
            onCheckedChange = { onVisibilityChange(visibility.copy(showControlButtons = it)) }
        )
        // 제어 버튼 마스터 ON일 때만 개별 토글 노출
        if (visibility.showControlButtons) {
            val config = visibility.controlButtonConfig
            SettingsToggleRow(
                label = "  클릭 모드 버튼",
                checked = config.showClickMode,
                onCheckedChange = {
                    onVisibilityChange(visibility.copy(controlButtonConfig = config.copy(showClickMode = it)))
                }
            )
            SettingsToggleRow(
                label = "  이동 모드 버튼",
                checked = config.showMoveMode,
                onCheckedChange = {
                    onVisibilityChange(visibility.copy(controlButtonConfig = config.copy(showMoveMode = it)))
                }
            )
            SettingsToggleRow(
                label = "  스크롤 모드 버튼",
                checked = config.showScrollMode,
                onCheckedChange = {
                    onVisibilityChange(visibility.copy(controlButtonConfig = config.copy(showScrollMode = it)))
                }
            )
            SettingsToggleRow(
                label = "  커서 모드 버튼",
                checked = config.showCursorMode,
                onCheckedChange = {
                    onVisibilityChange(visibility.copy(controlButtonConfig = config.copy(showCursorMode = it)))
                }
            )
            SettingsToggleRow(
                label = "  DPI 버튼",
                checked = config.showDpi,
                onCheckedChange = {
                    onVisibilityChange(visibility.copy(controlButtonConfig = config.copy(showDpi = it)))
                }
            )
            SettingsToggleRow(
                label = "  스크롤 감도 버튼",
                checked = config.showScrollSensitivity,
                onCheckedChange = {
                    onVisibilityChange(visibility.copy(controlButtonConfig = config.copy(showScrollSensitivity = it)))
                }
            )
        }
        // 기타 버튼 토글
        SettingsToggleRow(
            label = "포인트 다이나믹스 버튼",
            checked = visibility.showDynamicsButton,
            onCheckedChange = { onVisibilityChange(visibility.copy(showDynamicsButton = it)) }
        )
        SettingsToggleRow(
            label = "모드 프리셋 버튼",
            checked = visibility.showModePresetButton,
            onCheckedChange = { onVisibilityChange(visibility.copy(showModePresetButton = it)) }
        )
        SettingsToggleRow(
            label = "스크롤 위/아래 버튼",
            checked = visibility.showScrollButtons,
            onCheckedChange = { onVisibilityChange(visibility.copy(showScrollButtons = it)) }
        )
    }
}

@Composable
private fun SettingsToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color(0xFFEEEEEE)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
