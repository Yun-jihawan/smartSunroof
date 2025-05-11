package com.example.bluelink.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bluelink.model.EnvironmentData
import com.example.bluelink.model.ReservationDetails
import com.example.bluelink.model.SunroofUsageData
import com.example.bluelink.model.VehicleState
import com.example.bluelink.mqtt.MqttConstants
import com.example.bluelink.mqtt.MqttConnectionState // MqttConnectionState import
import com.example.bluelink.mqtt.MqttManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel // Channel for single-shot events
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow // receiveAsFlow for Channel
import kotlinx.coroutines.launch
import org.json.JSONObject

class MainViewModel : ViewModel() {

    // ... (기존 StateFlow 선언들) ...
    private val _vehicleState = MutableStateFlow(VehicleState())
    val vehicleState: StateFlow<VehicleState> = _vehicleState.asStateFlow()

    private val _environmentData = MutableStateFlow(EnvironmentData())
    val environmentData: StateFlow<EnvironmentData> = _environmentData.asStateFlow()

    private val _maintenanceNotification = MutableStateFlow("")
    val maintenanceNotification: StateFlow<String> = _maintenanceNotification.asStateFlow()

    private val _sunroofUsage = MutableStateFlow(SunroofUsageData("Model_S", 750, 3200))
    val sunroofUsage: StateFlow<SunroofUsageData> = _sunroofUsage.asStateFlow()

    private val _showReservationForm = MutableStateFlow(false)
    val showReservationForm: StateFlow<Boolean> = _showReservationForm.asStateFlow()

    private val _reservationDetails = MutableStateFlow(ReservationDetails())
    val reservationDetails: StateFlow<ReservationDetails> = _reservationDetails.asStateFlow()

    private val _reservationStatusMessage = MutableStateFlow("")
    val reservationStatusMessage: StateFlow<String> = _reservationStatusMessage.asStateFlow()

    val availableServiceCenters = listOf("블루링크 강남점", "블루링크 수원점", "블루링크 부산점")

    private val _isSunroofCommandInProgress = MutableStateFlow(false)
    val isSunroofCommandInProgress: StateFlow<Boolean> = _isSunroofCommandInProgress.asStateFlow()

    private val _isAcCommandInProgress = MutableStateFlow(false)
    val isAcCommandInProgress: StateFlow<Boolean> = _isAcCommandInProgress.asStateFlow()

    private var sunroofControlJob: Job? = null
    private var acControlJob: Job? = null

    private val _registeredVehicleInfo = MutableStateFlow("")
    val registeredVehicleInfo: StateFlow<String> = _registeredVehicleInfo.asStateFlow()

    private val _qrScanResult = MutableStateFlow("")
    val qrScanResult: StateFlow<String> = _qrScanResult.asStateFlow()

    private val mqttManager: MqttManager = MqttManager()

    // --- MQTT 연결 상태 및 오류 이벤트 ---
    private val _mqttConnectionState = MutableStateFlow(MqttConnectionState.IDLE)
    val mqttConnectionState: StateFlow<MqttConnectionState> = _mqttConnectionState.asStateFlow()

    // 일회성 이벤트를 위한 Channel (예: Toast 메시지)
    private val _mqttErrorEventChannel = Channel<String>()
    val mqttErrorEvent = _mqttErrorEventChannel.receiveAsFlow() // UI에서 구독하여 사용

    init {
        // MqttManager의 connectionState를 구독하여 ViewModel의 상태 업데이트
        mqttManager.connectionState
            .onEach { state ->
                _mqttConnectionState.value = state
                Log.d("MainViewModel", "MQTT Connection State Changed: $state")
                if (state == MqttConnectionState.CONNECTED) {
                    // 연결 성공 시 토픽 구독 (MqttManager 내부에서도 시도하지만, 여기서도 확인 가능)
                    subscribeToTopics()
                } else if (state == MqttConnectionState.ERROR && _mqttConnectionState.value != MqttConnectionState.CONNECTING) { // 연결 중 에러는 launchReconnect에서 처리
                    // ViewModel에서 일반적인 오류 이벤트 발생 (UI에서 Snackbar 등으로 표시 가능)
                    // _mqttErrorEventChannel.send("MQTT 연결 오류가 발생했습니다. 잠시 후 다시 시도합니다.")
                }
            }
            .launchIn(viewModelScope)

        // 초기 연결 시도
        mqttManager.connect() // MqttManager 생성자에서 connect()를 호출하지 않는 경우 여기서 호출

        observeMqttMessages()
        // subscribeToTopics() // 연결 성공 후 호출되도록 변경
        checkMaintenance()
    }

