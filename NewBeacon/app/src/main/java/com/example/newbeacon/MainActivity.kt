package com.example.newbeacon

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import org.altbeacon.beacon.Beacon
import org.altbeacon.beacon.BeaconParser
import org.altbeacon.beacon.BeaconTransmitter

class MainActivity : ComponentActivity() {
    private lateinit var statusTextView: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var radioGroup: RadioGroup
    private lateinit var radioButton: RadioButton

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var beaconTransmitter: BeaconTransmitter? = null
    private var beacon: Beacon? = null
    private var isAdvertising = false

//    private var selectedBus: String = ""
    private val busHash: HashMap<String, String> =
        HashMap()

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1234
    }
    private val requiredPermissions = arrayOf(
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.BLUETOOTH_ADMIN,
        android.Manifest.permission.BLUETOOTH_ADVERTISE,
        android.Manifest.permission.BLUETOOTH_SCAN,
        android.Manifest.permission.BLUETOOTH_CONNECT
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        val beaconParser = BeaconParser()
            .setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24")

        if (beaconTransmitter == null) {
            beaconTransmitter = BeaconTransmitter(applicationContext, beaconParser)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        busHash["TN 015 0123"] = "27B2F751-228D-4BEA-8A06-F8ADC74388E6".lowercase()
        busHash["KA 321 3210"] = "ACA22A9D-06B2-4789-9669-9313B2F5605A".lowercase()

        statusTextView = findViewById(R.id.statusTextView)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        radioGroup = findViewById(R.id.busSelect)

        radioGroup.clearCheck()

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "This device does not support Bluetooth.", Toast.LENGTH_LONG).show()
            finish() // Close the app
            return
        }

        if (!bluetoothAdapter!!.isEnabled) {
            promptEnableBluetooth()
        }

        if (!arePermissionsGranted()) {
            requestPermissions(requiredPermissions, PERMISSION_REQUEST_CODE)
        } else {
            initializeBeacon()
        }

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            radioButton = findViewById(checkedId)

            Toast.makeText(
                this@MainActivity,
                "Selected Bus is : " + radioButton.text,
                Toast.LENGTH_SHORT
            ).show()
        }

        startButton.setOnClickListener {
            if (!isAdvertising) {
                startAdvertising()
            }
        }

        stopButton.setOnClickListener {
            if (isAdvertising) {
                stopAdvertising()
            }
        }
    }

    private fun promptEnableBluetooth() {
        Toast.makeText(this, "Please enable bluetooth to start advertising", Toast.LENGTH_LONG).show()
        startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
    }

    private fun initializeBeacon(): Int {
        val checkedButton = radioGroup.checkedRadioButtonId
        if (checkedButton == -1) {
            Toast.makeText(this, "Please choose a bus", Toast.LENGTH_LONG).show()
            return -1
        }
        val buttonText = findViewById<RadioButton>(checkedButton).text
        val id1 = busHash[buttonText]

        if (id1 == null) {
            Toast.makeText(this, "Bus not found", Toast.LENGTH_LONG).show()
            return -1
        }

        beacon = Beacon.Builder()
            .setId1(id1)
            .setId2("1")
            .setId3("59")
            .setManufacturer(0x4C00)
            .setTxPower(-59)
            .build()

        return 0
    }

    private fun startAdvertising() {
        val ret = initializeBeacon()
        if (ret == -1) {
            Toast.makeText(this, "Unable to start beacon", Toast.LENGTH_LONG).show()
            return
        }

        if (isAdvertising) {
            Toast.makeText(this, "Already advertising", Toast.LENGTH_SHORT).show()
            return
        }

        if (!arePermissionsGranted()) {
            requestPermissions(requiredPermissions, PERMISSION_REQUEST_CODE)
            return
        }

        if (beacon != null && beaconTransmitter != null) {
            beacon?.let {
                beaconTransmitter?.startAdvertising(beacon, object: AdvertiseCallback() {
                    override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                        isAdvertising = true
                        val busText: String = busHash[radioButton.text]?: "Unknown bus"
                        statusTextView.text = getString(R.string.advertising_success, radioButton.text, busText)
                        startButton.visibility = View.GONE
                        stopButton.visibility = View.VISIBLE
                        radioGroup.visibility = View.GONE
                        Log.i("iBeacon", "Beacon advertising started successfully")
                    }

                    override fun onStartFailure(errorCode: Int) {
                        statusTextView.text = getString(R.string.advertising_failure, errorCode)
                        Log.e("iBeacon", "Beacon advertising failed: ($errorCode)")
                    }
                })
            } ?: Toast.makeText(this, "Beacon not initialized!", Toast.LENGTH_SHORT).show()
        } else {
            Log.e("BeaconError", "Beacon or BeaconTransmitter is null.")
        }
    }

    private fun stopAdvertising() {
        beaconTransmitter?.stopAdvertising()
        isAdvertising = false
        statusTextView.text = getString(R.string.stop_advertising)
        startButton.visibility = View.VISIBLE
        stopButton.visibility = View.GONE
        radioGroup.visibility = View.VISIBLE
        Log.i("iBeacon", "Beacon Advertising stopped")
    }

    private fun arePermissionsGranted(): Boolean {
        return requiredPermissions.all {permission ->
            checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
        deviceId: Int
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "Permissions granted! Ready to advertise.", Toast.LENGTH_SHORT).show()
                startAdvertising()
            } else {
                Toast.makeText(this, "Permissions denied. Cannot advertise.", Toast.LENGTH_SHORT).show()
                startButton.isEnabled = false
                stopButton.isEnabled = false
                radioGroup.isEnabled = false
            }
        }
    }
}