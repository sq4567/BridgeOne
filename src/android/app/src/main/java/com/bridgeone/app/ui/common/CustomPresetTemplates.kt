package com.bridgeone.app.ui.common

// ============================================================
// 커스텀 프리셋 기본 템플릿 (Phase 4.5.16)
// 앱 최초 실행 시 커스텀 프리셋이 없을 때 자동 제공
// ============================================================

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
