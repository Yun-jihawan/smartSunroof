package com.example.bluelink.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bluelink.model.EnvironmentData
import com.example.bluelink.model.VehicleState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// 메인 화면의 UI 상태 및 비즈니스 로직을 관리하는 ViewModel
class MainViewModel : ViewModel() {

    private val _vehicleState = MutableStateFlow(VehicleState())
    val vehicleState: StateFlow<VehicleState> = _vehicleState.asStateFlow()

    private val _environmentData = MutableStateFlow(EnvironmentData())
    val environmentData: StateFlow<EnvironmentData> = _environmentData.asStateFlow()

    private val _maintenanceNotification = MutableStateFlow("")
    val maintenanceNotification: StateFlow<String> = _maintenanceNotification.asStateFlow()

    private val _registeredVehicleInfo = MutableStateFlow("")
    val registeredVehicleInfo: StateFlow<String> = _registeredVehicleInfo.asStateFlow()

    // QR 스캔 결과를 임시 저장 (UI에 확인용으로 표시 후 등록 시 사용)
    private val _qrScanResult = MutableStateFlow("")
    val qrScanResult: StateFlow<String> = _qrScanResult.asStateFlow()

    init {
        loadInitialData()
        startDataRefreshCycle()
        checkMaintenance()
    }

    private fun loadInitialData() {
        _vehicleState.value = VehicleState(sunroofStatus = "닫힘", acStatus = "꺼짐")
        _environmentData.value = EnvironmentData(
            indoorTemperature = 22.5, indoorHumidity = 45.0,
            outdoorTemperature = 25.0, outdoorHumidity = 50.0,
            airQuality = "좋음", fineDust = "낮음"
        )
    }

    private fun startDataRefreshCycle() {
        viewModelScope.launch {
            while (true) {
                delay(10000)
                refreshData()
            }
        }
    }

    fun refreshData() {
        _vehicleState.value = _vehicleState.value.copy(
            sunroofStatus = if (Math.random() > 0.5) "열림" else "닫힘",
            acStatus = if (Math.random() > 0.7) "켜짐 - 냉방" else "꺼짐"
        )
        _environmentData.value = _environmentData.value.copy(
            indoorTemperature = (20..25).random().toDouble() + Math.random(),
            outdoorTemperature = (23..28).random().toDouble() + Math.random(),
            airQuality = if (Math.random() > 0.3) "좋음" else "보통",
            fineDust = if (Math.random() > 0.6) "보통" else "낮음"
        )
    }

    fun controlSunroof(action: String) {
        println("선루프 제어 명령: $action")
        _vehicleState.value = _vehicleState.value.copy(
            sunroofStatus = when(action.lowercase()){
                "open" -> "열림"
                "close" -> "닫힘"
                else -> _vehicleState.value.sunroofStatus
            }
        )
    }

    fun controlAC(action: String) {
        println("에어컨/히터 제어 명령: $action")
        _vehicleState.value = _vehicleState.value.copy(
            acStatus = when(action.lowercase()){
                "on" -> "켜짐 - 냉방"
                "off" -> "꺼짐"
                else -> _vehicleState.value.acStatus
            }
        )
    }

    private fun checkMaintenance() {
        if (Math.random() < 0.3) {
            _maintenanceNotification.value = "선루프 정기 점검이 필요합니다. (모델: GV80, 사용 시간: 500시간 초과)"
        }
    }

    // QR 스캔 결과 처리 함수
    fun processQrScanResult(contents: String?) {
        if (contents != null) {
            _qrScanResult.value = contents // 스캔된 내용을 _qrScanResult에 저장
            println("QR 스캔 결과 수신: $contents")
        } else {
            _qrScanResult.value = "" // 스캔 실패 또는 취소 시 초기화
            println("QR 스캔 실패 또는 취소됨")
        }
    }

    // 차량 등록 함수
    fun registerVehicle(vehicleInfoFromQr: String) {
        println("차량 등록 시도: $vehicleInfoFromQr")
        // 실제로는 여기서 vehicleInfoFromQr를 파싱하여 서버에 등록 요청 등을 수행
        _registeredVehicleInfo.value = vehicleInfoFromQr // 등록된 정보로 업데이트
        _qrScanResult.value = "" // QR 스캔 결과 임시 저장 값 초기화
        println("차량 등록 완료 (가상): $vehicleInfoFromQr")
    }

    // 등록된 차량 정보 초기화 함수 (다른 차량 등록을 위해)
    fun clearRegistration() {
        _registeredVehicleInfo.value = ""
        _qrScanResult.value = ""
        println("등록된 차량 정보 초기화됨")
    }
}