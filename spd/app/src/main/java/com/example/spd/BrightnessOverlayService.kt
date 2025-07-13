package com.example.spd

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.bluetooth.*
import android.bluetooth.BluetoothGatt // Keep explicit import
import android.bluetooth.le.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.*
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import java.util.*

class BrightnessOverlayService : Service() {

    private val TAG = "BrightnessOverlayService"

    // --- Configuration (Replace with your ESP32 details) ---
    private val ESP32_DEVICE_NAME = "ESP32_Brightness" // The name your ESP32 advertises
    private val SERVICE_UUID_STRING = "4fafc201-1fb5-459e-8fcc-c5c9c331914b" // Replace with your Service UUID
    private val CHARACTERISTIC_UUID_STRING = "beb5483e-36e1-4688-b7f5-ea07361b26a8" // Replace with your Characteristic UUID
    // --- End Configuration ---

    private lateinit var serviceUUID: UUID
    private lateinit var characteristicUUID: UUID

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private lateinit var overlayLayoutParams: WindowManager.LayoutParams

    private var bluetoothManager: BluetoothManager? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private var targetDevice: BluetoothDevice? = null
    private var brightnessCharacteristic: BluetoothGattCharacteristic? = null

    private var isScanning = false
    private var isConnected = false
    private val handler = Handler(Looper.getMainLooper())

    private val NOTIFICATION_CHANNEL_ID = "BrightnessOverlayChannel"
    private val NOTIFICATION_ID = 1

    // --- Service Lifecycle ---

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")

        // Initialize UUIDs
        try {
            serviceUUID = UUID.fromString(SERVICE_UUID_STRING)
            characteristicUUID = UUID.fromString(CHARACTERISTIC_UUID_STRING)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Invalid UUID format. Please check configuration.", e)
            stopSelf() // Stop service if UUIDs are invalid
            return
        }


        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createOverlayView()
        initializeBluetooth()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service onStartCommand")
        startForeground(NOTIFICATION_ID, createNotification("서비스 시작 중..."))

        if (bluetoothAdapter?.isEnabled == true) {
            startBleScan()
        } else {
            Log.w(TAG, "Bluetooth not enabled when starting service.")
            // Optionally, notify the user or attempt to request enable via Activity
            updateNotification("블루투스 비활성화됨")
            // Consider stopping the service if Bluetooth is essential and off
            // stopSelf()
        }

