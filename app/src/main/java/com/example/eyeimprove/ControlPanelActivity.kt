package com.example.eyeimprove

import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.UUID

class ControlPanelActivity : AppCompatActivity() {

    //Communication parameters
    private val UUID_STRING_WELL_KNOWN : String = "00001101-0000-1000-2000-00805F9B34FB"
    private val WELL_KNOWN_UUID: UUID = UUID.fromString(UUID_STRING_WELL_KNOWN)
    private lateinit var bluetoothSocket: BluetoothSocket

    override fun onCreate(savedInstanceState: Bundle??) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.control_screen)

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
}