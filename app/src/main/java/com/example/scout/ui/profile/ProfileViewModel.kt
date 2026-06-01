package com.example.scout.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.scout.data.db.ScoutDatabase
import com.example.scout.data.repository.FavouriteRepository
import com.example.scout.data.repository.SightingRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class ProfileViewModel(app: Application) : AndroidViewModel(app) {

    private val db = ScoutDatabase.getDatabase(app)
    private val sightingRepo = SightingRepository(db.sightingDao())
    private val favouriteRepo = FavouriteRepository(db.favouriteDao())

    val sightingsCount = sightingRepo.count
    val favouritesCount = favouriteRepo.count
    val distinctSpeciesCount = sightingRepo.distinctSpeciesCount
    val recentSightings = sightingRepo.recentSightings
}