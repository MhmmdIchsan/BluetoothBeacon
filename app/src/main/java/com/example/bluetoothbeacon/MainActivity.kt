package com.example.bluetoothbeacon

import android.Manifest
import android.view.Window
import android.view.WindowManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
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
            when (action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    val rssi: Int = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE).toInt()
                    device?.let {
                        if (deviceAdapter.deviceExists(device.address)) {
                            // If the device already exists in the list, update its RSSI
                            deviceAdapter.updateDevice(device, rssi)
                        } else {
                            // If the device does not exist in the list, add it
                            deviceAdapter.addDevice(device, rssi)
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    // Discovery has ended
                    Toast.makeText(context, "Discovery finished", Toast.LENGTH_SHORT).show()

                    // Remove undetected devices
                    deviceAdapter.removeUndetectedDevices()

                    // Restart discovery
                    bluetoothHandler.startScanning { device, rssi ->
                        if (deviceAdapter.deviceExists(device.address)) {
                            // If the device already exists in the list, update its RSSI
                            deviceAdapter.updateDevice(device, rssi)
                        } else {
                            // If the device does not exist in the list, add it
                            deviceAdapter.addDevice(device, rssi)
                        }
                    }
                }
            }
        }
    }

    private val updateHandler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            removeLostDevices() // Call this method before starting a new scan
            bluetoothHandler.startScanning { device, rssi ->
                if (deviceAdapter.deviceExists(device.address)) {
                    // If the device already exists in the list, update its RSSI
                    deviceAdapter.updateDevice(device, rssi)
                } else {
                    // If the device does not exist in the list, add it
                    deviceAdapter.addDevice(device, rssi)
                }
            }
            // Schedule the next update after 1 second
            updateHandler.postDelayed(this, 5000)
        }
    }

    private fun removeLostDevices() {
        val devicesToRemove = mutableListOf<Device>()
        val outOfRangeThreshold = 5000 // Set the out of range threshold to 5 seconds. You can adjust this value as needed.

        deviceAdapter.iterateDevices { device ->
            val timeSinceLastUpdate = System.currentTimeMillis() - device.lastUpdated
            if (timeSinceLastUpdate > outOfRangeThreshold) {
                devicesToRemove.add(device)
            }
        }
        devicesToRemove.forEach { device ->
            deviceAdapter.removeDevice(device.address)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        changeStatusBarColor(R.color.black)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.title = "Bluetooth Beacon"
        toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white))


        bluetoothHandler = BluetoothHandler(this)
        checkPermissions()
        initRecyclerView()

        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        registerReceiver(receiver, filter)
    }

    private fun changeStatusBarColor(colorResource: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val window: Window = window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = ContextCompat.getColor(this, colorResource)
        }
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
            bluetoothHandler.startScanning { device, rssi ->
                if (deviceAdapter.deviceExists(device.address)) {
                    // If the device already exists in the list, update its RSSI
                    deviceAdapter.updateDevice(device, rssi)
                } else {
                    // If the device does not exist in the list, add it
                    deviceAdapter.addDevice(device, rssi)
                }
            }
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            updateHandler.post(updateRunnable)
        }
    }

    override fun onPause() {
        super.onPause()
        updateHandler.removeCallbacks(updateRunnable)
        bluetoothHandler.stopScanning()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(receiver)
    }
}

