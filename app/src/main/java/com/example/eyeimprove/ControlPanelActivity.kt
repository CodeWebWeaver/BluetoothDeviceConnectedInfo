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
import org.json.JSONObject


class ControlPanelActivity : AppCompatActivity() {

    private var connectedDeviceAddress: String? = null
    private var connectedDevice: BluetoothDevice? = null

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothManager: BluetoothManager

    private var connectedDevices: MutableList<BluetoothDevice> = mutableListOf()

    //Communication parameters
    private val UUID_STRING_WELL_KNOWN: String = "00001101-0000-1000-2000-00805F9B34FB"
    private val WELL_KNOWN_UUID: UUID = UUID.fromString(UUID_STRING_WELL_KNOWN)
    private lateinit var bluetoothSocket: BluetoothSocket

    /** Inputs from connected Device  */
    //Views Inputs
    private lateinit var tempOutputFiller: TextView
    private lateinit var humidifyOutputFiller: TextView
    private lateinit var frequencyOutputFiller: TextView
    private lateinit var lightIntensityOutputFiller: TextView
    private lateinit var colorInputCard: CardView

    // Params inputs
    private var temperatureInput: Int? = null // null or something
    private var humidifyInput: Int? = null // null or something
    private var frequencyInput: Int? = null // null or something
    private var lightIntensityInput: Int? = null // null or something
    private var colorInput: String? = null // null or something

    /** Outputs to Device  */
    private val desiredParamsMap = HashMap<String, Any?>()
    private var jsonDesiredParams : JSONObject? = null

    // Views outputs
    private lateinit var tempInputField: EditText
    private lateinit var humidifyInputField: EditText
    private lateinit var frequencyInputField: EditText
    private lateinit var lightIntensityInputField: EditText
    private lateinit var colorSpinner: Spinner

    // Params outputs
    private var temperatureOutput: Int? = null // 15-25
    private var humidifyOutput: Int? = null //0 - 1
    private var frequencyOutput: Int? = null //10 - 900
    private var lightIntensityOutput: Int? = null //0 - 1
    private var colorOutput: String? = null //#252525

    // Color Spinner
    private lateinit var binding: ControlScreenBinding
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
            when (intent?.action) {
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    val device =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE) as? BluetoothDevice?
                    if (device?.address == connectedDeviceAddress) {
                        showToast("Disconnected with current device!")
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
        Log.i(
            "INFO", "Було отримано пристрій ${connectedDevice?.name}" +
                    " \n з адресом ${connectedDevice?.address}"
        )

        val returnButton = findViewById<Button>(R.id.control_screen_return_button)
        returnButton.setOnClickListener {
            navigateToActivity(PickerScreenActivity::class.java)
        }

        val submitButton = findViewById<Button>(R.id.control_screen_submit_button)
        submitButton.setOnClickListener {
            clearEditFocus()
            gatherInputs()
            sendInputs()
        }
    }

    private fun sendInputs() {
        jsonDesiredParams = JSONObject(desiredParamsMap)

        //Checking
        val jsonString = jsonDesiredParams.toString()
    }

    private fun gatherInputs() {
        temperatureOutput = validateAndGatherInput(tempInputField, 15, 25) // 15-25
        humidifyOutput = validateAndGatherInput(humidifyInputField, 0, 100) //0 - 1
        frequencyOutput = validateAndGatherInput(frequencyInputField, 10, 900) // 10 - 900
        lightIntensityOutput = validateAndGatherInput(lightIntensityInputField, 0, 100) //0 - 1

        desiredParamsMap["temperature"] = temperatureOutput
        desiredParamsMap["humidify"] = humidifyOutput
        desiredParamsMap["frequency"] = frequencyOutput
        desiredParamsMap["light_intensity"] = lightIntensityOutput
        desiredParamsMap["color"] = selectedColor.hex
    }

    private fun clearEditFocus() {
        tempInputField.clearFocus()
        humidifyInputField.clearFocus()
        frequencyInputField.clearFocus()
        lightIntensityInputField.clearFocus()
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
            editText.backgroundTintList = ColorStateList.valueOf(Color.RED)
            return null
        }

        if (inputValue !in minValue..maxValue) {
            // Если ввод пользователя не входит в диапазон, подчеркиваем поле другим цветом
            editText.backgroundTintList = ColorStateList.valueOf(Color.RED)
            return null
        }

        editText.backgroundTintList = ColorStateList.valueOf(Color.BLACK)
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
        bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH)
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

    private fun loadColorSpinner() {
        selectedColor = ColorList().defaultColor
        binding.colorSpinner.apply {
            adapter = ColorSpinnerAdapter(applicationContext, ColorList().basicColors())
            setSelection(ColorList().colorPosition(selectedColor), false)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    p0: AdapterView<*>?,
                    p1: View?,
                    position: Int,
                    p3: Long
                ) {
                    selectedColor = ColorList().basicColors()[position]
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {}
            }
        }
    }

    private fun initializeParams() {
        // Инициализация полей ввода
        tempInputField = findViewById(R.id.temp_input_field)
        humidifyInputField = findViewById(R.id.humidify_input_field)
        frequencyInputField = findViewById(R.id.frequency_input_field)
        lightIntensityInputField = findViewById(R.id.light_intensity_input_field)
        colorSpinner = findViewById(R.id.colorSpinner)

        // Инициализация полей вывода
        tempOutputFiller = findViewById(R.id.temp_output_filler)
        humidifyOutputFiller = findViewById(R.id.humidify_output_filler)
        frequencyOutputFiller = findViewById(R.id.frequency_output_filler)
        lightIntensityOutputFiller = findViewById(R.id.light_intensity_output_filler)
        colorInputCard = findViewById(R.id.color_input_card)

        tempOutputFiller.text = getString(R.string.current_temp)
        humidifyOutputFiller.text = getString(R.string.reset_humidify)
        frequencyOutputFiller.text = getString(R.string.reset_frequency)
        lightIntensityOutputFiller.text = getString(R.string.reset_light_intencity)
        colorInputCard.setCardBackgroundColor(resources.getColor(R.color.reset_input_color))
    }

    private fun resetParams() {
        // Сброс параметров
        temperatureInput = null
        humidifyInput = null
        frequencyInput = null
        lightIntensityInput = null
        colorInput = null

        // Сброс выходных параметров
        temperatureOutput = null
        humidifyOutput = null
        frequencyOutput = null
        lightIntensityOutput = null
        colorOutput = null

        tempInputField.text = null
        humidifyInputField.text = null
        frequencyInputField.text = null
        lightIntensityInputField.text = null

        desiredParamsMap.clear()

        connectedDeviceAddress = null
        connectedDevice = null
        jsonDesiredParams = null;
    }
}