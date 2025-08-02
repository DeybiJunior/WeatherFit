package com.dapm.weatherfit.features.inicio

// HourlyWeatherAdapter.kt
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dapm.weatherfit.R
import com.dapm.weatherfit.core.network.model.HourlyWeatherItem

class HourlyWeatherAdapter(
    private var hourlyWeatherList: List<HourlyWeatherItem> = emptyList(),
    private var unidadTemperatura: String = "Celsius" // Nuevo parámetro
) : RecyclerView.Adapter<HourlyWeatherAdapter.HourlyWeatherViewHolder>() {

    class HourlyWeatherViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val timeTextView: TextView = itemView.findViewById(R.id.textHora)
        val temperatureTextView: TextView = itemView.findViewById(R.id.textTemperatura)
        val weatherIconImageView: ImageView = itemView.findViewById(R.id.imageClima)
        val descriptionTextView: TextView = itemView.findViewById(R.id.textDescripcion)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HourlyWeatherViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_hourly_weather, parent, false)
        return HourlyWeatherViewHolder(view)
    }

    override fun onBindViewHolder(holder: HourlyWeatherViewHolder, position: Int) {
        val item = hourlyWeatherList[position]

        holder.timeTextView.text = item.getFormattedTime()

        // Aplicar conversión de temperatura según la unidad configurada
        val displayTemp = convertTemperature(item.temperature, unidadTemperatura)
        val symbol = getTemperatureSymbol(unidadTemperatura)
        holder.temperatureTextView.text = "${displayTemp.toInt()}°$symbol"

        holder.weatherIconImageView.setImageResource(item.getWeatherIcon())
        holder.descriptionTextView.text = item.getWeatherDescription()
    }

    override fun getItemCount(): Int = hourlyWeatherList.size

    // Método actualizado para incluir la unidad de temperatura
    fun updateData(newList: List<HourlyWeatherItem>, unidad: String = "Celsius") {
        hourlyWeatherList = newList
        unidadTemperatura = unidad
        notifyDataSetChanged()
    }

    // Método para mantener compatibilidad con el código existente
    fun updateData(newList: List<HourlyWeatherItem>) {
        updateData(newList, unidadTemperatura)
    }

    // Función para convertir temperatura
    private fun convertTemperature(celsius: Double, unidad: String): Double {
        return if (unidad == "Fahrenheit") {
            (celsius * 9.0 / 5.0) + 32.0
        } else {
            celsius
        }
    }

    // Función para obtener el símbolo de temperatura
    private fun getTemperatureSymbol(unidad: String): String {
        return if (unidad == "Fahrenheit") "F" else "C"
    }
}