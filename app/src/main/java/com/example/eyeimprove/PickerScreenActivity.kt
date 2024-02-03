package com.example.eyeimprove

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.reflect.Method
import java.util.UUID

class PickerScreenActivity : AppCompatActivity() {

    //Connection Parameters
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothManager: BluetoothManager
    private val bluetoothPermissionRequestCode = 123
    private var bluetoothPermissionCallback: ((Boolean) -> Unit)? = null
    private var connectedDevices: MutableList<BluetoothDevice> = mutableListOf()

    //Communication parameters
    private val UUID_STRING_WELL_KNOWN : String = "00001101-0000-1000-2000-00805F9B34FB"
    private val WELL_KNOWN_UUID: UUID = UUID.fromString(UUID_STRING_WELL_KNOWN)
    private lateinit var bluetoothSocket: BluetoothSocket
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.device_picker_screen)
        bluetoothManager = getSystemService(BluetoothManager::class.java) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
    }

    private suspend fun getPairedDeviceInfo() {
        // Асинхронна операція отримання списку з'єднаних пристроїв
        checkBluetoothPermission { isPermissionGranted ->
            if (isPermissionGranted) {
                // Действия, которые выполняются при наличии разрешения
                Toast.makeText(this@PickerScreenActivity,
                    "Bluetooth permission granted!",
                    Toast.LENGTH_SHORT).show()
                enableBluetooth()
            } else {
                // Действия, которые выполняются при отсутствии разрешения
                Toast.makeText(this@PickerScreenActivity,
                    "Bluetooth permission denied!",
                    Toast.LENGTH_SHORT).show()
            }
        }
        val pairedDevices = bluetoothAdapter.bondedDevices

        if (pairedDevices.isEmpty()) {
            lifecycleScope.launch(Dispatchers.Main) {
                showToastOnMainThread("No paired Bluetooth devices found")
                //displayDeviceInfo(connectedDevices)
            }
        } else {
            val connectedDevices = pairedDevices.filter { isConnected(it) }

            if (connectedDevices.isEmpty()) {
                lifecycleScope.launch(Dispatchers.Main) {
                    showToastOnMainThread("Connected Bluetooth devices not found")
                    //displayDeviceInfo(connectedDevices)
                }
            } else {
                // Зміни в UI повинні відбуватися на основному потоці
                lifecycleScope.launch(Dispatchers.Main) {
                    //displayDeviceInfo(connectedDevices)
                }
            }
        }
        delay(5000)
        getPairedDeviceInfo()
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
                    Toast.makeText(this, "Bluetooth disabled", Toast.LENGTH_SHORT)
                        .show()
                }
            }

            // Use the contract to launch the system enable Bluetooth dialog
            requestEnableContract.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }
    }

    /*private fun displayDeviceInfo(devices: List<BluetoothDevice>) {
        checkBluetoothPermission()
        val deviceNames = devices.joinToString("\n") { it.name ?: "Unknown" }
        val deviceInfos = devices.joinToString("\n") { it.address ?: "None"}

        bluetoothDeviceText.text = getString(R.string.bluetooth_device_text, deviceNames)
        bluetootInfoText.text = getString(R.string.device_info_text, deviceInfos)
    }*/

    private fun isConnected(device: BluetoothDevice): Boolean {
        return try {
            val m: Method = device.javaClass.getMethod("isConnected")
            m.invoke(device) as Boolean
        } catch (e: Exception) {
            throw IllegalStateException(e)
        }
    }

    private fun checkBluetoothPermission(callback: (Boolean) -> Unit) {
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

    private fun showToastOnMainThread(message: String) {
        Toast.makeText(this@PickerScreenActivity, message, Toast.LENGTH_SHORT).show()
    }

    private fun navigateToBluetoothCheckScreen(){
        val intent = Intent(this, BluetoothCheckScreenActivity::class.java)
        if (intent.resolveActivity(packageManager) != null) {
            // Активити существует, можно использовать интент
            startActivity(intent)
        } else {
            // Активити не найдена
            Toast.makeText(this, "Activity not found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToControlPanelScreen(){
        val intent = Intent(this, ControlPanelActivity::class.java)
        if (intent.resolveActivity(packageManager) != null) {
            // Активити существует, можно использовать интент
            startActivity(intent)
        } else {
            // Активити не найдена
            Toast.makeText(this, "Activity not found", Toast.LENGTH_SHORT).show()
        }
    }
}