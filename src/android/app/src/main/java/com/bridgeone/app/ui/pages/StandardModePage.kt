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
import androidx.compose.runtime.CompositionLocalProvider
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
import android.util.Log
import com.bridgeone.app.protocol.BridgeMode
import com.bridgeone.app.protocol.MultiCursorCommand
import com.bridgeone.app.usb.UsbSerialManager
import com.bridgeone.app.ui.common.AudioController
import com.bridgeone.app.ui.common.CustomPointerDynamicsPreset
import com.bridgeone.app.ui.common.CustomPresetsRepository
import com.bridgeone.app.ui.common.DYNAMICS_PRESETS
import com.bridgeone.app.ui.common.EdgeZonePresetsRepository
import com.bridgeone.app.ui.common.InputMode
import com.bridgeone.app.ui.common.LocalInputMode
import com.bridgeone.app.ui.common.MACRO_SCRIM_MIN_DISPLAY_MS
import com.bridgeone.app.ui.common.MacroFrameSequencer
import com.bridgeone.app.ui.common.MacroOverlayController
import com.bridgeone.app.ui.common.MODE_PRESETS
import com.bridgeone.app.ui.common.MultiCursorConstants
import com.bridgeone.app.ui.common.loadPadLabels
import com.bridgeone.app.ui.common.savePadLabels
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
import com.bridgeone.app.ui.components.touchpad.EdgeZoneAction
import com.bridgeone.app.ui.components.touchpad.MacroStep
import com.bridgeone.app.ui.components.touchpad.MULTI_CURSOR_COUNT_MIN
import com.bridgeone.app.ui.components.touchpad.MULTI_CURSOR_COUNT_MAX
import com.bridgeone.app.ui.components.touchpad.MouseButton
import com.bridgeone.app.ui.components.touchpad.MouseHoldMode
import com.bridgeone.app.ui.components.touchpad.MultiCursorController
import com.bridgeone.app.ui.components.touchpad.MultiCursorLayoutMode
import com.bridgeone.app.ui.components.touchpad.PadModeState
import com.bridgeone.app.ui.components.touchpad.PageNav
import com.bridgeone.app.ui.components.touchpad.ScrollMode
import com.bridgeone.app.ui.components.touchpad.TouchpadState
import com.bridgeone.app.ui.utils.ClickDetector
import com.bridgeone.app.ui.pages.standard.Page1TouchpadActions
import com.bridgeone.app.ui.pages.standard.Page2MultiCursorTouchpad
import com.bridgeone.app.ui.pages.standard.Page3AbsolutePointing
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
 * Phase 4.9.1: HorizontalPager 기반 6페이지 시스템
 * - Page 0: 터치패드 + Actions (상대좌표)
 * - Page 1: 멀티 커서 터치패드
 * - Page 2: 절대좌표 패드 (Phase 4.9에서 구현)
 * - Page 3: 키보드 (Phase 4.5에서 구현)
 * - Page 4: 마인크래프트 (Phase 4.6에서 구현)
 * - Page 5: 설정
 * - 하단 페이지 인디케이터 (닷 6개)
 */
private const val TAG = "StandardModePage"

