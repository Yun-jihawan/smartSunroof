package com.example.bluelink.model

// 가상 선루프 사용 데이터를 위한 데이터 클래스 (SWR-MOB-17)
data class SunroofUsageData(
    val sunroofModel: String = "Unknown",    // 선루프 모델명
    val totalOperatingHours: Int = 0,      // 총 사용 시간 (시간 단위)
    val openCloseCycles: Int = 0           // 총 개폐 횟수
)