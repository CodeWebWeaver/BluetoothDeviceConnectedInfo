package com.example.eyeimprove

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.eyeimprove.databinding.ControlScreenBinding
import java.lang.reflect.Method
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject


class ControlPanelActivity : AppCompatActivity() {

    private var connectedDeviceAddress:  String? = null
    private var connectedDevice : BluetoothDevice? = null

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothManager: BluetoothManager

    private var connectedDevices: MutableList<BluetoothDevice> = mutableListOf()

        //Communication parameters
    private val UUID_STRING_WELL_KNOWN : String = "00001101-0000-1000-2000-00805F9B34FB"
    private val WELL_KNOWN_UUID: UUID = UUID.fromString(UUID_STRING_WELL_KNOWN)
    private lateinit var bluetoothSocket: BluetoothSocket

    /** Inputs from connected Device  */
    //Views Inputs
    private lateinit var temp_output_filler : TextView
    private lateinit var humidify_output_filler : TextView
    private lateinit var frequency_output_filler : TextView
    private lateinit var light_intensity_output_filler : TextView
    private lateinit var color_input_card : CardView

    // Params inputs
    private var temperature_input : Int? = null // null or something
    private var humidify_input : Int? = null // null or something
    private var frequency_input : Int? = null // null or something
    private var light_intensity_input : Int? = null // null or something
    private var color_input : String? = null // null or something

    /** Outputs to Device  */
    val parametersMap = HashMap<String, Any?>()
    // Views outputs
    private lateinit var temp_input_field : EditText
    private lateinit var humidify_input_field : EditText
    private lateinit var frequency_input_field : EditText
    private lateinit var light_intensity_input_field : EditText
    private lateinit var colorSpinner : Spinner

    // Params outputs
    private var temperature_output : Int? = null // 15-25
    private var humidify_output : Int? = null //0 - 1
    private var frequency_output : Int? = null //10 - 900
    private var light_intensity_output : Int? = null //0 - 1
    private var color_output : String? = null //#252525

    // Color Spinner
    lateinit var binding: ControlScreenBinding
    lateinit var selectedColor: ColorObject


