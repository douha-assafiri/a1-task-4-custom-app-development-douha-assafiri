package com.example.scout.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.scout.databinding.FragmentSearchBinding
import com.example.scout.ui.explore.SpeciesAdapter
import com.google.android.material.snackbar.Snackbar
import com.example.scout.R

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SearchViewModel by viewModels()
    private lateinit var adapter: SpeciesAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = SpeciesAdapter { species ->
            findNavController().navigate(
                R.id.action_search_to_detail,
                android.os.Bundle().apply { putLong("taxonId", species.id) }
            )
        }
        binding.recyclerView.adapter = adapter

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.search(newText.orEmpty())
                return true
            }
        })

        binding.chipGroupFilters.setOnCheckedStateChangeListener { _, checkedIds ->
            val filter = when {
                R.id.chipPlants in checkedIds  -> "Plants"
                R.id.chipAnimals in checkedIds -> "Animals"
                else                           -> "All"
            }
            viewModel.setFilter(filter, binding.searchView.query.toString())
        }

        viewModel.isTrending.observe(viewLifecycleOwner) { trending ->
            binding.tvTrendingHeader.isVisible = trending
        }

        viewModel.results.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            val query = binding.searchView.query
            binding.tvEmpty.isVisible = list.isEmpty() && !query.isNullOrBlank()
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.isVisible = loading
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                Snackbar.make(binding.root, getString(R.string.no_internet), Snackbar.LENGTH_LONG)
                    .setAction(getString(R.string.retry)) {
                        viewModel.search(binding.searchView.query.toString())
                    }.show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}