package com.example.bluetoothbeacon

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import android.widget.Toast
import android.os.Handler
import android.os.Looper

class BluetoothHandler(private val context: Context) {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private val handler = Handler(Looper.getMainLooper())

    fun initBluetooth(): Boolean {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled
    }

    fun startScanning(onDeviceFound: (BluetoothDevice) -> Unit) {
        if (::bluetoothAdapter.isInitialized) {
            try {
                bluetoothAdapter.startDiscovery()
            } catch (e: SecurityException) {
                Log.e("Bluetooth", "SecurityException: Permission denied", e)
                Toast.makeText(context, "Permission denied for Bluetooth scanning", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.e("Bluetooth", "BluetoothAdapter is not initialized")
        }
    }

    fun stopScanning() {
        if (::bluetoothAdapter.isInitialized) {
            try {
                bluetoothAdapter.cancelDiscovery()
            } catch (e: SecurityException) {
                Log.e("Bluetooth", "SecurityException: Permission denied", e)
            }
        }
    }
}