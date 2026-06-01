package com.example.scout.data.api.models

import com.google.gson.annotations.SerializedName

data class ConservationStatus(
    val status: String?,
    @SerializedName("status_name") val statusName: String?
)