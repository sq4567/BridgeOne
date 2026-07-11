package com.bridgeone.app.ui.components.touchpad

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.bridgeone.app.ui.utils.AbsolutePointingConstants
import com.bridgeone.app.ui.utils.TouchRatio
import com.bridgeone.app.ui.utils.ZoneMapping
import com.bridgeone.app.ui.utils.ZoneRect
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor

/**
 * 멀티 존/직사각형 ROI 좌표 변환 순수 함수 모음 (Phase 4.9.10).
 *
 * [hitTestPad]([MultiCursorGridGeometry.kt])는 `internal`이라 이 패키지 밖에서 재사용할 수 없어,
 * 이 파일도 같은 `com.bridgeone.app.ui.components.touchpad` 패키지에 둔다.
 *
 * Reference: docs/development-plans/phase-4/phase-4-9-page3-absolute-pointing.md Phase 4.9.10
 */

/**
 * 패드 전체 영역을 [zoneCount](2~8)개 셀로 분할합니다(자동 배치 그리드용, Phase 4.9.11에서 사용).
 * [MultiCursorGridGeometry.divideGridAreas]는 2~4 전용 하드코딩이라 8분할에 재사용할 수 없어 별도 구현한다.
 * 2~4는 기존과 동일한 레이아웃(1×2/1×3/2×2), 5~8은 2행 그리드로 열을 ceil(N/2)/floor(N/2)로 나눠
 * 배치한다(행 우선 번호: 윗줄 좌→우, 아랫줄 좌→우).
 */
internal fun divideZoneAreas(width: Float, height: Float, zoneCount: Int): List<Rect> {
    require(zoneCount in AbsolutePointingConstants.MULTI_ZONE_COUNT_MIN..AbsolutePointingConstants.MULTI_ZONE_COUNT_MAX) {
        "zoneCount must be in ${AbsolutePointingConstants.MULTI_ZONE_COUNT_MIN}..${AbsolutePointingConstants.MULTI_ZONE_COUNT_MAX}, was $zoneCount"
    }
    return when (zoneCount) {
        2 -> listOf(
            Rect(0f, 0f, width / 2f, height),
            Rect(width / 2f, 0f, width, height)
        )
        3 -> listOf(
            Rect(0f, 0f, width / 3f, height),
            Rect(width / 3f, 0f, width * 2f / 3f, height),
            Rect(width * 2f / 3f, 0f, width, height)
        )
        4 -> listOf(
            Rect(0f, 0f, width / 2f, height / 2f),
            Rect(width / 2f, 0f, width, height / 2f),
            Rect(0f, height / 2f, width / 2f, height),
            Rect(width / 2f, height / 2f, width, height)
        )
        else -> {
            val topCols = ceil(zoneCount / 2f).toInt()
            val bottomCols = floor(zoneCount / 2f).toInt()
            val topRow = (0 until topCols).map { col ->
                Rect(width * col / topCols, 0f, width * (col + 1) / topCols, height / 2f)
            }
            val bottomRow = (0 until bottomCols).map { col ->
                Rect(width * col / bottomCols, height / 2f, width * (col + 1) / bottomCols, height)
            }
            topRow + bottomRow
        }
    }
}

/**
 * 패드 절대 px 좌표를 해당 셀(또는 자유배치 [padRect]) 기준 0~1 로컬 비율로 재정규화합니다.
 * 이 정규화를 빠뜨리면 좌표가 셀 오프셋만큼 어긋난다.
 */
internal fun normalizeInZone(pos: Offset, padRect: Rect): TouchRatio {
    val width = padRect.width
    val height = padRect.height
    val ratioX = if (width > 0f) ((pos.x - padRect.left) / width).coerceIn(0f, 1f) else 0f
    val ratioY = if (height > 0f) ((pos.y - padRect.top) / height).coerceIn(0f, 1f) else 0f
    return TouchRatio(ratioX, ratioY)
}

/**
 * 셀 로컬 0~1 좌표를 [pcRect](임의 종횡비 직사각형) 안으로 재매핑합니다.
 * 기존 단일 줌의 applyZoom()(정사각 배율+중심점 기반 축 독립 재매핑)을 일반화한 형태 —
 * x/y를 서로 독립적으로 계산해 임의 종횡비를 표현할 수 있다.
 */
internal fun applyRoi(localRatio: TouchRatio, pcRect: ZoneRect): TouchRatio {
    val outX = (pcRect.minX + localRatio.x * (pcRect.maxX - pcRect.minX)).coerceIn(0f, 1f)
    val outY = (pcRect.minY + localRatio.y * (pcRect.maxY - pcRect.minY)).coerceIn(0f, 1f)
    return TouchRatio(outX, outY)
}

/**
 * [normalizeInZone] 후 [mapping]이 정의됐으면 [applyRoi]를 적용, 아니면 항등(안전 동작,
 * 확대 없이 셀 영역 그대로 stretch)으로 처리합니다.
 */
internal fun resolveZoneRatio(pos: Offset, padRect: Rect, mapping: ZoneMapping): TouchRatio {
    val local = normalizeInZone(pos, padRect)
    return if (mapping.defined) applyRoi(local, mapping.pcRect) else local
}

/**
 * 존/영역 정의 제스처용 실시간 직사각형 계산 (멀티 존 4.9.11, 단일 줌 4.9.12에서 사용).
 * `dx = |finger.x - center.x|`, `dy = |finger.y - center.y|`로 `[center∓dx, center∓dy]`
 * (0~1 클램프) 직사각형을 계산한다. 손가락이 화면 밖으로 나가면 해당 축이 0 또는 1에서
 * 클램프되어 모니터 끝까지 확장된다.
 */
internal fun rectFromCenterDrag(center: TouchRatio, finger: TouchRatio): ZoneRect {
    val dx = abs(finger.x - center.x)
    val dy = abs(finger.y - center.y)
    val minX = (center.x - dx).coerceIn(0f, 1f)
    val maxX = (center.x + dx).coerceIn(0f, 1f)
    val minY = (center.y - dy).coerceIn(0f, 1f)
    val maxY = (center.y + dy).coerceIn(0f, 1f)
    return ZoneRect(minX, minY, maxX, maxY)
}
