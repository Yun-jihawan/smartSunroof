package com.example.bluelink.mqtt

object MqttConstants {
    const val MQTT_BROKER_URL = "tcp://test.mosquitto.org:1883"
    const val MQTT_CLIENT_ID_BASE = "bluelink_android_app_"

    const val VEHICLE_ID_PLACEHOLDER = "{vehicleId}"
    const val VEHICLE_TOPIC_BASE = "bluelink/vehicle/$VEHICLE_ID_PLACEHOLDER"

    const val TOPIC_SUFFIX_STATUS = "/status"
    const val TOPIC_SUFFIX_ENVIRONMENT = "/environment"
    const val TOPIC_SUFFIX_CONTROL_SUNROOF = "/control/sunroof" // 수동 제어용
    const val TOPIC_SUFFIX_CONTROL_AC = "/control/ac"          // 수동 제어용

    // 모드 변경을 위한 토픽 추가
    const val TOPIC_SUFFIX_SET_SUNROOF_MODE = "/control/sunroof/set_mode"
    const val TOPIC_SUFFIX_SET_AC_MODE = "/control/ac/set_mode"

    fun getStatusTopic(vehicleId: String): String {
        return "$VEHICLE_TOPIC_BASE$TOPIC_SUFFIX_STATUS".replace(VEHICLE_ID_PLACEHOLDER, vehicleId)
    }
    fun getEnvironmentTopic(vehicleId: String): String {
        return "$VEHICLE_TOPIC_BASE$TOPIC_SUFFIX_ENVIRONMENT".replace(VEHICLE_ID_PLACEHOLDER, vehicleId)
    }
    fun getControlSunroofTopic(vehicleId: String): String { // 수동 제어용
        return "$VEHICLE_TOPIC_BASE$TOPIC_SUFFIX_CONTROL_SUNROOF".replace(VEHICLE_ID_PLACEHOLDER, vehicleId)
    }
    fun getControlAcTopic(vehicleId: String): String { // 수동 제어용
        return "$VEHICLE_TOPIC_BASE$TOPIC_SUFFIX_CONTROL_AC".replace(VEHICLE_ID_PLACEHOLDER, vehicleId)
    }
    // 모드 설정 토픽 생성 함수
    fun getSetSunroofModeTopic(vehicleId: String): String {
        return "$VEHICLE_TOPIC_BASE$TOPIC_SUFFIX_SET_SUNROOF_MODE".replace(VEHICLE_ID_PLACEHOLDER, vehicleId)
    }
    fun getSetAcModeTopic(vehicleId: String): String {
        return "$VEHICLE_TOPIC_BASE$TOPIC_SUFFIX_SET_AC_MODE".replace(VEHICLE_ID_PLACEHOLDER, vehicleId)
    }
}