package com.example.bluelink.model

data class VehicleState(
    val sunroofStatus: String = "알 수 없음",
    val acStatus: String = "알 수 없음",
    val overallControlMode: String = "manual", // "manual" 또는 "auto"
    val isDriving: Boolean = false,           // 주행 중 여부
    val currentSpeed: Int = 0                 // 현재 속력 (km/h)
)