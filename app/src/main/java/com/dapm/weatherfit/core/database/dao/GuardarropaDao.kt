package com.dapm.weatherfit.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import com.dapm.weatherfit.core.database.entities.GuardarropaEntity
import androidx.room.Query

@Dao
interface GuardarropaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(item: GuardarropaEntity)

    @Query("SELECT * FROM guardarropa")
    suspend fun obtenerTodo(): List<GuardarropaEntity>

    @Delete
    suspend fun eliminar(item: GuardarropaEntity)
}

