package com.example.testbeacon

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.AdvertisingSet
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {
    private lateinit var statusTextView: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var radioGroup: RadioGroup
    private lateinit var radioButton: RadioButton
    private lateinit var bluetoothLeAdvertiser: BluetoothLeAdvertiser
    private lateinit var bluetoothAdapter: BluetoothAdapter

    private var isAdvertising = false

//    private var selectedBus: String = ""
    private val busHash: HashMap<String, String> =
        HashMap()

    private val settings = AdvertiseSettings.Builder()
        .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
        .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
        .setConnectable(false)
        .build()

    private var advertiseCallback: AdvertiseCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        supportActionBar!!.hide()

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        bluetoothLeAdvertiser = bluetoothAdapter.bluetoothLeAdvertiser

        if (!bluetoothAdapter.isEnabled) {
            promptEnableBluetooth()
            Log.e("iBeacon", "Bluetooth is off or not supported; cannot stop advertising")
            return
        }

        busHash["TN 015 0123"] = "27B2F751-228D-4BEA-8A06-F8ADC74388E6".lowercase()
        busHash["KA 321 3210"] = "ACA22A9D-06B2-4789-9669-9313B2F5605A".lowercase()

        statusTextView = findViewById(R.id.statusTextView)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        radioGroup = findViewById(R.id.busSelect)

        radioGroup.clearCheck()

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            radioButton = findViewById(checkedId)

            Toast.makeText(
                this@MainActivity,
                "Selected Bus is : " + radioButton.text,
                Toast.LENGTH_SHORT
            ).show()
        }

//        bleBeaconManager = BLEBeaconManager(this)
        if (!hasPermissions()) {
            requestPermissions()
        }

        startButton.setOnClickListener {
            if (!isAdvertising) {
                startBeacon()
            }
        }

        stopButton.setOnClickListener {
            if (isAdvertising) {
//                Log.i("Callback", "Reacting to button")
                stopBeacon()
            }
        }
    }

    private fun startBeacon() {
        if(!hasPermissions()) {
            Log.e("BLEBeacon", "Required permissions are not granted")
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            Log.e("iBeacon", "Bluetooth is off or not supported; cannot stop advertising")
            return
        }

        if (advertiseCallback == null) {
            advertiseCallback = object : AdvertiseCallback() {
                override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                    Log.i("Callback", "Advertising started successfully")
                }

                override fun onStartFailure(errorCode: Int) {
                    Log.e("Callback", "Advertising failed with error code: $errorCode")
                }
            }
        }

//        if (bluetoothLeAdvertiser == null) {
//            Log.e("BLEBeacon", "BLE Advertising is not supported on this device.")
//            return
//        }

        val checkedButton = radioGroup.checkedRadioButtonId
        if (checkedButton == -1) {
            Toast.makeText(this, "Please choose a bus", Toast.LENGTH_LONG).show()
            return
        }
        val buttonText = findViewById<RadioButton>(checkedButton).text
        val uuid = busHash[buttonText]

        if (uuid == null) {
            Toast.makeText(this, "Bus not found", Toast.LENGTH_LONG).show()
            return
        }
        // Everything needs to be changed into calls to a database of some kind.
        // uuid should be unique for each state, major, minor to be decided, don't fully understand txPower yet
        val beaconData = createIBeaconData(
            uuid = uuid,
            major = 1,
            minor = 59
        )

        val data = AdvertiseData.Builder()
            .addManufacturerData(0x4C00, beaconData) // 0x4C00 is Apple's Manufacturer ID
            .build()

//        if (ActivityCompat.checkSelfPermission(
//                this,
//                Manifest.permission.BLUETOOTH_ADVERTISE
//            ) != PackageManager.PERMISSION_GRANTED
//        ) {
//            requestPermissions()
//        }

        try {
            bluetoothLeAdvertiser.startAdvertising(settings, data, advertiseCallback)
            Log.i("iBeacon", "Beacon advertising started successfully")
        } catch (e: Exception) {
            Log.e("iBeacon", "Beacon advertising failed: ($e.message)")
        }
        isAdvertising = true
        val busText: String = busHash[radioButton.text]?: "Unknown bus"
        statusTextView.text = getString(R.string.advertising_success, radioButton.text, busText)
        startButton.visibility = View.GONE
        stopButton.visibility = View.VISIBLE
        radioGroup.visibility = View.GONE
    }

    private fun stopBeacon() {
        if (advertiseCallback == null) {
            Log.e("Callback", "AdvertiseCallback is null; cannot stop advertising")
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            Log.e("iBeacon", "Bluetooth is off or not supported; cannot stop advertising")
            return
        }

        if(!hasPermissions()) {
            Log.e("BLEBeacon", "Required permissions are not granted")
            return
        }

//        if (ActivityCompat.checkSelfPermission(
//                this,
//                Manifest.permission.BLUETOOTH_ADVERTISE
//            ) != PackageManager.PERMISSION_GRANTED
//        ) {
//            requestPermissions()
//        }

        try {
            bluetoothLeAdvertiser.stopAdvertising(advertiseCallback)
            Log.i("iBeacon", "Advertising stopped successfully")
        } catch (e: Exception) {
            Log.e("iBeacon", "Error stopping advertising: ${e.message}")
        }

        isAdvertising = false
        statusTextView.text = getString(R.string.stop_advertising)
        startButton.visibility = View.VISIBLE
        stopButton.visibility = View.GONE
        radioGroup.visibility = View.VISIBLE
    }

    private fun createIBeaconData(
        uuid: String,
        major: Int,
        minor: Int,
    ): ByteArray {
        val uuidBytes = uuid.replace("-", "")
            .chunked(2)
            .map {it.toInt(16).toByte()}
            .toByteArray()
        val majorBytes = byteArrayOf((major shr 8).toByte(), major.toByte())
        val minorBytes = byteArrayOf((minor shr 8).toByte(), minor.toByte())
        return byteArrayOf(
            0x02, 0x15
        ) + uuidBytes + majorBytes + minorBytes + byteArrayOf((-59).toByte())
    }

    private fun hasPermissions(): Boolean {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADMIN
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }

        return permissions.all {
            ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun promptEnableBluetooth() {
        Toast.makeText(this, "Please enable bluetooth to start advertising", Toast.LENGTH_LONG).show()
        startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
    }

    private fun requestPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }

        ActivityCompat.requestPermissions(this, permissions, 1)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        if(hasPermissions()) {
//            startBeacon()
//        }
    }
}
