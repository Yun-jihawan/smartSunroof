package com.example.bluelink.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bluelink.model.EnvironmentData
import com.example.bluelink.model.ReservationDetails
import com.example.bluelink.model.SunroofUsageData
import com.example.bluelink.model.VehicleState // VehicleState 모델에 sunroofMode, acMode 추가됨
import com.example.bluelink.mqtt.MqttConstants
import com.example.bluelink.mqtt.MqttConnectionState
import com.example.bluelink.mqtt.MqttManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

class MainViewModel : ViewModel() {

    private val _vehicleState = MutableStateFlow(VehicleState()) // 초기값에 mode 필드 포함
    val vehicleState: StateFlow<VehicleState> = _vehicleState.asStateFlow()

    // ... (다른 StateFlow 선언들은 이전과 동일)
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
    private val _currentVehicleId = MutableStateFlow<String?>(null)
    val currentVehicleId: StateFlow<String?> = _currentVehicleId.asStateFlow()
    private val _qrScanResult = MutableStateFlow("")
    val qrScanResult: StateFlow<String> = _qrScanResult.asStateFlow()
    private val mqttManager: MqttManager = MqttManager()
    private val _mqttConnectionState = MutableStateFlow(MqttConnectionState.IDLE)
    val mqttConnectionState: StateFlow<MqttConnectionState> = _mqttConnectionState.asStateFlow()
    private val _mqttErrorEventChannel = Channel<String>()
    val mqttErrorEvent = _mqttErrorEventChannel.receiveAsFlow()

    init {
        mqttManager.connectionState
            .onEach { state ->
                _mqttConnectionState.value = state
                Log.d("MainViewModel", "MQTT Connection State Changed: $state")
                if (state == MqttConnectionState.CONNECTED) {
                    _currentVehicleId.value?.let { vehicleId ->
                        subscribeToVehicleTopics(vehicleId)
                    }
                }
            }
            .launchIn(viewModelScope)

        mqttManager.connect()
        observeMqttMessages()
        checkMaintenance()
    }

    private fun subscribeToVehicleTopics(vehicleId: String) {
        if (vehicleId.isBlank()) return
        val topicsToSubscribe = listOf(
            MqttConstants.getStatusTopic(vehicleId),
            MqttConstants.getEnvironmentTopic(vehicleId)
        )
        Log.d("MainViewModel", "다음 차량 토픽 구독 시도 ($vehicleId): $topicsToSubscribe")
        mqttManager.subscribeToTopics(topicsToSubscribe)
    }

    private fun unsubscribeFromVehicleTopics(vehicleId: String) {
        if (vehicleId.isBlank()) return
        val topicsToUnsubscribe = listOf(
            MqttConstants.getStatusTopic(vehicleId),
            MqttConstants.getEnvironmentTopic(vehicleId)
        )
        Log.d("MainViewModel", "다음 차량 토픽 구독 해제 시도 ($vehicleId): $topicsToUnsubscribe")
        mqttManager.unsubscribeFromTopics(topicsToUnsubscribe)
    }

