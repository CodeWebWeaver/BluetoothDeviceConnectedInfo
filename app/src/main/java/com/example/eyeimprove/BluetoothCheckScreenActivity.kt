package com.example.eyeimprove

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class BluetoothCheckScreenActivity : AppCompatActivity() {

    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter

    private val bluetoothPermissionRequestCode = 123 // Выберите код запроса по вашему усмотрению

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.bluetooth_check_screen)

        bluetoothManager = getSystemService(BluetoothManager::class.java) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        val bluetoothCheckButton : Button = findViewById(R.id.bluetoothCheckButton)
        bluetoothCheckButton.setOnClickListener{
            manageBluetoothStatus()
        }
    }

    fun manageBluetoothStatus() {
        if (bluetoothAdapter.isEnabled) {
            // Bluetooth уже включен, перейти к PickerScreen
            navigateToPickerScreen()
            return
        } else {
            BluetoothManagerActivity().enableBluetooth()
            if (bluetoothAdapter.isEnabled) {
                navigateToPickerScreen()
            }
        }
    }

    fun navigateToPickerScreen() {
        val intent = Intent(this, PickerScreenActivity::class.java)
        if (intent.resolveActivity(packageManager) != null) {
            // Активити существует, можно использовать интент
            startActivity(intent)
        } else {
            // Активити не найдена
            Toast.makeText(this, "Activity not found", Toast.LENGTH_SHORT).show()
        }
    }
}