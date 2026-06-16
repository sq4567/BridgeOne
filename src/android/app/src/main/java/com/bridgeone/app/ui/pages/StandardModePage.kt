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
import androidx.compose.runtime.DisposableEffect
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
import com.bridgeone.app.ui.common.AudioController
import com.bridgeone.app.ui.common.CustomPointerDynamicsPreset
import com.bridgeone.app.ui.common.CustomPresetsRepository
import com.bridgeone.app.ui.common.DYNAMICS_PRESETS
import com.bridgeone.app.ui.common.EdgeZonePresetsRepository
import com.bridgeone.app.ui.common.InputMode
import com.bridgeone.app.ui.common.MACRO_INTRA_STEP_PRESS_RELEASE_MS
import com.bridgeone.app.ui.common.MACRO_MAX_HELD_KEYS
import com.bridgeone.app.ui.common.MACRO_SCRIM_MIN_DISPLAY_MS
import com.bridgeone.app.ui.common.MacroOverlayController
import com.bridgeone.app.ui.common.ToastController
import com.bridgeone.app.ui.common.ToastType
import com.bridgeone.app.ui.common.TtsGender
import com.bridgeone.app.ui.common.loadAudioFeedbackEnabled
import com.bridgeone.app.ui.common.loadInputMode
import com.bridgeone.app.ui.common.loadSwipeWrapEdge
import com.bridgeone.app.ui.common.loadTtsGender
import com.bridgeone.app.ui.common.loadTtsRate
import com.bridgeone.app.ui.common.saveAudioFeedbackEnabled
import com.bridgeone.app.ui.common.saveInputMode
import com.bridgeone.app.ui.common.saveSwipeWrapEdge
import com.bridgeone.app.ui.common.saveTtsGender
import com.bridgeone.app.ui.common.saveTtsRate
import com.bridgeone.app.ui.common.MODE_PRESETS
import com.bridgeone.app.ui.common.TouchpadButtonVisibility
import com.bridgeone.app.ui.common.TouchpadButtonVisibilityRepository
import com.bridgeone.app.ui.common.TouchpadEdgeZoneAssignment
import com.bridgeone.app.ui.common.TouchpadEdgeZoneAssignmentRepository
import com.bridgeone.app.ui.common.TouchpadIds
import com.bridgeone.app.ui.components.touchpad.DynamicsCurveEditor
import com.bridgeone.app.ui.components.touchpad.DynamicsPresetPopup
import com.bridgeone.app.ui.components.touchpad.ModeHistoryStack
import com.bridgeone.app.ui.components.touchpad.ModePresetPopup
import com.bridgeone.app.ui.components.touchpad.MacroStep
import com.bridgeone.app.ui.components.touchpad.MacroStepKind
import com.bridgeone.app.ui.components.touchpad.MouseButton
import com.bridgeone.app.ui.components.touchpad.MouseHoldMode
import com.bridgeone.app.ui.components.touchpad.PageNav
import com.bridgeone.app.ui.components.touchpad.ScrollMode
import com.bridgeone.app.ui.components.touchpad.TouchpadState
import com.bridgeone.app.ui.utils.ClickDetector
import com.bridgeone.app.protocol.BridgeFrame
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.text.style.TextAlign

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

    // 모드/세팅 변경 이력 스택 (세션 내 유지, 비영속)
    val historyStack = remember { ModeHistoryStack() }

    // 모든 모드 변경을 인터셉트해 의미있는 변화를 히스토리에 push한 뒤 상태 교체.
    // onTouchpadStateChange 콜백 대신 이 람다를 사용한다.
    val recordingOnChange: (TouchpadState) -> Unit = { newState ->
        if (historyStack.isMeaningfulChange(touchpadState, newState)) {
            historyStack.push(touchpadState)
        }
        touchpadState = newState
    }

    // 히스토리 스택에서 직전 상태를 pop해 복원.
    // recordingOnChange를 거치지 않으므로 복원 자체가 다시 push되는 일이 없다.
    // edgeInteractionMode는 현재 값을 유지(조작 방식이 되돌리기로 바뀌면 혼란 방지).
    val onRestorePrevious: () -> Unit = {
        val prev = historyStack.pop()
        if (prev != null) {
            touchpadState = prev.copy(edgeInteractionMode = touchpadState.edgeInteractionMode)
        } else {
            ToastController.show("이전 모드 및 세팅이 없습니다", ToastType.INFO)
        }
    }

    // 마우스 홀드 토글 세션 상태 (비영속, 앱 종료 시 자동 해제)
    var heldMouseButtons by remember { mutableStateOf(setOf<MouseButton>()) }
    DisposableEffect(Unit) {
        onDispose {
            if (heldMouseButtons.isNotEmpty()) {
                ClickDetector.sendFrame(ClickDetector.createMouseButtonFrame(0u))
            }
        }
    }

    // 단축키 발송 콜백: TAP 의미 (press 직후 release)
    // 현재 프로토콜(BridgeFrame)은 키코드 2개까지 지원 — keyCodes[0..1]만 전송
    val onSendShortcut: (Int, List<Int>, Boolean) -> Unit = { modifierBits, keyCodes, _ ->
        val modUByte = modifierBits.toUByte()
        val key1 = (keyCodes.getOrNull(0) ?: 0).toUByte()
        val key2 = (keyCodes.getOrNull(1) ?: 0).toUByte()
        val pressFrame = ClickDetector.createKeyboardFrame(setOf(modUByte), key1, key2)
        val releaseFrame = ClickDetector.createKeyboardFrame(emptySet(), 0u)
        ClickDetector.sendFrame(pressFrame)
        ClickDetector.sendFrame(releaseFrame)
    }

    // 매크로 발송 콜백: TAP/HOLD/RELEASE 스텝 순차 전송, 누적 홀드 상태 추적.
    // TAP: 홀드 상태와 합성해 press → INTRA 딜레이 → 홀드 상태로 복귀(release).
    // HOLD: 누적 상태 갱신 후 프레임 1회(release 없음). 일반 키 최대 MACRO_MAX_HELD_KEYS개.
    // RELEASE 전체(keyCodes 비어있고 mod=0): 모든 홀드 해제 → 0 프레임.
    // RELEASE 특정: 해당 키/mod만 누적에서 제거 → 갱신 상태 프레임.
    // finally: 잔여 홀드 키가 있으면 0 프레임 강제 전송(안전 해제).
    // 실행 중 스크림 오버레이 + PROGRESS 토스트로 화면 조작 차단.
    val onSendMacro: (List<MacroStep>, Int) -> Unit = { steps, stepDelayMs ->
        val estimatedMs = steps.foldIndexed(0L) { i, acc, step ->
            val count = if (step.kind == MacroStepKind.TAP) step.repeatCount.coerceAtLeast(1) else 1
            val delayMs = (step.delayAfterMs ?: stepDelayMs).toLong()
            val isLastStep = i == steps.lastIndex
            // TAP만 INTRA 딜레이 포함; HOLD/RELEASE는 단일 프레임
            val intraCost = if (step.kind == MacroStepKind.TAP) count * MACRO_INTRA_STEP_PRESS_RELEASE_MS else 0L
            acc + intraCost + if (isLastStep) (count - 1) * delayMs else count * delayMs
        }
        val displayMs = estimatedMs.coerceAtLeast(MACRO_SCRIM_MIN_DISPLAY_MS)
        MacroOverlayController.show()
        ToastController.show("매크로 실행 중", ToastType.PROGRESS, durationMs = displayMs)
        coroutineScope.launch {
            val startTime = System.currentTimeMillis()
            // 누적 홀드 상태
            var heldMod = 0
            val heldKeys = mutableListOf<Int>()
            try {
                steps.forEachIndexed { i, step ->
                    val delayMs = (step.delayAfterMs ?: stepDelayMs).toLong()
                    val isLastStep = i == steps.lastIndex
                    when (step.kind) {
                        MacroStepKind.TAP -> {
                            val count = step.repeatCount.coerceAtLeast(1)
                            // 합성 modifier: 홀드 중인 mod + 이 스텝 mod
                            val combinedMod = (heldMod or step.modifierBits).toUByte()
                            // 합성 키: 홀드 중인 키 + 이 스텝 키 (2개 제한)
                            val combinedKeys = (heldKeys + step.keyCodes).take(MACRO_MAX_HELD_KEYS)
                            val tapKey1 = (combinedKeys.getOrNull(0) ?: 0).toUByte()
                            val tapKey2 = (combinedKeys.getOrNull(1) ?: 0).toUByte()
                            // 홀드 상태 복귀 프레임 (release): 홀드만 남기고 TAP 스텝 키 해제
                            val holdMod = heldMod.toUByte()
                            val holdKey1 = (heldKeys.getOrNull(0) ?: 0).toUByte()
                            val holdKey2 = (heldKeys.getOrNull(1) ?: 0).toUByte()
                            repeat(count) { r ->
                                ClickDetector.sendFrame(ClickDetector.createKeyboardFrame(setOf(combinedMod), tapKey1, tapKey2))
                                kotlinx.coroutines.delay(MACRO_INTRA_STEP_PRESS_RELEASE_MS)
                                // 홀드가 남아있으면 홀드 상태로 복귀, 없으면 완전 0
                                if (heldMod != 0 || heldKeys.isNotEmpty()) {
                                    ClickDetector.sendFrame(ClickDetector.createKeyboardFrame(setOf(holdMod), holdKey1, holdKey2))
                                } else {
                                    ClickDetector.sendFrame(ClickDetector.createKeyboardFrame(emptySet(), 0u))
                                }
                                val isLastRepeat = r == count - 1
                                if (!isLastStep || !isLastRepeat) {
                                    kotlinx.coroutines.delay(delayMs)
                                }
                            }
                        }
                        MacroStepKind.HOLD -> {
                            // 누적 홀드 상태 갱신
                            heldMod = heldMod or step.modifierBits
                            for (code in step.keyCodes) {
                                if (code != 0 && !heldKeys.contains(code) && heldKeys.size < MACRO_MAX_HELD_KEYS) {
                                    heldKeys.add(code)
                                }
                            }
                            // 갱신된 누적 상태 프레임 전송 (release 없음)
                            val key1 = (heldKeys.getOrNull(0) ?: 0).toUByte()
                            val key2 = (heldKeys.getOrNull(1) ?: 0).toUByte()
                            ClickDetector.sendFrame(ClickDetector.createKeyboardFrame(setOf(heldMod.toUByte()), key1, key2))
                            if (!isLastStep) kotlinx.coroutines.delay(delayMs)
                        }
                        MacroStepKind.RELEASE -> {
                            val isReleaseAll = step.keyCodes.isEmpty() && step.modifierBits == 0
                            if (isReleaseAll) {
                                // 전체 릴리즈: 모든 홀드 해제
                                heldMod = 0
                                heldKeys.clear()
                                ClickDetector.sendFrame(ClickDetector.createKeyboardFrame(emptySet(), 0u))
                            } else {
                                // 특정 키 릴리즈: 해당 키/mod만 누적에서 제거
                                heldMod = heldMod and step.modifierBits.inv()
                                heldKeys.removeAll(step.keyCodes.toSet())
                                if (heldMod != 0 || heldKeys.isNotEmpty()) {
                                    val key1 = (heldKeys.getOrNull(0) ?: 0).toUByte()
                                    val key2 = (heldKeys.getOrNull(1) ?: 0).toUByte()
                                    ClickDetector.sendFrame(ClickDetector.createKeyboardFrame(setOf(heldMod.toUByte()), key1, key2))
                                } else {
                                    ClickDetector.sendFrame(ClickDetector.createKeyboardFrame(emptySet(), 0u))
                                }
                            }
                            if (!isLastStep) kotlinx.coroutines.delay(delayMs)
                        }
                    }
                }
            } finally {
                // 잔여 홀드 안전 해제: RELEASE 누락/예외 시에도 PC에 키가 눌린 채 남지 않게
                if (heldMod != 0 || heldKeys.isNotEmpty()) {
                    ClickDetector.sendFrame(ClickDetector.createKeyboardFrame(emptySet(), 0u))
                }
                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed < displayMs) kotlinx.coroutines.delay(displayMs - elapsed)
                MacroOverlayController.dismiss()
                ToastController.dismiss()
            }
        }
    }

    // 마우스 홀드 콜백: mode에 따라 강제 홀드/릴리즈 또는 토글 후 버튼 비트 전송
    val onMouseHoldToggle: (MouseButton, MouseHoldMode) -> Unit = { button, mode ->
        val newHeld = when (mode) {
            MouseHoldMode.HOLD -> heldMouseButtons + button
            MouseHoldMode.RELEASE -> heldMouseButtons - button
            MouseHoldMode.TOGGLE -> if (heldMouseButtons.contains(button)) heldMouseButtons - button else heldMouseButtons + button
        }
        heldMouseButtons = newHeld
        val buttonsByte = newHeld.fold(0) { acc, btn ->
            acc or when (btn) {
                MouseButton.LEFT   -> BridgeFrame.BUTTON_LEFT_MASK.toInt()
                MouseButton.RIGHT  -> BridgeFrame.BUTTON_RIGHT_MASK.toInt()
                MouseButton.MIDDLE -> BridgeFrame.BUTTON_MIDDLE_MASK.toInt()
            }
        }.toUByte()
        ClickDetector.sendFrame(ClickDetector.createMouseButtonFrame(buttonsByte))
        val isOn = newHeld.contains(button)
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
                        onTouchpadStateChange = recordingOnChange,
                        onRestorePrevious = onRestorePrevious,
                        onSendShortcut = onSendShortcut,
                        onSendMacro = onSendMacro,
                        onMouseHoldToggle = onMouseHoldToggle,
                        onCyclePage = onCyclePage,
                        onJumpToPage = onJumpToPage,
                        buttonVisibility = standardButtonVisibility[1] ?: TouchpadButtonVisibility.defaultFor(TouchpadIds.standardPage(1)),
                        onDpiLongPress = { dpiAdjustPopupVisible = true }
                    )
                    2 -> Page3KeyboardPlaceholder()
                    3 -> Page4MinecraftPlaceholder()
                    4 -> Page5Settings(
                        touchpadState = touchpadState,
                        onTouchpadStateChange = recordingOnChange,
                        inputMode = inputMode,
                        onInputModeChange = { inputMode = it },
                        swipeWrapEdge = swipeWrapEdge,
                        onSwipeWrapEdgeChange = { swipeWrapEdge = it },
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
    onRestorePrevious: () -> Unit = {},
    onSendShortcut: (Int, List<Int>, Boolean) -> Unit = { _, _, _ -> },
    onSendMacro: (List<MacroStep>, Int) -> Unit = { _, _ -> },
    onMouseHoldToggle: (MouseButton, MouseHoldMode) -> Unit = { _, _ -> },
    onCyclePage: (PageNav) -> Unit = {},
    onJumpToPage: (Int) -> Unit = {},
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
                        onRestorePrevious = onRestorePrevious,
                        onSendShortcut = onSendShortcut,
                        onSendMacro = onSendMacro,
                        onMouseHoldToggle = onMouseHoldToggle,
                        onCyclePage = onCyclePage,
                        onJumpToPage = onJumpToPage,
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
    onRestorePrevious: () -> Unit = {},
    onSendShortcut: (Int, List<Int>, Boolean) -> Unit = { _, _, _ -> },
    onSendMacro: (List<MacroStep>, Int) -> Unit = { _, _ -> },
    onMouseHoldToggle: (MouseButton, MouseHoldMode) -> Unit = { _, _ -> },
    onCyclePage: (PageNav) -> Unit = {},
    onJumpToPage: (Int) -> Unit = {},
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
            onRestorePrevious = onRestorePrevious,
            onSendShortcut = onSendShortcut,
            onSendMacro = onSendMacro,
            onMouseHoldToggle = onMouseHoldToggle,
            onCyclePage = onCyclePage,
            onJumpToPage = onJumpToPage,
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
    inputMode: InputMode = InputMode.NORMAL,
    onInputModeChange: (InputMode) -> Unit = {},
    swipeWrapEdge: Boolean = false,
    onSwipeWrapEdgeChange: (Boolean) -> Unit = {},
    audioFeedbackEnabled: Boolean = true,
    onAudioFeedbackEnabledChange: (Boolean) -> Unit = {},
    ttsRate: Float = 1.0f,
    onTtsRateChange: (Float) -> Unit = {},
    ttsGender: TtsGender = TtsGender.DEFAULT,
    onTtsGenderChange: (TtsGender) -> Unit = {},
    standardAssignments: Map<Int, TouchpadEdgeZoneAssignment> = emptyMap(),
    selectedZonePage: Int = 0,
    onSelectedZonePageChange: (Int) -> Unit = {},
    onOpenZoneEditor: () -> Unit = {},
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
                    text = "조작 방식",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFAAAAAA),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            item {
                SettingsInputModeSection(
                    currentMode = inputMode,
                    onModeSelected = onInputModeChange
                )
            }

            if (inputMode == InputMode.SWIPE) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF2A2A2A), RoundedCornerShape(8.dp))
                            .clickable { onSwipeWrapEdgeChange(!swipeWrapEdge) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "끝점 순환",
                                fontSize = 14.sp,
                                color = Color(0xFFEEEEEE)
                            )
                            Text(
                                text = "행의 마지막 요소에서 첫 요소로, 첫 요소에서 마지막 요소로 이동합니다",
                                fontSize = 12.sp,
                                color = Color(0xFF888888)
                            )
                        }
                        androidx.compose.material3.Switch(
                            checked = swipeWrapEdge,
                            onCheckedChange = onSwipeWrapEdgeChange
                        )
                    }
                }
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

            // 편집기 진입 항목 (ZONE 모드 선택 시에만 표시)
            if (touchpadState.edgeInteractionMode == EdgeInteractionMode.ZONE) {
                item {
                    val zoneCount = currentAssignment.config.run {
                        topZones.size + bottomZones.size + leftZones.size + rightZones.size
                    }
                    ZoneEditorEntryRow(
                        zoneCount = zoneCount,
                        onClick = onOpenZoneEditor
                    )
                }

                // 존 음성 안내 설정 (ZONE 모드에서만 유의미)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAudioFeedbackEnabledChange(!audioFeedbackEnabled) }
                            .padding(horizontal = 4.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "존 음성 안내", fontSize = 14.sp, color = Color(0xFFE0E0E0))
                            Text(
                                text = "존 진입 시 배정된 액션 이름을 읽어줌",
                                fontSize = 12.sp,
                                color = Color(0xFF888888)
                            )
                        }
                        androidx.compose.material3.Switch(
                            checked = audioFeedbackEnabled,
                            onCheckedChange = onAudioFeedbackEnabledChange
                        )
                    }
                }

                // TTS 말하기 속도 (음성 안내 켜진 경우에만 활성)
                if (audioFeedbackEnabled) {
                    item {
                        val cs = MaterialTheme.colorScheme
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = "말하기 속도", fontSize = 14.sp, color = Color(0xFFE0E0E0))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                BoxWithConstraints(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(28.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .pointerInput(onTtsRateChange) {
                                            awaitEachGesture {
                                                val down = awaitFirstDown(requireUnconsumed = false)
                                                down.consume()
                                                fun applyX(x: Float) {
                                                    val fr = (x / size.width).coerceIn(0f, 1f)
                                                    val v = 0.5f + fr * (3.0f - 0.5f)
                                                    onTtsRateChange((v * 10f).roundToInt() / 10f)
                                                }
                                                applyX(down.position.x)
                                                drag(down.id) { change ->
                                                    change.consume()
                                                    applyX(change.position.x)
                                                }
                                            }
                                        }
                                ) {
                                    val trackWidthPx = constraints.maxWidth
                                    val thumbFraction = ((ttsRate - 0.5f) / (3.0f - 0.5f)).coerceIn(0f, 1f)
                                    Box(Modifier.matchParentSize().background(cs.surfaceVariant))
                                    Box(Modifier.fillMaxHeight().fillMaxWidth(thumbFraction).background(cs.primary))
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterStart)
                                            .offset {
                                                val lineWidthPx = 3.dp.roundToPx()
                                                IntOffset(
                                                    (thumbFraction * trackWidthPx - lineWidthPx / 2f)
                                                        .roundToInt()
                                                        .coerceIn(0, (trackWidthPx - lineWidthPx).coerceAtLeast(0)),
                                                    0
                                                )
                                            }
                                            .fillMaxHeight()
                                            .width(3.dp)
                                            .background(Color.White)
                                    )
                                }
                                Text(
                                    text = "×${"%.1f".format(ttsRate)}",
                                    fontSize = 13.sp,
                                    color = cs.onSurface,
                                    modifier = Modifier.width(40.dp),
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                    }

                    // 음성 성별 선택
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "음성 성별", fontSize = 14.sp, color = Color(0xFFE0E0E0))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    TtsGender.FEMALE to "여성",
                                    TtsGender.MALE to "남성"
                                ).forEach { (g, label) ->
                                    val isSelected = ttsGender == g
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isSelected) Color(0xFF2979FF).copy(alpha = 0.2f)
                                                else Color(0xFF2A2A2A)
                                            )
                                            .border(
                                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                                color = if (isSelected) Color(0xFF2979FF) else Color(0xFF444444),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable { onTtsGenderChange(g) }
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 13.sp,
                                            color = if (isSelected) Color(0xFF2979FF) else Color(0xFFCCCCCC)
                                        )
                                    }
                                }
                            }
                        }
                    }
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
private fun SettingsInputModeSection(
    currentMode: InputMode,
    onModeSelected: (InputMode) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        InputMode.entries.forEach { mode ->
            val isSelected = currentMode == mode
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isSelected) Color(0xFF2A2A2A) else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onModeSelected(mode) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick = { onModeSelected(mode) }
                )
                Column {
                    Text(
                        text = when (mode) {
                            InputMode.NORMAL -> "일반"
                            InputMode.SWIPE -> "스와이프"
                        },
                        fontSize = 14.sp,
                        color = Color(0xFFEEEEEE)
                    )
                    Text(
                        text = when (mode) {
                            InputMode.NORMAL -> "요소를 직접 터치하고 드래그합니다"
                            InputMode.SWIPE -> "화면 어디서나 스와이프로 선택, 어디서나 터치로 조작합니다"
                        },
                        fontSize = 12.sp,
                        color = Color(0xFF888888)
                    )
                }
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
