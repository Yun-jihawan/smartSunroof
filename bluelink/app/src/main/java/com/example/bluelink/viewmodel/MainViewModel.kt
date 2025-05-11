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

    // ... (기존 StateFlow 선언들 유지) ...
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

    private val _registeredVehicleInfo = MutableStateFlow("") // QR 스캔 결과 원본 저장
    val registeredVehicleInfo: StateFlow<String> = _registeredVehicleInfo.asStateFlow()

    // 현재 등록된 차량의 ID (동적 토픽 생성에 사용)
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
                    // 연결 성공 시 현재 등록된 차량이 있다면 해당 차량 토픽 구독
                    _currentVehicleId.value?.let { vehicleId ->
                        subscribeToVehicleTopics(vehicleId)
                    }
                }
            }
            .launchIn(viewModelScope)

        mqttManager.connect() // MqttManager 생성자에서 connect 안 하므로 여기서 호출
        observeMqttMessages()
        checkMaintenance()

        // currentVehicleId 변경 감지하여 토픽 재구독/해제 (앱 실행 중 차량 변경 시)
        // 이 부분은 registerVehicle / clearRegistration 에서 명시적으로 처리하므로 중복될 수 있음.
        // 필요하다면 아래 로직 활성화 또는 register/clear에서만 처리하도록 조정.
        /*
        _currentVehicleId.onEach { vehicleId ->
            // 이전 차량 토픽 구독 해제 (구현 필요)
            // 새 차량 토픽 구독
            vehicleId?.let { subscribeToVehicleTopics(it) }
        }.launchIn(viewModelScope)
        */
    }

    // 특정 차량의 관련 토픽들을 구독하는 함수
    private fun subscribeToVehicleTopics(vehicleId: String) {
        if (vehicleId.isBlank()) return

        val topicsToSubscribe = listOf(
            MqttConstants.getStatusTopic(vehicleId),
            MqttConstants.getEnvironmentTopic(vehicleId)
        )
        Log.d("MainViewModel", "다음 차량 토픽 구독 시도 ($vehicleId): $topicsToSubscribe")
        mqttManager.subscribeToTopics(topicsToSubscribe)
    }

    // 특정 차량의 관련 토픽들을 구독 해제하는 함수
    private fun unsubscribeFromVehicleTopics(vehicleId: String) {
        if (vehicleId.isBlank()) return

        val topicsToUnsubscribe = listOf(
            MqttConstants.getStatusTopic(vehicleId),
            MqttConstants.getEnvironmentTopic(vehicleId)
            // 제어 토픽은 보통 발행만 하므로 구독 해제 필요 없음. 만약 응답을 구독한다면 추가.
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
                        // 현재 등록된 차량의 토픽에 대해서만 메시지 처리
                        when (topic) {
                            MqttConstants.getStatusTopic(vehicleId) -> {
                                val jsonObj = JSONObject(message)
                                val newSunroofStatus = jsonObj.optString("sunroof", _vehicleState.value.sunroofStatus)
                                val newAcStatus = jsonObj.optString("ac", _vehicleState.value.acStatus)

                                if (newSunroofStatus != _vehicleState.value.sunroofStatus && _isSunroofCommandInProgress.value) {
                                    sunroofControlJob?.cancel(); _isSunroofCommandInProgress.value = false
                                }
                                if (newAcStatus != _vehicleState.value.acStatus && _isAcCommandInProgress.value) {
                                    acControlJob?.cancel(); _isAcCommandInProgress.value = false
                                }
                                _vehicleState.value = VehicleState(sunroofStatus = newSunroofStatus, acStatus = newAcStatus)
                            }
                            MqttConstants.getEnvironmentTopic(vehicleId) -> {
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
                                Log.w("MainViewModel", "구독하지 않았거나 알 수 없는 토픽 메시지 수신: $topic")
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
        // ... (이전과 동일) ...
        if (_mqttConnectionState.value != MqttConnectionState.CONNECTING && _mqttConnectionState.value != MqttConnectionState.CONNECTED) {
            Log.d("MainViewModel", "사용자 요청으로 MQTT 재연결 시도")
            mqttManager.connect()
        } else {
            Log.d("MainViewModel", "이미 연결 중이거나 연결된 상태입니다.")
        }
    }

    // QR 코드에서 차량 ID를 파싱하는 가상 함수
    private fun parseVehicleIdFromQr(qrData: String): String? {
        // 예시: "차량ID:XYZ123,모델:GV70" 형태의 데이터에서 "XYZ123" 추출
        // 실제 QR 데이터 형식에 맞춰 파싱 로직 구현 필요
        return qrData.split(",").firstOrNull { it.startsWith("차량ID:") }?.substringAfter("차량ID:")?.trim()
    }

    fun registerVehicle(vehicleInfoFromQr: String) {
        Log.d("MainViewModel", "차량 등록 시도 (원본 QR 데이터): $vehicleInfoFromQr")
        val parsedVehicleId = parseVehicleIdFromQr(vehicleInfoFromQr)

        if (parsedVehicleId == null || parsedVehicleId.isBlank()) {
            Log.e("MainViewModel", "차량 ID 파싱 실패: $vehicleInfoFromQr")
            viewModelScope.launch { _mqttErrorEventChannel.send("잘못된 QR 코드 형식입니다 (차량 ID 없음).") }
            _qrScanResult.value = "" // 잘못된 QR 결과는 비움
            return
        }

        // 이전에 등록된 차량이 있다면 해당 차량 토픽 구독 해제
        _currentVehicleId.value?.let { oldVehicleId ->
            if (oldVehicleId != parsedVehicleId) { // 다른 차량으로 변경될 경우에만
                unsubscribeFromVehicleTopics(oldVehicleId)
            }
        }

        // 새 차량 정보 및 ID 설정
        _registeredVehicleInfo.value = vehicleInfoFromQr // 원본 QR 정보 저장
        _currentVehicleId.value = parsedVehicleId        // 파싱된 차량 ID 저장
        _qrScanResult.value = ""                         // QR 스캔 결과는 비움

        Log.d("MainViewModel", "차량 등록 완료: ID='${parsedVehicleId}', Info='${vehicleInfoFromQr}'")

        // MQTT 연결 상태 확인 후 새 차량 토픽 구독
        if (_mqttConnectionState.value == MqttConnectionState.CONNECTED) {
            subscribeToVehicleTopics(parsedVehicleId)
        } else {
            Log.w("MainViewModel", "MQTT 미연결 상태. 차량($parsedVehicleId) 토픽은 연결 후 자동 구독 예정.")
            // MqttManager의 connectComplete에서 currentTopics를 참조하여 구독하므로 별도 처리 불필요할 수 있음
            // 또는, _currentVehicleId가 설정되면 connectionState 구독 부분에서 처리됨
        }

        // 차량 모델에 따른 가상 사용 데이터 로드 및 유지보수 점검 (예시 로직)
        // if (vehicleInfoFromQr.contains("Model_X")) {
        //     _sunroofUsage.value = SunroofUsageData("Model_X", 1200, 5500)
        // } else {
        //     _sunroofUsage.value = SunroofUsageData(parsedVehicleId, 100, 500) // 모델명 대신 ID 사용 예시
        // }
        checkMaintenance()
        viewModelScope.launch { _mqttErrorEventChannel.send("${parsedVehicleId} 차량이 등록되었습니다.") }
    }

    fun clearRegistration() {
        Log.d("MainViewModel", "등록된 차량 정보 초기화")
        // 현재 등록된 차량의 토픽 구독 해제
        _currentVehicleId.value?.let { vehicleId ->
            unsubscribeFromVehicleTopics(vehicleId)
        }

        _registeredVehicleInfo.value = ""
        _currentVehicleId.value = null // 차량 ID도 null로 설정
        _qrScanResult.value = ""
        _sunroofUsage.value = SunroofUsageData()
        _vehicleState.value = VehicleState() // 차량 상태 초기화
        _environmentData.value = EnvironmentData() // 환경 데이터 초기화
        checkMaintenance()
        viewModelScope.launch { _mqttErrorEventChannel.send("차량 등록이 해제되었습니다.") }
    }


    fun controlSunroof(action: String) {
        val vehicleId = _currentVehicleId.value
        if (vehicleId == null) {
            viewModelScope.launch { _mqttErrorEventChannel.send("등록된 차량이 없습니다. 먼저 차량을 등록해주세요.") }
            return
        }
        if (_mqttConnectionState.value != MqttConnectionState.CONNECTED) {
            viewModelScope.launch { _mqttErrorEventChannel.send("MQTT가 연결되지 않아 명령을 보낼 수 없습니다.") }
            return
        }
        if (_isSunroofCommandInProgress.value || _isAcCommandInProgress.value) {
            viewModelScope.launch { _mqttErrorEventChannel.send("다른 제어 명령이 이미 진행 중입니다.") }
            return
        }

        val topic = MqttConstants.getControlSunroofTopic(vehicleId)
        val commandMessage = JSONObject().put("command", action).toString()
        mqttManager.publish(topic, commandMessage)
        _isSunroofCommandInProgress.value = true
        Log.d("MainViewModel", "선루프 제어 명령 발행 ($vehicleId): $action, 토픽: $topic, 로딩 시작")

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
        val vehicleId = _currentVehicleId.value
        if (vehicleId == null) {
            viewModelScope.launch { _mqttErrorEventChannel.send("등록된 차량이 없습니다. 먼저 차량을 등록해주세요.") }
            return
        }
        if (_mqttConnectionState.value != MqttConnectionState.CONNECTED) {
            viewModelScope.launch { _mqttErrorEventChannel.send("MQTT가 연결되지 않아 명령을 보낼 수 없습니다.") }
            return
        }
        if (_isSunroofCommandInProgress.value || _isAcCommandInProgress.value) {
            viewModelScope.launch { _mqttErrorEventChannel.send("다른 제어 명령이 이미 진행 중입니다.") }
            return
        }

        val topic = MqttConstants.getControlAcTopic(vehicleId)
        val commandMessage = JSONObject().put("command", action).toString()
        mqttManager.publish(topic, commandMessage)
        _isAcCommandInProgress.value = true
        Log.d("MainViewModel", "AC 제어 명령 발행 ($vehicleId): $action, 토픽: $topic, 로딩 시작")

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

    // ... (checkMaintenance, 예약 관련 함수, QR 스캔 결과 처리 함수 등 나머지 함수들은 이전과 거의 동일하게 유지) ...
    fun checkMaintenance() { // sunroofUsage가 vehicleId에 따라 변경된다면 이 부분도 수정 필요
        val usage = _sunroofUsage.value
        var notification = ""
        if (usage.totalOperatingHours > 1000) { // 이 임계값들도 차량 모델별로 달라질 수 있음 (SWR-MOB-17)
            notification += "선루프 사용 시간이 1000시간을 초과했습니다. 점검이 필요합니다. "
        }
        if (usage.openCloseCycles > 5000) {
            notification += "선루프 개폐 횟수가 5000회를 초과했습니다. 부품 점검을 권장합니다."
        }

        if (notification.isNotEmpty()) {
            val modelInfo = if (_currentVehicleId.value != null) "차량 [${_currentVehicleId.value}]의 " else ""
            _maintenanceNotification.value = "$modelInfo${usage.sunroofModel} 모델: $notification"
        } else {
            // ... (기존 랜덤 알림 또는 알림 없음 로직)
            if (Math.random() < 0.2 && _currentVehicleId.value != null) { // 차량 등록 시에만 랜덤 정기점검 알림
                _maintenanceNotification.value = "차량 [${_currentVehicleId.value}] 선루프 정기 점검 시기가 되었습니다."
            } else {
                _maintenanceNotification.value = ""
            }
        }
    }

    fun toggleReservationForm(show: Boolean) {
        _showReservationForm.value = show
        if (!show) { _reservationStatusMessage.value = "" }
    }
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
        viewModelScope.launch {
            delay(1000)
            _reservationStatusMessage.value = "${_currentVehicleId.value?.let { "[$it] " } ?: ""}예약 요청 완료 (가상)"
            _reservationDetails.value = ReservationDetails()
        }
    }
    fun processQrScanResult(contents: String?) {
        if (contents != null) { _qrScanResult.value = contents }
        else { _qrScanResult.value = "" }
    }
    // registerVehicle, clearRegistration은 위에서 수정됨

    override fun onCleared() {
        super.onCleared()
        sunroofControlJob?.cancel()
        acControlJob?.cancel()
        mqttManager.cleanup() // MqttManager에서 모든 구독 해제 및 연결 종료
        Log.d("MainViewModel", "ViewModel 파괴, MqttManager 정리됨")
    }
}