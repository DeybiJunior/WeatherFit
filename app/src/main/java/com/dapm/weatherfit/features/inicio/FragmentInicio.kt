package com.dapm.weatherfit.features.inicio

import android.Manifest
import android.location.Geocoder
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.dapm.weatherfit.core.database.AppDatabase
import com.dapm.weatherfit.core.database.dao.ClimaDao
import com.dapm.weatherfit.core.database.dao.UbicacionDao
import com.dapm.weatherfit.core.database.entities.ClimaEntity
import com.dapm.weatherfit.core.database.entities.UbicacionEntity
import com.dapm.weatherfit.core.network.WeatherRepository
import com.dapm.weatherfit.core.network.model.HourlyWeatherItem
import com.dapm.weatherfit.core.network.LocationHelper
import com.dapm.weatherfit.core.network.model.WeatherResponse

import com.google.gson.Gson
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.text.SimpleDateFormat
import java.util.*
import com.dapm.weatherfit.core.database.dao.ConfiguracionDao

import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.provider.Settings
import androidx.appcompat.app.AppCompatDelegate
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.dapm.weatherfit.R

class FragmentInicio : Fragment(R.layout.fragment_inicio) {

    //BD
    private lateinit var climaDao: ClimaDao
    private lateinit var ubicacionDao: UbicacionDao
    private lateinit var configuracionDao: ConfiguracionDao
    private val formatoFecha = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // Configuraciones
    private var unidadTemperatura = "Celsius" // Por defecto
    private var configuracionCargada = false

    // Views
    private lateinit var textHora: TextView
    private lateinit var textUbicacion: TextView
    private lateinit var textDia: TextView
    private lateinit var textTemperature: TextView
    private lateinit var textClima: TextView
    private lateinit var imageClima: ImageView
    private lateinit var btnReintentarUbicacion: Button
    private lateinit var recyclerPronostico: RecyclerView

    // Components
    private lateinit var weatherRepository: WeatherRepository
    private lateinit var locationHelper: LocationHelper
    private lateinit var hourlyAdapter: HourlyWeatherAdapter
    private lateinit var geocoder: Geocoder

    //refrescar fragment
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    // Última respuesta de clima para interpolación
    private var latestWeatherResponse: WeatherResponse? = null

