package com.example.scout.ui.sightings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.scout.R
import com.example.scout.databinding.FragmentSightingsBinding

class SightingsFragment : Fragment() {

    private var _binding: FragmentSightingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SightingsViewModel by viewModels()
    private lateinit var adapter: SightingsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSightingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = SightingsAdapter { sighting ->
            findNavController().navigate(
                R.id.action_sightings_to_detail,
                android.os.Bundle().apply { putLong("taxonId", sighting.taxonId ?: 0L) }
            )
        }
        binding.recyclerView.adapter = adapter

        binding.chipAll.setOnClickListener { viewModel.setFilter("All") }
        binding.chipPlants.setOnClickListener { viewModel.setFilter("Plant") }
        binding.chipAnimals.setOnClickListener { viewModel.setFilter("Animal") }

        binding.btnClearAll.setOnClickListener {
            showClearConfirmationDialog()
        }

        viewModel.totalCount.observe(viewLifecycleOwner) { count ->
            binding.tvCount.text = resources.getQuantityString(R.plurals.sightings_total, count, count)
            binding.btnClearAll.isVisible = count > 0
        }

        viewModel.sightings.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            binding.tvEmpty.isVisible = list.isEmpty()
            binding.recyclerView.isVisible = list.isNotEmpty()
        }
    }

    private fun showClearConfirmationDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(R.string.clear_sightings_title)
            .setMessage(R.string.clear_sightings_message)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.clearAll()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}