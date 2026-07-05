package com.bridgeone.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.offset
import com.bridgeone.app.ui.common.InputMode
import com.bridgeone.app.ui.common.LocalInputMode
import kotlin.math.roundToInt
import kotlin.math.sqrt

private val KB_BG = Color(0xFF0D0D0D)
private val KB_SURFACE = Color(0xFF1A1A1A)
private val KB_ACCENT = Color(0xFF4F8EF7)
private val KB_LABEL = Color(0xFF888888)

/** NORMAL 레이어 백스페이스 롱 프레스 반복 삭제 시작 지연 (ms). 기본값: 400L */
private const val BACKSPACE_REPEAT_INITIAL_DELAY_MS = 400L

/** NORMAL 레이어 백스페이스 롱 프레스 반복 삭제 간격 (ms). 기본값: 60L */
private const val BACKSPACE_REPEAT_INTERVAL_MS = 60L

// ── 키보드 모드 / 상태 타입 ──

enum class KeyboardMode { HANGUL, ENGLISH, SYMBOL }
private enum class ShiftMode { OFF, ONESHOT, LOCKED }
private enum class ComposePhase { IDLE, JASO_ONLY, HAS_VOWEL, JAMO_PENDING }
private enum class SpecialKey { SP, BACKSPACE, SHIFT, LANG_TOGGLE, SYMBOL_TOGGLE, ERASE, CANCEL, DONE, NEXT, PREV }

private sealed class GridCell {
    data class Jamo(val char: Char) : GridCell()
    data class Special(val key: SpecialKey) : GridCell()
    data class Suggestion(val text: String) : GridCell()
    object Empty : GridCell()
}

private data class KeyRow(val cells: List<GridCell>)
private data class CellPos(val row: Int, val col: Int)

// ── 한글 조합 인덱스 ──

// 초성 인덱스
private val CHO_IDX = mapOf(
    'ㄱ' to 0, 'ㄲ' to 1, 'ㄴ' to 2, 'ㄷ' to 3, 'ㄸ' to 4,
    'ㄹ' to 5, 'ㅁ' to 6, 'ㅂ' to 7, 'ㅃ' to 8, 'ㅅ' to 9,
    'ㅆ' to 10, 'ㅇ' to 11, 'ㅈ' to 12, 'ㅉ' to 13, 'ㅊ' to 14,
    'ㅋ' to 15, 'ㅌ' to 16, 'ㅍ' to 17, 'ㅎ' to 18
)

// 종성 인덱스
private val JONG_IDX = mapOf(
    'ㄱ' to 1, 'ㄲ' to 2, 'ㄴ' to 4, 'ㄷ' to 7,
    'ㄹ' to 8, 'ㅁ' to 16, 'ㅂ' to 17, 'ㅅ' to 19,
    'ㅆ' to 20, 'ㅇ' to 21, 'ㅈ' to 22, 'ㅊ' to 23,
    'ㅋ' to 24, 'ㅌ' to 25, 'ㅍ' to 26, 'ㅎ' to 27
)

// 이중 받침: (첫 자음, 둘째 자음) → 종성 인덱스
private val DOUBLE_JONG_IDX = mapOf(
    ('ㄱ' to 'ㅅ') to 3,   // ㄳ
    ('ㄴ' to 'ㅈ') to 5,   // ㄵ
    ('ㄴ' to 'ㅎ') to 6,   // ㄶ
    ('ㄹ' to 'ㄱ') to 9,   // ㄺ
    ('ㄹ' to 'ㅁ') to 10,  // ㄻ
    ('ㄹ' to 'ㅂ') to 11,  // ㄼ
    ('ㄹ' to 'ㅅ') to 12,  // ㄽ
    ('ㄹ' to 'ㅌ') to 13,  // ㄾ
    ('ㄹ' to 'ㅍ') to 14,  // ㄿ
    ('ㄹ' to 'ㅎ') to 15,  // ㅀ
    ('ㅂ' to 'ㅅ') to 18   // ㅄ
)

// 중성 인덱스
private val JUNG_IDX = mapOf(
    'ㅏ' to 0, 'ㅐ' to 1, 'ㅑ' to 2, 'ㅒ' to 3, 'ㅓ' to 4,
    'ㅔ' to 5, 'ㅕ' to 6, 'ㅖ' to 7, 'ㅗ' to 8, 'ㅘ' to 9,
    'ㅙ' to 10, 'ㅚ' to 11, 'ㅛ' to 12, 'ㅜ' to 13, 'ㅝ' to 14,
    'ㅞ' to 15, 'ㅟ' to 16, 'ㅠ' to 17, 'ㅡ' to 18, 'ㅢ' to 19,
    'ㅣ' to 20
)

