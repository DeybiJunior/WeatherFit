package com.dapm.weatherfit.core.database.dao

import androidx.room.*
import com.dapm.weatherfit.core.database.entities.ConfiguracionEntity

@Dao
interface ConfiguracionDao {
    @Query("SELECT * FROM configuracion WHERE id = 1")
    suspend fun obtener(): ConfiguracionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(config: ConfiguracionEntity)

    @Update
    suspend fun actualizar(config: ConfiguracionEntity)
}
