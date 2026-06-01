package com.example.scout.data.repository

import com.example.scout.data.api.ApiClient
import com.example.scout.data.api.models.TaxaResponse
import com.example.scout.data.api.models.TaxonResult
import com.example.scout.utils.LocaleUtils

class SpeciesRepository {
    private val api = ApiClient.api
    private val locale get() = LocaleUtils.apiLocale()

    suspend fun searchTaxa(query: String, page: Int = 1): TaxaResponse =
        api.searchTaxa(query = query, page = page, locale = locale)

    suspend fun browseByIconicTaxa(iconicTaxa: String?, query: String? = null, page: Int = 1): TaxaResponse =
        api.searchTaxa(iconicTaxa = iconicTaxa, query = query, page = page, perPage = 50, locale = locale)

    suspend fun getTaxon(id: Long): TaxonResult =
        api.getTaxon(id, locale = locale).results.first()
}