private fun syllable(cho: Int, jung: Int, jong: Int = 0): Char =
    (0xAC00 + (cho * 21 + jung) * 28 + jong).toChar()

// 복합 모음: (첫 모음, 둘째 모음) → 결합 모음
private val DOUBLE_JUNG = mapOf(
    ('ㅗ' to 'ㅏ') to 'ㅘ', ('ㅗ' to 'ㅐ') to 'ㅙ', ('ㅗ' to 'ㅣ') to 'ㅚ',
    ('ㅜ' to 'ㅓ') to 'ㅝ', ('ㅜ' to 'ㅔ') to 'ㅞ', ('ㅜ' to 'ㅣ') to 'ㅟ',
    ('ㅡ' to 'ㅣ') to 'ㅢ'
)

// 복합 모음 결합 판정을 위한 jungIdx → 단모음 char 역맵
private val IDX_TO_JUNG = JUNG_IDX.entries.associate { (k, v) -> v to k }

// ── 한글 조합 상태 머신 ──

private data class ComposerState(
    val committed: String = "",
    val phase: ComposePhase = ComposePhase.IDLE,
    val choIdx: Int = -1,
    val jungIdx: Int = -1,
    val pendingJasoChar: Char = ' ',   // JASO_ONLY: 단독 자음 / JAMO_PENDING: 임시 받침 첫 자음
    val pendingJasoChar2: Char = ' '   // JAMO_PENDING: 이중 받침 둘째 자음 (없으면 ' ')
) {
    val composingChar: String
        get() = when (phase) {
            ComposePhase.IDLE -> ""
            ComposePhase.JASO_ONLY -> pendingJasoChar.toString()
            ComposePhase.HAS_VOWEL -> syllable(choIdx, jungIdx).toString()
            ComposePhase.JAMO_PENDING -> {
                val jongI = if (pendingJasoChar2 != ' ')
                    DOUBLE_JONG_IDX[pendingJasoChar to pendingJasoChar2] ?: (JONG_IDX[pendingJasoChar] ?: 0)
                else
                    JONG_IDX[pendingJasoChar] ?: 0
                syllable(choIdx, jungIdx, jongI).toString()
            }
        }

    val display: String get() = committed + composingChar

    fun commitCurrent(): ComposerState = copy(
        committed = committed + composingChar,
        phase = ComposePhase.IDLE,
        choIdx = -1, jungIdx = -1, pendingJasoChar = ' ', pendingJasoChar2 = ' '
    )
}

// ── 두벌식 레이아웃 상수 ──

private val HANGUL_ROW0      = listOf('ㅂ','ㅈ','ㄷ','ㄱ','ㅅ','ㅛ','ㅕ','ㅑ','ㅐ','ㅔ')
private val HANGUL_SHIFT_ROW0 = listOf('ㅃ','ㅉ','ㄸ','ㄲ','ㅆ','ㅛ','ㅕ','ㅑ','ㅒ','ㅖ')
private val HANGUL_ROW1_CHARS = listOf('ㅁ','ㄴ','ㅇ','ㄹ','ㅎ','ㅗ','ㅓ','ㅏ','ㅣ')
private val HANGUL_ROW2_CHARS = listOf('ㅋ','ㅌ','ㅊ','ㅍ','ㅠ','ㅜ','ㅡ')  // ⇧ / ⌫ 제외

private val ENGLISH_ROW0_CHARS = "qwertyuiop".toList()
private val ENGLISH_ROW1_CHARS = "asdfghjkl".toList()
private val ENGLISH_ROW2_CHARS = "zxcvbnm".toList()   // ⇧ / ⌫ 제외

private val SYMBOL_ROW0_CHARS  = "1234567890".toList()
private val SYMBOL_ROW1_CHARS  = listOf('-', '/', ':', ';', '(', ')', '$', '&', '@')
private val SYMBOL_ROW2_CHARS  = listOf('.', ',', '?', '!', '\'', '"', '_')  // + Empty + ⌫

