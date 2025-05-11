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

    // 차량 상태 정보를 저장하는 MutableStateFlow (UI에서 관찰 가능)
    private val _vehicleState = MutableStateFlow(VehicleState())
    val vehicleState: StateFlow<VehicleState> = _vehicleState.asStateFlow()

    // 환경 데이터를 저장하는 MutableStateFlow
    private val _environmentData = MutableStateFlow(EnvironmentData())
    val environmentData: StateFlow<EnvironmentData> = _environmentData.asStateFlow()

    // 유지보수 알림 메시지를 저장하는 MutableStateFlow
    private val _maintenanceNotification = MutableStateFlow("")
    val maintenanceNotification: StateFlow<String> = _maintenanceNotification.asStateFlow()

    // 등록된 차량 정보를 저장하는 MutableStateFlow
    private val _registeredVehicleInfo = MutableStateFlow("")
    val registeredVehicleInfo: StateFlow<String> = _registeredVehicleInfo.asStateFlow()

    // QR 스캔 결과를 저장하는 MutableStateFlow (가상)
    private val _qrScanResult = MutableStateFlow("")
    val qrScanResult: StateFlow<String> = _qrScanResult.asStateFlow()

    // ViewModel이 생성될 때 초기 데이터 로드 및 주기적 업데이트 시작
    init {
        loadInitialData() // 초기 데이터 로드
        startDataRefreshCycle() // 데이터 주기적 새로고침 시작
        checkMaintenance() // 유지보수 필요 여부 확인 (가상)
    }

    // 초기 데이터를 로드하는 함수 (가상)
    private fun loadInitialData() {
        // 실제 앱에서는 MQTT 서비스 등을 통해 서버로부터 데이터를 가져옴
        _vehicleState.value = VehicleState(sunroofStatus = "닫힘", acStatus = "꺼짐")
        _environmentData.value = EnvironmentData(
            indoorTemperature = 22.5, indoorHumidity = 45.0,
            outdoorTemperature = 25.0, outdoorHumidity = 50.0,
            airQuality = "좋음", fineDust = "낮음"
        )
        // _registeredVehicleInfo.value = "나의 GV70" // 초기 등록 차량 정보 (테스트용)
    }

    // 데이터를 주기적으로 새로고침하는 사이클 시작 함수 (SWR-MOB-03 관련, 가상)
    private fun startDataRefreshCycle() {
        viewModelScope.launch {
            while (true) {
                delay(10000) // 10초 대기 (SWR-MOB-03 요구사항)
                refreshData() // 데이터 새로고침 함수 호출
            }
        }
    }

    // 데이터를 새로고침하는 함수 (가상)
    fun refreshData() {
        // 실제로는 MQTT를 통해 새로운 데이터를 수신하여 업데이트
        // 여기서는 임의의 값으로 변경하여 테스트
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
        // TODO: SWR-MOB-03 - MQTT를 통한 실제 데이터 수신 및 갱신 로직 구현 필요
    }

    // 선루프 제어 함수 (가상, SWR-MOB-04 관련)
    fun controlSunroof(action: String) {
        // 실제로는 MQTT를 통해 제어 명령을 전송하고 결과를 확인
        println("선루프 제어 명령: $action") // 로그 출력으로 대체
        // 가상으로 상태 변경 (실제로는 서버 응답에 따라 변경되어야 함)
        _vehicleState.value = _vehicleState.value.copy(
            sunroofStatus = when(action.lowercase()){
                "open" -> "열림"
                "close" -> "닫힘"
                else -> _vehicleState.value.sunroofStatus
            }
        )
        // TODO: SWR-MOB-04 - MQTT를 통한 실제 제어 명령 전송 및 결과 확인 로직 구현 필요
    }

    // 에어컨/히터 제어 함수 (가상, SWR-MOB-04 관련)
    fun controlAC(action: String) {
        // 실제로는 MQTT를 통해 제어 명령을 전송하고 결과를 확인
        println("에어컨/히터 제어 명령: $action") // 로그 출력으로 대체
        _vehicleState.value = _vehicleState.value.copy(
            acStatus = when(action.lowercase()){
                "on" -> "켜짐 - 냉방" // 단순화를 위해 냉방으로 고정
                "off" -> "꺼짐"
                else -> _vehicleState.value.acStatus
            }
        )
        // TODO: SWR-MOB-04 - MQTT를 통한 실제 제어 명령 전송 및 결과 확인 로직 구현 필요
    }

    // 유지보수 필요 여부 확인 함수 (가상, SR-MOB-11, SWR-MOB-17 관련)
    private fun checkMaintenance() {
        // 실제로는 선루프 사용 데이터(모델별 사용 시간, 개폐 횟수, 이상 징후)를 기반으로 판단
        // 여기서는 가상으로 30% 확률로 알림 발생
        if (Math.random() < 0.3) {
            _maintenanceNotification.value = "선루프 정기 점검이 필요합니다. (모델: GV80, 사용 시간: 500시간 초과)"
        }
        // TODO: SWR-MOB-17 - 실제 유지보수 임계값 설정 및 관리 로직 구현 필요
    }

    // QR 코드 스캔 시작 함수 (가상, SWR-MOB-28 관련)
    fun startQrScan() {
        // 실제로는 카메라를 이용한 QR 스캐너를 실행
        println("QR 코드 스캔 시작...") // 로그 출력으로 대체
        // 가상으로 스캔 결과 생성 (실제로는 스캐너 라이브러리 결과 사용)
        viewModelScope.launch {
            delay(2000) // 스캔하는 것처럼 잠시 대기
            val scannedData = "차량ID:XYZ12345,모델:GV70,색상:블랙" // 가상 QR 데이터
            _qrScanResult.value = scannedData
            println("QR 코드 스캔 완료: $scannedData")
        }
        // TODO: SWR-MOB-28 - 실제 QR 스캐너 연동 및 결과 처리 로직 구현 필요
    }

    // 차량 등록 함수 (가상, SWR-MOB-28 관련)
    fun registerVehicle(vehicleInfo: String) {
        // 실제로는 서버에 차량 정보를 전송하고 등록 결과를 받음
        println("차량 등록 시도: $vehicleInfo") // 로그 출력
        _registeredVehicleInfo.value = vehicleInfo // 화면에 바로 표시 (가상)
        _qrScanResult.value = "" // QR 스캔 결과 초기화
        println("차량 등록 완료 (가상): $vehicleInfo")
        // TODO: SWR-MOB-28 - 실제 차량 등록 및 인증 메커니즘 구현 필요 (SR-MOB-14)
    }
}