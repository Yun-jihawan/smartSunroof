package com.example.bluelink

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.bluelink.model.ReservationDetails
import com.example.bluelink.ui.theme.BluelinkTheme
import com.example.bluelink.viewmodel.MainViewModel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// ... (MainActivity 클래스 및 MainScreen, MonitoringScreen, ControlScreen, VehicleRegistrationScreen은 이전과 거의 동일하게 유지)
// MainActivity 클래스
class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BluelinkTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        modifier = Modifier.padding(innerPadding),
                        viewModel = mainViewModel
                    )
                }
            }
        }
    }
}

// 메인 화면 (탭 구성)
@Composable
fun MainScreen(modifier: Modifier = Modifier, viewModel: MainViewModel) {
    val tabs = listOf("모니터링", "제어", "유지보수", "차량 등록")
    var selectedTabIndex by remember { mutableIntStateOf(0) } // mutableIntStateOf 사용

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
            2 -> MaintenanceScreen(viewModel) // MaintenanceScreen 호출
            3 -> VehicleRegistrationScreen(viewModel)
        }
    }
}


// --- MonitoringScreen, ControlScreen, VehicleRegistrationScreen은 이전 코드 유지 ---
// (MonitoringScreen의 Text 안내 메시지 등은 그대로 두거나 필요에 따라 수정)

@Composable
fun MonitoringScreen(viewModel: MainViewModel) {
    val vehicleState by viewModel.vehicleState.collectAsState()
    val environmentData by viewModel.environmentData.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()), // 스크롤 추가
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("차량 모니터링", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text("선루프 상태: ${vehicleState.sunroofStatus}")
        Text("에어컨 상태: ${vehicleState.acStatus}")
        Spacer(modifier = Modifier.height(8.dp))
        Text("실내 온도: ${String.format("%.1f", environmentData.indoorTemperature)}°C, 습도: ${String.format("%.1f", environmentData.indoorHumidity)}%")
        Text("실외 온도: ${String.format("%.1f", environmentData.outdoorTemperature)}°C, 습도: ${String.format("%.1f", environmentData.outdoorHumidity)}%")
        Text("공기질: ${environmentData.airQuality}, 미세먼지: ${environmentData.fineDust}")
        Spacer(modifier = Modifier.height(16.dp))
        Text("(데이터는 MQTT를 통해 실시간으로 업데이트 됩니다)")
    }
}

@Composable
fun ControlScreen(viewModel: MainViewModel) {
    // ... (이전과 동일)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("차량 원격 제어", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { viewModel.controlSunroof("open") }) { Text("선루프 열기") }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { viewModel.controlSunroof("close") }) { Text("선루프 닫기") }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { viewModel.controlAC("on") }) { Text("에어컨 켜기") }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { viewModel.controlAC("off") }) { Text("에어컨 끄기") }
    }
}


@Composable
fun VehicleRegistrationScreen(viewModel: MainViewModel) {
    // ... (이전과 동일, rememberLauncherForActivityResult 등)
    val registeredVehicleInfo by viewModel.registeredVehicleInfo.collectAsState()
    val qrScanResultValue by viewModel.qrScanResult.collectAsState()

    val qrCodeLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            viewModel.processQrScanResult(result.contents)
        } else {
            viewModel.processQrScanResult(null)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("차량 등록", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        if (registeredVehicleInfo.isNotEmpty()) {
            Text("등록된 차량: $registeredVehicleInfo")
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { viewModel.clearRegistration() }) {
                Text("다른 차량 등록하기")
            }
        } else {
            Button(onClick = {
                val options = ScanOptions().apply {
                    setPrompt("QR 코드를 스캔해주세요")
                    setBeepEnabled(true)
                    setOrientationLocked(false)
                }
                qrCodeLauncher.launch(options)
            }) {
                Text("QR 코드로 차량 등록하기")
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (qrScanResultValue.isNotEmpty()) {
                Text("스캔된 정보 (확인용): $qrScanResultValue")
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { viewModel.registerVehicle(qrScanResultValue) }) {
                    Text("이 정보로 차량 등록")
                }
            }
        }
    }
}


// --- 유지보수 화면 및 예약 폼 구현 ---
@Composable
fun MaintenanceScreen(viewModel: MainViewModel) {
    val maintenanceNotification by viewModel.maintenanceNotification.collectAsState()
    val sunroofUsage by viewModel.sunroofUsage.collectAsState() // 선루프 사용 데이터
    val showReservationForm by viewModel.showReservationForm.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()), // 내용이 길어질 수 있으므로 스크롤 추가
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("선루프 유지보수 정보", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        // SWR-MOB-17: 선루프 사용 데이터 표시 (예시)
        Text("모델: ${sunroofUsage.sunroofModel}")
        Text("총 사용 시간: ${sunroofUsage.totalOperatingHours} 시간")
        Text("총 개폐 횟수: ${sunroofUsage.openCloseCycles} 회")
        Spacer(modifier = Modifier.height(16.dp))

        if (maintenanceNotification.isNotEmpty()) {
            Text("🔔 알림", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
            Text(maintenanceNotification, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { viewModel.toggleReservationForm(true) }) { // 예약 폼 표시
                Text("서비스 센터 예약하기")
            }
        } else {
            Text("현재 특별한 유지보수 알림이 없습니다. 정기적인 점검을 권장합니다.")
        }

        // SWR-MOB-19, SWR-MOB-20: 예약 폼 (조건부 표시)
        if (showReservationForm) {
            Spacer(modifier = Modifier.height(24.dp))
            Divider() // 구분선
            Spacer(modifier = Modifier.height(24.dp))
            ReservationForm(viewModel = viewModel)
        }
    }
}

