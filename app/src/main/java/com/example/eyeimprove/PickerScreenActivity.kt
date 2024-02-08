package com.example.eyeimprove

import DevicesAdapter
import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.lang.reflect.Method

class PickerScreenActivity : AppCompatActivity(), DevicesAdapter.OnDeviceClickListener {

    //Connection Parameters
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothManager: BluetoothManager
    private val bluetoothPermissionRequestCode = 123
    private var bluetoothPermissionCallback: ((Boolean) -> Unit)? = null

    private var connectedDevices: MutableList<BluetoothDevice> = mutableListOf()
    private val connectedDevicesMap = hashMapOf<String, String>()

    private var selectedDevice: BluetoothDevice? = null


    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action

            when (action) {
                BluetoothDevice.ACTION_ACL_CONNECTED, BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    val device =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE) as? BluetoothDevice?

                    checkBluetoothPermission()
                    if (device != null) {
                        when (action) {
                            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                                connectedDevices.add(device)
                                connectedDevicesMap[device.address] = device.name
                                runOnUiThread {
                                    updateOrDisplayDeviceInfo(connectedDevicesMap)
                                }

                            }

                            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                                connectedDevices.remove(device)
                                connectedDevicesMap.remove(device.address)
                                runOnUiThread {
                                    // Обновите список устройств на UI*******************
                                    updateOrDisplayDeviceInfo(connectedDevicesMap)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private val bluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Callback-функция
        if (!isGranted) {
            showToast("Bluetooth permission denied!")
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.device_picker_screen)

        bluetoothManager = getSystemService(BluetoothManager::class.java) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        val returnButton = findViewById<Button>(R.id.picker_return_button)
        returnButton.setOnClickListener {
            navigateToActivity(BluetoothCheckScreenActivity::class.java)
        }

        val submitButton = findViewById<Button>(R.id.picker_submit_button)
        submitButton.setOnClickListener {
            selectedDevice?.let { device ->
                val intent = Intent(this, ControlPanelActivity::class.java).apply {
                    putExtra("selectedDeviceAddress", device.address) // Передача адреса устройства
                }
                if (intent.resolveActivity(packageManager) != null) {
                    // Активити существует, можно использовать интент
                    startActivity(intent)
                } else {
                    // Активити не найдена
                    Toast.makeText(this, "Activity not found", Toast.LENGTH_SHORT).show()
                }
            }
        }


        val combinedFilter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        registerReceiver(bluetoothReceiver, combinedFilter)
        getPairedDeviceInfo();
    }

    private fun getPairedDeviceInfo() {
        checkBluetoothPermission { isPermissionGranted ->
            if (isPermissionGranted && bluetoothAdapter.isEnabled) {
                // Действия, которые выполняются при наличии разрешения
                val pairedDevices = bluetoothAdapter.bondedDevices

                if (pairedDevices.isEmpty()) {
                    showToast("No paired Bluetooth devices found")
                } else {
                    connectedDevices = pairedDevices.filter { isConnected(it) }.toMutableList()

                    if (connectedDevices.isEmpty()) {
                        showToast("Connected Bluetooth devices not found")
                    } else {
                        // Зміни в UI повинні відбуватися на основному потоці
                        connectedDevicesMap.putAll(connectedDevices.associateBy({ it.address }, { it.name }))
                        updateOrDisplayDeviceInfo(connectedDevicesMap)
                    }
                }

            } else {
                // Действия, которые выполняются при отсутствии разрешения
                Toast.makeText(this@PickerScreenActivity,
                    "Bluetooth permission denied!",
                    Toast.LENGTH_SHORT).show()
            }
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

    private fun updateOrDisplayDeviceInfo(devices: HashMap<String, String>) {
        val recyclerView = findViewById<RecyclerView>(R.id.connected_devices_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val adapter = recyclerView.adapter as? DevicesAdapter ?: DevicesAdapter(devices)
        adapter.setOnDeviceClickListener(this)
        adapter.updateData(devices)

        recyclerView.adapter = adapter
        adapter.notifyDataSetChanged()
    }

    private fun checkBluetoothPermission() {
        bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH)
    }

    private fun checkBluetoothPermission(callback: (Boolean) -> Unit) {
        bluetoothPermissionCallback = callback

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Запрос Bluetooth-разрешения
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH),
                bluetoothPermissionRequestCode
            )
        } else {
            // Разрешение уже предоставлено
            callback(true)
        }
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

    override fun onDeviceClick(deviceAddress: String) {
        Log.i("INFO", "Було натиснуто пристрій ${connectedDevicesMap.get(deviceAddress)} \n з адресом $deviceAddress")
        selectedDevice = connectedDevices.firstOrNull { it.address == deviceAddress }
    }
}