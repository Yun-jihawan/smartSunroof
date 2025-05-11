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
import org.json.JSONException
import org.json.JSONObject
import java.util.UUID // command_id 생성을 위해

class MainViewModel : ViewModel() {

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

    private val _isOverallModeChangeInProgress = MutableStateFlow(false)
    val isOverallModeChangeInProgress: StateFlow<Boolean> = _isOverallModeChangeInProgress.asStateFlow()

    private var sunroofControlJob: Job? = null
    private var acControlJob: Job? = null
    private var overallModeChangeJob: Job? = null

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
            MqttConstants.getSunroofControlResultTopic(vehicleId), // 수정된 이름 사용
            MqttConstants.getAcControlResultTopic(vehicleId),       // 수정된 이름 사용
            MqttConstants.getOverallModeSetResultTopic(vehicleId)   // 수정된 이름 사용
        )
        Log.d("MainViewModel", "다음 차량 토픽 구독 시도 ($vehicleId): $topicsToSubscribe")
        mqttManager.subscribeToTopics(topicsToSubscribe)
    }

    private fun unsubscribeFromVehicleTopics(vehicleId: String) {
        if (vehicleId.isBlank()) return
        val topicsToUnsubscribe = listOf(
            MqttConstants.getStatusTopic(vehicleId),
            MqttConstants.getEnvironmentTopic(vehicleId),
            MqttConstants.getSunroofControlResultTopic(vehicleId), // 수정된 이름 사용
            MqttConstants.getAcControlResultTopic(vehicleId),       // 수정된 이름 사용
            MqttConstants.getOverallModeSetResultTopic(vehicleId)   // 수정된 이름 사용
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
                            topic == MqttConstants.getSunroofControlResultTopic(vehicleId) -> handleControlResultMessage(message, "선루프 수동 제어", _isSunroofCommandInProgress, sunroofControlJob, MqttConstants.KEY_COMMAND_TYPE_SUNROOF_CONTROL)
                            topic == MqttConstants.getAcControlResultTopic(vehicleId) -> handleControlResultMessage(message, "에어컨 수동 제어", _isAcCommandInProgress, acControlJob, MqttConstants.KEY_COMMAND_TYPE_AC_CONTROL)
                            topic == MqttConstants.getOverallModeSetResultTopic(vehicleId) -> handleControlResultMessage(message, "차량 자동 제어 모드", _isOverallModeChangeInProgress, overallModeChangeJob, MqttConstants.KEY_COMMAND_TYPE_OVERALL_MODE_SET)
                            else -> Log.w("MainViewModel", "처리되지 않은 토픽 메시지 수신: $topic")
                        }
                    } ?: Log.w("MainViewModel", "등록된 차량 ID 없음. 메시지 무시: $topic")
                } catch (e: Exception) {
                    Log.e("MainViewModel", "MQTT 메시지 처리 중 오류: $message", e)
                    _mqttErrorEventChannel.trySend("수신된 데이터 형식에 오류가 있습니다.") // 메시지 구체화
                }
            }
            .launchIn(viewModelScope)
    }

    private fun handleVehicleStatusMessage(message: String) {
        try {
            val jsonObj = JSONObject(message)
            _vehicleState.value = VehicleState(
                sunroofStatus = jsonObj.optString(MqttConstants.KEY_SUNROOF_STATUS, _vehicleState.value.sunroofStatus),
                acStatus = jsonObj.optString(MqttConstants.KEY_AC_STATUS, _vehicleState.value.acStatus),
                overallControlMode = jsonObj.optString(MqttConstants.KEY_OVERALL_CONTROL_MODE, _vehicleState.value.overallControlMode),
                isDriving = jsonObj.optBoolean(MqttConstants.KEY_IS_DRIVING, _vehicleState.value.isDriving),
                currentSpeed = jsonObj.optInt(MqttConstants.KEY_CURRENT_SPEED_KMH, _vehicleState.value.currentSpeed)
            )
            Log.d("MainViewModel", "VehicleState 업데이트 (MQTT): ${_vehicleState.value}")
        } catch (e: JSONException) {
            Log.e("MainViewModel", "차량 상태 메시지 파싱 오류: $message", e)
            _mqttErrorEventChannel.trySend("차량 상태 정보 수신 중 오류가 발생했습니다.")
        }
    }

    private fun handleEnvironmentDataMessage(message: String) {
        try {
            val jsonObj = JSONObject(message)
            _environmentData.value = EnvironmentData(
                indoorTemperature = jsonObj.optDouble(MqttConstants.KEY_INDOOR_TEMP_C, _environmentData.value.indoorTemperature),
                indoorHumidity = jsonObj.optDouble(MqttConstants.KEY_INDOOR_HUMIDITY_PERCENT, _environmentData.value.indoorHumidity),
                outdoorTemperature = jsonObj.optDouble(MqttConstants.KEY_OUTDOOR_TEMP_C, _environmentData.value.outdoorTemperature),
                outdoorHumidity = jsonObj.optDouble(MqttConstants.KEY_OUTDOOR_HUMIDITY_PERCENT, _environmentData.value.outdoorHumidity),
                indoorAirQualityText = jsonObj.optString(MqttConstants.KEY_INDOOR_AIR_QUALITY_TEXT, _environmentData.value.indoorAirQualityText),
                indoorAqi = if (jsonObj.has(MqttConstants.KEY_INDOOR_AQI)) jsonObj.optInt(MqttConstants.KEY_INDOOR_AQI) else null,
                outdoorAirQualityText = jsonObj.optString(MqttConstants.KEY_OUTDOOR_AIR_QUALITY_TEXT, _environmentData.value.outdoorAirQualityText),
                outdoorAqi = if (jsonObj.has(MqttConstants.KEY_OUTDOOR_AQI)) jsonObj.optInt(MqttConstants.KEY_OUTDOOR_AQI) else null,
                fineDustPm25 = if (jsonObj.has(MqttConstants.KEY_FINE_DUST_PM25_UG_M3)) jsonObj.optDouble(MqttConstants.KEY_FINE_DUST_PM25_UG_M3) else null,
                fineDustStatusText = jsonObj.optString(MqttConstants.KEY_FINE_DUST_STATUS_TEXT, _environmentData.value.fineDustStatusText)
            )
            Log.d("MainViewModel", "EnvironmentData 업데이트 (MQTT): ${_environmentData.value}")
        } catch (e: JSONException) {
            Log.e("MainViewModel", "환경 데이터 메시지 파싱 오류: $message", e)
            _mqttErrorEventChannel.trySend("환경 정보 수신 중 오류가 발생했습니다.")
        }
    }

    private fun handleControlResultMessage(
        message: String,
        controlDescription: String, // 예: "선루프 열기", "자동 모드 설정"
        inProgressState: MutableStateFlow<Boolean>?,
        controlJob: Job?,
        expectedCommandType: String // 결과 메시지 내 command_type과 비교하기 위함
    ) {
        try {
            val jsonObj = JSONObject(message)
            val commandType = jsonObj.optString(MqttConstants.KEY_COMMAND_TYPE)
            val result = jsonObj.optString(MqttConstants.KEY_RESULT)
            val resultMessage = jsonObj.optString(MqttConstants.KEY_MESSAGE, "$controlDescription 결과") // 기본 메시지
            val errorCode = jsonObj.optString(MqttConstants.KEY_ERROR_CODE, "")

            // 해당 명령에 대한 결과인지 command_type으로 간단히 확인 (더 정확하게는 command_id_ref 사용)
            if (commandType != expectedCommandType && !expectedCommandType.contains(controlDescription, ignoreCase = true) /*유연한 비교*/) {
                Log.w("MainViewModel", "수신된 제어 결과($commandType)가 예상($expectedCommandType)과 다릅니다. 메시지: $message")
                // return // 다른 제어 결과일 수 있으므로 무시하거나, 혹은 포괄적인 controlType 문자열로 비교
            }

            inProgressState?.value = false
            controlJob?.cancel()

            val feedbackMessage = if (result == "success") {
                "$controlDescription 성공: $resultMessage"
            } else {
                "$controlDescription 실패: $resultMessage ${if (errorCode.isNotEmpty()) "(오류: $errorCode)" else ""}"
            }

            Log.i("MainViewModel", feedbackMessage)
            viewModelScope.launch { _mqttErrorEventChannel.send(feedbackMessage) }

            // 성공 시, 차량의 최신 상태는 status 토픽을 통해 업데이트될 것이므로,
            // 여기서 _vehicleState를 직접 수정하는 것은 일반적으로 권장되지 않음 (데이터 일관성).
            // 단, overall_control_mode와 같이 UI 즉각 반응이 중요한 경우 status 메시지 수신 전 여기서 임시 업데이트 고려 가능.
            // 현재는 status 메시지를 통해 모든 상태가 업데이트되도록 함.

        } catch (e: JSONException) {
            Log.e("MainViewModel", "$controlDescription 결과 메시지 파싱 오류: $message", e)
            inProgressState?.value = false // 파싱 오류 시에도 로딩은 해제
            controlJob?.cancel()
            _mqttErrorEventChannel.trySend("$controlDescription 결과 처리 중 오류가 발생했습니다.")
        }
    }

    // --- 제어 명령 발행 함수들 ---
    private fun publishControlCommand(
        vehicleId: String,
        topicSuffix: String,
        commandPayload: JSONObject,
        inProgressState: MutableStateFlow<Boolean>,
        controlJob: Job?,
        timeoutMessage: String,
        logMessagePrefix: String,
        jobSetter: (Job) -> Unit // Job을 업데이트하기 위한 콜백
    ) {
        if (_mqttConnectionState.value != MqttConnectionState.CONNECTED) {
            viewModelScope.launch { _mqttErrorEventChannel.send("서버에 연결되지 않았습니다.") }
            return
        }
        if (_isSunroofCommandInProgress.value || _isAcCommandInProgress.value || _isOverallModeChangeInProgress.value) {
            // 단, 자기 자신의 InProgress 상태는 허용해야 함 (예: 모드 변경 중 다시 모드 변경 시도)
            // 좀 더 세밀하게는, 현재 진행 중인 명령과 다른 종류의 명령일 때만 막도록 수정 가능.
            // 여기서는 어떤 명령이든 하나라도 진행 중이면 다른 명령 막도록 단순화.
            // if (inProgressState.value) { /* 자기 자신은 이미 진행 중 */ } else { /* 다른 명령 진행 중 */ }
            viewModelScope.launch { _mqttErrorEventChannel.send("다른 제어 작업이 진행 중입니다.") }
            return
        }

        val topic = "$MqttConstants.VEHICLE_TOPIC_BASE$topicSuffix".replace(MqttConstants.VEHICLE_ID_PLACEHOLDER, vehicleId)
        // 모든 명령에 command_id 추가 (응답 매칭용)
        val commandId = UUID.randomUUID().toString()
        commandPayload.put(MqttConstants.KEY_COMMAND_ID, commandId)

        mqttManager.publish(topic, commandPayload.toString())
        inProgressState.value = true
        Log.d("MainViewModel", "$logMessagePrefix 명령 발행 ($vehicleId): ${commandPayload.toString(2)}, Topic: $topic, CommandID: $commandId")

        controlJob?.cancel() // 이전 타임아웃 Job이 있다면 취소
        val newJob = viewModelScope.launch {
            delay(15000) // 타임아웃 시간 15초로 늘림 (SSL 고려 및 결과 응답 시간)
            if (inProgressState.value) { // 타임아웃 시점에도 여전히 진행 중이라면
                inProgressState.value = false
                Log.w("MainViewModel", "$logMessagePrefix 응답 시간 초과 ($vehicleId)")
                _mqttErrorEventChannel.trySend(timeoutMessage)
            }
        }
        jobSetter(newJob) // 생성된 Job을 ViewModel의 해당 Job 변수에 할당
    }


    fun controlSunroof(action: String) {
        val vehicleId = _currentVehicleId.value ?: return Unit.also { viewModelScope.launch { _mqttErrorEventChannel.send("차량을 먼저 등록해주세요.") } }
        if (_vehicleState.value.overallControlMode == "auto") {
            viewModelScope.launch { _mqttErrorEventChannel.send("차량이 자동 제어 모드 중입니다. 수동 제어를 사용하려면 자동 모드를 해제해주세요.") }
            return
        }
        val payload = JSONObject().apply {
            put(MqttConstants.KEY_ACTION, action)
            put(MqttConstants.KEY_COMMAND_TYPE, MqttConstants.KEY_COMMAND_TYPE_SUNROOF_CONTROL) // 명령 타입 명시
        }
        publishControlCommand(
            vehicleId,
            MqttConstants.TOPIC_SUFFIX_CONTROL_BASE + MqttConstants.TOPIC_SUFFIX_SUNROOF + MqttConstants.TOPIC_SUFFIX_COMMAND,
            payload,
            _isSunroofCommandInProgress,
            sunroofControlJob,
            "선루프 제어 응답이 없습니다. 차량 상태를 확인해주세요.",
            "선루프 수동 제어"
        ) { sunroofControlJob = it }
    }

    fun controlAC(action: String) {
        val vehicleId = _currentVehicleId.value ?: return Unit.also { viewModelScope.launch { _mqttErrorEventChannel.send("차량을 먼저 등록해주세요.") } }
        if (_vehicleState.value.overallControlMode == "auto") {
            viewModelScope.launch { _mqttErrorEventChannel.send("차량이 자동 제어 모드 중입니다. 수동 제어를 사용하려면 자동 모드를 해제해주세요.") }
            return
        }
        val payload = JSONObject().apply {
            put(MqttConstants.KEY_ACTION, action)
            put(MqttConstants.KEY_COMMAND_TYPE, MqttConstants.KEY_COMMAND_TYPE_AC_CONTROL)
            // 필요시 추가 파라미터: put(MqttConstants.KEY_TEMPERATURE_C, 22)
        }
        publishControlCommand(
            vehicleId,
            MqttConstants.TOPIC_SUFFIX_CONTROL_BASE + MqttConstants.TOPIC_SUFFIX_AC + MqttConstants.TOPIC_SUFFIX_COMMAND,
            payload,
            _isAcCommandInProgress,
            acControlJob,
            "에어컨 제어 응답이 없습니다. 차량 상태를 확인해주세요.",
            "에어컨 수동 제어"
        ) { acControlJob = it }
    }

    fun setOverallControlMode(isAuto: Boolean) {
        val vehicleId = _currentVehicleId.value ?: return Unit.also { viewModelScope.launch { _mqttErrorEventChannel.send("차량을 먼저 등록해주세요.") } }
        val newMode = if (isAuto) "auto" else "manual"
        val payload = JSONObject().apply {
            put(MqttConstants.KEY_MODE, newMode)
            put(MqttConstants.KEY_COMMAND_TYPE, MqttConstants.KEY_COMMAND_TYPE_OVERALL_MODE_SET)
        }
        publishControlCommand(
            vehicleId,
            MqttConstants.TOPIC_SUFFIX_CONTROL_BASE + MqttConstants.TOPIC_SUFFIX_MODE + MqttConstants.TOPIC_SUFFIX_SET,
            payload,
            _isOverallModeChangeInProgress,
            overallModeChangeJob,
            "차량 자동 제어 모드 변경 응답이 없습니다.",
            "차량 전체 제어 모드 변경"
        ) { overallModeChangeJob = it }
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
        overallModeChangeJob?.cancel()
        mqttManager.cleanup()
        Log.d("MainViewModel", "ViewModel 파괴, MqttManager 정리됨")
    }
}