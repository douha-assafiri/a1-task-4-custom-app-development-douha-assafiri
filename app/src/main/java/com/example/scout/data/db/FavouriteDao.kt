package com.example.scout.data.db

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.scout.data.db.entities.FavouriteEntity

@Dao
interface FavouriteDao {
    @Query("SELECT * FROM favourites ORDER BY savedAt DESC")
    fun getAll(): LiveData<List<FavouriteEntity>>

    @Query("SELECT * FROM favourites WHERE category = :category ORDER BY savedAt DESC")
    fun getByCategory(category: String): LiveData<List<FavouriteEntity>>

    @Query("SELECT * FROM favourites WHERE taxonId = :taxonId LIMIT 1")
    suspend fun getById(taxonId: Long): FavouriteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favourite: FavouriteEntity)

    @Delete
    suspend fun delete(favourite: FavouriteEntity)

    @Query("SELECT COUNT(*) FROM favourites")
    fun count(): LiveData<Int>
}