package com.example.bluelink.model

// 차량 실내외 환경 데이터를 담는 데이터 클래스
data class EnvironmentData(
    val indoorTemperature: Double = 0.0,
    val indoorHumidity: Double = 0.0,
    val outdoorTemperature: Double = 0.0,
    val outdoorHumidity: Double = 0.0,
    val airQuality: String = "알 수 없음",       // 실내 공기질 상태 (예: "좋음 (AQI: 35)")
    val outdoorAirQuality: String = "알 수 없음", // 외부 공기질 상태 추가
    val fineDust: String = "알 수 없음"        // 미세먼지 농도 (예: "보통 (PM2.5: 45µg/m³)")
    // 필요시 수치 필드 추가:
    // val indoorAirQualityIndex: Int? = null,
    // val outdoorAirQualityIndex: Int? = null,
    // val fineDustConcentration: Double? = null
)