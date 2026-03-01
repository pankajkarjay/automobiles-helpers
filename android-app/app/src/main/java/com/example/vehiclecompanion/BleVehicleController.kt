package com.example.vehiclecompanion

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import androidx.core.content.ContextCompat
import java.nio.charset.StandardCharsets
import java.util.UUID

class BleVehicleController(
    private val context: Context,
    private val events: Events
) {
    interface Events {
        fun onStateChanged(connected: Boolean, label: String)
        fun onLog(message: String)
    }

    companion object {
        val vehicleServiceUuid: UUID = UUID.fromString("0000abcd-0000-1000-8000-00805f9b34fb")
        val commandCharacteristicUuid: UUID = UUID.fromString("0000dcba-0000-1000-8000-00805f9b34fb")
    }

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val scanner: BluetoothLeScanner?
        get() = bluetoothAdapter?.bluetoothLeScanner

    private var currentGatt: BluetoothGatt? = null
    private var commandCharacteristic: BluetoothGattCharacteristic? = null
    private var isScanning = false

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            stopScan()
            connectToDevice(result.device)
        }

        override fun onScanFailed(errorCode: Int) {
            isScanning = false
            events.onLog("BLE scan failed: $errorCode")
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                events.onLog("Connected. Discovering services...")
                gatt.discoverServices()
                return
            }
            if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                events.onLog("Vehicle disconnected")
                resetConnection()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                events.onLog("Service discovery failed: $status")
                disconnect()
                return
            }

            val service: BluetoothGattService? = gatt.getService(vehicleServiceUuid)
            val characteristic = service?.getCharacteristic(commandCharacteristicUuid)
            if (characteristic == null) {
                events.onLog("Command characteristic not found")
                disconnect()
                return
            }

            commandCharacteristic = characteristic
            currentGatt = gatt
            events.onStateChanged(true, gatt.device.name ?: "Authorized Vehicle")
            events.onLog("Ready for commands")
        }
    }

    fun hasRequiredPermissions(): Boolean {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions += Manifest.permission.BLUETOOTH_SCAN
            permissions += Manifest.permission.BLUETOOTH_CONNECT
        } else {
            permissions += Manifest.permission.ACCESS_FINE_LOCATION
        }
        return permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    @SuppressLint("MissingPermission")
    fun connect() {
        if (!hasRequiredPermissions()) {
            events.onLog("Missing required Bluetooth permissions")
            return
        }

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            events.onLog("Bluetooth adapter unavailable or disabled")
            return
        }

        events.onLog("Scanning for authorized BLE vehicle module...")
        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(vehicleServiceUuid))
            .build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanner?.startScan(listOf(filter), settings, scanCallback)
        isScanning = true
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        stopScan()
        currentGatt?.disconnect()
        currentGatt?.close()
        resetConnection()
        events.onLog("Disconnected by user")
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice(device: BluetoothDevice) {
        events.onLog("Connecting to ${device.name ?: "vehicle"}...")
        device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    @SuppressLint("MissingPermission")
    private fun stopScan() {
        if (isScanning) {
            scanner?.stopScan(scanCallback)
            isScanning = false
        }
    }

    @SuppressLint("MissingPermission")
    fun sendCommand(command: String) {
        val gatt = currentGatt
        val characteristic = commandCharacteristic
        if (gatt == null || characteristic == null) {
            events.onLog("Not connected")
            return
        }

        characteristic.value = command.toByteArray(StandardCharsets.UTF_8)
        val writeOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeCharacteristic(characteristic, characteristic.value, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT) == BluetoothGatt.GATT_SUCCESS
        } else {
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            @Suppress("DEPRECATION")
            gatt.writeCharacteristic(characteristic)
        }

        if (writeOk) {
            events.onLog("Command sent: $command")
        } else {
            events.onLog("Failed to send: $command")
        }
    }

    private fun resetConnection() {
        commandCharacteristic = null
        currentGatt = null
        events.onStateChanged(false, "Disconnected")
    }
}
