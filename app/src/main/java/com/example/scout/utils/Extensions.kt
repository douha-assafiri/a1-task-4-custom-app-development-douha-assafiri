package com.example.scout.utils

import android.content.Context
import android.graphics.Color
import com.example.scout.R

private fun normaliseStatus(status: String?): String? =
    status?.lowercase()?.trim()?.replace(" ", "_")?.replace("-", "_")

fun conservationStatusLabel(status: String?): String = when (normaliseStatus(status)) {
    "lc", "least_concern"                    -> "Least Concern"
    "nt", "near_threatened"                  -> "Near Threatened"
    "vu", "vulnerable"                       -> "Vulnerable"
    "en", "endangered"                       -> "Endangered"
    "cr", "critically_endangered"            -> "Critically Endangered"
    "ew", "extinct_in_the_wild"              -> "Extinct in the Wild"
    "ex", "extinct"                          -> "Extinct"
    "dd", "data_deficient"                   -> "Data Deficient"
    "ne", "not_evaluated"                    -> "Not Evaluated"
    else                                     -> if (status.isNullOrBlank()) "Not Assessed" else "Unknown"
}

fun conservationStatusColor(context: Context, status: String?): Int = when (normaliseStatus(status)) {
    "lc", "least_concern"         -> context.getColor(R.color.status_lc)
    "nt", "near_threatened"       -> context.getColor(R.color.status_nt)
    "vu", "vulnerable"            -> context.getColor(R.color.status_vu)
    "en", "endangered"            -> context.getColor(R.color.status_en)
    "cr", "critically_endangered" -> context.getColor(R.color.status_cr)
    "ew", "extinct_in_the_wild"   -> Color.parseColor("#212121")
    "ex", "extinct"               -> Color.parseColor("#424242")
    else                          -> Color.parseColor("#9E9E9E")
}

fun iconicTaxonLabel(iconicTaxonName: String?): String = when (iconicTaxonName) {
    "Plantae"        -> "Flowering Plant"
    "Fungi"          -> "Fungus"
    "Chromista"      -> "Chromist"
    "Mammalia"       -> "Mammal"
    "Aves"           -> "Bird"
    "Reptilia"       -> "Reptile"
    "Amphibia"       -> "Amphibian"
    "Actinopterygii" -> "Fish"
    "Insecta"        -> "Insect"
    "Arachnida"      -> "Arachnid"
    "Mollusca"       -> "Mollusc"
    "Animalia"       -> "Animal"
    else             -> iconicTaxonName ?: "Unknown"
}

fun conservationStatusDescription(status: String?): String = when (status?.lowercase()) {
    "lc", "least_concern"           -> "Population is stable and not under significant threat of extinction."
    "nt", "near_threatened"         -> "Close to qualifying as threatened — may be at risk if current trends continue."
    "vu", "vulnerable"              -> "Faces a high risk of extinction in the wild without intervention."
    "en", "endangered"              -> "Faces a very high risk of extinction in the wild."
    "cr", "critically_endangered"   -> "Faces an extremely high risk of extinction — urgent conservation needed."
    "ex", "extinct"                 -> "No known individuals remaining anywhere in the world."
    else                            -> "Conservation status has not been formally assessed for this species."
}

fun habitatDescription(iconicTaxonName: String?): String = when (iconicTaxonName) {
    "Plantae"        -> "Found across diverse Australian ecosystems — from tropical rainforests and eucalyptus woodland to coastal heathland and alpine meadows."
    "Fungi"          -> "Thrives in moist, shaded environments across Australia, often appearing after rainfall in forests and grasslands."
    "Mammalia"       -> "Inhabits a wide range of Australian environments, from the arid outback and desert scrub to dense forests and coastal regions."
    "Aves"           -> "Found throughout Australia's diverse habitats, including open woodlands, wetlands, urban gardens, and offshore islands."
    "Reptilia"       -> "Adapted to Australian conditions from tropical rainforest to desert — often found basking on rocks and open ground."
    "Amphibia"       -> "Typically found near permanent or temporary water bodies, including ponds, streams, and flooded grasslands across Australia."
    "Actinopterygii" -> "Inhabits Australian freshwater rivers, lakes, and coastal marine environments depending on species."
    "Insecta"        -> "Found in virtually every Australian habitat — from gardens and forests to deserts and alpine zones."
    "Arachnida"      -> "Occurs across all Australian environments, from humid coastal areas to hot arid inland regions."
    "Mollusca"       -> "Found in marine, freshwater, and terrestrial habitats throughout Australia."
    else             -> "Found across various regions of Australia in suitable habitat for this type of species."
}

fun extractDidYouKnow(summary: String?): String? {
    if (summary.isNullOrBlank()) return null
    val sentences = summary.split(Regex("(?<=\\.)\\s+")).filter { it.length > 40 }
    return sentences.getOrNull(1)?.trim()
        ?: sentences.getOrNull(0)?.let { if (it.length > 80) it else null }
}

fun iconicTaxonCategory(iconicTaxonName: String?): String = when (iconicTaxonName) {
    "Plantae", "Fungi", "Chromista" -> "Plants"
    "Mammalia", "Aves", "Reptilia", "Amphibia",
    "Actinopterygii", "Insecta", "Arachnida", "Mollusca" -> "Animals"
    else -> "Other"
}