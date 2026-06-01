package com.example.scout.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.scout.data.db.entities.FavouriteEntity
import com.example.scout.data.db.entities.SightingEntity

@Database(entities = [FavouriteEntity::class, SightingEntity::class], version = 1)
abstract class ScoutDatabase : RoomDatabase() {
    abstract fun favouriteDao(): FavouriteDao
    abstract fun sightingDao(): SightingDao

    companion object {
        @Volatile private var INSTANCE: ScoutDatabase? = null

        fun getDatabase(context: Context): ScoutDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context, ScoutDatabase::class.java, "scout_db")
                    .build().also { INSTANCE = it }
            }
        }
    }
}