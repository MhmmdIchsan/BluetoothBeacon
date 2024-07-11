package com.example.bluetoothbeacon

import kotlinx.coroutines.*
import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class BluetoothHandler(private val context: Context) {
    private val bluetoothScope = CoroutineScope(Dispatchers.IO)
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var isScanning = false

    fun initBluetooth(): Boolean {
        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            return false
        }
        if (!bluetoothAdapter.isEnabled) {
            // Bluetooth is not enabled
            return false
        }
        return true
    }

    fun startScanning(onDeviceFoundWithRssi: (BluetoothDevice, Int) -> Unit) {
        // Check for ACCESS_FINE_LOCATION permission
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            // Show a message to the user or request the permission here
            return
        }

        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        context.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                val rssi: Int = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE).toInt()
                device?.let {
                    onDeviceFoundWithRssi(it, rssi)
                }
            }
        }, filter)

        // Start Bluetooth discovery in a coroutine
        bluetoothScope.launch {
            bluetoothAdapter?.startDiscovery()
        }
    }

    fun stopScanning() {
        // Check for BLUETOOTH_ADMIN permission
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            // Show a message to the user or request the permission here
            return
        }

        // Cancel the coroutine
        bluetoothScope.cancel()

        // Stop Bluetooth discovery
        bluetoothAdapter?.cancelDiscovery()
    }
}