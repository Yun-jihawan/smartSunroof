package com.example.bluelink

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.bluelink.ui.theme.BluelinkTheme
import com.example.bluelink.viewmodel.MainViewModel

// 앱의 메인 액티비티
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BluelinkTheme {
                // 전체 화면을 감싸는 Scaffold
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // 메인 화면 컨텐츠
                    MainScreen(
                        modifier = Modifier.padding(innerPadding),
                        viewModel = MainViewModel() // ViewModel 인스턴스 생성
                    )
                }
            }
        }
    }
}

// 메인 화면을 구성하는 Composable 함수
@Composable
fun MainScreen(modifier: Modifier = Modifier, viewModel: MainViewModel) {
    // 화면에 표시될 탭들의 제목
    val tabs = listOf("모니터링", "제어", "유지보수", "차량 등록")
    // 현재 선택된 탭의 인덱스를 저장하는 상태 변수
    var selectedTabIndex by remember { mutableStateOf(0) }

    // Column을 사용하여 요소들을 수직으로 배치
    Column(modifier = modifier.fillMaxSize()) {
        // 탭 메뉴를 구성하는 TabRow
        TabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index }, // 탭 클릭 시 선택된 탭 변경
                    text = { Text(title) } // 탭 제목 표시
                )
            }
        }

        // 선택된 탭에 따라 다른 컨텐츠를 표시
        when (selectedTabIndex) {
            0 -> MonitoringScreen(viewModel) // 모니터링 화면
            1 -> ControlScreen(viewModel)     // 제어 화면
            2 -> MaintenanceScreen(viewModel) // 유지보수 화면
            3 -> VehicleRegistrationScreen(viewModel) // 차량 등록 화면
        }
    }
}

// 모니터링 화면 Composable (임시)
@Composable
fun MonitoringScreen(viewModel: MainViewModel) {
    // ViewModel로부터 차량 상태 데이터를 관찰
    val vehicleState by viewModel.vehicleState.collectAsState()
    // ViewModel로부터 환경 데이터를 관찰
    val environmentData by viewModel.environmentData.collectAsState()

    // Column을 사용하여 정보들을 수직으로 나열
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally, // 가로 중앙 정렬
        verticalArrangement = Arrangement.Center // 세로 중앙 정렬
    ) {
        Text("차량 모니터링", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp)) // 간격
        Text("선루프 상태: ${vehicleState.sunroofStatus}")
        Text("에어컨 상태: ${vehicleState.acStatus}")
        Spacer(modifier = Modifier.height(8.dp))
        Text("실내 온도: ${environmentData.indoorTemperature}°C, 습도: ${environmentData.indoorHumidity}%")
        Text("실외 온도: ${environmentData.outdoorTemperature}°C, 습도: ${environmentData.outdoorHumidity}%")
        Text("공기질: ${environmentData.airQuality}, 미세먼지: ${environmentData.fineDust}")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { viewModel.refreshData() }) { // 데이터 새로고침 버튼
            Text("새로고침 (10초마다 자동 갱신 예정)")
        }
    }
}

// 제어 화면 Composable (임시)
@Composable
fun ControlScreen(viewModel: MainViewModel) {
    // Column을 사용하여 제어 버튼들을 수직으로 나열
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("차량 원격 제어", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { viewModel.controlSunroof("open") }) { // 선루프 열기 버튼
            Text("선루프 열기")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { viewModel.controlSunroof("close") }) { // 선루프 닫기 버튼
            Text("선루프 닫기")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { viewModel.controlAC("on") }) { // 에어컨 켜기 버튼
            Text("에어컨 켜기")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { viewModel.controlAC("off") }) { // 에어컨 끄기 버튼
            Text("에어컨 끄기")
        }
        // TODO: 수동/자동 제어 UI 추가
    }
}

// 유지보수 화면 Composable (임시)
@Composable
fun MaintenanceScreen(viewModel: MainViewModel) {
    // ViewModel로부터 유지보수 알림을 관찰
    val maintenanceNotification by viewModel.maintenanceNotification.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("선루프 유지보수", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        if (maintenanceNotification.isNotEmpty()) { // 알림이 있으면 표시
            Text("알림: $maintenanceNotification")
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { /* TODO: 서비스 센터 예약 화면으로 이동 */ }) {
                Text("서비스 센터 예약하기")
            }
        } else {
            Text("현재 유지보수 알림이 없습니다.")
        }
        // TODO: 서비스 센터 위치 정보 및 선택 UI 추가
    }
}

// 차량 등록 화면 Composable (임시) - 수정된 부분
@Composable
fun VehicleRegistrationScreen(viewModel: MainViewModel) {
    // ViewModel로부터 등록된 차량 정보를 관찰
    val registeredVehicleInfo by viewModel.registeredVehicleInfo.collectAsState()
    // ViewModel로부터 QR 스캔 결과를 관찰 (Composable 최상단에서 호출)
    val qrScanResultValue by viewModel.qrScanResult.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("차량 등록", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        if (registeredVehicleInfo.isNotEmpty()) { // 등록된 차량 정보가 있으면 표시
            Text("등록된 차량: $registeredVehicleInfo")
        } else {
            Button(onClick = { viewModel.startQrScan() }) { // QR 스캔 시작 버튼
                Text("QR 코드로 차량 등록하기")
            }
            // TODO: QR 스캐너 실행 및 결과 처리 (가상)
            // 가상 QR 스캔 결과 처리 예시
            // 수정: qrScanResultValue 변수를 사용하여 조건 검사 및 값 표시
            if (qrScanResultValue.isNotEmpty()) {
                Text("스캔된 차량 정보 (가상): $qrScanResultValue") // 수정된 부분
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { viewModel.registerVehicle(qrScanResultValue) }) { // 수정된 부분
                    Text("이 차량으로 등록")
                }
            }
        }
    }
}


// 메인 화면 미리보기
@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    BluelinkTheme {
        MainScreen(viewModel = MainViewModel()) // 미리보기용 ViewModel 인스턴스
    }
}