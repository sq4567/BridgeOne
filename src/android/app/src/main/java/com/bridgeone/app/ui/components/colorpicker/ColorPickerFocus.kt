package com.bridgeone.app.ui.components.colorpicker

import com.bridgeone.app.ui.common.ColorCategoryTab
import com.bridgeone.app.ui.common.swipe.FocusableElement

/**
 * ColorPickerSwipe 내부의 SWIPE 모드 포커스 대상 식별자.
 *
 * ColorPickerSwipe는 scope를 파라미터로 받으므로,
 * 이 element 타입과 임의의 scope 객체를 조합해 재사용 가능하다.
 */
sealed class ColorPickerElement : FocusableElement {
    /** 카테고리 셀 — tabId로 식별 ("__all__" 또는 ColorCategory.id) */
    data class CategoryCell(val tabId: String) : ColorPickerElement()
    /** 팔레트 스와치 셀 — hex로 식별 */
    data class Swatch(val hex: String) : ColorPickerElement()
    /** "직접 입력" 화면으로 전환하는 셀 */
    object ExpandToggle : ColorPickerElement()
    /** Hue(색조) 슬라이더 */
    object HueSlider : ColorPickerElement()
    /** Saturation(채도) 슬라이더 */
    object SatSlider : ColorPickerElement()
    /** Value(명도) 슬라이더 */
    object ValSlider : ColorPickerElement()
    /** Hex 문자열 입력 박스 */
    object HexBox : ColorPickerElement()
    /** HSV 값 적용 버튼 */
    object Apply : ColorPickerElement()
}

/**
 * ColorPickerSwipe의 화면 단계 (3단계 drill-down).
 * - [Category]: 카테고리 선택 그리드 (진입 기본 단계)
 * - [Swatches]: 선택 카테고리의 팔레트 스와치 그리드
 * - [DirectInput]: HSV 슬라이더 직접 입력
 */
sealed interface ColorPickerStage {
    /** 카테고리 선택 단계 (1단계). */
    object Category : ColorPickerStage

    /** 스와치 그리드 단계 (2단계). [tab]은 어떤 카테고리를 보여줄지 지정한다. */
    data class Swatches(val tab: ColorCategoryTab) : ColorPickerStage

    /**
     * HSV 슬라이더 직접 입력 단계 (3단계).
     * [sourceTab]이 non-null이면 뒤로가기 시 Swatches(sourceTab)로 복귀.
     * null이면 Category에서 직접 진입한 것이므로 Category로 복귀.
     */
    data class DirectInput(val sourceTab: ColorCategoryTab? = ColorCategoryTab.All) : ColorPickerStage
}
