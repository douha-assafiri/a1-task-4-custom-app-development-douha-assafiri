package com.example.scout.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.example.scout.data.api.models.TaxonResult
import com.example.scout.data.db.ScoutDatabase
import com.example.scout.data.db.entities.FavouriteEntity
import com.example.scout.data.repository.FavouriteRepository
import com.example.scout.data.db.entities.SightingEntity
import com.example.scout.data.repository.SightingRepository
import com.example.scout.data.repository.SpeciesRepository
import com.example.scout.utils.iconicTaxonCategory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SpeciesDetailViewModel(app: Application) : AndroidViewModel(app) {

    private val speciesRepo = SpeciesRepository()
    private val favouriteRepo = FavouriteRepository(ScoutDatabase.getDatabase(app).favouriteDao())
    private val sightingRepo = SightingRepository(ScoutDatabase.getDatabase(app).sightingDao())

    private val _taxon = MutableLiveData<TaxonResult>()
    val taxon: LiveData<TaxonResult> = _taxon

    private val _isFavourite = MutableLiveData<Boolean>()
    val isFavourite: LiveData<Boolean> = _isFavourite

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _taxonId = MutableLiveData<Long>()
    val userSightings: LiveData<List<SightingEntity>> =
        _taxonId.switchMap { id -> sightingRepo.getByTaxonId(id) }

    private val _similarSpecies = MutableLiveData<List<TaxonResult>>(emptyList())
    val similarSpecies: LiveData<List<TaxonResult>> = _similarSpecies

    fun load(taxonId: Long) {
        _taxonId.value = taxonId
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val result = speciesRepo.getTaxon(taxonId)
                _taxon.value = result
                _isFavourite.value = favouriteRepo.isFavourite(taxonId)
                val similar = speciesRepo.browseByIconicTaxa(
                    result.iconicTaxonName ?: "Animalia"
                ).results.filter { it.id != taxonId }.take(6)
                _similarSpecies.value = similar
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleFavourite() {
        val taxon = _taxon.value ?: return
        viewModelScope.launch {
            if (_isFavourite.value == true) {
                favouriteRepo.remove(taxon.id)
                _isFavourite.value = false
            } else {
                favouriteRepo.add(
                    FavouriteEntity(
                        taxonId = taxon.id,
                        commonName = taxon.commonName ?: taxon.name,
                        scientificName = taxon.name,
                        photoUrl = taxon.defaultPhoto?.mediumUrl,
                        conservationStatus = taxon.conservationStatus?.status,
                        category = iconicTaxonCategory(taxon.iconicTaxonName)
                    )
                )
                _isFavourite.value = true
            }
        }
    }
}