    private val bluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Callback-функция
        if (!isGranted) {
            showToast("Bluetooth permission denied!")
            finish()
        }
    }

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action

            when (action) {
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    val device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE) as? BluetoothDevice?
                    if (device?.address == connectedDeviceAddress) {
                        connectedDeviceAddress = null
                        connectedDevice = null
                        // Отключенное устройство - наше целевое устройство, возвращаемся на предыдущий экран
                        navigateToActivity(PickerScreenActivity::class.java)
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        registerReceiver(bluetoothReceiver, filter)

        initializeParams()
        resetParams()
    }
    override fun onStop() {
        super.onStop()
        unregisterReceiver(bluetoothReceiver)

        resetParams()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ControlScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        loadColorSpinner()

        checkBluetoothPermission()
        bluetoothManager = getSystemService(BluetoothManager::class.java) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        connectedDevice = getConnectedDevice()
        Log.i("INFO", "Було отримано пристрій ${connectedDevice?.name} \n з адресом ${connectedDevice?.address}")

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
            gatherInputs()
            sendInputs()
        }
    }

    private fun sendInputs() {
        val json = JSONObject(parametersMap)

        //Checking
        val jsonString = json.toString()
    }

    private fun gatherInputs() {
        temperature_output = validateAndGatherInput(temp_input_field, 15, 25) // 15-25
        humidify_output = validateAndGatherInput(humidify_input_field, 0, 100) //0 - 1
        frequency_output = validateAndGatherInput(frequency_input_field, 10, 900) // 10 - 900
        light_intensity_output = validateAndGatherInput(light_intensity_input_field, 0, 100) //0 - 1

        parametersMap["temperature"] = temperature_output
        parametersMap["humidify"] = humidify_output
        parametersMap["frequency"] = frequency_output
        parametersMap["light_intensity"] = light_intensity_output
        parametersMap["color"] = selectedColor.hex
    }

    private fun validateAndGatherInput(editText: EditText?, minValue: Int, maxValue: Int): Int? {
        val input = editText?.text?.toString()
        if (input.isNullOrEmpty()) {
            editText?.backgroundTintList = ColorStateList.valueOf(Color.YELLOW)
            return null
        }

        val inputValue = input.toIntOrNull()
        if (inputValue == null) {
            // Если ввод пользователя не удалось преобразовать в Int, подчеркиваем поле другим цветом
            editText?.backgroundTintList = ColorStateList.valueOf(Color.RED)
            return null
        }

        if (inputValue !in minValue..maxValue) {
            // Если ввод пользователя не входит в диапазон, подчеркиваем поле другим цветом
            editText?.backgroundTintList = ColorStateList.valueOf(Color.RED)
            return null
        }

        editText?.backgroundTintList = ColorStateList.valueOf(Color.BLACK)
        return inputValue
    }


    private fun getConnectedDevice(): BluetoothDevice? {
        connectedDeviceAddress = intent.getStringExtra("selectedDeviceAddress")
        checkBluetoothPermission()
        return bluetoothAdapter.bondedDevices
            .filter { isConnected(it) }
            .firstOrNull { it.address == connectedDeviceAddress }
    }

    private fun checkBluetoothPermission() {
        bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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

    private fun isConnected(device: BluetoothDevice): Boolean {
        return try {
            val m: Method = device.javaClass.getMethod("isConnected")
            m.invoke(device) as Boolean
        } catch (e: Exception) {
            throw IllegalStateException(e)
        }
    }

    private fun loadColorSpinner()
    {
        selectedColor = ColorList().defaultColor
        binding.colorSpinner.apply {
            adapter = ColorSpinnerAdapter(applicationContext, ColorList().basicColors())
            setSelection(ColorList().colorPosition(selectedColor), false)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener
            {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long)
                {
                    selectedColor = ColorList().basicColors()[position]
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {}
            }
        }
    }

    private fun initializeParams() {
        // Инициализация полей ввода
        temp_input_field = findViewById(R.id.temp_input_field)
        humidify_input_field = findViewById(R.id.humidify_input_field)
        frequency_input_field = findViewById(R.id.frequency_input_field)
        light_intensity_input_field = findViewById(R.id.light_intensity_input_field)
        colorSpinner = findViewById(R.id.colorSpinner)

        // Инициализация полей вывода
        temp_output_filler = findViewById(R.id.temp_output_filler)
        humidify_output_filler = findViewById(R.id.humidify_output_filler)
        frequency_output_filler = findViewById(R.id.frequency_output_filler)
        light_intensity_output_filler = findViewById(R.id.light_intensity_output_filler)
        color_input_card = findViewById(R.id.color_input_card)

        temp_output_filler.text = getString(R.string.current_temp)
        humidify_output_filler.text  = getString(R.string.reset_humidify)
        frequency_output_filler.text  = getString(R.string.reset_frequency)
        light_intensity_output_filler.text  = getString(R.string.reset_light_intencity)
        color_input_card.setCardBackgroundColor(getResources().getColor(R.color.reset_input_color))

        // Сброс параметров
        temperature_input = null
        humidify_input = null
        frequency_input = null
        light_intensity_input = null
        color_input = null

        // Сброс выходных параметров
        temperature_output = null
        humidify_output = null
        frequency_output = null
        light_intensity_output = null
        color_output = null
    }

    private fun resetParams() {
        // Сброс параметров
        temperature_input = null
        humidify_input = null
        frequency_input = null
        light_intensity_input = null
        color_input = null

        // Сброс выходных параметров
        temperature_output = null
        humidify_output = null
        frequency_output = null
        light_intensity_output = null
        color_output = null

        temp_output_filler.text = getString(R.string.current_temp)
        humidify_output_filler.text  = getString(R.string.reset_humidify)
        frequency_output_filler.text  = getString(R.string.reset_frequency)
        light_intensity_output_filler.text  = getString(R.string.reset_light_intencity)
        color_input_card.setCardBackgroundColor(getResources().getColor(R.color.reset_input_color))

        temp_input_field.text = null
        humidify_input_field.text = null
        frequency_input_field.text = null
        light_intensity_input_field.text = null

        parametersMap.clear()
    }
}