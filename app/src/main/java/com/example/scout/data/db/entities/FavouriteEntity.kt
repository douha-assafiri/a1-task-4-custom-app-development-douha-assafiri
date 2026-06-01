package com.example.scout.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favourites")
data class FavouriteEntity(
    @PrimaryKey val taxonId: Long,
    val commonName: String,
    val scientificName: String,
    val photoUrl: String?,
    val conservationStatus: String?,
    val category: String,
    val savedAt: Long = System.currentTimeMillis()
)