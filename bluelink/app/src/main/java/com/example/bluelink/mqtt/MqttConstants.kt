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
    const val TOPIC_SUFFIX_CONTROL_SUNROOF = "/control/sunroof"
    const val TOPIC_SUFFIX_CONTROL_AC = "/control/ac"
    const val TOPIC_SUFFIX_SET_SUNROOF_MODE = "/control/sunroof/set_mode"
    const val TOPIC_SUFFIX_SET_AC_MODE = "/control/ac/set_mode"

    // 제어 결과 응답 토픽 추가
    const val TOPIC_SUFFIX_CONTROL_SUNROOF_RESULT = "/control/sunroof/result"
    const val TOPIC_SUFFIX_CONTROL_AC_RESULT = "/control/ac/result"
    const val TOPIC_SUFFIX_SET_SUNROOF_MODE_RESULT = "/control/sunroof/set_mode/result"
    const val TOPIC_SUFFIX_SET_AC_MODE_RESULT = "/control/ac/set_mode/result"


    // --- 토픽 생성 헬퍼 함수들 ---
    fun getStatusTopic(vehicleId: String): String = "$VEHICLE_TOPIC_BASE$TOPIC_SUFFIX_STATUS".replace(VEHICLE_ID_PLACEHOLDER, vehicleId)
    fun getEnvironmentTopic(vehicleId: String): String = "$VEHICLE_TOPIC_BASE$TOPIC_SUFFIX_ENVIRONMENT".replace(VEHICLE_ID_PLACEHOLDER, vehicleId)

    // 제어 명령 토픽
    fun getControlSunroofTopic(vehicleId: String): String = "$VEHICLE_TOPIC_BASE$TOPIC_SUFFIX_CONTROL_SUNROOF".replace(VEHICLE_ID_PLACEHOLDER, vehicleId)
    fun getControlAcTopic(vehicleId: String): String = "$VEHICLE_TOPIC_BASE$TOPIC_SUFFIX_CONTROL_AC".replace(VEHICLE_ID_PLACEHOLDER, vehicleId)
    fun getSetSunroofModeTopic(vehicleId: String): String = "$VEHICLE_TOPIC_BASE$TOPIC_SUFFIX_SET_SUNROOF_MODE".replace(VEHICLE_ID_PLACEHOLDER, vehicleId)
    fun getSetAcModeTopic(vehicleId: String): String = "$VEHICLE_TOPIC_BASE$TOPIC_SUFFIX_SET_AC_MODE".replace(VEHICLE_ID_PLACEHOLDER, vehicleId)

    // 제어 결과 응답 토픽
    fun getControlSunroofResultTopic(vehicleId: String): String = "$VEHICLE_TOPIC_BASE$TOPIC_SUFFIX_CONTROL_SUNROOF_RESULT".replace(VEHICLE_ID_PLACEHOLDER, vehicleId)
    fun getControlAcResultTopic(vehicleId: String): String = "$VEHICLE_TOPIC_BASE$TOPIC_SUFFIX_CONTROL_AC_RESULT".replace(VEHICLE_ID_PLACEHOLDER, vehicleId)
    fun getSetSunroofModeResultTopic(vehicleId: String): String = "$VEHICLE_TOPIC_BASE$TOPIC_SUFFIX_SET_SUNROOF_MODE_RESULT".replace(VEHICLE_ID_PLACEHOLDER, vehicleId)
    fun getSetAcModeResultTopic(vehicleId: String): String = "$VEHICLE_TOPIC_BASE$TOPIC_SUFFIX_SET_AC_MODE_RESULT".replace(VEHICLE_ID_PLACEHOLDER, vehicleId)
}