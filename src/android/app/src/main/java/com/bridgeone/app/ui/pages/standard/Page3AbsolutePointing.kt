package com.bridgeone.app.ui.pages.standard

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bridgeone.app.ui.components.AbsolutePointingPad

// ============================================================
// Page 3: 절대좌표 패드 (Phase 4.9.1)
// ============================================================

@Composable
internal fun Page3AbsolutePointing() {
    AbsolutePointingPad(modifier = Modifier.fillMaxSize())
}
