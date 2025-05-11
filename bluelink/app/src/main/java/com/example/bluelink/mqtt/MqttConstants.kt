package com.example.bluelink.mqtt

// MQTT 통신을 위한 상수 정의 객체
object MqttConstants {
    // 테스트용 공개 MQTT 브로커 주소 (실제 환경에서는 보안 브로커 사용)
    const val MQTT_BROKER_URL = "tcp://test.mosquitto.org:1883"
    // 클라이언트 ID (각 클라이언트마다 고유해야 함, 실제로는 동적으로 생성 권장)
    const val MQTT_CLIENT_ID = "bluelink_android_app_test_0123" // 예시 ID

    // 발행(Publish)할 토픽 예시
    const val TOPIC_CONTROL_SUNROOF = "bluelink/vehicle/control/sunroof"
    const val TOPIC_CONTROL_AC = "bluelink/vehicle/control/ac"

    // 구독(Subscribe)할 토픽 예시
    const val TOPIC_VEHICLE_STATUS = "bluelink/vehicle/status" // 차량 상태 (선루프, AC 등)
    const val TOPIC_ENVIRONMENT_DATA = "bluelink/vehicle/environment" // 환경 데이터

    // TODO: 실제 차량 및 서비스에 맞는 토픽 구조로 변경 필요
    // 예시: 특정 차량 ID를 포함하는 토픽 bluelink/vehicle/{vehicleId}/status
}