package com.example.bluelink.model

data class VehicleState(
    val sunroofStatus: String = "알 수 없음",
    val acStatus: String = "알 수 없음",
    // sunroofMode와 acMode 대신 통합 제어 모드 사용
    val overallControlMode: String = "manual" // "manual" 또는 "auto"
)