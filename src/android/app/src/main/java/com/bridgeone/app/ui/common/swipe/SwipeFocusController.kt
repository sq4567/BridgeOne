package com.bridgeone.app.ui.common.swipe

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * 이동 불가 경계 히트 시 시각적 플래시 피드백 신호.
 * key가 매번 증가하므로 같은 요소가 연속으로 플래시돼도 LaunchedEffect가 재실행됨.
 */
data class FlashSignal(val element: FocusableElement, val key: Int)

/**
 * SWIPE 모드 전용 포커스 컨트롤러.
 *
 * 화면 어디서나 4방향 스와이프로 인접 요소 포커스를 이동하고,
 * 화면 어디든 탭으로 포커스된 요소를 활성화하는 인터랙션 모델의 상태 홀더.
 *
 * 두 단계 모드:
 * - [SwipeMode.SELECTION]: 스와이프 = 포커스 이동, 탭 = 활성화 (또는 조작 모드 진입)
 * - [SwipeMode.MANIPULATION]: 스와이프 = 선택된 요소 직접 조작, 탭 = 종료
 *
 * 가상 그리드 traversal (기본):
 * 각 [FocusableEntry]가 [FocusableEntry.gridRow]를 명시하면 그 값으로 행을 식별하고,
 * 같은 행 안에서는 bounds.center.x 오름차순으로 column 인덱스를 자동 부여한다.
 * 화면상 픽셀 거리와 무관하게 LEFT/RIGHT는 인접 column으로, UP/DOWN은 인접 row의
 * 가장 가까운 column으로 이동한다. 가장자리는 clamp (wrap 없음).
 *
 * 좌표 기반 fallback:
 * 활성 scope의 entry 중 하나라도 gridRow가 null이면, 해당 scope는 좌표 기반
 * nearest-neighbor cone traversal로 동작한다 (점진적 마이그레이션을 위한 호환 경로).
 */
class SwipeFocusController {

    var currentFocus: FocusableElement? by mutableStateOf(null)
        private set

    var mode: SwipeMode by mutableStateOf(SwipeMode.SELECTION)
        private set

    /** 첫 요소 등록이 완료되어 입력을 받을 준비가 됐는지. 기본값: false */
    var isReady: Boolean by mutableStateOf(false)
        private set

    /** 가상 그리드 행/열 끝점 wrap 여부. true면 마지막 요소→첫 요소, 첫 요소→마지막 요소로 순환. 기본값: false */
    var wrapEdge: Boolean = false

    /** scope 스택. 최상위(`last()`)가 현재 활성 scope. ROOT_SCOPE는 항상 바닥에 깔려 있음. */
    private val scopeStack = mutableStateListOf<Any>(ROOT_SCOPE)
    val activeScope: Any get() = scopeStack.last()

    /** 등록된 요소 맵. 키는 [FocusableElement] 인스턴스. */
    private val entries = mutableStateMapOf<FocusableElement, FocusableEntry>()

    /** 경계 히트 시 발행되는 플래시 신호. [SwipeFocusable]이 구독하여 붉은 점멸 애니메이션 실행. */
    var flashSignal: FlashSignal? by mutableStateOf(null)
        private set
    private var flashCounter = 0

    /** 현재 포커스 요소에 플래시 신호 발행. 연속 호출 시 key 증가로 애니메이션 재시작 보장. */
    fun triggerFlash(element: FocusableElement?) {
        if (element != null) flashSignal = FlashSignal(element, ++flashCounter)
    }

    /** [SwipeFocusable] composable이 자신을 등록/갱신할 때 호출. bounds 갱신 시에도 동일 호출. */
    fun register(entry: FocusableEntry) {
        entries[entry.element] = entry
        if (!isReady) isReady = true
    }

    /**
     * 초기/명시적 포커스 지정. 아직 등록되지 않은 element도 받아들이며,
     * 이후 [register]가 호출되면 자연스럽게 isFocused 상태로 합쳐진다.
     */
    fun setFocus(element: FocusableElement?) {
        currentFocus = element
    }

    /** 등록된 요소의 화면 좌표(bounds)를 반환. 미등록이면 null. */
    fun boundsOf(element: FocusableElement?): androidx.compose.ui.geometry.Rect? =
        if (element != null) entries[element]?.bounds else null

    /** [SwipeFocusable]이 dispose될 때 호출. */
    fun unregister(element: FocusableElement) {
        entries.remove(element)
        if (currentFocus == element) currentFocus = null
        if (entries.isEmpty()) isReady = false
    }

