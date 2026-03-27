package com.bridgeone.app.ui.common

import androidx.compose.ui.unit.dp

// ============================================================
// 레이아웃 Safe Zone 상수
// ============================================================
//
// 화면 상하단의 Safe Zone을 정의합니다.
//
// - TOP_SAFE_ZONE: 화면 최상단은 엄지손가락으로 닿기 어렵기 때문에
//   컴포넌트를 배치하지 않는 영역입니다.
//
// - BOTTOM_SAFE_ZONE: 화면 최하단은 내비게이션 바를 꺼내기 위한
//   스와이프 제스처 영역과 겹칩니다. 스와이프 시 컴포넌트를 잘못
//   건드리는 사고를 방지하기 위해 컴포넌트를 배치하지 않는 영역입니다.
//
// 이 상수들은 BridgeOneApp.kt에서 한 번만 적용되며,
// 모든 Active 상태 페이지는 자동으로 이 제약 안에 배치됩니다.

val TOP_SAFE_ZONE = 40.dp
val BOTTOM_SAFE_ZONE = 40.dp
