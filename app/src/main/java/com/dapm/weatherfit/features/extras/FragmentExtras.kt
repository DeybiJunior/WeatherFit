package com.dapm.weatherfit.features.extras

import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import android.Manifest
import com.dapm.weatherfit.R
import com.dapm.weatherfit.core.database.AppDatabase
import com.dapm.weatherfit.core.database.entities.ConfiguracionEntity
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.launch
import java.util.Calendar


class FragmentExtras : Fragment(R.layout.fragment_extras) {

    private lateinit var layoutUnidadTemperatura: LinearLayout
    private lateinit var layoutPreferenciasIA: LinearLayout
    private lateinit var layoutCompartir: LinearLayout
    private lateinit var textUnidadSeleccionada: TextView
    private lateinit var textPreferenciasIA: TextView
    private lateinit var switchNotificaciones: SwitchMaterial
    private lateinit var switchModoOscuro: SwitchMaterial

    private var unidadTemperaturaActual = "Celsius"
    private var preferenciasIAActual = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar vistas
        layoutUnidadTemperatura = view.findViewById(R.id.layoutUnidadTemperatura)
        layoutPreferenciasIA = view.findViewById(R.id.layoutPreferenciasIA)
        layoutCompartir = view.findViewById(R.id.layoutCompartir)
        textUnidadSeleccionada = view.findViewById(R.id.textUnidadSeleccionada)
        textPreferenciasIA = view.findViewById(R.id.textPreferenciasIA)
        switchNotificaciones = view.findViewById(R.id.switchNotificaciones)
        switchModoOscuro = view.findViewById(R.id.switchModoOscuro)

        cargarConfiguracion()
        configurarListeners()
        configurarTextoLegal(view.findViewById(R.id.textLegal))
    }
    // Enlace contactos
    private fun configurarTextoLegal(textView: TextView) {
        val fullText = textView.text.toString()
        val spannableString = SpannableString(fullText)

        val enlaces = mapOf(
            "Política de privacidad" to "https://deybijunior.github.io/assets/documents/Pol%C3%ADticadePrivacidad.html",
            "Contacto" to "https://deybijunior.github.io/"
        )

        for ((texto, url) in enlaces) {
            val start = fullText.indexOf(texto)
            if (start != -1) {
                val end = start + texto.length
                val span = object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        widget.context.startActivity(intent)
                    }

                    override fun updateDrawState(ds: TextPaint) {
                        super.updateDrawState(ds)
                        ds.isUnderlineText = true
                        ds.color = Color.GRAY
                    }
                }
                spannableString.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }

        textView.text = spannableString
        textView.movementMethod = LinkMovementMethod.getInstance()
    }


    private fun configurarListeners() {
        // Clic en unidad de temperatura
        layoutUnidadTemperatura.setOnClickListener {
            mostrarDialogoUnidadTemperatura()
        }

        // Clic en preferencias IA
        layoutPreferenciasIA.setOnClickListener {
            mostrarDialogoPreferenciasIA()
        }

        // Clic en compartir
        layoutCompartir.setOnClickListener {
            compartirPronostico()
        }

        // Switches
        switchNotificaciones.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                solicitarPermisoNotificaciones()
            } else {
                cancelarNotificacionDiaria()
            }
            guardarConfiguracion()
        }
        switchModoOscuro.setOnCheckedChangeListener { _, isChecked ->
            aplicarModoOscuro(isChecked)
            guardarConfiguracion()
        }
    }

    //notificacion
    private fun solicitarPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
            } else {
                programarNotificacionDiaria()
            }
        } else {
            programarNotificacionDiaria()
        }
    }

    private fun programarNotificacionDiaria() {
        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(requireContext(), NotificacionReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            1001,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Hora: 8:00 AM
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) add(Calendar.DAY_OF_YEAR, 1) // si ya pasó hoy, programar para mañana
        }

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    private fun cancelarNotificacionDiaria() {
        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(requireContext(), NotificacionReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            1001,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        alarmManager.cancel(pendingIntent)
    }

    //modo oscuro
    private fun aplicarModoOscuro(activar: Boolean) {
        val modo = if (activar) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(modo)
    }


    private fun mostrarDialogoUnidadTemperatura() {
        val opciones = arrayOf("Celsius (°C)", "Fahrenheit (°F)")
        val seleccionActual = if (unidadTemperaturaActual == "Celsius") 0 else 1

        AlertDialog.Builder(requireContext())
            .setTitle("Seleccionar unidad de temperatura")
            .setSingleChoiceItems(opciones, seleccionActual) { dialog, which ->
                unidadTemperaturaActual = if (which == 0) "Celsius" else "Fahrenheit"
                textUnidadSeleccionada.text = opciones[which]
                guardarConfiguracion()
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogoPreferenciasIA() {
        val editText = EditText(requireContext()).apply {
            setText(preferenciasIAActual)
            hint = "Ej: respuestas breves, información detallada, etc."
            setPadding(50, 30, 50, 30)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Preferencias de IA")
            .setMessage("Personaliza cómo quieres que responda la IA:")
            .setView(editText)
            .setPositiveButton("Guardar") { _, _ ->
                preferenciasIAActual = editText.text.toString()
                actualizarTextoPreferenciasIA()
                guardarConfiguracion()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    //compartir
    private fun compartirPronostico() {
        // Crear intent de compartir
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "¡Mira el pronóstico del tiempo en Weatherfit! 🌤️\n\nDescárgate la app para más información meteorológica.")
            putExtra(Intent.EXTRA_SUBJECT, "Pronóstico del Tiempo - Weatherfit")
        }

        // Mostrar selector de apps
        try {
            startActivity(Intent.createChooser(shareIntent, "Compartir pronóstico a través de:"))
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "No se encontraron apps para compartir", Toast.LENGTH_SHORT).show()
        }
    }

    private fun actualizarTextoPreferenciasIA() {
        textPreferenciasIA.text = if (preferenciasIAActual.isNotEmpty()) {
            preferenciasIAActual
        } else {
            "Configurar respuestas personalizadas"
        }
    }

    private fun cargarConfiguracion() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            val config = db.configuracionDao().obtener()

            config?.let {
                unidadTemperaturaActual = it.unidadTemperatura
                preferenciasIAActual = it.preferenciasIA

                textUnidadSeleccionada.text = if (it.unidadTemperatura == "Celsius") "Celsius (°C)" else "Fahrenheit (°F)"
                actualizarTextoPreferenciasIA()
                switchNotificaciones.isChecked = it.notificacionesActivas
                switchModoOscuro.isChecked = it.modoOscuro
            }
        }
    }

    private fun guardarConfiguracion() {
        val config = ConfiguracionEntity(
            id = 1,
            unidadTemperatura = unidadTemperaturaActual,
            notificacionesActivas = switchNotificaciones.isChecked,
            modoOscuro = switchModoOscuro.isChecked,
            preferenciasIA = preferenciasIAActual
        )

        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            db.configuracionDao().guardar(config)
        }
    }
}