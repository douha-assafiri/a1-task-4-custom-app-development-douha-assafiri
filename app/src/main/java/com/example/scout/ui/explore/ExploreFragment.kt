package com.example.scout.ui.explore

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import coil.load
import com.example.scout.R
import com.example.scout.data.api.models.TaxonResult
import com.example.scout.data.db.entities.SightingEntity
import com.example.scout.databinding.FragmentExploreBinding
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExploreFragment : Fragment() {

    private var _binding: FragmentExploreBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ExploreViewModel by viewModels()
    private lateinit var adapter: SpeciesAdapter

    // (iconic_taxa, optional query) pairs for each tab's chips
    private val plantChips = listOf(
        Triple(R.string.cat_flowers, "Plantae", "flower"),
        Triple(R.string.cat_trees,   "Plantae", "tree"),
        Triple(R.string.cat_fungi,   "Fungi",   "fungi")
    )
    private val animalChips = listOf(
        Triple(R.string.cat_mammals,  "Mammalia", null),
        Triple(R.string.cat_birds,    "Aves",     null),
        Triple(R.string.cat_reptiles, "Reptilia", null)
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentExploreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = SpeciesAdapter { species ->
            findNavController().navigate(
                R.id.action_explore_to_detail,
                android.os.Bundle().apply { putLong("taxonId", species.id) }
            )
        }
        binding.recyclerView.adapter = adapter

        setupTabs()
        observeViewModel()

        binding.swipeRefresh.setOnRefreshListener { viewModel.retry() }

        binding.btnSeeAllSightings.setOnClickListener {
            findNavController().navigate(R.id.sightingsFragment)
        }
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> { viewModel.loadCategory("Plantae"); buildChips("Plantae", plantChips) }
                    1 -> { viewModel.loadCategory("Animalia"); buildChips("Animalia", animalChips) }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
        buildChips("Plantae", plantChips)
    }

    private fun buildChips(allCategory: String, chips: List<Triple<Int, String, String?>>) {
        binding.chipGroupCategory.removeAllViews()
        val allChip = Chip(requireContext()).apply {
            text = getString(R.string.filter_all)
            isCheckable = true
            isChecked = true
            setChipBackgroundColorResource(R.color.green_light)
            setTextColor(requireContext().getColor(R.color.green_primary))
            chipStrokeWidth = 0f
            setOnClickListener { viewModel.loadCategory(allCategory) }
        }
        binding.chipGroupCategory.addView(allChip)
        chips.forEach { (labelRes, iconicTaxa, query) ->
            val chip = Chip(requireContext()).apply {
                text = getString(labelRes)
                isCheckable = true
                setChipBackgroundColorResource(R.color.green_light)
                setTextColor(requireContext().getColor(R.color.green_primary))
                chipStrokeWidth = 0f
                setOnClickListener { viewModel.loadSubcategory(iconicTaxa, query) }
            }
            binding.chipGroupCategory.addView(chip)
        }
    }

    private fun observeViewModel() {
        viewModel.featured.observe(viewLifecycleOwner, ::handleFeaturedSpecies)
        viewModel.species.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            binding.recyclerView.isVisible = true
            binding.swipeRefresh.isRefreshing = false
        }
        viewModel.recentSightings.observe(viewLifecycleOwner, ::updateRecentSightingsUI)
        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.isVisible = loading && binding.featuredCard.tag == null
        }
        viewModel.error.observe(viewLifecycleOwner, ::handleError)
    }

    private fun handleFeaturedSpecies(species: TaxonResult?) {
        if (species != null) {
            binding.featuredCard.isVisible = true
            val photoUrl = species.defaultPhoto?.mediumUrl
            if (photoUrl != null) {
                binding.ivFeatured.imageTintList = null
                binding.ivFeatured.load(photoUrl)
            }
            binding.tvFeaturedName.text = species.commonName ?: species.name
            binding.tvFeaturedSubtitle.text = buildString {
                append(species.name)
                species.iconicTaxonName?.let { append(" · $it") }
            }
            binding.featuredCard.setOnClickListener {
                findNavController().navigate(
                    R.id.action_explore_to_detail,
                    android.os.Bundle().apply { putLong("taxonId", species.id) }
                )
            }
        } else {
            binding.featuredCard.isVisible = false
        }
    }

    private fun updateRecentSightingsUI(sightings: List<SightingEntity>) {
        binding.recentSightingsContainer.removeAllViews()
        binding.tvNoSightings.isVisible = sightings.isEmpty()
        if (sightings.isNotEmpty()) {
            val fmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            sightings.forEach { sighting ->
                val itemView = layoutInflater.inflate(
                    R.layout.item_recent_sighting, binding.recentSightingsContainer, false
                )
                bindRecentSightingItem(itemView, sighting, fmt)
                binding.recentSightingsContainer.addView(itemView)
            }
        }
    }

    private fun bindRecentSightingItem(view: View, sighting: SightingEntity, fmt: SimpleDateFormat) {
        view.findViewById<TextView>(R.id.tvSightingName).text = sighting.speciesName
        view.findViewById<TextView>(R.id.tvSightingDate).text = fmt.format(Date(sighting.loggedAt))
        val thumb = view.findViewById<ImageView>(R.id.ivActivityThumb)
        if (!sighting.photoPath.isNullOrBlank()) {
            thumb.imageTintList = null
            thumb.load(File(sighting.photoPath))
        } else {
            val icon = if (sighting.category == "Plant") R.drawable.ic_explore else R.drawable.ic_log
            thumb.setImageResource(icon)
            thumb.imageTintList = android.content.res.ColorStateList.valueOf(
                requireContext().getColor(R.color.green_primary)
            )
        }
        view.setOnClickListener { findNavController().navigate(R.id.sightingsFragment) }
    }

    private fun handleError(error: String?) {
        if (error != null) {
            binding.swipeRefresh.isRefreshing = false
            Snackbar.make(binding.root, getString(R.string.no_internet), Snackbar.LENGTH_LONG)
                .setAction(getString(R.string.retry)) { viewModel.retry() }
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}