package com.dapm.weatherfit.core.database.dao

import androidx.room.*
import com.dapm.weatherfit.core.database.entities.ClimaEntity

@Dao
interface ClimaDao {
    @Query("SELECT * FROM clima WHERE fecha = :fecha")
    suspend fun obtenerClimaPorFecha(fecha: String): ClimaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarClima(clima: ClimaEntity)
}
