package com.example.scout.data.repository

import com.example.scout.data.db.FavouriteDao
import com.example.scout.data.db.entities.FavouriteEntity

class FavouriteRepository(
    private val dao: FavouriteDao
) {
    val allFavourites = dao.getAll()
    val count = dao.count()

    fun getByCategory(category: String) = dao.getByCategory(category)

    suspend fun isFavourite(taxonId: Long): Boolean =
        dao.getById(taxonId) != null

    suspend fun add(favourite: FavouriteEntity) =
        dao.insert(favourite)

    suspend fun remove(taxonId: Long) =
        dao.getById(taxonId)?.let { dao.delete(it) }
}