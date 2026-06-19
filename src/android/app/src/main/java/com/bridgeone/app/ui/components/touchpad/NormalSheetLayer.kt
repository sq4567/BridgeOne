package com.bridgeone.app.ui.components.touchpad

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable

/**
 * NORMAL 모드 전용 바텀시트 레이어 (Phase 4.7.5-D 추출).
 *
 * 아이콘/컬러 선택 바텀시트(단일 액션 + 액션 순환 후보)를 모은다. SWIPE 모드는 Box 안의
 * `CategoryIconDrawer`/`ColorPickerSwipe`를 쓰므로 본 레이어는 NORMAL 모드에서만 호출된다
 * (모드 가드는 호출부가 담당). 아이콘 시트는 호출부가 소유한 공유 [iconSheetState] 인스턴스를
 * 받아 두 시트가 동일 상태를 공유한다(재생성 금지).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NormalSheetLayer(
    selectedZone: EdgeZone?,
    iconSheetState: SheetState,
    showIconSheet: Boolean,
    onShowIconSheetChange: (Boolean) -> Unit,
    showCandidateIconSheet: Boolean,
    onShowCandidateIconSheetChange: (Boolean) -> Unit,
    showColorPicker: Boolean,
    onShowColorPickerChange: (Boolean) -> Unit,
    showCandidateColorPicker: Boolean,
    onShowCandidateColorPickerChange: (Boolean) -> Unit,
    rotationDraft: RotationCandidate,
    onRotationDraftChange: (RotationCandidate) -> Unit,
    updateSelectedZone: (EdgeZone) -> Unit,
) {
    // ── 아이콘 선택 바텀시트 (NORMAL 모드 전용; SWIPE 모드는 Box 안의 CategoryIconDrawer 사용) ──
    if (showIconSheet) {
        val displayIconKeyForSheet = run {
            val trigger = selectedZone?.trigger as? EdgeZoneTrigger.SingleAction
            trigger?.iconKey?.ifEmpty { selectedZone?.action?.defaultIconKey() ?: "" } ?: ""
        }
        NormalCategoryIconSheet(
            selectedIconKey = displayIconKeyForSheet,
            sheetState = iconSheetState,
            onPick = { key ->
                selectedZone?.let { updateSelectedZone(it.withIconKey(key)) }
                onShowIconSheetChange(false)
            },
            onDismiss = { onShowIconSheetChange(false) },
        )
    }

    // ── 액션 순환 후보 아이콘 선택 바텀시트 (NORMAL 모드 전용) ──
    if (showCandidateIconSheet) {
        val candidateIconKey = rotationDraft.iconKey.ifEmpty { rotationDraft.action.defaultIconKey() }
        NormalCategoryIconSheet(
            selectedIconKey = candidateIconKey,
            sheetState = iconSheetState,
            onPick = { key ->
                onRotationDraftChange(rotationDraft.copy(iconKey = key))
                onShowCandidateIconSheetChange(false)
            },
            onDismiss = { onShowCandidateIconSheetChange(false) },
        )
    }

    // ── 컬러 피커 바텀시트 (NORMAL 모드 전용; SWIPE 모드는 Box 안의 ColorPickerSwipe 사용) ──
    if (showColorPicker) {
        val colorSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val currentColorHex = run {
            val trigger = selectedZone?.trigger as? EdgeZoneTrigger.SingleAction
            trigger?.colorHex ?: ""
        }
        NormalCategoryColorSheet(
            selectedColorHex = currentColorHex,
            sheetState = colorSheetState,
            onPick = { hex ->
                selectedZone?.let { updateSelectedZone(it.withColor(hex)) }
                onShowColorPickerChange(false)
            },
            onDismiss = { onShowColorPickerChange(false) },
        )
    }

    // ── 액션 순환 후보 컬러 피커 바텀시트 (NORMAL 모드 전용) ──
    if (showCandidateColorPicker) {
        val candidateColorSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        NormalCategoryColorSheet(
            selectedColorHex = rotationDraft.colorHex,
            sheetState = candidateColorSheetState,
            onPick = { hex ->
                onRotationDraftChange(rotationDraft.copy(colorHex = hex))
                onShowCandidateColorPickerChange(false)
            },
            onDismiss = { onShowCandidateColorPickerChange(false) },
        )
    }
}
