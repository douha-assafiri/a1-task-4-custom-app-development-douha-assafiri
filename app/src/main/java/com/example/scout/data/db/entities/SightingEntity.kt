package com.example.scout.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sightings")
data class SightingEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val taxonId: Long?,
    val speciesName: String,
    val category: String,
    val photoPath: String?,
    val latitude: Double?,
    val longitude: Double?,
    val locationName: String?,
    val notes: String?,
    val loggedAt: Long = System.currentTimeMillis()
)