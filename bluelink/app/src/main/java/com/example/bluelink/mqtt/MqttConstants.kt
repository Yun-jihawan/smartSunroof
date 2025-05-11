package com.example.bluelink.mqtt

object MqttConstants {
    const val MQTT_BROKER_URL = "tcp://test.mosquitto.org:1883" // TLS는 건너뛰었으므로 비암호화 주소 유지
    const val MQTT_CLIENT_ID_BASE = "bluelink_android_app_" // 클라이언트 ID 기본 문자열

    // 토픽 구조
    const val VEHICLE_ID_PLACEHOLDER = "{vehicleId}"
    const val VEHICLE_TOPIC_BASE = "bluelink/vehicle/$VEHICLE_ID_PLACEHOLDER"

    // 기능별 토픽 경로 접미사
    const val TOPIC_SUFFIX_STATUS = "/status"
    const val TOPIC_SUFFIX_ENVIRONMENT = "/environment"

    const val TOPIC_SUFFIX_CONTROL_BASE = "/control" // 제어 관련 기본 경로
    const val TOPIC_SUFFIX_SUNROOF = "/sunroof"
    const val TOPIC_SUFFIX_AC = "/ac"
    const val TOPIC_SUFFIX_MODE = "/mode"
    const val TOPIC_SUFFIX_COMMAND = "/command" // 실제 명령 발행 시 사용
    const val TOPIC_SUFFIX_RESULT = "/result"   // 명령 결과 수신 시 사용
    const val TOPIC_SUFFIX_SET = "/set"         // 설정 값 변경 시 사용 (예: 모드 설정)


    // --- 토픽 생성 헬퍼 함수들 ---

    // 상태 및 환경 정보 토픽
    fun getStatusTopic(vehicleId: String): String = "$VEHICLE_TOPIC_BASE$TOPIC_SUFFIX_STATUS".replace(VEHICLE_ID_PLACEHOLDER, vehicleId)
    fun getEnvironmentTopic(vehicleId: String): String = "$VEHICLE_TOPIC_BASE$TOPIC_SUFFIX_ENVIRONMENT".replace(VEHICLE_ID_PLACEHOLDER, vehicleId)

    // 선루프 제어 토픽
    fun getSunroofControlCommandTopic(vehicleId: String): String = "$VEHICLE_TOPIC_BASE$TOPIC_SUFFIX_CONTROL_BASE$TOPIC_SUFFIX_SUNROOF$TOPIC_SUFFIX_COMMAND".replace(VEHICLE_ID_PLACEHOLDER, vehicleId)
    fun getSunroofControlResultTopic(vehicleId: String): String = "$VEHICLE_TOPIC_BASE$TOPIC_SUFFIX_CONTROL_BASE$TOPIC_SUFFIX_SUNROOF$TOPIC_SUFFIX_RESULT".replace(VEHICLE_ID_PLACEHOLDER, vehicleId)

    // AC 제어 토픽
    fun getAcControlCommandTopic(vehicleId: String): String = "$VEHICLE_TOPIC_BASE$TOPIC_SUFFIX_CONTROL_BASE$TOPIC_SUFFIX_AC$TOPIC_SUFFIX_COMMAND".replace(VEHICLE_ID_PLACEHOLDER, vehicleId)
    fun getAcControlResultTopic(vehicleId: String): String = "$VEHICLE_TOPIC_BASE$TOPIC_SUFFIX_CONTROL_BASE$TOPIC_SUFFIX_AC$TOPIC_SUFFIX_RESULT".replace(VEHICLE_ID_PLACEHOLDER, vehicleId)

    // 통합 제어 모드 설정 토픽
    fun getOverallModeSetTopic(vehicleId: String): String = "$VEHICLE_TOPIC_BASE$TOPIC_SUFFIX_CONTROL_BASE$TOPIC_SUFFIX_MODE$TOPIC_SUFFIX_SET".replace(VEHICLE_ID_PLACEHOLDER, vehicleId)
    fun getOverallModeSetResultTopic(vehicleId: String): String = "$VEHICLE_TOPIC_BASE$TOPIC_SUFFIX_CONTROL_BASE$TOPIC_SUFFIX_MODE$TOPIC_SUFFIX_SET$TOPIC_SUFFIX_RESULT".replace(VEHICLE_ID_PLACEHOLDER, vehicleId)


    // --- JSON 페이로드 키 상수 ---
    // 공통
    const val KEY_TIMESTAMP = "timestamp"
    const val KEY_COMMAND_ID = "command_id" // 요청 식별자
    const val KEY_COMMAND_ID_REF = "command_id_ref" // 응답에서 참조하는 요청 ID
    const val KEY_COMMAND_TYPE = "command_type"
    const val KEY_COMMAND_TYPE_SUNROOF_CONTROL = "sunroof_control"
    const val KEY_COMMAND_TYPE_AC_CONTROL = "ac_control"
    const val KEY_COMMAND_TYPE_OVERALL_MODE_SET = "overall_mode_set"
    const val KEY_RESULT = "result" // "success", "failure"
    const val KEY_MESSAGE = "message" // 사용자에게 보여줄 메시지
    const val KEY_ERROR_CODE = "error_code" // 실패 시 원인 코드

    // 차량 상태 (.../status)
    const val KEY_SUNROOF_STATUS = "sunroof_status" // "open", "closed", "opening", "closing", "error"
    const val KEY_AC_STATUS = "ac_status"           // "off", "cooling_low", "heating_high" 등
    const val KEY_OVERALL_CONTROL_MODE = "overall_control_mode" // "manual", "auto"
    const val KEY_IS_DRIVING = "is_driving"         // true, false
    const val KEY_CURRENT_SPEED_KMH = "current_speed_kmh" // Integer

    // 환경 정보 (.../environment)
    const val KEY_INDOOR_TEMP_C = "indoor_temp_celsius"
    const val KEY_INDOOR_HUMIDITY_PERCENT = "indoor_humidity_percent"
    const val KEY_OUTDOOR_TEMP_C = "outdoor_temp_celsius"
    const val KEY_OUTDOOR_HUMIDITY_PERCENT = "outdoor_humidity_percent"
    const val KEY_INDOOR_AIR_QUALITY_TEXT = "indoor_air_quality_text" // "좋음", "보통" 등
    const val KEY_INDOOR_AQI = "indoor_aqi"                         // (선택) 공기질 지수 (Int)
    const val KEY_OUTDOOR_AIR_QUALITY_TEXT = "outdoor_air_quality_text"
    const val KEY_OUTDOOR_AQI = "outdoor_aqi"                       // (선택) 공기질 지수 (Int)
    const val KEY_FINE_DUST_PM25_UG_M3 = "fine_dust_pm25_ug_m3"    // (선택) PM2.5 농도 (Double)
    const val KEY_FINE_DUST_STATUS_TEXT = "fine_dust_status_text"   // "낮음", "보통" 등

    // 제어 명령 페이로드 키
    const val KEY_ACTION = "action" // 예: "open", "close", "on", "off"
    const val KEY_MODE = "mode"     // 예: "auto", "manual" (모드 설정 시), 또는 "cooling" (AC 모드)
    const val KEY_FAN_SPEED = "fan_speed" // (선택) AC 팬 속도
    const val KEY_TEMPERATURE_C = "temperature_celsius" // (선택) AC 설정 온도
}