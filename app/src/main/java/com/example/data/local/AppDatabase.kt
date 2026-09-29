package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.local.converters.RoomConverters
import com.example.data.local.dao.*
import com.example.data.local.entity.*

@Database(
    entities = [
        LinkEntity::class,
        ActorEntity::class,
        StudioEntity::class,
        HanimeEntity::class,
        CoomerEntity::class,
        SettingsEntity::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun linkDao(): LinkDao
    abstract fun actorDao(): ActorDao
    abstract fun studioDao(): StudioDao
    abstract fun hanimeDao(): HanimeDao
    abstract fun coomerDao(): CoomerDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gvj_vault_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