private const val PAGE_COUNT = 6
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
    // Phase 4.8.10: 패드 커스텀 라벨을 SharedPreferences에서 복원해 초기값으로 주입.
    val multiCursor = remember { MultiCursorController(initialPadLabels = loadPadLabels(context)) }

    // Phase 4.8.10: 이름 편집 팝업 대상 패드 인덱스 (null = 숨김)
    var padLabelEditorTarget by remember { mutableStateOf<Int?>(null) }

    // Phase 4.8.7: disable() 시 cursorCount가 0으로 리셋되므로, ToggleMultiCursor 재활성화 시
    // 복원할 마지막 커서 수를 별도로 기억한다. 기본값: MULTI_CURSOR_COUNT_MIN(2)
    var lastMultiCursorCount by remember { mutableStateOf(MULTI_CURSOR_COUNT_MIN) }

    // Phase 4.8.8: 패드별 프리셋 매핑. 사용자가 팝업에서 한 번도 확정하지 않았다면(null) 모든
    // 패드의 기본값은 싱글 커서 모드에서 현재 사용 중인 프리셋을 따라간다. 팝업에서 확정하면
    // 그 값이 세션 동안 유지되며(SharedPreferences 영속 없음), 앱 재시작 시 다시 null로 복귀한다.
    var padPresetMappingOverride by remember { mutableStateOf<List<Int?>?>(null) }
    val padPresetMapping = padPresetMappingOverride
        ?: List(MULTI_CURSOR_COUNT_MAX) { pageState.touchpadState.modePresetIndex }

    // Phase 4.8.5: 멀티 커서 서버(Windows) 명령 전송 훅. 서버가 없어도(ESSENTIAL) 앱 내부
    // 상태만으로 완결 동작해야 하므로 전송을 스킵하고 로그만 남긴다. 실제 UART write는 Phase 5.
    val sendMultiCursorCommand: (String) -> Unit = { json ->
        if (UsbSerialManager.bridgeMode.value == BridgeMode.STANDARD) {
            Log.d(TAG, "multiCursorCommand: $json")
        } else {
            Log.d(TAG, "multiCursorCommand skipped (ESSENTIAL): $json")
        }
    }

    // Phase 4.8.7: 멀티 커서 활성화(현재 touchpadState를 시드로 사용).
    // Phase 4.8.8: 패드별로 padPresetMapping에 지정된 프리셋을 시드로 사용한다. 미지정 패드는
    // 이미 활성 중이던 패드라면 그 상태를 보존하고(팝업에서 개수만 바꾼 경우), 신규 패드거나
    // 애초에 비활성이었다면 현재 touchpadState를 시드로 사용한다(하위 호환).
    val enableMultiCursor: (Int) -> Unit = { count ->
        val safeCount = count.coerceIn(MULTI_CURSOR_COUNT_MIN, MULTI_CURSOR_COUNT_MAX)
        val existingPads = multiCursor.state.padModeStates
        val currentSeed = PadModeState(
            clickMode = pageState.touchpadState.clickMode,
            moveMode = pageState.touchpadState.moveMode,
            scrollMode = pageState.touchpadState.scrollMode,
            dpi = pageState.touchpadState.dpiLevel
        )
        val seeds = List(safeCount) { i ->
            padPresetMapping.getOrNull(i)?.let { MODE_PRESETS[it].padModeState }
                ?: existingPads.getOrNull(i)
                ?: currentSeed
        }
        multiCursor.enable(seeds)
        lastMultiCursorCount = safeCount
        pageState.touchpadState = pageState.touchpadState.copy(cursorMode = CursorMode.MULTI)
        sendMultiCursorCommand(MultiCursorCommand.buildShowVirtualCursor(safeCount))
    }

    // Phase 4.8.7: 멀티 커서 비활성화. 마지막 커서 수는 lastMultiCursorCount에 보존.
    val disableMultiCursor: () -> Unit = {
        lastMultiCursorCount = multiCursor.state.cursorCount
            .coerceIn(MULTI_CURSOR_COUNT_MIN, MULTI_CURSOR_COUNT_MAX)
        multiCursor.disable()
        pageState.touchpadState = pageState.touchpadState.copy(cursorMode = CursorMode.SINGLE)
        sendMultiCursorCommand(MultiCursorCommand.buildHideVirtualCursor())
    }

    // Phase 4.8.7: 커서 수 지정. 비활성 시 활성화, 활성 시 padModeStates 보존한 채 수만 변경.
    // Phase 4.8.8: 늘어난 신규 패드는 padPresetMapping에 지정된 프리셋을 재적용한다(없으면 pad1 상태).
    val setMultiCursorCount: (Int) -> Unit = { count ->
        val safeCount = count.coerceIn(MULTI_CURSOR_COUNT_MIN, MULTI_CURSOR_COUNT_MAX)
        if (multiCursor.state.isEnabled) {
            multiCursor.changeCursorCount(safeCount) { i ->
                padPresetMapping.getOrNull(i)?.let { MODE_PRESETS[it].padModeState }
                    ?: multiCursor.state.padModeStates.firstOrNull() ?: PadModeState()
            }
            lastMultiCursorCount = safeCount
            sendMultiCursorCommand(MultiCursorCommand.buildShowVirtualCursor(safeCount))
        } else {
            enableMultiCursor(safeCount)
        }
    }

    // 엣지 팝업 커서 개수 선택 서브 화면 진입 시 강조할 현재 멀티 커서 수.
    // 활성 중이면 실제 커서 수, 비활성이면 마지막으로 켰던(또는 켤) 수를 사용.
    val currentMultiCursorCount = if (multiCursor.state.isEnabled) multiCursor.state.cursorCount else lastMultiCursorCount

    // Phase 4.8.7: 특정 패드로 전환 + 서버 전송. 비활성이거나 범위 밖 인덱스면 무시.
    val switchToPad: (Int) -> Unit = { index ->
        if (multiCursor.state.isEnabled && index in 0 until multiCursor.state.cursorCount) {
            multiCursor.switchPad(index)
            sendMultiCursorCommand(
                MultiCursorCommand.buildMultiCursorSwitch(
                    touchpadId = MultiCursorCommand.padIndexToTouchpadId(index),
                    cursorPosition = null
                )
            )
        }
    }

    // Phase 4.8.7: 엣지 존 멀티 커서 액션 5종 디스패치. 서버 미연결(ESSENTIAL)에서도
    // sendMultiCursorCommand가 로그만 남기므로 크래시 없이 앱 내부 상태만으로 완결 동작한다.
    val onMultiCursorAction: (EdgeZoneAction) -> Unit = { action ->
        when (action) {
            EdgeZoneAction.ToggleMultiCursor ->
                if (multiCursor.state.isEnabled) disableMultiCursor() else enableMultiCursor(lastMultiCursorCount)
            is EdgeZoneAction.SetCursorCount -> setMultiCursorCount(action.count)
            is EdgeZoneAction.ActivatePad -> switchToPad(action.index)
            is EdgeZoneAction.CyclePad -> if (multiCursor.state.isEnabled) {
                val cursorCount = multiCursor.state.cursorCount
                val current = multiCursor.state.activePadIndex
                val next = if (action.direction == PageNav.NEXT) {
                    (current + 1) % cursorCount
                } else {
                    (current - 1 + cursorCount) % cursorCount
                }
                switchToPad(next)
            }
            EdgeZoneAction.ToggleMultiCursorLayout -> {
                multiCursor.toggleLayoutMode()
                val modeLabel = if (multiCursor.state.layoutMode == MultiCursorLayoutMode.GRID)
                    "그리드 분할" else "직접 전환 버튼"
                ToastController.show("$modeLabel 모드로 전환", ToastType.INFO)
            }
            else -> {}
        }
    }

    // Phase 4.8.10: 패드 이름 확정. 트리밍 후 빈 문자열이면 null로 저장해 번호로 폴백시킨다.
    val onPadLabelConfirmed: (Int, String) -> Unit = { index, label ->
        val trimmed = label.trim().take(MultiCursorConstants.PAD_LABEL_MAX_LENGTH)
        multiCursor.renamePad(index, trimmed.ifBlank { null })
        savePadLabels(context, multiCursor.state.padLabels)
        padLabelEditorTarget = null
        ToastController.show(
            if (trimmed.isBlank()) "패드 ${index + 1} 이름을 초기화했습니다" else "패드 이름을 \"$trimmed\"(으)로 변경했습니다",
            ToastType.SUCCESS
        )
    }

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
    // Phase 4.8.9: 페이지 1(index 1, Page 2 싱글 커서 모드)은 제어 버튼과 TOP이 겹치므로
    // 커스텀 편집 안 한(builtin_default) 경우 로드 시점에 TOP 존을 비운다.
    val standardTouchpadPages = remember { listOf(0, 1) }
    var standardAssignments by remember {
        mutableStateOf(standardTouchpadPages.associateWith { pageIdx ->
            val loaded = assignmentRepo.load(TouchpadIds.standardPage(pageIdx))
            if (pageIdx == 1) loaded.withTopClearedIfDefault() else loaded
        })
    }
    // Page 5 설정에서 현재 선택된 페이지 인덱스 (엣지 존 + 버튼 표시 공유)
    var selectedZonePage by remember { mutableStateOf(0) }

    // Phase 4.8.9: Page 2 멀티 커서 패드별 엣지 존 할당 (패드 인덱스 0~3, 항상 최대치 보유)
    // 패드 0·1은 그리드 어떤 개수든 항상 상단 행이라 제어 버튼과 겹치므로 TOP 기본값을 비운다.
    var page2PadAssignments by remember {
        mutableStateOf((0 until MULTI_CURSOR_COUNT_MAX).associateWith { padIdx ->
            val loaded = assignmentRepo.load(TouchpadIds.standardPage2Pad(padIdx))
            if (padIdx in 0..1) loaded.withTopClearedIfDefault() else loaded
        })
    }
    // 설정에서 편집 중인 패드 인덱스 (selectedZonePage == 1일 때만 사용). -1 = 싱글 모드
    // (standardAssignments[1] 편집), 0~3 = 그리드/직접 전환 패드(page2PadAssignments 편집)
    var selectedZonePad by remember { mutableStateOf(-1) }

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

    // Page 2 패드별 엣지 존 할당이 변경될 때 파일에 저장 (Phase 4.8.9)
    LaunchedEffect(page2PadAssignments) {
        page2PadAssignments.forEach { (padIdx, assignment) ->
            assignmentRepo.save(TouchpadIds.standardPage2Pad(padIdx), assignment)
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
                // 엣지 팝업 표시 방식(직접 터치/스와이프) 자동 결정을 위해 하위 트리에 InputMode 제공
                CompositionLocalProvider(LocalInputMode provides inputMode) {
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
                        padEdgeZoneAssignments = (0 until MULTI_CURSOR_COUNT_MAX).map {
                            page2PadAssignments[it] ?: TouchpadEdgeZoneAssignment.default()
                        },
                        onPadEdgeZoneAssignmentChange = { padIdx, updated ->
                            page2PadAssignments = page2PadAssignments + (padIdx to updated)
                        },
                        customPresets = customPresets,
                        onTouchpadStateChange = recordingOnChange,
                        onRestorePrevious = onRestorePrevious,
                        onSendShortcut = onSendShortcut,
                        onSendMacro = onSendMacro,
                        onMouseHoldToggle = onMouseHoldToggle,
                        onCyclePage = onCyclePage,
                        onJumpToPage = onJumpToPage,
                        onMultiCursorAction = onMultiCursorAction,
                        currentMultiCursorCount = currentMultiCursorCount,
                        buttonVisibility = standardButtonVisibility[1] ?: TouchpadButtonVisibility.defaultFor(TouchpadIds.standardPage(1)),
                        onDpiLongPress = { dpiAdjustPopupVisible = true },
                        multiCursorState = multiCursor.state,
                        onCursorModeClick = {
                            // Phase 4.8.6: 활성 중에도 즉시 해제하지 않고 팝업을 띄워
                            // 현재 수 강조 + 해제 버튼으로 커서 수 변경/해제를 함께 다룬다.
                            cursorCountPopupVisible = true
                        },
                        onCursorModeLongPress = { onMultiCursorAction(EdgeZoneAction.ToggleMultiCursorLayout) },
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
                        onPadSwitch = { index -> switchToPad(index) },
                        cursorCountPopupVisible = cursorCountPopupVisible,
                        padPresetMapping = padPresetMapping,
                        onCursorCountConfirmed = { count, mapping ->
                            cursorCountPopupVisible = false
                            padPresetMappingOverride = mapping
                            enableMultiCursor(count)
                        },
                        onCursorCountDismiss = { cursorCountPopupVisible = false },
                        onCursorCountDisable = {
                            cursorCountPopupVisible = false
                            disableMultiCursor()
                        },
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
                        onModePresetDismiss = { modePresetPopupVisible = false },
                        padLabelEditorTarget = padLabelEditorTarget,
                        onPadLongPress = { index -> padLabelEditorTarget = index },
                        onPadLabelConfirm = onPadLabelConfirmed,
                        onPadLabelDismiss = { padLabelEditorTarget = null }
                    )
                    2 -> Page3AbsolutePointing()
                    3 -> Page3KeyboardPlaceholder()
                    4 -> Page4MinecraftPlaceholder()
                    5 -> Page5Settings(
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
                        page2PadAssignments = page2PadAssignments,
                        selectedZonePad = selectedZonePad,
                        onSelectedZonePadChange = { selectedZonePad = it },
                        onOpenZoneEditor = { showZoneEditor = true },
                        standardButtonVisibility = standardButtonVisibility,
                        onButtonVisibilityChange = { pageIdx, updated ->
                            standardButtonVisibility = standardButtonVisibility + (pageIdx to updated)
                        }
                    )
                }
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
        // Phase 4.8.9: 페이지 2에서 패드(0~3) 선택 시 패드별 assignment, "싱글"(-1) 선택 시
        // 싱글 커서 모드에서 쓰이는 페이지 단위 assignment(standardAssignments[1]), 그 외
        // 페이지는 기존 페이지 단위 assignment 사용
        val targetAssignment = when {
            selectedZonePage == 1 && selectedZonePad == -1 -> standardAssignments[1] ?: TouchpadEdgeZoneAssignment.default()
            selectedZonePage == 1 -> page2PadAssignments[selectedZonePad] ?: TouchpadEdgeZoneAssignment.default()
            else -> standardAssignments[selectedZonePage] ?: TouchpadEdgeZoneAssignment.default()
        }
        // Phase 4.8.9: 패드 0·1과 싱글 모드는 그리드 개수(2/3/4)와 무관하게 항상 상단 행(또는 싱글
        // 전체 화면)이라 제어 버튼과 겹친다. 패드 2·3은 4분할 그리드에서만 존재하며 항상 하단 행이라
        // TOP 엣지가 자유롭다(직접 전환 버튼 모드에서 해당 패드가 활성화되면 실제로는 버튼에 가려질 수
        // 있으나, 버튼이 터치를 우선 소비해 크래시 없이 그 존만 무용지물이 되는 정도라 별도 제약을 두지 않는다).
        val zoneEditorVisibility = standardButtonVisibility[selectedZonePage]
            ?: TouchpadButtonVisibility.defaultFor(TouchpadIds.standardPage(selectedZonePage))
        val zoneEditorDisabledEdges: Map<com.bridgeone.app.ui.components.touchpad.EntryEdge, String> = when {
            !zoneEditorVisibility.showControlButtons -> emptyMap()
            selectedZonePage == 0 -> mapOf(com.bridgeone.app.ui.components.touchpad.EntryEdge.TOP to "제어 버튼")
            selectedZonePage == 1 && selectedZonePad in -1..1 -> mapOf(com.bridgeone.app.ui.components.touchpad.EntryEdge.TOP to "제어 버튼")
            else -> emptyMap()
        }
        com.bridgeone.app.ui.components.touchpad.EdgeZoneEditorScreen(
            initialConfig = targetAssignment.config,
            initialPresetId = targetAssignment.presetId,
            presetsRepo = edgeZonePresetsRepo,
            disabledEdges = zoneEditorDisabledEdges,
            bottomLeftButtonLabel = if (zoneEditorVisibility.showDynamicsButton) "다이나믹스" else null,
            bottomRightButtonLabel = if (zoneEditorVisibility.showModePresetButton) "모드 프리셋" else null,
            customPresets = customPresets,
            customPresetsRepo = customPresetsRepo,
            onCustomPresetsChange = { customPresets = it },
            customShortcutPresetsRepo = customShortcutPresetsRepo,
            customMacroPresetsRepo = customMacroPresetsRepo,
            onSave = { newConfig, presetId ->
                val updated = TouchpadEdgeZoneAssignment(newConfig, presetId)
                when {
                    selectedZonePage == 1 && selectedZonePad == -1 -> standardAssignments = standardAssignments + (1 to updated)
                    selectedZonePage == 1 -> page2PadAssignments = page2PadAssignments + (selectedZonePad to updated)
                    else -> standardAssignments = standardAssignments + (selectedZonePage to updated)
                }
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
