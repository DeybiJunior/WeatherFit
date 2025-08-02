package com.dapm.weatherfit.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ubicacion")
data class UbicacionEntity(
    @PrimaryKey val id: Int = 1,
    val latitud: Double,
    val longitud: Double,
    val nombreCiudad: String,
    val fechaActualizacion: String
)