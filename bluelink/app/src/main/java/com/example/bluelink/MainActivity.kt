package com.example.bluelink

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CloudOff // 연결 끊김 아이콘
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.SignalWifi4Bar // 연결됨 아이콘
import androidx.compose.material.icons.filled.Sync // 연결 중 아이콘
import androidx.compose.material.icons.filled.Error // 오류 아이콘
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.bluelink.mqtt.MqttConnectionState // MqttConnectionState import
import com.example.bluelink.ui.theme.BluelinkTheme
import com.example.bluelink.ui.theme.StatusBad
import com.example.bluelink.ui.theme.StatusGood
import com.example.bluelink.ui.theme.StatusNormal
import com.example.bluelink.ui.theme.StatusUnknown
import com.example.bluelink.viewmodel.MainViewModel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BluelinkTheme {
                // Snackbar를 사용하기 위해 ScaffoldState 준비
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope() // Snackbar를 보여주기 위한 코루틴 스코프

                // ViewModel의 오류 이벤트를 구독하여 Snackbar로 표시
                LaunchedEffect(key1 = mainViewModel.mqttErrorEvent) {
                    mainViewModel.mqttErrorEvent.collect { errorMessage ->
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = errorMessage,
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) } // SnackbarHost 추가
                ) { innerPadding ->
                    MainScreen(
                        modifier = Modifier.padding(innerPadding),
                        viewModel = mainViewModel
                    )
                }
            }
        }
    }
}

