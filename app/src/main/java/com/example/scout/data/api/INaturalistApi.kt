package com.example.scout.data.api

import com.example.scout.data.api.models.TaxaResponse
import com.example.scout.data.api.models.TaxonDetailResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface INaturalistApi {

    @GET("taxa?rank=species&place_id=6744&order_by=observations_count")
    suspend fun searchTaxa(
        @Query("q") query: String? = null,
        @Query("per_page") perPage: Int = 20,
        @Query("page") page: Int = 1,
        @Query("iconic_taxa") iconicTaxa: String? = null,
        @Query("taxon_name") taxonName: String? = null,
        @Query("locale") locale: String = "en"
    ): TaxaResponse

    @GET("taxa/{id}")
    suspend fun getTaxon(
        @Path("id") id: Long,
        @Query("locale") locale: String = "en"
    ): TaxonDetailResponse
}