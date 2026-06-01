package com.example.scout.ui.favourites

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.scout.R
import com.example.scout.databinding.FragmentFavouritesBinding

class FavouritesFragment : Fragment() {

    private var _binding: FragmentFavouritesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FavouritesViewModel by viewModels()
    private lateinit var adapter: FavouritesAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFavouritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = FavouritesAdapter { favourite ->
            findNavController().navigate(
                R.id.action_favourites_to_detail,
                android.os.Bundle().apply { putLong("taxonId", favourite.taxonId) }
            )
        }
        binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerView.adapter = adapter

        binding.chipAll.setOnClickListener { viewModel.setFilter("All") }
        binding.chipPlants.setOnClickListener { viewModel.setFilter("Plants") }
        binding.chipAnimals.setOnClickListener { viewModel.setFilter("Animals") }

        viewModel.totalCount.observe(viewLifecycleOwner) { count ->
            binding.tvCount.text = resources.getQuantityString(R.plurals.saved_species, count, count)
        }

        viewModel.favourites.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            binding.tvEmpty.isVisible = list.isEmpty()
            binding.recyclerView.isVisible = list.isNotEmpty()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}