package com.dapm.weatherfit.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "configuracion")
data class ConfiguracionEntity(
    @PrimaryKey val id: Int = 1,
    val unidadTemperatura: String,
    val notificacionesActivas: Boolean,
    val modoOscuro: Boolean,
    val preferenciasIA: String

)