    /**
     * 활성 scope 안의 요소 중 [direction] 방향 인접 요소로 포커스 이동.
     * 모든 entry가 gridRow를 가지면 그리드 기반, 아니면 좌표 기반 fallback.
     * @return 포커스가 실제로 이동했으면 true, 경계 히트(이동 불가)면 false.
     */
    fun moveFocus(direction: Direction): Boolean {
        moveInterceptor?.let { if (it(direction)) return true }
        val before = currentFocus
        val candidates = entries.values.filter { it.scope == activeScope }
        if (candidates.isEmpty()) return false

        val allGridded = candidates.all { it.gridRow != null }
        if (allGridded) {
            moveFocusGrid(direction, candidates)
        } else {
            moveFocusCoordinate(direction, candidates)
        }
        return currentFocus != before
    }

    /** 가상 그리드 traversal: gridRow + 같은 행 내 bounds.center.x 오름차순 column. */
    private fun moveFocusGrid(direction: Direction, candidates: List<FocusableEntry>) {
        // row → 같은 row 내 x 오름차순 column 리스트
        // bounds 크기가 0인 항목은 scroll 컨테이너 밖으로 clipped된 것으로 간주해 제외
        val rows: Map<Int, List<FocusableEntry>> = candidates
            .groupBy { it.gridRow!! }
            .mapValues { (_, list) ->
                // gridCol이 있으면 논리적 순서로, 없으면 화면 x 좌표로 정렬
                if (list.all { it.gridCol != null }) list.sortedBy { it.gridCol }
                else list.filter { it.bounds.width > 0f && it.bounds.height > 0f }
                         .sortedBy { it.bounds.center.x }
            }
        val sortedRowIndices = rows.keys.sorted()

        val current = currentFocus
        if (current == null) {
            // 포커스 없으면 첫 행의 첫 column
            val firstRow = sortedRowIndices.firstOrNull() ?: return
            currentFocus = rows[firstRow]?.firstOrNull()?.element
            return
        }

        val currentEntry = entries[current]
        if (currentEntry == null || currentEntry.gridRow == null) {
            currentFocus = rows[sortedRowIndices.first()]?.firstOrNull()?.element
            return
        }

        val curRow = currentEntry.gridRow
        val curRowList = rows[curRow] ?: return
        val curColIndex = curRowList.indexOfFirst { it.element == current }
        if (curColIndex < 0) return

        when (direction) {
            Direction.LEFT -> {
                val nextCol = curColIndex - 1
                if (nextCol >= 0) currentFocus = curRowList[nextCol].element
                else if (wrapEdge) currentFocus = curRowList.last().element
            }
            Direction.RIGHT -> {
                val nextCol = curColIndex + 1
                if (nextCol < curRowList.size) currentFocus = curRowList[nextCol].element
                else if (wrapEdge) currentFocus = curRowList.first().element
            }
            Direction.UP, Direction.DOWN -> {
                val curRowPos = sortedRowIndices.indexOf(curRow)
                val nextRowPos = if (direction == Direction.UP) curRowPos - 1 else curRowPos + 1
                if (nextRowPos < 0 || nextRowPos >= sortedRowIndices.size) return
                val nextRowList = rows[sortedRowIndices[nextRowPos]] ?: return
                val curX = currentEntry.bounds.center.x
                val nearest = nextRowList.minByOrNull { abs(it.bounds.center.x - curX) }
                if (nearest != null) currentFocus = nearest.element
            }
        }
    }

    /** 좌표 기반 cone traversal (gridRow 미지정 fallback). */
    private fun moveFocusCoordinate(direction: Direction, candidates: List<FocusableEntry>) {
        val current = currentFocus
        if (current == null) {
            currentFocus = candidates.minByOrNull {
                it.bounds.top * SCREEN_WIDTH_HINT_PX + it.bounds.left
            }?.element
            return
        }
        val currentEntry = entries[current]
        if (currentEntry == null) {
            currentFocus = candidates.firstOrNull()?.element
            return
        }
        val cx = currentEntry.bounds.center
        val filtered = candidates.filter { it.element != current }.filter { entry ->
            val c = entry.bounds.center
            val dx = c.x - cx.x
            val dy = c.y - cx.y
            when (direction) {
                Direction.LEFT -> dx < 0f && abs(dx) >= abs(dy)
                Direction.RIGHT -> dx > 0f && abs(dx) >= abs(dy)
                Direction.UP -> dy < 0f && abs(dy) >= abs(dx)
                Direction.DOWN -> dy > 0f && abs(dy) >= abs(dx)
            }
        }
        val next = filtered.minByOrNull { entry ->
            val c = entry.bounds.center
            val dx = c.x - cx.x
            val dy = c.y - cx.y
            sqrt(dx * dx + dy * dy)
        }
        if (next != null) currentFocus = next.element
    }

    /**
     * 현재 포커스된 요소를 활성화.
     * - manipulatable = true이면 [SwipeMode.MANIPULATION] 진입
     * - 그렇지 않으면 onActivate 호출 (즉시 실행)
     */
    fun activate() {
        val entry = entries[currentFocus] ?: return
        if (entry.manipulatable) {
            mode = SwipeMode.MANIPULATION
        } else {
            entry.onActivate()
        }
    }

