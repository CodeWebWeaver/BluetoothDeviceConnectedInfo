package com.example.eyeimprove

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.reflect.Method

class BluetoothManagerActivity : AppCompatActivity() {


    private  var bluetoothManager = getSystemService(BluetoothManager::class.java) as BluetoothManager
    private var bluetoothAdapter = bluetoothManager.adapter

    private val bluetoothPermissionRequestCode = 123
    private var bluetoothPermissionCallback: ((Boolean) -> Unit)? = null

    private var connectedDevices: MutableList<BluetoothDevice> = mutableListOf()
    private val connectedDevicesMap = hashMapOf<String, String>()

    val requestEnableContract = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            Toast.makeText(this, "Bluetooth enabled", Toast.LENGTH_SHORT)
                .show()
            BluetoothCheckScreenActivity().navigateToPickerScreen()
        } else {
            // Bluetooth not enabled, inform user
            Toast.makeText(this, "Bluetooth disabled", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            val currentActivity = this::class.simpleName

            when (action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    if (currentActivity == "BluetoothCheckScreenActivity") {
                        // Пользователь уже находится в BluetoothCheckScreenActivity, ничего не делать
                        return
                    }
                    val state =
                        intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF)
                    if (state == BluetoothAdapter.STATE_OFF) {
                        // Bluetooth выключен, перенаправить пользователя
                        val intent = Intent(context, BluetoothCheckScreenActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context?.startActivity(intent)
                    }
                }

                BluetoothDevice.ACTION_ACL_CONNECTED, BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    // Обработка событий подключения/отключения устройств
                    val device =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE) as? BluetoothDevice?

                    checkBluetoothPermission()
                    if (device != null) {
                        when (action) {
                            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                                connectedDevices.add(device)
                                connectedDevicesMap[device.name] = device.address
                                // Обновите список устройств на UI*******************
                            }

                            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                                connectedDevices.remove(device)
                                connectedDevicesMap.remove(device.name)
                                // Обновите список устройств на UI*******************
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
        if (isGranted) {

        } else {
            showToast("Bluetooth permission denied!")
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val combinedFilter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        registerReceiver(bluetoothReceiver, combinedFilter)


        bluetoothManager = getSystemService(BluetoothManager::class.java) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bluetoothReceiver)
    }

    private fun getPairedDeviceInfo() {
        checkBluetoothPermission()
        if (bluetoothAdapter.isEnabled) {
            // Действия, которые выполняются при наличии разрешения
            val pairedDevices = bluetoothAdapter.bondedDevices

            if (pairedDevices.isEmpty() || pairedDevices == null) {
                lifecycleScope.launch(Dispatchers.Main) {
                    showToast("No paired Bluetooth devices found")
                }
            } else {
                connectedDevices = pairedDevices.filter { isConnected(it) }.toMutableList()

                if (connectedDevices.isEmpty()) {
                    showToast("Connected Bluetooth devices not found")
                } else {
                    connectedDevicesMap.putAll(connectedDevices.associateBy({ it.name }, { it.address }))
                }
            }
        }
    }
    private fun updateDeviceList() {
        // Обновите список устройств
        // ...
    }

    private fun isConnected(device: BluetoothDevice): Boolean {
        return try {
            val m: Method = device.javaClass.getMethod("isConnected")
            m.invoke(device) as Boolean
        } catch (e: Exception) {
            throw IllegalStateException(e)
        }
    }



    fun enableBluetooth() {
        if (!bluetoothAdapter.isEnabled) {
            // Use the contract to launch the system enable Bluetooth dialog
            requestEnableContract.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }
    }

    private fun checkBluetoothPermissionBoolean(callback: (Boolean) -> Unit) {
        bluetoothPermissionCallback = callback

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Запрос Bluetooth-разрешения
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                bluetoothPermissionRequestCode
            )
        } else {
            // Разрешение уже предоставлено
            callback(true)
        }
    }


    private fun checkBluetoothPermission() {
        bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

}