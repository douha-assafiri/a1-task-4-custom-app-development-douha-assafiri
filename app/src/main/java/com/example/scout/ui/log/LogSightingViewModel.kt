package com.example.scout.ui.log

import android.app.Application
import android.location.Geocoder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.scout.data.db.ScoutDatabase
import com.example.scout.data.db.entities.SightingEntity
import com.example.scout.data.repository.SightingRepository
import com.example.scout.data.repository.SpeciesRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class LogSightingViewModel(
    app: Application,
) : AndroidViewModel(app) {
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    private val repo = SightingRepository(ScoutDatabase.getDatabase(app).sightingDao())
    val speciesRepo = SpeciesRepository()

    private val _saved = MutableLiveData<Boolean>()
    val saved: LiveData<Boolean> = _saved

    private val _locationResult = MutableLiveData<LocationResult?>()
    val locationResult: LiveData<LocationResult?> = _locationResult

    data class LocationResult(
        val name: String,
        val latitude: Double,
        val longitude: Double
    )

    data class SightingSubmission(
        val speciesName: String,
        val category: String,
        val photoPath: String?,
        val latitude: Double?,
        val longitude: Double?,
        val locationName: String?,
        val notes: String?,
        val taxonId: Long? = null
    )

    fun resolveAddress(lat: Double, lng: Double) {
        viewModelScope.launch(ioDispatcher) {
            val geocoder = Geocoder(getApplication(), Locale.getDefault())
            try {
                val name = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    suspendAddressFromCoords(geocoder, lat, lng)
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(lat, lng, 1)
                    formatAddress(addresses?.firstOrNull(), lat, lng)
                }
                _locationResult.postValue(LocationResult(name, lat, lng))
            } catch (_: Exception) {
                _locationResult.postValue(LocationResult(formatCoords(lat, lng), lat, lng))
            }
        }
    }

    fun searchLocation(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch(ioDispatcher) {
            val geocoder = Geocoder(getApplication(), Locale.getDefault())
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    suspendAddressFromName(geocoder, query)
                } else {
                    performLegacySearch(geocoder, query)
                }
            } catch (_: Exception) {
                _locationResult.postValue(null)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun performLegacySearch(geocoder: Geocoder, query: String) {
        val addresses = geocoder.getFromLocationName(query, 1)
        val a = addresses?.firstOrNull()
        if (a != null) {
            val name = formatAddressName(a, query)
            _locationResult.postValue(LocationResult(name, a.latitude, a.longitude))
        } else {
            _locationResult.postValue(null)
        }
    }

    private fun formatAddressName(a: android.location.Address, query: String): String {
        return buildString {
            a.locality?.let { append(it) }
            a.adminArea?.let { if (isNotEmpty()) append(", "); append(it) }
        }.ifEmpty { query }
    }

    @androidx.annotation.RequiresApi(android.os.Build.VERSION_CODES.TIRAMISU)
    private suspend fun suspendAddressFromCoords(geocoder: Geocoder, lat: Double, lng: Double): String =
        kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
            geocoder.getFromLocation(lat, lng, 1) { addresses ->
                continuation.resumeWith(Result.success(formatAddress(addresses.firstOrNull(), lat, lng)))
            }
        }

    @androidx.annotation.RequiresApi(android.os.Build.VERSION_CODES.TIRAMISU)
    private suspend fun suspendAddressFromName(geocoder: Geocoder, query: String) =
        kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
            geocoder.getFromLocationName(query, 1) { addresses ->
                val a = addresses.firstOrNull()
                if (a != null) {
                    val name = buildString {
                        a.locality?.let { append(it) }
                        a.adminArea?.let { if (isNotEmpty()) append(", "); append(it) }
                    }.ifEmpty { query }
                    _locationResult.postValue(LocationResult(name, a.latitude, a.longitude))
                } else {
                    _locationResult.postValue(null)
                }
                continuation.resumeWith(Result.success(Unit))
            }
        }

    private fun formatAddress(a: android.location.Address?, lat: Double, lng: Double): String {
        return buildString {
            a?.locality?.let { append(it) }
            a?.adminArea?.let { if (isNotEmpty()) append(", "); append(it) }
        }.ifEmpty { formatCoords(lat, lng) }
    }

    private fun formatCoords(lat: Double?, lng: Double?): String {
        return if ((lat != null) && (lng != null)) "%.4f, %.4f".format(lat, lng) else ""
    }

    fun save(submission: SightingSubmission) {
        viewModelScope.launch {
            repo.add(
                SightingEntity(
                    taxonId = submission.taxonId,
                    speciesName = submission.speciesName,
                    category = submission.category,
                    photoPath = submission.photoPath,
                    latitude = submission.latitude,
                    longitude = submission.longitude,
                    locationName = submission.locationName,
                    notes = submission.notes
                )
            )
            _saved.value = true
        }
    }
}