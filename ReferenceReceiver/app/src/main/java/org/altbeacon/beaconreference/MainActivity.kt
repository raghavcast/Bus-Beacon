package org.altbeacon.beaconreference

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import org.altbeacon.beacon.Beacon
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.permissions.BeaconScanPermissionsActivity

class MainActivity : AppCompatActivity() {
    lateinit var beaconListView: ListView
    lateinit var beaconCountTextView: TextView
//    lateinit var monitoringButton: Button
    lateinit var rangingButton: Button
    lateinit var beaconReferenceApplication: BeaconReferenceApplication
//    var alertDialog: AlertDialog? = null
    private val busHash: HashMap<String, String> =
        HashMap<String, String>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        beaconReferenceApplication = application as BeaconReferenceApplication
        busHash["27B2F751-228D-4BEA-8A06-F8ADC74388E6".lowercase()] = "TN 015 0123"
        busHash["ACA22A9D-06B2-4789-9669-9313B2F5605A".lowercase()] = "KA 321 3210"
        // Set up a Live Data observer for beacon data
        val regionViewModel = BeaconManager.getInstanceForApplication(this).getRegionViewModel(beaconReferenceApplication.region)
        regionViewModel.rangedBeacons.observe(this, rangingObserver)
        rangingButton = findViewById<Button>(R.id.rangingButton)
        beaconListView = findViewById<ListView>(R.id.beaconList)
        beaconCountTextView = findViewById<TextView>(R.id.beaconCount)
        beaconCountTextView.text = "Ranging disabled -- No beacons detected"
        beaconListView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, arrayOf("--"))
    }

    override fun onPause() {
        Log.d(TAG, "onPause")
        super.onPause()
    }
    override fun onResume() {
        Log.d(TAG, "onResume")
        super.onResume()
        // You MUST make sure the following dynamic permissions are granted by the user to detect beacons
        //
        //    Manifest.permission.BLUETOOTH_SCAN
        //    Manifest.permission.BLUETOOTH_CONNECT
        //    Manifest.permission.ACCESS_FINE_LOCATION
        //    Manifest.permission.ACCESS_BACKGROUND_LOCATION // only needed to detect in background
        //
        // The code needed to get these permissions has become increasingly complex, so it is in
        // its own file so as not to clutter this file focussed on how to use the library.

        if (!BeaconScanPermissionsActivity.allPermissionsGranted(this,
                true)) {
            val intent = Intent(this, BeaconScanPermissionsActivity::class.java)
            intent.putExtra("backgroundAccessRequested", true)
            startActivity(intent)
        }
        else {
            if (BeaconManager.getInstanceForApplication(this).monitoredRegions.size == 0) {
                (application as BeaconReferenceApplication).setupBeaconScanning()
            }
        }
    }

    val rangingObserver = Observer<Collection<Beacon>> { beacons ->
        Log.d(TAG, "Ranged: ${beacons.count()} beacons")
        if (BeaconManager.getInstanceForApplication(this).rangedRegions.size > 0) {
            beaconCountTextView.text = "Ranging enabled: ${beacons.count()} beacon(s) detected"
            beaconListView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1,
                beacons
                    .sortedBy { it.distance }
                    .map { beacon ->
                        val id1 = beacon.id1?.toString() ?: "Unknown UUID"
                        val id2 = beacon.id2?.toString() ?: "Unknown Major"
                        val id3 = beacon.id3?.toString() ?: "Unknown Minor"
                        val rssi = beacon.rssi
                        val txPower = beacon.txPower
                        val busInfo = busHash[id1] ?: "Unknown Bus"
                        Log.d("Strength", "Beacon RSSI: ${beacon.rssi}, distance: ${beacon.distance}")
                        "$busInfo\nuuid: $id1\nmajor: $id2 minor: $id3 rssi: ${rssi} txPower: $txPower\nest. distance: ${beacon.distance} m" }.toTypedArray())
        }
    }

    fun rangingButtonTapped(view: View) {
        val beaconManager = BeaconManager.getInstanceForApplication(this)
        if (beaconManager.rangedRegions.size == 0) {
            beaconManager.startMonitoring(beaconReferenceApplication.region)
            beaconManager.startRangingBeacons(beaconReferenceApplication.region)
            rangingButton.text = "Stop Ranging"
            beaconCountTextView.text = "Ranging enabled -- awaiting first callback"
        }
        else {
            beaconManager.stopRangingBeacons(beaconReferenceApplication.region)
            beaconManager.stopMonitoring(beaconReferenceApplication.region)
            rangingButton.text = "Start Ranging"
            beaconCountTextView.text = "Ranging disabled -- no beacons detected"
            beaconListView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, arrayOf("--"))
        }
    }

    companion object {
        val TAG = "MainActivity"
        val PERMISSION_REQUEST_BACKGROUND_LOCATION = 0
        val PERMISSION_REQUEST_BLUETOOTH_SCAN = 1
        val PERMISSION_REQUEST_BLUETOOTH_CONNECT = 2
        val PERMISSION_REQUEST_FINE_LOCATION = 3
    }

}
