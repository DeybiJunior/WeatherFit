package com.dapm.weatherfit.core.database.dao

import androidx.room.*
import com.dapm.weatherfit.core.database.entities.ClimaEntity
import com.dapm.weatherfit.core.database.entities.RespuestaIAEntity

@Dao
interface RespuestaIADao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(respuestaIA: RespuestaIAEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarClima(clima: ClimaEntity)


    @Query("SELECT * FROM clima WHERE fecha = :fecha LIMIT 1")
    suspend fun obtenerClimaPorFecha(fecha: String): ClimaEntity?

    @Query("SELECT * FROM respuesta_ia ORDER BY id DESC")
    suspend fun obtenerTodas(): List<RespuestaIAEntity>

    @Query("DELETE FROM respuesta_ia")
    suspend fun eliminarTodo()
}
