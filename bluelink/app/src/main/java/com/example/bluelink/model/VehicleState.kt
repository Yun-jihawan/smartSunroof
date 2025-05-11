package com.example.bluelink.model

// 차량의 주요 상태 정보를 담는 데이터 클래스
data class VehicleState(
    val sunroofStatus: String = "알 수 없음", // 예: "열림", "닫힘", "열리는 중", "닫히는 중"
    val acStatus: String = "알 수 없음",       // 예: "켜짐 - 냉방", "꺼짐", "켜짐 - 히터"
    val sunroofMode: String = "manual",      // 선루프 제어 모드: "manual", "auto"
    val acMode: String = "manual"            // AC 제어 모드: "manual", "auto"
    // TODO: 필요에 따라 더 많은 차량 상태 정보 추가
)