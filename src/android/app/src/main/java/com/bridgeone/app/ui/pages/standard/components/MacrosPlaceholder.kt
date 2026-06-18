package com.bridgeone.app.ui.pages.standard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ============================================================
// Macros Placeholder (Phase 4.2.5)
// ============================================================

/**
 * Macros 세로 리스트 (Disabled 상태)
 *
 * 3개 매크로 버튼: Macro 1, Macro 2, Macro 3
 * - 항상 Disabled 상태 (#C2C2C2, alpha 0.6)
 * - 탭 시 아무 동작 없음
 * - PlayArrow 아이콘 표시
 */
@Composable
internal fun MacrosPlaceholder() {
    val macros = listOf("Macro 1", "Macro 2", "Macro 3")

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        macros.forEach { label ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .alpha(0.6f)
                    .background(Color(0xFF2A2A2A), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Color(0xFFC2C2C2),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        color = Color(0xFFC2C2C2)
                    )
                }
            }
        }
    }
}
