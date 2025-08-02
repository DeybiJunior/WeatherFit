package com.dapm.weatherfit.core.network

// WeatherApiService.kt
import com.dapm.weatherfit.core.network.model.WeatherResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApiService {
    @GET("v1/forecast")
    suspend fun getWeatherForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("hourly") hourly: String = "temperature_2m,weather_code,relative_humidity_2m,wind_speed_10m",
        @Query("current") current: String = "temperature_2m,weather_code,relative_humidity_2m,wind_speed_10m",
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") forecastDays: Int = 1
    ): Response<WeatherResponse>
}