        // If the service is killed, restart it
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service onDestroy")
        removeOverlayView()
        stopBleScan() // Ensure scanning is stopped
        disconnectGatt() // Disconnect and close GATT connection
        handler.removeCallbacksAndMessages(null) // Clean up handler messages
    }

    override fun onBind(intent: Intent?): IBinder? {
        // This service doesn't provide binding, so return null
        return null
    }

    // --- Overlay Management ---

    @SuppressLint("InflateParams") // Okay for system overlay
    private fun createOverlayView() {
        if (overlayView != null) return // Avoid creating multiple overlays

        overlayView = View(this)
        // Initial state: fully transparent black
        overlayView?.setBackgroundColor(Color.BLACK)
        overlayView?.alpha = 0.0f // Start fully transparent

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE // Deprecated but needed for older versions
        }

        overlayLayoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayType,
            // Make the overlay non-touchable and non-focusable
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT // Allow transparency
        )
        overlayLayoutParams.gravity = Gravity.TOP or Gravity.START
        overlayLayoutParams.x = 0
        overlayLayoutParams.y = 0

        try {
            // Check if permission is granted before adding the view
            if (!Settings.canDrawOverlays(this)) {
                Log.e(TAG, "SYSTEM_ALERT_WINDOW permission not granted. Cannot add overlay.")
                updateNotification("오버레이 권한 필요")
                stopSelf() // Stop the service if permission is missing
                return
            }
            windowManager.addView(overlayView, overlayLayoutParams)
            Log.d(TAG, "Overlay view added.")
        } catch (e: Exception) {
            Log.e(TAG, "Error adding overlay view", e)
            overlayView = null // Ensure view is null if adding failed
            stopSelf() // Stop service if overlay cannot be added
        }
    }

    private fun removeOverlayView() {
        if (overlayView != null && overlayView!!.isAttachedToWindow) { // Check if attached before removing
            try {
                windowManager.removeView(overlayView)
                Log.d(TAG, "Overlay view removed.")
            } catch (e: Exception) {
                Log.e(TAG, "Error removing overlay view", e)
            } finally {
                overlayView = null
            }
        } else {
            overlayView = null // Ensure it's null even if not attached
        }
    }

    // Adjust overlay transparency (alpha)
    // brightnessValue: Expected range 0-255 (adjust mapping if ESP32 sends different range)
    private fun updateOverlayAlpha(brightnessValue: Int) {
        // Map brightness (0-255) to alpha (0.0f - 0.8f).
        // Inverse relationship: Higher brightness reading -> Lower screen brightness -> Higher overlay alpha (more opaque)
        // Clamp the brightness value just in case
        val clampedBrightness = brightnessValue.coerceIn(0, 255)
        // Normalize to 0.0 - 1.0
        val normalizedBrightness = clampedBrightness / 255.0f
        // Invert and scale to desired alpha range (e.g., 0.0 to 0.8 for max dimming)
        // Make sure alpha doesn't exceed 1.0f or go below 0.0f
        val targetAlpha = ( (1.0f - normalizedBrightness) * 0.8f ).coerceIn(0.0f, 1.0f)


        Log.d(TAG, "Received brightness: $brightnessValue, Calculated Alpha: $targetAlpha")

        overlayView?.let { view ->
            handler.post { // Update UI element on the main thread
                try {
                    view.alpha = targetAlpha
                    // Update layout params only if view is still attached
                    if (view.isAttachedToWindow) {
                        windowManager.updateViewLayout(view, overlayLayoutParams)
                    }
                } catch (e: IllegalArgumentException) {
                    // This can happen if the view is detached between the post and execution
                    Log.w(TAG, "Error updating overlay alpha, view might be detached: ${e.message}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating overlay alpha", e)
                }
            }
        }
    }


    // --- Bluetooth LE Management ---

    @SuppressLint("MissingPermission") // Permissions checked in Activity before starting service
    private fun initializeBluetooth() {
        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager?.adapter

        if (bluetoothAdapter == null) {
            Log.e(TAG, "Device doesn't support Bluetooth")
            stopSelf()
            return
        }

        bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
        if (bluetoothLeScanner == null) {
            Log.e(TAG, "Failed to get BluetoothLeScanner")
            // Attempting to get scanner can sometimes fail initially, maybe retry?
            // For now, stop the service.
            stopSelf()
        }
    }

    @SuppressLint("MissingPermission") // Permissions checked before calling
    private fun startBleScan() {
        // Check if already scanning
        if (isScanning) {
            Log.d(TAG, "Scan already in progress.")
            return
        }
        // Check if Bluetooth adapter is enabled
        if (bluetoothAdapter?.isEnabled == false) {
            Log.w(TAG,"Bluetooth is disabled. Cannot start scan.")
            updateNotification("블루투스 비활성화됨")
            // Consider stopping or waiting
            return
        }
        // Check if scanner is available
        if (bluetoothLeScanner == null) {
            Log.e(TAG, "BluetoothLeScanner is null. Cannot start scan.")
            updateNotification("BLE 스캐너 오류")
            stopSelf() // Stop if scanner isn't available
            return
        }


        // Check permissions again for safety
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "BLUETOOTH_SCAN permission missing")
                updateNotification("권한 오류 (Scan)")
                stopSelf()
                return
            }
        } else {
            // Fine location is needed for scanning pre-Android 12
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "ACCESS_FINE_LOCATION permission missing for BLE scan")
                updateNotification("권한 오류 (Location)")
                stopSelf()
                return
            }
        }


        val scanFilters = listOf(
            ScanFilter.Builder()
                // .setDeviceName(ESP32_DEVICE_NAME) // Filter by name - can be less reliable
                .setServiceUuid(ParcelUuid(serviceUUID)) // More reliable to filter by Service UUID
                .build()
        )
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY) // Use low latency for faster discovery
            .build()

        try {
            isScanning = true
            bluetoothLeScanner?.startScan(scanFilters, scanSettings, leScanCallback)
            Log.d(TAG, "BLE scan started for Service UUID: $serviceUUID")
            updateNotification("ESP32 검색 중...")

            // Stop scanning after a timeout period to save battery
            handler.removeCallbacks(stopScanRunnable) // Remove previous runnable if any
            handler.postDelayed(stopScanRunnable, SCAN_PERIOD)

        } catch (e: Exception) {
            Log.e(TAG, "Exception starting BLE scan", e)
            isScanning = false
            updateNotification("스캔 시작 오류")
            // Consider stopping or retrying
        }
    }

    // Runnable to stop scanning after a timeout
    private val stopScanRunnable = Runnable {
        if (isScanning) {
            Log.w(TAG, "Scan timeout reached.")
            stopBleScan()
            if (!isConnected) {
                updateNotification("장치 찾을 수 없음 (Timeout)")
                // Consider stopping the service or retrying later
                // stopSelf()
            }
        }
    }


    @SuppressLint("MissingPermission") // Permissions checked before calling
    private fun stopBleScan() {
        // Check if scanning and adapter is enabled
        if (!isScanning || bluetoothAdapter?.isEnabled == false || bluetoothLeScanner == null) {
            isScanning = false // Ensure flag is reset
            return
        }

        // Check permission again for safety
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Cannot stop scan, BLUETOOTH_SCAN permission missing")
                // Cannot stop scan without permission, potential leak
                isScanning = false // Still update the flag
                return
            }
        }

        try {
            bluetoothLeScanner?.stopScan(leScanCallback)
            Log.d(TAG, "BLE scan stopped.")
        } catch (e: Exception) {
            Log.e(TAG, "Exception stopping BLE scan", e)
        } finally {
            isScanning = false
            handler.removeCallbacks(stopScanRunnable) // Remove timeout runnable
        }
    }

    // Device scan callback
    private val leScanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission") // Permissions checked before calling startBleScan
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            // Check if we are still scanning and not already connected
            if (!isScanning || isConnected) return

            Log.i(TAG, "Device found: ${result.device.name ?: "Unknown"} (${result.device.address}) RSSI: ${result.rssi}")

            // Optional: Check signal strength (RSSI) - might help filter distant devices
            // if (result.rssi < -75) return // Ignore weak signals

            // Check if the found device matches the name (optional, UUID filter is primary)
            // if (result.device.name == ESP32_DEVICE_NAME) { // Uncomment if filtering by name too

            targetDevice = result.device
            Log.i(TAG, "Target device found: ${targetDevice?.address}. Stopping scan and connecting.")
            stopBleScan() // Stop scanning once the target is found
            connectToDevice(targetDevice!!)
            // }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            super.onBatchScanResults(results)
            if (!isScanning || isConnected) return
            // Process batch results if needed, find the target device
            results.firstOrNull()?.let { result -> // Example: process the first result in batch
                Log.i(TAG, "Batch Device found: ${result.device.name ?: "Unknown"} (${result.device.address})")
                targetDevice = result.device
                stopBleScan()
                connectToDevice(targetDevice!!)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e(TAG, "BLE Scan Failed with error code: $errorCode")
            isScanning = false
            updateNotification("스캔 오류: $errorCode")
            handler.removeCallbacks(stopScanRunnable) // Remove timeout runnable on failure
            // Handle scan failure (e.g., stop service, retry)
        }
    }

    // Connect to the target device
    @SuppressLint("MissingPermission") // Permissions checked before calling
    private fun connectToDevice(device: BluetoothDevice) {
        // Check permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Log.e(TAG, "BLUETOOTH_CONNECT permission missing")
            updateNotification("권한 오류 (Connect)")
            return
        }
        // Check if already connected or connecting
        if (isConnected || bluetoothGatt != null) {
            Log.w(TAG, "Already connected or connection attempt in progress.")
            return
        }

        Log.d(TAG, "Connecting to GATT server on device: ${device.address}")
        updateNotification("장치 연결 중...")
        // Connect directly to the device's GATT server.
        // autoConnect = false for direct connection attempt. Set to true to automatically
        // reconnect if the device comes back into range after disconnection.
        // Use TRANSPORT_LE for explicit LE connection
        bluetoothGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            device.connectGatt(this, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } else {
            @Suppress("DEPRECATION")
            device.connectGatt(this, false, gattCallback)
        }

        if (bluetoothGatt == null) {
            Log.e(TAG, "connectGatt returned null")
            updateNotification("GATT 연결 실패")
            // Consider retrying scan
            handler.postDelayed({ startBleScan() }, 1000)
        }
    }

    @SuppressLint("MissingPermission") // Permissions checked before calling
    private fun disconnectGatt() {
        // Check permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Log.e(TAG, "BLUETOOTH_CONNECT permission missing, cannot disconnect GATT")
            // Cannot disconnect properly without permission, but still clear state
            bluetoothGatt = null // Clear GATT reference
            isConnected = false
            brightnessCharacteristic = null
            targetDevice = null
            updateNotification("연결 끊김 (권한 없음)")
            return
        }

        if (bluetoothGatt != null) {
            Log.d(TAG, "Disconnecting and closing GATT.")
            try {
                bluetoothGatt?.disconnect()
                // Important: Close GATT client after disconnect to release resources
                bluetoothGatt?.close()
            } catch (e: Exception) {
                Log.e(TAG, "Exception during GATT disconnect/close", e)
            } finally {
                bluetoothGatt = null // Clear GATT reference
            }
        }
        // Reset state variables regardless of whether gatt object was null or closed successfully
        isConnected = false
        brightnessCharacteristic = null
        targetDevice = null // Clear target device reference
        updateNotification("연결 끊김")
        // Optionally restart scanning after a delay if desired
        // handler.postDelayed({ startBleScan() }, 5000) // Retry scan after 5 seconds
    }


    // GATT callback for connection changes, service discovery, and characteristic reads/notifications
    private val gattCallback = object : BluetoothGattCallback() {

        @SuppressLint("MissingPermission") // Permissions checked before calling connectToDevice
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val deviceAddress = gatt.device.address

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.i(TAG, "Successfully connected to $deviceAddress")
                    isConnected = true
                    updateNotification("장치 연결됨")
                    // Discover services after successful connection
                    // Run service discovery on a handler thread to avoid blocking the callback thread
                    handler.post {
                        if (ActivityCompat.checkSelfPermission(this@BrightnessOverlayService, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            Log.e(TAG, "BLUETOOTH_CONNECT permission missing for service discovery")
                            updateNotification("권한 오류 (Connect)")
                            disconnectGatt() // Disconnect if we can't proceed
                            return@post
                        }
                        Log.d(TAG, "Attempting to discover services...")
                        val discoveryInitiated = gatt.discoverServices()
                        if (!discoveryInitiated) {
                            Log.e(TAG, "Failed to initiate service discovery.")
                            disconnectGatt()
                        }
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.i(TAG, "Disconnected from $deviceAddress")
                    // Check if the disconnect was expected (e.g., initiated by disconnectGatt)
                    if (isConnected) { // Only log/retry if it was an unexpected disconnect
                        disconnectGatt() // Clean up resources
                        // Attempt to reconnect or rescan if needed
                        Log.d(TAG, "Unexpected disconnect. Retrying scan...")
                        handler.postDelayed({ startBleScan() }, 5000) // Retry scan after 5 seconds
                    }
                }
            } else {
                // Handle common errors using the literal value for GATT_ERROR
                val errorMsg = when(status) {
                    133 -> "GATT_ERROR (133)" // *** Use literal value 133 ***
                    8 -> "GATT CONN TIMEOUT"
                    19 -> "GATT CONN TERMINATE PEER USER"
                    22 -> "GATT CONN LMP TIMEOUT"
                    // Add other relevant BluetoothGatt status codes if needed
                    else -> "Unknown error status: $status"
                }
                Log.w(TAG, "Connection state change failed for $deviceAddress. Status: $status ($errorMsg)")
                disconnectGatt() // Clean up on failure
                // Retry scan after a delay on failure
                handler.postDelayed({ startBleScan() }, 5000)
            }
        }

        @SuppressLint("MissingPermission") // Permissions checked before discovering
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Services discovered successfully.")
                val service = gatt.getService(serviceUUID)
                if (service == null) {
                    Log.e(TAG, "Service UUID $serviceUUID not found.")
                    disconnectGatt()
                    return
                }

                brightnessCharacteristic = service.getCharacteristic(characteristicUUID)
                if (brightnessCharacteristic == null) {
                    Log.e(TAG, "Characteristic UUID $characteristicUUID not found in service $serviceUUID.")
                    disconnectGatt()
                    return
                }

                Log.i(TAG, "Found target characteristic. Enabling notifications...")
                // Enable notifications on the handler thread
                handler.post {
                    enableCharacteristicNotifications(gatt, brightnessCharacteristic!!)
                }

            } else {
                Log.w(TAG, "Service discovery failed with status: $status")
                disconnectGatt()
            }
        }

        // Result of a characteristic read operation (not used if using notifications)
        @Deprecated("Deprecated in Java")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Characteristic ${characteristic.uuid} read successfully")
                processCharacteristicValue(characteristic)
            } else {
                Log.w(TAG, "Characteristic read failed for ${characteristic.uuid}, status: $status")
            }
        }

        // Called when characteristic value changes (if notifications/indications enabled)
        // Note: This callback might be invoked on a binder thread. Process data quickly or post to another thread.
        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            Log.v(TAG, "Characteristic ${characteristic.uuid} changed") // Verbose log
            processCharacteristicValue(characteristic)
        }

        // New callback for Android 13+ (Tiramisu)
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            Log.v(TAG, "Characteristic ${characteristic.uuid} changed (Tiramisu)") // Verbose log
            processCharacteristicValue(characteristic, value)
        }


        // Result of enabling/disabling notifications/indications
        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            if (descriptor.characteristic.uuid == brightnessCharacteristic?.uuid &&
                descriptor.uuid == CLIENT_CHARACTERISTIC_CONFIG_UUID) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    val value = descriptor.value
                    if (Arrays.equals(value, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {
                        Log.i(TAG, "Successfully enabled notifications for ${descriptor.characteristic.uuid}")
                        updateNotification("밝기 수신 중...")
                        // Optional: Perform an initial read after enabling notifications
                        // handler.post { readBrightnessCharacteristic() }
                    } else if (Arrays.equals(value, BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)) {
                        Log.i(TAG, "Successfully disabled notifications for ${descriptor.characteristic.uuid}")
                    } else {
                        Log.w(TAG, "Descriptor written with unexpected value for ${descriptor.characteristic.uuid}")
                    }
                } else {
                    Log.e(TAG, "Failed to write descriptor ${descriptor.uuid} with status $status")
                    disconnectGatt() // Disconnect if we can't setup notifications
                }
            } else {
                Log.w(TAG, "onDescriptorWrite for unknown descriptor: ${descriptor.uuid}")
            }
        }
    }

    // Common function to process characteristic data from both old and new callbacks
    private fun processCharacteristicValue(characteristic: BluetoothGattCharacteristic, data: ByteArray? = null) {
        if (characteristic.uuid == characteristicUUID) {
            // Use data if provided (Android 13+), otherwise get from characteristic (legacy)
            val value = data ?: characteristic.value
            if (value != null && value.isNotEmpty()) {
                // --- Data Parsing Logic ---
                // Assuming the ESP32 sends a single byte (0-255) for brightness.
                // Adapt this parsing logic based on how ESP32 sends data
                // (e.g., integer, float, string).
                try {
                    val brightness = value[0].toInt() and 0xFF // Read the first byte as unsigned int

                    // Example for 2 bytes (Little Endian integer):
                    // if (value.size >= 2) {
                    //    val brightness = ((value[1].toInt() and 0xFF) shl 8) or (value[0].toInt() and 0xFF)
                    // }

                    // Example for String:
                    // val brightnessString = String(value, Charsets.UTF_8)
                    // val brightness = brightnessString.toIntOrNull() ?: 0

                    Log.d(TAG, "Received brightness value: $brightness (Hex: ${value.joinToString("") { "%02X".format(it) }})")
                    updateOverlayAlpha(brightness)
                    updateNotification("밝기: $brightness") // Update notification with value
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing characteristic data", e)
                }
                // --- End Data Parsing Logic ---
            } else {
                Log.w(TAG, "Received empty or null data on characteristic.")
            }
        }
    }


    // Enable notifications or indications for a characteristic
    @SuppressLint("MissingPermission") // Permissions checked before calling
    private fun enableCharacteristicNotifications(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        // Check permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Log.e(TAG, "BLUETOOTH_CONNECT permission missing, cannot enable notifications")
            updateNotification("권한 오류 (Connect)")
            disconnectGatt()
            return
        }

        val cccd = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID)
        if (cccd == null) {
            Log.e(TAG, "Client Characteristic Configuration Descriptor (CCCD) not found for ${characteristic.uuid}")
            disconnectGatt()
            return
        }

        // Check characteristic properties
        val properties = characteristic.properties
        val canNotify = (properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0
        val canIndicate = (properties and BluetoothGattCharacteristic.PROPERTY_INDICATE) > 0

        if (!canNotify && !canIndicate) {
            Log.e(TAG, "Characteristic ${characteristic.uuid} does not support notifications or indications.")
            disconnectGatt()
            return
        }

        // Prefer Notifications over Indications if available
        val enableValue = if (canNotify) {
            BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        } else {
            BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
        }

        // Enable notifications/indications locally on the Android device
        if (!gatt.setCharacteristicNotification(characteristic, true)) {
            Log.e(TAG, "Failed to enable notifications locally for ${characteristic.uuid}")
            disconnectGatt()
            return
        }

        // Write to the CCCD to enable notifications/indications on the peripheral (ESP32)
        Log.d(TAG,"Writing ${if(canNotify) "ENABLE_NOTIFICATION" else "ENABLE_INDICATION"} to CCCD for ${characteristic.uuid}")
        val writeSuccess: Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Use the new writeDescriptor method for Android 13+
            val result = gatt.writeDescriptor(cccd, enableValue)
            Log.d(TAG, "writeDescriptor (Android 13+) result code: $result")
            result == BluetoothStatusCodes.SUCCESS // Check for success code
        } else {
            // Use the deprecated method for older versions
            @Suppress("DEPRECATION")
            cccd.value = enableValue // Set the value before writing
            @Suppress("DEPRECATION")
            gatt.writeDescriptor(cccd) // Returns boolean indicating if operation was initiated
        }

        if (!writeSuccess) {
            Log.e(TAG, "Failed to initiate descriptor write operation.")
            disconnectGatt()
        } else {
            Log.d(TAG,"Initiated write to CCCD for ${characteristic.uuid}")
        }
    }


    // --- Notification Management ---

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "Brightness Overlay Service"
            val importance = NotificationManager.IMPORTANCE_LOW // Low importance = less intrusive
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, importance).apply {
                description = "Notification channel for the brightness overlay service"
                // Optionally disable sound, vibration, etc.
                // setSound(null, null)
                // enableVibration(false)
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created.")
        }
    }

    private fun createNotification(text: String): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java) // Intent to open MainActivity when notification is tapped
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, pendingIntentFlags)


        // Basic notification
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("밝기 오버레이 서비스")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Use a standard system icon for now
            // .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with your app's icon if available
            .setContentIntent(pendingIntent)
            .setOngoing(true) // Make it non-dismissable while service is running
            .setOnlyAlertOnce(true) // Don't make sound/vibrate for updates
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE) // Show immediately
            .setPriority(NotificationCompat.PRIORITY_LOW) // Low priority for less intrusion
            .build()
    }

    private fun updateNotification(text: String) {
        try {
            val notification = createNotification(text)
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            // Check for POST_NOTIFICATIONS permission before notifying on Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "POST_NOTIFICATIONS permission not granted. Cannot update notification.")
                return // Don't attempt to notify if permission is missing
            }
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating notification", e)
        }
    }


    companion object {
        private const val SCAN_PERIOD: Long = 15000 // Stops scanning after 15 seconds.
        // Standard UUID for the Client Characteristic Configuration Descriptor (CCCD)
        private val CLIENT_CHARACTERISTIC_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }
}
