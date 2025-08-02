package com.dapm.weatherfit.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clima")
data class ClimaEntity(
    @PrimaryKey val fecha: String,
    val weatherJson: String,
    val latitud: Double,
    val longitud: Double,
    val nombreCiudad: String
)