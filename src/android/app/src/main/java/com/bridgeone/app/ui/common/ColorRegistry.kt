package com.bridgeone.app.ui.common

import androidx.compose.ui.graphics.Color

// ============================================================
// ColorRegistry — 컬러 피커 색상 레지스트리
// ============================================================

/**
 * 컬러 피커 색상 단일 소스.
 * [IconRegistry]와 동형 API를 제공한다 — [colorsFor], [colorsIn], [categories].
 *
 * 각 카테고리마다 12색(6열×2행) 또는 그 배수로 구성해 그리드 정렬을 고려한다.
 * 카테고리 간 색 중복은 허용된다.
 */
object ColorRegistry {

    private val entries: Map<ColorCategory, List<Color>> = linkedMapOf(

        ColorCategory.WARM to listOf(
            // Row 1: 레드·핑크·오렌지·옐로 계열
            Color(0xFFF32121), Color(0xFFF44336), Color(0xFFE91E63), Color(0xFFFF5722), Color(0xFFFF8A00), Color(0xFFF3D021),
            // Row 2: 딥 레드·코럴·핫핑크·복숭아·딥 오렌지·골드
            Color(0xFFD50000), Color(0xFFFF7043), Color(0xFFFF4081), Color(0xFFFF8A65), Color(0xFFFF6D00), Color(0xFFFFD740),
        ),

        ColorCategory.COOL to listOf(
            // Row 1: 앱 블루·인디고·라이트블루·시안·앱 틸
            Color(0xFF2196F3), Color(0xFF1565C0), Color(0xFF3F51B5), Color(0xFF03A9F4), Color(0xFF00BCD4), Color(0xFF20D8AD),
            // Row 2: 블루 다크·딥블루·딥인디고·딥시안·틸 다크·그린틸
            Color(0xFF1976D2), Color(0xFF0D47A1), Color(0xFF283593), Color(0xFF00838F), Color(0xFF00BFA5), Color(0xFF009688),
        ),

        ColorCategory.PASTEL to listOf(
            // Row 1: 파스텔 핑크·피치·옐로·그린·블루·라벤더
            Color(0xFFFFC1CC), Color(0xFFFFD5B0), Color(0xFFFFF5B3), Color(0xFFC8F7C5), Color(0xFFB8DCFF), Color(0xFFD9B3FF),
            // Row 2: 라이트블러시·민트·코럴·라일락·베이비블루·레몬
            Color(0xFFFFB3E6), Color(0xFFB3FFEE), Color(0xFFFFE4B3), Color(0xFFEEB3FF), Color(0xFFB3CAFF), Color(0xFFFFD0B3),
        ),

        ColorCategory.SPRING to listOf(
            // Row 1: 앱 그린·그린400·연두·라임·봄핑크·벚꽃
            Color(0xFF84E268), Color(0xFF66BB6A), Color(0xFFA5D6A7), Color(0xFFE6EE9C), Color(0xFFFFB7C5), Color(0xFFF8BBD0),
            // Row 2: 민트·아쿠아·봄하늘·연보라·형광라임·새싹초록
            Color(0xFF80CBC4), Color(0xFF80DEEA), Color(0xFFFFF59D), Color(0xFFCE93D8), Color(0xFFCCFF90), Color(0xFF4DB6AC),
        ),

        ColorCategory.SUMMER to listOf(
            // Row 1: 시안·앱 틸·블루·선 옐로·딥 오렌지·핫핑크
            Color(0xFF00BCD4), Color(0xFF20D8AD), Color(0xFF2196F3), Color(0xFFFFEB3B), Color(0xFFFF5722), Color(0xFFE91E63),
            // Row 2: 시안A400·라이트블루A200·그린A200·옐로A700·오렌지A700·핑크A200
            Color(0xFF00E5FF), Color(0xFF40C4FF), Color(0xFF69F0AE), Color(0xFFFFEA00), Color(0xFFFF6D00), Color(0xFFFF4081),
        ),

        ColorCategory.AUTUMN to listOf(
            // Row 1: 초콜릿·브라운·브라운400·딥 오렌지·앰버·탄
            Color(0xFFD2691E), Color(0xFF795548), Color(0xFF8D6E63), Color(0xFFFF7043), Color(0xFFFF8F00), Color(0xFFBCAAA4),
            // Row 2: 다크브라운·번트 오렌지·버건디·딥 브라운·시에나·마호가니
            Color(0xFF6D4C41), Color(0xFFBF360C), Color(0xFF4E342E), Color(0xFFE65100), Color(0xFFA0522D), Color(0xFF8B4513),
        ),

        ColorCategory.WINTER to listOf(
            // Row 1: 라이트스틸블루·블루그레이·블루그레이300·블루그레이600·블루그레이400·블루그레이200
            Color(0xFFB0C4DE), Color(0xFF607D8B), Color(0xFF90A4AE), Color(0xFF546E7A), Color(0xFF78909C), Color(0xFFB0BEC5),
            // Row 2: 블루그레이700·딥 윈터·블루그레이100·아이시민트·파우더블루·미드나이트
            Color(0xFF455A64), Color(0xFF263238), Color(0xFFCFD8DC), Color(0xFF80DEEA), Color(0xFF81D4FA), Color(0xFFECEFF1),
        ),

        ColorCategory.MONO to listOf(
            // Row 1: 흰색 → 회색
            Color(0xFFFFFFFF), Color(0xFFF5F5F5), Color(0xFFE0E0E0), Color(0xFFC2C2C2), Color(0xFF9E9E9E), Color(0xFF757575),
            // Row 2: 다크 회색 → 검정
            Color(0xFF616161), Color(0xFF424242), Color(0xFF303030), Color(0xFF1E1E1E), Color(0xFF121212), Color(0xFF000000),
        ),

        ColorCategory.METAL to listOf(
            // Row 1: 실버·크롬·플래티넘·골드·웜 골드·다크 골드
            Color(0xFFD4D4D4), Color(0xFFB8B8C0), Color(0xFFA8A8B0), Color(0xFFFFD700), Color(0xFFDAA520), Color(0xFFB8860B),
            // Row 2: 브론즈·구리·아이언·스틸·다크 스틸·건메탈
            Color(0xFFCD7F32), Color(0xFFC87941), Color(0xFFCCCCCC), Color(0xFF8B8682), Color(0xFF5C5C6E), Color(0xFF3A3A4A),
        ),

        ColorCategory.SPACE to listOf(
            // Row 1: 스타블루·네뷸라·갤럭시·앱 라이트퍼플·앱 퍼플·네뷸라 핑크
            Color(0xFF3F51B5), Color(0xFF673AB7), Color(0xFF9C27B0), Color(0xFF818BFF), Color(0xFFB552F6), Color(0xFFE040FB),
            // Row 2: 스타 시안·딥 스페이스·미드나이트·우주 블랙·슈퍼노바·스타 옐로
            Color(0xFF00E5FF), Color(0xFF1A237E), Color(0xFF1A1A3A), Color(0xFF0D0D1A), Color(0xFFFF6D00), Color(0xFFFFEA00),
        ),

        ColorCategory.APP to listOf(
            // Row 1: 터치패드 기능색 (TouchpadColors.kt 기준)
            Color(0xFFF32121), Color(0xFFFF8A00), Color(0xFFF3D021), Color(0xFF84E268), Color(0xFF20D8AD), Color(0xFF2196F3),
            // Row 2: 터치패드 기능색 + 상태색
            Color(0xFF818BFF), Color(0xFFB552F6), Color(0xFF4CAF50), Color(0xFFF44336), Color(0xFFFF9800), Color(0xFF607D8B),
        ),
    )

    /** entries에 실제 색이 존재하는 카테고리만 (enum 순서 유지). */
    val categories: List<ColorCategory> =
        ColorCategory.entries.filter { entries[it]?.isNotEmpty() == true }

    /**
     * 모든 카테고리를 순서대로 flatten한 뒤 ARGB 기준으로 중복 제거한 전체 색 목록.
     * 카테고리 순서 → 카테고리 내 순서를 유지한다.
     */
    val allColors: List<Color> by lazy {
        val seen = LinkedHashSet<Color>()
        val result = mutableListOf<Color>()
        ColorCategory.entries.forEach { cat ->
            entries[cat]?.forEach { color ->
                if (seen.add(color)) result.add(color)
            }
        }
        result
    }

    /** [category]에 소속된 색 목록 (entries 정의 순서 유지). */
    fun colorsIn(category: ColorCategory): List<Color> =
        entries[category] ?: emptyList()

    /** 탭에 해당하는 색 목록. [ColorCategoryTab.All]이면 전체. */
    fun colorsFor(tab: ColorCategoryTab): List<Color> = when (tab) {
        is ColorCategoryTab.All  -> allColors
        is ColorCategoryTab.Real -> colorsIn(tab.category)
    }
}
