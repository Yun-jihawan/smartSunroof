package com.example.bluelink.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bluelink.model.EnvironmentData
import com.example.bluelink.model.ReservationDetails
import com.example.bluelink.model.SunroofUsageData
import com.example.bluelink.model.VehicleState
import com.example.bluelink.mqtt.MqttConstants
import com.example.bluelink.mqtt.MqttManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.json.JSONObject
// import java.text.SimpleDateFormat // MainViewModel에서 직접 사용하지 않으므로 주석 처리 또는 제거 가능
// import java.util.Calendar // MainViewModel에서 직접 사용하지 않으므로 주석 처리 또는 제거 가능
// import java.util.Locale // MainViewModel에서 직접 사용하지 않으므로 주석 처리 또는 제거 가능

class MainViewModel : ViewModel() {

    private val _vehicleState = MutableStateFlow(VehicleState())
    val vehicleState: StateFlow<VehicleState> = _vehicleState.asStateFlow()

    private val _environmentData = MutableStateFlow(EnvironmentData())
    val environmentData: StateFlow<EnvironmentData> = _environmentData.asStateFlow()

    // --- 유지보수 관련 상태 ---
    private val _maintenanceNotification = MutableStateFlow("")
    val maintenanceNotification: StateFlow<String> = _maintenanceNotification.asStateFlow()

    private val _sunroofUsage = MutableStateFlow(SunroofUsageData("Model_S", 750, 3200))
    val sunroofUsage: StateFlow<SunroofUsageData> = _sunroofUsage.asStateFlow()

    // --- 서비스 예약 관련 상태 ---
    private val _showReservationForm = MutableStateFlow(false)
    val showReservationForm: StateFlow<Boolean> = _showReservationForm.asStateFlow()

    private val _reservationDetails = MutableStateFlow(ReservationDetails())
    val reservationDetails: StateFlow<ReservationDetails> = _reservationDetails.asStateFlow()

    private val _reservationStatusMessage = MutableStateFlow("")
    val reservationStatusMessage: StateFlow<String> = _reservationStatusMessage.asStateFlow()

    val availableServiceCenters = listOf("블루링크 강남점", "블루링크 수원점", "블루링크 부산점")

    // --- 제어 명령 진행 상태 ---
    private val _isSunroofCommandInProgress = MutableStateFlow(false)
    val isSunroofCommandInProgress: StateFlow<Boolean> = _isSunroofCommandInProgress.asStateFlow()

    private val _isAcCommandInProgress = MutableStateFlow(false)
    val isAcCommandInProgress: StateFlow<Boolean> = _isAcCommandInProgress.asStateFlow()

    private var sunroofControlJob: Job? = null
    private var acControlJob: Job? = null

    // --- 기타 상태 ---
    private val _registeredVehicleInfo = MutableStateFlow("")
    val registeredVehicleInfo: StateFlow<String> = _registeredVehicleInfo.asStateFlow()

    private val _qrScanResult = MutableStateFlow("")
    val qrScanResult: StateFlow<String> = _qrScanResult.asStateFlow()

    private val mqttManager: MqttManager = MqttManager()

    init {
        mqttManager.connect()
        observeMqttMessages()
        subscribeToTopics()
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
                }
            }
            .launchIn(viewModelScope)
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

    fun controlSunroof(action: String) {
        if (_isSunroofCommandInProgress.value || _isAcCommandInProgress.value) {
            Log.w("MainViewModel", "다른 제어 명령이 이미 진행 중입니다.")
            return
        }
        val commandMessage = JSONObject().put("command", action).toString()
        mqttManager.publish(MqttConstants.TOPIC_CONTROL_SUNROOF, commandMessage)
        _isSunroofCommandInProgress.value = true
        Log.d("MainViewModel", "선루프 제어 명령 발행: $action, 로딩 시작")

        sunroofControlJob?.cancel()
        sunroofControlJob = viewModelScope.launch {
            delay(10000) // 10초 타임아웃
            if (_isSunroofCommandInProgress.value) {
                _isSunroofCommandInProgress.value = false
                Log.w("MainViewModel", "선루프 제어 응답 시간 초과")
                // 필요시 사용자에게 '응답 없음'과 같은 피드백을 줄 수 있는 상태 업데이트
                // _vehicleState.value = _vehicleState.value.copy(sunroofStatus = "${_vehicleState.value.sunroofStatus} (응답 지연)")
            }
        }
    }

    fun controlAC(action: String) {
        if (_isSunroofCommandInProgress.value || _isAcCommandInProgress.value) {
            Log.w("MainViewModel", "다른 제어 명령이 이미 진행 중입니다.")
            return
        }
        val commandMessage = JSONObject().put("command", action).toString()
        mqttManager.publish(MqttConstants.TOPIC_CONTROL_AC, commandMessage)
        _isAcCommandInProgress.value = true
        Log.d("MainViewModel", "AC 제어 명령 발행: $action, 로딩 시작")

        acControlJob?.cancel()
        acControlJob = viewModelScope.launch {
            delay(10000) // 10초 타임아웃
            if (_isAcCommandInProgress.value) {
                _isAcCommandInProgress.value = false
                Log.w("MainViewModel", "AC 제어 응답 시간 초과")
                // _vehicleState.value = _vehicleState.value.copy(acStatus = "${_vehicleState.value.acStatus} (응답 지연)")
            }
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
        // 가상으로 차량 모델에 따른 사용 데이터 로드 및 유지보수 점검
        // 예시: if (vehicleInfoFromQr.contains("Model_X")) {
        //     _sunroofUsage.value = SunroofUsageData("Model_X", 1200, 5500)
        // } else {
        //     _sunroofUsage.value = SunroofUsageData("Model_S_Default", 100, 500)
        // }
        checkMaintenance()
    }

    fun clearRegistration() {
        _registeredVehicleInfo.value = ""
        _qrScanResult.value = ""
        _sunroofUsage.value = SunroofUsageData() // 기본값으로 초기화
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