package com.example.scout.ui.explore

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.scout.R
import com.example.scout.data.api.models.TaxonResult
import com.example.scout.databinding.ItemSpeciesCardBinding
import com.example.scout.utils.conservationStatusColor
import com.example.scout.utils.conservationStatusLabel

class SpeciesAdapter(
    private val onItemClick: (TaxonResult) -> Unit
) : ListAdapter<TaxonResult, SpeciesAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(private val binding: ItemSpeciesCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TaxonResult) {
            binding.tvCommonName.text = item.commonName ?: item.name
            binding.tvScientificName.text = item.name
            binding.ivPhoto.load(item.defaultPhoto?.mediumUrl) {
                placeholder(R.drawable.ic_placeholder)
                error(R.drawable.ic_placeholder)
            }
            val status = item.conservationStatus?.status
            binding.tvStatus.text = conservationStatusLabel(status)
            binding.tvStatus.setBackgroundColor(conservationStatusColor(binding.root.context, status))
            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSpeciesCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object DiffCallback : DiffUtil.ItemCallback<TaxonResult>() {
        override fun areItemsTheSame(old: TaxonResult, new: TaxonResult) = old.id == new.id
        override fun areContentsTheSame(old: TaxonResult, new: TaxonResult) = old == new
    }
}