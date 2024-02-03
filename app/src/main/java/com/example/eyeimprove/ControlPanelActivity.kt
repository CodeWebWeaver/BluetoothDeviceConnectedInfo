package com.example.eyeimprove

import android.bluetooth.BluetoothSocket
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class ControlPanelActivity : AppCompatActivity() {

    private lateinit var bluetoothSocket: BluetoothSocket
    override fun onCreate(savedInstanceState: Bundle??) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.bluetooth_check_screen)
    }
}