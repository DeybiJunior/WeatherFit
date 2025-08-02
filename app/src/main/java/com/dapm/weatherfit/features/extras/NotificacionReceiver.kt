package com.dapm.weatherfit.features.extras

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.dapm.weatherfit.R

class NotificacionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = "daily_channel"
        val channelName = "Notificaciones diarias"

        // Crear canal de notificación (solo una vez)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.facebook) // icono que tengas
            .setContentTitle("WeatherFit")
            .setContentText("Consulta el pronóstico del día ☀️")
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1001, notification)
    }
}
