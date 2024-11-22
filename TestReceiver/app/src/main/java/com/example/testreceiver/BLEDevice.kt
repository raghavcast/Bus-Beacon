package com.example.testreceiver

import android.bluetooth.le.ScanResult

open class BLEDevice(scanResult: ScanResult) {
    private var rssi: Int = 0
    private var address: String = ""
    private var name: String = ""

    init {
        if (scanResult.device.name != null) {
            name = scanResult.device.name
        }
        address = scanResult.device.address
        rssi = scanResult.rssi
    }

    fun getAddress(): String {
        return address
    }

    fun getRssi(): Int {
        return rssi
    }
}
