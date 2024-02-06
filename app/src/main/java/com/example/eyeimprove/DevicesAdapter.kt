import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.eyeimprove.R

class DevicesAdapter(private val dataSet: HashMap<String, String>) :
    RecyclerView.Adapter<DevicesAdapter.ViewHolder>() {

    private var onDeviceClickListener: OnDeviceClickListener? = null
    private var lastClickedIndex: Int? = null

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val deviceName: TextView = view.findViewById(R.id.device_name)
        val deviceAddress: TextView = view.findViewById(R.id.device_address)
    }

    fun updateData(newData: HashMap<String, String>) {
        dataSet.putAll(newData)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.device_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = dataSet.entries.toList()[position]

        holder.deviceName.text = item.value
        holder.deviceAddress.text = item.key

        holder.itemView.setOnClickListener {
            val clickedIndex = holder.adapterPosition
            if (clickedIndex == lastClickedIndex) {
                // Карточка уже была нажата, сбрасываем цвет
                lastClickedIndex = null
            } else {
                // Карточка только что была нажата
                onDeviceClickListener?.onDeviceClick(item.key)
                lastClickedIndex = clickedIndex
            }
            notifyDataSetChanged()
        }

        // Проверяем, нажата ли эта карточка, и устанавливаем окрас
        if (lastClickedIndex == holder.adapterPosition) {
            holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.green_card_pressed))
        } else {
            holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.ard_normal_green))
        }
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

    interface OnDeviceClickListener {
        fun onDeviceClick(deviceAddress: String)
    }

    fun setOnDeviceClickListener(listener: OnDeviceClickListener) {
        onDeviceClickListener = listener
    }
}