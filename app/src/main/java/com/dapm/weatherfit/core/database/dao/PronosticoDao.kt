package com.dapm.weatherfit.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dapm.weatherfit.core.database.entities.PronosticoEntity

@Dao
interface PronosticoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(pronostico: List<PronosticoEntity>)

    @Query("SELECT * FROM pronostico WHERE fechaGuardado = :fecha")
    suspend fun obtenerPorFecha(fecha: String): List<PronosticoEntity>

    @Query("DELETE FROM pronostico")
    suspend fun eliminarTodo()
}
