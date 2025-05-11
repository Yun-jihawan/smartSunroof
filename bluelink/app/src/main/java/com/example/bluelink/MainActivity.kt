package com.example.bluelink

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.bluelink.ui.theme.BluelinkTheme
import com.example.bluelink.viewmodel.MainViewModel
import com.google.zxing.integration.android.IntentIntegrator // ZXing IntentIntegrator import
import com.journeyapps.barcodescanner.ScanContract // ZXing ScanContract import
import com.journeyapps.barcodescanner.ScanOptions // ZXing ScanOptions import

// 앱의 메인 액티비티
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BluelinkTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        modifier = Modifier.padding(innerPadding),
                        viewModel = MainViewModel()
                    )
                }
            }
        }
    }
}

// 메인 화면을 구성하는 Composable 함수
@Composable
fun MainScreen(modifier: Modifier = Modifier, viewModel: MainViewModel) {
    val tabs = listOf("모니터링", "제어", "유지보수", "차량 등록")
    var selectedTabIndex by remember { mutableStateOf(0) }

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title) }
                )
            }
        }

        when (selectedTabIndex) {
            0 -> MonitoringScreen(viewModel)
            1 -> ControlScreen(viewModel)
            2 -> MaintenanceScreen(viewModel)
            3 -> VehicleRegistrationScreen(viewModel) // VehicleRegistrationScreen 호출
        }
    }
}

// 모니터링 화면 Composable
@Composable
fun MonitoringScreen(viewModel: MainViewModel) {
    val vehicleState by viewModel.vehicleState.collectAsState()
    val environmentData by viewModel.environmentData.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("차량 모니터링", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text("선루프 상태: ${vehicleState.sunroofStatus}")
        Text("에어컨 상태: ${vehicleState.acStatus}")
        Spacer(modifier = Modifier.height(8.dp))
        Text("실내 온도: ${environmentData.indoorTemperature}°C, 습도: ${environmentData.indoorHumidity}%")
        Text("실외 온도: ${environmentData.outdoorTemperature}°C, 습도: ${environmentData.outdoorHumidity}%")
        Text("공기질: ${environmentData.airQuality}, 미세먼지: ${environmentData.fineDust}")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { viewModel.refreshData() }) {
            Text("새로고침 (10초마다 자동 갱신 예정)")
        }
    }
}

// 제어 화면 Composable
@Composable
fun ControlScreen(viewModel: MainViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("차량 원격 제어", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { viewModel.controlSunroof("open") }) {
            Text("선루프 열기")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { viewModel.controlSunroof("close") }) {
            Text("선루프 닫기")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { viewModel.controlAC("on") }) {
            Text("에어컨 켜기")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { viewModel.controlAC("off") }) {
            Text("에어컨 끄기")
        }
    }
}

// 유지보수 화면 Composable
@Composable
fun MaintenanceScreen(viewModel: MainViewModel) {
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
        if (maintenanceNotification.isNotEmpty()) {
            Text("알림: $maintenanceNotification")
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { /* TODO: 서비스 센터 예약 화면으로 이동 */ }) {
                Text("서비스 센터 예약하기")
            }
        } else {
            Text("현재 유지보수 알림이 없습니다.")
        }
    }
}

// 차량 등록 화면 Composable - QR 스캐너 연동
@Composable
fun VehicleRegistrationScreen(viewModel: MainViewModel) {
    val registeredVehicleInfo by viewModel.registeredVehicleInfo.collectAsState()
    val qrScanResultValue by viewModel.qrScanResult.collectAsState()
    val context = LocalContext.current // 현재 컨텍스트 가져오기

    // QR 스캐너 실행을 위한 ActivityResultLauncher 정의
    // ScanContract를 사용하여 ZXing 라이브러리의 스캔 결과를 받음
    val qrCodeLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        // 스캔 결과가 null이 아니고, 내용(contents)이 있다면 ViewModel로 전달
        if (result.contents != null) {
            viewModel.processQrScanResult(result.contents)
        } else {
            // 스캔 취소 또는 실패 시 처리 (예: 사용자에게 알림)
            viewModel.processQrScanResult(null) // 또는 빈 문자열 등으로 처리
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("차량 등록", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        if (registeredVehicleInfo.isNotEmpty()) {
            Text("등록된 차량: $registeredVehicleInfo")
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { viewModel.clearRegistration() }) { // 등록 정보 초기화 버튼 (예시)
                Text("다른 차량 등록하기")
            }
        } else {
            Button(onClick = {
                // QR 코드 스캐너 실행
                val options = ScanOptions()
                options.setPrompt("QR 코드를 스캔해주세요") // 스캔 화면에 표시될 메시지
                options.setBeepEnabled(true) // 스캔 시 비프음 활성화
                options.setOrientationLocked(false) // 화면 방향 고정 해제
                // options.setDesiredBarcodeFormats(ScanOptions.QR_CODE) // 특정 바코드 타입만 스캔 (필요시)
                qrCodeLauncher.launch(options) // 스캐너 실행
            }) {
                Text("QR 코드로 차량 등록하기")
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (qrScanResultValue.isNotEmpty()) {
                Text("스캔된 정보 (확인용): $qrScanResultValue")
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    viewModel.registerVehicle(qrScanResultValue)
                }) {
                    Text("이 정보로 차량 등록")
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
        MainScreen(viewModel = MainViewModel())
    }
}