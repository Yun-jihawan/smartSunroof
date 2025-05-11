package com.example.bluelink.mqtt

// MQTT 연결 상태를 나타내는 Enum 클래스
enum class MqttConnectionState {
    IDLE,          // 초기 상태 또는 연결 해제 상태
    CONNECTING,    // 연결 시도 중
    CONNECTED,     // 연결됨
    DISCONNECTED,  // 연결 끊김 (재연결 시도 가능)
    ERROR          // 연결 오류 발생
}