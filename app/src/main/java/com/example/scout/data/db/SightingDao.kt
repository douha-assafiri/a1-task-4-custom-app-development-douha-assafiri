package com.example.scout.data.db

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.scout.data.db.entities.SightingEntity

@Dao
interface SightingDao {
    @Query("SELECT * FROM sightings ORDER BY loggedAt DESC")
    fun getAll(): LiveData<List<SightingEntity>>

    @Query("SELECT * FROM sightings ORDER BY loggedAt DESC LIMIT :limit")
    fun getRecent(limit: Int): LiveData<List<SightingEntity>>

    @Query("SELECT * FROM sightings WHERE category = :category ORDER BY loggedAt DESC")
    fun getByCategory(category: String): LiveData<List<SightingEntity>>

    @Insert
    suspend fun insert(sighting: SightingEntity)

    @Delete
    suspend fun delete(sighting: SightingEntity)

    @Query("DELETE FROM sightings")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM sightings")
    fun count(): LiveData<Int>

    @Query("SELECT COUNT(DISTINCT speciesName) FROM sightings")
    fun countDistinctSpecies(): LiveData<Int>

    @Query("SELECT * FROM sightings WHERE taxonId = :taxonId ORDER BY loggedAt DESC")
    fun getByTaxonId(taxonId: Long): LiveData<List<SightingEntity>>
}