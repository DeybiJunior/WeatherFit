package com.dapm.weatherfit.core.database.dao

import androidx.room.*
import com.dapm.weatherfit.core.database.entities.UbicacionEntity

@Dao
interface UbicacionDao {
    @Query("SELECT * FROM ubicacion WHERE id = 1")
    suspend fun obtenerUbicacion(): UbicacionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarUbicacion(ubicacion: UbicacionEntity)

    @Query("DELETE FROM ubicacion")
    suspend fun eliminarUbicacion()
}