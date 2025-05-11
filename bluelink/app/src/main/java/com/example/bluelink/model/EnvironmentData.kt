package com.example.bluelink.model

// 차량 실내외 환경 데이터를 담는 데이터 클래스
data class EnvironmentData(
    val indoorTemperature: Double = 0.0, // 실내 온도
    val indoorHumidity: Double = 0.0,    // 실내 습도
    val outdoorTemperature: Double = 0.0,// 실외 온도
    val outdoorHumidity: Double = 0.0,   // 실외 습도
    val airQuality: String = "알 수 없음", // 공기질 상태 (예: "좋음", "나쁨")
    val fineDust: String = "알 수 없음"    // 미세먼지 농도 (예: "낮음", "높음")
    // TODO: 필요에 따라 더 많은 환경 데이터 추가
)