package com.example.eyeimprove

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.PersistableBundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class FirstScreenActivity : AppCompatActivity() {

    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter

    private val bluetoothPermissionRequestCode = 123 // Выберите код запроса по вашему усмотрению

    private var bluetoothPermissionCallback: ((Boolean) -> Unit)? = null

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
        checkBluetoothPermission { isPermissionGranted ->
            if (isPermissionGranted) {
                // Действия, которые выполняются при наличии разрешения
                Toast.makeText(this@FirstScreenActivity,
                    "Bluetooth permission granted!",
                    Toast.LENGTH_SHORT).show()
                enableBluetooth()
            } else {
                // Действия, которые выполняются при отсутствии разрешения
                Toast.makeText(this@FirstScreenActivity,
                    "Bluetooth permission denied!",
                    Toast.LENGTH_SHORT).show()
            }
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == bluetoothPermissionRequestCode) {
            val isPermissionGranted =
                grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            bluetoothPermissionCallback?.invoke(isPermissionGranted)
            bluetoothPermissionCallback = null
        }
    }

    private fun enableBluetooth() {
        if (!bluetoothAdapter.isEnabled) {
            // Use the contract to launch the system enable Bluetooth dialog
            requestEnableContract.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }
    }


}