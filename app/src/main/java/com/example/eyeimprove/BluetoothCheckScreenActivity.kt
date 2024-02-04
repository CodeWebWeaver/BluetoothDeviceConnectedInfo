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
    private var bluetoothPermissionCallback: ((Boolean) -> Unit)? = null

    val requestEnableContract = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            Toast.makeText(this, "Bluetooth enabled", Toast.LENGTH_SHORT)
                .show()
            navigateToPickerScreen()
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
        if (bluetoothAdapter.isEnabled) {
            // Bluetooth уже включен, перейти к PickerScreen
            navigateToPickerScreen()
            return
        }

        checkBluetoothPermission { isPermissionGranted ->
            if (isPermissionGranted) {
                // Действия, которые выполняются при наличии разрешения
                enableBluetooth()
            } else {
                // Действия, которые выполняются при отсутствии разрешения
                Toast.makeText(this@BluetoothCheckScreenActivity,
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

    private fun navigateToPickerScreen(){
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