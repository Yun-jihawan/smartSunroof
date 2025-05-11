package com.example.bluelink.mqtt

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
// SSL 관련 import는 TLS 건너뛰기로 인해 제거 또는 주석 처리
// import javax.net.ssl.SSLContext
// import javax.net.ssl.SSLSocketFactory

class MqttManager {
    private var client: MqttClient? = null
    private val TAG = "MqttManager"
    private val coroutineScope = CoroutineScope(Dispatchers.IO + Job())

    private val _receivedMessages = MutableSharedFlow<Pair<String, String>>(replay = 0, extraBufferCapacity = 1)
    val receivedMessages: SharedFlow<Pair<String, String>> = _receivedMessages

    private val _connectionState = MutableStateFlow(MqttConnectionState.IDLE)
    val connectionState: StateFlow<MqttConnectionState> = _connectionState.asStateFlow()

    // 현재 구독 중인 모든 토픽을 관리 (동적 토픽 처리를 위해 중요)
    private val subscribedTopics = mutableSetOf<String>()
    private var reconnectJob: Job? = null

    // SSLSocketFactory 생성 함수는 TLS 건너뛰므로 주석 처리 또는 제거
    /*
    private fun getSocketFactory(): SSLSocketFactory {
        val sslContext = SSLContext.getInstance("TLSv1.2")
        sslContext.init(null, null, null)
        return sslContext.socketFactory
    }
    */

