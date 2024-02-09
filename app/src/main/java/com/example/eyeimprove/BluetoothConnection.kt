package com.example.eyeimprove

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.Handler
import android.os.Looper
import android.util.Log
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class BluetoothCommunication(private val device: BluetoothDevice) {
    private var socket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private val handler = Handler(Looper.getMainLooper())

    private val tag = "BluetoothConnection"

    @SuppressLint("MissingPermission")
    fun connect() {
        Thread {
            try {
                socket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
                socket?.connect()
                inputStream = socket?.inputStream
                outputStream = socket?.outputStream
                handler.post {
                    // Сообщить об успешном подключении
                }
            } catch (e: IOException) {
                e.printStackTrace()
                handler.post {
                    Log.e(tag, "Socket's create() method failed", e)
                }
            }
        }.start()
    }

    fun disconnect() {
        try {
            socket?.close()
            inputStream?.close()
            outputStream?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun sendData(jsonObject: JSONObject) {
        Thread {
            try {
                outputStream?.write(jsonObject.toString().toByteArray())
                outputStream?.flush()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }.start()
    }

    fun receiveData(callback: (JSONObject) -> Unit) {
        Thread {
            try {
                val buffer = ByteArray(1024)
                var bytes: Int
                while (true) {
                    bytes = inputStream?.read(buffer) ?: break
                    val jsonString = String(buffer, 0, bytes)
                    val jsonObject = JSONObject(jsonString)
                    handler.post {
                        callback(jsonObject)
                    }
                }
            } catch (e: IOException) {
                Log.e(tag, "Error receiving data", e)
            }
        }.start()
    }
}
