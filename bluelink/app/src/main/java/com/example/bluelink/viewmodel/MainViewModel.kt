package com.example.bluelink.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bluelink.model.EnvironmentData
import com.example.bluelink.model.ReservationDetails
import com.example.bluelink.model.SunroofUsageData // 가상 선루프 사용 데이터 모델
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainViewModel : ViewModel() {

    private val _vehicleState = MutableStateFlow(VehicleState())
    val vehicleState: StateFlow<VehicleState> = _vehicleState.asStateFlow()

    private val _environmentData = MutableStateFlow(EnvironmentData())
    val environmentData: StateFlow<EnvironmentData> = _environmentData.asStateFlow()

    // --- 유지보수 관련 상태 ---
    private val _maintenanceNotification = MutableStateFlow("")
    val maintenanceNotification: StateFlow<String> = _maintenanceNotification.asStateFlow()

    // 가상 선루프 사용 데이터 (SWR-MOB-17)
    private val _sunroofUsage = MutableStateFlow(SunroofUsageData("Model_S", 750, 3200)) // 모델명, 사용시간(시간), 개폐횟수
    val sunroofUsage: StateFlow<SunroofUsageData> = _sunroofUsage.asStateFlow()

    // --- 서비스 예약 관련 상태 (SWR-MOB-19, SWR-MOB-20) ---
    private val _showReservationForm = MutableStateFlow(false) // 예약 폼 표시 여부
    val showReservationForm: StateFlow<Boolean> = _showReservationForm.asStateFlow()

    private val _reservationDetails = MutableStateFlow(ReservationDetails())
    val reservationDetails: StateFlow<ReservationDetails> = _reservationDetails.asStateFlow()

    private val _reservationStatusMessage = MutableStateFlow("") // 예약 시도 후 결과 메시지
    val reservationStatusMessage: StateFlow<String> = _reservationStatusMessage.asStateFlow()

    val availableServiceCenters = listOf("블루링크 강남점", "블루링크 수원점", "블루링크 부산점") // SWR-MOB-20

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
        checkMaintenance() // ViewModel 초기화 시 유지보수 상태 점검
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
                                // ... (이전과 동일)
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MainViewModel", "MQTT 메시지 파싱 오류: $message", e)
                }
            }
            .launchIn(viewModelScope)
    }

    // SWR-MOB-17: 유지보수 임계값 기반 알림 생성 로직 개선
    fun checkMaintenance() {
        val usage = _sunroofUsage.value
        var notification = ""
        if (usage.totalOperatingHours > 1000) { // 예시: 사용시간 1000시간 초과
            notification += "선루프 사용 시간이 1000시간을 초과했습니다. 점검이 필요합니다. "
        }
        if (usage.openCloseCycles > 5000) { // 예시: 개폐횟수 5000회 초과
            notification += "선루프 개폐 횟수가 5000회를 초과했습니다. 부품 점검을 권장합니다."
        }

        if (notification.isNotEmpty()) {
            _maintenanceNotification.value = "모델 [${usage.sunroofModel}]: $notification"
        } else {
            // 가상으로 20% 확률로 일반 점검 알림 (이전 로직 유지 또는 수정)
            if (Math.random() < 0.2) {
                _maintenanceNotification.value = "선루프 정기 점검 시기가 되었습니다. 가까운 서비스 센터에 문의하세요."
            } else {
                _maintenanceNotification.value = "" // 알림 없음
            }
        }
    }

    // 예약 폼 표시 상태 변경
    fun toggleReservationForm(show: Boolean) {
        _showReservationForm.value = show
        if (!show) { // 폼을 닫을 때 예약 상태 메시지 초기화
            _reservationStatusMessage.value = ""
        }
    }

    // 예약 상세 정보 업데이트 함수들
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

    // SWR-MOB-19: 서비스 예약 요청 처리 (가상)
    fun submitReservation() {
        val details = _reservationDetails.value
        if (details.date.isBlank() || details.time.isBlank() || details.serviceCenter.isBlank()) {
            _reservationStatusMessage.value = "예약 날짜, 시간, 서비스 센터를 모두 선택(입력)해주세요."
            return
        }
        // 가상 예약 처리
        Log.d("MainViewModel", "서비스 예약 요청: $details")
        // TODO: 실제 서버로 예약 정보 전송 로직 추가
        viewModelScope.launch {
            delay(1000) // 가상 네트워크 지연
            _reservationStatusMessage.value = "${details.serviceCenter}에 ${details.date} ${details.time} 예약 요청이 완료되었습니다. (가상)"
            _reservationDetails.value = ReservationDetails() // 폼 초기화
            // toggleReservationForm(false) // 예약 후 폼을 바로 닫을 경우
        }
    }


    fun controlSunroof(action: String) {
        val commandMessage = JSONObject().put("command", action).toString()
        mqttManager.publish(MqttConstants.TOPIC_CONTROL_SUNROOF, commandMessage)
    }

    fun controlAC(action: String) {
        val commandMessage = JSONObject().put("command", action).toString()
        mqttManager.publish(MqttConstants.TOPIC_CONTROL_AC, commandMessage)
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
        // 차량 등록 시, 해당 차량의 선루프 사용 데이터 로드 (가상)
        // 예시: vehicleInfoFromQr에서 모델명 파싱 후 _sunroofUsage 업데이트
        // val model = parseModelFromQr(vehicleInfoFromQr)
        // _sunroofUsage.value = loadSunroofUsageForModel(model)
        checkMaintenance() // 등록 후 유지보수 상태 다시 점검
    }

    fun clearRegistration() {
        _registeredVehicleInfo.value = ""
        _qrScanResult.value = ""
        _sunroofUsage.value = SunroofUsageData() // 기본값으로 초기화
        checkMaintenance()
    }

    override fun onCleared() {
        super.onCleared()
        mqttManager.cleanup()
        Log.d("MainViewModel", "ViewModel 파괴, MqttManager 정리됨")
    }
}