package com.dapm.weatherfit.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pronostico")
data class PronosticoEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dia: String, // Ej: "Lunes"
    val estado: String, // Ej: "Lluvia"
    val temperatura: Double,
    val weatherCode: Int,
    val fechaGuardado: String // Ej: "2025-06-19"
)