/** 모드 + shift 상태에 맞는 레이아웃 생성. Row0~Row2는 모드별 자모/기호, Row3는 동적 특수 키 행. */
private fun buildLayout(
    mode: KeyboardMode,
    shift: ShiftMode,
    hasPrev: Boolean = false,
    hasNext: Boolean = false,
    suggestions: List<String> = emptyList()
): List<KeyRow> {
    val row0: List<GridCell>
    val row1: List<GridCell>
    val row2: List<GridCell>

    when (mode) {
        KeyboardMode.HANGUL -> {
            val r0 = if (shift != ShiftMode.OFF) HANGUL_SHIFT_ROW0 else HANGUL_ROW0
            row0 = r0.map { GridCell.Jamo(it) }
            row1 = HANGUL_ROW1_CHARS.map { GridCell.Jamo(it) }
            row2 = listOf(GridCell.Special(SpecialKey.SHIFT)) +
                   HANGUL_ROW2_CHARS.map { GridCell.Jamo(it) } +
                   listOf(GridCell.Special(SpecialKey.BACKSPACE))
        }
        KeyboardMode.ENGLISH -> {
            row0 = ENGLISH_ROW0_CHARS.map {
                GridCell.Jamo(if (shift != ShiftMode.OFF) it.uppercaseChar() else it)
            }
            row1 = ENGLISH_ROW1_CHARS.map {
                GridCell.Jamo(if (shift != ShiftMode.OFF) it.uppercaseChar() else it)
            }
            row2 = listOf(GridCell.Special(SpecialKey.SHIFT)) +
                   ENGLISH_ROW2_CHARS.map {
                       GridCell.Jamo(if (shift != ShiftMode.OFF) it.uppercaseChar() else it)
                   } +
                   listOf(GridCell.Special(SpecialKey.BACKSPACE))
        }
        KeyboardMode.SYMBOL -> {
            row0 = SYMBOL_ROW0_CHARS.map { GridCell.Jamo(it) }
            row1 = SYMBOL_ROW1_CHARS.map { GridCell.Jamo(it) }
            row2 = SYMBOL_ROW2_CHARS.map { GridCell.Jamo(it) } +
                   listOf(GridCell.Special(SpecialKey.BACKSPACE))
        }
    }

    val row3cells = mutableListOf(
        GridCell.Special(SpecialKey.LANG_TOGGLE),
        GridCell.Special(SpecialKey.SYMBOL_TOGGLE),
        GridCell.Special(SpecialKey.SP)
    )
    if (hasPrev) row3cells.add(GridCell.Special(SpecialKey.PREV))
    row3cells.add(GridCell.Special(SpecialKey.CANCEL))
    row3cells.add(GridCell.Special(if (hasNext) SpecialKey.NEXT else SpecialKey.DONE))

    val rows = mutableListOf(KeyRow(row0), KeyRow(row1), KeyRow(row2), KeyRow(row3cells))
    if (suggestions.isNotEmpty()) {
        rows.add(0, KeyRow(suggestions.take(4).map { GridCell.Suggestion(it) }))
    }
    return rows
}

/** 특수 키 라벨. MODE_TOGGLE은 현재 모드를 표시 (탭하면 다음 모드로 전환). */
private fun specialLabel(key: SpecialKey, mode: KeyboardMode): String = when (key) {
    SpecialKey.SP -> "⎵"
    SpecialKey.BACKSPACE -> "⌫"
    SpecialKey.SHIFT -> "⇧"
    SpecialKey.LANG_TOGGLE -> when (mode) {
        KeyboardMode.HANGUL -> "한글"
        KeyboardMode.ENGLISH -> "영문"
        KeyboardMode.SYMBOL -> "한/영"
    }
    SpecialKey.SYMBOL_TOGGLE -> if (mode == KeyboardMode.SYMBOL) "한글" else "?123"
    SpecialKey.ERASE -> "초기화"
    SpecialKey.CANCEL -> "취소"
    SpecialKey.DONE -> "완료"
    SpecialKey.NEXT -> "다음"
    SpecialKey.PREV -> "이전"
}

/**
 * 빈 셀/범위 초과 CellPos를 가장 가까운 유효 셀로 보정.
 * 1) 같은 행 좌·우 교대 탐색
 * 2) 인접 행의 동일 분수 가로 위치 탐색
 */