    /**
     * 포커스 이동 인터셉터. 설치 시 moveFocus 진입부에서 먼저 호출되며,
     * true 반환 시 일반 traversal을 건너뛴다(외부가 이동을 처리). null이면 기존 동작과 동일. 기본값: null
     */
    var moveInterceptor: ((Direction) -> Boolean)? = null

    /** 더블탭 = 길게 누르기 등가. 현재 포커스 요소의 onActivateAlt 호출. */
    fun activateAlt() {
        val entry = entries[currentFocus] ?: return
        entry.onActivateAlt()
    }

    /** [SwipeMode.MANIPULATION] → [SwipeMode.SELECTION] 복귀. */
    fun exitManipulation() {
        mode = SwipeMode.SELECTION
    }

    /** 조작 모드에서 스와이프 거리를 현재 포커스 요소에 전달. */
    fun manipulate(deltaPx: Float, screenWidthPx: Float) {
        if (mode != SwipeMode.MANIPULATION) return
        val entry = entries[currentFocus] ?: return
        entry.onManipulate(deltaPx, screenWidthPx)
    }

    /** 메뉴/팝업 진입 시 호출. 새 scope로 전환되며 포커스는 초기화. */
    fun pushScope(scope: Any) {
        scopeStack.add(scope)
        currentFocus = null
        mode = SwipeMode.SELECTION
    }

    /** 메뉴/팝업 종료 시 호출. 이전 scope로 복귀하며 포커스는 초기화. */
    fun popScope() {
        if (scopeStack.size > 1) {
            scopeStack.removeAt(scopeStack.size - 1)
            currentFocus = null
            mode = SwipeMode.SELECTION
        }
    }

    /** scope 스택 깊이. ROOT_SCOPE 포함이므로 1 이상. */
    val scopeDepth: Int get() = scopeStack.size

    /**
     * SELECTION 모드에서 한 번의 포인터 이벤트에서 처리할 수 있는 최대 이동 스텝 수.
     * 기본값: Int.MAX_VALUE (무제한). 서랍·팝업처럼 빠른 스와이프 시 큰 점프를 막으려면 1-2로 설정.
     */
    var maxFocusStepsPerEvent: Int = Int.MAX_VALUE

    companion object {
        /** 첫 포커스 결정 시 top → left 우선순위를 만들기 위한 가중치. 기본값: 100000f */
        private const val SCREEN_WIDTH_HINT_PX = 100000f
    }
}

/** SWIPE 모드의 두 단계 상태. */
enum class SwipeMode {
    SELECTION,
    MANIPULATION,
}

/** 스와이프 방향. */
enum class Direction {
    UP, DOWN, LEFT, RIGHT,
}

/** 포커스 가능한 요소를 식별하기 위한 마커 인터페이스. 화면별로 sealed class 등으로 구현. */
interface FocusableElement

/** 모든 화면이 공유하는 루트 scope 식별자. */
object ROOT_SCOPE

/**
 * [SwipeFocusController]에 등록되는 요소의 메타데이터.
 *
 * @param element 식별자 (sealed class 케이스 등)
 * @param bounds 화면 절대 좌표 (window 기준)
 * @param manipulatable true면 activate() 시 조작 모드 진입, false면 즉시 onActivate 실행
 * @param scope 이 요소가 속한 scope. 메뉴/팝업 안의 요소는 해당 scope 키 사용
 * @param onActivate 탭 시 호출
 * @param onActivateAlt 더블탭 시 호출 (기본값: onActivate와 동일)
 * @param onManipulate 조작 모드에서 스와이프 시 호출. deltaPx는 직전 위치로부터의 변위(가로), screenWidthPx는 정규화용 화면 너비
 */
data class FocusableEntry(
    val element: FocusableElement,
    val bounds: Rect,
    val manipulatable: Boolean,
    val scope: Any,
    val onActivate: () -> Unit,
    val onActivateAlt: () -> Unit,
    val onManipulate: (deltaPx: Float, screenWidthPx: Float) -> Unit,
    val gridRow: Int? = null,
    val gridCol: Int? = null,
)

private val Rect.center: Offset get() = Offset((left + right) / 2f, (top + bottom) / 2f)

/** Composition tree 어디서나 활성 [SwipeFocusController]에 접근. null이면 SWIPE 인프라 미활성. */
val LocalSwipeFocusController = compositionLocalOf<SwipeFocusController?> { null }

/** Composable 안에서 [SwipeFocusController]를 기억하고 재사용. */
@Composable
fun rememberSwipeFocusController(): SwipeFocusController = remember { SwipeFocusController() }
