package com.example.scout.ui.explore

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.scout.data.api.models.TaxonResult
import com.example.scout.data.db.ScoutDatabase
import com.example.scout.data.db.entities.SightingEntity
import com.example.scout.data.repository.SightingRepository
import com.example.scout.data.repository.SpeciesRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ExploreViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = SpeciesRepository()
    private val sightingRepo = SightingRepository(ScoutDatabase.getDatabase(app).sightingDao())

    val recentSightings: LiveData<List<SightingEntity>> = sightingRepo.recentSightings

    private val _featured = MutableLiveData<TaxonResult?>()
    val featured: LiveData<TaxonResult?> = _featured

    private val _species = MutableLiveData<List<TaxonResult>>()
    val species: LiveData<List<TaxonResult>> = _species

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private var currentIconicTaxa = "Plantae"

    init { loadCategory("Plantae") }

    fun loadCategory(iconicTaxa: String) {
        currentIconicTaxa = iconicTaxa
        fetch(iconicTaxa, null)
    }

    fun loadSubcategory(iconicTaxa: String, query: String? = null) {
        fetch(iconicTaxa, query)
    }

    fun retry() = loadCategory(currentIconicTaxa)

    private fun fetch(iconicTaxa: String, query: String?) {
        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                val raw = repository.browseByIconicTaxa(iconicTaxa, query).results
                val filtered = raw.filter { matchesIconicTaxa(it.iconicTaxonName, iconicTaxa) }
                _featured.value = filtered.firstOrNull()
                _species.value = filtered.drop(1)
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun matchesIconicTaxa(resultTaxa: String?, filter: String): Boolean {
        val plants = setOf("Plantae", "Fungi", "Chromista")
        val animals = setOf("Animalia", "Mammalia", "Aves", "Reptilia", "Amphibia",
            "Actinopterygii", "Insecta", "Arachnida", "Mollusca")
        return when (filter) {
            "Plantae"  -> resultTaxa in plants
            "Animalia" -> resultTaxa in animals
            else       -> resultTaxa == filter
        }
    }
}