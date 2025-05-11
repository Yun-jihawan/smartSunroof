package com.example.bluelink.mqtt

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow // MutableStateFlow 사용
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow // StateFlow 사용
import kotlinx.coroutines.flow.asStateFlow // asStateFlow 사용
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

class MqttManager {
    private var client: MqttClient? = null
    private val TAG = "MqttManager"
    private val coroutineScope = CoroutineScope(Dispatchers.IO + Job())

    private val _receivedMessages = MutableSharedFlow<Pair<String, String>>(replay = 0, extraBufferCapacity = 1)
    val receivedMessages: SharedFlow<Pair<String, String>> = _receivedMessages

    // MQTT 연결 상태를 외부로 알리기 위한 StateFlow
    private val _connectionState = MutableStateFlow(MqttConnectionState.IDLE)
    val connectionState: StateFlow<MqttConnectionState> = _connectionState.asStateFlow()

    private var currentTopics = mutableSetOf<String>()
    private var reconnectJob: Job? = null // 재연결 작업을 위한 Job

    fun connect() {
        // 이미 연결 중이거나 연결된 상태면 중복 실행 방지
        if (_connectionState.value == MqttConnectionState.CONNECTING || _connectionState.value == MqttConnectionState.CONNECTED) {
            Log.d(TAG, "MQTT 이미 연결 중이거나 연결되어 있음: ${_connectionState.value}")
            return
        }
        reconnectJob?.cancel() // 기존 재연결 시도 중단
        _connectionState.value = MqttConnectionState.CONNECTING
        Log.d(TAG, "MQTT 연결 시도...")

        coroutineScope.launch {
            try {
                val persistence = MemoryPersistence()
                client = MqttClient(MqttConstants.MQTT_BROKER_URL, MqttConstants.MQTT_CLIENT_ID, persistence)

                val options = MqttConnectOptions().apply {
                    isAutomaticReconnect = false // 수동으로 재연결 관리
                    isCleanSession = true
                    connectionTimeout = 10 // 연결 타임아웃 (초)
                    keepAliveInterval = 20 // KeepAlive 간격 (초)
                }

                client?.setCallback(object : MqttCallbackExtended {
                    override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                        _connectionState.value = MqttConnectionState.CONNECTED
                        Log.d(TAG, "MQTT 연결 완료 (재연결: $reconnect): $serverURI")
                        coroutineScope.launch {
                            currentTopics.forEach { topic ->
                                actualSubscribe(topic)
                            }
                        }
                    }

                    override fun connectionLost(cause: Throwable?) {
                        _connectionState.value = MqttConnectionState.DISCONNECTED
                        Log.e(TAG, "MQTT 연결 끊김: ${cause?.message}", cause)
                        launchReconnect()
                    }

                    override fun messageArrived(topic: String?, message: MqttMessage?) {
                        if (topic != null && message != null) {
                            val msgString = String(message.payload)
                            Log.d(TAG, "MQTT 메시지 수신: Topic='${topic}', Message='${msgString}'")
                            _receivedMessages.tryEmit(Pair(topic, msgString))
                        }
                    }

                    override fun deliveryComplete(token: IMqttDeliveryToken?) {
                        Log.d(TAG, "MQTT 메시지 전달 완료")
                    }
                })

                client?.connect(options) // Blocking call
                // 성공 시 connectComplete 콜백에서 CONNECTED 상태로 변경됨

            } catch (e: MqttException) {
                Log.e(TAG, "MQTT 연결 중 예외 발생: ${e.message}", e)
                _connectionState.value = MqttConnectionState.ERROR
                client = null
                launchReconnect() // 연결 실패 시에도 재연결 시도
            }
        }
    }

    private fun launchReconnect() {
        reconnectJob?.cancel() // 이전 재연결 작업이 있다면 취소
        reconnectJob = coroutineScope.launch {
            if (_connectionState.value == MqttConnectionState.CONNECTED || _connectionState.value == MqttConnectionState.CONNECTING) {
                return@launch // 이미 연결되었거나 연결 시도 중이면 재연결 안 함
            }
            Log.d(TAG, "MQTT 재연결 시퀀스 시작...")
            var attempts = 0
            val maxAttempts = 5 // 최대 재시도 횟수
            var delayTime = 5000L // 초기 딜레이 5초

            while (attempts < maxAttempts && isActive) {
                if (_connectionState.value == MqttConnectionState.CONNECTED) break // 연결 성공 시 루프 종료

                Log.d(TAG, "${delayTime / 1000}초 후 MQTT 재연결 시도... (시도 ${attempts + 1}/$maxAttempts)")
                _connectionState.value = MqttConnectionState.CONNECTING // 재연결 시도 중임을 명시
                delay(delayTime)

                if (isActive && _connectionState.value != MqttConnectionState.CONNECTED) { // delay 후에도 여전히 연결 안됨 상태이고, 코루틴 활성 상태면
                    connect() // 다시 연결 시도 (connect 내부에서 상태를 CONNECTING으로 설정)
                }
                attempts++
                delayTime = (delayTime * 1.5).toLong().coerceAtMost(60000L) // 딜레이 점진적 증가 (최대 1분)
            }

            if (attempts >= maxAttempts && _connectionState.value != MqttConnectionState.CONNECTED) {
                _connectionState.value = MqttConnectionState.ERROR // 최대 시도 후에도 실패 시 오류 상태
                Log.e(TAG, "MQTT 최대 재연결 시도($maxAttempts) 실패.")
            }
        }
    }

    fun disconnect() {
        reconnectJob?.cancel() // 재연결 시도 중단
        _connectionState.value = MqttConnectionState.IDLE
        coroutineScope.launch {
            if (client?.isConnected == true) {
                try {
                    Log.d(TAG, "MQTT 연결 끊기 시도...")
                    client?.disconnect()
                    Log.d(TAG, "MQTT 연결 끊기 성공")
                } catch (e: MqttException) {
                    Log.e(TAG, "MQTT 연결 끊기 중 예외 발생: ${e.message}", e)
                } finally {
                    client = null
                }
            }
        }
    }

    private fun actualSubscribe(topic: String, qos: Int = 1) {
        // 이 함수는 이미 IO 스코프 내에서 호출된다고 가정
        try {
            client?.subscribe(topic, qos)
            Log.d(TAG, "MQTT 토픽 구독 성공: $topic")
        } catch (e: MqttException) {
            Log.e(TAG, "MQTT 토픽 구독 중 예외 발생 ($topic): ${e.message}", e)
            // 구독 실패 시 특정 액션 (예: ViewModel에 알림)
        }
    }

    fun subscribe(topic: String, qos: Int = 1) {
        currentTopics.add(topic)
        coroutineScope.launch { // 구독 요청 자체도 백그라운드에서 처리
            if (client?.isConnected == true) {
                actualSubscribe(topic, qos)
            } else {
                Log.w(TAG, "MQTT 연결되지 않아 토픽 구독 지연: $topic (연결 후 자동 구독 시도)")
            }
        }
    }

    fun publish(topic: String, message: String, qos: Int = 1, retained: Boolean = false) {
        coroutineScope.launch { // 발행 요청도 백그라운드에서 처리
            if (client?.isConnected == true) {
                try {
                    val mqttMessage = MqttMessage(message.toByteArray())
                    mqttMessage.qos = qos
                    mqttMessage.isRetained = retained
                    client?.publish(topic, mqttMessage)
                    Log.d(TAG, "MQTT 메시지 발행 성공: Topic='$topic', Message='$message'")
                } catch (e: MqttException) {
                    Log.e(TAG, "MQTT 메시지 발행 중 예외 발생 ($topic): ${e.message}", e)
                    // 발행 실패 시 특정 액션
                }
            } else {
                Log.w(TAG, "MQTT 연결되지 않아 메시지 발행 불가: Topic='$topic', Message='$message'")
            }
        }
    }

    fun cleanup() {
        disconnect()
        coroutineScope.cancel() // CoroutineScope의 cancel 확장 함수 사용
        Log.d(TAG, "MqttManager 정리됨 (코루틴 스코프 취소)")
    }
}