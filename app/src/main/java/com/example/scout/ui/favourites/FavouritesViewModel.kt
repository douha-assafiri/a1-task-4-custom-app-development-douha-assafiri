package com.example.scout.ui.favourites

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import com.example.scout.data.db.ScoutDatabase
import com.example.scout.data.db.entities.FavouriteEntity
import com.example.scout.data.repository.FavouriteRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class FavouritesViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = FavouriteRepository(ScoutDatabase.getDatabase(app).favouriteDao())

    private val _filter = MutableLiveData("All")

    val favourites: LiveData<List<FavouriteEntity>> = _filter.switchMap { filter ->
        when (filter) {
            "Plants" -> repo.getByCategory("Plants")
            "Animals" -> repo.getByCategory("Animals")
            else -> repo.allFavourites
        }
    }

    val totalCount: LiveData<Int> = repo.count

    fun setFilter(filter: String) { _filter.value = filter }
}