// SWR-MOB-19, SWR-MOB-20: 서비스 예약 폼 Composable
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservationForm(viewModel: MainViewModel) {
    val reservationDetails by viewModel.reservationDetails.collectAsState()
    val reservationStatusMessage by viewModel.reservationStatusMessage.collectAsState()
    val availableServiceCenters = viewModel.availableServiceCenters

    var serviceCenterExpanded by remember { mutableStateOf(false) }

    // 날짜 선택을 위한 상태 (간단한 예시로 TextField 사용)
    // 실제 앱에서는 DatePickerDialog 등을 사용하는 것이 좋음
    val calendar = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("서비스 예약", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        // 날짜 입력 (TextField 사용, 실제로는 DatePicker 사용 권장)
        OutlinedTextField(
            value = reservationDetails.date,
            onValueChange = { viewModel.updateReservationDate(it) },
            label = { Text("예약 날짜 (YYYY-MM-DD)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )
        Spacer(modifier = Modifier.height(8.dp))

        // 시간 입력 (TextField 사용, 실제로는 TimePicker 사용 권장)
        OutlinedTextField(
            value = reservationDetails.time,
            onValueChange = { viewModel.updateReservationTime(it) },
            label = { Text("예약 시간 (HH:MM)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )
        Spacer(modifier = Modifier.height(8.dp))

        // SWR-MOB-20: 서비스 센터 선택 (DropdownMenu 사용)
        Box {
            OutlinedTextField(
                value = reservationDetails.serviceCenter.ifEmpty { "서비스 센터 선택" },
                onValueChange = { }, // 직접 수정 방지
                label = { Text("서비스 센터") },
                readOnly = true,
                trailingIcon = { Icon(Icons.Filled.ArrowDropDown, "선택", Modifier.clickable { serviceCenterExpanded = true }) },
                modifier = Modifier.fillMaxWidth()
            )
            DropdownMenu(
                expanded = serviceCenterExpanded,
                onDismissRequest = { serviceCenterExpanded = false },
                modifier = Modifier.fillMaxWidth(0.8f) // 너비 조정
            ) {
                availableServiceCenters.forEach { center ->
                    DropdownMenuItem(
                        text = { Text(center) },
                        onClick = {
                            viewModel.updateSelectedServiceCenter(center)
                            serviceCenterExpanded = false
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        // 요청 사항 입력
        OutlinedTextField(
            value = reservationDetails.requestDetails,
            onValueChange = { viewModel.updateServiceRequestDetails(it) },
            label = { Text("추가 요청 사항 (선택)") },
            modifier = Modifier.height(100.dp).fillMaxWidth(), // 여러 줄 입력 가능하도록 높이 조절
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done)
        )
        Spacer(modifier = Modifier.height(16.dp))

        // 예약 상태 메시지 표시
        if (reservationStatusMessage.isNotEmpty()) {
            Text(
                reservationStatusMessage,
                color = if (reservationStatusMessage.contains("완료")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Row {
            Button(
                onClick = { viewModel.submitReservation() },
                modifier = Modifier.weight(1f)
            ) {
                Text("예약 요청")
            }
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(
                onClick = { viewModel.toggleReservationForm(false) }, // 폼 닫기
                modifier = Modifier.weight(1f)
            ) {
                Text("취소")
            }
        }
    }
}


// --- Preview 설정 ---
@Preview(showBackground = true, name = "Maintenance Screen - No Notification")
@Composable
fun MaintenanceScreenPreviewNoNotification() {
    val previewViewModel = MainViewModel() // Preview용 ViewModel
    // 알림 없는 상태로 설정
    // previewViewModel._maintenanceNotification.value = "" // 직접 접근은 어렵지만, 이런 상태를 가정
    BluelinkTheme {
        MaintenanceScreen(viewModel = previewViewModel)
    }
}

@Preview(showBackground = true, name = "Maintenance Screen - With Notification")
@Composable
fun MaintenanceScreenPreviewWithNotification() {
    val previewViewModel = MainViewModel()
    // 알림 있는 상태로 설정 (실제로는 ViewModel 내부 로직에 따라 결정됨)
    // previewViewModel._maintenanceNotification.value = "선루프 점검이 필요합니다."
    // previewViewModel._sunroofUsage.value = SunroofUsageData("Preview Model", 1200, 5500)
    // previewViewModel.checkMaintenance() // Preview에서 이 함수를 직접 호출하여 상태 변경 시도
    BluelinkTheme {
        Column { // Column으로 감싸야 Preview에서 정상적으로 보일 수 있음
            MaintenanceScreen(viewModel = previewViewModel)
        }
    }
}

@Preview(showBackground = true, name = "Reservation Form Preview")
@Composable
fun ReservationFormPreview() {
    BluelinkTheme {
        // ReservationForm은 Column 내부에 있으므로 Column으로 감싸서 Preview
        Column(modifier = Modifier.padding(16.dp)) {
            ReservationForm(viewModel = MainViewModel())
        }
    }
}

// DefaultPreview는 MainScreen을 사용하므로 유지
@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    BluelinkTheme {
        MainScreen(viewModel = MainViewModel())
    }
}