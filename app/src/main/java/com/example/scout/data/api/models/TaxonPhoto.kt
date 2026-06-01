package com.example.scout.data.api.models

import com.google.gson.annotations.SerializedName

data class TaxonPhoto(
    @SerializedName("medium_url") val mediumUrl: String?,
    @SerializedName("square_url") val squareUrl: String?
)