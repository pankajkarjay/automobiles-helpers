package com.example.vehiclecompanion

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity(), BleVehicleController.Events {

    private lateinit var controller: BleVehicleController

    private lateinit var statusText: TextView
    private lateinit var logText: TextView
    private lateinit var connectBtn: Button
    private lateinit var disconnectBtn: Button
    private lateinit var lockBtn: Button
    private lateinit var unlockBtn: Button
    private lateinit var startBtn: Button
    private lateinit var stopBtn: Button

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
            if (controller.hasRequiredPermissions()) {
                controller.connect()
            } else {
                onLog("Bluetooth permissions denied")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        logText = findViewById(R.id.logText)
        connectBtn = findViewById(R.id.connectBtn)
        disconnectBtn = findViewById(R.id.disconnectBtn)
        lockBtn = findViewById(R.id.lockBtn)
        unlockBtn = findViewById(R.id.unlockBtn)
        startBtn = findViewById(R.id.startBtn)
        stopBtn = findViewById(R.id.stopBtn)

        controller = BleVehicleController(this, this)

        connectBtn.setOnClickListener {
            if (controller.hasRequiredPermissions()) {
                controller.connect()
            } else {
                requestPermissions()
            }
        }

        disconnectBtn.setOnClickListener {
            controller.disconnect()
        }

        lockBtn.setOnClickListener { controller.sendCommand("LOCK") }
        unlockBtn.setOnClickListener { controller.sendCommand("UNLOCK") }
        startBtn.setOnClickListener { controller.sendCommand("START") }
        stopBtn.setOnClickListener { controller.sendCommand("STOP") }

        updateControlState(false)
    }

    override fun onStateChanged(connected: Boolean, label: String) {
        runOnUiThread {
            statusText.text = if (connected) "Connected: $label" else "Disconnected"
            updateControlState(connected)
        }
    }

    override fun onLog(message: String) {
        runOnUiThread {
            val existing = logText.text.toString()
            val next = if (existing.isBlank()) message else "$message\n$existing"
            logText.text = next
        }
    }

    private fun requestPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        permissionLauncher.launch(permissions)
    }

    private fun updateControlState(connected: Boolean) {
        connectBtn.isEnabled = !connected
        disconnectBtn.isEnabled = connected
        lockBtn.isEnabled = connected
        unlockBtn.isEnabled = connected
        startBtn.isEnabled = connected
        stopBtn.isEnabled = connected
    }
}
