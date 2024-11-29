//package com.example.testbeacon
//
//import android.Manifest
//import android.bluetooth.BluetoothAdapter
//import android.bluetooth.le.AdvertiseCallback
//import android.bluetooth.le.AdvertiseData
//import android.bluetooth.le.AdvertiseSettings
//import android.bluetooth.le.BluetoothLeAdvertiser
//import android.content.Context
//import android.content.pm.PackageManager
//import android.util.Log
//import android.view.View
//import androidx.compose.runtime.derivedStateOf
//import androidx.core.content.ContextCompat
//
//class BLEBeaconManager(private val context: Context) {
//
//
//
//
//    private var selectedBus: String = ""
//    private val busHash: HashMap<String, String> =
//        HashMap<String, String>()
//
//
//    fun startIBeaconAdvertising(uuid: String, major: Int, minor: Int) {
//
//    }
//
//    fun stopIBeaconAdvertising() {
//        advertiser.stopAdvertising(AdvertiseCallback())
//    }
//
//    private fun createIBeaconData(
//        uuid: String,
//        major: Int,
//        minor: Int,
//        txPower: Int
//    ): ByteArray {
//        val uuidBytes = uuid.replace("-", "")
//            .chunked(2)
//            .map {it.toInt(16).toByte()}
//            .toByteArray()
//        val majorBytes = byteArrayOf((major shr 8).toByte(), major.toByte())
//        val minorBytes = byteArrayOf((minor shr 8).toByte(), minor.toByte())
//        return byteArrayOf(
//            0x02, 0x15
//        ) + uuidBytes + majorBytes + minorBytes + byteArrayOf(txPower.toByte())
//    }
//
//    private val advertiseCallback = object : AdvertiseCallback() {
//        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
//            Log.i("BLEBeacon", "iBeacon advertising started successfully")
//        }
//
//        override fun onStartFailure(errorCode: Int) {
//            Log.e("BLEBeacon", "iBeacon advertising failed with error code: $errorCode")
//        }
//    }
//
//    private fun hasPermissions(): Boolean {
//        val permissions = if (
//            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
//        ) {
//            arrayOf(
//                Manifest.permission.BLUETOOTH_ADVERTISE,
//                Manifest.permission.BLUETOOTH_SCAN,
//                Manifest.permission.BLUETOOTH_CONNECT
//            )
//        } else {
//            arrayOf(
//                Manifest.permission.ACCESS_FINE_LOCATION
//            )
//        }
//
//        return permissions.all {
//            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
//        }
//    }
//}