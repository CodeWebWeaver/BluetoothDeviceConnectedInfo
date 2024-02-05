package com.example.eyeimprove

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
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
    private val connectedDevicesMap = hashMapOf<String, String>()

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
            navigateToActivity(ControlPanelActivity::class.java)
        }


        lifecycleScope.launch(Dispatchers.IO) {
            getPairedDeviceInfo();
        }

    }

    private suspend fun getPairedDeviceInfo() {
        // Асинхронна операція отримання списку з'єднаних пристроїв
        checkBluetoothPermission { isPermissionGranted ->
            if (isPermissionGranted && bluetoothAdapter.isEnabled) {
                // Действия, которые выполняются при наличии разрешения
                val pairedDevices = bluetoothAdapter.bondedDevices

                if (pairedDevices.isEmpty()) {
                    lifecycleScope.launch(Dispatchers.Main) {
                        showToastOnMainThread("No paired Bluetooth devices found")
                    }
                } else {
                    connectedDevices = pairedDevices.filter { isConnected(it) }.toMutableList()

                    if (connectedDevices.isEmpty()) {
                        lifecycleScope.launch(Dispatchers.Main) {
                            showToastOnMainThread("Connected Bluetooth devices not found")
                            //displayDeviceInfo(connectedDevices)
                        }
                    } else {
                        // Зміни в UI повинні відбуватися на основному потоці
                        lifecycleScope.launch(Dispatchers.Main) {
                            connectedDevicesMap.putAll(connectedDevices.associateBy({ it.name }, { it.address }))

                            displayDeviceInfo(connectedDevicesMap)
                        }
                    }
                }

            } else {
                // Действия, которые выполняются при отсутствии разрешения
                Toast.makeText(this@PickerScreenActivity,
                    "Bluetooth permission denied!",
                    Toast.LENGTH_SHORT).show()
            }
        }
        delay(5000)
        getPairedDeviceInfo()
    }

    private fun isConnected(device: BluetoothDevice): Boolean {
        return try {
            val m: Method = device.javaClass.getMethod("isConnected")
            m.invoke(device) as Boolean
        } catch (e: Exception) {
            throw IllegalStateException(e)
        }
    }

    private fun displayDeviceInfo(devices: HashMap<String, String>) {
        // Получить ссылку на RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.connected_devices_recycler_view)

        // Создать адаптер
        val adapter = DevicesAdapter(devices)

        // Установить адаптер для RecyclerView
        recyclerView.adapter = adapter
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

    class DevicesAdapter(private val dataSet: HashMap<String, String>) :
        RecyclerView.Adapter<DevicesAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val deviceName: TextView = view.findViewById(R.id.device_name)
            val deviceAddress: TextView = view.findViewById(R.id.device_address)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.device_card, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = dataSet.entries.toList()[position]
            holder.deviceName.text = item.key
            holder.deviceAddress.text = item.value
        }

        override fun getItemCount(): Int {
            return dataSet.size
        }
    }
}