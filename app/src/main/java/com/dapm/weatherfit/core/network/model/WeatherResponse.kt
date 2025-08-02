package com.dapm.weatherfit.core.network.model

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import com.dapm.weatherfit.R

// WeatherResponse.kt
data class WeatherResponse(
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    val hourly: HourlyData,
    val current: CurrentWeather?
)

data class HourlyData(
    val time: List<String>,
    val temperature_2m: List<Double>,
    val weather_code: List<Int>,
    val relative_humidity_2m: List<Int>,
    val wind_speed_10m: List<Double>
)

data class CurrentWeather(
    val time: String,
    val temperature_2m: Double,
    val weather_code: Int,
    val relative_humidity_2m: Int,
    val wind_speed_10m: Double
) {
    private fun esNoche(): Boolean {
        return try {
            // Si el tiempo viene como fecha actual del sistema, usar la hora actual
            if (time.contains("T")) {
                val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm", java.util.Locale.getDefault())
                val date = inputFormat.parse(time) ?: return false
                val calendar = java.util.Calendar.getInstance().apply { time = date }
                val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
                hour < 6 || hour >= 18
            } else {
                // Fallback: usar hora actual del sistema
                val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                currentHour < 6 || currentHour >= 18
            }
        } catch (e: Exception) {
            // En caso de error, usar hora actual del sistema
            val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            currentHour < 6 || currentHour >= 18
        }
    }

    private fun isDarkMode(context: Context): Boolean {
        val currentNightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES
    }

    fun getBackgroundColor(context: Context): Int {
        val isNight = esNoche()
        val isDarkMode = isDarkMode(context)

        // Aplicar color de noche solo para cielo despejado o parcialmente nublado
        if (isNight && (weather_code == 0 || weather_code in 1..3)) {
            return Color.parseColor("#1A237E") // Azul noche
        }

        // Colores según condición climática y tema del sistema
        return when (weather_code) {
            0 -> if (isDarkMode) Color.parseColor("#FFA000") else Color.parseColor("#FFF8E1") // cielo claro
            in 1..3 -> if (isDarkMode) Color.parseColor("#FFB300") else Color.parseColor("#FFF3CD") // parcialmente nublado
            45, 48 -> if (isDarkMode) Color.parseColor("#546E7A") else Color.parseColor("#ECEFF1") // niebla
            51, 53, 55 -> if (isDarkMode) Color.parseColor("#1976D2") else Color.parseColor("#E3F2FD") // llovizna
            56, 57 -> if (isDarkMode) Color.parseColor("#03A9F4") else Color.parseColor("#E1F5FE")
            61, 63, 65 -> if (isDarkMode) Color.parseColor("#0288D1") else Color.parseColor("#BBDEFB")
            66, 67 -> if (isDarkMode) Color.parseColor("#0097A7") else Color.parseColor("#B2EBF2")
            71, 73, 75 -> if (isDarkMode) Color.parseColor("#455A64") else Color.parseColor("#E0F7FA") // nieve
            77 -> if (isDarkMode) Color.parseColor("#546E7A") else Color.parseColor("#CFD8DC")
            80, 81, 82 -> if (isDarkMode) Color.parseColor("#1976D2") else Color.parseColor("#B3E5FC") // lluvia intensa
            85, 86 -> if (isDarkMode) Color.parseColor("#1E88E5") else Color.parseColor("#D6EAF8")
            95 -> if (isDarkMode) Color.parseColor("#212121") else Color.parseColor("#E0E0E0") // tormenta
            96, 99 -> if (isDarkMode) Color.parseColor("#263238") else Color.parseColor("#ECEFF1")
            else -> if (isDarkMode) Color.parseColor("#37474F") else Color.parseColor("#F5F5F5")
        }
    }
}

// HourlyWeatherItem.kt
data class HourlyWeatherItem(
    val time: String,
    val temperature: Double,
    val weatherCode: Int,
    val humidity: Int,
    val windSpeed: Double
) {
    fun getFormattedTime(): String {
        return try {
            val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm", java.util.Locale.getDefault())
            val outputFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            val date = inputFormat.parse(time)
            outputFormat.format(date ?: return time)
        } catch (e: Exception) {
            time
        }
    }

    fun getWeatherDescription(): String {
        return when (weatherCode) {
            0 -> "Despejado"
            1, 2, 3 -> "Parcialmente nublado"
            45, 48 -> "Niebla"
            51, 53, 55 -> "Llovizna"
            56, 57 -> "Llovizna helada"
            61, 63, 65 -> "Lluvia"
            66, 67 -> "Lluvia helada"
            71, 73, 75 -> "Nieve"
            77 -> "Granizo pequeño"
            80, 81, 82 -> "Chubascos"
            85, 86 -> "Chubascos de nieve"
            95 -> "Tormenta"
            96, 99 -> "Tormenta con granizo"
            else -> "Desconocido"
        }
    }

    private fun isNightTime(): Boolean {
        return try {
            val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm", java.util.Locale.getDefault())
            val date = inputFormat.parse(time) ?: return false
            val hour = java.util.Calendar.getInstance().apply { time = date }.get(java.util.Calendar.HOUR_OF_DAY)
            hour < 6 || hour >= 18
        } catch (e: Exception) {
            false
        }
    }

    fun getWeatherIcon(): Int {
        val isNight = isNightTime()

        return when (weatherCode) {
            0 -> if (isNight) R.drawable.clima_despejado_noche else R.drawable.clima_soleado
            1, 2, 3 -> if (isNight) R.drawable.clima_parcialmente_nublado_noche else R.drawable.clima_parcialmente_nublado
            45, 48 -> R.drawable.clima_niebla
            51, 53, 55 -> R.drawable.clima_llovizna
            56, 57 -> R.drawable.clima_lluvia_helada
            61, 63, 65 -> R.drawable.clima_lluvia
            66, 67 -> R.drawable.clima_lluvia_helada
            71, 73, 75 -> R.drawable.clima_nieve
            77 -> R.drawable.clima_granizo
            80, 81, 82 -> R.drawable.clima_chubascos
            85, 86 -> R.drawable.clima_chubascos_nieve
            95 -> R.drawable.clima_tormenta
            96, 99 -> R.drawable.clima_tormenta_granizo
            else -> R.drawable.clima_desconocido
        }
    }

}