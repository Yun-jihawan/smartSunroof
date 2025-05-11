package com.example.bluelink.model

data class VehicleState(
    // snake_case 키에 맞춰 필드명 변경 (선택 사항, ViewModel에서 매핑해도 됨)
    // 여기서는 Kotlin 관례(camelCase)를 따르고 ViewModel에서 매핑한다고 가정.
    // 하지만 JSON 키와 필드명이 다르면 GSON/Moshi 등 라이브러리 사용 시 @SerializedName 필요.
    // 현재는 JSONObject 직접 사용하므로 ViewModel에서 키 이름으로 접근.
    val sunroofStatus: String = "알 수 없음",      // from "sunroof_status"
    val acStatus: String = "알 수 없음",           // from "ac_status"
    val overallControlMode: String = "manual",  // from "overall_control_mode"
    val isDriving: Boolean = false,               // from "is_driving"
    val currentSpeed: Int = 0                     // from "current_speed_kmh"
    // val timestamp: Long = 0L (선택 사항: 상태 메시지 자체의 타임스탬프)
)