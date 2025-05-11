package com.example.bluelink.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bluelink.model.EnvironmentData
import com.example.bluelink.model.ReservationDetails
import com.example.bluelink.model.SunroofUsageData
import com.example.bluelink.model.VehicleState
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

    // ... (기존 StateFlow 선언들 대부분 유지) ...
    private val _vehicleState = MutableStateFlow(VehicleState())
    val vehicleState: StateFlow<VehicleState> = _vehicleState.asStateFlow()

    // --- 제어 명령 진행 상태 ---
    private val _isSunroofCommandInProgress = MutableStateFlow(false) // 수동 제어 (열기/닫기)
    val isSunroofCommandInProgress: StateFlow<Boolean> = _isSunroofCommandInProgress.asStateFlow()
    private val _isAcCommandInProgress = MutableStateFlow(false) // 수동 제어 (켜기/끄기)
    val isAcCommandInProgress: StateFlow<Boolean> = _isAcCommandInProgress.asStateFlow()

    // 모드 변경 진행 상태 추가
    private val _isSunroofModeChangeInProgress = MutableStateFlow(false)
    val isSunroofModeChangeInProgress: StateFlow<Boolean> = _isSunroofModeChangeInProgress.asStateFlow()
    private val _isAcModeChangeInProgress = MutableStateFlow(false)
    val isAcModeChangeInProgress: StateFlow<Boolean> = _isAcModeChangeInProgress.asStateFlow()

    private var sunroofControlJob: Job? = null
    private var acControlJob: Job? = null
    private var sunroofModeChangeJob: Job? = null // 모드 변경 타임아웃 Job
    private var acModeChangeJob: Job? = null     // 모드 변경 타임아웃 Job


    // ... (나머지 StateFlow 선언 및 init, MQTT 관련 함수들은 이전 답변의 최종본을 기반으로 함) ...
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
            MqttConstants.getEnvironmentTopic(vehicleId),
            MqttConstants.getControlSunroofResultTopic(vehicleId),
            MqttConstants.getControlAcResultTopic(vehicleId),
            MqttConstants.getSetSunroofModeResultTopic(vehicleId),
            MqttConstants.getSetAcModeResultTopic(vehicleId)
        )
        Log.d("MainViewModel", "다음 차량 토픽 구독 시도 ($vehicleId): $topicsToSubscribe")
        mqttManager.subscribeToTopics(topicsToSubscribe)
    }

    private fun unsubscribeFromVehicleTopics(vehicleId: String) {
        if (vehicleId.isBlank()) return
        val topicsToUnsubscribe = listOf(
            MqttConstants.getStatusTopic(vehicleId),
            MqttConstants.getEnvironmentTopic(vehicleId),
            MqttConstants.getControlSunroofResultTopic(vehicleId),
            MqttConstants.getControlAcResultTopic(vehicleId),
            MqttConstants.getSetSunroofModeResultTopic(vehicleId),
            MqttConstants.getSetAcModeResultTopic(vehicleId)
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
                        when {
                            topic == MqttConstants.getStatusTopic(vehicleId) -> handleVehicleStatusMessage(message)
                            topic == MqttConstants.getEnvironmentTopic(vehicleId) -> handleEnvironmentDataMessage(message)
                            topic == MqttConstants.getControlSunroofResultTopic(vehicleId) -> handleControlResultMessage(message, "선루프", _isSunroofCommandInProgress, sunroofControlJob)
                            topic == MqttConstants.getControlAcResultTopic(vehicleId) -> handleControlResultMessage(message, "에어컨", _isAcCommandInProgress, acControlJob)
                            // 모드 변경 결과에 대한 InProgress 상태 연결
                            topic == MqttConstants.getSetSunroofModeResultTopic(vehicleId) -> handleControlResultMessage(message, "선루프 모드", _isSunroofModeChangeInProgress, sunroofModeChangeJob)
                            topic == MqttConstants.getSetAcModeResultTopic(vehicleId) -> handleControlResultMessage(message, "에어컨 모드", _isAcModeChangeInProgress, acModeChangeJob)
                            else -> Log.w("MainViewModel", "처리되지 않은 토픽 메시지 수신: $topic")
                        }
                    } ?: Log.w("MainViewModel", "등록된 차량 ID 없음. 메시지 무시: $topic")
                } catch (e: Exception) {
                    Log.e("MainViewModel", "MQTT 메시지 처리 중 오류: $message", e)
                    _mqttErrorEventChannel.trySend("수신된 데이터 처리 중 오류가 발생했습니다.")
                }
            }
            .launchIn(viewModelScope)
    }

    private fun handleVehicleStatusMessage(message: String) { /* 이전과 동일 */
        val jsonObj = JSONObject(message)
        val newSunroofStatus = jsonObj.optString("sunroof_status", _vehicleState.value.sunroofStatus)
        val newAcStatus = jsonObj.optString("ac_status", _vehicleState.value.acStatus)
        val newSunroofMode = jsonObj.optString("sunroof_mode", _vehicleState.value.sunroofMode)
        val newAcMode = jsonObj.optString("ac_mode", _vehicleState.value.acMode)

        _vehicleState.value = VehicleState(
            sunroofStatus = newSunroofStatus,
            acStatus = newAcStatus,
            sunroofMode = newSunroofMode,
            acMode = newAcMode
        )
        Log.d("MainViewModel", "VehicleState 업데이트 (MQTT): ${_vehicleState.value}")
    }
    private fun handleEnvironmentDataMessage(message: String) { /* 이전과 동일 */
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

    private fun handleControlResultMessage(
        message: String,
        controlType: String,
        inProgressState: MutableStateFlow<Boolean>?, // Nullable로 변경
        controlJob: Job?
    ) {
        val jsonObj = JSONObject(message)
        val result = jsonObj.optString("result", "unknown")
        val resultMessage = jsonObj.optString("message", "$controlType 설정 결과 수신")

        inProgressState?.value = false // 로딩 상태가 있다면 해제
        controlJob?.cancel()

        val feedbackMessage = if (result == "success") {
            "$controlType 설정 성공: $resultMessage"
        } else {
            "$controlType 설정 실패: $resultMessage (사유: ${jsonObj.optString("reason_code", "알 수 없음")})"
        }

        Log.i("MainViewModel", feedbackMessage)
        viewModelScope.launch { _mqttErrorEventChannel.send(feedbackMessage) }

        // 중요: 제어 성공/실패 시에도 차량의 최신 상태는 status 토픽을 통해 업데이트 받아야 합니다.
        // 여기서 _vehicleState를 직접 수정하는 것은 status 토픽과의 동기화 문제를 일으킬 수 있습니다.
        // 예를 들어, 모드 변경 성공 시, 차량이 새로운 모드를 반영한 status 메시지를 보내주면 UI가 자동으로 업데이트됩니다.
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
        Log.d("MainViewModel", "차량 등록 시도 (원본 QR 데이터): $vehicleInfoFromQr")
        val parsedVehicleId = parseVehicleIdFromQr(vehicleInfoFromQr)

        if (parsedVehicleId == null || parsedVehicleId.isBlank()) {
            Log.e("MainViewModel", "차량 ID 파싱 실패: $vehicleInfoFromQr")
            viewModelScope.launch { _mqttErrorEventChannel.send("잘못된 QR 코드 형식입니다 (차량 ID 없음).") }
            _qrScanResult.value = ""
            return
        }

        _currentVehicleId.value?.let { oldVehicleId ->
            if (oldVehicleId != parsedVehicleId) { unsubscribeFromVehicleTopics(oldVehicleId) }
        }
        _registeredVehicleInfo.value = vehicleInfoFromQr
        _currentVehicleId.value = parsedVehicleId
        _qrScanResult.value = ""
        Log.d("MainViewModel", "차량 등록 완료: ID='${parsedVehicleId}'")
        if (_mqttConnectionState.value == MqttConnectionState.CONNECTED) {
            subscribeToVehicleTopics(parsedVehicleId)
        }
        checkMaintenance()
        viewModelScope.launch { _mqttErrorEventChannel.send("차량 [${parsedVehicleId}]이(가) 성공적으로 등록되었습니다.") }
    }

    fun clearRegistration() {
        Log.d("MainViewModel", "등록된 차량 정보 초기화")
        _currentVehicleId.value?.let { vehicleId -> unsubscribeFromVehicleTopics(vehicleId) }
        _registeredVehicleInfo.value = ""
        _currentVehicleId.value = null
        _qrScanResult.value = ""
        _sunroofUsage.value = SunroofUsageData()
        _vehicleState.value = VehicleState()
        _environmentData.value = EnvironmentData()
        checkMaintenance()
        viewModelScope.launch { _mqttErrorEventChannel.send("차량 등록이 해제되었습니다.") }
    }

    fun controlSunroof(action: String) {
        val vehicleId = _currentVehicleId.value ?: return Unit.also { viewModelScope.launch { _mqttErrorEventChannel.send("차량을 먼저 등록해주세요.") } }
        if (_mqttConnectionState.value != MqttConnectionState.CONNECTED) { viewModelScope.launch { _mqttErrorEventChannel.send("서버에 연결되지 않았습니다.") }; return }
        if (_vehicleState.value.sunroofMode == "auto") { viewModelScope.launch { _mqttErrorEventChannel.send("선루프 자동 모드 중입니다.") }; return }
        if (_isSunroofCommandInProgress.value || _isAcCommandInProgress.value || _isSunroofModeChangeInProgress.value || _isAcModeChangeInProgress.value) { // 모든 진행 상태 확인
            viewModelScope.launch { _mqttErrorEventChannel.send("다른 명령이 진행 중입니다.") }; return
        }

        val topic = MqttConstants.getControlSunroofTopic(vehicleId)
        val commandMessage = JSONObject().put("command", action).toString()
        mqttManager.publish(topic, commandMessage)
        _isSunroofCommandInProgress.value = true
        Log.d("MainViewModel", "선루프 수동 제어 명령 발행 ($vehicleId): $action")

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

    fun controlAC(action: String) {
        val vehicleId = _currentVehicleId.value ?: return Unit.also { viewModelScope.launch { _mqttErrorEventChannel.send("차량을 먼저 등록해주세요.") } }
        if (_mqttConnectionState.value != MqttConnectionState.CONNECTED) { viewModelScope.launch { _mqttErrorEventChannel.send("서버에 연결되지 않았습니다.") }; return }
        if (_vehicleState.value.acMode == "auto") { viewModelScope.launch { _mqttErrorEventChannel.send("에어컨 자동 모드 중입니다.") }; return }
        if (_isSunroofCommandInProgress.value || _isAcCommandInProgress.value || _isSunroofModeChangeInProgress.value || _isAcModeChangeInProgress.value) {
            viewModelScope.launch { _mqttErrorEventChannel.send("다른 명령이 진행 중입니다.") }; return
        }

        val topic = MqttConstants.getControlAcTopic(vehicleId)
        val commandMessage = JSONObject().put("command", action).toString()
        mqttManager.publish(topic, commandMessage)
        _isAcCommandInProgress.value = true
        Log.d("MainViewModel", "AC 수동 제어 명령 발행 ($vehicleId): $action")

        acControlJob?.cancel()
        acControlJob = viewModelScope.launch {
            delay(10000)
            if (_isAcCommandInProgress.value) {
                _isAcCommandInProgress.value = false
                Log.w("MainViewModel", "AC 제어 응답 시간 초과 ($vehicleId)")
                _mqttErrorEventChannel.trySend("에어컨 제어 응답이 없습니다.")
            }
        }
    }

    fun setSunroofMode(mode: String) { // mode: "auto" 또는 "manual"
        val vehicleId = _currentVehicleId.value ?: return Unit.also { viewModelScope.launch { _mqttErrorEventChannel.send("차량을 먼저 등록해주세요.") } }
        if (_mqttConnectionState.value != MqttConnectionState.CONNECTED) { viewModelScope.launch { _mqttErrorEventChannel.send("서버에 연결되지 않았습니다.") }; return }
        if (_isSunroofCommandInProgress.value || _isAcCommandInProgress.value || _isSunroofModeChangeInProgress.value || _isAcModeChangeInProgress.value) {
            viewModelScope.launch { _mqttErrorEventChannel.send("다른 명령이 진행 중입니다.") }; return
        }

        val topic = MqttConstants.getSetSunroofModeTopic(vehicleId)
        val message = JSONObject().put("mode", mode).toString()
        mqttManager.publish(topic, message)
        _isSunroofModeChangeInProgress.value = true // 모드 변경 로딩 시작
        Log.d("MainViewModel", "선루프 모드 변경 명령 발행 ($vehicleId): $mode")

        sunroofModeChangeJob?.cancel()
        sunroofModeChangeJob = viewModelScope.launch {
            delay(10000) // 10초 타임아웃
            if (_isSunroofModeChangeInProgress.value) {
                _isSunroofModeChangeInProgress.value = false
                Log.w("MainViewModel", "선루프 모드 변경 응답 시간 초과 ($vehicleId)")
                _mqttErrorEventChannel.trySend("선루프 모드 변경 응답이 없습니다.")
            }
        }
    }

    fun setAcMode(mode: String) { // mode: "auto" 또는 "manual"
        val vehicleId = _currentVehicleId.value ?: return Unit.also { viewModelScope.launch { _mqttErrorEventChannel.send("차량을 먼저 등록해주세요.") } }
        if (_mqttConnectionState.value != MqttConnectionState.CONNECTED) { viewModelScope.launch { _mqttErrorEventChannel.send("서버에 연결되지 않았습니다.") }; return }
        if (_isSunroofCommandInProgress.value || _isAcCommandInProgress.value || _isSunroofModeChangeInProgress.value || _isAcModeChangeInProgress.value) {
            viewModelScope.launch { _mqttErrorEventChannel.send("다른 명령이 진행 중입니다.") }; return
        }

        val topic = MqttConstants.getSetAcModeTopic(vehicleId)
        val message = JSONObject().put("mode", mode).toString()
        mqttManager.publish(topic, message)
        _isAcModeChangeInProgress.value = true // 모드 변경 로딩 시작
        Log.d("MainViewModel", "AC 모드 변경 명령 발행 ($vehicleId): $mode")

        acModeChangeJob?.cancel()
        acModeChangeJob = viewModelScope.launch {
            delay(10000) // 10초 타임아웃
            if (_isAcModeChangeInProgress.value) {
                _isAcModeChangeInProgress.value = false
                Log.w("MainViewModel", "AC 모드 변경 응답 시간 초과 ($vehicleId)")
                _mqttErrorEventChannel.trySend("AC 모드 변경 응답이 없습니다.")
            }
        }
    }

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
        sunroofModeChangeJob?.cancel() // 추가된 Job 취소
        acModeChangeJob?.cancel()     // 추가된 Job 취소
        mqttManager.cleanup()
        Log.d("MainViewModel", "ViewModel 파괴, MqttManager 정리됨")
    }
}