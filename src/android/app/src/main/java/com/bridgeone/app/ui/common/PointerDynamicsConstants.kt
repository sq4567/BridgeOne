package com.bridgeone.app.ui.common

import com.bridgeone.app.ui.components.touchpad.DynamicsAlgorithm
import com.bridgeone.app.ui.components.touchpad.PointerDynamicsPreset

// ============================================================
// 커스텀 포인터 다이나믹스 프리셋 데이터 모델 (Phase 4.5.16)
// ============================================================

/** 속도-배율 곡선의 꺾임점 */
data class CurveNode(
    val velocityDpMs: Float,
    val multiplier: Float
)

/** 커스텀 포인터 다이나믹스 프리셋 */
data class CustomPointerDynamicsPreset(
    val id: String,
    val name: String,
    val accelerationCurve: List<CurveNode>,
    val decelerationCurve: List<CurveNode>,
    val description: String = "",
    val iconKey: String = ""
)

/** 커스텀 프리셋에서 선택 가능한 아이콘 목록 (key → AppIconDef) */
val CUSTOM_PRESET_ICON_OPTIONS: List<Pair<String, AppIconDef>> = listOf(
    "star"           to AppIcons.PickStar,
    "flash"          to AppIcons.PickFlash,
    "bolt"           to AppIcons.PickBolt,
    "whatshot"       to AppIcons.PickWhatshot,
    "fast_forward"   to AppIcons.PickFastForward,
    "run"            to AppIcons.PickDirectionsRun,
    "speed"          to AppIcons.PickSpeed,
    "trending_up"    to AppIcons.PickTrendingUp,
    "bar_chart"      to AppIcons.PickBarChart,
    "show_chart"     to AppIcons.PickShowChart,
    "timeline"       to AppIcons.PickTimeline,
    "waves"          to AppIcons.PickWaves,
    "tune"           to AppIcons.PickTune,
    "adjust"         to AppIcons.PickAdjust,
    "filter"         to AppIcons.PickFilter,
    "center_focus"   to AppIcons.PickCenterFocus,
    "gps_fixed"      to AppIcons.PickGpsFixed,
    "my_location"    to AppIcons.PickMyLocation,
    "explore"        to AppIcons.PickExplore,
    "loop"           to AppIcons.PickLoop,
    "favorite"       to AppIcons.PickFavorite,
    "gamepad"        to AppIcons.PickGamepad,
    "extension"      to AppIcons.PickExtension,
    "settings"       to AppIcons.PickSettings,
    "build"          to AppIcons.PickBuild,
    "mouse"          to AppIcons.PickMouse,
    "touch"          to AppIcons.PickTouchApp,
    "timer"          to AppIcons.PickTimer,
    "autorenew"      to AppIcons.PickAutorenew,
    "vibration"      to AppIcons.PickVibration,
)

/** iconKey → AppIconDef 변환 (없으면 null → 이름 2자 텍스트 표시) */
fun customPresetIconOrNull(iconKey: String): AppIconDef? =
    CUSTOM_PRESET_ICON_OPTIONS.firstOrNull { it.first == iconKey }?.second

