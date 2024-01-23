package com.example.eyeimprove

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.core.view.postDelayed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.reflect.Method
import java.util.UUID

class MainActivity : AppCompatActivity() {

    //private val UUID_STRING_WELL_KNOWN : String = "00001101-0000-1000-2000-00805F9B34FB"
    //private val WELL_KNOWN_UUID: UUID = UUID.fromString(UUID_STRING_WELL_KNOWN)
    private val REQUEST_ENABLE_BT = 1

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothSocket: BluetoothSocket
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
        lifecycleScope.launch(Dispatchers.Main) {
            getPairedDeviceInfo()
        }
    }

    suspend fun getPairedDeviceInfo() {
        withContext(Dispatchers.IO) {
            // Асинхронна операція отримання списку з'єднаних пристроїв
            checkBluetoothPermission()
            val pairedDevices = bluetoothAdapter.bondedDevices

            if (pairedDevices.isEmpty()) {
                showToastOnMainThread("No paired Bluetooth devices found")
                displayDeviceInfo(connectedDevices)
            } else {
                val connectedDevices = pairedDevices.filter { isConnected(it) }

                if (connectedDevices.isEmpty()) {
                    showToastOnMainThread("Connected Bluetooth devices not found")
                    displayDeviceInfo(connectedDevices)
                } else {
                    // Зміни в UI повинні відбуватися на основному потоці
                    withContext(Dispatchers.Main) {
                        displayDeviceInfo(connectedDevices)
                    }
                }
            }
            delay(5000)
            getPairedDeviceInfo()
        }
    }

    fun displayDeviceInfo(devices: List<BluetoothDevice>) {
        textView.postDelayed(500) {
            checkBluetoothPermission()
            val deviceNames = devices.joinToString("\n") { it.name ?: "Unknown" }
            val deviceInfos = devices.joinToString("\n") { it.address ?: "None"}

            bluetoothDeviceText.text = getString(R.string.bluetooth_device_text, deviceNames)
            bluetootInfoText.text = getString(R.string.device_info_text, deviceInfos)
        }
    }

    private suspend fun showToastOnMainThread(message: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
        }
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

