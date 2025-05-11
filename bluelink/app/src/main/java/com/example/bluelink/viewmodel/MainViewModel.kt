package com.example.bluelink.viewmodel

// import android.app.Application // Application 컨텍스트 사용 제거
import android.util.Log
import androidx.lifecycle.ViewModel // ViewModel로 다시 변경 (Application 컨텍스트 불필요)
import androidx.lifecycle.viewModelScope
import com.example.bluelink.model.EnvironmentData
import com.example.bluelink.model.VehicleState
import com.example.bluelink.mqtt.MqttConstants
import com.example.bluelink.mqtt.MqttManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.json.JSONObject

// ViewModel로 변경 (Application 컨텍스트가 MqttManager의 새 구현에 필요하지 않음)
class MainViewModel : ViewModel() { // AndroidViewModel 대신 ViewModel 사용

    private val _vehicleState = MutableStateFlow(VehicleState())
    val vehicleState: StateFlow<VehicleState> = _vehicleState.asStateFlow()

    // ... (다른 StateFlow 선언은 동일) ...
    private val _environmentData = MutableStateFlow(EnvironmentData())
    val environmentData: StateFlow<EnvironmentData> = _environmentData.asStateFlow()

    private val _maintenanceNotification = MutableStateFlow("")
    val maintenanceNotification: StateFlow<String> = _maintenanceNotification.asStateFlow()

    private val _registeredVehicleInfo = MutableStateFlow("")
    val registeredVehicleInfo: StateFlow<String> = _registeredVehicleInfo.asStateFlow()

    private val _qrScanResult = MutableStateFlow("")
    val qrScanResult: StateFlow<String> = _qrScanResult.asStateFlow()


    // MqttManager 인스턴스 (Application 컨텍스트 없이 생성)
    private val mqttManager: MqttManager = MqttManager()

    init {
        mqttManager.connect()
        observeMqttMessages()

        viewModelScope.launch {
            // MqttClient.connect()가 비동기 코루틴 내에서 실행되므로,
            // 구독은 connect() 호출 후 또는 connectComplete 콜백에서 수행하는 것이 더 안정적입니다.
            // MqttManager 내부의 connectComplete에서 구독을 시도하도록 변경했으므로 여기서는 즉시 호출.
            subscribeToTopics()
        }
        checkMaintenance()
    }

    private fun subscribeToTopics() {
        mqttManager.subscribe(MqttConstants.TOPIC_VEHICLE_STATUS)
        mqttManager.subscribe(MqttConstants.TOPIC_ENVIRONMENT_DATA)
    }

    private fun observeMqttMessages() {
        mqttManager.receivedMessages
            .onEach { (topic, message) ->
                Log.d("MainViewModel", "MQTT 메시지 수신: $topic -> $message")
                try {
                    when (topic) {
                        MqttConstants.TOPIC_VEHICLE_STATUS -> {
                            val jsonObj = JSONObject(message)
                            _vehicleState.value = VehicleState(
                                sunroofStatus = jsonObj.optString("sunroof", _vehicleState.value.sunroofStatus),
                                acStatus = jsonObj.optString("ac", _vehicleState.value.acStatus)
                            )
                        }
                        MqttConstants.TOPIC_ENVIRONMENT_DATA -> {
                            val jsonObj = JSONObject(message)
                            _environmentData.value = EnvironmentData(
                                indoorTemperature = jsonObj.optDouble("indoorTemp", _environmentData.value.indoorTemperature),
                                indoorHumidity = jsonObj.optDouble("indoorHum", _environmentData.value.indoorHumidity),
                                outdoorTemperature = jsonObj.optDouble("outdoorTemp", _environmentData.value.outdoorTemperature),
                                outdoorHumidity = jsonObj.optDouble("outdoorHum", _environmentData.value.outdoorHumidity),
                                airQuality = jsonObj.optString("airQuality", _environmentData.value.airQuality),
                                fineDust = jsonObj.optString("fineDust", _environmentData.value.fineDust)
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MainViewModel", "MQTT 메시지 파싱 오류: $message", e)
                }
            }
            .launchIn(viewModelScope)
    }

    fun controlSunroof(action: String) {
        val commandMessage = JSONObject().put("command", action).toString()
        mqttManager.publish(MqttConstants.TOPIC_CONTROL_SUNROOF, commandMessage)
    }

    fun controlAC(action: String) {
        val commandMessage = JSONObject().put("command", action).toString()
        mqttManager.publish(MqttConstants.TOPIC_CONTROL_AC, commandMessage)
    }

    private fun checkMaintenance() {
        if (Math.random() < 0.3) {
            _maintenanceNotification.value = "선루프 정기 점검이 필요합니다."
        }
    }

    fun processQrScanResult(contents: String?) {
        if (contents != null) {
            _qrScanResult.value = contents
        } else {
            _qrScanResult.value = ""
        }
    }

    fun registerVehicle(vehicleInfoFromQr: String) {
        _registeredVehicleInfo.value = vehicleInfoFromQr
        _qrScanResult.value = ""
    }

    fun clearRegistration() {
        _registeredVehicleInfo.value = ""
        _qrScanResult.value = ""
    }

    override fun onCleared() {
        super.onCleared()
        mqttManager.cleanup() // MqttManager 정리 함수 호출
        Log.d("MainViewModel", "ViewModel 파괴, MqttManager 정리됨")
    }
}