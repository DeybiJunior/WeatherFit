package com.dapm.weatherfit.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.dapm.weatherfit.core.database.dao.ClimaDao
import com.dapm.weatherfit.core.database.dao.ConfiguracionDao
import com.dapm.weatherfit.core.database.dao.GuardarropaDao
import com.dapm.weatherfit.core.database.dao.PronosticoDao
import com.dapm.weatherfit.core.database.dao.RespuestaIADao
import com.dapm.weatherfit.core.database.dao.UbicacionDao
import com.dapm.weatherfit.core.database.entities.ClimaEntity
import com.dapm.weatherfit.core.database.entities.ConfiguracionEntity
import com.dapm.weatherfit.core.database.entities.GuardarropaEntity
import com.dapm.weatherfit.core.database.entities.PronosticoEntity
import com.dapm.weatherfit.core.database.entities.RespuestaIAEntity
import com.dapm.weatherfit.core.database.entities.UbicacionEntity

@Database(
    entities = [
        GuardarropaEntity::class,
        ClimaEntity::class,
        ConfiguracionEntity::class,
        RespuestaIAEntity::class,
        PronosticoEntity::class,
        UbicacionEntity::class
    ],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun guardarropaDao(): GuardarropaDao
    abstract fun climaDao(): ClimaDao
    abstract fun respuestaIADao(): RespuestaIADao
    abstract fun configuracionDao(): ConfiguracionDao
    abstract fun pronosticoDao(): PronosticoDao
    abstract fun ubicacionDao(): UbicacionDao


    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "weatherfit_db"
                )
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }
    }
}
