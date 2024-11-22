package com.example.testreceiver

import android.bluetooth.le.ScanResult
import com.example.testreceiver.utils.ConversionUtils

class IBeacon(scanResult: ScanResult, packetData: ByteArray): BLEDevice(scanResult) {
    private var uuid: String = ""
    private var rawData: ByteArray = ByteArray(30)

    private var major: Int? = null
    private val majorPosStart = 25
    private val majorPosEnd = 26

    private var minor: Int? = null
    private val minorPosStart = 27
    private val minorPosEnd = 28

    init {
        rawData = packetData
    }

    private fun parseUUID() {
        var startByte = 2
        while (startByte <= 5) {
            if (rawData[startByte + 2].toInt() and 0xff == 0x02 && rawData[startByte + 3].toInt() and 0xff == 0x15) {
                val uuidBytes = ByteArray(16)
                System.arraycopy(rawData, startByte + 4, uuidBytes, 0, 16)
                val hexString = ConversionUtils.bytesToHex(uuidBytes)
                if (!hexString.isNullOrEmpty()) {
                    uuid = hexString.substring(0, 8) + "-" +
                            hexString.substring(8, 12) + "-" +
                            hexString.substring(12, 16) + "-" +
                            hexString.substring(16, 20) + "-" +
                            hexString.substring(20, 32)
                    return
                }
            }
            startByte++
        }
    }

    fun getUUID(): String {
        if (uuid.isNullOrEmpty()) {
            parseUUID()
        }
        return uuid
    }

    fun getMajor(): Int {
        if (major == null)
            major = (rawData[majorPosStart].toInt() and 0xff) * 0x100 + (rawData[majorPosEnd].toInt() and 0xff)
        return major as Int
    }

    fun getMinor(): Int {
        if (minor == null)
            minor = (rawData[minorPosStart].toInt() and 0xff) * 0x100 + (rawData[minorPosEnd].toInt() and 0xff)
        return minor as Int
    }

    override fun toString(): String {
        return "Major= " + major.toString() + " Minor= " + minor.toString() + "rssi= " + getRssi()
    }
}