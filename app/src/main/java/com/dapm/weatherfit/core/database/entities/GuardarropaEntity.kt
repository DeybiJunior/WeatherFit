package com.dapm.weatherfit.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "guardarropa")
data class GuardarropaEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val tipo: String,
    val categoria: String?,
    val fotoUri: String
)
