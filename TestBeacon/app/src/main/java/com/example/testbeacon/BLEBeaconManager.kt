package com.example.testbeacon

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat

class BLEBeaconManager(private val context: Context) {
    private val bluetoothAdapter : BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    fun startIBeaconAdvertising() {
        if(!hasPermissions()) {
            Log.e("BLEBeacon", "Required permissions are not granted")
            return
        }

        val advertiser = bluetoothAdapter?.bluetoothLeAdvertiser
        if (advertiser == null) {
            Log.e("BLEBeacon", "BLE Advertising is not supported on this device.")
            return
        }

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(false)
            .build()

        // Everything needs to be changed into calls to a database of some kind.
        // uuid should be unique for each state, major, minor to be decided, don't fully understand txPower yet
        val beaconData = createIBeaconData(
            uuid = "9350C882-A23F-43B9-A176-A9C2AEEB1A5C",
            major = 1,
            minor = 1,
            txPower = -59
        )

        val data = AdvertiseData.Builder()
            .addManufacturerData(0x004C, beaconData) // 0x004C is Apple's Manufacturer ID
            .build()

        advertiser.startAdvertising(settings, data, advertiseCallback)
    }

    private fun createIBeaconData(
        uuid: String,
        major: Int,
        minor: Int,
        txPower: Int
    ): ByteArray {
        val uuidBytes = uuid.replace("-", "")
            .chunked(2)
            .map {it.toInt(16).toByte()}
            .toByteArray()
        val majorBytes = byteArrayOf((major shr 8).toByte(), major.toByte())
        val minorBytes = byteArrayOf((minor shr 8).toByte(), minor.toByte())
        return byteArrayOf(
            0x02, 0x15
        ) + uuidBytes + majorBytes + minorBytes + byteArrayOf(txPower.toByte())
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            Log.i("BLEBeacon", "iBeacon advertising started successfully")
        }

        override fun onStartFailure(errorCode: Int) {
            Log.e("BLEBeacon", "iBeacon advertising failed with error code: $errorCode")
        }
    }

    private fun hasPermissions(): Boolean {
        val permissions = if (
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
        ) {
            arrayOf(
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }

        return permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}