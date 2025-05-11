package com.example.bluelink

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
//import android.widget.Toast // ViewModel에서 이벤트로 처리
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility // AnimatedVisibility import
import androidx.compose.animation.ExperimentalAnimationApi // AnimatedVisibility가 실험적 API일 경우 필요 (버전에 따라 다름)
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Autorenew // 자동 모드 아이콘 (예시)
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.SignalWifi4Bar
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.material3.Switch // Switch 컴포넌트 import
import androidx.compose.material3.SwitchDefaults // Switch 색상 커스터마이징을 위해
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset // 인디케이터 커스터마이징을 위해
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
import androidx.compose.ui.unit.sp
import com.example.bluelink.mqtt.MqttConnectionState
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
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()

                LaunchedEffect(key1 = Unit) {
                    mainViewModel.mqttErrorEvent.collect { errorMessage ->
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = errorMessage,
                                duration = SnackbarDuration.Long
                            )
                        }
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) }
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

@Composable
fun MqttStatusIndicator(viewModel: MainViewModel) {
    val connectionState by viewModel.mqttConnectionState.collectAsState()
    val statusText: String
    val indicatorColor: Color
    val contentColor: Color
    val icon: androidx.compose.ui.graphics.vector.ImageVector

    when (connectionState) {
        MqttConnectionState.IDLE -> {
            statusText = "MQTT 연결 대기"
            indicatorColor = MaterialTheme.colorScheme.surfaceVariant
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            icon = Icons.Filled.CloudOff
        }
        MqttConnectionState.CONNECTING -> {
            statusText = "MQTT 연결 중..."
            indicatorColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            icon = Icons.Filled.Sync
        }
        MqttConnectionState.CONNECTED -> {
            statusText = "MQTT 연결됨"
            indicatorColor = StatusGood.copy(alpha = 0.7f)
            contentColor = Color.White // 테마에 따라 MaterialTheme.colorScheme.onPrimaryContainer 등 사용
            icon = Icons.Filled.SignalWifi4Bar
        }
        MqttConnectionState.DISCONNECTED -> {
            statusText = "MQTT 연결 끊김"
            indicatorColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            icon = Icons.Filled.CloudOff
        }
        MqttConnectionState.ERROR -> {
            statusText = "MQTT 연결 오류"
            indicatorColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
            contentColor = MaterialTheme.colorScheme.onErrorContainer
            icon = Icons.Filled.Error
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(indicatorColor)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = "MQTT Status Icon", tint = contentColor, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(statusText, color = contentColor, style = MaterialTheme.typography.labelMedium)
        if (connectionState == MqttConnectionState.ERROR || connectionState == MqttConnectionState.DISCONNECTED) {
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = { viewModel.attemptMqttReconnect() },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = contentColor.copy(alpha = 0.2f),
                    contentColor = contentColor
                ),
                shape = MaterialTheme.shapes.small
            ) {
                Text("재연결", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
            }
        }
    }
}


@Composable
fun MainScreen(modifier: Modifier = Modifier, viewModel: MainViewModel) {
    val tabs = listOf("모니터링", "제어", "유지보수", "차량 등록")
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        MqttStatusIndicator(viewModel = viewModel)

        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                // TabRowDefaults.Indicator를 사용하여 인디케이터 스타일 지정
                if (selectedTabIndex < tabPositions.size) { // IndexOutOfBounds 방지
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]), // 올바른 함수 사용
                        height = 3.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            title,
                            style = MaterialTheme.typography.labelLarge,
                            fontSize = 13.sp
                        )
                    },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTabIndex) {
                0 -> MonitoringScreen(viewModel)
                1 -> ControlScreen(viewModel)
                2 -> MaintenanceScreen(viewModel)
                3 -> VehicleRegistrationScreen(viewModel)
            }
        }
    }
}

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
            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "차량 모니터링",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        @Composable
        fun InfoRow(
            icon: androidx.compose.ui.graphics.vector.ImageVector,
            label: String,
            value: String,
            valueColor: Color = MaterialTheme.colorScheme.onSurface
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = valueColor)
                }
            }
        }

        val cardColors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
        val cardElevation = CardDefaults.cardElevation(defaultElevation = 2.dp)

        Card(modifier = Modifier.fillMaxWidth(), colors = cardColors, elevation = cardElevation) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                InfoRow(Icons.Filled.WbSunny, "선루프 상태", vehicleState.sunroofStatus)
                InfoRow(Icons.Filled.AcUnit, "에어컨 상태", vehicleState.acStatus)
            }
        }

        Card(modifier = Modifier.fillMaxWidth(), colors = cardColors, elevation = cardElevation) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                InfoRow(Icons.Filled.Thermostat, "실내 온도", "${String.format("%.1f", environmentData.indoorTemperature)}°C")
                InfoRow(Icons.Filled.Opacity, "실내 습도", "${String.format("%.1f", environmentData.indoorHumidity)}%")
                InfoRow(Icons.Filled.Thermostat, "실외 온도", "${String.format("%.1f", environmentData.outdoorTemperature)}°C")
                InfoRow(Icons.Filled.Opacity, "실외 습도", "${String.format("%.1f", environmentData.outdoorHumidity)}%")
            }
        }

        Card(modifier = Modifier.fillMaxWidth(), colors = cardColors, elevation = cardElevation) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                InfoRow(Icons.Filled.Cloud, "공기질", environmentData.airQuality, getStatusColor(environmentData.airQuality))
                InfoRow(Icons.Filled.Cloud, "미세먼지", environmentData.fineDust, getStatusColor(environmentData.fineDust))
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        Text(
            "(데이터는 MQTT를 통해 실시간으로 업데이트 됩니다)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp)
        )
    }
}

