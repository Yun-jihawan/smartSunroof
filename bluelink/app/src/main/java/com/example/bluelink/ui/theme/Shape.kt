package com.example.bluelink.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val AppShapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp), // 버튼, 카드 등에 사용될 수 있는 중간 크기 둥근 모서리
    large = RoundedCornerShape(16.dp) // 더 큰 요소에 사용될 수 있는 둥근 모서리
)