    private fun subscribeToTopics() {
        // 연결 상태 확인 후 구독 시도
        if (_mqttConnectionState.value == MqttConnectionState.CONNECTED) {
            mqttManager.subscribe(MqttConstants.TOPIC_VEHICLE_STATUS)
            mqttManager.subscribe(MqttConstants.TOPIC_ENVIRONMENT_DATA)
        } else {
            Log.w("MainViewModel", "MQTT 연결되지 않아 토픽 구독 불가 (현재 상태: ${_mqttConnectionState.value})")
        }
    }

    private fun observeMqttMessages() {
        mqttManager.receivedMessages
            .onEach { (topic, message) ->
                // ... (이전 메시지 처리 로직과 동일) ...
                Log.d("MainViewModel", "MQTT 메시지 수신: $topic -> $message")
                try {
                    when (topic) {
                        MqttConstants.TOPIC_VEHICLE_STATUS -> {
                            val jsonObj = JSONObject(message)
                            val newSunroofStatus = jsonObj.optString("sunroof", _vehicleState.value.sunroofStatus)
                            val newAcStatus = jsonObj.optString("ac", _vehicleState.value.acStatus)

                            if (newSunroofStatus != _vehicleState.value.sunroofStatus && _isSunroofCommandInProgress.value) {
                                sunroofControlJob?.cancel()
                                _isSunroofCommandInProgress.value = false
                                Log.d("MainViewModel", "선루프 상태 MQTT 업데이트로 로딩 해제: $newSunroofStatus")
                            }
                            if (newAcStatus != _vehicleState.value.acStatus && _isAcCommandInProgress.value) {
                                acControlJob?.cancel()
                                _isAcCommandInProgress.value = false
                                Log.d("MainViewModel", "AC 상태 MQTT 업데이트로 로딩 해제: $newAcStatus")
                            }

                            _vehicleState.value = VehicleState(
                                sunroofStatus = newSunroofStatus,
                                acStatus = newAcStatus
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
                    _mqttErrorEventChannel.trySend("수신 데이터 처리 중 오류가 발생했습니다.") // 파싱 오류 시 이벤트 발생
                }
            }
            .launchIn(viewModelScope)
    }

    // MQTT 재연결을 명시적으로 시도하는 함수 (UI에서 버튼 등으로 호출 가능)
    fun attemptMqttReconnect() {
        if (_mqttConnectionState.value != MqttConnectionState.CONNECTING && _mqttConnectionState.value != MqttConnectionState.CONNECTED) {
            Log.d("MainViewModel", "사용자 요청으로 MQTT 재연결 시도")
            mqttManager.connect() // MqttManager의 connect 함수는 내부적으로 상태를 CONNECTING으로 변경하고 재연결 로직을 포함할 수 있음
        } else {
            Log.d("MainViewModel", "이미 연결 중이거나 연결된 상태입니다.")
        }
    }


    fun controlSunroof(action: String) {
        if (_mqttConnectionState.value != MqttConnectionState.CONNECTED) {
            viewModelScope.launch { _mqttErrorEventChannel.send("MQTT가 연결되지 않아 명령을 보낼 수 없습니다.") }
            return
        }
        // ... (이전 제어 로직과 동일) ...
        if (_isSunroofCommandInProgress.value || _isAcCommandInProgress.value) {
            Log.w("MainViewModel", "다른 제어 명령이 이미 진행 중입니다.")
            viewModelScope.launch { _mqttErrorEventChannel.send("다른 제어 명령이 이미 진행 중입니다.") }
            return
        }
        val commandMessage = JSONObject().put("command", action).toString()
        mqttManager.publish(MqttConstants.TOPIC_CONTROL_SUNROOF, commandMessage)
        _isSunroofCommandInProgress.value = true
        Log.d("MainViewModel", "선루프 제어 명령 발행: $action, 로딩 시작")

        sunroofControlJob?.cancel()
        sunroofControlJob = viewModelScope.launch {
            delay(10000)
            if (_isSunroofCommandInProgress.value) {
                _isSunroofCommandInProgress.value = false
                Log.w("MainViewModel", "선루프 제어 응답 시간 초과")
                _mqttErrorEventChannel.trySend("선루프 제어 응답이 없습니다.")
            }
        }
    }

    fun controlAC(action: String) {
        if (_mqttConnectionState.value != MqttConnectionState.CONNECTED) {
            viewModelScope.launch { _mqttErrorEventChannel.send("MQTT가 연결되지 않아 명령을 보낼 수 없습니다.") }
            return
        }
        // ... (이전 제어 로직과 동일) ...
        if (_isSunroofCommandInProgress.value || _isAcCommandInProgress.value) {
            Log.w("MainViewModel", "다른 제어 명령이 이미 진행 중입니다.")
            viewModelScope.launch { _mqttErrorEventChannel.send("다른 제어 명령이 이미 진행 중입니다.") }
            return
        }
        val commandMessage = JSONObject().put("command", action).toString()
        mqttManager.publish(MqttConstants.TOPIC_CONTROL_AC, commandMessage)
        _isAcCommandInProgress.value = true
        Log.d("MainViewModel", "AC 제어 명령 발행: $action, 로딩 시작")

        acControlJob?.cancel()
        acControlJob = viewModelScope.launch {
            delay(10000)
            if (_isAcCommandInProgress.value) {
                _isAcCommandInProgress.value = false
                Log.w("MainViewModel", "AC 제어 응답 시간 초과")
                _mqttErrorEventChannel.trySend("AC 제어 응답이 없습니다.")
            }
        }
    }

    // ... (checkMaintenance, 예약 관련 함수, QR 코드 처리 함수 등은 이전과 동일하게 유지) ...
    fun checkMaintenance() {
        val usage = _sunroofUsage.value
        var notification = ""
        if (usage.totalOperatingHours > 1000) {
            notification += "선루프 사용 시간이 1000시간을 초과했습니다. 점검이 필요합니다. "
        }
        if (usage.openCloseCycles > 5000) {
            notification += "선루프 개폐 횟수가 5000회를 초과했습니다. 부품 점검을 권장합니다."
        }

        if (notification.isNotEmpty()) {
            _maintenanceNotification.value = "모델 [${usage.sunroofModel}]: $notification"
        } else {
            if (Math.random() < 0.2) {
                _maintenanceNotification.value = "선루프 정기 점검 시기가 되었습니다."
            } else {
                _maintenanceNotification.value = ""
            }
        }
    }

    fun toggleReservationForm(show: Boolean) {
        _showReservationForm.value = show
        if (!show) {
            _reservationStatusMessage.value = ""
        }
    }

    fun updateReservationDate(date: String) {
        _reservationDetails.value = _reservationDetails.value.copy(date = date)
    }
    fun updateReservationTime(time: String) {
        _reservationDetails.value = _reservationDetails.value.copy(time = time)
    }
    fun updateSelectedServiceCenter(center: String) {
        _reservationDetails.value = _reservationDetails.value.copy(serviceCenter = center)
    }
    fun updateServiceRequestDetails(details: String) {
        _reservationDetails.value = _reservationDetails.value.copy(requestDetails = details)
    }

    fun submitReservation() {
        val details = _reservationDetails.value
        if (details.date.isBlank() || details.time.isBlank() || details.serviceCenter.isBlank()) {
            _reservationStatusMessage.value = "예약 날짜, 시간, 서비스 센터를 모두 선택(입력)해주세요."
            return
        }
        Log.d("MainViewModel", "서비스 예약 요청: $details")
        viewModelScope.launch {
            delay(1000) // 가상 네트워크 지연
            _reservationStatusMessage.value = "${details.serviceCenter}에 ${details.date} ${details.time} 예약 요청이 완료되었습니다. (가상)"
            _reservationDetails.value = ReservationDetails()
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
        checkMaintenance()
    }

    fun clearRegistration() {
        _registeredVehicleInfo.value = ""
        _qrScanResult.value = ""
        _sunroofUsage.value = SunroofUsageData()
        checkMaintenance()
    }


    override fun onCleared() {
        super.onCleared()
        sunroofControlJob?.cancel()
        acControlJob?.cancel()
        mqttManager.cleanup()
        Log.d("MainViewModel", "ViewModel 파괴, MqttManager 정리됨")
    }
}