// MQTT 연결 상태 표시 Composable
@Composable
fun MqttStatusIndicator(viewModel: MainViewModel) {
    val connectionState by viewModel.mqttConnectionState.collectAsState()
    val statusText: String
    val indicatorColor: Color
    val icon: androidx.compose.ui.graphics.vector.ImageVector

    when (connectionState) {
        MqttConnectionState.IDLE -> {
            statusText = "MQTT 연결 대기"
            indicatorColor = Color.Gray
            icon = Icons.Filled.CloudOff
        }
        MqttConnectionState.CONNECTING -> {
            statusText = "MQTT 연결 중..."
            indicatorColor = MaterialTheme.colorScheme.primary // 테마 색상 사용
            icon = Icons.Filled.Sync
        }
        MqttConnectionState.CONNECTED -> {
            statusText = "MQTT 연결됨"
            indicatorColor = StatusGood // 초록색 계열
            icon = Icons.Filled.SignalWifi4Bar
        }
        MqttConnectionState.DISCONNECTED -> {
            statusText = "MQTT 연결 끊김"
            indicatorColor = Color.DarkGray
            icon = Icons.Filled.CloudOff
        }
        MqttConnectionState.ERROR -> {
            statusText = "MQTT 연결 오류"
            indicatorColor = StatusBad // 빨간색 계열
            icon = Icons.Filled.Error
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(indicatorColor.copy(alpha = 0.1f)) // 배경색 약간 투명하게
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = "MQTT Status Icon", tint = indicatorColor, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(statusText, color = indicatorColor, style = MaterialTheme.typography.bodySmall)
        if (connectionState == MqttConnectionState.ERROR || connectionState == MqttConnectionState.DISCONNECTED) {
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(onClick = { viewModel.attemptMqttReconnect() },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("재연결", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}


@Composable
fun MainScreen(modifier: Modifier = Modifier, viewModel: MainViewModel) {
    val tabs = listOf("모니터링", "제어", "유지보수", "차량 등록")
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    Column(modifier = modifier.fillMaxSize()) {
        // MQTT 연결 상태 표시줄 추가
        MqttStatusIndicator(viewModel = viewModel)

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

// MonitoringScreen, ControlScreen, VehicleRegistrationScreen, MaintenanceScreen, ReservationForm, Preview 함수들은
// 이전 답변에서 제공한 최종본과 동일하게 유지합니다.
// (MainActivity.kt 파일의 나머지 부분은 이전 답변의 최종본을 참고하여 그대로 두시면 됩니다.)
@Composable
fun MonitoringScreen(viewModel: MainViewModel) {
    val vehicleState by viewModel.vehicleState.collectAsState()
    val environmentData by viewModel.environmentData.collectAsState()

    @Composable
    fun getStatusColor(status: String): Color {
        return when (status.lowercase()) {
            "좋음", "낮음" -> StatusGood
            "보통" -> StatusNormal
            "나쁨", "높음", "매우 높음" -> StatusBad
            else -> StatusUnknown
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "차량 모니터링",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(16.dp))

        @Composable
        fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, valueColor: Color = LocalContentColor.current) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = label, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("$label: ", fontWeight = FontWeight.Bold)
                Text(value, color = valueColor)
            }
        }

        InfoRow(
            icon = Icons.Filled.WbSunny,
            label = "선루프 상태",
            value = vehicleState.sunroofStatus
        )
        InfoRow(
            icon = Icons.Filled.AcUnit,
            label = "에어컨 상태",
            value = vehicleState.acStatus
        )

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        InfoRow(
            icon = Icons.Filled.Thermostat,
            label = "실내 온도",
            value = "${String.format("%.1f", environmentData.indoorTemperature)}°C"
        )
        InfoRow(
            icon = Icons.Filled.Opacity,
            label = "실내 습도",
            value = "${String.format("%.1f", environmentData.indoorHumidity)}%"
        )

        Spacer(modifier = Modifier.height(8.dp))

        InfoRow(
            icon = Icons.Filled.Thermostat,
            label = "실외 온도",
            value = "${String.format("%.1f", environmentData.outdoorTemperature)}°C"
        )
        InfoRow(
            icon = Icons.Filled.Opacity,
            label = "실외 습도",
            value = "${String.format("%.1f", environmentData.outdoorHumidity)}%"
        )

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        InfoRow(
            icon = Icons.Filled.Cloud,
            label = "공기질",
            value = environmentData.airQuality,
            valueColor = getStatusColor(environmentData.airQuality)
        )
        InfoRow(
            icon = Icons.Filled.Cloud,
            label = "미세먼지",
            value = environmentData.fineDust,
            valueColor = getStatusColor(environmentData.fineDust)
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            "(데이터는 MQTT를 통해 실시간으로 업데이트 됩니다)",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
fun ControlScreen(viewModel: MainViewModel) {
    val vehicleState by viewModel.vehicleState.collectAsState()
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
                    // Toast.makeText(context, "선루프 열기 명령 전송", Toast.LENGTH_SHORT).show() // ViewModel에서 이벤트로 처리
                },
                enabled = !sunroofCommandInProgress && !acCommandInProgress
            ) {
                Text("선루프 열기")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    viewModel.controlSunroof("close")
                    // Toast.makeText(context, "선루프 닫기 명령 전송", Toast.LENGTH_SHORT).show()
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
                    // Toast.makeText(context, "에어컨 켜기 명령 전송", Toast.LENGTH_SHORT).show()
                },
                enabled = !sunroofCommandInProgress && !acCommandInProgress
            ) {
                Text("에어컨 켜기")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    viewModel.controlAC("off")
                    // Toast.makeText(context, "에어컨 끄기 명령 전송", Toast.LENGTH_SHORT).show()
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
                    Icons.Filled.AccessTime,
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
                modifier = Modifier.fillMaxWidth(0.8f)
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

@Preview(showBackground = true, name = "Monitoring Screen Preview")
@Composable
fun MonitoringScreenPreview() {
    BluelinkTheme {
        MonitoringScreen(viewModel = MainViewModel())
    }
}

@Preview(showBackground = true, name = "Control Screen Preview")
@Composable
fun ControlScreenPreview() {
    BluelinkTheme {
        ControlScreen(viewModel = MainViewModel())
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
    // previewViewModel.forceMaintenanceNotificationForPreview()
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