/** 그래프 편집기 상수 */
object CurveEditorConstants {
    /** 속도 축 최댓값 (dp/ms). 기본값: 6f */
    const val CURVE_VELOCITY_MAX = 6f
    /** 배율 축 최솟값 (0× = 커서 정지). 기본값: 0f */
    const val CURVE_MULTIPLIER_MIN = 0f
    /** 배율 축 최댓값. 기본값: 6f */
    const val CURVE_MULTIPLIER_MAX = 6f
    /** 노드 최대 개수 (양 끝 포함). 기본값: 7 */
    const val CURVE_MAX_NODES = 7
    /** 인접 노드 간 최소 속도 간격 (dp/ms). 기본값: 0.3f */
    const val CURVE_MIN_VELOCITY_GAP = 0.3f
    /** 기존 노드 터치 인식 반경 (dp). 기본값: 12f */
    const val CURVE_SNAP_THRESHOLD_DP = 12f
    /** 노드 추가 시 인접 노드와의 최소 거리 (dp). 기본값: 8f */
    const val CURVE_ADD_MIN_DP = 8f
    /** 템플릿 피커에서 항목 한 칸 이동에 필요한 스와이프 거리 (dp). 기본값: 60f */
    const val TEMPLATE_PICKER_SWIPE_STEP_DP = 60f
    /** 액션 그리드에서 인접 슬롯 한 칸 이동에 필요한 스와이프 거리 (dp). 기본값: 44f */
    const val ACTION_GRID_SWIPE_STEP_DP = 44f
    /** 노드 편집: X 축 한 스텝 이동량 (dp/ms). 기본값: 0.3f */
    const val CURVE_STEP_VELOCITY = 0.3f
    /** 노드 편집: Y 축 한 스텝 이동량 (배율). 기본값: 0.25f */
    const val CURVE_STEP_MULTIPLIER = 0.25f
    /** 스텝 정밀도 배율 프리셋. 인덱스 2(×1)가 기본값. */
    val NODE_STEP_SCALES = floatArrayOf(0.2f, 0.5f, 1f, 2f, 5f)
    /** 스텝 정밀도 라벨. NODE_STEP_SCALES 와 1:1 대응. */
    val NODE_STEP_SCALE_LABELS = listOf("미세", "천천", "기본", "빠름", "매우빠름")
    /** 스텝 정밀도 기본 인덱스 (×1). 기본값: 2 */
    const val NODE_STEP_DEFAULT_INDEX = 2
    /** 스텝 정밀도 피커 한 칸 스와이프 (dp). 기본값: 60f */
    const val NODE_STEP_PICKER_SWIPE_STEP_DP = 60f
    /** 스텝 정밀도 피커 위쪽 스와이프 취소 임계 (dp). 기본값: 40f */
    const val NODE_STEP_PICKER_CANCEL_DP = 40f
    /** 노드 선택 모드에서 인접 노드 한 칸 이동에 필요한 스와이프 거리 (dp). 기본값: 48f */
    const val NODE_SELECT_SWIPE_STEP_DP = 48f
    /** 저장 확인 요약: 저속 기준 속도 (dp/ms). 기본값: 0.5f */
    const val SUMMARY_LOW_VEL = 0.5f
    /** 저장 확인 요약: 중속 기준 속도 (dp/ms). 기본값: 3.0f */
    const val SUMMARY_MID_VEL = 3.0f
    /** 저장 확인 요약: 고속 기준 속도 (dp/ms). 기본값: 6.0f */
    const val SUMMARY_HIGH_VEL = 6.0f
    /** 곡선 제목 필드 고정 추천어. 기본값: 아래 목록 */
    val CURVE_NAME_SUGGESTIONS = listOf("정밀", "게임", "기본", "빠름")
    /** 곡선 설명 필드 고정 추천어. 기본값: 아래 목록 */
    val CURVE_DESC_SUGGESTIONS = listOf("정밀 작업용", "빠른 이동", "기본 설정")
}

/** 기본 커브: 배율 1.0 직선 (기존 코드 호환용) */
fun defaultCurve(): List<CurveNode> = listOf(
    CurveNode(0f, 1.0f),
    CurveNode(CurveEditorConstants.CURVE_VELOCITY_MAX, 1.0f)
)

/** 신규 커스텀 프리셋 기본 가속 곡선: 1× 직선 (1:1 이동) */
fun defaultAccelerationCurve(): List<CurveNode> = defaultCurve()

/** 신규 커스텀 프리셋 기본 감속 곡선: 1× 직선 (1:1 이동) */
fun defaultDecelerationCurve(): List<CurveNode> = defaultCurve()

/**
 * X(velocity) 한 스텝 이동. 양 끝 노드이거나 인접 gap 제약 위반 시 null.
 */
fun stepNodeX(curve: List<CurveNode>, i: Int, delta: Float): List<CurveNode>? {
    if (i == 0 || i == curve.lastIndex) return null
    val newV = curve[i].velocityDpMs + delta
    val minV = curve[i - 1].velocityDpMs + CurveEditorConstants.CURVE_MIN_VELOCITY_GAP
    val maxV = curve[i + 1].velocityDpMs - CurveEditorConstants.CURVE_MIN_VELOCITY_GAP
    if (newV < minV || newV > maxV) return null
    return curve.toMutableList().also { it[i] = it[i].copy(velocityDpMs = newV) }
}

/**
 * Y(multiplier) 한 스텝 이동. 클램프 후 변화 없으면 null.
 */
fun stepNodeY(curve: List<CurveNode>, i: Int, delta: Float): List<CurveNode>? {
    val newM = (curve[i].multiplier + delta)
        .coerceIn(CurveEditorConstants.CURVE_MULTIPLIER_MIN, CurveEditorConstants.CURVE_MULTIPLIER_MAX)
    if (newM == curve[i].multiplier) return null
    return curve.toMutableList().also { it[i] = it[i].copy(multiplier = newM) }
}

