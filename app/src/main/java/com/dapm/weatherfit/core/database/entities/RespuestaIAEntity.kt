package com.dapm.weatherfit.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "respuesta_ia")
data class RespuestaIAEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val pregunta: String,
    val respuesta: String,
    val fecha: String
)
