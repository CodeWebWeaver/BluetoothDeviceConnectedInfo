package com.example.eyeimprove

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.lang.reflect.Method
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private val UUID_STRING_WELL_KNOWN : String = "00001101-0000-1000-2000-00805F9B34FB"
    private val WELL_KNOWN_UUID: UUID = UUID.fromString(UUID_STRING_WELL_KNOWN)
    private val REQUEST_ENABLE_BT = 1

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothSocket: BluetoothSocket
    private lateinit var handler: Handler
    private lateinit var textView: TextView
    private lateinit var bluetoothDeviceText: TextView
    private lateinit var bluetootInfoText: TextView
    private var connectedDevices: MutableList<BluetoothDevice> = mutableListOf()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bluetoothDeviceText = findViewById(R.id.bluetoothDevice)
        bluetootInfoText = findViewById(R.id.bluetoothInfo)
        textView = findViewById(R.id.temperatureTextView)

        bluetoothManager = getSystemService(BluetoothManager::class.java) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        checkBluSupport()
        checkBluetoothPermission()
        enableBluetooth()
        getPairedDeviceInfo()
    }

    fun getPairedDeviceInfo() {

        val runnable = object : Runnable {
            override fun run() {
                checkBluetoothPermission()

                val pairedDevices = bluetoothAdapter.bondedDevices


                if (pairedDevices.isEmpty()) {
                    Toast.makeText(this@MainActivity, "No paired Bluetooth devices found", Toast.LENGTH_SHORT).show()
                    handler.postDelayed(this, 5000)
                } else {
                    val deviceNames = StringBuffer()
                    val deviceInfos = StringBuffer()

                    for(device in pairedDevices)
                    {
                        if(isConnected((device))) {
                            connectedDevices.add(device)
                            deviceNames.append(" " + device.name + "\n")
                            deviceInfos.append(" " + device.address + "\n")
                        }
                    }

                    if (deviceNames.isEmpty()) {
                        Toast.makeText(this@MainActivity, "Connected Bluetooth devices not found"
                                + deviceNames, Toast.LENGTH_SHORT).show()
                        handler.postDelayed(this, 5000)
                    } else {
                        Toast.makeText(this@MainActivity, "Paired Bluetooth devices found"
                                + deviceNames, Toast.LENGTH_SHORT).show()
                        // Found paired devices, display information about the first one
                        bluetoothDeviceText.text = getString(R.string.bluetooth_device_text, deviceNames)
                        bluetootInfoText.text = getString(R.string.device_info_text, deviceInfos)
                    }
                }
            }
        }

        handler = Handler(Looper.getMainLooper())
        handler.postDelayed(runnable, 5000)
    }

    private fun checkBluetoothPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request Bluetooth permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                REQUEST_ENABLE_BT
            )
            finish()
            return
        }
    }

    private fun enableBluetooth() {
        if (!bluetoothAdapter.isEnabled) {
            // Create ActivityResultContract for enabling Bluetooth
            val requestEnableContract = registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == RESULT_OK) {
                    Toast.makeText(this, "Bluetooth enabled", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    // Bluetooth not enabled, inform user
                    Toast.makeText(this, "Bluetooth not enabled", Toast.LENGTH_SHORT)
                        .show()
                }
            }

            // Use the contract to launch the system enable Bluetooth dialog
            requestEnableContract.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }
    }

    private fun checkBluSupport() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported", Toast.LENGTH_SHORT)
                .show()
            finish()
            return
        }
    }

    private fun isConnected(device: BluetoothDevice): Boolean {
        return try {
            val m: Method = device.javaClass.getMethod("isConnected")
            m.invoke(device) as Boolean
        } catch (e: Exception) {
            throw IllegalStateException(e)
        }
    }
}

