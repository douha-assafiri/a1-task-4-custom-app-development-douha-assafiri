package com.example.scout.ui.favourites

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.scout.R
import com.example.scout.data.db.entities.FavouriteEntity
import com.example.scout.databinding.ItemFavouriteCardBinding
import com.example.scout.utils.conservationStatusColor
import com.example.scout.utils.conservationStatusLabel

class FavouritesAdapter(
    private val onItemClick: (FavouriteEntity) -> Unit
) : ListAdapter<FavouriteEntity, FavouritesAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(private val binding: ItemFavouriteCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FavouriteEntity) {
            binding.tvName.text = item.commonName
            binding.tvScientificName.text = item.scientificName
            binding.tvCategory.text = item.category
            binding.ivPhoto.load(item.photoUrl) {
                placeholder(R.drawable.ic_placeholder)
                error(R.drawable.ic_placeholder)
            }
            val status = item.conservationStatus
            binding.tvStatus.text = conservationStatusLabel(status)
            binding.tvStatus.setBackgroundColor(conservationStatusColor(binding.root.context, status))
            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFavouriteCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    companion object DiffCallback : DiffUtil.ItemCallback<FavouriteEntity>() {
        override fun areItemsTheSame(old: FavouriteEntity, new: FavouriteEntity) = old.taxonId == new.taxonId
        override fun areContentsTheSame(old: FavouriteEntity, new: FavouriteEntity) = old == new
    }
}