    fun connect() {
        if (_connectionState.value == MqttConnectionState.CONNECTING || _connectionState.value == MqttConnectionState.CONNECTED) {
            Log.d(TAG, "MQTT 이미 연결 중이거나 연결되어 있음: ${_connectionState.value}")
            return
        }
        reconnectJob?.cancel()
        _connectionState.value = MqttConnectionState.CONNECTING
        // 클라이언트 ID에 고유값 추가 (예: 현재 시간, 랜덤 문자열 등) - 매번 새 ID 사용 시 cleanSession=true 권장
        val clientId = MqttConstants.MQTT_CLIENT_ID_BASE + System.currentTimeMillis()
        Log.d(TAG, "MQTT 연결 시도... Client ID: $clientId")


        coroutineScope.launch {
            try {
                val persistence = MemoryPersistence()
                // MQTT_BROKER_URL은 MqttConstants에서 가져옴
                client = MqttClient(MqttConstants.MQTT_BROKER_URL, clientId, persistence)

                val options = MqttConnectOptions().apply {
                    isAutomaticReconnect = false
                    isCleanSession = true // 새 클라이언트 ID 사용 시 이전 세션 정리
                    connectionTimeout = 10
                    keepAliveInterval = 20
                    // socketFactory = getSocketFactory() // TLS 건너뛰므로 이 라인 제거 또는 주석 처리
                }

                client?.setCallback(object : MqttCallbackExtended {
                    override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                        _connectionState.value = MqttConnectionState.CONNECTED
                        Log.d(TAG, "MQTT 연결 완료 (재연결: $reconnect): $serverURI")
                        // 연결 성공 시 저장된 토픽들 (subscribedTopics) 재구독
                        coroutineScope.launch {
                            val topicsToResubscribe = synchronized(subscribedTopics) { subscribedTopics.toList() }
                            Log.d(TAG, "연결 완료 후 다음 토픽들 재구독 시도: $topicsToResubscribe")
                            topicsToResubscribe.forEach { topic ->
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
                        // Log.d(TAG, "MQTT 메시지 전달 완료") // 너무 빈번할 수 있으므로 필요시 주석 해제
                    }
                })
                client?.connect(options)
            } catch (e: MqttException) {
                Log.e(TAG, "MQTT 연결 중 MqttException: ${e.message}", e)
                _connectionState.value = MqttConnectionState.ERROR
                client = null
                launchReconnect()
            } catch (e: Exception) {
                Log.e(TAG, "MQTT 연결 중 일반 Exception: ${e.message}", e)
                _connectionState.value = MqttConnectionState.ERROR
                client = null
                launchReconnect()
            }
        }
    }

    private fun launchReconnect() {
        // ... (이전 재연결 로직과 동일) ...
        reconnectJob?.cancel()
        reconnectJob = coroutineScope.launch {
            if (_connectionState.value == MqttConnectionState.CONNECTED || _connectionState.value == MqttConnectionState.CONNECTING) {
                return@launch
            }
            Log.d(TAG, "MQTT 재연결 시퀀스 시작...")
            var attempts = 0
            val maxAttempts = 5
            var delayTime = 5000L

            while (attempts < maxAttempts && isActive) {
                if (_connectionState.value == MqttConnectionState.CONNECTED) break
                Log.d(TAG, "${delayTime / 1000}초 후 MQTT 재연결 시도... (시도 ${attempts + 1}/$maxAttempts)")
                delay(delayTime)
                if (isActive && _connectionState.value != MqttConnectionState.CONNECTED) {
                    connect()
                }
                attempts++
                delayTime = (delayTime * 1.5).toLong().coerceAtMost(60000L)
            }
            if (attempts >= maxAttempts && _connectionState.value != MqttConnectionState.CONNECTED) {
                _connectionState.value = MqttConnectionState.ERROR
                Log.e(TAG, "MQTT 최대 재연결 시도($maxAttempts) 실패.")
            }
        }
    }

    fun disconnect() {
        // ... (이전과 동일, 로그 메시지에서 (SSL) 부분 제거) ...
        reconnectJob?.cancel()
        _connectionState.value = MqttConnectionState.IDLE
        coroutineScope.launch {
            if (client?.isConnected == true) {
                try {
                    Log.d(TAG, "MQTT 연결 끊기 시도...")
                    // 연결 끊기 전에 구독 중인 모든 토픽 해제
                    val topicsToUnsubscribe = synchronized(subscribedTopics) { subscribedTopics.toList() }
                    if (topicsToUnsubscribe.isNotEmpty()) {
                        client?.unsubscribe(topicsToUnsubscribe.toTypedArray())
                        Log.d(TAG, "다음 토픽들 구독 해제: $topicsToUnsubscribe")
                    }
                    synchronized(subscribedTopics) {
                        subscribedTopics.clear()
                    }
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
        if (client?.isConnected == true) {
            try {
                client?.subscribe(topic, qos)
                synchronized(subscribedTopics) { // 구독 성공 시 목록에 추가
                    subscribedTopics.add(topic)
                }
                Log.d(TAG, "MQTT 토픽 구독 성공: $topic (현재 구독 목록: $subscribedTopics)")
            } catch (e: MqttException) {
                Log.e(TAG, "MQTT 토픽 구독 중 예외 발생 ($topic): ${e.message}", e)
            }
        } else {
            Log.w(TAG, "MQTT 연결 안됨 - 구독 실패: $topic")
        }
    }

    // 여러 토픽을 한 번에 구독하는 함수
    fun subscribeToTopics(topics: List<String>, qos: Int = 1) {
        coroutineScope.launch {
            if (client?.isConnected == true) {
                topics.forEach { topic ->
                    actualSubscribe(topic, qos)
                }
            } else {
                // 연결되지 않았으면, 구독할 토픽 목록에만 추가해두고 연결 후 재구독 시도
                synchronized(subscribedTopics) {
                    subscribedTopics.addAll(topics)
                }
                Log.w(TAG, "MQTT 연결되지 않아 다음 토픽들 구독 지연: $topics (연결 후 자동 구독 시도)")
            }
        }
    }

    // 단일 토픽 구독 함수 (기존 유지 또는 subscribeToTopics 사용으로 통일)
    fun subscribe(topic: String, qos: Int = 1) {
        subscribeToTopics(listOf(topic), qos)
    }

    // 여러 토픽을 한 번에 구독 해제하는 함수
    fun unsubscribeFromTopics(topics: List<String>) {
        coroutineScope.launch {
            if (client?.isConnected == true && topics.isNotEmpty()) {
                try {
                    client?.unsubscribe(topics.toTypedArray())
                    synchronized(subscribedTopics) { // 구독 해제 성공 시 목록에서 제거
                        subscribedTopics.removeAll(topics.toSet())
                    }
                    Log.d(TAG, "MQTT 토픽 구독 해제 성공: $topics (현재 구독 목록: $subscribedTopics)")
                } catch (e: MqttException) {
                    Log.e(TAG, "MQTT 토픽 구독 해제 중 예외 발생 ($topics): ${e.message}", e)
                }
            } else if (topics.isNotEmpty()) {
                // 연결되지 않았어도, 관리 목록에서는 제거
                synchronized(subscribedTopics) {
                    subscribedTopics.removeAll(topics.toSet())
                }
                Log.w(TAG, "MQTT 연결되지 않았지만, 관리 목록에서 다음 토픽들 제거 시도: $topics")
            }
        }
    }
    // 단일 토픽 구독 해제
    fun unsubscribe(topic: String) {
        unsubscribeFromTopics(listOf(topic))
    }


    fun publish(topic: String, message: String, qos: Int = 1, retained: Boolean = false) {
        // ... (이전과 동일, 로그 메시지에서 (SSL) 부분 제거) ...
        coroutineScope.launch {
            if (client?.isConnected == true) {
                try {
                    val mqttMessage = MqttMessage(message.toByteArray())
                    mqttMessage.qos = qos
                    mqttMessage.isRetained = retained
                    client?.publish(topic, mqttMessage)
                    // Log.d(TAG, "MQTT 메시지 발행 성공: Topic='$topic', Message='$message'") // 너무 빈번할 수 있음
                } catch (e: MqttException) {
                    Log.e(TAG, "MQTT 메시지 발행 중 예외 발생 ($topic): ${e.message}", e)
                }
            } else {
                Log.w(TAG, "MQTT 연결되지 않아 메시지 발행 불가: Topic='$topic', Message='$message'")
            }
        }
    }

    fun cleanup() {
        disconnect() // disconnect 내부에서 구독 해제 및 클라이언트 정리 수행
        coroutineScope.cancel()
        Log.d(TAG, "MqttManager 정리됨 (코루틴 스코프 취소)")
    }
}