package com.example.bluelink

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Toast
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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Schedule // 아이콘 import 추가
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.bluelink.ui.theme.BluelinkTheme
import com.example.bluelink.viewmodel.MainViewModel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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

@Composable
fun MainScreen(modifier: Modifier = Modifier, viewModel: MainViewModel) {
    val tabs = listOf("모니터링", "제어", "유지보수", "차량 등록")
    var selectedTabIndex by remember { mutableIntStateOf(0) }

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
            3 -> VehicleRegistrationScreen(viewModel)
        }
    }
}

@Composable
fun MonitoringScreen(viewModel: MainViewModel) {
    val vehicleState by viewModel.vehicleState.collectAsState()
    val environmentData by viewModel.environmentData.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
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
    val vehicleState by viewModel.vehicleState.collectAsState()
    // collectAsState()를 사용하여 StateFlow의 값을 가져옵니다.
    val sunroofCommandInProgress by viewModel.isSunroofCommandInProgress.collectAsState()
    val acCommandInProgress by viewModel.isAcCommandInProgress.collectAsState()
    val context = LocalContext.current

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

        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = {
                    viewModel.controlSunroof("open")
                    Toast.makeText(context, "선루프 열기 명령 전송", Toast.LENGTH_SHORT).show()
                },
                // .value를 사용하여 Boolean 값을 가져와서 ! 연산자 사용
                enabled = !sunroofCommandInProgress && !acCommandInProgress
            ) {
                Text("선루프 열기")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    viewModel.controlSunroof("close")
                    Toast.makeText(context, "선루프 닫기 명령 전송", Toast.LENGTH_SHORT).show()
                },
                enabled = !sunroofCommandInProgress && !acCommandInProgress
            ) {
                Text("선루프 닫기")
            }
        }
        if (sunroofCommandInProgress) {
            Spacer(modifier = Modifier.height(8.dp))
            CircularProgressIndicator()
            Text("선루프 제어 중...")
        }
        Text("현재 선루프 상태: ${vehicleState.sunroofStatus}", style = MaterialTheme.typography.bodyMedium)

        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = {
                    viewModel.controlAC("on")
                    Toast.makeText(context, "에어컨 켜기 명령 전송", Toast.LENGTH_SHORT).show()
                },
                enabled = !sunroofCommandInProgress && !acCommandInProgress
            ) {
                Text("에어컨 켜기")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    viewModel.controlAC("off")
                    Toast.makeText(context, "에어컨 끄기 명령 전송", Toast.LENGTH_SHORT).show()
                },
                enabled = !sunroofCommandInProgress && !acCommandInProgress
            ) {
                Text("에어컨 끄기")
            }
        }
        if (acCommandInProgress) {
            Spacer(modifier = Modifier.height(8.dp))
            CircularProgressIndicator()
            Text("에어컨 제어 중...")
        }
        Text("현재 에어컨 상태: ${vehicleState.acStatus}", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun VehicleRegistrationScreen(viewModel: MainViewModel) {
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

@Composable
fun MaintenanceScreen(viewModel: MainViewModel) {
    val maintenanceNotification by viewModel.maintenanceNotification.collectAsState()
    val sunroofUsage by viewModel.sunroofUsage.collectAsState()
    val showReservationForm by viewModel.showReservationForm.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("선루프 유지보수 정보", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        Text("모델: ${sunroofUsage.sunroofModel}")
        Text("총 사용 시간: ${sunroofUsage.totalOperatingHours} 시간")
        Text("총 개폐 횟수: ${sunroofUsage.openCloseCycles} 회")
        Spacer(modifier = Modifier.height(16.dp))

        if (maintenanceNotification.isNotEmpty()) {
            Text("🔔 알림", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
            Text(maintenanceNotification, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { viewModel.toggleReservationForm(true) }) {
                Text("서비스 센터 예약하기")
            }
        } else {
            Text("현재 특별한 유지보수 알림이 없습니다. 정기적인 점검을 권장합니다.")
        }

        if (showReservationForm) {
            Spacer(modifier = Modifier.height(24.dp))
            Divider()
            Spacer(modifier = Modifier.height(24.dp))
            ReservationForm(viewModel = viewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservationForm(viewModel: MainViewModel) {
    val reservationDetails by viewModel.reservationDetails.collectAsState()
    val reservationStatusMessage by viewModel.reservationStatusMessage.collectAsState()
    val availableServiceCenters = viewModel.availableServiceCenters

    var serviceCenterExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    val datePickerDialog = DatePickerDialog(
        context,
        { _, selectedYear, selectedMonth, selectedDayOfMonth ->
            viewModel.updateReservationDate("$selectedYear-${selectedMonth + 1}-${selectedDayOfMonth}")
        }, year, month, day
    )
    datePickerDialog.datePicker.minDate = calendar.timeInMillis

    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val minute = calendar.get(Calendar.MINUTE)
    val timePickerDialog = TimePickerDialog(
        context,
        { _, selectedHour, selectedMinute ->
            viewModel.updateReservationTime(String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute))
        }, hour, minute, true
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("서비스 예약", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = reservationDetails.date,
            onValueChange = { },
            label = { Text("예약 날짜 (YYYY-MM-DD)") },
            readOnly = true,
            trailingIcon = {
                Icon(
                    Icons.Filled.DateRange,
                    contentDescription = "날짜 선택",
                    modifier = Modifier.clickable { datePickerDialog.show() }
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = reservationDetails.time,
            onValueChange = { },
            label = { Text("예약 시간 (HH:MM)") },
            readOnly = true,
            trailingIcon = {
                Icon(
                    Icons.Filled.Schedule, // 수정: Schedule 아이콘 사용
                    contentDescription = "시간 선택",
                    modifier = Modifier.clickable { timePickerDialog.show() }
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        Box {
            OutlinedTextField(
                value = reservationDetails.serviceCenter.ifEmpty { "서비스 센터 선택" },
                onValueChange = { },
                label = { Text("서비스 센터") },
                readOnly = true,
                trailingIcon = { Icon(Icons.Filled.ArrowDropDown, "선택", Modifier.clickable { serviceCenterExpanded = true }) },
                modifier = Modifier.fillMaxWidth()
            )
            DropdownMenu(
                expanded = serviceCenterExpanded,
                onDismissRequest = { serviceCenterExpanded = false },
                modifier = Modifier.fillMaxWidth(0.8f) // Use fillMaxWidth with a fraction
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

        OutlinedTextField(
            value = reservationDetails.requestDetails,
            onValueChange = { viewModel.updateServiceRequestDetails(it) },
            label = { Text("추가 요청 사항 (선택)") },
            modifier = Modifier.height(100.dp).fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done)
        )
        Spacer(modifier = Modifier.height(16.dp))

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
                onClick = { viewModel.toggleReservationForm(false) },
                modifier = Modifier.weight(1f)
            ) {
                Text("취소")
            }
        }
    }
}

@Preview(showBackground = true, name = "Maintenance Screen - No Notification")
@Composable
fun MaintenanceScreenPreviewNoNotification() {
    BluelinkTheme {
        MaintenanceScreen(viewModel = MainViewModel())
    }
}

@Preview(showBackground = true, name = "Maintenance Screen - With Notification")
@Composable
fun MaintenanceScreenPreviewWithNotification() {
    val previewViewModel = MainViewModel()
    BluelinkTheme {
        Column {
            MaintenanceScreen(viewModel = previewViewModel)
        }
    }
}

@Preview(showBackground = true, name = "Reservation Form Preview")
@Composable
fun ReservationFormPreview() {
    BluelinkTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            ReservationForm(viewModel = MainViewModel())
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    BluelinkTheme {
        MainScreen(viewModel = MainViewModel())
    }
}