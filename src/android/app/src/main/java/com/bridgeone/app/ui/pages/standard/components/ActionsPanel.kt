package com.bridgeone.app.ui.pages.standard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
internal fun ActionsPanel(
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
