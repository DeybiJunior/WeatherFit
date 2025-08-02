package com.dapm.weatherfit.presentation

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.dapm.weatherfit.R
import com.dapm.weatherfit.core.database.AppDatabase
import com.dapm.weatherfit.core.navigation.ViewPagerAdapter
import com.dapm.weatherfit.core.network.model.WeatherResponse
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var weatherBackgroundOverlay: View
    private val backgroundHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupViewPager()
        setupDarkMode()

        // Cargar fondo inicial
        loadWeatherBackground()

        // Programar próxima verificación
        scheduleNextBackgroundUpdate()
    }

    private fun initViews() {
        viewPager = findViewById(R.id.view_pager)
        bottomNavigationView = findViewById(R.id.bottom_navigation)
        weatherBackgroundOverlay = findViewById(R.id.weather_background_overlay)
    }

    private fun setupViewPager() {
        viewPager.adapter = ViewPagerAdapter(this)

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_inicio -> viewPager.currentItem = 0
                R.id.nav_recomendaciones -> viewPager.currentItem = 1
                R.id.nav_extras -> viewPager.currentItem = 2
            }
            true
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                bottomNavigationView.menu.getItem(position).isChecked = true
            }
        })
    }

    private val backgroundUpdateRunnable = object : Runnable {
        override fun run() {
            loadWeatherBackground()
            scheduleNextBackgroundUpdate()
        }
    }

    private fun scheduleNextBackgroundUpdate() {
        backgroundHandler.removeCallbacks(backgroundUpdateRunnable)
        val delayMs = getNextUpdateDelay()
        backgroundHandler.postDelayed(backgroundUpdateRunnable, delayMs)
    }

    private fun getNextUpdateDelay(): Long {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)

        // Horarios importantes: 6:00 AM (amanecer) y 18:00 PM (atardecer)
        val importantHours = listOf(6, 18)

        // Si estamos cerca de un cambio día/noche (dentro de los próximos 30 minutos)
        val nextImportantHour = importantHours.firstOrNull { hour ->
            hour > currentHour || (hour == currentHour && currentMinute < 30)
        } ?: (importantHours.first() + 24) // Si ya pasaron todos hoy, el primero de mañana

        val nextUpdateCalendar = Calendar.getInstance().apply {
            if (nextImportantHour > 23) {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, nextImportantHour - 24)
            } else {
                set(Calendar.HOUR_OF_DAY, nextImportantHour)
            }
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val delay = nextUpdateCalendar.timeInMillis - System.currentTimeMillis()

        // Si el delay es muy pequeño (menos de 1 minuto), programar para la próxima hora importante
        return if (delay < 60000) {
            val nextNextHour = if (nextImportantHour == 6) 18 else 6
            val nextNextCalendar = Calendar.getInstance().apply {
                if (nextNextHour < currentHour) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
                set(Calendar.HOUR_OF_DAY, nextNextHour)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            nextNextCalendar.timeInMillis - System.currentTimeMillis()
        } else {
            delay
        }
    }

    private fun loadWeatherBackground() {
        if (!::weatherBackgroundOverlay.isInitialized) return

        lifecycleScope.launch {
            try {
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val climaDao = AppDatabase.getDatabase(this@MainActivity).climaDao()
                val climaEntity = climaDao.obtenerClimaPorFecha(today)

                climaEntity?.let {
                    val weatherResponse = Gson().fromJson(it.weatherJson, WeatherResponse::class.java)
                    val current = weatherResponse.current ?: return@let

                    // Actualizar el tiempo actual para la detección correcta día/noche
                    val currentTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault()).format(Date())
                    val updatedCurrent = current.copy(time = currentTime)

                    val color = updatedCurrent.getBackgroundColor(this@MainActivity)
                    animateBackgroundColor(color)
                } ?: run {
                    // Color por defecto basado en la hora actual
                    val defaultColor = getDefaultColorForCurrentTime()
                    animateBackgroundColor(defaultColor)
                }
            } catch (e: Exception) {
                val defaultColor = getDefaultColorForCurrentTime()
                animateBackgroundColor(defaultColor)
            }
        }
    }

    private fun getDefaultColorForCurrentTime(): Int {
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val isNight = currentHour < 6 || currentHour >= 18
        val isDarkMode = (resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES

        return when {
            isNight -> Color.parseColor("#1A237E") // Azul noche
            isDarkMode -> Color.parseColor("#FFA000") // Naranja para modo oscuro
            else -> Color.parseColor("#FFD54F") // Amarillo para modo claro
        }
    }

    private fun setupDarkMode() {
        lifecycleScope.launch {
            try {
                val config = AppDatabase.getDatabase(this@MainActivity).configuracionDao().obtener()
                config?.let {
                    val modo = if (it.modoOscuro) {
                        AppCompatDelegate.MODE_NIGHT_YES
                    } else {
                        AppCompatDelegate.MODE_NIGHT_NO
                    }
                    AppCompatDelegate.setDefaultNightMode(modo)
                }
            } catch (e: Exception) {
                // Manejo de errores para configuración
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Actualizar fondo cuando la app vuelve al primer plano
        loadWeatherBackground()
    }

    override fun onDestroy() {
        super.onDestroy()
        backgroundHandler.removeCallbacks(backgroundUpdateRunnable)
    }

    private fun animateBackgroundColor(toColor: Int) {
        if (!::weatherBackgroundOverlay.isInitialized) return

        val currentColor = (weatherBackgroundOverlay.background as? ColorDrawable)?.color
            ?: Color.TRANSPARENT

        if (currentColor == toColor) return // No animar si el color es el mismo

        val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), currentColor, toColor)
        colorAnimation.duration = 1500
        colorAnimation.addUpdateListener { animator ->
            weatherBackgroundOverlay.setBackgroundColor(animator.animatedValue as Int)
        }
        colorAnimation.start()
    }

    fun updateWeatherBackground(weatherCode: Int, time: String) {
        val tempCurrentWeather = com.dapm.weatherfit.core.network.model.CurrentWeather(
            time = time,
            temperature_2m = 0.0,
            weather_code = weatherCode,
            relative_humidity_2m = 0,
            wind_speed_10m = 0.0
        )

        val color = tempCurrentWeather.getBackgroundColor(this)
        animateBackgroundColor(color)
    }
}