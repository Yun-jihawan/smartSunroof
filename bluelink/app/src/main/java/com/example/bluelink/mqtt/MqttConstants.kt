package com.example.bluelink.mqtt

object MqttConstants {
    const val MQTT_BROKER_URL = "tcp://test.mosquitto.org:1883"
    const val MQTT_CLIENT_ID_BASE = "bluelink_android_app_"

    const val VEHICLE_ID_PLACEHOLDER = "{vehicleId}"
    const val VEHICLE_TOPIC_BASE = "bluelink/vehicle/$VEHICLE_ID_PLACEHOLDER"

    // 상태 및 환경 데이터
    const val TOPIC_SUFFIX_STATUS = "/status"
    const val TOPIC_SUFFIX_ENVIRONMENT = "/environment"

    // 제어 명령 토픽
    const val TOPIC_SUFFIX_CONTROL_SUNROOF = "/control/sunroof" // 수동 제어용
    const val TOPIC_SUFFIX_CONTROL_AC = "/control/ac"          // 수동 제어용

    // 통합 자동 모드 설정 명령 및 결과 토픽
    const val TOPIC_SUFFIX_SET_OVERALL_MODE = "/control/set_overall_mode"
    const val TOPIC_SUFFIX_SET_OVERALL_MODE_RESULT = "/control/set_overall_mode/result"

    // !!! 아래 두 개 결과 토픽 상수가 이전 답변에서 누락되었을 수 있습니다 !!!
    // !!! 또는 이 함수들을 직접 호출하는 대신, 아래 헬퍼 함수를 통해 사용해야 합니다 !!!
    const val TOPIC_SUFFIX_CONTROL_SUNROOF_RESULT = "/control/sunroof/result" // 수동 선루프 제어 결과
    const val TOPIC_SUFFIX_CONTROL_AC_RESULT = "/control/ac/result"          // 수동 AC 제어 결과


    // --- 토픽 생성 헬퍼 함수들 ---
    fun getStatusTopic(vehicleId: String): String = "$VEHICLE_TOPIC_BASE$TOPIC_SUFFIX_STATUS".replace(VEHICLE_ID_PLACEHOLDER, vehicleId)
    fun getEnvironmentTopic(vehicleId: String): String = "$VEHICLE_TOPIC_BASE$TOPIC_SUFFIX_ENVIRONMENT".replace(VEHICLE_ID_PLACEHOLDER, vehicleId)

    // 수동 제어 명령 토픽
    fun getControlSunroofTopic(vehicleId: String): String = "$VEHICLE_TOPIC_BASE$TOPIC_SUFFIX_CONTROL_SUNROOF".replace(VEHICLE_ID_PLACEHOLDER, vehicleId)
    fun getControlAcTopic(vehicleId: String): String = "$VEHICLE_TOPIC_BASE$TOPIC_SUFFIX_CONTROL_AC".replace(VEHICLE_ID_PLACEHOLDER, vehicleId)

    // 통합 자동 모드 설정 토픽
    fun getSetOverallModeTopic(vehicleId: String): String = "$VEHICLE_TOPIC_BASE$TOPIC_SUFFIX_SET_OVERALL_MODE".replace(VEHICLE_ID_PLACEHOLDER, vehicleId)
    fun getSetOverallModeResultTopic(vehicleId: String): String = "$VEHICLE_TOPIC_BASE$TOPIC_SUFFIX_SET_OVERALL_MODE_RESULT".replace(VEHICLE_ID_PLACEHOLDER, vehicleId)

    // !!! 아래 두 함수가 올바르게 정의되어 있는지 확인 !!!
    // 수동 제어 결과 응답 토픽
    fun getControlSunroofResultTopic(vehicleId: String): String = "$VEHICLE_TOPIC_BASE$TOPIC_SUFFIX_CONTROL_SUNROOF_RESULT".replace(VEHICLE_ID_PLACEHOLDER, vehicleId)
    fun getControlAcResultTopic(vehicleId: String): String = "$VEHICLE_TOPIC_BASE$TOPIC_SUFFIX_CONTROL_AC_RESULT".replace(VEHICLE_ID_PLACEHOLDER, vehicleId)
}