package com.example.eyeimprove

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.UUID

class MainActivity : AppCompatActivity() {

    //Communication parameters
    private val UUID_STRING_WELL_KNOWN : String = "00001101-0000-1000-2000-00805F9B34FB"
    private val WELL_KNOWN_UUID: UUID = UUID.fromString(UUID_STRING_WELL_KNOWN)



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = Intent(this, FirstScreenActivity::class.java)
        if (intent.resolveActivity(packageManager) != null) {
            // Активити существует, можно использовать интент
            startActivity(intent)
        } else {
            // Активити не найдена
            Toast.makeText(this, "Activity not found", Toast.LENGTH_SHORT).show()
        }
    }


}

