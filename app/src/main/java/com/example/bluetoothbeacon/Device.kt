package com.example.bluetoothbeacon

data class Device(
    val name: String?,
    val type: String,
    val address: String,
    var rssi: Int,
    var lastUpdated: Long = System.currentTimeMillis()
)