package com.bridgeone.app.ui.pages

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bridgeone.app.protocol.BridgeMode
import com.bridgeone.app.ui.common.AudioController
import com.bridgeone.app.ui.common.CustomPointerDynamicsPreset
import com.bridgeone.app.ui.common.CustomPresetsRepository
import com.bridgeone.app.ui.common.DYNAMICS_PRESETS
import com.bridgeone.app.ui.common.EdgeZonePresetsRepository
import com.bridgeone.app.ui.common.InputMode
import com.bridgeone.app.ui.common.MACRO_SCRIM_MIN_DISPLAY_MS
import com.bridgeone.app.ui.common.MacroFrameSequencer
import com.bridgeone.app.ui.common.MacroOverlayController
import com.bridgeone.app.ui.common.MODE_PRESETS
import com.bridgeone.app.ui.common.ToastController
import com.bridgeone.app.ui.common.ToastType
import com.bridgeone.app.ui.common.TouchpadButtonVisibility
import com.bridgeone.app.ui.common.TouchpadButtonVisibilityRepository
import com.bridgeone.app.ui.common.TouchpadEdgeZoneAssignment
import com.bridgeone.app.ui.common.TouchpadEdgeZoneAssignmentRepository
import com.bridgeone.app.ui.common.TouchpadIds
import com.bridgeone.app.ui.common.TtsGender
import com.bridgeone.app.ui.common.loadAudioFeedbackEnabled
import com.bridgeone.app.ui.common.loadInputMode
import com.bridgeone.app.ui.common.loadSwipeWrapEdge
import com.bridgeone.app.ui.common.loadTtsGender
import com.bridgeone.app.ui.common.loadTtsRate
import com.bridgeone.app.ui.common.loadZoneMoveMethod
import com.bridgeone.app.ui.common.saveAudioFeedbackEnabled
import com.bridgeone.app.ui.common.saveInputMode
import com.bridgeone.app.ui.common.saveSwipeWrapEdge
import com.bridgeone.app.ui.common.saveZoneMoveMethod
import com.bridgeone.app.ui.common.saveTtsGender
import com.bridgeone.app.ui.common.saveTtsRate
import com.bridgeone.app.ui.components.touchpad.CursorMode
import com.bridgeone.app.ui.components.touchpad.DynamicsCurveEditor
import com.bridgeone.app.ui.components.touchpad.DpiLevel
import com.bridgeone.app.ui.components.touchpad.MacroStep
import com.bridgeone.app.ui.components.touchpad.MouseButton
import com.bridgeone.app.ui.components.touchpad.MouseHoldMode
import com.bridgeone.app.ui.components.touchpad.MultiCursorController
import com.bridgeone.app.ui.components.touchpad.PadModeState
import com.bridgeone.app.ui.components.touchpad.PageNav
import com.bridgeone.app.ui.components.touchpad.ScrollMode
import com.bridgeone.app.ui.components.touchpad.TouchpadState
import com.bridgeone.app.ui.utils.ClickDetector
import com.bridgeone.app.ui.pages.standard.Page1TouchpadActions
import com.bridgeone.app.ui.pages.standard.Page2MultiCursorTouchpad
import com.bridgeone.app.ui.pages.standard.Page3KeyboardPlaceholder
import com.bridgeone.app.ui.pages.standard.Page4MinecraftPlaceholder
import com.bridgeone.app.ui.pages.standard.Page5Settings
import com.bridgeone.app.ui.pages.standard.PageIndicator
import com.bridgeone.app.ui.pages.standard.loadDpiLevel
import com.bridgeone.app.ui.pages.standard.loadEdgeInteractionMode
import com.bridgeone.app.ui.pages.standard.saveDpiLevel
import com.bridgeone.app.ui.pages.standard.saveEdgeInteractionMode
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

    // Phase 4.7.4-C: 페이지 레벨 상태 홀더 (터치패드 상태·모드 이력·마우스 홀드 세션)
    // DpiLevel, EdgeInteractionMode는 SharedPreferences에서 복원해 초기값 주입
    val pageState = remember {
        StandardModePageState(
            TouchpadState(
                dpiLevel = loadDpiLevel(context),
                edgeInteractionMode = loadEdgeInteractionMode(context)
            )
        )
    }

    // Phase 4.8.2: 멀티 커서 상태 홀더. 페이저 바깥에서 1회 생성해 페이지 전환에도 유지된다.
    val multiCursor = remember { MultiCursorController() }

    // 모든 모드 변경을 인터셉트해 의미있는 변화를 히스토리에 push한 뒤 상태 교체.
    // onTouchpadStateChange 콜백 대신 이 람다를 사용한다.
    val recordingOnChange: (TouchpadState) -> Unit = { newState ->
        pageState.changeStateRecordingHistory(newState)
    }

    // 히스토리 스택에서 직전 상태를 pop해 복원. 복원할 게 없으면 토스트.
    val onRestorePrevious: () -> Unit = {
        if (!pageState.restorePrevious()) {
            ToastController.show("이전 모드 및 세팅이 없습니다", ToastType.INFO)
        }
    }

    // 마우스 홀드 세션 상태는 pageState가 보유. 앱 종료(컴포지션 dispose) 시 잔여 홀드 해제.
    DisposableEffect(Unit) {
        onDispose {
            if (pageState.heldMouseButtons.isNotEmpty()) {
                ClickDetector.sendFrame(ClickDetector.createMouseButtonFrame(0u))
            }
        }
    }

    // 단축키 발송 콜백: MacroFrameSequencer.buildShortcut으로 press+release 2프레임 생성
    val onSendShortcut: (Int, List<Int>, Boolean) -> Unit = { modifierBits, keyCodes, _ ->
        MacroFrameSequencer.buildShortcut(modifierBits, keyCodes)
            .forEach { ClickDetector.sendFrame(it) }
    }

    // 매크로 발송 콜백: MacroFrameSequencer.buildMacro로 TimedFrame 시퀀스 미리 생성,
    // 코루틴에서 순차 전송. 스크림 오버레이 + PROGRESS 토스트로 화면 조작 차단.
    val onSendMacro: (List<MacroStep>, Int) -> Unit = { steps, stepDelayMs ->
        val timedFrames = MacroFrameSequencer.buildMacro(steps, stepDelayMs)
        val estimatedMs = timedFrames.sumOf { it.delayAfterMs }
        val displayMs = estimatedMs.coerceAtLeast(MACRO_SCRIM_MIN_DISPLAY_MS)
        MacroOverlayController.show()
        ToastController.show("매크로 실행 중", ToastType.PROGRESS, durationMs = displayMs)
        coroutineScope.launch {
            val startTime = System.currentTimeMillis()
            try {
                timedFrames.forEach { tf ->
                    ClickDetector.sendFrame(tf.frame)
                    if (tf.delayAfterMs > 0L) kotlinx.coroutines.delay(tf.delayAfterMs)
                }
            } finally {
                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed < displayMs) kotlinx.coroutines.delay(displayMs - elapsed)
                MacroOverlayController.dismiss()
                ToastController.dismiss()
            }
        }
    }

    // 마우스 홀드 콜백: pageState에서 상태 갱신 후 버튼 비트 전송 + 토스트 (사이드이펙트는 잔류)
    val onMouseHoldToggle: (MouseButton, MouseHoldMode) -> Unit = { button, mode ->
        val buttonsByte = pageState.toggleMouseHold(button, mode)
        ClickDetector.sendFrame(ClickDetector.createMouseButtonFrame(buttonsByte))
        val isOn = pageState.heldMouseButtons.contains(button)
        val buttonLabel = when (button) {
            MouseButton.LEFT   -> "좌클릭"
            MouseButton.RIGHT  -> "우클릭"
            MouseButton.MIDDLE -> "중간클릭"
        }
        ToastController.show("$buttonLabel 홀드 ${if (isOn) "ON" else "OFF"}", ToastType.INFO)
    }

    // 페이지 순환 전환 콜백 (무한 페이저라 ±1만으로 0↔마지막 순환 자동 성립)
    val onCyclePage: (PageNav) -> Unit = { direction ->
        val delta = if (direction == PageNav.NEXT) 1 else -1
        coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + delta) }
    }

    // 특정 페이지 점프 콜백 (인덱스는 저장 시점 절대값 유지, 실행 시 클램프)
    val onJumpToPage: (Int) -> Unit = { logical ->
        val safePage = logical.coerceIn(0, PAGE_COUNT - 1)
        val target = pagerState.currentPage - (pagerState.currentPage % PAGE_COUNT) + safePage
        coroutineScope.launch { pagerState.animateScrollToPage(target) }
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

    // 앱 전체 조작 방식 (일반/스와이프). SharedPreferences에서 복원.
    var inputMode by remember { mutableStateOf(loadInputMode(context)) }
    LaunchedEffect(inputMode) {
        saveInputMode(context, inputMode)
    }

    // 스와이프 끝점 wrap 여부. SharedPreferences에서 복원.
    var swipeWrapEdge by remember { mutableStateOf(loadSwipeWrapEdge(context)) }
    LaunchedEffect(swipeWrapEdge) {
        saveSwipeWrapEdge(context, swipeWrapEdge)
    }

    // 존 이동 방식 (탭/드래그 앤 드롭). SharedPreferences에서 복원.
    var zoneMoveMethod by remember { mutableStateOf(loadZoneMoveMethod(context)) }
    LaunchedEffect(zoneMoveMethod) {
        saveZoneMoveMethod(context, zoneMoveMethod)
    }

    // 존 음성 안내 ON/OFF. SharedPreferences에서 복원. (Phase 4.6.4)
    var audioFeedbackEnabled by remember { mutableStateOf(loadAudioFeedbackEnabled(context)) }
    LaunchedEffect(audioFeedbackEnabled) {
        saveAudioFeedbackEnabled(context, audioFeedbackEnabled)
        AudioController.setEnabled(audioFeedbackEnabled)
    }

    // TTS 말하기 속도. SharedPreferences에서 복원. (Phase 4.6.4)
    var ttsRate by remember { mutableStateOf(loadTtsRate(context)) }
    LaunchedEffect(ttsRate) {
        saveTtsRate(context, ttsRate)
        AudioController.setRate(ttsRate)
    }

    // TTS 음성 성별. SharedPreferences에서 복원. (Phase 4.6.4)
    var ttsGender by remember { mutableStateOf(loadTtsGender(context)) }
    LaunchedEffect(ttsGender) {
        saveTtsGender(context, ttsGender)
        AudioController.setGender(ttsGender)
    }

    // 터치패드별 버튼 표시 설정
    val buttonVisibilityRepo = remember { TouchpadButtonVisibilityRepository(context) }
    var standardButtonVisibility by remember {
        mutableStateOf(standardTouchpadPages.associateWith { buttonVisibilityRepo.load(TouchpadIds.standardPage(it)) })
    }

    // DPI 레벨(사전 정의 값)이 변경될 때 SharedPreferences에 저장
    LaunchedEffect(pageState.touchpadState.dpiLevel) {
        saveDpiLevel(context, pageState.touchpadState.dpiLevel)
    }

    // 엣지 조작 방식이 변경될 때 SharedPreferences에 저장 (Phase 4.6.1)
    LaunchedEffect(pageState.touchpadState.edgeInteractionMode) {
        saveEdgeInteractionMode(context, pageState.touchpadState.edgeInteractionMode)
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

    // Phase 4.5.16: 커스텀 다이나믹스 프리셋 상태
    val customPresetsRepo = remember { CustomPresetsRepository(context) }
    var customPresets by remember { mutableStateOf<List<CustomPointerDynamicsPreset>>(emptyList()) }
    LaunchedEffect(Unit) { customPresets = customPresetsRepo.loadAll() }

    // 커스텀 단축키 프리셋 저장소
    val customShortcutPresetsRepo = remember { com.bridgeone.app.ui.common.CustomShortcutPresetsRepository(context) }
    // 커스텀 매크로 프리셋 저장소
    val customMacroPresetsRepo = remember { com.bridgeone.app.ui.common.CustomMacroPresetsRepository(context) }

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

    // Phase 4.8.2: 커서 수 선택 팝업 상태
    var cursorCountPopupVisible by remember { mutableStateOf(false) }

    // 페이지 전환 시 팝업 취소 (커스텀 값 미적용)
    LaunchedEffect(pagerState.currentPage) {
        if (dpiAdjustPopupVisible) dpiAdjustPopupVisible = false
        if (dynamicsPresetPopupVisible) dynamicsPresetPopupVisible = false
        if (modePresetPopupVisible) modePresetPopupVisible = false
        if (curveEditorVisible) curveEditorVisible = false
        if (cursorCountPopupVisible) cursorCountPopupVisible = false
    }

    // 스크롤 모드 전환 시 다이나믹스 팝업 취소 (Phase 4.3.8)
    LaunchedEffect(pageState.touchpadState.scrollMode) {
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
        val isScrollActive = pageState.touchpadState.scrollMode != ScrollMode.OFF
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
                userScrollEnabled = pageState.touchpadState.scrollMode == ScrollMode.OFF,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp, vertical = 8.dp)
            ) { page ->
                when (page % PAGE_COUNT) {
                    0 -> Page1TouchpadActions(
                        touchpadState = pageState.touchpadState,
                        edgeZoneAssignment = standardAssignments[0] ?: TouchpadEdgeZoneAssignment.default(),
                        onEdgeZoneAssignmentChange = { updated -> standardAssignments = standardAssignments + (0 to updated) },
                        customPresets = customPresets,
                        onTouchpadStateChange = recordingOnChange,
                        onRestorePrevious = onRestorePrevious,
                        onSendShortcut = onSendShortcut,
                        onSendMacro = onSendMacro,
                        onMouseHoldToggle = onMouseHoldToggle,
                        onCyclePage = onCyclePage,
                        onJumpToPage = onJumpToPage,
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
                            pageState.touchpadState = if (matchedLevel != null) {
                                pageState.touchpadState.copy(dpiLevel = matchedLevel, customDpiMultiplier = null)
                            } else {
                                pageState.touchpadState.copy(customDpiMultiplier = value)
                            }
                        },
                        onDpiAdjustDismiss = { dpiAdjustPopupVisible = false },
                        onDynamicsPresetConfirmed = { index ->
                            dynamicsPresetPopupVisible = false
                            pageState.touchpadState = pageState.touchpadState.copy(dynamicsPresetIndex = index)
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
                            if (pageState.touchpadState.dynamicsPresetIndex >= DYNAMICS_PRESETS.size + customPresets.size) {
                                pageState.touchpadState = pageState.touchpadState.copy(dynamicsPresetIndex = 0)
                            }
                        },
                        onModePresetConfirmed = { index ->
                            modePresetPopupVisible = false
                            val preset = MODE_PRESETS[index]
                            pageState.touchpadState = pageState.touchpadState.copy(
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
                    1 -> Page2MultiCursorTouchpad(
                        touchpadState = pageState.touchpadState,
                        edgeZoneAssignment = standardAssignments[1] ?: TouchpadEdgeZoneAssignment.default(),
                        onEdgeZoneAssignmentChange = { updated -> standardAssignments = standardAssignments + (1 to updated) },
                        customPresets = customPresets,
                        onTouchpadStateChange = recordingOnChange,
                        onRestorePrevious = onRestorePrevious,
                        onSendShortcut = onSendShortcut,
                        onSendMacro = onSendMacro,
                        onMouseHoldToggle = onMouseHoldToggle,
                        onCyclePage = onCyclePage,
                        onJumpToPage = onJumpToPage,
                        buttonVisibility = standardButtonVisibility[1] ?: TouchpadButtonVisibility.defaultFor(TouchpadIds.standardPage(1)),
                        onDpiLongPress = { dpiAdjustPopupVisible = true },
                        multiCursorState = multiCursor.state,
                        onCursorModeClick = {
                            if (multiCursor.state.isEnabled) {
                                multiCursor.disable()
                                pageState.touchpadState = pageState.touchpadState.copy(cursorMode = CursorMode.SINGLE)
                            } else {
                                cursorCountPopupVisible = true
                            }
                        },
                        onActivePadModeChange = { newState ->
                            multiCursor.updateActivePadMode {
                                it.copy(
                                    clickMode = newState.clickMode,
                                    moveMode = newState.moveMode,
                                    scrollMode = newState.scrollMode,
                                    dpi = newState.dpiLevel
                                )
                            }
                        },
                        cursorCountPopupVisible = cursorCountPopupVisible,
                        onCursorCountSelected = { count ->
                            cursorCountPopupVisible = false
                            val seed = PadModeState(
                                clickMode = pageState.touchpadState.clickMode,
                                moveMode = pageState.touchpadState.moveMode,
                                scrollMode = pageState.touchpadState.scrollMode,
                                dpi = pageState.touchpadState.dpiLevel
                            )
                            multiCursor.enable(count, seed)
                            pageState.touchpadState = pageState.touchpadState.copy(cursorMode = CursorMode.MULTI)
                        },
                        onCursorCountDismiss = { cursorCountPopupVisible = false },
                        onModePresetLongPress = { modePresetPopupVisible = true },
                        modePresetPopupVisible = modePresetPopupVisible,
                        onModePresetConfirmed = { index ->
                            modePresetPopupVisible = false
                            val preset = MODE_PRESETS[index]
                            if (multiCursor.state.isEnabled) {
                                multiCursor.updateActivePadMode {
                                    it.copy(
                                        clickMode = preset.padModeState.clickMode,
                                        moveMode = preset.padModeState.moveMode,
                                        scrollMode = preset.padModeState.scrollMode,
                                        dpi = preset.padModeState.dpi
                                    )
                                }
                                pageState.touchpadState = pageState.touchpadState.copy(
                                    dynamicsPresetIndex = preset.dynamicsPresetIndex,
                                    modePresetIndex = index
                                )
                            } else {
                                pageState.touchpadState = pageState.touchpadState.copy(
                                    clickMode = preset.padModeState.clickMode,
                                    moveMode = preset.padModeState.moveMode,
                                    scrollMode = preset.padModeState.scrollMode,
                                    dpiLevel = preset.padModeState.dpi,
                                    customDpiMultiplier = null,
                                    dynamicsPresetIndex = preset.dynamicsPresetIndex,
                                    modePresetIndex = index
                                )
                            }
                        },
                        onModePresetDismiss = { modePresetPopupVisible = false }
                    )
                    2 -> Page3KeyboardPlaceholder()
                    3 -> Page4MinecraftPlaceholder()
                    4 -> Page5Settings(
                        touchpadState = pageState.touchpadState,
                        onTouchpadStateChange = recordingOnChange,
                        inputMode = inputMode,
                        onInputModeChange = { inputMode = it },
                        swipeWrapEdge = swipeWrapEdge,
                        onSwipeWrapEdgeChange = { swipeWrapEdge = it },
                        zoneMoveMethod = zoneMoveMethod,
                        onZoneMoveMethodChange = { zoneMoveMethod = it },
                        audioFeedbackEnabled = audioFeedbackEnabled,
                        onAudioFeedbackEnabledChange = { audioFeedbackEnabled = it },
                        ttsRate = ttsRate,
                        onTtsRateChange = { ttsRate = it },
                        ttsGender = ttsGender,
                        onTtsGenderChange = { ttsGender = it },
                        standardAssignments = standardAssignments,
                        selectedZonePage = selectedZonePage,
                        onSelectedZonePageChange = { selectedZonePage = it },
                        onOpenZoneEditor = { showZoneEditor = true },
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
            customPresets = customPresets,
            customPresetsRepo = customPresetsRepo,
            onCustomPresetsChange = { customPresets = it },
            customShortcutPresetsRepo = customShortcutPresetsRepo,
            customMacroPresetsRepo = customMacroPresetsRepo,
            onSave = { newConfig, presetId ->
                standardAssignments = standardAssignments + (selectedZonePage to TouchpadEdgeZoneAssignment(newConfig, presetId))
                showZoneEditor = false
            },
            onBack = { showZoneEditor = false },
            pageCount = PAGE_COUNT
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
                        pageState.touchpadState = pageState.touchpadState.copy(dynamicsPresetIndex = newIndex)
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