/**
 * i 번 노드 다음 중간 위치에 노드 삽입. 제약 위반 시 null.
 */
fun addNodeAfter(curve: List<CurveNode>, i: Int): List<CurveNode>? {
    if (curve.size >= CurveEditorConstants.CURVE_MAX_NODES) return null
    if (i >= curve.lastIndex) return null
    val left = curve[i]
    val right = curve[i + 1]
    if (right.velocityDpMs - left.velocityDpMs < 2 * CurveEditorConstants.CURVE_MIN_VELOCITY_GAP) return null
    val newNode = CurveNode(
        velocityDpMs = (left.velocityDpMs + right.velocityDpMs) / 2f,
        multiplier = (left.multiplier + right.multiplier) / 2f
    )
    return curve.toMutableList().also { it.add(i + 1, newNode) }
}

/**
 * i 번 노드 삭제. 양 끝 노드(0 or last) 이면 null.
 */
fun deleteNodeAt(curve: List<CurveNode>, i: Int): List<CurveNode>? {
    if (i == 0 || i == curve.lastIndex) return null
    return curve.toMutableList().also { it.removeAt(i) }
}

/**
 * 포인터 다이나믹스 프리셋 목록 (Phase 4.3.8)
 *
 * 프리셋 추가/수정/삭제는 이 파일만 변경하면 전체에 반영됩니다.
 * 3~5개 권장. 인덱스 0은 항상 가속 없음(Off)으로 유지하세요.
 */
val DYNAMICS_PRESETS: List<PointerDynamicsPreset> = listOf(
    PointerDynamicsPreset(
        name = "Off",
        algorithm = DynamicsAlgorithm.NONE,
        intensityFactor = 1.0f,
        velocityThresholdDpMs = 0.5f,
        maxMultiplier = 1.0f,
        icon = AppIcons.DynamicsOff,
        description = "속도와 관계없이 일정하게 이동"
    ),
    PointerDynamicsPreset(
        name = "Precision",
        algorithm = DynamicsAlgorithm.WINDOWS_EPP,
        intensityFactor = 0.8f,
        velocityThresholdDpMs = 0.6f,
        maxMultiplier = 2.5f,
        icon = AppIcons.DynamicsPrecision,
        description = "느리면 그대로, 빠르면 약한 S커브 가속"
    ),
    PointerDynamicsPreset(
        name = "Standard",
        algorithm = DynamicsAlgorithm.WINDOWS_EPP,
        intensityFactor = 1.2f,
        velocityThresholdDpMs = 0.5f,
        maxMultiplier = 3.0f,
        icon = AppIcons.DynamicsStandard,
        description = "Windows EPP와 유사한 자연스러운 가속"
    ),
    PointerDynamicsPreset(
        name = "Fast",
        algorithm = DynamicsAlgorithm.LINEAR,
        intensityFactor = 1.5f,
        velocityThresholdDpMs = 0.4f,
        maxMultiplier = 4.0f,
        icon = AppIcons.DynamicsFast,
        description = "속도에 비례하여 커서 이동량 선형 증가"
    ),
)

/** 기본 프리셋 인덱스 (Off). TouchpadState.dynamicsPresetIndex 초기값과 일치해야 합니다. 기본값: 0 */
const val DEFAULT_PRESET_INDEX = 0

// ============================================================
// 커스텀 프리셋 기본 템플릿 (Phase 4.5.16)
// 앱 최초 실행 시 커스텀 프리셋이 없을 때 자동 제공
// ============================================================

// ============================================================
// 커스텀 프리셋 저장 확인 — 자연어 요약 (Phase 4.5.18.7)
// ============================================================

/** 곡선 요약 결과 */
data class CurveDescription(
    val summary: String,
    val lowSpeedMultiplier: Float,
    val midSpeedMultiplier: Float,
    val highSpeedMultiplier: Float,
    val asymmetryLabel: String
)

private fun interpolateCurveValue(curve: List<CurveNode>, v: Float): Float {
    if (curve.isEmpty()) return 1f
    if (v <= curve.first().velocityDpMs) return curve.first().multiplier
    if (v >= curve.last().velocityDpMs) return curve.last().multiplier
    val idx = curve.indexOfFirst { it.velocityDpMs > v }
    val lo = curve[idx - 1]; val hi = curve[idx]
    val t = (v - lo.velocityDpMs) / (hi.velocityDpMs - lo.velocityDpMs)
    return lo.multiplier + t * (hi.multiplier - lo.multiplier)
}