@Composable
fun ControlScreen(viewModel: MainViewModel) {
    val vehicleState by viewModel.vehicleState.collectAsState()
    val sunroofCommandInProgress by viewModel.isSunroofCommandInProgress.collectAsState()
    val acCommandInProgress by viewModel.isAcCommandInProgress.collectAsState()
    // 모드 변경 진행 상태 구독 추가
    val sunroofModeChangeInProgress by viewModel.isSunroofModeChangeInProgress.collectAsState()
    val acModeChangeInProgress by viewModel.isAcModeChangeInProgress.collectAsState()


    val isSunroofAutoMode = vehicleState.sunroofMode == "auto"
    val isAcAutoMode = vehicleState.acMode == "auto"

    // 어떤 명령이라도 진행 중인지 확인하는 플래그
    val anyCommandInProgress = sunroofCommandInProgress || acCommandInProgress || sunroofModeChangeInProgress || acModeChangeInProgress

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("차량 원격 제어", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)

        val cardColors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
        val cardElevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        val buttonShape = MaterialTheme.shapes.medium
        val primaryButtonColors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), // 비활성화 시 버튼 배경
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)   // 비활성화 시 버튼 내용
        )
        val secondaryButtonColors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        )
        val switchColors = SwitchDefaults.colors(
            checkedThumbColor = MaterialTheme.colorScheme.primary,
            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.54f),
            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledCheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            disabledCheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            disabledUncheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            disabledUncheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        )


        // --- 선루프 제어 섹션 ---
        Card(modifier = Modifier.fillMaxWidth(), colors = cardColors, elevation = cardElevation) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("선루프 제어", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("자동 모드", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    if (sunroofModeChangeInProgress) { // 모드 변경 중 로딩 표시
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                    } else {
                        Switch(
                            checked = isSunroofAutoMode,
                            onCheckedChange = { checked ->
                                viewModel.setSunroofMode(if (checked) "auto" else "manual")
                            },
                            thumbContent = if (isSunroofAutoMode) { { Icon(imageVector = Icons.Filled.Autorenew, contentDescription = "자동 모드 활성", tint = MaterialTheme.colorScheme.onPrimary) } } else null,
                            colors = switchColors,
                            enabled = !anyCommandInProgress || sunroofModeChangeInProgress // 자기 자신의 모드 변경 중이거나, 다른 어떤 명령도 진행 중이지 않을 때만 활성화 (좀 더 세밀한 제어 가능)
                            // 또는 간단하게 enabled = !anyCommandInProgress
                        )
                    }
                }
                Text("현재 선루프 모드: ${if(isSunroofAutoMode) "자동" else "수동"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = { viewModel.controlSunroof("open") },
                        enabled = !isSunroofAutoMode && !anyCommandInProgress, // 자동 모드 아니고, 어떤 명령도 진행 중이지 않을 때
                        shape = buttonShape, colors = primaryButtonColors
                    ) { Text("선루프 열기") }
                    Button(
                        onClick = { viewModel.controlSunroof("close") },
                        enabled = !isSunroofAutoMode && !anyCommandInProgress,
                        shape = buttonShape, colors = secondaryButtonColors
                    ) { Text("선루프 닫기") }
                }
                if (sunroofCommandInProgress) { // 수동 제어 진행 중 로딩 표시
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
                    Text("선루프 제어 중...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                }
                Text("현재 선루프 상태: ${vehicleState.sunroofStatus}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 8.dp))
            }
        }

        // --- 에어컨 제어 섹션 ---
        Card(modifier = Modifier.fillMaxWidth(), colors = cardColors, elevation = cardElevation) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("에어컨 제어", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("자동 모드", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    if (acModeChangeInProgress) { // 모드 변경 중 로딩 표시
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                    } else {
                        Switch(
                            checked = isAcAutoMode,
                            onCheckedChange = { checked ->
                                viewModel.setAcMode(if (checked) "auto" else "manual")
                            },
                            thumbContent = if (isAcAutoMode) { { Icon(imageVector = Icons.Filled.Autorenew, contentDescription = "자동 모드 활성", tint = MaterialTheme.colorScheme.onPrimary) } } else null,
                            colors = switchColors,
                            enabled = !anyCommandInProgress || acModeChangeInProgress
                        )
                    }
                }
                Text("현재 에어컨 모드: ${if(isAcAutoMode) "자동" else "수동"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = { viewModel.controlAC("on") },
                        enabled = !isAcAutoMode && !anyCommandInProgress,
                        shape = buttonShape, colors = primaryButtonColors
                    ) { Text("에어컨 켜기") }
                    Button(
                        onClick = { viewModel.controlAC("off") },
                        enabled = !isAcAutoMode && !anyCommandInProgress,
                        shape = buttonShape, colors = secondaryButtonColors
                    ) { Text("에어컨 끄기") }
                }
                if (acCommandInProgress) { // 수동 제어 진행 중 로딩 표시
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
                    Text("에어컨 제어 중...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                }
                Text("현재 에어컨 상태: ${vehicleState.acStatus}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

@Composable
fun VehicleRegistrationScreen(viewModel: MainViewModel) {
    val registeredVehicleInfo by viewModel.registeredVehicleInfo.collectAsState()
    val qrScanResultValue by viewModel.qrScanResult.collectAsState()

    val qrCodeLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (result.contents != null) viewModel.processQrScanResult(result.contents)
        else viewModel.processQrScanResult(null)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("차량 등록", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(32.dp))

        if (registeredVehicleInfo.isNotEmpty()) {
            Text("등록된 차량:", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(8.dp))
            Text(registeredVehicleInfo, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = { viewModel.clearRegistration() }, shape = MaterialTheme.shapes.medium, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary) ) {
                Text("다른 차량 등록하기", color = MaterialTheme.colorScheme.onSecondary)
            }
        } else {
            Button(
                onClick = {
                    val options = ScanOptions().apply {
                        setPrompt("QR 코드를 스캔해주세요"); setBeepEnabled(true); setOrientationLocked(false)
                    }
                    qrCodeLauncher.launch(options)
                },
                modifier = Modifier.fillMaxWidth(0.85f).height(56.dp),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) { Text("QR 코드로 차량 등록하기", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimary) }

            if (qrScanResultValue.isNotEmpty()) {
                Spacer(modifier = Modifier.height(32.dp))
                Text("스캔된 정보:", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(8.dp))
                Text(qrScanResultValue, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { viewModel.registerVehicle(qrScanResultValue) }, shape = MaterialTheme.shapes.medium, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary) ) {
                    Text("이 정보로 차량 등록", color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

// AnimatedVisibility 사용을 위해 OptIn 어노테이션 추가 (필요시)
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MaintenanceScreen(viewModel: MainViewModel) {
    val maintenanceNotification by viewModel.maintenanceNotification.collectAsState()
    val sunroofUsage by viewModel.sunroofUsage.collectAsState()
    val showReservationForm by viewModel.showReservationForm.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("선루프 유지보수 정보", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)

        val cardColors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
        val cardElevation = CardDefaults.cardElevation(defaultElevation = 2.dp)

        Card(modifier = Modifier.fillMaxWidth(), colors = cardColors, elevation = cardElevation) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("모델: ${sunroofUsage.sunroofModel}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                Text("총 사용 시간: ${sunroofUsage.totalOperatingHours} 시간", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                Text("총 개폐 횟수: ${sunroofUsage.openCloseCycles} 회", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            }
        }

        if (maintenanceNotification.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                elevation = cardElevation
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Error, contentDescription = "알림", tint = MaterialTheme.colorScheme.onErrorContainer)
                        Text("알림", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                    Text(maintenanceNotification, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onErrorContainer)
                    Button(
                        onClick = { viewModel.toggleReservationForm(true) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError),
                        shape = MaterialTheme.shapes.medium
                    ) { Text("서비스 센터 예약하기") }
                }
            }
        } else {
            Text("현재 특별한 유지보수 알림이 없습니다. 정기적인 점검을 권장합니다.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // AnimatedVisibility로 예약 폼 표시/숨김에 애니메이션 적용
        AnimatedVisibility(visible = showReservationForm) {
            Column { // AnimatedVisibility 내에는 단일 Composable 자식만 허용되므로 Column으로 감싸기
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(16.dp))
                ReservationForm(viewModel = viewModel)
            }
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
    val datePickerDialog = DatePickerDialog(context, { _, selectedYear, selectedMonth, selectedDayOfMonth ->
        viewModel.updateReservationDate("$selectedYear-${selectedMonth + 1}-${selectedDayOfMonth}")
    }, year, month, day).apply { datePicker.minDate = calendar.timeInMillis }

    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val minute = calendar.get(Calendar.MINUTE)
    val timePickerDialog = TimePickerDialog(context, { _, selectedHour, selectedMinute ->
        viewModel.updateReservationTime(String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute))
    }, hour, minute, true)

    val textFieldColors = TextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, disabledContainerColor = Color.Transparent,
        focusedIndicatorColor = MaterialTheme.colorScheme.primary, unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
        disabledIndicatorColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
        focusedLabelColor = MaterialTheme.colorScheme.primary, unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        cursorColor = MaterialTheme.colorScheme.primary,
        focusedTrailingIconColor = MaterialTheme.colorScheme.primary, unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
    val textFieldShape = MaterialTheme.shapes.medium

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("서비스 예약", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)

        OutlinedTextField(value = reservationDetails.date, onValueChange = { }, label = { Text("예약 날짜 (YYYY-MM-DD)") }, readOnly = true,
            trailingIcon = { Icon(Icons.Filled.DateRange, "날짜 선택", Modifier.clickable { datePickerDialog.show() }) },
            modifier = Modifier.fillMaxWidth(), shape = textFieldShape, colors = textFieldColors)

        OutlinedTextField(value = reservationDetails.time, onValueChange = { }, label = { Text("예약 시간 (HH:MM)") }, readOnly = true,
            trailingIcon = { Icon(Icons.Filled.AccessTime, "시간 선택", Modifier.clickable { timePickerDialog.show() }) },
            modifier = Modifier.fillMaxWidth(), shape = textFieldShape, colors = textFieldColors)

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(value = reservationDetails.serviceCenter.ifEmpty { "서비스 센터 선택" }, onValueChange = { }, label = { Text("서비스 센터") }, readOnly = true,
                trailingIcon = { Icon(Icons.Filled.ArrowDropDown, "선택", Modifier.clickable { serviceCenterExpanded = true }) },
                modifier = Modifier.fillMaxWidth(), shape = textFieldShape, colors = textFieldColors)
            DropdownMenu(expanded = serviceCenterExpanded, onDismissRequest = { serviceCenterExpanded = false },
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp))) {
                availableServiceCenters.forEach { center ->
                    DropdownMenuItem(text = { Text(center, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface) },
                        onClick = { viewModel.updateSelectedServiceCenter(center); serviceCenterExpanded = false })
                }
            }
        }

        OutlinedTextField(value = reservationDetails.requestDetails, onValueChange = { viewModel.updateServiceRequestDetails(it) }, label = { Text("추가 요청 사항 (선택)") },
            modifier = Modifier.height(120.dp).fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done), shape = textFieldShape, colors = textFieldColors)

        if (reservationStatusMessage.isNotEmpty()) {
            Text(reservationStatusMessage,
                color = if (reservationStatusMessage.contains("완료")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
        }

        Row(modifier = Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) { // 버튼 간 간격 추가
            Button(onClick = { viewModel.submitReservation() }, modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.medium ) { Text("예약 요청") }
            TextButton(onClick = { viewModel.toggleReservationForm(false) }, modifier = Modifier.weight(1f) ) { Text("취소") }
        }
    }
}

@Preview(showBackground = true, name = "Monitoring Screen Preview")
@Composable
fun MonitoringScreenPreview() { BluelinkTheme { MonitoringScreen(viewModel = MainViewModel()) } }

@Preview(showBackground = true, name = "Control Screen Preview")
@Composable
fun ControlScreenPreview() { BluelinkTheme { ControlScreen(viewModel = MainViewModel()) } }

@Preview(showBackground = true, name = "Maintenance Screen Preview")
@Composable
fun MaintenanceScreenPreview() { BluelinkTheme { MaintenanceScreen(viewModel = MainViewModel()) } }

@Preview(showBackground = true, name = "Reservation Form Preview")
@Composable
fun ReservationFormPreview() { BluelinkTheme { Surface(modifier = Modifier.padding(16.dp)) { ReservationForm(viewModel = MainViewModel()) } } }

@Preview(showBackground = true, name = "Main Screen Light")
@Composable
fun DefaultPreviewLight() { BluelinkTheme(darkTheme = false) { MainScreen(viewModel = MainViewModel()) } }

@Preview(showBackground = true, name = "Main Screen Dark")
@Composable
fun DefaultPreviewDark() { BluelinkTheme(darkTheme = true) { MainScreen(viewModel = MainViewModel()) } }