private fun resolveToValidCell(target: CellPos, layout: List<KeyRow>): CellPos {
    val row = target.row.coerceIn(0, layout.size - 1)
    val rowCells = layout[row].cells
    val col = target.col.coerceIn(0, rowCells.size - 1)
    if (rowCells[col] !is GridCell.Empty) return CellPos(row, col)

    // 같은 행 좌·우 교대 탐색
    for (dc in 1..rowCells.size) {
        val r = (col + dc) % rowCells.size
        val l = ((col - dc) % rowCells.size + rowCells.size) % rowCells.size
        if (rowCells[r] !is GridCell.Empty) return CellPos(row, r)
        if (rowCells[l] !is GridCell.Empty) return CellPos(row, l)
    }

    // 인접 행 동일 분수 위치 탐색
    val frac = (col + 0.5f) / rowCells.size
    for (dr in 1..layout.size) {
        for (candidate in listOf(
            (row + dr) % layout.size,
            ((row - dr) % layout.size + layout.size) % layout.size
        )) {
            val cCells = layout[candidate].cells
            val targetCol = ((frac * cCells.size) - 0.5f).roundToInt()
                .coerceIn(0, cCells.size - 1)
            if (cCells[targetCol] !is GridCell.Empty) return CellPos(candidate, targetCol)
        }
    }

    return CellPos(0, 0)
}

// ─────────────────────────────────────────────────────────────

/**
 * 스와이프 키보드 오버레이 (두벌식 쿼티 레이아웃)
 *
 * - Row0/Row1: 자모/문자 (shift에 따라 전환)
 * - Row2: ⇧ + 자모 7개 + ⌫ (9 cells)
 * - Row3: 한/A/123 모드 전환 + SP + 초기화 + 완료 (4 cells)
 * - 손가락 위치 추종 방식 스와이프: dx/dy → 행 우선 계산 + 분수 X 보존
 *
 * @param initialText  초기 텍스트
 * @param maxLength    최대 글자 수
 * @param onDone       완료 콜백 (확정된 텍스트 전달)
 */