/** 가속·감속 곡선을 받아 사용자용 자연어 요약과 핵심 수치를 반환 */
fun describeCurves(accel: List<CurveNode>, decel: List<CurveNode>): CurveDescription {
    val low  = interpolateCurveValue(accel, CurveEditorConstants.SUMMARY_LOW_VEL)
    val mid  = interpolateCurveValue(accel, CurveEditorConstants.SUMMARY_MID_VEL)
    val high = interpolateCurveValue(accel, CurveEditorConstants.SUMMARY_HIGH_VEL)
    val decelMid = interpolateCurveValue(decel, CurveEditorConstants.SUMMARY_MID_VEL)

    val asymDiff = kotlin.math.abs(mid - decelMid)
    val asymmetryLabel = when {
        asymDiff < 0.3f -> "없음"
        asymDiff < 0.8f -> "약함"
        else -> "강함"
    }

    val lowPart = when {
        low < 0.7f -> "느린 움직임을 더 천천히"
        low > 1.3f -> "느린 움직임도 빠르게"
        else -> "느린 움직임은 자연스럽게"
    }

    val highPart = when {
        high > 2.5f -> "빠른 움직임은 약 ${"%.1f".format(high)}배로 가속"
        high > 1.5f -> "빠른 움직임은 약간 가속"
        high >= 0.7f -> "속도 변화 적음"
        else -> "빠른 움직임을 오히려 줄여 안정"
    }

    val asymPart = if (asymDiff >= 0.5f) {
        if (decelMid < mid) ", 멈출 때 부드럽게 감속" else ", 멈출 때도 빠르게"
    } else ""

    return CurveDescription(
        summary = "$lowPart, $highPart$asymPart.",
        lowSpeedMultiplier = low,
        midSpeedMultiplier = mid,
        highSpeedMultiplier = high,
        asymmetryLabel = asymmetryLabel
    )
}

val CUSTOM_PRESET_TEMPLATES: List<CustomPointerDynamicsPreset> = listOf(
    CustomPointerDynamicsPreset(
        id = "template_balanced",
        name = "균형",
        description = "가속·감속 모두 완만하게 올라가는 범용 커브. 특별한 니즈 없을 때 기본으로 추천.",
        iconKey = "tune",
        accelerationCurve = listOf(
            CurveNode(0f, 1.0f),
            CurveNode(2f, 1.8f),
            CurveNode(4f, 3.0f),
            CurveNode(6f, 4.5f)
        ),
        decelerationCurve = listOf(
            CurveNode(0f, 1.0f),
            CurveNode(3f, 2.0f),
            CurveNode(6f, 3.0f)
        )
    ),
    CustomPointerDynamicsPreset(
        id = "template_precision",
        name = "정밀 우선",
        description = "느리게 움직일 때는 배율 낮게 유지해 정밀 조작에 유리. 빠르게 움직일 때만 커서가 빨라짐.",
        iconKey = "gps_fixed",
        accelerationCurve = listOf(
            CurveNode(0f, 1.0f),
            CurveNode(1f, 1.0f),
            CurveNode(3f, 2.5f),
            CurveNode(6f, 4.0f)
        ),
        decelerationCurve = listOf(
            CurveNode(0f, 1.0f),
            CurveNode(6f, 2.0f)
        )
    ),
    CustomPointerDynamicsPreset(
        id = "template_fast",
        name = "빠른 이동",
        description = "중간 속도부터 배율이 급격히 올라가 넓은 화면을 빠르게 가로지르기 좋음. 정밀 조작은 어려움.",
        iconKey = "bolt",
        accelerationCurve = listOf(
            CurveNode(0f, 1.0f),
            CurveNode(2f, 2.0f),
            CurveNode(4f, 5.0f),
            CurveNode(6f, 6.0f)
        ),
        decelerationCurve = listOf(
            CurveNode(0f, 1.0f),
            CurveNode(2f, 2.0f),
            CurveNode(4f, 5.0f),
            CurveNode(6f, 6.0f)
        )
    ),
    CustomPointerDynamicsPreset(
        id = "template_stable",
        name = "손 떨림 방지",
        description = "감속 배율을 낮게 설정해 손가락이 멈출 때 커서가 튀지 않고 안정적으로 정착함.",
        iconKey = "center_focus",
        accelerationCurve = listOf(
            CurveNode(0f, 1.0f),
            CurveNode(1.5f, 1.0f),
            CurveNode(6f, 3.0f)
        ),
        decelerationCurve = listOf(
            CurveNode(0f, 1.0f),
            CurveNode(6f, 1.5f)
        )
    )
)
