package com.example.bluelink.mqtt

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel // cancel 확장 함수 import 추가
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

// MqttClient를 직접 사용하는 방식으로 리팩토링된 MQTT 관리 클래스
class MqttManager {
    private var client: MqttClient? = null
    private val TAG = "MqttManager"
    // IO 작업을 위한 코루틴 스코프, Job()을 컨텍스트에 추가하여 개별적으로 취소 가능하게 함
    private val coroutineScope = CoroutineScope(Dispatchers.IO + Job())

    private val _receivedMessages = MutableSharedFlow<Pair<String, String>>(replay = 0, extraBufferCapacity = 1)
    val receivedMessages: SharedFlow<Pair<String, String>> = _receivedMessages

    private var currentTopics = mutableSetOf<String>() // 현재 구독 중인 토픽 관리

    fun connect() {
        coroutineScope.launch {
            if (client?.isConnected == true) {
                Log.d(TAG, "MQTT 이미 연결되어 있음")
                return@launch
            }
            try {
                val persistence = MemoryPersistence()
                client = MqttClient(MqttConstants.MQTT_BROKER_URL, MqttConstants.MQTT_CLIENT_ID, persistence)

                val options = MqttConnectOptions().apply {
                    isAutomaticReconnect = false // 수동 재연결 관리
                    isCleanSession = true
                    // options.userName = "username"
                    // options.password = "password".toCharArray()
                }

                client?.setCallback(object : MqttCallbackExtended {
                    override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                        Log.d(TAG, "MQTT 연결 완료 (재연결: $reconnect): $serverURI")
                        coroutineScope.launch {
                            currentTopics.forEach { topic ->
                                actualSubscribe(topic)
                            }
                        }
                    }

                    override fun connectionLost(cause: Throwable?) {
                        Log.e(TAG, "MQTT 연결 끊김: ${cause?.message}", cause)
                        launchReconnect() // 연결 끊김 시 재연결 시도
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

                Log.d(TAG, "MQTT 연결 시도...")
                client?.connect(options)
                Log.d(TAG, "MQTT 연결 성공 (connect 함수 호출 후)")

            } catch (e: MqttException) {
                Log.e(TAG, "MQTT 연결 중 예외 발생: ${e.message}", e)
                client = null
                launchReconnect() // 연결 실패 시에도 재연결 시도
            }
        }
    }

    private fun launchReconnect() {
        // 이미 재연결 코루틴이 실행 중인지 확인하거나, 중복 실행 방지 로직 추가 가능
        coroutineScope.launch {
            if (isActive) { // 현재 스코프가 활성 상태일 때만
                Log.d(TAG, "5초 후 MQTT 재연결 시도...")
                delay(5000)
                // delay 후에도 스코프가 활성 상태이고, 클라이언트가 연결되지 않았다면 재연결
                if (isActive && client?.isConnected != true) {
                    connect()
                }
            }
        }
    }


    fun disconnect() {
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
        try {
            client?.subscribe(topic, qos)
            Log.d(TAG, "MQTT 토픽 구독 성공: $topic")
        } catch (e: MqttException) {
            Log.e(TAG, "MQTT 토픽 구독 중 예외 발생 ($topic): ${e.message}", e)
        }
    }


    fun subscribe(topic: String, qos: Int = 1) {
        currentTopics.add(topic)
        coroutineScope.launch {
            if (client?.isConnected == true) {
                actualSubscribe(topic, qos)
            } else {
                Log.w(TAG, "MQTT 연결되지 않아 토픽 구독 지연: $topic")
            }
        }
    }

    fun publish(topic: String, message: String, qos: Int = 1, retained: Boolean = false) {
        coroutineScope.launch {
            if (client?.isConnected == true) {
                try {
                    val mqttMessage = MqttMessage(message.toByteArray())
                    mqttMessage.qos = qos
                    mqttMessage.isRetained = retained
                    client?.publish(topic, mqttMessage)
                    Log.d(TAG, "MQTT 메시지 발행 성공: Topic='$topic', Message='$message'")
                } catch (e: MqttException) {
                    Log.e(TAG, "MQTT 메시지 발행 중 예외 발생 ($topic): ${e.message}", e)
                }
            } else {
                Log.w(TAG, "MQTT 연결되지 않아 메시지 발행 불가: Topic='$topic', Message='$message'")
            }
        }
    }

    // MqttManager가 더 이상 필요 없을 때 코루틴 스코프 정리
    fun cleanup() {
        disconnect() // 연결 해제 시도
        // coroutineScope.coroutineContext[Job]?.cancel() // Job을 직접 가져와서 취소하거나
        coroutineScope.cancel() // CoroutineScope의 cancel 확장 함수 사용 (권장)
        Log.d(TAG, "MqttManager 정리됨 (코루틴 스코프 취소)")
    }
}