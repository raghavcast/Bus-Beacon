package com.example.testreceiver


import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.testreceiver.databinding.ActivityMainBinding
import com.example.testreceiver.ui.theme.TestReceiverTheme
import kotlin.system.exitProcess

class MainActivity : ComponentActivity() {

    private lateinit var binding: ActivityMainBinding
    private val TAG = "MainActivity"

    private lateinit var scanService: ScanService
    private lateinit var adapter: DeviceListAdapter
    private lateinit var deviceList: ArrayList<Any>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.scanBtn.setOnClickListener { startScan() }
        binding.exitBtn.setOnClickListener { exitApp() }
        val recycleView: RecyclerView = findViewById(R.id.deviceList)
        deviceList = ArrayList()
        this.adapter = DeviceListAdapter(this.deviceList)
        recycleView.adapter = this.adapter

        if (isPermissionGranted(this)) {
            Log.d(TAG, "@onCreate init scan service")
            scanService = ScanService(this, this.deviceList, this.adapter)
        }
    }

    private val BLE_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    // necessary permissions on Android >=12
    private val ANDROID_12_BLE_PERMISSIONS = arrayOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    private fun isPermissionGranted(context: Context): Boolean {
        Log.d(TAG, "@isPermissionGranted: checking bluetooth")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if ((ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED) ||
                (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED)
            ) {
                Log.d(TAG, "@isPermissionGranted: requesting Bluetooth on Android >= 12")
                ActivityCompat.requestPermissions(this, ANDROID_12_BLE_PERMISSIONS, 2)
                return false
            }
        } else {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.d(TAG, "@isPermissionGranted: requesting Location on Android < 12")
                ActivityCompat.requestPermissions(this, BLE_PERMISSIONS, 3)
                return false
            }
        }
        Log.d(TAG, "@isPermissionGranted Bluetooth permission is ON")
        return true
    }

    private fun exitApp() {
        // if scanning service is running, stop scan then exit
        if (scanService.isScanning()) {
            binding.scanBtn.text = resources.getString(R.string.label_scan)
            scanService.stopBLEScan()
        }
        this@MainActivity.finish()
        exitProcess(0)
    }


    private fun startScan() {
        // check Bluetooth
        if (!scanService.isBluetoothEnabled()) {
            Log.d(TAG, "@startScan Bluetooth is disabled")
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            requestBluetooth.launch(intent)
        } else {
            scanService.initScanner()
            // start scanning BLE device
            if (scanService.isScanning()) {
                binding.scanBtn.text = resources.getString(R.string.label_scan)
                scanService.stopBLEScan()
            } else {
                scanService.startBLEScan()
                binding.scanBtn.text = resources.getString(R.string.label_scanning)
            }
        }
    }

    fun onRadioAllClicked(view: View) {

    }

    fun onRadioiBeaconClicked(view: View) {

    }

    private var requestBluetooth =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                Log.d(TAG, "@requestBluetooth Bluetooth is enabled")
            } else {
                Log.d(TAG, "@requestBluetooth Bluetooth usage is denied")
            }
        }
}

@Composable
fun TextBox(name: String, modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "BLE Receiver",
            fontSize = 90.sp,
            lineHeight = 116.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(8.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    TestReceiverTheme {
        TextBox("Android")
    }
}