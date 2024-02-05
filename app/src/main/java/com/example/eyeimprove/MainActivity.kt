package com.example.eyeimprove

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startIntentOrShowError(this, Intent(this, BluetoothCheckScreenActivity::class.java))

        startIntentOrShowError(this, Intent(this, BluetoothCheckScreenActivity::class.java))
    }

    fun startIntentOrShowError(context: Context, intent: Intent) {
        if (intent.resolveActivity(context.packageManager) != null) {
            // Активити существует, можно использовать интент
            context.startActivity(intent)
        } else {
            // Активити не найдена
            Toast.makeText(context, "Activity not found", Toast.LENGTH_SHORT).show()
        }
    }
}