    private fun observeMqttMessages() {
        mqttManager.receivedMessages
            .onEach { (topic, message) ->
                Log.d("MainViewModel", "MQTT 메시지 수신: $topic -> $message")
                try {
                    _currentVehicleId.value?.let { vehicleId ->
                        when (topic) {
                            MqttConstants.getStatusTopic(vehicleId) -> {
                                val jsonObj = JSONObject(message)
                                val newSunroofStatus = jsonObj.optString("sunroof_status", _vehicleState.value.sunroofStatus) // 필드명 변경 가능성 고려
                                val newAcStatus = jsonObj.optString("ac_status", _vehicleState.value.acStatus) // 필드명 변경 가능성 고려
                                val newSunroofMode = jsonObj.optString("sunroof_mode", _vehicleState.value.sunroofMode) // 선루프 모드 수신
                                val newAcMode = jsonObj.optString("ac_mode", _vehicleState.value.acMode)             // AC 모드 수신

                                if (newSunroofStatus != _vehicleState.value.sunroofStatus && _isSunroofCommandInProgress.value) {
                                    sunroofControlJob?.cancel(); _isSunroofCommandInProgress.value = false
                                }
                                if (newAcStatus != _vehicleState.value.acStatus && _isAcCommandInProgress.value) {
                                    acControlJob?.cancel(); _isAcCommandInProgress.value = false
                                }
                                _vehicleState.value = VehicleState(
                                    sunroofStatus = newSunroofStatus,
                                    acStatus = newAcStatus,
                                    sunroofMode = newSunroofMode, // 수신된 모드로 업데이트
                                    acMode = newAcMode            // 수신된 모드로 업데이트
                                )
                                Log.d("MainViewModel", "VehicleState 업데이트: ${_vehicleState.value}")
                            }
                            MqttConstants.getEnvironmentTopic(vehicleId) -> {
                                // ... (환경 데이터 처리 로직은 이전과 동일)
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
                            else -> {
                                Log.w("MainViewModel", "알 수 없는 토픽 메시지 수신: $topic")
                            }
                        }
                    } ?: Log.w("MainViewModel", "등록된 차량 ID 없음. 메시지 무시: $topic")
                } catch (e: Exception) {
                    Log.e("MainViewModel", "MQTT 메시지 파싱 오류: $message", e)
                    _mqttErrorEventChannel.trySend("수신 데이터 처리 중 오류가 발생했습니다.")
                }
            }
            .launchIn(viewModelScope)
    }

    fun attemptMqttReconnect() {
        if (_mqttConnectionState.value != MqttConnectionState.CONNECTING && _mqttConnectionState.value != MqttConnectionState.CONNECTED) {
            Log.d("MainViewModel", "사용자 요청으로 MQTT 재연결 시도")
            mqttManager.connect()
        } else {
            Log.d("MainViewModel", "이미 연결 중이거나 연결된 상태입니다.")
        }
    }

    private fun parseVehicleIdFromQr(qrData: String): String? {
        return qrData.split(",").firstOrNull { it.startsWith("차량ID:") }?.substringAfter("차량ID:")?.trim()
    }

    fun registerVehicle(vehicleInfoFromQr: String) {
        // ... (이전 등록 로직과 대부분 동일, vehicleId 파싱 후 토픽 구독/해제) ...
        Log.d("MainViewModel", "차량 등록 시도 (원본 QR 데이터): $vehicleInfoFromQr")
        val parsedVehicleId = parseVehicleIdFromQr(vehicleInfoFromQr)

        if (parsedVehicleId == null || parsedVehicleId.isBlank()) {
            Log.e("MainViewModel", "차량 ID 파싱 실패: $vehicleInfoFromQr")
            viewModelScope.launch { _mqttErrorEventChannel.send("잘못된 QR 코드 형식입니다 (차량 ID 없음).") }
            _qrScanResult.value = ""
            return
        }

        _currentVehicleId.value?.let { oldVehicleId ->
            if (oldVehicleId != parsedVehicleId) {
                unsubscribeFromVehicleTopics(oldVehicleId)
            }
        }

        _registeredVehicleInfo.value = vehicleInfoFromQr
        _currentVehicleId.value = parsedVehicleId
        _qrScanResult.value = ""

        Log.d("MainViewModel", "차량 등록 완료: ID='${parsedVehicleId}', Info='${vehicleInfoFromQr}'")

        if (_mqttConnectionState.value == MqttConnectionState.CONNECTED) {
            subscribeToVehicleTopics(parsedVehicleId)
        } else {
            Log.w("MainViewModel", "MQTT 미연결 상태. 차량($parsedVehicleId) 토픽은 연결 후 자동 구독 예정.")
        }
        checkMaintenance()
        viewModelScope.launch { _mqttErrorEventChannel.send("${parsedVehicleId} 차량이 등록되었습니다.") }
    }

    fun clearRegistration() {
        // ... (이전 등록 해제 로직과 대부분 동일, vehicleId 기반 토픽 구독 해제) ...
        Log.d("MainViewModel", "등록된 차량 정보 초기화")
        _currentVehicleId.value?.let { vehicleId ->
            unsubscribeFromVehicleTopics(vehicleId)
        }

        _registeredVehicleInfo.value = ""
        _currentVehicleId.value = null
        _qrScanResult.value = ""
        _sunroofUsage.value = SunroofUsageData()
        _vehicleState.value = VehicleState() // 차량 상태 초기화 (모드 포함)
        _environmentData.value = EnvironmentData()
        checkMaintenance()
        viewModelScope.launch { _mqttErrorEventChannel.send("차량 등록이 해제되었습니다.") }
    }


    // --- 수동 제어 함수 (기존 로직 유지, 자동 모드일 때 UI에서 비활성화 가정) ---
    fun controlSunroof(action: String) { // action: "open", "close"
        val vehicleId = _currentVehicleId.value ?: return Unit.also { viewModelScope.launch { _mqttErrorEventChannel.send("등록된 차량 없음") } }
        if (_mqttConnectionState.value != MqttConnectionState.CONNECTED) {
            viewModelScope.launch { _mqttErrorEventChannel.send("MQTT 미연결") }
            return
        }
        if (_vehicleState.value.sunroofMode == "auto") { // 자동 모드일 경우 수동 제어 제한
            viewModelScope.launch { _mqttErrorEventChannel.send("선루프 자동 모드 중에는 수동 제어가 비활성화됩니다.") }
            return
        }
        if (_isSunroofCommandInProgress.value || _isAcCommandInProgress.value) {
            viewModelScope.launch { _mqttErrorEventChannel.send("다른 명령 진행 중") }
            return
        }

        val topic = MqttConstants.getControlSunroofTopic(vehicleId)
        val commandMessage = JSONObject().put("command", action).toString() // 예: {"command":"open"}
        mqttManager.publish(topic, commandMessage)
        _isSunroofCommandInProgress.value = true
        Log.d("MainViewModel", "선루프 수동 제어 명령 발행 ($vehicleId): $action, 토픽: $topic")

        sunroofControlJob?.cancel()
        sunroofControlJob = viewModelScope.launch {
            delay(10000)
            if (_isSunroofCommandInProgress.value) {
                _isSunroofCommandInProgress.value = false
                Log.w("MainViewModel", "선루프 제어 응답 시간 초과 ($vehicleId)")
                _mqttErrorEventChannel.trySend("선루프 제어 응답이 없습니다.")
            }
        }
    }

    fun controlAC(action: String) { // action: "on", "off"
        val vehicleId = _currentVehicleId.value ?: return Unit.also { viewModelScope.launch { _mqttErrorEventChannel.send("등록된 차량 없음") } }
        if (_mqttConnectionState.value != MqttConnectionState.CONNECTED) {
            viewModelScope.launch { _mqttErrorEventChannel.send("MQTT 미연결") }
            return
        }
        if (_vehicleState.value.acMode == "auto") { // 자동 모드일 경우 수동 제어 제한
            viewModelScope.launch { _mqttErrorEventChannel.send("에어컨 자동 모드 중에는 수동 제어가 비활성화됩니다.") }
            return
        }
        if (_isSunroofCommandInProgress.value || _isAcCommandInProgress.value) {
            viewModelScope.launch { _mqttErrorEventChannel.send("다른 명령 진행 중") }
            return
        }

        val topic = MqttConstants.getControlAcTopic(vehicleId)
        // 예: {"command":"on", "temperature": 22} 등 확장 가능성
        val commandMessage = JSONObject().put("command", action).toString()
        mqttManager.publish(topic, commandMessage)
        _isAcCommandInProgress.value = true
        Log.d("MainViewModel", "AC 수동 제어 명령 발행 ($vehicleId): $action, 토픽: $topic")

        acControlJob?.cancel()
        acControlJob = viewModelScope.launch {
            delay(10000)
            if (_isAcCommandInProgress.value) {
                _isAcCommandInProgress.value = false
                Log.w("MainViewModel", "AC 제어 응답 시간 초과 ($vehicleId)")
                _mqttErrorEventChannel.trySend("AC 제어 응답이 없습니다.")
            }
        }
    }

    // --- 자동/수동 모드 변경 함수 ---
    fun setSunroofMode(mode: String) { // mode: "auto" 또는 "manual"
        val vehicleId = _currentVehicleId.value ?: return Unit.also { viewModelScope.launch { _mqttErrorEventChannel.send("등록된 차량 없음") } }
        if (_mqttConnectionState.value != MqttConnectionState.CONNECTED) {
            viewModelScope.launch { _mqttErrorEventChannel.send("MQTT 미연결") }
            return
        }

        val topic = MqttConstants.getSetSunroofModeTopic(vehicleId)
        val message = JSONObject().put("mode", mode).toString()
        mqttManager.publish(topic, message)
        Log.d("MainViewModel", "선루프 모드 변경 명령 발행 ($vehicleId): $mode, 토픽: $topic")
        // 차량으로부터 실제 모드 변경 상태를 MQTT로 피드백 받아 _vehicleState.value.sunroofMode를 업데이트해야 함
        // 즉각적인 UI 피드백을 위해 임시로 변경 후, MQTT 응답으로 최종 확정하는 방식도 고려 가능
        // _vehicleState.value = _vehicleState.value.copy(sunroofMode = mode) // 임시 UI 업데이트
        viewModelScope.launch { _mqttErrorEventChannel.send("선루프 모드를 $mode 로 변경 요청했습니다.") }
    }

    fun setAcMode(mode: String) { // mode: "auto" 또는 "manual"
        val vehicleId = _currentVehicleId.value ?: return Unit.also { viewModelScope.launch { _mqttErrorEventChannel.send("등록된 차량 없음") } }
        if (_mqttConnectionState.value != MqttConnectionState.CONNECTED) {
            viewModelScope.launch { _mqttErrorEventChannel.send("MQTT 미연결") }
            return
        }

        val topic = MqttConstants.getSetAcModeTopic(vehicleId)
        val message = JSONObject().put("mode", mode).toString()
        mqttManager.publish(topic, message)
        Log.d("MainViewModel", "AC 모드 변경 명령 발행 ($vehicleId): $mode, 토픽: $topic")
        // _vehicleState.value = _vehicleState.value.copy(acMode = mode) // 임시 UI 업데이트
        viewModelScope.launch { _mqttErrorEventChannel.send("에어컨 모드를 $mode 로 변경 요청했습니다.") }
    }


    // ... (checkMaintenance, 예약 관련 함수 등은 이전과 동일하게 유지)
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
            val modelInfo = if (_currentVehicleId.value != null) "차량 [${_currentVehicleId.value}]의 " else ""
            _maintenanceNotification.value = "$modelInfo${usage.sunroofModel} 모델: $notification"
        } else {
            if (Math.random() < 0.2 && _currentVehicleId.value != null) {
                _maintenanceNotification.value = "차량 [${_currentVehicleId.value}] 선루프 정기 점검 시기가 되었습니다."
            } else {
                _maintenanceNotification.value = ""
            }
        }
    }
    fun toggleReservationForm(show: Boolean) { _showReservationForm.value = show; if (!show) { _reservationStatusMessage.value = "" } }
    fun updateReservationDate(date: String) { _reservationDetails.value = _reservationDetails.value.copy(date = date) }
    fun updateReservationTime(time: String) { _reservationDetails.value = _reservationDetails.value.copy(time = time) }
    fun updateSelectedServiceCenter(center: String) { _reservationDetails.value = _reservationDetails.value.copy(serviceCenter = center) }
    fun updateServiceRequestDetails(details: String) { _reservationDetails.value = _reservationDetails.value.copy(requestDetails = details) }
    fun submitReservation() {
        val details = _reservationDetails.value
        if (details.date.isBlank() || details.time.isBlank() || details.serviceCenter.isBlank()) {
            _reservationStatusMessage.value = "예약 날짜, 시간, 서비스 센터를 모두 선택(입력)해주세요."
            return
        }
        Log.d("MainViewModel", "서비스 예약 요청 (${_currentVehicleId.value}): $details")
        viewModelScope.launch { delay(1000); _reservationStatusMessage.value = "${_currentVehicleId.value?.let { "[$it] " } ?: ""}예약 요청 완료 (가상)"; _reservationDetails.value = ReservationDetails() }
    }
    fun processQrScanResult(contents: String?) { if (contents != null) { _qrScanResult.value = contents } else { _qrScanResult.value = "" } }


    override fun onCleared() {
        super.onCleared()
        sunroofControlJob?.cancel()
        acControlJob?.cancel()
        mqttManager.cleanup()
        Log.d("MainViewModel", "ViewModel 파괴, MqttManager 정리됨")
    }
}