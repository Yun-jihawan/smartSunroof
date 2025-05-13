package com.example.spd

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.spd.ui.theme.SpdTheme

class MainActivity : ComponentActivity() {

    private val TAG = "MainActivity"

    // Flag to track if service should be started after permissions are granted
    private var startServiceWhenReady = false

    // --- Permission Request Launchers ---

    // Launcher for requesting multiple permissions
    private val requestMultiplePermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            var allGranted = true
            permissions.entries.forEach {
                Log.d(TAG, "${it.key} granted: ${it.value}")
                if (!it.value) allGranted = false
            }

            if (allGranted) {
                Log.d(TAG, "All required permissions granted.")
                checkOverlayPermission() // Proceed to check overlay permission
            } else {
                Log.w(TAG, "Not all permissions were granted.")
                // Handle the case where permissions are denied (e.g., show a message)
                startServiceWhenReady = false // Reset flag
            }
        }

    // Launcher for requesting SYSTEM_ALERT_WINDOW permission
    private val requestOverlayPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            // Check if the permission was granted after returning from settings
            if (Settings.canDrawOverlays(this)) {
                Log.d(TAG, "SYSTEM_ALERT_WINDOW permission granted.")
                if (startServiceWhenReady) {
                    startOverlayService() // Start service if it was pending
                }
            } else {
                Log.w(TAG, "SYSTEM_ALERT_WINDOW permission denied.")
                startServiceWhenReady = false // Reset flag
                // Handle denial (e.g., show a message)
            }
        }

    // Launcher for enabling Bluetooth
    private val enableBluetoothLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                Log.d(TAG, "Bluetooth enabled successfully.")
                checkPermissionsAndStartService() // Re-check permissions and potentially start
            } else {
                Log.w(TAG, "Bluetooth not enabled.")
                startServiceWhenReady = false // Reset flag
                // Handle the case where the user doesn't enable Bluetooth
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // enableEdgeToEdge() // Consider if edge-to-edge is needed

        setContent {
            SpdTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MainScreen(
                        onStartServiceClick = {
                            startServiceWhenReady = true // Set flag to start service after checks
                            checkBluetoothAndPermissions()
                        },
                        onStopServiceClick = {
                            stopOverlayService()
                        }
                    )
                }
            }
        }
    }

    // --- Permission and Service Control Logic ---

    private fun checkBluetoothAndPermissions() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter == null) {
            Log.e(TAG, "Device doesn't support Bluetooth")
            // Handle device without Bluetooth support
            startServiceWhenReady = false
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            Log.d(TAG, "Bluetooth not enabled. Requesting...")
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher.launch(enableBtIntent)
        } else {
            Log.d(TAG, "Bluetooth is enabled.")
            checkPermissionsAndStartService() // Proceed if Bluetooth is already enabled
        }
    }


    private fun checkPermissionsAndStartService() {
        val requiredPermissions = mutableListOf<String>()

        // Bluetooth Permissions (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                requiredPermissions.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                requiredPermissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }
        // Location Permission (Needed for BLE Scan before Android 12, sometimes still useful)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requiredPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        // Notification Permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requiredPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (requiredPermissions.isNotEmpty()) {
            Log.d(TAG, "Requesting permissions: ${requiredPermissions.joinToString()}")
            requestMultiplePermissionsLauncher.launch(requiredPermissions.toTypedArray())
        } else {
            Log.d(TAG, "All basic permissions already granted.")
            // All permissions except overlay are granted, check overlay permission
            checkOverlayPermission()
        }
    }

    private fun checkOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            Log.d(TAG, "SYSTEM_ALERT_WINDOW permission not granted. Requesting...")
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            requestOverlayPermissionLauncher.launch(intent)
        } else {
            Log.d(TAG, "SYSTEM_ALERT_WINDOW permission already granted.")
            // All permissions granted, start the service if requested
            if (startServiceWhenReady) {
                startOverlayService()
            }
        }
    }


    private fun startOverlayService() {
        Log.d(TAG, "Starting BrightnessOverlayService.")
        val serviceIntent = Intent(this, BrightnessOverlayService::class.java)
        // Add potential extras if needed by the service
        // serviceIntent.putExtra("key", "value")
        ContextCompat.startForegroundService(this, serviceIntent)
        startServiceWhenReady = false // Reset flag after attempting to start
    }

    private fun stopOverlayService() {
        Log.d(TAG, "Stopping BrightnessOverlayService.")
        val serviceIntent = Intent(this, BrightnessOverlayService::class.java)
        stopService(serviceIntent)
        startServiceWhenReady = false // Reset flag
    }
}

// --- Composable UI ---

@Composable
fun MainScreen(onStartServiceClick: () -> Unit, onStopServiceClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("ESP32 밝기 오버레이", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onStartServiceClick) {
            Text("오버레이 시작")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onStopServiceClick) {
            Text("오버레이 중지")
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "시작 버튼을 누르면 필요한 권한을 요청합니다. '다른 앱 위에 표시' 권한은 설정에서 직접 허용해야 할 수 있습니다.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}
