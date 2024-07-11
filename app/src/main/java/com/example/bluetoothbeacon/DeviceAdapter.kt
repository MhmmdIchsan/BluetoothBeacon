package com.example.bluetoothbeacon

import android.Manifest
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class DeviceAdapter(private val context: Context) : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

    private val devices = mutableListOf<Device>()


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_device, parent, false)
        return DeviceViewHolder(view, parent.context)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = devices[position]
        holder.bind(device)
    }

    override fun getItemCount() = devices.size

    fun deviceExists(address: String): Boolean {
        return devices.any { it.address == address }
    }

    fun addDevice(device: BluetoothDevice, rssi: Int) {
        val hasBluetoothPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
        val deviceName = if (hasBluetoothPermission) device.name else null
        val deviceType = if (hasBluetoothPermission) classifyDevice(device) else "Unknown"
        if (!deviceName.isNullOrEmpty()) {
            val newDevice = Device(deviceName, deviceType, device.address, rssi, System.currentTimeMillis())
            devices.add(newDevice)
            notifyItemInserted(devices.size - 1)
        }
    }

    fun updateDevice(device: BluetoothDevice, newRssi: Int) {
        val index = devices.indexOfFirst { it.address == device.address }
        if (index != -1) {
            val updatedDevice = devices[index].copy(rssi = newRssi, lastUpdated = System.currentTimeMillis())
            devices[index] = updatedDevice
            notifyItemChanged(index)
        }
    }

    fun classifyDevice(device: BluetoothDevice): String {
        val hasBluetoothPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
        if (hasBluetoothPermission) {
            val deviceClass = device.bluetoothClass?.deviceClass ?: 0
            val majorDeviceClass = deviceClass and 0x1F00
            return when (majorDeviceClass) {
                BluetoothClass.Device.Major.COMPUTER -> "Computer"
                BluetoothClass.Device.Major.PHONE -> "Phone"
                BluetoothClass.Device.Major.NETWORKING -> "Networking"
                BluetoothClass.Device.Major.AUDIO_VIDEO -> "Audio Video"
                BluetoothClass.Device.Major.PERIPHERAL -> "Peripheral"
                BluetoothClass.Device.Major.IMAGING -> "Imaging"
                BluetoothClass.Device.Major.WEARABLE -> "Wearable"
                BluetoothClass.Device.Major.TOY -> "Toy"
                BluetoothClass.Device.Major.HEALTH -> "Health"
                else -> "Unknown"
            }
        } else {
            // Handle the case where the permission is not granted
            // You can request the permission, or inform the user that the permission is necessary
            return "Permission not granted"
        }
    }

    fun removeDevice(address: String) {
        val index = devices.indexOfFirst { it.address == address }
        if (index != -1) {
            devices.removeAt(index)
            notifyItemRemoved(index)
        }
        Log.d("DeviceAdapter", "Removed device: $address")
    }

    fun removeUndetectedDevices() {
        val devicesToRemove = mutableListOf<Device>()
        val rssiThreshold = -80 // Set the RSSI threshold. You can adjust this value as needed.

        devices.forEach { device ->
            if (device.rssi < rssiThreshold) {
                devicesToRemove.add(device)
            }
        }
        devicesToRemove.forEach { device ->
            devices.remove(device)
            notifyItemRemoved(devices.indexOf(device))
        }
        Log.d("DeviceAdapter", "Removed undetected devices: ${devicesToRemove.map { it.name }}")
    }



    fun iterateDevices(action: (Device) -> Unit) {
        for (device in devices) {
            action(device)
        }
    }

    class DeviceViewHolder(itemView: View, private val context: Context) : RecyclerView.ViewHolder(itemView) {
        private val deviceName: TextView = itemView.findViewById(R.id.device_name)
        private val deviceAddress: TextView = itemView.findViewById(R.id.device_address)
        private val deviceType: TextView = itemView.findViewById(R.id.device_type)
        private val deviceRssi: TextView = itemView.findViewById(R.id.device_rssi)

        fun bind(device: Device) {
            deviceName.text = device.name ?: "."
            deviceType.text = device.type
            deviceAddress.text = device.address
            deviceRssi.text = "${device.rssi} dBm"
        }
    }
}