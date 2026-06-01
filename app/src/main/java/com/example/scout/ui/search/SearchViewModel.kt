package com.example.scout.ui.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scout.data.api.models.TaxonResult
import com.example.scout.data.repository.SpeciesRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchViewModel : ViewModel() {

    private val repository = SpeciesRepository()

    private val _results = MutableLiveData<List<TaxonResult>>()
    val results: LiveData<List<TaxonResult>> = _results

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _isTrending = MutableLiveData(true)
    val isTrending: LiveData<Boolean> = _isTrending

    private var searchJob: Job? = null
    private var currentFilter = "All"

    init {
        loadTrending()
    }

    private fun loadTrending() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val iconicTaxa = when (currentFilter) {
                    "Plants"  -> "Plantae"
                    "Animals" -> "Animalia"
                    else      -> null
                }
                val response = repository.browseByIconicTaxa(iconicTaxa)
                val filtered = when (currentFilter) {
                    "Plants"  -> response.results.filter {
                        it.iconicTaxonName in listOf("Plantae", "Fungi", "Chromista")
                    }
                    "Animals" -> response.results.filter {
                        it.iconicTaxonName in listOf(
                            "Mammalia", "Aves", "Reptilia", "Amphibia",
                            "Actinopterygii", "Insecta", "Arachnida", "Mollusca"
                        )
                    }
                    else -> response.results
                }
                _results.value = filtered.take(8)
                _isTrending.value = true
            } catch (e: Exception) {
                _results.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun search(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _isTrending.value = true
            loadTrending()
            return
        }
        _isTrending.value = false
        searchJob = viewModelScope.launch {
            delay(500)
            _isLoading.value = true
            _error.value = null
            try {
                val response = repository.searchTaxa(query)
                val filtered = when (currentFilter) {
                    "Plants" -> response.results.filter {
                        it.iconicTaxonName in listOf("Plantae", "Fungi", "Chromista")
                    }
                    "Animals" -> response.results.filter {
                        it.iconicTaxonName in listOf(
                            "Mammalia", "Aves", "Reptilia", "Amphibia",
                            "Actinopterygii", "Insecta", "Arachnida", "Mollusca"
                        )
                    }
                    else -> response.results
                }
                _results.value = filtered
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setFilter(filter: String, currentQuery: String) {
        currentFilter = filter
        search(currentQuery)
    }
}