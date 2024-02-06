package com.example.eyeimprove

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.lang.reflect.Method
import java.util.UUID

class ControlPanelActivity : AppCompatActivity() {

    private var connectedDeviceAddress:  String? = null
    private var connectedDevice : BluetoothDevice? = null

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothManager: BluetoothManager

    private var connectedDevices: MutableList<BluetoothDevice> = mutableListOf()

        //Communication parameters
    private val UUID_STRING_WELL_KNOWN : String = "00001101-0000-1000-2000-00805F9B34FB"
    private val WELL_KNOWN_UUID: UUID = UUID.fromString(UUID_STRING_WELL_KNOWN)
    private lateinit var bluetoothSocket: BluetoothSocket

    private val bluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Callback-функция
        if (!isGranted) {
            showToast("Bluetooth permission denied!")
            finish()
        }
    }

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action

            when (action) {
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    val device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE) as? BluetoothDevice?
                    if (device?.address == connectedDeviceAddress) {
                        connectedDeviceAddress = null
                        connectedDevice = null
                        // Отключенное устройство - наше целевое устройство, возвращаемся на предыдущий экран
                        navigateToActivity(PickerScreenActivity::class.java)
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        registerReceiver(bluetoothReceiver, filter)
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(bluetoothReceiver)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.control_screen)

        checkBluetoothPermission()
        bluetoothManager = getSystemService(BluetoothManager::class.java) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        connectedDeviceAddress = intent.getStringExtra("selectedDeviceAddress")
        connectedDevice = getConnectedDevise(connectedDeviceAddress)

        Log.i("INFO", "Було отримано пристрій ${connectedDevice?.name} \n з адресом ${connectedDevice?.address}")

        val returnButton = findViewById<Button>(R.id.control_screen_return_button)
        returnButton.setOnClickListener {
            val intent = Intent(this, PickerScreenActivity::class.java)
            if (intent.resolveActivity(packageManager) != null) {
                // Активити существует, можно использовать интент
                startActivity(intent)
            } else {
                // Активити не найдена
                Toast.makeText(this, "Activity not found", Toast.LENGTH_SHORT).show()
            }
        }

        val submitButton = findViewById<Button>(R.id.control_screen_submit_button)
        submitButton.setOnClickListener {

        }
    }

    private fun getConnectedDevise(address: String?): BluetoothDevice? {
        checkBluetoothPermission()
        return bluetoothAdapter.bondedDevices
            .filter { isConnected(it) }
            .firstOrNull { it.address == address }
    }

    private fun checkBluetoothPermission() {
        bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun navigateToActivity(destinationActivity: Class<out Activity>) {
        val intent = Intent(this, destinationActivity)
        if (intent.resolveActivity(packageManager) != null) {
            // Активити существует, можно использовать интент
            startActivity(intent)
        } else {
            // Активити не найдена
            Toast.makeText(this, "Activity not found", Toast.LENGTH_SHORT).show()
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