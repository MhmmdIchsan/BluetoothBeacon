package com.example.bluetoothbeacon

data class Device(
    val name: String?,
    val type: String,
    val address: String,
    val rssi: Int
)
