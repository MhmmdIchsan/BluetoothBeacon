package com.example.bluetoothbeacon

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private val PERMISSION_REQUEST_CODE = 1
    private lateinit var bluetoothHandler: BluetoothHandler
    private lateinit var deviceAdapter: DeviceAdapter
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action: String = intent.action ?: ""
            when(action) {
                BluetoothDevice.ACTION_FOUND -> {
                    // A Bluetooth device was found
                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        try {
                            val deviceName = it.name
                            // Use deviceName here
                        } catch (e: SecurityException) {
                            Log.e("TAG", "SecurityException: Permission denied", e)
                        }
                        val deviceHardwareAddress = it.address // MAC address
                        val deviceType = deviceAdapter.classifyDevice(it)
                        // You can now add the device to your adapter
                        deviceAdapter.addDevice(it, 0) // replace 0 with actual RSSI if available
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bluetoothHandler = BluetoothHandler(this)
        checkPermissions()
        initRecyclerView()

        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(receiver, filter)
    }

    private fun checkPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), PERMISSION_REQUEST_CODE)
        } else {
            initializeBluetoothAndStartScanning()
        }
    }

    private fun initializeBluetoothAndStartScanning() {
        if (bluetoothHandler.initBluetooth()) {
            bluetoothHandler.startScanning({ device, rssi ->
                deviceAdapter.addDevice(device, rssi)
            }, { device ->
                deviceAdapter.removeDevice(device)
            })
        } else {
            Toast.makeText(this, "Bluetooth is not enabled", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initRecyclerView() {
        val recyclerView: RecyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        deviceAdapter = DeviceAdapter(this) // Pass the context here
        recyclerView.adapter = deviceAdapter
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                initializeBluetoothAndStartScanning()
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                initializeBluetoothAndStartScanning()
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                initializeBluetoothAndStartScanning()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        bluetoothHandler.stopScanning()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(receiver)
    }
}