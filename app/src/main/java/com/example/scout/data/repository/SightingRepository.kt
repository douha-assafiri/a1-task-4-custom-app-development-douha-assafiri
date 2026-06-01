package com.example.scout.data.repository

import com.example.scout.data.db.SightingDao
import com.example.scout.data.db.entities.SightingEntity

class SightingRepository(
    private val dao: SightingDao
) {
    val allSightings = dao.getAll()
    val recentSightings = dao.getRecent(3)
    val count = dao.count()
    val distinctSpeciesCount = dao.countDistinctSpecies()

    fun getByCategory(category: String) = dao.getByCategory(category)
    fun getByTaxonId(taxonId: Long) = dao.getByTaxonId(taxonId)

    suspend fun add(sighting: SightingEntity) =
        dao.insert(sighting)

    suspend fun remove(sighting: SightingEntity) =
        dao.delete(sighting)

    suspend fun clearAll() =
        dao.deleteAll()
}