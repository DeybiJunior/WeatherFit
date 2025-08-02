package com.dapm.weatherfit.core.network

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class LocationHelper(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun getCurrentLocation(): Result<Location> {
        if (!hasLocationPermission()) {
            return Result.failure(Exception("Permisos de ubicación no concedidos"))
        }

        return suspendCancellableCoroutine { continuation ->
            val cancellationTokenSource = CancellationTokenSource()

            // Configurar request de ubicación
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setWaitForAccurateLocation(true)
                .setMinUpdateIntervalMillis(2000)
                .setMaxUpdateDelayMillis(10000)
                .build()

            // Intentar obtener última ubicación conocida primero
            fusedLocationClient.lastLocation.addOnSuccessListener { lastLocation ->
                if (lastLocation != null && isLocationRecent(lastLocation)) {
                    try {
                        continuation.resume(Result.success(lastLocation))
                    } catch (e: Exception) {
                        // Continuation ya fue resumida
                    }
                    return@addOnSuccessListener
                }

                // Si no hay última ubicación o es muy antigua, solicitar nueva
                requestNewLocation(locationRequest, continuation, cancellationTokenSource)
            }.addOnFailureListener {
                // Si falla obtener última ubicación, solicitar nueva
                requestNewLocation(locationRequest, continuation, cancellationTokenSource)
            }

            // Cancelar si la corrutina se cancela
            continuation.invokeOnCancellation {
                cancellationTokenSource.cancel()
            }
        }
    }

    private fun requestNewLocation(
        locationRequest: LocationRequest,
        continuation: kotlin.coroutines.Continuation<Result<Location>>,
        cancellationTokenSource: CancellationTokenSource
    ) {
        var isCompleted = false

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                if (!isCompleted) {
                    isCompleted = true
                    fusedLocationClient.removeLocationUpdates(this)
                    val location = locationResult.lastLocation
                    if (location != null) {
                        try {
                            continuation.resume(Result.success(location))
                        } catch (e: Exception) {
                            // Continuation ya fue resumida
                        }
                    } else {
                        try {
                            continuation.resume(Result.failure(Exception("No se pudo obtener la ubicación")))
                        } catch (e: Exception) {
                            // Continuation ya fue resumida
                        }
                    }
                }
            }

            override fun onLocationAvailability(availability: LocationAvailability) {
                if (!availability.isLocationAvailable && !isCompleted) {
                    isCompleted = true
                    fusedLocationClient.removeLocationUpdates(this)
                    try {
                        continuation.resume(Result.failure(Exception("Ubicación no disponible. Verifica que el GPS esté habilitado.")))
                    } catch (e: Exception) {
                        // Continuation ya fue resumida
                    }
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )

            // Timeout después de 10 segundos
            android.os.Handler(Looper.getMainLooper()).postDelayed({
                if (!isCompleted) {
                    isCompleted = true
                    fusedLocationClient.removeLocationUpdates(locationCallback)
                    try {
                        continuation.resume(Result.failure(Exception("Timeout al obtener ubicación")))
                    } catch (e: Exception) {
                        // Continuation ya fue resumida
                    }
                }
            }, 10000)

        } catch (e: SecurityException) {
            if (!isCompleted) {
                isCompleted = true
                try {
                    continuation.resume(Result.failure(Exception("Permisos de ubicación denegados")))
                } catch (ex: Exception) {
                    // Continuation ya fue resumida
                }
            }
        }
    }

    private fun isLocationRecent(location: Location): Boolean {
        val fiveMinutesAgo = System.currentTimeMillis() - (5 * 60 * 1000)
        return location.time > fiveMinutesAgo
    }
}