    // Permission launcher
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
                loadWeatherData(forceRefresh = true)
            }

            else -> {
                showLocationPermissionError()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateMainActivityBackground()

        // Recargar configuraciones por si cambiaron
        lifecycleScope.launch {
            try {
                // Verificar si el fragment está adjunto
                if (!isAdded) return@launch

                val config = configuracionDao.obtener()
                config?.let {
                    val nuevaUnidad = it.unidadTemperatura
                    if (nuevaUnidad != unidadTemperatura) {
                        unidadTemperatura = nuevaUnidad
                        // Actualizar UI con nueva unidad
                        latestWeatherResponse?.let { weatherResponse ->
                            if (isAdded) {
                                updateWeatherUI(weatherResponse, interpolated = true)
                                // También actualizar el adapter con la nueva unidad
                                updateWeatherUI(weatherResponse, interpolated = false)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Manejo silencioso del error si el fragment no está adjunto
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        initComponents()
        setupRecyclerView()
        setupClickListeners()
        updateCurrentTime()

        // Cargar configuraciones primero
        cargarConfiguraciones()
    }

    private fun initViews(view: View) {
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        textHora = view.findViewById(R.id.textHora)
        textUbicacion = view.findViewById(R.id.textUbicacion)
        textDia = view.findViewById(R.id.textDia)
        textTemperature = view.findViewById(R.id.textTemperature)
        textClima = view.findViewById(R.id.textClima)
        imageClima = view.findViewById(R.id.imageClima)
        btnReintentarUbicacion = view.findViewById(R.id.btnReintentarUbicacion)
        recyclerPronostico = view.findViewById(R.id.recyclerPronostico)
    }

    private fun initComponents() {
        val db = AppDatabase.getDatabase(requireContext())
        climaDao = db.climaDao()
        ubicacionDao = db.ubicacionDao()
        configuracionDao = db.configuracionDao()
        weatherRepository = WeatherRepository()
        locationHelper = LocationHelper(requireContext())
        geocoder = Geocoder(requireContext(), Locale.getDefault())
    }

    private fun setupRecyclerView() {
        hourlyAdapter = HourlyWeatherAdapter(emptyList(), unidadTemperatura)
        recyclerPronostico.apply {
            adapter = hourlyAdapter
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }
    }

    private fun setupClickListeners() {
        btnReintentarUbicacion.setOnClickListener {
            checkPermissionsAndLoadWeather(forceRefresh = true)
        }
        swipeRefreshLayout.setOnRefreshListener {
            refreshWeatherData()
        }
        setupSwipeRefreshColors()
    }

    private fun setupSwipeRefreshColors() {
        // Configurar colores de la animación de refresco
        swipeRefreshLayout.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        )
    }

    private fun refreshWeatherData() {
        // Ocultar botón de reintentar al comenzar actualización
        btnReintentarUbicacion.visibility = View.GONE

        lifecycleScope.launch {
            try {
                // Obtener ubicación actual del dispositivo para comparar
                val locationResult = withTimeout(10000L) {
                    locationHelper.getCurrentLocation()
                }

                locationResult.onSuccess { currentLocation ->
                    if (!isAdded) return@onSuccess

                    val currentCityName = getCityName(currentLocation.latitude, currentLocation.longitude)
                    val ubicacionGuardada = ubicacionDao.obtenerUbicacion()

                    if (ubicacionGuardada != null) {
                        // Comparar si la ciudad actual es diferente a la guardada
                        val distancia = calcularDistancia(
                            currentLocation.latitude, currentLocation.longitude,
                            ubicacionGuardada.latitud, ubicacionGuardada.longitud
                        )

                        // Si la distancia es mayor a 5km o el nombre de ciudad es diferente, actualizar ubicación
                        if (distancia > 5.0 || currentCityName != ubicacionGuardada.nombreCiudad) {
                            // Nueva ubicación detectada
                            val nuevaUbicacion = UbicacionEntity(
                                latitud = currentLocation.latitude,
                                longitud = currentLocation.longitude,
                                nombreCiudad = currentCityName,
                                fechaActualizacion = formatoFecha.format(Date())
                            )
                            ubicacionDao.guardarUbicacion(nuevaUbicacion)

                            // Obtener clima para la nueva ubicación
                            getWeatherForLocation(
                                currentLocation.latitude,
                                currentLocation.longitude,
                                currentCityName,
                                forceRefresh = true
                            )
                        } else {
                            // Misma ubicación, solo actualizar clima
                            getWeatherForLocation(
                                ubicacionGuardada.latitud,
                                ubicacionGuardada.longitud,
                                ubicacionGuardada.nombreCiudad,
                                forceRefresh = true
                            )
                        }
                    } else {
                        // No hay ubicación guardada, guardar la actual
                        val nuevaUbicacion = UbicacionEntity(
                            latitud = currentLocation.latitude,
                            longitud = currentLocation.longitude,
                            nombreCiudad = currentCityName,
                            fechaActualizacion = formatoFecha.format(Date())
                        )
                        ubicacionDao.guardarUbicacion(nuevaUbicacion)

                        getWeatherForLocation(
                            currentLocation.latitude,
                            currentLocation.longitude,
                            currentCityName,
                            forceRefresh = true
                        )
                    }

                }.onFailure { error ->
                    if (!isAdded) return@onFailure

                    // Si no se puede obtener ubicación actual, usar la guardada
                    val ubicacionGuardada = ubicacionDao.obtenerUbicacion()
                    if (ubicacionGuardada != null) {
                        getWeatherForLocation(
                            ubicacionGuardada.latitud,
                            ubicacionGuardada.longitud,
                            ubicacionGuardada.nombreCiudad,
                            forceRefresh = true
                        )
                    } else {
                        showError("No se puede obtener la ubicación: ${error.message}")
                    }
                }

            } catch (e: Exception) {
                if (isAdded) {
                    showError("Error al refrescar: ${e.message}")
                }
            } finally {
                // IMPORTANTE: Detener la animación de refresco
                if (isAdded) {
                    swipeRefreshLayout.isRefreshing = false
                }
            }
        }
    }

    // Función auxiliar para calcular distancia entre dos puntos
    private fun calcularDistancia(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val radioTierra = 6371.0 // Radio de la Tierra en kilómetros

        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        val deltaLatRad = Math.toRadians(lat2 - lat1)
        val deltaLonRad = Math.toRadians(lon2 - lon1)

        val a = Math.sin(deltaLatRad / 2) * Math.sin(deltaLatRad / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                Math.sin(deltaLonRad / 2) * Math.sin(deltaLonRad / 2)

        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        return radioTierra * c
    }

    private fun cargarConfiguraciones() {
        lifecycleScope.launch {
            try {
                // Verificar si el fragment sigue adjunto antes de continuar
                if (!isAdded) return@launch

                val config = configuracionDao.obtener()
                config?.let {
                    unidadTemperatura = it.unidadTemperatura

                    // Aplicar modo oscuro si está configurado
                    if (it.modoOscuro) {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    }
                }
                configuracionCargada = true

                // Verificar nuevamente si el fragment sigue adjunto antes de continuar
                if (!isAdded) return@launch

                // Ahora que las configuraciones están cargadas, cargar el clima
                checkPermissionsAndLoadWeather()

                // Iniciar el bucle de actualización de tiempo
                startTimeUpdateLoop()

            } catch (e: Exception) {
                // Si hay error, usar configuración por defecto
                configuracionCargada = true

                // Verificar si el fragment sigue adjunto antes de continuar
                if (isAdded) {
                    checkPermissionsAndLoadWeather()
                    startTimeUpdateLoop()
                }
            }
        }
    }

    private fun startTimeUpdateLoop() {
        // Actualizar temperatura interpolada cada minuto
        viewLifecycleOwner.lifecycleScope.launch {
            while (true) {
                // Verificar si el fragment sigue adjunto
                if (!isAdded) break

                updateCurrentTime()

                // Actualizar temperatura interpolada si hay datos
                latestWeatherResponse?.let { weatherResponse ->
                    updateWeatherUI(weatherResponse, interpolated = true)
                }

                val now = Calendar.getInstance()
                if (now.get(Calendar.MINUTE) == 0) {
                    updateMainActivityBackground()
                }

                val seconds = now.get(Calendar.SECOND)
                val millisToNextMinute = (60 - seconds) * 1000L
                delay(millisToNextMinute)
            }
        }
    }

    private fun updateMainActivityBackground() {
        // Verificar que el fragment esté adjunto y la activity sea MainActivity
        if (!isAdded) return
        val mainActivity = activity as? com.dapm.weatherfit.presentation.MainActivity ?: return

        lifecycleScope.launch {
            try {
                // Obtener datos del clima actual
                val today = formatoFecha.format(Date())
                val climaGuardado = climaDao.obtenerClimaPorFecha(today)

                if (climaGuardado != null) {
                    // Si tenemos datos del clima, usar el código actual
                    val weatherResponse = Gson().fromJson(climaGuardado.weatherJson, WeatherResponse::class.java)

                    // Obtener el código de clima más cercano a la hora actual
                    val currentWeatherCode = getCurrentWeatherCode(weatherResponse)

                    // Crear timestamp actual en el formato correcto
                    val currentTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault()).format(Date())

                    // Actualizar el fondo de MainActivity
                    mainActivity.updateWeatherBackground(currentWeatherCode, currentTime)

                } else {
                    // Si no hay datos del clima, usar código por defecto (despejado)
                    val currentTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault()).format(Date())
                    mainActivity.updateWeatherBackground(0, currentTime) // 0 = despejado
                }

            } catch (e: Exception) {
                // En caso de error, usar valores por defecto
                val currentTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault()).format(Date())
                mainActivity.updateWeatherBackground(0, currentTime)
            }
        }
    }

    private fun getCurrentWeatherCode(weatherResponse: WeatherResponse): Int {
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
        val now = Date()

        // Buscar el índice de la hora más cercana
        var closestIndex = 0
        var minDifference = Long.MAX_VALUE

        for (i in weatherResponse.hourly.time.indices) {
            try {
                val itemDate = formatter.parse(weatherResponse.hourly.time[i])
                if (itemDate != null) {
                    val difference = kotlin.math.abs(itemDate.time - now.time)
                    if (difference < minDifference) {
                        minDifference = difference
                        closestIndex = i
                    }
                }
            } catch (e: Exception) {
                continue
            }
        }

        return weatherResponse.hourly.weather_code.getOrElse(closestIndex) { 0 }
    }

    private fun updateCurrentTime() {
        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val currentDay = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date()).uppercase()

        textHora.text = currentTime
        textDia.text = currentDay
    }

    private fun checkPermissionsAndLoadWeather(forceRefresh: Boolean = false) {
        // Solo cargar clima si las configuraciones ya están cargadas Y el fragment está adjunto
        if (!configuracionCargada || !isAdded) return

        when {
            !locationHelper.hasLocationPermission() -> {
                requestLocationPermissions()
            }

            !isLocationEnabled() -> {
                showLocationDisabledDialog()
            }

            else -> {
                loadWeatherData(forceRefresh)
            }
        }
    }

    private fun isLocationEnabled(): Boolean {
        // Verificar si el fragment está adjunto antes de usar requireContext()
        if (!isAdded) return false

        val locationManager =
            requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun showLocationDisabledDialog() {
        // Verificar si el fragment está adjunto antes de mostrar diálogo
        if (!isAdded) return

        AlertDialog.Builder(requireContext())
            .setTitle("Ubicación deshabilitada")
            .setMessage("Para obtener el clima actual, necesitas habilitar la ubicación en tu dispositivo.")
            .setPositiveButton("Configurar") { _, _ ->
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton("Cancelar") { _, _ ->
                if (isAdded) {
                    btnReintentarUbicacion.visibility = View.VISIBLE
                }
            }
            .show()
    }

    private fun requestLocationPermissions() {
        if (isAdded) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun loadWeatherData(forceRefresh: Boolean = false) {
        if (!isAdded) return

        btnReintentarUbicacion.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val today = formatoFecha.format(Date())

                // Si no es forzado y tenemos datos del clima de hoy, usarlos
                if (!forceRefresh) {
                    val climaGuardado = climaDao.obtenerClimaPorFecha(today)
                    if (climaGuardado != null) {
                        val weatherResponse = Gson().fromJson(climaGuardado.weatherJson, WeatherResponse::class.java)
                        latestWeatherResponse = weatherResponse
                        if (isAdded) {
                            textUbicacion.text = climaGuardado.nombreCiudad
                            updateWeatherUI(weatherResponse)
                        }
                        return@launch
                    }
                }

                // Verificar si tenemos ubicación guardada
                val ubicacionGuardada = ubicacionDao.obtenerUbicacion()

                if (ubicacionGuardada != null && !forceRefresh) {
                    // Usar ubicación guardada
                    getWeatherForLocation(ubicacionGuardada.latitud, ubicacionGuardada.longitud, ubicacionGuardada.nombreCiudad)
                } else {
                    // Obtener nueva ubicación
                    obtenerNuevaUbicacion()
                }

            } catch (e: Exception) {
                if (isAdded) {
                    showError("Error inesperado: ${e.message}")
                }
            }
        }
    }

    private suspend fun obtenerNuevaUbicacion() {
        if (!isAdded) return

        // Mostrar indicador de carga
        textUbicacion.text = "Obteniendo ubicación..."
        textTemperature.text = "--°${getTemperatureSymbol()}"

        try {
            val locationResult = withTimeout(15000L) {
                locationHelper.getCurrentLocation()
            }

            locationResult.onSuccess { location ->
                if (!isAdded) return@onSuccess

                val cityName = getCityName(location.latitude, location.longitude)

                val nuevaUbicacion = UbicacionEntity(
                    latitud = location.latitude,
                    longitud = location.longitude,
                    nombreCiudad = cityName,
                    fechaActualizacion = formatoFecha.format(Date())
                )
                ubicacionDao.guardarUbicacion(nuevaUbicacion)

                getWeatherForLocation(location.latitude, location.longitude, cityName)

            }.onFailure { error ->
                if (!isAdded) return@onFailure

                val errorMessage = when {
                    error.message?.contains("timeout", ignoreCase = true) == true ->
                        "Tiempo agotado al obtener ubicación. Verifica que el GPS esté habilitado."
                    error.message?.contains("permission", ignoreCase = true) == true ->
                        "Permisos de ubicación denegados"
                    else -> "Error al obtener ubicación: ${error.message}"
                }
                showError(errorMessage)
                // Detener refresh en caso de error
                if (swipeRefreshLayout.isRefreshing) {
                    swipeRefreshLayout.isRefreshing = false
                }
            }

        } catch (e: TimeoutCancellationException) {
            if (isAdded) {
                showError("Tiempo agotado al obtener ubicación. Verifica que el GPS esté habilitado.")
                if (swipeRefreshLayout.isRefreshing) {
                    swipeRefreshLayout.isRefreshing = false
                }
            }
        }
    }

    private suspend fun getWeatherForLocation(
        latitude: Double,
        longitude: Double,
        cityName: String,
        forceRefresh: Boolean = false
    ) {
        if (!isAdded) return

        textUbicacion.text = cityName

        val weatherResult = weatherRepository.getWeatherData(latitude, longitude)
        weatherResult.onSuccess { weatherResponse ->
            if (!isAdded) return@onSuccess

            latestWeatherResponse = weatherResponse

            val today = formatoFecha.format(Date())
            val climaEntity = ClimaEntity(
                fecha = today,
                weatherJson = Gson().toJson(weatherResponse),
                latitud = latitude,
                longitud = longitude,
                nombreCiudad = cityName
            )
            climaDao.insertarClima(climaEntity)

            updateWeatherUI(weatherResponse)

            // Ocultar botón de reintentar ya que la operación fue exitosa
            btnReintentarUbicacion.visibility = View.GONE

            // Detener animación de refresh si está activa
            if (swipeRefreshLayout.isRefreshing) {
                swipeRefreshLayout.isRefreshing = false
            }

        }.onFailure { error ->
            if (isAdded) {
                showError("Error al obtener datos del clima: ${error.message}")
                // Detener animación de refresh en caso de error también
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun getCityName(latitude: Double, longitude: Double): String {
        return try {
            if (!isAdded) return "Ubicación no disponible"

            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]

                val ciudad = address.locality ?: ""
                val provincia = address.adminArea ?: ""
                val pais = address.countryName ?: ""

                // Unir solo los campos que existan
                listOf(ciudad, provincia, pais)
                    .filter { it.isNotBlank() }
                    .joinToString(", ")
            } else {
                "Ubicación no disponible"
            }
        } catch (e: Exception) {
            "Ubicación no disponible"
        }
    }


    private fun updateWeatherUI(weatherResponse: WeatherResponse, interpolated: Boolean = false) {
        if (!isAdded) return

        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
        val now = Date()

        if (interpolated) {
            val index = weatherResponse.hourly.time.indexOfFirst { timeString ->
                formatter.parse(timeString)?.after(now) == true
            }

            if (index > 0) {
                val t1 = formatter.parse(weatherResponse.hourly.time[index - 1])!!
                val t2 = formatter.parse(weatherResponse.hourly.time[index])!!

                val temp1 = weatherResponse.hourly.temperature_2m[index - 1]
                val temp2 = weatherResponse.hourly.temperature_2m[index]

                val minutesTotal = (t2.time - t1.time).toFloat()
                val minutesPassed = (now.time - t1.time).toFloat()
                val percent = minutesPassed / minutesTotal
                val interpolatedTemp = temp1 + (temp2 - temp1) * percent

                // Aplicar conversión de temperatura según configuración
                val displayTemp = convertTemperature(interpolatedTemp)
                textTemperature.text = "${displayTemp.toInt()}°${getTemperatureSymbol()}"

                val weatherItem = HourlyWeatherItem(
                    weatherResponse.hourly.time[index - 1],
                    interpolatedTemp,
                    weatherResponse.hourly.weather_code[index - 1],
                    weatherResponse.hourly.relative_humidity_2m[index - 1],
                    weatherResponse.hourly.wind_speed_10m[index - 1]
                )
                textClima.text = weatherItem.getWeatherDescription()
                imageClima.setImageResource(weatherItem.getWeatherIcon())
            }
            return
        }

        // Cargar pronóstico por hora (solo desde ahora en adelante)
        val hourlyItems = mutableListOf<HourlyWeatherItem>()

        val calendar = Calendar.getInstance()
        calendar.time = now
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val nowRounded = calendar.time

        for (i in weatherResponse.hourly.time.indices) {
            val itemDate = formatter.parse(weatherResponse.hourly.time[i])
            if (itemDate != null && !itemDate.before(nowRounded)) {
                hourlyItems.add(
                    HourlyWeatherItem(
                        time = weatherResponse.hourly.time[i],
                        temperature = weatherResponse.hourly.temperature_2m[i],
                        weatherCode = weatherResponse.hourly.weather_code[i],
                        humidity = weatherResponse.hourly.relative_humidity_2m[i],
                        windSpeed = weatherResponse.hourly.wind_speed_10m[i]
                    )
                )
            }
        }


        val next24Hours = hourlyItems.take(24)

        // AQUÍ ES EL CAMBIO IMPORTANTE: Pasar la unidad de temperatura
        hourlyAdapter.updateData(next24Hours, unidadTemperatura)

        // También actualiza el clima actual la primera vez
        updateWeatherUI(weatherResponse, interpolated = true)
    }

    private fun convertTemperature(celsius: Double): Double {
        return if (unidadTemperatura == "Fahrenheit") {
            (celsius * 9 / 5) + 32
        } else {
            celsius
        }
    }

    private fun getTemperatureSymbol(): String {
        return if (unidadTemperatura == "Fahrenheit") "F" else "C"
    }

    private fun showError(message: String) {
        if (!isAdded) return

        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        btnReintentarUbicacion.visibility = View.VISIBLE
        textUbicacion.text = "Error al cargar ubicación"
        textTemperature.text = "--°${getTemperatureSymbol()}"
        textClima.text = "No disponible"

        // Detener animación de refresh si está activa
        if (swipeRefreshLayout.isRefreshing) {
            swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun showLocationPermissionError() {
        if (!isAdded) return

        Toast.makeText(
            requireContext(),
            "Se necesitan permisos de ubicación para mostrar el clima",
            Toast.LENGTH_LONG
        ).show()
        btnReintentarUbicacion.visibility = View.VISIBLE
    }

}