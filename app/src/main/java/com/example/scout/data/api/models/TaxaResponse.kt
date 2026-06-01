package com.example.scout.data.api.models

import com.google.gson.annotations.SerializedName

data class TaxaResponse(
    @SerializedName("total_results") val totalResults: Int,
    val page: Int,
    @SerializedName("per_page") val perPage: Int,
    val results: List<TaxonResult>
)