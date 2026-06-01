package com.example.scout.ui.detail

import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import coil.load
import com.example.scout.R
import com.example.scout.data.api.models.TaxonResult
import com.example.scout.data.db.entities.SightingEntity
import com.example.scout.databinding.FragmentSpeciesDetailBinding
import com.example.scout.utils.conservationStatusColor
import com.example.scout.utils.conservationStatusDescription
import com.example.scout.utils.conservationStatusLabel
import com.example.scout.utils.extractDidYouKnow
import com.example.scout.utils.habitatDescription
import com.example.scout.utils.iconicTaxonLabel
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SpeciesDetailFragment : Fragment() {

    private var _binding: FragmentSpeciesDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SpeciesDetailViewModel by viewModels()
    private var loadedTaxon: TaxonResult? = null
    private var favouriteInitialized = false
    private val dateFmt = SimpleDateFormat("d MMM yyyy", Locale.getDefault())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSpeciesDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.load(requireArguments().getLong("taxonId"))
        setupButtons()
        setupTabs()
        observeViewModel()
    }

    private fun setupButtons() {
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnFavourite.setOnClickListener {
            viewModel.toggleFavourite()
            favouriteInitialized = false
        }
        val logClick = View.OnClickListener { navigateToLog() }
        binding.btnLogSighting.setOnClickListener(logClick)
        binding.btnLogSightingFromTab.setOnClickListener(logClick)
    }

    private fun navigateToLog() {
        val taxon = loadedTaxon
        val args = Bundle().apply {
            putString("prefillName", taxon?.commonName ?: taxon?.name ?: "")
            putInt("prefillTaxonId", taxon?.id?.toInt() ?: -1)
            putString("prefillIconicTaxon", taxon?.iconicTaxonName ?: "")
        }
        findNavController().navigate(R.id.action_detail_to_log, args)
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                binding.overviewContent.isVisible  = tab.position == 0
                binding.habitatContent.isVisible   = tab.position == 1
                binding.sightingsContent.isVisible = tab.position == 2
            }
            override fun onTabUnselected(tab: TabLayout.Tab)  {
                // no action needed when tab is unselected
            }
            override fun onTabReselected(tab: TabLayout.Tab)  {
                // no action needed when tab is reselected
            }
        })
    }

    private fun observeViewModel() {
        viewModel.taxon.observe(viewLifecycleOwner, ::bindTaxon)
        viewModel.similarSpecies.observe(viewLifecycleOwner, ::bindSimilarSpecies)
        viewModel.isFavourite.observe(viewLifecycleOwner, ::bindFavourite)
        viewModel.userSightings.observe(viewLifecycleOwner, ::bindUserSightings)
        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.isVisible = loading
            if (!loading) binding.overviewContent.isVisible = true
        }
        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error != null) Snackbar.make(binding.root, getString(R.string.no_internet), Snackbar.LENGTH_LONG).show()
        }
    }

    private fun bindTaxon(taxon: TaxonResult) {
        loadedTaxon = taxon
        binding.ivHero.load(taxon.defaultPhoto?.mediumUrl) { placeholder(R.drawable.ic_placeholder) }
        binding.tvCommonName.text = taxon.commonName ?: taxon.name
        binding.tvScientificName.text = taxon.name

        applyStatusPill(taxon)

        binding.chipType.text = iconicTaxonLabel(taxon.iconicTaxonName)
        val count = taxon.observationsCount
        if (count != null && count > 0) {
            val formatted = NumberFormat.getNumberInstance(Locale.getDefault()).format(count)
            binding.chipObservations.text = getString(R.string.observations_worldwide, formatted)
            binding.chipObservations.isVisible = true
        } else {
            binding.chipObservations.isVisible = false
        }

        val rawSummary = taxon.wikipediaSummary ?: getString(R.string.no_description)
        val parsedSummary = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            Html.fromHtml(rawSummary, Html.FROM_HTML_MODE_LEGACY)
        else
            @Suppress("DEPRECATION") Html.fromHtml(rawSummary)
        binding.tvAbout.text = parsedSummary

        val funFact = extractDidYouKnow(parsedSummary.toString())
        binding.tvDidYouKnow.text = funFact
        binding.didYouKnowCard.isVisible = funFact != null

        binding.tvWhereToFind.text = habitatDescription(taxon.iconicTaxonName)
        binding.tvClassType.text = iconicTaxonLabel(taxon.iconicTaxonName)
        binding.tvClassScientific.text = taxon.name
    }

    private fun applyStatusPill(taxon: TaxonResult) {
        val code = taxon.conservationStatus?.status ?: taxon.conservationStatus?.statusName
        val label = "● ${conservationStatusLabel(code)}"
        val color = conservationStatusColor(requireContext(), code)
        fun pill() = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 100f
            setColor(ColorUtils.setAlphaComponent(color, 40))
        }
        binding.tvStatus.text = label
        binding.tvStatus.setTextColor(color)
        binding.tvStatus.background = pill()
        binding.tvStatusHabitat.text = label
        binding.tvStatusHabitat.setTextColor(color)
        binding.tvStatusHabitat.background = pill()
        binding.tvStatusDescription.text = conservationStatusDescription(code)
    }

    private fun bindFavourite(isFav: Boolean) {
        binding.btnFavourite.setImageResource(
            if (isFav) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
        )
        if (!favouriteInitialized) {
            favouriteInitialized = true
            return
        }
        val msg = if (isFav) getString(R.string.added_to_favourites)
                  else getString(R.string.removed_from_favourites)
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }

    private fun bindSimilarSpecies(list: List<TaxonResult>) {
        binding.similarSpeciesContainer.removeAllViews()
        binding.similarSpeciesSection.isVisible = list.isNotEmpty()
        list.forEach { species ->
            val card = layoutInflater.inflate(R.layout.item_similar_species, binding.similarSpeciesContainer, false)
            val iv = card.findViewById<ImageView>(R.id.ivSimilar)
            if (species.defaultPhoto?.mediumUrl != null) {
                iv.imageTintList = null
                iv.load(species.defaultPhoto.mediumUrl)
            }
            card.findViewById<TextView>(R.id.tvSimilarName).text = species.commonName ?: species.name
            card.findViewById<TextView>(R.id.tvSimilarScientific).text = species.name
            card.setOnClickListener {
                findNavController().navigate(
                    R.id.action_detail_to_detail,
                    Bundle().apply { putLong("taxonId", species.id) }
                )
            }
            binding.similarSpeciesContainer.addView(card)
        }
    }

    private fun bindUserSightings(sightings: List<SightingEntity>) {
        val count = sightings.size
        binding.tvSightingsCount.text = when (count) {
            0    -> getString(R.string.sightings_count_none)
            1    -> getString(R.string.sightings_count_one)
            else -> getString(R.string.sightings_count_many, count)
        }
        binding.tvNoPersonalSightings.isVisible = count == 0
        binding.userSightingsContainer.removeAllViews()
        sightings.forEach { sighting -> binding.userSightingsContainer.addView(buildSightingView(sighting)) }
    }

    private fun buildSightingView(sighting: SightingEntity): View {
        val v = layoutInflater.inflate(R.layout.item_recent_sighting, binding.userSightingsContainer, false)
        v.findViewById<TextView>(R.id.tvSightingName).text = sighting.speciesName
        val dateStr = buildString {
            append(dateFmt.format(Date(sighting.loggedAt)))
            sighting.locationName?.let { append(" · $it") }
        }
        v.findViewById<TextView>(R.id.tvSightingDate).text = dateStr

        v.findViewById<TextView>(R.id.tvSightingNotes).apply {
            if (!sighting.notes.isNullOrBlank()) {
                text = sighting.notes
                isVisible = true
            } else {
                isVisible = false
            }
        }

        val thumb = v.findViewById<ImageView>(R.id.ivActivityThumb)
        if (!sighting.photoPath.isNullOrBlank()) {
            thumb.imageTintList = null
            thumb.load(File(sighting.photoPath))
        }
        return v
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}