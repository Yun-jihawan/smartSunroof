package com.example.bluelink.model

data class EnvironmentData(
    val indoorTemperature: Double = 0.0,    // from "indoor_temp_celsius"
    val indoorHumidity: Double = 0.0,       // from "indoor_humidity_percent"
    val outdoorTemperature: Double = 0.0,   // from "outdoor_temp_celsius"
    val outdoorHumidity: Double = 0.0,      // from "outdoor_humidity_percent"
    val indoorAirQualityText: String = "알 수 없음", // from "indoor_air_quality_text"
    val indoorAqi: Int? = null,                     // from "indoor_aqi" (선택적 수치)
    val outdoorAirQualityText: String = "알 수 없음",// from "outdoor_air_quality_text"
    val outdoorAqi: Int? = null,                    // from "outdoor_aqi" (선택적 수치)
    val fineDustPm25: Double? = null,               // from "fine_dust_pm25_ug_m3" (선택적 수치)
    val fineDustStatusText: String = "알 수 없음"   // from "fine_dust_status_text"
    // val timestamp: Long = 0L (선택 사항: 환경 메시지 자체의 타임스탬프)
)