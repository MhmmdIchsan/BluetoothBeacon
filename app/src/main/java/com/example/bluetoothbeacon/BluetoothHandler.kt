package com.example.bluetoothbeacon

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.util.Log
import android.widget.Toast
import android.os.Handler
import android.os.Looper

class BluetoothHandler(private val context: Context) {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    private var scanCallback: ScanCallback? = null
    private val rssiMap = mutableMapOf<String, Int>()
    private val handler = Handler(Looper.getMainLooper())


    fun initBluetooth(): Boolean {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled
    }

    fun startScanning(onDeviceFound: (BluetoothDevice, Int) -> Unit, onDeviceIgnored: (BluetoothDevice) -> Unit) {
        if (::bluetoothAdapter.isInitialized) {
            try {
                val filters = ArrayList<ScanFilter>()
                val settings = ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build()

                scanCallback = object : ScanCallback() {
                    override fun onScanResult(callbackType: Int, result: ScanResult) {
                        val rssi = result.rssi
                        val device = result.device
                        val deviceName = device.name ?: "." // Menggunakan nama perangkat Bluetooth yang terdeteksi

                        Log.i("BLE", "Device found: $deviceName - ${device.address} - RSSI $rssi - ${device.bluetoothClass.majorDeviceClass}")

                        val previousRssi = rssiMap[result.device.address]
                        if (previousRssi == null || Math.abs(previousRssi - result.rssi) > 5) {
                            rssiMap[result.device.address] = result.rssi
                            handler.postDelayed({
                                if (rssiMap[result.device.address] == result.rssi) {
                                    onDeviceFound(result.device, result.rssi)
                                } else {
                                    onDeviceIgnored(result.device)
                                }
                            }, 2000)
                        }
                    }
                }

                bluetoothLeScanner.startScan(filters, settings, scanCallback)
            } catch (e: SecurityException) {
                Log.e("BLE", "SecurityException: Permission denied", e)
                Toast.makeText(context, "Permission denied for Bluetooth scanning", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.e("BLE", "BluetoothAdapter is not initialized")
        }
    }

    fun stopScanning() {
        if (::bluetoothAdapter.isInitialized && scanCallback != null) {
            try {
                bluetoothLeScanner.stopScan(scanCallback)
            } catch (e: SecurityException) {
                Log.e("BLE", "SecurityException: Permission denied", e)
            }
        }
    }
}