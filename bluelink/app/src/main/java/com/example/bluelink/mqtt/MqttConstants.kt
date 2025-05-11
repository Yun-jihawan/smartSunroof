package com.example.bluelink.mqtt

object MqttConstants {
    const val MQTT_BROKER_URL = "tcp://test.mosquitto.org:1883" // 비암호화된 주소로 유지 (TLS 건너뛰기)
    const val MQTT_CLIENT_ID_BASE = "bluelink_android_app_" // 기본 클라이언트 ID (뒤에 고유값 추가 권장)

    // 동적 토픽 생성을 위한 기본 경로 및 플레이스홀더
    const val VEHICLE_ID_PLACEHOLDER = "{vehicleId}"
    const val VEHICLE_TOPIC_BASE = "bluelink/vehicle/$VEHICLE_ID_PLACEHOLDER" // 예: bluelink/vehicle/MY_GV70

    // 기능별 토픽 경로 (기본 경로 뒤에 추가됨)
    const val TOPIC_SUFFIX_STATUS = "/status"                // 차량 상태
    const val TOPIC_SUFFIX_ENVIRONMENT = "/environment"      // 환경 데이터
    const val TOPIC_SUFFIX_CONTROL_SUNROOF = "/control/sunroof" // 선루프 제어
    const val TOPIC_SUFFIX_CONTROL_AC = "/control/ac"          // AC 제어

    // 전체적인 와일드카드 구독 (테스트/디버깅용, 실제 앱에서는 구체적 토픽 구독 권장)
    // const val TOPIC_WILDCARD_ALL_VEHICLES = "bluelink/vehicle/#"

    // 기존 고정 토픽들은 이제 사용하지 않거나, 일반 공지용으로만 사용 가능
    // const val TOPIC_CONTROL_SUNROOF = "bluelink/vehicle/control/sunroof" // 삭제 또는 주석
    // const val TOPIC_CONTROL_AC = "bluelink/vehicle/control/ac"          // 삭제 또는 주석
    // const val TOPIC_VEHICLE_STATUS = "bluelink/vehicle/status"          // 삭제 또는 주석
    // const val TOPIC_ENVIRONMENT_DATA = "bluelink/vehicle/environment"  // 삭제 또는 주석

    // 토픽 생성 헬퍼 함수 (선택 사항이지만 권장)
    fun getStatusTopic(vehicleId: String): String {
        return "$VEHICLE_TOPIC_BASE$TOPIC_SUFFIX_STATUS".replace(VEHICLE_ID_PLACEHOLDER, vehicleId)
    }

    fun getEnvironmentTopic(vehicleId: String): String {
        return "$VEHICLE_TOPIC_BASE$TOPIC_SUFFIX_ENVIRONMENT".replace(VEHICLE_ID_PLACEHOLDER, vehicleId)
    }

    fun getControlSunroofTopic(vehicleId: String): String {
        return "$VEHICLE_TOPIC_BASE$TOPIC_SUFFIX_CONTROL_SUNROOF".replace(VEHICLE_ID_PLACEHOLDER, vehicleId)
    }

    fun getControlAcTopic(vehicleId: String): String {
        return "$VEHICLE_TOPIC_BASE$TOPIC_SUFFIX_CONTROL_AC".replace(VEHICLE_ID_PLACEHOLDER, vehicleId)
    }
}