package com.example.scout.ui.sightings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.example.scout.data.db.ScoutDatabase
import com.example.scout.data.db.entities.SightingEntity
import com.example.scout.data.repository.SightingRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SightingsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = SightingRepository(ScoutDatabase.getDatabase(app).sightingDao())

    private val _filter = MutableLiveData("All")

    val sightings: LiveData<List<SightingEntity>> = _filter.switchMap { filter ->
        when (filter) {
            "Plant" -> repo.getByCategory("Plant")
            "Animal" -> repo.getByCategory("Animal")
            else -> repo.allSightings
        }
    }

    val totalCount: LiveData<Int> = repo.count

    fun setFilter(filter: String) { _filter.value = filter }

    fun clearAll() {
        viewModelScope.launch {
            repo.clearAll()
        }
    }
}