@Composable
fun SwipeKeyboardOverlay(
    initialText: String,
    maxLength: Int,
    onTextChange: ((String) -> Unit)? = null,
    onCancel: () -> Unit = {},
    onDone: (String) -> Unit,
    showScrim: Boolean = true,
    gestureFullHeight: Boolean = false,
    showGuide: Boolean = true,
    initialMode: KeyboardMode = KeyboardMode.HANGUL,
    suggestions: List<String> = emptyList(),
    onNext: ((String) -> Unit)? = null,
    onPrev: ((String) -> Unit)? = null,
    revertOnCancel: Boolean = true,
    onContentHeightMeasured: ((Int) -> Unit)? = null,
    showKeyboard: Boolean = true,
    /** 키보드를 화면 하단으로부터 위로 올릴 거리 (dp). 0이면 화면 하단에 붙음. */
    keyboardBottomPadding: Dp = 0.dp,
    overlay: (@Composable BoxScope.() -> Unit)? = null,
    /**
     * 히트 영역만 부모 크기 전체로 확장 (정렬/모양은 기존 TopCenter·bottom-rounded 로직 그대로 유지).
     * gestureFullHeight와 달리 contentAlignment를 바꾸지 않음 — 인라인(TopCenter) 키보드의
     * 스와이프 히트 영역만 넓히고 싶을 때 사용. 기본값: false
     */
    expandHitAreaOnly: Boolean = false,
    /**
     * TopCenter 정렬(showScrim=false && !gestureFullHeight)일 때 키보드 콘텐츠를 아래로
     * 밀어낼 거리(dp). expandHitAreaOnly로 히트 영역만 넓히고 시각적 위치는 특정 y좌표에
     * 그대로 유지하고 싶을 때 사용. 기본값: 0dp (기존 동작과 동일)
     */
    contentTopOffsetDp: Dp = 0.dp,
) {
    val inputMode = LocalInputMode.current
    val original = remember { initialText }
    var composer by remember { mutableStateOf(ComposerState(committed = initialText)) }
    var mode by remember { mutableStateOf(initialMode) }
    var shift by remember { mutableStateOf(ShiftMode.OFF) }
    val firstInputRow = if (suggestions.isEmpty()) 0 else 1
    var selectedCell by remember { mutableStateOf(CellPos(firstInputRow, 0)) }

    val hasPrev = onPrev != null
    val hasNext = onNext != null
    val layout = remember(mode, shift) { buildLayout(mode, shift, hasPrev, hasNext, suggestions) }

    val isKeyboardActive by rememberUpdatedState(showKeyboard)
    LaunchedEffect(showKeyboard) {
        if (showKeyboard) {
            composer = ComposerState(committed = initialText)
            mode = initialMode
            selectedCell = CellPos(if (suggestions.isEmpty()) 0 else 1, 0)
            shift = ShiftMode.OFF
        }
    }

    val currentOnTextChange by rememberUpdatedState(onTextChange)
    LaunchedEffect(composer) {
        currentOnTextChange?.invoke(composer.display)
    }

    fun inputJamo(c: Char) {
        val jungI = JUNG_IDX[c]
        val choI = CHO_IDX[c]

        if (jungI != null) {
            // 모음 입력
            composer = when (composer.phase) {
                ComposePhase.IDLE -> {
                    if (composer.committed.length < maxLength)
                        composer.copy(committed = composer.committed + c)
                    else composer
                }
                ComposePhase.JASO_ONLY -> {
                    composer.copy(phase = ComposePhase.HAS_VOWEL, jungIdx = jungI)
                }
                ComposePhase.HAS_VOWEL -> {
                    val curVowel = IDX_TO_JUNG[composer.jungIdx]
                    val merged = DOUBLE_JUNG[curVowel to c]
                    if (merged != null) {
                        composer.copy(jungIdx = JUNG_IDX[merged]!!)
                    } else {
                        val base = composer.commitCurrent()
                        if (base.committed.length < maxLength)
                            base.copy(committed = base.committed + c)
                        else base
                    }
                }
                ComposePhase.JAMO_PENDING -> {
                    if (composer.pendingJasoChar2 != ' ') {
                        // 이중 받침 분리: 첫 받침 → 종성, 둘째 받침 → 다음 초성
                        val firstJongI = JONG_IDX[composer.pendingJasoChar] ?: 0
                        val prevCommitted = if (composer.committed.length < maxLength)
                            composer.committed + syllable(composer.choIdx, composer.jungIdx, firstJongI)
                        else return
                        val newChoI = CHO_IDX[composer.pendingJasoChar2] ?: 11
                        if (prevCommitted.length < maxLength)
                            ComposerState(
                                committed = prevCommitted,
                                phase = ComposePhase.HAS_VOWEL,
                                choIdx = newChoI,
                                jungIdx = jungI
                            )
                        else ComposerState(committed = prevCommitted)
                    } else {
                        // 단일 받침 분리: 받침 → 다음 초성
                        val prevCommitted = if (composer.committed.length < maxLength)
                            composer.committed + syllable(composer.choIdx, composer.jungIdx)
                        else return
                        val newChoI = CHO_IDX[composer.pendingJasoChar] ?: 11
                        if (prevCommitted.length < maxLength)
                            ComposerState(
                                committed = prevCommitted,
                                phase = ComposePhase.HAS_VOWEL,
                                choIdx = newChoI,
                                jungIdx = jungI
                            )
                        else ComposerState(committed = prevCommitted)
                    }
                }
            }
        } else if (choI != null) {
            // 자음 입력
            composer = when (composer.phase) {
                ComposePhase.IDLE -> {
                    composer.copy(phase = ComposePhase.JASO_ONLY, choIdx = choI, pendingJasoChar = c)
                }
                ComposePhase.JASO_ONLY -> {
                    val base = composer.commitCurrent()
                    if (base.committed.length < maxLength)
                        base.copy(phase = ComposePhase.JASO_ONLY, choIdx = choI, pendingJasoChar = c)
                    else base
                }
                ComposePhase.HAS_VOWEL -> {
                    if (JONG_IDX.containsKey(c)) {
                        composer.copy(phase = ComposePhase.JAMO_PENDING, pendingJasoChar = c)
                    } else {
                        val base = composer.commitCurrent()
                        if (base.committed.length < maxLength)
                            base.copy(phase = ComposePhase.JASO_ONLY, choIdx = choI, pendingJasoChar = c)
                        else base
                    }
                }
                ComposePhase.JAMO_PENDING -> {
                    // 이중 받침 시도
                    val doubleJongI = if (composer.pendingJasoChar2 == ' ')
                        DOUBLE_JONG_IDX[composer.pendingJasoChar to c]
                    else null
                    if (doubleJongI != null) {
                        composer.copy(pendingJasoChar2 = c)
                    } else {
                        val base = composer.commitCurrent()
                        if (base.committed.length < maxLength)
                            base.copy(phase = ComposePhase.JASO_ONLY, choIdx = choI, pendingJasoChar = c)
                        else base
                    }
                }
            }
        }
    }

    fun inputAlpha(c: Char) {
        if (composer.phase != ComposePhase.IDLE) {
            composer = composer.commitCurrent()
        }
        if (composer.committed.length < maxLength) {
            composer = composer.copy(committed = composer.committed + c)
        }
    }

    fun handleSpecial(key: SpecialKey) {
        when (key) {
            SpecialKey.SP -> {
                val base = composer.commitCurrent()
                composer = if (base.committed.length < maxLength)
                    base.copy(committed = base.committed + ' ')
                else base
            }
            SpecialKey.BACKSPACE -> {
                val base = composer.commitCurrent()
                val next = if (base.committed.isNotEmpty())
                    base.copy(committed = base.committed.dropLast(1))
                else base
                composer = next
                if (next.committed.isEmpty()) {
                    mode = initialMode
                }
            }
            SpecialKey.SHIFT -> {
                shift = when (shift) {
                    ShiftMode.OFF -> ShiftMode.ONESHOT
                    ShiftMode.ONESHOT -> ShiftMode.LOCKED   // 두 번 연속 탭 → 락
                    ShiftMode.LOCKED -> ShiftMode.OFF
                }
            }
            SpecialKey.LANG_TOGGLE -> {
                mode = when (mode) {
                    KeyboardMode.HANGUL -> KeyboardMode.ENGLISH
                    else -> KeyboardMode.HANGUL
                }
                shift = ShiftMode.OFF
            }
            SpecialKey.SYMBOL_TOGGLE -> {
                mode = if (mode == KeyboardMode.SYMBOL) KeyboardMode.HANGUL else KeyboardMode.SYMBOL
                shift = ShiftMode.OFF
            }
            SpecialKey.ERASE -> {
                composer = ComposerState()
                mode = initialMode
                selectedCell = CellPos(firstInputRow, 0)
                shift = ShiftMode.OFF
            }
            SpecialKey.CANCEL -> {
                if (revertOnCancel) onTextChange?.invoke(original)
                onCancel()
            }
            SpecialKey.DONE -> {
                onDone(composer.commitCurrent().committed)
            }
            SpecialKey.NEXT -> {
                val committed = composer.commitCurrent()
                composer = committed
                onTextChange?.invoke(committed.committed)
                onNext?.invoke(committed.committed)
            }
            SpecialKey.PREV -> {
                val committed = composer.commitCurrent()
                composer = committed
                onTextChange?.invoke(committed.committed)
                onPrev?.invoke(committed.committed)
            }
        }
    }

    fun activateCell(cell: GridCell) {
        when (cell) {
            is GridCell.Empty -> {}
            is GridCell.Suggestion -> {
                val base = composer.commitCurrent()
                composer = ComposerState(committed = (base.committed + cell.text).take(maxLength))
            }
            is GridCell.Jamo -> {
                // cell.char에 이미 shift 적용됨 (buildLayout에서 결정)
                if (mode == KeyboardMode.HANGUL) inputJamo(cell.char)
                else inputAlpha(cell.char)
                if (shift == ShiftMode.ONESHOT) shift = ShiftMode.OFF  // 원샷만 1회 입력 후 해제, 락은 유지
            }
            is GridCell.Special -> handleSpecial(cell.key)
        }
    }

    // 그리드 실제 픽셀 크기 (pointerInput 스와이프 계산용)
    var gridWidthPx by remember { mutableIntStateOf(0) }
    var gridHeightPx by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .then(
                if (showScrim || gestureFullHeight || overlay != null || expandHitAreaOnly) Modifier.fillMaxSize()
                else Modifier.fillMaxWidth().wrapContentHeight()
            )
            .background(if (showScrim) Color.Black.copy(alpha = 0.5f) else Color.Transparent)
            .then(
                if (inputMode == InputMode.NORMAL) Modifier
                else Modifier.pointerInput(layout) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (!isKeyboardActive) return@awaitEachGesture
                    val totalRows = layout.size
                    val rowH = if (gridHeightPx > 0) gridHeightPx.toFloat() / totalRows
                               else size.height.toFloat() / totalRows
                    val totalW = if (gridWidthPx > 0) gridWidthPx.toFloat() else size.width.toFloat()
                    val tapThreshPx = 10.dp.toPx()

                    val startPos = down.position
                    val startCell = selectedCell
                    var moved = false

                    var ev = awaitPointerEvent()
                    while (ev.type != PointerEventType.Release) {
                        if (ev.type == PointerEventType.Move) {
                            ev.changes.forEach { it.consume() }
                            val pos = ev.changes.first().position
                            val dx = pos.x - startPos.x
                            val dy = pos.y - startPos.y
                            if (sqrt(dx * dx + dy * dy) > tapThreshPx) moved = true

                            // 행 결정
                            val rowDelta = (dy / rowH).roundToInt()
                            val newRow = (startCell.row + rowDelta).coerceIn(0, totalRows - 1)

                            // 열 결정: 분수 가로 위치 보존 (행마다 셀 폭이 다름)
                            val startRowCols = layout[startCell.row].cells.size
                            val newRowCols = layout[newRow].cells.size
                            val startFracX = (startCell.col + 0.5f) / startRowCols
                            val newFracX = startFracX + dx / totalW
                            val newColRaw = (newFracX * newRowCols).toInt()
                                .coerceIn(0, newRowCols - 1)

                            selectedCell = resolveToValidCell(CellPos(newRow, newColRaw), layout)
                        }
                        ev = awaitPointerEvent()
                    }

                    if (!moved) activateCell(layout[selectedCell.row].cells[selectedCell.col])
                }
            }
            ),
        contentAlignment = if (showScrim || gestureFullHeight) Alignment.BottomCenter else Alignment.TopCenter
    ) {
        AnimatedVisibility(
            visible = showKeyboard,
            enter = slideInVertically(tween(220)) { it } + fadeIn(tween(220)),
            exit = slideOutVertically(tween(180)) { it } + fadeOut(tween(180)),
            modifier = Modifier.offset(y = contentTopOffsetDp - keyboardBottomPadding),
        ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    KB_BG,
                    if (showScrim || gestureFullHeight) RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    else RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                )
                .padding(top = 12.dp, bottom = 8.dp)
                .then(
                    if (onContentHeightMeasured != null)
                        Modifier.onGloballyPositioned { onContentHeightMeasured(it.size.height) }
                    else Modifier
                ),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {

            // ── 키보드 그리드 ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { gridWidthPx = it.width; gridHeightPx = it.height }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(horizontal = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // ── 추천어 스트립 컨테이너 ──
                    if (suggestions.isNotEmpty()) {
                        val sugRow = layout[0]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(KB_ACCENT.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            sugRow.cells.forEachIndexed { colIdx, cell ->
                                if (cell is GridCell.Suggestion) {
                                    val interactionSource = remember { MutableInteractionSource() }
                                    val isPressed by interactionSource.collectIsPressedAsState()
                                    val isSelected = if (inputMode == InputMode.NORMAL) isPressed
                                                      else selectedCell == CellPos(0, colIdx)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(
                                                if (isSelected) KB_ACCENT.copy(alpha = 0.22f)
                                                else KB_ACCENT.copy(alpha = 0.10f),
                                                RoundedCornerShape(6.dp)
                                            )
                                            .then(
                                                if (isSelected) Modifier.border(
                                                    1.dp, KB_ACCENT, RoundedCornerShape(6.dp)
                                                ) else Modifier
                                            )
                                            .then(
                                                if (inputMode == InputMode.NORMAL)
                                                    Modifier.clickable(
                                                        interactionSource = interactionSource,
                                                        indication = null
                                                    ) { activateCell(cell) }
                                                else Modifier
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = cell.text,
                                            color = if (isSelected) KB_ACCENT
                                                    else KB_ACCENT.copy(alpha = 0.8f),
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold
                                                         else FontWeight.Normal,
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            textAlign = TextAlign.Center,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ── 키보드 키 컨테이너 ──
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(KB_SURFACE, RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (rowIdx in firstInputRow until layout.size) {
                            val row = layout[rowIdx]
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                row.cells.forEachIndexed { colIdx, cell ->
                                    val interactionSource = remember { MutableInteractionSource() }
                                    val isPressed by interactionSource.collectIsPressedAsState()
                                    val isSelected = if (inputMode == InputMode.NORMAL) isPressed
                                                      else selectedCell == CellPos(rowIdx, colIdx)
                                    when (cell) {
                                        is GridCell.Empty -> Spacer(Modifier.weight(1f))
                                        is GridCell.Suggestion -> {}
                                        is GridCell.Jamo -> Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .background(
                                                    if (isSelected) KB_ACCENT.copy(alpha = 0.22f)
                                                    else KB_BG,
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .then(
                                                    if (isSelected) Modifier.border(
                                                        1.dp, KB_ACCENT, RoundedCornerShape(6.dp)
                                                    ) else Modifier
                                                )
                                                .then(
                                                    if (inputMode == InputMode.NORMAL)
                                                        Modifier.clickable(
                                                            interactionSource = interactionSource,
                                                            indication = null
                                                        ) { activateCell(cell) }
                                                    else Modifier
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = cell.char.toString(),
                                                color = if (isSelected) KB_ACCENT else Color.White,
                                                fontSize = 16.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                modifier = Modifier.padding(vertical = 8.dp)
                                            )
                                        }
                                        is GridCell.Special -> {
                                            val isShiftActive = cell.key == SpecialKey.SHIFT &&
                                                shift != ShiftMode.OFF
                                            val isShiftLocked = cell.key == SpecialKey.SHIFT &&
                                                shift == ShiftMode.LOCKED
                                            val bgColor = when {
                                                isShiftLocked && !isSelected -> KB_ACCENT.copy(alpha = 0.55f)
                                                isShiftActive && !isSelected -> KB_ACCENT.copy(alpha = 0.35f)
                                                isSelected -> KB_ACCENT.copy(alpha = 0.22f)
                                                else -> KB_BG
                                            }
                                            val borderMod = if (isSelected || isShiftActive)
                                                Modifier.border(1.dp, KB_ACCENT, RoundedCornerShape(6.dp))
                                            else Modifier
                                            val labelColor = if (isSelected || isShiftActive) KB_ACCENT
                                                             else KB_LABEL
                                            val keyWeight = when (cell.key) {
                                                SpecialKey.SHIFT, SpecialKey.BACKSPACE -> 1.6f
                                                SpecialKey.SP -> 2f
                                                else -> 1f
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .weight(keyWeight)
                                                    .background(bgColor, RoundedCornerShape(6.dp))
                                                    .then(borderMod)
                                                    .then(
                                                        if (inputMode != InputMode.NORMAL) Modifier
                                                        else if (cell.key == SpecialKey.BACKSPACE) {
                                                            Modifier.pointerInput(cell) {
                                                                coroutineScope {
                                                                    awaitEachGesture {
                                                                        val down = awaitFirstDown(requireUnconsumed = false)
                                                                        val press = PressInteraction.Press(down.position)
                                                                        launch { interactionSource.emit(press) }
                                                                        handleSpecial(SpecialKey.BACKSPACE)   // 탭 즉시 1회 삭제
                                                                        val repeatJob = launch {
                                                                            delay(BACKSPACE_REPEAT_INITIAL_DELAY_MS)
                                                                            while (isKeyboardActive) {
                                                                                handleSpecial(SpecialKey.BACKSPACE)
                                                                                delay(BACKSPACE_REPEAT_INTERVAL_MS)
                                                                            }
                                                                        }
                                                                        waitForUpOrCancellation()
                                                                        repeatJob.cancel()
                                                                        launch { interactionSource.emit(PressInteraction.Release(press)) }
                                                                    }
                                                                }
                                                            }
                                                        } else {
                                                            Modifier.clickable(
                                                                interactionSource = interactionSource,
                                                                indication = null
                                                            ) { activateCell(cell) }
                                                        }
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = specialLabel(cell.key, mode),
                                                    color = labelColor,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isSelected || isShiftActive)
                                                        FontWeight.Bold else FontWeight.Normal,
                                                    modifier = Modifier.padding(vertical = 8.dp),
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── 조작 안내 (2×2 그리드) — showGuide=false 또는 NORMAL 모드 시 숨김 ──
            if (showGuide && inputMode == InputMode.SWIPE) {
                val hints = listOf(
                    Triple("↔", "드래그", "손가락을 밀어 키 선택"),
                    Triple("⊙", "손 떼기", "선택된 키 입력"),
                    Triple("⇧", "Shift", "쌍자음·대문자 (1회, 두 번=고정)"),
                    Triple("⇄", "모드", "한·A·123 순으로 전환")
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp)
                        .padding(top = 6.dp)
                        .background(KB_SURFACE, RoundedCornerShape(8.dp))
                ) {
                    hints.forEachIndexed { idx, (icon, action, desc) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = icon,
                                color = KB_ACCENT,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.width(24.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = action,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "→",
                                color = KB_LABEL,
                                fontSize = 12.sp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = desc,
                                color = KB_LABEL,
                                fontSize = 12.sp
                            )
                        }
                        if (idx < hints.lastIndex) {
                            Spacer(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp)
                                    .height(1.dp)
                                    .background(Color.White.copy(alpha = 0.08f))
                            )
                        }
                    }
                }
            }
        }
        } // AnimatedVisibility
        overlay?.invoke(this)
    }
}
