package com.example.scout.data.api.models

import com.google.gson.annotations.SerializedName

data class TaxonResult(
    val id: Long,
    val name: String,
    @SerializedName("preferred_common_name") val commonName: String?,
    val rank: String,
    @SerializedName("conservation_status") val conservationStatus: ConservationStatus?,
    @SerializedName("default_photo") val defaultPhoto: TaxonPhoto?,
    @SerializedName("wikipedia_summary") val wikipediaSummary: String?,
    @SerializedName("iconic_taxon_name") val iconicTaxonName: String?,
    @SerializedName("observations_count") val observationsCount: Int?
)